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
import org.opensearch.xreplication.utils.ReplicationHelper;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;



public class TransportStartFollowerAction extends HandledTransportAction<StartFollowersRequest, StartFollowerResponse> {
    protected Logger logger = LogManager.getLogger(getClass());
    private PersistentTasksService persistentTasksService;
    private ClusterService clusterService;

    @Inject
    public TransportStartFollowerAction(ClusterService clusterService, TransportService transportService, PersistentTasksService persistentTasksService, ActionFilters actionFilters, String executor) {
        super(StartFollowersAction.NAME, transportService, actionFilters, in -> new StartFollowersRequest(in), executor);
        this.clusterService = clusterService;
        this.persistentTasksService = persistentTasksService;
    }

    @Override
    protected void doExecute(Task task, StartFollowersRequest request, ActionListener<StartFollowerResponse> listener) {
        // Update data in cluster state?
        // start bootstrapping for each follower?
        // logger
        List<String> remoteClusterSeeds = ReplicationHelper.getAllClusterAliases(clusterService.getSettings());
        //TODO: Make it work for multiple followers.
        String followerAlias = request.getFollowerAliases()[0];
        if(remoteClusterSeeds.contains(remoteClusterSeeds) || true) {
            FollowerReplicationParams params = new FollowerReplicationParams(followerAlias);
            logger.info("[ankikala] Executing the follower start {}", Arrays.toString(request.getFollowerAliases()));
            persistentTasksService.sendStartRequest("follower:" + followerAlias, FollowerReplicationExecutor.NAME, params, new ActionListener<PersistentTasksCustomMetadata.PersistentTask<PersistentTaskParams>>() {
                @Override
                public void onResponse(PersistentTasksCustomMetadata.PersistentTask<PersistentTaskParams> persistentTaskParamsPersistentTask) {
                    long taskID = persistentTaskParamsPersistentTask.getAllocationId();
                    logger.info("Created task {}", taskID);
                    listener.onResponse(new StartFollowerResponse(RestStatus.OK));
                }

                @Override
                public void onFailure(Exception e) {
                    logger.info("Failed to create follower task for {}: {}",followerAlias, e);
                    e.printStackTrace();
                    listener.onFailure(e);
                }
            });
        }

    }
}
