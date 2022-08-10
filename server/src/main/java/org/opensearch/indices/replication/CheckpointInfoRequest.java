/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication;

import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.indices.replication.checkpoint.ReplicationCheckpoint;
import org.opensearch.indices.replication.common.SegmentReplicationTransportRequest;
import org.opensearch.transport.RemoteClusterAware;
import org.opensearch.transport.RemoteClusterAwareRequest;

import java.io.IOException;

/**
 * Request object for fetching segment metadata for a {@link ReplicationCheckpoint} from
 * a {@link SegmentReplicationSource}. This object is created by the target node and sent
 * to the source node.
 *
 * @opensearch.internal
 */
// Make request RemoteClusterAwareRequest so that it can be executed on the specified node on the leader cluster.
public class CheckpointInfoRequest extends SegmentReplicationTransportRequest implements RemoteClusterAwareRequest {

    private final ReplicationCheckpoint checkpoint;
    private final DiscoveryNode targetNode;
    private final Boolean isRemote;

    public Boolean getRemote() {
        return isRemote;
    }

    public CheckpointInfoRequest(StreamInput in) throws IOException {
        super(in);
        checkpoint = new ReplicationCheckpoint(in);
        targetNode = new DiscoveryNode(in);
        isRemote = in.readBoolean();
    }

    public CheckpointInfoRequest(
        long replicationId,
        String targetAllocationId,
        DiscoveryNode targetNode,
        ReplicationCheckpoint checkpoint
    ) {
        this(replicationId, targetAllocationId, targetNode, false, checkpoint);
    }

    public CheckpointInfoRequest(
        long replicationId,
        String targetAllocationId,
        DiscoveryNode targetNode,
        Boolean isRemote,
        ReplicationCheckpoint checkpoint
    ) {
        super(replicationId, targetAllocationId, targetNode);
        this.checkpoint = checkpoint;
        this.targetNode = targetNode;
        this.isRemote = isRemote;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        checkpoint.writeTo(out);
        targetNode.writeTo(out);
        out.writeBoolean(isRemote);
    }

    public ReplicationCheckpoint getCheckpoint() {
        return checkpoint;
    }

    @Override
    public DiscoveryNode getPreferredTargetNode() {
        return targetNode;
    }

}
