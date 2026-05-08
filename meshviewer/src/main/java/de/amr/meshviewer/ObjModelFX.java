/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;

import java.util.Map;

public record ObjModelFX(
    Map<String, MeshView> objectMeshViews,
    Map<String, MeshView> groupMeshViews,
    Map<String, MeshView> materialMeshViews,
    Map<String, PhongMaterial> materialsMap)
{
    public static final ObjModelFX EMPTY = new ObjModelFX(Map.of(), Map.of(), Map.of(), Map.of());
}
