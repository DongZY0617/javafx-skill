# 编码 / 命名 / Spring Boot / 版本 / API 合规

本文档是"深度合规审核"维度的主参考文档之一（对应设计书 3.6 节），管辖命名规范、编码规范、Spring Boot 陷阱、版本兼容性、API 误用排查共 5 类检查项。默认严重性基线：Minor。安全规则参见 `security-checklist.md`，CSS 合规参见 `css-compliance.md`，Properties null 安全参见 `binding-compliance.md`。

---

## 检查项 1：命名规范

**关注点**：类名 PascalCase，方法 / 变量 camelCase，常量 SCREAMING_SNAKE_CASE。

**通过判定标准**：
- 类名、接口名、枚举名使用 PascalCase（如 `UserController`、`UserService`）
- 方法名、变量名、字段名使用 camelCase（如 `handleSave`、`userName`）
- 常量（`static final`）使用 SCREAMING_SNAKE_CASE（如 `MAX_RETRY_COUNT`、`DEFAULT_TIMEOUT`）
- 包名全小写，无下划线或特殊字符（如 `com.example.app.controller`）
- 泛型类型参数使用单个大写字母（如 `T`、`E`、`K`、`V`）

**不通过判定标准**（任一即不通过）：
- 类名使用 camelCase 或 snake_case（如 `userController`、`user_controller`）
- 方法名 / 变量名使用 PascalCase 或 SCREAMING_SNAKE_CASE
- 常量使用 camelCase（如 `maxRetryCount`）
- 包名含大写字母或下划线

**严重性基线**：Minor
- 升级条件：公共 API 命名违反规范且影响调用方 → Major

**反例**：
```java
// ❌ 命名不规范
public class userController {        // 类名应 PascalCase
    private int Max_count = 100;     // 常量应 SCREAMING_SNAKE_CASE
    private String UserName;         // 字段应 camelCase

    public void HandleSave() { }     // 方法应 camelCase
}
```

**正例**：
```java
// ✅ 命名规范
public class UserController {
    private static final int MAX_COUNT = 100;  // 常量 SCREAMING_SNAKE_CASE
    private String userName;                    // 字段 camelCase

    public void handleSave() { }                // 方法 camelCase
}
```

---

## 检查项 2：编码规范

**关注点**：UTF-8 编码、4 空格缩进、显式导入（不使用通配符）、公共 API 有 Javadoc。

**通过判定标准**：
- 所有源文件使用 UTF-8 编码
- 缩进使用 4 个空格，不使用 Tab
- 使用显式导入（如 `import javafx.scene.control.Button;`），不使用通配符（`import javafx.scene.control.*;`）
- 公共 API（public 方法、public 类）有 Javadoc 注释
- 复杂逻辑使用行内注释说明

**不通过判定标准**（任一即不通过）：
- 文件编码非 UTF-8（如 GBK、ISO-8859-1），导致中文乱码
- 使用 Tab 缩进或缩进不一致
- 使用通配符导入（`import ...*;`）
- 公共 API 缺少 Javadoc

**严重性基线**：Minor（通配符导入、缺 Javadoc 等不影响运行）

**反例**：
```java
// ❌ 通配符导入
import javafx.scene.control.*;
import javafx.collections.*;

// ❌ Tab 缩进 + 缺 Javadoc
public class UserService {
	public User findById(Long id) {  // Tab 缩进
		return repository.findById(id);  // 公共方法无 Javadoc
	}
}
```

**正例**：
```java
// ✅ 显式导入
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

// ✅ 4 空格缩进 + Javadoc
public class UserService {
    /**
     * 根据 ID 查找用户。
     * @param id 用户 ID，不能为 null
     * @return 用户对象，未找到返回 null
     */
    public User findById(Long id) {
        return repository.findById(id);
    }
}
```

---

## 检查项 3：Spring Boot 陷阱

**关注点**：主类是否未直接继承 `Application`、Controller 是否标注 `@Scope("prototype")`、是否配置 `web-application-type: none`。

**通过判定标准**：
- Spring Boot 启动类（含 `main` 方法的类）不直接继承 `Application`，拆分为启动类 + JavaFX 入口类
- Controller 标注 `@Component` 且 `@Scope("prototype")`，避免单例状态污染
- `application.yml` 中 `spring.main.web-application-type` 设为 `none`（JavaFX 应用不需要 Web 服务器）
- 未引入 `spring-boot-devtools`（或设为 optional + 禁用 restart）

**不通过判定标准**（任一即不通过）：
- 启动类直接 `extends Application`（导致 "JavaFX runtime components are missing" 错误）
- Controller 标注 `@Component` 但未标注 `@Scope("prototype")`，单例 Controller 持有 @FXML 状态字段
- 未配置 `web-application-type: none`，启动时尝试初始化 Web 服务器
- 引入 `spring-boot-devtools` 且未禁用 restart（与 JavaFX Application 冲突）

**严重性基线**：
- 启动类直接继承 Application：Critical（不可降级，导致 Spring 容器初始化异常）
- Controller 缺少 @Scope("prototype")：Major
  - 降级条件：单例 Controller 无状态字段 → Minor
  - 升级条件：单例 Controller 持有 @FXML 状态字段 → 保持 Major

**反例**：
```java
// ❌ 启动类直接继承 Application，导致运行时错误
@SpringBootApplication
public class MyApp extends Application {
    @Override
    public void start(Stage stage) { /* ... */ }
    public static void main(String[] args) { launch(args); }
}
```
```java
// ❌ Controller 单例但持有状态字段
@Component
// 缺少 @Scope("prototype")
public class UserController implements Initializable {
    @FXML private TextField nameField;  // 单例下状态污染
}
```

**正例**：
```java
// ✅ 启动类不继承 Application
@SpringBootApplication
public class MyApp {
    static ConfigurableApplicationContext springContext;
    public static void main(String[] args) {
        springContext = SpringApplication.run(MyApp.class, args);
        Application.launch(JavaFXApp.class, args);
    }
}
// JavaFX 入口类单独继承 Application
public class JavaFXApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        loader.setControllerFactory(MyApp.springContext::getBean);
        stage.setScene(new Scene(loader.load()));
        stage.show();
    }
}
```
```java
// ✅ Controller 标注 @Scope("prototype")
@Component
@Scope("prototype")
public class UserController implements Initializable {
    @FXML private TextField nameField;
}
```
```yaml
# ✅ application.yml 配置
spring:
  main:
    web-application-type: none
```

---

## 检查项 4：版本兼容性

**关注点**：JavaFX 24+ 是否配置 `--enable-native-access=javafx.graphics`，版本选择是否符合 LTS 路线。

**通过判定标准**：
- JavaFX 24+ 项目在 `module-info.java` 或 JVM 参数中配置 `--enable-native-access=javafx.graphics`
- 版本选择符合 LTS 路线：JavaFX 21 LTS（JDK 17+）或 JavaFX 25 LTS（JDK 23+）
- JavaFX 17 LTS 标注支持至 2026.10
- JavaFX 26 已标注为已发布（非"计划"/"预期"）
- JDK 版本与 JavaFX 版本兼容

**不通过判定标准**（任一即不通过）：
- JavaFX 24+ 项目未配置 `--enable-native-access=javafx.graphics`
- 推荐已过期的版本（如 JavaFX 17 LTS 但未提示 2026.10 结束支持）
- 将 JavaFX 25 标注为"计划"或"预期"（实际已发布 LTS）
- 将 JavaFX 26 标注为"计划"（实际已发布）
- JDK 版本与 JavaFX 版本不兼容

**严重性基线**：Major（版本配置错误导致运行时警告或功能缺失）

**反例**：
```java
// ❌ JavaFX 24+ 未配置 --enable-native-access
// 运行时将输出警告：WARNING: A restricted method in java.lang.foreign.Linker has been called
```
```
// ❌ 版本推荐错误
// "JavaFX 25 计划于 2025 年发布"  ← 实际已发布，不应标注"计划"
```

**正例**：
```xml
<!-- ✅ pom.xml 中配置 JVM 参数（JavaFX 24+）-->
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <configuration>
        <options>
            <option>--enable-native-access=javafx.graphics</option>
        </options>
    </configuration>
</plugin>
```
```
// ✅ 版本矩阵准确
// JavaFX 21 LTS — JDK 17+，成熟稳定，推荐
// JavaFX 25 LTS — JDK 23+，最新 LTS
// JavaFX 26 — 已发布，最新特性
// JavaFX 17 LTS — 支持至 2026.10
```

---

## 检查项 5：API 误用排查

**关注点**：是否使用了不存在的 API（如 `select()`、`@FXML dispose()`）、是否使用 ControlsFX 旧 `Dialogs.create()`。

**通过判定标准**：
- 不使用 `ObservableValue.select()`（该 API 不存在，正确用法是 `Bindings.select()` 或 `SelectBinding`）
- 不声称存在 `@FXML dispose()` 生命周期方法（JavaFX 无此自动回调，须自定义 `dispose()` 并手动调用）
- 不使用 ControlsFX 旧 `Dialogs.create()` API（已废弃，使用 JavaFX 原生 `Alert` / `Dialog`）
- 不使用 `Person.select(p -> ...)` 等不存在的流式 API
- 不使用已废弃的 API（如 `javafx.scene.web.HTMLEditor` 的过时方法）

**不通过判定标准**（任一即不通过）：
- 使用 `observableValue.select(func)` 不存在的 API
- 标注 `@FXML private void dispose()` 并期望框架自动调用
- 使用 ControlsFX `Dialogs.create().owner(stage).message("...").showInformation()` 旧 API
- 使用不存在的流式属性选择 API

**严重性基线**：Major（使用不存在的 API 导致编译错误或运行时异常）

**反例**：
```java
// ❌ 使用不存在的 select() API
StringBinding name = person.select(p -> p.getName());  // select() 不存在

// ❌ 声称 @FXML dispose() 是生命周期方法
@FXML
private void dispose() {  // @FXML dispose() 不存在，不会被自动调用
    model.removeListener(listener);
}

// ❌ 使用 ControlsFX 旧 Dialogs API
Dialogs.create()
    .owner(stage)
    .title("提示")
    .message("保存成功")
    .showInformation();
```

**正例**：
```java
// ✅ 使用 Bindings.select 或直接属性访问
StringBinding name = Bindings.selectString(person, "name");

// ✅ 自定义 dispose() 方法（不加 @FXML），手动调用
public void dispose() {  // 普通 public 方法
    model.removeListener(listener);
}
// 在视图切换回调中手动调用：oldController.dispose();

// ✅ 使用 JavaFX 原生 Alert
Alert alert = new Alert(Alert.AlertType.INFORMATION);
alert.setTitle("提示");
alert.setHeaderText(null);
alert.setContentText("保存成功");
alert.showAndWait();
```
