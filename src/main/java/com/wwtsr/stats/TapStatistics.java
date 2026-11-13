package com.wwtsr.stats;

import com.wwtsr.input.InputKind;
import java.util.Objects;

/** The running counts for this session, starting from what was saved last time. */
public final class TapStatistics {

    private long keyPresses;
    private long mouseClicks;
    private boolean unsaved;

    public TapStatistics(TapCounts start) {
        Objects.requireNonNull(start);
        this.keyPresses = start.keyPresses();
        this.mouseClicks = start.mouseClicks();
    }

    public void record(InputKind kind) {
        switch (kind) {
            case KEY -> keyPresses = next(keyPresses);
            case MOUSE_BUTTON -> mouseClicks = next(mouseClicks);
        }
        unsaved = true;
    }

    public TapCounts counts() {
        return new TapCounts(keyPresses, mouseClicks);
    }

    /** Whether anything was recorded since the last {@link #markSaved()}. */
    public boolean hasUnsaved() {
        return unsaved;
    }

    public void markSaved() {
        unsaved = false;
    }

    private static long next(long value) {
        return value < Long.MAX_VALUE ? value + 1 : value;
    }
}
