package {{packageName}};

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Main View Integration Test Template.
 * <p>
 * Uses TestFX to verify that {@code main-view.fxml} loads correctly and that the
 * primary UI structure (menu bar, content area, status bar) is present and wired.
 *
 * <p><b>Why this verifies the application shell rather than a CRUD table:</b> the
 * bundled {@code main-view.fxml} is an application shell (a menu bar plus a content
 * area and a status bar). It does <em>not</em> contain a TableView or add/delete/
 * search controls. An earlier version of this template referenced {@code #tableView},
 * {@code #addButton}, {@code #deleteButton} and {@code #searchField}, none of which
 * exist in the FXML, so the test could neither compile nor run. The assertions below
 * have been realigned with the controls that actually exist in {@code main-view.fxml}
 * (menu items, {@code #contentArea}, {@code #statusBar}, {@code #statusLabel}).
 *
 * <p><b>Adapting this template for a real CRUD view:</b> once your project introduces
 * a CRUD FXML (a TableView plus add/delete buttons and a search field), copy this
 * class, point {@code start()} at your CRUD FXML, and replace the structural
 * assertions below with table-interaction tests (add / edit / delete / filter) that
 * use your real {@code fx:id} values.
 *
 * <p>Prerequisites:
 * <ul>
 *   <li>testfx-junit5 + openjfx-monocle dependencies in pom.xml</li>
 *   <li>main-view.fxml on the classpath at {@code /fxml/main-view.fxml}</li>
 *   <li>For CI: headless mode configured via the maven-surefire-plugin argLine</li>
 * </ul>
 *
 * @req FR-002, FR-004
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CRUDViewTest extends ApplicationTest {

    @Override
    public void start(Stage stage) throws IOException {
        // Load the application shell FXML.
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/main-view.fxml"));
        javafx.scene.Scene scene = new javafx.scene.Scene(loader.load());
        stage.setScene(scene);
        stage.show();
    }

    /**
     * The FXML loads and the root + content area are present.
     *
     * @req FR-002
     */
    @Test
    void mainViewLoadsSuccessfully_FR_002() {
        WaitForAsyncUtils.waitForFxEvents();
        BorderPane root = lookup("#root").query();
        assertNotNull(root, "Root BorderPane should be present");

        VBox contentArea = lookup("#contentArea").query();
        assertNotNull(contentArea, "Content area VBox should be present");
    }

    /**
     * The File menu exposes the New / Open / Exit items declared in the FXML.
     *
     * @req FR-002
     */
    @Test
    void fileMenuContainsExpectedItems_FR_002() {
        WaitForAsyncUtils.waitForFxEvents();
        MenuBar menuBar = lookup(".menu-bar").query();
        assertNotNull(menuBar, "Menu bar should be present");

        Menu fileMenu = menuBar.getMenus().stream()
                .filter(m -> "File".equals(m.getText()))
                .findFirst()
                .orElse(null);
        assertNotNull(fileMenu, "File menu should exist");

        List<String> fileItemTexts = fileMenu.getItems().stream()
                .map(MenuItem::getText)
                .collect(Collectors.toList());
        assertTrue(fileItemTexts.contains("New"), "File menu should contain 'New'");
        assertTrue(fileItemTexts.contains("Open..."), "File menu should contain 'Open...'");
        assertTrue(fileItemTexts.contains("Exit"), "File menu should contain 'Exit'");
    }

    /**
     * The Help menu exposes the About item declared in the FXML.
     *
     * @req FR-004
     */
    @Test
    void helpMenuContainsAboutItem_FR_004() {
        WaitForAsyncUtils.waitForFxEvents();
        MenuBar menuBar = lookup(".menu-bar").query();
        assertNotNull(menuBar, "Menu bar should be present");

        Menu helpMenu = menuBar.getMenus().stream()
                .filter(m -> "Help".equals(m.getText()))
                .findFirst()
                .orElse(null);
        assertNotNull(helpMenu, "Help menu should exist");

        List<String> helpItemTexts = helpMenu.getItems().stream()
                .map(MenuItem::getText)
                .collect(Collectors.toList());
        assertTrue(helpItemTexts.contains("About"), "Help menu should contain 'About'");
    }

    /**
     * The status bar and its label are present.
     *
     * @req FR-002
     */
    @Test
    void statusBarIsPresent_FR_002() {
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(lookup("#statusBar").query(), "Status bar should be present");
        assertNotNull(lookup("#statusLabel").query(), "Status label should be present");
    }

    /**
     * Opening the File menu does not throw. This exercises the menu rendering path;
     * adapt the assertions to your controller behaviour once handlers are implemented.
     *
     * @req FR-002
     */
    @Test
    void openingFileMenuDoesNotThrow_FR_002() {
        WaitForAsyncUtils.waitForFxEvents();
        clickOn("File");
        WaitForAsyncUtils.waitForFxEvents();
        // Close the menu again so the scene is left in a clean state.
        press(KeyCode.ESCAPE);
        WaitForAsyncUtils.waitForFxEvents();
    }
}
