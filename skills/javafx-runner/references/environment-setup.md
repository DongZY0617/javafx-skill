# Environment Detection and Monocle Headless Configuration

This document describes the environment detection logic performed in Step 1 of the runner workflow, and how to configure the Monocle headless framework for runtime verification in CI environments without a display. All verification dimensions depend on accurate environment detection to select the correct commands and verification items.

---

## 1. JDK Version Detection

**Detection Command**: `java -version`

**Detection Logic**:
- Run `java -version` and parse the version string from stderr (JDK 17+ writes version info to stderr by default)
- Extract the major version number (e.g., `17`, `21`, `24`)
- Confirm the major version meets the JavaFX minimum requirement (JavaFX 17 requires JDK 17+)

**Pass Criteria**:
- `java -version` executes successfully and outputs a recognizable version string
- The major version is >= 17

**Fail Handling**:
- If `java` is not on the PATH, record "JDK not found" and abort verification (cannot compile or run without a JDK)
- If the version is below 17, record "JDK version too low, requires 17+" and abort

**Example Output**:
```
openjdk version "21.0.2" 2024-01-16
OpenJDK Runtime Environment (build 21.0.2+13-58)
OpenJDK 64-Bit Server VM (build 21.0.2+13-58, mixed mode, sharing)
```

---

## 2. Build Tool Detection

**Detection Logic**:
- Check for the existence of `pom.xml` in the project root directory -> Maven project
- Check for the existence of `build.gradle` (or `build.gradle.kts`) in the project root directory -> Gradle project
- If both exist, prefer Maven (or confirm with the user)
- If neither exists, record "build tool not found, cannot verify" and abort

**Command Selection**:

| Build Tool | Compile Command | Run Command | Package Command |
|------------|-----------------|-------------|-----------------|
| Maven | `mvn compile -q` | `mvn javafx:run` | `mvn package -DskipTests` |
| Gradle | `gradle compileJava --quiet` | `gradle run` | `gradle build -x test` |

---

## 3. JavaFX Version Detection

**Detection Logic**:
- **Maven**: Parse the `<dependency>` entries in `pom.xml`, look for `org.openjfx:javafx-controls` / `javafx-fxml`, extract the `<version>` tag
- **Gradle**: Parse the `build.gradle` `javafx` plugin block or `implementation` dependencies, extract the JavaFX version
- If no JavaFX dependency is declared, record "JavaFX dependency not found, not a JavaFX project" and abort

**Version-Specific Verification Adjustments**:

| JavaFX Version | Minimum JDK | Additional Verification |
|----------------|-------------|--------------------------|
| 17 (LTS) | 17 | None |
| 21 (LTS) | 17 | None |
| 24 | 21 | Check `--enable-native-access=javafx.graphics` |
| 25 | 21 | Check `--enable-native-access=javafx.graphics` |
| 26 | 21 | Check `--enable-native-access=javafx.graphics` |

> **Key Rule**: For JavaFX 24+ projects, the runtime and packaging verification must check whether `--enable-native-access=javafx.graphics` is configured; for JavaFX 17/21, this check is skipped.

---

## 4. Modularity Detection

**Detection Logic**:
- Check for the existence of `src/main/java/module-info.java`
- If present, the project is a modular project; if absent, it is a non-modular project

**Impact on Verification**:

| Project Type | Compile | Run | Package |
|--------------|---------|-----|---------|
| Modular | Check `requires` / `exports` / `opens` completeness | Check runtime reflection access | jpackage uses `--module` and `--main-module` |
| Non-modular | Skip module config check | Check classpath dependencies | jpackage uses `--main-class` and `--main-jar` |

---

## 5. Display Detection

**Detection Logic**:
- **Linux**: Check the `DISPLAY` environment variable; if set (e.g., `:0`), a display is available; if unset, headless mode is required
- **Windows**: Check for an interactive desktop session (`SESSIONNAME` environment variable or the existence of an explorer.exe process); Windows CI runners without a desktop session require headless mode
- **macOS**: Check for a WindowServer session; CI runners (e.g., GitHub Actions macOS) typically have a display, but verify

**Decision Matrix**:

| Environment | Display Available | Run Command |
|-------------|-------------------|-------------|
| Local desktop (Windows/macOS/Linux) | Yes | `mvn javafx:run` |
| Linux CI (no `DISPLAY`) | No | `mvn javafx:run -Dmonocle.platform=Headless -Dprism.order=sw` |
| Windows CI (no desktop session) | No | `mvn javafx:run -Dmonocle.platform=Headless -Dprism.order=sw` |

---

## 6. Platform Toolchain Detection

**Detection Logic**:
Based on the current operating system (`os.name` system property), detect whether the jpackage toolchain is ready:

**Windows**:
- Detect Inno Setup: check whether `ISCC.exe` is on the PATH or at the default install location (`C:\Program Files (x86)\Inno Setup 6\ISCC.exe`)
- Detect WiX Toolset 4.x: check whether `candle.exe` / `light.exe` are on the PATH
- Required for `.exe` (Inno Setup) and `.msi` (WiX) installer generation

**macOS**:
- Detect Xcode command line tools: run `xcode-select -p`; if it returns a path, the tools are installed
- Required for `.dmg` and `.pkg` generation

**Linux**:
- Detect `dpkg-deb`: run `dpkg-deb --version`; if it succeeds, Debian packaging is available
- Detect `rpmbuild`: run `rpmbuild --version`; if it succeeds, RPM packaging is available
- Required for `.deb` and `.rpm` generation

**Toolchain Missing Handling**:
- If the toolchain is missing, record "toolchain not installed" and skip the corresponding installer type in packaging verification, marking the jpackage failure severity as Info (environment issue, not a code issue)
- Provide the installation command in the fix recommendation

---

## 7. Monocle Headless Configuration

Monocle is a headless implementation of the JavaFX Glass windowing toolkit, allowing JavaFX applications to run in environments without a display (e.g., CI servers). It is the standard approach for headless runtime verification.

### 7.1 Adding the Monocle Dependency

**Maven** (`pom.xml`):
```xml
<dependency>
    <groupId>org.testfx</groupId>
    <artifactId>openjfx-monocle</artifactId>
    <version>jdk-21+26</version>
    <scope>test</scope>
</dependency>
```

**Gradle** (`build.gradle`):
```groovy
testImplementation 'org.testfx:openjfx-monocle:jdk-21+26'
```

> **Version Note**: The Monocle version must match the JavaFX version family. For JavaFX 21, use `jdk-21+26`; for JavaFX 17, use `jdk-17+26`. Using a mismatched version may cause `ClassNotFoundException` at startup.

### 7.2 Headless Run Command

```bash
mvn javafx:run -Dmonocle.platform=Headless -Dprism.order=sw
```

**Parameter Explanation**:
- `-Dmonocle.platform=Headless`: Uses the Monocle headless platform instead of the native windowing system
- `-Dprism.order=sw`: Forces software rendering (no GPU required), suitable for CI environments

### 7.3 Headless Verification Pass Criteria

**Pass Criteria**:
- The application process starts in headless mode without display-related errors (`X11Display`, `Cannot open display`, etc.)
- `start()` executes to completion, no `NullPointerException` caused by missing display
- The process exits with code 0 (for smoke tests) or as expected

**Fail Criteria**:
- Output contains `X11Display: Can't open display` or `Cannot open display` (Monocle not configured)
- `ClassNotFoundException: com.sun.glass.ui.monocle.MonoclePlatform` (Monocle dependency missing)
- `-Dprism.order=sw` not set, hardware pipeline fails: `Prism - not using native acceleration`

### 7.4 Monocle Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `X11Display: Can't open display` | Monocle platform not activated | Add `-Dmonocle.platform=Headless` |
| `ClassNotFoundException: ...MonoclePlatform` | Monocle dependency missing | Add `org.testfx:openjfx-monocle` dependency |
| `Prism - not using native acceleration` warning | Software rendering not forced | Add `-Dprism.order=sw` |
| `Exception in Application start method` | Application code issue (not env) | Inspect the stack trace for the root cause |
| Headless runs locally but fails in CI | CI lacks the Monocle dependency in the build | Ensure the dependency is in the build config committed to VCS |

---

## 8. Verification Scope Declaration

After environment detection, declare the verification scope based on the user request and detection results, recorded in the report header:

- **Full Verification (default)**: Execute compile -> runtime -> packaging in sequence
- **Incremental Verification**: Only verify dimensions affected by changed files
- **Targeted Dimension Verification**: Only execute user-specified dimensions

**Scope Annotation Examples**:
- `Verification Mode: Full Verification`
- `Verification Scope: Compile Verification, Runtime Verification, Packaging Verification`
- `Environment: JDK 21 / Maven 3.9 / JavaFX 21 / OS Windows 11`
- `Modular: Yes`
- `Display: Available (local desktop) / Unavailable (CI, using Monocle headless)`
