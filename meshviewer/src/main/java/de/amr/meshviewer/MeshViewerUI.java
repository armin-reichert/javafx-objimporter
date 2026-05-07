/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

import de.amr.meshbuilder.MeshBuilder;
import de.amr.meshviewer.InnerTreeNode.NodeCategory;
import de.amr.objparser.ObjFileParser;
import de.amr.objparser.ObjModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class MeshViewerUI {

    public static final String STAGE_TITLE = "JavaFX OBJ Mesh Viewer";
    public static final String NO_OBJ_MODEL_TITLE = "No OBJ model";

    public static final Pattern ANON_OBJECT_PATTERN = Pattern.compile("^Object\\.anon_\\d+\\.(.+)$");

    public static final String CSS_ID_OBJ_MODEL_TREE = "objModelTree";
    public static final String CSS_ID_MODEL_INFO_PANE = "objModelInfo";
    public static final String CSS_ID_SAMPLE_INFO_PANE = "sampleInfo";

    public static final int SELECTION_AREA_WIDTH = 300;
    public static final int MODEL_INFO_AREA_WIDTH = 260;
    public static final int INFO_PANE_LABEL_COLUMN_WIDTH = 100;

    private final ObjectProperty<ObjModel> objModel = new SimpleObjectProperty<>();
    private final ObjectProperty<DrawMode> drawMode = new SimpleObjectProperty<>(DrawMode.FILL);
    private final BooleanProperty shortMeshViewNames = new SimpleBooleanProperty(true);
    private final ObjectProperty<Duration> parsingTime = new SimpleObjectProperty<>(Duration.ZERO);
    private final ObjectProperty<Duration> meshCreationTime = new SimpleObjectProperty<>(Duration.ZERO);

    private final ObservableList<SampleInfo> samples = FXCollections.observableArrayList();

    private Map<String, MeshView> objectMeshViews;
    private Map<String, MeshView> groupMeshViews;
    private Map<String, MeshView> materialMeshViews;
    private File currentModelDir;

    // UI
    private final Stage stage;
    private final Scene scene;
    private MenuBar menuBar;
    private Menu samplesMenu;
    private FileChooser fileChooser;

    // Layout
    private final BorderPane rootPane = new BorderPane();
    private final SplitPane splitLayout = new SplitPane();

    // Selection Area
    private Pane selectionArea;
    private ModelTree modelTree;

    // Preview Area
    private PreviewArea previewArea;

    // Model Info Area
    private Pane infoArea;
    private ModelInfoPane modelInfoPane;
    private SampleInfoPane sampleInfoPane;

    private final AboutDialog aboutDialog = new AboutDialog();

    public MeshViewerUI(Stage stage, double width, double height) {
        this.stage = requireNonNull(stage);
        scene = new Scene(rootPane);
        createUI(width, height);
        objModel.addListener(this::onObjModelChange);
    }

    private void onObjModelChange(ObservableValue<? extends ObjModel> ov, ObjModel oldModel, ObjModel newModel) {
        if (newModel != null) {
            final String url = newModel.url();
            final String title = URLDecoder.decode(url.substring(url.lastIndexOf('/') + 1), StandardCharsets.UTF_8);
            createMeshViews(newModel);
            modelTree.populate(title, objectMeshViews, groupMeshViews, materialMeshViews);
            modelTree.clearSelectedNodeSets();
            final Set<MeshView> allMeshViews = new HashSet<>();
            allMeshViews.addAll(objectMeshViews.values());
            allMeshViews.addAll(groupMeshViews.values());
            allMeshViews.addAll(materialMeshViews.values());
            int numMeshViews = allMeshViews.size();
            modelInfoPane.update(newModel, numMeshViews, parsingTime.get(), meshCreationTime.get());
        } else {
            objectMeshViews = Map.of();
            groupMeshViews = Map.of();
            materialMeshViews = Map.of();
            modelTree.populate(NO_OBJ_MODEL_TITLE, objectMeshViews, groupMeshViews, materialMeshViews);
            modelInfoPane.update(null, 0, null, null);
        }
    }

    // Public

    public void show() {
        stage.show();
        if (!samples.isEmpty()) {
            try {
                showSampleModel(samples.getFirst());
            } catch (IOException x){
                Logger.error(x, "Cannot show first sample model");
                previewArea.flash("Cannot show sample model");
            }
        }
        showModelInfo(false);
    }

    public void addSampleModel(SampleInfo sample) {
        requireNonNull(sample);
        samples.add(sample);
        final var menuItem = new MenuItem(sample.title());
        menuItem.setOnAction(_ -> {
            try {
                showSampleModel(sample);
            } catch (IOException x) {
                Logger.error(x, "Cannot show sample model");
                previewArea.flash("Cannot show sample model");
            }
        });
        samplesMenu.getItems().add(menuItem);
    }

    // Private

    private void showSampleModel(SampleInfo sample) throws IOException {
        final URL url = getClass().getResource(sample.path() + sample.fileName());
        showObjModel(url);
        previewArea.initSampleModel(modelTree, sample);
        sampleInfoPane.setVisible(true);
        sampleInfoPane.update(sample);
    }

    private void showObjModel(File objFile) throws IOException {
        requireNonNull(objFile);
        loadModelFromURL(objFile.toURI().toURL());
        currentModelDir = objFile.getParentFile();
        previewArea.reset();
        previewArea.assignFocusToSubScene();
        sampleInfoPane.setVisible(false);
        // initial selection: all material meshes
        modelTree.getRoot().getChildren().forEach(node -> node.setExpanded(false));
        modelTree.selectAllFrom(InnerTreeNode.NodeCategory.Materials);
        modelTree.getRoot().getChildren().getLast().setExpanded(true);
    }

    private void showObjModel(URL url) throws IOException {
        requireNonNull(url);
        loadModelFromURL(url);
        previewArea.reset();
        previewArea.assignFocusToSubScene();
        sampleInfoPane.setVisible(false);
        // initial selection: all material meshes
        modelTree.getRoot().getChildren().forEach(node -> node.setExpanded(false));
        modelTree.selectAllFrom(InnerTreeNode.NodeCategory.Materials);
        modelTree.getRoot().getChildren().getLast().setExpanded(true);
    }

    private void createUI(double width, double height) {
        final URL cssURL = getClass().getResource("/app.css");
        if (cssURL != null) {
            scene.getStylesheets().add(cssURL.toExternalForm());
        } else {
            Logger.error("Cannot load app.css");
        }

        createLayout();
        createFileChooser();

        stage.setScene(scene);
        stage.setTitle(STAGE_TITLE);
        stage.setWidth(width);
        stage.setHeight(height);

        addDragNDropSupport();
    }

    private void createLayout() {
        createPreviewArea();
        createSelectionArea();
        createInfoArea();
        createMenus(stage);

        splitLayout.setOrientation(Orientation.HORIZONTAL);

        rootPane.setTop(menuBar);
        rootPane.setCenter(splitLayout);

        final var previewWidthReduction = Bindings.createDoubleBinding(
            () -> SELECTION_AREA_WIDTH + (infoArea.isVisible() ? infoArea.getWidth() : 0),
            infoArea.visibleProperty(), infoArea.widthProperty()
        );
        previewArea.subScene().widthProperty().bind(rootPane.widthProperty().subtract(previewWidthReduction));
        previewArea.subScene().heightProperty().bind(previewArea.heightProperty());
    }

    private void createFileChooser() {
        fileChooser = new FileChooser();
        fileChooser.setTitle("Open OBJ File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("OBJ Files", "*.obj"));
    }

    private void createPreviewArea() {
        previewArea = new PreviewArea();
        previewArea.drawMode.bindBidirectional(drawMode);
    }

    private void createSelectionArea() {
        modelTree = new ModelTree(CSS_ID_OBJ_MODEL_TREE);
        modelTree.shortMeshViewNames.bind(shortMeshViewNames);
        for (NodeCategory category : NodeCategory.values()) {
            final ObservableSet<TreeNode> selectedNodes = FXCollections.observableSet();
            modelTree.selection().put(category, selectedNodes);
            selectedNodes.addListener((SetChangeListener<TreeNode>) change -> {
                Logger.debug("Selection changed for category {}: {}", category, change);
                updateDisplayedMeshViewSet(modelTree.getSelectionModel().getSelectedItem());
            });
        }

        modelTree.getSelectionModel().selectedItemProperty().addListener((_, _, selectedItem) -> {
            Logger.debug("Selected item: {}", selectedItem);
            updateDisplayedMeshViewSet(selectedItem);
        });

        selectionArea = new VBox(modelTree);
        selectionArea.setMinWidth(SELECTION_AREA_WIDTH);

        modelTree.prefHeightProperty().bind(selectionArea.heightProperty().subtract(1));
    }

    private void updateDisplayedMeshViewSet(TreeItem<TreeNode> selectedTreeItem) {
        if (selectedTreeItem == null) {
            Logger.debug("Nothing selected");
            return;
        }
        Collection<MeshView> all = Set.of();
        final Set<MeshView> displayed = new HashSet<>();

        if (selectedTreeItem.getValue() instanceof InnerTreeNode innerTreeNode) {
            switch (innerTreeNode.nodeCategory) {
                case Model -> {}
                case Objects -> {
                    all = objectMeshViews.values();
                    displayed.addAll(collectMeshViews(selectedTreeItem));
                }
                case Groups -> {
                    all = groupMeshViews.values();
                    displayed.addAll(collectMeshViews(selectedTreeItem));
                }
                case Materials -> {
                    all = materialMeshViews.values();
                    displayed.addAll(collectMeshViews(selectedTreeItem));
                }
            }
        }
        else if (selectedTreeItem.getValue() instanceof MeshTreeNode) {
            final TreeItem<TreeNode> parent = selectedTreeItem.getParent();
            if (parent.getValue() instanceof InnerTreeNode innerTreeNode) {
                switch (innerTreeNode.nodeCategory) {
                    case Model -> {}
                    case Objects -> {
                        all = objectMeshViews.values();
                        displayed.addAll(collectMeshViews(selectedTreeItem));
                    }
                    case Groups -> {
                        all = groupMeshViews.values();
                        displayed.addAll(collectMeshViews(selectedTreeItem));
                    }
                    case Materials -> {
                        all = materialMeshViews.values();
                        displayed.addAll(collectMeshViews(selectedTreeItem));
                    }
                }
            }
        }
        previewArea.selectDisplayedMeshViews(all, displayed);
    }

    private Set<MeshView> collectMeshViews(TreeItem<TreeNode> selectedTreeItem) {
        return selectedTreeItem.getChildren().stream()
            .map(TreeItem::getValue)
            .filter(node -> node.checked.get())
            .filter(MeshTreeNode.class::isInstance)
            .map(MeshTreeNode.class::cast)
            .map(meshTreeNode -> meshTreeNode.meshView)
            .collect(Collectors.toSet());
    }

    private void createInfoArea() {
        modelInfoPane = new ModelInfoPane(CSS_ID_MODEL_INFO_PANE);
        sampleInfoPane = new SampleInfoPane(CSS_ID_SAMPLE_INFO_PANE);

        infoArea = new VBox(modelInfoPane, sampleInfoPane);

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
        objectMeshViews = MeshBuilder.build(objModel, MeshBuilder.BuildMode.BY_OBJECT);
        groupMeshViews = MeshBuilder.build(objModel, MeshBuilder.BuildMode.BY_GROUP);
        materialMeshViews = MeshBuilder.build(objModel, MeshBuilder.BuildMode.BY_MATERIAL);
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

        final CheckMenuItem miShortMeshViewNames = new CheckMenuItem("Short Mesh Names");
        miShortMeshViewNames.selectedProperty().bindBidirectional(shortMeshViewNames);

        final CheckMenuItem miInfoVisible = new CheckMenuItem("Info");
        miInfoVisible.setOnAction(_ -> showModelInfo(miInfoVisible.isSelected()));

        final CheckMenuItem miWireframe = new CheckMenuItem("Wireframe");
        miWireframe.selectedProperty().addListener((_, _, sel) -> drawMode.set(sel ? DrawMode.LINE : DrawMode.FILL));
        drawMode.addListener((_, _, mode) -> miWireframe.setSelected(mode == DrawMode.LINE));

        viewMenu.getItems().addAll(miShortMeshViewNames, miWireframe, miInfoVisible);

        // -----------------------------
        // Samples menu
        // -----------------------------

        samplesMenu = new Menu("Samples");
        samplesMenu.disableProperty().bind(Bindings.isEmpty(samples));

        // -----------------------------
        // About menu
        // -----------------------------

        final var helpMenu = new Menu("?");

        final MenuItem miAbout = new MenuItem("About Mesh Viewer");
        miAbout.setOnAction(_ -> aboutDialog.showAndWait());

        helpMenu.getItems().setAll(miAbout);

        menuBar = new MenuBar(fileMenu, viewMenu, samplesMenu, helpMenu);
    }

    private void addDragNDropSupport() {
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
