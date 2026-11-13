package com.wwtsr.stats;

import com.wwtsr.io.AtomicFiles;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * Keeps the counts in a small text file:
 *
 * <pre>
 * version=1
 * keyPresses=123
 * mouseClicks=45
 * </pre>
 *
 * A missing, damaged or newer-format file reads as zero rather than stopping the app.
 */
public final class FileTapStore implements TapStore {

    static final int VERSION = 1;
    private static final String KEY_VERSION = "version";
    private static final String KEY_KEYS = "keyPresses";
    private static final String KEY_MOUSE = "mouseClicks";

    private final Path file;

    public FileTapStore(Path file) {
        this.file = Objects.requireNonNull(file);
    }

    @Override
    public TapCounts load() {
        Properties p = AtomicFiles.readProperties(file).orElse(null);
        if (p == null || count(p.getProperty(KEY_VERSION)) != VERSION) {
            return TapCounts.ZERO;
        }
        return new TapCounts(count(p.getProperty(KEY_KEYS)), count(p.getProperty(KEY_MOUSE)));
    }

    @Override
    public void save(TapCounts counts) throws IOException {
        Properties p = new Properties();
        p.setProperty(KEY_VERSION, Integer.toString(VERSION));
        p.setProperty(KEY_KEYS, Long.toString(counts.keyPresses()));
        p.setProperty(KEY_MOUSE, Long.toString(counts.mouseClicks()));
        AtomicFiles.writeProperties(file, p, "WWTSR tap counts");
    }

    /** A saved number, or 0 when it's missing, not a number or negative. */
    private static long count(String text) {
        if (text == null) {
            return 0;
        }
        try {
            return Math.max(0, Long.parseLong(text.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
