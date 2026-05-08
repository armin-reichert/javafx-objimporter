/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.modeltree;

import de.amr.objparser.ObjModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.layout.VBox;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ModelTreePane extends VBox {

    public static final String NO_OBJ_MODEL_TITLE = "No OBJ model";

    public static final String CSS_ID_OBJ_MODEL_TREE = "objModelTree";

    public final BooleanProperty shortMeshViewNames = new SimpleBooleanProperty(true);

    private final ModelTreeView modelTreeView;

    public ModelTreePane(double minWidth) {
        setMinWidth(minWidth);

        modelTreeView = new ModelTreeView(CSS_ID_OBJ_MODEL_TREE);
        modelTreeView.showShortMeshNames.bind(shortMeshViewNames);
        modelTreeView.prefHeightProperty().bind(heightProperty().subtract(1));

        getChildren().add(modelTreeView);
    }

    public ModelTreeView modelTreeView() {
        return modelTreeView;
    }

    public void update(
        ObjModel objModel,
        Map<String, MeshView> objectMeshViews,
        Map<String, MeshView> groupMeshViews,
        Map<String, MeshView> materialMeshViews,
        Map<String, PhongMaterial> materialsMap)
    {
        final String url = objModel.url();
        final String title = URLDecoder.decode(url.substring(url.lastIndexOf('/') + 1), StandardCharsets.UTF_8);
        modelTreeView.populate(title, objectMeshViews, groupMeshViews, materialMeshViews, materialsMap);
        modelTreeView.clearMeshSelection();
    }

    public void clear() {
        modelTreeView.populate(NO_OBJ_MODEL_TITLE, Map.of(), Map.of(), Map.of(), Map.of());
    }

    public void setInitialSelection() {
        modelTreeView.selectAllMeshesFromCategory(InnerTreeNode.NodeCategory.MeshesByMaterials);
        modelTreeView.getRoot().getChildren().forEach(node -> node.setExpanded(false));
        modelTreeView.getRoot().getChildren().getLast().setExpanded(true); // Expand materials node
    }

    public void setInitialExpansionState() {
        modelTreeView.getRoot().getChildren().forEach(node -> node.setExpanded(false));
        modelTreeView.getRoot().getChildren().getLast().setExpanded(true); // Expand materials node
    }
}
