/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication.xcluster;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.client.Client;
import org.opensearch.cluster.routing.ShardRouting;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.Nullable;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.Settings;
import org.opensearch.index.shard.IndexEventListener;
import org.opensearch.index.shard.IndexShard;
import org.opensearch.index.shard.ShardId;
import org.opensearch.indices.recovery.RecoverySettings;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.RemoteClusterService;
import org.opensearch.transport.SniffConnectionStrategy;
import org.opensearch.transport.TransportService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.opensearch.transport.SniffConnectionStrategy.REMOTE_CLUSTER_SEEDS;

public class XReplicationLeaderService implements IndexEventListener {
    private static final Logger logger = LogManager.getLogger(XReplicationLeaderService.class);

    private final ThreadPool threadPool;
    private final RecoverySettings recoverySettings;
    private final TransportService transportService;
    private final ClusterService clusterService;
    private final RemoteClusterService remoteService;
    private Map<String, Client> followerClients;

    public XReplicationLeaderService(
        final ThreadPool threadPool,
        final TransportService transportService,
        final ClusterService clusterService,
        final RecoverySettings recoverySettings

    ) {
        this.threadPool = threadPool;
        this.recoverySettings = recoverySettings;
        this.transportService = transportService;
        this.clusterService = clusterService;
        this.remoteService = transportService.getRemoteClusterService();

        /*
        transportService.registerRequestHandler(
            SegmentReplicationTargetService.Actions.FILE_CHUNK,
            ThreadPool.Names.GENERIC,
            FileChunkRequest::new,
            new SegmentReplicationTargetService.FileChunkTransportRequestHandler()
        );
        */

        followerClients = new HashMap<>();
        initializeFollowerClients();
        listenForFollowerUpdates();
    }

    private void listenForFollowerUpdates() {
        clusterService.getClusterSettings().addSettingsUpdateConsumer(REMOTE_CLUSTER_SEEDS, strings -> {
            strings.stream().filter(alias -> !followerClients.containsKey(alias)).forEach(alias -> bootstrapFollower(alias));
        });
    }

    private void bootstrapFollower(String followerAlias) {
        /*
            1. Create all follower indices with metadata on follower.
            2. Remote store settings?
            3. Add to clients on this node.
            4. Add follower to metadata?
         */
        return;
    }

    private void initializeFollowerClients() {
        REMOTE_CLUSTER_SEEDS.getNamespaces(clusterService.getSettings()).stream()
            .filter(alias -> !followerClients.containsKey(alias))
            .map(alias ->
                followerClients.put(alias, remoteService.getRemoteClusterClient(threadPool, alias)));
    }


    public void notifyAll(IndexShard indexShard, String refreshedLocalFiles) {
        initializeFollowerClients();
        for (Map.Entry<String,Client> follower : followerClients.entrySet()) {
            logger.info("Notifying {}", follower.getKey());
            // invoke replication.
            //follower.getValue().execute();
        }
    }


    public static final XReplicationLeaderService NO_OP = new XReplicationLeaderService() {
        @Override
        public void beforeIndexShardClosed(ShardId shardId, IndexShard indexShard, Settings indexSettings) {
            // NoOp;
        }

        @Override
        public void shardRoutingChanged(IndexShard indexShard, @Nullable ShardRouting oldRouting, ShardRouting newRouting) {
            // noOp;
        }
    };
    private XReplicationLeaderService() {
        this.threadPool = null;
        this.recoverySettings = null;
        this.transportService = null;
        this.clusterService = null;
        this.remoteService = null;
    }
}
