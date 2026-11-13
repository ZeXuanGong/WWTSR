package com.wwtsr.view;

import javafx.animation.Interpolator;

/**
 * An easing curve given by two control points, like CSS's {@code cubic-bezier(x1, y1, x2, y2)}.
 * Unlike JavaFX's own spline, the {@code y} values may go outside 0–1, which is what makes a
 * movement overshoot and settle back.
 */
public final class CubicBezier extends Interpolator {

    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;

    /**
     * @param x1 first control point's time, 0–1
     * @param x2 second control point's time, 0–1
     */
    public CubicBezier(double x1, double y1, double x2, double y2) {
        this.x1 = Math.clamp(x1, 0, 1);
        this.y1 = y1;
        this.x2 = Math.clamp(x2, 0, 1);
        this.y2 = y2;
    }

    @Override
    protected double curve(double t) {
        if (t <= 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        // The curve's x only ever grows, so the point with x = t is found by halving.
        double low = 0;
        double high = 1;
        for (int i = 0; i < 30; i++) {
            double middle = (low + high) / 2;
            if (bezier(middle, x1, x2) < t) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return bezier((low + high) / 2, y1, y2);
    }

    /** One coordinate of the curve from 0 to 1 with control values {@code a} and {@code b}. */
    private static double bezier(double u, double a, double b) {
        double v = 1 - u;
        return 3 * v * v * u * a + 3 * v * u * u * b + u * u * u;
    }
}
