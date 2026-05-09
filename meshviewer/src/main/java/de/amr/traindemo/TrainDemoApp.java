package de.amr.traindemo;

import de.amr.meshbuilder.MeshBuilder;
import de.amr.objparser.ObjFileParser;
import de.amr.objparser.ObjModel;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * A minimal example of how to load an OBJ file, create JavaFX materials and mesh views and display them in a scene.
 */
public class TrainDemoApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        // Load the toy train sample from the resources folder
        final URL url = getClass().getResource("/samples/toy_train/toyTrain.obj");
        if (url == null) {
            Logger.error("Could not load obj file");
            return;
        }

        // Parse the OBJ file and create the JavaFX materials and mesh views
        final ObjModel model3D = new ObjFileParser(url, StandardCharsets.UTF_8).parse();
        final Collection<MeshView> meshViews = new MeshBuilder(model3D).buildMeshViewsByMaterial().values();

        // Create the scene content
        final Group train = new Group(meshViews.toArray(MeshView[]::new));

        final Group world = new Group(train);
        // JavaFX coordinate system is upside-down, so flip the world
        final Transform upsideDown = new Rotate(180, Rotate.X_AXIS);
        world.getTransforms().add(upsideDown);

        final Scene scene = new Scene(world, 800, 600, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.DARKGRAY);

        // Camera looks at orgin, take a step back
        final PerspectiveCamera camera = new PerspectiveCamera(true);
        scene.setCamera(camera);
        camera.setTranslateZ(-60);

        // Zoom in/out with "+" and "-" keys
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case PLUS  -> camera.setTranslateZ(camera.getTranslateZ() + 1);
                case MINUS -> camera.setTranslateZ(camera.getTranslateZ() - 1);
            }
        });

        // Display the scene
        stage.setScene(scene);
        stage.setTitle("Train Demo");
        stage.show();
    }
}
