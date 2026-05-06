/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.app;

import de.amr.meshviewer.MeshViewerUI;
import de.amr.meshviewer.SampleInfo;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MeshViewerApp extends Application {

    public static HostServices HOST_SERVICES;

    @Override
    public void start(Stage stage) {
        HOST_SERVICES = getHostServices();
        final double screenHeight = Screen.getPrimary().getBounds().getHeight();
        final double screenWidth = Screen.getPrimary().getBounds().getWidth();
        final double aspect = screenWidth / screenHeight;
        final double height = Math.min(0.90 * screenHeight, 800);
        final double width = aspect * height;
        final MeshViewerUI ui = new MeshViewerUI(stage, width, height);
        for (SampleInfo sample : SamplesCollection.SAMPLES) {
            ui.addSampleModel(sample);
        }
        ui.show();
    }
}
