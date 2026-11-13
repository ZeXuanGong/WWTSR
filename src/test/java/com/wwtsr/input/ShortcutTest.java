package com.wwtsr.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShortcutTest {

    @Test
    void aFunctionKeyCanStandAlone() {
        Shortcut f9 = Shortcut.ofKey(0x78);
        assertTrue(f9.isSet());
        assertEquals("F9", f9.label());
        assertEquals("F24", Shortcut.ofKey(0x87).label());
    }

    @Test
    void otherKeysNeedCtrlAltOrWin() {
        assertEquals(Shortcut.NONE, Shortcut.ofKey('K'), "a letter alone");
        assertEquals(Shortcut.NONE, new Shortcut(Shortcut.SHIFT, 'K'), "Shift alone isn't enough");
        assertTrue(new Shortcut(Shortcut.CTRL, 'K').isSet());
        assertTrue(new Shortcut(Shortcut.ALT | Shortcut.SHIFT, '5').isSet());
        assertTrue(new Shortcut(Shortcut.WIN, 32).isSet());
    }

    @Test
    void keysThatAreNotShortcutKeysGiveNone() {
        assertEquals(Shortcut.NONE, new Shortcut(Shortcut.CTRL, 0x61), "number pad 1");
        assertEquals(Shortcut.NONE, new Shortcut(Shortcut.CTRL, 0));
        assertEquals(Shortcut.NONE, new Shortcut(Shortcut.CTRL, -4));
        assertEquals(Shortcut.NONE, new Shortcut(Shortcut.CTRL, 9_999));
        assertFalse(Shortcut.NONE.isSet());
        assertEquals("", Shortcut.NONE.label());
    }

    @Test
    void labelsNameWhatIsHeldInAFixedOrder() {
        int all = Shortcut.ALT | Shortcut.CTRL | Shortcut.SHIFT | Shortcut.WIN;
        assertEquals("Ctrl+Alt+Shift+Win+K", new Shortcut(all, 'K').label());
        assertEquals("Ctrl+←", new Shortcut(Shortcut.CTRL, 37).label());
        assertEquals("Alt+Space", new Shortcut(Shortcut.ALT, 32).label());
        assertEquals("Shift+F7", new Shortcut(Shortcut.SHIFT, 0x76).label());
        assertEquals("Ctrl+;", new Shortcut(Shortcut.CTRL, 186).label());
    }

    @Test
    void unknownHeldBitsAreDropped() {
        assertEquals(new Shortcut(Shortcut.CTRL, 'A'), new Shortcut(Shortcut.CTRL | 64, 'A'));
    }

    @Test
    void everyPressIsMatchedByOneRelease() {
        KeyRepeatFilter filter = new KeyRepeatFilter();
        assertFalse(filter.release(65), "let go without having been pressed here");
        assertTrue(filter.press(65));
        assertTrue(filter.isDown(65));
        assertFalse(filter.press(65), "repeat");
        assertTrue(filter.release(65));
        assertFalse(filter.isDown(65));
        assertFalse(filter.release(65), "only once");
    }
}
