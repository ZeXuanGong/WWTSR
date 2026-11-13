package com.wwtsr.view;

import java.util.ArrayList;
import java.util.List;
import javafx.stage.Screen;

/** The computer's screens in a steady order: the main one first, then the others. */
public final class Screens {

    private Screens() {
    }

    public static List<Screen> ordered() {
        Screen main = Screen.getPrimary();
        List<Screen> all = new ArrayList<>();
        all.add(main);
        for (Screen screen : Screen.getScreens()) {
            if (!screen.equals(main)) {
                all.add(screen);
            }
        }
        return all;
    }

    /** The screen with that number, or the main one when there is no such screen. */
    public static Screen at(int index) {
        List<Screen> all = ordered();
        return index >= 0 && index < all.size() ? all.get(index) : all.get(0);
    }
}
