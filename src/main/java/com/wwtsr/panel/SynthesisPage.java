package com.wwtsr.panel;

import com.wwtsr.chest.Reward;
import com.wwtsr.collect.Catalog;
import com.wwtsr.collect.Collectible;
import com.wwtsr.collect.Inventory;
import com.wwtsr.collect.ItemType;
import com.wwtsr.collect.Rarity;
import com.wwtsr.collect.Synthesis;
import com.wwtsr.i18n.Messages;
import com.wwtsr.view.RewardEffect;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Scale;

/**
 * Synthesis. Two recipes, picked by the tabs: ten spare collectibles of one kind and rarity
 * make one of the next rarity; five make one of the same rarity. The player fills a row of
 * slots: a click on an empty slot opens a chooser, a click on a filled one empties it, and
 * "one click" fills them all. When the slots are full, "synthesize" makes the new collectible
 * and plays its reveal.
 */
final class SynthesisPage implements Page {

    private static final double SLOT_WIDTH = 79;
    private static final double SLOT_HEIGHT = 75;
    private static final double RESULT = 264;
    private static final Color PAPER = Color.web("#fdf9ed");
    private static final Color EDGE = Color.web("#ddc7ac");

    private final PanelHost host;
    private final Consumer<String> toast;
    private final StackPane root = new StackPane();
    private final StackPane main = new StackPane();
    private final FlowPane slots = new FlowPane(6, 14);
    private final Label tip = new Label();
    private final Button make = new Button();
    private final StackPane chooser = new StackPane();
    /** What has been put in the slots, in order; one entry per slot filled. */
    private final List<Collectible> chosen = new ArrayList<>();
    private Synthesis.Mode mode = Synthesis.Mode.UPGRADE;
    private boolean showingResult;

    SynthesisPage(PanelHost host, Consumer<String> toast) {
        this.host = host;
        this.toast = toast;
        Messages m = host.messages();

        Tabs<Synthesis.Mode> modes = new Tabs<>(List.of(Synthesis.Mode.values()), mode,
                each -> m.get(each == Synthesis.Mode.UPGRADE ? "synthesis.upgrade" : "synthesis.same"), 60,
                picked -> {
                    mode = picked;
                    chosen.clear();
                    showingResult = false;
                    refresh();
                });
        VBox.setMargin(modes, new Insets(4, 0, 12, 0));

        slots.setAlignment(Pos.CENTER);
        slots.setMinSize(420, 164);
        slots.setMaxSize(420, 164);
        main.setMinHeight(RESULT);
        tip.getStyleClass().add("synthesis-tip");
        tip.setWrapText(true);
        tip.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(tip, new Insets(12, 0, 12, 0));

        Button oneClick = new Button(m.get("synthesis.oneClick"));
        oneClick.getStyleClass().add("common-button");
        oneClick.setOnAction(e -> fillAll());
        make.setText(m.get("synthesis.make"));
        make.getStyleClass().add("common-button");
        make.setOnAction(e -> synthesize());
        HBox buttons = new HBox(50, oneClick, make);
        buttons.setAlignment(Pos.CENTER);

        VBox page = new VBox(modes, main, tip, buttons);
        page.setPadding(new Insets(0, 27, 0, 27));
        chooser.setVisible(false);
        root.getChildren().addAll(page, chooser);
        refresh();
    }

    @Override
    public Node node() {
        return root;
    }

    @Override
    public void refresh() {
        Messages m = host.messages();
        Inventory bag = host.player().state().bag();
        // Anything chosen that is no longer spare (it was used elsewhere) falls out.
        Map<Collectible, Integer> kept = new LinkedHashMap<>();
        chosen.removeIf(item -> kept.merge(item, 1, Integer::sum) > bag.spare(item));
        tip.setText(m.get(mode == Synthesis.Mode.UPGRADE ? "synthesis.upgrade.text" : "synthesis.same.text"));
        make.setDisable(!Synthesis.valid(bag, counts(), mode));
        if (!showingResult) {
            fillSlots();
            main.getChildren().setAll(slots);
        }
    }

    // ---- the slots ----

    private void fillSlots() {
        slots.getChildren().clear();
        for (int i = 0; i < mode.cost(); i++) {
            Shape tile = BagPage.blob(SLOT_WIDTH, SLOT_HEIGHT);
            tile.setEffect(new DropShadow(0, 0, 2, EDGE));
            StackPane slot = new StackPane(tile);
            slot.setMinSize(SLOT_WIDTH, SLOT_HEIGHT);
            slot.setMaxSize(SLOT_WIDTH, SLOT_HEIGHT);
            slot.setCursor(Cursor.HAND);
            if (i < chosen.size()) {
                Collectible item = chosen.get(i);
                tile.setFill(Color.web(item.rarity().colour()).interpolate(Color.WHITE, 0.55));
                ImageView picture = new ImageView(host.assets().thumbnail(item));
                picture.setFitWidth(SLOT_HEIGHT * 0.86);
                picture.setFitHeight(SLOT_HEIGHT * 0.86);
                picture.setPreserveRatio(true);
                slot.getChildren().add(picture);
                int index = i;
                slot.setOnMouseClicked(e -> {
                    chosen.remove(index);
                    refresh();
                });
            } else {
                tile.setFill(PAPER);
                Label plus = new Label("+");
                plus.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #e3cba6;");
                slot.getChildren().add(plus);
                slot.setOnMouseClicked(e -> openChooser());
            }
            slots.getChildren().add(slot);
        }
    }

    private Map<Collectible, Integer> counts() {
        Map<Collectible, Integer> counts = new LinkedHashMap<>();
        for (Collectible item : chosen) {
            counts.merge(item, 1, Integer::sum);
        }
        return counts;
    }

    /** What may still be added: spare copies, of the kind and rarity already started on if any. */
    private List<Collectible> candidates() {
        Inventory bag = host.player().state().bag();
        Map<Collectible, Integer> counts = counts();
        List<Collectible> all = new ArrayList<>();
        for (Collectible item : Catalog.all()) {
            if (bag.spare(item) - counts.getOrDefault(item, 0) <= 0) {
                continue;
            }
            if (Synthesis.resultRarity(item.rarity(), mode).isEmpty()) {
                continue;
            }
            if (!chosen.isEmpty() && (item.type() != chosen.get(0).type() || item.rarity() != chosen.get(0).rarity())) {
                continue;
            }
            all.add(item);
        }
        return all;
    }

    /** Fills every slot: with more of what is started, or else the first kind and rarity that has enough. */
    private void fillAll() {
        Inventory bag = host.player().state().bag();
        if (chosen.isEmpty()) {
            search:
            for (ItemType type : ItemType.values()) {
                for (Rarity rarity : Rarity.values()) {
                    if (Synthesis.possible(bag, type, rarity, mode)) {
                        Synthesis.materials(bag, type, rarity, mode).forEach((item, count) -> {
                            for (int i = 0; i < count; i++) {
                                chosen.add(item);
                            }
                        });
                        break search;
                    }
                }
            }
        } else {
            while (chosen.size() < mode.cost()) {
                List<Collectible> more = candidates();
                if (more.isEmpty()) {
                    break;
                }
                chosen.add(more.get(0));
            }
        }
        if (chosen.size() < mode.cost()) {
            toast.accept(host.messages().get("synthesis.notEnough"));
        }
        showingResult = false;
        refresh();
    }

    // ---- the chooser ----

    /** A sheet over the page listing what can be added; a click on a card adds one of it. */
    private void openChooser() {
        Messages m = host.messages();
        List<Collectible> items = candidates();
        if (items.isEmpty()) {
            toast.accept(m.get(chosen.isEmpty() ? "synthesis.notEnough" : "synthesis.sameKind"));
            return;
        }
        Inventory bag = host.player().state().bag();
        Map<Collectible, Integer> counts = counts();
        FlowPane cards = new FlowPane(12, 12);
        cards.setPadding(new Insets(10));
        for (Collectible item : items) {
            cards.getChildren().add(new ItemCard(item, host.assets(), m)
                    .count(bag.spare(item) - counts.getOrDefault(item, 0))
                    .onClick(() -> {
                        chosen.add(item);
                        if (chosen.size() >= mode.cost() || candidates().isEmpty()) {
                            chooser.setVisible(false);
                        } else {
                            openChooser();
                        }
                        refresh();
                    }));
        }
        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Label close = new Label("✕");
        close.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #995e3f;");
        close.setMinSize(40, 40);
        close.setAlignment(Pos.CENTER);
        close.setCursor(Cursor.HAND);
        close.setOnMouseClicked(e -> chooser.setVisible(false));
        StackPane.setAlignment(close, Pos.TOP_RIGHT);
        StackPane.setMargin(close, new Insets(12, 12, 0, 0));

        StackPane sheet = new StackPane(scroll, close);
        sheet.setStyle("-fx-background-color: #fff3dd; -fx-background-radius: 30; -fx-border-color: #845137;"
                + " -fx-border-width: 2; -fx-border-radius: 30;");
        sheet.setPadding(new Insets(24));
        sheet.setMinSize(732, 448);
        sheet.setMaxSize(732, 448);
        chooser.setStyle("-fx-background-color: rgba(76, 70, 60, 0.6); -fx-background-radius: 30;");
        chooser.getChildren().setAll(sheet);
        chooser.setVisible(true);
    }

    // ---- making ----

    private void synthesize() {
        Messages m = host.messages();
        Map<Collectible, Integer> materials = counts();
        if (materials.keySet().stream().map(c -> c.type().name() + c.rarity().name()).distinct().count() > 1) {
            toast.accept(m.get("synthesis.sameKind"));
            return;
        }
        Optional<Reward> reward = host.player().synthesize(materials, mode, host.dice());
        if (reward.isEmpty()) {
            toast.accept(m.get("synthesis.notEnough"));
            return;
        }
        chosen.clear();
        showingResult = true;
        boolean first = reward.get().isFirst();
        // The reveal plays on a round stand, growing from the middle.
        Shape stand = BagPage.blob(RESULT, RESULT);
        stand.setFill(Color.web("#fbedd5"));
        RewardEffect effect = new RewardEffect(host.assets());
        double size = RESULT / RewardEffect.SIZE;
        effect.getTransforms().add(new Scale(size, size, 0, 0));
        Pane holder = new Pane(new Group(effect));
        holder.setMinSize(RESULT, RESULT);
        holder.setMaxSize(RESULT, RESULT);
        StackPane result = new StackPane(stand, holder);
        result.setMaxSize(RESULT, RESULT);
        result.setCursor(Cursor.HAND);
        // A click on the result goes back to the slots.
        result.setOnMouseClicked(e -> {
            showingResult = false;
            refresh();
        });
        main.getChildren().setAll(result);
        effect.play(reward.get(),
                m.get(first ? "reward." + reward.get().item().type().name() : "reward.duplicate"), first, true);
        toast.accept(m.get("synthesis.success"));
        refresh();
    }
}
