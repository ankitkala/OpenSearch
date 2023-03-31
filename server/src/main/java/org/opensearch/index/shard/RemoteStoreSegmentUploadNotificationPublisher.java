/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.shard;

import org.opensearch.common.inject.Inject;
import org.opensearch.indices.replication.checkpoint.SegmentReplicationCheckpointPublisher;


public class RemoteStoreSegmentUploadNotificationPublisher {
    private final SegmentReplicationCheckpointPublisher segRepPublisher;
    @Inject
    public RemoteStoreSegmentUploadNotificationPublisher(SegmentReplicationCheckpointPublisher segRepPublisher) {
        this.segRepPublisher = segRepPublisher;
    }

    public void notifySegmentUpload(IndexShard indexShard) {
        // Separate publisher for CCR.
        if (segRepPublisher != null) segRepPublisher.publish(indexShard, indexShard.getLatestReplicationCheckpoint());
    }
}
