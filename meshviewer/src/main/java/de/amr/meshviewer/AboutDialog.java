/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;

public class AboutDialog extends Dialog<Void> {

    public static final String STYLE = """
            -fx-background-color: linear-gradient(to bottom, #1a1a1a, #0f0f0f);
            -fx-padding: 20;
            -fx-font-size: 14px;
            -fx-text-fill: #e0e0e0;
            -fx-border-color: #3a6ea5;
            -fx-border-width: 1;
            -fx-border-radius: 4;
        """;

    public static final String TEXT = """
        MeshViewer — 3D Preview for JavaFX
        
        Explore JavaFX mesh views with geometry and materials defined in OBJ models.
        
        
        DISCLAIMER:
        
        OBJ parser development supported by Copilot AI.
        (Do *not* tell this on Reddit or you will get censured!)
        
        Written by an "unvaxxed", "climate change denying" old white man
        who hates wokeness and gender bullshit.
        
        © 2026 Armin Reichert
        """;

    public AboutDialog() {
        setTitle("About");
        final DialogPane pane = getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setStyle(STYLE);
        final var text = new Label(TEXT);
        text.setStyle("-fx-text-fill: #d0d0d0;");
        pane.setContent(text);
    }
}
