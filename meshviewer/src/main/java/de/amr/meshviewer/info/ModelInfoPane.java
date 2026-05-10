/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.info;

import de.amr.meshviewer.ObjModelFX;
import de.amr.objparser.ObjModel;
import javafx.application.HostServices;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;

import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

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
    }

    public void clear() {
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

    public void update(ObjModelFX model, long parsingTimeMillis) {
        requireNonNull(model);

        if (parsingTimeMillis >= 0) {
            lblParsingTime.setText("%.3f s".formatted(parsingTimeMillis / 1000.0));
        } else {
            lblParsingTime.setText(NA);
        }

        final ObjModel obj = model.objModel();

        lnkFile.setText(extractFilePart(obj.url()));
        setLinkAction(lnkFile, obj.url());
        lblVertices.setText(NUMBER_FORMAT.format(obj.vertexCount()));
        lblTexCoords.setText(NUMBER_FORMAT.format(obj.texCoordCount()));
        lblNormals.setText(NUMBER_FORMAT.format(obj.normalCount()));

        lblObjects.setText(NUMBER_FORMAT.format(obj.objects.size()));

        final int groupCount = obj.objects.stream()
            .mapToInt(object -> object.groups.size())
            .sum();
        lblGroups.setText(NUMBER_FORMAT.format(groupCount));

        final int faceCount = obj.objects.stream()
            .flatMap(object -> object.groups.stream())
            .mapToInt(group -> group.faces.size())
            .sum();
        lblFaces.setText(NUMBER_FORMAT.format(faceCount));

        final long smoothingGroupsCount = obj.objects.stream()
            .flatMap(object -> object.groups.stream())
            .flatMap(group -> group.faces.stream())
            .map(face -> face.smoothingGroup)
            .filter(Objects::nonNull)
            .distinct()
            .count();
        lblSmoothingGroups.setText(NUMBER_FORMAT.format(smoothingGroupsCount));

        final int materialCount = obj.materialLibsMap.values().stream()
            .mapToInt(Map::size)
            .sum();
        lblMaterials.setText(NUMBER_FORMAT.format(materialCount));
    }
}
