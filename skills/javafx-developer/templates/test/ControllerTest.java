package {{packageName}}.controller;

import javafx.event.ActionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Controller Unit Test Template.
 * <p>
 * Tests {@link MainController}'s menu-bar event handlers by invoking them
 * directly with a Mockito-provided {@link ActionEvent}. This validates the
 * controller wiring without launching the JavaFX Application Thread.
 * </p>
 *
 * <p>{@code MainController} is a menu controller (New / Open / Exit / About);
 * it does not depend on a Service layer, so no Service mock is required.
 * Mockito is used here to supply a controlled {@code ActionEvent} instance
 * instead of constructing a real UI event.</p>
 *
 * Prerequisites:
 * - Mockito dependency in pom.xml (test scope) — see pom-test-dependencies.xml
 * - {@code MainController} exposes package-private {@code @FXML} handlers
 *   ({@code handleNew}, {@code handleOpen}, {@code handleExit},
 *   {@code handleAbout}) so this test, living in the same package, can
 *   invoke them directly.
 *
 * @req FR-001, FR-002
 */
public class ControllerTest {

    private MainController controller;
    private ActionEvent mockEvent;

    @BeforeEach
    void setUp() {
        controller = new MainController();
        // Mockito supplies a controlled ActionEvent without building a real UI event.
        mockEvent = Mockito.mock(ActionEvent.class);
    }

    /**
     * @req FR-001
     */
    @Test
    void handleNewIsInvocableWithoutThrowing_FR_001() {
        assertDoesNotThrow(() -> controller.handleNew(mockEvent),
                "handleNew should complete without throwing");
    }

    /**
     * @req FR-001
     */
    @Test
    void handleOpenIsInvocableWithoutThrowing_FR_001() {
        assertDoesNotThrow(() -> controller.handleOpen(mockEvent),
                "handleOpen should complete without throwing");
    }

    /**
     * @req FR-001
     */
    @Test
    void handleAboutIsInvocableWithoutThrowing_FR_001() {
        assertDoesNotThrow(() -> controller.handleAbout(mockEvent),
                "handleAbout should complete without throwing");
    }

    /**
     * @req FR-002
     */
    @Test
    void handleExitIsInvocableWithoutThrowing_FR_002() {
        // handleExit delegates to Platform.exit(). In a unit-test context with no
        // running JavaFX application this is a safe no-op and must not throw.
        assertDoesNotThrow(() -> controller.handleExit(mockEvent),
                "handleExit should complete without throwing");
    }
}
