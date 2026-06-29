package {{packageName}}.model;

import java.time.LocalDateTime;
import java.util.Objects;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Generic observable model providing standard JavaFX property support.
 * <p>
 * Uses {@link StringProperty} for the name and {@link ObjectProperty} for
 * the creation timestamp, enabling automatic UI binding and change tracking.
 * </p>
 */
public class ObservableModel {

    private final StringProperty name = new SimpleStringProperty(this, "name");
    private final ObjectProperty<LocalDateTime> createdAt =
            new SimpleObjectProperty<>(this, "createdAt");

    /**
     * Creates a new model with a default name and the current timestamp.
     */
    public ObservableModel() {
        this.createdAt.set(LocalDateTime.now());
    }

    /**
     * Creates a new model with the given name and the current timestamp.
     *
     * @param name the initial name
     */
    public ObservableModel(String name) {
        this();
        setName(name);
    }

    /**
     * @return the name property
     */
    public final StringProperty nameProperty() {
        return name;
    }

    /**
     * @return the current name value
     */
    public final String getName() {
        return name.get();
    }

    /**
     * Sets the name value.
     *
     * @param name the new name
     */
    public final void setName(String name) {
        this.name.set(name);
    }

    /**
     * @return the creation timestamp property
     */
    public final ObjectProperty<LocalDateTime> createdAtProperty() {
        return createdAt;
    }

    /**
     * @return the creation timestamp value
     */
    public final LocalDateTime getCreatedAt() {
        return createdAt.get();
    }

    /**
     * Sets the creation timestamp.
     *
     * @param createdAt the new creation timestamp
     */
    public final void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt.set(createdAt);
    }

    @Override
    public String toString() {
        return "ObservableModel{name=" + getName() + ", createdAt=" + getCreatedAt() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ObservableModel that = (ObservableModel) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getCreatedAt(), that.getCreatedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getCreatedAt());
    }
}
