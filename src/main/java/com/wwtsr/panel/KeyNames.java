package com.wwtsr.panel;

import com.wwtsr.input.Shortcut;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Turns the keys pressed in the panel into {@link Shortcut}s. Shortcuts are kept as Windows
 * virtual-key codes; letters, digits and function keys have the same number in JavaFX, the
 * other keys are looked up here.
 */
public final class KeyNames {

    private static final Map<KeyCode, Integer> NAMED = Map.ofEntries(
            Map.entry(KeyCode.SPACE, 32), Map.entry(KeyCode.PAGE_UP, 33), Map.entry(KeyCode.PAGE_DOWN, 34),
            Map.entry(KeyCode.END, 35), Map.entry(KeyCode.HOME, 36),
            Map.entry(KeyCode.LEFT, 37), Map.entry(KeyCode.UP, 38), Map.entry(KeyCode.RIGHT, 39),
            Map.entry(KeyCode.DOWN, 40), Map.entry(KeyCode.INSERT, 45), Map.entry(KeyCode.DELETE, 46),
            Map.entry(KeyCode.SEMICOLON, 186), Map.entry(KeyCode.EQUALS, 187), Map.entry(KeyCode.COMMA, 188),
            Map.entry(KeyCode.MINUS, 189), Map.entry(KeyCode.PERIOD, 190), Map.entry(KeyCode.SLASH, 191),
            Map.entry(KeyCode.BACK_QUOTE, 192), Map.entry(KeyCode.OPEN_BRACKET, 219),
            Map.entry(KeyCode.BACK_SLASH, 220), Map.entry(KeyCode.CLOSE_BRACKET, 221), Map.entry(KeyCode.QUOTE, 222));

    private KeyNames() {
    }

    /** Whether the key is Ctrl, Alt, Shift or Win: held with a shortcut's key, never the key itself. */
    public static boolean isModifier(KeyCode key) {
        return key != null && key.isModifierKey();
    }

    /** The shortcut that key press makes, or {@link Shortcut#NONE} when it can't be one. */
    public static Shortcut shortcutFor(KeyEvent e) {
        return new Shortcut(modifiers(e), codeFor(e.getCode()));
    }

    /** What is being pressed, to show while a shortcut is chosen: "Ctrl+K", or just "Ctrl". */
    public static String pressedLabel(KeyEvent e) {
        List<String> parts = new ArrayList<>(Shortcut.modifierNames(modifiers(e)));
        KeyCode key = e.getCode();
        if (!isModifier(key)) {
            int code = codeFor(key);
            parts.add(code != 0 ? Shortcut.keyName(code) : key.getName());
        }
        return String.join("+", parts);
    }

    /** The code to save for a key, or 0 when that key can't be a shortcut's. */
    static int codeFor(KeyCode key) {
        if (key == null) {
            return 0;
        }
        if (key.isLetterKey() || key.isDigitKey() || key.isFunctionKey()) {
            return Shortcut.isShortcutKey(key.getCode()) ? key.getCode() : 0;
        }
        return NAMED.getOrDefault(key, 0);
    }

    private static int modifiers(KeyEvent e) {
        return (e.isAltDown() ? Shortcut.ALT : 0) | (e.isControlDown() ? Shortcut.CTRL : 0)
                | (e.isShiftDown() ? Shortcut.SHIFT : 0) | (e.isMetaDown() ? Shortcut.WIN : 0);
    }
}
