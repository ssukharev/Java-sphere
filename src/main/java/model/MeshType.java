package model;

public enum MeshType {
    RING_TRIANGULAR("Кольцевая треугольная сетка"),
    GEODESIC_TRIANGULAR("Геодезическая треугольная сетка");

    private final String displayName;

    MeshType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
