/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.info;

import javafx.application.HostServices;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;

public class SampleInfoPane extends InfoPane {

    private final Label lblTitle = new Label();
    private final Label lblAuthor = new Label();
    private final Hyperlink lnkHomepage = createHyperlink();
    private final Hyperlink lnkLicenseURL = createHyperlink();

    public SampleInfoPane(String cssID, HostServices hostServices) {
        super(hostServices);

        setId(cssID);
        setFocusTraversable(false); // Only mouse clicks allowed

        int row = -1;
        addRow(++row, new Label("Title:"), lblTitle);
        addRow(++row, new Label("Author:"), lblAuthor);
        addRow(++row, new Label("Internet"), lnkHomepage);
        addRow(++row, new Label("License"), lnkLicenseURL);
    }

    public void update(SampleInfo sample) {
        if (sample != null) {
            lblTitle.setText(sample.title());
            lblAuthor.setText(sample.author());

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
