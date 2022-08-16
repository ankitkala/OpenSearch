/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication;

import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionListener;
import org.opensearch.client.Client;
import org.opensearch.indices.recovery.MultiChunkTransfer;
import org.opensearch.indices.recovery.RecoverySettings;
import org.opensearch.indices.recovery.RetryableTransportClient;
import org.opensearch.indices.replication.checkpoint.crosscluster.PullRemoteSegmentFilesRequest;
import org.opensearch.indices.replication.checkpoint.crosscluster.RemoteClusterMultiChunkTransfer;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

import java.util.concurrent.atomic.AtomicLong;

public class PullRemoteSegmentHandler {
    private MultiChunkTransfer transfer;

    private PullRemoteSegmentHandler(MultiChunkTransfer transfer) {
        this.transfer = transfer;
    }

    public static PullRemoteSegmentHandler create(ThreadPool threadPool, RecoverySettings recoverySettings, TransportService transportService, Client client, PullRemoteSegmentFilesRequest request, Logger logger, ActionListener listener) {
        final RemoteSegmentFileChunkWriter segmentSegmentFileChunkWriter = new RemoteSegmentFileChunkWriter(
            request.getRequest().getReplicationId(),
            recoverySettings,
            new RetryableTransportClient(
                transportService,
                request.getTargetNode(),
                //request.getRequest().getTargetNode(),
                recoverySettings.internalActionRetryTimeout(),
                logger
            ),
            request.getRequest().getCheckpoint().getShardId(),
            SegmentReplicationTargetService.Actions.FILE_CHUNK,
            new AtomicLong(0),
            (throttleTime) -> {}
        );

        // TODO: Add a setting for concurrent fetches.
        MultiChunkTransfer transfer = new RemoteClusterMultiChunkTransfer(logger, threadPool.getThreadContext(), listener, 5, request, segmentSegmentFileChunkWriter, client);
        return new PullRemoteSegmentHandler(transfer);

    }
    public void start() {
        transfer.start();
    }
}
