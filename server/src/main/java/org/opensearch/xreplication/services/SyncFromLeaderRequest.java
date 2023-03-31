/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.services;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.index.shard.ShardId;
import org.opensearch.tasks.Task;
import org.opensearch.tasks.TaskId;
import org.opensearch.transport.TransportRequest;

import java.io.IOException;
import java.util.Map;

public class SyncFromLeaderRequest extends ActionRequest {
    private final ShardId shardId;

    public ShardId getShardId() {
        return shardId;
    }

    public SyncFromLeaderRequest(ShardId shardId) {
        this.shardId = shardId;
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

}
