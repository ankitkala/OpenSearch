/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.services;

import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.tasks.Task;
import org.opensearch.tasks.TaskId;
import org.opensearch.transport.TransportRequest;

import java.io.IOException;
import java.util.Map;

public class SyncFromLeaderRequest extends TransportRequest {
    public SyncFromLeaderRequest() {
    }

    public SyncFromLeaderRequest(StreamInput in) throws IOException {
        super(in);
    }

    @Override
    public void setParentTask(String parentTaskNode, long parentTaskId) {
        super.setParentTask(parentTaskNode, parentTaskId);
    }

    @Override
    public Task createTask(long id, String type, String action, TaskId parentTaskId, Map<String, String> headers) {
        return super.createTask(id, type, action, parentTaskId, headers);
    }

    @Override
    public String getDescription() {
        return super.getDescription();
    }
}
