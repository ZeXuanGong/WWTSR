package com.wwtsr.panel;

import com.wwtsr.i18n.Messages;
import com.wwtsr.input.Shortcut;
import com.wwtsr.settings.AppSettings;
import com.wwtsr.settings.Language;
import com.wwtsr.settings.Settings;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * The settings page: one row per setting, each a pale card with its name (and, for most, a
 * line of explanation) on the left and its control on the right; then a row of buttons.
 * Every control writes straight to the app's settings, which apply at once. The page shows
 * the settings as they were when it was built, and is rebuilt whenever it is opened.
 */
final class SettingsPage implements Page {

    /** The largest count shown as it is; a bigger one shows as this with a plus. */
    private static final long MAX_SHOWN_TAPS = 99_999_999_999_999L;

    /** A shortcut field and what it is for. */
    private record ShortcutRow(ShortcutField field, Function<Settings, Shortcut> get,
            BiFunction<Settings, Shortcut, Settings> set) {
    }

    private final PanelHost host;
    private final Consumer<String> toast;
    private final ScrollPane root = new ScrollPane();
    private final Label taps = new Label();
    private final Label screen = new Label();
    private final Label screenTip = new Label();
    private final java.util.List<ShortcutRow> shortcuts = new java.util.ArrayList<>();
    /** The shortcut waiting for a key, if any. */
    private ShortcutRow capturing;
    /** The shortcut pressed while one is being chosen; it is taken when the keys are let go. */
    private Shortcut pending;

    SettingsPage(PanelHost host, Consumer<String> toast) {
        this.host = host;
        this.toast = toast;
        Messages m = host.messages();
        AppSettings settings = host.settings();
        Settings s = settings.get();

        TextField nickname = new TextField(s.nickname());
        nickname.getStyleClass().add("setting-input");
        nickname.setPromptText(m.get("setting.nickname.default"));
        Runnable saveNickname = () -> settings.update(x -> x.withNickname(nickname.getText()));
        nickname.setOnAction(e -> saveNickname.run());
        nickname.focusedProperty().addListener((o, was, focused) -> {
            if (!focused) {
                saveNickname.run();
            }
        });
        taps.getStyleClass().add("setting-value");

        SwitchButton autoStart = new SwitchButton(s.launchAtLogin(), host::setLaunchAtLogin);
        autoStart.setDisable(!host.launchAtLoginAvailable());

        Button switchScreen = new Button(m.get("setting.screen.switch"));
        switchScreen.getStyleClass().add("setting-button");
        switchScreen.setOnAction(e -> {
            host.switchScreen();
            refresh();
        });
        // There is somewhere to switch to only with a second screen.
        boolean manyScreens = host.screenCount() > 1;
        switchScreen.setVisible(manyScreens);
        switchScreen.setManaged(manyScreens);
        screenTip.getStyleClass().add("setting-sub");

        Button checkVersion = new Button(m.get("setting.version.check"));
        checkVersion.getStyleClass().add("setting-button");
        checkVersion.setOnAction(e -> toast.accept(m.get("setting.version.latest", host.version())));

        Button dataFolder = commonButton(m.get("setting.dataFolder"), () -> {
            if (!host.openDataFolder()) {
                toast.accept(m.get("setting.dataFolder.failed"));
            }
        });
        Button restart = commonButton(m.get("menu.restart"), host::restart);
        restart.setVisible(host.restartAvailable());
        restart.setManaged(host.restartAvailable());
        Button quit = commonButton(m.get("menu.quit"), host::quit);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox buttons = new HBox(10, dataFolder, gap, restart, quit);
        buttons.setPadding(new Insets(50, 0, 8, 0));

        VBox rows = new VBox(10,
                row(m.get("setting.nickname"), null, nickname),
                row(m.get("setting.totalTaps"), null, taps),
                row(m.get(host.launchAtLoginAvailable() ? "setting.launchAtLogin" : "setting.launchAtLogin.unavailable"),
                        null, autoStart),
                row(m.get("setting.taskbar"), m.get("setting.taskbar.tip"),
                        toggle(s.showTaskbarIcon(), Settings::withShowTaskbarIcon)),
                row(m.get("setting.lockPosition"), m.get("setting.lockPosition.tip"),
                        toggle(s.lockPosition(), Settings::withLockPosition)),
                row(m.get("setting.key.hide"), m.get("setting.key.hide.tip"),
                        shortcut(Settings::hidePetKey, Settings::withHidePetKey),
                        new SwitchButton(!host.petVisible(), hidden -> host.setPetVisible(!hidden))),
                row(m.get("setting.gameMode"), m.get("setting.gameMode.tip"),
                        shortcut(Settings::gameModeKey, Settings::withGameModeKey),
                        toggle(s.gameMode(), Settings::withGameMode)),
                row(m.get("setting.autoGameMode"), m.get("setting.autoGameMode.tip"),
                        toggle(s.autoGameMode(), Settings::withAutoGameMode)),
                row(m.get("setting.fadeOnHover"), m.get("setting.fadeOnHover.tip"),
                        toggle(s.fadeOnHover(), Settings::withFadeOnHover)),
                row(m.get("setting.alwaysShowChests"), m.get("setting.alwaysShowChests.tip"),
                        toggle(s.alwaysShowChests(), Settings::withAlwaysShowChests)),
                row(m.get("setting.opacity"), null,
                        range(Settings.OPACITY_MIN, 100, s.opacityPercent(), Settings::withOpacityPercent)),
                row(m.get("setting.scale"), null,
                        range(Settings.SCALE_MIN, Settings.SCALE_MAX, s.scalePercent(), Settings::withScalePercent)),
                row(screen, screenTip, shortcut(Settings::switchScreenKey, Settings::withSwitchScreenKey), switchScreen),
                row(m.get("setting.language"), null, new SelectBox<>(List.of(Language.values()), s.language(),
                        l -> m.get("language." + l.name()), picked -> settings.update(x -> x.withLanguage(picked)))),
                row(m.get("setting.version", host.version()), null, checkVersion),
                buttons);
        rows.setPadding(new Insets(0, 27, 0, 27));
        root.setContent(rows);
        root.setFitToWidth(true);
        root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        // Catches the key for a shortcut being chosen, before any control uses it.
        root.addEventFilter(KeyEvent.KEY_PRESSED, this::keyPressed);
        root.addEventFilter(KeyEvent.KEY_RELEASED, this::keyReleased);
        refresh();
    }

    @Override
    public Node node() {
        return root;
    }

    /** Updates the numbers that change by themselves. */
    @Override
    public void refresh() {
        Messages m = host.messages();
        long count = host.totalTaps();
        taps.setText(count > MAX_SHOWN_TAPS ? MAX_SHOWN_TAPS + "+" : Long.toString(count));
        int total = Math.max(1, host.screenCount());
        int current = Math.min(host.settings().get().monitor(), total - 1) + 1;
        screen.setText(m.get("setting.screen", current));
        // The explanation names the key, so it is only there while a key is set.
        String key = host.settings().get().switchScreenKey().label();
        screenTip.setText(key.isEmpty() ? "" : m.get("setting.screen.tip", key));
        screenTip.setVisible(!key.isEmpty());
        screenTip.setManaged(!key.isEmpty());
    }

    /** Stops waiting for a shortcut key, if the page was. Call when the page goes away. */
    void cancelCapture() {
        stopCapture();
    }

    // ---- rows ----

    private static HBox row(String title, String explanation, Node... controls) {
        Label name = new Label(title);
        Label sub = null;
        if (explanation != null) {
            sub = new Label(explanation);
            sub.getStyleClass().add("setting-sub");
        }
        return row(name, sub, controls);
    }

    private static HBox row(Label title, Label explanation, Node... controls) {
        title.getStyleClass().add("setting-title");
        VBox text = new VBox(title);
        if (explanation != null) {
            if (!explanation.getStyleClass().contains("setting-sub")) {
                explanation.getStyleClass().add("setting-sub");
            }
            text.getChildren().add(explanation);
        }
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox right = new HBox(10, controls);
        right.setAlignment(Pos.CENTER_RIGHT);
        HBox row = new HBox(text, gap, right);
        row.getStyleClass().add("setting-item");
        return row;
    }

    private SwitchButton toggle(boolean on, BiFunction<Settings, Boolean, Settings> change) {
        return new SwitchButton(on, value -> host.settings().update(x -> change.apply(x, value)));
    }

    /** A slider for a percentage with its value beside it; its filled part follows the knob. */
    private Node range(int min, int max, int value, BiFunction<Settings, Integer, Settings> change) {
        Slider slider = new Slider(min, max, value);
        slider.getStyleClass().add("range");
        slider.setPrefWidth(170);
        Label shown = new Label(value + "%");
        shown.getStyleClass().add("setting-value");
        shown.setMinWidth(50);
        Runnable paint = () -> {
            double part = (slider.getValue() - min) / (max - min) * 100;
            slider.lookupAll(".track").forEach(track -> track.setStyle(
                    "-fx-background-color: linear-gradient(to right, #ffdca8 0%, #ffdca8 " + part + "%, #eed4b8 "
                            + part + "%, #eed4b8 100%);"));
        };
        Runnable apply = () -> host.settings().update(x -> change.apply(x, (int) Math.round(slider.getValue())));
        slider.valueProperty().addListener((o, was, now) -> {
            shown.setText(Math.round(now.doubleValue()) + "%");
            paint.run();
            // The setting changes when the knob is let go, not at every step of the way.
            if (!slider.isValueChanging()) {
                apply.run();
            }
        });
        slider.valueChangingProperty().addListener((o, was, changing) -> {
            if (!changing) {
                apply.run();
            }
        });
        // The track exists only once the slider has been given its skin.
        slider.skinProperty().addListener((o, was, skin) -> paint.run());
        HBox box = new HBox(10, slider, shown);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    private static Button commonButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("common-button");
        button.setOnAction(e -> action.run());
        return button;
    }

    // ---- shortcut keys ----

    /**
     * A field showing a shortcut. A click makes it wait for keys: what is pressed shows in the
     * field, and becomes the shortcut when the keys are let go; Escape or a second click leaves
     * it as it was; its round button clears it.
     */
    private ShortcutField shortcut(Function<Settings, Shortcut> get, BiFunction<Settings, Shortcut, Settings> set) {
        ShortcutRow[] self = new ShortcutRow[1];
        ShortcutField field = new ShortcutField(
                () -> {
                    if (capturing == self[0]) {
                        stopCapture();
                    } else {
                        startCapture(self[0]);
                    }
                },
                () -> {
                    stopCapture();
                    host.settings().update(x -> set.apply(x, Shortcut.NONE));
                    showShortcuts();
                });
        self[0] = new ShortcutRow(field, get, set);
        shortcuts.add(self[0]);
        show(self[0]);
        return field;
    }

    private void show(ShortcutRow shortcut) {
        String name = shortcut.get().apply(host.settings().get()).label();
        if (name.isEmpty()) {
            shortcut.field().showUnset(host.messages().get("setting.key.unset"));
        } else {
            shortcut.field().showKey(name);
        }
    }

    /** One shortcut changing can take the key away from another, so all are shown again. */
    private void showShortcuts() {
        shortcuts.forEach(this::show);
        refresh();
    }

    private void startCapture(ShortcutRow shortcut) {
        stopCapture();
        capturing = shortcut;
        pending = null;
        shortcut.field().showRecording(host.messages().get("setting.key.press"));
        // While a key is being chosen, pressing it must not set off the shortcut it already is.
        host.setHotkeysPaused(true);
        root.requestFocus();
    }

    private void stopCapture() {
        if (capturing != null) {
            ShortcutRow was = capturing;
            capturing = null;
            pending = null;
            show(was);
            host.setHotkeysPaused(false);
        }
    }

    private void keyPressed(KeyEvent e) {
        if (capturing == null) {
            return;
        }
        e.consume();
        if (e.getCode() == KeyCode.ESCAPE) {
            stopCapture();
            return;
        }
        if (KeyNames.isModifier(e.getCode())) {
            // Ctrl, Alt, Shift and Win only show what is held, until a key joins them.
            if (pending == null) {
                String held = KeyNames.pressedLabel(e);
                capturing.field().showRecording(held.isEmpty() ? host.messages().get("setting.key.press") : held);
            }
            return;
        }
        Shortcut pressed = KeyNames.shortcutFor(e);
        pending = pressed.isSet() ? pressed : null;
        capturing.field().showRecording(KeyNames.pressedLabel(e));
    }

    /** Letting the keys go ends the choosing: a usable shortcut is taken, anything else is dropped. */
    private void keyReleased(KeyEvent e) {
        if (capturing == null) {
            return;
        }
        e.consume();
        ShortcutRow chosen = capturing;
        Shortcut wanted = pending;
        stopCapture();
        if (wanted == null) {
            return;
        }
        for (ShortcutRow other : shortcuts) {
            if (other != chosen && other.get().apply(host.settings().get()).equals(wanted)) {
                toast.accept(host.messages().get("setting.key.duplicate"));
                return;
            }
        }
        host.settings().update(x -> chosen.set().apply(x, wanted));
        showShortcuts();
    }
}
