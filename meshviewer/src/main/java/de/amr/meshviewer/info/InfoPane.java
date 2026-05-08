/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.info;

import javafx.application.HostServices;
import javafx.event.Event;
import javafx.scene.control.Hyperlink;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import org.tinylog.Logger;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;

import static de.amr.meshviewer.MeshViewerUI.INFO_AREA_LABEL_COLUMN_WIDTH;
import static java.util.Objects.requireNonNull;

public class InfoPane extends GridPane {

    public static final String NA = "-";

    // For dots between thousands
    public static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.GERMANY);

    public static Hyperlink createHyperlink() {
        final Hyperlink link = new Hyperlink();
        link.setFocusTraversable(false);
        link.addEventFilter(KeyEvent.ANY, Event::consume);
        return link;
    }

    public static String extractFilePart(String url) {
        final String cleaned = URLDecoder.decode(url, StandardCharsets.UTF_8);
        int lastSlash = cleaned.lastIndexOf('/');
        if (lastSlash >= 0) {
            return cleaned.substring(lastSlash + 1);
        }
        return cleaned;
    }

    protected final HostServices hostServices;

    public InfoPane(HostServices hostServices) {
        this.hostServices = requireNonNull(hostServices);
        getStyleClass().add("infoPanel");

        final var constraints = new ColumnConstraints(
            0.5 * INFO_AREA_LABEL_COLUMN_WIDTH, // min
            INFO_AREA_LABEL_COLUMN_WIDTH, // pref
            1.25 * INFO_AREA_LABEL_COLUMN_WIDTH); // max
        getColumnConstraints().add(constraints);
    }

    public void setLinkAction(Hyperlink link, String url) {
        link.setOnAction(_ -> {
            try {
                hostServices.showDocument(url);
            } catch (Exception x) {
                Logger.error(x, "Could not open URL {}", url);
            }
        });
        link.setDisable(false);
    }
}
