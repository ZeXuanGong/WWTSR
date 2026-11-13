package com.wwtsr.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * A row of tabs on a pale bar, spread evenly along it, with a honey-coloured pill that slides
 * under the picked one.
 */
final class Tabs<T> extends Pane {

    private static final double HEIGHT = 36;
    /** The pill is a little taller than the bar. */
    private static final double SLIDER_HEIGHT = 40;
    private static final Duration SLIDE = Duration.millis(280);

    private final List<T> choices;
    private final List<Label> labels = new ArrayList<>();
    private final Region slider = new Region();
    private final DoubleProperty sliderX = new SimpleDoubleProperty();
    private final DoubleProperty sliderWidth = new SimpleDoubleProperty();
    private final Consumer<T> onPicked;
    private T selected;
    private boolean placed;

    /**
     * @param choices     what can be picked, in order
     * @param first       what is picked to begin with
     * @param label       the text for each choice
     * @param sidePadding the room on each side of a tab's text, inside its pill
     * @param onPicked    told when the pick changes
     */
    Tabs(List<T> choices, T first, Function<T, String> label, double sidePadding, Consumer<T> onPicked) {
        this.choices = List.copyOf(choices);
        this.onPicked = onPicked;
        this.selected = first;
        getStyleClass().add("tabs");
        setMinHeight(HEIGHT);
        setPrefHeight(HEIGHT);
        setMaxHeight(HEIGHT);
        slider.getStyleClass().add("tabs-slider");
        slider.setManaged(false);
        getChildren().add(slider);
        for (T choice : choices) {
            Label text = new Label(label.apply(choice));
            text.getStyleClass().add("tab-text");
            text.setPadding(new Insets(0, sidePadding, 0, sidePadding));
            text.setManaged(false);
            text.setOnMouseClicked(e -> pick(choice));
            labels.add(text);
            getChildren().add(text);
        }
        sliderX.addListener(o -> placeSlider());
        sliderWidth.addListener(o -> placeSlider());
        restyle();
    }

    T selected() {
        return selected;
    }

    private void pick(T choice) {
        if (choice.equals(selected)) {
            return;
        }
        selected = choice;
        restyle();
        Label target = labels.get(choices.indexOf(choice));
        new Timeline(new KeyFrame(SLIDE,
                new KeyValue(sliderX, target.getLayoutX(), Interpolator.EASE_BOTH),
                new KeyValue(sliderWidth, target.getWidth(), Interpolator.EASE_BOTH))).play();
        onPicked.accept(choice);
    }

    private void restyle() {
        for (int i = 0; i < labels.size(); i++) {
            labels.get(i).getStyleClass().remove("tab-text-active");
            if (choices.get(i).equals(selected)) {
                labels.get(i).getStyleClass().add("tab-text-active");
            }
        }
    }

    /** The tabs are spread along the bar with equal room around each. */
    @Override
    protected void layoutChildren() {
        double total = 0;
        for (Label text : labels) {
            total += text.prefWidth(-1);
        }
        double room = Math.max(0, (getWidth() - total) / labels.size());
        double x = room / 2;
        for (Label text : labels) {
            double w = text.prefWidth(-1);
            text.resizeRelocate(x, 0, w, HEIGHT);
            x += w + room;
        }
        // The first time the bar has a size, the pill goes straight under the picked tab.
        if (!placed) {
            Label target = labels.get(Math.max(0, choices.indexOf(selected)));
            sliderX.set(target.getLayoutX());
            sliderWidth.set(target.getWidth());
            placed = getWidth() > 0;
        }
        placeSlider();
    }

    private void placeSlider() {
        slider.resizeRelocate(sliderX.get(), (HEIGHT - SLIDER_HEIGHT) / 2, sliderWidth.get(), SLIDER_HEIGHT);
    }
}
