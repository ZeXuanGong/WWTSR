package com.wwtsr.player;

import com.wwtsr.chest.ChestSlot;
import com.wwtsr.collect.Inventory;
import com.wwtsr.collect.Loadout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Everything about the player that is kept between runs. Immutable, and always consistent:
 * the look and the presets only ever name things that are in the bag.
 *
 * @param readyAt when each chest is next ready, in milliseconds since 1970; a missing entry
 *                means "as soon as it's unlocked"
 * @param bag     what the player has
 * @param look    what the pet looks like now
 * @param presets the saved looks; always exactly {@value #PRESETS} slots, some empty
 * @param spentTaps how many taps have been used up opening chests
 */
public record PlayerState(
        Map<ChestSlot, Long> readyAt,
        Inventory bag,
        Loadout look,
        List<Optional<Loadout>> presets,
        long spentTaps) {

    public static final int PRESETS = 5;

    public static final PlayerState NEW = new PlayerState(Map.of(), Inventory.STARTER, Loadout.STARTER, List.of());

    /** A state in which no taps have been spent yet. */
    public PlayerState(Map<ChestSlot, Long> readyAt, Inventory bag, Loadout look, List<Optional<Loadout>> presets) {
        this(readyAt, bag, look, presets, 0);
    }

    public PlayerState {
        Objects.requireNonNull(bag);
        spentTaps = Math.max(0, spentTaps);
        EnumMap<ChestSlot, Long> times = new EnumMap<>(ChestSlot.class);
        readyAt.forEach((slot, at) -> {
            if (slot != null && at != null && at > 0) {
                times.put(slot, at);
            }
        });
        readyAt = Collections.unmodifiableMap(times);
        look = look.fitting(bag);
        List<Optional<Loadout>> slots = new ArrayList<>();
        for (int i = 0; i < PRESETS; i++) {
            Optional<Loadout> saved = i < presets.size() && presets.get(i) != null ? presets.get(i) : Optional.empty();
            Inventory held = bag;
            slots.add(saved.map(preset -> preset.fitting(held)));
        }
        presets = Collections.unmodifiableList(slots);
    }

    public long readyAt(ChestSlot slot) {
        return readyAt.getOrDefault(slot, 0L);
    }

    PlayerState withBag(Inventory value) {
        return new PlayerState(readyAt, value, look, presets, spentTaps);
    }

    PlayerState withLook(Loadout value) {
        return new PlayerState(readyAt, bag, value, presets, spentTaps);
    }

    PlayerState withPreset(int slot, Optional<Loadout> value) {
        List<Optional<Loadout>> slots = new ArrayList<>(presets);
        slots.set(slot, value);
        return new PlayerState(readyAt, bag, look, slots, spentTaps);
    }

    PlayerState withReadyAt(ChestSlot slot, long at) {
        Map<ChestSlot, Long> times = new EnumMap<>(ChestSlot.class);
        times.putAll(readyAt);
        times.put(slot, at);
        return new PlayerState(times, bag, look, presets, spentTaps);
    }

    PlayerState withSpentTaps(long value) {
        return new PlayerState(readyAt, bag, look, presets, value);
    }
}
