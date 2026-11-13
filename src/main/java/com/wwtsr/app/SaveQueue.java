package com.wwtsr.app;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Writes files one after another on a background thread, so saving never makes the pet
 * stutter and a later save can't be overtaken by an earlier one.
 */
final class SaveQueue {

    /** A save that may fail. */
    @FunctionalInterface
    interface Save {
        void run() throws IOException;
    }

    private static final long CLOSE_WAIT_SECONDS = 3;

    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "wwtsr-save");
        t.setDaemon(true);
        return t;
    });

    /** Queues a save. A failure is reported and doesn't stop the app. */
    void submit(String what, Save save) {
        try {
            writer.execute(() -> runNow(what, save));
        } catch (RejectedExecutionException e) {
            // Already closing: do it here rather than lose it.
            runNow(what, save);
        }
    }

    /** Waits a moment for queued saves to finish, then takes no more. */
    void close() {
        writer.shutdown();
        try {
            writer.awaitTermination(CLOSE_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void runNow(String what, Save save) {
        try {
            save.run();
        } catch (IOException | RuntimeException e) {
            System.err.println("WWTSR couldn't save " + what + ": " + e.getMessage());
        }
    }
}
