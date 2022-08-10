/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication.checkpoint.crosscluster;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionListener;
import org.opensearch.action.ActionType;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.single.shard.TransportSingleShardAction;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.routing.ShardsIterator;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.index.shard.IndexShard;
import org.opensearch.index.shard.ShardId;
import org.opensearch.indices.IndicesService;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

import java.io.IOException;

public class GetLeaderReplicationCheckpoint extends TransportSingleShardAction<GetLeaderCheckpointRequest, GetLeaderCheckpointResponse> {
    public static final String ACTION_NAME = "indices:admin/fetchLeaderCheckpoint";
    public static final ActionType<GetLeaderCheckpointResponse> INSTANCE = new ActionType(ACTION_NAME, GetLeaderCheckpointResponse::new) {};

    protected static Logger logger = LogManager.getLogger(GetLeaderReplicationCheckpoint.class);

    private final IndicesService indicesService;

    @Inject
    public GetLeaderReplicationCheckpoint(
        ClusterService clusterService,
        TransportService transportService,
        IndicesService indicesService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver
    ) {
        super(
            ACTION_NAME,
            threadPool,
            clusterService,
            transportService,
            actionFilters,
            indexNameExpressionResolver,
            GetLeaderCheckpointRequest::new,
            ThreadPool.Names.MANAGEMENT
        );
        this.indicesService = indicesService;
    }

    @Override
    protected GetLeaderCheckpointResponse shardOperation(GetLeaderCheckpointRequest request, ShardId shardId) throws IOException {
        return null;
    }

    @Override
    protected void asyncShardOperation(GetLeaderCheckpointRequest request, ShardId shardId, ActionListener<GetLeaderCheckpointResponse> listener) {
        IndexShard indexShard = indicesService.indexServiceSafe(shardId.getIndex()).getShard(shardId.id());
        listener.onResponse(new GetLeaderCheckpointResponse(indexShard.getLatestReplicationCheckpoint()));
    }

    @Override
    protected Writeable.Reader<GetLeaderCheckpointResponse> getResponseReader() {
        return GetLeaderCheckpointResponse::new;
    }

    @Override
    protected boolean resolveIndex(GetLeaderCheckpointRequest request) {
        // Since this action is invoked from remote clusters, always resolve the index.
        return true;
    }

    @Override
    protected ShardsIterator shards(ClusterState state, TransportSingleShardAction<GetLeaderCheckpointRequest, GetLeaderCheckpointResponse>.InternalRequest request) {
        // TODO: Try loadbalancing to replica shards as well.
        return state.routingTable().shardRoutingTable(request.request().getShardID()).primaryShardIt();
    }
}
