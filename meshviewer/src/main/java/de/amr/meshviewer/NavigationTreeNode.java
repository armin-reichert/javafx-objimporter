/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public sealed class NavigationTreeNode permits InnerTreeNode, MeshNode {
    public final BooleanProperty checked = new SimpleBooleanProperty(false);
}
