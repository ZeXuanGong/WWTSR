package com.wwtsr.settings;

import com.wwtsr.input.Shortcut;
import com.wwtsr.io.AtomicFiles;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Properties;

/**
 * Keeps the settings in a small text file with a {@code version} line. Each value is read on
 * its own: one that is missing or damaged takes its default and the rest are kept. A file in
 * a newer format reads as the defaults rather than being misread.
 */
public final class FileSettingsStore implements SettingsStore {

    static final int VERSION = 1;

    private final Path file;

    public FileSettingsStore(Path file) {
        this.file = Objects.requireNonNull(file);
    }

    @Override
    public Settings load() {
        Settings d = Settings.DEFAULT;
        Properties p = AtomicFiles.readProperties(file).orElse(null);
        if (p == null || number(p, "version", -1) != VERSION) {
            return d;
        }
        return new Settings(
                choice(p, "corner", Corner.class, d.corner()),
                number(p, "scalePercent", d.scalePercent()),
                number(p, "opacityPercent", d.opacityPercent()),
                flag(p, "lockPosition", d.lockPosition()),
                flag(p, "fadeOnHover", d.fadeOnHover()),
                flag(p, "alwaysShowChests", d.alwaysShowChests()),
                flag(p, "gameMode", d.gameMode()),
                flag(p, "autoGameMode", d.autoGameMode()),
                flag(p, "launchAtLogin", d.launchAtLogin()),
                flag(p, "showTaskbarIcon", d.showTaskbarIcon()),
                choice(p, "language", Language.class, d.language()),
                p.getProperty("nickname", d.nickname()),
                number(p, "monitor", d.monitor()),
                shortcut(p, "hidePetKey", d.hidePetKey()),
                shortcut(p, "switchScreenKey", d.switchScreenKey()),
                shortcut(p, "gameModeKey", d.gameModeKey()),
                placement(p));
    }

    @Override
    public void save(Settings s) throws IOException {
        Properties p = new Properties();
        p.setProperty("version", Integer.toString(VERSION));
        p.setProperty("corner", s.corner().name());
        p.setProperty("scalePercent", Integer.toString(s.scalePercent()));
        p.setProperty("opacityPercent", Integer.toString(s.opacityPercent()));
        p.setProperty("lockPosition", Boolean.toString(s.lockPosition()));
        p.setProperty("fadeOnHover", Boolean.toString(s.fadeOnHover()));
        p.setProperty("alwaysShowChests", Boolean.toString(s.alwaysShowChests()));
        p.setProperty("gameMode", Boolean.toString(s.gameMode()));
        p.setProperty("autoGameMode", Boolean.toString(s.autoGameMode()));
        p.setProperty("launchAtLogin", Boolean.toString(s.launchAtLogin()));
        p.setProperty("showTaskbarIcon", Boolean.toString(s.showTaskbarIcon()));
        p.setProperty("language", s.language().name());
        p.setProperty("nickname", s.nickname());
        p.setProperty("monitor", Integer.toString(s.monitor()));
        putShortcut(p, "hidePetKey", s.hidePetKey());
        putShortcut(p, "switchScreenKey", s.switchScreenKey());
        putShortcut(p, "gameModeKey", s.gameModeKey());
        s.placement().ifPresent(place -> {
            putAxis(p, "placeX", place.x());
            putAxis(p, "placeY", place.y());
        });
        AtomicFiles.writeProperties(file, p, "WWTSR settings");
    }

    /** A shortcut is two numbers: its key under {@code key}, what is held with it under {@code key}Modifiers. */
    private static Shortcut shortcut(Properties p, String key, Shortcut fallback) {
        if (p.getProperty(key) == null) {
            return fallback;
        }
        return new Shortcut(number(p, key + "Modifiers", 0), number(p, key, 0));
    }

    private static void putShortcut(Properties p, String key, Shortcut shortcut) {
        p.setProperty(key, Integer.toString(shortcut.key()));
        p.setProperty(key + "Modifiers", Integer.toString(shortcut.modifiers()));
    }

    /** The dragged place; empty unless both directions are there and readable. */
    private static Optional<Placement> placement(Properties p) {
        Optional<Placement.Axis> x = axis(p, "placeX");
        Optional<Placement.Axis> y = axis(p, "placeY");
        return x.isPresent() && y.isPresent() ? Optional.of(new Placement(x.get(), y.get())) : Optional.empty();
    }

    private static Optional<Placement.Axis> axis(Properties p, String key) {
        OptionalDouble ratio = decimal(p, key);
        if (ratio.isEmpty() || !Double.isFinite(ratio.getAsDouble())) {
            return Optional.empty();
        }
        return Optional.of(new Placement.Axis(ratio.getAsDouble(),
                flag(p, key + "MinEdge", false), flag(p, key + "MaxEdge", false)));
    }

    private static void putAxis(Properties p, String key, Placement.Axis axis) {
        p.setProperty(key, Double.toString(axis.ratio()));
        p.setProperty(key + "MinEdge", Boolean.toString(axis.minEdge()));
        p.setProperty(key + "MaxEdge", Boolean.toString(axis.maxEdge()));
    }

    private static int number(Properties p, String key, int fallback) {
        String text = p.getProperty(key);
        if (text == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean flag(Properties p, String key, boolean fallback) {
        String text = p.getProperty(key);
        if (text == null) {
            return fallback;
        }
        return switch (text.trim().toLowerCase()) {
            case "true" -> true;
            case "false" -> false;
            default -> fallback;
        };
    }

    private static <E extends Enum<E>> E choice(Properties p, String key, Class<E> type, E fallback) {
        String text = p.getProperty(key);
        if (text == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, text.trim());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static OptionalDouble decimal(Properties p, String key) {
        String text = p.getProperty(key);
        if (text == null) {
            return OptionalDouble.empty();
        }
        try {
            return OptionalDouble.of(Double.parseDouble(text.trim()));
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }
}
