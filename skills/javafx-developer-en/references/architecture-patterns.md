# JavaFX Architecture Patterns Guide

This guide details common architecture patterns used in JavaFX applications, including complete implementation examples of MVC and MVVM, pattern comparisons, common anti-patterns, service layer design, dependency injection solutions, and the event bus pattern.

---

## 1. MVC Pattern (Model-View-Controller)

MVC is the most classic UI architecture pattern. In JavaFX, the Model exposes data using JavaFX Properties, the View is described with FXML, and the Controller handles user interaction and updates the Model through the `@FXML` annotation.

### 1.1 Responsibility Division

| Component   | Responsibility                                                        |
|-------------|-----------------------------------------------------------------------|
| Model       | Data model and business state, using JavaFX Property for observability. |
| View        | Pure UI declaration (FXML), contains no business logic, displays Model data through binding. |
| Controller  | Receives user events, calls Service/Model methods, coordinates interaction between View and Model. |

### 1.2 Complete Code Example

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
        <TextField fx:id="titleField" promptText="Enter task title" HBox.hgrow="ALWAYS"/>
        <Button text="Add" onAction="#handleAddTask"/>
    </HBox>
    <ListView fx:id="taskListView" VBox.vgrow="ALWAYS"/>
    <Label fx:id="statusLabel" text="Total 0 tasks"/>
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
    private final TaskService taskService = new TaskService(); // Service layer

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        taskListView.setItems(tasks);
        taskListView.setCellFactory(lv -> new TaskListCell());

        // Bind the status label to the task count
        statusLabel.textProperty().bind(
            Bindings.size(tasks).asString("Total %d tasks"));

        // Load initial data
        tasks.addAll(taskService.loadTasks());
    }

    @FXML
    private void handleAddTask() {
        String title = titleField.getText().trim();
        if (!title.isEmpty()) {
            Task task = new Task(title);
            taskService.saveTask(task);  // Call Service
            tasks.add(task);
            titleField.clear();
        }
    }
}
```

### 1.3 MVC Data Flow

```
User interaction -> Controller.handleXxx() -> Service/Model update
                                        |
                                        v
View <- (binding/manual refresh) <- Model state change
```

---

## 2. MVVM Pattern (Model-View-ViewModel)

MVVM uses a ViewModel as an intermediary between View and Model, leveraging JavaFX's powerful data binding mechanism to completely decouple the View from logic. The ViewModel does not hold a reference to the View; it only exposes bindable Properties.

### 2.1 Responsibility Division

| Component  | Responsibility                                                          |
|------------|-------------------------------------------------------------------------|
| Model      | Pure data model and business entities.                                  |
| ViewModel  | Exposes Properties and Commands needed by the View, encapsulates business logic, has no UI dependencies. |
| View       | FXML + Controller (thin Controller), only responsible for binding UI controls to the ViewModel. |

### 2.2 Complete Code Example

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

    // Form input bindings
    private final StringProperty inputName = new SimpleStringProperty();
    private final IntegerProperty inputAge = new SimpleIntegerProperty();

    // List data
    private final ObservableList<User> users = FXCollections.observableArrayList();

    // Selected item
    private final ObjectProperty<User> selectedUser = new SimpleObjectProperty<>();

    // Computed property: whether the form is valid
    private final BooleanProperty formValid = new SimpleBooleanProperty(false);

    public UserViewModel() {
        // Form validation: name is non-empty and age is between 0-150
        formValid.bind(Bindings.createBooleanBinding(() -> {
            String name = getInputName();
            int age = getInputAge();
            return name != null && !name.trim().isEmpty() && age >= 0 && age <= 150;
        }, inputName, inputAge));

        // Load initial data
        users.addAll(userService.loadAllUsers());
    }

    /** Add user command */
    public void addUser() {
        if (!formValid.get()) return;
        User user = new User(getInputName().trim(), getInputAge());
        userService.save(user);
        users.add(user);
        inputName.set("");
        inputAge.set(0);
    }

    /** Remove selected user command */
    public void removeSelectedUser() {
        User selected = getSelectedUser();
        if (selected != null) {
            userService.delete(selected);
            users.remove(selected);
        }
    }

    // === Property accessors ===
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

**View Controller (thin Controller) — `UserViewController.java`**

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
        // Bidirectional binding: UI controls <-> ViewModel Property
        nameField.textProperty().bindBidirectional(viewModel.inputNameProperty());
        ageField.textProperty().bindBidirectional(
            viewModel.inputAgeProperty(), new NumberStringConverter());

        // Table data binding
        userTable.setItems(viewModel.getUsers());
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        ageCol.setCellValueFactory(c -> c.getValue().ageProperty());

        // Selected item binding
        viewModel.selectedUserProperty()
            .bind(userTable.getSelectionModel().selectedItemProperty());

        // Button disabled state bound to computed property
        addButton.disableProperty().bind(viewModel.formValidProperty().not());
        removeButton.disableProperty()
            .bind(viewModel.selectedUserProperty().isNull());

        // Button events delegated to ViewModel commands
        addButton.setOnAction(e -> viewModel.addUser());
        removeButton.setOnAction(e -> viewModel.removeSelectedUser());
    }
}
```

### 2.3 MVVM Data Flow

```
View (FXML + thin Controller) <-bidirectional binding-> ViewModel (Property/Command)
                                            | calls
                                            v
                                        Service / Model
```

---

## 3. MVC vs MVVM Comparison

| Comparison Dimension      | MVC                                  | MVVM                                       |
|---------------------------|--------------------------------------|--------------------------------------------|
| Complexity                | Lower, simple and direct structure   | Higher, requires additional ViewModel layer |
| Testability               | Medium, Controller depends on JavaFX controls | High, ViewModel has no UI dependencies, can be purely unit tested |
| Coupling                  | Controller directly manipulates View controls, higher coupling | View and logic decoupled through binding, lower coupling |
| Data synchronization      | Manual refresh or one-way binding    | Bidirectional binding auto-sync            |
| Code volume               | Less                                 | More (need to write ViewModel and binding code) |
| Applicable scenarios      | Simple forms, small tools, rapid prototypes | Medium to large applications, complex forms, high testability required |
| Learning curve            | Gentle                               | Steeper, need to understand binding mechanism |
| Team collaboration        | View and logic easily mixed          | Frontend/logic can be developed in parallel |

### Selection Advice

- **Small applications / utility tools**: Use MVC, fast and direct.
- **Medium to large applications / requiring unit tests**: Use MVVM, ViewModel can be tested independently.
- **Mixed usage**: Use MVC for simple pages, MVVM for complex pages; both can coexist in the same project.

---

## 4. Anti-patterns to Avoid

### 4.1 Fat Controller

**Problem**: The Controller accumulates large amounts of business logic, data access, and validation code, making it difficult to maintain and test.

```java
// Anti-pattern: Controller directly operates the database
@FXML
private void handleLogin() {
    String username = usernameField.getText();
    String password = passwordField.getText();
    // Business logic, validation, database access all stuffed in the Controller
    if (username.length() < 3) { /* ... */ }
    try (Connection conn = DriverManager.getConnection("jdbc:...")) {
        PreparedStatement ps = conn.prepareStatement("SELECT ...");
        // ...
    }
}
```

```java
// Correct approach: Controller only delegates to Service
@FXML
private void handleLogin() {
    boolean success = authService.login(
        usernameField.getText(), passwordField.getText());
    if (success) { showMainView(); }
    else { showError("Login failed"); }
}
```

### 4.2 Embedding Business Logic in FXML

**Problem**: Writing logic through scripts (such as JavaScript) in FXML's `onAction`, or handling business through complex inline expressions in the FXML Controller.

```xml
<!-- Anti-pattern: Embedding script logic in FXML -->
<Button text="Calculate" onAction="#calculate">
    <script>
        var result = parseInt(a) + parseInt(b);
        label.setText(result);
    </script>
</Button>
```

FXML should remain purely declarative; all logic should go into the Controller or ViewModel.

### 4.3 Tight Coupling

**Problem**: The Controller directly `new`s concrete dependency classes, making them impossible to replace and test.

```java
// Anti-pattern: Hardcoded dependencies
public class OrderController {
    private MySQLDatabase db = new MySQLDatabase();  // Tight coupling
    private EmailService email = new SmtpEmailService();
}
```

```java
// Correct approach: Interface + constructor injection
public class OrderController {
    private final Database db;
    private final EmailService email;

    public OrderController(Database db, EmailService email) {
        this.db = db;
        this.email = email;
    }
}
```

### 4.4 Other Common Anti-patterns

| Anti-pattern                          | Description and Improvement                                          |
|---------------------------------------|----------------------------------------------------------------------|
| God Controller                        | A single Controller manages all functionality; should be split into multiple Controllers. |
| Executing time-consuming operations on the UI thread | Blocks the JavaFX Application Thread causing UI freeze; should use `Task` + background thread. |
| Directly exposing internal collections | Should return unmodifiable views or wrap with Property.              |
| Ignoring resource release             | Not removing listeners causes memory leaks; see data binding guide.  |

---

## 5. Service Layer Design

The service layer encapsulates business logic and data access, acting as an intermediate layer between the Controller/ViewModel and the data layer, keeping the UI layer clean and testable.

### 5.1 Service Layer Responsibilities

- Encapsulate business rules and transaction boundaries.
- Coordinate multiple Repositories / DAOs.
- Provide UI-agnostic APIs that return domain objects.
- Handle exceptions and convert them to business semantics.

### 5.2 Service Layer Implementation Example

```java
package com.example.service;

import com.example.model.Task;
import com.example.repository.TaskRepository;

import java.util.List;

public class TaskService {

    private final TaskRepository repository;

    // Constructor injection of Repository (interface)
    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    /** Load all tasks */
    public List<Task> loadTasks() {
        return repository.findAll();
    }

    /** Save task (with business validation) */
    public void saveTask(Task task) {
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new IllegalArgumentException("Task title cannot be empty");
        }
        repository.save(task);
    }

    /** Toggle task completion status */
    public void toggleComplete(Task task) {
        task.setCompleted(!task.isCompleted());
        repository.update(task);
    }

    /** Delete task */
    public void deleteTask(Task task) {
        repository.delete(task);
    }
}
```

### 5.3 Asynchronous Service Calls

Time-consuming service calls should be executed on a background thread to avoid blocking the UI thread:

```java
@FXML
private void handleLoadTasks() {
    Task<List<Task>> loadTask = new Task<>() {
        @Override
        protected List<Task> call() {
            return taskService.loadTasks();  // Execute in background
        }
    };
    loadTask.setOnSucceeded(e -> {
        tasks.setAll(loadTask.getValue());  // Return to UI thread to update
        statusLabel.setText("Loading complete");
    });
    loadTask.setOnFailed(e -> {
        statusLabel.setText("Loading failed: " + loadTask.getException().getMessage());
    });
    new Thread(loadTask).start();
}
```

---

## 6. Dependency Injection

Dependency injection is used to decouple dependencies between components, making testing and implementation replacement easier. There are three common approaches in JavaFX.

### 6.1 Manual Dependency Injection

Suitable for small projects, passing dependencies manually through factories or constructors.

```java
public class AppFactory {

    public static MainController createMainController() {
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo);
        return new MainController(service);
    }
}

// In Application.start()
FXMLLoader loader = new FXMLLoader(url);
loader.setControllerFactory(c -> AppFactory.createMainController());
Parent root = loader.load();
```

### 6.2 Guice Dependency Injection

Google Guice is a lightweight DI framework, suitable for small to medium JavaFX projects.

**Maven dependency:**

```xml
<dependency>
    <groupId>com.google.inject</groupId>
    <artifactId>guice</artifactId>
    <version>7.0.0</version>
</dependency>
```

**Configuration and usage:**

```java
public class AppModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(TaskRepository.class).to(InMemoryTaskRepository.class);
        bind(TaskService.class).to(TaskServiceImpl.class);
    }
}

// Custom ControllerFactory to let FXMLLoader use Guice to create Controllers
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

// Application launch
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

**Using injection in Controller:**

```java
public class MainController implements Initializable {
    private final TaskService taskService;

    @Inject  // Guice constructor injection
    public MainController(TaskService taskService) {
        this.taskService = taskService;
    }
    // ...
}
```

> Note: When using Guice, `module-info.java` needs to `opens` the Controller package to the Guice module to support reflection.

### 6.3 Spring Framework Dependency Injection

Spring Boot can be integrated with JavaFX, suitable for projects already in the Spring ecosystem.

> **Critical pitfall: The main class cannot directly extend Application**
>
> When the main class (the class containing the `main` method) directly `extends Application`, the JVM will attempt to run using the JavaFX-specific launcher. In classpath mode (no `module-info.java`, running via `mvn spring-boot:run` or `java -jar`), the JavaFX launcher cannot find the `javafx.graphics` module and will throw an error:
> ```
> Error: JavaFX runtime components are missing, and are required to run this application
> ```
> **Solution**: Split the launch class into two classes - a Spring Boot launch class (not extending Application) + a JavaFX entry class (extending Application). See `references/spring-boot-integration.md` for details.

**Maven dependency:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>3.3.0</version>
</dependency>
```

**Spring Boot + JavaFX integration (correct approach: split into two classes):**

```java
// 1. Spring Boot launch class: does not extend Application, runs through normal JVM launch flow
@SpringBootApplication
public class MyApp {

    static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        // Start the Spring container first
        springContext = SpringApplication.run(MyApp.class, args);
        // Then start JavaFX (delegate to JavaFX entry class)
        Application.launch(JavaFXApp.class, args);
    }
}
```

```java
// 2. JavaFX entry class: extends Application, responsible for UI loading
public class JavaFXApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        // Get Controller Bean from Spring container via controllerFactory
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

**Spring-managed Controller:**

```java
@Component
public class MainController implements Initializable {
    private final TaskService taskService;

    // Spring constructor injection (no @Autowired annotation needed, Spring 4.3+ auto-injects single constructor)
    public MainController(TaskService taskService) {
        this.taskService = taskService;
    }
    // ...
}
```

### 6.4 Comparison of Three DI Approaches

| Approach | Complexity | Applicable Scenarios            | Pros                    | Cons                    |
|----------|------------|---------------------------------|-------------------------|--------------------------|
| Manual   | Low        | Small projects, prototypes      | No extra dependencies, simple and direct | Manual dependency maintenance, error-prone |
| Guice    | Medium     | Medium projects, need lightweight DI | Lightweight, clean API, fast startup | Ecosystem not as rich as Spring |
| Spring   | High       | Large enterprise applications, existing Spring ecosystem | Comprehensive features, powerful ecosystem | Heavier, slower startup |

---

## 7. Event Bus Pattern

The event bus is used to implement loosely coupled communication between components, especially suitable for scenarios where multiple unrelated modules need to respond to the same event (e.g., refreshing multiple views after a user logs in).

### 7.1 Simple Event Bus Implementation

```java
package com.example.eventbus;

import java.util.*;
import java.util.concurrent.*;

/**
 * Lightweight event bus, supports registering/unregistering listeners and publishing events.
 * Event dispatch is executed on the JavaFX Application Thread.
 */
public class EventBus {

    private final Map<Class<?>, List<EventListener<?>>> listeners =
        new ConcurrentHashMap<>();

    /** Register a listener */
    public <T> void subscribe(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                 .add(listener);
    }

    /** Unregister a listener */
    public <T> void unsubscribe(Class<T> eventType, EventListener<T> listener) {
        List<EventListener<?>> list = listeners.get(eventType);
        if (list != null) {
            list.remove(listener);
        }
    }

    /** Publish an event (notifies on the JavaFX Application Thread) */
    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        List<EventListener<?>> list = listeners.get(event.getClass());
        if (list != null) {
            for (EventListener<?> listener : list) {
                ((EventListener<T>) listener).onEvent(event);
            }
        }
    }

    @FunctionalInterface
    public interface EventListener<T> {
        void onEvent(T event);
    }
}
```

### 7.2 Defining Events

```java
package com.example.event;

// User logged in event
public record UserLoggedInEvent(String username, long timestamp) {}

// Task completed event
public record TaskCompletedEvent(Long taskId) {}

// Data refresh event
public record DataRefreshEvent(String source) {}
```

### 7.3 Usage Example

```java
public class HeaderController implements Initializable {
    private final EventBus eventBus = EventBus.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Subscribe to event
        eventBus.subscribe(UserLoggedInEvent.class, this::onUserLoggedIn);
    }

    private void onUserLoggedIn(UserLoggedInEvent event) {
        welcomeLabel.setText("Welcome, " + event.username());
    }

    @FXML
    private void handleLogin() {
        // ... login logic ...
        eventBus.publish(new UserLoggedInEvent(username, System.currentTimeMillis()));
    }
}

public class SidebarController implements Initializable {
    private final EventBus eventBus = EventBus.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Another controller independently subscribes to the same event
        eventBus.subscribe(UserLoggedInEvent.class, e -> {
            loadUserMenus(e.username());
        });
    }
}
```

### 7.4 Event Bus Usage Considerations

1. **Unregister listeners promptly**: Call `unsubscribe` when a Controller is destroyed to prevent memory leaks.
2. **Thread safety**: The above implementation uses `CopyOnWriteArrayList` to ensure concurrency safety; if event handling involves UI updates, use `Platform.runLater()` to switch to the UI thread.
3. **Avoid circular events**: Publishing new events within event handling may cause infinite loops; design carefully.
4. **Mature solutions**: For production environments, consider using Google Guava's `EventBus` or Apache DeltaSpike's event mechanism.

### 7.5 Guava EventBus Integration Example

```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>33.2.1-jre</version>
</dependency>
```

```java
// Create an event bus (can be configured as asynchronous)
EventBus eventBus = new EventBus("javafx-app");

// Subscribe: use the @Subscribe annotation
public class DashboardController {
    @Subscribe
    public void onUserLoggedIn(UserLoggedInEvent event) {
        Platform.runLater(() -> updateDashboard(event.username()));
    }
}

// Register and unregister
eventBus.register(dashboardController);
// eventBus.unregister(dashboardController); // Unregister on destroy

// Publish event
eventBus.post(new UserLoggedInEvent("admin", System.currentTimeMillis()));
```

---

## 8. Architecture Pattern Selection Decision Tree

```
Application scale?
|-- Small (< 10 pages, simple logic)
|   |-- MVC + manual dependency injection
|-- Medium (10-50 pages, medium complexity)
|   |-- MVVM + Guice + event bus
|-- Large (> 50 pages, complex business)
    |-- MVVM + Spring + event bus + service layer
```

### General Architecture Principles

1. **Single responsibility**: Each class does only one thing (Controller manages interaction, Service manages business, Repository manages data).
2. **Dependency inversion**: Depend on interfaces rather than concrete implementations, inject through DI.
3. **UI thread purity**: Time-consuming operations go to background threads, UI updates return to the Application Thread.
4. **Testability**: Decouple business logic from UI so that ViewModel/Service can be independently unit tested.
5. **Progressive architecture**: Start with simple patterns, gradually introduce more advanced patterns as complexity grows.
