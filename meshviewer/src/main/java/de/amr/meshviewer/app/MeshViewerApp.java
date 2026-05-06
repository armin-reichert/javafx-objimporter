/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.app;

import de.amr.meshviewer.MeshViewerUI;
import de.amr.meshviewer.SampleModel;
import de.amr.meshviewer.SampleState;
import javafx.application.Application;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MeshViewerApp extends Application {

    private final SampleModel[] SAMPLES = {
        new SampleModel(
            "Teapot",
            getClass().getResource("/models/newell_teaset/teapot.obj"),
            new SampleState(-10, 0, 0, 0)),

        new SampleModel(
            "Scooter",
            getClass().getResource("/models/scooter/Scooter-normals.obj"),
            new SampleState(-2, 0, 0, 0)),

        new SampleModel(
            "Scooter (Smooth)",
            getClass().getResource("/models/scooter/Scooter-smgrps.obj"),
            new SampleState(-2, 0, 0, 0)),

        new SampleModel(
            "Alien Animal",
            getClass().getResource("/models/alien_animal/Alien Animal.obj"),
            new SampleState(-50, 0, 0, 0)),

        new SampleModel("Beagle",
            getClass().getResource("/models/beagle/13041_Beagle_v1_L1.obj"),
            new SampleState(-150, 0, 0, 0)),

        new SampleModel("Aya",
            getClass().getResource("/models/aya_japanese_girl/091_W_Aya_100K.obj"),
            new SampleState(-2800, 0, 0, 0)),

        new SampleModel("Datsun 280Z",
            getClass().getResource("/models/datsun_280Z/Datsun_280Z.obj"),
            new SampleState(-4, 0, 0, 0)),

        new SampleModel("Pac-Man (by Gianmarco Cavallaccio)",
            getClass().getResource("/models/pacman/pacman.obj"),
            new SampleState(-42, 30, 0, 0))
    };

    @Override
    public void start(Stage stage) {
        final double screenHeight = Screen.getPrimary().getBounds().getHeight();
        final double screenWidth = Screen.getPrimary().getBounds().getWidth();
        final double aspect = screenWidth / screenHeight;
        final double height = Math.min(0.90 * screenHeight, 800);
        final double width = aspect * height;
        final MeshViewerUI ui = new MeshViewerUI(stage, width, height);
        for (SampleModel sample : SAMPLES) {
            ui.addSampleModel(sample);
        }
        ui.show();
    }
}
