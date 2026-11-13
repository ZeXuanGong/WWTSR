package com.wwtsr.app;

import com.wwtsr.assets.AssetPack;
import com.wwtsr.assets.GeneratedAssetPack;
import com.wwtsr.collect.Accessory;
import com.wwtsr.collect.Catalog;
import com.wwtsr.collect.Collectible;
import com.wwtsr.collect.Emote;
import com.wwtsr.collect.Inventory;
import com.wwtsr.collect.Loadout;
import com.wwtsr.collect.Role;
import com.wwtsr.i18n.Messages;
import com.wwtsr.menu.PanelPage;
import com.wwtsr.panel.PanelHost;
import com.wwtsr.panel.PanelWindow;
import com.wwtsr.player.Player;
import com.wwtsr.player.PlayerState;
import com.wwtsr.settings.AppSettings;
import com.wwtsr.settings.Language;
import com.wwtsr.settings.Settings;
import com.wwtsr.view.FxImages;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javax.imageio.ImageIO;

/**
 * A developer tool: draws every page of the panel to PNG files without showing a window, for
 * a made-up player who has found most things. Arguments: the folder to write into, and
 * optionally {@code zh} or {@code en} for the language.
 */
public final class PanelSheet extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        List<String> args = getParameters().getUnnamed();
        if (args.isEmpty()) {
            System.err.println("usage: PanelSheet <output folder> [zh|en]");
            Platform.exit();
            return;
        }
        Language language = args.size() > 1 && args.get(1).equals("en") ? Language.ENGLISH : Language.CHINESE;
        File folder = new File(args.get(0));
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new IOException("can't create " + folder);
        }
        AssetPack assets = new GeneratedAssetPack();
        PanelWindow panel = new PanelWindow(new SampleHost(assets, language), assets.icon());
        for (PanelPage page : PanelPage.values()) {
            File file = new File(folder, "panel-" + page.name().toLowerCase() + ".png");
            ImageIO.write(FxImages.toBuffered(panel.snapshot(page)), "png", file);
        }
        ImageIO.write(FxImages.toBuffered(panel.snapshot(PanelPage.SETTINGS, true)), "png",
                new File(folder, "panel-settings-folded.png"));
        Platform.exit();
    }

    /** A host with a well-stocked player and default settings; its buttons do nothing. */
    private static final class SampleHost implements PanelHost {
        private final AssetPack assets;
        private final Messages messages;
        private final AppSettings settings;
        private final Player player;

        SampleHost(AssetPack assets, Language language) {
            this.assets = assets;
            this.messages = Messages.forLanguage(language);
            this.settings = new AppSettings(Settings.DEFAULT.withLanguage(language).withNickname("Rover"));

            // Everything but the last few collectibles, with spares of the commoner ones.
            Map<Collectible, Integer> owned = new LinkedHashMap<>();
            List<Collectible> all = Catalog.all();
            for (int i = 0; i < all.size(); i++) {
                if (i % 5 != 4) {
                    owned.put(all.get(i), i % 3 == 0 ? 120 : 1 + i % 7);
                }
            }
            owned.put(Accessory.LEAF, 8);
            owned.put(Accessory.BOW, 6);
            Inventory bag = new Inventory(owned, Set.of(Accessory.BOW, Emote.HAPPY), Set.of(Accessory.GLASSES, Role.CAT),
                    List.of(Accessory.GLASSES, Role.CAT, Emote.HAPPY, Accessory.LEAF, Emote.ANGRY, Role.BEAR));
            Loadout look = new Loadout(Role.CAT, Optional.of(Accessory.GLASSES),
                    List.of(Emote.HAPPY, Emote.ANGRY, Emote.IDLE));
            player = new Player(new PlayerState(Map.of(), bag, look, List.of(
                    Optional.of(new Loadout(Role.BEAR, Optional.of(Accessory.BOW), List.of(Emote.HAPPY))),
                    Optional.empty(),
                    Optional.of(look))));
        }

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
            return 36_957;
        }

        @Override
        public RandomGenerator dice() {
            return new Random(1);
        }

        @Override
        public String version() {
            return "0.0.0";
        }

        @Override
        public boolean petVisible() {
            return true;
        }

        @Override
        public void setPetVisible(boolean visible) {
        }

        @Override
        public int screenCount() {
            return 2;
        }

        @Override
        public void switchScreen() {
        }

        @Override
        public boolean launchAtLoginAvailable() {
            return false;
        }

        @Override
        public void setLaunchAtLogin(boolean on) {
        }

        @Override
        public void setHotkeysPaused(boolean paused) {
        }

        @Override
        public boolean openDataFolder() {
            return false;
        }

        @Override
        public boolean restartAvailable() {
            return false;
        }

        @Override
        public void restart() {
        }

        @Override
        public void quit() {
        }
    }
}
