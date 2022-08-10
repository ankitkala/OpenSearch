/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication.checkpoint.crosscluster;

import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.action.support.single.shard.SingleShardRequest;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.index.shard.ShardId;

import java.io.IOException;

public class GetLeaderCheckpointRequest extends SingleShardRequest<GetLeaderCheckpointRequest> {
    private ShardId shardId;

    public GetLeaderCheckpointRequest(ShardId shardId) {
        super(shardId.getIndexName());
        this.shardId = shardId;
    }

    public GetLeaderCheckpointRequest(StreamInput in) throws IOException {
        super(in);
        this.shardId = new ShardId(in);
    }

    @Override
    public ActionRequestValidationException validate() {
        return super.validateNonNullIndex();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        shardId.writeTo(out);
    }

    public ShardId getShardID() {
        return this.shardId;
    }
}
