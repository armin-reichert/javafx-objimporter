/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

public final class InnerTreeNode extends NavigationTreeNode {
    public enum Type {Model, Object, Group, Material }

    public final String label;
    public final Type type;

    public InnerTreeNode(Type type, String label) {
        this.type = type;
        this.label = label;
    }
}
