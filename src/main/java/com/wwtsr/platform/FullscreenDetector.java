package com.wwtsr.platform;

/** Tells whether the program in front of everything else fills a whole screen (a game, a film). */
public interface FullscreenDetector {

    boolean foregroundIsFullscreen();

    /** The right implementation for the system the app is running on. */
    static FullscreenDetector forThisSystem() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("windows") ? new Win32FullscreenDetector() : () -> false;
    }
}
