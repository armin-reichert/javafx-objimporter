package de.amr.meshviewer;

import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import org.tinylog.Logger;

import static de.amr.meshviewer.MeshViewerUI.INFO_PANE_LABEL_COLUMN_WIDTH;

public class SampleInfoPane extends GridPane {

    private static final String NA = "-";

    private final Label lblTitle = new Label();
    private final Label lblAuthor = new Label();
    private final Label lblFileName = new Label();
    private final Hyperlink lnkLicenseURL = new Hyperlink();

    public SampleInfoPane(String cssID) {
        setId(cssID);

        final var constraints = new ColumnConstraints(INFO_PANE_LABEL_COLUMN_WIDTH, INFO_PANE_LABEL_COLUMN_WIDTH, INFO_PANE_LABEL_COLUMN_WIDTH);
        getColumnConstraints().add(constraints);

        int row = -1;
        addRow(++row, new Label("Title:"), lblTitle);
        addRow(++row, new Label("Author:"), lblAuthor);
        addRow(++row, new Label("File:"), lblFileName);
        addRow(++row, new Label("License:"), lnkLicenseURL);
    }

    public void update(SampleInfo sample) {
        if (sample != null) {
            lblTitle.setText(sample.title());
            lblAuthor.setText(sample.author());
            lblFileName.setText(sample.fileName());
            lnkLicenseURL.setText(sample.licenseType());
            if (sample.licenseURL() != null) {
                lnkLicenseURL.setOnAction(_ -> {
                    Logger.info("TODO: open hyperlink");
                });
            }
        }
    }
}
