/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.start;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.client.node.NodeClient;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestStatusToXContentListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.opensearch.rest.RestRequest.Method.POST;

public class RestXReplicateAction extends BaseRestHandler {
    protected Logger logger = LogManager.getLogger(getClass());
    @Override
    public List<Route> routes() {
        return unmodifiableList(
            asList(
                //new Route(GET, "/_fcr"),
                new Route(POST, "/_fcr")
            )
        );
    }

    @Override
    public String getName() {
        return "start_x_replicate_action";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        logger.info("ankikala: starting replication {}", request.params());
        logger.info("ankikala: starting replication {}", request.param("follower_aliases"));
        //TODO: consume the param from rest request. currently its null due to some issue. Needs debugging.
        StartXReplicationRequest startXReplicationRequest = new StartXReplicationRequest(Arrays.asList("remote-cluster").toArray(String[]::new));
        logger.info("ankikala: starting replication {}", Arrays.toString(startXReplicationRequest.getFollowerAliases()));
        return channel -> client.execute(StartXReplication.INSTANCE, startXReplicationRequest, new RestStatusToXContentListener<>(channel));
    }
}
