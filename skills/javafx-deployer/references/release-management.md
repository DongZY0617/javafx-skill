# Release Management

This document defines the rules for versioning, changelog generation, and release publishing for JavaFX desktop applications. It covers Semantic Versioning, Conventional Commits parsing, version bump automation, and CI/CD release workflows for both GitHub and GitLab.

## 1. Semantic Versioning

All releases follow Semantic Versioning `MAJOR.MINOR.PATCH`:

| Segment | Bump trigger | Backward compatibility |
|---------|--------------|------------------------|
| `MAJOR` | Breaking/incompatible API changes | No |
| `MINOR` | New backward-compatible features | Yes |
| `PATCH` | Backward-compatible bug fixes | Yes |

### Pre-release Suffixes
Pre-release versions append a hyphen and identifier to denote stability stage:

| Suffix | Example | Meaning |
|--------|---------|---------|
| `-alpha` | `1.0.0-alpha` | Early internal testing |
| `-beta` | `1.0.0-beta.2` | Feature complete, broader testing |
| `-rc` | `1.0.0-rc.1` | Release candidate, final validation |

### SNAPSHOT Convention
In Maven, a version ending in `-SNAPSHOT` (e.g. `1.2.0-SNAPSHOT`) denotes a development build that is not yet released. On release, strip `-SNAPSHOT`, build the artifacts, tag the commit, then bump the next development version with `-SNAPSHOT` re-appended.

## 2. Version Bump Script

The following Bash script reads the current version from `pom.xml`, increments the requested segment, updates `pom.xml`, commits the change, and creates a git tag.

```bash
#!/usr/bin/env bash
# bump-version.sh
set -euo pipefail

VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
BASE="${VERSION%-SNAPSHOT}"
IFS='.' read -r MAJOR MINOR PATCH <<< "$BASE"

BUMP="${1:-patch}"
case "$BUMP" in
  major) MAJOR=$((MAJOR+1)); MINOR=0; PATCH=0 ;;
  minor) MINOR=$((MINOR+1)); PATCH=0 ;;
  patch) PATCH=$((PATCH+1)) ;;
  release) ;; # strip SNAPSHOT only
  *) echo "Usage: $0 {major|minor|patch|release}"; exit 1 ;;
esac

NEW_VERSION="${MAJOR}.${MINOR}.${PATCH}"
mvn versions:set -DnewVersion="${NEW_VERSION}" -DgenerateBackupPoms=false
git add pom.xml
git commit -m "chore(release): ${NEW_VERSION}"
git tag "v${NEW_VERSION}"
echo "Released ${NEW_VERSION} and tagged v${NEW_VERSION}"

# Bump next dev version
NEXT_PATCH=$((PATCH+1))
DEV_VERSION="${MAJOR}.${MINOR}.${NEXT_PATCH}-SNAPSHOT"
mvn versions:set -DnewVersion="${DEV_VERSION}" -DgenerateBackupPoms=false
git add pom.xml
git commit -m "chore(dev): prepare ${DEV_VERSION}"
```

## 3. Conventional Commits Parsing

Commit messages follow the Conventional Commits spec: `type(scope): description`. The commit `type` determines the automatic bump level.

| Commit type | Bump level | Section in changelog |
|-------------|------------|----------------------|
| `feat` | minor | Added |
| `fix` | patch | Fixed |
| `perf` | patch | Changed |
| `breaking` / `BREAKING CHANGE` footer | major | Changed |
| `docs`, `chore`, `style`, `refactor`, `test` | none | (omitted or Removed) |

### Auto-determine Bump Type
To choose the bump from commit history since the last tag:

```bash
LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
RANGE="${LAST_TAG}..HEAD"
if git log "$RANGE" --grep="BREAKING CHANGE" -q || git log "$RANGE" --grep="^breaking" -q; then
  BUMP="major"
elif git log "$RANGE" --grep="^feat" -q; then
  BUMP="minor"
elif git log "$RANGE" --grep="^fix" -q; then
  BUMP="patch"
else
  BUMP="none"
fi
./bump-version.sh "$BUMP"
```

## 4. Changelog Generation

The changelog is grouped into four sections, each listing entries with links to the originating commit hash.

```markdown
## [1.2.0] - 2026-06-30

### Added
- Dark mode theme switcher ([abc1234](https://github.com/org/repo/commit/abc1234))
- Export to PDF option ([def5678](https://github.com/org/repo/commit/def5678))

### Changed
- Upgraded JavaFX runtime to 21.0.3 ([ghi9012](https://github.com/org/repo/commit/ghi9012))

### Fixed
- Crash on empty project load ([jkl3456](https://github.com/org/repo/commit/jkl3456))

### Removed
- Deprecated legacy import wizard ([mno7890](https://github.com/org/repo/commit/mno7890))
```

Automation tools such as `conventional-changelog` or `git-cliff` generate this file from commit history. Always commit the regenerated `CHANGELOG.md` together with the version bump.

## 5. GitHub Release Creation

Use `softprops/action-gh-release` to create the release and upload per-platform assets when a `v*.*.*` tag is pushed.

```yaml
release:
  needs: build
  runs-on: ubuntu-latest
  if: startsWith(github.ref, 'refs/tags/v')
  steps:
    - uses: actions/download-artifact@v4
      with:
        path: artifacts
    - uses: softprops/action-gh-release@v2
      with:
        generate_release_notes: true
        files: |
          artifacts/myapp-windows/*.msi
          artifacts/myapp-windows/*.exe
          artifacts/myapp-macos/*.dmg
          artifacts/myapp-linux/*.deb
          artifacts/myapp-linux/*.rpm
```

`generate_release_notes: true` auto-generates release notes from PRs and commits between the current and previous tag.

## 6. GitLab Release Creation

GitLab uses the `release-cli` to publish releases with per-platform asset links.

```yaml
release:
  stage: release
  image: registry.gitlab.com/gitlab-org/release-cli:latest
  rules:
    - if: $CI_COMMIT_TAG
  script:
    - echo "Creating release for $CI_COMMIT_TAG"
  release:
    tag_name: $CI_COMMIT_TAG
    description: "Release $CI_COMMIT_TAG"
    assets:
      links:
        - name: "Windows MSI"
          url: "${CI_PROJECT_URL}/-/jobs/${PACKAGE_WINDOWS_JOB_ID}/artifacts/file/target/MyApp.msi"
        - name: "macOS DMG"
          url: "${CI_PROJECT_URL}/-/jobs/${PACKAGE_MACOS_JOB_ID}/artifacts/file/target/MyApp.dmg"
        - name: "Linux DEB"
          url: "${CI_PROJECT_URL}/-/jobs/${PACKAGE_LINUX_JOB_ID}/artifacts/file/target/myapp.deb"
```

## 7. Release Workflow YAML

A complete tag-triggered release workflow builds all platforms, then publishes.

```yaml
name: Release
on:
  push:
    tags: [ 'v*.*.*' ]
jobs:
  build:
    strategy:
      fail-fast: false
      matrix:
        os: [ windows-latest, macos-latest, ubuntu-latest ]
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: maven
      - run: mvn -B clean package -DskipTests
      - name: jpackage
        shell: bash
        run: ./.github/scripts/jpackage.sh "${{ github.ref_name }}"
      - uses: actions/upload-artifact@v4
        with:
          name: assets-${{ matrix.os }}
          path: |
            target/*.msi
            target/*.exe
            target/*.dmg
            target/*.deb
            target/*.rpm
  publish:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/download-artifact@v4
        with: { path: assets }
      - uses: softprops/action-gh-release@v2
        with:
          generate_release_notes: true
          files: |
            assets/**/*
```

## 8. Release Checklist

Before a release is considered complete, every item below must be satisfied:

- [ ] Version bumped in `pom.xml` (SNAPSHOT stripped for release)
- [ ] `CHANGELOG.md` regenerated and committed
- [ ] All tests pass on `main` (`mvn verify`)
- [ ] Git tag `vX.Y.Z` pushed to remote
- [ ] CI matrix build succeeds on Windows, macOS, and Linux
- [ ] Artifacts signed and notarized (see `code-signing.md`)
- [ ] GitHub/GitLab Release published with all platform assets attached
- [ ] Next development version (`X.Y.Z+1-SNAPSHOT`) committed on `main`
- [ ] Release announcement communicated to users
