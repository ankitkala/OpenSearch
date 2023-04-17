/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.followers;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;

import java.io.IOException;

public class StartCCRFollowerTaskRequest extends ActionRequest implements ToXContent {
    public static String FOLLOWER_ALIASES = "follower_aliases";

    private final String[] followerAliases;

    public StartCCRFollowerTaskRequest(String[] followers) {
        this.followerAliases = followers;
    }

    public StartCCRFollowerTaskRequest(StreamInput in) throws IOException {
        super(in);
        followerAliases = in.readStringArray();
    }

    public String[] getFollowerAliases() {
        return followerAliases;
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
