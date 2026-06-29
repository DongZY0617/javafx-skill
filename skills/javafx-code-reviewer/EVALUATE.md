# JavaFX Code Reviewer 评估用例集

本文件定义 `javafx-code-reviewer` 技能的验收用例，用于量化审核输出质量。每个用例描述输入场景、用例类型、覆盖维度、预期发现的问题及可勾选的验证标准。

- **正样本**：有问题的代码，验证审核器的召回率（应发现的问题是否全部发现）
- **负样本**：合规的代码，验证审核器的精确率（不应误报，零假阳性）
- **边界用例**：降级判定、增量评审等，验证审核器在边界条件下的判定准确性

---

## 用例总览

| 编号 | 名称 | 类型 | 覆盖维度 | 预期问题数 |
|------|------|------|---------|-----------|
| 1 | 后台线程更新 UI | 正样本 | UI 线程安全性 | 1 Critical |
| 2 | FXML fx:id 不匹配 | 正样本 | FXML 使用规范 | 1 Major |
| 3 | 监听器泄漏 | 正样本 | 内存泄漏风险 | 1 Critical |
| 4 | 批量更新低效 | 正样本 | 性能表现 | 1 Major |
| 5 | Spring Boot 陷阱 | 正样本 | 深度合规审核 | 1 Critical + 1 Major |
| 6 | CSS var() 误用 | 正样本 | 深度合规审核（CSS） | 1 Major |
| 7 | API 误用（不存在的 dispose） | 正样本 | 深度合规审核（API） | 1 Major |
| 8 | 综合项目审核 | 正样本 | 全维度 | 多问题 |
| 9 | 合规代码（负样本） | 负样本 | 全维度 | 0（零误报） |
| 10 | 监听器降级判定 | 边界 | 内存泄漏风险 | 1 Major（降级） |
| 11 | 增量评审（仅改 CSS） | 边界 | 指定维度 | 仅 CSS 问题 |
| 12 | 修复交接完整性 | 正样本 | 全维度 | 含交接字段 |
| 13 | Controller 越层访问数据层 | 正样本 | 代码结构合理性 | 1 Major |
| 14 | 静态引用持有 Stage | 正样本 | 内存泄漏风险 | 1 Critical（不可降级） |

---

## 用例详情

### 用例 1：后台线程更新 UI

- **输入**：包含后台线程直接更新 UI 组件的代码
  ```java
  new Thread(() -> {
      // 模拟耗时操作
      Thread.sleep(2000);
      statusLabel.setText("完成");   // 后台线程直接更新 UI
  }).start();
  ```
- **类型**：正样本
- **覆盖维度**：UI 线程安全性
- **预期发现**：1 个 Critical — FX 线程更新违规
- **验证标准**：
  - [ ] 准确指出 `setText` 在非 FX 线程调用，将抛出 `IllegalStateException: Not on FX application thread`
  - [ ] 给出 `Platform.runLater()` 或 `Task.updateMessage()` 修正方案
  - [ ] 严重性判定为 Critical（不可降级，运行时必然抛异常）
  - [ ] 规范依据引用 `thread-safety-rules.md — FX 线程更新规则`
  - [ ] 含修复交接字段（target_file / target_lines / fix_type / fix_priority）

---

### 用例 2：FXML fx:id 不匹配

- **输入**：FXML 中声明的 `fx:id` 与 Controller 中的 `@FXML` 字段不一致
  ```xml
  <!-- user-view.fxml -->
  <Button fx:id="saveBtn" text="保存" onAction="#handleSave"/>
  <Label fx:id="nameLabel" text="姓名"/>
  ```
  ```java
  // UserController.java —— Controller 中只有 saveButton，无 saveBtn；多出未引用的 nameLabel
  @FXML private Button saveButton;  // 与 FXML 的 saveBtn 不匹配
  ```
- **类型**：正样本
- **覆盖维度**：FXML 使用规范
- **预期发现**：1 个 Major — fx:id 不匹配（字段缺失/多余）
- **验证标准**：
  - [ ] 逐项列出不匹配的 fx:id（`saveBtn` 在 Controller 中无对应字段）
  - [ ] 指出运行时将抛出 `LoadException`
  - [ ] 给出对齐方案（在 Controller 添加 `@FXML private Button saveBtn;` 或修改 FXML 的 fx:id）
  - [ ] 严重性判定为 Major（不可降级，运行时必然抛 LoadException）
  - [ ] 规范依据引用 `fxml-standards.md — fx:id 匹配规则`

---

### 用例 3：监听器泄漏

- **输入**：Controller 注册监听器但无清理方法
  ```java
  public class DetailController implements Initializable {
      @FXML private Label nameLabel;
      private final ObservableList<String> data = FXCollections.observableArrayList();

      @Override
      public void initialize(URL location, ResourceBundle resources) {
          // 注册监听器，但无任何 removeListener 或清理逻辑
          data.addListener((ListChangeListener<String>) c -> {
              nameLabel.setText("数据更新: " + c.getList().size());
          });
      }
      // 无 dispose / 无 setOnCloseRequest 清理
  }
  ```
- **类型**：正样本
- **覆盖维度**：内存泄漏风险
- **预期发现**：1 个 Critical — 监听器未移除致泄漏
- **验证标准**：
  - [ ] 指出缺少 `removeListener()` 调用
  - [ ] 说明视图切换后旧 Controller 无法被 GC，持续接收事件
  - [ ] 建议自定义 `dispose()` 方法并通过 `setOnCloseRequest` 或视图切换回调触发清理
  - [ ] 严重性判定为 Critical（默认基线）
  - [ ] 规范依据引用 `memory-management.md — 监听器移除规则`

---

### 用例 4：批量更新低效

- **输入**：循环 `add()` 逐条更新 ObservableList
  ```java
  // 从数据库加载 5000 条记录后逐条添加
  List<User> users = userService.loadAllUsers();
  for (User user : users) {
      userList.add(user);  // 循环 add，触发 N 次变更事件
  }
  ```
- **类型**：正样本
- **覆盖维度**：性能表现
- **预期发现**：1 个 Major — 批量更新低效
- **验证标准**：
  - [ ] 指出循环 `add()` 触发 N 次变更事件，导致 TableView 频繁重绘
  - [ ] 建议改用 `setAll()` 一次性替换（触发 1 次变更事件）
  - [ ] 说明触发事件次数差异（N 次 vs 1 次）
  - [ ] 严重性判定为 Major（数据量 >10000 条且在 FX 线程执行可升级 Critical）
  - [ ] 规范依据引用 `performance-guide.md — 批量更新规则`

---

### 用例 5：Spring Boot 陷阱

- **输入**：Spring Boot 集成场景下的两类典型错误
  ```java
  // 错误 1：启动类直接继承 Application
  @SpringBootApplication
  public class MyApp extends Application {  // ❌ 直接继承 Application
      @Override
      public void start(Stage stage) { /* ... */ }
      public static void main(String[] args) { launch(args); }
  }
  ```
  ```java
  // 错误 2：Controller 单例但持有 @FXML 状态字段
  @Component
  // 缺少 @Scope("prototype")
  public class UserController implements Initializable {
      @FXML private TextField nameField;  // 单例 Controller 持有状态字段
  }
  ```
- **类型**：正样本
- **覆盖维度**：深度合规审核（Spring Boot 陷阱）
- **预期发现**：1 个 Critical（启动类继承 Application）+ 1 个 Major（Controller 缺少 @Scope）
- **验证标准**：
  - [ ] 指出启动类直接继承 `Application` 将导致 "JavaFX runtime components are missing" 错误
  - [ ] 建议拆分为 `MyApp`（不继承 Application）+ `JavaFXApp`（继承 Application）
  - [ ] 启动类问题判定为 Critical（不可降级，导致 Spring 容器初始化异常）
  - [ ] 指出 Controller 缺少 `@Scope("prototype")`，单例状态污染
  - [ ] Controller 问题判定为 Major（持有 @FXML 状态字段，保持 Major）
  - [ ] 规范依据引用 `compliance-rules.md — Spring Boot 陷阱`

---

### 用例 6：CSS var() 误用

- **输入**：CSS 文件中使用了 Web CSS 的 `var()` 函数
  ```css
  .root {
      -fx-primary-color: #2196f3;
  }
  .button-primary {
      /* ❌ JavaFX CSS 不支持 var() */
      -fx-background-color: var(-fx-primary-color);
      -fx-background-radius: var(-fx-radius);
  }
  ```
- **类型**：正样本
- **覆盖维度**：深度合规审核（CSS 合规）
- **预期发现**：1 个 Major — 使用不支持的 var() 语法
- **验证标准**：
  - [ ] 指出 JavaFX CSS 不支持 `var()` 函数（这是 Web CSS 特性）
  - [ ] 给出直接引用查找色的方案：`-fx-background-color: -fx-primary-color;`
  - [ ] 圆角应改用字面量数值：`-fx-background-radius: 8;`
  - [ ] 严重性判定为 Major（不支持语法，不可降级）
  - [ ] 规范依据引用 `css-compliance.md — var() 禁止规则`

---

### 用例 7：API 误用（不存在的 dispose）

- **输入**：代码声称使用 `@FXML dispose()` 生命周期方法
  ```java
  public class MainController implements Initializable {
      @FXML
      private void dispose() {  // ❌ @FXML dispose() 不存在，非生命周期方法
          model.removeListener(listener);
      }
  }
  ```
- **类型**：正样本
- **覆盖维度**：深度合规审核（API 误用排查）
- **预期发现**：1 个 Major — 使用不存在的 API
- **验证标准**：
  - [ ] 指出 `@FXML dispose()` 不是 JavaFX 生命周期方法，不会被框架自动调用
  - [ ] 建议自定义 `dispose()` 方法（去掉 @FXML），通过 `setOnCloseRequest` 或视图切换回调显式触发
  - [ ] 严重性判定为 Major
  - [ ] 规范依据引用 `compliance-rules.md — API 误用排查`

---

### 用例 8：综合项目审核

- **输入**：一个完整的 JavaFX 项目（含多个 Controller、FXML、CSS、module-info），同时存在线程违规、fx:id 不匹配、监听器泄漏、CSS var() 等多种问题
- **类型**：正样本
- **覆盖维度**：全维度（六大维度）
- **预期发现**：跨维度多个问题（Critical + Major + Minor 混合）
- **验证标准**：
  - [ ] 输出完整评审报告，含评审摘要、问题清单、合规性总结表三部分
  - [ ] 问题清单按严重性降序排列（Critical → Major → Minor → Info）
  - [ ] 同一根因引发的多个表现合并为一个问题（去重）
  - [ ] 合规性总结表列出每个维度的检查项数、通过数、不通过数、通过率
  - [ ] 评审结论为"不通过"（存在 Critical 问题）
  - [ ] 每个问题含修复交接字段

---

### 用例 9：合规代码（负样本）

- **输入**：完全合规的 JavaFX 代码
  ```java
  // 线程安全：使用 Task + Platform.runLater
  // FXML：fx:id 与 @FXML 一一对应
  // 内存：监听器在 dispose() 中移除，Binding 已释放
  // CSS：使用查找色直接引用，无 var()，圆角用字面量
  // 命名：PascalCase / camelCase / SCREAMING_SNAKE_CASE 规范
  ```
- **类型**：负样本
- **覆盖维度**：全维度
- **预期发现**：0 个问题
- **验证标准**：
  - [ ] 问题清单为空
  - [ ] 评审结论为"通过"
  - [ ] 合规性总结表通过率 100%
  - [ ] 无误报（零假阳性）

---

### 用例 10：监听器降级判定

- **输入**：Controller 注册监听器，但监听对象的生命周期与 Controller 相同（同生共灭）
  ```java
  public class ListController implements Initializable {
      private final ObservableList<String> items = FXCollections.observableArrayList();
      private final ListChangeListener<String> listener = c -> updateCount();

      @Override
      public void initialize(URL location, ResourceBundle resources) {
          // items 是 Controller 的私有字段，随 Controller 一起销毁
          items.addListener(listener);
      }
  }
  ```
- **类型**：边界用例
- **覆盖维度**：内存泄漏风险
- **预期发现**：1 个 Major（从 Critical 降级）
- **验证标准**：
  - [ ] 识别出监听对象（`items`）生命周期与 Controller 相同
  - [ ] 严重性判定为 Major 而非 Critical（触发降级条件）
  - [ ] 报告"升降级说明"字段注明降级条件："监听对象生命周期与 Controller 相同（同生共灭），降为 Major"
  - [ ] 规范依据引用 `memory-management.md — 监听器移除规则` + SKILL.md 升降级条件表

---

### 用例 11：增量评审（仅改 CSS）

- **输入**：仅修改了 CSS 文件（如新增 `.button-primary` 样式类），用户要求增量评审
- **类型**：边界用例
- **覆盖维度**：指定维度（FXML 规范 + CSS 合规）
- **预期发现**：仅 CSS 相关问题（若有），不涉及线程安全/内存泄漏
- **验证标准**：
  - [ ] 跳过线程安全、内存泄漏、性能、代码结构维度
  - [ ] 仅执行 FXML 规范（styleClass 一致性检查）+ CSS 合规扫描
  - [ ] 报告头部标注评审模式为"增量评审"
  - [ ] 报告"评审范围"字段列出实际评审的 CSS 文件
  - [ ] 不报告与 CSS 无关的问题

---

### 用例 12：修复交接完整性

- **输入**：含多个问题（线程违规 + fx:id 不匹配 + 通配符导入）的代码
- **类型**：正样本
- **覆盖维度**：全维度
- **预期发现**：每个问题均含完整修复交接字段
- **验证标准**：
  - [ ] 每个问题含 `target_file`（文件路径）
  - [ ] 每个问题含 `target_lines`（起止行号）
  - [ ] 每个问题含 `fix_type`（replace / insert / delete）
  - [ ] 每个问题含 `fix_priority`（按严重性 + 位置排序的优先级，1 为最高）
  - [ ] 修复交接字段可被 `javafx-developer` 直接消费执行修复
  - [ ] fix_type=replace 时附有"修正示例"代码

---

### 用例 13：Controller 越层访问数据层

- **输入**：Controller 中直接包含 JDBC 查询逻辑，Service 层被绕过
  ```java
  public class UserController implements Initializable {
      @FXML private TableView<User> userTable;

      @FXML
      private void handleLoad() {
          // ❌ Controller 直接操作数据库，绕过 Service 层
          try (Connection conn = DriverManager.getConnection("jdbc:sqlite:app.db")) {
              Statement stmt = conn.createStatement();
              ResultSet rs = stmt.executeQuery("SELECT * FROM users");
              while (rs.next()) {
                  users.add(new User(rs.getString("name")));
              }
          } catch (SQLException e) { e.printStackTrace(); }
      }
  }
  ```
- **类型**：正样本
- **覆盖维度**：代码结构合理性
- **预期发现**：1 个 Major — 架构分层混乱（Controller 越层）
- **验证标准**：
  - [ ] 指出 Controller 越层访问数据层，违反职责单一性
  - [ ] 建议将数据访问委托给 Service 层，Controller 仅调用 `userService.loadAll()`
  - [ ] 严重性判定为 Major（默认基线；若仅个别方法越层不影响整体架构可降 Minor）
  - [ ] 规范依据引用 `structure-review.md — 架构模式合规性 / 职责单一性`
  - [ ] 含修复交接字段

---

### 用例 14：静态引用持有 Stage

- **输入**：类中 `private static Stage mainStage` 持有 UI 组件引用
  ```java
  public class StageManager {
      // ❌ 静态字段持有 Stage 引用，导致无法 GC
      private static Stage mainStage;

      public static void setMainStage(Stage stage) {
          mainStage = stage;
      }
  }
  ```
- **类型**：正样本
- **覆盖维度**：内存泄漏风险
- **预期发现**：1 个 Critical — 静态引用泄漏（不可降级）
- **验证标准**：
  - [ ] 严重性判定为 Critical（不可降级）
  - [ ] 指出静态字段持有 `Stage` 导致无法 GC，Stage 关闭后仍被静态引用保持
  - [ ] 建议改为实例字段或使用 `WeakReference` / `ObjectProperty<Stage>`
  - [ ] 规范依据引用 `memory-management.md — 静态引用排查` + SKILL.md 升降级条件表（标注不可降级）
  - [ ] 含修复交接字段
