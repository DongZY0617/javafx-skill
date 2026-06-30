# Security Testing Rules

This document defines the security testing rules, vulnerability classification, and fuzzing patterns for JavaFX applications. It serves as the reference for `javafx-tester`'s Security Testing dimension.

## 1. Dependency Vulnerability Scan

### 1.1 OWASP Dependency-Check Execution

**Tool**: OWASP Dependency-Check (CLI or Maven plugin)

**Maven plugin execution**:
```bash
mvn org.owasp:dependency-check-maven:check -Dformat=JSON -DoutputDirectory=target/security-report
```

**CLI execution**:
```bash
dependency-check --project <project-name> --scan target/ --format JSON --out target/security-report/
```

**Output parsing**: Parse `dependency-check-report.json` for:
- CVE ID (e.g., `CVE-2024-12345`)
- CVSS v3 score (0.0-10.0)
- Affected dependency (groupId:artifactId:version)
- Vulnerability description
- Remediation guidance (upgrade version)

### 1.2 CVSS Severity Classification

| CVSS v3 Score | Severity | Tester Action |
|--------------|----------|---------------|
| ≥ 9.0 (Critical) | Critical | Block delivery, fix immediately |
| 7.0-8.9 (High) | Critical | Block delivery, fix immediately |
| 4.0-6.9 (Medium) | Major | Fix within current iteration |
| < 4.0 (Low) | Minor | Record, monitor for upstream fixes |

### 1.3 Suppression Validation

Check `suppressions.xml` (if exists) for:
1. **Overly broad suppressions**: Suppressing entire dependencies or all CVEs for a package → Major (may hide real vulnerabilities)
2. **Expired suppressions**: Suppressions with past expiry dates → Minor (should be removed)
3. **Missing justification**: Suppressions without `<notes>` explaining why → Minor (documentation gap)

### 1.4 Remediation Fix Handoff

When a CVE is found, the Fix Handoff targets the `pom.xml` (or `build.gradle`) dependency version:
```json
{
  "target_file": "pom.xml",
  "target_lines": "45-48",
  "fix_type": "replace",
  "fix_priority": 1,
  "code_fingerprint": "sha256...",
  "anchor_pattern": "<dependency>...<artifactId>log4j-core</artifactId>...<version>2.14.0</version>",
  "corrected_example": "<version>2.17.1</version>  <!-- Fixed CVE-2021-44228 -->"
}
```

## 2. Input Fuzz Testing

### 2.1 Text Input Fuzzing Patterns

For every `TextField`, `TextArea`, `ComboBox` (editable), and `PasswordField` in the application, inject the following test inputs:

| Category | Test Inputs | Expected Behavior |
|----------|------------|-------------------|
| Empty | `""` | Graceful handling, no exception |
| Very long | `"A" * 10000` | UI remains responsive, input truncated or handled |
| Special chars | `<>"'&\n\r\t` | No parsing errors, no injection |
| SQL injection | `' OR 1=1 --`, `; DROP TABLE users--` | No SQL execution, input sanitized |
| XSS | `<script>alert(1)</script>` | No script execution in WebView |
| Path traversal | `../../etc/passwd`, `..\\..\\windows\\system32` | No file access, path validated |
| Null bytes | `"test\0malicious"` | Null byte stripped or rejected |
| Unicode | `\u0000`, `\uFFFF`, emoji sequences | Handled correctly, no encoding errors |
| Format strings | `%s%s%s%s`, `%n%n%n` | No format string vulnerability |
| Command injection | `; rm -rf /`, `| cat /etc/passwd` | No command execution |

### 2.2 Fuzz Testing Methodology

1. **Identify all input points**: Scan FXML files for `TextField`, `TextArea`, `ComboBox`, `PasswordField` elements
2. **Inject test inputs**: For each input point, inject all fuzz patterns from the table above
3. **Monitor for failures**:
   - `stderr` for uncaught exceptions
   - `Platform.runLater` errors
   - Application crashes (process exit)
   - Application hangs (no response for 5+ seconds)
4. **Record results**: For each input × pattern combination, record Pass (no failure) or Fail (exception/crash/hang)

### 2.3 Fuzz Testing Severity

| Outcome | Severity | Description |
|---------|----------|-------------|
| No exceptions, no crashes | Pass | Input properly handled |
| Minor UI glitches | Minor | Layout breaks but no crash, recovers on next interaction |
| Uncaught exception | Major | Input not validated, exception logged but app continues |
| Application crash | Critical | Input can cause denial of service |
| Application hang | Critical | Input causes infinite loop or deadlock |

### 2.4 SQL Injection Detection

If the application uses a database (JPA/Hibernate/JDBC), specifically test:
1. **JPQL injection**: Inject JPQL keywords (`' OR 1=1`, `' UNION SELECT`)
2. **Native SQL injection**: If native queries are used, inject SQL patterns
3. **HQL injection**: Inject HQL-specific patterns

**Cross-reference**: `../javafx-code-reviewer/references/database-integration.md` -- SQL Injection Prevention (static check for parameterized queries)

## 3. WebView Security

> Skip all checks in this section if the project does not use `WebView` / `WebEngine`.

### 3.1 JavaScript Access Control

**Check**: Is `webEngine.setJavaScriptEnabled(true)` called?

| Condition | Severity | Fix |
|-----------|----------|-----|
| JavaScript disabled (default) | Pass | No action |
| JavaScript enabled, loading trusted local content only | Pass | Acceptable |
| JavaScript enabled, loading external URLs | Major | Validate URLs, implement CSP |
| JavaScript enabled, loading user-provided URLs | Critical | Do not enable JS for untrusted content |

### 3.2 Content Source Validation

**Check**: What URLs/content does `WebEngine` load?

| Source | Severity | Fix |
|--------|----------|-----|
| Hardcoded trusted HTTPS URLs | Pass | Acceptable |
| User-provided URLs with validation | Pass | URL whitelist/sanitization |
| User-provided URLs without validation | Major | Implement URL validation |
| `file://` protocol from user input | Critical | Block file:// access from untrusted input |

### 3.3 Cross-Origin Resource Sharing

**Check**: Does `WebEngine` load content from multiple origins?

| Condition | Severity | Fix |
|-----------|----------|-----|
| Single origin | Pass | No CORS concerns |
| Multiple origins with proper CORS headers | Pass | Server-side CORS configured |
| Multiple origins without CORS validation | Major | Implement origin validation |

## 4. Sensitive Data Exposure

### 4.1 Password Handling

**Check**: How are passwords handled in the application?

| Condition | Severity | Fix |
|-----------|----------|-----|
| Uses `PasswordField` (masked input) | Pass | Correct approach |
| Uses `TextField` for password entry | Critical | Use `PasswordField` |
| Password logged to console/file | Critical | Remove logging, use debug-level only |
| Password stored in plaintext in memory longer than needed | Major | Clear password char array after use |
| Password stored in plaintext in file/database | Critical | Use hashing (bcrypt, Argon2) |

### 4.2 Sensitive Data in Logs

**Check**: Scan log statements (`System.out`, `logger.info`, etc.) for:
- Password fields
- API keys / tokens
- Personal data (email, phone, SSN)
- Database connection strings with credentials

| Condition | Severity | Fix |
|-----------|----------|-----|
| No sensitive data in logs | Pass | Correct |
| Sensitive data logged at DEBUG/TRACE level | Minor | Acceptable for development, ensure disabled in production |
| Sensitive data logged at INFO/WARN/ERROR level | Critical | Remove or mask sensitive data in logs |

### 4.3 In-Memory Data Protection

**Check**: How is sensitive data stored in memory?

| Condition | Severity | Fix |
|-----------|----------|-----|
| Passwords stored as `char[]` and cleared after use | Pass | Best practice |
| Passwords stored as `String` (immutable, cannot clear) | Minor | Use `char[]` for passwords |
| API keys stored as static final constants | Minor | Acceptable, but consider environment variables |
| Sensitive data in static fields (never garbage collected) | Major | Avoid static storage for sensitive data |

## 5. Threat Model Cross-Reference (STRIDE Traceability)

> **Conditional section**: Only executed if `architecture/architecture-handoff.json` exists and contains a `threat_model` section. If no threat model is present, this section is skipped and the tester proceeds with the standard security checks (§1-§4).

### 5.1 Consuming the Threat Model

When the architect has performed STRIDE threat modeling, the tester consumes the `threat_model.traceability_matrix` from `architecture-handoff.json` to execute threat-specific security tests. This ensures that every identified threat has corresponding test coverage.

**Read the threat model**:
```json
// From architecture-handoff.json
{
  "threat_model": {
    "traceability_matrix": [
      {
        "threat_id": "TM-001",
        "test_case_id": "SEC-TM-001",
        "test_description": "Fuzz update manifest with forged JSON",
        "test_type": "fuzz",
        "coverage_status": "covered"
      }
    ]
  }
}
```

### 5.2 Executing Threat-Specific Tests

For each entry in the `traceability_matrix` with `coverage_status: "covered"` or `"partially_covered"`, execute the corresponding security test:

| `test_type` | Execution Method | Example |
|-------------|-----------------|---------|
| `fuzz` | Inject crafted inputs at the identified attack surface | Fuzz the update manifest URL with forged JSON payloads |
| `static` | Scan source code or configuration files for the vulnerability | Scan preference files for plaintext credentials |
| `dynamic` | Launch the app and attempt the attack at runtime | Attempt to load FXML with `<fx:script>` tag |
| `dependency` | Run OWASP Dependency-Check for the identified component | Scan dependencies for known CVEs in the update library |

### 5.3 Test Result Mapping

Each threat-specific test result is recorded with a reference to the original threat ID:

```json
{
  "threat_test_results": [
    {
      "threat_id": "TM-001",
      "test_case_id": "SEC-TM-001",
      "test_type": "fuzz",
      "result": "pass",
      "detail": "App rejected forged manifest — HTTPS certificate validation prevented MITM",
      "severity": "Pass"
    },
    {
      "threat_id": "TM-I-01",
      "test_case_id": "SEC-TM-002",
      "test_type": "static",
      "result": "fail",
      "detail": "Password found in plaintext in preferences.xml — encryption not applied",
      "severity": "Critical"
    }
  ]
}
```

### 5.4 Coverage Gap Reporting

After executing all threat-specific tests, the tester reports:

1. **Covered threats**: Number of threats that had tests executed and passed
2. **Failed threat tests**: Threats where the test was executed but the vulnerability was confirmed (test failed → mitigation not effective)
3. **Untested threats**: Threats with `coverage_status: "not_covered"` in the traceability matrix — these are flagged as warnings
4. **Coverage percentage**: `covered_threats / total_threats × 100%`

### 5.5 Integration with Standard Security Checks

The threat model cross-reference (§5) runs **in addition to** the standard security checks (§1-§4), not as a replacement:

- **§1 Dependency Scan**: Always runs (catches CVEs regardless of threat model)
- **§2 Input Fuzzing**: Always runs (catches injection vulnerabilities in all input fields)
- **§3 WebView Security**: Always runs if WebView is present
- **§4 Sensitive Data Exposure**: Always runs (catches plaintext credentials regardless of threat model)
- **§5 Threat Model Cross-Reference**: Runs only if threat model exists (validates architect-identified threats specifically)

The threat model cross-reference results are merged into the `tester_sec_result` field alongside the standard security check results.
