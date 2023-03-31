/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.syncsegments;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.single.shard.TransportSingleShardAction;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.routing.ShardsIterator;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.index.IndexService;
import org.opensearch.index.shard.IndexShard;
import org.opensearch.index.shard.ShardId;
import org.opensearch.indices.IndicesService;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

import java.io.IOException;

public class TransportSyncFromLeaderAction extends TransportSingleShardAction<SyncFromLeaderRequest, SyncFromLeaderResponse> {

    private final TransportService transportService;
    private final IndicesService indicesService;
    protected Logger logger = LogManager.getLogger(getClass());
    private ClusterService clusterService;

    @Inject
    public TransportSyncFromLeaderAction(ThreadPool threadPool, ClusterService clusterService, TransportService transportService,
                                         IndicesService indicesService, ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                                         Writeable.Reader<SyncFromLeaderRequest> request) {
        super(SyncFromLeaderAction.NAME, threadPool, clusterService, transportService, actionFilters,
            indexNameExpressionResolver, request, ThreadPool.Names.WRITE);
        this.clusterService = clusterService;
        this.transportService = transportService;
        this.indicesService = indicesService;
    }

    @Override
    protected SyncFromLeaderResponse shardOperation(SyncFromLeaderRequest request, ShardId shardId) throws IOException {
        //TODO: Assert FCR, SegRep, Remote Store, etc.
        logger.info("Executing sync on follower");
        IndexShard shard = indicesService.getShardOrNull(shardId);
        if(shard == null) {
            logger.error("[ankikala] ShardID is null");
        }
        shard.syncSegmentsFromRemoteSegmentStore(false);
        return new SyncFromLeaderResponse();
    }

    @Override
    protected Writeable.Reader<SyncFromLeaderResponse> getResponseReader() {
        return SyncFromLeaderResponse::new;
    }

    @Override
    protected boolean resolveIndex(SyncFromLeaderRequest request) {
        return true;
    }

    @Override
    protected ShardsIterator shards(ClusterState state, TransportSingleShardAction<SyncFromLeaderRequest, SyncFromLeaderResponse>.InternalRequest request) {
        return state.routingTable().shardRoutingTable(request.request().shardId).primaryShardIt();
    }
}
