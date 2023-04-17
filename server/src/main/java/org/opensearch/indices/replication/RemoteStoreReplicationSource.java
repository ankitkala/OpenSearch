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

/**
 * Implementation of a {@link SegmentReplicationSource} where the source is remote store.
 *
 * @opensearch.internal
 */
public class RemoteStoreReplicationSource implements SegmentReplicationSource {

    private static final Logger logger = LogManager.getLogger(PrimaryShardReplicationSource.class);

    private final IndexShard indexShard;

    public RemoteStoreReplicationSource(IndexShard indexShard) {
        this.indexShard = indexShard;
    }

    @Override
    public void getCheckpointMetadata(long replicationId, ReplicationCheckpoint checkpoint, ActionListener<CheckpointInfoResponse> listener) {
        FilterDirectory remoteStoreDirectory = (FilterDirectory) indexShard.remoteStore().directory();
        FilterDirectory byteSizeCachingStoreDirectory = (FilterDirectory) remoteStoreDirectory.getDelegate();
        RemoteSegmentStoreDirectory remoteDirectory = (RemoteSegmentStoreDirectory) byteSizeCachingStoreDirectory.getDelegate();

        Map<String, StoreFileMetadata> metadataMap = null;
        // TODO: Need to figure out a way to pass this information for segment metadata via remote store.
        final Version version = indexShard.getSegmentInfosSnapshot().get().getCommitLuceneVersion();
        try {
            metadataMap = remoteDirectory.readLatestMetadataFile().entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey(),
                e -> new StoreFileMetadata(e.getValue().getOriginalFilename(), e.getValue().getLength(),
                    Store.digestToString(Long.valueOf(e.getValue().getChecksum())), version, null)));
        } catch (IOException e) {
            logger.error("Error fetching checkpoint metadata from remote store {}", e);
            e.printStackTrace();
        }
        // TODO: GET current checkpoint from remote store.
        listener.onResponse(new CheckpointInfoResponse(checkpoint, metadataMap, null));
    }

    @Override
    public void getSegmentFiles(long replicationId, ReplicationCheckpoint checkpoint, List<StoreFileMetadata> filesToFetch, IndexShard indexShard, ActionListener<GetSegmentFilesResponse> listener) {
        // have multiple retries in case we run into a race condition.
        int max_attempts = 3;
        while (max_attempts-- > 0) {
            try {
                indexShard.syncSegmentsFromRemoteSegmentStore(false, true, false);
                listener.onResponse(new GetSegmentFilesResponse(Collections.emptyList()));
                return;
            } catch (Exception e) {
                logger.error("Failed to sync segments {}", e);
                if (max_attempts == 0) {
                    listener.onFailure(e);
                    return;
                }
            }
        }
        listener.onFailure(new Exception("Unable to sync segments. You shouldn't get this exception ideally"));
    }



    @Override
    public String getDescription() {
        return null;
    }
}
