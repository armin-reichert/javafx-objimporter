package de.amr.meshviewer;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.Map;

public class PreviewArea extends StackPane {

    public static final String KEY_AUTO_ROTATE_HORIZONTALLY = "h";
    public static final String KEY_AUTO_ROTATE_VERTICALLY = "v";
    public static final String KEY_ROTATE_LEFT = "l";
    public static final String KEY_ROTATE_LEFT_LARGE = "L";
    public static final String KEY_ROTATE_RIGHT = "r";
    public static final String KEY_ROTATE_RIGHT_LARGE = "R";
    public static final String KEY_AUTOPLAY_TOGGLE = " ";
    public static final String KEY_WIREFRAME_TOGGLE = "w";

    public static final int ROTATE_SINGLE_STEP_DEGREES = 10;

    public static final int DEFAULT_ANGLE_X = 0;
    public static final int DEFAULT_ANGLE_Y = 0;

    public static final double AUTO_ROTATE_SPEED = 0.1;

    public static final int DEFAULT_ZOOM = -30;
    public static final int ZOOM_MIN = -10_000;
    public static final int ZOOM_MAX = -2;

    public static final double ZOOM_RATE_NORMAL = 0.02;
    public static final double ZOOM_RATE_LARGE = 0.5;

    public final ObjectProperty<DrawMode> drawMode = new SimpleObjectProperty<>(DrawMode.FILL);

    private final SubScene subScene;
    private final PerspectiveCamera cam;
    private final Translate camZoom = new Translate(0, 0, DEFAULT_ZOOM);
    private final Group world = new Group();
    private Group pivot; // currently shown mesh view (set) is contained in this group
    private double mouseOldX, mouseOldY;

    // Flip around x-axis (otherwise many objects are upside-down initially)
    private final Rotate flipYDirection = new Rotate(180, Rotate.X_AXIS);
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);

    // Preview Animation
    private Animation previewAutoRotateAnimation;
    private final Rotate autoRotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate autoRotateY = new Rotate(0, Rotate.Y_AXIS);
    private Point3D autoRotateAxis = Rotate.Y_AXIS; // horizontally be default

    private final FlashMessageOverlay flashMessageOverlay = new FlashMessageOverlay();

    public PreviewArea() {
        setId("preview");

        cam = new PerspectiveCamera(true);

        subScene = new SubScene(world, 400, 400, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(cam);
        subScene.focusedProperty().addListener((_, _, focussed) ->
            Logger.info("Subscene {}", focussed? "got focus" : "lost focus"));

        configureCamera();
        addLights();
        setKeyboardAndMouseHandlers();
        configureBackground();
        getChildren().setAll(subScene, flashMessageOverlay);

        // Make key events work as expected
        subScene.setFocusTraversable(true);
        flashMessageOverlay.setFocusTraversable(false);

        // Make mouse events work as expected
        setPickOnBounds(false);
        flashMessageOverlay.setMouseTransparent(true);
        flashMessageOverlay.setPickOnBounds(false);
        subScene.setPickOnBounds(true);
    }

    private void setKeyboardAndMouseHandlers() {
        subScene.setOnKeyPressed(e -> {
            boolean shift = e.isShiftDown();
            boolean control = e.isControlDown();
            boolean controlShift = control && shift;
            switch (e.getCode()) {
                case PLUS  -> {
                    int delta = controlShift ? 100 : shift ? 10 : 1;
                    zoomBy(delta);
                    e.consume();
                }
                case MINUS -> {
                    int delta = controlShift ? 100 : shift ? 10 : 1;
                    zoomBy(-delta);
                    e.consume();
                }
                case LEFT  -> {
                    rotateYBy(-1);
                    e.consume(); // do not deliver event to tab pane
                }
                case RIGHT -> {
                    rotateYBy(1);
                    e.consume(); // do not deliver event to tab pane
                }
                case UP    -> {
                    rotateXBy(-1);
                    e.consume(); // do not deliver event to tab pane
                }
                case DOWN  -> {
                    rotateXBy(1);
                    e.consume(); // do not deliver event to tab pane
                }
            }
        });

        subScene.setOnKeyTyped(e -> {
            onKeyTypedInPreview(e.getCharacter());
            e.consume();
        });

        subScene.setOnMouseClicked(e -> {
            Logger.info("Mouse clicked {}", e);
            if (e.getClickCount() == 2) {
                reset();
            }
            e.consume();
        });

        subScene.setOnMousePressed(e -> {
            Logger.info("Mouse pressed {}", e);
            mouseOldX = e.getSceneX();
            mouseOldY = e.getSceneY();
            e.consume();
        });

        subScene.setOnMouseDragged(e -> {
            Logger.info("Mouse dragged {}", e);
            double dx = e.getSceneX() - mouseOldX;
            double dy = e.getSceneY() - mouseOldY;

            boolean shift = e.isShiftDown();
            if (e.getButton() == MouseButton.PRIMARY) {
                if (shift) {
                    rotateXBy(-dx * 1.5);
                } else {
                    rotateYBy(-dy * 1.5);
                }
            }

            mouseOldX = e.getSceneX();
            mouseOldY = e.getSceneY();

            e.consume();
        });

        subScene.setOnScroll(e -> {
            final double rate = e.isShiftDown() ? ZOOM_RATE_LARGE : ZOOM_RATE_NORMAL;
            zoomBy(e.getDeltaY() * rate);
            Logger.info("Scroll event {}", e);
            e.consume();
        });
    }

    private void configureBackground() {
        final var darkGradient = new LinearGradient(
            0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#222")),
            new Stop(1, Color.web("#555"))
        );
        final var brightGradient = new LinearGradient(
            0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#666")),
            new Stop(1, Color.web("#aaa"))
        );
        backgroundProperty().bind(subScene.focusedProperty()
            .map(focussed -> focussed? brightGradient : darkGradient)
            .map(Background::fill)
        );
    }

    public void assignFocusToSubScene() {
        Logger.info("Trying to assign focus to preview subscene");
        Platform.runLater(subScene::requestFocus);
    }

    public SubScene subScene() {
        return subScene;
    }

    public void flash(String message) {
        flashMessageOverlay.showMessage(message);
    }

    public void displayMeshViews(Map<String, MeshView> meshViews) {
        pivot = new Group();
        meshViews.values().forEach(meshView -> {
            meshView.setCullFace(CullFace.NONE);
            meshView.drawModeProperty().bind(drawMode);
            pivot.getChildren().add(meshView);
        });
        pivot.getTransforms().addAll(flipYDirection, rotateX, rotateY, autoRotateX, autoRotateY);
        center(pivot);
        world.getChildren().setAll(pivot);
        assignFocusToSubScene();
    }

    private void center(Node node) {
        final Bounds b = node.getBoundsInLocal();
        node.getTransforms().add(new Translate(-b.getCenterX(), -b.getCenterY(), -b.getCenterZ()));
    }

    public void reset() {
        rotateX.setAngle(DEFAULT_ANGLE_X);
        rotateY.setAngle(DEFAULT_ANGLE_Y);
        autoRotateX.setAngle(DEFAULT_ANGLE_X);
        autoRotateY.setAngle(DEFAULT_ANGLE_Y);
        camZoom.setZ(DEFAULT_ZOOM);
    }

    private void configureCamera() {
        cam.getTransforms().addAll(camZoom);
        cam.setNearClip(0.1);
        cam.setFarClip(10_000);
    }

    private void addLights() {
        final var ambient = new AmbientLight(Color.color(0.3, 0.3, 0.3));

        final var keyLight = new PointLight(Color.WHITE);
        keyLight.setTranslateX(200);
        keyLight.setTranslateY(-200);
        keyLight.setTranslateZ(-300);

        final var fillLight = new PointLight(Color.color(0.6, 0.6, 0.8));
        fillLight.setTranslateX(-200);
        fillLight.setTranslateY(200);
        fillLight.setTranslateZ(-300);

        world.getChildren().addAll(ambient, keyLight, fillLight);
    }

    public void initSampleModel(SampleModel sample) throws IOException {
        final SampleState initial = sample.initialState();

        camZoom.setZ(initial.zoom());

        if (initial.rotateX() != 0) {
            pivot.getTransforms().addLast(new Rotate(initial.rotateX(), Rotate.X_AXIS));
        }
        if (initial.rotateY() != 0) {
            pivot.getTransforms().addLast(new Rotate(initial.rotateY(), Rotate.Y_AXIS));
        }
        if (initial.rotateZ() != 0) {
            pivot.getTransforms().addLast(new Rotate(initial.rotateZ(), Rotate.Z_AXIS));
        }

        if (sample.initialState().autoRotate()) {
            autoRotateAnimation().play();
        } else {
            autoRotateAnimation().stop();
        }
    }

    private Animation autoRotateAnimation() {
        if (previewAutoRotateAnimation == null) {
            createAutoRotateAnimation();
        }
        return previewAutoRotateAnimation;
    }

    private void createAutoRotateAnimation() {
        previewAutoRotateAnimation = new Timeline(
            new KeyFrame(Duration.millis(16), _ -> {
                if (autoRotateAxis == Rotate.X_AXIS) {
                    autoRotateX.setAngle(autoRotateX.getAngle() + AUTO_ROTATE_SPEED);
                } else {
                    autoRotateY.setAngle(autoRotateY.getAngle() - AUTO_ROTATE_SPEED);
                }
            }) // ~60 FPS
        );
        previewAutoRotateAnimation.setCycleCount(Animation.INDEFINITE);
    }

    private void onKeyTypedInPreview(String key) {
        if (KEY_AUTO_ROTATE_HORIZONTALLY.equals(key)) {
            autoRotateAxis = Rotate.Y_AXIS;
            flash("Auto-Rotate horizontally");
        }
        else if (KEY_AUTO_ROTATE_VERTICALLY.equals(key)) {
            autoRotateAxis = Rotate.X_AXIS;
            flash("Auto-Rotate vertically");
        }
        else if (KEY_ROTATE_LEFT.equals(key)) {
            rotateYBy(ROTATE_SINGLE_STEP_DEGREES);
        }
        else if (KEY_ROTATE_LEFT_LARGE.equals(key)) {
            rotateYBy(3 * ROTATE_SINGLE_STEP_DEGREES);
        }
        else if (KEY_ROTATE_RIGHT.equals(key)) {
            rotateYBy(-ROTATE_SINGLE_STEP_DEGREES);
        }
        else if (KEY_ROTATE_RIGHT_LARGE.equals(key)) {
            rotateYBy(-3 * ROTATE_SINGLE_STEP_DEGREES);
        }
        else if (KEY_WIREFRAME_TOGGLE.equals(key)) {
            final DrawMode mode = drawMode.get() == DrawMode.FILL ? DrawMode.LINE : DrawMode.FILL;
            drawMode.set(mode);
        }
        else if (KEY_AUTOPLAY_TOGGLE.equals(key)) {
            toggleAutoRotateWithFlashMessage();
        }
    }

    private void zoomBy(double delta) {
        double z = Math.clamp(camZoom.getZ() + delta, ZOOM_MIN, ZOOM_MAX);
        camZoom.setZ(z);
        Logger.info("Zoom: " + z);
    }

    private void rotateXBy(double delta) {
        rotateX.setAngle((rotateX.getAngle() + delta) % 360);
    }

    private void rotateYBy(double delta) {
        rotateY.setAngle((rotateY.getAngle() + delta) % 360);
    }

    public void startAutoRotate() {
        autoRotateAnimation().play();
        Logger.info("Auto-Rotate started");
    }

    public void pauseAutoRotate() {
        autoRotateAnimation().pause();
        Logger.info("Auto-Rotate paused");
    }

    private void toggleAutoRotateWithFlashMessage() {
        if (autoRotateAnimation().getStatus() == Animation.Status.RUNNING) {
            pauseAutoRotate();
            flash("Auto-Rotate paused");
        } else {
            startAutoRotate();
            flash("Auto-Rotate started");
        }
    }
}
