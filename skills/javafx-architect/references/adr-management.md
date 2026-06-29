# Architecture Decision Records (ADR) Management Reference

This reference supports the architect's Step 4 (ADR Management). It defines the Architecture Decision Record template (Michael Nygard format), the status lifecycle, numbering and naming conventions, the superseding process, the index file format, and JavaFX-specific examples. Every significant architecture or technology decision captured during system design must be recorded as an ADR under `architecture/adr/`.

---

## 1. ADR Template (Michael Nygard Format)

Each ADR documents a single decision. Use the following template — every section is mandatory.

```markdown
# ADR-{NNN}: {Human Readable Title}

## Status
{Proposed | Accepted | Deprecated | Superseded by ADR-XXX} ({YYYY-MM-DD})

## Context
{Why is this decision needed? Forces, constraints, the problem.}

## Decision
{The specific choice we are making.}

## Consequences
**Positive:**
- ...

**Negative:**
- ...

## Alternatives Considered
1. **{Alternative A}**: {why rejected}
2. **{Alternative B}**: {why rejected}
```

### 1.1 Section-by-Section Guidance

| Section | Write THIS | Do NOT write THIS |
|---------|-----------|--------------------|
| **Title** | A short noun-phrase naming the decision (`Use MVVM Architecture Pattern`) | A question (`Which pattern?`) or a vague topic (`Architecture`) |
| **Status** | One status word plus the date; `Superseded by ADR-XXX` when replaced | Free-text status, edit history, or reviewer names |
| **Context** | The problem forces, constraints, and why a decision is needed now | The decision itself, or a generic background dump unrelated to the choice |
| **Decision** | The specific, actionable choice in one or two sentences | Vague intent ("we will consider..."), options, or rationale (that belongs in Context) |
| **Consequences** | Both positive and negative impacts: effort, risk, what gets easier/harder | Only positives, or marketing-style benefits with no trade-offs |
| **Alternatives Considered** | At least two real alternatives, each with the reason for rejection | "We had no other options" or strawman alternatives set up to fail |

### 1.2 Complete Example: ADR-001

> File: `architecture/adr/ADR-001-use-mvvm-architecture.md`

```markdown
# ADR-001: Use MVVM Architecture Pattern

## Status
Accepted (2026-06-30)

## Context
The application is a 12-screen business-form JavaFX desktop app with heavy
two-way data binding (form fields, master-detail tables, live totals).
Controllers in a classic MVC setup would accumulate UI state and validation,
making screens hard to unit-test and prone to thread-safety bugs when
background Tasks update the UI. The team has prior MVVM experience and
needs ViewModels that run without the JavaFX runtime for fast JUnit tests.

## Decision
We will use the MVVM (Model-View-ViewModel) pattern with a thin Service
Layer. Controllers stay minimal (bind FXML nodes to ViewModel properties,
forward user events); ViewModels expose `StringProperty`/`BooleanProperty`/
`ObservableList` and hold all UI state and validation; Services hold
business rules. Controllers are built by the DI container via
`FXMLLoader.setControllerFactory`.

## Consequences
**Positive:**
- ViewModels are pure Java and unit-testable without a JavaFX runtime.
- Bidirectional binding removes manual `setText`/`getText` plumbing.
- UI and business logic can be developed and reviewed in parallel.

**Negative:**
- More classes per screen (Controller + ViewModel + FXML).
- Binding chains can leak listeners; ViewModels need explicit cleanup.
- Steeper learning curve for developers new to Properties binding.

## Alternatives Considered
1. **MVC**: Simpler for small apps, but UI state and validation pool in
   controllers across 12 screens, hurting testability and reuse.
2. **MVP**: Highly testable, but the explicit view-control contract is
   verbose for data-binding-heavy forms and fights JavaFX Properties.
3. **Event-Bus only**: Decouples producers/consumers, but data-flow tracing
   for form validation becomes opaque and hard to debug.
```

---

## 2. ADR Status Values

| Status | Meaning | When to set it |
|--------|---------|----------------|
| **Proposed** | The decision is being considered; not yet finalized. | A draft is circulated for review; no code follows it yet. |
| **Accepted** | The decision is finalized and in effect. | Review is complete; code and design must follow it. |
| **Deprecated** | The decision is no longer relevant, but was not directly replaced. | The feature/context disappeared (e.g., a module was cut). |
| **Superseded** | The decision has been replaced by a newer ADR. | A new ADR explicitly overturns this one. |

### 2.1 Status Transition Diagram

```
            review complete
   Proposed ─────────────────► Accepted
       │                           │
       │ abandoned                  │ overturned by
       ▼                           ▼ new ADR
   Deprecated ◄───────────── Superseded (by ADR-XXX)
```

Valid transitions: `Proposed → Accepted`, `Proposed → Deprecated`, `Accepted → Superseded`, `Accepted → Deprecated`. Status never moves backward (an Accepted ADR is not "un-accepted" — it is superseded). `Superseded` and `Deprecated` are terminal states.

---

## 3. ADR Numbering and Naming

- **Sequential numbering**: ADR-001, ADR-002, ADR-003... Numbers are assigned in creation order and **never reused**, even if an ADR is deprecated or superseded. Gaps from deleted drafts are left as-is.
- **File naming convention**: `ADR-{NNN}-{kebab-case-title}.md` — zero-padded three-digit number, hyphen, lower-case kebab-case title, `.md` extension.
- **Title format**: `ADR-{NNN}: {Human Readable Title}` inside the file (title case, readable, no kebab).

| Artifact | Format | Example |
|----------|--------|---------|
| Number | `ADR-{NNN}` | `ADR-001` |
| File name | `ADR-{NNN}-{kebab-case-title}.md` | `ADR-001-use-mvvm-architecture.md` |
| In-file title | `ADR-{NNN}: {Title}` | `ADR-001: Use MVVM Architecture Pattern` |

---

## 4. ADR Versioning and Superseding

When a decision is overturned, do **not** edit the old ADR's content. Instead:

1. Create the new ADR with the next free number.
2. In the new ADR's **Context** section, state `Supersedes ADR-YYY` and explain why the old decision no longer holds.
3. In the old ADR's **Status** line, change it to `Superseded by ADR-XXX` (this is the *only* edit allowed to an accepted ADR).
4. Add the new ADR to the index README and update the old one's status there.

> **Immutability rule**: Never modify the content (Context/Decision/Consequences/Alternatives) of an accepted ADR — only its status. The historical record must stay intact so reviewers can see what was decided and why.

### 4.1 Superseding Example: ADR-002 superseded by ADR-005

Old ADR — file `architecture/adr/ADR-002-use-sqlite-database.md`:

```markdown
# ADR-002: Use SQLite Database

## Status
Superseded by ADR-005 (2026-07-20)

## Context
The app is single-user, local-first, with < 1 GB of data; SQLite gives
zero-config single-file storage ideal for desktop deployment.

## Decision
We will use SQLite as the embedded local database accessed over JDBC.

## Consequences
**Positive:** zero install, single file, fast startup.
**Negative:** single-writer concurrency, no multi-user access.

## Alternatives Considered
1. **H2 embedded**: in-process SQL, but less production tooling.
2. **PostgreSQL**: overkill for a single-user local app.
```

New ADR — file `architecture/adr/ADR-005-use-postgresql-database.md`:

```markdown
# ADR-005: Use PostgreSQL Database

## Status
Accepted (2026-07-20)

## Context
Supersedes ADR-002. Requirements changed: the app now supports multi-user
shared access and cloud sync across workstations. SQLite's single-writer
model blocks concurrent edits, so ADR-002's local-only assumption no
longer holds. We need a client/server database with row-level locking.

## Decision
We will use PostgreSQL (client/server) accessed over JDBC.

## Consequences
**Positive:** multi-user concurrency, mature sync target, backups.
**Negative:** requires a server/URL, network dependency, heavier ops.

## Alternatives Considered
1. **Keep SQLite + sync layer** (ADR-002): conflict resolution is complex.
2. **H2 server mode**: lighter, but lacks production-grade concurrency tooling.
```

---

## 5. ADR Index File (README.md)

Maintain `architecture/adr/README.md` as the single index of all ADRs. List every ADR — including deprecated and superseded ones — sorted ascending by number. Update the index whenever an ADR is added or its status changes.

```markdown
# Architecture Decision Records

| Number | Title | Status | Date | File |
|--------|-------|--------|------|------|
| ADR-001 | Use MVVM Architecture Pattern | Accepted | 2026-06-30 | ADR-001-use-mvvm-architecture.md |
| ADR-002 | Use SQLite Database | Superseded by ADR-005 | 2026-06-30 | ADR-002-use-sqlite-database.md |
| ADR-003 | Manual Dependency Injection | Accepted | 2026-07-02 | ADR-003-manual-dependency-injection.md |
| ADR-004 | Use JavaFX 21 LTS | Accepted | 2026-07-05 | ADR-004-use-javafx-21-lts.md |
| ADR-005 | Use PostgreSQL Database | Accepted | 2026-07-20 | ADR-005-use-postgresql-database.md |
```

---

## 6. JavaFX-Specific ADR Examples

Brief seeds for common JavaFX decisions — each becomes a full ADR following the template in Section 1.

### 6.1 ADR: Choose JavaFX Version (21 LTS vs 25 LTS)

**Context**: The app targets JDK 21 and must ship on a long-term-support runtime to minimize upgrade churn for end users. JavaFX 21 LTS is mature and widely packaged by jpackage; JavaFX 25 LTS adds newer controls and virtualized-text improvements but is newer and less battle-tested in packaging tooling. We must pick the baseline that balances stability against feature needs.

### 6.2 ADR: Choose Database (SQLite vs PostgreSQL)

**Context**: The app is a local-first desktop tool today, but a roadmap item adds multi-device sync within 12 months. SQLite gives zero-config single-file storage ideal for desktop, while PostgreSQL offers client/server concurrency and a clear sync target. The decision hinges on whether to optimize for current single-user simplicity or future multi-user reach.

### 6.3 ADR: Choose Dependency Injection (Manual vs Guice)

**Context**: The app has ~15 services and controllers wired by hand today, which is transparent but starting to feel repetitive as features grow. Manual constructor injection keeps startup instant and the classpath tiny, while Guice offers declarative bindings and `setControllerFactory` integration at the cost of a dependency and reflective `opens`. We must decide before the controller count grows further and rewiring becomes painful.

---

## 7. ADR Quality Checklist

Before marking an ADR `Accepted`, confirm every item:

- [ ] Context explains **why** this decision is needed (not just **what** it is).
- [ ] Decision is specific and actionable (a reader could implement it without guessing).
- [ ] Consequences include **both** positive and negative impacts.
- [ ] At least **2 alternatives** are considered, each with a concrete rejection reason.
- [ ] Status is one of: `Proposed`, `Accepted`, `Deprecated`, `Superseded`.
- [ ] File follows the naming convention `ADR-{NNN}-{kebab-case-title}.md`.
- [ ] Number is sequential and not reused from a prior (deleted/superseded) ADR.
- [ ] ADR is referenced in the index `architecture/adr/README.md`.
- [ ] If superseding, old ADR status reads `Superseded by ADR-XXX` and new ADR Context notes `Supersedes ADR-YYY`.
- [ ] No content of a previously accepted ADR was edited — only its status line changed.
