package com.wwtsr.panel;

import com.wwtsr.assets.AssetPack;
import com.wwtsr.i18n.Messages;
import com.wwtsr.player.Player;
import com.wwtsr.settings.AppSettings;
import java.util.random.RandomGenerator;

/** What the panel needs from the app it lives in. */
public interface PanelHost {

    Player player();

    AppSettings settings();

    /** The text in the language in use now. */
    Messages messages();

    AssetPack assets();

    long totalTaps();

    RandomGenerator dice();

    String version();

    boolean petVisible();

    /** Shows or hides the pet. */
    void setPetVisible(boolean visible);

    /** How many screens there are. */
    int screenCount();

    /** Moves the pet to the next screen. */
    void switchScreen();

    boolean launchAtLoginAvailable();

    void setLaunchAtLogin(boolean on);

    /** While a new shortcut key is being chosen, pressing keys must not trigger the old ones. */
    void setHotkeysPaused(boolean paused);

    /** Opens the folder the app keeps its files in. Returns whether that worked. */
    boolean openDataFolder();

    boolean restartAvailable();

    void restart();

    void quit();
}
