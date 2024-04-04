/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.store;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FilterDirectory;

import java.io.IOException;
import java.nio.file.Path;

/**
 * BaseRemoteSegmentsStoreDirectory - abstract class created to extend on the functionality of RemoteSegmentStoreDirectory
 * Can add other public methods of RemoteSegmentStoreDirectory here as well such as init() and readLatestMetadata()
 */

public abstract class BaseRemoteSegmentStoreDirectory extends FilterDirectory {

    protected BaseRemoteSegmentStoreDirectory(Directory in) {
        super(in);
    }

    /**
     * Function to download a specific part of file (from pos to pos + length) from the remote store to a desired location
     */

    public abstract void downloadFile(String fileName, long pos, long length, Path location) throws IOException;
}
