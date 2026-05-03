package de.amr.meshviewer;

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.shape.MeshView;

import java.util.Map;

public class ObjModelNavigationTree extends TreeView<NavigationTreeNode> {

    public ObjModelNavigationTree() {
        final var root = new TreeItem<NavigationTreeNode>(new InnerTreeNode(InnerTreeNode.Type.Model, "No OBJ model loaded"));
        root.setExpanded(true);

        setFocusTraversable(false);
        setRoot(root);
        setShowRoot(true);

        setCellFactory(_ -> new TreeCell<>() {
            @Override
            protected void updateItem(NavigationTreeNode value, boolean empty) {
                super.updateItem(value, empty);

                if (empty || value == null) {
                    setText(null);
                    return;
                }

                setText(switch (value) {
                    case InnerTreeNode innerTreeNode -> innerTreeNode.label;
                    case MeshNode meshNode -> meshNode.meshName;
                    default -> "Unknown tree node";
                });
            }
        });
    }

    public void populate(
        String title,
        Map<String, MeshView> objectMeshViews,
        Map<String, MeshView> groupMeshViews,
        Map<String, MeshView> materialMeshViews)
    {
        final TreeItem<NavigationTreeNode> root = getRoot();
        root.setValue(new InnerTreeNode(InnerTreeNode.Type.Model, title));
        root.getChildren().clear();
        addLevel(InnerTreeNode.Type.Object,   objectMeshViews);
        addLevel(InnerTreeNode.Type.Group,    groupMeshViews);
        addLevel(InnerTreeNode.Type.Material, materialMeshViews);
    }

    private void addLevel(InnerTreeNode.Type type, Map<String, MeshView> meshViews) {
        String title = switch (type) {
            case Object  -> "Mesh Views by Object";
            case Group -> "Mesh Views by Group";
            case Material  -> "Mesh Views by Material";
            default -> "";
        };
        if (meshViews.isEmpty()) title += " (None)";
        final TreeItem<NavigationTreeNode> root = new TreeItem<>(new InnerTreeNode(type, title));
        root.setExpanded(true);
        meshViews.keySet().stream().sorted().forEach(meshName -> {
            final var meshNode = new MeshNode(meshName, meshViews.get(meshName));
            root.getChildren().add(new TreeItem<>(meshNode));
        });
        getRoot().getChildren().add(root);
    }
}
