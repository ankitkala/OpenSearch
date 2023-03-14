/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.task.follower;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.client.Client;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.persistent.AllocatedPersistentTask;
import org.opensearch.persistent.PersistentTaskState;
import org.opensearch.persistent.PersistentTasksCustomMetadata;
import org.opensearch.persistent.PersistentTasksExecutor;
import org.opensearch.tasks.TaskId;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.xreplication.services.XReplicationLeaderService;

import java.util.Map;

public class FollowerReplicationExecutor extends PersistentTasksExecutor<FollowerReplicationParams> {
    public static final String NAME = "cluster:indices/admin/replication_follower";
    private final Logger logger = LogManager.getLogger(FollowerReplicationExecutor.class);
    private ClusterService clusterService;
    private ThreadPool threadPool;
    private Client client;
    private XReplicationLeaderService leaderService;
    public FollowerReplicationExecutor(ClusterService clusterService, ThreadPool threadPool, Client client, XReplicationLeaderService leaderService) {
        // TODO:  Use a dedicated threadpool for FCR
        super(NAME, ThreadPool.Names.WRITE);
        this.clusterService = clusterService;
        this.threadPool = threadPool;
        this.client = client;
        this.leaderService = leaderService;
    }

    @Override
    public void validate(FollowerReplicationParams params, ClusterState clusterState) {
        super.validate(params, clusterState);
    }

    @Override
    protected void nodeOperation(AllocatedPersistentTask task, FollowerReplicationParams params, PersistentTaskState state) {
        logger.info("Executing");
        if (task instanceof FollowerReplicationTask) ((FollowerReplicationTask) task).execute();
        else {
            task.markAsFailed(new IllegalArgumentException(String.format("Unknown task class %s", task.getClass())));
        }
    }

    @Override
    protected AllocatedPersistentTask createTask(long id, String type, String action, TaskId parentTaskId, PersistentTasksCustomMetadata.PersistentTask<FollowerReplicationParams> taskInProgress, Map<String, String> headers) {
        return new FollowerReplicationTask(id, type, action, parentTaskId, taskInProgress, headers, leaderService);
    }
}
