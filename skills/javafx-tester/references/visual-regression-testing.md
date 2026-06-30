# Visual Regression Testing Rules

This document defines the visual regression testing rules, screenshot capture methodology, pixel comparison algorithm, and baseline management workflow for JavaFX applications. It serves as the reference for `javafx-tester`'s Visual Regression Testing dimension (Track D).

## 1. Overview

Visual regression testing detects unintended UI changes by comparing current rendered screenshots against approved baseline snapshots. Unlike accessibility testing (which checks semantic compliance) or performance testing (which measures speed), visual regression testing catches **pixel-level deviations** — a button shifted 2px to the left, a font rendering change, a color drift, or a layout collapse that passes all functional tests but looks wrong to users.

**Key principle**: Every visual change is either **intentional** (approved via baseline update) or **unintentional** (reported as regression). There is no "close enough" — the pixel diff is binary per-pixel, and the aggregate diff ratio determines pass/fail.

## 2. Tool Setup

### 2.1 TestFX

TestFX provides the testing framework for JavaFX UI testing, including screenshot capture utilities.

**Maven dependency** (`pom.xml`):
```xml
<dependency>
    <groupId>org.testfx</groupId>
    <artifactId>testfx-core</artifactId>
    <version>4.0.18</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testfx</groupId>
    <artifactId>testfx-junit5</artifactId>
    <version>4.0.18</version>
    <scope>test</scope>
</dependency>
```

### 2.2 Monocle (Headless Rendering)

For CI environments without a display, Monocle provides a headless rendering pipeline that produces pixel-identical output to a real display.

**Maven dependency**:
```xml
<dependency>
    <groupId>org.testfx</groupId>
    <artifactId>openjfx-monocle</artifactId>
    <version>jdk-17+21</version>
    <scope>test</scope>
</dependency>
```

**Headless launch configuration**:
```java
// Set before Application.launch() or in @BeforeAll
System.setProperty("testfx.robot", "glass");
System.setProperty("testfx.headless", "true");
System.setProperty("prism.order", "sw");
System.setProperty("prism.text", "t2k");
System.setProperty("java.awt.headless", "true");
```

> **Monocle vs real display**: Monocle uses software rendering (`prism.order=sw`), which produces deterministic output across platforms. Real GPU rendering may produce slightly different anti-aliasing on different drivers. For consistent baselines, **always capture baselines in the same rendering mode** used for regression testing.

### 2.3 Screenshot Capture Utility

```java
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.io.File;
import java.awt.image.BufferedImage;

public class SnapshotUtil {

    /**
     * Captures a screenshot of the given scene and returns it as a BufferedImage.
     * Waits for the scene to fully render before capturing.
     */
    public static BufferedImage captureScene(Scene scene) {
        // Wait for the scene to render — 2 pulse cycles ensure layout pass completes
        scene.snapshot(new WritableImage(1, 1)); // warm-up snapshot
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        scene.snapshot(new WritableImage(1, 1)); // second warm-up

        WritableImage image = scene.snapshot(null);
        return SwingFXUtils.fromFXImage(image, null);
    }

    /**
     * Saves a BufferedImage to the given path as PNG (lossless for pixel comparison).
     */
    public static void saveBaseline(BufferedImage image, String path) throws Exception {
        File file = new File(path);
        file.getParentFile().mkdirs();
        ImageIO.write(image, "png", file);
    }
}
```

## 3. Baseline Snapshot Management

### 3.1 Storage Location

Baseline snapshots are stored in the project's test resources directory:

```
src/test/resources/snapshots/
├── main-view/
│   ├── default.png              # Main view, default state
│   ├── dark-theme.png           # Main view, dark theme
│   └── with-data.png            # Main view with sample data loaded
├── login-dialog/
│   ├── empty.png                # Login dialog, no input
│   └── error-state.png          # Login dialog with validation error
├── settings/
│   ├── general-tab.png          # Settings dialog, General tab
│   └── advanced-tab.png         # Settings dialog, Advanced tab
└── snapshot-manifest.json       # Manifest of all snapshots with metadata
```

### 3.2 Naming Convention

`{view-name}/{state-description}.png`

- **view-name**: The FXML view or dialog name (kebab-case), e.g., `main-view`, `login-dialog`
- **state-description**: The visual state being captured (kebab-case), e.g., `default`, `dark-theme`, `with-data`, `error-state`

### 3.3 Snapshot Manifest

A JSON manifest tracks all baseline snapshots and their metadata:

```json
{
  "version": "1.0",
  "capture_config": {
    "rendering_mode": "monocle",
    "window_width": 1024,
    "window_height": 768,
    "font_smoothing": false,
    "animations_disabled": true
  },
  "snapshots": [
    {
      "id": "main-view/default",
      "path": "main-view/default.png",
      "view": "MainView",
      "description": "Main view in default state with no data loaded",
      "capture_date": "2026-06-30",
      "last_updated": "2026-06-30",
      "update_reason": "initial baseline",
      "width": 1024,
      "height": 768
    }
  ]
}
```

### 3.4 Window Size Standardization

All snapshots must be captured at a standardized window size to eliminate layout differences caused by resizing. The default size is **1024x768**, but projects can override this in the manifest:

```json
"capture_config": {
    "window_width": 1280,
    "window_height": 800
}
```

> **Multiple resolutions**: If the application supports multiple resolutions, capture a baseline for each. Use the naming convention `{view-name}/{state-description}@{width}x{height}.png` (e.g., `main-view/default@1280x800.png`).

## 4. Screenshot Capture Methodology

### 4.1 Views to Capture

Identify all renderable views in the application:

1. **Main window**: Each primary view/tab/pane
2. **Dialogs**: All modal and non-modal dialogs (login, settings, about, error)
3. **States**: Key visual states for each view:
   - Empty state (no data)
   - Populated state (with sample data)
   - Error state (validation errors shown)
   - Loading state (progress indicator visible)
   - Selection state (item selected in table/list)

### 4.2 Capture Procedure

For each view/state combination:

1. **Launch the application** (or navigate to the view) using TestFX
2. **Wait for rendering**: Ensure the scene graph is fully laid out and rendered
   ```java
   // Wait for at least 2 JavaFX pulse cycles
   WaitForAsyncUtils.waitForFxEvents(2);
   // Additional settle time for CSS and animations
   Thread.sleep(200);
   ```
3. **Disable animations**: Set all animations to their end-state before capturing
   ```java
   // Disable transitions temporarily
   AnimationPulseHelper.pause();
   // Or set animation speed to 0
   ```
4. **Standardize fonts**: Use deterministic font rendering
   ```java
   System.setProperty("prism.text", "t2k");  // T2K renderer is more deterministic
   System.setProperty("prism.lcdtext", "false");
   ```
5. **Capture screenshot**: Use `SnapshotUtil.captureScene(scene)`
6. **Save or compare**: Save as new baseline, or compare against existing baseline

### 4.3 Dynamic Content Handling

Some UI elements render non-deterministically (timestamps, avatars, charts with random data). These must be handled to avoid false positives:

1. **Ignore regions**: Define rectangular regions to exclude from comparison
   ```java
   // In snapshot-manifest.json
   {
     "id": "main-view/default",
     "ignore_regions": [
       { "x": 800, "y": 10, "width": 200, "height": 20, "reason": "timestamp display" }
     ]
   }
   ```
2. **Mock dynamic data**: Replace dynamic data with fixed test data before capturing
3. **Mask regions**: Render a solid color over dynamic areas before capture

## 5. Pixel Comparison Algorithm

### 5.1 Per-Pixel Comparison

```java
import java.awt.image.BufferedImage;

public class PixelComparator {

    /**
     * Compares two images pixel by pixel and returns the diff ratio.
     *
     * @param baseline  The approved baseline image
     * @param current   The current screenshot
     * @param tolerance Per-channel RGB tolerance (0-255). Pixels within this
     *                  tolerance are considered identical. Default: 3
     * @param ignoreRegions Rectangular regions to exclude from comparison
     * @return PixelDiffResult containing diff ratio and diff image
     */
    public static PixelDiffResult compare(
            BufferedImage baseline,
            BufferedImage current,
            int tolerance,
            Rectangle[] ignoreRegions) {

        int width = Math.min(baseline.getWidth(), current.getWidth());
        int height = Math.min(baseline.getHeight(), current.getHeight());
        int totalPixels = width * height;
        int diffPixels = 0;

        BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isInIgnoreRegion(x, y, ignoreRegions)) {
                    diffImage.setRGB(x, y, 0x808080); // gray = ignored
                    totalPixels--;
                    continue;
                }

                int baselineRGB = baseline.getRGB(x, y);
                int currentRGB = current.getRGB(x, y);

                if (pixelDiff(baselineRGB, currentRGB, tolerance)) {
                    diffPixels++;
                    diffImage.setRGB(x, y, 0xFF0000); // red = different
                } else {
                    diffImage.setRGB(x, y, baselineRGB); // original = same
                }
            }
        }

        double diffRatio = totalPixels > 0 ? (double) diffPixels / totalPixels : 0.0;
        return new PixelDiffResult(diffRatio, diffPixels, totalPixels, diffImage);
    }

    /**
     * Returns true if the two pixels differ by more than tolerance in any channel.
     */
    private static boolean pixelDiff(int rgb1, int rgb2, int tolerance) {
        int r1 = (rgb1 >> 16) & 0xFF, r2 = (rgb2 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF,  g2 = (rgb2 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF,         b2 = rgb2 & 0xFF;
        return Math.abs(r1 - r2) > tolerance
            || Math.abs(g1 - g2) > tolerance
            || Math.abs(b1 - b2) > tolerance;
    }
}
```

### 5.2 Diff Ratio Calculation

```
diff_ratio = diff_pixels / comparable_pixels
```

Where:
- `diff_pixels`: Number of pixels that differ beyond tolerance
- `comparable_pixels`: Total pixels minus ignored pixels

### 5.3 Tolerance Configuration

| Parameter | Default | Description |
|-----------|---------|-------------|
| `pixel_tolerance` | 3 | Per-channel RGB tolerance (0-255). Accounts for minor rendering differences in anti-aliasing |
| `diff_threshold` | 0.02 (2%) | Maximum acceptable diff ratio. Below = Pass, above = regression |
| `ignore_regions` | none | Rectangles excluded from comparison (dynamic content) |

> **Tolerance rationale**: A per-channel tolerance of 3 (out of 255) absorbs sub-pixel rendering differences from different font hinting or anti-aliasing algorithms, without masking real visual changes. The 2% diff threshold allows for minor rendering variance while catching meaningful layout or color changes.

## 6. Threshold Evaluation

| Diff Ratio | Severity | Description | Fix |
|-----------|----------|-------------|-----|
| < 1% | Pass | Negligible difference, within rendering noise | No action |
| 1% - 2% | Pass | Within threshold, acceptable variance | No action, record for tracking |
| 2% - 5% | Minor | Slight visual change detected, likely intentional | Review diff, update baseline if intentional |
| 5% - 15% | Major | Significant visual change, likely unintended regression | Investigate root cause, fix or update baseline |
| > 15% | Critical | Major layout collapse or complete view change | Block delivery, investigate immediately |

> **Baseline missing**: If no baseline exists for a view, the capture is saved as the initial baseline and the result is recorded as `"Baseline Created"` (Pass with note). This happens on first run or when a new view is added.

## 7. Anti-Flaky Strategies

Visual regression tests are notoriously flaky if not carefully controlled. The following strategies eliminate common sources of non-determinism:

### 7.1 Deterministic Rendering

| Strategy | Implementation | Problem Solved |
|----------|---------------|----------------|
| Use Monocle (software rendering) | `prism.order=sw` | GPU driver differences |
| Use T2K text renderer | `prism.text=t2k` | Font hinting differences |
| Disable LCD text | `prism.lcdtext=false` | Sub-pixel rendering differences |
| Disable animations | Pause pulse, set transition speed to 0 | Frame timing differences |
| Fixed window size | Standardize in manifest | Layout reflow from resize |
| Mock dynamic data | Replace timestamps, random data | Non-deterministic content |

### 7.2 Render Stabilization

Before capturing, wait for the scene to stabilize:

```java
// 1. Wait for all pending events to process
WaitForAsyncUtils.waitForFxEvents();

// 2. Wait for 2 pulse cycles (layout + render)
WaitForAsyncUtils.waitForFxEvents(2);

// 3. Settle time for CSS application and image loading
Thread.sleep(200);

// 4. Warm-up capture (first snapshot may differ due to lazy initialization)
scene.snapshot(new WritableImage(1, 1));

// 5. Actual capture
BufferedImage screenshot = SnapshotUtil.captureScene(scene);
```

### 7.3 Cross-Platform Font Consistency

Different OS platforms ship different default fonts, causing text rendering differences:

1. **Embed fonts**: Include font files in `src/main/resources/fonts/` and load them via `Font.loadFont()`
2. **Use cross-platform fonts**: Prefer fonts available on all platforms (e.g., "Segoe UI", "San Francisco", "DejaVu Sans" fallback chain)
3. **Set explicit font family** in CSS: `-fx-font-family: "Inter", "Segoe UI", "SansSerif";`
4. **Record font config in manifest**: Track which fonts were used during baseline capture

## 8. Baseline Update Workflow

### 8.1 When to Update Baselines

Baselines should be updated **only** when visual changes are intentional:

- New feature adds a new UI element
- Design refinement changes layout, color, or typography
- Theme update changes visual style
- Bug fix intentionally changes rendering

### 8.2 Update Switch

The update switch is controlled via system property or `.loop-config.json`:

**System property**:
```bash
mvn test -Dupdate.baselines=true
```

**`.loop-config.json`**:
```json
{
  "visual_regression": true,
  "vr_update_baselines": true,
  "vr_diff_threshold": 0.02
}
```

When the update switch is on:
1. Current screenshots are saved as new baselines (overwriting old ones)
2. The manifest's `last_updated` and `update_reason` fields are updated
3. No regression issues are reported for that run
4. The tester logs a `Baseline Updated` info entry for each updated snapshot

### 8.3 Update Process

1. **Developer makes intentional UI change** (e.g., updates button color in CSS)
2. **Run tester with update switch**: `mvn test -Dupdate.baselines=true` or set `vr_update_baselines: true`
3. **Tester captures new screenshots** and saves them as baselines
4. **Review diff**: The tester generates a diff report showing what changed (old vs new baseline)
5. **Commit new baselines**: Developer commits updated snapshots and manifest to version control
6. **Subsequent runs**: Normal regression testing resumes with the new baselines

### 8.4 Version Control

Baseline snapshots **must** be committed to version control alongside source code. This ensures:
- Team members share the same baselines
- CI runs against the same baselines
- Baseline changes are reviewable in pull requests
- Baseline history is preserved for auditing

**Git LFS recommendation**: Since PNG files are binary and can be large, consider using Git LFS:
```gitattributes
src/test/resources/snapshots/**/*.png filter=lfs diff=lfs merge=lfs -text
```

## 9. Integration with Fix Cycle

When visual regression testing detects a regression in the loop:

1. **Issue generated**: The tester creates a Fix Handoff entry targeting the file causing the visual change (CSS, FXML, or Java layout code)
2. **Diff image attached**: The diff image is saved to `target/test-output/vr-diffs/{view}/{state}-diff.png` and referenced in the issue
3. **Root cause hint**: The tester analyzes the diff pattern to suggest the likely cause:
   - **Layout shift** (rectangular diff region): Likely a layout constraint change in FXML or Java
   - **Color change** (uniform diff across element): Likely a CSS color property change
   - **Text change** (diff in text area): Likely a font, size, or content change
   - **Missing element** (large diff region where element was): Likely a visibility or conditional rendering bug
4. **Developer fix**: Developer either fixes the unintended change or updates the baseline if the change was intentional

## 10. Cross-References

- `../javafx-code-reviewer/references/css-compliance.md` -- CSS validation (static check, cross-reference for visual confirmation)
- `../javafx-code-reviewer/references/fxml-standards.md` -- FXML layout patterns (cross-reference for layout regression root cause)
- `../javafx-runner/references/test-coverage-gate.md` -- Test coverage (visual regression tests contribute to test coverage)
- `performance-testing.md` -- Performance testing (share TestFX infrastructure)
