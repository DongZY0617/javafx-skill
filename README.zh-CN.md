# JavaFX Developer 技能

> 首个面向 AI Agent 的 JavaFX 技能 — 涵盖项目搭建、FXML 界面设计、MVC/MVVM 架构、数据绑定、CSS 主题、Spring Boot 整合和跨平台打包。

## 这是什么？

这是一个 [Agent Skill](https://agentskills.io)，教会 AI Agent 如何构建 JavaFX 桌面应用。它提供覆盖完整开发生命周期的全方位指导 — 从项目脚手架到跨平台打包。

## 功能特性

- **项目搭建**：Maven/Gradle 配置、JavaFX 版本矩阵、JDK 兼容性
- **架构模式**：MVC 和 MVVM 完整代码示例、反模式
- **FXML 界面设计**：布局模板、Controller 模式、CSS 主题（亮色/暗色）
- **数据绑定**：Property 类型、绑定模式、表单验证
- **Spring Boot 整合**：启动类拆分、依赖注入机制、MyBatis + SQLite 集成
- **第三方库**：ControlsFX、MaterialFX、RichTextFX、Ikonli 集成指南
- **打包部署**：jpackage、jlink、CI/CD 集成
- **代码模板**：开箱即用的 pom.xml、module-info.java、Controller、Model、FXML、CSS 模板

## 安装

```bash
npx skills add https://github.com/DongZY0617/javafx-skill --skill javafx-developer
```

## 技能结构

```
javafx-skill/
├── README.md
├── README.zh-CN.md
├── LICENSE
├── .gitignore
└── skills/
    ├── javafx-developer/                  # 中文版
    │   ├── SKILL.md                          # 技能入口（中文）
    │   ├── references/                       # 深度参考文档
    │   │   ├── architecture-patterns.md      # MVC/MVVM 模式
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
    │       └── packaging/                    # jpackage 配置
    └── javafx-developer-en/               # 英文版
        ├── SKILL.md                          # 技能入口（英文）
        ├── references/                       # 共享参考文档
        └── templates/                        # 共享代码模板
```

## 安装

### 中文版
```bash
npx skills add https://github.com/DongZY0617/javafx-skill --skill javafx-developer
```

### 英文版
```bash
npx skills add https://github.com/DongZY0617/javafx-skill --skill javafx-developer-en
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

本技能记录了 Spring Boot + JavaFX 整合的关键陷阱：**主类不能直接继承 `Application`**，否则 JVM 会使用 JavaFX 启动器，在 classpath 模式下会启动失败。解决方案是拆分为两个类 — Spring Boot 启动类 + JavaFX 入口类。

### 完整模板库

包含所有常见 JavaFX 模式的生产级模板：CRUD 表格视图、登录对话框、主从视图、导航抽屉、亮色/暗色主题等。

## 许可证

Apache License 2.0 — 见 [LICENSE](LICENSE)

## 贡献

欢迎提交 Issue 和 PR！这是 Agent Skills 生态中的第一个 JavaFX 技能，欢迎贡献以扩展覆盖范围。
