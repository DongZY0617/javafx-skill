# JavaFX 项目搭建指南

本指南涵盖 JavaFX 项目的完整搭建流程，包括版本选择、构建工具配置（Maven / Gradle）、模块化设置、跨平台打包以及自定义运行时镜像（jlink）的创建。

---

## 一、JavaFX 版本路线图

JavaFX 自 OpenJFX 项目独立维护以来，采用每六个月一次的发布节奏（与 JDK LTS 节奏对齐）。下表列出了主要版本的发布日期、LTS 状态及关键特性。

| JavaFX 版本 | 发布日期        | 对应 JDK | LTS 状态 | 关键特性 / 说明                                                     |
|-------------|-----------------|----------|----------|---------------------------------------------------------------------|
| 17          | 2021-09-14      | JDK 17   | LTS（至 2026.10）| 长期支持版本，稳定成熟；第三方库兼容性最佳，适合遗留系统维护。    |
| 21          | 2023-09-19      | JDK 17   | LTS      | LTS 版本；新增 subscription-based listener API、性能与稳定性改进。  |
| 24          | 2025-03-18      | JDK 22   | 非 LTS   | 引入 `--enable-native-access` 要求以访问本地渲染层；多显示器改进。   |
| 25          | 2025-09-16      | JDK 23   | LTS      | 最新 LTS；继续完善 24 的特性，增强稳定性。当前最新 patch 25.0.3。   |
| 26          | 2026-03-17      | JDK 24   | 非 LTS   | 短期支持版本，跟随最新 JDK 特性。当前最新 patch 26.0.1。            |

### 版本选择建议

- **新项目首选**：**JavaFX 25 LTS**（最新 LTS，JDK 23+），享受最新特性与长期支持。
- **生产环境 / 保守方案**：**JavaFX 21 LTS**（成熟 LTS，JDK 17+），生态支持最佳。
- **遗留系统维护**：**JavaFX 17 LTS**（至 2026.10），适合不便升级的项目。
- **尝鲜最新特性**：**JavaFX 26**（JDK 24+），但需注意 `--enable-native-access` 标志要求。
- **长期维护**：跟随 JDK LTS 节奏，即 JavaFX 17 → 21 → 25。

---

## 二、Maven 配置

### 2.1 javafx-maven-plugin 0.0.8

`javafx-maven-plugin` 由 Gluon 维护，用于简化 JavaFX 应用的运行与调试。当前推荐版本为 **0.0.8**。

核心目标（goal）：

- `javafx:run`：运行 JavaFX 应用程序。
- `javafx:debug`：以调试模式运行，支持远程调试。
- `javafx:jlink`：通过 jlink 创建自定义运行时镜像并打包。

### 2.2 完整 pom.xml 示例

以下是一个基于 JavaFX 21 的完整 Maven 项目配置，支持跨平台运行：

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
        <!-- JavaFX 基础模块（按需引入） -->
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
            <!-- JavaFX Maven 插件 -->
            <plugin>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-maven-plugin</artifactId>
                <version>0.0.8</version>
                <configuration>
                    <mainClass>${main.class}</mainClass>
                    <!-- JavaFX 24+ 需要启用本地访问 -->
                    <options>
                        <option>--enable-native-access=javafx.graphics</option>
                    </options>
                </configuration>
            </plugin>

            <!-- 编译插件 -->
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

### 2.3 运行命令

```bash
# 运行应用
mvn javafx:run

# 调试模式
mvn javafx:debug

# 创建 jlink 运行时镜像
mvn javafx:jlink
```

---

## 三、Gradle 配置

### 3.1 org.openjfx.javafxplugin 0.1.0

Gradle 用户推荐使用官方 `org.openjfx.javafxplugin` 插件，当前版本为 **0.1.0**。该插件会自动处理跨平台依赖分类器（classifier），无需手动指定平台后缀。

### 3.2 完整 build.gradle 示例

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
    // JavaFX 24+ 需要启用本地访问
    applicationDefaultJvmArgs = ['--enable-native-access=javafx.graphics']
}

tasks.named('run') {
    // 确保运行时传入 JVM 参数
    jvmArgs += ['--enable-native-access=javafx.graphics']
}
```

### 3.3 Kotlin DSL (build.gradle.kts) 示例

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

### 3.4 运行命令

```bash
# 运行应用
gradle run

# 构建可执行 JAR
gradle build

# 创建 jlink 运行时镜像
gradle jlink
```

---

## 四、module-info.java 设置

JavaFX 11+ 推荐使用 Java 模块系统（JPMS）。在 `src/main/java/module-info.java` 中声明模块依赖与导出。

### 4.1 基本结构

```java
module com.example.javafxapp {
    // 声明依赖的 JavaFX 模块
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;

    // 导出包含公共类的包
    exports com.example.javafxapp;
    exports com.example.javafxapp.controller;
    exports com.example.javafxapp.model;

    // 开放包给 javafx.fxml 使用反射加载 Controller
    opens com.example.javafxapp.controller to javafx.fxml;
    opens com.example.javafxapp.view to javafx.fxml;

    // 如使用依赖注入框架（如 Guice），需开放相关包
    opens com.example.javafxapp to com.google.inject;
}
```

### 4.2 requires / exports / opens 关键字说明

| 关键字      | 作用                                                                 |
|-------------|----------------------------------------------------------------------|
| `requires`  | 声明本模块依赖的其他模块。加 `transitive` 表示传递依赖。             |
| `requires transitive` | 依赖会传递给依赖本模块的模块。                              |
| `exports`   | 将指定包的公共 API 对外暴露（编译期和运行期均可见）。                |
| `opens`     | 在运行期开放指定包供反射访问（深度反射），但编译期不暴露。           |
| `opens ... to` | 仅对指定模块开放反射访问。                                        |
| `uses` / `provides` | 服务加载机制（ServiceLoader）相关声明。                       |

### 4.3 关键注意事项

1. **FXML 加载需要 `opens`**：`FXMLLoader` 通过反射实例化 Controller 类，必须用 `opens` 而非 `exports` 开放 Controller 所在包。
2. **不要 `exports` 含 FXML Controller 的包给 `javafx.fxml`**，应使用 `opens ... to javafx.fxml`。
3. **资源文件**：`module-info.java` 必须位于 `src/main/java` 根目录，资源文件放在 `src/main/resources`。

---

## 五、JDK 24+ --enable-native-access 要求

从 JavaFX 24 开始，JavaFX 的图形渲染层（`javafx.graphics` 模块）通过 JNI 访问本地代码。在 JDK 24+ 的严格模块封装机制下，必须显式授予本地访问权限，否则应用将无法启动并抛出 `IllegalCallerException`。

### 5.1 添加方式

**命令行方式：**

```bash
java --enable-native-access=javafx.graphics -m com.example.javafxapp/com.example.javafxapp.MainApp
```

**Maven 方式（javafx-maven-plugin）：**

```xml
<configuration>
    <options>
        <option>--enable-native-access=javafx.graphics</option>
    </options>
</configuration>
```

**Gradle 方式：**

```groovy
application {
    applicationDefaultJvmArgs = ['--enable-native-access=javafx.graphics']
}
```

### 5.2 多模块场景

如果同时使用其他需要本地访问的库（如 JNI 库），可使用 `ALL-UNNAMED` 或列出多个模块名：

```bash
java --enable-native-access=javafx.graphics,com.example.nativeLib -m ...
```

> 注意：使用 `ALL-UNNAMED` 会降低安全性，仅推荐用于开发调试阶段。

---

## 六、跨平台分类器（Classifier）说明

JavaFX 的 `javafx.graphics` 模块包含平台相关的本地二进制库，因此 Maven 依赖需要通过分类器区分平台。

### 6.1 平台分类器对照表

| 平台                 | Classifier     | 说明                          |
|----------------------|----------------|-------------------------------|
| Windows x64          | `win`          | 自动选择，对应 `javafx.graphics` 的 Windows 本地库 |
| Linux x64            | `linux`        | Linux 平台本地库              |
| macOS x64 (Intel)    | `mac`          | macOS Intel 平台              |
| macOS aarch64 (Apple Silicon) | `mac-aarch64` | macOS M1/M2/M3 芯片 |
| Linux aarch64        | `linux-aarch64`| Linux ARM64 平台              |
| Linux arm32          | `linux-arm32-monocle` | 嵌入式 ARM 平台（Monocle）|

### 6.2 Maven 手动指定分类器

默认情况下，`javafx-maven-plugin` 和 Maven 会根据当前构建机器自动选择分类器。若需为特定平台打包，可手动指定：

```xml
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-graphics</artifactId>
    <version>21.0.4</version>
    <classifier>linux</classifier>
</dependency>
```

### 6.3 Maven 跨平台 Profile 配置

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

> 提示：使用 Gradle 的 `org.openjfx.javafxplugin` 插件时，分类器由插件自动处理，无需手动配置。

---

## 七、jlink 自定义运行时镜像

`jlink` 是 JDK 内置工具，可将模块化 Java 应用及其依赖打包为一个精简的自定义 JRE 镜像，无需目标机器预装 JDK。

### 7.1 前提条件

- 项目必须使用 Java 模块系统（即包含 `module-info.java`）。
- 所有依赖必须是模块化 JAR（或提供 `Automatic-Module-Name`）。
- JavaFX 的模块化 JAR 满足此要求。

### 7.2 使用 jlink 命令

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

参数说明：

| 参数                | 说明                                                       |
|---------------------|------------------------------------------------------------|
| `--module-path`     | 模块搜索路径，包含应用模块和 JavaFX 模块 JAR。             |
| `--add-modules`     | 要包含在镜像中的根模块。                                   |
| `--output`          | 输出目录。                                                 |
| `--strip-debug`     | 去除调试信息，减小镜像体积。                               |
| `--compress`        | 压缩级别（JDK 21+：`zip-0` 到 `zip-9`；旧版本：`0`-`2`）。 |
| `--no-header-files` | 排除头文件。                                               |
| `--no-man-pages`    | 排除 man 手册页。                                          |
| `--bind-services`   | 链接服务提供者（如需要 ServiceLoader）。                   |

### 7.3 使用 javafx-maven-plugin 创建 jlink 镜像

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

执行：

```bash
mvn javafx:jlink
```

生成的镜像位于 `target/javafx-runtime/`，其中包含一个启动脚本 `bin/app`（或 `bin/app.bat`）。

### 7.4 使用 Gradle 创建 jlink 镜像

Gradle 的 `org.openjfx.javafxplugin` 插件提供 `jlink` 任务：

```bash
gradle jlink
```

可在 `build.gradle` 中自定义：

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

### 7.5 运行 jlink 镜像

```bash
# Linux / macOS
./build/javafx-runtime/bin/app

# Windows
build\javafx-runtime\bin\app.bat
```

### 7.6 jlink 镜像体积优化建议

1. **仅引入必要模块**：`--add-modules` 只列出实际需要的模块。
2. **启用压缩**：使用 `--compress zip-6` 或更高。
3. **去除调试信息**：`--strip-debug`。
4. **排除不必要文件**：`--no-header-files --no-man-pages`。
5. 一个典型的 JavaFX 桌面应用 jlink 镜像体积约为 40-80 MB（取决于包含的模块）。

---

## 八、完整项目目录结构参考

```
javafx-app/
├── pom.xml                      # Maven 配置（或 build.gradle）
├── src/
│   └── main/
│       ├── java/
│       │   ├── module-info.java # 模块声明
│       │   └── com/
│       │       └── example/
│       │           └── javafxapp/
│       │               ├── MainApp.java          # Application 入口
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
└── target/                      # 构建输出
```

---

## 九、MainApp 入口类示例

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
        stage.setTitle("JavaFX 应用");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

---

## 十、常见问题排查

### Q1: 运行时报 "module not found" 错误

确保 `module-info.java` 中 `requires` 了所有使用的 JavaFX 模块，且依赖在 `pom.xml` / `build.gradle` 中正确声明。

### Q2: FXML 加载报 "Cannot load FXMLLoader" 或反射错误

检查 `module-info.java` 是否对 Controller 包执行了 `opens ... to javafx.fxml`。

### Q3: JavaFX 24 启动报 IllegalCallerException

添加 `--enable-native-access=javafx.graphics` JVM 参数。

### Q4: 跨平台构建时本地库缺失

确认 `javafx-graphics` 依赖使用了正确的平台分类器，或使用 Gradle 插件自动处理。

### Q5: jlink 报 "Module not found" 或自动模块错误

jlink 要求所有依赖都是显式模块或自动模块。若依赖为未命名模块（无 `Automatic-Module-Name`），需将其包装为自动模块或使用 `jpackage` 替代。

### Q6: 运行时报 "JavaFX runtime components are missing"

**场景**：Spring Boot + JavaFX 整合项目，通过 `mvn spring-boot:run` 或 `java -jar` 运行时报错。

**原因**：主类（包含 `main` 方法的类）直接 `extends Application`，JVM 使用 JavaFX 专用启动器运行，该启动器要求 `javafx.graphics` 作为命名模块存在于模块路径上。在 classpath 模式下（无 `module-info.java`），JavaFX JAR 放在 classpath 上，启动器找不到它们。

**解决**：将启动类拆分为两个类 —— Spring Boot 启动类（不继承 Application）+ JavaFX 入口类（继承 Application）。详见 `references/spring-boot-integration.md` 第三节。

```java
// 启动类：不继承 Application
@SpringBootApplication
public class MyApp {
    static ConfigurableApplicationContext springContext;
    public static void main(String[] args) {
        springContext = SpringApplication.run(MyApp.class, args);
        Application.launch(JavaFXApp.class, args);
    }
}

// JavaFX 入口类：继承 Application
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

> 注意：纯 JavaFX 项目使用 `mvn javafx:run` 运行时不会触发此错误，因为 `javafx-maven-plugin` 会将 JavaFX JAR 放到模块路径上。此问题仅在 classpath 模式下（Spring Boot 整合、`java -jar` 运行）出现。

### Q7: IDE 编译报 "ExceptionInInitializerError" + "TypeTag :: UNKNOWN"

**场景**：在 IntelliJ IDEA 中编译或运行项目时报以下错误：
```
java: java.lang.ExceptionInInitializerError
com.sun.tools.javac.code.TypeTag :: UNKNOWN
```
而命令行 `mvn compile` 编译正常。

**原因**：IDE 中选择的 Project JDK 与项目实际需要的 JDK 版本不一致。Lombok 通过反射访问 JDK 编译器内部 API（`com.sun.tools.javac.code.TypeTag`）来生成代码，当 IDE 使用的 JDK 版本与 Lombok 插件不兼容时，会触发此错误。

**解决**：
1. **检查 IDE 的 Project JDK**：File → Project Structure → Project SDK，确保与 `pom.xml` 中配置的 `java.version` 一致。
2. **更新 Lombok 插件**：Settings → Plugins → 搜索 Lombok → 更新到最新版本。
3. **检查 Maven 导入的 JDK**：Settings → Build → Build Tools → Maven → Importing → JDK for importer，确保与项目一致。

> **提示**：这是 Lombok 与 JDK 版本不匹配的经典错误。Lombok 1.18.30+ 支持 JDK 21，但 IDE 的 Lombok 插件可能未同步更新。确保 IDE 的 JDK 选择、Lombok 插件版本、`pom.xml` 中的 `java.version` 三者一致。

### Q8: HiDPI / 4K 屏幕下界面模糊

**场景**：在高分辨率屏幕（4K 显示器、Retina 屏）上，JavaFX 应用界面模糊或字体不清晰。

**原因**：JavaFX 默认支持 HiDPI 缩放，但某些情况下显式禁用了 HiDPI 或未正确检测显示器。

**解决**：
1. 确保**不要**设置 `-Dprism.allowhidpi=false`（默认为 true，显式设为 false 会禁用 HiDPI）。
2. Windows 上检查缩放设置：右键应用 → 属性 → 兼容性 → 更改高 DPI 设置 → 勾选"替代高 DPI 缩放行为"→ 选择"应用程序"。
3. 如果使用 `.properties` 配置，可添加 `-Dprism.allowhidpi=true` 确保启用。

### Q9: Linux 下中文字体显示为方块（豆腐块）

**场景**：在 Linux 系统上运行 JavaFX 应用，中文显示为方块或乱码。

**原因**：系统未安装中文字体，或 JavaFX 字体渲染引擎（Pango/Freetype）未找到中文字体。

**解决**：
1. 安装中文字体：`sudo apt install fonts-noto-cjk` 或 `sudo apt install fonts-wqy-microhei`。
2. 确认 fontconfig 配置正确：`fc-list :lang=zh` 应能列出中文字体。
3. 如果使用容器（Docker），需在镜像中安装字体。

### Q10: 非模块化项目用 `java -jar` 运行报 "缺少 JavaFX 运行时组件"

**场景**：非模块化项目（无 `module-info.java`），构建为 fat JAR 后用 `java -jar app.jar` 运行报错。

**原因**：非模块化项目的普通 JAR 不包含 JavaFX 模块，`java -jar` 不会自动将 JavaFX 添加到模块路径。

**解决**：使用 `maven-shade-plugin` 构建 fat JAR，并在启动时通过 `--module-path` 指定 JavaFX SDK 路径：
```bash
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar app.jar
```
或使用 `maven-shade-plugin` 将所有依赖（含 JavaFX）打入 fat JAR，但需注意 JavaFX 的平台本地库仍需额外配置。
