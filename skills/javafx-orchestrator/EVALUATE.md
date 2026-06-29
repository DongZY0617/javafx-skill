# EVALUATE.md — javafx-orchestrator

> Evaluation test cases for quantifying orchestration quality of the `javafx-orchestrator` skill.

## Evaluation Dimensions

| Dimension | Weight | Description |
|-----------|--------|-------------|
| Loop initialization | 15% | Correct loop state creation and skill routing |
| Gate evaluation | 25% | Combined Quality Gate logic correctness |
| Round management | 20% | Round counting, max rounds enforcement, convergence detection |
| State persistence | 20% | `.loop-state.json` serialization and recovery |
| Standalone fallback | 10% | Skills operate correctly without orchestrator |
| External integration | 10% | CI/CD and IDE plugin API correctness |

## Test Cases

### TC-01: Full Closed-Loop Pass (Round 1)

**Input**: User requests a simple CRUD JavaFX app. Generated code passes both review and verification on the first round.

**Expected**:
- Loop state initialized with `status: "generating"`, `current_round: 0`
- After generation: `status: "reviewing"`, `current_round: 1`
- Reviewer returns Pass
- Runner returns Pass
- Combined Gate: Pass
- Final state: `status: "passed"`, state archived
- Total rounds: 1

### TC-02: Fix Cycle (Round 2 Pass)

**Input**: Generated code has 2 Critical issues in review, 1 Major in verification. After fix consumption, Round 2 passes both gates.

**Expected**:
- Round 1: reviewer Fail (2 Critical), runner Fail (1 Major)
- Developer Fix Consumption triggered
- Round 2: incremental review Pass, incremental verification Pass
- Combined Gate: Pass
- Convergence trend: [3, 0]
- Total rounds: 2

### TC-03: Max Rounds Reached

**Input**: Code has persistent issues that don't converge after 3 rounds.

**Expected**:
- Round 1, 2, 3: all Fail
- Convergence trend shows non-convergence (e.g., [5, 4, 4])
- Loop pauses with `status: "paused"`
- User receives state report with all unresolved issues
- `.loop-state.json` preserved for manual recovery

### TC-04: Non-Convergence Detection

**Input**: Issue count does not decrease for 2 consecutive rounds.

**Expected**:
- Round 1: 4 issues → Round 2: 4 issues (no convergence)
- Loop pauses after Round 2 (2 consecutive non-converging rounds)
- `status: "paused"`, convergence_trend: [4, 4]

### TC-05: Cross-Session Recovery

**Input**: Loop interrupted at Round 2 (fixing phase). Session restarts.

**Expected**:
- Orchestrator detects `.loop-state.json`
- Validates project name and freshness (< 7 days)
- Restores `current_round: 2`, `convergence_trend`, `last_fix_handoff`
- Resumes from `next_action: "incremental_review_and_verify"`
- Loop continues normally

### TC-06: Stale State Handling

**Input**: `.loop-state.json` exists but is 10 days old.

**Expected**:
- Orchestrator detects stale state
- Archives old state to `.loop-state.archive.json`
- Starts fresh loop from Round 1

### TC-07: Standalone Skill Operation

**Input**: User requests "review my code" without generation.

**Expected**:
- Orchestrator routes to reviewer in Standalone mode
- No loop state created
- Reviewer produces report independently
- No Combined Quality Gate evaluation

### TC-08: Mixed Intent Handling

**Input**: User requests "create a JavaFX app and make sure it passes review and verification".

**Expected**:
- Orchestrator detects mixed intent (generate + review + verify)
- Initializes loop
- Routes through all three skills in sequence
- Manages the full closed-loop cycle

### TC-09: Compile Short-Circuit

**Input**: Code fails compile verification in Round 2.

**Expected**:
- Runner skips runtime, test, and packaging verification
- Records "subsequent verification skipped due to compile failure"
- Combined Gate: Fail
- Routes to developer for Fix Consumption

### TC-10: External API — CI/CD Integration

**Input**: CI pipeline reads `.loop-state.json` after a loop.

**Expected**:
- `status: "passed"` → CI pipeline succeeds
- `status: "paused"` → CI pipeline fails with state report
- JSON reports (`review-report.json`, `verification-report.json`) available for parsing
