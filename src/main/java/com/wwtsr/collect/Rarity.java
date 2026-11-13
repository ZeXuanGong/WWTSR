package com.wwtsr.collect;

import java.util.Optional;

/** How rare a collectible is. A chest rolls a rarity by {@link #weight()}, out of the weights' sum. */
public enum Rarity {
    COMMON(60, "#6fcf73"),
    UNCOMMON(25, "#5aa9f0"),
    RARE(12, "#b07cf5"),
    EPIC(3, "#f5c542");

    private final int weight;
    private final String colour;

    Rarity(int weight, String colour) {
        this.weight = weight;
        this.colour = colour;
    }

    public int weight() {
        return weight;
    }

    /** The rarity's colour, as a web colour such as {@code #6fcf73}. */
    public String colour() {
        return colour;
    }

    /** The rarity one step up, if there is one. */
    public Optional<Rarity> next() {
        int i = ordinal() + 1;
        return i < values().length ? Optional.of(values()[i]) : Optional.empty();
    }
}
