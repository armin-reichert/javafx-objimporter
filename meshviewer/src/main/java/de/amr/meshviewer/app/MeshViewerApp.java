/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.app;

import de.amr.meshviewer.MeshViewerUI;
import de.amr.meshviewer.SampleInfo;
import de.amr.meshviewer.TransformSettings;
import javafx.application.Application;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MeshViewerApp extends Application {

    private static final SampleInfo[] SAMPLES = {
        new SampleInfo(
            "Teapot",
            "Newell et al.",
            "teapot.obj",
            "/models/newell_teaset/",
            "LICENSE?",
            null, // TODO
            new TransformSettings(-10, 0, 0, 0)),

        new SampleInfo(
            "Scooter",
            "AUTHOR TODO",
            "Scooter-normals.obj",
            "/models/scooter/",
            "LICENSE?",
            null, // TODO
            new TransformSettings(-2, 0, 0, 0)),

        new SampleInfo(
            "Scooter (Smooth)",
            "AUTHOR TODO",
            "Scooter-smgrps.obj",
            "/models/scooter/",
            "LICENSE?",
            null, // TODO
            new TransformSettings(-2, 0, 0, 0)),

        new SampleInfo(
            "Alien Animal",
            "AUTHOR TODO",
            "Alien Animal.obj",
            "/models/alien_animal/",
            "LICENSE?",
            null, // TODO
            new TransformSettings(-50, 0, 0, 0)),

        new SampleInfo(
            "Beagle",
            "AUTHOR TODO",
            "13041_Beagle_v1_L1.obj",
            "/models/beagle/",
            "LICENSE?",
            null, // TODO
            new TransformSettings(-150, 0, 0, 0)),

        new SampleInfo(
            "Aya",
            "AUTHOR TODO",
            "091_W_Aya_100K.obj",
            "/models/aya_japanese_girl/",
            "LICENSE?",
            null, // TODO
            new TransformSettings(-2800, 0, 0, 0)),

        new SampleInfo(
            "Datsun 280Z",
            "AUTHOR TODO",
            "Datsun_280Z.obj",
            "/models/datsun_280Z/",
            "LICENSE?",
            null, // TODO
            new TransformSettings(-4, 0, 0, 0)),

        new SampleInfo("Pac-Man",
            "Gianmarco Cavallaccio",
            "pacman.obj",
            "/models/pacman/",
            "LICENSE?",
            null, // TODO
            new TransformSettings(-42, 30, 0, 0))
    };

    @Override
    public void start(Stage stage) {
        final double screenHeight = Screen.getPrimary().getBounds().getHeight();
        final double screenWidth = Screen.getPrimary().getBounds().getWidth();
        final double aspect = screenWidth / screenHeight;
        final double height = Math.min(0.90 * screenHeight, 800);
        final double width = aspect * height;
        final MeshViewerUI ui = new MeshViewerUI(stage, width, height);
        for (SampleInfo sample : SAMPLES) {
            ui.addSampleModel(sample);
        }
        ui.show();
    }
}
