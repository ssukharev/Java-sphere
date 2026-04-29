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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ObjExporter {
    public void export(Path file, DomeModel model) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("# Dome Designer OBJ export").append(System.lineSeparator());
        builder.append("o dome").append(System.lineSeparator());

        List<Node3D> sortedNodes = new ArrayList<>(model.getNodes());
        sortedNodes.sort((a, b) -> Integer.compare(a.getId(), b.getId()));

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
        Map<BarType, List<Bar>> barsByType = new EnumMap<>(BarType.class);
        for (BarType type : BarType.values()) {
            barsByType.put(type, new ArrayList<>());
        }
        for (Bar bar : model.getBars()) {
            barsByType.get(bar.getType()).add(bar);
        }

        for (BarType type : BarType.values()) {
            builder.append("g bars_").append(type.getCode()).append(System.lineSeparator());
            for (Bar bar : barsByType.get(type)) {
                Integer a = vertexIndexByNodeId.get(bar.getNodeA());
                Integer b = vertexIndexByNodeId.get(bar.getNodeB());
                if (a != null && b != null) {
                    builder.append("l ").append(a).append(' ').append(b).append(System.lineSeparator());
                }
            }
            builder.append(System.lineSeparator());
        }

        if (!model.getFaces().isEmpty()) {
            builder.append("g faces").append(System.lineSeparator());
            for (Face face : model.getFaces()) {
                Integer a = vertexIndexByNodeId.get(face.getA());
                Integer b = vertexIndexByNodeId.get(face.getB());
                Integer c = vertexIndexByNodeId.get(face.getC());
                if (a != null && b != null && c != null) {
                    builder.append("f ").append(a).append(' ').append(b).append(' ').append(c).append(System.lineSeparator());
                }
            }
        }

        Files.writeString(file, builder.toString(), StandardCharsets.UTF_8);
    }

    private String format(double value) {
        return String.format(Locale.US, "%.6f", value);
    }
}
