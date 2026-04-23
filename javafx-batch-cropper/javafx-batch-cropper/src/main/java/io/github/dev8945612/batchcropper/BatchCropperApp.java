package io.github.dev8945612.batchcropper;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class BatchCropperApp extends Application {
    private final ObservableList<File> inputFiles = FXCollections.observableArrayList();
    private final GridModel gridModel = new GridModel(3, 3);
    private final PreviewPane previewPane = new PreviewPane();
    private final ListView<File> filesListView = new ListView<>(inputFiles);
    private final Label statusLabel = new Label();
    private final Label outputLabel = new Label();

    private final Map<File, GridModel> perImageGrids = new HashMap<>();
    private final CheckBox applyToAllCheckbox = new CheckBox();
    private final Spinner<Integer> rowsSpinner = new Spinner<>(1, 20, 3);
    private final Spinner<Integer> colsSpinner = new Spinner<>(1, 20, 3);
    private boolean ignoringSpinnerEvents = false;

    private Path outputDirectory;
    private Stage primaryStage;
    private String cssPath;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.cssPath = getClass().getResource("app.css").toExternalForm();

        statusLabel.textProperty().bind(I18n.bind("status.initial"));
        outputLabel.textProperty().bind(I18n.bind("label.no.folder"));
        applyToAllCheckbox.textProperty().bind(I18n.bind("checkbox.apply.all"));

        previewPane.setGridModel(gridModel);
        previewPane.setOnGridChanged(() -> {
            if (!statusLabel.textProperty().isBound()) {
                statusLabel.setText(I18n.get("status.grid.set"));
            }
        });

        applyToAllCheckbox.setSelected(true);
        applyToAllCheckbox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            File current = filesListView.getSelectionModel().getSelectedItem();
            if (isSelected) {
                if (current != null && perImageGrids.containsKey(current)) {
                    gridModel.copyFrom(perImageGrids.get(current));
                }
                applyGridToUI(gridModel);
                statusLabel.textProperty().unbind();
                statusLabel.setText(I18n.get("status.mode.shared"));
            } else {
                if (current != null) {
                    GridModel imageGrid = perImageGrids.computeIfAbsent(current, f -> gridModel.copy());
                    applyGridToUI(imageGrid);
                }
                statusLabel.textProperty().unbind();
                statusLabel.setText(I18n.get("status.mode.per.image"));
            }
        });

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setLeft(buildSidebar());
        root.setCenter(buildPreviewArea());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1280, 820);
        scene.getStylesheets().add(cssPath);

        stage.titleProperty().bind(Bindings.createStringBinding(
            () -> "Batch Cropper FX",
            I18n.localeProperty()
        ));
        stage.setMinWidth(880);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    private GridModel activeGridModel() {
        if (!applyToAllCheckbox.isSelected()) {
            File selected = filesListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                return perImageGrids.computeIfAbsent(selected, f -> gridModel.copy());
            }
        }
        return gridModel;
    }

    private void applyGridToUI(GridModel grid) {
        ignoringSpinnerEvents = true;
        rowsSpinner.getValueFactory().setValue(grid.getRows());
        colsSpinner.getValueFactory().setValue(grid.getCols());
        ignoringSpinnerEvents = false;
        previewPane.setGridModel(grid);
    }

    private VBox buildSidebar() {
        Button addFilesButton = new Button();
        addFilesButton.textProperty().bind(I18n.bind("btn.add.files"));
        addFilesButton.getStyleClass().addAll("primary-button", "wide-button");
        addFilesButton.setOnAction(event -> openFiles());

        Button clearFilesButton = new Button();
        clearFilesButton.textProperty().bind(I18n.bind("btn.clear.files"));
        clearFilesButton.getStyleClass().add("wide-button");
        clearFilesButton.setOnAction(event -> {
            inputFiles.clear();
            filesListView.getSelectionModel().clearSelection();
            previewPane.setImage(null);
            perImageGrids.clear();
            statusLabel.textProperty().unbind();
            statusLabel.setText(I18n.get("status.cleared"));
        });

        filesListView.getStyleClass().add("file-list");
        filesListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        filesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldFile, newFile) -> showPreview(newFile));

        rowsSpinner.setEditable(true);
        colsSpinner.setEditable(true);
        rowsSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (ignoringSpinnerEvents) return;
            GridModel active = activeGridModel();
            active.repartitionWithinCurrentBounds(newV, colsSpinner.getValue());
            previewPane.setGridModel(active);
        });
        colsSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (ignoringSpinnerEvents) return;
            GridModel active = activeGridModel();
            active.repartitionWithinCurrentBounds(rowsSpinner.getValue(), newV);
            previewPane.setGridModel(active);
        });

        Label rowsLabel = new Label();
        rowsLabel.textProperty().bind(I18n.bind("label.rows"));
        rowsLabel.getStyleClass().add("field-label");
        HBox rowsRow = new HBox(10, rowsLabel, rowsSpinner);
        rowsRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(rowsSpinner, Priority.ALWAYS);
        rowsSpinner.setMaxWidth(Double.MAX_VALUE);

        Label colsLabel = new Label();
        colsLabel.textProperty().bind(I18n.bind("label.cols"));
        colsLabel.getStyleClass().add("field-label");
        HBox colsRow = new HBox(10, colsLabel, colsSpinner);
        colsRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(colsSpinner, Priority.ALWAYS);
        colsSpinner.setMaxWidth(Double.MAX_VALUE);

        ComboBox<CropShape> shapeBox = new ComboBox<>();
        shapeBox.getItems().addAll(CropShape.values());
        shapeBox.setValue(CropShape.RECTANGLE);
        shapeBox.setMaxWidth(Double.MAX_VALUE);
        shapeBox.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) previewPane.setCellShape(newV);
        });
        shapeBox.setCellFactory(list -> shapeCell());
        shapeBox.setButtonCell(shapeCell());
        I18n.localeProperty().addListener((obs, oldLang, newLang) -> {
            shapeBox.setCellFactory(list -> shapeCell());
            shapeBox.setButtonCell(shapeCell());
        });

        Button resetGridButton = new Button();
        resetGridButton.textProperty().bind(I18n.bind("btn.reset.grid"));
        resetGridButton.getStyleClass().add("wide-button");
        resetGridButton.setOnAction(event -> {
            GridModel active = activeGridModel();
            active.resetToFullImage();
            applyGridToUI(active);
            statusLabel.textProperty().unbind();
            statusLabel.setText(I18n.get("status.grid.reset"));
        });

        applyToAllCheckbox.getStyleClass().add("hint-label");

        Button chooseOutputButton = new Button();
        chooseOutputButton.textProperty().bind(I18n.bind("btn.choose.output"));
        chooseOutputButton.getStyleClass().add("wide-button");
        chooseOutputButton.setOnAction(event -> chooseOutputDirectory());

        ComboBox<String> formatBox = new ComboBox<>();
        formatBox.getItems().addAll("png", "jpg");
        formatBox.setValue("png");
        formatBox.setMaxWidth(100);

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setMaxSize(34, 34);

        Button exportButton = new Button();
        exportButton.textProperty().bind(I18n.bind("btn.export"));
        exportButton.getStyleClass().addAll("accent-button", "wide-button");
        exportButton.disableProperty().bind(Bindings.isEmpty(inputFiles));
        exportButton.setOnAction(event -> exportBatch(formatBox.getValue(), shapeBox.getValue(), exportButton, progressIndicator));

        HBox exportBox = new HBox(10, exportButton, progressIndicator);
        exportBox.setAlignment(Pos.CENTER_LEFT);

        Label mouseHelp = new Label();
        mouseHelp.textProperty().bind(I18n.bind("hint.drag"));
        mouseHelp.setWrapText(true);
        mouseHelp.getStyleClass().add("hint-label");

        ComboBox<String> langBox = new ComboBox<>();
        langBox.getItems().addAll("ru", "en");
        langBox.setValue(I18n.localeProperty().get());
        langBox.setMaxWidth(Double.MAX_VALUE);
        langBox.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) I18n.setLocale(newV);
        });

        Label shapeLabel = new Label();
        shapeLabel.textProperty().bind(I18n.bind("label.shape"));
        shapeLabel.getStyleClass().add("field-label");
        HBox shapeRow = new HBox(10, shapeLabel, shapeBox);
        shapeRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(shapeBox, Priority.ALWAYS);

        Label formatLabel = new Label();
        formatLabel.textProperty().bind(I18n.bind("label.format"));
        formatLabel.getStyleClass().add("field-label");
        HBox formatRow = new HBox(10, formatLabel, formatBox);
        formatRow.setAlignment(Pos.CENTER_LEFT);

        Label sectionFiles = sectionTitle("section.files");
        Label sectionGrid = sectionTitle("section.grid");
        Label sectionExport = sectionTitle("section.export");
        Label sectionLang = sectionTitle("section.language");

        VBox sidebar = new VBox(14,
                sectionLang,
                langBox,
                sectionFiles,
                addFilesButton,
                clearFilesButton,
                filesListView,
                sectionGrid,
                rowsRow,
                colsRow,
                shapeRow,
                resetGridButton,
                applyToAllCheckbox,
                mouseHelp,
                sectionExport,
                chooseOutputButton,
                outputLabel,
                formatRow,
                exportBox
        );
        sidebar.getStyleClass().add("sidebar");
        VBox.setVgrow(filesListView, Priority.ALWAYS);
        return sidebar;
    }

    private VBox buildPreviewArea() {
        Label previewTitle = new Label();
        previewTitle.textProperty().bind(I18n.bind("title.preview"));
        previewTitle.getStyleClass().add("header-title");

        Label previewSubTitle = new Label();
        previewSubTitle.textProperty().bind(I18n.bind("subtitle.preview"));
        previewSubTitle.getStyleClass().add("subtle-label");

        StackPane previewCard = new StackPane(previewPane);
        previewCard.getStyleClass().add("preview-card");
        VBox.setVgrow(previewCard, Priority.ALWAYS);

        Button previousButton = new Button();
        previousButton.textProperty().bind(I18n.bind("btn.prev"));
        Button nextButton = new Button();
        nextButton.textProperty().bind(I18n.bind("btn.next"));
        previousButton.setOnAction(event -> selectRelative(-1));
        nextButton.setOnAction(event -> selectRelative(1));
        previousButton.disableProperty().bind(Bindings.size(inputFiles).lessThanOrEqualTo(1));
        nextButton.disableProperty().bind(Bindings.size(inputFiles).lessThanOrEqualTo(1));

        Label selectionLabel = new Label();
        selectionLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            int idx = filesListView.getSelectionModel().getSelectedIndex();
            if (idx < 0 || inputFiles.isEmpty()) return I18n.get("status.no.image");
            return I18n.format("status.file.of", idx + 1, inputFiles.size(), inputFiles.get(idx).getName());
        }, filesListView.getSelectionModel().selectedIndexProperty(), inputFiles, I18n.localeProperty()));
        selectionLabel.getStyleClass().add("subtle-label");

        HBox navBar = new HBox(10, previousButton, nextButton, selectionLabel);
        navBar.setAlignment(Pos.CENTER_LEFT);

        VBox center = new VBox(10, previewTitle, previewSubTitle, navBar, previewCard);
        center.getStyleClass().add("center-pane");
        return center;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(statusLabel);
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    private Label sectionTitle(String key) {
        Label label = new Label();
        label.textProperty().bind(I18n.bind(key));
        label.getStyleClass().add("section-title");
        return label;
    }

    private ListCell<CropShape> shapeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(CropShape item, boolean empty) {
                super.updateItem(item, empty);
                textProperty().unbind();
                setText(empty || item == null ? null : I18n.get(item.key()));
            }
        };
    }

    private void openFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("btn.add.files"));
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );
        List<File> files = chooser.showOpenMultipleDialog(primaryStage);
        if (files == null || files.isEmpty()) return;

        List<File> added = new ArrayList<>();
        for (File file : files) {
            if (!inputFiles.contains(file)) added.add(file);
        }
        inputFiles.addAll(added);

        if (filesListView.getSelectionModel().getSelectedIndex() < 0 && !inputFiles.isEmpty()) {
            filesListView.getSelectionModel().selectFirst();
        }
        statusLabel.textProperty().unbind();
        statusLabel.setText(I18n.format("status.files.added", added.size(), inputFiles.size()));
    }

    private void showPreview(File file) {
        if (file == null) {
            previewPane.setImage(null);
            return;
        }
        if (!applyToAllCheckbox.isSelected()) {
            GridModel imageGrid = perImageGrids.computeIfAbsent(file, f -> gridModel.copy());
            applyGridToUI(imageGrid);
        }
        try {
            Image image = new Image(file.toURI().toString(), false);
            previewPane.setImage(image);
            statusLabel.textProperty().unbind();
            statusLabel.setText(I18n.format("status.preview.updated", file.getName()));
        } catch (Exception ex) {
            previewPane.setImage(null);
            showError(I18n.get("dlg.open.error.title"), ex.getMessage());
        }
    }

    private void selectRelative(int delta) {
        int current = filesListView.getSelectionModel().getSelectedIndex();
        if (current < 0) return;
        int next = current + delta;
        if (next >= 0 && next < inputFiles.size()) {
            filesListView.getSelectionModel().select(next);
            filesListView.scrollTo(next);
        }
    }

    private void chooseOutputDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(I18n.get("btn.choose.output"));
        File dir = chooser.showDialog(primaryStage);
        if (dir == null) return;
        outputDirectory = dir.toPath();
        outputLabel.textProperty().unbind();
        outputLabel.setText(outputDirectory.toString());
        statusLabel.textProperty().unbind();
        statusLabel.setText(I18n.get("status.output.selected"));
    }

    private void exportBatch(String format, CropShape shape, Button exportButton, ProgressIndicator progressIndicator) {
        if (inputFiles.isEmpty()) {
            showWarning(I18n.get("dlg.no.files.title"), I18n.get("dlg.no.files.msg"));
            return;
        }
        if (outputDirectory == null) {
            showWarning(I18n.get("dlg.no.output.title"), I18n.get("dlg.no.output.msg"));
            return;
        }

        List<File> filesToExport = new ArrayList<>(inputFiles);

        final Function<File, GridModel> gridResolver;
        if (applyToAllCheckbox.isSelected()) {
            GridModel snapshot = gridModel.copy();
            gridResolver = f -> snapshot;
        } else {
            Map<File, GridModel> snapshots = new HashMap<>();
            for (File f : filesToExport) {
                snapshots.put(f, perImageGrids.getOrDefault(f, gridModel).copy());
            }
            gridResolver = snapshots::get;
        }

        Task<BatchCropExporter.ExportResult> task = new Task<>() {
            @Override
            protected BatchCropExporter.ExportResult call() throws Exception {
                updateMessage(I18n.get("status.exporting"));
                return new BatchCropExporter().export(filesToExport, gridResolver, outputDirectory, format, shape);
            }
        };

        progressIndicator.visibleProperty().bind(task.runningProperty());
        progressIndicator.progressProperty().bind(task.progressProperty());
        exportButton.disableProperty().bind(task.runningProperty().or(Bindings.isEmpty(inputFiles)));
        statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(event -> {
            statusLabel.textProperty().unbind();
            progressIndicator.visibleProperty().unbind();
            progressIndicator.progressProperty().unbind();
            exportButton.disableProperty().unbind();
            exportButton.disableProperty().bind(Bindings.isEmpty(inputFiles));

            BatchCropExporter.ExportResult result = task.getValue();
            StringBuilder message = new StringBuilder(
                    I18n.format("dlg.export.done.msg", result.writtenFiles(), outputDirectory));
            if (!result.warnings().isEmpty()) {
                message.append(I18n.get("dlg.export.warnings"));
                result.warnings().forEach(w -> message.append("• ").append(w).append('\n'));
            }
            statusLabel.setText(I18n.format("status.export.done", result.writtenFiles()));
            showInfo(I18n.get("dlg.export.done.title"), message.toString());
        });

        task.setOnFailed(event -> {
            statusLabel.textProperty().unbind();
            progressIndicator.visibleProperty().unbind();
            progressIndicator.progressProperty().unbind();
            exportButton.disableProperty().unbind();
            exportButton.disableProperty().bind(Bindings.isEmpty(inputFiles));
            Throwable ex = task.getException();
            statusLabel.setText(I18n.get("status.export.error"));
            showError(I18n.get("dlg.export.error.title"),
                    ex == null ? I18n.get("dlg.export.unknown.error") : ex.getMessage());
        });

        Thread worker = new Thread(task, "batch-crop-export");
        worker.setDaemon(true);
        worker.start();
    }

    private void applyDialogStyle(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().add(cssPath);
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.initOwner(primaryStage);
        applyDialogStyle(alert);
        alert.showAndWait();
    }

    private void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.initOwner(primaryStage);
        applyDialogStyle(alert);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.initOwner(primaryStage);
        applyDialogStyle(alert);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
