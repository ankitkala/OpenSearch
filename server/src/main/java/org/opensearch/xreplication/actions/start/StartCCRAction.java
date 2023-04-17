/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.xreplication.actions.start;

import org.opensearch.action.ActionType;


public class StartCCRAction extends ActionType<StartCCRResponse> {
    public static final StartCCRAction INSTANCE = new StartCCRAction();
    public static final String NAME = "indices:data/read/replication_start";

    private StartCCRAction() {
        super(NAME, StartCCRResponse::new);
    }

}
