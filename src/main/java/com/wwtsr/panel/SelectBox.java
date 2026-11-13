package com.wwtsr.panel;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.stage.Popup;

/**
 * A drop-down: a pill showing the picked value with a small arrow; a click opens a rounded
 * list underneath where each line ends in a dot that lights up for the picked one.
 */
final class SelectBox<T> extends HBox {

    private static final double HEIGHT = 30;
    /** The list opens this far below the pill's top. */
    private static final double LIST_BELOW = 44;

    private final List<T> values;
    private final Function<T, String> label;
    private final Function<T, String> listLabel;
    private final Consumer<T> onPicked;
    private final Label text = new Label();
    private final Polygon arrow = new Polygon(0, 0, 14, 0, 7, 9);
    private final Popup popup = new Popup();
    private T value;

    /**
     * @param values   what can be picked, in order
     * @param current  what is picked now
     * @param label    the text for each value
     * @param onPicked told when another value is picked
     */
    SelectBox(List<T> values, T current, Function<T, String> label, Consumer<T> onPicked) {
        this(values, current, label, label, onPicked);
    }

    /**
     * @param label     the text shown on the pill for the picked value
     * @param listLabel the text for each value in the list
     */
    SelectBox(List<T> values, T current, Function<T, String> label, Function<T, String> listLabel,
            Consumer<T> onPicked) {
        this.values = List.copyOf(values);
        this.label = label;
        this.listLabel = listLabel;
        this.onPicked = onPicked;
        this.value = current;
        getStyleClass().add("select-input");
        setMinHeight(HEIGHT);
        setPrefHeight(HEIGHT);
        setMaxHeight(HEIGHT);
        setMinWidth(136);
        text.getStyleClass().add("select-text");
        text.setText(label.apply(current));
        arrow.getStyleClass().add("select-arrow");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        getChildren().addAll(text, gap, arrow);
        popup.setAutoHide(true);
        popup.setOnHidden(e -> arrow.setRotate(0));
        setOnMouseClicked(e -> {
            if (popup.isShowing()) {
                popup.hide();
            } else {
                open();
            }
        });
    }

    private void open() {
        VBox list = new VBox();
        list.getStyleClass().add("select-list");
        list.getStylesheets().addAll(getScene().getStylesheets());
        list.setMinWidth(getWidth());
        for (int i = 0; i < values.size(); i++) {
            T each = values.get(i);
            Label name = new Label(listLabel.apply(each));
            name.getStyleClass().add("select-option-text");
            Circle dot = new Circle(7);
            dot.getStyleClass().add("select-dot");
            if (Objects.equals(each, value)) {
                dot.getStyleClass().add("select-dot-on");
            }
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            HBox option = new HBox(name, gap, dot);
            option.getStyleClass().add("select-option");
            if (i == values.size() - 1) {
                option.getStyleClass().add("select-option-last");
            }
            option.setAlignment(Pos.CENTER_LEFT);
            option.setOnMouseClicked(e -> {
                popup.hide();
                if (!Objects.equals(each, value)) {
                    value = each;
                    text.setText(label.apply(each));
                    onPicked.accept(each);
                }
            });
            list.getChildren().add(option);
        }
        popup.getContent().setAll(list);
        Bounds here = localToScreen(getBoundsInLocal());
        arrow.setRotate(180);
        popup.show(getScene().getWindow(), here.getMinX(), here.getMinY() + LIST_BELOW);
    }
}
