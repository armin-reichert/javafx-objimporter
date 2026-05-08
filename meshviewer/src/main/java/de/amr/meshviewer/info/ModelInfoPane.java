/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.info;

import de.amr.objparser.ObjModel;
import javafx.application.HostServices;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.util.Map;
import java.util.Objects;

public class ModelInfoPane extends InfoPane {

    private final Hyperlink lnkFile = createHyperlink();
    private final Label lblVertices = new Label();
    private final Label lblTexCoords = new Label();
    private final Label lblNormals = new Label();
    private final Label lblObjects = new Label();
    private final Label lblGroups = new Label();
    private final Label lblFaces = new Label();
    private final Label lblSmoothingGroups = new Label();
    private final Label lblMaterials = new Label();
    private final Label lblParsingTime = new Label();
    private final Label lblMeshCreationTime = new Label();

    public ModelInfoPane(String cssID, HostServices hostServices) {
        super(hostServices);

        setId(cssID); // for CSS

        int row = -1;
        addRow(++row, new Label("File: "), lnkFile);
        addRow(++row, new Label("Vertices:"), lblVertices);
        addRow(++row, new Label("TexCoords:"), lblTexCoords);
        addRow(++row, new Label("Normals:"), lblNormals);
        addRow(++row, new Label("Objects:"), lblObjects);
        addRow(++row, new Label("Groups:"), lblGroups);
        addRow(++row, new Label("Faces:"), lblFaces);
        addRow(++row, new Label("Smoothing Groups:"), lblSmoothingGroups);
        addRow(++row, new Label("Materials:"), lblMaterials);
        addRow(++row, new Label("Parsing:"), lblParsingTime);
        addRow(++row, new Label("Mesh Creation:"), lblMeshCreationTime);
    }

    public void update(ObjModel model, int numMeshViews, Duration parsingTime, Duration meshCreationTime) {
        if (parsingTime != null) {
            lblParsingTime.setText("%.3f s".formatted(parsingTime.toSeconds()));
        } else {
            lblParsingTime.setText(NA);
        }
        if (meshCreationTime != null) {
            lblMeshCreationTime.setText("%.3f s (%d meshes)".formatted(meshCreationTime.toSeconds(), numMeshViews));
        } else {
            lblMeshCreationTime.setText(NA);
        }

        if (model == null) {
            lnkFile.setText(NA);
            lnkFile.setDisable(true);
            lblVertices.setText(NA);
            lblTexCoords.setText(NA);
            lblNormals.setText(NA);
            lblObjects.setText(NA);
            lblGroups.setText(NA);
            lblFaces.setText(NA);
            lblSmoothingGroups.setText(NA);
            lblMaterials.setText(NA);
            lblParsingTime.setText(NA);
            lblMaterials.setText(NA);
        }
        else {
            lnkFile.setText(extractFilePart(model.url()));
            setLinkAction(lnkFile, model.url());
            lblVertices.setText(NUMBER_FORMAT.format(model.vertexCount()));
            lblTexCoords.setText(NUMBER_FORMAT.format(model.texCoordCount()));
            lblNormals.setText(NUMBER_FORMAT.format(model.normalCount()));

            lblObjects.setText(NUMBER_FORMAT.format(model.objects.size()));

            final int groupCount = model.objects.stream()
                .mapToInt(o -> o.groups.size())
                .sum();
            lblGroups.setText(NUMBER_FORMAT.format(groupCount));

            final int faceCount = model.objects.stream()
                .flatMap(o -> o.groups.stream())
                .mapToInt(g -> g.faces.size())
                .sum();
            lblFaces.setText(NUMBER_FORMAT.format(faceCount));

            final long smoothingGroupsCount = model.objects.stream()
                .flatMap(o -> o.groups.stream())
                .flatMap(g -> g.faces.stream())
                .map(f -> f.smoothingGroup)
                .filter(Objects::nonNull)
                .distinct()
                .count();
            lblSmoothingGroups.setText(NUMBER_FORMAT.format(smoothingGroupsCount));

            final int materialCount = model.materialLibsMap.values().stream()
                .mapToInt(Map::size)
                .sum();
            lblMaterials.setText(NUMBER_FORMAT.format(materialCount));
        }
    }
}
