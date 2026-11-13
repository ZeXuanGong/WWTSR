package com.wwtsr.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wwtsr.i18n.Messages;
import com.wwtsr.menu.MenuEntry.Action;
import com.wwtsr.menu.MenuEntry.Choice;
import com.wwtsr.menu.MenuEntry.Option;
import com.wwtsr.menu.MenuEntry.Toggle;
import com.wwtsr.settings.AppSettings;
import com.wwtsr.settings.Language;
import com.wwtsr.settings.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class AppMenuTest {

    /** Stands in for the app: remembers what the menu asked for. */
    private static final class FakeApp implements MenuActions {
        final AppSettings settings = new AppSettings(Settings.DEFAULT);
        final List<PanelPage> opened = new ArrayList<>();
        boolean visible = true;
        boolean canRestart;
        boolean restartAsked;
        boolean quitAsked;
        String keyName = "F9";

        @Override
        public void openPanel(PanelPage page) {
            opened.add(page);
        }

        @Override
        public boolean petVisible() {
            return visible;
        }

        @Override
        public void togglePetVisible() {
            visible = !visible;
        }

        @Override
        public String hidePetKeyName() {
            return keyName;
        }

        @Override
        public void changeSettings(UnaryOperator<Settings> change) {
            settings.update(change);
        }

        @Override
        public boolean restartAvailable() {
            return canRestart;
        }

        @Override
        public void restart() {
            restartAsked = true;
        }

        @Override
        public void quit() {
            quitAsked = true;
        }

        List<MenuEntry> menu() {
            return AppMenu.build(settings.get(), Messages.forLanguage(Language.ENGLISH), this);
        }
    }

    private static String label(MenuEntry entry) {
        return switch (entry) {
            case Action a -> a.label();
            case Toggle t -> t.label();
            case Choice c -> c.label();
            case MenuEntry.Separator s -> "-";
        };
    }

    private static <T extends MenuEntry> T find(List<MenuEntry> menu, Class<T> type, String label) {
        return menu.stream()
                .filter(type::isInstance).map(type::cast)
                .filter(e -> label.equals(label(e)))
                .findFirst().orElseThrow(() -> new AssertionError("no entry: " + label));
    }

    private static Option selected(Choice choice) {
        List<Option> on = choice.options().stream().filter(Option::selected).toList();
        assertEquals(1, on.size(), "exactly one option selected in " + choice.label());
        return on.get(0);
    }

    @Test
    void theMenuListsThePanelPagesThenTheQuickSettings() {
        assertEquals(List.of("Bag", "Atlas", "Synthesis", "Settings", "-", "Hide pet (F9)", "Pet size",
                        "Pet opacity", "Lock position", "-", "Quit"),
                new FakeApp().menu().stream().map(AppMenuTest::label).toList());
    }

    @Test
    void thePageEntriesOpenThePanelOnThatPage() {
        FakeApp app = new FakeApp();
        find(app.menu(), Action.class, "Atlas").run().run();
        find(app.menu(), Action.class, "Settings").run().run();
        assertEquals(List.of(PanelPage.ATLAS, PanelPage.SETTINGS), app.opened);
    }

    @Test
    void theShowHideLineFollowsThePetAndItsKey() {
        FakeApp app = new FakeApp();
        find(app.menu(), Action.class, "Hide pet (F9)").run().run();
        assertFalse(app.visible);
        find(app.menu(), Action.class, "Show pet (F9)").run().run();
        assertTrue(app.visible);
        app.keyName = "";
        find(app.menu(), Action.class, "Hide pet");
    }

    @Test
    void pickingASizeChangesTheSettingAndTheTick() {
        FakeApp app = new FakeApp();
        Choice size = find(app.menu(), Choice.class, "Pet size");
        assertEquals("100%", selected(size).label());
        size.options().stream().filter(o -> o.label().equals("150%")).findFirst().orElseThrow().run().run();
        assertEquals(150, app.settings.get().scalePercent());
        assertEquals("150%", selected(find(app.menu(), Choice.class, "Pet size")).label());
    }

    @Test
    void lockPositionFlipsItsSetting() {
        FakeApp app = new FakeApp();
        Toggle lock = find(app.menu(), Toggle.class, "Lock position");
        assertFalse(lock.selected());
        lock.run().run();
        assertTrue(app.settings.get().lockPosition());
        assertTrue(find(app.menu(), Toggle.class, "Lock position").selected());
    }

    @Test
    void restartIsOfferedOnlyWhenItCanWork() {
        FakeApp app = new FakeApp();
        assertTrue(app.menu().stream().noneMatch(e -> "Restart".equals(label(e))));
        app.canRestart = true;
        find(app.menu(), Action.class, "Restart").run().run();
        assertTrue(app.restartAsked);
    }

    @Test
    void quitIsLast() {
        FakeApp app = new FakeApp();
        List<MenuEntry> menu = app.menu();
        ((Action) menu.get(menu.size() - 1)).run().run();
        assertTrue(app.quitAsked);
    }
}
