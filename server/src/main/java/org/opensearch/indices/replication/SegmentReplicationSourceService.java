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
import org.opensearch.action.ActionType;
import org.opensearch.action.support.ChannelActionListener;
import org.opensearch.action.support.replication.crosscluster.follower.SyncLeaderSegmentsAction;
import org.opensearch.action.support.replication.crosscluster.follower.SyncLeaderSegmentsResponse;
import org.opensearch.cluster.ClusterChangedEvent;
import org.opensearch.cluster.ClusterStateListener;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.Nullable;
import org.opensearch.common.component.AbstractLifecycleComponent;
import org.opensearch.common.settings.Settings;
import org.opensearch.index.shard.IndexEventListener;
import org.opensearch.index.shard.IndexShard;
import org.opensearch.index.shard.ShardId;
import org.opensearch.indices.IndicesService;
import org.opensearch.indices.recovery.RecoverySettings;
import org.opensearch.indices.recovery.RetryableTransportClient;
import org.opensearch.indices.replication.checkpoint.crosscluster.GetSegmentChunkRequest;
import org.opensearch.indices.replication.checkpoint.crosscluster.GetSegmentChunkResponse;
import org.opensearch.indices.replication.common.CopyState;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportActionProxy;
import org.opensearch.transport.TransportChannel;
import org.opensearch.transport.TransportRequestHandler;
import org.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service class that handles segment replication requests from replica shards.
 * Typically, the "source" is a primary shard. This code executes on the source node.
 *
 * @opensearch.internal
 */
public final class SegmentReplicationSourceService extends AbstractLifecycleComponent implements ClusterStateListener, IndexEventListener {

    private static final Logger logger = LogManager.getLogger(SegmentReplicationSourceService.class);
    private final RecoverySettings recoverySettings;
    private final TransportService transportService;
    private final IndicesService indicesService;

    /**
     * Internal actions used by the segment replication source service on the primary shard
     *
     * @opensearch.internal
     */
    public static class GetCheckpointInfoAction extends ActionType<CheckpointInfoResponse> {

        public static final GetCheckpointInfoAction INSTANCE = new GetCheckpointInfoAction();
        public static final String NAME = "internal:index/shard/replication/get_checkpoint_info";

        private GetCheckpointInfoAction() {
            super(NAME, CheckpointInfoResponse::new);
        }
    }

    public static class GetSegmentFilesAction extends ActionType<GetSegmentFilesResponse> {

        public static final GetSegmentFilesAction INSTANCE = new GetSegmentFilesAction();
        public static final String NAME = "internal:index/shard/replication/get_segment_files";

        private GetSegmentFilesAction() {
            super(NAME, GetSegmentFilesResponse::new);
        }

    }

    public static class GetSegmentChunkAction extends ActionType<GetSegmentChunkResponse> {

        public static final GetSegmentChunkAction INSTANCE = new GetSegmentChunkAction();
        public static final String NAME = "internal:index/shard/replication/get_segment_chunk";

        private GetSegmentChunkAction() {
            super(NAME, GetSegmentChunkResponse::new);
        }

    }

    // TODO: Remove stale strings
    public static class Actions {

        public static final String GET_CHECKPOINT_INFO = "internal:index/shard/replication/get_checkpoint_info";
        public static final String GET_SEGMENT_FILES = "internal:index/shard/replication/get_segment_files";
    }

    private final OngoingSegmentReplications ongoingSegmentReplications;

    public SegmentReplicationSourceService(
        IndicesService indicesService,
        TransportService transportService,
        RecoverySettings recoverySettings
    ) {
        this.transportService = transportService;
        this.indicesService = indicesService;
        this.recoverySettings = recoverySettings;

        transportService.registerRequestHandler(
            GetCheckpointInfoAction.NAME,
            ThreadPool.Names.GENERIC,
            CheckpointInfoRequest::new,
            new CheckpointInfoRequestHandler()
        );
        transportService.registerRequestHandler(
            GetSegmentFilesAction.NAME,
            ThreadPool.Names.GENERIC,
            GetSegmentFilesRequest::new,
            new GetSegmentFilesRequestHandler()
        );
        transportService.registerRequestHandler(
            GetSegmentChunkAction.NAME,
            ThreadPool.Names.GENERIC,
            GetSegmentChunkRequest::new,
            new GetSegmentChunkRequestHandler()
        );

        TransportActionProxy.registerProxyAction(transportService, GetCheckpointInfoAction.NAME, CheckpointInfoResponse::new);
        TransportActionProxy.registerProxyAction(transportService, GetSegmentFilesAction.NAME, GetSegmentFilesResponse::new);
        TransportActionProxy.registerProxyAction(transportService, GetSegmentChunkAction.NAME, GetSegmentChunkResponse::new);

        this.ongoingSegmentReplications = new OngoingSegmentReplications(indicesService, recoverySettings);
    }

    private class CheckpointInfoRequestHandler implements TransportRequestHandler<CheckpointInfoRequest> {
        @Override
        public void messageReceived(CheckpointInfoRequest request, TransportChannel channel, Task task) throws Exception {
            final RemoteSegmentFileChunkWriter segmentSegmentFileChunkWriter = new RemoteSegmentFileChunkWriter(
                request.getReplicationId(),
                recoverySettings,
                new RetryableTransportClient(
                    transportService,
                    request.getTargetNode(),
                    recoverySettings.internalActionRetryTimeout(),
                    logger
                ),
                request.getCheckpoint().getShardId(),
                SegmentReplicationTargetService.Actions.FILE_CHUNK,
                new AtomicLong(0),
                (throttleTime) -> {}
            );
            final CopyState copyState = ongoingSegmentReplications.prepareForReplication(request, segmentSegmentFileChunkWriter);
            channel.sendResponse(
                new CheckpointInfoResponse(
                    copyState.getCheckpoint(),
                    copyState.getMetadataSnapshot(),
                    copyState.getInfosBytes(),
                    copyState.getPendingDeleteFiles()
                )
            );
        }
    }

    private class GetSegmentFilesRequestHandler implements TransportRequestHandler<GetSegmentFilesRequest> {
        @Override
        public void messageReceived(GetSegmentFilesRequest request, TransportChannel channel, Task task) throws Exception {
            ongoingSegmentReplications.startSegmentCopy(request, new ChannelActionListener<>(channel, Actions.GET_SEGMENT_FILES, request));
        }
    }

    @Override
    public void clusterChanged(ClusterChangedEvent event) {
        if (event.nodesRemoved()) {
            for (DiscoveryNode removedNode : event.nodesDelta().removedNodes()) {
                ongoingSegmentReplications.cancelReplication(removedNode);
            }
        }
    }

    @Override
    protected void doStart() {
        final ClusterService clusterService = indicesService.clusterService();
        if (DiscoveryNode.isDataNode(clusterService.getSettings())) {
            clusterService.addListener(this);
        }
    }

    @Override
    protected void doStop() {
        final ClusterService clusterService = indicesService.clusterService();
        if (DiscoveryNode.isDataNode(clusterService.getSettings())) {
            indicesService.clusterService().removeListener(this);
        }
    }

    @Override
    protected void doClose() throws IOException {

    }

    @Override
    public void beforeIndexShardClosed(ShardId shardId, @Nullable IndexShard indexShard, Settings indexSettings) {
        if (indexShard != null) {
            ongoingSegmentReplications.cancel(indexShard, "shard is closed");
        }
    }

    private class GetSegmentChunkRequestHandler implements TransportRequestHandler<GetSegmentChunkRequest> {
        @Override
        public void messageReceived(GetSegmentChunkRequest request, TransportChannel channel, Task task) throws Exception {
            ongoingSegmentReplications.fetchSegmentChunk(request, new ChannelActionListener<>(channel, GetSegmentChunkAction.NAME, request));
        }
    }
}
