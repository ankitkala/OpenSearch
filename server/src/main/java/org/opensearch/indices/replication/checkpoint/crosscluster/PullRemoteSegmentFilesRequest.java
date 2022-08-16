/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication.checkpoint.crosscluster;

import org.opensearch.client.Client;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.index.store.StoreFileMetadata;
import org.opensearch.indices.replication.GetSegmentFilesRequest;
import org.opensearch.indices.replication.checkpoint.ReplicationCheckpoint;
import org.opensearch.indices.replication.common.SegmentReplicationTransportRequest;
import org.opensearch.transport.RemoteClusterAwareRequest;
import org.opensearch.transport.TransportRequest;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class PullRemoteSegmentFilesRequest extends SegmentReplicationTransportRequest implements RemoteClusterAwareRequest {
    private GetSegmentFilesRequest request;
    // node with replica shard
    private final DiscoveryNode targetNode;
    // node with primary shard
    private final DiscoveryNode sourceNode;
    private Client client;

    @Override
    public DiscoveryNode getTargetNode() {
        return targetNode;
    }

    public Client getClient() {
        return client;
    }

    public Optional<String> getRemoteClusterAlias() {
        return remoteClusterAlias;
    }

    private Optional<String> remoteClusterAlias;

    public GetSegmentFilesRequest getRequest() {
        return request;
    }

    public DiscoveryNode getSourceNode() {
        return sourceNode;
    }

    public PullRemoteSegmentFilesRequest(long replicationId, String targetAllocationId, DiscoveryNode targetNode, List<StoreFileMetadata> filesToFetch, ReplicationCheckpoint checkpoint, DiscoveryNode sourceNode, Optional<String> remoteClusterAlias) {
        super(replicationId, targetAllocationId, targetNode);
        this.remoteClusterAlias = remoteClusterAlias;
        this.request = new GetSegmentFilesRequest(replicationId, targetAllocationId, targetNode, filesToFetch, checkpoint);
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
        this.remoteClusterAlias = remoteClusterAlias;
    }

    public PullRemoteSegmentFilesRequest(StreamInput in) throws IOException {
        super(in);
        this.request = new GetSegmentFilesRequest(in);
        this.sourceNode = new DiscoveryNode(in);
        this.targetNode = new DiscoveryNode(in);
        this.remoteClusterAlias = Optional.ofNullable(in.readOptionalString());
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        request.writeTo(out);
        sourceNode.writeTo(out);
        targetNode.writeTo(out);
        out.writeOptionalString(remoteClusterAlias.orElse(null));
    }

    @Override
    public DiscoveryNode getPreferredTargetNode() {
        return sourceNode;
    }
}
