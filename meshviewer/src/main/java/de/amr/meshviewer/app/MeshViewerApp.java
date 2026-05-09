/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.app;

import de.amr.meshviewer.MeshViewerUI;
import de.amr.meshviewer.info.SampleInfo;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.tinylog.Logger;

import java.net.URL;
import java.util.List;

import static de.amr.meshviewer.SampleInfoReader.loadSampleInfo;

public class MeshViewerApp extends Application {

    public static final String INTEGRATED_SAMPLES_TOC = "/samples/toc.json";

    @Override
    public void start(Stage stage) {
        final Rectangle2D screen = Screen.getPrimary().getBounds();
        final double aspect = screen.getWidth() / screen.getHeight();
        // Use 90% of screen height but no more than 800px
        final double height = Math.min(0.90 * screen.getHeight(), 800);
        final double width = aspect * height;

        final MeshViewerUI ui = new MeshViewerUI(stage, width, height, getHostServices());

        final URL toc = getClass().getResource(INTEGRATED_SAMPLES_TOC);
        if (toc != null) {
            final List<SampleInfo> samplesToc = loadSampleInfo(toc);
            Logger.info("{} integrated samples should be available, see menu 'Samples'", samplesToc.size());
            for (final SampleInfo sample : samplesToc) {
                ui.addSampleModel(ui.menus().samplesMenu(), sample);
            }
        } else {
            Logger.error("Could not access samples TOC");
        }
        ui.show();
    }
}
