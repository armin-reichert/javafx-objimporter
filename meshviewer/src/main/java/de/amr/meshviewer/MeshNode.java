/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

import javafx.scene.shape.MeshView;

import java.util.regex.Matcher;

import static de.amr.meshviewer.MeshViewerUI.ANON_OBJECT_PATTERN;

public final class MeshNode extends NavigationTreeNode {

    private static String removeAnonObjectPrefix(String text) {
        if (text != null) {
            final Matcher m = ANON_OBJECT_PATTERN.matcher(text);
            if (m.matches()) {
                return m.group(1); // the OBJ file group name part
            }
        }
        return text;
    }

    public final String meshName;
    public final MeshView meshView;

    public MeshNode(String meshName, MeshView meshView) {
        this.meshName = meshName;
        this.meshView = meshView;
    }

    @Override
    public String toString() {
        return removeAnonObjectPrefix(meshName);
    }
}
