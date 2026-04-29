package model;

public enum BarType {
    HORIZONTAL("horizontal", "Горизонтальный"),
    RADIAL("radial", "Радиальный"),
    DIAGONAL("diagonal", "Диагональный"),
    BASE("base", "Основание");

    private final String code;
    private final String displayName;

    BarType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }
}
