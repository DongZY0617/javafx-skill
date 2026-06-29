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

    @Test
    void handleRefreshCallsServiceFindAll() {
        controller.handleRefresh();
        verify(mockService, times(1)).findAll();
    }

    @Test
    void handleSaveCallsServiceSaveWithCorrectArgument() {
        String data = "test-data";
        controller.setInputData(data);
        controller.handleSave();
        verify(mockService).save(data);
    }

    @Test
    void handleDeleteCallsServiceDeleteById() {
        String id = "test-id";
        controller.setSelectedId(id);
        controller.handleDelete();
        verify(mockService).deleteById(id);
    }

    @Test
    void handleSaveWithEmptyInputDoesNotCallService() {
        controller.setInputData("");
        controller.handleSave();
        verify(mockService, never()).save(any());
    }

    @Test
    void handleRefreshUpdatesStatusLabel() {
        Label statusLabel = new Label();
        controller.setStatusLabel(statusLabel);
        when(mockService.findAll()).thenReturn(java.util.List.of("item1", "item2"));
        controller.handleRefresh();
        assertTrue(statusLabel.getText().contains("2"), "Status label should reflect item count");
    }
}
