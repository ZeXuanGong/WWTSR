package com.wwtsr.panel;

import com.wwtsr.i18n.Messages;
import com.wwtsr.menu.PanelPage;
import java.util.EnumMap;
import java.util.Map;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * The panel: a window of its own, 1100 x 760 at full size. Down its left side runs a rail with
 * the logo, one button per page and, at the bottom, a button that folds the panel down to just
 * the rail. The page fills the rest, on paper with a faint pattern and a scalloped band along
 * the top; its close button sits in the top-right corner. The panel is dragged by the rail.
 */
public final class PanelWindow {

    private static final double HEIGHT = 756;
    private static final double RAIL_WIDTH = 243;
    private static final double RAIL_FOLDED = 101;
    private static final double CONTENT_WIDTH = 849;
    private static final double BUTTON_HEIGHT = 70;
    private static final double RADIUS = 50;
    private static final Duration TOAST_TIME = Duration.millis(2_000);

    /** Each page's sign on its button: a path drawn in a 24 x 24 box. */
    private static final Map<PanelPage, String> ICONS = Map.of(
            PanelPage.BAG, "M4 8h16v10a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z M8 8V6a4 4 0 0 1 8 0v2h-2V6a2 2 0 0 0-4 0v2z",
            PanelPage.ATLAS, "M6 3h12a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6z M3 6h2v12H3z",
            PanelPage.SYNTHESIS, "M3 9h18v3H3z M5 13h14v8H5z M8 4a2.5 2.5 0 0 1 4 2 2.5 2.5 0 0 1 4-2 2.5 2.5 0 0 1-2 4h-4a2.5 2.5 0 0 1-2-4z",
            PanelPage.SETTINGS, "M12 2l8.66 5v10L12 22l-8.66-5V7z M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z");
    private static final String FOLD_ICON = "M3 5h18v3H3z M3 16h18v3H3z M10 10.5h11v3H10z M3 12l5-3.5v7z";

    private static final double LOGO_WIDTH = 152;
    private static final double LOGO_HEIGHT = 82;
    private static final double LOGO_FOLDED = 60 / LOGO_WIDTH;

    private static Image paperPattern;
    private static Image paperBand;

    private final PanelHost host;
    private final Stage stage = new Stage(StageStyle.TRANSPARENT);
    private final HBox frame = new HBox();
    private final StackPane pageHolder = new StackPane();
    private final Label toast = new Label();
    private final PauseTransition toastTimer = new PauseTransition(TOAST_TIME);
    private final Map<PanelPage, Page> pages = new EnumMap<>(PanelPage.class);
    private final Map<PanelPage, HBox> navButtons = new EnumMap<>(PanelPage.class);
    private VBox rail;
    private Label logo;
    private Rectangle railCut;
    private Node content;
    private PanelPage current = PanelPage.BAG;
    private boolean folded;
    private double dragOffsetX;
    private double dragOffsetY;

    public PanelWindow(PanelHost host, Image icon) {
        this.host = host;
        frame.getStyleClass().add("panel");
        frame.setMinHeight(HEIGHT);
        frame.setMaxHeight(HEIGHT);
        // Never wider than the rail and the page: a folded rail makes the panel narrower.
        frame.setMaxWidth(Region.USE_PREF_SIZE);
        // Room below for the hard edge under the panel.
        StackPane root = new StackPane(frame);
        root.setPadding(new Insets(0, 0, 6, 0));
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(PanelWindow.class.getResource("panel.css").toExternalForm());
        stage.setScene(scene);
        stage.getIcons().add(icon);
        stage.setOnHidden(e -> leavePage());

        toast.getStyleClass().add("toast");
        toast.setVisible(false);
        toast.setMouseTransparent(true);
        toastTimer.setOnFinished(e -> toast.setVisible(false));

        build();
        // Whatever changes the player's things, the page on show follows.
        host.player().addListener(this::refresh);
    }

    /** Opens the panel on that page, bringing it to the front. */
    public void show(PanelPage page) {
        select(page);
        if (!stage.isShowing()) {
            stage.show();
            stage.centerOnScreen();
        }
        stage.setIconified(false);
        stage.toFront();
        stage.requestFocus();
    }

    public void close() {
        stage.hide();
    }

    /** Closes the panel if it is open; otherwise opens it on the page it last showed. */
    public void toggle() {
        if (stage.isShowing()) {
            close();
        } else {
            show(current);
        }
    }

    /** Builds everything again, in the language in use now. */
    public void rebuild() {
        leavePage();
        build();
    }

    /** Shows the page on show as things are now. */
    public void refresh() {
        if (stage.isShowing()) {
            page(current).refresh();
        }
    }

    /** A picture of the panel on that page, without showing it. For checking how it looks. */
    public WritableImage snapshot(PanelPage page) {
        return snapshot(page, false);
    }

    /** The same, with the rail folded or not. */
    public WritableImage snapshot(PanelPage page, boolean railFolded) {
        folded = railFolded;
        select(page);
        frame.applyCss();
        frame.autosize();
        frame.layout();
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return frame.snapshot(params, null);
    }

    // ---- building ----

    private void build() {
        Messages m = host.messages();
        stage.setTitle(m.get("app.name"));
        pages.clear();
        navButtons.clear();

        logo = new Label(m.get("app.name"));
        logo.getStyleClass().add("logo");
        logo.setMinSize(LOGO_WIDTH, LOGO_HEIGHT);
        logo.setMaxSize(LOGO_WIDTH, LOGO_HEIGHT);
        logo.setAlignment(Pos.CENTER);
        // In a group, so that the room the logo takes follows its scale.
        StackPane logoBox = new StackPane(new Group(logo));
        logoBox.setMinHeight(LOGO_HEIGHT);
        logoBox.setMaxHeight(LOGO_HEIGHT);
        rail = new VBox(logoBox);
        rail.getStyleClass().add("rail");
        for (PanelPage page : PanelPage.values()) {
            HBox button = navButton(ICONS.get(page), m.get("nav." + page.name()));
            button.setOnMouseClicked(e -> select(page));
            navButtons.put(page, button);
            rail.getChildren().add(button);
        }
        Region gap = new Region();
        VBox.setVgrow(gap, Priority.ALWAYS);
        HBox fold = navButton(FOLD_ICON, m.get("nav.collapse"));
        fold.getStyleClass().add("nav-collapse");
        fold.setOnMouseClicked(e -> {
            folded = !folded;
            // The page stays where it is on the screen: the rail grows or shrinks to its left.
            double right = stage.getX() + stage.getWidth();
            layout();
            if (stage.isShowing()) {
                stage.setX(right - stage.getWidth());
            }
        });
        rail.getChildren().addAll(gap, fold);
        // The rail is the handle the window is dragged by.
        rail.setOnMousePressed(e -> {
            dragOffsetX = stage.getX() - e.getScreenX();
            dragOffsetY = stage.getY() - e.getScreenY();
        });
        rail.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() + dragOffsetX);
            stage.setY(e.getScreenY() + dragOffsetY);
        });

        // Soft clouds in the rail's bottom-left corner, cut to the panel's rounded shape.
        Group clouds = new Group(cloud(34, HEIGHT - 20, 70), cloud(118, HEIGHT + 6, 62), cloud(-6, HEIGHT - 86, 46));
        Pane cloudLayer = new Pane(clouds);
        cloudLayer.setMouseTransparent(true);
        // The clouds never decide how wide the rail is.
        cloudLayer.setMinSize(0, 0);
        cloudLayer.setPrefSize(0, 0);
        railCut = new Rectangle(RAIL_WIDTH, HEIGHT - 4);
        railCut.setArcWidth(RADIUS * 2);
        railCut.setArcHeight(RADIUS * 2);
        cloudLayer.setClip(railCut);
        StackPane railStack = new StackPane(cloudLayer, rail);

        content = content();
        frame.getChildren().setAll(railStack, content);
        select(current);
    }

    private HBox navButton(String iconPath, String text) {
        Region icon = new Region();
        icon.getStyleClass().add("nav-icon");
        icon.setStyle("-fx-shape: \"" + iconPath + "\";");
        icon.setMinSize(30, 30);
        icon.setMaxSize(30, 30);
        StackPane iconBox = new StackPane(icon);
        iconBox.setMinSize(40, 54);
        iconBox.setMaxSize(40, 54);
        Label label = new Label(text);
        label.getStyleClass().add("nav-label");
        HBox button = new HBox(iconBox, label);
        button.getStyleClass().add("nav-button");
        button.setMinHeight(BUTTON_HEIGHT);
        button.setMaxHeight(BUTTON_HEIGHT);
        // Kept so folding can hide the words and leave the signs.
        button.getProperties().put("label", label);
        return button;
    }

    private static Circle cloud(double x, double y, double radius) {
        Circle cloud = new Circle(x, y, radius);
        cloud.getStyleClass().add("rail-cloud");
        return cloud;
    }

    /** The page area: sand-coloured edge, paper face with pattern and band, the page, the close button. */
    private Node content() {
        ensurePaper();
        Region face = new Region();
        face.getStyleClass().add("content-face");
        Region pattern = new Region();
        pattern.setBackground(new Background(new BackgroundImage(paperPattern, BackgroundRepeat.REPEAT,
                BackgroundRepeat.REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
        Region band = new Region();
        band.setBackground(new Background(new BackgroundImage(paperBand, BackgroundRepeat.REPEAT,
                BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
        band.setMaxHeight(43);
        StackPane.setAlignment(band, Pos.TOP_LEFT);
        // The paper stops 8 short of the left edge, showing a strip of sand.
        StackPane paper = new StackPane(face, pattern, band);
        StackPane.setMargin(paper, new Insets(0, 0, 0, 8));
        paper.setMouseTransparent(true);

        StackPane area = new StackPane(paper, pageHolder, closeButton(), toast);
        area.getStyleClass().add("content");
        area.setMinWidth(CONTENT_WIDTH);
        area.setMaxWidth(CONTENT_WIDTH);
        // Everything in the page area keeps inside its rounded corners.
        // (Its top-left corner is only slightly rounded; the other three are round.)
        Rectangle round = new Rectangle(CONTENT_WIDTH, HEIGHT - 4);
        round.setArcWidth(RADIUS * 2);
        round.setArcHeight(RADIUS * 2);
        Rectangle corner = new Rectangle(RADIUS * 2, RADIUS * 2);
        corner.setArcWidth(20);
        corner.setArcHeight(20);
        area.setClip(Shape.union(round, corner));
        StackPane.setAlignment(toast, Pos.CENTER);
        return area;
    }

    /** A rounded brown diamond with a pale cross, in the page area's top-right corner. */
    private Node closeButton() {
        Rectangle diamond = new Rectangle(-19, -19, 38, 38);
        diamond.setArcWidth(18);
        diamond.setArcHeight(18);
        diamond.setRotate(45);
        diamond.setFill(Color.web("#c88865"));
        diamond.setStroke(Color.web("#995e3f"));
        diamond.setStrokeWidth(2.5);
        Group cross = new Group();
        for (int sign : new int[] {-1, 1}) {
            Line stroke = new Line(-8, -8 * sign, 8, 8 * sign);
            stroke.setStroke(Color.web("#fff3dd"));
            stroke.setStrokeWidth(4);
            stroke.setStrokeLineCap(StrokeLineCap.ROUND);
            cross.getChildren().add(stroke);
        }
        StackPane button = new StackPane(new Group(diamond), cross);
        button.setMinSize(56, 61);
        button.setMaxSize(56, 61);
        button.setCursor(Cursor.HAND);
        button.setOnMouseClicked(e -> close());
        StackPane.setAlignment(button, Pos.TOP_RIGHT);
        StackPane.setMargin(button, new Insets(14, 16, 0, 0));
        return button;
    }

    /** Draws the paper's pattern and its top band once; every panel shares them. */
    private static void ensurePaper() {
        if (paperPattern != null) {
            return;
        }
        SnapshotParameters clear = new SnapshotParameters();
        clear.setFill(Color.TRANSPARENT);

        // A faint check of diamonds, 70 x 70.
        Canvas tile = new Canvas(70, 70);
        GraphicsContext g = tile.getGraphicsContext2D();
        g.setFill(Color.web("#fffaf0", 0.45));
        g.fillPolygon(new double[] {35, 70, 35, 0}, new double[] {0, 35, 70, 35}, 4);
        paperPattern = tile.snapshot(clear, null);

        // A band along the top with a scalloped lower edge and slanted hatching, 120 x 43.
        Canvas strip = new Canvas(120, 43);
        g = strip.getGraphicsContext2D();
        g.setFill(Color.web("#f6e6cc"));
        g.fillRect(0, 0, 120, 26);
        g.fillOval(-30, 8, 60, 34);
        g.fillOval(30, 8, 60, 34);
        g.fillOval(90, 8, 60, 34);
        g.setStroke(Color.web("#efdcbd"));
        g.setLineWidth(3);
        for (int x = -40; x < 140; x += 12) {
            g.strokeLine(x, 26, x + 22, 0);
        }
        paperBand = strip.snapshot(clear, null);
    }

    // ---- pages ----

    private Page page(PanelPage which) {
        return pages.computeIfAbsent(which, p -> switch (p) {
            case BAG -> new BagPage(host, this::showToast);
            case ATLAS -> new AtlasPage(host);
            case SYNTHESIS -> new SynthesisPage(host, this::showToast);
            case SETTINGS -> new SettingsPage(host, this::showToast);
        });
    }

    private void select(PanelPage which) {
        leavePage();
        current = which;
        // The settings page shows the settings as they were when it was built: build it fresh.
        if (which == PanelPage.SETTINGS) {
            pages.remove(which);
        }
        Page page = page(which);
        page.refresh();
        StackPane holder = new StackPane(page.node());
        // The bag starts higher than the other pages: its top row is the look and the presets.
        holder.setPadding(new Insets(which == PanelPage.BAG ? 60 : 90, 26, 32, 32));
        pageHolder.getChildren().setAll(holder);
        layout();
    }

    /**
     * Draws the rail full or folded, and fits the window to it. Folded, the rail is narrow:
     * a small logo and the buttons' signs without their words. The page beside it stays.
     */
    private void layout() {
        double width = folded ? RAIL_FOLDED : RAIL_WIDTH;
        rail.setMinWidth(width);
        rail.setMaxWidth(width);
        railCut.setWidth(width);
        // The logo is 152 wide at full size and 60 folded.
        logo.setScaleX(folded ? LOGO_FOLDED : 1);
        logo.setScaleY(folded ? LOGO_FOLDED : 1);
        for (Node child : rail.getChildren()) {
            if (child instanceof HBox button && button.getProperties().get("label") instanceof Label label) {
                label.setVisible(!folded);
                label.setManaged(!folded);
                button.setAlignment(folded ? Pos.CENTER : Pos.CENTER_LEFT);
            }
        }
        navButtons.forEach((page, button) -> {
            button.getStyleClass().remove("nav-button-active");
            if (page == current) {
                button.getStyleClass().add("nav-button-active");
            }
        });
        stage.sizeToScene();
    }

    /** Lets the page on show finish what it was in the middle of, before it goes away. */
    private void leavePage() {
        if (pages.get(PanelPage.SETTINGS) instanceof SettingsPage settings) {
            settings.cancelCapture();
        }
    }

    private void showToast(String text) {
        toast.setText(text);
        toast.setVisible(true);
        toastTimer.playFromStart();
    }
}
