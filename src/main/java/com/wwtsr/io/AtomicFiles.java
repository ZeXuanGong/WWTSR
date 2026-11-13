package com.wwtsr.io;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Properties;

/** Reading and writing the app's small text files safely. */
public final class AtomicFiles {

    private AtomicFiles() {
    }

    /**
     * Writes {@code properties} to {@code file} by writing a temporary file beside it and then
     * swapping it in, so a crash or power cut mid-save leaves the previous file intact.
     */
    public static void writeProperties(Path file, Properties properties, String comment) throws IOException {
        Path dir = file.toAbsolutePath().getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }
        Path temp = Files.createTempFile(dir, "save", ".tmp");
        try {
            try (Writer out = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                properties.store(out, comment);
            }
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /** The properties in {@code file}, or empty when it's missing or can't be read. Never throws. */
    public static Optional<Properties> readProperties(Path file) {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        Properties p = new Properties();
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(in);
            return Optional.of(p);
        } catch (IOException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
