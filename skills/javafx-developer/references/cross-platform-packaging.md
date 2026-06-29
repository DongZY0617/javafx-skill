# Cross-Platform Packaging Matrix

This reference provides a side-by-side comparison of the jpackage options, toolchains, icon formats, code signing, and CI/CD build matrix required to ship a JavaFX application to Windows, macOS, and Linux. It complements `packaging-deployment.md`, which covers the full packaging workflow in depth; this document focuses on the **differences across platforms** and the **build matrix strategy** for producing all three installers from a single codebase.

> **Core constraint**: `jpackage` cannot cross-compile. Every platform's installer must be produced on a host running that operating system. Multi-platform delivery is therefore a CI/CD matrix problem, not a single-command problem.

---

## 1. Platform-Specific jpackage Options Matrix

The matrix below maps each platform to its output type and the jpackage options that are specific to (or most relevant for) that platform. Common options (`--name`, `--app-version`, `--vendor`, `--module`, `--module-path`, `--runtime-image`, `--java-options`, `--dest`) apply to all platforms and are omitted from the table.

| Concern | Windows | macOS | Linux |
|---------|---------|-------|-------|
| Output `--type` | `msi` (WiX) / `exe` (Inno Setup) | `dmg` / `pkg` | `deb` / `rpm` |
| Icon `--icon` | `.ico` (multi-size embedded) | `.icns` | `.png` |
| Package name | `--name` | `--mac-package-name` | `--linux-package-name` |
| Package identifier | `--win-upgrade-uuid` (UUID v4) | `--mac-package-identifier` (reverse-DNS) | `--linux-deb-maintainer` (email) |
| Shortcuts / menu | `--win-menu`, `--win-shortcut`, `--win-dir-chooser`, `--win-per-user-install` | managed by `.app` bundle | managed by desktop entry |
| Code signing | external `signtool` / WiX resource-dir | `--mac-sign`, `--mac-signing-key-user-name` | GPG via `dpkg-sig` / `rpm --addsign` |
| License metadata | via WiX template | via `Info.plist` | `--linux-rpm-license-type` |
| Install directory | `--install-dir` | `.app` bundle convention | `--install-dir` |

### 1.1 Minimal Command Templates per Platform

```bash
# Windows (.msi) — requires WiX Toolset 4.x
jpackage --type msi --name "MyApp" --app-version 1.0.0 --vendor "MyCompany" \
  --module com.example/com.example.App --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icons/app.ico \
  --win-menu --win-shortcut \
  --win-upgrade-uuid "12345678-1234-4234-8234-123456789abc" \
  --java-options "--enable-native-access=javafx.graphics" --dest dist

# macOS (.dmg) — requires Xcode command line tools
jpackage --type dmg --name "MyApp" --app-version 1.0.0 --vendor "MyCompany" \
  --module com.example/com.example.App --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icons/app.icns \
  --mac-package-name "MyApp" \
  --mac-package-identifier "com.mycompany.myapp" \
  --mac-sign --mac-signing-key-user-name "Developer ID Application: Your Name (XXXXXXXXXX)" \
  --java-options "--enable-native-access=javafx.graphics" --dest dist

# Linux (.deb) — requires dpkg-deb
jpackage --type deb --name "myapp" --app-version 1.0.0 --vendor "MyCompany" \
  --module com.example/com.example.App --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icons/app.png \
  --linux-package-name "myapp" \
  --linux-deb-maintainer "dev@mycompany.com" \
  --linux-rpm-license-type "MIT" --install-dir /opt/myapp \
  --java-options "--enable-native-access=javafx.graphics" --dest dist

# Linux (.rpm) — requires rpmbuild
jpackage --type rpm --name "myapp" --app-version 1.0.0 --vendor "MyCompany" \
  --module com.example/com.example.App --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icons/app.png \
  --linux-package-name "myapp" \
  --linux-rpm-license-type "MIT" --install-dir /opt/myapp \
  --java-options "--enable-native-access=javafx.graphics" --dest dist
```

> The full per-platform command templates (including shortcut flags and signing) are also embedded in `javafx-developer`'s SKILL.md Packaging chapter. See `packaging-deployment.md` for the complete parameter reference and jlink runtime image generation.

---

## 2. Toolchain Requirements per Platform

jpackage invokes a native packaging toolchain that is unique to each platform. The toolchain must be present on the build host that produces the corresponding installer.

| Platform | Output Type | Required Toolchain | How to Install | Verification Command |
|----------|-------------|---------------------|-----------------|----------------------|
| Windows | `msi` | WiX Toolset 4.x | `dotnet tool install --global wix` | `wix --version` |
| Windows | `exe` | Inno Setup 6+ | Download from jrsoftware.org, add to `PATH` | `iscc /?` |
| macOS | `dmg` / `pkg` | Xcode command line tools | `xcode-select --install` | `xcode-select -p` |
| macOS | signing | Developer ID Application certificate | Apple Developer portal | `security find-identity -p codesigning` |
| Linux | `deb` | `dpkg-deb` | `apt install dpkg-dev` (Debian/Ubuntu) | `dpkg-deb --version` |
| Linux | `rpm` | `rpmbuild` | `dnf install rpm-build` (Fedora/RHEL) | `rpmbuild --version` |
| Linux | signing | `dpkg-sig` / `rpm` GPG | `apt install dpkg-sig` | `dpkg-sig --help` |

### 2.1 Toolchain Detection Notes

- **Windows**: WiX 4.x is installed as a `.NET` global tool; ensure `$HOME/.dotnet/tools` is on `PATH`. Inno Setup installs `ISCC.exe` to its install directory — add that directory to `PATH` manually.
- **macOS**: A full Xcode install is not required; the command line tools suffice for `jpackage`. Notarization additionally requires an App Store Connect API key or an app-specific password.
- **Linux**: A single host can produce both `.deb` and `.rpm` if both `dpkg-deb` and `rpmbuild` are installed (e.g., a container with both). Cross-distribution toolchains work on any Linux distro as long as the binary is present.

---

## 3. Icon Format Specifications and Conversion Guide

Each platform requires a different icon format. jpackage rejects mismatched formats, so the correct file must be supplied per platform.

| Platform | Format | Recommended Sizes | Required Embedded Sizes |
|----------|--------|-------------------|-------------------------|
| Windows | `.ico` | 256x256 base | 16, 32, 48, 256 (multi-size embedded) |
| macOS | `.icns` | 1024x1024 base | 16, 32, 64, 128, 256, 512, 1024 (incl. @2x) |
| Linux | `.png` | 512x512 | single file is sufficient |

### 3.1 Source File

Start from a single high-resolution source (1024x1024 PNG or SVG). Derive all three platform icons from this source so the brand stays consistent.

### 3.2 Windows `.ico` Generation

Merge multiple PNG sizes into one `.ico` using ImageMagick or `png2ico`:

```bash
# ImageMagick: generate multi-size .ico from a single source
magick icon1024.png -define icon:auto-resize=256,48,32,16 assets/icons/app.ico
```

### 3.3 macOS `.icns` Generation

Use the built-in `iconutil` command (macOS only) to assemble an `.iconset` into an `.icns`:

```bash
mkdir MyApp.iconset
sips -z 16 16   icon1024.png --out MyApp.iconset/icon_16x16.png
sips -z 32 32   icon1024.png --out MyApp.iconset/icon_16x16@2x.png
sips -z 32 32   icon1024.png --out MyApp.iconset/icon_32x32.png
sips -z 64 64   icon1024.png --out MyApp.iconset/icon_32x32@2x.png
sips -z 128 128 icon1024.png --out MyApp.iconset/icon_128x128.png
sips -z 256 256 icon1024.png --out MyApp.iconset/icon_128x128@2x.png
sips -z 256 256 icon1024.png --out MyApp.iconset/icon_256x256.png
sips -z 512 512 icon1024.png --out MyApp.iconset/icon_256x256@2x.png
sips -z 512 512 icon1024.png --out MyApp.iconset/icon_512x512.png
cp icon1024.png          MyApp.iconset/icon_512x512@2x.png

iconutil -c icns MyApp.iconset
```

> The `.icns` step runs on macOS. For cross-platform CI, commit the pre-built `.icns` to the repository rather than regenerating it on each build.

### 3.4 Linux `.png`

A single 512x512 PNG is sufficient. No conversion tool is needed; downscale the source with `sips` or ImageMagick:

```bash
magick icon1024.png -resize 512x512 assets/icons/app.png
```

---

## 4. Code Signing Overview

Code signing proves the installer originates from a trusted source and prevents OS security warnings (Windows SmartScreen, macOS Gatekeeper).

### 4.1 Windows (Authenticode)

jpackage itself does not sign the final installer; sign the produced `.msi`/`.exe` afterwards with `signtool` (requires a code signing certificate — OV or EV):

```bash
signtool sign /fd SHA256 /a /tr http://timestamp.digicert.com /td SHA256 \
  dist\MyApp-1.0.0.msi
```

- **Certificate types**: OV (Organizational Validation) triggers SmartScreen reputation building; EV (Extended Validation) is trusted immediately but requires a hardware token.
- **Alternative**: `osslsigncode` for cross-platform signing in CI.
- **WiX integration**: signing can also be wired through a WiX custom template placed in `--resource-dir`.

### 4.2 macOS (Signing + Notarization)

macOS distribution requires **both** signing and notarization, otherwise Gatekeeper blocks the app on first open.

```bash
# 1. Sign during jpackage
jpackage --type dmg --name MyApp \
  --mac-sign \
  --mac-signing-key-user-name "Developer ID Application: Your Name (XXXXXXXXXX)" \
  ...other args...

# 2. Submit for notarization
xcrun notarytool submit dist/MyApp-1.0.0.dmg \
  --apple-id "your@email.com" \
  --password "app-specific-password" \
  --team-id "XXXXXXXXXX" --wait

# 3. Staple the notarization ticket
xcrun stapler staple dist/MyApp-1.0.0.dmg
```

- **Requirement**: a Developer ID Application certificate from the Apple Developer Program.
- **App-specific password**: generate one at appleid.apple.com for `notarytool`.
- **Universal Binary**: for Apple Silicon + Intel support, build x64 and aarch64 app-images, merge with `lipo`, then sign and notarize the merged bundle.

### 4.3 Linux (GPG)

Linux `.deb`/`.rpm` packages are typically signed with GPG (optional but recommended for repositories):

```bash
# Sign deb
dpkg-sig --sign builder dist/myapp-1.0.0.deb

# Sign rpm (requires signing key configured in ~/.rpmmacros)
rpm --addsign dist/myapp-1.0.0.rpm
```

### 4.4 Signing Capability Matrix

| Platform | Mechanism | Required Credential | Mandatory? |
|----------|-----------|---------------------|------------|
| Windows | Authenticode (`signtool`) | Code signing certificate (OV/EV) | No (SmartScreen warning without it) |
| macOS | Developer ID + notarization | Developer ID Application cert + App Store Connect | Yes (Gatekeeper blocks unsigned apps) |
| Linux | GPG | GPG signing key | No (only for APT/YUM repos) |

---

## 5. CI/CD Multi-Platform Build Matrix Strategy

Because jpackage cannot cross-compile, multi-platform delivery is implemented as a **matrix build** that runs the same job on Windows, macOS, and Linux runners in parallel, each producing the artifact for its own OS.

### 5.1 Matrix Strategy Principles

1. **One job per platform**: a matrix dimension `os: [windows-latest, macos-latest, ubuntu-latest]` fans out three parallel jobs.
2. **Conditional toolchain install**: each job installs only the toolchain its platform needs (`if:` guards).
3. **JavaFX SDK per platform**: download the SDK classifier matching the runner (`windows`, `mac`, `linux`).
4. **Single source of truth for version**: derive `--app-version` from the Git tag so all three artifacts share one version.
5. **Artifact upload per platform**: upload each platform's installer with a platform-tagged name.
6. **Release step**: attach all artifacts to the same GitHub/GitLab release.

### 5.2 GitHub Actions Matrix Skeleton

```yaml
name: Build and Package

on:
  push:
    tags: ['v*']
  workflow_dispatch:

jobs:
  package:
    strategy:
      matrix:
        os: [windows-latest, macos-latest, ubuntu-latest]
        include:
          - os: windows-latest
            platform: windows
            artifact: MyApp-*.msi
            icon: assets/icons/app.ico
          - os: macos-latest
            platform: mac
            artifact: MyApp-*.dmg
            icon: assets/icons/app.icns
          - os: ubuntu-latest
            platform: linux
            artifact: myapp_*.deb
            icon: assets/icons/app.png

    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v4

      - name: Setup JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'

      - name: Setup JavaFX SDK
        run: |
          curl -L "https://download2.gluonhq.com/openjfx/21.0.11/openjfx-21.0.11_${{ matrix.platform }}-x64_bin-sdk.zip" -o javafx-sdk.zip
          if [ "$RUNNER_OS" = "Windows" ]; then 7z x javafx-sdk.zip -ojavafx-sdk; else unzip javafx-sdk.zip -d javafx-sdk; fi
          echo "JAVAFX_SDK=javafx-sdk/javafx-sdk-21.0.11/lib" >> $GITHUB_ENV
        shell: bash

      - name: Install WiX (Windows only)
        if: matrix.platform == 'windows'
        run: |
          dotnet tool install --global wix
          echo "$HOME/.dotnet/tools" >> $GITHUB_PATH

      - name: Build JAR
        run: mvn -B clean package -DskipTests

      - name: Create jlink runtime image
        run: |
          jlink --module-path "target/modules:$JAVAFX_SDK" \
            --add-modules com.example --output build/runtime \
            --strip-debug --compress zip-6 --no-header-files --no-man-pages
        shell: bash

      - name: Package (Windows)
        if: matrix.platform == 'windows'
        run: |
          jpackage --type msi --name MyApp --app-version ${GITHUB_REF_NAME} \
            --module com.example/com.example.App --module-path "target/modules:$JAVAFX_SDK" \
            --runtime-image build/runtime --icon ${{ matrix.icon }} \
            --win-menu --win-shortcut \
            --win-upgrade-uuid "12345678-1234-4234-8234-123456789abc" \
            --java-options "--enable-native-access=javafx.graphics" --dest dist

      - name: Package (macOS)
        if: matrix.platform == 'mac'
        run: |
          jpackage --type dmg --name MyApp --app-version ${GITHUB_REF_NAME} \
            --module com.example/com.example.App --module-path "target/modules:$JAVAFX_SDK" \
            --runtime-image build/runtime --icon ${{ matrix.icon }} \
            --mac-package-identifier com.mycompany.myapp \
            --java-options "--enable-native-access=javafx.graphics" --dest dist

      - name: Package (Linux)
        if: matrix.platform == 'linux'
        run: |
          jpackage --type deb --name myapp --app-version ${GITHUB_REF_NAME} \
            --module com.example/com.example.App --module-path "target/modules:$JAVAFX_SDK" \
            --runtime-image build/runtime --icon ${{ matrix.icon }} \
            --linux-deb-maintainer "dev@mycompany.com" \
            --java-options "--enable-native-access=javafx.graphics" --dest dist

      - uses: actions/upload-artifact@v4
        with:
          name: MyApp-${{ matrix.platform }}
          path: dist/${{ matrix.artifact }}

      - name: Release
        if: startsWith(github.ref, 'refs/tags/')
        uses: softprops/action-gh-release@v2
        with:
          files: dist/${{ matrix.artifact }}
```

### 5.3 Matrix Build Notes

- **Cross-compile limitation**: never attempt to produce a Windows `.msi` on a Linux runner — it will fail. Each artifact must come from its native runner.
- **Caching**: cache Maven/Gradle dependencies and the JavaFX SDK download to speed up matrix jobs.
- **macOS universal binary**: for Apple Silicon + Intel, add a second macOS matrix entry that builds the merged `lipo` bundle, or produce two separate `.dmg` artifacts.
- **Secrets management**: store signing certificates, Apple credentials, and GPG keys as CI secrets; never commit them to the repository.
- **Verification hook**: the `javafx-runner` skill can be invoked per platform in the matrix to verify each artifact (current-platform executable check + non-current-platform configuration-completeness check).

---

## 6. Cross-Platform Packaging Checklist

Before triggering a multi-platform matrix build, confirm every platform's configuration is complete. The `javafx-runner` packaging dimension validates this via configuration completeness (see `javafx-runner`'s Packaging Verification dimension).

| # | Check | Windows | macOS | Linux |
|---|-------|---------|-------|-------|
| 1 | Output `--type` defined | `msi`/`exe` | `dmg`/`pkg` | `deb`/`rpm` |
| 2 | Icon file exists & correct format | `.ico` | `.icns` | `.png` |
| 3 | Package name / identifier present | `--win-upgrade-uuid` (UUID v4) | `--mac-package-name` + `--mac-package-identifier` | `--linux-package-name` + `--linux-deb-maintainer` |
| 4 | Toolchain available on target runner | WiX / Inno Setup | Xcode tools | `dpkg-deb` / `rpmbuild` |
| 5 | JavaFX SDK classifier matches platform | `windows` | `mac` | `linux` |
| 6 | `--java-options "--enable-native-access=javafx.graphics"` (JavaFX 24+) | yes | yes | yes |
| 7 | Code signing configured | `signtool` + cert | `--mac-sign` + notarization | GPG (optional) |
| 8 | Version sourced from single tag | yes | yes | yes |

> **Reminder**: a project that only defines a Windows `msi` command will pass packaging verification on Windows but fail the "Cross-platform configuration completeness" check. Complete all three platform configs before delivering a multi-platform release.
