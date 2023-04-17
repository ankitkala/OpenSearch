/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.notifysecondary;

import org.opensearch.action.support.replication.ReplicationResponse;
import org.opensearch.common.io.stream.StreamInput;

import java.io.IOException;

public class NotifyCCRFollowersResponse extends ReplicationResponse {
    public NotifyCCRFollowersResponse(StreamInput in) throws IOException {
        super(in);
    }

    public NotifyCCRFollowersResponse() {

    }
}

