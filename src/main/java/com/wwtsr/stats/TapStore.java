package com.wwtsr.stats;

import java.io.IOException;

/** Where the counts are kept between runs. */
public interface TapStore {

    /** The saved counts, or {@link TapCounts#ZERO} when nothing usable is saved. Never throws. */
    TapCounts load();

    void save(TapCounts counts) throws IOException;
}
