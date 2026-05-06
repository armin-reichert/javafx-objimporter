/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

public record SampleInfo(
    String title,
    String author,
    String fileName,
    String path,
    String downloadURL,
    String homepage,
    String licenseType,
    String licenseURL,
    TransformSettings initialTransformSettings,
    MeshSelection initialMeshSelection)
{}
