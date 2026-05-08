/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.materialtree;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.paint.PhongMaterial;

import java.util.Map;

public class MaterialTreeView extends TreeView<MaterialTreeNode> {

    public MaterialTreeView() {
        final TreeItem<MaterialTreeNode> materialsNode = new TreeItem<>(new LabelNode("Materials"));
        setRoot(materialsNode);
        materialsNode.setExpanded(true);
    }

    public void populate(Map<String, PhongMaterial> materials) {
        getRoot().getChildren().clear();
        materials.forEach((name, material) -> {
            final TreeItem<MaterialTreeNode> item = new TreeItem<>(new PhongMaterialTreeNode(name, material));
            getRoot().getChildren().add(item);
        });
    }
}
