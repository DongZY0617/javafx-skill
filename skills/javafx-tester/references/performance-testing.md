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
