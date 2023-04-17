/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.syncsegments;

import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.action.support.nodes.BaseNodesRequest;
import org.opensearch.action.support.replication.ReplicationRequest;
import org.opensearch.action.support.single.shard.SingleShardRequest;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.index.shard.ShardId;

import java.io.IOException;

public class SyncFromLeaderRequest extends BaseNodesRequest<SyncFromLeaderRequest> {
    private ShardId shardId;
    public SyncFromLeaderRequest(StreamInput in) throws IOException {
        super(in);
        this.shardId = new ShardId(in);
    }

    public SyncFromLeaderRequest(ShardId shardId, String... nodesIds) {
        super(nodesIds);
        this.shardId = shardId;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        shardId.writeTo(out);
    }

    public ShardId getShardId() {
        return shardId;
    }

    //ShardId shardId;

    /*
    public SyncFromLeaderRequest(StreamInput in) throws IOException {
        super(in);
    }

    public SyncFromLeaderRequest(ShardId shardId) {
        super(shardId);
        //this.shardId = shardId;
    }

    public ShardId getShardId() {
        return shardId;
    }


    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        //shardId.writeTo(out);
    }

    @Override
    public String toString() {
        return "CCR SyncFromLeaderRequest for shardID " + shardId.toString();
    }

     */
}
