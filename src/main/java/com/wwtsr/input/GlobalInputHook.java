package com.wwtsr.input;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HMODULE;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.KBDLLHOOKSTRUCT;
import com.sun.jna.platform.win32.WinUser.LowLevelKeyboardProc;
import com.sun.jna.platform.win32.WinUser.LowLevelMouseProc;
import com.sun.jna.platform.win32.WinUser.MSG;
import com.sun.jna.platform.win32.WinUser.MSLLHOOKSTRUCT;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Hears key and mouse-button presses anywhere on Windows through low-level hooks. The hooks
 * live on their own thread, which runs the message loop Windows needs to deliver them.
 *
 * <p>Only the fact of a press, and of its letting go, is passed on. Key codes are used for a
 * moment to ignore the repeats of a held key and to recognise the app's own shortcuts; they are never stored,
 * logged or handed to listeners. No key is swallowed: every press still reaches the program
 * being used.
 *
 * <p>Windows doesn't show a normal program the input going to windows that run as
 * administrator, so presses there aren't heard unless this app is run as administrator too.
 */
public final class GlobalInputHook implements InputSource {

    private static final int WM_QUIT = 0x0012;
    private static final int WM_KEYDOWN = 0x0100;
    private static final int WM_KEYUP = 0x0101;
    private static final int WM_SYSKEYDOWN = 0x0104;
    private static final int WM_SYSKEYUP = 0x0105;
    private static final int WM_LBUTTONDOWN = 0x0201;
    private static final int WM_RBUTTONDOWN = 0x0204;
    private static final int WM_MBUTTONDOWN = 0x0207;
    private static final int WM_XBUTTONDOWN = 0x020B;
    private static final int WM_LBUTTONUP = 0x0202;
    private static final int WM_RBUTTONUP = 0x0205;
    private static final int WM_MBUTTONUP = 0x0208;
    private static final int WM_XBUTTONUP = 0x020C;
    // The hooks name the left and right key of each pair, never the pair.
    private static final int[] SHIFT_KEYS = {0x10, 0xA0, 0xA1};
    private static final int[] CTRL_KEYS = {0x11, 0xA2, 0xA3};
    private static final int[] ALT_KEYS = {0x12, 0xA4, 0xA5};
    private static final int[] WIN_KEYS = {0x5B, 0x5C};
    private static final long START_WAIT_SECONDS = 5;
    private static final long STOP_WAIT_MILLIS = 2_000;
    /** The hooks count as deaf when they are this much further behind than the computer. */
    private static final long DEAF_AFTER_MILLIS = 5_000;

    private final KeyRepeatFilter repeats = new KeyRepeatFilter();
    private volatile InputListener listener;
    /** The shortcuts: the only keys a code is ever compared against. */
    private volatile Map<Shortcut, Runnable> hotkeys = Map.of();
    private volatile Thread thread;
    private volatile int threadId;
    private volatile boolean installed;
    /** When either hook was last called, whatever for. */
    private volatile long lastHeardNanos = System.nanoTime();
    // Kept in fields so they aren't collected while Windows still calls them.
    private LowLevelKeyboardProc keyboardProc;
    private LowLevelMouseProc mouseProc;

    @Override
    public synchronized boolean start(InputListener inputListener) {
        if (thread != null) {
            return installed;
        }
        listener = inputListener;
        CountDownLatch ready = new CountDownLatch(1);
        Thread t = new Thread(() -> run(ready), "wwtsr-input-hook");
        t.setDaemon(true);
        thread = t;
        t.start();
        try {
            if (!ready.await(START_WAIT_SECONDS, TimeUnit.SECONDS)) {
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return installed;
    }

    @Override
    public synchronized void stop() {
        Thread t = thread;
        if (t == null) {
            return;
        }
        if (threadId != 0) {
            User32.INSTANCE.PostThreadMessage(threadId, WM_QUIT, new WPARAM(0), new LPARAM(0));
        }
        try {
            t.join(STOP_WAIT_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        thread = null;
        installed = false;
    }

    /**
     * Windows takes a low-level hook away, without saying so, from a program that was once too
     * slow to answer it. That shows as the computer having had input more recently than the
     * hooks heard any.
     */
    @Override
    public boolean seemsDeaf() {
        if (!installed) {
            return false;
        }
        try {
            WinUser.LASTINPUTINFO last = new WinUser.LASTINPUTINFO();
            if (!User32.INSTANCE.GetLastInputInfo(last)) {
                return false;
            }
            // Both are tick counts that wrap; their difference as an int is still right.
            long systemIdle = Integer.toUnsignedLong(Kernel32.INSTANCE.GetTickCount() - last.dwTime);
            long hookIdle = (System.nanoTime() - lastHeardNanos) / 1_000_000;
            return hookIdle > systemIdle + DEAF_AFTER_MILLIS;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    @Override
    public synchronized void restart() {
        InputListener l = listener;
        if (l == null) {
            return;
        }
        stop();
        start(l);
    }

    private void run(CountDownLatch ready) {
        HHOOK keyboardHook = null;
        HHOOK mouseHook = null;
        try {
            threadId = Kernel32.INSTANCE.GetCurrentThreadId();
            HMODULE module = Kernel32.INSTANCE.GetModuleHandle(null);
            keyboardProc = this::onKeyboard;
            mouseProc = this::onMouse;
            keyboardHook = User32.INSTANCE.SetWindowsHookEx(WinUser.WH_KEYBOARD_LL, keyboardProc, module, 0);
            mouseHook = User32.INSTANCE.SetWindowsHookEx(WinUser.WH_MOUSE_LL, mouseProc, module, 0);
            lastHeardNanos = System.nanoTime();
            installed = keyboardHook != null && mouseHook != null;
            ready.countDown();
            if (!installed) {
                return;
            }
            MSG msg = new MSG();
            while (User32.INSTANCE.GetMessage(msg, null, 0, 0) > 0) {
                User32.INSTANCE.TranslateMessage(msg);
                User32.INSTANCE.DispatchMessage(msg);
            }
        } catch (RuntimeException | LinkageError e) {
            // The native library couldn't be used here: report "can't listen", don't crash.
            installed = false;
        } finally {
            if (keyboardHook != null) {
                User32.INSTANCE.UnhookWindowsHookEx(keyboardHook);
            }
            if (mouseHook != null) {
                User32.INSTANCE.UnhookWindowsHookEx(mouseHook);
            }
            ready.countDown();
        }
    }

    private LRESULT onKeyboard(int code, WPARAM wParam, KBDLLHOOKSTRUCT info) {
        lastHeardNanos = System.nanoTime();
        if (code >= 0) {
            int message = wParam.intValue();
            if (message == WM_KEYDOWN || message == WM_SYSKEYDOWN) {
                if (repeats.press(info.vkCode)) {
                    report(InputKind.KEY);
                    runHotkey(info.vkCode);
                }
            } else if (message == WM_KEYUP || message == WM_SYSKEYUP) {
                if (repeats.release(info.vkCode)) {
                    reportRelease(InputKind.KEY);
                }
            }
        }
        return User32.INSTANCE.CallNextHookEx(null, code, wParam, new LPARAM(Pointer.nativeValue(info.getPointer())));
    }

    private LRESULT onMouse(int code, WPARAM wParam, MSLLHOOKSTRUCT info) {
        lastHeardNanos = System.nanoTime();
        if (code >= 0) {
            int message = wParam.intValue();
            if (message == WM_LBUTTONDOWN || message == WM_RBUTTONDOWN
                    || message == WM_MBUTTONDOWN || message == WM_XBUTTONDOWN) {
                report(InputKind.MOUSE_BUTTON);
            } else if (message == WM_LBUTTONUP || message == WM_RBUTTONUP
                    || message == WM_MBUTTONUP || message == WM_XBUTTONUP) {
                reportRelease(InputKind.MOUSE_BUTTON);
            }
        }
        return User32.INSTANCE.CallNextHookEx(null, code, wParam, new LPARAM(Pointer.nativeValue(info.getPointer())));
    }

    @Override
    public void setHotkeys(Map<Shortcut, Runnable> actions) {
        hotkeys = Map.copyOf(actions);
    }

    private void runHotkey(int keyCode) {
        Map<Shortcut, Runnable> current = hotkeys;
        if (current.isEmpty() || !Shortcut.isShortcutKey(keyCode)) {
            return;
        }
        Runnable action = current.get(new Shortcut(heldModifiers(), keyCode));
        if (action == null) {
            return;
        }
        try {
            action.run();
        } catch (RuntimeException e) {
            // Ignored, like a listener's mistake below.
        }
    }

    /** Which of Ctrl, Alt, Shift and Win are held right now. */
    private int heldModifiers() {
        return (anyDown(ALT_KEYS) ? Shortcut.ALT : 0) | (anyDown(CTRL_KEYS) ? Shortcut.CTRL : 0)
                | (anyDown(SHIFT_KEYS) ? Shortcut.SHIFT : 0) | (anyDown(WIN_KEYS) ? Shortcut.WIN : 0);
    }

    private boolean anyDown(int[] keys) {
        for (int key : keys) {
            if (repeats.isDown(key)) {
                return true;
            }
        }
        return false;
    }

    private void reportRelease(InputKind kind) {
        InputListener l = listener;
        if (l == null) {
            return;
        }
        try {
            l.released(kind);
        } catch (RuntimeException e) {
            // Ignored: input must keep flowing for the rest of the system.
        }
    }

    /** A listener's mistake must never reach Windows' hook chain. */
    private void report(InputKind kind) {
        InputListener l = listener;
        if (l == null) {
            return;
        }
        try {
            l.pressed(kind);
        } catch (RuntimeException e) {
            // Ignored: input must keep flowing for the rest of the system.
        }
    }
}
