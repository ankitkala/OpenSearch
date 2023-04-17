/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.task.follower;

import org.opensearch.Version;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.persistent.PersistentTaskParams;
import org.opensearch.xreplication.task.index.IndexReplicationParams;

import java.io.IOException;

public class FollowerReplicationParams implements PersistentTaskParams {
    private String follower_alias;

    public String getFollower_alias() {
        return follower_alias;
    }

    public FollowerReplicationParams(String follower_alias) {
        this.follower_alias = follower_alias;
    }

    public static Writeable.Reader<FollowerReplicationParams> READER = in -> new FollowerReplicationParams(in);

    public FollowerReplicationParams(StreamInput in) throws IOException {
        this(in.readString());
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().field("follower_alias", follower_alias).endObject();
    }

    @Override
    public String getWriteableName() {
        return FollowerReplicationExecutor.NAME;
    }

    @Override
    public Version getMinimalSupportedVersion() {
        return Version.V_2_0_0;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(follower_alias);
    }
}
