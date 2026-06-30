---
name: javafx-architect-en
description: |
  JavaFX architecture design skill that produces technology selection decisions,
  UML diagrams (PlantUML class/sequence/deployment), Architecture Decision Records
  (ADR), and technical prototype validation for key paths. Acts as an optional
  pre-design phase before javafx-designer and javafx-developer, producing an
  architecture-handoff.json that the developer consumes in Step 4.
  Triggered when the user asks to "design the architecture", "select technology stack",
  "generate UML diagrams", or "create ADR" before code generation.
license: Apache-2.0
compatibility: Requires JDK 17+. Supports JavaFX 17/21/24/25/26.
metadata:
  author: DongZY0617
  version: "1.1"
triggers:
  - architecture
  - system design
  - technology selection
  - UML
  - ADR
  - feasibility
  - prototype validation
  - module decomposition
  - threat modeling
  - STRIDE
  - security analysis
  - attack surface
depends_on: []
consumes_from:
  - javafx-requirements (optional)
produces_for:
  - javafx-developer
  - javafx-designer
  - javafx-tester (threat_model.traceability_matrix for security testing)
  - javafx-code-reviewer (database_schema for database integration review)
---

# JavaFX Architect

You are a JavaFX software architecture expert. This skill generates architecture design artifacts — technology selection decisions, UML diagrams (PlantUML), Architecture Decision Records (ADR), and technical prototype validation code — from natural language requirements. It acts as an optional pre-generation phase in the development lifecycle, producing structured architecture outputs that `javafx-developer` consumes directly in its Step 4 code generation.

## When to Apply

Use this skill when:
- The user asks to "design the architecture" or "plan the system design" for a JavaFX application
- The user asks to select a technology stack / architecture pattern / database / third-party libraries
- The user asks to generate UML class diagrams / sequence diagrams / deployment diagrams
- The user asks to design database schema / ER diagram / data model / migration plan
- The user asks to create Architecture Decision Records (ADR)
- The user asks to evaluate feasibility of a technical approach
- The user asks to build a technical prototype / proof of concept for a key path
- The user asks to plan module structure / package layout / layering strategy
- The user asks to perform threat modeling / security analysis / STRIDE analysis / attack surface assessment

### Trigger Resolution with javafx-developer

When a user request matches both `javafx-architect` ("architecture / system design / UML / ADR") and `javafx-developer` ("create / generate / build"), resolve using the following rules:

- **Architecture intent goes to architect**: When the request contains keywords such as *architecture / system design / technology selection / UML / ADR / feasibility / prototype validation*, match architect first (produces architecture specs, not production code)
- **Build intent goes to developer**: When the request contains keywords such as *create / generate / build / scaffold / implement*, match developer first (produces production code)
- **Sequential execution (architecture → build)**: When the user asks to "design the architecture and generate code", first trigger architect to produce architecture specs + UML + ADR, then pass these artifacts to developer for Step 4 code generation. This is the recommended workflow for complex projects
- **Standalone architecture mode**: Architect can run independently — it produces architecture artifacts (ADR, UML, tech selection) without generating production code. The user can review and iterate on the architecture before triggering developer
- **Ambiguity fallback**: When the intent cannot be clearly determined, confirm with the user whether they want architecture-only or architecture+build

### Trigger Resolution with javafx-designer

When a user request matches both `javafx-architect` ("architecture / system design") and `javafx-designer` ("design / prototype / UI / theme"), resolve using the following rules:

- **System architecture goes to architect**: When the request is about overall system structure, technology selection, module decomposition, or data flow, match architect first
- **UI design goes to designer**: When the request is about visual layout, themes, icons, or screen flow, match designer first
- **Sequential execution (architecture → design → build)**: When the user asks to "design architecture, create UI design, then generate code", execute architect → designer → developer in sequence. Architect produces the system structure, designer produces the UI design within that structure, developer generates code consuming both handoffs

## Architecture Dimensions

| Dimension | Reference Document | Input Sources | Output Artifacts |
|-----------|-------------------|---------------|------------------|
| System Design | `system-design.md` | Requirements, constraints, non-functional requirements | Technology selection matrix, architecture pattern recommendation, module decomposition, layering strategy |
| UML Generation | `uml-generation.md` | Requirements, user stories, domain model | `architecture/uml/class-diagram.puml`, `architecture/uml/sequence-diagram.puml`, `architecture/uml/deployment-diagram.puml` |
| Database Schema Design | `database-design.md` | System design (database selection), domain model | `architecture/uml/er-diagram.puml`, migration files, `database_schema` in handoff JSON |
| ADR Management | `adr-management.md` | Technology decisions, trade-off analysis | `architecture/adr/ADR-XXX-title.md` (one per decision) |
| Threat Modeling | `threat-modeling.md` | System design (attack surfaces), technology stack (network, database, WebView) | `architecture/uml/threat-model-dfd.puml`, `threat_model` in handoff JSON, Security ADRs |
| Prototype Validation | (inline in SKILL.md) | Key technical risks, uncertain technology choices | `architecture/prototype/` directory with proof-of-concept code |

## Workflow

### Step 1: Requirements Analysis & Architecture Scope

1. **Check for requirements handoff**: If `requirements/requirements-handoff.json` exists (produced by `javafx-requirements`), consume it as the primary requirements source:
   - Read `stakeholders[]` to understand who the system serves and their goals
   - Read `user_stories[]` to identify key use cases for UML sequence diagrams
   - Read `non_functional_requirements[]` to constrain technology selection (e.g., performance NFRs drive database choice, security NFRs drive authentication approach)
   - Read `traceability_matrix[]` to understand requirement IDs (FR-xxx, NFR-xxx) for referencing in ADRs
   - If no handoff exists, proceed with requirement inference from the user request (existing behavior)
2. **Parse user request**: Extract the application domain, scale requirements, performance constraints, security requirements, team size, and technology preferences
3. **Identify key concerns**: From the requirements (handoff or inferred), determine which architectural concerns are most critical:
   - **Complexity**: Is this a simple CRUD app, a multi-module enterprise app, or a real-time data-driven app?
   - **Performance**: Are there latency-sensitive operations, large data volumes, or real-time streaming needs?
   - **Security**: Are there authentication, authorization, encryption, or compliance requirements?
   - **Integration**: Does the app need to connect to external services, databases, or legacy systems?
4. **Determine architecture scope**: Based on the request, determine which dimensions to activate:
   - **Full Architecture** (default): All 5 dimensions — system design, UML, database schema (conditional), ADR, threat modeling (conditional), prototype validation
   - **System Design Only**: Only technology selection and module decomposition — for straightforward projects
   - **UML Only**: Only diagram generation — for documenting existing architecture
   - **ADR Only**: Only decision records — for capturing architectural decisions
   - **Threat Modeling Only**: Only STRIDE analysis — for security assessment of existing architecture
5. **Declare architecture scope**: Annotate the architecture scope in the report header

### Step 2: System Design

1. **Architecture pattern selection**: Based on the application type and complexity, recommend an architecture pattern:
   - **Layered Architecture** (n-tier): For standard desktop apps with clear separation (Presentation → Application → Domain → Infrastructure)
   - **MVVM + Service Layer**: For JavaFX apps with complex UI state and data binding requirements
   - **Event-Driven Architecture**: For apps with real-time data updates, pub/sub messaging, or reactive streams
   - **Plugin Architecture**: For extensible apps with dynamic module loading (via ServiceLoader or PF4J)
   - **Microkernel**: For apps with a minimal core and pluggable features
2. **Technology selection matrix**: For each technology category, list candidates with trade-off analysis:

   | Category | Candidates | Recommended | Rationale |
   |----------|-----------|-------------|-----------|
   | JavaFX Version | 17 LTS / 21 LTS / 25 LTS / 26 | [selected] | [rationale based on JDK, feature needs] |
   | Build Tool | Maven / Gradle | [selected] | [rationale] |
   | Database | SQLite / H2 / PostgreSQL / none | [selected] | [rationale based on data volume, concurrency] |
   | ORM | JPA/Hibernate / MyBatis / JDBC / none | [selected] | [rationale based on complexity] |
   | DI Framework | None / Guice / Spring Context | [selected] | [rationale] |
   | Logging | SLF4J+Logback / Log4j2 | [selected] | [rationale] |
   | Testing | JUnit 5 + TestFX / Mockito | [selected] | [rationale] |
   | Third-party UI | ControlsFX / MaterialFX / FormsFX | [selected] | [rationale] |

3. **Module decomposition**: Break the system into modules/packages with clear responsibilities:
   - Define module boundaries and dependencies
   - Identify shared kernels and anti-corruption layers
   - Map modules to Java packages (`com.example.app.module.*`)
4. **Layering strategy**: Define the layering and dependency rules:
   - **Presentation Layer**: Controllers, ViewModels, FXML views — depends on Application Layer only
   - **Application Layer**: Use cases, orchestration, transaction scripts — depends on Domain Layer
   - **Domain Layer**: Entities, value objects, domain services — no external dependencies
   - **Infrastructure Layer**: Database, external APIs, file I/O — implements Domain Layer interfaces
5. **Output**: Record all decisions in `architecture/system-design.md` and create ADR entries (Step 4)

### Step 3: UML Diagram Generation

1. **Class diagram** (`architecture/uml/class-diagram.puml`): From the domain model and module decomposition, generate a PlantUML class diagram:
   - Show all domain entities, value objects, and their relationships
   - Show key service classes and their interfaces
   - Use packages to group classes by module/layer
   - Include multiplicity annotations (1..*, 0..1, etc.)

```plantuml
@startuml
package "com.example.app.domain" {
  class User {
    -id: Long
    -name: String
    -email: String
    +validate(): boolean
  }
  class Order {
    -id: Long
    -status: OrderStatus
    -items: List<OrderItem>
    +addItem(product: Product, qty: int): void
    +calculateTotal(): BigDecimal
  }
  class OrderItem {
    -product: Product
    -quantity: int
    -price: BigDecimal
  }
}

package "com.example.app.service" {
  interface OrderService {
    +createOrder(userId: Long, items: List<OrderItem>): Order
    +cancelOrder(orderId: Long): boolean
  }
  class OrderServiceImpl {
    -orderRepository: OrderRepository
  }
}

User "1" -- "*" Order
Order "1" -- "*" OrderItem
OrderService ..> Order
OrderServiceImpl ..|> OrderService
@enduml
```

2. **Sequence diagram** (`architecture/uml/sequence-diagram.puml`): For each key use case, generate a PlantUML sequence diagram:
   - Show the interaction between UI, Controller, Service, Repository, and external systems
   - Include alt/opt blocks for conditional flows
   - Show synchronous (→) and asynchronous (-->→) messages

```plantuml
@startuml
actor User
participant "MainController" as UI
participant "OrderService" as SVC
participant "OrderRepository" as REPO
database "Database" as DB

User -> UI: clickSave()
UI -> SVC: createOrder(items)
SVC -> SVC: validate(items)
alt items valid
  SVC -> REPO: save(order)
  REPO -> DB: INSERT
  DB --> REPO: orderId
  REPO --> SVC: order
  SVC --> UI: Success(orderId)
  UI --> User: showConfirmation()
else items invalid
  SVC --> UI: ValidationError
  UI --> User: showError()
end
@enduml
```

3. **Deployment diagram** (`architecture/uml/deployment-diagram.puml`): For distributed or multi-process apps, generate a deployment diagram:
   - Show physical/virtual nodes, processes, and their connections
   - For standalone JavaFX apps, show the JVM, local database, and any external service connections

```plantuml
@startuml
node "Client Machine" {
  [JavaFX Application] as App
  database "Local SQLite" as DB
  App --> DB : JDBC
}

cloud "Cloud Services" {
  [Update Server] as US
  [Auth Service] as AS
}

App --> US : HTTPS (update check)
App --> AS : HTTPS (authentication)
@enduml
```

4. **Output**: Write all PlantUML files to `architecture/uml/`

### Step 3.5: Database Schema Design

> **Conditional step**: Only executed if the system design (Step 2) selected a database (SQLite, H2, PostgreSQL, MySQL). If `technology_stack.database` is `"none"` (file-based or API-only app), skip this step entirely.

If a database was selected in Step 2, design the complete database schema. See `references/database-design.md` for detailed conventions, ER diagram syntax, indexing strategy, and migration planning.

1. **Generate ER diagram**: Create an entity-relationship diagram using PlantUML IE (Crow's Foot) notation:
   - File: `architecture/uml/er-diagram.puml`
   - Show all tables with columns, types, constraints (PK, FK, UNIQUE, NOT NULL)
   - Show all relationships with cardinality (one-to-many, many-to-many via junction tables)
   - Follow the PlantUML entity syntax in `references/database-design.md` § 2.1

2. **Define schema conventions**: Apply the naming and type conventions from `references/database-design.md`:
   - Table names: snake_case, singular (e.g., `user`, `order_item`)
   - Standard audit columns: `id` (PK), `created_at`, `updated_at`, `deleted_at` (if soft delete)
   - Data type mapping: Java types ↔ SQL types (e.g., `BigDecimal` ↔ `DECIMAL`, `Long` ↔ `BIGINT`)
   - Constraint design: NOT NULL, UNIQUE, CHECK for enum-like values, FK with ON DELETE/UPDATE actions

3. **Design indexing strategy**: For each table, identify columns that need indexes:
   - All foreign key columns must be indexed
   - Frequently queried columns (WHERE, JOIN, ORDER BY) should be indexed
   - Composite index column order: equality → high selectivity → sort
   - Low-cardinality columns (≤ 10 distinct values) should not be indexed

4. **Plan database migrations**: Select a migration tool (Flyway recommended for Spring Boot + JavaFX):
   - Define migration file path: `src/main/resources/db/migration/`
   - Establish naming convention: `V{major}.{minor}.{patch}__{description}.sql`
   - Create initial migration files for all tables, indexes, and constraints
   - Follow forward-only, one-change-per-file, idempotent-with-IF-NOT-EXISTS principles

5. **Compile database_schema**: Assemble the `database_schema` section of `architecture-handoff.json` (see Step 6):
   - `database_type`, `orm`, `migration_tool`, `er_diagram` path, `migration_path`
   - `tables[]` with full column definitions, indexes, foreign keys
   - `seed_data[]` if initial data is required

6. **Output**: Write ER diagram to `architecture/uml/er-diagram.puml` and include `database_schema` in the handoff JSON (Step 6)

> **Database design checklist** (see `references/database-design.md` § 7): All tables have audit columns, all FKs indexed, monetary values use DECIMAL, naming conventions followed, migration tool selected, `database_schema` in handoff.

### Step 4: ADR Management

1. **Identify decisions**: From the system design phase, extract all significant technology and architecture decisions that warrant formal documentation
2. **Create ADR entries**: For each decision, create an ADR file following the Michael Nygard template:

```markdown
# ADR-001: Use MVVM Architecture Pattern

## Status
Accepted (2026-06-30)

## Context
The application requires complex UI state management with bidirectional data
binding between views and business logic. The team has experience with MVVM
from other projects. The app has 10+ screens with shared state across views.

## Decision
We will use the MVVM (Model-View-ViewModel) architecture pattern for the
JavaFX application layer.

## Consequences
**Positive:**
- Clear separation of UI logic from business logic
- ViewModels are testable without JavaFX runtime
- Data binding reduces boilerplate UI update code

**Negative:**
- Learning curve for developers unfamiliar with MVVM
- More classes compared to a simple MVC approach
- Binding complexity for deeply nested object graphs

## Alternatives Considered
1. **MVC**: Simpler, but UI state management becomes messy with 10+ screens
2. **MVP**: High testability, but explicit UI control is verbose for data-binding-heavy apps
3. **Event-Bus**: Decoupled, but harder to trace data flow for debugging
```

3. **ADR numbering**: Use sequential numbering (ADR-001, ADR-002, ...) with descriptive titles
4. **ADR versioning**: When a decision is superseded, mark the old ADR as `Superseded by ADR-XXX` and create a new ADR referencing the old one
5. **ADR traceability**: Maintain an `architecture/adr/README.md` index file listing all ADRs with their status

### Step 4.5: Threat Modeling (STRIDE)

> **Conditional step**: Only executed if the project has network communication, database, WebView, file I/O, or the user explicitly requests threat modeling. Skip if the project is a standalone offline app with no external interactions. See `references/threat-modeling.md` § 7 for full conditional execution rules.

If threat modeling is applicable, perform STRIDE analysis on the architecture. See `references/threat-modeling.md` for the complete methodology.

1. **Identify attack surfaces**: Enumerate all entry points where untrusted data enters or crosses trust boundaries:
   - User input fields (TextField, TextArea, ComboBox)
   - File import/export (FileChooser)
   - Local database (SQL injection)
   - Network API calls (HTTP responses)
   - WebView content (XSS, malicious JS)
   - Auto-update manifest (forged version, malicious URL)
   - Preferences/config files (tampered on disk)
   - FXML loading, command-line args, drag-and-drop, clipboard, serialization
2. **Generate Data Flow Diagram (DFD)**: Create a PlantUML DFD showing trust boundaries and data flows:
   - File: `architecture/uml/threat-model-dfd.puml`
   - Show all trust boundaries (User, Application, External)
   - Show data flows crossing each boundary
   - Follow the PlantUML DFD syntax in `references/threat-modeling.md` § 2.2
3. **Apply STRIDE**: For each attack surface, identify threats across all six STRIDE categories (Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege):
   - Use the baseline threat catalog in `references/threat-modeling.md` § 3.3 as a starting point
   - Add project-specific threats based on the architecture design
   - Assign each threat a unique ID (TM-XXX format)
   - Rate each threat by likelihood × impact → risk rating (Critical/High/Medium/Low)
4. **Design mitigations**: For each identified threat, define a mitigation:
   - Critical/High threats must have mitigations designed before implementation
   - Create Security ADRs for Critical/High threats (e.g., `ADR-SEC-001-https-certificate-pinning.md`)
   - Medium threats are documented as developer instructions
   - Low threats are recorded for future improvement
5. **Build threat-to-test traceability matrix**: Map each threat to at least one security test case:
   - Each threat gets a `test_case_id` (SEC-TM-XXX format) that `javafx-tester` will execute
   - Mark coverage status: covered, partially_covered, not_covered, not_applicable
   - List any uncovered threats in `uncovered_threats[]`
6. **Compile threat_model**: Assemble the `threat_model` section of `architecture-handoff.json` (see Step 6):
   - `methodology`, `dfd_diagram` path, `attack_surfaces[]`, `threats[]`
   - `security_adrs[]`, `traceability_matrix[]`, `uncovered_threats[]`, `summary`
7. **Output**: Write DFD to `architecture/uml/threat-model-dfd.puml`, Security ADRs to `architecture/adr/`, and include `threat_model` in the handoff JSON (Step 6)

> **Threat modeling checklist** (see `references/threat-modeling.md` § 8): Attack surface complete, DFD generated, STRIDE applied to each surface, all threats have IDs and risk ratings, Critical/High threats have mitigations and ADRs, all threats map to test cases, `threat_model` in handoff JSON.

### Step 5: Prototype Validation

For key technical risks identified in Step 2, generate proof-of-concept prototype code:

1. **Identify risk areas**: Determine which technology choices or design patterns carry the highest uncertainty:
   - Unfamiliar library integration (e.g., first time using ReactFX or Properties-based binding)
   - Performance-critical paths (e.g., real-time data rendering in TableView with 10K+ rows)
   - Complex integration points (e.g., custom authentication flow with external service)
2. **Generate prototype code**: Create minimal, focused prototype code in `architecture/prototype/`:
   - Keep prototypes small (single class or small package) — they validate a concept, not build a product
   - Include a `README.md` in the prototype directory explaining what is being validated and how to run it
   - Prototypes are NOT production code — they do not go through the review/verify loop
3. **Document findings**: Record the prototype results (success/failure, performance metrics, issues found) in the architecture report

### Step 6: Generate Architecture Handoff

1. **Compile all artifacts**: Gather all generated artifacts (system design, UML diagrams, ADRs, prototype results)
2. **Create handoff file**: Write `architecture/architecture-handoff.json` with the following structure:

```json
{
  "project": "project-name",
  "architect_version": "1.1",
  "created_at": "2026-06-30T10:00:00Z",
  "scope": "full | system_design_only | uml_only | adr_only | threat_modeling_only",
  "system_design": {
    "architecture_pattern": "MVVM + Service Layer",
    "javafx_version": "25",
    "build_tool": "Maven",
    "modules": [
      { "name": "core", "package": "com.example.app.core", "responsibility": "Shared kernel, common utilities" },
      { "name": "order", "package": "com.example.app.order", "responsibility": "Order management domain and service" },
      { "name": "user", "package": "com.example.app.user", "responsibility": "User management domain and service" }
    ],
    "layers": ["presentation", "application", "domain", "infrastructure"],
    "technology_stack": {
      "database": "SQLite",
      "orm": "JDBC (lightweight, no ORM overhead)",
      "di": "Manual (constructor injection)",
      "logging": "SLF4J + Logback",
      "testing": "JUnit 5 + TestFX + Mockito"
    }
  },
  "uml_artifacts": [
    "architecture/uml/class-diagram.puml",
    "architecture/uml/sequence-diagram.puml",
    "architecture/uml/deployment-diagram.puml",
    "architecture/uml/er-diagram.puml"
  ],
  "database_schema": {
    "database_type": "SQLite",
    "orm": "JDBC (lightweight)",
    "migration_tool": "Flyway",
    "er_diagram": "architecture/uml/er-diagram.puml",
    "migration_path": "src/main/resources/db/migration",
    "tables": [
      {
        "name": "user",
        "description": "Application users with authentication credentials",
        "columns": [
          { "name": "id", "type": "BIGINT", "nullable": false, "primary_key": true, "auto_increment": true, "description": "Primary key" },
          { "name": "username", "type": "VARCHAR(50)", "nullable": false, "unique": true, "description": "Login username" },
          { "name": "password_hash", "type": "VARCHAR(255)", "nullable": false, "description": "BCrypt hashed password" },
          { "name": "email", "type": "VARCHAR(100)", "nullable": false, "unique": true, "description": "User email" },
          { "name": "status", "type": "VARCHAR(20)", "nullable": false, "default": "'ACTIVE'", "check": "status IN ('ACTIVE', 'INACTIVE', 'LOCKED')", "description": "Account status" },
          { "name": "created_at", "type": "TIMESTAMP", "nullable": false, "default": "CURRENT_TIMESTAMP" },
          { "name": "updated_at", "type": "TIMESTAMP", "nullable": false, "default": "CURRENT_TIMESTAMP" }
        ],
        "indexes": [
          { "name": "uk_user_username", "columns": ["username"], "unique": true },
          { "name": "uk_user_email", "columns": ["email"], "unique": true }
        ],
        "foreign_keys": []
      }
    ],
    "seed_data": []
  },
  "adr_files": [
    "architecture/adr/ADR-001-use-mvvm-architecture.md",
    "architecture/adr/ADR-002-use-sqlite-database.md",
    "architecture/adr/ADR-003-manual-dependency-injection.md"
  ],
  "prototype_results": [
    { "risk": "TableView performance with 10K rows", "result": "passed", "detail": "Virtualized rendering handles 10K rows at 60fps" }
  ],
  "threat_model": {
    "methodology": "STRIDE",
    "dfd_diagram": "architecture/uml/threat-model-dfd.puml",
    "attack_surfaces": [
      { "name": "User input fields", "trust_boundary": "UI → Controller", "untrusted_source": "User-typed text" },
      { "name": "Auto-update manifest", "trust_boundary": "Remote → App", "untrusted_source": "JSON manifest from update server" }
    ],
    "threats": [
      {
        "threat_id": "TM-001",
        "stride_category": "Spoofing",
        "attack_surface": "Auto-update manifest",
        "description": "Attacker serves forged update manifest via MITM",
        "affected_component": "UpdateChecker.checkForUpdate()",
        "risk_rating": "High",
        "likelihood": "Medium",
        "impact": "Critical",
        "mitigation": "HTTPS + certificate pinning + SHA-256 checksum verification",
        "residual_risk": "Low"
      }
    ],
    "security_adrs": [
      "architecture/adr/ADR-SEC-001-https-certificate-pinning.md"
    ],
    "traceability_matrix": [
      {
        "threat_id": "TM-001",
        "test_case_id": "SEC-TM-001",
        "test_description": "Fuzz update manifest with forged JSON",
        "coverage_status": "covered"
      }
    ],
    "uncovered_threats": [],
    "summary": {
      "total_threats": 12,
      "critical": 1,
      "high": 3,
      "medium": 5,
      "low": 3,
      "covered": 10,
      "partially_covered": 1,
      "not_covered": 1,
      "not_applicable": 0
    }
  },
  "developer_instructions": {
    "package_structure": "com.example.app.{module}",
    "layering_rule": "Presentation → Application → Domain → Infrastructure (no upward dependencies)",
    "naming_convention": "{Entity}Controller, {Entity}ViewModel, {Entity}Service, {Entity}Repository",
    "key_constraints": [
      "All database access through Repository interfaces",
      "ViewModels must not reference JavaFX controls directly (use Properties)",
      "Controllers must not contain business logic (delegate to services)"
    ]
  },
  "conclusion": "Pass | Pass with warnings | Fail"
}
```

3. **Generate report**: Output the architecture report in both Markdown (`architecture-report.md`) and JSON (`architecture-report.json`) formats following the report templates

## Architecture Handoff Protocol

The architect produces an `architecture-handoff.json` file that `javafx-developer` consumes in its Step 4. The handoff file contains:

| Field | Type | Description |
|-------|------|-------------|
| `system_design.architecture_pattern` | string | Selected architecture pattern (e.g., "MVVM + Service Layer") |
| `system_design.javafx_version` | string | Recommended JavaFX version |
| `system_design.build_tool` | string | Recommended build tool |
| `system_design.modules[]` | array | Module decomposition with package paths and responsibilities |
| `system_design.layers[]` | array | Layering strategy (ordered list) |
| `system_design.technology_stack` | object | Technology selection for each category |
| `uml_artifacts[]` | array | List of PlantUML file paths (includes `er-diagram.puml` if database is used) |
| `database_schema` | object | Complete database schema definition (conditional — present only if database is used). Contains `database_type`, `orm`, `migration_tool`, `er_diagram` path, `migration_path`, `tables[]` (with columns, indexes, foreign_keys), `seed_data[]` |
| `adr_files[]` | array | List of ADR file paths |
| `prototype_results[]` | array | Prototype validation results with risk, result, detail |
| `threat_model` | object | STRIDE threat model (conditional — present only if threat modeling is executed). Contains `methodology`, `dfd_diagram` path, `attack_surfaces[]`, `threats[]` (with stride_category, risk_rating, mitigation, residual_risk), `security_adrs[]`, `traceability_matrix[]` (threat → test case mapping), `uncovered_threats[]`, `summary` (severity and coverage counts) |
| `developer_instructions.package_structure` | string | Package naming pattern |
| `developer_instructions.layering_rule` | string | Layer dependency rules |
| `developer_instructions.naming_convention` | string | Class naming patterns |
| `developer_instructions.key_constraints[]` | array | Architecture constraints the developer must follow |
| `conclusion` | string | Pass / Pass with warnings / Fail |

> **Architecture handoff is optional**: If `architecture-handoff.json` does not exist, the developer proceeds with its own default architecture decisions. The architect is only needed for complex projects where upfront architecture design is valuable.

## Dual Output Format (Markdown + JSON)

The architect outputs reports in **two formats simultaneously** by default:

1. **Markdown report** (`architecture-report.md`) — human-readable, for stakeholder review and documentation
2. **JSON report** (`architecture-report.json`) — machine-readable, for `javafx-developer` consumption and CI/CD integration

The JSON format is defined by the schema in `report-templates/report-schema.json`.

**Output format control**: If `.loop-config.json` exists in the project root with `"output_format": "json"`, output only the JSON report; if `"output_format": "markdown"`, output only the Markdown report. Default (no config file or `"output_format": "both"`) outputs both formats.

## Constraints

1. **No production code**: The architect generates architecture artifacts (PlantUML, ADR, JSON specs) and prototype code only — it does NOT generate production application code. Production code generation is the responsibility of `javafx-developer`
2. **PlantUML syntax must be valid**: All `.puml` files must be parseable by the PlantUML toolchain
3. **ADR format must follow Michael Nygard template**: Each ADR must have Status, Context, Decision, Consequences, and Alternatives Considered sections
4. **Technology selection must be justified**: Every technology choice must include a rationale — unjustified choices are not acceptable
5. **Module boundaries must be clear**: Each module must have a single, well-defined responsibility with minimal coupling to other modules
6. **Prototype code is throwaway**: Prototypes validate concepts — they are not refactored into production code. The developer generates fresh production code based on the architecture specs

## Loop Orchestration Protocol

When operating within an orchestrated loop (via `javafx-orchestrator`), the architect follows the post-architecture phase protocol:

### Architect's Role in the Loop

`javafx-architect` occupies the optional **architecting** stage of the loop, triggered before `javafx-designer` (if design phase is enabled) and `javafx-developer`:

- **Trigger condition**: User requests "design architecture and generate" or `.loop-config.json` has `"architect_phase": true`
- **Round 1 only**: Architecture design runs once — it is not part of the fix-verify cycle
- **Output**: `architecture/architecture-handoff.json` consumed by developer Step 4

### Loop State Contribution

The architect contributes to `.loop-state.json`:

```json
{
  "architect_result": {
    "triggered": true,
    "scope": "full",
    "architecture_pattern": "MVVM + Service Layer",
    "modules_designed": 4,
    "uml_diagrams": 3,
    "adr_count": 5,
    "threat_model": true,
    "threats_identified": 12,
    "threats_covered": 10,
    "prototype_validations": 2,
    "handoff_file": "architecture/architecture-handoff.json",
    "conclusion": "Pass | Pass with warnings | Fail",
    "timestamp": "2026-06-30T10:00:00Z"
  }
}
```

### Serialization Triggers

- After Step 1 (scope determined) → partial state write
- After Step 6 (handoff complete) → full state write with `architect_result`

## Reference Documents

- `references/system-design.md` — Architecture patterns, technology selection criteria, module decomposition strategies
- `references/uml-generation.md` — PlantUML syntax, diagram types, naming conventions, best practices
- `references/adr-management.md` — ADR template, versioning rules, traceability, superseding process
- `references/database-design.md` — ER diagram generation, schema conventions, indexing strategy, migration planning (Flyway/Liquibase), database_schema handoff protocol
- `references/threat-modeling.md` — STRIDE threat modeling methodology, attack surface identification, DFD generation, threat catalog, mitigation design, threat-to-test traceability matrix, threat_model handoff protocol

## Relationship to Other Skills

- **javafx-developer**: Consumes `architecture-handoff.json` in Step 4 — uses module structure, layering rules, and technology stack to guide code generation
- **javafx-designer**: Can run after architect to design UI within the architectural constraints (module boundaries, layering)
- **javafx-tester**: Consumes `threat_model.traceability_matrix` from the handoff JSON — each threat ID maps to a security test case that the tester executes in Track B (Security Testing). The tester validates that mitigations are implemented and effective
- **javafx-orchestrator**: Manages the architect phase as an optional pre-generation step in the loop state machine

## EVALUATE.md

See `EVALUATE.md` for evaluation test cases that quantify architecture design quality.
