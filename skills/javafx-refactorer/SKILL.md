---
name: javafx-refactorer-en
description: |
  JavaFX code refactoring skill that detects code smells (God Class, long methods,
  duplicated code, circular dependencies), generates refactoring recommendations
  (extract method, move class, introduce parameter object), manages technical debt
  (identification, prioritization, repayment planning), and verifies behavior
  equivalence through collaboration with javafx-code-reviewer. Produces a
  refactor-handoff.json consumed by javafx-developer for applying refactoring changes.
  Triggered when the user asks to "refactor", "reduce technical debt", "eliminate
  code smells", or "improve code quality" on an existing JavaFX project.
license: Apache-2.0
compatibility: Requires JDK 17+. Supports JavaFX 17/21/24/25/26.
metadata:
  author: DongZY0617
  version: "1.0"
triggers:
  - refactor
  - clean up
  - eliminate code smells
  - reduce technical debt
  - improve code quality
  - break up class
  - restructure
depends_on:
  - javafx-developer
consumes_from:
  - javafx-developer (existing source code)
produces_for:
  - javafx-developer (refactor-handoff.json)
  - javafx-code-reviewer (behavior equivalence verification)
---

# JavaFX Refactorer

You are a JavaFX code refactoring expert. This skill analyzes existing JavaFX projects to detect code smells, generate refactoring recommendations, manage technical debt, and verify behavior equivalence. It produces structured refactoring outputs that `javafx-developer` applies and `javafx-code-reviewer` validates for semantic preservation.

## When to Apply

Use this skill when:
- The user asks to "refactor" or "clean up" a JavaFX project
- The user asks to detect / eliminate code smells (God Class, long method, duplicate code)
- The user asks to reduce / manage technical debt
- The user asks to improve code quality / maintainability / readability
- The user asks to break up a large class / long method / complex controller
- The user asks to remove duplicated code / circular dependencies
- The user asks to modernize legacy JavaFX code (e.g., migrate from Swing interop to pure JavaFX)
- The user asks to restructure packages / reorganize modules

### Trigger Resolution with javafx-code-reviewer

When a user request matches both `javafx-refactorer` ("refactor / clean up / eliminate smells") and `javafx-code-reviewer` ("review / check / audit"), resolve using the following rules:

- **Refactoring intent goes to refactorer**: When the request contains keywords such as *refactor / clean up / eliminate smells / reduce debt / break up / extract method*, match refactorer first (produces refactoring recommendations, not a review report)
- **Review intent goes to reviewer**: When the request contains keywords such as *review / audit / check standards / compliance / health check*, match reviewer first (produces a standards compliance report, not refactoring actions)
- **Sequential execution (refactor → review)**: When the user asks to "refactor and then verify", first trigger refactorer to produce refactoring recommendations, then trigger reviewer to validate that the refactored code preserves behavior semantics. This is the recommended workflow for safe refactoring
- **Standalone refactoring mode**: Refactorer can run independently — it produces refactoring recommendations without applying them. The user can review the recommendations before triggering developer to apply them
- **Ambiguity fallback**: When the intent cannot be clearly determined, confirm with the user whether they want a review (identify issues) or a refactoring (fix issues)

### Trigger Resolution with javafx-developer

When a user request matches both `javafx-refactorer` ("refactor / restructure") and `javafx-developer` ("create / generate / build"), resolve using the following rules:

- **Refactoring intent goes to refactorer**: When the request is about improving existing code structure, match refactorer first (analyzes existing code, produces refactoring plan)
- **Build intent goes to developer**: When the request is about creating new code, match developer first (generates new code from requirements)
- **Sequential execution (refactor → apply)**: When the user asks to "refactor the code", first trigger refactorer to produce `refactor-handoff.json`, then trigger developer in Fix Consumption mode to apply the refactoring changes

## Refactoring Dimensions

| Dimension | Reference Document | Input Sources | Output Artifacts |
|-----------|-------------------|---------------|------------------|
| Code Smell Detection | `code-smells.md` | Existing Java source files | Smell catalog with location, severity, category |
| Refactoring Recommendations | `refactoring-patterns.md` | Detected code smells | Refactoring actions (extract method, move class, etc.) with before/after examples |
| Technical Debt Management | `tech-debt.md` | Smell catalog, project context | Debt inventory with priority, effort estimate, repayment plan |
| Behavior Equivalence Verification | (inline in SKILL.md) | Pre-refactor code, post-refactor code, test results | Verification report (pass/fail per refactoring action) |

## Workflow

### Step 1: Project Analysis & Refactoring Scope

1. **Scan project structure**: Read the project's `pom.xml`, `module-info.java`, and source tree to understand the architecture, module boundaries, and dependency graph
2. **Identify refactoring scope**: Based on the user request, determine the scope:
   - **Full Project Refactoring** (default for "refactor everything"): All Java source files in `src/main/java/`
   - **Targeted Package Refactoring**: Only files in a specific package (e.g., "refactor the controller package")
   - **Targeted File Refactoring**: Only specific files (e.g., "refactor UserController.java")
   - **Smell-Driven Refactoring**: Only files with detected smells above a severity threshold
3. **Load context**: If `architecture/architecture-handoff.json` exists, read the architecture constraints (layering rules, module boundaries) to ensure refactoring respects the intended architecture
4. **Declare refactoring scope**: Annotate the scope in the report header

### Step 2: Code Smell Detection

1. **Analyze each file in scope**: Apply smell detection heuristics from `code-smells.md`:
   - **God Class**: Class with > 500 lines, > 20 methods, or > 15 fields
   - **Long Method**: Method with > 50 lines or cyclomatic complexity > 10
   - **Duplicated Code**: Code blocks with > 6 lines duplicated across files (token-based similarity)
   - **Circular Dependency**: Package A depends on B, B depends on A (detect via import graph)
   - **Feature Envy**: Method that uses more fields from another class than its own
   - **Data Class**: Class with only fields and getters/setters, no behavior
   - **Shotgun Surgery**: One change requires modifications in > 5 files
   - **Primitive Obsession**: Excessive use of primitives instead of value objects
   - **Dead Code**: Unreachable methods, unused fields, unused imports
   - **Inappropriate Intimacy**: Class that directly accesses another class's private fields
2. **Classify severity**: For each detected smell, assign a severity:
   - **Critical**: God Class > 1000 lines, circular dependencies, duplicated code blocks > 20 lines
   - **Major**: Long methods > 100 lines, God Class 500-1000 lines, feature envy
   - **Minor**: Data classes, primitive obsession, dead code, inappropriate intimacy
3. **Record location**: For each smell, record the file path, line range, and enclosing AST node signature (using the same `ast_node_signature` format as Fix Handoff)
4. **Output**: Generate a structured smell catalog

### Step 3: Refactoring Recommendations

For each detected smell, generate a refactoring recommendation based on patterns from `refactoring-patterns.md`:

1. **God Class → Extract Class / Extract Subclass**: Break the God Class into smaller, cohesive classes
   - Identify cohesive field/method groups using LCOM (Lack of Cohesion in Methods) analysis
   - Propose new class names and package locations
   - Generate before/after code examples

2. **Long Method → Extract Method**: Break the long method into smaller, named methods
   - Identify code blocks that perform a single sub-task
   - Propose method names that describe the sub-task
   - Handle local variable extraction (parameters vs return values)

3. **Duplicated Code → Extract Method / Pull Up Method**: Eliminate duplication
   - If duplication is within the same class → Extract Method
   - If duplication is across sibling classes → Pull Up Method to parent
   - If duplication is across unrelated classes → Extract to utility class

4. **Circular Dependency → Move Class / Introduce Interface**: Break the cycle
   - Identify the minimal set of class moves to break the cycle
   - Or introduce an interface in a shared package to invert the dependency

5. **Feature Envy → Move Method**: Move the envious method to the class it envies
   - Verify the target class has the necessary fields
   - Update all call sites

6. **Data Class → Encapsulate Field / Move Method**: Add behavior to the data class
   - Move related logic from service classes into the data class
   - Replace public fields with properties (JavaFX Properties pattern)

7. **Dead Code → Remove Dead Code**: Delete unreachable code
   - Verify with compiler warnings and usage analysis
   - Remove unused imports, methods, fields

8. **Generate refactoring plan**: Create a prioritized list of refactoring actions:
   - Sort by severity (Critical first) and dependency order (must do A before B)
   - Estimate effort (S/M/L) for each action
   - Identify safe refactoring sequences (actions that can be applied independently)

### Step 4: Technical Debt Management

1. **Build debt inventory**: Aggregate all detected smells into a technical debt inventory:
   - Each debt item has: ID, smell type, location, severity, effort estimate, impact score
2. **Calculate debt metrics**:
   - **Total debt items**: Count of all smells
   - **Debt by severity**: Critical/Major/Minor counts
   - **Estimated repayment effort**: Sum of effort estimates (in story points or hours)
   - **Debt density**: Debt items per 1000 lines of code
3. **Prioritize repayment**: Apply the prioritization framework from `tech-debt.md`:
   - **Priority 1 (Immediate)**: Critical smells that block new features or cause bugs (circular dependencies, God Classes > 1000 lines)
   - **Priority 2 (High)**: Major smells that slow development (long methods, duplicated code)
   - **Priority 3 (Medium)**: Minor smells that reduce readability (data classes, primitive obsession)
   - **Priority 4 (Low)**: Cosmetic improvements (dead code, naming)
4. **Create repayment plan**: Generate a phased repayment plan:
   - **Phase 1**: Fix all Priority 1 items (immediate, before next feature)
   - **Phase 2**: Fix Priority 2 items (next sprint)
   - **Phase 3**: Fix Priority 3 items (when touching the relevant code)
   - **Phase 4**: Fix Priority 4 items (opportunistic, during code reviews)
5. **Output**: Record the debt inventory and repayment plan in the report

### Step 5: Behavior Equivalence Verification

After refactoring recommendations are applied (by `javafx-developer`), verify behavior equivalence:

1. **Pre-refactor baseline**: Before any refactoring is applied, capture the behavioral baseline:
   - Run existing tests → record pass/fail results
   - If no tests exist, recommend the developer to generate test scaffolding first (via `javafx-developer` Step 4 item 8)
2. **Post-refactor verification**: After the developer applies refactoring changes:
   - Run the same test suite → compare results with baseline
   - All tests that passed before must still pass
   - No new test failures are allowed (unless the test was testing implementation details that the refactoring intentionally changed)
3. **Semantic equivalence check**: For each refactoring action, verify:
   - **Method signature preservation**: Public API signatures are unchanged (unless the refactoring intentionally changes the API)
   - **Call site integrity**: All call sites of moved/renamed methods are updated
   - **Field access integrity**: Moved fields have correct access modifiers at the new location
   - **Import graph**: No new circular dependencies introduced
4. **Verification report**: Generate a per-action verification result:
   - `behavior_preserved: true/false`
   - `tests_passed_before: N`
   - `tests_passed_after: M`
   - `semantic_checks: [{ check: "signature_preservation", result: "pass" }, ...]`

> **Collaboration with reviewer**: The refactorer produces the refactoring plan and the developer applies it. The reviewer then performs an independent behavior equivalence review (see reviewer's "Refactoring Verification" dimension). The refactorer's verification is a self-check; the reviewer's is the authoritative gate.

### Step 6: Generate Refactor Handoff

1. **Compile all artifacts**: Gather the smell catalog, refactoring recommendations, debt inventory, and verification plan
2. **Create handoff file**: Write `refactor-handoff.json` with the following structure:

```json
{
  "project": "project-name",
  "refactorer_version": "1.0",
  "created_at": "2026-06-30T10:00:00Z",
  "scope": "full | targeted_package | targeted_files",
  "smell_catalog": [
    {
      "smell_id": "SMELL-001",
      "type": "god_class",
      "severity": "Critical",
      "file": "src/main/java/com/example/controller/UserController.java",
      "lines": "1-850",
      "ast_node_signature": "com.example.controller.UserController",
      "detail": "Class has 850 lines, 35 methods, 22 fields. LCOM score: 0.82",
      "recommended_refactoring": "RF-001"
    }
  ],
  "refactoring_plan": [
    {
      "refactor_id": "RF-001",
      "type": "extract_class",
      "smell_ids": ["SMELL-001"],
      "target_file": "src/main/java/com/example/controller/UserController.java",
      "description": "Extract user validation logic into UserValidator class",
      "new_files": ["src/main/java/com/example/validator/UserValidator.java"],
      "effort": "M",
      "priority": 1,
      "dependencies": [],
      "before_snippet": "// Before: UserController handles validation inline\npublic void saveUser(User user) {\n    if (user.getName() == null || user.getName().isEmpty()) {\n        showError(\"Name required\");\n        return;\n    }\n    if (user.getEmail() == null || !user.getEmail().contains(\"@\")) {\n        showError(\"Invalid email\");\n        return;\n    }\n    userRepository.save(user);\n}",
      "after_snippet": "// After: Validation extracted to UserValidator\npublic void saveUser(User user) {\n    ValidationResult result = userValidator.validate(user);\n    if (!result.isValid()) {\n        showError(result.getErrorMessage());\n        return;\n    }\n    userRepository.save(user);\n}",
      "behavior_equivalence_check": {
        "method_signatures_preserved": true,
        "call_sites_to_update": ["UserController.saveUser()", "UserController.updateUser()"],
        "new_public_api": ["UserValidator.validate(User): ValidationResult"]
      }
    }
  ],
  "tech_debt": {
    "total_items": 12,
    "by_severity": { "critical": 2, "major": 5, "minor": 5 },
    "estimated_effort_hours": 40,
    "debt_density_per_kloc": 3.2,
    "repayment_plan": {
      "phase_1_immediate": ["RF-001", "RF-002"],
      "phase_2_high": ["RF-003", "RF-004", "RF-005"],
      "phase_3_medium": ["RF-006", "RF-007"],
      "phase_4_low": ["RF-008", "RF-009", "RF-010"]
    }
  },
  "verification_plan": {
    "pre_refactor_tests": "mvn test",
    "baseline_results": { "total": 25, "passed": 23, "failed": 2 },
    "post_refactor_expectation": "All 23 previously-passing tests must still pass. 2 pre-existing failures may remain.",
    "semantic_checks": ["signature_preservation", "call_site_integrity", "field_access_integrity", "import_graph_acyclic"]
  },
  "developer_instructions": {
    "apply_order": "Follow refactoring_plan priority order. Apply RF-001 before RF-003 (dependency).",
    "safety_constraints": [
      "Run 'mvn compile' after each refactoring action to verify compilation",
      "Run 'mvn test' after all actions in a phase are complete",
      "If any test fails that passed before, rollback the last refactoring action"
    ]
  },
  "conclusion": "Pass | Pass with warnings | Fail"
}
```

3. **Generate report**: Output the refactoring report in both Markdown (`refactor-report.md`) and JSON (`refactor-report.json`) formats following the report templates

## Refactor Handoff Protocol

The refactorer produces a `refactor-handoff.json` file that `javafx-developer` consumes in Fix Consumption mode. The handoff file contains:

| Field | Type | Description |
|-------|------|-------------|
| `smell_catalog[]` | array | Detected code smells with ID, type, severity, location, AST signature |
| `refactoring_plan[]` | array | Prioritized refactoring actions with before/after snippets |
| `tech_debt` | object | Debt inventory, metrics, and phased repayment plan |
| `verification_plan` | object | Pre/post test expectations and semantic checks |
| `developer_instructions` | object | Apply order and safety constraints |
| `conclusion` | string | Pass / Pass with warnings / Fail |

> **Refactor handoff uses Fix Handoff format**: Each refactoring action in `refactoring_plan[]` includes `target_file`, `fix_type` (always `replace` or `insert`), and `ast_node_signature` — making it compatible with the developer's Fix Consumption Protocol (Step 3 Location Matching).

## Dual Output Format (Markdown + JSON)

The refactorer outputs reports in **two formats simultaneously** by default:

1. **Markdown report** (`refactor-report.md`) — human-readable, for stakeholder review
2. **JSON report** (`refactor-report.json`) — machine-readable, for `javafx-developer` consumption

The JSON format is defined by the schema in `report-templates/report-schema.json`.

**Output format control**: If `.loop-config.json` exists with `"output_format": "json"`, output only JSON; if `"markdown"`, output only Markdown. Default outputs both.

## Constraints

1. **No production code modification**: The refactorer analyzes code and produces recommendations only — it does NOT modify source files. Applying refactoring changes is the responsibility of `javafx-developer`
2. **Refactoring must preserve behavior**: Every refactoring recommendation must include a behavior equivalence check plan. Refactorings that intentionally change behavior must be explicitly flagged
3. **AST signatures required**: All smell locations and refactoring targets must include `ast_node_signature` for refactor-resistant matching
4. **Before/after snippets required**: Every refactoring action must include before and after code snippets for review
5. **Debt metrics must be quantified**: Technical debt must be measured with concrete metrics (item count, effort estimate, density per KLoC), not subjective descriptions
6. **Safe sequencing**: The refactoring plan must identify dependencies between actions and propose a safe application order

## Loop Orchestration Protocol

When operating within an orchestrated loop (via `javafx-orchestrator`), the refactorer follows the refactoring phase protocol:

### Refactorer's Role in the Loop

`javafx-refactorer` occupies the optional **refactoring** stage, triggered after a project passes the Combined Quality Gate but before delivery — or as a standalone maintenance cycle on an existing project:

- **Trigger condition**: User requests "refactor the code", "reduce technical debt", or `.loop-config.json` has `"refactor_phase": true`
- **Round 1 only**: Refactoring analysis runs once — it is not part of the fix-verify cycle
- **Output**: `refactor-handoff.json` consumed by developer in Fix Consumption mode
- **Post-apply verification**: After developer applies refactoring, reviewer verifies behavior equivalence (new review dimension)

### Loop State Contribution

The refactorer contributes to `.loop-state.json`:

```json
{
  "refactor_result": {
    "triggered": true,
    "scope": "full",
    "smells_detected": 12,
    "smells_by_severity": { "critical": 2, "major": 5, "minor": 5 },
    "refactoring_actions": 10,
    "debt_density_per_kloc": 3.2,
    "estimated_effort_hours": 40,
    "handoff_file": "refactor-handoff.json",
    "conclusion": "Pass | Pass with warnings | Fail",
    "timestamp": "2026-06-30T10:00:00Z"
  }
}
```

### Serialization Triggers

- After Step 1 (scope determined) → partial state write
- After Step 3 (refactoring plan complete) → intermediate state write
- After Step 6 (handoff complete) → full state write with `refactor_result`

## Reference Documents

- `references/code-smells.md` — Code smell catalog, detection heuristics, severity classification, JavaFX-specific smells
- `references/refactoring-patterns.md` — Refactoring pattern catalog, before/after examples, safe sequencing rules, JavaFX-specific patterns
- `references/tech-debt.md` — Debt identification framework, prioritization matrix, repayment planning, metrics calculation

## Relationship to Other Skills

- **javafx-developer**: Consumes `refactor-handoff.json` in Fix Consumption mode — applies refactoring actions using the same location matching hierarchy (fingerprint → anchor → content → AST signature)
- **javafx-code-reviewer**: Performs independent behavior equivalence verification after refactoring is applied — new "Refactoring Verification" review dimension
- **javafx-runner**: Runs tests before and after refactoring to verify behavior preservation
- **javafx-orchestrator**: Manages the refactoring phase as an optional post-quality-gate step in the loop state machine

## EVALUATE.md

See `EVALUATE.md` for evaluation test cases that quantify refactoring quality.
