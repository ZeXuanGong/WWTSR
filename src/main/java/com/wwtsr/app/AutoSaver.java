package com.wwtsr.app;

import com.wwtsr.stats.TapCounts;
import com.wwtsr.stats.TapStatistics;
import com.wwtsr.stats.TapStore;

/**
 * Saves the counts every so often and once more at exit. The statistics are only touched by
 * the caller's thread; the file is written through the {@link SaveQueue}.
 */
final class AutoSaver {

    private static final long INTERVAL_MILLIS = 30_000;

    private final TapStatistics statistics;
    private final TapStore store;
    private final SaveQueue queue;
    private long nextSaveAtMillis;

    AutoSaver(TapStatistics statistics, TapStore store, SaveQueue queue, long nowMillis) {
        this.statistics = statistics;
        this.store = store;
        this.queue = queue;
        this.nextSaveAtMillis = nowMillis + INTERVAL_MILLIS;
    }

    /** Call regularly: saves when the interval has passed and something changed. */
    void tick(long nowMillis) {
        if (nowMillis < nextSaveAtMillis) {
            return;
        }
        nextSaveAtMillis = nowMillis + INTERVAL_MILLIS;
        if (statistics.hasUnsaved()) {
            saveNow();
        }
    }

    /** Queues a save of the counts as they are now. */
    void saveNow() {
        TapCounts snapshot = statistics.counts();
        statistics.markSaved();
        queue.submit("the tap counts", () -> store.save(snapshot));
    }
}
