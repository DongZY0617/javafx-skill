# 编译验证规则

本文档是"编译验证"维度的判定依据，管辖 7 个检查项。通过执行 `mvn compile`（或 `gradle compileJava`）解析编译器输出，识别编译错误与警告，将"纸面正确"的代码推进到"可编译"。此维度违规默认为 Critical。与 `javafx-developer` 的质量检查清单·语法检查条目同源。

> **核心原则**：编译验证是动态验证链的第一环。编译失败时必须短路，跳过运行验证与打包验证（无法运行未编译的代码）。所有编译错误（`[ERROR]`）均为 Critical 且不可降级；编译警告（`[WARNING]`）按影响程度评定为 Minor 或 Major。

---

## 检查项 1：语法编译

**关注点**：所有 Java 源文件能否通过 `javac` 编译，无语法错误、未解析符号、方法签名不匹配等问题。

**通过判定标准**：
- 执行 `mvn compile -q`（或 `gradle compileJava --quiet`）退出码为 0，无 `[ERROR]` 输出
- 所有源文件无语法错误（缺分号、括号不匹配、非法关键字等）
- 所有符号引用可解析（无 `cannot find symbol`、`incompatible types` 等）
- 方法调用签名与方法定义匹配（参数数量、类型、返回值）

**不通过判定标准**（任一即不通过）：
- 编译器输出含 `[ERROR] /path/File.java:[line,col] ...` 格式的编译错误
- 存在 `cannot find symbol`（未导入类、未定义变量、拼写错误的类名）
- 存在 `incompatible types`（类型不兼容赋值或传参）
- 存在 `method ... in class ... cannot be applied to given types`（方法签名不匹配）
- 缺少导入语句导致符号无法解析（`import` 缺失）

**严重性基线**：Critical（不可降级，编译失败则项目无法运行）

**反例**：
```java
// ❌ 缺少导入，编译报 cannot find symbol
package com.example;

public class UserController {
    // List / ArrayList 未导入，编译失败
    private List<User> users = new ArrayList<>();

    public void add(User u) {
        users.add(u);
    }
}
```
编译器输出：
```
[ERROR] /src/main/java/com/example/UserController.java:[5,13] cannot find symbol
  symbol:   class List
  location: class com.example.UserController
[ERROR] /src/main/java/com/example/UserController.java:[5,27] cannot find symbol
  symbol:   class ArrayList
```

**正例**：
```java
// ✅ 补全导入，编译通过
package com.example;

import java.util.ArrayList;
import java.util.List;

public class UserController {
    private List<User> users = new ArrayList<>();

    public void add(User u) {
        users.add(u);
    }
}
```

---

## 检查项 2：依赖解析

**关注点**：Maven/Gradle 依赖能否全部解析，编译期无 `ClassNotFoundException` 或 `NoClassDefFoundError`，JavaFX 依赖版本是否存在且可用。

**通过判定标准**：
- `pom.xml` / `build.gradle` 中声明的所有依赖均可从仓库解析，无 `Could not resolve dependencies` / `Could not find artifact` 错误
- JavaFX 依赖（`javafx-controls`、`javafx-fxml` 等）版本一致且存在
- 依赖间无版本冲突导致编译期类找不到
- 非模块化项目的 JavaFX 依赖通过 `javafx-maven-plugin` 或 `javafx-gradle-plugin` 正确引入

**不通过判定标准**（任一即不通过）：
- 编译输出含 `Could not resolve dependencies for project ...`
- 编译输出含 `Could not find artifact org.openjfx:javafx-controls:jar:xx`
- JavaFX 各模块版本不一致（如 `javafx-controls` 为 21，`javafx-fxml` 为 17）导致 `NoClassDefFoundError`
- 缺少 `javafx-fxml` 依赖但代码使用 `FXMLLoader`，编译期报 `package javafx.fxml does not exist`

**严重性基线**：Critical（不可降级，依赖缺失则无法编译）

**反例**：
```xml
<!-- ❌ 使用 FXMLLoader 但 pom.xml 未声明 javafx-fxml 依赖 -->
<dependencies>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>21.0.2</version>
    </dependency>
    <!-- 缺少 javafx-fxml，编译 FXMLLoader 报错 -->
</dependencies>
```
编译器输出：
```
[ERROR] /src/main/java/com/example/App.java:[3,24] package javafx.fxml does not exist
```

**正例**：
```xml
<!-- ✅ 声明 javafx-fxml 依赖，版本与 javafx-controls 一致 -->
<dependencies>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>21.0.2</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>21.0.2</version>
    </dependency>
</dependencies>
```

---

## 检查项 3：模块配置

**关注点**：`module-info.java` 的 `requires` / `exports` / `opens` 声明是否与实际代码匹配，是否覆盖项目所有包及 JavaFX 反射需求。

**通过判定标准**：
- `module-info.java` 中 `requires javafx.controls`、`requires javafx.fxml` 等声明齐全，覆盖代码实际使用的 JavaFX 模块
- `exports` 声明覆盖需要被外部访问的包
- `opens` 声明覆盖所有需要被 JavaFX 反射访问的包：
  - 控制器所在包 `opens com.example.controller to javafx.fxml`
  - 模型属性所在包 `opens com.example.model to javafx.controls`（供 `PropertyValueFactory` 反射）
  - 国际化资源包所在包（如需 `ResourceBundle` 反射加载）
- 无多余 `requires`（未使用的模块声明不影响编译，但应清理）

**不通过判定标准**（任一即不通过）：
- 使用 `FXMLLoader` 但 `module-info.java` 缺少 `requires javafx.fxml`
- 控制器包未 `opens ... to javafx.fxml`（编译通过但运行时反射失败，此处在编译维度标记为模块配置缺失）
- 模型包未 `opens ... to javafx.controls`（`PropertyValueFactory` 运行时反射失败）
- `exports` 缺失导致其他模块无法访问（如库项目）
- 声明了 `requires` 但项目未引入对应依赖（编译报 `module not found`）

**严重性基线**：Critical
- 降级条件：缺失的 `opens` 不影响当前功能（如未使用 `PropertyValueFactory`、未用反射）→ Major
- 升级条件：—（保持 Critical，因运行时必然反射失败）

> **关键事实**：`opens` 缺失在编译期不会报错（编译能通过），但在运行时必然导致反射失败。编译验证阶段需主动比对 `module-info.java` 与实际包结构，提前标记此类"编译通过但运行失败"的隐患。

**反例**：
```java
// ❌ module-info.java 缺少 opens，FXML 控制器和模型属性均无法反射
module com.example.app {
    requires javafx.controls;
    requires javafx.fxml;
    // 缺少 opens com.example.controller to javafx.fxml;
    // 缺少 opens com.example.model to javafx.controls;
    exports com.example;
}
```

**正例**：
```java
// ✅ 完整声明 requires / opens
module com.example.app {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.example.controller to javafx.fxml;
    opens com.example.model to javafx.controls;

    exports com.example;
}
```

---

## 检查项 4：FXML 编译关联

**关注点**：FXML 中 `fx:controller` 指向的 Controller 类全限定名能否被类加载器解析，Controller 类是否存在且位于可访问的包中。

**通过判定标准**：
- FXML 文件中 `fx:controller="com.example.controller.UserController"` 指向的类存在且全限定名正确
- Controller 类所在包已 `opens ... to javafx.fxml`（模块化项目）或在类路径上（非模块化项目）
- Controller 类有无参构造方法（`FXMLLoader` 实例化要求）
- `onAction="#handleSave"` 等事件处理器在 Controller 中有对应方法

**不通过判定标准**（任一即不通过）：
- `fx:controller` 指向的类不存在（拼写错误、包名错误、类已删除/重命名）
- `fx:controller` 指向的类不在 `opens` 包中（编译期可识别模块配置问题）
- Controller 类缺少无参构造方法（编译期可通过反射需求识别）
- `onAction` 引用的方法在 Controller 中不存在（编译期无法直接识别，但可作为关联检查项提示）

**严重性基线**：Critical
- 降级条件：—（运行时必然抛 `LoadException`，不可降级）
- 升级条件：—

**反例**：
```xml
<!-- ❌ fx:controller 路径错误，类不存在或包名拼写错误 -->
<VBox xmlns="http://javafx.com/javafx/21"
      xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="com.example.controler.UserController">
    <!-- "controler" 拼写错误，应为 "controller" -->
</VBox>
```
运行时输出（编译期关联检查应提前标记）：
```
Caused by: java.lang.ClassNotFoundException: com.example.controler.UserController
```

**正例**：
```xml
<!-- ✅ fx:controller 全限定名与实际类一致 -->
<VBox xmlns="http://javafx.com/javafx/21"
      xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="com.example.controller.UserController">
</VBox>
```
```java
// 对应的 Controller 类，包名 com.example.controller，无参构造（默认）
package com.example.controller;

public class UserController {
    public UserController() { }  // FXMLLoader 实例化所需
}
```

---

## 检查项 5：泛型与类型

**关注点**：`TableView<User>` 等泛型使用是否类型安全，`cellValueFactory` / `cellFactory` 回调签名是否正确，`PropertyValueFactory` 泛型参数是否匹配。

**通过判定标准**：
- `TableView<T>`、`TableColumn<T, S>`、`ListView<T>` 等泛型参数类型一致
- `cellValueFactory` 回调签名 `Callback<CellDataFeatures<T, S>, ObservableValue<S>>` 正确
- `PropertyValueFactory<T, S>` 的泛型参数与 `TableColumn<T, S>` 一致
- `ObservableList<T>` 的元素类型与 `TableView<T>` / `ListView<T>` 一致
- 无 `unchecked` 警告或已用 `@SuppressWarnings("unchecked")` 显式标注并确认安全

**不通过判定标准**（任一即不通过）：
- `TableView<User>` 与 `TableColumn<Product, String>` 泛型参数不匹配（编译报 `incompatible types`）
- `PropertyValueFactory` 裸用（`new PropertyValueFactory("name")`）导致 `unchecked` 警告且类型不安全
- `cellValueFactory` 回调返回类型与 `TableColumn` 声明的 `S` 不一致
- `setItems()` 传入的 `ObservableList` 元素类型与 `TableView` 泛型不匹配

**严重性基线**：Major
- 降级条件：仅有 `unchecked` 警告且类型实际安全（运行时不抛 `ClassCastException`）→ Minor
- 升级条件：类型不匹配导致编译失败 → Critical

**反例**：
```java
// ❌ PropertyValueFactory 裸用 + 泛型不匹配，unchecked 警告且类型不安全
TableView<User> table = new TableView<>();
TableColumn<User, String> nameCol = new TableColumn<>("姓名");
// ❌ 裸用 PropertyValueFactory，丢失泛型约束
nameCol.setCellValueFactory(new PropertyValueFactory("name"));
// 编译警告：unchecked call to PropertyValueFactory(K)
```

**正例**：
```java
// ✅ 明确泛型参数，类型安全
TableView<User> table = new TableView<>();
TableColumn<User, String> nameCol = new TableColumn<>("姓名");
nameCol.setCellValueFactory(
    new PropertyValueFactory<User, String>("name"));
// 或使用类型安全的 lambda
nameCol.setCellValueFactory(cell -> cell.getValue().nameProperty());
```

---

## 检查项 6：资源路径编译期检查

**关注点**：`getClass().getResource("/fxml/xxx.fxml")` 等引用的资源路径在编译产物中是否存在，资源文件是否被正确打包到 `target/classes`。

**通过判定标准**：
- 代码中 `getResource("/fxml/xxx.fxml")` 引用的路径在 `src/main/resources/fxml/xxx.fxml` 存在
- 编译后 `target/classes/fxml/xxx.fxml` 存在（Maven 资源插件已复制）
- CSS 文件 `getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm())` 引用的路径存在
- 国际化资源包 `ResourceBundle.getBundle("i18n.messages")` 对应的 `.properties` 文件存在
- 资源路径以 `/` 开头表示从 classpath 根开始，不以 `/` 开头表示从类所在包开始，使用方式正确

**不通过判定标准**（任一即不通过）：
- `getResource()` 引用的路径在 `src/main/resources` 下不存在（编译期可通过文件比对识别）
- 资源文件存在但不在 `resources` 目录，未被 Maven 复制到 `target/classes`
- 资源路径大小写错误（Linux 下文件系统区分大小写，`/Fxml/Main.fxml` 与 `/fxml/Main.fxml` 不同）
- `pom.xml` 的 `<resources>` 配置过滤掉了非 `.java` 文件

**严重性基线**：Major
- 降级条件：资源缺失但有默认回退逻辑（如 `getResource` 返回 null 时用默认值）→ Minor
- 升级条件：资源缺失导致运行时 `NullPointerException`（`getResource` 返回 null 后直接 `.toExternalForm()`）→ Critical

**反例**：
```java
// ❌ 资源路径错误，src/main/resources 下无 user-view.fxml
@FXML
private void loadUserView() throws IOException {
    // 路径写错或文件不存在，getResource 返回 null
    Parent root = FXMLLoader.load(getClass().getResource("/fxml/user-view.fxml"));
    // 紧接着调用 .toExternalForm() 将抛 NullPointerException
}
```

**正例**：
```java
// ✅ 资源路径正确，且 src/main/resources/fxml/user-view.fxml 存在
@FXML
private void loadUserView() throws IOException {
    Parent root = FXMLLoader.load(getClass().getResource("/fxml/user-view.fxml"));
}
```
```
src/main/resources/
└── fxml/
    └── user-view.fxml   ← 资源文件存在，编译后复制到 target/classes/fxml/
```

---

## 检查项 7：编译警告排查

**关注点**：未使用导入、deprecation 警告、`unchecked` / `rawtypes` 警告是否影响运行，是否掩盖真实问题。

**通过判定标准**：
- 执行 `mvn compile` 无 `[WARNING]` 输出，或所有警告均已评估确认不影响运行
- 无未使用导入（`unused import`）
- 使用已废弃 API（`@Deprecated`）处有 `@SuppressWarnings("deprecation")` 或明确替代方案
- `unchecked` / `rawtypes` 警告已用泛型参数消除或显式标注 `@SuppressWarnings` 并确认安全
- 编译输出无 `redundant cast`、`unnecessary boxing` 等可清理警告

**不通过判定标准**（任一即不通过）：
- 存在大量未使用导入，降低代码可读性
- 使用已废弃 API 且无替代方案评估
- `unchecked` 警告掩盖真实的类型不安全操作（如裸用 `PropertyValueFactory`）
- `rawtypes` 警告（裸用泛型类型）导致类型约束丢失

**严重性基线**：Minor
- 降级条件：—（警告级别，已是最Minor）
- 升级条件：大量 `unchecked` 警告可能掩盖真实类型错误 → Major

> **关键事实**：编译警告本身不阻断编译，但 `unchecked` / `rawtypes` 警告往往是运行时 `ClassCastException` 的前兆。排查时应区分"无害警告"（如已确认安全的第三方 API 调用）与"有害警告"（如自身代码的类型不安全操作）。

**反例**：
```java
// ❌ 裸用泛型 + 未使用导入 + 已废弃 API
import java.util.Date;          // 未使用
import java.util.List;
import java.util.ArrayList;

public class Repository {
    @SuppressWarnings("all")    // ❌ 一刀切压制所有警告，掩盖真实问题
    public List findAll() {     // 裸用 List，丢失泛型约束
        return new ArrayList(); // 裸用 ArrayList
    }
}
```

**正例**：
```java
// ✅ 清理未使用导入，明确泛型，针对性压制警告
import java.util.List;
import java.util.ArrayList;

public class Repository {
    public List<User> findAll() {
        return new ArrayList<>();
    }
}
```
