# Packaging Verification Rules

This document is the criteria for the "Packaging Verification" dimension, governing 11 check items. It executes `mvn package` to generate a JAR, then executes `jpackage` to generate a native installer, verifying the packaging flow and artifact integrity. Default severity baseline: Major. Shares the same origin as `javafx-developer`'s Packaging chapter - jpackage command.

> **Core Principle**: A project that compiles and runs in the IDE is not necessarily deliverable. This dimension verifies that the JAR build succeeds, that the module path is complete, that the main class/module is correctly configured, that the platform toolchain is present, and that the final installer can be generated and is non-empty. Only when packaging produces a usable installer can the project be delivered to end users.

> **Cross-platform validation scope**: `jpackage` cannot cross-compile, so actual packaging (Check Items 1-8) can only be executed and verified on the **current** platform. For the other two platforms, verification is limited to **configuration completeness** (Check Items 9-11) — confirming via configuration inspection that each platform's jpackage command, icon, and metadata are present and well-formed. These configuration-only checks are recorded as Major when incomplete, with the note "validated by configuration completeness only; not executed on current platform". See `javafx-developer`'s `references/cross-platform-packaging.md` for the per-platform options matrix.

---

## Check Item 1: JAR Build

**Focus**: Whether `mvn package` can successfully generate an executable JAR, whether the JAR contains all necessary JavaFX module dependencies.

**Pass Criteria**:
- `mvn package -DskipTests` exits with code 0, the `target/` directory contains a JAR file
- The JAR size is reasonable (not 0 bytes, not unreasonably small)
- The JAR's `META-INF/MANIFEST.MF` contains the `Main-Class` attribute (for non-modular projects) or the module is correctly defined (for modular projects)
- JavaFX module dependencies are either bundled in the JAR (fat JAR / shade plugin) or referenced via the module path at runtime

**Fail Criteria** (any one constitutes failure):
- `mvn package` fails with `BUILD FAILURE`, no JAR is generated in `target/`
- A JAR is generated but is 0 bytes or missing the `Main-Class` attribute
- The JAR is missing JavaFX dependencies, running it with `java -jar` reports `Error: JavaFX runtime components are missing`

**Severity Baseline**: Critical (cannot be de-escalated; packaging flow is broken)

**Anti-pattern**:
```xml
<!-- pom.xml without shade/assembly plugin for a non-modular project -->
<!-- java -jar app.jar fails with "JavaFX runtime components are missing" -->
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-jar-plugin</artifactId>
            <version>3.3.0</version>
            <configuration>
                <archive>
                    <manifest>
                        <mainClass>com.example.App</mainClass>
                    </manifest>
                </archive>
            </configuration>
        </plugin>
        <!-- Missing shade plugin to bundle JavaFX deps -->
    </plugins>
</build>
```

Runtime output:
```
Error: JavaFX runtime components are missing, and are required to run this application
```

**Best Practice**:
```xml
<!-- Use the JavaFX Maven plugin for modular projects, or shade plugin for fat JARs -->
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>0.0.8</version>
    <configuration>
        <mainClass>com.example.app/com.example.app.App</mainClass>
    </configuration>
</plugin>
```

---

## Check Item 2: Module Path Integrity

**Focus**: Whether jpackage's `--module-path` includes the JavaFX SDK `lib` directory, whether `--add-modules` lists all required modules.

**Pass Criteria**:
- The `--module-path` includes the JavaFX SDK `lib` directory (or the Maven-resolved JavaFX modules)
- `--add-modules` lists all JavaFX modules the application uses (`javafx.controls`, `javafx.fxml`, `javafx.graphics`, etc.)
- The module path does not contain duplicate or conflicting module versions
- For Maven-based packaging, the `javafx-maven-plugin` resolves the JavaFX modules automatically

**Fail Criteria** (any one constitutes failure):
- `--add-modules` is missing `javafx.controls` or `javafx.fxml`, the generated installer reports `Module javafx.controls not found` at runtime
- `--module-path` points to a non-existent or wrong JavaFX SDK path
- Conflicting JavaFX versions on the module path cause `LayerInstantiationException`

**Severity Baseline**: Major
- Escalation condition: `module-path` misconfiguration causing the generated artifact to be unrunnable -> Critical

**Anti-pattern**:
```bash
# jpackage missing --add-modules, the installer cannot find JavaFX modules at runtime
jpackage --name myapp --input target --main-jar app.jar \
  --main-class com.example.App
# Missing: --module-path $PATH_TO_FX/lib --add-modules javafx.controls,javafx.fxml
```

Runtime output (when running the generated installer):
```
Error: Module javafx.controls not found, required by com.example.app
```

**Best Practice**:
```bash
# Include JavaFX module path and all required modules
jpackage --name myapp --input target --main-jar app.jar \
  --module-path $PATH_TO_FX/lib \
  --add-modules javafx.controls,javafx.fxml,javafx.graphics \
  --main-class com.example.App
```

---

## Check Item 3: Main Class and Main Module

**Focus**: Whether `--main-class` and `--main-module` (for modular projects) correctly point to the application entry point.

**Pass Criteria**:
- For modular projects, `--main-module com.example.app` and `--main-class com.example.app.App` both point to the actual module and main class
- For non-modular projects, `--main-class com.example.App` points to the class with the `main()` method
- The main class declared in `module-info.java` and `pom.xml` are consistent
- The main class has a valid `public static void main(String[] args)` method (or extends `Application`)

**Fail Criteria** (any one constitutes failure):
- `--main-class` points to a non-existent class, the installer fails to launch
- `--main-module` does not match the module name in `module-info.java`
- The main class extends `Application` but has no `main()` method and `--main-class` is used without the JavaFX launcher awareness
- A typo in the module or class name causes `Error: Module com.example.app not found` or `Main class not found`

**Severity Baseline**: Major
- Escalation condition: Misconfiguration causing the generated artifact to be unrunnable -> Critical

**Anti-pattern**:
```bash
# --main-module does not match module-info.java (actual module is com.example.app)
jpackage --name myapp --module-path $PATH_TO_FX/lib \
  --add-modules javafx.controls,javafx.fxml \
  --module com.example.MyApp/com.example.app.App   # wrong module name
```

Runtime output:
```
Error: Module com.example.MyApp not found
```

**Best Practice**:
```bash
# Module name matches module-info.java, class name matches the actual main class
jpackage --name myapp --module-path $PATH_TO_FX/lib \
  --add-modules javafx.controls,javafx.fxml \
  --module com.example.app/com.example.app.App
```

---

## Check Item 4: Native Access Configuration

**Focus**: Whether `--java-options "--enable-native-access=javafx.graphics"` is included in the packaging configuration (JavaFX 24+).

**Pass Criteria**:
- For JavaFX 24+ projects, `--java-options "--enable-native-access=javafx.graphics"` is included in the jpackage command
- The generated installer launches the application with the native access flag enabled
- For JavaFX 17/21 projects, this check is skipped (native access is not required)

**Fail Criteria** (any one constitutes failure):
- A JavaFX 24+ project's jpackage command omits `--java-options "--enable-native-access=javafx.graphics"`
- The generated installer launches without the flag, and the application reports `IllegalAccessError` on startup
- The flag is present but the module name is misspelled (e.g., `javafx.graphics` vs `javafxGraphics`)

**Severity Baseline**: Critical (cannot be de-escalated; runtime will always report `IllegalAccessError`)

**Anti-pattern**:
```bash
# JavaFX 24+ packaging without --enable-native-access
jpackage --name myapp --module-path $PATH_TO_FX/lib \
  --add-modules javafx.controls,javafx.fxml,javafx.graphics \
  --module com.example.app/com.example.app.App
# Missing: --java-options "--enable-native-access=javafx.graphics"
```

**Best Practice**:
```bash
# Include --java-options for native access (JavaFX 24+)
jpackage --name myapp --module-path $PATH_TO_FX/lib \
  --add-modules javafx.controls,javafx.fxml,javafx.graphics \
  --module com.example.app/com.example.app.App \
  --java-options "--enable-native-access=javafx.graphics"
```

---

## Check Item 5: Platform Toolchain

**Focus**: Whether Windows has Inno Setup (exe) or WiX Toolset 4.x (msi) installed; whether macOS has Xcode command line tools; whether Linux has `dpkg-deb` or `rpm-build`.

**Pass Criteria**:
- Windows: Inno Setup 6+ is installed (for `.exe` installers) or WiX Toolset 4.x (for `.msi`), and the tool is on the `PATH`
- macOS: Xcode command line tools are installed (`xcode-select -p` returns a path), `pkgbuild` / `productbuild` are available
- Linux: `dpkg-deb` is installed (for `.deb`) or `rpm-build` (for `.rpm`), on the `PATH`
- jpackage can locate the toolchain and proceed without a "toolchain not found" error

**Fail Criteria** (any one constitutes failure):
- Windows: jpackage fails with `Exception: jpackage failed ... Cannot find Inno Setup` or `Cannot find WiX toolset`
- macOS: jpackage fails with `Cannot find xcodebuild`
- Linux: jpackage fails with `Cannot find dpkg-deb` or `Cannot find rpmbuild`
- The toolchain is installed but not on the `PATH`, causing jpackage to fail to locate it

**Severity Baseline**: Major
- De-escalation condition: Toolchain not installed (environment issue, not a code issue) -> Info

**Anti-pattern**:
```bash
# Windows: packaging without Inno Setup installed
jpackage --name myapp --input target --main-jar app.jar \
  --main-class com.example.App --type exe
```

Runtime output:
```
Exception in thread "main" jdk.jpackage.internal.PackagerException:
  jpackage failed. Cannot find Inno Setup compiler (ISCC.exe)
```

**Best Practice**:
```bash
# Verify toolchain before packaging
# Windows: install Inno Setup 6 and add to PATH
#   iscc /?   (verify Inno Setup Compiler is available)
# macOS: install Xcode command line tools
#   xcode-select --install
# Linux: install dpkg-deb or rpmbuild
#   sudo apt install dpkg-dev   (Debian/Ubuntu)
#   sudo dnf install rpm-build  (Fedora/RHEL)

# Then package
jpackage --name myapp --input target --main-jar app.jar \
  --main-class com.example.App --type exe \
  --win-upgrade-uuid 12345678-1234-1234-1234-123456789abc
```

---

## Check Item 6: Icon Format

**Focus**: Whether the Windows icon is `.ico` (multi-size embedded), whether macOS is `.icns`, whether Linux is `.png`.

**Pass Criteria**:
- Windows: the icon file is `.ico` format with multiple sizes embedded (16x16, 32x32, 48x48, 256x256)
- macOS: the icon file is `.icns` format
- Linux: the icon file is `.png` format
- The icon file exists at the declared path and is not corrupt

**Fail Criteria** (any one constitutes failure):
- Windows: using a `.png` instead of `.ico`, jpackage fails or the installer displays a default icon
- macOS: using a `.png` instead of `.icns`, jpackage fails to generate the `.app` bundle icon
- The icon file does not exist at the declared `--icon` path
- The icon file is corrupt or has an unsupported format

**Severity Baseline**: Major (incorrect icon format causes packaging failure or default icon)

**Anti-pattern**:
```bash
# Windows: passing a .png icon to jpackage
jpackage --name myapp --input target --main-jar app.jar \
  --main-class com.example.App --type exe \
  --icon src/main/resources/icons/logo.png   # .png not supported on Windows
```

Runtime output:
```
Exception in thread "main" jdk.jpackage.internal.PackagerException:
  jpackage failed. Icon file must be .ico format on Windows
```

**Best Practice**:
```bash
# Use the correct icon format per platform
# Windows
jpackage --name myapp --input target --main-jar app.jar \
  --main-class com.example.App --type exe \
  --icon src/main/resources/icons/app.ico

# macOS
jpackage --name myapp --input target --main-jar app.jar \
  --main-class com.example.App --type dmg \
  --icon src/main/resources/icons/app.icns

# Linux
jpackage --name myapp --input target --main-jar app.jar \
  --main-class com.example.App --type deb \
  --icon src/main/resources/icons/app.png
```

---

## Check Item 7: Installer Generation

**Focus**: Whether `jpackage` can successfully generate installer files, whether the artifact size is reasonable (not 0 bytes).

**Pass Criteria**:
- jpackage exits with code 0, the output directory contains the installer file (`.exe` / `.msi` / `.dmg` / `.deb` / `.rpm`)
- The installer file size is reasonable (not 0 bytes; typically several MB for a JavaFX application)
- The installer can be launched and the application installs without error
- The installed application launches and runs correctly

**Fail Criteria** (any one constitutes failure):
- jpackage exits with a non-zero code, no installer file is generated
- An installer file is generated but is 0 bytes or unreasonably small (incomplete generation)
- The installer is generated but fails to install (corrupt installer)
- The installed application fails to launch

**Severity Baseline**: Major
- Escalation condition: `module-path` misconfiguration causing the generated artifact to be unrunnable -> Critical

**Anti-pattern**:
```bash
# jpackage fails partway, generates a 0-byte installer
jpackage --name myapp --input target --main-jar app.jar \
  --main-class com.example.App --type msi
# Output: BUILD FAILED, target/myapp.msi is 0 bytes
```

Runtime output:
```
Exception in thread "main" jdk.jpackage.internal.PackagerException:
  jpackage failed. Fatal error: WiX candle.exe returned non-zero exit code
```

**Best Practice**:
```bash
# Verify prerequisites, then package; check the output file size after generation
jpackage --name myapp --input target --main-jar app.jar \
  --main-class com.example.App --type msi \
  --win-upgrade-uuid 12345678-1234-1234-1234-123456789abc \
  --icon src/main/resources/icons/app.ico

# Verify the artifact
ls -la target/myapp.msi   # size should be > 0
```

---

## Check Item 8: Upgrade UUID

**Focus**: Whether Windows packaging includes a valid `--win-upgrade-uuid` (UUID v4 format).

**Pass Criteria**:
- Windows `.exe` / `.msi` packaging includes `--win-upgrade-uuid` with a valid UUID v4 format (`xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx`)
- The same UUID is reused across versions of the same application (enables upgrade instead of side-by-side install)
- The UUID is unique to this application (not a copy-pasted sample UUID)

**Fail Criteria** (any one constitutes failure):
- Windows packaging omits `--win-upgrade-uuid`, jpackage generates a random UUID each build (breaks upgrade flow)
- The UUID is not in valid v4 format (e.g., missing hyphens, wrong version digit)
- The same UUID is reused across different applications (conflict in the Windows installer registry)

**Severity Baseline**: Major (missing UUID breaks the upgrade flow but does not prevent installation)

**Anti-pattern**:
```bash
# Windows packaging without --win-upgrade-uuid
jpackage --name myapp --input target --main-jar app.jar \
  --main-class com.example.App --type exe
# Each build gets a random UUID; users cannot upgrade, must uninstall first
```

**Best Practice**:
```bash
# Generate a stable UUID once and reuse it across versions
#   Generate: uuidgen (Linux/macOS) or [guid]::NewGuid() (PowerShell)
jpackage --name myapp --input target --main-jar app.jar \
  --main-class com.example.App --type exe \
  --win-upgrade-uuid 12345678-1234-4234-8234-123456789abc \
  --icon src/main/resources/icons/app.ico
```

```properties
# Or store in jpackage-config.properties for reuse
win-upgrade-uuid=12345678-1234-4234-8234-123456789abc
```

---

## Check Item 9: Cross-Platform Configuration Completeness

**Focus**: Whether the project defines jpackage commands/configuration for **all three** desktop platforms (Windows `msi`/`exe`, macOS `dmg`/`pkg`, Linux `deb`/`rpm`).

**Pass Criteria**:
- The packaging configuration (`jpackage-config.properties`, build scripts, or CI workflow matrix) defines a jpackage command for Windows, macOS, and Linux
- For each non-current platform, the configuration specifies an output `--type` appropriate to that platform
- For each non-current platform, the referenced icon and platform-specific metadata are present (validated by configuration inspection, not execution)

**Fail Criteria** (any one constitutes failure):
- Only one or two platforms are configured (e.g., only Windows `msi`; macOS and Linux configs are missing)
- A platform's output type is mismatched (e.g., macOS config requests `--type msi`, which cannot be built on macOS)
- The CI workflow has no matrix dimension for one of the three operating systems

**Severity Baseline**: Major (incomplete multi-platform config means the CI matrix will fail on the missing platform)
- De-escalation condition: The project explicitly targets a single platform (documented intent) -> Info

**Verification Method**: Configuration inspection only — `jpackage` cannot cross-compile, so non-current platforms cannot be executed. Inspect `jpackage-config.properties` / build scripts / CI workflow. Record as "validated by configuration completeness only; not executed on current platform".

**Anti-pattern**:
```properties
# jpackage-config.properties — only Windows defined
win-type=msi
win-upgrade-uuid=12345678-1234-4234-8234-123456789abc
# Missing: mac-type, mac-package-identifier, linux-type, linux-deb-maintainer
```

**Best Practice**:
```properties
# jpackage-config.properties — all three platforms defined
win-type=msi
win-upgrade-uuid=12345678-1234-4234-8234-123456789abc
mac-type=dmg
mac-package-identifier=com.mycompany.myapp
linux-type=deb
linux-deb-maintainer=dev@mycompany.com
```

---

## Check Item 10: Platform-Specific Icon Format Validation

**Focus**: Whether each platform's icon reference points to the correct format and the file exists in the repository — Windows `.ico`, macOS `.icns`, Linux `.png`.

**Pass Criteria**:
- Windows config references an `.ico` file (multi-size embedded) that exists in the repository
- macOS config references an `.icns` file that exists in the repository
- Linux config references a `.png` file (>= 512x512 recommended) that exists in the repository
- For the current platform, the icon loads successfully during actual jpackage execution (executable check)
- For non-current platforms, the icon file exists and has the correct extension (configuration inspection)

**Fail Criteria** (any one constitutes failure):
- Windows config references `.png` instead of `.ico`
- macOS config references `.png` instead of `.icns`
- Linux config references `.ico` instead of `.png`
- The referenced icon file does not exist in the repository (would fail the target platform's CI build)
- The icon file is corrupt or has an unsupported format

**Severity Baseline**: Major (wrong-format or missing icon fails that platform's packaging build)
- For the current platform: Escalation condition -> if jpackage fails outright, follows Check Item 6/7 severity

**Verification Method**: Current platform — executable (jpackage succeeds with the icon). Non-current platforms — configuration inspection (file exists + correct extension).

**Anti-pattern**:
```bash
# macOS config references a .png icon — jpackage on macOS will fail
jpackage --type dmg --name MyApp --icon assets/icons/app.png ...
```

**Best Practice**:
```bash
# Each platform references its native icon format
# Windows
--icon assets/icons/app.ico
# macOS
--icon assets/icons/app.icns
# Linux
--icon assets/icons/app.png
```

> See `javafx-developer`'s `references/cross-platform-packaging.md` (Section 3) for the icon format specifications and conversion guide.

---

## Check Item 11: Platform-Specific Metadata

**Focus**: Whether each platform's required metadata is present and well-formed — Windows `--win-upgrade-uuid`, macOS `--mac-package-identifier`, Linux `--linux-deb-maintainer` and `--linux-package-name`.

**Pass Criteria**:
- Windows: `--win-upgrade-uuid` is present and in valid UUID v4 format (`xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx`); the UUID is stable/reused across versions of the same application
- macOS: `--mac-package-identifier` is present in reverse-DNS format (e.g., `com.mycompany.myapp`) and matches the signing/notarization profile
- Linux: `--linux-deb-maintainer` is a valid email address; `--linux-package-name` is a lowercase, valid package name
- For the current platform, metadata is validated during actual jpackage execution; for non-current platforms, validated by configuration inspection

**Fail Criteria** (any one constitutes failure):
- Windows `--win-upgrade-uuid` is missing, not in UUID v4 format, or copy-pasted from a sample (conflicts with another app)
- macOS `--mac-package-identifier` is missing, not in reverse-DNS format, or doesn't match the Developer ID signing profile
- Linux `--linux-deb-maintainer` is missing or not a valid email
- `--linux-package-name` is missing or contains invalid characters (uppercase, spaces)

**Severity Baseline**: Major (missing/malformed metadata causes the target platform's packaging to fail or produces a non-distributable artifact)
- De-escalation condition: Single-platform project where the metadata for the single target is complete -> Info for the non-target platforms

**Verification Method**: Current platform — executable (jpackage accepts the metadata). Non-current platforms — configuration inspection (metadata present + format well-formed).

**Anti-pattern**:
```bash
# macOS without --mac-package-identifier; Linux without --linux-deb-maintainer
jpackage --type dmg --name MyApp ...   # missing: --mac-package-identifier com.mycompany.myapp
jpackage --type deb --name myapp ...   # missing: --linux-deb-maintainer dev@mycompany.com
```

**Best Practice**:
```bash
# Windows
jpackage --type msi ... --win-upgrade-uuid "12345678-1234-4234-8234-123456789abc"
# macOS
jpackage --type dmg ... --mac-package-name "MyApp" --mac-package-identifier "com.mycompany.myapp"
# Linux
jpackage --type deb ... --linux-package-name "myapp" --linux-deb-maintainer "dev@mycompany.com"
```

> Note: For non-current platforms, these metadata checks are "validated by configuration completeness only; not executed on current platform". See `javafx-developer`'s `references/cross-platform-packaging.md` (Section 1 and Section 6) for the per-platform options matrix and the cross-platform packaging checklist.
