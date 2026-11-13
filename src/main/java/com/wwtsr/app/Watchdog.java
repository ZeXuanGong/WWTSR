package com.wwtsr.app;

import com.wwtsr.input.InputSource;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Watches the app from a thread of its own and writes down what it sees go wrong, in
 * {@code watchdog.log} in the data folder:
 *
 * <ul>
 *   <li>the user-interface thread not answering for a few seconds: what every thread was doing
 *       at that moment, and later how long the stall lasted;
 *   <li>the input source going deaf: noted, and the source restarted.
 * </ul>
 *
 * It only ever reads state and appends to its log, so it can't make things worse.
 */
final class Watchdog {

    private static final long CHECK_MILLIS = 1_000;
    /** The user-interface thread is stalled when it hasn't answered for this long. */
    private static final long STALL_MILLIS = 3_000;
    /** The input source is restarted at most this often. */
    private static final long RESTART_GAP_MILLIS = 30_000;
    /** The log is started afresh once it is this big. */
    private static final long LOG_LIMIT_BYTES = 2_000_000;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final boolean reporting;
    private final Path log;
    private final Consumer<Runnable> onUiThread;
    private final InputSource input;
    private final Thread thread;
    private volatile long lastAnswerNanos = System.nanoTime();
    private volatile boolean stopped;

    /**
     * @param onUiThread runs something on the user-interface thread, later
     * @param input      the input source to keep an eye on, or null
     */
    Watchdog(Path log, Consumer<Runnable> onUiThread, InputSource input) {
        this(log, onUiThread, input, true);
    }

    /**
     * @param reporting whether to watch the user-interface thread and keep the log; without
     *                  it the watchdog only restarts an input source that went deaf
     */
    Watchdog(Path log, Consumer<Runnable> onUiThread, InputSource input, boolean reporting) {
        this.reporting = reporting;
        this.log = log;
        this.onUiThread = onUiThread;
        this.input = input;
        thread = new Thread(this::run, "wwtsr-watchdog");
        thread.setDaemon(true);
    }

    void start() {
        write("started; Java " + Runtime.version() + ", " + System.getProperty("os.name"));
        thread.start();
    }

    void stop() {
        stopped = true;
        thread.interrupt();
    }

    private void run() {
        boolean stalled = false;
        long lastRestartNanos = System.nanoTime() - RESTART_GAP_MILLIS * 1_000_000;
        long lastCheckNanos = System.nanoTime();
        while (!stopped) {
            try {
                Thread.sleep(CHECK_MILLIS);
            } catch (InterruptedException e) {
                return;
            }
            long now = System.nanoTime();
            // A long gap between checks means the whole computer slept (or this process was
            // held up as a whole), not that one thread stalled: start counting again.
            boolean slept = (now - lastCheckNanos) / 1_000_000 > CHECK_MILLIS + STALL_MILLIS;
            lastCheckNanos = now;
            if (slept) {
                write("the process didn't run for " + (now - lastAnswerNanos) / 1_000_000 + " ms (sleep or suspend)");
                lastAnswerNanos = now;
            }
            if (reporting) {
                long silent = (now - lastAnswerNanos) / 1_000_000;
                if (!stalled && silent > STALL_MILLIS) {
                    stalled = true;
                    write("the user interface hasn't answered for " + silent + " ms\n" + allThreads());
                } else if (stalled && silent <= CHECK_MILLIS * 2) {
                    stalled = false;
                    write("the user interface answers again");
                }
                try {
                    onUiThread.accept(() -> lastAnswerNanos = System.nanoTime());
                } catch (RuntimeException e) {
                    // The toolkit is shutting down.
                    return;
                }
            }
            if (input != null && input.seemsDeaf()
                    && (now - lastRestartNanos) / 1_000_000 >= RESTART_GAP_MILLIS) {
                lastRestartNanos = now;
                write("the input hooks heard nothing while the computer had input: restarting them");
                input.restart();
            }
        }
    }

    /** What every thread is doing, with the locks it holds and waits for. */
    private static String allThreads() {
        StringBuilder text = new StringBuilder();
        try {
            for (ThreadInfo info : ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)) {
                text.append('"').append(info.getThreadName()).append("\" ").append(info.getThreadState());
                if (info.getLockName() != null) {
                    text.append(" waiting for ").append(info.getLockName());
                    if (info.getLockOwnerName() != null) {
                        text.append(" held by \"").append(info.getLockOwnerName()).append('"');
                    }
                }
                text.append('\n');
                for (StackTraceElement frame : info.getStackTrace()) {
                    text.append("    at ").append(frame).append('\n');
                }
            }
            Runtime runtime = Runtime.getRuntime();
            text.append("memory: ").append((runtime.totalMemory() - runtime.freeMemory()) / 1_000_000)
                    .append(" MB used of ").append(runtime.maxMemory() / 1_000_000).append(" MB\n");
        } catch (RuntimeException e) {
            text.append("(couldn't list the threads: ").append(e).append(")\n");
        }
        return text.toString();
    }

    private void write(String what) {
        if (!reporting) {
            return;
        }
        try {
            Files.createDirectories(log.getParent());
            if (Files.exists(log) && Files.size(log) > LOG_LIMIT_BYTES) {
                Files.delete(log);
            }
            Files.writeString(log, LocalDateTime.now().format(TIME) + "  " + what + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException e) {
            // Nothing to be done: the log is only a help.
        }
    }
}
