# JavaFX Code Review Report

> **Usage**: This template is a reusable skeleton for review reports. When generating an actual report, replace all `[placeholder]` content, remove unnecessary sections, and fill in actual review data. The report must contain three parts: Review Summary, Issue List, and Compliance Summary.

---

## Review Summary

- **Review Mode**: [Full Review / Incremental Review / Targeted Dimension Review]
- **Review Scope**: [List of files or dimensions involved]
- **Project Context**: JavaFX [version] / JDK [version] / [Build tool] / [Whether Spring Boot or third-party libraries are integrated]
- **Files Reviewed**: [N] Java / [M] FXML / [K] CSS / [Other]
- **Review Date**: [YYYY-MM-DD]
- **Total Issues Found**: [X] (Critical: [a] / Major: [b] / Minor: [c] / Info: [d])
- **Review Conclusion**: [Pass / Conditional Pass / Fail]

---

## Issue List

> Issues are sorted in descending severity order (Critical -> Major -> Minor -> Info); within the same level, sorted by code location. Multiple manifestations of the same root cause are merged into one issue.

### [Critical] Issue 1: [Issue Title]

- **Dimension**: [Code Structure / UI Thread Safety / FXML Standards / Memory Leak Risks / Performance / Deep Compliance Audit]
- **Problem Description**: [Specific description of the issue, explaining why it is a problem]
- **Code Location**: `[file path]:[line number range]`
- **Problematic Code**:
  ```java
  // [Problematic code snippet]
  ```
- **Impact Analysis**: [Actual runtime impact: crash / performance degradation / memory growth / style-only issue, etc.]
- **Optimization Recommendation**: [How to fix it, explaining the fix idea]
- **Corrected Example**:
  ```java
  // [Corrected code]
  ```
- **Rule Reference**: [Reference to rule item in references/, format: `document name -- Check Item title`]
- **Escalation/De-escalation Note**: [If severity deviates from the default baseline, note the triggering condition; if at default baseline, fill "Default baseline"]
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

- **Dimension**: [Dimension name]
- **Problem Description**: [Description]
- **Code Location**: `[file path]:[line number range]`
- **Problematic Code**:
  ```java
  // [Problematic code snippet]
  ```
- **Impact Analysis**: [Impact]
- **Optimization Recommendation**: [Recommendation]
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

- **Dimension**: [Dimension name]
- **Problem Description**: [Description]
- **Code Location**: `[file path]:[line number range]`
- **Problematic Code**:
  ```java
  // [Problematic code snippet]
  ```
- **Impact Analysis**: [Impact]
- **Optimization Recommendation**: [Recommendation]
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
  - `fix_priority: 3`

---

### [Minor] Issue 4: [Issue Title]

- **Dimension**: [Dimension name]
- **Problem Description**: [Description]
- **Code Location**: `[file path]:[line number range]`
- **Problematic Code**:
  ```java
  // [Problematic code snippet]
  ```
- **Impact Analysis**: [Impact]
- **Optimization Recommendation**: [Recommendation]
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

- **Dimension**: [Dimension name]
- **Problem Description**: [Description]
- **Code Location**: `[file path]:[line number range]`
- **Problematic Code**:
  ```java
  // [Problematic code snippet]
  ```
- **Impact Analysis**: [Impact]
- **Optimization Recommendation**: [Recommendation]
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
  - `fix_priority: 5`

---

## Compliance Summary

> Lists the check item count, pass count, fail count, and pass rate for each dimension.

| Dimension | Check Items | Passed | Failed | Pass Rate |
|-----------|-------------|--------|--------|-----------|
| Code Structure | 5 | [N] | [N] | [N%] |
| UI Thread Safety | 6 | [N] | [N] | [N%] |
| FXML Standards | 8 | [N] | [N] | [N%] |
| Memory Leak Risks | 7 | [N] | [N] | [N%] |
| Performance | 9 | [N] | [N] | [N%] |
| Deep Compliance Audit | [N] | [N] | [N] | [N%] |
| **Total** | **[N]** | **[N]** | **[N]** | **[N%]** |

---

## Review Conclusion

[Based on the issue statistics and severity distribution, give the overall review conclusion:]

- **Pass**: No Critical or Major issues, pass rate >= 80%
- **Conditional Pass**: Has Major issues but no Critical issues, all Major issues have clear fix plans
- **Fail**: Has Critical issues, must be fixed before release

**Conclusion**: [Pass / Conditional Pass / Fail]

**Recommendations**:
1. [Recommendation 1: e.g., Fix all Critical issues first]
2. [Recommendation 2: e.g., Plan Major issue fixes within this iteration]
3. [Recommendation 3: e.g., Minor issues can be batch fixed in the next iteration]

---

> **Report Notes**:
> - Code snippets, file paths, class names, and API names in this report remain in English without translation
> - Rule references uniformly cite `references/` document items, in the format `document name -- Check Item title`
> - Fix Handoff fields can be directly consumed by `javafx-developer` or automation tools to execute fixes
> - `fix_priority` is sorted by severity + code location, 1 is the highest priority, for ordering during batch fixes
> - `code_fingerprint` is the SHA-256 hash of the problematic code snippet (whitespace-normalized), used by `javafx-developer` for drift-resistant matching when line numbers have shifted
> - `anchor_pattern` is the surrounding context signature (2 lines before + 2 lines after), used as a secondary locator when fingerprint matching is ambiguous
> - `ast_node_signature` is the fully qualified method/field/class signature (e.g., `com.example.Class#method(params)`), used as a refactor-resistant locator when code has been moved or renamed; `null` for non-Java files
> - **Closed-Loop**: This report can trigger the automated closed-loop cycle. `javafx-developer` consumes fix handoff entries via its **Fix Consumption Protocol** (Step 5.5), then incremental re-review and re-verify are triggered automatically. See **Loop Orchestration Protocol** in each skill's SKILL.md for loop rules, termination conditions, and the combined quality gate.

---

## JSON Output Format (Optional)

In addition to the Markdown report above, a JSON version can be generated for programmatic consumption by `javafx-developer`, CI/CD pipelines, or IDE plugins. The JSON format contains the same information as the Markdown report but in a machine-readable structure, with a standalone `fix_handoffs` array for direct Fix Consumption.

### JSON Schema

```json
{
  "report_type": "code-review",
  "report_version": "1.0",
  "generated_at": "2026-06-29T10:00:00Z",
  "project": "project-name",
  "review_mode": "full | incremental | targeted",
  "round": 1,
  "summary": {
    "conclusion": "Pass | Conditional Pass | Fail",
    "pass_rate": 0.85,
    "total_issues": 10,
    "critical_count": 2,
    "major_count": 3,
    "minor_count": 4,
    "info_count": 1
  },
  "issues": [
    {
      "id": 1,
      "severity": "Critical",
      "dimension": "Code Structure",
      "title": "Issue title",
      "description": "Issue description",
      "file": "src/main/java/com/example/controller/MainController.java",
      "lines": "45-60",
      "rule_reference": "architecture-patterns.md -- Layer Separation",
      "problematic_code": "code snippet...",
      "corrected_example": "corrected code...",
      "fix_handoff": {
        "target_file": "src/main/java/com/example/controller/MainController.java",
        "target_lines": "45-60",
        "fix_type": "replace",
        "fix_priority": 1,
        "code_fingerprint": "sha256-hash-of-snippet",
        "anchor_pattern": "context signature lines",
        "ast_node_signature": "com.example.controller.MainController#handleSave(ActionEvent)"
      }
    }
  ],
  "fix_handoffs": [
    {
      "target_file": "src/main/java/com/example/controller/MainController.java",
      "target_lines": "45-60",
      "fix_type": "replace",
      "fix_priority": 1,
      "code_fingerprint": "sha256-hash-of-snippet",
      "anchor_pattern": "context signature lines",
      "ast_node_signature": "com.example.controller.MainController#handleSave(ActionEvent)",
      "corrected_example": "corrected code...",
      "issue_id": 1,
      "severity": "Critical"
    }
  ],
  "loop_state": {
    "current_round": 1,
    "convergence_trend": [10],
    "next_action": "fixing | incremental_review_and_verify | passed"
  }
}
```

### Field Reference

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `report_type` | string | Yes | Always `"code-review"` |
| `report_version` | string | Yes | Schema version, current `"1.0"` |
| `generated_at` | string | Yes | ISO 8601 timestamp |
| `project` | string | Yes | Project name |
| `review_mode` | string | Yes | `full`, `incremental`, or `targeted` |
| `round` | number | Yes | Current loop round number |
| `summary.conclusion` | string | Yes | `Pass`, `Conditional Pass`, or `Fail` |
| `summary.pass_rate` | number | Yes | Pass rate 0.0-1.0 |
| `summary.*_count` | number | Yes | Issue count by severity |
| `issues[].fix_handoff` | object | No | Present only for issues with fix plans |
| `fix_handoffs` | array | Yes | Standalone array of all fix handoffs, sorted by `fix_priority` |
| `loop_state` | object | Yes | Current loop state snapshot |

> **Note**: The JSON report is saved alongside the Markdown report as `review-report.json`. `javafx-developer`'s Fix Consumption Protocol can parse either format — JSON is preferred for automation, Markdown for human review.
