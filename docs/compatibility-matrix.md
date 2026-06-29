# Version Compatibility Matrix

> JavaFX Skill Set — JDK × JavaFX × Skill version compatibility reference.
> Last updated: 2026-06-30

## 1. JDK × JavaFX Compatibility

| JDK Version | JavaFX 17 | JavaFX 21 | JavaFX 24 | JavaFX 25 | JavaFX 26 |
|-------------|-----------|-----------|-----------|-----------|-----------|
| JDK 17 (LTS) | ✅ Full | ✅ Full | ⚠️ Limited | ❌ Not supported | ❌ Not supported |
| JDK 21 (LTS) | ✅ Full | ✅ Full | ✅ Full | ✅ Full | ⚠️ Limited |
| JDK 25 (LTS) | ✅ Full | ✅ Full | ✅ Full | ✅ Full | ✅ Full |

### Legend
- ✅ **Full**: All skill features supported, no known limitations
- ⚠️ **Limited**: Most features work, but some advanced features require newer JDK
- ❌ **Not supported**: JavaFX version requires a newer JDK than available

### Known Limitations
- **JDK 17 + JavaFX 24**: Virtual Thread support unavailable (requires JDK 21+); `javafx-runner` concurrency features limited to platform threads
- **JDK 21 + JavaFX 26**: Experimental — JavaFX 26 is the latest release, some third-party libraries may not yet support it
- **JDK 25 + JavaFX 17**: Works but not recommended — JavaFX 17 is EOL, security patches no longer backported

## 2. Skill Version Compatibility

All skills are currently at version **1.0**. The following table shows feature availability per skill version:

| Skill | Version | Min JDK | Min JavaFX | Key Features |
|-------|---------|---------|------------|--------------|
| javafx-architect | 1.0 | 17 | 17 | Technology selection, UML, ADR, prototype validation |
| javafx-designer | 1.0 | 17 | 17 | FXML prototypes, CSS themes, icon configs, interaction flows |
| javafx-developer | 1.0 | 17 | 17 | Code generation, Fix Consumption, concurrent fixes, AST matching |
| javafx-code-reviewer | 1.0 | 17 | 17 | 9 review dimensions, Fix Handoff generation, incremental review |
| javafx-runner | 1.0 | 17 | 17 | Compile/runtime/packaging verification, incremental compilation |
| javafx-tester | 1.0 | 17 | 17 | Performance, security, accessibility testing, Test Gate |
| javafx-refactorer | 1.0 | 17 | 17 | Code smell detection, refactoring patterns, tech debt management |
| javafx-docgen | 1.0 | 17 | 17 | API reference, user manual, architecture doc, changelog |
| javafx-deployer | 1.0 | 17 | 17 | jpackage, jlink, cross-platform packaging, secret management |
| javafx-orchestrator | 1.0 | 17 | 17 | Loop state machine, combined gate, serialization, recovery |

## 3. Build Tool Compatibility

| Build Tool | Version | Status | Notes |
|------------|---------|--------|-------|
| Maven | 3.8+ | ✅ Full support | Default build tool for all skills |
| Maven | 3.6-3.7 | ⚠️ Partial | Works but `mvn compile -q` incremental detection may be unreliable |
| Gradle | 8.0+ | ✅ Full support | Supported via `build.gradle` detection in javafx-runner |
| Gradle | 7.x | ⚠️ Partial | JPackage plugin may require 8.0+ |
| Ant | — | ❌ Not supported | No Ant build scripts generated |

## 4. Third-Party Library Compatibility

| Library | Min Version | Required JDK | Used By | Notes |
|---------|-------------|---------------|---------|-------|
| ControlsFX | 11.1.2 | 17 | developer, designer | JavaFX 17+ compatible |
| MaterialFX | 11.17.0 | 17 | developer, designer | Material Design components |
| FormsFX | 11.6.0 | 17 | developer | Form generation |
| ReactFX | 2.0.0 | 17 | developer | Reactive bindings (optional) |
| SLF4J | 2.0.0 | 17 | developer, runner | Logging facade |
| Logback | 1.4.0 | 17 | developer, runner | SLF4J implementation |
| JUnit 5 | 5.10.0 | 17 | tester, runner | Test framework |
| TestFX | 4.0.18 | 17 | tester | JavaFX UI testing |
| Mockito | 5.5.0 | 17 | tester | Mocking framework |
| JaCoCo | 0.8.11 | 17 | runner, tester | Code coverage |

## 5. Recommended Combinations

### 5.1 Production LTS Stack (Recommended for Enterprise)
- **JDK**: 21 (LTS)
- **JavaFX**: 21 (LTS)
- **Build Tool**: Maven 3.9+
- **Skills**: All 9 skills + orchestrator
- **Rationale**: Maximum stability, long-term support, all features available

### 5.2 Modern Development Stack (Recommended for New Projects)
- **JDK**: 25 (LTS)
- **JavaFX**: 25 (LTS)
- **Build Tool**: Maven 3.9+ or Gradle 8.5+
- **Skills**: All 9 skills + orchestrator
- **Rationale**: Latest LTS, virtual threads support, latest JavaFX features

### 5.3 Legacy Migration Stack (For Existing JavaFX 17 Projects)
- **JDK**: 17 (LTS)
- **JavaFX**: 17 (LTS)
- **Build Tool**: Maven 3.8+
- **Skills**: developer, code-reviewer, runner, refactorer (minimal set for migration)
- **Rationale**: Use refactorer to modernize code, then upgrade JDK/JavaFX to 21+
- **Upgrade path**: 17 → 21 → 25 (incremental, test at each step)

### 5.4 Cutting-Edge Stack (For Early Adopters)
- **JDK**: 25 (LTS)
- **JavaFX**: 26 (Latest)
- **Build Tool**: Gradle 8.5+
- **Skills**: All 9 skills + orchestrator
- **Rationale**: Access to newest JavaFX features, experimental libraries
- **Warning**: Some third-party libraries may not yet support JavaFX 26

## 6. Feature Availability Matrix

| Feature | JDK 17 | JDK 21 | JDK 25 | Notes |
|---------|--------|--------|--------|-------|
| Basic code generation | ✅ | ✅ | ✅ | All JDK versions |
| Incremental compilation | ✅ | ✅ | ✅ | Maven incremental compiler |
| Concurrent fix application | ✅ | ✅ | ✅ | Platform threads (max 4 parallel) |
| Virtual Thread optimization | ❌ | ✅ | ✅ | JDK 21+ required |
| Pattern matching for switch | ❌ | ✅ | ✅ | JDK 21+ required |
| Record patterns | ❌ | ✅ | ✅ | JDK 21+ required |
| Sealed classes | ✅ | ✅ | ✅ | JDK 17+ |
| Text blocks | ✅ | ✅ | ✅ | JDK 17+ |
| Scoped values | ❌ | ❌ | ✅ | JDK 25+ required (preview in 21) |

## 7. Breaking Changes History

| Version | Breaking Change | Migration Guide | Affected Skills |
|---------|----------------|-----------------|-----------------|
| 1.0 | Initial release | N/A | All |
