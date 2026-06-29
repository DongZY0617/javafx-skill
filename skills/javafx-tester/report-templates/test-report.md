# JavaFX Test Report

## Test Summary

- **Test Mode**: [Full Testing / Targeted Dimension Testing / Selective Testing]
- **Test Scope**: [List of test dimensions executed]
- **Project**: [Project Name]
- **Environment**: JDK [version] / Maven [version] / JavaFX [version] / OS [platform]
- **Display**: [Available (local desktop) / Unavailable (CI, using Monocle headless)]
- **Test Date**: [YYYY-MM-DD]
- **Test Duration**: [Xm Ys]
- **Total Issues Found**: [X] (Critical: [a] / Major: [b] / Minor: [c] / Info: [d])
- **Test Conclusion**: [Pass / Conditional Pass / Fail]

---

## Test Result Summary

| Dimension | Check Items | Passed | Failed | Skipped | Pass Rate |
|-----------|-------------|--------|--------|---------|-----------|
| Performance Testing | 5 | [N] | [N] | [N] | [N%] |
| Security Testing | 8 | [N] | [N] | [N] | [N%] |
| Accessibility Testing | 7 | [N] | [N] | [N] | [N%] |
| **Total** | **[N]** | **[N]** | **[N]** | **[N]** | **[N%]** |

---

## Performance Metrics

### Startup Time

| Run # | Cold Startup (ms) | Warm Startup (ms) | JVM Start (ms) | JavaFX Init (ms) | FXML Load (ms) |
|-------|-------------------|-------------------|----------------|-------------------|----------------|
| 1 | [N] | [N] | [N] | [N] | [N] |
| 2 | [N] | [N] | [N] | [N] | [N] |
| 3 | [N] | [N] | [N] | [N] | [N] |
| **Median** | **[N]** | **[N]** | **[N]** | **[N]** | **[N]** |

- **Threshold**: ≤ 3000ms (Pass) / 3000-5000ms (Minor) / 5000-10000ms (Major) / > 10000ms (Critical)
- **Result**: [Pass / Minor / Major / Critical]

### UI Response Latency

| Interaction | Response Time (ms) | Threshold | Result |
|-------------|---------------------|-----------|--------|
| Button click → result display | [N] | ≤ 100ms | [Pass / Minor / Major / Critical] |
| Search input → filtered results | [N] | ≤ 100ms | [Pass / Minor / Major / Critical] |
| Table row selection → detail panel | [N] | ≤ 100ms | [Pass / Minor / Major / Critical] |
| Tab/View switch → new view rendered | [N] | ≤ 100ms | [Pass / Minor / Major / Critical] |
| Data save → confirmation message | [N] | ≤ 100ms | [Pass / Minor / Major / Critical] |

### Memory Footprint

| Metric | Value | Threshold | Result |
|--------|-------|-----------|--------|
| Baseline heap (after startup) | [N] MB | [expected range] | [Pass / Minor / Major] |
| Heap after 5min usage | [N] MB | — | — |
| Net growth (after GC) | [N] MB | < 10MB (Pass) / 10-50MB (Minor) / 50-100MB (Major) / > 100MB (Critical) | [Pass / Minor / Major / Critical] |
| Full GC frequency | [N]/min | < 1/min (Pass) / 1-3/min (Minor) / > 3/min (Major) | [Pass / Minor / Major] |

---

## Security Findings

### Dependency Vulnerability Scan

| CVE ID | CVSS Score | Severity | Affected Dependency | Description | Remediation |
|--------|-----------|----------|---------------------|-------------|-------------|
| [CVE-XXXX-XXXXX] | [N.N] | [Critical/Major/Minor] | [group:artifact:version] | [Description] | [Upgrade to X.Y.Z] |

- **Scan Tool**: [OWASP Dependency-Check CLI / Maven Plugin]
- **Scan Date**: [YYYY-MM-DD]
- **Total CVEs Found**: [N] (Critical: [a] / High: [b] / Medium: [c] / Low: [d])
- **Suppression File**: [Exists / Not exists / Valid / Invalid]

### Input Fuzz Testing

| Input Field | Fuzz Pattern | Result | Exception | Severity |
|------------|-------------|--------|-----------|----------|
| [field name] | Empty string | [Pass/Fail] | [exception or "none"] | [Pass/Major/Critical] |
| [field name] | Very long string (10000 chars) | [Pass/Fail] | [exception or "none"] | [Pass/Major/Critical] |
| [field name] | SQL injection: `' OR 1=1 --` | [Pass/Fail] | [exception or "none"] | [Pass/Major/Critical] |
| [field name] | XSS: `<script>alert(1)</script>` | [Pass/Fail] | [exception or "none"] | [Pass/Major/Critical] |
| [field name] | Path traversal: `../../etc/passwd` | [Pass/Fail] | [exception or "none"] | [Pass/Major/Critical] |
| ... | ... | ... | ... | ... |

### WebView Security

> [Skipped — project does not use WebView / Completed]

| Check Item | Result | Severity | Notes |
|-----------|--------|----------|-------|
| JavaScript access control | [Pass/Fail/N/A] | [Pass/Major/Critical] | [notes] |
| Content source validation | [Pass/Fail/N/A] | [Pass/Major/Critical] | [notes] |
| Cross-origin restrictions | [Pass/Fail/N/A] | [Pass/Major/Critical] | [notes] |

---

## Accessibility Results

### Keyboard Navigation

| Check Item | Result | Severity | Notes |
|-----------|--------|----------|-------|
| Tab order traversal | [Pass/Fail] | [Pass/Minor/Major/Critical] | [N/N controls reachable] |
| Focus visibility | [Pass/Fail] | [Pass/Minor/Major] | [notes] |
| Keyboard shortcuts | [Pass/Fail] | [Pass/Minor/Major] | [N/N interactions have keyboard equivalents] |

### Color Contrast

| Element | Foreground | Background | Contrast Ratio | Threshold | Result |
|---------|-----------|------------|---------------|-----------|--------|
| [Label/Button name] | [#xxxxxx] | [#xxxxxx] | [N.NN:1] | ≥ 4.5:1 | [Pass/Fail] |
| [Label/Button name] | [#xxxxxx] | [#xxxxxx] | [N.NN:1] | ≥ 3.0:1 (large) | [Pass/Fail] |
| ... | ... | ... | ... | ... | ... |

- **Total Text Elements Checked**: [N]
- **Passing**: [N] / **Failing**: [N]

### Screen Reader Compatibility

| Check Item | Coverage | Threshold | Result |
|-----------|----------|-----------|--------|
| accessibleText on interactive controls | [N%] (N/N) | 100% (Pass) / 70-99% (Minor) / < 70% (Major) | [Pass/Minor/Major] |
| accessibleHelp on interactive controls | [N%] (N/N) | Same as accessibleText | [Pass/Minor/Major] |
| Image alt text | [N%] (N/N) | All informational images | [Pass/Minor] |
| Live region support | [Pass/Fail] | — | [Pass/Minor/Major] |

---

## Issue List

> Sorted by severity descending (Critical → Major → Minor → Info), then by test dimension (Performance → Security → Accessibility).

### ISSUE-001: [Issue Title]

- **Severity**: [Critical / Major / Minor / Info]
- **Dimension**: [Performance Testing / Security Testing / Accessibility Testing]
- **Test Check Item**: [Specific check item that detected this issue]
- **File**: [path/to/file.java]
- **Lines**: [start-end]
- **Description**: [Detailed description of the issue]
- **Problematic Code**:
  ```java
  // [code snippet]
  ```
- **Root Cause Analysis**: [Why this issue occurs]
- **Corrected Example**:
  ```java
  // [corrected code]
  ```
- **Rule Reference**: [references/document.md -- Check Item title]
- **Escalation Note**: [Default baseline / Escalated because: reason]
- **Fix Handoff**:
  - `target_file`: [path/to/file.java]
  - `target_lines`: [start-end]
  - `fix_type`: [replace / insert / delete]
  - `fix_priority`: [N]
  - `code_fingerprint`: [sha256...]
  - `anchor_pattern`: [context signature]

### ISSUE-002: [Issue Title]

[... same format ...]

---

## Fix Handoff List

> Standalone array, sorted by `fix_priority` ascending (1 = highest). Fix handoff format is fully consistent with `javafx-code-reviewer` and `javafx-runner`.

| # | Priority | Severity | Target File | Target Lines | Fix Type | Issue ID |
|---|----------|----------|-------------|--------------|----------|----------|
| 1 | [N] | [Critical/Major/Minor] | [file] | [lines] | [replace/insert/delete] | [ISSUE-XXX] |
| 2 | [N] | [Critical/Major/Minor] | [file] | [lines] | [replace/insert/delete] | [ISSUE-XXX] |
| ... | ... | ... | ... | ... | ... | ... |

---

## Loop State

```json
{
  "current_round": [N],
  "convergence_trend": [[N], [N], ...],
  "next_action": "fixing | incremental_test | passed | paused"
}
```

---

## JSON Output Format

The JSON output format mirrors this Markdown report structure. See `report-schema.json` for the complete JSON schema. Key fields:

- `summary.conclusion`: CI/CD quality gate decision
- `performance_metrics`: All timing and memory metrics
- `security_findings`: CVE list and fuzz test results
- `accessibility_results`: Contrast ratios and coverage metrics
- `fix_handoffs[]`: Standalone fix handoff array (consistent with reviewer and runner)
