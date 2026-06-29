# Database Integration Guide for JavaFX

This guide covers database layer integration for JavaFX applications, including JPA/Hibernate, MyBatis, Spring Data JPA, Flyway migrations, HikariCP connection pooling, common pitfalls around thread safety and transaction management on the UI thread, and integration of the database Entity with the JavaFX Property/Bean pattern. It extends `spring-boot-integration.md` (section 8) with deeper, database-focused guidance.

---

## 1. When to Add a Database Layer

A JavaFX desktop application needs a database layer when it must persist data across sessions, handle non-trivial relational data, or share state with other systems. Typical scenarios: local desktop apps persisting to SQLite/H2, line-of-business clients backed by a remote MySQL/PostgreSQL server, or offline-first apps syncing to a local store.

**Framework selection**:

| Scenario | Recommended Stack | Rationale |
|----------|-------------------|-----------|
| Small local app, hand-written SQL | JDBC + HikariCP | Minimal footprint, no ORM overhead |
| Medium app, SQL-fluent team | MyBatis + MyBatis-Spring | Full SQL control, simple mapping |
| Complex domain model, rapid CRUD | Spring Data JPA + Hibernate | Less boilerplate, repository abstractions |
| Schema evolves frequently | Above + Flyway | Versioned, repeatable migrations |

> **Default recommendation**: For Spring Boot + JavaFX projects, prefer Spring Data JPA + Flyway + HikariCP. For pure JavaFX (no Spring) projects, prefer MyBatis or plain JDBC with HikariCP.

---

## 2. JPA / Hibernate Integration

### 2.1 Dependencies

```xml
<!-- Spring Boot: spring-boot-starter-data-jpa pulls Hibernate + HikariCP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Database driver (H2 in-memory example) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 2.2 JPA Entity (with JavaFX Properties)

A JPA entity annotated with `@Entity` can expose JavaFX Properties for direct UI binding. The key technique is **property access**: keep the backing `Property` field as the JPA-managed attribute, and let JPA read/write through the getter/setter via `@Access(AccessType.PROPERTY)`.

```java
package com.example.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

@Entity
@Table(name = "users")
@Access(AccessType.PROPERTY)   // JPA reads/writes via getters/setters, not the field
public class User {

    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long getId() {
        return id.get();
    }

    public void setId(Long id) {
        // Handle null for primitive-typed Properties (see pitfall in section 8.2)
        this.id.set(id == null ? 0L : id);
    }

    @Transient   // Expose the Property object only to the UI, never persist it
    public LongProperty idProperty() {
        return id;
    }

    @Column(name = "name", nullable = false, length = 100)
    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    @Transient
    public StringProperty nameProperty() {
        return name;
    }

    @Column(name = "email", nullable = false, length = 200)
    public String getEmail() {
        return email.get();
    }

    public void setEmail(String email) {
        this.email.set(email);
    }

    @Transient
    public StringProperty emailProperty() {
        return email;
    }
}
```

**Key points**:
1. `@Access(AccessType.PROPERTY)` tells Hibernate to read/write via getters/setters so it reads the unwrapped value, not the `Property` object.
2. `xxxProperty()` accessors are annotated `@Transient` — the `Property` object itself is never persisted.
3. Primitive-typed Properties (`LongProperty`, `IntegerProperty`, `BooleanProperty`) must guard against `null` in their setters (new entities have a `null` id before insert).

### 2.3 EntityManager (non-Spring, pure JavaFX)

For projects not using Spring Boot, obtain an `EntityManagerFactory` from a `persistence.xml` and create short-lived `EntityManager` instances per operation.

**`src/main/resources/META-INF/persistence.xml`**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="https://jakarta.ee/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence
                                 https://jakarta.ee/xml/ns/persistence/persistence_3_0.xsd"
             version="3.0">
    <persistence-unit name="javafx-pu" transaction-type="RESOURCE_LOCAL">
        <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
        <class>com.example.model.User</class>
        <properties>
            <property name="jakarta.persistence.jdbc.url" value="jdbc:h2:./data/app"/>
            <property name="jakarta.persistence.jdbc.user" value="sa"/>
            <property name="jakarta.persistence.jdbc.password" value=""/>
            <property name="hibernate.hbm2ddl.auto" value="update"/>
            <property name="hibernate.show_sql" value="false"/>
            <property name="hibernate.format_sql" value="true"/>
        </properties>
    </persistence-unit>
</persistence>
```

```java
public final class PersistenceManager {
    private static final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("javafx-pu");

    private PersistenceManager() {}

    public static EntityManager createEntityManager() {
        return emf.createEntityManager();
    }

    public static void close() {
        emf.close();
    }
}
```

```java
// Usage: each operation opens/closes its own EntityManager on a background thread
public User findById(Long id) {
    EntityManager em = PersistenceManager.createEntityManager();
    try {
        return em.find(User.class, id);
    } finally {
        em.close();   // Always close in finally — EntityManager is NOT thread-safe
    }
}
```

> **Pitfall**: An `EntityManager` is **not thread-safe** and must never be shared across threads or stored in a Controller field that is accessed from the FX thread and a background `Task` simultaneously. Create a new one per operation, or per thread.

---

## 3. MyBatis Integration

### 3.1 Dependencies

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.4</version>
</dependency>
```

### 3.2 Mapper Interface

```java
package com.example.mapper;

import java.util.List;

import com.example.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User findById(@Param("id") Long id);

    List<User> findPage(@Param("keyword") String keyword,
                        @Param("offset") int offset,
                        @Param("limit") int limit);

    int insert(User user);

    int update(User user);

    int deleteById(@Param("id") Long id);
}
```

### 3.3 XML Mapping (`src/main/resources/mapper/UserMapper.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mapper.UserMapper">

    <resultMap id="userResultMap" type="com.example.model.User">
        <id     property="id"    column="id"/>
        <result property="name"  column="name"/>
        <result property="email" column="email"/>
    </resultMap>

    <select id="findById" resultMap="userResultMap">
        SELECT id, name, email FROM users WHERE id = #{id}
    </select>

    <select id="findPage" resultMap="userResultMap">
        SELECT id, name, email FROM users
        <where>
            <if test="keyword != null and keyword != ''">
                name LIKE CONCAT('%', #{keyword}, '%')
            </if>
        </where>
        ORDER BY id DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>

    <insert id="insert" parameterType="com.example.model.User"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO users (name, email) VALUES (#{name}, #{email})
    </insert>

    <update id="update" parameterType="com.example.model.User">
        UPDATE users SET name = #{name}, email = #{email} WHERE id = #{id}
    </update>

    <delete id="deleteById">
        DELETE FROM users WHERE id = #{id}
    </delete>
</mapper>
```

> **Security**: Always use `#{param}` (parameterized PreparedStatement placeholder). Never use `${param}` for user-controlled values — that is string concatenation and is a SQL injection vector (only `${}` is acceptable for static identifiers like table names chosen from a whitelist).

> **MyBatis + JavaFX Properties**: MyBatis invokes the getter/setter to populate the entity, so JavaFX Properties work out of the box. Apply the same null-handling in primitive-typed property setters (see section 8.2).

---

## 4. Repository Pattern with Spring Data JPA

Spring Data JPA generates the repository implementation at runtime from an interface, eliminating boilerplate CRUD code.

```java
package com.example.repository;

import java.util.List;
import java.util.Optional;

import com.example.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.name LIKE %:keyword% ORDER BY u.id DESC")
    List<User> search(@Param("keyword") String keyword);
}
```

**Key points**:
1. Extend `JpaRepository<Entity, IdType>` to get `findById`, `findAll`, `save`, `deleteById`, paging, and sorting for free.
2. Derived query methods (`findByEmail`, `findByNameContaining`) are parsed from the method name — no SQL needed.
3. Use `@Query` with JPQL **named parameters** (`:keyword` + `@Param("keyword")`) for anything beyond simple derived queries; never concatenate user input into JPQL.
4. Always wrap single-result lookups in `Optional<T>` to avoid `NullPointerException`.

> **Lifecycle note**: Spring Data repositories are thread-safe singletons backed by a connection pool. They are safe to inject into Controllers/Services and to call from background `Task` threads. They are **not** safe to call directly on the JavaFX Application Thread for long-running queries (see section 8.1).

---

## 5. Flyway Database Migrations

Flyway version-controls schema changes so that every environment applies migrations in the same deterministic order.

### 5.1 Dependencies

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<!-- Database-specific Flyway module (example: MySQL) -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

### 5.2 Configuration

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true   # For existing DBs without Flyway history
    baseline-version: 0
    validate-on-migrate: true    # Fail fast if checksums differ (detects manual edits)
```

### 5.3 Migration File Naming Convention

Flyway discovers migration scripts by filename pattern:

```
src/main/resources/db/migration/
├── V1__Create_users_table.sql        # Versioned migration
├── V2__Add_user_status_column.sql
├── V3__Seed_default_users.sql
└── R__Refresh_user_views.sql        # Repeatable migration (runs after every V* change)
```

- **Versioned** (`V{version}__{Description}.sql`): runs exactly once, in version order.
- **Repeatable** (`R__{Description}.sql`): re-runs whenever its checksum changes; use for views, stored procedures, triggers.

### 5.4 Rules

1. **Never edit an applied migration**: changing its checksum fails `validate-on-migrate`. Add a new `V{n}` migration instead.
2. **Keep migrations idempotent-friendly**: each `V` script should be runnable once on a fresh DB to produce the current schema.
3. **Large data migrations**: use `R__` scripts for view/proc definitions, batch DML in separate `V` scripts with explicit transactions.
4. **Desktop apps with embedded DBs**: enable `baseline-on-migrate: true` so the first launch against an empty/file DB works without manual baselining.

---

## 6. Connection Pool Configuration (HikariCP)

HikariCP is the default pool in Spring Boot 2+. Desktop apps must tune pool size conservatively — a desktop client rarely needs more than a few connections.

### 6.1 Dependencies

Spring Boot starter data-jpa already bundles HikariCP. For non-Spring projects:

```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.1.0</version>
</dependency>
```

### 6.2 application.yml (Spring Boot)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/myapp?useSSL=false&serverTimezone=UTC
    username: ${DB_USER:admin}
    password: ${DB_PASSWORD}      # From env var, never hardcoded
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      pool-name: JavaFXApp-Pool
      maximum-pool-size: 5         # Desktop apps: 3-10 is plenty
      minimum-idle: 1
      idle-timeout: 600000         # 10 min
      max-lifetime: 1800000        # 30 min (must be < DB wait_timeout)
      connection-timeout: 30000    # 30 s — fail fast if pool exhausted
      leak-detection-threshold: 60000   # Log leaked connections after 60 s
```

### 6.3 Programmatic Configuration (non-Spring)

```java
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:h2:./data/app");
config.setUsername("sa");
config.setPassword("");
config.setMaximumPoolSize(5);
config.setLeakDetectionThreshold(60_000);
config.setPoolName("JavaFXApp-Pool");

HikariDataSource ds = new HikariDataSource(config);
// Shutdown on Application.stop() to release pool and avoid process hanging
```

### 6.4 Tuning Guidelines

| Parameter | Desktop Default | Rationale |
|-----------|-----------------|-----------|
| `maximum-pool-size` | 3–10 | One user, low concurrency; large pools waste memory |
| `connection-timeout` | 30s | Fail fast so the UI can show an error instead of hanging |
| `leak-detection-threshold` | 60s | Catches forgotten `em.close()` / unclosed connections |
| `max-lifetime` | 30 min | Must be shorter than the DB `wait_timeout` to avoid stale connections |

> **Pitfall**: Setting `maximum-pool-size` too high in a desktop app wastes resources and can exhaust a small DB server (e.g., SQLite only allows one writer). For SQLite use `maximum-pool-size: 1` to avoid `database is locked` errors.

---

## 7. Integration with JavaFX Property/Bean Pattern

### 7.1 The Entity-as-Model Approach

When the JPA entity is also the JavaFX model (bound directly to the UI), keep these rules:

1. **`@Access(AccessType.PROPERTY)`** on the entity so Hibernate uses getters/setters (not the `Property` fields).
2. **`@Transient` on `xxxProperty()` accessors** so the `Property` object is never persisted.
3. **Null-safe setters** for primitive-typed Properties (`LongProperty`, `IntegerProperty`, `BooleanProperty`).
4. **Do not register long-lived listeners on entity properties held by the persistence context** — entity detach/merge can cause stale listener references (see section 8.3).

### 7.2 DTO vs Entity Bound to UI

| Approach | When to Use | Trade-off |
|----------|-------------|-----------|
| **Entity bound directly to UI** | Simple CRUD forms, single-screen edit | Less code; but a half-edited dirty entity can leak to the persistence context |
| **DTO / ViewModel bound to UI** | Multi-step wizards, complex validation, or sharing entities across views | Safer; copy DTO → entity at save time inside a transaction |

> **Recommendation**: For anything beyond a single-screen form, bind the UI to a separate ViewModel/DTO and copy values into a fresh entity inside the Service `@Transactional` method. This avoids persisting partially edited, invalid state.

### 7.3 ObservableList from a Query

When loading a list for a `TableView`, replace the list atomically with `setAll()` so only one change event fires:

```java
Task<List<User>> loadTask = new Task<>() {
    @Override
    protected List<User> call() {
        return userRepository.findAll();   // background thread
    }
};
loadTask.setOnSucceeded(e ->
        userTable.getItems().setAll(loadTask.getValue()));   // FX thread, atomic
new Thread(loadTask).start();
```

---

## 8. Common Pitfalls

### 8.1 Blocking DB Calls on the UI Thread (Most Common)

**Symptom**: UI freezes during save/load; the window shows "Not Responding".

**Cause**: `repository.findAll()` or `mapper.findPage(...)` is called synchronously in an `@FXML` event handler, which runs on the JavaFX Application Thread. The JDBC call blocks the UI thread.

**Solution**: Wrap every DB call in a `Task<T>` (or `javafx.concurrent.Service`) and update the UI via `setOnSucceeded` / `Platform.runLater()`:

```java
@FXML
private void handleSearch() {
    Task<List<User>> task = new Task<>() {
        @Override
        protected List<User> call() {
            return userService.search(keywordField.getText());   // background
        }
    };
    task.setOnSucceeded(e -> userTable.getItems().setAll(task.getValue()));
    task.setOnFailed(e -> showError(task.getException()));
    new Thread(task).start();
}
```

> Even "fast" queries can stall on a locked DB or network latency; always offload DB I/O from the FX thread.

### 8.2 Null Handling for Primitive-Typed Properties

**Symptom**: `NullPointerException` when inserting a new entity; JPA/MyBatis injects `null` into `SimpleLongProperty.set(long)`.

**Cause**: `SimpleLongProperty.set(long)`, `SimpleIntegerProperty.set(int)`, and `SimpleBooleanProperty.set(boolean)` accept only primitives and throw NPE on `null`.

**Solution**: Convert null to a default value in the setter:

```java
public void setId(Long id) {
    this.id.set(id == null ? 0L : id);
}
```

### 8.3 Transaction Boundary on the UI Thread

**Symptom**: `LazyInitializationException`, partial saves, or a transaction spanning multiple user interactions.

**Cause**: A `@Transactional` Service method is called on the FX thread (freezing the UI), OR an entity is detached from the persistence context and its lazy associations are accessed later in a Controller (after the session closed).

**Solution**:
1. Keep transactions **short and server-side** — open and close them inside a single Service method executed on a background thread.
2. Never carry a Hibernate-managed entity across user interactions. Convert to a detached DTO/ViewModel before returning to the Controller.
3. Use `@Transactional(readOnly = true)` for query-only methods — it can skip flushing and allow optimizations.

```java
@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User save(User input) {
        // Whole transaction lives and dies inside this method, on a background thread
        User merged = userRepository.save(input);
        return toDetached(merged);   // return a copy free of proxies
    }
}
```

### 8.4 Forgetting to Close the DataSource / EntityManagerFactory

**Symptom**: The JavaFX process does not exit after the window closes; DB connections leak.

**Cause**: The pool / `EntityManagerFactory` was opened at startup but never released.

**Solution**: Close resources in `Application.stop()`:

```java
@Override
public void stop() {
    if (springContext != null) {
        springContext.close();   // closes HikariCP, EntityManagerFactory, Flyway
    } else {
        PersistenceManager.close();   // non-Spring path
        if (dataSource != null) dataSource.close();
    }
}
```

### 8.5 Connection Pool Leak (Unclosed EntityManager / Connection)

**Symptom**: Pool exhausted (`HikariPool-1 - Connection is not available`), app hangs after some operations.

**Cause**: An `EntityManager` or `Connection` obtained manually was not closed in a `finally` block, or an exception was thrown before `close()`.

**Solution**: Use try-with-resources or try/finally for every manually-obtained connection:

```java
public User findById(Long id) {
    EntityManager em = emf.createEntityManager();
    try {
        return em.find(User.class, id);
    } finally {
        em.close();   // ALWAYS close, even on exception
    }
}
```

Enable `leak-detection-threshold` in HikariCP to catch this in dev (see section 6.2).

### 8.6 SQLite "database is locked"

**Symptom**: `org.sqlite.SQLiteException: [SQLITE_BUSY] The database file is locked`.

**Cause**: SQLite allows only one writer. A pool with `maximum-pool-size > 1` plus concurrent writes causes contention.

**Solution**: For SQLite, set `maximum-pool-size: 1`, or use a single-writer queue. Prefer H2 or a real server DB for concurrent write workloads.

### 8.7 Flyway Checksum Failure After Editing an Applied Migration

**Symptom**: `Flyway: Migration checksum mismatch`.

**Cause**: A migration script that already ran was edited in place.

**Solution**: Never edit applied migrations. Add a new `V{n}__....sql`. If you must repair (dev only), run `flyway repair` to update the schema history checksums.

---

## 9. Recommended Stack Summary

| Concern | Choice | Notes |
|---------|--------|-------|
| ORM / SQL mapper | Spring Data JPA (default) / MyBatis | JPA for domain models; MyBatis for SQL-heavy apps |
| Connection pool | HikariCP | Default in Spring Boot; tune pool size for desktop |
| Schema migration | Flyway | Versioned `V{n}` scripts; never edit applied ones |
| Entity ↔ UI | `@Access(PROPERTY)` + `@Transient` Property accessors | See section 7 |
| Background DB calls | `javafx.concurrent.Task` / `Service` | Never block the FX thread |
| Transaction scope | Single `@Transactional` Service method | Short, server-side, on a background thread |
| Desktop embedded DB | H2 (mode=MySQL) or SQLite | SQLite: pool size = 1 |

---

## 10. Related Documents

- `references/spring-boot-integration.md` — Spring Boot + JavaFX startup, DI, and the persistence-layer introduction this guide extends (section 8)
- `references/data-binding-patterns.md` — JavaFX Property types and binding modes
- `references/architecture-patterns.md` — MVC/MVVM/MVP layering, including where the repository/service layer sits
- `javafx-code-reviewer`'s `references/security-checklist.md` — SQL injection and secret-handling review criteria (cross-references this document's pitfalls)
- Templates: `templates/dao/Entity.java`, `templates/dao/Repository.java`, `templates/dao/FlywayMigration.sql`
