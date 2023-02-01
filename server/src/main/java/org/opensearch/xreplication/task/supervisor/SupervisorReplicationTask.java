/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.task.supervisor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.admin.indices.get.GetIndexRequest;
import org.opensearch.client.Client;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.persistent.AllocatedPersistentTask;
import org.opensearch.persistent.PersistentTasksCustomMetadata;
import org.opensearch.tasks.TaskId;
import org.opensearch.xreplication.actions.followers.StartFollowersAction;
import org.opensearch.xreplication.actions.followers.StartFollowersRequest;
import org.opensearch.xreplication.actions.index.StartIndexTaskAction;
import org.opensearch.xreplication.actions.index.StartIndexTaskRequest;
import org.opensearch.xreplication.services.XReplicationLeaderService;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class SupervisorReplicationTask extends AllocatedPersistentTask {
    private ClusterService clusterService;
    private Client client;
    private final Logger logger = LogManager.getLogger(SupervisorReplicationTask.class);
    String[] followers;
    public SupervisorReplicationTask(long id, String type, String action, TaskId parentTaskId,
                                     PersistentTasksCustomMetadata.PersistentTask<SupervisorReplicationParams> taskInProgress,
                                     Map<String, String> headers, ClusterService clusterService, Client client, XReplicationLeaderService leaderService) {
        super(id, type, action, String.format("FCR:IndexReplicationTask: %s", taskInProgress.getParams().getFollowerAliases().toString()), parentTaskId, headers);
        this.client = client;
        this.clusterService = clusterService;
        this.followers = taskInProgress.getParams().getFollowerAliases();
    }

    public void execute() {
        logger.info("[ankikala] Supervisor task is up, followers: {}", Arrays.toString(followers));
        createFollowerTasks(followers);

        String[] indices = Arrays.stream(client.admin().indices().getIndex(new GetIndexRequest()).actionGet().indices())
            .filter(i -> !i.startsWith(".")).collect(Collectors.toList()).toArray(String[]::new);
        createIndexTasks(indices);
    }

    private void createIndexTasks(String[] indices) {
        logger.info("creating index tasks {}: {}", Arrays.toString(indices), Arrays.toString(followers));
        StartIndexTaskRequest startIndexTaskRequest = new StartIndexTaskRequest(indices, followers);
        client.execute(StartIndexTaskAction.INSTANCE, startIndexTaskRequest);
    }

    private void createFollowerTasks(String[] followers) {
        logger.info("creating followers {}", Arrays.toString(followers));
        StartFollowersRequest startFollowersRequest = new StartFollowersRequest(followers);
        client.execute(StartFollowersAction.INSTANCE, startFollowersRequest);
    }
}
