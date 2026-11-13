package com.wwtsr.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KeyRepeatFilterTest {

    @Test
    void aHeldKeyCountsOnce() {
        KeyRepeatFilter filter = new KeyRepeatFilter();
        assertTrue(filter.press(65));
        assertFalse(filter.press(65), "repeat while held");
        assertFalse(filter.press(65), "repeat while held");
        filter.release(65);
        assertTrue(filter.press(65), "pressed again after release");
    }

    @Test
    void keysAreTrackedSeparately() {
        KeyRepeatFilter filter = new KeyRepeatFilter();
        assertTrue(filter.press(16));
        assertTrue(filter.press(65), "another key while the first is held");
        filter.release(16);
        assertFalse(filter.press(65), "still held");
    }

    @Test
    void unknownCodesAlwaysCountAndNeverFail() {
        KeyRepeatFilter filter = new KeyRepeatFilter();
        assertTrue(filter.press(-1));
        assertTrue(filter.press(100_000));
        assertTrue(filter.press(100_000));
        filter.release(-1);
        filter.release(100_000);
    }
}
