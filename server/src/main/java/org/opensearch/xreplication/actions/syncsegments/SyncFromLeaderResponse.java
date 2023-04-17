/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.syncsegments;

import org.opensearch.action.ActionResponse;
import org.opensearch.action.support.nodes.BaseNodesResponse;
import org.opensearch.action.support.replication.ReplicationResponse;
import org.opensearch.cluster.ClusterName;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.List;

public class SyncFromLeaderResponse extends BaseNodesResponse<SyncShardFromLeaderResponse> {
    protected SyncFromLeaderResponse(StreamInput in) throws IOException {
        super(in);
    }

    public SyncFromLeaderResponse(ClusterName clusterName, List nodes, List failures) {
        super(clusterName, nodes, failures);
    }

    @Override
    protected List<SyncShardFromLeaderResponse> readNodesFrom(StreamInput in) throws IOException {
        return in.readList(SyncShardFromLeaderResponse::new);
    }

    @Override
    protected void writeNodesTo(StreamOutput out, List<SyncShardFromLeaderResponse> nodes) throws IOException {
        out.writeList(nodes);
    }
}
