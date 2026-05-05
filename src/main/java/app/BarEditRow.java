package app;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import model.Bar;
import model.BarType;
import model.Node3D;

import java.util.Map;

public class BarEditRow {
    private static final double EPS = 1e-9;

    private final int id;
    private final int nodeA;
    private final int nodeB;
    private final double length;
    private final double startX;
    private final double startY;
    private final double startZ;
    private final double endX;
    private final double endY;
    private final double endZ;

    private final ObjectProperty<BarType> type;
    private final BooleanProperty active;

    public BarEditRow(Bar bar) {
        this.id = bar.getId();
        this.nodeA = bar.getNodeA();
        this.nodeB = bar.getNodeB();
        this.length = bar.getLength();
        this.startX = bar.getStartX();
        this.startY = bar.getStartY();
        this.startZ = bar.getStartZ();
        this.endX = bar.getEndX();
        this.endY = bar.getEndY();
        this.endZ = bar.getEndZ();
        this.type = new SimpleObjectProperty<>(bar.getType());
        this.active = new SimpleBooleanProperty(true);
    }

    public static BarEditRow create(
            int id,
            int nodeA,
            int nodeB,
            BarType type,
            Map<Integer, Node3D> nodes
    ) {
        if (nodeA == nodeB) {
            throw new IllegalArgumentException("Нельзя соединить узел сам с собой.");
        }
        Node3D a = nodes.get(nodeA);
        Node3D b = nodes.get(nodeB);
        if (a == null || b == null) {
            throw new IllegalArgumentException("Один из узлов не существует.");
        }

        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        double dz = b.getZ() - a.getZ();
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < EPS) {
            throw new IllegalArgumentException("Длина стержня близка к нулю.");
        }

        Bar bar = new Bar(
                id,
                nodeA,
                nodeB,
                length,
                type,
                a.getX(),
                a.getY(),
                a.getZ(),
                b.getX(),
                b.getY(),
                b.getZ()
        );
        return new BarEditRow(bar);
    }

    public Bar toBar() {
        return new Bar(
                id,
                nodeA,
                nodeB,
                length,
                getType(),
                startX,
                startY,
                startZ,
                endX,
                endY,
                endZ
        );
    }

    public int getId() {
        return id;
    }

    public int getNodeA() {
        return nodeA;
    }

    public int getNodeB() {
        return nodeB;
    }

    public double getLength() {
        return length;
    }

    public BarType getType() {
        return type.get();
    }

    public void setType(BarType barType) {
        this.type.set(barType);
    }

    public ObjectProperty<BarType> typeProperty() {
        return type;
    }

    public boolean isActive() {
        return active.get();
    }

    public void setActive(boolean value) {
        this.active.set(value);
    }

    public BooleanProperty activeProperty() {
        return active;
    }
}
