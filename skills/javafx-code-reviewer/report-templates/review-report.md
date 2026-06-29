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
> - **Closed-Loop**: This report can trigger the automated closed-loop cycle. `javafx-developer` consumes fix handoff entries via its **Fix Consumption Protocol** (Step 5.5), then incremental re-review and re-verify are triggered automatically. See **Loop Orchestration Protocol** in each skill's SKILL.md for loop rules, termination conditions, and the combined quality gate.
