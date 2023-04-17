/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.index;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;

import java.io.IOException;

public class StartCCRIndexTaskRequest extends ActionRequest implements ToXContent {

    private final String[] indices;

    public String[] getFollowerAliases() {
        return followerAliases;
    }

    private final String[] followerAliases;

    public StartCCRIndexTaskRequest(String[] indexName, String[] followerAliases) {
        this.indices = indexName;
        this.followerAliases = followerAliases;
    }

    public StartCCRIndexTaskRequest(StreamInput in) throws IOException {
        super(in);
        indices = in.readStringArray();
        followerAliases = in.readStringArray();
    }

    public String[] getIndices() {
        return indices;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return null;
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }
}
