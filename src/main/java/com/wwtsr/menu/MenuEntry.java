package com.wwtsr.menu;

import java.util.List;

/**
 * One line of a menu, described without any UI toolkit. The tray (AWT) and the pet's
 * right-click menu (JavaFX) are both drawn from the same list of these.
 */
public sealed interface MenuEntry {

    /** Something to do. */
    record Action(String label, Runnable run) implements MenuEntry {
    }

    /** Something that is on or off. */
    record Toggle(String label, boolean selected, boolean enabled, Runnable run) implements MenuEntry {
    }

    /** A submenu where one option is picked. */
    record Choice(String label, List<Option> options) implements MenuEntry {
        public Choice {
            options = List.copyOf(options);
        }
    }

    /** One option of a {@link Choice}. */
    record Option(String label, boolean selected, Runnable run) {
    }

    /** A dividing line. */
    record Separator() implements MenuEntry {
    }
}
