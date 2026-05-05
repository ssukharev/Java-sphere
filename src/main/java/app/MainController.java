package app;

import export.CsvExporter;
import export.ObjExporter;
import export.StlExporter;
import generator.DomeGenerator;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Bar;
import model.BarType;
import model.DomeModel;
import model.DomeParameters;
import model.MeshType;
import model.Node3D;
import view.DomeViewer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Set;

public class MainController {
    private final Stage stage;

    private final DomeGenerator domeGenerator = new DomeGenerator();
    private final DomeViewer domeViewer = new DomeViewer();
    private final CsvExporter csvExporter = new CsvExporter();
    private final ObjExporter objExporter = new ObjExporter();
    private final StlExporter stlExporter = new StlExporter();

    private final TextField radiusField = new TextField("10.0");
    private final TextField ringsField = new TextField("6");
    private final TextField segmentsField = new TextField("12");
    private final TextField tubeDiameterField = new TextField("0.05");
    private final TextField visualThicknessField = new TextField("0.12");
    private final TextField domeHeightFactorField = new TextField("0.5");
    private final ComboBox<MeshType> meshTypeComboBox = new ComboBox<>();

    private final CheckBox showNodesCheckBox = new CheckBox("Показывать узлы");
    private final CheckBox showAxesCheckBox = new CheckBox("Показывать оси");
    private final CheckBox colorBarsCheckBox = new CheckBox("Цветные стержни");

    private final TableView<BarEditRow> barsTable = new TableView<>();
    private final ObservableList<BarEditRow> barsData = FXCollections.observableArrayList();

    private final Label statusLabel = new Label("Модель не сформирована");

    private DomeModel sourceModel;
    private DomeModel workingModel;
    private int nextCustomBarId = 1;
    private boolean muteRowEvents;

    public MainController(Stage stage) {
        this.stage = stage;
    }

    public Parent createView() {
        meshTypeComboBox.getItems().setAll(MeshType.values());
        meshTypeComboBox.getSelectionModel().select(MeshType.RING_TRIANGULAR);

        showNodesCheckBox.setSelected(true);
        showAxesCheckBox.setSelected(true);
        colorBarsCheckBox.setSelected(true);

        domeViewer.setShowNodes(showNodesCheckBox.isSelected());
        domeViewer.setShowAxes(showAxesCheckBox.isSelected());
        domeViewer.setColorBars(colorBarsCheckBox.isSelected());

        showNodesCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> domeViewer.setShowNodes(newValue));
        showAxesCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> domeViewer.setShowAxes(newValue));
        colorBarsCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> domeViewer.setColorBars(newValue));
        domeViewer.setOnBarSelectClick(this::handleBarSelectionClick);
        domeViewer.setOnNodeConnect(this::handleNodeConnect);

        configureBarsTable();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(8));

        ScrollPane leftPanel = new ScrollPane(buildLeftPanel());
        leftPanel.setFitToWidth(true);
        leftPanel.setPrefWidth(360);
        leftPanel.setMinWidth(330);

        SplitPane rightPanel = new SplitPane();
        rightPanel.setOrientation(Orientation.VERTICAL);
        rightPanel.getItems().addAll(domeViewer.getView(), barsTable);
        rightPanel.setDividerPositions(0.7);

        root.setLeft(leftPanel);
        root.setCenter(rightPanel);
        root.setBottom(statusLabel);
        BorderPane.setMargin(statusLabel, new Insets(8, 0, 0, 4));

        return root;
    }

    private VBox buildLeftPanel() {
        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(8);
        inputGrid.setVgap(8);

        int row = 0;
        addRow(inputGrid, row++, "Радиус", radiusField);
        addRow(inputGrid, row++, "Количество колец", ringsField);
        addRow(inputGrid, row++, "Количество сегментов", segmentsField);
        addRow(inputGrid, row++, "Диаметр трубы", tubeDiameterField);
        addRow(inputGrid, row++, "Толщина визуального стержня", visualThicknessField);
        addRow(inputGrid, row++, "Высота купола", domeHeightFactorField);
        addRow(inputGrid, row, "Тип сетки", meshTypeComboBox);

        Button generateButton = new Button("Сформировать");
        generateButton.setMaxWidth(Double.MAX_VALUE);
        generateButton.setOnAction(event -> handleGenerate());

        Button clearButton = new Button("Очистить");
        clearButton.setMaxWidth(Double.MAX_VALUE);
        clearButton.setOnAction(event -> handleClear());

        Button resetCameraButton = new Button("Сбросить камеру");
        resetCameraButton.setMaxWidth(Double.MAX_VALUE);
        resetCameraButton.setOnAction(event -> domeViewer.resetCamera());

        Button removeSelectedButton = new Button("Удалить выбранные");
        removeSelectedButton.setMaxWidth(Double.MAX_VALUE);
        removeSelectedButton.setOnAction(event -> handleRemoveSelectedBar());

        Button exportCsvButton = new Button("Экспорт CSV");
        exportCsvButton.setMaxWidth(Double.MAX_VALUE);
        exportCsvButton.setOnAction(event -> handleExportCsv());

        Button exportObjButton = new Button("Экспорт OBJ");
        exportObjButton.setMaxWidth(Double.MAX_VALUE);
        exportObjButton.setOnAction(event -> handleExportObj());

        Button exportStlButton = new Button("Экспорт STL");
        exportStlButton.setMaxWidth(Double.MAX_VALUE);
        exportStlButton.setOnAction(event -> handleExportStl());

        VBox buttonsBox = new VBox(8,
                generateButton,
                clearButton,
                resetCameraButton,
                removeSelectedButton,
                new Separator(),
                exportCsvButton,
                exportObjButton,
                exportStlButton
        );

        VBox checkBoxBox = new VBox(6, showNodesCheckBox, showAxesCheckBox, colorBarsCheckBox);
        TitledPane displayOptionsPane = new TitledPane("Отображение", checkBoxBox);
        displayOptionsPane.setCollapsible(false);

        VBox panel = new VBox(12, inputGrid, displayOptionsPane, buttonsBox);
        panel.setPadding(new Insets(6));
        panel.setFillWidth(true);
        VBox.setVgrow(buttonsBox, Priority.NEVER);

        return panel;
    }

    private void addRow(GridPane grid, int row, String labelText, javafx.scene.Node field) {
        Label label = new Label(labelText);
        grid.add(label, 0, row);
        grid.add(field, 1, row);

        if (field instanceof TextField textField) {
            textField.setPrefColumnCount(10);
        }
    }

    private void configureBarsTable() {
        barsTable.setEditable(true);
        barsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<BarEditRow, Integer> idColumn = new TableColumn<>("№");
        idColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        idColumn.setPrefWidth(65);

        TableColumn<BarEditRow, Integer> nodeAColumn = new TableColumn<>("Узел A");
        nodeAColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getNodeA()));
        nodeAColumn.setPrefWidth(85);

        TableColumn<BarEditRow, Integer> nodeBColumn = new TableColumn<>("Узел B");
        nodeBColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getNodeB()));
        nodeBColumn.setPrefWidth(85);

        TableColumn<BarEditRow, String> lengthColumn = new TableColumn<>("Длина");
        lengthColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(format(data.getValue().getLength())));
        lengthColumn.setPrefWidth(110);

        TableColumn<BarEditRow, BarType> typeColumn = new TableColumn<>("Тип");
        typeColumn.setCellValueFactory(data -> data.getValue().typeProperty());
        typeColumn.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(BarType.values())));
        typeColumn.setOnEditCommit(event -> {
            event.getRowValue().setType(event.getNewValue());
            rebuildWorkingModelAndRefresh();
        });
        typeColumn.setPrefWidth(140);

        TableColumn<BarEditRow, Boolean> activeColumn = new TableColumn<>("Активен");
        activeColumn.setCellValueFactory(data -> data.getValue().activeProperty());
        activeColumn.setCellFactory(CheckBoxTableCell.forTableColumn(activeColumn));
        activeColumn.setEditable(true);
        activeColumn.setPrefWidth(90);

        barsTable.getColumns().addAll(idColumn, nodeAColumn, nodeBColumn, lengthColumn, typeColumn, activeColumn);
        barsTable.setItems(barsData);
        barsTable.setPlaceholder(new Label("Нет данных. Нажмите «Сформировать»."));
        barsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        barsTable.getSelectionModel().getSelectedItems().addListener((ListChangeListener<BarEditRow>) change -> syncViewerSelectionFromTable());
    }

    private void handleGenerate() {
        try {
            DomeParameters parameters = readParameters();
            List<String> validationErrors = parameters.validate();
            if (!validationErrors.isEmpty()) {
                showValidationError(String.join("\n", validationErrors));
                return;
            }

            sourceModel = domeGenerator.generate(parameters);
            resetEditableBarsFromSource();
        } catch (IllegalArgumentException ex) {
            showValidationError(ex.getMessage());
        } catch (Exception ex) {
            showError("Ошибка генерации", ex.getMessage());
        }
    }

    private void handleClear() {
        sourceModel = null;
        workingModel = null;
        barsData.clear();
        domeViewer.clearModel();
        statusLabel.setText("Модель очищена");
    }

    private void handleRemoveSelectedBar() {
        List<BarEditRow> selectedRows = new ArrayList<>(barsTable.getSelectionModel().getSelectedItems());
        if (selectedRows.isEmpty()) {
            showInfo("Редактирование", "Выберите стержни в таблице.");
            return;
        }

        barsData.removeAll(selectedRows);
        rebuildWorkingModelAndRefresh();
        // JavaFX TableView по умолчанию выбирает соседнюю строку после удаления.
        // Очищаем selection после обновления списка, чтобы не было авто-выбора.
        Platform.runLater(() -> {
            barsTable.getSelectionModel().clearSelection();
            syncViewerSelectionFromTable();
        });
    }

    private void handleNodeConnect(DomeViewer.NodeConnectEvent event) {
        if (event == null || !ensureSourceModelExists()) {
            return;
        }

        try {
            if (containsPair(event.fromNodeId(), event.toNodeId())) {
                statusLabel.setText("Стержень уже существует между узлами " + event.fromNodeId() + " и " + event.toNodeId());
                return;
            }

            BarEditRow newRow = BarEditRow.create(
                    nextCustomBarId++,
                    event.fromNodeId(),
                    event.toNodeId(),
                    BarType.DIAGONAL,
                    nodeMap(sourceModel)
            );
            registerRow(newRow);
            barsData.add(newRow);
            barsTable.getSelectionModel().clearSelection();
            barsTable.getSelectionModel().select(newRow);
            barsTable.scrollTo(newRow);
            rebuildWorkingModelAndRefresh();
            statusLabel.setText("Добавлен стержень между узлами " + event.fromNodeId() + " и " + event.toNodeId());
        } catch (IllegalArgumentException ex) {
            showValidationError(ex.getMessage());
        }
    }

    private void resetEditableBarsFromSource() {
        if (!ensureSourceModelExists()) {
            return;
        }

        muteRowEvents = true;
        barsData.clear();
        for (Bar bar : sourceModel.getBars()) {
            BarEditRow row = new BarEditRow(bar);
            registerRow(row);
            barsData.add(row);
        }
        muteRowEvents = false;

        nextCustomBarId = sourceModel.getBars().stream().mapToInt(Bar::getId).max().orElse(0) + 1;
        rebuildWorkingModelAndRefresh();
    }

    private void registerRow(BarEditRow row) {
        row.activeProperty().addListener((obs, oldValue, newValue) -> {
            if (!muteRowEvents) {
                rebuildWorkingModelAndRefresh();
            }
        });

        row.typeProperty().addListener((obs, oldValue, newValue) -> {
            if (!muteRowEvents) {
                rebuildWorkingModelAndRefresh();
            }
        });
    }

    private void handleBarSelectionClick(DomeViewer.BarPickEvent event) {
        if (event == null) {
            return;
        }

        int barId = event.barId();
        boolean multiToggle = event.multiSelectToggle();
        for (int i = 0; i < barsData.size(); i++) {
            if (barsData.get(i).getId() == barId) {
                if (!multiToggle) {
                    barsTable.getSelectionModel().clearSelection();
                    barsTable.getSelectionModel().select(i);
                } else if (barsTable.getSelectionModel().isSelected(i)) {
                    barsTable.getSelectionModel().clearSelection(i);
                } else {
                    barsTable.getSelectionModel().select(i);
                }
                barsTable.scrollTo(i);
                return;
            }
        }
    }

    private void syncViewerSelectionFromTable() {
        Set<Integer> selectedIds = barsTable.getSelectionModel().getSelectedItems().stream()
                .map(BarEditRow::getId)
                .collect(Collectors.toSet());
        domeViewer.setHighlightedBars(selectedIds);
    }

    private void rebuildWorkingModelAndRefresh() {
        if (sourceModel == null) {
            return;
        }

        List<Bar> activeBars = new ArrayList<>();
        Set<Long> uniquePairs = new HashSet<>();

        for (BarEditRow row : barsData) {
            if (!row.isActive()) {
                continue;
            }

            Bar bar = row.toBar();
            long key = pairKey(bar.getNodeA(), bar.getNodeB());
            if (uniquePairs.contains(key)) {
                continue;
            }

            uniquePairs.add(key);
            activeBars.add(bar);
        }

        workingModel = new DomeModel(
                sourceModel.getParameters(),
                sourceModel.getNodes(),
                activeBars,
                sourceModel.getFaces()
        );

        domeViewer.displayModel(
                workingModel,
                sourceModel.getParameters().getVisualThickness(),
                showNodesCheckBox.isSelected(),
                colorBarsCheckBox.isSelected()
        );
        domeViewer.setShowAxes(showAxesCheckBox.isSelected());
        syncViewerSelectionFromTable();

        long activeCount = barsData.stream().filter(BarEditRow::isActive).count();
        statusLabel.setText(String.format(
                Locale.forLanguageTag("ru-RU"),
                "Узлов: %d, активных стержней: %d из %d",
                sourceModel.getNodes().size(),
                activeCount,
                barsData.size()
        ));
    }

    private boolean containsPair(int nodeA, int nodeB) {
        long key = pairKey(nodeA, nodeB);
        for (BarEditRow row : barsData) {
            if (pairKey(row.getNodeA(), row.getNodeB()) == key) {
                return true;
            }
        }
        return false;
    }

    private long pairKey(int a, int b) {
        int min = Math.min(a, b);
        int max = Math.max(a, b);
        return (((long) min) << 32) | (max & 0xffffffffL);
    }

    private Map<Integer, Node3D> nodeMap(DomeModel model) {
        Map<Integer, Node3D> map = new HashMap<>();
        for (Node3D node : model.getNodes()) {
            map.put(node.getId(), node);
        }
        return map;
    }

    private void handleExportCsv() {
        if (!ensureWorkingModelExists()) {
            return;
        }

        File file = chooseSaveFile("Сохранить CSV", "dome-bars.csv", new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        if (file == null) {
            return;
        }

        try {
            csvExporter.export(file.toPath(), workingModel);
            statusLabel.setText("CSV экспортирован: " + file.getAbsolutePath());
        } catch (IOException ex) {
            showError("Ошибка экспорта CSV", ex.getMessage());
        }
    }

    private void handleExportObj() {
        if (!ensureWorkingModelExists()) {
            return;
        }

        File file = chooseSaveFile("Сохранить OBJ", "dome-model.obj", new FileChooser.ExtensionFilter("OBJ (*.obj)", "*.obj"));
        if (file == null) {
            return;
        }

        try {
            objExporter.export(file.toPath(), workingModel);
            statusLabel.setText("OBJ экспортирован: " + file.getAbsolutePath());
        } catch (IOException ex) {
            showError("Ошибка экспорта OBJ", ex.getMessage());
        }
    }

    private void handleExportStl() {
        if (!ensureWorkingModelExists()) {
            return;
        }

        File file = chooseSaveFile("Сохранить STL", "dome-model.stl", new FileChooser.ExtensionFilter("STL (*.stl)", "*.stl"));
        if (file == null) {
            return;
        }

        try {
            stlExporter.export(file.toPath(), workingModel);
            statusLabel.setText("STL экспортирован: " + file.getAbsolutePath());
        } catch (UnsupportedOperationException ex) {
            showInfo("STL", ex.getMessage());
            statusLabel.setText("STL сохранен как заглушка: " + file.getAbsolutePath());
        } catch (IOException ex) {
            showError("Ошибка экспорта STL", ex.getMessage());
        }
    }

    private DomeParameters readParameters() {
        double radius = parseDouble(radiusField, "Радиус");
        int rings = parseInt(ringsField, "Количество колец");
        int segments = parseInt(segmentsField, "Количество сегментов");
        double tubeDiameter = parseDouble(tubeDiameterField, "Диаметр трубы");
        double visualThickness = parseDouble(visualThicknessField, "Толщина визуального стержня");
        double domeHeightFactor = parseDouble(domeHeightFactorField, "Высота купола");

        MeshType meshType = meshTypeComboBox.getValue();

        return new DomeParameters(
                radius,
                rings,
                segments,
                tubeDiameter,
                visualThickness,
                domeHeightFactor,
                meshType
        );
    }

    private double parseDouble(TextField field, String fieldName) {
        String value = field.getText();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " не заполнен.");
        }

        String normalized = value.trim().replace(',', '.');
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " должен быть числом.");
        }
    }

    private int parseInt(TextField field, String fieldName) {
        String value = field.getText();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " не заполнен.");
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " должен быть целым числом.");
        }
    }

    private boolean ensureSourceModelExists() {
        if (sourceModel == null) {
            showInfo("Нет модели", "Сначала нажмите «Сформировать».");
            return false;
        }
        return true;
    }

    private boolean ensureWorkingModelExists() {
        if (workingModel == null) {
            showInfo("Нет модели", "Сначала нажмите «Сформировать».");
            return false;
        }
        return true;
    }

    private File chooseSaveFile(String title, String initialName, FileChooser.ExtensionFilter extensionFilter) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.setInitialFileName(initialName);
        chooser.getExtensionFilters().add(extensionFilter);

        File selected = chooser.showSaveDialog(stage);
        if (selected == null) {
            return null;
        }

        Path path = selected.toPath();
        if (path.getParent() != null) {
            path.getParent().toFile().mkdirs();
        }
        return selected;
    }

    private String format(double value) {
        return String.format(Locale.US, "%.5f", value);
    }

    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Ошибка параметров");
        alert.setHeaderText("Проверьте введенные значения");
        alert.setContentText(message);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null || message.isBlank() ? "Неизвестная ошибка" : message);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(stage);
        alert.showAndWait();
    }
}
