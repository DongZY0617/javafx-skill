# JavaFX 架构模式指南

本指南详细介绍 JavaFX 应用中常用的架构模式，包括 MVC、MVVM 的完整实现示例、模式对比、常见反模式、服务层设计、依赖注入方案以及事件总线模式。

---

## 一、MVC 模式（Model-View-Controller）

MVC 是最经典的 UI 架构模式。在 JavaFX 中，Model 使用 JavaFX Properties 暴露数据，View 使用 FXML 描述，Controller 通过 `@FXML` 注解处理用户交互并更新 Model。

### 1.1 职责划分

| 组件        | 职责                                                                 |
|-------------|----------------------------------------------------------------------|
| Model       | 数据模型与业务状态，使用 JavaFX Property 实现可观察性。              |
| View        | 纯 UI 声明（FXML），不包含业务逻辑，通过绑定展示 Model 数据。        |
| Controller  | 接收用户事件，调用 Service/Model 方法，协调 View 与 Model 交互。      |

### 1.2 完整代码示例

**Model — `Task.java`**

```java
package com.example.mvc.model;

import javafx.beans.property.*;

public class Task {
    private final StringProperty title = new SimpleStringProperty();
    private final BooleanProperty completed = new SimpleBooleanProperty(false);

    public Task(String title) {
        setTitle(title);
    }

    public StringProperty titleProperty() { return title; }
    public String getTitle() { return title.get(); }
    public void setTitle(String value) { title.set(value); }

    public BooleanProperty completedProperty() { return completed; }
    public boolean isCompleted() { return completed.get(); }
    public void setCompleted(boolean value) { completed.set(value); }
}
```

**View — `task.fxml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<VBox spacing="10" xmlns="http://javafx.com/javafx/21"
      xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="com.example.mvc.controller.TaskController">
    <HBox spacing="10">
        <TextField fx:id="titleField" promptText="输入任务标题" HBox.hgrow="ALWAYS"/>
        <Button text="添加" onAction="#handleAddTask"/>
    </HBox>
    <ListView fx:id="taskListView" VBox.vgrow="ALWAYS"/>
    <Label fx:id="statusLabel" text="共 0 个任务"/>
</VBox>
```

**Controller — `TaskController.java`**

```java
package com.example.mvc.controller;

import com.example.mvc.model.Task;
import com.example.mvc.service.TaskService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class TaskController implements Initializable {

    @FXML private TextField titleField;
    @FXML private ListView<Task> taskListView;
    @FXML private Label statusLabel;

    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final TaskService taskService = new TaskService(); // Service 层

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        taskListView.setItems(tasks);
        taskListView.setCellFactory(lv -> new TaskListCell());

        // 绑定状态标签到任务数量
        statusLabel.textProperty().bind(
            Bindings.size(tasks).asString("共 %d 个任务"));

        // 加载初始数据
        tasks.addAll(taskService.loadTasks());
    }

    @FXML
    private void handleAddTask() {
        String title = titleField.getText().trim();
        if (!title.isEmpty()) {
            Task task = new Task(title);
            taskService.saveTask(task);  // 调用 Service
            tasks.add(task);
            titleField.clear();
        }
    }
}
```

```java
// 在 TaskController 中添加（需 import javafx.scene.control.ListCell;）
// 自定义列表单元格
public static class TaskListCell extends ListCell<Task> {
    @Override
    protected void updateItem(Task task, boolean empty) {
        super.updateItem(task, empty);
        if (empty || task == null) {
            setText(null);
            setGraphic(null);
        } else {
            setText(task.getTitle());
            setStyle(task.isCompleted() ? "-fx-text-fill: gray;" : "-fx-text-fill: black;");
        }
    }
}
```

### 1.3 MVC 数据流

```
用户交互 → Controller.handleXxx() → Service/Model 更新
                                        ↓
View ← (绑定/手动刷新) ← Model 状态变化
```

---

## 二、MVVM 模式（Model-View-ViewModel）

MVVM 通过 ViewModel 作为 View 和 Model 之间的中介，利用 JavaFX 强大的数据绑定机制实现 View 与逻辑的彻底解耦。ViewModel 不持有 View 的引用，仅暴露可绑定的 Property。

### 2.1 职责划分

| 组件         | 职责                                                                 |
|--------------|----------------------------------------------------------------------|
| Model        | 纯数据模型与业务实体。                                               |
| ViewModel    | 暴露 View 所需的 Property 和 Command，封装业务逻辑，无 UI 依赖。     |
| View         | FXML + Controller（瘦 Controller），仅负责将 UI 控件绑定到 ViewModel。|

### 2.2 完整代码示例

**Model — `User.java`**

```java
package com.example.mvvm.model;

import javafx.beans.property.*;

public class User {
    private final StringProperty name = new SimpleStringProperty();
    private final IntegerProperty age = new SimpleIntegerProperty();

    public User(String name, int age) {
        setName(name);
        setAge(age);
    }

    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }

    public IntegerProperty ageProperty() { return age; }
    public int getAge() { return age.get(); }
    public void setAge(int value) { age.set(value); }
}
```

**ViewModel — `UserViewModel.java`**

```java
package com.example.mvvm.viewmodel;

import com.example.mvvm.model.User;
import com.example.mvvm.service.UserService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class UserViewModel {

    private final UserService userService = new UserService();

    // 表单输入绑定
    private final StringProperty inputName = new SimpleStringProperty();
    private final IntegerProperty inputAge = new SimpleIntegerProperty();

    // 列表数据
    private final ObservableList<User> users = FXCollections.observableArrayList();

    // 选中项
    private final ObjectProperty<User> selectedUser = new SimpleObjectProperty<>();

    // 计算属性：表单是否有效
    private final BooleanProperty formValid = new SimpleBooleanProperty(false);

    public UserViewModel() {
        // 表单校验：姓名非空且年龄在 0-150 之间
        formValid.bind(Bindings.createBooleanBinding(() -> {
            String name = getInputName();
            int age = getInputAge();
            return name != null && !name.trim().isEmpty() && age >= 0 && age <= 150;
        }, inputName, inputAge));

        // 加载初始数据
        users.addAll(userService.loadAllUsers());
    }

    /** 添加用户命令 */
    public void addUser() {
        if (!formValid.get()) return;
        User user = new User(getInputName().trim(), getInputAge());
        userService.save(user);
        users.add(user);
        inputName.set("");
        inputAge.set(0);
    }

    /** 删除选中用户命令 */
    public void removeSelectedUser() {
        User selected = getSelectedUser();
        if (selected != null) {
            userService.delete(selected);
            users.remove(selected);
        }
    }

    // === Property 访问器 ===
    public StringProperty inputNameProperty() { return inputName; }
    public String getInputName() { return inputName.get(); }

    public IntegerProperty inputAgeProperty() { return inputAge; }
    public int getInputAge() { return inputAge.get(); }

    public ObservableList<User> getUsers() { return users; }

    public ObjectProperty<User> selectedUserProperty() { return selectedUser; }
    public User getSelectedUser() { return selectedUser.get(); }
    public void setSelectedUser(User value) { selectedUser.set(value); }

    public BooleanProperty formValidProperty() { return formValid; }
}
```

**View Controller（瘦 Controller）— `UserViewController.java`**

```java
package com.example.mvvm.view;

import com.example.mvvm.viewmodel.UserViewModel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.net.URL;
import java.util.ResourceBundle;

public class UserViewController implements Initializable {

    @FXML private TextField nameField;
    @FXML private TextField ageField;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> nameCol;
    @FXML private TableColumn<User, Number> ageCol;
    @FXML private Button addButton;
    @FXML private Button removeButton;

    private final UserViewModel viewModel = new UserViewModel();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 双向绑定：UI 控件 ↔ ViewModel Property
        nameField.textProperty().bindBidirectional(viewModel.inputNameProperty());
        ageField.textProperty().bindBidirectional(
            viewModel.inputAgeProperty(), new NumberStringConverter());

        // 表格数据绑定
        userTable.setItems(viewModel.getUsers());
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        ageCol.setCellValueFactory(c -> c.getValue().ageProperty());

        // 选中项绑定
        viewModel.selectedUserProperty()
            .bind(userTable.getSelectionModel().selectedItemProperty());

        // 按钮禁用状态绑定到计算属性
        addButton.disableProperty().bind(viewModel.formValidProperty().not());
        removeButton.disableProperty()
            .bind(viewModel.selectedUserProperty().isNull());

        // 按钮事件委托给 ViewModel 命令
        addButton.setOnAction(e -> viewModel.addUser());
        removeButton.setOnAction(e -> viewModel.removeSelectedUser());
    }
}
```

### 2.3 MVVM 数据流

```
View (FXML + 瘦Controller) ←双向绑定→ ViewModel (Property/Command)
                                            ↓ 调用
                                        Service / Model
```

---

## 三、MVC 与 MVVM 对比

| 对比维度       | MVC                                  | MVVM                                       |
|----------------|--------------------------------------|--------------------------------------------|
| 复杂度         | 较低，结构简单直接                   | 较高，需额外 ViewModel 层                  |
| 可测试性       | 中等，Controller 依赖 JavaFX 控件    | 高，ViewModel 无 UI 依赖，可纯单元测试     |
| 耦合度         | Controller 直接操作 View 控件，耦合较高 | View 与逻辑通过绑定解耦，耦合度低        |
| 数据同步方式   | 手动刷新或单向绑定                   | 双向绑定自动同步                           |
| 代码量         | 较少                                 | 较多（需写 ViewModel 和绑定代码）          |
| 适用场景       | 简单表单、小型工具、快速原型         | 中大型应用、复杂表单、需高可测试性         |
| 学习曲线       | 平缓                                 | 较陡，需理解绑定机制                       |
| 团队协作       | View 与逻辑易混杂                    | 前端/逻辑可并行开发                        |

### 3.1 MVC 与 MVVM 的选择建议

- **小型应用 / 工具类**：使用 MVC，快速直接。
- **中大型应用 / 需要单元测试**：使用 MVVM，ViewModel 可独立测试。
- **混合使用**：简单页面用 MVC，复杂页面用 MVVM，两者可在同一项目中共存。

---

## 四、MVP 模式（Model-View-Presenter）

MVP（Model-View-Presenter）是介于 MVC 与 MVVM 之间的架构模式。它将 UI 逻辑完全从 View 中抽离到 Presenter，但不像 MVVM 那样依赖数据绑定，View 与 Presenter 之间通过接口显式交互。

**两种变体**

- **Passive View（被动视图）**：View 完全被动，所有 UI 状态更新都由 Presenter 通过 View 接口的方法调用完成。View 不包含任何逻辑，甚至连简单的格式化也交给 Presenter。可测试性最高，但 Presenter 代码量较大。
- **Supervising Controller（监督控制器）**：Presenter 负责主要 UI 逻辑，但允许 View 通过简单绑定（如直接属性绑定）处理部分显示逻辑，Presenter 仅干预复杂交互。代码量较少，是实践中更常用的变体。

**何时选择 MVP**

- 需要 MVC 与 MVVM 之间的折中：既想要完整的 UI 逻辑分离（提升可测试性），又不想引入 MVVM 的数据绑定机制（避免绑定链带来的复杂度与内存管理负担）。
- View 逻辑复杂但难以用声明式绑定表达（如依赖大量条件分支、动画协调）。
- 团队对 Presenter 接口风格更熟悉，或需要在不同 UI 框架间复用 Presenter。

**Presenter 示例**

```java
// View 接口：抽象出 View 暴露给 Presenter 的能力
public interface TaskView {
    String getInputText();
    void setTaskList(List<Task> tasks);
    void showEmptyInputError();
}

// Presenter：持有 View 接口引用，不依赖任何 JavaFX 控件，可独立单元测试
public class TaskPresenter {
    private final TaskView view;
    private final TaskService service;

    public TaskPresenter(TaskView view, TaskService service) {
        this.view = view;
        this.service = service;
    }

    public void onAddTask() {
        String title = view.getInputText();
        if (title == null || title.isBlank()) {
            view.showEmptyInputError();
            return;
        }
        Task task = new Task(title);
        service.saveTask(task);
        view.setTaskList(service.loadTasks());  // 通过接口更新 View
    }
}

// Controller 实现 View 接口，仅做 UI 操作，逻辑全部委托 Presenter
public class TaskController implements TaskView {
    private final TaskPresenter presenter;

    public TaskController(TaskService service) {
        this.presenter = new TaskPresenter(this, service);
    }

    @FXML private void handleAddTask() { presenter.onAddTask(); }

    @Override public String getInputText() { return titleField.getText(); }
    @Override public void setTaskList(List<Task> tasks) {
        taskListView.setItems(FXCollections.observableArrayList(tasks));
    }
    @Override public void showEmptyInputError() { statusLabel.setText("标题不能为空"); }
}
```

---

## 五、需避免的反模式（Anti-patterns）

### 5.1 胖控制器（Fat Controller）

**问题**：Controller 中堆积大量业务逻辑、数据访问、校验代码，导致难以维护和测试。

```java
// ❌ 反模式：Controller 直接操作数据库
@FXML
private void handleLogin() {
    String username = usernameField.getText();
    String password = passwordField.getText();
    // 业务逻辑、校验、数据库访问全部塞在 Controller 中
    if (username.length() < 3) { /* ... */ }
    try (Connection conn = DriverManager.getConnection("jdbc:...")) {
        PreparedStatement ps = conn.prepareStatement("SELECT ...");
        // ...
    }
}
```

```java
// ✅ 正确做法：Controller 仅委托给 Service
@FXML
private void handleLogin() {
    boolean success = authService.login(
        usernameField.getText(), passwordField.getText());
    if (success) { showMainView(); }
    else { showError("登录失败"); }
}
```

### 5.2 FXML 中嵌入业务逻辑

**问题**：在 FXML 的 `onAction` 中通过脚本（如 JavaScript）编写逻辑，或在 FXML Controller 中通过复杂内联表达式处理业务。

```xml
<!-- ❌ 反模式：FXML 中嵌入脚本逻辑 -->
<Button text="计算" onAction="#calculate">
    <fx:script>
        var result = parseInt(a) + parseInt(b);
        label.setText(result);
    </fx:script>
</Button>
```

FXML 应保持纯声明式，所有逻辑放到 Controller 或 ViewModel 中。

### 5.3 紧耦合（Tight Coupling）

**问题**：Controller 直接 `new` 具体依赖类，导致无法替换和测试。

```java
// ❌ 反模式：硬编码依赖
public class OrderController {
    private MySQLDatabase db = new MySQLDatabase();  // 紧耦合
    private EmailService email = new SmtpEmailService();
}
```

```java
// ✅ 正确做法：通过接口 + 构造注入
public class OrderController {
    private final Database db;
    private final EmailService email;

    public OrderController(Database db, EmailService email) {
        this.db = db;
        this.email = email;
    }
}
```

### 5.4 其他常见反模式

| 反模式                     | 说明与改进                                                         |
|----------------------------|--------------------------------------------------------------------|
| God Controller             | 单个 Controller 管理所有功能，应拆分为多个 Controller。            |
| 在 UI 线程执行耗时操作     | 阻塞 JavaFX Application Thread 导致界面卡死，应使用 `Task` + 后台线程。|
| 直接暴露内部集合           | 应返回不可修改视图或使用 Property 封装。                           |
| 忽略资源释放               | 未移除监听器导致内存泄漏，参见数据绑定指南。                       |

---

## 六、服务层设计（Service Layer）

服务层封装业务逻辑与数据访问，作为 Controller/ViewModel 与数据层之间的中间层，保持 UI 层的简洁与可测试性。

### 6.1 服务层职责

- 封装业务规则与事务边界。
- 协调多个 Repository / DAO。
- 提供 UI 无关的 API，返回领域对象。
- 处理异常并转换为业务语义。

### 6.2 服务层实现示例

```java
package com.example.service;

import com.example.model.Task;
import com.example.repository.TaskRepository;

import java.util.List;

public class TaskService {

    private final TaskRepository repository;

    // 通过构造注入 Repository（接口）
    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    /** 加载所有任务 */
    public List<Task> loadTasks() {
        return repository.findAll();
    }

    /** 保存任务（含业务校验） */
    public void saveTask(Task task) {
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new IllegalArgumentException("任务标题不能为空");
        }
        repository.save(task);
    }

    /** 切换任务完成状态 */
    public void toggleComplete(Task task) {
        task.setCompleted(!task.isCompleted());
        repository.update(task);
    }

    /** 删除任务 */
    public void deleteTask(Task task) {
        repository.delete(task);
    }
}
```

### 6.3 异步服务调用

耗时服务调用应在后台线程执行，避免阻塞 UI 线程：

```java
@FXML
private void handleLoadTasks() {
    Task<List<Task>> loadTask = new Task<>() {
        @Override
        protected List<Task> call() {
            return taskService.loadTasks();  // 后台执行
        }
    };
    loadTask.setOnSucceeded(e -> {
        tasks.setAll(loadTask.getValue());  // 回到 UI 线程更新
        statusLabel.setText("加载完成");
    });
    loadTask.setOnFailed(e -> {
        statusLabel.setText("加载失败: " + loadTask.getException().getMessage());
    });
    new Thread(loadTask).start();
}
```

---

## 七、依赖注入（Dependency Injection）

依赖注入用于解耦组件之间的依赖关系，便于测试和替换实现。JavaFX 中有三种常见方案。

### 7.1 手动依赖注入

适用于小型项目，通过工厂或构造方法手动传递依赖。

```java
public class AppFactory {

    public static MainController createMainController() {
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo);
        return new MainController(service);
    }
}

// 在 Application.start() 中
FXMLLoader loader = new FXMLLoader(url);
loader.setControllerFactory(c -> AppFactory.createMainController());
Parent root = loader.load();
```

### 7.2 Guice 依赖注入

Google Guice 是轻量级 DI 框架，适合中小型 JavaFX 项目。

**Maven 依赖：**

```xml
<dependency>
    <groupId>com.google.inject</groupId>
    <artifactId>guice</artifactId>
    <version>7.0.0</version>
</dependency>
```

**配置与使用：**

```java
public class AppModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(TaskRepository.class).to(InMemoryTaskRepository.class);
        bind(TaskService.class).to(TaskServiceImpl.class);
    }
}

// 自定义 ControllerFactory，让 FXMLLoader 使用 Guice 创建 Controller
public class GuiceControllerFactory implements Callback<Class<?>, Object> {
    private final Injector injector;

    public GuiceControllerFactory(Injector injector) {
        this.injector = injector;
    }

    @Override
    public Object call(Class<?> type) {
        return injector.getInstance(type);
    }
}

// Application 启动
public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Injector injector = Guice.createInjector(new AppModule());
        FXMLLoader loader = new FXMLLoader(url);
        loader.setControllerFactory(new GuiceControllerFactory(injector));
        Scene scene = new Scene(loader.load());
        stage.setScene(scene);
        stage.show();
    }
}
```

**Controller 中使用注入：**

```java
public class MainController implements Initializable {
    private final TaskService taskService;

    @Inject  // Guice 构造注入
    public MainController(TaskService taskService) {
        this.taskService = taskService;
    }
    // ...
}
```

> 注意：使用 Guice 时，`module-info.java` 需 `opens` Controller 包给 Guice 模块以支持反射。

### 7.3 Spring Framework 依赖注入

Spring Boot 可与 JavaFX 结合，适合已有 Spring 生态的项目。

> **⚠️ 关键陷阱：主类不能直接继承 Application**
>
> 当主类（包含 `main` 方法的类）直接 `extends Application` 时，JVM 会尝试使用 JavaFX 专用的启动器运行。在 classpath 模式下（无 `module-info.java`，通过 `mvn spring-boot:run` 或 `java -jar` 运行），JavaFX 启动器找不到 `javafx.graphics` 模块，会报错：
> ```
> Error: JavaFX runtime components are missing, and are required to run this application
> ```
> **解决方案**：将启动类拆分为两个类 —— Spring Boot 启动类（不继承 Application）+ JavaFX 入口类（继承 Application）。详见 `references/spring-boot-integration.md`。

**Maven 依赖：**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>3.5.0</version>
</dependency>
```

**Spring Boot + JavaFX 集成（正确写法：拆分两个类）：**

```java
// 1. Spring Boot 启动类：不继承 Application，走普通 JVM 启动流程
@SpringBootApplication
public class MyApp {

    static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        // 先启动 Spring 容器
        springContext = SpringApplication.run(MyApp.class, args);
        // 再启动 JavaFX（委托给 JavaFX 入口类）
        Application.launch(JavaFXApp.class, args);
    }
}
```

```java
// 2. JavaFX 入口类：继承 Application，负责 UI 加载
public class JavaFXApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        // 通过 controllerFactory 从 Spring 容器获取 Controller Bean
        loader.setControllerFactory(MyApp.springContext::getBean);
        Scene scene = new Scene(loader.load());
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        MyApp.springContext.close();
    }
}
```

**Spring 管理的 Controller：**

```java
@Component
public class MainController implements Initializable {
    private final TaskService taskService;

    // Spring 构造注入（无需 @Autowired 注解，Spring 4.3+ 单构造方法自动注入）
    public MainController(TaskService taskService) {
        this.taskService = taskService;
    }
    // ...
}
```

### 7.4 三种 DI 方案对比

| 方案   | 复杂度 | 适用场景                       | 优点                     | 缺点                     |
|--------|--------|--------------------------------|--------------------------|--------------------------|
| 手动   | 低     | 小型项目、原型                 | 无额外依赖、简单直接     | 手动维护依赖关系、易出错 |
| Guice  | 中     | 中型项目、需轻量 DI            | 轻量、API 简洁、启动快   | 生态不如 Spring 丰富     |
| Spring | 高     | 大型企业应用、已有 Spring 生态| 功能全面、生态强大       | 较重、启动较慢           |

---

## 八、事件总线模式（Event Bus）

事件总线用于实现组件间的松耦合通信，特别适合多个不直接关联的模块需要响应同一事件的场景（如：用户登录后刷新多个视图）。

### 8.1 简单事件总线实现

```java
package com.example.eventbus;

import javafx.application.Platform;
import java.util.*;
import java.util.concurrent.*;

/**
 * 轻量级事件总线，支持注册/注销监听器并发布事件。
 * 事件分发在 JavaFX Application Thread 上执行。
 */
public class EventBus {

    private static final EventBus INSTANCE = new EventBus();
    public static EventBus getInstance() { return INSTANCE; }

    private final Map<Class<?>, List<EventListener<?>>> listeners =
        new ConcurrentHashMap<>();

    /** 注册监听器 */
    public <T> void subscribe(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                 .add(listener);
    }

    /** 注销监听器 */
    public <T> void unsubscribe(Class<T> eventType, EventListener<T> listener) {
        List<EventListener<?>> list = listeners.get(eventType);
        if (list != null) {
            list.remove(listener);
        }
    }

    /** 发布事件（在 JavaFX Application Thread 上通知） */
    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        List<EventListener<?>> list = listeners.get(event.getClass());
        if (list != null) {
            for (EventListener<?> listener : list) {
                Platform.runLater(() -> ((EventListener<T>) listener).onEvent(event));
            }
        }
    }

    @FunctionalInterface
    public interface EventListener<T> {
        void onEvent(T event);
    }
}
```

### 8.2 定义事件

```java
package com.example.event;

// 用户登录事件
public record UserLoggedInEvent(String username, long timestamp) {}

// 任务完成事件
public record TaskCompletedEvent(Long taskId) {}

// 数据刷新事件
public record DataRefreshEvent(String source) {}
```

### 8.3 使用示例

```java
public class HeaderController implements Initializable {
    private final EventBus eventBus = EventBus.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 订阅事件
        eventBus.subscribe(UserLoggedInEvent.class, this::onUserLoggedIn);
    }

    private void onUserLoggedIn(UserLoggedInEvent event) {
        welcomeLabel.setText("欢迎, " + event.username());
    }

    @FXML
    private void handleLogin() {
        // ... 登录逻辑 ...
        eventBus.publish(new UserLoggedInEvent(username, System.currentTimeMillis()));
    }
}

public class SidebarController implements Initializable {
    private final EventBus eventBus = EventBus.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 另一个控制器独立订阅同一事件
        eventBus.subscribe(UserLoggedInEvent.class, e -> {
            loadUserMenus(e.username());
        });
    }
}
```

### 8.4 事件总线使用注意事项

1. **及时注销监听器**：Controller 销毁时调用 `unsubscribe`，防止内存泄漏。
2. **线程安全**：上述实现使用 `CopyOnWriteArrayList` 保证并发安全；若事件处理涉及 UI 更新，应使用 `Platform.runLater()` 切换到 UI 线程。
3. **避免循环事件**：事件处理中发布新事件可能导致无限循环，需谨慎设计。
4. **成熟方案**：生产环境可考虑使用 Google Guava 的 `EventBus` 或 Apache DeltaSpike 的事件机制。

### 8.5 Guava EventBus 集成示例

```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>33.2.1-jre</version>
</dependency>
```

```java
// 创建事件总线（可配置为异步）
EventBus eventBus = new EventBus("javafx-app");

// 订阅：使用 @Subscribe 注解
public class DashboardController {
    @Subscribe
    public void onUserLoggedIn(UserLoggedInEvent event) {
        Platform.runLater(() -> updateDashboard(event.username()));
    }
}

// 注册与注销
eventBus.register(dashboardController);
// eventBus.unregister(dashboardController); // 销毁时注销

// 发布事件
eventBus.post(new UserLoggedInEvent("admin", System.currentTimeMillis()));
```

---

## 九、架构模式选择决策树

```
应用规模？
├── 小型（< 10 个页面，简单逻辑）
│   └── MVC + 手动依赖注入
├── 中型（10-50 个页面，中等复杂度）
│   └── MVVM + Guice + 事件总线
└── 大型（> 50 个页面，复杂业务）
    └── MVVM + Spring + 事件总线 + 服务层
```

### 通用架构原则

1. **单一职责**：每个类只做一件事（Controller 管交互，Service 管业务，Repository 管数据）。
2. **依赖倒置**：依赖接口而非具体实现，通过 DI 注入。
3. **UI 线程纯净**：耗时操作放后台线程，UI 更新回到 Application Thread。
4. **可测试性**：业务逻辑与 UI 解耦，使 ViewModel/Service 可独立单元测试。
5. **渐进式架构**：从简单模式起步，随复杂度增长逐步引入更高级模式。
