# 安全合规清单

本文档是"深度合规审核"维度中安全规则的判定依据，管辖 4 个检查项：SQL 注入防护、路径遍历防护、硬编码密钥排查、WebView 安全。默认严重性基线：Major（安全漏洞）。与 `javafx-developer` 的安全规则同源。

> **安全第一原则**：安全类问题一律不得降级为 Minor。SQL 注入和路径遍历可能导致数据泄露或系统破坏，硬编码密钥可能导致凭据泄露，WebView 不安全配置可能导致任意代码执行。

---

## 检查项 1：SQL 注入防护

**关注点**：SQL 是否使用预编译（PreparedStatement）防注入，不拼接 SQL 字符串。

**通过判定标准**：
- 所有 SQL 查询使用 `PreparedStatement` + 参数化查询（`?` 占位符）
- 使用 MyBatis 时使用 `#{param}` 而非 `${param}`（后者是字符串拼接，有注入风险）
- 使用 JPA / Hibernate 时使用命名参数或位置参数，不拼接 JPQL / HQL
- 用户输入不直接拼接到 SQL 语句中

**不通过判定标准**（任一即不通过）：
- 使用 `Statement` + 字符串拼接 SQL（如 `"SELECT * FROM users WHERE name = '" + input + "'"`）
- MyBatis XML 中使用 `${param}` 接收用户输入（字符串拼接，SQL 注入）
- JPA 中使用字符串拼接 JPQL（`"FROM User WHERE name = '" + input + "'"`）
- 排序字段、表名等动态 SQL 未做白名单校验

**严重性基线**：Critical（SQL 注入导致数据泄露 / 篡改 / 删除）

**反例**：
```java
// ❌ Statement + 字符串拼接，SQL 注入
Statement stmt = conn.createStatement();
String sql = "SELECT * FROM users WHERE name = '" + userName + "'";
ResultSet rs = stmt.executeQuery(sql);  // 若 userName = "'; DROP TABLE users; --" 则灾难
```
```xml
<!-- ❌ MyBatis 使用 ${} 接收用户输入，SQL 注入 -->
<select id="findByName" resultType="User">
    SELECT * FROM users WHERE name = '${name}'
</select>
```

**正例**：
```java
// ✅ PreparedStatement 参数化查询
PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE name = ?");
ps.setString(1, userName);  // 参数化，自动转义
ResultSet rs = ps.executeQuery();
```
```xml
<!-- ✅ MyBatis 使用 #{} 参数化 -->
<select id="findByName" resultType="User">
    SELECT * FROM users WHERE name = #{name}
</select>
```

---

## 检查项 2：路径遍历防护

**关注点**：文件路径是否 `normalize()` 防遍历，不直接拼接用户输入的路径。

**通过判定标准**：
- 文件操作使用 `Paths.get()` + `Path.normalize()` 处理路径
- 对用户输入的文件名 / 路径进行校验，拒绝包含 `../` 或 `..\` 的路径
- 使用 `Path.toAbsolutePath()` 后校验是否在允许的根目录内
- 文件下载 / 上传场景对文件名做白名单校验或随机重命名

**不通过判定标准**（任一即不通过）：
- 直接拼接用户输入的路径到文件系统路径（如 `new File("uploads/" + fileName)`）
- 未对 `../` 路径遍历做校验
- 未 `normalize()` 规范化路径
- 允许绝对路径输入（如用户输入 `/etc/passwd`）

**严重性基线**：Critical（路径遍历导致任意文件读取 / 写入）

**反例**：
```java
// ❌ 直接拼接用户输入的文件名，路径遍历
String fileName = request.getParameter("file");
File file = new File("uploads/" + fileName);  // fileName = "../../etc/passwd" → 路径遍历
FileInputStream fis = new FileInputStream(file);
```

**正例**：
```java
// ✅ normalize() + 路径校验
String fileName = request.getParameter("file");
Path basePath = Paths.get("uploads/").toAbsolutePath().normalize();
Path targetPath = basePath.resolve(fileName).normalize();

// 校验解析后的路径仍在允许的根目录内
if (!targetPath.startsWith(basePath)) {
    throw new SecurityException("非法路径访问");
}
FileInputStream fis = new FileInputStream(targetPath.toFile());
```

---

## 检查项 3：硬编码密钥排查

**关注点**：是否无硬编码密钥（数据库密码、API Key、加密密钥等），使用配置文件或环境变量。

**通过判定标准**：
- 数据库密码、API Key、加密密钥、JWT Secret 等敏感信息不硬编码在源码中
- 敏感信息通过配置文件（`application.yml` / `application.properties`）或环境变量注入
- 配置文件中的敏感信息使用占位符（如 `${DB_PASSWORD}`）引用环境变量
- 密钥管理使用专业方案（如 Spring Cloud Config、Vault）

**不通过判定标准**（任一即不通过）：
- 源码中硬编码数据库密码（如 `DriverManager.getConnection("jdbc:...", "admin", "password123")`）
- 源码中硬编码 API Key / Secret
- 源码中硬编码加密密钥（如 `AES` 密钥直接写在代码中）
- 配置文件中明文存储密码且未使用占位符

**严重性基线**：Critical（密钥泄露导致系统被入侵）

**反例**：
```java
// ❌ 硬编码数据库密码
Connection conn = DriverManager.getConnection(
    "jdbc:mysql://localhost:3306/mydb",
    "admin",
    "P@ssw0rd123"  // 硬编码密码
);

// ❌ 硬编码 API Key
private static final String API_KEY = "sk-1234567890abcdef";  // 硬编码
```

**正例**：
```java
// ✅ 从环境变量 / 配置读取
@Value("${spring.datasource.password}")
private String dbPassword;

Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
```
```yaml
# ✅ application.yml 使用环境变量占位符
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/mydb}
    username: ${DB_USER:admin}
    password: ${DB_PASSWORD}  # 从环境变量读取，不提供默认值
```

---

## 检查项 4：WebView 安全

**关注点**：WebView 是否限制 JavaScript、是否限制内容来源、是否禁用不必要的功能。

**通过判定标准**：
- `WebView` 的 `WebEngine` 在加载不可信内容时禁用 JavaScript（`setJavaScriptEnabled(false)`）
- 或仅加载可信 HTTPS 内容，限制为已知域名
- 禁用 `WebEngine` 的本地文件访问（`setAllowFileAccess(false)`，JavaFX 24+）
- 不通过 `WebEngine.loadContent()` 加载用户输入的 HTML（XSS 风险）
- `WebView` 弹窗通过 `setCreatePopupHandler` 控制或禁用

**不通过判定标准**（任一即不通过）：
- `WebView` 加载用户输入或不可信 URL 且未禁用 JavaScript
- 通过 `loadContent()` 加载用户输入的 HTML 字符串（XSS）
- 未限制 `WebView` 可访问的内容来源
- `WebView` 允许访问本地文件系统且未做校验

**严重性基线**：Major（不安全 WebView 配置可能导致 XSS 或任意代码执行）
- 升级条件：加载完全不可信的外部内容且启用 JavaScript → Critical

**反例**：
```java
// ❌ WebView 加载用户输入 URL 且启用 JavaScript
WebView webView = new WebView();
WebEngine engine = webView.getEngine();
engine.setJavaScriptEnabled(true);  // 启用 JS
String userInput = urlField.getText();
engine.load(userInput);  // 加载用户输入的 URL，可能访问恶意页面

// ❌ loadContent 加载用户输入 HTML（XSS）
String html = userInput;  // 用户输入的 HTML
engine.loadContent(html);  // 可能执行恶意 JS
```

**正例**：
```java
// ✅ 限制 JavaScript + 仅加载可信 HTTPS
WebView webView = new WebView();
WebEngine engine = webView.getEngine();
engine.setJavaScriptEnabled(false);  // 禁用 JS（不可信内容时）

// 仅允许加载可信域名
String url = urlField.getText().trim();
if (isTrustedDomain(url)) {  // 白名单校验
    engine.load(url);
} else {
    showError("不允许访问该地址");
}

// ✅ 安全加载内容（仅可信 HTML）
engine.loadContent(sanitizedHtml);  // 经转义处理后的 HTML
```
