/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.task.index;


import org.opensearch.Version;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.persistent.PersistentTaskParams;

import java.io.IOException;

public class IndexReplicationParams implements PersistentTaskParams {
    private String indexName;

    public String[] getFollowers() {
        return followers;
    }

    private String[] followers;

    public String getIndexName() {
        return indexName;
    }
    public static Writeable.Reader<IndexReplicationParams> READER = in -> new IndexReplicationParams(in);
    public IndexReplicationParams(String indexName, String[] followers) {
        this.indexName = indexName;
        this.followers = followers;
    }

    public IndexReplicationParams(StreamInput in) throws IOException {
        this(in.readString(), in.readStringArray());
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().field("indexName", indexName).field("followers", followers).endObject();
    }

    @Override
    public String getWriteableName() {
        return IndexReplicationExecutor.NAME;
    }

    @Override
    public Version getMinimalSupportedVersion() {
        return Version.V_2_0_0;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(indexName);
        out.writeStringArray(followers);
    }
}
