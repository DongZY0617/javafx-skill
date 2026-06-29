# JavaFX CI/CD Pipeline Guide

This guide covers continuous integration and continuous delivery (CI/CD) for JavaFX projects, including headless UI testing with Monocle, cross-platform packaging with jpackage, environment setup, Loop Orchestration Protocol integration, and production-ready pipeline configurations for GitHub Actions and GitLab CI.

---

## 1. Overview

### 1.1 CI/CD Integration with the JavaFX Skill Set

The JavaFX skill set follows a closed-loop quality model defined by the **Loop Orchestration Protocol** (see `SKILL.md` → Loop Orchestration Protocol). The loop cycles through:

```
generate → review → verify → fix → re-verify
```

CI/CD pipelines operationalize the **verify** stage at scale. Where `javafx-runner` performs local verification (compile, runtime, packaging), CI pipelines perform the same verification on ephemeral infrastructure across multiple operating systems, JDK versions, and packaging targets — unattended and reproducible.

| Skill Role | Local Artifact | CI Equivalent |
|------------|---------------|---------------|
| `javafx-developer` | Generated source code | Repository commits / PR branches |
| `javafx-code-reviewer` | Review report (static) | Static analysis jobs (SpotBugs, Error Prone) |
| `javafx-runner` | Verification report (compile + runtime + packaging) | CI test, build, and package jobs |
| Fix Consumption | `.loop-state.json` loop state | CI re-run on push after fix commit |

### 1.2 Pipeline Goals

1. **Compile verification**: Catch syntax, module-path, and dependency errors on every push
2. **Headless UI testing**: Run TestFX integration tests in CI without a physical display (Monocle)
3. **Cross-platform packaging**: Produce native installers (exe/msi, dmg/pkg, deb/rpm) via jpackage matrix builds
4. **Artifact management**: Upload installers and build outputs as versioned artifacts
5. **Loop integration**: CI jobs map to the verify stage of the generate→review→verify→fix cycle, enabling automated re-verification after fixes

---

## 2. Environment Setup Requirements

### 2.1 JDK

| Requirement | Details |
|-------------|---------|
| Minimum version | JDK 17 (JavaFX 21 LTS baseline) |
| Recommended | JDK 21 LTS (Eclipse Temurin / Microsoft Build of OpenJDK) |
| Latest features | JDK 23+ (JavaFX 25 LTS), JDK 24+ (JavaFX 26) |
| Setup action | `actions/setup-java@v4` (GitHub), `java` image tag (GitLab) |

JavaFX 24+ requires the `--enable-native-access=javafx.graphics` JVM flag. This must be passed in all test runners, build plugins, and jpackage `--java-options`.

### 2.2 Maven

| Requirement | Details |
|-------------|---------|
| Version | Maven 3.8+ (bundled in GitHub Actions runners) |
| Cache key | `~/.m2/repository` keyed on `pom.xml` hash |
| Build command | `mvn -B clean package` (`-B` = batch mode, no interactive prompts) |

### 2.3 JavaFX SDK

For jpackage packaging, the CI environment needs either the JavaFX SDK or modular JavaFX JARs on the module path. When using Maven dependencies with platform classifiers, the SDK download step can be skipped for compile/test stages but is recommended for jpackage to ensure native libraries are available.

```bash
# Download JavaFX SDK for packaging
curl -L "https://download2.gluonhq.com/openjfx/21.0.11/openjfx-21.0.11_${PLATFORM}-x64_bin-sdk.zip" -o javafx-sdk.zip
unzip -q javafx-sdk.zip -d javafx-sdk
export JAVAFX_SDK="javafx-sdk/javafx-sdk-21.0.11/lib"
```

| Platform | Classifier | SDK URL Suffix |
|----------|------------|----------------|
| Windows x64 | `win` | `windows-x64_bin-sdk.zip` |
| Linux x64 | `linux` | `linux-x64_bin-sdk.zip` |
| macOS x64 | `mac` | `osx-x64_bin-sdk.zip` |
| macOS aarch64 | `mac-aarch64` | `osx-aarch64_bin-sdk.zip` |

### 2.4 Monocle (Headless Testing)

Monocle is a headless implementation of the JavaFX glass windowing toolkit. It allows TestFX integration tests to run on CI servers without a physical display or Xvnc.

| Requirement | Details |
|-------------|---------|
| Dependency | `org.testfx:openjfx-monocle:jdk-21+27` (test scope) |
| Required system properties | See table below |
| Linux only | Monocle is primarily used on Linux CI runners |

**Monocle system properties** (passed as Surefire `argLine`):

| Property | Value | Purpose |
|----------|-------|---------|
| `-Dtestfx.robot=awt` | AWT robot | Use AWT-based input injection |
| `-Dtestfx.headless=true` | true | Enable headless mode |
| `-Dprism.order=sw` | Software | Use software rendering pipeline |
| `-Dprism.text=t2k` | T2K | Use T2K text renderer (more stable in headless) |
| `--enable-native-access=javafx.graphics` | — | Required for JavaFX 24+ |

**Maven Surefire configuration** (see `templates/test/pom-test-dependencies.xml`):

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
    <configuration>
        <argLine>
            --enable-native-access=javafx.graphics
            -Dtestfx.robot=awt
            -Dtestfx.headless=true
            -Dprism.order=sw
            -Dprism.text=t2k
        </argLine>
    </configuration>
</plugin>
```

### 2.5 Platform Toolchains for Packaging

jpackage cannot cross-compile. Each platform's native installer must be built on a runner of the corresponding OS. Additional platform-specific tools are required:

| Platform | Required Tools | Installation |
|----------|---------------|--------------|
| Windows | WiX Toolset 4.x (msi), Inno Setup (exe) | `dotnet tool install --global wix` |
| macOS | Xcode command line tools | `xcode-select --install` (pre-installed on GitHub `macos-latest`) |
| Linux | `dpkg-deb` (deb), `rpmbuild` (rpm) | `sudo apt-get install -y dpkg rpm` |

For Linux packaging on CI, also install font packages to avoid tofu (missing glyphs) in the installer UI:

```bash
sudo apt-get install -y dpkg rpm libgl1-mesa-glx libxslt1.1
```

---

## 3. javafx-runner Verification to CI Stage Mapping

`javafx-runner` performs three verification dimensions locally. Each maps directly to a CI job/stage:

| javafx-runner Dimension | Local Verification | CI Stage | CI Job |
|------------------------|--------------------|----------|--------|
| Compile verification | `mvn compile` | `build` | `compile` |
| Runtime verification | `mvn test` (TestFX + Monocle) | `test` | `test` |
| Packaging verification | `jpackage` smoke build | `package` | `jpackage` |

### 3.1 CI Quality Gate Mapping

The Loop Orchestration Protocol defines a combined quality gate (reviewer AND runner must pass). In CI, this maps to job dependencies:

```
compile (gate: must pass)
   ↓
test (gate: must pass, headless Monocle)
   ↓
package (gate: conditional, required for release)
```

**Short-circuit rule**: If compile fails, test and package stages are skipped (matching the runner's "compile failure short-circuits" rule from the Loop Orchestration Protocol).

### 3.2 CI Result to Loop State Mapping

| CI Result | Loop State Equivalent | Next Action |
|-----------|-----------------------|------------|
| All jobs pass | `runner_result.conclusion: Pass` | Combined gate check; if reviewer also passes → deliver |
| Test job fails | `runner_result.conclusion: Fail` (runtime) | Generate fix handoff for runtime issues |
| Package job fails | `runner_result.conclusion: Conditional` (packaging) | Generate fix handoff for packaging issues |
| Compile fails | `runner_result.conclusion: Fail` (compile) | Short-circuit: skip runtime + packaging verification |

---

## 4. GitHub Actions Configuration

### 4.1 Workflow Structure

GitHub Actions workflows for JavaFX projects use a **matrix strategy** to run jobs across multiple operating systems. The key components are:

1. **Matrix**: `os: [windows-latest, macos-latest, ubuntu-latest]`
2. **JDK setup**: `actions/setup-java@v4` with Temurin distribution
3. **Maven cache**: `actions/cache@v4` keyed on `~/.m2/repository`
4. **JavaFX SDK**: Downloaded for jpackage (packaging stage only)
5. **Monocle**: Configured via Maven Surefire `argLine` (no CI-specific setup needed beyond Linux fonts)

### 4.2 Monocle Headless Testing on Linux

On Linux CI runners, TestFX tests run headlessly via Monocle. No `xvfb` or display server is needed — Monocle provides a software-based windowing toolkit.

Key configuration:
- The `-Dtestfx.headless=true` system property activates Monocle
- `-Dprism.order=sw` forces software rendering (no GPU on CI)
- `-Dprism.text=t2k` uses the T2K text engine for stable headless rendering
- The `openjfx-monocle` dependency must be on the test classpath

On Windows and macOS CI runners, a display server is available by default, so Monocle is not strictly required. However, for consistency, headless mode can be enabled on all platforms.

### 4.3 jpackage Cross-Platform Packaging Matrix

Each platform produces a different installer type. The matrix uses `include` to map OS to platform-specific configuration:

| OS | Platform | Installer Type | Icon Format | Extra Flags |
|----|----------|---------------|-------------|-------------|
| `windows-latest` | `windows` | `msi` | `.ico` | `--win-menu`, `--win-shortcut`, `--win-upgrade-uuid` |
| `macos-latest` | `mac` | `dmg` | `.icns` | `--mac-package-identifier` |
| `ubuntu-latest` | `linux` | `deb` | `.png` | `--linux-deb-maintainer` |

> **Cross-compilation limitation**: jpackage cannot produce a Windows installer on a Linux runner. Each platform must be built on its corresponding OS runner. This is why the matrix strategy is essential.

### 4.4 Full Workflow Template

See `templates/ci/github-actions.yml` for a complete, production-ready GitHub Actions workflow that includes:
- Multi-platform matrix (windows, macos, ubuntu)
- JDK 21 setup with Maven cache
- Compile verification step
- Test verification with Monocle headless mode
- jpackage packaging with platform-specific installer types
- Artifact upload

---

## 5. GitLab CI Configuration

### 5.1 Pipeline Structure

GitLab CI uses a `stages` array to define job ordering. For JavaFX projects:

```yaml
stages:
  - compile
  - test
  - package
```

### 5.2 Runner Tags

GitLab CI uses **tagged runners** to dispatch jobs to specific OS environments:

| Stage | Runner Tag | Image / Shell |
|-------|-----------|---------------|
| compile | `linux` | `maven:3.9-eclipse-temurin-21` |
| test | `linux` | `maven:3.9-eclipse-temurin-21` |
| package (windows) | `windows` | Windows shell runner |
| package (mac) | `macos` | macOS shell runner |
| package (linux) | `linux` | `maven:3.9-eclipse-temurin-21` |

### 5.3 Full Configuration Template

See `templates/ci/gitlab-ci.yml` for a complete GitLab CI template with:
- Compile stage (Maven)
- Test stage (TestFX + Monocle headless)
- Packaging stage with platform-specific jobs (Windows, macOS, Linux)
- Artifact publishing

---

## 6. Loop Orchestration Protocol Integration

### 6.1 CI as the Verify Stage

The Loop Orchestration Protocol defines the cycle: **generate → review → verify → fix → re-verify**. CI pipelines serve as the automated **verify** stage.

```
[Developer generates code] → [Reviewer reviews] → [CI pipeline verifies]
                                                        ↓ Pass
                                                    [Deliver]
                                                        ↓ Fail
                                                    [Developer fixes]
                                                        ↓
                                                    [CI re-verifies on new push]
```

### 6.2 Triggering the Loop via CI

CI pipelines trigger the loop in two ways:

1. **PR validation (generate → verify)**: When a developer pushes code (the "generate" stage output), the CI pipeline runs verification. If tests fail, the loop enters the "fix" stage — the developer consumes the fix handoff report, applies fixes, and pushes again, triggering a new CI run (re-verify).

2. **Nightly full verification (re-verify)**: A scheduled nightly pipeline runs the complete verification suite (compile + all tests + packaging) to catch regressions that may have slipped through PR-level checks.

### 6.3 CI-to-Loop State Bridge

CI results can be mapped to the `.loop-state.json` structure defined in the Loop Orchestration Protocol:

```json
{
  "loop_id": "uuid-v4",
  "project": "javafx-app",
  "current_round": 2,
  "rounds": [
    {
      "round": 1,
      "phase": "verify",
      "runner_result": {
        "conclusion": "Fail",
        "compile": "pass",
        "runtime": "fail",
        "packaging": "skipped"
      },
      "ci_run_id": "github-actions-run-12345",
      "ci_run_url": "https://github.com/owner/repo/actions/runs/12345"
    }
  ],
  "convergence_trend": [3, 1],
  "next_action": "fix"
}
```

### 6.4 Convergence Detection in CI

The Loop Orchestration Protocol defines convergence detection: "Compare current round issue count with previous round." In CI, this translates to:

- **Round N issues** = Number of failing tests / packaging errors in CI run N
- **Converging** = Round N issues < Round N-1 issues
- **Non-converging** = 2 consecutive rounds where issue count does not decrease → pause loop, report to user

CI pipelines support this by producing structured test reports (JUnit XML) that can be compared across runs:

```bash
# Generate JUnit XML report (Maven Surefire does this by default)
mvn -B test --no-transfer-progress
# Reports are in target/surefire-reports/*.xml
```

### 6.5 Max Rounds Enforcement

The protocol limits the fix→verify cycle to 3 rounds. In a CI-driven loop, this can be enforced by:

1. Tracking round count in `.loop-state.json` (committed or stored as CI artifact)
2. Using CI job conditions: `if: ${{ round < 3 }}` (GitHub) or `rules: if: $ROUND < 3` (GitLab)
3. If max rounds reached without pass → CI reports failure, loop pauses for user intervention

---

## 7. Example Workflows

### 7.1 PR Validation Workflow

**Purpose**: Fast feedback on every pull request. Runs compile + tests (headless) on Linux only. No packaging (too slow for PR iteration).

**Trigger**: `pull_request` events targeting `main` or `develop` branches.

**Jobs**:
1. `compile` — `mvn -B compile` on Ubuntu, JDK 21
2. `test` — `mvn -B test` with Monocle headless mode (depends on compile)

**Typical duration**: 3-5 minutes (with Maven cache).

```yaml
# Simplified PR validation (see full template for details)
name: PR Validation
on:
  pull_request:
    branches: [main, develop]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - run: mvn -B test
```

**Loop role**: This is the "verify" step after the initial "generate". If it fails, the developer enters "fix" mode and pushes again.

### 7.2 Release Packaging Workflow

**Purpose**: Build platform-specific native installers for a release. Triggered by Git tags (`v*`).

**Trigger**: `push` to tags matching `v*`, or `workflow_dispatch` (manual).

**Jobs**:
1. `package` — Matrix build across `windows-latest`, `macos-latest`, `ubuntu-latest`
2. Each job: compile → create jlink runtime → jpackage → upload artifact
3. Optional: create GitHub Release with uploaded installers

**Typical duration**: 10-20 minutes per platform (parallel).

```yaml
# Simplified release packaging (see full template for details)
name: Release
on:
  push:
    tags: ['v*']
jobs:
  package:
    strategy:
      matrix:
        os: [windows-latest, macos-latest, ubuntu-latest]
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - run: mvn -B clean package -DskipTests
      # ... jpackage steps per platform ...
      - uses: actions/upload-artifact@v4
        with:
          name: installer-${{ matrix.os }}
          path: dist/*
```

**Loop role**: This runs after the loop has passed (reviewer + runner both Pass). It is the delivery stage.

### 7.3 Nightly Full Verification Workflow

**Purpose**: Comprehensive verification to catch regressions. Runs the complete suite: compile + all tests + packaging smoke test, across all platforms and JDK versions.

**Trigger**: `schedule` with cron expression (e.g., `0 2 * * *` = 2:00 AM UTC daily).

**Jobs**:
1. `compile-matrix` — Compile on all 3 OSes × 2 JDK versions (21, 23)
2. `test-matrix` — Full test suite with Monocle on all platforms
3. `package-smoke` — jpackage build on all platforms (no release, just verify packaging works)
4. `report` — Aggregate results, post to Slack/email if failures

**Typical duration**: 20-40 minutes (parallel matrix).

```yaml
# Simplified nightly verification (see full template for details)
name: Nightly
on:
  schedule:
    - cron: '0 2 * * *'
  workflow_dispatch:
jobs:
  verify:
    strategy:
      matrix:
        os: [windows-latest, macos-latest, ubuntu-latest]
        jdk: ['21', '23']
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: ${{ matrix.jdk }}
          cache: maven
      - run: mvn -B clean verify
      # ... jpackage smoke test ...
```

**Loop role**: This is the "re-verify" step for the nightly cycle. If a regression is detected (e.g., a previously passing test now fails), it signals the loop to re-enter the "fix" stage.

---

## 8. CI/CD Best Practices for JavaFX

### 8.1 Caching

| Cache Target | Key | Path | Purpose |
|-------------|-----|------|---------|
| Maven repository | `{{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}` | `~/.m2/repository` | Avoid re-downloading dependencies |
| Gradle | `{{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}` | `~/.gradle/caches` | Same for Gradle projects |
| JavaFX SDK | `javafx-sdk-21.0.11-${{ matrix.platform }}` | `javafx-sdk/` | Avoid re-downloading SDK for packaging |

### 8.2 Version Consistency

- Extract the application version from the Git tag (`${{ github.ref_name }}`) and pass it to `jpackage --app-version`
- Ensure `pom.xml` `<version>`, `module-info.java`, and `jpackage --app-version` all use the same version string
- For SNAPSHOT builds, append the Git short SHA: `1.0.0-${{ github.sha }}`

### 8.3 Platform-Specific Considerations

| Platform | CI Gotcha | Solution |
|----------|-----------|----------|
| Windows | WiX not in PATH | Add `$HOME/.dotnet/tools` to `GITHUB_PATH` |
| macOS | Signing requires certificates | Store as GitHub Secrets / GitLab CI Variables |
| Linux | Missing fonts (tofu) | `apt-get install fonts-dejavu fonts-noto-cjk` |
| Linux | Missing OpenGL libs | `apt-get install libgl1-mesa-glx` |
| All | JavaFX 24+ native access | Pass `--enable-native-access=javafx.graphics` everywhere |

### 8.4 Artifact Naming

Use consistent artifact names that include platform and version:

```
MyApp-1.0.0-windows.msi
MyApp-1.0.0-macos.dmg
MyApp-1.0.0-linux.deb
```

### 8.5 Pipeline Security

1. **Secrets management**: Store signing certificates, API tokens, and passwords in GitHub Secrets or GitLab CI/CD Variables
2. **Dependency scanning**: Add `dependency-check` or Dependabot for vulnerability scanning
3. **Code signing**: Sign Windows installers with `signtool`, macOS with `codesign` + notarization
4. **Minimal permissions**: Use `permissions: contents: read` by default in GitHub Actions

---

## 9. Template File Reference

| Template | Path | Description |
|----------|------|-------------|
| GitHub Actions workflow | `templates/ci/github-actions.yml` | Multi-platform CI/CD pipeline for JavaFX |
| GitLab CI configuration | `templates/ci/gitlab-ci.yml` | GitLab equivalent with tagged runners |

### 9.1 Using the Templates

1. **Copy**: Copy the template file to your project's `.github/workflows/` (GitHub) or repository root (GitLab)
2. **Customize**: Replace placeholder values (`MY_APP`, `com.example.myapp`, version strings) with your project's actual values
3. **Configure secrets**: Add signing certificates and API tokens as repository secrets
4. **Test**: Trigger the workflow manually (`workflow_dispatch`) before enabling automatic triggers

### 9.2 Placeholder Reference

The templates use the following placeholders that you must replace:

| Placeholder | Meaning | Example Value |
|-------------|---------|---------------|
| `MY_APP` | Application display name | `MyApp` |
| `myapp` | Application lowercase name (Linux package) | `myapp` |
| `com.example.myapp` | Java module name | `com.example.myapp` |
| `com.example.myapp.MainApp` | Main class fully qualified name | `com.example.myapp.MainApp` |
| `1.0.0` | Application version | `1.0.0` |
| `com.mycompany.myapp` | macOS bundle identifier | `com.mycompany.myapp` |
| `dev@mycompany.com` | Linux deb maintainer email | `dev@mycompany.com` |
| `12345678-1234-1234-1234-123456789abc` | Windows upgrade UUID | Generate a new UUID |

---

## 10. Troubleshooting CI Issues

### Q1: TestFX tests fail on Linux CI with "HeadlessException"

**Cause**: Monocle not on the test classpath, or headless properties not set.

**Solution**: Ensure `org.testfx:openjfx-monocle` dependency is present and Surefire `argLine` includes `-Dtestfx.headless=true -Dprism.order=sw`.

### Q2: jpackage fails with "jlink not found" or "no runtime image"

**Cause**: jpackage requires a runtime image (via jlink) or module path with JavaFX SDK.

**Solution**: Either run `jlink` first and pass `--runtime-image`, or provide `--module-path` with JavaFX SDK lib directory.

### Q3: jpackage fails on Windows with "wix not found"

**Cause**: WiX Toolset 4.x not installed or not in PATH.

**Solution**: Install via `dotnet tool install --global wix` and add `$HOME/.dotnet/tools` to PATH. WiX 3.x is not compatible with modern jpackage.

### Q4: macOS build fails with "code signing failed"

**Cause**: Signing certificate or keychain not configured on the CI runner.

**Solution**: Import the signing certificate from secrets into the macOS keychain, or skip signing for nightly builds and only sign for releases.

### Q5: Tests pass locally but fail on CI (timing issues)

**Cause**: TestFX tests may be timing-sensitive; CI runners are often slower.

**Solution**: Add explicit waits in tests using `Thread.sleep()` or `Awaitility`. Increase TestFX timeout via `-Dtestfx.timeout=10000` (10 seconds).

### Q6: JavaFX 24+ tests fail with "IllegalCallerException"

**Cause**: The `--enable-native-access=javafx.graphics` flag is missing from the test JVM arguments.

**Solution**: Add the flag to Surefire `argLine` in `pom.xml`. See `templates/test/pom-test-dependencies.xml` for the complete configuration.

### Q7: Maven cache not working in GitHub Actions

**Cause**: Cache key does not match, or cache was evicted.

**Solution**: Use `actions/setup-java` with `cache: maven` parameter (simpler than manual `actions/cache`). Ensure `pom.xml` is committed before the cache step runs.

### Q8: GitLab CI packaging jobs stuck in "pending"

**Cause**: No runner with the required tags (`windows`, `macos`) is registered.

**Solution**: Register self-hosted runners for Windows and macOS, or use GitLab SaaS runners with the appropriate tags. Linux packaging can use shared Docker runners.
