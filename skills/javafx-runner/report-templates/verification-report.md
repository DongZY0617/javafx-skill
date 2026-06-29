# JavaFX Verification Report

> **Usage**: This template is a reusable skeleton for verification reports. When generating an actual report, replace all `[placeholder]` content, remove unnecessary sections, and fill in actual verification data. The report must contain three parts: Verification Summary, Issue List, and Verification Result Summary. The fix handoff field format is fully consistent with `javafx-code-reviewer`'s review report, for `javafx-developer` to directly consume.

---

## Verification Summary

- **Verification Mode**: [Full Verification / Incremental Verification / Targeted Dimension Verification]
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

| Dimension | Check Items | Passed | Failed | Skipped | Pass Rate |
|-----------|-------------|--------|--------|---------|-----------|
| Compile Verification | 7 | [N] | [N] | [N] | [N%] |
| Runtime Verification | 10 | [N] | [N] | [N] | [N%] |
| Test Verification | 5 | [N] | [N] | [N] | [N%] |
| Packaging Verification | 8 | [N] | [N] | [N] | [N%] |
| **Total** | **[N]** | **[N]** | **[N]** | **[N]** | **[N%]** |

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
> - The fix handoff field format is fully consistent with `javafx-code-reviewer`, ensuring `javafx-developer` can consume both reports using the same logic
> - **Closed-Loop**: This report can trigger the automated closed-loop cycle. `javafx-developer` consumes fix handoff entries via its **Fix Consumption Protocol** (Step 5.5), then incremental re-review and re-verify are triggered automatically. See **Loop Orchestration Protocol** in each skill's SKILL.md for loop rules, termination conditions, and the combined quality gate.
