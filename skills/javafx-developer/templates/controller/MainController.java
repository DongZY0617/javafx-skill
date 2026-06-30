package {{packageName}}.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Main controller for the primary application window.
 * <p>
 * Wires up the menu bar actions and manages the central content area.
 * </p>
 */
public class MainController extends BaseController {

    @FXML
    private BorderPane root;

    @FXML
    private MenuItem newMenuItem;

    @FXML
    private MenuItem openMenuItem;

    @FXML
    private MenuItem exitMenuItem;

    @FXML
    private MenuItem aboutMenuItem;

    @FXML
    private VBox contentArea;

    @FXML
    private HBox statusBar;

    @FXML
    private Label statusLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Perform any initialization logic here.
        // This method is called automatically after the FXML has been loaded.
    }

    @Override
    protected BorderPane getRoot() {
        return root;
    }

    // NOTE: The menu handlers below are package-private (not private) so that
    // ControllerTest can invoke them directly with a mocked ActionEvent.
    // FXML still injects and dispatches events to package-private @FXML methods.

    /**
     * Handles the "New" menu action.
     *
     * @param event the action event
     */
    @FXML
    void handleNew(ActionEvent event) {
        // TODO: implement new document logic
    }

    /**
     * Handles the "Open..." menu action.
     *
     * @param event the action event
     */
    @FXML
    void handleOpen(ActionEvent event) {
        // TODO: implement open logic
    }

    /**
     * Handles the "Exit" menu action.
     *
     * @param event the action event
     */
    @FXML
    void handleExit(ActionEvent event) {
        Platform.exit();
    }

    /**
     * Handles the "About" menu action.
     *
     * @param event the action event
     */
    @FXML
    void handleAbout(ActionEvent event) {
        // TODO: implement about dialog logic
    }
}
