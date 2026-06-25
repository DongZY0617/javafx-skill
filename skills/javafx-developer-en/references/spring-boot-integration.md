# Spring Boot + JavaFX Integration Guide

This guide provides a detailed introduction to integrating Spring Boot with JavaFX, covering startup class design, dependency injection, configuration management, persistence layer integration, and common pitfalls. This is one of the most in-demand JavaFX integration scenarios in real-world development.

---

## 1. Why Spring Boot

In pure JavaFX applications, Controllers are typically created via `new`, and dependencies are managed manually, making complex business logic difficult to handle. By introducing Spring Boot, you gain:

- **Dependency Injection**: Dependencies between Controllers, Services, and Repositories are managed by the Spring container
- **Transaction Management**: Declarative database transaction management via `@Transactional`
- **Auto-configuration**: Components like data sources, MyBatis/JPA work out of the box
- **Ecosystem Compatibility**: Directly use ecosystem components like Spring Data, Spring Security, etc.

---

## 2. Comparison of Three Integration Approaches

| Approach | Principle | Pros | Cons | Recommended Scenario |
|----------|-----------|------|------|----------------------|
| **Manual Integration** | Main class starts Spring then calls `Application.launch()` | Zero extra dependencies, flexible and controllable, beginner-friendly | Requires manual ControllerFactory management | General use (recommended) |
| **javafx-weaver** | net.rgielen:javafx-weaver auto-injects Controllers | High degree of automation, active community | Extra dependency, learning curve | Medium to large projects |
| **spring-boot-javafx-support** | org.bsc.javafx:javafx-spring-boot-starter | Simple configuration | Inactive maintenance, lagging versions | Quick prototyping |

> **This skill recommends the manual integration approach by default**: No extra dependencies needed, intuitive code, easy for beginners to get started.

---

## 3. Startup Class Splitting Principle (The Most Critical Knowledge Point)

### 3.1 The Problem: Main Class Directly Extends Application

**Wrong approach** (will cause startup failure):

```java
@SpringBootApplication
public class MyApp extends Application {  // Main class directly extends Application

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
        launch(args);  // Directly call launch()
    }
}
```

**Error message**:
```
Error: JavaFX runtime components are missing, and are required to run this application
```

### 3.2 Cause Analysis

When the JVM detects that the main class (the class containing `public static void main`) is a subclass of `Application`, it uses the JavaFX-specific launcher (`com.sun.javafx.application.LauncherImpl`) to run it. This launcher requires `javafx.graphics` to exist as a **named module** on the module path.

In classpath mode (without `module-info.java`, running via `mvn spring-boot:run` or `java -jar`), the JavaFX JARs are placed on the classpath as automatic modules, and the JavaFX launcher cannot find them, resulting in an error.

> Note: Pure JavaFX projects running with `mvn javafx:run` via `javafx-maven-plugin` do not trigger this error because the plugin places the JavaFX JARs on the module path. However, when integrating with Spring Boot, `mvn spring-boot:run` is typically used, which goes through classpath mode.

### 3.3 Solution: Split into Two Classes

**Correct approach**:

```java
// 1. Spring Boot startup class: does not extend Application, uses normal JVM startup flow
@SpringBootApplication
public class MyApp {

    static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        // Step 1: Start the Spring container
        springContext = SpringApplication.run(MyApp.class, args);
        // Step 2: Start JavaFX (delegated to the JavaFX entry class)
        Application.launch(JavaFXApp.class, args);
    }
}
```

```java
// 2. JavaFX entry class: extends Application, responsible for UI loading
public class JavaFXApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
        // Obtain Controller Bean from the Spring container via controllerFactory
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
        // Release the Spring container after JavaFX shuts down
        MyApp.springContext.close();
    }
}
```

**Key points**:
1. `MyApp` does not extend `Application`, so the JVM uses the normal startup flow and does not trigger the JavaFX launcher check
2. `springContext` is declared as `static` for `JavaFXApp` to access
3. `Application.launch(JavaFXApp.class, args)` specifies the JavaFX entry class
4. Close the Spring container in `stop()` to ensure resource release

---

## 4. Controller Injection Mechanism

### 4.1 controllerFactory Principle

FXMLLoader creates Controllers via `Class.newInstance()` by default (no-arg constructor). After setting `controllerFactory`, FXMLLoader passes the Controller class name to the factory method, and the factory decides how to instantiate it.

```java
loader.setControllerFactory(springContext::getBean);
```

This line of code is equivalent to:

```java
loader.setControllerFactory(controllerClass -> springContext.getBean(controllerClass));
```

When FXMLLoader parses `fx:controller="com.example.controller.UserController"`, it calls `springContext.getBean(UserController.class)` to obtain a Controller instance with injected dependencies from the Spring container.

### 4.2 Controllers Must Be Registered as Spring Beans

```java
@Component  // Must be annotated with @Component (or @Controller, etc., a Spring stereotype annotation)
public class UserController implements Initializable {

    private final UserService userService;  // Injected via constructor

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialization logic
    }
}
```

> **Note**: Spring 4.3+ supports automatic injection for single-constructor classes; no `@Autowired` annotation needed.

### 4.3 Controller Injection in Dialogs

You also need to set the controllerFactory when opening dialogs:

```java
private boolean showUserDialog(User user) {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user-dialog.fxml"));
    loader.setControllerFactory(springContext::getBean);  // Also needs to be set
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

> **Pitfall**: If the dialog FXMLLoader does not have the controllerFactory set, it will throw `NoSuchBeanException` or the Service injected in the Controller will be null.

---

## 5. Complete pom.xml Configuration

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
        <!-- Spring Boot core (without web server) -->
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

        <!-- JavaFX Graphics (includes platform native libraries) -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-graphics</artifactId>
            <version>${javafx.version}</version>
        </dependency>

        <!-- Lombok (optional) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
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

### Key Notes

1. **Use `spring-boot-starter` instead of `spring-boot-starter-web`**: Desktop applications do not need a web server.
2. **Do not use `javafx-maven-plugin`**: The run command is `mvn spring-boot:run` instead of `mvn javafx:run`.
3. **Do not create `module-info.java`**: Run in classpath mode to avoid the complexity of modularization.
4. **Lombok annotation processor**: The Spring Boot parent already manages the Lombok dependency; no need to separately configure `annotationProcessorPaths` in `maven-compiler-plugin`.

---

## 6. application.yml Configuration

```yaml
spring:
  # Do not start a web server (desktop application)
  main:
    web-application-type: none
    banner-mode: off
  # Data source configuration (using SQLite as an example)
  datasource:
    url: jdbc:sqlite:data/app.db
    driver-class-name: org.sqlite.JDBC
    username: sa
    password: ""
  # Automatically execute table creation scripts on startup
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql

# MyBatis configuration (if used)
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.model
  configuration:
    map-underscore-to-camel-case: true

logging:
  level:
    com.example: DEBUG
```

### Key Configuration Notes

| Configuration Item | Purpose | Notes |
|---------------------|---------|-------|
| `spring.main.web-application-type=none` | Disables the web server | Desktop applications do not need Tomcat/Netty |
| `spring.main.banner-mode=off` | Turns off the Spring banner | Optional, reduces console noise |
| `spring.sql.init.mode=always` | Executes SQL scripts on startup | Used for automatic table creation and data initialization |

---

## 7. Complete Project Structure

```
myapp/
├── pom.xml
├── data/                           # SQLite database file directory
│   └── app.db
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/
│       │       ├── MyApp.java              # Spring Boot startup class (does not extend Application)
│       │       ├── JavaFXApp.java          # JavaFX entry class (extends Application)
│       │       ├── controller/
│       │       │   ├── MainController.java # Main view controller (@Component)
│       │       │   └── DialogController.java
│       │       ├── service/
│       │       │   ├── UserService.java
│       │       │   └── impl/
│       │       │       └── UserServiceImpl.java
│       │       ├── mapper/
│       │       │   └── UserMapper.java     # MyBatis Mapper (@Mapper)
│       │       └── model/
│       │           └── User.java           # Entity class (JavaFX Properties)
│       └── resources/
│           ├── application.yml             # Spring Boot configuration
│           ├── schema.sql                  # Table creation script
│           ├── fxml/
│           │   ├── main-view.fxml
│           │   └── user-dialog.fxml
│           ├── css/
│           │   ├── light-theme.css
│           │   └── dark-theme.css
│           └── mapper/
│               └── UserMapper.xml          # MyBatis XML mapping
└── target/                         # Build output
```

> **Note**: Do not create `module-info.java`; run in classpath mode.

---

## 8. Integration with Persistence Layer Frameworks

### 8.1 MyBatis Integration

**Dependencies**:
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

**Mapper interface**:
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

**Service layer transaction**:
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

### 8.2 JavaFX Properties and MyBatis Compatibility

The getter/setter of JavaFX Properties can be correctly mapped by MyBatis, but you need to handle null values for primitive type Properties:

```java
public class User {
    private final LongProperty id = new SimpleLongProperty();

    public long getId() { return id.get(); }

    // MyBatis may pass null during injection (insert scenario); safe handling is needed
    public void setId(Long id) {
        this.id.set(id == null ? 0L : id);
    }

    public LongProperty idProperty() { return id; }
}
```

> **Pitfall**: `SimpleLongProperty.set(long)` accepts the primitive type `long`; passing `null` will throw `NullPointerException`. The same applies to `IntegerProperty` and `BooleanProperty`. You must handle null in the setter.

---

## 9. Running and Packaging

### 9.1 Development Run

```bash
# Run using the Spring Boot Maven plugin (recommended)
mvn spring-boot:run

# Or compile first, then run
mvn clean compile
mvn spring-boot:run
```

> **Do not use** `mvn javafx:run` because the project does not have `javafx-maven-plugin` configured, and the Spring Boot container needs to be started via `spring-boot:run`.

### 9.2 Packaging as an Executable JAR

```bash
mvn clean package
java -jar target/myapp-1.0.0.jar
```

### 9.3 Packaging as a Native Installer

Use jpackage to create a platform-native installer:

```bash
# First build the JAR
mvn clean package

# Create a Windows installer
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

> **Note**: When packaging, `--main-class` points to the Spring Boot startup class (`MyApp`), not the JavaFX entry class.

---

## 10. Common Pitfalls Checklist

### Pitfall 1: Main Class Directly Extends Application (Most Common)

**Symptom**: `Error: JavaFX runtime components are missing`

**Cause**: The main class extends Application, so the JVM uses the JavaFX launcher, which cannot find the JavaFX modules in classpath mode.

**Solution**: Split into a startup class + JavaFX entry class (see section 3).

### Pitfall 2: Dialog Controller Injection Failure

**Symptom**: When opening a dialog, the Service in the Controller is null, or a `NoSuchBeanException` is thrown.

**Cause**: The dialog's FXMLLoader does not have `controllerFactory` set.

**Solution**: All FXMLLoader instances need `loader.setControllerFactory(springContext::getBean)` set.

### Pitfall 3: Controller Not Annotated with @Component

**Symptom**: `NoSuchBeanDefinitionException: No qualifying bean of type 'UserController'`

**Cause**: The Controller class is not annotated with a Spring stereotype annotation, so the Bean does not exist in the Spring container.

**Solution**: Add the `@Component` annotation to the Controller class.

### Pitfall 4: Null Handling for JavaFX Properties

**Symptom**: `NullPointerException` thrown when inserting a new record; MyBatis injects a null value into a primitive type Property.

**Cause**: Methods like `SimpleLongProperty.set(long)` accept primitive types and do not accept null.

**Solution**: Convert null to a default value in the setter method (see section 8.2).

### Pitfall 5: Forgetting to Close the Spring Container

**Symptom**: The process does not exit after the application window is closed, or database connections are not released.

**Cause**: The Spring container is still running after JavaFX shuts down.

**Solution**: Call `springContext.close()` in `JavaFXApp.stop()`.

### Pitfall 6: Performing Long-running Database Operations on the UI Thread

**Symptom**: The UI freezes or becomes unresponsive.

**Cause**: MyBatis queries are executed synchronously on the JavaFX Application Thread.

**Solution**: Use `Task` + a background thread for long-running queries, and return results to the UI thread via `Platform.runLater()`:

```java
Task<List<User>> loadTask = new Task<>() {
    @Override
    protected List<User> call() {
        return userService.findPage(keyword, page, pageSize);  // Executed in background
    }
};
loadTask.setOnSucceeded(e -> {
    userTable.getItems().setAll(loadTask.getValue());  // Update on UI thread
});
new Thread(loadTask).start();
```

### Pitfall 7: Lombok Annotation Processor Configuration Conflict

**Symptom**: Maven compilation reports `No processor claimed any of these annotations` or Lombok annotations do not take effect.

**Cause**: `annotationProcessorPaths` was manually configured in `maven-compiler-plugin` but the Lombok version was not specified.

**Solution**: The Spring Boot parent already manages Lombok's annotation processor; no need to separately configure `annotationProcessorPaths` in `maven-compiler-plugin`. Remove that configuration.

### Pitfall 8: Lombok and JDK Version Mismatch in the IDE

**Symptom**: The following error occurs when compiling or running in IntelliJ IDEA, while command-line `mvn compile` works fine:
```
java: java.lang.ExceptionInInitializerError
com.sun.tools.javac.code.TypeTag :: UNKNOWN
```

**Cause**: The Project JDK selected in the IDE does not match the `java.version` configured in `pom.xml`. Lombok accesses the JDK compiler's internal API (`TypeTag`) via reflection, and this error is triggered when the JDK version used by the IDE is incompatible with the Lombok plugin.

**Solution**:
1. Ensure the IDE's Project SDK (File -> Project Structure -> Project SDK) matches the `java.version` in `pom.xml`
2. Update the IDE's Lombok plugin to the latest version (Settings -> Plugins -> Lombok)
3. Check the JDK used by Maven Importing (Settings -> Build -> Maven -> Importing -> JDK for importer)

> **Tip**: Lombok 1.18.30+ supports JDK 21, but the IDE's Lombok plugin may not have been updated accordingly. Ensure that the IDE's JDK selection, the Lombok plugin version, and the `java.version` in `pom.xml` are all consistent.

### Pitfall 9: Controller Singleton Causing State Pollution

**Symptom**: When opening multiple windows or dialogs of the same type, the input data from the previous window "leaks" into the next one, or the UI control state in the Controller becomes chaotic.

**Cause**: Spring registers all `@Component` beans as **singletons** (singleton scope) by default. But in JavaFX, each `FXMLLoader.load()` expects a **brand-new** Controller instance. When Spring returns the same singleton bean, the `@FXML` fields get overwritten by the new FXML's controls, the old control references are lost, leading to state pollution and memory leaks.

**Solution**: Add `@Scope("prototype")` to the Controller class to ensure a new instance is created each time it is obtained from the Spring container:

```java
@Component
@Scope("prototype")  // Create a new instance on each injection
public class UserDialogController implements Initializable {
    // ...
}
```

> **Note**: All Controllers that can be loaded multiple times via `FXMLLoader` should use the `prototype` scope. The main window Controller can remain a singleton if there is only one instance, but still be careful not to cache UI state in the Controller.

### Pitfall 10: spring-boot-devtools Causing JavaFX Abnormal Restart

**Symptom**: During development, after modifying code, the JavaFX window suddenly closes or duplicate windows, `Stage already showing`, and similar exceptions appear.

**Cause**: `spring-boot-devtools`'s auto-restart mechanism monitors classpath changes and triggers a Spring container restart when files change. But JavaFX's `Application` lifecycle is not synchronized with the Spring container; a container restart does not re-invoke `Application.launch()`, leading to inconsistent state.

**Solution**:
1. **Exclude the devtools dependency** (recommended): Set devtools to `optional` in `pom.xml` or remove it directly.
2. **Disable auto-restart**: Configure `spring.devtools.restart.enabled: false` in `application.yml`.
3. **Only disable the JavaFX class trigger**: Configure `spring.devtools.restart.exclude: static/**,public/**` in `application.yml` (exclude resource directories).

```xml
<!-- If devtools must be used, set it to optional -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

### Pitfall 11: Startup Failure After Upgrading to JavaFX 24+

**Symptom**: After upgrading the JavaFX version from 21 to 24+, `mvn spring-boot:run` reports an `IllegalCallerException` on startup.

**Cause**: JavaFX 24+'s graphics rendering layer accesses native code via JNI, which under JDK 24+'s strict module encapsulation requires `--enable-native-access=javafx.graphics`. But Spring Boot's `spring-boot-maven-plugin` does not pass this parameter by default.

**Solution**: Add the JVM argument in the `spring-boot-maven-plugin` configuration:

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

## 11. javafx-weaver Alternative

If the project is large in scale and requires more automated Controller injection, you can use [javafx-weaver](https://github.com/rgielen/javafx-weaver).

**Dependencies**:
```xml
<dependency>
    <groupId>net.rgielen</groupId>
    <artifactId>javafx-weaver-spring-boot-starter</artifactId>
    <version>2.0.1</version>
</dependency>
```

**Usage**:
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
        fxWeaver.loadView(MainController.class);  // Automatically loads FXML and injects the Controller
    }
}
```

> Note: javafx-weaver still requires splitting the startup class; the main class cannot directly extend Application.

---

## 12. Integration Approach Selection Decision Tree

```
Project scale and requirements?
├── Small projects / quick prototyping / beginners
│   └── Manual integration (recommended approach in this guide)
├── Medium projects / need automated Controller injection
│   └── javafx-weaver
└── Existing spring-boot-javafx-support legacy projects
    └── Keep as-is or migrate to manual integration
```
