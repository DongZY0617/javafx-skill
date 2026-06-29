# FXML Parsing Rules and User Workflow Extraction

This reference defines how the JavaFX DocGen skill parses FXML files to produce a user manual. It covers control extraction, `fx:id` and event-handler mapping, control inventory tables, step-by-step workflow derivation, and keyboard shortcut discovery.

## Overview

The user manual is generated from FXML layout files (typically under `src/main/resources/`) together with their associated Controller classes. The goal is to describe, in end-user terms, what each window contains and how to accomplish common tasks. The manual never exposes implementation details; it speaks about buttons, fields, and actions from the user's perspective.

## FXML File Parsing

Each `.fxml` file is parsed as XML. The parser extracts:

1. **Root element** — the top-level container (`BorderPane`, `VBox`, `HBox`, `AnchorPane`, `StackPane`, `GridPane`, etc.).
2. **`fx:controller` attribute** — the fully qualified Controller class name.
3. **Namespace declarations** — `xmlns:fx`, `xmlns` — used to resolve JavaFX types.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.BorderPane?>
<?import javafx.scene.control.Button?>
<?import javafx.scene.control.TextField?>

<BorderPane fx:controller="com.example.app.UserController"
            xmlns:fx="http://javafx.com/fxml">
    <top>
        <TextField fx:id="searchField" promptText="Search..."/>
    </top>
    <bottom>
        <Button fx:id="searchButton" text="Search" onAction="#handleSearch"/>
    </bottom>
</BorderPane>
```

## Extracting UI Controls

The parser walks every element in the FXML tree and records recognized JavaFX control types. Common controls and their documentation labels:

| FXML Element | User-Facing Label |
|--------------|-------------------|
| `Button` | Button |
| `TextField` / `PasswordField` | Text field |
| `TextArea` | Text area |
| `CheckBox` | Check box |
| `RadioButton` | Radio button |
| `ComboBox` / `ChoiceBox` | Drop-down list |
| `ListView` | List |
| `TableView` / `TreeView` | Table / Tree |
| `Menu` / `MenuItem` | Menu item |
| `Tab` | Tab |
| `Slider` | Slider |
| `DatePicker` | Date picker |

Each control entry captures: element type, `text` or `promptText` (visible label), `fx:id`, and any event handler attributes.

## Extracting fx:id and onAction Handlers

For every control element, the parser reads the following attributes:

- `fx:id` — the field name injected into the Controller.
- `onAction="#methodName"` — the action handler invoked on activation.
- `onKeyPressed="#methodName"` / `onKeyReleased` — keyboard event handlers.
- `onMouseClicked="#methodName"` — mouse handlers.
- `text`, `promptText`, `title` — visible labels shown to the end user.

```xml
<Button fx:id="saveButton" text="Save" onAction="#handleSave"/>
<Button fx:id="cancelButton" text="Cancel" onAction="#handleCancel"/>
<TextField fx:id="nameField" promptText="Enter full name"/>
```

## Mapping onAction to Controller Methods

Each `onAction="#handleXxx"` reference is resolved against the Controller class declared in `fx:controller`. The generator locates the method by name and extracts its Javadoc description as the action's user-facing explanation.

```java
public class UserController {
    /** Saves the current record and closes the dialog. */
    @FXML
    private void handleSave(ActionEvent event) { ... }

    /** Discards changes and closes the dialog without saving. */
    @FXML
    private void handleCancel(ActionEvent event) { ... }
}
```

Mapping result:

| fx:id | Handler | Description |
|-------|---------|-------------|
| `saveButton` | `handleSave` | Saves the current record and closes the dialog. |
| `cancelButton` | `handleCancel` | Discards changes and closes the dialog without saving. |

When the handler method is undocumented, the entry reads *No documentation available.* When the referenced method does not exist in the Controller, the entry is flagged as an unresolved reference and a warning is emitted.

## Generating Control Inventory Tables

Each view (FXML file) produces a control inventory table listing every interactive control. Non-interactive layout containers (`VBox`, `HBox`, `GridPane`) are excluded from the inventory but appear in the view overview.

```markdown
### User Form — Control Inventory

| Control | Label / Prompt | fx:id | Action | Description |
|---------|----------------|-------|--------|-------------|
| Button | Save | `saveButton` | `handleSave` | Saves the current record and closes the dialog. |
| Button | Cancel | `cancelButton` | `handleCancel` | Discards changes and closes the dialog without saving. |
| Text field | Enter full name | `nameField` | — | Accepts the user's full name. |
```

## Extracting User Workflows

A user workflow is a numbered, step-by-step description of a task the user can perform. Workflows are derived by combining the control inventory with the Controller's handler descriptions. The generator identifies candidate workflows from handler names and Javadoc:

- Handlers named `handleSave`, `handleNew`, `handleDelete`, `handleExport`, `handleSearch` each suggest a distinct task.
- The handler's Javadoc supplies the outcome; the controls with matching `fx:id` prefixes supply the inputs.

```markdown
### Workflow: Save a Record

1. Enter the required details in the **Name** text field.
2. (Optional) Adjust additional fields as needed.
3. Click the **Save** button.
4. The record is saved and the dialog closes automatically.
```

When the Controller lacks descriptive Javadoc, the generator falls back to a generic template derived from the handler name and the bound controls, clearly marked as auto-generated.

## Extracting Keyboard Shortcuts

Keyboard shortcuts are discovered by scanning Controller source for `KeyCodeCombination` and `KeyCombination` instances, plus `Mnemonic` registrations and `scene.getAccelerators()` entries.

```java
@FXML
private void initialize() {
    KeyCombination saveShortcut =
        new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
    saveButton.getScene().getAccelerators().put(saveShortcut, () -> handleSave(null));
}
```

Extracted shortcuts table:

```markdown
### Keyboard Shortcuts

| Shortcut | Action | Description |
|----------|--------|-------------|
| `Ctrl+S` | Save | Saves the current record. |
| `Ctrl+F` | Search | Focuses the search field. |
| `Esc` | Close | Closes the active dialog without saving. |
```

`KeyCode` constants are translated to human-readable form (`CONTROL_DOWN` -> `Ctrl`, `SHIFT_DOWN` -> `Shift`, `ALT_DOWN` -> `Alt`). Platform-specific modifiers are normalized: on macOS, `Ctrl` is rendered as `Cmd` when the `SHORTCUT_DOWN` modifier is used.

## View Overview Template

Each FXML file produces a view section following this structure:

```markdown
## User Form

The **User Form** window collects user details and provides actions to save or
cancel the current entry. It uses a `BorderPane` root layout with input fields
in the center and action buttons at the bottom.

- **Root layout**: BorderPane
- **Controller**: `com.example.app.UserController`

### Control Inventory
...table...

### Workflows
...steps...

### Keyboard Shortcuts
...table...
```

This keeps every view section consistent and navigable.
