package com.wwtsr.app;

import com.wwtsr.assets.AssetPack;
import com.wwtsr.assets.GeneratedAssetPack;
import com.wwtsr.chest.ChestSlot;
import com.wwtsr.chest.Reward;
import com.wwtsr.collect.Accessory;
import com.wwtsr.collect.Catalog;
import com.wwtsr.collect.Collectible;
import com.wwtsr.collect.Emote;
import com.wwtsr.collect.Loadout;
import com.wwtsr.collect.Role;
import com.wwtsr.pet.Pet;
import com.wwtsr.pet.PetTimings;
import com.wwtsr.view.FxImages;
import com.wwtsr.view.PetView;
import com.wwtsr.view.RewardEffect;
import javafx.util.Duration;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javax.imageio.ImageIO;

/**
 * A developer tool: draws the artwork to a PNG without showing a window: each pose, each
 * role, a pet with chests and a reward, and the thumbnail of every collectible. Used to check
 * the pictures. Argument: the PNG file to write.
 */
public final class ContactSheet extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        List<String> args = getParameters().getUnnamed();
        if (args.isEmpty()) {
            System.err.println("usage: ContactSheet <output.png>");
            Platform.exit();
            return;
        }
        AssetPack assets = new GeneratedAssetPack();

        PetView withChests = posed(assets, Loadout.STARTER, 0, 0);
        withChests.showChest(ChestSlot.FIRST, true, true, 0, 0);
        withChests.showChest(ChestSlot.SECOND, true, false, 17 * 60_000 + 5_000, 17.0 / 30);
        HBox poses = new HBox(8,
                posed(assets, Loadout.STARTER, 0, 0),         // idle
                posed(assets, Loadout.STARTER, 1, 100),       // left hand down
                posed(assets, Loadout.STARTER, 2, 100),       // right hand down
                posed(assets, Loadout.STARTER, 0, 5_000),     // blinking
                withChests);

        // The reward reveal: each rarity once it has settled, and the rarest part-way through.
        HBox rewards = new HBox(4);
        Collectible[] prizes = {Accessory.LEAF, Emote.ANGRY, Role.BUNNY, Accessory.CROWN};
        for (Collectible prize : prizes) {
            RewardEffect effect = new RewardEffect(assets);
            effect.showAt(new Reward(prize, 1), "New!", true, false, Duration.millis(1_600));
            rewards.getChildren().add(effect);
        }
        for (double at : new double[] {330, 520, 760}) {
            RewardEffect effect = new RewardEffect(assets);
            effect.showAt(new Reward(Accessory.CROWN, 2), "Duplicate", false, false, Duration.millis(at));
            rewards.getChildren().add(effect);
        }

        HBox roles = new HBox(8);
        Accessory[] hats = {Accessory.BOW, Accessory.HEADPHONES, Accessory.WIZARD_HAT, Accessory.CROWN};
        for (Role role : Role.values()) {
            Loadout look = new Loadout(role, Optional.of(hats[role.ordinal() % hats.length]),
                    List.of(Emote.HAPPY, Emote.ANGRY, Emote.STUNNED, Emote.THREATEN, Emote.TRIUMPH));
            PetView pet = posed(assets, look, role.ordinal() % 3, 100);
            pet.setEmotesOpenNow(true);
            roles.getChildren().add(pet);
        }

        HBox thumbnails = new HBox(6);
        for (Collectible item : Catalog.all()) {
            ImageView picture = new ImageView(assets.thumbnail(item));
            picture.setFitWidth(64);
            picture.setFitHeight(64);
            thumbnails.getChildren().add(picture);
        }

        VBox sheet = new VBox(12, poses, roles, rewards, thumbnails);
        sheet.setBackground(new Background(new BackgroundFill(Color.web("#5b6b7a"), null, null)));
        new Scene(sheet);
        sheet.applyCss();
        sheet.layout();
        WritableImage shot = sheet.snapshot(new SnapshotParameters(), null);
        ImageIO.write(FxImages.toBuffered(shot), "png", new File(args.get(0)));
        Platform.exit();
    }

    /** A view of a pet with that look after {@code taps} taps, with its clock moved to {@code atMillis}. */
    private static PetView posed(AssetPack assets, Loadout look, int taps, long atMillis) {
        Pet pet = new Pet(PetTimings.DEFAULT, 0, 36_957);
        PetView view = new PetView(assets, pet, slot -> { }, () -> { });
        view.setLook(look);
        for (int i = 0; i < taps; i++) {
            pet.tap(atMillis);
        }
        pet.tick(atMillis);
        return view;
    }
}
