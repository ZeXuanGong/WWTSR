package com.wwtsr.view;

import com.wwtsr.menu.MenuEntry;
import com.wwtsr.platform.WindowEffects;
import com.wwtsr.settings.Corner;
import com.wwtsr.settings.DockRange;
import com.wwtsr.settings.Placement;
import com.wwtsr.settings.Settings;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * The pet's window: no frame, see-through, always on top. Fully transparent pixels let the
 * mouse through to whatever is underneath. Size, opacity, place and the system effects follow
 * the {@link Settings} it is given.
 *
 * <p>While it is dragged the pet stays on its screen; when it is dropped it settles inside the
 * screen's free area (above the taskbar), and what is remembered is its {@link Placement}.
 */
public final class PetWindow implements DragController.Limits {

    /** Gap between a docked pet and the screen's left, right and top edges, in pixels. */
    private static final double SIDE_GAP = 16;
    /** How a dropped pet rises to stand on the taskbar. */
    private static final Duration SETTLE_TIME = Duration.millis(340);
    private static final CubicBezier SETTLE_CURVE = new CubicBezier(0.22, 1.15, 0.36, 1);
    /** A move smaller than this isn't worth making. */
    private static final double SAME_PLACE = 0.5;

    private final Stage stage;
    private final PetView view;
    private final String title;
    private final WindowEffects effects;
    private final Consumer<Placement> onMoved;
    private final DragController drag;
    private Settings settings = Settings.DEFAULT;
    private ContextMenu openMenu;
    private boolean mouseThrough;
    private Animation settling;
    /** The pivot to keep through a change of size, when the pet was dragged somewhere. */
    private Point2D keptPivot;

    public PetWindow(Stage stage, PetView view, String title, WindowEffects effects, Runnable onClick,
            Consumer<Placement> onMoved, Supplier<List<MenuEntry>> menu) {
        this.stage = stage;
        this.view = view;
        this.title = title;
        this.effects = effects;
        this.onMoved = onMoved;
        Scene scene = new Scene(view);
        scene.setFill(Color.TRANSPARENT);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        stage.setTitle(title);
        stage.setScene(scene);

        drag = new DragController(stage, view, this, onClick);
        view.setOnContextMenuRequested(e -> {
            if (openMenu != null) {
                openMenu.hide();
            }
            // Built when asked for, so it always shows the current settings.
            openMenu = FxMenuRenderer.render(menu.get());
            openMenu.show(view, e.getScreenX(), e.getScreenY());
        });
    }

    public boolean isShowing() {
        return stage.isShowing();
    }

    /** Whether the pet's right-click menu is open. */
    public boolean menuOpen() {
        return openMenu != null && openMenu.isShowing();
    }

    public void show() {
        stage.show();
        fit();
    }

    public void hide() {
        if (openMenu != null) {
            openMenu.hide();
        }
        stage.hide();
    }

    /** Makes the window match {@code next}. */
    public void apply(Settings next) {
        boolean resized = next.scalePercent() != settings.scalePercent();
        if (resized && stage.isShowing() && next.hasCustomPosition() && settings.monitor() == next.monitor()) {
            // A dragged pet keeps its pivot where it is while it grows or shrinks.
            keptPivot = pivot();
        }
        settings = next;
        view.setScale(next.scalePercent() / 100.0);
        view.setFadeOnHover(next.fadeOnHover());
        stage.setOpacity(next.opacityPercent() / 100.0);
        if (stage.isShowing()) {
            fit();
        }
    }

    /**
     * Puts the pet back where it belongs if the screen changed under it (its size, the
     * taskbar). Does nothing while the pet is being dragged or is settling.
     */
    public void keepInPlace() {
        if (stage.isShowing() && !drag.isDragging() && settling == null && keptPivot == null) {
            place();
        }
    }

    /**
     * Sizes the window to the pet's dock, then places it. The size is the dock's own, not what
     * is drawn: soft shadows reach a little past the dock and must not make the window bigger.
     */
    private void fit() {
        stage.setWidth(Math.ceil(PetView.DOCK_WIDTH * view.getScale()));
        stage.setHeight(Math.ceil(PetView.DOCK_HEIGHT * view.getScale()));
        Platform.runLater(() -> {
            if (!stage.isShowing()) {
                return;
            }
            place();
            applyEffects();
        });
    }

    /**
     * Makes the mouse pass through the whole pet to whatever is underneath (or stops doing
     * so). While it does, the pet can't be clicked, dragged or right-clicked.
     */
    public void setMouseThrough(boolean through) {
        if (mouseThrough == through) {
            return;
        }
        mouseThrough = through;
        if (stage.isShowing()) {
            applyEffects();
        }
    }

    private void applyEffects() {
        effects.apply(title, mouseThrough, settings.showTaskbarIcon());
    }

    // ---- DragController.Limits ----

    @Override
    public boolean canDrag() {
        return !settings.lockPosition();
    }

    @Override
    public void dragStarting() {
        stopSettling();
    }

    /** While dragged the pet may go anywhere on its screen, over the taskbar too. */
    @Override
    public Point2D nearestAllowed(double x, double y) {
        DockRange screen = range(Screens.at(settings.monitor()).getBounds());
        Point2D p = pivotOf(x, y);
        return cornerOf(screen.clampX(p.getX()), screen.clampY(p.getY()));
    }

    /**
     * A dropped pet moves into the screen's free area: sideways at once, and up from behind
     * the taskbar with a little bounce. Its place there is what is remembered.
     */
    @Override
    public void dropped() {
        DockRange free = freeRange();
        Point2D p = pivot();
        double x = free.clampX(p.getX());
        double y = free.clampY(p.getY());
        Point2D corner = cornerOf(x, y);
        stage.setX(corner.getX());
        if (Math.abs(corner.getY() - stage.getY()) > SAME_PLACE) {
            settleTo(corner.getY());
        }
        onMoved.accept(free.placementOf(x, y));
    }

    // ---- placing ----

    private void place() {
        Point2D pivot;
        if (keptPivot != null) {
            DockRange screen = range(Screens.at(settings.monitor()).getBounds());
            pivot = new Point2D(screen.clampX(keptPivot.getX()), screen.clampY(keptPivot.getY()));
            keptPivot = null;
            moveTo(cornerOf(pivot.getX(), pivot.getY()));
            // The same spot is a different share of the room now that the pet's size changed.
            onMoved.accept(freeRange().placementOf(pivot.getX(), pivot.getY()));
            return;
        }
        if (settings.placement().isPresent()) {
            DockRange free = freeRange();
            Placement placement = settings.placement().get();
            moveTo(cornerOf(free.xOf(placement), free.yOf(placement)));
            return;
        }
        // In a corner the pet keeps a little away from the screen's sides and top, and stands
        // right on the bottom (the top of the taskbar).
        double w = PetView.DOCK_WIDTH * view.getScale();
        double h = PetView.DOCK_HEIGHT * view.getScale();
        Rectangle2D area = Screens.at(settings.monitor()).getVisualBounds();
        Corner corner = settings.corner();
        moveTo(new Point2D(corner.isLeft() ? area.getMinX() + SIDE_GAP : area.getMaxX() - w - SIDE_GAP,
                corner.isTop() ? area.getMinY() + SIDE_GAP : area.getMaxY() - h));
    }

    private void moveTo(Point2D corner) {
        if (Math.abs(stage.getX() - corner.getX()) > SAME_PLACE || Math.abs(stage.getY() - corner.getY()) > SAME_PLACE) {
            stopSettling();
            stage.setX(corner.getX());
            stage.setY(corner.getY());
        }
    }

    private void settleTo(double y) {
        stopSettling();
        DoubleProperty top = new SimpleDoubleProperty(stage.getY());
        top.addListener((observable, before, now) -> stage.setY(now.doubleValue()));
        Timeline rise = new Timeline(new KeyFrame(SETTLE_TIME, new KeyValue(top, y, SETTLE_CURVE)));
        rise.setOnFinished(e -> {
            settling = null;
            stage.setY(y);
        });
        settling = rise;
        rise.play();
    }

    private void stopSettling() {
        if (settling != null) {
            settling.stop();
            settling = null;
        }
    }

    /** The room for the pivot in the free part of the pet's screen (the taskbar left out). */
    private DockRange freeRange() {
        return range(Screens.at(settings.monitor()).getVisualBounds());
    }

    /**
     * The room for the pivot inside {@code area}: the whole dock stays in sideways, the pet's
     * feet stay above the bottom, and only the empty space over its head may pass the top.
     */
    private DockRange range(Rectangle2D area) {
        double size = view.getScale();
        return DockRange.inside(area.getMinX(), area.getMinY(), area.getMaxX(), area.getMaxY(),
                PetView.PIVOT_X * size, (PetView.DOCK_WIDTH - PetView.PIVOT_X) * size, PetView.CONTENT_HEIGHT * size);
    }

    /** Where the pivot is on the screen right now. */
    private Point2D pivot() {
        return pivotOf(stage.getX(), stage.getY());
    }

    private Point2D pivotOf(double cornerX, double cornerY) {
        return new Point2D(cornerX + PetView.PIVOT_X * view.getScale(), cornerY + PetView.PIVOT_Y * view.getScale());
    }

    private Point2D cornerOf(double pivotX, double pivotY) {
        return new Point2D(pivotX - PetView.PIVOT_X * view.getScale(), pivotY - PetView.PIVOT_Y * view.getScale());
    }
}
