package com.wwtsr.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A shortcut: one key, with or without Ctrl, Alt, Shift and Win held. Immutable; anything
 * that can't be a shortcut becomes {@link #NONE}.
 *
 * <p>A function key can be a shortcut alone. Any other key needs Ctrl, Alt or Win with it
 * (Shift alone isn't enough), so ordinary typing never sets one off.
 *
 * @param modifiers which of {@link #ALT}, {@link #CTRL}, {@link #SHIFT} and {@link #WIN} are held, added together
 * @param key       the key, as a Windows virtual-key code; 0 for no shortcut
 */
public record Shortcut(int modifiers, int key) {

    public static final int ALT = 1;
    public static final int CTRL = 2;
    public static final int SHIFT = 4;
    public static final int WIN = 8;

    private static final int ALL_MODIFIERS = ALT | CTRL | SHIFT | WIN;
    private static final int F1 = 0x70;
    private static final int F24 = 0x87;

    /** The keys that aren't letters, digits or function keys, and what each is called. */
    private static final Map<Integer, String> NAMED = Map.ofEntries(
            Map.entry(32, "Space"), Map.entry(33, "PageUp"), Map.entry(34, "PageDown"),
            Map.entry(35, "End"), Map.entry(36, "Home"),
            Map.entry(37, "←"), Map.entry(38, "↑"), Map.entry(39, "→"), Map.entry(40, "↓"),
            Map.entry(45, "Insert"), Map.entry(46, "Delete"),
            Map.entry(186, ";"), Map.entry(187, "="), Map.entry(188, ","), Map.entry(189, "-"),
            Map.entry(190, "."), Map.entry(191, "/"), Map.entry(192, "`"),
            Map.entry(219, "["), Map.entry(220, "\\"), Map.entry(221, "]"), Map.entry(222, "'"));

    /** No shortcut. Declared after the key table, which making a shortcut reads. */
    public static final Shortcut NONE = new Shortcut(0, 0);

    public Shortcut {
        modifiers &= ALL_MODIFIERS;
        boolean usable = isShortcutKey(key) && (isFunctionKey(key) || (modifiers & (ALT | CTRL | WIN)) != 0);
        if (!usable) {
            modifiers = 0;
            key = 0;
        }
    }

    /** A key alone, as a function key can be. */
    public static Shortcut ofKey(int key) {
        return new Shortcut(0, key);
    }

    public boolean isSet() {
        return key != 0;
    }

    /** Whether that key can be the key of a shortcut: a letter, a digit, F1–F24 or a named key. */
    public static boolean isShortcutKey(int key) {
        return (key >= 'A' && key <= 'Z') || (key >= '0' && key <= '9') || isFunctionKey(key)
                || NAMED.containsKey(key);
    }

    /** What to call it: "Ctrl+Alt+K", "F9". Empty for {@link #NONE}. */
    public String label() {
        if (!isSet()) {
            return "";
        }
        List<String> parts = new ArrayList<>(modifierNames(modifiers));
        parts.add(keyName(key));
        return String.join("+", parts);
    }

    /** What to call a key that {@link #isShortcutKey(int) can be a shortcut's}: "F9", "K", "←". */
    public static String keyName(int key) {
        return isFunctionKey(key) ? "F" + (key - F1 + 1) : NAMED.getOrDefault(key, String.valueOf((char) key));
    }

    /** The names of the held keys in {@code modifiers}, in the order they are always written. */
    public static List<String> modifierNames(int modifiers) {
        List<String> names = new ArrayList<>();
        if ((modifiers & CTRL) != 0) {
            names.add("Ctrl");
        }
        if ((modifiers & ALT) != 0) {
            names.add("Alt");
        }
        if ((modifiers & SHIFT) != 0) {
            names.add("Shift");
        }
        if ((modifiers & WIN) != 0) {
            names.add("Win");
        }
        return names;
    }

    private static boolean isFunctionKey(int key) {
        return key >= F1 && key <= F24;
    }
}
