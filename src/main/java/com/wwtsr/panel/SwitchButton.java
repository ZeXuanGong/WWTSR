package com.wwtsr.panel;

import java.util.function.Consumer;
import javafx.animation.TranslateTransition;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * An on/off switch: a small pill with a round knob that slides to the right when it is on.
 * Off it is honey-coloured, on it is brown.
 */
final class SwitchButton extends Pane {

    private static final double WIDTH = 44;
    private static final double HEIGHT = 22;
    private static final double KNOB = 7;
    /** How far the knob slides. */
    private static final double TRAVEL = 22;
    private static final Duration SLIDE = Duration.millis(300);

    private final Region track = new Region();
    private final Circle knob = new Circle(KNOB);
    private boolean on;

    /**
     * @param onChange told the new state after a click
     */
    SwitchButton(boolean on, Consumer<Boolean> onChange) {
        setMinSize(WIDTH, HEIGHT);
        setPrefSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);
        track.getStyleClass().add("switch-track");
        track.setMinSize(WIDTH, HEIGHT);
        track.setPrefSize(WIDTH, HEIGHT);
        track.setMaxSize(WIDTH, HEIGHT);
        knob.getStyleClass().add("switch-knob");
        knob.setCenterX(4 + KNOB);
        knob.setCenterY(HEIGHT / 2);
        knob.setMouseTransparent(true);
        getChildren().addAll(track, knob);
        show(on, false);
        track.setOnMouseClicked(e -> {
            if (!isDisabled()) {
                show(!this.on, true);
                onChange.accept(this.on);
            }
        });
        disabledProperty().addListener((o, was, disabled) -> setOpacity(disabled ? 0.5 : 1));
    }

    boolean isOn() {
        return on;
    }

    /** Shows a state without telling anyone (the setting changed somewhere else). */
    void setOn(boolean value) {
        if (value != on) {
            show(value, true);
        }
    }

    private void show(boolean value, boolean slide) {
        on = value;
        if (value) {
            if (!track.getStyleClass().contains("switch-track-on")) {
                track.getStyleClass().add("switch-track-on");
            }
        } else {
            track.getStyleClass().remove("switch-track-on");
        }
        double to = value ? TRAVEL : 0;
        if (slide) {
            TranslateTransition move = new TranslateTransition(SLIDE, knob);
            move.setToX(to);
            move.play();
        } else {
            knob.setTranslateX(to);
        }
    }
}
