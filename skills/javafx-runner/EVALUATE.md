# JavaFX Runner 评估用例集

本文件定义 `javafx-runner` 技能的验收用例，用于量化验证输出质量。每个用例描述输入场景、用例类型、覆盖维度、预期发现的问题及可勾选的验证标准。

- **正样本**：有问题的项目，验证 runner 的召回率（应发现的问题是否全部发现，编译错误/运行时异常/打包失败是否准确捕获）
- **负样本**：可正常运行的项目，验证 runner 的精确率（不应误报，零假阳性）
- **边界用例**：降级判定、短路机制、headless 适配等，验证 runner 在边界条件下的判定准确性

---

## 用例总览

| 编号 | 名称 | 类型 | 覆盖维度 | 预期问题数 |
|------|------|------|---------|-----------|
| 1 | 编译错误-缺失导入 | 正样本 | 编译验证 | 1 Critical |
| 2 | 模块 opens 缺失 | 正样本 | 编译验证 + 运行验证 | 1 Critical |
| 3 | FXML fx:controller 路径错误 | 正样本 | 编译验证 + 运行验证 | 1 Critical |
| 4 | CSS var() 误用 | 正样本 | 运行验证 | 1 Major |
| 5 | 后台线程更新 UI 运行时验证 | 正样本 | 运行验证 | 1 Critical |
| 6 | JavaFX 24+ 缺少原生访问配置 | 正样本 | 运行验证 + 打包验证 | 1 Critical |
| 7 | jpackage 工具链缺失 | 正样本（降级 Info） | 打包验证 | 1 Info（降级） |
| 8 | jpackage 模块路径错误 | 正样本 | 打包验证 | 1 Critical（升级） |
| 9 | 可正常运行项目 | 负样本 | 全维度 | 0（零误报） |
| 10 | 启动超时 start() 阻塞 | 边界 | 运行验证 | 1 Critical（升级） |
| 11 | Headless 环境 FXML 加载 | 边界 | 运行验证 | headless 适配判定 |
| 12 | 编译失败短路 | 边界 | 编译验证 | 短路跳过后续 |
| 13 | 修复交接完整性 | 正样本 | 全维度 | 含完整交接字段 |

---

## 用例详情

### 用例 1：编译错误-缺失导入

- **输入**：Java 源文件使用了未导入的类，`mvn compile` 报 `cannot find symbol`
  ```java
  package com.example;

  public class UserController {
      // List / ArrayList 未导入，编译失败
      private List<User> users = new ArrayList<>();

      public void add(User u) {
          users.add(u);
      }
  }
  ```
- **类型**：正样本
- **覆盖维度**：编译验证（检查项 1：语法编译）
- **预期发现**：1 个 Critical — 缺失导入致编译失败
- **验证标准**：
  - [ ] 执行 `mvn compile -q` 捕获编译错误输出
  - [ ] 准确解析 `[ERROR] /path/UserController.java:[line,col] cannot find symbol` 格式
  - [ ] 指出缺失的导入（`java.util.List`、`java.util.ArrayList`）
  - [ ] 严重性判定为 Critical（不可降级，编译失败则项目无法运行）
  - [ ] 因编译失败短路，跳过运行验证与打包验证，报告中注明"因编译失败跳过后续验证"
  - [ ] 含修复交接字段（target_file / target_lines / fix_type=insert / fix_priority）

---

### 用例 2：模块 opens 缺失

- **输入**：`module-info.java` 缺少 `opens` 声明，编译通过但运行时反射失败
  ```java
  // module-info.java
  module com.example.app {
      requires javafx.controls;
      requires javafx.fxml;
      // 缺少 opens com.example.controller to javafx.fxml;
      // 缺少 opens com.example.model to javafx.controls;
      exports com.example;
  }
  ```
  ```java
  // UserController 使用 FXMLLoader 加载，User 模型用 PropertyValueFactory 反射
  TableColumn<User, String> nameCol = new TableColumn<>("姓名");
  nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
  ```
- **类型**：正样本
- **覆盖维度**：编译验证（检查项 3：模块配置）+ 运行验证（检查项 5：模块运行时）
- **预期发现**：1 个 Critical — 模块 opens 缺失致运行时反射失败（编译维度提前标记 + 运行维度实际捕获）
- **验证标准**：
  - [ ] 编译验证阶段主动比对 `module-info.java` 与实际包结构，标记 opens 缺失
  - [ ] 编译验证指出缺少 `opens com.example.controller to javafx.fxml` 和 `opens com.example.model to javafx.controls`
  - [ ] 运行验证阶段实际执行 `mvn javafx:run` 捕获 `IllegalAccessException` / `LoadException`
  - [ ] 同一根因引发的编译维度标记与运行时异常合并为一个问题（去重）
  - [ ] 严重性判定为 Critical（降级条件：缺失的 opens 不影响当前功能 → Major；本案使用 PropertyValueFactory，不可降级）
  - [ ] 规范依据引用 `compile-verification.md — 模块配置` + `runtime-verification.md — 模块运行时`
  - [ ] 含修复交接字段（fix_type=insert，插入 opens 声明）

---

### 用例 3：FXML fx:controller 路径错误

- **输入**：FXML 中 `fx:controller` 包名拼写错误，类不存在
  ```xml
  <!-- user-view.fxml -->
  <VBox xmlns="http://javafx.com/javafx/21"
        xmlns:fx="http://javafx.com/fxml/1"
        fx:controller="com.example.controler.UserController">
      <!-- "controler" 拼写错误，应为 "controller" -->
  </VBox>
  ```
- **类型**：正样本
- **覆盖维度**：编译验证（检查项 4：FXML 编译关联）+ 运行验证（检查项 2：FXML 加载）
- **预期发现**：1 个 Critical — fx:controller 路径错误致运行时 LoadException
- **验证标准**：
  - [ ] 编译验证阶段关联检查 FXML 的 `fx:controller` 与实际类，标记路径不匹配
  - [ ] 运行验证阶段实际捕获 `ClassNotFoundException: com.example.controler.UserController` 包装的 `LoadException`
  - [ ] 指出拼写错误（`controler` 应为 `controller`）
  - [ ] 严重性判定为 Critical（不可降级，运行时必然抛 LoadException）
  - [ ] 规范依据引用 `compile-verification.md — FXML 编译关联` + `runtime-verification.md — FXML 加载`
  - [ ] 含修复交接字段

---

### 用例 4：CSS var() 误用

- **输入**：CSS 文件使用 JavaFX CSS 不支持的 `var()` 函数
  ```css
  .root {
      -fx-primary-color: #2196f3;
  }
  .button-primary {
      -fx-background-color: var(-fx-primary-color);
      -fx-background-radius: var(-fx-radius);
  }
  ```
- **类型**：正样本
- **覆盖维度**：运行验证（检查项 3：CSS 解析）
- **预期发现**：1 个 Major — 使用不支持的 var() 语法
- **验证标准**：
  - [ ] 运行验证捕获输出 `WARNING: Could not resolve '-fx-primary-color'`
  - [ ] 指出 JavaFX CSS 不支持 `var()` 函数（Web CSS 特性）
  - [ ] 给出直接引用查找色的方案：`-fx-background-color: -fx-primary-color;`
  - [ ] 圆角改用字面量数值：`-fx-background-radius: 8;`
  - [ ] 严重性判定为 Major（不支持语法，不可降级；若仅 WARNING 不影响渲染可降 Minor，本案影响样式生效保持 Major）
  - [ ] 规范依据引用 `runtime-verification.md — CSS 解析`

---

### 用例 5：后台线程更新 UI 运行时验证

- **输入**：后台线程直接更新 UI，运行时抛 `IllegalStateException`
  ```java
  new Thread(() -> {
      try {
          Thread.sleep(2000);
      } catch (InterruptedException e) { }
      statusLabel.setText("完成");   // 后台线程直接更新 UI
  }).start();
  ```
- **类型**：正样本
- **覆盖维度**：运行验证（检查项 6：线程安全运行时验证）
- **预期发现**：1 个 Critical — 运行时抛 IllegalStateException
- **验证标准**：
  - [ ] 运行验证捕获 `IllegalStateException: Not on FX application thread; currentThread = Thread-0`
  - [ ] 准确指出 `setText` 在非 FX 线程调用
  - [ ] 给出 `Platform.runLater()` 或 `Task.updateMessage()` 修正方案
  - [ ] 严重性判定为 Critical（不可降级，运行时必然抛异常）
  - [ ] 与 reviewer 静态审核结论交叉验证（reviewer 静态发现代码模式，runner 动态捕获运行时异常）
  - [ ] 规范依据引用 `runtime-verification.md — 线程安全运行时验证` + `thread-safety-rules.md — FX 线程更新`
  - [ ] 含修复交接字段

---

### 用例 6：JavaFX 24+ 缺少原生访问配置

- **输入**：JavaFX 24 项目，`pom.xml` 的 `javafx-maven-plugin` 未配置 `--enable-native-access`
  ```xml
  <plugin>
      <groupId>org.openjfx</groupId>
      <artifactId>javafx-maven-plugin</artifactId>
      <version>0.0.8</version>
      <configuration>
          <mainClass>com.example.App</mainClass>
          <!-- 缺少 <options><option>--enable-native-access=javafx.graphics</option></options> -->
      </configuration>
  </plugin>
  ```
- **类型**：正样本
- **覆盖维度**：运行验证（检查项 7：JavaFX 24+ 原生访问）+ 打包验证（检查项 4：原生访问配置）
- **预期发现**：1 个 Critical — JavaFX 24+ 缺少原生访问配置
- **验证标准**：
  - [ ] 环境检测阶段正确提取 JavaFX 版本为 24，触发原生访问检查项
  - [ ] 运行验证捕获 `IllegalAccessError` 或 `WARNING: A restricted method in javafx.graphics has been called`
  - [ ] 打包验证指出 jpackage 命令缺少 `--java-options "--enable-native-access=javafx.graphics"`
  - [ ] 同一根因（缺少原生访问配置）在运行与打包维度的表现合并为一个问题
  - [ ] 严重性判定为 Critical（不可降级，JavaFX 24+ 运行时必然报 IllegalAccessError）
  - [ ] 规范依据引用 `runtime-verification.md — JavaFX 24+ 原生访问` + `packaging-verification.md — 原生访问配置`
  - [ ] 含修复交接字段（运行配置与打包配置两处）

---

### 用例 7：jpackage 工具链缺失

- **输入**：Windows 项目执行 `jpackage` 生成 `.exe`，但未安装 Inno Setup
  ```bash
  jpackage --name MyApp --input target --main-jar app.jar \
           --main-class com.example.App --type exe
  ```
  输出：`Failed to find Inno Setup. No .exe bundle generated.`
- **类型**：正样本（降级 Info）
- **覆盖维度**：打包验证（检查项 5：平台工具链 + 检查项 7：安装包生成）
- **预期发现**：1 个 Info（从 Major 降级）— 工具链缺失（环境问题，非代码问题）
- **验证标准**：
  - [ ] 打包验证捕获 `Failed to find Inno Setup` 输出
  - [ ] 诊断根因为工具链缺失（环境问题），而非代码/配置问题
  - [ ] 严重性判定为 Info（触发降级条件：工具链未安装属环境问题，降为 Info）
  - [ ] 报告"升降级说明"字段注明降级条件："工具链未安装（环境问题非代码问题），降为 Info"
  - [ ] 提供安装建议（如 `choco install innosetup`）
  - [ ] 不阻断代码交付（Info 级别仅提示）
  - [ ] 规范依据引用 `packaging-verification.md — 平台工具链`

---

### 用例 8：jpackage 模块路径错误

- **输入**：`jpackage` 的 `--module-path` 缺少 JavaFX SDK lib，`--add-modules` 遗漏 `javafx.fxml`
  ```bash
  jpackage \
    --name MyApp \
    --module com.example.app/com.example.App \
    --module-path target/modules \
    --add-modules javafx.controls
  # target/modules 缺少 JavaFX SDK，且遗漏 javafx.fxml
  ```
  安装包生成但运行时报 `Module javafx.fxml not found`
- **类型**：正样本
- **覆盖维度**：打包验证（检查项 2：模块路径完整性 + 检查项 7：安装包生成）
- **预期发现**：1 个 Critical（从 Major 升级）— module-path 配置错误致产物无法运行
- **验证标准**：
  - [ ] 打包验证识别 `--module-path` 缺少 JavaFX SDK lib 目录
  - [ ] 识别 `--add-modules` 遗漏实际使用的 `javafx.fxml`
  - [ ] 严重性判定为 Critical（触发升级条件：module-path 配置错误导致生成产物无法运行，从 Major 升级为 Critical）
  - [ ] 报告"升降级说明"字段注明升级条件："--module-path 配置错误导致生成产物无法运行，升为 Critical"
  - [ ] 给出修正命令（补充 `--module-path` 含 JavaFX SDK lib，`--add-modules` 补全 `javafx.fxml`）
  - [ ] 规范依据引用 `packaging-verification.md — 模块路径完整性`

---

### 用例 9：可正常运行项目

- **输入**：完全可编译、可运行、可打包的 JavaFX 项目
  ```
  - pom.xml 依赖完整（javafx-controls + javafx-fxml，版本一致）
  - module-info.java 声明完整（requires + opens 齐全）
  - FXML fx:controller 路径正确，fx:id 与 @FXML 一一对应
  - CSS 使用查找色直接引用，无 var()
  - 后台任务通过 Platform.runLater / Task 回调更新 UI
  - jpackage 命令参数完整，工具链已安装
  ```
- **类型**：负样本
- **覆盖维度**：全维度（编译验证 + 运行验证 + 打包验证）
- **预期发现**：0 个问题
- **验证标准**：
  - [ ] 编译验证：`mvn compile -q` 退出码 0，无 `[ERROR]` / `[WARNING]`
  - [ ] 运行验证：`mvn javafx:run` 退出码 0，无异常堆栈，主窗口正常显示
  - [ ] 打包验证：`mvn package` + `jpackage` 成功生成安装包，产物大小合理
  - [ ] 问题清单为空
  - [ ] 验证结论为"通过"
  - [ ] 验证结果总结表通过率 100%
  - [ ] 无误报（零假阳性）

---

### 用例 10：启动超时 start() 阻塞

- **输入**：`start()` 方法中调用 `Thread.sleep(30000)` 阻塞 FX 线程，导致启动超时
  ```java
  @Override
  public void start(Stage stage) throws Exception {
      Thread.sleep(30000);  // 阻塞 FX 线程 30 秒，启动超时
      String data = fetchDataFromNetwork();
      Label label = new Label(data);
      stage.setScene(new Scene(label, 300, 200));
      stage.show();
  }
  ```
- **类型**：边界用例
- **覆盖维度**：运行验证（检查项 9：启动超时检测）
- **预期发现**：1 个 Critical（从 Major 升级）— start() 中阻塞调用致启动超时
- **验证标准**：
  - [ ] 运行验证在 30 秒超时后终止进程，记录"启动超时，进程已终止"
  - [ ] 识别超时根因为 `start()` 中 `Thread.sleep()` 阻塞调用
  - [ ] 严重性判定为 Critical（触发升级条件：超时因 start() 中阻塞调用，从 Major 升级为 Critical）
  - [ ] 报告"升降级说明"字段注明升级条件："超时因 start() 中 Thread.sleep 阻塞调用，升为 Critical"
  - [ ] 给出修正方案：阻塞操作移至后台 Task，start() 快速完成
  - [ ] 规范依据引用 `runtime-verification.md — 启动超时检测` + SKILL.md 升降级条件表

---

### 用例 11：Headless 环境 FXML 加载

- **输入**：CI 无显示器环境，未配置 Monocle，运行 JavaFX 应用加载 FXML 报 `HeadlessException`
  ```bash
  # CI 环境（DISPLAY 为空），直接运行未配置 Monocle
  mvn javafx:run
  ```
  输出：`java.awt.HeadlessException`
- **类型**：边界用例
- **覆盖维度**：运行验证（检查项 8：Headless 模式验证）
- **预期发现**：headless 适配判定（环境配置问题，非代码问题）
- **验证标准**：
  - [ ] 环境检测阶段识别无显示器（DISPLAY 为空 / 无桌面会话）
  - [ ] 运行验证捕获 `java.awt.HeadlessException` 或 `UnsatisfiedLinkError: libglass.so`
  - [ ] 诊断为环境配置问题（未配置 Monocle），非代码问题
  - [ ] 提供 headless 运行命令：`mvn javafx:run -Dmonocle.platform=Headless -Dprism.order=sw`
  - [ ] 指出 Monocle 依赖配置方法（参照 `environment-setup.md — Monocle 依赖配置`）
  - [ ] 提示 Monocle 版本须与 JavaFX 版本严格匹配
  - [ ] 严重性判定为 Major（降级条件：本地有显示器正常，仅 CI headless 失败且非交付必需 → Minor）
  - [ ] 规范依据引用 `runtime-verification.md — Headless 模式验证` + `environment-setup.md`

---

### 用例 12：编译失败短路

- **输入**：项目存在编译错误（缺失导入），同时存在运行时问题（模块 opens 缺失）和打包问题（jpackage 配置错误）
  ```
  - 编译验证：UserController.java 缺失 List/ArrayList 导入，编译失败
  - 运行验证（若执行）：module-info.java 缺少 opens
  - 打包验证（若执行）：jpackage --module-path 错误
  ```
- **类型**：边界用例
- **覆盖维度**：编译验证（短路机制）
- **预期发现**：仅编译验证问题，运行验证与打包验证被短路跳过
- **验证标准**：
  - [ ] 编译验证捕获 `cannot find symbol` 编译错误，判定 Critical
  - [ ] 触发短路机制：因编译失败跳过运行验证和打包验证
  - [ ] 报告中明确注明"因编译失败跳过后续验证"
  - [ ] 验证结果总结表中运行验证与打包验证标记为"跳过"
  - [ ] 不执行 `mvn javafx:run` 和 `mvn package` / `jpackage`（无法运行未编译的代码）
  - [ ] 仅报告编译验证的 1 个 Critical 问题
  - [ ] 修复编译错误后，重新验证时执行运行验证与打包验证
  - [ ] 规范依据引用 SKILL.md 工作流步骤 2"编译失败短路"

---

### 用例 13：修复交接完整性

- **输入**：含多个验证问题的项目（编译错误 + 模块 opens 缺失 + jpackage 配置错误）
- **类型**：正样本
- **覆盖维度**：全维度
- **预期发现**：每个问题均含完整修复交接字段
- **验证标准**：
  - [ ] 每个问题含 `target_file`（文件路径）
  - [ ] 每个问题含 `target_lines`（起止行号）
  - [ ] 每个问题含 `fix_type`（replace / insert / delete）
  - [ ] 每个问题含 `fix_priority`（按严重性 + 验证维度排序的优先级，1 为最高）
  - [ ] fix_priority 排序规则：Critical 优先于 Major > Minor > Info；同等级按编译验证 > 运行验证 > 打包验证排列
  - [ ] 修复交接字段可被 `javafx-developer` 直接消费执行修复
  - [ ] fix_type=replace 时附有"修正示例"代码或命令
  - [ ] fix_type=insert 时附有需插入的代码或配置（如 opens 声明、--enable-native-access 参数）
  - [ ] 修复交接汇总表按 fix_priority 排序列出所有问题
  - [ ] 报告结构与 `javafx-code-reviewer` 评审报告同构，修复交接字段格式完全一致
