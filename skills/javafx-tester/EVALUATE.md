# EVALUATE.md — JavaFX Tester Skill Validation

This document validates that `javafx-tester` correctly identifies performance, security, and accessibility issues in JavaFX projects. Each test case provides a project setup, expected behavior, and pass criteria.

---

## Test Case 1: Startup Time — Critical Failure

**Scenario**: Controller's `initialize()` loads 10,000 records synchronously on the FX thread.

**Project setup**:
```java
public class UserController implements Initializable {
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Loads 10,000 records synchronously — blocks FX thread
        List<User> users = userRepository.findAll(); // 8 second query
        userTable.getItems().addAll(users);
    }
}
```

**Expected tester behavior**:
- Performance Testing dimension: Startup Time
- Cold startup median: > 5,000ms (likely 8,000-10,000ms)
- Severity: Major (5-10s range) or Critical (> 10s)
- Issue generated with Fix Handoff targeting `UserController.java`
- Corrected example should show lazy loading or background task

**Pass criteria**:
- [ ] Tester measures startup time and records > 5,000ms
- [ ] Issue severity is Major or Critical
- [ ] Fix Handoff `target_file` is `UserController.java`
- [ ] `fix_type` is `replace`
- [ ] Corrected example includes `Task` or `Service` for background loading
- [ ] Cross-reference to `../javafx-code-reviewer/references/performance-guide.md` is noted

---

## Test Case 2: UI Response Latency — Critical Failure

**Scenario**: Search field triggers synchronous database query on every keystroke without debounce.

**Project setup**:
```java
@FXML
private void handleSearchInput(KeyEvent event) {
    String query = searchField.getText();
    // Synchronous DB query on every keystroke — blocks FX thread
    List<User> results = userRepository.searchByName(query);
    searchResults.getItems().setAll(results);
}
```

**Expected tester behavior**:
- Performance Testing dimension: UI Response Latency
- Response time for "search input → filtered results": > 1,000ms
- Severity: Critical (> 1000ms, UI appears frozen)
- Issue generated with Fix Handoff

**Pass criteria**:
- [ ] Tester measures response latency and records > 1,000ms
- [ ] Issue severity is Critical
- [ ] Fix Handoff targets `handleSearchInput` method
- [ ] Corrected example includes debounce mechanism (`PauseTransition` or `Timeline`)

---

## Test Case 3: Memory Leak — Major Failure

**Scenario**: Controller adds event listeners but never removes them on window close.

**Project setup**:
```java
public class MainController {
    @FXML
    private TableView<User> userTable;

    @FXML
    public void initialize() {
        // Listener added but never removed — memory leak
        userManager.usersProperty().addListener((obs, old, newVal) -> {
            userTable.getItems().setAll(newVal);
        });
    }
    // No cleanup() or @PreDestroy method
}
```

**Expected tester behavior**:
- Performance Testing dimension: Memory Footprint
- Memory growth: 50-100MB over 5 minutes (Major)
- Issue generated with Fix Handoff
- Cross-reference to reviewer's Memory Leak Risks dimension

**Pass criteria**:
- [ ] Tester detects memory growth > 50MB over 5 min
- [ ] Issue severity is Major
- [ ] Fix Handoff targets listener registration line
- [ ] Corrected example includes `WeakChangeListener` or explicit `removeListener` in cleanup method

---

## Test Case 4: Dependency Vulnerability — Critical Failure

**Scenario**: Project uses log4j-core 2.14.0 (CVE-2021-44228, Log4Shell).

**Project setup**: `pom.xml` contains:
```xml
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.14.0</version>
</dependency>
```

**Expected tester behavior**:
- Security Testing dimension: Dependency Vulnerability Scan
- CVE-2021-44228 detected, CVSS 10.0
- Severity: Critical
- Issue generated with Fix Handoff targeting `pom.xml`
- Remediation: upgrade to 2.17.1+

**Pass criteria**:
- [ ] OWASP Dependency-Check is executed (Maven plugin or CLI)
- [ ] CVE-2021-44228 is detected and reported
- [ ] CVSS score is recorded as 10.0
- [ ] Issue severity is Critical
- [ ] Fix Handoff `target_file` is `pom.xml`
- [ ] Corrected example shows version `2.17.1` or later

---

## Test Case 5: SQL Injection — Critical Failure

**Scenario**: Login dialog uses string concatenation for SQL query.

**Project setup**:
```java
public boolean login(String username, String password) {
    String sql = "SELECT * FROM users WHERE username = '" + username
               + "' AND password = '" + password + "'";
    // Vulnerable to SQL injection
    return jdbcTemplate.queryForList(sql).size() > 0;
}
```

**Expected tester behavior**:
- Security Testing dimension: Input Fuzz Testing
- Fuzz pattern `' OR 1=1 --` injected into username field
- Result: query returns all users (injection successful)
- Severity: Critical
- Issue generated with Fix Handoff

**Pass criteria**:
- [ ] Fuzz testing injects SQL patterns into username field
- [ ] Injection is detected (query returns unexpected results or no exception)
- [ ] Issue severity is Critical
- [ ] Fix Handoff targets `login` method
- [ ] Corrected example uses `PreparedStatement` with parameterized query

---

## Test Case 6: Color Contrast — Major Failure

**Scenario**: CSS uses light gray text (#999999) on white background (#ffffff) for body text.

**Project setup**: `styles.css` contains:
```css
.body-text {
    -fx-text-fill: #999999;
    -fx-background-color: #ffffff;
    -fx-font-size: 14px;
}
```

**Expected tester behavior**:
- Accessibility Testing dimension: Color Contrast
- Contrast ratio: 2.85:1 (calculated: (0.359 + 0.05) / (1.0 + 0.05) ≈ 0.39 → ratio ≈ 2.85)
- Below WCAG AA threshold (4.5:1 for normal text)
- Severity: Major
- Issue generated with Fix Handoff targeting `styles.css`

**Pass criteria**:
- [ ] Tester parses CSS and extracts color values
- [ ] Contrast ratio is calculated and recorded
- [ ] Ratio is below 4.5:1 threshold
- [ ] Issue severity is Major
- [ ] Fix Handoff `target_file` is `styles.css`
- [ ] Corrected example uses darker text color (e.g., `#595959` for 4.5:1 ratio)

---

## Test Case 7: Keyboard Navigation — Major Failure

**Scenario**: Critical "Delete" button has `focusTraversable="false"` set in FXML.

**Project setup**: `main.fxml` contains:
```xml
<Button text="Delete" onAction="#handleDelete" focusTraversable="false"/>
```

**Expected tester behavior**:
- Accessibility Testing dimension: Keyboard Navigation
- Tab order traversal: "Delete" button not reachable via Tab
- Severity: Major (critical control not keyboard accessible)
- Issue generated with Fix Handoff targeting FXML

**Pass criteria**:
- [ ] Tester identifies "Delete" button as not reachable via Tab
- [ ] Issue severity is Major
- [ ] Fix Handoff `target_file` is the FXML file
- [ ] Corrected example removes `focusTraversable="false"` or sets to `true`

---

## Test Case 8: Accessible Text Missing — Minor Failure

**Scenario**: Application has 10 interactive controls, only 3 have `accessibleText`.

**Project setup**: Multiple FXML controls without `accessibleText`:
```xml
<Button text="Save" onAction="#handleSave"/>           <!-- missing accessibleText -->
<Button text="Cancel" onAction="#handleCancel"/>        <!-- missing accessibleText -->
<TextField fx:id="nameField" promptText="Enter name"/>  <!-- missing accessibleText -->
<Button text="Save" onAction="#handleSave" accessibleText="Save current changes to database"/>  <!-- has accessibleText -->
```

**Expected tester behavior**:
- Accessibility Testing dimension: Screen Reader Compatibility
- accessibleText coverage: 30% (3/10)
- Severity: Major (< 40% threshold)
- Issue generated for missing accessibleText controls

**Pass criteria**:
- [ ] Tester scans all FXML files for interactive controls
- [ ] Coverage is calculated as 30% (or appropriate ratio)
- [ ] Issue severity is Major
- [ ] Fix Handoff targets the FXML files with missing accessibleText
- [ ] Corrected example adds `accessibleText` attribute

---

## Test Case 9: WebView Security — Critical Failure

**Scenario**: Application loads user-provided URL in WebView with JavaScript enabled.

**Project setup**:
```java
public class BrowserController {
    @FXML private WebView webView;

    public void loadUrl(String url) {
        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.load(url);  // User-provided URL, no validation
    }
}
```

**Expected tester behavior**:
- Security Testing dimension: WebView Security
- JavaScript enabled with untrusted content: Major
- User-provided URL without validation: Major
- If URL can be `file://`: Critical
- Issue generated with Fix Handoff

**Pass criteria**:
- [ ] Tester detects `setJavaScriptEnabled(true)` with user-provided URL
- [ ] URL validation absence is detected
- [ ] Issue severity is Major or Critical
- [ ] Fix Handoff targets `loadUrl` method
- [ ] Corrected example includes URL validation and/or disables JavaScript for untrusted content

---

## Test Case 10: All Pass — No Issues

**Scenario**: Well-optimized JavaFX application with proper performance, security, and accessibility.

**Project setup**:
- Startup uses lazy loading and background tasks
- All inputs validated with parameterized queries
- All controls have `accessibleText` and proper contrast
- Dependencies are up-to-date (no CVEs)
- Memory is stable (listeners properly removed)

**Expected tester behavior**:
- All three dimensions: Pass
- No issues generated
- Test conclusion: Pass
- All performance metrics within thresholds

**Pass criteria**:
- [ ] Startup time ≤ 3,000ms
- [ ] UI response latency ≤ 100ms for all interactions
- [ ] Memory growth < 10MB over 5 min
- [ ] No CVEs found in dependency scan
- [ ] All fuzz test patterns result in Pass
- [ ] Keyboard navigation: all controls reachable
- [ ] Color contrast: all elements ≥ 4.5:1
- [ ] accessibleText coverage: 100%
- [ ] Test conclusion: Pass
- [ ] No Fix Handoff entries generated

---

## Test Case 11: Fix Handoff Format Consistency

**Scenario**: Verify that tester's Fix Handoff format is identical to reviewer and runner.

**Validation**: Compare Fix Handoff fields across all three skills' reports.

**Pass criteria**:
- [ ] `target_file` field present and is a string
- [ ] `target_lines` field present and matches pattern `\d+(-\d+)?`
- [ ] `fix_type` field present and is one of `replace`, `insert`, `delete`
- [ ] `fix_priority` field present and is integer ≥ 1
- [ ] `code_fingerprint` field present and matches `^[a-f0-9]{64}$`
- [ ] `anchor_pattern` field present and is a string
- [ ] `corrected_example` field present (for replace/insert) or absent (for delete)
- [ ] JSON report validates against `report-schema.json`

---

## Test Case 12: Loop State Serialization

**Scenario**: Tester runs in Round 2 of the loop, reads state file, writes result.

**Validation**: Verify Loop State read/write behavior.

**Pass criteria**:
- [ ] Tester reads `.loop-state.json` and extracts `current_round` and `last_fix_handoff`
- [ ] Round 2+ uses targeted testing (only dimensions affected by fixes)
- [ ] Tester writes only to `rounds[current_round].tester_result` field
- [ ] Tester does not modify `reviewer_result` or `runner_result`
- [ ] `next_action` is set to `"fixing"` if tester fails, `"passed"` if tester passes

---

## Test Case 13: Dual Output Format

**Scenario**: Verify both Markdown and JSON reports are generated.

**Pass criteria**:
- [ ] `test-report.md` is generated and is human-readable
- [ ] `test-report.json` is generated and validates against `report-schema.json`
- [ ] Both reports contain the same issues and Fix Handoffs
- [ ] `jq .summary.conclusion test-report.json` returns the conclusion
- [ ] If `.loop-config.json` has `"output_format": "json"`, only JSON is output
