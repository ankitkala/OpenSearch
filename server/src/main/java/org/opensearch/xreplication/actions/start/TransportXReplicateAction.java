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
import org.opensearch.xreplication.utils.ReplicationHelper;

import java.util.Arrays;
import java.util.List;



public class TransportXReplicateAction extends HandledTransportAction<StartXReplicationRequest, StartXReplicationResponse> {
    protected Logger logger = LogManager.getLogger(getClass());
    private PersistentTasksService persistentTasksService;
    private ClusterService clusterService;

    @Inject
    public TransportXReplicateAction(ClusterService clusterService, TransportService transportService, PersistentTasksService persistentTasksService, ActionFilters actionFilters, String executor) {
        super(StartXReplication.NAME, transportService, actionFilters, in -> new StartXReplicationRequest(in), executor);
        this.clusterService = clusterService;
        this.persistentTasksService = persistentTasksService;

    }

    @Override
    protected void doExecute(Task task, StartXReplicationRequest request, ActionListener<StartXReplicationResponse> listener) {
        // Update data in cluster state?
        // start bootstrapping for each follower?
        // logger

        SupervisorReplicationParams params = new SupervisorReplicationParams(request.getFollowerAliases());
        logger.info("[ankikala] Executing the rest request {}", Arrays.toString(params.getFollowerAliases()));
        persistentTasksService.sendStartRequest("FCRSupervisor", SupervisorReplicationExecutor.NAME, params, new ActionListener<PersistentTasksCustomMetadata.PersistentTask<PersistentTaskParams>>() {
            @Override
            public void onResponse(PersistentTasksCustomMetadata.PersistentTask<PersistentTaskParams> persistentTaskParamsPersistentTask) {
                long taskID = persistentTaskParamsPersistentTask.getAllocationId();
                logger.info("Created task {}", taskID);
                listener.onResponse(new StartXReplicationResponse(RestStatus.OK));
            }

            @Override
            public void onFailure(Exception e) {
                logger.info("Failed to create Replication supervisor: {}", e);
                e.printStackTrace();
                listener.onFailure(e);
            }
        });
    }
}
