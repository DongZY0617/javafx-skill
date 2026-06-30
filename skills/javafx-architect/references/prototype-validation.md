# Prototype Validation Reference

This reference supports the architect's Step 5 (Prototype Validation). It defines the purpose of technical prototyping, the structural rules for prototype code, the validation dimensions, the result recording format, performance benchmarking methods, and the handoff relationship between prototype results and `architecture-handoff.json`. All prototype code is emitted under `architecture/prototype/` and is throwaway — it never enters the production codebase. Prototype validation de-risks the architecture before `javafx-developer` commits to a full implementation in Step 4.

---

## 1. Prototype Validation Purpose

Technical prototypes validate the riskiest assumptions in the architecture before they are baked into production code. A prototype is a small, focused proof of concept that answers a single question: *does this technology choice or design pattern actually work under our constraints?*

Prototype validation serves two goals:

- **Verify key technical risks**: Confirm that an unfamiliar library integration, a performance-critical path, or a complex integration point behaves as assumed in the system design (Step 2). Examples: first-time use of Properties-based binding, real-time rendering of 10K+ rows in `TableView`, custom authentication against an external service.
- **Verify architecture feasibility**: Confirm that the selected architecture pattern, module decomposition, and technology stack can support the key use cases. A failed prototype may trigger an ADR supersession (see `adr-management.md` § 4) or a technology selection change in `system-design.md`.

> **When to prototype**: Prototype only the top 1–3 risks — the choices with the highest uncertainty × impact. Do not prototype well-understood decisions. If a risk is low-uncertainty, document the assumption and move on.

### 1.1 Risk Identification (Step 5.1)

Risk areas worth prototyping typically fall into these categories:

| Risk Category | Example | Prototype Focus |
|---------------|---------|-----------------|
| Unfamiliar library | ReactFX event streams, Ikonli font loading | API availability + basic behavior |
| Performance-critical path | `TableView` with 10K+ rows, real-time chart updates | Frame rate / latency under load |
| Complex integration | OAuth flow, external REST API, WebView embedding | End-to-end happy path |
| Cross-platform concern | jpackage on macOS notarization, file locking on Windows | Platform-specific behavior |
| Concurrency model | Background `Task` + UI thread marshalling | Thread-safety + no listener leaks |

---

## 2. Prototype Code Structure

Prototype code is **independent and isolated** — it lives in its own directory and is never refactored into production code. The developer generates fresh production code based on the architecture specs, not by copying prototypes.

### 2.1 Directory Layout

```
architecture/prototype/
├── README.md                          # What is validated and how to run it
├── tableview-10k-perf/                # One subdirectory per risk
│   ├── TableViewPerfPrototype.java    # Minimal main() class
│   ├── README.md                      # Risk, hypothesis, how to run, results
│   └── pom.xml or build.gradle        # Standalone build (optional, for isolated runs)
└── oauth-flow/
    ├── OAuthFlowPrototype.java
    └── README.md
```

### 2.2 Structural Rules

| Rule | Rationale |
|------|-----------|
| **One subdirectory per risk** | Keeps each proof of concept self-contained and independently runnable. |
| **Minimal scope** — single class or small package | A prototype validates one concept; it does not build a product. Keep it to a `main()` method plus a few helper classes at most. |
| **Include a per-prototype `README.md`** | Explains what is being validated, the hypothesis, how to run it, and the recorded results. |
| **Standalone build (optional)** | If the prototype needs dependencies not in the main project, include a minimal `pom.xml`/`build.gradle` so it runs in isolation. |
| **NOT production code** | Prototypes do NOT go through the review/verify loop (see `javafx-code-reviewer`, `javafx-runner`). They are not packaged, not tested by `javafx-tester`, and not documented by `javafx-docgen`. |
| **No package coupling to the app** | Use a `prototype.` package prefix (e.g., `prototype.tableviewperf`) so it is obvious this is throwaway and easy to delete. |

### 2.3 Per-Prototype README Template

```markdown
# Prototype: [Risk Name]

## Hypothesis
[The assumption being tested — e.g., "Virtualized TableView renders 10K rows at 60fps on a 2020 laptop."]

## Risk Addressed
[Link to the system-design risk / ADR it validates.]

## How to Run
[Exact commands — e.g., `mvn compile exec:java -Dexec.mainClass=prototype.tableviewperf.TableViewPerfPrototype`]

## Results
- result: passed | failed | blocked
- [Performance metric or observed behavior]
- [Issues found, if any]
```

---

## 3. Validation Dimensions

Every prototype must be evaluated across four dimensions. A prototype is only marked `passed` when all applicable dimensions succeed; a single `failed` dimension marks the whole risk as `failed`.

### 3.1 Compilation Passability

The prototype must compile cleanly against the selected JDK and JavaFX versions (see `system-design.md` technology stack). This is the lowest bar — if it does not compile, the API or library version assumption is wrong.

- **Pass criterion**: `javac` / `mvn compile` / `gradle compileJava` exits 0 with no errors.
- **Failure signals**: missing classes/methods (wrong library version), module access errors (`--add-opens` needed), incompatible JavaFX version.
- **On failure**: Revisit the technology selection in `system-design.md`; consider an ADR supersession.

### 3.2 Basic Functionality

The prototype must perform the core happy-path behavior it was built to test. This is a smoke test, not exhaustive testing — `javafx-tester` owns deep testing later.

- **Pass criterion**: The prototype runs end-to-end and exhibits the expected behavior (e.g., the `TableView` displays rows, the OAuth flow returns a token).
- **Failure signals**: runtime exceptions, silent no-ops, incorrect output, unhandled error paths on the happy path.
- **On failure**: Determine whether the failure is in the prototype (bug) or the approach (architecture risk). If the latter, record as `failed` and escalate.

### 3.3 Performance Benchmark

For performance-critical risks, the prototype must meet the non-functional requirement threshold (from `requirements-handoff.json` `non_functional_requirements[]`, or inferred). See § 5 for benchmarking methods.

- **Pass criterion**: Measured metric meets the NFR threshold (e.g., ≥ 60 fps, ≤ 200 ms latency).
- **Failure signals**: frame drops, UI freezing, latency exceeding budget, memory growth indicating a leak.
- **On failure**: The performance risk is confirmed — either adjust the architecture (e.g., add pagination, switch to virtualized controls) or relax the NFR via stakeholder discussion.

### 3.4 API Availability

For unfamiliar-library risks, confirm that the specific APIs the architecture depends on actually exist and behave as documented in the chosen library version.

- **Pass criterion**: Every API call the architecture plans to use (e.g., `TableView.setCellFactory`, `Bindings.createObjectBinding`, Ikonli `FontIcon` literal) resolves and returns expected results.
- **Failure signals**: `NoSuchMethodError`, deprecated/removed API, literal codes that do not render, methods that exist but behave differently than documented.
- **On failure**: Update the technology selection or find an alternative API path; record the workaround in the ADR.

### 3.5 Dimension Applicability Matrix

Not every dimension applies to every risk. Use the matrix to decide which dimensions to evaluate:

| Risk Category | Compilation | Basic Functionality | Performance | API Availability |
|---------------|:-----------:|:-------------------:|:-----------:|:----------------:|
| Unfamiliar library | ✓ | ✓ | — | ✓ |
| Performance-critical path | ✓ | ✓ | ✓ | — |
| Complex integration | ✓ | ✓ | (if latency-sensitive) | ✓ |
| Cross-platform concern | ✓ | ✓ | — | — |
| Concurrency model | ✓ | ✓ | (if throughput-sensitive) | — |

---

## 4. Result Recording Format

Each risk's outcome is recorded as a single object in the `prototype_results[]` array (see § 6). Use exactly three fields, with the `result` value restricted to the three states below.

### 4.1 Result Object Schema

```json
{
  "risk": "TableView performance with 10K rows",
  "result": "passed",
  "detail": "Virtualized rendering handles 10K rows at 60fps; no frame drops; peak heap 120MB"
}
```

| Field | Type | Allowed Values | Description |
|-------|------|----------------|-------------|
| `risk` | string | free text | Short name of the technical risk validated. Should match the risk identified in Step 5.1 and referenced in the prototype's `README.md`. |
| `result` | string | `passed` \| `failed` \| `blocked` | Outcome of the prototype validation. |
| `detail` | string | free text | Concise summary of what was observed — include the key metric (fps, latency), the root cause on failure, or why it was blocked. |

### 4.2 Result State Semantics

| State | Meaning | Architect Action |
|-------|---------|------------------|
| `passed` | The prototype compiled, ran, and met every applicable validation dimension. | Record the supporting metric in `detail`. Architecture proceeds; the risk is retired. |
| `failed` | The prototype compiled/ran but did NOT meet a validation dimension (functionality wrong, performance below threshold, API unavailable). | Record the root cause in `detail`. Trigger an ADR supersession or technology selection change in `system-design.md` if the failure invalidates the architecture. The handoff `conclusion` should reflect "Pass with warnings" or "Fail". |
| `blocked` | The prototype could not be executed due to an external blocker (environment unavailable, credentials missing, third-party service down). | Record the blocker in `detail`. The risk remains open; flag it in the report's "Warnings and Risks" section and assign it to the developer/tester for later validation. |

### 4.3 Worked Examples

```json
"prototype_results": [
  {
    "risk": "TableView performance with 10K rows",
    "result": "passed",
    "detail": "Virtualized rendering handles 10K rows at 60fps; no frame drops; peak heap 120MB"
  },
  {
    "risk": "OAuth2 PKCE flow against external IdP",
    "result": "failed",
    "detail": "IdP rejects localhost redirect URI; requires custom URI scheme. Switched to embedded browser callback — see ADR-006."
  },
  {
    "risk": "jpackage macOS notarization pipeline",
    "result": "blocked",
    "detail": "Apple Developer credentials unavailable in CI; manual notarization tested locally only. Re-validate in deployer phase."
  }
]
```

---

## 5. Performance Benchmarking Methods

For the Performance dimension (§ 3.3), choose a benchmarking method proportional to the risk's criticality. The goal is a defensible number, not a benchmark suite.

### 5.1 Method Selection

| Method | When to Use | Effort | Precision |
|--------|-------------|--------|-----------|
| **Simple timing** | Default — most JavaFX UI performance risks | Low | ±10–20 ms, sufficient for fps/latency smoke checks |
| **JMH microbenchmark** | Hot-path algorithm risk where microsecond-level variance matters (sorting, hashing, parsing) | High | ±µs, statistically rigorous |

Prefer the simple timing method unless the risk is a pure compute hot-path with no UI involvement. JMH adds build complexity that is rarely justified for prototype validation.

### 5.2 Simple Timing Method

Wrap the operation under test in `System.nanoTime()` deltas and repeat enough iterations to dampen JIT warmup and GC noise.

```java
public class TableViewPerfPrototype {
    public static void main(String[] args) {
        // 1. Warmup — let JIT compile before measuring
        for (int i = 0; i < 5; i++) {
            runScenario();
        }

        // 2. Measure
        int iterations = 20;
        long[] times = new long[iterations];
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            runScenario();
            times[i] = System.nanoTime() - start;
        }

        // 3. Report — median + p95 to ignore outliers (GC pauses)
        java.util.Arrays.sort(times);
        long medianMs = times[iterations / 2] / 1_000_000;
        long p95Ms = times[(int) (iterations * 0.95)] / 1_000_000;
        System.out.printf("median=%dms p95=%dms (n=%d)%n", medianMs, p95Ms, iterations);
    }

    private static void runScenario() {
        // Populate a TableView with 10K rows and force a layout pass
        // ...
    }
}
```

**Rules for credible simple-timing results**:

- Run at least 5 warmup iterations before measuring to let the JIT compile.
- Measure at least 20 iterations and report the **median** and **p95**, not the mean (GC pauses skew the mean).
- For UI frame-rate risks, use a `Platform.runLater` loop with an `AnimationTimer` and count frames rendered over a fixed window (e.g., 5 seconds) — `fps = frames / seconds`.
- Always record the hardware in the prototype `README.md` (CPU, RAM, OS) so the number is reproducible.

### 5.3 JMH Method (Optional)

Use JMH only for pure-compute hot paths. Add the `jmh-core` and `jmh-generator-annprocess` dependencies to the prototype's standalone build, then annotate a benchmark method:

```java
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(2)
public class SortBenchmark {
    private List<Integer> data;

    @Setup
    public void setup() {
        data = IntStream.range(0, 10_000).boxed().collect(Collectors.toList());
        java.util.Collections.shuffle(data);
    }

    @Benchmark
    public List<Integer> sortedCopy() {
        return data.stream().sorted().collect(Collectors.toList());
    }
}
```

Run with `mvn package && java -jar target/benchmarks.jar` and copy the JMH score line into the result `detail`.

### 5.4 Recording Performance Results

Always put the **measured number and the threshold** in the `detail` field so a reviewer can see the margin at a glance:

> `"detail": "median=42ms p95=58ms vs 200ms NFR budget — passed with 4x margin (2020 MacBook Pro, M1, 16GB)"`

---

## 6. Relationship to the Architecture Handoff

Prototype results are the bridge between Step 5 (validation) and Step 6 (handoff generation). They are written into the `prototype_results[]` array of `architecture/architecture-handoff.json`, which `javafx-developer` consumes in Step 4.

### 6.1 Handoff Field

| Handoff Field | Type | Description |
|---------------|------|-------------|
| `prototype_results[]` | array | One entry per validated risk, using the schema in § 4.1. Populated only if Step 5 was executed; omitted (empty array) when no risks warranted prototyping. |

### 6.2 End-to-End Flow

```
Step 5 (Prototype Validation)
  │
  │  For each risk:
  │    1. Generate prototype under architecture/prototype/{risk-slug}/
  │    2. Evaluate applicable dimensions (§ 3)
  │    3. Record { risk, result, detail } (§ 4)
  │
  ▼
Step 6 (Generate Architecture Handoff)
  │
  │  4. Collect all { risk, result, detail } objects
  │  5. Write them to architecture-handoff.json → prototype_results[]
  │  6. Mirror summary in architect-report.md § 8 (Prototype Validation table)
  │
  ▼
javafx-developer (Step 4)
  │
  │  7. Reads prototype_results[] to learn which risks are retired (passed),
  │     which changed the architecture (failed), and which remain open (blocked).
  │  8. Open (blocked) risks are forwarded to javafx-tester for runtime validation.
```

### 6.3 Interaction with Other Handoff Sections

- **`adr_files[]`**: A `failed` prototype often produces a new ADR (supersession or workaround). Link the ADR from the result `detail`.
- **`conclusion`**: If any prototype `failed` and the architecture could not be adjusted, set `conclusion` to `"Fail"` or `"Pass with warnings"`. If any prototype is `blocked`, prefer `"Pass with warnings"` and list the open risks in the report's "Warnings and Risks" section.
- **`developer_instructions.key_constraints[]`**: Lessons from prototypes (e.g., "TableView must stay virtualized; do not call `setFixedCellSize` with a negative value") should be added as developer constraints so the production code does not regress the validated behavior.

---

## 7. Prototype Validation Checklist

Before writing `prototype_results[]` to the handoff, confirm every item:

- [ ] Only the top 1–3 risks were prototyped — no gold-plating.
- [ ] Each prototype lives in its own subdirectory under `architecture/prototype/` with a `README.md`.
- [ ] Each prototype is standalone — no dependency on the (not-yet-existing) production codebase.
- [ ] Compilation was verified against the selected JDK + JavaFX versions.
- [ ] Basic functionality (happy path) was confirmed by running the prototype.
- [ ] For performance risks: a measured metric (median + p95, or fps) with the hardware noted was recorded.
- [ ] For unfamiliar-library risks: every planned API call was confirmed to exist and behave as expected.
- [ ] Every risk has a `prototype_results[]` entry with `risk`, `result` (one of `passed`/`failed`/`blocked`), and `detail`.
- [ ] `failed` results triggered an ADR or technology selection change, referenced in `detail`.
- [ ] `blocked` results are flagged in the report's "Warnings and Risks" section for downstream validation.
- [ ] Prototype code is NOT refactored into production — `javafx-developer` generates fresh code from the architecture specs.
