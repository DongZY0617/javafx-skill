# 打包验证规则

本文档是"打包验证"维度的判定依据，管辖 8 个检查项。通过执行 `mvn package` 生成 JAR，再执行 `jpackage` 生成原生安装包，验证打包流程和产物完整性，将"可运行"的代码推进到"可交付"。此维度违规默认为 Major。与 `javafx-developer` 的打包章节·jpackage 命令同源。

> **核心原则**：打包验证是动态验证链的最后一环，验证项目能否生成可分发的原生安装包。`jpackage` 依赖各平台工具链（Windows 的 Inno Setup/WiX、macOS 的 Xcode tools、Linux 的 dpkg-deb/rpmbuild），工具链缺失是打包失败的最常见原因。打包失败默认 Major，但工具链缺失（环境问题）降级为 Info，`--module-path` 等配置错误（代码问题）升级为 Critical。

---

## 检查项 1：JAR 构建

**关注点**：`mvn package` 能否成功生成可执行 JAR，JAR 内是否包含所有必要的 JavaFX 模块依赖。

**通过判定标准**：
- 执行 `mvn package -DskipTests` 退出码为 0，无 `[ERROR]` 输出
- `target/` 目录下生成 JAR 文件，文件大小合理（非 0 字节）
- 非模块化项目：fat JAR（uber JAR）包含 JavaFX 依赖类，或通过 `javafx-maven-plugin` 运行
- 模块化项目：JAR 的 `MANIFEST.MF` 中 `Main-Class` 正确，或 `module-info.class` 存在
- `pom.xml` 的 `maven-jar-plugin` / `maven-shade-plugin` 配置正确

**不通过判定标准**（任一即不通过）：
- `mvn package` 失败，退出码非 0
- `target/` 目录下无 JAR 文件（构建流程断裂）
- JAR 文件为 0 字节（构建异常）
- 非模块化项目的 JAR 缺少 JavaFX 依赖类，运行报 `Error: JavaFX runtime components are missing`
- `MANIFEST.MF` 中 `Main-Class` 错误或缺失

**严重性基线**：Critical（不可降级，JAR 产物缺失则打包流程断裂）

**反例**：
```xml
<!-- ❌ 非模块化项目未用 shade-plugin 打包 JavaFX 依赖，JAR 缺少运行时 -->
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-jar-plugin</artifactId>
            <configuration>
                <archive>
                    <manifest>
                        <mainClass>com.example.App</mainClass>
                    </manifest>
                </archive>
            </configuration>
        </plugin>
        <!-- 缺少 maven-shade-plugin，JavaFX 依赖未打入 JAR -->
    </plugins>
</build>
```
运行打包产物时报：
```
Error: JavaFX runtime components are missing, and are required to run this application
```

**正例**：
```xml
<!-- ✅ 使用 maven-shade-plugin 打包 fat JAR -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <executions>
        <execution>
            <phase>package</phase>
            <goals><goal>shade</goal></goals>
            <configuration>
                <transformers>
                    <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                        <mainClass>com.example.App</mainClass>
                    </transformer>
                </transformers>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## 检查项 2：模块路径完整性

**关注点**：`jpackage` 的 `--module-path` 是否包含 JavaFX SDK 的 `lib` 目录，`--add-modules` 是否列出所有必需模块。

**通过判定标准**：
- `--module-path` 包含项目编译产物路径与 JavaFX SDK 的 `lib` 目录
- `--add-modules` 列出所有实际使用的 JavaFX 模块（`javafx.controls`、`javafx.fxml`、`javafx.graphics` 等）
- `--module-path` 中所有路径均存在且可访问
- 模块化项目的 `--module-path` 指向 `target/modules` 或 JavaFX SDK `lib`

**不通过判定标准**（任一即不通过）：
- `--module-path` 缺少 JavaFX SDK 的 `lib` 目录，jpackage 报 `Module javafx.controls not found`
- `--add-modules` 遗漏实际使用的模块（如用了 FXML 但未列 `javafx.fxml`）
- `--module-path` 路径不存在或拼写错误
- 非模块化项目误用 `--module-path` / `--add-modules`（应使用 `--main-jar` + `--main-class`）

**严重性基线**：Critical
- 降级条件：—（`--module-path` 错误导致生成的安装包运行时报 `Module not found`，不可降级）
- 升级条件：—

> **关键事实**：`--module-path` 和 `--add-modules` 是模块化项目 jpackage 的核心参数。缺失 `--add-modules javafx.controls,javafx.fxml` 时，jpackage 可能成功生成安装包，但运行时报 `Module javafx.controls not found`——这是"打包成功但产物无法运行"的典型陷阱。

**反例**：
```bash
# ❌ --module-path 缺少 JavaFX SDK lib，--add-modules 遗漏 javafx.fxml
jpackage \
  --name MyApp \
  --module-path target/modules \
  --module com.example.app/com.example.App \
  --add-modules javafx.controls
# 遗漏 javafx.fxml，且 target/modules 缺少 JavaFX SDK
```
运行安装包时报：
```
Error: Module javafx.fxml not found, required by com.example.app
```

**正例**：
```bash
# ✅ --module-path 包含 JavaFX SDK lib，--add-modules 完整
jpackage \
  --name MyApp \
  --module-path "target/modules;C:\javafx-sdk-21\lib" \
  --module com.example.app/com.example.App \
  --add-modules javafx.controls,javafx.fxml,javafx.graphics
```

---

## 检查项 3：主类与主模块

**关注点**：`--main-class` 和 `--main-module`（模块化项目）是否正确指向应用入口。

**通过判定标准**：
- 模块化项目：`--module com.example.app/com.example.App` 格式正确（`模块名/主类全限定名`）
- 非模块化项目：`--main-jar app.jar --main-class com.example.App` 正确
- `--module` 指向的模块名与 `module-info.java` 声明一致
- 主类包含 `public static void main(String[] args)` 且继承 `javafx.application.Application`
- `--main-class` 的全限定名与实际类一致（无拼写错误）

**不通过判定标准**（任一即不通过）：
- `--module` 的模块名与 `module-info.java` 声明不一致
- `--main-class` 全限定名拼写错误或类不存在
- 主类未继承 `javafx.application.Application`，无 `main` 方法
- 模块化项目误用 `--main-jar` 而非 `--module`
- 非模块化项目误用 `--module` 而非 `--main-jar` + `--main-class`

**严重性基线**：Critical
- 降级条件：—（主类/主模块错误导致安装包无法启动，不可降级）
- 升级条件：—

**反例**：
```bash
# ❌ --module 模块名与 module-info.java 声明不一致
# module-info.java 声明: module com.example.app
jpackage --module com.example.myapp/com.example.App
# "myapp" 与 "app" 不一致，jpackage 报错
```
```
Error: Module com.example.myapp not found
```

**正例**：
```bash
# ✅ 模块名与 module-info.java 一致，主类全限定名正确
jpackage \
  --name MyApp \
  --module com.example.app/com.example.App \
  --module-path "target/modules;C:\javafx-sdk-21\lib" \
  --add-modules javafx.controls,javafx.fxml
```

---

## 检查项 4：原生访问配置

**关注点**：`--java-options "--enable-native-access=javafx.graphics"` 是否包含在打包配置中（JavaFX 24+）。

**通过判定标准**：
- JavaFX 24+ 项目的 `jpackage` 命令包含 `--java-options "--enable-native-access=javafx.graphics"`
- 打包生成的安装包运行时无 `IllegalAccessError` 原生访问错误
- `--java-options` 参数语法正确（引号包裹含空格的值）
- 模块化项目指定具体模块 `javafx.graphics`，非 `ALL-UNNAMED`

**不通过判定标准**（任一即不通过）：
- JavaFX 24+ 项目的 `jpackage` 命令缺少 `--java-options "--enable-native-access=javafx.graphics"`
- `--java-options` 语法错误（引号缺失导致参数被拆分）
- 误用 `=ALL-UNNAMED`（模块化项目应指定具体模块）
- 打包产物运行时报 `IllegalAccessError` 或原生访问警告

**严重性基线**：Critical（不可降级，JavaFX 24+ 运行时必然报 `IllegalAccessError`）

> **版本感知**：此检查项仅在检测到 JavaFX 24+ 时执行。JavaFX 17/21 不受原生访问限制，无需配置 `--enable-native-access`。runner 通过环境检测（见 `environment-setup.md`）提取 JavaFX 版本后动态决定是否执行此检查项。

**反例**：
```bash
# ❌ JavaFX 24+ 项目，jpackage 缺少 --enable-native-access
jpackage \
  --name MyApp \
  --module com.example.app/com.example.App \
  --module-path "target/modules;C:\javafx-sdk-24\lib" \
  --add-modules javafx.controls,javafx.fxml
# 缺少 --java-options "--enable-native-access=javafx.graphics"
```
安装包运行时报：
```
java.lang.IllegalAccessError: class javafx.graphics ... cannot access ...
```

**正例**：
```bash
# ✅ JavaFX 24+ 配置 --enable-native-access
jpackage \
  --name MyApp \
  --module com.example.app/com.example.App \
  --module-path "target/modules;C:\javafx-sdk-24\lib" \
  --add-modules javafx.controls,javafx.fxml \
  --java-options "--enable-native-access=javafx.graphics"
```

---

## 检查项 5：平台工具链

**关注点**：当前操作系统是否安装 jpackage 所需的平台工具链。

**通过判定标准**：
- **Windows**：安装 Inno Setup 6+（生成 `.exe`）或 WiX Toolset 4.x（生成 `.msi`），且已加入 `PATH`
- **macOS**：安装 Xcode Command Line Tools（`xcode-select -p` 返回路径）
- **Linux**：安装 `dpkg-deb`（Debian/Ubuntu，生成 `.deb`）或 `rpmbuild`（RHEL/Fedora，生成 `.rpm`）
- jpackage 能成功调用对应工具链生成安装包
- 工具链版本满足 jpackage 最低要求

**不通过判定标准**（任一即不通过）：
- Windows 未安装 Inno Setup 且未安装 WiX，jpackage 报 `Failed to find Inno Setup ... No .exe or .msi ... generated`
- macOS 未安装 Xcode Command Line Tools，jpackage 报 `Failed to find ... xcode-select`
- Linux 未安装 `dpkg-deb` 且未安装 `rpmbuild`，jpackage 报 `Failed to find ... dpkg-deb / rpmbuild`
- 工具链已安装但未加入 `PATH`，jpackage 无法调用

**严重性基线**：Major
- 降级条件：工具链未安装（环境问题，非代码问题）→ Info
- 升级条件：—（工具链问题不升级，仅环境依赖缺失）

### 各平台工具链检测方法

**Windows（Inno Setup / WiX）**：
```powershell
# 检测 Inno Setup（生成 .exe）
Get-Command iscc -ErrorAction SilentlyContinue
# 或查询注册表安装路径
Get-ItemProperty "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\*" |
    Where-Object { $_.DisplayName -like "*Inno Setup*" }

# 检测 WiX Toolset 4.x（生成 .msi）
Get-Command wix -ErrorAction SilentlyContinue
Get-Command candle -ErrorAction SilentlyContinue
```

**macOS（Xcode Command Line Tools）**：
```bash
# 检测 Xcode Command Line Tools
xcode-select -p
# 返回 /Library/Developer/CommandLineTools 或 /Applications/Xcode.app/Contents/Developer
# 无返回或报错则未安装，执行 xcode-select --install 安装
```

**Linux（dpkg-deb / rpmbuild）**：
```bash
# Debian/Ubuntu 系（生成 .deb）
which dpkg-deb
dpkg-deb --version

# RHEL/Fedora 系（生成 .rpm）
which rpmbuild
rpmbuild --version
```

> **关键事实**：jpackage 调用平台原生工具链生成安装包。工具链缺失时 jpackage 可能打印错误但仍返回退出码 0（生成部分产物），需检查实际产物文件是否生成。工具链缺失属环境问题，降级为 Info 提示用户安装，而非阻断代码交付。

**反例**：
```bash
# Windows 未安装 Inno Setup，jpackage 尝试生成 .exe 失败
jpackage --name MyApp --input target --main-jar app.jar --main-class com.example.App --type exe
```
```
Failed to find Inno Setup. No .exe bundle generated.
```

**正例**：
```bash
# 安装 Inno Setup 后加入 PATH，重新执行
# 安装命令（Chocolatey）：choco install innosetup
jpackage --name MyApp --input target --main-jar app.jar --main-class com.example.App --type exe
# 成功生成 MyApp-1.0.exe
```

---

## 检查项 6：图标格式

**关注点**：各平台图标文件格式是否正确，是否包含多尺寸。

**通过判定标准**：
- **Windows**：图标为 `.ico` 格式，内嵌多尺寸（16x16、32x32、48x48、256x256）
- **macOS**：图标为 `.icns` 格式，内嵌多尺寸
- **Linux**：图标为 `.png` 格式
- `--icon` 参数指向的图标文件存在且格式正确
- 未提供图标时 jpackage 使用默认图标（不阻断，但建议提供）

**不通过判定标准**（任一即不通过）：
- Windows 使用 `.png` 而非 `.ico`，jpackage 报 `Invalid icon ... must be .ico`
- macOS 使用 `.ico` 而非 `.icns`，jpackage 报 `Invalid icon ... must be .icns`
- 图标文件不存在，`--icon` 路径错误
- `.ico` 文件仅含单尺寸，高分屏显示模糊（非阻断，但建议多尺寸）

**严重性基线**：Minor
- 降级条件：—（图标问题不影响功能，已是最 Minor）
- 升级条件：图标格式严重错误导致 jpackage 失败 → Major

**反例**：
```bash
# ❌ Windows 使用 .png 图标，jpackage 报错
jpackage \
  --name MyApp \
  --input target \
  --main-jar app.jar \
  --main-class com.example.App \
  --icon target/icons/logo.png   # Windows 应为 .ico
```
```
Error: Invalid icon: must be .ico format on Windows
```

**正例**：
```bash
# ✅ Windows 使用 .ico 多尺寸图标
jpackage \
  --name MyApp \
  --input target \
  --main-jar app.jar \
  --main-class com.example.App \
  --icon target/icons/logo.ico
```

---

## 检查项 7：安装包生成

**关注点**：`jpackage` 能否成功生成安装包文件，产物大小是否合理（非 0 字节）。

**通过判定标准**：
- `jpackage` 退出码为 0，无 `[ERROR]` 输出
- 对应平台安装包文件已生成：
  - Windows：`MyApp-1.0.exe` 或 `MyApp-1.0.msi`
  - macOS：`MyApp-1.0.dmg` 或 `MyApp-1.0.pkg`
  - Linux：`myapp_1.0-1_amd64.deb` 或 `myapp-1.0-1.x86_64.rpm`
- 安装包文件大小合理（非 0 字节，通常 > 10MB，含 JRE）
- 安装包可在对应平台正常安装并启动

**不通过判定标准**（任一即不通过）：
- `jpackage` 退出码非 0，安装包未生成
- 安装包文件为 0 字节或异常小（< 1MB，可能仅含部分内容）
- 安装包生成但无法安装（安装过程报错）
- 安装后应用无法启动（缺少依赖、模块路径错误）
- 产物路径错误，jpackage 未输出到预期目录

**严重性基线**：Major
- 降级条件：工具链缺失导致无法生成（环境问题）→ Info
- 升级条件：`--module-path` / `--main-class` 配置错误导致生成产物无法运行 → Critical

**反例**：
```bash
# ❌ jpackage 因 --module-path 错误"成功"生成安装包，但产物无法运行
jpackage \
  --name MyApp \
  --module com.example.app/com.example.App \
  --module-path target/modules \
  --add-modules javafx.controls,javafx.fxml
# target/modules 缺少 JavaFX SDK，安装包生成但运行报 Module not found
```

**正例**：
```bash
# ✅ --module-path 完整，安装包生成且可运行
jpackage \
  --name MyApp \
  --module com.example.app/com.example.App \
  --module-path "target/modules;C:\javafx-sdk-21\lib" \
  --add-modules javafx.controls,javafx.fxml,javafx.graphics \
  --java-options "--enable-native-access=javafx.graphics"
# 成功生成 MyApp-1.0.exe，安装后可正常启动
```

---

## 检查项 8：升级 UUID

**关注点**：Windows 打包是否包含有效的 `--win-upgrade-uuid`（UUID v4 格式），确保版本升级时能正确替换旧版本。

**通过判定标准**：
- Windows 打包命令包含 `--win-upgrade-uuid` 参数
- UUID 为有效的 v4 格式（如 `550e8400-e29b-41d4-a716-446655440000`）
- 同一应用的多个版本使用相同 UUID（确保升级时识别为同一应用）
- UUID 非全 0、非默认占位值（如 `00000000-0000-0000-0000-000000000000`）

**不通过判定标准**（任一即不通过）：
- Windows 打包缺少 `--win-upgrade-uuid` 参数
- UUID 格式无效（非标准 UUID 格式）
- UUID 为全 0 或占位值
- 不同版本使用不同 UUID（升级时无法识别为同一应用，导致重复安装）

**严重性基线**：Minor
- 降级条件：—（升级 UUID 缺失不影响首次安装，已是最 Minor）
- 升级条件：缺失 `--win-upgrade-uuid` 导致无法升级且用户已部署多版本 → Major

> **平台特例**：此检查项仅在 Windows 平台执行。macOS 通过 `--mac-package-identifier`（Bundle Identifier）管理应用身份，Linux 通过包名管理。`--win-upgrade-uuid` 是 Windows 安装包升级识别的关键，缺失时新版本安装不会替换旧版本，导致重复安装与残留。

**反例**：
```bash
# ❌ Windows 打包缺少 --win-upgrade-uuid
jpackage \
  --name MyApp \
  --input target \
  --main-jar app.jar \
  --main-class com.example.App \
  --type exe \
  --app-version 1.0
# 缺少 --win-upgrade-uuid，升级时无法识别为同一应用
```

**正例**：
```bash
# ✅ 包含有效的 --win-upgrade-uuid（v4 格式）
jpackage \
  --name MyApp \
  --input target \
  --main-jar app.jar \
  --main-class com.example.App \
  --type exe \
  --app-version 1.0 \
  --win-upgrade-uuid "550e8400-e29b-41d4-a716-446655440000"
```
