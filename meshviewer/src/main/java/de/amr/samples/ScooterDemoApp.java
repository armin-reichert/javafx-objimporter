package de.amr.samples;

import de.amr.meshbuilder.MeshBuilder;
import de.amr.objparser.ObjFileParser;
import de.amr.objparser.ObjModel;
import javafx.animation.*;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.paint.Color;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ScooterDemoApp extends Application {

    public static final double AUTO_ROTATE_SPEED = 0.2;

    private PerspectiveCamera camera;
    private Animation autoRotateAnimation;
    private final Rotate autoRotateY = new Rotate(0, Rotate.Y_AXIS);

    private final ObjectProperty<DrawMode> drawMode = new SimpleObjectProperty<>(DrawMode.FILL);

    private Scene createScene() throws IOException {

        camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-2.5);

        Group scooter = loadModel(getClass().getResource("/scooter/Scooter-smgrps.obj"));
        scooter.getTransforms().add(new Rotate(90, Rotate.Y_AXIS));
        scooter.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
        scooter.getTransforms().add(autoRotateY);
        // move a bit down
        scooter.getTransforms().add(new Translate(0, -0.4, 0));

        animate(scooter);

        Group root = new Group(scooter);

        Scene scene = new Scene(root, 800, 600, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.LIGHTBLUE);
        scene.setCamera(camera);

        scene.setOnKeyTyped(e -> {
            if ("w".equals(e.getCharacter())) {
                drawMode.set(drawMode.get() == DrawMode.FILL ? DrawMode.LINE : DrawMode.FILL);
            }
        });

        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case PLUS  -> zoomBy(0.1);
                case MINUS -> zoomBy(-0.1);
            }
        });

        scene.setOnScroll(e -> {
            zoomBy(e.getDeltaY() * 0.01);
        });

        return scene;
    }

    private void zoomBy(double amount) {
        camera.setTranslateZ(camera.getTranslateZ() + amount);
    }

    private Group loadModel(URL url) throws IOException {
        Group modelRoot = new Group();

        ObjModel objModel = new ObjFileParser(url, StandardCharsets.UTF_8).parse();

        for (MeshView view : MeshBuilder.build(objModel, MeshBuilder.BuildMode.BY_GROUP).values()) {
            modelRoot.getChildren().add(view);
            view.drawModeProperty().bind(drawMode);
        }

        return modelRoot;
    }

    private void animate(Group model) {
        model.getChildren()
            .stream()
            .filter(view -> view.getId().endsWith(".RimFront") || view.getId().endsWith(".RimRear"))
            .forEach(view -> {
                RotateTransition rt = new RotateTransition(Duration.seconds(0.33), view);
                rt.setCycleCount(Integer.MAX_VALUE);
                rt.setAxis(Rotate.X_AXIS);
                rt.setByAngle(360);
                rt.setInterpolator(Interpolator.LINEAR);
                rt.play();
            });
    }

    private void createAutoRotateAnimation() {
        autoRotateAnimation = new Timeline(
            new KeyFrame(Duration.millis(16), _ -> autoRotateY.setAngle(autoRotateY.getAngle() - AUTO_ROTATE_SPEED)) // ~60 FPS
        );
        autoRotateAnimation.setCycleCount(Animation.INDEFINITE);

    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.setScene(createScene());
        stage.setTitle("Scooter (Courtesy of Almas B)");
        stage.show();
        createAutoRotateAnimation();
        autoRotateAnimation.play();
    }
}