# CSS Compliance Rules

This document is the criteria for CSS compliance within the "Deep Compliance Audit" dimension, governing 3 check items: `var()` prohibition, literal numeric value rules, and looked-up color usage rules. Default severity baseline: Major. Shares the same origin as `javafx-developer`'s `css-best-practices.md`.

> **Core Difference**: JavaFX CSS is not Web CSS. JavaFX CSS is based on CSS syntax but has many limitations; the most critical is that it does not support the `var()` function. JavaFX implements variable functionality through the "looked-up color" mechanism, where colors defined on `.root` are referenced directly by name by child nodes, without `var()` wrapping.

---

## Check Item 1: var() Prohibition Rule

**Focus**: Whether `var()` is not used (JavaFX CSS does not support it).

**Pass Criteria**:
- No `var()` function calls appear in CSS files
- Color variables use the looked-up color mechanism: define `-fx-xxx-color` in `.root`, child nodes reference directly by name (e.g., `-fx-background-color: -fx-primary-color;`)
- Size values use literal numeric values (e.g., `-fx-background-radius: 8;`), not referenced via `var()`

**Fail Criteria** (any one constitutes failure):
- CSS uses `var(-fx-primary-color)` syntax (JavaFX CSS does not support it, style does not take effect)
- CSS uses `var(-fx-radius)` to reference size variables
- Bringing Web CSS `var()` habits into JavaFX CSS

**Severity Baseline**: Major (unsupported syntax, style does not take effect, cannot be de-escalated)

> **Key Fact**: The JavaFX CSS parser does not recognize the `var()` function. Property declarations using `var()` are silently ignored, and the corresponding styles do not take effect. This is the most common error when migrating from Web CSS to JavaFX CSS.

**Bad Example**:
```css
/* Using var(), JavaFX CSS does not support, style does not take effect */
.root {
    -fx-primary-color: #2196f3;
    -fx-radius: 8;
}
.button-primary {
    -fx-background-color: var(-fx-primary-color);      /* Does not take effect */
    -fx-background-radius: var(-fx-radius);             /* Does not take effect */
    -fx-text-fill: var(-fx-text-color, #333333);        /* Fallback syntax not supported */
}
```

**Good Example**:
```css
/* Directly reference looked-up color, no var() wrapping needed */
.root {
    -fx-primary-color: #2196f3;
    -fx-text-color: #333333;
}
.button-primary {
    -fx-background-color: -fx-primary-color;   /* Direct reference by name */
    -fx-background-radius: 8;                   /* Literal numeric value */
    -fx-text-fill: -fx-text-color;              /* Direct reference by name */
}
```

---

## Check Item 2: Literal Numeric Value Rule

**Focus**: Whether size properties such as border radius use literal numeric values, rather than looked-up color references to size variables.

**Pass Criteria**:
- Size properties such as `-fx-background-radius`, `-fx-border-radius`, `-fx-padding` use literal numeric values (e.g., `8`, `4px`, `10 5 10 5`)
- Size values are not referenced via looked-up color variables (looked-up colors are primarily used for color values)
- Literal numeric values are consistent across multiple uses, or documented via CSS comments

**Fail Criteria** (any one constitutes failure):
- `-fx-background-radius: -fx-radius;` (referencing a size variable via looked-up color, unreliable in JavaFX)
- `-fx-padding: -fx-spacing;` (size property referencing a looked-up color variable)
- Size property values use `var()` references (also violates Check Item 1)

**Severity Baseline**: Major
- De-escalation condition: Only individual size properties misuse looked-up color references, does not affect overall layout → Minor

> **Key Fact**: The looked-up color mechanism in JavaFX is primarily used for **color** values. Using looked-up colors directly for size properties such as `-fx-background-radius` and `-fx-border-radius` is unreliable in JavaFX and may not be parsed or may be parsed to incorrect values. Size properties should use literal numeric values.

**Bad Example**:
```css
/* Size properties referenced via looked-up color, unreliable */
.root {
    -fx-radius: 8;
    -fx-spacing: 10;
}
.card {
    -fx-background-radius: -fx-radius;    /* Unreliable, may not take effect */
    -fx-border-radius: -fx-radius;        /* Unreliable */
    -fx-padding: -fx-spacing;             /* Unreliable */
}
```

**Good Example**:
```css
/* Size properties use literal numeric values */
.root {
    -fx-primary-color: #2196f3;  /* Looked-up color only for colors */
}
.card {
    -fx-background-color: -fx-primary-color;  /* Color uses looked-up color */
    -fx-background-radius: 8;                  /* Size uses literal */
    -fx-border-radius: 8;                      /* Size uses literal */
    -fx-padding: 10;                           /* Size uses literal */
}
```

---

## Check Item 3: Looked-up Color Usage Rule

**Focus**: Whether looked-up colors are defined in `.root` and referenced directly by name by child nodes, whether the scope is correct.

**Pass Criteria**:
- Looked-up colors are defined in `.root` with the `-fx-` prefix (e.g., `-fx-primary-color: #2196f3;`)
- Child nodes reference looked-up colors directly by name (e.g., `-fx-background-color: -fx-primary-color;`), without `var()` wrapping
- Theme switching is achieved by replacing looked-up color definitions on `.root` (or switching different CSS files)
- When locally overriding looked-up colors, redefine on a specific node, affecting only that node and its children

**Fail Criteria** (any one constitutes failure):
- Looked-up color referenced by a child node without being defined in `.root` (undefined looked-up color falls back to default value)
- Looked-up color definition does not start with the `-fx-` prefix (e.g., `primary-color` instead of `-fx-primary-color`, may not be recognized)
- Color values use Web CSS syntax instead of JavaFX-supported formats (e.g., using `rgb()` without spaces)
- Theme switching achieved by modifying node styles one by one, rather than replacing `.root` looked-up color definitions

**Severity Baseline**: Major
- De-escalation condition: Only individual looked-up color definitions are non-standard but functionality is normal → Minor

**Bad Example**:
```css
/* Looked-up color referenced without being defined in .root */
.button {
    -fx-background-color: -fx-primary-color;  /* -fx-primary-color is undefined, falls back to default */
}

/* Looked-up color definition without -fx- prefix */
.root {
    primary-color: #2196f3;  /* No -fx- prefix, may not be recognized as a looked-up color */
}

/* Theme switching modifies nodes one by one, rather than replacing .root definition */
/* In JS/Java: button1.setStyle("-fx-background-color: #ff0000;"); */
/* button2.setStyle("-fx-background-color: #ff0000;"); */
/* Should instead switch looked-up color definitions on .root */
```

**Good Example**:
```css
/* Looked-up colors defined in .root, child nodes reference directly */
.root {
    -fx-primary-color: #2196f3;
    -fx-accent-color: #ff9800;
    -fx-bg-color: #ffffff;
    -fx-text-color: #333333;
}

.button-primary {
    -fx-background-color: -fx-primary-color;  /* Direct reference */
    -fx-text-fill: white;
}

.label-title {
    -fx-text-fill: -fx-text-color;  /* Direct reference */
}

/* Theme switching: switch looked-up color definitions on .root */
/* dark-theme.css */
.root {
    -fx-primary-color: #1565c0;
    -fx-bg-color: #1e1e1e;
    -fx-text-color: #e0e0e0;
}
/* In Java: scene.getStylesheets().setAll("/css/dark-theme.css"); */
```

---

## Check Item 4: Looked-up Colors and Variable Resolution

**Focus**: Whether looked-up color variables are declared in the correct scope and order, whether references resolve deterministically, and whether circular color references exist.

### Variable Resolution Order

JavaFX resolves a looked-up color reference by walking a well-defined lookup chain. A reference to `-fx-my-color` on a node is resolved against, in order:

1. **Inline style** on the node itself (`node.setStyle("-fx-my-color: #abc;")`) — highest precedence.
2. **Stylesheet rule** matching the node (e.g., `.button { -fx-my-color: #abc; }`) — the most recently added stylesheet wins; within a stylesheet, later rules of equal specificity win.
3. **Inherited looked-up color** from the node's parent — looked-up colors cascade down the scene graph, so a value set on `.root` is visible to every descendant.
4. **Modena (JavaFX 8+) default** — the built-in user agent stylesheet defines the baseline palette (e.g., `-fx-base`, `-fx-accent`, `-fx-background`). Caspian is the legacy default for JavaFX 2.x.

The first definition found in this chain wins; later levels are not consulted for that node. This is why a color declared in `.root` is overridden by an inline style or a more specific stylesheet rule.

### Common Pitfall: Reference Before Declaration Across Stylesheets

When multiple stylesheets are loaded, a looked-up color referenced in an earlier stylesheet but declared in a later one will not resolve:

```css
/* app.css — loaded FIRST */
.button {
    -fx-background-color: -fx-primary-color;  /* -fx-primary-color not yet defined here */
}

/* theme.css — loaded SECOND */
.root {
    -fx-primary-color: #2196f3;  /* declared too late for app.css's reference */
}
```

Resolution is order-sensitive because each stylesheet is parsed and applied as it is added to the scene's stylesheet list. The reference in `app.css` is evaluated against the lookup chain at the time `app.css` is applied; `theme.css` has not yet contributed its `.root` definition, so `-fx-primary-color` falls back to its Modena default (or `null`).

**Detection**: The reviewer should flag any looked-up color that is referenced in a stylesheet but defined only in a stylesheet that is loaded later in `scene.getStylesheets()`. The tester can confirm at runtime by printing `node.getScene().getStylesheets()` order and checking each reference's declaration location.

**Fix**: Either (a) load the declaration stylesheet first, or (b) move all `.root` looked-up color declarations into a single base stylesheet loaded before any component stylesheets.

### Detecting Circular Color References

A circular reference occurs when looked-up colors form a cycle: `-fx-a` references `-fx-b`, which references `-fx-a` (directly or transitively). JavaFX does not detect this at parse time; at runtime the cycle produces either a stack overflow in the color resolver or, more commonly, a silently transparent/black fallback with no error.

```css
/* Circular: -fx-a -> -fx-b -> -fx-a */
.root {
    -fx-a: -fx-b;
    -fx-b: -fx-a;
}
.button {
    -fx-background-color: -fx-a;  /* resolves to nothing — circular */
}
```

**Detection algorithm** (reviewer static check):
1. Parse each stylesheet's `.root` block into a map of `{color-name -> value-token}`.
2. Build a directed graph: for each declaration whose value is itself a looked-up color reference (token matches `-fx-*` and is not a literal color/derivation), add an edge `name -> referenced-name`.
3. Run a cycle detection (DFS) on this graph. Any back-edge indicates a circular reference.

A self-reference (`-fx-a: -fx-a;`) is the trivial 1-cycle and should be flagged identically.

> **Note**: JavaFX color derivation functions (`derive(-fx-base, 50%)`, `ladder(-fx-base, ...)`) are NOT circular even though they reference another color — they terminate because they consume the referenced color as an input to a pure function. Only direct name-to-name cycles are defects.

### Best Practice: Declare All Custom Colors in .root at the Top

To avoid resolution-order and scope issues entirely:

```css
/* GOOD: all custom looked-up colors declared once, at the top, in .root */
.root {
    -fx-primary-color: #2196f3;
    -fx-accent-color: #ff9800;
    -fx-bg-color: #ffffff;
    -fx-text-color: #333333;
    -fx-error-color: #f44336;
    -fx-success-color: #4caf50;
}

/* Component rules only reference, never declare, looked-up colors */
.button-primary {
    -fx-background-color: -fx-primary-color;
    -fx-text-fill: -fx-text-color;
}
```

**Rules**:
- Declare every custom looked-up color exactly once, in the `.root` block of a single base stylesheet loaded first.
- Component stylesheets should only reference looked-up colors, never declare new ones at `.root` scope (they may override on a specific node, but never redefine the global value).
- Keep declaration order deterministic: base/theme stylesheet first, component stylesheets after.

**Severity Baseline**: Major
- De-escalation condition: Circular reference exists but is unreachable (the referencing rule matches no node in the running scene) → Minor.
- Escalation condition: Circular or unresolved reference affects a visible, default-styled control → Critical (the control renders with broken/transparent colors).

---

## Check Item 5: SelectBinding vs ObjectBinding Performance

**Focus**: Whether the chosen binding type (SelectBinding vs ObjectBinding vs `Bindings.createObjectBinding`) is appropriate for the property chain depth and lifecycle, and whether SelectBinding listener chains are leaked.

> **Context**: Although this check concerns Java code (not CSS), it is grouped under CSS Compliance because the most common trigger is CSS-driven theming where property values are bound to looked-up colors or theme properties. A leaky binding here often manifests as the same memory-growth symptoms as a CSS leak.

### When to Use SelectBinding vs ObjectBinding

JavaFX offers several binding strategies for deriving a value from one or more properties:

- **`SelectBinding`** (`Bindings.selectXXX(root, "child", "grandchild", ...)`): Use for **deep property chains** where the intermediate nodes can change. SelectBinding re-evaluates the chain reactively: if `root.getChild()` is replaced, it re-attaches its listeners to the new child and continues tracking `grandchild`. This is the only built-in binding that survives intermediate-node replacement.
- **`ObjectBinding` / `Bindings.createObjectBinding(supplier, dependencies...)`**: Use for **computed values** that depend on a known, fixed set of properties. The dependencies are listed explicitly; if any changes, the supplier is re-invoked. Intermediate nodes are assumed stable — if a dependency is replaced rather than mutated, the binding will NOT track the new node.
- **`Bindings.createObjectBinding` (one-shot)**: Use when the computation is a pure function of the current values and you do not need ongoing reactivity across a changing structure. Compute once (or on explicit invalidation), then discard the binding.

**Decision rule**:
- Deep chain (≥ 2 levels) where intermediate nodes can be swapped → `SelectBinding`.
- Flat computation over stable properties → `ObjectBinding` / `createObjectBinding`.
- One-shot derived value → `createObjectBinding` with no long-lived reference.

### Performance Comparison

SelectBinding is more powerful but more expensive:

- A `SelectBinding` over an N-level chain installs up to N listeners (one per level). Each time an intermediate node changes, it must detach from the old sub-chain and attach to the new one — an O(depth) operation per structural change.
- An `ObjectBinding` installs exactly one listener per declared dependency (constant), and never re-attaches. It is cheaper both to create and to maintain.
- For a deep chain that is structurally stable (intermediates never replaced), `ObjectBinding` is strictly faster; `SelectBinding`'s re-attachment machinery is pure overhead.

**Recommendation**: Do not default to `SelectBinding` for everything. If the intermediate nodes are stable for the binding's lifetime, prefer `ObjectBinding` with the leaf properties listed as dependencies.

### Common Memory Leak: SelectBinding Without dispose()

This is the most frequent binding-related leak in JavaFX applications:

```java
// LEAK: SelectBinding on a root that is later replaced/removed,
//       but the binding is never disposed.
StringBinding name = Bindings.selectString(
    viewModel.currentCustomerProperty(), "contact", "name");
nameLabel.textProperty().bind(name);
// ... later: viewModel.setCurrentCustomer(otherCustomer);
// The old SelectBinding's listener chain is still attached to the
// OLD customer's contact.name until GC — and if the old customer
// is retained elsewhere, the listeners keep it alive.
```

The listener chain created by `SelectBinding` is attached to live nodes. When the root property changes (`currentCustomer` is replaced), SelectBinding detaches from the old chain and re-attaches to the new one — but only if the SelectBinding itself is still alive and being read. If the binding has been abandoned (no strong reference held, but not explicitly `unbind()`/`dispose()`'d) while its listeners are still attached to nodes reachable from elsewhere, those nodes are kept alive through the listener chain — a classic leak.

**Detection**:
- Static (reviewer): Flag any `Bindings.selectXXX(...)` result that is not (a) stored in a field that is `unbind()`/`dispose()`'d in a `@Cleanup`/`stop()`/`close()` method, or (b) bound to a `Control` property whose lifecycle exactly matches the binding's intended lifetime.
- Runtime (tester): Confirm via heap dump — the leaked old customer appears in the dominator tree retained through a `SelectBinding` listener.

### Recommendation Summary

| Scenario | Recommended Binding | Cleanup Required |
|----------|--------------------|------------------|
| Deep reactive chain, intermediate nodes can change | `SelectBinding` | Yes — `unbind()` + null the field when the owning view closes; or wrap in a `Disposable` |
| Computed value over stable properties | `Bindings.createObjectBinding(supplier, dep1, dep2)` | Optional — GC-able once dependencies are unreachable |
| One-shot derived value (compute once, display) | `Bindings.createObjectBinding(supplier)` (no deps, evaluate immediately) | No — discard reference after reading `.get()` |
| String/number formatting of a single property | `Bindings.format` / `Bindings.convert` | Optional — bound to label lifecycle |

**Best practice**: For reactive chains, use `SelectBinding` but always pair it with explicit disposal. For one-shot computations, use `Bindings.createObjectBinding` and let it be GC'd. Never leave a `SelectBinding` referenced only by a weak/soft path while its listener chain is attached to live nodes.

**Severity Baseline**: Major
- De-escalation condition: SelectBinding chain is shallow (1 level, equivalent to a direct binding) → Minor.
- Escalation condition: SelectBinding on a root property that is replaced frequently (e.g., per-navigation) and never disposed → Critical (cumulative leak).

### Cross-References

When a binding leak is suspected or confirmed, cross-reference:
- `../javafx-tester/references/performance-testing.md` section 3.2 (Memory Growth Trend) — to confirm the leak at runtime.
- `../javafx-tester/references/performance-testing.md` section 7.4 (Identifying Memory Leaks via GC Frequency) — to detect the leak via GC log trends.
- `memory-leak-risks.md` -- Binding Release (the static rule backing this check item).
