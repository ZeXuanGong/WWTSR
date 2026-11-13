package com.wwtsr.settings;

import java.util.Objects;

/**
 * Where the person dragged the pet to, kept as a share of the room it had rather than as a
 * point on the screen, so the pet lands in the matching place after the screen's size, the
 * taskbar or the screen itself changes. Immutable.
 *
 * @param x the place from left to right
 * @param y the place from top to bottom
 */
public record Placement(Axis x, Axis y) {

    public Placement {
        Objects.requireNonNull(x);
        Objects.requireNonNull(y);
    }

    /**
     * A place along one direction.
     *
     * @param ratio   how far along the room, from 0 (the start) to 1 (the end)
     * @param minEdge the pet was against the start, and stays against it
     * @param maxEdge the pet was against the end, and stays against it
     */
    public record Axis(double ratio, boolean minEdge, boolean maxEdge) {

        /** Within this many pixels of an end counts as against it. */
        public static final double EDGE = 3;
        /** Room narrower than this has no "how far along". */
        private static final double NO_ROOM = 0.1;

        public Axis {
            ratio = Double.isFinite(ratio) ? Math.clamp(ratio, 0.0, 1.0) : 0;
        }

        /** The place of {@code value} in the room between {@code a} and {@code b}. */
        public static Axis of(double value, double a, double b) {
            double min = Math.min(a, b);
            double max = Math.max(a, b);
            double inside = Math.min(max, Math.max(min, value));
            double room = max - min;
            return new Axis(room > NO_ROOM ? (inside - min) / room : 0, inside - min <= EDGE, max - inside <= EDGE);
        }

        /** The point this place is at in the room between {@code a} and {@code b}. */
        public double in(double a, double b) {
            double min = Math.min(a, b);
            double max = Math.max(a, b);
            if (maxEdge) {
                return max;
            }
            return minEdge ? min : min + ratio * (max - min);
        }
    }
}
