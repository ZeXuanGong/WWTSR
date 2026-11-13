package com.wwtsr.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wwtsr.input.Shortcut;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsTest {

    @TempDir
    Path dir;

    @Test
    void sizeAndOpacityStayInRange() {
        assertEquals(50, Settings.DEFAULT.withScalePercent(1).scalePercent());
        assertEquals(200, Settings.DEFAULT.withScalePercent(9_999).scalePercent());
        assertEquals(0, Settings.DEFAULT.withOpacityPercent(-5).opacityPercent());
        assertEquals(100, Settings.DEFAULT.withOpacityPercent(500).opacityPercent());
    }

    @Test
    void dockingInACornerForgetsTheDraggedPosition() {
        Settings dragged = Settings.DEFAULT.withPlacement(new DockRange(0, 100, 0, 100).placementOf(30, 40));
        assertTrue(dragged.hasCustomPosition());
        Settings docked = dragged.withCorner(Corner.TOP_LEFT);
        assertFalse(docked.hasCustomPosition());
        assertEquals(Corner.TOP_LEFT, docked.corner());
    }

    @Test
    void aPlaceIsAShareOfTheRoomAndSticksToTheEdges() {
        DockRange room = new DockRange(100, 1_100, 200, 700);
        Placement middle = room.placementOf(350, 450);
        assertEquals(0.25, middle.x().ratio(), 1e-9);
        assertEquals(0.5, middle.y().ratio(), 1e-9);
        assertFalse(middle.x().minEdge() || middle.x().maxEdge());
        // The same share of a room of another size.
        DockRange other = new DockRange(0, 2_000, 0, 1_000);
        assertEquals(500, other.xOf(middle), 1e-9);
        assertEquals(500, other.yOf(middle), 1e-9);

        // Within 3 pixels of an end counts as against it, and stays against it.
        Placement low = room.placementOf(600, 698);
        assertTrue(low.y().maxEdge());
        assertEquals(1_000, other.yOf(low), 1e-9);
        Placement left = room.placementOf(102, 450);
        assertTrue(left.x().minEdge());
        assertEquals(0, other.xOf(left), 1e-9);
        assertFalse(room.placementOf(104, 450).x().minEdge());

        // Outside the room is brought to its nearest end.
        assertTrue(room.placementOf(-50, 9_999).x().minEdge());
        assertEquals(0, new Placement.Axis(Double.NaN, false, false).ratio(), "not a number");
        assertEquals(1, new Placement.Axis(7, false, false).ratio());
    }

    @Test
    void theRoomKeepsThePetOnTheScreen() {
        // A 1920x1080 screen; the pet reaches 124 left, 148 right and 211 up from its pivot.
        DockRange room = DockRange.inside(0, 0, 1_920, 1_080, 124, 148, 211);
        assertEquals(124, room.clampX(-500));
        assertEquals(1_772, room.clampX(5_000));
        assertEquals(211, room.clampY(0));
        assertEquals(1_080, room.clampY(2_000));
        assertEquals(700, room.clampX(700));
        // An area narrower than the pet leaves one place: the middle.
        DockRange tight = DockRange.inside(0, 0, 200, 100, 124, 148, 211);
        assertEquals(tight.minX(), tight.maxX());
        assertEquals(88, tight.clampX(1_000));
    }

    @Test
    void nicknamesAreTrimmedAndCut() {
        assertEquals("Rover", Settings.DEFAULT.withNickname("  Rover  ").nickname());
        assertEquals(16, Settings.DEFAULT.withNickname("x".repeat(40)).nickname().length());
        assertEquals("", Settings.DEFAULT.withNickname(null).nickname());
    }

    @Test
    void oneKeyCannotBeBothShortcuts() {
        Settings s = Settings.DEFAULT.withSwitchScreenKey(Settings.DEFAULT_HIDE_PET_KEY);
        assertEquals(Settings.DEFAULT_HIDE_PET_KEY, s.hidePetKey());
        assertEquals(Shortcut.NONE, s.switchScreenKey(), "the second one loses the key");
        assertEquals(Shortcut.NONE, Settings.DEFAULT.withHidePetKey(Shortcut.ofKey(9_999)).hidePetKey(), "not a key");
        assertEquals(Shortcut.NONE, Settings.DEFAULT.withHidePetKey(null).hidePetKey());
        Shortcut withCtrl = new Shortcut(Shortcut.CTRL, 0x78);
        assertEquals(withCtrl, Settings.DEFAULT.withSwitchScreenKey(withCtrl).switchScreenKey(),
                "the same key with Ctrl held is another shortcut");
    }

    @Test
    void movingToAnotherScreenKeepsTheDraggedPlace() {
        Placement place = new Placement(new Placement.Axis(0.25, false, false), new Placement.Axis(1, false, true));
        Settings moved = Settings.DEFAULT.withPlacement(place).withMonitor(1);
        assertEquals(1, moved.monitor());
        assertEquals(Optional.of(place), moved.placement());
        assertFalse(moved.withCorner(Corner.TOP_LEFT).hasCustomPosition(), "docking in a corner forgets it");
        assertEquals(0, Settings.DEFAULT.withMonitor(-3).monitor());
    }

    @Test
    void nothingSavedReadsAsDefaults() {
        assertEquals(Settings.DEFAULT, new FileSettingsStore(dir.resolve("settings.properties")).load());
    }

    @Test
    void savedSettingsComeBack() throws IOException {
        Settings chosen = Settings.DEFAULT
                .withCorner(Corner.TOP_LEFT)
                .withScalePercent(150)
                .withOpacityPercent(60)
                .withLockPosition(true)
                .withFadeOnHover(true)
                .withAlwaysShowChests(true)
                .withGameMode(true)
                .withAutoGameMode(false)
                .withLaunchAtLogin(true)
                .withShowTaskbarIcon(false)
                .withLanguage(Language.ENGLISH)
                .withNickname("Rover")
                .withMonitor(1)
                .withHidePetKey(Shortcut.ofKey(0x79))
                .withSwitchScreenKey(new Shortcut(Shortcut.CTRL | Shortcut.SHIFT, 0x41))
                .withPlacement(new Placement(new Placement.Axis(0.125, true, false),
                        new Placement.Axis(0.75, false, false)));
        FileSettingsStore store = new FileSettingsStore(dir.resolve("settings.properties"));
        store.save(chosen);
        assertEquals(chosen, store.load());
        store.save(Settings.DEFAULT);
        assertEquals(Settings.DEFAULT, store.load(), "a dropped position isn't left behind in the file");
    }

    @Test
    void oneDamagedValueTakesItsDefaultAndTheRestAreKept() throws IOException {
        Path file = dir.resolve("settings.properties");
        Files.writeString(file, """
                version=1
                corner=SOMEWHERE
                scalePercent=abc
                opacityPercent=60
                lockPosition=maybe
                language=ENGLISH
                placeX=0.5
                hidePetKey=65
                """);
        Settings loaded = new FileSettingsStore(file).load();
        assertEquals(Corner.BOTTOM_RIGHT, loaded.corner());
        assertEquals(100, loaded.scalePercent());
        assertEquals(60, loaded.opacityPercent());
        assertFalse(loaded.lockPosition());
        assertEquals(Language.ENGLISH, loaded.language());
        assertFalse(loaded.hasCustomPosition(), "half a position is no position");
        assertEquals(Shortcut.NONE, loaded.hidePetKey(), "a letter alone can't be a shortcut");
        assertEquals(Settings.DEFAULT_GAME_MODE_KEY, loaded.gameModeKey(), "a missing one takes its default");
    }

    @Test
    void outOfRangeValuesInTheFileAreClamped() throws IOException {
        Path file = dir.resolve("settings.properties");
        Files.writeString(file, "version=1\nscalePercent=100000\nopacityPercent=-3\n");
        Settings loaded = new FileSettingsStore(file).load();
        assertEquals(200, loaded.scalePercent());
        assertEquals(0, loaded.opacityPercent());
    }

    @Test
    void aNewerOrUnreadableFileReadsAsDefaults() throws IOException {
        Path file = dir.resolve("settings.properties");
        Files.writeString(file, "version=2\nscalePercent=150\n");
        assertEquals(Settings.DEFAULT, new FileSettingsStore(file).load());
        Files.writeString(file, "scalePercent=150\n");
        assertEquals(Settings.DEFAULT, new FileSettingsStore(file).load(), "no version line");
    }

    @Test
    void listenersHearOnlyRealChanges() {
        AppSettings settings = new AppSettings(Settings.DEFAULT);
        List<Settings> heard = new ArrayList<>();
        settings.addListener(heard::add);
        settings.update(s -> s.withScalePercent(100));
        assertTrue(heard.isEmpty(), "same value");
        settings.update(s -> s.withScalePercent(150));
        assertEquals(1, heard.size());
        assertEquals(150, settings.get().scalePercent());
    }
}
