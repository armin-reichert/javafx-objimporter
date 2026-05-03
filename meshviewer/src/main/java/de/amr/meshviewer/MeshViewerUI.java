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
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class MeshViewerUI {

    public static final String STAGE_TITLE = "JavaFX OBJ Mesh Viewer";

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
    private ObjModelInfoPanel modelInfoPane;

    private File workDir;

    public MeshViewerUI(Stage stage, double width, double height) {
        this.stage = requireNonNull(stage);
        createUI(width, height);
        addModelListener();
    }

    private void addModelListener() {
        objModel.addListener((_, _, newModel) -> {
            if (newModel != null) {
                final Instant start = Instant.now();
                currentObjectMeshViews   = MeshBuilder.build(newModel, MeshBuilder.BuildMode.BY_OBJECT);
                currentGroupMeshViews    = MeshBuilder.build(newModel, MeshBuilder.BuildMode.BY_GROUP);
                currentMaterialMeshViews = MeshBuilder.build(newModel, MeshBuilder.BuildMode.BY_MATERIAL);
                final java.time.Duration duration = java.time.Duration.between(start, Instant.now());
                meshCreationTime.set(Duration.millis(duration.toMillis()));
                navigationTreeView.populate(
                    createTreeTitle(newModel),
                    currentObjectMeshViews,
                    currentGroupMeshViews,
                    currentMaterialMeshViews
                );
                selectAllGroupsNodeInNavigationTree();
                modelInfoPane.update(newModel, parsingTime.get(), meshCreationTime.get());
            } else {
                currentObjectMeshViews = Map.of();
                currentGroupMeshViews = Map.of();
                currentMaterialMeshViews = Map.of();
                navigationTreeView.populate(
                    "No OBJ model",
                    currentObjectMeshViews,
                    currentGroupMeshViews,
                    currentMaterialMeshViews
                );
                modelInfoPane.update(null, null, null);
            }
        });
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
        workDir = objFile.getParentFile();
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

    private void createPreviewArea() {
        previewArea = new PreviewArea();
        previewArea.drawMode.bindBidirectional(drawMode);
    }

    private void createObjFileChooser() {
        fileChooser = new FileChooser();
        fileChooser.setTitle("Open OBJ File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("OBJ Files", "*.obj"));
    }

    private void createSelectionArea() {
        createNavigationTree();
        selectionArea = new VBox(navigationTreeView);
        selectionArea.setMinWidth(SELECTION_AREA_WIDTH);
        //selectionArea.setMaxWidth(SELECTION_AREA_WIDTH);
        navigationTreeView.prefHeightProperty().bind(selectionArea.heightProperty().subtract(1));
    }

    private void showModelInfo(boolean visible) {
        infoArea.setVisible(visible);
        if (visible) {
            splitLayout.getItems().setAll(selectionArea, previewArea, infoArea);
        } else {
            splitLayout.getItems().setAll(selectionArea, previewArea);
        }
    }

    private void createInfoArea() {
        modelInfoPane = new ObjModelInfoPanel("objModelInfo");
        infoArea = new VBox(modelInfoPane);
        infoArea.setBackground(Background.fill(Color.BLACK));
        infoArea.setMinWidth(MODEL_INFO_AREA_WIDTH);
        infoArea.setMaxWidth(MODEL_INFO_AREA_WIDTH);
    }

    private void loadModelFromURL(URL objFileURL) throws IOException {
        final var parser = new ObjFileParser(objFileURL, StandardCharsets.UTF_8);
        final long start = System.nanoTime();
        final ObjModel model = parser.parse();
        final long millis = (System.nanoTime() - start) / 1_000_000;
        parsingTime.set(Duration.millis(millis));
        objModel.set(model);
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
            if (workDir != null && workDir.exists()) {
                fileChooser.setInitialDirectory(workDir);
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

        final CheckMenuItem miModelInfoVisible = new CheckMenuItem("Model Statistics");
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

        menuBar = new MenuBar(fileMenu, viewMenu, samplesMenu);
    }

    private void createNavigationTree() {
        navigationTreeView = new ObjModelNavigationTree();
        navigationTreeView.getSelectionModel().selectedItemProperty().addListener((_, _, item) -> {
            Logger.info("Selected item: {}", item);
            if (item == null) return;
            switch (item.getValue()) {
                case MeshNode meshNode -> previewArea.displayMeshViews(List.of(meshNode.meshView));
                case InnerTreeNode innerNode -> {
                    switch (innerNode.type) {
                        case Object   -> previewArea.displayMeshViews(currentObjectMeshViews.values().stream().toList());
                        case Group    -> previewArea.displayMeshViews(currentGroupMeshViews.values().stream().toList());
                        case Material -> previewArea.displayMeshViews(currentMaterialMeshViews.values().stream().toList());
                        default       -> {}
                    }
                }
                default -> {}
            }
        });
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
}
