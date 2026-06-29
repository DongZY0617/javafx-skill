# FXML Prototype Generation Rules

This document defines the rules for generating FXML layout prototypes from natural language descriptions. It covers layout container selection, control placement patterns, naming conventions, control tree previews, placeholder content, and validation.

## Layout Container Selection Guide

Choosing the correct root container is the most important layout decision. Use the decision tree below to select the container that best fits the screen's purpose.

### Decision Tree

```
Is the screen a classic desktop window with menu bar + content + status bar?
├── YES → BorderPane
└── NO → Are controls arranged in a simple vertical or horizontal sequence?
        ├── YES (vertical) → VBox
        ├── YES (horizontal) → HBox
        └── NO → Is it a form with aligned labels and fields?
                ├── YES → GridPane
                └── NO → Are there two resizable side-by-side or stacked regions?
                        ├── YES → SplitPane
                        └── NO → Are there multiple categorised pages sharing one window?
                                ├── YES → TabPane
                                └── NO → Do views overlay or swap in the same region?
                                        ├── YES → StackPane
                                        └── NO → Default to VBox (safest fallback)
```

### Container Quick Reference

| Container | Best For | Grows On | Typical Children |
|-----------|----------|----------|------------------|
| `BorderPane` | Desktop main windows | center grows; top/bottom fixed height; left/right fixed width | MenuBar, ToolBar, TableView, StatusBar |
| `VBox` | Dialogs, forms, sidebars | vertical | Labels, TextFields, Buttons |
| `HBox` | Toolbars, button rows | horizontal | Buttons, Separators |
| `GridPane` | Settings panels, data-entry forms | both, via row/column constraints | Labels + inputs in column pairs |
| `SplitPane` | Master-detail, file explorers | the larger region grows | ListView + detail panel |
| `TabPane` | Settings categories, multi-view tabs | active tab content | Tab items |
| `StackPane` | Card UIs, layered overlays, splash screens | top child fills area | Overlapping Panes |

## Control Placement Patterns by Screen Type

### CRUD Screen
- **Top**: MenuBar (File, Edit, Help) + ToolBar (New, Edit, Delete, Refresh)
- **Center**: TableView with placeholder `<Label text="No data"/>`
- **Bottom**: HBox status bar with a Label (`statusLabel`) and a ProgressBar
- **Dialogs**: Separate `GridPane`-based edit/create dialogs with Save/Cancel buttons

### Dashboard
- **Root**: BorderPane or TilePane of metric cards
- **Center**: GridPane of chart cards (PieChart, BarChart, LineChart)
- **Top**: ToolBar with filter ComboBox and date-range controls

### Wizard
- **Root**: BorderPane
- **Top**: Progress indicator or step breadcrumbs (HBox of Labels)
- **Center**: StackPane that swaps step content
- **Bottom**: HBox with Back / Next / Finish / Cancel buttons (`backButton`, `nextButton`, `finishButton`, `cancelButton`)

### Dialog
- **Root**: VBox or GridPane wrapped in a `DialogPane`
- **Body**: Form fields with `fx:id` values
- **Buttons**: Apply / OK / Cancel using `ButtonType`

### Settings
- **Root**: SplitPane or BorderPane with a left ListView of categories
- **Center**: TabPane or ScrollPane containing GridPane option rows

## fx:id Naming Conventions

All interactive controls receive an `fx:id` so the controller can inject them.

| Rule | Convention | Example |
|------|-----------|---------|
| Casing | camelCase | `userNameField` |
| Suffix | Suffix by control type | `Button` → `saveButton`, `TextField` → `nameField`, `TableView` → `userTable` |
| Semantics | Describe purpose, never position | `deleteButton` not `rightButton` |
| Uniqueness | Unique within one FXML file | Never repeat an id |
| Booleans | For CheckBox/RadioButton use the noun | `autoSaveCheckBox` |

```xml
<Button fx:id="saveButton" text="Save" styleClass="button-primary"/>
<TextField fx:id="searchField" promptText="Search..."/>
<TableView fx:id="userTable" styleClass="data-table"/>
```

## styleClass Naming Conventions

`styleClass` values drive theming. Use kebab-case semantic names.

| Category | Pattern | Examples |
|----------|---------|---------|
| Regions | area/region nouns | `content-area`, `status-bar`, `sidebar`, `toolbar` |
| Buttons | `button-` + role | `button-primary`, `button-secondary`, `button-danger` |
| Inputs | control noun | `text-field`, `combo-box`, `data-table` |
| Text | role + type | `title-label`, `caption-text`, `error-label` |
| Cards | `card-` + purpose | `card-metric`, `card-chart` |

```xml
<VBox styleClass="content-area">
    <Label text="Dashboard" styleClass="title-label"/>
    <Button text="Delete" styleClass="button-danger"/>
</VBox>
```

## Control Tree Preview Format

Each FXML file is accompanied by a text-based control tree preview using standard tree-drawing characters. This gives developers an at-a-glance hierarchy without opening the FXML.

### Format Rules
1. Use `├──` for a node with more siblings below, `└──` for the last node
2. Use `│` to continue vertical guides; indent children by four spaces
3. Append `[fx:id=name]` when the control has an fx:id
4. Append `[styleClass=name]` when the control has a styleClass
5. Append quoted text for Labels/Menus (`"Text"`)

### Example

```
BorderPane (root)
├── MenuBar (top) [styleClass=app-menubar]
│   ├── Menu "File"
│   │   ├── MenuItem "New" [fx:id=newMenuItem]
│   │   └── MenuItem "Open" [fx:id=openMenuItem]
│   └── Menu "Help"
│       └── MenuItem "About" [fx:id=aboutMenuItem]
├── ToolBar (top) [styleClass=app-toolbar]
│   ├── Button "New" [fx:id=newButton]
│   └── Button "Save" [fx:id=saveButton]
├── VBox (center) [styleClass=content-area]
│   ├── Label "User List" [styleClass=title-label]
│   └── TableView [fx:id=userTable] [styleClass=data-table]
└── HBox (bottom) [styleClass=status-bar]
    └── Label "Ready" [fx:id=statusLabel]
```

## Placeholder Content Guidelines

Prototypes are not connected to data, so placeholders communicate intent.

| Control | Placeholder Strategy | Example |
|---------|----------------------|---------|
| `Label` | Describe the region or upcoming content | `text="Data Table Area"` |
| `TextField` | Use `promptText` for input hint | `promptText="Enter username"` |
| `TableView` | Set `placeholder` to a Label | `<placeholder><Label text="No records"/></placeholder>` |
| `TableView` columns | Name columns by data field | `text="Name"`, `text="Email"` |
| `ComboBox` | Add one sample item | `<items><FXCollections fx:factory="observableArrayList"><String fx:value="Option A"/></FXCollections></items>` |
| Charts | Seed with two sample series | So layout sizing is visible |

Keep placeholders neutral — never fake real data that could be mistaken for production content.

## FXML Validation Checklist

Before an FXML file is considered complete, verify every item below.

| # | Check | How to Verify |
|---|-------|---------------|
| 1 | Well-formed XML | Every tag is closed; attributes are quoted |
| 2 | XML declaration present | First line is `<?xml version="1.0" encoding="UTF-8"?>` |
| 3 | All imports present | Every used control has an `<?import javafx.scene.*?>` line |
| 4 | Root element has `fx:controller` (if controller-bound) | `fx:controller="com.example.MainController"` |
| 5 | fx:id uniqueness | No two `fx:id` values repeat within the file |
| 6 | fx:id matches controller field names | Each `fx:id` corresponds to an `@FXML` field |
| 7 | styleClass values are kebab-case | No underscores or camelCase in class names |
| 8 | No hardcoded colors | Styling is in CSS, not inline `-fx-background-color` |
| 9 | Placeholder content present | Empty regions have explanatory Labels |
| 10 | Min/pref/max sizing set on root | `minWidth`, `prefWidth`, `prefHeight` declared |
| 11 | Accessibility hints | `accessibleText` on icon-only buttons |
| 12 | Control tree preview generated | A `.tree.txt` companion or report section exists |

A prototype that fails any check must be corrected before the design report is emitted.
