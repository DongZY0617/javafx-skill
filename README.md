# JavaFX Skills

> A JavaFX skill collection for AI agents — covering project development (`javafx-developer`) and professional code review (`javafx-code-reviewer`), from project scaffolding to quality assurance.

## What is this?

This is an [Agent Skill](https://agentskills.io) collection that teaches AI agents how to build and review JavaFX desktop applications. It provides two complementary skills covering the full development lifecycle:

- **javafx-developer** — Build JavaFX applications, from project scaffolding to cross-platform packaging.
- **javafx-code-reviewer** — Review JavaFX code against official specifications and best practices.

Together they form a closed loop: **generate → review → fix**, ensuring continuous code quality.

## Skills

### javafx-developer

Build JavaFX desktop applications — from project scaffolding to cross-platform packaging.

- **Project Setup**: Maven/Gradle configuration, JavaFX version matrix, JDK compatibility
- **Architecture Patterns**: MVC, MVVM, and MVP with complete code examples, anti-patterns
- **FXML UI Design**: Layout templates, controller patterns, CSS theming (light/dark)
- **Data Binding**: Property types, binding modes, form validation patterns
- **Spring Boot Integration**: Startup class splitting, DI mechanism, MyBatis + SQLite integration
- **Third-Party Libraries**: ControlsFX, MaterialFX, RichTextFX, Ikonli integration guides
- **Packaging & Deployment**: jpackage, jlink, CI/CD integration
- **Code Templates**: Ready-to-use templates for pom.xml, module-info.java, controllers, models, FXML, CSS

### javafx-code-reviewer

Review JavaFX code against official specifications and best practices.

- **Code Structure Review**: Architecture layering, responsibility division, modular configuration
- **UI Thread Safety**: FX thread updates, background task handling, Platform.runLater correctness
- **FXML Standards**: fx:id matching, controller mapping, resource paths, event binding
- **Memory Leak Risks**: Listener removal, Binding disposal, static references, resource cleanup
- **Performance Analysis**: Virtualization, batch updates, throttling, CSS efficiency, lazy loading
- **Deep Compliance Audit**: Naming conventions, security rules, Spring Boot pitfalls, version compatibility
- **Structured Report**: Categorized findings with severity levels, code locations, and optimization suggestions

## Skill Structure

```
javafx-skill/
├── README.md
├── README.zh-CN.md
├── LICENSE
├── .gitignore
└── skills/
    ├── javafx-developer/                  # Development skill (Chinese)
    │   ├── SKILL.md                          # Skill entry point (Chinese)
    │   ├── EVALUATE.md                       # Evaluation test cases
    │   ├── references/                       # In-depth reference docs
    │   │   ├── architecture-patterns.md      # MVC/MVVM/MVP patterns
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
    │       ├── presenter/                    # MVP Presenter/View templates
    │       ├── service/                      # Service/Repository templates
    │       ├── viewmodel/                    # ViewModel template (MVVM)
    │       └── packaging/                    # jpackage config
    ├── javafx-developer-en/               # Development skill (English)
    │   ├── SKILL.md                          # Skill entry point (English)
    │   ├── EVALUATE.md                       # Evaluation test cases
    │   ├── references/                       # Independent reference docs
    │   └── templates/                        # Independent code templates
    ├── javafx-code-reviewer/              # Code review skill (Chinese)
    │   ├── SKILL.md                          # Skill entry point (Chinese)
    │   ├── EVALUATE.md                       # Evaluation test cases
    │   ├── references/                       # Review dimension docs
    │   │   ├── structure-review.md           # Code structure rules
    │   │   ├── thread-safety-rules.md        # UI thread safety rules
    │   │   ├── fxml-standards.md             # FXML usage standards
    │   │   ├── memory-management.md          # Memory management rules
    │   │   ├── performance-guide.md          # Performance optimization guide
    │   │   ├── binding-compliance.md         # Data binding compliance
    │   │   └── security-checklist.md         # Security compliance checklist
    │   └── report-templates/                 # Review report templates
    │       └── review-report.md              # Report template
    └── javafx-code-reviewer-en/           # Code review skill (English)
        ├── SKILL.md                          # Skill entry point (English)
        ├── EVALUATE.md                       # Evaluation test cases
        ├── references/                       # Independent reference docs
        └── report-templates/                 # Independent report templates
            └── review-report.md              # Report template
```

## Installation

### javafx-developer (Chinese)
```bash
npx skills add https://github.com/DongZY0617/javafx-skill --skill javafx-developer
```

### javafx-developer (English)
```bash
npx skills add https://github.com/DongZY0617/javafx-skill --skill javafx-developer-en
```

### javafx-code-reviewer (Chinese)
```bash
npx skills add https://github.com/DongZY0617/javafx-skill --skill javafx-code-reviewer
```

### javafx-code-reviewer (English)
```bash
npx skills add https://github.com/DongZY0617/javafx-skill --skill javafx-code-reviewer-en
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

The javafx-developer skill documents the critical pitfall of Spring Boot + JavaFX integration: **the main class must NOT directly extend `Application`**, otherwise JVM uses the JavaFX launcher which fails in classpath mode. The solution is to split into two classes — a Spring Boot launcher + a JavaFX entry class.

### Complete Template Library

The javafx-developer skill includes production-ready templates for common JavaFX patterns: base/main/dialog controllers, observable models, viewmodels, presenters, service layer, FXML layouts, light/dark themes, Maven/Gradle build configs, and jpackage deployment config.

### Specification-Sourced Code Review

The javafx-code-reviewer skill shares the same constraint system as javafx-developer, ensuring **"generate compliant, review consistent"** — review standards are sourced from the same specification baseline, avoiding contradictions between code generation and code review. It covers six review dimensions (structure, thread safety, FXML standards, memory leaks, performance, compliance) with a four-level severity system (Critical / Major / Minor / Info).

## License

Apache License 2.0 — see [LICENSE](LICENSE)

## Contributing

Issues and PRs are welcome! This repository hosts a growing collection of JavaFX skills for the Agent Skills ecosystem, and contributions to expand coverage are encouraged.
