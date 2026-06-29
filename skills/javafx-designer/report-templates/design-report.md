# Design Report

> **Project**: {{PROJECT_NAME}}  
> **Design Scope**: {{DESIGN_SCOPE}} (Full Design / Prototype Only / Theme Only / Flow Only)  
> **Generated At**: {{TIMESTAMP}}

## 1. Design Summary

| Item | Value |
|------|-------|
| Application Type | {{APP_TYPE}} (CRUD / Dashboard / Wizard / Dialog-heavy / Single-window / Multi-window) |
| Screens Designed | {{SCREEN_COUNT}} |
| Dimensions Activated | {{DIMENSIONS}} |
| Design Artifacts | {{ARTIFACT_COUNT}} files |
| Conclusion | {{CONCLUSION}} (Pass / Pass with warnings / Fail) |

## 2. Screen Inventory

| Screen Name | FXML File | Root Container | Control Count | fx:id Count |
|-------------|-----------|----------------|---------------|-------------|
| {{SCREEN_NAME}} | `design/fxml/{{SCREEN_FILE}}` | {{ROOT_CONTAINER}} | {{CONTROL_COUNT}} | {{FXID_COUNT}} |

### Control Tree Preview

{{CONTROL_TREE_PREVIEW}}

## 3. Interaction Flow

**Navigation Pattern**: {{NAVIGATION_PATTERN}} (Menu Bar / Tab Pane / Wizard Steps / Breadcrumb / Drawer)

```mermaid
{{MERMAID_DIAGRAM}}
```

## 4. Theme Design

### Color Palette

| Token | Light Theme | Dark Theme | Usage |
|-------|-------------|------------|-------|
| -fx-primary | #{{LIGHT_PRIMARY}} | #{{DARK_PRIMARY}} | Buttons, links, focus |
| -fx-bg-base | #{{LIGHT_BG_BASE}} | #{{DARK_BG_BASE}} | Main background |
| -fx-text-primary | #{{LIGHT_TEXT_PRIMARY}} | #{{DARK_TEXT_PRIMARY}} | Primary text |
| -fx-success | #{{SUCCESS}} | #{{SUCCESS}} | Success states |
| -fx-danger | #{{DANGER}} | #{{DANGER}} | Error states |

### Typography

| Level | Size | Weight | Usage |
|-------|------|--------|-------|
| Caption | 12px | Normal | Helper text, status bar |
| Body | 13px | Normal | Default text |
| Title | 18px | Bold | Section headers |
| Headline | 24px | Bold | Window titles |

### Theme Files

| Theme | File Path |
|-------|-----------|
| Light | `design/css/light-theme.css` |
| Dark | `design/css/dark-theme.css` |

## 5. Icon Configuration

**Icon Library**: {{ICON_LIBRARY}} (Ikonli {{ICON_PACK}} v{{ICON_VERSION}})

| UI Element | fx:id | Icon Literal | Icon Code |
|-----------|-------|-------------|-----------|
| {{UI_ELEMENT}} | {{FXID}} | {{ICON_LITERAL}} | {{ICON_CODE}} |

**Maven Dependencies**:
```xml
{{IKONLI_DEPENDENCIES}}
```

## 6. Responsive Layout

| Breakpoint | Width Range | Layout Behavior |
|------------|-------------|-----------------|
| Compact | < 600px | {{COMPACT_BEHAVIOR}} |
| Medium | 600-1024px | {{MEDIUM_BEHAVIOR}} |
| Large | > 1024px | {{LARGE_BEHAVIOR}} |

**Min Window Size**: {{MIN_WIDTH}} x {{MIN_HEIGHT}}  
**Default Window Size**: {{PREF_WIDTH}} x {{PREF_HEIGHT}}

## 7. Generated Artifacts

| Artifact | Path | Type |
|----------|------|------|
| {{ARTIFACT_NAME}} | `{{ARTIFACT_PATH}}` | {{ARTIFACT_TYPE}} |

## 8. Design Handoff

**Handoff File**: `design/design-handoff.json`

```json
{{DESIGN_HANDOFF_JSON}}
```

## 9. Warnings

{{WARNINGS}}

(If no warnings, output "No warnings.")

## 10. Loop State

- **Next Action**: {{NEXT_ACTION}} (generating | standalone_complete | redesign)
- **Design Phase**: {{DESIGN_PHASE}} (pre-generation | standalone | redesign)
