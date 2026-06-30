# STRIDE Threat Modeling for JavaFX Desktop Applications

This document defines the methodology for conducting threat modeling during the architecture design phase. It uses the STRIDE framework adapted for JavaFX desktop applications, producing a structured threat model that flows into `architecture-handoff.json` and is cross-referenced by `javafx-tester`'s Security Testing dimension.

## 1. STRIDE Framework Overview

STRIDE is a Microsoft threat classification framework that categorizes threats into six types. For JavaFX desktop applications, each category is adapted to the desktop context (local file system, embedded database, network APIs, WebView, auto-update channels).

| STRIDE Category | Property Violated | JavaFX Desktop Example |
|-----------------|-------------------|------------------------|
| **S**poofing | Authentication | Forged update manifest served by MITM; fake database file replaces local SQLite |
| **T**ampering | Integrity | Modified FXML loaded from external path; tampered preference file changes app behavior |
| **R**epudiation | Non-repudiation | User denies performing a destructive action because no audit log was kept |
| **I**nformation Disclosure | Confidentiality | Password stored in plaintext in preferences; crash log contains sensitive data |
| **D**enial of Service | Availability | Malformed input causes infinite loop; large file causes OutOfMemoryError |
| **E**levation of Privilege | Authorization | Unprivileged user modifies config file to enable admin features |

## 2. Attack Surface Identification

Before identifying threats, enumerate the application's attack surface — all entry points where untrusted data enters or crosses trust boundaries.

### 2.1 JavaFX Desktop Attack Surface Inventory

| Attack Surface | Trust Boundary | Untrusted Input Source |
|----------------|----------------|----------------------|
| User input fields | UI → Controller | User-typed text in TextField, TextArea, ComboBox |
| File import/export | OS → App | Files selected via FileChooser (any format, any size) |
| Local database | App → Storage | SQL queries against SQLite/H2 (injection via crafted data) |
| Network API calls | App → Remote | HTTP responses from REST APIs (JSON/XML payloads) |
| WebView content | Remote → UI | Web pages loaded in WebEngine (XSS, malicious JS) |
| Auto-update manifest | Remote → App | JSON manifest from update server (forged version, malicious URL) |
| Preferences/config | Storage → App | `.properties` / `.json` files in user home (tampered on disk) |
| FXML loading | Resource → UI | FXML files loaded at runtime (injection via `fx:include` or script) |
| Command-line args | OS → App | `Application.getParameters()` (scripted launch with crafted args) |
| Drag-and-drop | OS → UI | Dropped files of unknown type and origin |
| Clipboard | OS → UI | Pasted content from arbitrary source |
| Serialization | Storage → App | Deserialized Java objects (gadget chains if ObjectInputStream) |

### 2.2 Data Flow Diagram (DFD)

Generate a PlantUML DFD showing trust boundaries and data flows. Use the following syntax:

```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-Model/master/C4_Context.puml

' Trust boundaries as rectangles
rectangle "User Trust Boundary" {
  actor "End User" as User
}

rectangle "Application Trust Boundary" {
  rectangle "JavaFX UI Layer" as UI {
    component "FXML Controllers" as Ctrl
    component "WebView" as WV
  }
  rectangle "Business Logic" as Logic {
    component "Services" as Svc
    component "Input Validator" as Val
  }
  rectangle "Data Layer" as Data {
    component "Repository" as Repo
    component "Preferences" as Prefs
  }
}

rectangle "External Trust Boundary" {
  database "Local SQLite" as DB
  cloud "REST API" as API
  cloud "Update Server" as Upd
}

User --> Ctrl : user input
User --> WV : web interaction
Ctrl --> Val : validate
Val --> Svc : sanitized input
Svc --> Repo : query
Repo --> DB : JDBC
Svc --> API : HTTPS
UI --> Upd : check for updates
@enduml
```

Save as `architecture/uml/threat-model-dfd.puml`.

## 3. Threat Identification Worksheet

For each attack surface entry, apply STRIDE to identify potential threats. Use the following structured worksheet format:

### 3.1 Threat Entry Format

Each identified threat is recorded as a structured entry:

```json
{
  "threat_id": "TM-001",
  "stride_category": "Tampering",
  "attack_surface": "Auto-update manifest",
  "description": "Attacker serves a forged update manifest via DNS hijacking or MITM, directing the app to download a malicious installer",
  "affected_component": "UpdateChecker.checkForUpdate()",
  "risk_rating": "High",
  "likelihood": "Medium",
  "impact": "Critical",
  "mitigation": "Enforce HTTPS with certificate pinning; verify installer SHA-256 checksum against a hardcoded or signed expected hash; reject manifests without valid signature",
  "residual_risk": "Low — after HTTPS + checksum verification + signature validation"
}
```

### 3.2 Risk Rating Matrix

| Likelihood \ Impact | Low | Medium | High | Critical |
|---------------------|-----|--------|------|----------|
| Low | Low | Low | Medium | Medium |
| Medium | Low | Medium | High | High |
| High | Medium | High | High | Critical |

### 3.3 STRIDE Threat Catalog for JavaFX

The following catalog provides a baseline of common threats. The architect should select applicable threats and add project-specific ones.

#### Spoofing (S)

| ID | Threat | Mitigation |
|----|--------|------------|
| TM-S-01 | Forged update manifest via MITM | HTTPS + certificate pinning + installer signature verification |
| TM-S-02 | Fake database file replaces local SQLite | File permissions; database encryption (SQLCipher) |
| TM-S-03 | Spoofed REST API response via DNS hijack | HTTPS + TLS certificate validation; API key authentication |
| TM-S-04 | Clipboard paste impersonates trusted data | Validate all pasted content regardless of source |

#### Tampering (T)

| ID | Threat | Mitigation |
|----|--------|------------|
| TM-T-01 | Tampered preferences file changes app behavior | Sign preference file; validate schema on load; fail-safe defaults |
| TM-T-02 | Modified FXML loaded from external path | Load FXML from classpath only; reject external paths |
| TM-T-03 | Malicious command-line arguments | Whitelist accepted parameters; reject unknown args |
| TM-T-04 | Tampered JAR dependency (supply chain) | Verify dependency checksums; use locked dependency versions |

#### Repudiation (R)

| ID | Threat | Mitigation |
|----|--------|------------|
| TM-R-01 | User denies destructive action (no audit log) | Write audit log for delete/overwrite/export operations |
| TM-R-02 | User denies changing critical settings | Log setting changes with timestamp and old/new values |
| TM-R-03 | Admin denies disabling security feature | Log security-relevant configuration changes |

#### Information Disclosure (I)

| ID | Threat | Mitigation |
|----|--------|------------|
| TM-I-01 | Password stored in plaintext in preferences | Use SecurePreferences or OS keychain (java.security.KeyStore) |
| TM-I-02 | Crash log contains sensitive user data | Sanitize crash reports; redact credentials/tokens before writing |
| TM-I-03 | Database file readable by other users | File system permissions; database encryption |
| TM-I-04 | API key hardcoded in source code | Externalize to environment variables or config outside JAR |
| TM-I-05 | WebView localStorage leaks sensitive data | Clear WebView storage on logout; disable persistent storage |

#### Denial of Service (D)

| ID | Threat | Mitigation |
|----|--------|------------|
| TM-D-01 | Malformed input causes infinite loop | Input validation with timeout; reject inputs exceeding max length |
| TM-D-02 | Large file import causes OutOfMemoryError | Stream processing; file size limits; reject files > N MB |
| TM-D-03 | Rapid UI events overwhelm event handler | Debounce/throttle event processing; cancel pending work on new event |
| TM-D-04 | Malicious API response causes infinite retry | Circuit breaker; max retry limit; exponential backoff with cap |

#### Elevation of Privilege (E)

| ID | Threat | Mitigation |
|----|--------|------------|
| TM-E-01 | Config file modification enables admin features | Runtime privilege check; do not trust config for authorization |
| TM-E-02 | Deserialization gadget chain executes code | Avoid ObjectInputStream; use JSON with type whitelisting |
| TM-E-03 | FXML script injection executes arbitrary code | Disable FXML `<fx:script>`; load FXML from trusted classpath only |
| TM-E-04 | ServiceLoader exploitation loads malicious implementation | Seal packages in module-info.java; restrict ServiceLoader providers |

## 4. Mitigation Design

### 4.1 Mitigation Priority

Mitigations are prioritized by risk rating:

| Risk Rating | Mitigation Deadline | Architecture Action |
|-------------|--------------------|--------------------|
| Critical | Before implementation | Must be addressed in architecture design; ADR required |
| High | Before implementation | Must be addressed in architecture design; ADR recommended |
| Medium | During implementation | Documented as developer instruction; reviewed by code-reviewer |
| Low | Best effort | Recorded for future improvement; monitored by tester |

### 4.2 Mitigation Categories

1. **Preventive** — Stop the threat from occurring (input validation, authentication, authorization)
2. **Detective** — Detect the threat when it occurs (audit logging, anomaly detection)
3. **Corrective** — Respond after detection (auto-rollback, graceful degradation, user alert)
4. **Compensating** — Alternative control when primary mitigation is not feasible (file permissions when encryption is not available)

### 4.3 Security ADRs

For Critical and High risk threats, create a Security ADR documenting the mitigation decision:

```markdown
# ADR-SEC-001: Enforce HTTPS with Certificate Pinning for Update Server

## Status
Accepted (2026-06-30)

## Context
Threat TM-001 identifies that the auto-update manifest can be forged via MITM attack.
The update checker currently fetches the manifest over plain HTTPS without certificate
pinning, allowing an attacker with a valid CA certificate to intercept and modify
the manifest.

## Decision
We will implement certificate pinning for the update server connection. The app will
ship with the expected server certificate hash and reject connections that do not match.

## Consequences
**Positive:**
- Prevents MITM attacks on the update channel
- Ensures only genuine update manifests are accepted

**Negative:**
- Certificate rotation requires app update (pinned hash must be updated)
- Adds complexity to the update infrastructure

## Threats Addressed
- TM-S-01 (Spoofing: Forged update manifest)
- TM-T-01 (Tampering: Modified update URL)
```

## 5. Threat-to-Test Traceability Matrix

Each threat must map to at least one security test case in `javafx-tester`. The architect produces a traceability matrix that the tester consumes.

### 5.1 Matrix Format

```json
{
  "traceability_matrix": [
    {
      "threat_id": "TM-001",
      "threat_summary": "Forged update manifest via MITM",
      "test_case_id": "SEC-TM-001",
      "test_description": "Fuzz update manifest URL with forged JSON; verify app rejects unsigned installers",
      "test_type": "fuzz",
      "coverage_status": "covered"
    },
    {
      "threat_id": "TM-I-01",
      "threat_summary": "Password stored in plaintext in preferences",
      "test_case_id": "SEC-TM-002",
      "test_description": "Scan preference files for plaintext credentials; verify encryption",
      "test_type": "static",
      "coverage_status": "covered"
    },
    {
      "threat_id": "TM-E-03",
      "threat_summary": "FXML script injection executes arbitrary code",
      "test_case_id": "SEC-TM-003",
      "test_description": "Attempt to load FXML with <fx:script> tag; verify execution is rejected",
      "test_type": "dynamic",
      "coverage_status": "covered"
    }
  ]
}
```

### 5.2 Coverage Status Values

| Status | Meaning |
|--------|---------|
| `covered` | Threat has a corresponding test case that validates the mitigation |
| `partially_covered` | Test covers some but not all aspects of the threat |
| `not_covered` | No test case exists; threat is documented but untested (must be flagged as warning) |
| `not_applicable` | Threat does not apply to this project (e.g., no WebView → WebView threats are N/A) |

## 6. Threat Model Handoff Protocol

The threat model is included in `architecture-handoff.json` under the `threat_model` key:

```json
{
  "threat_model": {
    "methodology": "STRIDE",
    "dfd_diagram": "architecture/uml/threat-model-dfd.puml",
    "attack_surfaces": [
      {
        "name": "User input fields",
        "trust_boundary": "UI → Controller",
        "untrusted_source": "User-typed text"
      },
      {
        "name": "Auto-update manifest",
        "trust_boundary": "Remote → App",
        "untrusted_source": "JSON manifest from update server"
      }
    ],
    "threats": [
      {
        "threat_id": "TM-001",
        "stride_category": "Spoofing",
        "attack_surface": "Auto-update manifest",
        "description": "Attacker serves forged update manifest via MITM",
        "affected_component": "UpdateChecker.checkForUpdate()",
        "risk_rating": "High",
        "likelihood": "Medium",
        "impact": "Critical",
        "mitigation": "HTTPS + certificate pinning + SHA-256 checksum verification",
        "residual_risk": "Low"
      }
    ],
    "security_adrs": [
      "architecture/adr/ADR-SEC-001-https-certificate-pinning.md"
    ],
    "traceability_matrix": [
      {
        "threat_id": "TM-001",
        "test_case_id": "SEC-TM-001",
        "test_description": "Fuzz update manifest with forged JSON",
        "coverage_status": "covered"
      }
    ],
    "uncovered_threats": [],
    "summary": {
      "total_threats": 12,
      "critical": 1,
      "high": 3,
      "medium": 5,
      "low": 3,
      "covered": 10,
      "partially_covered": 1,
      "not_covered": 1,
      "not_applicable": 0
    }
  }
}
```

## 7. Conditional Execution

Threat modeling is **conditionally executed** based on the following rules:

| Condition | Action |
|-----------|--------|
| User explicitly requests threat modeling / security analysis | Execute full STRIDE analysis |
| `.loop-config.json` has `"threat_modeling": true` | Execute full STRIDE analysis |
| Project has network communication, database, or WebView | Execute (auto-detected from system design) |
| Project is a standalone offline app with no network, no database, no file I/O | Skip (minimal attack surface) |
| User explicitly says "skip threat modeling" | Skip, record in report |

When skipped, `threat_model` is absent from the handoff JSON, and the architect report notes "Threat modeling skipped: minimal attack surface" or "Threat modeling skipped per user request".

## 8. Threat Modeling Checklist

Before finalizing the threat model, verify:

- [ ] Attack surface inventory is complete (all entry points enumerated)
- [ ] DFD diagram generated with trust boundaries clearly marked
- [ ] STRIDE applied to each attack surface entry
- [ ] Each threat has a unique ID (TM-XXX format)
- [ ] Each threat has risk rating (likelihood × impact)
- [ ] Critical and High threats have mitigations designed
- [ ] Critical and High threats have Security ADRs
- [ ] Each threat maps to at least one test case in the traceability matrix
- [ ] Uncovered threats are listed in `uncovered_threats[]`
- [ ] Summary statistics are accurate (total, by severity, by coverage)
- [ ] `threat_model` section is included in `architecture-handoff.json`
