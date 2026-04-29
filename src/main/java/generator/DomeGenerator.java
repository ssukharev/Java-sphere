package generator;

import model.Bar;
import model.BarType;
import model.DomeModel;
import model.DomeParameters;
import model.Face;
import model.MeshType;
import model.Node3D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DomeGenerator {
    private static final double EPS = 1e-9;

    public DomeModel generate(DomeParameters parameters) {
        List<Node3D> nodes = new ArrayList<>();
        List<Bar> bars = new ArrayList<>();
        List<Face> faces = new ArrayList<>();
        Map<Integer, Node3D> nodeById = new HashMap<>();

        List<List<Integer>> rings = new ArrayList<>();

        int nextNodeId = 1;
        Node3D apex = new Node3D(nextNodeId++, 0, 0, parameters.getRadius());
        nodes.add(apex);
        nodeById.put(apex.getId(), apex);
        rings.add(List.of(apex.getId()));

        double radius = parameters.getRadius();
        double capHeight = Math.min(radius, 2.0 * radius * parameters.getDomeHeightFactor());
        double baseZ = radius - capHeight;
        double clamped = Math.max(-1.0, Math.min(1.0, baseZ / radius));
        double maxPhi = Math.acos(clamped);

        for (int ringIndex = 1; ringIndex <= parameters.getRings(); ringIndex++) {
            double t = ringIndex / (double) parameters.getRings();
            double phi = maxPhi * t;
            double z = radius * Math.cos(phi);
            double ringRadius = radius * Math.sin(phi);

            double shift = 0;
            if (parameters.getMeshType() == MeshType.GEODESIC_TRIANGULAR && ringIndex % 2 == 1) {
                shift = Math.PI / parameters.getSegments();
            }

            List<Integer> ring = new ArrayList<>(parameters.getSegments());
            for (int segment = 0; segment < parameters.getSegments(); segment++) {
                double theta = (2.0 * Math.PI * segment / parameters.getSegments()) + shift;
                double x = ringRadius * Math.cos(theta);
                double y = ringRadius * Math.sin(theta);

                Node3D node = new Node3D(nextNodeId++, x, y, z);
                nodes.add(node);
                nodeById.put(node.getId(), node);
                ring.add(node.getId());
            }
            rings.add(ring);
        }

        Set<Long> barKeys = new HashSet<>();
        int nextBarId = 1;

        List<Integer> firstRing = rings.get(1);
        for (int segment = 0; segment < parameters.getSegments(); segment++) {
            nextBarId = addBar(nextBarId, apex.getId(), firstRing.get(segment), BarType.RADIAL, nodeById, barKeys, bars);
        }

        for (int ringIndex = 1; ringIndex < rings.size(); ringIndex++) {
            List<Integer> ring = rings.get(ringIndex);
            BarType type = (ringIndex == rings.size() - 1) ? BarType.BASE : BarType.HORIZONTAL;
            for (int segment = 0; segment < parameters.getSegments(); segment++) {
                int a = ring.get(segment);
                int b = ring.get((segment + 1) % parameters.getSegments());
                nextBarId = addBar(nextBarId, a, b, type, nodeById, barKeys, bars);
            }
        }

        for (int ringIndex = 2; ringIndex < rings.size(); ringIndex++) {
            List<Integer> prevRing = rings.get(ringIndex - 1);
            List<Integer> currentRing = rings.get(ringIndex);

            boolean diagonalForward = parameters.getMeshType() == MeshType.RING_TRIANGULAR || ringIndex % 2 == 0;

            for (int segment = 0; segment < parameters.getSegments(); segment++) {
                int prev = prevRing.get(segment);
                int curr = currentRing.get(segment);
                nextBarId = addBar(nextBarId, prev, curr, BarType.RADIAL, nodeById, barKeys, bars);

                int diagonalTargetIndex = diagonalForward
                        ? (segment + 1) % parameters.getSegments()
                        : (segment - 1 + parameters.getSegments()) % parameters.getSegments();
                int diagonalTarget = currentRing.get(diagonalTargetIndex);

                nextBarId = addBar(nextBarId, prev, diagonalTarget, BarType.DIAGONAL, nodeById, barKeys, bars);
            }
        }

        int nextFaceId = 1;
        for (int segment = 0; segment < parameters.getSegments(); segment++) {
            int a = apex.getId();
            int b = firstRing.get(segment);
            int c = firstRing.get((segment + 1) % parameters.getSegments());
            faces.add(new Face(nextFaceId++, a, b, c));
        }

        for (int ringIndex = 2; ringIndex < rings.size(); ringIndex++) {
            List<Integer> prevRing = rings.get(ringIndex - 1);
            List<Integer> currentRing = rings.get(ringIndex);
            boolean forward = parameters.getMeshType() == MeshType.RING_TRIANGULAR || ringIndex % 2 == 0;

            for (int segment = 0; segment < parameters.getSegments(); segment++) {
                int next = (segment + 1) % parameters.getSegments();

                int p0 = prevRing.get(segment);
                int p1 = prevRing.get(next);
                int c0 = currentRing.get(segment);
                int c1 = currentRing.get(next);

                if (forward) {
                    faces.add(new Face(nextFaceId++, p0, c0, c1));
                    faces.add(new Face(nextFaceId++, p0, c1, p1));
                } else {
                    faces.add(new Face(nextFaceId++, p0, c0, p1));
                    faces.add(new Face(nextFaceId++, p1, c0, c1));
                }
            }
        }

        return new DomeModel(parameters, nodes, bars, faces);
    }

    private int addBar(
            int nextBarId,
            int nodeA,
            int nodeB,
            BarType type,
            Map<Integer, Node3D> nodeById,
            Set<Long> barKeys,
            List<Bar> bars
    ) {
        if (nodeA == nodeB) {
            return nextBarId;
        }

        long key = key(nodeA, nodeB);
        if (barKeys.contains(key)) {
            return nextBarId;
        }

        Node3D a = nodeById.get(nodeA);
        Node3D b = nodeById.get(nodeB);
        if (a == null || b == null) {
            return nextBarId;
        }

        double length = length(a, b);
        if (length < EPS) {
            return nextBarId;
        }

        barKeys.add(key);
        bars.add(new Bar(
                nextBarId,
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
        ));
        return nextBarId + 1;
    }

    private long key(int a, int b) {
        int min = Math.min(a, b);
        int max = Math.max(a, b);
        return (((long) min) << 32) | (max & 0xffffffffL);
    }

    private double length(Node3D a, Node3D b) {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        double dz = b.getZ() - a.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
