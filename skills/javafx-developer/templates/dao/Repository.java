package {{packageName}}.repository;

import java.util.List;
import java.util.Optional;

import {{packageName}}.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository interface template.
 * <p>
 * Spring Data generates the implementation at runtime from this interface,
 * eliminating boilerplate CRUD code. Extend {@link JpaRepository} to obtain
 * {@code findById}, {@code findAll}, {@code save}, {@code deleteById},
 * paging, and sorting for free. Add derived query methods or
 * {@link Query}-annotated methods for custom lookups.
 * </p>
 * <p>
 * <b>Thread safety</b>: Spring Data repositories are thread-safe singletons
 * backed by the HikariCP connection pool. They are safe to inject into
 * Controllers/Services and to call from a background
 * {@code javafx.concurrent.Task}. They must <b>not</b> be called directly on
 * the JavaFX Application Thread for long-running queries — wrap every DB call
 * in a {@code Task} and update the UI via {@code setOnSucceeded} /
 * {@code Platform.runLater()} (see {@code references/database-integration.md}
 * section 8.1).
 * </p>
 * <p>
 * <b>Security</b>: Always use named parameters ({@code :name} +
 * {@link Param}) or derived methods. Never concatenate user input into JPQL —
 * that is a SQL/JPQL injection vector.
 * </p>
 *
 * @param <User> the entity type
 * @param <Long> the primary key type
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by exact email match.
     * <p>Derived query method — Spring Data parses the method name.</p>
     *
     * @param email the email to match
     * @return an {@link Optional} containing the user, or
     *         {@link Optional#empty()} if none exists
     */
    Optional<User> findByEmail(String email);

    /**
     * Searches users whose name contains the keyword (case-insensitive),
     * newest first.
     * <p>
     * Uses a JPQL query with a <b>named parameter</b> ({@code :keyword}) to
     * prevent JPQL injection. Never concatenate user input into the query
     * string.
     * </p>
     *
     * @param keyword the substring to search for; may be empty to return all
     * @return the matching users, newest first; never {@code null}
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY u.id DESC")
    List<User> search(@Param("keyword") String keyword);

    /**
     * Counts active users.
     *
     * @return the number of users whose {@code active} flag is true
     */
    long countByActiveTrue();
}
