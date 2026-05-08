/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.info;

import javafx.application.HostServices;
import javafx.event.Event;
import javafx.scene.control.Hyperlink;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import org.tinylog.Logger;

import java.text.NumberFormat;
import java.util.Locale;

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

    protected final HostServices hostServices;

    public InfoPane(HostServices hostServices) {
        this.hostServices = requireNonNull(hostServices);
        getStyleClass().add("infoPanel");
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
