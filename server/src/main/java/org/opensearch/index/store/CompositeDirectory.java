/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.FilterDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.opensearch.index.store.remote.file.OnDemandCompositeBlockIndexInput;
import org.opensearch.index.store.remote.filecache.FileCache;
import org.opensearch.index.store.remote.utils.FileType;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class CompositeDirectory extends FilterDirectory {
    private static final Logger logger = LogManager.getLogger(CompositeDirectory.class);
    private final FSDirectory localDirectory;
    private final BaseRemoteSegmentStoreDirectory remoteDirectory;
    private final FileCache fileCache;
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();
    private final ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();

    public CompositeDirectory(FSDirectory localDirectory, BaseRemoteSegmentStoreDirectory remoteDirectory, FileCache fileCache) {
        super(localDirectory);
        this.localDirectory = localDirectory;
        this.remoteDirectory = remoteDirectory;
        this.fileCache = fileCache;
    }

    @Override
    public String[] listAll() throws IOException {
        logger.trace("listAll() called ...");
        readLock.lock();
        try {
            String[] localFiles = localDirectory.listAll();
            logger.trace("LocalDirectory files : {}", () -> Arrays.toString(localFiles));
            Set<String> allFiles = new HashSet<>(Arrays.asList(localFiles));
            if (remoteDirectory != null) {
                String[] remoteFiles = getRemoteFiles();
                allFiles.addAll(Arrays.asList(remoteFiles));
                logger.trace("Remote Directory files : {}", () -> Arrays.toString(remoteFiles));
            }
            Set<String> localLuceneFiles = allFiles.stream()
                .filter(file -> !FileType.isBlockFile(file))
                .collect(Collectors.toUnmodifiableSet());
            String[] files = new String[localLuceneFiles.size()];
            localLuceneFiles.toArray(files);
            Arrays.sort(files);
            logger.trace("listAll() returns : {}", () -> Arrays.toString(files));
            return files;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void deleteFile(String name) throws IOException {
        logger.trace("deleteFile() called {}", name);
        writeLock.lock();
        try {
            localDirectory.deleteFile(name);
            fileCache.remove(localDirectory.getDirectory().resolve(name));
        } catch (NoSuchFileException e) {
            /**
             * We might encounter NoSuchFileException in case file is deleted from local
             * But if it is present in remote we should just skip deleting this file
             * TODO : Handle cases where file is not present in remote as well
             */
            logger.trace("NoSuchFileException encountered while deleting {} from local", name);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public long fileLength(String name) throws IOException {
        logger.trace("fileLength() called {}", name);
        readLock.lock();
        try {
            long fileLength;
            if (Arrays.asList(getRemoteFiles()).contains(name) == false) {
                fileLength = localDirectory.fileLength(name);
                logger.trace("fileLength from Local {}", fileLength);
            } else {
                fileLength = remoteDirectory.fileLength(name);
                logger.trace("fileLength from Remote {}", fileLength);
            }
            return fileLength;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public IndexOutput createOutput(String name, IOContext context) throws IOException {
        logger.trace("createOutput() called {}", name);
        writeLock.lock();
        try {
            return localDirectory.createOutput(name, context);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
        logger.trace("createTempOutput() called {} , {}", prefix, suffix);
        writeLock.lock();
        try {
            return localDirectory.createTempOutput(prefix, suffix, context);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void sync(Collection<String> names) throws IOException {
        logger.trace("sync() called {}", names);
        writeLock.lock();
        try {
            Collection<String> newLocalFiles = new ArrayList<>();
            Collection<String> remoteFiles = Arrays.asList(getRemoteFiles());
            for (String name : names) {
                if (remoteFiles.contains(name) == false) newLocalFiles.add(name);
            }
            logger.trace("Synced files : {}", newLocalFiles);
            localDirectory.sync(newLocalFiles);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void syncMetaData() throws IOException {
        logger.trace("syncMetaData() called ");
        writeLock.lock();
        try {
            localDirectory.syncMetaData();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void rename(String source, String dest) throws IOException {
        logger.trace("rename() called {}, {}", source, dest);
        writeLock.lock();
        try {
            localDirectory.rename(source, dest);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public IndexInput openInput(String name, IOContext context) throws IOException {
        logger.trace("openInput() called {}", name);
        writeLock.lock();
        try {
            if (Arrays.asList(getRemoteFiles()).contains(name) == false) {
                // If file has not yet been uploaded to Remote Store, fetch it from the local directory
                logger.trace("File found in disk");
                return localDirectory.openInput(name, context);
            } else {
                // If file has been uploaded to the Remote Store, fetch it from the Remote Store in blocks via
                // OnDemandCompositeBlockIndexInput
                logger.trace("File to be fetched from Remote");
                return new OnDemandCompositeBlockIndexInput(remoteDirectory, name, localDirectory, fileCache, context);
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Lock obtainLock(String name) throws IOException {
        logger.trace("obtainLock() called {}", name);
        writeLock.lock();
        try {
            return localDirectory.obtainLock(name);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        writeLock.lock();
        try {
            localDirectory.close();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Set<String> getPendingDeletions() throws IOException {
        readLock.lock();
        try {
            return localDirectory.getPendingDeletions();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Function to perform operations once files have been uploaded to Remote Store
     * Once uploaded local files are safe to delete
     * @param files : files which have been successfully uploaded to Remote Store
     * @throws IOException
     */
    public void afterSyncToRemote(Collection<String> files) throws IOException {
        logger.trace("afterSyncToRemote called for {}", files);
        if (remoteDirectory == null) {
            logger.trace("afterSyncToRemote called even though remote directory is not set");
            return;
        }
        for (String fileName : files) {
            writeLock.lock();
            try {
                localDirectory.deleteFile(fileName);
            } finally {
                writeLock.unlock();
            }
        }
    }

    private String[] getRemoteFiles() {
        String[] remoteFiles;
        try {
            remoteFiles = remoteDirectory.listAll();
        } catch (Exception e) {
            /**
             * There are two scenarios where the listAll() call on remote directory fails:
             * - When remote directory is not set
             * - When init() of remote directory has not yet been called (which results in NullPointerException while calling listAll() for RemoteSegmentStoreDirectory)
             *
             * Returning an empty list in these scenarios
             */
            remoteFiles = new String[0];
        }
        return remoteFiles;
    }
}
