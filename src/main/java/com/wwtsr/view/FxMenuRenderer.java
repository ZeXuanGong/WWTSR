package com.wwtsr.view;

import com.wwtsr.menu.MenuEntry;
import java.util.List;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;

/** Draws a list of {@link MenuEntry} as a JavaFX right-click menu. */
public final class FxMenuRenderer {

    private FxMenuRenderer() {
    }

    public static ContextMenu render(List<MenuEntry> entries) {
        ContextMenu menu = new ContextMenu();
        for (MenuEntry entry : entries) {
            menu.getItems().add(item(entry));
        }
        return menu;
    }

    private static MenuItem item(MenuEntry entry) {
        return switch (entry) {
            case MenuEntry.Action action -> {
                MenuItem item = new MenuItem(action.label());
                item.setOnAction(e -> action.run().run());
                yield item;
            }
            case MenuEntry.Toggle toggle -> {
                CheckMenuItem item = new CheckMenuItem(toggle.label());
                item.setSelected(toggle.selected());
                item.setDisable(!toggle.enabled());
                item.setOnAction(e -> toggle.run().run());
                yield item;
            }
            case MenuEntry.Choice choice -> {
                Menu submenu = new Menu(choice.label());
                ToggleGroup group = new ToggleGroup();
                for (MenuEntry.Option option : choice.options()) {
                    RadioMenuItem item = new RadioMenuItem(option.label());
                    item.setToggleGroup(group);
                    item.setSelected(option.selected());
                    item.setOnAction(e -> option.run().run());
                    submenu.getItems().add(item);
                }
                yield submenu;
            }
            case MenuEntry.Separator separator -> new SeparatorMenuItem();
        };
    }
}
