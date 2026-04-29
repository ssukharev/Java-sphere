package model;

public class Face {
    private final int id;
    private final int a;
    private final int b;
    private final int c;

    public Face(int id, int a, int b, int c) {
        this.id = id;
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public int getId() {
        return id;
    }

    public int getA() {
        return a;
    }

    public int getB() {
        return b;
    }

    public int getC() {
        return c;
    }
}
