package de.amr.meshviewer;

import de.amr.meshviewer.InnerTreeNode.NodeCategory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.shape.MeshView;
import org.tinylog.Logger;

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

    private final ObservableMap<NodeCategory, ObservableSet<NavigationTreeNode>> selection = FXCollections.observableHashMap();

    public ObjModelNavigationTree(String cssID) {
        setId(cssID);

        final var root = new TreeItem<NavigationTreeNode>(new InnerTreeNode(NodeCategory.Model, "No OBJ model loaded"));
        root.setExpanded(true);

        //setFocusTraversable(false);
        setRoot(root);
        setShowRoot(true);
        setCellFactory(CheckBoxTreeCell.forTreeView());

        for (NodeCategory category : NodeCategory.values()) {
            selection.put(category, FXCollections.observableSet());
        }
    }

    public void clearSelection() {
        for (NodeCategory category : NodeCategory.values()) {
            selection.get(category).clear();
        }
    }

    public ObservableMap<NodeCategory, ObservableSet<NavigationTreeNode>> selection() {
        return selection;
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
        final NavigationTreeNode rootNode = new InnerTreeNode(category, title);

        // Subtree root
        final CheckBoxTreeItem<NavigationTreeNode> rootItem = new CheckBoxTreeItem<>(rootNode);
        rootItem.setExpanded(true);

        // Select/deselect all children when root is selected/deselected
        rootItem.selectedProperty().addListener((_, _, selected) -> {
            rootItem.getChildren().forEach(child -> {
                final CheckBoxTreeItem<NavigationTreeNode> childItem = (CheckBoxTreeItem<NavigationTreeNode>) child;
                childItem.setSelected(selected);
            });
        });

        // Children of subtree root
        meshViews.keySet().stream().sorted().forEach(meshName -> {
            final var childNode = new MeshNode(meshName, meshViews.get(meshName));
            final CheckBoxTreeItem<NavigationTreeNode> childItem = new CheckBoxTreeItem<>(childNode);
            rootItem.getChildren().add(childItem);
            childItem.selectedProperty().bindBidirectional(childNode.checked);
            //TODO consider "independent" property etc.
            childItem.selectedProperty().addListener((_, _, selected) -> {
                Logger.info("Tree node {}, selected={}", childNode, selected);
                if (selected) {
                    selection.get(category).add(childNode);
                }
                else {
                    selection.get(category).remove(childNode);
                }
            });
        });
        getRoot().getChildren().add(rootItem);
    }
}
