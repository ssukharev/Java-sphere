package export;

import model.Bar;
import model.BarType;
import model.DomeModel;
import model.Face;
import model.Node3D;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ObjExporter {
    public void export(Path file, DomeModel model) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("# Dome Designer OBJ export").append(System.lineSeparator());
        builder.append("# Each bar is exported as an individual object (o bar_...)").append(System.lineSeparator());
        builder.append(System.lineSeparator());

        List<Node3D> sortedNodes = new ArrayList<>(model.getNodes());
        sortedNodes.sort((a, b) -> Integer.compare(a.getId(), b.getId()));

        builder.append("o nodes_reference").append(System.lineSeparator());
        builder.append("g nodes_reference").append(System.lineSeparator());

        Map<Integer, Integer> vertexIndexByNodeId = new HashMap<>();
        int vertexIndex = 1;
        for (Node3D node : sortedNodes) {
            builder.append("v ")
                    .append(format(node.getX())).append(' ')
                    .append(format(node.getY())).append(' ')
                    .append(format(node.getZ()))
                    .append(System.lineSeparator());
            vertexIndexByNodeId.put(node.getId(), vertexIndex++);
        }
        builder.append(System.lineSeparator());

        if (!model.getFaces().isEmpty()) {
            builder.append("o dome_shell").append(System.lineSeparator());
            builder.append("g dome_shell").append(System.lineSeparator());
            for (Face face : model.getFaces()) {
                Integer a = vertexIndexByNodeId.get(face.getA());
                Integer b = vertexIndexByNodeId.get(face.getB());
                Integer c = vertexIndexByNodeId.get(face.getC());
                if (a != null && b != null && c != null) {
                    builder.append("f ").append(a).append(' ').append(b).append(' ').append(c).append(System.lineSeparator());
                }
            }
            builder.append(System.lineSeparator());
        }

        List<Bar> sortedBars = new ArrayList<>(model.getBars());
        sortedBars.sort((a, b) -> Integer.compare(a.getId(), b.getId()));

        for (Bar bar : sortedBars) {
            String objectName = "bar_" + String.format(Locale.US, "%04d", bar.getId()) + "_" + bar.getType().getCode();
            builder.append("o ").append(objectName).append(System.lineSeparator());
            builder.append("g ").append(objectName).append(System.lineSeparator());

            int v1 = vertexIndex++;
            builder.append("v ")
                    .append(format(bar.getStartX())).append(' ')
                    .append(format(bar.getStartY())).append(' ')
                    .append(format(bar.getStartZ()))
                    .append(System.lineSeparator());

            int v2 = vertexIndex++;
            builder.append("v ")
                    .append(format(bar.getEndX())).append(' ')
                    .append(format(bar.getEndY())).append(' ')
                    .append(format(bar.getEndZ()))
                    .append(System.lineSeparator());

            builder.append("l ").append(v1).append(' ').append(v2).append(System.lineSeparator());
            builder.append(System.lineSeparator());
        }

        Map<BarType, List<Integer>> idsByType = new HashMap<>();
        for (BarType type : BarType.values()) {
            idsByType.put(type, new ArrayList<>());
        }
        for (Bar bar : sortedBars) {
            idsByType.get(bar.getType()).add(bar.getId());
        }

        builder.append("# Type summary").append(System.lineSeparator());
        for (BarType type : BarType.values()) {
            List<Integer> ids = idsByType.get(type);
            builder.append("# ").append(type.getCode()).append(": ").append(ids.size()).append(" bars");
            if (!ids.isEmpty()) {
                builder.append(" (");
                for (int i = 0; i < ids.size(); i++) {
                    if (i > 0) {
                        builder.append(',');
                    }
                    builder.append(ids.get(i));
                }
                builder.append(')');
            }
            builder.append(System.lineSeparator());
        }

        Files.writeString(file, builder.toString(), StandardCharsets.UTF_8);
    }

    private String format(double value) {
        return String.format(Locale.US, "%.6f", value);
    }
}
