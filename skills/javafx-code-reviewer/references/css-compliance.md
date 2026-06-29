# CSS 合规规则

本文档是"深度合规审核"维度中 CSS 合规的判定依据，管辖 3 个检查项：`var()` 禁止、字面量数值规则、查找色使用规则。默认严重性基线：Major。与 `javafx-developer` 的 `css-best-practices.md` 同源。

> **核心差异**：JavaFX CSS 不是 Web CSS。JavaFX CSS 基于 CSS 语法但有诸多限制，最关键的是不支持 `var()` 函数。JavaFX 通过"查找色（looked-up color）"机制实现变量功能，在 `.root` 上定义后子节点直接按名引用，无需 `var()` 包裹。

---

## 检查项 1：var() 禁止规则

**关注点**：是否不使用 `var()`（JavaFX CSS 不支持）。

**通过判定标准**：
- CSS 文件中不出现任何 `var()` 函数调用
- 颜色变量通过查找色机制使用：`.root` 中定义 `-fx-xxx-color`，子节点直接按名引用（如 `-fx-background-color: -fx-primary-color;`）
- 尺寸值使用字面量数值（如 `-fx-background-radius: 8;`），不通过 `var()` 引用

**不通过判定标准**（任一即不通过）：
- CSS 中使用 `var(-fx-primary-color)` 语法（JavaFX CSS 不支持，样式不生效）
- CSS 中使用 `var(-fx-radius)` 引用尺寸变量
- 将 Web CSS 的 `var()` 习惯带入 JavaFX CSS

**严重性基线**：Major（不支持语法，样式不生效，不可降级）

> **关键事实**：JavaFX CSS 解析器不识别 `var()` 函数。使用 `var()` 的属性声明会被静默忽略，对应的样式不会生效。这是从 Web CSS 迁移到 JavaFX CSS 时最常见的错误。

**反例**：
```css
/* ❌ 使用 var()，JavaFX CSS 不支持，样式不生效 */
.root {
    -fx-primary-color: #2196f3;
    -fx-radius: 8;
}
.button-primary {
    -fx-background-color: var(-fx-primary-color);      /* 不生效 */
    -fx-background-radius: var(-fx-radius);             /* 不生效 */
    -fx-text-fill: var(-fx-text-color, #333333);        /* 不支持 fallback 语法 */
}
```

**正例**：
```css
/* ✅ 直接引用查找色，无需 var() 包裹 */
.root {
    -fx-primary-color: #2196f3;
    -fx-text-color: #333333;
}
.button-primary {
    -fx-background-color: -fx-primary-color;   /* 直接按名引用 */
    -fx-background-radius: 8;                   /* 字面量数值 */
    -fx-text-fill: -fx-text-color;              /* 直接按名引用 */
}
```

---

## 检查项 2：字面量数值规则

**关注点**：圆角等尺寸属性是否使用字面量数值，而非查找色引用尺寸变量。

**通过判定标准**：
- `-fx-background-radius`、`-fx-border-radius`、`-fx-padding` 等尺寸属性使用字面量数值（如 `8`、`4px`、`10 5 10 5`）
- 尺寸值不通过查找色变量引用（查找色主要用于颜色值）
- 字面量数值在多处使用时保持一致，或通过 CSS 注释说明约定

**不通过判定标准**（任一即不通过）：
- `-fx-background-radius: -fx-radius;`（通过查找色引用尺寸变量，JavaFX 中不可靠）
- `-fx-padding: -fx-spacing;`（尺寸属性引用查找色变量）
- 尺寸属性值使用 `var()` 引用（同时违反检查项 1）

**严重性基线**：Major
- 降级条件：仅个别尺寸属性误用查找色引用，不影响整体布局 → Minor

> **关键事实**：查找色（looked-up color）机制在 JavaFX 中主要用于**颜色**值。将查找色直接用于 `-fx-background-radius`、`-fx-border-radius` 等尺寸属性在 JavaFX 中不可靠，可能不被解析或解析为错误值。尺寸属性应使用字面量数值。

**反例**：
```css
/* ❌ 尺寸属性通过查找色引用，不可靠 */
.root {
    -fx-radius: 8;
    -fx-spacing: 10;
}
.card {
    -fx-background-radius: -fx-radius;    /* 不可靠，可能不生效 */
    -fx-border-radius: -fx-radius;        /* 不可靠 */
    -fx-padding: -fx-spacing;             /* 不可靠 */
}
```

**正例**：
```css
/* ✅ 尺寸属性使用字面量数值 */
.root {
    -fx-primary-color: #2196f3;  /* 查找色仅用于颜色 */
}
.card {
    -fx-background-color: -fx-primary-color;  /* 颜色用查找色 */
    -fx-background-radius: 8;                  /* 尺寸用字面量 */
    -fx-border-radius: 8;                      /* 尺寸用字面量 */
    -fx-padding: 10;                           /* 尺寸用字面量 */
}
```

---

## 检查项 3：查找色使用规则

**关注点**：查找色是否在 `.root` 中定义后由子节点直接按名引用，作用域是否正确。

**通过判定标准**：
- 查找色在 `.root` 中以 `-fx-` 前缀定义（如 `-fx-primary-color: #2196f3;`）
- 子节点直接按名引用查找色（如 `-fx-background-color: -fx-primary-color;`），无需 `var()` 包裹
- 主题切换通过替换 `.root` 上的查找色定义实现（或切换不同 CSS 文件）
- 局部覆盖查找色时在特定节点上重新定义，仅影响该节点及子节点

**不通过判定标准**（任一即不通过）：
- 查找色未在 `.root` 中定义而直接在子节点引用（未定义的查找色回退为默认值）
- 查找色定义不以 `-fx-` 前缀开头（如 `primary-color` 而非 `-fx-primary-color`，可能不被识别）
- 颜色值使用 Web CSS 语法而非 JavaFX 支持的格式（如使用 `rgb()` 但未加空格）
- 主题切换通过逐个修改节点样式实现，而非替换 `.root` 查找色定义

**严重性基线**：Major
- 降级条件：仅个别查找色定义不规范但功能正常 → Minor

**反例**：
```css
/* ❌ 查找色未在 .root 定义就直接引用 */
.button {
    -fx-background-color: -fx-primary-color;  /* -fx-primary-color 未定义，回退默认 */
}

/* ❌ 查找色定义无 -fx- 前缀 */
.root {
    primary-color: #2196f3;  /* 无 -fx- 前缀，可能不被识别为查找色 */
}

/* ❌ 主题切换逐个改节点，而非替换 .root 定义 */
/* JS/Java 中：button1.setStyle("-fx-background-color: #ff0000;"); */
/* button2.setStyle("-fx-background-color: #ff0000;"); */
/* 应改为切换 .root 上的查找色 */
```

**正例**：
```css
/* ✅ 查找色在 .root 中定义，子节点直接引用 */
.root {
    -fx-primary-color: #2196f3;
    -fx-accent-color: #ff9800;
    -fx-bg-color: #ffffff;
    -fx-text-color: #333333;
}

.button-primary {
    -fx-background-color: -fx-primary-color;  /* 直接引用 */
    -fx-text-fill: white;
}

.label-title {
    -fx-text-fill: -fx-text-color;  /* 直接引用 */
}

/* ✅ 主题切换：切换 .root 上的查找色定义 */
/* dark-theme.css */
.root {
    -fx-primary-color: #1565c0;
    -fx-bg-color: #1e1e1e;
    -fx-text-color: #e0e0e0;
}
/* Java 中切换：scene.getStylesheets().setAll("/css/dark-theme.css"); */
```
