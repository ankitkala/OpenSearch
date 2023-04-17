/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.followers;

import org.opensearch.action.ActionResponse;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.xcontent.StatusToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.rest.RestStatus;

import java.io.IOException;

public class StartCCRFollowerTaskResponse extends ActionResponse implements StatusToXContentObject {
    private RestStatus status;
    public StartCCRFollowerTaskResponse(RestStatus status) {
        this.status = status;
    }

    public StartCCRFollowerTaskResponse(StreamInput in) throws IOException {
        super(in);
        this.status = RestStatus.readFrom(in);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("status", status);
        builder.endObject();
        return builder;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeEnum(status);
    }

    @Override
    public RestStatus status() {
        return status;
    }
}
