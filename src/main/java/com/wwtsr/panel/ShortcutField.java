package com.wwtsr.panel;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Shows one shortcut key in a pill, with a small round button at its right end that clears it.
 * A click on the pill asks to choose a new key; while one is being chosen the pill's rim
 * darkens and it shows a prompt.
 */
final class ShortcutField extends StackPane {

    private static final double MIN_WIDTH = 90;
    private static final double HEIGHT = 30;

    private final Label text = new Label();
    private final Label clear = new Label("✕");

    /**
     * @param onClick a click on the pill
     * @param onClear a click on the clear button
     */
    ShortcutField(Runnable onClick, Runnable onClear) {
        getStyleClass().add("shortcut");
        setMinSize(MIN_WIDTH, HEIGHT);
        setPrefHeight(HEIGHT);
        setMaxHeight(HEIGHT);
        setPadding(new Insets(0, 34, 0, 12));
        setAlignment(Pos.CENTER);

        clear.getStyleClass().add("shortcut-clear");
        clear.setMinSize(16, 16);
        clear.setMaxSize(16, 16);
        StackPane.setAlignment(clear, Pos.CENTER_RIGHT);
        // The pill's right padding is for this button; it sits 10 from the pill's edge.
        StackPane.setMargin(clear, new Insets(0, -24, 0, 0));
        clear.setOnMouseClicked(e -> {
            onClear.run();
            e.consume();
        });
        setOnMouseClicked(e -> onClick.run());
        getChildren().addAll(text, clear);
    }

    /** Shows a key's name. */
    void showKey(String name) {
        show(name, "shortcut-text", false);
        clear.setDisable(false);
        clear.setOpacity(1);
    }

    /** Shows that no key is set, with a prompt to click. */
    void showUnset(String prompt) {
        show(prompt, "shortcut-placeholder", false);
        clear.setDisable(true);
        clear.setOpacity(0.3);
    }

    /** Shows that the next key pressed will become the shortcut. */
    void showRecording(String prompt) {
        show(prompt, "shortcut-placeholder", true);
    }

    private void show(String value, String styleClass, boolean recording) {
        text.setText(value);
        text.getStyleClass().setAll("label", styleClass);
        if (recording) {
            if (!getStyleClass().contains("shortcut-recording")) {
                getStyleClass().add("shortcut-recording");
            }
        } else {
            getStyleClass().remove("shortcut-recording");
        }
    }
}
