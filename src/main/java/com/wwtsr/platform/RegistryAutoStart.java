package com.wwtsr.platform;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

/**
 * Windows: a value under the current user's {@code Run} key, which Windows starts at sign-in.
 * It points at the installed app's launcher, so it is only available when the app runs from
 * one (the launcher tells us its path in the {@code jpackage.app-path} property); started from
 * a build tool there is no single program to point at.
 */
public final class RegistryAutoStart implements AutoStart {

    private static final String RUN_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String VALUE = "WWTSR";

    private final String launcher = System.getProperty("jpackage.app-path");

    @Override
    public boolean available() {
        return launcher != null && !launcher.isBlank();
    }

    @Override
    public boolean enabled() {
        try {
            return Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, RUN_KEY, VALUE);
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    @Override
    public boolean set(boolean on) {
        if (!available()) {
            return false;
        }
        try {
            if (on) {
                Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, RUN_KEY, VALUE, '"' + launcher + '"');
            } else if (enabled()) {
                Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, RUN_KEY, VALUE);
            }
            return true;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }
}
