# JavaFX Developer Skill

> The first JavaFX skill for AI agents — covering project setup, FXML UI design, MVC/MVVM architecture, data binding, CSS theming, Spring Boot integration, and cross-platform packaging.

## What is this?

This is an [Agent Skill](https://agentskills.io) that teaches AI agents how to build JavaFX desktop applications. It provides comprehensive guidance covering the full development lifecycle — from project scaffolding to cross-platform packaging.

## Features

- **Project Setup**: Maven/Gradle configuration, JavaFX version matrix, JDK compatibility
- **Architecture Patterns**: MVC and MVVM with complete code examples, anti-patterns
- **FXML UI Design**: Layout templates, controller patterns, CSS theming (light/dark)
- **Data Binding**: Property types, binding modes, form validation patterns
- **Spring Boot Integration**: Startup class splitting, DI mechanism, MyBatis + SQLite integration
- **Third-Party Libraries**: ControlsFX, MaterialFX, RichTextFX, Ikonli integration guides
- **Packaging & Deployment**: jpackage, jlink, CI/CD integration
- **Code Templates**: Ready-to-use templates for pom.xml, module-info.java, controllers, models, FXML, CSS

## Skill Structure

```
javafx-skill/
├── README.md
├── README.zh-CN.md
├── LICENSE
├── .gitignore
└── skills/
    ├── javafx-developer/                  # Chinese version (中文版)
    │   ├── SKILL.md                          # Skill entry point (Chinese)
    │   ├── references/                       # In-depth reference docs
    │   │   ├── architecture-patterns.md      # MVC/MVVM patterns
    │   │   ├── css-best-practices.md         # CSS theming guide
    │   │   ├── data-binding-patterns.md      # Property binding guide
    │   │   ├── packaging-deployment.md       # jpackage/jlink guide
    │   │   ├── project-setup.md             # Maven/Gradle setup
    │   │   ├── spring-boot-integration.md    # Spring Boot + JavaFX guide
    │   │   └── third-party-libraries.md      # Library integration
    │   └── templates/                        # Reusable code templates
    │       ├── controller/                   # Controller templates
    │       ├── css/                          # Light/dark theme CSS
    │       ├── fxml/                         # FXML layout templates
    │       ├── gradle/                       # Gradle build template
    │       ├── maven/                        # Maven POM + module-info
    │       ├── model/                        # Observable model template
    │       └── packaging/                    # jpackage config
    └── javafx-developer-en/               # English version
        ├── SKILL.md                          # Skill entry point (English)
        ├── references/                       # Independent reference docs
        └── templates/                        # Independent code templates
```

## Installation

### Chinese version (中文版)
```bash
npx skills add https://github.com/DongZY0617/javafx-skill --skill javafx-developer
```

### English version
```bash
npx skills add https://github.com/DongZY0617/javafx-skill --skill javafx-developer-en
```

## Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| JavaFX | 17 LTS / 21 LTS / 25 LTS | UI framework |
| JDK | 17+ | Runtime environment |
| Maven / Gradle | Latest | Build tools |
| Spring Boot | 3.2+ | DI container (optional) |
| MyBatis | 3.0+ | ORM (optional) |

## Key Highlights

### Spring Boot + JavaFX Integration

This skill documents the critical pitfall of Spring Boot + JavaFX integration: **the main class must NOT directly extend `Application`**, otherwise JVM uses the JavaFX launcher which fails in classpath mode. The solution is to split into two classes — a Spring Boot launcher + a JavaFX entry class.

### Complete Template Library

Includes production-ready templates for common JavaFX patterns: base/main/dialog controllers, observable models, viewmodels, service layer, FXML layouts, light/dark themes, Maven/Gradle build configs, and jpackage deployment config.

## License

Apache License 2.0 — see [LICENSE](LICENSE)

## Contributing

Issues and PRs are welcome! This is the first JavaFX skill in the Agent Skills ecosystem, and contributions to expand coverage are encouraged.
