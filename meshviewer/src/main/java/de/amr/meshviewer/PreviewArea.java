/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

import de.amr.meshviewer.info.SampleInfo;
import de.amr.meshviewer.meshtree.InnerTreeNode;
import de.amr.meshviewer.meshtree.MeshSelection;
import de.amr.meshviewer.meshtree.MeshTreeView;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.*;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import org.tinylog.Logger;

import java.util.Collection;
import java.util.Set;

public class PreviewArea extends StackPane {

    public static final Paint SKY_GRADIENT = new LinearGradient(
        0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
        new Stop(0.0, Color.web("#3A8DFF")),  // deep zenith blue
        new Stop(0.5, Color.web("#6BB6FF")),  // mid-sky
        new Stop(1.0, Color.web("#A7D8FF"))   // pale horizon blue
    );

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

    public static final double DEFAULT_ZOOM = -30;
    public static final double ZOOM_MIN = -10_000;
    public static final double ZOOM_MAX = -0.1;

    public static final double ZOOM_RATE_NORMAL = 0.1;
    public static final double ZOOM_RATE_LARGE  = 1.0;
    public static final double ZOOM_RATE_HUGE   = 10.0;

    // Flip around x-axis (otherwise many objects are upside-down initially)
    private static final Rotate FLIP_Y_DIRECTION = new Rotate(180, Rotate.X_AXIS);

    public final ObjectProperty<DrawMode> drawMode = new SimpleObjectProperty<>(DrawMode.FILL);

    private final Group meshesPivot = new Group();
    private final PerspectiveCamera cam = new PerspectiveCamera(true);
    private final Group previewGroup = new Group();
    private final Translate camZoom = new Translate(0, 0, DEFAULT_ZOOM);
    private final SubScene subScene;

    private double mouseOldX, mouseOldY;

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

        subScene = new SubScene(previewGroup, 400, 400, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(cam);
        subScene.focusedProperty().addListener((_, _, focussed) ->
            Logger.info("Subscene {}", focussed? "got focus" : "lost focus"));

        configureCamera();
        addLights();
        setKeyboardAndMouseHandlers();
        setBackground(Background.fill(SKY_GRADIENT));
        getChildren().setAll(subScene, flashMessageOverlay);

        // Make key events work as expected
        subScene.setFocusTraversable(true);
        flashMessageOverlay.setFocusTraversable(false);

        // Make mouse events work as expected
        setPickOnBounds(false);
        flashMessageOverlay.setMouseTransparent(true);
        flashMessageOverlay.setPickOnBounds(false);
        subScene.setPickOnBounds(true);

        addNoFocusWarningHint();

        previewGroup.getTransforms().add(FLIP_Y_DIRECTION);
    }

    private void addNoFocusWarningHint() {
        final Label noFocusWarning = new Label("Click to focus!");
        noFocusWarning.setMouseTransparent(true);
        noFocusWarning.setFocusTraversable(false);
        noFocusWarning.setId("noFocusWarning");
        noFocusWarning.visibleProperty().bind(subScene.focusedProperty().not());
        StackPane.setAlignment(noFocusWarning, Pos.CENTER);
        getChildren().add(noFocusWarning);
    }

    public void selectDisplayedMeshViews(Collection<MeshView> all, Set<MeshView> displayed) {

        all.forEach(meshView -> {
            meshView.setCullFace(CullFace.NONE);
            meshView.setVisible(true); // such that bounds computation takes it into account
        });
        meshesPivot.getChildren().setAll(all);

        final Bounds bounds = meshesPivot.getBoundsInLocal();
        final Translate center = new Translate(-bounds.getCenterX(), -bounds.getCenterY(), -bounds.getCenterZ());

        meshesPivot.getTransforms().setAll(center, rotateX, rotateY, autoRotateX, autoRotateY);

        all.forEach(meshView -> {
            meshView.drawModeProperty().bind(drawMode);
            meshView.setVisible(displayed.contains(meshView));
        });

        previewGroup.getChildren().setAll(meshesPivot);
        assignFocusToSubScene();
    }

    public void reset() {
        rotateX.setAngle(DEFAULT_ANGLE_X);
        rotateY.setAngle(DEFAULT_ANGLE_Y);
        autoRotateX.setAngle(DEFAULT_ANGLE_X);
        autoRotateY.setAngle(DEFAULT_ANGLE_Y);
        camZoom.setZ(DEFAULT_ZOOM);
    }

    public void assignFocusToSubScene() {
        Logger.trace("Trying to assign focus to preview subscene");
        Platform.runLater(subScene::requestFocus);
    }

    public SubScene subScene() {
        return subScene;
    }

    public void flash(String message) {
        flashMessageOverlay.showMessage(message);
    }

    // private

    private void setKeyboardAndMouseHandlers() {
        subScene.setOnKeyPressed(e -> {
            boolean shift = e.isShiftDown(), control = e.isControlDown(), controlShift = control && shift;
            switch (e.getCode()) {
                case PLUS -> {
                    final double rate = controlShift ? ZOOM_RATE_HUGE : shift ? ZOOM_RATE_LARGE : ZOOM_RATE_NORMAL;
                    zoomBy(rate);
                    e.consume();
                }
                case MINUS -> {
                    final double rate = controlShift ? ZOOM_RATE_HUGE : shift ? ZOOM_RATE_LARGE : ZOOM_RATE_NORMAL;
                    zoomBy(-rate);
                    e.consume();
                }
                case LEFT -> {
                    rotateYBy(-1);
                    e.consume(); // do not deliver event to tab pane
                }
                case RIGHT -> {
                    rotateYBy(1);
                    e.consume(); // do not deliver event to tab pane
                }
                case UP -> {
                    rotateXBy(-1);
                    e.consume(); // do not deliver event to tab pane
                }
                case DOWN -> {
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
            Logger.trace("Mouse clicked {}", e);
            if (e.getClickCount() == 2) {
                reset();
            }
            assignFocusToSubScene();
            e.consume();
        });

        subScene.setOnMousePressed(e -> {
            Logger.trace("Mouse pressed {}", e);
            mouseOldX = e.getSceneX();
            mouseOldY = e.getSceneY();
            e.consume();
        });

        subScene.setOnMouseDragged(e -> {
            Logger.trace("Mouse dragged {}", e);
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
            Logger.trace("Scroll event {}", e);
            boolean control = e.isControlDown();
            // Note: SHIFT + scroll is interpreted as horizontal scroll and deltaY is 0 in this case!
            double rate = control ? ZOOM_RATE_LARGE : ZOOM_RATE_NORMAL;
            double dy = e.getDeltaY() / 40.0; // normalize
            Logger.info("delta={}", dy);
            zoomBy(dy * rate);
        });
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

        previewGroup.getChildren().addAll(ambient, keyLight, fillLight);
    }

    public void initSampleModel(MeshTreeView tree, SampleInfo sample) {
        final SampleInitSettings settings = sample.initSettings();

        camZoom.setZ(settings.zoom());

        if (settings.rotateX() != 0) {
            meshesPivot.getTransforms().addLast(new Rotate(settings.rotateX(), Rotate.X_AXIS));
        }
        if (settings.rotateY() != 0) {
            meshesPivot.getTransforms().addLast(new Rotate(settings.rotateY(), Rotate.Y_AXIS));
        }
        if (settings.rotateZ() != 0) {
            meshesPivot.getTransforms().addLast(new Rotate(settings.rotateZ(), Rotate.Z_AXIS));
        }

        tree.getRoot().getChildren().forEach(node -> node.setExpanded(false));
        switch (settings.initialMeshSelection()) {
            case MeshSelection.ALL_OBJECTS -> {
                tree.selectAllMeshesFromCategory(InnerTreeNode.NodeCategory.MeshesByObjects);
                tree.getRoot().getChildren().getFirst().setExpanded(true);
            }
            case MeshSelection.ALL_GROUPS -> {
                tree.selectAllMeshesFromCategory(InnerTreeNode.NodeCategory.MeshesByGroups);
                tree.getRoot().getChildren().get(1).setExpanded(true);
            }
            case MeshSelection.ALL_MATERIALS -> {
                tree.selectAllMeshesFromCategory(InnerTreeNode.NodeCategory.MeshesByMaterials);
                tree.getRoot().getChildren().getLast().setExpanded(true);
            }
        }

        drawMode.set(settings.wireframe() ? DrawMode.LINE : DrawMode.FILL);
        if (settings.autorotate()) {
            autoRotateAnimation().playFromStart();
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
