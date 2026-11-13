package com.wwtsr.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wwtsr.input.InputKind;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TapStoreTest {

    @TempDir
    Path dir;

    @Test
    void nothingSavedReadsAsZero() {
        assertEquals(TapCounts.ZERO, new FileTapStore(dir.resolve("taps.properties")).load());
    }

    @Test
    void savedCountsComeBack() throws IOException {
        Path file = dir.resolve("nested").resolve("taps.properties");
        new FileTapStore(file).save(new TapCounts(36_000, 957));
        assertEquals(new TapCounts(36_000, 957), new FileTapStore(file).load());
    }

    @Test
    void savingReplacesTheOldFileAndLeavesNoTempFiles() throws IOException {
        Path file = dir.resolve("taps.properties");
        FileTapStore store = new FileTapStore(file);
        store.save(new TapCounts(1, 2));
        store.save(new TapCounts(3, 4));
        assertEquals(new TapCounts(3, 4), store.load());
        try (Stream<Path> files = Files.list(dir)) {
            assertEquals(1, files.count(), "only the counts file remains");
        }
    }

    @Test
    void damagedFilesReadAsZeroInsteadOfFailing() throws IOException {
        Path file = dir.resolve("taps.properties");
        FileTapStore store = new FileTapStore(file);
        Files.writeString(file, "\u0000\u0001 not a properties file \\u12");
        assertEquals(TapCounts.ZERO, store.load());
        Files.writeString(file, "version=1\nkeyPresses=abc\nmouseClicks=-5\n");
        assertEquals(TapCounts.ZERO, store.load());
        Files.writeString(file, "version=1\nkeyPresses=7\n");
        assertEquals(new TapCounts(7, 0), store.load(), "a missing count is zero");
    }

    @Test
    void aNewerFormatIsNotMisread() throws IOException {
        Path file = dir.resolve("taps.properties");
        Files.writeString(file, "version=2\nkeyPresses=7\nmouseClicks=8\n");
        assertEquals(TapCounts.ZERO, new FileTapStore(file).load());
    }

    @Test
    void statisticsCountEachKindAndTrackUnsavedChanges() {
        TapStatistics stats = new TapStatistics(new TapCounts(10, 20));
        assertFalse(stats.hasUnsaved());
        stats.record(InputKind.KEY);
        stats.record(InputKind.KEY);
        stats.record(InputKind.MOUSE_BUTTON);
        assertEquals(new TapCounts(12, 21), stats.counts());
        assertEquals(33, stats.counts().total());
        assertTrue(stats.hasUnsaved());
        stats.markSaved();
        assertFalse(stats.hasUnsaved());
    }

    @Test
    void totalsDoNotOverflow() {
        assertEquals(Long.MAX_VALUE, new TapCounts(Long.MAX_VALUE, 5).total());
        TapStatistics stats = new TapStatistics(new TapCounts(Long.MAX_VALUE, 0));
        stats.record(InputKind.KEY);
        assertEquals(Long.MAX_VALUE, stats.counts().keyPresses());
        assertThrows(IllegalArgumentException.class, () -> new TapCounts(-1, 0));
    }
}
