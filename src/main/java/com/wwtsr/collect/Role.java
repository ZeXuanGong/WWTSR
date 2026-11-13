package com.wwtsr.collect;

/** Who the pet is. Everyone starts with {@link #STARTER}. */
public enum Role implements Collectible {
    BEAR(Rarity.COMMON, Series.CLASSIC),
    CAT(Rarity.UNCOMMON, Series.CLASSIC),
    BUNNY(Rarity.RARE, Series.MEADOW),
    FROG(Rarity.EPIC, Series.MEADOW);

    /** The role every new player has. */
    public static final Role STARTER = BEAR;

    private final Rarity rarity;
    private final Series series;

    Role(Rarity rarity, Series series) {
        this.rarity = rarity;
        this.series = series;
    }

    @Override
    public String id() {
        return "role." + name();
    }

    @Override
    public ItemType type() {
        return ItemType.ROLE;
    }

    @Override
    public Rarity rarity() {
        return rarity;
    }

    @Override
    public Series series() {
        return series;
    }
}
