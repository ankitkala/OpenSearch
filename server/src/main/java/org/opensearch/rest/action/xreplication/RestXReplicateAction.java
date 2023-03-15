/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.rest.action.xreplication;

import org.opensearch.client.node.NodeClient;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.opensearch.rest.RestRequest.Method.GET;
import static org.opensearch.rest.RestRequest.Method.POST;

public class RestXReplicateAction extends BaseRestHandler {
    @Override
    public List<Route> routes() {
        return unmodifiableList(
            asList(
                new Route(GET, "/_search"),
                new Route(POST, "/_search"),
                new Route(GET, "/{index}/_search"),
                new Route(POST, "/{index}/_search")
            )
        );
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        return null;
    }


}
