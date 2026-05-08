/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

import de.amr.meshviewer.meshtree.MeshSelection;

public record SampleInitSettings(
    double zoom,
    double rotateX,
    double rotateY,
    double rotateZ,
    MeshSelection initialMeshSelection,
    boolean wireframe,
    boolean autorotate) {}
