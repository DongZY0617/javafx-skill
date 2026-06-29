# UI 线程安全规则

本文档是"UI 线程安全性"维度的判定依据，管辖 6 个检查项（对应设计书 3.2 节）。审查所有 UI 操作是否在 JavaFX Application Thread 上执行，后台任务是否正确处理。此维度违规默认为 Critical。与 `javafx-developer` 的架构规则·线程安全条目同源。

> **核心原则**：JavaFX 中所有 UI 组件的创建和修改必须在 JavaFX Application Thread（FX 线程）上执行。后台线程不得直接操作 UI，必须通过 `Platform.runLater()` 或 `Task` 回调切换到 FX 线程。

---

## 检查项 1：FX 线程更新

**关注点**：所有 UI 组件更新是否在 JavaFX Application Thread 执行。

**通过判定标准**：
- 所有 `setText`、`setItems`、`setVisible`、`setDisable`、`setStyle` 等 UI 组件操作均在 FX 线程执行
- 事件处理器（`onAction`、`setOnMouseClicked` 等）中的代码默认在 FX 线程，无需额外处理
- `initialize()` 方法中的 UI 初始化代码在 FX 线程执行

**不通过判定标准**（任一即不通过）：
- 在 `Thread`、`Runnable`、`Task.call()`、`ExecutorService` 提交的线程中直接调用 UI 组件方法
- 在 `ScheduledService.call()` 中直接更新 UI
- 在 `ChangeListener` 监听非 FX 线程属性时直接更新 UI

**严重性基线**：Critical（不可降级，运行时必然抛 `IllegalStateException: Not on FX application thread`）

**反例**：
```java
// ❌ 后台线程直接更新 UI，将抛 IllegalStateException
new Thread(() -> {
    Thread.sleep(2000);
    statusLabel.setText("完成");  // 非 FX 线程更新 UI
}).start();
```

**正例**：
```java
// ✅ 方式一：Platform.runLater 切回 FX 线程
new Thread(() -> {
    Thread.sleep(2000);
    Platform.runLater(() -> statusLabel.setText("完成"));
}).start();

// ✅ 方式二：使用 Task，在 succeeded 回调中更新（自动在 FX 线程）
Task<Void> task = new Task<>() {
    @Override
    protected Void call() throws Exception {
        Thread.sleep(2000);
        return null;
    }
};
task.setOnSucceeded(e -> statusLabel.setText("完成"));
new Thread(task).start();
```

---

## 检查项 2：后台任务封装

**关注点**：耗时操作是否使用 `Task<T>` 或 `Service` 封装，而非直接在事件处理器中阻塞。

**通过判定标准**：
- 耗时操作（数据库查询、文件 I/O、网络请求、大量计算）使用 `Task<T>` 或 `Service` 封装到后台线程
- 事件处理器中不包含阻塞操作，仅触发后台任务并更新 UI 状态
- `Task` 的 `call()` 方法在后台线程执行，`setOnSucceeded` / `setOnFailed` 回调在 FX 线程执行

**不通过判定标准**（任一即不通过）：
- 在事件处理器中直接执行耗时操作（同步阻塞 FX 线程）
- 使用 `new Thread(() -> { ... }).start()` 而非 `Task` / `Service`，无法获取结果和异常
- 后台线程中直接更新 UI（参见检查项 1）

**严重性基线**：Critical（阻塞 FX 线程导致界面卡死）

**反例**：
```java
// ❌ 事件处理器中直接执行耗时数据库查询，阻塞 FX 线程
@FXML
private void handleLoad() {
    List<User> users = userService.loadAll();  // 阻塞 FX 线程
    userTable.setItems(FXCollections.observableArrayList(users));
}
```

**正例**：
```java
// ✅ 使用 Task 封装，后台执行 + FX 线程更新
@FXML
private void handleLoad() {
    Task<List<User>> loadTask = new Task<>() {
        @Override
        protected List<User> call() {
            return userService.loadAll();  // 后台线程
        }
    };
    loadTask.setOnSucceeded(e ->
        userTable.setItems(FXCollections.observableArrayList(loadTask.getValue())));
    loadTask.setOnFailed(e ->
        showError("加载失败: " + loadTask.getException().getMessage()));
    new Thread(loadTask).start();
}
```

---

## 检查项 3：Platform.runLater 正确性

**关注点**：后台线程回 UI 线程是否使用 `Platform.runLater()`，是否存在过度调用导致性能问题。

**通过判定标准**：
- 后台线程更新 UI 时使用 `Platform.runLater()` 切换到 FX 线程
- 高频更新场景使用节流（如合并多次 `runLater` 为一次，或使用 `AnimationTimer`）
- `Task` 的 `updateMessage` / `updateProgress` / `updateValue` 方法内部已处理线程切换，无需额外 `runLater`

**不通过判定标准**（任一即不通过）：
- 后台线程直接更新 UI 而未使用 `Platform.runLater()`（属于检查项 1 违规）
- 在紧密循环中频繁调用 `Platform.runLater()`，导致 FX 线程事件队列积压、界面卡顿
- 在 `Task.call()` 中用 `Platform.runLater()` 更新进度，而非使用 `updateProgress()`（效率更低）

**严重性基线**：Major（过度调用导致性能问题）

**反例**：
```java
// ❌ 在紧密循环中频繁 runLater，FX 线程事件队列积压
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

**正例**：
```java
// ✅ 使用 Task 内置的 updateMessage/updateProgress，自动节流
Task<Void> task = new Task<>() {
    @Override
    protected Void call() {
        for (int i = 0; i < 100000; i++) {
            updateProgress(i, 100000);  // 内部自动合并到 FX 线程
            updateMessage(String.valueOf(i));
        }
        return null;
    }
};
progressBar.progressProperty().bind(task.progressProperty());
progressLabel.textProperty().bind(task.messageProperty());
```

---

## 检查项 4：阻塞调用排查

**关注点**：FX 线程上是否存在 `Thread.sleep`、同步 I/O、网络请求等阻塞操作。

**通过判定标准**：
- FX 线程上无 `Thread.sleep()` 调用
- FX 线程上无同步文件 I/O（`Files.readAllBytes`、`InputStream.read` 阻塞等）
- FX 线程上无网络请求（`HttpURLConnection`、`Socket` 等）
- FX 线程上无锁等待（`Object.wait()`、`Future.get()` 无超时等）

**不通过判定标准**（任一即不通过）：
- 事件处理器或 `initialize()` 中调用 `Thread.sleep()`
- FX 线程上执行同步文件读取 / 写入
- FX 线程上发起网络请求
- FX 线程上调用 `Future.get()` 无超时参数（可能永久阻塞）

**严重性基线**：Critical
- 降级条件：阻塞时间极短（< 16ms，如本地小文件读取）→ Major
- 升级条件：阻塞时间 > 1s 或涉及网络 I/O → 保持 Critical

**反例**：
```java
// ❌ FX 线程上 Thread.sleep + 同步网络请求
@FXML
private void handleRefresh() {
    Thread.sleep(3000);  // 阻塞 FX 线程 3 秒，界面卡死
    String data = fetchDataFromNetwork();  // 同步网络请求
    label.setText(data);
}
```

**正例**：
```java
// ✅ 阻塞操作移至后台 Task
@FXML
private void handleRefresh() {
    Task<String> refreshTask = new Task<>() {
        @Override
        protected String call() throws Exception {
            Thread.sleep(3000);  // 后台线程休眠
            return fetchDataFromNetwork();
        }
    };
    refreshTask.setOnSucceeded(e -> label.setText(refreshTask.getValue()));
    new Thread(refreshTask).start();
}
```

---

## 检查项 5：并发数据访问

**关注点**：跨线程共享数据是否使用 `synchronized` 或并发集合；`ObservableList` 的修改是否始终在 FX 线程执行。

**通过判定标准**：
- 跨线程共享的可变数据使用 `synchronized` 块、`ConcurrentHashMap` 等并发集合或 `AtomicXxx` 类
- `ObservableList` 的修改（`add`、`remove`、`setAll` 等）始终在 FX 线程执行
- 后台线程通过 `Platform.runLater()` 或 `Task` 回调修改 `ObservableList`
- 共享状态访问使用适当的同步机制

**不通过判定标准**（任一即不通过）：
- 后台线程直接修改 `ObservableList`（非线程安全，跨线程修改可能抛异常或导致数据不一致）
- 跨线程共享可变数据无同步机制，存在竞态条件
- 使用非线程安全的集合（`ArrayList`、`HashMap`）在多线程间共享且无外部同步

**严重性基线**：Critical（ObservableList 非线程安全，跨线程修改可能导致 UI 状态不一致或异常）

> **关键事实**：`ObservableList` 实现不是线程安全的。即使后台线程只是 `add` 一个元素，也可能在 FX 线程正在渲染时触发 `ListChangeListener`，导致并发修改异常。

**反例**：
```java
// ❌ 后台线程直接修改 ObservableList
ObservableList<User> users = FXCollections.observableArrayList();
new Thread(() -> {
    List<User> loaded = service.loadAll();
    users.addAll(loaded);  // 非 FX 线程修改 ObservableList
}).start();
```

**正例**：
```java
// ✅ 后台线程通过 Platform.runLater 或 Task 回调修改
ObservableList<User> users = FXCollections.observableArrayList();
Task<List<User>> task = new Task<>() {
    @Override
    protected List<User> call() {
        return service.loadAll();  // 后台线程仅读取
    }
};
task.setOnSucceeded(e -> users.addAll(task.getValue()));  // FX 线程修改
new Thread(task).start();
```

---

## 检查项 6：ScheduledService 使用

**关注点**：定时任务是否使用 `ScheduledService` 而非 `java.util.Timer`。

**通过判定标准**：
- 定时 / 周期性任务使用 `javafx.concurrent.ScheduledService` 封装
- `ScheduledService` 的 `call()` 在后台线程执行，回调自动在 FX 线程
- 如使用 `java.util.Timer`，其任务中更新 UI 时通过 `Platform.runLater()` 切换

**不通过判定标准**（任一即不通过）：
- 使用 `java.util.Timer` / `TimerTask` 且直接在 `run()` 中更新 UI（非 FX 线程）
- 使用 `ScheduledExecutorService` 且直接更新 UI
- 定时任务中直接操作 UI 组件而未切换到 FX 线程

**严重性基线**：Major

**反例**：
```java
// ❌ 使用 java.util.Timer 且直接更新 UI
Timer timer = new Timer();
timer.scheduleAtFixedRate(new TimerTask() {
    @Override
    public void run() {
        clockLabel.setText(LocalTime.now().toString());  // 非 FX 线程
    }
}, 0, 1000);
```

**正例**：
```java
// ✅ 方式一：使用 ScheduledService（推荐）
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

// ✅ 方式二：Timer + Platform.runLater
timer.scheduleAtFixedRate(new TimerTask() {
    @Override
    public void run() {
        Platform.runLater(() -> clockLabel.setText(LocalTime.now().toString()));
    }
}, 0, 1000);
```
