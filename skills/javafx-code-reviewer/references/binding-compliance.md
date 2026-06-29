# 数据绑定合规

本文档是**跨维度文档**，不单独对应某个维度，而是同时服务于三个维度：内存泄漏（绑定释放）、性能（绑定效率）、深度合规（Properties null 安全）。与 `javafx-developer` 的 `data-binding-patterns.md` 同源。

| 检查项 | 服务维度 | 严重性基线 |
|--------|---------|-----------|
| 绑定释放规则 | 内存泄漏风险 | Critical |
| 绑定效率规则 | 性能表现 | Major |
| Properties null 安全 | 深度合规审核 | Major |

---

## 检查项 1：绑定释放规则（服务维度：内存泄漏风险）

**关注点**：`Bindings.createXxxBinding()` 返回的 Binding 对象在不需要时是否调用 `dispose()`；单向 / 双向绑定在视图销毁时是否解绑。

**通过判定标准**：
- 通过 `Bindings.createXxxBinding()` 创建的 Binding 保存为字段，在视图销毁时调用 `dispose()`
- 通过 `bind()` 建立的单向绑定在视图销毁时调用 `unbind()`
- 通过 `bindBidirectional()` 建立的双向绑定在视图销毁时调用 `unbindBidirectional()`
- 不在循环或高频事件中反复创建 Binding 而不释放旧绑定

**不通过判定标准**（任一即不通过）：
- `Bindings.createXxxBinding()` 创建的 Binding 未保存引用，无法 `dispose()`
- 视图销毁时未调用 `unbind()` / `unbindBidirectional()` / `dispose()`
- 反复创建新 Binding 替换旧绑定但未释放旧的（绑定链累积 = 内存泄漏）

**严重性基线**：Critical
- 降级条件：短生命周期视图（如对话框）→ Major
- 升级条件：长生命周期视图（主窗口）且绑定数量多 → 保持 Critical

**反例**：
```java
// ❌ 反复创建 Binding 不释放，绑定链累积导致内存泄漏
@FXML
private void handleRefresh() {
    // 每次刷新都创建新 Binding，旧的不 dispose
    statusLabel.textProperty().bind(Bindings.createStringBinding(
        () -> "共 " + users.size() + " 条",
        Bindings.size(users)));
}

// ❌ 创建后无法释放（未保存引用）
public void init() {
    label.textProperty().bind(Bindings.createStringBinding(
        () -> compute(), model.prop1(), model.prop2()));
    // Binding 对象匿名创建，dispose 时找不到引用
}
```

**正例**：
```java
// ✅ 保存 Binding 引用，dispose 时释放
private StringBinding statusBinding;

@Override
public void initialize(URL location, ResourceBundle resources) {
    statusBinding = Bindings.createStringBinding(
        () -> "共 " + users.size() + " 条",
        Bindings.size(users));
    statusLabel.textProperty().bind(statusBinding);
}

public void dispose() {
    statusLabel.textProperty().unbind();  // 先解绑
    if (statusBinding != null) {
        statusBinding.dispose();           // 再释放 Binding
    }
}
```

> **关键区别**：`unbind()` 解除属性与 Binding 的关联（属性可再次 set），`dispose()` 释放 Binding 内部资源（移除对所有依赖的监听）。两者都需调用，顺序为先 `unbind()` 再 `dispose()`。

---

## 检查项 2：绑定效率规则（服务维度：性能表现）

**关注点**：是否避免在循环中创建 `Bindings.createXxxBinding()`，计算绑定是否可用更高效的替代方案。

**通过判定标准**：
- 不在循环或高频事件中创建 `Bindings.createXxxBinding()`
- 简单算术绑定使用 `add` / `subtract` / `multiply` / `divide` 等 Fluent API，而非 `createXxxBinding` 包装
- 字符串拼接使用 `Bindings.concat()` 或 `concat()`，而非 `createStringBinding` 手动拼接
- 绑定创建在初始化时一次性完成，运行时仅更新数据源
- 绑定依赖项控制在合理范围（建议 < 5 个），避免级联重算

**不通过判定标准**（任一即不通过）：
- 在循环中创建 `Bindings.createXxxBinding()`（绑定链累积，内存与 CPU 开销大）
- 简单的双属性映射使用 `createXxxBinding` 而非 Fluent API（不必要的开销）
- 绑定依赖项过多（> 10 个），任一变化都触发全量重算
- 在 `ChangeListener` 中手动同步 UI（应使用绑定声明式同步）

**严重性基线**：Major

**反例**：
```java
// ❌ 简单算术用 createXxxBinding 包装，效率低
NumberBinding total = Bindings.createDoubleBinding(
    () -> price.get() * quantity.get(),
    price, quantity);

// ❌ 在循环中创建绑定
for (Item item : items) {
    Label label = new Label();
    label.textProperty().bind(Bindings.createStringBinding(
        () -> item.getName() + ": " + item.getCount(),
        item.nameProperty(), item.countProperty()));
    container.getChildren().add(label);
}

// ❌ 用 ChangeListener 手动同步（应使用绑定）
price.addListener((obs, old, val) -> totalLabel.setText(
    String.valueOf(val.doubleValue() * quantity.get())));
```

**正例**：
```java
// ✅ 简单算术用 Fluent API
NumberBinding total = price.multiply(quantity);

// ✅ 字符串拼接用 concat
label.textProperty().bind(
    item.nameProperty().concat(": ").concat(item.countProperty().asString()));

// ✅ 声明式绑定，无需手动监听
totalLabel.textProperty().bind(total.asString());
```

---

## 检查项 3：Properties null 安全（服务维度：深度合规审核）

**关注点**：`SimpleLongProperty.set(null)` 等基本类型 Property 的 null 处理是否防 NPE。

**通过判定标准**：
- 基本类型 Property（`IntegerProperty`、`LongProperty`、`DoubleProperty`、`FloatProperty`、`BooleanProperty`）的 `set()` 不接受 null
- 从数据库或外部数据源读取可能为 null 的值时，先做 null 检查再 `set()`
- `ObjectProperty<T>` 的 `set(null)` 是合法的，但使用 `get()` 返回值前检查 null
- 双向绑定中 `StringConverter.fromString()` 处理空字符串返回默认值而非 null

**不通过判定标准**（任一即不通过）：
- `SimpleIntegerProperty.set(null)` / `SimpleLongProperty.set(null)` 等（抛 `NullPointerException`）
- 从数据库读取 `null` 直接 `set()` 到基本类型 Property
- 双向绑定中 `NumberStringConverter` 转换空字符串导致 NPE
- `ObjectProperty` 的 `get()` 返回值未判空直接调用方法

**严重性基线**：Major

> **关键事实**：`SimpleIntegerProperty`、`SimpleLongProperty`、`SimpleDoubleProperty`、`SimpleFloatProperty`、`SimpleBooleanProperty` 的 `set()` 方法接受基本类型参数，若传入 `null`（如从 `Number` 自动拆箱），将抛出 `NullPointerException`。这是 Spring Boot + MyBatis 场景的常见陷阱。

**反例**：
```java
// ❌ 数据库返回 null 直接 set 到 LongProperty，NPE
public void loadFromDb(UserEntity entity) {
    id.set(entity.getId());      // 若 getId() 返回 null → NPE
    age.set(entity.getAge());    // 若 getAge() 返回 null → NPE
}

// ❌ 双向绑定中 NumberStringConverter 转换空字符串 NPE
ageField.textProperty().bindBidirectional(viewModel.ageProperty(),
    new NumberStringConverter());
// 用户清空输入框 → fromString("") → 可能返回 null → ageProperty.set(null) → NPE
```

**正例**：
```java
// ✅ null 检查后再 set
public void loadFromDb(UserEntity entity) {
    id.set(entity.getId() != null ? entity.getId() : 0);
    age.set(entity.getAge() != null ? entity.getAge() : 0);
}

// ✅ 自定义 Converter 处理空字符串
public class SafeNumberConverter extends NumberStringConverter {
    @Override
    public Number fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;  // 空字符串返回默认值，不返回 null
        }
        return super.fromString(value);
    }
}
ageField.textProperty().bindBidirectional(viewModel.ageProperty(),
    new SafeNumberConverter());
```
