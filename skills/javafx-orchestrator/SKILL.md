---
name: javafx-orchestrator
description: |
  Closed-Loop Orchestration Controller for JavaFX Skill Set — manages the full
  development cycle (requirements → architect → design → generate → review →
  verify → test → fix → refactor → document → deploy) across ten JavaFX skills.
  Provides the authoritative Loop State Machine, Combined Quality Gate, Fix
  Handoff Format, Serialization Triggers, and State Recovery Protocol definitions.
  Triggered when the user asks to "orchestrate", "run the full loop", or when
  multiple skills need coordination.
license: Apache-2.0
compatibility: Requires JDK 17+. Supports JavaFX 17/21/24/25/26.
metadata:
  author: DongZY0617
  version: "1.0"
triggers:
  - orchestrate
  - full loop
  - run the cycle
  - coordinate skills
  - closed-loop
  - generate and review and verify
depends_on:
  - javafx-requirements (optional)
  - javafx-architect (optional)
  - javafx-designer (optional)
  - javafx-developer
  - javafx-code-reviewer
  - javafx-runner
  - javafx-tester (optional)
  - javafx-refactorer (optional)
  - javafx-docgen (optional)
  - javafx-deployer (optional)
consumes_from:
  - all skills (loop state contributions)
produces_for:
  - all skills (loop state, routing, config)
---

# javafx-orchestrator

> Closed-Loop Orchestration Controller for JavaFX Skill Set — manages the (optional) requirements → (optional) architecting → (optional) design → generate → review → verify → test → fix → (optional) refactor → document → (optional) deploy cycle across `javafx-requirements`, `javafx-architect`, `javafx-designer`, `javafx-developer`, `javafx-code-reviewer`, `javafx-runner`, `javafx-tester`, `javafx-refactorer`, `javafx-docgen`, and `javafx-deployer`.

## Metadata

- **Version**: 1.0
- **JavaFX Support**: 17 / 21 / 24 / 25 / 26
- **Dependencies**: `javafx-requirements`, `javafx-architect`, `javafx-designer`, `javafx-developer`, `javafx-code-reviewer`, `javafx-runner`, `javafx-tester`, `javafx-refactorer`, `javafx-docgen`, `javafx-deployer`
- **Role**: Orchestration layer — does not gather requirements, architect, design, generate, review, verify, test, refactor, document, or deploy code directly; coordinates the ten skills and manages loop state

## Purpose

`javafx-orchestrator` serves as the central coordination layer for the JavaFX skill set's closed-loop development cycle. It extracts the shared Loop Orchestration Protocol, Fix Handoff format, and Combined Quality Gate from the individual skills into a single source of truth, reducing protocol duplication and enabling independent evolution of orchestration logic.

### Why a Separate Orchestrator?

Before this skill existed, the Loop Orchestration Protocol, Fix Handoff format, and Combined Quality Gate were defined in all three skills' SKILL.md files. This created:
- **Maintenance burden**: Protocol changes required updating 3 files
- **Inconsistency risk**: Versions could drift between skills
- **Coupling**: Orchestration logic was embedded in generation/review/verification logic

The orchestrator addresses these by:
- Defining protocols once, referenced by all five skills
- Managing `.loop-state.json` as the single state authority
- Making routing decisions (which skill to trigger next) based on gate results
- Providing a unified API for external tools (CI/CD, IDE plugins) to interact with the loop

## Core Concepts

### 1. Loop Orchestration Protocol

The protocol defines a state machine that governs the closed-loop cycle. Since Round 1, the reviewer (static) and runner (dynamic) operate **in parallel** — they are independent and do not depend on each other's results:

```
                              ┌→ Reviewing ─────────────────────────────────────┐
                              │   (reviewer produces Fix Handoffs)              │
[Start] → (optional) Architecting → (optional) Designing → Generating → ┤        ├→ Combined Gate
              (architect produces     (designer produces    │           │   (runner produces Fix Handoffs)│   (reviewer AND runner
               architecture-handoff)   design-handoff.json) └───────────└→ Verifying ────────────────────┘    both Pass?)
                                                                        ↓ Pass          ↓ Fail
                                                                  Deep Testing     (round < max?) → Fixing
                                                                  (tester)         (round = max?) → [Paused]
                                                                        ↓
                                                                  Test Gate
                                                                  (tester Pass?)
                                                                        ↓ Pass          ↓ Fail
                                                                  (optional) Refactoring    (round < max?) → Fixing
                                                                  (refactorer)              (round = max?) → [Paused]
                                                                        ↓
                                                                  DocGen
                                                                        ↓
                                                                  Deploying
                                                                  (optional)
                                                                        ↓
                                                                  [Shipped]

                              ┌→ Re-Reviewing (incremental) ───────────────────┐
Fixing (merge & dedup) → ─────┤                                                ├→ Combined Gate
                              └→ Re-Verifying (incremental) ───────────────────┘    (reviewer AND runner
                                                                                 both Pass?)
                                                                        ↓ Pass          ↓ Fail
                                                                  Re-Testing        (round < max?) → Fixing
                                                                  (targeted)        (round = max?) → [Paused]
                                                                        ↓
                                                                  Test Gate
                                                                        ↓ Pass          ↓ Fail
                                                                  (optional) Refactoring    (round < max?) → Fixing
                                                                  (refactorer)              (round = max?) → [Paused]
                                                                        ↓
                                                                  DocGen
                                                                  (optional)
                                                                        ↓
                                                                  Deploying
                                                                  (optional)
                                                                        ↓
                                                                  [Shipped]
```

> **Requirements phase is optional**: If the user does not request requirements engineering ("create a JavaFX app" without "gather requirements" or "user stories"), the Requirements phase is skipped and the loop starts at Architecting (if enabled) or Designing (if enabled) or Generating. When the user says "gather requirements and design the architecture" or `.loop-config.json` has `"requirements_phase": true`, Requirements runs first and produces `requirements-handoff.json` which Architect consumes in Step 1 and Developer consumes in Step 1.

> **Architect phase is optional**: If the user does not request architecture design ("create a JavaFX app" without "architecture"), the Architecting phase is skipped and the loop starts directly at Designing (if enabled) or Generating. When the user says "design the architecture and generate" or `.loop-config.json` has `"architect_phase": true`, Architect runs first and produces `architecture-handoff.json` which Developer consumes in Step 4.

> **Design phase is optional**: If the user does not request design ("create a JavaFX app" without "design"), the Designing phase is skipped and the loop starts directly at Generating. When the user says "design and generate" or `.loop-config.json` has `"design_phase": true`, Designer runs first and produces `design-handoff.json` which Developer consumes in Step 4.

> **Test Gate is optional**: If `.loop-config.json` has `"deep_testing": false`, the Test Gate is skipped and the Combined Gate (reviewer + runner) is the final quality gate.

> **Deploy phase is optional**: If `.loop-config.json` has `"deploy_phase": false` (default), the Deploying phase is skipped and the project is shipped after DocGen. When the user requests deployment ("set up CI/CD", "deploy my app") or config has `"deploy_phase": true`, Deployer runs after DocGen to generate CI/CD pipelines, signing configs, and monitoring setup.

> **DocGen is optional**: If `.loop-config.json` has `"docgen": false`, the documentation generation step is skipped and the project is delivered immediately after the Test Gate (or Combined Gate if deep testing is disabled). When enabled (default), DocGen runs after all quality gates pass. The documentation gate is configurable: `"non-blocking"` (default — never blocks delivery) or `"blocking"` (blocks delivery on failure).

**Parallel execution rationale**: The reviewer reads code and checks against standards (no execution); the runner executes build commands and captures output (no code reading). They have no data dependency — the reviewer does not need runner results to review, and the runner does not need reviewer results to compile/run. Running them in parallel cuts single-round wall-clock time by ~50%.

### 2. Loop Rules

| Rule | Definition | Termination |
|------|-----------|-------------|
| Max rounds | Fix → verify cycle loops at most `max_rounds` rounds (default: 3, configurable via `.loop-config.json` `max_rounds` field) | At `max_rounds` rounds without pass → pause, report to user |
| Requirements phase | If user requests "gather requirements" / "user stories" / "acceptance criteria" or `requirements_phase: true` in config, requirements runs before architect and developer to produce stakeholder analysis, user stories, NFRs, and RTM seed | Requirements phase is optional — skipped when user requests direct code generation. Runs once (not part of fix cycle) |
| Architect phase | If user requests "design the architecture and generate" or `architect_phase: true` in config, architect runs before designer and developer to produce technology selection, UML diagrams, ADRs, and prototype validation | Architect phase is optional — skipped when user requests direct code generation. Runs once (not part of fix cycle) |
| Design phase | If user requests "design and generate" or `design_phase: true` in config, designer runs before developer to produce FXML prototypes, CSS themes, and icon configs | Design phase is optional — skipped when user requests direct code generation |
| Parallel execution | Reviewer and runner execute in parallel every round (no data dependency between static review and dynamic verification) | Cuts single-round wall-clock time by ~50% |
| Re-review strategy | Round 1: full re-review; Round 2+: incremental re-review (only dimensions touched by fixes) | Incremental re-review checks only fix-affected dimensions |
| Re-verify strategy | Every round: compile verification mandatory; runtime/test/packaging based on fix scope | Compile failure short-circuits: skip runtime and packaging |
| Fix Handoff merge | When both reviewer and runner produce Fix Handoffs, orchestrator deduplicates before passing to developer | Same file + overlapping lines → keep higher severity |
| Deep testing | If `deep_testing: true` (default), tester is triggered after Combined Gate passes; tester's 4 dimensions run in parallel (`tester_parallel: true` default) — performance, security, accessibility, and visual regression (requires TestFX+Monocle, auto-skipped if absent); tester Fix Handoffs are merged with reviewer+runner Fix Handoffs for developer consumption | Tester requires runnable project — skipped if runner fails; visual regression auto-skipped if TestFX/Monocle not in pom.xml |
| Documentation generation | If `docgen: true` (default), docgen is triggered after Test Gate passes (or after refactoring if enabled); generates API reference, user manual, architecture doc, changelog, README; optionally Javadoc HTML (`doc_javadoc_html: true`) | Gate mode configurable: `"non-blocking"` (default — never blocks delivery) or `"blocking"` (`doc_gate_mode: "blocking"` — blocks delivery on failure) |
| Refactoring | If `refactor_phase: true` or user requests refactoring, refactorer is triggered after Test Gate passes to detect code smells, generate refactoring recommendations, and manage technical debt | Refactorer produces `refactor-handoff.json` consumed by developer; reviewer verifies behavior equivalence (Dimension 9). Refactoring is optional — skipped by default |
| Deployment configuration | If `deploy_phase: true` or user requests deployment, deployer is triggered after DocGen completes; generates CI/CD pipelines, signing configs, auto-update, monitoring | Deployer generates config only — does NOT execute builds; produces `deploy-handoff.json` with setup instructions |
| Pre-fix backup | Before applying fixes, developer copies all target files to `.fix-backup/{timestamp}/` with a manifest | Enables automatic rollback if post-fix compilation fails |
| Post-fix rollback | After applying fixes, developer runs `mvn compile -q`; if compilation fails, all modified files are restored from backup, fixes marked `rolled_back` | Rollback restores project to pre-fix state; recommends manual intervention; event recorded in `rollback_events` |
| Backup cleanup | `.fix-backup/` is auto-cleaned when the loop passes the quality gate (delivery) | Paused/aborted loops preserve `.fix-backup/` for manual inspection |
| Convergence detection | Compare current round issue count with previous round | 2 consecutive non-converging rounds → pause, report to user |
| User intervention points | Max rounds reached / non-converging / unfixable issues | Pause with current state report |
| State persistence | Loop state serialized to `.loop-state.json` after each round | Cross-session recovery enabled |
| Dashboard generation | If `dashboard: true` (default), orchestrator generates `target/loop-dashboard.html` after every state update — embeds `.loop-state.json` data into an ECharts-powered HTML dashboard with auto-refresh | Dashboard can be disabled in CI environments via `dashboard: false` |

### 3. Combined Quality Gate

The loop terminates as **Pass** only when BOTH reviewer and runner pass:

| Reviewer Conclusion | Runner Conclusion | Overall | Action |
|---------------------|-------------------|---------|--------|
| Pass | Pass | **Pass** | Proceed to Test Gate (Step 4.5) → DocGen (Step 4.6) → Deliver |
| Pass | Conditional/Fail | **Fail** | Fix runner issues, continue loop |
| Conditional/Fail | Pass | **Fail** | Fix reviewer issues, continue loop |
| Conditional/Fail | Conditional/Fail | **Fail** | Fix both, continue loop |

**Priority rule**: When reviewer is Fail, fix reviewer issues first (static issues are usually root causes; fixing them may resolve runtime issues too).

### 5.5. Test Gate (After Tester)

The Test Gate evaluates after the tester's 4 parallel tracks complete. The aggregated `tester_result.conclusion` determines the gate result:

| Tester Aggregated Conclusion | Combined Gate Status | Overall Test Gate | Action |
|------------------------------|---------------------|-------------------|--------|
| Pass | Pass | **Pass** | Proceed to Refactoring (optional) → DocGen |
| Conditional Pass | Pass | **Conditional Pass** | Proceed to DocGen; merge tester Fix Handoffs with reviewer+runner for next round if any |
| Fail | Pass | **Fail** | Fix tester issues, continue loop (round < max) or pause (round = max) |
| Skipped (all tracks) | Pass | **Skipped** | Skip Test Gate, proceed to DocGen (tester dependencies missing or deep_testing disabled) |
| Pass | Fail | **N/A** | Combined Gate fails first — tester is not triggered |
| Any | Fail | **N/A** | Combined Gate must pass before Test Gate is evaluated |

**Priority rule**: When tester is Fail, merge tester Fix Handoffs with any remaining reviewer/runner Fix Handoffs for the developer to consume in the next fix round. Tester Critical findings (e.g., security vulnerabilities) take priority over reviewer/runner Major findings.

**Track-level partial failure**: If some tracks Pass but others Fail, the aggregated conclusion is Fail (any Fail → Fail). If some tracks Pass and others are Skipped (no Fail), the aggregated conclusion is Conditional Pass. See tester SKILL.md Section "Aggregation Gate" for the full 7-scenario matrix.

### 4. Individual Gate Criteria

**Reviewer Gate**:
- **Pass**: No Critical/Major issues, pass rate >= 80%
- **Conditional Pass**: Has Major but no Critical, clear fix plans exist
- **Fail**: Has Critical issues, must fix before delivery

**Runner Gate**:
- **Pass**: No Critical or Major issues, all check items pass (or skipped items are documented), pass rate >= 80%, JaCoCo line coverage >= 60% on critical paths (Controller/ViewModel classes)
- **Conditional Pass**: Has Major but no Critical, all Major issues have clear fix plans; runtime verification passes but packaging verification has non-blocking issues
- **Fail**: Has Critical issues (compilation errors, startup failures, etc.), must be fixed before delivery

### 5. Fix Handoff Format

The Fix Handoff is the machine-readable format used by reviewer and runner to communicate fix instructions to developer:

| Field | Type | Description |
|-------|------|-------------|
| `target_file` | string | File path to modify |
| `target_lines` | string | Line range (e.g., "45-60") |
| `fix_type` | enum | `replace`, `insert`, or `delete` |
| `fix_priority` | number | 1 (highest) to N, for batch ordering |
| `code_fingerprint` | string | SHA-256 hash of problematic code snippet (whitespace-normalized) |
| `anchor_pattern` | string | Surrounding context signature (2 lines before + 2 lines after) |
| `ast_node_signature` | string | AST-level anchor: fully qualified method or class signature (e.g., `com.example.controller.UserController#handleSave()`, `com.example.model.User`). Enables location matching when code has been moved or heavily refactored — the AST node is found by signature search, not line numbers. See [AST Anchor Format](#ast-anchor-format) below |

<a id="ast-anchor-format"></a>
#### AST Anchor Format

The `ast_node_signature` field provides a refactor-resistant location anchor that survives line drift, method relocation, and file renames. It uses Java fully qualified names with a `#` separator between class and member:

| Scope | Format | Example |
|-------|--------|---------|
| Method-level issue | `{package}.{Class}#{methodName}({paramTypes})` | `com.example.controller.UserController#handleSave(ActionEvent)` |
| Field-level issue | `{package}.{Class}#{fieldName}` | `com.example.model.User#userName` |
| Class-level issue | `{package}.{Class}` | `com.example.controller.UserController` |

**Parameter type rules**:
- Use fully qualified types for custom classes: `com.example.model.User`
- Use simple names for JDK types: `String`, `int`, `boolean`, `ActionEvent` (from `javafx.event.ActionEvent`)
- Use `[]` suffix for arrays: `String[]`
- Use `...` for varargs: `String...`
- Omit parameter names — only types: `handleSave(ActionEvent)` not `handleSave(ActionEvent event)`

**Extraction rules** (reviewer and runner):
1. If the issue is **inside a method body**, extract the enclosing method's signature
2. If the issue is a **field declaration or initializer**, extract the field name
3. If the issue is at **class level** (e.g., missing class-level annotation, class naming convention), extract the class fully qualified name
4. If the issue is in a **nested class**, use the outer class chain: `com.example.Outer$Inner#method()`
5. If the issue is in a **static initializer** or **instance initializer**, use `{package}.{Class}#<clinit>` or `{package}.{Class}#<init>`
6. If the file is **not a Java source file** (e.g., FXML, CSS, `module-info.java`), set `ast_node_signature` to `null` — AST anchors apply only to `.java` files

### 6. Loop State Serialization

The loop state is persisted to `.loop-state.json` in the project root:

```json
{
  "loop_id": "uuid-v4",
  "project": "project-name",
  "created_at": "2026-06-29T10:00:00Z",
  "updated_at": "2026-06-29T10:15:30Z",
  "current_round": 2,
  "max_rounds": 3,
  "status": "requirements | architecting | designing | generating | reviewing_and_verifying | testing | refactoring | fixing | passed | docgen | fix_documentation | deploying | delivered | shipped | paused",
  "requirements_result": {
    "triggered": true,
    "scope": "full | user_stories_only | nfr_only | change_impact_only",
    "stakeholders_identified": 3,
    "user_stories_count": 12,
    "nfr_count": 8,
    "acceptance_criteria_count": 28,
    "change_impact_reports": 0,
    "handoff_file": "requirements/requirements-handoff.json",
    "conclusion": "Pass | Pass with warnings | Fail",
    "timestamp": "2026-06-30T10:00:00Z"
  },
  "architect_result": {
    "triggered": true,
    "scope": "full",
    "architecture_pattern": "MVVM + Service Layer",
    "modules_designed": 4,
    "uml_diagrams": 3,
    "adr_count": 5,
    "database_schema": true,
    "database_tables": 6,
    "threat_model": true,
    "threats_identified": 12,
    "security_adrs": 3,
    "prototype_validations": 2,
    "handoff_file": "architecture/architecture-handoff.json",
    "conclusion": "Pass | Pass with warnings | Fail",
    "timestamp": "2026-06-30T10:00:00Z"
  },
  "design_result": {
    "triggered": true,
    "dimensions": ["prototype", "theme", "flow", "responsive", "icons"],
    "screens_designed": 3,
    "artifacts": ["design/fxml/main-view.fxml", "design/css/light-theme.css", "design/css/dark-theme.css", "design/flow/interaction-flow.mmd", "design/icons/icon-config.json"],
    "handoff_file": "design/design-handoff.json",
    "conclusion": "Pass | Pass with warnings | Fail",
    "timestamp": "2026-06-29T10:00:00Z"
  },
  "rounds": [
    {
      "round": 1,
      "phase": "review_and_verify",
      "reviewer_result": { "conclusion": "Fail", "critical": 2, "major": 3, "minor": 1, "pass_rate": 0.45 },
      "runner_result": { "conclusion": "Fail", "critical": 1, "major": 2, "pass_rate": 0.55 },
      "tester_perf_result": null,
      "tester_sec_result": null,
      "tester_a11y_result": null,
      "tester_vr_result": null,
      "tester_result": null,
      "combined_gate": "Fail",
      "fixes_applied": 5,
      "fixes_skipped": 1,
      "fixes_rolled_back": 0,
      "fix_handoffs_merged": 2,
      "fix_handoffs_deduplicated": 1,
      "fix_handoffs": [
        { "target_file": "src/main/java/com/example/controller/UserController.java", "target_lines": "45-60", "fix_type": "replace", "fix_priority": 1, "source": "reviewer", "status": "applied", "ast_node_signature": "com.example.controller.UserController#handleSave(ActionEvent)" },
        { "target_file": "src/main/java/com/example/view/MainWindow.java", "target_lines": "78-85", "fix_type": "insert", "fix_priority": 2, "source": "runner", "status": "applied", "ast_node_signature": "com.example.view.MainWindow#initialize()" },
        { "target_file": "src/main/java/com/example/util/ConfigLoader.java", "target_lines": "12-15", "fix_type": "replace", "fix_priority": 3, "source": "reviewer", "status": "skipped", "ast_node_signature": "com.example.util.ConfigLoader#load(String)" }
      ]
    }
  ],
  "convergence_trend": [6, 3],
  "last_fix_handoff": {
    "source": "merged",
    "reviewer_count": 3,
    "runner_count": 2,
    "merged_count": 4,
    "timestamp": "2026-06-29T10:15:00Z"
  },
  "rollback_events": [
    {
      "round": 2,
      "timestamp": "2026-06-29T10:20:00Z",
      "backup_dir": ".fix-backup/2026-06-29-101530/",
      "files_restored": 4,
      "compile_error_count": 2,
      "fix_count_rolled_back": 4,
      "rollback_verification": "passed"
    }
  ],
  "backup_cleanup": {
    "last_cleanup": "2026-06-29T10:25:00Z",
    "dirs_removed": 2,
    "space_freed_bytes": 456789
  },
  "docgen_result": {
    "triggered": true,
    "conclusion": "Pass | Pass with warnings | Fail (docs skipped)",
    "doc_gate_mode": "non-blocking",
    "gate_blocked": false,
    "javadoc_html_generated": false,
    "generated_documents": ["docs/api-reference.md", "docs/user-manual.md", "docs/architecture.md", "docs/CHANGELOG.md", "README.md"],
    "coverage": {
      "api_coverage_percent": 85.0,
      "fxml_coverage_percent": 100.0,
      "architecture_documented": true
    },
    "warnings": ["3 public methods missing Javadoc comments"],
    "timestamp": "2026-06-29T10:20:00Z"
  },
  "deploy_result": {
    "triggered": true,
    "dimensions": ["ci_cd", "release", "signing", "auto_update", "monitoring", "distribution", "rollback"],
    "platforms": ["windows", "macos", "linux"],
    "ci_cd_platform": "github-actions",
    "artifacts": [".github/workflows/build.yml", "scripts/sign-windows.sh", "scripts/notarize-macos.sh", "src/main/java/.../UpdateChecker.java", "packaging/msix/AppxManifest.xml", "packaging/snap/snapcraft.yaml", "docs/rollback-runbook.md"],
    "handoff_file": "deploy-handoff.json",
    "required_secrets": ["WINDOWS_CERT_PFX", "WINDOWS_CERT_PASSWORD", "APPLE_ID", "APPLE_TEAM_ID", "APPLE_APP_PASSWORD"],
    "conclusion": "Pass | Pass with warnings | Fail (deployment skipped)",
    "timestamp": "2026-06-30T10:00:00Z"
  },
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
  },
  "next_action": "requirements | architecting | designing | generating | incremental_review_and_verify | testing | refactoring | fixing | manual_intervention | passed | docgen | fix_documentation | deploying | delivered | shipped | paused"
}
```

<a id="parallel-write-safety"></a>
#### Loop State Parallel Write Safety

Two levels of parallelism exist in the loop, both protected by field isolation:

**Level 1 — Reviewer ∥ Runner** (Step 3): When the reviewer and runner execute in parallel, both need to write their results to `.loop-state.json`. The following rules ensure safe concurrent access:

1. **Field isolation**: The reviewer writes **only** to `rounds[current_round].reviewer_result`; the runner writes **only** to `rounds[current_round].runner_result`. These are disjoint JSON paths — no field is written by both
2. **No shared mutable state**: Neither skill modifies `status`, `current_round`, `convergence_trend`, or `next_action` — those are written **only** by the orchestrator after both skills complete (Step 3)
3. **Read-before-write**: Each skill reads the current state file, modifies only its own field, and writes back the full file. If a write conflict occurs (the file was modified between read and write), re-read and retry (optimistic concurrency)
4. **Orchestrator as coordinator**: The orchestrator does not write to the state file while reviewer and runner are running. It waits for both to complete, then performs the Combined Gate update in a single atomic write

**Level 2 — Tester Track A ∥ Track B ∥ Track C ∥ Track D** (Step 4.5): When the tester's four dimensions execute in parallel, each track writes to its own isolated field. The same safety rules apply:

5. **Track field isolation**: Performance writes **only** to `tester_perf_result`; Security writes **only** to `tester_sec_result`; Accessibility writes **only** to `tester_a11y_result`; Visual Regression writes **only** to `tester_vr_result`. These are disjoint JSON paths — no field is written by multiple tracks
6. **No cross-track reads**: Each track operates independently and does not read another track's in-progress results. Cross-track correlation happens only in Step 7 (Aggregation), after all tracks complete
7. **Aggregated field**: The unified `tester_result` field is computed **after** all tracks finish — it is written by the tester (or orchestrator) in a single atomic write, never by individual tracks
8. **Partial failure**: If one track errors or times out, it writes `"conclusion": "Skipped"` to its own field. The other tracks are unaffected and continue to completion

### 7. Loop Visualization Dashboard

The orchestrator auto-generates an HTML dashboard after every state update, providing real-time visibility into loop progress, convergence trends, and Fix Handoff status. The dashboard is a self-contained HTML file that can be opened in any browser and auto-refreshes every 10 seconds.

#### Data Source

The dashboard reads all visualization data from `.loop-state.json` — no separate data file is needed. The following fields are consumed:

| Field | Dashboard Usage |
|-------|-----------------|
| `project`, `updated_at` | Header: project name and last-updated timestamp |
| `current_round`, `max_rounds` | Status card: round progress (e.g., "2 / 3") |
| `status` | Status card: loop status badge (designing/fixing/passed/shipped/etc.) |
| `convergence_trend` | Status card + line chart: issue count trend across rounds |
| `rounds[].reviewer_result` | Line chart: reviewer issue count per round; severity breakdown bar chart |
| `rounds[].runner_result` | Line chart: runner issue count per round; severity breakdown bar chart |
| `rounds[].tester_result` | Phase timeline: Test phase status |
| `rounds[].tester_perf_result` | Track timeline: Performance track status and duration |
| `rounds[].tester_sec_result` | Track timeline: Security track status and duration |
| `rounds[].tester_a11y_result` | Track timeline: Accessibility track status and duration |
| `rounds[].tester_vr_result` | Track timeline: Visual Regression track status and duration |
| `rounds[].combined_gate` | Status card: latest Combined Gate result |
| `rounds[].fixes_applied/skipped/rolled_back` | Status card: aggregate fix counts |
| `rounds[].fix_handoffs` | Fix Handoff table: per-handoff details with status markers |
| `design_result` | Phase timeline: Design phase status |
| `docgen_result` | Phase timeline: DocGen phase status |
| `deploy_result` | Phase timeline: Deploy phase status |
| `requirements_result` | Phase timeline: Requirements phase status |
| `rollback_events` | Rollback events section: rollback history with verification status |
| `next_action` | Status card: next planned action |

#### Dashboard Layout

```
┌─────────────────────────────────────────────────────────────────┐
│  Header: Project Name | Last Updated | Auto-refresh: 10s       │
├──────────┬──────────┬──────────┬──────────┬──────────┬─────────┤
│  Round   │  Status  │ Converg. │  Gate    │  Fixes   │ Rollback│
│  2 / 3   │ Fixing   │ ↓ Decrease│  Fail   │  Applied │    0    │
├──────────┴──────────┴──────────┴──────────┴──────────┴─────────┤
│  Convergence Trend (ECharts)  │  Severity Breakdown (ECharts)  │
│  Line: Reviewer/Runner/Total  │  Stacked Bar: Critical/Major/  │
│                                │  Minor per round               │
├─────────────────────────────────────────────────────────────────┤
│  Phase Timeline                                                 │
│  Req → Arch → Design → Generate → Review → Verify → Test → DocGen → Deploy  │
│  [passed] [passed] [passed] [passed]  [failed]  [failed] [pending] ...      │
├─────────────────────────────────────────────────────────────────┤
│  Fix Handoff List                                               │
│  Round | File | Lines | Type | Priority | Source | Status      │
│  1     | UserController.java | 45-60 | replace | 1 | reviewer  | ✅ Applied  │
│  1     | MainWindow.java | 78-85 | insert | 2 | runner         | ✅ Applied  │
│  1     | ConfigLoader.java | 12-15 | replace | 3 | reviewer    | ⏭ Skipped  │
├─────────────────────────────────────────────────────────────────┤
│  Rollback Events (shown only if rollback_events is non-empty)   │
│  Round 2: 4 files restored, 2 compile errors, Verify: passed   │
├─────────────────────────────────────────────────────────────────┤
│  Footer: Generated by javafx-orchestrator · .loop-state.json   │
└─────────────────────────────────────────────────────────────────┘
```

#### Generation Protocol

The orchestrator generates the dashboard after **every state serialization** — i.e., whenever `.loop-state.json` is updated. This includes:

1. Read `.loop-state.json` from the project root
2. Read the dashboard template from `templates/loop-dashboard.html`
3. Replace the `{{LOOP_STATE_JSON}}` placeholder with the actual JSON content (stringified)
4. Write the result to `target/loop-dashboard.html`
5. The dashboard is self-contained — all data is embedded in the HTML, no external file dependencies (except the ECharts CDN)

**Generation triggers** (every state update):
- After Step 0 (requirements phase completion)
- After Step 0.5 (architect phase completion)
- After Step 1 (loop initialization)
- After Step 1.5 (design phase completion)
- After Step 2 (generation completion)
- After Step 3 (parallel review + verify completion)
- After Step 4 (Combined Gate evaluation)
- After Step 4.5 (Test Gate evaluation)
- After Step 4.55 (refactoring phase completion)
- After Step 4.6 (DocGen completion)
- After Step 4.7 (Deploy completion)
- After Step 5 (fix cycle completion)
- After Step 6 (state recovery)

#### Auto-Refresh

The dashboard HTML includes `<meta http-equiv="refresh" content="10">` in the `<head>`, causing the browser to reload the page every 10 seconds. Since the orchestrator regenerates the file after each state update, the user sees near-real-time loop progress by keeping the dashboard open in a browser tab.

#### Fix Handoff Status Markers

Each Fix Handoff entry in the dashboard table includes a `status` field with one of three values:

| Status | Icon | Color | Meaning |
|--------|------|-------|---------|
| `applied` | ✅ | Green | Fix was successfully applied and compiled |
| `skipped` | ⏭ | Gray | Fix was skipped (e.g., target file not found, or fingerprint mismatch) |
| `rolled_back` | ↩ | Red | Fix was applied but caused compilation failure and was rolled back |

#### Configuration

Dashboard generation is controlled by the `dashboard` config field in `.loop-config.json` (default: `true`). When disabled, the orchestrator skips dashboard generation after state updates — useful in CI environments where HTML rendering is not needed.

## Orchestration Workflow

> **Dashboard generation**: After every state update in the workflow below, the orchestrator generates `target/loop-dashboard.html` from `templates/loop-dashboard.html` (if `dashboard: true` in config). See [Section 7: Loop Visualization Dashboard](#7-loop-visualization-dashboard) for details.

### Step 1: Initialize Loop

1. **Receive user request**: Natural language description of the JavaFX project requirements
2. **Check for requirements intent**: If the user request contains requirements keywords ("gather requirements", "user stories", "acceptance criteria", "stakeholder analysis", "change impact") OR `.loop-config.json` has `"requirements_phase": true`, activate the requirements phase (Step 0)
3. **Check for architect intent**: If the user request contains architecture keywords ("design the architecture", "select technology stack", "generate UML", "create ADR") OR `.loop-config.json` has `"architect_phase": true`, activate the architect phase (Step 0.5)
4. **Check for design intent**: If the user request contains design keywords ("design and generate", "create a prototype", "design a theme") OR `.loop-config.json` has `"design_phase": true`, activate the design phase (Step 1.5)
5. **Create loop state**: Initialize `.loop-state.json` with `status: "requirements"` (if requirements phase) or `status: "architecting"` (if architect phase) or `status: "designing"` (if design phase only) or `status: "generating"` (if none), `current_round: 0`, `max_rounds: <value from .loop-config.json max_rounds field, default 3>`

### Step 0: Requirements Phase (Optional)

If requirements intent is detected:

1. **Route to requirements**: Trigger `javafx-requirements` with the user request. Requirements produces stakeholder analysis, user stories with acceptance criteria, non-functional requirements, and a requirement traceability matrix seed in the `requirements/` directory
2. **Wait for requirements result**: Requirements produces a `requirements-handoff.json` file with stakeholder list, user stories, NFRs, RTM seed, and developer instructions (req ID convention, annotation format, test naming convention)
3. **Check requirements conclusion**:
   - **Pass** → Proceed to Step 0.5 (Architect, if enabled) or Step 1.5 (Design, if enabled) or Step 2 (Generation). Architect will consume `requirements-handoff.json` in its Step 1; Developer will consume it in its Step 1
   - **Pass with warnings** → Proceed to next phase. Warnings are recorded in `requirements_result` but do not block generation
   - **Fail** → Pause loop with `status: "paused"`, report requirements failure to user. Do NOT proceed to architect or generation — requirements are prerequisites for informed architecture and code generation
4. **Update state**: Record `requirements_result` in `.loop-state.json` with triggered flag, scope, stakeholder count, user story count, NFR count, acceptance criteria count, change impact reports count, and handoff file path

> **Requirements phase is optional**: When the user requests direct code generation without requirements intent ("create a JavaFX app"), this step is skipped entirely. Requirements can also run in standalone mode (no loop) when the user wants to review and iterate on requirements before committing to architecture and code generation. Requirements runs once — it is not part of the fix-verify cycle.

### Step 0.5: Architect Phase (Optional)

If architect intent is detected:

1. **Route to architect**: Trigger `javafx-architect` with the user request. Architect produces technology selection, UML diagrams (PlantUML), Architecture Decision Records (ADR), and prototype validation in the `architecture/` directory
2. **Wait for architect result**: Architect produces an `architecture-handoff.json` file with the system design specs, UML artifact paths, ADR file paths, and developer instructions
3. **Check architect conclusion**:
   - **Pass** → Proceed to Step 1.5 (Design, if enabled) or Step 2 (Generation), set `status: "designing"` or `status: "generating"`. Developer will consume `architecture-handoff.json` in its Step 4
   - **Pass with warnings** → Proceed to next phase. Warnings are recorded in `architect_result` but do not block generation
   - **Fail** → Pause loop with `status: "paused"`, report architecture failure to user. Do NOT proceed to design or generation — architecture specs are prerequisites for code generation
4. **Update state**: Record `architect_result` in `.loop-state.json` with triggered flag, scope, architecture pattern, module count, UML diagram count, ADR count, database schema flag (tables count), threat model flag (threats count, security ADRs count), and handoff file path

> **Architect phase is optional**: When the user requests direct code generation without architecture intent ("create a JavaFX app"), this step is skipped entirely. Architect can also run in standalone mode (no loop) when the user wants to review the architecture before committing to code generation. Architect runs once — it is not part of the fix-verify cycle.

### Step 1.5: Design Phase (Optional)

If design intent is detected:

1. **Route to designer**: Trigger `javafx-designer` with the user request. Designer produces FXML prototypes, CSS themes, interaction flow diagrams, and icon configurations in the `design/` directory
2. **Wait for designer result**: Designer produces a `design-handoff.json` file with the list of design artifacts
3. **Check designer conclusion**:
   - **Pass** → Proceed to Step 2 (Generation), set `status: "generating"`. Developer will consume `design-handoff.json` in its Step 4
   - **Pass with warnings** → Proceed to Step 2 (Generation). Warnings are recorded in `design_result` but do not block generation
   - **Fail** → Pause loop with `status: "paused"`, report design failure to user. Do NOT proceed to generation — design artifacts are prerequisites for code generation
4. **Update state**: Record `design_result` in `.loop-state.json` with triggered flag, dimensions, screens designed, artifacts list, and handoff file path

> **Design phase is optional**: When the user requests direct code generation without design intent ("create a JavaFX app"), this step is skipped entirely and the loop starts at Step 2 (Generation). Designer can also run in standalone mode (no loop) when the user wants to review the design before committing to code generation.

### Step 2: Generation Phase

1. **Route to developer**: Trigger `javafx-developer` with the user request. Developer checks for `design/design-handoff.json` — if present, uses designer's FXML/CSS/icon artifacts instead of built-in templates
2. **Update state**: Set `status: "generating"`

### Step 3: Post-Generation Parallel Routing

After `javafx-developer` completes code generation:
1. **Update state**: Set `status: "reviewing_and_verifying"`, `current_round: 1`
2. **Trigger both skills in parallel**: Route to `javafx-code-reviewer` (Full Review mode) AND `javafx-runner` (Full Verification mode) simultaneously
3. **Wait for both results**: Both skills run independently — the reviewer produces a review report with Fix Handoff entries; the runner produces a verification report with Fix Handoff entries. Neither blocks the other

> **Parallel safety**: The reviewer and runner write to **separate fields** in `.loop-state.json` (`reviewer_result` and `runner_result` respectively). They do not mutate shared state and require no mutual exclusion. See [Loop State Parallel Write Safety](#parallel-write-safety) below.

### Step 4: Post-Parallel-Execution Decision (Combined Gate)

After **both** `javafx-code-reviewer` and `javafx-runner` complete:
1. **Read both conclusions**: reviewer `Pass`/`Conditional Pass`/`Fail` AND runner `Pass`/`Conditional Pass`/`Fail`
2. **Update state**: Record both results in `rounds[current_round].reviewer_result` (with `conclusion`, `critical`, `major`, `minor`, `pass_rate`) and `rounds[current_round].runner_result` (with `conclusion`, `critical`, `major`, `pass_rate`)
3. **Merge Fix Handoffs**: Collect Fix Handoff entries from both reports into a single deduplicated list (see [Fix Handoff Merge & Dedup](#fix-handoff-merge--dedup) below)
4. **Combined Gate evaluation**:
   - Both Pass → Check `deep_testing` config:
     - `true` (default) → Set `status: "testing"`, route to `javafx-tester` (Step 4.5: Test Gate)
     - `false` → Check `docgen` config (Step 4.6: Documentation Gate)
   - Either Fail/Conditional → Route to developer for Fix Consumption with the merged Fix Handoff list

<a id="fix-handoff-merge--dedup"></a>
#### Fix Handoff Merge & Dedup

When reviewer and runner run in parallel, both may produce Fix Handoffs targeting the same file and overlapping line ranges. The orchestrator merges them before passing to the developer:

1. **Collect**: Gather all Fix Handoff entries from both reports into a single list
2. **Deduplicate**: For entries targeting the **same `target_file`** with **overlapping `target_lines`** ranges:
   - Keep the entry with the **higher severity** (Critical > Major > Minor > Info)
   - If both have the same severity, keep the **lower `fix_priority`** number (higher priority)
   - Record the discarded entry's source (`reviewer` or `runner`) in the kept entry's `dedup_merged_from` field for traceability
3. **Re-sort**: Sort the deduplicated list by `fix_priority` ascending (1 = highest) across both sources
4. **Pass to developer**: The merged list is handed to `javafx-developer`'s Fix Consumption Protocol as a single batch

### Step 4.5: Test Gate (Optional — Deep Testing)

After the Combined Gate passes (reviewer AND runner both Pass), if `deep_testing` is enabled:

1. **Update state**: Set `status: "testing"`
2. **Route to tester**: Trigger `javafx-tester` with the project. The tester dispatches all 4 dimensions (performance, security, accessibility, visual regression) **in parallel** as independent tracks, each writing to its own isolated state field (`tester_perf_result`, `tester_sec_result`, `tester_a11y_result`, `tester_vr_result`). If `visual_regression` is disabled in config, only 3 dimensions run
3. **Wait for all tracks**: The tester waits for all parallel tracks to complete (or timeout), then aggregates their results into the unified `tester_result` field using the aggregation gate (see `javafx-tester/SKILL.md` → Step 7)
4. **Test Gate evaluation** (reads the aggregated `tester_result`):
   - **Pass** → Proceed to Step 4.6 (Documentation Gate)
   - **Fail** → Merge tester Fix Handoffs with any existing reviewer/runner Fix Handoffs (same merge logic as Step 3), route to developer (Fix Cycle, Step 4)
5. **Tester skip condition**: If runner failed to produce a runnable project (compile/startup failure), tester is skipped — the Combined Gate would have already routed to Fix Cycle
6. **Parallel fallback**: If `.loop-config.json` has `"tester_parallel": false`, the tester executes the dimensions sequentially (field isolation rules still apply — only execution order changes)

> **Test Gate is optional**: Controlled by `deep_testing` config (default: `true`). When `false`, this step is skipped entirely and the Combined Gate is the final quality gate.

### Step 4.55: Refactoring Phase (Optional)

After the Test Gate passes (or Combined Gate if deep testing disabled), if refactoring is enabled:

1. **Check refactor trigger**: If `refactor_phase: true` in config OR the user request contains refactoring keywords ("refactor", "reduce technical debt", "eliminate code smells"), activate the refactoring phase
2. **Update state**: Set `status: "refactoring"`, set `next_action: "refactoring"`
3. **Route to refactorer**: Trigger `javafx-refactorer` with the project. Refactorer analyzes existing code for smells, generates refactoring recommendations, and produces `refactor-handoff.json`
4. **Wait for refactorer result**: Refactorer produces smell catalog, refactoring plan, tech debt inventory, and verification plan
5. **Check refactorer conclusion**:
   - **Pass** → Proceed to apply refactoring (Step 5.5 below)
   - **Pass with warnings** → Proceed to apply refactoring. Warnings are recorded but do not block
   - **Fail** → Skip refactoring, proceed to Step 4.6 (DocGen). Log refactoring failure as warning
6. **Apply refactoring** (Step 5.5): Route to `javafx-developer` in Fix Consumption mode to apply the refactoring actions from `refactor-handoff.json`. Developer uses the 4-level location matching hierarchy (fingerprint → anchor → content → AST signature)
7. **Verify behavior equivalence**: After refactoring is applied, route to `javafx-code-reviewer` with Dimension 9 (Refactoring Verification) activated. Reviewer checks method signature preservation, call site integrity, field access integrity, import graph acyclicity, and test result preservation
8. **Check verification result**:
   - **Pass** → Proceed to Step 4.6 (DocGen). Refactoring is complete
   - **Fail** → Developer rolls back refactoring changes (using the pre-fix backup mechanism from T2.4). Proceed to Step 4.6 without refactoring. Log behavior equivalence failure

> **Refactoring phase is optional**: Controlled by `refactor_phase` config (default: `false`). When `false`, this step is skipped entirely. Refactoring can also run as a standalone maintenance cycle on a delivered project — in this case, the user triggers refactorer directly without the full loop.

### Step 4.6: Documentation Gate (Optional — DocGen)

After all quality gates pass (Combined Gate + optional Test Gate + optional Refactoring), if `docgen` is enabled:

1. **Update state**: Set `status: "docgen"`, set `next_action: "docgen"`
2. **Route to docgen**: Trigger `javafx-docgen` with the project (generates API reference, user manual, architecture doc, changelog, README; optionally Javadoc HTML)
3. **Wait for docgen result**: DocGen produces a documentation report (Markdown + JSON)
4. **Documentation Gate evaluation** (configurable — blocking or non-blocking):

   | DocGen Conclusion | Non-Blocking (default) | Blocking (`doc_gate_mode: "blocking"`) |
   |-------------------|------------------------|----------------------------------------|
   | Pass | Delivered | Delivered |
   | Pass with warnings | Delivered | Delivered |
   | Fail | Delivered (docs skipped, failure logged) | **Blocked** — `next_action: "fix_documentation"`, stays in `"passed"` state |

   - **Non-blocking mode** (default): Documentation failures do not block delivery. If DocGen fails, the project is still delivered (code quality is already verified by upstream gates). Failure is logged in `docgen_result`.
   - **Blocking mode** (`doc_gate_mode: "blocking"`): Documentation failures block delivery. If DocGen fails, `gate_blocked` is set to `true`, `next_action` is `"fix_documentation"`, and the orchestrator does not proceed to the deployer phase. The user must fix documentation issues and re-run DocGen, or set `"docgen": false` to bypass the gate.

5. **Update state**: Set `status: "delivered"` (non-blocking mode or blocking pass) or keep `"passed"` with `next_action: "fix_documentation"` (blocking fail). Archive state, record `docgen_result` in `.loop-state.json`

> **Configurable gate**: The documentation gate mode is controlled by `doc_gate_mode` in `.loop-config.json`. The default is `"non-blocking"` (backward-compatible — never blocks delivery). Projects that require documentation completeness before delivery must explicitly set `"doc_gate_mode": "blocking"`.

> **DocGen is optional**: Controlled by `docgen` config (default: `true`). When `false`, this step is skipped entirely (gate not evaluated) and the project is delivered immediately after the Test Gate (or Combined Gate if deep testing is also disabled).

### Step 4.7: Deployment Gate (Optional — Deploy)

After DocGen completes (or is skipped), if `deploy_phase` is enabled or the user explicitly requests deployment:

1. **Update state**: Set `status: "deploying"`, set `next_action: "deploying"`
2. **Route to deployer**: Trigger `javafx-deployer` with the project. Deployer reads `pom.xml` and packaging configuration to generate CI/CD pipelines, release management, code signing, auto-update, and runtime monitoring
3. **Wait for deployer result**: Deployer produces a deployment report (Markdown + JSON) and `deploy-handoff.json`
4. **Deployment Gate evaluation** (never blocks shipping — project is already delivered):
   - **Pass** → Deployment configurations generated successfully, proceed to shipping
   - **Pass with warnings** → Configurations generated with gaps (e.g., missing signing certificate), warnings recorded in `deploy_result`, proceed to shipping
   - **Fail** → Deployment configuration generation failed, project is still shipped (code quality and docs are already verified), failure logged in `deploy_result`
5. **Update state**: Set `status: "shipped"`, archive state, record `deploy_result` in `.loop-state.json`

> **Deployment Gate never blocks shipping**: Unlike quality gates, the Deployment Gate does not block shipping. The project has already passed all quality gates and documentation generation — Deployer only adds deployment infrastructure. If Deployer fails, the project is shipped without deployment configs and the failure is logged for future improvement.

> **Deployer generates config only**: Deployer does NOT execute `mvn package` or `jpackage`. It only generates configuration files (YAML, shell scripts, Java classes). Build execution is delegated to the CI/CD pipeline.

> **Deploy phase is optional**: Controlled by `deploy_phase` config (default: `false`). When `false`, this step is skipped and the project is shipped immediately after DocGen. Can also be triggered by user request ("set up CI/CD", "deploy my app") regardless of config.

### Step 5: Fix Cycle

After `javafx-developer` completes Fix Consumption:
1. **Update state**: Set `status: "fixing"`, record fix summary including `fixes_applied`, `fixes_skipped`, `fixes_rolled_back` counts, and the `fix_handoffs` array with per-handoff `status` markers (`applied` / `skipped` / `rolled_back`) for dashboard visualization
2. **Check rollback events**: If `rollback_events` contains entries for the current round:
   - Rollback means the fixes caused compilation failure and were reverted
   - Do **not** proceed to incremental review/verify — the project is in its pre-fix state, so re-review/re-verify would produce the same results as the previous round
   - Instead, **pause the loop** with `status: "paused"` and `next_action: "manual_intervention"`, include the rollback event details and compile errors in the pause report. Recommend the user review the fix handoff entries and apply fixes individually to isolate the problematic change
   - If `rollback_events` has `rollback_verification: "failed"`, this is a critical error — the project may be in an inconsistent state, recommend immediate manual inspection
3. **Increment round**: If this is a new round (no rollback), `current_round++`
4. **Check max rounds**: If `current_round > max_rounds` → pause, report to user
5. **Check convergence**: Compare issue count with previous round
   - 2 consecutive non-converging rounds → pause, report to user
6. **Route to incremental review AND verification in parallel**: Trigger reviewer in Incremental Review mode AND runner in Targeted Verification mode simultaneously

### Step 6: Incremental Cycle

After incremental review and verification (both complete):
1. **Merge Fix Handoffs**: Deduplicate Fix Handoff entries from both incremental reports (same merge logic as Step 3)
2. **Evaluate Combined Gate**: Both reviewer and runner Pass?
   - Yes → Proceed to Step 4.5 (Test Gate, if `deep_testing` enabled) → Step 4.6 (Documentation Gate, if `docgen` enabled) → **Delivered**
   - No → Go to Step 5 (Fix Cycle) for next round

### Step 6.5: Serialization Triggers (Authoritative)

The following table defines all state serialization events in the loop. This is the **authoritative definition** — individual skills reference this table instead of maintaining their own copies.

| Trigger | Writer | Action |
|---------|--------|--------|
| Loop initialization (Step 1) | Orchestrator | Create `.loop-state.json` with `status`, `current_round: 0`, `max_rounds`, `project` |
| Requirements phase completes (Step 0) | Orchestrator | Write `requirements_result` with scope, stakeholder count, user story count, NFR count, handoff file |
| Architect phase completes (Step 0.5) | Orchestrator | Write `architect_result` with scope, pattern, module count, UML count, ADR count, database schema flag, threat model flag, handoff file |
| Design phase completes (Step 1.5) | Orchestrator | Write `design_result` with scope, screen count, handoff file |
| Generation completes (Step 2) | Orchestrator | Set `status: "reviewing_and_verifying"`, `current_round: 1` |
| Reviewer completes (parallel) | Reviewer | Write **only** `rounds[current_round].reviewer_result` (field isolation — do not modify `runner_result`) |
| Runner completes (parallel) | Runner | Write **only** `rounds[current_round].runner_result` (field isolation — do not modify `reviewer_result`) |
| Both reviewer + runner complete | Orchestrator | Write `combined_gate` and `next_action` (atomic write) |
| Tester track completes (parallel, Step 4.5) | Tester | Each track writes **only** its own field: `tester_perf_result` / `tester_sec_result` / `tester_a11y_result` / `tester_vr_result` (field isolation — do not modify another track's field) |
| All tester tracks complete (Step 4.5) | Tester/Orchestrator | Compute aggregated `tester_result` from four track fields (atomic write). Set `next_action` based on Test Gate |
| Refactoring completes (Step 4.55) | Orchestrator | Write `refactor_result` with smells, actions, debt metrics |
| Pre-fix backup created (developer Step 1.5) | Developer | No state write — backup manifest written to `.fix-backup/{timestamp}/manifest.json` |
| Fixes applied (developer Step 5.5) | Developer | Update fix summary, `fixes_applied`, `fixes_skipped`, `fixes_rolled_back`, `fix_handoffs[]` with per-handoff status |
| Post-fix compile fails, rollback triggered | Developer | Append `rollback_event` to `rollback_events[]` with timestamp, backup dir, files restored, compile errors, verification status |
| Rollback verification fails | Developer | Append `rollback_event` with `rollback_verification: "failed"`, set `status: "paused"` |
| DocGen completes (Step 4.6) | Orchestrator | Write `docgen_result` with coverage metrics, warnings |
| Deploy completes (Step 4.7) | Orchestrator | Write `deploy_result` with artifacts, secrets, conclusion |
| Loop passes all quality gates | Orchestrator | Write final state with `status: "passed"`, archive to `.loop-state.archive.json`. Clean up `.fix-backup/` |
| Loop reaches max rounds / non-converging | Orchestrator | Write `status: "paused"`, include resume instructions. Preserve `.fix-backup/` |
| User manually requests pause | Orchestrator | Write `status: "paused"`. Preserve `.fix-backup/` |
| Dashboard generation | Orchestrator | After every state write above, regenerate `target/loop-dashboard.html` (if `dashboard: true`) |

> **Parallel write safety**: Reviewer and runner write to **different fields** (`reviewer_result` vs `runner_result`) — they never conflict. The orchestrator waits for both to complete before writing `combined_gate` and `next_action` in a single atomic write.

### Step 7: State Recovery (Cross-Session)

If the orchestrator detects `.loop-state.json` on startup:
1. **Validate**: Check `project` matches and `updated_at` is within 7 days
2. **Restore**: Load `current_round`, `convergence_trend`, `last_fix_handoff`
3. **Resume**: Continue from `next_action`
4. **Stale handling**: If > 7 days or project hash mismatch → start fresh

## Skill Routing Rules

### Trigger Detection

| User Intent | Routed Skill | Orchestrator Action |
|-------------|-------------|---------------------|
| "Design the architecture and generate a JavaFX app" | javafx-architect → javafx-developer | Initialize loop with architect phase, then generate |
| "Select technology stack / plan module structure" | javafx-architect | Standalone architecture design (no loop) |
| "Generate UML diagrams / class diagram / sequence diagram" | javafx-architect | Standalone UML generation (no loop) |
| "Create ADR / architecture decision records" | javafx-architect | Standalone ADR management (no loop) |
| "Refactor the code / reduce technical debt / eliminate code smells" | javafx-refactorer → javafx-developer → javafx-code-reviewer | Refactoring phase: analyze → apply → verify behavior equivalence |
| "Refactor and then verify" | javafx-refactorer → javafx-code-reviewer | Standalone refactoring + behavior equivalence verification |
| "Design and generate a JavaFX app that..." | javafx-designer → javafx-developer | Initialize loop with design phase, then generate |
| "Design the architecture, create UI design, and generate code" | javafx-architect → javafx-designer → javafx-developer | Full pipeline: architect → design → generate |
| "Design a UI prototype / mockup / wireframe" | javafx-designer | Standalone design (no loop) |
| "Design a theme / color scheme / dark mode" | javafx-designer | Standalone theme design (no loop) |
| "Plan the UI layout / screen flow / navigation" | javafx-designer | Standalone flow design (no loop) |
| "Create a JavaFX app that..." | javafx-developer | Initialize new loop, Step 1 (no design phase) |
| "Review my JavaFX code" | javafx-code-reviewer | Standalone review (no loop) |
| "Verify my JavaFX project" | javafx-runner | Standalone verification (no loop) |
| "Performance test / benchmark my app" | javafx-tester | Standalone performance testing (no loop) |
| "Security test / vulnerability scan" | javafx-tester | Standalone security testing (no loop) |
| "Accessibility test / a11y check" | javafx-tester | Standalone accessibility testing (no loop) |
| "Deep test / comprehensive test" | javafx-tester | Standalone full testing (all 4 dimensions) |
| "Generate docs / API docs / user manual / README / changelog" | javafx-docgen | Standalone documentation generation (no loop) |
| "Document my JavaFX project" | javafx-docgen | Standalone documentation generation (no loop) |
| "Prepare for release / generate delivery docs" | javafx-docgen | Standalone documentation generation (no loop) |
| "Set up CI/CD / pipeline / deploy" | javafx-deployer | Standalone deployment configuration (no loop) |
| "Sign my application / code signing / notarize" | javafx-deployer | Standalone signing configuration (no loop) |
| "Set up auto-update / automatic updates" | javafx-deployer | Standalone auto-update configuration (no loop) |
| "Set up monitoring / crash reporting / logging" | javafx-deployer | Standalone monitoring configuration (no loop) |
| "Fix the issues in the report" | javafx-developer (Fix Consumption) | Resume loop from fix handoff |
| Mixed intent (generate + review + verify) | All three, review & verify in parallel | Full closed-loop cycle (Step 3 triggers reviewer ∥ runner) |
| Mixed intent (generate + review + verify + deep test) | All four | Full closed-loop with deep testing (Step 3 triggers reviewer ∥ runner → Step 4 Combined Gate → Step 4.5 Test Gate if configured) |
| Mixed intent (generate + review + verify + deep test + docs) | All five | Full closed-loop with deep testing and documentation (Step 3 → Step 4 → Step 4.5 → Step 4.6 → Delivered) |
| Mixed intent (generate + review + verify + docs, no deep test) | All four (skip tester) | Full closed-loop with documentation, no deep testing (Step 3 → Step 4 → Step 4.6 → Delivered) |
| Mixed intent (design + generate + review + verify + deep test + docs) | All six | Full pipeline with design phase (Step 1.5 Design → Step 2 Generate → Step 3 → Step 4 → Step 4.5 → Step 4.6 → Delivered) |
| Mixed intent (architect + design + generate + review + verify + deep test + docs) | All seven | Full pipeline with architect and design phases (Step 0.5 Architect → Step 1.5 Design → Step 2 Generate → Step 3 → Step 4 → Step 4.5 → Step 4.6 → Delivered) |

### Standalone vs. Orchestrated Mode

- **Standalone**: User explicitly requests a single skill (e.g., "review my code"). The orchestrator does not manage a loop; the skill runs independently and produces its report.
- **Orchestrated**: User requests generation or a mixed intent. The orchestrator manages the full loop, routing between skills until the Combined Quality Gate passes or the loop terminates.

## External Integration API

### For CI/CD Pipelines

CI/CD systems can interact with the orchestrator via `.loop-state.json`:

1. **Read state**: `cat .loop-state.json` to get current loop status
2. **Check delivery status**: `status == "delivered"` indicates the project passed all quality gates and documentation was generated (or skipped). `status == "passed"` indicates quality gates passed but DocGen has not yet run
3. **Check documentation status**: `docgen_result.conclusion` indicates whether documentation was generated successfully
4. **Resume loop**: If `status == "paused"`, the CI can trigger a fix cycle by invoking `javafx-developer` with the fix handoff report
5. **Dashboard artifact**: `target/loop-dashboard.html` is auto-generated after each state update — CI can publish it as a build artifact for visual loop monitoring

### For IDE Plugins

IDE plugins can consume the JSON report format (see report templates' JSON Output Format sections) to:
- Display review issues inline with code
- Show coverage metrics in the editor
- Trigger fix consumption from the IDE
- Monitor loop progress via `.loop-state.json`
- Display documentation coverage metrics from `docgen_result`
- Open `target/loop-dashboard.html` in a built-in browser for real-time loop visualization

## Loop Configuration (`.loop-config.json`)

The orchestrator and skills read optional configuration from `.loop-config.json` in the project root. If the file does not exist, all defaults apply. This allows per-project customization of loop behavior without modifying skill definitions.

### Configuration Schema

```json
{
  "output_format": "both",
  "max_rounds": 3,
  "clean_compile": false,
  "coverage_threshold": 0.60,
  "parallel_execution": true,
  "deep_testing": true,
  "tester_parallel": true,
  "visual_regression": true,
  "vr_update_baselines": false,
  "vr_diff_threshold": 0.02,
  "docgen": true,
  "design_phase": false,
  "deploy_phase": false,
  "dashboard": true,
  "architect_phase": false,
  "refactor_phase": false
}
```

### Field Reference

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `output_format` | string | `"both"` | Report output format: `"both"` (Markdown + JSON), `"json"` (JSON only), `"markdown"` (Markdown only). Controls reviewer, runner, docgen, and deployer report output |
| `max_rounds` | number | `3` | Maximum fix-verify loop rounds before pausing. Override for complex projects that need more iterations |
| `clean_compile` | boolean | `false` | If `true`, runner executes `mvn clean compile` (full rebuild). If `false` (default), runner uses `mvn compile` (incremental compilation) |
| `coverage_threshold` | number | `0.60` | JaCoCo line coverage threshold (0.0-1.0). Projects with higher coverage requirements can override |
| `parallel_execution` | boolean | `true` | If `true` (default), reviewer and runner execute in parallel. If `false`, executes serially (reviewer first, then runner) for debugging purposes |
| `deep_testing` | boolean | `true` | If `true` (default), `javafx-tester` is triggered after the Combined Gate (reviewer + runner) passes. If `false`, the Test Gate is skipped and the Combined Gate is the final quality gate. Useful for projects where deep testing is not yet required |
| `tester_parallel` | boolean | `true` | If `true` (default), the tester's dimensions execute in parallel as independent tracks with isolated state fields. If `false`, executes sequentially (field isolation rules still apply — only execution order changes). Useful for resource-constrained CI environments |
| `visual_regression` | boolean | `true` | If `true` (default), visual regression testing (Track D) is included in the tester's parallel tracks. If `false`, Track D is skipped entirely. Requires TestFX + Monocle dependencies in `pom.xml`; if absent, Track D is auto-skipped with a note regardless of this setting |
| `vr_update_baselines` | boolean | `false` | If `true`, the tester captures new screenshots and overwrites existing baselines without comparison. Used when intentional UI changes require baseline refresh. If `false` (default), normal comparison mode is used. Also settable via `-Dupdate.baselines=true` system property |
| `vr_diff_threshold` | number | `0.02` | Pixel diff ratio threshold for visual regression (0.02 = 2%). Below = Pass, above = regression. Lower values are stricter (more sensitive to changes), higher values are more permissive |
| `docgen` | boolean | `true` | If `true` (default), `javafx-docgen` is triggered after all quality gates pass (Combined Gate + optional Test Gate). If `false`, documentation generation is skipped and the project is delivered immediately |
| `doc_gate_mode` | string | `"non-blocking"` | Documentation gate mode: `"non-blocking"` (default — failures do not block delivery) or `"blocking"` (failures block delivery, `next_action` becomes `"fix_documentation"`) |
| `doc_javadoc_html` | boolean | `false` | If `true`, `javafx-docgen` also generates standard Javadoc HTML output (`docs/api-reference-html/`) using `mvn javadoc:javadoc`, in addition to the Markdown API reference |
| `design_phase` | boolean | `false` | If `true`, `javafx-designer` runs before `javafx-developer` to produce FXML prototypes, CSS themes, and icon configs. If `false` (default), developer uses its own built-in templates. Can also be triggered by user request ("design and generate") regardless of this setting |
| `architect_phase` | boolean | `false` | If `true`, `javafx-architect` runs before `javafx-designer` and `javafx-developer` to produce technology selection, UML diagrams (PlantUML), Architecture Decision Records (ADR), and prototype validation. If `false` (default), architecture design is skipped. Can also be triggered by user request ("design the architecture and generate", "select technology stack"). Architect produces `architecture-handoff.json` consumed by developer Step 4. Runs once — not part of the fix-verify cycle |
| `refactor_phase` | boolean | `false` | If `true`, `javafx-refactorer` runs after the Test Gate passes to detect code smells, generate refactoring recommendations, and manage technical debt. If `false` (default), refactoring is skipped. Can also be triggered by user request ("refactor the code", "reduce technical debt"). Refactorer produces `refactor-handoff.json` consumed by developer in Fix Consumption mode. After refactoring is applied, reviewer verifies behavior equivalence (Dimension 9). Runs once — not part of the fix-verify cycle |
| `deploy_phase` | boolean | `false` | If `true`, `javafx-deployer` runs after DocGen to generate CI/CD pipelines, signing configs, auto-update, and monitoring. If `false` (default), deployment is skipped. Can also be triggered by user request ("set up CI/CD", "deploy my app"). Deployer generates config only — never executes builds |
| `dashboard` | boolean | `true` | If `true` (default), orchestrator generates `target/loop-dashboard.html` after every state update — a self-contained HTML dashboard with ECharts charts, Fix Handoff table, and auto-refresh. If `false`, dashboard generation is skipped (useful in CI environments). The dashboard template is at `templates/loop-dashboard.html` |

### CI/CD Integration with Configuration

CI/CD pipelines can leverage the configuration file for automated quality gates:

```bash
# Check if the project passed the Combined Quality Gate
jq .status .loop-state.json

# Extract conclusion from JSON report for quality gate decision
jq .summary.conclusion review-report.json
jq .summary.conclusion verification-report.json

# Check coverage against threshold
jq '.jacoco_report.overall_line_coverage >= .loop-config.json.coverage_threshold' verification-report.json

# Check if any rollback events occurred (indicates fix instability)
jq '.rollback_events | length > 0' .loop-state.json

# Extract rollback details for CI failure reporting
jq '.rollback_events[-1] | {round, compile_errors, files_restored}' .loop-state.json

# Check if documentation was generated (post-delivery check)
jq .docgen_result.conclusion .loop-state.json

# Check documentation coverage metrics
jq .docgen_result.coverage .loop-state.json

# Verify full delivery: quality gates passed AND documentation generated
jq '.status == "delivered" and .docgen_result.conclusion != "Fail (docs skipped)"' .loop-state.json

# Check if dashboard was generated (artifacts check)
ls -la target/loop-dashboard.html

# Extract dashboard data for external monitoring (convergence trend)
jq '.convergence_trend' .loop-state.json

# Get latest round Fix Handoff summary for CI reporting
jq '.rounds[-1] | {round, fixes_applied, fixes_skipped, fixes_rolled_back, combined_gate}' .loop-state.json

# Check rollback events for CI failure alerting
jq '.rollback_events | length > 0' .loop-state.json
```

## Reference Documents

The orchestrator references the following documents from the ten skills:

- `../javafx-architect/SKILL.md` — Architecture phase artifacts (technology selection, UML diagrams, ADR, prototype validation)
- `../javafx-designer/SKILL.md` — Design phase artifacts (FXML prototypes, CSS themes, icon configs)
- `../javafx-developer/SKILL.md` — Generation and Fix Consumption Protocol
- `../javafx-code-reviewer/SKILL.md` — Review dimensions and Fix Handoff generation
- `../javafx-runner/SKILL.md` — Verification dimensions and Runtime Findings Feedback
- `../javafx-tester/SKILL.md` — Deep testing dimensions (performance, security, accessibility) and Test Gate
- `../javafx-refactorer/SKILL.md` — Refactoring phase artifacts (code smell catalog, refactoring plan, tech debt inventory, behavior equivalence verification)
- `../javafx-docgen/SKILL.md` — Documentation generation dimensions and Documentation Gate
- `../javafx-deployer/SKILL.md` — Deployment configuration (CI/CD, signing, auto-update, monitoring) and Deployment Gate
- `../javafx-developer/references/ci-cd-pipeline.md` — CI/CD integration with loop orchestration
- `../javafx-runner/references/test-coverage-gate.md` — Coverage threshold rules
- `templates/loop-dashboard.html` — Loop visualization dashboard template (ECharts-powered, auto-refresh, `{{LOOP_STATE_JSON}}` placeholder)

## Relationship to Individual Skills

The orchestrator does **not replace** the Loop Orchestration Protocol sections in the ten skills' SKILL.md files. Instead:

1. The ten skills retain their protocol sections for standalone operation
2. When orchestrated, the orchestrator's protocol takes precedence
3. The ten skills reference the orchestrator: "When operating within an orchestrated loop, see `javafx-orchestrator/SKILL.md` for the authoritative protocol"

This ensures backward compatibility — each skill can still operate independently without the orchestrator.

## EVALUATE.md

See `EVALUATE.md` for evaluation test cases that quantify orchestration quality.
