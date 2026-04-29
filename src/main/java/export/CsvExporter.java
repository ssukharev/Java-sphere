package export;

import model.Bar;
import model.DomeModel;
import model.Node3D;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class CsvExporter {
    public void export(Path file, DomeModel model) throws IOException {
        StringBuilder builder = new StringBuilder();

        builder.append("NodeId,X,Y,Z").append(System.lineSeparator());
        for (Node3D node : model.getNodes()) {
            builder.append(node.getId()).append(',')
                    .append(format(node.getX())).append(',')
                    .append(format(node.getY())).append(',')
                    .append(format(node.getZ()))
                    .append(System.lineSeparator());
        }

        builder.append(System.lineSeparator());
        builder.append("BarId,NodeA,NodeB,Length,Type").append(System.lineSeparator());
        for (Bar bar : model.getBars()) {
            builder.append(bar.getId()).append(',')
                    .append(bar.getNodeA()).append(',')
                    .append(bar.getNodeB()).append(',')
                    .append(format(bar.getLength())).append(',')
                    .append(bar.getType().getCode())
                    .append(System.lineSeparator());
        }

        Files.writeString(file, builder.toString(), StandardCharsets.UTF_8);
    }

    private String format(double value) {
        return String.format(Locale.US, "%.6f", value);
    }
}
