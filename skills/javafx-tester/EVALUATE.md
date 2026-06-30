# EVALUATE.md — JavaFX Tester Skill Validation

This document validates that `javafx-tester` correctly identifies performance, security, accessibility, and visual regression issues in JavaFX projects. Each test case provides a project setup, expected behavior, and pass criteria.

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

**Scenario**: Well-optimized JavaFX application with proper performance, security, accessibility, and visual regression.

**Project setup**:
- Startup uses lazy loading and background tasks
- All inputs validated with parameterized queries
- All controls have `accessibleText` and proper contrast
- Dependencies are up-to-date (no CVEs)
- Memory is stable (listeners properly removed)
- Visual baselines exist for all views and match current rendering

**Expected tester behavior**:
- All four dimensions: Pass
- No issues generated
- Test conclusion: Pass
- All performance metrics within thresholds
- All visual regression snapshots within diff threshold

**Pass criteria**:
- [ ] Startup time ≤ 3,000ms
- [ ] UI response latency ≤ 100ms for all interactions
- [ ] Memory growth < 10MB over 5 min
- [ ] No CVEs found in dependency scan
- [ ] All fuzz test patterns result in Pass
- [ ] Keyboard navigation: all controls reachable
- [ ] Color contrast: all elements ≥ 4.5:1
- [ ] accessibleText coverage: 100%
- [ ] All visual regression snapshots have diff_ratio < 2%
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
- [ ] `ast_node_signature` field present: non-null for Java files (matches `{package}.{Class}#{method(params)}`), null for non-Java files (FXML/CSS/module-info.java)
- [ ] `corrected_example` field present (for replace/insert) or absent (for delete)
- [ ] JSON report validates against `report-schema.json`

---

## Test Case 12: Loop State Serialization (Parallel Track Fields)

**Scenario**: Tester runs in Round 2 of the loop, reads state file, writes results to four isolated track fields.

**Validation**: Verify Loop State read/write behavior with parallel track field isolation.

**Pass criteria**:
- [ ] Tester reads `.loop-state.json` and extracts `current_round` and `last_fix_handoff`
- [ ] Round 2+ uses targeted testing (only dimensions affected by fixes)
- [ ] Performance track writes only to `rounds[current_round].tester_perf_result`
- [ ] Security track writes only to `rounds[current_round].tester_sec_result`
- [ ] Accessibility track writes only to `rounds[current_round].tester_a11y_result`
- [ ] Visual Regression track writes only to `rounds[current_round].tester_vr_result`
- [ ] No track modifies `reviewer_result`, `runner_result`, or another track's field
- [ ] Aggregated `tester_result` is computed after all tracks complete (not written by individual tracks)
- [ ] `next_action` is set to `"fixing"` if aggregated tester_result fails, `"passed"` if it passes

---

## Test Case 13: Dual Output Format

**Scenario**: Verify both Markdown and JSON reports are generated.

**Pass criteria**:
- [ ] `test-report.md` is generated and is human-readable
- [ ] `test-report.json` is generated and validates against `report-schema.json`
- [ ] Both reports contain the same issues and Fix Handoffs
- [ ] `jq .summary.conclusion test-report.json` returns the conclusion
- [ ] If `.loop-config.json` has `"output_format": "json"`, only JSON is output

---

## Test Case 14: Parallel Execution — All Four Tracks Run Simultaneously

**Scenario**: Full testing mode with `tester_parallel: true` (default). Verify the four dimensions execute concurrently, not sequentially.

**Project setup**: A well-structured JavaFX application with performance, security, accessibility, and visual regression concerns across all four dimensions. TestFX + Monocle dependencies present in `pom.xml`.

**Expected tester behavior**:
- All four tracks (Performance, Security, Accessibility, Visual Regression) are dispatched simultaneously
- Each track launches its own app instance or performs static analysis independently
- `parallel_execution.execution_mode` is `"parallel"` in the JSON report
- `parallel_execution.wall_clock_ms` < `parallel_execution.sum_of_track_ms` (parallel speedup)
- `parallel_execution.speedup_factor` > 1.0

**Pass criteria**:
- [ ] All four tracks have `start_time` values within 1 second of each other
- [ ] `parallel_execution.tracks.performance.status` is `"completed"`
- [ ] `parallel_execution.tracks.security.status` is `"completed"`
- [ ] `parallel_execution.tracks.accessibility.status` is `"completed"`
- [ ] `parallel_execution.tracks.visual_regression.status` is `"completed"`
- [ ] `wall_clock_ms` is less than `sum_of_track_ms`
- [ ] `speedup_factor` is greater than 1.5 (realistic parallel speedup)
- [ ] Report contains issues from all four dimensions

---

## Test Case 15: Partial Failure — One Track Times Out, Others Continue

**Scenario**: Security track's OWASP Dependency-Check takes longer than 10 minutes (timeout), but Performance and Accessibility tracks complete normally.

**Project setup**: Project with many dependencies causing slow OWASP scan, plus normal performance and accessibility characteristics.

**Expected tester behavior**:
- Performance track completes normally with `status: "completed"`
- Security track times out with `status: "timeout"`, `conclusion: "Skipped"`, and `error_message` present
- Accessibility track completes normally with `status: "completed"`
- Overall conclusion is `Conditional Pass` (one track skipped, none failed)
- `tester_sec_result.conclusion` is `"Skipped"` in loop state

**Pass criteria**:
- [ ] Performance track `status` is `"completed"` and `conclusion` is not `"Skipped"`
- [ ] Security track `status` is `"timeout"` and `conclusion` is `"Skipped"`
- [ ] Security track `error_message` field is present and non-empty
- [ ] Accessibility track `status` is `"completed"` and `conclusion` is not `"Skipped"`
- [ ] Aggregated `tester_result.conclusion` is `"Conditional Pass"`
- [ ] Issues from Performance and Accessibility tracks are present in the report
- [ ] Security track's partial findings (if any were collected before timeout) are included

---

## Test Case 16: Field Isolation — No Cross-Track State Pollution

**Scenario**: Verify that each track writes only to its own state field and does not corrupt another track's results.

**Project setup**: Project with issues in all four dimensions (startup delay, SQL injection, missing accessibleText, visual layout regression).

**Validation**: Inspect `.loop-state.json` after tester completes and verify field integrity.

**Pass criteria**:
- [ ] `tester_perf_result` contains only performance-related issue counts (critical/major/minor/info)
- [ ] `tester_sec_result` contains only security-related issue counts
- [ ] `tester_a11y_result` contains only accessibility-related issue counts
- [ ] `tester_vr_result` contains only visual regression issue counts plus snapshot metrics
- [ ] Sum of all four tracks' `critical` counts equals `tester_result.critical`
- [ ] Sum of all four tracks' `major` counts equals `tester_result.major`
- [ ] `reviewer_result` and `runner_result` fields are unchanged from before tester ran
- [ ] Each track's `duration_ms` is recorded and positive
- [ ] No track's field contains data belonging to another dimension

---

## Test Case 17: Aggregation Gate — Overall Conclusion Computation

**Scenario**: Verify the aggregation gate correctly computes the overall conclusion from four track conclusions.

**Test matrix** (multiple sub-scenarios):

| Perf | Security | A11y | VR | Expected Overall |
|------|----------|------|----|------------------|
| Pass | Pass | Pass | Pass | Pass |
| Pass | Fail | Pass | Pass | Fail |
| Conditional Pass | Pass | Conditional Pass | Pass | Conditional Pass |
| Pass | Skipped | Pass | Pass | Conditional Pass |
| Fail | Fail | Pass | Pass | Fail |
| Pass | Pass | Pass | Skipped | Conditional Pass |
| Pass | Pass | Pass | Fail | Fail |

**Pass criteria**:
- [ ] All seven sub-scenarios produce the expected overall conclusion
- [ ] When any track is `Fail`, overall is `Fail`
- [ ] When no track is `Fail` but at least one is `Skipped`, overall is `Conditional Pass`
- [ ] When no track is `Fail` or `Skipped` but at least one is `Conditional Pass`, overall is `Conditional Pass`
- [ ] When all tracks are `Pass`, overall is `Pass`
- [ ] `pass_rate` is computed as `(total checks - failed checks) / total checks` across all four tracks

---

## Test Case 18: Sequential Fallback — tester_parallel: false

**Scenario**: `.loop-config.json` has `"tester_parallel": false`. Verify the tester falls back to sequential execution while maintaining field isolation.

**Project setup**: Same project as Test Case 14, but with `tester_parallel: false` in `.loop-config.json`.

**Expected tester behavior**:
- Four dimensions execute sequentially (Performance → Security → Accessibility → Visual Regression)
- `parallel_execution.execution_mode` is `"sequential"` in the JSON report
- `parallel_execution.wall_clock_ms` ≈ `parallel_execution.sum_of_track_ms` (no speedup)
- `parallel_execution.speedup_factor` ≈ 1.0
- Field isolation rules still apply — each dimension writes to its own field

**Pass criteria**:
- [ ] `parallel_execution.execution_mode` is `"sequential"`
- [ ] Track start times are sequential (Security starts after Performance ends, Accessibility starts after Security ends, Visual Regression starts after Accessibility ends)
- [ ] `speedup_factor` is between 0.9 and 1.1 (approximately no speedup)
- [ ] `tester_perf_result`, `tester_sec_result`, `tester_a11y_result`, `tester_vr_result` are all populated (same as parallel mode)
- [ ] Aggregated `tester_result` is computed identically to parallel mode
- [ ] Report content (issues, metrics) is identical regardless of execution mode

---

## Test Case 19: Visual Regression — Layout Shift Detection (Major)

**Scenario**: Developer changes a CSS file to adjust button padding, but the change inadvertently shifts the toolbar layout by 4 pixels.

**Project setup**: Baseline snapshot exists for `main-view/default`. CSS change in `styles.css` modifies `-fx-padding` on `.toolbar-button`, causing a 4px shift across the entire toolbar.

**Expected tester behavior**:
- Visual Regression Testing dimension: Visual Regression Testing
- Screenshot captured for `main-view/default` via TestFX + Monocle
- Pixel comparison against baseline: ~8% diff ratio (Major, 5-15% range)
- Diff image generated at `target/test-output/vr-diffs/main-view/default-diff.png`
- Root cause analysis: "Layout shift" (rectangular diff region)
- Issue generated with Fix Handoff targeting `styles.css`
- `tester_vr_result.snapshots_regressed` is 1

**Pass criteria**:
- [ ] Tester captures screenshot via TestFX in headless mode (Monocle)
- [ ] Diff ratio is calculated and recorded (expected 5-15% range)
- [ ] Issue severity is Major
- [ ] Diff image is generated and path is recorded in report
- [ ] Root cause analysis identifies "Layout shift" pattern
- [ ] Fix Handoff `target_file` is `styles.css`
- [ ] `visual_regression_results.baseline_mode` is `"compare"`
- [ ] `tester_vr_result` field is populated with snapshot metrics

---

## Test Case 20: Visual Regression — First Run Baseline Creation (Pass)

**Scenario**: Project has no existing baseline snapshots. Tester runs for the first time.

**Project setup**: No `src/test/resources/snapshots/` directory exists. TestFX + Monocle dependencies are present.

**Expected tester behavior**:
- Visual Regression Testing dimension
- Tester detects no baseline manifest exists
- Captures screenshots for all identified views/states
- Saves them as initial baselines in `src/test/resources/snapshots/`
- Creates `snapshot-manifest.json` with all snapshot metadata
- No regression issues reported (first run = baseline creation)
- Result: Pass with note "Baseline Created"
- `visual_regression_results.baseline_mode` is `"create"`
- `tester_vr_result.snapshots_baseline_created` > 0

**Pass criteria**:
- [ ] Tester detects absence of baseline snapshots
- [ ] Screenshots are captured and saved as PNG files
- [ ] `snapshot-manifest.json` is created with correct metadata
- [ ] No regression issues are reported
- [ ] `visual_regression_results.baseline_mode` is `"create"`
- [ ] Each `snapshot_results[].result` is `"Baseline Created"`
- [ ] `tester_vr_result.conclusion` is `"Pass"`
- [ ] Manifest `capture_config` records rendering mode, window size, etc.

---

## Test Case 21: Visual Regression — Baseline Update Mode (Pass)

**Scenario**: Developer intentionally changes the primary button color from blue to green. Runs tester with `vr_update_baselines: true` to update baselines.

**Project setup**: Baseline snapshots exist. CSS change in `styles.css` updates `-fx-background-color` on `.primary-button` from `#0078d4` to `#107c10`. `.loop-config.json` has `"vr_update_baselines": true`.

**Expected tester behavior**:
- Visual Regression Testing dimension
- Tester detects `vr_update_baselines` is enabled
- Captures new screenshots and overwrites existing baselines
- Updates manifest `last_updated` and `update_reason` fields
- No regression issues reported (update mode skips comparison)
- Result: Pass with note "Baseline Updated"
- `visual_regression_results.baseline_mode` is `"update"`

**Pass criteria**:
- [ ] Tester detects `vr_update_baselines: true` in config
- [ ] New screenshots overwrite existing baseline PNG files
- [ ] Manifest `last_updated` is updated to current date
- [ ] Manifest `update_reason` is recorded
- [ ] No regression issues are reported
- [ ] `visual_regression_results.baseline_mode` is `"update"`
- [ ] Each `snapshot_results[].result` is `"Baseline Updated"`
- [ ] `tester_vr_result.conclusion` is `"Pass"`

---

## Test Case 22: Visual Regression — TestFX/Monocle Not Available (Skipped)

**Scenario**: Project does not have TestFX or Monocle dependencies in `pom.xml`. Visual regression testing cannot run.

**Project setup**: `pom.xml` has no `testfx-core`, `testfx-junit5`, or `openjfx-monocle` dependencies. `.loop-config.json` has `"visual_regression": true` (default).

**Expected tester behavior**:
- Visual Regression Testing dimension
- Tester detects missing TestFX/Monocle dependencies
- Track D is skipped with `status: "skipped"`, `conclusion: "Skipped"`
- Error note: "TestFX/Monocle dependencies not found in pom.xml. Add testfx-core, testfx-junit5, and openjfx-monocle to enable visual regression testing."
- Other three tracks (Performance, Security, Accessibility) run normally
- Overall conclusion is `Conditional Pass` (one track skipped)
- `tester_vr_result.conclusion` is `"Skipped"`

**Pass criteria**:
- [ ] Tester detects missing TestFX dependency in `pom.xml`
- [ ] Track D `status` is `"skipped"` and `conclusion` is `"Skipped"`
- [ ] Error message is present and mentions missing dependencies
- [ ] Other three tracks are not affected (all `completed`)
- [ ] Aggregated `tester_result.conclusion` is `"Conditional Pass"`
- [ ] `parallel_execution.tracks.visual_regression.status` is `"skipped"`

---

## Test Case 23: Visual Regression — Anti-Flaky Pixel Threshold Verification

**Scenario**: Verify that the pixel tolerance and diff threshold correctly filter out rendering noise while catching real changes.

**Project setup**: Two test scenarios:
1. **Noise scenario**: Same code, re-captured screenshot — should Pass (diff < 2%)
2. **Real change scenario**: Font size changed from 14px to 16px — should report regression (diff > 2%)

**Test configuration**: `pixel_tolerance: 3` (per-channel RGB), `diff_threshold: 0.02` (2%)

**Expected tester behavior**:
- Noise scenario: Diff ratio < 1% (within tolerance, rendering noise) → Pass
- Real change scenario: Diff ratio > 5% (text size change affects rendering) → Major

**Pass criteria**:
- [ ] Noise scenario diff ratio is < 1% (tolerance absorbs rendering noise)
- [ ] Noise scenario result is `Pass` (no false positive)
- [ ] Real change scenario diff ratio is > 2% (exceeds threshold)
- [ ] Real change scenario result is `Major` (real regression detected)
- [ ] Pixel tolerance (3) is applied per-channel (R, G, B independently)
- [ ] Diff image for noise scenario is mostly original-color (few red pixels)
- [ ] Diff image for real change scenario shows red pixels in text areas
- [ ] `visual_regression_results.pixel_tolerance` is recorded as 3
- [ ] `visual_regression_results.diff_threshold` is recorded as 0.02
