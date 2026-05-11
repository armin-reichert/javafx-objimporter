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
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * A minimal example of how to load an OBJ file, create JavaFX materials and mesh views and display them in a scene.
 */
public class TrainDemoApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        // Load the toy train sample from the resources folder
        final URL url = getClass().getResource("/samples/toy_train/toyTrain.obj");
        if (url == null) {
            throw new IllegalArgumentException("Could not find train model OBJ file");
        }

        // Parse the OBJ file and create the JavaFX materials and mesh views
        final ObjModel model3D = new ObjFileParser(url, StandardCharsets.UTF_8).parse();
        final Map<String, MeshView> meshViews = new MeshBuilder(model3D).buildMeshViewsByMaterial();

        // Put all created mesh views inside a transformation group, adjust vertical train position
        final Group train = new Group(meshViews.values().toArray(MeshView[]::new));
        train.setTranslateY(-5);

        // JavaFX coordinate system is upside-down, so flip the world
        final Group world = new Group(train);
        world.getTransforms().add(new Rotate(180, Rotate.X_AXIS));

        // Camera looks at origin of scene, take a step back
        final PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-40);

        // Crete scene, camera can be zoomed with "+" and "-" keys
        final Scene scene = new Scene(world, 700, 300, true, SceneAntialiasing.BALANCED);
        scene.setCamera(camera);
        scene.setFill(Color.DARKGRAY);
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case PLUS  -> camera.setTranslateZ(camera.getTranslateZ() + 1);
                case MINUS -> camera.setTranslateZ(camera.getTranslateZ() - 1);
            }
        });

        // Show the scene inside a window
        stage.setTitle("Train Demo");
        stage.setScene(scene);
        stage.show();
    }
}
