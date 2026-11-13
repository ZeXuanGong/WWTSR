package com.wwtsr.view;

import com.wwtsr.assets.AssetPack;
import com.wwtsr.chest.Reward;
import com.wwtsr.collect.Accessory;
import com.wwtsr.collect.ItemType;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import com.wwtsr.collect.Rarity;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.beans.value.WritableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

/**
 * The picture and movement of a reward being revealed, on a {@value #SIZE}×{@value #SIZE}
 * stage. From back to front: rays that open and then turn (the two highest rarities), a glowing
 * outline (the highest), a rounded tile in the rarity's colour, the reward itself cut to the
 * tile's shape, a small mark for its kind, and on top sparkles and confetti that burst and
 * vanish. Under it, a line of text.
 *
 * <p>How it moves: everything drops in from above; the tile lands with a twist and a squash;
 * the reward jumps up out of it and settles; sparkles and confetti fly out and fade. The
 * higher the rarity, the more of these there are.
 */
public final class RewardEffect extends Pane {

    public static final double SIZE = 260;

    // Where things sit on the stage.
    private static final double CENTRE_X = 130;
    private static final double CENTRE_Y = 132;
    private static final double TILE_HALF = 58;
    private static final double RAYS_RADIUS = 102;
    /** Confetti flies out from a point below the tile's middle. */
    private static final double CONFETTI_ORIGIN_Y = 186;
    /** The text's top edge is 76% of the way down the stage. */
    private static final double LABEL_TOP = SIZE * 0.76;

    // How things move (milliseconds).
    private static final double DROP_MILLIS = 700;
    private static final double DROP_DISTANCE = 53;
    private static final double LAND_DELAY = 200;
    private static final double LAND_MILLIS = 560;
    private static final double RAYS_DELAY = 80;
    private static final double RAYS_MILLIS = 480;
    private static final double RAYS_TURN_MILLIS = 3_200;
    private static final double BURST_MILLIS = 800;
    private static final double LABEL_MILLIS = 450;

    private static final Interpolator SETTLE = new CubicBezier(0.16, 0.72, 0.28, 1);
    private static final Interpolator BOUNCE = new CubicBezier(0.18, 0.9, 0.22, 1.12);

    private static final Color CREAM = Color.web("#fff6e0");
    private static final Color INK = Color.web("#3b3340");

    private final AssetPack assets;
    private Animation playing;
    private Animation turning;

    public RewardEffect(AssetPack assets) {
        this.assets = assets;
        setMinSize(SIZE, SIZE);
        setPrefSize(SIZE, SIZE);
        setMaxSize(SIZE, SIZE);
        setMouseTransparent(true);
    }

    /**
     * Shows a reward and plays its reveal.
     *
     * @param text the line under the tile
     * @param big  whether the line is the larger "new kind of thing" style
     * @param pop  grow from the middle instead of dropping in from above (used inside a page)
     */
    public void play(Reward reward, String text, boolean big, boolean pop) {
        build(reward, text, big, pop);
        playing.play();
        if (turning != null) {
            turning.play();
        }
    }

    /** Shows a reward as it looks {@code at} that moment of its reveal, standing still. */
    public void showAt(Reward reward, String text, boolean big, boolean pop, Duration at) {
        build(reward, text, big, pop);
        playing.play();
        playing.pause();
        playing.jumpTo(at);
    }

    public void stop() {
        if (playing != null) {
            playing.stop();
        }
        if (turning != null) {
            turning.stop();
        }
        getChildren().clear();
    }

    // ---- building the picture and its movement ----

    private void build(Reward reward, String text, boolean big, boolean pop) {
        stop();
        Rarity rarity = reward.item().rarity();
        int level = rarity.ordinal() + 1;
        List<Animation> motion = new ArrayList<>();
        Group drop = new Group();

        if (level >= 3) {
            Group rays = rays(level == 4 ? Color.web("#ffe2a8") : Color.web("#e3c9ff"));
            Group opening = new Group(rays);
            drop.getChildren().add(opening);
            motion.add(raysOpen(opening));
            RotateTransition turn = new RotateTransition(Duration.millis(RAYS_TURN_MILLIS), rays);
            turn.setByAngle(360);
            turn.setInterpolator(Interpolator.LINEAR);
            turn.setCycleCount(Animation.INDEFINITE);
            turn.setDelay(Duration.millis(RAYS_DELAY));
            turning = turn;
        } else {
            turning = null;
        }
        if (level == 4) {
            Polygon outline = squircle(TILE_HALF + 9);
            outline.setFill(null);
            outline.setStroke(Color.web("#ffd447"));
            outline.setStrokeWidth(7);
            outline.setEffect(new GaussianBlur(9));
            drop.getChildren().add(outline);
        }

        Group tile = tile(rarity);
        Node prize = prize(reward);
        Node mark = mark(reward.item().type());
        drop.getChildren().addAll(tile, prize, mark);
        motion.add(tileLands(tile));
        motion.add(pop ? prizePops(prize) : prizeJumps(prize));

        Group sparkles = sparkles(rarity);
        drop.getChildren().add(sparkles);
        motion.add(burst(sparkles, 0.24, 0.5, CENTRE_Y));
        if (level >= 2) {
            Group confetti = confetti(rarity);
            drop.getChildren().add(confetti);
            motion.add(burst(confetti, 0.08, 0.6, CONFETTI_ORIGIN_Y));
        }
        motion.add(pop ? groupPops(drop) : groupDrops(drop));

        Text label = label(text, rarity, big);
        motion.add(labelAppears(label));

        getChildren().setAll(drop, label);
        playing = new ParallelTransition(motion.toArray(Animation[]::new));
    }

    /** A rounded square that bulges a little: a superellipse around the tile's middle. */
    private static Polygon squircle(double half) {
        Polygon shape = new Polygon();
        int points = 72;
        double power = 2.0 / 3.6;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            shape.getPoints().addAll(
                    CENTRE_X + half * Math.signum(cos) * Math.pow(Math.abs(cos), power),
                    CENTRE_Y + half * Math.signum(sin) * Math.pow(Math.abs(sin), power));
        }
        return shape;
    }

    private static Group tile(Rarity rarity) {
        Polygon rim = squircle(TILE_HALF);
        rim.setFill(CREAM);
        Group tile = new Group(rim);
        if (rarity == Rarity.EPIC) {
            // The rarest tile is striped in several colours.
            Color[] bands = {Color.web("#8fd9cf"), Color.web("#ffe6a6"), Color.web("#ffc38a"), Color.web("#ff9a8c")};
            Group stripes = new Group();
            for (int i = 0; i < bands.length; i++) {
                Rectangle band = new Rectangle(CENTRE_X - 120, CENTRE_Y + 36 - i * 34, 240, 36);
                band.setFill(bands[i]);
                stripes.getChildren().add(band);
            }
            stripes.getTransforms().add(new Rotate(-28, CENTRE_X, CENTRE_Y));
            Group clipped = new Group(stripes);
            clipped.setClip(squircle(TILE_HALF - 6));
            tile.getChildren().add(clipped);
        } else {
            Polygon face = squircle(TILE_HALF - 6);
            face.setFill(Color.web(rarity.colour()).interpolate(Color.WHITE, 0.55));
            tile.getChildren().add(face);
        }
        if (rarity.ordinal() >= Rarity.RARE.ordinal()) {
            tile.setEffect(new DropShadow(10, Color.rgb(254, 246, 221, 0.5)));
        }
        return tile;
    }

    /** The reward's picture, cut to the tile's shape. */
    private Node prize(Reward reward) {
        double face = (TILE_HALF - 6) * 2;
        ImageView picture;
        if (reward.item() instanceof Accessory accessory) {
            // Just the accessory, as large as fits: not the faint head its thumbnail sits on.
            Image layer = assets.accessory(accessory);
            picture = new ImageView(layer);
            picture.setViewport(drawnArea(layer));
            picture.setFitWidth(face * 0.86);
            picture.setFitHeight(face * 0.86);
        } else {
            double size = face * (reward.item().type() == ItemType.EMOTE ? 0.82 : 1.05);
            picture = new ImageView(assets.thumbnail(reward.item()));
            picture.setFitWidth(size);
            picture.setFitHeight(size);
        }
        picture.setPreserveRatio(true);
        picture.setX(CENTRE_X - picture.getLayoutBounds().getWidth() / 2);
        picture.setY(CENTRE_Y - picture.getLayoutBounds().getHeight() / 2);
        Group prize = new Group(picture);
        prize.setClip(squircle(TILE_HALF - 6));
        // Moves as a whole, clip and all, so the cut edge stays with the picture.
        return new Group(prize);
    }

    /** The part of a picture that has anything drawn in it, with a little room around. */
    private static Rectangle2D drawnArea(Image image) {
        PixelReader pixels = image.getPixelReader();
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int left = width;
        int top = height;
        int right = -1;
        int bottom = -1;
        if (pixels != null) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if ((pixels.getArgb(x, y) >>> 24) > 16) {
                        left = Math.min(left, x);
                        right = Math.max(right, x);
                        top = Math.min(top, y);
                        bottom = Math.max(bottom, y);
                    }
                }
            }
        }
        if (right < left) {
            return new Rectangle2D(0, 0, width, height);
        }
        int room = 3;
        left = Math.max(0, left - room);
        top = Math.max(0, top - room);
        right = Math.min(width - 1, right + room);
        bottom = Math.min(height - 1, bottom + room);
        return new Rectangle2D(left, top, right - left + 1, bottom - top + 1);
    }

    /** A small tab in the tile's bottom-left corner with a sign for the kind of reward. */
    private static Node mark(ItemType type) {
        double x = CENTRE_X - TILE_HALF + 6;
        double y = CENTRE_Y + TILE_HALF - 6 - 26;
        Rectangle tab = new Rectangle(x, y, 26, 26);
        tab.setArcWidth(16);
        tab.setArcHeight(16);
        tab.setFill(CREAM);
        return new Group(tab, TypeMark.of(type, x + 13, y + 13, Color.web("#b98b5e"), CREAM));
    }

    /** Thin wedges fanning out from the middle, bright at the centre and fading outwards. */
    private static Group rays(Color colour) {
        Group rays = new Group();
        RadialGradient glow = new RadialGradient(0, 0, CENTRE_X, CENTRE_Y, RAYS_RADIUS, false, CycleMethod.NO_CYCLE,
                new Stop(0, colour), new Stop(0.55, colour.deriveColor(0, 1, 1, 0.75)), new Stop(1, Color.TRANSPARENT));
        int count = 18;
        for (int i = 0; i < count; i++) {
            double wide = i % 2 == 0 ? 7 : 4;
            double angle = 360.0 * i / count + (i % 3) * 3;
            Polygon ray = new Polygon(CENTRE_X, CENTRE_Y,
                    CENTRE_X + RAYS_RADIUS, CENTRE_Y - RAYS_RADIUS * Math.tan(Math.toRadians(wide)),
                    CENTRE_X + RAYS_RADIUS, CENTRE_Y + RAYS_RADIUS * Math.tan(Math.toRadians(wide)));
            ray.setFill(glow);
            ray.setOpacity(i % 2 == 0 ? 1 : 0.7);
            ray.getTransforms().add(new Rotate(angle, CENTRE_X, CENTRE_Y));
            rays.getChildren().add(ray);
        }
        return rays;
    }

    /** Four-pointed sparkles scattered around the tile. */
    private static Group sparkles(Rarity rarity) {
        Color tint = Color.web(rarity.colour()).interpolate(Color.WHITE, 0.45);
        double[][] spots = {
                {-78, -44, 7}, {74, -52, 9}, {84, -30, 4}, {-64, 30, 5}, {70, 40, 6}, {-86, 6, 3}, {6, -66, 4},
                {-40, 54, 3}, {52, 58, 4},
        };
        Group sparkles = new Group();
        for (int i = 0; i < spots.length; i++) {
            double x = CENTRE_X + spots[i][0];
            double y = CENTRE_Y + spots[i][1];
            double r = spots[i][2];
            double thin = r * 0.28;
            Polygon star = new Polygon(x, y - r, x + thin, y - thin, x + r, y, x + thin, y + thin,
                    x, y + r, x - thin, y + thin, x - r, y, x - thin, y - thin);
            star.setFill(i % 2 == 0 ? Color.WHITE : tint);
            star.setEffect(new DropShadow(4, tint));
            sparkles.getChildren().add(star);
        }
        return sparkles;
    }

    /** Little curled strips of paper thrown out around the tile. */
    private static Group confetti(Rarity rarity) {
        Color main = Color.web(rarity.colour());
        Color[] colours = rarity == Rarity.EPIC
                ? new Color[] {Color.web("#ffc24d"), Color.web("#ff8a5c"), Color.web("#7cc7ff"), Color.web("#c58bea"),
                        Color.web("#7ed957")}
                : new Color[] {main, main.interpolate(Color.WHITE, 0.45), main.darker()};
        double[][] strips = {
                {-84, -34, 20}, {-58, -58, 200}, {-92, 10, 320}, {86, -30, 150}, {62, -60, 300}, {94, 12, 30},
                {-30, -70, 250}, {34, -72, 100},
        };
        Group confetti = new Group();
        for (int i = 0; i < strips.length; i++) {
            Arc strip = new Arc(CENTRE_X + strips[i][0], CENTRE_Y + strips[i][1], 9, 9, strips[i][2], 110);
            strip.setType(ArcType.OPEN);
            strip.setFill(null);
            strip.setStroke(colours[i % colours.length]);
            strip.setStrokeWidth(4);
            strip.setStrokeLineCap(StrokeLineCap.ROUND);
            confetti.getChildren().add(strip);
        }
        return confetti;
    }

    private static Text label(String text, Rarity rarity, boolean big) {
        Color colour = switch (rarity) {
            case COMMON -> Color.web("#cfe6ba");
            case UNCOMMON -> Color.web("#bac9e6");
            case RARE -> Color.web("#eec2e6");
            case EPIC -> Color.web("#f9998c");
        };
        Text label = new Text(text);
        label.setFont(Font.font("System", FontWeight.BOLD, big ? 22 : 17));
        label.setFill(colour);
        label.setStroke(INK);
        label.setStrokeWidth(0.6);
        label.setEffect(new DropShadow(2, Color.rgb(0, 0, 0, 0.6)));
        label.setX(CENTRE_X - label.getLayoutBounds().getWidth() / 2);
        label.setTextOrigin(VPos.TOP);
        label.setY(LABEL_TOP);
        label.setOpacity(0);
        return label;
    }

    // ---- movement ----

    /** Everything drops in from above while it appears. */
    private static Animation groupDrops(Group drop) {
        Translate fall = new Translate(0, -DROP_DISTANCE);
        drop.getTransforms().add(fall);
        drop.setOpacity(0);
        return new Timeline(new KeyFrame(Duration.millis(DROP_MILLIS),
                new KeyValue(fall.yProperty(), 0, SETTLE), new KeyValue(drop.opacityProperty(), 1, SETTLE)));
    }

    /** Everything grows from the middle, a little too far, and settles. */
    private static Animation groupPops(Group drop) {
        Scale grow = new Scale(0.42, 0.42, CENTRE_X, CENTRE_Y);
        drop.getTransforms().add(grow);
        drop.setOpacity(0);
        return new Timeline(
                frame(DROP_MILLIS * 0.62, SETTLE, grow.xProperty(), 1.06, grow.yProperty(), 1.06, drop.opacityProperty(), 1),
                frame(DROP_MILLIS, SETTLE, grow.xProperty(), 1, grow.yProperty(), 1));
    }

    /** The tile lands twisted and small, squashes flat, springs tall and settles. */
    private static Animation tileLands(Node tile) {
        Scale size = new Scale(0.75, 0.75, CENTRE_X, CENTRE_Y);
        Rotate twist = new Rotate(-20, CENTRE_X, CENTRE_Y);
        tile.getTransforms().addAll(twist, size);
        Timeline land = new Timeline(
                frame(LAND_MILLIS * 0.58, BOUNCE, size.xProperty(), 1.06, size.yProperty(), 0.94, twist.angleProperty(), 0),
                frame(LAND_MILLIS * 0.78, BOUNCE, size.xProperty(), 0.98, size.yProperty(), 1.02),
                frame(LAND_MILLIS, BOUNCE, size.xProperty(), 1, size.yProperty(), 1));
        land.setDelay(Duration.millis(LAND_DELAY));
        return land;
    }

    /** The reward starts low, small and twisted, jumps above its place, dips and settles. */
    private static Animation prizeJumps(Node prize) {
        double tall = TILE_HALF * 2;
        Translate lift = new Translate(0, tall * 0.156);
        Scale size = new Scale(0.58, 0.58, CENTRE_X, CENTRE_Y);
        Rotate twist = new Rotate(-20, CENTRE_X, CENTRE_Y);
        prize.getTransforms().addAll(lift, twist, size);
        Timeline jump = new Timeline(
                frame(LAND_MILLIS * 0.55, BOUNCE, lift.yProperty(), -tall * 0.085, size.xProperty(), 1.12,
                        size.yProperty(), 1.12, twist.angleProperty(), 0),
                frame(LAND_MILLIS * 0.78, BOUNCE, lift.yProperty(), tall * 0.021, size.xProperty(), 0.96,
                        size.yProperty(), 0.96),
                frame(LAND_MILLIS, BOUNCE, lift.yProperty(), 0, size.xProperty(), 1, size.yProperty(), 1));
        jump.setDelay(Duration.millis(LAND_DELAY));
        return jump;
    }

    /** The reward grows in place with a small twist. */
    private static Animation prizePops(Node prize) {
        Scale size = new Scale(0.42, 0.42, CENTRE_X, CENTRE_Y);
        Rotate twist = new Rotate(-12, CENTRE_X, CENTRE_Y);
        prize.getTransforms().addAll(twist, size);
        Timeline pop = new Timeline(
                frame(LAND_MILLIS * 0.55, BOUNCE, size.xProperty(), 1.04, size.yProperty(), 1.04, twist.angleProperty(), 0),
                frame(LAND_MILLIS * 0.78, BOUNCE, size.xProperty(), 0.96, size.yProperty(), 0.96),
                frame(LAND_MILLIS, BOUNCE, size.xProperty(), 1, size.yProperty(), 1));
        pop.setDelay(Duration.millis(LAND_DELAY));
        return pop;
    }

    /** The rays open from small and turned to full size. */
    private static Animation raysOpen(Node rays) {
        Scale size = new Scale(0.38, 0.38, CENTRE_X, CENTRE_Y);
        Rotate twist = new Rotate(-18, CENTRE_X, CENTRE_Y);
        rays.getTransforms().addAll(twist, size);
        Timeline open = new Timeline(frame(RAYS_MILLIS, Interpolator.EASE_OUT,
                size.xProperty(), 1, size.yProperty(), 1, twist.angleProperty(), 0));
        open.setDelay(Duration.millis(RAYS_DELAY));
        return open;
    }

    /**
     * Pieces fly outwards from a point and fade: they grow from {@code startScale} to full
     * size, are fully visible at {@code peak} of the way, and gone at the end.
     */
    private static Animation burst(Node pieces, double startScale, double peak, double originY) {
        Scale size = new Scale(startScale, startScale, CENTRE_X, originY);
        pieces.getTransforms().add(size);
        pieces.setOpacity(0);
        Timeline grow = new Timeline(frame(BURST_MILLIS, SETTLE, size.xProperty(), 1, size.yProperty(), 1));
        Timeline fade = new Timeline(
                frame(BURST_MILLIS * peak, SETTLE, pieces.opacityProperty(), 1),
                frame(BURST_MILLIS, SETTLE, pieces.opacityProperty(), 0));
        ParallelTransition burst = new ParallelTransition(grow, fade);
        burst.setDelay(Duration.millis(LAND_DELAY));
        return burst;
    }

    private static Animation labelAppears(Text label) {
        // It only fades in; it doesn't move.
        Timeline appear = new Timeline(frame(LABEL_MILLIS, Interpolator.EASE_OUT, label.opacityProperty(), 1));
        appear.setDelay(Duration.millis(LAND_DELAY));
        return appear;
    }

    /** A key frame at {@code millis} that brings each value (given as property, target pairs) to its target. */
    @SuppressWarnings("unchecked")
    private static KeyFrame frame(double millis, Interpolator easing, Object... propertyThenTarget) {
        KeyValue[] values = new KeyValue[propertyThenTarget.length / 2];
        for (int i = 0; i < values.length; i++) {
            values[i] = new KeyValue((WritableValue<Number>) propertyThenTarget[i * 2],
                    (Number) propertyThenTarget[i * 2 + 1], easing);
        }
        return new KeyFrame(Duration.millis(millis), values);
    }
}
