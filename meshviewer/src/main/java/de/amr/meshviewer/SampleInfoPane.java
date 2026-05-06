package de.amr.meshviewer;

import de.amr.meshviewer.app.MeshViewerApp;
import javafx.event.Event;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import org.tinylog.Logger;

import static de.amr.meshviewer.MeshViewerUI.INFO_PANE_LABEL_COLUMN_WIDTH;

public class SampleInfoPane extends GridPane {

    private static final String NA = "No info available";

    private static Hyperlink createHyperlink() {
        final Hyperlink link = new Hyperlink();
        link.setFocusTraversable(false);
        link.addEventFilter(KeyEvent.ANY, Event::consume);
        return link;
    }

    private final Label lblTitle = new Label();
    private final Label lblAuthor = new Label();
    private final Hyperlink lnkFile = createHyperlink();
    private final Hyperlink lnkHomepage = createHyperlink();
    private final Hyperlink lnkLicenseURL = createHyperlink();

    public SampleInfoPane(String cssID) {
        setId(cssID);

        setFocusTraversable(false); // Only mouse clicks allowed

        final var constraints = new ColumnConstraints(INFO_PANE_LABEL_COLUMN_WIDTH, INFO_PANE_LABEL_COLUMN_WIDTH, INFO_PANE_LABEL_COLUMN_WIDTH);
        getColumnConstraints().add(constraints);

        int row = -1;
        addRow(++row, new Label("Title:"), lblTitle);
        addRow(++row, new Label("Author:"), lblAuthor);
        addRow(++row, new Label("File:"), lnkFile);
        addRow(++row, new Label("Internet"), lnkHomepage);
        addRow(++row, new Label("License"), lnkLicenseURL);
    }

    private void setLinkAction(Hyperlink link, String url) {
        link.setOnAction(_ -> {
            try {
                MeshViewerApp.HOST_SERVICES.showDocument(url);
            } catch (Exception x) {
                Logger.error(x, "Could not open URL {}", url);
            }
        });
        link.setDisable(false);
    }

    public void update(SampleInfo sample) {
        if (sample != null) {
            lblTitle.setText(sample.title());
            lblAuthor.setText(sample.author());

            lnkFile.setText(sample.fileName());
            if (sample.downloadURL() != null) {
                setLinkAction(lnkFile, sample.downloadURL());
            } else {
                lnkFile.setDisable(true);
            }

            if (sample.homepage() != null) {
                lnkHomepage.setText("Visit Homepage");
                setLinkAction(lnkHomepage, sample.homepage());
            } else {
                lnkHomepage.setText(NA);
                lnkHomepage.setDisable(true);
            }

            if (sample.licenseURL() != null) {
                lnkLicenseURL.setText("View License file");
                setLinkAction(lnkLicenseURL, sample.licenseURL());
            } else {
                lnkLicenseURL.setText(NA);
                lnkLicenseURL.setDisable(true);
            }
        }
    }
}
