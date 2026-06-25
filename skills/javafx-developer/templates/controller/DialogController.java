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
 * 对话框控制器模板。
 * 配合 dialog.fxml 使用，处理用户输入并返回结果。
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
        // 确定按钮在名称为空时禁用
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

    /** 获取用户输入的名称 */
    public String getName() { return name; }

    /** 获取用户输入的描述 */
    public String getDescription() { return description; }
}
