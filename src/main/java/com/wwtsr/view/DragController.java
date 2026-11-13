package com.wwtsr.view;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * Lets the pet be dragged around with the left mouse button. A press that doesn't move is a
 * click; a finished drag is reported so the window can settle where it was dropped. Where the
 * window may go while it is dragged is the {@link Limits}' decision, not this class's.
 */
public final class DragController {

    /** What the controller asks and tells the window it drags. */
    public interface Limits {

        /** Asked on each movement; while it says no, the pet stays where it is. */
        boolean canDrag();

        /** The place nearest to ({@code x}, {@code y}) the window's corner may be dragged to. */
        Point2D nearestAllowed(double x, double y);

        /** A press began: it will move the window, or turn out to be a click. */
        void dragStarting();

        /** The window was dropped where it is now. */
        void dropped();
    }

    /** Movement of up to this many pixels each way still counts as a click. */
    private static final double CLICK_SLOP = 4;

    private final Stage stage;
    private final Limits limits;
    private final Runnable onClick;
    private double pressScreenX;
    private double pressScreenY;
    private double stageStartX;
    private double stageStartY;
    private boolean pressed;
    private boolean dragging;

    public DragController(Stage stage, Node handle, Limits limits, Runnable onClick) {
        this.stage = stage;
        this.limits = limits;
        this.onClick = onClick;
        handle.setOnMousePressed(this::pressed);
        handle.setOnMouseDragged(this::dragged);
        handle.setOnMouseReleased(this::released);
    }

    /** Whether the pet is being moved right now. */
    public boolean isDragging() {
        return dragging;
    }

    private void pressed(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY) {
            return;
        }
        limits.dragStarting();
        pressScreenX = e.getScreenX();
        pressScreenY = e.getScreenY();
        stageStartX = stage.getX();
        stageStartY = stage.getY();
        pressed = true;
        dragging = false;
    }

    private void dragged(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY || !pressed || !limits.canDrag()) {
            return;
        }
        double dx = e.getScreenX() - pressScreenX;
        double dy = e.getScreenY() - pressScreenY;
        if (!dragging && Math.abs(dx) <= CLICK_SLOP && Math.abs(dy) <= CLICK_SLOP) {
            return;
        }
        dragging = true;
        Point2D to = limits.nearestAllowed(stageStartX + dx, stageStartY + dy);
        stage.setX(to.getX());
        stage.setY(to.getY());
    }

    private void released(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY || !pressed) {
            return;
        }
        pressed = false;
        boolean moved = dragging;
        dragging = false;
        if (moved) {
            limits.dropped();
        } else {
            onClick.run();
        }
    }
}
