/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication;

import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.index.store.StoreFileMetadata;
import org.opensearch.indices.replication.checkpoint.ReplicationCheckpoint;
import org.opensearch.indices.replication.common.SegmentReplicationTransportRequest;
import org.opensearch.transport.RemoteClusterAwareRequest;

import java.io.IOException;
import java.util.List;

/**
 * Request object for fetching a list of segment files metadata from a {@link SegmentReplicationSource}.
 * This object is created by the target node and sent to the source node.
 *
 * @opensearch.internal
 */
// Make request RemoteClusterAwareRequest so that it can be executed on the specified node on the leader cluster.
public class GetSegmentFilesRequest extends SegmentReplicationTransportRequest implements RemoteClusterAwareRequest {

    private final List<StoreFileMetadata> filesToFetch;
    private final ReplicationCheckpoint checkpoint;
    private final DiscoveryNode targetNode;

    public GetSegmentFilesRequest(StreamInput in) throws IOException {
        super(in);
        this.filesToFetch = in.readList(StoreFileMetadata::new);
        this.checkpoint = new ReplicationCheckpoint(in);
        targetNode = new DiscoveryNode(in);
    }

    public GetSegmentFilesRequest(
        long replicationId,
        String targetAllocationId,
        DiscoveryNode targetNode,
        List<StoreFileMetadata> filesToFetch,
        ReplicationCheckpoint checkpoint
    ) {
        super(replicationId, targetAllocationId, targetNode);
        this.filesToFetch = filesToFetch;
        this.checkpoint = checkpoint;
        this.targetNode = targetNode;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeList(filesToFetch);
        checkpoint.writeTo(out);
        targetNode.writeTo(out);
    }

    public ReplicationCheckpoint getCheckpoint() {
        return checkpoint;
    }

    public List<StoreFileMetadata> getFilesToFetch() {
        return filesToFetch;
    }

    @Override
    public DiscoveryNode getPreferredTargetNode() {
        return targetNode;
    }
}
