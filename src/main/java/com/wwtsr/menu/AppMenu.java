package com.wwtsr.menu;

import com.wwtsr.i18n.Messages;
import com.wwtsr.menu.MenuEntry.Action;
import com.wwtsr.menu.MenuEntry.Choice;
import com.wwtsr.menu.MenuEntry.Option;
import com.wwtsr.menu.MenuEntry.Separator;
import com.wwtsr.menu.MenuEntry.Toggle;
import com.wwtsr.settings.Settings;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the app's right-click and tray menu from the current settings: the way into the
 * panel's pages, and the few things worth reaching without opening it.
 */
public final class AppMenu {

    static final List<Integer> SIZES = List.of(50, 75, 100, 125, 150, 200);
    static final List<Integer> OPACITIES = List.of(100, 80, 60, 40);

    private AppMenu() {
    }

    public static List<MenuEntry> build(Settings s, Messages m, MenuActions actions) {
        List<MenuEntry> menu = new ArrayList<>();
        for (PanelPage page : PanelPage.values()) {
            menu.add(new Action(m.get("nav." + page.name()), () -> actions.openPanel(page)));
        }
        menu.add(new Separator());

        String key = actions.hidePetKeyName();
        String toggle = m.get(actions.petVisible() ? "menu.hide" : "menu.show");
        menu.add(new Action(key.isEmpty() ? toggle : m.get("menu.withKey", toggle, key), actions::togglePetVisible));

        List<Option> sizes = new ArrayList<>();
        for (int size : SIZES) {
            sizes.add(new Option(m.get("menu.percent", size), s.scalePercent() == size,
                    () -> actions.changeSettings(x -> x.withScalePercent(size))));
        }
        menu.add(new Choice(m.get("setting.scale"), sizes));

        List<Option> opacities = new ArrayList<>();
        for (int opacity : OPACITIES) {
            opacities.add(new Option(m.get("menu.percent", opacity), s.opacityPercent() == opacity,
                    () -> actions.changeSettings(x -> x.withOpacityPercent(opacity))));
        }
        menu.add(new Choice(m.get("setting.opacity"), opacities));

        menu.add(new Toggle(m.get("setting.lockPosition"), s.lockPosition(), true,
                () -> actions.changeSettings(x -> x.withLockPosition(!x.lockPosition()))));
        menu.add(new Separator());

        if (actions.restartAvailable()) {
            menu.add(new Action(m.get("menu.restart"), actions::restart));
        }
        menu.add(new Action(m.get("menu.quit"), actions::quit));
        return List.copyOf(menu);
    }
}
