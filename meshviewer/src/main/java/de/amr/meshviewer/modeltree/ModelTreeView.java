package de.amr.meshviewer.modeltree;

import de.amr.meshviewer.modeltree.InnerTreeNode.NodeCategory;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import org.tinylog.Logger;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class ModelTreeView extends TreeView<ModelTreeNode> {

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

    private final ObservableMap<NodeCategory, ObservableSet<ModelTreeNode>> meshSelection = FXCollections.observableHashMap();

    public final BooleanProperty showShortMeshNames = new SimpleBooleanProperty(true);

    public ModelTreeView(String cssID) {
        setId(cssID);

        final var root = new TreeItem<ModelTreeNode>(new InnerTreeNode(NodeCategory.Model, "No OBJ model loaded"));
        root.setExpanded(true);

        //setFocusTraversable(false);
        setRoot(root);
        setShowRoot(true);
        setCellFactory(CheckBoxTreeCell.forTreeView());

        meshSelection.put(NodeCategory.MeshesByObjects, FXCollections.observableSet());
        meshSelection.put(NodeCategory.MeshesByGroups, FXCollections.observableSet());
        meshSelection.put(NodeCategory.MeshesByMaterials, FXCollections.observableSet());

        showShortMeshNames.addListener((_, _, shortName) -> {
            traverse(getRoot(), item -> {
                if (item.getValue() instanceof MeshTreeNode meshTreeNode) {
                    meshTreeNode.setShortName(shortName);
                }
            });
            refresh();
        });

        // To be able to e.g. hide the check box for the root, we need an explicit cell factory
        setCellFactory(_ -> new CheckBoxTreeCell<>() {
            @Override
            public void updateItem(ModelTreeNode node, boolean empty) {
                super.updateItem(node, empty);

                if (empty || node == null) {
                    return;
                }

                final TreeItem<ModelTreeNode> item = getTreeItem();
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
            else if (newItem.getValue() instanceof MeshTreeNode) {
                // Select corresponding category node
                getSelectionModel().select(newItem.getParent());
            }
        });
    }

    public void clearMeshSelection() {
        meshSelection.values().forEach(Set::clear);
    }

    public void selectAllMeshesFromCategory(NodeCategory category) {
        final int childIndex = switch (category) {
            case Model ->
                throw new IllegalArgumentException("Category %s not allowed here".formatted(category));
            case MeshesByObjects -> 0;
            case MeshesByGroups -> 1;
            case MeshesByMaterials -> 2;
        };

        final CheckBoxTreeItem<ModelTreeNode> categoryCheckBox = (CheckBoxTreeItem<ModelTreeNode>) getRoot().getChildren().get(childIndex);
        getSelectionModel().clearSelection();
        getSelectionModel().select(categoryCheckBox);

        // Select checkboxes for groups category and groups mesh nodes
        categoryCheckBox.setSelected(true);
        categoryCheckBox.getChildren().stream()
            .filter(CheckBoxTreeItem.class::isInstance).map(CheckBoxTreeItem.class::cast)
            .forEach(node -> node.setSelected(true));
    }

    public ObservableMap<NodeCategory, ObservableSet<ModelTreeNode>> selection() {
        return meshSelection;
    }

    public void populate(
        String title,
        Map<String, MeshView> objectMeshViews,
        Map<String, MeshView> groupMeshViews,
        Map<String, MeshView> materialMeshViews,
        Map<String, PhongMaterial> materials)
    {
        getRoot().setValue(new InnerTreeNode(NodeCategory.Model, title));
        getRoot().getChildren().clear();

        addMeshViewTreeNodes(NodeCategory.MeshesByObjects,   objectMeshViews);
        addMeshViewTreeNodes(NodeCategory.MeshesByGroups,    groupMeshViews);
        addMeshViewTreeNodes(NodeCategory.MeshesByMaterials, materialMeshViews);
    }

    private void addMeshViewTreeNodes(NodeCategory category, Map<String, MeshView> meshViews) {
        final String title = computeCategoryNodeLabel(category, meshViews.isEmpty());
        final ModelTreeNode rootNode = new InnerTreeNode(category, title);

        // Subtree root
        final CheckBoxTreeItem<ModelTreeNode> rootItem = new CheckBoxTreeItem<>(rootNode);
        rootItem.setExpanded(true);

        // Select/deselect all children when root is selected/deselected
        rootItem.selectedProperty().addListener((_, _, selected) -> rootItem.getChildren().forEach(child -> {
            final CheckBoxTreeItem<ModelTreeNode> childItem = (CheckBoxTreeItem<ModelTreeNode>) child;
            childItem.setSelected(selected);
            if (selected) {
                meshSelection.get(category).add(childItem.getValue());
            } else {
                meshSelection.get(category).remove(childItem.getValue());
            }
        }));

        // Children of subtree root
        meshViews.keySet().stream().sorted().forEach(meshName -> {
            final var childNode = new MeshTreeNode(meshName, meshViews.get(meshName), showShortMeshNames.get());
            final CheckBoxTreeItem<ModelTreeNode> childItem = new CheckBoxTreeItem<>(childNode);
            rootItem.getChildren().add(childItem);
            childItem.selectedProperty().bindBidirectional(childNode.checked);
            childItem.selectedProperty().addListener((_, _, selected) -> {
                Logger.debug("Tree node {}, selected={}", childNode, selected);
                Logger.debug("Selection before: {}", meshSelection.get(category));
                if (selected) {
                    meshSelection.get(category).add(childNode);
                }
                else {
                    meshSelection.get(category).remove(childNode);
                }
                Logger.debug("Selection after: {}", meshSelection.get(category));
            });
        });
        getRoot().getChildren().add(rootItem);
    }
}
