package {{packageName}}.view;

import java.util.List;

import {{packageName}}.model.{{entityName}};

/**
 * View interface template (MVP pattern).
 * <p>
 * Abstracts the capabilities the View exposes to the Presenter, with no
 * JavaFX dependencies. The Controller implements this interface and wraps
 * UI operations into interface methods; the Presenter controls the View
 * through this interface, achieving complete decoupling of UI and logic.
 * </p>
 * <p>
 * Design principle: interface methods should be fine-grained, exposing only
 * the operations the Presenter needs. Avoid passing the entire Stage or
 * Scene into the Presenter.
 * </p>
 */
public interface {{entityName}}View {

    /**
     * Retrieves the user-entered data.
     *
     * @return the entity object containing the input data, or null if input is invalid
     */
    {{entityName}} getInputData();

    /**
     * Refreshes the data list display.
     *
     * @param list the data list to display
     */
    void setDataList(List<{{entityName}}> list);

    /**
     * Clears the input area.
     */
    void clearInput();

    /**
     * Displays a validation error message.
     *
     * @param message the error description
     */
    void showValidationError(String message);
}
