/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.info;

import de.amr.meshviewer.SampleInitSettings;

public record SampleInfo(
    String title,
    String author,
    String fileName,
    String path,
    String downloadURL,
    String homepage,
    String licenseType,
    String licenseURL,
    SampleInitSettings initSettings)
{}
