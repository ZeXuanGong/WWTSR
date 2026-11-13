package com.wwtsr.view;

import com.wwtsr.assets.AssetPack;
import com.wwtsr.collect.Emote;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

/**
 * The emotes the pet has ready, as round buttons in an arc up the pet's left side: from beside
 * its hand up and over to the top of its head. Opening it, the buttons fly out of one spot at
 * the bottom left one after another, growing and spinning into place with a little overshoot;
 * closing sends them back in the opposite order. A press squashes a button like jelly: it
 * flattens while held, then springs wide and wobbles back.
 *
 * <p>Lay this pane with its top-left corner on the pet picture's top-left corner.
 */
public final class EmoteFan extends Pane {

    private static final double DOT = 32;
    private static final double PICTURE = 30;
    /** Where each button rests, relative to the pet picture's top-left corner; the first is lowest. */
    private static final double[][] PLACES = {{-10, 143}, {-21, 108}, {-18, 72}, {0, 40}, {28, 16}};
    /** Every button is moved right by at least this much. */
    private static final double INSET_MIN = 2;
    /** How far each button is turned when it starts to fly out. */
    private static final double[] SPINS = {-70, -95, -120, -145, -170};
    /** The spot all the buttons fly out of. */
    private static final double START_X = 6;
    private static final double START_Y = 165;
    private static final double START_SCALE = 0.15;
    private static final Duration FLY = Duration.millis(520);
    private static final Duration APPEAR = Duration.millis(280);
    private static final Duration STAGGER = Duration.millis(45);
    private static final Interpolator OUT = new CubicBezier(0.34, 1.45, 0.48, 1);
    private static final Interpolator BACK = new CubicBezier(0.4, 0, 0.6, 1);
    private static final Color RESTING = Color.rgb(253, 249, 237, 0.5);
    private static final Color HOVERED = Color.rgb(253, 249, 237, 1);

    /** One button and the transforms that move it. */
    private record Dot(Group node, Translate shift, Scale size, Rotate spin, Jelly jelly) {
    }

    private final AssetPack assets;
    private final Consumer<Emote> onPicked;
    private final List<Dot> dots = new ArrayList<>();
    private final AnimationTimer wobble;
    private ParallelTransition moving;
    private boolean open;
    private double inset = INSET_MIN;

    public EmoteFan(AssetPack assets, Consumer<Emote> onPicked) {
        this.assets = assets;
        this.onPicked = onPicked;
        // Only the buttons catch the mouse, not the empty space between them.
        setPickOnBounds(false);
        wobble = new AnimationTimer() {
            private long before;

            @Override
            public void handle(long now) {
                // A long gap between frames must not fling the springs.
                double seconds = before == 0 ? 0 : Math.min((now - before) / 1e9, 0.028);
                before = now;
                boolean moving = false;
                for (Dot dot : dots) {
                    moving |= dot.jelly().step(seconds);
                }
                if (!moving) {
                    before = 0;
                    stop();
                }
            }
        };
    }

    public boolean isOpen() {
        return open;
    }

    /**
     * Moves every button right by {@code value} pixels, so the fan stays on the screen when
     * the pet is at its left edge.
     */
    public void setInset(double value) {
        inset = Math.max(INSET_MIN, value);
        for (int i = 0; i < dots.size(); i++) {
            dots.get(i).node().setLayoutX(PLACES[i][0] + inset);
        }
    }

    public void setEmotes(List<Emote> emotes) {
        if (moving != null) {
            moving.stop();
        }
        dots.clear();
        getChildren().clear();
        for (int i = 0; i < Math.min(emotes.size(), PLACES.length); i++) {
            Dot dot = dot(emotes.get(i), i);
            dots.add(dot);
            getChildren().add(dot.node());
            place(dot, i, open);
        }
        if (dots.isEmpty()) {
            open = false;
        }
    }

    /** Opens or closes the fan, with the fly-out or fly-back. */
    public void setOpen(boolean shouldOpen) {
        if (open == shouldOpen || (shouldOpen && dots.isEmpty())) {
            return;
        }
        open = shouldOpen;
        if (moving != null) {
            moving.stop();
        }
        moving = new ParallelTransition();
        for (int i = 0; i < dots.size(); i++) {
            Dot dot = dots.get(i);
            // Out: lowest first. Back: highest first.
            Duration delay = STAGGER.multiply(open ? i : PLACES.length - 1 - i);
            Interpolator easing = open ? OUT : BACK;
            Timeline fly = new Timeline(new KeyFrame(FLY,
                    new KeyValue(dot.shift().xProperty(), open ? 0 : START_X - PLACES[i][0], easing),
                    new KeyValue(dot.shift().yProperty(), open ? 0 : START_Y - PLACES[i][1], easing),
                    new KeyValue(dot.size().xProperty(), open ? 1 : START_SCALE, easing),
                    new KeyValue(dot.size().yProperty(), open ? 1 : START_SCALE, easing),
                    new KeyValue(dot.spin().angleProperty(), open ? 0 : SPINS[i], easing)));
            fly.setDelay(delay);
            FadeTransition fade = new FadeTransition(APPEAR, dot.node());
            fade.setToValue(open ? 1 : 0);
            fade.setDelay(delay);
            dot.node().setMouseTransparent(!open);
            moving.getChildren().addAll(fly, fade);
        }
        moving.play();
    }

    /** Opens or closes the fan at once, with no movement. */
    public void setOpenNow(boolean shouldOpen) {
        if (moving != null) {
            moving.stop();
        }
        open = shouldOpen && !dots.isEmpty();
        for (int i = 0; i < dots.size(); i++) {
            place(dots.get(i), i, open);
        }
    }

    private void place(Dot dot, int index, boolean atRest) {
        dot.shift().setX(atRest ? 0 : START_X - PLACES[index][0]);
        dot.shift().setY(atRest ? 0 : START_Y - PLACES[index][1]);
        dot.size().setX(atRest ? 1 : START_SCALE);
        dot.size().setY(atRest ? 1 : START_SCALE);
        dot.spin().setAngle(atRest ? 0 : SPINS[index]);
        dot.node().setOpacity(atRest ? 1 : 0);
        dot.node().setMouseTransparent(!atRest);
    }

    private Dot dot(Emote emote, int index) {
        double half = DOT / 2;
        Circle back = new Circle(half, half, half, RESTING);
        // A soft shadow along the lower left and a glint at the upper right make it look round.
        Arc shadow = new Arc(half, half, half - 2, half - 2, 190, 100);
        shadow.setType(ArcType.OPEN);
        shadow.setFill(null);
        shadow.setStroke(Color.rgb(120, 110, 100, 0.45));
        shadow.setStrokeWidth(2.2);
        shadow.setStrokeLineCap(StrokeLineCap.ROUND);
        Arc glint = new Arc(half, half, half - 3, half - 3, 35, 45);
        glint.setType(ArcType.OPEN);
        glint.setFill(null);
        glint.setStroke(Color.WHITE);
        glint.setStrokeWidth(2);
        glint.setStrokeLineCap(StrokeLineCap.ROUND);
        Group button = new Group(back, shadow, glint);
        button.setEffect(new DropShadow(2, 0, 2, Color.rgb(0, 0, 0, 0.2)));

        ImageView picture = new ImageView(assets.emote(emote));
        picture.setFitWidth(PICTURE);
        picture.setFitHeight(PICTURE);
        picture.setX((DOT - PICTURE) / 2);
        picture.setY((DOT - PICTURE) / 2);

        Group node = new Group(button, picture);
        node.setLayoutX(PLACES[index][0] + inset);
        node.setLayoutY(PLACES[index][1]);
        node.setCursor(Cursor.HAND);

        Translate shift = new Translate();
        Rotate spin = new Rotate(0, half, half);
        Scale size = new Scale(1, 1, half, half);
        node.getTransforms().addAll(shift, spin, size);
        Jelly jelly = new Jelly(button, picture);

        node.hoverProperty().addListener((o, was, hovering) -> back.setFill(hovering ? HOVERED : RESTING));
        node.setOnMousePressed(e -> {
            jelly.press();
            wobble.start();
            // The pet under the button must not take this as the start of a drag or a click.
            e.consume();
        });
        node.setOnMouseReleased(e -> {
            jelly.release();
            wobble.start();
            e.consume();
        });
        node.setOnMouseClicked(e -> {
            if (e.isStillSincePress()) {
                onPicked.accept(emote);
            }
            e.consume();
        });
        return new Dot(node, shift, size, spin, jelly);
    }

    /**
     * The jelly of one button: three springs (width, height and tilt) pulled towards targets
     * that a press and a release move. Held, the button is flat and narrow; let go, it swells
     * past its size for a moment and then settles. The picture inside shifts and stretches a
     * little the other way.
     */
    private static final class Jelly {

        /** How long a press lasts at least before the release counts. */
        private static final double MIN_HOLD_MILLIS = 80;
        private static final double SWELL_MILLIS = 115;

        private final Scale buttonSize = new Scale(1, 1, DOT / 2, DOT * 0.62);
        private final Rotate buttonTilt = new Rotate(0, DOT / 2, DOT * 0.62);
        private final Scale pictureSize = new Scale(1, 1, DOT / 2, DOT * 0.78);
        private final Translate pictureShift = new Translate();
        private double x = 1;
        private double y = 1;
        private double r;
        private double vx;
        private double vy;
        private double vr;
        private double targetX = 1;
        private double targetY = 1;
        private double targetR;
        private long pressedAt;
        private boolean held;
        private PauseTransition later;

        Jelly(Group button, ImageView picture) {
            button.getTransforms().addAll(buttonTilt, buttonSize);
            picture.getTransforms().addAll(pictureShift, pictureSize);
        }

        void press() {
            cancelLater();
            held = true;
            pressedAt = System.nanoTime();
            targetX = 0.86;
            targetY = 0.72;
            targetR = -0.025;
            vy -= 0.8;
        }

        void release() {
            if (!held) {
                return;
            }
            held = false;
            double heldMillis = (System.nanoTime() - pressedAt) / 1e6;
            after(Math.max(0, MIN_HOLD_MILLIS - heldMillis), () -> {
                targetX = 1.48;
                targetY = 1.2;
                targetR = 0.035;
                vx += 2.4;
                vy += 3.3;
                vr += 0.32;
                after(SWELL_MILLIS, () -> {
                    targetX = 1;
                    targetY = 1;
                    targetR = 0;
                });
            });
        }

        /** Moves the springs on by {@code seconds}. Returns whether anything is still moving. */
        boolean step(double seconds) {
            double[] sx = spring(x, vx, targetX, 245, 13.2, seconds);
            double[] sy = spring(y, vy, targetY, 285, 14, seconds);
            double[] sr = spring(r, vr, targetR, 205, 12.5, seconds);
            x = sx[0];
            vx = sx[1];
            y = sy[0];
            vy = sy[1];
            r = sr[0];
            vr = sr[1];

            double wide = Math.clamp(x, 0.72, 1.72);
            double tall = Math.clamp(y, 0.62, 1.55);
            double tilt = Math.clamp(r + (vx - vy) * 0.0025, -0.075, 0.075);
            buttonSize.setX(wide);
            buttonSize.setY(tall);
            buttonTilt.setAngle(Math.toDegrees(tilt));
            pictureSize.setX(1 - (wide - 1) * 0.16);
            pictureSize.setY(1 - (tall - 1) * 0.13);
            pictureShift.setY((1 - tall) * 3.5 - vy * 0.21);

            boolean still = Math.abs(x - targetX) < 0.001 && Math.abs(y - targetY) < 0.001
                    && Math.abs(r - targetR) < 0.001 && Math.abs(vx) < 0.01 && Math.abs(vy) < 0.01
                    && Math.abs(vr) < 0.01;
            return held || later != null || !still;
        }

        /** One step of a damped spring: position and speed after {@code seconds}. */
        private static double[] spring(double value, double speed, double target, double stiffness, double damping,
                double seconds) {
            double pull = (target - value) * stiffness - speed * damping;
            speed += pull * seconds;
            value += speed * seconds;
            return new double[] {value, speed};
        }

        private void after(double millis, Runnable action) {
            cancelLater();
            PauseTransition wait = new PauseTransition(Duration.millis(Math.max(1, millis)));
            later = wait;
            wait.setOnFinished(e -> {
                if (later == wait) {
                    later = null;
                }
                action.run();
            });
            wait.play();
        }

        private void cancelLater() {
            if (later != null) {
                later.stop();
                later = null;
            }
        }
    }
}
