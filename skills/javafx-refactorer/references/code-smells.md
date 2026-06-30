# Code Smells Reference

This document is the authoritative catalog of code smells detected by the `javafx-refactorer` skill. It defines standard Fowler smells, JavaFX-specific smells, quantitative detection heuristics, severity classification, and the end-to-end detection workflow. Output from these heuristics feeds the `smell_catalog[]` array in `refactor-handoff.json`.

---

## 1. Code Smell Catalog

The following table catalogs 12 standard code smells. Categories follow Fowler/Beck: **Bloaters** (over-sized constructs), **Object-Oriented Abusers** (misuse of OO), **Change Preventers** (coupling that blocks change), **Dispensables** (pointless code), and **Couplers** (excessive inter-class coupling).

| # | Name | Category | Definition | Detection Heuristic | JavaFX Example | Severity |
|---|------|----------|------------|---------------------|----------------|----------|
| 1 | God Class | Bloaters | Class knows/does too much, low cohesion | lines > 500 OR methods > 20 OR fields > 15 OR LCOM > 0.8 | `UserController` with 850 lines, 35 methods, 22 fields, handles CRUD + validation + PDF export | Critical |
| 2 | Long Method | Bloaters | Method does too much, hard to follow | lines > 50 OR cyclomatic complexity > 10 | `handleSave()` spanning 120 lines mixing validation, DB write, and UI refresh | Major |
| 3 | Long Parameter List | Bloaters | Method takes too many params, hard to call/test | parameter count > 5 | `createUser(String, String, int, String, boolean, LocalDate)` (6 params) | Minor |
| 4 | Duplicated Code | Dispensables | Identical/similar blocks across locations | token-similar lines > 6 (> 20 => Critical) | identical validation block copy-pasted in `UserController` and `OrderController` | Major |
| 5 | Circular Dependency | Change Preventers | Packages/classes form an import cycle | cycle in directed import graph | `controller` imports `service`, `service` imports `controller` | Critical |
| 6 | Feature Envy | Couplers | Method uses more of another class than its own | foreign-field accesses > own-field accesses | `ReportService.format()` reads 6 fields of `User` but none of `ReportService` | Major |
| 7 | Data Class | Dispensables | Only fields + getters/setters, no behavior | 0 methods besides accessors | `UserDTO` with 10 fields and only getters/setters | Minor |
| 8 | Shotgun Surgery | Change Preventers | One logical change spreads across many files | one change => files modified > 5 | adding a `phone` field touches 7 files (entity, DTO, controller, FXML, CSS, service, DAO) | Major |
| 9 | Primitive Obsession | Dispensables | Primitives where value objects belong | primitives > 40% of fields with missing domain types | using `String` for `email`, `phoneNumber`, `postalCode` instead of value objects | Minor |
| 10 | Dead Code | Dispensables | Unreachable/unused code | unreachable methods, unused fields/imports | `legacyExport()` never called; `oldDateFormat` import unused | Minor |
| 11 | Inappropriate Intimacy | Couplers | Class pokes at another's internals | direct access to private/protected fields of another class | `OrderController` reaching into `UserPane`'s `private TextField nameField` | Major |
| 12 | Message Chain | Couplers | `a.getB().getC().getD()` Law of Demeter violation | chain length > 2 (consecutive calls on returned objects) | `app.getMainWindow().getUserPane().getNameField().setText(...)` | Major |

> **Threshold semantics**: "OR" thresholds trigger when ANY condition is met; combined metrics (e.g., God Class with both size and LCOM) escalate severity. LCOM and cyclomatic complexity are computed as defined in Section 3.

### Representative Examples

**God Class** (Bloaters, Critical):
```java
// God Class — CRUD + validation + export + UI all in one
public class UserController {
    @FXML private TableView<User> table;
    private final UserRepository repo = new UserRepository();
    // ... 22 fields total ...
    public void saveUser(User u) { /* validate + persist + refresh table */ }
    public void exportPdf(List<User> users) { /* 80 lines of PDF logic */ }
    // ... 35 methods total, 850 lines, LCOM 0.82 ...
}
```

**Long Method** (Bloaters, Major) -> fix: Extract Method:
```java
// BEFORE: 120-line handler mixing concerns, CC 14
@FXML
private void handleSave() {
    // validate (30 lines) ... persist (40 lines) ... refresh UI (50 lines) ...
}

// AFTER: each sub-task is a named method; handleSave becomes a readable outline
@FXML
private void handleSave() {
    ValidationResult vr = validateInput();
    if (!vr.isValid()) { showError(vr.getMessage()); return; }
    User saved = userRepository.save(buildUserFromInput());
    refreshTable(saved);
}
```

**Message Chain** (Couplers, Major) -> fix: Hide Delegate / Law of Demeter:
```java
// BEFORE: chain length 4 — brittle to intermediate structure changes
app.getMainWindow().getUserPane().getNameField().setText(name);

// AFTER: delegate through a purposeful method
app.setUserName(name);
```

---

## 2. JavaFX-Specific Code Smells

Beyond standard Fowler smells, JavaFX projects exhibit UI-framework-specific smells. These are first-class entries in the smell catalog, using descriptive names without a prefix (e.g., `ui_logic_in_controller`, `blocking_ui_thread`).

| # | Smell | Definition | Detection Heuristic | Example | Fix Recommendation |
|---|-------|------------|---------------------|---------|--------------------|
| 1 | UI Logic in Controller | Controller contains business logic instead of delegating to a service | controller method contains DB/I/O calls OR > 3 business-decision branches | `handleSave()` writes directly to JDBC inside the controller | Move logic to a `Service`; controller only wires UI to the service |
| 2 | FXML God File | FXML file too large with deeply nested containment | FXML lines > 300 OR nesting depth > 6 | `main-view.fxml` with 450 lines and 8-level `VBox>ScrollPane>VBox>HBox>...` | Split into `fx:include` sub-FXML files per view region |
| 3 | CSS Class Explosion | Too many CSS classes in one stylesheet | distinct `.class` selectors > 50 in one `.css` | `app.css` defining 72 unrelated style classes | Split by module/scene; reuse classes; adopt BEM-style naming |
| 4 | Tight UI Coupling | Controller directly references UI controls of another controller | controller references another controller's `@FXML` controls | `Dashboard` reaching into `UserPane.tableView` | Communicate via a shared ViewModel or event bus, not control refs |
| 5 | Blocking UI Thread | Long task runs on JavaFX Application Thread | I/O/DB/`Thread.sleep` inside an event handler or `initialize()` | `handleLoad()` doing `repo.findAll()` on the FX thread (UI freezes) | Wrap in a `Task` + `ExecutorService`; update UI via `updateValue` |
| 6 | Property Misuse | Using `SimpleStringProperty` for non-UI business state | `SimpleXxxProperty` field in a non-UI model/service class | `User` model storing `name` as `SimpleStringProperty` instead of `String` | Use plain fields in the model; expose Properties only in the ViewModel |
| 7 | Listener Leak | Listeners added but never removed in long-lived apps | `addListener` without a matching `removeListener` in a long-lived node | `tableView` adding a `ChangeListener` per refresh without removal | Store the listener reference; remove it on `dispose()`/view teardown |

### Blocking UI Thread Example

```java
// BAD: DB call on the FX Application Thread freezes the UI
@FXML
private void handleLoad() {
    List<User> users = userRepository.findAll(); // blocks UI thread
    table.getItems().setAll(users);
}

// GOOD: offload to a background Task
@FXML
private void handleLoad() {
    Task<List<User>> task = new Task<>() {
        @Override protected List<User> call() { return userRepository.findAll(); }
    };
    task.setOnSucceeded(e -> table.getItems().setAll(task.getValue()));
    new Thread(task).start();
}
```

---

## 3. Detection Heuristics Detail

Quantitative heuristics keep smell detection objective and reproducible across runs.

### 3.1 Line Count Metrics
- Count only **physical source lines** of statements and declarations.
- **Exclude**: blank lines, single/multi-line comments (`//`, `/* */`), and Javadoc.
- Class line count spans from the class declaration `class X {` to its closing `}` (nested classes count toward the enclosing class).
- Method line count spans from the method signature to its closing `}`.

### 3.2 Cyclomatic Complexity (CC)
- Formula: `CC = decision_points + 1`.
- Decision points: `if`, `else if`, `for`, `while`, `do`, `case` labels, `catch`, `&&`, `||`, ternary `?:`, and each `switch` arm.
- A straight-line method with no decisions has `CC = 1`.
- Threshold: `CC > 10` flags Long Method; `CC > 20` escalates to Critical.

### 3.3 LCOM (Lack of Cohesion in Methods)
- LCOM (Chidamber-Kemerer): for a class with `m` methods, consider all method pairs.
  - `P` = number of method pairs that share **no** fields.
  - `Q` = number of method pairs that share **at least one** field.
  - `LCOM = max(0, P - Q)`.
- Normalize to `[0,1]`: `LCOM_norm = LCOM / (m * (m - 1) / 2)` (total method pairs).
- Interpretation: high LCOM = low cohesion (methods cluster on disjoint fields).
- Threshold: `LCOM_norm > 0.8` indicates a God Class candidate.

### 3.4 Token-Based Similarity for Duplicate Detection
1. **Normalize**: collapse whitespace, strip comments, optionally lowercase identifiers.
2. **Tokenize**: split into a token stream (keywords, identifiers, operators, literals).
3. **Window**: slide an N-line window (default 6 lines); hash each window (Rabin-Karp / winnowing).
4. **Compare**: two windows are duplicates if token-sequence equality holds, or similarity ratio > 0.85.
5. **Merge** adjacent duplicate windows into a single block; report block start/end lines and files.
- Threshold: block > 6 lines => Major; block > 20 lines => Critical.

### 3.5 Import Graph Analysis for Circular Dependencies
1. **Build a directed graph** `G`: nodes = classes (or packages); edge `A -> B` if `A` imports `B` (resolved to a project class, not a library).
2. **Detect cycles via DFS** with a recursion stack (white/gray/black coloring). Revisiting a gray node => back edge => cycle.
3. **Tarjan's SCC** algorithm yields all strongly-connected components; any SCC of size > 1 (or a self-loop) is a circular dependency.
4. **Report** the participating nodes and the minimal edge set whose removal breaks the cycle (candidate for Move Class / Introduce Interface).

**Circular Dependency Example**:
```
// controller/UserController.java   -> imports service.UserService
// service/UserService.java         -> imports controller.UserController  (CYCLE)
//
// Detection: DFS coloring revisits gray node UserService => back edge.
// Fix: introduce interface IUserCallback in a shared package;
//      UserService depends on the interface, breaking the concrete cycle.
```

### Metric Collection Summary

| Metric | Scope | Granularity | Used By Smell |
|--------|-------|-------------|---------------|
| Lines (excl. comments/blank) | class, method | count | God Class, Long Method, FXML God File |
| Method count | class | count | God Class |
| Field count | class | count | God Class, Primitive Obsession |
| Cyclomatic Complexity | method | int | Long Method |
| LCOM_norm | class | [0,1] | God Class |
| Parameter count | method | int | Long Parameter List |
| Field-access ratio (foreign vs own) | method | ratio | Feature Envy |
| Token-similarity ratio | block | [0,1] | Duplicated Code |
| Import edges | project | graph | Circular Dependency |
| `.class` selectors | stylesheet | count | CSS Class Explosion |
| Listener add/remove delta | node | count | Listener Leak |

---

## 4. Severity Classification Framework

| Severity | Criteria | Action Required |
|----------|----------|-----------------|
| Critical | Blocks features or causes bugs/runtime failures; must fix before delivery | Immediate; before any new feature work |
| Major | Slows development, raises change cost; fix in current sprint | Current sprint |
| Minor | Reduces readability/maintainability; fix opportunistically | Opportunistic; during touch/review |

### Severity Matrix (smell -> default severity by threshold range)

| Smell | Minor | Major | Critical |
|-------|-------|-------|----------|
| God Class | — | 500-1000 lines / 20-30 methods | > 1000 lines / > 30 methods / LCOM > 0.9 |
| Long Method | — | 51-100 lines / CC 11-20 | > 100 lines / CC > 20 |
| Long Parameter List | 6-7 params | 8-10 params | > 10 params |
| Duplicated Code | — | 7-20 lines similar | > 20 lines similar |
| Circular Dependency | — | — | any cycle |
| Feature Envy | — | foreign > own accesses | — |
| Data Class | always | — | — |
| Shotgun Surgery | — | 6-8 files | > 8 files |
| Primitive Obsession | always | — | — |
| Dead Code | always | — | — |
| Inappropriate Intimacy | — | private read access | private write access |
| Message Chain | length 3 | length 4 | length > 4 |
| UI Logic in Controller | — | DB/I/O in handler | — |
| FXML God File | — | 300-500 lines | > 500 lines |
| CSS Class Explosion | 51-80 classes | > 80 classes | — |
| Tight UI Coupling | — | cross-controller control ref | — |
| Blocking UI Thread | — | — | any blocking I/O on FX thread |
| Property Misuse | always | — | — |
| Listener Leak | — | — | leak in long-lived view |

> Severity may be **escalated/de-escalated** by context (e.g., a God Class in a long-lived main window stays Critical; a duplicated block in a short-lived dialog may be de-escalated to Minor).

---

## 5. Detection Workflow

The detection process runs as a deterministic pipeline producing the `smell_catalog[]`.

1. **Scan files**: Walk the refactoring scope (`src/main/java`, plus `*.fxml`/`*.css` for JavaFX smells). Build an AST per Java file and a parse tree per FXML/CSS file.
2. **Collect metrics**: Per class record lines, methods, fields, LCOM; per method record lines and CC; build the project-wide import graph.
3. **Apply heuristics**: Evaluate each smell's detection heuristic (Section 1 & 2 tables) against the metrics. Token-based duplicate detection and DFS cycle detection run as separate passes.
4. **Classify severity**: Map each detected smell to a severity using the Section 4 matrix, then apply contextual escalation/de-escalation.
5. **Record AST signatures**: For each smell, capture `file`, `lines` range, and `ast_node_signature` (fully-qualified class + method signature, e.g. `com.example.controller.UserController#saveUser(User):void`) for refactor-resistant matching by `javafx-developer`.
6. **Generate smell catalog**: Emit the structured `smell_catalog[]` with `smell_id`, `type`, `severity`, `file`, `lines`, `ast_node_signature`, `detail`, and `recommended_refactoring` id. This catalog feeds Step 3 (Refactoring Recommendations) and Step 4 (Technical Debt Management) of the refactorer workflow.

---

## Reference Documents

- `refactoring-patterns.md` — maps each smell to a refactoring action (Extract Class, Extract Method, Move Method, etc.) with before/after examples and safe sequencing rules.
- `tech-debt.md` — converts this smell catalog into a prioritized, effort-estimated technical debt inventory and phased repayment plan.
