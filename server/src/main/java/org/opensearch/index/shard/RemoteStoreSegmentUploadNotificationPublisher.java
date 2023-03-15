/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.shard;

import org.opensearch.common.inject.Inject;
import org.opensearch.xreplication.actions.notifysecondary.NotifySecondariesAction;


public class RemoteStoreSegmentUploadNotificationPublisher {
    private final NotifySecondariesAction xReplicatePublisher;
    @Inject
    public RemoteStoreSegmentUploadNotificationPublisher(NotifySecondariesAction xReplicatePublisher) {
        this.xReplicatePublisher = xReplicatePublisher;
    }

    public void notifySegmentUpload(IndexShard indexShard, String refreshedLocalFiles) {
        if (xReplicatePublisher == null) return;
        // Invoke checkpoint publisher/follower notify
        xReplicatePublisher.publish(indexShard, refreshedLocalFiles);
    }
}
