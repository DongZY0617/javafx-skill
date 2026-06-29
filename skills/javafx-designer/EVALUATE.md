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

- **Input**: "Design a dark theme for my existing JavaFX app. Use a purple primary color (#7c3aed) and slate gray backgrounds."
- **Type**: Boundary case
- **Covered Dimensions**: Theme only
- **Expected Artifacts**:
  - `design/css/light-theme.css` and `design/css/dark-theme.css` only
  - No FXML, Mermaid, or icon files
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
