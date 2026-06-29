# JavaFX Documentation Generation Report

## Report Summary

- **Project**: [Project Name]
- **Version**: [Project Version]
- **DocGen Mode**: [Post-Loop / Standalone]
- **Generation Date**: [YYYY-MM-DD]
- **Generation Duration**: [Xm Ys]
- **Documents Generated**: [N]
- **Conclusion**: [Pass / Pass with warnings / Fail]

---

## Generated Documents

| # | Document Type | File Path | Status | Content Summary |
|---|--------------|-----------|--------|-----------------|
| 1 | API Reference | `docs/api-reference.md` | [Generated / Skipped / Failed] | [Brief summary] |
| 2 | API Reference (JSON) | `docs/api-reference.json` | [Generated / Skipped / Failed] | [Brief summary] |
| 3 | User Manual | `docs/user-manual.md` | [Generated / Skipped / Failed] | [Brief summary] |
| 4 | Architecture Document | `docs/architecture.md` | [Generated / Skipped / Failed] | [Brief summary] |
| 5 | Changelog | `docs/CHANGELOG.md` | [Generated / Skipped / Failed] | [Brief summary] |
| 6 | Quick-Start README | `README.md` | [Generated / Skipped / Failed] | [Brief summary] |

---

## Coverage Assessment

### API Documentation Coverage

| Metric | Value |
|--------|-------|
| Total public classes | [N] |
| Classes with Javadoc | [N] |
| API coverage rate | [N%] |
| Total public methods | [N] |
| Methods with Javadoc | [N] |
| Method coverage rate | [N%] |
| Total public fields | [N] |
| Fields with Javadoc | [N] |
| Field coverage rate | [N%] |

### User Manual Coverage

| Metric | Value |
|--------|-------|
| Total FXML files | [N] |
| FXML files with manual sections | [N] |
| FXML coverage rate | [N%] |
| Total UI controls documented | [N] |
| Total user workflows documented | [N] |
| Total keyboard shortcuts documented | [N] |

### Architecture Documentation Coverage

| Metric | Value |
|--------|-------|
| Module system | [Modular / Non-modular] |
| Packages documented | [N] |
| Dependencies listed | [N] |
| Architecture pattern | [MVC / MVVM / MVP / Layered] |
| Diagrams generated | [N] |

---

## Project Information

```json
{
  "project_name": "[name]",
  "project_version": "[version]",
  "build_tool": "[Maven / Gradle]",
  "jdk_version": "[version]",
  "javafx_version": "[version]",
  "architecture_pattern": "[MVC / MVVM / MVP / Layered]",
  "module_system": "[Modular / Non-modular]",
  "main_class": "[fully.qualified.MainClass]"
}
```

---

## Warnings

> Warnings do not block delivery but indicate areas for documentation improvement.

- [WARNING-001]: [N] public classes missing Javadoc comments
- [WARNING-002]: [N] public methods missing Javadoc comments
- [WARNING-003]: [N] FXML files have no associated Controller (cannot generate user workflows)
- [WARNING-004]: Git not available or no commits — changelog generation skipped
- [WARNING-005]: [N] conventional commit messages not following Conventional Commits format

---

## Loop State

```json
{
  "loop_status": "passed",
  "current_round": [N],
  "reviewer_conclusion": "Pass",
  "runner_conclusion": "Pass",
  "tester_conclusion": "Pass / Skipped",
  "docgen_conclusion": "Pass / Pass with warnings / Fail"
}
```

---

## JSON Output Format

The JSON output format mirrors this Markdown report structure. See `report-schema.json` for the complete JSON schema. Key fields:

- `generated_documents[]`: List of all generated files with paths, types, and content summaries
- `coverage`: API coverage, FXML coverage, architecture documentation coverage
- `project_info`: Project name, version, JavaFX version, architecture pattern
- `warnings[]`: List of warnings (non-blocking)
