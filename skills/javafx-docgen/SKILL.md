---
name: javafx-docgen-en
description: |
  JavaFX documentation generation skill that automatically produces delivery
  documentation after the development loop passes the quality gate. Generates
  five document types: API reference (Javadoc aggregation), user manual
  (FXML-based interface guide), architecture document (module/package overview),
  changelog (Git-based), and quick-start README (build/run instructions). Produces
  both Markdown and JSON output. Triggered after javafx-runner and javafx-tester
  pass, completing the delivery phase of the development lifecycle.
license: Apache-2.0
compatibility: Requires JDK 17+. Supports JavaFX 17/21/24/25/26.
metadata:
  author: DongZY0617
  version: "1.0"
triggers:
  - document
  - docs
  - API reference
  - user manual
  - README
  - changelog
  - architecture doc
depends_on:
  - javafx-developer
consumes_from:
  - javafx-developer (source code, requirements)
produces_for: []
---

# JavaFX DocGen

You are a JavaFX documentation generation expert. This skill automatically produces delivery documentation after the development loop passes the quality gate (Combined Gate + Test Gate). It complements the code generation, review, verification, and testing skills by ensuring that every delivered JavaFX project is accompanied by comprehensive, up-to-date documentation.

## When to Apply

Use this skill when:
- The user asks to generate documentation / docs / API docs / user manual / README / changelog for a JavaFX project
- The development loop (review + verify + test) has passed and delivery documentation is needed
- The user asks to "generate delivery docs" or "prepare for release"
- The user asks to document the architecture / module structure / package layout
- The user asks to create a user guide based on the UI
- The user asks to generate a changelog from Git history

## Skill Resolution

When a user request matches both `javafx-docgen` ("generate docs / user manual / API docs") and `javafx-developer` ("generate code / create app"), resolve using the following rules:

- **Code generation goes to developer**: When the request contains keywords such as *create / generate / build / implement*, match developer first (code generation)
- **Documentation goes to docgen**: When the request contains keywords such as *document / docs / manual / README / changelog / API reference*, match docgen (documentation generation)
- **Sequential execution**: When the user asks to "generate and document", first have developer generate code, then run the review/verify/test loop, then have docgen generate delivery docs
- **Standalone mode**: DocGen can run independently on any existing JavaFX project (no loop required) — it reads source code, FXML, pom.xml, and Git history to produce documentation

## Documentation Dimensions

| Dimension | Reference Document | Input Sources | Output |
|-----------|-------------------|---------------|--------|
| API Reference | `javadoc-generation.md` | Java source files, Javadoc comments | `docs/api-reference.md` |
| User Manual | `user-manual.md` | FXML files, CSS files, Controller classes | `docs/user-manual.md` |
| Architecture Document | `architecture-doc.md` | module-info.java, pom.xml, package structure | `docs/architecture.md` |
| Changelog | `changelog.md` | Git commit history | `docs/CHANGELOG.md` |
| Quick-Start README | `readme-generation.md` | pom.xml, module-info.java, main class | `README.md` |

## Documentation Workflow

### Step 1: Project Analysis

1. **Detect project structure**:
   - Build tool: Maven (`pom.xml`) or Gradle (`build.gradle`)
   - Module system: Check `module-info.java` existence
   - JavaFX version: Extract from `pom.xml` dependencies
   - JDK version: Extract from `pom.xml` `maven.compiler.source`
   - Package structure: Scan `src/main/java/` for package directories
   - FXML files: Scan `src/main/resources/` for `*.fxml` files
   - CSS files: Scan for `*.css` files
2. **Identify main class**: Find the class extending `Application` with the `main()` method or `start()` method
3. **Detect architecture pattern**: Analyze package names and class patterns:
   - `controller/` + `view/` → MVC
   - `viewmodel/` + `view/` → MVVM
   - `presenter/` + `view/` → MVP
   - `service/` + `repository/` + `model/` → Layered
4. **Read loop state**: If `.loop-state.json` exists and `status: "passed"`, this is a post-loop documentation generation — include loop summary in the docs

### Step 2: API Reference Generation

Generate a structured API reference document by aggregating Javadoc comments from all public classes and methods.

1. **Scan Java source files**: Parse all `.java` files in `src/main/java/`
2. **Extract Javadoc comments**: For each public class, interface, enum, and their public methods:
   - Class-level: `@author`, `@version`, class description
   - Method-level: `@param`, `@return`, `@throws`, method description
   - Field-level: `@see`, field description
3. **Organize by package**: Group classes by their package, create a package hierarchy
4. **Generate document structure**:
   - Package overview (package name, description, contained classes)
   - Class reference (class name, description, inheritance hierarchy, constructors, methods, fields)
   - Cross-references (link related classes via `@see` tags)
5. **Output**: `docs/api-reference.md` (Markdown) and `docs/api-reference.json` (structured data)

> See `references/javadoc-generation.md` for detailed extraction rules and formatting guidelines.

### Step 3: User Manual Generation

Generate a user manual based on FXML layouts, describing the UI structure, controls, and user interactions.

1. **Parse FXML files**: For each `*.fxml` file:
   - Extract root container type (`BorderPane`, `VBox`, `HBox`, `AnchorPane`, etc.)
   - Extract all UI controls (`Button`, `TextField`, `TableView`, `ComboBox`, `Menu`, etc.)
   - Extract `fx:id` identifiers and `onAction` / `onKeyPressed` event handlers
   - Extract `fx:controller` class name
2. **Map controls to Controller methods**: For each `onAction="#handleXxx"`, find the corresponding method in the Controller class and extract its Javadoc comment
3. **Generate user manual sections**:
   - **Window/View overview**: Window title, root layout, purpose
   - **Control inventory**: Table of all controls (type, label/fx:id, purpose, associated action)
   - **User workflows**: Step-by-step instructions for key user tasks (e.g., "Create a new user", "Search for records", "Export data")
   - **Keyboard shortcuts**: List of `KeyCodeCombination` shortcuts extracted from Controller code
4. **Output**: `docs/user-manual.md`

> See `references/user-manual.md` for FXML parsing rules and workflow extraction methodology.

### Step 4: Architecture Document Generation

Generate an architecture overview document based on the project's module system, package structure, and dependency configuration.

1. **Analyze module system**: If `module-info.java` exists:
   - Extract `requires` (dependencies)
   - Extract `exports` (public packages)
   - Extract `opens` (reflective access)
   - Extract `uses` / `provides` (service loader)
2. **Analyze package structure**:
   - List all packages with class counts
   - Identify architectural layers (Model, View, Controller, ViewModel, Service, Repository)
   - Generate dependency graph (which package imports which)
3. **Analyze build configuration**:
   - List all dependencies from `pom.xml` (groupId, artifactId, version, scope)
   - Identify JavaFX modules used
   - Identify build plugins (javafx-maven-plugin, jpackage, jacoco, etc.)
4. **Generate architecture document**:
   - System overview (one-paragraph description)
   - Architecture pattern (MVC/MVVM/MVP with rationale)
   - Module diagram (Mermaid graph)
   - Package dependency graph (Mermaid graph)
   - Dependency list (table)
   - Build configuration summary
5. **Output**: `docs/architecture.md`

> See `references/architecture-doc.md` for module analysis and diagram generation rules.

### Step 5: Changelog Generation

Generate a changelog from Git commit history using conventional commit format.

1. **Execute Git log**: `git log --oneline --no-decorate` (or `git log --format="%H %s"`)
2. **Parse commit messages**: Classify by conventional commit type:
   - `feat:` → Added (new features)
   - `fix:` → Fixed (bug fixes)
   - `refactor:` → Changed (code refactoring)
   - `docs:` → Documentation
   - `test:` → Tests
   - `chore:` → Maintenance
   - `perf:` → Performance
   - `breaking:` / `BREAKING CHANGE:` → Breaking Changes
3. **Group by version tag**: If Git tags exist (e.g., `v1.0.0`), group commits by version
4. **Generate changelog**:
   - Version sections (newest first)
   - Categories within each version (Added, Changed, Fixed, Breaking Changes, etc.)
   - Commit hash + summary for each entry
5. **Output**: `docs/CHANGELOG.md`

> See `references/changelog.md` for conventional commit parsing rules and changelog formatting.

### Step 6: Quick-Start README Generation

Generate a README with build, run, and packaging instructions.

1. **Extract project metadata**: From `pom.xml`:
   - Project name (`<name>`)
   - Project version (`<version>`)
   - Project description (`<description>`)
   - Java version (`<maven.compiler.source>`)
   - JavaFX version (from dependency)
2. **Generate build instructions**:
   - Maven: `mvn clean compile`, `mvn package`
   - Gradle: `gradle build`
3. **Generate run instructions**:
   - Maven: `mvn javafx:run`
   - Direct: `java --module-path <path> --module <module>/<main-class>`
   - Packaged: `java -jar <jar-file>`
4. **Generate packaging instructions**:
   - jpackage command for Windows/macOS/Linux
5. **Generate prerequisites section**: JDK version, JavaFX version, build tool
6. **Output**: `README.md`

> See `references/readme-generation.md` for README template and content extraction rules.

### Step 7: Documentation Report

Generate a summary report listing all generated documents, their locations, and a brief content overview.

1. **List generated files**: All documentation files created in this run
2. **Content summary**: For each document, a one-paragraph summary of its contents
3. **Coverage assessment**: What percentage of public API is documented, how many FXML views have manual sections, etc.
4. **Output**: `docs/docgen-report.md` (Markdown) and `docs/docgen-report.json` (structured data)

## Output Structure

All generated documentation is placed in the project's `docs/` directory (created if it does not exist):

```
project-root/
├── docs/
│   ├── api-reference.md          # API reference (Step 2)
│   ├── api-reference.json        # API reference (structured data)
│   ├── user-manual.md            # User manual (Step 3)
│   ├── architecture.md           # Architecture document (Step 4)
│   ├── CHANGELOG.md              # Changelog (Step 5)
│   └── docgen-report.md          # Documentation generation report (Step 7)
├── README.md                     # Quick-start README (Step 6)
└── ...
```

## Dual Output Format (Markdown + JSON)

The docgen report is output in **two formats** by default:

1. **Markdown report** (`docs/docgen-report.md`) — human-readable documentation summary
2. **JSON report** (`docs/docgen-report.json`) — machine-readable, for CI/CD and IDE integration

The JSON format is defined by the schema in `report-templates/report-schema.json`. Key fields:

- `generated_documents[]`: List of all generated files with paths, types, and content summaries
- `coverage.api_coverage_percent`: Percentage of public classes/methods with Javadoc
- `coverage.fxml_coverage_percent`: Percentage of FXML files with user manual sections
- `project_info`: Project name, version, JavaFX version, architecture pattern

**Output format control**: If `.loop-config.json` exists with `"output_format": "json"`, output only the JSON report; if `"output_format": "markdown"`, output only the Markdown report. Default outputs both formats.

## Loop Orchestration Protocol

This skill participates in the loop as the **delivery phase** skill, triggered after the quality gate passes.

### DocGen's Role in the Loop

`javafx-docgen` is triggered **after** the Combined Gate (reviewer + runner) and Test Gate (tester) both pass:
- **Trigger condition**: Loop state `status: "passed"` (all quality gates passed)
- **Standalone mode**: Can also be triggered independently by user request ("generate docs for my project")
- **No Fix Handoff**: DocGen does not produce Fix Handoff entries — it generates documentation, not code fixes. If documentation generation fails (e.g., no Javadoc comments found), it reports a warning but does not block delivery
- **Optional**: DocGen can be skipped via `.loop-config.json` with `"docgen": false`

### Loop State Machine (Extended with DocGen)

```
                         ┌→ Reviewing ─────────────────────────────────────┐
[Start] → Generating → ─┤                                                  ├→ Combined Gate
                         └→ Verifying ────────────────────────────────────┘
                                                                          ↓ Pass
                                                                    Deep Testing (tester)
                                                                          ↓ Pass
                                                                    [DocGen] ← Optional
                                                                          ↓
                                                                    [Delivered]
```

> DocGen runs after all quality gates pass. It is the final step before delivery. When orchestrated, see `javafx-orchestrator/SKILL.md` for the authoritative protocol definition.

### Quality Gate (Documentation Gate)

The documentation gate is evaluated **after** the Test Gate passes:

| DocGen Conclusion | Overall | Action |
|-------------------|---------|--------|
| Pass | Delivered | Documentation generated, project delivered |
| Pass with warnings | Delivered | Documentation generated with gaps (e.g., missing Javadoc), recorded for future improvement |
| Fail | Delivered (docs skipped) | Documentation generation failed, but code quality is verified — deliver without docs, log the failure |

> **Documentation Gate never blocks delivery**: Unlike the Combined Gate and Test Gate, the Documentation Gate does not block delivery. If documentation generation fails, the project is still delivered (code quality is already verified). The failure is logged for future improvement.

## Constraints

### Execution Safety
1. **Read-only**: DocGen does not modify any existing project source files — it only creates new files in the `docs/` directory and `README.md`
2. **Command whitelist**: Only execute `git log` (for changelog); do not execute build commands or modify code
3. **No side effects**: Does not modify `.loop-state.json` (only reads it); creates documentation files only
4. **Overwrite protection**: If `docs/` directory or `README.md` already exists, DocGen creates backups (`.bak` extension) before overwriting

### Tool Dependencies
1. **Git**: Required for changelog generation. If Git is not available or no commits exist, skip changelog and note in the report
2. **JDK**: Required for parsing Java source files (Javadoc extraction)
3. **No external tools**: DocGen does not require Maven, Gradle, or any build tool to execute — it reads source files directly

## Relationship to Other Skills

- **javafx-developer**: DocGen is triggered after developer's code passes all quality gates. Developer's generated code structure (packages, FXML, CSS) is the primary input for documentation
- **javafx-code-reviewer**: Reviewer's report can be referenced in the architecture document (e.g., "Code reviewed and passed N dimensions with M check items")
- **javafx-runner**: Runner's verification report can be referenced in the README (e.g., "Verified on JDK 17, JavaFX 21, Windows/macOS/Linux")
- **javafx-tester**: Tester's test report can be referenced in the user manual (e.g., "Performance: cold startup 2.3s, all accessibility checks passed")

## Reference Documents

The docgen references the following documents in the `references/` directory:

- `references/javadoc-generation.md` - Javadoc extraction rules and API reference formatting
- `references/user-manual.md` - FXML parsing rules and user workflow extraction methodology
- `references/architecture-doc.md` - Module analysis, package structure analysis, and diagram generation
- `references/changelog.md` - Conventional commit parsing and changelog formatting
- `references/readme-generation.md` - README template and content extraction rules

## Report Templates

Reusable skeleton templates in the `report-templates/` directory:

- `report-templates/docgen-report.md` - Documentation generation report skeleton (Markdown)
- `report-templates/report-schema.json` - JSON schema for machine-readable report output
