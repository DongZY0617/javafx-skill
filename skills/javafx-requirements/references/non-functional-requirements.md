# Non-Functional Requirements Reference

> Categories, quantification methods, and verification mapping for non-functional requirements in JavaFX desktop applications.

## Why NFRs Matter

Functional requirements describe what the app does. Non-functional requirements describe how well it does it. For JavaFX desktop apps, NFRs are often the difference between a usable tool and an abandoned one:

- A CRUD app that takes 15 seconds to start will frustrate users even if every feature works
- A data-handling app without input validation is a security liability even if the UI is beautiful
- An app that only runs on Windows limits adoption in mixed-OS environments

## NFR Categories for JavaFX

### Performance (NFR-PERF-xxx)

Performance NFRs govern responsiveness and throughput. For JavaFX, the critical metrics are:

| Metric | Typical Target | Measurement Method |
|--------|---------------|-------------------|
| Cold startup time | ≤ 3 seconds | Time from `Application.launch()` to `primaryStage.show()` |
| UI frame rate | ≥ 60 FPS (16ms/frame) | Frame timing via PulseLogger or FXCanvas profiling |
| UI responsiveness | ≤ 100ms for user actions | Time from event to visual feedback |
| Table render time (10K rows) | ≤ 500ms | Time from data load to TableView fully rendered |
| Memory usage (idle) | ≤ 256MB heap | JVM heap after full load, measured via `Runtime.totalMemory()` |
| Database query (single) | ≤ 50ms | Query execution time via JDBC timing |

**Quantification template**:
```markdown
### NFR-PERF-001: Cold Startup Time

- **Description**: The application must launch and display the main window within an acceptable time frame
- **Target**: ≤ 3 seconds from launch to first window visible
- **Measurement**: Time from `Application.launch()` to `primaryStage.show()` completion, measured on a reference machine (Intel i5-1135G7, 16GB RAM, SSD, JDK 23)
- **Verification**: `javafx-tester` performance testing dimension — `MainWindowTest#testColdStartupTime`
- **Priority**: Must
- **Rationale**: Users expect desktop apps to launch quickly; > 5s leads to perceived "hang" and force-quits
```

### Security (NFR-SEC-xxx)

Security NFRs govern data protection and attack resistance. Apply when the app handles sensitive data, authentication, or network communication:

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Input validation | 100% of user inputs validated | Code review + fuzz testing |
| SQL injection prevention | 100% parameterized queries | Static analysis (SpotBugs) + code review |
| Credential storage | No plaintext credentials in code | Code review + secrets scanning |
| Path traversal prevention | All file paths sanitized | Fuzz testing + code review |
| Dependency vulnerabilities | 0 critical CVEs | OWASP Dependency-Check |
| WebView security | No `setJavaScriptEnabled` on untrusted content | Code review |

### Compatibility (NFR-COMPAT-xxx)

Compatibility NFRs govern cross-platform behavior. JavaFX's write-once-run-anywhere promise requires explicit platform targets:

| Platform | Typical Target | Considerations |
|----------|---------------|----------------|
| Windows 10/11 | Must support | MSI/EXE packaging, registry integration |
| macOS 12+ | Must/Should support | DMG/PKG packaging, notarization, dark mode |
| Ubuntu 22.04+ | Should support | DEB/RPM packaging, GTK theme integration |
| JDK version | Minimum supported | Specify floor (e.g., JDK 17) and ceiling |

**Quantification example**:
```markdown
### NFR-COMPAT-001: Cross-Platform File Paths

- **Description**: The application must handle file paths correctly across Windows, macOS, and Linux
- **Target**: 100% of file operations use `Path` API (not string concatenation), no hardcoded path separators
- **Measurement**: Static analysis scan for `File.separator` usage and hardcoded `\` or `/` in paths
- **Verification**: `javafx-runner` cross-platform configuration check + `javafx-tester` on target platforms
- **Priority**: Must
```

### Usability (NFR-UI-xxx)

Usability NFRs govern user experience quality:

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Color contrast (text) | ≥ 4.5:1 (WCAG AA) | Automated contrast check |
| Keyboard accessibility | 100% of actions reachable via keyboard | Manual + automated a11y test |
| Screen reader support | Key controls have ARIA-like labels | `javafx-tester` accessibility dimension |
| Error messages | User-readable, no stack traces | Code review + manual testing |
| Responsive layout | 800x600 to 1920x1080 without clipping | Visual verification at boundary sizes |

### Reliability (NFR-REL-xxx)

Reliability NFRs govern failure handling and uptime:

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Crash recovery | Auto-recover unsaved data on next launch | Manual crash test + data persistence check |
| Graceful degradation | Network failure shows offline mode, not crash | Network simulation test |
| Error logging | 100% of uncaught exceptions logged | Code review + crash handler test |
| Data integrity | No data corruption on abnormal exit | Transaction + journal test |

### Maintainability (NFR-MAINT-xxx)

Maintainability NFRs govern long-term code health:

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Test coverage (critical paths) | ≥ 80% line coverage | JaCoCo report |
| Test coverage (overall) | ≥ 60% line coverage | JaCoCo report |
| Cyclomatic complexity | ≤ 15 per method | PMD/Checkstyle |
| Code duplication | ≤ 3% duplicated lines | CPD/PMD |
| Javadoc coverage (public API) | ≥ 90% | `javafx-docgen` coverage metric |

## NFR Quantification Rules

### Rule 1: Every NFR Must Have a Number

**Bad**: "The app should be fast"
**Good**: "Cold startup ≤ 3 seconds"

### Rule 2: Every NFR Must Have a Measurement Method

**Bad**: "The app should be secure"
**Good**: "0 critical CVEs as reported by OWASP Dependency-Check"

### Rule 3: Every NFR Must Map to a Verification Method

Each NFR must reference which skill/dimension will verify it:
- Performance NFRs → `javafx-tester` performance dimension
- Security NFRs → `javafx-tester` security dimension + `javafx-code-reviewer` security checklist
- Compatibility NFRs → `javafx-runner` cross-platform check + `javafx-tester` on target platforms
- Usability NFRs → `javafx-tester` accessibility dimension
- Maintainability NFRs → `javafx-runner` JaCoCo coverage gate + `javafx-code-reviewer` structure review

### Rule 4: NFRs Must Be Prioritized

Use MoSCoW priority for NFRs:
- **Must**: Non-negotiable — the app is not deliverable without meeting this
- **Should**: Important — should be met, but deliverable with documented gap if not
- **Could**: Nice to have — met if time permits, documented as future work if not
- **Won't**: Explicitly out of scope for this release

## NFR Conflict Resolution

NFRs often conflict. Common trade-offs:

| Conflict | Resolution Approach |
|----------|---------------------|
| Performance vs. Security | Encryption adds latency — benchmark both, negotiate acceptable balance |
| Compatibility vs. Features | Platform-specific features limit compatibility — isolate platform code behind interfaces |
| Usability vs. Performance | Rich animations improve UX but cost CPU — use on idle, disable under load |
| Maintainability vs. Time-to-market | Tests slow initial delivery but enable future speed — set minimum coverage, not zero |

When a conflict is identified, document it as an ADR (Architecture Decision Record) in the architect phase.
