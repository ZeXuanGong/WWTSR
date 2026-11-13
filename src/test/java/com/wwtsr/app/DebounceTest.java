package com.wwtsr.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DebounceTest {

    @Test
    void aSecondGoTooSoonIsRefused() {
        Debounce gap = new Debounce(280);
        assertTrue(gap.pass(0), "the first always passes, whatever the clock says");
        assertFalse(gap.pass(100));
        assertFalse(gap.pass(279));
        assertTrue(gap.pass(280));
        assertFalse(gap.pass(500), "counted from the last one that passed");
        assertTrue(gap.pass(560));
    }

    @Test
    void aNegativeGapIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Debounce(-1));
    }
}
