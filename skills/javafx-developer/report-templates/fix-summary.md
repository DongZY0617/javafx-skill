# JavaFX Developer — Fix Summary Report

> This Markdown template corresponds to the `fix_summary` mode of `report-schema.json`.
> Use this template when the developer operates in Fix Consumption mode (Step 5.5),
> consuming Fix Handoffs from code-reviewer, runner, or tester.

## Generation Metadata

| Field | Value |
|-------|-------|
| Project | {{project}} |
| Developer Version | {{developer_version}} |
| Created At | {{created_at}} |
| Mode | Fix Consumption |
| Round | {{round}} |
| Sources Consumed | {{sources}} (e.g., reviewer, runner, tester) |

## Fix Summary

**Total Fixes Received**: {{total_fixes}}
**Applied**: {{applied_count}}
**Skipped**: {{skipped_count}}
**Rolled Back**: {{rolled_back_count}}
**Merged (dedup)**: {{merged_count}}

### Applied Fixes

| # | Source | Issue ID | Severity | Target File | Target Lines | Fix Type | Status |
|---|--------|----------|----------|-------------|--------------|----------|--------|
{{#each fixes}}
| {{@index}} | {{source}} | {{issue_id}} | {{severity}} | {{target_file}} | {{target_lines}} | {{fix_type}} | {{status}} |
{{/each}}

### Skipped Fixes

| # | Issue ID | Target File | Reason |
|---|----------|-------------|--------|
{{#each skipped_fixes}}
| {{@index}} | {{issue_id}} | {{target_file}} | {{skip_reason}} |
{{/each}}

### Rolled Back Fixes

| # | Issue ID | Target File | Rollback Reason |
|---|----------|-------------|-----------------|
{{#each rolled_back_fixes}}
| {{@index}} | {{issue_id}} | {{target_file}} | {{rollback_reason}} |
{{/each}}

## Location Matching Results

| Fix ID | Match Level | Matched |
|--------|-------------|---------|
{{#each location_results}}
| {{fix_id}} | {{match_level}} (1=fingerprint, 2=anchor, 3=content, 4=AST) | {{matched}} |
{{/each}}

## Cross-Impact Warnings

{{#if cross_impact_warnings}}
{{#each cross_impact_warnings}}
- **{{warning_type}}**: {{description}} (affected files: {{affected_files}})
{{/each}}
{{else}}
No cross-impact warnings detected.
{{/if}}

## Rollback Events

{{#if rollback_events}}
{{#each rollback_events}}
- **Timestamp**: {{timestamp}}
  - **Trigger**: {{trigger}} (e.g., compilation failure)
  - **Files Restored**: {{files_restored}}
  - **Backup Location**: {{backup_location}}
{{/each}}
{{else}}
No rollback events occurred during this fix round.
{{/if}}

## Compilation Verification

| Check | Result |
|-------|--------|
| Pre-fix compilation | {{pre_fix_compile}} |
| Post-fix compilation | {{post_fix_compile}} |
| Incremental compile used | {{incremental_compile}} |
| Compile duration (ms) | {{compile_duration_ms}} |

## Conclusion

**Overall**: {{conclusion}}

{{conclusion_detail}}

---

> **Note**: This fix summary is serialized to `developer-report.json` per `report-schema.json`.
> The orchestrator reads `fix_summary.fixes[].status` to determine loop continuation:
> - All `applied` + post-fix compilation passes → proceed to next gate
> - Any `rolled_back` → enter rollback recovery (see orchestrator state machine)
> - Any `skipped` with `skip_reason: "not_found"` → log warning, may indicate code drift
