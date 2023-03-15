/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication;

import org.opensearch.common.inject.Inject;
import org.opensearch.index.shard.IndexShard;
import org.opensearch.indices.replication.xcluster.NotifySecondariesAction;

public class RemoteStoreSegmentUploadNotificationPublisher {
    private final NotifySecondariesAction xReplicatePublisher;
    @Inject
    public RemoteStoreSegmentUploadNotificationPublisher(NotifySecondariesAction xReplicatePublisher) {
        this.xReplicatePublisher = xReplicatePublisher;
    }

    public void notifySegmentUpload(IndexShard indexShard, String refreshedLocalFiles) {
        // Invoke checkpoint publisher/follower notify
        xReplicatePublisher.publish(indexShard, refreshedLocalFiles);
    }
}
