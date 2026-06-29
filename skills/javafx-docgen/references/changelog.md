# Conventional Commit Parsing and Changelog Formatting

This reference defines how the JavaFX DocGen skill reads Git history, parses conventional commit messages, groups them by version tag, and renders a human-readable changelog. It covers the git command, the commit-message grammar, categorization, and the rendered output format.

## Overview

The changelog is generated entirely from the project's Git history. No manual entries are required. The generator executes `git log`, classifies each commit by its conventional-commit type, groups commits under the version tag that precedes them, and renders a Keep-a-Changelog-style document.

## Executing git log

The generator runs the following command to retrieve commit metadata:

```bash
git log --tags --simplify-by-decoration \
  --pretty="format:%H%x09%d%x09%s"
```

Fields are tab-separated: commit hash, decorations (tags), and subject. When tags are present, a second command lists tags in chronological order to anchor version sections:

```bash
git tag --sort=creatordate
```

If the repository has no commits (for example, a freshly initialized project), changelog generation is skipped and the docgen report records a warning. If Git is not installed, the same skip-and-warn behavior applies.

## Conventional Commit Grammar

A conventional commit subject has the form:

```text
<type>[optional scope][!]: <description>
```

- `type` — one of the recognized types listed below.
- `scope` — optional, parenthesized, e.g. `(controller)`.
- `!` — signals a breaking change at the type level.
- `description` — imperative-mood summary.

Examples:

```text
feat(controller): add CSV export action
fix(model): correct timezone parsing in Order
refactor(service)!: rename DataProvider SPI
docs: update README run instructions
```

A separate `BREAKING CHANGE:` footer in the commit body also marks the commit as breaking, regardless of type:

```text
feat: add preference persistence

BREAKING CHANGE: PreferenceManager now requires a Path argument.
```

## Commit Type Classification

| Commit Type | Changelog Category | Notes |
|-------------|--------------------|-------|
| `feat` | Added | New feature |
| `fix` | Fixed | Bug fix |
| `perf` | Changed | Performance improvement |
| `refactor` | Changed | Code restructuring, no behavior change |
| `docs` | (Documentation) | Only included if `include_docs` is set |
| `test` | (Tests) | Only included if `include_tests` is set |
| `build` / `ci` / `chore` | (Maintenance) | Omitted by default |
| `revert` | Removed | Reverts a prior change |
| `!` or `BREAKING CHANGE:` | Breaking Changes | Surfaced at the top of each version |

## Grouping Commits by Version Tag

Commits are bucketed under the most recent tag that precedes them. Commits before the first tag fall under an `Unreleased` section. Tags are expected to follow semantic versioning (`v1.0.0`, `1.2.3`); non-semver tags are still used as section headers but are sorted by creation date.

```text
v1.2.0  <-- tag
  feat: add export dialog
  fix: crash on empty list
v1.1.0
  feat: add search field
HEAD (Unreleased)
  feat: add dark theme
```

## Changelog Categories

Within each version section, entries are grouped into Keep-a-Changelog categories, in this fixed order:

1. **Breaking Changes**
2. **Added**
3. **Changed**
4. **Deprecated**
5. **Removed**
6. **Fixed**
7. **Security**

`Deprecated` and `Security` entries are sourced from commit descriptions containing the words `deprecat` or `security`/`CVE`, respectively, in addition to explicit types.

## Formatting Changelog Entries

Each entry is a bullet list item with a short description and a commit hash reference. The scope, when present, is shown in bold before the description.

```markdown
## [1.2.0] - 2026-05-10

### Added
- **controller**: add CSV export action (`a1b2c3d`)
- preference persistence layer (`e4f5g6h`)

### Fixed
- **model**: correct timezone parsing in Order (`9h8i7j6`)

### Breaking Changes
- **service**: rename DataProvider SPI (`d3c2b1a`)
```

Descriptions are normalized: the type prefix and trailing period are stripped, and the first letter is capitalized. Long descriptions are truncated at 120 characters with an ellipsis.

## Handling Non-Conventional Commits

Commits whose subject does not match the conventional grammar are not discarded. They are collected into an `Other` subsection at the bottom of each version, listed verbatim with their hash. This keeps the history complete while signaling that the message did not follow the convention.

```markdown
### Other
- updated several files (`0p1q2r3`)
- wip (`4s5t6u7`)
```

If more than 40% of commits in a version are non-conventional, the docgen report emits a style warning recommending adoption of conventional commits.

## Changelog Document Template

The final `CHANGELOG.md` follows this structure:

```markdown
# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/)
and this project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- ...

## [1.2.0] - 2026-05-10

### Added
- ...

### Fixed
- ...
```

Versions are listed newest-first. The `[Unreleased]` section is always present, even when empty, so contributors know where to record upcoming changes.
