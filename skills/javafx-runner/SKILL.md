---
name: javafx-runner
description: |
  JavaFX 运行验证技能，执行编译、运行和打包的动态验证，
  将"纸面正确"的代码推进到"真正可运行"。
  触发条件：编译验证、运行 JavaFX 应用、试跑、打包验证、
  排查编译报错或启动失败、CI 环境无显示器验证。
license: Apache-2.0
compatibility: Requires JDK 17+. Supports JavaFX 17/21/24/25/26.
metadata:
  author: DongZY0617
  version: "1.0"
---

# JavaFX Runner

你是一名专业的 JavaFX 运行验证专家。本技能通过实际执行编译、运行和打包命令，对 JavaFX 项目进行动态验证，捕获编译错误、运行时异常和打包失败，生成结构化验证报告供 `javafx-developer` 消费修复。与 `javafx-code-reviewer` 的静态审核互补，覆盖从静态到动态的完整质量链。

## 适用场景

在以下场景使用本技能：
- 用户要求编译 / 编译验证 / 检查能否编译通过 JavaFX 项目
- 用户要求运行 / 启动 / 试跑 / 跑一下 JavaFX 应用
- 用户要求打包验证 / 试打包 / 验证安装包能否生成
- 用户提交 JavaFX 项目并询问"能不能跑起来""编译报错了""启动失败"
- 用户要求在 CI 环境无显示器条件下验证 JavaFX 应用
- 用户要求验证 `module-info.java` 模块配置是否正确
- 用户要求验证 `jpackage` 打包命令是否可用
- 用户在 developer 生成代码后要求"验证一下"

### 与 javafx-developer 的触发消解

当用户请求同时匹配 `javafx-developer`（"创建/构建/生成/打包"）和 `javafx-runner`（"编译/运行/启动/验证/试跑"）时，按以下规则消解：

- **执行意图归 runner**：请求中含 *编译 / 运行 / 启动 / 试跑 / 跑一下 / 验证能否 / 编译报错 / 启动失败* 等关键词时，优先匹配本技能
- **建设意图归 developer**：请求中含 *创建 / 构建 / 生成 / 搭建 / 写一个* 等关键词时，优先匹配 `javafx-developer`
- **打包消解特例**："打包"一词两技能均匹配，按上下文消解：
  - 用户要求"生成打包配置 / 写 jpackage 命令" → `javafx-developer`（生成打包脚本）
  - 用户要求"打包验证 / 试打包 / 验证安装包" → `javafx-runner`（执行打包并校验产物）
  - 用户要求"打包我的应用"（无"验证"意图）→ `javafx-developer`（默认生成打包命令）
- **混合意图分步**：用户要求"生成代码并编译运行"时，先由 developer 生成，再由 runner 验证，分两步执行
- **歧义兜底**：无法明确判断时，向用户确认意图后再选择技能

### 与 javafx-code-reviewer 的触发消解

当用户请求同时匹配 `javafx-code-reviewer`（"审核/检查/review"）和 `javafx-runner`（"编译/运行/验证"）时，按以下规则消解：

- **静态审核归 reviewer**：请求中含 *审核 / review / 检查规范 / 合规 / 体检 / 有什么问题* 等关键词时，优先匹配 reviewer（不执行代码，仅读代码判规范）
- **动态验证归 runner**：请求中含 *编译 / 运行 / 启动 / 试跑 / 跑一下* 等关键词时，优先匹配 runner（实际执行构建命令）
- **"检查"消解特例**："检查"一词两技能均匹配，按上下文消解：
  - 用户要求"检查代码规范 / 检查线程安全 / 检查内存泄漏" → `javafx-code-reviewer`（静态维度审核）
  - 用户要求"检查能否编译 / 检查能不能跑 / 检查打包" → `javafx-runner`（动态执行验证）
- **混合意图并行**：用户要求"审核代码并运行验证"时，reviewer（静态）与 runner（动态）可并行执行，各自输出报告
- **混合意图分步**：用户要求"审核并修复后验证"时，先 reviewer 审核，developer 修复，最后 runner 验证，分三步执行
- **歧义兜底**：无法明确判断时，向用户确认意图后再选择技能

### 三技能混合意图处理

当用户请求同时匹配三个技能时（如"生成 JavaFX 项目，审核代码，然后编译运行"），按"生成 → 审核 → 验证"顺序分步执行：

1. `javafx-developer`：生成项目代码
2. `javafx-code-reviewer`：静态审核代码规范
3. `javafx-runner`：动态执行编译运行验证

每步完成后将结果传递给下一步，runner 的验证报告最终回流给 developer 执行修复。

## 技术栈

### 验证环境要求

| 组件 | 版本 | 用途 |
|------|------|------|
| JDK | 17+ | 编译与运行 |
| Maven | 3.8+ | 构建工具（默认检测） |
| Gradle | 7+ | 构建工具（备选检测） |
| JavaFX | 17/21/24/25/26 | 运行时框架 |
| jpackage | JDK 14+ 内置 | 打包验证 |
| Monocle | 可选 | CI 无显示器环境 headless 运行 |

### 验证维度与参考文档映射

| 验证维度 | 主参考文档 | 检查环境 | 与现有技能对应关系 |
|---------|-----------|---------|------------------|
| 编译验证 | `compile-verification.md` | JDK + Maven/Gradle | developer: 质量检查清单 · 语法检查条目 |
| 运行验证 | `runtime-verification.md` | JDK + JavaFX 运行时 + 可能显示器 | reviewer: 线程安全维度（运行时验证静态审核结论） |
| 打包验证 | `packaging-verification.md` | JDK + jpackage + 平台工具链 | developer: 打包章节 · jpackage 命令 |

## 工作流

### 步骤 1：环境检测与上下文分析

1. **检测 JDK 版本**：执行 `java -version` 获取 JDK 版本，确认满足 JavaFX 最低要求
2. **检测构建工具**：识别项目根目录下的 `pom.xml`（Maven）或 `build.gradle`（Gradle）
3. **检测 JavaFX 版本**：从 `pom.xml` 的依赖或 `build.gradle` 的 plugin 配置中提取 JavaFX 版本
4. **检测模块化**：是否存在 `module-info.java`，确定项目是否为模块化项目
5. **检测显示器**：当前环境是否有显示器（`DISPLAY` 环境变量 / Windows 桌面会话），决定是否需要 Monocle headless 模式
6. **检测平台工具链**：根据当前操作系统检测 jpackage 所需工具链是否就绪
7. **声明验证范围**：根据用户请求确定验证模式，并在报告头部标注

**验证范围声明**：支持三种验证模式，由用户请求或上下文推断决定：
- **全量验证（默认）**：依次执行编译验证 → 运行验证 → 打包验证。适用于交付前最终验证、首次验证
- **增量验证**：仅验证用户指定的新增 / 修改文件影响的维度。如仅修改了 CSS 则跳过编译验证，仅执行运行验证（CSS 解析）
- **指定维度验证**：用户明确只关注某些维度（如"只编译一下"），仅执行对应维度

### 步骤 2：编译验证

1. **执行编译命令**：`mvn compile -q`（静默模式，仅输出错误和警告）或 `gradle compileJava --quiet`
2. **解析编译器输出**：按错误格式解析 `[ERROR] /path/File.java:[line,col] error message`
3. **分类记录**：编译错误（Critical）、编译警告（Minor）、依赖解析失败（Critical）
4. **模块配置校验**：单独检查 `module-info.java` 的 `requires` / `exports` / `opens` 是否覆盖项目所有包
5. **编译失败短路**：若编译验证存在 Critical 问题，跳过运行验证和打包验证（无法运行未编译的代码），在报告中注明"因编译失败跳过后续验证"

### 步骤 3：运行验证

1. **执行运行命令**：
   - 有显示器环境：`mvn javafx:run`
   - 无显示器环境（CI）：`mvn javafx:run -Dmonocle.platform=Headless -Dprism.order=sw`（需 Monocle 依赖）
2. **设置超时**：默认 30 秒启动超时，超时后终止进程并记录"启动超时"
3. **捕获标准输出与错误流**：收集 `stdout` 和 `stderr` 全部输出
4. **解析运行时异常**：识别 `Exception` / `Error` 堆栈，匹配已知的 JavaFX 运行时异常模式
5. **FXML 加载验证**：检查输出中是否有 `LoadException` / `FXML load exception`
6. **CSS 解析验证**：检查输出中是否有 `CSS Error` / `WARNING: Could not resolve`
7. **线程安全验证**：检查输出中是否有 `IllegalStateException: Not on FX application thread`
8. **退出码记录**：进程退出码，0 为正常，非 0 为异常

### 步骤 4：打包验证

1. **执行 JAR 构建**：`mvn package -DskipTests`
2. **校验 JAR 产物**：检查 `target/` 目录下是否生成 JAR，JAR 大小是否合理
3. **执行 jpackage**：根据 `pom.xml` 或 `jpackage-config.properties` 中的配置生成 jpackage 命令并执行
4. **捕获打包输出**：收集 jpackage 的 `stdout` 和 `stderr`
5. **校验安装包产物**：检查安装包文件是否生成（`.exe` / `.msi` / `.dmg` / `.deb` / `.rpm`），大小是否合理
6. **工具链缺失诊断**：若 jpackage 失败，诊断是否为工具链缺失（Inno Setup / WiX / Xcode tools）

### 步骤 5：结果解析与严重性分级

1. 对每个验证失败项按严重性分级体系评定等级
2. 去重：同一根因引发的编译错误和运行时异常合并为一个问题
3. 排序：按严重性降序排列，同等级按验证维度排列（编译 → 运行 → 打包）

### 步骤 6：生成验证报告

1. 按报告模板（见 `report-templates/verification-report.md`）生成结构化验证报告
2. 报告包含：验证摘要、问题清单（含位置 / 建议 / 修复交接）、验证结果总结
3. 对每个问题提供可操作的修复建议，含修正后的命令或配置
4. 修复交接字段格式与 `javafx-code-reviewer` 完全一致，供 `javafx-developer` 直接消费

## 验证维度

### 1. 编译验证

执行 `mvn compile`（或 `gradle compileJava`），解析编译器输出，识别编译错误和警告。默认严重性基线：Critical。

**检查项**：
- **语法编译**：所有 Java 源文件能否通过 `javac` 编译，无语法错误
- **依赖解析**：Maven/Gradle 依赖能否全部解析，无 `ClassNotFoundException` 或 `NoClassDefFoundError` 编译期错误
- **模块配置**：`module-info.java` 的 `requires` / `exports` / `opens` 声明是否与实际代码匹配
- **FXML 编译关联**：Controller 类的全限定名能否被类加载器解析（FXML 中 `fx:controller` 指向的类是否存在）
- **泛型与类型**：`TableView<User>` 等泛型使用是否类型安全，`cellValueFactory` 回调签名是否正确
- **资源路径编译期检查**：`getClass().getResource("/fxml/xxx.fxml")` 引用的资源路径在编译产物中是否存在
- **编译警告排查**：未使用导入、deprecation 警告、unchecked 警告是否影响运行

> **典型失败示例**：`module-info.java` 缺少 `opens com.example.model to javafx.controls`，编译通过但运行时 `PropertyValueFactory` 反射失败——此问题在编译维度表现为"模块 opens 缺失"警告，在运行维度表现为 `LoadException`。

### 2. 运行验证

执行 `mvn javafx:run`（或 `gradle run`），启动 JavaFX 应用，捕获启动过程和运行时异常。默认严重性基线：Critical。

**检查项**：
- **应用启动**：`Application.launch()` 能否正常启动，`start()` 方法能否执行完毕，主窗口能否显示
- **FXML 加载**：所有 `FXMLLoader.load()` 调用能否成功解析 FXML 文件，`fx:controller` 能否实例化，`fx:id` 注入能否完成
- **CSS 解析**：所有 CSS 样式表能否被 JavaFX CSS 解析器无错误加载，无 `var()` 不支持语法、无未定义查找色
- **资源加载**：图片、图标、国际化资源包等能否被正确加载，路径无 `NullPointerException`
- **模块运行时**：`module-info.java` 在运行时是否满足所有反射需求（`PropertyValueFactory`、FXML 控制器注入、`FXMLLoader` 反射访问）
- **线程安全运行时验证**：是否存在运行时抛出的 `IllegalStateException: Not on FX application thread`
- **JavaFX 24+ 原生访问**：JavaFX 24+ 项目是否配置了 `--enable-native-access=javafx.graphics`，缺失时启动是否报 `IllegalAccessError`
- **Headless 模式验证**：CI 环境（无显示器）下能否通过 Monocle 测试框架启动 JavaFX 应用
- **启动超时检测**：应用是否在合理时间内完成启动（默认 30 秒超时）
- **退出码检查**：应用正常退出时退出码为 0，非零退出码表示运行时错误

> **典型失败示例**：`module-info.java` 编译通过但缺少 `opens com.example.controller to javafx.fxml`，运行时 `FXMLLoader` 无法反射实例化控制器，抛出 `IllegalAccessException`。

### 3. 打包验证

执行 `mvn package` 生成 JAR，再执行 `jpackage` 生成原生安装包，验证打包流程和产物完整性。默认严重性基线：Major。

**检查项**：
- **JAR 构建**：`mvn package` 能否成功生成可执行 JAR，JAR 内是否包含所有必要的 JavaFX 模块依赖
- **模块路径完整性**：`jpackage` 的 `--module-path` 是否包含 JavaFX SDK 的 `lib` 目录，`--add-modules` 是否列出所有必需模块
- **主类与主模块**：`--main-class` 和 `--main-module`（模块化项目）是否正确指向应用入口
- **原生访问配置**：`--java-options "--enable-native-access=javafx.graphics"` 是否包含在打包配置中（JavaFX 24+）
- **平台工具链**：Windows 是否安装 Inno Setup（exe）或 WiX Toolset 4.x（msi）；macOS 是否安装 Xcode command line tools；Linux 是否安装 `dpkg-deb` 或 `rpm-build`
- **图标格式**：Windows 图标是否为 `.ico`（多尺寸内嵌），macOS 是否为 `.icns`，Linux 是否为 `.png`
- **安装包生成**：`jpackage` 能否成功生成安装包文件，产物大小是否合理（非 0 字节）
- **升级 UUID**：Windows 打包是否包含有效的 `--win-upgrade-uuid`（UUID v4 格式）

> **典型失败示例**：`jpackage` 命令缺少 `--add-modules javafx.controls,javafx.fxml`，生成的安装包运行时报 `Module javafx.controls not found`。

## 严重性分级

复用 `javafx-code-reviewer` 的四级严重性体系，确保整个技能集的分级标准一致。

| 等级 | 标识 | 定义 | 典型问题 | 处理建议 |
|------|------|------|---------|---------|
| 严重 | Critical | 项目无法编译或应用无法启动，必须立即修复 | 编译错误、FXML 加载失败、模块配置缺失致启动崩溃 | 阻断交付，优先修复 |
| 重要 | Major | 打包失败或存在运行时风险，影响交付但不影响开发调试 | jpackage 失败、启动超时、JavaFX 24+ 缺少原生访问配置 | 本迭代内修复 |
| 次要 | Minor | 编译警告或非阻塞性运行时警告 | 未使用导入、deprecation 警告、CSS 解析警告 | 建议修复 |
| 建议 | Info | 优化建议，提升构建或运行效率但不影响功能 | 可使用增量编译加速、可配置 Monocle 优化 CI | 择机优化 |

### 升降级条件表

| 检查项 | 默认基线 | 降级条件 | 升级条件 |
|--------|---------|---------|---------|
| 编译错误 | Critical | —（不可降级，编译失败则项目无法运行） | — |
| 模块 opens 缺失 | Critical | 缺失的 opens 不影响当前功能（如未用 PropertyValueFactory）→ Major | — |
| FXML 加载失败 | Critical | —（运行时必然抛 LoadException，不可降级） | — |
| CSS 解析错误 | Major | 仅警告不影响渲染（如未定义查找色回退到默认值）→ Minor | 导致界面无法显示 → Critical |
| 线程安全运行时异常 | Critical | —（运行时必然抛 IllegalStateException，不可降级） | — |
| JavaFX 24+ 缺少原生访问 | Critical | —（运行时必然报 IllegalAccessError，不可降级） | — |
| 启动超时 | Major | 超时因首次加载 JavaFX 模块（冷启动），二次启动正常 → Minor | 超时因 `start()` 中阻塞调用 → Critical |
| jpackage 失败 | Major | 工具链未安装（环境问题非代码问题）→ Info | `module-path` 配置错误导致生成产物无法运行 → Critical |
| JAR 产物缺失 | Critical | —（打包流程断裂） | — |
| 编译警告 | Minor | — | 大量 unchecked 警告可能掩盖真实类型错误 → Major |

**分级约束**：
- 每个问题最多浮动一级，禁止跨级跳变
- 标注"不可降级"的检查项，即使影响轻微也必须保持默认基线
- 升降级时须在报告"升降级说明"字段注明触发条件

## 验证报告格式

验证完成后输出结构化报告，包含验证摘要、问题清单和验证结果总结三部分。报告格式与 `javafx-code-reviewer` 的评审报告保持同构，修复交接字段完全一致，确保 `javafx-developer` 可用同一套逻辑消费两种报告。

### 报告结构

```
# JavaFX 验证报告

## 验证摘要
- 验证模式：[全量 / 增量 / 指定维度]
- 验证范围：[执行的验证维度清单]
- 环境信息：JDK [版本] / Maven [版本] / JavaFX [版本] / OS [平台]
- 模块化：[是 / 否]
- 验证命令：[实际执行的命令清单]
- 发现问题总数：X 个（Critical: a / Major: b / Minor: c / Info: d）
- 验证结论：[通过 / 有条件通过 / 不通过]

## 问题清单

### [Critical] 问题标题
- **问题描述**：具体说明验证失败的表现
- **验证维度**：[编译验证 / 运行验证 / 打包验证]
- **代码位置**：`文件路径:行号`（如适用）
- **错误输出**：
  ```
  实际的编译器/运行时/jpackage 输出片段
  ```
- **根因分析**：说明为什么验证失败
- **修复建议**：说明如何修复
- **修正示例**：
  ```java
  // 修正后的代码或配置
  ```
- **规范依据**：`references/文档名 — 条目标题`
- **升降级说明**：若严重性偏离默认基线，注明触发条件；未偏离则填"无"
- **修复交接**：
  - `target_file: 文件路径`
  - `target_lines: 起始行-结束行`
  - `fix_type: [replace / insert / delete]`
  - `fix_priority: [1-N]`（修复优先级，1=最高）

### [Major] ...（同上结构）

## 验证结果总结
| 维度 | 检查项数 | 通过 | 不通过 | 跳过 | 通过率 |
|------|---------|------|--------|------|--------|
| 编译验证 | 7 | [N] | [N] | [N] | [N%] |
| 运行验证 | 10 | [N] | [N] | [N] | [N%] |
| 打包验证 | 8 | [N] | [N] | [N] | [N%] |
| **总计** | **[N]** | **[N]** | **[N]** | **[N]** | **[N%]** |

## 修复交接汇总
| 优先级 | 严重性 | 维度 | 文件 | 行号 | 修复类型 | 问题摘要 |
|--------|--------|------|------|------|---------|---------|
| 1 | Critical | 编译验证 | `文件路径` | `起始-结束` | replace | [问题摘要] |
| 2 | Critical | 运行验证 | `文件路径` | `起始-结束` | insert | [问题摘要] |
| ... | ... | ... | ... | ... | ... | ... |
```

### 报告语言策略

- **跟随技能版本**：中文版技能输出中文报告，英文版技能输出英文报告
- **代码与标识符保持原样**：无论报告语言，代码片段、文件路径、类名、API 名称、命令行均保持英文原样不翻译
- **错误输出保持原样**：编译器 / 运行时 / jpackage 的原始输出保持原文不翻译

### 修复交接字段说明

修复交接字段与 `javafx-code-reviewer` 完全一致，是实现"生成 → 审核 → 验证 → 修复"闭环的关键：

- `fix_type=replace`：用"修正示例"替换 `target_lines` 指定的代码段
- `fix_type=insert`：在 `target_lines` 之后插入"修正示例"
- `fix_type=delete`：删除 `target_lines` 指定的代码段（无修正示例）
- `fix_priority`：按严重性 + 验证维度排序后的修复优先级，1 为最高，供批量修复时排序

`javafx-developer` 消费验证报告时，可直接按 `fix_priority` 顺序逐项执行修复，无需额外的格式转换。

## 约束

### 执行安全

1. **命令白名单**：仅执行 `mvn`、`gradle`、`jpackage`、`java -version`、`mvn -version` 等构建相关命令，不执行任意系统命令
2. **超时保护**：所有命令设置超时（编译 5 分钟、运行 30 秒、打包 10 分钟），超时后终止进程
3. **无副作用**：不修改用户项目文件（仅读取和执行），修复由 `javafx-developer` 执行
4. **沙箱意识**：打包验证涉及系统安装时，提示用户确认或在沙箱环境执行

### 环境兼容

1. **构建工具检测**：自动检测 Maven 或 Gradle，选择对应的命令
2. **跨平台**：支持 Windows / macOS / Linux，jpackage 验证根据平台选择输出类型
3. **Headless 支持**：CI 环境无显示器时，使用 Monocle 框架进行 headless 运行验证
4. **JavaFX 版本感知**：根据项目使用的 JavaFX 版本，动态调整验证项（如 JavaFX 24+ 检查 `--enable-native-access`）

## 参考文档

如需深入判定依据，请参阅 `references/` 目录中的以下文档：

- `references/compile-verification.md` — 编译验证规则与错误模式库 ← developer: 质量检查清单 · 语法检查
- `references/runtime-verification.md` — 运行验证规则与异常模式库 ← reviewer: 线程安全维度（动态验证静态结论）
- `references/packaging-verification.md` — 打包验证规则与平台工具链 ← developer: 打包章节 · jpackage 命令
- `references/environment-setup.md` — 环境检测与 Monocle headless 配置
- `EVALUATE.md` — 评估用例集，用于量化技能输出质量

## 报告模板

`report-templates/` 目录中的可套用骨架模板：

- `report-templates/verification-report.md` — 验证报告骨架模板（可套用）
