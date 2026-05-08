/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

import de.amr.meshviewer.tree.MeshSelection;

public record SampleInitSettings(
    double zoom,
    double rotateX,
    double rotateY,
    double rotateZ,
    MeshSelection initialMeshSelection,
    boolean wireframe,
    boolean autorotate) {}
