package de.amr.meshviewer;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.shape.MeshView;

import java.util.Map;
import java.util.regex.Matcher;

import static de.amr.meshviewer.MeshViewerUI.ANON_OBJECT_PATTERN;

public class ObjModelNavigationTree extends TreeView<NavigationTreeNode> {

    public ObjModelNavigationTree(String cssID) {
        setId(cssID);

        final var root = new TreeItem<NavigationTreeNode>(new InnerTreeNode(InnerTreeNode.Type.Model, "No OBJ model loaded"));
        root.setExpanded(true);

        //setFocusTraversable(false);
        setRoot(root);
        setShowRoot(true);

        showLabelInTreeNode();
        //showCheckBoxAndLabelInTreeNode();
    }

    private void showLabelInTreeNode() {
        setCellFactory(_ -> new TreeCell<>() {
            @Override
            protected void updateItem(NavigationTreeNode value, boolean empty) {
                super.updateItem(value, empty);

                if (empty || value == null) {
                    setText(null);
                    return;
                }

                final String label = switch (value) {
                    case InnerTreeNode innerTreeNode -> innerTreeNode.label;
                    case MeshNode meshNode -> removeAnonObjectPrefix(meshNode.meshName);
                    default -> "Unknown tree node";
                };
                setText(label);
            }
        });
    }

    private void showCheckBoxAndLabelInTreeNode() {
        setCellFactory(tv -> new TreeCell<>() {
            private final CheckBox checkBox = new CheckBox();
            private final Label label = new Label();
            private final HBox box = new HBox(6, checkBox, label);

            @Override
            protected void updateItem(NavigationTreeNode value, boolean empty) {
                super.updateItem(value, empty);

                final String labelText = switch (value) {
                    case null -> "";
                    case InnerTreeNode innerTreeNode -> innerTreeNode.label;
                    case MeshNode meshNode -> removeAnonObjectPrefix(meshNode.meshName);
                    default -> "Unknown tree node";
                };
                if (empty || value == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);      // important: don't use default text rendering
                    setGraphic(box);    // your custom UI
                    label.setText(labelText);
                }
            }
        });
    }

    private String removeAnonObjectPrefix(String text) {
        if (text != null) {
            final Matcher m = ANON_OBJECT_PATTERN.matcher(text);
            if (m.matches()) {
                return m.group(1); // the OBJ file group name part
            }
        }
        return text;
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
