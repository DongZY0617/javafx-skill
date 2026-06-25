# JavaFX Data Binding Patterns Guide

This guide comprehensively introduces JavaFX's property system, unidirectional and bidirectional binding, computed bindings, number bindings, ObservableList/Map, TableView data binding, form validation patterns, memory management, and JavaFX 21+ subscription-based listeners.

---

## 1. Property Type System

The core of JavaFX is a typed property system, where each primitive data type has a corresponding Property interface and implementation.

### 1.1 Property Type Reference Table

| Data Type  | Property Interface       | Simple Implementation Class | Read-Only Wrapper Class      |
|------------|--------------------------|-----------------------------|------------------------------|
| String     | `StringProperty`         | `SimpleStringProperty`      | `ReadOnlyStringWrapper`      |
| int        | `IntegerProperty`        | `SimpleIntegerProperty`     | `ReadOnlyIntegerWrapper`     |
| long       | `LongProperty`           | `SimpleLongProperty`        | `ReadOnlyLongWrapper`        |
| float      | `FloatProperty`          | `SimpleFloatProperty`       | `ReadOnlyFloatWrapper`       |
| double     | `DoubleProperty`         | `SimpleDoubleProperty`      | `ReadOnlyDoubleWrapper`      |
| boolean    | `BooleanProperty`        | `SimpleBooleanProperty`     | `ReadOnlyBooleanWrapper`     |
| Any Object | `ObjectProperty<T>`      | `SimpleObjectProperty<T>`   | `ReadOnlyObjectWrapper<T>`   |
| List       | `ListProperty<E>`        | `SimpleListProperty<E>`     | `ReadOnlyListWrapper<E>`     |
| Map        | `MapProperty<K,V>`       | `SimpleMapProperty<K,V>`    | `ReadOnlyMapWrapper<K,V>`    |
| Set        | `SetProperty<E>`         | `SimpleSetProperty<E>`      | `ReadOnlySetWrapper<E>`      |

### 1.2 SimpleXxxProperty Basic Usage

```java
public class Person {
    private final StringProperty name = new SimpleStringProperty(this, "name", "");
    private final IntegerProperty age = new SimpleIntegerProperty(this, "age", 0);
    private final DoubleProperty salary = new SimpleDoubleProperty(this, "salary", 0.0);
    private final BooleanProperty active = new SimpleBooleanProperty(this, "active", true);
    private final ObjectProperty<LocalDate> birthday =
        new SimpleObjectProperty<>(this, "birthday", LocalDate.now());

    // Constructor parameter description: (bean, name, initialValue)
    // bean is usually the object the property belongs to (this), for property traceability
    // name is the property name, used for debugging and reflection

    // Standard Property accessor pattern
    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }

    public IntegerProperty ageProperty() { return age; }
    public int getAge() { return age.get(); }
    public void setAge(int value) { age.set(value); }

    public DoubleProperty salaryProperty() { return salary; }
    public double getSalary() { return salary.get(); }
    public void setSalary(double value) { salary.set(value); }

    public BooleanProperty activeProperty() { return active; }
    public boolean isActive() { return active.get(); }
    public void setActive(boolean value) { active.set(value); }

    public ObjectProperty<LocalDate> birthdayProperty() { return birthday; }
    public LocalDate getBirthday() { return birthday.get(); }
    public void setBirthday(LocalDate value) { birthday.set(value); }
}
```

### 1.3 SimpleXxxProperty vs ReadOnlyXxxProperty

When a property only allows internal modification and is read-only externally, use `ReadOnlyXxxWrapper` to expose a read-only view.

```java
public class Account {
    // Internally writable Wrapper
    private final ReadOnlyDoubleWrapper balance = new ReadOnlyDoubleWrapper(this, "balance", 0.0);

    // Expose read-only Property externally
    public ReadOnlyDoubleProperty balanceProperty() { return balance.getReadOnlyProperty(); }
    public double getBalance() { return balance.get(); }

    // Only internal methods can modify
    public void deposit(double amount) {
        balance.set(balance.get() + amount);
    }

    public void withdraw(double amount) {
        if (amount > balance.get()) {
            throw new IllegalStateException("Insufficient balance");
        }
        balance.set(balance.get() - amount);
    }
}
```

> Key difference: `SimpleXxxProperty` is fully read-write externally; `ReadOnlyXxxWrapper` returns a read-only view via `getReadOnlyProperty()`, external code cannot call `set()`, but internal code can still modify through the wrapper.

---

## 2. Unidirectional Binding

Unidirectional binding makes the target property automatically follow changes in the source property, with direction **source → target**. The target becomes read-only.

### 2.1 Basic Unidirectional Binding

```java
StringProperty source = new SimpleStringProperty("Hello");
StringProperty target = new SimpleStringProperty();

// target is bound to source: when source changes, target updates automatically
target.bind(source);

System.out.println(target.get());  // "Hello"
source.set("World");
System.out.println(target.get());  // "World"

// After binding, target becomes read-only, calling set() will throw RuntimeException
// target.set("Error");  // ❌ throws exception
```

### 2.2 UI Control Unidirectional Binding Example

```java
// Label displays the content of TextField (unidirectional)
label.textProperty().bind(textField.textProperty());

// Progress bar bound to progress value
progressBar.progressProperty().bind(task.progressProperty());

// Label displays the slider's current value
valueLabel.textProperty().bind(
    slider.valueProperty().asString("Current value: %.1f"));
```

---

## 3. Bidirectional Binding

Bidirectional binding synchronizes two properties with each other, a change in either one updates the other.

### 3.1 Basic Bidirectional Binding

```java
StringProperty propA = new SimpleStringProperty("A");
StringProperty propB = new SimpleStringProperty("B");

propA.bindBidirectional(propB);

System.out.println(propA.get());  // "A" (keeps original value, no immediate sync)
propB.set("NewValue");
System.out.println(propA.get());  // "NewValue"

propA.set("Another");
System.out.println(propB.get());  // "Another"
```

### 3.2 Bidirectional Binding with Type Conversion

Bidirectional binding between properties of different types requires a converter:

```java
// Bidirectional binding between TextField (String) and IntegerProperty
TextField ageField = new TextField();
IntegerProperty age = new SimpleIntegerProperty(25);

ageField.textProperty().bindBidirectional(age, new NumberStringConverter());

// Now both sync with each other, entering numbers in the input field updates age, modifying age updates the input field
```

### 3.3 Custom Formatted Bidirectional Binding

```java
// Bidirectional binding between date and string, with specified format
ObjectProperty<LocalDate> date = new SimpleObjectProperty<>(LocalDate.now());
TextField dateField = new TextField();

dateField.textProperty().bindBidirectional(date, new StringConverter<>() {
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String toString(LocalDate date) {
        return date == null ? "" : date.format(fmt);
    }

    @Override
    public LocalDate fromString(String text) {
        return (text == null || text.isEmpty()) ? null : LocalDate.parse(text, fmt);
    }
});
```

---

## 4. Computed Bindings

The `Bindings` utility class provides factory methods to create derived read-only bindings based on one or more source properties.

### 4.1 createStringBinding

```java
StringProperty firstName = new SimpleStringProperty("John");
StringProperty lastName = new SimpleStringProperty("Doe");

// Concatenate full name, depends on firstName and lastName
StringBinding fullName = Bindings.createStringBinding(
    () -> firstName.get() + lastName.get(),
    firstName, lastName
);

System.out.println(fullName.get());  // "JohnDoe"
lastName.set("Smith");
System.out.println(fullName.get());  // "JohnSmith"
```

### 4.2 createBooleanBinding

```java
StringProperty username = new SimpleStringProperty();
StringProperty password = new SimpleStringProperty();

// Whether the form is valid
BooleanBinding formValid = Bindings.createBooleanBinding(
    () -> username.get() != null && username.get().length() >= 3
       && password.get() != null && password.get().length() >= 6,
    username, password
);

// Bind to button disabled state
loginButton.disableProperty().bind(formValid.not());
```

### 4.3 createIntegerBinding / createObjectBinding

```java
ObservableList<Item> items = FXCollections.observableArrayList();

// Count the number of completed items in the list
IntegerBinding completedCount = Bindings.createIntegerBinding(
    () -> (int) items.stream().filter(Item::isCompleted).count(),
    items
);

// Compute the object of the selected item
ObjectProperty<Item> selected = new SimpleObjectProperty<>();
ObjectBinding<String> selectedName = Bindings.createObjectBinding(
    () -> selected.get() == null ? "None selected" : selected.get().getName(),
    selected
);
```

### 4.4 selectBinding (Nested Property Access)

```java
// Access properties of nested objects
ObjectProperty<Person> person = new SimpleObjectProperty<>(new Person("Alice"));
StringBinding name = person.select(p -> p.nameProperty());
// Automatically updates when person or person.name changes
```

---

## 5. Number Binding

Numeric properties support arithmetic operation bindings, the result is a `NumberBinding`.

### 5.1 Arithmetic Operations

```java
IntegerProperty quantity = new SimpleIntegerProperty(5);
DoubleProperty price = new SimpleDoubleProperty(19.99);

// Multiplication: total price
NumberBinding total = quantity.multiply(price);
System.out.println(total.doubleValue());  // 99.95

// Addition
IntegerProperty a = new SimpleIntegerProperty(10);
IntegerProperty b = new SimpleIntegerProperty(3);
NumberBinding sum = a.add(b);        // 13
NumberBinding diff = a.subtract(b);  // 7
NumberBinding product = a.multiply(b); // 30
NumberBinding quotient = a.divide(b);  // 3.333...
```

### 5.2 Chained Operations

```java
DoubleProperty basePrice = new SimpleDoubleProperty(100.0);
DoubleProperty taxRate = new SimpleDoubleProperty(0.08);   // 8% tax rate
DoubleProperty discount = new SimpleDoubleProperty(10.0);  // discount

// Final price = (basePrice - discount) * (1 + taxRate)
NumberBinding finalPrice = basePrice
    .subtract(discount)
    .multiply(taxRate.add(1.0));

System.out.println(finalPrice.doubleValue());  // (100-10) * 1.08 = 97.2
```

### 5.3 Numeric Comparison Binding

```java
IntegerProperty stock = new SimpleIntegerProperty(5);
IntegerProperty threshold = new SimpleIntegerProperty(10);

// Whether stock is below threshold
BooleanBinding lowStock = stock.lessThan(threshold);
warningLabel.visibleProperty().bind(lowStock);
```

### 5.4 Conditional Binding (when/then/otherwise)

```java
IntegerProperty score = new SimpleIntegerProperty(75);

// Return pass/fail text based on score
StringBinding result = Bindings.when(score.greaterThanOrEqualTo(60))
    .then("Pass")
    .otherwise("Fail");
```

---

## 6. ObservableList and ListChangeListener

`ObservableList` is an observable list, any add/remove/update operation notifies listeners.

### 6.1 Creation and Basic Operations

```java
ObservableList<String> names = FXCollections.observableArrayList();
names.add("Alice");
names.addAll("Bob", "Charlie");
names.remove("Bob");
names.set(0, "Alicia");  // replace
```

### 6.2 ListChangeListener

```java
names.addListener((ListChangeListener<String>) change -> {
    while (change.next()) {
        if (change.wasAdded()) {
            System.out.println("Added: " + change.getAddedSubList());
        }
        if (change.wasRemoved()) {
            System.out.println("Removed: " + change.getRemoved());
        }
        if (change.wasUpdated()) {
            System.out.println("Updated: index " + change.getFrom() + " to " + change.getTo());
        }
        if (change.wasReplaced()) {
            System.out.println("Replace operation");
        }
    }
});
```

### 6.3 Using Extractors to Automatically Observe Element Properties

By default, ObservableList only listens to list structural changes (add/remove). To listen to internal property changes of elements, you need to use an extractor:

```java
// Also triggers list update events when Task's title or completed property changes
ObservableList<Task> tasks = FXCollections.observableArrayList(
    task -> new Observable[]{ task.titleProperty(), task.completedProperty() }
);

tasks.addListener((ListChangeListener<Task>) c -> {
    while (c.next()) {
        if (c.wasUpdated()) {
            System.out.println("Task property modified: " + c.getList().subList(c.getFrom(), c.getTo()));
        }
    }
});

Task t = new Task("Study");
tasks.add(t);
t.setCompleted(true);  // triggers wasUpdated event
```

---

## 7. ObservableMap and MapChangeListener

```java
ObservableMap<String, Integer> scores = FXCollections.observableHashMap();

scores.addListener((MapChangeListener<String, Integer>) change -> {
    if (change.wasAdded()) {
        System.out.println("Added/Updated key: " + change.getKey()
            + " = " + change.getValueAdded());
    }
    if (change.wasRemoved()) {
        System.out.println("Removed key: " + change.getKey()
            + " old value: " + change.getValueRemoved());
    }
});

scores.put("Math", 95);    // triggers wasAdded
scores.put("Math", 98);    // triggers wasAdded + wasRemoved (update)
scores.remove("Math");     // triggers wasRemoved
```

---

## 8. TableView Data Binding (FilteredList + SortedList)

`FilteredList` and `SortedList` are the standard patterns for implementing search filtering and sorting in TableView.

### 8.1 Complete Example

```java
public class UserTableController implements Initializable {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> nameCol;
    @FXML private TableColumn<User, Integer> ageCol;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> ageFilterCombo;

    // Original data
    private final ObservableList<User> masterData = FXCollections.observableArrayList();

    // Filtered data
    private final FilteredList<User> filteredData = new FilteredList<>(masterData, p -> true);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load initial data
        masterData.addAll(
            new User("Alice", 25),
            new User("Bob", 30),
            new User("Charlie", 22),
            new User("David", 35)
        );

        // Sorted data (wrapping FilteredList)
        SortedList<User> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(userTable.comparatorProperty());

        // Bind to TableView
        userTable.setItems(sortedData);

        // Column mapping
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        ageCol.setCellValueFactory(c -> c.getValue().ageProperty().asObject());

        // Search filter: by name containing keyword
        searchField.textProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        ageFilterCombo.getItems().addAll("All", "<30", ">=30");
        ageFilterCombo.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> updateFilter());
    }

    private void updateFilter() {
        String keyword = searchField.getText();
        String ageFilter = ageFilterCombo.getValue();

        filteredData.setPredicate(user -> {
            // Name filter
            boolean nameMatch = keyword == null || keyword.isEmpty()
                || user.getName().toLowerCase().contains(keyword.toLowerCase());

            // Age filter
            boolean ageMatch = true;
            if (">=30".equals(ageFilter)) {
                ageMatch = user.getAge() >= 30;
            } else if ("<30".equals(ageFilter)) {
                ageMatch = user.getAge() < 30;
            }

            return nameMatch && ageMatch;
        });
    }
}
```

### 8.2 Data Flow

```
masterData (ObservableList)
    ↓ filter
filteredData (FilteredList)
    ↓ sort
sortedData (SortedList)
    ↓ bind
userTable (TableView)
```

> Key point: `SortedList`'s comparator is bound to TableView's comparator, so clicking column headers to sort takes effect automatically.

---

## 9. Form Validation Patterns

Use bindings to implement declarative form validation.

### 9.1 Validation Binding Example

```java
public class RegistrationForm {

    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty password = new SimpleStringProperty();
    private final StringProperty confirmPassword = new SimpleStringProperty();

    // Validation result for each field
    private final BooleanBinding usernameValid = Bindings.createBooleanBinding(
        () -> username.get() != null && username.get().matches("[a-zA-Z0-9_]{3,20}"),
        username
    );

    private final BooleanBinding emailValid = Bindings.createBooleanBinding(
        () -> email.get() != null && email.get().matches("^[\\w.-]+@[\\w.-]+\\.\\w+$"),
        email
    );

    private final BooleanBinding passwordValid = Bindings.createBooleanBinding(
        () -> password.get() != null && password.get().length() >= 8
           && password.get().matches(".*[A-Z].*") && password.get().matches(".*[0-9].*"),
        password
    );

    private final BooleanBinding passwordsMatch = Bindings.createBooleanBinding(
        () -> password.get() != null && password.get().equals(confirmPassword.get()),
        password, confirmPassword
    );

    // Whether the entire form is valid
    private final BooleanBinding formValid = usernameValid
        .and(emailValid)
        .and(passwordValid)
        .and(passwordsMatch);

    // Error messages
    private final StringBinding usernameError = Bindings.when(usernameValid)
        .then("").otherwise("Username must be 3-20 alphanumeric characters or underscores");
    private final StringBinding emailError = Bindings.when(emailValid)
        .then("").otherwise("Invalid email format");
    private final StringBinding passwordError = Bindings.when(passwordValid)
        .then("").otherwise("Password must be at least 8 characters, with uppercase letters and numbers");
    private final StringBinding confirmError = Bindings.when(passwordsMatch)
        .then("").otherwise("Passwords do not match");

    // Bind to UI in Controller
    public void bindToUI(TextField userField, TextField emailField,
                         TextField passField, TextField confirmField,
                         Label userErrLabel, Label emailErrLabel,
                         Label passErrLabel, Label confirmErrLabel,
                         Button submitButton) {
        userField.textProperty().bindBidirectional(username);
        emailField.textProperty().bindBidirectional(email);
        passField.textProperty().bindBidirectional(password);
        confirmField.textProperty().bindBidirectional(confirmPassword);

        userErrLabel.textProperty().bind(usernameError);
        emailErrLabel.textProperty().bind(emailError);
        passErrLabel.textProperty().bind(passwordError);
        confirmErrLabel.textProperty().bind(confirmError);

        // Error label visibility
        userErrLabel.visibleProperty().bind(usernameValid.not());
        emailErrLabel.visibleProperty().bind(emailValid.not());
        passErrLabel.visibleProperty().bind(passwordValid.not());
        confirmErrLabel.visibleProperty().bind(passwordsMatch.not());

        // Submit button enabled state
        submitButton.disableProperty().bind(formValid.not());
    }
}
```

---

## 10. Memory Management: Unbinding and Listener Removal

Bindings and listeners hold object references, failing to clean them up in time leads to memory leaks.

### 10.1 Unbinding Unidirectional Binding

```java
StringProperty target = new SimpleStringProperty();
target.bind(source);

// Unbind
target.unbind();
// After unbinding, target becomes writable again, no longer follows source
```

### 10.2 Unbinding Bidirectional Binding

```java
propA.bindBidirectional(propB);
// Unbind bidirectional binding
propA.unbindBidirectional(propB);
```

### 10.3 Removing ChangeListener / InvalidationListener

```java
ChangeListener<String> listener = (obs, oldVal, newVal) -> {
    System.out.println("Changed: " + oldVal + " -> " + newVal);
};

nameProperty.addListener(listener);

// Must remove the listener with the same reference
nameProperty.removeListener(listener);
```

> Note: When adding listeners using Lambda expressions, you must save the reference to remove them. The same applies to anonymous inner classes.

### 10.4 Using WeakChangeListener to Avoid Leaks

When you cannot guarantee listener removal, use weak reference listeners:

```java
// WeakChangeListener does not prevent the listener object from being GC'd
nameProperty.addListener(new WeakChangeListener<>((obs, oldVal, newVal) -> {
    System.out.println("Weak listener: " + newVal);
}));
```

> Weak listeners are suitable for scenarios where the listener's lifecycle is shorter than the observed property. But note: if the listener Lambda has no other strong references, it may be collected prematurely.

### 10.5 Common Memory Leak Scenarios and Countermeasures

| Scenario                                                | Countermeasure                                              |
|---------------------------------------------------------|-------------------------------------------------------------|
| Listeners added in Controller not removed               | Remove in `@FXML`-annotated `dispose()` or on unload        |
| Short-lived object listening to long-lived property     | Use `WeakChangeListener`                                    |
| Listeners on static properties                          | Explicitly remove on application shutdown                   |
| Bidirectional binding not unbound                       | Call `unbindBidirectional()` when no longer needed          |
| ObservableList extractor listener                       | Be mindful of element listener release when clearing or replacing list |

---

## 11. JavaFX 21+ Subscription-based Listeners

JavaFX 21 introduced a `Subscription`-based listener API, providing a more modern and safer resource management approach, replacing the traditional `addListener` / `removeListener` pattern.

### 11.1 Basic Usage

```java
// JavaFX 21+: subscribe returns a Subscription
Subscription subscription = nameProperty.subscribe((obs, oldVal, newVal) -> {
    System.out.println("Name changed: " + oldVal + " -> " + newVal);
});

// Unsubscribe when no longer needed
subscription.unsubscribe();
// After unsubscribing, the listener is automatically removed, and unsubscribe() is idempotent (can be called repeatedly)
```

### 11.2 Subscription Methods of ObservableValue

```java
// Subscribe to value changes (with old and new values)
Subscription sub1 = property.subscribe((observable, oldValue, newValue) -> {
    System.out.println("Changed: " + oldValue + " -> " + newValue);
});

// Subscribe to only the new value
Subscription sub2 = property.subscribe(newValue -> {
    System.out.println("New value: " + newValue);
});

// Subscribe to invalidation (invalidity notification)
Subscription sub3 = property.subscribeInvalidations(observable -> {
    System.out.println("Property invalidated");
});
```

### 11.3 Subscription Methods of ObservableList

```java
ObservableList<String> list = FXCollections.observableArrayList();

// Subscribe to list changes
Subscription sub = list.subscribe(change -> {
    while (change.next()) {
        if (change.wasAdded()) {
            System.out.println("Added: " + change.getAddedSubList());
        }
        if (change.wasRemoved()) {
            System.out.println("Removed: " + change.getRemoved());
        }
    }
});
```

### 11.4 Combined Subscription Management

Multiple Subscriptions can be merged for management:

```java
Subscription sub1 = propA.subscribe(...);
Subscription sub2 = propB.subscribe(...);
Subscription sub3 = list.subscribe(...);

// Merge into a single Subscription
Subscription combined = Subscription.combine(sub1, sub2, sub3);

// Unsubscribe all at once
combined.unsubscribe();
```

### 11.5 try-with-resources Pattern

`Subscription` implements `AutoCloseable`, supporting try-with-resources:

```java
try (Subscription sub = property.subscribe(val -> update(val))) {
    // Listener is active within this scope
    doWork();
} // automatically unsubscribes
```

### 11.6 Traditional API vs Subscription API Comparison

| Feature        | addListener / removeListener              | subscribe (Subscription)                |
|----------------|-------------------------------------------|-----------------------------------------|
| Removal method | Need to save listener reference, manual remove | Call `subscription.unsubscribe()`       |
| Idempotency    | Repeated remove is harmless but watch for reference consistency | `unsubscribe()` is idempotent, safe to call multiple times |
| Combined management | Need to remove one by one manually   | `Subscription.combine()` for one-click management |
| Resource safety | Easy to forget remove, causing leaks    | Safer with try-with-resources           |
| Available version | All JavaFX versions                    | JavaFX 21+                              |

### 11.7 Migration Recommendations

- New projects (JavaFX 21+) should prefer the `subscribe` API.
- Projects needing JavaFX 17 compatibility should continue using `addListener`, but must pair it with `removeListener`.
- When mixing, maintain consistency, use a unified style within the same module.

---

## 12. Summary of Binding Pattern Best Practices

| Practice                               | Description                                                       |
|----------------------------------------|-------------------------------------------------------------------|
| Prefer declarative binding             | Use `bind()` / `Bindings.createXxxBinding()` instead of manual listener refresh |
| Distinguish unidirectional vs bidirectional | Use unidirectional for display only, bidirectional for form input |
| Use createXxxBinding for computed properties | Avoid manual computation in listeners, declare dependencies with bindings |
| Use NumberBinding for numeric operations | Chained arithmetic bindings clearly express computation logic   |
| Use FilteredList+SortedList for TableView | Standard filtering and sorting pattern, avoid manual list manipulation |
| Use BooleanBinding composition for form validation | Declarative validation, automatically drives UI state |
| Unbind and remove listeners in time    | Prevent memory leaks, prefer Subscription in JavaFX 21+          |
| Use extractor to observe element properties | ObservableList needs extractor to listen to internal element changes |
| Use ReadOnlyXxxWrapper for read-only properties | Protect internal state, only expose read-only view       |
