package app;

import export.CsvExporter;
import export.ObjExporter;
import export.StlExporter;
import generator.DomeGenerator;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
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
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Bar;
import model.DomeModel;
import model.DomeParameters;
import model.MeshType;
import view.DomeViewer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

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

    private final TableView<Bar> barsTable = new TableView<>();
    private final ObservableList<Bar> barsData = FXCollections.observableArrayList();

    private final Label statusLabel = new Label("Модель не сформирована");

    private DomeModel currentModel;

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

        configureBarsTable();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(8));

        ScrollPane leftPanel = new ScrollPane(buildLeftPanel());
        leftPanel.setFitToWidth(true);
        leftPanel.setPrefWidth(330);
        leftPanel.setMinWidth(300);

        SplitPane rightPanel = new SplitPane();
        rightPanel.setOrientation(Orientation.VERTICAL);
        rightPanel.getItems().addAll(domeViewer.getView(), barsTable);
        rightPanel.setDividerPositions(0.72);

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
                new Separator(),
                exportCsvButton,
                exportObjButton,
                exportStlButton
        );

        VBox checkBoxBox = new VBox(6, showNodesCheckBox, showAxesCheckBox, colorBarsCheckBox);
        TitledPane displayOptionsPane = new TitledPane("Отображение", checkBoxBox);
        displayOptionsPane.setCollapsible(false);

        TitledPane barsPane = new TitledPane("Ведомость стержней", new Label("Таблица справа"));
        barsPane.setCollapsible(false);

        VBox panel = new VBox(12, inputGrid, displayOptionsPane, buttonsBox, barsPane);
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
        TableColumn<Bar, Integer> idColumn = new TableColumn<>("№");
        idColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        idColumn.setPrefWidth(70);

        TableColumn<Bar, Integer> nodeAColumn = new TableColumn<>("Узел A");
        nodeAColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getNodeA()));
        nodeAColumn.setPrefWidth(90);

        TableColumn<Bar, Integer> nodeBColumn = new TableColumn<>("Узел B");
        nodeBColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getNodeB()));
        nodeBColumn.setPrefWidth(90);

        TableColumn<Bar, String> lengthColumn = new TableColumn<>("Длина");
        lengthColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(format(data.getValue().getLength())));
        lengthColumn.setPrefWidth(120);

        TableColumn<Bar, String> typeColumn = new TableColumn<>("Тип");
        typeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getType().getDisplayName()));
        typeColumn.setPrefWidth(150);

        barsTable.getColumns().addAll(idColumn, nodeAColumn, nodeBColumn, lengthColumn, typeColumn);
        barsTable.setItems(barsData);
        barsTable.setPlaceholder(new Label("Нет данных. Нажмите «Сформировать»."));
        barsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void handleGenerate() {
        try {
            DomeParameters parameters = readParameters();
            List<String> validationErrors = parameters.validate();
            if (!validationErrors.isEmpty()) {
                showValidationError(String.join("\n", validationErrors));
                return;
            }

            currentModel = domeGenerator.generate(parameters);
            barsData.setAll(currentModel.getBars());
            domeViewer.displayModel(
                    currentModel,
                    parameters.getVisualThickness(),
                    showNodesCheckBox.isSelected(),
                    colorBarsCheckBox.isSelected()
            );
            domeViewer.setShowAxes(showAxesCheckBox.isSelected());

            statusLabel.setText(String.format(
                    Locale.forLanguageTag("ru-RU"),
                    "Сформировано: узлов %d, стержней %d, граней %d",
                    currentModel.getNodes().size(),
                    currentModel.getBars().size(),
                    currentModel.getFaces().size()
            ));
        } catch (IllegalArgumentException ex) {
            showValidationError(ex.getMessage());
        } catch (Exception ex) {
            showError("Ошибка генерации", ex.getMessage());
        }
    }

    private void handleClear() {
        currentModel = null;
        barsData.clear();
        domeViewer.clearModel();
        statusLabel.setText("Модель очищена");
    }

    private void handleExportCsv() {
        if (!ensureModelExists()) {
            return;
        }

        File file = chooseSaveFile("Сохранить CSV", "dome-bars.csv", new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        if (file == null) {
            return;
        }

        try {
            csvExporter.export(file.toPath(), currentModel);
            statusLabel.setText("CSV экспортирован: " + file.getAbsolutePath());
        } catch (IOException ex) {
            showError("Ошибка экспорта CSV", ex.getMessage());
        }
    }

    private void handleExportObj() {
        if (!ensureModelExists()) {
            return;
        }

        File file = chooseSaveFile("Сохранить OBJ", "dome-model.obj", new FileChooser.ExtensionFilter("OBJ (*.obj)", "*.obj"));
        if (file == null) {
            return;
        }

        try {
            objExporter.export(file.toPath(), currentModel);
            statusLabel.setText("OBJ экспортирован: " + file.getAbsolutePath());
        } catch (IOException ex) {
            showError("Ошибка экспорта OBJ", ex.getMessage());
        }
    }

    private void handleExportStl() {
        if (!ensureModelExists()) {
            return;
        }

        File file = chooseSaveFile("Сохранить STL", "dome-model.stl", new FileChooser.ExtensionFilter("STL (*.stl)", "*.stl"));
        if (file == null) {
            return;
        }

        try {
            stlExporter.export(file.toPath(), currentModel);
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

    private boolean ensureModelExists() {
        if (currentModel == null) {
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
