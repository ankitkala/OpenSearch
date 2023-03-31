/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.syncsegments;

import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.action.support.single.shard.SingleShardRequest;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.index.shard.ShardId;

import java.io.IOException;

public class SyncFromLeaderRequest extends SingleShardRequest<SyncFromLeaderRequest> {
    ShardId shardId;

    public SyncFromLeaderRequest(ShardId shardId) {
        super(shardId.getIndexName());
        this.shardId = shardId;
    }

    public SyncFromLeaderRequest(StreamInput in) throws IOException {
        super(in);
        this.shardId = new ShardId(in);
    }

    public ShardId getShardId() {
        return shardId;
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }
}
