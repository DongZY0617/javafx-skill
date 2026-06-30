# JavaFX Runner Evaluation Test Cases

This file defines the acceptance test cases for the `javafx-runner` skill, used to quantify verification output quality. Each case describes the input scenario, case type, covered dimensions, expected issues, and checkable verification standards.

- **Positive samples**: Projects with real failures that verify runner recall (whether all expected issues are discovered by actually executing build commands)
- **Negative samples**: Healthy projects that verify runner precision (no false positives, zero false positives)
- **Boundary cases**: Escalation/de-escalation decisions, incremental verification, headless mode, etc., verifying runner accuracy under boundary conditions

---

## Case Overview

| ID | Name | Type | Covered Dimensions | Expected Issues |
|----|------|------|--------------------|-----------------|
| 1 | Compilation syntax error | Positive | Compile Verification | 1 Critical |
| 2 | Module opens missing (PropertyValueFactory) | Positive | Compile Verification / Runtime Verification | 1 Critical |
| 3 | FXML controller load failure | Positive | Runtime Verification | 1 Critical |
| 4 | CSS var() parse error | Positive | Runtime Verification | 1 Major |
| 5 | Thread safety runtime exception | Positive | Runtime Verification | 1 Critical |
| 6 | JavaFX 24+ native access missing | Positive | Runtime Verification | 1 Critical |
| 7 | jpackage missing --add-modules | Positive | Packaging Verification | 1 Critical (escalated) |
| 8 | jpackage toolchain missing | Boundary | Packaging Verification | 1 Info (de-escalated) |
| 9 | Startup timeout due to blocking call | Positive | Runtime Verification | 1 Critical (escalated) |
| 10 | Headless CI verification (Monocle) | Boundary | Runtime Verification / Environment Setup | 0 (passes with Monocle) |
| 11 | Healthy project (negative sample) | Negative | All dimensions | 0 (zero false positives) |
| 12 | Incremental verification (CSS only) | Boundary | Targeted dimension | CSS runtime issues only |
| 13 | Fix handoff completeness | Positive | All dimensions | Includes handoff fields |
| 14 | Static Analysis — SpotBugs NPE | Positive | Static Analysis Verification | 1 Major (NP_NULL_ON_SOME_PATH) |
| 15 | Static Analysis — PMD false positive | Negative | Static Analysis Verification | 0 (suppressed) |
| 16 | Static Analysis — Checkstyle method length | Positive | Static Analysis Verification | 1 Minor (MethodLengthCheck) |
| 17 | Static Analysis skipped (no plugins) | Boundary | Static Analysis Verification | 0 (skipped) |
| 18 | Static Analysis skipped (compile fail) | Short-circuit | Static Analysis Verification | 0 (short-circuited) |
| 19 | Test Verification — all pass | Negative | Test Verification | 0 (tests pass, coverage >= 60%) |
| 20 | Low coverage triggers Major | Positive | Test Verification | 1 Major (JaCoCo coverage < 60%) |
| 21 | Test failure triggers Critical | Positive | Test Verification | 1 Critical (test failure) |

---

## Case 1: Compilation Syntax Error

- **Input**: A JavaFX project where a source file has a syntax error (missing semicolon, unbalanced parenthesis)
  ```java
  // UserController.java
  @FXML
  private void handleSave() {
      String name = nameField.getText()   // missing semicolon
      if (name.isEmpty() {
          showError("Name is required");
      }
  }
  ```
- **Type**: Positive sample
- **Covered Dimension**: Compile Verification
- **Expected Finding**: 1 Critical - syntax compilation failure
- **Verification Standards**:
  - [ ] Executes `mvn compile -q`, captures the `[ERROR] /path/UserController.java:[line,col] ';' expected` output
  - [ ] Accurately locates the root syntax error (not the cascaded dependent errors)
  - [ ] Severity determined as Critical (cannot be de-escalated; compilation failure prevents running)
  - [ ] Compile failure short-circuit is triggered: runtime and packaging verification are skipped, report notes "subsequent verification skipped due to compile failure"
  - [ ] Rule reference cites `compile-verification.md -- Syntax Compilation`
  - [ ] Includes fix handoff fields (target_file / target_lines / fix_type / fix_priority)

---

## Case 2: Module opens Missing (PropertyValueFactory)

- **Input**: A modular JavaFX project where `module-info.java` exports the model package but does not `opens` it to `javafx.controls`; the Controller uses `PropertyValueFactory`
  ```java
  // module-info.java
  module com.example.app {
      requires javafx.controls;
      requires javafx.fxml;
      exports com.example.app.model;
      // Missing: opens com.example.app.model to javafx.controls;
  }
  ```
  ```java
  // UserController.java
  TableColumn<User, String> nameCol = new TableColumn<>("Name");
  nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
  ```
- **Type**: Positive sample
- **Covered Dimension**: Compile Verification / Runtime Verification
- **Expected Finding**: 1 Critical - module opens missing causing PropertyValueFactory reflection failure
- **Verification Standards**:
  - [ ] Compile verification passes (this is a compile-pass / runtime-fail gap), but the module configuration check flags the missing `opens`
  - [ ] Runtime verification captures `IllegalAccessException: ... cannot access class com.example.app.model.User because module com.example.app does not "opens com.example.app.model" to module javafx.controls`
  - [ ] Deduplicates: the compile-dimension "module opens missing" warning and the runtime-dimension `IllegalAccessException` are merged into one issue (same root cause)
  - [ ] Severity determined as Critical (default baseline; PropertyValueFactory is used, so de-escalation does not apply)
  - [ ] Fix recommendation adds `opens com.example.app.model to javafx.controls;` to `module-info.java`
  - [ ] Rule reference cites `compile-verification.md -- Module Configuration` + `runtime-verification.md -- Module Runtime`

---

## Case 3: FXML Controller Load Failure

- **Input**: A modular JavaFX project where `module-info.java` is missing `opens com.example.controller to javafx.fxml`; the FXML declares `fx:controller`
  ```java
  // module-info.java
  module com.example.app {
      requires javafx.controls;
      requires javafx.fxml;
      opens com.example.app.model to javafx.controls;
      // Missing: opens com.example.app.controller to javafx.fxml;
  }
  ```
  ```xml
  <!-- user-view.fxml -->
  <VBox xmlns:fx="http://javafx.com/fxml"
        fx:controller="com.example.app.controller.UserController">
  ```
- **Type**: Positive sample
- **Covered Dimension**: Runtime Verification
- **Expected Finding**: 1 Critical - FXML load failure (LoadException caused by IllegalAccessException)
- **Verification Standards**:
  - [ ] Runtime verification executes `mvn javafx:run`, captures `javafx.fxml.LoadException` in stdout/stderr
  - [ ] Identifies the root cause: `IllegalAccessException` because the controller package is not `opens` to `javafx.fxml`
  - [ ] Severity determined as Critical (cannot be de-escalated; runtime will always throw `LoadException`)
  - [ ] Fix recommendation adds `opens com.example.app.controller to javafx.fxml;` to `module-info.java`
  - [ ] Rule reference cites `runtime-verification.md -- FXML Load`
  - [ ] Includes fix handoff fields

---

## Case 4: CSS var() Parse Error

- **Input**: A JavaFX project where the CSS file uses the unsupported Web CSS `var()` function
  ```css
  /* app.css */
  .root {
      -fx-primary-color: #2196f3;
  }
  .button-primary {
      -fx-background-color: var(-fx-primary-color);   /* JavaFX CSS does not support var() */
      -fx-background-radius: var(-fx-radius);          /* unsupported */
  }
  ```
- **Type**: Positive sample
- **Covered Dimension**: Runtime Verification
- **Expected Finding**: 1 Major - CSS parse error (unsupported var() syntax)
- **Verification Standards**:
  - [ ] Runtime verification captures `WARNING: Could not resolve ...` in stdout/stderr when the stylesheet is loaded
  - [ ] Points out that JavaFX CSS does not support the `var()` function (this is a Web CSS feature)
  - [ ] Provides a direct looked-up color reference fix: `-fx-background-color: -fx-primary-color;`
  - [ ] Border radius fix uses a literal numeric value: `-fx-background-radius: 8;`
  - [ ] Severity determined as Major (default baseline; does not cause the UI to fail to display, so no escalation to Critical)
  - [ ] Rule reference cites `runtime-verification.md -- CSS Parse`

---

## Case 5: Thread Safety Runtime Exception

- **Input**: A JavaFX project where a background thread directly updates a UI component
  ```java
  new Thread(() -> {
      try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
      statusLabel.setText("Done");   // Background thread directly updating UI
  }).start();
  ```
- **Type**: Positive sample
- **Covered Dimension**: Runtime Verification
- **Expected Finding**: 1 Critical - thread safety runtime exception
- **Verification Standards**:
  - [ ] Runtime verification captures `IllegalStateException: Not on FX application thread; not on FX application thread` in stderr
  - [ ] Identifies that `setText` is called on a non-FX thread (background `Thread`)
  - [ ] Provides a `Platform.runLater()` or `Task.setOnSucceeded()` fix
  - [ ] Severity determined as Critical (cannot be de-escalated; runtime will always throw `IllegalStateException`)
  - [ ] Rule reference cites `runtime-verification.md -- Thread Safety Runtime Verification`
  - [ ] Includes fix handoff fields

---

## Case 6: JavaFX 24+ Native Access Missing

- **Input**: A JavaFX 24 project where the `javafx-maven-plugin` configuration omits `--enable-native-access=javafx.graphics`
  ```xml
  <plugin>
      <groupId>org.openjfx</groupId>
      <artifactId>javafx-maven-plugin</artifactId>
      <version>0.0.8</version>
      <configuration>
          <mainClass>com.example.app/com.example.app.App</mainClass>
          <!-- Missing: <options><option>--enable-native-access=javafx.graphics</option></options> -->
      </configuration>
  </plugin>
  ```
- **Type**: Positive sample
- **Covered Dimension**: Runtime Verification
- **Expected Finding**: 1 Critical - JavaFX 24+ missing native access configuration
- **Verification Standards**:
  - [ ] Environment detection identifies the project as JavaFX 24+, activating the native access check
  - [ ] Runtime verification captures `IllegalAccessError: ... cannot access ... because module javafx.graphics does not have native access`
  - [ ] Severity determined as Critical (cannot be de-escalated; runtime will always report `IllegalAccessError`)
  - [ ] Fix recommendation adds `<options><option>--enable-native-access=javafx.graphics</option></options>` to the plugin configuration
  - [ ] Rule reference cites `runtime-verification.md -- JavaFX 24+ Native Access`
  - [ ] Notes that this check is skipped for JavaFX 17/21 projects (version-aware verification)

---

## Case 7: jpackage Missing --add-modules

- **Input**: A JavaFX project where the jpackage command omits `--add-modules javafx.controls,javafx.fxml`; the generated installer cannot find JavaFX modules at runtime
  ```bash
  jpackage --name myapp --input target --main-jar app.jar \
    --main-class com.example.App
  # Missing: --module-path $PATH_TO_FX/lib --add-modules javafx.controls,javafx.fxml
  ```
- **Type**: Positive sample
- **Covered Dimension**: Packaging Verification
- **Expected Finding**: 1 Critical - module-path/add-modules misconfiguration causing the generated artifact to be unrunnable (escalated from Major)
- **Verification Standards**:
  - [ ] Packaging verification executes `mvn package -DskipTests` (JAR builds successfully), then executes jpackage
  - [ ] Captures `Error: Module javafx.controls not found, required by com.example.app` when running the generated installer
  - [ ] Severity escalated from Major to Critical (module-path misconfiguration causing the generated artifact to be unrunnable)
  - [ ] Report "Escalation/De-escalation Note" field records the escalation condition: "module-path misconfiguration causing the generated artifact to be unrunnable, escalated to Critical"
  - [ ] Fix recommendation adds `--module-path $PATH_TO_FX/lib --add-modules javafx.controls,javafx.fxml`
  - [ ] Rule reference cites `packaging-verification.md -- Module Path Integrity`

---

## Case 8: jpackage Toolchain Missing

- **Input**: A Windows JavaFX project where jpackage is invoked for `.exe` generation, but Inno Setup is not installed
  ```bash
  jpackage --name myapp --input target --main-jar app.jar \
    --main-class com.example.App --type exe
  # Inno Setup (ISCC.exe) is not installed on the system
  ```
- **Type**: Boundary case
- **Covered Dimension**: Packaging Verification
- **Expected Finding**: 1 Info - toolchain missing (de-escalated from Major to Info, environment issue not a code issue)
- **Verification Standards**:
  - [ ] Packaging verification captures `jpackage failed. Cannot find Inno Setup compiler (ISCC.exe)`
  - [ ] Diagnoses the failure as a toolchain missing issue (environment problem, not a code problem)
  - [ ] Severity de-escalated from Major to Info (toolchain not installed is an environment issue)
  - [ ] Report "Escalation/De-escalation Note" field records the de-escalation condition: "Toolchain not installed (environment issue, not a code issue), de-escalated to Info"
  - [ ] Fix recommendation provides the Inno Setup installation guidance, not a code change
  - [ ] Rule reference cites `packaging-verification.md -- Platform Toolchain`

---

## Case 9: Startup Timeout Due to Blocking Call

- **Input**: A JavaFX project where `start()` performs a synchronous network request that blocks the FX thread for 40 seconds, exceeding the 30-second timeout
  ```java
  @Override
  public void start(Stage primaryStage) throws Exception {
      // Blocks for 40 seconds, exceeds the 30-second startup timeout
      String config = fetchConfigFromRemoteServer();   // synchronous network call
      Parent root = FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));
      primaryStage.setScene(new Scene(root));
      primaryStage.show();
  }
  ```
- **Type**: Positive sample
- **Covered Dimension**: Runtime Verification
- **Expected Finding**: 1 Critical - startup timeout due to blocking call in `start()` (escalated from Major)
- **Verification Standards**:
  - [ ] Runtime verification records "startup timeout" after the 30-second timeout, terminates the process
  - [ ] Identifies the root cause: blocking call (synchronous network request) in `start()` on the FX thread
  - [ ] Severity escalated from Major to Critical (timeout due to blocking call in `start()`)
  - [ ] Report "Escalation/De-escalation Note" field records the escalation condition: "Timeout due to blocking call in start(), escalated to Critical"
  - [ ] Fix recommendation moves the blocking operation to a background `Task`, shows the window immediately
  - [ ] Rule reference cites `runtime-verification.md -- Startup Timeout Detection`

---

## Case 10: Headless CI Verification (Monocle)

- **Input**: A JavaFX project verified in a Linux CI environment without a display (`DISPLAY` unset); the project has the Monocle dependency and is run with headless flags
  ```xml
  <!-- pom.xml has the Monocle dependency -->
  <dependency>
      <groupId>org.testfx</groupId>
      <artifactId>openjfx-monocle</artifactId>
      <version>jdk-17+21</version>
      <scope>test</scope>
  </dependency>
  ```
  ```bash
  # CI runs with headless flags
  mvn javafx:run -Dmonocle.platform=Headless -Dprism.order=sw
  ```
- **Type**: Boundary case
- **Covered Dimension**: Runtime Verification / Environment Setup
- **Expected Finding**: 0 issues (the application starts successfully in headless mode)
- **Verification Standards**:
  - [ ] Environment detection identifies no display (`DISPLAY` unset), selects Monocle headless mode
  - [ ] Runtime verification uses the headless command `-Dmonocle.platform=Headless -Dprism.order=sw`
  - [ ] The application starts without `X11Display: Can't open display` errors
  - [ ] No `ClassNotFoundException: ...MonoclePlatform` (Monocle dependency is present)
  - [ ] Process exits with code 0 (smoke test passes)
  - [ ] Report header annotates "Display: Unavailable (CI, using Monocle headless)"
  - [ ] Rule reference cites `environment-setup.md -- Monocle Headless Configuration`

---

## Case 11: Healthy Project (Negative Sample)

- **Input**: A fully healthy JavaFX project
  ```
  // Compile: mvn compile passes with no errors or warnings
  // Runtime: mvn javafx:run starts, window displays, no exceptions
  // Packaging: mvn package generates JAR, jpackage generates installer
  // module-info.java: all requires/exports/opens correct
  // No thread safety issues, no CSS errors, no resource path issues
  ```
- **Type**: Negative sample
- **Covered Dimension**: All dimensions
- **Expected Finding**: 0 issues
- **Verification Standards**:
  - [ ] Issue list is empty
  - [ ] Verification conclusion is "Pass"
  - [ ] Verification result summary shows all check items passed, pass rate 100%
  - [ ] No false positives (zero false positives) - no issues are reported for healthy code
  - [ ] All three dimensions executed (compile, runtime, packaging) without short-circuit

---

## Case 12: Incremental Verification (CSS Only)

- **Input**: Only a CSS file was modified (e.g., adjusting a `.button-primary` style), user requests incremental verification
- **Type**: Boundary case
- **Covered Dimension**: Targeted dimension (Runtime Verification - CSS parse only)
- **Expected Finding**: Only CSS-related runtime issues (if any), no compile or packaging issues
- **Verification Standards**:
  - [ ] Skips compile verification (CSS changes do not affect compilation)
  - [ ] Skips packaging verification (CSS changes do not affect packaging)
  - [ ] Only executes runtime verification (CSS parse check)
  - [ ] Report header annotates verification mode as "Incremental Verification"
  - [ ] Report "Verification Scope" field lists only "Runtime Verification (CSS parse)"
  - [ ] Does not report compile or packaging issues unrelated to the CSS change

---

## Case 13: Fix Handoff Completeness

- **Input**: A JavaFX project with multiple verification failures (compile error + module opens missing + jpackage missing --add-modules)
- **Type**: Positive sample
- **Covered Dimension**: All dimensions
- **Expected Finding**: Each issue includes complete fix handoff fields
- **Verification Standards**:
  - [ ] Each issue includes `target_file` (file path)
  - [ ] Each issue includes `target_lines` (start and end line numbers)
  - [ ] Each issue includes `fix_type` (replace / insert / delete)
  - [ ] Each issue includes `fix_priority` (priority sorted by severity + dimension, 1 is highest)
  - [ ] Each issue includes `code_fingerprint` (64-character hex string, content hash for fingerprint matching)
  - [ ] Each issue includes `anchor_pattern` (contextual anchor pattern for line-drift recovery)
  - [ ] Each issue includes `ast_node_signature` (fully qualified AST signature; null for non-Java files like module-info.java)
  - [ ] The Fix Handoff Summary table lists all issues sorted by fix_priority
  - [ ] Fix handoff fields can be directly consumed by `javafx-developer` to execute fixes with no format conversion
  - [ ] `fix_type=replace` and `fix_type=insert` issues include a "Corrected Example" code snippet
  - [ ] Compile error (Critical) gets `fix_priority: 1`, runtime issue (Critical) gets `fix_priority: 2`, packaging issue (Critical) gets `fix_priority: 3` (sorted by dimension within the same severity)

## Case 14: Static Analysis — SpotBugs NPE Detection

- **Input**: A JavaFX project where `UserService.findById()` has a possible null pointer dereference (user variable used without null check after `userRepository.findById()` call). SpotBugs configured in pom.xml.
- **Type**: Positive sample
- **Covered Dimension**: Static Analysis Verification
- **Expected Finding**: SpotBugs detects `NP_NULL_ON_SOME_PATH` at the relevant line
- **Verification Standards**:
  - [ ] Runner executes `mvn spotbugs:check pmd:check checkstyle:check` after compile verification passes
  - [ ] `target/spotbugsXml.xml` is generated and parsed
  - [ ] Finding is mapped to unified issue structure with `tool: "spotbugs"`, `rule_id: "NP_NULL_ON_SOME_PATH"`, `severity: "Major"` (SpotBugs priority 1)
  - [ ] `ast_node_signature` is extracted (e.g., `com.example.UserService#findById(Long)`)
  - [ ] `target/static-analysis-findings.json` is generated containing the finding
  - [ ] Verification report includes "Static Analysis Verification" section with SpotBugs finding count

## Case 15: Static Analysis — PMD Unused @FXML Field (False Positive Suppression)

- **Input**: A JavaFX project with an FXML controller that has `@FXML private TableView<User> userTable;` — the field is injected by FXMLLoader, not used directly in code. PMD with XPath suppression for `@FXML` is configured.
- **Type**: Negative sample (false positive should be suppressed)
- **Covered Dimension**: Static Analysis Verification
- **Expected Finding**: PMD does NOT report `UnusedPrivateField` for `@FXML`-annotated fields
- **Verification Standards**:
  - [ ] `target/pmd.xml` is generated and parsed
  - [ ] No `UnusedPrivateField` finding for `@FXML`-annotated fields (XPath suppression works)
  - [ ] If PMD does report it (suppression not applied), the finding is still recorded but reviewer will later mark it as `false_positive`

## Case 16: Static Analysis — Checkstyle Method Length Violation

- **Input**: A JavaFX project where `UserController.handleSave()` is 85 lines long (exceeds the 80-line limit in checkstyle.xml).
- **Type**: Positive sample
- **Covered Dimension**: Static Analysis Verification
- **Expected Finding**: Checkstyle detects `MethodLengthCheck` violation
- **Verification Standards**:
  - [ ] `target/checkstyle-result.xml` is generated and parsed
  - [ ] Finding is mapped with `tool: "checkstyle"`, `rule_id: "MethodLengthCheck"`, `severity: "Minor"` (Checkstyle error → Minor)
  - [ ] `line_number` and `source_file` are correctly extracted
  - [ ] Finding is included in `target/static-analysis-findings.json`

## Case 17: Static Analysis Skipped (No Plugins Configured)

- **Input**: A JavaFX project whose `pom.xml` does NOT include SpotBugs/PMD/Checkstyle plugin configurations.
- **Type**: Edge case
- **Covered Dimension**: Static Analysis Verification
- **Expected Finding**: Runner skips static analysis with a note
- **Verification Standards**:
  - [ ] Runner detects missing plugin configurations in pom.xml
  - [ ] Static analysis step is skipped with note: "Static analysis skipped — no SpotBugs/PMD/Checkstyle plugins configured"
  - [ ] No `target/static-analysis-findings.json` is generated
  - [ ] Verification report notes the skip in the Static Analysis Verification section
  - [ ] Runtime verification proceeds normally (not blocked by static analysis skip)

## Case 18: Static Analysis Skipped (Compile Failure)

- **Input**: A JavaFX project with a compilation error AND SpotBugs/PMD/Checkstyle configured.
- **Type**: Short-circuit test
- **Covered Dimension**: Static Analysis Verification
- **Expected Finding**: Static analysis is skipped because compile verification failed
- **Verification Standards**:
  - [ ] Compile verification fails (Critical issue recorded)
  - [ ] Static analysis is skipped (SpotBugs needs compiled bytecode)
  - [ ] No `target/static-analysis-findings.json` is generated
  - [ ] Verification report notes: "Static analysis skipped — compile verification failed"

---

## Case 19: Test Verification Pass

- **Input**: A JavaFX project with a healthy test suite. `mvn test` passes with 0 failures and 0 errors; JaCoCo reports critical-path (Controller/ViewModel) line coverage >= 60%.
  ```java
  // UserControllerTest.java
  @Test
  void shouldLoadUsersOnInitialize() {
      // TestFX test verifying FXML load + controller injection + initial data
      interact(() -> assertThat(lookup("#userTable").queryTableView().getItems()).isNotEmpty());
  }
  ```
  ```xml
  <!-- pom.xml has jacoco-maven-plugin configured; target/site/jacoco/jacoco.xml generated -->
  <!-- Controller line coverage: 0.78, ViewModel line coverage: 0.82 -->
  ```
- **Type**: Negative sample
- **Covered Dimension**: Test Verification
- **Expected Finding**: 0 issues — all tests pass and JaCoCo coverage meets the 60% threshold
- **Verification Standards**:
  - [ ] Runner executes `mvn test` (or `mvn test jacoco:report`) and captures `BUILD SUCCESS` with `Tests run: N, Failures: 0, Errors: 0, Skipped: 0`
  - [ ] Runner parses `target/site/jacoco/jacoco.xml` and extracts Controller/ViewModel line coverage
  - [ ] Controller and ViewModel line coverage are both >= 0.60 (60%)
  - [ ] `jacoco_report.passed` is `true` in the JSON report
  - [ ] `summary.dimensions.test.conclusion` is `"Pass"`
  - [ ] No Test Verification issues are recorded (zero false positives)
  - [ ] Test compilation check item passes (no errors in `src/test/java/`)
  - [ ] FXML load test and UI interaction test check items are satisfied (if present)
  - [ ] Packaging verification proceeds normally (test verification did not short-circuit)

---

## Case 20: Low Coverage Triggers Major

- **Input**: A JavaFX project where `mvn test` passes with 0 failures, but JaCoCo reports Controller/ViewModel line coverage below the 60% threshold. Several Controller methods (e.g., `handleSave`, `handleDelete`) have 0 test coverage.
  ```xml
  <!-- target/site/jacoco/jacoco.xml -->
  <!-- Controller line coverage: 0.42 (below 0.60 threshold) -->
  <!-- Uncovered methods: UserController#handleSave, UserController#handleDelete -->
  ```
- **Type**: Positive sample
- **Covered Dimension**: Test Verification
- **Expected Finding**: 1 Major — JaCoCo critical-path coverage below 60% threshold
- **Verification Standards**:
  - [ ] Runner executes `mvn test jacoco:report` and confirms all tests pass (`Tests run: N, Failures: 0, Errors: 0`)
  - [ ] Runner parses `target/site/jacoco/jacoco.xml` and detects Controller line coverage (0.42) < 0.60 threshold
  - [ ] A Major issue is recorded with `dimension: "Test Verification"`
  - [ ] Issue title/description names the threshold (60%) and the actual coverage value (42%)
  - [ ] `jacoco_report.passed` is `false` and `jacoco_report.uncovered_methods` lists `UserController#handleSave` and `UserController#handleDelete`
  - [ ] `summary.dimensions.test.conclusion` is `"Conditional Pass"` (tests pass but coverage gate fails)
  - [ ] Rule reference cites `test-coverage-gate.md -- JaCoCo Coverage Report`
  - [ ] Severity is Major (not Critical — tests pass, this is a coverage gap, not a test failure)
  - [ ] Fix recommendation lists the uncovered methods and recommends adding tests for them

---

## Case 21: Test Failure Triggers Critical

- **Input**: A JavaFX project where `mvn test` reports failing test cases (e.g., a TestFX test asserting `TableView` initial data fails because the Controller's `initialize()` does not load data).
  ```
  Tests run: 5, Failures: 1, Errors: 0, Skipped: 0
  <<< FAILURE! - shouldLoadUsersOnInitialize(UserControllerTest)
  ```
- **Type**: Positive sample
- **Covered Dimension**: Test Verification
- **Expected Finding**: 1 Critical — test execution failure
- **Verification Standards**:
  - [ ] Runner executes `mvn test` and captures the `BUILD FAILURE` / `Tests run: 5, Failures: 1, Errors: 0` output
  - [ ] The failing test case name (`shouldLoadUsersOnInitialize`) and failure message are captured in the issue description / `raw_output`
  - [ ] A Critical issue is recorded with `dimension: "Test Verification"`
  - [ ] Severity is Critical (default baseline for test execution failure; tests must pass before delivery)
  - [ ] Test failure short-circuit is triggered: packaging verification is skipped, report notes "should not package untested code"
  - [ ] `summary.dimensions.test.conclusion` is `"Fail"`
  - [ ] Rule reference cites `test-verification.md -- Test Execution`
  - [ ] Includes fix handoff fields (target_file pointing to the Controller or test under test)
  - [ ] Fix recommendation addresses the root cause (e.g., Controller `initialize()` must load data, or the test assertion must match actual behavior)
