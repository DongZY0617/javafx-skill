# JavaFX Skill Set

A closed-loop JavaFX desktop application development skill set covering the full lifecycle: from requirements engineering to architecture design, UI design, code generation, code review, runtime verification, deep testing, refactoring, documentation, deployment, and rollback.

## Overview

This skill set consists of **11 skills** coordinated by `javafx-orchestrator`, forming a complete development loop:

```
Requirements → Architect → Designer → Developer → Reviewer ∥ Runner → Tester → Refactorer → DocGen → Deployer → Shipped
                                                                              ↑________ Fix Handoff ________↓
```

- **Compatibility**: Requires JDK 17+. Supports JavaFX 17 / 21 / 24 / 25 / 26.
- **License**: Apache-2.0
- **Author**: DongZY0617

## Skills

| # | Skill | Version | Role | Key Triggers |
|---|-------|---------|------|--------------|
| 1 | `javafx-requirements` | 1.0 | Requirements engineering — stakeholders, user stories, acceptance criteria, NFRs, traceability | requirements, user stories, acceptance criteria, stakeholder analysis |
| 2 | `javafx-architect` | 1.1 | Architecture design — tech selection, UML, ADR, database schema, STRIDE threat modeling, prototype validation | architecture, system design, UML, ADR, threat modeling, STRIDE |
| 3 | `javafx-designer` | 1.0 | UI/UX visual design — FXML prototypes, CSS themes, interaction flows, responsive layout, icon config | design, prototype, UI design, theme, CSS theme, interaction flow |
| 4 | `javafx-developer` | 1.1 | Code generation — project scaffolding, FXML, MVC/MVVM, data binding, CSS, packaging, networking, custom controls | create, generate, build, scaffold, implement, JavaFX app |
| 5 | `javafx-code-reviewer` | 1.1 | Static code review — 10 dimensions: structure, thread safety, FXML, memory, performance, compliance, security, database, requirements, refactoring | review, audit, check, compliance, code quality |
| 6 | `javafx-runner` | 1.1 | Runtime verification — compile, run, package, smoke test, headless CI verification | verify, compile, run, package, deploy verification |
| 7 | `javafx-tester` | 1.2 | Deep testing — 4 parallel tracks: performance, security, accessibility, visual regression | test, performance test, security test, accessibility test, visual regression |
| 8 | `javafx-refactorer` | 1.0 | Code refactoring — code smell detection, refactoring recommendations, technical debt management | refactor, clean up, eliminate code smells, reduce technical debt |
| 9 | `javafx-docgen` | 1.1 | Documentation generation — API reference, user manual, architecture doc, changelog, README, Javadoc HTML | document, docs, API reference, user manual, changelog, javadoc |
| 10 | `javafx-deployer` | 1.1 | Deployment & DevOps — CI/CD, release, signing, auto-update, monitoring, distribution channels, rollback | deploy, CI/CD, release, sign, auto-update, MSIX, Snap, Flatpak, rollback |
| 11 | `javafx-orchestrator` | 1.0 | Closed-loop orchestration — state machine, quality gates, fix handoff, routing, state recovery | orchestrate, full loop, run the cycle, coordinate skills |

## Skill Dependency Graph

```
javafx-orchestrator (coordinates all)
    │
    ├── javafx-requirements (optional, pre-architect)
    │       └── produces: requirements-handoff.json
    │             ↓ consumed by: architect, developer
    │
    ├── javafx-architect (optional, pre-generation)
    │       └── produces: architecture-handoff.json
    │             ↓ consumed by: developer, designer, tester (threat_model)
    │
    ├── javafx-designer (optional, pre-generation)
    │       └── produces: design artifacts (FXML prototypes, CSS themes)
    │             ↓ consumed by: developer
    │
    ├── javafx-developer (core)
    │       └── produces: source code
    │             ↓ consumed by: code-reviewer, runner, tester, docgen, deployer
    │
    ├── javafx-code-reviewer ──┐ (parallel, static review)
    │       └── produces: fix handoff report → developer
    │
    ├── javafx-runner ─────────┘ (parallel, dynamic verification)
    │       └── produces: fix handoff report → developer
    │
    ├── javafx-tester (after Combined Gate passes)
    │       └── produces: fix handoff report → developer
    │             ↓ 4 tracks: Performance ∥ Security ∥ Accessibility ∥ Visual Regression
    │
    ├── javafx-refactorer (optional, after Test Gate)
    │       └── produces: refactor-handoff.json → developer
    │
    ├── javafx-docgen (after Test Gate)
    │       └── produces: API reference, user manual, architecture doc, changelog, README
    │
    └── javafx-deployer (optional, post-delivery)
            └── produces: CI/CD configs, signing scripts, auto-update, distribution, rollback
```

## Quick Start

### Option 1: Full Closed-Loop (Recommended for New Projects)

Trigger the orchestrator to run the complete development cycle:

```
Orchestrate a full loop for my JavaFX inventory management application.
It should have a TableView for inventory, a form for adding items,
and SQLite database storage.
```

The orchestrator will execute: requirements → architect → designer → developer → reviewer ∥ runner → tester → docgen → deployer.

### Option 2: Generate and Review (Common Workflow)

```
Create a JavaFX application with a login form and dashboard.
Then review and verify the generated code.
```

This triggers developer → code-reviewer ∥ runner in sequence.

### Option 3: Individual Skill (Standalone)

Each skill can run independently:

```
# Architecture only
Design the architecture for a JavaFX app with REST API and SQLite database. Include threat modeling.

# Code review only
Review my JavaFX code for thread safety and memory leaks.

# Testing only
Run performance and security tests on my JavaFX application.

# Deployment only
Set up CI/CD with GitHub Actions, Windows code signing, and auto-update for my JavaFX app.
```

## Loop Configuration

Create a `.loop-config.json` in your project root to customize the loop:

```json
{
  "architect": true,
  "designer": true,
  "deep_testing": true,
  "refactoring": false,
  "docgen": true,
  "doc_gate_mode": "non-blocking",
  "doc_javadoc_html": false,
  "deployer": false,
  "max_fix_iterations": 3,
  "parallel_mode": true,
  "vr_update_baselines": false
}
```

| Config | Default | Description |
|--------|---------|-------------|
| `architect` | `true` | Run architecture design phase before code generation |
| `designer` | `true` | Run UI design phase before code generation |
| `deep_testing` | `true` | Run deep testing (performance, security, a11y, visual regression) after Combined Gate |
| `refactoring` | `false` | Run refactoring phase after Test Gate passes |
| `docgen` | `true` | Run documentation generation after Test Gate |
| `doc_gate_mode` | `"non-blocking"` | Documentation gate: `"non-blocking"` (default) or `"blocking"` |
| `doc_javadoc_html` | `false` | Generate Javadoc HTML site in addition to Markdown API reference |
| `deployer` | `false` | Run deployment configuration after DocGen |
| `max_fix_iterations` | `3` | Maximum fix-and-re-review cycles before manual intervention |
| `parallel_mode` | `true` | Run code-reviewer and runner in parallel |
| `vr_update_baselines` | `false` | Update visual regression baseline images (use `-Dupdate.baselines=true` alternatively) |

## Quality Gates

The loop implements three quality gates:

| Gate | Stage | Blocking | Description |
|------|-------|----------|-------------|
| **Combined Gate** | After reviewer ∥ runner | Yes | Code must pass static review AND runtime verification before proceeding |
| **Test Gate** | After tester | Yes | Deep testing (performance, security, a11y, visual regression) must pass |
| **Documentation Gate** | After docgen | Configurable | Documentation generation — blocking or non-blocking (configurable via `doc_gate_mode`) |

## Fix Handoff Protocol

When reviewer, runner, or tester identifies issues, they produce a **Fix Handoff Report** that flows back to the developer:

```
[Issue Found] → Fix Handoff Report → Developer (Fix Consumption Mode) → Re-generate → Re-review/verify
```

The loop supports up to `max_fix_iterations` (default: 3) cycles before escalating to manual intervention.

## Project Structure

```
javafx-skill/
├── README.md                          # This file
├── skills/
│   ├── javafx-requirements/
│   │   ├── SKILL.md                   # Skill definition
│   │   ├── EVALUATE.md                # 12 test cases
│   │   ├── references/                # 4 reference docs
│   │   ├── templates/                 # Requirement templates
│   │   └── report-templates/          # Report schema
│   ├── javafx-architect/
│   │   ├── SKILL.md
│   │   ├── EVALUATE.md                # 25 test cases
│   │   ├── references/                # 5 reference docs
│   │   └── report-templates/
│   ├── javafx-designer/
│   │   ├── SKILL.md
│   │   ├── EVALUATE.md                # 15 test cases
│   │   ├── references/                # 5 reference docs
│   │   └── report-templates/
│   ├── javafx-developer/
│   │   ├── SKILL.md
│   │   ├── EVALUATE.md                # 16 test cases
│   │   ├── references/                # 13 reference docs
│   │   ├── templates/                 # 13 template directories (incl. maven/pom.xml with JaCoCo)
│   │   └── report-templates/          # Report schema
│   ├── javafx-code-reviewer/
│   │   ├── SKILL.md
│   │   ├── EVALUATE.md                # 14 test cases
│   │   ├── references/                # 11 reference docs
│   │   └── report-templates/
│   ├── javafx-runner/
│   │   ├── SKILL.md
│   │   ├── EVALUATE.md                # 18 test cases
│   │   ├── references/                # 7 reference docs
│   │   └── report-templates/
│   ├── javafx-tester/
│   │   ├── SKILL.md
│   │   ├── EVALUATE.md                # 23 test cases
│   │   ├── references/                # 4 reference docs
│   │   └── report-templates/
│   ├── javafx-refactorer/
│   │   ├── SKILL.md
│   │   ├── EVALUATE.md                # 15 test cases
│   │   ├── references/                # 3 reference docs
│   │   └── report-templates/
│   ├── javafx-docgen/
│   │   ├── SKILL.md
│   │   ├── EVALUATE.md                # 18 test cases
│   │   ├── references/                # 5 reference docs
│   │   └── report-templates/
│   ├── javafx-deployer/
│   │   ├── SKILL.md
│   │   ├── EVALUATE.md                # 20 test cases
│   │   ├── references/                # 7 reference docs
│   │   └── report-templates/
│   └── javafx-orchestrator/
│       ├── SKILL.md
│       ├── EVALUATE.md                # 14 test cases
│       └── templates/                 # Loop dashboard HTML
```

## Technology Stack

- **Language**: Java (JDK 17+)
- **Framework**: JavaFX 17 / 21 / 24 / 25 / 26
- **Build Tool**: Maven (default), Gradle (supported)
- **Architecture**: MVC / MVVM + Service Layer
- **Database**: SQLite (default), H2, PostgreSQL (supported)
- **ORM**: MyBatis (default), JPA/Hibernate (supported)
- **UI Libraries**: ControlsFX, MaterialFX, RichTextFX, Ikonli
- **Networking**: Retrofit (retrofit-spring-boot-starter)
- **Static Analysis**: SpotBugs, PMD, Checkstyle
- **Testing**: TestFX 4.0.18, Monocle (jdk-17+21), JMH (optional)
- **Packaging**: jpackage, jlink
- **CI/CD**: GitHub Actions, GitLab CI
- **Distribution**: MSIX/Microsoft Store, Mac App Store, Snap, Flatpak

## Evaluation

Each skill includes an `EVALUATE.md` file with acceptance test cases:

- **Positive samples**: Real-world scenarios verifying output completeness
- **Negative samples**: Constraint violations verifying robustness
- **Boundary cases**: Partial scopes, standalone mode, platform-specific scenarios

Total: **190 evaluation test cases** across 11 skills.

## Loop State Machine

The orchestrator manages loop state via `.loop-state.json`:

```
[Start] → requirements → architecting → designing → generating
  → reviewing_and_verifying → [Combined Gate]
  → testing → [Test Gate]
  → (optional) refactoring
  → docgen → [Documentation Gate]
  → (optional) deploying → delivered → (optional) shipped
```

States: `requirements` → `architecting` → `designing` → `generating` → `reviewing_and_verifying` → `fixing` → `passed` → `testing` → `refactoring` → `docgen` → `fix_documentation` → `deploying` → `delivered` → `shipped` → `paused`

## License

Apache-2.0
