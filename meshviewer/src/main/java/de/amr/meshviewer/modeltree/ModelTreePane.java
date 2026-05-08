/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.modeltree;

import de.amr.meshviewer.ObjModelFX;
import de.amr.objparser.ObjModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.layout.VBox;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

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

    public void update(ObjModel objModel, ObjModelFX fxModel)
    {
        final String url = objModel.url();
        final String title = URLDecoder.decode(url.substring(url.lastIndexOf('/') + 1), StandardCharsets.UTF_8);
        modelTreeView.populate(title, fxModel);
        modelTreeView.clearMeshSelection();
    }

    public void clear() {
        modelTreeView.populate(NO_OBJ_MODEL_TITLE, ObjModelFX.EMPTY);
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
