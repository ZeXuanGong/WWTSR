package com.wwtsr.player;

import com.wwtsr.chest.ChestSlot;
import com.wwtsr.collect.Accessory;
import com.wwtsr.collect.Catalog;
import com.wwtsr.collect.Collectible;
import com.wwtsr.collect.Emote;
import com.wwtsr.collect.Inventory;
import com.wwtsr.collect.Loadout;
import com.wwtsr.collect.Role;
import com.wwtsr.io.AtomicFiles;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Keeps the player's state in a small text file with a {@code version} line. Collectibles are
 * written by their ids. Each value is read on its own, so one damaged line (or a collectible
 * this version doesn't know) loses only itself; a newer format reads as a new player.
 */
public final class FilePlayerStore implements PlayerStore {

    static final int VERSION = 1;

    private final Path file;

    public FilePlayerStore(Path file) {
        this.file = Objects.requireNonNull(file);
    }

    @Override
    public PlayerState load() {
        Properties p = AtomicFiles.readProperties(file).orElse(null);
        if (p == null || number(p.getProperty("version")) != VERSION) {
            return PlayerState.NEW;
        }
        Map<ChestSlot, Long> times = new EnumMap<>(ChestSlot.class);
        for (ChestSlot slot : ChestSlot.values()) {
            times.put(slot, number(p.getProperty("chest." + slot.name())));
        }
        Map<Collectible, Integer> owned = new LinkedHashMap<>();
        for (Collectible item : Catalog.all()) {
            owned.put(item, (int) Math.min(Integer.MAX_VALUE, number(p.getProperty("owned." + item.id()))));
        }
        Inventory bag = new Inventory(owned,
                new LinkedHashSet<>(items(p.getProperty("favourites"))),
                new LinkedHashSet<>(items(p.getProperty("new"))),
                items(p.getProperty("recent")));
        List<Optional<Loadout>> presets = new ArrayList<>();
        for (int i = 0; i < PlayerState.PRESETS; i++) {
            presets.add(look(p, "preset." + i));
        }
        return new PlayerState(times, bag, look(p, "look").orElse(Loadout.STARTER), presets,
                number(p.getProperty("spentTaps")));
    }

    @Override
    public void save(PlayerState state) throws IOException {
        Properties p = new Properties();
        p.setProperty("version", Integer.toString(VERSION));
        p.setProperty("spentTaps", Long.toString(state.spentTaps()));
        state.readyAt().forEach((slot, at) -> p.setProperty("chest." + slot.name(), Long.toString(at)));
        Inventory bag = state.bag();
        bag.owned().forEach((item, count) -> p.setProperty("owned." + item.id(), Integer.toString(count)));
        p.setProperty("favourites", ids(bag.favourites()));
        p.setProperty("new", ids(bag.fresh()));
        p.setProperty("recent", ids(bag.recent()));
        write(p, "look", state.look());
        for (int i = 0; i < PlayerState.PRESETS; i++) {
            int slot = i;
            state.presets().get(i).ifPresent(preset -> write(p, "preset." + slot, preset));
        }
        AtomicFiles.writeProperties(file, p, "WWTSR player");
    }

    private static void write(Properties p, String key, Loadout look) {
        p.setProperty(key + ".role", look.role().id());
        look.accessory().ifPresent(accessory -> p.setProperty(key + ".accessory", accessory.id()));
        p.setProperty(key + ".emotes", ids(look.emotes()));
    }

    /** A look saved under {@code key}; empty when no role is saved there. */
    private static Optional<Loadout> look(Properties p, String key) {
        Optional<Role> role = Catalog.byId(p.getProperty(key + ".role"))
                .filter(Role.class::isInstance).map(Role.class::cast);
        if (role.isEmpty()) {
            return Optional.empty();
        }
        Optional<Accessory> accessory = Catalog.byId(p.getProperty(key + ".accessory"))
                .filter(Accessory.class::isInstance).map(Accessory.class::cast);
        List<Emote> emotes = items(p.getProperty(key + ".emotes")).stream()
                .filter(Emote.class::isInstance).map(Emote.class::cast).toList();
        return Optional.of(new Loadout(role.get(), accessory, emotes));
    }

    private static String ids(java.util.Collection<? extends Collectible> items) {
        return items.stream().map(Collectible::id).collect(Collectors.joining(","));
    }

    /** The collectibles named in a comma-separated list; names this version doesn't know are skipped. */
    private static List<Collectible> items(String list) {
        List<Collectible> found = new ArrayList<>();
        if (list != null) {
            Set<Collectible> seen = new LinkedHashSet<>();
            for (String id : list.split(",")) {
                Catalog.byId(id).ifPresent(seen::add);
            }
            found.addAll(seen);
        }
        return found;
    }

    /** A saved number, or 0 when it's missing, not a number or negative. */
    private static long number(String text) {
        if (text == null) {
            return 0;
        }
        try {
            return Math.max(0, Long.parseLong(text.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
