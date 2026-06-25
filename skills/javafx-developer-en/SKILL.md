---
name: javafx-developer-en
description: |
  JavaFX desktop application development expertise covering project setup, FXML UI design,
  MVC/MVVM architecture, data binding, CSS theming, and cross-platform packaging.
  Invoke when: building JavaFX apps, creating FXML layouts, designing desktop UIs,
  implementing data binding, integrating ControlsFX/RichTextFX, or packaging JavaFX apps.
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

## Technology Stack

### JavaFX Version Matrix (as of 2026)

| JavaFX Version | Min JDK | LTS | Recommended Use |
|----------------|---------|-----|-----------------|
| 26.x | JDK 24 | No | Latest features (Metal renderer, Headless preview) |
| 25.x | JDK 23 | **Yes** | Production preferred choice |
| 21.x | JDK 17 | **Yes** | Conservative stable option |
| 17.x | JDK 11 | Yes (until 2026.10) | Legacy system maintenance |

**Default recommendation**: JavaFX 21 (LTS, JDK 17+) unless user requests otherwise.

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

1. **Identify intent**: Determine which capability module(s) the request falls into
2. **Extract key info**: Project name, package path, functionality, UI type, data model
3. **Ask for missing info**: If critical info is missing, ask the user
4. **Infer defaults**: Use reasonable defaults for package names, class names, module names

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

1. **Evaluate complexity**: Recommend MVC for simple apps, MVVM for complex ones
2. **Design layering**: Define Model, View, Controller/ViewModel responsibilities
3. **Plan file structure**: Organize classes, FXML, CSS files
4. **Select UI patterns**: Choose from preset UI component patterns

**MVC vs MVVM decision**:
- **MVC**: Simple CRUD apps, admin panels, small tools → Controller directly manipulates UI
- **MVVM**: Complex business logic, multi-view apps → ViewModel exposes Properties, View binds to them

### Step 4: Code Generation & Template Filling

1. **Load templates**: Read from `templates/` directory
2. **Variable replacement**: Replace placeholders with actual values
3. **Logic filling**: Add business logic code based on user requirements
4. **Style generation**: Generate corresponding CSS files
5. **Resource handling**: Handle icons, images, and static resource references

### Step 5: Quality Check

1. **Syntax check**: Verify Java/XML/CSS syntax correctness
2. **Naming convention**: Validate class/method/variable names follow Java conventions
3. **Module check**: Verify `module-info.java` exports/requires completeness
4. **Security review**: Check for SQL injection, path traversal, hardcoded credentials
5. **Best practices**: Verify adherence to JavaFX official recommended patterns

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
| ControlsFX | Dialogs, notifications, validation | `org.controlsfx:controlsfx:11.2.0` |
| MaterialFX | Material Design controls | `io.github.palexdev:materialfx:11.17.0` |
| RichTextFX | Rich text editor, code highlighting | `org.fxmisc.richtext:richtextfx:0.11.2` |
| Ikonli | Font icons (FontAwesome, Material) | `org.kordamp.ikonli:ikonli-javafx:12.3.1` |
| ValidatorFX | Form validation framework | `net.synedra:validatorfx:1.0.2` |
| TestFX | UI automated testing | `org.testfx:testfx-junit5:4.0.18` |

For detailed integration guides, see `references/third-party-libraries.md`.

## Packaging

### jpackage (Recommended)

```bash
# Build JAR first
mvn clean package

# Create native installer
jpackage \
  --type exe \
  --name "MyApp" \
  --app-version 1.0.0 \
  --input target/libs \
  --main-jar myapp.jar \
  --main-class com.example.App \
  --icon src/main/resources/icon.ico \
  --win-menu \
  --win-shortcut \
  --java-options "--enable-native-access=javafx.graphics"
```

### Output types by platform:
- Windows: `--type exe` or `--type msi`
- macOS: `--type dmg` or `--type pkg`
- Linux: `--type deb` or `--type rpm`

For detailed packaging guide, see `references/packaging-deployment.md`.

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
4. **Next steps** — Suggestions for extensions, testing, packaging

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

## Reference Documents

For in-depth guidance, refer to these documents in the `references/` directory:

- `references/project-setup.md` — Maven/Gradle configuration, version matrix, modular setup
- `references/architecture-patterns.md` — MVC/MVVM detailed comparison, anti-patterns
- `references/spring-boot-integration.md` — Spring Boot + JavaFX integration, startup class splitting, DI, common pitfalls
- `references/css-best-practices.md` — CSS selectors, theme variables, responsive layout
- `references/data-binding-patterns.md` — Property types, binding modes, form validation
- `references/third-party-libraries.md` — Library integration guides, compatibility matrix
- `references/packaging-deployment.md` — jpackage, jlink, CI/CD integration

## Template Library

Reusable code templates in `templates/` directory:

- `templates/maven/pom.xml` — Maven POM template
- `templates/maven/module-info.java` — Module descriptor template
- `templates/gradle/build.gradle` — Gradle build template
- `templates/fxml/main-view.fxml` — Main window FXML template
- `templates/fxml/dialog.fxml` — Dialog FXML template
- `templates/controller/MainController.java` — Controller template
- `templates/controller/BaseController.java` — Base controller template
- `templates/model/ObservableModel.java` — Model template
- `templates/css/light-theme.css` — Light theme CSS
- `templates/css/dark-theme.css` — Dark theme CSS
- `templates/packaging/jpackage-config.properties` — Packaging config
