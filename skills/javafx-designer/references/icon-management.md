# Ikonli Icon Library Management

This document defines how to select, configure, and apply Ikonli icon fonts in JavaFX applications. It covers the library overview, pack comparison, Maven configuration, literal format, mapping workflow, FXML and Java usage, sizing and coloring, the icon config JSON schema, and a common mappings reference table.

## Ikonli Overview

Ikonli is a JavaFX icon library by Kordamp that renders vector icons through font glyphs. It provides the `FontIcon` control, which behaves like a `Label` but draws a single glyph from a bundled icon font. Because icons are font-based, they scale crisply at any size and inherit color through CSS, with no image assets required.

### How It Works
1. An icon font (`.ttf`) is bundled inside an icon pack JAR.
2. Each glyph maps to a Unicode code point (e.g., `\ue14d`).
3. `FontIcon` takes an icon literal (`prefix-name`) and resolves it to the correct glyph and font.
4. Size and color are controlled via CSS properties (`-fx-icon-size`, `-fx-icon-color`).

## Icon Pack Comparison

Choose one pack per project for visual consistency. Mixing packs produces inconsistent stroke weights and styles.

| Pack | Maven Artifact | Prefix | Style | Coverage | Best For |
|------|----------------|--------|-------|----------|----------|
| Material Design | `ikonli-materialdesign-pack` | `mdal-` (Material Design) / `mdi-` | Solid + outline, modern | 4000+ icons | General desktop apps, Material-style UIs |
| Font Awesome | `ikonli-fontawesome-pack` | `fa-` (solid) / `far-` (regular) | Solid + regular, widely recognized | 1500+ icons | Web-familiar UIs, broad coverage |
| Feather | `ikonli-feather-pack` | `fi-` | Minimal line icons, 1px stroke | 280+ icons | Clean, lightweight dashboards |
| Bootstrap Icons | `ikonli-bootstrapicons-pack` | `bi-` | Bootstrap set, balanced | 1800+ icons | Bootstrap-styled apps |

Selection guidance: prefer Material Design for comprehensive desktop apps; Feather for minimalist designs; Font Awesome when users expect web-familiar icons; Bootstrap for Bootstrap-themed UIs.

## Maven Dependency Configuration

Add the core `ikonli-javafx` module plus exactly one icon pack. Use version **12.3.1** for both.

```xml
<!-- Core JavaFX integration -->
<dependency>
    <groupId>org.kordamp.ikonli</groupId>
    <artifactId>ikonli-javafx</artifactId>
    <version>12.3.1</version>
</dependency>

<!-- Icon pack: choose one -->
<dependency>
    <groupId>org.kordamp.ikonli</groupId>
    <artifactId>ikonli-materialdesign-pack</artifactId>
    <version>12.3.1</version>
</dependency>
```

The font is loaded automatically once the pack JAR is on the classpath; no manual `Font.loadFont()` call is required.

## Icon Literal Format

An icon literal is `prefix-name`, where `prefix` identifies the pack and `name` is the icon identifier using underscores for spaces.

| Pack | Literal Example | Unicode Code |
|------|----------------|--------------|
| Material Design | `mdal-save` | `\ue161` |
| Font Awesome solid | `fa-save` | `\uf0c7` |
| Font Awesome regular | `far-save` | `\uf0c7` |
| Feather | `fi-save` | `\ue8a0` |
| Bootstrap | `bi-save` | `\uf1a1` |

The literal is the single source of truth — FXML, Java, and JSON all reference it.

## Icon Mapping Workflow

1. **Enumerate UI elements**: Walk the FXML prototypes and list every element needing an icon (menu items, toolbar buttons, tab icons, list cells).
2. **Select icons**: For each element, choose the icon name from the chosen pack's catalog that best matches the action.
3. **Map to FontIcon**: Record the literal and its Unicode code for each element.
4. **Generate config**: Emit `design/icons/icon-config.json` with the full mapping.
5. **Apply in FXML/Java**: Insert `<FontIcon>` elements or construct `new FontIcon(literal)`.

## FontIcon Usage in FXML

```xml
<?import org.kordamp.ikonli.javafx.FontIcon?>
<?import org.kordamp.ikonli.materialdesign.MaterialDesignAL?>

<MenuItem fx:id="saveMenuItem" text="Save">
    <graphic>
        <FontIcon iconLiteral="mdal-save" iconSize="16"/>
    </graphic>
</MenuItem>

<Button fx:id="deleteButton" text="Delete">
    <graphic>
        <FontIcon iconLiteral="mdal-delete" iconSize="18"/>
    </graphic>
</Button>
```

## FontIcon Usage in Java

```java
import org.kordamp.ikonli.javafx.FontIcon;

FontIcon saveIcon = new FontIcon("mdal-save");
saveIcon.setIconSize(16);

Button saveButton = new Button("Save", saveIcon);

// Icon-only button
FontIcon deleteIcon = new FontIcon("mdal-delete");
deleteIcon.setIconSize(18);
Button deleteButton = new Button("", deleteIcon);
deleteButton.setAccessibleText("Delete");
```

## Icon Sizing and Coloring

Icon size and color are controlled through CSS properties specific to `FontIcon`, plus standard text-fill.

| Property | Purpose | Example |
|----------|---------|---------|
| `-fx-icon-size` | Glyph size in pixels | `-fx-icon-size: 18px;` |
| `-fx-icon-color` | Glyph color | `-fx-icon-color: -fx-text-primary;` |

```css
.menu-item > .graphic {
    -fx-icon-size: 16px;
    -fx-icon-color: -fx-text-secondary;
}
.button-primary > .graphic {
    -fx-icon-size: 18px;
    -fx-icon-color: -fx-text-on-primary;
}
.button-danger:hover > .graphic {
    -fx-icon-color: -fx-text-on-primary;
}
```

## Icon Config JSON Format

The icon mapping is persisted to `design/icons/icon-config.json` so `javafx-developer` can wire icons automatically. The schema lists each UI element, its literal, and the Unicode code.

```json
{
  "library": "ikonli-materialdesign-pack",
  "version": "12.3.1",
  "prefix": "mdal-",
  "elements": [
    { "ui_element": "newMenuItem", "icon_literal": "mdal-content_copy", "icon_code": "ue14d", "category": "file" },
    { "ui_element": "openMenuItem", "icon_literal": "mdal-folder_open", "icon_code": "ue2c8", "category": "file" },
    { "ui_element": "saveButton", "icon_literal": "mdal-save", "icon_code": "ue161", "category": "file" },
    { "ui_element": "deleteButton", "icon_literal": "mdal-delete", "icon_code": "ue872", "category": "edit" }
  ]
}
```

Fields: `library` and `version` mirror the Maven coordinates; `prefix` is the pack prefix; each entry's `ui_element` matches an `fx:id` in the FXML; `icon_literal` is the value used in `<FontIcon>`; `icon_code` is the Unicode glyph for verification; `category` groups icons for documentation.

## Common Icon Mappings Table

These literals use the Material Design (`mdal-`) pack. Substitute the appropriate prefix when another pack is selected.

### File Operations
| UI Element | Icon Literal | Unicode | Meaning |
|-----------|--------------|---------|---------|
| newMenuItem | `mdal-content_copy` | `ue14d` | New file |
| openMenuItem | `mdal-folder_open` | `ue2c8` | Open file |
| saveMenuItem / saveButton | `mdal-save` | `ue161` | Save |
| closeMenuItem | `mdal-close` | `ue5cd` | Close |
| printMenuItem | `mdal-print` | `ue8ad` | Print |
| exportMenuItem | `mdal-file_download` | `ue2c6` | Export |

### Edit Operations
| UI Element | Icon Literal | Unicode | Meaning |
|-----------|--------------|---------|---------|
| undoMenuItem | `mdal-undo` | `ue166` | Undo |
| redoMenuItem | `mdal-redo` | `ue15a` | Redo |
| cutMenuItem | `mdal-content_cut` | `ue14e` | Cut |
| copyMenuItem | `mdal-content_copy` | `ue14d` | Copy |
| pasteMenuItem | `mdal-content_paste` | `ue14f` | Paste |
| deleteButton | `mdal-delete` | `ue872` | Delete |
| editButton | `mdal-edit` | `ue254` | Edit |
| findMenuItem | `mdal-search` | `ue8b6` | Find |

### Navigation
| UI Element | Icon Literal | Unicode | Meaning |
|-----------|--------------|---------|---------|
| backButton | `mdal-arrow_back` | `ue5cb` | Back |
| forwardButton | `mdal-arrow_forward` | `ue5cb` | Forward |
| homeButton | `mdal-home` | `ue88a` | Home |
| refreshButton | `mdal-refresh` | `ue5d5` | Refresh |
| upButton | `mdal-arrow_upward` | `ue5d8` | Up |

### View
| UI Element | Icon Literal | Unicode | Meaning |
|-----------|--------------|---------|---------|
| zoomInButton | `mdal-zoom_in` | `ue8ff` | Zoom in |
| zoomOutButton | `mdal-zoom_out` | `ue900` | Zoom out |
| fullscreenButton | `mdal-fullscreen` | `ue5d0` | Fullscreen |
| fullscreenExitButton | `mdal-fullscreen_exit` | `ue5d1` | Exit fullscreen |
| settingsMenuItem | `mdal-settings` | `ue8b8` | Settings |
| helpMenuItem | `mdal-help` | `ue887` | Help |

Always verify literals against the official Ikonli icon catalog for the chosen pack version, since codes can shift between releases.
