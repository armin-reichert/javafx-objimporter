/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.tree;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public sealed class TreeNode permits InnerTreeNode, MeshTreeNode {
    public final BooleanProperty checked = new SimpleBooleanProperty(false);
}
