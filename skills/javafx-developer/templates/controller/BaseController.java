package {{packageName}}.controller;

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
 * 控制器抽象基类，为所有应用控制器提供通用功能。
 * <p>
 * 提供舞台（Stage）管理、视图导航以及标准对话框（提示与确认）显示等辅助方法。
 * </p>
 */
public abstract class BaseController implements Initializable {

    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 默认空实现，子类可按需重写。
    }

    /**
     * 返回该控制器视图的根节点。
     * <p>
     * 子类必须实现此方法以暴露根区域，从而支持舞台与窗口查找。
     * </p>
     *
     * @return 视图的根区域
     */
    protected abstract Region getRoot();

    /**
     * 返回与此控制器关联的舞台，按需从根节点的场景窗口中延迟解析。
     *
     * @return 关联的舞台；若视图尚未挂载或根节点为空则返回 {@code null}
     */
    public Stage getStage() {
        if (stage == null) {
            Region root = getRoot();
            // 根节点尚未就绪时安全返回 null，避免空指针异常
            if (root == null) {
                return null;
            }
            Scene scene = root.getScene();
            if (scene != null && scene.getWindow() instanceof Stage) {
                stage = (Stage) scene.getWindow();
            }
        }
        return stage;
    }

    /**
     * 显式设置与此控制器关联的舞台。
     *
     * @param stage 要关联的舞台
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * 从给定的 FXML 资源路径加载视图，为其创建新舞台，并返回加载得到的控制器。
     * <p>
     * 当存在属主舞台时会设置属主关系，保证对话框层级与焦点归属正确。
     * </p>
     *
     * @param <T>       控制器类型
     * @param fxmlPath  FXML 资源路径
     * @param title     新舞台的标题
     * @param modality  新舞台采用的模态类型
     * @return 加载视图的控制器
     * @throws IOException 若 FXML 资源无法加载
     */
    protected <T extends BaseController> T loadView(String fxmlPath, String title, Modality modality)
            throws IOException {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                getClass().getResource(fxmlPath),
                "未找到 FXML 资源: " + fxmlPath));
        Parent root = loader.load();
        T controller = loader.getController();

        Stage dialogStage = new Stage();
        dialogStage.setTitle(title);
        dialogStage.initModality(modality);
        // 当存在属主舞台时设置属主，确保对话框层级与焦点归属正确
        Stage owner = getStage();
        if (owner != null) {
            dialogStage.initOwner(owner);
        }
        dialogStage.setScene(new Scene(root));
        controller.setStage(dialogStage);

        return controller;
    }

    /**
     * 显示指定类型的提示对话框，附带标题与消息，并以当前控制器的舞台作为属主。
     *
     * @param type    提示框类型
     * @param title   对话框标题
     * @param message 消息内容
     * @return 包含用户响应的 Optional
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
     * 显示确认对话框，并返回用户是否确认（点击了"确定"）。
     *
     * @param title   对话框标题
     * @param message 消息内容
     * @return 若用户确认返回 {@code true}，否则返回 {@code false}
     */
    protected boolean showConfirmation(String title, String message) {
        return showAlert(Alert.AlertType.CONFIRMATION, title, message)
                .filter(response -> response == ButtonType.OK)
                .isPresent();
    }
}
