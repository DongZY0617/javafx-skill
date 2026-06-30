# Rollback Strategy

This document defines the rollback strategy for JavaFX desktop application deployments. It covers version pinning, post-release health checks, automatic rollback triggers, the rollback mechanism implementation, and the Runbook for manual rollback operations. It serves as the reference for `javafx-deployer`'s Rollback Strategy dimension.

## 1. Overview

When a new version of a JavaFX application is released and causes unexpected crashes or regressions, a rollback strategy ensures users can quickly return to the last known-good version. This document covers two rollback approaches:

1. **Automatic rollback** (for direct-download apps with custom UpdateChecker): The app detects post-update failures and automatically reverts to the previous version
2. **Manual rollback** (for all distribution channels): A documented Runbook for operators to execute a manual rollback via the update manifest

Store-distributed apps (Microsoft Store, Mac App Store, Snap) have store-managed rollback and do not need custom rollback logic — see `distribution-channels.md` § 4.5 for Snap's automatic rollback.

## 2. Version Pinning

### 2.1 Concept

Version pinning prevents users from receiving a known-bad update. When a critical bug is discovered in a released version, the publisher can "pin" the update manifest to the last known-good version, effectively halting the rollout of the bad version.

### 2.2 Update Manifest Pinning

The update manifest includes a `pinned_version` field that overrides `latest_version` when set:

```json
{
  "latest_version": "1.4.0",
  "pinned_version": "1.3.2",
  "pinned_reason": "Critical startup crash in 1.4.0 on Windows 11 — investigation in progress",
  "minimum_version": "1.0.0",
  "release_notes": "1.3.2: Stable release with dark mode and PDF export.",
  "rollback_enabled": true,
  "rollback_version": "1.3.2",
  "platforms": {
    "windows": { "url": "https://<your-cdn-domain>/myapp/1.3.2/MyApp.msi", "size": 47234496, "sha256": "8f86d081..." },
    "macos":   { "url": "https://<your-cdn-domain>/myapp/1.3.2/MyApp.dmg", "size": 51111360, "sha256": "b3f5b2c9..." },
    "linux":   { "url": "https://<your-cdn-domain>/myapp/1.3.2/myapp.deb", "size": 38087616, "sha256": "d7e1d4f8..." }
  }
}
```

| Field | Type | Purpose |
|-------|------|---------|
| `pinned_version` | string (nullable) | When non-null, the UpdateChecker treats this as the latest version instead of `latest_version` |
| `pinned_reason` | string | Human-readable explanation of why the version is pinned |
| `rollback_enabled` | boolean | Whether automatic rollback is enabled for this release |
| `rollback_version` | string | The version to roll back to if automatic rollback triggers |

### 2.3 UpdateChecker Pinning Logic

```java
public Optional<UpdateInfo> checkForUpdate() throws IOException, InterruptedException {
    UpdateManifest manifest = fetchManifest();

    // Determine the effective target version
    String targetVersion = manifest.pinned_version != null
        ? manifest.pinned_version
        : manifest.latest_version;

    if (compareVersions(targetVersion, currentVersion) <= 0) {
        return Optional.empty(); // up to date (or pinned to current/older)
    }
    // ... return update info for targetVersion
}
```

> **Pinning is a halt, not a downgrade**: Pinning stops new updates from going out but does not automatically downgrade users who already installed the bad version. For automatic downgrade, see § 3 (Automatic Rollback).

## 3. Automatic Rollback

### 3.1 Concept

Automatic rollback monitors the application's health after an update. If the app crashes within a configurable grace period after updating, it automatically rolls back to the previous version on the next launch.

### 3.2 Rollback State File

The UpdateChecker writes a state file after each successful update to track whether a rollback is needed:

```json
{
  "updated_at": "2026-06-30T10:30:00Z",
  "previous_version": "1.3.2",
  "current_version": "1.4.0",
  "rollback_version": "1.3.2",
  "crash_count": 0,
  "grace_period_hours": 24,
  "crash_threshold": 3,
  "rollback_triggered": false,
  "rollback_url": null
}
```

| Field | Type | Purpose |
|-------|------|---------|
| `updated_at` | ISO timestamp | When the update was applied |
| `previous_version` | string | The version before the update |
| `current_version` | string | The version after the update |
| `rollback_version` | string | The version to roll back to (from manifest `rollback_version`) |
| `crash_count` | integer | Number of crashes since the update |
| `grace_period_hours` | integer | Window after update during which crashes trigger rollback (default: 24) |
| `crash_threshold` | integer | Number of crashes within grace period to trigger rollback (default: 3) |
| `rollback_triggered` | boolean | Whether rollback has been triggered (prevents re-triggering) |
| `rollback_url` | string (nullable) | URL of the rollback installer (from manifest `platforms[platform].url`) |

### 3.3 Crash Detection Integration

The `CrashHandler` (see `runtime-monitoring.md`) is extended to increment the rollback crash counter:

```java
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final Path ROLLBACK_STATE = Paths.get(
        System.getProperty("user.home"), ".myapp", "rollback-state.json");

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // 1. Write crash report (existing behavior)
        writeCrashReport(thread, throwable);

        // 2. Check if rollback tracking is active
        RollbackState state = RollbackState.load(ROLLBACK_STATE);
        if (state == null || state.rollback_triggered) {
            return; // No update tracking or already rolled back
        }

        // 3. Check if within grace period
        if (state.isWithinGracePeriod()) {
            state.crash_count++;
            state.save(ROLLBACK_STATE);

            // 4. Check if crash threshold exceeded
            if (state.crash_count >= state.crash_threshold) {
                triggerRollback(state);
            }
        }
    }

    private void triggerRollback(RollbackState state) {
        state.rollback_triggered = true;
        state.save(ROLLBACK_STATE);

        // Fetch rollback installer URL from manifest
        String rollbackUrl = fetchRollbackUrl(state.rollback_version);

        // Download rollback installer silently
        Path installer = downloadInstaller(rollbackUrl);

        // Launch installer and exit
        new ProcessBuilder(installer.toString()).inheritIO().start();
        Platform.exit();
        System.exit(1);
    }
}
```

### 3.4 Grace Period Logic

The grace period prevents false rollbacks from pre-existing bugs:

```java
public boolean isWithinGracePeriod() {
    if (updated_at == null) return false;
    LocalDateTime updated = LocalDateTime.parse(updated_at, ISO_DATE_TIME);
    LocalDateTime expiry = updated.plusHours(grace_period_hours);
    return LocalDateTime.now().isBefore(expiry);
}
```

- **Default grace period**: 24 hours after update — if the app crashes 3+ times within 24 hours, rollback triggers
- **Grace period expiry**: After the grace period, crashes are treated as normal bugs (no automatic rollback)
- **Manual override**: Users can force a rollback via the "About" dialog → "Rollback to previous version" button (see § 5)

### 3.5 Post-Rollback Behavior

After a rollback is triggered:
1. The rollback installer is downloaded and launched (silently or with confirmation dialog)
2. The `rollback-state.json` is updated with `rollback_triggered: true`
3. On next launch, the UpdateChecker detects the rolled-back version and **does not** prompt for the bad update again (until the bad version is superseded by a new release)
4. The `pinned_version` in the server manifest ensures other users don't receive the bad update either

### 3.6 Rollback Safety Guard

To prevent infinite rollback loops (rollback → crash → rollback → ...):

```java
public boolean shouldAttemptRollback(RollbackState state) {
    if (state.rollback_triggered) {
        // Already rolled back — don't attempt again
        return false;
    }
    if (state.current_version.equals(state.rollback_version)) {
        // We're already on the rollback version — something is wrong
        return false;
    }
    if (state.crash_count >= state.crash_threshold * 2) {
        // Excessive crashes even after threshold — abort rollback to prevent loop
        return false;
    }
    return true;
}
```

## 4. Post-Release Health Check

### 4.1 Concept

A post-release health check is a CI/CD step that monitors the application's health after a release is published. It checks crash reports, user feedback, and telemetry to detect regressions early.

### 4.2 CI/CD Health Check Workflow

Add a health check job to the release workflow that runs 1 hour, 6 hours, and 24 hours after release:

```yaml
jobs:
  post-release-health-check:
    runs-on: ubuntu-latest
    needs: release
    if: github.ref_type == 'tag'
    steps:
      - name: Wait 1 hour before first check
        run: sleep 3600

      - name: Check crash report endpoint
        run: |
          CRASH_COUNT=$(curl -s "https://<your-api-domain>/crashes?version=${{ github.ref_name }}&hours=1" | jq '.count')
          echo "Crash count in last hour: $CRASH_COUNT"
          if [ "$CRASH_COUNT" -gt 10 ]; then
            echo "::warning::High crash count detected — consider pinning to previous version"
            # Trigger version pinning via API
            curl -X POST "https://<your-api-domain>/manifest/pin" \
              -H "Authorization: Bearer ${{ secrets.MANIFEST_API_KEY }}" \
              -d "{\"pinned_version\": \"$(git describe --abbrev=0 ${{ github.ref_name }}^)\"}"
          fi

      - name: Check download success rate
        run: |
          SUCCESS_RATE=$(curl -s "https://<your-api-domain>/downloads/success-rate?version=${{ github.ref_name }}" | jq '.rate')
          echo "Download success rate: $SUCCESS_RATE"
          if (( $(echo "$SUCCESS_RATE < 0.95" | bc -l) )); then
            echo "::warning::Low download success rate — possible installer corruption"
          fi

      - name: Schedule 6-hour check
        if: success()
        run: echo "Scheduling 6-hour health check..."
```

### 4.3 Health Check Metrics

| Metric | Source | Alert Threshold | Action |
|--------|--------|----------------|--------|
| Crash count per hour | Crash report API | > 10/hour | Pin previous version |
| Crash rate (crashes/installs) | Telemetry | > 5% | Pin previous version |
| Download success rate | CDN logs | < 95% | Check CDN health |
| App launch success rate | Telemetry | < 98% | Pin previous version |
| User-reported issues | GitHub Issues / Support | > 5 in 24h | Investigate and consider pin |
| Average startup time | Telemetry | > 5s (regression) | Investigate performance regression |

### 4.4 Telemetry Collection (Optional)

If the app collects telemetry (with user consent), the following metrics should be sent to the health monitoring endpoint:

```java
public class HealthMetrics {
    private String appVersion;
    private String osName;
    private String osVersion;
    private long startupTimeMs;
    private boolean launchedSuccessfully;
    private int crashCountSinceUpdate;
    private String previousVersion; // null if no recent update

    public void sendToHealthEndpoint() {
        HttpClient client = HttpClient.newHttpClient();
        String json = toJson();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://<your-api-domain>/health"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }
}
```

> **Privacy note**: Telemetry must be opt-in with a clear privacy policy. Include a checkbox in the first-run wizard: "Send anonymous usage data to help improve the app". See `runtime-monitoring.md` § 3 for consent management.

## 5. Manual Rollback Runbook

### 5.1 When to Execute Manual Rollback

Execute the manual rollback Runbook when:
- Automatic rollback did not trigger (crash threshold not met, but issue is confirmed)
- A non-crash regression is detected (e.g., data corruption, feature broken)
- User reports a critical issue confirmed by the development team
- A security vulnerability is discovered in a dependency bundled with the release

### 5.2 Runbook: Pin Previous Version

**Objective**: Halt the rollout of the current version and redirect all update checks to the previous known-good version.

**Prerequisites**:
- Access to the update manifest server (CDN or object storage)
- The previous version's installer files are still hosted at their original URLs

**Steps**:

1. **Identify the rollback target version**:
   ```bash
   # Get the previous tag
   git describe --abbrev=0 v1.4.0^
   # Output: v1.3.2
   ```

2. **Update the manifest JSON** on the server:
   ```json
   {
     "latest_version": "1.4.0",
     "pinned_version": "1.3.2",
     "pinned_reason": "Critical data export bug discovered in 1.4.0 — rollback to 1.3.2 while fix is prepared",
     "rollback_enabled": true,
     "rollback_version": "1.3.2",
     "platforms": {
       "windows": { "url": "https://<your-cdn-domain>/myapp/1.3.2/MyApp.msi", ... },
       "macos":   { "url": "https://<your-cdn-domain>/myapp/1.3.2/MyApp.dmg", ... },
       "linux":   { "url": "https://<your-cdn-domain>/myapp/1.3.2/myapp.deb", ... }
     }
   }
   ```

3. **Verify the manifest update**:
   ```bash
   curl -s https://<your-cdn-domain>/myapp/update-manifest.json | jq .pinned_version
   # Expected output: "1.3.2"
   ```

4. **Notify users** (optional): Post a notice on the app's website / social media:
   > "We've identified an issue in version 1.4.0 affecting data export. We've rolled back updates to version 1.3.2. If you've already updated to 1.4.0, you can download 1.3.2 from [link] or wait for the automatic rollback."

5. **Monitor**: Watch the crash report endpoint and download metrics to confirm users are receiving 1.3.2.

### 5.3 Runbook: Force Rollback for All Users

**Objective**: Force all users (including those who already updated) to roll back to the previous version.

**Prerequisites**:
- The update manifest server is accessible
- The previous version's installer files are still hosted

**Steps**:

1. **Update the manifest** to force rollback:
   ```json
   {
     "latest_version": "1.3.2",
     "pinned_version": null,
     "minimum_version": "1.3.2",
     "rollback_enabled": true,
     "rollback_version": "1.3.2",
     "platforms": {
       "windows": { "url": "https://<your-cdn-domain>/myapp/1.3.2/MyApp.msi", ... },
       ...
     }
   }
   ```
   Setting `minimum_version` to `1.3.2` forces all users on 1.4.0 to update (downgrade) to 1.3.2 — the UpdateChecker treats `minimum_version` as a forced update that cannot be skipped.

2. **Verify** the manifest:
   ```bash
   curl -s https://<your-cdn-domain>/myapp/update-manifest.json | jq '{latest, minimum: .minimum_version}'
   # Expected: {"latest": "1.3.2", "minimum": "1.3.2"}
   ```

3. **Monitor** the download metrics for 1.3.2 installers — expect a spike as all 1.4.0 users receive the forced downgrade.

### 5.4 Runbook: Store-Managed Rollback (Microsoft Store / Mac App Store)

Store-distributed apps cannot use the manifest-based rollback. Instead:

1. **Microsoft Store**: Go to Partner Center → Your app → Packages → Remove the bad version's package. The Store will offer the previous version to new users. Already-installed users must uninstall and reinstall manually.
2. **Mac App Store**: Go to App Store Connect → Your app → App Versions → Find the bad version → "Remove from Sale". Previous version becomes the current version.
3. **Snap**: Revert the Snap Store channel to the previous revision: `snapcraft push myapp_1.3.2_amd64.snap --release=stable` (re-push the old snap to the stable channel).

### 5.5 Runbook: Post-Rollback Verification

After executing a rollback:

1. **Verify the manifest** is correctly serving the rollback version
2. **Test the rollback installer** on all platforms (Windows, macOS, Linux) to ensure it installs cleanly over the bad version
3. **Monitor crash reports** — crash rate should drop to pre-1.4.0 levels within 24 hours
4. **Prepare the fix**: Create a hotfix branch from the rollback target version, fix the bug, and release as 1.4.1 (not 1.4.0 re-release — never re-release a pulled version number)
5. **Update the manifest** to point to 1.4.1 once the fix is validated: set `latest_version: "1.4.1"`, clear `pinned_version`

## 6. Rollback Configuration

### 6.1 update-config.json Extensions

```json
{
  "check_on_startup": true,
  "check_interval_hours": 24,
  "manifest_url": "https://<your-cdn-domain>/myapp/update-manifest.json",
  "allow_skip_version": true,
  "rollback_enabled": true,
  "rollback_grace_period_hours": 24,
  "rollback_crash_threshold": 3,
  "rollback_silent": false
}
```

| Field | Type | Default | Purpose |
|-------|------|---------|---------|
| `rollback_enabled` | boolean | true | Enable automatic rollback on post-update crashes |
| `rollback_grace_period_hours` | integer | 24 | Hours after update during which crashes count toward rollback |
| `rollback_crash_threshold` | integer | 3 | Number of crashes in grace period to trigger rollback |
| `rollback_silent` | boolean | false | If true, rollback downloads and installs silently. If false, shows a confirmation dialog: "The app has encountered errors after the recent update. Would you like to revert to version X?" |

### 6.2 CI/CD Health Check Configuration

```json
{
  "health_check_enabled": true,
  "health_check_intervals_hours": [1, 6, 24],
  "crash_rate_threshold": 0.05,
  "launch_failure_threshold": 0.02,
  "auto_pin_on_alert": true,
  "manifest_api_url": "https://<your-api-domain>/manifest",
  "manifest_api_key_secret": "MANIFEST_API_KEY"
}
```

## 7. Rollback Checklist

Before enabling rollback for a release:

- [ ] Previous version's installer files are still hosted on CDN
- [ ] Update manifest includes `rollback_enabled`, `rollback_version`, and `rollback_url` fields
- [ ] `rollback-state.json` path is configured and writable by the app
- [ ] CrashHandler is integrated with rollback crash counting
- [ ] Grace period and crash threshold are configured in `update-config.json`
- [ ] CI/CD health check workflow is configured with alert thresholds
- [ ] Manual rollback Runbook is documented and accessible to the operations team
- [ ] Store-distributed builds have store rollback procedure documented (Partner Center / App Store Connect / Snap Store)

## 8. Cross-References

- `auto-update.md` — UpdateChecker implementation (rollback extends UpdateChecker with crash monitoring)
- `runtime-monitoring.md` — CrashHandler implementation (extended for rollback crash counting)
- `distribution-channels.md` — Store-managed rollback (Snap auto-rollback, Store package removal)
- `ci-cd-pipeline.md` — CI/CD pipeline generation (health check job integration)
- `release-management.md` — Version management (never re-release a pulled version number)
