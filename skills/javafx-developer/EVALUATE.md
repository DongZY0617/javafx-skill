# 评估用例集

本文件定义了 JavaFX Developer 技能的验收用例，用于量化技能输出质量。每个用例描述输入场景、预期输出和验证标准。

---

## 用例 1：基础 CRUD 表格视图

**输入**：「帮我创建一个用户管理界面，用 TableView 显示用户列表，支持新增、编辑、删除」

**预期输出**：
- 生成 `User` 模型类（含 `StringProperty`/`LongProperty` 等 JavaFX Properties）
- 生成 `UserController` 实现 `Initializable`，包含 `TableView` 绑定逻辑
- 生成 FXML 布局文件，包含 `TableView` + `TableColumn`
- 生成对应的 CSS 样式文件
- 提供 Maven/Gradle 依赖说明和运行命令

**验证标准**：
- [ ] `TableView` 的 `TableColumn` 通过 `cellValueFactory` 绑定到 Model 的 Properties
- [ ] FXML 的 `fx:id` 与 Controller 的 `@FXML` 字段一一对应
- [ ] `module-info.java` 包含 `opens model to javafx.base`（支持 PropertyValueFactory 反射）
- [ ] 代码可编译，无语法错误
- [ ] CSS 样式不使用 `var()` 函数

---

## 用例 2：MVVM 架构应用

**输入**：「用 MVVM 模式创建一个任务管理应用，需要 ViewModel 暴露 Properties 供 View 绑定」

**预期输出**：
- 生成 `Task` 模型类和 `TaskViewModel` 类
- ViewModel 暴露 `StringProperty`/`BooleanProperty` 等可绑定属性
- View（FXML + Controller）通过双向绑定连接 ViewModel
- Controller 仅处理 UI 事件，业务逻辑委托给 ViewModel/Service

**验证标准**：
- [ ] ViewModel 不直接引用 UI 控件（无 `@FXML` 注入）
- [ ] 双向绑定使用 `bindBidirectional()`
- [ ] 计算属性使用 `Bindings.createXxxBinding()`（不使用不存在的 `select()` API）
- [ ] Service 层通过构造注入到 ViewModel

---

## 用例 3：Spring Boot + JavaFX 整合

**输入**：「用 Spring Boot + JavaFX + MyBatis + SQLite 创建一个应用」

**预期输出**：
- 启动类拆分为 `MyApp`（不继承 Application）+ `JavaFXApp`（继承 Application）
- Controller 标注 `@Component` + `@Scope("prototype")`
- `application.yml` 配置 `web-application-type: none`
- MyBatis Mapper 接口和 XML 映射文件
- `controllerFactory` 设置为 `springContext::getBean`

**验证标准**：
- [ ] 主类**不**继承 `Application`（避免 "JavaFX runtime components are missing" 错误）
- [ ] Controller 标注 `@Scope("prototype")`（避免单例状态污染）
- [ ] JavaFX Properties 的 setter 处理 null（避免 `SimpleLongProperty.set(null)` NPE）
- [ ] `application.yml` 中 `spring.main.web-application-type` 设为 `none`
- [ ] 未引入 `spring-boot-devtools`（或设为 optional + 禁用 restart）

---

## 用例 4：对话框与表单验证

**输入**：「创建一个用户输入对话框，包含名称和邮箱字段，保存按钮在输入无效时禁用」

**预期输出**：
- 对话框 FXML 布局（TextField + TextArea + Button）
- DialogController 继承 BaseController，处理 OK/Cancel 事件
- 表单验证使用 `BooleanBinding` 组合实现声明式校验
- 保存按钮的 `disableProperty` 绑定到验证 Binding

**验证标准**：
- [ ] 对话框 Controller 可通过 `getResult()` 获取用户输入
- [ ] 验证逻辑使用 `Bindings.createBooleanBinding()` 或 `isEmpty().or()` 组合
- [ ] 使用 JavaFX 原生 `Alert` 或 `Dialog`（不使用 ControlsFX 旧 `Dialogs.create()` API）
- [ ] 对话框关闭后资源正确释放

---

## 用例 5：跨平台打包

**输入**：「把我的 JavaFX 应用打包成 Windows exe 安装包」

**预期输出**：
- 提供 `jpackage` 命令，包含 `--type exe`、`--win-menu`、`--win-shortcut`
- 包含 `--win-upgrade-uuid` 参数
- 包含 `--java-options "--enable-native-access=javafx.graphics"`
- 说明需安装 Inno Setup（exe）或 WiX Toolset 4.x（msi）
- 提供图标格式要求（`.ico`，多尺寸内嵌）

**验证标准**：
- [ ] 命令中包含 `--enable-native-access=javafx.graphics`
- [ ] WiX 版本说明为 4.x（通过 `dotnet tool install` 安装），不是 3.x
- [ ] 不使用 `gu install native-image`（GraalVM JDK 21+ 已内置）
- [ ] `--win-upgrade-uuid` 使用有效 UUID 格式

---

## 用例 6：CSS 主题切换

**输入**：「实现亮色/暗色主题切换功能」

**预期输出**：
- 亮色和暗色两套 CSS 文件，`.root` 中定义主题变量
- ThemeManager 类管理主题切换和偏好持久化
- 颜色变量使用直接引用（`-fx-primary`），不使用 `var()`
- 圆角使用字面量数值，不通过查找色引用尺寸变量

**验证标准**：
- [ ] CSS 中**不**使用 `var()` 函数（JavaFX CSS 不支持）
- [ ] 颜色变量在 `.root` 中定义，子节点直接按名引用
- [ ] `-fx-border-radius`/`-fx-background-radius` 使用字面量数值
- [ ] 主题切换通过 `scene.getStylesheets().setAll()` 实现
- [ ] 用户偏好持久化到 `Preferences`

---

## 用例 7：数据绑定与内存管理

**输入**：「实现一个主从视图，选中列表项时更新详情表单，注意防止内存泄漏」

**预期输出**：
- 使用 `FilteredList` + `SortedList` 处理列表过滤排序
- 选中监听器更新详情视图
- 在自定义 `dispose()` 方法中移除监听器（不使用不存在的 `@FXML dispose()`）
- 通过 `stage.setOnCloseRequest()` 或视图切换回调触发清理

**验证标准**：
- [ ] 不使用 `person.select(p -> ...)` 不存在的 API
- [ ] 监听器在自定义清理方法中通过 `removeListener()` 移除
- [ ] 不声称存在 `@FXML dispose()` 生命周期方法
- [ ] `Bindings.createXxxBinding()` 返回的 Binding 在不需要时被释放
- [ ] 后台任务使用 `Task` + `Platform.runLater()` 回到 UI 线程

---

## 用例 8：版本选择与兼容性

**输入**：「我用的 JDK 17，应该选哪个 JavaFX 版本？」

**预期输出**：
- 推荐 JavaFX 21 LTS（JDK 17+，成熟稳定）
- 说明 JavaFX 25 LTS 为最新 LTS（JDK 23+），如需最新特性可升级
- 说明 JavaFX 17 LTS 至 2026.10 结束支持
- 提醒 JavaFX 24+ 需添加 `--enable-native-access=javafx.graphics`

**验证标准**：
- [ ] 版本矩阵与 Gluon 官方路线图一致
- [ ] JavaFX 25 已标注为已发布 LTS（非"计划"/"预期"）
- [ ] JavaFX 26 已标注为已发布（非"计划"）
- [ ] JavaFX 17 LTS 标注支持至 2026.10
- [ ] 提及 `--enable-native-access` 要求适用于 JavaFX 24+
