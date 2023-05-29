/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication;

import org.apache.lucene.codecs.Codec;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.store.FilterDirectory;
import org.apache.lucene.util.Version;
import org.opensearch.action.ActionListener;
import org.opensearch.action.support.PlainActionFuture;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.common.concurrent.GatedCloseable;
import org.opensearch.common.settings.Settings;
import org.opensearch.index.engine.InternalEngineFactory;
import org.opensearch.index.shard.IndexShard;
import org.opensearch.index.shard.IndexShardTestCase;
import org.opensearch.index.shard.RemoteStoreRefreshListenerTests;
import org.opensearch.index.store.RemoteSegmentStoreDirectory;
import org.opensearch.index.store.Store;
import org.opensearch.indices.replication.checkpoint.ReplicationCheckpoint;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RemoteStoreReplicationSourceTests extends IndexShardTestCase {

    private static final long PRIMARY_TERM = 1L;
    private static final long SEGMENTS_GEN = 2L;
    private static final long VERSION = 4L;
    private static final long REPLICATION_ID = 123L;
    private RemoteStoreReplicationSource replicationSource;
    private IndexShard indexShard;

    private IndexShard mockShard;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        indexShard = newStartedShard(
            true,
            Settings.builder().put(IndexMetadata.SETTING_REMOTE_STORE_ENABLED, true).build(),
            new InternalEngineFactory()
        );

        // mock shard
        mockShard = mock(IndexShard.class);
        Store store = mock(Store.class);
        when(mockShard.store()).thenReturn(store);
        when(store.directory()).thenReturn(indexShard.store().directory());
        Store remoteStore = mock(Store.class);
        when(mockShard.remoteStore()).thenReturn(remoteStore);
        RemoteSegmentStoreDirectory remoteSegmentStoreDirectory =
            (RemoteSegmentStoreDirectory) ((FilterDirectory) ((FilterDirectory) indexShard.remoteStore().directory()).getDelegate())
                .getDelegate();
        FilterDirectory remoteStoreFilterDirectory = new RemoteStoreRefreshListenerTests.TestFilterDirectory(
            new RemoteStoreRefreshListenerTests.TestFilterDirectory(remoteSegmentStoreDirectory)
        );
        when(remoteStore.directory()).thenReturn(remoteStoreFilterDirectory);

        replicationSource = new RemoteStoreReplicationSource(mockShard);
        when(mockShard.getSegmentInfosSnapshot()).thenReturn(new GatedCloseable<>(new SegmentInfos(Version.LATEST.major), () -> {}));
    }

    @Override
    public void tearDown() throws Exception {
        closeShards(indexShard);
        super.tearDown();
    }

    public void testGetCheckpointMetadata() {
        final ReplicationCheckpoint checkpoint = new ReplicationCheckpoint(
            indexShard.shardId(),
            PRIMARY_TERM,
            SEGMENTS_GEN,
            VERSION,
            Codec.getDefault().getName()
        );

        final PlainActionFuture<CheckpointInfoResponse> res = PlainActionFuture.newFuture();
        replicationSource.getCheckpointMetadata(
            REPLICATION_ID,
            checkpoint,
            ActionListener.wrap(r -> res.onResponse(r), e -> new AssertionError("getCheckpointMetadata execution failed", e))
        );

        try {
            CheckpointInfoResponse response = res.get();
            assert (response.getCheckpoint().equals(checkpoint));
            assert (response.getMetadataMap().isEmpty());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

    }

    public void testGetSegmentFiles() {
        final ReplicationCheckpoint checkpoint = new ReplicationCheckpoint(
            indexShard.shardId(),
            PRIMARY_TERM,
            SEGMENTS_GEN,
            VERSION,
            Codec.getDefault().getName()
        );
        final PlainActionFuture<GetSegmentFilesResponse> res = PlainActionFuture.newFuture();
        replicationSource.getSegmentFiles(
            REPLICATION_ID,
            checkpoint,
            Collections.emptyList(),
            indexShard,
            ActionListener.wrap(r -> res.onResponse(r), e -> new AssertionError("testGetSegmentFiles execution failed", e))
        );
        try {
            GetSegmentFilesResponse response = res.get();
            assert (response.files.isEmpty());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
