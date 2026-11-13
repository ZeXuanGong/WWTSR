package com.wwtsr.input;

/** For systems where presses outside the app's own window can't be heard. */
public final class NoInputSource implements InputSource {

    @Override
    public boolean start(InputListener listener) {
        return false;
    }

    @Override
    public void stop() {
    }
}
