# UI Thread Safety Rules

This document is the criteria for the "UI Thread Safety" dimension, governing 6 check items (corresponding to design spec section 3.2). It reviews whether all UI operations execute on the JavaFX Application Thread and whether background tasks are handled correctly. Violations in this dimension default to Critical. Shares the same origin as `javafx-developer`'s architecture rules · thread safety items.

> **Core Principle**: In JavaFX, all creation and modification of UI components must execute on the JavaFX Application Thread (FX thread). Background threads must not directly manipulate UI; they must switch to the FX thread via `Platform.runLater()` or `Task` callbacks.

---

## Check Item 1: FX Thread Updates

**Focus**: Whether all UI component updates execute on the JavaFX Application Thread.

**Pass Criteria**:
- All UI component operations such as `setText`, `setItems`, `setVisible`, `setDisable`, `setStyle` execute on the FX thread
- Code in event handlers (`onAction`, `setOnMouseClicked`, etc.) executes on the FX thread by default, requiring no additional handling
- UI initialization code in `initialize()` executes on the FX thread

**Fail Criteria** (any one constitutes failure):
- Calling UI component methods directly in `Thread`, `Runnable`, `Task.call()`, or threads submitted by `ExecutorService`
- Directly updating UI in `ScheduledService.call()`
- Directly updating UI in a `ChangeListener` listening to a non-FX thread property

**Severity Baseline**: Critical (cannot be de-escalated; runtime will always throw `IllegalStateException: Not on FX application thread`)

**Bad Example**:
```java
// Background thread directly updating UI, will throw IllegalStateException
new Thread(() -> {
    Thread.sleep(2000);
    statusLabel.setText("Done");  // Non-FX thread updating UI
}).start();
```

**Good Example**:
```java
// Option 1: Platform.runLater to switch back to FX thread
new Thread(() -> {
    Thread.sleep(2000);
    Platform.runLater(() -> statusLabel.setText("Done"));
}).start();

// Option 2: Use Task, update in succeeded callback (automatically on FX thread)
Task<Void> task = new Task<>() {
    @Override
    protected Void call() throws Exception {
        Thread.sleep(2000);
        return null;
    }
};
task.setOnSucceeded(e -> statusLabel.setText("Done"));
new Thread(task).start();
```

---

## Check Item 2: Background Task Encapsulation

**Focus**: Whether time-consuming operations use `Task<T>` or `Service` encapsulation, rather than blocking directly in event handlers.

**Pass Criteria**:
- Time-consuming operations (database queries, file I/O, network requests, heavy computation) use `Task<T>` or `Service` to encapsulate into background threads
- Event handlers contain no blocking operations, only triggering background tasks and updating UI state
- The `Task` `call()` method executes on a background thread, `setOnSucceeded` / `setOnFailed` callbacks execute on the FX thread

**Fail Criteria** (any one constitutes failure):
- Executing time-consuming operations directly in event handlers (synchronously blocking the FX thread)
- Using `new Thread(() -> { ... }).start()` instead of `Task` / `Service`, unable to obtain results and exceptions
- Directly updating UI from a background thread (see Check Item 1)

**Severity Baseline**: Critical (blocking the FX thread causes UI freeze)

**Bad Example**:
```java
// Executing time-consuming database query directly in event handler, blocking FX thread
@FXML
private void handleLoad() {
    List<User> users = userService.loadAll();  // Blocks FX thread
    userTable.setItems(FXCollections.observableArrayList(users));
}
```

**Good Example**:
```java
// Use Task encapsulation, background execution + FX thread update
@FXML
private void handleLoad() {
    Task<List<User>> loadTask = new Task<>() {
        @Override
        protected List<User> call() {
            return userService.loadAll();  // Background thread
        }
    };
    loadTask.setOnSucceeded(e ->
        userTable.setItems(FXCollections.observableArrayList(loadTask.getValue())));
    loadTask.setOnFailed(e ->
        showError("Load failed: " + loadTask.getException().getMessage()));
    new Thread(loadTask).start();
}
```

---

## Check Item 3: Platform.runLater Correctness

**Focus**: Whether background threads returning to the UI thread use `Platform.runLater()`, whether excessive calls cause performance issues.

**Pass Criteria**:
- Background threads use `Platform.runLater()` to switch to the FX thread when updating UI
- High-frequency update scenarios use throttling (e.g., merging multiple `runLater` calls into one, or using `AnimationTimer`)
- The `Task` `updateMessage` / `updateProgress` / `updateValue` methods handle thread switching internally, requiring no additional `runLater`

**Fail Criteria** (any one constitutes failure):
- Background thread directly updates UI without using `Platform.runLater()` (constitutes a Check Item 1 violation)
- Frequently calling `Platform.runLater()` in a tight loop, causing the FX thread event queue to back up and UI to stutter
- Using `Platform.runLater()` to update progress in `Task.call()` instead of using `updateProgress()` (less efficient)

**Severity Baseline**: Major (excessive calls causing performance issues)

**Bad Example**:
```java
// Frequent runLater in tight loop, FX thread event queue backs up
Task<Void> task = new Task<>() {
    @Override
    protected Void call() {
        for (int i = 0; i < 100000; i++) {
            int finalI = i;
            Platform.runLater(() -> progressLabel.setText(String.valueOf(finalI)));
        }
        return null;
    }
};
```

**Good Example**:
```java
// Use Task's built-in updateMessage/updateProgress, automatic throttling
Task<Void> task = new Task<>() {
    @Override
    protected Void call() {
        for (int i = 0; i < 100000; i++) {
            updateProgress(i, 100000);  // Internally merged to FX thread
            updateMessage(String.valueOf(i));
        }
        return null;
    }
};
progressBar.progressProperty().bind(task.progressProperty());
progressLabel.textProperty().bind(task.messageProperty());
```

---

## Check Item 4: Blocking Call Detection

**Focus**: Whether `Thread.sleep`, synchronous I/O, network requests, or other blocking operations exist on the FX thread.

**Pass Criteria**:
- No `Thread.sleep()` calls on the FX thread
- No synchronous file I/O on the FX thread (`Files.readAllBytes`, `InputStream.read` blocking, etc.)
- No network requests on the FX thread (`HttpURLConnection`, `Socket`, etc.)
- No lock waits on the FX thread (`Object.wait()`, `Future.get()` without timeout, etc.)

**Fail Criteria** (any one constitutes failure):
- Calling `Thread.sleep()` in event handlers or `initialize()`
- Performing synchronous file read / write on the FX thread
- Making network requests on the FX thread
- Calling `Future.get()` without a timeout parameter on the FX thread (may block indefinitely)

**Severity Baseline**: Critical
- De-escalation condition: Blocking time very short (< 16ms, e.g., small local file read) → Major
- Escalation condition: Blocking time > 1s or involves network I/O → remain Critical

**Bad Example**:
```java
// Thread.sleep + synchronous network request on FX thread
@FXML
private void handleRefresh() {
    Thread.sleep(3000);  // Blocks FX thread for 3 seconds, UI freezes
    String data = fetchDataFromNetwork();  // Synchronous network request
    label.setText(data);
}
```

**Good Example**:
```java
// Move blocking operations to background Task
@FXML
private void handleRefresh() {
    Task<String> refreshTask = new Task<>() {
        @Override
        protected String call() throws Exception {
            Thread.sleep(3000);  // Background thread sleep
            return fetchDataFromNetwork();
        }
    };
    refreshTask.setOnSucceeded(e -> label.setText(refreshTask.getValue()));
    new Thread(refreshTask).start();
}
```

---

## Check Item 5: Concurrent Data Access

**Focus**: Whether cross-thread shared data uses `synchronized` or concurrent collections; whether `ObservableList` modifications always execute on the FX thread.

**Pass Criteria**:
- Cross-thread shared mutable data uses `synchronized` blocks, `ConcurrentHashMap` and other concurrent collections, or `AtomicXxx` classes
- `ObservableList` modifications (`add`, `remove`, `setAll`, etc.) always execute on the FX thread
- Background threads modify `ObservableList` via `Platform.runLater()` or `Task` callbacks
- Shared state access uses appropriate synchronization mechanisms

**Fail Criteria** (any one constitutes failure):
- Background thread directly modifies `ObservableList` (not thread-safe; cross-thread modification may throw exceptions or cause data inconsistency)
- Cross-thread shared mutable data has no synchronization mechanism, with race conditions
- Using non-thread-safe collections (`ArrayList`, `HashMap`) shared across multiple threads without external synchronization

**Severity Baseline**: Critical (ObservableList is not thread-safe; cross-thread modification may cause UI state inconsistency or exceptions)

> **Key Fact**: `ObservableList` implementations are not thread-safe. Even if a background thread only `add`s one element, it may trigger `ListChangeListener` while the FX thread is rendering, causing concurrent modification exceptions.

**Bad Example**:
```java
// Background thread directly modifying ObservableList
ObservableList<User> users = FXCollections.observableArrayList();
new Thread(() -> {
    List<User> loaded = service.loadAll();
    users.addAll(loaded);  // Non-FX thread modifying ObservableList
}).start();
```

**Good Example**:
```java
// Background thread modifies via Platform.runLater or Task callback
ObservableList<User> users = FXCollections.observableArrayList();
Task<List<User>> task = new Task<>() {
    @Override
    protected List<User> call() {
        return service.loadAll();  // Background thread only reads
    }
};
task.setOnSucceeded(e -> users.addAll(task.getValue()));  // FX thread modifies
new Thread(task).start();
```

---

## Check Item 6: ScheduledService Usage

**Focus**: Whether scheduled tasks use `ScheduledService` rather than `java.util.Timer`.

**Pass Criteria**:
- Scheduled / periodic tasks use `javafx.concurrent.ScheduledService` encapsulation
- `ScheduledService` `call()` executes on a background thread, callbacks are automatically on the FX thread
- If using `java.util.Timer`, its tasks switch to the FX thread via `Platform.runLater()` when updating UI

**Fail Criteria** (any one constitutes failure):
- Using `java.util.Timer` / `TimerTask` and directly updating UI in `run()` (non-FX thread)
- Using `ScheduledExecutorService` and directly updating UI
- Scheduled task directly manipulating UI components without switching to the FX thread

**Severity Baseline**: Major

**Bad Example**:
```java
// Using java.util.Timer and directly updating UI
Timer timer = new Timer();
timer.scheduleAtFixedRate(new TimerTask() {
    @Override
    public void run() {
        clockLabel.setText(LocalTime.now().toString());  // Non-FX thread
    }
}, 0, 1000);
```

**Good Example**:
```java
// Option 1: Use ScheduledService (recommended)
ScheduledService<Void> clockService = new ScheduledService<>() {
    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                updateMessage(LocalTime.now().toString());
                return null;
            }
        };
    }
};
clockService.setPeriod(Duration.seconds(1));
clockService.setOnSucceeded(e -> clockLabel.setText(clockService.getLastValue().toString()));
clockService.start();

// Option 2: Timer + Platform.runLater
timer.scheduleAtFixedRate(new TimerTask() {
    @Override
    public void run() {
        Platform.runLater(() -> clockLabel.setText(LocalTime.now().toString()));
    }
}, 0, 1000);
```
