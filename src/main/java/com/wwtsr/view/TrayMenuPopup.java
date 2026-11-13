package com.wwtsr.view;

import com.wwtsr.menu.MenuEntry;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Shows the app's menu at a point on the screen that isn't over any of the app's windows:
 * next to the tray icon. A JavaFX menu needs a window to belong to, so an invisible one-pixel
 * window is put at that point for as long as the menu is open; it also takes the keyboard
 * focus, so clicking anywhere else closes the menu.
 */
public final class TrayMenuPopup {

    private Stage anchor;
    private ContextMenu open;

    public void show(double screenX, double screenY, List<MenuEntry> entries) {
        hide();
        Stage stage = anchor();
        stage.setX(screenX);
        stage.setY(screenY);
        stage.show();
        stage.toFront();
        stage.requestFocus();

        ContextMenu menu = FxMenuRenderer.render(entries);
        menu.setOnHidden(e -> {
            if (open == menu) {
                open = null;
                stage.hide();
            }
        });
        open = menu;
        // The menu keeps itself on the screen, so near the taskbar it opens upwards.
        menu.show(stage, screenX, screenY);
    }

    public void hide() {
        if (open != null) {
            open.hide();
        }
    }

    private Stage anchor() {
        if (anchor == null) {
            // A utility window has no taskbar button; fully see-through, it can't be seen.
            Stage stage = new Stage(StageStyle.UTILITY);
            Region nothing = new Region();
            Scene scene = new Scene(nothing, 1, 1);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.setOpacity(0);
            stage.setAlwaysOnTop(true);
            stage.setWidth(1);
            stage.setHeight(1);
            // Losing focus without picking anything (a click elsewhere) closes the menu.
            stage.focusedProperty().addListener((o, was, focused) -> {
                if (!focused) {
                    hide();
                }
            });
            anchor = stage;
        }
        return anchor;
    }
}
