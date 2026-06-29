# EVALUATE.md — javafx-architect

> 15 evaluation test cases that quantify architecture design quality.
> Each test case includes: description, input, expected output, and pass criteria.

## Test Cases

### TC-01: Full Architecture Design (Default Scope)
**Description**: User requests full architecture design for a new JavaFX e-commerce desktop app.
**Input**: "Design the architecture for a JavaFX e-commerce app with order management, inventory, and user authentication. Use SQLite and MVVM."
**Expected Output**: All 4 dimensions activated. System design with technology matrix, 3 UML diagrams (class/sequence/deployment), 3+ ADRs, architecture-handoff.json.
**Pass Criteria**: `architecture-handoff.json` exists with `scope: "full"`, `conclusion: "Pass"`, all 4 dimension artifacts present.

### TC-02: System Design Only Scope
**Description**: User requests only technology selection and module decomposition.
**Input**: "Select the technology stack and plan module structure for a JavaFX inventory app."
**Expected Output**: `scope: "system_design_only"`, technology selection matrix filled, module decomposition table, no UML/ADR/prototype.
**Pass Criteria**: `architecture-handoff.json` has `scope: "system_design_only"`, `uml_artifacts` and `adr_files` arrays are empty.

### TC-03: UML Only Scope
**Description**: User requests only UML diagram generation.
**Input**: "Generate UML class and sequence diagrams for my existing JavaFX app."
**Expected Output**: `scope: "uml_only"`, class-diagram.puml and sequence-diagram.puml generated, no system design or ADR.
**Pass Criteria**: `.puml` files are valid PlantUML syntax, `system_design` field is minimal or absent.

### TC-04: ADR Only Scope
**Description**: User requests only Architecture Decision Records.
**Input**: "Create ADRs for our technology decisions: JavaFX 25, SQLite, manual DI."
**Expected Output**: `scope: "adr_only"`, 3 ADR files created with Michael Nygard format, README.md index created.
**Pass Criteria**: Each ADR has Status/Context/Decision/Consequences/Alternatives sections, index file lists all ADRs.

### TC-05: PlantUML Class Diagram Syntax Validation
**Description**: Verify generated class diagram is valid PlantUML.
**Input**: Requirements for a CRUD app with User, Order, Product entities.
**Expected Output**: `class-diagram.puml` with @startuml/@enduml, class declarations, relationships, packages.
**Pass Criteria**: File starts with `@startuml` and ends with `@enduml`, all classes are in package blocks, relationships have direction and multiplicity.

### TC-06: ADR Michael Nygard Format Compliance
**Description**: Verify ADR follows the Nygard template exactly.
**Input**: "Create an ADR for choosing MVVM architecture."
**Expected Output**: ADR file with all 5 required sections.
**Pass Criteria**: File contains `## Status`, `## Context`, `## Decision`, `## Consequences`, `## Alternatives Considered` headers. Status is one of: Proposed/Accepted/Deprecated/Superseded.

### TC-07: Technology Selection Justification
**Description**: Every technology choice must include a rationale.
**Input**: "Select technologies for a JavaFX real-time monitoring dashboard."
**Expected Output**: Technology selection matrix with rationale column filled for every category.
**Pass Criteria**: No empty rationale cells. Each rationale references project-specific requirements (not generic).

### TC-08: Architecture Handoff JSON Structure
**Description**: Verify handoff JSON is valid against report-schema.json.
**Input**: Full architecture design request.
**Expected Output**: `architecture-handoff.json` with all required fields.
**Pass Criteria**: JSON validates against `report-templates/report-schema.json`. Required fields: project, architect_version, created_at, scope, system_design, developer_instructions, conclusion.

### TC-09: Module Decomposition Quality
**Description**: Modules must have single responsibility and clear boundaries.
**Input**: Requirements for a multi-module app (user management, order processing, reporting, notifications).
**Expected Output**: 4+ modules with distinct responsibilities, package paths, and dependency lists.
**Pass Criteria**: Each module has a unique responsibility (no overlap), dependencies form a DAG (no cycles), package paths follow `com.example.app.{module}` pattern.

### TC-10: Prototype Validation for High-Risk Technology
**Description**: When an unfamiliar technology is selected, a prototype should be generated.
**Input**: "Use ReactFX for reactive data binding in the TableView — we haven't used it before."
**Expected Output**: Prototype code in `architecture/prototype/` with README.md, prototype result recorded in handoff.
**Pass Criteria**: `prototype_results` array is non-empty, result is `passed`/`failed`/`inconclusive`, prototype directory has at least one `.java` file and a `README.md`.

### TC-11: Developer Instructions Completeness
**Description**: Handoff must include actionable instructions for the developer.
**Input**: Full architecture design.
**Expected Output**: `developer_instructions` with package_structure, layering_rule, naming_convention, key_constraints.
**Pass Criteria**: All 4 fields are non-empty strings (or non-empty array for key_constraints). Naming convention includes patterns for Controller/ViewModel/Service/Repository. Key constraints has at least 2 entries.

### TC-12: ADR Superseding Process
**Description**: When a decision is changed, the old ADR is superseded correctly.
**Input**: Existing ADR-002 (Accepted: Use SQLite). User says "We're switching to PostgreSQL."
**Expected Output**: ADR-002 status changed to `Superseded by ADR-005`, new ADR-005 created with `Supersedes ADR-002` in Context.
**Pass Criteria**: ADR-002 status is `Superseded by ADR-005`, ADR-005 Context mentions `Supersedes ADR-002`, ADR-002 content (besides status) is unchanged.

### TC-13: Layering Strategy Dependency Rules
**Description**: Layering must enforce no-upward-dependency rule.
**Input**: Requirements for a 4-layer JavaFX app.
**Expected Output**: Layer table with correct dependency direction.
**Pass Criteria**: Domain layer has no dependencies. Infrastructure depends on Domain (implements interfaces). Application depends on Domain. Presentation depends on Application. No layer depends upward.

### TC-14: Standalone Mode (No Developer Trigger)
**Description**: Architect can run without triggering developer.
**Input**: "Design the architecture only — don't generate code yet."
**Expected Output**: Architecture artifacts produced, no `architecture-handoff.json` consumed by developer, no code generated.
**Pass Criteria**: `architecture-handoff.json` exists but no Java source files are generated. Loop state shows `architect_result` but no `rounds` array (developer not triggered).

### TC-15: Dual Output Format
**Description**: Both Markdown and JSON reports are generated by default.
**Input**: Full architecture design request (no .loop-config.json).
**Expected Output**: `architecture-report.md` (human-readable) and `architecture-report.json` (machine-readable).
**Pass Criteria**: Both files exist. JSON validates against schema. Markdown contains all sections from the template. When `.loop-config.json` has `"output_format": "json"`, only JSON is produced.
