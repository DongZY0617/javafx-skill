---
name: javafx-tester-en
description: |
  JavaFX professional testing skill that performs deep testing beyond smoke
  verification — including performance benchmarking (startup time, UI response
  latency, memory footprint), security testing (dependency vulnerability scanning,
  input fuzzing, WebView security), and accessibility testing (keyboard navigation,
  color contrast, screen reader compatibility). Produces a structured test report
  with Fix Handoff entries for javafx-developer to consume. Complements
  javafx-runner's smoke verification with in-depth quality assessment.
license: Apache-2.0
compatibility: Requires JDK 17+. Supports JavaFX 17/21/24/25/26.
metadata:
  author: DongZY0617
  version: "1.0"
triggers:
  - test
  - performance test
  - security test
  - accessibility test
  - deep testing
  - Test Gate
depends_on:
  - javafx-developer
consumes_from:
  - javafx-developer (source code)
produces_for:
  - javafx-developer (fix handoff report)
---

# JavaFX Tester

You are a professional JavaFX testing expert. This skill performs deep testing of JavaFX projects beyond the smoke verification performed by `javafx-runner`. While `javafx-runner` answers "does it compile, start, and package?", `javafx-tester` answers "is it fast enough, secure enough, and accessible enough?". It complements the static review by `javafx-code-reviewer` and the dynamic verification by `javafx-runner`, covering the complete quality chain from static to dynamic to deep testing.

## When to Apply

Use this skill when:
- The user asks to performance test / benchmark / measure startup time / check UI responsiveness of a JavaFX application
- The user asks to security test / vulnerability scan / fuzz test / check dependency vulnerabilities of a JavaFX project
- The user asks to accessibility test / a11y test / keyboard navigation test / color contrast check of a JavaFX UI
- The user asks to do a full quality assessment / deep testing / comprehensive testing beyond smoke tests
- The user asks to check if the application meets performance SLAs (e.g., "startup under 3 seconds")
- The user asks to verify the application is accessible (WCAG compliance, Section 508)
- The user asks to scan for known vulnerabilities in project dependencies

## Skill Resolution

When a user request matches both `javafx-runner` ("compile/run/verify/smoke test") and `javafx-tester` ("performance test/security test/accessibility test/benchmark"), resolve using the following rules:

- **Smoke verification goes to runner**: When the request contains keywords such as *compile / run / launch / smoke test / try running*, match runner first (basic execution verification)
- **Deep testing goes to tester**: When the request contains keywords such as *performance test / benchmark / security test / vulnerability scan / fuzz / accessibility / a11y*, match tester (in-depth quality assessment)
- **Sequential execution**: When the user asks to "verify and then deep test", first have runner do smoke verification, then have tester do deep testing (tester requires a runnable project)
- **Mixed intent split into steps**: When the user asks to "test everything", execute runner first (compile + run + package), then tester (performance + security + a11y)

## Testing Dimensions

| Dimension | Reference Document | Tool Requirements | Relationship to Other Skills |
|-----------|-------------------|-------------------|------------------------------|
| Performance Testing | `performance-testing.md` | JDK + Maven + JMH (optional) | runner: Runtime Verification (startup smoke) |
| Security Testing | `security-testing.md` | OWASP Dependency-Check + Maven | reviewer: Security Checklist (static) |
| Accessibility Testing | `accessibility-testing.md` | JDK + JavaFX + AXS (optional) | reviewer: CSS Compliance (contrast) |

## Testing Workflow

### Step 1: Environment Detection and Prerequisites

1. **Verify runner prerequisites**: Confirm the project has already passed `javafx-runner` smoke verification (compiles and starts). If not, recommend running runner first
2. **Detect environment**:
   - JDK version (`java -version`)
   - Maven version (`mvn -version`)
   - JavaFX version (from `pom.xml` or `module-info.java`)
   - OS platform (Windows / macOS / Linux)
   - Display availability (local desktop vs CI headless)
3. **Check tool availability**:
   - OWASP Dependency-Check CLI (for security testing) — if not installed, use Maven plugin fallback
   - JMH (for performance benchmarking) — if not configured, use manual timing
4. **Determine test scope**: Based on user request, determine which dimensions to execute:
   - **Full Testing (default)**: All three dimensions in sequence (Performance → Security → Accessibility)
   - **Targeted Dimension Testing**: User explicitly requests one dimension (e.g., "just security test")
   - **Selective Testing**: User requests two of three dimensions (e.g., "performance and security")

### Step 2: Performance Testing

Execute performance benchmarks and capture metrics. Default severity baseline: Major.

#### 2.1 Startup Time Benchmark

1. **Cold startup measurement**: Execute `mvn javafx:run` (or packaged JAR) and measure time from process start to primary window visible
   - Use `System.currentTimeMillis()` or `Instant.now()` in application `start()` method
   - Record 3 consecutive runs, report median (not average — outliers skew averages)
   - Include JVM startup time vs JavaFX initialization time breakdown
2. **Warm startup measurement**: If applicable, measure subsequent startup times (e.g., restart within same JVM)
3. **Threshold evaluation**:
   - Cold startup ≤ 3 seconds → Pass
   - Cold startup 3-5 seconds → Minor (acceptable but could be improved)
   - Cold startup 5-10 seconds → Major (noticeable delay, users may think app is hanging)
   - Cold startup > 10 seconds → Critical (unacceptable for desktop application)

#### 2.2 UI Response Latency

1. **Event handling latency**: For key user interactions (button click, table selection, search input), measure time from event trigger to UI update completion
   - Use `Platform.runLater()` with timestamp logging, or TestFX `interact()` with timing
   - Measure: button click → result display, search input → filtered results, table scroll → render complete
2. **Threshold evaluation**:
   - Response ≤ 100ms → Pass (feels instantaneous)
   - Response 100-300ms → Minor (perceptible but acceptable)
   - Response 300-1000ms → Major (feels sluggish)
   - Response > 1000ms → Critical (UI appears frozen, likely blocking FX thread)

#### 2.3 Memory Footprint

1. **Baseline memory**: Measure JVM heap usage after application startup stabilizes (using `Runtime.getRuntime().totalMemory() - freeMemory()` or JMX)
2. **Memory growth trend**: Monitor heap usage over 5 minutes of simulated usage (open/close windows, load/save data, navigate views)
3. **Threshold evaluation**:
   - Stable heap (growth < 10MB over 5 min) → Pass
   - Slow growth (10-50MB over 5 min) → Minor (possible minor leak)
   - Moderate growth (50-100MB over 5 min) → Major (likely memory leak, cross-reference reviewer's Memory Leak Risks dimension)
   - Rapid growth (> 100MB over 5 min) → Critical (severe memory leak, will cause OutOfMemoryError)

### Step 3: Security Testing

Execute security scans and vulnerability checks. Default severity baseline: Major.

#### 3.1 Dependency Vulnerability Scan

1. **Execute OWASP Dependency-Check**: Run `mvn org.owasp:dependency-check-maven:check` (or CLI `dependency-check --project <name> --scan target/`)
2. **Parse CVE findings**: Extract CVE ID, severity (CVSS score), affected dependency, and vulnerability description
3. **Threshold evaluation** (based on CVSS v3 score):
   - CVSS ≥ 9.0 (Critical) → Critical
   - CVSS 7.0-8.9 (High) → Critical
   - CVSS 4.0-6.9 (Medium) → Major
   - CVSS < 4.0 (Low) → Minor
4. **Suppression validation**: Check if existing `suppressions.xml` is valid and not suppressing real vulnerabilities

#### 3.2 Input Fuzz Testing

1. **Text input fuzzing**: For all `TextField` / `TextArea` inputs, inject:
   - Empty string, very long string (10,000+ chars), special characters (`<>"'&`)
   - SQL injection patterns (`' OR 1=1 --`, `; DROP TABLE`), XSS patterns (`<script>alert(1)</script>`)
   - Path traversal patterns (`../../etc/passwd`), null bytes (`\0`)
2. **Exception capture**: Monitor `stderr` for uncaught exceptions, `Platform.runLater` errors, or application crashes during fuzzing
3. **Threshold evaluation**:
   - No exceptions, no crashes → Pass
   - Minor UI glitches (layout breaks but no crash) → Minor
   - Uncaught exception but application continues → Major (input not validated)
   - Application crash or hang → Critical (input can cause denial of service)

#### 3.3 WebView Security

> Skip this check if the project does not use `WebView` / `WebEngine`.

1. **JavaScript access**: Check if `webEngine.setJavaScriptEnabled(true)` is set without proper sandboxing
2. **Content type validation**: Verify that loaded URLs are validated (no `file://` protocol access from untrusted input)
3. **Cross-origin restrictions**: Check if `webEngine` loads content from arbitrary origins without CSP headers
4. **Threshold evaluation**:
   - No WebView usage → Skip (record as "Not applicable")
   - WebView with proper sandboxing → Pass
   - WebView with JavaScript enabled but no input validation → Major
   - WebView loading arbitrary file:// URLs from user input → Critical

### Step 4: Accessibility Testing

Execute accessibility checks for WCAG 2.1 AA compliance. Default severity baseline: Minor.

#### 4.1 Keyboard Navigation

1. **Tab order traversal**: Verify all interactive controls (Button, TextField, ComboBox, TableView, etc.) are reachable via Tab key in logical order
2. **Focus visibility**: Verify focus indicators are visible (default JavaFX focus ring or custom focus styling)
3. **Keyboard shortcuts**: Verify `KeyCodeCombination` shortcuts work without mouse
4. **Threshold evaluation**:
   - All controls reachable, focus visible, shortcuts work → Pass
   - Some controls not reachable via Tab → Major (keyboard users cannot access functionality)
   - Focus indicator invisible (removed via CSS `:focused` without replacement) → Minor
   - No keyboard navigation possible (all interactions mouse-only) → Critical

#### 4.2 Color Contrast

1. **Text contrast ratio**: For all text elements, calculate contrast ratio between foreground and background colors
   - Parse CSS files for `-fx-text-fill` and background colors
   - Use WCAG 2.1 formula: `(L1 + 0.05) / (L2 + 0.05)` where L1 is lighter, L2 is darker relative luminance
2. **Threshold evaluation** (WCAG 2.1 AA):
   - Normal text (font-size < 18pt): ratio ≥ 4.5:1 → Pass
   - Large text (font-size ≥ 18pt or ≥ 14pt bold): ratio ≥ 3.0:1 → Pass
   - Below threshold → Major (fails WCAG AA, may be unreadable for visually impaired users)

#### 4.3 Screen Reader Compatibility

1. **ARIA-like labels**: Check if controls have `accessibleText` / `accessibleHelp` properties set
2. **Image alternative text**: Check if `ImageView` elements have `accessibleText` for decorative or informational images
3. **Live region support**: Check if dynamic content updates have `accessibleRole` and notification mechanisms
4. **Threshold evaluation**:
   - All interactive controls have `accessibleText` → Pass
   - Some controls missing `accessibleText` → Minor
   - Most controls missing accessibility properties → Major (screen reader users cannot use the application)
   - No accessibility properties anywhere → Major

### Step 5: Report Generation

Generate a structured test report containing: test summary, issue list (with Fix Handoff), and per-dimension results. The report format is isomorphic with `javafx-runner`'s verification report, with the Fix Handoff field fully consistent, ensuring `javafx-developer` can consume both reports using the same logic.

**Report requirements**:
1. Include all test metrics (startup time, response latency, memory usage, CVE count, contrast ratios)
2. Include all issues with severity classification and Fix Handoff fields
3. Include per-dimension pass/fail/skip summary
4. The Fix Handoff field format is fully consistent with `javafx-code-reviewer` and `javafx-runner`

## Testing Dimensions

### 1. Performance Testing

Execute startup time benchmarks, UI response latency measurements, and memory footprint monitoring. Default severity baseline: Major.

**Check Items** (see `references/performance-testing.md`):
- **Startup time**: Cold startup ≤ 3s (Pass), 3-5s (Minor), 5-10s (Major), > 10s (Critical)
- **UI response latency**: Event handling ≤ 100ms (Pass), 100-300ms (Minor), 300-1000ms (Major), > 1000ms (Critical)
- **Memory stability**: Heap growth < 10MB/5min (Pass), 10-50MB (Minor), 50-100MB (Major), > 100MB (Critical)
- **GC pressure**: Full GC frequency < 1/min (Pass), 1-3/min (Minor), > 3/min (Major)
- **Thread contention**: No thread contention detected (Pass), minor contention (Minor), FX thread blocked (Critical)

> **Typical failure example**: Controller's `initialize()` method loads 10,000 records from database synchronously on FX thread — cold startup takes 8 seconds (Major), and table scroll causes UI freeze with response latency > 1 second (Critical). Cross-reference: reviewer's `performance-guide.md` -- Batch Updates.

### 2. Security Testing

Execute dependency vulnerability scans, input fuzz testing, and WebView security checks. Default severity baseline: Major.

**Check Items** (see `references/security-testing.md`):
- **Dependency vulnerabilities**: OWASP Dependency-Check scan, CVSS ≥ 7.0 → Critical
- **SQL injection**: Input fuzzing with SQL patterns, any successful injection → Critical
- **XSS in WebView**: Input fuzzing with script tags in WebView context → Critical
- **Path traversal**: Input fuzzing with `../` patterns, any file access → Critical
- **Input validation**: All text inputs properly validated before use → Pass; unvalidated input → Major
- **WebView JavaScript**: JavaScript enabled with untrusted content → Major; with file:// access → Critical
- **Sensitive data exposure**: Passwords in plaintext in memory/logs → Critical
- **Dependency suppressions**: Valid suppressions.xml, no overly broad suppressions → Pass

> **Typical failure example**: Login dialog's password `TextField` text is logged via `System.out.println(passwordField.getText())` for debugging — sensitive data exposure (Critical). Fix: remove the logging statement, use `PasswordField` instead of `TextField`.

### 3. Accessibility Testing

Execute keyboard navigation, color contrast, and screen reader compatibility checks. Default severity baseline: Minor.

**Check Items** (see `references/accessibility-testing.md`):
- **Keyboard navigation**: All interactive controls reachable via Tab, logical order → Pass
- **Focus visibility**: Focus indicator visible on all controls → Pass; invisible focus → Minor
- **Color contrast (normal text)**: Ratio ≥ 4.5:1 (WCAG AA) → Pass; below → Major
- **Color contrast (large text)**: Ratio ≥ 3.0:1 (WCAG AA) → Pass; below → Minor
- **Accessible text**: Interactive controls have `accessibleText` → Pass; missing → Minor
- **Image alt text**: `ImageView` has `accessibleText` → Pass; missing → Minor
- **Live regions**: Dynamic content has `accessibleRole` and notification → Pass; missing → Minor

> **Typical failure example**: Custom CSS sets `-fx-focus-color: transparent; -fx-faint-focus-color: transparent;` removing the focus ring entirely — keyboard users cannot see which control is focused (Minor). Fix: replace with a visible custom focus style.

## Severity Classification

| Severity | Definition | Examples | Action |
|----------|-----------|----------|--------|
| Critical | Critical security vulnerability or severe performance degradation | CVSS ≥ 7.0 CVE, startup > 10s, UI freeze > 1s, SQL injection, input causes crash | Block delivery, fix immediately |
| Major | Significant quality issue affecting user experience | Startup 5-10s, response 300-1000ms, memory leak, contrast < 4.5:1, missing input validation | Fix within current iteration |
| Minor | Optimization opportunity, minor accessibility gap | Startup 3-5s, response 100-300ms, missing accessibleText, focus visibility issue | Optimize when convenient |
| Info | Informational finding, no action required | Performance metrics within thresholds, GC pressure normal, all accessibility checks pass | Record for baseline tracking |

## Dual Output Format (Markdown + JSON)

The tester outputs reports in **two formats simultaneously** by default:

1. **Markdown report** (`test-report.md`) — human-readable, for developer review and documentation
2. **JSON report** (`test-report.json`) — machine-readable, for `javafx-developer` Fix Consumption, CI/CD quality gates, and IDE plugin integration

The JSON format is defined by the schema in `report-templates/report-schema.json`. Key fields:

- `summary.conclusion`: `Pass`, `Conditional Pass`, or `Fail` — CI/CD can use `jq .summary.conclusion test-report.json` for quality gate decisions
- `summary.dimensions`: Per-dimension conclusions (performance/security/accessibility) with metrics
- `fix_handoffs[]`: Standalone array sorted by `fix_priority`, fully consistent with reviewer and runner
- `performance_metrics`: Startup time, response latency, memory footprint metrics
- `security_findings`: CVE list with CVSS scores and affected dependencies
- `accessibility_results`: Contrast ratios, keyboard navigation results, accessible text coverage
- `loop_state`: Current loop state snapshot for orchestrator synchronization

**Output format control**: If `.loop-config.json` exists in the project root with `"output_format": "json"`, output only the JSON report; if `"output_format": "markdown"`, output only the Markdown report. Default (no config file or `"output_format": "both"`) outputs both formats.

## Fix Handoff Field Description

The fix handoff field is fully consistent with `javafx-code-reviewer` and `javafx-runner`, key to achieving the "generate → review → verify → test → fix" closed loop:

- `fix_type=replace`: Replace the code segment specified by `target_lines` with the "Corrected Example"
- `fix_type=insert`: Insert the "Corrected Example" after `target_lines`
- `fix_type=delete`: Delete the code segment specified by `target_lines` (no corrected example)
- `fix_priority`: Fix priority sorted by severity + test dimension, 1 is highest, for ordering during batch fixes
- `code_fingerprint`: SHA-256 hash of the problematic code snippet (normalized: whitespace-trimmed). Used for drift-resistant matching
- `anchor_pattern`: Signature of surrounding context (2 lines before + 2 lines after). Used as a secondary locator
- `ast_node_signature`: AST-level anchor — fully qualified method/field/class signature (e.g., `com.example.Class#method(params)`). Extracted from the enclosing AST node of the problematic code identified during testing. Provides refactor-resistant matching — when methods are moved to different files, the developer's Fix Consumption Protocol can locate the code by signature search. Set to `null` for non-Java files. See `javafx-orchestrator/SKILL.md` → Fix Handoff Format → AST Anchor Format for the full specification

## Loop Orchestration Protocol

This protocol is shared across `javafx-developer`, `javafx-code-reviewer`, `javafx-runner`, and `javafx-tester`. It defines the automated closed-loop cycle: **generate → review → verify → test → fix → re-verify**, until the quality gate passes or termination conditions are met.

### Tester's Role in the Loop

`javafx-tester` occupies the **deep test** stage of the loop, triggered **after** `javafx-runner` passes:
- **Trigger condition**: Runner smoke verification passes (project compiles and starts)
- **Round 1**: Full testing — performance + security + accessibility (all three dimensions)
- **Round 2+**: Targeted testing — only dimensions affected by fixes (e.g., if only CSS was fixed, re-test accessibility only)
- **Optional**: Tester can be configured as optional in the loop via `.loop-config.json` with `"deep_testing": false`

### Loop State Machine

```
                         ┌→ Reviewing ─────────────────────────────────────┐
                         │   (reviewer produces Fix Handoffs)              │
[Start] → Generating → ─┤                                                  ├→ Combined Gate
                         │   (runner produces Fix Handoffs)                │   (reviewer AND runner
                         └→ Verifying ────────────────────────────────────┘    both Pass?)
                                                                          ↓ Pass          ↓ Fail
                                                                    Deep Testing     (round < max?) → Fixing
                                                                    (tester)         (round = max?) → [Paused]
                                                                          ↓
                                                                    Test Gate
                                                                    (tester Pass?)
                                                                          ↓ Pass          ↓ Fail
                                                                    [Delivered]    (round < max?) → Fixing
                                                                                   (round = max?) → [Paused]
```

> **Parallel execution**: Reviewer and runner run in parallel — they have no data dependency. Tester runs **after** both pass (tester requires a runnable project). When orchestrated, see `javafx-orchestrator/SKILL.md` for the authoritative protocol definition.

### Loop Rules

| Rule | Definition | Termination |
|------|-----------|-------------|
| Max rounds | Fix → verify → test cycle loops at most 3 rounds | At 3 rounds without pass → pause, report to user |
| Tester trigger | Tester is triggered after runner passes; skipped if runner fails | Runner failure short-circuits: skip tester |
| Re-test strategy | Round 1: full testing; Round 2+: targeted testing (only dimensions touched by fixes) | Targeted testing checks only fix-affected dimensions |
| Convergence detection | Compare current round issue count with previous round | 2 consecutive non-converging rounds → pause, report to user |
| User intervention points | Max rounds reached / non-converging / unfixable issues | Pause with current state report |
| State persistence | Loop state serialized to `.loop-state.json` after each round | Cross-session recovery enabled |

### Quality Gate (Test Gate)

The test gate is evaluated **after** the Combined Gate (reviewer + runner) passes:

| Tester Conclusion | Overall | Action |
|-------------------|---------|--------|
| Pass | Loop Passed | Deliver |
| Conditional Pass | Loop Passed (with notes) | Deliver, record minor issues for next iteration |
| Fail | Loop Continues | Route to developer for Fix Consumption |

> **Test Gate is optional**: If `.loop-config.json` has `"deep_testing": false`, the Test Gate is skipped and the Combined Gate (reviewer + runner) is the final gate. This is useful for projects where deep testing is not yet required.

### Loop State Serialization

The loop state is serialized to `.loop-state.json` in the project root directory by `javafx-developer`. The tester reads this file to determine the current round and select the appropriate testing strategy:

- **Round 1** (after runner passes): Full testing — performance + security + accessibility
- **Round 2+** (state file exists): Targeted testing — only dimensions affected by fixes (identified by `target_file` in the state's `last_fix_handoff`)

#### Tester's Serialization Responsibilities

1. **Read state**: Before starting testing, check for `.loop-state.json`. If found, extract `current_round` and `last_fix_handoff` to determine testing scope
2. **Determine strategy**: Round 1 → Full Testing; Round 2+ → Targeted Testing (only dimensions related to `target_file`s in the fix handoff)
3. **Write result**: After completing testing, update **only** the `rounds[current_round].tester_result` field with conclusion, issue counts by severity, and fix handoff count. Do not modify `reviewer_result` or `runner_result` (field isolation)
4. **Set next action**: If tester passes, set `status: "passed"` and archive; if tester fails, set `next_action: "fixing"` (developer consumes merged fix handoffs from reviewer + runner + tester)

## Constraints

### Execution Safety
1. **Command whitelist**: Only execute build-related commands (`mvn`, `gradle`, `java -version`, `dependency-check`); do not execute arbitrary system commands
2. **Timeout protection**: Performance benchmarks 5 minutes, security scans 10 minutes, accessibility tests 5 minutes; terminate after timeout
3. **No side effects**: Do not modify user project files (only read and execute); fixes are performed by `javafx-developer`
4. **Fuzz testing safety**: Input fuzzing must not delete or modify project data; use in-memory test data only

### Tool Dependencies
1. **OWASP Dependency-Check**: If CLI not available, use Maven plugin (`org.owasp:dependency-check-maven:check`)
2. **JMH**: If not configured in project, use manual `System.nanoTime()` timing (less precise but sufficient)
3. **Monocle**: For headless CI performance testing, require Monocle dependency

## Relationship to Other Skills

- **javafx-runner**: Tester is triggered **after** runner passes. Runner does smoke verification (does it start?), tester does deep testing (is it good enough?)
- **javafx-code-reviewer**: Reviewer's static checks (Memory Leak Risks, Performance, Security Checklist) are complemented by tester's dynamic measurements. Reviewer finds potential issues, tester confirms actual impact
- **javafx-developer**: Developer consumes tester's Fix Handoff entries via the same Fix Consumption Protocol as reviewer and runner reports

## Reference Documents

The tester references the following documents in the `references/` directory:

- `references/performance-testing.md` - Performance testing rules, thresholds, and measurement methodologies
- `references/security-testing.md` - Security testing rules, vulnerability classification, and fuzzing patterns
- `references/accessibility-testing.md` - Accessibility testing rules, WCAG 2.1 criteria, and check methodologies

The tester also cross-references documents from other skills:

- `../javafx-code-reviewer/references/performance-guide.md` - Static performance rules (cross-reference for runtime confirmation)
- `../javafx-code-reviewer/references/security-checklist.md` - Static security rules (cross-reference for dynamic confirmation)
- `../javafx-code-reviewer/references/css-compliance.md` - CSS contrast rules (cross-reference for accessibility)
- `../javafx-runner/references/test-coverage-gate.md` - Coverage threshold rules

## Report Templates

Reusable skeleton templates in the `report-templates/` directory:

- `report-templates/test-report.md` - Test report skeleton template (Markdown, human-readable)
- `report-templates/report-schema.json` - JSON schema for machine-readable report output (CI/CD, IDE, Fix Consumption)
