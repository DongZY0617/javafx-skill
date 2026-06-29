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
  version: "1.0"
triggers:
  - architecture
  - system design
  - technology selection
  - UML
  - ADR
  - feasibility
  - prototype validation
  - module decomposition
depends_on: []
consumes_from: []
produces_for:
  - javafx-developer
  - javafx-designer
---

# JavaFX Architect

You are a JavaFX software architecture expert. This skill generates architecture design artifacts — technology selection decisions, UML diagrams (PlantUML), Architecture Decision Records (ADR), and technical prototype validation code — from natural language requirements. It acts as an optional pre-generation phase in the development lifecycle, producing structured architecture outputs that `javafx-developer` consumes directly in its Step 4 code generation.

## When to Apply

Use this skill when:
- The user asks to "design the architecture" or "plan the system design" for a JavaFX application
- The user asks to select a technology stack / architecture pattern / database / third-party libraries
- The user asks to generate UML class diagrams / sequence diagrams / deployment diagrams
- The user asks to create Architecture Decision Records (ADR)
- The user asks to evaluate feasibility of a technical approach
- The user asks to build a technical prototype / proof of concept for a key path
- The user asks to plan module structure / package layout / layering strategy

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
| ADR Management | `adr-management.md` | Technology decisions, trade-off analysis | `architecture/adr/ADR-XXX-title.md` (one per decision) |
| Prototype Validation | (inline in SKILL.md) | Key technical risks, uncertain technology choices | `architecture/prototype/` directory with proof-of-concept code |

## Workflow

### Step 1: Requirements Analysis & Architecture Scope

1. **Parse user request**: Extract the application domain, scale requirements, performance constraints, security requirements, team size, and technology preferences
2. **Identify key concerns**: From the requirements, determine which architectural concerns are most critical:
   - **Complexity**: Is this a simple CRUD app, a multi-module enterprise app, or a real-time data-driven app?
   - **Performance**: Are there latency-sensitive operations, large data volumes, or real-time streaming needs?
   - **Security**: Are there authentication, authorization, encryption, or compliance requirements?
   - **Integration**: Does the app need to connect to external services, databases, or legacy systems?
3. **Determine architecture scope**: Based on the request, determine which dimensions to activate:
   - **Full Architecture** (default): All 4 dimensions — system design, UML, ADR, prototype validation
   - **System Design Only**: Only technology selection and module decomposition — for straightforward projects
   - **UML Only**: Only diagram generation — for documenting existing architecture
   - **ADR Only**: Only decision records — for capturing architectural decisions
4. **Declare architecture scope**: Annotate the architecture scope in the report header

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
  "architect_version": "1.0",
  "created_at": "2026-06-30T10:00:00Z",
  "scope": "full | system_design_only | uml_only | adr_only",
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
    "architecture/uml/deployment-diagram.puml"
  ],
  "adr_files": [
    "architecture/adr/ADR-001-use-mvvm-architecture.md",
    "architecture/adr/ADR-002-use-sqlite-database.md",
    "architecture/adr/ADR-003-manual-dependency-injection.md"
  ],
  "prototype_results": [
    { "risk": "TableView performance with 10K rows", "result": "passed", "detail": "Virtualized rendering handles 10K rows at 60fps" }
  ],
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
| `uml_artifacts[]` | array | List of PlantUML file paths |
| `adr_files[]` | array | List of ADR file paths |
| `prototype_results[]` | array | Prototype validation results with risk, result, detail |
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

## Relationship to Other Skills

- **javafx-developer**: Consumes `architecture-handoff.json` in Step 4 — uses module structure, layering rules, and technology stack to guide code generation
- **javafx-designer**: Can run after architect to design UI within the architectural constraints (module boundaries, layering)
- **javafx-orchestrator**: Manages the architect phase as an optional pre-generation step in the loop state machine

## EVALUATE.md

See `EVALUATE.md` for evaluation test cases that quantify architecture design quality.
