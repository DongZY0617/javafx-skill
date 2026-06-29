package {{packageName}}.service;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository interface template.
 * <p>
 * Defines the standard CRUD data access contract for an entity, decoupled from
 * the underlying persistence technology (database, file, in-memory, etc.).
 * The Service layer accesses data sources through this interface, making it
 * easy to swap storage implementations or mock them in unit tests.
 * </p>
 *
 * @param <T>  the entity type
 * @param <ID> the primary key type
 */
public interface Repository<T, ID> {

    /**
     * Finds a single entity by its primary key.
     *
     * @param id the primary key value, must not be {@code null}
     * @return an {@link Optional} containing the entity, or
     *         {@link Optional#empty()} if no matching record exists
     */
    Optional<T> findById(ID id);

    /**
     * Retrieves all entities.
     *
     * @return the list of entities; an empty list when there is no data
     *         (must never return {@code null})
     */
    List<T> findAll();

    /**
     * Saves an entity (insert or update).
     * <p>
     * If the entity carries a primary key that already exists in the
     * persistence layer, an update is performed; otherwise an insert.
     * </p>
     *
     * @param entity the entity to save, must not be {@code null}
     * @return the saved entity (may include a generated primary key or version)
     */
    T save(T entity);

    /**
     * Deletes the entity identified by the given primary key.
     * <p>
     * If no record exists for the key, this method should return silently
     * without throwing an exception.
     * </p>
     *
     * @param id the primary key value, must not be {@code null}
     */
    void deleteById(ID id);
}
