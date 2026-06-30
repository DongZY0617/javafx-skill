# EVALUATE.md — JavaFX DocGen Skill Validation

This document validates that `javafx-docgen` correctly generates delivery documentation for JavaFX projects. Each test case provides a project setup, expected behavior, and pass criteria.

---

## Test Case 1: API Reference — Full Javadoc Coverage

**Scenario**: Project has 5 public classes with complete Javadoc on all public methods.

**Project setup**:
```java
/**
 * Manages user CRUD operations.
 * @author DongZY
 * @version 1.0
 */
public class UserService {
    /**
     * Creates a new user with the given details.
     * @param name the user's full name
     * @param email the user's email address
     * @return the created User entity
     * @throws IllegalArgumentException if name is empty
     */
    public User createUser(String name, String email) { ... }
}
```

**Expected docgen behavior**:
- API Reference generated at `docs/api-reference.md`
- All 5 classes documented with class-level Javadoc
- All public methods documented with `@param`, `@return`, `@throws`
- Classes organized by package
- Coverage: 100% classes, 100% methods

**Pass criteria**:
- [ ] `docs/api-reference.md` file is created
- [ ] All 5 public classes appear in the document
- [ ] `@author` and `@version` tags are extracted
- [ ] `@param` tags are listed for each method
- [ ] `@return` tag is documented
- [ ] `@throws` tag is documented
- [ ] Classes are grouped by package

---

## Test Case 2: API Reference — Missing Javadoc

**Scenario**: Project has 5 public classes, but only 2 have Javadoc.

**Expected docgen behavior**:
- API Reference generated with 40% coverage
- 2 classes with full documentation
- 3 classes marked as "No documentation available"
- Warning generated: "3 public classes missing Javadoc comments"
- Conclusion: Pass with warnings

**Pass criteria**:
- [ ] `docs/api-reference.md` file is created
- [ ] 2 classes have full documentation
- [ ] 3 classes show "No documentation available" placeholder
- [ ] Coverage report shows 40% class coverage
- [ ] Warning is recorded in the report
- [ ] Conclusion is "Pass with warnings"

---

## Test Case 3: User Manual — Full FXML Coverage

**Scenario**: Project has 3 FXML files, each with `fx:controller` and multiple UI controls.

**Project setup**: `main.fxml`, `user-dialog.fxml`, `settings.fxml` — each with Buttons, TextFields, TableViews, and `onAction` handlers.

**Expected docgen behavior**:
- User Manual generated at `docs/user-manual.md`
- 3 sections (one per FXML view)
- Each section has: view overview, control inventory table, user workflows
- `onAction` handlers mapped to Controller methods
- Keyboard shortcuts extracted from `KeyCodeCombination`

**Pass criteria**:
- [ ] `docs/user-manual.md` file is created
- [ ] 3 view sections are generated
- [ ] Each section has a control inventory table
- [ ] `onAction` handlers are mapped to Controller method descriptions
- [ ] At least one user workflow is documented per view
- [ ] Keyboard shortcuts (if any) are listed

---

## Test Case 4: User Manual — FXML Without Controller

**Scenario**: One FXML file has no `fx:controller` attribute.

**Expected docgen behavior**:
- User Manual still generated for that FXML
- Control inventory generated (controls listed)
- User workflows section shows "No controller associated — workflows cannot be derived"
- Warning: "1 FXML file has no associated Controller"

**Pass criteria**:
- [ ] FXML without controller is still documented
- [ ] Control inventory is generated
- [ ] Workflow section notes missing controller
- [ ] Warning is recorded

---

## Test Case 5: Architecture Document — Modular Project

**Scenario**: Project has `module-info.java` with requires, exports, opens.

**Project setup**:
```java
module com.example.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    exports com.example.app.controller;
    opens com.example.app.view to javafx.fxml;
}
```

**Expected docgen behavior**:
- Architecture document generated at `docs/architecture.md`
- Module dependencies listed (javafx.controls, javafx.fxml, java.sql)
- Exported packages listed
- Open packages listed
- Mermaid module diagram generated
- Architecture pattern identified (e.g., MVC from package names)

**Pass criteria**:
- [ ] `docs/architecture.md` file is created
- [ ] Module directives are parsed and listed
- [ ] Mermaid module diagram is included
- [ ] Package dependency graph is included
- [ ] Architecture pattern is identified
- [ ] Dependencies from pom.xml are listed

---

## Test Case 6: Architecture Document — Non-Modular Project

**Scenario**: Project has no `module-info.java`.

**Expected docgen behavior**:
- Architecture document still generated
- Module system noted as "Non-modular"
- Package structure analysis still performed
- Architecture pattern identified from package names

**Pass criteria**:
- [ ] Architecture document is generated
- [ ] Module system is noted as "Non-modular"
- [ ] Package structure is documented
- [ ] No module diagram (noted as "N/A — non-modular")

---

## Test Case 7: Changelog — Conventional Commits

**Scenario**: Project has Git history with conventional commit messages.

**Project setup**: Git log shows:
```
abc1234 feat: add user search functionality
def5678 fix: resolve table sorting crash
ghi9012 refactor: extract service layer
jkl3456 docs: update README
```

**Expected docgen behavior**:
- Changelog generated at `docs/CHANGELOG.md`
- Commits grouped by category (Added, Fixed, Changed, Documentation)
- Commit hash + message for each entry

**Pass criteria**:
- [ ] `docs/CHANGELOG.md` file is created
- [ ] "feat" commits are under "Added"
- [ ] "fix" commits are under "Fixed"
- [ ] "refactor" commits are under "Changed"
- [ ] "docs" commits are under "Documentation"
- [ ] Commit hashes are included

---

## Test Case 8: Changelog — No Git History

**Scenario**: Project is not a Git repository or has no commits.

**Expected docgen behavior**:
- Changelog generation skipped
- Warning: "Git not available or no commits — changelog generation skipped"
- Other documents still generated
- Conclusion: Pass with warnings

**Pass criteria**:
- [ ] `docs/CHANGELOG.md` is NOT created (or contains only a placeholder)
- [ ] Warning is recorded
- [ ] Other 4 documents are still generated
- [ ] Conclusion is "Pass with warnings"

---

## Test Case 9: Quick-Start README

**Scenario**: Project with Maven build, JavaFX 21, JDK 17.

**Project setup**: `pom.xml` with:
```xml
<name>User Manager</name>
<version>1.0.0</version>
<description>A JavaFX user management application</description>
<maven.compiler.source>17</maven.compiler.source>
```

**Expected docgen behavior**:
- README generated at `README.md`
- Contains: project name, description, prerequisites, build instructions, run instructions, packaging instructions
- Maven commands: `mvn clean compile`, `mvn javafx:run`
- jpackage commands for Windows/macOS/Linux

**Pass criteria**:
- [ ] `README.md` file is created at project root
- [ ] Project name and description are included
- [ ] Prerequisites section lists JDK 17 and JavaFX 21
- [ ] Build instructions include `mvn clean compile`
- [ ] Run instructions include `mvn javafx:run`
- [ ] Packaging instructions include jpackage commands

---

## Test Case 10: Post-Loop Documentation Generation

**Scenario**: DocGen is triggered after the development loop passes (reviewer + runner + tester all Pass).

**Project setup**: `.loop-state.json` has `status: "passed"`, `current_round: 2`.

**Expected docgen behavior**:
- All 5 document types generated
- Loop state referenced in the docgen report
- Conclusion: Pass (or Pass with warnings if Javadoc is incomplete)

**Pass criteria**:
- [ ] DocGen is triggered after quality gate passes
- [ ] All 5 document types are generated (or skipped with reason)
- [ ] `docs/docgen-report.md` is generated
- [ ] Loop state is included in the report
- [ ] Conclusion is "Pass" or "Pass with warnings"

---

## Test Case 11: Standalone Mode

**Scenario**: User requests documentation generation on an existing project without a loop.

**Project setup**: Existing JavaFX project, no `.loop-state.json`.

**Expected docgen behavior**:
- All 5 document types generated
- `docgen_mode` is "standalone"
- No loop state in the report

**Pass criteria**:
- [ ] DocGen runs independently without loop state
- [ ] `docgen_mode` is "standalone"
- [ ] Loop state section is omitted from the report

---

## Test Case 12: Overwrite Protection

**Scenario**: `docs/` directory and `README.md` already exist.

**Expected docgen behavior**:
- Existing files are backed up with `.bak` extension before overwriting
- New documentation files are generated

**Pass criteria**:
- [ ] Existing `README.md` is backed up as `README.md.bak`
- [ ] Existing `docs/*.md` files are backed up as `*.md.bak`
- [ ] New documentation files are generated successfully

---

## Test Case 13: Dual Output Format

**Scenario**: Verify both Markdown and JSON reports are generated.

**Pass criteria**:
- [ ] `docs/docgen-report.md` is generated and human-readable
- [ ] `docs/docgen-report.json` is generated and validates against `report-schema.json`
- [ ] Both reports contain the same generated documents list
- [ ] `jq .conclusion docgen-report.json` returns the conclusion
- [ ] If `.loop-config.json` has `"output_format": "json"`, only JSON is output

---

## Test Case 14: Non-Blocking Documentation Gate (Default)

**Scenario**: DocGen fails to generate some documents (e.g., Javadoc parsing error). `doc_gate_mode` is "non-blocking" or not set (default).

**Expected docgen behavior**:
- Failed documents are marked as "failed" in the report
- Successfully generated documents are still available
- Conclusion: "Fail" but delivery is NOT blocked
- Warning explains what failed and why
- `gate_blocked` is `false` in the report

**Pass criteria**:
- [ ] Failed documents are marked as "failed" with reason
- [ ] Successfully generated documents are still available
- [ ] Conclusion is "Fail"
- [ ] Delivery is NOT blocked (non-blocking mode)
- [ ] `gate_blocked` is `false` in the report
- [ ] `next_action` is "delivered" (not "fix_documentation")
- [ ] Failure reason is logged in the report

---

## Test Case 15: Blocking Documentation Gate

**Scenario**: `.loop-config.json` has `"doc_gate_mode": "blocking"`. DocGen fails to generate API reference due to Javadoc parsing errors.

**Expected docgen behavior**:
- Failed documents are marked as "failed" in the report
- Conclusion: "Fail"
- Delivery IS blocked — project stays in "passed" state
- `gate_blocked` is `true` in the report
- `next_action` is "fix_documentation"
- Orchestrator does not proceed to deployer phase

**Pass criteria**:
- [ ] Failed documents are marked as "failed" with reason
- [ ] Conclusion is "Fail"
- [ ] Delivery IS blocked (blocking mode)
- [ ] `gate_blocked` is `true` in the report
- [ ] `next_action` is "fix_documentation" (not "delivered")
- [ ] Loop state remains "passed" (not "delivered")
- [ ] Orchestrator does not trigger deployer

---

## Test Case 16: Blocking Gate Bypass via Skip Config

**Scenario**: `.loop-config.json` has `"doc_gate_mode": "blocking"` AND `"docgen": false`.

**Expected docgen behavior**:
- DocGen is skipped entirely
- Gate is bypassed (not evaluated)
- Project is delivered
- Report notes "DocGen skipped per configuration"

**Pass criteria**:
- [ ] No documentation files are generated
- [ ] `docgen_result` is absent or `triggered: false` in loop state
- [ ] Project is delivered (loop state: "delivered")
- [ ] `next_action` is "delivered"
- [ ] No `gate_blocked` field (gate was not evaluated)

---

## Test Case 17: Javadoc HTML Generation

**Scenario**: `.loop-config.json` has `"doc_javadoc_html": true`. Project has 5 public classes with Javadoc comments.

**Expected docgen behavior**:
- Markdown API reference generated at `docs/api-reference.md`
- Javadoc HTML site generated at `docs/api-reference-html/`
- `index.html` exists in the HTML output directory
- All public packages have `package-summary.html` pages
- All public classes have individual HTML pages
- `javadoc_html_generated` is `true` in the report
- `coverage.api_html` section is present with `javadoc_errors: 0`

**Pass criteria**:
- [ ] `docs/api-reference.md` is generated (Markdown, always)
- [ ] `docs/api-reference-html/` directory exists
- [ ] `docs/api-reference-html/index.html` exists
- [ ] No Javadoc errors in Maven output
- [ ] `javadoc_html_generated` is `true` in `docgen-report.json`
- [ ] `coverage.api_html.generated` is `true`
- [ ] `coverage.api_html.javadoc_errors` is 0
- [ ] `coverage.api_html.index_html_exists` is `true`
- [ ] Generated documents list includes an entry with `document_type: "api-reference-html"`

---

## Test Case 18: Javadoc HTML Not Generated (Default)

**Scenario**: `.loop-config.json` does not have `doc_javadoc_html` set (or set to `false`). Project has public classes.

**Expected docgen behavior**:
- Only Markdown API reference generated at `docs/api-reference.md`
- No `docs/api-reference-html/` directory created
- `javadoc_html_generated` is `false` in the report
- No `coverage.api_html` section in the report

**Pass criteria**:
- [ ] `docs/api-reference.md` is generated
- [ ] `docs/api-reference-html/` directory does NOT exist
- [ ] `javadoc_html_generated` is `false` in `docgen-report.json`
- [ ] No `coverage.api_html` section in the report
- [ ] Generated documents list does NOT include `api-reference-html`
