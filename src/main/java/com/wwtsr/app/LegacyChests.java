package com.wwtsr.app;

import com.wwtsr.chest.ChestSlot;
import com.wwtsr.collect.Accessory;
import com.wwtsr.collect.Collectible;
import com.wwtsr.collect.Inventory;
import com.wwtsr.collect.Loadout;
import com.wwtsr.collect.Role;
import com.wwtsr.io.AtomicFiles;
import com.wwtsr.player.PlayerState;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Reads the chest file of earlier versions ({@code chests.properties}: the chests' times, the
 * accessories found and the one worn), so nothing a player had is lost when the player file
 * takes over.
 */
final class LegacyChests {

    private LegacyChests() {
    }

    /** The player an old chest file describes; empty when there is no such file. Never throws. */
    static Optional<PlayerState> read(Path file) {
        Properties p = AtomicFiles.readProperties(file).orElse(null);
        if (p == null) {
            return Optional.empty();
        }
        Map<ChestSlot, Long> times = new EnumMap<>(ChestSlot.class);
        for (ChestSlot slot : ChestSlot.values()) {
            times.put(slot, number(p.getProperty("readyAt." + slot.name())));
        }
        Map<Collectible, Integer> owned = new LinkedHashMap<>();
        for (Accessory accessory : Accessory.values()) {
            owned.put(accessory, (int) Math.min(Integer.MAX_VALUE, number(p.getProperty("owned." + accessory.name()))));
        }
        Optional<Accessory> worn = Optional.empty();
        try {
            String name = p.getProperty("worn");
            if (name != null) {
                worn = Optional.of(Accessory.valueOf(name.trim()));
            }
        } catch (IllegalArgumentException e) {
            // Not an accessory this version knows: wear nothing.
        }
        Inventory bag = new Inventory(owned, Set.of(), Set.of(), List.of());
        return Optional.of(new PlayerState(times, bag, new Loadout(Role.STARTER, worn, List.of()), List.of()));
    }

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
