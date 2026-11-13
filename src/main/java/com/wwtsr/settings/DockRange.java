package com.wwtsr.settings;

/**
 * The room the pet's pivot (the middle of its bottom edge) can move in, so that the whole pet
 * stays inside an area of the screen. When the area is too small for the pet, the room is the
 * single middle point. Immutable.
 *
 * @param minX the pivot's leftmost place
 * @param maxX the pivot's rightmost place
 * @param minY the pivot's highest place
 * @param maxY the pivot's lowest place
 */
public record DockRange(double minX, double maxX, double minY, double maxY) {

    public DockRange {
        if (minX > maxX) {
            minX = (minX + maxX) / 2;
            maxX = minX;
        }
        if (minY > maxY) {
            minY = (minY + maxY) / 2;
            maxY = minY;
        }
    }

    /**
     * The room inside the area from ({@code left}, {@code top}) to ({@code right},
     * {@code bottom}), for a pet that reaches {@code leftOfPivot} and {@code rightOfPivot} to
     * the sides of its pivot and {@code abovePivot} over it.
     */
    public static DockRange inside(double left, double top, double right, double bottom,
            double leftOfPivot, double rightOfPivot, double abovePivot) {
        return new DockRange(left + leftOfPivot, right - rightOfPivot, top + abovePivot, bottom);
    }

    public double clampX(double x) {
        return Math.min(maxX, Math.max(minX, x));
    }

    public double clampY(double y) {
        return Math.min(maxY, Math.max(minY, y));
    }

    /** The pivot point ({@code x}, {@code y}) as a place in this room. */
    public Placement placementOf(double x, double y) {
        return new Placement(Placement.Axis.of(x, minX, maxX), Placement.Axis.of(y, minY, maxY));
    }

    public double xOf(Placement placement) {
        return placement.x().in(minX, maxX);
    }

    public double yOf(Placement placement) {
        return placement.y().in(minY, maxY);
    }
}
