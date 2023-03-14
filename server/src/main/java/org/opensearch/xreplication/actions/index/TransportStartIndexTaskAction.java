/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.index;

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
import org.opensearch.xreplication.task.index.IndexReplicationExecutor;
import org.opensearch.xreplication.task.index.IndexReplicationParams;

import java.util.Arrays;

public class TransportStartIndexTaskAction extends HandledTransportAction<StartIndexTaskRequest, StartIndexTaskResponse> {
    protected Logger logger = LogManager.getLogger(getClass());
    private PersistentTasksService persistentTasksService;
    private ClusterService clusterService;

    @Inject
    public TransportStartIndexTaskAction(ClusterService clusterService, TransportService transportService, PersistentTasksService persistentTasksService, ActionFilters actionFilters, String executor) {
        super(StartIndexTaskAction.NAME, transportService, actionFilters, in -> new StartIndexTaskRequest(in), executor);
        this.clusterService = clusterService;
        this.persistentTasksService = persistentTasksService;
    }

    @Override
    protected void doExecute(Task task, StartIndexTaskRequest request, ActionListener<StartIndexTaskResponse> listener) {
        String[] follower_aliases = request.getFollowerAliases();
        //TODO: Wait for all tasks to be up and respond with success/failures.
        for (String indexName: request.getIndices()) {
            startIndexTask(indexName, follower_aliases);
        }
    }


    private void startIndexTask(String indexName, String[] follower_aliases) {
        IndexReplicationParams params = new IndexReplicationParams(indexName, follower_aliases);
        logger.info("[ankikala] Executing the index start {}: {}", indexName, Arrays.toString(follower_aliases));
        persistentTasksService.sendStartRequest("index:" + indexName, IndexReplicationExecutor.NAME,
            params, new ActionListener<PersistentTasksCustomMetadata.PersistentTask<PersistentTaskParams>>() {
                @Override
                public void onResponse(PersistentTasksCustomMetadata.PersistentTask<PersistentTaskParams> persistentTaskParamsPersistentTask) {
                    long taskID = persistentTaskParamsPersistentTask.getAllocationId();
                    logger.info("Created task {}", taskID);
                    //listener.onResponse(new StartIndexTaskResponse(RestStatus.OK));
                }

                @Override
                public void onFailure(Exception e) {
                    logger.info("Failed to create index task for {}: {}",indexName, e);
                    e.printStackTrace();
                    //listener.onFailure(e);
                }
            });
    }
}
