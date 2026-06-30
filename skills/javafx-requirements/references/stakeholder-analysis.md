# Stakeholder Analysis Reference

> Techniques for identifying stakeholders, analyzing their goals, and mapping their influence on a JavaFX desktop application project.

## Why Stakeholder Analysis Matters

Requirements do not emerge from vacuum — they originate from people who have a stake in the application's outcome. Skipping stakeholder analysis leads to:
- **Missing requirements**: Stakeholders not consulted → their needs never captured
- **Conflicting priorities**: Stakeholders with different goals → unresolved conflicts surface late
- **Unrealistic NFRs**: Business sponsors set performance targets without understanding technical trade-offs
- **Adoption resistance**: End users excluded from requirements → the app doesn't fit their workflow

## Stakeholder Identification

### Who Counts as a Stakeholder?

A stakeholder is anyone who is affected by, or can affect, the JavaFX application. For desktop apps, common stakeholder categories:

| Category | Examples | Typical Goals |
|----------|----------|---------------|
| End Users | Operators, data entry clerks, analysts | Efficient workflow, ease of use, reliability |
| Business Sponsors | Product owner, department head, CEO | ROI, time-to-market, compliance |
| Developers | In-house team, contractors | Maintainability, clear requirements, testability |
| Operators/IT | System administrators, DevOps | Easy deployment, monitoring, low maintenance |
| Compliance/Security | Data protection officer, auditor | Data security, audit trails, regulatory compliance |
| Integrators | Teams building APIs the app consumes | Stable interfaces, clear contracts |

### Identification Technique: The "Who Cares?" Sweep

Walk through these questions to surface stakeholders:

1. **Who uses the app daily?** → End users
2. **Who pays for it or sponsors it?** → Business sponsors
3. **Who builds and maintains it?** → Developers, operators
4. **Who integrates with it?** → External system owners
5. **Who audits or regulates it?** → Compliance, security
6. **Who is affected if it fails?** → Downstream users, business processes

## Goal Analysis

### Structuring Stakeholder Goals

For each stakeholder, document goals in a structured format:

```markdown
### SH-001: System Administrator

**Role**: Manages user accounts and monitors system activity
**Influence**: High — can approve or veto features
**Priority**: High — primary end user

**Goals**:
1. Create and manage user accounts efficiently
2. Monitor system activity and detect anomalies
3. Generate compliance reports on demand

**Pain Points**:
- Current process requires manual CSV import for bulk user creation
- No visibility into user login activity
- Report generation takes too long

**Success Criteria**:
- Bulk user creation via CSV import completes in < 5 seconds for 100 users
- Activity dashboard shows real-time login/logout events
- Compliance reports generate in < 10 seconds
```

### Goal-to-Requirement Mapping

Each stakeholder goal should map to at least one user story or NFR. This mapping is the foundation of the Requirement Traceability Matrix:

| Stakeholder Goal | Maps To | Type |
|-----------------|---------|------|
| "Create user accounts efficiently" | FR-001 (Create user), FR-002 (Bulk import) | Functional |
| "Bulk import completes in < 5s" | NFR-PERF-002 | Non-Functional |
| "Detect anomalies in login activity" | FR-005 (Activity monitoring) | Functional |

## Influence Mapping

### Power-Interest Grid

Plot stakeholders on a 2x2 grid to prioritize engagement:

```
High Power ┌────────────────────┬────────────────────────┐
           │  Keep Satisfied    │  Manage Closely        │
           │  (e.g., Auditor)   │  (e.g., Sponsor,       │
           │                    │   Lead User)           │
           ├────────────────────┼────────────────────────┤
           │  Monitor           │  Keep Informed         │
           │  (e.g., Integrator)│  (e.g., End User,      │
           │                    │   Developer)           │
Low Power  └────────────────────┴────────────────────────┘
           Low Interest              High Interest
```

- **Manage Closely**: High power + high interest — these stakeholders' goals drive the core requirements. Engage them in every requirement review
- **Keep Satisfied**: High power + low interest — meet their needs (e.g., compliance) but don't overwhelm them with details
- **Keep Informed**: Low power + high interest — end users and developers. Keep them updated, their feedback improves requirements
- **Monitor**: Low power + low interest — monitor for changes, minimal active engagement

### Prioritization from Influence

Stakeholder influence directly affects requirement priority:
- Requirements from "Manage Closely" stakeholders → Must (MoSCoW)
- Requirements from "Keep Satisfied" stakeholders → Should (MoSCoW)
- Requirements from "Keep Informed" stakeholders → Could (MoSCoW)
- Requirements from "Monitor" stakeholders → Won't (this iteration)

## Common Pitfalls

1. **Single stakeholder assumption**: Assuming the sponsor is the only stakeholder → missing end-user needs
2. **Vague goals**: "The app should be user-friendly" is not a goal — probe for specific, measurable outcomes
3. **No priority differentiation**: Treating all stakeholders as equal priority → scope creep and no clear MVP
4. **Static analysis**: Stakeholder influence changes over time — revisit the analysis when project context shifts
