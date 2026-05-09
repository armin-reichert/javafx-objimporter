/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

import de.amr.meshviewer.info.SampleInfo;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.shape.DrawMode;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class MeshViewerMenus {

    private final MeshViewerUI ui;
    private final MenuBar menuBar;
    private final Menu samplesMenu;

    public MeshViewerMenus(MeshViewerUI ui) {
        this.ui = ui;
        this.menuBar = new MenuBar();

        // -----------------------------
        // File menu
        // -----------------------------

        Menu fileMenu = new Menu("File");

        MenuItem openItem = new MenuItem("Open OBJ…");
        MenuItem exitItem = new MenuItem("Exit");

        fileMenu.getItems().addAll(openItem, exitItem);

        openItem.setOnAction(_ -> {
            if (ui.currentModelDir() != null && ui.currentModelDir().exists()) {
                ui.fileChooser().setInitialDirectory(ui.currentModelDir());
            }
            final File objFile = ui.fileChooser().showOpenDialog(ui.stage());
            if (objFile != null) {
                try {
                    ui.showObjModel(objFile);
                } catch (IOException x) {
                    Logger.error(x, "Cannot show OBJ model from file {}", objFile);
                    ui.previewArea().flash("Cannot show OBJ model");
                }
            }
        });

        exitItem.setOnAction(_ -> Platform.exit());

        // -----------------------------
        // View menu
        // -----------------------------

        final Menu viewMenu = new Menu("View");

        final CheckMenuItem miWireframe = new CheckMenuItem("Wireframe");
        miWireframe.selectedProperty().addListener((_, _, sel) -> ui.drawMode.set(sel ? DrawMode.LINE : DrawMode.FILL));
        ui.drawMode.addListener((_, _, mode) -> miWireframe.setSelected(mode == DrawMode.LINE));

        final CheckMenuItem miShortMeshViewNames = new CheckMenuItem("Short Mesh Names");
        miShortMeshViewNames.selectedProperty().bindBidirectional(ui.shortMeshViewNames);

        final CheckMenuItem miSelectionAreaVisible = new CheckMenuItem("Selection");
        miSelectionAreaVisible.selectedProperty().bindBidirectional(ui.selectionTabPane().visibleProperty());
        miSelectionAreaVisible.setOnAction(_ -> ui.showSelectionArea(miSelectionAreaVisible.isSelected()));

        final CheckMenuItem miInfoVisible = new CheckMenuItem("Info");
        miInfoVisible.selectedProperty().bindBidirectional(ui.infoArea().visibleProperty());
        miInfoVisible.setOnAction(_ -> ui.showInfoArea(miInfoVisible.isSelected()));

        viewMenu.getItems().addAll(miWireframe, miShortMeshViewNames, miSelectionAreaVisible, miInfoVisible);

        // -----------------------------
        // Samples menu
        // -----------------------------

        samplesMenu = new Menu("Samples");
        samplesMenu.disableProperty().bind(Bindings.isEmpty(ui.samples()));

        // -----------------------------
        // About menu
        // -----------------------------

        final var helpMenu = new Menu("?");

        final MenuItem miAbout = new MenuItem("About Mesh Viewer");
        miAbout.setOnAction(_ -> ui.aboutDialog().showAndWait());

        helpMenu.getItems().setAll(miAbout);

        menuBar.getMenus().setAll(fileMenu, viewMenu, samplesMenu, helpMenu);
    }

    public MenuBar menuBar() {
        return menuBar;
    }

    public Menu samplesMenu() {
        return samplesMenu;
    }

    public void addSample(Menu menu, SampleInfo sample) {
        final var menuItem = new MenuItem(sample.title());
        // Test if sample URL is accessible
        final URL url = getClass().getResource(sample.path() + sample.fileName());
        if (url != null) {
            menuItem.setOnAction(_ -> {
                try {
                    ui.showSampleModel(sample);
                } catch (IOException x) {
                    Logger.error(x, "Cannot show sample model");
                    ui.previewArea().flash("Cannot show sample model");
                }
            });
        } else {
            menuItem.setDisable(true);
        }
        menu.getItems().add(menuItem);
    }
}
