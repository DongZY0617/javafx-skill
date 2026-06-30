# Custom Controls Development Reference

> Patterns for building reusable custom JavaFX controls using the Control/Skin architecture. Covers property definition with JavaFX Beans conventions, CSS stylable properties, Canvas-based rendering, event handling, and a complete RatingControl example (Control + Skin + CSS).

## When to Build a Custom Control

Build a custom control when standard JavaFX UI controls (`Button`, `TableView`, `ComboBox`, etc.) cannot meet your needs:

| Scenario | Recommended Approach |
|----------|---------------------|
| Compose existing controls in a reusable layout | **Custom Region/Pane** (extend `StackPane`, `HBox`, etc.) — simpler, no Skin needed |
| New interactive behavior + custom visual rendering | **Control + Skin** — full power of CSS styling and behavior separation |
| High-performance custom drawing (charts, gauges) | **Canvas-based Control + Skin** — GPU-accelerated pixel rendering |
| Replace the visual appearance of an existing control | **Skin override** — implement a custom Skin for an existing Control |

**Rule of thumb**: If you only need to compose existing controls, extend `Region` or a layout pane. If you need custom rendering, CSS-stylable properties, or novel interaction behavior, use the full Control/Skin pattern.

## Control / Skin Architecture

### Separation of Concerns

JavaFX follows a strict MVC-like separation:

```
┌─────────────────────────────────────────────────┐
│  Control (Model + Controller)                   │
│  ├── Public API (properties, methods)           │
│  ├── CSS-stylable properties                    │
│  └── No rendering logic                         │
├─────────────────────────────────────────────────┤
│  Skin (View)                                    │
│  ├── Visual rendering (nodes or canvas)         │
│  ├── Event handling (mouse, keyboard)           │
│  ├── Layout logic                               │
│  └── Bound to Control's properties              │
├─────────────────────────────────────────────────┤
│  CSS (Style)                                    │
│  ├── Property style declarations                │
│  ├── Pseudo-class states (:hover, :pressed)     │
│  └── Loaded via getUserAgentStylesheet()        │
└─────────────────────────────────────────────────┘
```

- **Control** (`extends Control`): Defines the public API — properties, methods, CSS metadata. Contains NO rendering logic. Think of it as the "interface" that users of your control interact with.
- **Skin** (`extends SkinBase<MyControl>`): Implements the visual rendering and interaction behavior. Listens to the Control's properties and updates the view when they change. Think of it as the "implementation" of the visual layer.
- **CSS**: Styles the control's properties and pseudo-classes, allowing theme customization without code changes.

### Why Not Just Extend Region?

| Feature | Region/Pane | Control + Skin |
|---------|-------------|----------------|
| CSS-stylable custom properties | Limited (only standard properties) | Full support (define your own `-fx-my-color`) |
| Pseudo-class states (:hover, :disabled) | Standard only | Custom pseudo-classes |
| Focus traversal | Manual | Built-in |
| Skinning/theme swapping | Not possible | Yes — swap Skin at runtime |
| Tooltips, context menus | Manual | Built-in API |
| Complexity | Low | Medium |

## JavaFX Properties (Beans Convention)

Custom controls expose state through JavaFX properties. Every public property follows the standard three-method pattern:

```java
// Private field with lazy initialization
private DoubleProperty rating;

// Getter (returns the property, never null)
public final DoubleProperty ratingProperty() {
    if (rating == null) {
        rating = new SimpleDoubleProperty(this, "rating", 0.0);
    }
    return rating;
}

// Setter (delegates to property)
public final void setRating(double value) {
    ratingProperty().set(value);
}

// Value getter
public final double getRating() {
    return rating == null ? 0.0 : rating.get();
}
```

### Rules

1. **`final` getters/setters**: Property getter/setter methods must be `final` to prevent subclassing from breaking the property contract
2. **Lazy initialization**: The property field is `null` until first accessed — saves memory for unused properties
3. **Property getter never null**: `ratingProperty()` must always return a non-null `Property` object (initialize on first access)
4. **Constructor name**: Pass the bean name (string) as the second argument to `SimpleXxxProperty` — this is used by CSS and tools

### Read-Only Properties

For computed or internal state that shouldn't be set externally:

```java
private ReadOnlyDoubleWrapper percentage;  // wrapper, not the read-only version

public final ReadOnlyDoubleProperty percentageProperty() {
    if (percentage == null) {
        percentage = new ReadOnlyDoubleWrapper(this, "percentage", 0.0);
    }
    return percentage.getReadOnlyProperty();  // expose read-only view
}

public final double getPercentage() {
    return percentage == null ? 0.0 : percentage.get();
}
// No setter — external code cannot change this
```

## CSS-Stylable Properties

Custom controls can define their own CSS properties (e.g., `-fx-star-color`) that designers can style via CSS without touching Java code.

### Step 1: Define StyleableProperty Fields

```java
// StyleableProperty fields — CSS-stylable versions of regular properties
private StyleableObjectProperty<Paint> starColor;
private StyleableObjectProperty<Paint> starBorderColor;
private StyleableDoubleProperty starSize;
```

### Step 2: Define CssMetaData

Each CSS property needs `CssMetaData` that tells JavaFX how to apply the CSS value to the property:

```java
// CSS metadata for -fx-star-color
private static final CssMetaData<RatingControl, Paint> STAR_COLOR =
        new CssMetaData<RatingControl, Paint>("-fx-star-color",
                StyleConverter.getPaintConverter(), Color.GOLD) {

            @Override
            public boolean isSettable(RatingControl control) {
                return control.starColor == null || !control.starColor.isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(RatingControl control) {
                return control.starColorProperty();
            }
        };
```

- **`isSettable`**: Returns `true` if the CSS can set this property (i.e., it's not bound). If the developer programmatically bound the property, CSS won't override it.
- **`getStyleableProperty`**: Returns the actual `StyleableProperty` field from the control instance.
- **Default value** (`Color.GOLD`): Used when no CSS rule specifies the property.

### Step 3: Aggregate CssMetaData

Collect all CSS metadata into a list, including the parent class's metadata:

```java
private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

static {
    List<CssMetaData<? extends Styleable, ?>> list = new ArrayList<>(
            Control.getClassCssMetaData());  // inherit parent's CSS properties
    list.add(STAR_COLOR);
    list.add(STAR_BORDER_COLOR);
    list.add(STAR_SIZE);
    STYLEABLES = Collections.unmodifiableList(list);
}

@Override
public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
    return STYLEABLES;
}
```

### Step 4: Use in CSS

```css
.rating-control {
    -fx-star-color: gold;
    -fx-star-border-color: #999;
    -fx-star-size: 24;
}

.rating-control:readonly {
    -fx-star-color: #ccc;
}
```

## Pseudo-Classes

Pseudo-classes represent visual states (`:hover`, `:pressed`, `:disabled`, `:readonly`). Define custom pseudo-classes for your control's states:

```java
// Define a pseudo-class "readonly"
private static final PseudoClass READONLY_PSEUDO_CLASS =
        PseudoClass.getPseudoClass("readonly");

// Update pseudo-class state when the property changes
private void updatePseudoClassState() {
    pseudoClassStateChanged(READONLY_PSEUDO_CLASS, isReadOnly());
}

// In the property setter:
public final void setReadOnly(boolean value) {
    readOnlyProperty().set(value);
    updatePseudoClassState();
}
```

CSS usage:
```css
.rating-control:readonly .star {
    -fx-fill: #ccc;
}
```

## Canvas-Based Rendering

For high-performance custom drawing (charts, gauges, custom shapes), use `Canvas` instead of stacking multiple `Shape` nodes. `Canvas` provides a `GraphicsContext` for immediate-mode drawing — faster for complex visuals.

### Canvas vs Shape Nodes

| Aspect | Shape nodes (Path, Circle, etc.) | Canvas |
|--------|----------------------------------|--------|
| Performance (10+ shapes) | Slower — each shape is a scene graph node | Faster — single node, GPU-accelerated |
| CSS styling | Individual shapes can be styled | Manual — redraw on style change |
| Event handling | Per-shape hit testing | Manual hit testing |
| Best for | Simple, few shapes, CSS-styled | Complex, many shapes, high perf |

### Canvas Drawing Pattern

```java
public class GaugeSkin extends SkinBase<GaugeControl> {

    private final Canvas canvas;
    private final GraphicsContext gc;

    public GaugeSkin(GaugeControl control) {
        super(control);
        this.canvas = new Canvas();
        this.gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        // Redraw when value changes
        control.valueProperty().addListener((obs, old, val) -> draw());

        // Redraw on resize
        canvas.widthProperty().addListener((obs, old, val) -> draw());
        canvas.heightProperty().addListener((obs, old, val) -> draw());
    }

    private void draw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        gc.clearRect(0, 0, w, h);

        // Read CSS-stylable properties for colors
        GaugeControl control = getSkinnable();
        Paint fillColor = control.getArcColor();
        Paint bgColor = control.getBackgroundColor();

        // Draw arc
        gc.setStroke(bgColor);
        gc.setLineWidth(control.getStrokeWidth());
        gc.strokeArc(10, 10, w - 20, h - 20, 90, 360, ArcType.OPEN);

        gc.setStroke(fillColor);
        double angle = 360 * (control.getValue() / control.getMaxValue());
        gc.strokeArc(10, 10, w - 20, h - 20, 90, -angle, ArcType.OPEN);
    }

    @Override
    protected double computeMinWidth(double height) {
        return 80;
    }

    @Override
    protected double computeMinHeight(double width) {
        return 80;
    }

    @Override
    protected double computePrefWidth(double height) {
        return 200;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 200;
    }

    @Override
    protected void layoutChildren(double contentX, double contentY,
                                   double contentWidth, double contentHeight) {
        // Size the canvas to the control's content area
        canvas.resizeRelocate(contentX, contentY, contentWidth, contentHeight);
        draw();  // redraw after layout
    }
}
```

### Canvas Resize Strategy

Canvas does not automatically resize with its parent. You must:
1. Override `layoutChildren()` in the Skin to call `canvas.resizeRelocate()`
2. Add listeners to `canvas.widthProperty()` and `canvas.heightProperty()` to trigger redraws
3. Call `draw()` after layout completes

## Event Handling in Skins

Skins handle mouse and keyboard events. Register event handlers in the constructor:

```java
// Mouse press — start interaction
getSkinnable().setOnMousePressed(event -> {
    if (getSkinnable().isReadOnly()) return;
    double x = event.getX();
    updateValueFromMouse(x);
});

// Mouse drag — continue interaction
getSkinnable().setOnMouseDragged(event -> {
    if (getSkinnable().isReadOnly()) return;
    updateValueFromMouse(event.getX());
});

private void updateValueFromMouse(double mouseX) {
    double width = getWidth();
    double ratio = Math.max(0, Math.min(1, mouseX / width));
    double newValue = ratio * getSkinnable().getMaxValue();
    getSkinnable().setValue(newValue);
}
```

## Focus Handling

Custom controls that accept input should support focus traversal:

```java
public RatingControl() {
    setFocusTraversable(true);
    // Keyboard support
    setOnKeyPressed(event -> {
        if (isReadOnly()) return;
        switch (event.getCode()) {
            case LEFT -> setRating(Math.max(0, getRating() - getIncrement()));
            case RIGHT -> setRating(Math.min(getMaxRating(), getRating() + getIncrement()));
            case SPACE -> setRating(getRating());  // confirm
        }
        event.consume();
    });
}
```

## Complete Example: RatingControl

A star rating control with CSS-stylable colors, mouse/keyboard interaction, read-only mode, and SVG-based star rendering.

### RatingControl.java (Control)

```java
package com.example.app.controls;

import javafx.beans.property.*;
import javafx.css.*;
import javafx.scene.control.Control;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RatingControl extends Control {

    // === Regular Properties ===

    // rating (0 to maxRating)
    private DoubleProperty rating;
    public final DoubleProperty ratingProperty() {
        if (rating == null) {
            rating = new SimpleDoubleProperty(this, "rating", 0.0) {
                @Override
                public void set(double newValue) {
                    double clamped = Math.max(0, Math.min(getMaxRating(), newValue));
                    super.set(clamped);
                }
            };
        }
        return rating;
    }
    public final void setRating(double value) { ratingProperty().set(value); }
    public final double getRating() { return rating == null ? 0.0 : rating.get(); }

    // maxRating (default 5)
    private DoubleProperty maxRating;
    public final DoubleProperty maxRatingProperty() {
        if (maxRating == null) {
            maxRating = new SimpleDoubleProperty(this, "maxRating", 5.0);
        }
        return maxRating;
    }
    public final void setMaxRating(double value) { maxRatingProperty().set(value); }
    public final double getMaxRating() { return maxRating == null ? 5.0 : maxRating.get(); }

    // readOnly
    private BooleanProperty readOnly;
    public final BooleanProperty readOnlyProperty() {
        if (readOnly == null) {
            readOnly = new SimpleBooleanProperty(this, "readOnly", false);
            readOnly.addListener((obs, old, val) -> {
                pseudoClassStateChanged(READONLY_PSEUDO_CLASS, val);
                setFocusTraversable(!val);
            });
        }
        return readOnly;
    }
    public final void setReadOnly(boolean value) { readOnlyProperty().set(value); }
    public final boolean isReadOnly() { return readOnly == null ? false : readOnly.get(); }

    // increment (keyboard step)
    private DoubleProperty increment;
    public final DoubleProperty incrementProperty() {
        if (increment == null) {
            increment = new SimpleDoubleProperty(this, "increment", 0.5);
        }
        return increment;
    }
    public final void setIncrement(double value) { incrementProperty().set(value); }
    public final double getIncrement() { return increment == null ? 0.5 : increment.get(); }

    // === CSS-Stylable Properties ===

    // -fx-star-color (filled star color)
    private StyleableObjectProperty<Paint> starColor;
    public final StyleableObjectProperty<Paint> starColorProperty() {
        if (starColor == null) {
            starColor = new SimpleStyleableObjectProperty<>(STAR_COLOR, this, "starColor", Color.GOLD);
        }
        return starColor;
    }
    public final void setStarColor(Paint value) { starColorProperty().set(value); }
    public final Paint getStarColor() { return starColor == null ? Color.GOLD : starColor.get(); }

    // -fx-star-border-color (unfilled star border)
    private StyleableObjectProperty<Paint> starBorderColor;
    public final StyleableObjectProperty<Paint> starBorderColorProperty() {
        if (starBorderColor == null) {
            starBorderColor = new SimpleStyleableObjectProperty<>(STAR_BORDER_COLOR, this,
                    "starBorderColor", Color.web("#cccccc"));
        }
        return starBorderColor;
    }
    public final void setStarBorderColor(Paint value) { starBorderColorProperty().set(value); }
    public final Paint getStarBorderColor() {
        return starBorderColor == null ? Color.web("#cccccc") : starBorderColor.get();
    }

    // -fx-star-size (star size in pixels)
    private StyleableDoubleProperty starSize;
    public final StyleableDoubleProperty starSizeProperty() {
        if (starSize == null) {
            starSize = new SimpleStyleableDoubleProperty(STAR_SIZE, this, "starSize", 24.0);
        }
        return starSize;
    }
    public final void setStarSize(double value) { starSizeProperty().set(value); }
    public final double getStarSize() { return starSize == null ? 24.0 : starSize.get(); }

    // === Pseudo-Class ===

    private static final PseudoClass READONLY_PSEUDO_CLASS =
            PseudoClass.getPseudoClass("readonly");

    // === CSS Metadata ===

    private static final CssMetaData<RatingControl, Paint> STAR_COLOR =
            new CssMetaData<>("-fx-star-color", StyleConverter.getPaintConverter(), Color.GOLD) {
                @Override
                public boolean isSettable(RatingControl control) {
                    return control.starColor == null || !control.starColor.isBound();
                }
                @Override
                public StyleableProperty<Paint> getStyleableProperty(RatingControl control) {
                    return control.starColorProperty();
                }
            };

    private static final CssMetaData<RatingControl, Paint> STAR_BORDER_COLOR =
            new CssMetaData<>("-fx-star-border-color", StyleConverter.getPaintConverter(),
                    Color.web("#cccccc")) {
                @Override
                public boolean isSettable(RatingControl control) {
                    return control.starBorderColor == null || !control.starBorderColor.isBound();
                }
                @Override
                public StyleableProperty<Paint> getStyleableProperty(RatingControl control) {
                    return control.starBorderColorProperty();
                }
            };

    private static final CssMetaData<RatingControl, Number> STAR_SIZE =
            new CssMetaData<>("-fx-star-size", StyleConverter.getSizeConverter(), 24.0) {
                @Override
                public boolean isSettable(RatingControl control) {
                    return control.starSize == null || !control.starSize.isBound();
                }
                @Override
                public StyleableProperty<Number> getStyleableProperty(RatingControl control) {
                    return control.starSizeProperty();
                }
            };

    private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
    static {
        List<CssMetaData<? extends Styleable, ?>> list = new ArrayList<>(Control.getClassCssMetaData());
        list.add(STAR_COLOR);
        list.add(STAR_BORDER_COLOR);
        list.add(STAR_SIZE);
        STYLEABLES = Collections.unmodifiableList(list);
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return STYLEABLES;
    }

    // === Constructor ===

    public RatingControl() {
        getStyleClass().setAll("rating-control");
        setFocusTraversable(true);

        // Clamp rating when maxRating changes
        maxRatingProperty().addListener((obs, old, val) -> {
            if (getRating() > val.doubleValue()) {
                setRating(val.doubleValue());
            }
        });

        // Keyboard navigation
        setOnKeyPressed(event -> {
            if (isReadOnly()) return;
            switch (event.getCode()) {
                case LEFT  -> setRating(Math.max(0, getRating() - getIncrement()));
                case RIGHT -> setRating(Math.min(getMaxRating(), getRating() + getIncrement()));
                default -> { return; }
            }
            event.consume();
        });
    }

    // === Skin ===

    @Override
    protected javafx.scene.control.Skin<?> createDefaultSkin() {
        return new RatingSkin(this);
    }

    // === Stylesheet ===

    @Override
    public String getUserAgentStylesheet() {
        return RatingControl.class.getResource("/css/rating-control.css").toExternalForm();
    }
}
```

### RatingSkin.java (Skin)

```java
package com.example.app.controls;

import javafx.beans.binding.Bindings;
import javafx.scene.control.SkinBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeType;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

public class RatingSkin extends SkinBase<RatingControl> {

    private final HBox container;
    private final List<SVGPath> stars;

    public RatingSkin(RatingControl control) {
        super(control);
        this.container = new HBox();
        this.stars = new ArrayList<>();
        getChildren().add(container);

        // Build star shapes
        rebuildStars();

        // Rebuild when maxRating changes
        control.maxRatingProperty().addListener((obs, old, val) -> rebuildStars());

        // Update visual when rating or colors change
        control.ratingProperty().addListener((obs, old, val) -> updateDisplay());
        control.starColorProperty().addListener((obs, old, val) -> updateDisplay());
        control.starBorderColorProperty().addListener((obs, old, val) -> updateDisplay());
        control.starSizeProperty().addListener((obs, old, val) -> updateDisplay());
        control.readOnlyProperty().addListener((obs, old, val) -> updateDisplay());

        // Mouse interaction
        container.setOnMousePressed(this::handleMouse);
        container.setOnMouseDragged(this::handleMouse);
    }

    private void rebuildStars() {
        container.getChildren().clear();
        stars.clear();

        int count = (int) Math.round(getSkinnable().getMaxRating());
        double size = getSkinnable().getStarSize();

        for (int i = 0; i < count; i++) {
            SVGPath star = new SVGPath();
            star.setContent(STAR_PATH);
            star.setStrokeType(StrokeType.INSIDE);
            star.setStrokeWidth(1);
            star.setScaleX(size / 24.0);
            star.setScaleY(size / 24.0);
            stars.add(star);
            container.getChildren().add(star);
        }
        updateDisplay();
    }

    private void updateDisplay() {
        RatingControl control = getSkinnable();
        double rating = control.getRating();
        Paint fillColor = control.getStarColor();
        Paint borderColor = control.getStarBorderColor();

        for (int i = 0; i < stars.size(); i++) {
            SVGPath star = stars.get(i);
            double starValue = i + 1;

            if (starValue <= rating) {
                // Fully filled
                star.setFill(fillColor);
                star.setStroke(fillColor);
            } else if (starValue - 0.5 <= rating && control.getIncrement() <= 0.5) {
                // Half filled (for 0.5 increment)
                star.setFill(fillColor);
                star.setStroke(borderColor);
                star.setOpacity(0.6);
            } else {
                // Empty
                star.setFill(javafx.scene.paint.Color.TRANSPARENT);
                star.setStroke(borderColor);
                star.setOpacity(1.0);
            }
        }
    }

    private void handleMouse(MouseEvent event) {
        RatingControl control = getSkinnable();
        if (control.isReadOnly()) return;

        double width = container.getWidth();
        if (width <= 0) return;

        double ratio = Math.max(0, Math.min(1, event.getX() / width));
        double newValue = ratio * control.getMaxRating();

        // Snap to increment
        double increment = control.getIncrement();
        newValue = Math.round(newValue / increment) * increment;

        control.setRating(newValue);
    }

    @Override
    protected double computeMinWidth(double height) {
        return getSkinnable().getStarSize() * getSkinnable().getMaxRating() + 4;
    }

    @Override
    protected double computeMinHeight(double width) {
        return getSkinnable().getStarSize() + 4;
    }

    @Override
    protected double computePrefWidth(double height) {
        return computeMinWidth(height);
    }

    @Override
    protected double computePrefHeight(double width) {
        return computeMinHeight(width);
    }

    // SVG path for a 5-pointed star (24x24 viewport)
    private static final String STAR_PATH =
            "M12 2L14.59 8.41L21 9.27L16 14.14L17.18 21.02L12 17.77L6.82 21.02L8 14.14L3 9.27L9.41 8.41Z";
}
```

### rating-control.css

```css
.rating-control {
    -fx-star-color: gold;
    -fx-star-border-color: #cccccc;
    -fx-star-size: 24;
    -fx-padding: 2;
    -fx-spacing: 2;
}

/* Hover effect */
.rating-control:hover {
    -fx-cursor: hand;
}

/* Read-only state */
.rating-control:readonly {
    -fx-star-color: #b0b0b0;
    -fx-cursor: default;
}

/* Disabled state */
.rating-control:disabled {
    -fx-opacity: 0.5;
}

/* Focused state */
.rating-control:focused {
    -fx-background-color: -fx-focus-color;
    -fx-background-insets: -2;
    -fx-background-radius: 3;
}
```

### FXML Usage

```xml
<?import com.example.app.controls.RatingControl?>

<RatingControl fx:id="ratingControl"
               rating="3.5"
               maxRating="5"
               increment="0.5"
               starColor="gold"
               starBorderColor="#ccc"
               starSize="24"/>
```

### Java Usage

```java
RatingControl rating = new RatingControl();
rating.setRating(3.5);
rating.setMaxRating(5);
rating.setIncrement(0.5);
rating.setReadOnly(false);

// Listen to changes
rating.ratingProperty().addListener((obs, old, val) -> {
    System.out.println("New rating: " + val);
});
```

## Control Template Checklist

When building a new custom control, follow this checklist:

1. [ ] **Control class** (`extends Control`)
   - [ ] Define regular properties (DoubleProperty, BooleanProperty, etc.) with lazy init + final getter/setter
   - [ ] Define CSS-stylable properties (StyleableObjectProperty, StyleableDoubleProperty) with lazy init
   - [ ] Define CssMetaData for each stylable property (isSettable + getStyleableProperty)
   - [ ] Aggregate CssMetaData in static block + override `getControlCssMetaData()`
   - [ ] Define pseudo-classes (PseudoClass.getPseudoClass) + update on property change
   - [ ] Constructor: set styleClass, setFocusTraversable, register key handlers
   - [ ] Override `createDefaultSkin()` → return your Skin
   - [ ] Override `getUserAgentStylesheet()` → return CSS resource URL
2. [ ] **Skin class** (`extends SkinBase<MyControl>`)
   - [ ] Constructor: create visual nodes, register event handlers, bind to properties
   - [ ] Property listeners: call `updateDisplay()` when control properties change
   - [ ] Event handlers: mouse press/drag, keyboard
   - [ ] Override `computeMinWidth/Height` and `computePrefWidth/Height`
   - [ ] Override `layoutChildren()` if using Canvas (call resizeRelocate + draw)
3. [ ] **CSS file**
   - [ ] Default values for all stylable properties
   - [ ] Pseudo-class states (:readonly, :disabled, :focused, :hover)
   - [ ] Place in `resources/css/{control-name}.css`
4. [ ] **Resources**
   - [ ] CSS file on classpath at `/css/{control-name}.css`
   - [ ] Any SVG paths or images

## Common Pitfalls

1. **Non-final property methods**: If getter/setter are not `final`, subclassing can break the property contract — always use `final`
2. **Eager property initialization**: Initializing all properties in the constructor wastes memory for properties the user never accesses — use lazy initialization
3. **Forgetting isSettable**: If `isSettable()` always returns `true`, CSS will override programmatic bindings — check `!property.isBound()`
4. **Canvas not resizing**: Canvas doesn't auto-resize — you must call `resizeRelocate()` in `layoutChildren()` and redraw on width/height change
5. **No min/pref size overrides**: Without `computeMinWidth/Height` and `computePrefWidth/Height`, your control may have zero size in a layout
6. **Pseudo-class not updated**: Setting a property that should change a pseudo-class state without calling `pseudoClassStateChanged()` — the CSS won't reflect the new state
7. **Event handler on Skin's node only**: If you register mouse handlers on the Skin's root node but the Control has padding, clicks in the padding area won't register — register on the Control itself
8. **Stylesheet path wrong**: `getUserAgentStylesheet()` must return a valid URL — test with `getResource()` returning non-null before deployment
