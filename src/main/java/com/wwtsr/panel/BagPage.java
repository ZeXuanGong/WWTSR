package com.wwtsr.panel;

import com.wwtsr.assets.AssetPack;
import com.wwtsr.collect.Catalog;
import com.wwtsr.collect.Collectible;
import com.wwtsr.collect.Emote;
import com.wwtsr.collect.Inventory;
import com.wwtsr.collect.Loadout;
import com.wwtsr.i18n.Messages;
import com.wwtsr.pet.BodyPose;
import com.wwtsr.pet.HeadPose;
import com.wwtsr.player.Player;
import com.wwtsr.player.PlayerState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/**
 * The bag. Across the top: the look being worn, drawn large on a rounded stand with its five
 * emote slots fanned up its left side, and beside it the saved presets. Below: everything the
 * player has, by tab and filter; a click on a card uses it.
 */
final class BagPage implements Page {

    private static final double TOP_HEIGHT = 248;
    private static final double STAND_WIDTH = 228;
    private static final double STAND_HEIGHT = 207;
    /** The stand starts this far in, leaving room for the emote slots that stick out to its left. */
    private static final double STAND_X = 64;
    /**
     * The emote slots: how far left of the stand, how far above its bottom, and their size.
     * The first is lowest.
     */
    private static final double[][] EMOTE_SLOTS = {
            {-45, -19, 60, 61}, {-59, 33, 52, 54}, {-59, 91, 51, 53}, {-47, 137, 59, 60}, {-16, 176, 62, 62}};
    private static final double PRESET = 105;
    private static final Color PAPER = Color.web("#fdf9ed");
    private static final Color EDGE = Color.web("#ddc7ac");

    private final PanelHost host;
    private final Consumer<String> toast;
    private final VBox root = new VBox(24);
    private final HBox top = new HBox();
    private final ItemList list;

    BagPage(PanelHost host, Consumer<String> toast) {
        this.host = host;
        this.toast = toast;
        Messages m = host.messages();
        top.setMinHeight(TOP_HEIGHT);
        top.setMaxHeight(TOP_HEIGHT);
        list = new ItemList(m, m.get("tab.recent"), this::items, this::card, null);
        VBox.setVgrow(list.node(), Priority.ALWAYS);
        root.getChildren().addAll(top, list.node());
        refresh();
    }

    @Override
    public Node node() {
        return root;
    }

    @Override
    public void refresh() {
        Node presets = presets();
        HBox.setHgrow(presets, Priority.ALWAYS);
        top.getChildren().setAll(preview(), presets);
        list.refresh();
    }

    // ---- the look ----

    /** The pet as it looks now on its stand, with the emote slots up its left side. */
    private Node preview() {
        Player player = host.player();
        Loadout look = player.state().look();
        Pane pane = new Pane();
        double width = STAND_X + STAND_WIDTH + 28;
        pane.setMinSize(width, TOP_HEIGHT);
        pane.setMaxSize(width, TOP_HEIGHT);
        double standTop = TOP_HEIGHT - 12 - STAND_HEIGHT;

        Shape stand = blob(STAND_WIDTH, STAND_HEIGHT);
        stand.setFill(PAPER);
        stand.setStroke(Color.web("#f0dcbb"));
        stand.setStrokeWidth(3);
        stand.setLayoutX(STAND_X);
        stand.setLayoutY(standTop);

        // The whole pet, a fifth larger, standing on the stand; its top may poke out above.
        Node pet = lookPicture(look, 1.2);
        pet.setLayoutX(STAND_X + STAND_WIDTH / 2 - 100);
        pet.setLayoutY(standTop + STAND_HEIGHT / 2 - 100 * 0.98);
        Rectangle above = new Rectangle(-20, -80, STAND_WIDTH + 40, 80 + STAND_HEIGHT / 2);
        Shape window = Shape.union(blob(STAND_WIDTH, STAND_HEIGHT), above);
        window.setLayoutX(STAND_X);
        window.setLayoutY(standTop);
        Group cut = new Group(pet);
        cut.setClip(window);
        pane.getChildren().addAll(stand, cut);

        for (int i = 0; i < EMOTE_SLOTS.length; i++) {
            double[] at = EMOTE_SLOTS[i];
            Node slot = emoteSlot(i < look.emotes().size() ? Optional.of(look.emotes().get(i)) : Optional.empty(),
                    at[2], at[3]);
            slot.setLayoutX(STAND_X + at[0]);
            slot.setLayoutY(standTop + STAND_HEIGHT - at[1] - at[3]);
            pane.getChildren().add(slot);
        }
        return pane;
    }

    /** One emote slot: a pale petal, empty with a plus or holding an emote that a click takes out. */
    private Node emoteSlot(Optional<Emote> emote, double width, double height) {
        Shape petal = blob(width, height);
        petal.setFill(PAPER);
        petal.setEffect(new DropShadow(0, 0, 2, EDGE));
        StackPane slot = new StackPane(petal);
        slot.setMinSize(width, height);
        slot.setMaxSize(width, height);
        if (emote.isPresent()) {
            ImageView picture = new ImageView(host.assets().emote(emote.get()));
            picture.setFitWidth(40);
            picture.setFitHeight(40);
            // The mark that takes the emote out shows while the mouse is on the slot.
            Label remove = new Label("✕");
            remove.setStyle("-fx-background-color: #c88865; -fx-background-radius: 8; -fx-text-fill: white;"
                    + " -fx-font-size: 10px; -fx-font-weight: bold; -fx-alignment: center;");
            remove.setMinSize(16, 16);
            remove.setMaxSize(16, 16);
            remove.visibleProperty().bind(slot.hoverProperty());
            StackPane.setAlignment(remove, Pos.TOP_RIGHT);
            slot.getChildren().addAll(picture, remove);
            slot.setCursor(Cursor.HAND);
            slot.setOnMouseClicked(e -> host.player().use(emote.get()));
        } else {
            Label plus = new Label("+");
            plus.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #e3cba6;");
            slot.getChildren().add(plus);
        }
        return slot;
    }

    /** A pet wearing a look: body, head and accessory, 200 x 200 before scaling. */
    private Node lookPicture(Loadout look, double scale) {
        AssetPack assets = host.assets();
        StackPane picture = new StackPane(new ImageView(assets.body(look.role(), BodyPose.IDLE)),
                new ImageView(assets.head(look.role(), HeadPose.EYES_OPEN)));
        look.accessory().ifPresent(accessory -> picture.getChildren().add(new ImageView(assets.accessory(accessory))));
        picture.setMinSize(200, 200);
        picture.setMaxSize(200, 200);
        picture.setScaleX(scale);
        picture.setScaleY(scale);
        picture.setMouseTransparent(true);
        return picture;
    }

    // ---- presets ----

    /** The saved looks first, then empty slots that save the current look when clicked. */
    private Node presets() {
        Messages m = host.messages();
        Player player = host.player();
        List<Optional<Loadout>> saved = player.state().presets();
        FlowPane tiles = new FlowPane(10, 10);
        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < PlayerState.PRESETS; i++) {
            if (saved.get(i).isPresent()) {
                tiles.getChildren().add(presetTile(i, saved.get(i).get()));
            } else {
                empty.add(i);
            }
        }
        for (int slot : empty) {
            Shape tile = blob(PRESET, PRESET);
            tile.setFill(PAPER);
            tile.setEffect(new DropShadow(0, 0, 2, EDGE));
            Label plus = new Label("+");
            plus.getStyleClass().add("preset-plus");
            StackPane add = new StackPane(tile, plus);
            add.setMinSize(PRESET, PRESET);
            add.setMaxSize(PRESET, PRESET);
            add.setCursor(Cursor.HAND);
            add.setOnMouseClicked(e -> player.savePreset(slot));
            tiles.getChildren().add(add);
        }
        Label title = new Label(m.get("bag.presets"));
        title.getStyleClass().add("preset-title");
        VBox box = new VBox(4, title, tiles);
        box.getStyleClass().add("preset-divider");
        return box;
    }

    private Node presetTile(int slot, Loadout preset) {
        Player player = host.player();
        boolean active = preset.equals(player.state().look());
        Shape tile = blob(PRESET, PRESET);
        tile.setFill(PAPER);
        if (active) {
            tile.setStroke(Color.web("#fed291"));
            tile.setStrokeWidth(4);
        }
        tile.setEffect(new DropShadow(0, 0, 2, EDGE));
        Node pet = lookPicture(preset, 0.44);
        Group cut = new Group(pet);
        pet.setLayoutX(PRESET / 2 - 100);
        pet.setLayoutY(PRESET / 2 - 100);
        cut.setClip(blob(PRESET, PRESET));

        Group remove = new Group(new Circle(11.5, 12, 10, Color.web("#c88865")));
        Label cross = new Label("✕");
        cross.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
        StackPane removeButton = new StackPane(remove, cross);
        removeButton.setMinSize(23, 24);
        removeButton.setMaxSize(23, 24);
        removeButton.setCursor(Cursor.HAND);
        removeButton.setOnMouseClicked(e -> {
            player.clearPreset(slot);
            e.consume();
        });
        StackPane.setAlignment(removeButton, Pos.TOP_RIGHT);

        StackPane holder = new StackPane(tile, new Pane(cut), removeButton);
        holder.setMinSize(PRESET, PRESET);
        holder.setMaxSize(PRESET, PRESET);
        holder.setCursor(Cursor.HAND);
        // The mark that deletes the preset shows while the mouse is on it.
        removeButton.visibleProperty().bind(holder.hoverProperty());
        holder.setOnMouseClicked(e -> player.applyPreset(slot));
        return holder;
    }

    // ---- the list ----

    /** The latest finds on the first tab; on the others that kind, starred first and then rarest first. */
    private List<Collectible> items(Optional<com.wwtsr.collect.ItemType> tab) {
        Inventory bag = host.player().state().bag();
        if (tab.isEmpty()) {
            return bag.recent();
        }
        return Catalog.of(tab.get()).stream().filter(bag::owns)
                .sorted(Comparator.comparing((Collectible item) -> !bag.favourites().contains(item))
                        .thenComparing(item -> -item.rarity().ordinal()))
                .toList();
    }

    private ItemCard card(Collectible item) {
        Messages m = host.messages();
        Player player = host.player();
        Inventory bag = player.state().bag();
        ItemCard card = new ItemCard(item, host.assets(), m)
                .count(bag.count(item))
                .inUse(player.inUse(item))
                .star(bag.favourites().contains(item), () -> player.toggleFavourite(item))
                .onClick(() -> {
                    if (player.use(item) == Player.UseResult.EMOTES_FULL) {
                        toast.accept(m.get("bag.emotes.full"));
                    }
                });
        if (bag.fresh().contains(item)) {
            card.fresh(m.get("item.new"));
        }
        return card;
    }

    /** A rounded shape that bulges a little, filling a box of that size. */
    static Shape blob(double width, double height) {
        Polygon shape = new Polygon();
        double power = 2.0 / 3.4;
        for (int i = 0; i < 64; i++) {
            double angle = 2 * Math.PI * i / 64;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            shape.getPoints().addAll(
                    width / 2 + width / 2 * Math.signum(cos) * Math.pow(Math.abs(cos), power),
                    height / 2 + height / 2 * Math.signum(sin) * Math.pow(Math.abs(sin), power));
        }
        return shape;
    }
}
