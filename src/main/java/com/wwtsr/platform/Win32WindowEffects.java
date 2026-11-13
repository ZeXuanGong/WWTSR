package com.wwtsr.platform;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;

/** Windows: changes the window's extended styles. */
public final class Win32WindowEffects implements WindowEffects {

    private static final int GWL_EXSTYLE = -20;
    private static final int WS_EX_TRANSPARENT = 0x0000_0020;
    private static final int WS_EX_TOOLWINDOW = 0x0000_0080;
    private static final int WS_EX_APPWINDOW = 0x0004_0000;
    private static final int WS_EX_LAYERED = 0x0008_0000;
    private static final int SW_HIDE = 0;
    private static final int SW_SHOWNOACTIVATE = 4;
    private static final int TITLE_CHARS = 256;

    @Override
    public void apply(String windowTitle, boolean clickThrough, boolean showTaskbarIcon) {
        try {
            HWND window = findOwnWindow(windowTitle);
            if (window == null) {
                return;
            }
            int before = User32.INSTANCE.GetWindowLong(window, GWL_EXSTYLE);
            int after = before;
            if (clickThrough) {
                after |= WS_EX_TRANSPARENT | WS_EX_LAYERED;
            } else {
                after &= ~WS_EX_TRANSPARENT;
            }
            if (showTaskbarIcon) {
                after = (after & ~WS_EX_TOOLWINDOW) | WS_EX_APPWINDOW;
            } else {
                after = (after | WS_EX_TOOLWINDOW) & ~WS_EX_APPWINDOW;
            }
            if (after == before) {
                return;
            }
            // The taskbar only notices its styles changing while the window is hidden.
            boolean taskbarChanged = ((after ^ before) & (WS_EX_TOOLWINDOW | WS_EX_APPWINDOW)) != 0;
            if (taskbarChanged) {
                User32.INSTANCE.ShowWindow(window, SW_HIDE);
            }
            User32.INSTANCE.SetWindowLong(window, GWL_EXSTYLE, after);
            if (taskbarChanged) {
                User32.INSTANCE.ShowWindow(window, SW_SHOWNOACTIVATE);
            }
        } catch (RuntimeException | LinkageError e) {
            // The effect is a nicety: without it the pet still works.
        }
    }

    /**
     * This program's own top-level window with that title. Looking only at our own windows
     * matters: another program (a folder window, say) can have the same title.
     */
    private static HWND findOwnWindow(String title) {
        int ownProcess = Kernel32.INSTANCE.GetCurrentProcessId();
        HWND[] found = new HWND[1];
        User32.INSTANCE.EnumWindows((window, data) -> {
            IntByReference process = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(window, process);
            if (process.getValue() != ownProcess) {
                return true;
            }
            char[] text = new char[TITLE_CHARS];
            int length = User32.INSTANCE.GetWindowText(window, text, TITLE_CHARS);
            if (length > 0 && title.equals(new String(text, 0, Math.min(length, TITLE_CHARS)))) {
                found[0] = window;
                return false;
            }
            return true;
        }, null);
        return found[0];
    }
}
