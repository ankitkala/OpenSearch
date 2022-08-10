/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.support.replication.crosscluster.follower;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.index.shard.ShardId;

import java.io.IOException;

public class SyncLeaderSegmentsRequest extends ActionRequest {
    private String leaderAlias;
    private ShardId leaderShardId;
    private ShardId followerShardId;

    public SyncLeaderSegmentsRequest(String leaderAlias, ShardId leaderShardId, ShardId followerShardId) {
        this.leaderAlias = leaderAlias;
        this.leaderShardId = leaderShardId;
        this.followerShardId = followerShardId;
    }

    public ShardId getLeaderShardId() {
        return leaderShardId;
    }

    public ShardId getFollowerShardId() {
        return followerShardId;
    }

    public SyncLeaderSegmentsRequest(StreamInput in) throws IOException {
        super(in);
        this.leaderAlias = in.readString();
        this.leaderShardId = new ShardId(in);
        this.followerShardId = new ShardId(in);
    }

    @Override
    public ActionRequestValidationException validate() {
        //TODO: Add validation here.
        return null;
    }

    public String getLeaderAlias() {
        return this.leaderAlias;
    }


    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(leaderAlias);
        leaderShardId.writeTo(out);
        followerShardId.writeTo(out);
    }
}
