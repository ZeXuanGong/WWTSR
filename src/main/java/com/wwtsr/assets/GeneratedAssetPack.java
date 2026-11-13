package com.wwtsr.assets;

import com.wwtsr.collect.Accessory;
import com.wwtsr.collect.Collectible;
import com.wwtsr.collect.Emote;
import com.wwtsr.collect.Role;
import com.wwtsr.pet.BodyPose;
import com.wwtsr.pet.HeadPose;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

/**
 * Placeholder pictures drawn in code: four round little animals whose hands tap on the counter bar, things
 * for them to wear and faces for their emotes. Original artwork, made so the app has something
 * to show before real pictures are supplied. Must be created on the JavaFX thread (it draws on
 * canvases and takes snapshots).
 *
 * <p>Every role shares one head shape (x 34–166, y 24–128, eyes near y 73), so one drawing of
 * each accessory fits them all.
 */
public final class GeneratedAssetPack implements AssetPack {

    private static final double W = 200;
    private static final double H = 200;
    private static final double EMOTE = 80;

    private static final Color OUTLINE = Color.web("#3b3340");
    private static final Color BELLY = Color.web("#f4fbf6");
    private static final Color CHEEK = Color.web("#ffb3b8");

    /** A role's colours. */
    private record Palette(Color fur, Color dark) {
    }

    private static final Map<Role, Palette> PALETTES = new EnumMap<>(Map.of(
            Role.BEAR, new Palette(Color.web("#9fe0cf"), Color.web("#74c7b3")),
            Role.CAT, new Palette(Color.web("#ffcf94"), Color.web("#f0a55a")),
            Role.BUNNY, new Palette(Color.web("#fde8f0"), Color.web("#f2b8cd")),
            Role.FROG, new Palette(Color.web("#b4e67a"), Color.web("#7cc043"))));

    private final Map<Role, Map<BodyPose, Image>> bodies = new EnumMap<>(Role.class);
    private final Map<Role, Map<HeadPose, Image>> heads = new EnumMap<>(Role.class);
    private final Map<Accessory, Image> accessories = new EnumMap<>(Accessory.class);
    private final Map<Emote, Image> emotes = new EnumMap<>(Emote.class);
    private final Map<Collectible, Image> thumbnails = new HashMap<>();

    public GeneratedAssetPack() {
        for (Role role : Role.values()) {
            Map<BodyPose, Image> roleBodies = new EnumMap<>(BodyPose.class);
            for (BodyPose pose : BodyPose.values()) {
                roleBodies.put(pose, render(W, H, g -> drawBody(g, role, pose)));
            }
            bodies.put(role, roleBodies);
            Map<HeadPose, Image> roleHeads = new EnumMap<>(HeadPose.class);
            for (HeadPose pose : HeadPose.values()) {
                roleHeads.put(pose, render(W, H, g -> drawHead(g, role, pose)));
            }
            heads.put(role, roleHeads);
            thumbnails.put(role, crop(roleHeads.get(HeadPose.EYES_OPEN)));
        }
        Image plainHead = heads.get(Role.STARTER).get(HeadPose.EYES_OPEN);
        for (Accessory accessory : Accessory.values()) {
            Image layer = render(W, H, g -> drawAccessory(g, accessory));
            accessories.put(accessory, layer);
            // Shown on a faint head, so it's clear where it goes.
            thumbnails.put(accessory, crop(render(W, H, g -> {
                g.setGlobalAlpha(0.3);
                g.drawImage(plainHead, 0, 0);
                g.setGlobalAlpha(1);
                g.drawImage(layer, 0, 0);
            })));
        }
        for (Emote emote : Emote.values()) {
            Image face = render(EMOTE, EMOTE, g -> drawEmote(g, emote));
            emotes.put(emote, face);
            thumbnails.put(emote, face);
        }
    }

    @Override
    public double width() {
        return W;
    }

    @Override
    public double height() {
        return H;
    }

    @Override
    public Image body(Role role, BodyPose pose) {
        return bodies.get(role).get(pose);
    }

    @Override
    public Image head(Role role, HeadPose pose) {
        return heads.get(role).get(pose);
    }

    @Override
    public Image accessory(Accessory accessory) {
        return accessories.get(accessory);
    }

    @Override
    public Image emote(Emote emote) {
        return emotes.get(emote);
    }

    @Override
    public Image thumbnail(Collectible item) {
        return thumbnails.get(item);
    }

    @Override
    public Image icon() {
        return thumbnails.get(Role.STARTER);
    }

    // ---- drawing ----

    private interface Painter {
        void paint(GraphicsContext g);
    }

    private static Image render(double width, double height, Painter painter) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setLineWidth(4);
        g.setLineCap(StrokeLineCap.ROUND);
        g.setStroke(OUTLINE);
        painter.paint(g);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return canvas.snapshot(params, null);
    }

    /** The head and whatever is on it, without the empty space below. */
    private static Image crop(Image layer) {
        return new WritableImage(layer.getPixelReader(), 20, 0, 160, 160);
    }

    private static void drawBody(GraphicsContext g, Role role, BodyPose pose) {
        Palette p = PALETTES.get(role);
        // Torso.
        oval(g, p.fur(), 52, 96, 96, 92);
        oval(g, BELLY, 72, 116, 56, 60);
        // Hands: held up, or down on the counter bar, which covers the picture from y = 179.
        drawHand(g, p, 48, pose == BodyPose.LEFT_DOWN);
        drawHand(g, p, 152, pose == BodyPose.RIGHT_DOWN);
    }

    private static void drawHand(GraphicsContext g, Palette p, double centreX, boolean down) {
        double y = down ? 158 : 138;
        oval(g, p.dark(), centreX - 17, y, 34, 26);
        if (down) {
            // Little lines that show the tap.
            g.strokeLine(centreX - 26, y - 8, centreX - 32, y - 16);
            g.strokeLine(centreX, y - 12, centreX, y - 22);
            g.strokeLine(centreX + 26, y - 8, centreX + 32, y - 16);
        }
    }

    private static void drawHead(GraphicsContext g, Role role, HeadPose pose) {
        Palette p = PALETTES.get(role);
        // Ears, behind the head.
        switch (role) {
            case BEAR -> {
                oval(g, p.dark(), 38, 14, 40, 40);
                oval(g, p.dark(), 122, 14, 40, 40);
            }
            case CAT -> {
                polygon(g, p.dark(), 40, 60, 46, 8, 86, 36);
                polygon(g, p.dark(), 160, 60, 154, 8, 114, 36);
            }
            case BUNNY -> {
                oval(g, p.fur(), 56, 0, 26, 62);
                oval(g, p.fur(), 118, 0, 26, 62);
                g.setFill(p.dark());
                g.fillOval(63, 10, 12, 40);
                g.fillOval(125, 10, 12, 40);
            }
            case FROG -> {
                oval(g, p.fur(), 50, 8, 40, 40);
                oval(g, p.fur(), 110, 8, 40, 40);
            }
        }
        // Head.
        oval(g, p.fur(), 34, 24, 132, 104);
        // Cheeks.
        g.setFill(CHEEK);
        g.fillOval(52, 84, 20, 12);
        g.fillOval(128, 84, 20, 12);
        // Eyes.
        if (pose == HeadPose.EYES_OPEN) {
            g.setFill(OUTLINE);
            g.fillOval(68, 64, 14, 18);
            g.fillOval(118, 64, 14, 18);
            g.setFill(Color.WHITE);
            g.fillOval(72, 67, 5, 5);
            g.fillOval(122, 67, 5, 5);
        } else {
            g.strokeArc(66, 66, 18, 14, 180, 180, ArcType.OPEN);
            g.strokeArc(116, 66, 18, 14, 180, 180, ArcType.OPEN);
        }
        // Mouth.
        g.strokeArc(92, 84, 16, 12, 180, 180, ArcType.OPEN);
        if (role == Role.CAT) {
            // Whiskers.
            g.setLineWidth(2.5);
            g.strokeLine(44, 78, 28, 74);
            g.strokeLine(44, 86, 28, 90);
            g.strokeLine(156, 78, 172, 74);
            g.strokeLine(156, 86, 172, 90);
            g.setLineWidth(4);
        }
    }

    private static void drawAccessory(GraphicsContext g, Accessory accessory) {
        switch (accessory) {
            case LEAF -> {
                g.strokeLine(100, 26, 100, 14);
                oval(g, Color.web("#7ed957"), 98, 2, 30, 15);
                g.strokeLine(104, 10, 122, 9);
            }
            case BOW -> {
                polygon(g, Color.web("#ff8fb1"), 150, 26, 128, 12, 128, 40);
                polygon(g, Color.web("#ff8fb1"), 150, 26, 172, 12, 172, 40);
                oval(g, Color.web("#ff5f8f"), 143, 19, 14, 14);
            }
            case GLASSES -> {
                g.setFill(Color.rgb(255, 255, 255, 0.35));
                g.fillOval(58, 57, 34, 34);
                g.fillOval(108, 57, 34, 34);
                g.strokeOval(58, 57, 34, 34);
                g.strokeOval(108, 57, 34, 34);
                g.strokeLine(92, 72, 108, 72);
            }
            case FLOWER -> {
                Color petal = Color.web("#ffd1e8");
                for (int i = 0; i < 5; i++) {
                    double angle = Math.toRadians(i * 72 - 90);
                    oval(g, petal, 58 + Math.cos(angle) * 11 - 8, 30 + Math.sin(angle) * 11 - 8, 16, 16);
                }
                oval(g, Color.web("#ffd447"), 51, 23, 14, 14);
            }
            case HEADPHONES -> {
                g.setLineWidth(8);
                g.strokeArc(32, 12, 136, 130, 0, 180, ArcType.OPEN);
                g.setLineWidth(4);
                g.setFill(Color.web("#ff6b6b"));
                g.fillRoundRect(18, 58, 24, 40, 12, 12);
                g.strokeRoundRect(18, 58, 24, 40, 12, 12);
                g.fillRoundRect(158, 58, 24, 40, 12, 12);
                g.strokeRoundRect(158, 58, 24, 40, 12, 12);
            }
            case WIZARD_HAT -> {
                polygon(g, Color.web("#7a5cf0"), 62, 34, 138, 34, 112, 2);
                oval(g, Color.web("#6246d6"), 44, 26, 112, 18);
                g.setFill(Color.web("#ffd447"));
                g.fillOval(96, 16, 9, 9);
            }
            case CROWN -> {
                polygon(g, Color.web("#f5c542"), 68, 32, 68, 8, 84, 20, 100, 2, 116, 20, 132, 8, 132, 32);
                g.setFill(Color.web("#ff6b6b"));
                g.fillOval(95, 20, 10, 10);
                g.setFill(Color.web("#5aa9f0"));
                g.fillOval(75, 22, 7, 7);
                g.fillOval(118, 22, 7, 7);
            }
            case HALO -> {
                g.setStroke(Color.web("#f5c542"));
                g.setLineWidth(7);
                g.strokeOval(64, 3, 72, 16);
                g.setStroke(Color.web("#fff3b0"));
                g.setLineWidth(2);
                g.strokeOval(64, 3, 72, 16);
            }
        }
    }

    /** A round face on an 80×80 picture; the colour and the features say the feeling. */
    private static void drawEmote(GraphicsContext g, Emote emote) {
        Color face = switch (emote) {
            case IDLE -> Color.web("#fff3c4");
            case HAPPY -> Color.web("#ffe07a");
            case SURPRISED -> Color.web("#cfe8ff");
            case ANGRY -> Color.web("#ff9c8a");
            case PAIN -> Color.web("#d9c8f5");
            case EFFORT -> Color.web("#ffc98b");
            case STUNNED -> Color.web("#c9f0d6");
            case THREATEN -> Color.web("#8d7fb0");
            case TRIUMPH -> Color.web("#ffd447");
        };
        g.setLineWidth(3.5);
        oval(g, face, 6, 6, 68, 68);
        g.setFill(OUTLINE);
        switch (emote) {
            case IDLE -> {
                g.fillOval(25, 32, 8, 10);
                g.fillOval(47, 32, 8, 10);
                g.strokeLine(32, 54, 48, 54);
            }
            case HAPPY -> {
                g.strokeArc(22, 30, 14, 14, 0, 180, ArcType.OPEN);
                g.strokeArc(44, 30, 14, 14, 0, 180, ArcType.OPEN);
                g.strokeArc(26, 38, 28, 22, 180, 180, ArcType.OPEN);
            }
            case SURPRISED -> {
                g.strokeOval(22, 26, 12, 16);
                g.strokeOval(46, 26, 12, 16);
                g.strokeOval(33, 50, 14, 14);
            }
            case ANGRY -> {
                g.strokeLine(20, 26, 35, 33);
                g.strokeLine(60, 26, 45, 33);
                g.fillOval(25, 34, 8, 9);
                g.fillOval(47, 34, 8, 9);
                g.strokeArc(28, 52, 24, 16, 0, 180, ArcType.OPEN);
            }
            case PAIN -> {
                g.strokeLine(22, 30, 34, 36);
                g.strokeLine(34, 36, 22, 42);
                g.strokeLine(58, 30, 46, 36);
                g.strokeLine(46, 36, 58, 42);
                g.strokeArc(30, 52, 20, 14, 0, 180, ArcType.OPEN);
            }
            case EFFORT -> {
                g.strokeLine(21, 32, 35, 36);
                g.strokeLine(59, 32, 45, 36);
                g.strokeLine(28, 54, 52, 54);
                g.strokeLine(28, 60, 52, 60);
                g.strokeLine(36, 54, 36, 60);
                g.strokeLine(44, 54, 44, 60);
                oval(g, Color.web("#7cc7ff"), 60, 14, 10, 14);
            }
            case STUNNED -> {
                g.strokeLine(22, 30, 34, 42);
                g.strokeLine(34, 30, 22, 42);
                g.strokeLine(46, 30, 58, 42);
                g.strokeLine(58, 30, 46, 42);
                g.strokeArc(30, 50, 10, 10, 0, 180, ArcType.OPEN);
                g.strokeArc(40, 50, 10, 10, 180, 180, ArcType.OPEN);
            }
            case THREATEN -> {
                g.strokeLine(18, 24, 36, 34);
                g.strokeLine(62, 24, 44, 34);
                g.setFill(Color.web("#ffe07a"));
                g.fillOval(24, 33, 10, 8);
                g.fillOval(46, 33, 10, 8);
                g.setFill(Color.WHITE);
                g.fillRect(27, 50, 26, 10);
                g.strokeRect(27, 50, 26, 10);
                g.strokeLine(36, 50, 36, 60);
                g.strokeLine(44, 50, 44, 60);
            }
            case TRIUMPH -> {
                star(g, 28, 36, 8);
                star(g, 52, 36, 8);
                g.setFill(Color.WHITE);
                g.fillArc(24, 40, 32, 26, 180, 180, ArcType.CHORD);
                g.strokeArc(24, 40, 32, 26, 180, 180, ArcType.CHORD);
            }
        }
    }

    /** A small filled five-pointed star centred on a point. */
    private static void star(GraphicsContext g, double cx, double cy, double radius) {
        double[] xs = new double[10];
        double[] ys = new double[10];
        for (int i = 0; i < 10; i++) {
            double r = i % 2 == 0 ? radius : radius * 0.45;
            double angle = Math.toRadians(i * 36 - 90);
            xs[i] = cx + Math.cos(angle) * r;
            ys[i] = cy + Math.sin(angle) * r;
        }
        g.setFill(OUTLINE);
        g.fillPolygon(xs, ys, 10);
    }

    /** A filled, outlined shape through the given points ({@code x0, y0, x1, y1, …}). */
    private static void polygon(GraphicsContext g, Color fill, double... points) {
        int n = points.length / 2;
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = points[i * 2];
            ys[i] = points[i * 2 + 1];
        }
        g.setFill(fill);
        g.fillPolygon(xs, ys, n);
        g.strokePolygon(xs, ys, n);
    }

    private static void oval(GraphicsContext g, Color fill, double x, double y, double w, double h) {
        g.setFill(fill);
        g.fillOval(x, y, w, h);
        g.strokeOval(x, y, w, h);
    }
}
