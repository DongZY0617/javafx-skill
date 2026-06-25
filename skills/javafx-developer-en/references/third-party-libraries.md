# JavaFX 第三方库集成指南

本指南介绍 JavaFX 生态中常用第三方库的功能、Maven 坐标、使用示例及版本兼容性，帮助开发者快速选型与集成。

---

## 一、ControlsFX

ControlsFX 是 JavaFX 生态中最成熟的扩展控件库，提供对话框、通知、自动补全、属性表等丰富组件。

### 1.1 主要功能

| 功能模块            | 说明                                                       |
|---------------------|------------------------------------------------------------|
| Dialogs             | 增强对话框（确认、输入、异常展示等），比原生 Alert 更灵活  |
| Notifications       | 桌面通知气泡，支持自定义位置、动画、关闭动作               |
| AutoComplete        | 文本框自动补全                                             |
| PropertySheet       | 属性表控件，可视化编辑对象属性                             |
| CheckComboBox       | 可多选的下拉框                                             |
| CheckListView       | 可多选的列表                                               |
| RangeSlider         | 双滑块范围选择器                                           |
| Rating              | 星级评分控件                                               |
| SegmentedButton     | 分段按钮组                                                 |
| StatusBar           | 状态栏控件                                                 |
| MasterDetailPane    | 主从布局面板                                               |
| PlusMinusSlider     | 加减滑块                                                   |
| SpreadsheetView     | 电子表格视图                                               |

### 1.2 Maven 坐标

```xml
<dependency>
    <groupId>org.controlsfx</groupId>
    <artifactId>controlsfx</artifactId>
    <version>11.2.1</version>
</dependency>
```

### 1.3 代码示例

**对话框：**

```java
import org.controlsfx.dialog.Dialogs;

// 确认对话框
boolean confirm = Dialogs.create()
    .title("确认删除")
    .masthead("您确定要删除此项目吗？")
    .message("此操作不可撤销。")
    .showConfirm() == ButtonType.OK;

// 输入对话框
Optional<String> result = Dialogs.create()
    .title("输入名称")
    .masthead("请输入项目名称")
    .showTextInput();
```

> 注意：ControlsFX 较新版本推荐使用 JavaFX 原生 `Alert` 和 `TextInputDialog`，ControlsFX 的 `Dialogs` API 主要用于兼容旧代码。

**通知气泡：**

```java
import org.controlsfx.control.Notifications;

Notifications.create()
    .title("操作成功")
    .text("数据已保存")
    .graphic(new ImageView(successIcon))
    .hideAfter(Duration.seconds(3))
    .position(Pos.BOTTOM_RIGHT)
    .showInformation();

// 错误通知
Notifications.create()
    .title("保存失败")
    .text("网络连接超时")
    .showError();
```

**自动补全：**

```java
import org.controlsfx.control.textfield.TextFields;

TextField searchField = new TextField();
// 绑定自动补全建议列表
TextFields.bindAutoCompletion(searchField,
    "Apple", "Banana", "Cherry", "Date", "Elderberry");
```

**属性表：**

```java
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.BeanProperty;
import org.controlsfx.property.editor.DefaultPropertyEditorFactory;

PropertySheet propertySheet = new PropertySheet();
// 添加基于 JavaBean 的属性项
propertySheet.getItems().addAll(
    new BeanProperty(person, "name"),
    new BeanProperty(person, "age"),
    new BeanProperty(person, "email")
);
```

**CheckComboBox（多选下拉框）：**

```java
import org.controlsfx.control.CheckComboBox;

CheckComboBox<String> checkCombo = new CheckComboBox<>(
    FXCollections.observableArrayList("选项A", "选项B", "选项C"));
checkCombo.getCheckModel().check(0);  // 勾选第一项
ObservableList<String> checked = checkCombo.getCheckModel().getCheckedItems();
```

---

## 二、MaterialFX

MaterialFX 提供 Google Material Design 风格的 JavaFX 控件，包含现代化的视觉设计与丰富的交互组件。

### 2.1 主要控件

| 控件              | 说明                                |
|-------------------|-------------------------------------|
| MFXButton         | Material 风格按钮                   |
| MFXTextField      | 带浮动标签的文本框                  |
| MFXCheckbox       | Material 复选框                     |
| MFXComboBox       | 增强下拉框                          |
| MFXDatePicker     | 日期选择器                          |
| MFXTableView      | 增强表格（支持过滤、排序）          |
| MFXDialog         | Material 风格对话框                 |
| MFXNotification   | 通知组件                            |
| MFXProgressBar    | 进度条                              |
| MFXSlider         | 滑块                                |
| MFXStepper        | 步骤导航器                          |
| MFXFilterComboBox | 可过滤下拉框                        |

### 2.2 Maven 坐标

```xml
<dependency>
    <groupId>io.github.palexdev</groupId>
    <artifactId>materialfx</artifactId>
    <version>11.17.0</version>
</dependency>
```

### 2.3 使用示例

```java
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.MFXComboBox;

// Material 风格按钮
MFXButton button = new MFXButton("提交");
button.setStyle("-fx-background-color: #6200ee; -fx-text-fill: white;");

// 浮动标签文本框
MFXTextField textField = new MFXTextField();
textField.setFloatingText("用户名");
textField.setPromptText("请输入用户名");

// 增强下拉框
MFXComboBox<String> combo = new MFXComboBox<>();
combo.getItems().addAll("选项一", "选项二", "选项三");
combo.setPromptText("请选择");
combo.selectFirst();

// 在 FXML 中使用
```

```xml
<?import io.github.palexdev.materialfx.controls.*?>

<MFXButton text="提交" styleClass="mfx-button"/>
<MFXTextField floatingText="邮箱" promptText="请输入邮箱"/>
```

---

## 三、RichTextFX

RichTextFX 提供富文本编辑区域，支持样式化文本、代码高亮、行号显示等功能，是构建代码编辑器或富文本编辑器的首选。

### 3.1 主要功能

| 功能             | 说明                                              |
|------------------|---------------------------------------------------|
| StyleClassedTextArea | 基于样式类的富文本区域                        |
| CodeArea         | 专为代码编辑设计的文本区域，支持行号              |
| InlineCssTextArea | 基于 CSS 的富文本区域                            |
| 语法高亮         | 通过样式类实现关键词、注释、字符串等高亮          |
| 行号显示         | 自动行号 gutter                                   |
| 撤销/重做        | 内置撤销重做支持                                  |

### 3.2 Maven 坐标

```xml
<dependency>
    <groupId>org.fxmisc.richtext</groupId>
    <artifactId>richtextfx</artifactId>
    <version>0.11.3</version>
</dependency>
```

### 3.3 代码示例

**基本代码编辑器：**

```java
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

CodeArea codeArea = new CodeArea();
// 添加行号
codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

// 设置初始内容
codeArea.replaceText(0, 0, "public class Hello {\n    \n}");

// 应用语法高亮（简化示例）
codeArea.textProperty().addListener((obs, oldText, newText) -> {
    // 清除旧样式
    codeArea.setStyle(0, newText.length(), "-fx-fill: black;");

    // 高亮关键字
    String[] keywords = {"public", "class", "void", "static", "private"};
    for (String keyword : keywords) {
        int idx = 0;
        while ((idx = newText.indexOf(keyword, idx)) >= 0) {
            codeArea.setStyle(idx, idx + keyword.length(),
                "-fx-fill: #cc7832; -fx-font-weight: bold;");
            idx += keyword.length();
        }
    }
});
```

**富文本编辑：**

```java
import org.fxmisc.richtext.StyleClassedTextArea;

StyleClassedTextArea area = new StyleClassedTextArea();
area.appendText("普通文本 ");
area.appendText("红色加粗", List.of("red-bold"));
area.appendText(" 普通文本");

// CSS 中定义样式类
// .red-bold { -fx-fill: red; -fx-font-weight: bold; }
```

---

## 四、Ikonli

Ikonli 提供基于字体图标（Font Icons）的统一方案，集成数十种图标包，替代传统图片图标。

### 4.1 支持的图标包（部分）

| 图标包            | 说明                              | Maven artifactId              |
|-------------------|-----------------------------------|-------------------------------|
| MaterialDesign    | Google Material Design 图标       | `ikonli-materialdesign2-pack` |
| FontAwesome       | Font Awesome 图标                 | `ikonli-fontawesome-pack`     |
| Material Icons    | Material Icons                    | `ikonli-materialicons-pack`   |
| Ionicons          | Ionicons 图标                     | `ikonli-ionicons4-pack`       |
| Octicons          | GitHub Octicons                   | `ikonli-octicons-pack`        |
| Feather           | Feather Icons                     | `ikonli-feather-pack`         |
| Bootstrap Icons   | Bootstrap Icons                   | `ikonli-bootstrapicons-pack`  |
| BoxIcons          | Box Icons                         | `ikonli-boxicons-pack`        |
| AntDesign Icons   | Ant Design Icons                  | `ikonli-antdesignicons-pack`  |
| CoreUI            | CoreUI Icons                      | `ikonli-coreui-pack`          |

### 4.2 Maven 坐标

```xml
<!-- 核心库 -->
<dependency>
    <groupId>org.kordamp.ikonli</groupId>
    <artifactId>ikonli-javafx</artifactId>
    <version>12.3.1</version>
</dependency>

<!-- 按需选择图标包 -->
<dependency>
    <groupId>org.kordamp.ikonli</groupId>
    <artifactId>ikonli-materialdesign2-pack</artifactId>
    <version>12.3.1</version>
</dependency>
<dependency>
    <groupId>org.kordamp.ikonli</groupId>
    <artifactId>ikonli-fontawesome-pack</artifactId>
    <version>12.3.1</version>
</dependency>
```

### 4.3 使用示例

```java
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.fontawesome.FontAwesome;

// 创建图标
FontIcon saveIcon = new FontIcon(MaterialDesignS.CONTENT_SAVE);
saveIcon.setIconSize(24);
saveIcon.setIconColor(Color.BLUE);

// 添加到按钮
Button saveButton = new Button("保存");
saveButton.setGraphic(saveIcon);

// FontAwesome 图标
FontIcon userIcon = new FontIcon(FontAwesome.USER);
userIcon.setIconSize(20);

// 在 FXML 中使用
```

```xml
<?import org.kordamp.ikonli.javafx.FontIcon?>
<?import org.kordamp.ikonli.materialdesign2.MaterialDesignS?>

<FontIcon iconLiteral="md2al-content_save" iconSize="24" />
```

---

## 五、ValidatorFX

ValidatorFX 提供声明式表单校验框架，与 JavaFX 控件深度集成。

### 5.1 Maven 坐标

```xml
<dependency>
    <groupId>net.synedra</groupId>
    <artifactId>validatorfx</artifactId>
    <version>0.4.0</version>
</dependency>
```

### 5.2 使用示例

```java
import net.synedra.validatorfx.Validator;

public class FormController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField ageField;
    @FXML private Button submitButton;

    private Validator validator = new Validator();

    @FXML
    public void initialize() {
        // 用户名校验：非空且长度 >= 3
        validator.createCheck()
            .dependsOn("username", usernameField.textProperty())
            .withMethod(c -> {
                String username = c.get("username");
                if (username == null || username.trim().length() < 3) {
                    c.error("用户名至少 3 个字符");
                }
            })
            .decoratingWith(this::decorateError)
            .decorate(usernameField);

        // 邮箱校验
        validator.createCheck()
            .dependsOn("email", emailField.textProperty())
            .withMethod(c -> {
                String email = c.get("email");
                if (email == null || !email.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) {
                    c.error("邮箱格式不正确");
                }
            })
            .decoratingWith(this::decorateError)
            .decorate(emailField);

        // 年龄校验
        validator.createCheck()
            .dependsOn("age", ageField.textProperty())
            .withMethod(c -> {
                try {
                    int age = Integer.parseInt(c.get("age"));
                    if (age < 0 || age > 150) {
                        c.error("年龄必须在 0-150 之间");
                    }
                } catch (NumberFormatException e) {
                    c.error("年龄必须是数字");
                }
            })
            .decoratingWith(this::decorateError)
            .decorate(ageField);

        // 提交按钮绑定校验状态
        submitButton.disableProperty().bind(validator.containsErrorsProperty());
    }

    /** 错误装饰器：为控件添加红色边框和提示 */
    private Decoration decorateError(ValidationMessage message) {
        return new Decoration() {
            @Override
            public void add(Node target) {
                target.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                // 可添加 Tooltip 显示错误信息
                if (target instanceof Control control) {
                    Tooltip tooltip = new Tooltip(message.getText());
                    tooltip.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                    Tooltip.install(control, tooltip);
                }
            }

            @Override
            public void remove(Node target) {
                target.setStyle("");
                if (target instanceof Control control) {
                    Tooltip.uninstall(control, null);
                }
            }
        };
    }

    @FXML
    private void handleSubmit() {
        if (!validator.containsErrors()) {
            // 提交逻辑
        }
    }
}
```

---

## 六、TestFX

TestFX 是 JavaFX 的 UI 自动化测试框架，支持模拟用户交互并断言 UI 状态。

### 6.1 Maven 坐标

```xml
<dependency>
    <groupId>org.testfx</groupId>
    <artifactId>testfx-core</artifactId>
    <version>4.0.18</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testfx</groupId>
    <artifactId>testfx-junit5</artifactId>
    <version>4.0.18</version>
    <scope>test</scope>
</dependency>
```

### 6.2 测试示例

```java
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

class MainAppTest extends ApplicationTest {

    @Override
    public void start(Stage stage) throws Exception {
        // 启动应用
        new MainApp().start(stage);
    }

    @Test
    void shouldAddTaskWhenClickingAddButton() {
        // 输入任务标题
        clickOn("#titleField").write("测试任务");

        // 点击添加按钮
        clickOn("#addButton");

        // 验证列表包含新任务
        verifyThat("#taskListView", node -> {
            ListView<?> list = (ListView<?>) node;
            return list.getItems().size() == 1;
        });

        // 验证状态标签更新
        verifyThat("#statusLabel", hasText("共 1 个任务"));
    }

    @Test
    void shouldShowErrorWhenTitleEmpty() {
        // 不输入直接点击添加
        clickOn("#addButton");

        // 验证列表仍为空
        verifyThat("#taskListView", node -> {
            ListView<?> list = (ListView<?>) node;
            return list.getItems().isEmpty();
        });
    }
}
```

### 6.3 TestFX 常用操作

```java
// 鼠标操作
clickOn("#buttonId");
rightClickOn("#nodeId");
doubleClickOn("#nodeId");
moveTo("#nodeId");

// 键盘操作
clickOn("#textField").write("Hello World");
type(KeyCode.ENTER);
press(KeyCode.CONTROL).press(KeyCode.C).release(KeyCode.C).release(KeyCode.CONTROL);

// 查找节点
Button button = lookup("#submitButton").query();
Label label = lookup(".error-label").query();

// 断言
verifyThat("#label", hasText("Success"));
verifyThat("#button", NodeMatchers.isVisible());
verifyThat("#button", NodeMatchers.isDisabled());
```

---

## 七、JMetro

JMetro 是基于 Microsoft Modern UI（Metro/Fluent Design）风格的 JavaFX 主题，提供现代扁平化外观。

### 7.1 Maven 坐标

```xml
<dependency>
    <groupId>org.jfxtras</groupId>
    <artifactId>jmetro</artifactId>
    <version>11.6.16</version>
</dependency>
```

### 7.2 使用示例

```java
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(url);
        Scene scene = new Scene(loader.load());

        // 应用 JMetro 主题（亮色或暗色）
        JMetro jMetro = new JMetro(Style.LIGHT);
        jMetro.setScene(scene);

        stage.setScene(scene);
        stage.show();
    }
}
```

JMetro 会自动为所有标准 JavaFX 控件应用 Modern UI 风格，无需手动编写 CSS。

---

## 八、BootstrapFX

BootstrapFX 将 Twitter Bootstrap 的 CSS 样式引入 JavaFX，提供熟悉的 Bootstrap 风格控件外观。

### 8.1 Maven 坐标

```xml
<dependency>
    <groupId>org.kordamp.bootstrapfx</groupId>
    <artifactId>bootstrapfx-core</artifactId>
    <version>0.4.0</version>
</dependency>
```

### 8.2 使用示例

```java
public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Scene scene = new Scene(root, 800, 600);

        // 加载 BootstrapFX 样式表
        scene.getStylesheets().add(
            "org/kordamp/bootstrapfx/bootstrapfx.css");

        stage.setScene(scene);
        stage.show();
    }
}
```

```xml
<!-- 在 FXML 中使用 Bootstrap 样式类 -->
<Button text="Primary" styleClass="btn,btn-primary"/>
<Button text="Danger" styleClass="btn,btn-danger"/>
<Button text="Success" styleClass="btn,btn-success"/>
<Label text="警告提示" styleClass="alert,alert-warning"/>
<Label text="信息提示" styleClass="badge,badge-info"/>
```

---

## 九、FXGL

FXGL 是基于 JavaFX 的 2D 游戏开发引擎，提供游戏循环、物理引擎、动画、AI、粒子系统等完整功能。

### 9.1 Maven 坐标

```xml
<dependency>
    <groupId>com.github.almasb</groupId>
    <artifactId>fxgl</artifactId>
    <version>17.3</version>
</dependency>
```

### 9.2 使用示例

```java
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.input.KeyCode;
import javafx.scene.shape.Rectangle;

public class SimpleGame extends GameApplication {

    private Entity player;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("简单游戏");
        settings.setWidth(800);
        settings.setHeight(600);
    }

    @Override
    protected void initGame() {
        // 创建玩家实体
        player = FXGL.entityBuilder()
            .at(400, 300)
            .view(new Rectangle(40, 40, javafx.scene.paint.Color.BLUE))
            .buildAndAttach();
    }

    @Override
    protected void initInput() {
        // 键盘控制
        FXGL.onKey(KeyCode.W, () -> player.translateY(-5));
        FXGL.onKey(KeyCode.S, () -> player.translateY(5));
        FXGL.onKey(KeyCode.A, () -> player.translateX(-5));
        FXGL.onKey(KeyCode.D, () -> player.translateX(5));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

---

## 十、兼容性矩阵

下表展示各第三方库与 JavaFX 版本的兼容情况（基于公开信息整理，实际使用前请查阅最新文档）。

| 库               | Maven 版本    | JavaFX 17 | JavaFX 21 | JavaFX 24+ | 备注                          |
|------------------|---------------|-----------|-----------|------------|-------------------------------|
| ControlsFX       | 11.2.1        | 兼容      | 兼容      | 基本兼容   | 成熟稳定，广泛使用            |
| MaterialFX       | 11.17.0       | 兼容      | 兼容      | 需测试     | 需 JavaFX 17+                 |
| RichTextFX       | 0.11.3        | 兼容      | 兼容      | 需测试     | 依赖 ReactFX / Flowless       |
| Ikonli           | 12.3.1        | 兼容      | 兼容      | 兼容       | 纯 Java，兼容性好             |
| ValidatorFX      | 0.4.0         | 兼容      | 兼容      | 需测试     | 轻量级                        |
| TestFX           | 4.0.18        | 兼容      | 兼容      | 需测试     | 测试框架                      |
| JMetro           | 11.6.16       | 兼容      | 兼容      | 需测试     | 主题库                        |
| BootstrapFX      | 0.4.0         | 兼容      | 兼容      | 兼容       | 纯 CSS，兼容性好              |
| FXGL             | 17.3          | 兼容      | 兼容      | 需测试     | 游戏引擎，需 JavaFX 17+       |

### 兼容性注意事项

1. **JavaFX 24+ 的 `--enable-native-access`**：部分库若依赖本地代码，可能需要额外配置 JVM 参数。
2. **模块化冲突**：非模块化库在 jlink 打包时可能报错，需通过 `--add-modules` 或将库转为自动模块处理。
3. **版本锁定**：建议在 `pom.xml` 中统一锁定 JavaFX 版本与库版本，避免传递依赖冲突。
4. **测试验证**：升级 JavaFX 版本后，务必回归测试所有第三方库功能。

---

## 十一、库选型建议

| 需求场景                 | 推荐库                              |
|--------------------------|-------------------------------------|
| 对话框 / 通知 / 高级控件 | ControlsFX                          |
| Material Design 风格 UI  | MaterialFX                          |
| 代码编辑器 / 富文本      | RichTextFX                          |
| 字体图标                 | Ikonli                              |
| 表单校验                 | ValidatorFX 或原生 BooleanBinding   |
| UI 自动化测试            | TestFX                              |
| 现代主题（Metro 风格）   | JMetro                              |
| Bootstrap 风格           | BootstrapFX                         |
| 2D 游戏开发              | FXGL                                |
| 多选下拉框               | ControlsFX (CheckComboBox)          |
| 范围滑块                 | ControlsFX (RangeSlider)            |
| 属性编辑面板             | ControlsFX (PropertySheet)          |
