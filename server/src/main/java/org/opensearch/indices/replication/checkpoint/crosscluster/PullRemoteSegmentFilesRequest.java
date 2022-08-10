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
import org.opensearch.transport.TransportRequest;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class PullRemoteSegmentFilesRequest extends TransportRequest {
    private GetSegmentFilesRequest request;
    private DiscoveryNode currentNode;
    private Client client;

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

    public DiscoveryNode getCurrentNode() {
        return currentNode;
    }

    public PullRemoteSegmentFilesRequest(long replicationId, String targetAllocationId, DiscoveryNode targetNode, List<StoreFileMetadata> filesToFetch, ReplicationCheckpoint checkpoint, DiscoveryNode currentNode, Optional<String> remoteClusterAlias) {
        this.remoteClusterAlias = remoteClusterAlias;
        this.request = new GetSegmentFilesRequest(replicationId, targetAllocationId, targetNode, filesToFetch, checkpoint);
        this.currentNode = currentNode;
        this.remoteClusterAlias = remoteClusterAlias;
    }

    public PullRemoteSegmentFilesRequest(StreamInput in) throws IOException {
        super(in);
        this.request = new GetSegmentFilesRequest(in);
        this.currentNode = new DiscoveryNode(in);
        this.remoteClusterAlias = Optional.ofNullable(in.readOptionalString());
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        request.writeTo(out);
        currentNode.writeTo(out);
        out.writeOptionalString(remoteClusterAlias.orElse(null));
    }
}
