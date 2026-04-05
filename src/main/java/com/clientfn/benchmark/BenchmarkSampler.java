package com.clientfn.benchmark;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;

public final class BenchmarkSampler implements IFpsStats {
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final long HEAP_INTERVAL_NS = 10L * NANOS_PER_SECOND;
    private static final int MAX_RING_CAPACITY = 420_000;
    private static final List<GarbageCollectorMXBean> GC_BEANS = ManagementFactory.getGarbageCollectorMXBeans();

    private final LongRingBuffer frameTimesNs;
    private final int durationSeconds;
    private final long durationNs;
    private final long[] heapSnapshotBytes;

    private boolean running;
    private long startNs;
    private long endNs;
    private long nextHeapSampleNs;
    private int heapSamples;

    private long gcCountStart;
    private long gcPauseMsStart;
    private long gcCountEnd;
    private long gcPauseMsEnd;

    private long totalFrameCount;
    private long totalFrameTimeNs;

    private boolean sortedDirty = true;
    private long[] sortedCache = new long[0];

    public BenchmarkSampler(int durationSeconds) {
        this(durationSeconds, 7000);
    }

    public BenchmarkSampler(int durationSeconds, int expectedMaxFps) {
        int safeDuration = Math.max(durationSeconds, 1);
        int safeExpectedFps = Math.max(expectedMaxFps, 120);
        int requestedCapacity = safeDuration * safeExpectedFps;
        int capacity = Math.min(Math.max(requestedCapacity, 2048), MAX_RING_CAPACITY);
        this.durationSeconds = safeDuration;
        this.durationNs = safeDuration * NANOS_PER_SECOND;
        this.frameTimesNs = new LongRingBuffer(capacity);
        this.heapSnapshotBytes = new long[Math.max(2, (safeDuration / 10) + 2)];
    }

    public void begin(long nowNs) {
        this.running = true;
        this.startNs = nowNs;
        this.endNs = nowNs;
        this.nextHeapSampleNs = nowNs;
        this.heapSamples = 0;
        this.gcCountStart = readGcCollectionCount();
        this.gcPauseMsStart = readGcPauseMillis();
        this.gcCountEnd = this.gcCountStart;
        this.gcPauseMsEnd = this.gcPauseMsStart;
        this.totalFrameCount = 0L;
        this.totalFrameTimeNs = 0L;
        this.frameTimesNs.clear();
        this.sortedDirty = true;
        this.sortedCache = new long[0];
    }

    public void recordFrame(long frameTimeNs, long nowNs) {
        if (!this.running || frameTimeNs <= 0L) {
            return;
        }
        this.frameTimesNs.add(frameTimeNs);
        this.totalFrameCount++;
        this.totalFrameTimeNs += frameTimeNs;
        this.sortedDirty = true;
        while (this.heapSamples < this.heapSnapshotBytes.length && nowNs >= this.nextHeapSampleNs) {
            this.heapSnapshotBytes[this.heapSamples++] = getUsedHeapBytes();
            this.nextHeapSampleNs += HEAP_INTERVAL_NS;
        }
    }

    public boolean shouldFinish(long nowNs) {
        return this.running && nowNs - this.startNs >= this.durationNs;
    }

    public void finish(long nowNs) {
        if (!this.running) {
            return;
        }
        this.running = false;
        this.endNs = nowNs;
        this.gcCountEnd = readGcCollectionCount();
        this.gcPauseMsEnd = readGcPauseMillis();
        if (this.heapSamples < this.heapSnapshotBytes.length) {
            this.heapSnapshotBytes[this.heapSamples++] = getUsedHeapBytes();
        }
    }

    public int getDurationSeconds() {
        return this.durationSeconds;
    }

    public long getGcCount() {
        long end = this.running ? readGcCollectionCount() : this.gcCountEnd;
        return Math.max(0L, end - this.gcCountStart);
    }

    public long getGcPauseMs() {
        long end = this.running ? readGcPauseMillis() : this.gcPauseMsEnd;
        return Math.max(0L, end - this.gcPauseMsStart);
    }

    public long[] getHeapSnapshotBytes() {
        return Arrays.copyOf(this.heapSnapshotBytes, this.heapSamples);
    }

    public long getTotalFrameCount() {
        return this.totalFrameCount;
    }

    @Override
    public double getAverageFps(int windowSeconds) {
        int samples = this.frameTimesNs.size();
        if (samples <= 0) {
            return 0.0D;
        }
        long windowNs = windowSeconds <= 0
            ? Long.MAX_VALUE
            : Math.max(1L, (long) windowSeconds) * NANOS_PER_SECOND;

        long accumulatedNs = 0L;
        int usedFrames = 0;
        for (int i = 0; i < samples; i++) {
            long frameNs = this.frameTimesNs.getFromNewest(i);
            accumulatedNs += frameNs;
            usedFrames++;
            if (accumulatedNs >= windowNs) {
                break;
            }
        }
        if (accumulatedNs <= 0L) {
            return 0.0D;
        }
        return (usedFrames * (double) NANOS_PER_SECOND) / (double) accumulatedNs;
    }

    @Override
    public double getFrameTimePercentile(double p) {
        long[] sorted = getSortedSamples();
        if (sorted.length == 0) {
            return 0.0D;
        }
        double clamped = p;
        if (clamped < 0.0D) {
            clamped = 0.0D;
        } else if (clamped > 1.0D) {
            clamped = 1.0D;
        }
        int index = (int) Math.ceil(clamped * sorted.length) - 1;
        if (index < 0) {
            index = 0;
        } else if (index >= sorted.length) {
            index = sorted.length - 1;
        }
        return sorted[index] / 1_000_000.0D;
    }

    private long[] getSortedSamples() {
        int samples = this.frameTimesNs.size();
        if (samples == 0) {
            return new long[0];
        }
        if (!this.sortedDirty && this.sortedCache.length == samples) {
            return this.sortedCache;
        }
        long[] copy = new long[samples];
        this.frameTimesNs.copyOldestToNewest(copy);
        Arrays.sort(copy);
        this.sortedCache = copy;
        this.sortedDirty = false;
        return this.sortedCache;
    }

    private static long getUsedHeapBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static long readGcCollectionCount() {
        long total = 0L;
        for (GarbageCollectorMXBean bean : GC_BEANS) {
            long value = bean.getCollectionCount();
            if (value > 0L) {
                total += value;
            }
        }
        return total;
    }

    private static long readGcPauseMillis() {
        long total = 0L;
        for (GarbageCollectorMXBean bean : GC_BEANS) {
            long value = bean.getCollectionTime();
            if (value > 0L) {
                total += value;
            }
        }
        return total;
    }
}
