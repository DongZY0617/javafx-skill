# EVALUATE.md — javafx-architect

> 25 evaluation test cases that quantify architecture design quality.
> Each test case includes: description, input, expected output, and pass criteria.

## Test Cases

### TC-01: Full Architecture Design (Default Scope)
**Description**: User requests full architecture design for a new JavaFX e-commerce desktop app.
**Input**: "Design the architecture for a JavaFX e-commerce app with order management, inventory, and user authentication. Use SQLite and MVVM."
**Expected Output**: All 6 dimensions activated. System design with technology matrix, 3+ UML diagrams (class/sequence/deployment + optional ER diagram for database), 3+ ADRs, database schema (tables + migration plan), threat model (DFD + STRIDE catalog + security ADRs), architecture-handoff.json.
**Pass Criteria**: `architecture-handoff.json` exists with `scope: "full"`, `conclusion: "Pass"`, all 6 dimension artifacts present.

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

### TC-16: Database Schema Design — ER Diagram Generation
**Description**: When a database is selected in system design, an ER diagram must be generated.
**Input**: "Design the architecture for a JavaFX inventory app with SQLite. Include user management, products, and orders."
**Expected Output**: `architecture/uml/er-diagram.puml` generated with PlantUML entity syntax showing all tables, columns, PK/FK markers, and Crow's Foot relationship notation.
**Pass Criteria**:
- [ ] `er-diagram.puml` file exists in `architecture/uml/`
- [ ] File starts with `@startuml` and ends with `@enduml`
- [ ] All entity blocks have `id` primary key marked with `<<PK>>`
- [ ] Foreign key columns marked with `<<FK>>`
- [ ] Relationships use Crow's Foot notation (`||--o{`, `||--|{`, etc.)
- [ ] `uml_artifacts` array in handoff JSON includes `er-diagram.puml` path

### TC-17: Database Schema Design — Handoff JSON Structure
**Description**: The `database_schema` section must be present and valid in the handoff JSON.
**Input**: Full architecture design request with database (SQLite + MyBatis).
**Expected Output**: `architecture-handoff.json` contains `database_schema` object with complete table definitions.
**Pass Criteria**:
- [ ] `database_schema` field present in handoff JSON
- [ ] `database_schema.database_type` is "SQLite"
- [ ] `database_schema.orm` is "MyBatis"
- [ ] `database_schema.migration_tool` is "Flyway" or "Liquibase"
- [ ] `database_schema.migration_path` is "src/main/resources/db/migration"
- [ ] `database_schema.tables[]` has at least one table with columns, indexes, and foreign_keys arrays
- [ ] Each table has `id` column with `primary_key: true` and `auto_increment: true`
- [ ] Each table has `created_at` and `updated_at` columns
- [ ] All foreign key columns have corresponding index entries

### TC-18: Database Schema Design — No Database (Skip Condition)
**Description**: When the system design selects "none" for database, Step 3.5 is skipped.
**Input**: "Design architecture for a JavaFX app that uses file-based JSON storage — no database needed."
**Expected Output**: System design records `database: "none"`. No ER diagram generated. No `database_schema` in handoff JSON.
**Pass Criteria**:
- [ ] `system_design.technology_stack.database` is "none"
- [ ] No `er-diagram.puml` file generated
- [ ] `database_schema` field absent from `architecture-handoff.json`
- [ ] `uml_artifacts` array does not include ER diagram path
- [ ] No migration files generated

### TC-19: Database Schema Design — Indexing Strategy
**Description**: All foreign key columns must be indexed, and composite indexes follow correct column order.
**Input**: "Design schema for an order management system with orders, order items, users, and products."
**Expected Output**: `database_schema.tables[]` includes indexes for all FK columns and appropriate composite indexes.
**Pass Criteria**:
- [ ] Every table with foreign keys has index entries for each FK column
- [ ] `order.user_id` has an index (e.g., `idx_order_user_id`)
- [ ] `order_item.order_id` has an index (e.g., `idx_order_item_order_id`)
- [ ] Junction table `user_role` (if many-to-many) has indexes on both FK columns
- [ ] Composite indexes (if any) have column order: equality → high selectivity → sort
- [ ] Index names follow convention: `idx_{table}_{columns}` or `uk_{table}_{columns}`

### TC-20: Database Schema Design — Migration Planning
**Description**: Migration tool must be selected and migration file path defined.
**Input**: "Design architecture with PostgreSQL and Flyway for a multi-user JavaFX app."
**Expected Output**: Migration tool configured, file path defined, Flyway dependency recommended.
**Pass Criteria**:
- [ ] `database_schema.migration_tool` is "Flyway"
- [ ] `database_schema.migration_path` is "src/main/resources/db/migration"
- [ ] Technology stack in system design includes Flyway dependency
- [ ] An ADR exists documenting the migration tool choice
- [ ] Seed data (if required) is listed in `database_schema.seed_data[]` with migration file paths

### TC-21: Threat Modeling — DFD Generation
**Description**: Verify the architect generates a valid PlantUML Data Flow Diagram with trust boundaries.
**Input**: "Design architecture for a JavaFX app with REST API integration, SQLite database, and WebView. Include threat modeling."
**Expected Output**: `architecture/uml/threat-model-dfd.puml` generated with trust boundary rectangles, data flows, and all attack surfaces represented.
**Pass Criteria**:
- [ ] `threat-model-dfd.puml` file exists in `architecture/uml/`
- [ ] File starts with `@startuml` and ends with `enduml`
- [ ] DFD contains at least 3 trust boundary rectangles (User, Application, External)
- [ ] All attack surfaces from the system design are represented (UI, database, API, WebView)
- [ ] Data flows show direction arrows with labels
- [ ] `threat_model.dfd_diagram` in handoff JSON points to the file path

### TC-22: Threat Modeling — STRIDE Coverage
**Description**: Verify all six STRIDE categories are applied to the identified attack surfaces.
**Input**: "Perform STRIDE threat modeling for my JavaFX app with network API, local SQLite, and auto-update."
**Expected Output**: `threat_model.threats[]` in handoff JSON contains threats from all six STRIDE categories (Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege).
**Pass Criteria**:
- [ ] `threat_model.threats[]` array has at least 6 entries
- [ ] At least one threat has `stride_category: "Spoofing"`
- [ ] At least one threat has `stride_category: "Tampering"`
- [ ] At least one threat has `stride_category: "Repudiation"`
- [ ] At least one threat has `stride_category: "Information Disclosure"`
- [ ] At least one threat has `stride_category: "Denial of Service"`
- [ ] At least one threat has `stride_category: "Elevation of Privilege"`
- [ ] Each threat has a unique `threat_id` matching `^TM-` pattern
- [ ] Each threat has `risk_rating` (Critical/High/Medium/Low)

### TC-23: Threat Modeling — Traceability Matrix
**Description**: Each threat must map to at least one security test case in the traceability matrix.
**Input**: "Design architecture with threat modeling for a JavaFX app with user authentication and file import."
**Expected Output**: `threat_model.traceability_matrix[]` contains entries mapping each threat to a `SEC-TM-XXX` test case.
**Pass Criteria**:
- [ ] `traceability_matrix[]` has at least one entry per identified threat
- [ ] Each entry has `threat_id` matching a threat in `threats[]`
- [ ] Each entry has `test_case_id` matching `^SEC-TM-` pattern
- [ ] Each entry has `coverage_status` (covered/partially_covered/not_covered/not_applicable)
- [ ] Threats with `coverage_status: "not_covered"` are listed in `uncovered_threats[]`
- [ ] `summary.covered + summary.partially_covered + summary.not_covered + summary.not_applicable` equals `summary.total_threats`

### TC-24: Threat Modeling — Security ADRs for Critical/High Threats
**Description**: Critical and High risk threats must have Security ADRs documenting the mitigation.
**Input**: "Design architecture with threat modeling for a JavaFX app with auto-update and REST API."
**Expected Output**: Security ADR files created for all Critical/High threats, listed in `threat_model.security_adrs[]`.
**Pass Criteria**:
- [ ] Every threat with `risk_rating: "Critical"` or `"High"` has a corresponding Security ADR
- [ ] Security ADR files follow the Michael Nygard template (Status, Context, Decision, Consequences)
- [ ] Each Security ADR includes a "Threats Addressed" section listing threat IDs
- [ ] `threat_model.security_adrs[]` lists all Security ADR file paths
- [ ] ADR files are named with `ADR-SEC-` prefix

### TC-25: Threat Modeling — Skip Condition
**Description**: When the project has no network, database, WebView, or file I/O, threat modeling is skipped.
**Input**: "Design architecture for a simple standalone JavaFX calculator app — no network, no database, no file operations."
**Expected Output**: System design records no external interactions. No threat model generated. No `threat_model` in handoff JSON.
**Pass Criteria**:
- [ ] `threat_model` field absent from `architecture-handoff.json`
- [ ] No `threat-model-dfd.puml` file generated
- [ ] No Security ADR files created
- [ ] Architecture report notes "Threat modeling skipped: minimal attack surface"
- [ ] `architect_result.threat_model` is `false` in `.loop-state.json`
