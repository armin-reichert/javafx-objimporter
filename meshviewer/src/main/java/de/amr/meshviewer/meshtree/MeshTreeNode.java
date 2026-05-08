/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer.meshtree;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public sealed class MeshTreeNode permits InnerTreeNode, MeshTreeLeaf {
    public final BooleanProperty checked = new SimpleBooleanProperty(false);
}
