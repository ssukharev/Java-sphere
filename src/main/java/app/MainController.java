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
import javafx.geometry.Pos;
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
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Bar;
import model.BarType;
import model.DomeModel;
import model.DomeParameters;
import model.Face;
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
import java.util.Comparator;
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
    private final Label measurementsLabel = new Label("Нет данных. Нажмите «Сформировать».");
    private final ScrollPane measurementsScroll = new ScrollPane(measurementsLabel);
    private final TitledPane measurementsPane = new TitledPane("Результаты измерений", measurementsScroll);

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
        rightPanel.getItems().addAll(buildViewerWithMeasurements(), barsTable);
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

    private StackPane buildViewerWithMeasurements() {
        measurementsPane.getStyleClass().add("measurements-pane");
        measurementsLabel.getStyleClass().add("measurements-text");
        measurementsLabel.setStyle("-fx-font-family: Menlo, Monaco, 'Courier New', monospace; -fx-font-size: 12px;");
        measurementsLabel.setWrapText(false);
        measurementsScroll.setFitToWidth(true);
        measurementsScroll.setFitToHeight(true);
        measurementsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        measurementsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        measurementsScroll.setPrefViewportHeight(220);
        measurementsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        measurementsPane.setExpanded(false);
        measurementsPane.setCollapsible(true);
        measurementsPane.setMaxWidth(330);
        VBox modePanel = buildModePanel();
        modePanel.setMinWidth(340);
        modePanel.setPrefWidth(340);
        modePanel.setMaxWidth(340);
        modePanel.setMinHeight(68);
        modePanel.setPrefHeight(68);
        modePanel.setMaxHeight(68);

        StackPane viewerStack = new StackPane(domeViewer.getView(), modePanel, measurementsPane);
        StackPane.setAlignment(modePanel, Pos.TOP_LEFT);
        StackPane.setMargin(modePanel, new Insets(12, 0, 0, 12));
        StackPane.setAlignment(measurementsPane, Pos.TOP_RIGHT);
        StackPane.setMargin(measurementsPane, new Insets(12, 12, 0, 0));
        return viewerStack;
    }

    private VBox buildModePanel() {
        Label title = new Label("Режим");
        title.getStyleClass().add("mode-panel-title");

        ToggleGroup modeGroup = new ToggleGroup();
        HBox buttons = new HBox(6);

        for (String modeName : List.of("План", "Каркас", "Схема", "Кровля", "Тент")) {
            ToggleButton button = new ToggleButton(modeName);
            button.getStyleClass().add("mode-toggle");
            button.setToggleGroup(modeGroup);
            button.setFocusTraversable(false);
            buttons.getChildren().add(button);
        }

        if (!buttons.getChildren().isEmpty() && buttons.getChildren().get(0) instanceof ToggleButton firstButton) {
            firstButton.setSelected(true);
        }

        VBox panel = new VBox(4, title, buttons);
        panel.getStyleClass().add("mode-panel");
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
            updateMeasurementsPanel(sourceModel);
            measurementsPane.setExpanded(true);
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
        measurementsLabel.setText("Нет данных. Нажмите «Сформировать».");
        measurementsPane.setExpanded(false);
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
        updateMeasurementsPanel(workingModel);
    }

    private void updateMeasurementsPanel(DomeModel model) {
        DomeModel target = model != null ? model : sourceModel;
        if (target == null || target.getNodes().isEmpty()) {
            measurementsLabel.setText("Нет данных. Нажмите «Сформировать».");
            return;
        }
        measurementsLabel.setText(buildMeasurementsText(target));
    }

    private String buildMeasurementsText(DomeModel model) {
        Set<Integer> connectedNodeIds = new HashSet<>();
        for (Bar bar : model.getBars()) {
            connectedNodeIds.add(bar.getNodeA());
            connectedNodeIds.add(bar.getNodeB());
        }
        List<Node3D> connectedNodes = model.getNodes().stream()
                .filter(node -> connectedNodeIds.contains(node.getId()))
                .toList();
        if (connectedNodes.isEmpty()) {
            connectedNodes = model.getNodes();
        }

        if (connectedNodes.isEmpty()) {
            return "Нет данных по узлам.";
        }

        List<Node3D> baseNodes = findBaseNodes(model, connectedNodes);
        double minBaseZ = baseNodes.stream().mapToDouble(Node3D::getZ).min().orElse(0);
        double maxZ = connectedNodes.stream().mapToDouble(Node3D::getZ).max().orElse(0);
        double height = Math.max(0, maxZ - minBaseZ);

        Point2 center = findPlanCenter(baseNodes);
        Range baseRadiusRange = findRadiusRange(baseNodes, center);
        double baseArea = polygonArea(baseNodes, center);

        Set<Long> activeEdges = buildEdgeSet(model.getBars());
        List<FaceGeometry> faceGeometries = collectFaceGeometries(model, activeEdges);
        double coverageArea = faceGeometries.stream().mapToDouble(FaceGeometry::area).sum();

        int uniqueFaceShapes = (int) faceGeometries.stream()
                .map(FaceGeometry::shapeSignature)
                .distinct()
                .count();
        int uniqueBarLengths = (int) model.getBars().stream()
                .map(bar -> Math.round(bar.getLength() * 1000.0))
                .distinct()
                .count();
        int uniqueVertexDegrees = (int) buildNodeDegrees(model.getBars()).values().stream()
                .distinct()
                .count();

        double totalBarLength = model.getBars().stream().mapToDouble(Bar::getLength).sum();
        double profile = model.getParameters().getTubeDiameter();
        int profileMm = (int) Math.round(profile * 1000.0);
        double volume = totalBarLength * profile * profile;
        Range barLengthRangeMm = range(model.getBars().stream().mapToDouble(bar -> bar.getLength() * 1000.0).toArray());

        Range adjacentAngles = computeAdjacentAngles(faceGeometries);
        Range minHeightsRange = range(faceGeometries.stream().mapToDouble(FaceGeometry::minHeightMm).toArray());
        Range maxSideRange = range(faceGeometries.stream().mapToDouble(FaceGeometry::maxSideMm).toArray());

        StringBuilder sb = new StringBuilder();
        sb.append("Высота от основания, м      ").append(fmt(height)).append('\n');
        sb.append("Радиус основания, м         ").append(fmtRange(baseRadiusRange, 2)).append('\n');
        sb.append("Площадь основания, м²       ").append(fmt(baseArea)).append('\n');
        sb.append("Площадь покрытия, м²        ").append(fmt(coverageArea)).append('\n');
        sb.append('\n');
        sb.append("Размеры").append('\n');
        sb.append("  Граней                    ").append(uniqueFaceShapes).append(" (").append(faceGeometries.size()).append(")").append('\n');
        sb.append("  Ребер                     ").append(uniqueBarLengths).append(" (").append(model.getBars().size()).append(")").append('\n');
        sb.append("  Вершин                    ").append(uniqueVertexDegrees).append(" (").append(connectedNodes.size()).append(")").append('\n');
        sb.append('\n');
        sb.append("Балки (ребра) ").append(profileMm).append("x").append(profileMm).append("мм").append('\n');
        sb.append("  Суммарная длина, м        ").append(fmt(totalBarLength)).append('\n');
        sb.append("  Объем ребер, м³           ").append(fmt(volume)).append('\n');
        sb.append("  Длина ребра, мм           ").append(fmtRange(barLengthRangeMm, 0)).append('\n');
        sb.append("  Угол смежных граней, °    ").append(fmtRange(adjacentAngles, 2)).append('\n');
        sb.append('\n');
        sb.append("Треугольники").append('\n');
        sb.append("  Мин. высота, мм           ").append(fmtRange(minHeightsRange, 0)).append('\n');
        sb.append("  Макс. сторона, мм         ").append(fmtRange(maxSideRange, 0));

        return sb.toString();
    }

    private List<Node3D> findBaseNodes(DomeModel model, List<Node3D> connectedNodes) {
        Set<Integer> baseIds = new HashSet<>();
        for (Bar bar : model.getBars()) {
            if (bar.getType() == BarType.BASE) {
                baseIds.add(bar.getNodeA());
                baseIds.add(bar.getNodeB());
            }
        }
        if (!baseIds.isEmpty()) {
            return connectedNodes.stream().filter(node -> baseIds.contains(node.getId())).toList();
        }

        double minZ = connectedNodes.stream().mapToDouble(Node3D::getZ).min().orElse(0);
        return connectedNodes.stream().filter(node -> Math.abs(node.getZ() - minZ) < 1e-6).toList();
    }

    private Point2 findPlanCenter(List<Node3D> nodes) {
        if (nodes.isEmpty()) {
            return new Point2(0, 0);
        }
        double sx = 0;
        double sy = 0;
        for (Node3D node : nodes) {
            sx += node.getX();
            sy += node.getY();
        }
        return new Point2(sx / nodes.size(), sy / nodes.size());
    }

    private Range findRadiusRange(List<Node3D> nodes, Point2 center) {
        double[] radii = nodes.stream()
                .mapToDouble(node -> {
                    double dx = node.getX() - center.x();
                    double dy = node.getY() - center.y();
                    return Math.hypot(dx, dy);
                })
                .toArray();
        return range(radii);
    }

    private double polygonArea(List<Node3D> nodes, Point2 center) {
        if (nodes.size() < 3) {
            return 0;
        }

        List<Node3D> ordered = new ArrayList<>(nodes);
        ordered.sort(Comparator.comparingDouble(node -> Math.atan2(node.getY() - center.y(), node.getX() - center.x())));

        double area2 = 0;
        for (int i = 0; i < ordered.size(); i++) {
            Node3D a = ordered.get(i);
            Node3D b = ordered.get((i + 1) % ordered.size());
            area2 += (a.getX() * b.getY()) - (b.getX() * a.getY());
        }
        return Math.abs(area2) * 0.5;
    }

    private Set<Long> buildEdgeSet(List<Bar> bars) {
        Set<Long> edges = new HashSet<>();
        for (Bar bar : bars) {
            edges.add(pairKey(bar.getNodeA(), bar.getNodeB()));
        }
        return edges;
    }

    private List<FaceGeometry> collectFaceGeometries(DomeModel model, Set<Long> activeEdges) {
        List<FaceGeometry> result = new ArrayList<>();
        for (Face face : model.getFaces()) {
            long ab = pairKey(face.getA(), face.getB());
            long bc = pairKey(face.getB(), face.getC());
            long ca = pairKey(face.getC(), face.getA());
            if (!activeEdges.contains(ab) || !activeEdges.contains(bc) || !activeEdges.contains(ca)) {
                continue;
            }

            Node3D a = model.getNodeById(face.getA());
            Node3D b = model.getNodeById(face.getB());
            Node3D c = model.getNodeById(face.getC());
            if (a == null || b == null || c == null) {
                continue;
            }
            FaceGeometry geometry = FaceGeometry.from(a, b, c);
            if (geometry != null) {
                result.add(geometry);
            }
        }
        return result;
    }

    private Map<Integer, Integer> buildNodeDegrees(List<Bar> bars) {
        Map<Integer, Integer> degrees = new HashMap<>();
        for (Bar bar : bars) {
            degrees.merge(bar.getNodeA(), 1, Integer::sum);
            degrees.merge(bar.getNodeB(), 1, Integer::sum);
        }
        return degrees;
    }

    private Range computeAdjacentAngles(List<FaceGeometry> faces) {
        Map<Long, List<FaceGeometry>> byEdge = new HashMap<>();
        for (FaceGeometry face : faces) {
            byEdge.computeIfAbsent(face.edgeAB(), key -> new ArrayList<>()).add(face);
            byEdge.computeIfAbsent(face.edgeBC(), key -> new ArrayList<>()).add(face);
            byEdge.computeIfAbsent(face.edgeCA(), key -> new ArrayList<>()).add(face);
        }

        List<Double> values = new ArrayList<>();
        for (List<FaceGeometry> edgeFaces : byEdge.values()) {
            if (edgeFaces.size() < 2) {
                continue;
            }
            for (int i = 0; i < edgeFaces.size(); i++) {
                for (int j = i + 1; j < edgeFaces.size(); j++) {
                    double dot = edgeFaces.get(i).normal().dot(edgeFaces.get(j).normal());
                    dot = Math.max(-1.0, Math.min(1.0, Math.abs(dot)));
                    double angle = 180.0 - Math.toDegrees(Math.acos(dot));
                    values.add(angle);
                }
            }
        }
        return range(values.stream().mapToDouble(Double::doubleValue).toArray());
    }

    private Range range(double[] values) {
        if (values == null || values.length == 0) {
            return Range.noData();
        }
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double value : values) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                continue;
            }
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        if (!Double.isFinite(min) || !Double.isFinite(max)) {
            return Range.noData();
        }
        return new Range(min, max);
    }

    private String fmt(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private String fmtRange(Range range, int decimals) {
        if (range.empty()) {
            return "—";
        }
        String pattern = decimals <= 0 ? "%.0f" : "%." + decimals + "f";
        if (Math.abs(range.max() - range.min()) < (decimals <= 0 ? 1.0 : Math.pow(10, -decimals))) {
            return String.format(Locale.US, pattern, range.min());
        }
        return String.format(Locale.US, pattern, range.min()) + "-" + String.format(Locale.US, pattern, range.max());
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

    private static long edgeKey(int a, int b) {
        int min = Math.min(a, b);
        int max = Math.max(a, b);
        return (((long) min) << 32) | (max & 0xffffffffL);
    }

    private static double distance(Node3D a, Node3D b) {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        double dz = b.getZ() - a.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static Vec3 normal(Node3D a, Node3D b, Node3D c) {
        Vec3 ab = new Vec3(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ());
        Vec3 ac = new Vec3(c.getX() - a.getX(), c.getY() - a.getY(), c.getZ() - a.getZ());
        Vec3 cross = ab.cross(ac);
        double norm = cross.norm();
        if (norm < 1e-12) {
            return new Vec3(0, 0, 1);
        }
        return cross.scale(1.0 / norm);
    }

    private record Point2(double x, double y) {
    }

    private record Range(double min, double max, boolean empty) {
        static Range noData() {
            return new Range(0, 0, true);
        }

        Range(double min, double max) {
            this(min, max, false);
        }
    }

    private record Vec3(double x, double y, double z) {
        Vec3 cross(Vec3 other) {
            return new Vec3(
                    y * other.z - z * other.y,
                    z * other.x - x * other.z,
                    x * other.y - y * other.x
            );
        }

        double dot(Vec3 other) {
            return x * other.x + y * other.y + z * other.z;
        }

        double norm() {
            return Math.sqrt(x * x + y * y + z * z);
        }

        Vec3 scale(double k) {
            return new Vec3(x * k, y * k, z * k);
        }
    }

    private record FaceGeometry(
            long edgeAB,
            long edgeBC,
            long edgeCA,
            double area,
            double maxSideMm,
            double minHeightMm,
            String shapeSignature,
            Vec3 normal
    ) {
        static FaceGeometry from(Node3D a, Node3D b, Node3D c) {
            double ab = distance(a, b);
            double bc = distance(b, c);
            double ca = distance(c, a);
            double s = 0.5 * (ab + bc + ca);
            double areaSquared = s * (s - ab) * (s - bc) * (s - ca);
            if (areaSquared <= 1e-12) {
                return null;
            }

            double area = Math.sqrt(areaSquared);
            double maxSideMm = Math.max(ab, Math.max(bc, ca)) * 1000.0;
            double minHeightMm = (2.0 * area / Math.max(ab, Math.max(bc, ca))) * 1000.0;

            long abMm = Math.round(ab * 1000.0);
            long bcMm = Math.round(bc * 1000.0);
            long caMm = Math.round(ca * 1000.0);
            long[] lengths = new long[]{abMm, bcMm, caMm};
            java.util.Arrays.sort(lengths);
            String signature = lengths[0] + "-" + lengths[1] + "-" + lengths[2];

            return new FaceGeometry(
                    edgeKey(a.getId(), b.getId()),
                    edgeKey(b.getId(), c.getId()),
                    edgeKey(c.getId(), a.getId()),
                    area,
                    maxSideMm,
                    minHeightMm,
                    signature,
                    MainController.normal(a, b, c)
            );
        }
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
