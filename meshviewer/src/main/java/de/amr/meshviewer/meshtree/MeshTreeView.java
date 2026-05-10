/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.meshtree;

import de.amr.meshviewer.ObjModelFX;
import de.amr.meshviewer.meshtree.InnerTreeNode.NodeCategory;
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
import java.util.Set;
import java.util.function.Consumer;

public class MeshTreeView extends TreeView<MeshTreeNode> {

    private static String computeCategoryNodeLabel(NodeCategory category, boolean empty) {
        final String emptySuffix = empty ? " (empty)" : "";
        return switch (category) {
            case MeshesByObjects -> "Mesh Views by Object" + emptySuffix;
            case MeshesByGroups -> "Mesh Views by Group" + emptySuffix;
            case MeshesByMaterials -> "Mesh Views by Material" + emptySuffix;
            default -> "";
        };
    }

    private static <T> void traverse(TreeItem<T> item, Consumer<TreeItem<T>> visitor) {
        visitor.accept(item);
        for (TreeItem<T> child : item.getChildren()) {
            traverse(child, visitor);
        }
    }

    private final ObservableMap<NodeCategory, ObservableSet<MeshTreeNode>> nodeSelection = FXCollections.observableHashMap();

    public final BooleanProperty showShortMeshNames = new SimpleBooleanProperty(true);

    public MeshTreeView(String cssID) {
        setId(cssID);

        final var root = new TreeItem<MeshTreeNode>(new InnerTreeNode(NodeCategory.Model, "No OBJ model loaded"));
        root.setExpanded(true);

        //setFocusTraversable(false);
        setRoot(root);
        setShowRoot(true);
        setCellFactory(CheckBoxTreeCell.forTreeView());

        nodeSelection.put(NodeCategory.MeshesByObjects, FXCollections.observableSet());
        nodeSelection.put(NodeCategory.MeshesByGroups, FXCollections.observableSet());
        nodeSelection.put(NodeCategory.MeshesByMaterials, FXCollections.observableSet());

        showShortMeshNames.addListener((_, _, shortName) -> {
            traverse(getRoot(), item -> {
                if (item.getValue() instanceof MeshTreeLeaf meshTreeLeaf) {
                    meshTreeLeaf.setShortName(shortName);
                }
            });
            refresh();
        });

        // To be able to e.g. hide the check box for the root, we need an explicit cell factory
        setCellFactory(_ -> new CheckBoxTreeCell<>() {
            @Override
            public void updateItem(MeshTreeNode node, boolean empty) {
                super.updateItem(node, empty);

                if (empty || node == null) {
                    return;
                }

                final TreeItem<MeshTreeNode> item = getTreeItem();
                if (item.getParent() == null) {
                    setGraphic(null);
                }
                // Set the text for all nodes
                setText(node.toString());
            }
        });

        // Disable root selection
        getSelectionModel().selectedItemProperty().addListener((_, oldItem, newItem) -> {
            if (newItem == null) {
                return;
            }
            if (newItem == getRoot()) { // Revert selection
                getSelectionModel().select(oldItem);
            }
            else if (newItem.getValue() instanceof MeshTreeLeaf) {
                // Select corresponding category node
                getSelectionModel().select(newItem.getParent());
            }
        });
    }

    public void clearMeshSelection() {
        nodeSelection.values().forEach(Set::clear);
    }

    public void selectAllMeshesFromCategory(NodeCategory category) {
        final int childIndex = switch (category) {
            case Model ->
                throw new IllegalArgumentException("Category %s not allowed here".formatted(category));
            case MeshesByObjects -> 0;
            case MeshesByGroups -> 1;
            case MeshesByMaterials -> 2;
        };

        final CheckBoxTreeItem<MeshTreeNode> categoryCheckBox = (CheckBoxTreeItem<MeshTreeNode>) getRoot().getChildren().get(childIndex);
        getSelectionModel().clearSelection();
        getSelectionModel().select(categoryCheckBox);

        // Select checkboxes for groups category and groups mesh nodes
        categoryCheckBox.setSelected(true);
        categoryCheckBox.getChildren().stream()
            .filter(CheckBoxTreeItem.class::isInstance).map(CheckBoxTreeItem.class::cast)
            .forEach(node -> node.setSelected(true));
    }

    public ObservableMap<NodeCategory, ObservableSet<MeshTreeNode>> selection() {
        return nodeSelection;
    }

    // TODO: Avoid creating all mesh views when tree is populated. Expand only one category
    //       and create its mesh views. Create mesh views for other categories when their root is expanded.
    public void populate(String title, ObjModelFX fxModel)
    {
        getRoot().setValue(new InnerTreeNode(NodeCategory.Model, title));
        getRoot().getChildren().clear();

        addMeshViewTreeNodes(NodeCategory.MeshesByObjects,   fxModel.objectMeshViews());
        addMeshViewTreeNodes(NodeCategory.MeshesByGroups,    fxModel.groupMeshViews());
        addMeshViewTreeNodes(NodeCategory.MeshesByMaterials, fxModel.materialMeshViews());
    }

    private void addMeshViewTreeNodes(NodeCategory category, Map<String, MeshView> meshViews) {
        final String title = computeCategoryNodeLabel(category, meshViews.isEmpty());
        final MeshTreeNode rootNode = new InnerTreeNode(category, title);

        // Subtree root
        final CheckBoxTreeItem<MeshTreeNode> rootItem = new CheckBoxTreeItem<>(rootNode);
        rootItem.setExpanded(true);

        // Select/deselect all children when root is selected/deselected
        rootItem.selectedProperty().addListener((_, _, selected) -> rootItem.getChildren().forEach(child -> {
            final CheckBoxTreeItem<MeshTreeNode> childItem = (CheckBoxTreeItem<MeshTreeNode>) child;
            childItem.setSelected(selected);
            if (selected) {
                nodeSelection.get(category).add(childItem.getValue());
            } else {
                nodeSelection.get(category).remove(childItem.getValue());
            }
        }));

        // Children of subtree root
        meshViews.keySet().stream().sorted().forEach(meshName -> {
            final var childNode = new MeshTreeLeaf(meshName, meshViews.get(meshName), showShortMeshNames.get());
            final CheckBoxTreeItem<MeshTreeNode> childItem = new CheckBoxTreeItem<>(childNode);
            rootItem.getChildren().add(childItem);
            childItem.selectedProperty().bindBidirectional(childNode.checked);
            childItem.selectedProperty().addListener((_, _, selected) -> {
                Logger.debug("Tree node {}, selected={}", childNode, selected);
                Logger.debug("Selection before: {}", nodeSelection.get(category));
                if (selected) {
                    nodeSelection.get(category).add(childNode);
                }
                else {
                    nodeSelection.get(category).remove(childNode);
                }
                Logger.debug("Selection after: {}", nodeSelection.get(category));
            });
        });
        getRoot().getChildren().add(rootItem);
    }
}
