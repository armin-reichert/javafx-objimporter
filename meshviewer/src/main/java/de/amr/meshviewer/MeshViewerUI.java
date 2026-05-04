/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

import de.amr.meshbuilder.MeshBuilder;
import de.amr.objparser.ObjFileParser;
import de.amr.objparser.ObjModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class MeshViewerUI {

    public static final String STAGE_TITLE = "JavaFX OBJ Mesh Viewer";
    public static final String NO_OBJ_MODEL_TITLE = "No OBJ model";

    public static final String CSS_ID_OBJ_MODEL_TREE = "objModelTree";
    public static final String CSS_ID_OBJ_MODEL_INFO_PANEL = "objModelInfo";

    public static final int SELECTION_AREA_WIDTH = 300;
    public static final int MODEL_INFO_AREA_WIDTH = 200;

    private final ObjectProperty<ObjModel> objModel = new SimpleObjectProperty<>();
    private final ObjectProperty<DrawMode> drawMode = new SimpleObjectProperty<>(DrawMode.FILL);

    private final ObjectProperty<Duration> parsingTime = new SimpleObjectProperty<>(Duration.ZERO);
    private final ObjectProperty<Duration> meshCreationTime = new SimpleObjectProperty<>(Duration.ZERO);

    private final ObservableList<SampleModel> sampleModels = FXCollections.observableArrayList();

    private Map<String, MeshView> currentObjectMeshViews;
    private Map<String, MeshView> currentGroupMeshViews;
    private Map<String, MeshView> currentMaterialMeshViews;
    private File currentModelDir;

    // UI
    private final Stage stage;
    private MenuBar menuBar;
    private Menu samplesMenu;
    private FileChooser fileChooser;

    // Layout
    private BorderPane rootPane;
    private final SplitPane splitLayout = new SplitPane();

    // Selection Area
    private Pane selectionArea;
    private ObjModelNavigationTree navigationTreeView;

    // Preview Area
    private PreviewArea previewArea;

    // Model Info Area
    private Pane infoArea;
    private ObjModelInfoPane infoPane;

    public MeshViewerUI(Stage stage, double width, double height) {
        this.stage = requireNonNull(stage);
        createUI(width, height);
        objModel.addListener(this::onObjModelChange);
    }

    private void onObjModelChange(ObservableValue<? extends ObjModel> ov, ObjModel oldModel, ObjModel newModel) {
        if (newModel != null) {
            createMeshViews(newModel);
            navigationTreeView.populate(createTreeTitle(newModel), currentObjectMeshViews, currentGroupMeshViews, currentMaterialMeshViews);
            selectAllGroupsNodeInNavigationTree();
            infoPane.update(newModel, parsingTime.get(), meshCreationTime.get());
        } else {
            currentObjectMeshViews = Map.of();
            currentGroupMeshViews = Map.of();
            currentMaterialMeshViews = Map.of();
            navigationTreeView.populate(NO_OBJ_MODEL_TITLE, currentObjectMeshViews, currentGroupMeshViews, currentMaterialMeshViews);
            infoPane.update(null, null, null);
        }
    }

    // Public

    public void show() {
        stage.show();
        if (!sampleModels.isEmpty()) {
            try {
                showObjModel(sampleModels.getFirst().url());
                previewArea.initSampleModel(sampleModels.getFirst());
            } catch (IOException x){
                Logger.error(x, "Cannot show first sample model");
                previewArea.flash("Cannot show sample model");
            }
        }
        showModelInfo(false);
    }

    public void addSampleModel(SampleModel sample) {
        requireNonNull(sample);
        sampleModels.add(sample);

        final var item = new MenuItem(sample.title());
        item.setOnAction(_ -> {
            try {
                showObjModel(sample.url());
                previewArea.initSampleModel(sample);
            } catch (IOException x) {
                Logger.error(x, "Cannot show sample model");
                previewArea.flash("Cannot show sample model");
            }
        });
        samplesMenu.getItems().add(item);
    }

    // Private

    private void showObjModel(File objFile) throws IOException {
        requireNonNull(objFile);
        loadModelFromURL(objFile.toURI().toURL());
        selectAllGroupsNodeInNavigationTree();
        currentModelDir = objFile.getParentFile();
        previewArea.reset();
        previewArea.assignFocusToSubScene();
    }

    private void showObjModel(URL url) throws IOException {
        requireNonNull(url);
        loadModelFromURL(url);
        selectAllGroupsNodeInNavigationTree();
        previewArea.reset();
        previewArea.assignFocusToSubScene();
    }

    private void createUI(double width, double height) {
        createLayout();

        final Scene scene = new Scene(rootPane);
        final URL cssURL = getClass().getResource("/app.css");
        if (cssURL != null) {
            scene.getStylesheets().add(cssURL.toExternalForm());
        } else {
            Logger.error("Cannot load app.css");
        }
        addFileDragNDropSupport(scene);
        createObjFileChooser();

        stage.setScene(scene);
        stage.setTitle(STAGE_TITLE);
        stage.setWidth(width);
        stage.setHeight(height);
    }

    private void createLayout() {
        createPreviewArea();
        createSelectionArea();
        createInfoArea();
        createMenus(stage);

        splitLayout.setOrientation(Orientation.HORIZONTAL);

        rootPane = new BorderPane();
        rootPane.setTop(menuBar);
        rootPane.setCenter(splitLayout);

        final var previewWidthReduction = Bindings.createDoubleBinding(
            () -> SELECTION_AREA_WIDTH + (infoArea.isVisible() ? infoArea.getWidth() : 0),
            infoArea.visibleProperty(), infoArea.widthProperty()
        );
        previewArea.subScene().widthProperty().bind(rootPane.widthProperty().subtract(previewWidthReduction));
        previewArea.subScene().heightProperty().bind(previewArea.heightProperty());
    }

    private void createObjFileChooser() {
        fileChooser = new FileChooser();
        fileChooser.setTitle("Open OBJ File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("OBJ Files", "*.obj"));
    }

    private void createPreviewArea() {
        previewArea = new PreviewArea();
        previewArea.drawMode.bindBidirectional(drawMode);
    }

    private void createSelectionArea() {
        navigationTreeView = new ObjModelNavigationTree(CSS_ID_OBJ_MODEL_TREE);
        navigationTreeView.getSelectionModel().selectedItemProperty().addListener((_, _, item) -> {
            Logger.info("Selected item: {}", item);
            if (item == null) return;
            switch (item.getValue()) {
                case MeshNode meshNode -> previewArea.displayMeshViews(Map.of(meshNode.meshName, meshNode.meshView));
                case InnerTreeNode innerNode -> {
                    switch (innerNode.type) {
                        case Model    -> {}
                        case Object   -> previewArea.displayMeshViews(currentObjectMeshViews);
                        case Group    -> previewArea.displayMeshViews(currentGroupMeshViews);
                        case Material -> previewArea.displayMeshViews(currentMaterialMeshViews);
                    }
                }
                default -> {}
            }
        });

        selectionArea = new VBox(navigationTreeView);
        selectionArea.setMinWidth(SELECTION_AREA_WIDTH);

        navigationTreeView.prefHeightProperty().bind(selectionArea.heightProperty().subtract(1));
    }

    private void selectAllGroupsNodeInNavigationTree() {
        final TreeItem<NavigationTreeNode> root = navigationTreeView.getRoot();
        if (root.getChildren().size() >= 2) {
            final TreeItem<NavigationTreeNode> allGroups = root.getChildren().get(1);
            navigationTreeView.getSelectionModel().select(allGroups);
        }
    }

    private String createTreeTitle(ObjModel objModel) {
        final String url = objModel.url();
        return URLDecoder.decode(url.substring(url.lastIndexOf('/') + 1), StandardCharsets.UTF_8);
    }

    private void createInfoArea() {
        infoPane = new ObjModelInfoPane(CSS_ID_OBJ_MODEL_INFO_PANEL);
        infoArea = new VBox(infoPane);

        infoArea.setBackground(Background.fill(Color.BLACK));
        infoArea.setMinWidth(MODEL_INFO_AREA_WIDTH);
        infoArea.setMaxWidth(MODEL_INFO_AREA_WIDTH);
    }

    private void showModelInfo(boolean visible) {
        if (visible) {
            splitLayout.getItems().setAll(selectionArea, previewArea, infoArea);
        } else {
            splitLayout.getItems().setAll(selectionArea, previewArea);
        }
        infoArea.setVisible(visible); // Triggers recomputation of preview subscene width!
    }

    private void loadModelFromURL(URL objFileURL) throws IOException {
        final ObjModel model = parseObjModel(objFileURL);
        objModel.set(model);
    }

    private ObjModel parseObjModel(URL objFileURL) throws IOException {
        final Instant start = Instant.now();
        final ObjModel model = new ObjFileParser(objFileURL, StandardCharsets.UTF_8).parse();
        final java.time.Duration duration = java.time.Duration.between(start, Instant.now());
        parsingTime.set(Duration.millis(duration.toMillis()));
        return model;
    }

    private void createMeshViews(ObjModel objModel) {
        final Instant start = Instant.now();
        currentObjectMeshViews   = MeshBuilder.build(objModel, MeshBuilder.BuildMode.BY_OBJECT);
        currentGroupMeshViews    = MeshBuilder.build(objModel, MeshBuilder.BuildMode.BY_GROUP);
        currentMaterialMeshViews = MeshBuilder.build(objModel, MeshBuilder.BuildMode.BY_MATERIAL);
        final java.time.Duration duration = java.time.Duration.between(start, Instant.now());
        meshCreationTime.set(Duration.millis(duration.toMillis()));
    }

    private void createMenus(Stage stage) {

        // -----------------------------
        // File menu
        // -----------------------------

        Menu fileMenu = new Menu("File");

        MenuItem openItem = new MenuItem("Open OBJ…");
        MenuItem exitItem = new MenuItem("Exit");

        fileMenu.getItems().addAll(openItem, exitItem);

        openItem.setOnAction(_ -> {
            if (currentModelDir != null && currentModelDir.exists()) {
                fileChooser.setInitialDirectory(currentModelDir);
            }
            final File objFile = fileChooser.showOpenDialog(stage);
            if (objFile != null) {
                try {
                    showObjModel(objFile);
                } catch (IOException x) {
                    Logger.error(x, "Cannot show OBJ model from file {}", objFile);
                    previewArea.flash("Cannot show OBJ model");
                }
            }
        });

        exitItem.setOnAction(_ -> Platform.exit());

        // -----------------------------
        // View menu
        // -----------------------------

        final Menu viewMenu = new Menu("View");

        final CheckMenuItem miModelInfoVisible = new CheckMenuItem("Statistics");
        miModelInfoVisible.setOnAction(_ -> showModelInfo(miModelInfoVisible.isSelected()));

        final CheckMenuItem miWireframe = new CheckMenuItem("Wireframe");
        miWireframe.selectedProperty().addListener((_, _, sel) -> drawMode.set(sel ? DrawMode.LINE : DrawMode.FILL));
        drawMode.addListener((_, _, mode) -> miWireframe.setSelected(mode == DrawMode.LINE));

        viewMenu.getItems().addAll(miWireframe, miModelInfoVisible);

        // -----------------------------
        // Samples menu
        // -----------------------------

        samplesMenu = new Menu("Samples");
        samplesMenu.disableProperty().bind(Bindings.isEmpty(sampleModels));

        // -----------------------------
        // About menu
        // -----------------------------

        final var helpMenu = new Menu("?");

        final MenuItem miAbout = new MenuItem("About Mesh Viewer");
        miAbout.setOnAction(_ -> showAboutDialog());

        helpMenu.getItems().setAll(miAbout);

        menuBar = new MenuBar(fileMenu, viewMenu, samplesMenu, helpMenu);
    }

    private void addFileDragNDropSupport(Scene scene) {
        // Accept file drag-over
        scene.setOnDragOver(e -> {
            if (e.getGestureSource() != scene && e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            } else {
                // This seems to have no effect on Windows
                e.acceptTransferModes(TransferMode.NONE);
            }
            e.consume();
        });

        // Handle file drop
        scene.setOnDragDropped(e -> {
            final Dragboard db = e.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                final File file = db.getFiles().getFirst();
                // Only accept OBJ files
                if (file.getName().toLowerCase().endsWith(".obj")) {
                    success = true;
                    try {
                        showObjModel(file);
                    } catch (IOException x) {
                        Logger.error(x, "Cannot show OBJ model file {}", file);
                    }
                }
            }
            e.setDropCompleted(success);
            e.consume();
            if (success) {
                Platform.runLater(() -> {
                    stage.toFront();
                    stage.requestFocus();
                });
            }
        });
    }

    private void showAboutDialog() {
        Dialog<Void> about = new Dialog<>();
        about.setTitle("About");

        DialogPane pane = about.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);

        pane.setStyle(
"""
    -fx-background-color: linear-gradient(to bottom, #1a1a1a, #0f0f0f);
    -fx-padding: 20;
    -fx-font-size: 14px;
    -fx-text-fill: #e0e0e0;
    -fx-border-color: #3a6ea5;
    -fx-border-width: 1;
    -fx-border-radius: 4;
""");

        final var content = new Label(
"""
MeshViewer — 3D Preview for JavaFX

Explore JavaFX mesh views with geometry and materials defined in OBJ models.


DISCLAIMER:

OBJ parser development supported by Copilot AI.
(Do *not* tell this on Reddit or you will get censured!)

Written by an "unvaxxed", "climate change denying" old white man
who hates wokeness and gender bullshit.

© 2026 Armin Reichert
""");

        content.setStyle("-fx-text-fill: #d0d0d0;");
        pane.setContent(content);
        about.showAndWait();
    }
}
