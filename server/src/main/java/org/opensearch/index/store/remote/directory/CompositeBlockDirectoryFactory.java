/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.store.remote.directory;

import org.apache.lucene.store.Directory;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.shard.ShardPath;
import org.opensearch.index.store.remote.filecache.FileCache;
import org.opensearch.plugins.IndexStorePlugin;
import org.opensearch.repositories.RepositoriesService;
import org.opensearch.threadpool.ThreadPool;

import java.io.IOException;
import java.util.function.Supplier;

public class CompositeBlockDirectoryFactory implements IndexStorePlugin.DirectoryFactory {
    IndexStorePlugin.DirectoryFactory localDirectoryFactory;
    public CompositeBlockDirectoryFactory(Supplier<RepositoriesService> repositoriesService, ThreadPool threadPool,
                                          FileCache remoteStoreFileCache, IndexStorePlugin.DirectoryFactory remoteDirectoryFactory,
                                          IndexStorePlugin.DirectoryFactory localDirectoryFactory) {
        this.localDirectoryFactory = localDirectoryFactory;

    }

    @Override
    public Directory newDirectory(IndexSettings indexSettings, ShardPath shardPath) throws IOException {
        Directory localDir = localDirectoryFactory.newDirectory(indexSettings, shardPath);
        return new CompositeBlockDirectory(localDir);
    }


}
