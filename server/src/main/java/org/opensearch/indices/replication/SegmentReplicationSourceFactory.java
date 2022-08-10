/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication;

import org.opensearch.client.Client;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.cluster.routing.ShardRouting;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.index.shard.IndexShard;
import org.opensearch.index.shard.ShardId;
import org.opensearch.indices.recovery.RecoverySettings;
import org.opensearch.indices.replication.checkpoint.crosscluster.LeaderClusterSegmentReplicationSource;
import org.opensearch.indices.replication.checkpoint.crosscluster.RemoteClusterConfig;
import org.opensearch.transport.TransportService;

import java.util.Optional;

/**
 * Factory to build {@link SegmentReplicationSource} used by {@link SegmentReplicationTargetService}.
 *
 * @opensearch.internal
 */
public class SegmentReplicationSourceFactory {

    private TransportService transportService;
    private RecoverySettings recoverySettings;
    private ClusterService clusterService;
    private Client client;

    public SegmentReplicationSourceFactory(
        TransportService transportService,
        RecoverySettings recoverySettings,
        ClusterService clusterService,
        Client client
        ) {
        this.transportService = transportService;
        this.recoverySettings = recoverySettings;
        this.clusterService = clusterService;
        this.client = client;
    }
    public SegmentReplicationSource get(IndexShard shard) {
        return get(shard, Optional.empty());
    }
    public SegmentReplicationSource get(IndexShard shard, Optional<RemoteClusterConfig> remoteClusterConfig) {
        if(remoteClusterConfig.isEmpty()) {
            return new PrimaryShardReplicationSource(
                clusterService.localNode(),
                shard.routingEntry().allocationId().getId(),
                transportService,
                recoverySettings,
                getPrimaryNode(shard.shardId())
            );
        } else {
            // TODO: Update the source for remote cluster
            return new LeaderClusterSegmentReplicationSource(
                clusterService.localNode(),
                shard.routingEntry().allocationId().getId(),
                remoteClusterConfig.get(),
                recoverySettings,
                transportService
            );
        }
    }

    private DiscoveryNode getPrimaryNode(ShardId shardId) {
        ShardRouting primaryShard = clusterService.state().routingTable().shardRoutingTable(shardId).primaryShard();
        return clusterService.state().nodes().get(primaryShard.currentNodeId());
    }
}
