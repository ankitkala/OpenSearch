/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.task.follower;

import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.persistent.PersistentTaskState;

import java.io.IOException;

public class FollowerReplicationState implements PersistentTaskState {
    enum State {
        NOT_STARTED, BOOTSTRAPPING, SYNCING, PAUSED, FAILED
    }
    private State state;

    public static Writeable.Reader<FollowerReplicationState> READER =
        in -> new FollowerReplicationState(in.readEnum(State.class));

    public FollowerReplicationState(State state) {
        this.state = state;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().field("state", state).endObject();
    }

    @Override
    public String getWriteableName() {
        return FollowerReplicationExecutor.NAME;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeEnum(state);
    }
}
