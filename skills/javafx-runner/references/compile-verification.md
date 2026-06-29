# Compile Verification Rules

This document is the criteria for the "Compile Verification" dimension, governing 7 check items. It executes `mvn compile` (or `gradle compileJava`), parses the compiler output, and identifies compilation errors and warnings. Default severity baseline: Critical. Shares the same origin as `javafx-developer`'s Quality checklist - syntax check items.

> **Core Principle**: Compilation is the first gate of dynamic verification. Code that cannot compile cannot run or be packaged. This dimension focuses on whether the project can pass the compiler, whether dependencies resolve, whether module configuration is complete, and whether compile-time type checking is sound.

---

## Check Item 1: Syntax Compilation

**Focus**: Whether all Java source files can pass `javac` compilation with no syntax errors.

**Pass Criteria**:
- `mvn compile -q` (or `gradle compileJava --quiet`) exits with code 0, output contains no `[ERROR]` lines
- All Java source files under `src/main/java` compile without syntax errors (missing semicolons, unbalanced braces, invalid keywords, etc.)
- No unresolved symbol references caused by syntax errors (e.g., a typo in a class name preventing the compiler from resolving it)

**Fail Criteria** (any one constitutes failure):
- The compiler outputs `[ERROR] /path/File.java:[line,col] ...` syntax error records
- The build exits with a non-zero exit code due to compilation failure
- A single syntax error cascades into a large number of dependent errors (the root error must be located)

**Severity Baseline**: Critical (cannot be de-escalated; if compilation fails the project cannot run)

**Anti-pattern**:
```java
// UserController.java - missing semicolon and unbalanced parenthesis
public class UserController {
    @FXML
    private void handleSave() {
        String name = nameField.getText()   // missing semicolon
        if (name.isEmpty() {
            showError("Name is required");
        }
    }
}
```

Compiler output:
```
[ERROR] /src/main/java/com/example/controller/UserController.java:[24,36] ';' expected
[ERROR] /src/main/java/com/example/controller/UserController.java:[25,29] ')' expected
```

**Best Practice**:
```java
// Correct syntax, semicolons and parentheses complete
public class UserController {
    @FXML
    private void handleSave() {
        String name = nameField.getText();
        if (name.isEmpty()) {
            showError("Name is required");
        }
    }
}
```

---

## Check Item 2: Dependency Resolution

**Focus**: Whether all Maven/Gradle dependencies can be resolved, with no `ClassNotFoundException` or `NoClassDefFoundError` compile-time errors.

**Pass Criteria**:
- `mvn compile` / `gradle compileJava` resolves all declared dependencies, output contains no dependency resolution errors
- All `import` statements in source files correspond to resolvable dependencies in `pom.xml` / `build.gradle`
- JavaFX dependencies (`javafx.controls`, `javafx.fxml`, `javafx.graphics`, etc.) are declared with the correct version and can be downloaded from the repository

**Fail Criteria** (any one constitutes failure):
- The compiler outputs `cannot find symbol` for a class that should come from a third-party dependency
- Maven outputs `Could not resolve dependencies for project ...` or `Failure to find ... in ...`
- JavaFX modules are not declared, causing `package javafx.scene.control does not exist`

**Severity Baseline**: Critical (cannot be de-escalated; unresolved dependencies prevent compilation)

**Anti-pattern**:
```xml
<!-- pom.xml missing javafx.fxml dependency, but source imports javafx.fxml.FXML -->
<dependencies>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>21</version>
    </dependency>
    <!-- Missing javafx-fxml -->
</dependencies>
```

Compiler output:
```
[ERROR] /src/main/java/com/example/App.java:[3,24] package javafx.fxml does not exist
[ERROR] /src/main/java/com/example/App.java:[3,1] import javafx.fxml.FXMLLoader;
```

**Best Practice**:
```xml
<dependencies>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>21</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>21</version>
    </dependency>
</dependencies>
```

---

## Check Item 3: Module Configuration

**Focus**: Whether `module-info.java`'s `requires` / `exports` / `opens` declarations match the actual code.

**Pass Criteria**:
- `module-info.java` declares all required `requires` (javafx.controls, javafx.fxml, etc.)
- Packages requiring reflection access are exposed to the corresponding modules via `opens`
- `opens com.example.model to javafx.controls` is declared when using `PropertyValueFactory`
- `opens com.example.controller to javafx.fxml` is declared when loading controllers via `FXMLLoader`
- `exports` correctly exports public API packages without over-exposing internal implementation packages

**Fail Criteria** (any one constitutes failure):
- Missing `requires javafx.fxml`, causing FXMLLoader-related classes to be unavailable at compile time
- Using `PropertyValueFactory` but not `opens`-ing the model package to `javafx.controls` (compiles but reflection fails at runtime - caught here as a configuration mismatch)
- `exports` over-exposes internal implementation packages (e.g., exporting the controller package, which should only be `opens` to javafx.fxml)

**Severity Baseline**: Critical
- De-escalation condition: Missing `opens` does not affect current functionality (e.g., PropertyValueFactory not used) -> Major

**Anti-pattern**:
```java
// module-info.java - only exports, no opens; PropertyValueFactory reflection will fail at runtime
module com.example.app {
    requires javafx.controls;
    requires javafx.fxml;
    exports com.example.app.model;   // exports alone is not enough; reflection requires opens
}
```

**Best Practice**:
```java
// Correctly opens packages requiring reflection to the corresponding modules
module com.example.app {
    requires javafx.controls;
    requires javafx.fxml;
    exports com.example.app.model;
    opens com.example.app.model to javafx.controls;       // PropertyValueFactory reflection
    opens com.example.app.controller to javafx.fxml;      // FXMLLoader reflection
}
```

---

## Check Item 4: FXML Compile Association

**Focus**: Whether the fully qualified name of the Controller class can be resolved by the class loader (whether the class pointed to by `fx:controller` in FXML exists).

**Pass Criteria**:
- The `fx:controller` attribute in every FXML file points to an existing Controller fully qualified class name
- The Controller class is accessible under the module path (the package is `opens` to `javafx.fxml` or `exports`-ed)
- No typos in the Controller class name or package path in the FXML

**Fail Criteria** (any one constitutes failure):
- `fx:controller="com.example.controller.UserController"` points to a non-existent class (compile-time class resolution fails, or FXML association cannot be validated)
- The Controller class exists but the package is not exported/opened, causing the class loader to fail to access it at runtime
- The FXML references a Controller that has been moved or renamed without updating the `fx:controller` attribute

**Severity Baseline**: Critical (a mismatched Controller prevents FXML from loading)

**Anti-pattern**:
```xml
<!-- user-view.fxml - fx:controller points to a non-existent class path -->
<VBox xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.example.view.UserController">   <!-- actual package is com.example.controller -->
    <!-- ... -->
</VBox>
```

**Best Practice**:
```xml
<!-- fx:controller points to the correct fully qualified class name -->
<VBox xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.example.controller.UserController">
    <!-- ... -->
</VBox>
```

---

## Check Item 5: Generics and Types

**Focus**: Whether generic usages such as `TableView<User>` are type-safe, whether `cellValueFactory` callback signatures are correct.

**Pass Criteria**:
- Generic types such as `TableView<User>`, `ListView<String>`, `ComboBox<Region>` are declared with explicit type parameters
- `cellValueFactory` callbacks use the correct generic signatures: `new PropertyValueFactory<User, String>("name")` matches the `TableColumn<User, String>` declaration
- `Callback<CellDataFeatures<S, T>, ObservableValue<T>>` type parameters are consistent
- No raw type usage (`TableView` without `<...>`) that would generate unchecked warnings

**Fail Criteria** (any one constitutes failure):
- `cellValueFactory` callback generic types do not match the `TableColumn` declaration, causing a compile-time type error
- Using raw types `TableView` / `TableColumn` that trigger unchecked warnings and may mask real type errors
- `setCellFactory` callback signature does not match `Callback<TableColumn<S, T>, TableCell<S, T>>`

**Severity Baseline**: Critical (type mismatch is a compile error)
- Note: raw type usage that only generates warnings (no compile error) falls under Check Item 7 (Compilation Warning Triage) as Minor

**Anti-pattern**:
```java
// Type mismatch: TableColumn<User, String> but PropertyValueFactory uses <User, Integer>
TableColumn<User, String> nameCol = new TableColumn<>("Name");
nameCol.setCellValueFactory(new PropertyValueFactory<User, Integer>("name"));  // type mismatch
```

Compiler output:
```
[ERROR] incompatible types: PropertyValueFactory<User,Integer> cannot be converted to Callback<CellDataFeatures<User,String>,ObservableValue<String>>
```

**Best Practice**:
```java
// Generic types are consistent
TableColumn<User, String> nameCol = new TableColumn<>("Name");
nameCol.setCellValueFactory(new PropertyValueFactory<User, String>("name"));
// Or use the lambda form with explicit types
nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
```

---

## Check Item 6: Resource Path Compile-Time Check

**Focus**: Whether the resource paths referenced by `getClass().getResource("/fxml/xxx.fxml")` exist in the compile output.

**Pass Criteria**:
- All resource paths referenced by `getClass().getResource(...)` / `getClass().getResourceAsStream(...)` exist under `src/main/resources`
- The resource path in code matches the actual file path under `resources/` (case-sensitive, leading `/` convention)
- CSS files referenced via `scene.getStylesheets().add(...)` exist in the resources directory
- Image and icon resources referenced by `new Image("/images/logo.png")` exist

**Fail Criteria** (any one constitutes failure):
- Code references `/fxml/user-view.fxml` but the file is named `UserView.fxml` (case mismatch) or located in a different directory
- Resource paths use absolute filesystem paths (`/home/user/project/...` or `C:\project\...`) instead of classpath-relative paths
- Resources are placed under `src/main/java` instead of `src/main/resources`, causing them to be absent from the compile output classpath

**Severity Baseline**: Critical (a missing resource path causes a `NullPointerException` at runtime when `getResource` returns null)

**Anti-pattern**:
```java
// Resource path case mismatch; getResource returns null at runtime
Parent root = FXMLLoader.load(getClass().getResource("/fxml/UserView.fxml"));
// Actual file under resources: /fxml/user-view.fxml (lowercase)
```

**Best Practice**:
```java
// Path matches the actual resource file under src/main/resources/fxml/user-view.fxml
Parent root = FXMLLoader.load(getClass().getResource("/fxml/user-view.fxml"));
```

---

## Check Item 7: Compilation Warning Triage

**Focus**: Whether unused imports, deprecation warnings, and unchecked warnings affect running.

**Pass Criteria**:
- `mvn compile` outputs no warnings, or all warnings have been reviewed and confirmed to have no runtime impact
- Unused imports have been removed
- Deprecation warnings (`@Deprecated` API usage) have a documented migration plan or are confirmed safe for the current version
- Unchecked warnings (`unchecked` generic operations) have been reviewed to ensure no real type-safety risk

**Fail Criteria** (any one constitutes failure):
- A large number of unchecked warnings exist that may mask real type errors
- Deprecation warnings indicate use of APIs that will be removed in the target JavaFX version
- Unused imports are left in place (clutter, indicates incomplete cleanup)

**Severity Baseline**: Minor
- Escalation condition: Large number of unchecked warnings may mask real type errors -> Major

**Anti-pattern**:
```java
// Raw type usage generates unchecked warnings, may mask type errors
TableView users = new TableView();   // raw type, no generic parameter
TableColumn col = new TableColumn("Name");
col.setCellValueFactory(new PropertyValueFactory("name"));   // unchecked
users.getColumns().add(col);
```

Compiler output:
```
[WARNING] /src/main/java/com/example/App.java:[15,18] unchecked call to TableColumn() as a member of the raw type TableColumn
[WARNING] /src/main/java/com/example/App.java:[16,42] unchecked call to PropertyValueFactory() as a member of the raw type PropertyValueFactory
```

**Best Practice**:
```java
// Explicit generics, no unchecked warnings
TableView<User> users = new TableView<>();
TableColumn<User, String> col = new TableColumn<>("Name");
col.setCellValueFactory(new PropertyValueFactory<User, String>("name"));
users.getColumns().add(col);
```
