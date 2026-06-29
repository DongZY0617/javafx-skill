package {{packageName}}.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * JPA Entity template with JavaFX Property integration.
 * <p>
 * Demonstrates the recommended pattern for binding a JPA-managed entity
 * directly to JavaFX UI controls while keeping the persistence layer and
 * the UI binding layer cleanly separated:
 * </p>
 * <ul>
 *   <li>{@link Access}({@link AccessType#PROPERTY}) — Hibernate reads and
 *       writes through the getters/setters, so it reads the unwrapped value
 *       rather than the {@code Property} object.</li>
 *   <li>{@link Transient} on each {@code xxxProperty()} accessor — the
 *       {@code Property} object itself is never persisted.</li>
 *   <li>Null-safe setters for primitive-typed Properties
 *       ({@link LongProperty}, {@link BooleanProperty}) so that JPA/MyBatis
 *       can pass {@code null} (e.g. for a new entity's id) without throwing
 *       {@code NullPointerException}.</li>
 * </ul>
 * <p>
 * See {@code references/database-integration.md} section 2 and 7 for the
 * full rationale and the DTO-vs-entity discussion.
 * </p>
 */
@Entity
@Table(name = "users")
@Access(AccessType.PROPERTY)
public class User {

    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final BooleanProperty active = new SimpleBooleanProperty(true);

    /**
     * Primary key.
     *
     * @return the id, or {@code 0} when unset (never {@code null})
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long getId() {
        return id.get();
    }

    /**
     * Sets the primary key.
     * <p>
     * {@code null} is converted to {@code 0L} because
     * {@link SimpleLongProperty#set(long)} accepts only the primitive
     * {@code long} and would otherwise throw {@code NullPointerException}
     * when the persistence layer injects {@code null} for a new entity.
     * </p>
     *
     * @param id the id value, may be {@code null}
     */
    public void setId(Long id) {
        this.id.set(id == null ? 0L : id);
    }

    /**
     * @return the id property for UI binding; never persisted
     */
    @Transient
    public LongProperty idProperty() {
        return id;
    }

    /**
     * @return the user's display name
     */
    @Column(name = "name", nullable = false, length = 100)
    public String getName() {
        return name.get();
    }

    /**
     * Sets the user's display name.
     *
     * @param name the new name, may be {@code null}
     */
    public void setName(String name) {
        this.name.set(name);
    }

    /**
     * @return the name property for UI binding; never persisted
     */
    @Transient
    public StringProperty nameProperty() {
        return name;
    }

    /**
     * @return the user's email address
     */
    @Column(name = "email", nullable = false, length = 200)
    public String getEmail() {
        return email.get();
    }

    /**
     * Sets the user's email address.
     *
     * @param email the new email, may be {@code null}
     */
    public void setEmail(String email) {
        this.email.set(email);
    }

    /**
     * @return the email property for UI binding; never persisted
     */
    @Transient
    public StringProperty emailProperty() {
        return email;
    }

    /**
     * @return whether the user account is active
     */
    @Column(name = "active", nullable = false)
    public boolean isActive() {
        return active.get();
    }

    /**
     * Sets whether the user account is active.
     * <p>
     * {@code null} is converted to {@code false} because
     * {@link SimpleBooleanProperty#set(boolean)} accepts only the primitive
     * {@code boolean}.
     * </p>
     *
     * @param active the new active flag, may be {@code null}
     */
    public void setActive(Boolean active) {
        this.active.set(active == null ? false : active);
    }

    /**
     * @return the active property for UI binding; never persisted
     */
    @Transient
    public BooleanProperty activeProperty() {
        return active;
    }

    @Override
    public String toString() {
        return "User{id=" + getId() + ", name=" + getName()
                + ", email=" + getEmail() + ", active=" + isActive() + "}";
    }
}
