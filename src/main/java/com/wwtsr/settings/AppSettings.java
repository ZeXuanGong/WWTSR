package com.wwtsr.settings;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * The settings in use right now. Everything that depends on a setting (the window, the menus,
 * the file on disk) listens here, so a change is made in one place and reaches them all.
 * Use from one thread (the JavaFX thread in the app).
 */
public final class AppSettings {

    private final List<Consumer<Settings>> listeners = new CopyOnWriteArrayList<>();
    private Settings current;

    public AppSettings(Settings start) {
        this.current = Objects.requireNonNull(start);
    }

    public Settings get() {
        return current;
    }

    public void addListener(Consumer<Settings> listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    /** Applies {@code change}; listeners hear about it only when something really changed. */
    public void update(UnaryOperator<Settings> change) {
        Settings next = Objects.requireNonNull(change.apply(current));
        if (next.equals(current)) {
            return;
        }
        current = next;
        for (Consumer<Settings> listener : listeners) {
            listener.accept(next);
        }
    }
}
