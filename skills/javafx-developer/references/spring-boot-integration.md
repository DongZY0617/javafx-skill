# Spring Boot + JavaFX 整合指南

本指南详细介绍 Spring Boot 与 JavaFX 的整合方案，涵盖启动类设计、依赖注入、配置管理、持久层整合以及常见陷阱。这是实际开发中需求量最大的 JavaFX 整合场景之一。

---

## 一、为什么需要 Spring Boot

纯 JavaFX 应用中，Controller 通常通过 `new` 创建，依赖关系手动维护，难以管理复杂业务。引入 Spring Boot 后可获得：

- **依赖注入**：Controller、Service、Repository 之间的依赖由 Spring 容器管理
- **事务管理**：通过 `@Transactional` 声明式管理数据库事务
- **自动配置**：数据源、MyBatis/JPA 等组件开箱即用
- **生态兼容**：可直接使用 Spring Data、Spring Security 等生态组件

---

## 二、三种整合方式对比

| 方式 | 原理 | 优点 | 缺点 | 推荐场景 |
|------|------|------|------|----------|
| **手动整合** | 主类启动 Spring 后调用 `Application.launch()` | 零额外依赖、灵活可控、小白友好 | 需手动管理 ControllerFactory | 通用场景（推荐） |
| **javafx-weaver** | net.rgielen:javafx-weaver 自动注入 Controller | 自动化程度高、社区活跃 | 额外依赖、学习成本 | 中大型项目 |
| **spring-boot-javafx-support** | org.bsc.javafx:javafx-spring-boot-starter | 配置简单 | 维护不活跃、版本滞后 | 快速原型 |

> **本技能默认推荐手动整合方式**：无需额外依赖，代码直观，便于小白上手。

---

## 三、启动类拆分原则（最关键的知识点）

### 3.1 问题：主类直接继承 Application

**❌ 错误写法**（会导致启动失败）：

```java
@SpringBootApplication
public class MyApp extends Application {  // 主类直接继承 Application

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        springContext = SpringApplication.run(MyApp.class);
    }

    @Override
    public void start(Stage stage) throws IOException {
        // ...
    }

    public static void main(String[] args) {
        launch(args);  // 直接调用 launch()
    }
}
```

**报错信息**：
```
Error: JavaFX runtime components are missing, and are required to run this application
```

### 3.2 原因分析

当 JVM 检测到主类（包含 `public static void main` 的类）是 `Application` 的子类时，会使用 JavaFX 专用的启动器（`com.sun.javafx.application.LauncherImpl`）来运行。该启动器要求 `javafx.graphics` 作为**命名模块**存在于模块路径（module path）上。

在 classpath 模式下（无 `module-info.java`，通过 `mvn spring-boot:run` 或 `java -jar` 运行），JavaFX JAR 作为自动模块放在 classpath 上，JavaFX 启动器无法找到它们，于是报错。

> 注意：纯 JavaFX 项目使用 `javafx-maven-plugin` 的 `mvn javafx:run` 运行时不会触发此错误，因为插件会将 JavaFX JAR 放到模块路径上。但与 Spring Boot 整合时通常使用 `mvn spring-boot:run`，走的是 classpath 模式。

### 3.3 解决方案：拆分为两个类

**✅ 正确写法**：

```java
// 1. Spring Boot 启动类：不继承 Application，走普通 JVM 启动流程
@SpringBootApplication
public class MyApp {

    static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        // 第一步：启动 Spring 容器
        springContext = SpringApplication.run(MyApp.class, args);
        // 第二步：启动 JavaFX（委托给 JavaFX 入口类）
        Application.launch(JavaFXApp.class, args);
    }
}
```

```java
// 2. JavaFX 入口类：继承 Application，负责 UI 加载
public class JavaFXApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
        // 通过 controllerFactory 从 Spring 容器获取 Controller Bean
        loader.setControllerFactory(MyApp.springContext::getBean);

        Scene scene = new Scene(loader.load(), 1024, 768);
        scene.getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm());

        stage.setTitle("My Application");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        // 关闭 JavaFX 后释放 Spring 容器
        MyApp.springContext.close();
    }
}
```

**关键要点**：
1. `MyApp` 不继承 `Application`，JVM 使用普通启动流程，不触发 JavaFX 启动器检查
2. `springContext` 声明为 `static`，供 `JavaFXApp` 访问
3. `Application.launch(JavaFXApp.class, args)` 指定 JavaFX 入口类
4. `stop()` 中关闭 Spring 容器，确保资源释放

---

## 四、Controller 注入机制

### 4.1 controllerFactory 原理

FXMLLoader 默认通过 `Class.newInstance()` 创建 Controller（无参构造）。设置 `controllerFactory` 后，FXMLLoader 会将 Controller 类名传递给工厂方法，由工厂决定如何实例化。

```java
loader.setControllerFactory(springContext::getBean);
```

这行代码等价于：

```java
loader.setControllerFactory(controllerClass -> springContext.getBean(controllerClass));
```

当 FXMLLoader 解析到 `fx:controller="com.example.controller.UserController"` 时，会调用 `springContext.getBean(UserController.class)`，从 Spring 容器获取已注入依赖的 Controller 实例。

### 4.2 Controller 必须注册为 Spring Bean

```java
@Component  // 必须标注 @Component（或 @Controller 等 Spring 构造型注解）
public class UserController implements Initializable {

    private final UserService userService;  // 通过构造注入

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化逻辑
    }
}
```

> **注意**：Spring 4.3+ 单构造方法自动注入，无需 `@Autowired` 注解。

### 4.3 对话框中的 Controller 注入

打开对话框时也需要设置 controllerFactory：

```java
private boolean showUserDialog(User user) {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user-dialog.fxml"));
    loader.setControllerFactory(springContext::getBean);  // 同样需要设置
    Parent root = loader.load();

    UserDialogController controller = loader.getController();
    controller.setUser(user);

    Stage dialogStage = new Stage();
    dialogStage.initModality(Modality.APPLICATION_MODAL);
    dialogStage.setScene(new Scene(root));
    controller.setDialogStage(dialogStage);
    dialogStage.showAndWait();

    return controller.isSaved();
}
```

> **陷阱**：如果对话框 FXMLLoader 未设置 controllerFactory，会报 `NoSuchBeanException` 或 Controller 中注入的 Service 为 null。

---

## 五、完整 pom.xml 配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>myapp</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
        <javafx.version>21.0.11</javafx.version>
    </properties>

    <dependencies>
        <!-- Spring Boot 核心（不含 Web 服务器） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <!-- JavaFX Controls -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <version>${javafx.version}</version>
        </dependency>

        <!-- JavaFX FXML -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-fxml</artifactId>
            <version>${javafx.version}</version>
        </dependency>

        <!-- JavaFX Graphics（含平台本地库） -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-graphics</artifactId>
            <version>${javafx.version}</version>
        </dependency>

        <!-- Lombok（可选） -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 关键说明

1. **使用 `spring-boot-starter` 而非 `spring-boot-starter-web`**：桌面应用不需要 Web 服务器。
2. **不使用 `javafx-maven-plugin`**：运行命令用 `mvn spring-boot:run` 而非 `mvn javafx:run`。
3. **不创建 `module-info.java`**：采用 classpath 方式运行，避免模块化带来的复杂性。
4. **Lombok 注解处理器**：Spring Boot parent 已管理 Lombok 依赖，无需在 `maven-compiler-plugin` 中单独配置 `annotationProcessorPaths`。

---

## 六、application.yml 配置

```yaml
spring:
  # 不启动 Web 服务器（桌面应用）
  main:
    web-application-type: none
    banner-mode: off
  # 数据源配置（以 SQLite 为例）
  datasource:
    url: jdbc:sqlite:data/app.db
    driver-class-name: org.sqlite.JDBC
    username: sa
    password: ""
  # 启动时自动执行建表脚本
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql

# MyBatis 配置（如使用）
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.model
  configuration:
    map-underscore-to-camel-case: true

logging:
  level:
    com.example: DEBUG
```

### 关键配置说明

| 配置项 | 作用 | 说明 |
|--------|------|------|
| `spring.main.web-application-type=none` | 禁用 Web 服务器 | 桌面应用不需要 Tomcat/Netty |
| `spring.main.banner-mode=off` | 关闭 Spring Banner | 可选，减少控制台噪音 |
| `spring.sql.init.mode=always` | 启动时执行 SQL 脚本 | 用于自动建表和初始化数据 |

---

## 七、完整项目结构

```
myapp/
├── pom.xml
├── data/                           # SQLite 数据库文件目录
│   └── app.db
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/
│       │       ├── MyApp.java              # Spring Boot 启动类（不继承 Application）
│       │       ├── JavaFXApp.java          # JavaFX 入口类（继承 Application）
│       │       ├── controller/
│       │       │   ├── MainController.java # 主界面控制器（@Component）
│       │       │   └── DialogController.java
│       │       ├── service/
│       │       │   ├── UserService.java
│       │       │   └── impl/
│       │       │       └── UserServiceImpl.java
│       │       ├── mapper/
│       │       │   └── UserMapper.java     # MyBatis Mapper（@Mapper）
│       │       └── model/
│       │           └── User.java           # 实体类（JavaFX Properties）
│       └── resources/
│           ├── application.yml             # Spring Boot 配置
│           ├── schema.sql                  # 建表脚本
│           ├── fxml/
│           │   ├── main-view.fxml
│           │   └── user-dialog.fxml
│           ├── css/
│           │   ├── light-theme.css
│           │   └── dark-theme.css
│           └── mapper/
│               └── UserMapper.xml          # MyBatis XML 映射
└── target/                         # 构建输出
```

> **注意**：不创建 `module-info.java`，采用 classpath 方式运行。

---

## 八、与持久层框架整合

### 8.1 MyBatis 整合

**依赖**：
```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.4</version>
</dependency>
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.47.0.0</version>
</dependency>
```

**Mapper 接口**：
```java
@Mapper
public interface UserMapper {
    List<User> findPage(@Param("keyword") String keyword,
                        @Param("offset") int offset,
                        @Param("limit") int limit);
    int insert(User user);
    int update(User user);
    int deleteById(@Param("id") Long id);
}
```

**Service 层事务**：
```java
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Transactional
    public User save(User user) {
        if (user.getId() == 0) {
            userMapper.insert(user);
        } else {
            userMapper.update(user);
        }
        return user;
    }
}
```

### 8.2 JavaFX Properties 与 MyBatis 兼容性

JavaFX Properties 的 getter/setter 可被 MyBatis 正确映射，但需注意基本类型 Property 的 null 处理：

```java
public class User {
    private final LongProperty id = new SimpleLongProperty();

    public long getId() { return id.get(); }

    // MyBatis 注入时可能传入 null（新增场景），需做安全处理
    public void setId(Long id) {
        this.id.set(id == null ? 0L : id);
    }

    public LongProperty idProperty() { return id; }
}
```

> **陷阱**：`SimpleLongProperty.set(long)` 接收基本类型 `long`，传入 `null` 会抛 `NullPointerException`。`IntegerProperty`、`BooleanProperty` 同理。必须在 setter 中处理 null。

---

## 九、运行与打包

### 9.1 开发运行

```bash
# 使用 Spring Boot Maven 插件运行（推荐）
mvn spring-boot:run

# 或先编译再运行
mvn clean compile
mvn spring-boot:run
```

> **不要使用** `mvn javafx:run`，因为项目未配置 `javafx-maven-plugin`，且 Spring Boot 容器需要通过 `spring-boot:run` 启动。

### 9.2 打包为可执行 JAR

```bash
mvn clean package
java -jar target/myapp-1.0.0.jar
```

### 9.3 打包为原生安装包

使用 jpackage 创建平台原生安装包：

```bash
# 先构建 JAR
mvn clean package

# 创建 Windows 安装包
jpackage \
  --type exe \
  --name "MyApp" \
  --app-version 1.0.0 \
  --input target \
  --main-jar myapp-1.0.0.jar \
  --main-class com.example.MyApp \
  --win-menu \
  --win-shortcut
```

> **注意**：打包时 `--main-class` 指向 Spring Boot 启动类（`MyApp`），而非 JavaFX 入口类。

---

## 十、常见陷阱清单

### 陷阱 1：主类直接继承 Application（最常见）

**现象**：`Error: JavaFX runtime components are missing`

**原因**：主类 extends Application，JVM 使用 JavaFX 启动器，classpath 模式下找不到 JavaFX 模块。

**解决**：拆分为启动类 + JavaFX 入口类（见第三节）。

### 陷阱 2：对话框 Controller 注入失败

**现象**：打开对话框时 Controller 中 Service 为 null，或抛 `NoSuchBeanException`。

**原因**：对话框的 FXMLLoader 未设置 `controllerFactory`。

**解决**：所有 FXMLLoader 都需设置 `loader.setControllerFactory(springContext::getBean)`。

### 陷阱 3：Controller 未标注 @Component

**现象**：`NoSuchBeanDefinitionException: No qualifying bean of type 'UserController'`。

**原因**：Controller 类未标注 Spring 构造型注解，Spring 容器中不存在该 Bean。

**解决**：在 Controller 类上添加 `@Component` 注解。

### 陷阱 4：JavaFX Properties 的 null 处理

**现象**：新增记录时抛 `NullPointerException`，MyBatis 注入 null 值到基本类型 Property。

**原因**：`SimpleLongProperty.set(long)` 等方法接收基本类型，不接受 null。

**解决**：在 setter 方法中将 null 转换为默认值（见第 8.2 节）。

### 陷阱 5：忘记关闭 Spring 容器

**现象**：应用窗口关闭后进程不退出，或数据库连接未释放。

**原因**：JavaFX 关闭后 Spring 容器仍在运行。

**解决**：在 `JavaFXApp.stop()` 中调用 `springContext.close()`。

### 陷阱 6：在 UI 线程执行耗时数据库操作

**现象**：界面卡顿、无响应。

**原因**：MyBatis 查询在 JavaFX Application Thread 上同步执行。

**解决**：耗时查询使用 `Task` + 后台线程，结果通过 `Platform.runLater()` 回到 UI 线程：

```java
Task<List<User>> loadTask = new Task<>() {
    @Override
    protected List<User> call() {
        return userService.findPage(keyword, page, pageSize);  // 后台执行
    }
};
loadTask.setOnSucceeded(e -> {
    userTable.getItems().setAll(loadTask.getValue());  // UI 线程更新
});
new Thread(loadTask).start();
```

### 陷阱 7：Lombok 注解处理器配置冲突

**现象**：Maven 编译报 `No processor claimed any of these annotations` 或 Lombok 注解不生效。

**原因**：在 `maven-compiler-plugin` 中手动配置了 `annotationProcessorPaths` 但未指定 Lombok version。

**解决**：Spring Boot parent 已管理 Lombok 的注解处理器，无需在 `maven-compiler-plugin` 中单独配置 `annotationProcessorPaths`。移除该配置即可。

### 陷阱 8：IDE 中 Lombok 与 JDK 版本不匹配

**现象**：在 IntelliJ IDEA 中编译或运行时报以下错误，而命令行 `mvn compile` 正常：
```
java: java.lang.ExceptionInInitializerError
com.sun.tools.javac.code.TypeTag :: UNKNOWN
```

**原因**：IDE 中选择的 Project JDK 与 `pom.xml` 中配置的 `java.version` 不一致。Lombok 通过反射访问 JDK 编译器内部 API（`TypeTag`），当 IDE 使用的 JDK 版本与 Lombok 插件不兼容时触发此错误。

**解决**：
1. 确保 IDE 的 Project SDK（File → Project Structure → Project SDK）与 `pom.xml` 中的 `java.version` 一致
2. 更新 IDE 的 Lombok 插件到最新版本（Settings → Plugins → Lombok）
3. 检查 Maven Importing 使用的 JDK（Settings → Build → Maven → Importing → JDK for importer）

> **提示**：Lombok 1.18.30+ 支持 JDK 21，但 IDE 的 Lombok 插件可能未同步更新。确保 IDE 的 JDK 选择、Lombok 插件版本、`pom.xml` 中的 `java.version` 三者一致。

### 陷阱 9：Controller 单例导致状态污染

**现象**：打开多个相同类型的窗口或对话框时，前一个窗口的输入数据"泄漏"到后一个窗口，或 Controller 中的 UI 控件状态混乱。

**原因**：Spring 默认将所有 `@Component` 注册为**单例**（singleton scope）。但 JavaFX 中每次 `FXMLLoader.load()` 都期望获得一个**全新的** Controller 实例。当 Spring 返回同一个单例 Bean 时，`@FXML` 字段会被新 FXML 的控件覆盖，旧控件引用丢失，导致状态污染和内存泄漏。

**解决**：在 Controller 类上添加 `@Scope("prototype")`，确保每次从 Spring 容器获取时都创建新实例：

```java
@Component
@Scope("prototype")  // 每次注入创建新实例
public class UserDialogController implements Initializable {
    // ...
}
```

> **注意**：所有可通过 `FXMLLoader` 多次加载的 Controller 都应使用 `prototype` 作用域。主窗口 Controller 如果只有一个实例可保持单例，但仍需注意不要在 Controller 中缓存 UI 状态。

### 陷阱 10：spring-boot-devtools 导致 JavaFX 异常重启

**现象**：开发时修改代码后，JavaFX 窗口突然关闭或出现重复窗口、`Stage already showing` 等异常。

**原因**：`spring-boot-devtools` 的自动重启机制监听 classpath 变化，文件变更时触发 Spring 容器重启。但 JavaFX 的 `Application` 生命周期与 Spring 容器不同步，容器重启不会重新调用 `Application.launch()`，导致状态不一致。

**解决**：
1. **排除 devtools 依赖**（推荐）：在 `pom.xml` 中将 devtools 设为 `optional` 或直接移除。
2. **禁用自动重启**：在 `application.yml` 中配置 `spring.devtools.restart.enabled: false`。
3. **仅禁用 JavaFX 类的触发**：在 `application.yml` 中配置 `spring.devtools.restart.exclude: static/**,public/**`（排除资源目录）。

```xml
<!-- 如必须使用 devtools，设为 optional -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

### 陷阱 11：升级 JavaFX 24+ 后启动失败

**现象**：将 JavaFX 版本从 21 升级到 24+ 后，`mvn spring-boot:run` 启动报 `IllegalCallerException`。

**原因**：JavaFX 24+ 的图形渲染层通过 JNI 访问本地代码，在 JDK 24+ 的严格模块封装下需要 `--enable-native-access=javafx.graphics`。但 Spring Boot 的 `spring-boot-maven-plugin` 默认不传递此参数。

**解决**：在 `spring-boot-maven-plugin` 配置中添加 JVM 参数：

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <jvmArguments>--enable-native-access=javafx.graphics</jvmArguments>
    </configuration>
</plugin>
```

---

## 十一、javafx-weaver 替代方案

如果项目规模较大，需要更自动化的 Controller 注入，可使用 [javafx-weaver](https://github.com/rgielen/javafx-weaver)。

**依赖**：
```xml
<dependency>
    <groupId>net.rgielen</groupId>
    <artifactId>javafx-weaver-spring-boot-starter</artifactId>
    <version>2.0.1</version>
</dependency>
```

**使用**：
```java
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}

@Component
public class MainController implements FxController {
    private final FxWeaver fxWeaver;

    public MainController(FxWeaver fxWeaver) {
        this.fxWeaver = fxWeaver;
    }

    public void showView() {
        fxWeaver.loadView(MainController.class);  // 自动加载 FXML 并注入 Controller
    }
}
```

> 注意：javafx-weaver 仍然需要拆分启动类，不能让主类直接继承 Application。

---

## 十二、整合方式选择决策树

```
项目规模与需求？
├── 小型项目 / 快速原型 / 小白上手
│   └── 手动整合（本指南推荐方案）
├── 中型项目 / 需要自动化 Controller 注入
│   └── javafx-weaver
└── 已有 spring-boot-javafx-support 遗留项目
    └── 保持现状或迁移到手动整合
```
