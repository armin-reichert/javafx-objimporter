package de.amr.meshviewer;

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.shape.MeshView;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ObjModelNavigationTree extends TreeView<NavigationTreeNode> {

    public static final Pattern ANON_OBJECT_PATTERN = Pattern.compile("^Object\\.anon_\\d+\\.(.+)$");

    public ObjModelNavigationTree(String cssID) {
        setId(cssID);

        final var root = new TreeItem<NavigationTreeNode>(new InnerTreeNode(InnerTreeNode.Type.Model, "No OBJ model loaded"));
        root.setExpanded(true);

        //setFocusTraversable(false);
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

                final String label = switch (value) {
                    case InnerTreeNode innerTreeNode -> innerTreeNode.label;
                    case MeshNode meshNode -> removeAnonObjectPrefix(meshNode.meshName);
                    default -> "Unknown tree node";
                };
                setText(label);
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
