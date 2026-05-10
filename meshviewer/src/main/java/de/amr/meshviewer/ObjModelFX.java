/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

import de.amr.meshbuilder.MeshBuilder;
import de.amr.objparser.ObjModel;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import org.tinylog.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class ObjModelFX {

    public static final ObjModelFX EMPTY = new ObjModelFX();

    private static void logTime(Runnable code, String description) {
        final Instant before = Instant.now();
        try {
            code.run();
            final Duration duration = Duration.between(before, Instant.now());
            Logger.info("{} millis: {}", duration.toMillis(), description);
        } catch (Exception x) {
            Logger.error(x, "Error measuring runtime of '{}'", description);
        }
    }

    private final ObjModel objModel;
    private Map<String, MeshView> objectMeshViews;
    private Map<String, MeshView> groupMeshViews;
    private Map<String, MeshView> materialMeshViews;

    private MeshBuilder builder;

    private ObjModelFX() {
        objModel = null;
        objectMeshViews = Map.of();
        groupMeshViews = Map.of();
        materialMeshViews = Map.of();
    }

    public ObjModelFX(ObjModel objModel) {
        this.objModel = requireNonNull(objModel);
        builder = new MeshBuilder(objModel);
    }

    public ObjModel objModel() {
        return objModel;
    }

    public Map<String, MeshView> objectMeshViews() {
        if (objectMeshViews == null) {
            logTime(
                () -> objectMeshViews = builder.buildMeshViewsByObject(),
                "Creating mesh views for objects of OBJ %s".formatted(objModel.url())
            );
        }
        return objectMeshViews;
    }

    public Map<String, MeshView> groupMeshViews() {
        if (groupMeshViews == null) {
            logTime(
                () -> groupMeshViews =  builder.buildMeshViewsByGroup(),
                "Creating mesh views for groups of OBJ %s".formatted(objModel.url())
            );
        }
        return groupMeshViews;
    }

    public Map<String, MeshView> materialMeshViews() {
        if (materialMeshViews == null) {
            logTime(
                () -> materialMeshViews = builder.buildMeshViewsByMaterial(),
                "Creating mesh views for materials of OBJ %s".formatted(objModel.url())
            );
        }
        return materialMeshViews;
    }

    public Map<String, PhongMaterial> materials() {
        return builder.materials();
    }
}
