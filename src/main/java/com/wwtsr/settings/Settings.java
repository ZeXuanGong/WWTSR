package com.wwtsr.settings;

import com.wwtsr.input.Shortcut;
import java.util.Objects;
import java.util.Optional;

/**
 * Everything the person can choose. Immutable: a change makes a new value through a
 * {@code with…} method, so a half-changed setting never exists.
 *
 * @param corner           the corner the pet docks in
 * @param scalePercent     the pet's size, {@value #SCALE_MIN}–{@value #SCALE_MAX}
 * @param opacityPercent   how solid the pet is, {@value #OPACITY_MIN}–100
 * @param lockPosition     the pet can't be dragged
 * @param fadeOnHover      the pet turns faint while the mouse rests on it
 * @param alwaysShowChests show chests that aren't ready yet, with their countdown
 * @param gameMode         the mouse passes through the pet, except while a chest is ready
 * @param autoGameMode     switch game mode on by itself while a full-screen program is in front
 * @param launchAtLogin    start with the computer
 * @param showTaskbarIcon  show a button on the taskbar
 * @param language         the language of the app's text
 * @param nickname         what the person calls themselves; at most {@value #NICKNAME_MAX} characters
 * @param monitor          which screen the pet docks on, counting from 0; a screen that isn't
 *                         there means the main one
 * @param hidePetKey       the shortcut that shows and hides the pet
 * @param switchScreenKey  the shortcut that moves the pet to the next screen
 * @param gameModeKey      the shortcut that switches game mode on and off
 * @param placement        where the person dragged the pet to, if they did
 */
public record Settings(
        Corner corner,
        int scalePercent,
        int opacityPercent,
        boolean lockPosition,
        boolean fadeOnHover,
        boolean alwaysShowChests,
        boolean gameMode,
        boolean autoGameMode,
        boolean launchAtLogin,
        boolean showTaskbarIcon,
        Language language,
        String nickname,
        int monitor,
        Shortcut hidePetKey,
        Shortcut switchScreenKey,
        Shortcut gameModeKey,
        Optional<Placement> placement) {

    public static final int SCALE_MIN = 50;
    public static final int SCALE_MAX = 200;
    public static final int SCALE_DEFAULT = 100;
    /** At 0 the pet can't be seen at all; the tray menu and the panel still work. */
    public static final int OPACITY_MIN = 0;
    public static final int NICKNAME_MAX = 16;
    /** F9. */
    public static final Shortcut DEFAULT_HIDE_PET_KEY = Shortcut.ofKey(0x78);
    /** F8. */
    public static final Shortcut DEFAULT_SWITCH_SCREEN_KEY = Shortcut.ofKey(0x77);
    /** F7. */
    public static final Shortcut DEFAULT_GAME_MODE_KEY = Shortcut.ofKey(0x76);

    public static final Settings DEFAULT = new Settings(
            Corner.BOTTOM_RIGHT, SCALE_DEFAULT, 100, false, false, false, false, true, false, true,
            Language.AUTO, "", 0, DEFAULT_HIDE_PET_KEY, DEFAULT_SWITCH_SCREEN_KEY, DEFAULT_GAME_MODE_KEY,
            Optional.empty());

    public Settings {
        Objects.requireNonNull(corner);
        Objects.requireNonNull(language);
        Objects.requireNonNull(placement);
        hidePetKey = hidePetKey == null ? Shortcut.NONE : hidePetKey;
        switchScreenKey = switchScreenKey == null ? Shortcut.NONE : switchScreenKey;
        gameModeKey = gameModeKey == null ? Shortcut.NONE : gameModeKey;
        scalePercent = Math.clamp(scalePercent, SCALE_MIN, SCALE_MAX);
        opacityPercent = Math.clamp(opacityPercent, OPACITY_MIN, 100);
        nickname = nickname == null ? "" : nickname.strip();
        if (nickname.codePointCount(0, nickname.length()) > NICKNAME_MAX) {
            nickname = nickname.substring(0, nickname.offsetByCodePoints(0, NICKNAME_MAX));
        }
        monitor = Math.max(0, monitor);
        // One shortcut can't do two jobs: the later one loses it.
        if (switchScreenKey.isSet() && switchScreenKey.equals(hidePetKey)) {
            switchScreenKey = Shortcut.NONE;
        }
        if (gameModeKey.isSet() && (gameModeKey.equals(hidePetKey) || gameModeKey.equals(switchScreenKey))) {
            gameModeKey = Shortcut.NONE;
        }
    }

    public boolean hasCustomPosition() {
        return placement.isPresent();
    }

    /** Docking in a corner forgets where the pet was dragged to. */
    public Settings withCorner(Corner value) {
        Copy c = copy();
        c.corner = value;
        c.placement = Optional.empty();
        return c.build();
    }

    public Settings withScalePercent(int value) {
        Copy c = copy();
        c.scalePercent = value;
        return c.build();
    }

    public Settings withOpacityPercent(int value) {
        Copy c = copy();
        c.opacityPercent = value;
        return c.build();
    }

    public Settings withLockPosition(boolean value) {
        Copy c = copy();
        c.lockPosition = value;
        return c.build();
    }

    public Settings withFadeOnHover(boolean value) {
        Copy c = copy();
        c.fadeOnHover = value;
        return c.build();
    }

    public Settings withAlwaysShowChests(boolean value) {
        Copy c = copy();
        c.alwaysShowChests = value;
        return c.build();
    }

    public Settings withGameMode(boolean value) {
        Copy c = copy();
        c.gameMode = value;
        return c.build();
    }

    public Settings withAutoGameMode(boolean value) {
        Copy c = copy();
        c.autoGameMode = value;
        return c.build();
    }

    public Settings withLaunchAtLogin(boolean value) {
        Copy c = copy();
        c.launchAtLogin = value;
        return c.build();
    }

    public Settings withShowTaskbarIcon(boolean value) {
        Copy c = copy();
        c.showTaskbarIcon = value;
        return c.build();
    }

    public Settings withLanguage(Language value) {
        Copy c = copy();
        c.language = value;
        return c.build();
    }

    public Settings withNickname(String value) {
        Copy c = copy();
        c.nickname = value;
        return c.build();
    }

    /** Moving to another screen keeps the pet's place: it lands in the matching spot there. */
    public Settings withMonitor(int value) {
        Copy c = copy();
        c.monitor = value;
        return c.build();
    }

    public Settings withHidePetKey(Shortcut value) {
        Copy c = copy();
        c.hidePetKey = value;
        return c.build();
    }

    public Settings withSwitchScreenKey(Shortcut value) {
        Copy c = copy();
        c.switchScreenKey = value;
        return c.build();
    }

    public Settings withGameModeKey(Shortcut value) {
        Copy c = copy();
        c.gameModeKey = value;
        return c.build();
    }

    public Settings withPlacement(Placement value) {
        Copy c = copy();
        c.placement = Optional.of(value);
        return c.build();
    }

    private Copy copy() {
        return new Copy(this);
    }

    /** A changeable copy, so each {@code with…} method names only what it changes. */
    private static final class Copy {
        Corner corner;
        int scalePercent;
        int opacityPercent;
        boolean lockPosition;
        boolean fadeOnHover;
        boolean alwaysShowChests;
        boolean gameMode;
        boolean autoGameMode;
        boolean launchAtLogin;
        boolean showTaskbarIcon;
        Language language;
        String nickname;
        int monitor;
        Shortcut hidePetKey;
        Shortcut switchScreenKey;
        Shortcut gameModeKey;
        Optional<Placement> placement;

        Copy(Settings s) {
            corner = s.corner;
            scalePercent = s.scalePercent;
            opacityPercent = s.opacityPercent;
            lockPosition = s.lockPosition;
            fadeOnHover = s.fadeOnHover;
            alwaysShowChests = s.alwaysShowChests;
            gameMode = s.gameMode;
            autoGameMode = s.autoGameMode;
            launchAtLogin = s.launchAtLogin;
            showTaskbarIcon = s.showTaskbarIcon;
            language = s.language;
            nickname = s.nickname;
            monitor = s.monitor;
            hidePetKey = s.hidePetKey;
            switchScreenKey = s.switchScreenKey;
            gameModeKey = s.gameModeKey;
            placement = s.placement;
        }

        Settings build() {
            return new Settings(corner, scalePercent, opacityPercent, lockPosition, fadeOnHover, alwaysShowChests,
                    gameMode, autoGameMode, launchAtLogin, showTaskbarIcon, language, nickname, monitor,
                    hidePetKey, switchScreenKey, gameModeKey, placement);
        }
    }
}
