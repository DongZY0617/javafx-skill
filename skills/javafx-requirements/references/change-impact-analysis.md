# Change Impact Analysis Reference

> Techniques for analyzing the impact of requirement changes on architecture, code, and tests, maintaining traceability chain integrity throughout the project lifecycle.

## When Change Impact Analysis Is Needed

Change impact analysis is triggered when a requirement is added, modified, or removed after the initial requirements have been established and architecture/code may already exist:

- **New requirement**: A stakeholder requests a feature not in the original scope
- **Modified requirement**: An existing requirement's description, target, or priority changes
- **Removed requirement**: A requirement is descoped or cancelled
- **NFR adjustment**: A performance, security, or compatibility target shifts (e.g., startup time tightened from 5s to 3s)

## Change Impact Analysis Workflow

### Step 1: Document the Change

Capture the change request in a structured format:

```markdown
## Change Request: CHANGE-001

**Requested by**: Business Sponsor (SH-002)
**Date**: 2026-07-01
**Change type**: New requirement
**Description**: Add "Export users to CSV" functionality
**Rationale**: Compliance team needs CSV exports for audit reporting
**Priority**: Must (compliance deadline 2026-07-15)
```

### Step 2: Trace Forward (Requirement → Code → Test)

Follow the traceability chain forward to identify all affected artifacts:

```
FR-005 (new) ──→ No existing code (new feature)
                ──→ New ExportService class needed
                ──→ UserController needs export button (FR-001 exists, add FR-005 annotation)
                ──→ New test: ExportServiceTest#testCsvFormat_FR_005()
```

**Forward tracing technique**: For each new/modified requirement ID, search:
1. `requirements-handoff.json` → which user stories and acceptance criteria are affected
2. `architecture-handoff.json` → which modules, classes, UML diagrams are affected
3. `requirements.md` RTM → which source files have `@req` annotations for affected requirements
4. Test files → which test methods reference the affected requirement IDs

### Step 3: Trace Backward (Changed Code → Affected Requirements)

When code changes are proposed, trace backward to ensure no other requirements are broken:

```
UserController.java modified ──→ @req FR-001, FR-005
                               ──→ FR-001 (Create user) still satisfied? Verify AC-001.1, AC-001.2
                               ──→ FR-005 (Export) is the new requirement
```

**Backward tracing technique**: For each file being modified:
1. Read all `@req` annotations in the file
2. For each `@req` ID, check if the modification affects its acceptance criteria
3. If an existing requirement's AC is affected, mark it `Modified` in the RTM
4. Verify modified requirements still pass their acceptance criteria after the change

### Step 4: Assess Impact Severity

Classify the change severity based on blast radius:

| Severity | Criteria | Example |
|----------|----------|---------|
| **Low** | Isolated change, ≤ 3 files, no architectural impact, no ADR affected | Add a new validation rule to a single controller |
| **Medium** | Cross-module change, 4-10 files, may affect architecture, no ADR invalidated | Add a new feature spanning controller + service + repository |
| **High** | Architectural impact, > 10 files, ADR may be invalidated, core data model changes | Change database from SQLite to PostgreSQL, affecting all data access |

### Step 5: Generate Impact Report

Write the impact analysis to `requirements/change-impact/CHANGE-{id}.md` using the template in the SKILL.md. The report must include:

1. **Change description**: What is changing and why
2. **Affected requirements**: Which FR/NFR IDs are added, modified, or removed
3. **Architecture impact**: Modules, classes, UML diagrams, ADRs affected
4. **Code impact**: Files to create, modify, or delete, with their `@req` annotations
5. **Test impact**: Test methods to add, modify, or delete
6. **Impact severity**: Low / Medium / High with justification
7. **RTM update**: New requirement IDs added, existing IDs marked `Modified`

### Step 6: Update Traceability Matrix

After the change impact analysis:
1. Add new requirement IDs to the RTM with status `Planned`
2. Mark modified requirements with status `Modified` (preserve original description in change history)
3. Mark removed requirements with status `Removed` (never delete the row — preserve history)
4. Update the `change_impact_reports` array in `requirements-handoff.json`

## Impact Analysis by Change Type

### New Requirement (Additive Change)

Forward trace: Identify where the new feature plugs in
- Which module will host the new code?
- Which existing files need new `@req` annotations?
- Which new test files are needed?

Backward trace: Minimal — the new requirement doesn't break existing ones
- Verify the new feature doesn't conflict with existing requirements
- Check if the new feature introduces NFR pressure (e.g., new heavy computation affects performance NFRs)

### Modified Requirement (Mutating Change)

Forward trace: Identify all code implementing the old requirement
- Find all files with `@req FR-xxx` for the modified requirement
- Assess whether existing code satisfies the modified AC or needs changes

Backward trace: Critical — modifications can break other requirements
- Check if the modification affects shared code paths
- Verify dependent requirements still have valid implementations

### Removed Requirement (Subtractive Change)

Forward trace: Identify code that implements the removed requirement
- Find all files with `@req FR-xxx` for the removed requirement
- Determine if the code can be safely deleted or if it serves other requirements too

Backward trace: Critical — removal can create orphan code
- Check if any `@req INFRA` files were actually supporting the removed requirement
- Verify no other requirement depends on the removed feature's side effects

### NFR Adjustment (Quality Change)

Forward trace: Identify code affected by the new quality target
- Tighter performance NFR → identify slow code paths that now violate the target
- Tighter security NFR → identify code that now fails the new security standard

Backward trace: Check if meeting the new NFR requires changes that affect functional requirements
- e.g., adding encryption may slow down data access, affecting performance NFRs
- Document NFR conflicts and resolve via ADR

## Maintaining Traceability Chain Integrity

The traceability chain is only valuable if it stays accurate. These rules ensure integrity:

1. **Never delete requirement IDs**: Removed requirements are marked `Removed`, not deleted — the history must be preserved
2. **Never reuse requirement IDs**: Once an ID is assigned, it is never reassigned to a different requirement
3. **Update RTM on every change**: Any requirement change triggers an RTM update in the same change impact report
4. **`@req` annotations must match RTM**: If code has `@req FR-005`, the RTM must list FR-005 — `javafx-code-reviewer` enforces this
5. **Change history is append-only**: The change history section of `requirements.md` grows over time — old entries are never edited, only appended to

## Integration with Reviewer

The `javafx-code-reviewer` Requirements Coverage dimension validates traceability integrity. When change impact analysis has been performed:
- The reviewer checks that all new requirement IDs in the change impact report have corresponding `@req` annotations in code
- The reviewer checks that modified requirements' new acceptance criteria are covered by tests
- The reviewer checks that removed requirements' code has been deleted or re-annotated to other requirements
- RTM consistency (Check Item 4) verifies the RTM matches actual code annotations
