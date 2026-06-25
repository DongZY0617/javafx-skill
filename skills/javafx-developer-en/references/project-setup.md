# JavaFX Project Setup Guide

This guide covers the complete setup process for JavaFX projects, including version selection, build tool configuration (Maven / Gradle), modular setup, cross-platform packaging, and custom runtime image (jlink) creation.

---

## 1. JavaFX Version Roadmap

Since being independently maintained by the OpenJFX project, JavaFX adopts a six-month release cadence (aligned with the JDK LTS schedule). The table below lists the release dates, LTS status, and key features of major versions.

| JavaFX Version | Release Date   | Corresponding JDK | LTS Status | Key Features / Notes                                                |
|----------------|-----------------|--------------------|------------|---------------------------------------------------------------------|
| 17             | 2021-09-14      | JDK 17             | LTS (until Oct 2026) | Long-term support version, stable and mature; best third-party library compatibility, suitable for legacy system maintenance. |
| 21             | 2023-09-19      | JDK 17             | LTS        | LTS version; adds subscription-based listener API, performance and stability improvements. |
| 24             | 2025-03-18      | JDK 22             | Non-LTS    | Introduces `--enable-native-access` requirement for accessing the native rendering layer; multi-monitor improvements. |
| 25             | 2025-09-16      | JDK 23             | LTS        | Latest LTS; continues to refine features from 24 and enhances stability. Current latest patch 25.0.3. |
| 26             | 2026-03-17      | JDK 24             | Non-LTS    | Short-term support version, follows the latest JDK features. Current latest patch 26.0.1.        |

### Version Selection Recommendations

- **New projects (preferred)**: **JavaFX 25 LTS** (latest LTS, JDK 23+) — enjoy the latest features and long-term support.
- **Production / Conservative choice**: **JavaFX 21 LTS** (mature LTS, JDK 17+) — best ecosystem support.
- **Legacy system maintenance**: **JavaFX 17 LTS** (until Oct 2026) — suitable for projects that are inconvenient to upgrade.
- **Trying the latest features**: **JavaFX 26** (JDK 24+), but note the `--enable-native-access` flag requirement.
- **Long-term maintenance**: Follow the JDK LTS cadence, i.e., JavaFX 17 -> 21 -> 25.

---

## 2. Maven Configuration

### 2.1 javafx-maven-plugin 0.0.8

The `javafx-maven-plugin` is maintained by Gluon and simplifies running and debugging JavaFX applications. The currently recommended version is **0.0.8**.

Core goals (goals):

- `javafx:run`: Run the JavaFX application.
- `javafx:debug`: Run in debug mode, supports remote debugging.
- `javafx:jlink`: Create a custom runtime image and package it via jlink.

### 2.2 Complete pom.xml Example

The following is a complete Maven project configuration based on JavaFX 21, supporting cross-platform execution:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>javafx-app</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <javafx.version>21.0.11</javafx.version>
        <main.class>com.example.javafxapp.MainApp</main.class>
    </properties>

    <dependencies>
        <!-- JavaFX base modules (include as needed) -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <version>${javafx.version}</version>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-fxml</artifactId>
            <version>${javafx.version}</version>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-graphics</artifactId>
            <version>${javafx.version}</version>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-web</artifactId>
            <version>${javafx.version}</version>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-media</artifactId>
            <version>${javafx.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- JavaFX Maven Plugin -->
            <plugin>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-maven-plugin</artifactId>
                <version>0.0.8</version>
                <configuration>
                    <mainClass>${main.class}</mainClass>
                    <!-- JavaFX 24+ requires native access to be enabled -->
                    <options>
                        <option>--enable-native-access=javafx.graphics</option>
                    </options>
                </configuration>
            </plugin>

            <!-- Compiler Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <release>21</release>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2.3 Run Commands

```bash
# Run the application
mvn javafx:run

# Debug mode
mvn javafx:debug

# Create a jlink runtime image
mvn javafx:jlink
```

---

## 3. Gradle Configuration

### 3.1 org.openjfx.javafxplugin 0.1.0

Gradle users are recommended to use the official `org.openjfx.javafxplugin` plugin, currently at version **0.1.0**. This plugin automatically handles cross-platform dependency classifiers, eliminating the need to manually specify platform suffixes.

### 3.2 Complete build.gradle Example

```groovy
plugins {
    id 'java'
    id 'application'
    id 'org.openjfx.javafxplugin' version '0.1.0'
}

group = 'com.example'
version = '1.0.0'

repositories {
    mavenCentral()
}

javafx {
    version = "21.0.11"
    modules = ['javafx.controls', 'javafx.fxml', 'javafx.graphics', 'javafx.web', 'javafx.media']
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = 'com.example.javafxapp.MainApp'
    // JavaFX 24+ requires native access to be enabled
    applicationDefaultJvmArgs = ['--enable-native-access=javafx.graphics']
}

tasks.named('run') {
    // Ensure JVM args are passed at runtime
    jvmArgs += ['--enable-native-access=javafx.graphics']
}
```

### 3.3 Kotlin DSL (build.gradle.kts) Example

```kotlin
plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

javafx {
    version = "21.0.11"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "com.example.javafxapp.MainApp"
    applicationDefaultJvmArgs = listOf("--enable-native-access=javafx.graphics")
}
```

### 3.4 Run Commands

```bash
# Run the application
gradle run

# Build executable JAR
gradle build

# Create a jlink runtime image
gradle jlink
```

---

## 4. module-info.java Setup

JavaFX 11+ recommends using the Java Module System (JPMS). Declare module dependencies and exports in `src/main/java/module-info.java`.

### 4.1 Basic Structure

```java
module com.example.javafxapp {
    // Declare required JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;

    // Export packages containing public classes
    exports com.example.javafxapp;
    exports com.example.javafxapp.controller;
    exports com.example.javafxapp.model;

    // Open packages for javafx.fxml to load Controllers via reflection
    opens com.example.javafxapp.controller to javafx.fxml;
    opens com.example.javafxapp.view to javafx.fxml;

    // If using a dependency injection framework (e.g., Guice), open the relevant packages
    opens com.example.javafxapp to com.google.inject;
}
```

### 4.2 requires / exports / opens Keyword Reference

| Keyword              | Purpose                                                                 |
|----------------------|-------------------------------------------------------------------------|
| `requires`           | Declares other modules that this module depends on. Adding `transitive` indicates a transitive dependency. |
| `requires transitive` | The dependency is passed to modules that depend on this module.        |
| `exports`            | Exposes the public API of the specified package to the outside (visible at both compile time and runtime). |
| `opens`              | Opens the specified package for reflective access (deep reflection) at runtime, but does not expose it at compile time. |
| `opens ... to`       | Opens reflective access only to the specified module(s).                |
| `uses` / `provides`  | Service loader mechanism (ServiceLoader) related declarations.          |

### 4.3 Key Notes

1. **FXML loading requires `opens`**: `FXMLLoader` instantiates Controller classes via reflection, so you must use `opens` rather than `exports` to open the package containing Controllers.
2. **Do not `exports` packages containing FXML Controllers to `javafx.fxml`**; use `opens ... to javafx.fxml` instead.
3. **Resource files**: `module-info.java` must be located at the root of `src/main/java`, and resource files go in `src/main/resources`.

---

## 5. JDK 24+ --enable-native-access Requirement

Starting with JavaFX 24, JavaFX's graphics rendering layer (the `javafx.graphics` module) accesses native code via JNI. Under JDK 24+'s strict module encapsulation mechanism, native access must be explicitly granted, otherwise the application will fail to start and throw an `IllegalCallerException`.

### 5.1 How to Add

**Command line:**

```bash
java --enable-native-access=javafx.graphics -m com.example.javafxapp/com.example.javafxapp.MainApp
```

**Maven (javafx-maven-plugin):**

```xml
<configuration>
    <options>
        <option>--enable-native-access=javafx.graphics</option>
    </options>
</configuration>
```

**Gradle:**

```groovy
application {
    applicationDefaultJvmArgs = ['--enable-native-access=javafx.graphics']
}
```

### 5.2 Multi-module Scenario

If using other libraries that require native access (e.g., JNI libraries) at the same time, you can use `ALL-UNNAMED` or list multiple module names:

```bash
java --enable-native-access=javafx.graphics,com.example.nativeLib -m ...
```

> Note: Using `ALL-UNNAMED` reduces security and is only recommended for development and debugging phases.

---

## 6. Cross-platform Classifiers

JavaFX's `javafx.graphics` module contains platform-specific native binary libraries, so Maven dependencies need to distinguish platforms via classifiers.

### 6.1 Platform Classifier Reference

| Platform             | Classifier          | Notes                                                        |
|----------------------|---------------------|--------------------------------------------------------------|
| Windows x64          | `win`               | Auto-selected, corresponds to the Windows native library of `javafx.graphics` |
| Linux x64            | `linux`             | Linux platform native library                                |
| macOS x64 (Intel)    | `mac`               | macOS Intel platform                                         |
| macOS aarch64 (Apple Silicon) | `mac-aarch64` | macOS M1/M2/M3 chips                                         |
| Linux aarch64        | `linux-aarch64`     | Linux ARM64 platform                                         |
| Linux arm32          | `linux-arm32-monocle` | Embedded ARM platform (Monocle)                            |

### 6.2 Manually Specifying the Classifier in Maven

By default, `javafx-maven-plugin` and Maven automatically select the classifier based on the current build machine. If you need to package for a specific platform, you can specify it manually:

```xml
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-graphics</artifactId>
    <version>21.0.11</version>
    <classifier>linux</classifier>
</dependency>
```

### 6.3 Maven Cross-platform Profile Configuration

```xml
<profiles>
    <profile>
        <id>windows</id>
        <activation>
            <os><family>windows</family></os>
        </activation>
        <dependencies>
            <dependency>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-graphics</artifactId>
                <version>${javafx.version}</version>
                <classifier>win</classifier>
            </dependency>
        </dependencies>
    </profile>
    <profile>
        <id>linux</id>
        <activation>
            <os><family>linux</family></os>
        </activation>
        <dependencies>
            <dependency>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-graphics</artifactId>
                <version>${javafx.version}</version>
                <classifier>linux</classifier>
            </dependency>
        </dependencies>
    </profile>
    <profile>
        <id>mac</id>
        <activation>
            <os><family>mac</family></os>
        </activation>
        <dependencies>
            <dependency>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-graphics</artifactId>
                <version>${javafx.version}</version>
                <classifier>mac</classifier>
            </dependency>
        </dependencies>
    </profile>
</profiles>
```

> Tip: When using the Gradle `org.openjfx.javafxplugin` plugin, the classifier is handled automatically by the plugin and does not need to be configured manually.

---

## 7. jlink Custom Runtime Image

`jlink` is a JDK built-in tool that packages a modular Java application and its dependencies into a slim custom JRE image, without requiring the target machine to have a JDK pre-installed.

### 7.1 Prerequisites

- The project must use the Java Module System (i.e., include `module-info.java`).
- All dependencies must be modular JARs (or provide an `Automatic-Module-Name`).
- JavaFX's modular JARs meet this requirement.

### 7.2 Using the jlink Command

```bash
jlink \
  --module-path "mods:libs" \
  --add-modules com.example.javafxapp \
  --output build/javafx-runtime \
  --strip-debug \
  --compress zip-6 \
  --no-header-files \
  --no-man-pages \
  --bind-services
```

Parameter descriptions:

| Parameter          | Description                                                        |
|--------------------|--------------------------------------------------------------------|
| `--module-path`    | Module search path, containing application modules and JavaFX module JARs. |
| `--add-modules`    | Root modules to include in the image.                              |
| `--output`         | Output directory.                                                  |
| `--strip-debug`    | Strips debug information to reduce image size.                     |
| `--compress`       | Compression level (JDK 21+: `zip-0` to `zip-9`; older versions: `0`-`2`). |
| `--no-header-files`| Excludes header files.                                             |
| `--no-man-pages`   | Excludes man pages.                                                |
| `--bind-services`  | Links service providers (if ServiceLoader is needed).              |

### 7.3 Creating a jlink Image with javafx-maven-plugin

```xml
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>0.0.8</version>
    <configuration>
        <mainClass>${main.class}</mainClass>
        <options>
            <option>--enable-native-access=javafx.graphics</option>
        </options>
    </configuration>
    <executions>
        <execution>
            <id>default-cli</id>
            <goals>
                <goal>jlink</goal>
            </goals>
            <configuration>
                <stripDebug>true</stripDebug>
                <compress>zip-6</compress>
                <noHeaderFiles>true</noHeaderFiles>
                <noManPages>true</noManPages>
                <launcher>app=${main.class}</launcher>
                <jlinkImageName>javafx-runtime</jlinkImageName>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Execute:

```bash
mvn javafx:jlink
```

The generated image is located in `target/javafx-runtime/`, which contains a launcher script `bin/app` (or `bin/app.bat`).

### 7.4 Creating a jlink Image with Gradle

The Gradle `org.openjfx.javafxplugin` plugin provides a `jlink` task:

```bash
gradle jlink
```

It can be customized in `build.gradle`:

```groovy
jlink {
    options = ['--strip-debug', '--compress', 'zip-6', '--no-header-files', '--no-man-pages']
    launcher {
        name = 'app'
        jvmArgs = ['--enable-native-access=javafx.graphics']
    }
    jlinkBasePath = 'build'
    imageName = 'javafx-runtime'
}
```

### 7.5 Running the jlink Image

```bash
# Linux / macOS
./build/javafx-runtime/bin/app

# Windows
build\javafx-runtime\bin\app.bat
```

### 7.6 jlink Image Size Optimization Tips

1. **Include only necessary modules**: List only the modules actually needed in `--add-modules`.
2. **Enable compression**: Use `--compress zip-6` or higher.
3. **Strip debug information**: `--strip-debug`.
4. **Exclude unnecessary files**: `--no-header-files --no-man-pages`.
5. A typical JavaFX desktop application jlink image is about 40-80 MB (depending on the included modules).

---

## 8. Complete Project Directory Structure Reference

```
javafx-app/
├── pom.xml                      # Maven configuration (or build.gradle)
├── src/
│   └── main/
│       ├── java/
│       │   ├── module-info.java # Module declaration
│       │   └── com/
│       │       └── example/
│       │           └── javafxapp/
│       │               ├── MainApp.java          # Application entry point
│       │               ├── controller/
│       │               │   └── MainController.java
│       │               ├── model/
│       │               │   └── User.java
│       │               └── service/
│       │                   └── UserService.java
│       └── resources/
│           ├── fxml/
│           │   └── main.fxml
│           ├── css/
│           │   └── style.css
│           └── images/
│               └── icon.png
└── target/                      # Build output
```

---

## 9. MainApp Entry Class Example

```java
package com.example.javafxapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
            MainApp.class.getResource("/fxml/main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1024, 768);
        scene.getStylesheets().add(
            getClass().getResource("/css/style.css").toExternalForm());
        stage.setTitle("JavaFX Application");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

---

## 10. Common Troubleshooting

### Q1: "module not found" Error at Runtime

Ensure that `module-info.java` has `requires` for all JavaFX modules in use, and that the dependencies are correctly declared in `pom.xml` / `build.gradle`.

### Q2: FXML Loading Reports "Cannot load FXMLLoader" or Reflection Errors

Check whether `module-info.java` has `opens ... to javafx.fxml` for the Controller package.

### Q3: JavaFX 24 Reports IllegalCallerException on Startup

Add the `--enable-native-access=javafx.graphics` JVM argument.

### Q4: Missing Native Libraries in Cross-platform Builds

Confirm that the `javafx-graphics` dependency uses the correct platform classifier, or use the Gradle plugin to handle it automatically.

### Q5: jlink Reports "Module not found" or Automatic Module Errors

jlink requires all dependencies to be explicit modules or automatic modules. If a dependency is an unnamed module (no `Automatic-Module-Name`), it must be wrapped as an automatic module or `jpackage` should be used instead.

### Q6: "JavaFX runtime components are missing" Error at Runtime

**Scenario**: Spring Boot + JavaFX integration project, error occurs when running via `mvn spring-boot:run` or `java -jar`.

**Cause**: The main class (the class containing the `main` method) directly `extends Application`, so the JVM uses the JavaFX-specific launcher to run. This launcher requires `javafx.graphics` to exist as a named module on the module path. In classpath mode (without `module-info.java`), the JavaFX JARs are placed on the classpath, and the launcher cannot find them.

**Solution**: Split the startup class into two classes -- a Spring Boot startup class (not extending Application) + a JavaFX entry class (extending Application). See section 3 of `references/spring-boot-integration.md` for details.

```java
// Startup class: does not extend Application
@SpringBootApplication
public class MyApp {
    static ConfigurableApplicationContext springContext;
    public static void main(String[] args) {
        springContext = SpringApplication.run(MyApp.class, args);
        Application.launch(JavaFXApp.class, args);
    }
}

// JavaFX entry class: extends Application
public class JavaFXApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        loader.setControllerFactory(MyApp.springContext::getBean);
        stage.setScene(new Scene(loader.load()));
        stage.show();
    }
}
```

> Note: Pure JavaFX projects running with `mvn javafx:run` do not trigger this error because `javafx-maven-plugin` places the JavaFX JARs on the module path. This issue only occurs in classpath mode (Spring Boot integration, `java -jar` execution).

### Q7: IDE Compilation Reports "ExceptionInInitializerError" + "TypeTag :: UNKNOWN"

**Scenario**: The following error occurs when compiling or running the project in IntelliJ IDEA:
```
java: java.lang.ExceptionInInitializerError
com.sun.tools.javac.code.TypeTag :: UNKNOWN
```
while command-line `mvn compile` compiles normally.

**Cause**: The Project JDK selected in the IDE does not match the JDK version actually required by the project. Lombok accesses the JDK compiler's internal API (`com.sun.tools.javac.code.TypeTag`) via reflection to generate code. When the JDK version used by the IDE is incompatible with the Lombok plugin, this error is triggered.

**Solution**:
1. **Check the IDE's Project JDK**: File -> Project Structure -> Project SDK, ensure it matches the `java.version` configured in `pom.xml`.
2. **Update the Lombok plugin**: Settings -> Plugins -> search for Lombok -> update to the latest version.
3. **Check the Maven importer JDK**: Settings -> Build -> Build Tools -> Maven -> Importing -> JDK for importer, ensure it matches the project.

> **Tip**: This is a classic error caused by a mismatch between Lombok and the JDK version. Lombok 1.18.30+ supports JDK 21, but the IDE's Lombok plugin may not have been updated accordingly. Ensure that the IDE's JDK selection, the Lombok plugin version, and the `java.version` in `pom.xml` are all consistent.

### Q8: Blurry UI on HiDPI / 4K Screens

**Scenario**: On high-resolution screens (4K monitors, Retina displays), the JavaFX application UI is blurry or fonts are unclear.

**Cause**: JavaFX supports HiDPI scaling by default, but in some cases HiDPI is explicitly disabled or the display is not detected correctly.

**Solution**:
1. Make sure **not** to set `-Dprism.allowhidpi=false` (the default is true; explicitly setting it to false disables HiDPI).
2. On Windows, check the scaling settings: right-click the app -> Properties -> Compatibility -> Change high DPI settings -> check "Override high DPI scaling behavior" -> select "Application".
3. If using a `.properties` configuration, you can add `-Dprism.allowhidpi=true` to ensure it is enabled.

### Q9: Chinese Characters Show as Squares (Tofu) on Linux

**Scenario**: Running a JavaFX application on a Linux system, Chinese characters display as squares or garbled text.

**Cause**: The system does not have Chinese fonts installed, or the JavaFX font rendering engine (Pango/Freetype) cannot find Chinese fonts.

**Solution**:
1. Install Chinese fonts: `sudo apt install fonts-noto-cjk` or `sudo apt install fonts-wqy-microhei`.
2. Confirm fontconfig is configured correctly: `fc-list :lang=zh` should list Chinese fonts.
3. If using a container (Docker), install fonts in the image.

### Q10: Non-modular Project Reports "JavaFX runtime components missing" with `java -jar`

**Scenario**: A non-modular project (no `module-info.java`), built as a fat JAR and run with `java -jar app.jar`, reports an error.

**Cause**: A normal JAR from a non-modular project does not contain the JavaFX modules, and `java -jar` does not automatically add JavaFX to the module path.

**Solution**: Build a fat JAR with `maven-shade-plugin`, and specify the JavaFX SDK path via `--module-path` at startup:
```bash
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar app.jar
```
Or use `maven-shade-plugin` to bundle all dependencies (including JavaFX) into a fat JAR, but note that JavaFX's platform native libraries still need additional configuration.
