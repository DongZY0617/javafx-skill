# CSS Compliance Rules

This document is the criteria for CSS compliance within the "Deep Compliance Audit" dimension, governing 3 check items: `var()` prohibition, literal numeric value rules, and looked-up color usage rules. Default severity baseline: Major. Shares the same origin as `javafx-developer`'s `css-best-practices.md`.

> **Core Difference**: JavaFX CSS is not Web CSS. JavaFX CSS is based on CSS syntax but has many limitations; the most critical is that it does not support the `var()` function. JavaFX implements variable functionality through the "looked-up color" mechanism, where colors defined on `.root` are referenced directly by name by child nodes, without `var()` wrapping.

---

## Check Item 1: var() Prohibition Rule

**Focus**: Whether `var()` is not used (JavaFX CSS does not support it).

**Pass Criteria**:
- No `var()` function calls appear in CSS files
- Color variables use the looked-up color mechanism: define `-fx-xxx-color` in `.root`, child nodes reference directly by name (e.g., `-fx-background-color: -fx-primary-color;`)
- Size values use literal numeric values (e.g., `-fx-background-radius: 8;`), not referenced via `var()`

**Fail Criteria** (any one constitutes failure):
- CSS uses `var(-fx-primary-color)` syntax (JavaFX CSS does not support it, style does not take effect)
- CSS uses `var(-fx-radius)` to reference size variables
- Bringing Web CSS `var()` habits into JavaFX CSS

**Severity Baseline**: Major (unsupported syntax, style does not take effect, cannot be de-escalated)

> **Key Fact**: The JavaFX CSS parser does not recognize the `var()` function. Property declarations using `var()` are silently ignored, and the corresponding styles do not take effect. This is the most common error when migrating from Web CSS to JavaFX CSS.

**Bad Example**:
```css
/* Using var(), JavaFX CSS does not support, style does not take effect */
.root {
    -fx-primary-color: #2196f3;
    -fx-radius: 8;
}
.button-primary {
    -fx-background-color: var(-fx-primary-color);      /* Does not take effect */
    -fx-background-radius: var(-fx-radius);             /* Does not take effect */
    -fx-text-fill: var(-fx-text-color, #333333);        /* Fallback syntax not supported */
}
```

**Good Example**:
```css
/* Directly reference looked-up color, no var() wrapping needed */
.root {
    -fx-primary-color: #2196f3;
    -fx-text-color: #333333;
}
.button-primary {
    -fx-background-color: -fx-primary-color;   /* Direct reference by name */
    -fx-background-radius: 8;                   /* Literal numeric value */
    -fx-text-fill: -fx-text-color;              /* Direct reference by name */
}
```

---

## Check Item 2: Literal Numeric Value Rule

**Focus**: Whether size properties such as border radius use literal numeric values, rather than looked-up color references to size variables.

**Pass Criteria**:
- Size properties such as `-fx-background-radius`, `-fx-border-radius`, `-fx-padding` use literal numeric values (e.g., `8`, `4px`, `10 5 10 5`)
- Size values are not referenced via looked-up color variables (looked-up colors are primarily used for color values)
- Literal numeric values are consistent across multiple uses, or documented via CSS comments

**Fail Criteria** (any one constitutes failure):
- `-fx-background-radius: -fx-radius;` (referencing a size variable via looked-up color, unreliable in JavaFX)
- `-fx-padding: -fx-spacing;` (size property referencing a looked-up color variable)
- Size property values use `var()` references (also violates Check Item 1)

**Severity Baseline**: Major
- De-escalation condition: Only individual size properties misuse looked-up color references, does not affect overall layout → Minor

> **Key Fact**: The looked-up color mechanism in JavaFX is primarily used for **color** values. Using looked-up colors directly for size properties such as `-fx-background-radius` and `-fx-border-radius` is unreliable in JavaFX and may not be parsed or may be parsed to incorrect values. Size properties should use literal numeric values.

**Bad Example**:
```css
/* Size properties referenced via looked-up color, unreliable */
.root {
    -fx-radius: 8;
    -fx-spacing: 10;
}
.card {
    -fx-background-radius: -fx-radius;    /* Unreliable, may not take effect */
    -fx-border-radius: -fx-radius;        /* Unreliable */
    -fx-padding: -fx-spacing;             /* Unreliable */
}
```

**Good Example**:
```css
/* Size properties use literal numeric values */
.root {
    -fx-primary-color: #2196f3;  /* Looked-up color only for colors */
}
.card {
    -fx-background-color: -fx-primary-color;  /* Color uses looked-up color */
    -fx-background-radius: 8;                  /* Size uses literal */
    -fx-border-radius: 8;                      /* Size uses literal */
    -fx-padding: 10;                           /* Size uses literal */
}
```

---

## Check Item 3: Looked-up Color Usage Rule

**Focus**: Whether looked-up colors are defined in `.root` and referenced directly by name by child nodes, whether the scope is correct.

**Pass Criteria**:
- Looked-up colors are defined in `.root` with the `-fx-` prefix (e.g., `-fx-primary-color: #2196f3;`)
- Child nodes reference looked-up colors directly by name (e.g., `-fx-background-color: -fx-primary-color;`), without `var()` wrapping
- Theme switching is achieved by replacing looked-up color definitions on `.root` (or switching different CSS files)
- When locally overriding looked-up colors, redefine on a specific node, affecting only that node and its children

**Fail Criteria** (any one constitutes failure):
- Looked-up color referenced by a child node without being defined in `.root` (undefined looked-up color falls back to default value)
- Looked-up color definition does not start with the `-fx-` prefix (e.g., `primary-color` instead of `-fx-primary-color`, may not be recognized)
- Color values use Web CSS syntax instead of JavaFX-supported formats (e.g., using `rgb()` without spaces)
- Theme switching achieved by modifying node styles one by one, rather than replacing `.root` looked-up color definitions

**Severity Baseline**: Major
- De-escalation condition: Only individual looked-up color definitions are non-standard but functionality is normal → Minor

**Bad Example**:
```css
/* Looked-up color referenced without being defined in .root */
.button {
    -fx-background-color: -fx-primary-color;  /* -fx-primary-color is undefined, falls back to default */
}

/* Looked-up color definition without -fx- prefix */
.root {
    primary-color: #2196f3;  /* No -fx- prefix, may not be recognized as a looked-up color */
}

/* Theme switching modifies nodes one by one, rather than replacing .root definition */
/* In JS/Java: button1.setStyle("-fx-background-color: #ff0000;"); */
/* button2.setStyle("-fx-background-color: #ff0000;"); */
/* Should instead switch looked-up color definitions on .root */
```

**Good Example**:
```css
/* Looked-up colors defined in .root, child nodes reference directly */
.root {
    -fx-primary-color: #2196f3;
    -fx-accent-color: #ff9800;
    -fx-bg-color: #ffffff;
    -fx-text-color: #333333;
}

.button-primary {
    -fx-background-color: -fx-primary-color;  /* Direct reference */
    -fx-text-fill: white;
}

.label-title {
    -fx-text-fill: -fx-text-color;  /* Direct reference */
}

/* Theme switching: switch looked-up color definitions on .root */
/* dark-theme.css */
.root {
    -fx-primary-color: #1565c0;
    -fx-bg-color: #1e1e1e;
    -fx-text-color: #e0e0e0;
}
/* In Java: scene.getStylesheets().setAll("/css/dark-theme.css"); */
```
