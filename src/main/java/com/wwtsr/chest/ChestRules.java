package com.wwtsr.chest;

import com.wwtsr.collect.Catalog;
import com.wwtsr.collect.Collectible;
import com.wwtsr.collect.Rarity;
import java.util.List;
import java.util.random.RandomGenerator;

/** The rules of chests that don't depend on any one player. */
public final class ChestRules {

    /** The wait between opening a chest and opening it again. */
    public static final long COOLDOWN_MILLIS = 30 * 60 * 1_000L;

    /** How many taps opening a chest uses up. */
    public static final long OPEN_COST = 1_000;

    /** With at least this many taps both chests show at once; with fewer, one at a time. */
    public static final long BOTH_AT = 2_000;

    private ChestRules() {
    }

    /** What a chest gives: a rarity by weight, then any collectible of that rarity. */
    public static Collectible roll(RandomGenerator dice) {
        int total = 0;
        for (Rarity rarity : Rarity.values()) {
            total += rarity.weight();
        }
        int pick = dice.nextInt(total);
        Rarity rolled = Rarity.COMMON;
        for (Rarity rarity : Rarity.values()) {
            if (pick < rarity.weight()) {
                rolled = rarity;
                break;
            }
            pick -= rarity.weight();
        }
        List<Collectible> choices = Catalog.of(rolled);
        return choices.get(dice.nextInt(choices.size()));
    }
}
