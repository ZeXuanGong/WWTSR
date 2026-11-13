package com.wwtsr.pet;

/**
 * How long each movement lasts, in milliseconds.
 *
 * @param tapHoldMillis       how long a hand stays down after a tap
 * @param blinkIntervalMillis time between blinks
 * @param blinkDurationMillis how long the eyes stay closed
 */
public record PetTimings(long tapHoldMillis, long blinkIntervalMillis, long blinkDurationMillis) {

    public static final PetTimings DEFAULT = new PetTimings(220, 5_000, 250);

    public PetTimings {
        if (tapHoldMillis <= 0 || blinkIntervalMillis <= 0 || blinkDurationMillis <= 0) {
            throw new IllegalArgumentException("timings must be positive");
        }
        if (blinkDurationMillis >= blinkIntervalMillis) {
            throw new IllegalArgumentException("a blink must be shorter than the time between blinks");
        }
    }
}
