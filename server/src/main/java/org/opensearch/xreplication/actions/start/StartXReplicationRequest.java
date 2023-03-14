/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.start;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.common.Strings;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.xcontent.ToXContent;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.rest.RestRequest;

import java.io.IOException;

public class StartXReplicationRequest extends ActionRequest implements ToXContent {
    public static String FOLLOWER_ALIASES = "follower_aliases";

    private final String[] followerAliases;

    public StartXReplicationRequest(RestRequest restRequest) {
        this(Strings.splitStringByCommaToArray(restRequest.param(FOLLOWER_ALIASES)));
    }
    public StartXReplicationRequest(String[] followers) {
        this.followerAliases = followers;
    }

    public StartXReplicationRequest(StreamInput in) throws IOException {
        super(in);
        this.followerAliases = in.readStringArray();
    }

    public String[] getFollowerAliases() {
        return followerAliases;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().field("follower_alias", followerAliases).endObject();
    }

    @Override
    public ActionRequestValidationException validate() {
       // TODO: Enable the validation
        /*
        if (this.followerAliases.length == 0) {
            ActionRequestValidationException e = new ActionRequestValidationException();
            e.addValidationError("Missing follower_aliases");
            throw e;
        }
        */
        return null;
    }
}
