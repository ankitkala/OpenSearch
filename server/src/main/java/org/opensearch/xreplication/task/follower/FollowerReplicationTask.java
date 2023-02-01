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
import org.opensearch.persistent.AllocatedPersistentTask;
import org.opensearch.persistent.PersistentTasksCustomMetadata;
import org.opensearch.tasks.TaskId;
import org.opensearch.xreplication.services.XReplicationLeaderService;

import java.util.Map;

public class FollowerReplicationTask extends AllocatedPersistentTask {
    private static final Logger logger = LogManager.getLogger(FollowerReplicationTask.class);

    public FollowerReplicationTask(long id, String type, String action, TaskId parentTaskId,
                                   PersistentTasksCustomMetadata.PersistentTask<FollowerReplicationParams> taskInProgress, Map<String, String> headers, XReplicationLeaderService leaderService) {
        super(id, type, action, String.format("FCR:IndexReplicationTask:{}", taskInProgress.getParams()), parentTaskId, headers);
    }

    public void execute() {
        logger.info("[ankikala] Follower task is up");
    }
}
