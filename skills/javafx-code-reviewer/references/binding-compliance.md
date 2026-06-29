# Data Binding Compliance

This document is a **cross-dimension document**; it does not correspond to a single dimension but simultaneously serves three dimensions: memory leaks (binding disposal), performance (binding efficiency), and deep compliance (Properties null safety). Shares the same origin as `javafx-developer`'s `data-binding-patterns.md`.

| Check Item | Served Dimension | Severity Baseline |
|------------|------------------|-------------------|
| Binding Disposal Rules | Memory Leak Risks | Critical |
| Binding Efficiency Rules | Performance | Major |
| Properties Null Safety | Deep Compliance Audit | Major |

---

## Check Item 1: Binding Disposal Rules (Served Dimension: Memory Leak Risks)

**Focus**: Whether Binding objects returned by `Bindings.createXxxBinding()` call `dispose()` when no longer needed; whether one-way / bidirectional bindings are unbound when the view is destroyed.

**Pass Criteria**:
- Bindings created via `Bindings.createXxxBinding()` are saved as fields and `dispose()` is called when the view is destroyed
- One-way bindings established via `bind()` call `unbind()` when the view is destroyed
- Bidirectional bindings established via `bindBidirectional()` call `unbindBidirectional()` when the view is destroyed
- No repeatedly creating Bindings in loops or high-frequency events without releasing old ones

**Fail Criteria** (any one constitutes failure):
- Bindings created via `Bindings.createXxxBinding()` are not saved as references, making `dispose()` impossible
- `unbind()` / `unbindBidirectional()` / `dispose()` not called when the view is destroyed
- Repeatedly creating new Bindings to replace old ones without releasing the old ones (binding chain accumulation = memory leak)

**Severity Baseline**: Critical
- De-escalation condition: Short-lived view (e.g., dialog) → Major
- Escalation condition: Long-lived view (main window) with many bindings → remain Critical

**Bad Example**:
```java
// Repeatedly creating Bindings without releasing, binding chain accumulates causing memory leak
@FXML
private void handleRefresh() {
    // Creates a new Binding on every refresh, old ones not disposed
    statusLabel.textProperty().bind(Bindings.createStringBinding(
        () -> "Total " + users.size() + " items",
        Bindings.size(users)));
}

// Cannot release after creation (reference not saved)
public void init() {
    label.textProperty().bind(Bindings.createStringBinding(
        () -> compute(), model.prop1(), model.prop2()));
    // Binding object created anonymously, cannot find reference for dispose
}
```

**Good Example**:
```java
// Save Binding reference, release on dispose
private StringBinding statusBinding;

@Override
public void initialize(URL location, ResourceBundle resources) {
    statusBinding = Bindings.createStringBinding(
        () -> "Total " + users.size() + " items",
        Bindings.size(users));
    statusLabel.textProperty().bind(statusBinding);
}

public void dispose() {
    statusLabel.textProperty().unbind();  // Unbind first
    if (statusBinding != null) {
        statusBinding.dispose();           // Then release Binding
    }
}
```

> **Key Distinction**: `unbind()` removes the association between the property and the Binding (the property can be set again), while `dispose()` releases the Binding's internal resources (removes listeners on all dependencies). Both must be called, in the order of `unbind()` first, then `dispose()`.

---

## Check Item 2: Binding Efficiency Rules (Served Dimension: Performance)

**Focus**: Whether creating `Bindings.createXxxBinding()` in loops is avoided, whether computed bindings can use more efficient alternatives.

**Pass Criteria**:
- No creating `Bindings.createXxxBinding()` in loops or high-frequency events
- Simple arithmetic bindings use Fluent API such as `add` / `subtract` / `multiply` / `divide`, rather than `createXxxBinding` wrapping
- String concatenation uses `Bindings.concat()` or `concat()`, rather than `createStringBinding` manual concatenation
- Binding creation is done once at initialization, only data sources are updated at runtime
- Binding dependencies are kept within a reasonable range (recommended < 5), avoiding cascading recomputation

**Fail Criteria** (any one constitutes failure):
- Creating `Bindings.createXxxBinding()` in loops (binding chains accumulate, high memory and CPU overhead)
- Simple two-property mapping uses `createXxxBinding` instead of Fluent API (unnecessary overhead)
- Too many binding dependencies (> 10), any change triggers full recomputation
- Manually syncing UI in `ChangeListener` (should use declarative binding sync)

**Severity Baseline**: Major

**Bad Example**:
```java
// Simple arithmetic wrapped with createXxxBinding, inefficient
NumberBinding total = Bindings.createDoubleBinding(
    () -> price.get() * quantity.get(),
    price, quantity);

// Creating bindings in a loop
for (Item item : items) {
    Label label = new Label();
    label.textProperty().bind(Bindings.createStringBinding(
        () -> item.getName() + ": " + item.getCount(),
        item.nameProperty(), item.countProperty()));
    container.getChildren().add(label);
}

// Manual sync with ChangeListener (should use binding)
price.addListener((obs, old, val) -> totalLabel.setText(
    String.valueOf(val.doubleValue() * quantity.get())));
```

**Good Example**:
```java
// Simple arithmetic with Fluent API
NumberBinding total = price.multiply(quantity);

// String concatenation with concat
label.textProperty().bind(
    item.nameProperty().concat(": ").concat(item.countProperty().asString()));

// Declarative binding, no manual listening needed
totalLabel.textProperty().bind(total.asString());
```

---

## Check Item 3: Properties Null Safety (Served Dimension: Deep Compliance Audit)

**Focus**: Whether `SimpleLongProperty.set(null)` and other primitive type Property null handling prevents NPE.

**Pass Criteria**:
- Primitive type Properties (`IntegerProperty`, `LongProperty`, `DoubleProperty`, `FloatProperty`, `BooleanProperty`) `set()` does not accept null
- When reading values that may be null from a database or external data source, perform a null check before `set()`
- `ObjectProperty<T>` `set(null)` is legal, but check null before using the `get()` return value
- `StringConverter.fromString()` in bidirectional bindings handles empty strings by returning a default value rather than null

**Fail Criteria** (any one constitutes failure):
- `SimpleIntegerProperty.set(null)` / `SimpleLongProperty.set(null)` etc. (throws `NullPointerException`)
- Reading `null` from a database and directly `set()`-ing it to a primitive type Property
- `NumberStringConverter` converting an empty string in a bidirectional binding causing NPE
- `ObjectProperty` `get()` return value used directly without null check, calling methods on it

**Severity Baseline**: Major

> **Key Fact**: `SimpleIntegerProperty`, `SimpleLongProperty`, `SimpleDoubleProperty`, `SimpleFloatProperty`, `SimpleBooleanProperty` `set()` methods accept primitive type parameters; if `null` is passed (e.g., auto-unboxing from `Number`), a `NullPointerException` will be thrown. This is a common pitfall in Spring Boot + MyBatis scenarios.

**Bad Example**:
```java
// Database returns null, directly set to LongProperty, NPE
public void loadFromDb(UserEntity entity) {
    id.set(entity.getId());      // If getId() returns null -> NPE
    age.set(entity.getAge());    // If getAge() returns null -> NPE
}

// NumberStringConverter converting empty string in bidirectional binding, NPE
ageField.textProperty().bindBidirectional(viewModel.ageProperty(),
    new NumberStringConverter());
// User clears input box -> fromString("") -> may return null -> ageProperty.set(null) -> NPE
```

**Good Example**:
```java
// Null check before set
public void loadFromDb(UserEntity entity) {
    id.set(entity.getId() != null ? entity.getId() : 0);
    age.set(entity.getAge() != null ? entity.getAge() : 0);
}

// Custom Converter to handle empty strings
public class SafeNumberConverter extends NumberStringConverter {
    @Override
    public Number fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;  // Empty string returns default value, not null
        }
        return super.fromString(value);
    }
}
ageField.textProperty().bindBidirectional(viewModel.ageProperty(),
    new SafeNumberConverter());
```
