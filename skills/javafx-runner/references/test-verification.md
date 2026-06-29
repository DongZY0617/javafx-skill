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
→ Missing -Dprism.order=sw or -Dmonocle.platform=Headless
```
