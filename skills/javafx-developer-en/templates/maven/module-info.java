/**
 * Module descriptor for the ${artifactId} application.
 *
 * Declares dependencies on JavaFX modules and opens the controller
 * package to javafx.fxml so the FXML loader can inject controllers.
 */
module ${moduleName} {
    requires javafx.controls;
    requires javafx.fxml;

    opens ${packageName}.controller to javafx.fxml;

    exports ${packageName};
}
