# Performance Testing Rules

This document defines the performance testing rules, thresholds, and measurement methodologies for JavaFX applications. It serves as the reference for `javafx-tester`'s Performance Testing dimension.

## 1. Startup Time Benchmark

### 1.1 Cold Startup

**Definition**: Time from JVM process start to primary window becoming visible and interactive.

**Measurement methodology**:
1. Add timestamp logging in `start()` method:
   ```java
   @Override
   public void start(Stage primaryStage) {
       long startTime = System.currentTimeMillis();
       // ... initialization code ...
       primaryStage.show();
       long endTime = System.currentTimeMillis();
       System.out.println("Startup time: " + (endTime - startTime) + "ms");
   }
   ```
2. For more precise measurement, use `Instant.now()` and `Duration.between()`
3. Record 3 consecutive runs, report **median** (not average — outliers from JVM warmup skew averages)
4. Include breakdown: JVM startup → class loading → JavaFX toolkit init → FXML loading → Controller initialization → CSS application → first render

**Threshold evaluation**:

| Startup Time | Severity | Action |
|-------------|----------|--------|
| ≤ 3 seconds | Pass | No action needed |
| 3-5 seconds | Minor | Consider lazy loading for non-critical components |
| 5-10 seconds | Major | Profile startup, identify bottlenecks, defer initialization |
| > 10 seconds | Critical | Unacceptable for desktop application, must optimize |

### 1.2 Warm Startup

**Definition**: Time for subsequent application restarts within the same JVM (if applicable).

**Measurement**: Execute application restart (close primary stage, reopen) and measure time.

**Threshold**: Should be ≤ 50% of cold startup time. If warm startup is similar to cold startup, investigate class reloading issues.

### 1.3 Startup Optimization Cross-References

When startup time fails, cross-reference these reviewer static checks:
- `../javafx-code-reviewer/references/performance-guide.md` -- Lazy Loading
- `../javafx-code-reviewer/references/performance-guide.md` -- Batch Updates (bulk data loading on startup)

## 2. UI Response Latency

### 2.1 Event Handling Latency

**Definition**: Time from user interaction (click, type, select) to visible UI update completion.

**Measurement methodology**:
1. Use TestFX `interact()` with timing:
   ```java
   long start = System.nanoTime();
   clickOn("#submitButton");
   waitFor("#resultLabel");
   long end = System.nanoTime();
   long latencyMs = (end - start) / 1_000_000;
   ```
2. Key interactions to measure:
   - Button click → result display
   - Search input → filtered results update
   - Table row selection → detail panel update
   - Tab/View switch → new view rendered
   - Data save → confirmation message

**Threshold evaluation**:

| Response Time | Severity | User Perception |
|--------------|----------|-----------------|
| ≤ 100ms | Pass | Instantaneous |
| 100-300ms | Minor | Perceptible but acceptable |
| 300-1000ms | Major | Feels sluggish, "loading..." indicator recommended |
| > 1000ms | Critical | UI appears frozen, likely blocking FX thread |

### 2.2 Scroll and Render Performance

**Definition**: Frame rate during table scrolling, list scrolling, or animation.

**Measurement**: Count frames rendered per second during scroll using `AnimationTimer`:
```java
AnimationTimer frameCounter = new AnimationTimer() {
    private long lastUpdate = 0;
    private int frameCount = 0;
    @Override
    public void handle(long now) {
        if (now - lastUpdate > 1_000_000_000) {
            System.out.println("FPS: " + frameCount);
            frameCount = 0;
            lastUpdate = now;
        }
        frameCount++;
    }
};
```

**Threshold**: ≥ 60 FPS → Pass; 30-60 FPS → Minor; < 30 FPS → Major; < 15 FPS → Critical.

### 2.3 Response Latency Cross-References

When response latency fails, cross-reference:
- `../javafx-code-reviewer/references/performance-guide.md` -- Virtualization (for large lists/tables)
- `../javafx-code-reviewer/references/performance-guide.md` -- Throttle and Debounce (for search inputs)

## 3. Memory Footprint

### 3.1 Baseline Memory

**Definition**: JVM heap usage after application startup stabilizes (no user interaction).

**Measurement**:
```java
Runtime runtime = Runtime.getRuntime();
long usedMemory = runtime.totalMemory() - runtime.freeMemory();
System.out.println("Used memory: " + (usedMemory / 1024 / 1024) + " MB");
```

**Baseline expectations** (by application complexity):
- Simple utility app: < 50 MB
- Medium CRUD app: 50-150 MB
- Complex data app: 150-300 MB
- Very complex app (multiple windows, large data): 300-500 MB

### 3.2 Memory Growth Trend

**Definition**: Heap usage growth over 5 minutes of simulated usage.

**Measurement methodology**:
1. Record baseline heap after startup
2. Simulate usage: open/close windows, load/save data, navigate views, scroll tables
3. Force GC (`System.gc()`) and record heap
4. Calculate net growth = (final heap after GC) - (baseline heap after GC)
5. Repeat 3 times, report median

**Threshold evaluation**:

| Growth (5 min) | Severity | Interpretation |
|---------------|----------|----------------|
| < 10 MB | Pass | Stable, no significant leak |
| 10-50 MB | Minor | Possible minor leak, monitor |
| 50-100 MB | Major | Likely memory leak, investigate |
| > 100 MB | Critical | Severe leak, will cause OutOfMemoryError |

### 3.3 Memory Cross-References

When memory growth fails, cross-reference:
- `../javafx-code-reviewer/references/memory-leak-risks.md` -- Listener Removal
- `../javafx-code-reviewer/references/memory-leak-risks.md` -- Binding Release
- `../javafx-code-reviewer/references/memory-leak-risks.md` -- Static References

## 4. GC Pressure

### 4.1 Full GC Frequency

**Definition**: Number of Full GC events per minute during normal usage.

**Measurement**: Enable GC logging with `-Xlog:gc*:file=gc.log` and parse the log for Full GC events.

**Threshold**:
- < 1 Full GC/min → Pass
- 1-3 Full GC/min → Minor (acceptable, but heap may be too small)
- > 3 Full GC/min → Major (excessive GC pressure, increase heap or reduce allocation)

### 4.2 GC Pause Time

**Definition**: Duration of stop-the-world GC pauses.

**Threshold**:
- < 50ms → Pass
- 50-200ms → Minor
- 200-500ms → Major (may cause visible UI stutter)
- > 500ms → Critical (UI will freeze during GC)

## 5. Thread Contention

### 5.1 FX Thread Blocking

**Definition**: Whether the JavaFX Application Thread is blocked by long-running operations.

**Measurement**: Use `Platform.runLater()` with periodic timestamp logging to detect gaps:
```java
AnimationTimer blockerDetector = new AnimationTimer() {
    private long lastTime = 0;
    @Override
    public void handle(long now) {
        if (lastTime > 0) {
            long gap = (now - lastTime) / 1_000_000; // ms
            if (gap > 100) {
                System.out.println("FX thread blocked for " + gap + "ms");
            }
        }
        lastTime = now;
    }
};
```

**Threshold**:
- No gaps > 16ms (60 FPS) → Pass
- Occasional gaps 16-100ms → Minor
- Frequent gaps > 100ms → Major
- Any gap > 1000ms → Critical (FX thread is blocked)

### 5.2 Thread Contention Cross-References

When FX thread blocking is detected, cross-reference:
- `../javafx-code-reviewer/references/thread-safety-guide.md` -- FX Thread Update
- `../javafx-code-reviewer/references/thread-safety-guide.md` -- Background Task Wrapping
- `../javafx-code-reviewer/references/thread-safety-guide.md` -- Platform.runLater

## 6. JMH Benchmark Templates

JMH (Java Microbenchmark Harness) provides repeatable, statistically sound microbenchmarks for JavaFX UI performance testing. Because JavaFX UI operations must run on the FX Application Thread, JMH benchmarks wrap their work in `Platform.runLater()` and block the measurement thread until the FX thread completes the work item.

### 6.1 Benchmark Setup Conventions

All JavaFX JMH benchmarks should use the following annotation baseline to ensure consistent, comparable results:

```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Thread)
```

- `Mode.AverageTime` reports the average operation time, which is the most meaningful metric for UI responsiveness (p50 user experience).
- `TimeUnit.MILLISECONDS` matches the threshold scales defined in sections 2.1 and 5.1.
- `@Fork(1)` runs the benchmark in a single forked JVM to avoid cross-run contamination while keeping total runtime manageable.
- `@Warmup(iterations = 3)` lets the JIT compile hot paths before measurement begins.
- `@Measurement(iterations = 5)` produces a stable average with a tight confidence interval.
- `@State(Scope.Thread)` gives each benchmark thread its own state instance, which is essential because the FX thread is single-threaded.

### 6.2 FX Thread Bridge Helper

Because JMH invokes `@Benchmark` methods on a worker thread, benchmarks must dispatch work to the FX thread and wait for completion. Use this helper inside `@State` setup:

```java
@State(Scope.Thread)
public abstract class FxBenchmarkBase {
    private CountDownLatch toolkitLatch;
    private volatile Stage stage;

    @Setup(Level.Trial)
    public void initToolkit() throws Exception {
        toolkitLatch = new CountDownLatch(1);
        Platform.startup(() -> {
            stage = new Stage();
            toolkitLatch.countDown();
        });
        toolkitLatch.await();
    }

    /** Runs a runnable on the FX thread and blocks until it completes. */
    protected void runOnFx(Runnable r) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { r.run(); } finally { done.countDown(); }
        });
        done.await();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        Platform.runLater(() -> { if (stage != null) stage.close(); });
    }
}
```

### 6.3 Example: TableView Rendering Benchmark (1000 Rows)

Measures the time to populate a `TableView` with 1000 rows and force a layout pass. This directly validates the UI Response Latency thresholds in section 2.1.

```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Thread)
public class TableViewRenderBenchmark extends FxBenchmarkBase {

    private TableView<Row> table;
    private ObservableList<Row> rows;

    @Setup(Level.Invocation)
    public void setupTable() throws InterruptedException {
        runOnFx(() -> {
            table = new TableView<>();
            TableColumn<Row, String> c1 = new TableColumn<>("Name");
            c1.setCellValueFactory(new PropertyValueType<>("name"));
            TableColumn<Row, String> c2 = new TableColumn<>("Value");
            c2.setCellValueFactory(new PropertyValueType<>("value"));
            table.getColumns().addAll(c1, c2);
            rows = FXCollections.observableArrayList();
            for (int i = 0; i < 1000; i++) {
                rows.add(new Row("item-" + i, String.valueOf(i)));
            }
        });
    }

    @Benchmark
    public void renderThousandRows() throws InterruptedException {
        runOnFx(() -> {
            table.setItems(rows);
            table.requestLayout();
            table.layout();
        });
    }
}
```

**Interpreting results**: Map the reported average time to section 2.1 thresholds. A render time > 300ms (Major) suggests the table lacks virtualization or is doing heavy cell factory work per row; cross-reference `../javafx-code-reviewer/references/performance-guide.md` -- Virtualization.

### 6.4 Example: CSS Theme Switch Benchmark

Measures the cost of swapping a stylesheet on the scene root, which forces a full CSS reapply across the node graph. Useful for validating live-theme-switch features.

```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Thread)
public class CssThemeSwitchBenchmark extends FxBenchmarkBase {

    private Scene scene;
    private final String light = "/css/light-theme.css";
    private final String dark  = "/css/dark-theme.css";

    @Setup(Level.Trial)
    public void setupScene() throws InterruptedException {
        runOnFx(() -> {
            Pane root = new VBox();
            for (int i = 0; i < 200; i++) {
                root.getChildren().add(new Button("B" + i));
            }
            scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(light);
        });
    }

    @Benchmark
    public void switchTheme() throws InterruptedException {
        runOnFx(() -> {
            ObservableList<String> sheets = scene.getStylesheets();
            sheets.setAll(sheets.contains(light) ? dark : light);
        });
    }
}
```

**Interpreting results**: Theme switch times > 100ms indicate either an excessively deep node graph or inefficient CSS selectors (universal `*` or deep descendant selectors). Cross-reference `../javafx-code-reviewer/references/css-compliance.md` for selector efficiency guidance.

## 7. GC Analysis

GC behavior is a leading cause of perceived UI jank in JavaFX applications. This section defines how to capture, parse, and interpret GC logs to diagnose frame drops and memory leaks.

### 7.1 JVM Flags for GC Analysis

Run the application with these flags to produce a detailed GC log:

```
-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xlog:gc*:file=gc.log:time,uptime,level,tags
```

- `-Xlog:gc*` (JDK 9+) is the unified logging replacement for the legacy `-XX:+PrintGCDetails` / `-XX:+PrintGCTimeStamps` pair. The `:time,uptime,level,tags` decoration adds timestamps and GC cause tags that are required for correlating GC events with UI frame drops.
- On JDK 8, use the legacy pair instead: `-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xloggc:gc.log`.
- For heap analysis, also add `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./heapdump.hprof`.

### 7.2 Detecting UI Pauses Caused by GC

A frame drop occurs whenever the FX thread is unable to render a frame within the 16.6ms budget (60 FPS). To attribute a frame drop to GC:

1. Enable GC logging as above and simultaneously run the FX thread gap detector from section 5.1.
2. Correlate timestamps: any GC pause whose duration exceeds 16ms and whose timestamp falls inside a detected FX thread gap is the likely cause of that frame drop.
3. In the GC log, look for lines tagged `Pause` (stop-the-world events). Example:
   ```
   [2.345s][info][gc] GC(7) Pause Young (G1 Evacuation Pause) 256M->128M(512M) 18.234ms
   ```
   A pause of 18.234ms > 16ms = one or more dropped frames.

**Severity mapping** (extends section 4.2):

| GC Pause Duration | Frame Impact | Severity |
|-------------------|--------------|----------|
| < 16ms | No visible drop | Pass |
| 16-50ms | 1-2 dropped frames | Minor |
| 50-200ms | Visible stutter | Major |
| > 200ms | UI freeze | Critical |

Only `Pause` events (stop-the-world) affect the FX thread; concurrent phases (e.g., `Concurrent Mark`, `Concurrent Relocate`) do not pause the FX thread and can be ignored for frame-drop analysis.

### 7.3 G1 vs ZGC Recommendations for JavaFX

| Collector | Max Pause Target | JavaFX Suitability | Notes |
|-----------|------------------|--------------------|-------|
| G1 (`-XX:+UseG1GC`) | ~200ms (configurable via `-XX:MaxGCPauseMillis`) | Default; good for most desktop apps | Tuned with `-XX:MaxGCPauseMillis=50` to keep pauses near the frame budget. Increase `-XX:G1HeapRegionSize` for heaps > 4GB. |
| ZGC (`-XX:+UseZGC`) | < 1ms (sub-millisecond, JDK 15+ production) | Recommended for animation-heavy or real-time JavaFX apps | ZGC pauses are independent of heap size and consistently below 1ms, eliminating GC-induced frame drops. Requires JDK 15+. Higher CPU overhead than G1. |
| Serial / Parallel | Unbounded | Not recommended for JavaFX | Long stop-the-world pauses will freeze the UI. |

**Recommendation**:
- For typical CRUD/desktop apps with heaps < 2GB: G1 with `-XX:MaxGCPauseMillis=50`.
- For apps with heavy animation, large heaps (≥ 4GB), or strict 60 FPS requirements: ZGC (JDK 15+).
- Always set `-XX:+UseG1GC` or `-XX:+UseZGC` explicitly; do not rely on JVM defaults, which vary by JDK version and heap size.

### 7.4 Identifying Memory Leaks via GC Frequency

A growing GC frequency over time is a strong signal of a slow memory leak (objects survive GC because they are still reachable):

1. Capture a GC log over a 30-minute usage session.
2. Bin the log into 5-minute windows and count `Pause Full` (or `Pause Young`) events per window.
3. Plot events-per-minute vs. time. A healthy app shows a flat or decreasing trend (after warmup). A leak shows a steadily increasing trend.
4. Correlate with heap after each GC (`X->Y(Z)` in the log): if the post-GC heap `Y` grows monotonically across windows, live (retained) memory is accumulating — a leak.
5. Confirm the leak source with a heap dump (`jmap -dump:live,format=b,file=live.hprof <pid>`) taken after forcing GC, then inspect dominator tree in MAT/VisualVM.

**Leak severity** (extends section 3.2):

| Post-GC Heap Trend (5-min windows) | GC Frequency Trend | Interpretation |
|------------------------------------|--------------------|----------------|
| Flat | Flat | Stable, no leak |
| Flat | Increasing | Suspected leak, objects surviving but not yet retaining much heap — investigate early |
| Increasing | Increasing | Confirmed leak, will eventually cause OutOfMemoryError |
| Rapidly increasing | Rapidly increasing | Critical leak, fix immediately |

When a leak is confirmed, cross-reference `../javafx-code-reviewer/references/memory-leak-risks.md` for the common root causes (Listener Removal, Binding Release, Static References).

## 8. FX Thread Blocking Detection

The FX Application Thread is single-threaded; any blocking call on it freezes the entire UI. This section defines runtime techniques to detect, diagnose, and root-cause FX thread blocking, and to reconcile findings with the reviewer's static rules.

### 8.1 Platform.runLater() Frequency Sampling

The FX thread processes `Platform.runLater()` submissions sequentially. When blocking occurs, submissions queue up and the inter-submission gap grows. Sampling submission timestamps detects this queue buildup:

```java
public final class FxQueueSampler {
    private final LongConsumer gapReporter;
    private long lastNanos = 0;

    public FxQueueSampler(LongConsumer gapReporterMs) {
        this.gapReporter = gapReporterMs;
    }

    /** Schedule this on a cadence (e.g., every 16ms via AnimationTimer). */
    public void sample() {
        Platform.runLater(() -> {
            long now = System.nanoTime();
            if (lastNanos != 0) {
                long gapMs = (now - lastNanos) / 1_000_000;
                gapReporter.accept(gapMs);
            }
            lastNanos = now;
        });
    }
}
```

**Interpretation**: Under normal load, gaps track the sampler cadence (e.g., ~16ms). A gap that spikes to hundreds of ms indicates the FX thread was blocked between samples. Log gaps > 100ms as candidate blocking events and correlate with section 5.1 threshold evaluation.

### 8.2 Event Dispatch Monitoring

Enable JavaFX internal event logging to trace every event dispatched on the FX thread:

```
-Djavafx.eventlog=true
```

This prints each dispatched event (type, source, target) to the console. Use it to:

- Confirm whether a user event (e.g., a button press) was actually delivered and when, relative to the observed freeze.
- Detect event storms — a flood of `MOUSE_MOVED` or `SCROLL` events queuing ahead of a critical handler, delaying its execution.
- Pair with timestamp logging in event handlers to measure per-handler dispatch latency.

> **Note**: `-Djavafx.eventlog=true` is verbose and intended for diagnosis only. Never enable it in production or during JMH benchmarks — the logging itself perturbs timing measurements.

### 8.3 Thread Dumps During UI Freeze

When the UI freezes, the FX thread is almost certainly blocked inside a single call stack. Capture it with `jstack`:

1. While the UI is frozen, identify the JVM PID: `jps -l`.
2. Capture three thread dumps 1-2 seconds apart: `jstack <pid> > td1.txt`.
3. In each dump, locate the `JavaFX Application Thread` stack.
4. A blocking call appears as a frame near the top of the stack, e.g.:
   - `java.net.SocketInputStream.socketRead0` — JDBC/network call on FX thread.
   - `java.io.FileInputStream.readBytes` — file I/O on FX thread.
   - `java.lang.Thread.sleep` — explicit sleep on FX thread.
   - `java.util.concurrent.FutureTask.get` — waiting on a background task on the FX thread.
5. If the same blocking frame appears across all three dumps, it is a long-running blocking call (not a transient GC pause). The stack trace points directly at the offending code.

**Alternative**: Use `jcmd <pid> Thread.print` (JDK 9+) which produces the same output and supports repeated invocation in a script.

### 8.4 Common Blocking Patterns

The following patterns are the most frequent causes of FX thread blocking found in real JavaFX codebases:

| Pattern | Stack Signature | Correct Approach |
|---------|-----------------|------------------|
| JDBC calls on FX thread | `java.sql.PreparedStatement.executeQuery` on FX thread | Move DB access to a `Task`/`Service` or `ExecutorService`; publish results via `Platform.runLater()` |
| Synchronous file I/O on FX thread | `java.nio.Files.readAllBytes` / `BufferedReader.readLine` on FX thread | Use background `Task`; show a progress indicator on the FX thread |
| `Thread.sleep()` on FX thread | `java.lang.Thread.sleep` on FX thread | Replace with `PauseTransition` or a scheduled background task |
| Blocking queue/future on FX thread | `Future.get()` / `BlockingQueue.take()` on FX thread | Await on a background thread, notify FX thread on completion |
| Long computation on FX thread | Application frames (sorting, filtering large lists) on FX thread | Offload to `Task`; stream partial results back via `updateValue()` |
| Native/platform blocking calls | AWT/Swing interop, `FileDialog.showAndWait` on a slow filesystem | Run in background; cache results |

### 8.5 Mapping Runtime Findings Back to Reviewer Static Rules

Runtime detection and static review are complementary. After identifying a blocking call at runtime, reconcile it with the reviewer's static checks to either confirm the static finding or refute a false positive:

- **Confirm**: A `jstack` stack showing `java.sql.Connection.executeQuery` on the FX thread confirms the static rule `../javafx-code-reviewer/references/thread-safety-guide.md` -- Background Task Wrapping (which flags DB calls reachable from FX-thread entry points). The runtime finding raises confidence and provides evidence for the report.
- **Confirm**: A `Thread.sleep` frame on the FX thread confirms `../javafx-code-reviewer/references/thread-safety-guide.md` -- Platform.runLater misuse.
- **Refute**: If the static rule flags a method as a potential blocker but `jstack` during a freeze never shows that method on the FX thread, the static finding is a false positive (e.g., the method is only ever called from a background `Task`). Record the refutation so the reviewer heuristic can be tightened.
- **Refute**: If `-Djavafx.eventlog=true` shows a dispatch gap with NO blocking frame in `jstack`, the freeze is GC-induced (section 7.2), not a blocking call — the static "Background Task Wrapping" rule does not apply and should not be cited.

The reconciliation result (confirmed / refuted) should be attached to the runner's dynamic finding so that the reviewer can calibrate its static rules over time.
