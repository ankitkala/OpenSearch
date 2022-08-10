/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.support.replication.crosscluster.follower;

import org.opensearch.action.ActionType;

/*
  Action which attempts to sync the follow shard's segments with Leader shard.
 */
public class SyncLeaderSegmentsAction extends ActionType<SyncLeaderSegmentsResponse> {

        public static final SyncLeaderSegmentsAction INSTANCE = new SyncLeaderSegmentsAction();
        public static final String NAME = "indices:data/read/replication/changes";

        private SyncLeaderSegmentsAction() {
            super(NAME, SyncLeaderSegmentsResponse::new);
        }
}
