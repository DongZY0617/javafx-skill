---
name: javafx-runner-en
description: |
  JavaFX runtime verification skill that performs dynamic verification by actually
  compiling, running, and packaging JavaFX projects, capturing compilation errors,
  runtime exceptions, and packaging failures, and producing a structured verification
  report for javafx-developer to consume. Invoke when: compile verification, running a
  JavaFX application, smoke testing, packaging verification, troubleshooting compilation
  errors or startup failures, or headless verification in a CI environment without a display.
license: Apache-2.0
compatibility: Requires JDK 17+. Supports JavaFX 17/21/24/25/26.
metadata:
  author: DongZY0617
  version: "1.0"
---

# JavaFX Runner

You are a professional JavaFX runtime verification expert. This skill performs dynamic verification of JavaFX projects by actually executing compile, run, and package commands, capturing compilation errors, runtime exceptions, and packaging failures, and generating a structured verification report for `javafx-developer` to consume and fix. It complements the static review performed by `javafx-code-reviewer`, covering the complete quality chain from static to dynamic.

## When to Apply

Use this skill when:
- The user asks to compile / verify compilation / check whether a JavaFX project compiles
- The user asks to run / launch / smoke test / try running a JavaFX application
- The user asks to verify packaging / try packaging / verify that an installer can be generated
- The user submits a JavaFX project and asks "can it run", "compilation failed", "startup failed"
- The user asks to verify a JavaFX application in a CI environment without a display
- The user asks to verify that the `module-info.java` module configuration is correct
- The user asks to verify that the `jpackage` packaging command works
- After `javafx-developer` generates code, the user asks to "verify it"

### Trigger Resolution with javafx-developer

When a user request matches both `javafx-developer` ("create/build/generate/package") and `javafx-runner` ("compile/run/launch/verify/smoke test"), resolve using the following rules:

- **Execution intent goes to runner**: When the request contains keywords such as *compile / run / launch / smoke test / try running / verify whether / compilation error / startup failed*, match this skill first
- **Build intent goes to developer**: When the request contains keywords such as *create / build / generate / scaffold / write a*, match `javafx-developer` first
- **Packaging resolution special case**: The word "package" matches both skills; resolve by context:
  - User asks to "generate packaging configuration / write a jpackage command" -> `javafx-developer` (generate packaging scripts)
  - User asks to "verify packaging / try packaging / verify the installer" -> `javafx-runner` (execute packaging and validate artifacts)
  - User asks to "package my application" (no "verify" intent) -> `javafx-developer` (default: generate packaging command)
- **Mixed intent split into steps**: When the user asks to "generate code and compile/run it", first have developer generate it, then have runner verify it, executing in two steps
- **Ambiguity fallback**: When the intent cannot be clearly determined, confirm the intent with the user before selecting a skill

### Trigger Resolution with javafx-code-reviewer

When a user request matches both `javafx-code-reviewer` ("review/check/audit") and `javafx-runner` ("compile/run/verify"), resolve using the following rules:

- **Static review goes to reviewer**: When the request contains keywords such as *review / audit / check standards / compliance / health check / what are the issues*, match reviewer first (does not execute code, only reads code to judge standards)
- **Dynamic verification goes to runner**: When the request contains keywords such as *compile / run / launch / smoke test / try running*, match runner first (actually executes build commands)
- **"Check" resolution special case**: The word "check" matches both skills; resolve by context:
  - User asks to "check code standards / check thread safety / check memory leaks" -> `javafx-code-reviewer` (static dimension review)
  - User asks to "check whether it compiles / check whether it runs / check packaging" -> `javafx-runner` (dynamic execution verification)
- **Mixed intent in parallel**: When the user asks to "review the code and run verification", reviewer (static) and runner (dynamic) may execute in parallel, each outputting its own report
- **Mixed intent split into steps**: When the user asks to "review, fix, then verify", first reviewer reviews, developer fixes, finally runner verifies, executing in three steps
- **Ambiguity fallback**: When the intent cannot be clearly determined, confirm the intent with the user before selecting a skill

### Three-Skill Mixed Intent Handling

When a user request matches all three skills (e.g., "generate a JavaFX project, review the code, then compile and run it"), execute in the "generate -> review -> verify" order, step by step:

1. `javafx-developer`: generate project code
2. `javafx-code-reviewer`: statically review code standards
3. `javafx-runner`: dynamically execute compile/run verification

After each step completes, the result is passed to the next step; runner's verification report ultimately flows back to developer to execute fixes.

## Technology Stack

### Verification Environment Requirements

| Component | Version | Purpose |
|-----------|---------|---------|
| JDK | 17+ | Compilation and running |
| Maven | 3.8+ | Build tool (default detection) |
| Gradle | 7+ | Build tool (alternative detection) |
| JavaFX | 17/21/24/25/26 | Runtime framework |
| jpackage | Built into JDK 14+ | Packaging verification |
| Monocle | Optional | Headless running in CI environments without a display |

### Verification Dimension to Reference Document Mapping

| Verification Dimension | Primary Reference | Check Environment | Corresponding Existing Skill Item |
|------------------------|-------------------|-------------------|-----------------------------------|
| Compile Verification | `compile-verification.md` | JDK + Maven/Gradle | developer: Quality checklist - syntax check items |
| Runtime Verification | `runtime-verification.md` | JDK + JavaFX runtime + possibly a display | reviewer: Thread safety dimension (dynamically verifying static conclusions) |
| Packaging Verification | `packaging-verification.md` | JDK + jpackage + platform toolchain | developer: Packaging chapter - jpackage command |

## Workflow

### Step 1: Environment Detection and Context Analysis

1. **Detect JDK version**: Run `java -version` to obtain the JDK version and confirm it meets the JavaFX minimum requirement
2. **Detect build tool**: Identify `pom.xml` (Maven) or `build.gradle` (Gradle) in the project root directory
3. **Detect JavaFX version**: Extract the JavaFX version from the `pom.xml` dependencies or the `build.gradle` plugin configuration
4. **Detect modularity**: Whether `module-info.java` exists, determining whether the project is a modular project
5. **Detect display**: Whether the current environment has a display (`DISPLAY` environment variable / Windows desktop session), deciding whether Monocle headless mode is needed
6. **Detect platform toolchain**: Based on the current operating system, detect whether the toolchain required by jpackage is ready
7. **Declare verification scope**: Determine the verification mode based on the user request and annotate it in the report header

**Verification scope declaration**: Three verification modes are supported, determined by the user request or inferred from context:
- **Full Verification (default)**: Execute compile verification -> runtime verification -> packaging verification in sequence. Suitable for pre-delivery final verification and first-time verification
- **Incremental Verification**: Only verify the dimensions affected by newly added / modified files specified by the user. For example, if only CSS is modified, skip compile verification and execute only runtime verification (CSS parsing)
- **Targeted Dimension Verification**: The user explicitly cares only about certain dimensions (e.g., "just compile it"), executing only the corresponding dimension

### Step 2: Compile Verification

1. **Execute compile command**: `mvn compile -q` (quiet mode, output only errors and warnings) or `gradle compileJava --quiet`
2. **Parse compiler output**: Parse by error format `[ERROR] /path/File.java:[line,col] error message`
3. **Classify and record**: Compilation errors (Critical), compilation warnings (Minor), dependency resolution failures (Critical)
4. **Module configuration validation**: Separately check whether `module-info.java`'s `requires` / `exports` / `opens` cover all packages in the project
5. **Compile failure short-circuit**: If compile verification has Critical issues, skip runtime verification and packaging verification (cannot run uncompiled code), noting "subsequent verification skipped due to compile failure" in the report

### Step 3: Runtime Verification

1. **Execute run command**:
   - With a display environment: `mvn javafx:run`
   - Without a display environment (CI): `mvn javafx:run -Dmonocle.platform=Headless -Dprism.order=sw` (requires Monocle dependency)
2. **Set timeout**: Default 30-second startup timeout; after timeout, terminate the process and record "startup timeout"
3. **Capture stdout and stderr**: Collect all `stdout` and `stderr` output
4. **Parse runtime exceptions**: Identify `Exception` / `Error` stacks, matching known JavaFX runtime exception patterns
5. **FXML load verification**: Check whether the output contains `LoadException` / `FXML load exception`
6. **CSS parse verification**: Check whether the output contains `CSS Error` / `WARNING: Could not resolve`
7. **Thread safety verification**: Check whether the output contains `IllegalStateException: Not on FX application thread`
8. **Exit code recording**: The process exit code, 0 for normal, non-0 for abnormal

### Step 4: Packaging Verification

1. **Execute JAR build**: `mvn package -DskipTests`
2. **Validate JAR artifact**: Check whether a JAR is generated in the `target/` directory and whether the JAR size is reasonable
3. **Execute jpackage**: Generate the jpackage command from the configuration in `pom.xml` or `jpackage-config.properties` and execute it
4. **Capture packaging output**: Collect jpackage's `stdout` and `stderr`
5. **Validate installer artifact**: Check whether installer files are generated (`.exe` / `.msi` / `.dmg` / `.deb` / `.rpm`) and whether the size is reasonable
6. **Toolchain missing diagnosis**: If jpackage fails, diagnose whether it is a toolchain missing issue (Inno Setup / WiX / Xcode tools)

### Step 5: Result Parsing and Severity Classification

1. Assign a level to each verification failure per the severity classification system
2. Deduplicate: merge compilation errors and runtime exceptions caused by the same root cause into one issue
3. Sort: arrange in descending severity order, within the same level sort by verification dimension (compile -> runtime -> packaging)

### Step 6: Generate Verification Report

1. Generate a structured verification report following the report template (see `report-templates/verification-report.md`)
2. The report includes: verification summary, issue list (with location / recommendation / fix handoff), verification result summary
3. Provide actionable fix recommendations for each issue, including corrected commands or configuration
4. The fix handoff field format is fully consistent with `javafx-code-reviewer`, for `javafx-developer` to directly consume

## Verification Dimensions

### 1. Compile Verification

Execute `mvn compile` (or `gradle compileJava`), parse the compiler output, and identify compilation errors and warnings. Default severity baseline: Critical.

**Check Items**:
- **Syntax compilation**: Whether all Java source files can pass `javac` compilation with no syntax errors
- **Dependency resolution**: Whether all Maven/Gradle dependencies can be resolved, with no `ClassNotFoundException` or `NoClassDefFoundError` compile-time errors
- **Module configuration**: Whether `module-info.java`'s `requires` / `exports` / `opens` declarations match the actual code
- **FXML compile association**: Whether the fully qualified name of the Controller class can be resolved by the class loader (whether the class pointed to by `fx:controller` in FXML exists)
- **Generics and types**: Whether generic usages such as `TableView<User>` are type-safe, whether `cellValueFactory` callback signatures are correct
- **Resource path compile-time check**: Whether the resource paths referenced by `getClass().getResource("/fxml/xxx.fxml")` exist in the compile output
- **Compilation warning triage**: Whether unused imports, deprecation warnings, and unchecked warnings affect running

> **Typical failure example**: `module-info.java` is missing `opens com.example.model to javafx.controls`; compilation passes but `PropertyValueFactory` reflection fails at runtime - this issue manifests as a "module opens missing" warning in the compile dimension and as a `LoadException` in the runtime dimension.

### 2. Runtime Verification

Execute `mvn javafx:run` (or `gradle run`), launch the JavaFX application, and capture startup process and runtime exceptions. Default severity baseline: Critical.

**Check Items**:
- **Application startup**: Whether `Application.launch()` can start normally, whether the `start()` method can complete, whether the main window can be displayed
- **FXML load**: Whether all `FXMLLoader.load()` calls can successfully parse FXML files, whether `fx:controller` can be instantiated, whether `fx:id` injection can complete
- **CSS parse**: Whether all CSS stylesheets can be loaded by the JavaFX CSS parser without errors, with no unsupported `var()` syntax and no undefined looked-up colors
- **Resource load**: Whether images, icons, and internationalization resource bundles can be loaded correctly, with no `NullPointerException` on paths
- **Module runtime**: Whether `module-info.java` satisfies all reflection requirements at runtime (`PropertyValueFactory`, FXML controller injection, `FXMLLoader` reflection access)
- **Thread safety runtime verification**: Whether a runtime `IllegalStateException: Not on FX application thread` is thrown
- **JavaFX 24+ native access**: Whether JavaFX 24+ projects configure `--enable-native-access=javafx.graphics`; whether startup reports `IllegalAccessError` when missing
- **Headless mode verification**: Whether a JavaFX application can be launched via the Monocle test framework in a CI environment (without a display)
- **Startup timeout detection**: Whether the application completes startup within a reasonable time (default 30-second timeout)
- **Exit code check**: The exit code is 0 when the application exits normally; a non-zero exit code indicates a runtime error

> **Typical failure example**: `module-info.java` compiles but is missing `opens com.example.controller to javafx.fxml`; at runtime `FXMLLoader` cannot reflectively instantiate the controller and throws `IllegalAccessException`.

### 3. Test Verification

Execute `mvn test` (or `gradle test`), parse test results, and identify test failures and coverage gaps. Default severity baseline: Major.

**Check Items**:
- **Test compilation**: Whether test source files in `src/test/java/` compile without errors
- **Test execution**: Whether all tests pass with 0 failures and 0 errors
- **Test coverage**: Whether critical paths are covered (warning if Controller/ViewModel has 0 tests)
- **FXML load test**: Whether a TestFX test verifies FXML loading and controller injection
- **UI interaction test**: Whether at least one TestFX test covers button click / form submit / table selection

> **Short-circuit**: If compile verification fails, skip test verification (cannot run tests on uncompilable code). If test verification fails, skip packaging verification (should not package untested code).

> **Typical failure example**: `Tests run: 5, Failures: 1, Errors: 0` — a test asserting `TableView` has initial data fails because the Controller's `initialize()` method doesn't load data.

### 4. Packaging Verification

Execute `mvn package` to generate a JAR, then execute `jpackage` to generate a native installer, verifying the packaging flow and artifact integrity. Default severity baseline: Major.

**Check Items**:
- **JAR build**: Whether `mvn package` can successfully generate an executable JAR, whether the JAR contains all necessary JavaFX module dependencies
- **Module path integrity**: Whether jpackage's `--module-path` includes the JavaFX SDK `lib` directory, whether `--add-modules` lists all required modules
- **Main class and main module**: Whether `--main-class` and `--main-module` (for modular projects) correctly point to the application entry point
- **Native access configuration**: Whether `--java-options "--enable-native-access=javafx.graphics"` is included in the packaging configuration (JavaFX 24+)
- **Platform toolchain**: Whether Windows has Inno Setup (exe) or WiX Toolset 4.x (msi) installed; whether macOS has Xcode command line tools; whether Linux has `dpkg-deb` or `rpm-build`
- **Icon format**: Whether the Windows icon is `.ico` (multi-size embedded), whether macOS is `.icns`, whether Linux is `.png`
- **Installer generation**: Whether `jpackage` can successfully generate installer files, whether the artifact size is reasonable (not 0 bytes)
- **Upgrade UUID**: Whether Windows packaging includes a valid `--win-upgrade-uuid` (UUID v4 format)

> **Typical failure example**: The `jpackage` command is missing `--add-modules javafx.controls,javafx.fxml`; the generated installer reports `Module javafx.controls not found` at runtime.

## Severity Classification

Reuses the four-level severity system of `javafx-code-reviewer`, ensuring consistent classification standards across the entire skill set.

| Level | Identifier | Definition | Typical Issues | Handling Recommendation |
|-------|------------|------------|----------------|-------------------------|
| Critical | Critical | Project cannot compile or application cannot start; must be fixed immediately | Compilation errors, FXML load failure, module configuration missing causing startup crash | Block delivery, fix first |
| Major | Major | Packaging failed or runtime risk exists, affects delivery but not development debugging | jpackage failure, startup timeout, JavaFX 24+ missing native access configuration | Fix within this iteration |
| Minor | Minor | Compilation warnings or non-blocking runtime warnings | Unused imports, deprecation warnings, CSS parse warnings | Recommend fixing |
| Info | Info | Optimization suggestions that improve build or run efficiency but do not affect functionality | Can use incremental compilation to speed up, can configure Monocle to optimize CI | Optimize when convenient |

### Escalation/De-escalation Conditions

| Check Item | Default Baseline | De-escalation Condition | Escalation Condition |
|------------|------------------|------------------------|----------------------|
| Compilation error | Critical | - (cannot be de-escalated; if compilation fails the project cannot run) | - |
| Module opens missing | Critical | Missing opens does not affect current functionality (e.g., PropertyValueFactory not used) -> Major | - |
| FXML load failure | Critical | - (runtime will always throw LoadException, cannot be de-escalated) | - |
| CSS parse error | Major | Only a warning that does not affect rendering (e.g., undefined looked-up color falls back to default) -> Minor | Causes the UI to fail to display -> Critical |
| Thread safety runtime exception | Critical | - (runtime will always throw IllegalStateException, cannot be de-escalated) | - |
| JavaFX 24+ missing native access | Critical | - (runtime will always report IllegalAccessError, cannot be de-escalated) | - |
| Startup timeout | Major | Timeout due to first-time loading of JavaFX modules (cold start), second startup is normal -> Minor | Timeout due to blocking call in `start()` -> Critical |
| jpackage failure | Major | Toolchain not installed (environment issue, not a code issue) -> Info | `module-path` misconfiguration causing the generated artifact to be unrunnable -> Critical |
| JAR artifact missing | Critical | - (packaging flow broken) | - |
| Compilation warning | Minor | - | Large number of unchecked warnings may mask real type errors -> Major |

**Classification constraints**:
- Each issue may move at most one level; cross-level jumps are prohibited
- Check items marked "cannot be de-escalated" must retain their default baseline even if the impact is minor
- When escalating or de-escalating, the triggering condition must be noted in the report's "Escalation/De-escalation Note" field

## Verification Report Format

After verification is complete, output a structured report containing three parts: verification summary, issue list, and verification result summary. The report format is isomorphic with `javafx-code-reviewer`'s review report, with the fix handoff field fully consistent, ensuring `javafx-developer` can consume both reports using the same logic.

### Report Structure

```
# JavaFX Verification Report

## Verification Summary
- Verification Mode: [Full / Incremental / Targeted Dimension]
- Verification Scope: [List of verification dimensions executed]
- Environment: JDK [version] / Maven [version] / JavaFX [version] / OS [platform]
- Modular: [Yes / No]
- Verification Commands: [List of commands actually executed]
- Total Issues Found: X (Critical: a / Major: b / Minor: c / Info: d)
- Verification Conclusion: [Pass / Conditional Pass / Fail]

## Issue List

### [Critical] Issue Title
- **Problem Description**: Specific description of the verification failure
- **Verification Dimension**: [Compile Verification / Runtime Verification / Packaging Verification]
- **Code Location**: `file path:line number` (if applicable)
- **Error Output**:
  ```
  Actual compiler/runtime/jpackage output snippet
  ```
- **Root Cause Analysis**: Explain why verification failed
- **Fix Recommendation**: Explain how to fix it
- **Corrected Example**:
  ```java
  // Corrected code or configuration
  ```
- **Rule Reference**: `references/document name -- Check Item title`
- **Escalation/De-escalation Note**: If severity deviates from the default baseline, note the triggering condition; if not, fill "None"
- **Fix Handoff**:
  - `target_file: file path`
  - `target_lines: start line-end line`
  - `fix_type: [replace / insert / delete]`
  - `fix_priority: [1-N]` (fix priority, 1=highest)

### [Major] ... (same structure)

## Verification Result Summary
| Dimension | Check Items | Passed | Failed | Skipped | Pass Rate |
|-----------|-------------|--------|--------|---------|-----------|
| Compile Verification | 7 | [N] | [N] | [N] | [N%] |
| Runtime Verification | 10 | [N] | [N] | [N] | [N%] |
| Packaging Verification | 8 | [N] | [N] | [N] | [N%] |
| **Total** | **[N]** | **[N]** | **[N]** | **[N]** | **[N%]** |

## Fix Handoff Summary
| Priority | Severity | Dimension | File | Lines | Fix Type | Issue Summary |
|----------|----------|-----------|------|-------|----------|---------------|
| 1 | Critical | Compile Verification | `file path` | `start-end` | replace | [issue summary] |
| 2 | Critical | Runtime Verification | `file path` | `start-end` | insert | [issue summary] |
| ... | ... | ... | ... | ... | ... | ... |
```

### Report Language Strategy

- **Follow skill version**: The Chinese skill outputs Chinese reports; the English skill outputs English reports
- **Code and identifiers remain as-is**: Regardless of report language, code snippets, file paths, class names, API names, and command lines remain in English without translation
- **Error output remains as-is**: The raw output of the compiler / runtime / jpackage is kept verbatim without translation

### Fix Handoff Field Description

The fix handoff field is fully consistent with `javafx-code-reviewer` and is key to achieving the "generate -> review -> verify -> fix" closed loop:

- `fix_type=replace`: Replace the code segment specified by `target_lines` with the "Corrected Example"
- `fix_type=insert`: Insert the "Corrected Example" after `target_lines`
- `fix_type=delete`: Delete the code segment specified by `target_lines` (no corrected example)
- `fix_priority`: Fix priority sorted by severity + verification dimension, 1 is highest, for ordering during batch fixes

When `javafx-developer` consumes the verification report, it can directly execute fixes item by item in `fix_priority` order, with no additional format conversion required.

## Constraints

### Execution Safety

1. **Command whitelist**: Only execute build-related commands such as `mvn`, `gradle`, `jpackage`, `java -version`, `mvn -version`; do not execute arbitrary system commands
2. **Timeout protection**: All commands set timeouts (compile 5 minutes, run 30 seconds, packaging 10 minutes); terminate the process after timeout
3. **No side effects**: Do not modify user project files (only read and execute); fixes are performed by `javafx-developer`
4. **Sandbox awareness**: When packaging verification involves system installation, prompt the user to confirm or execute in a sandbox environment

### Environment Compatibility

1. **Build tool detection**: Automatically detect Maven or Gradle and select the corresponding command
2. **Cross-platform**: Supports Windows / macOS / Linux; jpackage verification selects the output type based on the platform
3. **Headless support**: When a CI environment has no display, use the Monocle framework for headless run verification
4. **JavaFX version awareness**: Dynamically adjust verification items based on the JavaFX version used by the project (e.g., JavaFX 24+ checks `--enable-native-access`)

## Loop Orchestration Protocol

This protocol is shared across `javafx-developer`, `javafx-code-reviewer`, and `javafx-runner`. It defines the automated closed-loop cycle: **generate → review → verify → fix → re-verify**, until the quality gate passes or termination conditions are met.

### Runner's Role in the Loop

`javafx-runner` occupies the **verify** stage of the loop:
- **Round 1**: Full verification — environment detection + compile + runtime + packaging
- **Round 2+**: Targeted verification — compile always; runtime/packaging only if fixes touch related files (identified by `target_file` in the fix handoff)
- **Short-circuit**: If compile verification fails, skip runtime and packaging verification (cannot run uncompiled code)

### Loop State Machine

```
[Start] → Generating → Reviewing → (reviewer Pass?) → Verifying → (runner Pass?) → [Delivered]
                          ↓ No                          ↓ No
                       Fixing ←────────────────────── Fixing
                          ↓
                     Re-Reviewing (incremental)
```

### Loop Rules

| Rule | Definition | Termination |
|------|-----------|-------------|
| Max rounds | Fix → verify cycle loops at most 3 rounds | At 3 rounds without pass → pause, report to user |
| Re-review strategy | Round 1: full re-review; Round 2+: incremental re-review (only dimensions touched by fixes) | Incremental re-review checks only fix-affected dimensions |
| Re-verify strategy | Every round: compile verification mandatory; runtime/packaging based on fix scope | Compile failure short-circuits: skip runtime and packaging |
| Convergence detection | Compare current round issue count with previous round | 2 consecutive non-converging rounds → pause, report to user |
| User intervention points | Max rounds reached / non-converging / unfixable issues | Pause with current state report |

### Quality Gate (Combined)

The loop terminates as **Pass** only when BOTH reviewer and runner pass:

| Reviewer Conclusion | Runner Conclusion | Overall | Action |
|---------------------|-------------------|---------|--------|
| Pass | Pass | **Pass** | Deliver, exit loop |
| Pass | Conditional/Fail | **Fail** | Fix runner issues, continue loop |
| Conditional/Fail | Pass | **Fail** | Fix reviewer issues, continue loop |
| Conditional/Fail | Conditional/Fail | **Fail** | Fix both, continue loop |

**Priority rule**: When reviewer is Fail, fix reviewer issues first (static issues are usually root causes; fixing them may resolve runtime issues too).

### Individual Gate Criteria (Runner)

- **Pass**: No Critical or Major issues, all check items pass (or skipped items are documented), pass rate >= 80%
- **Conditional Pass**: Has Major but no Critical, all Major issues have clear fix plans; runtime verification passes but packaging verification has non-blocking issues
- **Fail**: Has Critical issues (compilation errors, startup failures, etc.), must be fixed before delivery

## Runtime Findings Feedback Protocol

This protocol defines how `javafx-runner` feeds runtime-discovered issue patterns back to `javafx-code-reviewer`'s static rule library, enabling skill set self-evolution.

### When to Generate Feedback

During runtime/test verification, when runner discovers an issue pattern that:
- Is NOT covered by any existing reviewer check item
- Occurs repeatedly (>= 2 occurrences in the same project)
- Has a clear, describable code pattern (not a one-off environment issue)

### Feedback Fields

Each finding in the report's **Runtime Findings Feedback** section contains:

| Field | Description |
|-------|-------------|
| `pattern` | Description of the recurring runtime issue pattern |
| `runner_check` | Which runner check item detected this |
| `suggested_reviewer_rule.target_document` | Which reviewer references/ document should receive the new rule |
| `suggested_reviewer_rule.suggested_check_item` | Proposed title for the new check item |
| `suggested_reviewer_rule.description` | What the new check item should verify |
| `suggested_reviewer_rule.suggested_severity` | Proposed severity baseline |
| `evidence.occurrences` | How many times this pattern was found |
| `evidence.sample_stack_trace` | Representative stack trace |
| `evidence.affected_files` | List of files where the pattern was found |

### Feedback Processing Flow

1. **Capture**: Runner detects a runtime exception pattern, records exception type, stack trace, and trigger context
2. **Attribution**: Runner analyzes the root cause and determines whether it is a "novel pattern not covered by reviewer rules"
3. **Suggest**: Runner outputs `suggested_reviewer_rule` in the report with proposed check item description and severity
4. **Review**: Maintainer (user or skill author) reviews the suggestion and decides whether to adopt it into reviewer's `references/` documents
5. **Update**: If adopted, update the corresponding reviewer `references/` document with the new check item — next static review cycle will cover this pattern

### Design Constraints

- Feedback is **advisory, not automatic**: suggestions require maintainer review before adoption (prevents rule quality degradation from auto-generated false positives)
- If all runtime issues were already covered by existing reviewer rules, state explicitly: "No novel runtime patterns found"
- Feedback section is omitted entirely when no novel patterns exist (keep report concise)

## Reference Documents

For in-depth criteria, refer to the following documents in the `references/` directory:

- `references/compile-verification.md` - Compile verification rules and error pattern library <- developer: Quality checklist - syntax check
- `references/runtime-verification.md` - Runtime verification rules and exception pattern library <- reviewer: Thread safety dimension (dynamically verifying static conclusions)
- `references/packaging-verification.md` - Packaging verification rules and platform toolchain <- developer: Packaging chapter - jpackage command
- `references/test-verification.md` - Test verification rules and failure pattern library <- developer: Quality checklist - test coverage
- `references/environment-setup.md` - Environment detection and Monocle headless configuration
- `EVALUATE.md` - Evaluation test case set, for quantifying skill output quality

## Report Template

Reusable skeleton template in the `report-templates/` directory:

- `report-templates/verification-report.md` - Verification report skeleton template (reusable)
