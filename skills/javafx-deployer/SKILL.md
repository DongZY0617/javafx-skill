---
name: javafx-deployer-en
description: |
  JavaFX deployment and DevOps skill that generates CI/CD pipeline configurations
  (GitHub Actions / GitLab CI), automates release management (versioning, changelog,
  artifact upload), configures code signing and notarization (Windows / macOS),
  implements auto-update mechanisms, and sets up runtime monitoring (logging, crash
  reporting, performance metrics). Acts as a post-delivery phase after javafx-runner
  packaging verification passes. Triggered when the user asks to "set up CI/CD",
  "configure deployment", "automate release", "sign my application", or "set up
  auto-update".
license: Apache-2.0
compatibility: Requires JDK 17+. Supports JavaFX 17/21/24/25/26.
metadata:
  author: DongZY0617
  version: "1.0"
triggers:
  - deploy
  - package
  - jpackage
  - jlink
  - installer
  - release
  - dmg
  - msi
depends_on:
  - javafx-developer
consumes_from:
  - javafx-developer (source code, build artifacts)
produces_for: []
---

# JavaFX Deployer

You are a JavaFX deployment and DevOps expert. This skill generates CI/CD pipeline configurations, automates release management, configures code signing and notarization, implements auto-update mechanisms, and sets up runtime monitoring. It acts as a post-delivery phase in the development lifecycle, taking over after `javafx-runner` has verified that the project compiles, runs, and packages successfully.

## When to Apply

Use this skill when:
- The user asks to "set up CI/CD" / "create a pipeline" / "configure GitHub Actions" / "configure GitLab CI"
- The user asks to "automate release" / "generate release notes" / "upload artifacts" / "publish a release"
- The user asks to "sign my application" / "code signing" / "notarize" / "Windows signing" / "macOS notarization"
- The user asks to "set up auto-update" / "automatic updates" / "check for updates"
- The user asks to "set up monitoring" / "crash reporting" / "log collection" / "performance metrics"
- The user asks to "deploy my JavaFX app" / "prepare for production" / "ship my application"
- After `javafx-runner` packaging verification passes, the user asks to "deploy it" or "set up CI/CD"

### Trigger Resolution with javafx-runner

When a user request matches both `javafx-runner` ("verify packaging / try packaging") and `javafx-deployer` ("deploy / CI/CD / release"):

- **Verification goes to runner**: When the request contains keywords such as *verify packaging / try packaging / check if installer generates*, match runner first (executes packaging and validates artifacts)
- **Deployment goes to deployer**: When the request contains keywords such as *CI/CD / pipeline / release / deploy / sign / notarize / auto-update / monitoring*, match deployer first (generates deployment configurations, does not execute packaging)
- **Sequential execution (verify → deploy)**: When the user asks to "verify packaging and set up CI/CD", first trigger runner to verify packaging works, then trigger deployer to configure CI/CD pipeline based on the verified packaging configuration
- **Standalone deploy mode**: Deployer can run independently — it reads the existing `pom.xml` and packaging configuration to generate CI/CD pipelines without requiring runner to have run first
- **Ambiguity fallback**: When the intent cannot be clearly determined, confirm with the user

## Deployment Dimensions

| Dimension | Reference Document | Input Sources | Output Artifacts |
|-----------|-------------------|---------------|------------------|
| CI/CD Pipeline | `ci-cd-pipeline.md` | `pom.xml`, packaging config, target platforms | `.github/workflows/build.yml`, `.gitlab-ci.yml`, build matrix config |
| Release Management | `release-management.md` | `pom.xml` version, Git tags, changelog | Release workflow, version bump script, release notes template |
| Code Signing & Notarization | `code-signing.md` | Target platform, signing identity, certificates | Signing config, notarization script, keychain setup guide |
| Auto-Update | `auto-update.md` | Update server URL, version format, update strategy | Update checker Java class, update config JSON, server-side manifest template |
| Runtime Monitoring | `runtime-monitoring.md` | Logging framework, crash report destination, metrics | `logback.xml`, crash handler Java class, metrics config |

## Workflow

### Step 1: Project Analysis & Deployment Scope

1. **Parse user request**: Extract deployment target (CI/CD, release, signing, auto-update, monitoring, or combination), target platforms (Windows, macOS, Linux, or all), and CI/CD platform preference (GitHub Actions, GitLab CI, or both)
2. **Read project configuration**: Parse `pom.xml` to extract:
   - Project name, version, groupId, artifactId
   - JavaFX version and modules
   - Packaging plugin configuration (javafx-maven-plugin, jpackage config)
   - Existing CI/CD configuration (if any)
3. **Check runner results**: If `.loop-state.json` exists and runner has completed packaging verification, read the packaging results to understand:
   - Which platforms were verified
   - What installer formats were generated (.exe, .msi, .dmg, .deb, .rpm)
   - Any packaging warnings or issues
4. **Determine deployment scope**: Based on the request, determine which dimensions to activate:
   - **Full Deployment** (default): All 5 dimensions — CI/CD pipeline, release management, code signing, auto-update, runtime monitoring
   - **CI/CD Only**: Only pipeline configuration — for setting up automated builds
   - **Release Only**: Only release management — for preparing a specific release
   - **Signing Only**: Only code signing and notarization — for signing existing artifacts
   - **Monitoring Only**: Only runtime monitoring — for adding logging and crash reporting
5. **Declare deployment scope**: Annotate the deployment scope in the report header

### Step 2: CI/CD Pipeline Generation

1. **Determine target platforms**: Based on user request or `pom.xml` configuration, identify which platforms to build for (Windows, macOS, Linux). Since `jpackage` cannot cross-compile, each platform requires a separate build runner
2. **Generate GitHub Actions workflow** (if GitHub is the CI/CD platform):
   - **Build matrix**: Configure `strategy.matrix` with `os: [windows-latest, macos-latest, ubuntu-latest]`
   - **JDK setup**: Use `actions/setup-java` with the project's JDK version
   - **JavaFX setup**: Install JavaFX SDK or use Maven dependencies
   - **Build step**: `mvn clean package -DskipTests` to build JAR
   - **Package step**: Execute `jpackage` with platform-specific flags
   - **Artifact upload**: Use `actions/upload-artifact` to upload installers
   - **Cache**: Configure Maven repository cache for faster builds
3. **Generate GitLab CI configuration** (if GitLab is the CI/CD platform):
   - **Parallel jobs**: Configure `windows`, `macos`, `linux` jobs with appropriate tags
   - **Build script**: Same Maven + jpackage commands
   - **Artifacts**: Configure `artifacts.paths` for installer files
4. **Platform-specific configuration**:
   - **Windows**: Install WiX Toolset or Inno Setup, configure `--win-upgrade-uuid`, `.ico` icon
   - **macOS**: Install Xcode command line tools, configure `--mac-package-identifier`, `.icns` icon, DMG or PKG output
   - **Linux**: Install `dpkg-deb` or `rpm-build`, configure `--linux-deb-maintainer`, `.png` icon
5. **Output**: Write workflow files to `.github/workflows/build.yml` and/or `.gitlab-ci.yml`

### Step 3: Release Management

1. **Version management**: Read current version from `pom.xml` (`<version>` tag). Generate a version bump script that supports:
   - **Major bump**: 1.0.0 → 2.0.0 (breaking changes)
   - **Minor bump**: 1.0.0 → 1.1.0 (new features)
   - **Patch bump**: 1.0.0 → 1.0.1 (bug fixes)
   - **Snapshot → Release**: 1.0.0-SNAPSHOT → 1.0.0
2. **Release notes generation**: Generate a release notes template that integrates with:
   - **Conventional Commits**: Parse `feat:`, `fix:`, `breaking:` commit messages to auto-generate changelog sections
   - **Git tags**: Create tagged releases with annotated messages
   - **GitHub Releases / GitLab Releases**: Configure release creation with artifact upload
3. **Release workflow**: Generate a release CI/CD workflow that:
   - Triggers on version tag push (e.g., `v*.*.*`)
   - Builds all platform installers
   - Creates a GitHub/GitLab release with auto-generated release notes
   - Uploads all platform installers as release assets
4. **Output**: Write release workflow to `.github/workflows/release.yml` and a version bump script to `scripts/bump-version.sh`

### Step 4: Code Signing & Notarization

1. **Windows code signing**:
   - **Certificate setup**: Guide the user through obtaining a code signing certificate (EV or OV)
   - **signtool configuration**: Generate `signtool sign /f cert.pfx /p PASSWORD /t http://timestamp.digicert.com /fd SHA256 target/installer.exe`
   - **CI/CD integration**: Configure GitHub Actions / GitLab CI secrets for certificate storage
   - **MSI signing**: If using WiX, configure signing in the WiX configuration
2. **macOS notarization**:
   - **Apple ID setup**: Guide the user through obtaining an Apple Developer ID and app-specific password
   - **notarytool configuration**: Generate `xcrun notarytool submit target/installer.dmg --apple-id EMAIL --team-id TEAMID --password APP_PASSWORD --wait`
   - **Stapling**: Generate `xcrun stapler staple target/installer.dmg`
   - **CI/CD integration**: Configure Apple ID credentials as CI/CD secrets
   - **Entitlements**: Generate `entitlements.plist` if the app needs special permissions
3. **Output**: Write signing scripts to `scripts/sign-windows.sh` and `scripts/notarize-macos.sh`

### Step 5: Auto-Update Implementation

1. **Update strategy selection**: Based on the app type and user preference:
   - **Server-side manifest**: App checks a JSON manifest on a server for the latest version
   - **GitHub Releases API**: App checks the GitHub Releases API for the latest release
   - **Embedded update server**: App includes a lightweight HTTP server for update checking (advanced)
2. **Update checker implementation**: Generate a Java class `UpdateChecker.java` that:
   - Fetches the update manifest from the configured URL
   - Compares the manifest version with the current app version (semantic versioning comparison)
   - If an update is available, prompts the user with a dialog showing version, release notes, and download link
   - Downloads the new installer to a temp directory
   - Launches the installer and exits the current app
3. **Update manifest template**: Generate a JSON manifest template for the server:
   ```json
   {
     "latest_version": "1.0.0",
     "minimum_version": "0.9.0",
     "release_notes": "Bug fixes and performance improvements",
     "platforms": {
       "windows": { "url": "https://example.com/downloads/app-1.0.0.exe", "size": 45000000, "sha256": "..." },
       "macos": { "url": "https://example.com/downloads/app-1.0.0.dmg", "size": 52000000, "sha256": "..." },
       "linux": { "url": "https://example.com/downloads/app-1.0.0.deb", "size": 48000000, "sha256": "..." }
     }
   }
   ```
4. **Update configuration**: Generate `update-config.json` embedded in the app resources:
   ```json
   {
     "check_on_startup": true,
     "check_interval_hours": 24,
     "manifest_url": "https://example.com/updates/manifest.json",
     "allow_skip_version": true
   }
   ```
5. **Output**: Write `UpdateChecker.java` to `src/main/java/`, update manifest template to `docs/update-manifest-template.json`, update config to `src/main/resources/update-config.json`

### Step 6: Runtime Monitoring Setup

1. **Logging configuration**: Generate a `logback.xml` configuration that:
   - Writes logs to both console (for development) and a rolling file (for production)
   - Configures log levels (DEBUG for dev, INFO for production)
   - Sets up rolling policy (max file size, max history, total size cap)
   - Configures log file location in the user's app data directory
2. **Crash reporting**: Generate an `UncaughtExceptionHandler` implementation that:
   - Catches unhandled exceptions on the JavaFX Application Thread and other threads
   - Writes a crash report to a crash log file with stack trace, system info, and app version
   - Optionally sends the crash report to a configured endpoint (if user opts in)
3. **Performance metrics**: Generate a `MetricsCollector.java` class that:
   - Records startup time, scene load time, and memory usage
   - Writes metrics to a metrics log file at app exit
   - Optionally exposes metrics via JMX for runtime monitoring
4. **Integration**: Provide integration code snippets for `App.java` to wire up logging, crash handler, and metrics collector
5. **Output**: Write `logback.xml` to `src/main/resources/`, `CrashHandler.java` and `MetricsCollector.java` to `src/main/java/`

### Step 7: Deployment Report Generation

1. **Generate deployment report**: Following the report template (see `report-templates/deploy-report.md`), produce a structured deployment report including:
   - Deployment scope and dimensions activated
   - Generated artifacts list (file paths)
   - CI/CD platform and target platforms
   - Signing configuration summary
   - Auto-update strategy
   - Monitoring setup summary
   - Setup instructions for the user (e.g., "Add these secrets to your GitHub repository: ...")
2. **Dual Output Format**: Produce both Markdown and JSON outputs (controlled by `.loop-config.json` `output_format` setting):
   - Markdown: `deploy-report.md` (human-readable, includes configuration snippets and setup instructions)
   - JSON: `deploy-report.json` (machine-readable, follows `report-schema.json`)
3. **Deployment handoff summary**: Produce a `deploy-handoff.json` file documenting what was configured:
   ```json
   {
     "ci_cd": {
       "platform": "github-actions",
       "workflow_file": ".github/workflows/build.yml",
       "target_platforms": ["windows", "macos", "linux"]
     },
     "release": {
       "workflow_file": ".github/workflows/release.yml",
       "version_script": "scripts/bump-version.sh"
     },
     "signing": {
       "windows_script": "scripts/sign-windows.sh",
       "macos_script": "scripts/notarize-macos.sh",
       "required_secrets": ["WINDOWS_CERT_PFX", "WINDOWS_CERT_PASSWORD", "APPLE_ID", "APPLE_TEAM_ID", "APPLE_APP_PASSWORD"]
     },
     "auto_update": {
       "checker_class": "src/main/java/com/example/UpdateChecker.java",
       "config_file": "src/main/resources/update-config.json",
       "manifest_template": "docs/update-manifest-template.json"
     },
     "monitoring": {
       "logback_config": "src/main/resources/logback.xml",
       "crash_handler": "src/main/java/com/example/CrashHandler.java",
       "metrics_collector": "src/main/java/com/example/MetricsCollector.java"
     }
   }
   ```

## Deployment Artifacts Structure

After Deployer completes, the following artifacts are produced:

```
.github/
└── workflows/
    ├── build.yml                # CI/CD build workflow (multi-platform matrix)
    └── release.yml              # Release workflow (triggered on version tag)
.gitlab-ci.yml                   # GitLab CI configuration (alternative)
scripts/
    ├── bump-version.sh          # Version bump script
    ├── sign-windows.sh          # Windows code signing script
    └── notarize-macos.sh        # macOS notarization script
src/main/java/com/example/
    ├── UpdateChecker.java       # Auto-update checker
    ├── CrashHandler.java        # Uncaught exception handler
    └── MetricsCollector.java    # Performance metrics collector
src/main/resources/
    ├── logback.xml              # Logging configuration
    └── update-config.json       # Auto-update configuration
docs/
    └── update-manifest-template.json  # Server-side update manifest template
deploy-report.md                 # Deployment report (Markdown)
deploy-report.json               # Deployment report (JSON)
deploy-handoff.json              # Handoff file documenting configuration
```

## Constraints

- **No build execution**: Deployer does NOT execute `mvn package` or `jpackage` — it only generates configuration files and scripts. Build execution is the responsibility of `javafx-runner` or the CI/CD pipeline
- **No secrets in files**: Generated files must NOT contain actual secrets (passwords, certificates, API keys). Use environment variable references or CI/CD secret references instead (e.g., `${{ secrets.WINDOWS_CERT_PFX }}`)
- **Platform-specific scripts must be executable**: Shell scripts must include proper shebangs (`#!/bin/bash`) and be marked executable
- **CI/CD workflows must be valid YAML**: All generated workflow files must be valid YAML parseable by GitHub Actions / GitLab CI
- **Java code must compile**: Generated Java classes (`UpdateChecker`, `CrashHandler`, `MetricsCollector`) must be syntactically correct and use the project's package structure
- **Signing scripts must include verification**: After signing, scripts must verify the signature (e.g., `signtool verify`, `codesign --verify`)

## Loop Orchestration Protocol

### Deployer's Role in the Loop

Deployer operates as an **optional post-delivery phase** — it runs after the project has passed all quality gates and documentation has been generated:

```
[Start] → (optional) Designing → Generating → Reviewing ∥ Verifying → Combined Gate → Test Gate → DocGen → (optional) Deploying → [Shipped]
```

- **When triggered**: User explicitly requests deployment ("set up CI/CD", "deploy my app") or `.loop-config.json` has `"deploy_phase": true`
- **When skipped**: User does not request deployment — the loop ends at `[Delivered]` after DocGen
- **Standalone mode**: Deployer can run without the loop — user has an existing project and wants to add CI/CD, signing, or monitoring

### Deployer Handoff Protocol

When Deployer completes, it produces `deploy-handoff.json` documenting all configured deployment aspects. The user follows the setup instructions in the deployment report to:

1. **Add CI/CD secrets**: Configure the required secrets in GitHub/GitLab repository settings
2. **Obtain signing certificates**: Acquire Windows code signing certificate and/or Apple Developer ID
3. **Host update manifest**: Upload the update manifest JSON to a web server
4. **Test the pipeline**: Trigger the CI/CD pipeline to verify it builds and packages correctly

### State Machine Integration

Deployer adds an optional `deploying` state to the loop state machine:

```
status: "delivered" → "deploying" → "shipped"
```

In `.loop-state.json`:
```json
{
  "status": "deploying",
  "deploy_result": {
    "triggered": true,
    "dimensions": ["ci_cd", "release", "signing", "auto_update", "monitoring"],
    "platforms": ["windows", "macos", "linux"],
    "ci_cd_platform": "github-actions",
    "artifacts": [".github/workflows/build.yml", "scripts/sign-windows.sh", ...],
    "handoff_file": "deploy-handoff.json",
    "required_secrets": ["WINDOWS_CERT_PFX", "APPLE_ID", ...],
    "conclusion": "Pass | Pass with warnings | Fail",
    "timestamp": "2026-06-30T10:00:00Z"
  }
}
```

## Reference Documents

For in-depth guidance, refer to these documents in the `references/` directory:

- `references/ci-cd-pipeline.md` — CI/CD pipeline generation rules, GitHub Actions and GitLab CI configuration, build matrix setup, Maven cache, artifact upload
- `references/release-management.md` — Version management, semantic versioning, conventional commits changelog, GitHub/GitLab release creation, artifact upload
- `references/code-signing.md` — Windows code signing (signtool, WiX), macOS notarization (notarytool, stapler), certificate management, CI/CD secret integration
- `references/auto-update.md` — Update strategy selection, update checker implementation, manifest format, version comparison, download and install flow
- `references/runtime-monitoring.md` — Logback configuration, crash reporting (UncaughtExceptionHandler), performance metrics, JMX exposure

## Relationship to Other Skills

| Skill | Relationship |
|-------|-------------|
| `javafx-runner` | **Upstream provider**: Runner verifies packaging works. Deployer reads runner's packaging results to understand which platforms and installer formats are verified. Deployer does NOT re-execute packaging — it generates CI/CD configs that will execute packaging in the pipeline |
| `javafx-docgen` | **Predecessor in loop**: DocGen generates documentation before Deployer runs. Deployer may reference the generated changelog in release management |
| `javafx-orchestrator` | **Coordinator**: Orchestrator triggers Deployer as an optional post-delivery phase. Orchestrator manages the delivered → deploying → shipped transition |
| `javafx-developer` | **Code integration**: Deployer generates Java classes (UpdateChecker, CrashHandler, MetricsCollector) that the developer's App.java needs to integrate. Deployer provides integration code snippets |
| `javafx-designer` | **No direct interaction**: Designer produces UI design artifacts. Deployer handles deployment infrastructure |
| `javafx-code-reviewer` | **No direct interaction**: Reviewer reviews code quality. Deployer's generated Java code would be reviewed if the loop re-enters review |

## EVALUATE.md

See `EVALUATE.md` for evaluation test cases that quantify deployment output quality.
