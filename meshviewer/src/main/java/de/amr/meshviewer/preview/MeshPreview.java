/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.preview;

import de.amr.meshviewer.FlashMessageOverlay;
import de.amr.meshviewer.MeshViewerUI;
import de.amr.meshviewer.SampleInitSettings;
import de.amr.meshviewer.info.SampleInfo;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import org.tinylog.Logger;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static javafx.scene.input.KeyCombination.CONTROL_DOWN;
import static javafx.scene.input.KeyCombination.SHIFT_DOWN;

public class MeshPreview extends StackPane {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat(" 0.00;-0.00");

    public static final Paint SKY_GRADIENT = new LinearGradient(
        0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
        new Stop(0.0, Color.web("#3A8DFF")),  // deep zenith blue
        new Stop(0.5, Color.web("#6BB6FF")),  // mid-sky
        new Stop(1.0, Color.web("#A7D8FF"))   // pale horizon blue
    );

    public static final String KEY_AUTOPLAY_TOGGLE = "a";
    public static final String KEY_BOUNDING_BOXES_TOGGLE = "b";
    public static final String KEY_AUTO_ROTATE_HORIZONTALLY = "h";
    public static final String KEY_AUTO_ROTATE_VERTICALLY = "v";
    public static final String KEY_ROTATE_LEFT = "l";
    public static final String KEY_ROTATE_RIGHT = "r";
    public static final String KEY_WIREFRAME_TOGGLE = "w";

    public static final KeyCombination KEY_RESET_PREVIEW = new KeyCodeCombination(KeyCode.R, CONTROL_DOWN, SHIFT_DOWN);

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

    public static final Rotate CAMERA_UPSIDE_DOWN = new Rotate(180, Rotate.X_AXIS);

    public static final PhongMaterial RED_MATERIAL = new PhongMaterial(Color.RED);
    public static final PhongMaterial BLUE_MATERIAL = new PhongMaterial(Color.BLUE);
    public static final PhongMaterial WHITE_MATERIAL = new PhongMaterial(Color.WHITE);

    public static final PhongMaterial TRANSPARENT_LIGHTGRAY_MATERIAL = new PhongMaterial(Color.rgb(150, 150, 150, 0.6));
    public static final PhongMaterial TRANSPARENT_DARK_GRAY_MATERIAL = new PhongMaterial(Color.rgb(30, 30, 30, 0.5));

    public final ObjectProperty<DrawMode> drawMode = new SimpleObjectProperty<>(DrawMode.FILL);
    public final BooleanProperty xzPlaneVisible = new SimpleBooleanProperty(false);
    public final BooleanProperty boundingBoxesVisible = new SimpleBooleanProperty(false);
    public final BooleanProperty transformInfoVisible = new SimpleBooleanProperty(true);

    private FlashMessageOverlay flashMessageOverlay;
    private final SubScene subScene;
    private final Group world = new Group();
    private final PerspectiveCamera cam = new PerspectiveCamera(true);
    private final Group camPivot = new Group();
    private final Group meshesPivotParent = new Group();
    private final Group meshesPivot = new Group();
    private Group xzPlane;
    private Label focusLostLabel;
    private Label transformInfoLabel;

    // Camera transforms
    private final Translate cameraZoom = new Translate(0, 0, DEFAULT_ZOOM);

    // Content transforms
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate autoRotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate autoRotateY = new Rotate(0, Rotate.Y_AXIS);

    // Animation
    private Animation autoRotateAnimation;
    private Point3D autoRotateAxis = Rotate.Y_AXIS; // y-axis (horizontally) by default

    private double mouseOldX, mouseOldY;

    public MeshPreview(MeshViewerUI ui) {
        setId("preview"); // CSS ID

        cam.setNearClip(0.1);
        cam.setFarClip(10_000);
        cam.setFieldOfView(30);

        // Flip y-direction because 3D models usually have positive y-direction upwards and in JavaFX it's downwards
        camPivot.getTransforms().addAll(CAMERA_UPSIDE_DOWN, cameraZoom);

        meshesPivot.getTransforms().setAll(rotateX, rotateY, autoRotateX, autoRotateY);

        subScene = new SubScene(world, 400, 400, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(cam);

        createFlashMessageOverlay();
        createTransformInfoLabel();
        createFocusLostLabel();

        // Scene graph:
        // StackPane (MeshPreview)
        //   + Subscene
        //       + world
        //           + meshesPivotParent
        //             - meshesPivot
        //       + camPivot
        //           - cam
        //   - transformInfoLabel
        //   - focusLostLabel
        //   - flashMessageOverlay

        camPivot.getChildren().add(cam);
        meshesPivotParent.getChildren().add(meshesPivot);

        world.getChildren().addAll(meshesPivotParent, camPivot);

        getChildren().addAll(subScene, transformInfoLabel, focusLostLabel, flashMessageOverlay);

        // Make key and mouse events work as expected
        subScene.setFocusTraversable(true);
        subScene.setPickOnBounds(true);
        setPickOnBounds(false);

        setBackground(Background.fill(SKY_GRADIENT));
        setInputHandlers(ui);
    }

    public void display(Collection<MeshView> allMeshViews, Set<MeshView> displayedMeshViews) {
        meshesPivot.getChildren().clear();

        if (displayedMeshViews.isEmpty()) {
            assignFocusToSubScene();
            return;
        }

        allMeshViews.forEach(meshView -> {
            meshView.setCullFace(CullFace.NONE);
            meshView.drawModeProperty().bind(drawMode);
        });

        // Add bounding boxes to displayed mesh views
        final List<Box> boundingBoxes = new ArrayList<>();
        for (MeshView meshView : allMeshViews) {
            final Box boundingBox = createBoundingBox(meshView);
            boundingBox.visibleProperty().bind(meshView.visibleProperty().and(boundingBoxesVisible));
            boundingBoxes.add(boundingBox);
            meshesPivot.getChildren().add(new Group(boundingBox, meshView));
        }

        // Important: Plane has to be added *last*!
        if (xzPlane != null) {
            xzPlane.visibleProperty().unbind();
        }

        // Compute area of projection to xy-plane for size computation
        final Rectangle2D xzProjection = computeXZProjection(boundingBoxes);
        xzPlane = createXZPlane(2 * xzProjection.getWidth(), 2 * xzProjection.getHeight());
        xzPlane.visibleProperty().bindBidirectional(xzPlaneVisible);

        meshesPivot.getChildren().add(xzPlane);

        // Only show those in displayedMeshViews set
        allMeshViews.forEach(meshView -> meshView.setVisible(displayedMeshViews.contains(meshView)));

        assignFocusToSubScene();
    }

    public void reset(SampleInfo sample) {
        meshesPivotParent.setTranslateX(0);
        meshesPivotParent.setTranslateY(0);
        rotateX.setAngle(DEFAULT_ANGLE_X);
        rotateY.setAngle(DEFAULT_ANGLE_Y);
        autoRotateX.setAngle(DEFAULT_ANGLE_X);
        autoRotateY.setAngle(DEFAULT_ANGLE_Y);
        cameraZoom.setZ(DEFAULT_ZOOM);
        if (sample != null) {
            initSample(sample);
        }
    }

    //TODO Still unclear if this is correct
    public void centerMeshViewSet(Collection<MeshView> meshViews) {
        final Collection<Box> boundingBoxes = meshViews.stream().map(this::createBoundingBox).toList();
        Rectangle2D rect = computeXYProjection(boundingBoxes);
        Logger.info("Projection into xy plane: {}", rect);
        double y = 0.5 * (rect.getMaxY() - rect.getMinY());
        meshesPivotParent.setTranslateY(-y);
    }

    public void initSample(SampleInfo sample) {
        final SampleInitSettings settings = sample.initSettings();

        cameraZoom.setZ(settings.zoom());
        if (settings.rotateX() != 0) {
            meshesPivot.getTransforms().addLast(new Rotate(settings.rotateX(), Rotate.X_AXIS));
        }
        if (settings.rotateY() != 0) {
            meshesPivot.getTransforms().addLast(new Rotate(settings.rotateY(), Rotate.Y_AXIS));
        }
        if (settings.rotateZ() != 0) {
            meshesPivot.getTransforms().addLast(new Rotate(settings.rotateZ(), Rotate.Z_AXIS));
        }

        drawMode.set(settings.wireframe() ? DrawMode.LINE : DrawMode.FILL);

        if (settings.autorotate()) {
            autoRotateAnimation().playFromStart();
        } else {
            autoRotateAnimation().stop();
        }
    }

    public void showMessage(String message) {
        flashMessageOverlay.showMessage(message);
    }

    private Box createBoundingBox(MeshView meshView) {
        Bounds b = meshView.getBoundsInLocal(); // local, not parent

        Box box = new Box(b.getWidth(), b.getHeight(), b.getDepth());
        box.setDrawMode(DrawMode.LINE);
        box.setMaterial(new PhongMaterial(Color.RED));
        box.visibleProperty().bind(boundingBoxesVisible);

        box.setTranslateX(b.getCenterX());
        box.setTranslateY(b.getCenterY());
        box.setTranslateZ(b.getCenterZ());

        return box;
    }

    private Rectangle2D computeXYProjection(Collection<Box> boxes) {
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
        return new Rectangle2D(minX, minY, Math.max(maxX - minX, 1), Math.max(maxY - minY, 1));
    }

    private Rectangle2D computeXZProjection(Collection<Box> boxes) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (Box box : boxes) {
            final Bounds b = box.getBoundsInLocal();
            minX = Math.min(minX, b.getMinX());
            maxX = Math.max(maxX, b.getMaxX());
            minZ = Math.min(minZ, b.getMinZ());
            maxZ = Math.max(maxZ, b.getMaxZ());
        }
        return new Rectangle2D(minX, minZ, Math.max(maxX - minX, 1), Math.max(maxZ - minZ, 1));
    }

    public void assignFocusToSubScene() {
        Logger.trace("Trying to assign focus to preview subscene");
        Platform.runLater(subScene::requestFocus);
    }

    public SubScene subScene() {
        return subScene;
    }

    // private

    private void createFlashMessageOverlay() {
        flashMessageOverlay = new FlashMessageOverlay();
        flashMessageOverlay.setFocusTraversable(false);
        flashMessageOverlay.setMouseTransparent(true);
        flashMessageOverlay.setPickOnBounds(false);
    }

    private static Group createXZPlane(double width, double height) {
        final Group planeGroup = new Group();

        final double r = 0.5 * Math.max(width, height);
        final double h = 0.02;

        final Cylinder plane = new Cylinder(10 * r, h);
        plane.setMaterial(TRANSPARENT_LIGHTGRAY_MATERIAL);

        final Cylinder shadow = new Cylinder(r, 1.1 * h);
        shadow.setMaterial(TRANSPARENT_DARK_GRAY_MATERIAL);

        final Box xAxis = new Box(2*r, h, h);
        xAxis.setMaterial(RED_MATERIAL);

        final Box zAxis = new Box(h, h, 2*r);
        zAxis.setMaterial(BLUE_MATERIAL);

        planeGroup.getChildren().addAll(xAxis, zAxis);

        final double markerRadius = 0.02;
        final Sphere originMarker = new Sphere(2 * markerRadius);
        originMarker.setMaterial(WHITE_MATERIAL);
        planeGroup.getChildren().add(originMarker);

        for (int i = 1; i <= (int) r; ++i) {
            final int scale = i % 10 == 0 ? 3 : 1;

            final Sphere markerX = new Sphere(scale * markerRadius);
            markerX.setMaterial(RED_MATERIAL);
            markerX.setTranslateX(i);
            planeGroup.getChildren().add(markerX);

            final Sphere markerZ = new Sphere(scale * markerRadius);
            markerZ.setMaterial(BLUE_MATERIAL);
            markerZ.setTranslateZ(i);
            planeGroup.getChildren().add(markerZ);
        }

        planeGroup.getChildren().addAll(plane, shadow);

        return planeGroup;
    }

    private Animation autoRotateAnimation() {
        if (autoRotateAnimation == null) {
            createAutoRotateAnimation();
        }
        return autoRotateAnimation;
    }

    private void createAutoRotateAnimation() {
        autoRotateAnimation = new Timeline(
            new KeyFrame(Duration.millis(16), _ -> {
                if (autoRotateAxis == Rotate.X_AXIS) {
                    autoRotateX.setAngle(autoRotateX.getAngle() + AUTO_ROTATE_SPEED);
                } else {
                    autoRotateY.setAngle(autoRotateY.getAngle() - AUTO_ROTATE_SPEED);
                }
            }) // ~60 FPS
        );
        autoRotateAnimation.setCycleCount(Animation.INDEFINITE);
    }

    private void setInputHandlers(MeshViewerUI ui) {
        subScene.setOnKeyPressed(e -> {
            boolean shift = e.isShiftDown(), control = e.isControlDown(), controlShift = control && shift;

            if (KEY_RESET_PREVIEW.match(e)) {
                reset(ui.currentSample());
                e.consume();
                return;
            }

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
                        final double dist = shift ? 10 * MOVE_DIST : MOVE_DIST;
                        move(meshesPivotParent, -dist, 0);
                    }
                    e.consume(); // do not deliver event to tab pane
                }

                case RIGHT -> {
                    if (control) {
                        rotatePreviewByY(1);
                    }
                    else {
                        final double dist = shift ? 10 * MOVE_DIST : MOVE_DIST;
                        move(meshesPivotParent, dist, 0);
                    }
                    e.consume(); // do not deliver event to tab pane
                }

                case UP -> {
                    if (control) {
                        rotatePreviewByX(-1);
                    } else {
                        final double dist = shift ? 10 * MOVE_DIST : MOVE_DIST;
                        move(meshesPivotParent, 0, dist);
                    }
                    e.consume(); // do not deliver event to tab pane
                }

                case DOWN -> {
                    if (control) {
                        rotatePreviewByX(1);
                    } else {
                        final double dist = shift ? 10 * MOVE_DIST : MOVE_DIST;
                        move(meshesPivotParent, 0, -dist);
                    }
                    e.consume(); // do not deliver event to tab pane
                }
            }
        });

        subScene.setOnKeyTyped(e -> {
            onCharTyped(e.getCharacter());
            e.consume();
        });

        subScene.setOnMouseClicked(e -> {
            Logger.trace("Mouse clicked {}", e);
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

    private void onCharTyped(String ch) {
        if (KEY_AUTO_ROTATE_HORIZONTALLY.equals(ch)) {
            autoRotateAxis = Rotate.Y_AXIS;
            showMessage("Auto-Rotate horizontally");
        }
        else if (KEY_AUTO_ROTATE_VERTICALLY.equals(ch)) {
            autoRotateAxis = Rotate.X_AXIS;
            showMessage("Auto-Rotate vertically");
        }
        else if (KEY_ROTATE_LEFT.equals(ch)) {
            rotatePreviewByY(30);
        }
        else if (KEY_ROTATE_RIGHT.equals(ch)) {
            rotatePreviewByY(-30);
        }
        else if (KEY_WIREFRAME_TOGGLE.equals(ch)) {
            final DrawMode mode = drawMode.get() == DrawMode.FILL ? DrawMode.LINE : DrawMode.FILL;
            drawMode.set(mode);
        }
        else if (KEY_AUTOPLAY_TOGGLE.equals(ch)) {
            toggleAutoRotateWithFlashMessage();
        }
        else if (KEY_BOUNDING_BOXES_TOGGLE.equals(ch)) {
            boundingBoxesVisible.set(!boundingBoxesVisible.get());
        }
    }

    private void zoomBy(double delta) {
        double z = Math.clamp(cameraZoom.getZ() + delta, ZOOM_MIN, ZOOM_MAX);
        cameraZoom.setZ(z);
        Logger.info("Zoom: " + z);
    }

    private void move(Node node, double dx, double dy) {
        node.setTranslateX(node.getTranslateX() + dx);
        node.setTranslateY(node.getTranslateY() + dy);
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
            showMessage("Auto-Rotate paused");
        } else {
            startAutoRotate();
            showMessage("Auto-Rotate started");
        }
    }

    private void createFocusLostLabel() {
        focusLostLabel = new Label("Click to focus!");
        focusLostLabel.setId("noFocusWarning");
        focusLostLabel.setMouseTransparent(true);
        focusLostLabel.setFocusTraversable(false);
        focusLostLabel.visibleProperty().bind(subScene.focusedProperty().not());
        StackPane.setAlignment(focusLostLabel, Pos.BOTTOM_CENTER);
        focusLostLabel.setTranslateY(-40);
    }

    private void createTransformInfoLabel() {
        transformInfoLabel = new Label();
        transformInfoVisible.addListener((_, _, _visible) -> {
            if (_visible) {
                fadeIn(transformInfoLabel);
            } else {
                fadeOut(transformInfoLabel);
            }
        });
        transformInfoLabel.textProperty().bind(Bindings.createStringBinding(this::formatTransformStatus,
            cameraZoom.zProperty(),
            meshesPivotParent.translateXProperty(), meshesPivotParent.translateYProperty(),
            rotateX.angleProperty(), rotateY.angleProperty(),
            autoRotateX.angleProperty(), autoRotateY.angleProperty()
        ));
        transformInfoLabel.setId("transformStatus");
        transformInfoLabel.setMouseTransparent(true);
        transformInfoLabel.setFocusTraversable(false);
        StackPane.setAlignment(transformInfoLabel, Pos.BOTTOM_CENTER);
    }

    private String formatTransformStatus() {
        final double zoom = cameraZoom.getZ();
        final double x = meshesPivotParent.getTranslateX();
        final double y = meshesPivotParent.getTranslateY();
        final double angleX = normalizedAngle(rotateX.getAngle() + autoRotateX.getAngle());
        final double angleY = normalizedAngle(rotateY.getAngle() + autoRotateY.getAngle());
        return "Zoom: %s | Position: x=%s y=%s | Rotation: x=%s y=%s".formatted(
            DECIMAL_FORMAT.format(zoom),
            DECIMAL_FORMAT.format(x),
            DECIMAL_FORMAT.format(y),
            DECIMAL_FORMAT.format(angleX),
            DECIMAL_FORMAT.format(angleY)
        );
    }

    private static double normalizedAngle(double angle) {
        return  ((angle % 360) + 360) % 360;
    }

    private void fadeIn(Node node) {
        node.setVisible(true);
        final FadeTransition ft = new FadeTransition(Duration.millis(1000), node);
        ft.setInterpolator(Interpolator.EASE_IN);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void fadeOut(Node node) {
        final FadeTransition ft = new FadeTransition(Duration.millis(1000), node);
        ft.setInterpolator(Interpolator.EASE_OUT);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(_ -> node.setVisible(false));
        ft.play();
    }
}
