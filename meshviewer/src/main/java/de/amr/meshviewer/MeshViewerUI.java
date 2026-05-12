/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

import de.amr.meshviewer.info.ModelInfoPane;
import de.amr.meshviewer.info.SampleInfo;
import de.amr.meshviewer.info.SampleInfoPane;
import de.amr.meshviewer.materialtree.MaterialInfoPane;
import de.amr.meshviewer.meshtree.InnerTreeNode;
import de.amr.meshviewer.meshtree.MeshTreeLeaf;
import de.amr.meshviewer.meshtree.MeshTreeNode;
import de.amr.meshviewer.meshtree.MeshTreePane;
import de.amr.meshviewer.preview.MeshPreview;
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
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
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
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
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

    public final ObjectProperty<DrawMode> drawMode = new SimpleObjectProperty<>(DrawMode.FILL);
    public final BooleanProperty boundingBoxesVisible = new SimpleBooleanProperty(false);
    public final BooleanProperty meshViewNamesShort = new SimpleBooleanProperty(true);

    private final ObservableList<SampleInfo> samples = FXCollections.observableArrayList();
    private final ObservableList<SampleInfo> userSamples = FXCollections.observableArrayList();

    private final ObjectProperty<ObjModelFX> fxModel = new SimpleObjectProperty<>(ObjModelFX.EMPTY);

    private long parsingTimeMillis;

    private final HostServices hostServices;
    private File currentModelDir;

    // UI
    private final Stage stage;
    private final Scene scene;
    private MeshViewerMenus menus;
    private FileChooser fileChooser;

    // Layout
    private final BorderPane mainLayout = new BorderPane();
    private final SplitPane layoutSplitPane = new SplitPane();

    // Selection Area
    private TabPane selectionTabPane = new TabPane();
    private MeshTreePane meshTreePane;
    private MaterialInfoPane materialInfoPane;

    // Preview Area
    private MeshPreview previewArea;

    // Model Info Area
    private Pane infoArea;
    private ModelInfoPane modelInfoPane;
    private SampleInfoPane sampleInfoPane;

    private final AboutDialog aboutDialog = new AboutDialog();

    public MeshViewerUI(Stage stage, double width, double height, HostServices hostServices) {
        this.stage = requireNonNull(stage);
        this.hostServices = requireNonNull(hostServices);

        createSceneLayout();

        scene = new Scene(mainLayout);
        final URL cssURL = getClass().getResource("/app.css");
        if (cssURL != null) {
            scene.getStylesheets().add(cssURL.toExternalForm());
        } else {
            Logger.error("Cannot load app.css");
        }

        createFileChooser();
        addDragNDropSupport();

        stage.setScene(scene);
        stage.setTitle(STAGE_TITLE);
        stage.setWidth(width);
        stage.setHeight(height);

        fxModel.addListener(this::handleModelChange);
        addKeyListener();

        Platform.runLater(() -> {
            showSelectionArea(false);
            showInfoArea(true);
        });
    }

    private void handleModelChange(ObservableValue<? extends ObjModelFX> py, ObjModelFX oldModel, ObjModelFX newModel) {
        if (newModel != ObjModelFX.EMPTY) {
            meshTreePane.update(newModel);
            materialInfoPane.update(newModel);
            modelInfoPane.update(newModel, parsingTimeMillis);
        } else {
            meshTreePane.clear();
            materialInfoPane.clear();
            modelInfoPane.clear();
        }
    }

    // Public

    public File currentModelDir() {
        return currentModelDir;
    }

    public FileChooser fileChooser() {
        return fileChooser;
    }

    public Stage stage() {
        return stage;
    }

    public MeshPreview previewArea() {
        return previewArea;
    }

    public TabPane selectionTabPane() {
        return selectionTabPane;
    }

    public Pane infoArea() {
        return infoArea;
    }

    public AboutDialog aboutDialog() {
        return aboutDialog;
    }

    public ObservableList<SampleInfo> samples() {
        return samples;
    }

    public ObservableList<SampleInfo> userSamples() {
        return userSamples;
    }

    public void show() {
        stage.show();
        if (!samples.isEmpty()) {
            try {
                showIntegratedSample(samples.getFirst());
            } catch (IOException x){
                Logger.error(x, "Cannot show first sample model");
                previewArea.flash("Cannot show sample model");
            }
        }
        showInfoArea(false);
    }

    public void addIntegratedSample(SampleInfo sample) {
        requireNonNull(sample);

        samples.add(sample);
        menus.addIntegratedSample(sample);
    }

    public void addUserSample(File userSampleDir, SampleInfo sample) {
        requireNonNull(userSampleDir);
        requireNonNull(sample);

        userSamples.add(sample);
        menus.addUserSample(userSampleDir, sample);
    }

    public void showIntegratedSample(SampleInfo sample) throws IOException {
        final URL url = getClass().getResource(sample.path() + sample.fileName());
        showObjModel(url);
        previewArea.initSampleModel(meshTreePane.modelTreeView(), sample);
        sampleInfoPane.setVisible(true);
        sampleInfoPane.update(sample);
    }

    public void showUserSample(File userSampleDir, SampleInfo sample) {
        final File objFile = new File(userSampleDir, sample.path() + sample.fileName());
        if (objFile.exists() && objFile.isFile()) {
            try {
                showObjModel(objFile);
                previewArea.initSampleModel(meshTreePane.modelTreeView(), sample);
                sampleInfoPane.setVisible(true);
                sampleInfoPane.update(sample);
            }
            catch (IOException x) {
                Logger.error(x);
            }
        }
    }

    public void showObjModel(File objFile) throws IOException {
        requireNonNull(objFile);
        loadModelFromURL(objFile.toURI().toURL());
        currentModelDir = objFile.getParentFile();
        previewArea.reset();
        previewArea.assignFocusToSubScene();
        sampleInfoPane.setVisible(false);
        meshTreePane.setInitialSelection();
    }

    public void showObjModel(URL url) throws IOException {
        requireNonNull(url);
        loadModelFromURL(url);
        previewArea.reset();
        previewArea.assignFocusToSubScene();
        sampleInfoPane.setVisible(false);
        meshTreePane.setInitialExpansionState();
    }

    public void showSelectionArea(boolean visible) {
        selectionTabPane.setVisible(visible);
        updateLayout();
    }

    public boolean isSelectionAreaVisible() {
        return selectionTabPane.isVisible();
    }

    public void showInfoArea(boolean visible) {
        infoArea.setVisible(visible); // Triggers recomputation of preview subscene width!
        updateLayout();
    }

    public boolean isInfoAreaVisible() {
        return infoArea.isVisible();
    }

    // Private

    private void addKeyListener() {
        scene.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            switch (e.getCharacter()) {
                case "s" -> {
                    final boolean visible = isSelectionAreaVisible();
                    showSelectionArea(!visible);
                    e.consume();
                }
                case "i" -> {
                    boolean visible = isInfoAreaVisible();
                    showInfoArea(!visible);
                    e.consume();
                }
            }
        });
    }


    private void createSceneLayout() {
        createSelectionArea();
        createPreviewArea();
        createInfoArea();

        layoutSplitPane.setOrientation(Orientation.HORIZONTAL);

        // Not sure if this is the best way but it works well
        final var previewWidthReduction = Bindings.createDoubleBinding(
            () -> {
                double w = 0;
                if (selectionTabPane.isVisible()) w += selectionTabPane.getWidth();
                if (infoArea.isVisible()) w += infoArea.getWidth();
                return w;
            },
            selectionTabPane.visibleProperty(), selectionTabPane.widthProperty(),
            infoArea.visibleProperty(), infoArea.widthProperty()
        );
        previewArea.subScene().widthProperty().bind(mainLayout.widthProperty().subtract(previewWidthReduction));
        previewArea.subScene().heightProperty().bind(previewArea.heightProperty());

        menus = new MeshViewerMenus(this);
        mainLayout.setTop(menus.menuBar());
        mainLayout.setCenter(layoutSplitPane);
    }

    private void createSelectionArea() {
        createMeshTreePane();

        materialInfoPane = new MaterialInfoPane();

        final Tab meshTreeTab = new Tab("Mesh Tree", meshTreePane);
        meshTreeTab.setClosable(false);

        final Tab materialInfoTab = new Tab("Materials", materialInfoPane);
        materialInfoTab.setClosable(false);

        selectionTabPane = new TabPane();
        selectionTabPane.setSide(Side.BOTTOM);
        selectionTabPane.setMinWidth(TREE_AREA_WIDTH);
        selectionTabPane.getTabs().addAll(meshTreeTab, materialInfoTab);
    }

    private void createFileChooser() {
        fileChooser = new FileChooser();
        fileChooser.setTitle("Open OBJ File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("OBJ Files", "*.obj"));
    }

    private void createPreviewArea() {
        previewArea = new MeshPreview();
        previewArea.drawMode.bindBidirectional(drawMode);
        previewArea.boundingBoxesVisible.bind(boundingBoxesVisible);
    }

    private void createMeshTreePane() {
        meshTreePane = new MeshTreePane(TREE_AREA_WIDTH);
        meshTreePane.meshViewNamesShort.bind(meshViewNamesShort);

        for (InnerTreeNode.NodeCategory category : InnerTreeNode.NodeCategory.values()) {
            final ObservableSet<MeshTreeNode> selectedNodes = FXCollections.observableSet();
            meshTreePane.modelTreeView().selection().put(category, selectedNodes);
            selectedNodes.addListener((SetChangeListener<MeshTreeNode>) change -> {
                Logger.debug("Selection changed for category {}: {}", category, change);
                updateDisplayedMeshViewSet(
                    meshTreePane.modelTreeView().getSelectionModel().getSelectedItem()
                );
            });
        }

        meshTreePane.modelTreeView().getSelectionModel().selectedItemProperty().addListener((_, _, selectedItem) -> {
            Logger.debug("Selected item: {}", selectedItem);
            updateDisplayedMeshViewSet(selectedItem);
        });

    }

    private void updateDisplayedMeshViewSet(TreeItem<MeshTreeNode> selectedTreeItem) {
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
                    all = fxModel.get().objectMeshViews().values();
                    displayed.addAll(collectMeshViews(selectedTreeItem));
                }
                case MeshesByGroups -> {
                    all = fxModel.get().groupMeshViews().values();
                    displayed.addAll(collectMeshViews(selectedTreeItem));
                }
                case MeshesByMaterials -> {
                    all = fxModel.get().materialMeshViews().values();
                    displayed.addAll(collectMeshViews(selectedTreeItem));
                }
            }
        }
        else if (selectedTreeItem.getValue() instanceof MeshTreeLeaf) {
            final TreeItem<MeshTreeNode> parent = selectedTreeItem.getParent();
            if (parent.getValue() instanceof InnerTreeNode innerTreeNode) {
                switch (innerTreeNode.nodeCategory) {
                    case Model -> {}
                    case MeshesByObjects -> {
                        all = fxModel.get().objectMeshViews().values();
                        displayed.addAll(collectMeshViews(selectedTreeItem));
                    }
                    case MeshesByGroups -> {
                        all = fxModel.get().groupMeshViews().values();
                        displayed.addAll(collectMeshViews(selectedTreeItem));
                    }
                    case MeshesByMaterials -> {
                        all = fxModel.get().materialMeshViews().values();
                        displayed.addAll(collectMeshViews(selectedTreeItem));
                    }
                }
            }
        }
        previewArea.selectDisplayedMeshViews(all, displayed);
    }

    private Set<MeshView> collectMeshViews(TreeItem<MeshTreeNode> selectedTreeItem) {
        return selectedTreeItem.getChildren().stream()
            .map(TreeItem::getValue)
            .filter(node -> node.checked.get())
            .filter(MeshTreeLeaf.class::isInstance)
            .map(MeshTreeLeaf.class::cast)
            .map(meshTreeLeaf -> meshTreeLeaf.meshView)
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

    private void updateLayout() {
        layoutSplitPane.getItems().clear();
        if (selectionTabPane.isVisible()) {
            layoutSplitPane.getItems().add(selectionTabPane);
        }
        layoutSplitPane.getItems().add(previewArea);
        if (infoArea.isVisible()) {
            layoutSplitPane.getItems().add(infoArea);
        }
    }

    // --- Model access

    private void loadModelFromURL(URL objFileURL) throws IOException {
        final ObjModel obj = parseObjModel(objFileURL);
        fxModel.set(new ObjModelFX(obj));
    }

    private ObjModel parseObjModel(URL objFileURL) throws IOException {
        final Instant start = Instant.now();
        final ObjModel obj = new ObjFileParser(objFileURL, StandardCharsets.UTF_8).parse();
        final var duration = java.time.Duration.between(start, Instant.now());
        parsingTimeMillis = duration.toMillis();
        return obj;
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
