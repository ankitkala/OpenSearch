/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.index;

import org.opensearch.action.ActionType;


public class StartCCRIndexTaskAction extends ActionType<StartCCRIndexTaskResponse> {
    public static final StartCCRIndexTaskAction INSTANCE = new StartCCRIndexTaskAction();
    //TODO: Change the action name to a more meaningful value
    public static final String NAME = "indices:data/read/index_start";

    private StartCCRIndexTaskAction() {
        super(NAME, StartCCRIndexTaskResponse::new);
    }

}
