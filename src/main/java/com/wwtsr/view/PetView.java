package com.wwtsr.view;

import com.wwtsr.assets.AssetPack;
import com.wwtsr.chest.ChestSlot;
import com.wwtsr.chest.Reward;
import com.wwtsr.collect.Emote;
import com.wwtsr.collect.Loadout;
import com.wwtsr.collect.Role;
import com.wwtsr.pet.Pet;
import com.wwtsr.pet.PetListener;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

/**
 * Draws a {@link Pet} and everything around it, laid out on a fixed "dock":
 *
 * <pre>
 *   272 wide: 24 of room on the left (for the emote fan), the 200-wide pet, 48 on the right
 *             (for the chests)
 *   331 tall: 120 of room above the pet (emote bubbles float up into it, the toast sits in
 *             it), the 200-tall pet, and the counter bar overlapping the pet's bottom by 21
 * </pre>
 *
 * The whole dock can be scaled; its size follows the scale, so the window fits it. Everything
 * outside the drawings is see-through and lets the mouse through.
 */
public final class PetView extends Group implements PetListener {

    public static final double DOCK_WIDTH = 272;
    public static final double DOCK_HEIGHT = 331;
    /** The point that stays put when the pet changes size: the middle of the pet's bottom edge. */
    public static final double PIVOT_X = 124;
    public static final double PIVOT_Y = 331;
    /** How far the pet itself (picture and counter bar) reaches above the pivot. */
    public static final double CONTENT_HEIGHT = 211;

    private static final double PAD_LEFT = 24;
    private static final double HEADROOM = 120;
    /** Where the counter bar and the chests sit, relative to the pet picture's top-left corner. */
    private static final double COUNTER_X = 5;
    private static final double COUNTER_Y = 179;
    private static final double[] FIRST_CHEST = {174, 143};
    private static final double[] SECOND_CHEST = {184, 95};

    /** How long the mouse rests on the pet before it turns faint. */
    private static final Duration HOVER_DELAY = Duration.millis(300);
    private static final double FAINT = 0.4;

    // Emote bubbles.
    private static final double BUBBLE_SIZE = 80;
    private static final double BUBBLE_RISE = 102;
    private static final Duration BUBBLE_TIME = Duration.millis(1_000);
    /** Bubbles take these places in turn, so several in a row spread out rather than stack. */
    private static final double[][] BUBBLE_PLACES = {
            {-30, 10}, {4, -6}, {28, 12}, {-14, 2}, {36, 6}, {-38, 16}, {16, -10}, {-6, 8}};
    private static final double BUBBLE_JITTER_X = 5;
    private static final double BUBBLE_JITTER_Y = 4;
    /** Every bubble starts this much higher than the pet's middle. */
    private static final double BUBBLE_LIFT = 50;

    /** A reward appears above the pet, centred this far in from the picture's right edge. */
    private static final double REWARD_FROM_RIGHT = 90;
    private static final Duration TOAST_TIME = Duration.millis(2_500);
    private static final double TOAST_GAP = 12;

    private final AssetPack assets;
    private final ImageView body = new ImageView();
    private final ImageView head = new ImageView();
    private final ImageView worn = new ImageView();
    private final CounterBar counter;
    private final RewardPopup reward;
    private final EmoteFan fan;
    private final StackPane figure;
    private final Pane bubbles = new Pane();
    private final Label toast = new Label();
    private final Map<ChestSlot, ChestButton> chests = new EnumMap<>(ChestSlot.class);
    private final Scale scale = new Scale(1, 1, 0, 0);
    private final Pane dock = new Pane();
    private final PauseTransition hoverTimer = new PauseTransition(HOVER_DELAY);
    private final PauseTransition toastTimer = new PauseTransition(TOAST_TIME);
    private final Pet pet;
    private Role role = Role.STARTER;
    private boolean fadeOnHover;
    private int nextBubblePlace;

    /**
     * @param onChestOpened a chest has been clicked open
     * @param onMenu        a click on the counter bar's three-line button
     */
    public PetView(AssetPack assets, Pet pet, Consumer<ChestSlot> onChestOpened, Runnable onMenu) {
        this.assets = assets;
        this.pet = pet;
        this.reward = new RewardPopup(assets);
        this.fan = new EmoteFan(assets, this::showEmote);
        this.counter = new CounterBar(onMenu);

        dock.setMinSize(DOCK_WIDTH, DOCK_HEIGHT);
        dock.setPrefSize(DOCK_WIDTH, DOCK_HEIGHT);
        dock.setMaxSize(DOCK_WIDTH, DOCK_HEIGHT);
        dock.setBackground(Background.EMPTY);
        dock.setPickOnBounds(false);

        figure = new StackPane(body, head, worn);
        figure.setMinSize(assets.width(), assets.height());
        figure.setMaxSize(assets.width(), assets.height());
        figure.setPrefSize(assets.width(), assets.height());
        at(figure, 0, 0);
        bubbles.setMouseTransparent(true);
        at(bubbles, 0, 0);
        at(fan, 0, 0);
        at(counter, COUNTER_X, COUNTER_Y);
        dock.getChildren().addAll(figure, counter);

        double[][] places = {FIRST_CHEST, SECOND_CHEST};
        for (ChestSlot slot : ChestSlot.values()) {
            ChestButton button = new ChestButton(() -> onChestOpened.accept(slot));
            chests.put(slot, button);
            at(button, places[slot.ordinal()][0], places[slot.ordinal()][1]);
            dock.getChildren().add(button);
        }

        toast.setTextFill(Color.WHITE);
        toast.setFont(Font.font("System", 20));
        toast.setPadding(new Insets(12, 20, 12, 20));
        toast.setBackground(new Background(new BackgroundFill(Color.rgb(0, 0, 0, 0.8), new CornerRadii(12),
                Insets.EMPTY)));
        toast.setMouseTransparent(true);
        toast.setVisible(false);
        // Kept centred over the pet and just above it, whatever its text.
        toast.layoutBoundsProperty().addListener((o, was, bounds) -> placeToast());
        toastTimer.setOnFinished(e -> toast.setVisible(false));

        dock.getChildren().addAll(fan, bubbles, toast);
        dock.getTransforms().add(scale);
        getChildren().add(dock);

        hoverTimer.setOnFinished(e -> {
            if (fadeOnHover && isHover()) {
                dock.setOpacity(FAINT);
            }
        });
        hoverProperty().addListener((o, was, hovering) -> hoverChanged(hovering));

        petChanged(pet);
        pet.addListener(this);
    }

    /** 1 is full size. */
    public void setScale(double factor) {
        scale.setX(factor);
        scale.setY(factor);
    }

    public double getScale() {
        return scale.getX();
    }

    /** Whether the pet turns faint while the mouse rests on it. */
    public void setFadeOnHover(boolean on) {
        fadeOnHover = on;
        if (!on) {
            hoverTimer.stop();
            dock.setOpacity(1);
        }
    }

    /** Shows who the pet is, what it wears and which emotes it has ready. */
    public void setLook(Loadout look) {
        role = look.role();
        worn.setImage(look.accessory().map(assets::accessory).orElse(null));
        fan.setEmotes(look.emotes());
        petChanged(pet);
    }

    /** Opens the fan of emotes if it is closed, and closes it if it is open. */
    public void toggleEmotes() {
        if (!fan.isOpen()) {
            // At the screen's left edge the fan moves right, so none of it is cut off.
            Point2D corner = figure.localToScreen(0, 0);
            double petScale = Math.max(0.01, scale.getX());
            fan.setInset(corner == null ? 0 : Math.ceil(21 + 14 - corner.getX() / petScale));
        }
        fan.setOpen(!fan.isOpen());
    }

    public void closeEmotes() {
        fan.setOpen(false);
    }

    /** Opens or closes the fan at once, without the fly-out. For pictures of the pet. */
    public void setEmotesOpenNow(boolean open) {
        fan.setOpenNow(open);
    }

    /**
     * @param visible         whether the chest is drawn at all
     * @param canOpen         whether it can be opened now
     * @param remainingMillis the wait left when it can't
     * @param remainingPart   how much of the whole wait is left, 0–1
     */
    public void showChest(ChestSlot slot, boolean visible, boolean canOpen, long remainingMillis, double remainingPart) {
        chests.get(slot).show(visible, canOpen, remainingMillis, remainingPart);
    }

    /**
     * Plays a reward above the pet. Nothing happens while the pet isn't in a window on screen.
     *
     * @param big whether the text is the larger "new kind of thing" style
     */
    public void showReward(Reward what, String text, boolean big) {
        if (getScene() == null || getScene().getWindow() == null || !getScene().getWindow().isShowing()) {
            return;
        }
        Point2D above = figure.localToScreen(assets.width() - REWARD_FROM_RIGHT, 0);
        if (above != null) {
            reward.play(getScene().getWindow(), above.getX(), above.getY(), scale.getX(), what, text, big);
        }
    }

    /**
     * Pops an emote up from the pet: it floats upwards, growing a little, and fades away near
     * the end. Each bubble starts at the next of a fixed round of places, nudged a little.
     */
    public void showEmote(Emote emote) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double[] place = BUBBLE_PLACES[nextBubblePlace++ % BUBBLE_PLACES.length];
        double offsetX = Math.round(place[0] + random.nextDouble(-BUBBLE_JITTER_X, BUBBLE_JITTER_X));
        double offsetY = Math.round(place[1] + random.nextDouble(-BUBBLE_JITTER_Y, BUBBLE_JITTER_Y) - BUBBLE_LIFT);

        ImageView bubble = new ImageView(assets.emote(emote));
        bubble.setFitWidth(BUBBLE_SIZE);
        bubble.setFitHeight(BUBBLE_SIZE);
        // Its anchor is the pet's middle plus the offset; the bubble hangs from a point half-way
        // across and 30% down itself.
        bubble.setLayoutX(assets.width() / 2 + offsetX - BUBBLE_SIZE * 0.5);
        bubble.setLayoutY(assets.height() / 2 + offsetY - BUBBLE_SIZE * 0.3);
        bubbles.getChildren().add(bubble);

        TranslateTransition rise = new TranslateTransition(BUBBLE_TIME, bubble);
        rise.setByY(-BUBBLE_RISE);
        ScaleTransition grow = new ScaleTransition(BUBBLE_TIME, bubble);
        grow.setToX(1.08);
        grow.setToY(1.08);
        // Solid for 70% of the way, then gone.
        FadeTransition fade = new FadeTransition(BUBBLE_TIME.multiply(0.3), bubble);
        fade.setDelay(BUBBLE_TIME.multiply(0.7));
        fade.setToValue(0);
        ParallelTransition all = new ParallelTransition(rise, grow, fade);
        all.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        all.setOnFinished(e -> bubbles.getChildren().remove(bubble));
        all.play();
    }

    /** Shows a line of text just above the pet for a moment. */
    public void showToast(String text) {
        toast.setText(text);
        toast.setVisible(true);
        placeToast();
        toastTimer.playFromStart();
    }

    /** Called on the JavaFX thread: the pet only changes there. */
    @Override
    public void petChanged(Pet changed) {
        body.setImage(assets.body(role, changed.bodyPose()));
        head.setImage(assets.head(role, changed.headPose()));
        counter.show(changed.tapCount());
    }

    /** Puts a node at a place given relative to the pet picture's top-left corner. */
    private static void at(javafx.scene.Node node, double x, double y) {
        node.setLayoutX(PAD_LEFT + x);
        node.setLayoutY(HEADROOM + y);
    }

    private void placeToast() {
        double width = toast.getLayoutBounds().getWidth();
        double height = toast.getLayoutBounds().getHeight();
        toast.setLayoutX(PAD_LEFT + assets.width() / 2 - width / 2);
        toast.setLayoutY(HEADROOM - height - TOAST_GAP);
    }

    private void hoverChanged(boolean hovering) {
        if (hovering) {
            if (fadeOnHover) {
                hoverTimer.playFromStart();
            }
        } else {
            hoverTimer.stop();
            dock.setOpacity(1);
        }
    }
}
