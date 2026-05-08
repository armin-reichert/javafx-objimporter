/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.meshtree;

/**
 * Type of inner nodes in mesh views modeltree.
 */
public final class InnerTreeNode extends MeshTreeNode {

    /** The inner node categories. */
    public enum NodeCategory {
        Model,
        MeshesByObjects,
        MeshesByGroups,
        MeshesByMaterials,
    }

    public final String label;
    public final NodeCategory nodeCategory;

    public InnerTreeNode(NodeCategory nodeCategory, String label) {
        this.nodeCategory = nodeCategory;
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
