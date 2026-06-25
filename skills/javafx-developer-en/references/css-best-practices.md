# JavaFX CSS 最佳实践指南

本指南涵盖 JavaFX CSS 的语法特性、变量与主题架构、选择器优先级、常用控件样式、响应式布局、动画过渡、`derive()` 函数、Scene Builder 集成以及运行时主题切换。

---

## 一、JavaFX CSS 与 Web CSS 的语法差异

JavaFX CSS 基于 CSS 语法，但存在若干关键差异，理解这些差异是正确样式化的前提。

| 特性            | Web CSS                          | JavaFX CSS                                    |
|-----------------|----------------------------------|-----------------------------------------------|
| 属性前缀        | 无前缀（如 `color`）             | 多数属性以 `-fx-` 前缀（如 `-fx-text-fill`）  |
| 颜色属性        | `color`（文字）、`background-color` | `-fx-text-fill`（文字）、`-fx-background-color` |
| 尺寸单位        | 支持 `px`、`em`、`rem`、`%` 等    | 支持 `px`、`em`、`pt`，但不支持 `rem`         |
| 选择器类型      | 标签、类、ID、属性、伪类          | 类型选择器用类名（如 `.button`）、ID、伪类    |
| 伪类            | `:hover`、`:focus` 等             | `:hover`、`:focused`、`:pressed`、`:armed` 等 |
| 变量（自定义属性）| `--var` + `var()`               | `-fx-var`（JavaFX 17+），或通过代码设置       |
| 布局属性        | `display`、`flex`、`grid`         | JavaFX 布局由 Layout 容器管理，CSS 仅控制外观 |
| 函数            | `calc()`、`rgb()` 等              | `derive()`、`ladder()` 等 JavaFX 特有函数     |
| 继承            | 部分属性继承                      | `-fx-font-*` 等属性可继承                     |

### 1.1 类型选择器说明

在 Web CSS 中 `button` 选择标签名，而在 JavaFX CSS 中，类型选择器使用控件的样式类名（全小写），例如：

```css
/* JavaFX 中 .button 等价于匹配所有 Button 控件 */
.button {
    -fx-background-color: #4a90d9;
    -fx-text-fill: white;
}
```

### 1.2 常用 JavaFX CSS 属性对照

| 用途         | Web CSS 属性        | JavaFX CSS 属性            |
|--------------|---------------------|----------------------------|
| 文字颜色     | `color`             | `-fx-text-fill`            |
| 背景颜色     | `background-color`  | `-fx-background-color`     |
| 字体大小     | `font-size`         | `-fx-font-size`            |
| 字体粗细     | `font-weight`       | `-fx-font-weight`          |
| 内边距       | `padding`           | `-fx-padding`              |
| 圆角         | `border-radius`     | `-fx-background-radius`    |
| 边框         | `border`            | `-fx-border-color` / `-fx-border-width` |
| 光标         | `cursor`            | `-fx-cursor`               |
| 透明度       | `opacity`           | `-fx-opacity` / `opacity`  |

---

## 二、CSS 变量（自定义属性）

JavaFX 17+ 支持 CSS 自定义属性（变量），使用 `-fx-` 前缀定义，通过 `var()` 引用。这使得主题色集中管理成为可能。

### 2.1 定义与使用

```css
.root {
    /* 定义主题变量 */
    -fx-primary-color: #2196f3;
    -fx-accent-color: #ff9800;
    -fx-bg-color: #ffffff;
    -fx-text-color: #333333;
    -fx-radius: 8;
}

/* 引用变量 */
.button-primary {
    -fx-background-color: var(-fx-primary-color);
    -fx-text-fill: white;
    -fx-background-radius: var(-fx-radius);
}
```

### 2.2 变量作用域

- 在 `.root` 上定义的变量具有全局作用域，所有节点可访问。
- 在特定节点上重新定义变量可覆盖全局值，仅影响该节点及其子节点。

```css
.custom-pane {
    /* 仅此 pane 及其子节点使用不同的主色 */
    -fx-primary-color: #e91e63;
}
```

---

## 三、主题变量架构（亮色 / 暗色）

通过 CSS 变量构建完整的主题系统，实现亮色与暗色主题的统一管理。

### 3.1 亮色主题变量定义

```css
/* light-theme.css */
.root {
    /* 背景色系 */
    -fx-bg-primary: #ffffff;
    -fx-bg-secondary: #f5f5f5;
    -fx-bg-tertiary: #e0e0e0;

    /* 文字色系 */
    -fx-text-primary: #212121;
    -fx-text-secondary: #757575;
    -fx-text-disabled: #bdbdbd;

    /* 强调色 */
    -fx-accent: #2196f3;
    -fx-accent-hover: derive(-fx-accent, -15%);
    -fx-accent-pressed: derive(-fx-accent, -25%);

    /* 状态色 */
    -fx-success: #4caf50;
    -fx-warning: #ff9800;
    -fx-danger: #f44336;

    /* 边框与分割线 */
    -fx-border-color: #e0e0e0;
    -fx-divider-color: #eeeeee;

    /* 圆角与间距 */
    -fx-radius-sm: 4;
    -fx-radius-md: 8;
    -fx-radius-lg: 12;
}
```

### 3.2 暗色主题变量定义

```css
/* dark-theme.css */
.root {
    -fx-bg-primary: #1e1e1e;
    -fx-bg-secondary: #252525;
    -fx-bg-tertiary: #333333;

    -fx-text-primary: #ffffff;
    -fx-text-secondary: #b0b0b0;
    -fx-text-disabled: #666666;

    -fx-accent: #64b5f6;
    -fx-accent-hover: derive(-fx-accent, 15%);
    -fx-accent-pressed: derive(-fx-accent, 25%);

    -fx-success: #66bb6a;
    -fx-warning: #ffa726;
    -fx-danger: #ef5350;

    -fx-border-color: #444444;
    -fx-divider-color: #383838;

    -fx-radius-sm: 4;
    -fx-radius-md: 8;
    -fx-radius-lg: 12;
}
```

### 3.3 控件样式引用主题变量

```css
.button {
    -fx-background-color: var(-fx-bg-secondary);
    -fx-text-fill: var(-fx-text-primary);
    -fx-background-radius: var(-fx-radius-md);
    -fx-padding: 8 16 8 16;
}

.button:hover {
    -fx-background-color: var(-fx-bg-tertiary);
}

.button-primary {
    -fx-background-color: var(-fx-accent);
    -fx-text-fill: white;
}

.button-primary:hover {
    -fx-background-color: var(-fx-accent-hover);
}

.text-field {
    -fx-background-color: var(-fx-bg-primary);
    -fx-text-fill: var(-fx-text-primary);
    -fx-border-color: var(-fx-border-color);
    -fx-border-width: 1;
    -fx-background-radius: var(-fx-radius-sm);
}

.label {
    -fx-text-fill: var(-fx-text-primary);
}
```

---

## 四、选择器优先级与特异性

JavaFX CSS 遵循与 Web CSS 类似的特异性规则，优先级从高到低为：

1. **内联样式**（通过 `setStyle()` 设置）— 优先级最高
2. **ID 选择器**（`#myId`）
3. **样式类 + 伪类选择器**（`.button:hover`）
4. **样式类选择器**（`.button`）
5. **类型选择器**（`.button` 匹配所有 Button）

### 4.1 优先级示例

```css
/* 类型选择器：优先级最低 */
.button {
    -fx-background-color: gray;
}

/* 样式类选择器：优先级高于类型选择器 */
.danger-button {
    -fx-background-color: red;
}

/* ID 选择器：优先级最高 */
#saveButton {
    -fx-background-color: green;
}
```

```java
// 内联样式：优先级最高，覆盖所有 CSS 规则
saveButton.setStyle("-fx-background-color: blue;");
```

### 4.2 !important

JavaFX CSS 不支持 `!important`。若需强制覆盖，使用更高优先级的选择器或内联样式。

### 4.3 多样式类叠加

一个节点可拥有多个样式类，后添加的类在 CSS 中后定义则优先：

```java
button.getStyleClass().addAll("button", "primary", "large");
```

---

## 五、常用控件样式

### 5.1 Button

```css
.button {
    -fx-background-color: var(-fx-accent);
    -fx-text-fill: white;
    -fx-font-size: 14px;
    -fx-font-weight: bold;
    -fx-padding: 8 20 8 20;
    -fx-background-radius: 6;
    -fx-border-width: 0;
    -fx-cursor: hand;
}

.button:hover {
    -fx-background-color: var(-fx-accent-hover);
}

.button:pressed {
    -fx-background-color: var(-fx-accent-pressed);
}

.button:disabled {
    -fx-opacity: 0.5;
    -fx-cursor: default;
}

/* 带边框的次要按钮 */
.button-outline {
    -fx-background-color: transparent;
    -fx-border-color: var(-fx-accent);
    -fx-border-width: 1.5;
    -fx-border-radius: 6;
    -fx-text-fill: var(-fx-accent);
}
```

### 5.2 TextField / PasswordField

```css
.text-field {
    -fx-background-color: var(-fx-bg-primary);
    -fx-text-fill: var(-fx-text-primary);
    -fx-prompt-text-fill: var(-fx-text-disabled);
    -fx-border-color: var(-fx-border-color);
    -fx-border-width: 1;
    -fx-border-radius: 4;
    -fx-background-radius: 4;
    -fx-padding: 8 10 8 10;
    -fx-font-size: 14px;
}

.text-field:focused {
    -fx-border-color: var(-fx-accent);
    -fx-border-width: 2;
}

.text-field.error {
    -fx-border-color: var(-fx-danger);
}
```

### 5.3 TableView

```css
.table-view {
    -fx-background-color: var(--fx-bg-primary);
    -fx-border-color: var(--fx-border-color);
    -fx-border-width: 1;
}

/* 表头 */
.table-view .column-header {
    -fx-background-color: var(--fx-bg-secondary);
    -fx-border-color: var(--fx-divider-color);
}

.table-view .column-header .label {
    -fx-text-fill: var(--fx-text-primary);
    -fx-font-weight: bold;
}

/* 行 */
.table-row-cell {
    -fx-background-color: var(--fx-bg-primary);
    -fx-border-color: var(--fx-divider-color);
}

.table-row-cell:odd {
    -fx-background-color: var(--fx-bg-secondary);
}

.table-row-cell:selected {
    -fx-background-color: var(--fx-accent);
}

.table-row-cell:selected .text {
    -fx-fill: white;
}

/* 空表格提示 */
.table-view .placeholder .label {
    -fx-text-fill: var(--fx-text-secondary);
}
```

### 5.4 ListView

```css
.list-view {
    -fx-background-color: var(--fx-bg-primary);
    -fx-border-color: var(--fx-border-color);
    -fx-border-width: 1;
    -fx-background-radius: 4;
}

.list-cell {
    -fx-background-color: transparent;
    -fx-text-fill: var(--fx-text-primary);
    -fx-padding: 8 12 8 12;
}

.list-cell:filled:hover {
    -fx-background-color: var(--fx-bg-tertiary);
}

.list-cell:filled:selected {
    -fx-background-color: var(--fx-accent);
    -fx-text-fill: white;
}
```

### 5.5 ComboBox

```css
.combo-box {
    -fx-background-color: var(--fx-bg-primary);
    -fx-border-color: var(--fx-border-color);
    -fx-border-radius: 4;
    -fx-background-radius: 4;
}

.combo-box .arrow-button {
    -fx-background-color: var(--fx-bg-secondary);
}

.combo-box .list-cell {
    -fx-text-fill: var(--fx-text-primary);
}
```

### 5.6 ScrollBar

```css
.scroll-bar {
    -fx-background-color: transparent;
}

.scroll-bar .thumb {
    -fx-background-color: var(--fx-text-disabled);
    -fx-background-radius: 4;
}

.scroll-bar .thumb:hover {
    -fx-background-color: var(--fx-text-secondary);
}

.scroll-bar .increment-button,
.scroll-bar .decrement-button {
    -fx-background-color: transparent;
    -fx-padding: 0;
}
```

### 5.7 MenuBar / MenuItem

```css
.menu-bar {
    -fx-background-color: var(--fx-bg-secondary);
    -fx-border-color: var(--fx-divider-color);
    -fx-border-width: 0 0 1 0;
}

.menu-bar .label {
    -fx-text-fill: var(--fx-text-primary);
}

.menu-item .label {
    -fx-text-fill: var(--fx-text-primary);
}

.menu-item:focused {
    -fx-background-color: var(--fx-accent);
}

.menu-item:focused .label {
    -fx-text-fill: white;
}
```

---

## 六、响应式布局技巧

JavaFX 没有像 Web 的媒体查询（media query），但可通过以下方式实现响应式效果。

### 6.1 使用 Layout 容器自动伸缩

```java
// HBox / VBox 的 hgrow / vgrow 属性实现弹性布局
HBox.setHgrow(textField, Priority.ALWAYS);
VBox.setVgrow(tableView, Priority.ALWAYS);
```

### 6.2 监听窗口尺寸动态切换样式

```java
scene.widthProperty().addListener((obs, oldVal, newVal) -> {
    double width = newVal.doubleValue();
    ObservableList<String> classes = root.getStyleClass();
    classes.removeAll("layout-small", "layout-medium", "layout-large");
    if (width < 600) {
        classes.add("layout-small");
    } else if (width < 1000) {
        classes.add("layout-medium");
    } else {
        classes.add("layout-large");
    }
});
```

```css
/* 小屏布局：单列、紧凑 */
.layout-small .sidebar {
    -fx-pref-width: 0;
    -fx-max-width: 0;
}

.layout-small .content {
    -fx-font-size: 12px;
}

/* 大屏布局：宽侧边栏 */
.layout-large .sidebar {
    -fx-pref-width: 250;
}

.layout-large .content {
    -fx-font-size: 14px;
}
```

### 6.3 使用 Stage 最大化/全屏监听

```java
stage.maximizedProperty().addListener((obs, old, isMax) -> {
    root.setStyle(isMax ? "-fx-font-size: 16px;" : "-fx-font-size: 14px;");
});
```

---

## 七、动画与过渡

JavaFX CSS 支持通过 `Transition` API 实现动画，也可在 CSS 中使用 `-fx-transition` 风格的过渡（需结合代码）。

### 7.1 CSS 中无法直接定义过渡

与 Web CSS 的 `transition` 不同，JavaFX CSS 不支持直接在样式表中定义过渡动画。动画需通过 Java 代码的 `Transition` 类实现。

### 7.2 常用动画代码示例

```java
// 淡入淡出
FadeTransition fade = new FadeTransition(Duration.millis(300), button);
fade.setFromValue(0.0);
fade.setToValue(1.0);
fade.play();

// 缩放过渡（悬停效果）
ScaleTransition scale = new ScaleTransition(Duration.millis(200), button);
scale.setToX(1.05);
scale.setToY(1.05);
scale.setAutoReverse(true);
scale.setCycleCount(2);

button.setOnMouseEntered(e -> {
    scale.setFromX(1.0); scale.setFromY(1.0);
    scale.setToX(1.05); scale.setToY(1.05);
    scale.playFromStart();
});

button.setOnMouseExited(e -> {
    scale.setFromX(1.05); scale.setFromY(1.05);
    scale.setToX(1.0); scale.setToY(1.0);
    scale.playFromStart();
});

// 平移
TranslateTransition move = new TranslateTransition(Duration.millis(500), node);
move.setToX(100);
move.play();

// 旋转
RotateTransition rotate = new RotateTransition(Duration.millis(1000), node);
rotate.setByAngle(360);
rotate.setCycleCount(Animation.INDEFINITE);
rotate.play();
```

### 7.3 组合动画

```java
// 并行动画：同时执行多个动画
ParallelTransition parallel = new ParallelTransition(
    new FadeTransition(Duration.millis(400), node),
    new ScaleTransition(Duration.millis(400), node)
);
parallel.play();

// 顺序动画：依次执行
SequentialTransition sequential = new SequentialTransition(
    new PauseTransition(Duration.millis(500)),
    new FadeTransition(Duration.millis(300), node)
);
sequential.play();
```

### 7.4 通过 CSS 伪类触发动画状态

```css
.card {
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);
}

.card:hover {
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0, 0, 5);
}
```

配合代码中的 `Transition` 实现平滑过渡。

---

## 八、derive() 函数

`derive()` 是 JavaFX CSS 特有的颜色函数，用于基于基准颜色生成更亮或更暗的变体，非常适合构建配色方案。

### 8.1 语法

```css
derive(<color>, <percentage>)
```

- 百分比为正：颜色变亮（向白色混合）。
- 百分比为负：颜色变暗（向黑色混合）。
- 范围：`-100%`（纯黑）到 `100%`（纯白）。

### 8.2 使用示例

```css
.root {
    -fx-base-color: #2196f3;
}

.button {
    -fx-background-color: derive(-fx-base-color, 0%);    /* 原色 */
}

.button:hover {
    -fx-background-color: derive(-fx-base-color, 15%);   /* 变亮 15% */
}

.button:pressed {
    -fx-background-color: derive(-fx-base-color, -20%);  /* 变暗 20% */
}

.button:disabled {
    -fx-background-color: derive(-fx-base-color, 60%);   /* 大幅变亮，呈淡色 */
}
```

### 8.3 ladder() 函数

`ladder()` 根据背景色亮度自动选择前景色，实现自适应文字颜色：

```css
.label {
    /* 根据背景色亮度，在白色和黑色之间选择文字颜色 */
    -fx-text-fill: ladder(-fx-background-color,
        white 49%,
        black 50%);
}
```

含义：当背景亮度低于 49% 时使用白色文字，高于 50% 时使用黑色文字。

---

## 九、Scene Builder 集成

Scene Builder 是官方可视化 FXML 设计工具，支持实时预览 CSS 样式。

### 9.1 加载 CSS 文件

1. 在 Scene Builder 中打开 FXML 文件。
2. 在左侧 **Documents** 面板选择 **Controller** 或选中根节点。
3. 在右侧 **Properties** 面板找到 **Stylesheets** 属性。
4. 点击 `+` 添加 CSS 文件（需先将 CSS 放入 resources 目录）。

### 9.2 实时预览

加载 CSS 后，Scene Builder 会实时渲染样式效果，便于可视化调整。

### 9.3 使用样式类

1. 选中控件。
2. 在 **Properties** 面板找到 **Style Class** 字段。
3. 输入样式类名（如 `button-primary`），多个类用空格分隔。

### 9.4 内联样式调试

在 **Properties** 面板的 **Style** 字段输入内联 CSS 进行快速测试：

```
-fx-background-color: #ff0000; -fx-text-fill: white;
```

### 9.5 推荐工作流

1. 在 Scene Builder 中搭建 FXML 布局并分配样式类名。
2. 在外部编辑器（IDE）中编写 CSS 文件。
3. 回到 Scene Builder 加载 CSS 预览效果。
4. 迭代调整 CSS 与布局。

---

## 十、运行时主题切换

实现亮色 / 暗色主题的动态切换，无需重启应用。

### 10.1 准备主题 CSS 文件

```
resources/
├── css/
│   ├── light-theme.css   # 亮色主题变量
│   ├── dark-theme.css    # 暗色主题变量
│   └── controls.css      # 控件样式（引用变量，与主题无关）
```

### 10.2 主题切换代码

```java
package com.example.theme;

import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;

public class ThemeManager {

    private static final String LIGHT_THEME = "/css/light-theme.css";
    private static final String DARK_THEME = "/css/dark-theme.css";
    private static final String CONTROLS = "/css/controls.css";

    private final Scene scene;
    private boolean darkMode = false;

    public ThemeManager(Scene scene) {
        this.scene = scene;
        // 初始加载亮色主题 + 控件样式
        applyTheme(false);
    }

    /** 切换主题 */
    public void toggleTheme() {
        darkMode = !darkMode;
        applyTheme(darkMode);
    }

    private void applyTheme(boolean dark) {
        scene.getStylesheets().clear();
        // 先加载主题变量文件，再加载控件样式
        scene.getStylesheets().add(
            getClass().getResource(dark ? DARK_THEME : LIGHT_THEME).toExternalForm());
        scene.getStylesheets().add(
            getClass().getResource(CONTROLS).toExternalForm());
    }

    public boolean isDarkMode() {
        return darkMode;
    }
}
```

### 10.3 在 Controller 中绑定切换按钮

```java
public class MainController implements Initializable {

    @FXML private ToggleButton themeToggle;
    private ThemeManager themeManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        themeManager = new ThemeManager(themeToggle.getScene());
        themeToggle.setText("切换暗色主题");
        themeToggle.setOnAction(e -> {
            themeManager.toggleTheme();
            themeToggle.setText(themeManager.isDarkMode()
                ? "切换亮色主题" : "切换暗色主题");
        });
    }
}
```

### 10.4 持久化用户主题偏好

```java
public class ThemeManager {
    private static final String PREF_KEY = "app.theme.dark";

    public ThemeManager(Scene scene) {
        this.scene = scene;
        // 从 Preferences 读取上次选择
        darkMode = Preferences.userNodeForPackage(ThemeManager.class)
            .getBoolean(PREF_KEY, false);
        applyTheme(darkMode);
    }

    public void toggleTheme() {
        darkMode = !darkMode;
        applyTheme(darkMode);
        // 持久化保存
        Preferences.userNodeForPackage(ThemeManager.class)
            .putBoolean(PREF_KEY, darkMode);
    }
}
```

### 10.5 平滑过渡（可选）

切换主题时为根节点添加淡入淡出效果：

```java
private void applyThemeWithTransition(boolean dark) {
    FadeTransition fadeOut = new FadeTransition(Duration.millis(150), scene.getRoot());
    fadeOut.setFromValue(1.0);
    fadeOut.setToValue(0.3);
    fadeOut.setOnFinished(e -> {
        applyTheme(dark);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), scene.getRoot());
        fadeIn.setFromValue(0.3);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    });
    fadeOut.play();
}
```

---

## 十一、CSS 最佳实践总结

| 实践                           | 说明                                                         |
|--------------------------------|--------------------------------------------------------------|
| 使用 CSS 变量管理主题色        | 颜色集中定义在 `.root`，便于统一修改和主题切换。             |
| 避免内联样式                   | 内联样式优先级最高且难以维护，仅在动态调试时使用。           |
| 语义化样式类名                 | 使用 `.button-primary` 而非 `.blue-button`，与视觉解耦。     |
| 分离主题变量与控件样式         | 主题文件只定义变量，控件文件引用变量，实现主题可插拔。       |
| 利用 derive() 构建配色         | 从基准色派生悬停/按下状态色，保持配色一致性。                |
| 避免过度嵌套选择器             | 选择器层级过深降低性能且难以覆盖，保持 2-3 层以内。          |
| 为所有交互状态提供样式         | 包括 `:hover`、`:pressed`、`:focused`、`:disabled`。         |
| 使用 Scene Builder 可视化调试  | 实时预览提升样式开发效率。                                   |
| 注释分区管理 CSS               | 用注释划分区块（按钮、表格、表单等），便于维护。             |
