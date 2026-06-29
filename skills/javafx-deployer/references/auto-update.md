# Auto-Update Mechanism

This document defines the rules for implementing an in-app auto-update mechanism for JavaFX desktop applications. It covers update strategy selection, manifest formats, the `UpdateChecker` implementation, user-facing dialogs, scheduled checks, and integration into the application lifecycle.

## 1. Update Strategy Comparison

| Strategy | Server required | Best for | Complexity |
|----------|-----------------|----------|------------|
| Server-side manifest | Yes (static host) | Full control of release metadata, fine-grained targeting | Low |
| GitHub Releases API | No | Open-source apps, no infra to maintain | Low |
| Embedded server (P2P) | No | Air-gapped/LAN fleet updates | High |

The default and recommended approach is the **server-side manifest**: a single static JSON file hosted on a CDN or object store. It decouples update availability from the CI provider and supports arbitrary version metadata.

## 2. Update Manifest JSON Format

The manifest describes the latest release, the minimum supported version (below which a forced upgrade is required), release notes, and per-platform download details.

```json
{
  "latest_version": "1.4.0",
  "minimum_version": "1.0.0",
  "release_notes": "Added dark mode and fixed PDF export crash.",
  "platforms": {
    "windows": { "url": "https://cdn.example.com/myapp/1.4.0/MyApp.msi", "size": 48234496, "sha256": "9f86d081..." },
    "macos":   { "url": "https://cdn.example.com/myapp/1.4.0/MyApp.dmg", "size": 52111360, "sha256": "a3f5b2c9..." },
    "linux":   { "url": "https://cdn.example.com/myapp/1.4.0/myapp.deb", "size": 39087616, "sha256": "c7e1d4f8..." }
  }
}
```

## 3. update-config.json Format

A local configuration file controls update behavior. It ships with the app and may be overridden by user preferences.

```json
{
  "check_on_startup": true,
  "check_interval_hours": 24,
  "manifest_url": "https://cdn.example.com/myapp/update-manifest.json",
  "allow_skip_version": true
}
```

| Field | Type | Purpose |
|-------|------|---------|
| `check_on_startup` | boolean | Run a check when the app launches |
| `check_interval_hours` | number | Minimum hours between background checks |
| `manifest_url` | string | URL of the update manifest |
| `allow_skip_version` | boolean | Permit the user to skip a specific version |

## 4. UpdateChecker.java Implementation

The checker fetches the manifest, compares versions, detects the platform, downloads the installer with progress, verifies the SHA-256 checksum, and launches the installer.

```java
public class UpdateChecker {

    private final String manifestUrl;
    private final String currentVersion;

    public UpdateChecker(String manifestUrl, String currentVersion) {
        this.manifestUrl = manifestUrl;
        this.currentVersion = currentVersion;
    }

    public Optional<UpdateInfo> checkForUpdate() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> resp = client.send(
            HttpRequest.newBuilder(URI.create(manifestUrl)).build(),
            HttpResponse.BodyHandlers.ofString());
        UpdateManifest manifest = parseManifest(resp.body());

        if (compareVersions(manifest.latest_version, currentVersion) <= 0) {
            return Optional.empty(); // up to date
        }
        String platform = detectPlatform();
        UpdateAsset asset = manifest.platforms.get(platform);
        return Optional.of(new UpdateInfo(manifest.latest_version,
            manifest.release_notes, asset));
    }

    /** Parses "1.2.3" into [1, 2, 3] and compares major -> minor -> patch. */
    public static int compareVersions(String v1, String v2) {
        int[] a = parseSemver(v1);
        int[] b = parseSemver(v2);
        for (int i = 0; i < 3; i++) {
            if (a[i] != b[i]) return Integer.compare(a[i], b[i]);
        }
        return 0;
    }

    private static int[] parseSemver(String v) {
        String base = v.split("-")[0];
        String[] parts = base.split("\\.");
        int[] out = new int[3];
        for (int i = 0; i < 3; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }

    public static String detectPlatform() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win"))  return "windows";
        if (os.contains("mac"))  return "macos";
        return "linux";
    }
}
```

### Download with Progress
Download using `BodyHandlers.ofFile` and report progress via a JavaFX `ProgressBar`.

```java
public Path download(UpdateAsset asset, ProgressBar bar) throws Exception {
    Path tmp = Files.createTempFile("update-", getExtension(asset.url));
    HttpClient client = HttpClient.newHttpClient();
    long size = asset.size;
    client.send(HttpRequest.newBuilder(URI.create(asset.url)).build(),
        HttpResponse.BodyHandlers.ofFile(tmp));
    Platform.runLater(() -> bar.setProgress(1.0));
    return tmp;
}
```

### SHA-256 Verification
```java
private String sha256(Path file) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    try (InputStream is = Files.newInputStream(file)) {
        byte[] buf = new byte[8192]; int n;
        while ((n = is.read(buf)) != -1) md.update(buf, 0, n);
    }
    StringBuilder sb = new StringBuilder();
    for (byte b : md.digest()) sb.append(String.format("%02x", b));
    return sb.toString();
}
```

### Launch Installer
```java
public void launchInstaller(Path installer) throws IOException {
    new ProcessBuilder(installer.toString()).inheritIO().start();
    Platform.exit();
    System.exit(0);
}
```

## 5. JavaFX Update Notification Dialog

Display an `Alert` showing the new version, release notes, and three actions.

```java
Alert alert = new Alert(Alert.AlertType.INFORMATION);
alert.setTitle("Update Available");
alert.setHeaderText("Version " + info.version + " is available");
alert.setContentText(info.releaseNotes);

ButtonType download = new ButtonType("Download", ButtonBar.ButtonData.YES);
ButtonType skip    = new ButtonType("Skip this version", ButtonBar.ButtonData.NO);
ButtonType later   = new ButtonType("Later", ButtonBar.ButtonData.CANCEL_CLOSE);
alert.getButtonTypes().setAll(download, skip, later);

Optional<ButtonType> result = alert.showAndWait();
if (result.orElse(later) == download) {
    ProgressBar bar = new ProgressBar();
    // wire bar into download(...), then launchInstaller(...)
} else if (result.orElse(later) == skip) {
    Preferences.userNodeForPackage(App.class).put("skipped_version", info.version);
}
```

## 6. Background Scheduled Check

Use a `ScheduledExecutorService` to run checks at the configured interval without blocking the UI.

```java
ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
long hours = config.check_interval_hours;
scheduler.scheduleAtFixedRate(() -> {
    try {
        Optional<UpdateInfo> info = new UpdateChecker(...).checkForUpdate();
        info.ifPresent(i -> Platform.runLater(() -> showUpdateDialog(i)));
    } catch (Exception e) {
        // log; never crash the app from a background update check
    }
}, hours, hours, TimeUnit.HOURS);
```

## 7. Skip Version Feature

Persist the skipped version using the Java Preferences API so the user is not re-prompted for the same release.

```java
public boolean isSkipped(String version) {
    return version.equals(Preferences.userNodeForPackage(App.class)
        .get("skipped_version", ""));
}
```

Before showing the dialog, skip the prompt if `isSkipped(info.version)` returns true and the version is not below `minimum_version`. If the running version is below `minimum_version`, force the update regardless of skip state.

## 8. Integration in App.java

Call the update check after the primary UI has loaded so the user is not blocked at startup.

```java
public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
        stage.setScene(new Scene(root));
        stage.show();

        UpdateConfig config = UpdateConfig.load();
        if (config.check_on_startup) {
            CompletableFuture.runAsync(() -> {
                try {
                    Optional<UpdateInfo> info =
                        new UpdateChecker(config.manifest_url, App.VERSION)
                            .checkForUpdate();
                    info.filter(i -> !isSkipped(i.version))
                        .ifPresent(i -> Platform.runLater(() -> showUpdateDialog(i)));
                } catch (Exception ignored) { }
            });
        }
    }
}
```

The check runs off the JavaFX Application Thread, and any dialog interaction is marshaled back via `Platform.runLater` to keep UI updates thread-safe.
