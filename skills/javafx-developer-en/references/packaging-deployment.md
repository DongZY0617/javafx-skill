# JavaFX Packaging and Deployment Guide

This guide covers the full workflow of packaging and deploying JavaFX applications, including the jpackage tool, cross-platform packaging, jlink runtime images, icon requirements, the JavaPackager Gradle plugin, Gluon Substrate native images, CI/CD integration, code signing, and auto-update.

---

## 1. jpackage Tool Overview and Requirements

`jpackage` is a packaging tool built into JDK 14+, which can package Java applications into platform-native installers (exe/msi, dmg/pkg, deb/rpm), with an embedded custom JRE, so users do not need to pre-install Java.

### 1.1 Prerequisites

| Requirement      | Description                                                          |
|------------------|----------------------------------------------------------------------|
| JDK version      | JDK 14+ (JDK 21 LTS recommended)                                     |
| Project modularity | Recommended to use module-info.java, jpackage can combine with jlink to generate a slim runtime |
| Platform toolchain | The target packaging platform needs the corresponding tools installed (see platform-specific requirements below) |
| JavaFX SDK/JAR   | JavaFX modules need to be added to the module-path                   |

### 1.2 Additional Tool Requirements per Platform

| Platform | Required Tools                                       | Installation Method                   |
|----------|------------------------------------------------------|---------------------------------------|
| Windows  | WiX Toolset 4.x (generate msi), Inno Setup (generate exe) | `dotnet tool install --global wix` (v4+) |
| macOS    | Xcode command line tools                             | `xcode-select --install`              |
| Linux    | `dpkg-deb` (deb), `rpmbuild` (rpm)                   | `apt install dpkg rpm` / `yum install rpm-build` |

### 1.3 jpackage Basic Workflow

```
JavaFX Application (modular)
    ↓ jlink (optional, generates custom runtime)
Custom JRE Image
    ↓ jpackage
Native Installer (.exe/.msi/.dmg/.pkg/.deb/.rpm)
```

---

## 2. jpackage Complete Parameter Reference

### 2.1 Common Parameters

| Parameter                   | Description                                                       |
|-----------------------------|-------------------------------------------------------------------|
| `--type`                    | Output type: `app-image`, `exe`, `msi`, `dmg`, `pkg`, `deb`, `rpm` |
| `--name`                    | Application name                                                  |
| `--app-version`             | Application version number                                        |
| `--vendor`                  | Vendor name                                                       |
| `--description`             | Application description                                           |
| `--input` / `-i`            | Input directory (stores application JAR and resources)            |
| `--main-jar`                | Main JAR file name                                                |
| `--main-class`              | Fully qualified name of the main class                            |
| `--module` / `-m`           | Main module name (modular application): `module-name/main-class`  |
| `--module-path` / `-p`      | Module path                                                       |
| `--runtime-image`           | Pre-built runtime image path (jlink product)                      |
| `--icon`                    | Application icon file path                                        |
| `--dest` / `-d`             | Output directory                                                  |
| `--java-options`            | Parameters passed to the JVM (can be used multiple times)         |
| `--arguments`               | Command-line arguments passed to the main class                   |
| `--add-modules`             | Additional modules added to the runtime                           |
| `--jlink-options`           | Options passed to jlink                                           |
| `--license-file`            | License file path                                                 |
| `--resource-dir`            | Resource override directory (can place custom install scripts, templates, etc.) |
| `--app-content`             | Additional content directory (extra files)                        |
| `--temp`                    | Temporary working directory                                       |
| `--verbose`                 | Verbose output                                                    |
| `--about-url`               | About page URL                                                    |
| `--file-associations`       | File association configuration file                               |
| `--install-dir`             | Installation directory                                            |
| `--linux-package-name`      | Linux package name                                                |
| `--linux-deb-maintainer`    | deb maintainer email                                              |
| `--linux-rpm-license-type`  | rpm license type                                                  |
| `--mac-package-name`        | macOS package name                                                |
| `--mac-package-identifier`  | macOS package identifier                                          |
| `--mac-sign`                | macOS code signing                                                |
| `--mac-signing-key-user-name`| macOS signing key user name                                      |
| `--win-dir-chooser`         | Windows installation directory chooser                            |
| `--win-menu`                | Windows Start menu shortcut                                       |
| `--win-shortcut`            | Windows desktop shortcut                                          |
| `--win-per-user-install`    | Windows per-user install                                          |
| `--win-upgrade-uuid`        | Windows upgrade UUID (used for version upgrade identification)    |

### 2.2 Basic Command Examples

```bash
# Modular application packaging (recommended)
jpackage \
  --type app-image \
  --name MyApp \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icon.ico \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist

# Non-modular application packaging
jpackage \
  --type app-image \
  --name MyApp \
  --app-version 1.0.0 \
  --input target \
  --main-jar myapp-1.0.0.jar \
  --main-class com.example.myapp.MainApp \
  --module-path "libs" \
  --add-modules javafx.controls,javafx.fxml \
  --icon assets/icon.ico \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

---

## 3. Platform-Specific Packaging

### 3.1 Windows (exe / msi)

**Generate exe installer (requires Inno Setup):**

```bash
jpackage \
  --type exe \
  --name MyApp \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icon.ico \
  --win-menu \
  --win-shortcut \
  --win-dir-chooser \
  --win-upgrade-uuid "12345678-1234-1234-1234-123456789abc" \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

**Generate msi installer (requires WiX Toolset):**

```bash
jpackage \
  --type msi \
  --name MyApp \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icon.ico \
  --win-menu \
  --win-shortcut \
  --win-upgrade-uuid "12345678-1234-1234-1234-123456789abc" \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

> `--win-upgrade-uuid` is critical: installer packages of different versions with the same UUID can upgrade each other, the UUID must remain consistent and globally unique.

### 3.2 macOS (dmg / pkg)

```bash
jpackage \
  --type dmg \
  --name MyApp \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icon.icns \
  --mac-package-name MyApp \
  --mac-package-identifier com.mycompany.myapp \
  --mac-sign \
  --mac-signing-key-user-name "Developer ID Application: Your Name (XXXXXXXXXX)" \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

> macOS packaging must be performed on a macOS system. Use `--type app-image` to generate a `.app` application bundle, use `dmg` or `pkg` for distribution.

### 3.3 Linux (deb / rpm)

**Generate deb package:**

```bash
jpackage \
  --type deb \
  --name myapp \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icon.png \
  --linux-package-name myapp \
  --linux-deb-maintainer "dev@mycompany.com" \
  --linux-rpm-license-type "MIT" \
  --install-dir /opt/myapp \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

**Generate rpm package:**

```bash
jpackage \
  --type rpm \
  --name myapp \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icon.png \
  --linux-package-name myapp \
  --linux-rpm-license-type "MIT" \
  --install-dir /opt/myapp \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

---

## 4. jlink Custom Runtime Image

jpackage can automatically invoke jlink to generate the runtime, or you can manually generate an image with jlink first and pass it in via `--runtime-image` for finer control.

### 4.1 Manually Creating a jlink Image

```bash
jlink \
  --module-path "mods:libs" \
  --add-modules com.example.myapp \
  --output build/javafx-runtime \
  --strip-debug \
  --compress zip-6 \
  --no-header-files \
  --no-man-pages \
  --bind-services
```

### 4.2 Passing the jlink Image to jpackage

```bash
jpackage \
  --type app-image \
  --name MyApp \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

### 4.3 jlink Image Size Optimization

| Optimization Method          | Effect                                  |
|------------------------------|-----------------------------------------|
| `--strip-debug`              | Removes debug info, reduces 20-30%      |
| `--compress zip-6`           | Compresses modules, reduces 30-50%      |
| Only `--add-modules` necessary modules | Avoids including unused modules |
| `--no-header-files`          | Removes C header files                  |
| `--no-man-pages`             | Removes man pages                       |
| `--strip-native-commands`    | Removes native commands like java/keytool, further reducing size |

Typical JavaFX application jlink image size: 40-80 MB (including JavaFX modules).

---

## 5. Icon Requirements

Each platform has different format and specification requirements for application icons.

| Platform | Icon Format | Recommended Size                  | Notes                              |
|----------|-------------|-----------------------------------|------------------------------------|
| Windows  | `.ico`      | 256x256 (multi-size embedded)     | Must include 16/32/48/256 etc. multiple sizes |
| macOS    | `.icns`     | 1024x1024 (multi-size embedded)   | Must include 16/32/64/128/256/512/1024 |
| Linux    | `.png`      | 512x512                           | A single PNG file is sufficient    |

### 5.1 Icon Creation Recommendations

1. **Source file**: Use a 1024x1024 or higher resolution PNG/SVG as the source file.
2. **Windows .ico generation**: Use tools (such as png2ico, ImageMagick) to merge multi-size PNGs into an ico.
3. **macOS .icns generation**: Use the `iconutil` command:

```bash
# Create an iconset directory and place icons of various sizes
mkdir MyApp.iconset
sips -z 16 16     icon1024.png --out MyApp.iconset/icon_16x16.png
sips -z 32 32     icon1024.png --out MyApp.iconset/icon_16x16@2x.png
sips -z 32 32     icon1024.png --out MyApp.iconset/icon_32x32.png
sips -z 64 64     icon1024.png --out MyApp.iconset/icon_32x32@2x.png
sips -z 128 128   icon1024.png --out MyApp.iconset/icon_128x128.png
sips -z 256 256   icon1024.png --out MyApp.iconset/icon_128x128@2x.png
sips -z 256 256   icon1024.png --out MyApp.iconset/icon_256x256.png
sips -z 512 512   icon1024.png --out MyApp.iconset/icon_256x256@2x.png
sips -z 512 512   icon1024.png --out MyApp.iconset/icon_512x512.png
cp icon1024.png          MyApp.iconset/icon_512x512@2x.png

# Generate icns
iconutil -c icns MyApp.iconset
```

---

## 6. JavaPackager Gradle Plugin (fvarrui)

`javapackager` is a Gradle plugin maintained by fvarrui, which wraps jpackage to simplify cross-platform packaging configuration.

### 6.1 Plugin Coordinates

```groovy
plugins {
    id 'java'
    id 'application'
    id 'org.openjfx.javafxplugin' version '0.1.0'
    id 'io.github.fvarrui.javapackager' version '1.7.7'
}
```

### 6.2 Complete Configuration Example

```groovy
javafx {
    version = "21.0.11"
    modules = ['javafx.controls', 'javafx.fxml']
}

application {
    mainClass = 'com.example.myapp.MainApp'
}

javapackager {
    // Basic information
    mainClass = 'com.example.myapp.MainApp'
    version = '1.0.0'
    name = 'MyApp'
    description = 'My JavaFX Application'
    organization = 'MyCompany'

    // Packaging type (auto-selected based on current platform by default)
    // Can specify: deb, rpm, dmg, pkg, exe, msi, app-image
    // bundleJre = true  // embed JRE

    // Icon
    // icon = file('src/main/resources/icons/icon.ico')

    // JVM arguments
    jvmArgs = ['--enable-native-access=javafx.graphics', '-Xmx512m']

    // Runtime image
    generateInstaller = true
    copyDependencies = true

    // Platform-specific configuration
    linux {
        // deb/rpm specific configuration
        installationPath = '/opt/myapp'
    }
    mac {
        // dmg/pkg specific configuration
        bundleName = 'MyApp'
        bundleIdentifier = 'com.mycompany.myapp'
    }
    windows {
        // exe/msi specific configuration
        displayName = 'MyApp'
        installationPath = 'C:\\Program Files\\MyApp'
        shortcut = true
        menuGroup = true
        // upgradeUuid = '12345678-1234-1234-1234-123456789abc'
    }
}
```

### 6.3 Executing Packaging

```bash
# Package for current platform
gradle package

# Package for specified platform (requires corresponding platform environment)
gradle package -PtargetPlatform=linux
gradle package -PtargetPlatform=mac
gradle package -PtargetPlatform=windows
```

---

## 7. Gluon Substrate / GraalVM Native Image

Gluon Substrate is based on GraalVM Native Image, which can compile JavaFX applications into native executables, achieving millisecond-level startup and lower memory usage.

### 7.1 Advantages and Limitations

| Advantage                          | Limitation                                    |
|------------------------------------|-----------------------------------------------|
| Extremely fast startup (millisecond level) | Long compilation time (several minutes) |
| Low memory usage                   | Requires GraalVM environment                  |
| Single executable, no JRE needed   | Reflection requires configuration (GraalVM limitation) |
| Better distribution experience     | Some dynamic features (dynamic class loading) are limited |

### 7.2 GluonFX Maven Plugin Configuration

```xml
<plugin>
    <groupId>com.gluonhq</groupId>
    <artifactId>gluonfx-maven-plugin</artifactId>
    <version>1.0.22</version>
    <configuration>
        <target>host</target>
        <mainClass>com.example.myapp.MainApp</mainClass>
        <!-- GraalVM installation path -->
        <graalvmHome>/path/to/graalvm</graalvmHome>
        <!-- Enable native access -->
        <nativeImageArgs>
            <arg>--enable-native-access=javafx.graphics</arg>
        </nativeImageArgs>
    </configuration>
</plugin>
```

### 7.3 Building a Native Image

```bash
# Set up GraalVM environment (GraalVM JDK 21+ has native-image built-in, no need for gu install)
export GRAALVM_HOME=/path/to/graalvm
export PATH=$GRAALVM_HOME/bin:$PATH

# Build native image
mvn gluonfx:build

# Run
mvn gluonfx:nativerun
```

### 7.4 GluonFX Gradle Plugin

```groovy
plugins {
    id 'com.gluonhq.gluonfx-gradle-plugin' version '1.0.22'
}

gluonfx {
    target = 'host'
    mainClass = 'com.example.myapp.MainApp'
    graalvmHome = '/path/to/graalvm'
    nativeImageArgs = ['--enable-native-access=javafx.graphics']
}
```

```bash
gradle nativeBuild
gradle nativeRun
```

### 7.5 Cross-Platform Native Images

GraalVM Native Image does not support cross-compilation, it needs to be built on the target platform. Gluon provides a cloud build service (Gluon Cloud) that can generate native images for multiple platforms in the cloud.

---

### 7.6 ARM64 and macOS Universal Binary Packaging

### 7.7 Windows ARM64

JDK 24+ supports the Windows ARM64 target platform. When packaging, you need to use the ARM64 versions of the JDK and JavaFX SDK:

```bash
# Build with the ARM64 JDK
jpackage \
  --type msi \
  --name MyApp \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/runtime-arm64 \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

> Note: Windows ARM64 must be built on an ARM64 device or emulator; jpackage does not support cross-compilation.

### 7.8 macOS Universal Binary (x64 + aarch64 Merge)

macOS supports Universal Binary, which can include both Intel and Apple Silicon versions in the same application bundle:

```bash
# 1. Build x64 and aarch64 jlink images separately
jlink --module-path "mods:libs-x64" --add-modules com.example.myapp \
      --output build/runtime-x64 --strip-debug --compress zip-6
jlink --module-path "mods:libs-aarch64" --add-modules com.example.myapp \
      --output build/runtime-aarch64 --strip-debug --compress zip-6

# 2. Use jpackage to build two app-images separately
jpackage --type app-image --name MyApp --module com.example.myapp/com.example.myapp.MainApp \
         --module-path "mods:libs-x64" --runtime-image build/runtime-x64 --dest dist-x64
jpackage --type app-image --name MyApp --module com.example.myapp/com.example.myapp.MainApp \
         --module-path "mods:libs-aarch64" --runtime-image build/runtime-aarch64 --dest dist-aarch64

# 3. Merge into a Universal Binary
lipo -create -output dist-universal/MyApp.app/Contents/MacOS/MyApp \
     dist-x64/MyApp.app/Contents/MacOS/MyApp \
     dist-aarch64/MyApp.app/Contents/MacOS/MyApp

# 4. Package as dmg
jpackage --type dmg --name MyApp --app-image dist-universal/MyApp.app --dest dist
```

> Tip: Gluon's JavaFX SDK provides both `mac` and `mac-aarch64` classifiers; download the corresponding versions separately when building.

---

## 8. CI/CD Integration (GitHub Actions)

The following GitHub Actions workflow implements multi-platform automated packaging.

### 8.1 Complete Workflow Example

```yaml
name: Build and Package

on:
  push:
    tags:
      - 'v*'
  workflow_dispatch:

jobs:
  package:
    strategy:
      matrix:
        os: [windows-latest, macos-latest, ubuntu-latest]
        include:
          - os: windows-latest
            platform: windows
            artifact: MyApp-*.exe
          - os: macos-latest
            platform: mac
            artifact: MyApp-*.dmg
          - os: ubuntu-latest
            platform: linux
            artifact: MyApp-*.deb

    runs-on: ${{ matrix.os }}

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'

      - name: Setup JavaFX SDK
        run: |
          # Download JavaFX SDK (jpackage requires SDK or modular JARs)
          curl -L https://download2.gluonhq.com/openjfx/21.0.11/openjfx-21.0.11_${{ matrix.platform }}-x64_bin-sdk.zip -o javafx-sdk.zip
          7z x javafx-sdk.zip -ojavafx-sdk || unzip javafx-sdk.zip -d javafx-sdk
          echo "JAVAFX_SDK=javafx-sdk/javafx-sdk-21.0.11/lib" >> $GITHUB_ENV
        shell: bash

      - name: Install WiX Toolset (Windows)
        if: matrix.platform == 'windows'
        run: |
          dotnet tool install --global wix
          echo "$HOME/.dotnet/tools" >> $GITHUB_PATH

      - name: Build with Maven
        run: mvn -B clean package -DskipTests

      - name: Create jlink runtime image
        run: |
          jlink \
            --module-path "target/modules:$JAVAFX_SDK" \
            --add-modules com.example.myapp \
            --output build/runtime \
            --strip-debug \
            --compress zip-6 \
            --no-header-files \
            --no-man-pages
        shell: bash

      - name: Package with jpackage (Windows)
        if: matrix.platform == 'windows'
        run: |
          jpackage \
            --type msi \
            --name MyApp \
            --app-version ${{ github.ref_name }} \
            --module com.example.myapp/com.example.myapp.MainApp \
            --module-path "target/modules:$JAVAFX_SDK" \
            --runtime-image build/runtime \
            --icon assets/icon.ico \
            --win-menu --win-shortcut \
            --win-upgrade-uuid "12345678-1234-1234-1234-123456789abc" \
            --java-options "--enable-native-access=javafx.graphics" \
            --dest dist

      - name: Package with jpackage (macOS)
        if: matrix.platform == 'mac'
        run: |
          jpackage \
            --type dmg \
            --name MyApp \
            --app-version ${{ github.ref_name }} \
            --module com.example.myapp/com.example.myapp.MainApp \
            --module-path "target/modules:$JAVAFX_SDK" \
            --runtime-image build/runtime \
            --icon assets/icon.icns \
            --mac-package-identifier com.mycompany.myapp \
            --java-options "--enable-native-access=javafx.graphics" \
            --dest dist

      - name: Package with jpackage (Linux)
        if: matrix.platform == 'linux'
        run: |
          jpackage \
            --type deb \
            --name myapp \
            --app-version ${{ github.ref_name }} \
            --module com.example.myapp/com.example.myapp.MainApp \
            --module-path "target/modules:$JAVAFX_SDK" \
            --runtime-image build/runtime \
            --icon assets/icon.png \
            --linux-deb-maintainer "dev@mycompany.com" \
            --java-options "--enable-native-access=javafx.graphics" \
            --dest dist

      - name: Upload artifacts
        uses: actions/upload-artifact@v4
        with:
          name: MyApp-${{ matrix.platform }}
          path: dist/${{ matrix.artifact }}

      - name: Release
        if: startsWith(github.ref, 'refs/tags/')
        uses: softprops/action-gh-release@v2
        with:
          files: dist/${{ matrix.artifact }}
```

### 8.2 CI/CD Notes

1. **Cross-platform limitation**: jpackage cannot cross-compile, each platform must be built on a runner of the corresponding OS.
2. **JavaFX SDK download**: The CI environment needs to download the JavaFX SDK or use modular JAR dependencies.
3. **Cache dependencies**: Use `actions/cache` to cache Maven/Gradle dependencies to speed up builds.
4. **Version number management**: Extract version numbers from Git tags to maintain version consistency.

---

## 9. Code Signing Overview

Code signing ensures that installers are from a trusted source, avoiding OS security warnings (such as Windows SmartScreen, macOS Gatekeeper).

### 9.1 Windows Code Signing

```bash
jpackage \
  --type msi \
  --name MyApp \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/runtime \
  --win-upgrade-uuid "..." \
  --java-options "--enable-native-access=javafx.graphics" \
  --resource-dir sign-resources \
  --dest dist
```

Windows signing requires providing a WiX custom template via `--resource-dir` or using third-party tools (signtool, osslsigncode) to sign the generated installer:

```bash
# Sign using signtool (requires a code signing certificate)
signtool sign /fd SHA256 /a /tr http://timestamp.digicert.com /td SHA256 dist\MyApp-1.0.0.msi
```

### 9.2 macOS Code Signing and Notarization

```bash
# Sign
jpackage \
  --type dmg \
  --name MyApp \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/runtime \
  --mac-sign \
  --mac-signing-key-user-name "Developer ID Application: Your Name (XXXXXXXXXX)" \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist

# Notarization
xcrun notarytool submit dist/MyApp-1.0.0.dmg \
  --apple-id "your@email.com" \
  --password "app-specific-password" \
  --team-id "XXXXXXXXXX" \
  --wait

# Staple the notarization ticket
xcrun stapler staple dist/MyApp-1.0.0.dmg
```

> macOS distribution must go through signing + notarization, otherwise users will be blocked by Gatekeeper on first open.

### 9.3 Linux Signing

Linux deb/rpm packages are usually signed with GPG:

```bash
# Sign deb package
dpkg-sig --sign builder dist/myapp-1.0.0.deb

# Sign rpm package (requires signing key configured in rpmmacros)
rpm --addsign dist/myapp-1.0.0.rpm
```

---

## 10. Auto-Update Strategies

JavaFX applications do not have a built-in auto-update mechanism, you need to implement it yourself or use third-party solutions.

### 10.1 Self-Implemented Update Check

```java
public class UpdateChecker {
    private static final String VERSION_URL = "https://mycompany.com/api/latest-version";

    public void checkForUpdates() {
        Task<VersionInfo> task = new Task<>() {
            @Override
            protected VersionInfo call() throws Exception {
                // Request latest version information
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VERSION_URL))
                    .build();
                HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
                // Parse JSON to get version number and download link
                return parseVersionInfo(response.body());
            }
        };
        task.setOnSucceeded(e -> {
            VersionInfo info = task.getValue();
            if (isNewer(info.getVersion(), getCurrentVersion())) {
                showUpdateDialog(info);
            }
        });
        new Thread(task).start();
    }

    private void showUpdateDialog(VersionInfo info) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("New version found");
        alert.setHeaderText("New version " + info.getVersion() + " available");
        alert.setContentText("Download update now?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Open download page or start download
            HostServices services = getHostServices();
            services.showDocument(info.getDownloadUrl());
        }
    }
}
```

### 10.2 Common Auto-Update Solutions

| Solution                | Description                                                       |
|-------------------------|-------------------------------------------------------------------|
| Self-implemented version check | Request the latest version from the server on app startup, prompt user to download new installer |
| Windows MSI upgrade     | msi with the same `--win-upgrade-uuid` can upgrade silently       |
| Background incremental update | Only download changed JARs/resources, replace and restart (needs self-implementation) |
| Third-party update framework | Such as update4j (JavaFX-friendly update framework)           |

### 10.3 update4j Integration Example

```xml
<dependency>
    <groupId>org.update4j</groupId>
    <artifactId>update4j</artifactId>
    <version>1.5.9</version>
</dependency>
```

```java
import org.update4j.Configuration;

// Load update configuration from remote
Configuration config = Configuration.read(new URL("https://mycompany.com/update/config.xml"));

// Check and update
if (config.requiresUpdate()) {
    config.update();  // download updated files
}

// Launch application
config.launch();  // launch main class
```

---

## 11. The --enable-native-access Flag in JavaFX 24+

The JavaFX 24+ graphics rendering layer accesses native code via JNI, which under the JDK 24+ strict encapsulation mechanism requires explicitly granting native access permission.

### 11.1 Adding the Flag at Packaging Time

**jpackage method:**

```bash
jpackage \
  --type msi \
  --name MyApp \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/runtime \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

**jlink image method (hardcoded in the launch script):**

The app-image generated by jpackage automatically writes `--java-options` into the launch script (`bin/MyApp` or `bin/MyApp.bat`), no need for the user to add it manually.

### 11.2 Verifying the Flag Takes Effect

```bash
# Check the generated launch script
cat dist/MyApp/bin/MyApp  # Linux/macOS
type dist\MyApp\bin\MyApp.bat  # Windows
```

The launch script should contain the `--enable-native-access=javafx.graphics` parameter.

### 11.3 Multi-Module Native Access

If the application also uses other native libraries, you can list multiple module names:

```bash
--java-options "--enable-native-access=javafx.graphics,com.example.nativelib"
```

---

## 12. Summary of Packaging and Deployment Best Practices

| Practice                          | Description                                                       |
|-----------------------------------|-------------------------------------------------------------------|
| Prefer modularity + jlink         | Reduce size, improve startup speed                                |
| Unified version number management | Extract version from build config or Git tag, maintain consistency |
| Build separately per platform     | jpackage does not support cross-compilation, build each platform independently |
| Keep upgrade-uuid consistent      | Windows upgrades depend on a fixed UUID                           |
| macOS must sign + notarize        | Otherwise users cannot open normally                              |
| Embed JRE                         | Embed runtime via jlink/jpackage, users don't need to pre-install Java |
| Add native access flag for JavaFX 24+ | `--enable-native-access=javafx.graphics`                  |
| CI/CD automated multi-platform packaging | Use GitHub Actions matrix build                          |
| Provide auto-update check         | Improve user experience, push fixes in time                       |
| Test install and uninstall process | Ensure shortcuts, registry, file associations are correct        |
