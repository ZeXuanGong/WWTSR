package com.wwtsr.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * The bar under the pet: the tap count on a strip rounded at its left end, and at the right a
 * cap with a sparkle and a three-line button that opens the panel.
 */
public final class CounterBar extends Pane {

    public static final double WIDTH = 195;
    public static final double HEIGHT = 32;

    /** The largest count the bar shows as it is. */
    public static final long MAX_SHOWN = 99_999_999L;

    private static final double COUNT_WIDTH = 136;
    private static final double CAP_WIDTH = 61;
    /** The cap overlaps the count strip by this much. */
    private static final double OVERLAP = 2;
    /** The button takes up the cap's right-hand part. */
    private static final double BUTTON_WIDTH = 38;
    private static final Color PAPER = Color.web("#fdf9ed");
    private static final Color COCOA = Color.web("#8e5e43");
    private static final Color GOLD = Color.web("#ffc56e");

    private final Label number = new Label("0");

    /**
     * @param onMenu a click on the three-line button
     */
    public CounterBar(Runnable onMenu) {
        setMinSize(WIDTH, HEIGHT);
        setPrefSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);
        setEffect(new DropShadow(2, 0, 2, Color.rgb(0, 0, 0, 0.2)));

        number.setTextFill(COCOA);
        number.setFont(Font.font("System", FontWeight.BOLD, 18));
        number.setAlignment(Pos.CENTER_LEFT);
        number.setPadding(new Insets(0, 0, 0, 18));
        number.setMinSize(COUNT_WIDTH, HEIGHT);
        number.setMaxSize(COUNT_WIDTH, HEIGHT);
        number.setBackground(new Background(new BackgroundFill(PAPER,
                new CornerRadii(HEIGHT / 2, 0, 0, HEIGHT / 2, false), Insets.EMPTY)));

        Group cap = cap();
        cap.setLayoutX(COUNT_WIDTH - OVERLAP);

        // An unseen area over the cap's right-hand part takes the click.
        Rectangle button = new Rectangle(BUTTON_WIDTH, HEIGHT, Color.TRANSPARENT);
        button.setLayoutX(WIDTH - BUTTON_WIDTH);
        button.setCursor(Cursor.HAND);
        // Not the start of a drag, and not a click on the pet.
        button.setOnMousePressed(e -> e.consume());
        button.setOnMouseReleased(e -> e.consume());
        button.setOnMouseClicked(e -> {
            if (e.isStillSincePress()) {
                onMenu.run();
            }
            e.consume();
        });
        getChildren().addAll(number, cap, button);
    }

    /** Shows the count; one too big for the bar shows as the largest that fits, with a plus. */
    public void show(long count) {
        number.setText(count > MAX_SHOWN ? MAX_SHOWN + "+" : Long.toString(Math.max(0, count)));
    }

    /** The right-hand end: a dark disc with a sparkle, then three short lines. */
    private static Group cap() {
        Rectangle body = new Rectangle(CAP_WIDTH, HEIGHT, PAPER);
        // Square where it meets the count strip, round at the far end.
        Rectangle roundEnd = new Rectangle(CAP_WIDTH - HEIGHT, 0, HEIGHT, HEIGHT);
        roundEnd.setArcWidth(HEIGHT);
        roundEnd.setArcHeight(HEIGHT);
        roundEnd.setFill(PAPER);
        body.setWidth(CAP_WIDTH - HEIGHT / 2);

        double discX = 13;
        double middle = HEIGHT / 2;
        Circle disc = new Circle(discX, middle, 11, COCOA);
        double r = 7;
        double thin = 2;
        Polygon sparkle = new Polygon(discX, middle - r, discX + thin, middle - thin, discX + r, middle,
                discX + thin, middle + thin, discX, middle + r, discX - thin, middle + thin, discX - r, middle,
                discX - thin, middle - thin);
        sparkle.setFill(GOLD);

        Group cap = new Group(body, roundEnd, disc, sparkle);
        for (int i = -1; i <= 1; i++) {
            Line line = new Line(38, middle + i * 5, 50, middle + i * 5);
            line.setStroke(COCOA);
            line.setStrokeWidth(2.4);
            line.setStrokeLineCap(StrokeLineCap.ROUND);
            cap.getChildren().add(line);
        }
        return cap;
    }
}
