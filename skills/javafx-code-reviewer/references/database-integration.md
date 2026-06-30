# Database Integration Review Standards

This document defines the review criteria for the "Database Access Security" dimension (Dimension 7) in `javafx-code-reviewer`. It governs SQL injection prevention, connection pool management, transaction handling, ORM entity mapping, and database resource cleanup in JavaFX desktop applications. Default severity baseline: Major.

This document cross-references the database schema defined by `javafx-architect` in `references/database-design.md` and the `database_schema` section of `architecture-handoff.json`.

---

## Check Item 1: SQL Injection Prevention

**Focus**: Whether all database queries use parameterized statements (PreparedStatement / named parameters) and no SQL is constructed via string concatenation with user input.

**Pass Criteria**:
- All SQL queries use `PreparedStatement` with `?` placeholders, or MyBatis `#{param}` named parameters
- No `Statement` (without "Prepared") is used for queries containing user input
- No string concatenation (`"SELECT ... WHERE name = '" + userInput + "'"`) in SQL construction
- Dynamic table names or column names (which cannot be parameterized) are validated against a whitelist
- `ORDER BY` clauses with dynamic column names use whitelist validation, not direct user input

**Common Violations**:
```java
// CRITICAL: String concatenation — SQL injection
String sql = "SELECT * FROM user WHERE username = '" + username + "'";
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery(sql);

// Pass: Parameterized query
String sql = "SELECT * FROM user WHERE username = ?";
PreparedStatement stmt = conn.prepareStatement(sql);
stmt.setString(1, username);
ResultSet rs = stmt.executeQuery(sql);

// Pass: MyBatis named parameter
// Mapper XML: SELECT * FROM user WHERE username = #{username}

// CRITICAL: Dynamic column name without whitelist
String sql = "SELECT * FROM user ORDER BY " + sortColumn; // injection if sortColumn is user input

// Pass: Dynamic column name with whitelist
List<String> allowedColumns = List.of("username", "email", "created_at");
if (!allowedColumns.contains(sortColumn)) {
    throw new IllegalArgumentException("Invalid sort column");
}
String sql = "SELECT * FROM user ORDER BY " + sortColumn;
```

**Severity Guide**:
- String concatenation with user input in SQL → Critical (direct SQL injection)
- `Statement` used with user input → Critical
- Dynamic column/table name without whitelist → Major
- String concatenation in non-user-input SQL (e.g., hardcoded constants) → Minor

---

## Check Item 2: Connection Pool Management

**Focus**: Whether database connections are obtained from a connection pool (not created ad-hoc) and properly closed after use.

**Pass Criteria**:
- A connection pool is configured (HikariCP, c3p0, or Spring-managed DataSource)
- No `DriverManager.getConnection()` calls in production code (acceptable only in test utilities)
- Connections obtained from the pool are always returned (closed) — verified via try-with-resources or try-finally
- Connection pool configuration is reasonable (max pool size, idle timeout, leak detection)

**Common Violations**:
```java
// CRITICAL: Ad-hoc connection creation (bypasses pool)
Connection conn = DriverManager.getConnection(url, user, pass);
// ... use connection ...
// Missing close — connection leak!

// Pass: Try-with-resources ensures auto-close
try (Connection conn = dataSource.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql)) {
    stmt.setString(1, username);
    try (ResultSet rs = stmt.executeQuery()) {
        // process result
    }
}

// Major: Connection obtained but not in try-with-resources
Connection conn = dataSource.getConnection();
PreparedStatement stmt = conn.prepareStatement(sql);
// ... if exception thrown here, conn is never closed ...
stmt.close();
conn.close();

// Pass: Try-finally fallback (when try-with-resources is not possible)
Connection conn = null;
try {
    conn = dataSource.getConnection();
    // ... use connection ...
} finally {
    if (conn != null) {
        try { conn.close(); } catch (SQLException ignored) {}
    }
}
```

**HikariCP Configuration for JavaFX Desktop Apps**:
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 5        # Desktop apps: small pool (3-10)
      minimum-idle: 1             # Keep 1 idle connection
      idle-timeout: 300000        # 5 min idle timeout
      connection-timeout: 30000   # 30s connection timeout
      max-lifetime: 1800000       # 30 min max connection lifetime
      leak-detection-threshold: 60000  # 1 min leak detection
```

> **Desktop app note**: Connection pools for desktop apps should be small (3-10 connections max). Unlike web servers handling hundreds of concurrent requests, desktop apps typically have 1-2 active database sessions at a time. Over-sizing the pool wastes memory and may exhaust database connections if multiple app instances run on the same machine.

**Severity Guide**:
- `DriverManager.getConnection()` in production code → Major
- Connection not closed (leak) → Critical
- Connection closed but not in try-with-resources → Minor (risky but functional)
- Pool size > 20 for desktop app → Minor (wasteful but not dangerous)

---

## Check Item 3: Transaction Management

**Focus**: Whether database operations that must be atomic are wrapped in transactions, and whether transactions are properly committed or rolled back.

**Pass Criteria**:
- Multi-statement operations that must be atomic are wrapped in transactions
- Transactions are explicitly committed after successful operations
- Transactions are rolled back on exception
- Auto-commit is disabled for multi-statement transactions (`conn.setAutoCommit(false)`)
- Transaction scope is minimal (do not include non-DB operations inside a transaction)

**Common Violations**:
```java
// CRITICAL: Multi-statement operation without transaction
public void createOrder(Order order, List<OrderItem> items) {
    orderDao.insert(order);
    for (OrderItem item : items) {
        itemDao.insert(item);  // If this fails, order is created without items
    }
}

// Pass: Transaction wrapping
public void createOrder(Order order, List<OrderItem> items) {
    try (Connection conn = dataSource.getConnection()) {
        conn.setAutoCommit(false);
        try {
            orderDao.insert(conn, order);
            for (OrderItem item : items) {
                itemDao.insert(conn, item);
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw new DataAccessException("Failed to create order", e);
        }
    }
}

// Pass: Spring @Transactional annotation (if using Spring)
@Transactional
public void createOrder(Order order, List<OrderItem> items) {
    orderDao.insert(order);
    for (OrderItem item : items) {
        itemDao.insert(item);
    }
}

// Major: Transaction includes non-DB operations
@Transactional
public void createOrder(Order order, List<OrderItem> items) {
    orderDao.insert(order);
    for (OrderItem item : items) {
        itemDao.insert(item);
    }
    // Non-DB operation inside transaction — holds DB connection unnecessarily
    sendNotificationEmail(order.getUserEmail());  // Move outside transaction!
    exportOrderPdf(order);                         // Move outside transaction!
}
```

**Severity Guide**:
- Multi-statement write without transaction → Critical (data inconsistency)
- Transaction not rolled back on exception → Critical
- Non-DB operations inside transaction → Major (connection held too long)
- Read-only operation in transaction unnecessarily → Minor

---

## Check Item 4: ORM Entity Mapping Verification

**Focus**: Whether ORM entities (JPA/Hibernate or MyBatis mappers) correctly match the database schema defined in `architecture-handoff.json`'s `database_schema` section.

**Pass Criteria**:
- Entity table names match schema table names (`@Table(name = "user")` matches `user` table)
- Entity field names map to correct columns (via `@Column` or MyBatis `resultMap`)
- Entity field types match schema column types (`String` ↔ `VARCHAR`, `Long` ↔ `BIGINT`, `BigDecimal` ↔ `DECIMAL`)
- Nullable columns are mapped to nullable entity fields (object wrappers, not primitives: `Integer` not `int`)
- Foreign key relationships are correctly annotated (`@ManyToOne`, `@OneToMany`, `@JoinColumn`)
- Primary key generation strategy matches schema (`@GeneratedValue(strategy = IDENTITY)` for auto-increment)
- Unique constraints from schema are reflected in entity annotations or validation

**Cross-Reference**: Verify against `architecture-handoff.json` → `database_schema.tables[]` for each entity class.

**Common Violations**:
```java
// Major: Type mismatch — BigDecimal for DECIMAL is correct, but Double is wrong
@Column(name = "total_amount")
private Double totalAmount;  // Should be BigDecimal for DECIMAL(10,2)

// Major: Nullable column mapped to primitive (NPE risk on null DB value)
@Column(name = "last_login_at")
private long lastLoginAt;  // Should be Long (nullable) since column is nullable

// Pass: Nullable column mapped to wrapper type
@Column(name = "last_login_at")
private LocalDateTime lastLoginAt;  // Object type, nullable

// Minor: Missing @Table annotation (defaults to class name, which may not match)
@Entity
public class UserEntity {  // Table is "user", not "user_entity"
    // Should be: @Table(name = "user")
}
```

**Severity Guide**:
- Type mismatch (DECIMAL ↔ Double) → Major (precision loss)
- Nullable column mapped to primitive → Major (NPE risk)
- Table/column name mismatch → Major (runtime SQL error)
- Missing FK relationship annotation → Minor (functional but loses navigation)
- Missing @GeneratedValue → Major (insert fails — no ID generation)

---

## Check Item 5: Database Resource Cleanup

**Focus**: Whether all database resources (Connection, PreparedStatement, ResultSet) are properly closed, even in error scenarios.

**Pass Criteria**:
- `ResultSet` is closed after use (via try-with-resources or finally)
- `PreparedStatement` is closed after use (via try-with-resources or finally)
- `Connection` is returned to pool after use (via try-with-resources or finally)
- No resource leaks in error paths — exceptions do not skip cleanup code
- `ResultSet` and `Statement` do not remain open across method boundaries

**Common Violations**:
```java
// CRITICAL: ResultSet never closed
public User findByUsername(String username) {
    PreparedStatement stmt = conn.prepareStatement("SELECT * FROM user WHERE username = ?");
    stmt.setString(1, username);
    ResultSet rs = stmt.executeQuery();  // rs never closed!
    if (rs.next()) {
        return mapUser(rs);
    }
    return null;
    // stmt never closed either!
}

// Pass: All resources in try-with-resources
public User findByUsername(String username) throws SQLException {
    String sql = "SELECT * FROM user WHERE username = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, username);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return mapUser(rs);
            }
        }
    }
    return null;
}
```

**Severity Guide**:
- ResultSet or Statement never closed → Critical (cursor leak, memory leak)
- Connection not returned to pool → Critical (pool exhaustion)
- Resource closed in happy path but not error path → Major
- Resource passed across method boundary → Minor (unclear ownership)

---

## Check Item 6: Migration File Compliance

**Focus**: Whether database migration files follow the conventions defined by `javafx-architect`'s `database-design.md`.

**Pass Criteria**:
- Migration files follow naming convention: `V{version}__{description}.sql` (Flyway) or proper changeset IDs (Liquibase)
- Migration files are forward-only (no "undo" migrations in production paths)
- Each migration file contains one logical change (not multiple unrelated changes)
- Migration files use `IF NOT EXISTS` / `IF EXISTS` guards for idempotency
- No migration file has been modified after being applied to any environment
- Migration file path matches `architecture-handoff.json` → `database_schema.migration_path`

**Cross-Reference**: Verify against `architecture-handoff.json` → `database_schema.migration_path` and `database_schema.migration_tool`.

**Severity Guide**:
- Modified migration file after deployment → Critical (migration history corruption)
- Non-sequential version numbers → Major (Flyway will reject)
- Multiple unrelated changes in one file → Minor (maintainability issue)
- Missing IF NOT EXISTS guard → Minor (idempotency risk)
- Migration file outside expected path → Major

---

## Check Item 7: Schema-to-Code Consistency

**Focus**: Whether the actual database schema (as defined in migrations) matches the schema specified in `architecture-handoff.json` → `database_schema`.

**Pass Criteria**:
- All tables in `database_schema.tables[]` have corresponding migration files
- Column definitions in migrations match `database_schema` (name, type, nullable, constraints)
- Indexes specified in `database_schema.tables[].indexes[]` are created in migrations
- Foreign keys specified in `database_schema.tables[].foreign_keys[]` are created in migrations
- No "drift" — no tables/columns in migrations that are not in the schema definition (or they are documented as additional implementation details)

**Cross-Reference**: Compare migration SQL files against `architecture-handoff.json` → `database_schema`.

**Severity Guide**:
- Table in schema but no migration → Critical (table won't exist)
- Column type mismatch between schema and migration → Major
- Missing index specified in schema → Major (performance regression)
- Extra table in migration not in schema → Minor (undocumented addition)
- Missing FK constraint specified in schema → Major (data integrity risk)

---

## Cross-References

- `../javafx-architect/references/database-design.md` -- Schema design conventions, ER diagrams, migration planning
- `security-checklist.md` -- SQL injection (cross-reference for application-level input validation)
- `../javafx-developer/references/architecture-patterns.md` -- Repository pattern, DAO layering
- `../javafx-developer/references/networking-retrofit.md` -- API layer (cross-reference for remote data access vs local DB)
