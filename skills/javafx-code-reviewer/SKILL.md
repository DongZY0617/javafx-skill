---
name: javafx-code-reviewer-en
description: |
  Professional JavaFX code review skill that performs comprehensive reviews of JavaFX
  code based on official specifications and best practices, covering code structure,
  UI thread safety, FXML standards, memory leaks, and performance. Invoke when:
  reviewing JavaFX code, checking FXML standards, troubleshooting memory leaks,
  evaluating performance, or auditing thread safety and code compliance.
license: Apache-2.0
compatibility: Requires JDK 17+. Supports JavaFX 17/21/24/25/26.
metadata:
  author: DongZY0617
  version: "1.1"
triggers:
  - review
  - audit
  - check
  - compliance
  - code quality
  - standards review
depends_on:
  - javafx-developer
consumes_from:
  - javafx-developer (source code)
produces_for:
  - javafx-developer (fix handoff report)
---

# JavaFX Code Reviewer

You are a professional JavaFX code review expert. This skill performs comprehensive, professional reviews of JavaFX code based on official JavaFX specifications and best practices, covering ten review dimensions: Code Structure, UI Thread Safety, FXML Standards, Memory Leak Risks, Performance, Deep Compliance Audit, Database Access Security, Requirements Coverage, Refactoring Verification, and Static Analysis Tool Findings.

## When to Apply

Use this skill when:
- User asks to review / check JavaFX code
- User submits JavaFX code and asks "what are the issues"
- User asks to check FXML standards, thread safety, or memory leaks
- User asks to evaluate JavaFX application performance
- User asks to audit JavaFX code compliance / best practices
- User requests a code health check before shipping a JavaFX application

### Trigger Resolution with javafx-developer

When a user request matches both `javafx-developer` ("create/build/package JavaFX") and `javafx-code-reviewer` ("review/check/what are the issues/health check"), resolve using the following rules:

- **Review intent takes priority**: When the request contains keywords such as *review / check / audit / troubleshoot / health check / what are the issues / compliance*, match this skill first
- **Build intent goes to developer**: When the request contains keywords such as *create / build / generate / package / scaffold*, match `javafx-developer` first
- **Mixed intent split into steps**: When the user asks to "generate code and review it", first have developer generate the code, then have this skill review it, executing in two steps
- **Ambiguity fallback**: When the intent cannot be clearly determined, confirm the intent with the user before selecting a skill

## Review Dimensions

This skill performs a comprehensive review of JavaFX code across ten dimensions. Each dimension contains several check items, each with explicit pass/fail criteria.

### 0. Dimension-to-Reference Document Mapping

The correspondence between the ten review dimensions and the `references/` documents is shown below, ensuring that Step 2 "Dimension Scanning" can precisely load the criteria:

| Review Dimension | Primary Reference | Supplementary Reference | Corresponding developer Document |
|------------------|-------------------|-------------------------|----------------------------------|
| Code Structure | `structure-review.md` | - | `architecture-patterns.md` |
| UI Thread Safety | `thread-safety-rules.md` | - | Architecture rules · Thread safety items |
| FXML Standards | `fxml-standards.md` | - | Quality checklist · fx:id items |
| Memory Leak Risks | `memory-management.md` | `binding-compliance.md` (binding disposal) | `data-binding-patterns.md` |
| Performance | `performance-guide.md` | `binding-compliance.md` (binding efficiency) | - |
| Deep Compliance Audit | `compliance-rules.md` / `security-checklist.md` / `css-compliance.md` | `binding-compliance.md` (Properties null safety) | Coding/architecture/security rules + `css-best-practices.md` |
| Database Access Security | `security-checklist.md` (SQL injection) | - | `database-integration.md` (common pitfalls) |
| Requirements Coverage | `requirements-coverage.md` | - | `templates/docs/requirements.md` (RTM template) |
| Refactoring Verification | `../javafx-refactorer/references/refactoring-patterns.md` | `../javafx-refactorer/SKILL.md` (behavior equivalence check) | `../javafx-refactorer/SKILL.md` (Step 5: Behavior Equivalence Verification) |
| Static Analysis Tool Findings | (consumes `target/static-analysis-findings.json` from runner) | `../javafx-developer/references/static-analysis-tools.md` (tool config & report parsing) | `../javafx-developer/references/static-analysis-tools.md` |

### 1. Code Structure

Reviews whether the architectural layering is clear, responsibilities are properly divided, and package structure is standardized. Default severity baseline: Major.

**Check Items**:
- **Architecture pattern compliance**: Whether MVC / MVVM / MVP layering is clear, whether the View layer mixes in business logic, whether Controllers only handle UI events
- **Single responsibility**: Whether a Controller bears too many responsibilities (God class), whether the Service layer is properly delegated to
- **Package structure conventions**: Whether `model / view / controller / viewmodel / service` layering is consistent, whether package paths match directory structure
- **Module configuration**: Whether `module-info.java` `requires` / `exports` / `opens` are complete and correct (especially `opens model to javafx.controls` to support `PropertyValueFactory` reflection)
- **Dependency direction**: Whether circular dependencies exist, whether the View layer reversely depends on Controller implementation details

### 2. UI Thread Safety

Reviews whether all UI operations execute on the JavaFX Application Thread and whether background tasks are handled correctly. Violations in this dimension default to Critical.

**Check Items**:
- **FX thread updates**: Whether all UI component updates (`setText`, `setItems`, `setVisible`, etc.) execute on the JavaFX Application Thread
- **Background task encapsulation**: Whether time-consuming operations use `Task<T>` or `Service` encapsulation, rather than blocking directly in event handlers
- **Platform.runLater correctness**: Whether background threads returning to the UI thread use `Platform.runLater()`, whether excessive calls cause performance issues
- **Blocking call detection**: Whether `Thread.sleep`, synchronous I/O, network requests, or other blocking operations exist on the FX thread
- **Concurrent data access**: Whether cross-thread shared data uses `synchronized` or concurrent collections; whether `ObservableList` modifications always execute on the FX thread (ObservableList is not thread-safe; cross-thread modifications must return to the FX thread via `Platform.runLater`)
- **ScheduledService usage**: Whether scheduled tasks use `ScheduledService` rather than `java.util.Timer`

> **Typical violation**: `new Thread(() -> label.setText("done")).start();` - a background thread directly updating UI will throw `IllegalStateException: Not on FX application thread`.

### 3. FXML Standards

Reviews the mapping between FXML files and Controllers, resource loading methods, and markup usage. Default severity baseline: Major.

**Check Items**:
- **fx:id matching**: Whether each `fx:id` in FXML has a corresponding `@FXML` field in the Controller, and vice versa
- **Controller mapping**: Whether the `fx:controller` path correctly points to the Controller fully qualified class name
- **Script prohibition**: Whether `<fx:script>` is used in FXML (should be prohibited; logic must be in the Controller)
- **Event handlers**: Whether methods referenced by `onAction="#method"` exist in the Controller with the signature `void method(ActionEvent)` or no-arg
- **Resource paths**: Whether `FXMLLoader` loading uses `getClass().getResource("/fxml/xxx.fxml")`, rather than filesystem absolute paths
- **styleClass consistency**: Whether `styleClass` references in FXML are defined in the corresponding CSS
- **controllerFactory**: Whether `loader.setControllerFactory(springContext::getBean)` is set in Spring Boot scenarios
- **Root element namespace**: Whether `xmlns:fx="http://javafx.com/fxml"` is declared, whether the FXML version matches the JavaFX version

### 4. Memory Leak Risks

Reviews whether listeners, bindings, static references, and other constructs pose leak risks. Violations in this dimension default to Critical.

**Check Items**:
- **Listener removal**: Whether `ChangeListener` / `ListChangeListener` registered via `addListener()` are removed via `removeListener()` when the view is destroyed
- **Binding disposal**: Whether Binding objects returned by `Bindings.createXxxBinding()` call `dispose()` when no longer needed
- **Weak reference usage**: Whether listeners on long-lived objects consider using `WeakChangeListener` / `WeakListChangeListener`
- **Static reference detection**: Whether static fields hold references to UI components (`Stage`, `Node`), preventing GC
- **Anonymous inner classes**: Whether event handler anonymous inner classes implicitly hold outer Controller references, causing leaks
- **Stage close cleanup**: Whether `setOnCloseRequest` or view-switching callbacks perform resource cleanup (stopping `Timeline` / `Animation`, closing streams, releasing bindings)
- **Bidirectional binding unbinding**: Whether bindings established by `bindBidirectional()` call `unbindBidirectional()` when the view is destroyed

> **Typical violation**: A Controller registers `model.addListener(...)` but provides no cleanup method; after view switching, the old Controller cannot be GC'd and continues receiving events.

### 5. Performance

Reviews whether the code has performance bottlenecks and whether it follows JavaFX performance optimization best practices. Default severity baseline: Major.

**Check Items**:
- **TableView virtualization**: Whether large datasets rely on `TableView` virtualization, whether `ListView` + manual rendering is misused causing performance degradation
- **Batch updates**: Whether batch modifications to `ObservableList` use `setAll()` for one-time replacement (triggering 1 change event), rather than looping `add()` item by item (triggering N change events)
- **Throttle/debounce**: Whether high-frequency input (search boxes, sliders) uses debounce timers to avoid triggering full refreshes on every input
- **CSS selector efficiency**: Whether CSS avoids deeply nested selectors, whether style class switching in loops is avoided
- **Lazy loading**: Whether heavy views / tabs use lazy loading, rather than full initialization at startup
- **Layout computation**: Whether `layout()` / `requestLayout()` are called in loops, whether unnecessary `autosize()` is avoided
- **Image loading**: Whether large images are loaded and scaled on background threads, whether decoding large images on the FX thread is avoided
- **FilteredList efficiency**: Whether the `FilteredList` predicate is overly complex, whether index optimization is considered for large datasets
- **Binding efficiency**: Whether creating `Bindings.createXxxBinding()` in loops is avoided, whether computed bindings can be replaced with more efficient `SelectBinding` / `ObjectBinding`

### 6. Deep Compliance Audit

Reviews whether the code conforms to JavaFX coding standards, security rules, and framework integration best practices. Default severity baseline: Minor. Managed by 3 primary documents:

**Check Items**:
- **Naming conventions** `[compliance-rules.md]`: PascalCase for classes, camelCase for methods / variables, SCREAMING_SNAKE_CASE for constants
- **Coding standards** `[compliance-rules.md]`: UTF-8 encoding, 4-space indentation, explicit imports (no wildcard `import ...*`), Javadoc on public API
- **Security rules** `[security-checklist.md]`: Whether SQL uses prepared statements to prevent injection, whether file paths use `normalize()` to prevent traversal, whether there are no hardcoded secrets, whether WebView restricts JavaScript
- **Spring Boot pitfalls** `[compliance-rules.md]`: Whether the main class does not directly extend `Application`, whether Controllers are annotated with `@Scope("prototype")`, whether `web-application-type: none` is configured
- **Version compatibility** `[compliance-rules.md]`: Whether JavaFX 24+ configures `--enable-native-access=javafx.graphics`, whether version selection follows the LTS roadmap
- **CSS compliance** `[css-compliance.md]`: Whether `var()` is not used (JavaFX CSS does not support it), whether border radius uses literal numeric values rather than looked-up color references to size variables
- **API misuse detection** `[compliance-rules.md]`: Whether nonexistent APIs are used (e.g., `select()`, `@FXML dispose()`), whether the deprecated ControlsFX `Dialogs.create()` is used
- **Properties null safety** `[binding-compliance.md]`: Whether `SimpleLongProperty.set(null)` and similar handle null to prevent NPE

### 7. Database Access Security

Reviews the database access layer (Entity, Repository/Mapper, Service transaction, connection pool) for SQL injection, resource leaks, unsafe transaction boundaries on the UI thread, sensitive data exposure, and entity serialization safety. This dimension applies when the project contains a persistence layer (JPA/Hibernate, MyBatis, JDBC, Spring Data JPA). Default severity baseline: Major (security/resource issues). Skipped entirely when the project has no database access code.

**Check Items**:
- **SQL injection prevention** `[security-checklist.md]`: Whether all SQL uses parameterized queries — `PreparedStatement` with `?` placeholders, MyBatis `#{param}` (not `${param}` for user-controlled values), JPA/JPQL named or positional parameters — without concatenating user input; whether dynamic sort fields / table names are whitelisted (cross-references the Deep Compliance Audit security rule, applied here specifically to the DAO/Repository layer)
- **Connection pool leak** `[database-integration.md]`: Whether every manually-obtained `EntityManager` / `Connection` is closed in a `finally` block or try-with-resources; whether `Application.stop()` closes the `DataSource` / `EntityManagerFactory` / Spring context (prevents process hang and pool exhaustion); whether HikariCP `leak-detection-threshold` is configured in dev
- **Transaction boundary in UI thread** `[database-integration.md]`: Whether `@Transactional` Service methods or direct DB calls are executed on the JavaFX Application Thread (freezes the UI and widens the transaction scope across user interactions); whether DB I/O is wrapped in `Task`/`Service` on a background thread with results returned via `Platform.runLater()`; whether a Hibernate-managed entity is detached before crossing into Controller state (avoids `LazyInitializationException`)
- **Sensitive data exposure in logs** `[security-checklist.md]`: Whether `hibernate.show_sql` / `format_sql` is disabled in production (logs full SQL including parameters); whether credentials, tokens, or PII columns are logged at DEBUG/TRACE; whether `application.yml` uses environment-variable placeholders (`${DB_PASSWORD}`) instead of plaintext secrets
- **Entity serialization safety** `[database-integration.md]`: Whether JPA entities with JavaFX `Property` fields are safely (de)serialized — `Property` objects are not reliably serializable and should be marked `transient`/excluded when entities cross a serialization boundary (RMI, cache, JSON via DTO); whether lazy-loading proxies are detached before serialization (avoids `LazyInitializationException` during JSON marshalling); whether bidirectional associations have a controlled `toString()` to prevent infinite recursion / stack overflow

> **Typical violation**: A Controller's `@FXML` handler calls `userRepository.findAll()` synchronously on the JavaFX Application Thread, freezing the UI; and an `EntityManager` opened in the handler is never closed when an exception is thrown, leaking a pooled connection until the pool is exhausted.

### 8. Requirements Coverage

Reviews whether every requirement in the `requirements.md` specification has corresponding code implementation and test coverage, and whether every code file traces back to a legitimate requirement. This dimension provides bidirectional traceability: forward (requirement → code → test) and backward (code → requirement). Default severity baseline: Major. Skipped entirely when the project has no `requirements.md` file.

**Check Items**:
- **Requirement implementation coverage** `[requirements-coverage.md]`: Whether every functional requirement (FR-xxx) in `requirements.md` Section 3.1 has at least one Java source file annotated with `@req FR-xxx`
- **Requirement test coverage** `[requirements-coverage.md]`: Whether every functional requirement has at least one test method annotated with `@req FR-xxx` or named with `_{REQ_ID}` suffix; test coverage >= 80%
- **Orphan code detection** `[requirements-coverage.md]`: Whether every Java source file in `src/main/java/` has a valid `@req` annotation; files without `@req` are flagged as orphan code
- **RTM consistency** `[requirements-coverage.md]`: Whether the Requirement Traceability Matrix in `requirements.md` Section 7 is consistent with actual code annotations — file paths exist, coverage numbers match, no phantom entries
- **Test method naming convention** `[requirements-coverage.md]`: Whether test methods follow `test{Behavior}_{REQ_ID}()` naming convention with matching `@req` Javadoc
- **Non-functional requirement verification** `[requirements-coverage.md]`: Whether NFR-PERF/NFR-SEC/NFR-COMPAT requirements have corresponding implementation evidence in code

> **Typical violation**: `requirements.md` lists FR-003 "Export to CSV" but no source file has `@req FR-003` — the requirement was never implemented. Or: `CsvExporter.java` has no `@req` annotation — orphan code with no traceable requirement.

### 9. Refactoring Verification

Reviews whether refactoring changes (applied by `javafx-developer` from `javafx-refactorer`'s `refactor-handoff.json`) preserve behavior semantics. This dimension is **only activated when a refactoring has been applied** — it is skipped in normal review cycles. The reviewer checks that the refactored code is behaviorally equivalent to the pre-refactor code. Default severity baseline: Critical (behavior changes are highest severity).

**Check Items**:
- **Method signature preservation**: Whether public API signatures (public/protected methods) are unchanged after refactoring, unless the refactoring intentionally changes the API (flagged in `behavior_equivalence_check.method_signatures_preserved: false`)
- **Call site integrity**: Whether all call sites of moved/renamed methods are correctly updated — no dangling references to old method names or old class locations
- **Field access integrity**: Whether moved fields have correct access modifiers at their new location — no widened or narrowed visibility unless intentional
- **Import graph acyclicity**: Whether the refactoring introduced new circular dependencies that did not exist before — the import graph must remain acyclic
- **Test result preservation**: Whether all tests that passed before refactoring still pass after refactoring — no new test failures are allowed (pre-existing failures may remain)
- **Semantic equivalence**: Whether the refactored code produces the same output for the same input — verified through test suite execution and manual inspection of before/after snippets

> **Typical violation**: Refactoring moved `validateUser()` from `UserController` to `UserValidator`, but a call site in `MainWindowController` still references `UserController.validateUser()` — dangling reference causing compilation failure. Or: refactoring extracted a method but changed the order of side effects, causing a test that checks logging output to fail.

> **Activation condition**: This dimension is activated when `.loop-state.json` has `refactor_result.triggered: true` AND the developer has applied refactoring changes (indicated by `status: "reviewing_and_verifying"` after a refactoring phase). When not activated, this dimension is skipped entirely.

### 10. Static Analysis Tool Findings

Consumes deterministic static analysis findings from `javafx-runner`'s Static Analysis Verification dimension. This dimension merges tool-detected issues (SpotBugs, PMD, Checkstyle) with the reviewer's own LLM-based review, providing a deterministic baseline that complements semantic analysis. Default severity baseline: Minor (Major for SpotBugs High-priority findings).

**Check Items**:
- **Findings file consumption**: Whether `target/static-analysis-findings.json` exists (produced by runner's Step 2.5). If the file does not exist, this dimension is skipped with a note: "Static Analysis Tool Findings skipped — no static-analysis-findings.json. Consider running javafx-runner with static analysis enabled."
- **Deduplication with LLM findings**: For each tool finding, check whether the reviewer's own LLM review (Dimensions 1-9) already identified the same issue at the same location. Tool findings already found by LLM are annotated as "confirmed by {tool}" for higher confidence. Tool findings NOT found by LLM are added as supplementary issues with `source: "spotbugs" | "pmd" | "checkstyle"`
- **SpotBugs findings triage**: Review each SpotBugs finding for validity — some may be false positives in JavaFX context (e.g., `UWF_NULL_FIELD` on lazy-initialized Properties). Invalid findings are marked `status: "false_positive"` with justification. Valid findings are integrated into the issue list
- **PMD findings triage**: Review each PMD finding for validity — some may be false positives (e.g., `UnusedPrivateField` on `@FXML`-injected fields if the PMD XPath suppression was not applied). Invalid findings are marked `status: "false_positive"`. Valid findings are integrated
- **Checkstyle findings triage**: Review each Checkstyle finding — style violations are typically valid, but may be intentionally suppressed via `CHECKSTYLE:OFF` comments. Suppressed findings are marked `status: "suppressed"`
- **Cross-validation with LLM findings**: Use tool findings to validate the reviewer's own conclusions. If SpotBugs reports `NP_NULL_ON_SOME_PATH` at a location where the reviewer's LLM review flagged an NPE risk (Dimension 2 or 6), the LLM finding is annotated "confirmed by SpotBugs" for higher confidence. Conversely, if the LLM flagged an issue that no tool detected, the LLM finding stands (LLM can find semantic issues tools miss)

> **Typical finding**: SpotBugs reports `NP_NULL_ON_SOME_PATH` on `UserService.findById()` at line 45 — the reviewer's Dimension 2 (UI Thread Safety) or Dimension 6 (Deep Compliance) may have also flagged this as a null-pointer risk. The reviewer annotates the LLM finding as "confirmed by SpotBugs (NP_NULL_ON_SOME_PATH)" and includes the tool finding as supporting evidence.

> **False positive example**: SpotBugs reports `UWF_NULL_FIELD` on a `DoubleProperty rating` field in a custom control — this is a false positive because JavaFX Properties use lazy initialization (null until first access). The reviewer marks this as `status: "false_positive"` with justification: "JavaFX Property lazy initialization pattern — field is intentionally null until ratingProperty() is first called."

> **Relationship to runner**: This dimension depends on `javafx-runner`'s Static Analysis Verification (Step 2.5) having executed and produced `target/static-analysis-findings.json`. If runner did not execute static analysis (e.g., compile failed, or `.loop-config.json` has `static_analysis: false`), this dimension is skipped.

## Review Workflow

### Step 1: Code Collection and Context Analysis

1. **Identify input scope**: Determine the file types involved in the code under review (Java / FXML / CSS / module-info / pom.xml)
2. **Declare review scope**: Determine the review mode based on the user request and annotate it in the report header
3. **Extract context**: The JavaFX version, JDK version, build tool, and whether Spring Boot / third-party libraries are integrated
4. **Establish relationships**: Identify Controller ↔ FXML ↔ Model correspondences and build the review context graph

**Review scope declaration**: Three review modes are supported, determined by the user request or inferred from context:
- **Full Review (default)**: Performs a complete ten-dimension scan of all JavaFX-related files in the project. Suitable for pre-release health checks and first-time reviews. Note: Dimension 9 (Refactoring Verification) is only activated when a refactoring has been applied; Dimension 10 (Static Analysis Tool Findings) is only activated when `target/static-analysis-findings.json` exists
- **Incremental Review**: Only reviews user-specified new / modified files and their directly associated files (e.g., if a Controller is modified, its FXML is also reviewed). Suitable for continuous review during iterative development
- **Targeted Dimension Review**: The user explicitly cares only about certain dimensions (e.g., "only check thread safety"), loading only the corresponding primary reference document for scanning

### Step 2: Dimension Scanning (Item-by-Item Check)

1. Scan through the ten dimensions sequentially, evaluating each check item as pass / fail
2. For failed items, record: problem description, code location (filename + line number / code snippet), violated rule item
3. Load the dimension reference documents from `references/` as criteria (load primary documents per the mapping table, load supplementary documents as needed)
4. **Incremental mode optimization**: During incremental review, skip dimensions unrelated to the changed files; if only CSS is modified, skip thread safety and memory leak dimensions, executing only FXML standards + deep compliance (CSS portion)

### Step 3: Deep Analysis (Cross-Referencing)

1. **Cross-file correlation**: Cross-validate FXML `fx:id` with Controller fields, cross-validate CSS `styleClass` with FXML references
2. **Pattern recognition**: Identify recurring problem patterns (e.g., multiple Controllers all failing to remove listeners) and merge them into systemic issues
3. **Impact assessment**: Evaluate the actual runtime impact of each issue (crash / performance degradation / memory growth / style-only issue)

### Step 4: Severity Classification and Sorting

1. Assign a severity level to each issue per the severity classification system
2. Deduplicate: merge multiple manifestations caused by the same root cause into one issue
3. Sort: arrange in descending severity order, within the same level sort by code location

### Step 5: Generate Review Report

1. Generate a structured review report following the report template (see `report-templates/review-report.md`)
2. The report includes: summary statistics, issue list (with location / recommendation / rule reference), compliance summary
3. Provide actionable optimization recommendations for each issue, including corrected example code
4. **Extract AST node signatures**: For each issue in a `.java` file, extract the `ast_node_signature` by identifying the enclosing AST node:
   - If the issue is inside a method body → extract `{package}.{Class}#{methodName}({paramTypes})`
   - If the issue is a field declaration → extract `{package}.{Class}#{fieldName}`
   - If the issue is at class level → extract `{package}.{Class}`
   - If the file is not a Java source file (FXML, CSS, `module-info.java`) → set to `null`
   - See `javafx-orchestrator/SKILL.md` → Fix Handoff Format → AST Anchor Format for the full extraction specification

## Severity Classification

All discovered issues are classified into the following four-level severity system, which determines fix priority:

| Level | Identifier | Definition | Typical Issues | Handling Recommendation |
|-------|------------|------------|----------------|------------------------|
| Critical | Critical | Causes crashes, data loss, or severe memory leaks; must be fixed immediately | Background thread updating UI, listener not removed causing leak, NPE risk | Block release, fix first |
| Major | Major | Violates core standards, affects maintainability or has performance bottlenecks | Disorganized architecture layering, FXML-Controller mismatch, inefficient CSS | Fix within this iteration |
| Minor | Minor | Violates coding standards or style conventions, does not affect runtime | Non-standard naming, missing Javadoc, wildcard imports | Recommend fixing |
| Info | Info | Optimization suggestions that improve code quality but are not violations | Extractable common methods, better API alternatives | Optimize when convenient |

### Escalation/De-escalation Conditions

Each check item has a default severity baseline, but may move up or down by one level based on actual impact. The following are escalation/de-escalation conditions for key check items across dimensions; they must be strictly followed to ensure classification consistency:

| Check Item | Default Baseline | De-escalation Condition | Escalation Condition |
|------------|------------------|------------------------|----------------------|
| FX thread update violation | Critical | - (cannot be de-escalated; runtime will always throw an exception) | - |
| Listener not removed | Critical | Listener object lifecycle same as Controller (co-terminus) → Major | Has caused OOM or reproducible memory growth → remain Critical |
| Binding not disposed | Critical | Short-lived view (e.g., dialog) → Major | Long-lived view (main window) with many bindings → remain Critical |
| Static reference holding UI component | Critical | - (cannot be de-escalated) | - |
| FX thread blocking call | Critical | Blocking time very short (<16ms, e.g., small local file read) → Major | Blocking time >1s or involves network I/O → remain Critical |
| Disorganized architecture layering | Major | Only individual methods cross layers, does not affect overall architecture → Minor | Prevents independent testing or multiple circular dependencies → Critical |
| FXML fx:id mismatch | Major | - (runtime will always throw LoadException, cannot be de-escalated) | Multiple fx:id mismatches → remain Major |
| Inefficient batch update (loop add) | Major | Data volume <100 items → Minor | Data volume >10000 items and executing on FX thread → Critical |
| CSS using var() | Major | - (unsupported syntax, cannot be de-escalated) | - |
| Non-standard naming | Minor | - | Public API naming violates standards and affects callers → Major |
| Wildcard imports | Minor | - | - |
| Spring Boot startup class directly extending Application | Critical | - (causes Spring container initialization exception, cannot be de-escalated) | - |
| Controller missing @Scope("prototype") | Major | Singleton Controller with no state fields → Minor | Singleton Controller holding @FXML state fields → remain Major |

**Classification constraints**:
- Each issue may move at most one level; cross-level jumps are prohibited (e.g., Critical dropping directly to Minor)
- Check items marked "cannot be de-escalated" must retain their default baseline even if the impact is minor
- When escalating or de-escalating, the triggering condition must be noted in the report's "Escalation/De-escalation Note" field to ensure traceability

## Review Report Format

After the review is complete, output a structured report containing three parts: summary statistics, issue list, and compliance summary.

### Report Structure

```
# JavaFX Code Review Report

## Review Summary
- Review Mode: [Full / Incremental / Targeted Dimension]
- Review Scope: [List of files or dimensions involved]
- Files Reviewed: N Java / M FXML / K CSS
- Total Issues Found: X (Critical: a / Major: b / Minor: c / Info: d)
- Review Conclusion: [Pass / Conditional Pass / Fail]

## Issue List

### [Critical] Issue Title
- **Problem Description**: Specific description of the issue
- **Code Location**: `file path:line number`
- **Problematic Code**:
  ```java
  // problematic code snippet
  ```
- **Optimization Recommendation**: How to fix it
- **Corrected Example**:
  ```java
  // corrected code
  ```
- **Rule Reference**: Reference to the rule item in references/ (format: `document name - item title`)
- **Escalation/De-escalation Note**: If severity deviates from the default baseline, note the triggering condition
- **Fix Handoff**: Machine-readable fix location anchors for javafx-developer or the user to directly execute fixes
  - `target_file: file path`
  - `target_lines: start line-end line`
  - `fix_type: [replace / insert / delete]`
  - `fix_priority: 1` (fix priority, 1=highest)
  - `code_fingerprint: sha256 hash` (hash of the problematic code snippet, normalized: whitespace-trimmed, for drift-resistant matching)
  - `anchor_pattern: context signature` (2 lines before + 2 lines after the target, for secondary location when fingerprint match is ambiguous)
  - `ast_node_signature: com.example.Class#method(params)` (AST-level anchor — fully qualified method/field/class signature, for refactor-resistant matching when code has been moved; `null` for non-Java files)

### [Major] ... (same structure)

## Compliance Summary
| Dimension | Check Items | Passed | Failed | Pass Rate |
|-----------|-------------|--------|--------|-----------|
| Code Structure | 5 | 4 | 1 | 80% |
| Thread Safety | 6 | 5 | 1 | 83% |
| ... | ... | ... | ... | ... |
| **Total** | **N** | **N** | **N** | **N%** |
```

### Report Language Strategy

- **Follow skill version**: The Chinese skill outputs Chinese reports; the English skill outputs English reports
- **Code and identifiers remain as-is**: Regardless of report language, code snippets, file paths, class names, and API names remain in English without translation
- **Rule reference citations**: Uniformly cite `references/` document items, formatted as `document name - item title`

### Fix Handoff Field Description

The "Fix Handoff" field is key to achieving the "generate → review → fix" closed loop, enabling review results to be directly consumed by `javafx-developer` or automation tools:
- `fix_type=replace`: Replace the code segment specified by `target_lines` with the "Corrected Example"
- `fix_type=insert`: Insert the "Corrected Example" after `target_lines`
- `fix_type=delete`: Delete the code segment specified by `target_lines` (no corrected example)
- `fix_priority`: Fix priority sorted by severity + code location, 1 is highest, for ordering during batch fixes
- `code_fingerprint`: SHA-256 hash of the problematic code snippet (normalized: whitespace-trimmed, leading/trailing spaces removed). Used for drift-resistant matching — if line numbers have shifted due to prior fixes, the fingerprint still identifies the correct code location
- `anchor_pattern`: Signature of surrounding context (2 lines before + 2 lines after the target lines, concatenated and normalized). Used as a secondary locator when the fingerprint match is ambiguous or multiple matches exist
- `ast_node_signature`: AST-level anchor in the format `{package}.{Class}#{methodName}({paramTypes})` for method-level issues, `{package}.{Class}#{fieldName}` for field-level issues, or `{package}.{Class}` for class-level issues. Extracted from the enclosing AST node of the problematic code. Provides refactor-resistant matching — when methods are moved to different files or classes are renamed, the developer's Fix Consumption Protocol can locate the code by signature search instead of line numbers. Set to `null` for non-Java files (FXML, CSS, `module-info.java`). See `javafx-orchestrator/SKILL.md` → Fix Handoff Format → AST Anchor Format for the full specification

### Dual Output Format (Markdown + JSON)

The reviewer outputs reports in **two formats simultaneously** by default:

1. **Markdown report** (`review-report.md`) — human-readable, for developer review and documentation
2. **JSON report** (`review-report.json`) — machine-readable, for `javafx-developer` Fix Consumption, CI/CD quality gates, and IDE plugin integration

The JSON format is defined by the schema in `report-templates/report-schema.json`. It contains the same information as the Markdown report but in a structured format with a standalone `fix_handoffs` array for direct programmatic consumption. Key fields:

- `summary.conclusion`: `Pass`, `Conditional Pass`, or `Fail` — CI/CD can use `jq .summary.conclusion review-report.json` for quality gate decisions
- `fix_handoffs[]`: Standalone array sorted by `fix_priority`, each entry includes `target_file`, `target_lines`, `fix_type`, `fix_priority`, `code_fingerprint`, `anchor_pattern`, `ast_node_signature`, `corrected_example`, `issue_id`, and `severity`
- `loop_state`: Current loop state snapshot for orchestrator synchronization

**Output format control**: If `.loop-config.json` exists in the project root with `"output_format": "json"`, output only the JSON report; if `"output_format": "markdown"`, output only the Markdown report. Default (no config file or `"output_format": "both"`) outputs both formats.

## Constraints

The following constraints are shared with `javafx-developer`, ensuring that review standards are consistent with generation standards. Each constraint is annotated with its corresponding `references/` document.

### Coding Standards (→ `compliance-rules.md`)
1. **Naming**: PascalCase for classes, camelCase for methods/variables, SCREAMING_SNAKE_CASE for constants
2. **Indentation**: 4 spaces, no tabs
3. **Encoding**: UTF-8 for all source files
4. **Imports**: Explicit imports, no wildcards (`import javafx.scene.control.*`)
5. **Comments**: Javadoc on public API, inline comments for complex logic

### Architecture Rules (→ `structure-review.md`, `fxml-standards.md`, `thread-safety-rules.md`)
1. **FXML purity**: No `<fx:script>` in FXML files
2. **Controller responsibility**: Only handle UI events and view state, delegate business logic to Service
3. **Binding first**: Prefer JavaFX Properties binding over manual UI sync
4. **Resource paths**: Use `getClass().getResource()` for FXML/CSS loading
5. **Thread safety**: All UI updates on JavaFX Application Thread, use `Task`/`Service` for background work

### Security Rules (→ `security-checklist.md`)
1. **Input validation**: Validate all user input, no SQL/command concatenation
2. **Path safety**: Use `Paths.get()` + `Path.normalize()` for file operations
3. **No hardcoded secrets**: Use config files or environment variables
4. **WebView security**: Disable JavaScript or restrict to trusted content

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
> The sections below describe only the **reviewer's role and responsibilities** within the loop — the minimal subset needed for standalone operation.

### Reviewer's Role in the Loop

`javafx-code-reviewer` occupies the **review** stage of the loop:
- **Round 1**: Full review — all ten dimensions, all JavaFX files
- **Round 2+**: Incremental review — only dimensions touched by `javafx-developer`'s fixes (identified by `target_file` in the fix handoff)

### Individual Gate Criteria (Reviewer)

- **Pass**: No Critical or Major issues, pass rate >= 80%
- **Conditional Pass**: Has Major but no Critical, all Major issues have clear fix plans
- **Fail**: Has Critical issues, must be fixed before release

### Reviewer's Serialization Responsibilities

1. **Read state**: Before starting review, check for `.loop-state.json`. If found, extract `current_round` and `last_fix_handoff` to determine review scope
2. **Determine strategy**: Round 1 → Full Review; Round 2+ → Incremental Review (load only dimensions related to `target_file`s in the fix handoff)
3. **Write result**: After completing review, update **only** the `rounds[current_round].reviewer_result` field with conclusion, issue counts by severity, and fix handoff count. Do not modify `runner_result` or other fields (parallel write safety — runner writes to its own field concurrently)
4. **Set next action**: If both reviewer and runner have completed, set `next_action: "fixing"` (developer consumes merged fix handoffs); if only reviewer has completed, leave `next_action` unchanged (orchestrator will update after runner also completes)

> **Fix Handoff Format**: See `javafx-orchestrator/SKILL.md` → Fix Handoff Format for the authoritative field definitions. The reviewer generates Fix Handoffs with `target_file`, `target_lines`, `fix_type`, `fix_priority`, `code_fingerprint`, `anchor_pattern`, and `ast_node_signature` fields.

## Runtime Findings Reception

`javafx-code-reviewer` can receive runtime findings feedback from `javafx-runner`'s verification reports. This enables the skill set to self-evolve: runtime-discovered patterns flow back to static rules, so future reviews catch them earlier.

### How Feedback Arrives

When `javafx-runner` discovers a runtime issue pattern not covered by existing reviewer rules, it outputs a **Runtime Findings Feedback** section in its verification report, containing:
- `suggested_reviewer_rule.target_document`: Which `references/` document should receive the new rule
- `suggested_reviewer_rule.suggested_check_item`: Proposed check item title
- `suggested_reviewer_rule.description`: What the new check item should verify
- `suggested_reviewer_rule.suggested_severity`: Proposed severity baseline
- `evidence`: Occurrences, stack traces, affected files

### Adoption Process

1. **Review suggestion**: Evaluate whether the suggested rule is valid and generalizable (not project-specific)
2. **Draft check item**: If adopted, draft a new check item following the standard format (Focus / Pass Criteria / Fail Criteria / Severity Baseline / Bad Example / Good Example)
3. **Cross-reference**: Add `Runtime Verification Required` annotation linking back to the runner check item that discovered the pattern
4. **Update document**: Add the new check item to the target `references/` document
5. **Update count**: Update the dimension's check item count in the review report template's Compliance Summary table

### Adoption Criteria

- **Adopt**: Pattern is generalizable across projects, has clear pass/fail criteria, and occurs >= 2 times
- **Reject**: Pattern is project-specific, cannot be generalized, or is already covered by an existing check item (possibly under a different name)
- **Defer**: Pattern needs more evidence (only 1 occurrence); revisit after more runtime data

## Reference Documents

For in-depth criteria, refer to the following documents in the `references/` directory:

- `references/structure-review.md` - Code structure review standards ← developer: `architecture-patterns.md`
- `references/thread-safety-rules.md` - UI thread safety rules ← developer: architecture rules · thread safety
- `references/fxml-standards.md` - FXML standards ← developer: quality checklist · fx:id
- `references/memory-management.md` - Memory management rules ← developer: `data-binding-patterns.md`
- `references/performance-guide.md` - Performance optimization guide
- `references/binding-compliance.md` - Data binding compliance (cross-dimension: memory/performance/compliance)
- `references/compliance-rules.md` - Coding/naming/Spring Boot/version/API compliance
- `references/security-checklist.md` - Security compliance checklist ← developer: security rules
- `references/css-compliance.md` - CSS compliance rules ← developer: `css-best-practices.md`
- `references/requirements-coverage.md` - Requirements coverage rules (requirement traceability, @req annotation, orphan code detection) ← developer: `templates/docs/requirements.md` (RTM template)

> **Cross-reference (developer skill)**: Dimension 10 (Static Analysis Tool Findings) cross-references the developer skill's `references/static-analysis-tools.md` — that document defines the SpotBugs/PMD/Checkstyle plugin configuration, JavaFX-tailored rule sets, report parsing formats, unified issue mapping structure, and false positive exclusions. The document lives in the `javafx-developer/references/` directory, not this skill's `references/` directory.

## Report Templates

Reusable skeleton templates in the `report-templates/` directory:

- `report-templates/review-report.md` - Review report skeleton template (Markdown, human-readable)
- `report-templates/report-schema.json` - JSON schema for machine-readable report output (CI/CD, IDE, Fix Consumption)
