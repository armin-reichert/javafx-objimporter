package de.amr.meshviewer.materialtree;

import javafx.scene.paint.PhongMaterial;

public final class PhongMaterialTreeNode extends MaterialTreeNode {
    public final String materialName;
    public final PhongMaterial material;

    public PhongMaterialTreeNode(String materialName, PhongMaterial material) {
        this.materialName = materialName;
        this.material = material;
    }

    @Override
    public String toString() {
        return materialName;
    }
}
