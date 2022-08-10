/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.support.replication.crosscluster.follower;

import org.opensearch.action.ActionResponse;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;

import java.io.IOException;

public class SyncLeaderSegmentsResponse extends ActionResponse {
    // Empty response for now.
    // TODO: Add attributes to the response for providing better visibility to the caller.
    public SyncLeaderSegmentsResponse() {

    }

    public SyncLeaderSegmentsResponse(StreamInput streamInput) {

    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {

    }
}
