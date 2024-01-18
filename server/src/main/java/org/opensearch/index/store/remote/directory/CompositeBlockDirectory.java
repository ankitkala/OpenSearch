/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.store.remote.directory;

import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.FilterDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.opensearch.index.store.remote.RemoteStoreInterface;
import org.opensearch.index.store.remote.filecache.FileCache;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class CompositeBlockDirectory extends Directory {
    private static String BLOCK_EXTENSION = "._block";

    private FilterDirectory baseDirectory;
    private final FSDirectory fsDirectory;
    private RemoteStoreInterface remote;

    private FileCache cache;
    boolean isOpen;
    public CompositeBlockDirectory(FilterDirectory baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.fsDirectory = (FSDirectory) FilterDirectory.unwrap(baseDirectory);
        isOpen = false;

    }
    public void setRemote(RemoteStoreInterface remoteStore) {
        remote = remoteStore;
        isOpen = false;
    }

    @Override
    protected void ensureOpen() throws AlreadyClosedException {
        // check for isOpen
    }
    private boolean isBlockFile(String file) {
        return file.contains(BLOCK_EXTENSION);
    }

    @Override
    public String[] listAll() throws IOException {
        Set<String> allFiles = Arrays.asList(baseDirectory.listAll()).stream().filter(file -> isBlockFile(file) == false).collect(Collectors.toSet());
        allFiles.addAll(remote.getTrackedFiles());

        String[] files = new String[allFiles.size()];
        allFiles.toArray(files);
        Arrays.sort(files);

        return files;
    }

    @Override
    public void deleteFile(String name) throws IOException {
        // TODO: Add support for pending deletions.
        // TODO: Add support for deleting block files? needed?

        if(remote.getTrackedFiles().contains(name)) {
            remote.delete(name);
        }
        // assuming its tracked in cache.
        if (!cache.remove(fsDirectory.getDirectory().resolve(name))) {
            baseDirectory.deleteFile(name);
        }
    }

    @Override
    public long fileLength(String name) throws IOException {
        if(remote.getTrackedFiles().contains(name)) {
            return remote.getFileInfo(name).length();
        } else {
            return baseDirectory.fileLength(name);
        }
    }

    @Override
    public IndexOutput createOutput(String name, IOContext context) throws IOException {
        return baseDirectory.createOutput(name, context);
    }

    @Override
    public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
        return baseDirectory.createTempOutput(prefix, suffix, context);
    }

    @Override
    public void sync(Collection<String> names) throws IOException {
        // TODO: what sync means for composite directory.
        Set<String> remoteFiles = remote.getTrackedFiles();
        baseDirectory.sync(names.stream().filter(remoteFiles::contains).collect(Collectors.toSet()));
    }

    @Override
    public void syncMetaData() throws IOException {
        // TODO: what sync metadata means for composite directory.
    }

    @Override
    public void rename(String source, String dest) throws IOException {

    }

    @Override
    public IndexInput openInput(String name, IOContext context) throws IOException {
        // TODO:
        return baseDirectory.openInput(name, context);
    }

    @Override
    public Lock obtainLock(String name) throws IOException {
        // TODO: Lock implications for composite dir,
        return null;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public Set<String> getPendingDeletions() throws IOException {
        return null; // pending deletions integration pending.
    }
}
