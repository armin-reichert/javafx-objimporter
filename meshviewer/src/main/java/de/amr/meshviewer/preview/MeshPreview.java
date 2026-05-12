/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.preview;

import de.amr.meshviewer.FlashMessageOverlay;
import de.amr.meshviewer.SampleInitSettings;
import de.amr.meshviewer.info.SampleInfo;
import de.amr.meshviewer.meshtree.InnerTreeNode;
import de.amr.meshviewer.meshtree.MeshSelection;
import de.amr.meshviewer.meshtree.MeshTreeView;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MeshPreview extends StackPane {

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

    public static final double MOVE_DIST = 0.25;

    public final ObjectProperty<DrawMode> drawMode = new SimpleObjectProperty<>(DrawMode.FILL);
    public final BooleanProperty floorVisible = new SimpleBooleanProperty(false);
    public final BooleanProperty boundingBoxesVisible = new SimpleBooleanProperty(false);

    private final Group meshesGroup = new Group();
    private Group floorGroup;

    private final SubScene subScene;

    private double mouseOldX, mouseOldY;

    // Camera transforms
    private final Translate camTranslate = new Translate(0, 0, DEFAULT_ZOOM);

    // Transforms
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);


    // Preview Animation
    private Animation previewAutoRotateAnimation;
    private final Rotate autoRotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate autoRotateY = new Rotate(0, Rotate.Y_AXIS);
    private Point3D autoRotateAxis = Rotate.Y_AXIS; // horizontally be default

    private final FlashMessageOverlay flashMessageOverlay;

    public MeshPreview() {
        setId("preview");

        final PerspectiveCamera cam = new PerspectiveCamera(true);
        cam.setNearClip(0.1);
        cam.setFarClip(10_000);

        final Group cameraView = new Group(cam);
        cameraView.getTransforms().addAll(new Rotate(180, Rotate.X_AXIS), camTranslate);

        final Group top = new Group(meshesGroup, cameraView);

        subScene = new SubScene(top, 400, 400, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(cam);
        subScene.focusedProperty().addListener((_, _, focussed) ->
            Logger.info("Subscene {}", focussed? "got focus" : "lost focus"));

        flashMessageOverlay = new FlashMessageOverlay();
        flashMessageOverlay.setFocusTraversable(false);
        flashMessageOverlay.setMouseTransparent(true);
        flashMessageOverlay.setPickOnBounds(false);

        setKeyboardAndMouseHandlers();
        setBackground(Background.fill(SKY_GRADIENT));
        getChildren().addAll(subScene, flashMessageOverlay);

        // Make key events work as expected
        subScene.setFocusTraversable(true);

        // Make mouse events work as expected
        setPickOnBounds(false);
        subScene.setPickOnBounds(true);

        addNoFocusWarningHint();
    }

    private static Group createFloorGroup(double width, double height) {
        final Group g = new Group();

        final double size = Math.max(width, height);
        final double ft = 0.005;

        final PhongMaterial red = new PhongMaterial(Color.RED);
        final PhongMaterial blue = new PhongMaterial(Color.BLUE);
        final PhongMaterial white = new PhongMaterial(Color.WHITE);

        final Box floor = new Box(size, ft, size);
        final PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.rgb(40, 40, 40, 0.6));
        material.setSpecularColor(Color.TRANSPARENT);
        floor.setMaterial(material);

        final Box xAxis = new Box(size, ft, ft);
        xAxis.setTranslateY(.5 * ft);
        xAxis.setMaterial(red);

        final Box zAxis = new Box(ft, ft, size);
        zAxis.setTranslateY(.5 * ft);
        zAxis.setMaterial(blue);

        g.getChildren().addAll(xAxis, zAxis);

        final double markerRadius = 0.02;
        final Sphere originMarker = new Sphere(2 * markerRadius);
        originMarker.setMaterial(white);
        g.getChildren().add(originMarker);

        for (int i = 1; i <= (int) (0.5 * size); ++i) {
            final int scale = i % 10 == 0 ? 3 : 1;

            final Sphere markerX = new Sphere(scale * markerRadius);
            markerX.setMaterial(red);
            markerX.setTranslateX(i);
            g.getChildren().add(markerX);

            final Sphere markerZ = new Sphere(scale * markerRadius);
            markerZ.setMaterial(blue);
            markerZ.setTranslateZ(i);
            g.getChildren().add(markerZ);
        }
        
        g.getChildren().add(floor);

        return g;
    }

    public void flash(String message) {
        flashMessageOverlay.showMessage(message);
    }

    public void selectDisplayedMeshViews(Collection<MeshView> allMeshViews, Set<MeshView> displayedMeshViews) {
        meshesGroup.getChildren().clear();

        if (displayedMeshViews.isEmpty()) {
            assignFocusToSubScene();
            return;
        }

        allMeshViews.forEach(meshView -> {
            meshView.setVisible(true);
            meshView.setCullFace(CullFace.NONE);
            meshView.drawModeProperty().bind(drawMode);
        });

        // Add bounding boxes to displayed mesh views
        final List<Box> boundingBoxes = new ArrayList<>();
        for (MeshView meshView : allMeshViews) {
            final Box boundingBox = createBoundingBox(meshView, Color.RED);
            boundingBoxes.add(boundingBox);
            meshesGroup.getChildren().add(new Group(boundingBox, meshView));
        }

        // Important: Floor has to be added *last*!
        if (floorGroup != null) {
            floorGroup.visibleProperty().unbind();
        }

        // Compute area of projection to xy-plane for floor size computation
        final Rectangle2D projection = computeProjection(boundingBoxes);
        floorGroup = createFloorGroup(projection.getWidth(), projection.getHeight());
        floorGroup.visibleProperty().bindBidirectional(floorVisible);

        meshesGroup.getChildren().add(floorGroup);
        meshesGroup.getTransforms().setAll(rotateX, rotateY, autoRotateX, autoRotateY);

        // Only show those in displayedMeshViews set
        allMeshViews.forEach(meshView -> {
            meshView.setVisible(displayedMeshViews.contains(meshView));
        });

        assignFocusToSubScene();
    }

    private Box createBoundingBox(MeshView meshView, Color color) {
        Bounds b = meshView.getBoundsInLocal(); // local, not parent

        Box box = new Box(b.getWidth(), b.getHeight(), b.getDepth());
        box.setDrawMode(DrawMode.LINE);
        box.setMaterial(new PhongMaterial(color));
        box.visibleProperty().bind(boundingBoxesVisible);

        box.setTranslateX(b.getCenterX());
        box.setTranslateY(b.getCenterY());
        box.setTranslateZ(b.getCenterZ());

        return box;
    }

    private Rectangle2D computeProjection(Collection<Box> boxes) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (Box box : boxes) {
            final Bounds b = box.getBoundsInLocal();
            minX = Math.min(minX, b.getMinX());
            maxX = Math.max(maxX, b.getMaxX());
            minY = Math.min(minY, b.getMinY());
            maxY = Math.max(maxY, b.getMaxY());
        }
        return new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
    }

    public void reset() {
        rotateX.setAngle(DEFAULT_ANGLE_X);
        rotateY.setAngle(DEFAULT_ANGLE_Y);
        autoRotateX.setAngle(DEFAULT_ANGLE_X);
        autoRotateY.setAngle(DEFAULT_ANGLE_Y);
        camTranslate.setX(0);
        camTranslate.setY(0);
        camTranslate.setZ(DEFAULT_ZOOM);
    }

    public void assignFocusToSubScene() {
        Logger.trace("Trying to assign focus to preview subscene");
        Platform.runLater(subScene::requestFocus);
    }

    public SubScene subScene() {
        return subScene;
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
                    if (control) {
                        rotatePreviewByY(-1);
                    } else {
                        moveCamBy(MOVE_DIST, 0);
                    }
                    e.consume(); // do not deliver event to tab pane
                }

                case RIGHT -> {
                    if (control) {
                        rotatePreviewByY(1);
                    }
                    else {
                        moveCamBy(-MOVE_DIST, 0);
                    }
                    e.consume(); // do not deliver event to tab pane
                }

                case UP -> {
                    if (control) {
                        rotatePreviewByX(-1);
                    } else {
                        moveCamBy(0, MOVE_DIST);
                    }
                    e.consume(); // do not deliver event to tab pane
                }

                case DOWN -> {
                    if (control) {
                        rotatePreviewByX(1);
                    } else {
                        moveCamBy(0, -MOVE_DIST);
                    }
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

            if (e.getButton() == MouseButton.PRIMARY) {
                // TODO: How to implement correctly with flipped world?
                rotatePreviewByY(0.5 * dx);
                rotatePreviewByX(0.5 * dy);
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

    public void initSampleModel(MeshTreeView tree, SampleInfo sample) {
        final SampleInitSettings settings = sample.initSettings();

        // TODO initial cam x, y
        camTranslate.setZ(settings.zoom());

        if (settings.rotateX() != 0) {
            meshesGroup.getTransforms().addLast(new Rotate(settings.rotateX(), Rotate.X_AXIS));
        }
        if (settings.rotateY() != 0) {
            meshesGroup.getTransforms().addLast(new Rotate(settings.rotateY(), Rotate.Y_AXIS));
        }
        if (settings.rotateZ() != 0) {
            meshesGroup.getTransforms().addLast(new Rotate(settings.rotateZ(), Rotate.Z_AXIS));
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
            rotatePreviewByY(ROTATE_SINGLE_STEP_DEGREES);
        }
        else if (KEY_ROTATE_LEFT_LARGE.equals(key)) {
            rotatePreviewByY(3 * ROTATE_SINGLE_STEP_DEGREES);
        }
        else if (KEY_ROTATE_RIGHT.equals(key)) {
            rotatePreviewByY(-ROTATE_SINGLE_STEP_DEGREES);
        }
        else if (KEY_ROTATE_RIGHT_LARGE.equals(key)) {
            rotatePreviewByY(-3 * ROTATE_SINGLE_STEP_DEGREES);
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
        double z = Math.clamp(camTranslate.getZ() + delta, ZOOM_MIN, ZOOM_MAX);
        camTranslate.setZ(z);
        Logger.info("Zoom: " + z);
    }

    private void moveCamBy(double dx, double dy) {
        camTranslate.setX(camTranslate.getX() + dx);
        camTranslate.setY(camTranslate.getY() + dy);
    }

    private void rotatePreviewByX(double delta) {
        rotateX.setAngle((rotateX.getAngle() + delta) % 360);
    }

    private void rotatePreviewByY(double delta) {
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

    private void addNoFocusWarningHint() {
        final Label noFocusWarning = new Label("Click to focus!");
        noFocusWarning.setMouseTransparent(true);
        noFocusWarning.setFocusTraversable(false);
        noFocusWarning.setId("noFocusWarning");
        noFocusWarning.visibleProperty().bind(subScene.focusedProperty().not());
        StackPane.setAlignment(noFocusWarning, Pos.BOTTOM_CENTER);
        noFocusWarning.setTranslateY(-5);
        getChildren().add(noFocusWarning);
    }
}
