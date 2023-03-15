/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication.xcluster;

import org.opensearch.action.support.replication.ReplicationResponse;
import org.opensearch.common.io.stream.StreamInput;

import java.io.IOException;

public class NotifySecondariesResponse extends ReplicationResponse {
    public NotifySecondariesResponse(StreamInput in) throws IOException {
        super(in);
    }

    public NotifySecondariesResponse() {

    }
}
