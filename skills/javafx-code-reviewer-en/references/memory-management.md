# Memory Management Rules

This document is the criteria for the "Memory Leak Risks" dimension, governing 7 check items (corresponding to design spec section 3.4). It reviews whether listeners, bindings, static references, and other constructs pose leak risks. Violations in this dimension default to Critical. Shares the same origin as `javafx-developer`'s `data-binding-patterns.md`; for detailed binding disposal rules, see the cross-dimension document `binding-compliance.md`.

> **Core Risk**: JavaFX's event listener and binding mechanisms are high-incidence areas for memory leaks. After a listener is registered, it holds a reference to the target object; if not removed when the view is destroyed, the old Controller cannot be GC'd, continues receiving events, and occupies memory.

---

## Check Item 1: Listener Removal

**Focus**: Whether `ChangeListener` / `ListChangeListener` registered via `addListener()` are removed via `removeListener()` when the view is destroyed.

**Pass Criteria**:
- All listeners registered via `addListener()` are removed via `removeListener()` when the view is destroyed
- Listener references are saved as fields, ensuring the same instance is removed
- Removal is performed in `setOnCloseRequest`, view-switching callbacks, or a custom `dispose()` method

**Fail Criteria** (any one constitutes failure):
- Registered `ChangeListener` / `ListChangeListener` with no corresponding `removeListener()` call
- Registered listeners using anonymous lambdas without saving references, making removal impossible
- No listener cleanup performed during view switching / Stage closing

**Severity Baseline**: Critical
- De-escalation condition: Listener object lifecycle is the same as the Controller (co-terminus) → Major
- Escalation condition: Has caused OOM or reproducible memory growth → remain Critical

**Bad Example**:
```java
// Registered listener but no cleanup method, old Controller cannot be GC'd after view switching
public class DetailController implements Initializable {
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        model.addItemListener((ListChangeListener<Item>) c -> updateView());
        // No removeListener, no dispose, no setOnCloseRequest
    }
}
```

**Good Example**:
```java
// Save listener reference, remove in dispose()
public class DetailController implements Initializable {
    private ListChangeListener<Item> itemListener;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        itemListener = (ListChangeListener<Item>) c -> updateView();
        model.getItems().addListener(itemListener);
    }

    /** Called when the view is destroyed, removes the listener */
    public void dispose() {
        model.getItems().removeListener(itemListener);
    }
}
// In view-switching callback: oldController.dispose();
```

---

## Check Item 2: Binding Disposal

**Focus**: Whether Binding objects returned by `Bindings.createXxxBinding()` call `dispose()` when no longer needed.

**Pass Criteria**:
- Binding objects created via `Bindings.createXxxBinding()` are saved as fields and `dispose()` is called when the view is destroyed
- Bindings in short-lived views (dialogs, popups) are released when closed
- One-way bindings established via `bind()` call `unbind()` when the view is destroyed

**Fail Criteria** (any one constitutes failure):
- Bindings created via `Bindings.createXxxBinding()` are not saved as references, making `dispose()` impossible
- Bindings are repeatedly created in long-lived views without release, causing binding chains to accumulate
- Created Bindings are not released when the view is destroyed

**Severity Baseline**: Critical
- De-escalation condition: Short-lived view (e.g., dialog) → Major
- Escalation condition: Long-lived view (main window) with many bindings → remain Critical

> **Supplementary Rule**: For detailed binding disposal criteria and good/bad examples, see `binding-compliance.md - Binding Disposal Rules`.

**Bad Example**:
```java
// Creates a new Binding on every refresh without releasing, binding chain accumulates
@FXML
private void handleRefresh() {
    // Creates a new Binding each time, old ones not released
    label.textProperty().bind(Bindings.createStringBinding(
        () -> computeLabel(), model.nameProperty(), model.ageProperty()));
}
```

**Good Example**:
```java
// Save Binding reference, release on dispose
private StringBinding labelBinding;

@Override
public void initialize(URL location, ResourceBundle resources) {
    labelBinding = Bindings.createStringBinding(
        () -> computeLabel(), model.nameProperty(), model.ageProperty());
    label.textProperty().bind(labelBinding);
}

public void dispose() {
    label.textProperty().unbind();
    labelBinding.dispose();
}
```

---

## Check Item 3: Weak Reference Usage

**Focus**: Whether listeners on long-lived objects consider using `WeakChangeListener` / `WeakListChangeListener`.

**Pass Criteria**:
- Listeners registered on long-lived objects (global Model, singleton Service) use `WeakChangeListener` / `WeakListChangeListener`
- Or ensure explicit `removeListener` when the short-lived Controller is destroyed
- When using weak reference listeners, maintain a strong reference to the original listener (to prevent premature GC)

**Fail Criteria** (any one constitutes failure):
- Registering regular (non-weak) listeners on long-lived objects with no removal mechanism
- Using `WeakChangeListener` but not maintaining a strong reference to the original listener, causing the listener to be prematurely reclaimed and become ineffective

**Severity Baseline**: Major

**Bad Example**:
```java
// Registering a regular listener on a global Model (long-lived) with no removal mechanism
public class GlobalModel {
    private static final GlobalModel INSTANCE = new GlobalModel();
    // ...
}
// In Controller
GlobalModel.getInstance().addListener((ChangeListener<String>) (obs, old, val) -> {
    updateView();  // Regular listener, still held by Model after Controller is destroyed
});
```

**Good Example**:
```java
// Wrap with WeakChangeListener
private ChangeListener<String> strongRef;  // Maintain strong reference to prevent premature GC

@Override
public void initialize(URL location, ResourceBundle resources) {
    strongRef = (obs, old, val) -> updateView();
    GlobalModel.getInstance().nameProperty().addListener(
        new WeakChangeListener<>(strongRef));
}
```

---

## Check Item 4: Static Reference Detection

**Focus**: Whether static fields hold references to UI components (`Stage`, `Node`), preventing GC.

**Pass Criteria**:
- Static fields do not hold strong references to UI components (`Stage`, `Node`, `Scene`, `Control`)
- If global access to UI components is needed, use `WeakReference` or `ObjectProperty<T>` and clear at the appropriate time
- Static fields only hold stateless utility objects, configuration constants, etc.

**Fail Criteria** (any one constitutes failure):
- `static` fields hold references to UI components such as `Stage`, `Node`, `Control`
- Static collections cache UI components without a clearing mechanism
- Singleton classes hold UI component references and the singleton lifecycle is longer than the UI component

**Severity Baseline**: Critical (cannot be de-escalated)

**Bad Example**:
```java
// Static field holding Stage reference, after Stage is closed it is still retained by the static reference, cannot be GC'd
public class StageManager {
    private static Stage mainStage;

    public static void setMainStage(Stage stage) {
        mainStage = stage;  // Static reference leak
    }
}
```

**Good Example**:
```java
// Option 1: Change to instance field
public class StageManager {
    private Stage mainStage;  // Instance field, reclaimed together with Manager
    public void setMainStage(Stage stage) { this.mainStage = stage; }
}

// Option 2: Use WeakReference
public class StageManager {
    private static WeakReference<Stage> mainStageRef;
    public static void setMainStage(Stage stage) {
        mainStageRef = new WeakReference<>(stage);
    }
}

// Option 3: Use ObjectProperty and clear on close
public class StageManager {
    private static final ObjectProperty<Stage> mainStage = new SimpleObjectProperty<>();
    public static ObjectProperty<Stage> mainStageProperty() { return mainStage; }
    public static void clear() { mainStage.set(null); }
}
```

---

## Check Item 5: Anonymous Inner Classes

**Focus**: Whether event handler anonymous inner classes implicitly hold outer Controller references, causing leaks.

**Pass Criteria**:
- Outer references captured by anonymous inner classes / lambdas can be released when the Controller is destroyed
- Event handlers are cleared via `setOnXxx(null)` or unbound in `dispose()`
- Anonymous listeners on long-lived objects use weak references or explicit removal

**Fail Criteria** (any one constitutes failure):
- Registering anonymous inner class listeners on long-lived objects with no removal mechanism (anonymous inner classes implicitly hold the outer class `this` reference)
- Event handlers registered to external long-lived objects but not cleared when the view is destroyed

**Severity Baseline**: Major

**Bad Example**:
```java
// Registering an anonymous listener on a long-lived EventBus, implicitly holding Controller
public class MyController implements Initializable {
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Anonymous lambda implicitly holds this (MyController), EventBus holds lambda -> Controller cannot be GC'd
        GlobalEventBus.subscribe(UserUpdatedEvent.class, e -> refreshView());
    }
    // No unsubscribe
}
```

**Good Example**:
```java
// Save listener reference, unsubscribe on dispose
public class MyController implements Initializable {
    private Consumer<UserUpdatedEvent> handler;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        handler = e -> refreshView();
        GlobalEventBus.subscribe(UserUpdatedEvent.class, handler);
    }

    public void dispose() {
        GlobalEventBus.unsubscribe(UserUpdatedEvent.class, handler);
    }
}
```

---

## Check Item 6: Stage Close Cleanup

**Focus**: Whether `setOnCloseRequest` or view-switching callbacks perform resource cleanup.

**Pass Criteria**:
- `Stage.setOnCloseRequest()` or `setOnHiding()` performs resource cleanup
- View switching calls the old Controller's `dispose()` method via callback
- Cleanup includes: stopping `Timeline` / `Animation`, closing streams, releasing bindings, removing listeners
- `ScheduledService` is `cancel()`-ed when the view is destroyed

**Fail Criteria** (any one constitutes failure):
- No resource cleanup performed when the Stage is closed or view is switched
- Running `Timeline` / `Animation` / `ScheduledService` not stopped
- Open file streams / database connections not closed

**Severity Baseline**: Critical

**Bad Example**:
```java
// No cleanup logic on Stage close, Timeline keeps running
public class DashboardController implements Initializable {
    private Timeline timeline;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> refresh()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }
    // No setOnCloseRequest cleanup, timeline still running after Stage is closed
}
```

**Good Example**:
```java
// Stop Timeline and release resources on Stage close
public class DashboardController implements Initializable {
    private Timeline timeline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> refresh()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    public void dispose() {
        if (timeline != null) timeline.stop();
        // Other cleanup: remove listeners, release bindings, etc.
    }
}
// In Application or parent controller
stage.setOnCloseRequest(e -> dashboardController.dispose());
// Or on view switching
oldController.dispose();
```

---

## Check Item 7: Bidirectional Binding Unbinding

**Focus**: Whether bindings established by `bindBidirectional()` call `unbindBidirectional()` when the view is destroyed.

**Pass Criteria**:
- Bidirectional bindings established via `bindBidirectional()` call `unbindBidirectional()` when the view is destroyed
- The two properties involved in the bidirectional binding no longer affect each other after the view is destroyed
- Custom `StringConverter` (commonly used in bidirectional bindings) has no state leaks

**Fail Criteria** (any one constitutes failure):
- Using `bindBidirectional()` but not calling `unbindBidirectional()` when the view is destroyed
- Bidirectional binding still in effect after the view is destroyed, causing properties of the closed view to be unexpectedly modified

**Severity Baseline**: Critical
- De-escalation condition: Short-lived view (e.g., dialog) → Major

**Bad Example**:
```java
// Bidirectional binding established but no unbinding
@Override
public void initialize(URL location, ResourceBundle resources) {
    nameField.textProperty().bindBidirectional(viewModel.nameProperty());
    ageField.textProperty().bindBidirectional(viewModel.ageProperty(),
        new NumberStringConverter());
    // No unbindBidirectional, binding still in effect after view is destroyed
}
```

**Good Example**:
```java
@Override
public void initialize(URL location, ResourceBundle resources) {
    nameField.textProperty().bindBidirectional(viewModel.nameProperty());
    ageField.textProperty().bindBidirectional(viewModel.ageProperty(),
        new NumberStringConverter());
}

public void dispose() {
    // Unbind bidirectional bindings when the view is destroyed
    nameField.textProperty().unbindBidirectional(viewModel.nameProperty());
    ageField.textProperty().unbindBidirectional(viewModel.ageProperty());
}
```
