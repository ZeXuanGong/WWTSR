package com.wwtsr.pet;

/** How many taps there have been. Never negative, and stops at {@link Long#MAX_VALUE}. */
public final class TapCounter {

    private long count;

    public TapCounter(long start) {
        if (start < 0) {
            throw new IllegalArgumentException("a count can't be negative");
        }
        this.count = start;
    }

    public long count() {
        return count;
    }

    /** Takes {@code amount} off the count, never going below zero. */
    public void spend(long amount) {
        count = Math.max(0, count - Math.max(0, amount));
    }

    public void increment() {
        if (count < Long.MAX_VALUE) {
            count++;
        }
    }
}
