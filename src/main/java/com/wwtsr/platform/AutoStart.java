package com.wwtsr.platform;

/** Starting the app when the person signs in to the computer. */
public interface AutoStart {

    /** Whether it can be switched on here (it needs an installed app to point at). */
    boolean available();

    boolean enabled();

    /** Switches it on or off. Returns whether that worked. */
    boolean set(boolean on);

    /** The right implementation for the system the app is running on. */
    static AutoStart forThisSystem() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("windows") ? new RegistryAutoStart() : new NoAutoStart();
    }
}
