package com.clientfn.benchmark;

import java.util.Arrays;

/**
 * Fixed-capacity ring buffer for primitive long samples.
 */
final class LongRingBuffer {
    private final long[] values;
    private int head;
    private int size;

    LongRingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        this.values = new long[capacity];
    }

    int capacity() {
        return this.values.length;
    }

    int size() {
        return this.size;
    }

    void clear() {
        this.head = 0;
        this.size = 0;
        Arrays.fill(this.values, 0L);
    }

    void add(long value) {
        this.values[this.head] = value;
        this.head = (this.head + 1) % this.values.length;
        if (this.size < this.values.length) {
            this.size++;
        }
    }

    long getFromNewest(int newestIndex) {
        if (newestIndex < 0 || newestIndex >= this.size) {
            throw new IndexOutOfBoundsException("newestIndex=" + newestIndex + ", size=" + this.size);
        }
        int idx = this.head - 1 - newestIndex;
        while (idx < 0) {
            idx += this.values.length;
        }
        return this.values[idx];
    }

    void copyOldestToNewest(long[] destination) {
        if (destination.length < this.size) {
            throw new IllegalArgumentException("destination too small");
        }
        int start = this.head - this.size;
        while (start < 0) {
            start += this.values.length;
        }
        for (int i = 0; i < this.size; i++) {
            destination[i] = this.values[(start + i) % this.values.length];
        }
    }
}
