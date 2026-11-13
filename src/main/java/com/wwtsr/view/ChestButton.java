package com.wwtsr.view;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

/**
 * One chest beside the pet, in a speech bubble. When it can be opened the bubble holds a
 * little chest and takes a click: the bubble squashes and springs back, the chest's lid tips
 * open, and then the chest is opened. While it's waiting the bubble shows the time left, with
 * a rim that drains as the time runs down. It keeps its place when hidden.
 */
public final class ChestButton extends Pane {

    public static final double WIDTH = 62;
    public static final double HEIGHT = 44;

    // The press: when the lid tips, and how long the whole press lasts before the chest opens.
    private static final Duration LID_AFTER = Duration.millis(170);
    private static final Duration OPEN_AFTER = Duration.millis(700);
    private static final Duration LID_TIME = Duration.millis(280);
    private static final double LID_ANGLE = -14;
    private static final double SQUASH_MILLIS = 480;

    // The waiting bubble.
    private static final double WAIT_WIDTH = 64;
    private static final double WAIT_HEIGHT = 30;
    private static final double RIM = 3.6;

    private static final Color BUBBLE = Color.web("#ffd18c");
    private static final Color BUBBLE_SHADE = Color.web("#f0b56a");
    private static final Color PAPER = Color.web("#fdf9ed");
    private static final Color RIM_COLOUR = Color.web("#fed291");
    private static final Color COCOA = Color.web("#8e5e43");
    private static final Color WOOD = Color.web("#8a5a3c");
    private static final Color WOOD_LIGHT = Color.web("#a56f4b");

    private final Runnable onOpen;
    private final Group ready;
    private final Group waiting;
    private final Scale squash = new Scale(1, 1, WIDTH / 2, HEIGHT / 2);
    private final Rotate lid = new Rotate(0, 14, 11 + 7);
    private final Rectangle rimLine;
    private final double rimLength;
    private final Label time = new Label();
    private boolean canOpen;
    private boolean pressing;

    public ChestButton(Runnable onOpen) {
        this.onOpen = onOpen;
        setMinSize(WIDTH, HEIGHT);
        setPrefSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);
        setPickOnBounds(false);

        ready = readyBubble();
        ready.getTransforms().add(squash);
        ready.setEffect(new DropShadow(2, 0, 2, Color.rgb(0, 0, 0, 0.2)));
        ready.setCursor(Cursor.HAND);
        // Not the start of a drag, and not a click on the pet.
        ready.setOnMousePressed(e -> e.consume());
        ready.setOnMouseReleased(e -> e.consume());
        ready.setOnMouseClicked(e -> {
            if (e.isStillSincePress()) {
                press();
            }
            e.consume();
        });

        // The waiting bubble sits in the middle of the button's height.
        double top = (HEIGHT - WAIT_HEIGHT) / 2;
        Rectangle pill = pill(0, top, WAIT_WIDTH, WAIT_HEIGHT);
        pill.setFill(PAPER);
        rimLine = pill(RIM / 2, top + RIM / 2, WAIT_WIDTH - RIM, WAIT_HEIGHT - RIM);
        rimLine.setFill(null);
        rimLine.setStroke(RIM_COLOUR);
        rimLine.setStrokeWidth(RIM);
        rimLine.setStrokeType(StrokeType.CENTERED);
        rimLine.setStrokeLineCap(StrokeLineCap.BUTT);
        double r = (WAIT_HEIGHT - RIM) / 2;
        rimLength = 2 * (WAIT_WIDTH - RIM - 2 * r) + 2 * Math.PI * r;
        Polygon tail = new Polygon(10, top + WAIT_HEIGHT - 4, 6, top + WAIT_HEIGHT + 6, 20, top + WAIT_HEIGHT - 2);
        tail.setFill(PAPER);
        time.setTextFill(COCOA);
        time.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        time.setAlignment(Pos.CENTER);
        time.setMinSize(WAIT_WIDTH, WAIT_HEIGHT);
        time.setLayoutY(top);
        waiting = new Group(tail, pill, rimLine, time);
        waiting.setEffect(new DropShadow(2, 0, 2, Color.rgb(0, 0, 0, 0.2)));

        getChildren().addAll(waiting, ready);
        show(false, false, 0, 1);
    }

    /**
     * @param visible         whether the chest is drawn at all
     * @param openable        whether it can be opened now
     * @param remainingMillis the wait left when it can't
     * @param remainingPart   how much of the whole wait is left, 0–1
     */
    public void show(boolean visible, boolean openable, long remainingMillis, double remainingPart) {
        canOpen = visible && openable;
        setVisible(visible);
        // A press in progress finishes on the chest it started on.
        ready.setVisible(canOpen || pressing);
        waiting.setVisible(!ready.isVisible());
        time.setText(clock(remainingMillis));
        double part = Math.clamp(remainingPart, 0, 1);
        rimLine.setVisible(part > 0);
        rimLine.getStrokeDashArray().setAll(rimLength * part, rimLength);
    }

    /** Minutes and seconds, rounded up so it never shows 00:00 while still waiting. */
    static String clock(long millis) {
        long seconds = Math.max(0, (millis + 999) / 1_000);
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    /** The bubble squashes and springs back, the lid tips open, and then the chest is opened. */
    private void press() {
        if (!canOpen || pressing) {
            return;
        }
        pressing = true;
        Interpolator ease = Interpolator.EASE_OUT;
        new Timeline(
                frame(SQUASH_MILLIS * 0.35, ease, 1.14, 0.84),
                frame(SQUASH_MILLIS * 0.58, ease, 0.96, 1.05),
                frame(SQUASH_MILLIS * 0.78, ease, 1.03, 0.98),
                frame(SQUASH_MILLIS, ease, 1, 1)).play();
        Timeline tip = new Timeline(new KeyFrame(LID_TIME, new KeyValue(lid.angleProperty(), LID_ANGLE, ease)));
        tip.setDelay(LID_AFTER);
        tip.play();
        PauseTransition wait = new PauseTransition(OPEN_AFTER);
        wait.setOnFinished(e -> {
            pressing = false;
            lid.setAngle(0);
            onOpen.run();
        });
        wait.play();
    }

    private KeyFrame frame(double millis, Interpolator ease, double x, double y) {
        return new KeyFrame(Duration.millis(millis),
                new KeyValue(squash.xProperty(), x, ease), new KeyValue(squash.yProperty(), y, ease));
    }

    /** The bubble with the little chest in it. */
    private Group readyBubble() {
        // A rounded bubble with a tail at the bottom left, darker along its left and bottom.
        Shape shade = Shape.union(pill(2, 5, 50, 34), new Polygon(8, 32, 4, 43, 20, 37));
        shade.setFill(BUBBLE_SHADE);
        Shape face = Shape.union(pill(5, 4, 48, 32), new Polygon(11, 30, 8, 39, 20, 35));
        face.setFill(BUBBLE);
        Group bubble = new Group(shade, face);
        // Three short strokes off the top-right corner.
        double[][] strokes = {{53, 6, 57, 1}, {56, 11, 61, 9}, {56, 16, 61, 17}};
        for (double[] s : strokes) {
            Line stroke = new Line(s[0], s[1], s[2], s[3]);
            stroke.setStroke(BUBBLE);
            stroke.setStrokeWidth(2.4);
            stroke.setStrokeLineCap(StrokeLineCap.ROUND);
            bubble.getChildren().add(stroke);
        }
        bubble.getChildren().addAll(chestBody(), chestLid());
        return bubble;
    }

    /** The chest's box: 28 wide, in the bubble at (14, 11). */
    private static Node chestBody() {
        Rectangle box = new Rectangle(14, 11 + 8, 28, 13);
        box.setArcWidth(5);
        box.setArcHeight(5);
        box.setFill(WOOD);
        Rectangle lock = new Rectangle(14 + 11.5, 11 + 6, 5, 6);
        lock.setArcWidth(3);
        lock.setArcHeight(3);
        lock.setFill(Color.web("#5f6f86"));
        return new Group(box, lock);
    }

    /** The chest's lid, hinged at its left end so it can tip open. */
    private Node chestLid() {
        Rectangle top = new Rectangle(14, 11, 28, 8);
        top.setArcWidth(7);
        top.setArcHeight(7);
        top.setFill(WOOD_LIGHT);
        Group lidGroup = new Group(top);
        lidGroup.getTransforms().add(lid);
        return lidGroup;
    }

    private static Rectangle pill(double x, double y, double width, double height) {
        Rectangle pill = new Rectangle(x, y, width, height);
        pill.setArcWidth(height);
        pill.setArcHeight(height);
        return pill;
    }
}
