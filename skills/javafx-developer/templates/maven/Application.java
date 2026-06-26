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
 * JavaFX 应用程序入口。
 * <p>
 * 继承 {@link Application}，在 {@link #start(Stage)} 方法中加载主视图 FXML，
 * 构建 {@link Scene} 并绑定 CSS 样式表，最终显示主舞台。
 * </p>
 * <p>
 * 默认加载 {@code /fxml/main-view.fxml} 作为主视图，
 * 应用 {@code /css/light-theme.css} 作为基础样式表。资源文件位于 {@code src/main/resources}。
 * </p>
 */
public class MainApp extends Application {

    /** 主视图 FXML 资源路径。 */
    private static final String FXML_PATH = "/fxml/main-view.fxml";

    /** 基础样式表资源路径。 */
    private static final String CSS_PATH = "/css/light-theme.css";

    /** 默认舞台标题。 */
    private static final String DEFAULT_TITLE = "{{artifactId}}";

    /**
     * 应用程序入口，由 JavaFX 运行时在 JavaFX Application Thread 上调用。
     * <p>
     * 加载主视图 FXML，创建 {@link Scene} 并应用 CSS 样式表，
     * 随后将场景设置到主舞台并显示。
     * </p>
     *
     * @param stage 由 JavaFX 运行时提供的主舞台
     * @throws IOException 若主视图 FXML 资源无法加载
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                MainApp.class.getResource(FXML_PATH),
                "未找到 FXML 资源: " + FXML_PATH));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        // 应用基础样式表；样式表缺失时忽略以保证界面仍可正常显示
        URL cssUrl = MainApp.class.getResource(CSS_PATH);
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setTitle(DEFAULT_TITLE);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * 程序主入口方法。
     * <p>
     * 委托给 {@link Application#launch(String...)} 启动 JavaFX 应用程序。
     * 在支持 JavaFX 启动器的环境中亦可省略此方法。
     * </p>
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        launch(args);
    }
}
