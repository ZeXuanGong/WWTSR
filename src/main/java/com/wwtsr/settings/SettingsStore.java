package com.wwtsr.settings;

import java.io.IOException;

/** Where the settings are kept between runs. */
public interface SettingsStore {

    /** The saved settings; anything missing or unusable takes its default. Never throws. */
    Settings load();

    void save(Settings settings) throws IOException;
}
