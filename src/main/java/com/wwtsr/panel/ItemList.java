package com.wwtsr.panel;

import com.wwtsr.collect.Collectible;
import com.wwtsr.collect.ItemType;
import com.wwtsr.collect.Rarity;
import com.wwtsr.collect.Series;
import com.wwtsr.i18n.Messages;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * The part the bag and the atlas share: a bar of tabs (one for everything or the latest, then
 * one per kind), drop-downs that narrow what is shown, and the cards themselves, either in one
 * run or in groups by rarity that can be folded away.
 *
 * <p>The drop-downs appear on the kind tabs only; the one for the series appears on the roles
 * tab only.
 */
final class ItemList {

    /** Rarest first. */
    private static final List<Rarity> GROUP_ORDER = List.of(Rarity.EPIC, Rarity.RARE, Rarity.UNCOMMON, Rarity.COMMON);

    private final Messages messages;
    private final Function<Optional<ItemType>, List<Collectible>> source;
    private final Function<Collectible, ItemCard> card;
    private final Function<List<Collectible>, String> groupCount;
    private final VBox root = new VBox();
    private final HBox filters = new HBox(20);
    private final VBox list = new VBox();
    private final Set<Rarity> folded = EnumSet.noneOf(Rarity.class);
    private Optional<ItemType> tab = Optional.empty();
    private Optional<Series> series = Optional.empty();
    private Optional<Rarity> rarity = Optional.empty();

    /**
     * @param firstTab   the name of the first tab (everything, or the latest)
     * @param source     the collectibles for a tab: empty for the first tab, else that kind
     * @param card       makes the card for a collectible
     * @param groupCount when given, the first tab is shown in foldable groups by rarity, and
     *                   this gives the text after each group's name; when null, every tab is
     *                   one run of cards
     */
    ItemList(Messages messages, String firstTab, Function<Optional<ItemType>, List<Collectible>> source,
            Function<Collectible, ItemCard> card, Function<List<Collectible>, String> groupCount) {
        this.messages = messages;
        this.source = source;
        this.card = card;
        this.groupCount = groupCount;

        List<Optional<ItemType>> choices = new ArrayList<>();
        choices.add(Optional.empty());
        for (ItemType type : ItemType.values()) {
            choices.add(Optional.of(type));
        }
        Tabs<Optional<ItemType>> tabs = new Tabs<>(choices, tab,
                t -> t.map(type -> messages.get("type." + type.name())).orElse(firstTab), 30, picked -> {
                    tab = picked;
                    series = Optional.empty();
                    rarity = Optional.empty();
                    showFilters();
                    refresh();
                });
        VBox.setMargin(tabs, new Insets(0, 0, 20, 0));
        VBox.setMargin(filters, new Insets(0, 0, 10, 0));

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.getChildren().addAll(tabs, filters, scroll);
        showFilters();
    }

    Node node() {
        return root;
    }

    void refresh() {
        List<Collectible> items = new ArrayList<>(source.apply(tab));
        items.removeIf(item -> series.isPresent() && item.series() != series.get());
        items.removeIf(item -> rarity.isPresent() && item.rarity() != rarity.get());
        list.getChildren().clear();
        if (groupCount != null && tab.isEmpty()) {
            for (Rarity each : GROUP_ORDER) {
                List<Collectible> group = items.stream().filter(item -> item.rarity() == each).toList();
                if (group.isEmpty()) {
                    continue;
                }
                boolean hidden = folded.contains(each);
                Label title = new Label(messages.get("rarity." + each.name()) + groupCount.apply(group)
                        + (hidden ? "  ▸" : "  ▾"));
                title.getStyleClass().add("group-title");
                title.setPadding(new Insets(6, 0, 6, 16));
                title.setOnMouseClicked(e -> {
                    if (!folded.remove(each)) {
                        folded.add(each);
                    }
                    refresh();
                });
                list.getChildren().add(title);
                if (!hidden) {
                    list.getChildren().add(cards(group));
                }
            }
        } else {
            list.getChildren().add(cards(items));
        }
    }

    private FlowPane cards(List<Collectible> items) {
        FlowPane flow = new FlowPane(20, 20);
        flow.setPadding(new Insets(12, 24, 12, 24));
        for (Collectible item : items) {
            flow.getChildren().add(card.apply(item));
        }
        return flow;
    }

    private void showFilters() {
        filters.getChildren().clear();
        boolean any = tab.isPresent();
        filters.setVisible(any);
        filters.setManaged(any);
        if (!any) {
            return;
        }
        if (tab.get() == ItemType.ROLE) {
            filters.getChildren().add(filter(messages.get("filter.series"), List.of(Series.values()),
                    s -> messages.get("series." + s.name()), picked -> {
                        series = picked;
                        refresh();
                    }));
        }
        filters.getChildren().add(filter(messages.get("filter.rarity"), List.of(Rarity.values()),
                r -> messages.get("rarity." + r.name()), picked -> {
                    rarity = picked;
                    refresh();
                }));
    }

    /** A drop-down whose first entry, named after the filter, means "any". */
    private <T> SelectBox<Optional<T>> filter(String name, List<T> values, Function<T, String> label,
            java.util.function.Consumer<Optional<T>> onPicked) {
        List<Optional<T>> options = new ArrayList<>();
        options.add(Optional.empty());
        for (T value : values) {
            options.add(Optional.of(value));
        }
        return new SelectBox<>(options, Optional.empty(),
                option -> option.map(label).orElse(name),
                option -> option.map(label).orElse(messages.get("tab.all")), onPicked);
    }
}
