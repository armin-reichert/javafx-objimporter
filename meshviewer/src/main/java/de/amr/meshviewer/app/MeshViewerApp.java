/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.app;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.amr.meshviewer.MeshViewerUI;
import de.amr.meshviewer.SampleInfo;
import javafx.application.Application;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MeshViewerApp extends Application {

    private List<SampleInfo> samples = List.of();

    @Override
    public void init() {
        loadSampleModels();
    }

    @Override
    public void start(Stage stage) {
        final double screenHeight = Screen.getPrimary().getBounds().getHeight();
        final double screenWidth = Screen.getPrimary().getBounds().getWidth();
        final double aspect = screenWidth / screenHeight;
        final double height = Math.min(0.90 * screenHeight, 800);
        final double width = aspect * height;
        final MeshViewerUI ui = new MeshViewerUI(stage, width, height, getHostServices());
        for (final SampleInfo sample : samples) {
            ui.addSampleModel(sample);
        }
        ui.show();
    }

    private void loadSampleModels() {
        final Gson gson = new Gson();
        try (final InputStream in = getClass().getResourceAsStream("/models/toc.json")) {
            if (in != null) {
                samples = gson.fromJson(
                    new InputStreamReader(in, StandardCharsets.UTF_8),
                    new TypeToken<List<SampleInfo>>() {}.getType());
            }
        } catch (IOException x) {
            Logger.error(x, "Could not load models/toc.json");
        }
        Logger.info("Found {} sample models", samples.size());
    }
}
