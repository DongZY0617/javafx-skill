# JavaFX CSS Best Practices Guide

This guide covers JavaFX CSS syntax features, variables and theme architecture, selector specificity, common control styling, responsive layout, animations and transitions, the `derive()` function, Scene Builder integration, and runtime theme switching.

---

## 1. Syntax Differences Between JavaFX CSS and Web CSS

JavaFX CSS is based on CSS syntax, but has several key differences. Understanding these differences is a prerequisite for correct styling.

| Feature              | Web CSS                          | JavaFX CSS                                    |
|----------------------|----------------------------------|-----------------------------------------------|
| Property prefix      | No prefix (e.g., `color`)        | Most properties use the `-fx-` prefix (e.g., `-fx-text-fill`) |
| Color properties     | `color` (text), `background-color` | `-fx-text-fill` (text), `-fx-background-color` |
| Size units           | Supports `px`, `em`, `rem`, `%`, etc. | Supports `px`, `em`, `pt`, but not `rem`      |
| Selector types       | Tag, class, ID, attribute, pseudo-class | Type selectors use class names (e.g., `.button`), ID, pseudo-class |
| Pseudo-classes       | `:hover`, `:focus`, etc.         | `:hover`, `:focused`, `:pressed`, `:armed`, etc. |
| Variables (custom properties) | `--var` + `var()`        | `-fx-var` (JavaFX 17+), or set via code       |
| Layout properties    | `display`, `flex`, `grid`        | JavaFX layout is managed by Layout containers; CSS only controls appearance |
| Functions            | `calc()`, `rgb()`, etc.          | `derive()`, `ladder()`, and other JavaFX-specific functions |
| Inheritance          | Some properties inherit          | Properties like `-fx-font-*` can inherit      |

### 1.1 Type Selector Notes

In Web CSS, `button` selects the tag name, whereas in JavaFX CSS, type selectors use the control's style class name (all lowercase), for example:

```css
/* In JavaFX, .button is equivalent to matching all Button controls */
.button {
    -fx-background-color: #4a90d9;
    -fx-text-fill: white;
}
```

### 1.2 Common JavaFX CSS Property Mapping

| Purpose        | Web CSS Property     | JavaFX CSS Property         |
|----------------|----------------------|-----------------------------|
| Text color     | `color`              | `-fx-text-fill`             |
| Background color | `background-color`  | `-fx-background-color`      |
| Font size      | `font-size`          | `-fx-font-size`             |
| Font weight    | `font-weight`        | `-fx-font-weight`           |
| Padding        | `padding`            | `-fx-padding`               |
| Border radius  | `border-radius`      | `-fx-background-radius`     |
| Border         | `border`             | `-fx-border-color` / `-fx-border-width` |
| Cursor         | `cursor`             | `-fx-cursor`                |
| Opacity        | `opacity`            | `-fx-opacity` / `opacity`   |

---

## 2. CSS Variables (Custom Properties)

JavaFX 17+ supports CSS custom properties (variables), defined with the `-fx-` prefix and referenced via `var()`. This makes centralized management of theme colors possible.

### 2.1 Definition and Usage

```css
.root {
    /* Define theme variables */
    -fx-primary-color: #2196f3;
    -fx-accent-color: #ff9800;
    -fx-bg-color: #ffffff;
    -fx-text-color: #333333;
    -fx-radius: 8;
}

/* Reference variables */
.button-primary {
    -fx-background-color: var(-fx-primary-color);
    -fx-text-fill: white;
    -fx-background-radius: var(-fx-radius);
}
```

### 2.2 Variable Scope

- Variables defined on `.root` have global scope and are accessible by all nodes.
- Redefining a variable on a specific node overrides the global value, affecting only that node and its children.

```css
.custom-pane {
    /* Only this pane and its children use a different primary color */
    -fx-primary-color: #e91e63;
}
```

---

## 3. Theme Variable Architecture (Light / Dark)

Build a complete theme system through CSS variables, enabling unified management of light and dark themes.

### 3.1 Light Theme Variable Definition

```css
/* light-theme.css */
.root {
    /* Background colors */
    -fx-bg-primary: #ffffff;
    -fx-bg-secondary: #f5f5f5;
    -fx-bg-tertiary: #e0e0e0;

    /* Text colors */
    -fx-text-primary: #212121;
    -fx-text-secondary: #757575;
    -fx-text-disabled: #bdbdbd;

    /* Accent colors */
    -fx-accent: #2196f3;
    -fx-accent-hover: derive(-fx-accent, -15%);
    -fx-accent-pressed: derive(-fx-accent, -25%);

    /* Status colors */
    -fx-success: #4caf50;
    -fx-warning: #ff9800;
    -fx-danger: #f44336;

    /* Borders and dividers */
    -fx-border-color: #e0e0e0;
    -fx-divider-color: #eeeeee;

    /* Radius and spacing */
    -fx-radius-sm: 4;
    -fx-radius-md: 8;
    -fx-radius-lg: 12;
}
```

### 3.2 Dark Theme Variable Definition

```css
/* dark-theme.css */
.root {
    -fx-bg-primary: #1e1e1e;
    -fx-bg-secondary: #252525;
    -fx-bg-tertiary: #333333;

    -fx-text-primary: #ffffff;
    -fx-text-secondary: #b0b0b0;
    -fx-text-disabled: #666666;

    -fx-accent: #64b5f6;
    -fx-accent-hover: derive(-fx-accent, 15%);
    -fx-accent-pressed: derive(-fx-accent, 25%);

    -fx-success: #66bb6a;
    -fx-warning: #ffa726;
    -fx-danger: #ef5350;

    -fx-border-color: #444444;
    -fx-divider-color: #383838;

    -fx-radius-sm: 4;
    -fx-radius-md: 8;
    -fx-radius-lg: 12;
}
```

### 3.3 Control Styles Referencing Theme Variables

```css
.button {
    -fx-background-color: var(-fx-bg-secondary);
    -fx-text-fill: var(-fx-text-primary);
    -fx-background-radius: var(-fx-radius-md);
    -fx-padding: 8 16 8 16;
}

.button:hover {
    -fx-background-color: var(-fx-bg-tertiary);
}

.button-primary {
    -fx-background-color: var(-fx-accent);
    -fx-text-fill: white;
}

.button-primary:hover {
    -fx-background-color: var(-fx-accent-hover);
}

.text-field {
    -fx-background-color: var(-fx-bg-primary);
    -fx-text-fill: var(-fx-text-primary);
    -fx-border-color: var(-fx-border-color);
    -fx-border-width: 1;
    -fx-background-radius: var(-fx-radius-sm);
}

.label {
    -fx-text-fill: var(-fx-text-primary);
}
```

---

## 4. Selector Priority and Specificity

JavaFX CSS follows specificity rules similar to Web CSS. Priority from highest to lowest:

1. **Inline styles** (set via `setStyle()`) - highest priority
2. **ID selectors** (`#myId`)
3. **Style class + pseudo-class selectors** (`.button:hover`)
4. **Style class selectors** (`.button`)
5. **Type selectors** (`.button` matches all Buttons)

### 4.1 Priority Example

```css
/* Type selector: lowest priority */
.button {
    -fx-background-color: gray;
}

/* Style class selector: higher priority than type selector */
.danger-button {
    -fx-background-color: red;
}

/* ID selector: highest priority */
#saveButton {
    -fx-background-color: green;
}
```

```java
// Inline style: highest priority, overrides all CSS rules
saveButton.setStyle("-fx-background-color: blue;");
```

### 4.2 !important

JavaFX CSS does not support `!important`. To force an override, use a higher-priority selector or inline style.

### 4.3 Multiple Style Class Stacking

A node can have multiple style classes; classes added later that are also defined later in CSS take priority:

```java
button.getStyleClass().addAll("button", "primary", "large");
```

---

## 5. Common Control Styles

### 5.1 Button

```css
.button {
    -fx-background-color: var(-fx-accent);
    -fx-text-fill: white;
    -fx-font-size: 14px;
    -fx-font-weight: bold;
    -fx-padding: 8 20 8 20;
    -fx-background-radius: 6;
    -fx-border-width: 0;
    -fx-cursor: hand;
}

.button:hover {
    -fx-background-color: var(-fx-accent-hover);
}

.button:pressed {
    -fx-background-color: var(-fx-accent-pressed);
}

.button:disabled {
    -fx-opacity: 0.5;
    -fx-cursor: default;
}

/* Outlined secondary button */
.button-outline {
    -fx-background-color: transparent;
    -fx-border-color: var(-fx-accent);
    -fx-border-width: 1.5;
    -fx-border-radius: 6;
    -fx-text-fill: var(-fx-accent);
}
```

### 5.2 TextField / PasswordField

```css
.text-field {
    -fx-background-color: var(-fx-bg-primary);
    -fx-text-fill: var(-fx-text-primary);
    -fx-prompt-text-fill: var(-fx-text-disabled);
    -fx-border-color: var(-fx-border-color);
    -fx-border-width: 1;
    -fx-border-radius: 4;
    -fx-background-radius: 4;
    -fx-padding: 8 10 8 10;
    -fx-font-size: 14px;
}

.text-field:focused {
    -fx-border-color: var(-fx-accent);
    -fx-border-width: 2;
}

.text-field.error {
    -fx-border-color: var(-fx-danger);
}
```

### 5.3 TableView

```css
.table-view {
    -fx-background-color: var(--fx-bg-primary);
    -fx-border-color: var(--fx-border-color);
    -fx-border-width: 1;
}

/* Table header */
.table-view .column-header {
    -fx-background-color: var(--fx-bg-secondary);
    -fx-border-color: var(--fx-divider-color);
}

.table-view .column-header .label {
    -fx-text-fill: var(--fx-text-primary);
    -fx-font-weight: bold;
}

/* Rows */
.table-row-cell {
    -fx-background-color: var(--fx-bg-primary);
    -fx-border-color: var(--fx-divider-color);
}

.table-row-cell:odd {
    -fx-background-color: var(--fx-bg-secondary);
}

.table-row-cell:selected {
    -fx-background-color: var(--fx-accent);
}

.table-row-cell:selected .text {
    -fx-fill: white;
}

/* Empty table placeholder */
.table-view .placeholder .label {
    -fx-text-fill: var(--fx-text-secondary);
}
```

### 5.4 ListView

```css
.list-view {
    -fx-background-color: var(-fx-bg-primary);
    -fx-border-color: var(--fx-border-color);
    -fx-border-width: 1;
    -fx-background-radius: 4;
}

.list-cell {
    -fx-background-color: transparent;
    -fx-text-fill: var(-fx-text-primary);
    -fx-padding: 8 12 8 12;
}

.list-cell:filled:hover {
    -fx-background-color: var(-fx-bg-tertiary);
}

.list-cell:filled:selected {
    -fx-background-color: var(-fx-accent);
    -fx-text-fill: white;
}
```

### 5.5 ComboBox

```css
.combo-box {
    -fx-background-color: var(-fx-bg-primary);
    -fx-border-color: var(--fx-border-color);
    -fx-border-radius: 4;
    -fx-background-radius: 4;
}

.combo-box .arrow-button {
    -fx-background-color: var(-fx-bg-secondary);
}

.combo-box .list-cell {
    -fx-text-fill: var(-fx-text-primary);
}
```

### 5.6 ScrollBar

```css
.scroll-bar {
    -fx-background-color: transparent;
}

.scroll-bar .thumb {
    -fx-background-color: var(-fx-text-disabled);
    -fx-background-radius: 4;
}

.scroll-bar .thumb:hover {
    -fx-background-color: var(-fx-text-secondary);
}

.scroll-bar .increment-button,
.scroll-bar .decrement-button {
    -fx-background-color: transparent;
    -fx-padding: 0;
}
```

### 5.7 MenuBar / MenuItem

```css
.menu-bar {
    -fx-background-color: var(-fx-bg-secondary);
    -fx-border-color: var(--fx-divider-color);
    -fx-border-width: 0 0 1 0;
}

.menu-bar .label {
    -fx-text-fill: var(-fx-text-primary);
}

.menu-item .label {
    -fx-text-fill: var(-fx-text-primary);
}

.menu-item:focused {
    -fx-background-color: var(-fx-accent);
}

.menu-item:focused .label {
    -fx-text-fill: white;
}
```

---

## 6. Responsive Layout Techniques

JavaFX does not have media queries like the Web, but responsive effects can be achieved through the following methods.

### 6.1 Using Layout Containers for Auto-resizing

```java
// HBox / VBox hgrow / vgrow properties enable flexible layout
HBox.setHgrow(textField, Priority.ALWAYS);
VBox.setVgrow(tableView, Priority.ALWAYS);
```

### 6.2 Listening to Window Size to Dynamically Switch Styles

```java
scene.widthProperty().addListener((obs, oldVal, newVal) -> {
    double width = newVal.doubleValue();
    ObservableList<String> classes = root.getStyleClass();
    classes.removeAll("layout-small", "layout-medium", "layout-large");
    if (width < 600) {
        classes.add("layout-small");
    } else if (width < 1000) {
        classes.add("layout-medium");
    } else {
        classes.add("layout-large");
    }
});
```

```css
/* Small screen layout: single column, compact */
.layout-small .sidebar {
    -fx-pref-width: 0;
    -fx-max-width: 0;
}

.layout-small .content {
    -fx-font-size: 12px;
}

/* Large screen layout: wide sidebar */
.layout-large .sidebar {
    -fx-pref-width: 250;
}

.layout-large .content {
    -fx-font-size: 14px;
}
```

### 6.3 Using Stage Maximize/Fullscreen Listeners

```java
stage.maximizedProperty().addListener((obs, old, isMax) -> {
    root.setStyle(isMax ? "-fx-font-size: 16px;" : "-fx-font-size: 14px;");
});
```

---

## 7. Animations and Transitions

JavaFX CSS supports animations through the `Transition` API, and can also use `-fx-transition`-style transitions in CSS (needs to be combined with code).

### 7.1 Transitions Cannot Be Defined Directly in CSS

Unlike Web CSS's `transition`, JavaFX CSS does not support defining transition animations directly in stylesheets. Animations must be implemented through Java code's `Transition` class.

### 7.2 Common Animation Code Examples

```java
// Fade in/out
FadeTransition fade = new FadeTransition(Duration.millis(300), button);
fade.setFromValue(0.0);
fade.setToValue(1.0);
fade.play();

// Scale transition (hover effect)
ScaleTransition scale = new ScaleTransition(Duration.millis(200), button);
scale.setToX(1.05);
scale.setToY(1.05);
scale.setAutoReverse(true);
scale.setCycleCount(2);

button.setOnMouseEntered(e -> {
    scale.setFromX(1.0); scale.setFromY(1.0);
    scale.setToX(1.05); scale.setToY(1.05);
    scale.playFromStart();
});

button.setOnMouseExited(e -> {
    scale.setFromX(1.05); scale.setFromY(1.05);
    scale.setToX(1.0); scale.setToY(1.0);
    scale.playFromStart();
});

// Translate
TranslateTransition move = new TranslateTransition(Duration.millis(500), node);
move.setToX(100);
move.play();

// Rotate
RotateTransition rotate = new RotateTransition(Duration.millis(1000), node);
rotate.setByAngle(360);
rotate.setCycleCount(Animation.INDEFINITE);
rotate.play();
```

### 7.3 Composite Animations

```java
// Parallel animation: execute multiple animations simultaneously
ParallelTransition parallel = new ParallelTransition(
    new FadeTransition(Duration.millis(400), node),
    new ScaleTransition(Duration.millis(400), node)
);
parallel.play();

// Sequential animation: execute one after another
SequentialTransition sequential = new SequentialTransition(
    new PauseTransition(Duration.millis(500)),
    new FadeTransition(Duration.millis(300), node)
);
sequential.play();
```

### 7.4 Triggering Animation States via CSS Pseudo-classes

```css
.card {
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);
}

.card:hover {
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0, 0, 5);
}
```

Combined with `Transition` in code to achieve smooth transitions.

---

## 8. The derive() Function

`derive()` is a JavaFX CSS-specific color function used to generate lighter or darker variants based on a base color, ideal for building color schemes.

### 8.1 Syntax

```css
derive(<color>, <percentage>)
```

- Positive percentage: color becomes lighter (mixes toward white).
- Negative percentage: color becomes darker (mixes toward black).
- Range: `-100%` (pure black) to `100%` (pure white).

### 8.2 Usage Example

```css
.root {
    -fx-base-color: #2196f3;
}

.button {
    -fx-background-color: derive(-fx-base-color, 0%);    /* Original color */
}

.button:hover {
    -fx-background-color: derive(-fx-base-color, 15%);   /* Lighten by 15% */
}

.button:pressed {
    -fx-background-color: derive(-fx-base-color, -20%);  /* Darken by 20% */
}

.button:disabled {
    -fx-background-color: derive(-fx-base-color, 60%);   /* Significantly lightened, pale color */
}
```

### 8.3 The ladder() Function

`ladder()` automatically selects a foreground color based on background brightness, achieving adaptive text color:

```css
.label {
    /* Based on background brightness, choose text color between white and black */
    -fx-text-fill: ladder(-fx-background-color,
        white 49%,
        black 50%);
}
```

Meaning: when the background brightness is below 49%, white text is used; above 50%, black text is used.

---

## 9. Scene Builder Integration

Scene Builder is the official visual FXML design tool, supporting real-time preview of CSS styles.

### 9.1 Loading CSS Files

1. Open the FXML file in Scene Builder.
2. In the left **Documents** panel, select **Controller** or select the root node.
3. In the right **Properties** panel, find the **Stylesheets** property.
4. Click `+` to add a CSS file (the CSS file must be placed in the resources directory first).

### 9.2 Real-time Preview

After loading CSS, Scene Builder renders the style effects in real time, making visual adjustment convenient.

### 9.3 Using Style Classes

1. Select a control.
2. In the **Properties** panel, find the **Style Class** field.
3. Enter the style class name (e.g., `button-primary`); separate multiple classes with spaces.

### 9.4 Inline Style Debugging

Enter inline CSS in the **Style** field of the **Properties** panel for quick testing:

```
-fx-background-color: #ff0000; -fx-text-fill: white;
```

### 9.5 Recommended Workflow

1. Build the FXML layout in Scene Builder and assign style class names.
2. Write the CSS file in an external editor (IDE).
3. Return to Scene Builder to load the CSS and preview effects.
4. Iteratively adjust CSS and layout.

---

## 10. Runtime Theme Switching

Implement dynamic switching between light / dark themes without restarting the application.

### 10.1 Preparing Theme CSS Files

```
resources/
|-- css/
|   |-- light-theme.css   # Light theme variables
|   |-- dark-theme.css    # Dark theme variables
|   |-- controls.css      # Control styles (reference variables, theme-agnostic)
```

### 10.2 Theme Switching Code

```java
package com.example.theme;

import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;

public class ThemeManager {

    private static final String LIGHT_THEME = "/css/light-theme.css";
    private static final String DARK_THEME = "/css/dark-theme.css";
    private static final String CONTROLS = "/css/controls.css";

    private final Scene scene;
    private boolean darkMode = false;

    public ThemeManager(Scene scene) {
        this.scene = scene;
        // Initially load light theme + control styles
        applyTheme(false);
    }

    /** Switch theme */
    public void toggleTheme() {
        darkMode = !darkMode;
        applyTheme(darkMode);
    }

    private void applyTheme(boolean dark) {
        scene.getStylesheets().clear();
        // Load theme variable file first, then control styles
        scene.getStylesheets().add(
            getClass().getResource(dark ? DARK_THEME : LIGHT_THEME).toExternalForm());
        scene.getStylesheets().add(
            getClass().getResource(CONTROLS).toExternalForm());
    }

    public boolean isDarkMode() {
        return darkMode;
    }
}
```

### 10.3 Binding the Toggle Button in the Controller

```java
public class MainController implements Initializable {

    @FXML private ToggleButton themeToggle;
    private ThemeManager themeManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        themeManager = new ThemeManager(themeToggle.getScene());
        themeToggle.setText("Switch to dark theme");
        themeToggle.setOnAction(e -> {
            themeManager.toggleTheme();
            themeToggle.setText(themeManager.isDarkMode()
                ? "Switch to light theme" : "Switch to dark theme");
        });
    }
}
```

### 10.4 Persisting User Theme Preference

```java
public class ThemeManager {
    private static final String PREF_KEY = "app.theme.dark";

    public ThemeManager(Scene scene) {
        this.scene = scene;
        // Read last selection from Preferences
        darkMode = Preferences.userNodeForPackage(ThemeManager.class)
            .getBoolean(PREF_KEY, false);
        applyTheme(darkMode);
    }

    public void toggleTheme() {
        darkMode = !darkMode;
        applyTheme(darkMode);
        // Persist save
        Preferences.userNodeForPackage(ThemeManager.class)
            .putBoolean(PREF_KEY, darkMode);
    }
}
```

### 10.5 Smooth Transition (Optional)

Add a fade effect to the root node when switching themes:

```java
private void applyThemeWithTransition(boolean dark) {
    FadeTransition fadeOut = new FadeTransition(Duration.millis(150), scene.getRoot());
    fadeOut.setFromValue(1.0);
    fadeOut.setToValue(0.3);
    fadeOut.setOnFinished(e -> {
        applyTheme(dark);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), scene.getRoot());
        fadeIn.setFromValue(0.3);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    });
    fadeOut.play();
}
```

---

## 11. CSS Best Practices Summary

| Practice                              | Description                                                          |
|---------------------------------------|----------------------------------------------------------------------|
| Use CSS variables to manage theme colors | Define colors centrally in `.root` for easy modification and theme switching. |
| Avoid inline styles                   | Inline styles have the highest priority and are hard to maintain; use only for dynamic debugging. |
| Semantic style class names            | Use `.button-primary` instead of `.blue-button`, decoupling from visuals. |
| Separate theme variables from control styles | Theme files only define variables; control files reference variables, making themes pluggable. |
| Use derive() to build color schemes   | Derive hover/pressed state colors from the base color to maintain color consistency. |
| Avoid overly nested selectors         | Deep selector hierarchies reduce performance and are hard to override; keep within 2-3 levels. |
| Provide styles for all interaction states | Including `:hover`, `:pressed`, `:focused`, `:disabled`.          |
| Use Scene Builder for visual debugging | Real-time preview improves style development efficiency.             |
| Manage CSS with comment sections      | Divide blocks with comments (buttons, tables, forms, etc.) for maintainability. |
