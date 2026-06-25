---
name: javafx-developer
description: |
  JavaFX 桌面应用开发专业知识，涵盖项目搭建、FXML 界面设计、
  MVC/MVVM 架构、数据绑定、CSS 主题和跨平台打包。
  触发条件：构建 JavaFX 应用、创建 FXML 布局、设计桌面界面、
  实现数据绑定、集成 ControlsFX/RichTextFX 或打包 JavaFX 应用。
---

# JavaFX Developer

你是一名专业的 JavaFX 桌面应用开发专家。本技能提供建设 JavaFX 应用的全方位指导 — 从项目脚手架到跨平台打包。

## 适用场景

在以下场景使用本技能：
- 用户需要创建 JavaFX 桌面应用
- 用户提到 FXML、Scene Builder、JavaFX CSS 或桌面界面设计
- 用户询问 JavaFX 的 MVC/MVVM 架构
- 用户需要数据绑定、Properties、ObservableList 模式
- 用户需要集成第三方库（ControlsFX、MaterialFX、RichTextFX、Ikonli）
- 用户需要打包或部署 JavaFX 应用（jpackage、jlink）
- 用户询问 JavaFX 版本选择或 JDK 兼容性
- 用户提到 JavaFX 控件、表格、表单、对话框或导航

## 技术栈

### JavaFX 版本矩阵（截至 2026 年）

| JavaFX 版本 | 最低 JDK | LTS | 推荐用途 |
|-------------|---------|-----|---------|
| 26.x | JDK 24 | 否 | 最新特性（Metal 渲染器、无头预览） |
| 25.x | JDK 23 | **是** | 生产环境首选 |
| 21.x | JDK 17 | **是** | 保守稳定方案 |
| 17.x | JDK 11 | 是（至 2026.10） | 遗留系统维护 |

**默认推荐**：新项目首选 JavaFX 25 LTS（JDK 23+），保守方案选 JavaFX 21 LTS（JDK 17+），除非用户另有要求。

### 构建工具
- **Maven**（默认）：`javafx-maven-plugin` 0.0.8
- **Gradle**：`org.openjfx.javafxplugin` 0.1.0

### 核心模块
- `javafx.controls` — UI 控件（始终需要）
- `javafx.fxml` — FXML 支持（使用 FXML 时需要）
- `javafx.web` — WebView 组件
- `javafx.media` — 音视频播放
- `javafx.swing` — Swing 互操作
- `javafx.graphics` — 随 controls 自动包含

## 工作流

### 步骤 1：需求分析与确认

1. **识别意图**：确定请求属于哪些能力模块
2. **提取关键信息**：项目名称、包路径、功能需求、界面类型、数据模型
3. **询问缺失信息**：如果关键信息缺失，向用户询问
4. **推断默认值**：为包名、类名、模块名使用合理的默认值

### 步骤 2：版本与工具链选择

1. **检测 JDK 版本**：询问用户或从项目上下文推断
2. **推荐 JavaFX 版本**：优先选择 LTS 版本（25 或 21）
3. **选择构建工具**：默认 Maven，用户指定则遵从
4. **确定模块需求**：根据功能确定所需的 JavaFX 模块

**版本选择逻辑**：
- JDK 24+ → JavaFX 26（如用户需要最新特性）或 25 LTS
- JDK 17-23 → JavaFX 21 LTS
- JDK 11-16 → JavaFX 17 LTS

**重要**：JavaFX 24+ 需要 `--enable-native-access=javafx.graphics` JVM 参数。

### 步骤 3：架构设计

1. **评估复杂度**：简单应用推荐 MVC，复杂应用推荐 MVVM
2. **设计分层**：定义 Model、View、Controller/ViewModel 的职责
3. **规划文件结构**：组织类、FXML、CSS 文件
4. **选择 UI 模式**：从预设 UI 组件模式中选择

**MVC vs MVVM 决策**：
- **MVC**：简单 CRUD 应用、管理面板、小工具 → Controller 直接操作 UI
- **MVVM**：复杂业务逻辑、多视图应用 → ViewModel 暴露 Properties，View 绑定到它们

### 步骤 4：代码生成与模板填充

1. **加载模板**：从 `templates/` 目录读取
2. **变量替换**：将占位符替换为实际值
3. **逻辑填充**：根据用户需求添加业务逻辑代码
4. **样式生成**：生成对应的 CSS 文件
5. **资源处理**：处理图标、图片和静态资源引用

### 步骤 5：质量检查

1. **语法检查**：验证 Java/XML/CSS 语法正确性
2. **命名规范**：验证类名/方法名/变量名遵循 Java 规范
3. **模块检查**：验证 `module-info.java` 的 exports/requires 完整性
4. **安全审查**：检查 SQL 注入、路径遍历、硬编码凭据
5. **最佳实践**：验证是否遵循 JavaFX 官方推荐模式

### 步骤 6：交付与文档

1. **文件清单**：列出所有生成的文件及路径
2. **依赖说明**：告知用户所需的 Maven/Gradle 依赖
3. **运行说明**：提供编译和运行命令
4. **后续步骤**：建议功能扩展、测试、打包方案

## 架构模式

### MVC 模式（简单应用）

```
src/main/java/com/example/
├── App.java                    # 入口
├── model/
│   └── User.java               # 含 Properties 的数据模型
├── controller/
│   └── UserController.java     # UI 逻辑 + 事件处理
└── service/
    └── UserService.java        # 业务逻辑

src/main/resources/
├── fxml/
│   └── user-view.fxml          # 布局
└── css/
    └── style.css               # 样式
```

### MVVM 模式（复杂应用）

```
src/main/java/com/example/
├── App.java
├── model/
│   └── User.java               # 纯数据模型
├── viewmodel/
│   └── UserViewModel.java      # 暴露 Properties、命令
├── view/
│   └── UserController.java     # 将 UI 绑定到 ViewModel
└── service/
    └── UserService.java

src/main/resources/
├── fxml/
│   └── user-view.fxml
└── css/
    └── style.css
```

## 代码示例

### 应用入口

```java
package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
        Scene scene = new Scene(loader.load(), 1024, 768);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        stage.setTitle("我的 JavaFX 应用");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

### 含 Properties 的模型

```java
package com.example.model;

import javafx.beans.property.*;

public class User {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final BooleanProperty active = new SimpleBooleanProperty(true);

    // ID
    public long getId() { return id.get(); }
    public void setId(long value) { id.set(value); }
    public LongProperty idProperty() { return id; }

    // 姓名
    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }
    public StringProperty nameProperty() { return name; }

    // 邮箱
    public String getEmail() { return email.get(); }
    public void setEmail(String value) { email.set(value); }
    public StringProperty emailProperty() { return email; }

    // 启用状态
    public boolean isActive() { return active.get(); }
    public void setActive(boolean value) { active.set(value); }
    public BooleanProperty activeProperty() { return active; }
}
```

### 含数据绑定的 Controller

```java
package com.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import com.example.model.User;
import java.net.URL;
import java.util.ResourceBundle;

public class UserController implements Initializable {
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private CheckBox activeCheckBox;
    @FXML private Label statusLabel;
    @FXML private Button saveButton;

    private final User model = new User();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 双向绑定
        nameField.textProperty().bindBidirectional(model.nameProperty());
        emailField.textProperty().bindBidirectional(model.emailProperty());
        activeCheckBox.selectedProperty().bindBidirectional(model.activeProperty());

        // 计算绑定
        statusLabel.textProperty().bind(
            model.nameProperty().concat(" - ").concat(model.emailProperty())
        );

        // 验证绑定
        saveButton.disableProperty().bind(
            model.nameProperty().isEmpty().or(model.emailProperty().isEmpty())
        );
    }

    @FXML
    private void handleSave() {
        // 委托给 Service 层
        System.out.println("保存用户: " + model.getName());
    }
}
```

### FXML 布局

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.layout.VBox?>
<?import javafx.scene.layout.HBox?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.TextField?>
<?import javafx.scene.control.CheckBox?>
<?import javafx.scene.control.Button?>
<?import javafx.scene.layout.GridPane?>

<VBox xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.example.controller.UserController"
      spacing="15" styleClass="form-container">

    <GridPane hgap="10" vgap="10">
        <Label text="姓名:" GridPane.rowIndex="0" GridPane.columnIndex="0"/>
        <TextField fx:id="nameField" GridPane.rowIndex="0" GridPane.columnIndex="1"/>

        <Label text="邮箱:" GridPane.rowIndex="1" GridPane.columnIndex="0"/>
        <TextField fx:id="emailField" GridPane.rowIndex="1" GridPane.columnIndex="1"/>

        <Label text="启用:" GridPane.rowIndex="2" GridPane.columnIndex="0"/>
        <CheckBox fx:id="activeCheckBox" GridPane.rowIndex="2" GridPane.columnIndex="1"/>
    </GridPane>

    <HBox spacing="10">
        <Button fx:id="saveButton" text="保存" onAction="#handleSave" styleClass="button-primary"/>
        <Label fx:id="statusLabel" styleClass="status-label"/>
    </HBox>
</VBox>
```

### CSS 样式

```css
.root {
    -fx-primary: #3b82f6;
    -fx-bg-base: #ffffff;
    -fx-text-primary: #0f172a;
    -fx-border: #e2e8f0;
}

.form-container {
    -fx-padding: 20;
    -fx-background-color: -fx-bg-base;
}

.button-primary {
    -fx-background-color: -fx-primary;
    -fx-text-fill: white;
    -fx-padding: 8 16;
    -fx-background-radius: 6;
}

.button-primary:hover {
    -fx-background-color: derive(-fx-primary, -10%);
}
```

## 常见 UI 模式

### CRUD 表格视图
- `TableView` 配合 `TableColumn` 绑定到 Properties
- `FilteredList` + `SortedList` 实现过滤/排序
- 通过 `Pagination` 控件实现分页
- 右键菜单实现行操作（编辑、删除）

### 登录对话框
- `Dialog<User>` 配合自定义 `DialogPane`
- 表单验证即时反馈
- "记住我"使用 Preferences API
- 认证过程中的加载状态

### 主从视图
- 分割面板：主列表 + 详情表单
- 选择监听器更新详情视图
- 防抖定时器实现自动保存
- 未保存更改警告

### 导航抽屉
- `BorderPane` 配合可折叠侧边栏
- `ListView` 或自定义菜单项
- 通过 `FXMLLoader` 加载到内容区域实现视图切换
- 面包屑导航

## 第三方库集成

| 库 | 用途 | Maven 坐标 |
|----|------|-----------|
| ControlsFX | 对话框、通知、验证 | `org.controlsfx:controlsfx:11.2.1` |
| MaterialFX | Material Design 控件 | `io.github.palexdev:materialfx:11.17.0` |
| RichTextFX | 富文本编辑器、代码高亮 | `org.fxmisc.richtext:richtextfx:0.11.5` |
| Ikonli | 字体图标（FontAwesome、Material） | `org.kordamp.ikonli:ikonli-javafx:12.3.1` |
| ValidatorFX | 表单验证框架 | `net.synedra:validatorfx:0.4.0` |
| TestFX | UI 自动化测试 | `org.testfx:testfx-junit5:4.0.18` |

详细集成指南见 `references/third-party-libraries.md`。

## 打包

### jpackage（推荐）

```bash
# 先构建 JAR
mvn clean package

# 创建原生安装包
jpackage \
  --type exe \
  --name "MyApp" \
  --app-version 1.0.0 \
  --input target/libs \
  --main-jar myapp.jar \
  --main-class com.example.App \
  --icon src/main/resources/icon.ico \
  --win-menu \
  --win-shortcut \
  --java-options "--enable-native-access=javafx.graphics"
```

### 各平台输出类型：
- Windows：`--type exe` 或 `--type msi`
- macOS：`--type dmg` 或 `--type pkg`
- Linux：`--type deb` 或 `--type rpm`

详细打包指南见 `references/packaging-deployment.md`。

## 约束

### 编码规范
1. **命名**：类名 PascalCase，方法/变量 camelCase，常量 SCREAMING_SNAKE_CASE
2. **缩进**：4 个空格，不使用 Tab
3. **编码**：所有源文件使用 UTF-8
4. **导入**：显式导入，不使用通配符（`import javafx.scene.control.*`）
5. **注释**：公共 API 使用 Javadoc，复杂逻辑使用行内注释

### 架构规则
1. **FXML 纯净性**：FXML 文件中不使用 `<fx:script>`
2. **Controller 职责**：仅处理 UI 事件和视图状态，业务逻辑委托给 Service
3. **绑定优先**：优先使用 JavaFX Properties 绑定，而非手动同步 UI
4. **资源路径**：使用 `getClass().getResource()` 加载 FXML/CSS
5. **线程安全**：所有 UI 更新在 JavaFX Application Thread 上执行，使用 `Task`/`Service` 处理后台任务

### 安全规则
1. **输入验证**：验证所有用户输入，不拼接 SQL/命令
2. **路径安全**：使用 `Paths.get()` + `Path.normalize()` 处理文件操作
3. **无硬编码密钥**：使用配置文件或环境变量
4. **WebView 安全**：禁用 JavaScript 或限制为可信内容

## 输出格式

交付代码时，始终提供：

1. **文件清单** — 列出所有生成的文件及完整路径
2. **依赖说明** — 所需的 Maven/Gradle 依赖
3. **运行说明** — 编译和运行命令（如 `mvn javafx:run`）
4. **后续步骤** — 扩展、测试、打包建议

### 输出示例结构

```
## 生成的文件

### Java 源码
- `src/main/java/com/example/App.java` — 应用入口
- `src/main/java/com/example/controller/MainController.java` — 主控制器
- `src/main/java/com/example/model/User.java` — 数据模型

### 资源文件
- `src/main/resources/fxml/main-view.fxml` — 主布局
- `src/main/resources/css/style.css` — 样式表

### 构建配置
- `pom.xml` — Maven 构建配置
- `src/main/java/module-info.java` — 模块描述符

### 依赖
[如需额外的 Maven 依赖]

### 运行命令
mvn javafx:run
```

## 质量检查清单

交付前，验证以下事项：
- [ ] 所有 Java 文件无语法错误可编译通过
- [ ] FXML 的 `fx:id` 字段与 Controller 字段匹配
- [ ] CSS 文件无语法错误，变量已定义
- [ ] `module-info.java` 包含所有必要的 `requires` 和 `opens`
- [ ] 包路径一致，无拼写错误
- [ ] 类名/方法名遵循命名规范
- [ ] 无硬编码敏感信息或绝对路径
- [ ] 线程安全：后台任务使用 `Platform.runLater()`
- [ ] 资源路径正确（相对路径，非绝对路径）
- [ ] 公共 API 有 Javadoc 注释

## 参考文档

如需深入指导，请参阅 `references/` 目录中的以下文档：

- `references/project-setup.md` — Maven/Gradle 配置、版本矩阵、模块化设置
- `references/architecture-patterns.md` — MVC/MVVM 详细对比、反模式
- `references/spring-boot-integration.md` — Spring Boot + JavaFX 整合、启动类拆分、依赖注入、常见陷阱
- `references/css-best-practices.md` — CSS 选择器、主题变量、响应式布局
- `references/data-binding-patterns.md` — Property 类型、绑定模式、表单验证
- `references/third-party-libraries.md` — 库集成指南、兼容性矩阵
- `references/packaging-deployment.md` — jpackage、jlink、CI/CD 集成
- `EVALUATE.md` — 评估用例集，用于量化技能输出质量

## 模板库

`templates/` 目录中的可复用代码模板：

- `templates/maven/pom.xml` — Maven POM 模板
- `templates/maven/module-info.java` — 模块描述符模板
- `templates/gradle/build.gradle` — Gradle 构建模板
- `templates/fxml/main-view.fxml` — 主窗口 FXML 模板
- `templates/fxml/dialog.fxml` — 对话框 FXML 模板
- `templates/controller/MainController.java` — Controller 模板
- `templates/controller/BaseController.java` — 基类 Controller 模板
- `templates/controller/DialogController.java` — 对话框 Controller 模板
- `templates/model/ObservableModel.java` — Model 模板
- `templates/viewmodel/UserViewModel.java` — ViewModel 模板（MVVM 模式）
- `templates/service/Service.java` — Service 层模板
- `templates/css/light-theme.css` — 亮色主题 CSS
- `templates/css/dark-theme.css` — 暗色主题 CSS
- `templates/packaging/jpackage-config.properties` — 打包配置
