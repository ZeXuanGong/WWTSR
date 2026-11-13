package com.wwtsr.chest;

import com.wwtsr.collect.Collectible;

/**
 * What a chest (or a synthesis) gave.
 *
 * @param item  what was found
 * @param owned how many of it the player has now (1 means it's the first)
 */
public record Reward(Collectible item, int owned) {

    public boolean isFirst() {
        return owned == 1;
    }
}
