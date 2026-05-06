/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

import java.net.URL;

public record SampleInfo(
    String title,
    String author,
    String fileName,
    String path,
    String licenseType,
    URL licenseURL,
    TransformSettings initialState) {
}
