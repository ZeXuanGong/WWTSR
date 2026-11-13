package com.wwtsr.app;

/**
 * Lets something through only when enough time has passed since it last got through. Callers
 * hand in the time, so it can be tested without waiting. Use from one thread.
 */
final class Debounce {

    private final long gapMillis;
    private long lastMillis;
    private boolean passedBefore;

    Debounce(long gapMillis) {
        if (gapMillis < 0) {
            throw new IllegalArgumentException("a gap can't be negative");
        }
        this.gapMillis = gapMillis;
    }

    /** Whether it may happen at {@code nowMillis}. Saying yes starts the wait again. */
    boolean pass(long nowMillis) {
        if (passedBefore && nowMillis - lastMillis < gapMillis) {
            return false;
        }
        passedBefore = true;
        lastMillis = nowMillis;
        return true;
    }
}
