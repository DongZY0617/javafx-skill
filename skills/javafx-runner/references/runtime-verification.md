# 运行验证规则

本文档是"运行验证"维度的判定依据，管辖 10 个检查项。通过执行 `mvn javafx:run`（或 `gradle run`）启动 JavaFX 应用，捕获启动过程和运行时异常，将"可编译"的代码推进到"真正可运行"。此维度违规默认为 Critical。与 `javafx-code-reviewer` 的线程安全维度互补——reviewer 静态审核线程安全结论，runner 动态验证运行时是否实际抛出异常。

> **核心原则**：运行验证是动态验证链的核心环节。编译通过不等于运行正常——模块 `opens` 缺失、`fx:controller` 反射失败、CSS `var()` 误用等问题只在运行时暴露。运行验证通过实际启动应用捕获这些"编译通过但运行失败"的隐患。所有运行时异常（`Exception` / `Error` 堆栈）默认 Critical，启动超时与退出码异常按场景评定。

---

## 检查项 1：应用启动

**关注点**：`Application.launch()` 能否正常启动，`start()` 方法能否执行完毕，主窗口能否显示。

**通过判定标准**：
- 执行 `mvn javafx:run` 后 JavaFX 运行时成功初始化，无 `java.lang.reflect.InvocationTargetException`
- `start(Stage stage)` 方法执行完毕，未抛出未捕获异常
- 主窗口（`Stage`）调用 `stage.show()` 成功显示
- 启动过程无 `Exception in thread "JavaFX Application Thread"` 输出
- 应用在超时阈值（默认 30 秒）内完成启动

**不通过判定标准**（任一即不通过）：
- 运行输出含 `Exception in Application start method`，`start()` 抛出未捕获异常
- 启动阶段抛出 `java.lang.NullPointerException`（如 `stage` 为 null、组件未初始化）
- 抛出 `IllegalStateException: Location is not set.`（FXML 路径错误导致 FXMLLoader 无内容）
- 应用启动后立即崩溃，退出码非 0
- `Application.launch()` 抛出 `IllegalStateException: Application launch must not be called more than once`

**严重性基线**：Critical（不可降级，应用无法启动则无法交付）

**反例**：
```java
// ❌ start() 中直接访问未初始化组件，抛 NullPointerException
public class App extends Application {
    private Label statusLabel;  // 未初始化

    @Override
    public void start(Stage stage) throws Exception {
        statusLabel.setText("启动中");  // NullPointerException
        stage.show();
    }
}
```
运行时输出：
```
Exception in Application start method
java.lang.reflect.InvocationTargetException
    at ... App.start(App.java:9)
Caused by: java.lang.NullPointerException
    at com.example.App.start(App.java:9)
```

**正例**：
```java
// ✅ 组件正确初始化后再使用
public class App extends Application {
    private Label statusLabel = new Label();  // 初始化

    @Override
    public void start(Stage stage) throws Exception {
        statusLabel.setText("启动中");
        stage.setScene(new Scene(statusLabel, 300, 200));
        stage.show();
    }
}
```

---

## 检查项 2：FXML 加载

**关注点**：所有 `FXMLLoader.load()` 调用能否成功解析 FXML 文件，`fx:controller` 能否实例化，`fx:id` 注入能否完成。

**通过判定标准**：
- 所有 `FXMLLoader.load()` 调用返回非 null 的 `Parent`，无 `LoadException`
- FXML 中 `fx:controller` 指向的类被成功实例化（无 `ClassNotFoundException` / `IllegalAccessException`）
- FXML 中所有 `fx:id` 在 Controller 中有对应 `@FXML` 字段并完成注入
- `onAction="#method"` 引用的方法在 Controller 中存在且签名正确（`@FXML private void method(ActionEvent event)` 或无参）
- 控制器包已 `opens ... to javafx.fxml`（模块化项目），反射访问正常

**不通过判定标准**（任一即不通过）：
- 运行输出含 `javafx.fxml.LoadException`，FXML 解析失败
- `fx:controller` 类无法实例化，抛出 `IllegalAccessException`（模块 `opens` 缺失）
- `fx:id` 与 Controller 字段不匹配，抛出 `LoadException: ... couldn't be set on ...`
- `onAction` 引用方法不存在，抛出 `LoadException: Controller method not found`
- FXML 文件路径错误，`FXMLLoader.load()` 抛出 `NullPointerException`（`getResource` 返回 null）

**严重性基线**：Critical（不可降级，运行时必然抛 `LoadException`）

> **关键事实**：`opens` 缺失是 FXML 加载失败的最常见根因。`module-info.java` 缺少 `opens com.example.controller to javafx.fxml` 时，`FXMLLoader` 无法反射实例化控制器，抛出 `IllegalAccessException`，最终包装为 `LoadException`。

**反例**：
```java
// ❌ FXML 路径错误 + fx:id 不匹配，运行时抛 LoadException
@FXML
private void loadView() throws IOException {
    // 路径大小写错误，Linux 下 getResource 返回 null
    Parent root = FXMLLoader.load(getClass().getResource("/fxml/UserView.fxml"));
    // 实际文件为 user-view.fxml
}
```
运行时输出：
```
Exception in Application start method
java.lang.reflect.InvocationTargetException
Caused by: javafx.fxml.LoadException:
    at javafx.fxml.FXMLLoader.loadImpl(FXMLLoader.java:3240)
Caused by: java.lang.NullPointerException: Location is required.
```

**正例**：
```java
// ✅ 路径正确，且 Controller 包已 opens，fx:id 与字段一一对应
@FXML
private void loadView() throws IOException {
    Parent root = FXMLLoader.load(getClass().getResource("/fxml/user-view.fxml"));
}
```

---

## 检查项 3：CSS 解析

**关注点**：所有 CSS 样式表能否被 JavaFX CSS 解析器无错误加载，无 `var()` 不支持语法、无未定义查找色、无非法属性。

**通过判定标准**：
- 运行输出无 `CSS Error` / `WARNING: Could not resolve` 相关日志
- CSS 文件中未使用 Web CSS 独有的 `var()` 函数（JavaFX CSS 不支持）
- 所有查找色（`-fx-primary-color`）在使用前已在 `.root` 中定义
- 所有属性名符合 JavaFX CSS 规范（`-fx-` 前缀）
- 圆角等数值使用字面量而非 `var()` 引用

**不通过判定标准**（任一即不通过）：
- 运行输出含 `CSS Error parsing ...` 或 `WARNING: Could not resolve '-fx-xxx-color'`
- CSS 中使用 `var()` 函数（JavaFX CSS 不支持，解析为字面量导致样式失效）
- 引用了未定义的查找色（`-fx-undefined-color`），JavaFX 静默回退到默认值并输出 WARNING
- 使用了 Web CSS 属性（如 `background-color` 而非 `-fx-background-color`）
- CSS 选择器语法错误导致整条规则被跳过

**严重性基线**：Major
- 降级条件：仅 WARNING 不影响渲染（如未定义查找色回退到默认值，界面仍正常显示）→ Minor
- 升级条件：CSS 解析错误导致界面无法显示或布局错乱 → Critical

**反例**：
```css
/* ❌ 使用 JavaFX CSS 不支持的 var() 语法 */
.root {
    -fx-primary-color: #2196f3;
}
.button-primary {
    -fx-background-color: var(-fx-primary-color);  /* var() 不被解析 */
    -fx-background-radius: var(-fx-radius);          /* 未定义且不支持 var() */
}
```
运行时输出：
```
WARNING: Could not resolve '-fx-primary-color' while resolving lookups for '-fx-background-color' from rule '*.button-primary'
```

**正例**：
```css
/* ✅ 直接引用查找色，圆角用字面量 */
.root {
    -fx-primary-color: #2196f3;
}
.button-primary {
    -fx-background-color: -fx-primary-color;
    -fx-background-radius: 8;
}
```

---

## 检查项 4：资源加载

**关注点**：图片、图标、国际化资源包等能否被正确加载，路径无 `NullPointerException`，编码无乱码。

**通过判定标准**：
- 所有 `new Image(String url)` / `new Image(InputStream)` 加载成功，无 `IllegalArgumentException: Invalid URL or resource not found`
- 图标资源（`stage.getIcons().add(new Image(...))`）路径正确
- 国际化资源包 `ResourceBundle.getBundle("i18n.messages")` 加载成功，无 `MissingResourceException`
- 资源路径大小写在所有平台一致（Linux 区分大小写）
- 编译产物 `target/classes` 中包含所有资源文件（FXML / CSS / 图片 / properties）

**不通过判定标准**（任一即不通过）：
- 运行输出含 `IllegalArgumentException: Invalid URL or resource not found`
- `getResource()` 返回 null 后直接调用方法，抛出 `NullPointerException`
- 国际化资源包缺失，抛出 `MissingResourceException: Can't find bundle for base name i18n.messages`
- 图片路径大小写错误，Windows 下正常但 Linux 下加载失败
- 资源文件未被 Maven 复制到 `target/classes`（`<resources>` 配置错误）

**严重性基线**：Major
- 降级条件：资源缺失但有默认回退逻辑，不影响核心功能 → Minor
- 升级条件：核心资源缺失导致应用无法启动（如主 FXML 加载失败）→ Critical

**反例**：
```java
// ❌ 图片路径错误，getResource 返回 null 后直接用，抛 NullPointerException
@Override
public void start(Stage stage) throws Exception {
    // 路径 /images/logo.PNG 与实际 logo.png 大小写不一致，Linux 下失败
    Image icon = new Image(getClass().getResource("/images/logo.PNG").toExternalForm());
    stage.getIcons().add(icon);
}
```

**正例**：
```java
// ✅ 路径大小写一致，且校验非 null
@Override
public void start(Stage stage) throws Exception {
    URL iconUrl = getClass().getResource("/images/logo.png");
    if (iconUrl != null) {
        stage.getIcons().add(new Image(iconUrl.toExternalForm()));
    }
}
```

---

## 检查项 5：模块运行时

**关注点**：`module-info.java` 在运行时是否满足所有反射需求（`PropertyValueFactory`、FXML 控制器注入、`FXMLLoader` 反射访问）。

**通过判定标准**：
- `module-info.java` 中 `opens` 声明覆盖所有需要反射访问的包，运行时无 `IllegalAccessException`
- `PropertyValueFactory` 能成功反射读取模型属性（模型包已 `opens ... to javafx.controls`）
- `FXMLLoader` 能反射实例化控制器（控制器包已 `opens ... to javafx.fxml`）
- 运行时无 `class com.example.X (in module com.example.app) cannot access class ... because module ... does not open ...` 错误
- `exports` 与 `opens` 配合正确，跨模块访问正常

**不通过判定标准**（任一即不通过）：
- 运行输出含 `java.lang.IllegalAccessException`，模块未 `opens` 对应包
- `PropertyValueFactory` 反射失败，运行时输出 `IllegalStateException: Cannot set property ...` 或表格列为空
- `FXMLLoader` 无法反射实例化控制器，抛出 `IllegalAccessException` 包装的 `LoadException`
- 运行输出含 `module ... does not open ... to javafx.controls` / `to javafx.fxml`

**严重性基线**：Critical
- 降级条件：缺失的 `opens` 不影响当前实际功能（如未使用 `PropertyValueFactory`）→ Major
- 升级条件：—（保持 Critical，反射失败导致功能不可用）

> **关键事实**：模块 `opens` 缺失是"编译通过但运行失败"的典型代表。`module-info.java` 缺少 `opens com.example.model to javafx.controls` 时，编译通过，但运行时 `PropertyValueFactory` 反射读取 `User.name` 属性失败，表格列显示为空。runner 通过实际运行捕获此问题，弥补 reviewer 静态审核的盲区。

**反例**：
```java
// ❌ module-info.java 缺少 opens com.example.model to javafx.controls
module com.example.app {
    requires javafx.controls;
    requires javafx.fxml;
    opens com.example.controller to javafx.fxml;
    // 缺少 opens com.example.model to javafx.controls;
}
```
```java
// PropertyValueFactory 反射 User.name 失败
TableColumn<User, String> nameCol = new TableColumn<>("姓名");
nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
// 运行时：表格"姓名"列为空，控制台输出反射访问被拒
```

**正例**：
```java
// ✅ 完整 opens 声明，PropertyValueFactory 反射正常
module com.example.app {
    requires javafx.controls;
    requires javafx.fxml;
    opens com.example.controller to javafx.fxml;
    opens com.example.model to javafx.controls;  // 供 PropertyValueFactory 反射
}
```

---

## 检查项 6：线程安全运行时验证

**关注点**：是否存在运行时抛出的 `IllegalStateException: Not on FX application thread`，验证 reviewer 静态审核的线程安全结论。

**通过判定标准**：
- 运行输出无 `IllegalStateException: Not on FX application thread`
- 后台线程更新 UI 均通过 `Platform.runLater()` 或 `Task` 回调切换到 FX 线程
- `ObservableList` 的修改均在 FX 线程执行
- 无 `ConcurrentModificationException`（跨线程修改 ObservableList）

**不通过判定标准**（任一即不通过）：
- 运行输出含 `IllegalStateException: Not on FX application thread; currentThread = ...`
- 后台线程直接调用 `setText` / `setItems` / `setVisible` 等 UI 组件方法
- 后台线程直接修改 `ObservableList`，触发 `ConcurrentModificationException`
- `Task.call()` 中直接更新 UI 而未通过 `updateMessage` / `updateProgress` 或 `Platform.runLater`

**严重性基线**：Critical（不可降级，运行时必然抛 `IllegalStateException`）

> **关键事实**：本检查项是 runner 与 reviewer 协作的关键节点。reviewer 静态审核可发现"后台线程直接更新 UI"的代码模式，但某些动态场景（如 `Task` 回调时序、`ScheduledService` 周期触发）只有在运行时才暴露。runner 通过实际运行捕获这些动态线程违规，与 reviewer 的静态结论交叉验证。

**反例**：
```java
// ❌ 后台线程直接更新 UI，运行时抛 IllegalStateException
new Thread(() -> {
    try {
        Thread.sleep(2000);
    } catch (InterruptedException e) { }
    statusLabel.setText("完成");  // 非 FX 线程更新 UI
}).start();
```
运行时输出：
```
Exception in thread "Thread-0" java.lang.IllegalStateException:
    Not on FX application thread; currentThread = Thread-0
    at javafx.graphics/javafx.application.Platform.runLater(...)
    at com.example.App.lambda$start$0(App.java:15)
```

**正例**：
```java
// ✅ 通过 Platform.runLater 切回 FX 线程
new Thread(() -> {
    try {
        Thread.sleep(2000);
    } catch (InterruptedException e) { }
    Platform.runLater(() -> statusLabel.setText("完成"));
}).start();
```

---

## 检查项 7：JavaFX 24+ 原生访问

**关注点**：JavaFX 24+ 项目是否配置了 `--enable-native-access=javafx.graphics`，缺失时启动是否报 `IllegalAccessError` 或相关原生访问警告。

**通过判定标准**：
- JavaFX 24+ 项目的运行命令或 `pom.xml` 中包含 `--enable-native-access=javafx.graphics`（或 `=ALL-UNNAMED`）
- 运行输出无 `IllegalAccessError: class javafx.graphics ... cannot access ...` 错误
- 运行输出无 `WARNING: A restricted method in javafx.graphics ... has been called` 警告（或已显式启用）
- 打包配置（`jpackage`）中包含 `--java-options "--enable-native-access=javafx.graphics"`

**不通过判定标准**（任一即不通过）：
- JavaFX 24+ 项目运行时输出 `IllegalAccessError` 或 `IllegalAccessWarning`，原生访问未启用
- 运行输出含 `A restricted method in javafx.graphics has been called` 且未配置 `--enable-native-access`
- `pom.xml` 的 `javafx-maven-plugin` 配置中缺少 `<options>` 中的 `--enable-native-access`
- 仅配置了 `--enable-native-access=ALL-UNNAMED` 但项目为模块化（应指定具体模块 `javafx.graphics`）

**严重性基线**：Critical（不可降级，JavaFX 24+ 运行时必然报 `IllegalAccessError`）

> **版本感知**：JavaFX 24 起严格限制原生访问，`javafx.graphics` 中调用原生方法（渲染、窗口管理）需要显式启用 `--enable-native-access`。JavaFX 17/21 不受此约束。runner 根据检测到的 JavaFX 版本动态决定是否执行此检查项。

**反例**：
```xml
<!-- ❌ JavaFX 24+ 项目，pom.xml 未配置 --enable-native-access -->
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>0.0.8</version>
    <configuration>
        <mainClass>com.example.App</mainClass>
        <!-- 缺少 <options><option>--enable-native-access=javafx.graphics</option></options> -->
    </configuration>
</plugin>
```
运行时输出：
```
WARNING: A restricted method in javafx.graphics (com.sun.glass.ui.Window) has been called
java.lang.IllegalAccessError: ...
```

**正例**：
```xml
<!-- ✅ JavaFX 24+ 配置 --enable-native-access -->
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>0.0.8</version>
    <configuration>
        <mainClass>com.example.App</mainClass>
        <options>
            <option>--enable-native-access=javafx.graphics</option>
        </options>
    </configuration>
</plugin>
```

---

## 检查项 8：Headless 模式验证

**关注点**：CI 环境（无显示器）下能否通过 Monocle 测试框架启动 JavaFX 应用，headless 运行命令是否正确。

**通过判定标准**：
- 无显示器环境下通过 `mvn javafx:run -Dmonocle.platform=Headless -Dprism.order=sw` 成功启动
- Monocle 依赖已正确声明（Maven 的 `testtestfx-monocle` 或 `org.openjfx:javafx-monocle`）
- 运行输出无 `java.awt.HeadlessException`
- 运行输出无 `Prism - pipeline: errors initializing native pipeline`
- headless 模式下 FXML 加载、CSS 解析等逻辑验证均正常

**不通过判定标准**（任一即不通过）：
- CI 环境运行报 `java.awt.HeadlessException`，未配置 Monocle
- 运行报 `java.lang.UnsatisfiedLinkError: ... glass.dll / libglass.so`，缺少显示器且未用 Monocle
- Monocle 依赖缺失，运行报 `ClassNotFoundException: com.sun.glass.ui.monocle.MonoclePlatform`
- `-Dprism.order=sw` 未设置，尝试初始化硬件渲染管线失败
- Monocle 版本与 JavaFX 版本不匹配，运行报 `NoClassDefFoundError`

**严重性基线**：Major
- 降级条件：本地有显示器环境正常，仅 CI headless 失败且非交付必需 → Minor
- 升级条件：CI 是唯一验证环境且 headless 失败导致无法验证 → Critical

> **关键事实**：Monocle 是 JavaFX 官方的 headless 实现替代 Glass 窗口工具包，使 JavaFX 可在无显示器环境（CI、Docker）运行。Monocle 版本必须与 JavaFX 版本匹配，否则会因内部 API 变更报 `NoClassDefFoundError`。配置详见 `environment-setup.md`。

**反例**：
```bash
# ❌ CI 环境直接运行，未配置 Monocle，报 HeadlessException
mvn javafx:run
# 运行输出：
# java.awt.HeadlessException
#     at javafx.graphics/com.sun.glass.ui.Screen.validateScreenSettings(...)
```

**正例**：
```bash
# ✅ CI 环境配置 Monocle headless 运行
mvn javafx:run -Dmonocle.platform=Headless -Dprism.order=sw
# 运行正常，JavaFX 在无显示器环境启动
```

---

## 检查项 9：启动超时检测

**关注点**：应用是否在合理时间内完成启动（默认 30 秒超时），`start()` 中是否存在阻塞调用导致启动卡死。

**通过判定标准**：
- 应用在超时阈值（默认 30 秒）内完成启动，主窗口显示
- `start()` 方法中无阻塞调用（`Thread.sleep`、同步 I/O、网络请求、`Future.get()` 无超时）
- 超时后进程被终止，运行输出记录"启动超时，进程已终止"
- 二次启动（若启用）在合理时间内完成，排除冷启动因素

**不通过判定标准**（任一即不通过）：
- 应用启动超过 30 秒未完成，被超时机制终止
- `start()` 中调用 `Thread.sleep()` 阻塞 FX 线程
- `start()` 中执行同步网络请求或数据库查询，阻塞启动
- `start()` 中调用 `Future.get()` 无超时参数，可能永久阻塞
- 启动过程死锁（如 FX 线程等待后台线程锁，而后台线程等待 FX 线程）

**严重性基线**：Major
- 降级条件：超时因首次加载 JavaFX 模块（冷启动），二次启动正常 → Minor
- 升级条件：超时因 `start()` 中阻塞调用（如 `Thread.sleep`、同步 I/O）→ Critical

**反例**：
```java
// ❌ start() 中执行同步网络请求 + Thread.sleep，启动超时
@Override
public void start(Stage stage) throws Exception {
    // 阻塞 FX 线程，启动卡死
    Thread.sleep(30000);  // 休眠 30 秒
    String data = fetchDataFromNetwork();  // 同步网络请求
    Label label = new Label(data);
    stage.setScene(new Scene(label));
    stage.show();
}
```
运行时输出：
```
[INFO] 启动超时（30s），进程已终止
```

**正例**：
```java
// ✅ 阻塞操作移至后台 Task，start() 快速完成
@Override
public void start(Stage stage) throws Exception {
    Label label = new Label("加载中...");
    stage.setScene(new Scene(label, 300, 200));
    stage.show();

    Task<String> loadTask = new Task<>() {
        @Override
        protected String call() throws Exception {
            Thread.sleep(3000);
            return fetchDataFromNetwork();
        }
    };
    loadTask.setOnSucceeded(e -> label.setText(loadTask.getValue()));
    new Thread(loadTask).start();
}
```

---

## 检查项 10：退出码检查

**关注点**：应用正常退出时退出码为 0，非零退出码表示运行时错误，需结合输出诊断根因。

**通过判定标准**：
- 应用正常退出（用户关闭窗口或调用 `Platform.exit()`），退出码为 0
- 退出前无未捕获异常，无 `System.exit(non-zero)` 调用
- 退出码为 0 且运行输出无 `Exception` / `Error` 堆栈
- `setOnCloseRequest` 中资源清理正常完成，无清理过程抛出异常

**不通过判定标准**（任一即不通过）：
- 进程退出码非 0，表示运行时错误
- 运行输出含 `Exception in thread "JavaFX Application Thread"` 导致非正常退出
- 退出前 `setOnCloseRequest` 中清理逻辑抛出异常，进程异常退出
- 代码中显式调用 `System.exit(1)` 等非零退出（非预期）
- JVM 崩溃（`SIGSEGV` / `EXCEPTION_ACCESS_VIOLATION`），退出码为 134 / 0xC0000005 等

**严重性基线**：Critical
- 降级条件：退出码非 0 但应用已完成核心功能演示（如演示后 `System.exit(1)` 用于特定信号）→ Major
- 升级条件：JVM 崩溃（`SIGSEGV`）通常与原生访问或 native 库相关 → 保持 Critical 并关联检查项 7

> **关键事实**：退出码是运行验证的最终判定信号。退出码 0 + 无异常堆栈 = 运行验证通过。非零退出码需回溯输出中的异常堆栈定位根因，并与前述检查项（启动、FXML、线程安全等）交叉关联。同一根因引发的异常和退出码合并为一个问题。

**反例**：
```java
// ❌ 未捕获异常导致非零退出码
@Override
public void start(Stage stage) throws Exception {
    Button btn = new Button("点击");
    btn.setOnAction(e -> {
        // 未捕获异常，FX 线程默认处理打印堆栈但不退出
        // 若异常发生在启动阶段则直接崩溃
        throw new RuntimeException("未处理异常");
    });
    stage.setScene(new Scene(btn, 300, 200));
    stage.show();
}
```
```
进程退出码：1
Exception in thread "JavaFX Application Thread" java.lang.RuntimeException: 未处理异常
```

**正例**：
```java
// ✅ 异常被捕获处理，正常退出码 0
@Override
public void start(Stage stage) throws Exception {
    Button btn = new Button("点击");
    btn.setOnAction(e -> {
        try {
            doRiskyOperation();
        } catch (Exception ex) {
            showError("操作失败: " + ex.getMessage());
        }
    });
    stage.setScene(new Scene(btn, 300, 200));
    stage.show();
}
```
```
进程退出码：0
```
