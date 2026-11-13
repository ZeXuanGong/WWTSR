package com.wwtsr.input;

import java.util.BitSet;

/**
 * Tells a fresh key press from the repeats the system sends while a key is held, so holding a
 * key counts once. It remembers only which keys are down right now, never what was typed.
 */
public final class KeyRepeatFilter {

    private static final int KEYS = 256;

    private final BitSet down = new BitSet(KEYS);

    /** A key-down message arrived. Returns whether it is a new press (not a repeat). */
    public boolean press(int keyCode) {
        if (keyCode < 0 || keyCode >= KEYS) {
            return true;
        }
        boolean wasDown = down.get(keyCode);
        down.set(keyCode);
        return !wasDown;
    }

    /**
     * A key-up message arrived. Returns whether a press of that key was reported before, so
     * every press is matched by exactly one release.
     */
    public boolean release(int keyCode) {
        if (keyCode < 0 || keyCode >= KEYS) {
            return true;
        }
        boolean wasDown = down.get(keyCode);
        down.clear(keyCode);
        return wasDown;
    }

    /** Whether that key is held right now. */
    public boolean isDown(int keyCode) {
        return keyCode >= 0 && keyCode < KEYS && down.get(keyCode);
    }
}
