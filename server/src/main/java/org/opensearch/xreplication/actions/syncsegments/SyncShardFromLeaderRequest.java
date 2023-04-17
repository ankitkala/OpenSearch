/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.syncsegments;

import org.opensearch.action.support.nodes.BaseNodeRequest;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.index.shard.ShardId;

import java.io.IOException;

public class SyncShardFromLeaderRequest extends BaseNodeRequest {
    private ShardId shardId;
    public SyncShardFromLeaderRequest(ShardId shardId) {
        super();
        this.shardId = shardId;
    }

    public SyncShardFromLeaderRequest(StreamInput streamInput) throws IOException {
        super(streamInput);
        this.shardId = new ShardId(streamInput);
    }

    public ShardId getShardId() {
        return shardId;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        shardId.writeTo(out);
    }
}
