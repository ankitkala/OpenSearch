/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.replication.xcluster;

import org.opensearch.action.support.replication.ReplicationRequest;
import org.opensearch.common.io.stream.StreamInput;

import java.io.IOException;

public class NotifySecondariesRequest extends ReplicationRequest<NotifySecondariesRequest> {
    public NotifySecondariesRequest(StreamInput in) throws IOException {
        super(in);
    }

    @Override
    public String toString() {
        return null;
    }
}
