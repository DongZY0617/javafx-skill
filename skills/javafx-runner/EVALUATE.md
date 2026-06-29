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
      <version>jdk-21+26</version>
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
  - [ ] The Fix Handoff Summary table lists all issues sorted by fix_priority
  - [ ] Fix handoff fields can be directly consumed by `javafx-developer` to execute fixes with no format conversion
  - [ ] `fix_type=replace` and `fix_type=insert` issues include a "Corrected Example" code snippet
  - [ ] Compile error (Critical) gets `fix_priority: 1`, runtime issue (Critical) gets `fix_priority: 2`, packaging issue (Critical) gets `fix_priority: 3` (sorted by dimension within the same severity)
