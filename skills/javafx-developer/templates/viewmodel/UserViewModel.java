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

    /** Save action (delegated to the Service layer) */
    public void save() {
        // TODO: delegate to the Service layer for persistence
        System.out.println("Save: " + getName());
    }

    /** Load data from a model */
    public void loadFromModel(ObservableModel model) {
        if (model != null) {
            // TODO: sync data from the model into the ViewModel
            // Available fields: model.getName() / model.getCreatedAt()
        }
    }
}
