# JavaFX Verification Report

> **Usage**: This template is a reusable skeleton for verification reports. When generating an actual report, replace all `[placeholder]` content, remove unnecessary sections, and fill in actual verification data. The report must contain three parts: Verification Summary, Issue List, and Verification Result Summary. The fix handoff field format is fully consistent with `javafx-code-reviewer`'s review report, for `javafx-developer` to directly consume.

---

## Verification Summary

- **Verification Mode**: [Full Verification / Incremental Verification / Targeted Dimension Verification]
- **Compile Mode**: [Incremental / Full] — whether incremental compilation was used (default for Round 2+) or full clean compilation
- **Verification Scope**: [List of verification dimensions executed]
- **Environment**: JDK [version] / Maven [version] / JavaFX [version] / OS [platform]
- **Modular**: [Yes / No]
- **Display**: [Available (local desktop) / Unavailable (CI, using Monocle headless)]
- **Verification Commands**: [List of commands actually executed]
- **Verification Date**: [YYYY-MM-DD]
- **Total Issues Found**: [X] (Critical: [a] / Major: [b] / Minor: [c] / Info: [d])
- **Verification Conclusion**: [Pass / Conditional Pass / Fail]

---

## Issue List

> Issues are sorted in descending severity order (Critical -> Major -> Minor -> Info); within the same level, sorted by verification dimension (Compile -> Runtime -> Test -> Packaging). Multiple manifestations of the same root cause are merged into one issue.

### [Critical] Issue 1: [Issue Title]

- **Problem Description**: [Specific description of the verification failure]
- **Verification Dimension**: [Compile Verification / Runtime Verification / Packaging Verification]
- **Code Location**: `[file path]:[line number range]` (if applicable)
- **Error Output**:
  ```
  [Actual compiler / runtime / jpackage output snippet]
  ```
- **Root Cause Analysis**: [Explain why verification failed]
- **Fix Recommendation**: [How to fix it, explaining the fix idea]
- **Corrected Example**:
  ```java
  // [Corrected code or configuration]
  ```
- **Rule Reference**: [Reference to check item in references/, format: `document name -- Check Item title`]
- **Escalation/De-escalation Note**: [If severity deviates from the default baseline, note the triggering condition; if at default baseline, fill "None"]
- **Fix Handoff**:
  - `target_file: [file path]`
  - `target_lines: [start line]-[end line]`
  - `fix_type: [replace / insert / delete]`
  - `fix_priority: 1`
  - `code_fingerprint: [sha256 hash of problematic code snippet]`
  - `anchor_pattern: [2 lines before + 2 lines after, normalized]`
  - `ast_node_signature: [com.example.Class#method(params) or null for non-Java files]`

---

### [Critical] Issue 2: [Issue Title]

- **Problem Description**: [Description]
- **Verification Dimension**: [Dimension name]
- **Code Location**: `[file path]:[line number range]`
- **Error Output**:
  ```
  [Output snippet]
  ```
- **Root Cause Analysis**: [Analysis]
- **Fix Recommendation**: [Recommendation]
- **Corrected Example**:
  ```java
  // [Corrected code]
  ```
- **Rule Reference**: [`document name -- Check Item title`]
- **Escalation/De-escalation Note**: [Note]
- **Fix Handoff**:
  - `target_file: [file path]`
  - `target_lines: [start line]-[end line]`
  - `fix_type: [replace / insert / delete]`
  - `fix_priority: 2`

---

### [Major] Issue 3: [Issue Title]

- **Problem Description**: [Description]
- **Verification Dimension**: [Dimension name]
- **Code Location**: `[file path]:[line number range]`
- **Error Output**:
  ```
  [Output snippet]
  ```
- **Root Cause Analysis**: [Analysis]
- **Fix Recommendation**: [Recommendation]
- **Corrected Example**:
  ```bash
  # [Corrected command or configuration]
  ```
- **Rule Reference**: [`document name -- Check Item title`]
- **Escalation/De-escalation Note**: [Note]
- **Fix Handoff**:
  - `target_file: [file path]`
  - `target_lines: [start line]-[end line]`
  - `fix_type: [replace / insert / delete]`
  - `fix_priority: 3`

---

### [Minor] Issue 4: [Issue Title]

- **Problem Description**: [Description]
- **Verification Dimension**: [Dimension name]
- **Code Location**: `[file path]:[line number range]`
- **Error Output**:
  ```
  [Output snippet]
  ```
- **Root Cause Analysis**: [Analysis]
- **Fix Recommendation**: [Recommendation]
- **Corrected Example**:
  ```java
  // [Corrected code]
  ```
- **Rule Reference**: [`document name -- Check Item title`]
- **Escalation/De-escalation Note**: [Note]
- **Fix Handoff**:
  - `target_file: [file path]`
  - `target_lines: [start line]-[end line]`
  - `fix_type: [replace / insert / delete]`
  - `fix_priority: 4`

---

### [Info] Issue 5: [Issue Title]

- **Problem Description**: [Description]
- **Verification Dimension**: [Dimension name]
- **Code Location**: `[file path]:[line number range]` (if applicable)
- **Error Output**:
  ```
  [Output snippet]
  ```
- **Root Cause Analysis**: [Analysis]
- **Fix Recommendation**: [Recommendation]
- **Corrected Example**:
  ```bash
  # [Corrected command or configuration]
  ```
- **Rule Reference**: [`document name -- Check Item title`]
- **Escalation/De-escalation Note**: [Note]
- **Fix Handoff**:
  - `target_file: [file path]`
  - `target_lines: [start line]-[end line]`
  - `fix_type: [replace / insert / delete]`
  - `fix_priority: 5`

---

## Verification Result Summary

> Lists the check item count, pass count, fail count, skipped count, and pass rate for each dimension.

| Dimension | Check Items | Passed | Failed | Skipped | Compile Mode | Pass Rate |
|-----------|-------------|--------|--------|---------|--------------|-----------|
| Compile Verification | 7 | [N] | [N] | [N] | [Incremental / Full] | [N%] |
| Static Analysis Verification | 6 | [N] | [N] | [N] | — | [N%] |
| Runtime Verification | 10 | [N] | [N] | [N] | — | [N%] |
| Test Verification | 5 | [N] | [N] | [N] | — | [N%] |
| Packaging Verification | 8 | [N] | [N] | [N] | — | [N%] |
| **Total** | **[N]** | **[N]** | **[N]** | **[N]** | | **[N%]** |

> **Skipped Note**: A dimension is skipped when compile verification short-circuits (compile failure prevents running uncompiled code and tests), when test verification short-circuits (test failure prevents packaging untested code), when the toolchain is missing (packaging cannot proceed), or when incremental/targeted mode excludes it.

---

## Fix Handoff Summary

> Sorted by fix_priority (1 is highest). `javafx-developer` can execute fixes item by item in this order.

| Priority | Severity | Dimension | File | Lines | Fix Type | Issue Summary |
|----------|----------|-----------|------|-------|----------|---------------|
| 1 | Critical | Compile Verification | `[file path]` | `[start-end]` | replace | [issue summary] |
| 2 | Critical | Runtime Verification | `[file path]` | `[start-end]` | insert | [issue summary] |
| 3 | Major | Packaging Verification | `[file path]` | `[start-end]` | replace | [issue summary] |
| ... | ... | ... | ... | ... | ... | ... |

---

## UI Preview

> This section embeds the UI screenshot captured during runtime verification, providing visual confirmation of the rendered application. If screenshot capture was not possible, an FXML control tree diagram is shown as a structural preview.

![Main Window Screenshot](target/ui-preview.png)

- **Capture Method**: [Headless Preview API / Monocle + Robot / AWT Robot / FXML control tree diagram]
- **Resolution**: [width]x[height] (if applicable)
- **Capture Time**: [timestamp]
- **Fallback Reason**: [If no screenshot, explain why — e.g., "Monocle not available in CI environment"]

---

## Verification Conclusion

[Based on the issue statistics and severity distribution, give the overall verification conclusion:]

- **Pass**: No Critical or Major issues, all check items pass (or skipped items are documented), pass rate >= 80%
- **Conditional Pass**: Has Major issues but no Critical issues, all Major issues have clear fix plans; runtime verification passes but packaging verification has non-blocking issues
- **Fail**: Has Critical issues (compilation errors, startup failures, etc.), must be fixed before delivery

**Conclusion**: [Pass / Conditional Pass / Fail]

**Recommendations**:
1. [Recommendation 1: e.g., Fix all Critical compile errors first, as they short-circuit subsequent verification]
2. [Recommendation 2: e.g., Plan Major packaging issues within this iteration]
3. [Recommendation 3: e.g., Minor warnings can be batch fixed in the next iteration]

---

## Runtime Findings Feedback

> **Usage**: This section is populated when `javafx-runner` discovers runtime issue patterns NOT covered by `javafx-code-reviewer`'s static rules. These findings are suggestions for the reviewer's rule library — they require human/maintainer review before adoption. Omit this entire section if no novel patterns were found.

### Finding 1: [Pattern Title]

- **Pattern**: [Description of the recurring runtime issue pattern, e.g., "Task.setOnSucceeded callback spawns further background work without returning to FX thread"]
- **Runner Check**: [Which runner check item detected this, e.g., `runtime-verification.md` #6 (Thread Safety)]
- **Suggested Reviewer Rule**:
  - **Target Document**: [e.g., `thread-safety-rules.md`]
  - **Suggested Check Item**: [e.g., "Task Callback Thread Context"]
  - **Description**: [What the new check item should verify]
  - **Suggested Severity**: [Critical / Major / Minor]
- **Evidence**:
  - **Occurrences**: [N times in this project]
  - **Sample Stack Trace**:
    ```
    [Relevant stack trace snippet]
    ```
  - **Affected Files**: [List of files where this pattern was found]

### Finding 2: [Pattern Title]

- **Pattern**: [Description]
- **Runner Check**: [Source check item]
- **Suggested Reviewer Rule**:
  - **Target Document**: [e.g., `memory-management.md`]
  - **Suggested Check Item**: [Title]
  - **Description**: [Description]
  - **Suggested Severity**: [Level]
- **Evidence**:
  - **Occurrences**: [N]
  - **Sample Stack Trace**:
    ```
    [Snippet]
    ```
  - **Affected Files**: [List]

---

> **Feedback Notes**:
> - These findings are **suggestions**, not automatic rule additions — a maintainer must review and approve before adding to reviewer's `references/` documents
> - The goal is to enable skill set self-evolution: runtime-discovered patterns flow back to static rules, so future reviews can catch them earlier
> - If no novel patterns were found (all runtime issues were already covered by reviewer rules), this section should state: "No novel runtime patterns found; all issues were covered by existing reviewer rules."

---

> **Report Notes**:
> - Code snippets, file paths, class names, API names, and command lines in this report remain in English without translation
> - The raw output of the compiler / runtime / jpackage is kept verbatim without translation
> - Rule references uniformly cite `references/` document items, in the format `document name -- Check Item title`
> - Fix Handoff fields can be directly consumed by `javafx-developer` or automation tools to execute fixes
> - `fix_priority` is sorted by severity + verification dimension, 1 is the highest priority, for ordering during batch fixes
> - `code_fingerprint` is the SHA-256 hash of the problematic code snippet (whitespace-normalized), used by `javafx-developer` for drift-resistant matching when line numbers have shifted
> - `anchor_pattern` is the surrounding context signature (2 lines before + 2 lines after), used as a secondary locator when fingerprint matching is ambiguous
> - `ast_node_signature` is the fully qualified method/field/class signature (e.g., `com.example.Class#method(params)`), used as a refactor-resistant locator when code has been moved or renamed; `null` for non-Java files
> - The fix handoff field format is fully consistent with `javafx-code-reviewer`, ensuring `javafx-developer` can consume both reports using the same logic
> - **Closed-Loop**: This report can trigger the automated closed-loop cycle. `javafx-developer` consumes fix handoff entries via its **Fix Consumption Protocol** (Step 5.5), then incremental re-review and re-verify are triggered automatically. See **Loop Orchestration Protocol** in each skill's SKILL.md for loop rules, termination conditions, and the combined quality gate.

---

## JSON Output Format (Optional)

In addition to the Markdown report above, a JSON version can be generated for programmatic consumption by `javafx-developer`, CI/CD pipelines, or IDE plugins. The JSON format contains the same information as the Markdown report but in a machine-readable structure, with a standalone `fix_handoffs` array for direct Fix Consumption.

### JSON Schema

```json
{
  "report_type": "verification",
  "report_version": "1.0",
  "generated_at": "2026-06-29T10:00:00Z",
  "project": "project-name",
  "verification_mode": "full | targeted",
  "round": 1,
  "summary": {
    "conclusion": "Pass | Conditional Pass | Fail",
    "pass_rate": 0.85,
    "total_issues": 8,
    "critical_count": 1,
    "major_count": 2,
    "minor_count": 3,
    "info_count": 2,
    "dimensions": {
      "compile": { "conclusion": "Pass", "issues": 0 },
      "runtime": { "conclusion": "Pass", "issues": 0 },
      "test": { "conclusion": "Conditional Pass", "issues": 3, "jacoco_line_coverage": 0.65 },
      "packaging": { "conclusion": "Pass", "issues": 0 }
    }
  },
  "issues": [
    {
      "id": 1,
      "severity": "Critical",
      "dimension": "Compile Verification",
      "title": "Issue title",
      "description": "Issue description",
      "file": "src/main/java/com/example/App.java",
      "lines": "12-15",
      "rule_reference": "compile-verification.md -- Module Configuration",
      "raw_output": "compiler output...",
      "fix_handoff": {
        "target_file": "src/main/java/com/example/App.java",
        "target_lines": "12-15",
        "fix_type": "replace",
        "fix_priority": 1,
        "code_fingerprint": "sha256-hash-of-snippet",
        "anchor_pattern": "context signature lines",
        "ast_node_signature": "com.example.App#start(Stage)"
      }
    }
  ],
  "fix_handoffs": [
    {
      "target_file": "src/main/java/com/example/App.java",
      "target_lines": "12-15",
      "fix_type": "replace",
      "fix_priority": 1,
      "code_fingerprint": "sha256-hash-of-snippet",
      "anchor_pattern": "context signature lines",
      "ast_node_signature": "com.example.App#start(Stage)",
      "corrected_example": "corrected code...",
      "issue_id": 1,
      "severity": "Critical"
    }
  ],
  "ui_preview": {
    "available": true,
    "method": "Headless Preview API | Monocle + Robot | AWT Robot | FXML control tree diagram",
    "file": "target/ui-preview.png",
    "resolution": "800x600"
  },
  "jacoco_report": {
    "overall_line_coverage": 0.725,
    "controller_line_coverage": 0.653,
    "viewmodel_line_coverage": 0.482,
    "threshold": 0.60,
    "passed": false,
    "uncovered_methods": [
      { "class": "UserViewModel", "method": "validateInput()", "lines_missed": 12 },
      { "class": "UserViewModel", "method": "saveUser()", "lines_missed": 8 }
    ]
  },
  "loop_state": {
    "current_round": 1,
    "convergence_trend": [8],
    "next_action": "fixing | incremental_review_and_verify | passed"
  }
}
```

### Field Reference

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `report_type` | string | Yes | Always `"verification"` |
| `report_version` | string | Yes | Schema version, current `"1.0"` |
| `generated_at` | string | Yes | ISO 8601 timestamp |
| `project` | string | Yes | Project name |
| `verification_mode` | string | Yes | `full` or `targeted` |
| `round` | number | Yes | Current loop round number |
| `summary.dimensions` | object | Yes | Per-dimension conclusions and issue counts |
| `summary.dimensions.test.jacoco_line_coverage` | number | No | Present if JaCoCo report was generated |
| `issues[].fix_handoff` | object | No | Present only for issues with fix plans |
| `fix_handoffs` | array | Yes | Standalone array of all fix handoffs, sorted by `fix_priority` |
| `ui_preview` | object | No | UI screenshot capture result |
| `jacoco_report` | object | No | JaCoCo coverage report summary |
| `loop_state` | object | Yes | Current loop state snapshot |

> **Note**: The JSON report is saved alongside the Markdown report as `verification-report.json`. `javafx-developer`'s Fix Consumption Protocol can parse either format — JSON is preferred for automation, Markdown for human review.
