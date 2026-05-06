/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

import javafx.scene.shape.MeshView;

import java.util.regex.Matcher;

import static de.amr.meshviewer.MeshViewerUI.ANON_OBJECT_PATTERN;

public final class MeshTreeNode extends ModelTreeNode {

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
    public boolean shortName;

    public MeshTreeNode(String meshName, MeshView meshView, boolean shortName) {
        this.meshName = meshName;
        this.meshView = meshView;
        this.shortName = shortName;
    }

    public void setShortName(boolean shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName ? removeAnonObjectPrefix(meshName) : meshName;
    }
}
