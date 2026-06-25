# JavaFX 数据绑定模式指南

本指南全面介绍 JavaFX 的属性系统、单向与双向绑定、计算绑定、数值绑定、ObservableList/Map、TableView 数据绑定、表单校验模式、内存管理以及 JavaFX 21+ 的订阅式监听器。

---

## 一、属性类型系统（Property Type System）

JavaFX 的核心是一套类型化的属性系统，每种基本数据类型都有对应的 Property 接口和实现。

### 1.1 属性类型对照表

| 数据类型 | Property 接口            | 简单实现类                | 只读包装类               |
|----------|--------------------------|---------------------------|--------------------------|
| String   | `StringProperty`         | `SimpleStringProperty`    | `ReadOnlyStringWrapper`  |
| int      | `IntegerProperty`        | `SimpleIntegerProperty`   | `ReadOnlyIntegerWrapper` |
| long     | `LongProperty`           | `SimpleLongProperty`      | `ReadOnlyLongWrapper`    |
| float    | `FloatProperty`          | `SimpleFloatProperty`     | `ReadOnlyFloatWrapper`   |
| double   | `DoubleProperty`         | `SimpleDoubleProperty`    | `ReadOnlyDoubleWrapper`  |
| boolean  | `BooleanProperty`        | `SimpleBooleanProperty`   | `ReadOnlyBooleanWrapper` |
| 任意对象 | `ObjectProperty<T>`      | `SimpleObjectProperty<T>` | `ReadOnlyObjectWrapper<T>`|
| List     | `ListProperty<E>`        | `SimpleListProperty<E>`   | `ReadOnlyListWrapper<E>` |
| Map      | `MapProperty<K,V>`       | `SimpleMapProperty<K,V>`  | `ReadOnlyMapWrapper<K,V>`|
| Set      | `SetProperty<E>`         | `SimpleSetProperty<E>`    | `ReadOnlySetWrapper<E>`  |

### 1.2 SimpleXxxProperty 基本用法

```java
public class Person {
    private final StringProperty name = new SimpleStringProperty(this, "name", "");
    private final IntegerProperty age = new SimpleIntegerProperty(this, "age", 0);
    private final DoubleProperty salary = new SimpleDoubleProperty(this, "salary", 0.0);
    private final BooleanProperty active = new SimpleBooleanProperty(this, "active", true);
    private final ObjectProperty<LocalDate> birthday =
        new SimpleObjectProperty<>(this, "birthday", LocalDate.now());

    // 构造函数参数说明：(bean, name, initialValue)
    // bean 通常为属性所属的对象（this），便于属性溯源
    // name 为属性名称，用于调试和反射

    // 标准 Property 访问器模式
    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }

    public IntegerProperty ageProperty() { return age; }
    public int getAge() { return age.get(); }
    public void setAge(int value) { age.set(value); }

    public DoubleProperty salaryProperty() { return salary; }
    public double getSalary() { return salary.get(); }
    public void setSalary(double value) { salary.set(value); }

    public BooleanProperty activeProperty() { return active; }
    public boolean isActive() { return active.get(); }
    public void setActive(boolean value) { active.set(value); }

    public ObjectProperty<LocalDate> birthdayProperty() { return birthday; }
    public LocalDate getBirthday() { return birthday.get(); }
    public void setBirthday(LocalDate value) { birthday.set(value); }
}
```

### 1.3 SimpleXxxProperty vs ReadOnlyXxxProperty

当属性仅允许内部修改、外部只读时，使用 `ReadOnlyXxxWrapper` 暴露只读视图。

```java
public class Account {
    // 内部可写的 Wrapper
    private final ReadOnlyDoubleWrapper balance = new ReadOnlyDoubleWrapper(this, "balance", 0.0);

    // 对外暴露只读 Property
    public ReadOnlyDoubleProperty balanceProperty() { return balance.getReadOnlyProperty(); }
    public double getBalance() { return balance.get(); }

    // 仅内部方法可修改
    public void deposit(double amount) {
        balance.set(balance.get() + amount);
    }

    public void withdraw(double amount) {
        if (amount > balance.get()) {
            throw new IllegalStateException("余额不足");
        }
        balance.set(balance.get() - amount);
    }
}
```

> 关键区别：`SimpleXxxProperty` 对外完全可读写；`ReadOnlyXxxWrapper` 通过 `getReadOnlyProperty()` 返回只读视图，外部无法 `set()`，但内部仍可通过 wrapper 修改。

---

## 二、单向绑定（Unidirectional Binding）

单向绑定使目标属性自动跟随源属性变化，方向为 **源 → 目标**。目标变为只读。

### 2.1 基本单向绑定

```java
StringProperty source = new SimpleStringProperty("Hello");
StringProperty target = new SimpleStringProperty();

// target 绑定到 source：source 变化时 target 自动更新
target.bind(source);

System.out.println(target.get());  // "Hello"
source.set("World");
System.out.println(target.get());  // "World"

// 绑定后 target 变为只读，调用 set() 会抛出 RuntimeException
// target.set("Error");  // ❌ 抛出异常
```

### 2.2 UI 控件单向绑定示例

```java
// Label 显示 TextField 的内容（单向）
label.textProperty().bind(textField.textProperty());

// 进度条绑定到进度值
progressBar.progressProperty().bind(task.progressProperty());

// 标签显示滑块当前值
valueLabel.textProperty().bind(
    slider.valueProperty().asString("当前值: %.1f"));
```

---

## 三、双向绑定（Bidirectional Binding）

双向绑定使两个属性互相同步，任一方变化都会更新另一方。

### 3.1 基本双向绑定

```java
StringProperty propA = new SimpleStringProperty("A");
StringProperty propB = new SimpleStringProperty("B");

propA.bindBidirectional(propB);

System.out.println(propA.get());  // "A"（保持原值，不立即同步）
propB.set("NewValue");
System.out.println(propA.get());  // "NewValue"

propA.set("Another");
System.out.println(propB.get());  // "Another"
```

### 3.2 类型转换的双向绑定

不同类型属性间的双向绑定需提供转换器：

```java
// TextField（String）与 IntegerProperty 双向绑定
TextField ageField = new TextField();
IntegerProperty age = new SimpleIntegerProperty(25);

ageField.textProperty().bindBidirectional(age, new NumberStringConverter());

// 现在两者互相同步，输入框输入数字会更新 age，修改 age 会更新输入框
```

### 3.3 自定义格式化双向绑定

```java
// 日期与字符串双向绑定，指定格式
ObjectProperty<LocalDate> date = new SimpleObjectProperty<>(LocalDate.now());
TextField dateField = new TextField();

dateField.textProperty().bindBidirectional(date, new StringConverter<>() {
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String toString(LocalDate date) {
        return date == null ? "" : date.format(fmt);
    }

    @Override
    public LocalDate fromString(String text) {
        return (text == null || text.isEmpty()) ? null : LocalDate.parse(text, fmt);
    }
});
```

### 3.4 内容绑定（Content Binding）

内容绑定用于同步两个 `ObservableList` / `ObservableMap` / `ObservableSet` 的**内容**，而非把一个属性绑定到另一个属性。它通过 `bindContent()`（单向）和 `bindContentBidirectional()`（双向）实现，常用于多个视图共享同一数据源或主从列表同步。

> 注意：内容绑定作用于集合本身，调用方必须是 `ListProperty` / `MapProperty` 等（通过 `SimpleListProperty` 包装），普通 `ObservableList` 不直接支持 `bindContent`。

```java
ObservableList<String> sourceList = FXCollections.observableArrayList("A", "B", "C");
ListProperty<String> targetList = new SimpleListProperty<>(FXCollections.observableArrayList());

// 单向内容绑定：源列表变化同步到目标列表（目标变为只读）
targetList.bindContent(sourceList);

sourceList.add("D");
System.out.println(targetList.get());  // [A, B, C, D]
```

```java
ListProperty<String> list1 = new SimpleListProperty<>(FXCollections.observableArrayList("1", "2"));
ListProperty<String> list2 = new SimpleListProperty<>(FXCollections.observableArrayList());

// 双向内容绑定：两个列表双向同步
list1.bindContentBidirectional(list2);

list2.add("3");
System.out.println(list1.get());  // [1, 2, 3]

list1.remove("1");
System.out.println(list2.get());  // [2, 3]
```

**适用场景**

- **多视图共享同一数据源**：主数据列表与多个子视图的局部列表保持同步，任一视图修改数据后其他视图自动更新。
- **主从列表同步**：主表选中项驱动的明细列表与缓存列表双向绑定，避免手动复制元素。
- **聚合视图**：将多个来源列表的内容汇聚到一个展示列表。

> 与普通属性绑定的区别：`bindContent` 同步的是集合元素，源列表增删元素会反映到目标；但若源列表元素自身属性变化，需配合 extractor 才能触发更新。

---

## 四、计算绑定（Computed Bindings）

`Bindings` 工具类提供工厂方法，基于一个或多个源属性创建派生的只读绑定。

### 4.1 createStringBinding

```java
StringProperty firstName = new SimpleStringProperty("张");
StringProperty lastName = new SimpleStringProperty("三");

// 拼接全名，依赖 firstName 和 lastName
StringBinding fullName = Bindings.createStringBinding(
    () -> firstName.get() + lastName.get(),
    firstName, lastName
);

System.out.println(fullName.get());  // "张三"
lastName.set("四");
System.out.println(fullName.get());  // "张四"
```

### 4.2 createBooleanBinding

```java
StringProperty username = new SimpleStringProperty();
StringProperty password = new SimpleStringProperty();

// 表单是否有效
BooleanBinding formValid = Bindings.createBooleanBinding(
    () -> username.get() != null && username.get().length() >= 3
       && password.get() != null && password.get().length() >= 6,
    username, password
);

// 绑定到按钮禁用状态
loginButton.disableProperty().bind(formValid.not());
```

### 4.3 createIntegerBinding / createObjectBinding

```java
ObservableList<Item> items = FXCollections.observableArrayList();

// 计算列表中已完成项的数量
IntegerBinding completedCount = Bindings.createIntegerBinding(
    () -> (int) items.stream().filter(Item::isCompleted).count(),
    items
);

// 计算选中项的对象
ObjectProperty<Item> selected = new SimpleObjectProperty<>();
ObjectBinding<String> selectedName = Bindings.createObjectBinding(
    () -> selected.get() == null ? "无选中" : selected.get().getName(),
    selected
);
```

### 4.4 selectBinding（嵌套属性访问）

```java
// 访问嵌套对象的属性
ObjectProperty<Person> person = new SimpleObjectProperty<>(new Person("Alice"));

// 方式1：Bindings.select（字符串方式，已过时但可用）
StringBinding name1 = Bindings.selectString(person, "name");

// 方式2（推荐）：createStringBinding + null 安全
StringBinding name2 = Bindings.createStringBinding(
    () -> person.get() == null ? "" : person.get().getName(),
    person
);
// 当 person 或 person.name 变化时自动更新
```

---

## 五、数值绑定（NumberBinding）

数值属性支持算术运算绑定，结果为 `NumberBinding`。

### 5.1 算术运算

```java
IntegerProperty quantity = new SimpleIntegerProperty(5);
DoubleProperty price = new SimpleDoubleProperty(19.99);

// 乘法：总价
NumberBinding total = quantity.multiply(price);
System.out.println(total.doubleValue());  // 99.95

// 加法
IntegerProperty a = new SimpleIntegerProperty(10);
IntegerProperty b = new SimpleIntegerProperty(3);
NumberBinding sum = a.add(b);        // 13
NumberBinding diff = a.subtract(b);  // 7
NumberBinding product = a.multiply(b); // 30
NumberBinding quotient = a.divide(b);  // 3.333...
```

### 5.2 链式运算

```java
DoubleProperty basePrice = new SimpleDoubleProperty(100.0);
DoubleProperty taxRate = new SimpleDoubleProperty(0.08);   // 8% 税率
DoubleProperty discount = new SimpleDoubleProperty(10.0);  // 折扣

// 最终价格 = (basePrice - discount) * (1 + taxRate)
NumberBinding finalPrice = basePrice
    .subtract(discount)
    .multiply(taxRate.add(1.0));

System.out.println(finalPrice.doubleValue());  // (100-10) * 1.08 = 97.2
```

### 5.3 数值比较绑定

```java
IntegerProperty stock = new SimpleIntegerProperty(5);
IntegerProperty threshold = new SimpleIntegerProperty(10);

// 库存是否低于阈值
BooleanBinding lowStock = stock.lessThan(threshold);
warningLabel.visibleProperty().bind(lowStock);
```

### 5.4 条件绑定（when/then/otherwise）

```java
IntegerProperty score = new SimpleIntegerProperty(75);

// 根据分数返回及格/不及格文字
StringBinding result = Bindings.when(score.greaterThanOrEqualTo(60))
    .then("及格")
    .otherwise("不及格");
```

---

## 六、ObservableList 与 ListChangeListener

`ObservableList` 是可观察的列表，任何增删改操作都会通知监听器。

### 6.1 创建与基本操作

```java
ObservableList<String> names = FXCollections.observableArrayList();
names.add("Alice");
names.addAll("Bob", "Charlie");
names.remove("Bob");
names.set(0, "Alicia");  // 替换
```

### 6.2 ListChangeListener

```java
names.addListener((ListChangeListener<String>) change -> {
    while (change.next()) {
        if (change.wasAdded()) {
            System.out.println("添加: " + change.getAddedSubList());
        }
        if (change.wasRemoved()) {
            System.out.println("移除: " + change.getRemoved());
        }
        if (change.wasUpdated()) {
            System.out.println("更新: 索引 " + change.getFrom() + " 到 " + change.getTo());
        }
        if (change.wasReplaced()) {
            System.out.println("替换操作");
        }
    }
});
```

### 6.3 使用 extractors 自动观察元素属性

默认情况下，ObservableList 仅监听列表结构变化（增删）。若需监听元素内部属性变化，需使用 extractor：

```java
// 当 Task 的 title 或 completed 属性变化时也触发列表更新事件
ObservableList<Task> tasks = FXCollections.observableArrayList(
    task -> new Observable[]{ task.titleProperty(), task.completedProperty() }
);

tasks.addListener((ListChangeListener<Task>) c -> {
    while (c.next()) {
        if (c.wasUpdated()) {
            System.out.println("任务属性被修改: " + c.getList().subList(c.getFrom(), c.getTo()));
        }
    }
});

Task t = new Task("学习");
tasks.add(t);
t.setCompleted(true);  // 触发 wasUpdated 事件
```

---

## 七、ObservableMap 与 MapChangeListener

```java
ObservableMap<String, Integer> scores = FXCollections.observableHashMap();

scores.addListener((MapChangeListener<String, Integer>) change -> {
    if (change.wasAdded()) {
        System.out.println("添加/更新键: " + change.getKey()
            + " = " + change.getValueAdded());
    }
    if (change.wasRemoved()) {
        System.out.println("移除键: " + change.getKey()
            + " 旧值: " + change.getValueRemoved());
    }
});

scores.put("Math", 95);    // 触发 wasAdded
scores.put("Math", 98);    // 触发 wasAdded + wasRemoved（更新）
scores.remove("Math");     // 触发 wasRemoved
```

---

## 八、TableView 数据绑定（FilteredList + SortedList）

`FilteredList` 和 `SortedList` 是 TableView 实现搜索过滤与排序的标准模式。

### 8.1 完整示例

```java
public class UserTableController implements Initializable {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> nameCol;
    @FXML private TableColumn<User, Integer> ageCol;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> ageFilterCombo;

    // 原始数据
    private final ObservableList<User> masterData = FXCollections.observableArrayList();

    // 过滤后的数据
    private final FilteredList<User> filteredData = new FilteredList<>(masterData, p -> true);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 加载初始数据
        masterData.addAll(
            new User("Alice", 25),
            new User("Bob", 30),
            new User("Charlie", 22),
            new User("David", 35)
        );

        // 排序后的数据（包装 FilteredList）
        SortedList<User> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(userTable.comparatorProperty());

        // 绑定到 TableView
        userTable.setItems(sortedData);

        // 列映射
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        ageCol.setCellValueFactory(c -> c.getValue().ageProperty().asObject());

        // 搜索过滤：按姓名包含关键字
        searchField.textProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        ageFilterCombo.getItems().addAll("全部", "<30", ">=30");
        ageFilterCombo.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> updateFilter());
    }

    private void updateFilter() {
        String keyword = searchField.getText();
        String ageFilter = ageFilterCombo.getValue();

        filteredData.setPredicate(user -> {
            // 姓名过滤
            boolean nameMatch = keyword == null || keyword.isEmpty()
                || user.getName().toLowerCase().contains(keyword.toLowerCase());

            // 年龄过滤
            boolean ageMatch = true;
            if (">=30".equals(ageFilter)) {
                ageMatch = user.getAge() >= 30;
            } else if ("<30".equals(ageFilter)) {
                ageMatch = user.getAge() < 30;
            }

            return nameMatch && ageMatch;
        });
    }
}
```

### 8.2 数据流

```
masterData (ObservableList)
    ↓ 过滤
filteredData (FilteredList)
    ↓ 排序
sortedData (SortedList)
    ↓ 绑定
userTable (TableView)
```

> 关键点：`SortedList` 的 comparator 绑定到 TableView 的 comparator，这样点击列头排序时自动生效。

---

## 九、表单校验模式

利用绑定实现声明式表单校验。

### 9.1 校验绑定示例

```java
public class RegistrationForm {

    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty password = new SimpleStringProperty();
    private final StringProperty confirmPassword = new SimpleStringProperty();

    // 各字段校验结果
    private final BooleanBinding usernameValid = Bindings.createBooleanBinding(
        () -> username.get() != null && username.get().matches("[a-zA-Z0-9_]{3,20}"),
        username
    );

    private final BooleanBinding emailValid = Bindings.createBooleanBinding(
        () -> email.get() != null && email.get().matches("^[\\w.-]+@[\\w.-]+\\.\\w+$"),
        email
    );

    private final BooleanBinding passwordValid = Bindings.createBooleanBinding(
        () -> password.get() != null && password.get().length() >= 8
           && password.get().matches(".*[A-Z].*") && password.get().matches(".*[0-9].*"),
        password
    );

    private final BooleanBinding passwordsMatch = Bindings.createBooleanBinding(
        () -> password.get() != null && password.get().equals(confirmPassword.get()),
        password, confirmPassword
    );

    // 整体表单是否有效
    private final BooleanBinding formValid = usernameValid
        .and(emailValid)
        .and(passwordValid)
        .and(passwordsMatch);

    // 错误提示
    private final StringBinding usernameError = Bindings.when(usernameValid)
        .then("").otherwise("用户名需为3-20位字母数字下划线");
    private final StringBinding emailError = Bindings.when(emailValid)
        .then("").otherwise("邮箱格式不正确");
    private final StringBinding passwordError = Bindings.when(passwordValid)
        .then("").otherwise("密码至少8位，含大写字母和数字");
    private final StringBinding confirmError = Bindings.when(passwordsMatch)
        .then("").otherwise("两次密码不一致");

    // 在 Controller 中绑定到 UI
    public void bindToUI(TextField userField, TextField emailField,
                         TextField passField, TextField confirmField,
                         Label userErrLabel, Label emailErrLabel,
                         Label passErrLabel, Label confirmErrLabel,
                         Button submitButton) {
        userField.textProperty().bindBidirectional(username);
        emailField.textProperty().bindBidirectional(email);
        passField.textProperty().bindBidirectional(password);
        confirmField.textProperty().bindBidirectional(confirmPassword);

        userErrLabel.textProperty().bind(usernameError);
        emailErrLabel.textProperty().bind(emailError);
        passErrLabel.textProperty().bind(passwordError);
        confirmErrLabel.textProperty().bind(confirmError);

        // 错误标签可见性
        userErrLabel.visibleProperty().bind(usernameValid.not());
        emailErrLabel.visibleProperty().bind(emailValid.not());
        passErrLabel.visibleProperty().bind(passwordValid.not());
        confirmErrLabel.visibleProperty().bind(passwordsMatch.not());

        // 提交按钮启用状态
        submitButton.disableProperty().bind(formValid.not());
    }
}
```

---

## 十、内存管理：解绑与监听器移除

绑定和监听器会持有对象引用，若不及时清理会导致内存泄漏。

### 10.1 解除单向绑定

```java
StringProperty target = new SimpleStringProperty();
target.bind(source);

// 解除绑定
target.unbind();
// 解绑后 target 恢复可写，不再跟随 source
```

### 10.2 解除双向绑定

```java
propA.bindBidirectional(propB);
// 解除双向绑定
propA.unbindBidirectional(propB);
```

### 10.3 移除 ChangeListener / InvalidationListener

```java
ChangeListener<String> listener = (obs, oldVal, newVal) -> {
    System.out.println("变化: " + oldVal + " -> " + newVal);
};

nameProperty.addListener(listener);

// 必须移除同一引用的监听器
nameProperty.removeListener(listener);
```

> 注意：使用 Lambda 表达式添加监听器时，必须保存引用才能移除。匿名内部类同理。

### 10.4 使用 WeakChangeListener 避免泄漏

当无法保证移除监听器时，使用弱引用监听器：

```java
// WeakChangeListener 不会阻止监听器对象被 GC 回收
nameProperty.addListener(new WeakChangeListener<>((obs, oldVal, newVal) -> {
    System.out.println("弱监听: " + newVal);
}));
```

> 弱监听器适合监听器生命周期短于被监听属性的场景。但需注意：若监听器 Lambda 没有其他强引用，可能被过早回收。

### 10.5 常见内存泄漏场景与对策

| 场景                                   | 对策                                              |
|----------------------------------------|---------------------------------------------------|
| Controller 中添加监听器未移除          | 在自定义 `dispose()` 方法中移除监听器，并通过 `stage.setOnCloseRequest()` 或视图切换回调手动触发 |
| 短生命周期对象监听长生命周期属性       | 使用 `WeakChangeListener`                         |
| 静态属性上的监听器                     | 应用关闭时显式移除                                |
| 双向绑定未解除                         | 不再需要时调用 `unbindBidirectional()`            |
| ObservableList 的 extractor 监听       | 列表清空或替换时注意元素监听器释放                |

### 10.6 绑定链内存泄漏专题

绑定链（binding chain）是指多个 Binding/Property 相互依赖形成的引用链。一旦链中某个节点无法被 GC，整条链及其依赖的 UI 控件都可能无法回收，是 JavaFX 内存泄漏的高发区。

**1. `Bindings.createXxxBinding()` 的强引用问题**

`Bindings.createStringBinding(...)` 等工厂方法返回的 Binding 对象会**强引用**传入的依赖属性（用于注册监听并计算值）。如果该 Binding 被绑定到某个长生命周期属性（如静态属性、单例 ViewModel 的字段），那么所有依赖属性都会被间接持有而无法回收。

```java
// ❌ 危险：binding 被长生命周期属性持有，依赖的 sourceA/sourceB 也无法释放
staticProperty.bind(Bindings.createStringBinding(
    () -> sourceA.get() + sourceB.get(),
    sourceA, sourceB
));
// 解绑时必须显式 unbind，否则 binding 仍持有 sourceA/sourceB
staticProperty.unbind();
```

对策：长生命周期目标上的临时 Binding，使用完毕后及时 `unbind()`；或改用 `WeakReference` 包装依赖。

**2. `ObjectProperty<UI控件>` 之间的绑定链导致 UI 子树无法 GC**

当多个 `ObjectProperty<Node>` 通过 `bind()` 形成链路时，目标属性持有 Binding，Binding 持有源属性，源属性又可能持有 UI 控件。一旦链路中某个 Property 被长生命周期对象引用，整条 UI 子树都无法被 GC 回收，即使该子树已从场景图中移除。

```java
// ❌ 反模式：动态创建的子节点属性绑定到长生命周期的父属性
parentProperty.bind(childProperty);
// child 从 Scene 移除后，若 parentProperty 仍存活且未解绑，child 整棵子树无法回收
```

对策：动态 UI 子树从场景图移除时，务必调用 `unbind()` / `unbindBidirectional()` 断开与长生命周期属性的连接。

**3. `TableView` / `ListView` cellFactory 中的监听器清理**

`cellFactory` 在 cell 复用时会被反复调用 `updateItem()`。如果在 `updateItem()` 中为 cell 内部控件添加监听器却不清理，每次复用都会累积一层监听器，导致 cell 持有的旧数据对象无法释放，并造成重复触发。

```java
// ❌ 反模式：每次 updateItem 都新增监听器，从不移除
@Override
protected void updateItem(Task task, boolean empty) {
    super.updateItem(task, empty);
    if (empty || task == null) { return; }
    task.titleProperty().addListener((obs, o, n) -> setText(n));  // 复用 N 次后累积 N 个监听器
}

// ✅ 正确做法：在 empty/旧数据分支中移除旧监听器，或使用 WeakChangeListener
private Task previousTask;
private ChangeListener<String> titleListener;
@Override
protected void updateItem(Task task, boolean empty) {
    super.updateItem(task, empty);
    if (titleListener != null && previousTask != null) {
        previousTask.titleProperty().removeListener(titleListener);  // 清理上一个 cell 数据的监听器
        titleListener = null;
    }
    if (empty || task == null) {
        setText(null);
    } else {
        setText(task.getTitle());
        titleListener = (obs, o, n) -> setText(n);
        task.titleProperty().addListener(titleListener);
        previousTask = task;
    }
}
```

> 最佳实践：cellFactory 中优先使用 **数据绑定**（`textProperty().bind(task.titleProperty())`）并在 `updateItem` 的 empty 分支 `unbind()`，而非手动 `addListener`，可从根源避免监听器累积。

---

## 十一、JavaFX 21+ 订阅式监听器（Subscription-based Listeners）

JavaFX 21 引入了基于 `Subscription` 的监听器 API，提供更现代、更安全的资源管理方式，替代传统的 `addListener` / `removeListener` 模式。

### 11.1 基本用法

```java
// JavaFX 21+：subscribe 返回 Subscription
Subscription subscription = nameProperty.subscribe((obs, oldVal, newVal) -> {
    System.out.println("名称变化: " + oldVal + " -> " + newVal);
});

// 不再需要时取消订阅
subscription.unsubscribe();
// 取消后监听器自动移除，且 unsubscribe() 是幂等的（可重复调用）
```

### 11.2 ObservableValue 的订阅方法

```java
// 订阅值变化（含旧值和新值）
Subscription sub1 = property.subscribe((observable, oldValue, newValue) -> {
    System.out.println("变化: " + oldValue + " -> " + newValue);
});

// 订阅仅新值
Subscription sub2 = property.subscribe(newValue -> {
    System.out.println("新值: " + newValue);
});

// 订阅 invalidation（失效通知）
Subscription sub3 = property.subscribe((InvalidationListener) observable -> {
    System.out.println("属性已失效");
});
```

### 11.3 ObservableList 的订阅方法

```java
ObservableList<String> list = FXCollections.observableArrayList();

// 订阅列表变化
Subscription sub = list.subscribe(change -> {
    while (change.next()) {
        if (change.wasAdded()) {
            System.out.println("添加: " + change.getAddedSubList());
        }
        if (change.wasRemoved()) {
            System.out.println("移除: " + change.getRemoved());
        }
    }
});
```

### 11.4 组合订阅管理

多个 Subscription 可合并管理：

```java
Subscription sub1 = propA.subscribe(...);
Subscription sub2 = propB.subscribe(...);
Subscription sub3 = list.subscribe(...);

// 合并为一个 Subscription
Subscription combined = Subscription.combine(sub1, sub2, sub3);

// 一次性取消所有
combined.unsubscribe();
```

### 11.5 try-with-resources 模式

`Subscription` 实现 `AutoCloseable`，支持 try-with-resources：

```java
try (Subscription sub = property.subscribe(val -> update(val))) {
    // 在此作用域内监听生效
    doWork();
} // 自动 unsubscribe
```

### 11.6 传统 API 与 Subscription API 对比

| 特性            | addListener / removeListener          | subscribe (Subscription)              |
|-----------------|---------------------------------------|----------------------------------------|
| 移除方式        | 需保存监听器引用手动 remove           | 调用 `subscription.unsubscribe()`      |
| 幂等性          | 重复 remove 无害但需注意引用一致性    | `unsubscribe()` 幂等，可安全多次调用   |
| 组合管理        | 需手动逐个移除                        | `Subscription.combine()` 一键管理      |
| 资源安全        | 易遗漏 remove 导致泄漏                | 配合 try-with-resources 更安全         |
| 可用版本        | 所有 JavaFX 版本                      | JavaFX 21+                             |

### 11.7 迁移建议

- 新项目（JavaFX 21+）优先使用 `subscribe` API。
- 需兼容 JavaFX 17 的项目继续使用 `addListener`，但务必配套 `removeListener`。
- 混合使用时保持一致性，同一模块内统一风格。

---

## 十二、绑定模式最佳实践总结

| 实践                              | 说明                                                       |
|-----------------------------------|------------------------------------------------------------|
| 优先使用声明式绑定                | 用 `bind()` / `Bindings.createXxxBinding()` 代替手动监听刷新 |
| 区分单向与双向                    | 仅展示用单向，表单输入用双向                                |
| 计算属性用 createXxxBinding        | 避免在监听器中手动计算，用绑定声明依赖关系                  |
| 数值运算用 NumberBinding           | 链式算术绑定清晰表达计算逻辑                                |
| TableView 用 FilteredList+SortedList | 标准过滤排序模式，避免手动操作列表                        |
| 表单校验用 BooleanBinding 组合     | 声明式校验，自动驱动 UI 状态                                |
| 及时解绑与移除监听器              | 防止内存泄漏，JavaFX 21+ 优先用 Subscription               |
| 使用 extractor 观察元素属性        | ObservableList 需 extractor 才能监听元素内部变化            |
| 只读属性用 ReadOnlyXxxWrapper      | 保护内部状态，仅暴露只读视图                                |
