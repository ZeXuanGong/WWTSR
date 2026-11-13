package com.wwtsr.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wwtsr.input.InputListener;
import com.wwtsr.input.InputSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WatchdogTest {

    @Test
    void aStalledUserInterfaceIsWrittenDownWithEveryThread(@TempDir Path dir) throws Exception {
        Path log = dir.resolve("watchdog.log");
        AtomicBoolean answering = new AtomicBoolean(false);
        // A user-interface thread that doesn't run what it is given, then does.
        Watchdog watchdog = new Watchdog(log, task -> {
            if (answering.get()) {
                task.run();
            }
        }, null);
        watchdog.start();
        try {
            waitFor(log, "hasn't answered");
            String text = Files.readString(log);
            assertTrue(text.contains("\"wwtsr-watchdog\""), "lists the threads");
            assertTrue(text.contains("    at "), "with what they were doing");

            answering.set(true);
            waitFor(log, "answers again");
        } finally {
            watchdog.stop();
        }
    }

    @Test
    void aDeafInputSourceIsRestartedAndAnAnsweringInterfaceIsNotReported(@TempDir Path dir) throws Exception {
        Path log = dir.resolve("watchdog.log");
        AtomicInteger restarts = new AtomicInteger();
        InputSource deaf = new InputSource() {
            @Override
            public boolean start(InputListener listener) {
                return true;
            }

            @Override
            public boolean seemsDeaf() {
                return true;
            }

            @Override
            public void restart() {
                restarts.incrementAndGet();
            }

            @Override
            public void stop() {
            }
        };
        Watchdog watchdog = new Watchdog(log, Runnable::run, deaf);
        watchdog.start();
        try {
            waitFor(log, "restarting them");
            Thread.sleep(2_500);
        } finally {
            watchdog.stop();
        }
        assertTrue(restarts.get() == 1, "not restarted over and over");
        assertFalse(Files.readString(log).contains("hasn't answered"));
    }

    @Test
    void withoutReportingItStillRestartsADeafInputSourceButWritesNothing(@TempDir Path dir) throws Exception {
        Path log = dir.resolve("watchdog.log");
        AtomicInteger restarts = new AtomicInteger();
        InputSource deaf = new InputSource() {
            @Override
            public boolean start(InputListener listener) {
                return true;
            }

            @Override
            public boolean seemsDeaf() {
                return true;
            }

            @Override
            public void restart() {
                restarts.incrementAndGet();
            }

            @Override
            public void stop() {
            }
        };
        // A user interface that never answers: not this watchdog's business.
        Watchdog watchdog = new Watchdog(log, task -> { }, deaf, false);
        watchdog.start();
        try {
            long end = System.nanoTime() + 15_000_000_000L;
            while (restarts.get() == 0 && System.nanoTime() < end) {
                Thread.sleep(100);
            }
            Thread.sleep(4_000);
        } finally {
            watchdog.stop();
        }
        assertTrue(restarts.get() == 1);
        assertFalse(Files.exists(log), "no log in a release");
    }

    private static void waitFor(Path log, String text) throws Exception {
        long end = System.nanoTime() + 15_000_000_000L;
        while (System.nanoTime() < end) {
            if (Files.exists(log) && Files.readString(log).contains(text)) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("the log never said: " + text);
    }
}
