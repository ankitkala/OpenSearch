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
import org.opensearch.action.ActionListener;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.inject.Inject;
import org.opensearch.persistent.PersistentTaskParams;
import org.opensearch.persistent.PersistentTasksCustomMetadata;
import org.opensearch.persistent.PersistentTasksService;
import org.opensearch.rest.RestStatus;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;
import org.opensearch.xreplication.task.supervisor.SupervisorReplicationExecutor;
import org.opensearch.xreplication.task.supervisor.SupervisorReplicationParams;


/**
 * Transport action to start the full cluster replication.
 */
public class TransportStartCCRAction extends HandledTransportAction<StartCCRRequest, StartCCRResponse> {
    protected Logger logger = LogManager.getLogger(getClass());
    private PersistentTasksService persistentTasksService;
    private ClusterService clusterService;

    @Inject
    public TransportStartCCRAction(ClusterService clusterService, TransportService transportService, PersistentTasksService persistentTasksService, ActionFilters actionFilters, String executor) {
        super(StartCCRAction.NAME, transportService, actionFilters, in -> new StartCCRRequest(in), executor);
        this.clusterService = clusterService;
        this.persistentTasksService = persistentTasksService;

    }

    @Override
    protected void doExecute(Task task, StartCCRRequest request, ActionListener<StartCCRResponse> listener) {
        SupervisorReplicationParams params = new SupervisorReplicationParams(request.getFollowerAliases());
        persistentTasksService.sendStartRequest("FCRSupervisor", SupervisorReplicationExecutor.NAME, params, new ActionListener<PersistentTasksCustomMetadata.PersistentTask<PersistentTaskParams>>() {
            @Override
            public void onResponse(PersistentTasksCustomMetadata.PersistentTask<PersistentTaskParams> persistentTaskParamsPersistentTask) {
                persistentTaskParamsPersistentTask.getAllocationId();
                listener.onResponse(new StartCCRResponse(RestStatus.OK));
            }

            @Override
            public void onFailure(Exception e) {
                logger.error("Failed to create Replication supervisor: {}", e);
                listener.onFailure(e);
            }
        });
    }
}
