package {{packageName}};

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testfx.framework.junit5.ApplicationTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Main Window Integration Test Template.
 * Verifies FXML loading, controller injection, CSS loading, and basic scene structure.
 * Uses TestFX ApplicationTest for JavaFX lifecycle management.
 *
 * Prerequisites:
 * - testfx-junit5 dependency in pom.xml
 * - FXML file at /fxml/main-view.fxml
 * - CSS file at /css/light-theme.css
 *
 * @req FR-001
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MainWindowTest extends ApplicationTest {

    private Parent rootNode;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
        rootNode = loader.load();
        assertNotNull(loader.getController(), "Controller should be injected by FXMLLoader");
        Scene scene = new Scene(rootNode);
        scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    /**
     * @req FR-001
     */
    @Test
    void fxmlLoadsSuccessfully_FR_001() {
        assertNotNull(rootNode, "FXML root node should be loaded");
    }

    /**
     * @req NFR-UI-001
     */
    @Test
    void cssStylesheetApplied_NFR_UI_001() {
        assertFalse(((Scene) rootNode.getScene()).getStylesheets().isEmpty(),
                "At least one CSS stylesheet should be applied");
    }

    /**
     * @req FR-001
     */
    @Test
    void menuBarPresent_FR_001() {
        // Verify the menu bar exists with expected menus
        // Adjust lookup queries based on your FXML structure
        // Example: verifyThat("#menuBar", NodeMatchers.isVisible());
        assertNotNull(lookup(".menu-bar").query(), "MenuBar should be present in main view");
    }

    /**
     * @req FR-001
     */
    @Test
    void contentAreaPresent_FR_001() {
        // Adjust the selector based on your FXML fx:id or structure
        // Example: verifyThat("#contentArea", NodeMatchers.isVisible());
        assertNotNull(lookup("#contentArea").query(), "Content area should be present");
    }
}
