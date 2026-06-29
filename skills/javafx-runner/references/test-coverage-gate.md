# Test Coverage Gate — JaCoCo Coverage Threshold Rules

This document defines the test coverage measurement rules, threshold configuration, and quality gate integration for JavaFX projects verified by `javafx-runner`.

## Overview

The test coverage gate enforces minimum coverage thresholds on critical code paths (Controller and ViewModel classes) to ensure that generated code is adequately tested. Coverage is measured using the JaCoCo Maven plugin, which instruments bytecode during test execution and generates an XML report parseable by `javafx-runner`.

## JaCoCo Plugin Configuration

### pom.xml Configuration

Add the JaCoCo plugin to the `<build><plugins>` section of `pom.xml`:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>CLASS</element>
                        <includes>
                            <include>com.example.controller.*</include>
                            <include>com.example.viewmodel.*</include>
                        </includes>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.60</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Key Configuration Points

| Element | Value | Purpose |
|---------|-------|---------|
| `jacoco-maven-plugin` version | 0.8.12 | Latest stable as of 2026, supports JDK 21+ |
| `prepare-agent` goal | default phase | Instruments bytecode before test execution |
| `report` goal | `test` phase | Generates XML and HTML reports in `target/site/jacoco/` |
| `check` goal | default phase | Enforces coverage rules, fails build if thresholds not met |
| `element` | `CLASS` | Coverage measured per class, not per package or method |
| `includes` | `com.example.controller.*`, `com.example.viewmodel.*` | Only critical paths are gated |
| `counter` | `LINE` | Line coverage is the primary metric |
| `minimum` | `0.60` | 60% line coverage required on critical paths |

## Coverage Thresholds

### Default Thresholds

| Metric | Threshold | Scope | Severity if Below |
|--------|-----------|-------|-------------------|
| Line coverage | >= 60% | Controller and ViewModel classes | Major |
| Branch coverage | >= 40% | Controller and ViewModel classes | Minor (advisory) |
| Class coverage | 100% | All Controller and ViewModel classes must have at least one test | Major if any class has 0% |
| Method coverage | >= 50% | Public methods in Controller and ViewModel | Minor |

### Threshold Adjustment Rules

- **Small projects** (< 5 classes): Line coverage threshold lowered to 50% (fewer classes, harder to reach 60% with basic tests)
- **Large projects** (> 20 classes): Branch coverage threshold raised to 50% (more complex logic requires better branch coverage)
- **Spring Boot integrated**: Controller coverage threshold lowered to 50% (DI complexity makes 60% harder to achieve with unit tests)

## Report Parsing

### JaCoCo XML Report Structure

The XML report is generated at `target/site/jacoco/jacoco.xml`. `javafx-runner` parses this file to extract coverage metrics:

```xml
<report>
  <package name="com/example/controller">
    <class name="com/example/controller/MainController">
      <method name="initialize" desc="()V">
        <counter type="INSTRUCTION" missed="5" covered="20"/>
        <counter type="LINE" missed="1" covered="8"/>
        <counter type="BRANCH" missed="1" covered="3"/>
      </method>
      <counter type="LINE" missed="3" covered="25"/>
      <counter type="BRANCH" missed="2" covered="8"/>
    </class>
  </package>
</report>
```

### Parsing Logic

1. **Extract class-level counters**: For each class in `com.example.controller.*` or `com.example.viewmodel.*`, read `LINE` and `BRANCH` counters
2. **Compute coverage ratio**: `covered / (covered + missed)` for each class
3. **Identify uncovered methods**: Methods with `LINE.missed > 0` and `LINE.covered == 0` are flagged as "uncovered"
4. **Generate fix handoff**: If coverage is below threshold, create a Fix Handoff entry:
   - `target_file`: The test file for the uncovered class
   - `target_lines`: End of the test class (for appending new test methods)
   - `fix_type`: `insert`
   - `fix_priority`: Based on coverage gap (larger gap = higher priority)
   - `code_fingerprint`: Hash of the test class's last method
   - `anchor_pattern`: Last 2 lines + closing brace of the test class

### Coverage Report Output

```markdown
## JaCoCo Coverage Report

### Summary
- **Overall Line Coverage**: 72.5%
- **Controller Line Coverage**: 65.3% (threshold: 60%) — PASS
- **ViewModel Line Coverage**: 48.2% (threshold: 60%) — FAIL
- **Branch Coverage**: 38.7% (threshold: 40%) — Minor

### Uncovered Methods (ViewModel)
| Class | Method | Lines Missed | Lines Covered |
|-------|--------|-------------|---------------|
| UserViewModel | validateInput() | 12 | 0 |
| UserViewModel | saveUser() | 8 | 3 |

### Fix Handoff
- `target_file: src/test/java/com/example/viewmodel/UserViewModelTest.java`
- `target_lines: [end of file]`
- `fix_type: insert`
- `fix_priority: 3`
- `code_fingerprint: [hash]`
- `anchor_pattern: [last 2 lines + closing brace]`

### Corrected Example
```java
@Test
void testValidateInput() {
    UserViewModel vm = new UserViewModel();
    vm.nameProperty().set("");
    assertFalse(vm.validateInput(), "Empty name should fail validation");
    vm.nameProperty().set("Valid Name");
    assertTrue(vm.validateInput(), "Valid name should pass validation");
}

@Test
void testSaveUser() {
    UserViewModel vm = new UserViewModel();
    vm.nameProperty().set("Test User");
    vm.emailProperty().set("test@example.com");
    assertTrue(vm.saveUser(), "Valid user should save successfully");
    assertNotNull(vm.savedUserProperty().get());
}
```
```

## Quality Gate Integration

### Gate Logic

| Test Verification Result | Coverage Result | Overall Gate |
|--------------------------|----------------|-------------|
| All tests pass | Coverage >= threshold | PASS |
| All tests pass | Coverage < threshold | Conditional Pass (Major issue: coverage gap) |
| Test failures | N/A (coverage report may be incomplete) | FAIL |
| No test classes | N/A | FAIL (Major: no tests for Controller/ViewModel) |

### Interaction with Combined Quality Gate

The coverage threshold is part of the **runner's Individual Gate**, which feeds into the **Combined Quality Gate**:

- If coverage is below threshold → runner result is "Conditional Pass" → combined gate is "Fail" → loop continues
- If coverage meets threshold → runner result can be "Pass" → combined gate depends on reviewer result
- The coverage gate does NOT override Critical issues (compilation errors, runtime crashes always take precedence)

## CI/CD Integration

When running in CI (GitHub Actions / GitLab CI), the JaCoCo report is:
1. Generated as part of `mvn test jacoco:report`
2. Parsed by `javafx-runner` from `target/site/jacoco/jacoco.xml`
3. Published as a CI artifact for trend analysis
4. Can be visualized using JaCoCo HTML report (`target/site/jacoco/index.html`)

For CI-specific configuration, see `javafx-developer`'s `references/ci-cd-pipeline.md` and `templates/ci/github-actions.yml`.
