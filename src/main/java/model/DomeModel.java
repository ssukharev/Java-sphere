package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DomeModel {
    private final DomeParameters parameters;
    private final List<Node3D> nodes;
    private final List<Bar> bars;
    private final List<Face> faces;
    private final Map<Integer, Node3D> nodeById;

    public DomeModel(DomeParameters parameters, List<Node3D> nodes, List<Bar> bars, List<Face> faces) {
        this.parameters = parameters;
        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
        this.bars = Collections.unmodifiableList(new ArrayList<>(bars));
        this.faces = Collections.unmodifiableList(new ArrayList<>(faces));
        this.nodeById = this.nodes.stream().collect(Collectors.toUnmodifiableMap(Node3D::getId, n -> n));
    }

    public DomeParameters getParameters() {
        return parameters;
    }

    public List<Node3D> getNodes() {
        return nodes;
    }

    public List<Bar> getBars() {
        return bars;
    }

    public List<Face> getFaces() {
        return faces;
    }

    public Node3D getNodeById(int id) {
        return nodeById.get(id);
    }
}
