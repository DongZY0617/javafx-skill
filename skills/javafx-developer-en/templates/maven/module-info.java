/**
 * Module descriptor for the {{artifactId}} application.
 *
 * Declares dependencies on JavaFX modules and opens the controller
 * package to javafx.fxml so the FXML loader can inject controllers.
 * Also opens the model package to javafx.base to support reflective
 * access by PropertyValueFactory.
 */
module {{moduleName}} {
    requires javafx.controls;
    requires javafx.fxml;

    opens {{packageName}}.controller to javafx.fxml;
    opens {{packageName}}.model to javafx.base;

    exports {{packageName}};
}
