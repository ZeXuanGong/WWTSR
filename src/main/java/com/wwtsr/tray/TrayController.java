package com.wwtsr.tray;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executor;

/**
 * The app's icon in the system tray. The tray belongs to AWT, so everything here is done on
 * AWT's thread, and what the person does with the icon is sent back to {@code runOn}.
 *
 * <p>The icon has no AWT menu of its own: AWT's native menus can't show Chinese text on
 * current Java versions (every character comes out as a box). A right-click is reported
 * instead, and the app shows its own menu there.
 */
public final class TrayController {

    /** Told where on the screen a menu was asked for. */
    @FunctionalInterface
    public interface MenuRequest {
        void at(int screenX, int screenY);
    }

    private final Executor runOn;
    private TrayIcon icon;

    /**
     * @param runOn where the callbacks run (the JavaFX thread in the app)
     */
    public TrayController(Executor runOn) {
        this.runOn = runOn;
    }

    /** Whether this computer has a system tray at all. */
    public static boolean supported() {
        try {
            return SystemTray.isSupported();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    /**
     * Puts the icon in the tray.
     *
     * @param onMenuRequested a right-click on the icon
     * @param onDoubleClick   a double-click on the icon
     */
    public void install(BufferedImage image, String tooltip, MenuRequest onMenuRequested, Runnable onDoubleClick) {
        EventQueue.invokeLater(() -> {
            if (icon != null || !supported()) {
                return;
            }
            TrayIcon created = new TrayIcon(image, tooltip);
            created.setImageAutoSize(true);
            created.addActionListener(e -> runOn.execute(onDoubleClick));
            created.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                        int x = e.getXOnScreen();
                        int y = e.getYOnScreen();
                        runOn.execute(() -> onMenuRequested.at(x, y));
                    }
                }
            });
            try {
                SystemTray.getSystemTray().add(created);
                icon = created;
            } catch (AWTException | RuntimeException e) {
                // No tray icon: the pet's right-click menu still has everything.
            }
        });
    }

    public void setTooltip(String tooltip) {
        EventQueue.invokeLater(() -> {
            if (icon != null) {
                icon.setToolTip(tooltip);
            }
        });
    }

    public void remove() {
        EventQueue.invokeLater(() -> {
            if (icon != null) {
                SystemTray.getSystemTray().remove(icon);
                icon = null;
            }
        });
    }
}
