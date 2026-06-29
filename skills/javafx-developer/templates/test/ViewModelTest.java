package {{packageName}};

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ViewModel Unit Test Template.
 * Tests binding logic, computed properties, and command methods.
 * Uses JavaFX Properties directly (no UI needed, runs on any thread).
 *
 * Prerequisites:
 * - ViewModel exposes JavaFX Properties (StringProperty, BooleanProperty, etc.)
 * - JUnit 5 dependency in pom.xml (test scope)
 */
public class ViewModelTest {

    private UserViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new UserViewModel();
    }

    @Test
    void namePropertyDefaultsToEmpty() {
        assertEquals("", viewModel.nameProperty().get(), "Name should default to empty string");
    }

    @Test
    void saveDisabledWhenNameIsEmpty() {
        viewModel.nameProperty().set("");
        assertTrue(viewModel.saveDisabledProperty().get(),
                "Save should be disabled when name is empty");
    }

    @Test
    void saveEnabledWhenNameIsNotEmpty() {
        viewModel.nameProperty().set("John Doe");
        assertFalse(viewModel.saveDisabledProperty().get(),
                "Save should be enabled when name is not empty");
    }

    @Test
    void descriptionPropertyAcceptsLongText() {
        String longDescription = "A".repeat(500);
        viewModel.descriptionProperty().set(longDescription);
        assertEquals(500, viewModel.descriptionProperty().get().length(),
                "Description should accept long text");
    }

    @Test
    void formValidBindsToMultipleProperties() {
        // Test computed binding: formValid = !name.isEmpty() && !description.isEmpty()
        viewModel.nameProperty().set("Test");
        viewModel.descriptionProperty().set("");
        assertFalse(viewModel.isFormValid(), "Form should be invalid with empty description");

        viewModel.descriptionProperty().set("Description");
        assertTrue(viewModel.isFormValid(), "Form should be valid with both fields filled");
    }

    @Test
    void resetClearsAllProperties() {
        viewModel.nameProperty().set("John");
        viewModel.descriptionProperty().set("Some description");
        viewModel.reset();
        assertEquals("", viewModel.nameProperty().get(), "Name should be cleared after reset");
        assertEquals("", viewModel.descriptionProperty().get(), "Description should be cleared after reset");
    }
}
