package {{packageName}};

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX application entry point.
 * <p>
 * Extends {@link Application} and, within {@link #start(Stage)}, loads the
 * main view FXML, builds a {@link Scene} with an attached CSS stylesheet,
 * and finally shows the primary stage.
 * </p>
 * <p>
 * By default loads {@code /fxml/main-view.fxml} as the main view and applies
 * {@code /css/light-theme.css} as the base stylesheet. Resource files reside
 * under {@code src/main/resources}.
 * </p>
 */
public class MainApp extends Application {

    /** Path to the main view FXML resource. */
    private static final String FXML_PATH = "/fxml/main-view.fxml";

    /** Path to the base CSS stylesheet. */
    private static final String CSS_PATH = "/css/light-theme.css";

    /** Default stage title. */
    private static final String DEFAULT_TITLE = "{{artifactId}}";

    /**
     * Application entry point, invoked by the JavaFX runtime on the
     * JavaFX Application Thread.
     * <p>
     * Loads the main view FXML, creates a {@link Scene} and applies the CSS
     * stylesheet, then sets the scene on the primary stage and shows it.
     * </p>
     *
     * @param stage the primary stage provided by the JavaFX runtime
     * @throws IOException if the main view FXML resource cannot be loaded
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                MainApp.class.getResource(FXML_PATH),
                "FXML resource not found: " + FXML_PATH));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        // Apply the base stylesheet; ignore when missing so the UI can still render
        URL cssUrl = MainApp.class.getResource(CSS_PATH);
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setTitle(DEFAULT_TITLE);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Main entry point method.
     * <p>
     * Delegates to {@link Application#launch(String...)} to start the JavaFX
     * application. This method may be omitted in environments that support
     * the JavaFX launcher.
     * </p>
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
