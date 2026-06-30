# Distribution Channels

This document defines the rules for distributing JavaFX desktop applications through official platform stores and package managers. It covers MSIX / Microsoft Store (Windows), Mac App Store (macOS), and Snap / Flatpak (Linux). It serves as the reference for `javafx-deployer`'s Distribution Channels dimension.

## 1. Overview

Beyond direct download (installer EXE/MSI/DMG/DEB/RPM), JavaFX apps can be distributed through official platform stores. Store distribution provides:

- **Automatic updates** handled by the OS
- **Trust and discoverability** — users find the app via store search
- **Code signing requirement** — stores enforce signing, improving security
- **Sandbox restrictions** — stores may impose file system / network restrictions

| Channel | Platform | Package Format | Store | Update Mechanism |
|---------|----------|---------------|-------|------------------|
| MSIX | Windows | `.msix` / `.msixbundle` | Microsoft Store | Store-managed |
| PKG | macOS | `.pkg` (notarized) | Mac App Store | Store-managed |
| Snap | Linux | `.snap` | Snap Store | Snap daemon (`snapd`) |
| Flatpak | Linux | `.flatpak` | Flathub | Flatpak runtime |

## 2. MSIX / Microsoft Store (Windows)

### 2.1 What is MSIX?

MSIX is Microsoft's modern packaging format that replaces legacy MSI and AppX. It provides:
- Clean install/uninstall (no registry bloat, no leftover files)
- Automatic updates via Microsoft Store
- Sandboxed execution with capability-based permissions
- Support for Win32, UWP, and Java desktop apps

### 2.2 Prerequisites

- **Windows SDK 10.0.22000+** (for `MakeAppx` and `signtool`)
- **Code signing certificate** — EV certificate recommended for Store submission; self-signed for testing
- **App Installer** (optional) — for `.appinstaller` side-loading distribution outside the Store
- **Publisher ID** — obtained from Microsoft Partner Center

### 2.3 Packaging with jpackage + MSIX

JavaFX apps can produce MSIX using `jpackage` with the `--type msix` flag:

```bash
jpackage \
  --name MyApp \
  --type msix \
  --input target/libs \
  --main-jar myapp.jar \
  --main-class com.example.App \
  --java-options "--add-modules javafx.controls,javafx.fxml" \
  --win-menu \
  --win-menu-group MyApp \
  --win-upgrade-uuid "12345678-1234-1234-1234-123456789012" \
  --icon src/main/resources/icons/app.ico \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --copyright "Copyright 2026 MyCompany" \
  --description "My JavaFX Application" \
  --verbose
```

Key flags for MSIX:
- `--type msix` — produce MSIX package instead of EXE/MSI
- `--app-version` — semantic version (must match Store version)
- `--win-upgrade-uuid` — stable UUID for upgrade tracking (generate once, reuse for all versions)
- `--vendor` — must match the Publisher identity in Partner Center

### 2.4 MSIX Manifest Customization

`jpackage` generates a default `AppxManifest.xml` inside the MSIX. For Store submission, you may need to customize capabilities and visual assets. Create a custom manifest template:

```xml
<?xml version="1.0" encoding="utf-8"?>
<Package
  xmlns="http://schemas.microsoft.com/appx/manifest/foundation/windows10"
  xmlns:uap="http://schemas.microsoft.com/appx/manifest/uap/windows10"
  xmlns:rescap="http://schemas.microsoft.com/appx/manifest/foundation/windows10/restrictedcapabilities">

  <Identity Name="MyCompany.MyApp"
            Publisher="CN=MyCompany, O=MyCompany, L=City, S=State, C=Country"
            Version="1.0.0.0"
            ProcessorArchitecture="x64"/>

  <Properties>
    <DisplayName>MyApp</DisplayName>
    <PublisherDisplayName>MyCompany</PublisherDisplayName>
    <Logo>assets\StoreLogo.png</Logo>
  </Properties>

  <Resources>
    <Resource Language="en-us"/>
  </Resources>

  <Dependencies>
    <TargetDeviceFamily Name="Windows.Desktop" MinVersion="10.0.17763.0" MaxVersionTested="10.0.26100.0"/>
  </Dependencies>

  <Applications>
    <Application Id="MyApp" Executable="MyApp.exe" EntryPoint="Windows.FullTrustApplication">
      <uap:VisualElements
        DisplayName="MyApp"
        Description="My JavaFX Application"
        BackgroundColor="transparent"
        Square150x150Logo="assets\Square150x150Logo.png"
        Square44x44Logo="assets\Square44x44Logo.png">
        <uap:DefaultTile Wide310x150Logo="assets\Wide310x150Logo.png"/>
        <uap:SplashScreen Image="assets\SplashScreen.png"/>
      </uap:VisualElements>
    </Application>
  </Applications>

  <Capabilities>
    <rescap:Capability Name="runFullTrust"/>
    <Capability Name="internetClient"/>
  </Capabilities>
</Package>
```

> **runFullTrust capability**: JavaFX desktop apps run with full trust (not sandboxed like UWP). This capability must be declared and is approved automatically for desktop bridge apps.

### 2.5 Microsoft Store Submission

1. **Register at Microsoft Partner Center** (https://partner.microsoft.com)
2. **Reserve app name** — the app name must be unique in the Store
3. **Create app package** — upload the signed `.msix` (or `.msixbundle` for multi-arch)
4. **Configure Store listing** — description, screenshots, pricing, age rating
5. **Submit for certification** — Microsoft runs automated and manual review (1-3 business days)
6. **Publish** — after certification passes, the app goes live

### 2.6 App Installer (Side-loading)

For distributing MSIX outside the Store, create an `.appinstaller` file that enables click-to-install and automatic updates from a web server:

```xml
<?xml version="1.0" encoding="utf-8"?>
<AppInstaller
  xmlns="http://schemas.microsoft.com/appx/appinstaller/2021"
  Version="1.0.0.0"
  Uri="https://<your-cdn-domain>/myapp/MyApp.appinstaller">

  <MainPackage
    Name="MyCompany.MyApp"
    Publisher="CN=MyCompany, O=MyCompany, L=City, S=State, C=Country"
    Version="1.0.0.0"
    Uri="https://<your-cdn-domain>/myapp/1.0.0/MyApp.msix"/>

  <UpdateSettings>
    <OnLaunch HoursBetweenUpdateChecks="24"/>
    <AutomaticBackgroundTask/>
    <ForceUpdateFromAnyVersion>false</ForceUpdateFromAnyVersion>
  </UpdateSettings>
</AppInstaller>
```

## 3. Mac App Store (macOS)

### 3.1 Prerequisites

- **Apple Developer Program membership** ($99/year)
- **App Store app ID** — registered in Apple Developer Portal
- **Mac App Store entitlements** — the app must use the Mac App Store sandbox
- **Notarization** — app must be notarized by Apple (automated virus/malware scan)

### 3.2 Sandboxing Requirement

Mac App Store apps **must** run in the App Sandbox. This restricts:
- File system access to designated containers (`~/Library/Containers/{bundle-id}/`)
- Network access must be declared via entitlements
- No direct hardware access without entitlements
- No forking/exec'ing arbitrary processes

For JavaFX apps, this means:
- All user documents must be accessed via `NSOpenPanel` / `NSSavePanel` (user must explicitly grant access)
- The Java runtime and JavaFX modules must be bundled inside the app (no system-wide JRE dependency)
- Preferences must use `java.util.prefs.Preferences` (stored in the sandbox container)

### 3.3 Entitlements for Mac App Store

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>com.apple.security.app-sandbox</key>
    <true/>
    <key>com.apple.security.network.client</key>
    <true/>
    <key>com.apple.security.network.server</key>
    <false/>
    <key>com.apple.security.files.user-selected.read-write</key>
    <true/>
    <key>com.apple.security.files.bookmarks.app-scope</key>
    <true/>
    <key>com.apple.security.printing</key>
    <true/>
</dict>
</plist>
```

| Entitlement | Purpose |
|-------------|---------|
| `app-sandbox` | Required for Mac App Store |
| `network.client` | Outbound network (HTTP API calls) |
| `network.server` | Inbound network (embedded server) |
| `files.user-selected.read-write` | Read/write files chosen by user via open/save dialog |
| `files.bookmarks.app-scope` | Remember file access across app launches |
| `printing` | Print documents |

### 3.4 Packaging for Mac App Store

Use `jpackage` with `--type app-image` first, then sign and submit:

```bash
# Step 1: Create app image
jpackage \
  --name MyApp \
  --type app-image \
  --input target/libs \
  --main-jar myapp.jar \
  --main-class com.example.App \
  --java-options "--add-modules javafx.controls,javafx.fxml" \
  --mac-package-identifier com.example.myapp \
  --mac-package-name "MyApp" \
  --mac-sign \
  --mac-signing-key-user-name "Developer ID Application: MyCompany (TEAMID)" \
  --icon src/main/resources/icons/app.icns \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --verbose

# Step 2: Apply Mac App Store entitlements
codesign --force --deep --options runtime \
  --entitlements mac-app-store-entitlements.plist \
  --sign "3rd Party Mac Developer Application: MyCompany (TEAMID)" \
  MyApp.app

# Step 3: Create PKG for App Store
productbuild \
  --component MyApp.app /Applications \
  --sign "3rd Party Mac Developer Installer: MyCompany (TEAMID)" \
  --product product-requirements.plist \
  MyApp.pkg

# Step 4: Submit to App Store via Transporter or altool
xcrun altool --upload-app -f MyApp.pkg \
  --type macos \
  --username "apple-id@<your-cdn-domain>" \
  --password "app-specific-password"
```

> **Signing identity difference**: For Mac App Store, use "3rd Party Mac Developer Application" (not "Developer ID Application"). The latter is for direct distribution outside the Store.

### 3.5 Mac App Store Limitations for JavaFX

- **No auto-launch on login** without `com.apple.security.login-items` entitlement
- **No access to `/tmp` or system directories** — use the sandbox container
- **JRE must be bundled** — increases app size by ~80-150MB (use `jlink` to create a custom runtime image to minimize size)
- **No self-update mechanism needed** — the App Store handles updates automatically

## 4. Snap (Linux)

### 4.1 What is Snap?

Snap is a package manager developed by Canonical (Ubuntu). Snaps are containerized packages that run across Linux distributions with automatic updates and rollback built in.

### 4.2 Creating a Snap for JavaFX

Create a `snapcraft.yaml` in the project root:

```yaml
name: myapp
version: '1.0.0'
summary: My JavaFX Application
description: |
  A cross-platform desktop application built with JavaFX.

grade: stable
confinement: strict
base: core22

apps:
  myapp:
    command: bin/myapp
    desktop: share/applications/myapp.desktop
    extensions: [gnome]
    plugs:
      - desktop
      - desktop-legacy
      - wayland
      - x11
      - home
      - network
      - audio-playback
      - opengl
    environment:
      JAVA_HOME: "$SNAP/usr/lib/jvm/java-17-openjdk-amd64"
      PATH: "$SNAP/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH"

parts:
  myapp:
    plugin: dump
    source: target/dist
    organize:
      '*.jar': lib/
      'bin/*': bin/
      'lib/*': lib/
    stage-packages:
      - openjdk-17-jre
      - libgl1
      - libegl1
      - libxext6
      - libxrender1
      - libxtst6
      - libxi6
      - fonts-dejavu-core

  desktop:
    plugin: dump
    source: src/main/resources/snap
    organize:
      'myapp.desktop': share/applications/
      'myapp.png': share/icons/
```

### 4.3 Snap Plugs (Permissions)

| Plug | Purpose |
|------|---------|
| `desktop` | Access to desktop session (Wayland/X11) |
| `desktop-legacy` | Legacy X11 desktop integration |
| `x11` | X11 windowing system |
| `wayland` | Wayland display server |
| `home` | Read/write user home directory |
| `network` | Network access (outbound HTTP/TCP) |
| `audio-playback` | Play audio |
| `opengl` | GPU acceleration for JavaFX rendering |
| `cups` | Printing |
| `removable-media` | USB drives / external storage |

### 4.4 Building and Publishing

```bash
# Install snapcraft
sudo snap install snapcraft --classic

# Build the snap
snapcraft

# Install locally for testing
sudo snap install myapp_1.0.0_amd64.snap --dangerous

# Publish to Snap Store
snapcraft login
snapcraft register myapp
snapcraft push myapp_1.0.0_amd64.snap
```

### 4.5 Snap Auto-Update and Rollback

Snaps update automatically via `snapd`. Key features:
- **Automatic refresh**: `snapd` checks for updates up to 4 times per day
- **Automatic rollback**: If a snap fails to start after update, `snapd` rolls back to the previous version automatically
- **Manual refresh control**: Users can pin a snap to a specific channel: `snap refresh myapp --channel=1.0/stable`
- **Scheduled refresh**: Administrators can configure refresh windows

## 5. Flatpak (Linux)

### 5.1 What is Flatpak?

Flatpak is a cross-distribution Linux packaging format maintained by the GNOME project. It runs applications in sandboxed environments with declarative permissions.

### 5.2 Creating a Flatpak for JavaFX

Create a manifest file `com.example.MyApp.json`:

```json
{
  "app-id": "com.example.MyApp",
  "runtime": "org.gnome.Platform",
  "runtime-version": "46",
  "sdk": "org.gnome.Sdk",
  "command": "myapp",
  "finish-args": [
    "--socket=x11",
    "--socket=wayland",
    "--share=network",
    "--filesystem=home",
    "--device=dri",
    "--share=ipc",
    "--socket=pulseaudio"
  ],
  "modules": [
    {
      "name": "openjdk",
      "buildsystem": "simple",
      "build-commands": [
        "mkdir -p /app/jdk",
        "tar -xf jdk-17_linux-x64_bin.tar.gz --strip-components=1 -C /app/jdk"
      ],
      "sources": [
        {
          "type": "archive",
          "url": "https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_linux-x64_bin.tar.gz",
          "sha256": "0022753d0cceecacdd3af790c257d5b3b1bf1f3a4172e9c1b1e9c5b3b1bf1f3a"
        }
      ]
    },
    {
      "name": "myapp",
      "buildsystem": "simple",
      "build-commands": [
        "install -Dm755 myapp.sh /app/bin/myapp",
        "install -Dm644 myapp.jar /app/lib/myapp.jar",
        "install -Dm644 com.example.MyApp.desktop /app/share/applications/com.example.MyApp.desktop",
        "install -Dm644 com.example.MyApp.png /app/share/icons/hicolor/256x256/apps/com.example.MyApp.png",
        "install -Dm644 com.example.MyApp.metainfo.xml /app/share/metainfo/com.example.MyApp.metainfo.xml"
      ],
      "sources": [
        { "type": "file", "path": "target/myapp.jar" },
        { "type": "file", "path": "src/main/resources/flatpak/myapp.sh" },
        { "type": "file", "path": "src/main/resources/flatpak/com.example.MyApp.desktop" },
        { "type": "file", "path": "src/main/resources/flatpak/com.example.MyApp.png" },
        { "type": "file", "path": "src/main/resources/flatpak/com.example.MyApp.metainfo.xml" }
      ]
    }
  ]
}
```

### 5.3 Flatpak Finish Args (Permissions)

| Arg | Purpose |
|-----|---------|
| `--socket=x11` | X11 windowing system |
| `--socket=wayland` | Wayland display server |
| `--share=network` | Network access |
| `--filesystem=home` | Read/write user home directory |
| `--device=dri` | GPU (Direct Rendering Infrastructure) for JavaFX hardware acceleration |
| `--share=ipc` | Shared memory for X11 |
| `--socket=pulseaudio` | Audio playback |
| `--filesystem=/media` | Removable media access |
| `--talk-name=org.cups.cupsd` | Printing |

### 5.4 Launcher Script

The `myapp.sh` launcher script sets up the Java environment inside the Flatpak sandbox:

```bash
#!/bin/bash
exec /app/jdk/bin/java \
  --module-path /app/jdk/javafx \
  --add-modules javafx.controls,javafx.fxml,javafx.graphics \
  -jar /app/lib/myapp.jar "$@"
```

### 5.5 Building and Publishing

```bash
# Install flatpak and the GNOME SDK
flatpak install flathub org.gnome.Platform//46 org.gnome.Sdk//46

# Build the Flatpak
flatpak-builder build-dir com.example.MyApp.json

# Install locally for testing
flatpak build-install build-dir com.example.MyApp

# Publish to Flathub
flatpak-builder --repo=flathub --gpg-sign=<key-id> build-dir com.example.MyApp.json
```

### 5.6 Flatpak Auto-Update

Flatpak updates automatically via the system's Flatpak update mechanism:
```bash
# Manual update
flatpak update com.example.MyApp

# Check for updates
flatpak remote-ls --updates flathub
```

## 6. Distribution Channel Decision Matrix

| Factor | Direct Download | Microsoft Store | Mac App Store | Snap | Flatpak |
|--------|----------------|-----------------|---------------|------|---------|
| Setup effort | Low | Medium | High | Medium | Medium |
| Code signing | Optional | Required (EV) | Required | Optional | Optional |
| Auto-update | Custom (UpdateChecker) | Store-managed | Store-managed | snapd-managed | Flatpak-managed |
| Sandboxing | None | MSIX sandbox | App Sandbox | Strict | Strict |
| Revenue share | 0% | 12-15% | 15-30% | 0% | 0% |
| Discovery | Your website | Store search | Store search | Snap Store | Flathub |
| Linux distro support | All (manual) | N/A | N/A | Most (snapd) | Most (flatpak) |
| Update rollback | Manual (custom) | Automatic | Automatic | Automatic | Manual |

## 7. Multi-Channel Distribution Strategy

For maximum reach, a JavaFX app can be distributed through multiple channels simultaneously:

1. **Primary**: Direct download (your website) with UpdateChecker auto-update — full control, no store fees
2. **Secondary**: Microsoft Store (Windows) + Mac App Store (macOS) — for users who prefer store-installed apps
3. **Linux**: Snap + Flatpak — covers most Linux distributions

> **Version synchronization**: When distributing through multiple channels, ensure all channels have the same version available within 24-48 hours. The update manifest (for direct download) should list the latest version, and store submissions should be submitted in parallel.

> **Update checker disable for store builds**: When building for store distribution, disable the in-app `UpdateChecker` (the store handles updates). Use a build flag: `-Dstore.distribution=true` and conditionally skip update checks in `App.java`.

## 8. CI/CD Integration for Store Submission

### 8.1 GitHub Actions — MSIX Build + Store Publish

```yaml
jobs:
  build-msix:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build MSIX
        run: |
          mvn clean package -DskipTests
          jpackage --name MyApp --type msix --input target/libs \
            --main-jar myapp.jar --main-class com.example.App \
            --app-version ${{ github.ref_name }} \
            --win-upgrade-uuid "12345678-1234-1234-1234-123456789012" \
            --icon src/main/resources/icons/app.ico \
            --vendor "MyCompany"
      - name: Sign MSIX
        run: |
          signtool sign /fd SHA256 /f ${{ secrets.MSIX_CERT_PFX }} \
            /p ${{ secrets.MSIX_CERT_PASSWORD }} \
            /tr http://timestamp.digicert.com MyApp.msix
      - name: Upload MSIX artifact
        uses: actions/upload-artifact@v4
        with:
          name: MyApp-MSIX
          path: MyApp.msix
```

### 8.2 Snap Build in CI

```yaml
jobs:
  build-snap:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: snapcore/action-build@v1
        id: snapcraft
      - uses: snapcore/action-publish@v1
        env:
          SNAPCRAFT_STORE_CREDENTIALS: ${{ secrets.SNAP_STORE_TOKEN }}
        with:
          snap: ${{ steps.snapcraft.outputs.snap }}
          release: stable
```

## 9. Cross-References

- `code-signing.md` — Code signing certificates and notarization (required for store submission)
- `auto-update.md` — In-app update checker (disable for store builds; use for direct download)
- `rollback-strategy.md` — Rollback strategy for failed updates (store builds use store-managed rollback)
- `ci-cd-pipeline.md` — CI/CD pipeline generation (integrate store build and publish steps)
- `release-management.md` — Version management (store versions must match release versions)
