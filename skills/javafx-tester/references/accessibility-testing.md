# Accessibility Testing Rules

This document defines the accessibility testing rules, WCAG 2.1 criteria, and check methodologies for JavaFX applications. It serves as the reference for `javafx-tester`'s Accessibility Testing dimension.

## 1. Keyboard Navigation

### 1.1 Tab Order Traversal

**Definition**: All interactive controls must be reachable via the Tab key in a logical order.

**Test methodology**:
1. Launch the application
2. Press Tab repeatedly from the first focusable control
3. Record the order of focused controls
4. Verify:
   - All interactive controls (Button, TextField, ComboBox, TableView, CheckBox, RadioButton, etc.) are reachable
   - Tab order follows visual reading order (top-to-bottom, left-to-right) or logical grouping
   - Non-interactive elements (Label, ImageView, Separator) are not in the tab order (unless they have `setFocusTraversable(true)` for a reason)
   - `Tab` and `Shift+Tab` both work correctly

**Threshold evaluation**:

| Condition | Severity | Fix |
|-----------|----------|-----|
| All controls reachable, logical order | Pass | No action |
| Some controls not in tab order | Major | Set `setFocusTraversable(true)` or adjust `focusTraversable` in FXML |
| Tab order illogical (jumps randomly) | Minor | Set explicit `focusTraversable` order or use `setOnKeyPressed` |
| No keyboard navigation possible | Critical | All mouse-only interactions must have keyboard equivalents |

### 1.2 Focus Visibility

**Definition**: The currently focused control must have a visible focus indicator.

**Test methodology**:
1. Tab through all controls
2. Visually verify each focused control has a visible focus ring or border
3. Check CSS for focus removal:
   ```css
   /* Problematic: removes focus indicator entirely */
   .button:focused {
       -fx-focus-color: transparent;
       -fx-faint-focus-color: transparent;
   }
   ```

**Threshold evaluation**:

| Condition | Severity | Fix |
|-----------|----------|-----|
| Default focus ring visible | Pass | No action |
| Custom focus styling, visible | Pass | Acceptable |
| Focus ring removed, no replacement | Minor | Replace with visible custom focus style |
| Focus ring removed on critical controls (submit, delete) | Major | Must have visible focus indicator |

### 1.3 Keyboard Shortcuts

**Definition**: All functionality accessible via mouse must also be accessible via keyboard.

**Test methodology**:
1. Identify all mouse-only interactions (context menus, drag-and-drop, double-click)
2. Verify keyboard equivalents exist:
   - Context menu: `Shift+F10` or dedicated menu bar
   - Double-click: `Enter` or `Space`
   - Drag-and-drop: Cut/Copy/Paste (`Ctrl+X/C/V`)
3. Verify `KeyCodeCombination` shortcuts are properly registered

**Threshold evaluation**:

| Condition | Severity | Fix |
|-----------|----------|-----|
| All interactions have keyboard equivalents | Pass | No action |
| Some interactions missing keyboard equivalent | Minor | Add keyboard shortcut or alternative |
| Critical interactions (save, delete) mouse-only | Major | Must add keyboard shortcut |

## 2. Color Contrast

### 2.1 Contrast Ratio Calculation

**WCAG 2.1 formula**: `(L1 + 0.05) / (L2 + 0.05)` where:
- L1 = relative luminance of the lighter color
- L2 = relative luminance of the darker color
- Relative luminance: `L = 0.2126 * R + 0.7152 * G + 0.0722 * B` (where R, G, B are gamma-corrected)

**Java implementation**:
```java
public static double contrastRatio(Color c1, Color c2) {
    double l1 = relativeLuminance(c1);
    double l2 = relativeLuminance(c2);
    return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
}

private static double relativeLuminance(Color c) {
    double r = c.getRed() <= 0.03928 ? c.getRed() / 12.92 : Math.pow((c.getRed() + 0.055) / 1.055, 2.4);
    double g = c.getGreen() <= 0.03928 ? c.getGreen() / 12.92 : Math.pow((c.getGreen() + 0.055) / 1.055, 2.4);
    double b = c.getBlue() <= 0.03928 ? c.getBlue() / 12.92 : Math.pow((c.getBlue() + 0.055) / 1.055, 2.4);
    return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}
```

### 2.2 Normal Text Contrast

**Definition**: Text with font-size < 18pt (or < 14pt bold).

**Test methodology**:
1. Parse CSS files for `-fx-text-fill` (foreground) and background color declarations
2. Parse inline styles in FXML (`style="-fx-text-fill: #xxx"`)
3. Calculate contrast ratio for each text/background combination
4. Check default JavaFX theme colors (Modena) if no custom CSS is applied

**Threshold (WCAG 2.1 AA)**:

| Contrast Ratio | Severity | Fix |
|---------------|----------|-----|
| ≥ 4.5:1 | Pass | No action |
| 3.0:1 - 4.5:1 | Major | Increase contrast (darken text or lighten background) |
| < 3.0:1 | Critical | Text is very difficult to read for visually impaired users |

### 2.3 Large Text Contrast

**Definition**: Text with font-size ≥ 18pt (or ≥ 14pt bold).

**Threshold (WCAG 2.1 AA)**:

| Contrast Ratio | Severity | Fix |
|---------------|----------|-----|
| ≥ 3.0:1 | Pass | No action |
| < 3.0:1 | Minor | Increase contrast |

### 2.4 Non-Text Contrast (WCAG 2.1 AA - 1.4.11)

**Definition**: UI components and graphical objects must have sufficient contrast against adjacent colors.

**Applies to**:
- Button borders against background
- Input field borders
- Focus indicators
- Icons and meaningful graphics

**Threshold**: ≥ 3.0:1 contrast ratio.

### 2.5 Contrast Cross-References

When contrast fails, cross-reference:
- `../javafx-code-reviewer/references/css-compliance.md` -- Color Contrast (static CSS check)

## 3. Screen Reader Compatibility

### 3.1 Accessible Text

**Definition**: Interactive controls should have `accessibleText` property set for screen reader announcement.

**Test methodology**:
1. Scan FXML files for controls missing `accessibleText`:
   ```xml
   <!-- Missing accessibleText -->
   <Button text="Save" onAction="#handleSave"/>

   <!-- With accessibleText -->
   <Button text="Save" onAction="#handleSave" accessibleText="Save current changes to database"/>
   ```
2. Scan Java code for `setAccessibleText()` calls
3. Calculate coverage: (controls with accessibleText) / (total interactive controls)

**Threshold evaluation**:

| Coverage | Severity | Fix |
|---------|----------|-----|
| 100% | Pass | All controls have accessible text |
| 70-99% | Minor | Most controls have accessible text, add missing ones |
| 40-69% | Major | Many controls missing accessible text |
| < 40% | Major | Screen reader users cannot effectively use the application |

### 3.2 Accessible Help

**Definition**: Controls should have `accessibleHelp` property for extended descriptions.

**Test methodology**: Same as accessibleText, but check for `accessibleHelp` property.

**Threshold**: Same as accessibleText, but severity is one level lower (Pass → Pass, Minor → Pass, Major → Minor).

### 3.3 Image Alternative Text

**Definition**: `ImageView` elements that convey information should have `accessibleText`.

**Test methodology**:
1. Scan FXML for `ImageView` elements
2. Check if `accessibleText` is set
3. Exclude decorative images (those with `managed="false"` or in decorative containers)

**Threshold**:

| Condition | Severity | Fix |
|-----------|----------|-----|
| All informational images have accessibleText | Pass | No action |
| Some informational images missing accessibleText | Minor | Add accessibleText |
| Decorative images correctly have no accessibleText | Pass | Correct (screen readers skip decorative images) |

### 3.4 Live Region Support

**Definition**: Dynamic content updates should be announced to screen reader users.

**Test methodology**:
1. Identify dynamic content areas (status messages, progress indicators, notification toasts)
2. Check if these elements have `accessibleRole` set (e.g., `ACCESSIBLE_STATUS_BAR` for status messages)
3. Check if updates trigger accessibility notifications via `AccessibilityProvider`

**Threshold**:

| Condition | Severity | Fix |
|-----------|----------|-----|
| Dynamic content has proper accessibleRole | Pass | No action |
| Dynamic content missing accessibleRole | Minor | Add accessibleRole and notification |
| Status messages not announced | Major | Screen reader users miss important updates |

## 4. Color-Independent Information

### 4.1 Color as Sole Indicator

**Definition**: Information must not be conveyed by color alone (WCAG 2.1 - 1.4.1).

**Test methodology**:
1. Identify color-coded information (e.g., red text for errors, green for success)
2. Verify each color-coded element also has text or icon indicator:
   - Error messages: "Error:" prefix or error icon in addition to red color
   - Success messages: "Success:" prefix or checkmark icon in addition to green color
   - Status indicators: text label in addition to colored dot

**Threshold**:

| Condition | Severity | Fix |
|-----------|----------|-----|
| All color-coded info has text/icon alternative | Pass | No action |
| Some color-only indicators | Major | Add text or icon alongside color |
| Critical information color-only | Critical | Colorblind users cannot perceive the information |

## 5. Resize and Reflow

### 5.1 Content Reflow

**Definition**: Content must remain usable when the window is resized to 320px width (WCAG 2.1 - 1.4.10).

**Test methodology**:
1. Resize the application window to 320x256 pixels
2. Verify all content remains accessible (no horizontal scroll required for primary content)
3. Verify no overlapping or clipped text

**Threshold**:

| Condition | Severity | Fix |
|-----------|----------|-----|
| All content accessible at 320px | Pass | No action |
| Some content clipped but still accessible | Minor | Adjust layout constraints |
| Critical content inaccessible at 320px | Major | Use responsive layout (AnchorPane constraints, FlowPane) |

## 6. WCAG 2.1 AA Mapping for JavaFX

This section provides a consolidated mapping between WCAG 2.1 AA success criteria and the concrete JavaFX API features that satisfy them, together with the test method used to verify each. Use this table as the authoritative cross-reference when reporting accessibility findings: every accessibility defect should cite both the WCAG criterion and the JavaFX mechanism that failed.

| WCAG Criterion | JavaFX Implementation | Test Method |
|----------------|-----------------------|-------------|
| 1.1.1 Non-text Content | `AccessibleText` / `AccessibleRole` on `ImageView` and icon-only controls | Verify `accessibleText` set on all `Image`/`ImageView` and on `Button`/`Label` that use only graphic content; scan FXML for `ImageView` nodes lacking `accessibleText` (see section 3.3) |
| 1.3.1 Info and Relationships | `AccessibleAttribute.PARENT` / `CHILDREN` exposed via `AccessibleAttribute` enumeration; container roles (`ACCESSIBLE_PARENT`) preserve tree structure | Verify the accessible tree structure matches the visual hierarchy: query `Node.queryAccessibleAttribute(AccessibleAttribute.PARENT)` / `CHILDREN` and assert the parent/child relationships mirror the scene graph; verify `Label`/`TextField` pairing via `labelFor` is reflected in the accessible tree |
| 1.4.3 Contrast (Minimum) | CSS `-fx-text-fill` and `-fx-background-color` color values | Verify contrast ratio >= 4.5:1 for normal text and >= 3.0:1 for large text using the calculation in section 2.1; parse CSS and FXML inline styles for color pairs (see section 2.2) |
| 2.1.1 Keyboard | `setOnKeyPressed` / `setOnKeyTyped` handlers, `KeyCodeCombination`, `Mnemonics` | Verify all actions exposed via mouse are also reachable via keyboard; press Tab/Enter/Space/Shift+F10 and confirm equivalent behavior; confirm no mouse-only interaction paths exist (see section 1.3) |
| 2.4.3 Focus Order | `focusTraversable` property + custom traversal via `KeyEvent` handling or `TraversalEngine` | Verify Tab order is logical: Tab from the first focusable control and record the visited sequence; confirm it follows reading order (top-to-bottom, left-to-right) and that non-interactive nodes have `focusTraversable=false` (see section 1.1) |
| 3.3.2 Labels or Instructions | `Label.labelFor` property binding the `Label` to its associated input control | Verify every `TextField`, `ComboBox`, `TextArea`, `PasswordField`, `ChoiceBox`, `Spinner`, and `DatePicker` has an associated `Label` with `labelFor` set (or an `accessibleText` fallback); scan FXML for inputs lacking `labelFor` binding |
| 4.1.2 Name, Role, Value | `AccessibleRole` (role) + `AccessibleText` (name) + value-exposing attributes (`AccessibleAttribute.VALUE`, `TEXT`, `SELECTED`, etc.) | Verify the screen reader announces the correct name, role, and current value for each control: enable the platform screen reader (NVDA/VoiceOver/Orca) and traverse the UI; alternatively assert `accessibleRole`, `accessibleText`, and `queryAccessibleAttribute(VALUE)` programmatically |

### 6.1 Using the Mapping in Reports

When the tester emits an accessibility finding, it should reference the WCAG criterion number (e.g., "WCAG 1.4.3") in addition to the JavaFX-specific detail. This allows the finding to be aggregated into a standards-compliance dashboard alongside web and mobile findings, and lets the reviewer cross-reference the static accessibility rules that map to the same criterion.

## 7. Screen Reader Compatibility Matrix

JavaFX relies on the platform's native accessibility bridge (Windows: UI Automation / MSAA; macOS: NSAccessibility; Linux: ATK/AT-SPI). Compatibility varies by screen reader and operating system. The matrix below records the known support level and issues for each combination, to be used when interpreting screen-reader test results.

| Screen Reader | OS | JavaFX Support | Known Issues |
|---------------|----|----------------|--------------|
| NVDA | Windows | Good via `AccessibleRole` and `AccessibleText` | `TableView` cell reading requires `accessibleText` on cells; without it NVDA announces only the row index, not cell content. Custom cell factories must set `accessibleText` per cell. Live region announcements require `ACCESSIBLE_STATUS_BAR` role. |
| VoiceOver | macOS | Good | Tab key traversal differs from Windows: VoiceOver intercepts Tab for its own navigation; use `VO+Right Arrow` for element traversal. `AccessibleText` is read correctly but `accessibleHelp` is announced only after a delay. ComboBox expansion is not always announced — wrap with `ACCESSIBLE_COMBO_BOX` role explicitly. |
| Orca | Linux | Limited | Requires the `-Djavafx.accessible=true` JVM flag to enable the ATK bridge; without it Orca sees no accessible tree at all. Even with the flag, custom controls are frequently invisible to Orca; `AccessibleRole` mappings are incomplete for some JavaFX controls (e.g., `Spinner`, `Pagination`). |

### 7.1 Test Guidance per Reader

- **NVDA (Windows)**: Primary test target. Use NVDA's element list (`Insert+F7`) to verify all controls appear with correct name/role. Run the focus-order test from section 1.1 while NVDA is active to confirm spoken order matches Tab order.
- **VoiceOver (macOS)**: Use `VO+A` to read the entire window top-to-bottom and confirm content order matches the visual reading order. Note the Tab-traversal difference and document expected keyboard shortcuts accordingly.
- **Orca (Linux)**: Always launch the JVM with `-Djavafx.accessible=true`. Treat Orca failures with caution — some are platform-bridge gaps rather than application defects; cross-check the same control with NVDA/VoiceOver before marking Critical.

### 7.2 Cross-References

When a screen-reader compatibility issue is rooted in missing JavaFX accessibility properties rather than the bridge itself, cross-reference:
- `../javafx-code-reviewer/references/accessibility-guide.md` -- AccessibleRole / AccessibleText (static checks for missing properties)
- `../javafx-code-reviewer/references/css-compliance.md` -- Color Contrast (static CSS check backing criterion 1.4.3)
