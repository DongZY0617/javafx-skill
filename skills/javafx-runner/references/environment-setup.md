# 环境检测与 Monocle Headless 配置

本文档是 `javafx-runner` 环境检测与 headless 运行配置的操作指南，对应工作流步骤 1（环境检测与上下文分析）。提供 JDK / Maven / Gradle / JavaFX 版本检测方法、模块化与显示器检测、Monocle 依赖配置、headless 运行命令及 CI 环境适配方案。本文件不定义检查项的通过/不通过标准，而是为前三个验证维度（编译 / 运行 / 打包）提供环境前置条件。

> **核心原则**：环境检测是所有验证维度的前置步骤。JavaFX 版本决定是否执行 JavaFX 24+ 原生访问检查；显示器有无决定是否启用 Monocle headless 模式；构建工具（Maven/Gradle）决定执行的命令；平台决定 jpackage 工具链检测项。检测失败时应在报告中标注环境缺失，而非直接判定代码问题。

---

## 1. JDK 版本检测

**用途**：确认 JDK 满足 JavaFX 最低要求（17+），决定可用的 JavaFX 版本范围与 `jpackage` 可用性（JDK 14+ 内置）。

**检测命令**：
```bash
java -version
```
输出示例：
```
openjdk version "21.0.2" 2024-01-16
OpenJDK Runtime Environment (build 21.0.2+13)
OpenJDK Runtime Environment Temurin-21.0.2+13 (build 21.0.2+13)
```

**版本提取与判定**：
- 解析输出第一行的 `version "xx.x.x"` 提取主版本号
- JDK 17+：满足 JavaFX 17/21 运行要求
- JDK 21+：满足 JavaFX 21/24/25/26 运行要求
- JDK 24+：满足 JavaFX 24/25/26 运行要求与原生访问特性
- JDK < 17：不满足，报告中标注"JDK 版本过低，需升级至 17+"

**`JAVA_HOME` 检测**：
```powershell
# Windows PowerShell
$env:JAVA_HOME
java -version
```
```bash
# Linux / macOS
echo $JAVA_HOME
java -version
```

---

## 2. Maven / Gradle 检测

**用途**：识别项目构建工具，决定后续执行的编译 / 运行 / 打包命令。

### 2.1 构建工具识别

**检测方法**：检查项目根目录下的构建文件。
- 存在 `pom.xml` → Maven 项目
- 存在 `build.gradle` 或 `build.gradle.kts` → Gradle 项目
- 两者均存在 → 优先 Maven（或向用户确认）

### 2.2 Maven 检测

**检测命令**：
```bash
mvn -version
```
输出示例：
```
Apache Maven 3.9.6
Maven home: C:\apache-maven-3.9.6
Java version: 21.0.2, vendor: Eclipse Adoptium
```

**版本要求**：Maven 3.8+。低于 3.8 时部分 JavaFX 插件可能不兼容。

### 2.3 Gradle 检测

**检测命令**：
```bash
# 使用项目自带的 gradlew（推荐）
./gradlew --version        # Linux / macOS
.\gradlew.bat --version    # Windows PowerShell
```
输出示例：
```
Gradle 8.5
Kotlin:       1.9.20
Groovy:       3.0.19
```

**版本要求**：Gradle 7+。低于 7 时 `javafx-gradle-plugin` 可能不兼容。

---

## 3. JavaFX 版本提取

**用途**：确定项目使用的 JavaFX 版本，动态调整验证项（如 JavaFX 24+ 检查 `--enable-native-access`）。

### 3.1 Maven 项目提取

**检测方法**：解析 `pom.xml` 中 JavaFX 依赖的版本。

```bash
# 提取 javafx-controls 依赖版本
grep -A 1 "javafx-controls" pom.xml | grep -oP '(?<=<version>)[^<]+'
```

`pom.xml` 示例：
```xml
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>21.0.2</version>
</dependency>
```
提取结果：`21.0.2` → 主版本 21。

### 3.2 Gradle 项目提取

**检测方法**：解析 `build.gradle` 中 JavaFX 插件配置。

```groovy
// build.gradle
javafx {
    version = "21.0.2"
    modules = ['javafx.controls', 'javafx.fxml']
}
```

```bash
grep "version" build.gradle | grep -i javafx
```

### 3.3 版本判定与验证项映射

| JavaFX 主版本 | 原生访问检查 | 兼容 JDK |
|--------------|-------------|---------|
| 17 | 不执行 | 17+ |
| 21 | 不执行 | 17+ |
| 24 | 执行（检查项 7：JavaFX 24+ 原生访问） | 21+（建议 24+） |
| 25 / 26 | 执行 | 24+ |

---

## 4. 模块化检测

**用途**：确定项目是否为模块化项目，决定运行命令与 jpackage 参数（`--module` vs `--main-jar`）。

**检测方法**：检查 `src/main/java/module-info.java` 是否存在。
```bash
# 检测 module-info.java
test -f src/main/java/module-info.java && echo "模块化项目" || echo "非模块化项目"
```

**判定与命令映射**：
- **模块化项目**（存在 `module-info.java`）：
  - 运行：`mvn javafx:run`（插件自动识别模块）
  - 打包：`jpackage --module com.example.app/com.example.App --module-path ...`
- **非模块化项目**（无 `module-info.java`）：
  - 运行：`mvn javafx:run`（插件以 classpath 模式运行）
  - 打包：`jpackage --main-jar app.jar --main-class com.example.App --input target`

---

## 5. 显示器检测

**用途**：判断当前环境是否有显示器，决定是否启用 Monocle headless 模式。

### 5.1 各平台检测方法

**Linux**：
```bash
# 检测 DISPLAY 环境变量
echo $DISPLAY
# 有值（如 :0）→ 有显示器；空值 → 无显示器（需 headless）
```

**Windows**：
```powershell
# 检测是否有交互式桌面会话
[Environment]::UserInteractive
# 检测 SessionName
$env:SESSIONNAME
# CI 环境（如 GitHub Actions）通常无桌面会话
```

**macOS**：
```bash
# 检测 WindowServer 是否运行
pgrep -x WindowServer > /dev/null && echo "有显示器" || echo "无显示器"
```

### 5.2 CI 环境判定

CI 环境（GitHub Actions、GitLab CI、Jenkins 无显示器节点）通常无显示器，必须启用 Monocle headless 模式：
```bash
# GitHub Actions / GitLab CI 等容器环境
# DISPLAY 为空或 SessionName 为空 → 判定无显示器
# 自动启用 -Dmonocle.platform=Headless -Dprism.order=sw
```

---

## 6. Monocle 依赖配置

**用途**：在无显示器环境（CI、Docker）中运行 JavaFX 应用，替代 Glass 窗口工具包。

> **版本匹配关键**：Monocle 版本必须与 JavaFX 版本严格匹配。Monocle 依赖 JavaFX 内部 API，版本不匹配会因内部 API 变更报 `NoClassDefFoundError`。下表对应关系须严格遵守。

### 6.1 Monocle 版本对应表

| JavaFX 版本 | Monocle 依赖 | 备注 |
|------------|-------------|------|
| 17 | `org.testfx:testfx-monocle:17.0.2` 或 `org.openjfx:javafx-monocle:17` | testfx-monocle 封装 openjfx monocle |
| 21 | `org.testfx:testfx-monocle:21.0.2` | 与 JavaFX 21 严格匹配 |
| 24 / 25 / 26 | `org.testfx:testfx-monocle:24.0.0+` | 需匹配主版本 |

### 6.2 Maven 配置

在 `pom.xml` 中添加 Monocle 依赖（scope 为 `test` 或按需调整）：
```xml
<dependencies>
    <!-- JavaFX 主依赖 -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>21.0.2</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>21.0.2</version>
    </dependency>

    <!-- Monocle headless 支持（版本与 JavaFX 严格匹配） -->
    <dependency>
        <groupId>org.testfx</groupId>
        <artifactId>testfx-monocle</artifactId>
        <version>21.0.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 6.3 Gradle 配置

在 `build.gradle` 中添加 Monocle 依赖：
```groovy
dependencies {
    implementation 'org.openjfx:javafx-controls:21.0.2'
    implementation 'org.openjfx:javafx-fxml:21.0.2'

    // Monocle headless 支持（版本与 JavaFX 严格匹配）
    testImplementation 'org.testfx:testfx-monocle:21.0.2'
}
```

`build.gradle.kts`（Kotlin DSL）：
```kotlin
dependencies {
    implementation("org.openjfx:javafx-controls:21.0.2")
    implementation("org.openjfx:javafx-fxml:21.0.2")

    testImplementation("org.testfx:testfx-monocle:21.0.2")
}
```

---

## 7. Headless 运行命令

**用途**：在无显示器环境通过 Monocle 启动 JavaFX 应用，执行运行验证。

### 7.1 Maven headless 运行

```bash
mvn javafx:run -Dmonocle.platform=Headless -Dprism.order=sw
```

**参数说明**：
- `-Dmonocle.platform=Headless`：启用 Monocle headless 平台，替代 Glass 窗口工具包
- `-Dprism.order=sw`：强制使用软件渲染管线（不依赖 GPU）

**通过 `pom.xml` 的 `javafx-maven-plugin` 配置**：
```xml
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>0.0.8</version>
    <configuration>
        <mainClass>com.example.App</mainClass>
        <options>
            <option>-Dmonocle.platform=Headless</option>
            <option>-Dprism.order=sw</option>
            <!-- JavaFX 24+ 额外配置 -->
            <option>--enable-native-access=javafx.graphics</option>
        </options>
    </configuration>
</plugin>
```

### 7.2 Gradle headless 运行

```bash
./gradlew run -Pmonocle.platform=Headless -Pprism.order=sw
# 或通过 JVM args 配置
./gradlew run -Djavafx.options="-Dmonocle.platform=Headless -Dprism.order=sw"
```

`build.gradle` 配置：
```groovy
run {
    if (project.hasProperty('headless')) {
        jvmArgs = ['-Dmonocle.platform=Headless', '-Dprism.order=sw']
    }
}
```

### 7.3 直接 java 命令 headless 运行

```bash
# 模块化项目
java -Dmonocle.platform=Headless -Dprism.order=sw \
     --module-path "target/modules;C:\javafx-sdk-21\lib" \
     --add-modules javafx.controls,javafx.fxml,javafx.graphics \
     --module com.example.app/com.example.App

# JavaFX 24+ 追加原生访问
java -Dmonocle.platform=Headless -Dprism.order=sw \
     --enable-native-access=javafx.graphics \
     --module-path "target/modules" \
     --module com.example.app/com.example.App
```

---

## 8. CI 环境适配

**用途**：在 CI 流水线（无显示器）中执行 JavaFX 运行验证，确保代码在合并前通过动态验证。

### 8.1 GitHub Actions 示例

`.github/workflows/javafx-verify.yml`：
```yaml
name: JavaFX Verify

on: [push, pull_request]

jobs:
  verify:
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
        java: [21]

    steps:
      - uses: actions/checkout@v4

      - name: Setup JDK
        uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
          distribution: 'temurin'

      - name: Setup Maven
        uses: stCarolas/setup-maven@v4.5
        with:
          maven-version: 3.9.6

      # Linux 安装 headless 依赖（Xvfb 虚拟帧缓冲，Monocle 替代方案）
      - name: Install headless deps (Linux)
        if: runner.os == 'Linux'
        run: |
          sudo apt-get update
          sudo apt-get install -y xvfb

      # 编译验证（全平台通用）
      - name: Compile verification
        run: mvn compile -q

      # 运行验证 - Linux 使用 Xvfb 虚拟显示器
      - name: Runtime verification (Linux)
        if: runner.os == 'Linux'
        run: xvfb-run mvn javafx:run -Dmonocle.platform=Headless -Dprism.order=sw
        timeout-minutes: 1

      # 运行验证 - Windows / macOS（CI 桌面会话）
      - name: Runtime verification (Windows/macOS)
        if: runner.os != 'Linux'
        run: mvn javafx:run -Dmonocle.platform=Headless -Dprism.order=sw
        timeout-minutes: 1

      # 打包验证（按平台安装工具链）
      - name: Install Inno Setup (Windows)
        if: runner.os == 'Windows'
        run: choco install innosetup -y

      - name: Install Xcode tools (macOS)
        if: runner.os == 'macOS'
        run: xcode-select --install || true

      - name: Install dpkg-deb (Linux)
        if: runner.os == 'Linux'
        run: sudo apt-get install -y dpkg-dev

      - name: Packaging verification
        run: mvn package -DskipTests
        timeout-minutes: 10
```

### 8.2 CI 环境注意事项

- **超时保护**：运行验证设置 `timeout-minutes: 1`（运行命令内部 30 秒超时 + 缓冲），打包验证设置 `timeout-minutes: 10`，避免卡死流水线
- **Linux Xvfb**：除 Monocle 外，Linux CI 可用 `xvfb-run` 提供虚拟帧缓冲，二者择一
- **工具链缓存**：Windows Inno Setup 安装较慢，建议使用 `actions/cache` 缓存 Chocolatey 包
- **JavaFX SDK 路径**：CI 中 JavaFX SDK 路径需与 `--module-path` 一致，建议通过环境变量注入
- **退出码传递**：CI 默认以非零退出码标记失败，runner 的退出码检查与 CI 状态联动

### 8.3 GitLab CI 示例

`.gitlab-ci.yml`：
```yaml
javafx-verify:
  image: maven:3.9-eclipse-temurin-21
  script:
    - apt-get update && apt-get install -y xvfb
    - mvn compile -q
    - xvfb-run mvn javafx:run -Dmonocle.platform=Headless -Dprism.order=sw
    - mvn package -DskipTests
  timeout: 15 minutes
  rules:
    - if: $CI_PIPELINE_SOURCE == "merge_request_event"
```
