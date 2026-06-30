# Evaluation Test Cases

This file defines the acceptance test cases for the JavaFX Developer skill, used to quantify skill output quality. Each case describes the input scenario, expected output, and verification standards.

---

## Test Case 1: Basic CRUD Table View

**Input**: "Help me create a user management interface, display the user list with TableView, supporting add, edit, and delete"

**Expected output**:
- Generate a `User` model class (with `StringProperty`/`LongProperty` and other JavaFX Properties)
- Generate a `UserController` implementing `Initializable`, containing `TableView` binding logic
- Generate an FXML layout file containing `TableView` + `TableColumn`
- Generate the corresponding CSS style file
- Provide Maven/Gradle dependency descriptions and run commands

**Verification standards**:
- [ ] The `TableView`'s `TableColumn` binds to the Model's Properties via `cellValueFactory`
- [ ] The FXML `fx:id` corresponds one-to-one with the Controller's `@FXML` fields
- [ ] `module-info.java` includes `opens model to javafx.controls` (to support PropertyValueFactory reflection)
- [ ] The code compiles without syntax errors
- [ ] CSS styles do not use the `var()` function

---

## Test Case 2: MVVM Architecture Application

**Input**: "Create a task management app using the MVVM pattern, the ViewModel needs to expose Properties for the View to bind to"

**Expected output**:
- Generate a `Task` model class and a `TaskViewModel` class
- The ViewModel exposes bindable properties such as `StringProperty`/`BooleanProperty`
- The View (FXML + Controller) connects to the ViewModel via bidirectional binding
- The Controller only handles UI events, business logic is delegated to the ViewModel/Service

**Verification standards**:
- [ ] The ViewModel does not directly reference UI controls (no `@FXML` injection)
- [ ] Bidirectional binding uses `bindBidirectional()`
- [ ] Computed properties use `Bindings.createXxxBinding()` (does not use the nonexistent `select()` API)
- [ ] The Service layer is injected into the ViewModel via constructor injection

---

## Test Case 3: Spring Boot + JavaFX Integration

**Input**: "Create an app using Spring Boot + JavaFX + MyBatis + SQLite"

**Expected output**:
- The startup class is split into `MyApp` (does not extend Application) + `JavaFXApp` (extends Application)
- Controller is annotated with `@Component` + `@Scope("prototype")`
- `application.yml` configures `web-application-type: none`
- MyBatis Mapper interface and XML mapping file
- `controllerFactory` is set to `springContext::getBean`

**Verification standards**:
- [ ] The main class does **not** extend `Application` (to avoid the "JavaFX runtime components are missing" error)
- [ ] Controller is annotated with `@Scope("prototype")` (to avoid singleton state pollution)
- [ ] JavaFX Properties' setters handle null (to avoid `SimpleLongProperty.set(null)` NPE)
- [ ] `spring.main.web-application-type` is set to `none` in `application.yml`
- [ ] `spring-boot-devtools` is not introduced (or set to optional + restart disabled)

---

## Test Case 4: Dialog and Form Validation

**Input**: "Create a user input dialog, containing name and email fields, the save button is disabled when the input is invalid"

**Expected output**:
- Dialog FXML layout (TextField + TextArea + Button)
- DialogController extends BaseController, handles OK/Cancel events
- Form validation uses `BooleanBinding` composition to implement declarative validation
- The save button's `disableProperty` is bound to the validation Binding

**Verification standards**:
- [ ] The dialog Controller can obtain user input via `getName()`/`getDescription()`
- [ ] Validation logic uses `Bindings.createBooleanBinding()` or `isEmpty().or()` composition
- [ ] Uses JavaFX native `Alert` or `Dialog` (does not use the deprecated ControlsFX `Dialogs.create()` API)
- [ ] Resources are correctly released after the dialog closes

---

## Test Case 5: Cross-Platform Packaging

**Input**: "Package my JavaFX app into a Windows exe installer"

**Expected output**:
- Provide a `jpackage` command containing `--type exe`, `--win-menu`, `--win-shortcut`
- Include the `--win-upgrade-uuid` parameter
- Include `--java-options "--enable-native-access=javafx.graphics"`
- Explain that Inno Setup (exe) or WiX Toolset 4.x (msi) needs to be installed
- Provide icon format requirements (`.ico`, multi-size embedded)

**Verification standards**:
- [ ] The command includes `--enable-native-access=javafx.graphics`
- [ ] The WiX version is documented as 4.x (installed via `dotnet tool install`), not 3.x
- [ ] Does not use `gu install native-image` (GraalVM JDK 21+ has it built-in)
- [ ] `--win-upgrade-uuid` uses a valid UUID format

---

## Test Case 6: CSS Theme Switching

**Input**: "Implement a light/dark theme switching feature"

**Expected output**:
- Two CSS files for light and dark, with theme variables defined in `.root`
- A ThemeManager class manages theme switching and preference persistence
- Color variables use direct references (`-fx-primary`), not `var()`
- Border radius uses literal numeric values, not size variables referenced via lookup

**Verification standards**:
- [ ] CSS does **not** use the `var()` function (JavaFX CSS does not support it)
- [ ] Color variables are defined in `.root`, child nodes reference them directly by name
- [ ] `-fx-border-radius`/`-fx-background-radius` use literal numeric values
- [ ] Theme switching is implemented via `scene.getStylesheets().setAll()`
- [ ] User preferences are persisted to `Preferences`

---

## Test Case 7: Data Binding and Memory Management

**Input**: "Implement a master-detail view, update the detail form when a list item is selected, be careful to prevent memory leaks"

**Expected output**:
- Use `FilteredList` + `SortedList` to handle list filtering and sorting
- A selection listener updates the detail view
- Remove listeners in a custom `dispose()` method (do not use the nonexistent `@FXML dispose()`)
- Trigger cleanup via `stage.setOnCloseRequest()` or view-switching callbacks

**Verification standards**:
- [ ] Does not use the nonexistent `person.select(p -> ...)` API
- [ ] Listeners are removed via `removeListener()` in a custom cleanup method
- [ ] Does not claim the existence of an `@FXML dispose()` lifecycle method
- [ ] Bindings returned by `Bindings.createXxxBinding()` are released when no longer needed
- [ ] Background tasks use `Task` + `Platform.runLater()` to return to the UI thread

---

## Test Case 8: Version Selection and Compatibility

**Input**: "I'm using JDK 17, which JavaFX version should I choose?"

**Expected output**:
- Recommend JavaFX 21 LTS (JDK 17+, mature and stable)
- Explain that JavaFX 25 LTS is the latest LTS (JDK 23+), can upgrade if the latest features are needed
- Explain that JavaFX 17 LTS ends support in Oct 2026
- Remind that JavaFX 24+ requires adding `--enable-native-access=javafx.graphics`

**Verification standards**:
- [ ] The version matrix is consistent with the official Gluon roadmap
- [ ] JavaFX 25 is marked as a released LTS (not "planned"/"expected")
- [ ] JavaFX 26 is marked as released (not "planned")
- [ ] JavaFX 17 LTS is marked as supported until Oct 2026
- [ ] Mentions that the `--enable-native-access` requirement applies to JavaFX 24+

---

## Test Case 9: Fix Consumption — Fingerprint Match (Positive)

**Input**: A fix handoff report from `javafx-code-reviewer` containing 3 fixes. The code at `target_lines` has not changed since the review, so `code_fingerprint` matches exactly.

**Expected output**:
- Enter Fix Consumption mode (Steps 1–5 skipped, Step 5.5 activated)
- Create `.fix-backup/{timestamp}/` directory with `manifest.json` before applying any fixes
- For each fix, compute SHA-256 of the code at `target_lines` (whitespace-normalized) and match against `code_fingerprint`
- All 3 fixes matched at Level 1 (fingerprint), applied at `target_lines` directly
- Post-fix `mvn compile -q` passes
- Fix Summary lists all 3 fixes with status `applied`

**Verification standards**:
- [ ] `.fix-backup/{timestamp}/manifest.json` exists and records all backed-up files, loop ID, and round number
- [ ] All 3 fixes have status `applied` (not `relocated` or `skipped`)
- [ ] Fixes are sorted by `fix_priority` ascending in the Fix Summary
- [ ] `mvn compile -q` exit code is 0 after applying fixes
- [ ] `.loop-state.json` is updated with `fixes_applied: 3`, `fixes_skipped: 0`, `fixes_rolled_back: 0`

---

## Test Case 10: Fix Consumption — Line Drift Recovery via Anchor Matching (Boundary)

**Input**: A fix handoff report with 2 fixes. Fix #1 (insert at line 30) was already applied in a prior batch, shifting Fix #2's `target_lines` (originally "45-60") down by 5 lines. The `code_fingerprint` for Fix #2 no longer matches, but `anchor_pattern` (2 lines before + 2 lines after) still matches at exactly one location.

**Expected output**:
- Fix #1 applied at Level 1 (fingerprint match)
- Fix #2: fingerprint mismatch detected (line drift), fall back to Level 2 (anchor-based matching)
- Anchor pattern matches at exactly one location in the file
- Fix #2 applied at the relocated position, status recorded as `applied (relocated by anchor)`
- Fix Summary flags `line_drift: true` for Fix #2

**Verification standards**:
- [ ] Fix #1 status is `applied`, Fix #2 status is `applied (relocated by anchor)`
- [ ] The Fix Summary includes a `line_drift` flag for Fix #2
- [ ] Fixes within the same file group are applied serially with `target_lines` start line **descending** (highest line first) to minimize drift
- [ ] Post-fix compilation passes
- [ ] The relocated position is correct (the fix is applied at the actual code location, not the stale line number)

---

## Test Case 11: Fix Consumption — AST Signature Relocation (Boundary)

**Input**: A fix handoff report where the target method has been moved to a different class and renamed since the review. `code_fingerprint`, `anchor_pattern`, and content-based matching all fail. The `ast_node_signature` is `com.example.controller.UserController#handleSave(ActionEvent)`, and the method now lives at `com.example.controller.UserController#onSave(ActionEvent)`.

**Expected output**:
- Levels 1–3 (fingerprint, anchor, content) all fail
- Fall back to Level 4 (AST signature matching): search for the method by signature
- If the method is found by partial signature match (same class, method with matching param types), apply the fix there
- Status recorded as `applied (relocated by AST)`
- If the method cannot be found at all, status is `skipped (AST signature not found)`

**Verification standards**:
- [ ] The 4-level matching hierarchy is attempted in order: fingerprint → anchor → content → AST signature
- [ ] If AST match succeeds, the fix is applied at the correct method location
- [ ] The `ast_node_signature` field is used for signature search, not line numbers
- [ ] For non-Java files (FXML, CSS, `module-info.java`), `ast_node_signature` is `null` and Level 4 is skipped
- [ ] If all 4 levels fail, status is `skipped (line drift)` with a clear message in the Fix Summary

---

## Test Case 12: Fix Consumption — Parallel File Groups (Positive)

**Input**: A fix handoff report with 6 fixes targeting 4 different files: `UserController.java` (2 fixes), `UserService.java` (1 fix), `main-view.fxml` (1 fix), `styles.css` (2 fixes). No two file groups share the same file.

**Expected output**:
- Fixes are grouped by `target_file` into 4 parallel file groups
- Each group applies its fixes serially (within the group), sorted by `target_lines` start line descending
- Groups execute concurrently (parallel file groups)
- Thread-safe result buffer collects status from all groups
- Cross-impact checks (Controller-FXML, module-info, CSS references, FXML-Controller binding) run in parallel after all groups complete
- All 6 fixes applied, post-fix compilation passes

**Verification standards**:
- [ ] Fixes in the same file are in the same group and applied serially (no concurrent writes to the same file)
- [ ] Fixes in different files are in different groups and may execute concurrently
- [ ] Within each group, fixes are sorted by `target_lines` start line descending (highest first)
- [ ] All 4 cross-impact checks pass before compile verification
- [ ] Fix Summary merges results from all parallel groups, sorted by `fix_priority` across all groups

---

## Test Case 13: Fix Consumption — Compile Failure and Automatic Rollback (Negative)

**Input**: A fix handoff report with 3 fixes. Fix #2 introduces a syntax error (incorrect `corrected_example`). Fixes #1 and #3 are valid. After applying all 3 fixes, `mvn compile -q` fails.

**Expected output**:
- All 3 fixes applied to the working tree
- `mvn compile -q` fails (compilation error)
- Automatic rollback triggered: all modified files restored from `.fix-backup/{timestamp}/`
- All 3 fixes marked as `rolled_back` (not just Fix #2 — the entire batch is rolled back)
- `rollback_event` appended to `.loop-state.json` with timestamp, backup dir, files restored, compile error count, `rollback_verification: "passed"`
- Rollback verification: re-run `mvn compile -q` to confirm project compiles again
- Fix Summary recommends manual intervention

**Verification standards**:
- [ ] All 3 fixes have status `rolled_back` (batch-level rollback, not individual)
- [ ] `.fix-backup/{timestamp}/` files are restored to their original locations
- [ ] `rollback_event` in `.loop-state.json` contains: `round`, `timestamp`, `backup_dir`, `files_restored`, `compile_error_count`, `rollback_verification`
- [ ] Post-rollback `mvn compile -q` passes (project is back to pre-fix state)
- [ ] Fix Summary recommends "manual intervention required — fixes caused compilation failure"
- [ ] `.fix-backup/` is NOT cleaned up (preserved for manual inspection on rollback)

---

## Test Case 14: Fix Consumption — Multi-Source Handoff Merge (Positive)

**Input**: A merged fix handoff batch from the orchestrator containing fixes from both `javafx-code-reviewer` (4 fixes) and `javafx-runner` (2 fixes). Two fixes target the same file with overlapping line ranges: reviewer Fix A (lines 45-60, Critical) and runner Fix B (lines 50-55, Major). The orchestrator has already deduplicated these (kept Fix A, the higher severity), recording `dedup_merged_from: "runner"` on Fix A.

**Expected output**:
- Developer receives the already-merged and deduplicated batch (5 fixes total, not 6)
- Fix A has `dedup_merged_from: "runner"` field for traceability
- All 5 fixes applied using the standard 4-level matching hierarchy
- Fix Summary lists all 5 fixes, with Fix A noting it superseded a runner fix

**Verification standards**:
- [ ] The developer does NOT re-deduplicate (the orchestrator already did this) — it consumes the merged batch as-is
- [ ] Fix A's `dedup_merged_from` field is preserved in the Fix Summary for traceability
- [ ] Fixes from both sources are sorted by `fix_priority` across the entire merged batch
- [ ] The `source` field (`reviewer` or `runner`) is preserved per fix in the Fix Summary
- [ ] Total fixes applied = 5 (not 6), confirming dedup was respected

---

## Test Case 15: Fix Consumption — Architecture Handoff Consumption (Positive)

**Input**: User requests "design the architecture and generate code for a user management app". The orchestrator runs `javafx-architect` first, producing `architecture-handoff.json` with `system_design.architecture_pattern: "MVVM + Service Layer"`, `database_schema` with 3 tables, and `developer_instructions` specifying `@req` annotation format. Then `javafx-developer` is triggered.

**Expected output**:
- Developer Step 1 checks for `architecture-handoff.json` and consumes it
- The generated project follows the MVVM + Service Layer pattern from the handoff
- Database integration follows the `database_schema` (table names, column types, indexes match)
- `@req` annotations in generated code follow the `req_id_convention` from the handoff
- `requirements.md` in `docs/` reflects the architecture handoff's module structure

**Verification standards**:
- [ ] Generated code uses MVVM pattern (ViewModel with no `@FXML` references, Controller delegates to ViewModel)
- [ ] Database entity classes match the `database_schema` table definitions (field names, types)
- [ ] `@req FR-xxx` annotations match the `req_id_convention` from the handoff
- [ ] If `database_schema` is absent (project has `database: "none"`), no database code is generated
- [ ] The `developer_instructions` (annotation format, test naming convention) are followed

---

## Test Case 16: Fix Consumption — Constraint Violation (Negative)

**Input**: A fix handoff report where one fix has `fix_type: "replace"` but provides no `corrected_example` field. Another fix targets a file that does not exist in the project (`target_file: "src/main/java/com/example/NonExistent.java"`).

**Expected output**:
- Fix without `corrected_example` (for `fix_type: "replace"`): status `skipped (missing corrected_example)`, included in Fix Summary as a warning
- Fix targeting non-existent file: status `skipped (target file not found)`, included in Fix Summary
- Other valid fixes are applied normally
- Fix Summary clearly distinguishes `applied`, `skipped`, and `failed` statuses
- Post-fix compilation passes (only valid fixes were applied)

**Verification standards**:
- [ ] `fix_type: "replace"` without `corrected_example` is skipped, not applied with empty content
- [ ] `fix_type: "delete"` without `corrected_example` is valid (deletion needs no replacement) and should be applied
- [ ] Fix targeting non-existent file has status `skipped (target file not found)`
- [ ] Valid fixes in the same batch are still applied (one bad fix does not block the batch)
- [ ] Fix Summary includes a `warnings` section listing all skipped fixes with reasons
- [ ] `.loop-state.json` records `fixes_applied`, `fixes_skipped` counts accurately

---

## Test Case 17: Design Handoff Consumption

**Input**: User requests "design the UI and generate code for a user management app". The orchestrator runs `javafx-designer` first, producing `design/design-handoff.json` with FXML prototypes in `design/fxml/*.fxml`, a CSS theme in `design/css/light-theme.css` + `dark-theme.css`, an icon config in `design/icons/icon-config.json`, and an interaction flow in `design/flow/interaction-flow.mmd`. Then `javafx-developer` is triggered.

**Expected output**:
- Developer Step 4 detects `design/design-handoff.json` and consumes the design handoff instead of using built-in templates
- FXML prototypes from `design/fxml/*.fxml` are used as the base layout (not the built-in `templates/fxml/*.fxml`)
- CSS theme files (`light-theme.css`, `dark-theme.css`) are copied to `src/main/resources/css/`
- If `icon-config.json` is present, the Ikonli Maven dependency is added to `pom.xml` and FontIcon usage is configured in FXML and Java code
- `fx:id` names declared in the designer's FXML match the `@FXML` fields in the generated controller exactly

**Verification standards**:
- [ ] `design-handoff.json` parsed — the developer reads and consumes the handoff artifacts
- [ ] FXML prototype used as base (not built-in template) — the generated FXML derives from `design/fxml/*.fxml`, preserving layout containers, `fx:id` assignments, and `styleClass` values
- [ ] CSS theme files copied to `src/main/resources/css/` — the designer's `light-theme.css` and `dark-theme.css` replace the built-in CSS templates
- [ ] Ikonli dependency added to `pom.xml` if `icon-config.json` is present (e.g., `org.kordamp.ikonli:ikonli-javafx`)
- [ ] `fx:id` names from the FXML match `@FXML` fields in the generated controller exactly
- [ ] `developer_instructions.fx_id_consistency` enforced — any fx:id consistency rules from the handoff are respected

---

## Test Case 18: Requirements Handoff Consumption

**Input**: User requests "create a JavaFX app based on these requirements". The orchestrator runs `javafx-requirements` first, producing `requirements/requirements-handoff.json` with `user_stories[]`, `non_functional_requirements[]`, a `traceability_matrix[]` seed, and `developer_instructions` containing `req_id_convention` and `test_naming_convention`. Then `javafx-developer` is triggered.

**Expected output**:
- Developer Step 1 detects `requirements/requirements-handoff.json` and consumes it as the primary requirements source (instead of inferring requirements from the user request)
- Each user story (US-xxx) is mapped to a functional requirement (FR-xxx) in `requirements.md`
- NFRs are applied as generation constraints (e.g., a performance NFR forces background-thread execution for DB I/O)
- The Requirement Traceability Matrix in `requirements.md` Section 7 is pre-filled from the handoff's `traceability_matrix[]` seed
- `@req` annotations are added to the Javadoc of generated public methods, following the `req_id_convention`

**Verification standards**:
- [ ] `requirements-handoff.json` parsed — `user_stories[]`, `non_functional_requirements[]`, `traceability_matrix[]`, and `developer_instructions` are read
- [ ] `user_stories` mapped to functional requirements — each US-xxx becomes an FR-xxx entry in `requirements.md`
- [ ] NFRs applied as generation constraints (e.g., performance NFR → DB access wrapped in `Task`/background thread; security NFR → parameterized queries)
- [ ] RTM seed pre-filled in `requirements.md` (Section 7) from the handoff's `traceability_matrix[]`
- [ ] `@req` annotations added to public methods in generated code (format follows `req_id_convention`, e.g., `@req FR-001`)
- [ ] Test methods follow the `test{Behavior}_{REQ_ID}()` naming convention (e.g., `testUserCreation_FR_001()`)
