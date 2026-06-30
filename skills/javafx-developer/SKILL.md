---
name: javafx-developer
description: |
  JavaFX desktop application development expertise covering project setup, FXML UI design,
  MVC/MVVM architecture, data binding, CSS theming, and cross-platform packaging.
  Invoke when: building JavaFX apps, creating FXML layouts, designing desktop UIs,
  implementing data binding, integrating ControlsFX/RichTextFX, or packaging JavaFX apps.
license: Apache-2.0
compatibility: Requires JDK 17+. Supports JavaFX 17/21/24/25/26.
metadata:
  author: DongZY0617
  version: "1.1"
triggers:
  - create
  - generate
  - build
  - scaffold
  - implement
  - JavaFX app
  - fix
  - apply refactoring
depends_on:
  - javafx-architect (optional)
  - javafx-designer (optional)
  - javafx-code-reviewer
  - javafx-runner
  - javafx-refactorer (optional)
consumes_from:
  - javafx-architect (optional, architecture-handoff.json)
  - javafx-designer (optional, design-handoff.json)
  - javafx-code-reviewer (fix handoff report)
  - javafx-runner (fix handoff report)
  - javafx-tester (fix handoff report)
  - javafx-refactorer (optional, refactor-handoff.json)
produces_for:
  - javafx-code-reviewer
  - javafx-runner
  - javafx-tester
  - javafx-docgen
---

# JavaFX Developer

You are an expert JavaFX desktop application developer. This skill provides comprehensive guidance for building JavaFX applications — from project scaffolding to cross-platform packaging.

## When to Apply

Use this skill when:
- User wants to create a JavaFX desktop application
- User mentions FXML, Scene Builder, JavaFX CSS, or desktop UI design
- User asks about MVC/MVVM architecture for JavaFX
- User needs data binding, Properties, ObservableList patterns
- User wants to integrate third-party libraries (ControlsFX, MaterialFX, RichTextFX, Ikonli)
- User needs to package or deploy a JavaFX application (jpackage, jlink)
- User asks about JavaFX version selection or JDK compatibility
- User mentions JavaFX controls, tables, forms, dialogs, or navigation
- User provides a fix handoff report from `javafx-code-reviewer` or `javafx-runner` (enters Fix Consumption mode, see Step 5.5 and Fix Consumption Protocol below)

### Trigger Resolution with Other Skills

Developer's triggers (`create`, `generate`, `build`, `scaffold`, `implement`) are broad by design — developer is the primary code-generation skill. However, ambiguity can arise with other skills:

| User intent | Routed to | Rationale |
|-------------|-----------|-----------|
| "create a JavaFX app" | **developer** | Code generation intent |
| "generate code for..." | **developer** | Explicit code generation |
| "build a JavaFX app" | **developer** | Scaffolding intent (not compilation — compilation is `javafx-runner`) |
| "design the UI and generate code" | **designer → developer** (sequential) | Design first, then generate from design handoff |
| "review the code" | **code-reviewer** | Review intent, not generation |
| "verify it compiles" | **runner** | Verification intent |
| "refactor this class" | **refactorer** | Refactoring analysis intent |
| Ambiguous ("fix this app") | **Confirm with user** | Could mean code review, runtime fix, or refactoring |

**Special case — `build`**: When user says "build the project", this means **compilation/packaging verification** → route to `javafx-runner`. When user says "build a JavaFX app", this means **scaffolding/generation** → route to `javafx-developer`. The presence of "a/an" before "JavaFX app" distinguishes generation from compilation.

## Technology Stack

### JavaFX Version Matrix (as of 2026)

| JavaFX Version | Min JDK | LTS | Recommended Use |
|----------------|---------|-----|-----------------|
| 26.x | JDK 24 | No | Latest features (Metal renderer, Headless preview) |
| 25.x | JDK 23 | **Yes** | Production preferred choice |
| 24.x | JDK 22 | No | Transitional version (introduces `--enable-native-access` requirement) |
| 21.x | JDK 17 | **Yes** | Conservative stable option |
| 17.x | JDK 11 | Yes (until 2026.10) | Legacy maintenance (expiring soon, not recommended for new projects) |

> **Note**: The "Min JDK" in this table refers to the minimum JDK required to run that JavaFX version; the "Corresponding JDK" in the version table of `project-setup.md` refers to the `--release` level used when compiling that JavaFX. The two have different bases but the data is consistent.

**Default recommendation**: New projects prefer JavaFX 25 LTS (JDK 23+), conservative choice JavaFX 21 LTS (JDK 17+), unless user requests otherwise.

### Build Tools
- **Maven** (default): `javafx-maven-plugin` 0.0.8
- **Gradle**: `org.openjfx.javafxplugin` 0.1.0

### Core Modules
- `javafx.controls` — UI controls (always needed)
- `javafx.fxml` — FXML support (if using FXML)
- `javafx.web` — WebView component
- `javafx.media` — Audio/video playback
- `javafx.swing` — Swing interop
- `javafx.graphics` — Auto-included with controls

## Workflow

### Step 1: Requirements Analysis & Clarification

1. **Check for requirements handoff**: If `requirements/requirements-handoff.json` exists (produced by `javafx-requirements`), consume it as the primary requirements source instead of inferring requirements from scratch:
   - Use `user_stories[]` as the basis for the feature list in `requirements.md` — each user story (US-xxx → FR-xxx) becomes a functional requirement entry
   - Use `non_functional_requirements[]` to populate the non-functional requirements section — each NFR is already quantified with targets and verification methods
   - Use `traceability_matrix[]` as the seed for the Requirement Traceability Matrix (Section 7 of `requirements.md`) — pre-populated with requirement IDs, source stakeholders, and verification methods
   - Use `developer_instructions.req_id_convention` for `@req` annotation format (FR-xxx, NFR-xxx)
   - Use `developer_instructions.test_naming_convention` for test method naming (e.g., `testUserCreation_FR_001()`)
   - Use `developer_instructions.key_constraints[]` to ensure requirements constraints are respected during code generation
   - If no handoff exists, proceed with requirement inference from the user request (existing behavior — Steps 2-5 below)
2. **Identify intent**: Determine which capability module(s) the request falls into
3. **Extract key info**: Project name, package path, functionality, UI type, data model
4. **Ask for missing info**: If critical info is missing, ask the user
5. **Infer defaults**: Use reasonable defaults for package names, class names, module names
6. **Output requirements spec**: Generate a `requirements.md` file using `templates/docs/requirements.md` as the template. Document functional requirements (feature list, UI flows), non-functional requirements (performance, security, compatibility), and technical constraints (JavaFX version, JDK, build tool, architecture pattern). This serves as the baseline for incremental review and requirement traceability — reviewer can reference requirement IDs in review reports. When consuming a requirements handoff, the spec is enriched with stakeholder intent, acceptance criteria, and pre-seeded RTM

### Step 2: Version & Toolchain Selection

1. **Detect JDK version**: Ask user or infer from project context
2. **Recommend JavaFX version**: Prefer LTS versions (25 or 21)
3. **Choose build tool**: Default Maven, follow user preference if specified
4. **Determine module needs**: Based on functionality, identify required JavaFX modules

**Version selection logic**:
- JDK 24+ → JavaFX 26 (if user wants latest) or 25 LTS
- JDK 17-23 → JavaFX 21 LTS
- JDK 11-16 → JavaFX 17 LTS

**Important**: JavaFX 24+ requires `--enable-native-access=javafx.graphics` JVM argument.

### Step 3: Architecture Design

1. **Evaluate complexity**: Recommend MVC for simple apps, MVVM for complex ones, MVP for high testability
2. **Design layering**: Define Model, View, Controller/ViewModel/Presenter responsibilities
3. **Plan file structure**: Organize classes, FXML, CSS files
4. **Select UI patterns**: Choose from preset UI component patterns

**MVC / MVVM / MVP decision**:
- **MVC**: Simple CRUD apps, admin panels, small tools → Controller directly manipulates UI
- **MVVM**: Complex business logic, multi-view apps → ViewModel exposes Properties, View binds to them
- **MVP**: High testability needed but data binding undesirable → Presenter explicitly controls UI via a View interface, Controller implements the interface and delegates logic to Presenter

### Step 4: Code Generation & Template Filling

1. **Check for architecture handoff**: Before loading templates, check if `architecture/architecture-handoff.json` exists in the project root. If it does, `javafx-architect` has produced architecture specs that should guide code generation:
   - **Package structure**: Use the `developer_instructions.package_structure` pattern (e.g., `com.example.app.{module}`) instead of the default package naming
   - **Layering rule**: Follow the `developer_instructions.layering_rule` — place classes in the correct layer (Presentation/Application/Domain/Infrastructure) and enforce no upward dependencies
   - **Naming convention**: Apply the `developer_instructions.naming_convention` patterns (e.g., `{Entity}Controller`, `{Entity}ViewModel`, `{Entity}Service`, `{Entity}Repository`) instead of built-in defaults
   - **Technology stack**: Use the versions and libraries specified in `system_design.technology_stack` (database, ORM, DI, logging) when generating `pom.xml` dependencies
   - **Module decomposition**: Generate code organized by the modules defined in `system_design.modules[]` — each module gets its own package with the specified responsibility
   - **Key constraints**: Enforce all `developer_instructions.key_constraints` (e.g., "All database access through Repository interfaces", "ViewModels must not reference JavaFX controls directly")
   - **UML reference**: Read `architecture/uml/class-diagram.puml` to understand the domain model and class relationships before generating entity and service classes
   - **Database schema**: Consume `database_schema` to generate JPA entity classes, Repository interfaces, and Flyway/Liquibase migration files. Entity field names, types, and indexes must match the schema table definitions, and migration scripts must reflect the schema's DDL (see `database-design.md` §6.2 handoff protocol)
   - **Threat model**: Consume `threat_model` to implement security mitigations in code — e.g., input validation on fields flagged in the threat model, permission/authorization checks for sensitive operations, parameterized queries to prevent SQL injection, and secure handling of secrets identified as assets (see `threat-modeling.md` handoff protocol)
   - If `architecture-handoff.json` does not exist, proceed with built-in defaults (default behavior)
2. **Check for design handoff**: Before loading templates, check if `design/design-handoff.json` exists in the project root. If it does, `javafx-designer` has produced design artifacts that should be used instead of built-in templates:
   - **FXML templates**: Read FXML prototypes from `design/fxml/*.fxml` instead of `templates/fxml/*.fxml`. These prototypes already have proper layout containers, fx:id assignments, and styleClass values
   - **CSS themes**: Copy `design/css/light-theme.css` and `design/css/dark-theme.css` to `src/main/resources/css/` instead of using built-in CSS templates. The designer's themes include complete CSS variable systems and component styles
   - **Icon config**: Read `design/icons/icon-config.json` and add the Ikonli Maven dependencies to `pom.xml`. Configure FontIcon usage in FXML and Java code based on the icon mapping
   - **Interaction flow**: Read `design/flow/interaction-flow.mmd` to understand screen transitions and implement navigation logic accordingly
   - If `design-handoff.json` does not exist, proceed with built-in templates (default behavior)
3. **Load templates**: Read from `templates/` directory (or `design/` directory if designer handoff exists)
4. **Variable replacement**: Replace placeholders with actual values
5. **Logic filling**: Add business logic code based on user requirements
6. **Style generation**: Generate corresponding CSS files (or use designer's CSS if available)
7. **Resource handling**: Handle icons, images, and static resource references (use designer's icon config if available)
8. **Requirement traceability annotation**: Annotate every generated Java source file with `@req` tags in the Javadoc class header, linking each file to the requirement ID(s) it implements. Format: `@req FR-001` (single) or `@req FR-001, FR-002` (multiple). Requirement IDs are sourced from the `requirements.md` specification (Section 3.1 Feature List). If a file implements infrastructure or utility functionality not tied to a specific functional requirement, annotate it with `@req INFRA` (infrastructure). This enables the reviewer's Requirements Coverage dimension to detect orphan code and unimplemented requirements
   ```java
   /**
    * User management controller.
    *
    * @req FR-001, FR-002
    * @author javafx-developer
    */
   public class UserController { ... }
   ```
9. **Test scaffolding** (default): Generate test skeletons for each Controller and ViewModel using TestFX templates (`templates/test/`). This is now the default behavior — no user request needed. Generate:
   - `MainWindowTest.java` — Verifies main window initialization and FXML loading
   - `ControllerTest.java` — Verifies controller actions and event handlers
   - `ViewModelTest.java` — Verifies ViewModel property bindings and state transitions (MVVM only)
   - `CRUDViewTest.java` — Verifies CRUD operations if TableView is present
   - Each test method must reference the requirement ID it validates via the naming convention `test{Behavior}_{REQ_ID}()` — e.g., `testUserCreation_FR_001()`, and include `@req FR-001` in the test method's Javadoc. This enables the reviewer to verify that every requirement has test coverage
   - Add JaCoCo plugin to `pom.xml` for coverage measurement (see `javafx-runner`'s `references/test-coverage-gate.md` for threshold details)
10. **Update RTM**: After all files are generated, update the Requirement Traceability Matrix in `requirements.md` (Section 7) — fill in the implementation file paths, test case references, and coverage summary. Set status to `Implemented` for code-only items and `Tested` for items with test scaffolding

### Step 5: Quality Check

1. **Syntax check**: Verify Java/XML/CSS syntax correctness
2. **Naming convention**: Validate class/method/variable names follow Java conventions
3. **Module check**: Verify `module-info.java` exports/requires completeness
4. **Security review**: Check for SQL injection, path traversal, hardcoded credentials
5. **Best practices**: Verify adherence to JavaFX official recommended patterns

### Step 5.5: Fix Consumption (When Input Is a Fix Handoff Report)

> This step activates when the input is a fix handoff report from `javafx-code-reviewer` or `javafx-runner`, identified by the presence of `fix_handoff` fields (`target_file`, `target_lines`, `fix_type`, `fix_priority`, `code_fingerprint`, `anchor_pattern`, `ast_node_signature`). In this mode, Steps 1–5 are skipped.

1. **State recovery**: Check for `.loop-state.json` in the project root directory. If found, load the loop state (current round, issue history, convergence trend) and resume from the last checkpoint. If not found, initialize a new loop state as Round 1
2. **Parse report & concurrent grouping**: Extract all fix handoff entries, sort by `fix_priority` ascending (1 = highest). Group fixes by `target_file` into concurrent groups (Step 1a) — different file groups can execute in parallel (up to 4 concurrent), same-file fixes execute serially in reverse line order
3. **Pre-fix backup**: Create `.fix-backup/{timestamp}/` directory and copy all target files before applying any fixes. Write a `manifest.json` recording backed-up files, loop ID, and round number (see Fix Consumption Protocol Step 1.5). Backup is created before parallel execution begins
4. **Locate & apply fix** (concurrent across file groups): Launch parallel file groups — each group applies its fixes serially using the 4-level location matching hierarchy: fingerprint → anchor → content → AST signature (see Fix Consumption Protocol Steps 2–3). Thread-safe result buffer collects status from all groups
5. **Cross-impact check** (parallel): Run 4 independent checks concurrently — Controller-FXML consistency (A), module-info completeness (B), CSS reference resolution (C), FXML-Controller binding (D). All must pass before compile verification (see Fix Consumption Protocol Step 4)
6. **Post-fix compile verification & rollback**: Run `mvn compile -q` to verify fixes did not break compilation. If compilation fails, automatically restore all modified files from `.fix-backup/{timestamp}/`, mark all fixes as `rolled_back`, and record a rollback event in `.loop-state.json` (see Fix Consumption Protocol Step 4.5)
7. **Output fix summary** (thread-safe aggregation): Merge results from all parallel groups, sort by `fix_priority`, list all applied fixes with status (applied/skipped/failed/rolled_back), flag line drift, recommend next step (re-review or re-verify)
8. **Update RTM**: If a fix modifies, adds, or removes a file that has `@req` annotations, update the Requirement Traceability Matrix in `requirements.md` (Section 7) to reflect the change. If a fix introduces new functionality not in the original requirements, add a new requirement ID and annotate the code accordingly
9. **State persistence**: After completing fixes, update `.loop-state.json` with the current round number, applied fixes, issue counts, convergence trend, and rollback events. This enables cross-session loop recovery

See **Fix Consumption Protocol** section below for full specification.

### Step 6: Delivery & Documentation

1. **File manifest**: List all generated files with paths
2. **Dependencies**: Inform user of required Maven/Gradle dependencies
3. **Run instructions**: Provide compile and run commands
4. **Next steps**: Suggest feature extensions, testing, packaging

## Architecture Patterns

### MVC Pattern (Simple Apps)

```
src/main/java/com/example/
├── App.java                    # Entry point
├── model/
│   └── User.java               # Data model with Properties
├── controller/
│   └── UserController.java     # UI logic + event handling
└── service/
    └── UserService.java        # Business logic

src/main/resources/
├── fxml/
│   └── user-view.fxml          # Layout
└── css/
    └── style.css               # Styles
```

### MVVM Pattern (Complex Apps)

```
src/main/java/com/example/
├── App.java
├── model/
│   └── User.java               # Pure data model
├── viewmodel/
│   └── UserViewModel.java      # Exposes Properties, commands
├── view/
│   └── UserController.java     # Binds UI to ViewModel
└── service/
    └── UserService.java

src/main/resources/
├── fxml/
│   └── user-view.fxml
└── css/
    └── style.css
```

### MVP Pattern (High Testability Apps)

```
src/main/java/com/example/
├── App.java
├── model/
│   └── User.java               # Pure data model
├── view/
│   └── UserView.java           # View interface (abstracts UI operations)
├── presenter/
│   └── UserPresenter.java      # Holds View interface ref, no JavaFX dependency
├── controller/
│   └── UserController.java     # Implements View interface, delegates to Presenter
└── service/
    └── UserService.java

src/main/resources/
├── fxml/
│   └── user-view.fxml
└── css/
    └── style.css
```

## Code Examples

### Application Entry Point

```java
package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
        Scene scene = new Scene(loader.load(), 1024, 768);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        stage.setTitle("My JavaFX App");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

### Model with Properties

```java
package com.example.model;

import javafx.beans.property.*;

public class User {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final BooleanProperty active = new SimpleBooleanProperty(true);

    // ID
    public long getId() { return id.get(); }
    public void setId(long value) { id.set(value); }
    public LongProperty idProperty() { return id; }

    // Name
    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }
    public StringProperty nameProperty() { return name; }

    // Email
    public String getEmail() { return email.get(); }
    public void setEmail(String value) { email.set(value); }
    public StringProperty emailProperty() { return email; }

    // Active
    public boolean isActive() { return active.get(); }
    public void setActive(boolean value) { active.set(value); }
    public BooleanProperty activeProperty() { return active; }
}
```

### Controller with Data Binding

```java
package com.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import com.example.model.User;
import java.net.URL;
import java.util.ResourceBundle;

public class UserController implements Initializable {
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private CheckBox activeCheckBox;
    @FXML private Label statusLabel;
    @FXML private Button saveButton;

    private final User model = new User();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Bidirectional binding
        nameField.textProperty().bindBidirectional(model.nameProperty());
        emailField.textProperty().bindBidirectional(model.emailProperty());
        activeCheckBox.selectedProperty().bindBidirectional(model.activeProperty());

        // Computed binding
        statusLabel.textProperty().bind(
            model.nameProperty().concat(" - ").concat(model.emailProperty())
        );

        // Validation
        saveButton.disableProperty().bind(
            model.nameProperty().isEmpty().or(model.emailProperty().isEmpty())
        );
    }

    @FXML
    private void handleSave() {
        // Delegate to service layer
        System.out.println("Saving user: " + model.getName());
    }
}
```

### FXML Layout

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.layout.VBox?>
<?import javafx.scene.layout.HBox?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.TextField?>
<?import javafx.scene.control.CheckBox?>
<?import javafx.scene.control.Button?>
<?import javafx.scene.layout.GridPane?>

<VBox xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.example.controller.UserController"
      spacing="15" styleClass="form-container">

    <GridPane hgap="10" vgap="10">
        <Label text="Name:" GridPane.rowIndex="0" GridPane.columnIndex="0"/>
        <TextField fx:id="nameField" GridPane.rowIndex="0" GridPane.columnIndex="1"/>

        <Label text="Email:" GridPane.rowIndex="1" GridPane.columnIndex="0"/>
        <TextField fx:id="emailField" GridPane.rowIndex="1" GridPane.columnIndex="1"/>

        <Label text="Active:" GridPane.rowIndex="2" GridPane.columnIndex="0"/>
        <CheckBox fx:id="activeCheckBox" GridPane.rowIndex="2" GridPane.columnIndex="1"/>
    </GridPane>

    <HBox spacing="10">
        <Button fx:id="saveButton" text="Save" onAction="#handleSave" styleClass="button-primary"/>
        <Label fx:id="statusLabel" styleClass="status-label"/>
    </HBox>
</VBox>
```

### CSS Styling

```css
.root {
    -fx-primary: #3b82f6;
    -fx-bg-base: #ffffff;
    -fx-text-primary: #0f172a;
    -fx-border: #e2e8f0;
}

.form-container {
    -fx-padding: 20;
    -fx-background-color: -fx-bg-base;
}

.button-primary {
    -fx-background-color: -fx-primary;
    -fx-text-fill: white;
    -fx-padding: 8 16;
    -fx-background-radius: 6;
}

.button-primary:hover {
    -fx-background-color: derive(-fx-primary, -10%);
}
```

## Common UI Patterns

### CRUD Table View
- `TableView` with `TableColumn` bound to Properties
- `FilteredList` + `SortedList` for filtering/sorting
- Pagination via `Pagination` control
- Context menu for row actions (edit, delete)

### Login Dialog
- `Dialog<User>` with custom `DialogPane`
- Form validation with immediate feedback
- "Remember me" with Preferences API
- Loading state during authentication

### Master-Detail View
- Split pane with master list + detail form
- Selection listener updates detail view
- Auto-save with debounce timer
- Unsaved changes warning

### Navigation Drawer
- `BorderPane` with collapsible side panel
- `ListView` or custom menu items
- View switching via `FXMLLoader` loading into content area
- Breadcrumb tracking

## Third-Party Library Integration

| Library | Purpose | Maven Coordinate |
|---------|---------|-----------------|
| ControlsFX | Dialogs, notifications, validation | `org.controlsfx:controlsfx:11.2.1` |
| MaterialFX | Material Design controls | `io.github.palexdev:materialfx:11.17.0` |
| RichTextFX | Rich text editor, code highlighting | `org.fxmisc.richtext:richtextfx:0.11.5` |
| Ikonli | Font icons (FontAwesome, Material) | `org.kordamp.ikonli:ikonli-javafx:12.3.1` |
| ValidatorFX | Form validation framework | `net.synedra:validatorfx:0.4.0` |
| TestFX | UI automated testing | `org.testfx:testfx-junit5:4.0.18` |

For detailed integration guides, see `references/third-party-libraries.md`.

## Network Integration (Retrofit)

For JavaFX + Spring Boot applications that communicate with a backend REST API, use `retrofit-spring-boot-starter` for declarative HTTP clients.

| Concern | Pattern |
|---------|---------|
| API definition | `@RetrofitClient` annotated interfaces with `@GET`/`@POST`/`@PUT`/`@DELETE` |
| HTTP client | Custom OkHttpClient registered via `SourceOkHttpClientRegistrar` with interceptors and timeouts |
| Authentication | OkHttp interceptor injecting `Authorization: Bearer <token>` from `TokenContext` |
| Response parsing | `ApiResult.of(rawMap).assertSuccess()` — unified wrapper with code assertion and typed extraction |
| Async execution | `AsyncUtil.run()` — dedicated thread pool (daemon threads, DiscardOldestPolicy) + `runLater()` for UI thread switching |
| Exception handling | `ApiException` with response code; `GlobalExceptionHandler` with dialog throttling and auth-expired detection |
| Auth expiry | `AuthContextHolder` — CAS-guarded navigation to login screen on 401 responses |

**Critical**: Retrofit calls are synchronous and blocking — they MUST NOT run on the JavaFX Application Thread. Always wrap with `AsyncUtil.run()`.

For complete integration patterns (config, API interface, interceptor, Service layer, async, exception handling), see `references/networking-retrofit.md`.

## Custom Controls

When standard JavaFX controls cannot meet the requirements, build custom controls using the Control/Skin architecture.

| Aspect | Pattern |
|--------|---------|
| Control class | `extends Control` — public API, properties, CSS metadata; no rendering logic |
| Skin class | `extends SkinBase<MyControl>` — visual rendering, event handling, layout |
| Properties | JavaFX Beans convention (lazy init + final getter/setter) |
| CSS styling | `StyleableObjectProperty` + `CssMetaData` for custom `-fx-*` properties |
| Pseudo-classes | `PseudoClass.getPseudoClass()` for visual states (:readonly, :hover) |
| High-perf drawing | `Canvas` + `GraphicsContext` for complex visuals (charts, gauges) |

**Rule of thumb**: If you only need to compose existing controls, extend `Region` or a layout pane. If you need custom rendering, CSS-stylable properties, or novel interaction, use Control + Skin.

For complete patterns (properties, CSS metadata, pseudo-classes, Canvas rendering, event handling) and a full RatingControl example (Control + Skin + CSS), see `references/custom-controls.md`.

## Packaging

### jpackage (Recommended)

`jpackage` packages a JavaFX application into a platform-native installer with an embedded custom JRE. **jpackage does not support cross-compilation** — each platform's installer must be built on a runner of the corresponding OS. To distribute on all three desktop platforms, maintain a separate jpackage command per platform (the templates below) and orchestrate them via a CI/CD matrix build.

#### Common Prerequisites

```bash
# Build the application JAR first (shared across all platforms)
mvn clean package
```

#### Windows Template (`.msi`)

Requires WiX Toolset 4.x on the PATH. Use a stable `--win-upgrade-uuid` so newer versions upgrade in place rather than install side-by-side.

```bash
jpackage \
  --type msi \
  --name "MyApp" \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example/com.example.App \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon src/main/resources/icons/app.ico \
  --win-menu \
  --win-shortcut \
  --win-upgrade-uuid "12345678-1234-4234-8234-123456789abc" \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

#### macOS Template (`.dmg`)

Requires Xcode command line tools. The `--mac-package-identifier` (reverse-DNS) must be stable and match the signing/notarization profile.

```bash
jpackage \
  --type dmg \
  --name "MyApp" \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example/com.example.App \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon src/main/resources/icons/app.icns \
  --mac-package-name "MyApp" \
  --mac-package-identifier "com.mycompany.myapp" \
  --mac-sign \
  --mac-signing-key-user-name "Developer ID Application: Your Name (XXXXXXXXXX)" \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

#### Linux Templates (`.deb` and `.rpm`)

Requires `dpkg-deb` (for `.deb`) or `rpm-build` (for `.rpm`). Run both commands to produce both distribution formats.

```bash
# .deb (Debian/Ubuntu)
jpackage \
  --type deb \
  --name "myapp" \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example/com.example.App \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon src/main/resources/icons/app.png \
  --linux-package-name "myapp" \
  --linux-deb-maintainer "dev@mycompany.com" \
  --linux-rpm-license-type "MIT" \
  --install-dir /opt/myapp \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist

# .rpm (Fedora/RHEL)
jpackage \
  --type rpm \
  --name "myapp" \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example/com.example.App \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon src/main/resources/icons/app.png \
  --linux-package-name "myapp" \
  --linux-rpm-license-type "MIT" \
  --install-dir /opt/myapp \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

#### Toolchain Requirements per Platform

| Platform | Output Type | Required Toolchain | Install Command |
|----------|-------------|---------------------|-----------------|
| Windows | `msi` | WiX Toolset 4.x | `dotnet tool install --global wix` |
| Windows | `exe` | Inno Setup 6+ | Download from jrsoftware.org, add to `PATH` |
| macOS | `dmg` / `pkg` | Xcode command line tools | `xcode-select --install` |
| Linux | `deb` | `dpkg-deb` | `apt install dpkg-dev` (Debian/Ubuntu) |
| Linux | `rpm` | `rpmbuild` | `dnf install rpm-build` (Fedora/RHEL) |

> **Note**: jpackage cannot cross-compile. Building a Windows `.msi` requires a Windows host with WiX; a macOS `.dmg` requires macOS with Xcode tools; Linux `.deb`/`.rpm` require the respective Linux toolchain. For multi-platform delivery, generate each artifact on its native OS via a CI/CD matrix build (see `references/cross-platform-packaging.md`).

### Output types by platform:
- Windows: `--type exe` (Inno Setup) or `--type msi` (WiX)
- macOS: `--type dmg` or `--type pkg`
- Linux: `--type deb` or `--type rpm`

For detailed packaging guides, see `references/packaging-deployment.md` and `references/cross-platform-packaging.md`.

## Constraints

### Coding Standards
1. **Naming**: PascalCase for classes, camelCase for methods/variables, SCREAMING_SNAKE_CASE for constants
2. **Indentation**: 4 spaces, no tabs
3. **Encoding**: UTF-8 for all source files
4. **Imports**: Explicit imports, no wildcards (`import javafx.scene.control.*`)
5. **Comments**: Javadoc on public API, inline comments for complex logic

### Architecture Rules
1. **FXML purity**: No `<fx:script>` in FXML files
2. **Controller responsibility**: Only UI events and view state, delegate business logic to Service
3. **Binding first**: Prefer JavaFX Properties binding over manual UI sync
4. **Resource paths**: Use `getClass().getResource()` for FXML/CSS loading
5. **Thread safety**: All UI updates on JavaFX Application Thread, use `Task`/`Service` for background work

### Security Rules
1. **Input validation**: Validate all user input, no SQL/command concatenation
2. **Path safety**: Use `Paths.get()` + `Path.normalize()` for file operations
3. **No hardcoded secrets**: Use config files or environment variables
4. **WebView security**: Disable JavaScript or restrict to trusted content

## Output Format

When delivering code, always provide:

1. **File manifest** — List all generated files with full paths
2. **Dependencies** — Required Maven/Gradle dependencies
3. **Run instructions** — Compile and run commands (e.g., `mvn javafx:run`)
4. **UI preview** — If a UI screenshot is available (from `javafx-runner` verification or manual capture), embed it in the delivery to give the user a visual confirmation of the rendered UI. If no screenshot is available, provide an FXML control tree diagram (as a Mermaid flowchart) as a structural preview
5. **Next steps** — Suggestions for extensions, testing, packaging

### Example Output Structure

```
## Generated Files

### Java Source
- `src/main/java/com/example/App.java` — Application entry point
- `src/main/java/com/example/controller/MainController.java` — Main controller
- `src/main/java/com/example/model/User.java` — Data model

### Resources
- `src/main/resources/fxml/main-view.fxml` — Main layout
- `src/main/resources/css/style.css` — Stylesheet

### Build Config
- `pom.xml` — Maven build configuration
- `src/main/java/module-info.java` — Module descriptor

### Dependencies
[Additional Maven dependencies if needed]

### Run Command
mvn javafx:run

### UI Preview
- `target/ui-preview.png` — Main window screenshot (if available from javafx-runner verification)
- Or: FXML control tree diagram (Mermaid flowchart) as structural preview fallback
```

## Quality Checklist

Before delivering, verify:
- [ ] All Java files compile without syntax errors
- [ ] FXML `fx:id` fields match Controller fields
- [ ] CSS files have no syntax errors, variables defined
- [ ] `module-info.java` includes all necessary `requires` and `opens`
- [ ] Package paths are consistent, no typos
- [ ] Class/method names follow naming conventions
- [ ] No hardcoded sensitive info or absolute paths
- [ ] Thread safety: background tasks use `Platform.runLater()`
- [ ] Resource paths are correct (relative, not absolute)
- [ ] Javadoc comments on public API
- [ ] Every Java source file has `@req` annotation in class Javadoc header
- [ ] Test methods follow `test{Behavior}_{REQ_ID}()` naming convention
- [ ] Requirement Traceability Matrix in `requirements.md` is updated with implementation and test mappings
- [ ] JavaFX 24+ projects configured with `--enable-native-access=javafx.graphics`
- [ ] Spring Boot Controllers annotated with `@Scope("prototype")`

## Fix Consumption Protocol

This protocol defines how `javafx-developer` consumes fix handoff reports produced by `javafx-code-reviewer` and `javafx-runner`. It enables the closed-loop "fix → re-verify" cycle without manual intervention.

### When to Apply

Use this protocol when the input is a fix handoff report, identified by the presence of `fix_handoff` fields:
- `target_file`: File path to modify
- `target_lines`: Start-end line range
- `fix_type`: `replace` / `insert` / `delete`
- `fix_priority`: Integer, 1 = highest priority
- `code_fingerprint`: SHA-256 hash of the problematic code snippet (for drift-resistant matching)
- `anchor_pattern`: Surrounding context signature (for secondary location matching)
- `ast_node_signature`: Fully qualified method/field/class signature (for refactor-resistant matching; `null` for non-Java files)

This protocol is **also activated when `refactor-handoff.json` exists** (produced by `javafx-refactorer`). Unlike code-reviewer/runner reports, the refactorer embeds the 7 unified Fix Handoff fields **inside each `refactoring_plan[]` array item** rather than under a top-level `fix_handoff` field. Therefore, treat `refactor-handoff.json` as a valid fix handoff source when:
- The file `refactor-handoff.json` is present in the project, **and**
- Its `refactoring_plan[]` array contains items with the unified Fix Handoff fields: `target_file`, `target_lines`, `fix_type`, `fix_priority`, `code_fingerprint`, `anchor_pattern`, `ast_node_signature`.

In this case, each `refactoring_plan[]` item is parsed as a single fix entry (using the same field semantics as the `fix_handoff` fields above), with `refactor_id` mapping to the fix id and `before_snippet`/`after_snippet` supplying the replace content. No field-name conversion is required — the refactorer's `refactor-handoff-schema.json` already aligns these fields with the unified Fix Handoff contract.

### Workflow

**Step 1: Parse Report**
- Extract all fix handoff entries from the report
- **Multi-source merge**: When the input contains fix handoffs from **both** `javafx-code-reviewer` and `javafx-runner` (parallel execution mode), handle merging based on the execution context:
  1. **Detect orchestrated batch**: Check whether any entry already carries a `dedup_merged_from` field — this signals that the orchestrator has already merged and deduplicated the batch (see `javafx-orchestrator/SKILL.md` → Step 4 "Fix Handoff Merge & Dedup"). If present, **skip deduplication entirely** — consume the pre-merged batch as-is and proceed directly to re-sorting (step 3). Do NOT re-deduplicate, as this would discard the orchestrator's traceability metadata (`dedup_merged_from`).
  2. **Standalone deduplication (standalone mode only)**: If NO entry carries a `dedup_merged_from` field — i.e., the developer is operating in standalone mode and directly receiving unmerged dual-source handoffs — perform deduplication: For entries targeting the **same `target_file`** with **overlapping `target_lines`** ranges (e.g., reviewer reports lines 10-15 and runner reports lines 12-18 on the same file):
     - Keep the entry with the **higher severity** (Critical > Major > Minor > Info)
     - If both have the same severity, keep the **lower `fix_priority`** number (higher priority)
     - Record the discarded entry's source and issue title in the kept entry's `dedup_merged_from` field for traceability (mirroring the orchestrator's field name)
  3. **Re-sort**: Sort the (deduplicated or pre-merged) list by `fix_priority` ascending (1 = highest) across all sources
- Sort by `fix_priority` ascending (1 = highest)
- Group entries by `target_file` for batch processing

**Step 1a: Concurrent Fix Grouping**

After parsing and sorting, group fixes into **concurrent groups** for parallel execution:

1. **File-based grouping**: Group all fix entries by `target_file`. Each group contains all fixes targeting the same file:
   ```
   Group A (UserController.java): [RF-001 (lines 10-15), RF-004 (lines 45-50)]
   Group B (User.java):            [RF-002 (lines 8-12)]
   Group C (user-view.fxml):       [RF-003 (lines 20-25)]
   Group D (OrderService.java):    [RF-005 (lines 30-35), RF-006 (lines 60-65)]
   ```

2. **Parallel eligibility**: Groups targeting **different files** are eligible for parallel execution. Groups targeting the **same file** (after AST relocation in Step 3) must be merged and executed serially.

3. **Intra-file ordering**: Within each file group, sort fixes by:
   - **Primary**: `fix_priority` ascending (1 = highest priority, applied first)
   - **Secondary**: `target_lines` start line **descending** (highest line first — prevents line drift from earlier fixes shifting later line numbers)
   - After each fix within a group, recompute line numbers for remaining fixes in the same group based on the line count delta

4. **Concurrency limit**: The maximum number of parallel groups is bounded by `min(file_group_count, 4)` — a hard limit of 4 concurrent file modifications to avoid excessive I/O contention and maintain debuggability. When there are ≤ 4 file groups, all execute in parallel. When there are > 4, groups are processed in batches of 4.

5. **Dependency-aware scheduling**: If a fix in Group A creates a new file that a fix in Group B references (detected via `new_files` field in refactoring handoffs), Group B must wait for Group A to complete. Record such dependencies in a **group dependency graph**:
   - Build a DAG: Group A → Group B if any fix in B depends on a `new_file` created by a fix in A
   - Topological sort: Execute groups in dependency order; independent groups run in parallel

6. **Output**: A list of file groups with intra-file ordering and inter-group dependency graph, ready for parallel execution in Step 2

**Step 1.5: Pre-Fix Backup**
Before applying any fixes, create backups of all files that will be modified:
1. **Create backup directory**: `.fix-backup/{timestamp}/` in the project root, where `{timestamp}` is `yyyy-MM-dd-HHmmss` format (e.g., `.fix-backup/2026-06-29-101530/`)
2. **Copy target files**: For each unique `target_file` in the fix handoff list, copy the current version to the backup directory, preserving the relative path structure:
   ```
   .fix-backup/2026-06-29-101530/
   ├── src/main/java/com/example/controller/UserController.java
   ├── src/main/java/com/example/model/User.java
   └── src/main/resources/fxml/user-view.fxml
   ```
3. **Create backup manifest**: Write `.fix-backup/{timestamp}/manifest.json` recording:
   - `timestamp`: Backup creation time
   - `loop_id`: Current loop ID from `.loop-state.json`
   - `round`: Current round number
   - `files`: List of backed-up file paths (relative to project root)
   - `fix_count`: Total number of fix handoff entries
4. **Skip backup for skipped fixes**: If a fix entry is already marked as `skipped` during parsing (e.g., invalid target file), do not include its `target_file` in the backup
5. **Backup failure handling**: If a `target_file` does not exist (file was deleted or path is wrong), skip it and note in the backup manifest as `missing`

**Step 2: Locate & Apply Fix** (per entry, with concurrent execution across file groups)

Fixes are applied using a **two-level execution model**:
- **Inter-file level (parallel)**: Different file groups execute concurrently (up to 4 parallel groups, per Step 1a concurrency limit)
- **Intra-file level (serial)**: Within each file group, fixes are applied serially in the order determined by Step 1a (priority ascending, then line number descending)

**Parallel execution flow**:
1. **Launch file groups**: For each independent file group (no unresolved dependencies in the group dependency graph), launch a parallel execution context
2. **Intra-group serial application**: Within each group, apply fixes one by one:
   - Read `target_file`, navigate to `target_lines`
   - Validate: verify the code at `target_lines` matches the "Problematic Code" snippet in the report (using Step 3 location matching)
   - Execute `fix_type`:
     - `replace`: Replace the target lines with the "Corrected Example" from the report
     - `insert`: Insert the "Corrected Example" after `target_lines`
     - `delete`: Remove `target_lines`
   - Record fix status: `applied` / `skipped` / `failed` (thread-safe write to Fix Summary — see Step 5)
   - **Recompute line numbers**: After each fix within the group, adjust remaining fixes' `target_lines` based on the line count delta (added lines → shift down, removed lines → shift up)
3. **Wait for all groups**: After launching all parallel groups, wait for all to complete before proceeding to Step 4 (Cross-Impact Check)
4. **Dependency resolution**: If a group has dependencies on other groups (from Step 1a DAG), it waits for its dependencies to complete before starting

**Thread safety guarantees**:
- Each file group operates on a **distinct file** — no file is modified by two groups simultaneously
- The Fix Summary is written using a **thread-safe append** mechanism (see Step 5 for details)
- The pre-fix backup (Step 1.5) is created **before** any parallel execution begins — all groups read from the same backup baseline
- If any group encounters a fatal error (e.g., file not found, disk full), that group is aborted and its fixes are marked as `failed`. Other groups continue independently

**Step 3: Location Matching & Line Drift Handling**
- **Fingerprint-first matching** (primary): Compute the SHA-256 hash of the code at `target_lines` (normalized: whitespace-trimmed) and compare with the report's `code_fingerprint` field. If they match, apply the fix at `target_lines` directly
- **Anchor-based matching** (secondary): If fingerprint does not match (line drift detected), use `anchor_pattern` (2 lines before + 2 lines after) to search for the correct location within the file. If the anchor pattern matches at exactly one location, apply the fix there and record status as `applied (relocated by anchor)`
- **Content-based matching** (fallback): If both fingerprint and anchor matching fail, fall back to searching for the "Problematic Code" snippet content within ±10 lines of the specified range. If found, apply fix at the actual location and record status as `applied (relocated)`
- **AST signature matching** (refactor-resistant): If all three methods above fail (indicating the code has been moved, renamed, or significantly refactored), use `ast_node_signature` to locate the code by its structural identity:
  1. Parse `ast_node_signature` to extract the fully qualified class name and optional member name (split on `#`)
  2. Search the project's source tree for the class file (resolve `{package}.{Class}` to `src/main/java/{package}/{Class}.java`)
  3. If the class file is found in a **different file** than `target_file`, update `target_file` to the actual location and record status as `applied (relocated by AST: {old_file} → {new_file})`
  4. If the signature contains a method (e.g., `#handleSave(ActionEvent)`), parse the file to find the method declaration matching the name and parameter types. Apply the fix at the method's actual location
  5. If the signature contains a field (e.g., `#userName`), find the field declaration and apply the fix there
  6. If only a class signature is present (e.g., `com.example.controller.UserController`), locate the class declaration and apply the fix at the class level
  7. If the class is not found in the expected package, perform a project-wide search by class name (simple name match) to handle package renames
  8. If the method/field is not found within the located class, record status as `skipped (AST node not found)` and include the signature in the Fix Summary for manual investigation
- **No match**: If all four methods fail, record status as `skipped (line drift)` and include in Fix Summary
- **Intra-file batch ordering** (within concurrent group): When multiple fixes target the same file, apply them in reverse line order (highest line first) to prevent earlier fixes from shifting later line numbers. After each fix, recompute line numbers for remaining fixes targeting the same file based on the line count delta of the applied fix. This ordering is determined in Step 1a and enforced during Step 2's intra-group serial application
- **AST relocation and group reassignment**: If AST signature matching (level 4) relocates a fix to a **different file** than originally grouped, that fix is moved to the target file's group. If the target file's group is already executing, the relocated fix is deferred to a post-parallel phase and applied serially after all parallel groups complete

**Step 4: Cross-Impact Check** (parallel execution)

After applying all fixes (all parallel file groups complete), verify cross-file consistency. The four check categories are **independent** and can be executed **in parallel**:

1. **Controller-FXML consistency check** (parallel task A): If any fix modified a Controller class, re-check that FXML `fx:id` fields match the Controller's `@FXML`-annotated fields. Scan all FXML files that reference the modified Controller via `fx:controller`
2. **Module descriptor check** (parallel task B): If any fix modified `module-info.java`, re-check all `requires` and `opens` directives are complete — verify that every package referenced by modified files has a corresponding `opens` or `exports` directive
3. **CSS reference check** (parallel task C): If any fix modified a CSS file, re-check that all `styleClass` references in FXML files still resolve to a CSS class definition. If any fix modified FXML, verify that new `styleClass` attributes have corresponding CSS definitions
4. **FXML-Controller binding check** (parallel task D): If any fix modified an FXML file, re-check the `fx:controller` path is valid and all event handler signatures (`onAction`, `onMouseClicked`, etc.) match methods in the Controller class

**Parallel execution rules**:
- All four checks run concurrently — they examine different file types and have no data dependency on each other
- Each check produces a **pass/fail result** with details of any mismatches found
- If any check fails, record the mismatch in the Fix Summary as a cross-impact warning
- All checks must pass for the fix batch to proceed to Step 4.5 (compile verification). A failed cross-impact check does NOT trigger rollback — instead, it generates a warning that is included in the Fix Summary for the reviewer/runner to evaluate in the next round

**Step 4.5: Post-Fix Compile Verification & Rollback**
After all fixes are applied and cross-impact checks pass, verify that the project still compiles:
1. **Execute compile verification**: Run `mvn compile -q` (incremental) to verify that the applied fixes did not introduce compilation errors. For Gradle projects, use `gradle compileJava --quiet`
2. **Compile passes** → Proceed to Step 5 (Output Fix Summary), set all fix statuses to `applied`
3. **Compile fails** → **Automatic rollback** is triggered:
   a. **Identify failed files**: Parse compiler output to identify which files have compilation errors
   b. **Rollback all modified files**: Restore every file from `.fix-backup/{timestamp}/` to its original location, overwriting the modified version. Use the backup manifest to ensure all backed-up files are restored
   c. **Verify rollback**: Re-run `mvn compile -q` to confirm the project compiles again after rollback (sanity check — the project should be in its pre-fix state)
   d. **Mark fix statuses**: Set all fix entries to `rolled_back` in the Fix Summary
   e. **Record rollback event**: Add a `rollback_event` entry to `.loop-state.json` (see Loop State Serialization)
   f. **Recommend next action**: If rollback occurs, recommend "manual intervention required — fixes caused compilation failure. Review the fix handoff entries and apply fixes individually to isolate the problematic change."
4. **Rollback failure handling**: If the rollback itself fails (e.g., backup file is corrupted or missing), mark status as `rollback_failed` and recommend immediate manual intervention. This is a critical error — the project may be in an inconsistent state

> **Why compile-only**: Runtime verification is deferred to `javafx-runner` in the next loop round. The rollback checkpoint only checks compilation — if the code compiles but has runtime issues, those are caught by the runner's verification, not by this checkpoint. This keeps the rollback decision fast and focused on structural integrity.

**Step 5: Output Fix Summary** (thread-safe aggregation)

After all parallel file groups complete (Step 2), all cross-impact checks finish (Step 4), and compile verification passes (Step 4.5), aggregate the results into a Fix Summary:

**Thread-safe Fix Summary construction**:
- During parallel execution (Step 2), each file group appends its fix results to a **thread-safe result buffer**. The buffer uses a concurrent append mechanism — each group writes only its own entries, and no group reads or modifies another group's entries
- After all groups complete, the buffer is **sorted** by `fix_priority` ascending to produce the final Fix Summary table
- Cross-impact check results (Step 4) are merged into the Fix Summary as additional warning rows, annotated with `cross-impact` in the Note column
- Rollback events (Step 4.5) are appended as a separate section below the Fix Summary table

| # | Priority | File | Lines | Fix Type | Status | Note |
|---|----------|------|-------|----------|--------|------|
| 1 | 1 | `path/to/File.java` | 10-15 | replace | applied | — |
| 2 | 2 | `path/to/Other.java` | 30-30 | insert | applied (relocated) | Line drift, found at line 32 |
| 3 | 3 | `path/to/Config.java` | 5-8 | delete | skipped | Line drift, snippet not found |
| 4 | 4 | `path/to/Broken.java` | 20-25 | replace | rolled_back | Compile failure after fix, file restored from backup |
| 5 | 5 | `path/to/Moved.java` | 45-50 | replace | applied (relocated by AST) | Method moved to `path/to/NewLocation.java`, found via `com.example.Moved#handleAction()` |

**Next Step Recommendation**:
- If all fixes applied: recommend "re-review and re-verify in parallel" (when both sources were present) or "re-review"/"re-verify" (single source)
- If any fix skipped: list skipped fixes and recommend manual intervention
- If any fix failed: describe the failure reason and recommend manual review
- If any fix rolled_back: recommend "manual intervention required — fixes caused compilation failure. Review the fix handoff entries and apply fixes individually to isolate the problematic change. Backup preserved at `.fix-backup/{timestamp}/`"

### Quality Check Scope in Fix Consumption Mode

In fix consumption mode, the full 12-item Quality Checklist is NOT re-run. Instead, only check:
1. Items directly relevant to the applied fixes
2. Cross-impact items identified in Step 4
3. Java compilation of modified files (syntax check)

## Loop Orchestration Protocol

> **Authoritative source**: When operating within an orchestrated loop, see `javafx-orchestrator/SKILL.md` for the authoritative definitions of:
> - **Loop State Machine** (state transitions, parallel execution, fix cycle)
> - **Loop Rules** (max rounds, re-review/re-verify strategy, convergence detection, pre-fix backup, rollback, backup cleanup)
> - **Combined Quality Gate** (reviewer + runner pass/fail matrix, priority rule)
> - **Loop State JSON** format (`.loop-state.json` schema with all fields)
> - **Serialization Triggers** (who writes what, when, and with what field isolation)
> - **State Recovery Protocol** (cross-session recovery, stale handling)
> - **Fix Handoff Format** (field definitions including `ast_node_signature`)
>
> The sections below describe only the **developer's role and responsibilities** within the loop — the minimal subset needed for standalone operation.

### Developer's Role in the Loop

`javafx-developer` occupies two stages of the loop:
- **Generating** (Round 1): Generate initial project code from user requirements (Steps 1–6)
- **Fixing** (Round 1+): Consume fix handoff reports from reviewer/runner, apply fixes (Step 5.5), output Fix Summary

### Developer's Serialization Responsibilities

1. **Read state**: Before applying fixes, check for `.loop-state.json` to determine current round and fix scope
2. **Apply fixes**: Consume merged Fix Handoffs using the [Fix Consumption Protocol](#fix-consumption-protocol) (4-level location matching: fingerprint → anchor → content → AST signature)
3. **Write result**: After applying fixes, update `rounds[current_round]` with `fixes_applied`, `fixes_skipped`, `fixes_rolled_back` counts and `fix_handoffs[]` array with per-handoff status (`applied`/`skipped`/`rolled_back`)
4. **Pre-fix backup**: Before applying any fixes, copy all target files to `.fix-backup/{timestamp}/` with a `manifest.json` — enables automatic rollback if post-fix compilation fails
5. **Rollback**: If post-fix compile fails, restore all modified files from `.fix-backup/{timestamp}/`, mark fixes as `rolled_back`, append `rollback_event` to state. See `javafx-orchestrator/SKILL.md` → Serialization Triggers for the full rollback event schema
6. **Backup cleanup**: `.fix-backup/` directory is auto-cleaned when the loop passes the quality gate. Paused/aborted loops preserve `.fix-backup/` for manual inspection

> **Fix Handoff Format**: See `javafx-orchestrator/SKILL.md` → Fix Handoff Format for the authoritative field definitions (`target_file`, `target_lines`, `fix_type`, `fix_priority`, `code_fingerprint`, `anchor_pattern`, `ast_node_signature`, and the optional `dedup_merged_from` added during merge). The developer's Fix Consumption Protocol (Step 1) describes how these fields are used for 4-level location matching, and how `dedup_merged_from` is used to detect pre-merged batches (skip dedup) versus standalone mode (perform dedup).

## Reference Documents

For in-depth guidance, refer to these documents in the `references/` directory:

- `references/project-setup.md` — Maven/Gradle configuration, version matrix, modular setup
- `references/architecture-patterns.md` — MVC/MVVM/MVP detailed comparison, anti-patterns
- `references/spring-boot-integration.md` — Spring Boot + JavaFX integration, startup class splitting, DI, common pitfalls
- `references/database-integration.md` — Database layer integration (JPA/Hibernate, MyBatis, Spring Data JPA, Flyway, HikariCP, JavaFX Property/Entity integration, thread-safety pitfalls)
- `references/css-best-practices.md` — CSS selectors, theme variables, responsive layout
- `references/data-binding-patterns.md` — Property types, binding modes, form validation
- `references/third-party-libraries.md` — Library integration guides, compatibility matrix
- `references/networking-retrofit.md` — Retrofit (retrofit-spring-boot-starter) network integration: API interface definition, OkHttp client registration, token interceptor, unified response parsing (ApiResult), async execution with UI thread safety, global exception handling, timeout/retry config
- `references/custom-controls.md` — Custom control development: Control/Skin architecture, JavaFX properties (Beans convention), CSS-stylable properties, pseudo-classes, Canvas-based rendering, event handling, complete RatingControl example (Control + Skin + CSS)
- `references/static-analysis-tools.md` — Static analysis tool integration: SpotBugs/PMD/Checkstyle Maven plugin configuration, JavaFX-tailored rule sets, false positive exclusions, report parsing, unified issue mapping for runner/reviewer consumption
- `references/packaging-deployment.md` — jpackage, jlink, CI/CD integration
- `references/cross-platform-packaging.md` — Cross-platform jpackage options matrix, per-platform toolchains, icon conversion, code signing, CI/CD build matrix
- `references/ci-cd-pipeline.md` — CI/CD pipeline configuration, Monocle headless testing, Loop Orchestration Protocol integration, example workflows
- `EVALUATE.md` — Evaluation test cases, used to quantify skill output quality

## Template Library

Reusable code templates in `templates/` directory:

- `templates/maven/pom.xml` — Maven POM template
- `templates/maven/module-info.java` — Module descriptor template
- `templates/maven/Application.java` — Application entry point template
- `templates/gradle/build.gradle` — Gradle build template
- `templates/fxml/main-view.fxml` — Main window FXML template
- `templates/fxml/dialog.fxml` — Dialog FXML template
- `templates/controller/MainController.java` — Controller template
- `templates/controller/BaseController.java` — Base controller template
- `templates/controller/DialogController.java` — Dialog controller template
- `templates/model/ObservableModel.java` — Model template
- `templates/viewmodel/UserViewModel.java` — ViewModel template (MVVM pattern)
- `templates/service/AbstractService.java` — Abstract service layer template (named AbstractService to avoid clashing with javafx.concurrent.Service)
- `templates/service/Repository.java` — Repository interface template
- `templates/dao/Entity.java` — JPA Entity template with JavaFX Property integration (@Access(PROPERTY), @Transient Property accessors, null-safe primitive setters)
- `templates/dao/Repository.java` — Spring Data JPA Repository interface template (JpaRepository extension, derived/JPQL query methods, thread-safety notes)
- `templates/dao/FlywayMigration.sql` — Flyway versioned migration SQL template (naming convention, idempotency rules, repeatable migration example)
- `templates/presenter/Presenter.java` — Presenter template (MVP pattern)
- `templates/presenter/View.java` — View interface template (MVP pattern)
- `templates/css/light-theme.css` — Light theme CSS
- `templates/css/dark-theme.css` — Dark theme CSS
- `templates/test/pom-test-dependencies.xml` — TestFX + JUnit 5 + Monocle test dependencies snippet
- `templates/test/MainWindowTest.java` — Main window integration test template (FXML load, controller injection, CSS)
- `templates/test/ControllerTest.java` — Controller unit test template (mocked Service, event handling logic)
- `templates/test/ViewModelTest.java` — ViewModel unit test template (binding logic, computed properties)
- `templates/test/CRUDViewTest.java` — Main view integration test template (FXML load, menu/content/status-bar structure verification; adaptable to CRUD views)
- `templates/packaging/jpackage-config.properties` — Packaging config
- `templates/ci/github-actions.yml` — GitHub Actions CI/CD workflow template (multi-platform matrix, Monocle headless testing, jpackage packaging)
- `templates/ci/gitlab-ci.yml` — GitLab CI/CD template (compile, test, cross-platform packaging with tagged runners)
- `templates/docs/requirements.md` — Requirements specification template (functional/non-functional requirements, technical constraints, traceability)
