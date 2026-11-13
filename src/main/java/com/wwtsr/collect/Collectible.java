package com.wwtsr.collect;

/**
 * Something that can be found in a chest, kept in the bag and used: a {@link Role}, an
 * {@link Accessory} or an {@link Emote}.
 */
public sealed interface Collectible permits Role, Accessory, Emote {

    /** A name that never changes, used in saved files and to look up text: {@code accessory.CROWN}. */
    String id();

    ItemType type();

    Rarity rarity();

    Series series();
}
