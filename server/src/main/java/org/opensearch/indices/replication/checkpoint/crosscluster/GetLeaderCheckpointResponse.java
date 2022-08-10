/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication.checkpoint.crosscluster;

import org.opensearch.action.ActionResponse;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.indices.replication.checkpoint.ReplicationCheckpoint;

import java.io.IOException;

public class GetLeaderCheckpointResponse extends ActionResponse {
    private ReplicationCheckpoint replicationCheckpoint;

    public GetLeaderCheckpointResponse(ReplicationCheckpoint replicationCheckpoint) {
        this.replicationCheckpoint = replicationCheckpoint;
    }

    public GetLeaderCheckpointResponse(StreamInput in) throws IOException {
        this.replicationCheckpoint = new ReplicationCheckpoint(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        replicationCheckpoint.writeTo(out);
    }

    public ReplicationCheckpoint getReplicationCheckpoint() {
        return replicationCheckpoint;
    }
}
