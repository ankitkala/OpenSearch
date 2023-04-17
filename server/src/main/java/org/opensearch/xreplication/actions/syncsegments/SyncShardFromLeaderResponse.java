/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.syncsegments;

import org.opensearch.action.support.nodes.BaseNodeResponse;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.io.stream.StreamInput;

import java.io.IOException;

public class SyncShardFromLeaderResponse extends BaseNodeResponse {
    protected SyncShardFromLeaderResponse(StreamInput in) throws IOException {
        super(in);
    }

    public SyncShardFromLeaderResponse(DiscoveryNode discoveryNode) {
        super(discoveryNode);
    }
}
