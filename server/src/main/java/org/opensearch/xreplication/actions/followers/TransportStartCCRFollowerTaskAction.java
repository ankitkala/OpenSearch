/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.followers;

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
import org.opensearch.xreplication.task.follower.FollowerReplicationExecutor;
import org.opensearch.xreplication.task.follower.FollowerReplicationParams;


import java.util.Set;


public class TransportStartCCRFollowerTaskAction extends HandledTransportAction<StartCCRFollowerTaskRequest, StartCCRFollowerTaskResponse> {
    private final TransportService transportService;
    protected Logger logger = LogManager.getLogger(getClass());
    private PersistentTasksService persistentTasksService;
    private ClusterService clusterService;

    @Inject
    public TransportStartCCRFollowerTaskAction(ClusterService clusterService, TransportService transportService, PersistentTasksService persistentTasksService, ActionFilters actionFilters, String executor) {
        super(StartCCRFollowerTaskAction.NAME, transportService, actionFilters, in -> new StartCCRFollowerTaskRequest(in), executor);
        this.clusterService = clusterService;
        this.persistentTasksService = persistentTasksService;
        this.transportService = transportService;
    }

    @Override
    protected void doExecute(Task task, StartCCRFollowerTaskRequest request, ActionListener<StartCCRFollowerTaskResponse> listener) {
        // Update data in cluster state?
        // start bootstrapping for each follower?
        // logger
        Set<String> remoteClusterSeeds = transportService.getRemoteClusterService().getRegisteredRemoteClusterNames();
        //TODO: Make it work for multiple followers.
        String followerAlias = request.getFollowerAliases()[0];
        if(remoteClusterSeeds.contains(remoteClusterSeeds)) {
            FollowerReplicationParams params = new FollowerReplicationParams(followerAlias);
            persistentTasksService.sendStartRequest("follower:" + followerAlias, FollowerReplicationExecutor.NAME, params, new ActionListener<PersistentTasksCustomMetadata.PersistentTask<PersistentTaskParams>>() {
                @Override
                public void onResponse(PersistentTasksCustomMetadata.PersistentTask<PersistentTaskParams> persistentTaskParamsPersistentTask) {
                    persistentTaskParamsPersistentTask.getAllocationId();
                    listener.onResponse(new StartCCRFollowerTaskResponse(RestStatus.OK));
                }

                @Override
                public void onFailure(Exception e) {
                    logger.error("Failed to create follower task for {}: {}",followerAlias, e);
                    listener.onFailure(e);
                }
            });
        }

    }
}
