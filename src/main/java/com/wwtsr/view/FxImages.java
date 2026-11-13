package com.wwtsr.view;

import java.awt.image.BufferedImage;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

/** Turning JavaFX pictures into the kind the rest of Java (files, the tray) understands. */
public final class FxImages {

    private FxImages() {
    }

    public static BufferedImage toBuffered(Image image) {
        int w = Math.max(1, (int) image.getWidth());
        int h = Math.max(1, (int) image.getHeight());
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = image.getPixelReader();
        if (reader == null) {
            return out;
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        return out;
    }
}
