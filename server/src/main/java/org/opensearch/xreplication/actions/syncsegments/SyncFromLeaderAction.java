/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.syncsegments;

import org.opensearch.action.ActionType;


public class SyncFromLeaderAction extends ActionType<SyncFromLeaderResponse> {

    public static final SyncFromLeaderAction INSTANCE = new SyncFromLeaderAction();
    //TODO: Change the action name to a more meaningful value
    public static final String NAME = "indices:data/read/sync_from_leader";

    public SyncFromLeaderAction() {
        super(NAME, SyncFromLeaderResponse::new);
    }
}
