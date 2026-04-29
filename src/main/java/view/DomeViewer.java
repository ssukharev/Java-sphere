package view;

import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DomeViewer {
    private static final double MIN_CAMERA_Z = -10000;
    private static final double MAX_CAMERA_Z = -40;
    private static final double TARGET_MODEL_HALF_SIZE = 260;

    private final StackPane root = new StackPane();
    private final Group world = new Group();
    private final Group domeGroup = new Group();
    private final Group barsGroup = new Group();
    private final Group nodesGroup = new Group();
    private final Group axesGroup = GeometryUtils.createAxes(250, 0.6);

    private final Rotate rotateX = new Rotate(-25, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(-35, Rotate.Y_AXIS);
    private final Translate pan = new Translate();
    private final Scale scale = new Scale(1, 1, 1);

    private final PerspectiveCamera camera = new PerspectiveCamera(true);

    private final PhongMaterial defaultBarMaterial = new PhongMaterial(Color.web("#7f8c8d"));
    private final PhongMaterial nodeMaterial = new PhongMaterial(Color.web("#111111"));
    private final PhongMaterial horizontalMaterial = new PhongMaterial(Color.web("#f39c12"));
    private final PhongMaterial radialMaterial = new PhongMaterial(Color.web("#27ae60"));
    private final PhongMaterial diagonalMaterial = new PhongMaterial(Color.web("#2980b9"));
    private final PhongMaterial baseMaterial = new PhongMaterial(Color.web("#c0392b"));

    private DomeModel currentModel;
    private double currentBarRadius = 0.12;
    private boolean colorBars = true;

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
        subScene.setFill(Color.web("#f0f3f5"));

        camera.setNearClip(0.1);
        camera.setFarClip(50000);
        camera.setTranslateZ(-900);
        subScene.setCamera(camera);

        AmbientLight ambientLight = new AmbientLight(Color.color(0.6, 0.6, 0.6));
        PointLight pointLight = new PointLight(Color.WHITE);
        pointLight.setTranslateX(-500);
        pointLight.setTranslateY(-500);
        pointLight.setTranslateZ(-500);
        world.getChildren().addAll(ambientLight, pointLight);

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

    public void resetCamera() {
        rotateX.setAngle(-25);
        rotateY.setAngle(-35);
        pan.setX(0);
        pan.setY(0);
        camera.setTranslateZ(-900);
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

        for (Bar bar : currentModel.getBars()) {
            Point3D from = centeredPoints.get(bar.getNodeA());
            Point3D to = centeredPoints.get(bar.getNodeB());
            if (from == null || to == null) {
                continue;
            }

            PhongMaterial material = colorBars ? materialForType(bar.getType()) : defaultBarMaterial;
            Cylinder cylinder = GeometryUtils.createConnectionCylinder(from, to, currentBarRadius, material);
            if (cylinder != null) {
                barsGroup.getChildren().add(cylinder);
            }
        }

        double nodeRadius = Math.max(currentBarRadius * 1.8, currentBarRadius + 0.01);
        for (Node3D node : currentModel.getNodes()) {
            Point3D point = centeredPoints.get(node.getId());
            if (point == null) {
                continue;
            }
            Sphere sphere = GeometryUtils.createNodeSphere(point, nodeRadius, nodeMaterial);
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
        });

        subScene.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
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

        subScene.addEventHandler(ScrollEvent.SCROLL, event -> {
            double factor = event.getDeltaY() > 0 ? 0.9 : 1.1;
            double nextZ = camera.getTranslateZ() * factor;
            nextZ = Math.max(MIN_CAMERA_Z, Math.min(MAX_CAMERA_Z, nextZ));
            camera.setTranslateZ(nextZ);
        });
    }
}
