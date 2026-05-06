/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

public final class InnerTreeNode extends ModelTreeNode {

    public enum NodeCategory {Model, Objects, Groups, Materials}

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
