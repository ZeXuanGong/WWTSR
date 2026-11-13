package com.wwtsr.collect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A look: who the pet is, what it wears and which emotes it has ready. The same shape is used
 * for what is on now and for each saved preset. Immutable.
 *
 * @param role      who the pet is
 * @param accessory what it wears, if anything
 * @param emotes    the emotes it can show, in order, at most {@value #EMOTES_MAX}, no repeats
 */
public record Loadout(Role role, Optional<Accessory> accessory, List<Emote> emotes) {

    public static final int EMOTES_MAX = 5;

    public static final Loadout STARTER = new Loadout(Role.STARTER, Optional.empty(), List.of());

    public Loadout {
        Objects.requireNonNull(role);
        Objects.requireNonNull(accessory);
        List<Emote> kept = new ArrayList<>();
        for (Emote emote : emotes) {
            if (emote != null && !kept.contains(emote) && kept.size() < EMOTES_MAX) {
                kept.add(emote);
            }
        }
        emotes = Collections.unmodifiableList(kept);
    }

    public Loadout withRole(Role value) {
        return new Loadout(value, accessory, emotes);
    }

    public Loadout withAccessory(Optional<Accessory> value) {
        return new Loadout(role, value, emotes);
    }

    public Loadout withEmotes(List<Emote> value) {
        return new Loadout(role, accessory, value);
    }

    /** This look with anything the bag doesn't hold taken out. */
    public Loadout fitting(Inventory bag) {
        Role fitRole = bag.owns(role) ? role : Role.STARTER;
        Optional<Accessory> fitAccessory = accessory.filter(bag::owns);
        List<Emote> fitEmotes = emotes.stream().filter(bag::owns).toList();
        return new Loadout(fitRole, fitAccessory, fitEmotes);
    }
}
