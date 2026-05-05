package view;

import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import model.Bar;
import model.BarType;
import model.DomeModel;
import model.Node3D;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class DomeViewer {
    private static final double MIN_CAMERA_Z = -10000;
    private static final double MAX_CAMERA_Z = -40;
    private static final double TARGET_MODEL_HALF_SIZE = 260;
    private static final double DEFAULT_ROTATE_X = 0;
    private static final double DEFAULT_ROTATE_Y = 0;

    private final StackPane root = new StackPane();
    private final Group world = new Group();
    private final Group domeGroup = new Group();
    private final Group barsGroup = new Group();
    private final Group nodesGroup = new Group();
    private final Group axesGroup = GeometryUtils.createAxes(250, 0.6);

    private final Rotate rotateX = new Rotate(DEFAULT_ROTATE_X, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(DEFAULT_ROTATE_Y, Rotate.Y_AXIS);
    private final Translate pan = new Translate();
    private final Scale scale = new Scale(1, 1, 1);

    private final PerspectiveCamera camera = new PerspectiveCamera(true);

    private final PhongMaterial defaultBarMaterial = new PhongMaterial(Color.web("#7f8c8d"));
    private final PhongMaterial nodeMaterial = new PhongMaterial(Color.web("#111111"));
    private final PhongMaterial nodeStartMaterial = new PhongMaterial(Color.web("#ffb400"));
    private final PhongMaterial nodeTargetMaterial = new PhongMaterial(Color.web("#2ecc71"));
    private final PhongMaterial horizontalMaterial = new PhongMaterial(Color.web("#f39c12"));
    private final PhongMaterial radialMaterial = new PhongMaterial(Color.web("#27ae60"));
    private final PhongMaterial diagonalMaterial = new PhongMaterial(Color.web("#2980b9"));
    private final PhongMaterial baseMaterial = new PhongMaterial(Color.web("#c0392b"));
    private final PhongMaterial selectedBarMaterial = new PhongMaterial(Color.web("#111111"));

    private DomeModel currentModel;
    private double currentBarRadius = 0.12;
    private boolean colorBars = true;
    private final Set<Integer> highlightedBarIds = new HashSet<>();

    private Consumer<BarPickEvent> onBarSelectClick;
    private Consumer<NodeConnectEvent> onNodeConnect;

    private Integer connectionStartNodeId;
    private Integer connectionTargetNodeId;
    private boolean connectionDragActive;

    private double dragAnchorX;
    private double dragAnchorY;
    private double anchorRotateX;
    private double anchorRotateY;
    private double anchorPanX;
    private double anchorPanY;

    public DomeViewer() {
        domeGroup.getChildren().addAll(barsGroup, nodesGroup);
        domeGroup.getTransforms().add(scale);

        world.getTransforms().addAll(rotateY, rotateX, pan);
        world.getChildren().addAll(axesGroup, domeGroup);

        SubScene subScene = new SubScene(world, 900, 700, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#e9edf1"));

        camera.setNearClip(0.1);
        camera.setFarClip(50000);
        camera.setTranslateZ(-900);
        subScene.setCamera(camera);

        AmbientLight ambientLight = new AmbientLight(Color.color(0.74, 0.74, 0.74));

        PointLight keyLight = new PointLight(Color.color(0.72, 0.72, 0.72));
        keyLight.setTranslateX(-650);
        keyLight.setTranslateY(-450);
        keyLight.setTranslateZ(-700);

        PointLight fillLight = new PointLight(Color.color(0.38, 0.38, 0.38));
        fillLight.setTranslateX(620);
        fillLight.setTranslateY(-280);
        fillLight.setTranslateZ(520);

        world.getChildren().addAll(ambientLight, keyLight, fillLight);

        softenMaterial(defaultBarMaterial);
        softenMaterial(nodeMaterial);
        softenMaterial(nodeStartMaterial);
        softenMaterial(nodeTargetMaterial);
        softenMaterial(horizontalMaterial);
        softenMaterial(radialMaterial);
        softenMaterial(diagonalMaterial);
        softenMaterial(baseMaterial);
        softenMaterial(selectedBarMaterial);

        setupMouseControls(subScene);

        subScene.widthProperty().bind(root.widthProperty());
        subScene.heightProperty().bind(root.heightProperty());
        root.getChildren().add(subScene);
    }

    public Parent getView() {
        return root;
    }

    public void displayModel(DomeModel model, double barRadius, boolean showNodes, boolean colorBarsEnabled) {
        this.currentModel = model;
        this.currentBarRadius = barRadius;
        this.colorBars = colorBarsEnabled;
        renderModel();
        setShowNodes(showNodes);
    }

    public void clearModel() {
        currentModel = null;
        highlightedBarIds.clear();
        connectionStartNodeId = null;
        connectionTargetNodeId = null;
        connectionDragActive = false;
        barsGroup.getChildren().clear();
        nodesGroup.getChildren().clear();
        scale.setX(1);
        scale.setY(1);
        scale.setZ(1);
    }

    public void setShowNodes(boolean show) {
        nodesGroup.setVisible(show);
    }

    public void setShowAxes(boolean show) {
        axesGroup.setVisible(show);
    }

    public void setColorBars(boolean enabled) {
        this.colorBars = enabled;
        if (currentModel != null) {
            renderModel();
        }
    }

    public void setHighlightedBars(Set<Integer> barIds) {
        highlightedBarIds.clear();
        if (barIds != null) {
            highlightedBarIds.addAll(barIds);
        }
        if (currentModel != null) {
            renderModel();
        }
    }

    public void resetCamera() {
        rotateX.setAngle(DEFAULT_ROTATE_X);
        rotateY.setAngle(DEFAULT_ROTATE_Y);
        pan.setX(0);
        pan.setY(0);
        camera.setTranslateZ(-900);
    }

    public void setOnBarSelectClick(Consumer<BarPickEvent> onBarSelectClick) {
        this.onBarSelectClick = onBarSelectClick;
    }

    public void setOnNodeConnect(Consumer<NodeConnectEvent> onNodeConnect) {
        this.onNodeConnect = onNodeConnect;
    }

    private void renderModel() {
        barsGroup.getChildren().clear();
        nodesGroup.getChildren().clear();

        if (currentModel == null || currentModel.getNodes().isEmpty()) {
            return;
        }

        Map<Integer, Point3D> rawPoints = new HashMap<>();
        for (Node3D node : currentModel.getNodes()) {
            rawPoints.put(node.getId(), GeometryUtils.toFxPoint(node));
        }

        Point3D centroid = computeCentroid(rawPoints.values().stream().toList());

        Map<Integer, Point3D> centeredPoints = new HashMap<>();
        double maxAbs = 1;
        for (Node3D node : currentModel.getNodes()) {
            Point3D centered = rawPoints.get(node.getId()).subtract(centroid);
            centeredPoints.put(node.getId(), centered);

            maxAbs = Math.max(maxAbs, Math.abs(centered.getX()));
            maxAbs = Math.max(maxAbs, Math.abs(centered.getY()));
            maxAbs = Math.max(maxAbs, Math.abs(centered.getZ()));
        }

        double scaleFactor = TARGET_MODEL_HALF_SIZE / maxAbs;
        scale.setX(scaleFactor);
        scale.setY(scaleFactor);
        scale.setZ(scaleFactor);

        Set<Integer> connectedNodeIds = new HashSet<>();
        for (Bar bar : currentModel.getBars()) {
            Point3D from = centeredPoints.get(bar.getNodeA());
            Point3D to = centeredPoints.get(bar.getNodeB());
            if (from == null || to == null) {
                continue;
            }

            connectedNodeIds.add(bar.getNodeA());
            connectedNodeIds.add(bar.getNodeB());

            boolean selected = highlightedBarIds.contains(bar.getId());
            PhongMaterial material = selected
                    ? selectedBarMaterial
                    : (colorBars ? materialForType(bar.getType()) : defaultBarMaterial);
            double radius = selected ? currentBarRadius * 1.35 : currentBarRadius;
            Cylinder cylinder = GeometryUtils.createConnectionCylinder(from, to, radius, material);
            if (cylinder != null) {
                cylinder.setOpacity(1.0);
                cylinder.setUserData(new PickTag(PickType.BAR, bar.getId()));
                barsGroup.getChildren().add(cylinder);
            }
        }

        double nodeRadius = Math.max(currentBarRadius * 1.8, currentBarRadius + 0.01);
        for (Node3D node : currentModel.getNodes()) {
            if (!connectedNodeIds.contains(node.getId())) {
                continue;
            }
            Point3D point = centeredPoints.get(node.getId());
            if (point == null) {
                continue;
            }

            PhongMaterial material;
            if (Objects.equals(connectionStartNodeId, node.getId())) {
                material = nodeStartMaterial;
            } else if (Objects.equals(connectionTargetNodeId, node.getId())) {
                material = nodeTargetMaterial;
            } else {
                material = nodeMaterial;
            }

            Sphere sphere = GeometryUtils.createNodeSphere(point, nodeRadius, material);
            sphere.setOpacity(1.0);
            sphere.setUserData(new PickTag(PickType.NODE, node.getId()));
            nodesGroup.getChildren().add(sphere);
        }
    }

    private PhongMaterial materialForType(BarType type) {
        return switch (type) {
            case HORIZONTAL -> horizontalMaterial;
            case RADIAL -> radialMaterial;
            case DIAGONAL -> diagonalMaterial;
            case BASE -> baseMaterial;
        };
    }

    private void softenMaterial(PhongMaterial material) {
        material.setSpecularColor(Color.color(0.18, 0.18, 0.18));
        material.setSpecularPower(7);
    }

    private Point3D computeCentroid(List<Point3D> points) {
        if (points.isEmpty()) {
            return Point3D.ZERO;
        }

        double sx = 0;
        double sy = 0;
        double sz = 0;
        for (Point3D point : points) {
            sx += point.getX();
            sy += point.getY();
            sz += point.getZ();
        }

        return new Point3D(sx / points.size(), sy / points.size(), sz / points.size());
    }

    private void setupMouseControls(SubScene subScene) {
        subScene.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            dragAnchorX = event.getSceneX();
            dragAnchorY = event.getSceneY();
            anchorRotateX = rotateX.getAngle();
            anchorRotateY = rotateY.getAngle();
            anchorPanX = pan.getX();
            anchorPanY = pan.getY();

            if (event.getButton() == MouseButton.PRIMARY) {
                Integer nodeId = findNodeId(event.getPickResult().getIntersectedNode());
                if (nodeId != null) {
                    connectionDragActive = true;
                    connectionStartNodeId = nodeId;
                    connectionTargetNodeId = null;
                    renderModel();
                    event.consume();
                }
            }
        });

        subScene.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            if (connectionDragActive) {
                Integer hoveredNode = findNodeId(event.getPickResult().getIntersectedNode());
                Integer nextTarget = null;
                if (hoveredNode != null && !hoveredNode.equals(connectionStartNodeId)) {
                    nextTarget = hoveredNode;
                }

                if (!Objects.equals(nextTarget, connectionTargetNodeId)) {
                    connectionTargetNodeId = nextTarget;
                    renderModel();
                }
                event.consume();
                return;
            }

            double dx = event.getSceneX() - dragAnchorX;
            double dy = event.getSceneY() - dragAnchorY;

            if (event.isPrimaryButtonDown()) {
                rotateY.setAngle(anchorRotateY + dx * 0.35);
                rotateX.setAngle(anchorRotateX - dy * 0.35);
            } else if (event.isSecondaryButtonDown() || event.isMiddleButtonDown()) {
                pan.setX(anchorPanX + dx);
                pan.setY(anchorPanY + dy);
            }
        });

        subScene.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            if (!connectionDragActive) {
                return;
            }

            Integer start = connectionStartNodeId;
            Integer target = findNodeId(event.getPickResult().getIntersectedNode());
            if (target == null) {
                target = connectionTargetNodeId;
            }

            connectionDragActive = false;
            connectionStartNodeId = null;
            connectionTargetNodeId = null;
            renderModel();

            if (start != null && target != null && !start.equals(target) && onNodeConnect != null) {
                onNodeConnect.accept(new NodeConnectEvent(start, target));
            }
            event.consume();
        });

        subScene.addEventHandler(ScrollEvent.SCROLL, event -> {
            double factor = event.getDeltaY() > 0 ? 0.9 : 1.1;
            double nextZ = camera.getTranslateZ() * factor;
            nextZ = Math.max(MIN_CAMERA_Z, Math.min(MAX_CAMERA_Z, nextZ));
            camera.setTranslateZ(nextZ);
        });

        subScene.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (connectionDragActive || event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 1 || !event.isShiftDown()) {
                return;
            }

            Integer barId = findBarId(event.getPickResult().getIntersectedNode());
            if (barId == null) {
                return;
            }

            if (onBarSelectClick != null) {
                onBarSelectClick.accept(new BarPickEvent(barId, true));
            }
            event.consume();
        });
    }

    private Integer findBarId(Node node) {
        PickTag tag = findPickTag(node);
        if (tag != null && tag.type() == PickType.BAR) {
            return tag.id();
        }
        return null;
    }

    private Integer findNodeId(Node node) {
        PickTag tag = findPickTag(node);
        if (tag != null && tag.type() == PickType.NODE) {
            return tag.id();
        }
        return null;
    }

    private PickTag findPickTag(Node node) {
        Node current = node;
        while (current != null) {
            Object data = current.getUserData();
            if (data instanceof PickTag tag) {
                return tag;
            }
            current = current.getParent();
        }
        return null;
    }

    public Set<Integer> getHighlightedBarIds() {
        return Collections.unmodifiableSet(highlightedBarIds);
    }

    private enum PickType {
        BAR,
        NODE
    }

    private record PickTag(PickType type, int id) {
    }

    public record BarPickEvent(int barId, boolean multiSelectToggle) {
    }

    public record NodeConnectEvent(int fromNodeId, int toNodeId) {
    }
}
