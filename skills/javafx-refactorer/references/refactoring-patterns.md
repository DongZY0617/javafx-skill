# Refactoring Patterns Reference

This document is the authoritative catalog of refactoring patterns applied by the `javafx-refactorer` skill. It maps each detected code smell (see `code-smells.md`) to a concrete refactoring action with before/after examples, effort/risk estimates, safe sequencing rules, and a validation checklist. Output from these patterns feeds the `refactoring_plan[]` array in `refactor-handoff.json`.

---

## 1. Refactoring Pattern Catalog

The following table catalogs 12 standard refactoring patterns. Each pattern resolves one or more code smells and carries an effort estimate (S/M/L) and risk level (Low/Medium/High) per the definitions in Section 4.

| # | Pattern | Smell Resolved | Description | Effort | Risk |
|---|---------|---------------|-------------|--------|------|
| 1 | Extract Method | Long Method | Identify a sub-task block inside a long method, create a named method, pass parameters and return values | S | Low |
| 2 | Extract Class | God Class | Identify a cohesive group of fields/methods, create a new class, move them, delegate from the original | M | Medium |
| 3 | Move Method | Feature Envy | Move the envious method to the class whose fields it uses most, update all call sites | M | Medium |
| 4 | Move Class | Circular Dependency | Relocate a class to a different package to break an import cycle | M | Medium |
| 5 | Extract Interface | Circular Dependency | Introduce an interface in a shared package; dependents depend on the interface, not the concrete class | M | Medium |
| 6 | Pull Up Method | Duplicated Code | Move an identical method from sibling subclasses to their common parent class | S | Low |
| 7 | Extract Method Object | Long Method (many locals) | Convert a long method with many local variables into a class whose fields hold those locals | L | Medium |
| 8 | Introduce Parameter Object | Long Parameter List | Group repeated parameter clusters into a single value object passed by reference | M | Low |
| 9 | Replace Conditional with Polymorphism | Long if-else / switch | Replace a type-switch with a strategy/subtype per branch; dispatch via polymorphism | L | High |
| 10 | Encapsulate Field | Data Class | Replace a public/raw field with a property accessor (JavaFX Property or getter/setter) | S | Low |
| 11 | Remove Dead Code | Dead Code | Delete unreachable methods, unused fields, and unused imports after confirming zero references | S | Low |
| 12 | Replace Inheritance with Delegation | Inappropriate Inheritance | Replace a subclass relationship with a held reference (composition over inheritance) | L | High |

### Before/After Code Examples

**1. Extract Method** (Long Method, S, Low):
```java
// BEFORE: validation, persistence, and UI refresh all inline
public void saveUser(User u) {
    if (u.getName() == null || u.getName().isBlank()) { showError("Name required"); return; }
    if (u.getEmail() == null || !u.getEmail().contains("@")) { showError("Invalid email"); return; }
    repo.save(u); table.getItems().add(u);
}
// AFTER: each sub-task is a named method
public void saveUser(User u) {
    if (!validate(u)) return;
    repo.save(u); refreshTable(u);
}
```

**2. Extract Class** (God Class, M, Medium):
```java
// BEFORE: UserController holds validation, persistence, and export
public class UserController { /* 35 methods, 22 fields, 850 lines */ }
// AFTER: cohesive groups split into separate classes
public class UserController { /* delegates to validator + service */ }
public class UserValidator { public ValidationResult validate(User u) { ... } }
public class UserService { public User save(User u) { ... } }
```

**3. Move Method** (Feature Envy, M, Medium):
```java
// BEFORE: ReportService.format() reads 6 fields of User, 0 of its own
public class ReportService { String format(User u) { return u.getName() + " " + u.getEmail(); } }
// AFTER: method moved to the envied class
public class User { String formatForReport() { return name + " " + email; } }
public class ReportService { String format(User u) { return u.formatForReport(); } }
```

**4. Move Class** (Circular Dependency, M, Medium):
```java
// BEFORE: controller pkg <-> service pkg cycle
//   com.app.controller.UserController  -> imports com.app.service.UserService
//   com.app.service.UserService        -> imports com.app.controller.UserController
// AFTER: move UserService into controller-shared package, cycle broken
//   com.app.controller.UserController  -> imports com.app.shared.UserService
//   com.app.shared.UserService         -> (no controller import)
```

**5. Extract Interface** (Circular Dependency, M, Medium):
```java
// BEFORE: UserService imports UserController (concrete callback)
class UserService { void process(UserController cb) { cb.onDone(); } }
// AFTER: depend on an interface in a shared package
package com.app.shared; interface UserCallback { void onDone(); }
class UserService { void process(UserCallback cb) { cb.onDone(); } }
class UserController implements UserCallback { public void onDone() { ... } }
```

**6. Pull Up Method** (Duplicated Code, S, Low):
```java
// BEFORE: identical validate() in AdminController and UserController
class AdminController { boolean validate(User u) { ... } }
class UserController  { boolean validate(User u) { ... } }
// AFTER: moved to shared parent
abstract class BaseController { boolean validate(User u) { ... } }
class AdminController extends BaseController { }
class UserController  extends BaseController { }
```

**7. Extract Method Object** (Long Method w/ many locals, L, Medium):
```java
// BEFORE: 90-line method with 8 local variables, hard to sub-extract
public Invoice computeInvoice(Order o) { /* 8 locals, 90 lines */ }
// AFTER: method becomes a class; locals become fields
class InvoiceCalculator {
    private final Order order; private double subtotal, tax, discount; /* ... */
    Invoice compute() { calcSubtotal(); applyDiscount(); addTax(); return build(); }
}
```

**8. Introduce Parameter Object** (Long Parameter List, M, Low):
```java
// BEFORE: 6 parameters repeated across create/update/import
User createUser(String name, String email, int age, String phone, String role, boolean active);
// AFTER: grouped into a value object
User createUser(UserRequest req);
record UserRequest(String name, String email, int age, String phone, String role, boolean active) {}
```

**9. Replace Conditional with Polymorphism** (Long if-else, L, High):
```java
// BEFORE: switch on discount type
double applyDiscount(Order o) {
    switch (o.getType()) { case "VIP": return o.total() * 0.8; case "BULK": return o.total() * 0.9; default: return o.total(); }
}
// AFTER: strategy per type
interface DiscountStrategy { double apply(Order o); }
class VipDiscount implements DiscountStrategy { public double apply(Order o) { return o.total() * 0.8; } }
double applyDiscount(Order o) { return o.getStrategy().apply(o); }
```

**10. Encapsulate Field** (Data Class, S, Low):
```java
// BEFORE: public mutable field
public class UserDTO { public String name; }
// AFTER: JavaFX Property accessor
public class UserDTO {
    private final StringProperty name = new SimpleStringProperty();
    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String v) { name.set(v); }
}
```

**11. Remove Dead Code** (Dead Code, S, Low):
```java
// BEFORE: legacyExport() never called; unused import
import java.util.logging.Logger;  // unused
public void legacyExport() { /* 40 lines, zero callers */ }
// AFTER: deleted entirely
// (method and import removed; compiler confirms zero references)
```

**12. Replace Inheritance with Delegation** (Inappropriate Inheritance, L, High):
```java
// BEFORE: Stack extends ArrayList (inherits 50+ unneeded methods)
public class StringStack extends ArrayList<String> { void push(String s) { add(s); } }
// AFTER: hold a reference, expose only stack methods
public class StringStack {
    private final List<String> items = new ArrayList<>();
    public void push(String s) { items.add(s); }
    public String pop() { return items.remove(items.size() - 1); }
}
```

---

## 2. JavaFX-Specific Refactoring Patterns

These patterns target smells unique to JavaFX projects (see Section 2 of `code-smells.md`). Each includes a JavaFX-specific before/after example.

### 2.1 Extract Controller Logic to Service

Resolves **UI Logic in Controller**. Moves business logic (DB/I/O/business decisions) out of the FXML controller into a Service class; the controller only wires UI events to service calls.

```java
// BEFORE: JDBC call inside the FX Application Thread, inside controller
@FXML private void handleSave() {
    String name = nameField.getText();
    try (Connection c = DriverManager.getConnection(URL)) {
        PreparedStatement ps = c.prepareStatement("INSERT INTO users(name) VALUES(?)");
        ps.setString(1, name); ps.executeUpdate();
    } catch (SQLException e) { showError(e.getMessage()); }
    refreshTable();
}
// AFTER: controller delegates to an injected service
@FXML private void handleSave() {
    userService.save(nameField.getText());
    refreshTable();
}
// UserService.java — com.example.service
public void save(String name) { repository.insert(new User(name)); }
```

### 2.2 Extract FXML Component

Resolves **FXML God File**. Breaks a large FXML file into smaller `fx:include` components, each with its own controller.

```xml
<!-- BEFORE: main-view.fxml 450 lines, 8-level nesting -->
<VBox>
  <ToolBar> ... </ToolBar>
  <TableView> <!-- 150 lines of columns --> </TableView>
  <HBox> <!-- 120 lines of filter controls --> </HBox>
</VBox>
<!-- AFTER: main-view.fxml delegates to includes -->
<VBox>
  <fx:include source="toolbar.fxml"/>
  <fx:include source="user-table.fxml"/>
  <fx:include source="filter-bar.fxml"/>
</VBox>
```

### 2.3 Replace SwingInterop with Pure JavaFX

Resolves legacy `SwingNode`/`JFXPanel` interop. Migrates embedded Swing components to native JavaFX equivalents, removing the Swing dependency and thread-bridging complexity.

```java
// BEFORE: Swing JTable wrapped in SwingNode
SwingNode swingNode = new SwingNode();
JTable table = new JTable(data, cols);
swingNode.setContent(new JScrollPane(table));
// AFTER: native JavaFX TableView
TableView<User> table = new TableView<>();
TableColumn<User,String> nameCol = new TableColumn<>("Name");
nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
table.getColumns().add(nameCol);
table.setItems(FXCollections.observableArrayList(users));
```

### 2.4 Extract ViewModel from Controller

Resolves **UI Logic in Controller** and **Tight UI Coupling**. Moves UI state and presentation logic from the Controller to a ViewModel, enabling MVVM and testability without FXML.

```java
// BEFORE: controller holds state + logic, untestable without FXML
public class LoginController {
    @FXML private TextField userField, passField;
    @FXML private Label errorLabel;
    @FXML private void handleLogin() {
        if (authService.login(userField.getText(), passField.getText())) { goToMain(); }
        else { errorLabel.setText("Invalid credentials"); }
    }
}
// AFTER: ViewModel holds observable state; controller only binds
public class LoginViewModel {
    private final StringProperty user = new SimpleStringProperty();
    private final StringProperty pass = new SimpleStringProperty();
    private final StringProperty error = new SimpleStringProperty();
    private final AuthService authService;
    public void login() {
        if (!authService.login(user.get(), pass.get())) error.set("Invalid credentials");
    }
    // ... property accessors ...
}
// Controller binds fields to ViewModel properties
```

### 2.5 Replace ObservableList Misuse

Resolves improper collection usage — using a plain `ArrayList` where JavaFX data binding requires `ObservableList`, causing the UI to not auto-update.

```java
// BEFORE: ArrayList — TableView never refreshes on add
private List<User> users = new ArrayList<>();
users.add(newUser); table.getItems().setAll(users); // manual refresh needed
// AFTER: ObservableList — TableView auto-updates
private ObservableList<User> users = FXCollections.observableArrayList();
users.add(newUser); table.setItems(users); // UI updates automatically
```

### 2.6 Extract CSS Theme Variables

Resolves **CSS Class Explosion** and hardcoded color duplication. Replaces scattered hardcoded CSS values with JavaFX CSS variables (`-fx-` custom properties) defined once in a `.root` rule.

```css
/* BEFORE: hardcoded values repeated across 50+ rules */
.button-primary { -fx-background-color: #2563eb; -fx-text-fill: #ffffff; }
.button-danger  { -fx-background-color: #dc2626; -fx-text-fill: #ffffff; }
/* AFTER: variables defined once, referenced everywhere */
.root {
    -fx-primary-color: #2563eb;
    -fx-danger-color:  #dc2626;
    -fx-text-on-color: #ffffff;
}
.button-primary { -fx-background-color: -fx-primary-color; -fx-text-fill: -fx-text-on-color; }
.button-danger  { -fx-background-color: -fx-danger-color;  -fx-text-fill: -fx-text-on-color; }
```

---

## 3. Safe Sequencing Rules

Refactoring actions are not independent — some must precede others to keep the code compilable and tests green at every step.

### 3.1 Dependency Ordering

- **Extract Method before Extract Class**: Extracting methods first makes cohesive groups visible, simplifying the decision of what to move into a new class.
- **Extract Interface before Move Class**: Introducing the interface first lets callers compile against the abstraction; the concrete class can then be relocated without breaking imports.
- **Encapsulate Field before Move Method**: If a moved method accesses raw fields, encapsulate them first so the move only touches accessors.
- **Extract Controller Logic to Service before Extract ViewModel**: Move business logic to a Service first; then extract presentation state into a ViewModel that calls the Service.
- **Remove Dead Code first**: Removing dead code reduces noise and the surface area that later refactorings must analyze.

### 3.2 Independence (Parallel-Safe)

The following groups have no mutual dependency and can be applied in parallel (different files/packages):
- Remove Dead Code (any scope) || Encapsulate Field (different classes) || Extract CSS Theme Variables (CSS only)
- Pull Up Method in hierarchy A || Pull Up Method in hierarchy B (disjoint type hierarchies)

### 3.3 Precondition and Postcondition Checks

| Pattern | Precondition (must be true before) | Postcondition (must verify after) |
|---------|------------------------------------|-----------------------------------|
| Extract Method | Method compiles; local var data-flow understood | All call sites unchanged; tests pass |
| Extract Class | LCOM analysis identifies a cohesive group | Original class compiles with delegation; new class compiles |
| Move Method | Target class has (or can receive) the needed fields | All call sites updated; no new Feature Envy |
| Move Class | Destination package exists; no name clash | Import graph is acyclic; all imports updated |
| Extract Interface | Interface methods are a subset of concrete methods | Dependents compile against interface; cycle broken |
| Pull Up Method | Method bodies are identical across siblings | Subclasses no longer override; parent compiles |
| Extract Method Object | All locals can become fields | Original method delegates to new class; tests pass |
| Introduce Parameter Object | Parameter cluster is stable across call sites | All call sites use the new object; builders available |
| Replace Conditional w/ Polymorphism | Each branch maps to one type | No switch remains; dispatch via subtype; tests pass |
| Encapsulate Field | No external direct field writes remain | All access goes through accessor; bindings intact |
| Remove Dead Code | Compiler + usage analysis confirms zero callers | Compile succeeds; binary size reduced |
| Replace Inheritance w/ Delegation | Delegate holds all needed behavior | No `extends`; only intended methods exposed |

### 3.4 Dependency Matrix

| Pattern A | Relationship | Pattern B | Reason |
|-----------|-------------|-----------|--------|
| Extract Method | must be done BEFORE | Extract Class | Methods must exist as units before moving them |
| Extract Interface | must be done BEFORE | Move Class | Interface lets callers compile during relocation |
| Encapsulate Field | must be done BEFORE | Move Method | Accessors are stable move targets |
| Remove Dead Code | must be done BEFORE | any other | Reduces analysis surface; avoids refactoring dead code |
| Extract Controller→Service | must be done BEFORE | Extract ViewModel | Service must exist for ViewModel to call |
| Extract Class | must be done BEFORE | Replace Inheritance w/ Delegation | Delegation target should be a clean extracted class |
| Extract Method | can run in PARALLEL with | Remove Dead Code (different file) | No shared scope |
| Encapsulate Field | can run in PARALLEL with | Extract CSS Theme Variables | Java vs CSS, disjoint |
| Introduce Parameter Object | must be done BEFORE | Extract Method Object | Reduces params before converting to fields |

---

## 4. Effort and Risk Assessment

### 4.1 Effort Estimation Guide

| Effort | Definition | Typical Patterns |
|--------|-----------|------------------|
| S | < 1 hour; mechanical, localized | Extract Method, Pull Up Method, Encapsulate Field, Remove Dead Code |
| M | 1–4 hours; requires analysis of cohesion/coupling | Extract Class, Move Method, Move Class, Extract Interface, Introduce Parameter Object |
| L | > 4 hours; architectural or multi-file | Extract Method Object, Replace Conditional with Polymorphism, Replace Inheritance with Delegation |

### 4.2 Risk Assessment Criteria

| Risk | Criteria | Typical Patterns |
|------|----------|------------------|
| Low | Mechanical transformation, IDE/tool-supportable, no public API change | Extract Method, Pull Up Method, Encapsulate Field, Remove Dead Code, Introduce Parameter Object |
| Medium | Requires careful coupling/cohesion analysis; touches multiple classes but API stays stable | Extract Class, Move Method, Move Class, Extract Interface, Extract Method Object |
| High | Changes public API, class hierarchy, or runtime dispatch; high blast radius | Replace Conditional with Polymorphism, Replace Inheritance with Delegation |

### 4.3 Risk Mitigation Strategy

1. **Always run tests before and after** — capture a pre-refactor baseline (`mvn test`); every previously-passing test must still pass post-refactor.
2. **Use version control** — commit (or tag) before each refactoring action so any step can be rolled back independently.
3. **Apply incrementally** — one pattern at a time; compile (`mvn compile`) after each action before moving to the next.
4. **Prefer low-risk first** — sequence S/Low patterns before M/Medium and L/High to build confidence and shrink the analysis surface early.
5. **Flag API changes** — any High-risk refactoring that alters public API must be explicitly recorded in `behavior_equivalence_check` in the handoff JSON.

---

## 5. Refactoring Validation Checklist

Every refactoring action in `refactoring_plan[]` must satisfy all of the following before it is handed off to `javafx-developer`:

- [ ] Before/after code snippets provided for each action
- [ ] All call sites identified and an update plan provided (list every file + method)
- [ ] New files listed with proposed package locations (fully-qualified path)
- [ ] Public API changes (if any) explicitly flagged in `behavior_equivalence_check`
- [ ] Behavior equivalence check plan included (signature preservation, call-site integrity, field-access integrity, import-graph acyclicity)
- [ ] Effort estimate (S/M/L) and risk level (Low/Medium/High) provided for each action
- [ ] Dependencies on other refactorings identified (must-do-before / can-run-in-parallel)
- [ ] Pre-refactor test baseline captured (test count, pass/fail)
- [ ] Postcondition checks listed for the specific pattern (see Section 3.3)
- [ ] Safe application order respects the dependency matrix (see Section 3.4)

---

## Reference Documents

- `code-smells.md` — Code smell catalog, detection heuristics, severity classification, JavaFX-specific smells; this document's patterns resolve those smells.
- `tech-debt.md` — Converts the smell catalog and refactoring plan into a prioritized, effort-estimated technical debt inventory and phased repayment plan.
