/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

import de.amr.meshbuilder.MeshBuilder;
import de.amr.meshviewer.info.ModelInfoPane;
import de.amr.meshviewer.info.SampleInfo;
import de.amr.meshviewer.info.SampleInfoPane;
import de.amr.meshviewer.modeltree.InnerTreeNode;
import de.amr.meshviewer.modeltree.MeshTreeNode;
import de.amr.meshviewer.modeltree.ModelTreeNode;
import de.amr.meshviewer.modeltree.ModelTreePane;
import de.amr.objparser.ObjFileParser;
import de.amr.objparser.ObjModel;
import javafx.application.HostServices;
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
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
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

    public static final Pattern ANON_OBJECT_PATTERN = Pattern.compile("^Object\\.anon_\\d+\\.(.+)$");

    public static final String CSS_ID_MODEL_INFO_PANE = "objModelInfo";
    public static final String CSS_ID_SAMPLE_INFO_PANE = "sampleInfo";

    public static final int TREE_AREA_WIDTH = 350;
    public static final int INFO_AREA_WIDTH = 300;
    public static final int INFO_AREA_LABEL_COLUMN_WIDTH = 125;

    private final ObjectProperty<ObjModel> objModel = new SimpleObjectProperty<>();
    private final ObjectProperty<DrawMode> drawMode = new SimpleObjectProperty<>(DrawMode.FILL);
    private final BooleanProperty shortMeshViewNames = new SimpleBooleanProperty(true);
    private final ObjectProperty<Duration> parsingTime = new SimpleObjectProperty<>(Duration.ZERO);
    private final ObjectProperty<Duration> meshCreationTime = new SimpleObjectProperty<>(Duration.ZERO);

    private final ObservableList<SampleInfo> samples = FXCollections.observableArrayList();

    private final HostServices hostServices;

    private Map<String, MeshView> objectMeshViews;
    private Map<String, MeshView> groupMeshViews;
    private Map<String, MeshView> materialMeshViews;
    private Map<String, PhongMaterial> materialsMap;

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
    private ModelTreePane modelTreePane;

    // Preview Area
    private PreviewArea previewArea;

    // Model Info Area
    private Pane infoArea;
    private ModelInfoPane modelInfoPane;
    private SampleInfoPane sampleInfoPane;

    private final AboutDialog aboutDialog = new AboutDialog();

    public MeshViewerUI(Stage stage, double width, double height, HostServices hostServices) {
        this.stage = requireNonNull(stage);
        this.hostServices = requireNonNull(hostServices);
        scene = new Scene(rootPane);
        createUI(width, height);
        objModel.addListener(this::onObjModelChange);
        Platform.runLater(() -> {
            showTreeArea(true);
            showInfoArea(false);
        });
    }

    private void onObjModelChange(ObservableValue<? extends ObjModel> ov, ObjModel oldModel, ObjModel newModel) {
        if (newModel != null) {
            createMeshViewsAndPhongMaterials(newModel);
            final Set<MeshView> allMeshViews = new HashSet<>();
            allMeshViews.addAll(objectMeshViews.values());
            allMeshViews.addAll(groupMeshViews.values());
            allMeshViews.addAll(materialMeshViews.values());
            modelTreePane.update(newModel, objectMeshViews, groupMeshViews, materialMeshViews, materialsMap);
            modelInfoPane.update(newModel, allMeshViews.size(), parsingTime.get(), meshCreationTime.get());
        } else {
            clearMeshViewsAndPhongMaterials();
            modelTreePane.clear();
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
        showInfoArea(false);
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
        previewArea.initSampleModel(modelTreePane.modelTreeView(), sample);
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
        modelTreePane.setInitialSelection();
    }

    private void showObjModel(URL url) throws IOException {
        requireNonNull(url);
        loadModelFromURL(url);
        previewArea.reset();
        previewArea.assignFocusToSubScene();
        sampleInfoPane.setVisible(false);
        modelTreePane.setInitialExpansionState();
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
        createModelTreePane();
        createInfoArea();

        splitLayout.setOrientation(Orientation.HORIZONTAL);

        createMenus(stage);

        rootPane.setTop(menuBar);
        rootPane.setCenter(splitLayout);

        final var previewClipping = Bindings.createDoubleBinding(
            () -> {
                double w = 0;
                if (modelTreePane.isVisible()) w += modelTreePane.getWidth();
                if (infoArea.isVisible()) w += infoArea.getWidth();
                return w;
            },
            modelTreePane.visibleProperty(), modelTreePane.widthProperty(),
            infoArea.visibleProperty(), infoArea.widthProperty()
        );
        previewArea.subScene().widthProperty().bind(rootPane.widthProperty().subtract(previewClipping));
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

    private void createModelTreePane() {
        modelTreePane = new ModelTreePane(TREE_AREA_WIDTH);
        modelTreePane.shortMeshViewNames.bind(shortMeshViewNames);

        for (InnerTreeNode.NodeCategory category : InnerTreeNode.NodeCategory.values()) {
            final ObservableSet<ModelTreeNode> selectedNodes = FXCollections.observableSet();
            modelTreePane.modelTreeView().selection().put(category, selectedNodes);
            selectedNodes.addListener((SetChangeListener<ModelTreeNode>) change -> {
                Logger.debug("Selection changed for category {}: {}", category, change);
                updateDisplayedMeshViewSet(
                    modelTreePane.modelTreeView().getSelectionModel().getSelectedItem()
                );
            });
        }

        modelTreePane.modelTreeView().getSelectionModel().selectedItemProperty().addListener((_, _, selectedItem) -> {
            Logger.debug("Selected item: {}", selectedItem);
            updateDisplayedMeshViewSet(selectedItem);
        });

    }

    private void updateDisplayedMeshViewSet(TreeItem<ModelTreeNode> selectedTreeItem) {
        if (selectedTreeItem == null) {
            Logger.debug("Nothing selected");
            return;
        }
        Collection<MeshView> all = Set.of();
        final Set<MeshView> displayed = new HashSet<>();

        if (selectedTreeItem.getValue() instanceof InnerTreeNode innerTreeNode) {
            switch (innerTreeNode.nodeCategory) {
                case Model -> {}
                case MeshesByObjects -> {
                    all = objectMeshViews.values();
                    displayed.addAll(collectMeshViews(selectedTreeItem));
                }
                case MeshesByGroups -> {
                    all = groupMeshViews.values();
                    displayed.addAll(collectMeshViews(selectedTreeItem));
                }
                case MeshesByMaterials -> {
                    all = materialMeshViews.values();
                    displayed.addAll(collectMeshViews(selectedTreeItem));
                }
            }
        }
        else if (selectedTreeItem.getValue() instanceof MeshTreeNode) {
            final TreeItem<ModelTreeNode> parent = selectedTreeItem.getParent();
            if (parent.getValue() instanceof InnerTreeNode innerTreeNode) {
                switch (innerTreeNode.nodeCategory) {
                    case Model -> {}
                    case MeshesByObjects -> {
                        all = objectMeshViews.values();
                        displayed.addAll(collectMeshViews(selectedTreeItem));
                    }
                    case MeshesByGroups -> {
                        all = groupMeshViews.values();
                        displayed.addAll(collectMeshViews(selectedTreeItem));
                    }
                    case MeshesByMaterials -> {
                        all = materialMeshViews.values();
                        displayed.addAll(collectMeshViews(selectedTreeItem));
                    }
                }
            }
        }
        previewArea.selectDisplayedMeshViews(all, displayed);
    }

    private Set<MeshView> collectMeshViews(TreeItem<ModelTreeNode> selectedTreeItem) {
        return selectedTreeItem.getChildren().stream()
            .map(TreeItem::getValue)
            .filter(node -> node.checked.get())
            .filter(MeshTreeNode.class::isInstance)
            .map(MeshTreeNode.class::cast)
            .map(meshTreeNode -> meshTreeNode.meshView)
            .collect(Collectors.toSet());
    }

    private void createInfoArea() {
        modelInfoPane = new ModelInfoPane(CSS_ID_MODEL_INFO_PANE, hostServices);
        sampleInfoPane = new SampleInfoPane(CSS_ID_SAMPLE_INFO_PANE, hostServices);

        infoArea = new VBox(sampleInfoPane, modelInfoPane);

        infoArea.setBackground(Background.fill(Color.BLACK));
        infoArea.setMinWidth(INFO_AREA_WIDTH);
        infoArea.setMaxWidth(INFO_AREA_WIDTH);
    }

    private void showTreeArea(boolean visible) {
        modelTreePane.setVisible(visible);
        updateSplitLayoutItems();
    }

    private void showInfoArea(boolean visible) {
        infoArea.setVisible(visible); // Triggers recomputation of preview subscene width!
        updateSplitLayoutItems();
    }

    private void updateSplitLayoutItems() {
        splitLayout.getItems().clear();
        if (modelTreePane.isVisible()) {
            splitLayout.getItems().add(modelTreePane);
        }
        splitLayout.getItems().add(previewArea);
        if (infoArea.isVisible()) {
            splitLayout.getItems().add(infoArea);
        }
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

    private void createMeshViewsAndPhongMaterials(ObjModel objModel) {
        final Instant start = Instant.now();
        final MeshBuilder builder = new MeshBuilder(objModel);
        objectMeshViews = builder.buildMeshViewsByObject();
        groupMeshViews = builder.buildMeshViewsByGroup();
        materialMeshViews = builder.buildMeshViewsByMaterial();
        materialsMap = builder.materials();
        final java.time.Duration duration = java.time.Duration.between(start, Instant.now());
        meshCreationTime.set(Duration.millis(duration.toMillis()));
    }

    private void clearMeshViewsAndPhongMaterials() {
        objectMeshViews = Map.of();
        groupMeshViews = Map.of();
        materialMeshViews = Map.of();
        materialsMap = Map.of();
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

        final CheckMenuItem miWireframe = new CheckMenuItem("Wireframe");
        miWireframe.selectedProperty().addListener((_, _, sel) -> drawMode.set(sel ? DrawMode.LINE : DrawMode.FILL));
        drawMode.addListener((_, _, mode) -> miWireframe.setSelected(mode == DrawMode.LINE));

        final CheckMenuItem miShortMeshViewNames = new CheckMenuItem("Short Mesh Names");
        miShortMeshViewNames.selectedProperty().bindBidirectional(shortMeshViewNames);

        final CheckMenuItem miTreeVisible = new CheckMenuItem("Mesh Tree");
        miTreeVisible.selectedProperty().bindBidirectional(modelTreePane.visibleProperty());
        miTreeVisible.setOnAction(_ -> showTreeArea(miTreeVisible.isSelected()));

        final CheckMenuItem miInfoVisible = new CheckMenuItem("Info");
        miInfoVisible.selectedProperty().bindBidirectional(infoArea.visibleProperty());
        miInfoVisible.setOnAction(_ -> showInfoArea(miInfoVisible.isSelected()));

        viewMenu.getItems().addAll(miWireframe, miShortMeshViewNames, miTreeVisible, miInfoVisible);

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
