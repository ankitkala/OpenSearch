/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.task.supervisor;

import org.opensearch.Version;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.persistent.PersistentTaskParams;

import java.io.IOException;

public class SupervisorReplicationParams implements PersistentTaskParams {
    private String[] followerAliases;

    public String[] getFollowerAliases() {
        return followerAliases;
    }

    public SupervisorReplicationParams(String[] followerAliases) {
        this.followerAliases = followerAliases;
    }

    public static Writeable.Reader<SupervisorReplicationParams> READER = in -> new SupervisorReplicationParams(in);

    public SupervisorReplicationParams(StreamInput in) throws IOException {
        this(in.readStringArray());
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().field("followerAliases", followerAliases).endObject();
    }

    @Override
    public String getWriteableName() {
        return SupervisorReplicationExecutor.NAME;
    }

    @Override
    public Version getMinimalSupportedVersion() {
        return Version.V_2_0_0;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeStringArray(followerAliases);
    }
}
