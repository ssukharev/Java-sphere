package export;

import model.DomeModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class StlExporter {
    public void export(Path file, DomeModel model) throws IOException {
        String placeholder = """
                solid dome
                endsolid dome
                """;

        Files.writeString(file, placeholder, StandardCharsets.UTF_8);
        throw new UnsupportedOperationException(
                "Экспорт STL пока реализован как заглушка. TODO: сгенерировать цилиндрический mesh по каждому стержню."
        );
    }
}
