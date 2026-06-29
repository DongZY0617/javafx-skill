# 性能优化指南

本文档是"性能表现"维度的判定依据，管辖 9 个检查项（对应设计书 3.5 节）。审查代码是否存在性能瓶颈，是否遵循 JavaFX 性能优化最佳实践。默认严重性基线：Major。绑定效率的详细规则参见跨维度文档 `binding-compliance.md`。

---

## 检查项 1：TableView 虚拟化

**关注点**：大数据量是否依赖 `TableView` 虚拟化，是否误用 `ListView` + 手动渲染导致性能下降。

**通过判定标准**：
- 大数据量列表展示使用 `TableView` 或 `ListView`，依赖其内置虚拟化（仅渲染可见行）
- 不在 CellFactory 中创建重型节点（如嵌套 FXML、大量子节点）
- `CellFactory` 中正确实现 `updateItem`，复用单元格而非每次创建新节点

**不通过判定标准**（任一即不通过）：
- 使用 `VBox` / `FlowPane` 等非虚拟化容器手动渲染大量数据行（全部节点同时在场景图中）
- `CellFactory` 中每次 `updateItem` 都 `new` 新控件，未复用单元格
- 在 `CellFactory` 中加载 FXML 或执行耗时操作

**严重性基线**：Major

**反例**：
```java
// ❌ 用 VBox 手动渲染 1000 行，全部节点同时在场景图中
for (User user : users) {
    HBox row = new HBox(new Label(user.getName()), new Label(user.getEmail()));
    dataContainer.getChildren().add(row);  // 无虚拟化，内存与渲染开销巨大
}
```

**正例**：
```java
// ✅ 使用 TableView 虚拟化，仅渲染可见行
TableView<User> table = new TableView<>();
TableColumn<User, String> nameCol = new TableColumn<>("姓名");
nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
table.setItems(users);  // 虚拟化，自动仅渲染可见行
```

---

## 检查项 2：批量更新

**关注点**：批量修改 `ObservableList` 时是否使用 `setAll()` 一次性替换，而非循环 `add()` 逐条添加。

**通过判定标准**：
- 批量替换数据使用 `setAll(collection)`（触发 1 次变更事件）
- 批量添加使用 `addAll(collection)`（触发 1 次变更事件），而非循环 `add()`
- 需要静默批量更新时使用 `FXCollections.observableArrayList` + `beginChange()` / `endChange()`

**不通过判定标准**（任一即不通过）：
- 循环调用 `add()` 逐条添加（触发 N 次变更事件，TableView 频繁重绘）
- 循环调用 `remove()` 逐条删除（触发 N 次变更事件）
- 在 FX 线程上对大数据量（>10000）执行批量操作且未优化

**严重性基线**：Major
- 降级条件：数据量 < 100 条 → Minor
- 升级条件：数据量 > 10000 条且在 FX 线程执行 → Critical

**反例**：
```java
// ❌ 循环 add，触发 5000 次变更事件
List<User> loaded = userService.loadAll();
for (User user : loaded) {
    users.add(user);  // 每次 add 触发一次 ListChangeListener
}
```

**正例**：
```java
// ✅ 使用 setAll 一次性替换，仅触发 1 次变更事件
List<User> loaded = userService.loadAll();
users.setAll(loaded);  // 1 次事件，TableView 仅重绘一次
```

---

## 检查项 3：节流防抖

**关注点**：高频输入（搜索框、滑块）是否使用防抖定时器，避免每次输入触发完整刷新。

**通过判定标准**：
- 搜索框输入使用防抖（如延迟 300ms 后触发搜索，期间输入重置计时器）
- 滑块（`Slider`）拖动使用防抖或 `AnimationTimer` 节流
- 高频事件（`textProperty` 变化、`valueProperty` 变化）不直接触发重型操作

**不通过判定标准**（任一即不通过）：
- 搜索框每次按键直接触发数据库查询或网络请求（无防抖）
- `Slider.valueProperty` 每次变化直接触发重计算（无节流）
- 高频事件处理器中执行重型操作（文件 I/O、数据库查询）

**严重性基线**：Major

**反例**：
```java
// ❌ 每次按键都触发搜索，输入"hello"触发 5 次查询
searchField.textProperty().addListener((obs, old, text) -> {
    List<Result> results = searchService.search(text);  // 每次按键查询
    resultsList.setAll(results);
});
```

**正例**：
```java
// ✅ 使用防抖，停止输入 300ms 后才触发搜索
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

## 检查项 4：CSS 选择器效率

**关注点**：CSS 是否避免深层嵌套选择器、是否避免在循环中切换样式类。

**通过判定标准**：
- CSS 选择器简洁，避免深层嵌套（如 `.root .vbox .hbox .button`）
- 使用样式类（`getStyleClass().add()`）切换样式，而非 `setStyle()` 内联
- 不在循环中频繁 `getStyleClass().add()` / `remove()` 切换样式类

**不通过判定标准**（任一即不通过）：
- CSS 选择器深层嵌套（> 3 层），匹配开销大
- 在循环中使用 `setStyle()` 设置内联样式（每次触发 CSS 重新计算）
- 在循环中频繁切换 `styleClass`（每次切换触发 CSS 重新匹配）

**严重性基线**：Major
- 降级条件：仅个别选择器略深，不影响整体性能 → Minor

**反例**：
```java
// ❌ 在循环中 setStyle，每次触发 CSS 重新计算
for (Node node : nodes) {
    node.setStyle("-fx-background-color: #ff0000;");  // 内联样式，性能差
}
```
```css
/* ❌ 深层嵌套选择器 */
.root .content .panel .form .button { -fx-background-color: blue; }
```

**正例**：
```java
// ✅ 使用样式类，CSS 中定义
for (Node node : nodes) {
    node.getStyleClass().add("highlight");  // 批量添加样式类
}
```
```css
/* ✅ 简洁的选择器 */
.highlight { -fx-background-color: #ff0000; }
```

---

## 检查项 5：懒加载

**关注点**：重型视图 / 标签页是否使用懒加载，而非启动时全量初始化。

**通过判定标准**：
- `Tab` / `TabPane` 的内容在首次切换到该 Tab 时才加载（懒加载）
- 重型视图（图表、大表格）在需要显示时才创建
- 启动时仅初始化主视图，子视图按需加载

**不通过判定标准**（任一即不通过）：
- 启动时全量初始化所有 Tab 内容（含未访问的 Tab）
- 所有视图在 `start()` 中一次性加载到内存
- 重型组件（如 `WebView`、大图表）在启动时创建但未立即显示

**严重性基线**：Major

**反例**：
```java
// ❌ 启动时全量初始化所有 Tab 内容
TabPane tabPane = new TabPane();
tabPane.getTabs().addAll(
    createUsersTab(),      // 立即加载用户管理
    createOrdersTab(),     // 立即加载订单管理
    createReportsTab(),    // 立即加载报表（重型）
    createSettingsTab()    // 立即加载设置
);
```

**正例**：
```java
// ✅ Tab 内容懒加载，首次切换时才创建
TabPane tabPane = new TabPane();
Tab usersTab = new Tab("用户管理");
usersTab.selectedProperty().addListener((obs, wasSel, isSel) -> {
    if (isSel && usersTab.getContent() == null) {
        usersTab.setContent(createUsersTab());  // 首次切换时加载
    }
});
```

---

## 检查项 6：布局计算

**关注点**：是否在循环中调用 `layout()` / `requestLayout()`，是否避免不必要的 `autosize()`。

**通过判定标准**：
- 不在循环中手动调用 `layout()` 或 `requestLayout()`
- 依赖 JavaFX 自动布局传递，仅在必要时手动触发
- 批量修改节点属性后一次性触发布局，而非每次修改后触发

**不通过判定标准**（任一即不通过）：
- 在循环中调用 `layout()` 或 `requestLayout()`（每次迭代触发完整布局传递）
- 不必要地调用 `autosize()`（JavaFX 会自动处理）
- 在 `layoutChildren()` 中执行重型操作

**严重性基线**：Major

**反例**：
```java
// ❌ 循环中调用 requestLayout，每次迭代触发布局传递
for (Node node : nodes) {
    node.setPrefSize(100, 50);
    node.requestLayout();  // 每次迭代都触发布局，性能极差
}
```

**正例**：
```java
// ✅ 批量修改后由 JavaFX 自动合并布局传递
for (Node node : nodes) {
    node.setPrefSize(100, 50);
    // 不手动调用 requestLayout，JavaFX 自动在下一次 pulse 中处理
}
```

---

## 检查项 7：图片加载

**关注点**：大图是否使用后台线程加载并缩放，是否避免在 FX 线程解码大图。

**通过判定标准**：
- 大图在后台线程解码和缩放，完成后通过 `Platform.runLater()` 设置到 `ImageView`
- 使用 `Image` 的后台加载构造器：`new Image(url, true)`（后台加载）
- 大图缩放到显示尺寸后再设置，避免在内存中保存全分辨率图片

**不通过判定标准**（任一即不通过）：
- 在 FX 线程上加载大图（如 `new Image("file:big-photo.jpg")` 同步加载），阻塞 UI
- 加载大图后未缩放，在内存中保存全分辨率图片（内存浪费）
- 在 `ImageView.setImage()` 前在 FX 线程解码

**严重性基线**：Major

**反例**：
```java
// ❌ 在 FX 线程同步加载大图，阻塞 UI
@FXML
private void loadImage() {
    Image image = new Image("file:/photos/large.jpg");  // 同步加载，阻塞
    imageView.setImage(image);
}
```

**正例**：
```java
// ✅ 方式一：后台加载
Image image = new Image("file:/photos/large.jpg", true);  // true = 后台加载
imageView.setImage(image);

// ✅ 方式二：Task 后台解码 + 缩放
Task<Image> loadTask = new Task<>() {
    @Override
    protected Image call() {
        Image full = new Image("file:/photos/large.jpg");
        // 缩放到显示尺寸
        return scaleImage(full, 400, 300);
    }
};
loadTask.setOnSucceeded(e -> imageView.setImage(loadTask.getValue()));
new Thread(loadTask).start();
```

---

## 检查项 8：FilteredList 效率

**关注点**：`FilteredList` 的 predicate 是否过于复杂，大数据量是否考虑索引优化。

**通过判定标准**：
- `FilteredList` 的 predicate 逻辑简洁，无重型操作（无 I/O、无复杂计算）
- 大数据量（> 10000）考虑使用索引或预过滤优化
- predicate 不在每次评估时创建新对象

**不通过判定标准**（任一即不通过）：
- `FilteredList` 的 predicate 中执行数据库查询或文件 I/O
- predicate 过于复杂（多重嵌套条件、正则匹配大量数据），导致过滤延迟
- 大数据量未优化，每次输入变化都全量重新过滤

**严重性基线**：Major
- 降级条件：数据量 < 1000 条 → Minor

**反例**：
```java
// ❌ predicate 中执行重型正则匹配 + 数据库查询
FilteredList<User> filtered = new FilteredList<>(users);
filtered.predicateProperty().bind(Bindings.createObjectBinding(() -> {
    Pattern pattern = Pattern.compile(searchField.getText());  // 每次编译正则
    return user -> {
        // predicate 中查数据库验证，极慢
        return pattern.matcher(user.getName()).matches()
            && dbService.isActive(user.getId());
    };
}, searchField.textProperty()));
```

**正例**：
```java
// ✅ predicate 逻辑简洁，预编译正则，无 I/O
FilteredList<User> filtered = new FilteredList<>(users);
filtered.predicateProperty().bind(Bindings.createObjectBinding(() -> {
    String query = searchField.getText().toLowerCase();
    if (query.isEmpty()) return null;  // 无过滤
    return user -> user.getName().toLowerCase().contains(query);  // 纯内存操作
}, searchField.textProperty()));
```

---

## 检查项 9：绑定效率

**关注点**：是否避免在循环中创建 `Bindings.createXxxBinding()`，计算绑定是否可用更高效的替代方案。

**通过判定标准**：
- 不在循环中创建 `Bindings.createXxxBinding()`（每次创建新绑定链，内存与 CPU 开销大）
- 简单属性映射使用 `bind()` 直接绑定，而非 `createXxxBinding` 包装
- 复杂计算绑定评估是否可用 `SelectBinding` / `ObjectBinding` 等更高效的 API
- 绑定创建在初始化时一次性完成，而非每次事件触发时重建

**不通过判定标准**（任一即不通过）：
- 在循环或高频事件中创建 `Bindings.createXxxBinding()`（绑定链累积）
- 简单的双属性映射使用 `createXxxBinding` 而非直接 `bind` / `add` / `subtract`
- 绑定依赖项过多（> 5 个），导致每次任一依赖变化都全量重算

**严重性基线**：Major

> **补充规则**：绑定效率的详细判定标准与正反例参见 `binding-compliance.md — 绑定效率规则`。

**反例**：
```java
// ❌ 在循环中创建绑定，每次迭代新建绑定链
for (Item item : items) {
    Label label = new Label();
    label.textProperty().bind(Bindings.createStringBinding(
        () -> item.getName() + " (" + item.getCount() + ")",
        item.nameProperty(), item.countProperty()));
    container.getChildren().add(label);
}
// 若 items 频繁变化，绑定链不断累积，内存泄漏 + 性能下降
```

**正例**：
```java
// ✅ 简单映射使用 Bindings.concat 或直接算术绑定
label.textProperty().bind(
    item.nameProperty().concat(" (").concat(item.countProperty().asString()).concat(")")
);
// 或在 CellFactory 中复用单元格，仅 updateItem 时更新文本
```
