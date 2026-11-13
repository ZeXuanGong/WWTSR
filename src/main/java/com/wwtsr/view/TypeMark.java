package com.wwtsr.view;

import com.wwtsr.collect.ItemType;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;

/** The little sign for each kind of collectible: a head for a role, a crown for an accessory, a face for an emote. */
public final class TypeMark {

    private TypeMark() {
    }

    /**
     * @param cx     the sign's middle
     * @param cy     the sign's middle
     * @param colour what the sign is drawn in
     * @param ground the colour behind it (used for the face's eyes and smile)
     */
    public static Node of(ItemType type, double cx, double cy, Color colour, Color ground) {
        Group mark = new Group();
        switch (type) {
            case ROLE -> mark.getChildren().addAll(new Circle(cx - 5, cy - 5, 3.2, colour),
                    new Circle(cx + 5, cy - 5, 3.2, colour), new Circle(cx, cy + 1, 7, colour));
            case ACCESSORY -> {
                Polygon crown = new Polygon(cx - 8, cy + 6, cx - 8, cy - 5, cx - 3, cy, cx, cy - 7, cx + 3, cy,
                        cx + 8, cy - 5, cx + 8, cy + 6);
                crown.setFill(colour);
                mark.getChildren().add(crown);
            }
            case EMOTE -> {
                Arc smile = new Arc(cx, cy + 1, 4.5, 4, 200, 140);
                smile.setType(ArcType.OPEN);
                smile.setFill(null);
                smile.setStroke(ground);
                smile.setStrokeWidth(1.6);
                mark.getChildren().addAll(new Circle(cx, cy, 8, colour), new Circle(cx - 3, cy - 2, 1.3, ground),
                        new Circle(cx + 3, cy - 2, 1.3, ground), smile);
            }
        }
        return mark;
    }
}
