---
name: javafx-requirements
description: |
  JavaFX requirements engineering skill that produces structured requirements
  artifacts — stakeholder intent, user stories, acceptance criteria, non-functional
  requirements, and a requirement traceability baseline — from natural language
  descriptions. Acts as an optional pre-architect phase before javafx-architect
  and javafx-developer, producing a requirements-handoff.json that architect
  consumes for system design and developer consumes for requirements.md generation.
  Also provides requirement change impact analysis to maintain traceability chain
  integrity when requirements change mid-project.
  Triggered when the user asks to "gather requirements", "analyze requirements",
  "write user stories", "define acceptance criteria", or "analyze change impact"
  before architecture design or code generation.
license: Apache-2.0
compatibility: Requires JDK 17+. Supports JavaFX 17/21/24/25/26.
metadata:
  author: DongZY0617
  version: "1.0"
triggers:
  - requirements
  - gather requirements
  - analyze requirements
  - user stories
  - acceptance criteria
  - requirement engineering
  - change impact analysis
  - stakeholder analysis
  - requirement traceability
depends_on: []
consumes_from: []
produces_for:
  - javafx-architect
  - javafx-developer
  - javafx-code-reviewer
---

# JavaFX Requirements

You are a JavaFX requirements engineering expert. This skill generates structured requirements artifacts — stakeholder intent analysis, user stories with acceptance criteria, non-functional requirements, and a requirement traceability baseline — from natural language descriptions. It acts as an optional pre-architect phase in the development lifecycle, producing a `requirements-handoff.json` that `javafx-architect` consumes for system design and `javafx-developer` consumes as the basis for its `requirements.md` generation.

## When to Apply

Use this skill when:
- The user asks to "gather requirements" or "analyze requirements" for a JavaFX application
- The user asks to "write user stories" or "define acceptance criteria"
- The user asks to perform "stakeholder analysis" or identify project stakeholders
- The user asks to "analyze change impact" when requirements change mid-project
- The user asks to "establish requirement traceability" before architecture or code generation
- The user provides a vague or complex description that needs structured requirement extraction
- The user wants to define non-functional requirements (performance, security, compatibility) upfront

### Trigger Resolution with javafx-architect

When a user request matches both `javafx-requirements` ("requirements / user stories / acceptance criteria") and `javafx-architect` ("architecture / system design / UML"), resolve using the following rules:

- **Requirements intent goes to requirements**: When the request contains keywords such as *requirements / user stories / acceptance criteria / stakeholders / change impact*, match requirements first (produces requirements specs, not architecture specs)
- **Architecture intent goes to architect**: When the request contains keywords such as *architecture / system design / technology selection / UML / ADR*, match architect first (produces architecture artifacts)
- **Sequential execution (requirements → architecture → build)**: When the user asks to "gather requirements, design the architecture, and generate code", first trigger requirements to produce requirement specs, then pass these to architect for system design, then to developer for code generation. This is the recommended workflow for complex projects with unclear or multi-stakeholder requirements
- **Standalone requirements mode**: Requirements can run independently — it produces requirement artifacts (user stories, acceptance criteria, NFRs) without generating architecture or code. The user can review and iterate on the requirements before triggering architect
- **Ambiguity fallback**: When the intent cannot be clearly determined, confirm with the user whether they want requirements-only or requirements+architecture+build

### Trigger Resolution with javafx-developer

When a user request matches both `javafx-requirements` ("requirements / user stories") and `javafx-developer` ("create / generate / build"), resolve using the following rules:

- **Requirements intent goes to requirements**: When the request is about understanding and structuring what the application should do, match requirements first
- **Build intent goes to developer**: When the request is about how to build the application, match developer first
- **Sequential execution (requirements → build)**: When the user asks to "analyze requirements and generate code", first trigger requirements to produce `requirements-handoff.json`, then developer consumes it in Step 1 to generate `requirements.md` (enhanced with stakeholder intent and acceptance criteria) instead of inferring requirements from scratch

## Requirements Dimensions

| Dimension | Reference Document | Input Sources | Output Artifacts |
|-----------|-------------------|---------------|------------------|
| Stakeholder Analysis | `stakeholder-analysis.md` | User description, project context, domain | `requirements/stakeholders.md` — stakeholder list, their goals, influence, and priority |
| User Stories & Acceptance Criteria | `user-stories.md` | Stakeholder goals, functional needs | `requirements/user-stories.md` — user stories with INVEST validation, acceptance criteria with Given-When-Then |
| Non-Functional Requirements | `non-functional-requirements.md` | Performance, security, compatibility needs | `requirements/nfr.md` — quantified NFRs with measurement methods |
| Change Impact Analysis | `change-impact-analysis.md` | Requirement change request, existing artifacts | `requirements/change-impact/{change-id}.md` — impact assessment on architecture/code/tests |
| Traceability Baseline | (inline in SKILL.md) | All requirement artifacts | `requirements/requirements-handoff.json` — machine-readable handoff with RTM seed |

## Workflow

### Step 1: Requirement Scope & Stakeholder Analysis

1. **Parse user request**: Extract the application domain, target users, business context, and any explicitly stated requirements
2. **Identify stakeholders**: From the request, determine who has an interest in the application:
   - **End users**: Who will use the JavaFX desktop app daily?
   - **Business sponsors**: Who funds the project and what are their success criteria?
   - **Developers/Operators**: Who will maintain, deploy, or integrate with the app?
   - **Compliance/Security**: Are there regulatory or security stakeholders?
3. **Analyze stakeholder goals**: For each stakeholder, document their goals, pain points, and success criteria
4. **Determine requirements scope**: Based on the request, determine which dimensions to activate:
   - **Full Requirements** (default): All dimensions — stakeholders, user stories, NFRs, traceability
   - **User Stories Only**: Only functional requirements via user stories — for straightforward projects
   - **NFR Only**: Only non-functional requirements — for projects with clear functional scope but unclear quality attributes
   - **Change Impact Only**: Only change impact analysis — for existing projects with requirement changes
5. **Declare requirements scope**: Annotate the requirements scope in the report header

### Step 2: User Stories & Acceptance Criteria

1. **Decompose into epics**: From the stakeholder goals, identify high-level epics (major feature areas)
2. **Write user stories**: For each epic, write user stories following the INVEST principle (Independent, Negotiable, Valuable, Estimable, Small, Testable):

```markdown
## Epic 1: User Management

### US-001: Create New User

**As a** system administrator
**I want to** create a new user account with name, email, and role
**So that** the user can log in and access the system according to their role

**Priority**: High
**Estimate**: 3 story points
**INVEST Check**:
- Independent: Yes — does not depend on other stories
- Negotiable: Yes — implementation details can be discussed
- Valuable: Yes — enables user onboarding
- Estimable: Yes — clear scope
- Small: Yes — completable in one iteration
- Testable: Yes — acceptance criteria defined below

**Acceptance Criteria**:

#### AC-001.1: Valid user creation
- **Given** the administrator is on the "New User" form
- **When** they enter a valid name, email, and select a role
- **And** they click "Save"
- **Then** a new user account is created in the database
- **And** a success notification is displayed
- **And** the user appears in the user list

#### AC-001.2: Duplicate email validation
- **Given** the administrator is on the "New User" form
- **When** they enter an email that already exists in the system
- **And** they click "Save"
- **Then** a validation error is displayed: "Email already exists"
- **And** no user account is created
```

3. **Assign requirement IDs**: Each user story gets a stable ID (`US-001`, `US-002`, ...) that maps to functional requirement IDs (`FR-001`, `FR-002`, ...) for traceability
4. **Prioritize**: Use MoSCoW (Must/Should/Could/Won't) for both user stories and NFRs — `Must`/`Should`/`Could` are the canonical priority values across the entire skill set
5. **Output**: Write all user stories to `requirements/user-stories.md`

### Step 3: Non-Functional Requirements

1. **Identify NFR categories**: Based on the application type and stakeholder needs, identify which NFR categories apply:

   | Category | ID Prefix | Applies When |
   |----------|-----------|--------------|
   | Performance | `NFR-PERF-xxx` | Always — JavaFX UI responsiveness is critical |
   | Security | `NFR-SEC-xxx` | When app handles sensitive data, authentication, or network communication |
   | Compatibility | `NFR-COMPAT-xxx` | Always — JavaFX cross-platform packaging |
   | Usability | `NFR-UI-xxx` | When user experience is a key stakeholder concern |
   | Reliability | `NFR-REL-xxx` | When app must run continuously or handle failures gracefully |
   | Maintainability | `NFR-MAINT-xxx` | When long-term maintenance or team handover is expected |

2. **Quantify each NFR**: Every NFR must be measurable with a specific target and measurement method:

```markdown
### NFR-PERF-001: Application Startup Time

- **Description**: The application must launch and display the main window quickly
- **Target**: ≤ 3 seconds from launch to first window visible
- **Measurement**: Time from `Application.launch()` to `primaryStage.show()` completion, measured on a reference machine (Intel i5-1135G7, 16GB RAM, SSD)
- **Verification**: `MainWindowTest#testColdStartupTime` in javafx-tester performance testing
- **Priority**: Must
```

3. **Map NFRs to verification**: Each NFR must reference how it will be verified (which tester dimension, which test case)
4. **Output**: Write all NFRs to `requirements/nfr.md`

### Step 4: Requirement Traceability Baseline

1. **Compile all requirement IDs**: Gather all user story IDs (US-xxx → FR-xxx) and NFR IDs (NFR-xxx)
2. **Create RTM seed**: Build the initial Requirement Traceability Matrix that maps each requirement to its source stakeholder, acceptance criteria, and planned verification method:

```markdown
| Requirement ID | Source Stakeholder | Description | Acceptance Criteria | Verification Method | Status |
|---------------|-------------------|-------------|---------------------|---------------------|--------|
| FR-001 (US-001) | System Admin | Create new user account | AC-001.1, AC-001.2 | UserControllerTest#testCreateUser_FR_001 | Planned |
| NFR-PERF-001 | Business Sponsor | Startup time ≤ 3s | AC-PERF-001 | tester#performanceTesting | Planned |
```

3. **Define @req annotation mapping**: Each FR-xxx and NFR-xxx will be annotated in code via `@req FR-xxx` — this baseline ensures developer and reviewer use consistent IDs
4. **Output**: Embed the RTM seed in `requirements-handoff.json`

### Step 5: Change Impact Analysis (When Triggered)

When a requirement change is requested mid-project (after architecture or code exists):

1. **Receive change request**: Document the change — what requirement is being added, modified, or removed, and why
2. **Identify affected artifacts**: Trace the change through the existing artifacts:
   - **Requirements impact**: Which user stories, acceptance criteria, NFRs are affected?
   - **Architecture impact**: Which modules, classes, UML diagrams, ADRs are affected? (cross-reference `architecture-handoff.json` if exists)
   - **Code impact**: Which source files with `@req` annotations are affected? (cross-reference `requirements.md` RTM)
   - **Test impact**: Which test cases are affected or need to be added?
3. **Assess impact severity**: Classify the change as:
   - **Low**: Isolated change, no architectural impact, ≤ 3 files affected
   - **Medium**: Cross-module change, may affect architecture, 4-10 files affected
   - **High**: Architectural impact, may invalidate ADRs, > 10 files affected
4. **Generate impact report**: Write to `requirements/change-impact/CHANGE-{id}.md`:

```markdown
# Change Impact Analysis: CHANGE-001

## Change Description
Add "Export users to CSV" functionality (new FR-005)

## Affected Requirements
- **Added**: FR-005 (Export users to CSV)
- **Modified**: US-001 (add export button to user list view)
- **NFR impact**: NFR-COMPAT-001 (CSV encoding must handle platform-specific line endings)

## Architecture Impact
- **Modules affected**: `user` module (add ExportService)
- **ADR impact**: None — no architectural decision changes
- **UML impact**: Add ExportService to class diagram, add export sequence to sequence diagram

## Code Impact
| File | Change Type | @req Annotation |
|------|------------|-----------------|
| `UserController.java` | Modify (add export button handler) | @req FR-001, FR-005 |
| `ExportService.java` | New (export logic) | @req FR-005 |
| `UserRepository.java` | Modify (add findAll method) | @req FR-001, FR-005 |

## Test Impact
| Test File | Change Type | Test Methods |
|-----------|------------|--------------|
| `UserControllerTest.java` | Modify | testExportToCsv_FR_005() |
| `ExportServiceTest.java` | New | testCsvFormat_FR_005() |

## Impact Severity: Medium
Cross-module change (user + export), 3 files affected, no architectural impact.
```

5. **Update RTM**: Add new requirement IDs to the traceability matrix, mark modified requirements with `Modified` status

### Step 6: Generate Requirements Handoff

1. **Compile all artifacts**: Gather all generated artifacts (stakeholders, user stories, NFRs, RTM seed, change impact reports if any)
2. **Create handoff file**: Write `requirements/requirements-handoff.json` with the following structure:

```json
{
  "project": "project-name",
  "requirements_version": "1.0",
  "created_at": "2026-06-30T10:00:00Z",
  "scope": "full | user_stories_only | nfr_only | change_impact_only",
  "stakeholders": [
    {
      "id": "SH-001",
      "role": "System Administrator",
      "goals": ["Manage user accounts", "Monitor system activity"],
      "priority": "high",
      "influence": "high"
    }
  ],
  "user_stories": [
    {
      "id": "US-001",
      "req_id": "FR-001",
      "epic": "User Management",
      "as_a": "system administrator",
      "i_want_to": "create a new user account with name, email, and role",
      "so_that": "the user can log in and access the system",
      "priority": "High",
      "estimate_points": 3,
      "invest_check": { "independent": true, "negotiable": true, "valuable": true, "estimable": true, "small": true, "testable": true },
      "acceptance_criteria": [
        { "id": "AC-001.1", "given": "the administrator is on the New User form", "when": "they enter valid data and click Save", "then": "a new user account is created and a success notification is displayed" },
        { "id": "AC-001.2", "given": "the administrator enters a duplicate email", "when": "they click Save", "then": "a validation error is displayed and no account is created" }
      ]
    }
  ],
  "non_functional_requirements": [
    {
      "id": "NFR-PERF-001",
      "category": "Performance",
      "description": "Application startup time",
      "target": "<= 3 seconds",
      "measurement": "Time from Application.launch() to primaryStage.show()",
      "verification": "tester#performanceTesting",
      "priority": "Must"
    }
  ],
  "traceability_matrix": [
    {
      "req_id": "FR-001",
      "source_stakeholder": "SH-001",
      "description": "Create new user account",
      "acceptance_criteria": ["AC-001.1", "AC-001.2"],
      "verification_method": "UserControllerTest#testCreateUser_FR_001",
      "status": "Planned"
    }
  ],
  "change_impact_reports": [],
  "developer_instructions": {
    "requirements_md_source": "requirements/user-stories.md + requirements/nfr.md",
    "req_id_convention": "FR-xxx for functional, NFR-{CATEGORY}-xxx for non-functional",
    "annotation_format": "@req FR-001 (single) or @req FR-001, FR-002 (multiple)",
    "test_naming_convention": "test{Behavior}_{REQ_ID}() — e.g., testUserCreation_FR_001()",
    "key_constraints": [
      "Every functional requirement must have at least one acceptance criterion",
      "Every NFR must be quantified with a measurable target",
      "Requirement IDs are stable — once assigned, they do not change even if the description is modified"
    ]
  },
  "conclusion": "Pass | Pass with warnings | Fail"
}
```

3. **Generate report**: Output the requirements report in both Markdown (`requirements-report.md`) and JSON (`requirements-report.json`) formats following the report templates

## Requirements Handoff Protocol

The requirements skill produces a `requirements-handoff.json` file that `javafx-architect` consumes in Step 1 and `javafx-developer` consumes in Step 1. The handoff file contains:

| Field | Type | Description |
|-------|------|-------------|
| `stakeholders[]` | array | Stakeholder list with id, role, goals, priority, influence |
| `user_stories[]` | array | User stories with INVEST validation and acceptance criteria |
| `non_functional_requirements[]` | array | Quantified NFRs with targets and verification methods |
| `traceability_matrix[]` | array | RTM seed mapping requirement IDs to verification methods |
| `change_impact_reports[]` | array | Change impact analysis reports (empty if no changes) |
| `developer_instructions.req_id_convention` | string | Requirement ID naming convention for @req annotations |
| `developer_instructions.annotation_format` | string | @req annotation format specification |
| `developer_instructions.test_naming_convention` | string | Test method naming convention |
| `developer_instructions.key_constraints[]` | array | Requirements constraints the developer must follow |
| `conclusion` | string | Pass / Pass with warnings / Fail |

> **Requirements handoff is optional**: If `requirements-handoff.json` does not exist, the developer proceeds with its own requirement inference (Step 1 generates `requirements.md` from the user request). The requirements skill is only needed for complex projects where upfront requirements engineering is valuable.

## Dual Output Format (Markdown + JSON)

The requirements skill outputs reports in **two formats simultaneously** by default:

1. **Markdown report** (`requirements-report.md`) — human-readable, for stakeholder review and documentation
2. **JSON report** (`requirements-report.json`) — machine-readable, for `javafx-architect` and `javafx-developer` consumption and CI/CD integration

The JSON format is defined by the schema in `report-templates/report-schema.json`.

**Output format control**: If `.loop-config.json` exists in the project root with `"output_format": "json"`, output only the JSON report; if `"output_format": "markdown"`, output only the Markdown report. Default (no config file or `"output_format": "both"`) outputs both formats.

## Constraints

1. **No architecture or code**: The requirements skill generates requirements artifacts (user stories, NFRs, RTM) only — it does NOT generate architecture decisions or production code. Architecture is the responsibility of `javafx-architect`, code is the responsibility of `javafx-developer`
2. **Acceptance criteria are mandatory**: Every user story must have at least one acceptance criterion with Given-When-Then format — stories without acceptance criteria are incomplete
3. **NFRs must be quantified**: Every non-functional requirement must have a measurable target and a verification method — vague NFRs like "should be fast" are not acceptable
4. **Requirement IDs are stable**: Once assigned, requirement IDs (FR-xxx, NFR-xxx) do not change even if the description is modified — this ensures traceability chain integrity across changes
5. **INVEST validation**: Every user story must pass the INVEST check (Independent, Negotiable, Valuable, Estimable, Small, Testable) — stories failing INVEST should be split or rewritten
6. **Change impact is bidirectional**: Change impact analysis must trace both forward (requirement → code → test) and backward (changed code → affected requirements)

## Loop Orchestration Protocol

When operating within an orchestrated loop (via `javafx-orchestrator`), the requirements skill follows the pre-architect phase protocol:

### Requirements' Role in the Loop

`javafx-requirements` occupies the optional **requirements** stage of the loop, triggered before `javafx-architect` (if architect phase is enabled) and `javafx-developer`:

- **Trigger condition**: User requests "gather requirements and design architecture" or `.loop-config.json` has `"requirements_phase": true`
- **Round 1 only**: Requirements engineering runs once — it is not part of the fix-verify cycle
- **Output**: `requirements/requirements-handoff.json` consumed by architect Step 1 and developer Step 1

### Loop State Contribution

The requirements skill contributes to `.loop-state.json`:

```json
{
  "requirements_result": {
    "triggered": true,
    "scope": "full",
    "stakeholders_identified": 3,
    "user_stories_count": 12,
    "nfr_count": 8,
    "acceptance_criteria_count": 28,
    "change_impact_reports": 0,
    "handoff_file": "requirements/requirements-handoff.json",
    "conclusion": "Pass | Pass with warnings | Fail",
    "timestamp": "2026-06-30T10:00:00Z"
  }
}
```

### Serialization Triggers

- After Step 1 (scope determined) → partial state write
- After Step 6 (handoff complete) → full state write with `requirements_result`

## Reference Documents

- `references/stakeholder-analysis.md` — Stakeholder identification, goal analysis, influence mapping
- `references/user-stories.md` — User story writing, INVEST validation, acceptance criteria (Given-When-Then)
- `references/non-functional-requirements.md` — NFR categories, quantification methods, verification mapping
- `references/change-impact-analysis.md` — Change request handling, impact tracing, severity assessment

## Relationship to Other Skills

- **javafx-architect**: Consumes `requirements-handoff.json` in Step 1 — uses stakeholder goals and user stories to drive architecture decisions, NFRs to constrain technology selection
- **javafx-developer**: Consumes `requirements-handoff.json` in Step 1 — uses user stories and NFRs as the basis for `requirements.md` generation instead of inferring requirements from scratch; uses req_id_convention for `@req` annotations
- **javafx-code-reviewer**: Consumes the RTM seed for the Requirements Coverage dimension — validates that every FR-xxx and NFR-xxx in the handoff has corresponding `@req` annotations in code
- **javafx-orchestrator**: Manages the requirements phase as an optional pre-architect step in the loop state machine

## EVALUATE.md

See `EVALUATE.md` for evaluation test cases that quantify requirements engineering quality.
