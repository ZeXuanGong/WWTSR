package com.wwtsr.pet;

/** Closes the eyes for a moment at a steady interval. */
public final class Blinker {

    private final PetTimings timings;
    private long nextBlinkAtMillis;
    private long reopenAtMillis;
    private boolean closed;

    public Blinker(PetTimings timings, long startMillis) {
        this.timings = timings;
        this.nextBlinkAtMillis = startMillis + timings.blinkIntervalMillis();
    }

    public HeadPose pose() {
        return closed ? HeadPose.EYES_CLOSED : HeadPose.EYES_OPEN;
    }

    /** Moves time on. Returns whether the pose changed. */
    public boolean tick(long nowMillis) {
        boolean before = closed;
        if (closed && nowMillis >= reopenAtMillis) {
            closed = false;
        }
        if (!closed && nowMillis >= nextBlinkAtMillis) {
            reopenAtMillis = nextBlinkAtMillis + timings.blinkDurationMillis();
            // A blink that was due long ago (the computer slept) is skipped, not shown late.
            closed = nowMillis < reopenAtMillis;
            nextBlinkAtMillis = nowMillis + timings.blinkIntervalMillis();
        }
        return before != closed;
    }
}
