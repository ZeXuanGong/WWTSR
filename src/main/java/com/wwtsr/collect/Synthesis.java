package com.wwtsr.collect;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Turning spare collectibles into a new one. Materials are always spare copies (those beyond
 * the first), so making something never loses a collectible the player has.
 */
public final class Synthesis {

    /** The two recipes. */
    public enum Mode {
        /** Ten of one kind and rarity make one of the next rarity up. */
        UPGRADE(10),
        /** Five of one kind and rarity make one of the same rarity. */
        SAME_TIER(5);

        private final int cost;

        Mode(int cost) {
            this.cost = cost;
        }

        public int cost() {
            return cost;
        }
    }

    /**
     * What a synthesis did.
     *
     * @param bag  the bag afterwards
     * @param made what was made
     * @param used what was used up, and how many of each
     */
    public record Result(Inventory bag, Collectible made, Map<Collectible, Integer> used) {
    }

    private Synthesis() {
    }

    /** How many spare copies of that kind and rarity the bag holds. */
    public static int spare(Inventory bag, ItemType type, Rarity rarity) {
        int total = 0;
        for (Collectible item : Catalog.of(type, rarity)) {
            total += bag.spare(item);
        }
        return total;
    }

    /** The rarity the recipe makes, or empty when there is nothing above (upgrading the top rarity). */
    public static Optional<Rarity> resultRarity(Rarity rarity, Mode mode) {
        return mode == Mode.UPGRADE ? rarity.next() : Optional.of(rarity);
    }

    public static boolean possible(Inventory bag, ItemType type, Rarity rarity, Mode mode) {
        return resultRarity(rarity, mode).isPresent() && spare(bag, type, rarity) >= mode.cost();
    }

    /**
     * Which spare copies the recipe would use: from the collectibles with the most spares
     * first. Empty when there aren't enough.
     */
    public static Map<Collectible, Integer> materials(Inventory bag, ItemType type, Rarity rarity, Mode mode) {
        if (!possible(bag, type, rarity, mode)) {
            return Map.of();
        }
        List<Collectible> richest = Catalog.of(type, rarity).stream()
                .sorted(Comparator.comparingInt(bag::spare).reversed())
                .toList();
        Map<Collectible, Integer> used = new LinkedHashMap<>();
        int needed = mode.cost();
        for (Collectible item : richest) {
            int take = Math.min(needed, bag.spare(item));
            if (take > 0) {
                used.put(item, take);
                needed -= take;
            }
        }
        return used;
    }

    /**
     * Whether the chosen materials make a valid recipe: exactly as many as it costs, all of one
     * kind and one rarity, none of them a collectible's last copy, and something above to make.
     */
    public static boolean valid(Inventory bag, Map<Collectible, Integer> chosen, Mode mode) {
        int total = 0;
        Collectible first = null;
        for (Map.Entry<Collectible, Integer> entry : chosen.entrySet()) {
            Collectible item = entry.getKey();
            int count = entry.getValue() == null ? 0 : entry.getValue();
            if (count <= 0) {
                continue;
            }
            if (first == null) {
                first = item;
            } else if (item.type() != first.type() || item.rarity() != first.rarity()) {
                return false;
            }
            if (count > bag.spare(item)) {
                return false;
            }
            total += count;
        }
        return first != null && total == mode.cost() && resultRarity(first.rarity(), mode).isPresent();
    }

    /** Runs the recipe with materials the player chose. Empty, with nothing changed, when they aren't valid. */
    public static Optional<Result> makeFrom(Inventory bag, Map<Collectible, Integer> chosen, Mode mode,
            RandomGenerator dice) {
        if (!valid(bag, chosen, mode)) {
            return Optional.empty();
        }
        Collectible first = chosen.entrySet().stream().filter(e -> e.getValue() > 0).findFirst().orElseThrow().getKey();
        List<Collectible> choices = Catalog.of(first.type(), resultRarity(first.rarity(), mode).orElseThrow());
        if (choices.isEmpty()) {
            return Optional.empty();
        }
        Inventory after = bag;
        Map<Collectible, Integer> used = new LinkedHashMap<>();
        for (Map.Entry<Collectible, Integer> entry : chosen.entrySet()) {
            if (entry.getValue() > 0) {
                after = after.minusSpare(entry.getKey(), entry.getValue());
                used.put(entry.getKey(), entry.getValue());
            }
        }
        Collectible made = choices.get(dice.nextInt(choices.size()));
        return Optional.of(new Result(after.plus(made), made, used));
    }

    /** Runs the recipe. Empty, with nothing changed, when it isn't possible. */
    public static Optional<Result> make(Inventory bag, ItemType type, Rarity rarity, Mode mode, RandomGenerator dice) {
        Map<Collectible, Integer> used = materials(bag, type, rarity, mode);
        Optional<Rarity> makes = resultRarity(rarity, mode);
        if (used.isEmpty() || makes.isEmpty()) {
            return Optional.empty();
        }
        List<Collectible> choices = Catalog.of(type, makes.get());
        if (choices.isEmpty()) {
            return Optional.empty();
        }
        Inventory after = bag;
        for (Map.Entry<Collectible, Integer> entry : used.entrySet()) {
            after = after.minusSpare(entry.getKey(), entry.getValue());
        }
        Collectible made = choices.get(dice.nextInt(choices.size()));
        return Optional.of(new Result(after.plus(made), made, used));
    }
}
