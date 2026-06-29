# Performance Optimization Guide

This document is the criteria for the "Performance" dimension, governing 9 check items (corresponding to design spec section 3.5). It reviews whether the code has performance bottlenecks and whether it follows JavaFX performance optimization best practices. Default severity baseline: Major. For detailed binding efficiency rules, see the cross-dimension document `binding-compliance.md`.

---

## Check Item 1: TableView Virtualization

**Focus**: Whether large datasets rely on `TableView` virtualization, whether `ListView` + manual rendering is misused causing performance degradation.

**Pass Criteria**:
- Large dataset list display uses `TableView` or `ListView`, relying on their built-in virtualization (only visible rows are rendered)
- No heavy nodes (e.g., nested FXML, many child nodes) are created in CellFactory
- `CellFactory` correctly implements `updateItem`, reusing cells rather than creating new nodes each time

**Fail Criteria** (any one constitutes failure):
- Using non-virtualized containers like `VBox` / `FlowPane` to manually render many data rows (all nodes in the scene graph simultaneously)
- `CellFactory` creates new controls on every `updateItem` without reusing cells
- Loading FXML or performing time-consuming operations in `CellFactory`

**Severity Baseline**: Major

**Bad Example**:
```java
// Using VBox to manually render 1000 rows, all nodes in the scene graph simultaneously
for (User user : users) {
    HBox row = new HBox(new Label(user.getName()), new Label(user.getEmail()));
    dataContainer.getChildren().add(row);  // No virtualization, huge memory and rendering overhead
}
```

**Good Example**:
```java
// Use TableView virtualization, only visible rows are rendered
TableView<User> table = new TableView<>();
TableColumn<User, String> nameCol = new TableColumn<>("Name");
nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
table.setItems(users);  // Virtualization, automatically only renders visible rows
```

---

## Check Item 2: Batch Updates

**Focus**: Whether batch modifications to `ObservableList` use `setAll()` for one-time replacement, rather than looping `add()` item by item.

**Pass Criteria**:
- Batch data replacement uses `setAll(collection)` (triggers 1 change event)
- Batch addition uses `addAll(collection)` (triggers 1 change event), rather than looping `add()`
- When silent batch updates are needed, use `FXCollections.observableArrayList` + `beginChange()` / `endChange()`

**Fail Criteria** (any one constitutes failure):
- Looping `add()` to add items one by one (triggers N change events, TableView repaints frequently)
- Looping `remove()` to delete items one by one (triggers N change events)
- Performing batch operations on large datasets (>10000) on the FX thread without optimization

**Severity Baseline**: Major
- De-escalation condition: Data volume < 100 items → Minor
- Escalation condition: Data volume > 10000 items and executing on FX thread → Critical

**Bad Example**:
```java
// Loop add, triggers 5000 change events
List<User> loaded = userService.loadAll();
for (User user : loaded) {
    users.add(user);  // Each add triggers a ListChangeListener
}
```

**Good Example**:
```java
// Use setAll for one-time replacement, only triggers 1 change event
List<User> loaded = userService.loadAll();
users.setAll(loaded);  // 1 event, TableView repaints only once
```

---

## Check Item 3: Throttle/Debounce

**Focus**: Whether high-frequency input (search boxes, sliders) uses debounce timers to avoid triggering full refreshes on every input.

**Pass Criteria**:
- Search box input uses debounce (e.g., trigger search after 300ms delay, resetting the timer on input during that period)
- Slider (`Slider`) dragging uses debounce or `AnimationTimer` throttling
- High-frequency events (`textProperty` changes, `valueProperty` changes) do not directly trigger heavy operations

**Fail Criteria** (any one constitutes failure):
- Search box triggers database query or network request on every keystroke (no debounce)
- `Slider.valueProperty` triggers recomputation on every change (no throttling)
- Heavy operations (file I/O, database queries) in high-frequency event handlers

**Severity Baseline**: Major

**Bad Example**:
```java
// Triggers search on every keystroke, typing "hello" triggers 5 queries
searchField.textProperty().addListener((obs, old, text) -> {
    List<Result> results = searchService.search(text);  // Query on every keystroke
    resultsList.setAll(results);
});
```

**Good Example**:
```java
// Use debounce, search is triggered 300ms after input stops
Timeline debounceTimer = new Timeline();
searchField.textProperty().addListener((obs, old, text) -> {
    debounceTimer.stop();
    KeyFrame frame = new KeyFrame(Duration.millis(300), e -> {
        List<Result> results = searchService.search(text);
        resultsList.setAll(results);
    });
    debounceTimer.getKeyFrames().setAll(frame);
    debounceTimer.play();
});
```

---

## Check Item 4: CSS Selector Efficiency

**Focus**: Whether CSS avoids deeply nested selectors, whether style class switching in loops is avoided.

**Pass Criteria**:
- CSS selectors are concise, avoiding deep nesting (e.g., `.root .vbox .hbox .button`)
- Style switching uses style classes (`getStyleClass().add()`) rather than `setStyle()` inline
- No frequent `getStyleClass().add()` / `remove()` style class switching in loops

**Fail Criteria** (any one constitutes failure):
- CSS selectors are deeply nested (> 3 levels), with high matching overhead
- Using `setStyle()` to set inline styles in loops (triggers CSS recalculation each time)
- Frequently switching `styleClass` in loops (each switch triggers CSS re-matching)

**Severity Baseline**: Major
- De-escalation condition: Only individual selectors slightly deep, does not affect overall performance → Minor

**Bad Example**:
```java
// setStyle in loop, triggers CSS recalculation each time
for (Node node : nodes) {
    node.setStyle("-fx-background-color: #ff0000;");  // Inline style, poor performance
}
```
```css
/* Deeply nested selector */
.root .content .panel .form .button { -fx-background-color: blue; }
```

**Good Example**:
```java
// Use style classes, defined in CSS
for (Node node : nodes) {
    node.getStyleClass().add("highlight");  // Batch add style class
}
```
```css
/* Concise selector */
.highlight { -fx-background-color: #ff0000; }
```

---

## Check Item 5: Lazy Loading

**Focus**: Whether heavy views / tabs use lazy loading, rather than full initialization at startup.

**Pass Criteria**:
- `Tab` / `TabPane` content is loaded only when first switched to that Tab (lazy loading)
- Heavy views (charts, large tables) are created only when needed for display
- Only the main view is initialized at startup, sub-views are loaded on demand

**Fail Criteria** (any one constitutes failure):
- Full initialization of all Tab content at startup (including unvisited Tabs)
- All views loaded into memory at once in `start()`
- Heavy components (e.g., `WebView`, large charts) created at startup but not immediately displayed

**Severity Baseline**: Major

**Bad Example**:
```java
// Full initialization of all Tab content at startup
TabPane tabPane = new TabPane();
tabPane.getTabs().addAll(
    createUsersTab(),      // Immediately load user management
    createOrdersTab(),     // Immediately load order management
    createReportsTab(),    // Immediately load reports (heavy)
    createSettingsTab()    // Immediately load settings
);
```

**Good Example**:
```java
// Tab content lazy loading, created on first switch
TabPane tabPane = new TabPane();
Tab usersTab = new Tab("User Management");
usersTab.selectedProperty().addListener((obs, wasSel, isSel) -> {
    if (isSel && usersTab.getContent() == null) {
        usersTab.setContent(createUsersTab());  // Load on first switch
    }
});
```

---

## Check Item 6: Layout Computation

**Focus**: Whether `layout()` / `requestLayout()` are called in loops, whether unnecessary `autosize()` is avoided.

**Pass Criteria**:
- No manual `layout()` or `requestLayout()` calls in loops
- Rely on JavaFX automatic layout passes, only manually triggering when necessary
- Trigger layout once after batch modifying node properties, rather than triggering after each modification

**Fail Criteria** (any one constitutes failure):
- Calling `layout()` or `requestLayout()` in loops (each iteration triggers a full layout pass)
- Unnecessarily calling `autosize()` (JavaFX handles this automatically)
- Performing heavy operations in `layoutChildren()`

**Severity Baseline**: Major

**Bad Example**:
```java
// Calling requestLayout in loop, triggers layout pass on each iteration
for (Node node : nodes) {
    node.setPrefSize(100, 50);
    node.requestLayout();  // Triggers layout on each iteration, extremely poor performance
}
```

**Good Example**:
```java
// After batch modifications, JavaFX automatically merges layout passes
for (Node node : nodes) {
    node.setPrefSize(100, 50);
    // Do not manually call requestLayout, JavaFX automatically handles it in the next pulse
}
```

---

## Check Item 7: Image Loading

**Focus**: Whether large images are loaded and scaled on background threads, whether decoding large images on the FX thread is avoided.

**Pass Criteria**:
- Large images are decoded and scaled on background threads, then set to `ImageView` via `Platform.runLater()` when complete
- Using the `Image` background loading constructor: `new Image(url, true)` (background loading)
- Large images are scaled to display size before setting, avoiding keeping full-resolution images in memory

**Fail Criteria** (any one constitutes failure):
- Loading large images on the FX thread (e.g., `new Image("file:big-photo.jpg")` synchronous loading), blocking UI
- Loading large images without scaling, keeping full-resolution images in memory (memory waste)
- Decoding on the FX thread before `ImageView.setImage()`

**Severity Baseline**: Major

**Bad Example**:
```java
// Synchronously loading a large image on the FX thread, blocking UI
@FXML
private void loadImage() {
    Image image = new Image("file:/photos/large.jpg");  // Synchronous load, blocks
    imageView.setImage(image);
}
```

**Good Example**:
```java
// Option 1: Background loading
Image image = new Image("file:/photos/large.jpg", true);  // true = background loading
imageView.setImage(image);

// Option 2: Task background decoding + scaling
Task<Image> loadTask = new Task<>() {
    @Override
    protected Image call() {
        Image full = new Image("file:/photos/large.jpg");
        // Scale to display size
        return scaleImage(full, 400, 300);
    }
};
loadTask.setOnSucceeded(e -> imageView.setImage(loadTask.getValue()));
new Thread(loadTask).start();
```

---

## Check Item 8: FilteredList Efficiency

**Focus**: Whether the `FilteredList` predicate is overly complex, whether index optimization is considered for large datasets.

**Pass Criteria**:
- The `FilteredList` predicate logic is concise, with no heavy operations (no I/O, no complex computation)
- Large datasets (> 10000) consider using indexes or pre-filtering optimization
- The predicate does not create new objects on each evaluation

**Fail Criteria** (any one constitutes failure):
- The `FilteredList` predicate executes database queries or file I/O
- The predicate is overly complex (multiple nested conditions, regex matching large datasets), causing filtering latency
- Large datasets without optimization, fully re-filtering on every input change

**Severity Baseline**: Major
- De-escalation condition: Data volume < 1000 items → Minor

**Bad Example**:
```java
// Predicate executes heavy regex matching + database query
FilteredList<User> filtered = new FilteredList<>(users);
filtered.predicateProperty().bind(Bindings.createObjectBinding(() -> {
    Pattern pattern = Pattern.compile(searchField.getText());  // Compiles regex each time
    return user -> {
        // Database query in predicate, extremely slow
        return pattern.matcher(user.getName()).matches()
            && dbService.isActive(user.getId());
    };
}, searchField.textProperty()));
```

**Good Example**:
```java
// Predicate logic is concise, pre-compiled regex, no I/O
FilteredList<User> filtered = new FilteredList<>(users);
filtered.predicateProperty().bind(Bindings.createObjectBinding(() -> {
    String query = searchField.getText().toLowerCase();
    if (query.isEmpty()) return null;  // No filtering
    return user -> user.getName().toLowerCase().contains(query);  // Pure memory operation
}, searchField.textProperty()));
```

---

## Check Item 9: Binding Efficiency

**Focus**: Whether creating `Bindings.createXxxBinding()` in loops is avoided, whether computed bindings can use more efficient alternatives.

**Pass Criteria**:
- No creating `Bindings.createXxxBinding()` in loops (each creation builds a new binding chain, with high memory and CPU overhead)
- Simple property mapping uses `bind()` direct binding, rather than `createXxxBinding` wrapping
- Complex computed bindings evaluate whether more efficient APIs like `SelectBinding` / `ObjectBinding` can be used
- Binding creation is done once at initialization, rather than rebuilt on each event trigger

**Fail Criteria** (any one constitutes failure):
- Creating `Bindings.createXxxBinding()` in loops or high-frequency events (binding chains accumulate)
- Simple two-property mapping uses `createXxxBinding` instead of direct `bind` / `add` / `subtract`
- Too many binding dependencies (> 5), causing full recomputation on any dependency change

**Severity Baseline**: Major

> **Supplementary Rule**: For detailed binding efficiency criteria and good/bad examples, see `binding-compliance.md - Binding Efficiency Rules`.

**Bad Example**:
```java
// Creating bindings in a loop, each iteration builds a new binding chain
for (Item item : items) {
    Label label = new Label();
    label.textProperty().bind(Bindings.createStringBinding(
        () -> item.getName() + " (" + item.getCount() + ")",
        item.nameProperty(), item.countProperty()));
    container.getChildren().add(label);
}
// If items change frequently, binding chains accumulate, memory leak + performance degradation
```

**Good Example**:
```java
// Simple mapping uses Bindings.concat or direct arithmetic binding
label.textProperty().bind(
    item.nameProperty().concat(" (").concat(item.countProperty().asString()).concat(")")
);
// Or reuse cells in CellFactory, only updating text on updateItem
```
