package com.wwtsr.app;

import com.wwtsr.assets.AssetPack;
import com.wwtsr.assets.GeneratedAssetPack;
import com.wwtsr.chest.ChestRules;
import com.wwtsr.chest.ChestSlot;
import com.wwtsr.chest.Reward;
import com.wwtsr.i18n.Messages;
import com.wwtsr.input.InputKind;
import com.wwtsr.input.InputListener;
import com.wwtsr.input.InputSource;
import com.wwtsr.input.Shortcut;
import com.wwtsr.menu.AppMenu;
import com.wwtsr.menu.MenuActions;
import com.wwtsr.menu.MenuEntry;
import com.wwtsr.menu.PanelPage;
import com.wwtsr.panel.PanelHost;
import com.wwtsr.panel.PanelWindow;
import com.wwtsr.pet.Pet;
import com.wwtsr.pet.PetTimings;
import com.wwtsr.platform.AutoStart;
import com.wwtsr.platform.FullscreenDetector;
import com.wwtsr.platform.SingleInstance;
import com.wwtsr.platform.WindowEffects;
import com.wwtsr.player.FilePlayerStore;
import com.wwtsr.player.Player;
import com.wwtsr.player.PlayerState;
import com.wwtsr.player.PlayerStore;
import com.wwtsr.settings.AppSettings;
import com.wwtsr.settings.FileSettingsStore;
import com.wwtsr.settings.Settings;
import com.wwtsr.settings.SettingsStore;
import com.wwtsr.stats.FileTapStore;
import com.wwtsr.stats.TapCounts;
import com.wwtsr.stats.TapStatistics;
import com.wwtsr.stats.TapStore;
import com.wwtsr.tray.TrayController;
import com.wwtsr.view.FxImages;
import com.wwtsr.view.PetView;
import com.wwtsr.view.PetWindow;
import com.wwtsr.view.Screens;
import com.wwtsr.view.TrayMenuPopup;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.random.RandomGenerator;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Starts WWTSR and wires its parts together: the pet, its pictures and window, the presses
 * heard anywhere on the computer, the player's chests and collectibles, the panel, the
 * settings, the menus and what is saved.
 *
 * <p>{@code --demo} makes the pet tap by itself with default settings; it listens to no input,
 * shows no tray icon and saves nothing.
 */
public final class WwtsrApp extends Application implements MenuActions, PanelHost {

    private static final String TITLE = "WWTSR";
    private static final String VERSION = "0.2.0";
    /** Time between taps in {@code --demo}. */
    private static final long DEMO_TAP_MILLIS = 400;
    /** How often the chests' countdowns are redrawn. */
    private static final long CHEST_REFRESH_MILLIS = 250;
    /** How often the screen is looked at: a full-screen program in front, a moved taskbar. */
    private static final long SCREEN_CHECK_MILLIS = 1_500;
    /** A shortcut pressed again sooner than this after it last ran is ignored. */
    private static final long SHORTCUT_GAP_MILLIS = 280;

    private final RandomGenerator dice = RandomGenerator.getDefault();
    /** The installed app's launcher, when the app runs from one. */
    private final String launcher = System.getProperty("jpackage.app-path");
    private boolean demo;
    private SingleInstance instance;
    private SaveQueue saves;
    private AppSettings settings;
    private Messages messages;
    private AutoStart autoStart;
    private FullscreenDetector fullscreen;
    private AssetPack assets;
    private Pet pet;
    private TapStatistics statistics;
    private AutoSaver saver;
    private Player player;
    private PetView view;
    private PetWindow window;
    private PanelWindow panel;
    private TrayController tray;
    private InputSource input;
    private AnimationTimer clock;
    private Watchdog watchdog;
    /** The person hid the pet (the shortcut key, the menu). */
    private boolean hiddenByPerson;
    private boolean fullscreenInFront;
    private final Debounce hidePetGap = new Debounce(SHORTCUT_GAP_MILLIS);
    private final Debounce gameModeGap = new Debounce(SHORTCUT_GAP_MILLIS);
    private final Debounce switchScreenGap = new Debounce(SHORTCUT_GAP_MILLIS);
    private boolean gameModeOn;
    private boolean hotkeysPaused;
    private boolean restartWanted;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        demo = getParameters().getUnnamed().contains("--demo");
        if (!demo && !becomeTheOnlyCopy()) {
            Platform.exit();
            return;
        }
        // Hiding the pet or closing the panel must not end the app: only Quit does.
        Platform.setImplicitExit(false);
        long now = nowMillis();
        saves = new SaveQueue();

        SettingsStore settingsStore = new FileSettingsStore(AppPaths.settingsFile());
        autoStart = AutoStart.forThisSystem();
        fullscreen = FullscreenDetector.forThisSystem();
        Settings loaded = demo ? Settings.DEFAULT : settingsStore.load();
        if (autoStart.available()) {
            // What Windows will really do wins over what the file remembers.
            loaded = loaded.withLaunchAtLogin(autoStart.enabled());
        }
        settings = new AppSettings(loaded);
        messages = Messages.forLanguage(loaded.language());

        TapStore tapStore = new FileTapStore(AppPaths.tapCountsFile());
        statistics = new TapStatistics(demo ? TapCounts.ZERO : tapStore.load());
        PlayerStore playerStore = new FilePlayerStore(AppPaths.playerFile());
        player = new Player(demo ? PlayerState.NEW : loadPlayer(playerStore));
        // The pet shows the taps left to spend: all there have been, less what chests have cost.
        pet = new Pet(PetTimings.DEFAULT, now,
                Math.max(0, statistics.counts().total() - player.state().spentTaps()));

        assets = new GeneratedAssetPack();
        view = new PetView(assets, pet, this::openChest, () -> panel.toggle());
        view.setLook(player.state().look());
        player.addListener(() -> {
            view.setLook(player.state().look());
            refreshChests();
            if (!demo) {
                PlayerState snapshot = player.state();
                saves.submit("the player", () -> playerStore.save(snapshot));
            }
        });

        boolean hearsEverything = false;
        if (!demo) {
            saver = new AutoSaver(statistics, tapStore, saves, now);
            input = InputSource.forThisSystem();
            applyHotkeys();
            // Presses arrive on the hook's thread; everything else lives on the JavaFX thread.
            hearsEverything = input.start(new InputListener() {
                @Override
                public void pressed(InputKind kind) {
                    Platform.runLater(() -> press(kind));
                }

                @Override
                public void released(InputKind kind) {
                    Platform.runLater(() -> release(kind));
                }
            });
        }
        // A click on the pet opens or closes its emotes. When presses everywhere are heard the
        // click is already counted as a tap; otherwise it is counted here.
        boolean countClicksHere = !demo && !hearsEverything;
        Runnable onClick = () -> {
            view.toggleEmotes();
            if (countClicksHere) {
                press(InputKind.MOUSE_BUTTON);
                release(InputKind.MOUSE_BUTTON);
            }
        };

        window = new PetWindow(stage, view, TITLE, WindowEffects.forThisSystem(), onClick,
                placement -> changeSettings(s -> s.withPlacement(placement)), this::menu);
        // Closing the window from outside (Alt+F4, the taskbar) quits, rather than leaving a
        // hidden app behind.
        stage.setOnCloseRequest(e -> quit());
        // The taskbar button shows the same face as the tray icon.
        stage.getIcons().add(assets.icon());
        window.apply(settings.get());
        panel = new PanelWindow(this, assets.icon());
        refreshChests();
        updateVisibility();

        if (!demo && TrayController.supported()) {
            tray = new TrayController(Platform::runLater);
            TrayMenuPopup trayMenu = new TrayMenuPopup();
            tray.install(FxImages.toBuffered(assets.icon()), messages.get("app.name"),
                    (x, y) -> trayMenu.show(x, y, menu()), this::togglePetVisible);
        }

        settings.addListener(new java.util.function.Consumer<>() {
            private Settings before = settings.get();

            @Override
            public void accept(Settings next) {
                boolean languageChanged = next.language() != before.language();
                before = next;
                if (languageChanged) {
                    messages = Messages.forLanguage(next.language());
                    panel.rebuild();
                    if (tray != null) {
                        tray.setTooltip(messages.get("app.name"));
                    }
                }
                window.apply(next);
                applyHotkeys();
                refreshChests();
                updateVisibility();
                if (!demo) {
                    saves.submit("the settings", () -> settingsStore.save(next));
                }
            }
        });

        clock = new AnimationTimer() {
            private long nextDemoTap = now + DEMO_TAP_MILLIS;
            private long nextChestRefresh;
            private long nextScreenCheck;

            @Override
            public void handle(long nowNanos) {
                long t = nowNanos / 1_000_000;
                if (demo && t >= nextDemoTap) {
                    pet.tap(t);
                    nextDemoTap = t + DEMO_TAP_MILLIS;
                }
                pet.tick(t);
                if (saver != null) {
                    saver.tick(t);
                }
                if (t >= nextScreenCheck) {
                    nextScreenCheck = t + SCREEN_CHECK_MILLIS;
                    followFullscreen();
                    window.keepInPlace();
                }
                if (t >= nextChestRefresh) {
                    nextChestRefresh = t + CHEST_REFRESH_MILLIS;
                    refreshChests();
                    updateVisibility();
                }
            }
        };
        clock.start();

        // Input hooks that went deaf are always restarted. Watching the user interface and
        // keeping a log is for debug runs only (mvn javafx:run sets the property).
        if (!demo) {
            watchdog = new Watchdog(AppPaths.watchdogLog(), Platform::runLater, input,
                    Boolean.getBoolean("wwtsr.debug"));
            watchdog.start();
        }
    }

    @Override
    public void stop() {
        if (watchdog != null) {
            watchdog.stop();
        }
        if (clock != null) {
            clock.stop();
        }
        if (input != null) {
            input.stop();
        }
        if (tray != null) {
            tray.remove();
        }
        if (saver != null) {
            saver.saveNow();
        }
        if (saves != null) {
            saves.close();
        }
        if (instance != null) {
            instance.close();
        }
        // Started last, once the "only one copy" lock is free for it.
        if (restartWanted && restartAvailable()) {
            try {
                new ProcessBuilder(launcher).start();
            } catch (IOException e) {
                System.err.println("WWTSR couldn't start again: " + e.getMessage());
            }
        }
    }

    // ---- MenuActions ----

    @Override
    public void openPanel(PanelPage page) {
        panel.show(page);
    }

    @Override
    public boolean petVisible() {
        return !hiddenByPerson;
    }

    @Override
    public void togglePetVisible() {
        hiddenByPerson = !hiddenByPerson;
        updateVisibility();
    }

    @Override
    public String hidePetKeyName() {
        return settings.get().hidePetKey().label();
    }

    @Override
    public void changeSettings(UnaryOperator<Settings> change) {
        settings.update(change);
    }

    @Override
    public boolean restartAvailable() {
        return launcher != null && !launcher.isBlank();
    }

    @Override
    public void restart() {
        restartWanted = true;
        Platform.exit();
    }

    @Override
    public void quit() {
        Platform.exit();
    }

    // ---- PanelHost ----

    @Override
    public Player player() {
        return player;
    }

    @Override
    public AppSettings settings() {
        return settings;
    }

    @Override
    public Messages messages() {
        return messages;
    }

    @Override
    public AssetPack assets() {
        return assets;
    }

    @Override
    public long totalTaps() {
        // Every tap there has been, including the ones chests have used up.
        return statistics.counts().total();
    }

    @Override
    public RandomGenerator dice() {
        return dice;
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public void setPetVisible(boolean visible) {
        if (visible == hiddenByPerson) {
            togglePetVisible();
        }
    }

    @Override
    public int screenCount() {
        return Screens.ordered().size();
    }

    @Override
    public void switchScreen() {
        int count = screenCount();
        settings.update(s -> s.withMonitor((Math.min(s.monitor(), count - 1) + 1) % count));
    }

    @Override
    public boolean launchAtLoginAvailable() {
        return autoStart.available();
    }

    @Override
    public void setLaunchAtLogin(boolean on) {
        if (autoStart.set(on)) {
            settings.update(s -> s.withLaunchAtLogin(on));
        }
    }

    @Override
    public void setHotkeysPaused(boolean paused) {
        hotkeysPaused = paused;
        applyHotkeys();
    }

    @Override
    public boolean openDataFolder() {
        Path folder = AppPaths.dataDir();
        if (!Files.isDirectory(folder) || !Desktop.isDesktopSupported()) {
            return false;
        }
        // Asking the system to open a folder can take a moment: not on the JavaFX thread.
        Thread opener = new Thread(() -> {
            try {
                Desktop.getDesktop().open(folder.toFile());
            } catch (IOException | RuntimeException e) {
                System.err.println("WWTSR couldn't open its data folder: " + e.getMessage());
            }
        }, "wwtsr-open-folder");
        opener.setDaemon(true);
        opener.start();
        return true;
    }

    // ---- helpers ----

    private List<MenuEntry> menu() {
        return AppMenu.build(settings.get(), messages, this);
    }

    /**
     * Something was pressed: a hand goes down. A mouse button is counted now, a key when it
     * is let go. Clicks made while the pet's own menu is open aren't taps at all.
     */
    private void press(InputKind kind) {
        if (kind == InputKind.MOUSE_BUTTON) {
            if (window.menuOpen()) {
                return;
            }
            statistics.record(kind);
            pet.count();
        }
        pet.strike(nowMillis());
    }

    /** What was pressed was let go: a key is counted, and the hand comes up at once. */
    private void release(InputKind kind) {
        if (kind == InputKind.KEY) {
            statistics.record(kind);
            pet.count();
        }
        pet.release();
    }

    /**
     * Full-screen game mode: when a full-screen program comes to the front, or leaves it, the
     * game mode setting itself is switched to match (and saved, like any change to it).
     */
    private void followFullscreen() {
        if (!settings.get().autoGameMode()) {
            fullscreenInFront = false;
            return;
        }
        boolean inFront = fullscreen.foregroundIsFullscreen();
        if (inFront != fullscreenInFront) {
            fullscreenInFront = inFront;
            if (settings.get().gameMode() != inFront) {
                changeSettings(s -> s.withGameMode(inFront));
            }
        }
    }

    /** Tells the input source which keys are shortcuts now (none while one is being chosen). */
    private void applyHotkeys() {
        if (input == null) {
            return;
        }
        Map<Shortcut, Runnable> keys = new HashMap<>();
        if (!hotkeysPaused) {
            Settings s = settings.get();
            addHotkey(keys, s.hidePetKey(), hidePetGap, this::togglePetVisible);
            addHotkey(keys, s.switchScreenKey(), switchScreenGap, this::switchScreen);
            addHotkey(keys, s.gameModeKey(), gameModeGap, () -> changeSettings(x -> x.withGameMode(!x.gameMode())));
        }
        input.setHotkeys(keys);
    }

    /** The action runs on the JavaFX thread, and not again until {@code gap} has passed. */
    private static void addHotkey(Map<Shortcut, Runnable> keys, Shortcut shortcut, Debounce gap, Runnable action) {
        if (shortcut.isSet()) {
            keys.put(shortcut, () -> Platform.runLater(() -> {
                if (gap.pass(nowMillis())) {
                    action.run();
                }
            }));
        }
    }

    /** Opens a chest the person clicked and shows what was inside. */
    private void openChest(ChestSlot slot) {
        Optional<Reward> opened = player.open(slot, pet.tapCount(), System.currentTimeMillis(), dice);
        if (opened.isEmpty()) {
            return;
        }
        Reward reward = opened.get();
        pet.spend(ChestRules.OPEN_COST);
        boolean first = reward.isFirst();
        view.showReward(reward,
                messages.get(first ? "reward." + reward.item().type().name() : "reward.duplicate"), first);
    }

    /**
     * Draws each chest as it is now. A chest that can be opened always shows; one that is
     * still waiting shows only while the mouse is on the pet, or when the setting says always.
     */
    private void refreshChests() {
        long taps = pet.tapCount();
        long now = System.currentTimeMillis();
        boolean showWaiting = settings.get().alwaysShowChests() || view.isHover();
        for (ChestSlot slot : ChestSlot.values()) {
            boolean ready = player.ready(slot, taps, now);
            long remaining = player.remainingMillis(slot, now);
            view.showChest(slot, player.shown(slot, taps, now, showWaiting), ready, remaining,
                    (double) remaining / ChestRules.COOLDOWN_MILLIS);
        }
    }

    /**
     * Shows or hides the window (the person's choice), and applies game mode: the pet stays
     * on screen but the mouse passes straight through it, so it can't get in the way; while a
     * chest is waiting to be opened it can be clicked again.
     */
    private void updateVisibility() {
        boolean gameMode = settings.get().gameMode();
        if (gameMode != gameModeOn) {
            gameModeOn = gameMode;
            view.showToast(messages.get(gameMode ? "game.entered" : "game.exited"));
            if (gameMode) {
                view.closeEmotes();
            }
        }
        boolean chestWaiting = player.anyReady(pet.tapCount(), System.currentTimeMillis());
        window.setMouseThrough(gameMode && !chestWaiting);
        boolean show = !hiddenByPerson;
        if (show && !window.isShowing()) {
            window.show();
        } else if (!show && window.isShowing()) {
            window.hide();
        }
    }

    /** The saved player; or, coming from an earlier version, what its chest file held. */
    private static PlayerState loadPlayer(PlayerStore store) {
        if (Files.isRegularFile(AppPaths.playerFile())) {
            return store.load();
        }
        return LegacyChests.read(AppPaths.legacyChestsFile()).orElse(PlayerState.NEW);
    }

    /**
     * Whether this is the only running copy. If the lock file can't be used at all, the app
     * still starts: refusing to run would be worse than a second pet.
     */
    private boolean becomeTheOnlyCopy() {
        try {
            Optional<SingleInstance> held = SingleInstance.tryAcquire(AppPaths.lockFile());
            if (held.isEmpty()) {
                return false;
            }
            instance = held.get();
            return true;
        } catch (IOException e) {
            return true;
        }
    }

    /** The same clock {@link AnimationTimer} reports, in milliseconds. */
    private static long nowMillis() {
        return System.nanoTime() / 1_000_000;
    }
}
