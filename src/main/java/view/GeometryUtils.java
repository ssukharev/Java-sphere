package view;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import model.Node3D;

public final class GeometryUtils {
    private static final double EPS = 1e-9;

    private GeometryUtils() {
    }

    public static Point3D toFxPoint(Node3D node) {
        return new Point3D(node.getX(), -node.getZ(), node.getY());
    }

    public static Cylinder createConnectionCylinder(Point3D from, Point3D to, double radius, Material material) {
        Point3D diff = to.subtract(from);
        double height = diff.magnitude();
        if (height < EPS) {
            return null;
        }

        Cylinder cylinder = new Cylinder(radius, height, 14);
        cylinder.setMaterial(material);

        Point3D midpoint = from.midpoint(to);
        cylinder.setTranslateX(midpoint.getX());
        cylinder.setTranslateY(midpoint.getY());
        cylinder.setTranslateZ(midpoint.getZ());

        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D direction = diff.normalize();
        double dot = clamp(yAxis.dotProduct(direction), -1.0, 1.0);
        double angle = Math.toDegrees(Math.acos(dot));

        Point3D axis = yAxis.crossProduct(direction);
        if (axis.magnitude() < EPS) {
            if (dot < 0) {
                cylinder.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
            }
        } else {
            cylinder.getTransforms().add(new Rotate(angle, axis));
        }

        return cylinder;
    }

    public static Sphere createNodeSphere(Point3D at, double radius, Material material) {
        Sphere sphere = new Sphere(radius);
        sphere.setMaterial(material);
        sphere.setTranslateX(at.getX());
        sphere.setTranslateY(at.getY());
        sphere.setTranslateZ(at.getZ());
        return sphere;
    }

    public static Group createAxes(double length, double radius) {
        Group axes = new Group();

        PhongMaterial xMaterial = new PhongMaterial(Color.web("#d9534f"));
        PhongMaterial yMaterial = new PhongMaterial(Color.web("#5cb85c"));
        PhongMaterial zMaterial = new PhongMaterial(Color.web("#428bca"));

        Point3D origin = new Point3D(0, 0, 0);
        Cylinder xAxis = createConnectionCylinder(origin, new Point3D(length, 0, 0), radius, xMaterial);
        Cylinder yAxis = createConnectionCylinder(origin, new Point3D(0, 0, length), radius, yMaterial);
        Cylinder zAxis = createConnectionCylinder(origin, new Point3D(0, -length, 0), radius, zMaterial);

        if (xAxis != null) {
            axes.getChildren().add(xAxis);
        }
        if (yAxis != null) {
            axes.getChildren().add(yAxis);
        }
        if (zAxis != null) {
            axes.getChildren().add(zAxis);
        }

        return axes;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
