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
import org.opensearch.client.Client;
import org.opensearch.cluster.action.shard.ShardStateAction;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.common.settings.Settings;
import org.opensearch.index.shard.IndexShard;
import org.opensearch.indices.IndicesService;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;
import org.opensearch.xreplication.services.XReplicationLeaderService;

import java.io.IOException;

public class NotifySecondariesAction extends TransportReplicationAction<
    NotifySecondariesRequest,
    NotifySecondariesRequest,
    NotifySecondariesResponse> {

    private final XReplicationLeaderService leaderService;
    @Inject
    public NotifySecondariesAction(Settings settings,
                                   String actionName,
                                   TransportService transportService,
                                   ClusterService clusterService,
                                   IndicesService indicesService,
                                   ThreadPool threadPool,
                                   ShardStateAction shardStateAction,
                                   ActionFilters actionFilters,
                                   XReplicationLeaderService leaderService,
                                   Writeable.Reader<NotifySecondariesRequest> notifySecondariesRequestReader,
                                   Writeable.Reader<NotifySecondariesRequest> notifySecondariesRequestReader2,
                                   String executor) {
        super(settings, actionName, transportService, clusterService, indicesService, threadPool, shardStateAction,
            actionFilters, notifySecondariesRequestReader, notifySecondariesRequestReader2, executor);
        // initialize primary service.
        this.leaderService = leaderService;
    }

    @Override
    protected void doExecute(Task task, NotifySecondariesRequest request, ActionListener<NotifySecondariesResponse> listener) {
        assert false : "use NotifySecondariesAction#publish";
    }

    @Override
    protected NotifySecondariesResponse newResponseInstance(StreamInput in) throws IOException {
        return new NotifySecondariesResponse(in);
    }

    @Override
    protected void shardOperationOnPrimary(NotifySecondariesRequest shardRequest, IndexShard primary, ActionListener<PrimaryResult<NotifySecondariesRequest, NotifySecondariesResponse>> listener) {
        // Do stuff here.
    }

    @Override
    protected void shardOperationOnReplica(NotifySecondariesRequest shardRequest, IndexShard replica, ActionListener<ReplicaResult> listener) {
        ActionListener.completeWith(listener, () -> new PrimaryResult<>(shardRequest, new NotifySecondariesResponse()));
    }

    public void publish(IndexShard indexShard, String refreshedLocalFiles) {
        // Do stuff here.
        // Step 1: Get settings and fetch all the remotes.
        // Step 2: setup all clients.
        //Client secondaryClient = this.transportService.getRemoteClusterService().getRemoteClusterClient(ThreadPool.Names.SAME, "");
        leaderService.notifyAll(indexShard, refreshedLocalFiles);
    }
}
