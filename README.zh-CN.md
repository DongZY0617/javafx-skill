# JavaFX 技能集

> 面向 AI Agent 的 JavaFX 技能集合 — 涵盖项目开发（`javafx-developer`）、专业代码审核（`javafx-code-reviewer`）与运行验证（`javafx-runner`），从项目搭建到质量保障。

## 这是什么？

这是一个 [Agent Skill](https://agentskills.io) 技能集合，教会 AI Agent 如何构建、审核和验证 JavaFX 桌面应用。它提供三个互补技能，覆盖完整开发生命周期：

- **javafx-developer** — 构建 JavaFX 应用，从项目脚手架到跨平台打包。
- **javafx-code-reviewer** — 依据官方规范与最佳实践审核 JavaFX 代码。
- **javafx-runner** — 通过执行编译、运行和打包命令动态验证 JavaFX 项目。

三者构成闭环：**生成 → 审核 → 验证 → 修复**，确保代码质量从静态分析到运行时验证持续达标。

## 技能说明

### javafx-developer

构建 JavaFX 桌面应用 — 从项目脚手架到跨平台打包。

- **项目搭建**：Maven/Gradle 配置、JavaFX 版本矩阵、JDK 兼容性
- **架构模式**：MVC、MVVM、MVP 完整代码示例、反模式
- **FXML 界面设计**：布局模板、Controller 模式、CSS 主题（亮色/暗色）
- **数据绑定**：Property 类型、绑定模式、表单验证
- **Spring Boot 整合**：启动类拆分、依赖注入机制、MyBatis + SQLite 集成
- **第三方库**：ControlsFX、MaterialFX、RichTextFX、Ikonli 集成指南
- **打包部署**：jpackage、jlink、CI/CD 集成
- **代码模板**：开箱即用的 pom.xml、module-info.java、Controller、Model、FXML、CSS 模板

### javafx-code-reviewer

依据官方规范与最佳实践审核 JavaFX 代码。

- **代码结构审核**：架构分层、职责划分、模块化配置
- **UI 线程安全**：FX 线程更新、后台任务处理、Platform.runLater 正确性
- **FXML 规范**：fx:id 匹配、控制器映射、资源路径、事件绑定
- **内存泄漏风险**：监听器移除、Binding 释放、静态引用、资源清理
- **性能表现**：虚拟化、批量更新、节流防抖、CSS 效率、懒加载
- **深度合规审核**：命名规范、安全规则、Spring Boot 陷阱、版本兼容性
- **结构化报告**：分级问题清单，含严重性等级、代码位置与优化建议

### javafx-runner

通过执行编译、运行和打包命令动态验证 JavaFX 项目。

- **编译验证**：语法编译、依赖解析、模块配置、FXML 控制器解析
- **运行验证**：应用启动、FXML 加载、CSS 解析、模块运行时反射、线程安全运行时验证
- **打包验证**：JAR 构建、模块路径完整性、jpackage 工具链、安装包生成、升级 UUID
- **Headless 支持**：CI 无显示器环境通过 Monocle 框架验证
- **编译失败短路**：编译失败时跳过运行和打包验证，避免无意义执行
- **结构化报告**：验证结果含修复交接字段，与 javafx-code-reviewer 格式一致

## 技能结构

```
javafx-skill/
├── README.md
├── README.zh-CN.md
├── LICENSE
├── .gitignore
└── skills/
    ├── javafx-developer/                  # 开发技能（中文版）
    │   ├── SKILL.md                          # 技能入口（中文）
    │   ├── EVALUATE.md                       # 评估用例集
    │   ├── references/                       # 深度参考文档
    │   │   ├── architecture-patterns.md      # MVC/MVVM/MVP 模式
    │   │   ├── css-best-practices.md         # CSS 主题指南
    │   │   ├── data-binding-patterns.md      # 属性绑定指南
    │   │   ├── packaging-deployment.md       # jpackage/jlink 指南
    │   │   ├── project-setup.md              # Maven/Gradle 配置
    │   │   ├── spring-boot-integration.md    # Spring Boot + JavaFX 指南
    │   │   └── third-party-libraries.md      # 第三方库集成
    │   └── templates/                        # 可复用代码模板
    │       ├── controller/                   # Controller 模板
    │       ├── css/                          # 亮色/暗色主题 CSS
    │       ├── fxml/                         # FXML 布局模板
    │       ├── gradle/                       # Gradle 构建模板
    │       ├── maven/                        # Maven POM + module-info
    │       ├── model/                        # Observable Model 模板
    │       ├── presenter/                    # MVP Presenter/View 模板
    │       ├── service/                      # Service/Repository 模板
    │       ├── viewmodel/                    # ViewModel 模板（MVVM）
    │       └── packaging/                    # jpackage 配置
    ├── javafx-developer-en/               # 开发技能（英文版）
    │   ├── SKILL.md                          # 技能入口（英文）
    │   ├── EVALUATE.md                       # 评估用例集
    │   ├── references/                       # 独立参考文档
    │   └── templates/                        # 独立代码模板
    ├── javafx-code-reviewer/              # 审核技能（中文版）
    │   ├── SKILL.md                          # 技能入口（中文）
    │   ├── EVALUATE.md                       # 评估用例集
    │   ├── references/                       # 审核维度文档
    │   │   ├── structure-review.md           # 代码结构审核规范
    │   │   ├── thread-safety-rules.md        # UI 线程安全规则
    │   │   ├── fxml-standards.md             # FXML 使用规范
    │   │   ├── memory-management.md          # 内存管理规则
    │   │   ├── performance-guide.md          # 性能优化指南
    │   │   ├── binding-compliance.md         # 数据绑定合规规则
    │   │   └── security-checklist.md         # 安全合规清单
    │   └── report-templates/                 # 评审报告模板
    │       └── review-report.md              # 报告模板
    └── javafx-code-reviewer-en/           # 审核技能（英文版）
        ├── SKILL.md                          # 技能入口（英文）
        ├── EVALUATE.md                       # 评估用例集
        ├── references/                       # 独立参考文档
        └── report-templates/                 # 独立报告模板
            └── review-report.md              # 报告模板
    ├── javafx-runner/                     # 运行验证技能（中文版）
    │   ├── SKILL.md                          # 技能入口（中文）
    │   ├── EVALUATE.md                       # 评估用例集
    │   ├── references/                       # 验证维度文档
    │   │   ├── compile-verification.md       # 编译验证规则
    │   │   ├── runtime-verification.md       # 运行验证规则
    │   │   ├── packaging-verification.md     # 打包验证规则
    │   │   └── environment-setup.md          # 环境检测与 Monocle 配置
    │   └── report-templates/                 # 验证报告模板
    │       └── verification-report.md        # 报告模板
    └── javafx-runner-en/                  # 运行验证技能（英文版）
        ├── SKILL.md                          # 技能入口（英文）
        ├── EVALUATE.md                       # 评估用例集
        ├── references/                       # 独立参考文档
        └── report-templates/                 # 独立报告模板
            └── verification-report.md        # 报告模板
```

## 安装

### javafx-developer（中文版）
```bash
npx skills add https://github.com/DongZY0617/javafx-skill --skill javafx-developer
```

### javafx-developer（英文版）
```bash
npx skills add https://github.com/DongZY0617/javafx-skill --skill javafx-developer-en
```

### javafx-code-reviewer（中文版）
```bash
npx skills add https://github.com/DongZY0617/javafx-skill --skill javafx-code-reviewer
```

### javafx-code-reviewer（英文版）
```bash
npx skills add https://github.com/DongZY0617/javafx-skill --skill javafx-code-reviewer-en
```

### javafx-runner（中文版）
```bash
npx skills add https://github.com/DongZY0617/javafx-skill --skill javafx-runner
```

### javafx-runner（英文版）
```bash
npx skills add https://github.com/DongZY0617/javafx-skill --skill javafx-runner-en
```

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| JavaFX | 17 LTS / 21 LTS / 25 LTS | UI 框架 |
| JDK | 17+ | 运行环境 |
| Maven / Gradle | 最新版 | 构建工具 |
| Spring Boot | 3.2+ | 依赖注入容器（可选） |
| MyBatis | 3.0+ | ORM（可选） |

## 核心亮点

### Spring Boot + JavaFX 整合

javafx-developer 技能记录了 Spring Boot + JavaFX 整合的关键陷阱：**主类不能直接继承 `Application`**，否则 JVM 会使用 JavaFX 启动器，在 classpath 模式下会启动失败。解决方案是拆分为两个类 — Spring Boot 启动类 + JavaFX 入口类。

### 完整模板库

javafx-developer 技能包含常见 JavaFX 模式的生产级模板：基础/主窗口/对话框控制器、可观察模型、ViewModel、Presenter、Service 层、FXML 布局、亮色/暗色主题、Maven/Gradle 构建配置和 jpackage 打包配置。

### 规范同源的代码审核

javafx-code-reviewer 技能与 javafx-developer 共享同一套约束体系，确保**"生成即合规、审核即一致"** — 审核标准与生成标准同源，避免代码生成与审核之间的矛盾。覆盖六大审核维度（结构、线程安全、FXML 规范、内存泄漏、性能、合规），采用四级严重性体系（Critical / Major / Minor / Info）。

### 静态到动态的验证链

javafx-runner 技能与 javafx-code-reviewer 互补，将代码质量保障从静态分析推进到动态运行时验证。它通过执行 `mvn compile`、`mvn javafx:run` 和 `jpackage`，捕获那些通过静态审核但在运行时崩溃的问题 — 如 `module-info.java` 的 `opens` 声明缺失，编译通过但 FXML 反射时崩溃。验证报告与审核报告共享同一套修复交接字段格式，javafx-developer 可用同一套逻辑消费两种报告。

## 许可证

Apache License 2.0 — 见 [LICENSE](LICENSE)

## 贡献

欢迎提交 Issue 和 PR！本仓库托管不断增长的 JavaFX 技能集合，欢迎贡献以扩展覆盖范围。
