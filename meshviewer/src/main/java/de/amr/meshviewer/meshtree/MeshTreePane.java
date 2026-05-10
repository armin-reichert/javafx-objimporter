/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.meshtree;

import de.amr.meshviewer.ObjModelFX;
import de.amr.objparser.ObjModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.layout.VBox;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class MeshTreePane extends VBox {

    public static final String NO_OBJ_MODEL_TITLE = "No OBJ model";

    public static final String CSS_ID_OBJ_MODEL_TREE = "objModelTree";

    public final BooleanProperty shortMeshViewNames = new SimpleBooleanProperty(true);

    private final MeshTreeView meshTreeView;

    public MeshTreePane(double minWidth) {
        setMinWidth(minWidth);

        meshTreeView = new MeshTreeView(CSS_ID_OBJ_MODEL_TREE);
        meshTreeView.showShortMeshNames.bind(shortMeshViewNames);
        meshTreeView.prefHeightProperty().bind(heightProperty().subtract(1));
        getChildren().add(meshTreeView);
    }

    public MeshTreeView modelTreeView() {
        return meshTreeView;
    }

    public void update(ObjModelFX fxModel)
    {
        final String url = fxModel.objModel().url();
        final String title = URLDecoder.decode(url.substring(url.lastIndexOf('/') + 1), StandardCharsets.UTF_8);
        meshTreeView.populate(title, fxModel);
        meshTreeView.clearMeshSelection();
    }

    public void clear() {
        meshTreeView.populate(NO_OBJ_MODEL_TITLE, ObjModelFX.EMPTY);
    }

    public void setInitialSelection() {
        meshTreeView.selectAllMeshesFromCategory(InnerTreeNode.NodeCategory.MeshesByMaterials);
        meshTreeView.getRoot().getChildren().forEach(node -> node.setExpanded(false));
        meshTreeView.getRoot().getChildren().getLast().setExpanded(true); // Expand materials node
    }

    public void setInitialExpansionState() {
        meshTreeView.getRoot().getChildren().forEach(node -> node.setExpanded(false));
        meshTreeView.getRoot().getChildren().getLast().setExpanded(true); // Expand materials node
    }
}
