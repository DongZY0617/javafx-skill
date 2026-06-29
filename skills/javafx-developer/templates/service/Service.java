package {{packageName}}.service;

/**
 * Service layer template.
 * Encapsulates business logic, decoupled from the Controller/ViewModel.
 * Controllers/ViewModels access data sources through this layer.
 */
public class Service<T> {

    // TODO: inject a Repository or data access layer

    /**
     * Query a single record by ID.
     */
    public T findById(Long id) {
        // TODO: implement query logic
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Save a record (insert or update).
     */
    public T save(T entity) {
        // TODO: implement save logic
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Delete a record by ID.
     */
    public void deleteById(Long id) {
        // TODO: implement delete logic
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
