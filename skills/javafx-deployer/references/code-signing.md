# Code Signing and Notarization

This document defines the rules for code signing Windows installers and signing/notarizing macOS applications for JavaFX desktop releases. Signing is required for SmartScreen reputation on Windows and Gatekeeper acceptance on macOS.

## 1. Windows Code Signing

### Certificate Types

| Type | Storage | Trust level | Notes |
|------|---------|-------------|-------|
| EV (Extended Validation) | Hardware token (USB HSM) | Highest; instant SmartScreen reputation | Cannot be exported as a file; signing must run on a machine with the token attached |
| OV (Organization Validation) | `.pfx` file | Standard; requires reputation buildup | Portable, suitable for CI/CD via base64 secret |

### signtool Command
Use `signtool` from the Windows SDK to sign with an OV certificate. The RFC 3161 timestamp server is mandatory so the signature remains valid after certificate expiry.

```bash
signtool sign /f cert.pfx /p "$WINDOWS_CERT_PASSWORD" \
  /tr http://timestamp.digicert.com /td sha256 /fd sha256 \
  /a installer.exe
```

| Flag | Meaning |
|------|---------|
| `/f` | Path to the PFX certificate file |
| `/p` | Certificate password |
| `/tr` | RFC 3161 timestamp server URL |
| `/td` | Timestamp digest algorithm (sha256) |
| `/fd` | File digest algorithm (sha256) |
| `/a` | Automatically choose the best certificate |

### signtool Verify
Always verify the signature after signing.

```bash
signtool verify /pa /v installer.exe
```

`/pa` uses the Default Authenticode Verification Policy; `/v` prints verbose output including the certificate chain.

### CI/CD Integration
Store the PFX as a base64-encoded secret so it can be decoded at build time. Never commit the raw `.pfx` file.

```yaml
- name: Decode certificate
  run: |
    echo "$WINDOWS_CERT_PFX" | base64 --decode > cert.pfx
- name: Sign installer
  run: |
    signtool sign /f cert.pfx /p "$WINDOWS_CERT_PASSWORD" \
      /tr http://timestamp.digicert.com /td sha256 /fd sha256 \
      target/MyApp.msi
- name: Verify signature
  run: signtool verify /pa /v target/MyApp.msi
```

### WiX Integration
When producing a WiX bootstrapper (`.exe` wrapping an `.msi`), sign both the inner `.msi` and the outer `.exe` bootstrapper. Sign the `.msi` first, then build the bootstrapper, then sign the `.exe`. This ensures the installer chain is fully trusted.

## 2. macOS Code Signing and Notarization

### Certificates
Two certificates are required for a complete macOS distribution:

| Certificate | Purpose | Output |
|-------------|---------|--------|
| Developer ID Application | Signs the `.app` bundle | `MyApp.app` |
| Developer ID Installer | Signs the `.pkg` installer | `MyApp.pkg` |

### codesign Command
Sign the application bundle deeply with the hardened runtime enabled.

```bash
codesign --deep --force --verify --verbose \
  --sign "Developer ID Application: NAME (TEAMID)" \
  --options runtime \
  --entitlements Entitlements.plist \
  MyApp.app
```

### Entitlements.plist
JavaFX applications require specific entitlements because the JVM performs JIT compilation and loads dynamic libraries.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>com.apple.security.cs.allow-jit</key>
  <true/>
  <key>com.apple.security.cs.disable-library-validation</key>
  <true/>
  <key>com.apple.security.cs.allow-unsigned-executable-memory</key>
  <true/>
  <key>com.apple.security.network.client</key>
  <true/>
</dict>
</plist>
```

| Entitlement | Reason |
|-------------|--------|
| `allow-jit` | JVM JIT compiler writes executable memory |
| `disable-library-validation` | Loads unsigned JNI/native libraries bundled with JavaFX |
| `allow-unsigned-executable-memory` | Required by some JVM/GraalVM runtimes |
| `network.client` | Outbound HTTP for auto-update and telemetry |

### Notarization
Submit the packaged `.dmg` (or `.zip` of the `.app`) to Apple's notary service using `notarytool`. This requires an app-specific password generated from the Apple ID account.

```bash
xcrun notarytool submit MyApp.dmg \
  --apple-id "$APPLE_ID" \
  --team-id "$APPLE_TEAM_ID" \
  --password "$APPLE_APP_PASSWORD" \
  --wait
```

`--wait` blocks until Apple finishes processing. The notarization status is then polled and reported.

### Stapling
Staple the notarization ticket to the distribution so it can be verified offline.

```bash
xcrun stapler staple MyApp.dmg
```

### Verification
Confirm the staple and Gatekeeper acceptance before release.

```bash
xcrun stapler validate MyApp.dmg
spctl --assess --verbose=4 MyApp.dmg
```

A successful `spctl` output reads `source=Notarized Developer ID`.

## 3. CI/CD Secret Management

All signing material must be stored as protected CI/CD secrets and never echoed to logs.

| Secret name | Platform | Purpose |
|-------------|----------|---------|
| `WINDOWS_CERT_PFX` | Windows | Base64-encoded PFX certificate |
| `WINDOWS_CERT_PASSWORD` | Windows | PFX password |
| `APPLE_ID` | macOS | Apple ID email |
| `APPLE_TEAM_ID` | macOS | Developer team identifier |
| `APPLE_APP_PASSWORD` | macOS | App-specific password for notarytool |
| `MACOS_CERTIFICATE_P12` | macOS | Base64-encoded signing certificate (CI keychain) |
| `MACOS_CERTIFICATE_PASSWORD` | macOS | P12 password |

On macOS CI runners, import the P12 into a temporary keychain, set it as the default, and unlock it before signing.

```bash
security create-keychain -p "$KEYCHAIN_PASSWORD" build.keychain
security default-keychain -s build.keychain
security unlock-keychain -p "$KEYCHAIN_PASSWORD" build.keychain
security import cert.p12 -k build.keychain -P "$MACOS_CERTIFICATE_PASSWORD" -T /usr/bin/codesign
security set-partition-list -S apple-tool:,apple: -s -k "$KEYCHAIN_PASSWORD" build.keychain
```

## 4. Signing Script Template (Windows)

`sign-windows.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail
INSTALLER="${1:?usage: sign-windows.sh <installer>}"
CERT="${WINDOWS_CERT_PFX:-cert.pfx}"
echo "$WINDOWS_CERT_PFX" | base64 --decode > cert.pfx
signtool sign /f cert.pfx /p "$WINDOWS_CERT_PASSWORD" \
  /tr http://timestamp.digicert.com /td sha256 /fd sha256 /a "$INSTALLER"
signtool verify /pa /v "$INSTALLER"
rm -f cert.pfx
echo "Signed and verified: $INSTALLER"
```

## 5. Signing Script Template (macOS)

`notarize-macos.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail
DMG="${1:?usage: notarize-macos.sh <dmg>}"
APP_CERT="Developer ID Application: ${APPLE_DEVELOPER_NAME} (${APPLE_TEAM_ID})"

codesign --deep --force --verify --verbose \
  --sign "$APP_CERT" --options runtime \
  --entitlements Entitlements.plist MyApp.app

xcrun notarytool submit "$DMG" \
  --apple-id "$APPLE_ID" --team-id "$APPLE_TEAM_ID" \
  --password "$APPLE_APP_PASSWORD" --wait

xcrun stapler staple "$DMG"
xcrun stapler validate "$DMG"
spctl --assess --verbose=4 "$DMG"
echo "Notarized and stapled: $DMG"
```

## 6. Signing Order Summary

The correct order of operations for a fully trusted release:

1. Build the unsigned artifact (`jpackage`).
2. Sign the artifact (Windows `signtool` / macOS `codesign`).
3. Package for distribution (create `.dmg` on macOS).
4. Notarize (macOS only) and staple the ticket.
5. Verify the signature and notarization status.
6. Upload the signed artifact to the release.
