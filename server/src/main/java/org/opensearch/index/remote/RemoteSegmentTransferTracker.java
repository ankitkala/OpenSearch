/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.remote;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.opensearch.common.CheckedFunction;
import org.opensearch.common.logging.Loggers;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.common.util.MovingAverage;
import org.opensearch.common.util.Streak;
import org.opensearch.common.util.concurrent.ConcurrentCollections;
import org.opensearch.core.index.shard.ShardId;
import org.opensearch.index.store.DirectoryFileTransferTracker;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.opensearch.index.shard.RemoteStoreRefreshListener.EXCLUDE_FILES;

/**
 * Keeps track of remote refresh which happens in {@link org.opensearch.index.shard.RemoteStoreRefreshListener}. This consist of multiple critical metrics.
 *
 * @opensearch.internal
 */
public class RemoteSegmentTransferTracker {

    private final Logger logger;

    /**
     * ShardId for which this instance tracks the remote segment upload metadata.
     */
    private final ShardId shardId;

    /**
     * Every refresh is assigned a sequence number. This is the sequence number of the most recent refresh.
     */
    private volatile long localRefreshSeqNo;

    /**
     * The refresh time of the most recent refresh.
     */
    private volatile long localRefreshTimeMs;

    /**
     * The refresh time(clock) of the most recent refresh.
     */
    private volatile long localRefreshClockTimeMs;

    /**
     * Sequence number of the most recent remote refresh.
     */
    private volatile long remoteRefreshSeqNo;

    /**
     * The refresh time of most recent remote refresh.
     */
    private volatile long remoteRefreshTimeMs;

    /**
     * The refresh time(clock) of most recent remote refresh.
     */
    private volatile long remoteRefreshClockTimeMs;

    /**
     * Keeps the seq no lag computed so that we do not compute it for every request.
     */
    private volatile long refreshSeqNoLag;

    /**
     * Keeps the time (ms) lag computed so that we do not compute it for every request.
     */
    private volatile long timeMsLag;

    /**
     * Keeps track of the total bytes of segment files which were uploaded to remote store during last successful remote refresh
     */
    private volatile long lastSuccessfulRemoteRefreshBytes;

    /**
     * Cumulative sum of size in bytes of segment files for which upload has started during remote refresh.
     */
    private volatile long uploadBytesStarted;

    /**
     * Cumulative sum of size in bytes of segment files for which upload has failed during remote refresh.
     */
    private volatile long uploadBytesFailed;

    /**
     * Cumulative sum of size in bytes of segment files for which upload has succeeded during remote refresh.
     */
    private volatile long uploadBytesSucceeded;

    /**
     * Cumulative sum of count of remote refreshes that have started.
     */
    private volatile long totalUploadsStarted;

    /**
     * Cumulative sum of count of remote refreshes that have failed.
     */
    private volatile long totalUploadsFailed;

    /**
     * Cumulative sum of count of remote refreshes that have succeeded.
     */
    private volatile long totalUploadsSucceeded;

    /**
     * Cumulative sum of rejection counts for this shard.
     */
    private final AtomicLong rejectionCount = new AtomicLong();

    /**
     * Keeps track of rejection count with each rejection reason.
     */
    private final Map<String, AtomicLong> rejectionCountMap = ConcurrentCollections.newConcurrentMap();

    /**
     * Keeps track of segment files and their size in bytes which are part of the most recent refresh.
     */
    private final Map<String, Long> latestLocalFileNameLengthMap = ConcurrentCollections.newConcurrentMap();

    /**
     * This contains the files from the last successful remote refresh and ongoing uploads. This gets reset to just the
     * last successful remote refresh state on successful remote refresh.
     */
    private final Set<String> latestUploadedFiles = ConcurrentCollections.newConcurrentSet();

    /**
     * Keeps the bytes lag computed so that we do not compute it for every request.
     */
    private volatile long bytesLag;

    /**
     * Holds count of consecutive failures until last success. Gets reset to zero if there is a success.
     */
    private final Streak failures = new Streak();

    /**
     * Provides moving average over the last N total size in bytes of segment files uploaded as part of remote refresh.
     * N is window size. Wrapped with {@code AtomicReference} for dynamic changes in window size.
     */
    private final AtomicReference<MovingAverage> uploadBytesMovingAverageReference;

    /**
     * This lock object is used for making sure we do not miss any data
     */
    private final Object uploadBytesMutex = new Object();

    /**
     * Provides moving average over the last N upload speed (in bytes/s) of segment files uploaded as part of remote refresh.
     * N is window size. Wrapped with {@code AtomicReference} for dynamic changes in window size.
     */
    private final AtomicReference<MovingAverage> uploadBytesPerSecMovingAverageReference;

    private final Object uploadBytesPerSecMutex = new Object();

    /**
     * Provides moving average over the last N overall upload time (in nanos) as part of remote refresh.N is window size.
     * Wrapped with {@code AtomicReference} for dynamic changes in window size.
     */
    private final AtomicReference<MovingAverage> uploadTimeMsMovingAverageReference;

    private final Object uploadTimeMsMutex = new Object();

    /**
     * {@link org.opensearch.index.store.Store.StoreDirectory} level file transfer tracker, used to show download stats
     */
    private final DirectoryFileTransferTracker directoryFileTransferTracker;

    public RemoteSegmentTransferTracker(
        ShardId shardId,
        DirectoryFileTransferTracker directoryFileTransferTracker,
        int uploadBytesMovingAverageWindowSize,
        int uploadBytesPerSecMovingAverageWindowSize,
        int uploadTimeMsMovingAverageWindowSize
    ) {
        logger = Loggers.getLogger(getClass(), shardId);
        this.shardId = shardId;
        // Both the local refresh time and remote refresh time are set with current time to give consistent view of time lag when it arises.
        long currentClockTimeMs = System.currentTimeMillis();
        long currentTimeMs = System.nanoTime() / 1_000_000L;
        localRefreshTimeMs = currentTimeMs;
        remoteRefreshTimeMs = currentTimeMs;
        localRefreshClockTimeMs = currentClockTimeMs;
        remoteRefreshClockTimeMs = currentClockTimeMs;
        uploadBytesMovingAverageReference = new AtomicReference<>(new MovingAverage(uploadBytesMovingAverageWindowSize));
        uploadBytesPerSecMovingAverageReference = new AtomicReference<>(new MovingAverage(uploadBytesPerSecMovingAverageWindowSize));
        uploadTimeMsMovingAverageReference = new AtomicReference<>(new MovingAverage(uploadTimeMsMovingAverageWindowSize));
        this.directoryFileTransferTracker = directoryFileTransferTracker;
    }

    ShardId getShardId() {
        return shardId;
    }

    public long getLocalRefreshSeqNo() {
        return localRefreshSeqNo;
    }

    // Visible for testing
    void updateLocalRefreshSeqNo(long localRefreshSeqNo) {
        assert localRefreshSeqNo >= this.localRefreshSeqNo : "newLocalRefreshSeqNo="
            + localRefreshSeqNo
            + " < "
            + "currentLocalRefreshSeqNo="
            + this.localRefreshSeqNo;
        this.localRefreshSeqNo = localRefreshSeqNo;
        computeRefreshSeqNoLag();
    }

    public long getLocalRefreshTimeMs() {
        return localRefreshTimeMs;
    }

    public long getLocalRefreshClockTimeMs() {
        return localRefreshClockTimeMs;
    }

    /**
     * Updates the last refresh time and refresh seq no which is seen by local store.
     */
    public void updateLocalRefreshTimeAndSeqNo() {
        updateLocalRefreshClockTimeMs(System.currentTimeMillis());
        updateLocalRefreshTimeMs(System.nanoTime() / 1_000_000L);
        updateLocalRefreshSeqNo(getLocalRefreshSeqNo() + 1);
    }

    // Visible for testing
    void updateLocalRefreshTimeMs(long localRefreshTimeMs) {
        assert localRefreshTimeMs >= this.localRefreshTimeMs : "newLocalRefreshTimeMs="
            + localRefreshTimeMs
            + " < "
            + "currentLocalRefreshTimeMs="
            + this.localRefreshTimeMs;
        this.localRefreshTimeMs = localRefreshTimeMs;
        computeTimeMsLag();
    }

    private void updateLocalRefreshClockTimeMs(long localRefreshClockTimeMs) {
        this.localRefreshClockTimeMs = localRefreshClockTimeMs;
    }

    long getRemoteRefreshSeqNo() {
        return remoteRefreshSeqNo;
    }

    public void updateRemoteRefreshSeqNo(long remoteRefreshSeqNo) {
        assert remoteRefreshSeqNo >= this.remoteRefreshSeqNo : "newRemoteRefreshSeqNo="
            + remoteRefreshSeqNo
            + " < "
            + "currentRemoteRefreshSeqNo="
            + this.remoteRefreshSeqNo;
        this.remoteRefreshSeqNo = remoteRefreshSeqNo;
        computeRefreshSeqNoLag();
    }

    long getRemoteRefreshTimeMs() {
        return remoteRefreshTimeMs;
    }

    long getRemoteRefreshClockTimeMs() {
        return remoteRefreshClockTimeMs;
    }

    public void updateRemoteRefreshTimeMs(long remoteRefreshTimeMs) {
        assert remoteRefreshTimeMs >= this.remoteRefreshTimeMs : "newRemoteRefreshTimeMs="
            + remoteRefreshTimeMs
            + " < "
            + "currentRemoteRefreshTimeMs="
            + this.remoteRefreshTimeMs;
        this.remoteRefreshTimeMs = remoteRefreshTimeMs;
        computeTimeMsLag();
    }

    public void updateRemoteRefreshClockTimeMs(long remoteRefreshClockTimeMs) {
        this.remoteRefreshClockTimeMs = remoteRefreshClockTimeMs;
    }

    private void computeRefreshSeqNoLag() {
        refreshSeqNoLag = localRefreshSeqNo - remoteRefreshSeqNo;
    }

    public long getRefreshSeqNoLag() {
        return refreshSeqNoLag;
    }

    private void computeTimeMsLag() {
        timeMsLag = localRefreshTimeMs - remoteRefreshTimeMs;
    }

    public long getTimeMsLag() {
        return timeMsLag;
    }

    public long getBytesLag() {
        return bytesLag;
    }

    public long getUploadBytesStarted() {
        return uploadBytesStarted;
    }

    public void addUploadBytesStarted(long size) {
        uploadBytesStarted += size;
    }

    public long getUploadBytesFailed() {
        return uploadBytesFailed;
    }

    public void addUploadBytesFailed(long size) {
        uploadBytesFailed += size;
    }

    public long getUploadBytesSucceeded() {
        return uploadBytesSucceeded;
    }

    public void addUploadBytesSucceeded(long size) {
        uploadBytesSucceeded += size;
    }

    public long getInflightUploadBytes() {
        return uploadBytesStarted - uploadBytesFailed - uploadBytesSucceeded;
    }

    public long getTotalUploadsStarted() {
        return totalUploadsStarted;
    }

    public void incrementTotalUploadsStarted() {
        totalUploadsStarted += 1;
    }

    public long getTotalUploadsFailed() {
        return totalUploadsFailed;
    }

    public void incrementTotalUploadsFailed() {
        totalUploadsFailed += 1;
        failures.record(true);
    }

    public long getTotalUploadsSucceeded() {
        return totalUploadsSucceeded;
    }

    public void incrementTotalUploadsSucceeded() {
        totalUploadsSucceeded += 1;
        failures.record(false);
    }

    public long getInflightUploads() {
        return totalUploadsStarted - totalUploadsFailed - totalUploadsSucceeded;
    }

    public long getRejectionCount() {
        return rejectionCount.get();
    }

    void incrementRejectionCount() {
        rejectionCount.incrementAndGet();
    }

    void incrementRejectionCount(String rejectionReason) {
        rejectionCountMap.computeIfAbsent(rejectionReason, k -> new AtomicLong()).incrementAndGet();
        incrementRejectionCount();
    }

    long getRejectionCount(String rejectionReason) {
        return rejectionCountMap.get(rejectionReason).get();
    }

    public Map<String, Long> getLatestLocalFileNameLengthMap() {
        return Collections.unmodifiableMap(latestLocalFileNameLengthMap);
    }

    /**
     * Updates the latestLocalFileNameLengthMap by adding file name and it's size to the map. The method is given a function as an argument which is used for determining the file size (length in bytes). This method is also provided the collection of segment files which are the latest refresh local segment files. This method also removes the stale segment files from the map that are not part of the input segment files.
     *
     * @param segmentFiles     list of local refreshed segment files
     * @param fileSizeFunction function is used to determine the file size in bytes
     */
    public void updateLatestLocalFileNameLengthMap(
        Collection<String> segmentFiles,
        CheckedFunction<String, Long, IOException> fileSizeFunction
    ) {
        // Update the map
        segmentFiles.stream()
            .filter(file -> EXCLUDE_FILES.contains(file) == false)
            .filter(file -> latestLocalFileNameLengthMap.containsKey(file) == false || latestLocalFileNameLengthMap.get(file) == 0)
            .forEach(file -> {
                long fileSize = 0;
                try {
                    fileSize = fileSizeFunction.apply(file);
                } catch (IOException e) {
                    logger.warn(new ParameterizedMessage("Exception while reading the fileLength of file={}", file), e);
                }
                latestLocalFileNameLengthMap.put(file, fileSize);
            });
        Set<String> fileSet = new HashSet<>(segmentFiles);
        // Remove keys from the fileSizeMap that do not exist in the latest segment files
        latestLocalFileNameLengthMap.entrySet().removeIf(entry -> fileSet.contains(entry.getKey()) == false);
        computeBytesLag();
    }

    public void addToLatestUploadedFiles(String file) {
        this.latestUploadedFiles.add(file);
        computeBytesLag();
    }

    public void setLatestUploadedFiles(Set<String> files) {
        this.latestUploadedFiles.clear();
        this.latestUploadedFiles.addAll(files);
        computeBytesLag();
    }

    private void computeBytesLag() {
        if (latestLocalFileNameLengthMap.isEmpty()) {
            return;
        }
        Set<String> filesNotYetUploaded = latestLocalFileNameLengthMap.keySet()
            .stream()
            .filter(f -> !latestUploadedFiles.contains(f))
            .collect(Collectors.toSet());
        this.bytesLag = filesNotYetUploaded.stream().map(latestLocalFileNameLengthMap::get).mapToLong(Long::longValue).sum();
    }

    int getConsecutiveFailureCount() {
        return failures.length();
    }

    boolean isUploadBytesAverageReady() {
        return uploadBytesMovingAverageReference.get().isReady();
    }

    double getUploadBytesAverage() {
        return uploadBytesMovingAverageReference.get().getAverage();
    }

    public void addUploadBytes(long size) {
        lastSuccessfulRemoteRefreshBytes = size;
        synchronized (uploadBytesMutex) {
            this.uploadBytesMovingAverageReference.get().record(size);
        }
    }

    /**
     * Updates the window size for data collection of upload bytes. This also resets any data collected so far.
     *
     * @param updatedSize the updated size
     */
    void updateUploadBytesMovingAverageWindowSize(int updatedSize) {
        synchronized (uploadBytesMutex) {
            this.uploadBytesMovingAverageReference.set(this.uploadBytesMovingAverageReference.get().copyWithSize(updatedSize));
        }
    }

    boolean isUploadBytesPerSecAverageReady() {
        return uploadBytesPerSecMovingAverageReference.get().isReady();
    }

    double getUploadBytesPerSecAverage() {
        return uploadBytesPerSecMovingAverageReference.get().getAverage();
    }

    public void addUploadBytesPerSec(long bytesPerSec) {
        synchronized (uploadBytesPerSecMutex) {
            this.uploadBytesPerSecMovingAverageReference.get().record(bytesPerSec);
        }
    }

    /**
     * Updates the window size for data collection of upload bytes per second. This also resets any data collected so far.
     *
     * @param updatedSize the updated size
     */
    void updateUploadBytesPerSecMovingAverageWindowSize(int updatedSize) {
        synchronized (uploadBytesPerSecMutex) {
            this.uploadBytesPerSecMovingAverageReference.set(this.uploadBytesPerSecMovingAverageReference.get().copyWithSize(updatedSize));
        }
    }

    boolean isUploadTimeMsAverageReady() {
        return uploadTimeMsMovingAverageReference.get().isReady();
    }

    double getUploadTimeMsAverage() {
        return uploadTimeMsMovingAverageReference.get().getAverage();
    }

    public void addUploadTimeMs(long timeMs) {
        synchronized (uploadTimeMsMutex) {
            this.uploadTimeMsMovingAverageReference.get().record(timeMs);
        }
    }

    /**
     * Updates the window size for data collection of upload time (ms). This also resets any data collected so far.
     *
     * @param updatedSize the updated size
     */
    void updateUploadTimeMsMovingAverageWindowSize(int updatedSize) {
        synchronized (uploadTimeMsMutex) {
            this.uploadTimeMsMovingAverageReference.set(this.uploadTimeMsMovingAverageReference.get().copyWithSize(updatedSize));
        }
    }

    public DirectoryFileTransferTracker getDirectoryFileTransferTracker() {
        return directoryFileTransferTracker;
    }

    public RemoteSegmentTransferTracker.Stats stats() {
        return new RemoteSegmentTransferTracker.Stats(
            shardId,
            localRefreshClockTimeMs,
            remoteRefreshClockTimeMs,
            timeMsLag,
            localRefreshSeqNo,
            remoteRefreshSeqNo,
            uploadBytesStarted,
            uploadBytesSucceeded,
            uploadBytesFailed,
            totalUploadsStarted,
            totalUploadsSucceeded,
            totalUploadsFailed,
            rejectionCount.get(),
            failures.length(),
            lastSuccessfulRemoteRefreshBytes,
            uploadBytesMovingAverageReference.get().getAverage(),
            uploadBytesPerSecMovingAverageReference.get().getAverage(),
            uploadTimeMsMovingAverageReference.get().getAverage(),
            getBytesLag(),
            directoryFileTransferTracker.stats()
        );
    }

    /**
     * Represents the tracker's state as seen in the stats API.
     *
     * @opensearch.internal
     */
    public static class Stats implements Writeable {

        public final ShardId shardId;
        public final long localRefreshClockTimeMs;
        public final long remoteRefreshClockTimeMs;
        public final long refreshTimeLagMs;
        public final long localRefreshNumber;
        public final long remoteRefreshNumber;
        public final long uploadBytesStarted;
        public final long uploadBytesFailed;
        public final long uploadBytesSucceeded;
        public final long totalUploadsStarted;
        public final long totalUploadsFailed;
        public final long totalUploadsSucceeded;
        public final long rejectionCount;
        public final long consecutiveFailuresCount;
        public final long lastSuccessfulRemoteRefreshBytes;
        public final double uploadBytesMovingAverage;
        public final double uploadBytesPerSecMovingAverage;
        public final double uploadTimeMovingAverage;
        public final long bytesLag;
        public final DirectoryFileTransferTracker.Stats directoryFileTransferTrackerStats;

        public Stats(
            ShardId shardId,
            long localRefreshClockTimeMs,
            long remoteRefreshClockTimeMs,
            long refreshTimeLagMs,
            long localRefreshNumber,
            long remoteRefreshNumber,
            long uploadBytesStarted,
            long uploadBytesSucceeded,
            long uploadBytesFailed,
            long totalUploadsStarted,
            long totalUploadsSucceeded,
            long totalUploadsFailed,
            long rejectionCount,
            long consecutiveFailuresCount,
            long lastSuccessfulRemoteRefreshBytes,
            double uploadBytesMovingAverage,
            double uploadBytesPerSecMovingAverage,
            double uploadTimeMovingAverage,
            long bytesLag,
            DirectoryFileTransferTracker.Stats directoryFileTransferTrackerStats
        ) {
            this.shardId = shardId;
            this.localRefreshClockTimeMs = localRefreshClockTimeMs;
            this.remoteRefreshClockTimeMs = remoteRefreshClockTimeMs;
            this.refreshTimeLagMs = refreshTimeLagMs;
            this.localRefreshNumber = localRefreshNumber;
            this.remoteRefreshNumber = remoteRefreshNumber;
            this.uploadBytesStarted = uploadBytesStarted;
            this.uploadBytesFailed = uploadBytesFailed;
            this.uploadBytesSucceeded = uploadBytesSucceeded;
            this.totalUploadsStarted = totalUploadsStarted;
            this.totalUploadsFailed = totalUploadsFailed;
            this.totalUploadsSucceeded = totalUploadsSucceeded;
            this.rejectionCount = rejectionCount;
            this.consecutiveFailuresCount = consecutiveFailuresCount;
            this.lastSuccessfulRemoteRefreshBytes = lastSuccessfulRemoteRefreshBytes;
            this.uploadBytesMovingAverage = uploadBytesMovingAverage;
            this.uploadBytesPerSecMovingAverage = uploadBytesPerSecMovingAverage;
            this.uploadTimeMovingAverage = uploadTimeMovingAverage;
            this.bytesLag = bytesLag;
            this.directoryFileTransferTrackerStats = directoryFileTransferTrackerStats;
        }

        public Stats(StreamInput in) throws IOException {
            try {
                this.shardId = new ShardId(in);
                this.localRefreshClockTimeMs = in.readLong();
                this.remoteRefreshClockTimeMs = in.readLong();
                this.refreshTimeLagMs = in.readLong();
                this.localRefreshNumber = in.readLong();
                this.remoteRefreshNumber = in.readLong();
                this.uploadBytesStarted = in.readLong();
                this.uploadBytesFailed = in.readLong();
                this.uploadBytesSucceeded = in.readLong();
                this.totalUploadsStarted = in.readLong();
                this.totalUploadsFailed = in.readLong();
                this.totalUploadsSucceeded = in.readLong();
                this.rejectionCount = in.readLong();
                this.consecutiveFailuresCount = in.readLong();
                this.lastSuccessfulRemoteRefreshBytes = in.readLong();
                this.uploadBytesMovingAverage = in.readDouble();
                this.uploadBytesPerSecMovingAverage = in.readDouble();
                this.uploadTimeMovingAverage = in.readDouble();
                this.bytesLag = in.readLong();
                this.directoryFileTransferTrackerStats = in.readOptionalWriteable(DirectoryFileTransferTracker.Stats::new);
            } catch (IOException e) {
                throw e;
            }
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            shardId.writeTo(out);
            out.writeLong(localRefreshClockTimeMs);
            out.writeLong(remoteRefreshClockTimeMs);
            out.writeLong(refreshTimeLagMs);
            out.writeLong(localRefreshNumber);
            out.writeLong(remoteRefreshNumber);
            out.writeLong(uploadBytesStarted);
            out.writeLong(uploadBytesFailed);
            out.writeLong(uploadBytesSucceeded);
            out.writeLong(totalUploadsStarted);
            out.writeLong(totalUploadsFailed);
            out.writeLong(totalUploadsSucceeded);
            out.writeLong(rejectionCount);
            out.writeLong(consecutiveFailuresCount);
            out.writeLong(lastSuccessfulRemoteRefreshBytes);
            out.writeDouble(uploadBytesMovingAverage);
            out.writeDouble(uploadBytesPerSecMovingAverage);
            out.writeDouble(uploadTimeMovingAverage);
            out.writeLong(bytesLag);
            out.writeOptionalWriteable(directoryFileTransferTrackerStats);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Stats other = (Stats) obj;

            return this.shardId.toString().equals(other.shardId.toString())
                && this.localRefreshClockTimeMs == other.localRefreshClockTimeMs
                && this.remoteRefreshClockTimeMs == other.remoteRefreshClockTimeMs
                && this.refreshTimeLagMs == other.refreshTimeLagMs
                && this.localRefreshNumber == other.localRefreshNumber
                && this.remoteRefreshNumber == other.remoteRefreshNumber
                && this.uploadBytesStarted == other.uploadBytesStarted
                && this.uploadBytesFailed == other.uploadBytesFailed
                && this.uploadBytesSucceeded == other.uploadBytesSucceeded
                && this.totalUploadsStarted == other.totalUploadsStarted
                && this.totalUploadsFailed == other.totalUploadsFailed
                && this.totalUploadsSucceeded == other.totalUploadsSucceeded
                && this.rejectionCount == other.rejectionCount
                && this.consecutiveFailuresCount == other.consecutiveFailuresCount
                && this.lastSuccessfulRemoteRefreshBytes == other.lastSuccessfulRemoteRefreshBytes
                && Double.compare(this.uploadBytesMovingAverage, other.uploadBytesMovingAverage) == 0
                && Double.compare(this.uploadBytesPerSecMovingAverage, other.uploadBytesPerSecMovingAverage) == 0
                && Double.compare(this.uploadTimeMovingAverage, other.uploadTimeMovingAverage) == 0
                && this.bytesLag == other.bytesLag
                && this.directoryFileTransferTrackerStats.equals(other.directoryFileTransferTrackerStats);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                shardId,
                localRefreshClockTimeMs,
                remoteRefreshClockTimeMs,
                refreshTimeLagMs,
                localRefreshNumber,
                remoteRefreshNumber,
                uploadBytesStarted,
                uploadBytesFailed,
                uploadBytesSucceeded,
                totalUploadsStarted,
                totalUploadsFailed,
                totalUploadsSucceeded,
                rejectionCount,
                consecutiveFailuresCount,
                lastSuccessfulRemoteRefreshBytes,
                uploadBytesMovingAverage,
                uploadBytesPerSecMovingAverage,
                uploadTimeMovingAverage,
                bytesLag,
                directoryFileTransferTrackerStats
            );
        }
    }
}
