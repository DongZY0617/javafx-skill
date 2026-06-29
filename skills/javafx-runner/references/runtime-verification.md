# Runtime Verification Rules

This document is the criteria for the "Runtime Verification" dimension, governing 10 check items. It executes `mvn javafx:run` (or `gradle run`), launches the JavaFX application, and captures the startup process and runtime exceptions. Default severity baseline: Critical. Shares the same origin as `javafx-code-reviewer`'s Thread safety dimension (dynamically verifying static conclusions).

> **Core Principle**: Passing compilation only proves the code is syntactically and type correct; it does not prove the application can actually start and run. This dimension executes the application, captures stdout/stderr, and verifies that startup, FXML loading, CSS parsing, resource loading, module reflection, thread safety, and exit behavior all meet expectations.

---

## Check Item 1: Application Startup

**Focus**: Whether `Application.launch()` can start normally, whether the `start()` method can complete, whether the main window can be displayed.

**Pass Criteria**:
- `mvn javafx:run` launches the JVM and the JavaFX runtime without the process crashing during startup
- The `start(Stage stage)` method executes to completion, the primary `Stage` is shown via `stage.show()`
- The output contains no uncaught `Exception` / `Error` stack traces during the startup phase
- The JavaFX toolkit initializes successfully (no `java.lang.UnsatisfiedLinkError` for native libraries)

**Fail Criteria** (any one constitutes failure):
- The process exits during startup with a non-zero exit code
- The output contains an uncaught exception stack trace before `stage.show()` completes
- `Application.launch()` throws `IllegalStateException: Toolkit already initialized` or similar
- The JavaFX runtime fails to load native libraries (`Error: failed to load native library`)

**Severity Baseline**: Critical (cannot be de-escalated; if the application cannot start it cannot be delivered)

**Anti-pattern**:
```java
// start() throws because primaryStage is null or stage.show() is never called
@Override
public void start(Stage primaryStage) throws Exception {
    Parent root = FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));
    Scene scene = new Scene(root);
    // Missing primaryStage.setScene(scene) and primaryStage.show()
}
```

Runtime output:
```
Exception in Application constructor
Exception in thread "main" java.lang.RuntimeException: Unable to construct Application instance: class com.example.App
```

**Best Practice**:
```java
@Override
public void start(Stage primaryStage) throws Exception {
    Parent root = FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));
    Scene scene = new Scene(root);
    primaryStage.setScene(scene);
    primaryStage.setTitle("My App");
    primaryStage.show();   // Window is actually displayed
}
```

---

## Check Item 2: FXML Load

**Focus**: Whether all `FXMLLoader.load()` calls can successfully parse FXML files, whether `fx:controller` can be instantiated, whether `fx:id` injection can complete.

**Pass Criteria**:
- Every `FXMLLoader.load()` call returns a non-null root node, output contains no `LoadException`
- The `fx:controller` class is instantiated reflectively without `IllegalAccessException`
- All `fx:id` declared in FXML have matching `@FXML` fields in the Controller, injection completes without `NullPointerException`
- Controller `initialize()` method (or `Initializable.initialize()`) executes without throwing

**Fail Criteria** (any one constitutes failure):
- Output contains `javafx.fxml.LoadException` or `FXML load exception`
- `FXMLLoader` cannot reflectively access the Controller due to missing `opens` to `javafx.fxml`, throwing `IllegalAccessException`
- A `fx:id` in FXML has no corresponding `@FXML` field, throwing `LoadException: ... is not a field`
- An `onAction="#method"` references a method that does not exist in the Controller

**Severity Baseline**: Critical (cannot be de-escalated; runtime will always throw `LoadException`)

**Anti-pattern**:
```java
// module-info.java missing opens controller to javafx.fxml
module com.example.app {
    requires javafx.controls;
    requires javafx.fxml;
    exports com.example.app.controller;   // exports is not enough for reflection
    // Missing: opens com.example.app.controller to javafx.fxml;
}
```

Runtime output:
```
Caused by: java.lang.IllegalAccessException: class javafx.fxml.FXMLLoader$ControllerAccessor (in module javafx.fxml)
  cannot access class com.example.app.controller.UserController (in module com.example.app)
  because module com.example.app does not "opens com.example.app.controller" to module javafx.fxml
```

**Best Practice**:
```java
module com.example.app {
    requires javafx.controls;
    requires javafx.fxml;
    opens com.example.app.controller to javafx.fxml;   // FXMLLoader reflection access
}
```

---

## Check Item 3: CSS Parse

**Focus**: Whether all CSS stylesheets can be loaded by the JavaFX CSS parser without errors, with no unsupported `var()` syntax and no undefined looked-up colors.

**Pass Criteria**:
- `scene.getStylesheets().add("/css/app.css")` loads the stylesheet, output contains no `CSS Error`
- No use of the Web CSS `var()` function (unsupported by JavaFX CSS)
- All looked-up colors referenced via `-fx-xxx` are defined somewhere in the stylesheet chain
- Border radius uses literal numeric values, not looked-up color references to size variables

**Fail Criteria** (any one constitutes failure):
- Output contains `CSS Error` or `WARNING: Could not resolve`
- A stylesheet uses `var(-fx-primary-color)` (unsupported syntax, the property is silently ignored)
- A looked-up color reference `-fx-color: -fx-undefined` resolves to nothing and falls back to a default, degrading the visual style

**Severity Baseline**: Major
- De-escalation condition: Only a warning that does not affect rendering (e.g., undefined looked-up color falls back to default) -> Minor
- Escalation condition: Causes the UI to fail to display -> Critical

**Anti-pattern**:
```css
/* app.css - using unsupported var() and undefined looked-up color */
.button-primary {
    -fx-background-color: var(-fx-primary-color);   /* JavaFX CSS does not support var() */
    -fx-background-radius: var(-fx-radius);         /* unsupported */
    -fx-text-fill: -fx-undefined-color;             /* undefined looked-up color */
}
```

Runtime output:
```
WARNING: Could not resolve '-fx-undefined-color' while resolving lookups in '-fx-text-fill' of .button-primary
```

**Best Practice**:
```css
/* Use direct looked-up color references and literal border radius values */
.root {
    -fx-primary-color: #2196f3;
}
.button-primary {
    -fx-background-color: -fx-primary-color;   /* direct reference */
    -fx-background-radius: 8;                  /* literal numeric value */
    -fx-text-fill: white;
}
```

---

## Check Item 4: Resource Load

**Focus**: Whether images, icons, and internationalization resource bundles can be loaded correctly, with no `NullPointerException` on paths.

**Pass Criteria**:
- `new Image("/images/logo.png")` loads without `IllegalArgumentException: Invalid URL`
- `ResourceBundle.getBundle("messages")` finds the properties file on the classpath
- `getClass().getResource(...)` calls return non-null URLs for all referenced resources
- No `NullPointerException` caused by a resource path returning null and being passed to a method expecting non-null

**Fail Criteria** (any one constitutes failure):
- `getClass().getResource("/images/logo.png")` returns null, and the subsequent `new Image(url)` throws `NullPointerException`
- An image path uses a filesystem path (`C:\project\images\logo.png`) instead of a classpath path
- A `ResourceBundle` cannot be found, throwing `MissingResourceException`
- Resources are placed under `src/main/java` instead of `src/main/resources`, so they are absent from the runtime classpath

**Severity Baseline**: Critical (a null resource causes an NPE at runtime)

**Anti-pattern**:
```java
// Image path is a filesystem path; getResource returns null at runtime
Image logo = new Image("C:/project/images/logo.png");   // not on classpath
// Or:
URL url = getClass().getResource("/images/Logo.png");    // case mismatch, actual file is logo.png
ImageView logoView = new ImageView(new Image(url.toExternalForm()));   // NPE: url is null
```

Runtime output:
```
Exception in thread "JavaFX Application Thread" java.lang.NullPointerException
  at com.example.App.start(App.java:20)
```

**Best Practice**:
```java
// Use classpath-relative paths, verify resource exists under src/main/resources/images/logo.png
Image logo = new Image(getClass().getResourceAsStream("/images/logo.png"));
ImageView logoView = new ImageView(logo);
```

---

## Check Item 5: Module Runtime

**Focus**: Whether `module-info.java` satisfies all reflection requirements at runtime (`PropertyValueFactory`, FXML controller injection, `FXMLLoader` reflection access).

**Pass Criteria**:
- `PropertyValueFactory` can reflectively access model properties at runtime, no `IllegalAccessException`
- `FXMLLoader` can reflectively instantiate Controllers and inject `@FXML` fields
- All `opens` declarations required for runtime reflection are present
- No runtime `InaccessibleObjectException` caused by a reflective access on a non-opened package

**Fail Criteria** (any one constitutes failure):
- `PropertyValueFactory` throws `IllegalAccessException` at runtime because `opens model to javafx.controls` is missing
- `FXMLLoader` throws `IllegalAccessException` because `opens controller to javafx.fxml` is missing
- A `Field.setAccessible(true)` call fails with `InaccessibleObjectException` because the target package is not opened
- The module compiles but fails at runtime because `opens` was omitted (a classic compile-pass / runtime-fail gap)

**Severity Baseline**: Critical
- De-escalation condition: Missing `opens` does not affect current functionality (e.g., PropertyValueFactory not used) -> Major

**Anti-pattern**:
```java
// module-info.java - compiles but PropertyValueFactory reflection fails at runtime
module com.example.app {
    requires javafx.controls;
    requires javafx.fxml;
    exports com.example.app.model;
    // Missing: opens com.example.app.model to javafx.controls;
}
```

```java
// Controller uses PropertyValueFactory
TableColumn<User, String> nameCol = new TableColumn<>("Name");
nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
```

Runtime output:
```
Exception in thread "JavaFX Application Thread" java.lang.IllegalAccessException:
  class javafx.scene.control.cell.PropertyValueFactory cannot access class com.example.app.model.User
  because module com.example.app does not "opens com.example.app.model" to module javafx.controls
```

**Best Practice**:
```java
module com.example.app {
    requires javafx.controls;
    requires javafx.fxml;
    exports com.example.app.model;
    opens com.example.app.model to javafx.controls;       // PropertyValueFactory reflection
}
```

---

## Check Item 6: Thread Safety Runtime Verification

**Focus**: Whether a runtime `IllegalStateException: Not on FX application thread` is thrown.

**Pass Criteria**:
- The application runs without throwing `IllegalStateException: Not on FX application thread`
- All UI component updates execute on the JavaFX Application Thread (verified by the absence of the exception in output)
- Background threads that update UI use `Platform.runLater()` or `Task` callbacks
- `ObservableList` modifications bound to UI controls always execute on the FX thread

**Fail Criteria** (any one constitutes failure):
- Output contains `java.lang.IllegalStateException: Not on FX application thread; not on FX application thread`
- A background thread directly calls `setText`, `setItems`, or other UI mutation methods
- A `Task.call()` method directly updates UI components instead of using `updateMessage` / `updateValue`

**Severity Baseline**: Critical (cannot be de-escalated; runtime will always throw `IllegalStateException`)

**Anti-pattern**:
```java
// Background thread directly updating UI
new Thread(() -> {
    Thread.sleep(2000);
    statusLabel.setText("Done");   // throws IllegalStateException
}).start();
```

Runtime output:
```
Exception in thread "Thread-0" java.lang.IllegalStateException: Not on FX application thread;
  current thread = Thread-0; JavaFX Application Thread = JavaFX Application Thread
  at com.sun.javafx.tk.Toolkit.checkFxUserThread(Toolkit.java:...)
```

**Best Practice**:
```java
// Use Platform.runLater to switch back to the FX thread
new Thread(() -> {
    try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    Platform.runLater(() -> statusLabel.setText("Done"));
}).start();

// Or use Task with onSucceeded callback (automatically on FX thread)
Task<Void> task = new Task<>() {
    @Override
    protected Void call() throws Exception {
        Thread.sleep(2000);
        return null;
    }
};
task.setOnSucceeded(e -> statusLabel.setText("Done"));
new Thread(task).start();
```

---

## Check Item 7: JavaFX 24+ Native Access

**Focus**: Whether JavaFX 24+ projects configure `--enable-native-access=javafx.graphics`; whether startup reports `IllegalAccessError` when missing.

**Pass Criteria**:
- For JavaFX 24+ projects, the JVM is launched with `--enable-native-access=javafx.graphics` (configured via `pom.xml` `javafx-maven-plugin` `<options>` or `module-info`)
- The application starts without `IllegalAccessError` or native access warnings
- For JavaFX 17/21 projects, this check is skipped (native access is not required)

**Fail Criteria** (any one constitutes failure):
- A JavaFX 24+ project starts without `--enable-native-access=javafx.graphics`, and the output contains `IllegalAccessError: ... native access`
- The output contains a warning about restricted native methods being denied
- The JavaFX graphics module fails to initialize native rendering without the flag

**Severity Baseline**: Critical (cannot be de-escalated; runtime will always report `IllegalAccessError`)

**Anti-pattern**:
```xml
<!-- pom.xml with JavaFX 24+ but missing --enable-native-access -->
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>0.0.8</version>
    <configuration>
        <mainClass>com.example.app/com.example.app.App</mainClass>
        <!-- Missing: <options><option>--enable-native-access=javafx.graphics</option></options> -->
    </configuration>
</plugin>
```

Runtime output:
```
Exception in thread "main" java.lang.IllegalAccessError: class javafx.graphics (in module javafx.graphics)
  cannot access class sun.java2d ... because module javafx.graphics does not have native access
```

**Best Practice**:
```xml
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>0.0.8</version>
    <configuration>
        <mainClass>com.example.app/com.example.app.App</mainClass>
        <options>
            <option>--enable-native-access=javafx.graphics</option>
        </options>
    </configuration>
</plugin>
```

---

## Check Item 8: Headless Mode Verification

**Focus**: Whether a JavaFX application can be launched via the Monocle test framework in a CI environment (without a display).

**Pass Criteria**:
- In an environment without a display (`DISPLAY` unset on Linux, or no desktop session), the application launches via Monocle with `-Dmonocle.platform=Headless -Dprism.order=sw`
- The process does not crash due to missing display (`X11Display: Can't open display` or similar)
- The Monocle dependency (`org.testfx:openjfx-monocle`) is present in the test scope
- Headless startup completes within the timeout, exit code is 0 (for smoke tests) or as expected

**Fail Criteria** (any one constitutes failure):
- In a headless environment, the application crashes with `X11Display` / display-related errors because Monocle is not configured
- The Monocle dependency is missing, causing `ClassNotFoundException: com.sun.glass.ui.monocle.MonoclePlatform`
- `-Dprism.order=sw` (software rendering) is not set, causing the hardware pipeline to fail without a GPU

**Severity Baseline**: Major (affects CI verification but not local development)

**Anti-pattern**:
```bash
# CI environment runs mvn javafx:run without Monocle, display is unavailable
mvn javafx:run
# Output: X11Display: Can't open display :0
# Process exits with error
```

**Best Practice**:
```xml
<!-- Add Monocle dependency for headless testing -->
<dependency>
    <groupId>org.testfx</groupId>
    <artifactId>openjfx-monocle</artifactId>
    <version>jdk-21+26</version>
    <scope>test</scope>
</dependency>
```

```bash
# Run with Monocle headless mode in CI
mvn javafx:run -Dmonocle.platform=Headless -Dprism.order=sw
```

---

## Check Item 9: Startup Timeout Detection

**Focus**: Whether the application completes startup within a reasonable time (default 30-second timeout).

**Pass Criteria**:
- The application process completes startup (main window shown, `start()` returns) within the 30-second default timeout
- No blocking calls (`Thread.sleep`, synchronous network I/O, lock waits) exist in `start()` or `init()`
- The process does not hang waiting for external resources during startup

**Fail Criteria** (any one constitutes failure):
- The process is terminated after the 30-second timeout, output records "startup timeout"
- `start()` contains a blocking call (synchronous database query, network request) that delays window display
- The application initializes all heavy views at startup instead of lazy loading, exceeding the timeout

**Severity Baseline**: Major
- De-escalation condition: Timeout due to first-time loading of JavaFX modules (cold start), second startup is normal -> Minor
- Escalation condition: Timeout due to blocking call in `start()` -> Critical

**Anti-pattern**:
```java
// start() performs a synchronous network request, blocking the FX thread
@Override
public void start(Stage primaryStage) throws Exception {
    // Blocks for 40 seconds, exceeds the 30-second timeout
    String config = fetchConfigFromRemoteServer();   // synchronous network call
    Parent root = FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));
    primaryStage.setScene(new Scene(root));
    primaryStage.show();
}
```

Runtime output:
```
[INFO] --- javafx-maven-plugin:0.0.8:run (default-cli) @ app ---
... (no further output for 30 seconds)
[ERROR] Process timed out after 30 seconds, terminating
```

**Best Practice**:
```java
// Move blocking operations to a background Task, show window immediately
@Override
public void start(Stage primaryStage) throws Exception {
    Parent root = FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));
    primaryStage.setScene(new Scene(root));
    primaryStage.show();   // Window shows immediately

    // Load config in background
    Task<String> configTask = new Task<>() {
        @Override
        protected String call() throws Exception {
            return fetchConfigFromRemoteServer();   // background thread
        }
    };
    configTask.setOnSucceeded(e -> applyConfig(configTask.getValue()));
    new Thread(configTask).start();
}
```

---

## Check Item 10: Exit Code Check

**Focus**: The exit code is 0 when the application exits normally; a non-zero exit code indicates a runtime error.

**Pass Criteria**:
- When the application exits normally (user closes the window or `Platform.exit()` is called), the process exit code is 0
- No uncaught exception causes a non-zero exit
- `System.exit()` is not called with a non-zero code during normal operation
- Shutdown hooks (if any) complete without throwing

**Fail Criteria** (any one constitutes failure):
- The process exits with a non-zero exit code (e.g., exit code 1)
- An uncaught exception in the JavaFX Application Thread causes a non-zero exit
- `Platform.exit()` is not called, and `System.exit(non-zero)` is used incorrectly
- The process is killed by the OS (signal) due to resource exhaustion, recorded as a non-zero / abnormal exit

**Severity Baseline**: Critical (a non-zero exit indicates a runtime error that crashed the application)

**Anti-pattern**:
```java
// Uncaught exception in an event handler crashes the application with non-zero exit
@FXML
private void handleLoad() {
    List<User> users = userService.loadAll();   // throws RuntimeException if DB is down
    userTable.setItems(FXCollections.observableArrayList(users));
    // No try-catch, exception propagates, application crashes
}
```

Runtime output:
```
Exception in thread "JavaFX Application Thread" java.lang.RuntimeException: Database connection failed
  at com.example.service.UserService.loadAll(UserService.java:45)
  ...
Process exited with code 1
```

**Best Practice**:
```java
// Handle exceptions gracefully, keep the application running
@FXML
private void handleLoad() {
    Task<List<User>> loadTask = new Task<>() {
        @Override
        protected List<User> call() throws Exception {
            return userService.loadAll();
        }
    };
    loadTask.setOnSucceeded(e ->
        userTable.setItems(FXCollections.observableArrayList(loadTask.getValue())));
    loadTask.setOnFailed(e -> {
        Throwable ex = loadTask.getException();
        showError("Load failed: " + ex.getMessage());   // user-facing error, app stays alive
    });
    new Thread(loadTask).start();
}
```
