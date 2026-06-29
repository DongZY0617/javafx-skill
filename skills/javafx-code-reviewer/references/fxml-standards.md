# FXML 使用规范

本文档是"FXML 使用规范"维度的判定依据，管辖 8 个检查项（对应设计书 3.3 节）。审查 FXML 文件与 Controller 的映射关系、资源加载方式及标记使用是否规范。默认严重性基线：Major。与 `javafx-developer` 的质量清单·fx:id 条目同源。

---

## 检查项 1：fx:id 匹配

**关注点**：FXML 中每个 `fx:id` 是否在 Controller 中有对应的 `@FXML` 字段，反之亦然。

**通过判定标准**：
- FXML 中每个 `fx:id="xxx"` 在 Controller 中都有对应的 `@FXML private Type xxx;` 字段
- Controller 中每个 `@FXML` 字段都在 FXML 中有对应的 `fx:id`
- 字段类型与 FXML 中对应控件类型一致（如 `fx:id="nameField"` 对应 `@FXML private TextField nameField;`）

**不通过判定标准**（任一即不通过）：
- FXML 中存在 `fx:id` 在 Controller 中无对应 `@FXML` 字段（运行时抛 `LoadException`）
- Controller 中存在 `@FXML` 字段在 FXML 中无对应 `fx:id`（字段为 null）
- 字段类型与控件类型不匹配

**严重性基线**：Major（不可降级，运行时必然抛 `LoadException`）

**反例**：
```xml
<!-- FXML: fx:id="saveBtn" -->
<Button fx:id="saveBtn" text="保存"/>
```
```java
// ❌ Controller 中字段名不匹配
@FXML private Button saveButton;  // 应为 saveBtn
```

**正例**：
```xml
<Button fx:id="saveBtn" text="保存"/>
```
```java
// ✅ fx:id 与 @FXML 字段一一对应
@FXML private Button saveBtn;
```

---

## 检查项 2：控制器映射

**关注点**：`fx:controller` 路径是否正确指向 Controller 全限定类名。

**通过判定标准**：
- `fx:controller` 使用 Controller 的全限定类名（如 `com.example.app.controller.UserController`）
- 类名与实际 Controller 类完全一致（含大小写）
- Controller 类存在且可被 FXMLLoader 通过反射实例化（或通过 controllerFactory 创建）

**不通过判定标准**（任一即不通过）：
- `fx:controller` 路径错误（包名或类名拼写错误）
- 使用简单类名而非全限定类名（除非 controllerFactory 已配置）
- Controller 类不存在

**严重性基线**：Major

**反例**：
```xml
<!-- ❌ 使用简单类名，FXMLLoader 无法定位 -->
<VBox fx:controller="UserController">
```

**正例**：
```xml
<!-- ✅ 使用全限定类名 -->
<VBox fx:controller="com.example.app.controller.UserController">
```

---

## 检查项 3：脚本禁止

**关注点**：FXML 中是否使用 `<fx:script>`（应禁止，逻辑须在 Controller 中）。

**通过判定标准**：
- FXML 文件中不包含任何 `<fx:script>` 标签
- 所有业务逻辑和事件处理都在 Controller 或 ViewModel 中实现
- FXML 保持纯声明式，仅描述 UI 结构和绑定

**不通过判定标准**（任一即不通过）：
- FXML 中使用 `<fx:script>` 嵌入 JavaScript 或其他脚本逻辑
- FXML 中通过内联表达式处理业务逻辑

**严重性基线**：Major

**反例**：
```xml
<!-- ❌ FXML 中嵌入脚本逻辑 -->
<Button text="计算" onAction="#calculate">
    <fx:script>
        var result = parseInt(a) + parseInt(b);
        label.setText(result);
    </fx:script>
</Button>
```

**正例**：
```xml
<!-- ✅ FXML 纯声明式，逻辑在 Controller 中 -->
<Button text="计算" onAction="#handleCalculate"/>
```
```java
@FXML
private void handleCalculate() {
    int result = parseInt(aField.getText()) + parseInt(bField.getText());
    resultLabel.setText(String.valueOf(result));
}
```

---

## 检查项 4：事件处理器

**关注点**：`onAction="#method"` 引用的方法是否在 Controller 中存在且签名正确。

**通过判定标准**：
- `onAction="#method"` / `onMouseClicked="#method"` 等引用的方法在 Controller 中存在
- 方法签名为 `void method(ActionEvent event)` 或 `void method()`（无参）
- 方法使用 `@FXML` 注解（或为 public）
- 方法名与 FXML 中引用的名称一致（含大小写）

**不通过判定标准**（任一即不通过）：
- FXML 中引用的事件处理器方法在 Controller 中不存在（运行时抛 `LoadException`）
- 方法签名不匹配（如期望 `void(ActionEvent)` 但实际为 `String method(ActionEvent)`）
- 方法为 private 且未标注 `@FXML`（FXMLLoader 无法访问）

**严重性基线**：Major

**反例**：
```xml
<!-- FXML 引用 handleSave -->
<Button text="保存" onAction="#handleSave"/>
```
```java
// ❌ Controller 中方法名不匹配（应为 handleSave）
@FXML
private void saveData(ActionEvent event) { /* ... */ }
```

**正例**：
```xml
<Button text="保存" onAction="#handleSave"/>
```
```java
// ✅ 方法名匹配，签名正确
@FXML
private void handleSave(ActionEvent event) { /* ... */ }

// ✅ 或无参版本
@FXML
private void handleSave() { /* ... */ }
```

---

## 检查项 5：资源路径

**关注点**：`FXMLLoader` 加载是否使用 `getClass().getResource()`，而非文件系统绝对路径。

**通过判定标准**：
- FXML / CSS 加载使用 `getClass().getResource("/fxml/xxx.fxml")` 或 `getClass().getResourceAsStream()`
- 资源文件放置在 `src/main/resources` 下，通过 classpath 加载
- 路径以 `/` 开头表示从 classpath 根开始
- 国际化资源使用 `ResourceBundle.getBundle("i18n.messages")` 加载

**不通过判定标准**（任一即不通过）：
- 使用文件系统绝对路径（如 `new File("C:/app/fxml/main.fxml")`）加载 FXML
- 使用相对路径 `new File("fxml/main.fxml")` 加载（打包后失效）
- 使用 `FileInputStream` 加载 classpath 资源
- 资源路径不以 `/` 开头，导致从当前包路径解析失败

**严重性基线**：Major

**反例**：
```java
// ❌ 使用文件系统路径，打包后失效
FXMLLoader loader = new FXMLLoader(new File("fxml/main.fxml").toURI().toURL());
// ❌ 使用 FileInputStream
FXMLLoader loader = new FXMLLoader(new FileInputStream("fxml/main.fxml"));
```

**正例**：
```java
// ✅ 使用 getClass().getResource 从 classpath 加载
FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
Parent root = loader.load();
```

---

## 检查项 6：styleClass 一致性

**关注点**：FXML 中引用的 `styleClass` 是否在对应 CSS 中有定义。

**通过判定标准**：
- FXML 中 `styleClass="xxx"` 或 `styleClass="a b c"` 引用的每个样式类在对应 CSS 文件中有定义
- CSS 样式表已通过 `scene.getStylesheets().add()` 正确加载
- 样式类名在 FXML 与 CSS 之间一致（含大小写）

**不通过判定标准**（任一即不通过）：
- FXML 中引用的 `styleClass` 在 CSS 中无定义（样式不生效）
- CSS 样式表未加载到 Scene，导致所有 styleClass 失效
- 样式类名拼写不一致（如 FXML 用 `button-primary`，CSS 定义 `.button_Primary`）

**严重性基线**：Major
- 降级条件：仅个别样式类未定义，不影响核心功能 → Minor

**反例**：
```xml
<!-- FXML 引用 button-primary 和 card-shadow -->
<Button styleClass="button-primary"/>
<VBox styleClass="card-shadow"/>
```
```css
/* ❌ CSS 只定义了 button-primary，缺少 card-shadow */
.button-primary { -fx-background-color: #2196f3; }
/* .card-shadow 未定义 */
```

**正例**：
```xml
<Button styleClass="button-primary"/>
<VBox styleClass="card-shadow"/>
```
```css
/* ✅ CSS 中两个样式类都有定义 */
.button-primary { -fx-background-color: #2196f3; }
.card-shadow { -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2); }
```

---

## 检查项 7：controllerFactory

**关注点**：Spring Boot 场景下是否设置 `loader.setControllerFactory(springContext::getBean)`。

**通过判定标准**：
- Spring Boot 集成项目中，FXMLLoader 设置了 `setControllerFactory(springContext::getBean)`
- Controller 标注 `@Component` 且 `@Scope("prototype")`，由 Spring 容器管理
- 通过 controllerFactory 注入的 Controller 可使用 `@Autowired` 依赖注入

**不通过判定标准**（任一即不通过）：
- Spring Boot 项目中未设置 `controllerFactory`，Controller 由 FXMLLoader 默认 `new` 创建，无法注入依赖
- `controllerFactory` 设置错误（如返回错误的 Bean 类型）
- Controller 标注 `@Component` 但未设置 `controllerFactory`，导致 Spring 注入的依赖为 null

**严重性基线**：Major

**反例**：
```java
// ❌ Spring Boot 项目未设置 controllerFactory，Controller 依赖无法注入
@FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
// 缺少 loader.setControllerFactory(...)
Parent root = loader.load();
// UserController 中的 @Autowired userService 为 null
```

**正例**：
```java
// ✅ 设置 controllerFactory，Controller 由 Spring 容器创建
FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
loader.setControllerFactory(springContext::getBean);
Parent root = loader.load();
// UserController 中的 userService 已注入
```

---

## 检查项 8：根元素命名空间

**关注点**：是否声明 `xmlns:fx="http://javafx.com/fxml"`，FXML 版本是否匹配 JavaFX 版本。

**通过判定标准**：
- 根元素声明了 `xmlns:fx="http://javafx.com/fxml"`（或 `xmlns:fx="http://javafx.com/fxml/1"`）
- 如使用 JavaFX 特定版本特性，声明了对应的 `xmlns="http://javafx.com/javafx/XX"`（XX 为版本号）
- FXML 版本与项目使用的 JavaFX 版本兼容

**不通过判定标准**（任一即不通过）：
- 根元素未声明 `xmlns:fx` 命名空间，导致 `fx:id`、`fx:controller` 等标记无法解析
- FXML 声明的版本远高于项目实际 JavaFX 版本，使用了不支持的特性
- 缺少 XML 声明 `<?xml version="1.0" encoding="UTF-8"?>`

**严重性基线**：Major
- 降级条件：仅缺少版本号声明但功能正常 → Minor

**反例**：
```xml
<!-- ❌ 未声明 xmlns:fx 命名空间 -->
<VBox>
    <Button fx:id="btn" text="按钮"/>  <!-- fx:id 无法解析 -->
</VBox>
```

**正例**：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- ✅ 声明 xmlns 和 xmlns:fx -->
<VBox xmlns="http://javafx.com/javafx/21"
      xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.example.app.controller.MainController">
    <Button fx:id="btn" text="按钮"/>
</VBox>
```
