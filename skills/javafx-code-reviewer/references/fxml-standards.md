# FXML Standards

This document is the criteria for the "FXML Standards" dimension, governing 8 check items (corresponding to design spec section 3.3). It reviews the mapping between FXML files and Controllers, resource loading methods, and markup usage. Default severity baseline: Major. Shares the same origin as `javafx-developer`'s quality checklist · fx:id items.

---

## Check Item 1: fx:id Matching

**Focus**: Whether each `fx:id` in FXML has a corresponding `@FXML` field in the Controller, and vice versa.

**Pass Criteria**:
- Each `fx:id="xxx"` in FXML has a corresponding `@FXML private Type xxx;` field in the Controller
- Each `@FXML` field in the Controller has a corresponding `fx:id` in FXML
- Field types match the corresponding control type in FXML (e.g., `fx:id="nameField"` corresponds to `@FXML private TextField nameField;`)

**Fail Criteria** (any one constitutes failure):
- An `fx:id` exists in FXML with no corresponding `@FXML` field in the Controller (runtime throws `LoadException`)
- An `@FXML` field exists in the Controller with no corresponding `fx:id` in FXML (field is null)
- Field type does not match the control type

**Severity Baseline**: Major (cannot be de-escalated; runtime will always throw `LoadException`)

**Bad Example**:
```xml
<!-- FXML: fx:id="saveBtn" -->
<Button fx:id="saveBtn" text="Save"/>
```
```java
// Field name in Controller does not match
@FXML private Button saveButton;  // Should be saveBtn
```

**Good Example**:
```xml
<Button fx:id="saveBtn" text="Save"/>
```
```java
// fx:id and @FXML field correspond one-to-one
@FXML private Button saveBtn;
```

---

## Check Item 2: Controller Mapping

**Focus**: Whether the `fx:controller` path correctly points to the Controller fully qualified class name.

**Pass Criteria**:
- `fx:controller` uses the Controller's fully qualified class name (e.g., `com.example.app.controller.UserController`)
- The class name exactly matches the actual Controller class (including case)
- The Controller class exists and can be instantiated by FXMLLoader via reflection (or created via controllerFactory)

**Fail Criteria** (any one constitutes failure):
- `fx:controller` path is incorrect (package name or class name typo)
- Using a simple class name instead of a fully qualified class name (unless controllerFactory is configured)
- The Controller class does not exist

**Severity Baseline**: Major

**Bad Example**:
```xml
<!-- Using simple class name, FXMLLoader cannot locate it -->
<VBox fx:controller="UserController">
```

**Good Example**:
```xml
<!-- Using fully qualified class name -->
<VBox fx:controller="com.example.app.controller.UserController">
```

---

## Check Item 3: Script Prohibition

**Focus**: Whether `<fx:script>` is used in FXML (should be prohibited; logic must be in the Controller).

**Pass Criteria**:
- FXML files contain no `<fx:script>` tags
- All business logic and event handling are implemented in the Controller or ViewModel
- FXML remains purely declarative, only describing UI structure and bindings

**Fail Criteria** (any one constitutes failure):
- FXML uses `<fx:script>` to embed JavaScript or other scripting logic
- FXML uses inline expressions to handle business logic

**Severity Baseline**: Major

**Bad Example**:
```xml
<!-- Embedding script logic in FXML -->
<Button text="Calculate" onAction="#calculate">
    <fx:script>
        var result = parseInt(a) + parseInt(b);
        label.setText(result);
    </fx:script>
</Button>
```

**Good Example**:
```xml
<!-- FXML purely declarative, logic in Controller -->
<Button text="Calculate" onAction="#handleCalculate"/>
```
```java
@FXML
private void handleCalculate() {
    int result = parseInt(aField.getText()) + parseInt(bField.getText());
    resultLabel.setText(String.valueOf(result));
}
```

---

## Check Item 4: Event Handlers

**Focus**: Whether methods referenced by `onAction="#method"` exist in the Controller with the correct signature.

**Pass Criteria**:
- Methods referenced by `onAction="#method"` / `onMouseClicked="#method"` etc. exist in the Controller
- Method signature is `void method(ActionEvent event)` or `void method()` (no-arg)
- Method is annotated with `@FXML` (or is public)
- Method name matches the name referenced in FXML (including case)

**Fail Criteria** (any one constitutes failure):
- Event handler method referenced in FXML does not exist in the Controller (runtime throws `LoadException`)
- Method signature does not match (e.g., expects `void(ActionEvent)` but is actually `String method(ActionEvent)`)
- Method is private and not annotated with `@FXML` (FXMLLoader cannot access it)

**Severity Baseline**: Major

**Bad Example**:
```xml
<!-- FXML references handleSave -->
<Button text="Save" onAction="#handleSave"/>
```
```java
// Method name in Controller does not match (should be handleSave)
@FXML
private void saveData(ActionEvent event) { /* ... */ }
```

**Good Example**:
```xml
<Button text="Save" onAction="#handleSave"/>
```
```java
// Method name matches, signature correct
@FXML
private void handleSave(ActionEvent event) { /* ... */ }

// Or no-arg version
@FXML
private void handleSave() { /* ... */ }
```

---

## Check Item 5: Resource Paths

**Focus**: Whether `FXMLLoader` loading uses `getClass().getResource()`, rather than filesystem absolute paths.

**Pass Criteria**:
- FXML / CSS loading uses `getClass().getResource("/fxml/xxx.fxml")` or `getClass().getResourceAsStream()`
- Resource files are placed under `src/main/resources` and loaded via classpath
- Paths starting with `/` indicate loading from the classpath root
- Internationalization resources are loaded using `ResourceBundle.getBundle("i18n.messages")`

**Fail Criteria** (any one constitutes failure):
- Using filesystem absolute paths (e.g., `new File("C:/app/fxml/main.fxml")`) to load FXML
- Using relative path `new File("fxml/main.fxml")` to load (fails after packaging)
- Using `FileInputStream` to load classpath resources
- Resource path does not start with `/`, causing resolution failure from the current package path

**Severity Baseline**: Major

**Bad Example**:
```java
// Using filesystem path, fails after packaging
FXMLLoader loader = new FXMLLoader(new File("fxml/main.fxml").toURI().toURL());
// Using FileInputStream
FXMLLoader loader = new FXMLLoader(new FileInputStream("fxml/main.fxml"));
```

**Good Example**:
```java
// Use getClass().getResource to load from classpath
FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
Parent root = loader.load();
```

---

## Check Item 6: styleClass Consistency

**Focus**: Whether `styleClass` references in FXML are defined in the corresponding CSS.

**Pass Criteria**:
- Each style class referenced by `styleClass="xxx"` or `styleClass="a b c"` in FXML is defined in the corresponding CSS file
- The CSS stylesheet is correctly loaded via `scene.getStylesheets().add()`
- Style class names are consistent between FXML and CSS (including case)

**Fail Criteria** (any one constitutes failure):
- A `styleClass` referenced in FXML is not defined in CSS (style does not take effect)
- CSS stylesheet is not loaded into the Scene, causing all styleClasses to fail
- Style class name spelling is inconsistent (e.g., FXML uses `button-primary`, CSS defines `.button_Primary`)

**Severity Baseline**: Major
- De-escalation condition: Only individual style classes are undefined, does not affect core functionality → Minor

**Bad Example**:
```xml
<!-- FXML references button-primary and card-shadow -->
<Button styleClass="button-primary"/>
<VBox styleClass="card-shadow"/>
```
```css
/* CSS only defines button-primary, missing card-shadow */
.button-primary { -fx-background-color: #2196f3; }
/* .card-shadow is not defined */
```

**Good Example**:
```xml
<Button styleClass="button-primary"/>
<VBox styleClass="card-shadow"/>
```
```css
/* Both style classes are defined in CSS */
.button-primary { -fx-background-color: #2196f3; }
.card-shadow { -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2); }
```

---

## Check Item 7: controllerFactory

**Focus**: Whether `loader.setControllerFactory(springContext::getBean)` is set in Spring Boot scenarios.

**Pass Criteria**:
- In Spring Boot integration projects, FXMLLoader has `setControllerFactory(springContext::getBean)` set
- Controllers are annotated with `@Component` and `@Scope("prototype")`, managed by the Spring container
- Controllers injected via controllerFactory can use `@Autowired` dependency injection

**Fail Criteria** (any one constitutes failure):
- Spring Boot project does not set `controllerFactory`; Controllers are created by FXMLLoader's default `new`, unable to inject dependencies
- `controllerFactory` is set incorrectly (e.g., returns the wrong Bean type)
- Controller is annotated with `@Component` but `controllerFactory` is not set, causing Spring-injected dependencies to be null

**Severity Baseline**: Major

**Bad Example**:
```java
// Spring Boot project without controllerFactory, Controller dependencies cannot be injected
@FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
// Missing loader.setControllerFactory(...)
Parent root = loader.load();
// @Autowired userService in UserController is null
```

**Good Example**:
```java
// Set controllerFactory, Controller created by Spring container
FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
loader.setControllerFactory(springContext::getBean);
Parent root = loader.load();
// userService in UserController is injected
```

---

## Check Item 8: Root Element Namespace

**Focus**: Whether `xmlns:fx="http://javafx.com/fxml"` is declared, whether the FXML version matches the JavaFX version.

**Pass Criteria**:
- The root element declares `xmlns:fx="http://javafx.com/fxml"` (or `xmlns:fx="http://javafx.com/fxml/1"`)
- If using JavaFX version-specific features, the corresponding `xmlns="http://javafx.com/javafx/XX"` is declared (XX is the version number)
- The FXML version is compatible with the project's JavaFX version

**Fail Criteria** (any one constitutes failure):
- The root element does not declare the `xmlns:fx` namespace, causing `fx:id`, `fx:controller`, and other markup to fail parsing
- The FXML declared version is far higher than the project's actual JavaFX version, using unsupported features
- Missing the XML declaration `<?xml version="1.0" encoding="UTF-8"?>`

**Severity Baseline**: Major
- De-escalation condition: Only missing version declaration but functionality is normal → Minor

**Bad Example**:
```xml
<!-- xmlns:fx namespace not declared -->
<VBox>
    <Button fx:id="btn" text="Button"/>  <!-- fx:id cannot be parsed -->
</VBox>
```

**Good Example**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- Declare xmlns and xmlns:fx -->
<VBox xmlns="http://javafx.com/javafx/21"
      xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.example.app.controller.MainController">
    <Button fx:id="btn" text="Button"/>
</VBox>
```
