package com.wwtsr.platform;

/**
 * Things a window can do that the UI toolkit doesn't offer and the operating system does.
 */
public interface WindowEffects {

    /**
     * Applies the effects to this program's window with the given title.
     *
     * @param clickThrough    the mouse goes through the whole window
     * @param showTaskbarIcon the window has a button on the taskbar
     */
    void apply(String windowTitle, boolean clickThrough, boolean showTaskbarIcon);

    /** The right implementation for the system the app is running on. */
    static WindowEffects forThisSystem() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("windows") ? new Win32WindowEffects() : (title, through, taskbar) -> { };
    }
}
