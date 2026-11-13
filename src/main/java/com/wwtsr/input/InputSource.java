package com.wwtsr.input;

import java.util.Map;

/**
 * Where presses come from. The app only needs to know that something was pressed, so this is
 * all it sees of the operating system's input.
 */
public interface InputSource {

    /**
     * Starts reporting presses to {@code listener}.
     *
     * @return whether presses anywhere on the computer will be reported; {@code false} means
     *         this source can't listen here and the caller should fall back to its own window
     */
    boolean start(InputListener listener);

    /**
     * Sets the shortcuts: each runs its action whenever its key is pressed anywhere with
     * exactly its Ctrl, Alt, Shift and Win held. Replaces the earlier set; an empty map
     * switches shortcuts off. The keys still reach the program being used. Actions may run on
     * any thread.
     */
    default void setHotkeys(Map<Shortcut, Runnable> actions) {
    }

    /**
     * Whether the source seems to have stopped hearing: the computer has had input lately and
     * the source heard none of it. {@code false} when it can't tell.
     */
    default boolean seemsDeaf() {
        return false;
    }

    /** Stops and starts again with the same listener and shortcut keys. */
    default void restart() {
    }

    /** Stops reporting. Safe to call when not started. */
    void stop();

    /** The best source for the system the app is running on. */
    static InputSource forThisSystem() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("windows") ? new GlobalInputHook() : new NoInputSource();
    }
}
