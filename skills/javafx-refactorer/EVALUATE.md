# EVALUATE.md — javafx-refactorer

> 18 evaluation test cases that quantify refactoring quality.
> Each test case includes: description, input, expected output, and pass criteria.

## Test Cases

### TC-01: Full Project Refactoring (Default Scope)
**Description**: User requests full refactoring analysis on an existing JavaFX project with multiple code smells.
**Input**: "Refactor my JavaFX inventory management app — it's gotten messy over 2 years of development."
**Expected Output**: All 4 dimensions activated. Smell catalog with 5+ smells detected, refactoring plan with 5+ actions, tech debt inventory with metrics, verification plan, refactor-handoff.json.
**Pass Criteria**: `refactor-handoff.json` exists with `scope: "full"`, `conclusion: "Pass"`, `smell_catalog` has 5+ items, `refactoring_plan` has 5+ actions, `tech_debt.total_items` matches smell catalog count.

### TC-02: Targeted Package Refactoring
**Description**: User requests refactoring only on the controller package.
**Input**: "Refactor the controller package — those classes are too big."
**Expected Output**: `scope: "targeted_package"`, only files in the controller package analyzed, smells detected only in controller files.
**Pass Criteria**: `smell_catalog` entries all have file paths within the controller package. No smells reported from other packages.

### TC-03: God Class Detection and Extract Class Recommendation
**Description**: A controller class has 850 lines, 35 methods, and 22 fields.
**Input**: Java source file with God Class smell.
**Expected Output**: Smell detected as `god_class` with severity Critical. Refactoring recommendation is `extract_class` with before/after snippets showing the extracted class.
**Pass Criteria**: Smell `type: "god_class"`, `severity: "Critical"`. Refactoring `type: "extract_class"`, `before_snippet` and `after_snippet` are non-empty, `new_files` lists the proposed extracted class.

### TC-04: Long Method Detection and Extract Method Recommendation
**Description**: A method has 75 lines and cyclomatic complexity of 15.
**Input**: Java source file with a long method.
**Expected Output**: Smell detected as `long_method` with severity Major. Refactoring recommendation is `extract_method`.
**Pass Criteria**: Smell `type: "long_method"`, `severity: "Major"`. Refactoring `type: "extract_method"`, before snippet shows the long method, after snippet shows it broken into named sub-methods.

### TC-05: Duplicated Code Detection Across Files
**Description**: Two controller classes have identical 15-line validation blocks.
**Input**: Two Java source files with duplicated code blocks.
**Expected Output**: Smell detected as `duplicated_code` with severity Critical (>6 lines). Refactoring recommendation is `extract_method` to a shared utility class.
**Pass Criteria**: Smell `type: "duplicated_code"`, `severity: "Critical"`. Refactoring `type: "extract_method"`, `new_files` includes a utility class path. Both source files are listed as target files.

### TC-06: Circular Dependency Detection
**Description**: Package A imports from package B, and package B imports from package A.
**Input**: Project with circular package dependencies.
**Expected Output**: Smell detected as `circular_dependency`. Refactoring recommendation is `move_class` or `extract_interface`.
**Pass Criteria**: Smell `type: "circular_dependency"`, `severity: "Critical"`. Refactoring breaks the cycle by moving a class or introducing an interface in a shared package.

### TC-07: Technical Debt Metrics Calculation
**Description**: Project has 12 smells across 3750 lines of code.
**Input**: Full project refactoring analysis.
**Expected Output**: Debt metrics calculated: `total_items: 12`, `debt_density_per_kloc: 3.2`, `estimated_effort_hours` is a positive number.
**Pass Criteria**: `tech_debt.total_items` equals smell catalog length. `debt_density_per_kloc` = total_items / (total_lines / 1000). `estimated_effort_hours` is sum of per-action effort estimates (S=1h, M=3h, L=8h).

### TC-08: Repayment Plan Phasing
**Description**: 10 refactoring actions with mixed priorities.
**Input**: Smell catalog with 2 Critical, 5 Major, 3 Minor smells.
**Expected Output**: Repayment plan with 4 phases: Phase 1 has 2 actions (P1), Phase 2 has 5 actions (P2), Phase 3 has 3 actions (P3), Phase 4 is empty.
**Pass Criteria**: `repayment_plan.phase_1_immediate` has 2 RF-IDs, `phase_2_high` has 5, `phase_3_medium` has 3, `phase_4_low` is empty or absent.

### TC-09: Behavior Equivalence Check Plan
**Description**: Each refactoring action must include a behavior equivalence check.
**Input**: Full project refactoring analysis.
**Expected Output**: Every action in `refactoring_plan` has `behavior_equivalence_check` with `method_signatures_preserved` field.
**Pass Criteria**: 100% of refactoring actions have `behavior_equivalence_check` object. `verification_plan.semantic_checks` includes all 4 checks (signature_preservation, call_site_integrity, field_access_integrity, import_graph_acyclic).

### TC-10: AST Node Signature in Smell Catalog
**Description**: All smell locations must include AST node signatures for Java files.
**Input**: Java source files with smells.
**Expected Output**: Each smell in `smell_catalog` has `ast_node_signature` field populated.
**Pass Criteria**: All smells in `.java` files have non-null `ast_node_signature` following the format `com.example.Class#method(params)` or `com.example.Class`. Smells in non-Java files (FXML, CSS) have `null`.

### TC-11: Before/After Snippets Required
**Description**: Every refactoring action must include before and after code snippets.
**Input**: Full project refactoring analysis.
**Expected Output**: Every action in `refactoring_plan` has non-empty `before_snippet` and `after_snippet`.
**Pass Criteria**: 100% of refactoring actions have both snippets. Snippets contain actual Java code (not placeholders). After snippet demonstrates the refactored structure.

### TC-12: Safe Sequencing and Dependencies
**Description**: Refactoring plan must identify dependencies between actions.
**Input**: Multiple refactoring actions where some depend on others.
**Expected Output**: `dependencies` field in each action lists prerequisite RF-IDs. `developer_instructions.apply_order` specifies the correct sequence.
**Pass Criteria**: If RF-003 depends on RF-001, then RF-001's `dependencies` is empty and RF-003's `dependencies` includes "RF-001". `apply_order` respects all dependency constraints.

### TC-13: JavaFX-Specific Smell Detection
**Description**: Detect JavaFX-specific smells like UI logic in controller and blocking UI thread.
**Input**: Controller with business logic in event handler and database call on JavaFX Application Thread.
**Expected Output**: Smells detected as `ui_logic_in_controller` and `blocking_ui_thread`.
**Pass Criteria**: Smell catalog includes entries with `type: "ui_logic_in_controller"` and `type: "blocking_ui_thread"`. Refactoring recommendations include `extract_controller_logic_to_service` for the UI logic smell.

### TC-14: Dual Output Format
**Description**: Both Markdown and JSON reports are generated by default.
**Input**: Full project refactoring request (no .loop-config.json).
**Expected Output**: `refactor-report.md` (human-readable) and `refactor-report.json` (machine-readable).
**Pass Criteria**: Both files exist. JSON validates against `report-schema.json`. Markdown contains all sections from the template.

### TC-15: Standalone Mode (No Developer Trigger)
**Description**: Refactorer can run without triggering developer to apply changes.
**Input**: "Analyze the code for refactoring opportunities — don't apply changes yet."
**Expected Output**: Refactoring artifacts produced, `refactor-handoff.json` exists, no source files modified.
**Pass Criteria**: `refactor-handoff.json` exists with complete smell catalog and refactoring plan. No Java source files are modified (git diff shows no changes to src/). Loop state shows `refactor_result` but no `rounds` array (developer not triggered).

### TC-16: Behavior Equivalence Violation (Negative)
**Description**: Refactoring recommendation changes observable behavior but is NOT flagged as behavior-risky.
**Input**: A refactoring plan recommends inlining a method that has a side effect (e.g., logging or state mutation) into two call sites. The `behavior_equivalence_check` for this action does not flag the side-effect divergence.
**Expected Output**: The refactoring plan should mark this action with `behavior_equivalent: false` or `behavior_risk: "high"` and include a `verification_required` flag. If it doesn't, the test fails.
**Pass Criteria**:
- [ ] The action's `behavior_equivalence_check` identifies the side-effect divergence
- [ ] The action is marked `behavior_equivalent: false` or `behavior_risk: "high"`
- [ ] `verification_plan` includes a specific test recommendation for this action
- [ ] If the check fails to detect the behavior change, the refactorer's conclusion must be `Fail` or `Pass with warnings` (not clean `Pass`)

### TC-17: Constraint Violation — Modifying Production Code (Negative)
**Description**: Refactorer must NOT modify any production source files. This test verifies the constraint is enforced.
**Input**: Full project refactoring request with 5 detected smells and a complete refactoring plan.
**Expected Output**: Refactorer produces analysis artifacts (smell catalog, refactoring plan, tech debt inventory, verification plan) and `refactor-handoff.json`. No files under `src/main/` are modified.
**Pass Criteria**:
- [ ] `git diff --name-only src/main/` returns empty (no production code changes)
- [ ] `refactor-handoff.json` contains all recommended actions but none are applied
- [ ] Only `refactor-report.md`, `refactor-report.json`, and `refactor-handoff.json` are created/modified
- [ ] The `developer_instructions` section describes how developer should apply the changes (refactorer delegates, does not execute)
- [ ] If any `src/main/` file is modified, the test fails with "Constraint violated: refactorer modified production code"

### TC-18: Refactor Handoff Fix Consumption Compatibility
**Description**: Verify that `refactor-handoff.json` produced by the refactorer contains the unified Fix Handoff fields in every `refactoring_plan` item and can be consumed by the developer's Fix Consumption Protocol without conversion.
**Input**: A full project refactoring request that produces `refactor-handoff.json` with 3 refactoring actions (extract_class, extract_method, move_class). The handoff is then passed to `javafx-developer`'s Fix Consumption Protocol.
**Expected Output**: Each `refactoring_plan` item contains `target_file`, `target_lines`, `fix_type`, `fix_priority`, `code_fingerprint`, `anchor_pattern`, and `ast_node_signature` — matching the unified Fix Handoff format defined in `javafx-orchestrator/SKILL.md`. The developer's Fix Consumption Protocol parses the handoff and activates Fix Consumption mode without any field conversion.
**Pass Criteria**:
- [ ] All `refactoring_plan` items contain `target_file` (file path string)
- [ ] All items contain `target_lines` matching the valid pattern `^\d+(-\d+)?$` (single line or start-end range)
- [ ] All items contain `fix_type` with value `replace`, `insert`, or `delete`
- [ ] All items contain `fix_priority` as an integer ≥ 1
- [ ] All items contain `code_fingerprint` as a valid SHA-256 hash matching `^[a-f0-9]{64}$`
- [ ] All items contain `anchor_pattern` (surrounding context signature)
- [ ] All items contain `ast_node_signature` (a method/field/class signature string, or `null` for non-Java targets)
- [ ] `smell_catalog` field names aligned with `report-schema.json` (`smell_id`, `lines`, `detail`, `ast_node_signature`)
- [ ] Developer Fix Consumption Protocol can parse the handoff without conversion (recognizes the unified Fix Handoff fields and activates Step 5.5)
