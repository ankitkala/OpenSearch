/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.store.remote.file;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.opensearch.index.store.BaseRemoteSegmentStoreDirectory;
import org.opensearch.index.store.remote.filecache.CachedIndexInput;
import org.opensearch.index.store.remote.filecache.FileCache;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class OnDemandCompositeBlockIndexInput extends OnDemandBlockIndexInput {

    private static final Logger logger = LogManager.getLogger(OnDemandCompositeBlockIndexInput.class);
    private final BaseRemoteSegmentStoreDirectory remoteDirectory;
    private final String fileName;
    private final Long originalFileSize;
    private final FSDirectory directory;
    private final IOContext context;
    private final FileCache fileCache;

    public OnDemandCompositeBlockIndexInput(
        BaseRemoteSegmentStoreDirectory remoteDirectory,
        String fileName,
        FSDirectory directory,
        FileCache fileCache,
        IOContext context
    ) throws IOException {
        this(
            OnDemandBlockIndexInput.builder()
                .resourceDescription("OnDemandCompositeBlockIndexInput")
                .isClone(false)
                .offset(0L)
                .length(remoteDirectory.fileLength(fileName)),
            remoteDirectory,
            fileName,
            directory,
            fileCache,
            context
        );
    }

    public OnDemandCompositeBlockIndexInput(
        Builder builder,
        BaseRemoteSegmentStoreDirectory remoteDirectory,
        String fileName,
        FSDirectory directory,
        FileCache fileCache,
        IOContext context
    ) throws IOException {
        super(builder);
        this.remoteDirectory = remoteDirectory;
        this.directory = directory;
        this.fileName = fileName;
        this.fileCache = fileCache;
        this.context = context;
        originalFileSize = remoteDirectory.fileLength(fileName);
    }

    @Override
    protected OnDemandCompositeBlockIndexInput buildSlice(String sliceDescription, long offset, long length) {
        try {
            return new OnDemandCompositeBlockIndexInput(
                OnDemandBlockIndexInput.builder()
                    .blockSizeShift(blockSizeShift)
                    .isClone(true)
                    .offset(this.offset + offset)
                    .length(length)
                    .resourceDescription(sliceDescription),
                remoteDirectory,
                fileName,
                directory,
                fileCache,
                context
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected IndexInput fetchBlock(int blockId) throws IOException {
        logger.trace("fetchBlock called with blockId -> {}", blockId);
        final String blockFileName = fileName + "_block_" + blockId;
        final long blockStart = getBlockStart(blockId);
        final long length = getActualBlockSize(blockId);
        logger.trace(
            "File: {} , Block File: {} , Length: {} , BlockSize: {} , OriginalFileSize: {}",
            fileName,
            blockFileName,
            blockStart,
            length,
            originalFileSize
        );
        Path filePath = directory.getDirectory().resolve(blockFileName);
        final CachedIndexInput cacheEntry = fileCache.compute(filePath, (path, cachedIndexInput) -> {
            if (cachedIndexInput == null || cachedIndexInput.isClosed()) {
                // Doesn't exist or is closed, either way create a new one
                try {
                    logger.trace("Downloading block from Remote");
                    remoteDirectory.downloadFile(fileName, blockStart, length, filePath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return new CachedIndexInputImpl(blockFileName);
            } else {
                logger.trace("Block already present in cache");
                // already in the cache and ready to be used (open)
                return cachedIndexInput;
            }
        });

        return cacheEntry.getIndexInput();
    }

    @Override
    public OnDemandBlockIndexInput clone() {
        OnDemandCompositeBlockIndexInput clone = buildSlice("clone", 0L, this.length);
        // ensures that clones may be positioned at the same point as the blocked file they were cloned from
        clone.cloneBlock(this);
        return clone;
    }

    private long getActualBlockSize(int blockId) {
        return (blockId != getBlock(originalFileSize - 1)) ? blockSize : getBlockOffset(originalFileSize - 1) + 1;
    }

    private class CachedIndexInputImpl implements CachedIndexInput {
        AtomicBoolean isClosed;
        String blockFileName;

        CachedIndexInputImpl(String blockFileName) {
            this.blockFileName = blockFileName;
            isClosed = new AtomicBoolean(false);
        }

        @Override
        public IndexInput getIndexInput() throws IOException {
            return directory.openInput(blockFileName, context);
        }

        @Override
        public long length() {
            try {
                return directory.fileLength(blockFileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean isClosed() {
            return isClosed.get();
        }

        @Override
        public void close() throws Exception {
            isClosed.set(true);
        }
    }
}
