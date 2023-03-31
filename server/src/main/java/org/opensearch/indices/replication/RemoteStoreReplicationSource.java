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
import org.opensearch.action.ActionListener;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.index.shard.IndexShard;
import org.opensearch.index.store.Store;
import org.opensearch.index.store.StoreFileMetadata;
import org.opensearch.indices.recovery.RecoverySettings;
import org.opensearch.indices.recovery.RetryableTransportClient;
import org.opensearch.indices.replication.checkpoint.ReplicationCheckpoint;
import org.opensearch.transport.TransportResponse;
import org.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.opensearch.indices.replication.SegmentReplicationSourceService.Actions.GET_CHECKPOINT_INFO;

public class RemoteStoreReplicationSource implements SegmentReplicationSource {

    private static final Logger logger = LogManager.getLogger(PrimaryShardReplicationSource.class);

    private final RetryableTransportClient transportClient;

    private final DiscoveryNode sourceNode;
    private final DiscoveryNode targetNode;
    private final String targetAllocationId;
    private final RecoverySettings recoverySettings;

    public RemoteStoreReplicationSource(
        DiscoveryNode targetNode,
        String targetAllocationId,
        TransportService transportService,
        RecoverySettings recoverySettings,
        DiscoveryNode sourceNode
    ) {
        this.targetAllocationId = targetAllocationId;
        this.transportClient = new RetryableTransportClient(
            transportService,
            sourceNode,
            recoverySettings.internalActionRetryTimeout(),
            logger
        );
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
        this.recoverySettings = recoverySettings;
    }

    @Override
    public void getCheckpointMetadata(long replicationId, ReplicationCheckpoint checkpoint, ActionListener<CheckpointInfoResponse> listener) {
        logger.info("[ankikala] Skipping getCheckpointMetadata");
        listener.onResponse(new CheckpointInfoResponse(null, null, null));
    }

    @Override
    public void getSegmentFiles(long replicationId, ReplicationCheckpoint checkpoint, List<StoreFileMetadata> filesToFetch, IndexShard indexShard, ActionListener<GetSegmentFilesResponse> listener) {
        /*
        try {
            logger.info("[ankikala] Source: syncing segments from remote store");
            indexShard.syncSegmentsFromRemoteSegmentStore(false);
        } catch (IOException e) {
            logger.error("[ankikala] Failed to sync segments {}", e);
            e.printStackTrace();
        }
        */
        listener.onResponse(new GetSegmentFilesResponse(Collections.emptyList()));
    }

    @Override
    public String getDescription() {
        return null;
    }
}
