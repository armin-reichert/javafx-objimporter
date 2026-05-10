/*
 * Copyright (c) 2021-2026 Armin Reichert (MIT License)
 */
package de.amr.meshviewer.preview;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

/**
 * Shows coordinates axes (x-axis=red, y-axis=green, z-axis=blue).
 */
public class CoordinateSystem extends Group {

    public static final double MARKER_RADIUS = 0.02;
    public static final double AXIS_RADIUS = 0.01;

    public CoordinateSystem() {
        this(1000);
    }

    public CoordinateSystem(double axisLength) {
        Sphere origin = new Sphere(MARKER_RADIUS);
        origin.setMaterial(new PhongMaterial(Color.CHOCOLATE));

        Cylinder xAxis = createAxis(Color.RED.brighter(), axisLength);
        xAxis.getTransforms().add(new Rotate(90, Rotate.Z_AXIS));
        Sphere xAxisMarker = new Sphere(MARKER_RADIUS);
        xAxisMarker.setMaterial(new PhongMaterial(Color.RED));
        xAxisMarker.setTranslateX(1);

        Cylinder yAxis = createAxis(Color.GREEN.brighter(), axisLength);
        Sphere yAxisMarker = new Sphere(MARKER_RADIUS);
        yAxisMarker.setMaterial(new PhongMaterial(Color.GREEN));
        yAxisMarker.setTranslateY(1);

        Cylinder zAxis = createAxis(Color.BLUE.brighter(), axisLength / 2);
        zAxis.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
        Sphere zAxisMarker = new Sphere(MARKER_RADIUS);
        zAxisMarker.setMaterial(new PhongMaterial(Color.BLUE));
        zAxisMarker.setTranslateZ(1);

        getChildren().addAll(origin, xAxis, xAxisMarker, yAxis, yAxisMarker, zAxis, zAxisMarker);
    }

    // Cylinder height points to y-direction
    private Cylinder createAxis(Color color, double height) {
        Cylinder axis = new Cylinder(AXIS_RADIUS, height);
        axis.setMaterial(new PhongMaterial(color));
        return axis;
    }
}