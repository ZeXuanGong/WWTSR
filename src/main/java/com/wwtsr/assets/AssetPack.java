package com.wwtsr.assets;

import com.wwtsr.collect.Accessory;
import com.wwtsr.collect.Collectible;
import com.wwtsr.collect.Emote;
import com.wwtsr.collect.Role;
import com.wwtsr.pet.BodyPose;
import com.wwtsr.pet.HeadPose;
import javafx.scene.image.Image;

/**
 * The pictures everything is drawn from. A pet is a body layer and a head layer of the same
 * size, one picture per pose, with an accessory layer on top. Swap the implementation to
 * change how everything looks.
 */
public interface AssetPack {

    /** Width of every pet layer, in pixels at 100% size. */
    double width();

    /** Height of every pet layer, in pixels at 100% size. */
    double height();

    Image body(Role role, BodyPose pose);

    Image head(Role role, HeadPose pose);

    /** An accessory, drawn on a layer the size of the head picture so it sits in the right place. */
    Image accessory(Accessory accessory);

    /** An emote: a small square picture. */
    Image emote(Emote emote);

    /** A square picture of any collectible, for the bag and the atlas. */
    Image thumbnail(Collectible item);

    /** A square picture for the taskbar button and the tray icon. */
    Image icon();
}
