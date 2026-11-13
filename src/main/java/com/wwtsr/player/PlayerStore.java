package com.wwtsr.player;

import java.io.IOException;

/** Where the player's state is kept between runs. */
public interface PlayerStore {

    /** The saved state; anything missing or unusable is left out. Never throws. */
    PlayerState load();

    void save(PlayerState state) throws IOException;
}
