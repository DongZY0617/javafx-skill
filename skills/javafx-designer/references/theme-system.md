# CSS Theme Design System

This document defines the design-token system and component style patterns used to generate JavaFX CSS themes. It covers color palettes, CSS variable naming, typography, spacing, border radius, component patterns, and light/dark theme generation rules.

## Color Palette Definition

A palette is composed of brand colors, surface colors, text colors, borders, and semantic colors. Each token has a base value plus derived variants.

### Brand Colors
| Token | Purpose | Derivation |
|-------|---------|-----------|
| Primary | Buttons, links, focus rings, active states | User brand color |
| Primary hover | Hovered primary controls | Base lightened 8% |
| Primary pressed | Pressed primary controls | Base darkened 8% |
| Secondary | Secondary buttons, toggles | Complement or neutral grey |
| Accent | Highlights, badges | Distinct hue from primary |

### Semantic Colors
| Token | Meaning | Light value | Dark value |
|-------|---------|-------------|-----------|
| Success | Confirmations, valid states | `#2e7d32` | `#4caf50` |
| Warning | Cautions, pending states | `#ed6c02` | `#ffb74d` |
| Danger | Errors, destructive actions | `#d32f2f` | `#ef5350` |
| Info | Informational banners | `#0288d1` | `#29b6f6` |

### Deriving Dark/Light Variants
- **Lighten**: increase lightness toward white for hover states.
- **Darken**: decrease lightness toward black for pressed states.
- **Invert surfaces**: light theme uses white/near-white backgrounds; dark theme uses `#1e1e1e` base and `#2d2d2d` elevated surfaces.
- **Preserve contrast**: text must reach WCAG AA (4.5:1) against its surface in both themes.

## CSS Variable Naming Convention

All tokens are declared as CSS variables in the `.root` selector so components never hardcode values. Names use the `-fx-` prefix followed by a category and a role.

| Category | Naming Pattern | Examples |
|----------|---------------|---------|
| Brand | `-fx-primary`, `-fx-primary-hover`, `-fx-secondary`, `-fx-accent` | `-fx-primary: #1976d2;` |
| Background | `-fx-bg-base`, `-fx-bg-elevated`, `-fx-bg-hover` | `-fx-bg-base: #ffffff;` |
| Text | `-fx-text-primary`, `-fx-text-secondary`, `-fx-text-disabled` | `-fx-text-primary: #212121;` |
| Border | `-fx-border-color`, `-fx-border-subtle` | `-fx-border-color: #e0e0e0;` |
| Semantic | `-fx-success`, `-fx-warning`, `-fx-danger`, `-fx-info` | `-fx-danger: #d32f2f;` |
| Spacing | `-fx-spacing-xs` ... `-fx-spacing-xl` | `-fx-spacing-lg: 16px;` |
| Radius | `-fx-radius-sm`, `-fx-radius-md`, `-fx-radius-lg` | `-fx-radius-md: 6px;` |
| Typography | `-fx-font-family`, `-fx-font-size-body`, `-fx-font-weight-bold` | `-fx-font-size-body: 13px;` |

```css
.root {
    -fx-primary: #1976d2;
    -fx-bg-base: #ffffff;
    -fx-text-primary: #212121;
    -fx-border-color: #e0e0e0;
}
```

## Typography Scale

| Token | Value | Usage |
|-------|-------|-------|
| `-fx-font-family` | `"Segoe UI", "San Francisco", "Inter", system-ui` | Global default |
| `-fx-font-size-caption` | `12px` | Helper text, table cell secondary |
| `-fx-font-size-body` | `13px` | Default body text |
| `-fx-font-size-subtitle` | `15px` | Section subtitles |
| `-fx-font-size-title` | `18px` | Dialog/screen titles |
| `-fx-font-size-headline` | `24px` | Dashboard headlines |
| `-fx-font-weight-normal` | `Normal` (400) | Body text |
| `-fx-font-weight-medium` | `500` | Labels, menu items |
| `-fx-font-weight-bold` | `Bold` (700) | Titles, emphasis |

## Spacing System

Consistent spacing creates visual rhythm. Use these values for padding, margins, and gaps.

| Token | Value | When to Use |
|-------|-------|-------------|
| `-fx-spacing-xs` | `4px` | Tight gaps between icon and label |
| `-fx-spacing-sm` | `8px` | Default gaps inside a control group |
| `-fx-spacing-md` | `12px` | Spacing between form rows |
| `-fx-spacing-lg` | `16px` | Padding around content areas |
| `-fx-spacing-xl` | `24px` | Major section separation |

## Border Radius System

| Token | Value | Usage |
|-------|-------|-------|
| `-fx-radius-sm` | `4px` | Small controls: chips, tags |
| `-fx-radius-md` | `6px` | Buttons, text fields, combo boxes |
| `-fx-radius-lg` | `8px` | Cards, dialogs, panels |

## Component Style Patterns

Each component class references tokens, never literal values.

### Buttons
```css
.button-primary {
    -fx-background-color: -fx-primary;
    -fx-text-fill: -fx-text-on-primary;
    -fx-background-radius: -fx-radius-md;
    -fx-padding: 8px 16px;
}
.button-primary:hover { -fx-background-color: -fx-primary-hover; }
.button-secondary {
    -fx-background-color: -fx-bg-elevated;
    -fx-text-fill: -fx-text-primary;
    -fx-border-color: -fx-border-color;
    -fx-background-radius: -fx-radius-md;
}
.button-danger {
    -fx-background-color: -fx-danger;
    -fx-text-fill: -fx-text-on-primary;
}
```

### Text Fields, Labels, Tables, Menus, Scroll Bars, Dialogs
| Component | styleClass | Key Properties |
|-----------|-----------|----------------|
| Text field | `.text-field` | `border-color`, `radius-md`, focus ring uses `-fx-primary` |
| Label | `.title-label` / `.caption-text` | `font-size` token, `text-fill` token |
| Table | `.data-table` | `cell` padding `spacing-sm`, header `-fx-bg-elevated` |
| Menu bar | `.app-menubar` | `background` base, hover `-fx-bg-hover` |
| Scroll bar | `.scroll-bar` | Thumb `-fx-border-subtle`, track transparent |
| Dialog | `.dialog-pane` | `radius-lg`, `padding spacing-xl` |

## Light Theme Generation Rules

1. **Backgrounds**: base `#ffffff`, elevated `#f5f5f5`, hover `#eeeeee`.
2. **Text**: primary `#212121`, secondary `#757575`, disabled `#bdbdbd`.
3. **Borders**: subtle `#e0e0e0`, strong `#9e9e9e`.
4. **On-primary text**: white (`#ffffff`) so text reads on colored buttons.
5. **Shadows**: light drop shadow `derive(#000000, -85%)` for elevation cues.

```css
.root {
    -fx-bg-base: #ffffff;
    -fx-bg-elevated: #f5f5f5;
    -fx-text-primary: #212121;
    -fx-text-secondary: #757575;
    -fx-border-color: #e0e0e0;
}
```

## Dark Theme Generation Rules

1. **Backgrounds**: base `#1e1e1e`, elevated `#2d2d2d`, hover `#383838`.
2. **Text**: primary `#e0e0e0`, secondary `#a0a0a0`, disabled `#5a5a5a`.
3. **Borders**: subtle `#3a3a3a`, strong `#5a5a5a`.
4. **Semantic colors** lightened (see palette table) to glow against dark surfaces.
5. **On-primary text**: white or near-white depending on primary luminance.

```css
.root {
    -fx-bg-base: #1e1e1e;
    -fx-bg-elevated: #2d2d2d;
    -fx-text-primary: #e0e0e0;
    -fx-text-secondary: #a0a0a0;
    -fx-border-color: #3a3a3a;
}
```

The dark theme must be **structurally identical** to the light theme — same selectors, same variable names, only the variable values differ.

## Theme Switching Implementation

Themes are applied at runtime by manipulating the scene's stylesheet list. Store both files in `src/main/resources/css/` and swap the single active stylesheet.

```java
public class ThemeManager {
    private static final String LIGHT = "/css/light-theme.css";
    private static final String DARK  = "/css/dark-theme.css";

    public static void apply(Scene scene, boolean darkMode) {
        scene.getStylesheets().clear();
        String path = darkMode ? DARK : LIGHT;
        scene.getStylesheets().add(
            ThemeManager.class.getResource(path).toExternalForm());
    }

    public static void toggle(Scene scene) {
        boolean dark = scene.getStylesheets().stream()
            .anyMatch(s -> s.contains("dark-theme"));
        apply(scene, !dark);
    }
}
```

Toggle from a menu item:
```java
darkModeMenuItem.setOnAction(e -> ThemeManager.toggle(scene));
```

Because both themes define the same variables, switching is instant and no control reconfiguration is needed.
