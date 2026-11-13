package com.wwtsr.collect;

/** Something the pet can wear. */
public enum Accessory implements Collectible {
    LEAF(Rarity.COMMON, Series.MEADOW),
    BOW(Rarity.COMMON, Series.CLASSIC),
    GLASSES(Rarity.UNCOMMON, Series.CLASSIC),
    FLOWER(Rarity.UNCOMMON, Series.MEADOW),
    HEADPHONES(Rarity.RARE, Series.PARTY),
    WIZARD_HAT(Rarity.RARE, Series.PARTY),
    CROWN(Rarity.EPIC, Series.PARTY),
    HALO(Rarity.EPIC, Series.CLASSIC);

    private final Rarity rarity;
    private final Series series;

    Accessory(Rarity rarity, Series series) {
        this.rarity = rarity;
        this.series = series;
    }

    @Override
    public String id() {
        return "accessory." + name();
    }

    @Override
    public ItemType type() {
        return ItemType.ACCESSORY;
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
