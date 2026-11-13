package com.wwtsr.app;

import com.wwtsr.assets.GeneratedAssetPack;
import com.wwtsr.view.FxImages;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javax.imageio.ImageIO;

/**
 * A developer tool: writes the app's icon as a Windows .ico file, for the packaged app's
 * launcher. Argument: the .ico file to write.
 */
public final class IconFile extends Application {

    private static final int[] SIZES = {16, 24, 32, 48, 64, 256};

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        List<String> args = getParameters().getUnnamed();
        if (args.isEmpty()) {
            System.err.println("usage: IconFile <output.ico>");
            Platform.exit();
            return;
        }
        BufferedImage icon = FxImages.toBuffered(new GeneratedAssetPack().icon());
        List<byte[]> pictures = new ArrayList<>();
        for (int size : SIZES) {
            BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(icon, 0, 0, size, size, null);
            g.dispose();
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(scaled, "png", png);
            pictures.add(png.toByteArray());
        }
        Files.write(Path.of(args.get(0)), ico(pictures));
        Platform.exit();
    }

    /** An .ico file: a directory of the pictures, then each picture as a PNG. */
    private static byte[] ico(List<byte[]> pictures) {
        int headers = 6 + 16 * pictures.size();
        int total = headers;
        for (byte[] picture : pictures) {
            total += picture.length;
        }
        ByteBuffer out = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        out.putShort((short) 0).putShort((short) 1).putShort((short) pictures.size());
        int offset = headers;
        for (int i = 0; i < pictures.size(); i++) {
            // A size of 256 is written as 0.
            byte side = (byte) (SIZES[i] >= 256 ? 0 : SIZES[i]);
            out.put(side).put(side).put((byte) 0).put((byte) 0);
            out.putShort((short) 1).putShort((short) 32);
            out.putInt(pictures.get(i).length).putInt(offset);
            offset += pictures.get(i).length;
        }
        for (byte[] picture : pictures) {
            out.put(picture);
        }
        return out.array();
    }
}
