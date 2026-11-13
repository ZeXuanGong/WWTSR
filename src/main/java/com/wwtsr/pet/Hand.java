package com.wwtsr.pet;

/** Which hand the pet taps with. Taps alternate between the two. */
public enum Hand {
    LEFT,
    RIGHT;

    public Hand other() {
        return this == LEFT ? RIGHT : LEFT;
    }
}
