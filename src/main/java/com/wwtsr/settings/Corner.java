package com.wwtsr.settings;

/** The corner of the screen the pet docks in. */
public enum Corner {
    TOP_LEFT(true, true),
    TOP_RIGHT(false, true),
    BOTTOM_LEFT(true, false),
    BOTTOM_RIGHT(false, false);

    private final boolean left;
    private final boolean top;

    Corner(boolean left, boolean top) {
        this.left = left;
        this.top = top;
    }

    public boolean isLeft() {
        return left;
    }

    public boolean isTop() {
        return top;
    }
}
