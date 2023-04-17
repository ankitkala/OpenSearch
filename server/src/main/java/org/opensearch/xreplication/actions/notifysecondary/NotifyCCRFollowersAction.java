/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.notifysecondary;

import org.opensearch.action.ActionListener;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.replication.ReplicationResponse;
import org.opensearch.action.support.replication.TransportReplicationAction;
import org.opensearch.cluster.action.shard.ShardStateAction;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.settings.Settings;
import org.opensearch.index.shard.IndexShard;
import org.opensearch.indices.IndicesService;
import org.opensearch.indices.replication.checkpoint.ReplicationCheckpoint;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;
import org.opensearch.xreplication.services.XReplicationLeaderService;

import java.io.IOException;

public class NotifyCCRFollowersAction extends TransportReplicationAction<
    NotifyCCRFollowersRequest,
    NotifyCCRFollowersRequest,
    ReplicationResponse> {

    private final XReplicationLeaderService leaderService;
    @Inject
    public NotifyCCRFollowersAction(Settings settings,
                                    String actionName,
                                    TransportService transportService,
                                    ClusterService clusterService,
                                    IndicesService indicesService,
                                    ThreadPool threadPool,
                                    ShardStateAction shardStateAction,
                                    ActionFilters actionFilters,
                                    XReplicationLeaderService leaderService) {
        super(settings, actionName, transportService, clusterService, indicesService, threadPool, shardStateAction,
            actionFilters, NotifyCCRFollowersRequest::new, NotifyCCRFollowersRequest::new, ThreadPool.Names.GENERIC);
        // initialize primary service.
        this.leaderService = leaderService;
    }

    @Override
    protected void doExecute(Task task, NotifyCCRFollowersRequest request, ActionListener<ReplicationResponse> listener) {
        assert false : "use NotifySecondariesAction#publish";
    }

    @Override
    protected ReplicationResponse newResponseInstance(StreamInput in) throws IOException {
        return new ReplicationResponse(in);
    }

    @Override
    protected void shardOperationOnPrimary(NotifyCCRFollowersRequest shardRequest, IndexShard primary, ActionListener<PrimaryResult<NotifyCCRFollowersRequest, ReplicationResponse>> listener) {
        ActionListener.completeWith(listener, () -> new PrimaryResult<>(shardRequest, new ReplicationResponse()));
    }

    @Override
    protected void shardOperationOnReplica(NotifyCCRFollowersRequest shardRequest, IndexShard replica, ActionListener<ReplicaResult> listener) {
        ActionListener.completeWith(listener, () -> new ReplicaResult());
    }

    public void publish(IndexShard indexShard, ReplicationCheckpoint checkpoint) {
        // Do stuff here.
        // Step 1: Get settings and fetch all the remotes.
        // Step 2: setup all clients.
        //Client secondaryClient = this.transportService.getRemoteClusterService().getRemoteClusterClient(ThreadPool.Names.SAME, "");
        leaderService.syncFollowerSegments(indexShard, checkpoint);
    }
}
