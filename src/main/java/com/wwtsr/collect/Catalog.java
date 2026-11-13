package com.wwtsr.collect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Every collectible there is, in a fixed order (roles, then accessories, then emotes). */
public final class Catalog {

    private static final List<Collectible> ALL;
    private static final Map<String, Collectible> BY_ID = new LinkedHashMap<>();

    static {
        List<Collectible> all = new ArrayList<>();
        all.addAll(List.of(Role.values()));
        all.addAll(List.of(Accessory.values()));
        all.addAll(List.of(Emote.values()));
        ALL = List.copyOf(all);
        for (Collectible item : ALL) {
            BY_ID.put(item.id(), item);
        }
    }

    private Catalog() {
    }

    public static List<Collectible> all() {
        return ALL;
    }

    public static List<Collectible> of(ItemType type) {
        return ALL.stream().filter(c -> c.type() == type).toList();
    }

    public static List<Collectible> of(ItemType type, Rarity rarity) {
        return ALL.stream().filter(c -> c.type() == type && c.rarity() == rarity).toList();
    }

    public static List<Collectible> of(Rarity rarity) {
        return ALL.stream().filter(c -> c.rarity() == rarity).toList();
    }

    /** The collectible with that {@link Collectible#id()}, if this version knows it. */
    public static Optional<Collectible> byId(String id) {
        return Optional.ofNullable(id == null ? null : BY_ID.get(id.trim()));
    }
}
