# User Stories & Acceptance Criteria Reference

> Patterns for writing effective user stories, applying INVEST validation, and defining acceptance criteria with Given-When-Then for JavaFX desktop applications.

## User Story Format

### Standard Template

```
As a <role>
I want to <action>
So that <benefit>
```

The three parts serve distinct purposes:
- **Role**: Identifies WHO needs the feature — ties back to a stakeholder (SH-xxx)
- **Action**: Describes WHAT the feature does — the observable behavior
- **Benefit**: Explains WHY it matters — the value delivered

### JavaFX-Specific Considerations

Desktop applications have interaction patterns distinct from web or mobile:

| Pattern | Story Consideration |
|---------|---------------------|
| Keyboard navigation | Stories should specify keyboard shortcuts for power-user workflows |
| Drag-and-drop | Specify source, target, and feedback for DnD operations |
| Context menus | Right-click actions should be explicit user stories |
| Multi-window | Stories should specify parent-child window relationships |
| Offline operation | Desktop apps may need offline capability — specify in NFRs |
| File system access | Stories involving file I/O should specify permissions and error handling |

## INVEST Validation

Every user story must pass the INVEST check. Stories failing one or more criteria should be split, rewritten, or restructured.

### I — Independent

The story can be implemented in any order relative to other stories. Dependencies create scheduling constraints and integration risk.

**Bad (dependent)**:
```
US-003: Edit user details (depends on US-001: Create user)
```

**Good (independent)**:
```
US-001: Create user account
US-002: View user list
US-003: Edit user details (can be implemented independently if a seed user exists)
```

If dependencies are unavoidable, document them explicitly and sequence the stories.

### N — Negotiable

The story describes the "what" not the "how." Implementation details are negotiable between the team and stakeholders.

**Bad (non-negotiable)**:
```
US-004: Store users in SQLite database with a users table containing id, name, email columns
```

**Good (negotiable)**:
```
US-004: Persist user accounts so they survive application restarts
```

### V — Valuable

The story delivers clear value to a stakeholder. If you cannot identify the value, the story is likely too technical or unnecessary.

**Bad (no clear value)**:
```
US-005: Implement a generic DAO layer
```

**Good (valuable)**:
```
US-005: Save and load user data so administrators don't re-enter data after restart
```

### E — Estimable

The team can estimate the effort required. If a story is too vague or technically uncertain to estimate, it needs more detail or a spike.

**Bad (not estimable)**:
```
US-006: Make the app feel modern
```

**Good (estimable)**:
```
US-006: Apply a dark theme with consistent typography and spacing to all screens
```

### S — Small

The story can be completed within a single iteration. Large stories ("epics") should be decomposed into smaller, implementable stories.

**Bad (too large)**:
```
US-007: Complete user management system
```

**Good (decomposed)**:
```
US-007a: Create user account
US-007b: List users in a table
US-007c: Edit user details
US-007d: Delete user with confirmation
US-007e: Search users by name
```

### T — Testable

The story has clear acceptance criteria that can be verified. If you cannot define testable criteria, the story is too ambiguous.

**Bad (not testable)**:
```
US-008: The app should be responsive
```

**Good (testable)**:
```
US-008: The main window resizes gracefully from 800x600 to 1920x1080 without clipping
```

## Acceptance Criteria

### Given-When-Then Format

Acceptance criteria use the BDD (Behavior-Driven Development) format:

```
Given <precondition>
When <action>
Then <expected outcome>
And <additional outcome>
```

Each user story should have at least one acceptance criterion. Complex stories may have multiple.

### JavaFX-Specific Acceptance Criteria Patterns

#### UI Interaction Pattern

```markdown
#### AC-001.1: Button click triggers action
- **Given** the user is on the main screen
- **When** they click the "Save" button
- **Then** the form data is validated
- **And** if valid, the data is saved to the database
- **And** a success notification appears in the status bar
```

#### Validation Pattern

```markdown
#### AC-002.1: Invalid input shows error
- **Given** the user is filling in the email field
- **When** they enter "not-an-email" and tab out
- **Then** the field border turns red
- **And** an error tooltip displays: "Please enter a valid email address"
- **And** the Save button is disabled
```

#### Navigation Pattern

```markdown
#### AC-003.1: Navigation between screens
- **Given** the user is on the User List screen
- **When** they double-click a user row
- **Then** the User Detail screen opens
- **And** the selected user's data is populated in the form
- **And** the back button is enabled
```

#### Table Interaction Pattern

```markdown
#### AC-004.1: Table sorting
- **Given** the user is viewing a table with 50 rows
- **When** they click the "Name" column header
- **Then** the table is sorted alphabetically by name (ascending)
- **And** a sort indicator arrow appears on the column header
- **When** they click the header again
- **Then** the sort order reverses to descending
```

### Acceptance Criteria Anti-Patterns

1. **Implementation details**: "Save button calls `userRepository.save()`" — this is how, not what
2. **Vague outcomes**: "The app works correctly" — not verifiable
3. **Missing preconditions**: "Then the user is saved" — what state was the app in?
4. **Multiple behaviors per criterion**: Split into separate criteria if testing more than one thing

## Epic Decomposition

Epics are large user stories that span multiple iterations. Decompose them using these strategies:

### By Workflow Step

```
Epic: User Management
├── US-001: Create user
├── US-002: List users
├── US-003: Edit user
├── US-004: Delete user
└── US-005: Search users
```

### By User Role

```
Epic: Reporting
├── US-006: Generate summary report (analyst)
├── US-007: Export report to PDF (analyst)
├── US-008: Schedule recurring report (admin)
└── US-009: View report audit log (auditor)
```

### By Complexity Layer

```
Epic: Data Import
├── US-010: Import CSV file (basic — parse and validate)
├── US-011: Preview import data before committing (intermediate)
└── US-012: Rollback failed import (advanced — transaction)
```

## Requirement ID Assignment

Each user story receives two IDs:
- **US-xxx**: The story identifier (stable, never reused)
- **FR-xxx**: The functional requirement identifier (mapped 1:1 to US-xxx, used in `@req` annotations)

The mapping is recorded in the traceability matrix. When a story is split, new US/FR IDs are assigned; the old ID is marked `Superseded` but never deleted — this preserves traceability history.
