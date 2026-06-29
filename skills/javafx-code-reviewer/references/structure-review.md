# 代码结构审核规范

本文档是"代码结构合理性"维度的判定依据，管辖 5 个检查项（对应设计书 3.1 节）。审核 JavaFX 代码的架构分层、职责划分、包结构、模块化配置及依赖方向。默认严重性基线：Major。与 `javafx-developer` 的 `architecture-patterns.md` 同源。

---

## 检查项 1：架构模式合规性

**关注点**：MVC / MVVM / MVP 分层是否清晰，View 层是否混入业务逻辑，Controller 是否仅处理 UI 事件。

**通过判定标准**：
- 采用了明确的架构模式（MVC / MVVM / MVP），各层职责划分清晰
- Controller 仅处理 UI 事件和视图状态编排，不包含业务规则、数据访问、校验逻辑
- View（FXML）为纯声明式，不含业务逻辑或脚本
- 业务逻辑委托给 Service 层，数据访问委托给 Repository / DAO 层

**不通过判定标准**（任一即不通过）：
- Controller 中直接包含数据库访问（JDBC / JPA / MyBatis 调用）
- Controller 中包含复杂业务规则计算（应在 Service 层）
- FXML 中通过 `<fx:script>` 嵌入业务逻辑
- View 层直接操作 Model 的持久化方法

**严重性基线**：Major
- 降级条件：仅个别方法越层，不影响整体架构 → Minor
- 升级条件：导致无法独立测试或多处循环依赖 → Critical

**反例**：
```java
// ❌ Controller 直接操作数据库，绕过 Service 层
@FXML
private void handleLoad() {
    try (Connection conn = DriverManager.getConnection("jdbc:sqlite:app.db")) {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM users");
        while (rs.next()) {
            users.add(new User(rs.getString("name")));
        }
    } catch (SQLException e) { e.printStackTrace(); }
}
```

**正例**：
```java
// ✅ Controller 仅委托给 Service
@FXML
private void handleLoad() {
    List<User> loaded = userService.loadAll();  // 委托 Service 层
    users.setAll(loaded);
}
```

---

## 检查项 2：职责单一性

**关注点**：Controller 是否承担过多职责（上帝类），Service 层是否被正确委托。

**通过判定标准**：
- 每个 Controller 职责单一，对应一个视图或一组紧密相关的视图
- Controller 行数适中（建议 < 400 行），无上帝类（God Controller）
- Service 层被正确委托，承载业务逻辑与事务边界
- 各类符合单一职责原则，一个类只有一个变化的理由

**不通过判定标准**（任一即不通过）：
- 单个 Controller 管理多个不相关功能模块（如同时管理用户、订单、设置）
- Controller 行数过大（> 500 行）且职责混杂
- 缺少 Service 层，Controller 直接访问 Repository / DAO
- 存在 God Controller（管理所有功能的全能类）

**严重性基线**：Major
- 降级条件：仅个别方法越层，不影响整体架构 → Minor

**反例**：
```java
// ❌ God Controller：单个类管理所有功能
public class MainController {
    @FXML private void handleUser() { /* 用户管理 */ }
    @FXML private void handleOrder() { /* 订单管理 */ }
    @FXML private void handleSettings() { /* 系统设置 */ }
    @FXML private void handleReport() { /* 报表生成 */ }
    // ... 超过 1000 行
}
```

**正例**：
```java
// ✅ 按功能拆分为多个 Controller，各自委托对应 Service
public class UserController { /* 仅用户管理 */ }
public class OrderController { /* 仅订单管理 */ }
```

---

## 检查项 3：包结构规范

**关注点**：`model / view / controller / viewmodel / service` 分层是否一致，包路径与目录结构是否匹配。

**通过判定标准**：
- 包结构按职责分层，如 `com.example.app.model`、`com.example.app.view`、`com.example.app.controller`、`com.example.app.service`
- 包路径与物理目录结构一一对应
- 同类文件归入同一包（所有 Controller 在 controller 包，所有 Model 在 model 包）
- 包命名使用全小写，无下划线或特殊字符

**不通过判定标准**（任一即不通过）：
- 包结构混乱（Controller 与 Model 放在同一包）
- 包路径与目录结构不匹配
- 缺少分层，所有类堆在默认包或单一包中
- 包命名不规范（含大写字母、下划线）

**严重性基线**：Major
- 降级条件：仅个别文件放错包，整体结构清晰 → Minor

**正例**：
```
src/main/java/com/example/app/
├── model/          # 数据模型
│   └── User.java
├── view/           # FXML 视图
│   └── user-view.fxml
├── controller/     # 控制器
│   └── UserController.java
├── viewmodel/      # ViewModel（MVVM 模式）
│   └── UserViewModel.java
└── service/        # 业务服务
    └── UserService.java
```

---

## 检查项 4：模块化配置

**关注点**：`module-info.java` 的 `requires` / `exports` / `opens` 是否完整正确。

**通过判定标准**：
- `module-info.java` 声明了所有必需的 `requires`（javafx.controls、javafx.fxml 等）
- `exports` 正确导出公共 API 包
- 需要反射访问的包通过 `opens` 暴露给对应模块
- 特别地：使用 `PropertyValueFactory` 反射访问 Model 属性时，须 `opens model to javafx.controls`

**不通过判定标准**（任一即不通过）：
- 缺少 `requires javafx.fxml`，导致 FXMLLoader 无法工作
- 使用 `PropertyValueFactory` 但未 `opens` model 包给 `javafx.controls`，运行时反射失败
- `exports` 过度暴露内部实现包（如导出 controller 包）
- 缺少 `module-info.java` 但项目使用模块化构建

**严重性基线**：Major

**反例**：
```java
// ❌ 缺少 opens，PropertyValueFactory 反射将失败
module com.example.app {
    requires javafx.controls;
    requires javafx.fxml;
    exports com.example.app.model;  // 仅 exports 不够，反射需 opens
}
```

**正例**：
```java
// ✅ 正确 opens model 包给 javafx.controls 支持反射
module com.example.app {
    requires javafx.controls;
    requires javafx.fxml;
    exports com.example.app.model;
    opens com.example.app.model to javafx.controls;  // PropertyValueFactory 反射需要
    opens com.example.app.controller to javafx.fxml; // FXMLLoader 反射需要
}
```

---

## 检查项 5：依赖方向

**关注点**：是否存在循环依赖，View 层是否反向依赖 Controller 实现细节。

**通过判定标准**：
- 依赖方向单向：View → Controller → Service → Repository → Model
- 不存在循环依赖（A 依赖 B，B 依赖 A）
- Controller 依赖 Service 接口而非具体实现（依赖倒置）
- View 层（FXML）不直接引用 Controller 的内部方法，仅通过 `fx:controller` 和 `onAction` 绑定

**不通过判定标准**（任一即不通过）：
- 存在循环依赖（Controller A 依赖 Controller B，B 又依赖 A）
- Controller 直接 `new` 具体依赖类，无法替换和测试
- View 层反向依赖 Controller 的实现细节
- Service 层反向依赖 Controller（业务层不应知道 UI 层）

**严重性基线**：Major
- 降级条件：仅个别方法越层，不影响整体架构 → Minor
- 升级条件：多处循环依赖导致无法独立测试 → Critical

**反例**：
```java
// ❌ 紧耦合：Controller 直接 new 具体实现，且存在循环依赖
public class OrderController {
    private MySQLDatabase db = new MySQLDatabase();  // 硬编码依赖
    private ReportController reportCtrl;             // Controller 间循环依赖
}
```

**正例**：
```java
// ✅ 依赖倒置：通过接口 + 构造注入
public class OrderController {
    private final Database db;           // 依赖接口
    private final OrderService service;  // 依赖 Service 接口

    public OrderController(Database db, OrderService service) {
        this.db = db;
        this.service = service;
    }
}
```
