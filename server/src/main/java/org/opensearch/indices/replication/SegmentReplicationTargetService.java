/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.opensearch.OpenSearchException;
import org.opensearch.action.ActionListener;
import org.opensearch.action.ActionType;
import org.opensearch.action.support.ChannelActionListener;
import org.opensearch.client.Client;
import org.opensearch.common.Nullable;
import org.opensearch.common.settings.Settings;
import org.opensearch.index.shard.IndexEventListener;
import org.opensearch.index.shard.IndexShard;
import org.opensearch.index.shard.ShardId;
import org.opensearch.indices.recovery.FileChunkRequest;
import org.opensearch.indices.recovery.RecoverySettings;
import org.opensearch.indices.replication.checkpoint.ReplicationCheckpoint;
import org.opensearch.indices.replication.checkpoint.crosscluster.PullRemoteSegmentFilesRequest;
import org.opensearch.indices.replication.checkpoint.crosscluster.RemoteClusterConfig;
import org.opensearch.indices.replication.common.ReplicationCollection;
import org.opensearch.indices.replication.common.ReplicationCollection.ReplicationRef;
import org.opensearch.indices.replication.common.ReplicationListener;
import org.opensearch.indices.replication.common.ReplicationState;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportChannel;
import org.opensearch.transport.TransportRequestHandler;
import org.opensearch.transport.TransportService;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service class that orchestrates replication events on replicas.
 *
 * @opensearch.internal
 */
public class SegmentReplicationTargetService implements IndexEventListener {

    private static final Logger logger = LogManager.getLogger(SegmentReplicationTargetService.class);

    private final ThreadPool threadPool;
    private final RecoverySettings recoverySettings;
    private final TransportService transportService;
    private final Client client;

    private final ReplicationCollection<SegmentReplicationTarget> onGoingReplications;

    private final SegmentReplicationSourceFactory sourceFactory;

    /**
     * The internal actions
     *
     * @opensearch.internal
     */
    public static class Actions {
        public static final String FILE_CHUNK = "internal:index/shard/replication/file_chunk";
    }

    public SegmentReplicationTargetService(
        final ThreadPool threadPool,
        final RecoverySettings recoverySettings,
        final TransportService transportService,
        final Client client,
        final SegmentReplicationSourceFactory sourceFactory
    ) {
        this.threadPool = threadPool;
        this.recoverySettings = recoverySettings;
        this.transportService = transportService;
        this.onGoingReplications = new ReplicationCollection<>(logger, threadPool);
        this.sourceFactory = sourceFactory;
        this.client = client;

        transportService.registerRequestHandler(
            Actions.FILE_CHUNK,
            ThreadPool.Names.GENERIC,
            FileChunkRequest::new,
            new FileChunkTransportRequestHandler()
        );

        transportService.registerRequestHandler(
            PullSegmentsAction.NAME,
            ThreadPool.Names.GENERIC,
            PullRemoteSegmentFilesRequest::new,
            new PullRemoteSegmentsActionRequestHandler()
        );
    }

    @Override
    public void beforeIndexShardClosed(ShardId shardId, @Nullable IndexShard indexShard, Settings indexSettings) {
        if (indexShard != null) {
            onGoingReplications.cancelForShard(shardId, "shard closed");
        }
    }

    public synchronized void onNewCheckpoint(final ReplicationCheckpoint receivedCheckpoint, final IndexShard replicaShard, Optional<RemoteClusterConfig> remoteClusterConfig) {
        if (onGoingReplications.isShardReplicating(replicaShard.shardId())) {
            logger.trace(
                () -> new ParameterizedMessage(
                    "Ignoring new replication checkpoint - shard is currently replicating to checkpoint {}",
                    replicaShard.getLatestReplicationCheckpoint()
                )
            );
            return;
        }

        if (replicaShard.shouldProcessCheckpoint(receivedCheckpoint)) {
            startReplication(receivedCheckpoint, replicaShard, remoteClusterConfig, new SegmentReplicationListener() {
                @Override
                public void onReplicationDone(SegmentReplicationState state) {}

                @Override
                public void onReplicationFailure(SegmentReplicationState state, OpenSearchException e, boolean sendShardFailure) {
                    if (sendShardFailure == true) {
                        logger.error("replication failure", e);
                        replicaShard.failShard("replication failure", e);
                    }
                }
            });

        }
    }

    /**
     * Invoked when a new checkpoint is received from a primary shard.
     * It checks if a new checkpoint should be processed or not and starts replication if needed.
     * @param receivedCheckpoint       received checkpoint that is checked for processing
     * @param replicaShard      replica shard on which checkpoint is received
     */
    public synchronized void onNewCheckpoint(final ReplicationCheckpoint receivedCheckpoint, final IndexShard replicaShard) {
        onNewCheckpoint(receivedCheckpoint, replicaShard, Optional.empty());
    }

    public void startReplication(
        final ReplicationCheckpoint checkpoint,
        final IndexShard indexShard,
        final SegmentReplicationListener listener
    ) {
        startReplication(checkpoint, indexShard, Optional.empty(), listener);
    }

    public void startReplication(
        final ReplicationCheckpoint checkpoint,
        final IndexShard indexShard,
        Optional<RemoteClusterConfig> remoteClusterConfig,
        final SegmentReplicationListener listener
    ) {
        startReplication(new SegmentReplicationTarget(checkpoint, indexShard, sourceFactory.get(indexShard, remoteClusterConfig), listener));
    }

    public void startReplication(final SegmentReplicationTarget target) {
        final long replicationId = onGoingReplications.start(target, recoverySettings.activityTimeout());
        logger.trace(() -> new ParameterizedMessage("Starting replication {}", replicationId));
        threadPool.generic().execute(new ReplicationRunner(replicationId));
    }

    /**
     * Listener that runs on changes in Replication state
     *
     * @opensearch.internal
     */
    public interface SegmentReplicationListener extends ReplicationListener {

        @Override
        default void onDone(ReplicationState state) {
            onReplicationDone((SegmentReplicationState) state);
        }

        @Override
        default void onFailure(ReplicationState state, OpenSearchException e, boolean sendShardFailure) {
            onReplicationFailure((SegmentReplicationState) state, e, sendShardFailure);
        }

        void onReplicationDone(SegmentReplicationState state);

        void onReplicationFailure(SegmentReplicationState state, OpenSearchException e, boolean sendShardFailure);
    }

    /**
     * Runnable implementation to trigger a replication event.
     */
    private class ReplicationRunner implements Runnable {

        final long replicationId;

        public ReplicationRunner(long replicationId) {
            this.replicationId = replicationId;
        }

        @Override
        public void run() {
            start(replicationId);
        }
    }

    private void start(final long replicationId) {
        try (ReplicationRef<SegmentReplicationTarget> replicationRef = onGoingReplications.get(replicationId)) {
            replicationRef.get().startReplication(new ActionListener<>() {
                @Override
                public void onResponse(Void o) {
                    onGoingReplications.markAsDone(replicationId);
                }

                @Override
                public void onFailure(Exception e) {
                    onGoingReplications.fail(replicationId, new OpenSearchException("Segment Replication failed", e), true);
                }
            });
        }
    }

    private class FileChunkTransportRequestHandler implements TransportRequestHandler<FileChunkRequest> {

        // How many bytes we've copied since we last called RateLimiter.pause
        final AtomicLong bytesSinceLastPause = new AtomicLong();

        @Override
        public void messageReceived(final FileChunkRequest request, TransportChannel channel, Task task) throws Exception {
            //TODO: move to getSafe.
            try (ReplicationRef<SegmentReplicationTarget> ref = onGoingReplications.get(request.recoveryId())) {
                final SegmentReplicationTarget target = ref.get();
                final ActionListener<Void> listener = target.createOrFinishListener(channel, Actions.FILE_CHUNK, request);
                target.handleFileChunk(request, target, bytesSinceLastPause, recoverySettings.rateLimiter(), listener);
            }
        }
    }

    public static class PullSegmentsAction extends ActionType<GetSegmentFilesResponse> {

        public static final PullSegmentsAction INSTANCE = new PullSegmentsAction();
        public static final String NAME = "internal:index/shard/replication/pull_segment_files";

        private PullSegmentsAction() {
            super(NAME, GetSegmentFilesResponse::new);
        }

    }

    public class PullRemoteSegmentsActionRequestHandler implements TransportRequestHandler<PullRemoteSegmentFilesRequest> {
        @Override
        public void messageReceived(PullRemoteSegmentFilesRequest request, TransportChannel channel, Task task) throws Exception {
            PullRemoteSegmentHandler.create(threadPool, recoverySettings, transportService, client, request, logger,
                new ChannelActionListener<>(channel, PullSegmentsAction.NAME, request)).start();
        }
    }

}
