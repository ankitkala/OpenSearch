/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.rest.action.xreplication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionListener;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

public class TransportXReplicateAction extends HandledTransportAction<StartXReplicationRequest, StartXReplicationResponse> {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    public TransportXReplicateAction(String actionName, TransportService transportService, ActionFilters actionFilters, Writeable.Reader<StartXReplicationRequest> startXReplicationRequestReader, String executor) {
        super(actionName, transportService, actionFilters, startXReplicationRequestReader, executor);
    }

    @Override
    protected void doExecute(Task task, StartXReplicationRequest request, ActionListener<StartXReplicationResponse> listener) {
        // Update data in cluster state?
        // start bootstrapping for each follower?
        // logger

    }
}
