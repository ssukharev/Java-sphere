package model;

import java.util.ArrayList;
import java.util.List;

public class DomeParameters {
    private final double radius;
    private final int rings;
    private final int segments;
    private final double tubeDiameter;
    private final double visualThickness;
    private final double domeHeightFactor;
    private final MeshType meshType;

    public DomeParameters(
            double radius,
            int rings,
            int segments,
            double tubeDiameter,
            double visualThickness,
            double domeHeightFactor,
            MeshType meshType
    ) {
        this.radius = radius;
        this.rings = rings;
        this.segments = segments;
        this.tubeDiameter = tubeDiameter;
        this.visualThickness = visualThickness;
        this.domeHeightFactor = domeHeightFactor;
        this.meshType = meshType;
    }

    public double getRadius() {
        return radius;
    }

    public int getRings() {
        return rings;
    }

    public int getSegments() {
        return segments;
    }

    public double getTubeDiameter() {
        return tubeDiameter;
    }

    public double getVisualThickness() {
        return visualThickness;
    }

    public double getDomeHeightFactor() {
        return domeHeightFactor;
    }

    public MeshType getMeshType() {
        return meshType;
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (radius <= 0) {
            errors.add("Радиус должен быть больше 0.");
        }
        if (rings < 2) {
            errors.add("Количество колец должно быть не меньше 2.");
        }
        if (segments < 6) {
            errors.add("Количество сегментов должно быть не меньше 6.");
        }
        if (tubeDiameter <= 0) {
            errors.add("Диаметр трубы должен быть больше 0.");
        }
        if (visualThickness <= 0) {
            errors.add("Толщина визуального стержня должна быть больше 0.");
        }
        if (domeHeightFactor <= 0 || domeHeightFactor > 1) {
            errors.add("Высота купола должна быть в диапазоне (0; 1].");
        }
        if (meshType == null) {
            errors.add("Выберите тип сетки.");
        }

        return errors;
    }
}
