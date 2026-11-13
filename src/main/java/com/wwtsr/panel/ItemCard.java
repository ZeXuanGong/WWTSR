package com.wwtsr.panel;

import com.wwtsr.assets.AssetPack;
import com.wwtsr.collect.Collectible;
import com.wwtsr.collect.ItemType;
import com.wwtsr.i18n.Messages;
import com.wwtsr.view.TypeMark;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

/**
 * One collectible as a card, 120 x 139: its picture on a rounded tile in the rarity's colour,
 * its name underneath, and marks in the corners. Pages say which marks to show: how many
 * (bottom right of the tile), the kind (bottom left), NEW (top left, sticking out), the star
 * (top right).
 */
final class ItemCard extends Pane {

    static final double WIDTH = 120;
    static final double HEIGHT = 139;

    private static final double THUMB_X = 5;
    private static final double THUMB_Y = 4;
    private static final double THUMB_WIDTH = 110;
    private static final double THUMB_HEIGHT = 97;
    /** The marks along the tile's bottom edge sit this far down the card. */
    private static final double MARK_Y = 80;
    /** Counts above this show as "99+". */
    private static final int COUNT_MAX = 99;
    private static final Color CARD = Color.web("#fef6dd");
    private static final Color CARD_EDGE = Color.web("#ddc7ac");
    private static final Color GLOW = Color.web("#ffe179");
    private static final Color MARK = Color.web("#b98b5e");
    private static final Color MARK_GROUND = Color.web("#fdf9ed");

    private final Rectangle card = new Rectangle(WIDTH, HEIGHT);

    ItemCard(Collectible item, AssetPack assets, Messages messages) {
        setMinSize(WIDTH, HEIGHT);
        setPrefSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);

        card.setArcWidth(40);
        card.setArcHeight(40);
        card.setFill(CARD);
        card.setEffect(new DropShadow(0, 0, 2, CARD_EDGE));

        // The tile and its picture, cut to the tile's rounded shape.
        Polygon tile = tileShape();
        tile.setFill(Color.web(item.rarity().colour()).interpolate(Color.WHITE, 0.55));
        double grow = switch (item.type()) {
            case ROLE -> 1.14;
            case ACCESSORY -> 1.4;
            case EMOTE -> 1.1;
        };
        // A role's and an accessory's thumbnails have room around them; an emote's has none.
        double size = (item.type() == ItemType.EMOTE ? THUMB_HEIGHT * 0.78 : THUMB_HEIGHT) * (grow / 1.14);
        ImageView picture = new ImageView(assets.thumbnail(item));
        picture.setFitWidth(size);
        picture.setFitHeight(size);
        picture.setPreserveRatio(true);
        picture.setX(THUMB_X + (THUMB_WIDTH - size) / 2);
        picture.setY(THUMB_Y + (THUMB_HEIGHT - size) / 2);
        Group cut = new Group(picture);
        cut.setClip(tileShape());

        Label name = new Label(messages.get(item.id()));
        name.getStyleClass().add("item-name");
        name.setMinWidth(95);
        name.setMaxWidth(95);
        name.setLayoutX((WIDTH - 95) / 2);
        name.setLayoutY(THUMB_Y + THUMB_HEIGHT + 4);

        // The kind of collectible, on a small tab at the tile's bottom left.
        Region typeTab = new Region();
        typeTab.getStyleClass().add("item-type");
        fix(typeTab, 2, MARK_Y, 26, 24);
        getChildren().addAll(card, tile, cut, name, typeTab,
                TypeMark.of(item.type(), 2 + 13, MARK_Y + 12, MARK, MARK_GROUND));
    }

    /** Shows how many there are; nothing for a single one. */
    ItemCard count(int count) {
        if (count > 1) {
            Label badge = new Label(count > COUNT_MAX ? COUNT_MAX + "+" : Integer.toString(count));
            badge.getStyleClass().add("item-qty");
            StackPane holder = new StackPane(badge);
            holder.setAlignment(Pos.TOP_RIGHT);
            fix(holder, 0, MARK_Y - 4, WIDTH - 2, 34);
            holder.setMouseTransparent(true);
            getChildren().add(holder);
        }
        return this;
    }

    /** Marks the card NEW: a round badge sticking out of the top-left corner. */
    ItemCard fresh(String text) {
        Label badge = new Label(text);
        badge.getStyleClass().add("item-new");
        fix(badge, -10, -10, 36, 36);
        badge.setMouseTransparent(true);
        getChildren().add(badge);
        return this;
    }

    /** Adds the star, top right: filled when starred. A click on it runs {@code onToggle}. */
    ItemCard star(boolean starred, Runnable onToggle) {
        double cx = WIDTH - 12 - 9.5;
        double cy = 12 + 9.5;
        Polygon star = new Polygon();
        for (int i = 0; i < 10; i++) {
            double r = i % 2 == 0 ? 9.5 : 4.2;
            double angle = Math.toRadians(i * 36 - 90);
            star.getPoints().addAll(cx + Math.cos(angle) * r, cy + Math.sin(angle) * r);
        }
        star.setFill(starred ? Color.web("#ffb53d") : Color.rgb(255, 255, 255, 0.55));
        star.setStroke(starred ? Color.web("#e0902a") : Color.web("#d8c4a4"));
        star.setStrokeWidth(1.2);
        star.setCursor(Cursor.HAND);
        star.setOnMouseClicked(e -> {
            onToggle.run();
            e.consume();
        });
        getChildren().add(star);
        return this;
    }

    /** Draws the card as the one in use: a golden glow all round it. */
    ItemCard inUse(boolean used) {
        if (used) {
            card.setStroke(GLOW);
            card.setStrokeWidth(4);
            card.setEffect(new DropShadow(6, Color.web("#ffd166")));
        }
        return this;
    }

    /** Draws the card veiled, for something the player doesn't have. */
    ItemCard dim(boolean dimmed) {
        if (dimmed) {
            Rectangle veil = new Rectangle(WIDTH, HEIGHT);
            veil.setArcWidth(40);
            veil.setArcHeight(40);
            veil.setFill(Color.web("#fff3dd", 0.7));
            veil.setMouseTransparent(true);
            getChildren().add(veil);
        }
        return this;
    }

    ItemCard onClick(Runnable action) {
        setCursor(Cursor.HAND);
        setOnMouseClicked(e -> action.run());
        return this;
    }

    /** Puts a part at a place on the card with a fixed size. */
    private static void fix(Region part, double x, double y, double width, double height) {
        part.setMinSize(width, height);
        part.setPrefSize(width, height);
        part.setMaxSize(width, height);
        part.setLayoutX(x);
        part.setLayoutY(y);
    }

    /** The tile: a rounded rectangle that bulges a little. */
    private static Polygon tileShape() {
        Polygon shape = new Polygon();
        double cx = THUMB_X + THUMB_WIDTH / 2;
        double cy = THUMB_Y + THUMB_HEIGHT / 2;
        double power = 2.0 / 4.2;
        for (int i = 0; i < 64; i++) {
            double angle = 2 * Math.PI * i / 64;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            shape.getPoints().addAll(
                    cx + THUMB_WIDTH / 2 * Math.signum(cos) * Math.pow(Math.abs(cos), power),
                    cy + THUMB_HEIGHT / 2 * Math.signum(sin) * Math.pow(Math.abs(sin), power));
        }
        return shape;
    }
}
