package com.wwtsr.app;

import java.nio.file.Path;

/** Where the app keeps its files on this computer. */
final class AppPaths {

    private static final String FOLDER = "WWTSR";

    private AppPaths() {
    }

    /**
     * The app's data folder: {@code %APPDATA%\WWTSR} on Windows, {@code ~/.wwtsr} elsewhere.
     * The system property {@code wwtsr.dataDir} overrides it (used when trying things out).
     */
    static Path dataDir() {
        String override = System.getProperty("wwtsr.dataDir");
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, FOLDER);
        }
        return Path.of(System.getProperty("user.home", "."), "." + FOLDER.toLowerCase());
    }

    static Path settingsFile() {
        return dataDir().resolve("settings.properties");
    }

    static Path playerFile() {
        return dataDir().resolve("player.properties");
    }

    /** Where chests were kept before there was a player file. */
    static Path legacyChestsFile() {
        return dataDir().resolve("chests.properties");
    }

    static Path lockFile() {
        return dataDir().resolve("running.lock");
    }

    static Path watchdogLog() {
        return dataDir().resolve("watchdog.log");
    }

    static Path tapCountsFile() {
        return dataDir().resolve("taps.properties");
    }
}
