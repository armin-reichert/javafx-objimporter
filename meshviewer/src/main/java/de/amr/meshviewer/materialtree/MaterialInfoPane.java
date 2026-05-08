package de.amr.meshviewer.materialtree;

import de.amr.meshviewer.ObjModelFX;
import javafx.scene.layout.VBox;

import java.util.Map;

public class MaterialInfoPane extends VBox {

    private final MaterialTreeView treeView;

    public MaterialInfoPane() {
        treeView = new MaterialTreeView();
        getChildren().add(treeView);
    }

    public void update(ObjModelFX fxModel) {
        treeView.populate(fxModel.materialsMap());
    }

    public void clear() {
        treeView.populate(Map.of());
    }

    public MaterialTreeView treeView() {
        return treeView;
    }
}
