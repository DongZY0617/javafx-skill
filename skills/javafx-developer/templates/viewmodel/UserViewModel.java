package {{packageName}}.viewmodel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import {{packageName}}.model.ObservableModel;

/**
 * ViewModel template (MVVM pattern).
 * Exposes Properties for the View to bind to and encapsulates business logic.
 */
public class UserViewModel {

    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final BooleanProperty saveDisabled = new SimpleBooleanProperty(true);

    public UserViewModel() {
        // Disable the save button when the name is empty
        saveDisabled.bind(name.isEmpty());
    }

    // Name property
    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }

    // Description property
    public StringProperty descriptionProperty() { return description; }
    public String getDescription() { return description.get(); }
    public void setDescription(String value) { description.set(value); }

    // Save button disabled state (read-only)
    public BooleanProperty saveDisabledProperty() { return saveDisabled; }

    /**
     * Computed form validity.
     * <p>
     * The form is considered valid only when both the name and description
     * fields are non-empty. This mirrors the binding contract expected by
     * {@code ViewModelTest}: {@code formValid = !name.isEmpty() && !description.isEmpty()}.
     * </p>
     *
     * @return {@code true} if both name and description are non-empty
     */
    public boolean isFormValid() {
        return !name.get().isEmpty() && !description.get().isEmpty();
    }

    /**
     * Resets all editable properties to their default (empty) state.
     * <p>
     * Clears the name and description fields, leaving the ViewModel ready
     * for fresh input. Expected by {@code ViewModelTest#resetClearsAllProperties_FR_003}.
     * </p>
     */
    public void reset() {
        name.set("");
        description.set("");
    }

    /** Save action (delegated to the Service layer) */
    public void save() {
        // TODO: delegate to UserService.save(this.toModel()) for persistence
    }

    /** Load data from a model */
    public void loadFromModel(ObservableModel model) {
        if (model != null) {
            // TODO: sync data from the model into the ViewModel
            // Available fields: model.getName() / model.getCreatedAt()
        }
    }
}
