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
import org.opensearch.action.FailedNodeException;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.nodes.TransportNodesAction;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.index.shard.IndexShard;
import org.opensearch.indices.IndicesService;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.List;

public class TransportSyncFromLeaderAction extends TransportNodesAction<SyncFromLeaderRequest, SyncFromLeaderResponse, SyncShardFromLeaderRequest, SyncShardFromLeaderResponse> {
    protected Logger logger = LogManager.getLogger(getClass());
    private IndicesService indicesService;
    @Inject
    public TransportSyncFromLeaderAction(IndicesService indicesService, ThreadPool threadPool, ClusterService clusterService, TransportService transportService,
                                         ActionFilters actionFilters) {
        super(SyncFromLeaderAction.NAME, threadPool, clusterService, transportService, actionFilters, SyncFromLeaderRequest::new, SyncShardFromLeaderRequest::new, ThreadPool.Names.WRITE, ThreadPool.Names.WRITE, SyncShardFromLeaderResponse.class);
        this.indicesService = indicesService;
    }

    @Override
    protected SyncFromLeaderResponse newResponse(SyncFromLeaderRequest request, List<SyncShardFromLeaderResponse> syncShardFromLeaderResponses, List<FailedNodeException> failures) {
        return new SyncFromLeaderResponse(clusterService.getClusterName(), syncShardFromLeaderResponses, failures);
    }

    @Override
    protected SyncShardFromLeaderRequest newNodeRequest(SyncFromLeaderRequest request) {
        return new SyncShardFromLeaderRequest(request.getShardId());
    }

    @Override
    protected SyncShardFromLeaderResponse newNodeResponse(StreamInput in) throws IOException {
        return new SyncShardFromLeaderResponse(in);
    }

    @Override
    protected SyncShardFromLeaderResponse nodeOperation(SyncShardFromLeaderRequest request) {
        // Do here.
        logger.info("invoked sync on this node for {}", request.getShardId());
        IndexShard shard = indicesService.getShardOrNull(request.getShardId());
        if(shard == null) {
            logger.error("[ankikala] ShardID is null");
        }
        try {
            shard.syncSegmentsFromRemoteSegmentStore(false, true, false);
        } catch (IOException e) {
            logger.error("Unable to sync segments on clusterService.localNode()");
        }
        return new SyncShardFromLeaderResponse(clusterService.localNode());
    }
}
