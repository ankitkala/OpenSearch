/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.cluster.routing.ShardRouting;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.Nullable;
import org.opensearch.common.settings.Settings;
import org.opensearch.index.shard.IndexEventListener;
import org.opensearch.index.shard.IndexShard;
import org.opensearch.index.shard.ShardId;
import org.opensearch.indices.recovery.RecoverySettings;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportChannel;
import org.opensearch.transport.TransportRequestHandler;
import org.opensearch.transport.TransportService;

public class XReplicationFollowerService implements IndexEventListener {
    private static final Logger logger = LogManager.getLogger(XReplicationFollowerService.class);

    private final ThreadPool threadPool;
    private final RecoverySettings recoverySettings;
    private final TransportService transportService;
    private final ClusterService clusterService;

    public static class Actions {
        public static final String SYNC_FROM_LEADER = "internal:index/shard/xreplication/follower/sync_from_leader";
    }

    public XReplicationFollowerService(
        final ThreadPool threadPool,
        final TransportService transportService,
        ClusterService clusterService,
        final RecoverySettings recoverySettings
    ) {
        this.threadPool = threadPool;
        this.recoverySettings = recoverySettings;
        this.transportService = transportService;
        this.clusterService = clusterService;

        /*
        transportService.registerRequestHandler(
            Actions.SYNC_FROM_LEADER,
            ThreadPool.Names.GENERIC,
            SyncFromLeaderRequest::new,
            new SyncFromLeaderRequestHandler()
        );
        */

        /*
        transportService.registerRequestHandler(
            Actions.SYNC_FROM_LEADER,
            ThreadPool.Names.GENERIC,
            FileChunkRequest::new,
            new SegmentReplicationTargetService.FileChunkTransportRequestHandler()
        );
        */
    }

    public static final XReplicationFollowerService NO_OP = new XReplicationFollowerService() {
        @Override
        public void beforeIndexShardClosed(ShardId shardId, IndexShard indexShard, Settings indexSettings) {
            // NoOp;
        }

        @Override
        public void shardRoutingChanged(IndexShard indexShard, @Nullable ShardRouting oldRouting, ShardRouting newRouting) {
            // noOp;
        }
    };

    private XReplicationFollowerService() {
        this.threadPool = null;
        this.recoverySettings = null;
        this.transportService = null;
        this.clusterService = null;
    }


    private class SyncFromLeaderRequestHandler implements TransportRequestHandler<SyncFromLeaderRequest> {
        @Override
        public void messageReceived(SyncFromLeaderRequest request, TransportChannel channel, Task task) throws Exception {

        }
    }
}
