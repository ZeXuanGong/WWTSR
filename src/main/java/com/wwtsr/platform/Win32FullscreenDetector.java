package com.wwtsr.platform;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HMONITOR;
import com.sun.jna.platform.win32.WinUser.MONITORINFO;
import com.sun.jna.ptr.IntByReference;
import java.util.Set;

/**
 * Windows: the foreground window is full-screen when it covers its whole monitor, isn't the
 * desktop itself and isn't one of this app's own windows.
 */
public final class Win32FullscreenDetector implements FullscreenDetector {

    /** The desktop and its wallpaper layers cover the screen too, but nobody is "in" them. */
    private static final Set<String> DESKTOP_CLASSES = Set.of("Progman", "WorkerW", "Shell_TrayWnd");
    private static final int CLASS_CHARS = 64;

    @Override
    public boolean foregroundIsFullscreen() {
        try {
            HWND window = User32.INSTANCE.GetForegroundWindow();
            if (window == null) {
                return false;
            }
            IntByReference process = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(window, process);
            if (process.getValue() == Kernel32.INSTANCE.GetCurrentProcessId()) {
                return false;
            }
            char[] name = new char[CLASS_CHARS];
            int length = User32.INSTANCE.GetClassName(window, name, CLASS_CHARS);
            if (length > 0 && DESKTOP_CLASSES.contains(new String(name, 0, Math.min(length, CLASS_CHARS)))) {
                return false;
            }
            RECT rect = new RECT();
            if (!User32.INSTANCE.GetWindowRect(window, rect)) {
                return false;
            }
            HMONITOR monitor = User32.INSTANCE.MonitorFromWindow(window, WinUser.MONITOR_DEFAULTTONEAREST);
            MONITORINFO info = new MONITORINFO();
            if (monitor == null || !User32.INSTANCE.GetMonitorInfo(monitor, info).booleanValue()) {
                return false;
            }
            RECT screen = info.rcMonitor;
            return rect.left <= screen.left && rect.top <= screen.top
                    && rect.right >= screen.right && rect.bottom >= screen.bottom;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }
}
