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
import org.opensearch.xreplication.actions.notifysecondary.NotifySecondariesAction;


public class RemoteStoreSegmentUploadNotificationPublisher {
    private final NotifySecondariesAction xReplicatePublisher;
    private final SegmentReplicationCheckpointPublisher segRepPublisher;
    @Inject
    public RemoteStoreSegmentUploadNotificationPublisher(NotifySecondariesAction xReplicatePublisher, SegmentReplicationCheckpointPublisher segRepPublisher) {
        this.xReplicatePublisher = xReplicatePublisher;
        this.segRepPublisher = segRepPublisher;
    }

    public void notifySegmentUpload(IndexShard indexShard, String refreshedLocalFiles) {
        if (xReplicatePublisher != null) xReplicatePublisher.publish(indexShard, refreshedLocalFiles);
        if (segRepPublisher != null) segRepPublisher.publish(indexShard, indexShard.getLatestReplicationCheckpoint());
    }
}
