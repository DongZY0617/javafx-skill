# Test Verification Rules

> This document defines the check items, pass/fail criteria, and severity baselines for the **Test Verification** dimension.
> Test verification executes `mvn test` (or `gradle test`), parses test results, and identifies test failures and coverage gaps.
> It is the fourth verification dimension, positioned after Runtime Verification and before Packaging Verification.

## Core Principle

Test verification answers "does the code do the right thing?" — as opposed to Runtime Verification which answers "does the code start without crashing?". Test verification catches functional correctness defects that smoke tests cannot. A project that starts successfully but has 0 tests or failing tests is not ready for delivery.

## Check Items

### Check Item 1: Test Compilation

**Focus**: Whether test source files (`src/test/java/`) compile without errors.

**Pass Criteria**:
- All test Java files compile successfully with `mvn test-compile`
- No `cannot find symbol` errors in test code
- Test dependencies (TestFX, JUnit 5, Mockito) are resolved

**Fail Criteria** (any one constitutes failure):
- Test compilation fails with syntax errors
- Missing test dependencies (TestFX/JUnit not in pom.xml)
- Test code references non-existent production classes/methods

**Severity Baseline**: Critical

---

### Check Item 2: Test Execution

**Focus**: Whether all tests pass with zero failures.

**Pass Criteria**:
- `mvn test` completes with 0 failures, 0 errors
- Tests run on JavaFX Application Thread where required (TestFX ApplicationTest)
- Headless mode works in CI (Monocle/prism software rendering)

**Fail Criteria** (any one constitutes failure):
- Any test failure (`Tests run: N, Failures: >0`)
- Any test error (exception thrown during test execution)
- Test suite hangs or times out (default 5-minute timeout)

**Severity Baseline**: Major

> **Runtime Verification Required**
> - If tests fail due to UI thread exceptions, cross-reference `runtime-verification.md` #6 (Thread Safety)
> - Runner dynamic finding supersedes static heuristic when conflicting

---

### Check Item 3: Test Coverage

**Focus**: Whether critical application paths have test coverage.

**Pass Criteria**:
- At least one test exists for each Controller class
- At least one test exists for each ViewModel class (if MVVM)
- At least one TestFX integration test covers main window startup

**Fail Criteria** (any one constitutes failure):
- Zero test files exist in the project
- Controller classes with 0 tests (warning only, not blocking)

**Severity Baseline**: Minor
- De-escalation condition: Project is a small prototype / single-file app → Info
- Escalation condition: Production app with 0 tests → Major

---

### Check Item 4: FXML Load Test

**Focus**: Whether a TestFX test verifies FXML loading and controller injection.

**Pass Criteria**:
- At least one test loads FXML via `FXMLLoader.load()` and verifies root node is non-null
- At least one test verifies controller injection (`loader.getController() != null`)
- At least one test verifies CSS stylesheet loading (no parse errors)

**Fail Criteria** (any one constitutes failure):
- No FXML load test exists despite having FXML files in the project

**Severity Baseline**: Major
- De-escalation condition: Only 1 FXML file (simple view) → Minor

---

### Check Item 5: UI Interaction Test

**Focus**: Whether at least one TestFX test covers UI interactions (button click, form submit, table selection).

**Pass Criteria**:
- At least one test simulates button click (`clickOn("#buttonId")`)
- At least one test verifies table content after data load
- At least one test covers form input + submit flow

**Fail Criteria** (any one constitutes failure):
- No UI interaction test exists despite having interactive UI elements

**Severity Baseline**: Minor
- Escalation condition: Complex CRUD app with no interaction tests → Major

## Short-Circuit Rules

- **Compile Verification fails** → Skip Test Verification (cannot run tests on uncompilable code); mark as "Skipped (compile failure)"
- **Test Verification fails** → Skip Packaging Verification (should not package untested code); mark as "Skipped (test failure)"
- **Test Verification passes but coverage = 0** → Downgrade to Info, does not block flow

## Typical Failure Examples

**Test compilation failure**:
```
[ERROR] /src/test/java/com/example/MainWindowTest.java:[15,5] cannot find symbol
  symbol:   class MainController
  location: class com.example.MainWindowTest
```

**Test execution failure**:
```
[ERROR] Tests run: 5, Failures: 1, Errors: 0, Skipped: 0
[ERROR] <<< FAILURE! - shouldShowInitialData()
[ERROR] java.lang.AssertionError: TableView should have initial data after load
```

**Headless mode failure** (CI without display):
```
java.awt.AWTError: "Toolkit not initialized"
```
> Missing -Dprism.order=sw or -Dmonocle.platform=Headless

## JaCoCo XML Report Parsing

Test coverage is reported by the JaCoCo Maven/Gradle plugin as an XML document. The runner parses this XML to populate the coverage fields of its report schema and to drive Check Item 3 (Test Coverage) pass/fail decisions.

### Report Location

The JaCoCo XML report is emitted at:

```
target/site/jacoco/jacoco.xml          (Maven, single module)
build/reports/jacoco/test/jacoco.xml   (Gradle, single module)
```

For the report to be produced, the build must include the `jacoco-maven-plugin` (Maven) or `jacoco` plugin (Gradle) with the `prepare-agent` and `report` goals bound. If `jacoco.xml` is absent after `mvn test`, Check Item 3 cannot be evaluated and should be marked "Skipped (no coverage report)" rather than failed.

### Key XML Elements

The JaCoCo XML is organized as a tree of `<package>`, `<class>`, `<method>`, and `<counter>` elements. Coverage numbers live in `<counter>` elements that carry a `type`, a `missed` count, and a `covered` count:

```xml
<counter type="LINE" missed="12" covered="88"/>
<counter type="BRANCH" missed="3" covered="17"/>
<counter type="METHOD" missed="1" covered="9"/>
<counter type="CLASS" missed="0" covered="5"/>
```

- `type="LINE"` — line coverage, the primary metric for Check Item 3.
- `type="BRANCH"` — branch (decision-point) coverage, used to detect untested if/else and switch arms.
- `type="METHOD"` — method coverage, used to confirm each Controller/ViewModel method has at least one test.
- `type="CLASS"` — class coverage, used to confirm at least one test exercises each Controller/ViewModel class.

Each `<class>` and `<package>` element also carries a rolled-up `<counter>` that aggregates all of its children, so coverage can be read at any granularity without summing manually.

### Extracting Coverage for Specific Classes via XPath

To evaluate Check Item 3 per-class (Controller/ViewModel coverage), extract the coverage counter for a specific class using XPath:

```xpath
/report/package[@name='com/example/ui']/class[@name='com/example/ui/MainController']/counter[@type='LINE']
```

This returns the `<counter>` element for `MainController`, from which `covered` and `missed` are read. Repeat for each Controller and ViewModel class discovered in the source tree.

For a package-level rollup (useful for the "at least one test per Controller" check):

```xpath
/report/package[@name='com/example/ui']/counter[@type='CLASS']
```

### Mapping JaCoCo Counters to the Runner Report Schema

The runner report schema stores coverage as a percentage. Convert JaCoCo's `missed`/`covered` counts to the schema fields as follows:

```
line_coverage_percent     = covered(LINE)   / (covered(LINE)   + missed(LINE))   * 100
branch_coverage_percent   = covered(BRANCH) / (covered(BRANCH) + missed(BRANCH)) * 100
method_coverage_percent   = covered(METHOD) / (covered(METHOD) + missed(METHOD)) * 100
class_coverage_percent    = covered(CLASS)  / (covered(CLASS)  + missed(CLASS))  * 100
```

Round to one decimal place when writing to the report. A class with `covered=0` and `missed=0` (no executable lines, e.g., a pure data holder) should be recorded as `100%` (nothing to cover) and excluded from the "untested Controller" warning.

The runner's per-class coverage entries should be tagged with the class FQCN so the reviewer can correlate them with static findings on the same class.

### Handling Multi-Module Projects

For a multi-module Maven project, each module produces its own `target/site/jacoco/jacoco.xml`. To get a single aggregate report across all modules, bind the `report-aggregate` goal of `jacoco-maven-plugin` in a dedicated reporting module that depends on all other modules:

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <executions>
    <execution>
      <id>aggregate-report</id>
      <phase>verify</phase>
      <goals><goal>report-aggregate</goal></goals>
    </execution>
  </executions>
</plugin>
```

The aggregate report is emitted at `target/site/jacoco-aggregate/jacoco.xml` and contains merged `<package>`/`<class>` entries from every dependency module. The runner should prefer the aggregate report when present and fall back to per-module reports otherwise, summing `missed`/`covered` across modules for the project-level totals.

For Gradle, use the `jacocoAggregation` plugin (or `jacocoMerge` + `jacocoReport`) to produce an equivalent merged XML.

## TestFX Failure Modes

TestFX integration tests fail in characteristic ways that are distinct from plain JUnit failures. Recognizing the failure mode speeds up diagnosis. The runner should classify TestFX failures into the categories below and attach the category to the failure record.

### Common Failure Modes

| Failure Mode | Typical Error Message | Root Cause |
|--------------|------------------------|------------|
| Node not found | `org.testfx.service.finder.impl.NodeFinderImpl$NodeQueryException: Node not found for query "#saveButton"` | Query mismatch — the CSS/FX id or selector does not match any node in the scene graph. Causes: wrong id, node not yet rendered, node in a different window. |
| Timeout waiting for events | `java.util.concurrent.TimeoutException: Timeout waiting for events to be processed` | FX thread blocked — a handler or layout pass is taking longer than TestFX's event-processing timeout (default 30s), so queued input events never get dispatched. |
| Window not showing | `java.lang.IllegalStateException: Window not showing` or `No stages shown` | Startup failure — the `start()` method threw before showing the primary stage, or the stage was closed before the test interacted with it. |

### Diagnostic Steps per Failure Mode

**Node not found**:
1. Print the full scene graph with `lookup(".root").toString()` or `NodeFinder` traversal to confirm what nodes actually exist and their ids.
2. Check whether the node is in a different `Window` — TestFX's default `lookup` searches the focused window. Use `window("Window Title").lookup("#id")` to target a specific window.
3. Verify the node is actually rendered (not lazily created). Add `waitFor("#saveButton")` before interacting, or use `clickOn("#saveButton", MouseButton.PRIMARY)` with a wait.
4. Confirm the id is set via `setId("saveButton")` in code or `fx:id` vs `id` in FXML (note: `fx:id` is for controller injection, `id` is the CSS id used by `lookup`).

**Timeout waiting for events**:
1. Capture a thread dump during the hang (`jstack <pid>`); look for the `JavaFX Application Thread` stack to find the blocking call. Cross-reference `../javafx-tester/references/performance-testing.md` section 8 (FX Thread Blocking Detection).
2. Reduce the blocking: if the handler performs I/O or DB calls, mock them in the test.
3. Increase the timeout only as a last resort via `PropertiesThread.properties().setProperty("testfx.timeout", "60s")` — a legitimate test should not need more than the default.

**Window not showing**:
1. Run the FXML load test (Check Item 4) in isolation — if it also fails, the FXML/Controller has a load-time error (missing resource, bad controller reference) that prevents `start()` from showing the stage.
2. Check the test's `ApplicationTest` lifecycle: a `@BeforeEach` that closes the stage, or a `start(Stage)` override that returns early, will leave no stage shown.
3. Look for uncaught exceptions on the FX thread: register `Thread.setDefaultUncaughtExceptionHandler` in the test to log startup exceptions that TestFX swallows.

### Capturing Screenshots on Test Failure

A screenshot of the scene at the moment of failure is the single most useful artifact for debugging UI test failures, especially "Node not found" (shows the actual visible UI) and "Window not showing" (shows whether a dialog is open over the primary stage).

Register a JUnit 5 extension that captures the screenshot after each failed test:

```java
public class ScreenshotOnFailure implements AfterTestExecutionCallback {
    @Override
    public void afterTestExecution(ExtensionContext ctx) {
        if (ctx.getExecutionException().isEmpty()) {
            return; // test passed, no screenshot
        }
        String name = ctx.getTestClass().map(Class::getSimpleName).orElse("test")
                   + "-" + ctx.getTestMethod().map(Method::getName).orElse("method");
        Path out = Path.of("target/testfx-screenshots", name + ".png");
        Files.createDirectories(out.getParent());
        // Capture on the FX thread to avoid concurrent scene access
        FxRobotContext ctx2 = ...; // from ApplicationTest
        Platform.runLater(() -> {
            List<Window> windows = Window.getWindows();
            if (windows.isEmpty()) { return; }
            WritableImage img = windows.get(0).getScene().snapshot(null);
            try { SwingFXUtils.fromFXImage(img, ImageIO.read(...)); /* write PNG */ }
            catch (IOException e) { /* log */ }
        });
    }
}
```

The screenshot path (`target/testfx-screenshots/<test>.png`) should be attached to the runner's failure record so it is surfaced in the report. For headless CI, ensure `prism.order=sw` so the snapshot is rendered without a GPU.

## Database Integration Test Patterns

JavaFX desktop apps frequently persist data via JPA/Hibernate. Database integration tests validate the repository/service layer that backs the UI. These tests run as part of `mvn test` and feed Check Item 2 (Test Execution) and Check Item 3 (Test Coverage) for the non-UI layers.

### @DataJpaTest Slice Testing

Spring Boot's `@DataJpaTest` loads only the JPA slice of the application context — repositories, entities, and an embedded database — without starting the full application or the JavaFX layer. This keeps repository tests fast and isolated from UI concerns:

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository repository;

    @Test
    void findByEmail_returnsCustomer_whenEmailExists() {
        repository.save(new Customer("alice@example.com", "Alice"));
        Optional<Customer> found = repository.findByEmail("alice@example.com");
        assertThat(found).isPresent().get()
            .extracting(Customer::getName).isEqualTo("Alice");
    }
}
```

- `@DataJpaTest` auto-configures an in-memory database (see H2 below), auto-detects `@Entity` classes, and rolls back each test's transaction by default.
- It does NOT load `@Component`, `@Service`, or `@Controller` beans — only `@Repository`. This prevents UI/controller code from leaking into repository tests.
- Combine with `@Import(YourService.class)` when a repository test needs a specific service bean.

### H2 In-Memory Database for Test Isolation

Configure H2 as the test database so each test run starts with a clean, empty schema, with no external database dependency:

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate.dialect: org.hibernate.dialect.H2Dialect
```

- `MODE=PostgreSQL` makes H2 accept PostgreSQL-compatible SQL so production queries (written for Postgres) run unchanged in tests.
- `ddl-auto=create-drop` recreates the schema on startup and drops it on shutdown, guaranteeing isolation between runs.
- `DB_CLOSE_DELAY=-1` keeps the in-memory DB alive across connections so Flyway/Liquibase and JPA can share it within a test.

> **Compatibility note**: H2 does not support every feature of the production database. Tests that depend on database-specific functions (e.g., Postgres JSONB, full-text search) should be tagged and run against a real database (e.g., via Testcontainers) rather than H2. Flag such tests in the runner report as "database-dependent".

### @Sql Annotations for Test Data Setup

Use `@Sql` to load deterministic seed data per test instead of building entities programmatically. This keeps test data declarative and reviewable:

```java
@DataJpaTest
@Sql(scripts = "/testdata/customers.sql")
class CustomerReportRepositoryTest {

    @Test
    void countActiveCustomers_returnsExpectedCount() {
        long count = repository.countActiveCustomers();
        assertThat(count).isEqualTo(42);
    }

    @Test
    @Sql(scripts = {"/testdata/customers.sql", "/testdata/extra-customers.sql"})
    void countActiveCustomers_includesExtraSeedData() {
        assertThat(repository.countActiveCustomers()).isEqualTo(55);
    }
}
```

- `@Sql` scripts run after schema creation and before the test method, within the same transaction.
- Place scripts under `src/test/resources/testdata/` to keep them out of the production classpath.
- Use `@Sql(executionPhase = BEFORE_TEST_METHOD)` / `AFTER_TEST_METHOD` for finer control.

### Transaction Rollback Between Tests

`@DataJpaTest` rolls back the test transaction by default, so data inserted by one test is not visible to the next. This is the desired behavior for isolation:

- Do NOT add `@Rollback(false)` or `@Commit` to repository tests unless the test explicitly verifies commit behavior — doing so breaks isolation and causes flaky, order-dependent tests.
- If a test needs to verify that a write is actually flushed to the database (not just the persistence context), inject `TestEntityManager` and call `testEntityManager.flush()` + `testEntityManager.clear()` before asserting, while still letting the transaction roll back at the end.
- For service-layer tests that require a real commit (e.g., to test `@Transactional` propagation), use `@Transactional(propagation = Propagation.REQUIRES_NEW)` on a helper method or extract the commit into a dedicated `@TestConfiguration` bean.

### Cross-References

When database integration tests fail, cross-reference:
- The repository/service class FQCN against the reviewer's static checks for that class.
- `../javafx-tester/references/performance-testing.md` section 8.4 (Common Blocking Patterns) — JDBC on the FX thread — when a DB test failure is actually caused by UI code calling the repository synchronously.
