/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication.xcluster;

import org.opensearch.action.ActionListener;
import org.opensearch.action.support.ActionFilters;
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

import java.io.IOException;

public class SyncFromLeaderAction extends TransportReplicationAction<
    SyncFromLeaderRequest,
    SyncFromLeaderRequest,
    SyncFromLeaderResponse> {

    private final XReplicationFollowerService followerService;
    @Inject
    public SyncFromLeaderAction(Settings settings,
                                   String actionName,
                                   TransportService transportService,
                                   ClusterService clusterService,
                                   IndicesService indicesService,
                                   ThreadPool threadPool,
                                   ShardStateAction shardStateAction,
                                   ActionFilters actionFilters,
                                   XReplicationFollowerService followerService,
                                   Writeable.Reader<SyncFromLeaderRequest> reader,
                                   String executor) {
        super(settings, actionName, transportService, clusterService, indicesService, threadPool, shardStateAction,
            actionFilters, reader, reader, executor);
        // initialize primary service.
        this.followerService = followerService;
    }

    @Override
    protected void doExecute(Task task, SyncFromLeaderRequest request, ActionListener<SyncFromLeaderResponse> listener) {
        assert false : "use NotifySecondariesAction#publish";
    }

    @Override
    protected SyncFromLeaderResponse newResponseInstance(StreamInput in) throws IOException {
        return new SyncFromLeaderResponse(in);
    }

    @Override
    protected void shardOperationOnPrimary(SyncFromLeaderRequest shardRequest, IndexShard primary, ActionListener<PrimaryResult<SyncFromLeaderRequest, SyncFromLeaderResponse>> listener) {

    }

    @Override
    protected void shardOperationOnReplica(SyncFromLeaderRequest shardRequest, IndexShard replica, ActionListener<ReplicaResult> listener) {
        //ActionListener.completeWith(listener, () -> new PrimaryResult<>(shardRequest, new NotifySecondariesResponse()));
    }
}
