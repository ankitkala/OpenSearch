/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.start;

import org.opensearch.action.ActionType;


public class StartXReplication extends ActionType<StartXReplicationResponse> {
    public static final StartXReplication INSTANCE = new StartXReplication();
    public static final String NAME = "indices:data/read/replication_start";

    private StartXReplication() {
        super(NAME, StartXReplicationResponse::new);
    }

}
