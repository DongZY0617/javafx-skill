# Runtime Monitoring

This document defines the rules for runtime monitoring of JavaFX desktop applications, covering structured logging with Logback, crash reporting, performance metrics collection, and integration into the application lifecycle.

## 1. Logback Configuration (logback.xml)

Logback provides two primary appenders: a console appender for development and a rolling file appender for production.

### Console Appender (Development)
Used during development with colorized output and DEBUG level.

```xml
<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
  <encoder>
    <pattern>%d{HH:mm:ss.SSS} %highlight(%-5level) [%thread] %logger{36} - %msg%n</pattern>
  </encoder>
</appender>
<root level="DEBUG">
  <appender-ref ref="STDOUT" />
</root>
```

### Rolling File Appender (Production)
Production writes to a rolling file under the user's home directory.

```xml
<property name="LOG_DIR" value="${user.home}/.appname/logs" />
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
  <file>${LOG_DIR}/appname.log</file>
  <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
    <fileNamePattern>${LOG_DIR}/appname-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
    <maxFileSize>10MB</maxFileSize>
    <maxHistory>30</maxHistory>
    <totalSizeCap>1GB</totalSizeCap>
  </rollingPolicy>
  <encoder>
    <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{40} - %msg%n</pattern>
  </encoder>
</appender>
<root level="INFO">
  <appender-ref ref="FILE" />
</root>
```

| Rolling policy attribute | Value | Purpose |
|--------------------------|-------|---------|
| `maxFileSize` | 10MB | Rotate when a single file exceeds this size |
| `maxHistory` | 30 | Keep at most 30 days of archived logs |
| `totalSizeCap` | 1GB | Delete oldest archives when total exceeds this cap |
| `fileNamePattern` | `appname-%d{yyyy-MM-dd}.%i.log` | Date plus index for size-rotated files within a day |

### Profile-Based Configuration
- `logback-test.xml` on the classpath activates in development/test runs.
- `logback.xml` on the classpath is the production default.

Logback resolves `logback-test.xml` first when present, so dev settings never leak into production artifacts.

## 2. Crash Reporting (CrashHandler.java)

Install a global uncaught exception handler to capture crashes on any thread, including the JavaFX Application Thread.

```java
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        writeReport(t, e);
    }

    public static void install() {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler());
        // JavaFX Application Thread handler
        Thread.currentThread().setUncaughtExceptionHandler(new CrashHandler());
    }

    private void writeReport(Thread t, Throwable e) {
        Path dir = Paths.get(System.getProperty("user.home"),
            ".appname", "crashes");
        try {
            Files.createDirectories(dir);
            String ts = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path file = dir.resolve("crash-" + ts + ".txt");
            try (Writer w = Files.newBufferedWriter(file)) {
                w.write("Timestamp: " + LocalDateTime.now() + "\n");
                w.write("App version: " + App.VERSION + "\n");
                w.write("OS: " + System.getProperty("os.name") + " "
                    + System.getProperty("os.version") + "\n");
                w.write("Java: " + System.getProperty("java.version") + "\n");
                w.write("Thread: " + t.getName() + "\n");
                w.write("Stack trace:\n");
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                w.write(sw.toString());
            }
            maybeReportRemote(file);
        } catch (IOException ignored) { }
    }
}
```

### Crash Report Format
Each report file contains:

| Field | Source |
|-------|--------|
| Timestamp | `LocalDateTime.now()` |
| App version | `App.VERSION` |
| OS | `os.name` + `os.version` |
| Java version | `java.version` |
| Thread name | `t.getName()` |
| Stack trace | `Throwable.printStackTrace()` |

### Crash File Location
Reports are written to `${user.home}/.appname/crashes/crash-{timestamp}.txt`.

### Optional Remote Reporting
If the user has opted in, POST the crash file to a configured endpoint.

```java
private void maybeReportRemote(Path file) {
    if (!Preferences.userNodeForPackage(App.class)
        .getBoolean("report_crashes", false)) return;
    HttpClient.newHttpClient().sendAsync(HttpRequest.newBuilder(
        URI.create("https://<your-api-domain>/crash-report"))
        .header("Content-Type", "text/plain")
        .POST(HttpRequest.BodyPublishers.ofFile(file)).build(),
        HttpResponse.BodyHandlers.ofString());
}
```

## 3. Performance Metrics (MetricsCollector.java)

Collect startup time, scene load time, memory usage, and GC statistics, writing them to a per-day metrics file.

```java
public class MetricsCollector {
    private static long startNano;
    private static final Path DIR = Paths.get(
        System.getProperty("user.home"), ".appname", "metrics");

    public static void recordStart() { startNano = System.nanoTime(); }

    public static long recordStartupComplete() {
        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNano);
        append("startup_ms=" + ms);
        return ms;
    }

    public static long measureLoad(Supplier<Object> loader) {
        long s = System.nanoTime();
        Object o = loader.get();
        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - s);
        append("scene_load_ms=" + ms);
        return ms;
    }

    public static void snapshotMemory() {
        Runtime r = Runtime.getRuntime();
        long used = r.totalMemory() - r.freeMemory();
        append("heap_used_mb=" + (used / 1024 / 1024));
    }

    public static void snapshotGc() {
        for (GarbageCollectorMXBean gc :
            ManagementFactory.getGarbageCollectorMXBeans()) {
            append("gc_" + gc.getName() + "_count=" + gc.getCollectionCount()
                + " time_ms=" + gc.getCollectionTime());
        }
    }
}
```

| Metric | Source | Example |
|--------|--------|---------|
| Startup time | `System.nanoTime()` in `main()` vs end of `start()` | `startup_ms=820` |
| Scene load time | `FXMLLoader` load duration | `scene_load_ms=45` |
| Memory usage | `Runtime.totalMemory() - Runtime.freeMemory()` | `heap_used_mb=128` |
| GC stats | `ManagementFactory.getGarbageCollectorMXBeans()` | `gc_G1_Young_count=3 time_ms=40` |

### Metrics File
Metrics are appended to `${user.home}/.appname/metrics/metrics-{date}.log`.

### JMX Exposure
Expose metrics as an MBean for runtime inspection via JConsole or VisualVM.

```java
public interface MetricsCollectorMBean {
    long getStartupMs();
    long getHeapUsedMb();
    long getGcCount();
}

// Registration
MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
mbs.registerMBean(metrics, new ObjectName("com.myapp:type=Metrics"));
```

## 4. Integration in App.java

| Lifecycle hook | Action |
|----------------|--------|
| `main()` (before launch) | Install crash handler, start metrics timer |
| `start()` | Record startup time, initialize logging |
| `stop()` | Write final metrics, flush logs |

```java
public class App extends Application {
    @Override
    public void init() throws Exception {
        CrashHandler.install();
        MetricsCollector.recordStart();
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
        stage.setScene(new Scene(root));
        stage.show();
        MetricsCollector.recordStartupComplete();
        MetricsCollector.snapshotMemory();
    }

    @Override
    public void stop() {
        MetricsCollector.snapshotMemory();
        MetricsCollector.snapshotGc();
        LoggerFactory.getILoggerFactory().stop();
    }
}
```

## 5. Log Level Guidelines

| Level | When to use | Example |
|-------|-------------|---------|
| `ERROR` | Production blockers requiring attention | "Failed to load user project" |
| `WARN`  | Recoverable issues, degraded behavior | "Fallback font used; theme asset missing" |
| `INFO`  | Significant user actions and lifecycle | "User exported PDF (12 pages)" |
| `DEBUG` | Development diagnostics | "FXML loader resolved /main.fxml in 45ms" |
| `TRACE` | Detailed control flow | "Entering export loop iteration 3/12" |

Production runs at `INFO`; enable `DEBUG` or `TRACE` temporarily via a runtime flag or JMX to diagnose issues without redeploying.
