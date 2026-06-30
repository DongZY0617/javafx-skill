# JavaFX Code Reviewer Evaluation Test Cases

This file defines the acceptance test cases for the `javafx-code-reviewer` skill, used to quantify review output quality. Each case describes the input scenario, case type, covered dimensions, expected issues, and checkable verification standards.

- **Positive samples**: Problematic code that verifies reviewer recall (whether all expected issues are discovered)
- **Negative samples**: Compliant code that verifies reviewer precision (no false positives, zero false positives)
- **Boundary cases**: De-escalation decisions, incremental review, etc., verifying reviewer accuracy under boundary conditions

---

## Case Overview

| ID | Name | Type | Covered Dimensions | Expected Issues |
|----|------|------|--------------------|-----------------|
| 1 | Background thread updating UI | Positive | UI Thread Safety | 1 Critical |
| 2 | FXML fx:id mismatch | Positive | FXML Standards | 1 Major |
| 3 | Listener leak | Positive | Memory Leak Risks | 1 Critical |
| 4 | Inefficient batch update | Positive | Performance | 1 Major |
| 5 | Spring Boot pitfalls | Positive | Deep Compliance Audit | 1 Critical + 1 Major |
| 6 | CSS var() misuse | Positive | Deep Compliance Audit (CSS) | 1 Major |
| 7 | API misuse (nonexistent dispose) | Positive | Deep Compliance Audit (API) | 1 Major |
| 8 | Comprehensive project review | Positive | All dimensions | Multiple issues |
| 9 | Compliant code (negative sample) | Negative | All dimensions | 0 (zero false positives) |
| 10 | Listener de-escalation decision | Boundary | Memory Leak Risks | 1 Major (de-escalated) |
| 11 | Incremental review (CSS only) | Boundary | Targeted dimension | CSS issues only |
| 12 | Fix handoff completeness | Positive | All dimensions | Includes handoff fields |
| 13 | Controller crossing layers to data layer | Positive | Code Structure | 1 Major |
| 14 | Static reference holding Stage | Positive | Memory Leak Risks | 1 Critical (cannot be de-escalated) |
| 15 | Database Access Security Review | Positive | Database Access Security | 1 Critical + 2 Major |
| 16 | Refactoring Verification | Positive | Refactoring Verification | Behavior equivalence verified (no drift) |
| 17 | Static Analysis Findings (missing file) | Boundary | Static Analysis Tool Findings | Dimension 10 skipped (no failure) |

---

## Case 1: Background Thread Updating UI

- **Input**: Code that directly updates UI components from a background thread
  ```java
  new Thread(() -> {
      // Simulate time-consuming operation
      Thread.sleep(2000);
      statusLabel.setText("Done");   // Background thread directly updating UI
  }).start();
  ```
- **Type**: Positive sample
- **Covered Dimension**: UI Thread Safety
- **Expected Finding**: 1 Critical - FX thread update violation
- **Verification Standards**:
  - [ ] Accurately identifies that `setText` is called on a non-FX thread, will throw `IllegalStateException: Not on FX application thread`
  - [ ] Provides `Platform.runLater()` or `Task.updateMessage()` fix
  - [ ] Severity determined as Critical (cannot be de-escalated; runtime will always throw an exception)
  - [ ] Rule reference cites `thread-safety-rules.md - FX Thread Update Rules`
  - [ ] Includes fix handoff fields (target_file / target_lines / fix_type / fix_priority)

---

## Case 2: FXML fx:id Mismatch

- **Input**: `fx:id` declared in FXML does not match `@FXML` fields in the Controller
  ```xml
  <!-- user-view.fxml -->
  <Button fx:id="saveBtn" text="Save" onAction="#handleSave"/>
  <Label fx:id="nameLabel" text="Name"/>
  ```
  ```java
  // UserController.java - Controller only has saveButton, no saveBtn; extra unreferenced nameLabel
  @FXML private Button saveButton;  // Does not match FXML's saveBtn
  ```
- **Type**: Positive sample
- **Covered Dimension**: FXML Standards
- **Expected Finding**: 1 Major - fx:id mismatch (missing/extra fields)
- **Verification Standards**:
  - [ ] Lists each mismatched fx:id item by item (`saveBtn` has no corresponding field in Controller)
  - [ ] Points out that runtime will throw `LoadException`
  - [ ] Provides alignment fix (add `@FXML private Button saveBtn;` in Controller or modify FXML fx:id)
  - [ ] Severity determined as Major (cannot be de-escalated; runtime will always throw LoadException)
  - [ ] Rule reference cites `fxml-standards.md - fx:id Matching Rules`

---

## Case 3: Listener Leak

- **Input**: Controller registers a listener but has no cleanup method
  ```java
  public class DetailController implements Initializable {
      @FXML private Label nameLabel;
      private final ObservableList<String> data = FXCollections.observableArrayList();

      @Override
      public void initialize(URL location, ResourceBundle resources) {
          // Registers listener, but no removeListener or cleanup logic
          data.addListener((ListChangeListener<String>) c -> {
              nameLabel.setText("Data updated: " + c.getList().size());
          });
      }
      // No dispose, no setOnCloseRequest cleanup
  }
  ```
- **Type**: Positive sample
- **Covered Dimension**: Memory Leak Risks
- **Expected Finding**: 1 Critical - listener not removed causing leak
- **Verification Standards**:
  - [ ] Identifies the missing `removeListener()` call
  - [ ] Explains that after view switching, the old Controller cannot be GC'd and continues receiving events
  - [ ] Recommends a custom `dispose()` method triggered via `setOnCloseRequest` or view-switching callback for cleanup
  - [ ] Severity determined as Critical (default baseline)
  - [ ] Rule reference cites `memory-management.md - Listener Removal Rules`

---

## Case 4: Inefficient Batch Update

- **Input**: Looping `add()` to update ObservableList item by item
  ```java
  // Loading 5000 records from database, adding one by one
  List<User> users = userService.loadAllUsers();
  for (User user : users) {
      userList.add(user);  // Loop add, triggers N change events
  }
  ```
- **Type**: Positive sample
- **Covered Dimension**: Performance
- **Expected Finding**: 1 Major - inefficient batch update
- **Verification Standards**:
  - [ ] Identifies that looping `add()` triggers N change events, causing frequent TableView repaints
  - [ ] Recommends using `setAll()` for one-time replacement (triggers 1 change event)
  - [ ] Explains the difference in event count (N times vs 1 time)
  - [ ] Severity determined as Major (data volume >10000 items and executing on FX thread can escalate to Critical)
  - [ ] Rule reference cites `performance-guide.md - Batch Update Rules`

---

## Case 5: Spring Boot Pitfalls

- **Input**: Two typical errors in a Spring Boot integration scenario
  ```java
  // Error 1: Startup class directly extends Application
  @SpringBootApplication
  public class MyApp extends Application {  // Directly extends Application
      @Override
      public void start(Stage stage) { /* ... */ }
      public static void main(String[] args) { launch(args); }
  }
  ```
  ```java
  // Error 2: Controller is singleton but holds @FXML state fields
  @Component
  // Missing @Scope("prototype")
  public class UserController implements Initializable {
      @FXML private TextField nameField;  // Singleton Controller holding state fields
  }
  ```
- **Type**: Positive sample
- **Covered Dimension**: Deep Compliance Audit (Spring Boot pitfalls)
- **Expected Finding**: 1 Critical (startup class extends Application) + 1 Major (Controller missing @Scope)
- **Verification Standards**:
  - [ ] Points out that the startup class directly extending `Application` will cause the "JavaFX runtime components are missing" error
  - [ ] Recommends splitting into `MyApp` (does not extend Application) + `JavaFXApp` (extends Application)
  - [ ] Startup class issue determined as Critical (cannot be de-escalated; causes Spring container initialization exception)
  - [ ] Points out that Controller is missing `@Scope("prototype")`, singleton state pollution
  - [ ] Controller issue determined as Major (holding @FXML state fields, remains Major)
  - [ ] Rule reference cites `compliance-rules.md - Spring Boot Pitfalls`

---

## Case 6: CSS var() Misuse

- **Input**: CSS file uses the Web CSS `var()` function
  ```css
  .root {
      -fx-primary-color: #2196f3;
  }
  .button-primary {
      /* JavaFX CSS does not support var() */
      -fx-background-color: var(-fx-primary-color);
      -fx-background-radius: var(-fx-radius);
  }
  ```
- **Type**: Positive sample
- **Covered Dimension**: Deep Compliance Audit (CSS compliance)
- **Expected Finding**: 1 Major - using unsupported var() syntax
- **Verification Standards**:
  - [ ] Points out that JavaFX CSS does not support the `var()` function (this is a Web CSS feature)
  - [ ] Provides a direct looked-up color reference solution: `-fx-background-color: -fx-primary-color;`
  - [ ] Border radius should use literal numeric values: `-fx-background-radius: 8;`
  - [ ] Severity determined as Major (unsupported syntax, cannot be de-escalated)
  - [ ] Rule reference cites `css-compliance.md - var() Prohibition Rule`

---

## Case 7: API Misuse (Nonexistent dispose)

- **Input**: Code claims to use the `@FXML dispose()` lifecycle method
  ```java
  public class MainController implements Initializable {
      @FXML
      private void dispose() {  // @FXML dispose() does not exist, not a lifecycle method
          model.removeListener(listener);
      }
  }
  ```
- **Type**: Positive sample
- **Covered Dimension**: Deep Compliance Audit (API misuse detection)
- **Expected Finding**: 1 Major - using a nonexistent API
- **Verification Standards**:
  - [ ] Points out that `@FXML dispose()` is not a JavaFX lifecycle method and will not be automatically called by the framework
  - [ ] Recommends a custom `dispose()` method (without @FXML), explicitly triggered via `setOnCloseRequest` or view-switching callback
  - [ ] Severity determined as Major
  - [ ] Rule reference cites `compliance-rules.md - API Misuse Detection`

---

## Case 8: Comprehensive Project Review

- **Input**: A complete JavaFX project (with multiple Controllers, FXML, CSS, module-info), simultaneously containing thread violations, fx:id mismatches, listener leaks, CSS var(), and other issues
- **Type**: Positive sample
- **Covered Dimension**: All dimensions (six dimensions)
- **Expected Finding**: Multiple cross-dimension issues (Critical + Major + Minor mix)
- **Verification Standards**:
  - [ ] Outputs a complete review report with three parts: review summary, issue list, compliance summary table
  - [ ] Issue list sorted in descending severity order (Critical → Major → Minor → Info)
  - [ ] Multiple manifestations caused by the same root cause merged into one issue (deduplication)
  - [ ] Compliance summary table lists each dimension's check item count, pass count, fail count, pass rate
  - [ ] Review conclusion is "Fail" (Critical issues exist)
  - [ ] Each issue includes fix handoff fields

---

## Case 9: Compliant Code (Negative Sample)

- **Input**: Fully compliant JavaFX code
  ```java
  // Thread safety: uses Task + Platform.runLater
  // FXML: fx:id and @FXML correspond one-to-one
  // Memory: listeners removed in dispose(), Bindings disposed
  // CSS: uses direct looked-up color references, no var(), border radius uses literals
  // Naming: PascalCase / camelCase / SCREAMING_SNAKE_CASE compliant
  ```
- **Type**: Negative sample
- **Covered Dimension**: All dimensions
- **Expected Finding**: 0 issues
- **Verification Standards**:
  - [ ] Issue list is empty
  - [ ] Review conclusion is "Pass"
  - [ ] Compliance summary table pass rate 100%
  - [ ] No false positives (zero false positives)

---

## Case 10: Listener De-escalation Decision

- **Input**: Controller registers a listener, but the listener object's lifecycle is the same as the Controller (co-terminus)
  ```java
  public class ListController implements Initializable {
      private final ObservableList<String> items = FXCollections.observableArrayList();
      private final ListChangeListener<String> listener = c -> updateCount();

      @Override
      public void initialize(URL location, ResourceBundle resources) {
          // items is a private field of the Controller, destroyed together with the Controller
          items.addListener(listener);
      }
  }
  ```
- **Type**: Boundary case
- **Covered Dimension**: Memory Leak Risks
- **Expected Finding**: 1 Major (de-escalated from Critical)
- **Verification Standards**:
  - [ ] Identifies that the listener object (`items`) lifecycle is the same as the Controller
  - [ ] Severity determined as Major rather than Critical (triggers de-escalation condition)
  - [ ] Report "Escalation/De-escalation Note" field records de-escalation condition: "Listener object lifecycle is the same as the Controller (co-terminus), de-escalated to Major"
  - [ ] Rule reference cites `memory-management.md - Listener Removal Rules` + SKILL.md escalation/de-escalation conditions table

---

## Case 11: Incremental Review (CSS Only)

- **Input**: Only CSS files were modified (e.g., adding a `.button-primary` style class), user requests incremental review
- **Type**: Boundary case
- **Covered Dimension**: Targeted dimension (FXML standards + CSS compliance)
- **Expected Finding**: Only CSS-related issues (if any), no thread safety / memory leak issues
- **Verification Standards**:
  - [ ] Skips thread safety, memory leak, performance, code structure dimensions
  - [ ] Only executes FXML standards (styleClass consistency check) + CSS compliance scan
  - [ ] Report header annotates review mode as "Incremental Review"
  - [ ] Report "Review Scope" field lists the actual CSS files reviewed
  - [ ] Does not report issues unrelated to CSS

---

## Case 12: Fix Handoff Completeness

- **Input**: Code with multiple issues (thread violation + fx:id mismatch + wildcard imports)
- **Type**: Positive sample
- **Covered Dimension**: All dimensions
- **Expected Finding**: Each issue includes complete fix handoff fields
- **Verification Standards**:
  - [ ] Each issue includes `target_file` (file path)
  - [ ] Each issue includes `target_lines` (start and end line numbers)
  - [ ] Each issue includes `fix_type` (replace / insert / delete)
  - [ ] Each issue includes `fix_priority` (priority sorted by severity + location, 1 is highest)
  - [ ] Fix handoff fields can be directly consumed by `javafx-developer` to execute fixes
  - [ ] fix_type=replace includes a "Corrected Example" code snippet

---

## Case 13: Controller Crossing Layers to Data Layer

- **Input**: Controller directly contains JDBC query logic, Service layer is bypassed
  ```java
  public class UserController implements Initializable {
      @FXML private TableView<User> userTable;

      @FXML
      private void handleLoad() {
          // Controller directly accessing database, bypassing Service layer
          try (Connection conn = DriverManager.getConnection("jdbc:sqlite:app.db")) {
              Statement stmt = conn.createStatement();
              ResultSet rs = stmt.executeQuery("SELECT * FROM users");
              while (rs.next()) {
                  users.add(new User(rs.getString("name")));
              }
          } catch (SQLException e) { e.printStackTrace(); }
      }
  }
  ```
- **Type**: Positive sample
- **Covered Dimension**: Code Structure
- **Expected Finding**: 1 Major - disorganized architecture layering (Controller crossing layers)
- **Verification Standards**:
  - [ ] Points out that the Controller crosses layers to access the data layer, violating single responsibility
  - [ ] Recommends delegating data access to the Service layer, Controller only calls `userService.loadAll()`
  - [ ] Severity determined as Major (default baseline; if only individual methods cross layers and do not affect overall architecture, can be de-escalated to Minor)
  - [ ] Rule reference cites `structure-review.md - Architecture Pattern Compliance / Single Responsibility`
  - [ ] Includes fix handoff fields

---

## Case 14: Static Reference Holding Stage

- **Input**: Class with `private static Stage mainStage` holding a UI component reference
  ```java
  public class StageManager {
      // Static field holding Stage reference, preventing GC
      private static Stage mainStage;

      public static void setMainStage(Stage stage) {
          mainStage = stage;
      }
  }
  ```
- **Type**: Positive sample
- **Covered Dimension**: Memory Leak Risks
- **Expected Finding**: 1 Critical - static reference leak (cannot be de-escalated)
- **Verification Standards**:
  - [ ] Severity determined as Critical (cannot be de-escalated)
  - [ ] Points out that the static field holding `Stage` prevents GC; after Stage is closed, it is still retained by the static reference
  - [ ] Recommends changing to an instance field or using `WeakReference` / `ObjectProperty<Stage>`
  - [ ] Rule reference cites `memory-management.md - Static Reference Detection` + SKILL.md escalation/de-escalation conditions table (annotated as cannot be de-escalated)
  - [ ] Includes fix handoff fields

---

## Case 15: Database Access Security Review

- **Input**: A JavaFX project with MyBatis integration containing three database access security issues
  ```java
  // Issue 1: MyBatis mapper uses ${} for user-controlled value (SQL injection risk)
  // UserMapper.xml
  <select id="findByCondition" resultType="User">
      SELECT * FROM users
      WHERE name LIKE '%${keyword}%'   <!-- ${} concatenates raw value — SQL injection -->
      ORDER BY ${sortField}            <!-- dynamic sort field not whitelisted -->
  </select>
  ```
  ```java
  // Issue 2: Connection obtained from pool but not closed in finally block
  public class UserService {
      public User findById(Long id) {
          Connection conn = dataSource.getConnection();  // No try-with-resources, no finally
          Statement stmt = conn.createStatement();
          ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE id = " + id);
          if (rs.next()) { return map(rs); }
          return null;  // Connection never closed on exception path
      }
  }
  ```
  ```java
  // Issue 3: Database transaction spanning the UI thread
  public class UserController implements Initializable {
      @FXML
      private void handleLoad() {
          // @Transactional executed synchronously on JavaFX Application Thread
          userService.saveAll(loadedUsers);  // freezes UI; widens transaction scope
      }
  }
  ```
- **Type**: Positive sample
- **Covered Dimension**: Database Access Security
- **Expected Finding**: 1 Critical (SQL injection) + 2 Major (connection leak, UI-thread transaction)
- **Verification Standards**:
  - [ ] SQL injection detected in `${}` usage — flags both the `keyword` LIKE clause and the un-whitelisted `${sortField}` dynamic sort field
  - [ ] Connection leak detected — points out `Connection` not closed in a `finally` block or try-with-resources; on the exception path the pooled connection leaks until the pool is exhausted
  - [ ] UI-thread transaction boundary violation flagged — points out `@Transactional` / DB I/O executing on the JavaFX Application Thread (freezes UI, widens transaction scope); recommends wrapping in `Task` / `Service` on a background thread with `Platform.runLater()` to return results
  - [ ] Fix handoff with `ast_node_signature` for each issue (e.g., `com.example.service.UserService#findById(Long)`)
  - [ ] Recommendations include MyBatis `#{}` parameterization (and whitelist validation for dynamic sort fields / table names)

---

## Case 16: Refactoring Verification

- **Input**: A JavaFX project that was recently refactored (extract method applied to a long method). `.loop-state.json` shows `refactor_result.triggered: true` and the developer has applied changes with `status: "reviewing_and_verifying"`. The `refactor-handoff.json` records the applied refactoring with a `behavior_equivalence_check` block (`method_signatures_preserved`, `call_sites_to_update`, `new_public_api`).
- **Type**: Positive sample
- **Covered Dimension**: Refactoring Verification
- **Expected Finding**: Behavior equivalence verified — no behavior drift detected (0 issues, or issues only where intentional API changes were declared in the handoff)
- **Verification Standards**:
  - [ ] `refactor-handoff.json` consumed — the reviewer loads the handoff to know which refactorings were applied and what the expected API changes are
  - [ ] `method_signatures_preserved` verified — public API signatures unchanged unless the refactoring intentionally changed the API (flagged `method_signatures_preserved: false` in the handoff)
  - [ ] `call_sites_to_update` all updated — no dangling references to old method names or old class locations
  - [ ] `new_public_api` documented — newly introduced public APIs from the refactoring are present and consistent with the handoff
  - [ ] No behavior drift detected — previously-passing tests still pass, side-effect ordering preserved, no new circular dependencies introduced

---

## Case 17: Static Analysis Tool Findings Consumption — Missing File Boundary

- **Input**: A JavaFX project under review where `target/static-analysis-findings.json` is missing (the runner did not produce it — e.g., compile failed before static analysis, or `.loop-config.json` has `static_analysis: false`). All other dimensions (1-9) have source code available to review.
- **Type**: Boundary case
- **Covered Dimension**: Static Analysis Tool Findings
- **Expected Finding**: Dimension 10 skipped with a documented note (no issues, no failure)
- **Verification Standards**:
  - [ ] Missing file detected gracefully — the reviewer checks for `target/static-analysis-findings.json` and handles its absence without erroring
  - [ ] Dimension 10 skipped (not failed) — the dimension is recorded as "skipped" rather than "failed" in the compliance summary
  - [ ] Warning documented in report — the note "Static analysis findings file not found — Dimension 10 skipped" appears in the report (review summary / compliance summary)
  - [ ] Other dimensions proceed normally — Dimensions 1-9 run as usual; their findings are unaffected by the Dimension 10 skip
  - [ ] `conclusion` not affected by skip — the overall review conclusion is determined by Dimensions 1-9 only; a missing static analysis file does not cause a Pass to become Fail
