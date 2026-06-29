# CI/CD Pipeline Generation Rules

This document defines the rules for generating Continuous Integration and Continuous Deployment pipelines for JavaFX desktop applications. Generated pipelines produce cross-platform native installers (.exe/.msi, .dmg, .deb/.rpm) via `jpackage`.

## 1. GitHub Actions Workflow Structure

### Triggers
A complete workflow must declare three trigger types:

| Trigger | Event | Purpose |
|---------|-------|---------|
| `push` | Branches `main`, `develop`, tags `v*.*.*` | Build on merge and release tags |
| `pull_request` | Branches `main`, `develop` | Validate contributions before merge |
| `workflow_dispatch` | Manual | Allow on-demand runs with inputs |

```yaml
on:
  push:
    branches: [ main, develop ]
    tags: [ 'v*.*.*' ]
  pull_request:
    branches: [ main, develop ]
  workflow_dispatch:
    inputs:
      debug_enabled:
        description: 'Enable debug logging'
        required: false
        default: 'false'
```

### Strategy Matrix
Cross-platform builds use a matrix strategy targeting all supported operating systems.

```yaml
strategy:
  fail-fast: false
  matrix:
    os: [ windows-latest, macos-latest, ubuntu-latest ]
runs-on: ${{ matrix.os }}
```

`fail-fast: false` ensures a failure on one platform does not cancel the others. For experimental platforms (e.g. `ubuntu-22.04-arm`), add `continue-on-error: true` or use `allow-failures`-style handling.

## 2. JDK Setup

Use `actions/setup-java` with the Eclipse Temurin distribution and enable Maven caching.

```yaml
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    distribution: 'temurin'
    java-version: '21'
    cache: maven
```

The `cache: maven` option automatically caches `~/.m2/repository`. For finer control, use `actions/cache` explicitly:

```yaml
- name: Cache Maven repository
  uses: actions/cache@v4
  with:
    path: ~/.m2/repository
    key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
    restore-keys: |
      ${{ runner.os }}-maven-
```

## 3. Build Steps

### Compile and Package
```yaml
- name: Build with Maven
  run: mvn -B clean package -DskipTests
```

### jpackage Execution
`jpackage` flags differ per platform. Use a conditional step per runner OS.

```yaml
- name: Package (Windows)
  if: runner.os == 'Windows'
  run: |
    $jpackagePath = "$env:JAVA_HOME\bin\jpackage.exe"
    & $jpackagePath --type msi --name MyApp --input target --main-jar MyApp.jar `
      --app-version ${{ github.ref_name }} --vendor "MyCompany" `
      --win-dir-chooser --win-menu --win-shortcut --win-upgrade-uuid "GUID-HERE"

- name: Package (macOS)
  if: runner.os == 'macOS'
  run: |
    jpackage --type dmg --name MyApp --input target --main-jar MyApp.jar \
      --app-version ${{ github.ref_name }} --vendor "MyCompany" \
      --mac-package-name "MyApp" --mac-package-identifier "com.mycompany.myapp"

- name: Package (Linux)
  if: runner.os == 'Linux'
  run: |
    jpackage --type deb --name myapp --input target --main-jar MyApp.jar \
      --app-version ${{ github.ref_name }} --vendor "MyCompany" \
      --linux-menu-group "Utility" --linux-shortcut
```

## 4. Artifact Upload

Upload artifacts using platform-specific glob paths.

```yaml
- name: Upload Windows artifacts
  if: runner.os == 'Windows'
  uses: actions/upload-artifact@v4
  with:
    name: myapp-windows
    path: |
      target/*.msi
      target/*.exe

- name: Upload macOS artifacts
  if: runner.os == 'macOS'
  uses: actions/upload-artifact@v4
  with:
    name: myapp-macos
    path: target/*.dmg

- name: Upload Linux artifacts
  if: runner.os == 'Linux'
  uses: actions/upload-artifact@v4
  with:
    name: myapp-linux
    path: |
      target/*.deb
      target/*.rpm
```

## 5. Platform-Specific Setup Steps

### Windows (WiX / Inno Setup)
`jpackage` for MSI requires WiX Toolset. The Windows runner already provides it, but pin the version for reproducibility.

```yaml
- name: Install WiX Toolset
  if: runner.os == 'Windows'
  run: |
    choco install wixtoolset --version=3.14.1 -y
```

### macOS (Xcode tools)
```yaml
- name: Select Xcode
  if: runner.os == 'macOS'
  run: sudo xcode-select -s /Applications/Xcode.app
```

### Linux (dpkg-deb / rpm-build)
```yaml
- name: Install packaging tools
  if: runner.os == 'Linux'
  run: |
    sudo apt-get update
    sudo apt-get install -y dpkg-dev rpm fakeroot
```

## 6. GitLab CI Configuration

GitLab uses parallel jobs with tags to select runners per platform.

```yaml
stages:
  - build
  - package
  - release

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"

cache:
  key: ${CI_COMMIT_REF_SLUG}
  paths:
    - .m2/repository

build:
  stage: build
  script:
    - mvn -B clean package -DskipTests
  artifacts:
    paths:
      - target/*.jar
    expire_in: 1 day

package:windows:
  stage: package
  tags: [ windows ]
  script:
    - jpackage --type msi --name MyApp --input target --main-jar MyApp.jar --app-version $CI_COMMIT_TAG
  artifacts:
    paths: [ "target/*.msi" ]

package:macos:
  stage: package
  tags: [ macos ]
  script:
    - jpackage --type dmg --name MyApp --input target --main-jar MyApp.jar --app-version $CI_COMMIT_TAG
  artifacts:
    paths: [ "target/*.dmg" ]

package:linux:
  stage: package
  tags: [ linux ]
  script:
    - jpackage --type deb --name myapp --input target --main-jar MyApp.jar --app-version $CI_COMMIT_TAG
  artifacts:
    paths: [ "target/*.deb", "target/*.rpm" ]
```

## 7. Complete GitHub Actions Example

```yaml
name: Build & Release
on:
  push:
    branches: [ main ]
    tags: [ 'v*.*.*' ]
  workflow_dispatch:
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
        run: ./.github/scripts/jpackage.sh
      - uses: actions/upload-artifact@v4
        with:
          name: app-${{ matrix.os }}
          path: |
            target/*.msi
            target/*.dmg
            target/*.deb
            target/*.rpm
```

## 8. Matrix Build Best Practices

- Always set `fail-fast: false` so all platforms complete independently.
- Use `continue-on-error` for experimental OS entries added to the matrix.
- Cache Maven dependencies keyed by `pom.xml` hash to maximize reuse.
- Pin tool versions (JDK, WiX, Xcode) to guarantee reproducible builds.
- Name artifacts with the OS suffix to prevent collisions across matrix jobs.
