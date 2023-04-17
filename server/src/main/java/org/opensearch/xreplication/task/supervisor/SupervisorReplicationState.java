/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.task.supervisor;

import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.persistent.PersistentTaskState;

import java.io.IOException;

public class SupervisorReplicationState implements PersistentTaskState {
    enum State {
        NOT_STARTED, SYNCING, PAUSED, FAILED
    }
    private State state;

    public static Writeable.Reader<SupervisorReplicationState> READER =
        in -> new SupervisorReplicationState(in.readEnum(State.class));

    public SupervisorReplicationState(State state) {
        this.state = state;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().field("state", state).endObject();
    }

    @Override
    public String getWriteableName() {
        return SupervisorReplicationExecutor.NAME;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeEnum(state);
    }
}
