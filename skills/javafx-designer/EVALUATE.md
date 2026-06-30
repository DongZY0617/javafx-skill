# JavaFX Designer Evaluation Test Cases

This file defines the acceptance test cases for the `javafx-designer` skill, used to quantify design output quality. Each case describes the input scenario, case type, covered dimensions, expected outputs, and checkable verification standards.

- **Positive samples**: Real design requests that verify designer output completeness and correctness
- **Negative samples**: Edge cases and constraint violations that verify designer robustness
- **Boundary cases**: Partial design scopes, standalone mode, theme-only mode, etc.

---

## Case Overview

| ID | Name | Type | Covered Dimensions | Expected Artifacts |
|----|------|------|--------------------|--------------------|
| 1 | Full design: CRUD app | Positive | All 5 | FXML + CSS + Mermaid + Icons + JSON |
| 2 | Full design: Wizard app | Positive | All 5 | FXML + CSS + Mermaid + Icons + JSON |
| 3 | Prototype only: Dashboard | Boundary | Prototype | FXML only |
| 4 | Theme only: Restyle existing app | Boundary | Theme | CSS only (light + dark) |
| 5 | Flow only: User journey mapping | Boundary | Flow | Mermaid only |
| 6 | FXML validity check | Positive | Prototype | Well-formed FXML |
| 7 | Light/dark theme structural identity | Positive | Theme | Same selectors, different colors |
| 8 | fx:id uniqueness within FXML | Positive | Prototype | All fx:id unique |
| 9 | Mermaid syntax validity | Positive | Flow | Valid Mermaid flowchart |
| 10 | Icon mapping completeness | Positive | Icons | All UI elements have icons |
| 11 | Responsive layout annotations | Positive | Responsive | min/pref/max sizes set |
| 12 | Design handoff JSON format | Positive | All 5 | Valid design-handoff.json |
| 13 | Standalone mode (no loop) | Boundary | All 5 | Artifacts + standalone_complete |
| 14 | Pre-generation mode (with loop) | Boundary | All 5 | Artifacts + generating next_action |
| 15 | No production code generated | Negative | All 5 | No .java files in design/ |
| 16 | Invalid FXML rejected | Negative | Prototype | Error report, no FXML output |
| 17 | Duplicate fx:id detected | Negative | Prototype | Error report, validation failure |
| 18 | Mermaid syntax error | Negative | Flow | Error report, no .mmd output |
| 19 | Hardcoded CSS color detected | Negative | Theme | Error report, validation failure |
| 20 | Architecture handoff consumption | Positive | Prototype | FXML aligned to architecture_pattern |

---

## Case 1: Full Design — CRUD Application

- **Input**: "Design and generate a JavaFX CRUD application for managing users with a table view, add/edit/delete buttons, and a search field. Include light and dark themes."
- **Type**: Positive sample
- **Covered Dimensions**: All 5 (prototype, theme, flow, responsive, icons)
- **Expected Artifacts**:
  - `design/fxml/main-view.fxml` (BorderPane with MenuBar, TableView, search field, CRUD buttons)
  - `design/css/light-theme.css` and `design/css/dark-theme.css`
  - `design/flow/interaction-flow.mmd` (Mermaid with CRUD operations flow)
  - `design/icons/icon-config.json` (icons for add/edit/delete/search)
  - `design/design-handoff.json`
- **Verification Standards**:
  - [ ] Main view FXML uses BorderPane as root container
  - [ ] FXML contains TableView with fx:id
  - [ ] FXML contains search TextField with fx:id
  - [ ] FXML contains add/edit/delete Buttons with fx:id
  - [ ] Light theme CSS defines `.root` with CSS variables
  - [ ] Dark theme CSS has same selectors as light theme
  - [ ] Mermaid diagram shows CRUD flow (view → add/edit/delete → save/cancel)
  - [ ] Icon config maps add/edit/delete/search to Ikonli literals
  - [ ] design-handoff.json lists all screens with controls
  - [ ] Design report has conclusion "Pass"

---

## Case 2: Full Design — Wizard Application

- **Input**: "Design a JavaFX wizard application with 3 steps: user info, preferences, and confirmation. Include step navigation and a progress indicator."
- **Type**: Positive sample
- **Covered Dimensions**: All 5
- **Expected Artifacts**:
  - `design/fxml/wizard-view.fxml` (StackPane or BorderPane with StepIndicator)
  - FXML for each wizard step (or embedded in single FXML)
  - CSS themes with wizard-specific styles
  - Mermaid diagram showing wizard flow (Step 1 → Step 2 → Step 3 → Confirm)
- **Verification Standards**:
  - [ ] FXML uses appropriate layout for wizard (StackPane for step switching or BorderPane)
  - [ ] Navigation buttons (Previous/Next/Finish/Cancel) have fx:id
  - [ ] Mermaid diagram shows linear wizard flow with back navigation
  - [ ] Icon config includes navigation icons (back, forward, check, cancel)

---

## Case 3: Prototype Only — Dashboard

- **Input**: "Create a UI prototype for a dashboard with stat cards, a chart area, and a recent activity list. I don't need themes or icons yet."
- **Type**: Boundary case
- **Covered Dimensions**: Prototype only
- **Expected Artifacts**:
  - `design/fxml/dashboard.fxml` only
  - No CSS, Mermaid, or icon files
- **Verification Standards**:
  - [ ] Only FXML files are generated (no CSS/Mermaid/icon artifacts)
  - [ ] Design report scope is "Prototype Only"
  - [ ] Design report dimensions array contains only "prototype"
  - [ ] FXML contains stat card layout (GridPane or HBox of VBoxes)
  - [ ] FXML contains chart area placeholder
  - [ ] FXML contains activity list placeholder

---

## Case 4: Theme Only — Restyle Existing App

- **Input**: "Design a dark theme for my existing JavaFX app. Use a purple primary color (#7c3aed) and slate gray backgrounds. The dark theme is the primary deliverable; the light theme should be auto-generated as a structural mirror to satisfy the light/dark structural identity rule."
- **Type**: Boundary case
- **Covered Dimensions**: Theme only
- **Expected Artifacts**:
  - `design/css/dark-theme.css` (primary output — the user-requested dark theme with #7c3aed primary and slate gray backgrounds) and `design/css/light-theme.css` (structural mirror — same selectors and CSS variable names, only color values differ)
  - No FXML, Mermaid, or icon files
- **Note**: The dark theme is the primary output explicitly requested by the user; the light theme is generated as a structural mirror because theme switching requires a structurally identical counterpart (see `references/theme-system.md` — "dark theme must be structurally identical to the light theme"). Both must be produced even though only the dark theme was explicitly requested.
- **Verification Standards**:
  - [ ] Only CSS files are generated
  - [ ] Design report scope is "Theme Only"
  - [ ] Primary color in CSS is #7c3aed as requested
  - [ ] Dark theme uses slate gray backgrounds
  - [ ] Both themes define all CSS variables in `.root`
  - [ ] Component styles reference CSS variables (no hardcoded colors)

---

## Case 5: Flow Only — User Journey Mapping

- **Input**: "Generate an interaction flow diagram for a JavaFX login system: user enters credentials → validate → success goes to dashboard, failure shows error and retries. After 3 failures, lock account."
- **Type**: Boundary case
- **Covered Dimensions**: Flow only
- **Expected Artifacts**:
  - `design/flow/interaction-flow.mmd` only
- **Verification Standards**:
  - [ ] Only Mermaid file is generated
  - [ ] Design report scope is "Flow Only"
  - [ ] Mermaid diagram shows login → validate → success/failure branches
  - [ ] Failure branch includes retry and lockout after 3 attempts
  - [ ] Mermaid syntax is valid (graph TD, decision diamonds, labeled edges)

---

## Case 6: FXML Validity Check

- **Input**: "Design a simple JavaFX app with a form: name field, email field, submit button."
- **Type**: Positive sample
- **Covered Dimensions**: Prototype
- **Expected Artifacts**: `design/fxml/form-view.fxml`
- **Verification Standards**:
  - [ ] FXML file is well-formed XML (parseable by any XML parser)
  - [ ] All JavaFX imports are present (`<?import javafx.scene.control.*?>`)
  - [ ] Root element has `xmlns="http://javafx.com/javafx/17"` and `xmlns:fx="http://javafx.com/fxml/1"`
  - [ ] No unclosed tags or malformed attributes
  - [ ] fx:controller attribute is present (placeholder)

---

## Case 7: Light/Dark Theme Structural Identity

- **Input**: "Design themes for a JavaFX app with a blue primary color."
- **Type**: Positive sample
- **Covered Dimensions**: Theme
- **Expected Artifacts**: `design/css/light-theme.css` and `design/css/dark-theme.css`
- **Verification Standards**:
  - [ ] Both files define the same CSS selectors (`.button`, `.text-field`, `.label`, etc.)
  - [ ] Both files define the same CSS variable names in `.root`
  - [ ] Light theme has white/light backgrounds
  - [ ] Dark theme has dark backgrounds (#1e1e1e or darker)
  - [ ] Text colors are inverted (dark text in light, light text in dark)
  - [ ] Semantic colors (success/warning/danger) are same or similar in both

---

## Case 8: fx:id Uniqueness Within FXML

- **Input**: "Design a settings dialog with general settings, appearance settings, and notification settings tabs."
- **Type**: Positive sample
- **Covered Dimensions**: Prototype
- **Expected Artifacts**: `design/fxml/settings-dialog.fxml`
- **Verification Standards**:
  - [ ] All fx:id values within the FXML file are unique
  - [ ] fx:id values use camelCase naming
  - [ ] fx:id values are semantically meaningful (e.g., `generalTab`, `appearanceTab`)
  - [ ] No two controls share the same fx:id

---

## Case 9: Mermaid Syntax Validity

- **Input**: "Design the interaction flow for a multi-screen app: login → dashboard → settings → about, with logout returning to login."
- **Type**: Positive sample
- **Covered Dimensions**: Flow
- **Expected Artifacts**: `design/flow/interaction-flow.mmd`
- **Verification Standards**:
  - [ ] File starts with `graph TD` or `graph LR`
  - [ ] All nodes have valid syntax (e.g., `A[Label]`, `B{Decision?}`)
  - [ ] All edges use `-->` syntax
  - [ ] Edge labels use `-- label -->` syntax
  - [ ] No syntax errors that would prevent Mermaid rendering
  - [ ] All screens mentioned in the input are present in the diagram

---

## Case 10: Icon Mapping Completeness

- **Input**: "Design a JavaFX text editor with File menu (New, Open, Save, Exit), Edit menu (Undo, Redo, Cut, Copy, Paste), and a toolbar with Save and Print buttons."
- **Type**: Positive sample
- **Covered Dimensions**: Icons
- **Expected Artifacts**: `design/icons/icon-config.json`
- **Verification Standards**:
  - [ ] Every menu item has an icon mapping
  - [ ] Every toolbar button has an icon mapping
  - [ ] Icon literals use valid Ikonli format (prefix-name)
  - [ ] Maven dependency snippet includes ikonli-javafx and at least one icon pack
  - [ ] Icon config JSON is valid and parseable

---

## Case 11: Responsive Layout Annotations

- **Input**: "Design a responsive JavaFX app that works on small windows (800x600) and large screens (1920x1080). Sidebar should collapse on small windows."
- **Type**: Positive sample
- **Covered Dimensions**: Responsive
- **Expected Artifacts**: FXML with responsive annotations
- **Verification Standards**:
  - [ ] FXML root has minWidth="800" and minHeight="600"
  - [ ] FXML root has prefWidth and prefHeight set
  - [ ] Design report responsive section lists 3 breakpoints (Compact, Medium, Large)
  - [ ] Compact breakpoint behavior mentions sidebar collapse
  - [ ] Layout uses AnchorPane or BorderPane for auto-resize behavior

---

## Case 12: Design Handoff JSON Format

- **Input**: "Design and generate a simple JavaFX app with a main window and an about dialog."
- **Type**: Positive sample
- **Covered Dimensions**: All 5
- **Expected Artifacts**: `design/design-handoff.json`
- **Verification Standards**:
  - [ ] JSON is valid and parseable
  - [ ] Contains "screens" array with at least 2 entries (main-view, about-dialog)
  - [ ] Each screen entry has name, fxml_path, controller_class, and controls array
  - [ ] Contains "themes" object with light and dark paths
  - [ ] Contains "icons" object with config path, library, and version
  - [ ] Contains "interaction_flow" field with Mermaid file path

---

## Case 13: Standalone Mode (No Loop)

- **Input**: "Design a UI prototype for my app. I'll review it before generating code."
- **Type**: Boundary case
- **Covered Dimensions**: All 5
- **Expected Artifacts**: All design artifacts + design report
- **Verification Standards**:
  - [ ] Design report loop_state.next_action is "standalone_complete"
  - [ ] Design report loop_state.design_phase is "standalone"
  - [ ] No `.loop-state.json` is created (standalone mode)
  - [ ] No Java code files are generated
  - [ ] User can review artifacts and later trigger developer separately

---

## Case 14: Pre-Generation Mode (With Loop)

- **Input**: "Design and generate a JavaFX user management app with a table view and CRUD operations."
- **Type**: Boundary case
- **Covered Dimensions**: All 5
- **Expected Artifacts**: All design artifacts + design-handoff.json
- **Verification Standards**:
  - [ ] Design report loop_state.next_action is "generating"
  - [ ] Design report loop_state.design_phase is "pre-generation"
  - [ ] design-handoff.json is produced for developer consumption
  - [ ] `.loop-state.json` has status "designing" then transitions to "generating"
  - [ ] Developer is triggered after designer completes (in orchestrated mode)

---

## Case 15: No Production Code Generated

- **Input**: "Design a JavaFX app with a login screen, dashboard, and settings."
- **Type**: Negative sample
- **Covered Dimensions**: All 5
- **Expected Artifacts**: Design artifacts only (FXML, CSS, Mermaid, JSON)
- **Verification Standards**:
  - [ ] No `.java` files are generated in the `design/` directory
  - [ ] No controller logic is generated (FXML has fx:controller placeholder only)
  - [ ] No service classes, repositories, or business logic code
  - [ ] No `pom.xml` is created or modified (only Maven dependency snippet is provided in report)
  - [ ] No `module-info.java` is generated
  - [ ] Designer produces only FXML, CSS, Mermaid, and JSON artifacts

---

## Case 16: Invalid FXML Rejected

- **Input**: "Design a JavaFX login form." — but the input/reference FXML contains malformed XML (e.g., unclosed `<TextField>` tag, mismatched `</VBox>` closing tag, attribute without quotes).
- **Type**: Negative sample
- **Covered Dimensions**: Prototype
- **Expected Artifacts**: Error/diagnostic report; no FXML file committed to `design/fxml/`.
- **Verification Standards**:
  - [ ] Designer detects the malformed XML during the FXML validation phase (Step 2)
  - [ ] An error is reported identifying the specific XML well-formedness violation (e.g., "unclosed tag `<TextField>` at line N")
  - [ ] No invalid `.fxml` file is written to `design/fxml/`
  - [ ] `design-handoff.json` is either not produced or marked with a validation failure / `conclusion: "Fail"`
  - [ ] Designer does not attempt to generate controllers or downstream artifacts from the malformed FXML
  - [ ] The error message includes a corrective hint (e.g., "close the `<TextField>` tag")

---

## Case 17: Duplicate fx:id Detected

- **Input**: "Design a settings dialog with general, appearance, and notification tabs." — the generated/reviewed FXML inadvertently assigns the same `fx:id="tabPane"` to two distinct controls (e.g., two TabPanes).
- **Type**: Negative sample
- **Covered Dimensions**: Prototype
- **Expected Artifacts**: Validation error report; FXML retained only after the duplicate is resolved.
- **Verification Standards**:
  - [ ] Designer detects the duplicate `fx:id` during the FXML validation phase (fx:id uniqueness check, Step 2)
  - [ ] The error identifies the duplicated `fx:id` value and both offending control locations (line numbers)
  - [ ] The duplicate `fx:id` is flagged before any handoff is finalized
  - [ ] `design-handoff.json` lists each control with a unique `fx:id` (no duplicates after correction)
  - [ ] If not auto-corrected, `conclusion` is `"Fail"` with the duplicate cited as the failure reason
  - [ ] No two controls in the final FXML share the same `fx:id`

---

## Case 18: Mermaid Syntax Error

- **Input**: "Design the interaction flow for a login system." — the generated/reviewed Mermaid file contains syntax errors (e.g., missing `graph TD` declaration, invalid edge `==>` instead of `-->`, node id with spaces `A[Login Screen]` used as `A Login Screen`).
- **Type**: Negative sample
- **Covered Dimensions**: Flow
- **Expected Artifacts**: Error/diagnostic report; no `.mmd` file committed to `design/flow/`.
- **Verification Standards**:
  - [ ] Designer detects the Mermaid syntax error during the flow validation phase
  - [ ] The error identifies the specific syntax violation and offending line
  - [ ] No invalid `.mmd` file is written to `design/flow/`
  - [ ] `design-handoff.json` `interaction_flow` is either absent or marked invalid
  - [ ] Designer does not proceed to render or embed the broken diagram
  - [ ] The error message includes a corrective suggestion (e.g., "use `-->` for edges" or "start with `graph TD`")

---

## Case 19: Hardcoded CSS Color Detected

- **Input**: "Design themes for a JavaFX app with a blue primary color." — the generated CSS contains hardcoded color literals (e.g., `.button { -fx-background-color: #2563eb; }`) instead of referencing a CSS variable defined in `.root`.
- **Type**: Negative sample
- **Covered Dimensions**: Theme
- **Expected Artifacts**: Validation error report; CSS accepted only after colors are converted to CSS variables.
- **Verification Standards**:
  - [ ] Designer detects hardcoded hex/rgb color values in component selectors (outside `.root`) during the theme validation phase
  - [ ] Each hardcoded color is reported with the file, selector, and line number
  - [ ] The violation is explained: component styles must reference CSS variables (e.g., `-fx-background-color: -fx-primary-color`) defined in `.root`
  - [ ] CSS files are accepted only after all hardcoded colors are replaced with variable references
  - [ ] If not corrected, `conclusion` is `"Fail"` with hardcoded colors cited as the failure reason
  - [ ] Both light and dark theme files reference the same CSS variable names (no hardcoded colors)

---

## Case 20: Architecture Handoff Consumption

- **Input**: A project where `architecture-handoff.json` exists (produced by `javafx-architect`) with `system_design.architecture_pattern` set to `"MVVM"` and `developer_instructions.package_structure` defining the package layout. User then requests: "Design the UI based on the architecture."
- **Type**: Positive sample
- **Covered Dimensions**: Prototype (and aligned All 5 when full scope)
- **Expected Artifacts**: FXML whose structure aligns with the consumed `architecture_pattern`, plus `design-handoff.json` referencing the architecture.
- **Verification Standards**:
  - [ ] Designer detects and reads `architecture-handoff.json` in Step 1.2 (does not re-derive the architecture pattern)
  - [ ] FXML structure aligns with `system_design.architecture_pattern`:
    - `MVC` → FXML uses `fx:controller` with direct model/service references
    - `MVVM` → FXML binds to ViewModel properties (`fx:id` + `@FXML` fields intended for ViewModel binding), no direct service/repository references in the controller
    - `MVP` → FXML uses a passive view with presenter-mediated event handlers
  - [ ] Controller class names and package paths follow `developer_instructions.naming_convention` and `package_structure` from the handoff
  - [ ] `design-handoff.json` records the consumed `architecture_pattern` and references the source `architecture-handoff.json`
  - [ ] Screen/control decomposition mirrors the `system_design.modules[]` boundaries (e.g., one view per module where applicable)
  - [ ] When the architecture handoff specifies a `database` (non-"none"), the designer does NOT generate data-access FXML logic (stays in the presentation layer per `developer_instructions.layering_rule`)
  - [ ] Designer does not contradict architectural layering rules (e.g., no direct JDBC calls wired in FXML/controller for an MVVM + Service Layer architecture)

---

## Case 21: Design Handoff Schema Version Rejection (Negative)

- **Input**: A `design-handoff.json` with `"design_version": "2.0"` (the schema's `const` is `"1.0"`). User attempts to consume this handoff via `javafx-developer`.
- **Type**: Negative sample
- **Expected Artifacts**: Error/diagnostic report; no code generation from the mismatched handoff.
- **Verification Standards**:
  - [ ] Developer detects the version mismatch against the `const: "1.0"` constraint before any field-level processing
  - [ ] Error message clearly states the expected version ("1.0") and the actual version ("2.0")
  - [ ] No partial processing occurs — the handoff is rejected in full
  - [ ] No FXML, CSS, or controller code is generated from the mismatched handoff
  - [ ] `conclusion` is `Fail` with version mismatch cited as the failure reason
