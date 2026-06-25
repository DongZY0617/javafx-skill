package {{packageName}}.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

/**
 * Dialog controller template.
 * Used together with dialog.fxml to handle user input and return results.
 */
public class DialogController extends BaseController {

    @FXML private GridPane root;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionArea;
    @FXML private Button okButton;
    @FXML private Button cancelButton;

    private String name = "";
    private String description = "";

    @Override
    protected GridPane getRoot() {
        return root;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Disable the OK button when the name is empty
        okButton.disableProperty().bind(
            nameField.textProperty().isEmpty()
        );
    }

    @FXML
    private void handleOk() {
        name = nameField.getText();
        description = descriptionArea.getText();
        closeDialog();
    }

    @FXML
    private void handleCancel() {
        name = "";
        description = "";
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = getStage();
        if (stage != null) {
            stage.close();
        }
    }

    /** Returns the name entered by the user */
    public String getName() { return name; }

    /** Returns the description entered by the user */
    public String getDescription() { return description; }
}
