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
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FilterDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.util.Version;
import org.opensearch.action.ActionListener;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.index.shard.IndexShard;
import org.opensearch.index.store.RemoteSegmentStoreDirectory;
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
import java.util.Map;
import java.util.stream.Collectors;

import static org.opensearch.indices.replication.SegmentReplicationSourceService.Actions.GET_CHECKPOINT_INFO;

public class RemoteStoreReplicationSource implements SegmentReplicationSource {

    private static final Logger logger = LogManager.getLogger(PrimaryShardReplicationSource.class);

    private final IndexShard indexShard;

    public RemoteStoreReplicationSource(IndexShard indexShard) {
        this.indexShard = indexShard;
    }

    @Override
    public void getCheckpointMetadata(long replicationId, ReplicationCheckpoint checkpoint, ActionListener<CheckpointInfoResponse> listener) {
        //RemoteSegmentStoreDirectory storeDirectory = (RemoteSegmentStoreDirectory) indexShard.remoteStore().directory();

        FilterDirectory remoteStoreDirectory = (FilterDirectory) indexShard.remoteStore().directory();
        FilterDirectory byteSizeCachingStoreDirectory = (FilterDirectory) remoteStoreDirectory.getDelegate();
        RemoteSegmentStoreDirectory remoteDirectory = (RemoteSegmentStoreDirectory) byteSizeCachingStoreDirectory.getDelegate();

        Map<String, StoreFileMetadata> metadataMap = null;


        // TODO: Need to figure out a way to pass this information for segment metadata from remote store.
        final Version version = indexShard.getSegmentInfosSnapshot().get().getCommitLuceneVersion();
        try {
            metadataMap = remoteDirectory.readLatestMetadataFile().entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey(),
                e -> new StoreFileMetadata(e.getValue().getOriginalFilename(), 0, e.getValue().getChecksum(), version, null)));
        } catch (IOException e) {
            logger.error("Error fetching checkpoint metadata from remote store {}", e);
            e.printStackTrace();
        }

        // TODO: GET current checkpoint from remote store.
        listener.onResponse(new CheckpointInfoResponse(checkpoint, metadataMap, null));
    }

    @Override
    public void getSegmentFiles(long replicationId, ReplicationCheckpoint checkpoint, List<StoreFileMetadata> filesToFetch, IndexShard indexShard, ActionListener<GetSegmentFilesResponse> listener) {
        try {
            logger.info("[ankikala] Source: syncing segments from remote store");
            indexShard.syncSegmentsFromRemoteSegmentStore(false, true);
        } catch (IOException e) {
            logger.error("[ankikala] Failed to sync segments {}", e);
            e.printStackTrace();
        }
        listener.onResponse(new GetSegmentFilesResponse(Collections.emptyList()));
    }

    @Override
    public String getDescription() {
        return null;
    }
}
