/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.store.remote;

import java.util.Set;

public interface RemoteStoreInterface {
    Set<String> getTrackedFiles();
    FileInfo getFileInfo(String name);

    void delete(String name);

    interface FileInfo {
        long length();
    }

}
