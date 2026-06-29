# Coding / Naming / Spring Boot / Version / API Compliance

This document is one of the primary reference documents for the "Deep Compliance Audit" dimension (corresponding to design spec section 3.6), governing 5 categories of check items: naming conventions, coding standards, Spring Boot pitfalls, version compatibility, and API misuse detection. Default severity baseline: Minor. For security rules, see `security-checklist.md`; for CSS compliance, see `css-compliance.md`; for Properties null safety, see `binding-compliance.md`.

---

## Check Item 1: Naming Conventions

**Focus**: PascalCase for classes, camelCase for methods / variables, SCREAMING_SNAKE_CASE for constants.

**Pass Criteria**:
- Class names, interface names, enum names use PascalCase (e.g., `UserController`, `UserService`)
- Method names, variable names, field names use camelCase (e.g., `handleSave`, `userName`)
- Constants (`static final`) use SCREAMING_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT`)
- Package names are all lowercase, no underscores or special characters (e.g., `com.example.app.controller`)
- Generic type parameters use single uppercase letters (e.g., `T`, `E`, `K`, `V`)

**Fail Criteria** (any one constitutes failure):
- Class names use camelCase or snake_case (e.g., `userController`, `user_controller`)
- Method names / variable names use PascalCase or SCREAMING_SNAKE_CASE
- Constants use camelCase (e.g., `maxRetryCount`)
- Package names contain uppercase letters or underscores

**Severity Baseline**: Minor
- Escalation condition: Public API naming violates standards and affects callers → Major

**Bad Example**:
```java
// Non-standard naming
public class userController {        // Class name should be PascalCase
    private int Max_count = 100;     // Constant should be SCREAMING_SNAKE_CASE
    private String UserName;         // Field should be camelCase

    public void HandleSave() { }     // Method should be camelCase
}
```

**Good Example**:
```java
// Standard naming
public class UserController {
    private static final int MAX_COUNT = 100;  // Constant SCREAMING_SNAKE_CASE
    private String userName;                    // Field camelCase

    public void handleSave() { }                // Method camelCase
}
```

---

## Check Item 2: Coding Standards

**Focus**: UTF-8 encoding, 4-space indentation, explicit imports (no wildcards), Javadoc on public API.

**Pass Criteria**:
- All source files use UTF-8 encoding
- Indentation uses 4 spaces, no tabs
- Uses explicit imports (e.g., `import javafx.scene.control.Button;`), no wildcards (`import javafx.scene.control.*;`)
- Public API (public methods, public classes) has Javadoc comments
- Complex logic uses inline comments for explanation

**Fail Criteria** (any one constitutes failure):
- File encoding is not UTF-8 (e.g., GBK, ISO-8859-1), causing garbled characters
- Using tab indentation or inconsistent indentation
- Using wildcard imports (`import ...*;`)
- Public API missing Javadoc

**Severity Baseline**: Minor (wildcard imports, missing Javadoc, etc. do not affect runtime)

**Bad Example**:
```java
// Wildcard imports
import javafx.scene.control.*;
import javafx.collections.*;

// Tab indentation + missing Javadoc
public class UserService {
	public User findById(Long id) {  // Tab indentation
		return repository.findById(id);  // Public method without Javadoc
	}
}
```

**Good Example**:
```java
// Explicit imports
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

// 4-space indentation + Javadoc
public class UserService {
    /**
     * Find a user by ID.
     * @param id the user ID, must not be null
     * @return the user object, or null if not found
     */
    public User findById(Long id) {
        return repository.findById(id);
    }
}
```

---

## Check Item 3: Spring Boot Pitfalls

**Focus**: Whether the main class does not directly extend `Application`, whether Controllers are annotated with `@Scope("prototype")`, whether `web-application-type: none` is configured.

**Pass Criteria**:
- The Spring Boot startup class (the class containing the `main` method) does not directly extend `Application`, split into startup class + JavaFX entry class
- Controllers are annotated with `@Component` and `@Scope("prototype")` to avoid singleton state pollution
- `application.yml` sets `spring.main.web-application-type` to `none` (JavaFX applications do not need a web server)
- `spring-boot-devtools` is not introduced (or set to optional + restart disabled)

**Fail Criteria** (any one constitutes failure):
- Startup class directly `extends Application` (causes the "JavaFX runtime components are missing" error)
- Controller annotated with `@Component` but not `@Scope("prototype")`, singleton Controller holding @FXML state fields
- `web-application-type: none` not configured, attempting to initialize a web server on startup
- `spring-boot-devtools` introduced without disabling restart (conflicts with JavaFX Application)

**Severity Baseline**:
- Startup class directly extending Application: Critical (cannot be de-escalated; causes Spring container initialization exception)
- Controller missing @Scope("prototype"): Major
  - De-escalation condition: Singleton Controller with no state fields → Minor
  - Escalation condition: Singleton Controller holding @FXML state fields → remain Major

**Bad Example**:
```java
// Startup class directly extends Application, causing runtime error
@SpringBootApplication
public class MyApp extends Application {
    @Override
    public void start(Stage stage) { /* ... */ }
    public static void main(String[] args) { launch(args); }
}
```
```java
// Controller is singleton but holds state fields
@Component
// Missing @Scope("prototype")
public class UserController implements Initializable {
    @FXML private TextField nameField;  // State pollution under singleton
}
```

**Good Example**:
```java
// Startup class does not extend Application
@SpringBootApplication
public class MyApp {
    static ConfigurableApplicationContext springContext;
    public static void main(String[] args) {
        springContext = SpringApplication.run(MyApp.class, args);
        Application.launch(JavaFXApp.class, args);
    }
}
// JavaFX entry class separately extends Application
public class JavaFXApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        loader.setControllerFactory(MyApp.springContext::getBean);
        stage.setScene(new Scene(loader.load()));
        stage.show();
    }
}
```
```java
// Controller annotated with @Scope("prototype")
@Component
@Scope("prototype")
public class UserController implements Initializable {
    @FXML private TextField nameField;
}
```
```yaml
# application.yml configuration
spring:
  main:
    web-application-type: none
```

---

## Check Item 4: Version Compatibility

**Focus**: Whether JavaFX 24+ configures `--enable-native-access=javafx.graphics`, whether version selection follows the LTS roadmap.

**Pass Criteria**:
- JavaFX 24+ projects configure `--enable-native-access=javafx.graphics` in `module-info.java` or JVM arguments
- Version selection follows the LTS roadmap: JavaFX 21 LTS (JDK 17+) or JavaFX 25 LTS (JDK 23+)
- JavaFX 17 LTS is marked as supported until 2026.10
- JavaFX 26 is marked as released (not "planned"/"expected")
- JDK version is compatible with JavaFX version

**Fail Criteria** (any one constitutes failure):
- JavaFX 24+ project does not configure `--enable-native-access=javafx.graphics`
- Recommending an expired version (e.g., JavaFX 17 LTS without noting 2026.10 end of support)
- Marking JavaFX 25 as "planned" or "expected" (actually already released LTS)
- Marking JavaFX 26 as "planned" (actually already released)
- JDK version incompatible with JavaFX version

**Severity Baseline**: Major (version configuration errors cause runtime warnings or missing features)

**Bad Example**:
```java
// JavaFX 24+ without --enable-native-access configured
// Runtime will output warning: WARNING: A restricted method in java.lang.foreign.Linker has been called
```
```
// Version recommendation error
// "JavaFX 25 planned for release in 2025"  <- Actually already released, should not be marked as "planned"
```

**Good Example**:
```xml
<!-- Configure JVM arguments in pom.xml (JavaFX 24+) -->
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <configuration>
        <options>
            <option>--enable-native-access=javafx.graphics</option>
        </options>
    </configuration>
</plugin>
```
```
// Accurate version matrix
// JavaFX 21 LTS - JDK 17+, mature and stable, recommended
// JavaFX 25 LTS - JDK 23+, latest LTS
// JavaFX 26 - released, latest features
// JavaFX 17 LTS - supported until 2026.10
```

---

## Check Item 5: API Misuse Detection

**Focus**: Whether nonexistent APIs are used (e.g., `select()`, `@FXML dispose()`), whether the deprecated ControlsFX `Dialogs.create()` is used.

**Pass Criteria**:
- Does not use `ObservableValue.select()` (this API does not exist; the correct usage is `Bindings.select()` or `SelectBinding`)
- Does not claim the existence of an `@FXML dispose()` lifecycle method (JavaFX has no such automatic callback; a custom `dispose()` must be defined and called manually)
- Does not use the deprecated ControlsFX `Dialogs.create()` API (use JavaFX native `Alert` / `Dialog`)
- Does not use nonexistent fluent property selection APIs such as `Person.select(p -> ...)`
- Does not use deprecated APIs (e.g., outdated methods of `javafx.scene.web.HTMLEditor`)

**Fail Criteria** (any one constitutes failure):
- Using the nonexistent `observableValue.select(func)` API
- Annotating `@FXML private void dispose()` and expecting the framework to call it automatically
- Using the deprecated ControlsFX `Dialogs.create().owner(stage).message("...").showInformation()` API
- Using nonexistent fluent property selection APIs

**Severity Baseline**: Major (using nonexistent APIs causes compile errors or runtime exceptions)

**Bad Example**:
```java
// Using the nonexistent select() API
StringBinding name = person.select(p -> p.getName());  // select() does not exist

// Claiming @FXML dispose() is a lifecycle method
@FXML
private void dispose() {  // @FXML dispose() does not exist, will not be automatically called
    model.removeListener(listener);
}

// Using deprecated ControlsFX Dialogs API
Dialogs.create()
    .owner(stage)
    .title("Notice")
    .message("Saved successfully")
    .showInformation();
```

**Good Example**:
```java
// Use Bindings.select or direct property access
StringBinding name = Bindings.selectString(person, "name");

// Custom dispose() method (without @FXML), called manually
public void dispose() {  // Regular public method
    model.removeListener(listener);
}
// Manually call in view-switching callback: oldController.dispose();

// Use JavaFX native Alert
Alert alert = new Alert(Alert.AlertType.INFORMATION);
alert.setTitle("Notice");
alert.setHeaderText(null);
alert.setContentText("Saved successfully");
alert.showAndWait();
```
