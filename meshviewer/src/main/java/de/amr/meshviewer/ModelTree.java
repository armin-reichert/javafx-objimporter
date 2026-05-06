package de.amr.meshviewer;

import de.amr.meshviewer.InnerTreeNode.NodeCategory;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import java.util.function.Consumer;

public class ModelTree extends TreeView<TreeNode> {

    private static String computeCategoryNodeLabel(NodeCategory category, boolean empty) {
        final String emptySuffix = empty ? " (empty)" : "";
        return switch (category) {
            case Objects -> "Mesh Views by Object" + emptySuffix;
            case Groups -> "Mesh Views by Group" + emptySuffix;
            case Materials -> "Mesh Views by Material" + emptySuffix;
            default -> "";
        };
    }

    private static <T> void traverse(TreeItem<T> item, Consumer<TreeItem<T>> visitor) {
        visitor.accept(item);
        for (TreeItem<T> child : item.getChildren()) {
            traverse(child, visitor);
        }
    }

    private final ObservableMap<NodeCategory, ObservableSet<TreeNode>> selection = FXCollections.observableHashMap();
    public final BooleanProperty shortMeshViewNames = new SimpleBooleanProperty(true);

    public ModelTree(String cssID) {
        setId(cssID);

        final var root = new TreeItem<TreeNode>(new InnerTreeNode(NodeCategory.Model, "No OBJ model loaded"));
        root.setExpanded(true);

        //setFocusTraversable(false);
        setRoot(root);
        setShowRoot(true);
        setCellFactory(CheckBoxTreeCell.forTreeView());

        for (NodeCategory category : NodeCategory.values()) {
            selection.put(category, FXCollections.observableSet());
        }

        shortMeshViewNames.addListener((_,_,shortName) -> {
            traverse(getRoot(), item -> {
                if (item.getValue() instanceof MeshTreeNode meshTreeNode) {
                    meshTreeNode.setShortName(shortName);
                }
            });
            refresh();
        });

        // To be able to e.g. hide the check box for the root, we need an explicit cell factory
        setCellFactory(_ -> new CheckBoxTreeCell<TreeNode>() {
            @Override
            public void updateItem(TreeNode node, boolean empty) {
                super.updateItem(node, empty);

                if (empty || node == null) {
                    return;
                }

                TreeItem<TreeNode> item = getTreeItem();

                // Hide checkbox for the root
                if (item.getParent() == null) {
                    // Option A: hide the checkbox completely
                    setGraphic(null);
                }

                // Set the text for all nodes
                setText(node.toString());
            }
        });

        // Disable root selection
        getSelectionModel().selectedItemProperty().addListener((_, oldItem, newItem) -> {
            if (newItem == getRoot()) {
                // Revert selection
                getSelectionModel().select(oldItem);
            }
        });

    }

    public void clearSelectedNodeSets() {
        for (NodeCategory category : NodeCategory.values()) {
            selection.get(category).clear();
        }
    }

    public ObservableMap<NodeCategory, ObservableSet<TreeNode>> selection() {
        return selection;
    }

    public void populate(
        String title,
        Map<String, MeshView> objectMeshViews,
        Map<String, MeshView> groupMeshViews,
        Map<String, MeshView> materialMeshViews)
    {
        final TreeItem<TreeNode> root = getRoot();
        root.setValue(new InnerTreeNode(NodeCategory.Model, title));
        root.getChildren().clear();
        addLevel(NodeCategory.Objects,   objectMeshViews);
        addLevel(NodeCategory.Groups,    groupMeshViews);
        addLevel(NodeCategory.Materials, materialMeshViews);
    }

    private void addLevel(NodeCategory category, Map<String, MeshView> meshViews) {
        final String title = computeCategoryNodeLabel(category, meshViews.isEmpty());
        final TreeNode rootNode = new InnerTreeNode(category, title);

        // Subtree root
        final CheckBoxTreeItem<TreeNode> rootItem = new CheckBoxTreeItem<>(rootNode);
        rootItem.setExpanded(true);

        // Select/deselect all children when root is selected/deselected
        rootItem.selectedProperty().addListener((_, _, selected) -> {
            rootItem.getChildren().forEach(child -> {
                final CheckBoxTreeItem<TreeNode> childItem = (CheckBoxTreeItem<TreeNode>) child;
                childItem.setSelected(selected);
                if (selected) {
                    selection.get(category).add(childItem.getValue());
                } else {
                    selection.get(category).remove(childItem.getValue());
                }
            });
        });

        // Children of subtree root
        meshViews.keySet().stream().sorted().forEach(meshName -> {
            final var childNode = new MeshTreeNode(meshName, meshViews.get(meshName), shortMeshViewNames.get());
            final CheckBoxTreeItem<TreeNode> childItem = new CheckBoxTreeItem<>(childNode);
            rootItem.getChildren().add(childItem);
            childItem.selectedProperty().bindBidirectional(childNode.checked);
            childItem.selectedProperty().addListener((_, _, selected) -> {
                Logger.debug("Tree node {}, selected={}", childNode, selected);
                Logger.debug("Selection before: {}", selection.get(category));
                if (selected) {
                    selection.get(category).add(childNode);
                }
                else {
                    selection.get(category).remove(childNode);
                }
                Logger.debug("Selection after: {}", selection.get(category));
            });
        });
        getRoot().getChildren().add(rootItem);
    }
}
