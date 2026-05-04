package de.amr.meshviewer;

import de.amr.meshviewer.InnerTreeNode.NodeCategory;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.shape.MeshView;

import java.util.Map;

public class ObjModelNavigationTree extends TreeView<NavigationTreeNode> {


    private static String computeCategoryNodeLabel(NodeCategory category, boolean empty) {
        final String emptySuffix = empty ? " (empty)" : "";
        return switch (category) {
            case Objects -> "Mesh Views by Object" + emptySuffix;
            case Groups -> "Mesh Views by Group" + emptySuffix;
            case Materials -> "Mesh Views by Material" + emptySuffix;
            default -> "";
        };
    }

    public ObjModelNavigationTree(String cssID) {
        setId(cssID);

        final var root = new TreeItem<NavigationTreeNode>(new InnerTreeNode(NodeCategory.Model, "No OBJ model loaded"));
        root.setExpanded(true);

        //setFocusTraversable(false);
        setRoot(root);
        setShowRoot(true);

        setCellFactory(CheckBoxTreeCell.forTreeView());
        //showLabelInTreeNode();
    }

    public void populate(
        String title,
        Map<String, MeshView> objectMeshViews,
        Map<String, MeshView> groupMeshViews,
        Map<String, MeshView> materialMeshViews)
    {
        final TreeItem<NavigationTreeNode> root = getRoot();
        root.setValue(new InnerTreeNode(NodeCategory.Model, title));
        root.getChildren().clear();
        addLevel(NodeCategory.Objects,   objectMeshViews);
        addLevel(NodeCategory.Groups,    groupMeshViews);
        addLevel(NodeCategory.Materials, materialMeshViews);
    }

    private void addLevel(NodeCategory category, Map<String, MeshView> meshViews) {
        final String title = computeCategoryNodeLabel(category, meshViews.isEmpty());
        final TreeItem<NavigationTreeNode> rootItem = new TreeItem<>(new InnerTreeNode(category, title));
        rootItem.setExpanded(true);
        meshViews.keySet().stream().sorted().forEach(meshName -> {
            final var meshNode = new MeshNode(meshName, meshViews.get(meshName));
            rootItem.getChildren().add(new CheckBoxTreeItem<>(meshNode));
        });
        getRoot().getChildren().add(rootItem);
    }
}
