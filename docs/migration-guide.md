# Migration Guide

> JavaFX Skill Set — version upgrade and deprecated feature migration reference.
> Last updated: 2026-06-30

## 1. Overview

This document provides migration guidance for users upgrading between versions of the JavaFX Skill Set. It covers breaking changes, deprecated features, and step-by-step upgrade procedures.

## 2. Current Version: 1.0

This is the initial release of the JavaFX Skill Set. All 10 skills (9 operational + 1 orchestrator) are at version 1.0.

### Skills in v1.0

| Skill | Version | Status |
|-------|---------|--------|
| javafx-architect | 1.0 | ✅ Active |
| javafx-designer | 1.0 | ✅ Active |
| javafx-developer | 1.0 | ✅ Active |
| javafx-code-reviewer | 1.0 | ✅ Active |
| javafx-runner | 1.0 | ✅ Active |
| javafx-tester | 1.0 | ✅ Active |
| javafx-refactorer | 1.0 | ✅ Active |
| javafx-docgen | 1.0 | ✅ Active |
| javafx-deployer | 1.0 | ✅ Active |
| javafx-orchestrator | 1.0 | ✅ Active |

## 3. Deprecated Features

No features have been deprecated yet in v1.0. This section will be updated as features are deprecated in future versions.

### Deprecation Policy
- A feature is marked as **Deprecated** when a replacement is available
- Deprecated features remain functional for **2 minor versions** after deprecation
- After 2 minor versions, deprecated features are removed and marked as **Removed**
- Users are notified via the migration guide at least 1 version before removal

## 4. Breaking Changes

### v1.0 (Initial Release)
No breaking changes — this is the first release.

### Future Breaking Changes (Planned)
The following changes are being considered for future versions and may introduce breaking changes:

| Planned Change | Target Version | Impact | Migration Path |
|---------------|----------------|--------|----------------|
| Fix Handoff format v2 (additional fields) | 1.1 | Low — v1 fields remain compatible | Add new optional fields; v1 consumers ignore unknown fields |
| Loop State JSON v2 (restructured rounds) | 1.2 | Medium — `rounds[]` structure changes | Provide automatic v1→v2 migration script |
| Reviewer dimension reorganization | 1.2 | Low — dimensions merged/split | Map old dimension names to new names |

## 5. Upgrade Procedures

### 5.1 Upgrading from Pre-release to v1.0

If you were using a pre-release version of the JavaFX Skill Set:

1. **Backup your project**: Create a full backup of your project directory, including `.loop-state.json` and `.fix-backup/`
2. **Update all SKILL.md files**: Replace all skill files with v1.0 versions
3. **Delete old loop state**: Remove `.loop-state.json` — v1.0 introduces a new state format
4. **Clean fix backups**: Remove `.fix-backup/` directory — v1.0 backup format has changed
5. **Re-run from scratch**: Start a new loop with `javafx-orchestrator`
6. **Verify configuration**: Check `.loop-config.json` — v1.0 introduces new fields (`architect_phase`, `refactor_phase`)

### 5.2 Upgrading JDK + JavaFX

#### JDK 17 → JDK 21
1. Install JDK 21
2. Update `pom.xml`: `<maven.compiler.source>21</maven.compiler.source>`
3. Update JavaFX dependency: `<javafx.version>21</javafx.version>`
4. Run `mvn clean compile` to verify
5. Run `javafx-runner` full verification
6. **New features available**: Virtual Threads, Pattern Matching for switch, Record Patterns

#### JDK 21 → JDK 25
1. Install JDK 25
2. Update `pom.xml`: `<maven.compiler.source>25</maven.compiler.source>`
3. Update JavaFX dependency: `<javafx.version>25</javafx.version>`
4. Run `mvn clean compile` to verify
5. Run `javafx-runner` full verification
6. **New features available**: Scoped Values (stable), latest language features

#### JavaFX 17 → JavaFX 21
1. Update `pom.xml`: `<javafx.version>21</javafx.version>`
2. Check for deprecated API usage: Run `javafx-code-reviewer` full review
3. Fix any deprecation warnings found by reviewer
4. Run `mvn clean compile` to verify
5. Run `javafx-runner` runtime verification

### 5.3 Upgrading Skill Versions

When a new skill version is released:

1. **Read the migration guide**: Check this document for breaking changes
2. **Backup current state**: Save `.loop-state.json` and project files
3. **Update SKILL.md files**: Replace with new versions
4. **Update report schemas**: If JSON schemas changed, update `report-schema.json` files
5. **Run incremental review**: Use `javafx-code-reviewer` in incremental mode to check for new violations
6. **Run full verification**: Use `javafx-runner` to verify compilation and runtime

## 6. Skill Dependency Graph

The following diagram shows the dependency relationships between skills. When upgrading, update skills in topological order (dependencies first):

```
javafx-architect (no dependencies)
       ↓
javafx-designer (depends on architect, optional)
       ↓
javafx-developer (depends on architect/designer, optional; reviewer/runner, required)
       ↓
javafx-code-reviewer (depends on developer)
javafx-runner (depends on developer)
javafx-tester (depends on developer)
javafx-refactorer (depends on developer)
       ↓
javafx-docgen (depends on developer)
javafx-deployer (depends on developer)
       ↓
javafx-orchestrator (depends on all)
```

### Upgrade Order
1. `javafx-architect` (no dependencies)
2. `javafx-designer`
3. `javafx-developer`
4. `javafx-code-reviewer`, `javafx-runner`, `javafx-tester`, `javafx-refactorer` (parallel)
5. `javafx-docgen`, `javafx-deployer` (parallel)
6. `javafx-orchestrator` (last — depends on all)

## 7. Configuration Migration

### `.loop-config.json` Field History

| Field | Introduced | Status | Notes |
|-------|-----------|--------|-------|
| `output_format` | v1.0 | ✅ Active | Controls Markdown/JSON/both output |
| `max_rounds` | v1.0 | ✅ Active | Max fix-verify cycles (default: 3) |
| `clean_compile` | v1.0 | ✅ Active | Force full compilation (default: false) |
| `coverage_threshold` | v1.0 | ✅ Active | JaCoCo coverage threshold (default: 0.60) |
| `parallel_execution` | v1.0 | ✅ Active | Enable parallel reviewer+runner (default: true) |
| `deep_testing` | v1.0 | ✅ Active | Enable tester phase (default: true) |
| `docgen` | v1.0 | ✅ Active | Enable documentation generation (default: true) |
| `design_phase` | v1.0 | ✅ Active | Enable designer phase (default: false) |
| `deploy_phase` | v1.0 | ✅ Active | Enable deployer phase (default: false) |
| `dashboard` | v1.0 | ✅ Active | Enable dashboard generation (default: true) |
| `architect_phase` | v1.0 | ✅ Active | Enable architect phase (default: false) |
| `refactor_phase` | v1.0 | ✅ Active | Enable refactorer phase (default: false) |

## 8. Upgrade Checklist

Use this checklist when upgrading to a new version of the JavaFX Skill Set:

- [ ] Read the migration guide for breaking changes
- [ ] Backup project directory (including `.loop-state.json` and `.fix-backup/`)
- [ ] Backup current `.loop-config.json`
- [ ] Update SKILL.md files in topological order (see Section 6)
- [ ] Update `report-schema.json` files if schemas changed
- [ ] Update `report-templates/*.md` if templates changed
- [ ] Update `.loop-config.json` with any new fields
- [ ] Run `mvn clean compile` to verify compilation
- [ ] Run `javafx-code-reviewer` full review to check for new violations
- [ ] Run `javafx-runner` full verification
- [ ] If using `javafx-tester`, run deep testing
- [ ] Verify `.loop-state.json` is correctly initialized
- [ ] Test a complete loop cycle end-to-end

## 9. Getting Help

If you encounter issues during migration:
1. Check the `.loop-state.json` for error details
2. Review the `javafx-runner` verification report
3. Check the `javafx-code-reviewer` review report
4. If the issue persists, start a fresh loop with `status: "generating"` (skip state recovery)
