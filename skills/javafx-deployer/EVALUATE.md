# JavaFX Deployer Evaluation Test Cases

This file defines the acceptance test cases for the `javafx-deployer` skill, used to quantify deployment output quality. Each case describes the input scenario, case type, covered dimensions, expected outputs, and checkable verification standards.

- **Positive samples**: Real deployment requests that verify deployer output completeness and correctness
- **Negative samples**: Constraint violations that verify deployer robustness
- **Boundary cases**: Partial deployment scopes, standalone mode, platform-specific scenarios

---

## Case Overview

| ID | Name | Type | Covered Dimensions | Expected Artifacts |
|----|------|------|--------------------|--------------------|
| 1 | Full deployment: GitHub Actions | Positive | All 5 | CI/CD + Release + Signing + Auto-update + Monitoring |
| 2 | Full deployment: GitLab CI | Positive | All 5 | Same as above with GitLab config |
| 3 | CI/CD only: multi-platform matrix | Boundary | CI/CD | Workflow YAML only |
| 4 | Release only: version bump + changelog | Boundary | Release | Release workflow + version script |
| 5 | Signing only: Windows + macOS | Boundary | Signing | Signing scripts only |
| 6 | Monitoring only: logging + crash + metrics | Boundary | Monitoring | logback.xml + Java classes |
| 7 | No secrets in generated files | Negative | Signing | Scripts use env vars, no plaintext secrets |
| 8 | YAML validity check | Positive | CI/CD | Valid YAML parseable by CI/CD platform |
| 9 | Java code compilability | Positive | Auto-update, Monitoring | Generated Java is syntactically correct |
| 10 | Auto-update manifest format | Positive | Auto-update | Valid JSON manifest with all required fields |
| 11 | Post-delivery mode (with loop) | Boundary | All 5 | Artifacts + shipped next_action |
| 12 | Standalone mode (no loop) | Boundary | All 5 | Artifacts + standalone_complete |
| 13 | Single platform (Windows only) | Boundary | CI/CD, Signing | Windows-only config |
| 14 | No build execution | Negative | All 5 | No mvn package or jpackage executed |
| 15 | Shell scripts executable | Positive | Signing, Release | Scripts have shebang and executable permission |

---

## Case 1: Full Deployment — GitHub Actions

- **Input**: "Set up CI/CD for my JavaFX app using GitHub Actions. I need multi-platform builds, automated releases, Windows code signing, macOS notarization, auto-update, and runtime monitoring."
- **Type**: Positive sample
- **Covered Dimensions**: All 5 (ci_cd, release, signing, auto_update, monitoring)
- **Expected Artifacts**:
  - `.github/workflows/build.yml` (multi-platform build matrix)
  - `.github/workflows/release.yml` (tag-triggered release)
  - `scripts/bump-version.sh`, `scripts/sign-windows.sh`, `scripts/notarize-macos.sh`
  - `src/main/java/.../UpdateChecker.java`, `CrashHandler.java`, `MetricsCollector.java`
  - `src/main/resources/logback.xml`, `update-config.json`
  - `docs/update-manifest-template.json`
  - `deploy-handoff.json`
- **Verification Standards**:
  - [ ] Build workflow has `strategy.matrix` with `os: [windows-latest, macos-latest, ubuntu-latest]`
  - [ ] Build workflow uses `actions/setup-java` with correct JDK version
  - [ ] Build workflow includes Maven cache configuration
  - [ ] Build workflow uploads artifacts with platform-specific paths
  - [ ] Release workflow triggers on `v*.*.*` tag push
  - [ ] Signing scripts reference secrets via `${{ secrets.* }}` syntax
  - [ ] UpdateChecker.java has semantic version comparison logic
  - [ ] CrashHandler.java sets `setDefaultUncaughtExceptionHandler`
  - [ ] logback.xml has rolling file appender with 10MB/30day/1GB policy
  - [ ] deploy-handoff.json lists all artifacts and required secrets
  - [ ] Design report has conclusion "Pass"

---

## Case 2: Full Deployment — GitLab CI

- **Input**: "Configure deployment for my JavaFX app using GitLab CI with multi-platform builds, releases, signing, auto-update, and monitoring."
- **Type**: Positive sample
- **Covered Dimensions**: All 5
- **Expected Artifacts**: Same as Case 1 but with `.gitlab-ci.yml` instead of GitHub Actions workflows
- **Verification Standards**:
  - [ ] `.gitlab-ci.yml` has separate jobs for windows, macos, linux with appropriate tags
  - [ ] Each job configures `artifacts.paths` for installer files
  - [ ] GitLab release uses `release-cli`
  - [ ] Signing scripts use `$CI_VARIABLE` syntax for secrets (GitLab CI variables)
  - [ ] All other artifacts same as Case 1

---

## Case 3: CI/CD Only — Multi-Platform Matrix

- **Input**: "Just set up a CI/CD pipeline for my JavaFX app. I don't need signing or auto-update yet."
- **Type**: Boundary case
- **Covered Dimensions**: CI/CD only
- **Expected Artifacts**: `.github/workflows/build.yml` only
- **Verification Standards**:
  - [ ] Only CI/CD workflow file is generated
  - [ ] No release, signing, auto-update, or monitoring artifacts
  - [ ] Deploy report scope is "CI/CD Only"
  - [ ] Deploy report dimensions array contains only "ci_cd"
  - [ ] Build matrix includes all 3 platforms

---

## Case 4: Release Only — Version Bump + Changelog

- **Input**: "Set up automated release management for my JavaFX app. I want version bumping and changelog generation from conventional commits."
- **Type**: Boundary case
- **Covered Dimensions**: Release only
- **Expected Artifacts**: `.github/workflows/release.yml`, `scripts/bump-version.sh`
- **Verification Standards**:
  - [ ] Only release workflow and version script are generated
  - [ ] Deploy report scope is "Release Only"
  - [ ] Version script supports major/minor/patch/release flags
  - [ ] Version script reads version from pom.xml and creates git tag
  - [ ] Release workflow triggers on tag push
  - [ ] Release workflow creates GitHub/GitLab release with assets

---

## Case 5: Signing Only — Windows + macOS

- **Input**: "I need code signing scripts for my JavaFX app — Windows signtool and macOS notarization."
- **Type**: Boundary case
- **Covered Dimensions**: Signing only
- **Expected Artifacts**: `scripts/sign-windows.sh`, `scripts/notarize-macos.sh`
- **Verification Standards**:
  - [ ] Only signing scripts are generated
  - [ ] Deploy report scope is "Signing Only"
  - [ ] Windows script uses `signtool sign` with SHA-256 and RFC 3161 timestamp
  - [ ] Windows script includes `signtool verify` after signing
  - [ ] macOS script uses `xcrun notarytool submit --wait`
  - [ ] macOS script includes `xcrun stapler staple` after notarization
  - [ ] Both scripts reference secrets via environment variables, not plaintext

---

## Case 6: Monitoring Only — Logging + Crash + Metrics

- **Input**: "Add runtime monitoring to my JavaFX app — logging, crash reporting, and performance metrics."
- **Type**: Boundary case
- **Covered Dimensions**: Monitoring only
- **Expected Artifacts**: `logback.xml`, `CrashHandler.java`, `MetricsCollector.java`
- **Verification Standards**:
  - [ ] Only monitoring artifacts are generated
  - [ ] Deploy report scope is "Monitoring Only"
  - [ ] logback.xml has console appender (DEBUG) and rolling file appender (INFO)
  - [ ] logback.xml rolling policy: maxFileSize 10MB, maxHistory 30, totalSizeCap 1GB
  - [ ] CrashHandler.java implements `Thread.UncaughtExceptionHandler`
  - [ ] CrashHandler writes crash reports to user home `.appname/crashes/`
  - [ ] MetricsCollector records startup time and memory usage
  - [ ] Integration code snippets for App.java are provided

---

## Case 7: No Secrets in Generated Files

- **Input**: "Set up code signing for Windows and macOS."
- **Type**: Negative sample
- **Covered Dimensions**: Signing
- **Expected Artifacts**: Signing scripts
- **Verification Standards**:
  - [ ] No generated file contains plaintext passwords, certificates, or API keys
  - [ ] Signing scripts use `$WINDOWS_CERT_PFX`, `$WINDOWS_CERT_PASSWORD` environment variables
  - [ ] macOS script uses `$APPLE_ID`, `$APPLE_TEAM_ID`, `$APPLE_APP_PASSWORD` environment variables
  - [ ] GitHub Actions workflows use `${{ secrets.* }}` syntax
  - [ ] Deploy report lists required secrets in `required_secrets` array
  - [ ] Setup instructions tell user to add secrets to CI/CD repository settings

---

## Case 8: YAML Validity Check

- **Input**: "Create a GitHub Actions workflow for my JavaFX app targeting Windows, macOS, and Linux."
- **Type**: Positive sample
- **Covered Dimensions**: CI/CD
- **Expected Artifacts**: `.github/workflows/build.yml`
- **Verification Standards**:
  - [ ] Generated YAML is valid and parseable by any YAML parser
  - [ ] Workflow has `name`, `on`, `jobs` top-level keys
  - [ ] `on` includes at least `push` or `pull_request` trigger
  - [ ] `jobs.build.strategy.matrix.os` contains all 3 platforms
  - [ ] Each step has `name` and `uses` or `run` key
  - [ ] `actions/upload-artifact` step is present
  - [ ] No YAML syntax errors (correct indentation, no tabs)

---

## Case 9: Java Code Compilability

- **Input**: "Add auto-update and runtime monitoring to my JavaFX app."
- **Type**: Positive sample
- **Covered Dimensions**: Auto-update, Monitoring
- **Expected Artifacts**: `UpdateChecker.java`, `CrashHandler.java`, `MetricsCollector.java`
- **Verification Standards**:
  - [ ] All Java files use the project's package declaration (from pom.xml groupId)
  - [ ] All import statements are valid (JavaFX, java.net.http, java.util, etc.)
  - [ ] UpdateChecker has `checkForUpdate()` method
  - [ ] CrashHandler implements `Thread.UncaughtExceptionHandler` with `uncaughtException()` method
  - [ ] MetricsCollector has `recordStartupTime()` and `recordMemoryUsage()` methods
  - [ ] No syntax errors (balanced braces, semicolons, correct method signatures)
  - [ ] Code compiles with JDK 17+

---

## Case 10: Auto-Update Manifest Format

- **Input**: "Set up auto-update for my JavaFX app using a server-side manifest."
- **Type**: Positive sample
- **Covered Dimensions**: Auto-update
- **Expected Artifacts**: `docs/update-manifest-template.json`, `src/main/resources/update-config.json`
- **Verification Standards**:
  - [ ] Manifest JSON is valid and parseable
  - [ ] Manifest has `latest_version`, `minimum_version`, `release_notes` fields
  - [ ] Manifest has `platforms` object with `windows`, `macos`, `linux` keys
  - [ ] Each platform entry has `url`, `size`, `sha256` fields
  - [ ] update-config.json has `check_on_startup`, `check_interval_hours`, `manifest_url`, `allow_skip_version`
  - [ ] Manifest template uses placeholder values (e.g., `https://example.com/...`)

---

## Case 11: Post-Delivery Mode (With Loop)

- **Input**: After loop passes all gates and DocGen completes: "Now deploy my app — set up CI/CD, signing, and auto-update."
- **Type**: Boundary case
- **Covered Dimensions**: All 5
- **Expected Artifacts**: All deployment artifacts + deploy-handoff.json
- **Verification Standards**:
  - [ ] Deploy report loop_state.next_action is "shipped"
  - [ ] Deploy report loop_state.deploy_phase is "post-delivery"
  - [ ] `.loop-state.json` transitions: `delivered` → `deploying` → `shipped`
  - [ ] `deploy_result` is recorded in `.loop-state.json`
  - [ ] Deployer reads runner's packaging results from `.loop-state.json` if available

---

## Case 12: Standalone Mode (No Loop)

- **Input**: "I have an existing JavaFX project. Set up CI/CD, release management, and monitoring."
- **Type**: Boundary case
- **Covered Dimensions**: CI/CD, Release, Monitoring
- **Expected Artifacts**: Workflow files, release workflow, monitoring Java classes
- **Verification Standards**:
  - [ ] Deploy report loop_state.next_action is "standalone_complete"
  - [ ] Deploy report loop_state.deploy_phase is "standalone"
  - [ ] No `.loop-state.json` is created (standalone mode)
  - [ ] Deployer reads pom.xml directly for project configuration
  - [ ] No dependency on prior loop execution

---

## Case 13: Single Platform (Windows Only)

- **Input**: "Set up CI/CD for my JavaFX app — I only need Windows builds."
- **Type**: Boundary case
- **Covered Dimensions**: CI/CD
- **Expected Artifacts**: `.github/workflows/build.yml` (Windows only)
- **Verification Standards**:
  - [ ] Build matrix contains only `windows-latest`
  - [ ] No macOS or Linux build steps
  - [ ] Artifact upload path targets `.exe` / `.msi` only
  - [ ] Platform-specific setup installs WiX Toolset or Inno Setup only
  - [ ] Deploy report target_platforms contains only "windows"

---

## Case 14: No Build Execution

- **Input**: "Deploy my JavaFX app — set up CI/CD and signing."
- **Type**: Negative sample
- **Covered Dimensions**: CI/CD, Signing
- **Expected Artifacts**: Configuration files and scripts only
- **Verification Standards**:
  - [ ] Deployer does NOT execute `mvn package` or `mvn clean package`
  - [ ] Deployer does NOT execute `jpackage` directly
  - [ ] Deployer only generates configuration files and scripts
  - [ ] Build execution is delegated to the CI/CD pipeline (workflow YAML contains build commands)
  - [ ] Deploy report does not claim to have built or packaged the project

---

## Case 15: Shell Scripts Executable

- **Input**: "Generate code signing scripts and version bump script."
- **Type**: Positive sample
- **Covered Dimensions**: Signing, Release
- **Expected Artifacts**: `scripts/sign-windows.sh`, `scripts/notarize-macos.sh`, `scripts/bump-version.sh`
- **Verification Standards**:
  - [ ] All shell scripts start with `#!/bin/bash` shebang
  - [ ] Scripts use `set -e` for fail-on-error behavior
  - [ ] Scripts reference environment variables with `${VAR_NAME}` syntax
  - [ ] Scripts include error handling (check for required env vars before proceeding)
  - [ ] Scripts include echo/printf statements for progress feedback
  - [ ] Version bump script accepts `--major`, `--minor`, `--patch`, `--release` flags
