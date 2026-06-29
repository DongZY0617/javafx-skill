# Code Structure Review Standards

This document is the criteria for the "Code Structure" dimension, governing 5 check items (corresponding to design spec section 3.1). It reviews the architectural layering, responsibility division, package structure, module configuration, and dependency direction of JavaFX code. Default severity baseline: Major. Shares the same origin as `javafx-developer`'s `architecture-patterns.md`.

---

## Check Item 1: Architecture Pattern Compliance

**Focus**: Whether MVC / MVVM / MVP layering is clear, whether the View layer mixes in business logic, whether Controllers only handle UI events.

**Pass Criteria**:
- A clear architecture pattern (MVC / MVVM / MVP) is adopted, with well-defined responsibilities for each layer
- Controllers only handle UI events and view state orchestration, without business rules, data access, or validation logic
- Views (FXML) are purely declarative, containing no business logic or scripts
- Business logic is delegated to the Service layer, data access is delegated to the Repository / DAO layer

**Fail Criteria** (any one constitutes failure):
- Controller directly contains database access (JDBC / JPA / MyBatis calls)
- Controller contains complex business rule computation (should be in the Service layer)
- FXML embeds business logic via `<fx:script>`
- View layer directly manipulates Model persistence methods

**Severity Baseline**: Major
- De-escalation condition: Only individual methods cross layers, does not affect overall architecture → Minor
- Escalation condition: Prevents independent testing or multiple circular dependencies → Critical

**Bad Example**:
```java
// Controller directly accessing database, bypassing Service layer
@FXML
private void handleLoad() {
    try (Connection conn = DriverManager.getConnection("jdbc:sqlite:app.db")) {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM users");
        while (rs.next()) {
            users.add(new User(rs.getString("name")));
        }
    } catch (SQLException e) { e.printStackTrace(); }
}
```

**Good Example**:
```java
// Controller only delegates to Service
@FXML
private void handleLoad() {
    List<User> loaded = userService.loadAll();  // Delegate to Service layer
    users.setAll(loaded);
}
```

---

## Check Item 2: Single Responsibility

**Focus**: Whether a Controller bears too many responsibilities (God class), whether the Service layer is properly delegated to.

**Pass Criteria**:
- Each Controller has a single responsibility, corresponding to one view or a group of closely related views
- Controller line count is moderate (recommended < 400 lines), no God Controller
- The Service layer is properly delegated to, carrying business logic and transaction boundaries
- Each class follows the single responsibility principle, with only one reason to change

**Fail Criteria** (any one constitutes failure):
- A single Controller manages multiple unrelated functional modules (e.g., simultaneously managing users, orders, settings)
- Controller line count is too large (> 500 lines) with mixed responsibilities
- Missing Service layer, Controller directly accesses Repository / DAO
- God Controller exists (an all-in-one class managing all functionality)

**Severity Baseline**: Major
- De-escalation condition: Only individual methods cross layers, does not affect overall architecture → Minor

**Bad Example**:
```java
// God Controller: a single class managing all functionality
public class MainController {
    @FXML private void handleUser() { /* user management */ }
    @FXML private void handleOrder() { /* order management */ }
    @FXML private void handleSettings() { /* system settings */ }
    @FXML private void handleReport() { /* report generation */ }
    // ... over 1000 lines
}
```

**Good Example**:
```java
// Split into multiple Controllers by function, each delegating to its corresponding Service
public class UserController { /* user management only */ }
public class OrderController { /* order management only */ }
```

---

## Check Item 3: Package Structure Conventions

**Focus**: Whether `model / view / controller / viewmodel / service` layering is consistent, whether package paths match directory structure.

**Pass Criteria**:
- Package structure is organized by responsibility, e.g., `com.example.app.model`, `com.example.app.view`, `com.example.app.controller`, `com.example.app.service`
- Package paths correspond one-to-one with physical directory structure
- Same-type files are placed in the same package (all Controllers in the controller package, all Models in the model package)
- Package names use all lowercase, no underscores or special characters

**Fail Criteria** (any one constitutes failure):
- Package structure is disorganized (Controllers and Models placed in the same package)
- Package paths do not match directory structure
- Missing layering, all classes piled in the default package or a single package
- Package naming is non-standard (contains uppercase letters, underscores)

**Severity Baseline**: Major
- De-escalation condition: Only individual files placed in the wrong package, overall structure is clear → Minor

**Good Example**:
```
src/main/java/com/example/app/
├── model/          # Data models
│   └── User.java
├── view/           # FXML views
│   └── user-view.fxml
├── controller/     # Controllers
│   └── UserController.java
├── viewmodel/      # ViewModels (MVVM pattern)
│   └── UserViewModel.java
└── service/        # Business services
    └── UserService.java
```

---

## Check Item 4: Module Configuration

**Focus**: Whether `module-info.java` `requires` / `exports` / `opens` are complete and correct.

**Pass Criteria**:
- `module-info.java` declares all required `requires` (javafx.controls, javafx.fxml, etc.)
- `exports` correctly exports public API packages
- Packages requiring reflection access are exposed to the corresponding modules via `opens`
- In particular: when using `PropertyValueFactory` for reflection access to Model properties, `opens model to javafx.controls` is required

**Fail Criteria** (any one constitutes failure):
- Missing `requires javafx.fxml`, causing FXMLLoader to fail
- Using `PropertyValueFactory` but not `opens`-ing the model package to `javafx.controls`, causing runtime reflection failure
- `exports` over-exposes internal implementation packages (e.g., exporting the controller package)
- Missing `module-info.java` but the project uses modular builds

**Severity Baseline**: Major

**Bad Example**:
```java
// Missing opens, PropertyValueFactory reflection will fail
module com.example.app {
    requires javafx.controls;
    requires javafx.fxml;
    exports com.example.app.model;  // exports alone is not enough, reflection requires opens
}
```

**Good Example**:
```java
// Correctly opens model package to javafx.controls to support reflection
module com.example.app {
    requires javafx.controls;
    requires javafx.fxml;
    exports com.example.app.model;
    opens com.example.app.model to javafx.controls;  // PropertyValueFactory reflection requires this
    opens com.example.app.controller to javafx.fxml; // FXMLLoader reflection requires this
}
```

> **Runtime Verification Required**
> - Static analysis detects missing `opens` as a warning; runtime crashes with `LoadException` / `IllegalAccessException`
> - Runner check: `runtime-verification.md` #5 (Module Runtime) — dynamically catches reflection access failures
> - If static result is a warning only, trigger runner to confirm whether runtime crash occurs
> - Runner finding supersedes static heuristic when conflicting

---

## Check Item 5: Dependency Direction

**Focus**: Whether circular dependencies exist, whether the View layer reversely depends on Controller implementation details.

**Pass Criteria**:
- Dependency direction is unidirectional: View → Controller → Service → Repository → Model
- No circular dependencies (A depends on B, B depends on A)
- Controllers depend on Service interfaces rather than concrete implementations (dependency inversion)
- The View layer (FXML) does not directly reference Controller internal methods, only binding through `fx:controller` and `onAction`

**Fail Criteria** (any one constitutes failure):
- Circular dependencies exist (Controller A depends on Controller B, B depends on A)
- Controller directly `new`s concrete dependency classes, cannot be replaced or tested
- View layer reversely depends on Controller implementation details
- Service layer reversely depends on Controller (business layer should not know about the UI layer)

**Severity Baseline**: Major
- De-escalation condition: Only individual methods cross layers, does not affect overall architecture → Minor
- Escalation condition: Multiple circular dependencies preventing independent testing → Critical

**Bad Example**:
```java
// Tight coupling: Controller directly new's concrete implementation, with circular dependency
public class OrderController {
    private MySQLDatabase db = new MySQLDatabase();  // Hardcoded dependency
    private ReportController reportCtrl;             // Inter-Controller circular dependency
}
```

**Good Example**:
```java
// Dependency inversion: via interface + constructor injection
public class OrderController {
    private final Database db;           // Depends on interface
    private final OrderService service;  // Depends on Service interface

    public OrderController(Database db, OrderService service) {
        this.db = db;
        this.service = service;
    }
}
```
