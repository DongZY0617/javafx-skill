# Technical Debt Management Reference

This document is the authoritative framework for technical debt management in the `javafx-refactorer` skill. It defines how detected code smells (see `code-smells.md`) are converted into a quantified, prioritized, and repayable debt inventory. Output from this framework feeds the `tech_debt` object in `refactor-handoff.json`, consumed by `javafx-developer` during phased repayment.

---

## 1. Debt Identification Framework

Technical debt is the implied cost of future rework caused by choosing an expedient solution now over a better approach that would take longer. In JavaFX projects, debt accrues across five layers.

### 1.1 Debt Types

| Type | Definition | JavaFX Example |
|------|------------|----------------|
| Code debt | Low-quality implementation at the statement/method level | Duplicated validation blocks, magic numbers in handlers |
| Design debt | Poor class/package structure, missing abstractions | God Class controller mixing CRUD + validation + export |
| Architecture debt | Missing or violated layering, broken module boundaries | Controller package importing service package which imports controller back |
| Test debt | Missing, weak, or non-deterministic tests | Controller logic with zero tests because it requires FXML to instantiate |
| Documentation debt | Missing/stale docs, Javadoc, FXML comments, architecture records | No ADR explaining why SwingNode interop was retained |

### 1.2 Sources of Debt

Debt is introduced by concrete pressures, not laziness alone:

- **Rushed features** — deadlines force shortcuts (inline DB calls in handlers, skipped extraction)
- **Lack of testing** — untested code decays faster because regressions go unnoticed
- **Missing architecture** — no layering rules means logic drifts to wherever it compiles
- **Framework version upgrades** — JavaFX API changes leave legacy patterns behind
- **Third-party dependency drift** — libraries evolve; pinned versions accumulate CVEs and incompatibilities

### 1.3 Debt vs. Code Smell

A **code smell** is the symptom — a structural pattern in the code (e.g., a 120-line method). **Technical debt** is the quantified impact of that smell: how much it costs to maintain, how much it slows change, and how much effort is required to repay. Every debt item links back to a smell via `smell_id`; the smell is the evidence, the debt item is the business case for fixing it.

### 1.4 JavaFX-Specific Debt Sources

| Source | Description | Typical Smell |
|--------|-------------|---------------|
| JavaFX version lag | Project stuck on JavaFX 17 when 25 is LTS; misses API improvements, security fixes, performance work | Dead Code, outdated API usage |
| Swing interop debt | `SwingNode`/`JFXPanel` bridges retained long after migration was feasible; thread-bridging complexity | Tight coupling, blocking UI thread |
| CSS technical debt | Hardcoded color values scattered across stylesheets, no theme variable system | CSS Class Explosion |
| FXML structural debt | Monolithic FXML files (300+ lines, deep nesting) that cannot be reused or tested in isolation | FXML God File |
| Property misuse debt | `SimpleXxxProperty` in model/service classes instead of plain fields, or plain fields where binding is needed | Property Misuse |

---

## 2. Debt Inventory Schema

Every detected smell that warrants repayment is recorded as a debt inventory item. The schema is machine-readable and links directly to the smell catalog.

```yaml
debt_id: DEBT-001
smell_id: SMELL-001            # link to smell catalog in code-smells.md
type: code | design | architecture | test | documentation
severity: Critical | Major | Minor
location: file:lines + ast_node_signature
impact_score: 1-10             # business impact of NOT fixing (10 = highest)
effort_estimate: S | M | L     # S < 1hr, M 1-4hr, L > 4hr
roi_score: impact_score / effort_normalized   # higher = fix first
status: open | in_progress | resolved | wont_fix
```

### Example Debt Items

```yaml
# DEBT-001: God Class controller blocks all user-feature work
debt_id: DEBT-001
smell_id: SMELL-001
type: design
severity: Critical
location: "src/main/java/com/example/controller/UserController.java:1-850 | com.example.controller.UserController"
impact_score: 9
effort_estimate: L
roi_score: 1.13           # 9 / 8
status: open

# DEBT-002: Duplicated validation block across two controllers
debt_id: DEBT-002
smell_id: SMELL-004
type: code
severity: Major
location: "UserController.java:45-72, OrderController.java:38-65 | duplicated validate(User) block"
impact_score: 6
effort_estimate: S
roi_score: 6.0            # 6 / 1
status: open

# DEBT-003: Controller has no tests — regressions go undetected
debt_id: DEBT-003
smell_id: SMELL-013
type: test
severity: Major
location: "UserController.java (whole class) | 0% line coverage on saveUser/handleSave"
impact_score: 7
effort_estimate: M
roi_score: 2.33           # 7 / 3
status: open
```

---

## 3. Prioritization Matrix

### 3.1 Impact x Effort Quadrant

| | Low Effort (S) | High Effort (L) |
|---|---|---|
| **High Impact** | Quick Win — do first | Major Project — plan carefully |
| **Low Impact** | Fill-in — opportunistic | Reconsider — maybe skip |

### 3.2 ROI-Based Prioritization

ROI measures repayment value per unit of effort. Higher ROI means fix first.

- **Formula**: `ROI = ImpactScore / EffortNormalized`
- **Effort normalization**: `S = 1`, `M = 3`, `L = 8` (hours approximation)

| Priority | Level | Trigger |
|----------|-------|---------|
| P1 | Immediate | Critical severity OR ROI >= 4.0 |
| P2 | High | Major severity OR ROI 2.0–3.9 |
| P3 | Medium | Minor severity with ROI 1.0–1.9 |
| P4 | Low | Minor severity with ROI < 1.0 |

### 3.3 Worked Example

Five debt items, sorted by ROI to determine priority:

| Debt ID | Impact | Effort | Norm | ROI | Severity | Priority |
|---------|--------|--------|------|-----|----------|----------|
| DEBT-002 | 6 | S | 1 | 6.00 | Major | P1 (ROI >= 4) |
| DEBT-005 | 8 | M | 3 | 2.67 | Major | P2 |
| DEBT-003 | 7 | M | 3 | 2.33 | Major | P2 |
| DEBT-004 | 4 | S | 1 | 4.00 | Minor | P1 (ROI >= 4) |
| DEBT-001 | 9 | L | 8 | 1.13 | Critical | P1 (Critical) |

> DEBT-001 has a low ROI (1.13) but is escalated to P1 because Critical severity overrides ROI — blocking debt is repaid regardless of cost.

### 3.4 Severity Override Rule

ROI is the default ranking signal, but severity can override it:

- **Critical severity forces P1** regardless of ROI — debt that blocks features or causes runtime failures cannot be deferred based on cost alone.
- **A Minor item with high ROI (>= 4.0) is promoted to P1** — cheap wins that meaningfully reduce debt count should be seized immediately.
- When two items share the same priority, the higher ROI wins the tiebreak.

---

## 4. Repayment Planning

### 4.1 Phase-Based Strategy

| Phase | Scope | Timing |
|-------|-------|--------|
| Phase 1 (Immediate) | All P1 items | Before next feature work begins |
| Phase 2 (High) | All P2 items | Next sprint |
| Phase 3 (Medium) | All P3 items | When touching the relevant code |
| Phase 4 (Low) | All P4 items | Opportunistic, during code reviews |

### 4.2 Sprint Integration

Allocate a fixed percentage of sprint capacity to debt repayment so progress is continuous, not episodic:

- **20% of sprint capacity** reserved for debt repayment (e.g., in a 40-hour sprint, 8 hours go to debt)
- P1 items consume this capacity first; remaining capacity funds P2/P3 items
- If no P1 items remain, the 20% rolls forward into a "debt buffer" for future sprints

### 4.3 Boy Scout Rule

> "Always leave the code behind in a better state than you found it."

When a developer touches a file for any feature or bugfix, they must resolve at least one debt item in that file (preferably the highest-ROI open item). This prevents debt from growing even when no dedicated repayment sprint is scheduled.

### 4.4 Debt Ceiling

Define a maximum debt density threshold to prevent runaway accumulation:

- **Ceiling**: `< 5 debt items per KLoC` (1000 lines of code)
- **Action when exceeded**: Freeze new feature development until debt density is reduced below the ceiling
- **Measurement**: Recompute after every sprint; trend must be flat or decreasing

---

## 5. Debt Metrics and Reporting

### 5.1 Metric Definitions

| Metric | Definition |
|--------|------------|
| Total debt items | Count of all open + in-progress debt items |
| Debt by severity | Critical / Major / Minor counts |
| Debt by type | code / design / architecture / test / documentation counts |
| Estimated repayment effort | Sum of effort estimates converted to hours (S=1, M=3, L=8) |
| Debt density | Debt items per 1000 lines of code (KLoC) |
| Debt trend | Delta over time — increasing, stable, or decreasing |

### 5.2 Sample Debt Metrics Dashboard

| Metric | Current | Previous | Trend |
|--------|---------|----------|-------|
| Total debt items | 12 | 15 | Decreasing (-3) |
| Critical | 2 | 4 | Decreasing (-2) |
| Major | 5 | 6 | Decreasing (-1) |
| Minor | 5 | 5 | Stable |
| Code debt | 4 | 5 | Decreasing |
| Design debt | 3 | 3 | Stable |
| Architecture debt | 1 | 2 | Decreasing |
| Test debt | 3 | 4 | Decreasing |
| Documentation debt | 1 | 1 | Stable |
| Est. repayment effort | 40 hrs | 52 hrs | Decreasing |
| Debt density | 3.2 / KLoC | 4.1 / KLoC | Decreasing |
| Ceiling (5.0 / KLoC) | Below | Below | OK |

### 5.3 Reporting Cadence

Metrics are recomputed and the dashboard refreshed at three checkpoints:

- **Per sprint** — after the repayment capacity is consumed, recompute density and trend.
- **Per release** — full re-scan of the codebase; retire resolved items, add newly detected ones.
- **On demand** — when a developer triggers the Boy Scout Rule on a touched file, update that file's local debt status immediately.

---

## 6. Debt Management Workflow

The end-to-end process runs as a recurring cycle, not a one-time exercise:

1. **Identify debt** — Run smell detection (`code-smells.md`); classify each smell into a debt type (Section 1).
2. **Build inventory** — Create a debt item per smell using the Section 2 schema; link `smell_id` back to the catalog.
3. **Calculate metrics** — Compute total items, by-severity, by-type, effort sum, and density (Section 5).
4. **Prioritize** — Compute ROI for each item; assign P1–P4 using the Section 3 matrix; apply Critical-severity override.
5. **Create repayment plan** — Group items into Phases 1–4 (Section 4); respect refactoring dependency order from `refactoring-patterns.md`.
6. **Track progress** — Update `status` (open -> in_progress -> resolved) as items are repaid; recompute metrics after each sprint.
7. **Re-evaluate periodically** — Re-run detection each sprint or release; re-score impact as the codebase evolves; retire items that no longer apply (set `status: wont_fix` with a recorded reason).

> The workflow feeds `tech_debt` in `refactor-handoff.json`: the inventory becomes `tech_debt.items[]`, metrics populate `tech_debt` summary fields, and the repayment plan maps to `repayment_plan.phase_1_immediate` through `phase_4_low`.

---

## Reference Documents

- `code-smells.md` — Code smell catalog that this document converts into a quantified debt inventory; each `smell_id` links a debt item to its detected smell.
- `refactoring-patterns.md` — Refactoring patterns that repay each debt item; the repayment plan's dependency order follows the safe sequencing rules defined there.
