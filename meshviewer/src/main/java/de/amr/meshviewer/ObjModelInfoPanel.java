/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

import de.amr.objparser.ObjModel;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ObjModelInfoPanel extends GridPane {

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.GERMANY);
    private static final String NA = "-";

    private final Label lblVertices = new Label();
    private final Label lblTexCoords = new Label();
    private final Label lblNormals = new Label();
    private final Label lblObjects = new Label();
    private final Label lblGroups = new Label();
    private final Label lblFaces = new Label();
    private final Label lblSmoothingGroups = new Label();
    private final Label lblMaterials = new Label();
    private final Label lblLoadingTime = new Label();

    public ObjModelInfoPanel(String id) {
        setId(id); // for CSS

        addRow(0, new Label("Vertices:"), lblVertices);
        addRow(1, new Label("TexCoords:"), lblTexCoords);
        addRow(2, new Label("Normals:"), lblNormals);
        addRow(3, new Label("Objects:"), lblObjects);
        addRow(4, new Label("Groups:"), lblGroups);
        addRow(5, new Label("Faces:"), lblFaces);
        addRow(6, new Label("Smoothing Groups:"), lblSmoothingGroups);
        addRow(7, new Label("Materials:"), lblMaterials);
        addRow(8, new Label("LoadingTime:"), lblLoadingTime);
    }

    public void update(ObjModel model, Duration loadingTime) {
        if (loadingTime != null) {
            lblLoadingTime.setText("%.3f sec".formatted(loadingTime.toSeconds()));
        } else {
            lblLoadingTime.setText(NA);
        }

        if (model == null) {
            lblVertices.setText(NA);
            lblTexCoords.setText(NA);
            lblNormals.setText(NA);
            lblObjects.setText(NA);
            lblGroups.setText(NA);
            lblFaces.setText(NA);
            lblSmoothingGroups.setText(NA);
            lblMaterials.setText(NA);
            lblLoadingTime.setText(NA);
        }
        else {

            lblVertices.setText(NUMBER_FORMAT.format(model.vertexCount()));
            lblTexCoords.setText(NUMBER_FORMAT.format(model.texCoordCount()));
            lblNormals.setText(NUMBER_FORMAT.format(model.normalCount()));

            lblObjects.setText(NUMBER_FORMAT.format(model.objects.size()));

            int groupCount = model.objects.stream()
                .mapToInt(o -> o.groups.size())
                .sum();
            lblGroups.setText(NUMBER_FORMAT.format(groupCount));

            int faceCount = model.objects.stream()
                .flatMap(o -> o.groups.stream())
                .mapToInt(g -> g.faces.size())
                .sum();
            lblFaces.setText(NUMBER_FORMAT.format(faceCount));

            long smoothingGroups = model.objects.stream()
                .flatMap(o -> o.groups.stream())
                .flatMap(g -> g.faces.stream())
                .map(f -> f.smoothingGroup)
                .filter(Objects::nonNull)
                .distinct()
                .count();
            lblSmoothingGroups.setText(NUMBER_FORMAT.format(smoothingGroups));

            int materialCount = model.materialLibsMap.values().stream()
                .mapToInt(Map::size)
                .sum();
            lblMaterials.setText(NUMBER_FORMAT.format(materialCount));
        }
    }
}
