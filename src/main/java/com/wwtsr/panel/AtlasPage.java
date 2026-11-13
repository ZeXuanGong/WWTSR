package com.wwtsr.panel;

import com.wwtsr.collect.Catalog;
import com.wwtsr.collect.Collectible;
import com.wwtsr.collect.Inventory;
import com.wwtsr.i18n.Messages;
import javafx.scene.Node;

/**
 * The atlas: every collectible there is. The first tab shows them all in groups by rarity,
 * each headed by its name and how many of the group have been found, and foldable; the other
 * tabs show one kind. What hasn't been found is veiled.
 */
final class AtlasPage implements Page {

    private final PanelHost host;
    private final ItemList list;

    AtlasPage(PanelHost host) {
        this.host = host;
        Messages m = host.messages();
        list = new ItemList(m, m.get("tab.all"),
                tab -> tab.map(Catalog::of).orElse(Catalog.all()),
                this::card,
                group -> "（" + group.stream().filter(item -> host.player().state().bag().owns(item)).count()
                        + "/" + group.size() + "）");
        refresh();
    }

    @Override
    public Node node() {
        return list.node();
    }

    @Override
    public void refresh() {
        list.refresh();
    }

    private ItemCard card(Collectible item) {
        Inventory bag = host.player().state().bag();
        ItemCard card = new ItemCard(item, host.assets(), host.messages());
        if (!bag.owns(item)) {
            return card.dim(true);
        }
        card.count(bag.count(item)).star(bag.favourites().contains(item), () -> host.player().toggleFavourite(item));
        if (bag.fresh().contains(item)) {
            card.fresh(host.messages().get("item.new"));
        }
        return card;
    }
}
