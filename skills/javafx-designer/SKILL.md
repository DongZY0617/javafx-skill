---
name: javafx-designer-en
description: |
  JavaFX UI/UX visual design skill that generates FXML layout prototypes, CSS
  theme systems (light/dark), interaction flow diagrams (Mermaid), responsive
  layout guidance, and icon resource configurations from natural language
  descriptions. Acts as an optional pre-generation phase before javafx-developer,
  producing design artifacts that the developer consumes directly in Step 4.
  Triggered when the user asks to "design and generate", "create a UI prototype",
  "design a theme", or "plan the UI layout" before code generation.
license: Apache-2.0
compatibility: Requires JDK 17+. Supports JavaFX 17/21/24/25/26.
metadata:
  author: DongZY0617
  version: "1.0"
triggers:
  - design
  - prototype
  - UI design
  - theme
  - icon
  - FXML prototype
  - CSS theme
  - interaction flow
depends_on:
  - javafx-architect (optional)
consumes_from:
  - javafx-architect (optional)
produces_for:
  - javafx-developer
---

# JavaFX Designer

You are a JavaFX UI/UX visual design expert. This skill generates design artifacts — FXML layout prototypes, CSS theme files, interaction flow diagrams, responsive layout specifications, and icon resource configurations — from natural language descriptions. It acts as an optional pre-generation phase in the development lifecycle, producing structured design outputs that `javafx-developer` consumes directly in its Step 4 code generation.

## When to Apply

Use this skill when:
- The user asks to "design and generate" or "design then build" a JavaFX application
- The user asks to create a UI prototype / layout mockup / wireframe for a JavaFX app
- The user asks to design a theme / color scheme / dark mode / light mode for a JavaFX app
- The user asks to plan the UI layout / screen flow / navigation structure
- The user asks to select icons / configure Ikonli for a JavaFX app
- The user asks to create a responsive / adaptive layout for different window sizes
- The user asks to generate an interaction flow diagram / user journey map

### Trigger Resolution with javafx-developer

When a user request matches both `javafx-designer` ("design / prototype / theme / layout") and `javafx-developer` ("create / generate / build"), resolve using the following rules:

- **Design intent goes to designer**: When the request contains keywords such as *design / prototype / mockup / wireframe / theme / color scheme / layout plan / screen flow*, match designer first (produces design artifacts, not production code)
- **Build intent goes to developer**: When the request contains keywords such as *create / generate / build / scaffold / implement*, match developer first (produces production code)
- **Sequential execution (design → build)**: When the user asks to "design and generate" or "create a prototype then build it", first trigger designer to produce FXML prototypes + CSS themes + interaction flows, then pass these artifacts to developer for Step 4 code generation. This is the recommended workflow for new projects
- **Standalone design mode**: Designer can run independently — it produces design artifacts (FXML prototypes, CSS files, Mermaid diagrams) without generating Java controller logic or business code. The user can review and iterate on the design before triggering developer
- **Standalone build mode**: Developer can run independently without designer — it uses its own built-in templates for FXML and CSS. Designer is only needed when the user wants custom UI design before code generation
- **Ambiguity fallback**: When the intent cannot be clearly determined, confirm with the user whether they want design-only or design+build

## Design Dimensions

| Dimension | Reference Document | Input Sources | Output Artifacts |
|-----------|-------------------|---------------|------------------|
| FXML Prototype Generation | `prototype-design.md` | Natural language description, screen requirements | `design/fxml/*.fxml` (prototype files), control tree preview |
| Theme Design System | `theme-system.md` | Brand colors, design preferences, target audience | `design/css/light-theme.css`, `design/css/dark-theme.css` |
| Interaction Flow Diagram | `interaction-flow.md` | User stories, use cases, screen list | `design/flow/interaction-flow.mmd` (Mermaid diagram) |
| Responsive Layout | `responsive-layout.md` | Target screen sizes, window resize requirements | Responsive layout specification embedded in FXML prototypes |
| Icon Resource Management | `icon-management.md` | UI element list, icon style preference | `design/icons/icon-config.json`, Ikonli Maven dependency snippet |

## Workflow

### Step 1: Requirement Analysis & Design Scope

1. **Parse user request**: Extract the application type (CRUD, dashboard, wizard, dialog-heavy, single-window, multi-window), target audience, and design preferences
2. **Identify screens**: From the user description, enumerate all screens/views the application needs (e.g., main window, settings dialog, about dialog, login screen)
3. **Determine design scope**: Based on the request, determine which dimensions to activate:
   - **Full Design** (default): All 5 dimensions — FXML prototypes, CSS themes, interaction flow, responsive layout, icon config
   - **Prototype Only**: Only FXML prototype generation — for quick layout mockups
   - **Theme Only**: Only CSS theme system — for restyling existing projects
   - **Flow Only**: Only interaction flow diagram — for planning user journeys
4. **Declare design scope**: Annotate the design scope in the report header

### Step 2: Interaction Flow Design

1. **Map user journeys**: From the user description, identify primary user flows (e.g., "open app → view dashboard → click edit → modify form → save → confirm")
2. **Generate Mermaid flowchart**: Create a Mermaid flowchart diagram showing screen transitions, decision points, and user actions
3. **Identify navigation patterns**: Determine navigation structure (menu bar, tab pane, breadcrumb, wizard steps)
4. **Output**: Write `design/flow/interaction-flow.mmd` with the Mermaid diagram

```
graph TD
    A[App Start] --> B{Logged in?}
    B -- No --> C[Login Screen]
    C --> D[Authenticate]
    D -- Success --> E[Main Dashboard]
    D -- Failure --> C
    B -- Yes --> E
    E --> F[View Data Table]
    F --> G[Click Edit]
    G --> H[Edit Form]
    H --> I{Save?}
    I -- Yes --> J[Validate Input]
    J -- Valid --> K[Save to DB]
    K --> E
    J -- Invalid --> H
    I -- No --> E
```

### Step 3: FXML Prototype Generation

1. **For each screen** identified in Step 1, generate an FXML prototype:
   - **Layout container selection**: Choose the appropriate root container based on screen type:
     - `BorderPane` — Classic desktop app (menu bar top, content center, status bar bottom)
     - `VBox` / `HBox` — Simple linear layouts, dialogs
     - `GridPane` — Form-heavy screens, settings panels
     - `SplitPane` — Master-detail views, file explorers
     - `TabPane` — Multi-tab interfaces, settings categories
     - `StackPane` — Layered views, card-based UIs
   - **Control placement**: Place controls based on the screen's purpose — toolbar, content area, form fields, action buttons
   - **fx:id assignment**: Assign meaningful `fx:id` values to all interactive controls (buttons, text fields, tables, etc.) for later controller binding
   - **styleClass assignment**: Assign semantic `styleClass` values to all styled elements (e.g., `content-area`, `status-bar`, `button-primary`)
   - **Placeholder content**: Include placeholder text/labels to indicate where dynamic content will go (e.g., `<Label text="Data Table Area"/>`)
2. **Generate control tree preview**: For each FXML file, generate a text-based control tree showing the hierarchy:
   ```
   BorderPane (root)
   ├── MenuBar (top)
   │   ├── Menu "File"
   │   │   ├── MenuItem "New" [fx:id=newMenuItem]
   │   │   └── MenuItem "Open" [fx:id=openMenuItem]
   │   └── Menu "Help"
   ├── VBox (center) [styleClass=content-area]
   │   ├── Label "Content Area" [styleClass=content-title]
   │   └── TableView [fx:id=tableView]
   └── HBox (bottom) [styleClass=status-bar]
       └── Label "Ready" [fx:id=statusLabel]
   ```
3. **Responsive annotations**: Add responsive layout annotations to the FXML (see `responsive-layout.md`):
   - Set `minWidth`/`minHeight` for minimum usable window size
   - Set `prefWidth`/`prefHeight` for default window size
   - Use `AnchorPane` constraints or `BorderPane` auto-resize for responsive behavior
4. **Output**: Write FXML files to `design/fxml/{screen-name}.fxml`

### Step 4: Theme Design System

1. **Define color palette**: Based on the user's brand colors or design preferences:
   - **Primary color**: Main brand color for buttons, links, focus states
   - **Primary dark/light**: Variants for hover/pressed states
   - **Background colors**: Base, elevated (cards), hover
   - **Text colors**: Primary, secondary
   - **Border color**: Subtle separation
   - **Semantic colors**: Success (green), Warning (amber), Danger (red), Info (blue)
2. **Define typography**: Font family, sizes (caption 12px, body 13px, title 18px, headline 24px), weights
3. **Define spacing system**: Consistent padding/margin values (xs 4px, sm 8px, md 12px, lg 16px, xl 24px)
4. **Define border radius**: sm 4px, md 6px, lg 8px
5. **Generate CSS variables**: Define all design tokens as CSS variables in the `.root` selector
6. **Generate light theme**: Write `design/css/light-theme.css` with all component styles (buttons, text fields, labels, tables, menus, scroll bars, etc.)
7. **Generate dark theme**: Write `design/css/dark-theme.css` with inverted color scheme — same structure, different color values
8. **Theme switching snippet**: Provide a Java code snippet for runtime theme switching:
   ```java
   // Toggle between light and dark themes
   scene.getStylesheets().clear();
   scene.getStylesheets().add(isDarkMode
       ? getClass().getResource("/css/dark-theme.css").toExternalForm()
       : getClass().getResource("/css/light-theme.css").toExternalForm());
   ```

### Step 5: Icon Resource Management

1. **Identify icon needs**: From the FXML prototypes, enumerate all UI elements that need icons (menu items, buttons, toolbar actions, tab icons, etc.)
2. **Select icon library**: Recommend an Ikonli icon pack based on the design style:
   - **Material Design** (`org.kordamp.ikonli:ikonli-materialdesign-pack`) — Google Material icons, modern and clean
   - **Font Awesome** (`org.kordamp.ikonli:ikonli-fontawesome-pack`) — Widely recognized, comprehensive
   - **Feather** (`org.kordamp.ikonli:ikonli-feather-pack`) — Minimal, lightweight line icons
   - **Bootstrap** (`org.kordamp.ikonli:ikonli-bootstrapicons-pack`) — Bootstrap icon set
3. **Map icons to UI elements**: Create a mapping table:
   | UI Element | Ikonli Icon | Literal Code |
   |-----------|-------------|-------------|
   | New menu item | `mdal-content_copy` | `\ue14d` |
   | Open menu item | `mdal-folder_open` | `\ue2c8` |
   | Save button | `mdal-save` | `\ue161` |
   | Delete button | `mdal-delete` | `\ue872` |
4. **Generate icon config**: Write `design/icons/icon-config.json` with the icon mapping
5. **Provide Maven dependency**: Output the Ikonli Maven dependency snippet for `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.kordamp.ikonli</groupId>
       <artifactId>ikonli-javafx</artifactId>
       <version>12.3.1</version>
   </dependency>
   <dependency>
       <groupId>org.kordamp.ikonli</groupId>
       <artifactId>ikonli-materialdesign-pack</artifactId>
       <version>12.3.1</version>
   </dependency>
   ```

### Step 6: Design Report Generation

1. **Generate design report**: Following the report template (see `report-templates/design-report.md`), produce a structured design report including:
   - Design scope and dimensions activated
   - Generated artifacts list (file paths)
   - Screen inventory with control counts
   - Theme color palette summary
   - Icon mapping table
   - Design decisions and rationale
2. **Dual Output Format**: Produce both Markdown and JSON outputs (controlled by `.loop-config.json` `output_format` setting):
   - Markdown: `design-report.md` (human-readable, includes Mermaid diagrams and control tree previews)
   - JSON: `design-report.json` (machine-readable, follows `report-schema.json`)
3. **Design handoff summary**: Produce a `design-handoff.json` file that `javafx-developer` consumes:
   ```json
   {
     "screens": [
       {
         "name": "main-view",
         "fxml_path": "design/fxml/main-view.fxml",
         "controller_class": "MainController",
         "controls": ["newMenuItem", "openMenuItem", "tableView", "statusLabel"]
       }
     ],
     "themes": {
       "light": "design/css/light-theme.css",
       "dark": "design/css/dark-theme.css"
     },
     "icons": {
       "config": "design/icons/icon-config.json",
       "library": "ikonli-materialdesign-pack",
       "version": "12.3.1"
     },
     "interaction_flow": "design/flow/interaction-flow.mmd"
   }
   ```

## Design Artifacts Structure

After Designer completes, the following artifacts are produced in the project root:

```
design/
├── fxml/
│   ├── main-view.fxml          # Main window prototype
│   ├── settings-dialog.fxml    # Settings dialog prototype
│   └── about-dialog.fxml       # About dialog prototype
├── css/
│   ├── light-theme.css         # Light theme stylesheet
│   └── dark-theme.css          # Dark theme stylesheet
├── flow/
│   └── interaction-flow.mmd    # Mermaid interaction flow diagram
├── icons/
│   └── icon-config.json        # Icon mapping configuration
├── design-report.md            # Design report (Markdown)
├── design-report.json          # Design report (JSON)
└── design-handoff.json         # Handoff file for javafx-developer
```

## Constraints

- **No production code**: Designer does NOT generate Java controller logic, service classes, or business code — only FXML, CSS, Mermaid, and JSON artifacts
- **FXML must be valid**: All generated FXML files must be well-formed XML with correct `<?import>` declarations and valid JavaFX control elements
- **CSS must use variables**: All CSS files must define design tokens as CSS variables in the `.root` selector — no hardcoded color values in component styles
- **Light and dark themes must be structurally identical**: Same selectors, same variable names, only color values differ
- **fx:id must be unique**: Within a single FXML file, all `fx:id` values must be unique
- **Mermaid syntax must be valid**: The interaction flow diagram must use valid Mermaid flowchart syntax (`graph TD` / `graph LR`)
- **Icon literals must be accurate**: Ikonli icon literal codes must match the selected icon pack's actual codes

## Loop Orchestration Protocol

### Designer's Role in the Loop

Designer operates as an **optional pre-loop phase** — it runs before the generate → review → verify → test → fix cycle begins:

```
[Start] → (optional) Designing → Generating → Reviewing ∥ Verifying → Combined Gate → Test Gate → DocGen → [Delivered]
```

- **When triggered**: User explicitly requests design ("design and generate", "create a prototype first") or `.loop-config.json` has `"design_phase": true`
- **When skipped**: User requests direct code generation ("create a JavaFX app that...") without design intent — developer uses its own built-in templates
- **Standalone mode**: Designer can run without the loop — user reviews design artifacts, iterates, then separately triggers developer when satisfied

### Design Handoff Protocol

When Designer completes, it produces `design-handoff.json` which developer consumes in Step 4:

1. **FXML prototypes**: Developer reads `design/fxml/*.fxml` files and uses them as the FXML templates instead of its own built-in `templates/fxml/*.fxml`
2. **CSS themes**: Developer reads `design/css/*.css` files and copies them to `src/main/resources/css/` instead of using its own built-in CSS templates
3. **Icon config**: Developer reads `design/icons/icon-config.json` and adds the Ikonli dependency to `pom.xml`, configures icon font loading
4. **Interaction flow**: Developer uses the interaction flow diagram to understand screen transitions and implement navigation logic

### State Machine Integration

Designer adds an optional `designing` state to the loop state machine:

```
status: "designing" → "generating" → "reviewing_and_verifying" → ...
```

In `.loop-state.json`:
```json
{
  "status": "designing",
  "design_result": {
    "triggered": true,
    "dimensions": ["prototype", "theme", "flow", "responsive", "icons"],
    "screens_designed": 3,
    "artifacts": ["design/fxml/main-view.fxml", "design/css/light-theme.css", ...],
    "handoff_file": "design/design-handoff.json",
    "timestamp": "2026-06-29T10:00:00Z"
  }
}
```

## Reference Documents

For in-depth guidance, refer to these documents in the `references/` directory:

- `references/prototype-design.md` — FXML prototype generation rules, layout container selection guide, control placement patterns, control tree preview format
- `references/theme-system.md` — CSS theme design system, color palette definition, typography scale, spacing system, light/dark theme generation rules
- `references/interaction-flow.md` — Interaction flow diagram generation, Mermaid syntax guide, user journey mapping, navigation pattern selection
- `references/responsive-layout.md` — Responsive layout guidance, AnchorPane constraints, min/pref/max sizing, window resize behavior
- `references/icon-management.md` — Ikonli icon library selection, icon mapping, Maven dependency configuration, icon literal reference

## Relationship to Other Skills

| Skill | Relationship |
|-------|-------------|
| `javafx-developer` | **Downstream consumer**: Developer consumes Designer's FXML prototypes, CSS themes, and icon configs in Step 4. If Designer ran, Developer skips its own template-based FXML/CSS generation and uses Designer's artifacts instead |
| `javafx-orchestrator` | **Coordinator**: Orchestrator triggers Designer as an optional pre-generation phase when the user requests "design and generate". Orchestrator manages the design → generate handoff |
| `javafx-code-reviewer` | **No direct interaction**: Reviewer reviews the final code (after developer generates from designer's artifacts). Reviewer's FXML Standards dimension validates the FXML that originated from Designer |
| `javafx-runner` | **No direct interaction**: Runner verifies the compiled project. Designer's FXML must be valid for the project to compile |
| `javafx-docgen` | **No direct interaction**: DocGen generates documentation from the final code. Designer's interaction flow diagram may be referenced in the user manual |

## EVALUATE.md

See `EVALUATE.md` for evaluation test cases that quantify design output quality.
