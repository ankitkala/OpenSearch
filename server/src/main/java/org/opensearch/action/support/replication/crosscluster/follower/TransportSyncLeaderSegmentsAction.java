/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.support.replication.crosscluster.follower;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionListener;
import org.opensearch.action.admin.cluster.state.ClusterStateRequest;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.action.support.IndicesOptions;
import org.opensearch.client.Client;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.inject.Inject;
import org.opensearch.index.shard.IndexShard;
import org.opensearch.index.shard.ShardId;
import org.opensearch.indices.IndicesService;
import org.opensearch.indices.replication.SegmentReplicationTargetService;
import org.opensearch.indices.replication.checkpoint.ReplicationCheckpoint;
import org.opensearch.indices.replication.checkpoint.crosscluster.GetLeaderCheckpointRequest;
import org.opensearch.indices.replication.checkpoint.crosscluster.GetLeaderCheckpointResponse;
import org.opensearch.indices.replication.checkpoint.crosscluster.GetLeaderReplicationCheckpoint;
import org.opensearch.indices.replication.checkpoint.crosscluster.RemoteClusterConfig;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

/*
  Transport action which attempts to sync the follow shard's segments with Leader shard.
 */
public class TransportSyncLeaderSegmentsAction extends HandledTransportAction<SyncLeaderSegmentsRequest, SyncLeaderSegmentsResponse> {
    protected static Logger logger = LogManager.getLogger(TransportSyncLeaderSegmentsAction.class);

    private Client client;
    private IndicesService indicesService;
    private final SegmentReplicationTargetService replicationService;
    private final TransportService transportService;

    @Inject
    public TransportSyncLeaderSegmentsAction(ActionFilters actionFilters, Client client,
                                                IndicesService indicesService, TransportService transportService,
                                                SegmentReplicationTargetService replicationService) {
        super(SyncLeaderSegmentsAction.NAME, transportService, actionFilters, SyncLeaderSegmentsRequest::new);
        this.client = client;
        this.indicesService = indicesService;
        this.replicationService = replicationService;
        this.transportService = transportService;
    }

    @Override
    protected void doExecute(Task task, SyncLeaderSegmentsRequest request, ActionListener<SyncLeaderSegmentsResponse> listener) {
        // Fetch Replication checkpoint from leader.
        logger.info("Fetching replication checkpoint");
        IndexShard indexShard = indicesService.indexServiceSafe(request.getFollowerShardId().getIndex()).getShard(request.getFollowerShardId().id());
        Client remoteClient = client.getRemoteClusterClient(request.getLeaderAlias());
        GetLeaderCheckpointRequest getLeaderCheckpointRequest = new GetLeaderCheckpointRequest(request.getLeaderShardId());

        // 1. Get Leader checkpoint
        GetLeaderCheckpointResponse checkpoint = remoteClient.execute(GetLeaderReplicationCheckpoint.INSTANCE, getLeaderCheckpointRequest).actionGet();
        // 2. sync the follower shard(local) with leader's checkpoint(remote).
        syncFromLeader(checkpoint, indexShard, remoteClient, request.getLeaderShardId(), request.getLeaderAlias(), listener);
    }

    private void syncFromLeader(GetLeaderCheckpointResponse checkpointResponse, IndexShard indexShard,
                            Client remoteClient, ShardId leaderShardId, String leaderAlias, ActionListener listener) {
        ReplicationCheckpoint checkpoint = checkpointResponse.getReplicationCheckpoint();
        logger.info("Checking if shouldProcessCheckpoint");
        if (indexShard.shouldProcessCheckpoint(checkpoint)) {
            logger.info("processing checkpoint");

            DiscoveryNode targetNode = null;
            try {
                targetNode = getLeaderTargetNode(remoteClient, leaderShardId);
            } catch (ExecutionException | InterruptedException e) {
                logger.error("ankikala: Unable to find targetNode");
                e.printStackTrace();
                listener.onFailure(e);
            }
            logger.info("Calling onNewCheckpoint with {}", targetNode);
            RemoteClusterConfig remoteClusterConfig = new RemoteClusterConfig(remoteClient, targetNode, leaderAlias);

            // Trigger the replication event.
            replicationService.onNewCheckpoint(checkpoint, indexShard, Optional.of(remoteClusterConfig));
        }
        listener.onResponse(new SyncLeaderSegmentsResponse());
    }

    protected DiscoveryNode getLeaderTargetNode(Client remoteClient, ShardId remoteShardID) throws ExecutionException, InterruptedException {
        ClusterStateRequest request = remoteClient.admin().cluster().prepareState()
            .clear()
            .setIndices(remoteShardID.getIndexName())
            .setRoutingTable(true)
            .setNodes(true)
            .setIndicesOptions(IndicesOptions.STRICT_SINGLE_INDEX_NO_EXPAND_FORBID_CLOSED)
            .request();
        ClusterState state = remoteClient.admin().cluster().state(request).get().getState();
        String currentNodeId = state.getRoutingNodes().activePrimary(remoteShardID).currentNodeId();
        return state.nodes().get(currentNodeId);
    }

}
