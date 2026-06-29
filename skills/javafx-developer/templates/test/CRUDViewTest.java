package {{packageName}};

import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.testfx.api.FxAssert.*;
import static org.testfx.matcher.control.TableViewMatchers.*;

/**
 * CRUD View Integration Test Template.
 * Uses TestFX to verify table interactions: add, edit, delete, filter.
 * Runs on JavaFX Application Thread via ApplicationTest.
 *
 * Prerequisites:
 * - testfx-junit5 + openjfx-monocle dependencies in pom.xml
 * - FXML file with TableView (fx:id="tableView"), TextField (fx:id="searchField")
 * - For CI: configure headless mode via surefire argLine
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CRUDViewTest extends ApplicationTest {

    private TableView<?> tableView;

    @Override
    public void start(Stage stage) throws IOException {
        // Load your CRUD view FXML
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/main-view.fxml"));
        javafx.scene.Scene scene = new javafx.scene.Scene(loader.load());
        stage.setScene(scene);
        stage.show();
        tableView = lookup("#tableView").query();
    }

    @Test
    void tableViewInitializesWithData() {
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(tableView.getItems().size() > 0,
                "TableView should have initial data after load");
    }

    @Test
    void addNewItemInsertsRow() {
        int initialCount = tableView.getItems().size();
        clickOn("#addButton");
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(initialCount + 1, tableView.getItems().size(),
                "Adding an item should increase row count by 1");
    }

    @Test
    void deleteSelectedRemovesRow() {
        // Select first row
        clickOn(tableView);
        WaitForAsyncUtils.waitForFxEvents();
        int initialCount = tableView.getItems().size();
        clickOn("#deleteButton");
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(initialCount - 1, tableView.getItems().size(),
                "Deleting an item should decrease row count by 1");
    }

    @Test
    void searchFieldFiltersTable() {
        // Type in search field to filter
        clickOn("#searchField").write("nonexistent");
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(0, tableView.getItems().size(),
                "Searching for nonexistent text should show 0 results");

        // Clear search
        clickOn("#searchField").press(KeyCode.CONTROL, KeyCode.A).eraseText(1);
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(tableView.getItems().size() > 0,
                "Clearing search should restore all items");
    }

    @Test
    void editSelectedUpdatesRow() {
        // Double-click first cell to edit
        doubleClickOn(tableView);
        WaitForAsyncUtils.waitForFxEvents();
        write("Edited");
        press(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();
        // Verify the cell content changed (adjust based on your model)
        // Example: assertEquals("Edited", firstCell.getValue());
    }
}
