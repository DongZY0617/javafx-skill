# Architecture Design Report

> **Project**: [project-name]
> **Architect Version**: 1.1
> **Date**: [YYYY-MM-DD]
> **Scope**: [full | system_design_only | uml_only | adr_only | threat_modeling_only]
> **Conclusion**: [Pass | Pass with warnings | Fail]

---

## 1. Executive Summary

[Brief summary of the architecture design — what was analyzed, what pattern was selected, how many ADRs were created, and whether prototype validation passed.]

## 2. Architecture Scope

| Dimension | Activated | Artifacts Produced |
|-----------|-----------|-------------------|
| System Design | [Yes/No] | [count] |
| UML Generation | [Yes/No] | [count] diagrams |
| ADR Management | [Yes/No] | [count] ADRs |
| Database Schema Design | [Yes/No] | [count] tables, [count] migration files |
| Threat Modeling (STRIDE) | [Yes/No] | [count] threats, [count] security ADRs |
| Prototype Validation | [Yes/No] | [count] prototypes |

## 3. System Design

### 3.1 Architecture Pattern

**Selected Pattern**: [e.g., MVVM + Service Layer]

**Rationale**: [Why this pattern was selected for this project]

### 3.2 Technology Selection Matrix

| Category | Candidates | Recommended | Rationale |
|----------|-----------|-------------|-----------|
| JavaFX Version | [candidates] | [selected] | [rationale] |
| Build Tool | [candidates] | [selected] | [rationale] |
| Database | [candidates] | [selected] | [rationale] |
| ORM | [candidates] | [selected] | [rationale] |
| DI Framework | [candidates] | [selected] | [rationale] |
| Logging | [candidates] | [selected] | [rationale] |
| Testing | [candidates] | [selected] | [rationale] |
| Third-party UI | [candidates] | [selected] | [rationale] |

### 3.3 Module Decomposition

| Module | Package | Responsibility | Dependencies |
|--------|---------|---------------|--------------|
| [module-name] | [package] | [responsibility] | [depends-on] |

### 3.4 Layering Strategy

| Layer | Contains | Depends On |
|-------|---------|------------|
| Presentation | Controllers, ViewModels, FXML | Application |
| Application | Use cases, orchestration | Domain |
| Domain | Entities, value objects | (none) |
| Infrastructure | Database, external APIs | Domain (implements interfaces) |

## 4. UML Diagrams

### 4.1 Class Diagram

- **File**: `architecture/uml/class-diagram.puml`
- **Classes shown**: [count]
- **Packages**: [list]
- **Key relationships**: [summary]

### 4.2 Sequence Diagrams

| Use Case | File | Participants |
|----------|------|-------------|
| [use-case-name] | `architecture/uml/sequence-diagram.puml` | [list] |

### 4.3 Deployment Diagram

- **File**: `architecture/uml/deployment-diagram.puml`
- **Nodes**: [list]
- **External connections**: [list]

## 5. Architecture Decision Records

| ADR # | Title | Status | File |
|-------|-------|--------|------|
| ADR-001 | [title] | Accepted | `architecture/adr/ADR-001-[title].md` |
| ADR-002 | [title] | Accepted | `architecture/adr/ADR-002-[title].md` |

## 6. Database Schema Design

> *Conditional section — present only if `system_design.technology_stack.database` is not "none".*

### 6.1 ER Diagram

- **File**: `architecture/uml/er-diagram.puml`
- **Tables**: [count]
- **Relationships**: [summary of foreign key relationships]

### 6.2 Schema Definition

| Table | Description | Columns | Indexes | Foreign Keys |
|-------|-------------|---------|---------|--------------|
| [table-name] | [description] | [count] | [count] | [count] |

### 6.3 Migration Plan

- **Migration tool**: [Flyway / Liquibase / None]
- **Migration path**: `src/main/resources/db/migration`
- **Initial migrations**: [count] files
- **Seed data**: [count] tables seeded

## 7. Threat Modeling (STRIDE)

> *Conditional section — present only if threat modeling was executed.*

### 7.1 Data Flow Diagram (DFD)

- **File**: `architecture/uml/threat-model-dfd.puml`
- **Trust boundaries**: [count]
- **Attack surfaces**: [count]

### 7.2 Threat Catalog

| Threat ID | STRIDE Category | Description | Risk Rating | Mitigation |
|-----------|----------------|-------------|-------------|------------|
| TM-001 | [category] | [description] | [Critical/High/Medium/Low] | [mitigation] |

### 7.3 Security ADRs

| ADR # | Title | Status | File |
|-------|-------|--------|------|
| ADR-SEC-001 | [title] | Accepted | `architecture/adr/ADR-SEC-001-[title].md` |

### 7.4 Threat-Test Traceability

| Threat ID | Test Case ID | Coverage Status |
|-----------|-------------|-----------------|
| TM-001 | SEC-TM-001 | covered |

## 8. Prototype Validation

| # | Risk Area | Prototype File | Result | Detail |
|---|----------|---------------|--------|--------|
| 1 | [risk] | `architecture/prototype/[name]/` | [passed/failed] | [detail] |

## 9. Developer Instructions

### 9.1 Package Structure
```
[package pattern, e.g., com.example.app.{module}]
```

### 9.2 Layering Rule
[layer dependency rules]

### 9.3 Naming Convention
| Type | Pattern | Example |
|------|---------|---------|
| Controller | {Entity}Controller | UserController |
| ViewModel | {Entity}ViewModel | UserViewModel |
| Service | {Entity}Service / {Entity}ServiceImpl | UserService |
| Repository | {Entity}Repository | UserRepository |

### 9.4 Key Constraints
1. [constraint 1]
2. [constraint 2]
3. [constraint 3]

## 10. Warnings and Risks

[List any warnings, unresolved risks, or areas requiring further investigation]

## 11. Handoff

- **Handoff file**: `architecture/architecture-handoff.json`
- **Consumed by**: `javafx-developer` Step 4
- **Loop state**: `architect_result` in `.loop-state.json`

---

> **Notes**:
> - This report is generated by `javafx-architect` and is for stakeholder review
> - The machine-readable JSON version (`architecture-report.json`) is consumed by `javafx-developer`
> - Architecture handoff is optional — if `architecture-handoff.json` is absent, the developer uses default architecture decisions
