# Security Compliance Checklist

This document is the criteria for security rules within the "Deep Compliance Audit" dimension, governing 4 check items: SQL injection prevention, path traversal prevention, hardcoded secrets detection, and WebView security. Default severity baseline: Major (security vulnerabilities). Shares the same origin as `javafx-developer`'s security rules.

> **Security First Principle**: Security issues must never be de-escalated to Minor. SQL injection and path traversal can lead to data breaches or system damage, hardcoded secrets can lead to credential leaks, and insecure WebView configuration can lead to arbitrary code execution.

---

## Check Item 1: SQL Injection Prevention

**Focus**: Whether SQL uses prepared statements (PreparedStatement) to prevent injection, without concatenating SQL strings.

**Pass Criteria**:
- All SQL queries use `PreparedStatement` + parameterized queries (`?` placeholders)
- When using MyBatis, use `#{param}` instead of `${param}` (the latter is string concatenation, with injection risk)
- When using JPA / Hibernate, use named parameters or positional parameters, without concatenating JPQL / HQL
- User input is not directly concatenated into SQL statements

**Fail Criteria** (any one constitutes failure):
- Using `Statement` + string concatenation for SQL (e.g., `"SELECT * FROM users WHERE name = '" + input + "'"`)
- MyBatis XML using `${param}` to receive user input (string concatenation, SQL injection)
- JPA using string concatenation for JPQL (`"FROM User WHERE name = '" + input + "'"`)
- Dynamic SQL for sort fields, table names, etc. without whitelist validation

**Severity Baseline**: Critical (SQL injection leads to data breach / tampering / deletion)

**Bad Example**:
```java
// Statement + string concatenation, SQL injection
Statement stmt = conn.createStatement();
String sql = "SELECT * FROM users WHERE name = '" + userName + "'";
ResultSet rs = stmt.executeQuery(sql);  // If userName = "'; DROP TABLE users; --" then disaster
```
```xml
<!-- MyBatis using ${} to receive user input, SQL injection -->
<select id="findByName" resultType="User">
    SELECT * FROM users WHERE name = '${name}'
</select>
```

**Good Example**:
```java
// PreparedStatement parameterized query
PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE name = ?");
ps.setString(1, userName);  // Parameterized, automatic escaping
ResultSet rs = ps.executeQuery();
```
```xml
<!-- MyBatis using #{} parameterization -->
<select id="findByName" resultType="User">
    SELECT * FROM users WHERE name = #{name}
</select>
```

---

## Check Item 2: Path Traversal Prevention

**Focus**: Whether file paths use `normalize()` to prevent traversal, without directly concatenating user-input paths.

**Pass Criteria**:
- File operations use `Paths.get()` + `Path.normalize()` to handle paths
- User-input file names / paths are validated, rejecting paths containing `../` or `..\`
- After using `Path.toAbsolutePath()`, verify that the path is within the allowed root directory
- File download / upload scenarios perform whitelist validation or random renaming of file names

**Fail Criteria** (any one constitutes failure):
- Directly concatenating user-input paths to filesystem paths (e.g., `new File("uploads/" + fileName)`)
- No validation for `../` path traversal
- No `normalize()` to canonicalize paths
- Allowing absolute path input (e.g., user inputs `/etc/passwd`)

**Severity Baseline**: Critical (path traversal leads to arbitrary file read / write)

**Bad Example**:
```java
// Directly concatenating user-input file name, path traversal
String fileName = request.getParameter("file");
File file = new File("uploads/" + fileName);  // fileName = "../../etc/passwd" -> path traversal
FileInputStream fis = new FileInputStream(file);
```

**Good Example**:
```java
// normalize() + path validation
String fileName = request.getParameter("file");
Path basePath = Paths.get("uploads/").toAbsolutePath().normalize();
Path targetPath = basePath.resolve(fileName).normalize();

// Verify that the resolved path is still within the allowed root directory
if (!targetPath.startsWith(basePath)) {
    throw new SecurityException("Illegal path access");
}
FileInputStream fis = new FileInputStream(targetPath.toFile());
```

---

## Check Item 3: Hardcoded Secrets Detection

**Focus**: Whether there are no hardcoded secrets (database passwords, API keys, encryption keys, etc.), using config files or environment variables.

**Pass Criteria**:
- Sensitive information such as database passwords, API keys, encryption keys, JWT secrets is not hardcoded in source code
- Sensitive information is injected via config files (`application.yml` / `application.properties`) or environment variables
- Sensitive information in config files uses placeholders (e.g., `${DB_PASSWORD}`) to reference environment variables
- Key management uses professional solutions (e.g., Spring Cloud Config, Vault)

**Fail Criteria** (any one constitutes failure):
- Hardcoded database password in source code (e.g., `DriverManager.getConnection("jdbc:...", "admin", "password123")`)
- Hardcoded API key / secret in source code
- Hardcoded encryption key in source code (e.g., `AES` key directly written in code)
- Plaintext password stored in config file without using placeholders

**Severity Baseline**: Critical (secret leak leads to system compromise)

**Bad Example**:
```java
// Hardcoded database password
Connection conn = DriverManager.getConnection(
    "jdbc:mysql://localhost:3306/mydb",
    "admin",
    "P@ssw0rd123"  // Hardcoded password
);

// Hardcoded API key
private static final String API_KEY = "sk-1234567890abcdef";  // Hardcoded
```

**Good Example**:
```java
// Read from environment variables / config
@Value("${spring.datasource.password}")
private String dbPassword;

Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
```
```yaml
# application.yml using environment variable placeholders
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/mydb}
    username: ${DB_USER:admin}
    password: ${DB_PASSWORD}  # Read from environment variable, no default value provided
```

---

## Check Item 4: WebView Security

**Focus**: Whether WebView restricts JavaScript, whether content sources are restricted, whether unnecessary features are disabled.

**Pass Criteria**:
- `WebView`'s `WebEngine` disables JavaScript when loading untrusted content (`setJavaScriptEnabled(false)`)
- Or only loads trusted HTTPS content, restricted to known domains
- Disables `WebEngine` local file access (`setAllowFileAccess(false)`, JavaFX 24+)
- Does not load user-input HTML via `WebEngine.loadContent()` (XSS risk)
- `WebView` popups are controlled or disabled via `setCreatePopupHandler`

**Fail Criteria** (any one constitutes failure):
- `WebView` loads user-input or untrusted URLs without disabling JavaScript
- Loading user-input HTML strings via `loadContent()` (XSS)
- `WebView` accessible content sources are not restricted
- `WebView` allows access to the local filesystem without validation

**Severity Baseline**: Major (insecure WebView configuration may lead to XSS or arbitrary code execution)
- Escalation condition: Loading completely untrusted external content with JavaScript enabled → Critical

**Bad Example**:
```java
// WebView loading user-input URL with JavaScript enabled
WebView webView = new WebView();
WebEngine engine = webView.getEngine();
engine.setJavaScriptEnabled(true);  // JS enabled
String userInput = urlField.getText();
engine.load(userInput);  // Loading user-input URL, may access malicious pages

// loadContent loading user-input HTML (XSS)
String html = userInput;  // User-input HTML
engine.loadContent(html);  // May execute malicious JS
```

**Good Example**:
```java
// Restrict JavaScript + only load trusted HTTPS
WebView webView = new WebView();
WebEngine engine = webView.getEngine();
engine.setJavaScriptEnabled(false);  // Disable JS (for untrusted content)

// Only allow loading trusted domains
String url = urlField.getText().trim();
if (isTrustedDomain(url)) {  // Whitelist validation
    engine.load(url);
} else {
    showError("Access to this address is not allowed");
}

// Safe content loading (trusted HTML only)
engine.loadContent(sanitizedHtml);  // HTML after escaping
```
