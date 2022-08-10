/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication.checkpoint.crosscluster;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionListener;
import org.opensearch.client.Client;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.index.store.Store;
import org.opensearch.index.store.StoreFileMetadata;
import org.opensearch.indices.recovery.RecoverySettings;
import org.opensearch.indices.recovery.RetryableTransportClient;
import org.opensearch.indices.replication.*;
import org.opensearch.indices.replication.checkpoint.ReplicationCheckpoint;
import org.opensearch.transport.TransportService;

import java.util.List;
import java.util.Optional;

import static org.opensearch.indices.replication.SegmentReplicationSourceService.GetCheckpointInfoAction;

/**
 * Implementation of a {@link SegmentReplicationSource} where the source is a primary node.
 * This code executes on the target node.
 *
 * @opensearch.internal
 */
public class LeaderClusterSegmentReplicationSource implements SegmentReplicationSource {

    private static final Logger logger = LogManager.getLogger(LeaderClusterSegmentReplicationSource.class);


    private final DiscoveryNode currentNode;
    private final String targetAllocationId;
    private final Client remoteClient;
    private final DiscoveryNode targetNode;
    private final RetryableTransportClient transportClient;
    private final String remoteClusterAlias;

    public LeaderClusterSegmentReplicationSource(
        DiscoveryNode currentNode,
        String targetAllocationId,
        RemoteClusterConfig remoteClusterConfig,
        RecoverySettings recoverySettings, TransportService transportService) {
        this.targetAllocationId = targetAllocationId;
        this.currentNode = currentNode;
        this.remoteClient = remoteClusterConfig.getRemoteClient();
        this.remoteClusterAlias = remoteClusterConfig.remoteClusterAlias();
        this.targetNode = remoteClusterConfig.getTargetNode();
        this.transportClient = new RetryableTransportClient(
            transportService,
            currentNode,
            recoverySettings.internalActionRetryTimeout(),
            logger
        );
    }

    @Override
    public void getCheckpointMetadata(
        long replicationId,
        ReplicationCheckpoint checkpoint,
        ActionListener<CheckpointInfoResponse> listener
    ) {
        logger.info("Getting checkpoint metadata from leader");
        final ActionListener<CheckpointInfoResponse> responseListener = ActionListener.map(listener, r -> r);
        final CheckpointInfoRequest request = new CheckpointInfoRequest(replicationId, targetAllocationId, targetNode, true, checkpoint);
        //TODO: Add retries
        remoteClient.execute(GetCheckpointInfoAction.INSTANCE, request, responseListener);
    }

    @Override
    public void getSegmentFiles(
        long replicationId,
        ReplicationCheckpoint checkpoint,
        List<StoreFileMetadata> filesToFetch,
        Store store,
        ActionListener<GetSegmentFilesResponse> listener
    ) {

        logger.info("Getting segments file from leader: {}", filesToFetch);
        final PullRemoteSegmentFilesRequest request = new PullRemoteSegmentFilesRequest(
            replicationId,
            targetAllocationId,
            targetNode,
            filesToFetch,
            checkpoint,
            currentNode,
            Optional.of(remoteClusterAlias));

        final Writeable.Reader<GetSegmentFilesResponse> reader = GetSegmentFilesResponse::new;
        // Pull the segments from leader shard(instead of the existing push).
        // TODO: Add retries
        transportClient.executeRetryableAction(SegmentReplicationTargetService.PullSegmentsAction.NAME, request, listener, reader);

    }
}

