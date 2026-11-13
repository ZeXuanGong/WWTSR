package com.wwtsr.platform;

/** For systems where starting at sign-in isn't supported. */
public final class NoAutoStart implements AutoStart {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public boolean set(boolean on) {
        return false;
    }
}
