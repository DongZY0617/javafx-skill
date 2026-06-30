# EVALUATE.md — javafx-requirements

> Evaluation test cases for quantifying requirements engineering quality of the `javafx-requirements` skill.

## Evaluation Dimensions

| Dimension | Weight | Description |
|-----------|--------|-------------|
| Stakeholder coverage | 20% | All relevant stakeholders identified with goals and influence |
| User story quality | 25% | Stories pass INVEST, have acceptance criteria with Given-When-Then |
| NFR quantification | 20% | Every NFR has measurable target and verification method |
| Traceability integrity | 20% | RTM seed is complete and consistent with user stories/NFRs |
| Change impact analysis | 15% | Impact reports correctly trace forward and backward |

## Test Cases

### TC-01: Full Requirements Engineering (Pass)

**Input**: User requests a user management JavaFX app with vague description: "I need an app to manage users with roles and activity logging."

**Expected**:
- Stakeholder analysis identifies ≥ 2 stakeholders (admin, auditor/compliance)
- ≥ 4 user stories with INVEST validation all passing
- Each story has ≥ 1 acceptance criterion in Given-When-Then format
- ≥ 3 NFRs quantified with measurable targets
- RTM seed maps every FR-xxx and NFR-xxx to a verification method
- `requirements-handoff.json` produced with valid structure
- Conclusion: Pass

### TC-02: Vague Requirements (Pass with Warnings)

**Input**: User says "Make a useful JavaFX tool" with no further detail.

**Expected**:
- Requirements skill asks clarifying questions about domain, users, and features
- After clarification, produces requirements with explicit assumptions documented
- Warnings section notes which requirements were inferred vs explicitly stated
- Conclusion: Pass with warnings

### TC-03: Missing Acceptance Criteria (Fail)

**Input**: User story US-001 is generated without acceptance criteria.

**Expected**:
- INVEST check fails on "Testable" criterion
- Skill flags the story as incomplete and requests AC before proceeding
- If user insists on proceeding, conclusion: Fail with reason "User stories without acceptance criteria"

### TC-04: Unquantified NFR (Fail)

**Input**: NFR generated as "The app should be fast" with no measurable target.

**Expected**:
- Skill rejects the NFR and requires quantification
- If user insists, conclusion: Fail with reason "NFRs must have measurable targets"

### TC-05: Change Impact Analysis — New Requirement (Low Severity)

**Input**: After architecture and code exist, user requests adding "Export to CSV" (new FR-005).

**Expected**:
- Change impact report CHANGE-001 generated
- Forward trace identifies: new ExportService class, UserController modification, new test
- Backward trace verifies FR-001 (existing) not broken by UserController modification
- Impact severity: Low (≤ 3 files, no architectural impact)
- RTM updated with FR-005 (status: Planned)

### TC-06: Change Impact Analysis — NFR Adjustment (Medium Severity)

**Input**: Performance NFR tightened from "startup ≤ 5s" to "startup ≤ 2s".

**Expected**:
- Change impact report generated
- Forward trace identifies slow code paths (lazy loading, startup sequence)
- Architecture impact: may require deferred initialization pattern
- Impact severity: Medium (cross-module, may affect architecture)
- RTM marks NFR-PERF-001 as Modified with new target

### TC-07: Change Impact Analysis — Requirement Removal (High Severity)

**Input**: User removes FR-003 "Activity logging" after code is implemented.

**Expected**:
- Change impact report generated
- Forward trace identifies files with `@req FR-003` that need deletion or re-annotation
- Backward trace checks if any other requirement depends on activity logging infrastructure
- Impact severity: High (> 10 files affected if logging is pervasive)
- RTM marks FR-003 as Removed (not deleted)

### TC-08: Traceability Integrity — RTM Consistency

**Input**: Requirements handoff has FR-001 in user stories but missing from RTM seed.

**Expected**:
- Skill detects RTM inconsistency during Step 4
- Warning generated: "FR-001 present in user stories but missing from traceability matrix"
- Skill auto-adds missing entries to RTM
- Conclusion: Pass with warnings

### TC-09: Handoff Consumption by Architect

**Input**: `requirements-handoff.json` is consumed by `javafx-architect` Step 1.

**Expected**:
- Architect reads stakeholders to understand who the system serves
- Architect reads user stories to identify key use cases for sequence diagrams
- Architect reads NFRs to constrain technology selection (e.g., performance NFR drives database choice)
- Architect references RTM requirement IDs in ADRs for traceability

**Verification standards**:
- [ ] Architect's Step 1 reads `stakeholders[]` from `requirements-handoff.json`
- [ ] Architect's Step 1 reads `user_stories[]` to identify key use cases for sequence diagrams
- [ ] Architect's Step 1 reads `non_functional_requirements[]` to constrain technology selection
- [ ] Architect references RTM requirement IDs (FR-xxx, NFR-xxx) in generated ADRs

### TC-10: Handoff Consumption by Developer

**Input**: `requirements-handoff.json` is consumed by `javafx-developer` Step 1.

**Expected**:
- Developer uses user stories as basis for `requirements.md` feature list instead of inferring from scratch
- Developer uses `req_id_convention` for `@req` annotations (FR-xxx, NFR-xxx)
- Developer uses `test_naming_convention` for test method names
- Generated `requirements.md` RTM is seeded from the handoff's `traceability_matrix`

**Verification standards**:
- [ ] Developer's Step 1 reads `user_stories[]` as basis for `requirements.md` feature list
- [ ] Developer uses `developer_instructions.req_id_convention` for `@req` annotations
- [ ] Developer uses `developer_instructions.test_naming_convention` for test method names
- [ ] Generated `requirements.md` RTM is seeded from `traceability_matrix` in the handoff

### TC-11: Handoff Consumption by Reviewer

**Input**: `requirements-handoff.json` RTM seed is consumed by `javafx-code-reviewer` Requirements Coverage dimension.

**Expected**:
- Reviewer uses RTM seed as the authoritative requirement list
- Reviewer checks that every FR-xxx and NFR-xxx in the handoff has `@req` annotations in code
- Reviewer checks that every `@req` annotation in code references a requirement ID in the handoff
- If handoff doesn't exist, reviewer falls back to `requirements.md` (existing behavior)

**Verification standards**:
- [ ] Reviewer uses RTM seed as the authoritative requirement list
- [ ] Reviewer checks that every FR-xxx and NFR-xxx in the handoff has `@req` annotations in code
- [ ] Reviewer checks that every `@req` annotation in code references a requirement ID in the handoff
- [ ] If handoff doesn't exist, reviewer falls back to `requirements.md` (existing behavior)

### TC-12: Standalone Mode (No Loop)

**Input**: User explicitly requests "analyze requirements only" without architecture or code generation.

**Expected**:
- Requirements skill runs independently
- Produces `requirements-handoff.json` and all artifact files
- Does NOT trigger architect or developer
- User can review requirements and later trigger architect with the handoff

### TC-13: Scope — User Stories Only (Boundary)

**Input**: User explicitly requests "I only need user stories for a JavaFX inventory app — skip NFRs and stakeholder analysis."

**Expected**:
- `scope: "user_stories_only"` in the report
- User stories are fully developed (INVEST-compliant, with acceptance criteria)
- NFR section is present but marked as "Not requested in this scope"
- Stakeholder analysis is minimal (inferred from user stories, not full stakeholder mapping)
- `change_impact_reports` is empty
- RTM seed contains only FR-xxx entries (no NFR-xxx)

**Pass criteria**:
- [ ] `scope` field is `"user_stories_only"`
- [ ] User stories pass INVEST (all 6 criteria)
- [ ] NFR section exists but is explicitly marked as out-of-scope
- [ ] RTM seed has FR-xxx entries only
- [ ] `conclusion` is `Pass`

### TC-16: Schema Version Rejection (Negative)

**Input**: A `requirements-handoff.json` with `"requirements_version": "2.0"` (the schema's `const` is `"1.0"`). User attempts to consume this handoff via `javafx-architect` or `javafx-developer`.

**Expected**:
- The consuming skill detects the version mismatch against the `const: "1.0"` constraint
- The handoff is rejected with a clear error message: "requirements_version '2.0' does not match expected '1.0'. Incompatible handoff — regenerate or upgrade."
- No processing occurs based on the mismatched handoff
- The consuming skill reports `conclusion: "Fail"` with the version mismatch cited

**Pass criteria**:
- [ ] Version mismatch is detected before any field-level processing
- [ ] Error message clearly states the expected version ("1.0") and the actual version ("2.0")
- [ ] No partial processing occurs — the handoff is rejected in full
- [ ] `conclusion` is `Fail` with version mismatch as the failure reason (scope reduction is intentional, not a failure)

### TC-14: Scope — NFR Only (Boundary)

**Input**: User explicitly requests "I already have user stories — I just need non-functional requirements quantified for a JavaFX trading dashboard."

**Expected**:
- `scope: "nfr_only"` in the report
- NFRs are fully quantified (each has measurable target + verification method)
- User stories section references existing stories (not re-generated)
- Stakeholder analysis is skipped
- RTM seed contains only NFR-xxx entries

**Pass criteria**:
- [ ] `scope` field is `"nfr_only"`
- [ ] All NFRs have `measurable_target` and `verification_method` fields populated
- [ ] At least 3 NFRs are quantified (performance, reliability, usability minimum)
- [ ] User stories are NOT re-generated (existing stories referenced)
- [ ] `conclusion` is `Pass`

### TC-15: Scope — Change Impact Only (Boundary)

**Input**: User explicitly requests "Analyze the impact of adding real-time notifications to our existing JavaFX chat app — we have existing requirements."

**Expected**:
- `scope: "change_impact_only"` in the report
- Change impact report traces forward (what existing components are affected) and backward (what drives the change)
- Impact rating (Low/Medium/High) is assigned with justification
- No new user stories or NFRs are generated (only impact analysis)
- Existing requirements are referenced, not re-authored

**Pass criteria**:
- [ ] `scope` field is `"change_impact_only"`
- [ ] `change_impact_reports` has at least 1 entry with `change_description`, `impact_rating`, `affected_components[]`
- [ ] Forward trace (change → affected components) and backward trace (change → driving requirement) are both present
- [ ] No new user stories or NFRs are generated
- [ ] `conclusion` is `Pass`
