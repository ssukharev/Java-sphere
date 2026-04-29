package model;

public class Bar {
    private final int id;
    private final int nodeA;
    private final int nodeB;
    private final double length;
    private final BarType type;
    private final double startX;
    private final double startY;
    private final double startZ;
    private final double endX;
    private final double endY;
    private final double endZ;

    public Bar(
            int id,
            int nodeA,
            int nodeB,
            double length,
            BarType type,
            double startX,
            double startY,
            double startZ,
            double endX,
            double endY,
            double endZ
    ) {
        this.id = id;
        this.nodeA = nodeA;
        this.nodeB = nodeB;
        this.length = length;
        this.type = type;
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.endX = endX;
        this.endY = endY;
        this.endZ = endZ;
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
        return type;
    }

    public double getStartX() {
        return startX;
    }

    public double getStartY() {
        return startY;
    }

    public double getStartZ() {
        return startZ;
    }

    public double getEndX() {
        return endX;
    }

    public double getEndY() {
        return endY;
    }

    public double getEndZ() {
        return endZ;
    }
}
