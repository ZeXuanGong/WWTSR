package com.wwtsr.menu;

import com.wwtsr.settings.Settings;
import java.util.function.UnaryOperator;

/** What the menu can ask the app to do. */
public interface MenuActions {

    /** Opens the panel on that page. */
    void openPanel(PanelPage page);

    boolean petVisible();

    void togglePetVisible();

    /** The name of the key that shows and hides the pet ("F9"), or empty when none is set. */
    String hidePetKeyName();

    void changeSettings(UnaryOperator<Settings> change);

    /** Whether the app can start itself again (it needs an installed app to start). */
    boolean restartAvailable();

    void restart();

    void quit();
}
