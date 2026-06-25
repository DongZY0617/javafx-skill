/**
 * {{artifactId}} 应用的模块描述符。
 *
 * 声明对 JavaFX 模块的依赖，并向 javafx.fxml 开放 controller 包，
 * 以便 FXML 加载器能够注入控制器。
 * 同时向 javafx.base 开放 model 包，以支持 PropertyValueFactory 的反射访问。
 */
module {{moduleName}} {
    requires javafx.controls;
    requires javafx.fxml;

    opens {{packageName}}.controller to javafx.fxml;
    opens {{packageName}}.model to javafx.base;

    exports {{packageName}};
}
