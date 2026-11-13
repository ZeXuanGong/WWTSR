package com.wwtsr.collect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The bag: how many of each collectible the player has, which they starred, which are new
 * since they last looked, and what they got most recently. Immutable; every change gives a new
 * bag. The starter role is always there.
 *
 * @param owned      how many of each (only those owned at least once)
 * @param favourites the starred ones (always owned)
 * @param fresh      the ones marked NEW (always owned)
 * @param recent     the latest finds, newest first, at most {@value #RECENT_MAX} (always owned)
 */
public record Inventory(
        Map<Collectible, Integer> owned,
        Set<Collectible> favourites,
        Set<Collectible> fresh,
        List<Collectible> recent) {

    public static final int RECENT_MAX = 30;

    public static final Inventory STARTER = new Inventory(Map.of(), Set.of(), Set.of(), List.of());

    public Inventory {
        // Kept in catalog order so everything that walks the bag sees the same order.
        Map<Collectible, Integer> counts = new LinkedHashMap<>();
        for (Collectible item : Catalog.all()) {
            Integer count = owned.get(item);
            if (count != null && count > 0) {
                counts.put(item, count);
            }
        }
        counts.putIfAbsent(Role.STARTER, 1);
        Set<Collectible> stars = new LinkedHashSet<>();
        Set<Collectible> news = new LinkedHashSet<>();
        for (Collectible item : counts.keySet()) {
            if (favourites.contains(item)) {
                stars.add(item);
            }
            if (fresh.contains(item)) {
                news.add(item);
            }
        }
        List<Collectible> latest = new ArrayList<>();
        for (Collectible item : recent) {
            if (item != null && counts.containsKey(item) && !latest.contains(item) && latest.size() < RECENT_MAX) {
                latest.add(item);
            }
        }
        owned = Collections.unmodifiableMap(counts);
        favourites = Collections.unmodifiableSet(stars);
        fresh = Collections.unmodifiableSet(news);
        recent = Collections.unmodifiableList(latest);
    }

    public int count(Collectible item) {
        return owned.getOrDefault(item, 0);
    }

    public boolean owns(Collectible item) {
        return owned.containsKey(item);
    }

    /** Copies beyond the first: what can be given up without losing the collectible. */
    public int spare(Collectible item) {
        return Math.max(0, count(item) - 1);
    }

    /** One more of {@code item}: it becomes NEW and the most recent find. */
    public Inventory plus(Collectible item) {
        Map<Collectible, Integer> counts = new LinkedHashMap<>(owned);
        counts.merge(item, 1, (a, b) -> a == Integer.MAX_VALUE ? a : a + b);
        Set<Collectible> news = new LinkedHashSet<>(fresh);
        news.add(item);
        List<Collectible> latest = new ArrayList<>();
        latest.add(item);
        latest.addAll(recent);
        return new Inventory(counts, favourites, news, latest);
    }

    /** {@code howMany} fewer of {@code item}, never going below one copy. */
    public Inventory minusSpare(Collectible item, int howMany) {
        int take = Math.min(Math.max(0, howMany), spare(item));
        if (take == 0) {
            return this;
        }
        Map<Collectible, Integer> counts = new LinkedHashMap<>(owned);
        counts.put(item, count(item) - take);
        return new Inventory(counts, favourites, fresh, recent);
    }

    public Inventory withFavourite(Collectible item, boolean starred) {
        Set<Collectible> stars = new LinkedHashSet<>(favourites);
        if (starred) {
            stars.add(item);
        } else {
            stars.remove(item);
        }
        return new Inventory(owned, stars, fresh, recent);
    }

    /** The player has looked at {@code item}: it is no longer NEW. */
    public Inventory seen(Collectible item) {
        if (!fresh.contains(item)) {
            return this;
        }
        Set<Collectible> news = new LinkedHashSet<>(fresh);
        news.remove(item);
        return new Inventory(owned, favourites, news, recent);
    }
}
