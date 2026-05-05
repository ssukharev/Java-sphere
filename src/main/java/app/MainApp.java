package app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        MainController controller = new MainController(primaryStage);
        Scene scene = new Scene(controller.createView(), 1500, 920, true);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/styles/app.css"),
                "Не найден styles/app.css"
        ).toExternalForm());

        primaryStage.setTitle("Проектирование металлической полусферы / геодезического купола");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
