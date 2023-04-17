/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.shard;

import org.opensearch.common.inject.Inject;
import org.opensearch.indices.replication.checkpoint.ReplicationCheckpoint;
import org.opensearch.indices.replication.checkpoint.SegmentReplicationCheckpointPublisher;
import org.opensearch.xreplication.actions.notifysecondary.NotifyCCRFollowersAction;

/**
 * Hook to publish notification after primary uploads segments to the remote store.
 *
 * @opensearch.internal
 */
public class RemoteStoreSegmentUploadNotificationPublisher {
    private final SegmentReplicationCheckpointPublisher segRepPublisher;
    private final NotifyCCRFollowersAction xReplicatePublisher;

    @Inject
    public RemoteStoreSegmentUploadNotificationPublisher(SegmentReplicationCheckpointPublisher segRepPublisher, NotifyCCRFollowersAction xReplicatePublisher) {
        this.segRepPublisher = segRepPublisher;
        this.xReplicatePublisher = xReplicatePublisher;
    }

    // Notify replicas and CCR followers after the segments have been uploaded to the remote store by primary(during refresh).
    public void notifySegmentUpload(IndexShard indexShard, ReplicationCheckpoint checkpoint) {
        // we don't call indexShard.getLatestReplicationCheckpoint() as it might have a newer refreshed checkpoint.
        // Instead we send the one which has been uploaded to remote store.
        // TODO: Parallise both the notifications.
        if (segRepPublisher != null) segRepPublisher.publish(indexShard, checkpoint);
        if (xReplicatePublisher != null) xReplicatePublisher.publish(indexShard, checkpoint);
    }

    public static final RemoteStoreSegmentUploadNotificationPublisher EMPTY = new RemoteStoreSegmentUploadNotificationPublisher(null, null);
}
