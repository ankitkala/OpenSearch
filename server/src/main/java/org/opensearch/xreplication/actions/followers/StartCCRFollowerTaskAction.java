/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.followers;

import org.opensearch.action.ActionType;


public class StartCCRFollowerTaskAction extends ActionType<StartCCRFollowerTaskResponse> {
    public static final StartCCRFollowerTaskAction INSTANCE = new StartCCRFollowerTaskAction();
    //TODO: Change the action name to a more meaningful value
    public static final String NAME = "indices:data/read/follower_start";

    private StartCCRFollowerTaskAction() {
        super(NAME, StartCCRFollowerTaskResponse::new);
    }

}
