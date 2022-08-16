/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication.checkpoint.crosscluster;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.index.store.StoreFileMetadata;
import org.opensearch.indices.replication.common.SegmentReplicationTransportRequest;
import org.opensearch.transport.RemoteClusterAwareRequest;
import org.opensearch.transport.TransportRequest;

import java.io.IOException;

public class GetSegmentChunkRequest extends SegmentReplicationTransportRequest implements RemoteClusterAwareRequest {
    private final StoreFileMetadata file;
    private final DiscoveryNode targetNode;
    private final DiscoveryNode sourceNode;

    public GetSegmentChunkRequest(long replicationId, String targetAllocationId,StoreFileMetadata file, DiscoveryNode sourceNode, DiscoveryNode targetNode) {
        super(replicationId, targetAllocationId, targetNode);
        this.file = file;
        this.targetNode = targetNode;
        this.sourceNode = sourceNode;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        file.writeTo(out);
        targetNode.writeTo(out);
        sourceNode.writeTo(out);
    }

    public GetSegmentChunkRequest(StreamInput in) throws IOException {
        super(in);
        this.file = new StoreFileMetadata(in);
        this.targetNode = new DiscoveryNode(in);
        this.sourceNode = new DiscoveryNode(in);
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }


    public StoreFileMetadata getMetadataFilename() {
        return file;
    }

    @Override
    public DiscoveryNode getPreferredTargetNode() {
        return sourceNode;
    }
}
