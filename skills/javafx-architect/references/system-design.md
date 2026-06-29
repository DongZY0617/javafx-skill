# JavaFX System Design Reference

This reference supports the architect's Step 2 (System Design). It defines architecture pattern selection, technology selection criteria, module decomposition, layering strategy, and a feasibility assessment framework for JavaFX desktop applications. Every recommendation here must be justified when recorded in an ADR.

---

## 1. Architecture Patterns

### 1.1 Pattern Comparison

| Pattern | When to Use | Pros | Cons | JavaFX-Specific Implementation |
|---------|-------------|------|------|--------------------------------|
| **Layered (n-tier)** | Standard desktop apps with clear Presentation/Application/Domain/Infrastructure separation; team familiar with classic tiering | Easy to reason about; clear dependency direction; maps to packages | Can become a "pass-through" layer soup; risk of anemic domain model | One package per layer; controllers call services, services call repositories; no upward dependencies |
| **MVVM + Service Layer** | Apps with complex UI state, many forms, heavy data binding, and high testability needs | ViewModel is UI-free and unit-testable; bidirectional `Property` binding reduces boilerplate; parallel UI/logic work | More classes; binding chains can leak listeners; steep learning curve | ViewModel exposes `StringProperty`/`BooleanProperty`/`ObservableList`; thin controller binds `TextField.textProperty()` bidirectionally; service layer holds business rules |
| **Event-Driven** | Real-time data, pub/sub messaging, reactive streams, multi-view refresh on one event (e.g., login refreshes header + sidebar + dashboard) | Loose coupling between producers/consumers; easy to add new subscribers; natural fit for async data | Harder to trace data flow; risk of event storms/cycles; debugging is indirect | `EventBus` (Guava or custom) dispatching on the JavaFX Application Thread via `Platform.runLater`; or ReactFX `EventStreams` for reactive composition |
| **Plugin Architecture** | Extensible apps where third parties or future teams add features without recompiling the core (IDEs, tools, dashboards) | Open/closed principle; core stays small; features ship independently | Lifecycle/version complexity; plugin API design is hard; security surface | `ServiceLoader` for simple JDK-native extensions; **PF4J** for full lifecycle (start/stop/stop), isolation, and extension points |
| **Microkernel** | Apps where a minimal core must remain stable while features are pluggable and optional (editor with optional format plugins) | Core is tiny and stable; features are optional/composable; high extensibility | Indirection overhead; must design the kernel API up front; harder "first feature" | Core exposes an `ExtensionPoint` SPI; features registered via `ServiceLoader` or PF4J; kernel loads extensions at startup and wires them into a menu/toolbar |

### 1.2 Decision Matrix: App Type → Pattern → Rationale

| App Type | Recommended Pattern | Rationale |
|----------|---------------------|-----------|
| Utility / single-window tool (< 5 screens) | Layered (Presentation + thin Service) | Low overhead; MVC-with-service is enough; binding optional |
| CRUD/business form app (10–50 screens) | MVVM + Service Layer | State management + testability dominate; binding pays off |
| Real-time dashboard (live market/telemetry) | Event-Driven + MVVM UI | Pub/sub decouples data producers from many views; binding surfaces updates |
| IDE / extensible editor / tooling platform | Microkernel + Plugin | Stable core, pluggable features, third-party extensions |
| Modular enterprise suite (many optional features) | Plugin (PF4J) on a Layered core | Feature isolation, independent delivery, versioned plugin API |
| Mixed (forms + real-time + extensions) | Layered core + MVVM UI + Event bus for cross-cutting signals | Combine patterns by concern; do not force one pattern everywhere |

> **Principle**: Start with the simplest pattern that fits. Introduce Event-Driven or Plugin only when a concrete requirement (real-time data, third-party extensibility) forces it.

---

## 2. Technology Selection Criteria

### 2.1 Database — SQLite vs H2 vs PostgreSQL vs None

| Option | Data Volume | Concurrency | Deployment | When to Choose |
|--------|-------------|-------------|------------|----------------|
| **None (in-memory / file)** | < 1 MB | Single user | No install | Config-only tools, caches, throwaway prototypes |
| **SQLite** | < 1 TB | Single writer | Zero-config, single file | Local-first desktop apps, embedded storage, offline-first |
| **H2 (embedded)** | < 50 GB | Single process | Jar bundled | Needs in-process SQL with fast startup, or an in-memory test DB |
| **PostgreSQL (client/server)** | Unlimited | Multi-user | Requires server/URL | Shared data, multi-client, server-side business rules, cloud sync |

> **Rule of thumb**: Desktop app + local data → SQLite. Need a server or multi-user → PostgreSQL. Unit/integration tests → H2 in-memory.

### 2.2 ORM — JPA/Hibernate vs MyBatis vs JDBC vs None

| Option | Complexity Control | Best For | Trade-off |
|--------|--------------------|----------|-----------|
| **JPA / Hibernate** | High abstraction, low SQL control | Rich domain models, complex object graphs, portability | Heavyweight; startup cost; N+1 risk; learning curve |
| **MyBatis** | Medium — SQL you write, mapping automated | Complex queries, reporting, DB-specific tuning | You own the SQL; more mapping boilerplate |
| **JDBC (hand-rolled)** | Low abstraction, full SQL control | Simple schemas, few queries, minimal footprint | Verbose; manual mapping; risk of SQL string sprawl |
| **None (repository over files/objects)** | No persistence layer | Tools with no relational data | Not suitable for queryable persistent data |

> **Selection by complexity vs control**: Few entities + simple queries → JDBC. Many entities + relationships → JPA. Complex/specialized SQL → MyBatis.

### 2.3 DI Framework — None (manual) vs Guice vs Spring Context

| Option | Project Size | Pros | Cons |
|--------|--------------|------|------|
| **None (manual, constructor + factory)** | Small / prototype | No dependencies, instant startup, fully transparent | Manual wiring scales poorly past ~10 classes |
| **Guice** | Medium | Lightweight, clean binding API, fast | Smaller ecosystem; must `opens` packages for reflection |
| **Spring Context** | Large / enterprise | Rich features (profiles, config, AOP), huge ecosystem | Heavier; slower startup; avoid full Boot unless needed |

> Wire `FXMLLoader.setControllerFactory(injector::getInstance)` so the DI container builds controllers. For Spring, the launch class must NOT extend `Application` (see spring-boot-integration reference).

### 2.4 Logging — SLF4J+Logback vs Log4j2

| Option | Throughput | Latency (async) | Notes |
|--------|------------|-----------------|-------|
| **SLF4J + Logback** | Good | Good | De facto Java standard; simple config; pairs with most libs |
| **Log4j2** | Higher (disruptor async) | Lower | Best for very high-volume/low-latency logging; more config surface |

> For typical JavaFX apps, SLF4J + Logback is sufficient. Choose Log4j2 only when benchmarks show logging on a hot path.

### 2.5 Testing — JUnit 5 + TestFX + Mockito Setup Matrix

| Test Layer | Tool | Scope | Setup Note |
|------------|------|-------|------------|
| Domain/Application logic | **JUnit 5** | Pure-Java unit tests, no JavaFX runtime | Fast; run in plain `mvn test` |
| UI/ViewModel logic | **JUnit 5 + Mockito** | Mock services/repos; assert ViewModel `Property` values | ViewModel must not hold JavaFX `Control` references |
| UI interaction | **TestFX** | Clicks, typing, node lookup (`lookup("#btn").queryButton()`) | Needs `--add-opens javafx.graphics/javafx.scene=ALL-UNNAMED`; run in a separate `forkCount=1` surefire execution |
| Async/Task behavior | **JUnit 5 + Mockito** | Verify `Task` onSucceeded/onFailed callbacks with mocked service | Use `CompletableFuture` to await background results in tests |

### 2.6 Third-party UI — ControlsFX vs MaterialFX vs FormsFX

| Library | Strength | Use When |
|---------|----------|----------|
| **ControlsFX** | Breadth — notifications, dialogs, validations, `CheckComboBox`, `BreadcrumbBar`, `Wizard` | You need many small, polished controls over stock JavaFX |
| **MaterialFX** | Material Design look + theming, custom controls (MFXButton, MFXTableView, MFXTextField) | You want a consistent Material visual identity out of the box |
| **FormsFX** | Declarative form building with built-in validation and binding | You have many data-entry forms and want validation-free boilerplate removed |

> Pick one primary UI library to keep visual consistency; mix sparingly (e.g., FormsFX for forms + ControlsFX for notifications).

---

## 3. Module Decomposition Strategy

### 3.1 Domain-Driven Module Boundaries

Decompose by **bounded contexts** — each module owns one coherent subdomain and its own domain model. A module exposes a public API (services/ports) and hides internal entities. Two modules must not share an entity type; they communicate via DTOs or domain events.

### 3.2 Shared Kernel vs Anti-Corruption Layer

| Pattern | When | Effect |
|---------|------|--------|
| **Shared Kernel** | Two contexts must evolve the same core concept (e.g., `UserId`) and team trusts each other | Small shared package; changes require coordination |
| **Anti-Corruption Layer (ACL)** | Integrating a legacy/external model that must not leak into your domain | Translate external types to your domain types at the boundary; isolates change |

### 3.3 Package Structure Convention

```
com.example.app
  ├── core          (shared kernel: common types, events, utilities)
  ├── catalog       (bounded context: product browsing/search)
  │   ├── domain    (entities, value objects, domain services)
  │   ├── application (use cases, ports)
  │   ├── infrastructure (repository impls, external adapters)
  │   └── presentation (controllers, viewmodels, fxml)
  ├── order
  ├── user
  └── payment
```

Pattern: `com.example.app.{module}.{layer}`. Layer suffixes are fixed: `domain`, `application`, `infrastructure`, `presentation`.

### 3.4 Module Dependency Rules (DAG Enforcement)

- Dependencies form a **DAG (directed acyclic graph)** — no cycles between modules.
- `presentation` → `application` → `domain`; `infrastructure` → `domain` (implements ports).
- Modules may depend only on another module's **application** package (its public use-case API), never on its `domain` internals or `infrastructure`.
- Enforce with ArchUnit or a module-info layer check; fail the build on violations.

### 3.5 Example: E-commerce Module Decomposition

| Module | Bounded Context | Depends On | Public API |
|--------|-----------------|------------|------------|
| `catalog` | Product browsing, search, inventory view | `core` | `CatalogQueryService` |
| `order` | Cart, order lifecycle, totals | `catalog`, `user` | `OrderService` |
| `user` | Accounts, auth, profiles | `core` | `UserService` |
| `payment` | Payment methods, capture, refund | `order` | `PaymentService` |
| `shipping` | Shipment creation, tracking | `order` | `ShippingService` |

Cross-context communication uses domain events (e.g., `OrderPlacedEvent`) consumed by `payment` and `shipping`, keeping modules decoupled.

---

## 4. Layering Strategy

### 4.1 Four-Layer Model

| Layer | Contains | Depends On | JavaFX Mapping |
|-------|----------|------------|----------------|
| **Presentation** | Controllers, ViewModels, FXML views | Application only | `*Controller`, `*ViewModel`, `.fxml` |
| **Application** | Use cases, orchestration, transaction scripts, ports | Domain | `*UseCase`, port interfaces |
| **Domain** | Entities, value objects, domain services | Nothing (pure Java) | `*Entity`, `*ValueObject` |
| **Infrastructure** | DB, external APIs, file I/O, adapters | Domain (implements ports) | `Jdbc*Repository`, `*Adapter` |

### 4.2 Dependency Inversion

The **domain** layer defines port interfaces (e.g., `OrderRepository`); the **infrastructure** layer provides implementations (`JdbcOrderRepository`). Upper layers depend only on the interface, so the domain stays free of JDBC/Hibernate/JavaFX concerns. This makes domain logic unit-testable with fakes/mocks.

```java
// domain (port) — no framework imports
public interface OrderRepository {
    Order findById(OrderId id);
    void save(Order order);
}

// infrastructure — implements the port
public final class JdbcOrderRepository implements OrderRepository { /* JDBC here */ }
```

### 4.3 Cross-Cutting Concerns

| Concern | Strategy |
|---------|----------|
| **Logging** | SLF4J at module boundaries + service entry/exit; never in domain entities |
| **Security / auth** | Application-layer guard before use case executes; principal passed as parameter, not pulled from UI |
| **Error handling** | Domain throws domain exceptions; application catches and maps to user-facing results; controller shows result, never raw stack traces |
| **Transactions** | Application layer demarcates transaction boundaries (or repository per-call) — not the controller |

### 4.4 Anti-patterns to Avoid

| Anti-pattern | Symptom | Fix |
|--------------|---------|-----|
| **Smart UI** | Business rules embedded in controller/FXML event handlers | Move logic to application/domain; controller only delegates |
| **Anemic domain model** | Entities are getter/setter bags; all logic in services | Push behavior (e.g., `order.cancel()`) into the entity |
| **Business logic in controllers** | `handleSave()` validates, computes, persists | Controller delegates to a use case; validate in domain |
| **Domain depends on frameworks** | `@Entity`/`javafx.*` in domain layer | Keep domain pure; map at infrastructure boundary |
| **UI thread blocking** | Long DB/network call on JavaFX Application Thread | Wrap in `Task`/background thread; update UI via `Platform.runLater` |

---

## 5. Feasibility Assessment Framework

### 5.1 Risk Matrix (Likelihood × Impact)

Score Likelihood (1–5) and Impact (1–5); Risk = L × I. Prioritize risks scoring ≥ 15.

| Technical Risk | Likelihood | Impact | Risk | Mitigation |
|----------------|-----------|--------|------|------------|
| TableView jank with 10K+ rows | 4 | 4 | 16 | Virtualized cells + background paging prototype |
| JavaFX/PF4J plugin classloader conflict | 3 | 4 | 12 | Isolated plugin prototype before adoption |
| Hibernate startup > 3s on cold launch | 3 | 3 | 9 | Switch to JDBC or lazy-init the EMF |
| Cross-platform font/render mismatch | 2 | 3 | 6 | Early visual QA on Windows/macOS/Linux |
| Memory leak from unremoved listeners | 4 | 3 | 12 | Use `WeakChangeListener` / cleanup in `controller.stop()` |

### 5.2 Prototype Priority Scoring

`Priority = Uncertainty (1–5) × Business Value (1–5)`. Build prototypes for the top 1–3 scores.

| Candidate Prototype | Uncertainty | Business Value | Priority | Verdict |
|---------------------|-------------|----------------|----------|---------|
| Large-data TableView virtualization | 4 | 5 | 20 | Prototype first |
| PF4J dynamic feature loading | 4 | 3 | 12 | Prototype if plugin path chosen |
| Reactive event stream (ReactFX) | 3 | 3 | 9 | Time-box spike |
| Custom theming pipeline | 2 | 4 | 8 | Defer to designer |

### 5.3 Performance Benchmark Targets for JavaFX Apps

| Metric | Target | Measurement |
|--------|--------|-------------|
| Cold startup (to first frame interactive) | < 3 s | `Application.start()` entry to `stage.show()` + first idle |
| UI response to user input | < 100 ms | From event dispatch to visual feedback |
| TableView render at 10K rows | 60 fps scroll | Visual profiler / frame timer; virtualized cells |
| Background task → UI update | < 16 ms on FX thread | `Platform.runLater` work must fit one frame |
| Memory (steady state, mid app) | < 256 MB heap | `jcmd <pid> GC.heap_info` after warm-up |

> If a prototype misses a target, record the gap in the prototype result and either adjust the design or relax the requirement via an ADR. Never silently ship below target.
