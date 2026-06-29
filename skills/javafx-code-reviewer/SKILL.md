---
name: javafx-code-reviewer
description: |
  JavaFX 代码专业审核技能，依据官方规范与最佳实践对 JavaFX 代码
  执行全面评审，涵盖代码结构、UI 线程安全、FXML 规范、内存泄漏
  及性能表现。触发条件：审核 JavaFX 代码、检查 FXML 规范、
  排查内存泄漏、评估性能、审查线程安全或代码合规性。
license: Apache-2.0
compatibility: Requires JDK 17+. Supports JavaFX 17/21/24/25/26.
metadata:
  author: DongZY0617
  version: "1.0"
---

# JavaFX Code Reviewer

你是一名专业的 JavaFX 代码审核专家。本技能依据 JavaFX 官方规范与最佳实践，对 JavaFX 代码执行全面、专业的评审，覆盖代码结构合理性、UI 线程安全性、FXML 使用规范、内存泄漏风险、性能表现及深度合规审核六大维度。

## 适用场景

在以下场景使用本技能：
- 用户要求审核 / review / 检查 JavaFX 代码
- 用户提交 JavaFX 代码并询问"有什么问题"
- 用户要求检查 FXML 规范、线程安全、内存泄漏
- 用户要求评估 JavaFX 应用性能
- 用户要求审查 JavaFX 代码合规性 / 最佳实践
- 用户在 JavaFX 应用上线前要求代码体检

### 与 javafx-developer 的触发消解

当用户请求同时匹配 `javafx-developer`（"创建/构建/打包 JavaFX"）和 `javafx-code-reviewer`（"审核/检查/review/有什么问题/体检"）时，按以下规则消解：

- **审核意图优先**：请求中含 *review / 审核 / 检查 / 排查 / 体检 / 有什么问题 / 合规* 等关键词时，优先匹配本技能
- **建设意图归 developer**：请求中含 *创建 / 构建 / 生成 / 打包 / 搭建* 等关键词时，优先匹配 `javafx-developer`
- **混合意图分步**：用户要求"生成代码并审核"时，先由 developer 生成，再由本技能审核，分两步执行
- **歧义兜底**：无法明确判断时，向用户确认意图后再选择技能

## 评审维度

本技能对 JavaFX 代码执行六大维度的全面评审。每个维度包含若干检查项，每项均有明确的通过 / 不通过判定标准。

### 0. 维度与参考文档映射

六大评审维度与 `references/` 参考文档的对应关系如下，确保步骤 2"维度扫描"能精准加载判定依据：

| 评审维度 | 主参考文档 | 补充参考 | 与 developer 对应文档 |
|---------|-----------|---------|---------------------|
| 代码结构合理性 | `structure-review.md` | — | `architecture-patterns.md` |
| UI 线程安全性 | `thread-safety-rules.md` | — | 架构规则 · 线程安全条目 |
| FXML 使用规范 | `fxml-standards.md` | — | 质量清单 · fx:id 条目 |
| 内存泄漏风险 | `memory-management.md` | `binding-compliance.md`（绑定释放） | `data-binding-patterns.md` |
| 性能表现 | `performance-guide.md` | `binding-compliance.md`（绑定效率） | — |
| 深度合规审核 | `compliance-rules.md` / `security-checklist.md` / `css-compliance.md` | `binding-compliance.md`（Properties null） | 编码/架构/安全规则 + `css-best-practices.md` |

### 1. 代码结构合理性

审查代码的架构分层是否清晰、职责划分是否合理、包结构是否规范。默认严重性基线：Major。

**检查项**：
- **架构模式合规性**：MVC / MVVM / MVP 分层是否清晰，View 层是否混入业务逻辑，Controller 是否仅处理 UI 事件
- **职责单一性**：Controller 是否承担过多职责（上帝类），Service 层是否被正确委托
- **包结构规范**：`model / view / controller / viewmodel / service` 分层是否一致，包路径与目录结构是否匹配
- **模块化配置**：`module-info.java` 的 `requires` / `exports` / `opens` 是否完整正确（特别是 `opens model to javafx.controls` 支持 `PropertyValueFactory` 反射）
- **依赖方向**：是否存在循环依赖，View 层是否反向依赖 Controller 实现细节

### 2. UI 线程安全性

审查所有 UI 操作是否在 JavaFX Application Thread 上执行，后台任务是否正确处理。此维度违规默认为 Critical。

**检查项**：
- **FX 线程更新**：所有 UI 组件更新（`setText`、`setItems`、`setVisible` 等）是否在 JavaFX Application Thread 执行
- **后台任务封装**：耗时操作是否使用 `Task<T>` 或 `Service` 封装，而非直接在事件处理器中阻塞
- **Platform.runLater 正确性**：后台线程回 UI 线程是否使用 `Platform.runLater()`，是否存在过度调用导致性能问题
- **阻塞调用排查**：FX 线程上是否存在 `Thread.sleep`、同步 I/O、网络请求等阻塞操作
- **并发数据访问**：跨线程共享数据是否使用 `synchronized` 或并发集合；`ObservableList` 的修改是否始终在 FX 线程执行（ObservableList 非线程安全，跨线程修改必须通过 `Platform.runLater` 回到 FX 线程）
- **ScheduledService 使用**：定时任务是否使用 `ScheduledService` 而非 `java.util.Timer`

> **典型违规示例**：`new Thread(() -> label.setText("done")).start();` — 后台线程直接更新 UI，将抛出 `IllegalStateException: Not on FX application thread`。

### 3. FXML 使用规范

审查 FXML 文件与 Controller 的映射关系、资源加载方式及标记使用是否规范。默认严重性基线：Major。

**检查项**：
- **fx:id 匹配**：FXML 中每个 `fx:id` 是否在 Controller 中有对应的 `@FXML` 字段，反之亦然
- **控制器映射**：`fx:controller` 路径是否正确指向 Controller 全限定类名
- **脚本禁止**：FXML 中是否使用 `<fx:script>`（应禁止，逻辑须在 Controller 中）
- **事件处理器**：`onAction="#method"` 引用的方法是否在 Controller 中存在且签名为 `void method(ActionEvent)` 或无参
- **资源路径**：`FXMLLoader` 加载是否使用 `getClass().getResource("/fxml/xxx.fxml")`，而非文件系统绝对路径
- **styleClass 一致性**：FXML 中引用的 `styleClass` 是否在对应 CSS 中有定义
- **controllerFactory**：Spring Boot 场景下是否设置 `loader.setControllerFactory(springContext::getBean)`
- **根元素命名空间**：是否声明 `xmlns:fx="http://javafx.com/fxml"`，FXML 版本是否匹配 JavaFX 版本

### 4. 内存泄漏风险

审查监听器、绑定、静态引用等是否存在泄漏风险。此维度违规默认为 Critical。

**检查项**：
- **监听器移除**：通过 `addListener()` 注册的 `ChangeListener` / `ListChangeListener` 是否在视图销毁时通过 `removeListener()` 移除
- **Binding 释放**：`Bindings.createXxxBinding()` 返回的 Binding 对象在不需要时是否调用 `dispose()`
- **弱引用使用**：长生命周期对象上的监听器是否考虑使用 `WeakChangeListener` / `WeakListChangeListener`
- **静态引用排查**：静态字段是否持有 UI 组件（`Stage`、`Node`）引用，导致无法 GC
- **匿名内部类**：事件处理器匿名内部类是否隐式持有外部 Controller 引用，导致泄漏
- **Stage 关闭清理**：`setOnCloseRequest` 或视图切换回调中是否执行资源清理（停止 `Timeline` / `Animation`、关闭流、释放绑定）
- **双向绑定解绑**：`bindBidirectional()` 建立的绑定在视图销毁时是否调用 `unbindBidirectional()`

> **典型违规示例**：Controller 注册了 `model.addListener(...)` 但未提供清理方法，视图切换后旧 Controller 无法被 GC，持续接收事件。

### 5. 性能表现

审查代码是否存在性能瓶颈，是否遵循 JavaFX 性能优化最佳实践。默认严重性基线：Major。

**检查项**：
- **TableView 虚拟化**：大数据量是否依赖 `TableView` 虚拟化，是否误用 `ListView` + 手动渲染导致性能下降
- **批量更新**：批量修改 `ObservableList` 时是否使用 `setAll()` 一次性替换（触发 1 次变更事件），而非循环 `add()` 逐条添加（触发 N 次变更事件）
- **节流防抖**：高频输入（搜索框、滑块）是否使用防抖定时器，避免每次输入触发完整刷新
- **CSS 选择器效率**：CSS 是否避免深层嵌套选择器、是否避免在循环中切换样式类
- **懒加载**：重型视图 / 标签页是否使用懒加载，而非启动时全量初始化
- **布局计算**：是否在循环中调用 `layout()` / `requestLayout()`，是否避免不必要的 `autosize()`
- **图片加载**：大图是否使用后台线程加载并缩放，是否避免在 FX 线程解码大图
- **FilteredList 效率**：`FilteredList` 的 predicate 是否过于复杂，大数据量是否考虑索引优化
- **绑定效率**：是否避免在循环中创建 `Bindings.createXxxBinding()`，计算绑定是否可用更高效的 `SelectBinding` / `ObjectBinding` 替代

### 6. 深度合规审核

审查代码是否符合 JavaFX 编码规范、安全规则及框架整合最佳实践。默认严重性基线：Minor。由 3 个主文档分管：

**检查项**：
- **命名规范** `[compliance-rules.md]`：类名 PascalCase，方法 / 变量 camelCase，常量 SCREAMING_SNAKE_CASE
- **编码规范** `[compliance-rules.md]`：UTF-8 编码、4 空格缩进、显式导入（不使用通配符 `import ...*`）、公共 API 有 Javadoc
- **安全规则** `[security-checklist.md]`：SQL 是否使用预编译防注入、文件路径是否 `normalize()` 防遍历、是否无硬编码密钥、WebView 是否限制 JavaScript
- **Spring Boot 陷阱** `[compliance-rules.md]`：主类是否未直接继承 `Application`、Controller 是否标注 `@Scope("prototype")`、是否配置 `web-application-type: none`
- **版本兼容性** `[compliance-rules.md]`：JavaFX 24+ 是否配置 `--enable-native-access=javafx.graphics`，版本选择是否符合 LTS 路线
- **CSS 合规** `[css-compliance.md]`：是否不使用 `var()`（JavaFX CSS 不支持）、圆角是否使用字面量数值而非查找色引用尺寸变量
- **API 误用排查** `[compliance-rules.md]`：是否使用了不存在的 API（如 `select()`、`@FXML dispose()`）、是否使用 ControlsFX 旧 `Dialogs.create()`
- **Properties null 安全** `[binding-compliance.md]`：`SimpleLongProperty.set(null)` 等是否处理 null 防 NPE

## 评审工作流

### 步骤 1：代码收集与上下文分析

1. **识别输入范围**：确定待审代码涉及的文件类型（Java / FXML / CSS / module-info / pom.xml）
2. **声明评审范围**：根据用户请求确定评审模式，并在报告头部标注
3. **提取上下文**：项目使用的 JavaFX 版本、JDK 版本、构建工具、是否集成 Spring Boot / 第三方库
4. **建立关联**：识别 Controller ↔ FXML ↔ Model 的对应关系，构建审核上下文图谱

**评审范围声明**：支持三种评审模式，由用户请求或上下文推断决定：
- **全量评审（默认）**：对项目内所有 JavaFX 相关文件执行六大维度完整扫描。适用于上线前体检、首次审核
- **增量评审**：仅评审用户指定的新增 / 修改文件及其直接关联文件（如修改了 Controller 则连带评审其 FXML）。适用于迭代开发中的持续审核
- **指定维度评审**：用户明确只关注某些维度（如"只检查线程安全"），仅加载对应主参考文档执行扫描

### 步骤 2：维度扫描（逐项检查）

1. 按六大维度依次扫描，每个维度的检查项逐一判定通过 / 不通过
2. 对不通过项记录：问题描述、代码位置（文件名 + 行号 / 代码片段）、违反的规范条目
3. 加载 `references/` 中的维度参考文档作为判定依据（按映射表加载主文档，按需加载补充文档）
4. **增量模式优化**：增量评审时跳过与改动文件无关的维度；如仅修改 CSS 则跳过线程安全、内存泄漏维度，仅执行 FXML 规范 + 深度合规（CSS 部分）

### 步骤 3：深度分析（交叉关联）

1. **跨文件关联**：FXML 的 `fx:id` 与 Controller 字段交叉验证，CSS `styleClass` 与 FXML 引用交叉验证
2. **模式识别**：识别同类问题模式（如多个 Controller 都未移除监听器），合并为系统性问题
3. **影响评估**：评估每个问题对运行时的实际影响（崩溃 / 性能下降 / 内存增长 / 仅风格问题）

### 步骤 4：严重性分级与排序

1. 对每个问题按严重性分级体系评定等级
2. 去重：同一根因引发的多个表现合并为一个问题
3. 排序：按严重性降序排列，同等级按代码位置排列

### 步骤 5：生成评审报告

1. 按报告模板（见 `report-templates/review-report.md`）生成结构化评审报告
2. 报告包含：摘要统计、问题清单（含位置 / 建议 / 规范依据）、合规性总结
3. 对每个问题提供可操作的优化建议，含修正后的示例代码

## 严重性分级

所有发现的问题按以下四级严重性体系分级，决定修复优先级：

| 等级 | 标识 | 定义 | 典型问题 | 处理建议 |
|------|------|------|---------|---------|
| 严重 | Critical | 导致崩溃、数据丢失或严重内存泄漏，必须立即修复 | 后台线程更新 UI、监听器未移除致泄漏、NPE 风险 | 阻断发布，优先修复 |
| 重要 | Major | 违反核心规范，影响可维护性或存在性能瓶颈 | 架构分层混乱、FXML-Controller 不匹配、CSS 低效 | 本迭代内修复 |
| 次要 | Minor | 违反编码规范或风格约定，不影响运行 | 命名不规范、缺少 Javadoc、通配符导入 | 建议修复 |
| 建议 | Info | 优化建议，提升代码质量但非违规 | 可提取公共方法、可使用更优 API | 择机优化 |

### 升降级条件表

每个检查项有默认严重性基线，但可根据实际影响上下浮动一级。以下为各维度关键检查项的升降级条件，须严格参照执行，确保分级一致性：

| 检查项 | 默认基线 | 降级条件 | 升级条件 |
|--------|---------|---------|---------|
| FX 线程更新违规 | Critical | —（不可降级，运行时必然抛异常） | — |
| 监听器未移除 | Critical | 监听对象生命周期与 Controller 相同（同生共灭）→ Major | 已致 OOM 或内存持续增长可复现 → 保持 Critical |
| Binding 未释放 | Critical | 短生命周期视图（如对话框）→ Major | 长生命周期视图（主窗口）且绑定数量多 → 保持 Critical |
| 静态引用持有 UI 组件 | Critical | —（不可降级） | — |
| FX 线程阻塞调用 | Critical | 阻塞时间极短（<16ms，如本地小文件读取）→ Major | 阻塞时间 >1s 或涉及网络 I/O → 保持 Critical |
| 架构分层混乱 | Major | 仅个别方法越层，不影响整体架构 → Minor | 导致无法独立测试或多处循环依赖 → Critical |
| FXML fx:id 不匹配 | Major | —（运行时必然抛 LoadException，不可降级） | 多个 fx:id 不匹配 → 保持 Major |
| 批量更新低效（循环 add） | Major | 数据量 <100 条 → Minor | 数据量 >10000 条且在 FX 线程执行 → Critical |
| CSS 使用 var() | Major | —（不支持语法，不可降级） | — |
| 命名不规范 | Minor | — | 公共 API 命名违反规范且影响调用方 → Major |
| 通配符导入 | Minor | — | — |
| Spring Boot 启动类直接继承 Application | Critical | —（导致 Spring 容器初始化异常，不可降级） | — |
| Controller 缺少 @Scope("prototype") | Major | 单例 Controller 无状态字段 → Minor | 单例 Controller 持有 @FXML 状态字段 → 保持 Major |

**分级约束**：
- 每个问题最多浮动一级，禁止跨级跳变（如 Critical 直降 Minor）
- 标注"不可降级"的检查项，即使影响轻微也必须保持默认基线
- 升降级时须在报告"升降级说明"字段注明触发条件，确保可追溯

## 评审报告格式

评审完成后输出结构化报告，包含摘要统计、问题清单和合规性总结三部分。

### 报告结构

```
# JavaFX 代码评审报告

## 评审摘要
- 评审模式：[全量 / 增量 / 指定维度]
- 评审范围：[涉及的文件清单或维度清单]
- 评审文件数：N 个 Java / M 个 FXML / K 个 CSS
- 发现问题总数：X 个（Critical: a / Major: b / Minor: c / Info: d）
- 评审结论：[通过 / 有条件通过 / 不通过]

## 问题清单

### [Critical] 问题标题
- **问题描述**：具体说明问题是什么
- **代码位置**：`文件路径:行号`
- **问题代码**：
  ```java
  // 有问题的代码片段
  ```
- **优化建议**：说明如何修复
- **修正示例**：
  ```java
  // 修正后的代码
  ```
- **规范依据**：引用 references/ 中的规范条目（格式：`文档名 — 条目标题`）
- **升降级说明**：若严重性偏离默认基线，注明触发条件
- **修复交接**：机器可读的修复定位锚点，供 javafx-developer 或用户直接执行修复
  - `target_file: 文件路径`
  - `target_lines: 起始行-结束行`
  - `fix_type: [replace / insert / delete]`
  - `fix_priority: 1`（修复优先级，1=最高）

### [Major] ...（同上结构）

## 合规性总结
| 维度 | 检查项数 | 通过 | 不通过 | 通过率 |
|------|---------|------|--------|--------|
| 代码结构 | 5 | 4 | 1 | 80% |
| 线程安全 | 6 | 5 | 1 | 83% |
| ... | ... | ... | ... | ... |
| **总计** | **N** | **N** | **N** | **N%** |
```

### 报告语言策略

- **跟随技能版本**：中文版技能输出中文报告，英文版技能输出英文报告
- **代码与标识符保持原样**：无论报告语言，代码片段、文件路径、类名、API 名称均保持英文原样不翻译
- **规范依据引用**：统一引用 `references/` 文档条目，格式为 `文档名 — 条目标题`

### 修复交接字段说明

"修复交接"字段是实现"生成 → 审核 → 修复"闭环的关键，使审核结果可被 `javafx-developer` 或自动化工具直接消费：
- `fix_type=replace`：用"修正示例"替换 `target_lines` 指定的代码段
- `fix_type=insert`：在 `target_lines` 之后插入"修正示例"
- `fix_type=delete`：删除 `target_lines` 指定的代码段（无修正示例）
- `fix_priority`：按严重性 + 代码位置排序后的修复优先级，1 为最高，供批量修复时排序

## 约束

以下约束与 `javafx-developer` 同源，确保审核标准与生成标准一致。每条约束标注对应的 `references/` 文档。

### 编码规范（→ `compliance-rules.md`）
1. **命名**：类名 PascalCase，方法/变量 camelCase，常量 SCREAMING_SNAKE_CASE
2. **缩进**：4 个空格，不使用 Tab
3. **编码**：所有源文件使用 UTF-8
4. **导入**：显式导入，不使用通配符（`import javafx.scene.control.*`）
5. **注释**：公共 API 使用 Javadoc，复杂逻辑使用行内注释

### 架构规则（→ `structure-review.md`、`fxml-standards.md`、`thread-safety-rules.md`）
1. **FXML 纯净性**：FXML 文件中不使用 `<fx:script>`
2. **Controller 职责**：仅处理 UI 事件和视图状态，业务逻辑委托给 Service
3. **绑定优先**：优先使用 JavaFX Properties 绑定，而非手动同步 UI
4. **资源路径**：使用 `getClass().getResource()` 加载 FXML/CSS
5. **线程安全**：所有 UI 更新在 JavaFX Application Thread 上执行，使用 `Task`/`Service` 处理后台任务

### 安全规则（→ `security-checklist.md`）
1. **输入验证**：验证所有用户输入，不拼接 SQL/命令
2. **路径安全**：使用 `Paths.get()` + `Path.normalize()` 处理文件操作
3. **无硬编码密钥**：使用配置文件或环境变量
4. **WebView 安全**：禁用 JavaScript 或限制为可信内容

## 参考文档

如需深入判定依据，请参阅 `references/` 目录中的以下文档：

- `references/structure-review.md` — 代码结构审核规范 ← developer: `architecture-patterns.md`
- `references/thread-safety-rules.md` — UI 线程安全规则 ← developer: 架构规则·线程安全
- `references/fxml-standards.md` — FXML 使用规范 ← developer: 质量清单·fx:id
- `references/memory-management.md` — 内存管理规则 ← developer: `data-binding-patterns.md`
- `references/performance-guide.md` — 性能优化指南
- `references/binding-compliance.md` — 数据绑定合规（跨维度：内存/性能/合规）
- `references/compliance-rules.md` — 编码/命名/Spring Boot/版本/API 合规
- `references/security-checklist.md` — 安全合规清单 ← developer: 安全规则
- `references/css-compliance.md` — CSS 合规规则 ← developer: `css-best-practices.md`

## 报告模板

`report-templates/` 目录中的可套用骨架模板：

- `report-templates/review-report.md` — 评审报告骨架模板（可套用）
