package {{packageName}}.service;

import java.util.List;

/**
 * Abstract service layer template.
 * <p>
 * Encapsulates business logic, decoupled from the Controller/ViewModel.
 * Controllers/ViewModels access data sources through this layer.
 * <p>
 * Named {@code AbstractService} (rather than {@code Service}) to avoid an import
 * clash with {@link javafx.concurrent.Service} in projects that also run background
 * tasks on the JavaFX Application Thread. Concrete services should extend this
 * class, for example:
 * <pre>{@code
 * public class UserService extends AbstractService<User> {
 *     // implement findAll / findById / save / deleteById
 * }
 * }</pre>
 *
 * @param <T> the entity type managed by this service
 */
public abstract class AbstractService<T> {

    // TODO: inject a Repository or data access layer

    /**
     * Query all records.
     *
     * @return a list of all records (never {@code null})
     */
    public List<T> findAll() {
        // TODO: implement query logic
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Query a single record by ID.
     *
     * @param id the record identifier
     * @return the matching record, or {@code null} if not found
     */
    public T findById(Long id) {
        // TODO: implement query logic
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Save a record (insert or update).
     *
     * @param entity the record to save
     * @return the saved record (with generated id populated on insert)
     */
    public T save(T entity) {
        // TODO: implement save logic
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Delete a record by ID.
     *
     * @param id the identifier of the record to delete
     */
    public void deleteById(Long id) {
        // TODO: implement delete logic
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
