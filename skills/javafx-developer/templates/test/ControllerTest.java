package {{packageName}};

import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Controller Unit Test Template.
 * Tests Controller event handling logic with mocked Service layer.
 * Does NOT require JavaFX Application Thread (pure logic testing).
 *
 * Prerequisites:
 * - Mockito dependency in pom.xml (test scope)
 * - Controller delegates business logic to a Service interface
 *
 * @req FR-001, FR-002
 */
public class ControllerTest {

    private MainController controller;
    private Service<String> mockService;

    @BeforeEach
    void setUp() {
        mockService = Mockito.mock(Service.class);
        controller = new MainController();
        controller.setService(mockService);
    }

    /**
     * @req FR-001
     */
    @Test
    void handleRefreshCallsServiceFindAll_FR_001() {
        controller.handleRefresh();
        verify(mockService, times(1)).findAll();
    }

    /**
     * @req FR-002
     */
    @Test
    void handleSaveCallsServiceSaveWithCorrectArgument_FR_002() {
        String data = "test-data";
        controller.setInputData(data);
        controller.handleSave();
        verify(mockService).save(data);
    }

    /**
     * @req FR-002
     */
    @Test
    void handleDeleteCallsServiceDeleteById_FR_002() {
        String id = "test-id";
        controller.setSelectedId(id);
        controller.handleDelete();
        verify(mockService).deleteById(id);
    }

    /**
     * @req FR-002
     */
    @Test
    void handleSaveWithEmptyInputDoesNotCallService_FR_002() {
        controller.setInputData("");
        controller.handleSave();
        verify(mockService, never()).save(any());
    }

    /**
     * @req FR-001
     */
    @Test
    void handleRefreshUpdatesStatusLabel_FR_001() {
        Label statusLabel = new Label();
        controller.setStatusLabel(statusLabel);
        when(mockService.findAll()).thenReturn(java.util.List.of("item1", "item2"));
        controller.handleRefresh();
        assertTrue(statusLabel.getText().contains("2"), "Status label should reflect item count");
    }
}
