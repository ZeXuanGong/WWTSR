package com.wwtsr.view;

import com.wwtsr.assets.AssetPack;
import com.wwtsr.chest.Reward;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.Group;
import javafx.scene.transform.Scale;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Shows what a chest gave, floating above the pet: the {@link RewardEffect} plays, stays for a
 * moment and fades away. It lives in a little see-through window of its own, so it can sit
 * above the pet's window without making that window any bigger.
 */
public final class RewardPopup {

    /** How long the reward stays before it starts to fade. */
    private static final Duration STAY = Duration.millis(2_500);
    private static final Duration FADE = Duration.millis(500);
    /** The effect is drawn a little smaller than its full size. */
    private static final double SHRINK = 0.9;
    /** How far above the pet's top the effect's own top is, at 100% size. */
    private static final double ABOVE = 180;

    private final RewardEffect effect;
    private final Scale scale = new Scale(SHRINK, SHRINK, 0, 0);
    private final Popup popup = new Popup();
    private SequentialTransition playing;

    public RewardPopup(AssetPack assets) {
        effect = new RewardEffect(assets);
        effect.getTransforms().add(scale);
        popup.getContent().add(new Group(effect));
        popup.setAutoHide(false);
        popup.setHideOnEscape(false);
        // If the pet is at the top of the screen there is no room above: keep it on screen.
        popup.setAutoFix(true);
    }

    /**
     * Plays a reward above a point.
     *
     * @param owner    the pet's window
     * @param centreX  the screen x the effect is centred on
     * @param topY     the screen y of the pet's top
     * @param petScale the pet's size (1 is full size); the effect is sized to match
     * @param big      whether the text is the larger "new kind of thing" style
     */
    public void play(Window owner, double centreX, double topY, double petScale, Reward reward, String text, boolean big) {
        if (playing != null) {
            playing.stop();
        }
        double size = SHRINK * petScale;
        scale.setX(size);
        scale.setY(size);
        effect.setOpacity(1);
        effect.play(reward, text, big, false);
        popup.show(owner, centreX - RewardEffect.SIZE * size / 2, topY - ABOVE * petScale);

        FadeTransition fade = new FadeTransition(FADE, effect);
        fade.setToValue(0);
        playing = new SequentialTransition(new PauseTransition(STAY), fade);
        playing.setOnFinished(e -> hide());
        playing.play();
    }

    public void hide() {
        if (playing != null) {
            playing.stop();
        }
        effect.stop();
        popup.hide();
    }
}
