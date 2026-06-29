# Deployment Report

> **Project**: {{PROJECT_NAME}}  
> **Version**: {{PROJECT_VERSION}}  
> **Deployment Scope**: {{DEPLOY_SCOPE}} (Full Deployment / CI/CD Only / Release Only / Signing Only / Monitoring Only)  
> **Generated At**: {{TIMESTAMP}}

## 1. Deployment Summary

| Item | Value |
|------|-------|
| CI/CD Platform | {{CI_CD_PLATFORM}} (GitHub Actions / GitLab CI / Both) |
| Target Platforms | {{TARGET_PLATFORMS}} (Windows / macOS / Linux) |
| Dimensions Activated | {{DIMENSIONS}} |
| Artifacts Generated | {{ARTIFACT_COUNT}} files |
| Required Secrets | {{SECRET_COUNT}} |
| Conclusion | {{CONCLUSION}} (Pass / Pass with warnings / Fail) |

## 2. CI/CD Pipeline

**Workflow File**: `{{CI_CD_WORKFLOW_FILE}}`

**Build Matrix**:

| Platform | Runner | Installer Format | Toolchain |
|----------|--------|-----------------|-----------|
| Windows | windows-latest | .exe / .msi | WiX Toolset / Inno Setup |
| macOS | macos-latest | .dmg / .pkg | Xcode Command Line Tools |
| Linux | ubuntu-latest | .deb / .rpm | dpkg-deb / rpm-build |

**Pipeline Steps**:
1. Checkout code
2. Setup JDK {{JDK_VERSION}}
3. Cache Maven repository
4. Build: `mvn clean package -DskipTests`
5. Package: `jpackage` with platform-specific flags
6. Upload artifacts

## 3. Release Management

**Release Workflow**: `{{RELEASE_WORKFLOW_FILE}}`  
**Version Bump Script**: `{{VERSION_SCRIPT}}`

**Version**: Current `{{CURRENT_VERSION}}` → Next `{{NEXT_VERSION}}`

**Release Trigger**: Git tag push (`v*.*.*`)

**Release Assets**:
| Platform | Asset Name | Format |
|----------|-----------|--------|
| Windows | `{{APP_NAME}}-{{VERSION}}.exe` | EXE installer |
| macOS | `{{APP_NAME}}-{{VERSION}}.dmg` | DMG image |
| Linux | `{{APP_NAME}}-{{VERSION}}.deb` | DEB package |

## 4. Code Signing & Notarization

### Windows Signing
- **Script**: `scripts/sign-windows.sh`
- **Tool**: signtool (Windows SDK)
- **Timestamp**: http://timestamp.digicert.com (RFC 3161)
- **Hash Algorithm**: SHA-256
- **Required Secrets**: `WINDOWS_CERT_PFX`, `WINDOWS_CERT_PASSWORD`

### macOS Notarization
- **Script**: `scripts/notarize-macos.sh`
- **Tool**: xcrun notarytool + xcrun stapler
- **Entitlements**: `{{ENTITLEMENTS_FILE}}`
- **Required Secrets**: `APPLE_ID`, `APPLE_TEAM_ID`, `APPLE_APP_PASSWORD`

## 5. Auto-Update

**Strategy**: {{UPDATE_STRATEGY}} (Server-side manifest / GitHub Releases API / Embedded)

**Update Checker**: `{{UPDATE_CHECKER_CLASS}}`  
**Update Config**: `src/main/resources/update-config.json`

```json
{{UPDATE_CONFIG_JSON}}
```

**Update Manifest Template**: `docs/update-manifest-template.json`

## 6. Runtime Monitoring

### Logging
- **Config**: `src/main/resources/logback.xml`
- **Log Location**: `~/.{{APP_NAME}}/logs/`
- **Levels**: Console DEBUG (dev), File INFO (production)
- **Rolling Policy**: 10MB max, 30 days history, 1GB total cap

### Crash Reporting
- **Handler**: `{{CRASH_HANDLER_CLASS}}`
- **Crash Reports**: `~/.{{APP_NAME}}/crashes/`
- **Remote Reporting**: {{REMOTE_REPORTING}} (enabled / disabled)

### Performance Metrics
- **Collector**: `{{METRICS_COLLECTOR_CLASS}}`
- **Metrics File**: `~/.{{APP_NAME}}/metrics/`
- **JMX Exposure**: {{JMX_ENABLED}} (enabled / disabled)

## 7. Generated Artifacts

| Artifact | Path | Type |
|----------|------|------|
| {{ARTIFACT_NAME}} | `{{ARTIFACT_PATH}}` | {{ARTIFACT_TYPE}} |

## 8. Setup Instructions

### Required Secrets

Add the following secrets to your {{CI_CD_PLATFORM}} repository:

| Secret Name | Description |
|-------------|-------------|
| {{SECRET_NAME}} | {{SECRET_DESCRIPTION}} |

### Manual Setup Steps

1. {{SETUP_STEP}}
2. {{SETUP_STEP}}
3. {{SETUP_STEP}}

## 9. Warnings

{{WARNINGS}}

(If no warnings, output "No warnings.")

## 10. Loop State

- **Next Action**: {{NEXT_ACTION}} (shipped | standalone_complete | redeploy)
- **Deploy Phase**: {{DEPLOY_PHASE}} (post-delivery | standalone | redeploy)
