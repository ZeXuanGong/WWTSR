package com.wwtsr.player;

import com.wwtsr.chest.ChestRules;
import com.wwtsr.chest.ChestSlot;
import com.wwtsr.chest.Reward;
import com.wwtsr.collect.Accessory;
import com.wwtsr.collect.Collectible;
import com.wwtsr.collect.Emote;
import com.wwtsr.collect.Inventory;
import com.wwtsr.collect.ItemType;
import com.wwtsr.collect.Loadout;
import com.wwtsr.collect.Rarity;
import com.wwtsr.collect.Role;
import com.wwtsr.collect.Synthesis;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.random.RandomGenerator;

/**
 * The player: their chests, bag, look and presets, and everything they can do with them.
 * Callers hand in the tap count, the time and the dice, so it can be tested without waiting.
 * Listeners hear about every change. Use from one thread.
 */
public final class Player {

    /** What happened when the player tried to use a collectible. */
    public enum UseResult {
        /** The look changed. */
        CHANGED,
        /** The player doesn't have it. */
        NOT_OWNED,
        /** It's an emote and all the emote slots are taken. */
        EMOTES_FULL
    }

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private PlayerState state;

    public Player(PlayerState start) {
        this.state = Objects.requireNonNull(start);
    }

    public PlayerState state() {
        return state;
    }

    public void addListener(Runnable listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    // ---- chests ----

    /**
     * How long until the chest can be opened; 0 when it can. Never more than the cooldown, so
     * a clock that was set back can't lock a chest for longer than one wait.
     */
    public long remainingMillis(ChestSlot slot, long nowEpochMillis) {
        return Math.clamp(state.readyAt(slot) - nowEpochMillis, 0, ChestRules.COOLDOWN_MILLIS);
    }

    /**
     * Whether the chest is drawn beside the pet.
     *
     * <ul>
     *   <li>With fewer taps than one chest costs, neither is drawn.
     *   <li>With enough for one chest but not two, only one is drawn: the first if its wait is
     *       over, otherwise the second.
     *   <li>With enough for two, both can be drawn.
     * </ul>
     *
     * A chest whose wait isn't over is drawn (as a countdown) only when {@code showWaiting}.
     *
     * @param totalTaps   the taps the player has left to spend
     * @param showWaiting whether chests that are still waiting show too
     */
    public boolean shown(ChestSlot slot, long totalTaps, long nowEpochMillis, boolean showWaiting) {
        if (totalTaps < ChestRules.OPEN_COST) {
            return false;
        }
        boolean firstCooled = remainingMillis(ChestSlot.FIRST, nowEpochMillis) == 0;
        if (totalTaps < ChestRules.BOTH_AT) {
            ChestSlot only = firstCooled ? ChestSlot.FIRST : ChestSlot.SECOND;
            if (slot != only) {
                return false;
            }
        }
        return remainingMillis(slot, nowEpochMillis) == 0 || showWaiting;
    }

    /** Whether the chest can be opened: it is drawn, its wait is over, and there are taps to pay for it. */
    public boolean ready(ChestSlot slot, long totalTaps, long nowEpochMillis) {
        return shown(slot, totalTaps, nowEpochMillis, false);
    }

    public boolean anyReady(long totalTaps, long nowEpochMillis) {
        for (ChestSlot slot : ChestSlot.values()) {
            if (ready(slot, totalTaps, nowEpochMillis)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Opens the chest if it's ready, using up {@link ChestRules#OPEN_COST} taps. Empty when it
     * isn't ready; nothing changes then.
     *
     * @param totalTaps the taps the player has left to spend
     */
    public Optional<Reward> open(ChestSlot slot, long totalTaps, long nowEpochMillis, RandomGenerator dice) {
        if (!ready(slot, totalTaps, nowEpochMillis)) {
            return Optional.empty();
        }
        Collectible found = ChestRules.roll(dice);
        Inventory bag = state.bag().plus(found);
        change(state.withBag(bag).withReadyAt(slot, nowEpochMillis + ChestRules.COOLDOWN_MILLIS)
                .withSpentTaps(state.spentTaps() + ChestRules.OPEN_COST));
        return Optional.of(new Reward(found, bag.count(found)));
    }

    // ---- the look ----

    /**
     * Uses a collectible from the bag: a role becomes the pet; an accessory is put on, or
     * taken off if it's the one being worn; an emote is added to the emote slots, or removed
     * if it's already there.
     */
    public UseResult use(Collectible item) {
        if (!state.bag().owns(item)) {
            return UseResult.NOT_OWNED;
        }
        Loadout look = state.look();
        Loadout next = switch (item) {
            case Role role -> look.withRole(role);
            case Accessory accessory -> look.withAccessory(
                    look.accessory().filter(accessory::equals).isPresent() ? Optional.empty() : Optional.of(accessory));
            case Emote emote -> {
                List<Emote> emotes = new ArrayList<>(look.emotes());
                if (!emotes.remove(emote)) {
                    if (emotes.size() >= Loadout.EMOTES_MAX) {
                        yield null;
                    }
                    emotes.add(emote);
                }
                yield look.withEmotes(emotes);
            }
        };
        if (next == null) {
            return UseResult.EMOTES_FULL;
        }
        // Looking at something by using it also clears its NEW mark.
        change(state.withLook(next).withBag(state.bag().seen(item)));
        return UseResult.CHANGED;
    }

    /** Whether {@code item} is part of the look right now. */
    public boolean inUse(Collectible item) {
        Loadout look = state.look();
        return switch (item) {
            case Role role -> look.role() == role;
            case Accessory accessory -> look.accessory().filter(accessory::equals).isPresent();
            case Emote emote -> look.emotes().contains(emote);
        };
    }

    // ---- presets ----

    /** Saves the current look in a preset slot, replacing what was there. */
    public void savePreset(int slot) {
        if (validSlot(slot)) {
            change(state.withPreset(slot, Optional.of(state.look())));
        }
    }

    /** Puts on the look saved in a preset slot. Returns whether there was one. */
    public boolean applyPreset(int slot) {
        if (!validSlot(slot) || state.presets().get(slot).isEmpty()) {
            return false;
        }
        change(state.withLook(state.presets().get(slot).get()));
        return true;
    }

    public void clearPreset(int slot) {
        if (validSlot(slot)) {
            change(state.withPreset(slot, Optional.empty()));
        }
    }

    // ---- the bag ----

    public void toggleFavourite(Collectible item) {
        Inventory bag = state.bag();
        if (bag.owns(item)) {
            change(state.withBag(bag.withFavourite(item, !bag.favourites().contains(item))));
        }
    }

    /** The player has looked at {@code item}: it is no longer NEW. */
    public void markSeen(Collectible item) {
        change(state.withBag(state.bag().seen(item)));
    }

    /** Makes a new collectible out of spare ones. Empty, with nothing changed, when there aren't enough. */
    public Optional<Reward> synthesize(ItemType type, Rarity rarity, Synthesis.Mode mode, RandomGenerator dice) {
        Optional<Synthesis.Result> result = Synthesis.make(state.bag(), type, rarity, mode, dice);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        Inventory bag = result.get().bag();
        change(state.withBag(bag));
        return Optional.of(new Reward(result.get().made(), bag.count(result.get().made())));
    }

    /** Makes a new collectible out of spare ones the player chose. Empty, with nothing changed, when they aren't valid. */
    public Optional<Reward> synthesize(java.util.Map<Collectible, Integer> chosen, Synthesis.Mode mode,
            RandomGenerator dice) {
        Optional<Synthesis.Result> result = Synthesis.makeFrom(state.bag(), chosen, mode, dice);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        Inventory bag = result.get().bag();
        change(state.withBag(bag));
        return Optional.of(new Reward(result.get().made(), bag.count(result.get().made())));
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < PlayerState.PRESETS;
    }

    private void change(PlayerState next) {
        if (next.equals(state)) {
            return;
        }
        state = next;
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
