package com.wwtsr.collect;

/** A little picture the pet can pop up to show how it feels. */
public enum Emote implements Collectible {
    IDLE(Rarity.COMMON, Series.CLASSIC),
    HAPPY(Rarity.COMMON, Series.CLASSIC),
    SURPRISED(Rarity.COMMON, Series.MEADOW),
    ANGRY(Rarity.UNCOMMON, Series.CLASSIC),
    PAIN(Rarity.UNCOMMON, Series.MEADOW),
    EFFORT(Rarity.RARE, Series.PARTY),
    STUNNED(Rarity.RARE, Series.MEADOW),
    THREATEN(Rarity.EPIC, Series.PARTY),
    TRIUMPH(Rarity.EPIC, Series.PARTY);

    private final Rarity rarity;
    private final Series series;

    Emote(Rarity rarity, Series series) {
        this.rarity = rarity;
        this.series = series;
    }

    @Override
    public String id() {
        return "emote." + name();
    }

    @Override
    public ItemType type() {
        return ItemType.EMOTE;
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
