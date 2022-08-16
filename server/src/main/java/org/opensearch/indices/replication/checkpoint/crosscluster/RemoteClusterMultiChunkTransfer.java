/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication.checkpoint.crosscluster;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionFuture;
import org.opensearch.action.ActionListener;
import org.opensearch.action.support.replication.crosscluster.follower.TransportSyncLeaderSegmentsAction;
import org.opensearch.client.Client;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.index.store.StoreFileMetadata;
import org.opensearch.indices.recovery.FileChunkWriter;
import org.opensearch.indices.recovery.MultiChunkTransfer;
import org.opensearch.indices.replication.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class RemoteClusterMultiChunkTransfer extends MultiChunkTransfer<StoreFileMetadata, GetSegmentChunkResponse> {
    protected static Logger logger = LogManager.getLogger(RemoteClusterMultiChunkTransfer.class);
    private FileChunkWriter writer;
    private Client remoteClient;
    private DiscoveryNode sourceNode;
    private DiscoveryNode targetNode;

    private long replicationId;
    private String targetAllocationId;
    //Optional<String> remoteClusterAlias;
    public RemoteClusterMultiChunkTransfer(Logger logger, ThreadContext threadContext, ActionListener<Void> listener,
                                           int maxConcurrentChunks, PullRemoteSegmentFilesRequest request,
                                           RemoteSegmentFileChunkWriter writer, Client client) {
        super(logger, threadContext, listener, maxConcurrentChunks, request.getRequest().getFilesToFetch());
        this.writer = writer;
        this.remoteClient = request.getRemoteClusterAlias().map(a -> client.getRemoteClusterClient(a)).orElse(client);
        this.targetNode = request.getTargetNode();
        this.sourceNode = request.getSourceNode();
        this.replicationId = request.getReplicationId();
        this.targetAllocationId = request.getTargetAllocationId();
    }

    @Override
    protected GetSegmentChunkResponse nextChunkRequest(StoreFileMetadata resource) {
        // Fetch chunk from remote cluster.
        logger.info("fetching segment {}", resource);
        final GetSegmentChunkRequest request = new GetSegmentChunkRequest(replicationId, targetAllocationId, resource, sourceNode, targetNode);
        ActionFuture<GetSegmentChunkResponse> future = remoteClient.execute(SegmentReplicationSourceService.GetSegmentChunkAction.INSTANCE, request);
        GetSegmentChunkResponse response = future.actionGet();
        logger.info("Got the segment {}", resource);
        return response;
    }

    @Override
    protected void executeChunkRequest(GetSegmentChunkResponse request, ActionListener<Void> listener) {
        logger.info("executing the chunk request");
        writer.writeFileChunk(
            request.getMd(),
            request.getPosition(),
            request.getContent(),
            request.isLastChunk(),
            0,
            listener
        );
    }

    @Override
    protected void handleError(StoreFileMetadata resource, Exception e) throws Exception {

    }

    @Override
    public void close() throws IOException {

    }
}
