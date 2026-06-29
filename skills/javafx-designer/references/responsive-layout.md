# Responsive Layout Guidance

JavaFX does not support CSS media queries, so responsive behaviour is achieved through property listeners, bindings, layout constraints, and size policies. This document defines the strategies, sizing rules, container behaviours, listener patterns, breakpoints, and common adaptive patterns used in generated FXML.

## JavaFX Responsive Strategies

| Strategy | Mechanism | When to Apply |
|----------|-----------|---------------|
| Layout constraints | `AnchorPane` anchors, `BorderPane` regions | Let controls stretch with the window |
| Bindings | `widthProperty` / `heightProperty` bindings | Dynamically compute sizes |
| Listeners | `addListener` on scene dimensions | Toggle UI structure at breakpoints |
| Size policies | `minWidth` / `prefWidth` / `maxWidth` | Constrain controls within containers |
| SplitPane dividers | `setResizable` + `dividerPositions` | Allocate space between regions |

There are **no media queries** in JavaFX CSS. All adaptive logic lives in FXML attributes or Java controller code.

## Min / Pref / Max Sizing

Every resizable root should declare three size tiers so the window behaves predictably.

| Property | Purpose | Typical Value |
|----------|---------|---------------|
| `minWidth` / `minHeight` | Smallest usable window before UI breaks | 600 x 400 |
| `prefWidth` / `prefHeight` | Default window size on first show | 1024 x 768 |
| `maxWidth` / `maxHeight` | Largest window (use `MAX_VALUE` for unlimited) | `Infinity` |

```xml
<BorderPane minWidth="600.0" minHeight="400.0"
            prefWidth="1024.0" prefHeight="768.0"
            maxWidth="Infinity" maxHeight="Infinity">
</BorderPane>
```

Guidelines:
- Set `minWidth`/`minHeight` on the **root** element so the OS prevents shrinking below usable bounds.
- Set `prefWidth`/`prefHeight` so the stage opens at a comfortable default.
- Avoid setting `maxWidth`/`maxHeight` unless the design requires a fixed maximum (e.g., a dialog).

## AnchorPane Constraints

`AnchorPane` lets individual controls stretch to fill available space by anchoring opposite edges. A control anchored on both left and right stretches horizontally; both top and bottom stretches vertically.

| Constraint | Effect |
|-----------|--------|
| `AnchorPane.leftAnchor` | Pin left edge; control grows rightward |
| `AnchorPane.rightAnchor` | Pin right edge; control grows leftward |
| `AnchorPane.topAnchor` | Pin top edge |
| `AnchorPane.bottomAnchor` | Pin bottom edge |

```xml
<AnchorPane>
    <TableView fx:id="table" AnchorPane.leftAnchor="16.0"
               AnchorPane.rightAnchor="16.0"
               AnchorPane.topAnchor="16.0"
               AnchorPane.bottomAnchor="16.0"/>
</AnchorPane>
```

A common `16.0` margin on all four sides creates consistent content padding while letting the table fill the window. Set only the anchors you need: pinning one edge keeps the control fixed on that side and free on the other.

## BorderPane Auto-Resize Behavior

`BorderPane` divides the window into five regions with deterministic resize rules.

| Region | Resize Behavior | Typical Content |
|--------|-----------------|-----------------|
| `center` | Grows in both directions to fill remaining space | Tables, editors, charts |
| `top` | Fixed height, full width | MenuBar, ToolBar |
| `bottom` | Fixed height, full width | StatusBar |
| `left` | Fixed width, full height | Navigation sidebar |
| `right` | Fixed width, full height | Detail / inspector panel |

Children placed in `top`/`bottom` should set an explicit `prefHeight`; children in `left`/`right` should set an explicit `prefWidth`. The center child must not set fixed sizes so it can absorb growth.

## SplitPane Resizable Panels

`SplitPane` divides space between two or more panels with a draggable divider.

| Property | Purpose |
|-----------|---------|
| `setResizable(true/false)` | Allow/disallow a panel to resize with the window |
| `dividerPositions` | Initial position of each divider (0.0–1.0) |
| `SplitPane.setDividerPosition()` | Update at runtime |

```xml
<SplitPane fx:id="split" dividerPositions="0.3" orientation="HORIZONTAL">
    <items>
        <ListView fx:id="navList" minWidth="180.0"/>
        <BorderPane fx:id="detail"/>
    </items>
</SplitPane>
```

Use `minWidth` on the master panel so it never collapses below a usable width. Set `dividerPositions` so the master-detail ratio is sensible at the default window size.

## Window Resize Listener Pattern

For structural adaptation that constraints alone cannot express, attach a listener to the scene's width property and switch layouts at breakpoints.

```java
scene.widthProperty().addListener((obs, oldVal, newVal) -> {
    double width = newVal.doubleValue();
    if (width < 600) {
        applyCompactLayout();   // hide sidebar, stack toolbar
    } else if (width < 1024) {
        applyMediumLayout();    // collapsed sidebar, full toolbar
    } else {
        applyLargeLayout();     // expanded sidebar, multi-column
    }
});
```

Best practices:
- Keep the listener lightweight — toggle visibility or swap managed/unmanaged, do not rebuild the scene graph.
- Use `setManaged(false)` together with `setVisible(false)` to remove a node from layout calculations entirely.
- Debounce rapid resize events if logic is expensive.

## Responsive Breakpoints

Adopt three breakpoints mirroring common web practice, expressed in pixels.

| Breakpoint | Width Range | Layout Intent |
|-----------|-------------|---------------|
| Compact | < 600 px | Single column, hidden sidebar, stacked toolbar |
| Medium | 600–1024 px | Collapsed sidebar (icons only), single content column |
| Large | > 1024 px | Expanded sidebar, multi-column dashboard, wide tables |

Map each breakpoint to a Java method that adjusts the scene graph (see listener pattern above).

## Common Responsive Patterns

### Sidebar Collapse
At compact width, hide or collapse the left navigation into an icon-only rail.

```java
void applyCompactLayout() {
    navSidebar.setManaged(false);
    navSidebar.setVisible(false);
    toolbar.setWrapText(true);
}
```

### Toolbar Wrap
Allow a `ToolBar` to wrap its items instead of overflowing off-screen. There is no built-in wrap, so the workaround is to split items into multiple `ToolBar` rows inside a `VBox` and toggle their visibility at breakpoints.

### Table Column Hide
Hide low-priority columns on narrow windows to keep the primary columns readable.

```java
void applyCompactLayout() {
    emailColumn.setVisible(false);
    phoneColumn.setVisible(false);
}
void applyLargeLayout() {
    emailColumn.setVisible(true);
    phoneColumn.setVisible(true);
}
```

### Adaptive Form Layout
On large windows, place label and field side-by-side in a `GridPane` (two columns). On compact windows, switch to a single-column `VBox` where labels sit above fields. Implement by toggling which parent holds the fields, or by changing `GridPane` column constraints.

## Responsive Validation Checklist

| # | Check |
|---|-------|
| 1 | Root declares `minWidth` and `minHeight` |
| 2 | Root declares `prefWidth` and `prefHeight` |
| 3 | Center region of BorderPane has no fixed size |
| 4 | AnchorPane stretch controls anchored on opposing edges |
| 5 | SplitPane panels have `minWidth` preventing collapse |
| 6 | Scene width listener covers all three breakpoints |
| 7 | Hidden nodes use both `setManaged(false)` and `setVisible(false)` |
| 8 | No hardcoded pixel widths on content that should grow |
