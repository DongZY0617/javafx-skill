---
name: javafx-runner
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
  version: "1.1"
triggers:
  - verify
  - compile
  - run
  - test execution
  - package
  - deploy verification
depends_on:
  - javafx-developer
consumes_from:
  - javafx-developer (source code)
produces_for:
  - javafx-developer (fix handoff report)
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
| Static Analysis Verification | `static-analysis-tools.md` (developer reference) | JDK + Maven/Gradle + SpotBugs/PMD/Checkstyle | reviewer: Static Analysis Tool Findings dimension (deterministic baseline) |
| Runtime Verification | `runtime-verification.md` | JDK + JavaFX runtime + possibly a display | reviewer: Thread safety dimension (dynamically verifying static conclusions) |
| Visual Preview | `visual-preview.md` | JDK + JavaFX runtime + display or Monocle | developer: Output format - UI preview |
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

1. **Determine compile mode**: Before executing the compile command, determine whether incremental or full compilation should be used:
   - **Detect existing build output**: Check if `target/classes/` exists. If it does and the newest file timestamp in `target/classes/` is newer than the newest file timestamp in `src/`, use **incremental compilation mode** (Maven's default behavior — only recompiles changed source files)
   - **Clean compile override**: If `.loop-config.json` exists in the project root with `"clean_compile": true`, execute `mvn clean compile -q` (full rebuild from scratch). This deletes `target/` first, forcing all source files to be recompiled
   - **Default behavior**: Without configuration override, **always use `mvn compile -q`** (incremental). Never use `mvn clean compile` by default — it wastes time by recompiling unchanged files
   - **Record compile mode**: Note `"incremental"` or `"full"` in the verification report for traceability
2. **Execute compile command**: `mvn compile -q` (incremental, default) or `mvn clean compile -q` (full, only when `.loop-config.json` has `clean_compile: true`) — quiet mode outputs only errors and warnings. For Gradle: `gradle compileJava --quiet` (incremental) or `gradle clean compileJava --quiet` (full)
3. **Parse compiler output**: Parse by error format `[ERROR] /path/File.java:[line,col] error message`
4. **Classify and record**: Compilation errors (Critical), compilation warnings (Minor), dependency resolution failures (Critical)
5. **Module configuration validation**: Separately check whether `module-info.java`'s `requires` / `exports` / `opens` cover all packages in the project
6. **POM change detection**: If `pom.xml` (or `build.gradle`) was modified since the last round (compare timestamp against `last_fix_handoff`), force a full compilation regardless of config — dependency changes may affect all source files
7. **Compile failure short-circuit**: If compile verification has Critical issues, skip runtime verification and packaging verification (cannot run uncompiled code), noting "subsequent verification skipped due to compile failure" in the report

### Step 2.5: Static Analysis Verification

After compilation succeeds, execute SpotBugs, PMD, and Checkstyle to detect bug patterns, code quality issues, and style violations. This dimension provides deterministic analysis that complements `javafx-code-reviewer`'s LLM-based review.

1. **Check tool configuration**: Verify that the project's `pom.xml` includes the SpotBugs, PMD, and Checkstyle plugin configurations (see `javafx-developer/references/static-analysis-tools.md`). If the plugins are not configured, skip this step with a note: "Static analysis skipped — no SpotBugs/PMD/Checkstyle plugins configured in pom.xml. Consider adding them via javafx-developer template."
2. **Execute static analysis commands**:
   - Maven: `mvn spotbugs:check pmd:check checkstyle:check` (runs all three in sequence)
   - Gradle: `gradle spotbugsMain pmdMain checkstyleMain` (equivalent)
   - Set a 5-minute timeout for the combined execution
3. **Parse XML reports**:
   - SpotBugs: Parse `target/spotbugsXml.xml` — extract `BugInstance` elements with `type`, `priority` (1=High, 2=Medium, 3=Low), `category`, `Class.classname`, `Method.name`, `SourceLine.start`
   - PMD: Parse `target/pmd.xml` — extract `violation` elements with `rule`, `ruleset`, `priority` (1-5), `beginline`, `file.name`
   - Checkstyle: Parse `target/checkstyle-result.xml` — extract `error` elements with `source` (rule ID), `severity` (error/warning), `line`, `file.name`
4. **Map to unified issue structure**: Convert each tool finding to the unified format with `tool`, `rule_id`, `severity`, `category`, `source_file`, `line_number`, `ast_node_signature`, `message`, `fix_suggestion`
   - **Priority to severity mapping**: SpotBugs 1→Major, 2→Minor, 3→Info; PMD 1-2→Major, 3→Minor, 4-5→Info; Checkstyle error→Minor, warning→Info
   - Tool findings never map to Critical — Critical is reserved for compilation/runtime issues
5. **Extract AST signatures**: For each finding in a `.java` file, extract the `ast_node_signature` from the tool's reported class/method context (SpotBugs provides `Class.classname` + `Method.name`; PMD and Checkstyle provide `file.name` + `line` — infer enclosing method from source)
6. **Write unified findings file**: Output `target/static-analysis-findings.json` containing the merged, deduplicated findings array — this file is consumed by `javafx-code-reviewer` as Dimension 10 (Static Analysis Tool Findings)
7. **Record in verification report**: Include a "Static Analysis Verification" section in the verification report with per-tool issue counts, top findings, and the unified findings file path

**Check Items**:
- **SpotBugs execution**: Whether `mvn spotbugs:check` completes without plugin errors (config issues, missing dependencies)
- **PMD execution**: Whether `mvn pmd:check` completes without plugin errors
- **Checkstyle execution**: Whether `mvn checkstyle:check` completes without plugin errors
- **Report generation**: Whether all three XML reports are generated in `target/`
- **Finding classification**: Whether findings are correctly mapped to severity levels
- **AST signature extraction**: Whether `ast_node_signature` is extracted for each Java finding

> **Short-circuit**: If compile verification fails, skip static analysis (SpotBugs needs compiled bytecode). Static analysis findings do NOT short-circuit runtime verification — they are recorded as issues but don't block runtime testing.

> **Configuration-aware**: If `.loop-config.json` has `"static_analysis": false`, skip this step entirely. If `"static_analysis_tools": ["spotbugs"]` is specified, run only the listed tools.

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
9. **UI screenshot capture**: If startup succeeds (exit code 0 or application still running within timeout), capture a screenshot of the main window. Use JavaFX 26+ Headless Preview API when available; otherwise use Monocle + `Robot` API or AWT `Robot` for display environments. Save as `target/ui-preview.png` and embed in the verification report

### Step 4: Packaging Verification

1. **Execute JAR build**: `mvn package -DskipTests`
2. **Validate JAR artifact**: Check whether a JAR is generated in the `target/` directory and whether the JAR size is reasonable
3. **Execute jpackage**: Generate the jpackage command from the configuration in `pom.xml` or `jpackage-config.properties` and execute it
4. **Capture packaging output**: Collect jpackage's `stdout` and `stderr`
5. **Validate installer artifact**: Check whether installer files are generated (`.exe` / `.msi` / `.dmg` / `.deb` / `.rpm`) and whether the size is reasonable
6. **Toolchain missing diagnosis**: If jpackage fails, diagnose whether it is a toolchain missing issue (Inno Setup / WiX / Xcode tools)
7. **Cross-platform configuration completeness**: Since `jpackage` cannot cross-compile, the actual build/installer above runs only for the **current** platform. For the other two platforms, inspect the packaging configuration (`jpackage-config.properties` / build scripts / CI workflow) to confirm each defines its output type, icon (`.ico` / `.icns` / `.png`), and platform-specific metadata (Windows `--win-upgrade-uuid`, macOS `--mac-package-identifier`, Linux `--linux-deb-maintainer`). Record these as "validated by configuration completeness only; not executed on current platform"

### Step 5: Result Parsing and Severity Classification

1. Assign a level to each verification failure per the severity classification system
2. Deduplicate: merge compilation errors and runtime exceptions caused by the same root cause into one issue
3. Sort: arrange in descending severity order, within the same level sort by verification dimension (compile -> static analysis -> runtime -> test -> packaging)

### Step 6: Generate Verification Report

1. Generate a structured verification report following the report template (see `report-templates/verification-report.md`)
2. The report includes: verification summary, issue list (with location / recommendation / fix handoff), verification result summary
3. Provide actionable fix recommendations for each issue, including corrected commands or configuration
4. The fix handoff field format is fully consistent with `javafx-code-reviewer`, for `javafx-developer` to directly consume
5. **Extract AST node signatures**: For each issue in a `.java` file, extract the `ast_node_signature` from the compiler error location or runtime stack trace:
   - Parse the compiler output `[ERROR] /path/File.java:[line,col] message` to identify the file and line, then determine the enclosing method/field/class
   - For runtime exceptions, use the stack trace's top application frame to identify the enclosing method
   - If the issue is inside a method body → extract `{package}.{Class}#{methodName}({paramTypes})`
   - If the issue is a field declaration → extract `{package}.{Class}#{fieldName}`
   - If the issue is at class level → extract `{package}.{Class}`
   - If the file is not a Java source file (FXML, CSS, `module-info.java`) → set to `null`
   - See `javafx-orchestrator/SKILL.md` → Fix Handoff Format → AST Anchor Format for the full extraction specification

## Verification Dimensions

### 1. Compile Verification

Execute `mvn compile -q` (incremental, default) or `mvn clean compile -q` (full, when `.loop-config.json` has `clean_compile: true` or `pom.xml` changed), parse the compiler output, and identify compilation errors and warnings. Default severity baseline: Critical.

**Compile Mode**: The verification report records whether incremental or full compilation was used. Incremental mode (default for Round 2+) only recompiles changed files, reducing compile time by 60-80%. Full mode is used for Round 1 or when explicitly configured.

**Check Items**:
- **Syntax compilation**: Whether all Java source files can pass `javac` compilation with no syntax errors
- **Dependency resolution**: Whether all Maven/Gradle dependencies can be resolved, with no `ClassNotFoundException` or `NoClassDefFoundError` compile-time errors
- **Module configuration**: Whether `module-info.java`'s `requires` / `exports` / `opens` declarations match the actual code
- **FXML compile association**: Whether the fully qualified name of the Controller class can be resolved by the class loader (whether the class pointed to by `fx:controller` in FXML exists)
- **Generics and types**: Whether generic usages such as `TableView<User>` are type-safe, whether `cellValueFactory` callback signatures are correct
- **Resource path compile-time check**: Whether the resource paths referenced by `getClass().getResource("/fxml/xxx.fxml")` exist in the compile output
- **Compilation warning triage**: Whether unused imports, deprecation warnings, and unchecked warnings affect running

> **Typical failure example**: `module-info.java` is missing `opens com.example.model to javafx.controls`; compilation passes but `PropertyValueFactory` reflection fails at runtime - this issue manifests as a "module opens missing" warning in the compile dimension and as a `LoadException` in the runtime dimension.

### 2. Static Analysis Verification

Execute SpotBugs, PMD, and Checkstyle to detect bug patterns, code quality issues, and style violations. This dimension provides deterministic analysis that complements `javafx-code-reviewer`'s LLM-based review. See `static-analysis-tools.md` reference (in javafx-developer) for configuration details, rule sets, and report parsing.

**Check Items**:
- **SpotBugs execution**: Whether `mvn spotbugs:check` completes without plugin errors (config issues, missing dependencies)
- **PMD execution**: Whether `mvn pmd:check` completes without plugin errors
- **Checkstyle execution**: Whether `mvn checkstyle:check` completes without plugin errors
- **Report generation**: Whether all three XML reports are generated in `target/` (`spotbugsXml.xml`, `pmd.xml`, `checkstyle-result.xml`)
- **Finding classification**: Whether findings are correctly mapped to severity levels (Major/Minor/Info)
- **Unified findings output**: Whether `target/static-analysis-findings.json` is generated with unified issue structure for reviewer consumption

**Default severity baseline**: Minor (tool findings are code quality issues, not runtime failures — Major for SpotBugs High-priority findings)

> **Short-circuit**: If compile verification fails, skip static analysis (SpotBugs needs compiled bytecode). Static analysis findings do NOT short-circuit runtime verification.

### 3. Runtime Verification

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
- **UI screenshot capture**: After successful application startup, capture a screenshot of the main window for visual preview. See `references/visual-preview.md` for capture methods by JavaFX version and environment

> **Typical failure example**: `module-info.java` compiles but is missing `opens com.example.controller to javafx.fxml`; at runtime `FXMLLoader` cannot reflectively instantiate the controller and throws `IllegalAccessException`.

### 4. Test Verification

Execute `mvn test` (or `gradle test`), parse test results, and identify test failures and coverage gaps. Default severity baseline: Major.

**Check Items**:
- **Test compilation**: Whether test source files in `src/test/java/` compile without errors
- **Test execution**: Whether all tests pass with 0 failures and 0 errors
- **Test coverage**: Whether critical paths are covered (warning if Controller/ViewModel has 0 tests)
- **JaCoCo coverage report**: Execute `mvn test jacoco:report` to generate coverage report; parse `target/site/jacoco/jacoco.xml` for line and branch coverage metrics. Threshold: line coverage >= 60% on Controller and ViewModel classes. Below threshold: record as Major issue with specific uncovered method names. See `references/test-coverage-gate.md` for threshold configuration and reporting rules
- **FXML load test**: Whether a TestFX test verifies FXML loading and controller injection
- **UI interaction test**: Whether at least one TestFX test covers button click / form submit / table selection
- **Database integration test**: Whether repository/DAO methods (CRUD, query, transaction) are covered by integration tests; whether a `@DataJpaTest` / `@MybatisTest` slice test or a TestFX test exercising a real (e.g., H2 in-memory) datasource validates Entity persistence, Repository/Mapper queries, and `@Transactional` Service save flows end-to-end; whether Flyway migrations run cleanly against a test database; whether null-handling for primitive-typed JavaFX Properties is asserted (insert new entity with null id). Cross-reference: developer `references/database-integration.md` section 8 (Common Pitfalls) for the patterns these tests must guard against

> **Short-circuit**: If compile verification fails, skip test verification (cannot run tests on uncompilable code). If test verification fails, skip packaging verification (should not package untested code).

> **Typical failure example**: `Tests run: 5, Failures: 1, Errors: 0` — a test asserting `TableView` has initial data fails because the Controller's `initialize()` method doesn't load data.

### 5. Packaging Verification

Execute `mvn package` to generate a JAR, then execute `jpackage` to generate a native installer, verifying the packaging flow and artifact integrity. Default severity baseline: Major.

> **Cross-platform validation scope**: `jpackage` cannot cross-compile, so actual packaging can only be executed and verified on the **current** platform. For the other two platforms, verification is limited to **configuration completeness** — confirming that the per-platform jpackage command, icon, and metadata are all present and well-formed in the project's packaging configuration (e.g., `jpackage-config.properties`, build scripts, or CI matrix). The check items below distinguish between "executed" verification (current platform) and "configuration-only" verification (non-current platforms).

**Check Items**:
- **JAR build**: Whether `mvn package` can successfully generate an executable JAR, whether the JAR contains all necessary JavaFX module dependencies
- **Module path integrity**: Whether jpackage's `--module-path` includes the JavaFX SDK `lib` directory, whether `--add-modules` lists all required modules
- **Main class and main module**: Whether `--main-class` and `--main-module` (for modular projects) correctly point to the application entry point
- **Native access configuration**: Whether `--java-options "--enable-native-access=javafx.graphics"` is included in the packaging configuration (JavaFX 24+)
- **Platform toolchain**: Whether Windows has Inno Setup (exe) or WiX Toolset 4.x (msi) installed; whether macOS has Xcode command line tools; whether Linux has `dpkg-deb` or `rpm-build`
- **Icon format**: Whether the Windows icon is `.ico` (multi-size embedded), whether macOS is `.icns`, whether Linux is `.png`
- **Installer generation**: Whether `jpackage` can successfully generate installer files, whether the artifact size is reasonable (not 0 bytes)
- **Upgrade UUID**: Whether Windows packaging includes a valid `--win-upgrade-uuid` (UUID v4 format)
- **Cross-platform configuration completeness**: Whether the project defines jpackage commands/configuration for **all three** platforms (Windows `msi`/`exe`, macOS `dmg`/`pkg`, Linux `deb`/`rpm`). For each non-current platform, verify by configuration inspection that its output type, icon reference, and platform-specific metadata are present — these cannot be executed on the current OS but must be validated so the CI matrix will succeed on the target runner
- **Platform-specific icon format validation**: Whether each platform's icon reference points to the correct format and the file exists in the repository — Windows must reference `.ico`, macOS `.icns`, Linux `.png`. A missing or wrong-format icon for a non-current platform would fail that platform's CI build, so it must be caught by configuration inspection here
- **Platform-specific metadata**: Whether each platform's required metadata is present and well-formed — Windows `--win-upgrade-uuid` (stable UUID v4, reused across versions), macOS `--mac-package-identifier` (reverse-DNS, matches signing profile), Linux `--linux-deb-maintainer` (valid email) and `--linux-package-name`. Missing metadata for a non-current platform is recorded as Major with a recommendation to complete it before triggering the multi-platform matrix build

> **Verification split**: Executable checks (JAR build, module path, toolchain, installer generation, current-platform icon/UUID) are validated by actually running commands. Configuration-only checks (non-current-platform icon format, metadata, and the three-platform completeness) are validated by inspecting `jpackage-config.properties` / build scripts / CI workflow — these are recorded as Major when incomplete, with the note "validated by configuration completeness only; not executed on current platform".

> **Typical failure example**: The `jpackage` command is missing `--add-modules javafx.controls,javafx.fxml`; the generated installer reports `Module javafx.controls not found` at runtime. A cross-platform variant: the project only defines a Windows `msi` command and omits macOS/Linux config — the "Cross-platform configuration completeness" check fails with "missing macOS dmg and Linux deb/rpm configuration".

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
- Compile Mode: [Incremental / Full]
- Verification Scope: [List of verification dimensions executed]
- Environment: JDK [version] / Maven [version] / JavaFX [version] / OS [platform]
- Modular: [Yes / No]
- Verification Commands: [List of commands actually executed]
- Total Issues Found: X (Critical: a / Major: b / Minor: c / Info: d)
- Verification Conclusion: [Pass / Conditional Pass / Fail]

## Issue List

### [Critical] Issue Title
- **Problem Description**: Specific description of the verification failure
- **Verification Dimension**: [Compile Verification / Static Analysis Verification / Runtime Verification / Test Verification / Packaging Verification]
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
  - `code_fingerprint: sha256 hash` (hash of the problematic code snippet, normalized: whitespace-trimmed, for drift-resistant matching)
  - `anchor_pattern: context signature` (2 lines before + 2 lines after the target, for secondary location when fingerprint match is ambiguous)
  - `ast_node_signature: com.example.Class#method(params)` (AST-level anchor — fully qualified method/field/class signature, for refactor-resistant matching when code has been moved; `null` for non-Java files)

### [Major] ... (same structure)

## Verification Result Summary
| Dimension | Check Items | Passed | Failed | Skipped | Pass Rate |
|-----------|-------------|--------|--------|---------|-----------|
| Compile Verification | 7 | [N] | [N] | [N] | [N%] |
| Static Analysis Verification | 6 | [N] | [N] | [N] | [N%] |
| Runtime Verification | 10 | [N] | [N] | [N] | [N%] |
| Test Verification | 8 | [N] | [N] | [N] | [N%] |
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
- `code_fingerprint`: SHA-256 hash of the problematic code snippet (normalized: whitespace-trimmed, leading/trailing spaces removed). Used for drift-resistant matching — if line numbers have shifted due to prior fixes, the fingerprint still identifies the correct code location
- `anchor_pattern`: Signature of surrounding context (2 lines before + 2 lines after the target lines, concatenated and normalized). Used as a secondary locator when the fingerprint match is ambiguous or multiple matches exist
- `ast_node_signature`: AST-level anchor in the format `{package}.{Class}#{methodName}({paramTypes})` for method-level issues, `{package}.{Class}#{fieldName}` for field-level issues, or `{package}.{Class}` for class-level issues. Extracted from the compiler error location or runtime stack trace — the enclosing AST node of the problematic code. Provides refactor-resistant matching — when methods are moved to different files or classes are renamed, the developer's Fix Consumption Protocol can locate the code by signature search. Set to `null` for non-Java files (FXML, CSS, `module-info.java`). See `javafx-orchestrator/SKILL.md` → Fix Handoff Format → AST Anchor Format for the full specification

When `javafx-developer` consumes the verification report, it can directly execute fixes item by item in `fix_priority` order, with no additional format conversion required.

### Dual Output Format (Markdown + JSON)

The runner outputs reports in **two formats simultaneously** by default:

1. **Markdown report** (`verification-report.md`) — human-readable, for developer review and documentation
2. **JSON report** (`verification-report.json`) — machine-readable, for `javafx-developer` Fix Consumption, CI/CD quality gates, and IDE plugin integration

The JSON format is defined by the schema in `report-templates/report-schema.json`. It contains the same information as the Markdown report but in a structured format with a standalone `fix_handoffs` array for direct programmatic consumption. Key fields:

- `summary.conclusion`: `Pass`, `Conditional Pass`, or `Fail` — CI/CD can use `jq .summary.conclusion verification-report.json` for quality gate decisions
- `summary.dimensions`: Per-dimension conclusions (compile/runtime/test/packaging) with issue counts and JaCoCo coverage data
- `fix_handoffs[]`: Standalone array sorted by `fix_priority`, each entry includes `target_file`, `target_lines`, `fix_type`, `fix_priority`, `code_fingerprint`, `anchor_pattern`, `ast_node_signature`, `corrected_example`, `issue_id`, and `severity`
- `jacoco_report`: Coverage summary with uncovered methods list (present if JaCoCo report was generated)
- `ui_preview`: Screenshot capture result metadata
- `loop_state`: Current loop state snapshot for orchestrator synchronization

**Output format control**: If `.loop-config.json` exists in the project root with `"output_format": "json"`, output only the JSON report; if `"output_format": "markdown"`, output only the Markdown report. Default (no config file or `"output_format": "both"`) outputs both formats.

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

> **Authoritative source**: When operating within an orchestrated loop, see `javafx-orchestrator/SKILL.md` for the authoritative definitions of:
> - **Loop State Machine** (state transitions, parallel execution, fix cycle)
> - **Loop Rules** (max rounds, re-review/re-verify strategy, convergence detection)
> - **Combined Quality Gate** (reviewer + runner pass/fail matrix, priority rule)
> - **Loop State JSON** format (`.loop-state.json` schema with all fields)
> - **Serialization Triggers** (who writes what, when, and with what field isolation)
> - **State Recovery Protocol** (cross-session recovery, stale handling)
> - **Fix Handoff Format** (field definitions including `ast_node_signature`)
>
> The sections below describe only the **runner's role and responsibilities** within the loop — the minimal subset needed for standalone operation.

### Runner's Role in the Loop

`javafx-runner` occupies the **verify** stage of the loop:
- **Round 1**: Full verification — environment detection + compile + runtime + packaging
- **Round 2+**: Targeted verification — compile always (incremental mode by default, see [Incremental Compilation](#incremental-compilation) below); runtime/packaging only if fixes touch related files (identified by `target_file` in the fix handoff)
- **Short-circuit**: If compile verification fails, skip runtime and packaging verification (cannot run uncompiled code)

<a id="incremental-compilation"></a>
#### Incremental Compilation

From Round 2 onward, the runner leverages **incremental compilation** to avoid recompiling unchanged source files:

1. **Default mode**: `mvn compile -q` (incremental) — Maven's compiler plugin only recompiles source files whose timestamps are newer than their corresponding `.class` files in `target/classes/`. This reduces Round 2+ compile time by 60-80% compared to full compilation
2. **Full compile triggers**: A full compilation (`mvn clean compile -q`) is automatically triggered when:
   - `.loop-config.json` has `"clean_compile": true` (user explicitly requests full rebuild)
   - `pom.xml` or `build.gradle` was modified since the last round (dependency changes may affect all files)
3. **Compile mode recording**: The compile mode (`"incremental"` or `"full"`) is recorded in:
   - The verification report's Compile Verification dimension
   - The JSON report's `summary.dimensions.compile.compile_mode` field
   - The Loop State's `rounds[current_round].compile_mode` field
4. **Safety guarantee**: Incremental compilation produces identical results to full compilation for the changed files — Maven's incremental compiler correctly handles dependent file recompilation within the same module

### Individual Gate Criteria (Runner)

- **Pass**: No Critical or Major issues, all check items pass (or skipped items are documented), pass rate >= 80%, JaCoCo line coverage >= 60% on critical paths (Controller/ViewModel classes)
- **Conditional Pass**: Has Major but no Critical, all Major issues have clear fix plans; runtime verification passes but packaging verification has non-blocking issues
- **Fail**: Has Critical issues (compilation errors, startup failures, etc.), must be fixed before delivery

### Runner's Serialization Responsibilities

1. **Read state**: Before starting verification, check for `.loop-state.json`. If found, extract `current_round` and `last_fix_handoff` to determine verification scope. Also extract `rounds[current_round - 1].compile_mode` to detect if the previous round used incremental compilation
2. **Determine strategy**: Round 1 → Full Verification; Round 2+ → Targeted Verification (compile always via incremental mode; runtime/test/packaging based on fix scope)
3. **Determine compile mode**: Check `.loop-config.json` for `clean_compile` setting and `pom.xml` timestamp vs `last_fix_handoff` timestamp (see [Incremental Compilation](#incremental-compilation) for full logic)
4. **Short-circuit check**: If compile verification fails, skip runtime/test/packaging and record "subsequent verification skipped due to compile failure" in the state file
5. **Write result**: After completing verification, update **only** the `rounds[current_round].runner_result` field with conclusion, issue counts by severity, fix handoff count, and `compile_mode` (`"incremental"` or `"full"`). Do not modify `reviewer_result` or other fields (parallel write safety — reviewer writes to its own field concurrently)
6. **Set next action**: If both reviewer and runner have completed, set `status: "passed"` (if both pass) and archive the state file, or set `next_action: "fixing"` (if either fails); if only runner has completed, leave `next_action` unchanged (orchestrator will update after reviewer also completes)

> **Fix Handoff Format**: See `javafx-orchestrator/SKILL.md` → Fix Handoff Format for the authoritative field definitions. The runner generates Fix Handoffs with `target_file`, `target_lines`, `fix_type`, `fix_priority`, `code_fingerprint`, `anchor_pattern`, and `ast_node_signature` fields.

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
- `references/test-coverage-gate.md` - JaCoCo coverage threshold rules, pom.xml configuration, report parsing
- `references/visual-preview.md` - UI screenshot capture methods by JavaFX version and environment
- `references/environment-setup.md` - Environment detection and Monocle headless configuration

> **Cross-reference (developer skill)**: The Static Analysis Verification dimension cross-references the developer skill's `references/static-analysis-tools.md` — that document defines the SpotBugs/PMD/Checkstyle plugin configuration, JavaFX-tailored rule sets, report parsing formats, and the unified issue mapping structure. The document lives in the `javafx-developer/references/` directory, not this skill's `references/` directory.
- `EVALUATE.md` - Evaluation test case set, for quantifying skill output quality

> **Cross-reference (developer skill)**: The "Database integration test" check item in the Test Verification dimension cross-references the developer skill's `references/database-integration.md` (section 8 "Common Pitfalls") — that document defines the runtime patterns (UI-thread blocking DB calls, null-handling for primitive Properties, connection-pool leaks, transaction-boundary scope, Flyway migration failures) that the database integration tests must guard against. The document lives in the `javafx-developer/references/` directory, not this skill's `references/` directory.

## Report Template

## Report Templates

Reusable skeleton templates in the `report-templates/` directory:

- `report-templates/verification-report.md` - Verification report skeleton template (Markdown, human-readable)
- `report-templates/report-schema.json` - JSON schema for machine-readable report output (CI/CD, IDE, Fix Consumption)
