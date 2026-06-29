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
