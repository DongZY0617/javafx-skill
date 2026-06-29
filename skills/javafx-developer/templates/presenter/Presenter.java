package {{packageName}}.presenter;

import java.util.List;

import {{packageName}}.model.{{entityName}};
import {{packageName}}.service.{{entityName}}Service;
import {{packageName}}.view.{{entityName}}View;

/**
 * Presenter template (MVP pattern).
 * <p>
 * Holds a reference to {@link {{entityName}}View} and explicitly controls the UI
 * through this interface without depending on any JavaFX controls,
 * enabling independent unit testing.
 * </p>
 * <p>
 * The Controller implements the View interface and passes itself to the
 * Presenter constructor. All business logic is handled by the Presenter;
 * the Controller only provides glue code for UI operations.
 * </p>
 */
public class {{entityName}}Presenter {

    private final {{entityName}}View view;
    private final {{entityName}}Service service;

    /**
     * Constructs the Presenter.
     *
     * @param view    the View interface reference (typically implemented by the Controller)
     * @param service the business logic layer
     */
    public {{entityName}}Presenter({{entityName}}View view, {{entityName}}Service service) {
        this.view = view;
        this.service = service;
    }

    /**
     * Loads all data and updates the view.
     */
    public void onLoadData() {
        List<{{entityName}}> list = service.findAll();
        view.setDataList(list);
    }

    /**
     * Handles the add operation.
     * <p>
     * Retrieves input data from the view, validates it, saves via the
     * Service, and refreshes the list.
     * </p>
     */
    public void onAdd() {
        {{entityName}} entity = view.getInputData();
        if (entity == null || !isValid(entity)) {
            view.showValidationError("Data is incomplete or invalid");
            return;
        }
        service.save(entity);
        view.clearInput();
        onLoadData();
    }

    /**
     * Handles the delete operation.
     *
     * @param id the primary key of the record to delete
     */
    public void onDelete(Long id) {
        service.deleteById(id);
        onLoadData();
    }

    /**
     * Simple validation, extend as needed.
     *
     * @param entity the entity to validate
     * @return true if validation passes
     */
    private boolean isValid({{entityName}} entity) {
        return entity != null;
    }
}
