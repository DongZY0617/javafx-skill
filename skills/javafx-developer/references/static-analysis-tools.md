# Static Analysis Tool Integration Reference

> Integration guide for SpotBugs, PMD, and Checkstyle in JavaFX + Spring Boot projects. Covers Maven plugin configuration, rule sets tailored for JavaFX patterns, report parsing, and how `javafx-runner` executes tools and `javafx-code-reviewer` consumes findings.

## Overview

Three complementary static analysis tools provide deterministic code quality coverage:

| Tool | Focus | What It Catches | Report Location |
|------|-------|-----------------|-----------------|
| **SpotBugs** | Bug patterns | Null pointer risks, resource leaks, concurrency issues, bad casts | `target/spotbugsXml.xml` |
| **PMD** | Code quality | Copy-paste, complexity, unused code, design issues | `target/pmd.xml` |
| **Checkstyle** | Code style | Naming, imports, whitespace, method length, cyclomatic complexity | `target/checkstyle-result.xml` |

### Why All Three?

Each tool covers a different category — they are complementary, not redundant:

- SpotBugs uses bytecode analysis → catches runtime risks (NPE, leaks) that PMD/Checkstyle miss
- PMD uses source code analysis → catches design issues (copy-paste, complexity) that SpotBugs misses
- Checkstyle enforces conventions → catches style violations that affect maintainability but not correctness

## Maven Plugin Configuration

The `pom.xml` template (see `templates/maven/pom.xml`) pre-configures all three plugins. Key configuration points:

### Version Properties

```xml
<properties>
    <spotbugs.version>4.8.6.6</spotbugs.version>
    <spotbugs.maven.plugin.version>4.8.6.6</spotbugs.maven.plugin.version>
    <pmd.version>7.7.0</pmd.version>
    <checkstyle.version>10.18.1</checkstyle.version>
</properties>
```

### Execution Commands

```bash
# Run all three tools (generates XML + HTML reports)
mvn spotbugs:check pmd:check checkstyle:check

# Run individually
mvn spotbugs:check       # → target/spotbugsXml.xml, target/spotbugs.html
mvn pmd:check            # → target/pmd.xml
mvn checkstyle:check     # → target/checkstyle-result.xml

# Run with build (compile first, then analyze)
mvn compile spotbugs:check pmd:check checkstyle:check
```

> **Note**: All plugins are configured with `failOnError=false` / `failsOnError=false` — they generate reports but do NOT fail the Maven build. This allows `javafx-runner` to parse the reports and classify severity independently.

## Rule Set Configuration

### Checkstyle (`checkstyle.xml`)

Located at project root (`${project.basedir}/checkstyle.xml`). Tailored for JavaFX:

| Rule Category | Key Rules | JavaFX Consideration |
|---------------|-----------|---------------------|
| Naming | `ConstantName`, `MemberName`, `MethodName` | Standard conventions |
| Imports | `AvoidStarImport`, `UnusedImports`, `ImportOrder` | Groups: java, javax, jakarta, org, com |
| Code Structure | `MethodLength(max=80)`, `ParameterNumber(max=7)` | — |
| Complexity | `CyclomaticComplexity(max=15)` | Aligned with NFR-MAINT-002 |
| Coding | `EmptyBlock`, `EqualsHashCode`, `NestedIfDepth(max=3)` | — |
| Design | `FinalClass`, `HideUtilityClassConstructor` | — |

**JavaFX-specific**: FXML controllers use `@FXML` on package-private fields — Checkstyle is configured with `SuppressionCommentFilter` and `VisibilityModifier` allows `packageAllowed=true` and `protectedAllowed=true`.

### PMD (`pmd-ruleset.xml`)

Located at project root (`${project.basedir}/pmd-ruleset.xml`). Uses PMD 7 category-based rules:

| Category | Key Rules | JavaFX Consideration |
|----------|-----------|---------------------|
| Best Practices | `UnusedPrivateField` (excluded for `@FXML`), `UnusedPrivateMethod` (excluded for FXML controllers) | XPath suppression for `@FXML`-annotated fields |
| Code Style | `ClassNamingConventions` with suffix `Controller\|Service\|Repository\|View\|Model\|ViewModel\|Presenter\|Util\|Exception` | Enforces naming conventions |
| Design | `CognitiveComplexity(reportLevel=20)`, `TooManyMethods(max=20)`, `TooManyFields(max=25)` | — |
| Error Prone | `BeanMembersShouldSerialize` (excluded — JavaFX Properties are not standard beans) | — |
| Performance | Full category | Catches inefficient string operations, unnecessary object creation |
| Security | Full category | Catches hardcoded passwords, insecure random |

**JavaFX-specific suppressions**: PMD's `UnusedPrivateField` and `UnusedPrivateMethod` are configured with XPath suppression for `@FXML`-annotated fields — these fields/methods are injected/called via reflection by `FXMLLoader`, not direct code.

### SpotBugs (`spotbugs-exclude.xml`)

Located at project root (`${project.basedir}/spotbugs-exclude.xml`). Excludes common JavaFX false positives:

| Excluded Pattern | Reason |
|-----------------|--------|
| `UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR` on `@FXML` classes | FXML-injected fields are not set in constructor |
| `UWF_NULL_FIELD` on `*Property` types | JavaFX Properties use lazy initialization pattern (null until first access) |
| `UPM_UNCALLED_PUBLIC_METHOD` on `@FXML` methods | FXML event handlers are called via reflection by FXMLLoader |
| `UPM_UNCALLED_PUBLIC_METHOD` on `*Application` classes | JavaFX lifecycle methods (`start()`, `init()`, `stop()`) may appear unused |
| `EI_EXPOSE_REP` on `*Test` classes | Test classes may expose internal state for assertion |
| All checks on `generated`/`auto` packages | Generated code is not manually maintained |

## Report Parsing

`javafx-runner` parses the XML reports generated by each tool. The unified parsing approach:

### SpotBugs Report (`target/spotbugsXml.xml`)

```xml
<SpotBugs>
  <BugInstance type="NP_NULL_ON_SOME_PATH" priority="2" category="CORRECTNESS">
    <Class classname="com.example.UserService"/>
    <Method classname="com.example.UserService" name="findById" signature="(J)Lcom/example/User;"/>
    <SourceLine classname="com.example.UserService" start="45" end="45"/>
  </BugInstance>
</SpotBugs>
```

**Parsing fields**:
- `type` → rule ID (e.g., `NP_NULL_ON_SOME_PATH`)
- `priority` → 1=High, 2=Medium, 3=Low
- `category` → CORRECTNESS, PERFORMANCE, SECURITY, etc.
- `SourceLine.start` → line number
- `Class.classname` + `Method.name` → AST signature `{package}.{Class}#{method}({params})`

### PMD Report (`target/pmd.xml`)

```xml
<pmd>
  <file name="src/main/java/com/example/UserService.java">
    <violation beginline="45" endline="45" rule="AvoidInstantiatingObjectsInLoops"
               ruleset="Performance" priority="3">
      Avoid instantiating objects in loops
    </violation>
  </file>
</pmd>
```

**Parsing fields**:
- `rule` → rule ID
- `ruleset` → category (Performance, Design, etc.)
- `priority` → 1=High, 2=Medium High, 3=Medium, 4=Medium Low, 5=Low
- `beginline` → line number
- `file.name` → source file path

### Checkstyle Report (`target/checkstyle-result.xml`)

```xml
<checkstyle>
  <file name="src/main/java/com/example/UserService.java">
    <error line="45" column="12" severity="warning"
           message="Method length is 85 lines (max allowed is 80)."
           source="com.puppycrawl.tools.checkstyle.checks.sizes.MethodLengthCheck"/>
  </file>
</checkstyle>
```

**Parsing fields**:
- `source` → rule ID (Checkstyle check class name)
- `severity` → `error` or `warning`
- `line`, `column` → location
- `file.name` → source file path

### Unified Issue Mapping

All three tool findings are mapped to a unified issue structure for `javafx-code-reviewer` consumption:

```json
{
  "tool": "spotbugs | pmd | checkstyle",
  "rule_id": "NP_NULL_ON_SOME_PATH | AvoidInstantiatingObjectsInLoops | MethodLengthCheck",
  "severity": "Critical | Major | Minor | Info",
  "category": "CORRECTNESS | PERFORMANCE | STYLE",
  "source_file": "src/main/java/com/example/UserService.java",
  "line_number": 45,
  "ast_node_signature": "com.example.UserService#findById(Long)",
  "message": "Possible null pointer dereference of user",
  "fix_suggestion": "Add null check before accessing user fields"
}
```

**Priority to severity mapping**:

| Tool | Priority | Mapped Severity |
|------|----------|-----------------|
| SpotBugs | 1 (High) | Major |
| SpotBugs | 2 (Medium) | Minor |
| SpotBugs | 3 (Low) | Info |
| PMD | 1-2 (High) | Major |
| PMD | 3 (Medium) | Minor |
| PMD | 4-5 (Low) | Info |
| Checkstyle | error | Minor |
| Checkstyle | warning | Info |

> **Note**: Tool findings never map to Critical — Critical is reserved for compilation errors and runtime crashes discovered by runner's dynamic verification. Tool findings are always deterministic code quality issues.

## Runner Integration

`javafx-runner` executes static analysis as part of its verification workflow. See the runner's Step 2.5 (Static Analysis Verification) for the execution protocol.

### Execution Order

```
Step 2: Compile Verification (mvn compile)
    ↓ (if compile passes)
Step 2.5: Static Analysis Verification (mvn spotbugs:check pmd:check checkstyle:check)
    ↓
Step 3: Runtime Verification (mvn javafx:run)
```

Static analysis runs **after compilation** (tools need compiled bytecode for SpotBugs) and **before runtime** (catch issues before launching the app).

### Short-Circuit

- If compile verification fails → skip static analysis (no bytecode to analyze)
- Static analysis findings do NOT short-circuit runtime verification — they are recorded as issues but don't block runtime testing

### Report Integration

Static analysis findings are included in the runner's verification report as a separate dimension ("Static Analysis Verification"), with the same Fix Handoff format as other dimensions.

## Reviewer Integration

`javafx-code-reviewer` consumes the unified static analysis findings as Dimension 10 (Static Analysis Tool Findings). The reviewer:

1. Reads `static-analysis-findings.json` produced by runner (if exists)
2. For each tool finding, checks if the reviewer's own LLM review already identified the same issue (deduplication)
3. Tool findings NOT already found by LLM review are added as supplementary issues with `source: "spotbugs" | "pmd" | "checkstyle"`
4. Tool findings already found by LLM review are noted as "confirmed by static analysis tool" for higher confidence
5. The reviewer may also use tool findings to validate its own conclusions (e.g., if SpotBugs reports `NP_NULL_ON_SOME_PATH`, the reviewer's NPE risk finding is corroborated)

### Deduplication Logic

| Scenario | Action |
|----------|--------|
| LLM found issue + tool found same issue | Keep LLM finding, annotate "confirmed by {tool}" |
| LLM found issue + tool did NOT find it | Keep LLM finding (LLM can find semantic issues tools miss) |
| LLM did NOT find issue + tool found it | Add tool finding as supplementary issue with `source: {tool}` |

## Gradle Configuration

For Gradle projects, equivalent plugins:

```groovy
plugins {
    id 'com.github.spotbugs' version '6.0.22'
    id 'pmd'
    id 'checkstyle'
}

spotbugs {
    effort = 'max'
    reportLevel = 'low'
    excludeFilter = file('spotbugs-exclude.xml')
}

pmd {
    toolVersion = '7.7.0'
    rulesFiles = files('pmd-ruleset.xml')
    consoleOutput = false
}

checkstyle {
    toolVersion = '10.18.1'
    configFile = file('checkstyle.xml')
}

// Generate XML reports
spotbugsMain {
    reports {
        xml.required = true
        html.required = true
    }
}
```

## Common Pitfalls

1. **Tools fail the build**: All plugins are configured with `failOnError=false` — if tools find issues, the build still succeeds, allowing runner to parse reports independently. If you enable `failOnError=true`, the build will fail on any finding, blocking runner execution
2. **False positives on @FXML fields**: Without the XPath suppression in PMD and the exclude filter in SpotBugs, `@FXML`-injected fields will be flagged as "unused" — always include the JavaFX-specific exclusions
3. **Missing config files**: If `checkstyle.xml`, `pmd-ruleset.xml`, or `spotbugs-exclude.xml` are missing, the plugins will use default rules (which may be too strict or too lenient). The developer template includes all three config files
4. **SpotBugs needs compiled classes**: SpotBugs analyzes bytecode, not source — it must run after `mvn compile`. If you run `mvn spotbugs:check` without compiling first, you'll get an empty report
5. **PMD version mismatch**: PMD 7.x uses category-based rulesets (`category/java/bestpractices.xml`), while PMD 6.x used ruleset files. Ensure the `pmd-ruleset.xml` matches the PMD version in the plugin
6. **Checkstyle suppressions file optional**: The `checkstyle.xml` references `checkstyle-suppressions.xml` with `optional=true` — if the file doesn't exist, Checkstyle runs without file-level suppressions
