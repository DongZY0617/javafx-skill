package ${packageName}.controller;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Abstract base controller providing common functionality shared by all
 * application controllers.
 * <p>
 * Provides helpers for stage management, view navigation, and standard
 * dialog display (alerts and confirmations).
 * </p>
 */
public abstract class BaseController implements Initializable {

    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Default no-op implementation. Subclasses may override.
    }

    /**
     * Returns the root node of this controller's view.
     * <p>
     * Subclasses must implement this to expose the root region so that
     * stage and window lookups can be performed.
     * </p>
     *
     * @return the root region of the view
     */
    protected abstract Region getRoot();

    /**
     * Returns the stage associated with this controller, lazily resolving
     * it from the root node's scene window.
     *
     * @return the stage, or {@code null} if the view is not yet attached
     */
    public Stage getStage() {
        if (stage == null) {
            Scene scene = getRoot().getScene();
            if (scene != null && scene.getWindow() instanceof Stage) {
                stage = (Stage) scene.getWindow();
            }
        }
        return stage;
    }

    /**
     * Explicitly sets the stage for this controller.
     *
     * @param stage the stage to associate with this controller
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Loads a view from the given FXML resource path, creates a new stage
     * for it, and returns the loaded controller.
     *
     * @param <T>       the controller type
     * @param fxmlPath  the path to the FXML resource
     * @param title     the title for the new stage
     * @param modality  the modality to apply to the new stage
     * @return the controller of the loaded view
     * @throws IOException if the FXML resource cannot be loaded
     */
    protected <T extends BaseController> T loadView(String fxmlPath, String title, Modality modality)
            throws IOException {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                getClass().getResource(fxmlPath),
                "FXML resource not found: " + fxmlPath));
        Parent root = loader.load();
        T controller = loader.getController();

        Stage dialogStage = new Stage();
        dialogStage.setTitle(title);
        dialogStage.initModality(modality);
        dialogStage.setScene(new Scene(root));
        controller.setStage(dialogStage);

        return controller;
    }

    /**
     * Displays an alert dialog of the given type with the supplied title
     * and message, owned by this controller's stage.
     *
     * @param type    the alert type
     * @param title   the dialog title
     * @param message the message content
     * @return an optional containing the user's response
     */
    protected Optional<ButtonType> showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Stage owner = getStage();
        if (owner != null) {
            alert.initOwner(owner);
        }
        return alert.showAndWait();
    }

    /**
     * Displays a confirmation dialog and returns whether the user
     * confirmed (clicked OK).
     *
     * @param title   the dialog title
     * @param message the message content
     * @return {@code true} if the user confirmed, {@code false} otherwise
     */
    protected boolean showConfirmation(String title, String message) {
        return showAlert(Alert.AlertType.CONFIRMATION, title, message)
                .filter(response -> response == ButtonType.OK)
                .isPresent();
    }
}
