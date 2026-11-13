package com.wwtsr.input;

/** Told about each press and each letting go. May be called on any thread. */
@FunctionalInterface
public interface InputListener {

    void pressed(InputKind kind);

    /** What was pressed has been let go. A held key is reported once, when it comes up. */
    default void released(InputKind kind) {
    }
}
