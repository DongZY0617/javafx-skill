# 内存管理规则

本文档是"内存泄漏风险"维度的判定依据，管辖 7 个检查项（对应设计书 3.4 节）。审查监听器、绑定、静态引用等是否存在泄漏风险。此维度违规默认为 Critical。与 `javafx-developer` 的 `data-binding-patterns.md` 同源，绑定释放的详细规则参见跨维度文档 `binding-compliance.md`。

> **核心风险**：JavaFX 的事件监听和绑定机制是内存泄漏的高发区。监听器注册后会持有目标对象的引用，若不在视图销毁时移除，将导致旧 Controller 无法被 GC，持续接收事件并占用内存。

---

## 检查项 1：监听器移除

**关注点**：通过 `addListener()` 注册的 `ChangeListener` / `ListChangeListener` 是否在视图销毁时通过 `removeListener()` 移除。

**通过判定标准**：
- 所有通过 `addListener()` 注册的监听器在视图销毁时通过 `removeListener()` 移除
- 监听器引用保存为字段，确保移除的是同一实例
- 移除操作在 `setOnCloseRequest`、视图切换回调或自定义 `dispose()` 方法中执行

**不通过判定标准**（任一即不通过）：
- 注册了 `ChangeListener` / `ListChangeListener` 但无对应的 `removeListener()` 调用
- 使用匿名 lambda 注册监听器且未保存引用，导致无法移除
- 视图切换 / Stage 关闭时未执行监听器清理

**严重性基线**：Critical
- 降级条件：监听对象生命周期与 Controller 相同（同生共灭）→ Major
- 升级条件：已致 OOM 或内存持续增长可复现 → 保持 Critical

**反例**：
```java
// ❌ 注册监听器但无清理方法，视图切换后旧 Controller 无法 GC
public class DetailController implements Initializable {
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        model.addItemListener((ListChangeListener<Item>) c -> updateView());
        // 无 removeListener，无 dispose，无 setOnCloseRequest
    }
}
```

**正例**：
```java
// ✅ 保存监听器引用，在 dispose() 中移除
public class DetailController implements Initializable {
    private ListChangeListener<Item> itemListener;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        itemListener = (ListChangeListener<Item>) c -> updateView();
        model.getItems().addListener(itemListener);
    }

    /** 视图销毁时调用，移除监听器 */
    public void dispose() {
        model.getItems().removeListener(itemListener);
    }
}
// 在视图切换回调中：oldController.dispose();
```

---

## 检查项 2：Binding 释放

**关注点**：`Bindings.createXxxBinding()` 返回的 Binding 对象在不需要时是否调用 `dispose()`。

**通过判定标准**：
- 通过 `Bindings.createXxxBinding()` 创建的 Binding 对象保存为字段，在视图销毁时调用 `dispose()`
- 短生命周期视图（对话框、弹出窗口）中的 Binding 在关闭时释放
- 使用 `bind()` 建立的单向绑定，在视图销毁时调用 `unbind()`

**不通过判定标准**（任一即不通过）：
- `Bindings.createXxxBinding()` 创建的 Binding 未保存引用，无法 `dispose()`
- 长生命周期视图中反复创建 Binding 但不释放，导致绑定链累积
- 视图销毁时未释放已创建的 Binding

**严重性基线**：Critical
- 降级条件：短生命周期视图（如对话框）→ Major
- 升级条件：长生命周期视图（主窗口）且绑定数量多 → 保持 Critical

> **补充规则**：绑定释放的详细判定标准与正反例参见 `binding-compliance.md — 绑定释放规则`。

**反例**：
```java
// ❌ 每次刷新都创建新 Binding 但不释放，绑定链累积
@FXML
private void handleRefresh() {
    // 每次创建新 Binding，旧的不释放
    label.textProperty().bind(Bindings.createStringBinding(
        () -> computeLabel(), model.nameProperty(), model.ageProperty()));
}
```

**正例**：
```java
// ✅ 保存 Binding 引用，dispose 时释放
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

## 检查项 3：弱引用使用

**关注点**：长生命周期对象上的监听器是否考虑使用 `WeakChangeListener` / `WeakListChangeListener`。

**通过判定标准**：
- 长生命周期对象（全局 Model、单例 Service）上注册的监听器使用 `WeakChangeListener` / `WeakListChangeListener`
- 或确保在短生命周期 Controller 销毁时显式 `removeListener`
- 使用弱引用监听器时，保持对原始监听器的强引用（防止被过早 GC）

**不通过判定标准**（任一即不通过）：
- 在长生命周期对象上注册普通（非弱引用）监听器且无移除机制
- 使用 `WeakChangeListener` 但未保持对原始监听器的强引用，导致监听器被过早回收失效

**严重性基线**：Major

**反例**：
```java
// ❌ 在全局 Model（长生命周期）上注册普通监听器，无移除机制
public class GlobalModel {
    private static final GlobalModel INSTANCE = new GlobalModel();
    // ...
}
// Controller 中
GlobalModel.getInstance().addListener((ChangeListener<String>) (obs, old, val) -> {
    updateView();  // 普通监听器，Controller 销毁后仍被 Model 持有
});
```

**正例**：
```java
// ✅ 使用 WeakChangeListener 包装
private ChangeListener<String> strongRef;  // 保持强引用防止过早 GC

@Override
public void initialize(URL location, ResourceBundle resources) {
    strongRef = (obs, old, val) -> updateView();
    GlobalModel.getInstance().nameProperty().addListener(
        new WeakChangeListener<>(strongRef));
}
```

---

## 检查项 4：静态引用排查

**关注点**：静态字段是否持有 UI 组件（`Stage`、`Node`）引用，导致无法 GC。

**通过判定标准**：
- 静态字段不持有 UI 组件（`Stage`、`Node`、`Scene`、`Control`）的强引用
- 如需全局访问 UI 组件，使用 `WeakReference` 或 `ObjectProperty<T>` 并在适当时机清除
- 静态字段仅持有无状态的工具对象、配置常量等

**不通过判定标准**（任一即不通过）：
- `static` 字段持有 `Stage`、`Node`、`Control` 等 UI 组件引用
- 静态集合中缓存 UI 组件且无清除机制
- 单例类持有 UI 组件引用且单例生命周期长于 UI 组件

**严重性基线**：Critical（不可降级）

**反例**：
```java
// ❌ 静态字段持有 Stage 引用，Stage 关闭后仍被静态引用保持，无法 GC
public class StageManager {
    private static Stage mainStage;

    public static void setMainStage(Stage stage) {
        mainStage = stage;  // 静态引用泄漏
    }
}
```

**正例**：
```java
// ✅ 方式一：改为实例字段
public class StageManager {
    private Stage mainStage;  // 实例字段，随 Manager 一起回收
    public void setMainStage(Stage stage) { this.mainStage = stage; }
}

// ✅ 方式二：使用 WeakReference
public class StageManager {
    private static WeakReference<Stage> mainStageRef;
    public static void setMainStage(Stage stage) {
        mainStageRef = new WeakReference<>(stage);
    }
}

// ✅ 方式三：使用 ObjectProperty 并在关闭时清除
public class StageManager {
    private static final ObjectProperty<Stage> mainStage = new SimpleObjectProperty<>();
    public static ObjectProperty<Stage> mainStageProperty() { return mainStage; }
    public static void clear() { mainStage.set(null); }
}
```

---

## 检查项 5：匿名内部类

**关注点**：事件处理器匿名内部类是否隐式持有外部 Controller 引用，导致泄漏。

**通过判定标准**：
- 匿名内部类 / lambda 捕获的外部引用在 Controller 销毁时可被释放
- 事件处理器通过 `setOnXxx(null)` 清除，或在 `dispose()` 中解绑
- 长生命周期对象上的匿名监听器使用弱引用或显式移除

**不通过判定标准**（任一即不通过）：
- 在长生命周期对象上注册匿名内部类监听器且无移除机制（匿名内部类隐式持有外部类 `this` 引用）
- 事件处理器注册到外部长生命周期对象但未在视图销毁时清除

**严重性基线**：Major

**反例**：
```java
// ❌ 在长生命周期的 EventBus 上注册匿名监听器，隐式持有 Controller
public class MyController implements Initializable {
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 匿名 lambda 隐式持有 this（MyController），EventBus 持有 lambda → Controller 无法 GC
        GlobalEventBus.subscribe(UserUpdatedEvent.class, e -> refreshView());
    }
    // 无 unsubscribe
}
```

**正例**：
```java
// ✅ 保存监听器引用，dispose 时注销
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

## 检查项 6：Stage 关闭清理

**关注点**：`setOnCloseRequest` 或视图切换回调中是否执行资源清理。

**通过判定标准**：
- `Stage.setOnCloseRequest()` 或 `setOnHiding()` 中执行资源清理
- 视图切换时通过回调调用旧 Controller 的 `dispose()` 方法
- 清理内容包括：停止 `Timeline` / `Animation`、关闭流、释放绑定、移除监听器
- `ScheduledService` 在视图销毁时 `cancel()`

**不通过判定标准**（任一即不通过）：
- Stage 关闭或视图切换时未执行任何资源清理
- 运行中的 `Timeline` / `Animation` / `ScheduledService` 未停止
- 打开的文件流 / 数据库连接未关闭

**严重性基线**：Critical

**反例**：
```java
// ❌ Stage 关闭时无清理逻辑，Timeline 持续运行
public class DashboardController implements Initializable {
    private Timeline timeline;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> refresh()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }
    // 无 setOnCloseRequest 清理，Stage 关闭后 timeline 仍在运行
}
```

**正例**：
```java
// ✅ Stage 关闭时停止 Timeline 并释放资源
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
        // 其他清理：移除监听器、释放绑定等
    }
}
// 在 Application 或父控制器中
stage.setOnCloseRequest(e -> dashboardController.dispose());
// 或视图切换时
oldController.dispose();
```

---

## 检查项 7：双向绑定解绑

**关注点**：`bindBidirectional()` 建立的绑定在视图销毁时是否调用 `unbindBidirectional()`。

**通过判定标准**：
- 通过 `bindBidirectional()` 建立的双向绑定在视图销毁时调用 `unbindBidirectional()`
- 双向绑定涉及的两个属性在视图销毁后不再相互影响
- 自定义 `StringConverter`（双向绑定常用）无状态泄漏

**不通过判定标准**（任一即不通过）：
- 使用 `bindBidirectional()` 但在视图销毁时未调用 `unbindBidirectional()`
- 视图销毁后双向绑定仍生效，导致已关闭视图的属性被意外修改

**严重性基线**：Critical
- 降级条件：短生命周期视图（如对话框）→ Major

**反例**：
```java
// ❌ 双向绑定建立后无解绑
@Override
public void initialize(URL location, ResourceBundle resources) {
    nameField.textProperty().bindBidirectional(viewModel.nameProperty());
    ageField.textProperty().bindBidirectional(viewModel.ageProperty(),
        new NumberStringConverter());
    // 无 unbindBidirectional，视图销毁后绑定仍生效
}
```

**正例**：
```java
@Override
public void initialize(URL location, ResourceBundle resources) {
    nameField.textProperty().bindBidirectional(viewModel.nameProperty());
    ageField.textProperty().bindBidirectional(viewModel.ageProperty(),
        new NumberStringConverter());
}

public void dispose() {
    // ✅ 视图销毁时解绑双向绑定
    nameField.textProperty().unbindBidirectional(viewModel.nameProperty());
    ageField.textProperty().unbindBidirectional(viewModel.ageProperty());
}
```
