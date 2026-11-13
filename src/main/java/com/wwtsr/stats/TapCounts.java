package com.wwtsr.stats;

/**
 * How many presses there have been of each kind. Counts only: nothing about which keys.
 */
public record TapCounts(long keyPresses, long mouseClicks) {

    public static final TapCounts ZERO = new TapCounts(0, 0);

    public TapCounts {
        if (keyPresses < 0 || mouseClicks < 0) {
            throw new IllegalArgumentException("counts can't be negative");
        }
    }

    /** Both kinds together; stops at {@link Long#MAX_VALUE} instead of overflowing. */
    public long total() {
        long sum = keyPresses + mouseClicks;
        return sum < 0 ? Long.MAX_VALUE : sum;
    }
}
