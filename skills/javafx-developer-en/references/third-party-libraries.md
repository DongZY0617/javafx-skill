# JavaFX Third-party Library Integration Guide

This guide introduces the features, Maven coordinates, usage examples, and version compatibility of commonly used third-party libraries in the JavaFX ecosystem, helping developers quickly select and integrate them.

---

## 1. ControlsFX

ControlsFX is the most mature extended controls library in the JavaFX ecosystem, providing rich components such as dialogs, notifications, auto-completion, property sheets, and more.

### 1.1 Key Features

| Feature Module     | Description                                                        |
|--------------------|--------------------------------------------------------------------|
| Dialogs            | Enhanced dialogs (confirmation, input, exception display, etc.), more flexible than the native Alert |
| Notifications      | Desktop notification bubbles, supporting custom position, animation, and close actions |
| AutoComplete       | Text field auto-completion                                         |
| PropertySheet      | Property sheet control for visually editing object properties      |
| CheckComboBox      | Multi-select dropdown                                              |
| CheckListView      | Multi-select list                                                  |
| RangeSlider        | Dual-handle range slider                                           |
| Rating             | Star rating control                                                |
| SegmentedButton    | Segmented button group                                             |
| StatusBar          | Status bar control                                                 |
| MasterDetailPane   | Master-detail layout pane                                          |
| PlusMinusSlider    | Plus/minus slider                                                  |
| SpreadsheetView    | Spreadsheet view                                                   |

### 1.2 Maven Coordinates

```xml
<dependency>
    <groupId>org.controlsfx</groupId>
    <artifactId>controlsfx</artifactId>
    <version>11.2.1</version>
</dependency>
```

### 1.3 Code Examples

**Dialogs:**

```java
import org.controlsfx.dialog.Dialogs;

// Confirmation dialog
boolean confirm = Dialogs.create()
    .title("Confirm Delete")
    .masthead("Are you sure you want to delete this item?")
    .message("This action cannot be undone.")
    .showConfirm() == ButtonType.OK;

// Input dialog
Optional<String> result = Dialogs.create()
    .title("Enter Name")
    .masthead("Please enter the item name")
    .showTextInput();
```

> Note: Newer versions of ControlsFX recommend using JavaFX's native `Alert` and `TextInputDialog`. The ControlsFX `Dialogs` API is mainly for backward compatibility with legacy code.

**Notification Bubbles:**

```java
import org.controlsfx.control.Notifications;

Notifications.create()
    .title("Operation Successful")
    .text("Data has been saved")
    .graphic(new ImageView(successIcon))
    .hideAfter(Duration.seconds(3))
    .position(Pos.BOTTOM_RIGHT)
    .showInformation();

// Error notification
Notifications.create()
    .title("Save Failed")
    .text("Network connection timed out")
    .showError();
```

**Auto-completion:**

```java
import org.controlsfx.control.textfield.TextFields;

TextField searchField = new TextField();
// Bind auto-completion suggestion list
TextFields.bindAutoCompletion(searchField,
    "Apple", "Banana", "Cherry", "Date", "Elderberry");
```

**Property Sheet:**

```java
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.BeanProperty;
import org.controlsfx.property.editor.DefaultPropertyEditorFactory;

PropertySheet propertySheet = new PropertySheet();
// Add JavaBean-based property items
propertySheet.getItems().addAll(
    new BeanProperty(person, "name"),
    new BeanProperty(person, "age"),
    new BeanProperty(person, "email")
);
```

**CheckComboBox (Multi-select Dropdown):**

```java
import org.controlsfx.control.CheckComboBox;

CheckComboBox<String> checkCombo = new CheckComboBox<>(
    FXCollections.observableArrayList("Option A", "Option B", "Option C"));
checkCombo.getCheckModel().check(0);  // Check the first item
ObservableList<String> checked = checkCombo.getCheckModel().getCheckedItems();
```

---

## 2. MaterialFX

MaterialFX provides JavaFX controls in the Google Material Design style, featuring modern visual design and rich interactive components.

### 2.1 Key Controls

| Control            | Description                                |
|--------------------|--------------------------------------------|
| MFXButton          | Material-style button                      |
| MFXTextField       | Text field with floating label             |
| MFXCheckbox        | Material checkbox                          |
| MFXComboBox        | Enhanced dropdown                          |
| MFXDatePicker      | Date picker                                |
| MFXTableView       | Enhanced table (supports filtering, sorting) |
| MFXDialog          | Material-style dialog                      |
| MFXNotification    | Notification component                     |
| MFXProgressBar     | Progress bar                               |
| MFXSlider          | Slider                                     |
| MFXStepper         | Step navigator                             |
| MFXFilterComboBox  | Filterable dropdown                        |

### 2.2 Maven Coordinates

```xml
<dependency>
    <groupId>io.github.palexdev</groupId>
    <artifactId>materialfx</artifactId>
    <version>11.17.0</version>
</dependency>
```

### 2.3 Usage Example

```java
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.MFXComboBox;

// Material-style button
MFXButton button = new MFXButton("Submit");
button.setStyle("-fx-background-color: #6200ee; -fx-text-fill: white;");

// Floating label text field
MFXTextField textField = new MFXTextField();
textField.setFloatingText("Username");
textField.setPromptText("Please enter username");

// Enhanced dropdown
MFXComboBox<String> combo = new MFXComboBox<>();
combo.getItems().addAll("Option 1", "Option 2", "Option 3");
combo.setPromptText("Please select");
combo.selectFirst();

// Use in FXML
```

```xml
<?import io.github.palexdev.materialfx.controls.*?>

<MFXButton text="Submit" styleClass="mfx-button"/>
<MFXTextField floatingText="Email" promptText="Please enter email"/>
```

---

## 3. RichTextFX

RichTextFX provides a rich text editing area that supports styled text, syntax highlighting, line numbers, and more. It is the top choice for building code editors or rich text editors.

### 3.1 Key Features

| Feature              | Description                                              |
|----------------------|---------------------------------------------------------|
| StyleClassedTextArea | Style class-based rich text area                        |
| CodeArea             | Text area designed specifically for code editing, supports line numbers |
| InlineCssTextArea    | CSS-based rich text area                                |
| Syntax Highlighting  | Highlights keywords, comments, strings, etc. via style classes |
| Line Numbers         | Automatic line number gutter                            |
| Undo/Redo            | Built-in undo/redo support                              |

### 3.2 Maven Coordinates

```xml
<dependency>
    <groupId>org.fxmisc.richtext</groupId>
    <artifactId>richtextfx</artifactId>
    <version>0.11.3</version>
</dependency>
```

### 3.3 Code Examples

**Basic Code Editor:**

```java
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

CodeArea codeArea = new CodeArea();
// Add line numbers
codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

// Set initial content
codeArea.replaceText(0, 0, "public class Hello {\n    \n}");

// Apply syntax highlighting (simplified example)
codeArea.textProperty().addListener((obs, oldText, newText) -> {
    // Clear old styles
    codeArea.setStyle(0, newText.length(), "-fx-fill: black;");

    // Highlight keywords
    String[] keywords = {"public", "class", "void", "static", "private"};
    for (String keyword : keywords) {
        int idx = 0;
        while ((idx = newText.indexOf(keyword, idx)) >= 0) {
            codeArea.setStyle(idx, idx + keyword.length(),
                "-fx-fill: #cc7832; -fx-font-weight: bold;");
            idx += keyword.length();
        }
    }
});
```

**Rich Text Editing:**

```java
import org.fxmisc.richtext.StyleClassedTextArea;

StyleClassedTextArea area = new StyleClassedTextArea();
area.appendText("Normal text ");
area.appendText("Red bold", List.of("red-bold"));
area.appendText(" Normal text");

// Define style classes in CSS
// .red-bold { -fx-fill: red; -fx-font-weight: bold; }
```

---

## 4. Ikonli

Ikonli provides a unified solution based on font icons, integrating dozens of icon packs as an alternative to traditional image icons.

### 4.1 Supported Icon Packs (Partial)

| Icon Pack          | Description                        | Maven artifactId              |
|--------------------|------------------------------------|-------------------------------|
| MaterialDesign     | Google Material Design icons       | `ikonli-materialdesign2-pack` |
| FontAwesome        | Font Awesome icons                 | `ikonli-fontawesome-pack`     |
| Material Icons     | Material Icons                     | `ikonli-materialicons-pack`   |
| Ionicons           | Ionicons icons                     | `ikonli-ionicons4-pack`       |
| Octicons           | GitHub Octicons                    | `ikonli-octicons-pack`        |
| Feather            | Feather Icons                      | `ikonli-feather-pack`         |
| Bootstrap Icons    | Bootstrap Icons                    | `ikonli-bootstrapicons-pack`  |
| BoxIcons           | Box Icons                          | `ikonli-boxicons-pack`        |
| AntDesign Icons    | Ant Design Icons                   | `ikonli-antdesignicons-pack`  |
| CoreUI             | CoreUI Icons                       | `ikonli-coreui-pack`          |

### 4.2 Maven Coordinates

```xml
<!-- Core library -->
<dependency>
    <groupId>org.kordamp.ikonli</groupId>
    <artifactId>ikonli-javafx</artifactId>
    <version>12.3.1</version>
</dependency>

<!-- Select icon packs as needed -->
<dependency>
    <groupId>org.kordamp.ikonli</groupId>
    <artifactId>ikonli-materialdesign2-pack</artifactId>
    <version>12.3.1</version>
</dependency>
<dependency>
    <groupId>org.kordamp.ikonli</groupId>
    <artifactId>ikonli-fontawesome-pack</artifactId>
    <version>12.3.1</version>
</dependency>
```

### 4.3 Usage Example

```java
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.fontawesome.FontAwesome;

// Create icon
FontIcon saveIcon = new FontIcon(MaterialDesignS.CONTENT_SAVE);
saveIcon.setIconSize(24);
saveIcon.setIconColor(Color.BLUE);

// Add to button
Button saveButton = new Button("Save");
saveButton.setGraphic(saveIcon);

// FontAwesome icon
FontIcon userIcon = new FontIcon(FontAwesome.USER);
userIcon.setIconSize(20);

// Use in FXML
```

```xml
<?import org.kordamp.ikonli.javafx.FontIcon?>
<?import org.kordamp.ikonli.materialdesign2.MaterialDesignS?>

<FontIcon iconLiteral="md2al-content_save" iconSize="24" />
```

---

## 5. ValidatorFX

ValidatorFX provides a declarative form validation framework that integrates deeply with JavaFX controls.

### 5.1 Maven Coordinates

```xml
<dependency>
    <groupId>net.synedra</groupId>
    <artifactId>validatorfx</artifactId>
    <version>0.4.0</version>
</dependency>
```

### 5.2 Usage Example

```java
import net.synedra.validatorfx.Validator;

public class FormController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField ageField;
    @FXML private Button submitButton;

    private Validator validator = new Validator();

    @FXML
    public void initialize() {
        // Username validation: non-empty and length >= 3
        validator.createCheck()
            .dependsOn("username", usernameField.textProperty())
            .withMethod(c -> {
                String username = c.get("username");
                if (username == null || username.trim().length() < 3) {
                    c.error("Username must be at least 3 characters");
                }
            })
            .decoratingWith(this::decorateError)
            .decorate(usernameField);

        // Email validation
        validator.createCheck()
            .dependsOn("email", emailField.textProperty())
            .withMethod(c -> {
                String email = c.get("email");
                if (email == null || !email.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) {
                    c.error("Invalid email format");
                }
            })
            .decoratingWith(this::decorateError)
            .decorate(emailField);

        // Age validation
        validator.createCheck()
            .dependsOn("age", ageField.textProperty())
            .withMethod(c -> {
                try {
                    int age = Integer.parseInt(c.get("age"));
                    if (age < 0 || age > 150) {
                        c.error("Age must be between 0 and 150");
                    }
                } catch (NumberFormatException e) {
                    c.error("Age must be a number");
                }
            })
            .decoratingWith(this::decorateError)
            .decorate(ageField);

        // Bind submit button to validation status
        submitButton.disableProperty().bind(validator.containsErrorsProperty());
    }

    /** Error decorator: adds a red border and tooltip to the control */
    private Decoration decorateError(ValidationMessage message) {
        return new Decoration() {
            @Override
            public void add(Node target) {
                target.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                // Can add a Tooltip to display error message
                if (target instanceof Control control) {
                    Tooltip tooltip = new Tooltip(message.getText());
                    tooltip.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                    Tooltip.install(control, tooltip);
                }
            }

            @Override
            public void remove(Node target) {
                target.setStyle("");
                if (target instanceof Control control) {
                    Tooltip.uninstall(control, null);
                }
            }
        };
    }

    @FXML
    private void handleSubmit() {
        if (!validator.containsErrors()) {
            // Submit logic
        }
    }
}
```

---

## 6. TestFX

TestFX is a UI automation testing framework for JavaFX that supports simulating user interactions and asserting UI state.

### 6.1 Maven Coordinates

```xml
<dependency>
    <groupId>org.testfx</groupId>
    <artifactId>testfx-core</artifactId>
    <version>4.0.18</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testfx</groupId>
    <artifactId>testfx-junit5</artifactId>
    <version>4.0.18</version>
    <scope>test</scope>
</dependency>
```

### 6.2 Test Example

```java
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

class MainAppTest extends ApplicationTest {

    @Override
    public void start(Stage stage) throws Exception {
        // Start the application
        new MainApp().start(stage);
    }

    @Test
    void shouldAddTaskWhenClickingAddButton() {
        // Enter task title
        clickOn("#titleField").write("Test Task");

        // Click the add button
        clickOn("#addButton");

        // Verify the list contains the new task
        verifyThat("#taskListView", node -> {
            ListView<?> list = (ListView<?>) node;
            return list.getItems().size() == 1;
        });

        // Verify the status label updates
        verifyThat("#statusLabel", hasText("1 task in total"));
    }

    @Test
    void shouldShowErrorWhenTitleEmpty() {
        // Click add without entering anything
        clickOn("#addButton");

        // Verify the list is still empty
        verifyThat("#taskListView", node -> {
            ListView<?> list = (ListView<?>) node;
            return list.getItems().isEmpty();
        });
    }
}
```

### 6.3 Common TestFX Operations

```java
// Mouse operations
clickOn("#buttonId");
rightClickOn("#nodeId");
doubleClickOn("#nodeId");
moveTo("#nodeId");

// Keyboard operations
clickOn("#textField").write("Hello World");
type(KeyCode.ENTER);
press(KeyCode.CONTROL).press(KeyCode.C).release(KeyCode.C).release(KeyCode.CONTROL);

// Find nodes
Button button = lookup("#submitButton").query();
Label label = lookup(".error-label").query();

// Assertions
verifyThat("#label", hasText("Success"));
verifyThat("#button", NodeMatchers.isVisible());
verifyThat("#button", NodeMatchers.isDisabled());
```

---

## 7. JMetro

JMetro is a JavaFX theme based on the Microsoft Modern UI (Metro/Fluent Design) style, providing a modern flat appearance.

### 7.1 Maven Coordinates

```xml
<dependency>
    <groupId>org.jfxtras</groupId>
    <artifactId>jmetro</artifactId>
    <version>11.6.16</version>
</dependency>
```

### 7.2 Usage Example

```java
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(url);
        Scene scene = new Scene(loader.load());

        // Apply the JMetro theme (light or dark)
        JMetro jMetro = new JMetro(Style.LIGHT);
        jMetro.setScene(scene);

        stage.setScene(scene);
        stage.show();
    }
}
```

JMetro automatically applies the Modern UI style to all standard JavaFX controls without the need to manually write CSS.

---

## 8. BootstrapFX

BootstrapFX brings Twitter Bootstrap's CSS styles to JavaFX, providing the familiar Bootstrap-style control appearance.

### 8.1 Maven Coordinates

```xml
<dependency>
    <groupId>org.kordamp.bootstrapfx</groupId>
    <artifactId>bootstrapfx-core</artifactId>
    <version>0.4.0</version>
</dependency>
```

### 8.2 Usage Example

```java
public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Scene scene = new Scene(root, 800, 600);

        // Load the BootstrapFX stylesheet
        scene.getStylesheets().add(
            "org/kordamp/bootstrapfx/bootstrapfx.css");

        stage.setScene(scene);
        stage.show();
    }
}
```

```xml
<!-- Use Bootstrap style classes in FXML -->
<Button text="Primary" styleClass="btn,btn-primary"/>
<Button text="Danger" styleClass="btn,btn-danger"/>
<Button text="Success" styleClass="btn,btn-success"/>
<Label text="Warning" styleClass="alert,alert-warning"/>
<Label text="Info" styleClass="badge,badge-info"/>
```

---

## 9. FXGL

FXGL is a 2D game development engine based on JavaFX, providing complete features including game loop, physics engine, animation, AI, particle system, and more.

### 9.1 Maven Coordinates

```xml
<dependency>
    <groupId>com.github.almasb</groupId>
    <artifactId>fxgl</artifactId>
    <version>17.3</version>
</dependency>
```

### 9.2 Usage Example

```java
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.input.KeyCode;
import javafx.scene.shape.Rectangle;

public class SimpleGame extends GameApplication {

    private Entity player;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("Simple Game");
        settings.setWidth(800);
        settings.setHeight(600);
    }

    @Override
    protected void initGame() {
        // Create player entity
        player = FXGL.entityBuilder()
            .at(400, 300)
            .view(new Rectangle(40, 40, javafx.scene.paint.Color.BLUE))
            .buildAndAttach();
    }

    @Override
    protected void initInput() {
        // Keyboard control
        FXGL.onKey(KeyCode.W, () -> player.translateY(-5));
        FXGL.onKey(KeyCode.S, () -> player.translateY(5));
        FXGL.onKey(KeyCode.A, () -> player.translateX(-5));
        FXGL.onKey(KeyCode.D, () -> player.translateX(5));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

---

## 10. Compatibility Matrix

The table below shows the compatibility of each third-party library with JavaFX versions (compiled from public information; please consult the latest documentation before actual use).

| Library      | Maven Version | JavaFX 17 | JavaFX 21 | JavaFX 24+   | Notes                            |
|--------------|---------------|-----------|-----------|--------------|----------------------------------|
| ControlsFX   | 11.2.1        | Compatible | Compatible | Mostly compatible | Mature and stable, widely used |
| MaterialFX   | 11.17.0       | Compatible | Compatible | Needs testing | Requires JavaFX 17+              |
| RichTextFX   | 0.11.3        | Compatible | Compatible | Needs testing | Depends on ReactFX / Flowless    |
| Ikonli       | 12.3.1        | Compatible | Compatible | Compatible   | Pure Java, good compatibility    |
| ValidatorFX  | 0.4.0         | Compatible | Compatible | Needs testing | Lightweight                      |
| TestFX       | 4.0.18        | Compatible | Compatible | Needs testing | Testing framework                |
| JMetro       | 11.6.16       | Compatible | Compatible | Needs testing | Theme library                    |
| BootstrapFX  | 0.4.0         | Compatible | Compatible | Compatible   | Pure CSS, good compatibility     |
| FXGL         | 17.3          | Compatible | Compatible | Needs testing | Game engine, requires JavaFX 17+ |

### Compatibility Notes

1. **JavaFX 24+ `--enable-native-access`**: Some libraries that depend on native code may require additional JVM argument configuration.
2. **Modular conflicts**: Non-modular libraries may cause errors during jlink packaging; handle them via `--add-modules` or by converting the library to an automatic module.
3. **Version pinning**: It is recommended to uniformly pin the JavaFX version and library versions in `pom.xml` to avoid transitive dependency conflicts.
4. **Test verification**: After upgrading the JavaFX version, be sure to regression-test all third-party library functionality.

---

## 11. Library Selection Recommendations

| Requirement Scenario                 | Recommended Library                 |
|--------------------------------------|-------------------------------------|
| Dialogs / notifications / advanced controls | ControlsFX                          |
| Material Design style UI             | MaterialFX                          |
| Code editor / rich text              | RichTextFX                          |
| Font icons                           | Ikonli                              |
| Form validation                      | ValidatorFX or native BooleanBinding |
| UI automation testing                | TestFX                              |
| Modern theme (Metro style)           | JMetro                              |
| Bootstrap style                      | BootstrapFX                         |
| 2D game development                  | FXGL                                |
| Multi-select dropdown                | ControlsFX (CheckComboBox)          |
| Range slider                         | ControlsFX (RangeSlider)            |
| Property editing panel               | ControlsFX (PropertySheet)          |
