# JavaFX 打包与部署指南

本指南涵盖 JavaFX 应用的打包与部署全流程，包括 jpackage 工具、跨平台打包、jlink 运行时镜像、图标要求、JavaPackager Gradle 插件、Gluon Substrate 原生镜像、CI/CD 集成、代码签名与自动更新。

---

## 一、jpackage 工具概述与要求

`jpackage` 是 JDK 14+ 内置的打包工具，可将 Java 应用打包为平台原生的安装包（exe/msi、dmg/pkg、deb/rpm），内嵌自定义 JRE，用户无需预装 Java。

### 1.1 前提条件

| 要求            | 说明                                                              |
|-----------------|-------------------------------------------------------------------|
| JDK 版本        | JDK 14+（推荐 JDK 21 LTS）                                        |
| 项目模块化      | 推荐使用 module-info.java，jpackage 可结合 jlink 生成精简运行时   |
| 平台工具链      | 打包目标平台需安装对应工具（见下文各平台要求）                    |
| JavaFX SDK/JAR  | 需将 JavaFX 模块加入 module-path                                  |

### 1.2 各平台额外工具要求

| 平台    | 必需工具                                          | 安装方式                              |
|---------|---------------------------------------------------|---------------------------------------|
| Windows | WiX Toolset 3.x（生成 msi）、Inno Setup（生成 exe）| 官网下载安装                          |
| macOS   | Xcode command line tools                          | `xcode-select --install`              |
| Linux   | `dpkg-deb`（deb）、`rpmbuild`（rpm）              | `apt install dpkg rpm` / `yum install rpm-build` |

### 1.3 jpackage 基本工作流

```
JavaFX 应用 (模块化)
    ↓ jlink（可选，生成自定义运行时）
自定义 JRE 镜像
    ↓ jpackage
原生安装包 (.exe/.msi/.dmg/.pkg/.deb/.rpm)
```

---

## 二、jpackage 完整参数参考

### 2.1 常用参数

| 参数                        | 说明                                                       |
|-----------------------------|------------------------------------------------------------|
| `--type`                    | 输出类型：`app-image`、`exe`、`msi`、`dmg`、`pkg`、`deb`、`rpm` |
| `--name`                    | 应用名称                                                   |
| `--app-version`             | 应用版本号                                                 |
| `--vendor`                  | 供应商名称                                                 |
| `--description`             | 应用描述                                                   |
| `--input` / `-i`            | 输入目录（存放应用 JAR 及资源）                            |
| `--main-jar`                | 主 JAR 文件名                                              |
| `--main-class`              | 主类全限定名                                               |
| `--module` / `-m`           | 主模块名（模块化应用）：`模块名/主类名`                    |
| `--module-path` / `-p`      | 模块路径                                                   |
| `--runtime-image`           | 预构建的运行时镜像路径（jlink 产物）                       |
| `--icon`                    | 应用图标文件路径                                           |
| `--dest` / `-d`             | 输出目录                                                   |
| `--java-options`            | 传递给 JVM 的参数（可多次使用）                            |
| `--arguments`               | 传递给主类的命令行参数                                     |
| `--add-modules`             | 额外添加到运行时的模块                                     |
| `--jlink-options`           | 传递给 jlink 的选项                                        |
| `--license-file`            | 许可证文件路径                                             |
| `--resource-dir`            | 资源覆盖目录（可放置自定义安装脚本、模板等）               |
| `--app-content`             | 额外内容目录（附加文件）                                   |
| `--temp`                    | 临时工作目录                                               |
| `--verbose`                 | 详细输出                                                   |
| `--about-url`               | 关于页面 URL                                               |
| `--file-associations`       | 文件关联配置文件                                           |
| `--install-dir`             | 安装目录                                                   |
| `--linux-package-name`      | Linux 包名                                                 |
| `--linux-deb-maintainer`    | deb 维护者邮箱                                             |
| `--linux-rpm-license-type`  | rpm 许可证类型                                             |
| `--mac-package-name`        | macOS 包名                                                 |
| `--mac-package-identifier`  | macOS 包标识符                                             |
| `--mac-sign`                | macOS 代码签名                                             |
| `--mac-signing-key-user-name`| macOS 签名密钥用户名                                      |
| `--win-dir-chooser`         | Windows 安装目录选择器                                     |
| `--win-menu`                | Windows 开始菜单快捷方式                                   |
| `--win-shortcut`            | Windows 桌面快捷方式                                       |
| `--win-per-user-install`    | Windows 按用户安装                                         |
| `--win-upgrade-uuid`        | Windows 升级 UUID（用于版本升级标识）                      |

### 2.2 基本命令示例

```bash
# 模块化应用打包（推荐）
jpackage \
  --type app-image \
  --name MyApp \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icon.ico \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist

# 非模块化应用打包
jpackage \
  --type app-image \
  --name MyApp \
  --app-version 1.0.0 \
  --input target \
  --main-jar myapp-1.0.0.jar \
  --main-class com.example.myapp.MainApp \
  --module-path "libs" \
  --add-modules javafx.controls,javafx.fxml \
  --icon assets/icon.ico \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

---

## 三、平台特定打包

### 3.1 Windows（exe / msi）

**生成 exe 安装程序（需 Inno Setup）：**

```bash
jpackage \
  --type exe \
  --name MyApp \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icon.ico \
  --win-menu \
  --win-shortcut \
  --win-dir-chooser \
  --win-upgrade-uuid "12345678-1234-1234-1234-123456789abc" \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

**生成 msi 安装程序（需 WiX Toolset）：**

```bash
jpackage \
  --type msi \
  --name MyApp \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icon.ico \
  --win-menu \
  --win-shortcut \
  --win-upgrade-uuid "12345678-1234-1234-1234-123456789abc" \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

> `--win-upgrade-uuid` 至关重要：相同 UUID 的不同版本安装包可互相升级，UUID 必须保持一致且全局唯一。

### 3.2 macOS（dmg / pkg）

```bash
jpackage \
  --type dmg \
  --name MyApp \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icon.icns \
  --mac-package-name MyApp \
  --mac-package-identifier com.mycompany.myapp \
  --mac-sign \
  --mac-signing-key-user-name "Developer ID Application: Your Name (XXXXXXXXXX)" \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

> macOS 打包必须在 macOS 系统上执行。生成 `.app` 应用包用 `--type app-image`，分发用 `dmg` 或 `pkg`。

### 3.3 Linux（deb / rpm）

**生成 deb 包：**

```bash
jpackage \
  --type deb \
  --name myapp \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icon.png \
  --linux-package-name myapp \
  --linux-deb-maintainer "dev@mycompany.com" \
  --linux-rpm-license-type "MIT" \
  --install-dir /opt/myapp \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

**生成 rpm 包：**

```bash
jpackage \
  --type rpm \
  --name myapp \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon assets/icon.png \
  --linux-package-name myapp \
  --linux-rpm-license-type "MIT" \
  --install-dir /opt/myapp \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

---

## 四、jlink 自定义运行时镜像

jpackage 可自动调用 jlink 生成运行时，也可先手动用 jlink 生成镜像再通过 `--runtime-image` 传入，获得更精细的控制。

### 4.1 手动创建 jlink 镜像

```bash
jlink \
  --module-path "mods:libs" \
  --add-modules com.example.myapp \
  --output build/javafx-runtime \
  --strip-debug \
  --compress zip-6 \
  --no-header-files \
  --no-man-pages \
  --bind-services
```

### 4.2 将 jlink 镜像传给 jpackage

```bash
jpackage \
  --type app-image \
  --name MyApp \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

### 4.3 jlink 镜像体积优化

| 优化手段                | 效果                                  |
|-------------------------|---------------------------------------|
| `--strip-debug`         | 去除调试信息，减小 20-30%             |
| `--compress zip-6`      | 压缩模块，减小 30-50%                 |
| 仅 `--add-modules` 必要模块 | 避免引入未使用模块                |
| `--no-header-files`     | 去除 C 头文件                         |
| `--no-man-pages`        | 去除 man 手册                         |

典型 JavaFX 应用 jlink 镜像体积：40-80 MB（含 JavaFX 模块）。

---

## 五、图标要求

各平台对应用图标的格式与规格要求不同。

| 平台    | 图标格式    | 推荐尺寸                          | 说明                              |
|---------|-------------|-----------------------------------|-----------------------------------|
| Windows | `.ico`      | 256x256（多尺寸内嵌）             | 需包含 16/32/48/256 等多尺寸      |
| macOS   | `.icns`     | 1024x1024（多尺寸内嵌）           | 需包含 16/32/64/128/256/512/1024  |
| Linux   | `.png`      | 512x512                           | 单一 PNG 文件即可                 |

### 5.1 图标制作建议

1. **源文件**：使用 1024x1024 或更高分辨率的 PNG/SVG 作为源文件。
2. **Windows .ico 生成**：使用工具（如 png2ico、ImageMagick）将多尺寸 PNG 合并为 ico。
3. **macOS .icns 生成**：使用 `iconutil` 命令：

```bash
# 创建 iconset 目录并放入各尺寸图标
mkdir MyApp.iconset
sips -z 16 16     icon1024.png --out MyApp.iconset/icon_16x16.png
sips -z 32 32     icon1024.png --out MyApp.iconset/icon_16x16@2x.png
sips -z 32 32     icon1024.png --out MyApp.iconset/icon_32x32.png
sips -z 64 64     icon1024.png --out MyApp.iconset/icon_32x32@2x.png
sips -z 128 128   icon1024.png --out MyApp.iconset/icon_128x128.png
sips -z 256 256   icon1024.png --out MyApp.iconset/icon_128x128@2x.png
sips -z 256 256   icon1024.png --out MyApp.iconset/icon_256x256.png
sips -z 512 512   icon1024.png --out MyApp.iconset/icon_256x256@2x.png
sips -z 512 512   icon1024.png --out MyApp.iconset/icon_512x512.png
cp icon1024.png          MyApp.iconset/icon_512x512@2x.png

# 生成 icns
iconutil -c icns MyApp.iconset
```

---

## 六、JavaPackager Gradle 插件（fvarrui）

`javapackager` 是由 fvarrui 维护的 Gradle 插件，封装了 jpackage，简化跨平台打包配置。

### 6.1 插件坐标

```groovy
plugins {
    id 'java'
    id 'application'
    id 'org.openjfx.javafxplugin' version '0.1.0'
    id 'io.github.fvarrui.javapackager' version '1.7.7'
}
```

### 6.2 完整配置示例

```groovy
javafx {
    version = "21.0.4"
    modules = ['javafx.controls', 'javafx.fxml']
}

application {
    mainClass = 'com.example.myapp.MainApp'
}

javapackager {
    // 基本信息
    mainClass = 'com.example.myapp.MainApp'
    version = '1.0.0'
    name = 'MyApp'
    description = 'My JavaFX Application'
    organization = 'MyCompany'

    // 打包类型（默认根据当前平台自动选择）
    // 可指定：deb, rpm, dmg, pkg, exe, msi, app-image
    // bundleJre = true  // 内嵌 JRE

    // 图标
    // icon = file('src/main/resources/icons/icon.ico')

    // JVM 参数
    jvmArgs = ['--enable-native-access=javafx.graphics', '-Xmx512m']

    // 运行时镜像
    generateInstaller = true
    copyDependencies = true

    // 平台特定配置
    linux {
        // deb/rpm 特定配置
        installationPath = '/opt/myapp'
    }
    mac {
        // dmg/pkg 特定配置
        bundleName = 'MyApp'
        bundleIdentifier = 'com.mycompany.myapp'
    }
    windows {
        // exe/msi 特定配置
        displayName = 'MyApp'
        installationPath = 'C:\\Program Files\\MyApp'
        shortcut = true
        menuGroup = true
        // upgradeUuid = '12345678-1234-1234-1234-123456789abc'
    }
}
```

### 6.3 执行打包

```bash
# 当前平台打包
gradle package

# 指定平台打包（需对应平台环境）
gradle package -PtargetPlatform=linux
gradle package -PtargetPlatform=mac
gradle package -PtargetPlatform=windows
```

---

## 七、Gluon Substrate / GraalVM 原生镜像

Gluon Substrate 基于 GraalVM Native Image，可将 JavaFX 应用编译为原生可执行文件，实现毫秒级启动和更低内存占用。

### 7.1 优势与限制

| 优势                          | 限制                                    |
|-------------------------------|-----------------------------------------|
| 启动速度极快（毫秒级）        | 编译时间长（数分钟）                    |
| 内存占用低                    | 需 GraalVM 环境                         |
| 单一可执行文件，无需 JRE      | 反射需配置（GraalVM 限制）              |
| 更好的分发体验                | 部分动态特性（动态类加载）受限          |

### 7.2 GluonFX Maven 插件配置

```xml
<plugin>
    <groupId>com.gluonhq</groupId>
    <artifactId>gluonfx-maven-plugin</artifactId>
    <version>1.0.22</version>
    <configuration>
        <target>host</target>
        <mainClass>com.example.myapp.MainApp</mainClass>
        <!-- GraalVM 安装路径 -->
        <graalvmHome>/path/to/graalvm</graalvmHome>
        <!-- 启用本地访问 -->
        <nativeImageArgs>
            <arg>--enable-native-access=javafx.graphics</arg>
        </nativeImageArgs>
    </configuration>
</plugin>
```

### 7.3 构建原生镜像

```bash
# 设置 GraalVM 环境
export GRAALVM_HOME=/path/to/graalvm
export PATH=$GRAALVM_HOME/bin:$PATH

# 安装 native-image 组件
gu install native-image

# 构建原生镜像
mvn gluonfx:build

# 运行
mvn gluonfx:nativerun
```

### 7.4 GluonFX Gradle 插件

```groovy
plugins {
    id 'com.gluonhq.gluonfx-gradle-plugin' version '1.0.22'
}

gluonfx {
    target = 'host'
    mainClass = 'com.example.myapp.MainApp'
    graalvmHome = '/path/to/graalvm'
    nativeImageArgs = ['--enable-native-access=javafx.graphics']
}
```

```bash
gradle nativeBuild
gradle nativeRun
```

### 7.5 跨平台原生镜像

GraalVM Native Image 不支持交叉编译，需在目标平台上构建。Gluon 提供云端构建服务（Gluon Cloud），可在云端为多平台生成原生镜像。

---

## 八、CI/CD 集成（GitHub Actions）

以下 GitHub Actions 工作流实现多平台自动打包。

### 8.1 完整工作流示例

```yaml
name: Build and Package

on:
  push:
    tags:
      - 'v*'
  workflow_dispatch:

jobs:
  package:
    strategy:
      matrix:
        os: [windows-latest, macos-latest, ubuntu-latest]
        include:
          - os: windows-latest
            platform: windows
            artifact: MyApp-*.exe
          - os: macos-latest
            platform: mac
            artifact: MyApp-*.dmg
          - os: ubuntu-latest
            platform: linux
            artifact: MyApp-*.deb

    runs-on: ${{ matrix.os }}

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'

      - name: Setup JavaFX SDK
        run: |
          # 下载 JavaFX SDK（jpackage 需要 SDK 或模块化 JAR）
          curl -L https://download2.gluonhq.com/openjfx/21.0.4/openjfx-21.0.4_${{ matrix.platform }}-x64_bin-sdk.zip -o javafx-sdk.zip
          7z x javafx-sdk.zip -ojavafx-sdk || unzip javafx-sdk.zip -d javafx-sdk
          echo "JAVAFX_SDK=javafx-sdk/javafx-sdk-21.0.4/lib" >> $GITHUB_ENV
        shell: bash

      - name: Install WiX Toolset (Windows)
        if: matrix.platform == 'windows'
        run: |
          dotnet tool install --global wix
          echo "WIX=C:\Program Files (x86)\WiX Toolset v3.14\bin" >> $GITHUB_ENV

      - name: Build with Maven
        run: mvn -B clean package -DskipTests

      - name: Create jlink runtime image
        run: |
          jlink \
            --module-path "target/modules:$JAVAFX_SDK" \
            --add-modules com.example.myapp \
            --output build/runtime \
            --strip-debug \
            --compress zip-6 \
            --no-header-files \
            --no-man-pages
        shell: bash

      - name: Package with jpackage (Windows)
        if: matrix.platform == 'windows'
        run: |
          jpackage \
            --type msi \
            --name MyApp \
            --app-version ${{ github.ref_name }} \
            --module com.example.myapp/com.example.myapp.MainApp \
            --module-path "target/modules:$JAVAFX_SDK" \
            --runtime-image build/runtime \
            --icon assets/icon.ico \
            --win-menu --win-shortcut \
            --win-upgrade-uuid "12345678-1234-1234-1234-123456789abc" \
            --java-options "--enable-native-access=javafx.graphics" \
            --dest dist

      - name: Package with jpackage (macOS)
        if: matrix.platform == 'mac'
        run: |
          jpackage \
            --type dmg \
            --name MyApp \
            --app-version ${{ github.ref_name }} \
            --module com.example.myapp/com.example.myapp.MainApp \
            --module-path "target/modules:$JAVAFX_SDK" \
            --runtime-image build/runtime \
            --icon assets/icon.icns \
            --mac-package-identifier com.mycompany.myapp \
            --java-options "--enable-native-access=javafx.graphics" \
            --dest dist

      - name: Package with jpackage (Linux)
        if: matrix.platform == 'linux'
        run: |
          jpackage \
            --type deb \
            --name myapp \
            --app-version ${{ github.ref_name }} \
            --module com.example.myapp/com.example.myapp.MainApp \
            --module-path "target/modules:$JAVAFX_SDK" \
            --runtime-image build/runtime \
            --icon assets/icon.png \
            --linux-deb-maintainer "dev@mycompany.com" \
            --java-options "--enable-native-access=javafx.graphics" \
            --dest dist

      - name: Upload artifacts
        uses: actions/upload-artifact@v4
        with:
          name: MyApp-${{ matrix.platform }}
          path: dist/${{ matrix.artifact }}

      - name: Release
        if: startsWith(github.ref, 'refs/tags/')
        uses: softprops/action-gh-release@v2
        with:
          files: dist/${{ matrix.artifact }}
```

### 8.2 CI/CD 注意事项

1. **跨平台限制**：jpackage 无法交叉编译，每个平台需在对应 OS 的 runner 上构建。
2. **JavaFX SDK 下载**：CI 环境需下载 JavaFX SDK 或使用模块化 JAR 依赖。
3. **缓存依赖**：使用 `actions/cache` 缓存 Maven/Gradle 依赖加速构建。
4. **版本号管理**：从 Git tag 提取版本号，保持版本一致。

---

## 九、代码签名概述

代码签名确保安装包来源可信，避免操作系统安全警告（如 Windows SmartScreen、macOS Gatekeeper）。

### 9.1 Windows 代码签名

```bash
jpackage \
  --type msi \
  --name MyApp \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/runtime \
  --win-upgrade-uuid "..." \
  --java-options "--enable-native-access=javafx.graphics" \
  --resource-dir sign-resources \
  --dest dist
```

Windows 签名需通过 `--resource-dir` 提供 WiX 自定义模板或使用第三方工具（signtool、osslsigncode）对生成的安装包签名：

```bash
# 使用 signtool 签名（需代码签名证书）
signtool sign /fd SHA256 /a /tr http://timestamp.digicert.com /td SHA256 dist\MyApp-1.0.0.msi
```

### 9.2 macOS 代码签名与公证

```bash
# 签名
jpackage \
  --type dmg \
  --name MyApp \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/runtime \
  --mac-sign \
  --mac-signing-key-user-name "Developer ID Application: Your Name (XXXXXXXXXX)" \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist

# 公证（Notarization）
xcrun notarytool submit dist/MyApp-1.0.0.dmg \
  --apple-id "your@email.com" \
  --password "app-specific-password" \
  --team-id "XXXXXXXXXX" \
  --wait

# 装订公证票据
xcrun stapler staple dist/MyApp-1.0.0.dmg
```

> macOS 分发必须经过签名 + 公证，否则用户首次打开会被 Gatekeeper 拦截。

### 9.3 Linux 签名

Linux deb/rpm 包通常通过 GPG 签名：

```bash
# deb 包签名
dpkg-sig --sign builder dist/myapp-1.0.0.deb

# rpm 包签名（需在 rpmmacros 配置签名密钥）
rpm --addsign dist/myapp-1.0.0.rpm
```

---

## 十、自动更新策略

JavaFX 应用没有内置的自动更新机制，需自行实现或借助第三方方案。

### 10.1 自实现更新检查

```java
public class UpdateChecker {
    private static final String VERSION_URL = "https://mycompany.com/api/latest-version";

    public void checkForUpdates() {
        Task<VersionInfo> task = new Task<>() {
            @Override
            protected VersionInfo call() throws Exception {
                // 请求最新版本信息
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VERSION_URL))
                    .build();
                HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
                // 解析 JSON 获取版本号和下载链接
                return parseVersionInfo(response.body());
            }
        };
        task.setOnSucceeded(e -> {
            VersionInfo info = task.getValue();
            if (isNewer(info.getVersion(), getCurrentVersion())) {
                showUpdateDialog(info);
            }
        });
        new Thread(task).start();
    }

    private void showUpdateDialog(VersionInfo info) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("发现新版本");
        alert.setHeaderText("新版本 " + info.getVersion() + " 可用");
        alert.setContentText("是否立即下载更新？");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // 打开下载页面或启动下载
            HostServices services = getHostServices();
            services.showDocument(info.getDownloadUrl());
        }
    }
}
```

### 10.2 常见自动更新方案

| 方案                    | 说明                                                       |
|-------------------------|------------------------------------------------------------|
| 自实现版本检查          | 应用启动时请求服务器最新版本，提示用户下载新安装包         |
| Windows MSI 升级        | 使用相同 `--win-upgrade-uuid` 的 msi 可静默升级            |
| 后台增量更新            | 仅下载变更的 JAR/资源，替换后重启（需自实现）              |
| 第三方更新框架          | 如 update4j（JavaFX 友好的更新框架）                       |

### 10.3 update4j 集成示例

```xml
<dependency>
    <groupId>org.update4j</groupId>
    <artifactId>update4j</artifactId>
    <version>1.5.9</version>
</dependency>
```

```java
import org.update4j.Configuration;

// 从远程加载更新配置
Configuration config = Configuration.read(new URL("https://mycompany.com/update/config.xml"));

// 检查并更新
if (config.requiresUpdate()) {
    config.update();  // 下载更新的文件
}

// 启动应用
config.launch();  // 启动主类
```

---

## 十一、JavaFX 24+ 的 --enable-native-access 标志

JavaFX 24+ 的图形渲染层通过 JNI 访问本地代码，在 JDK 24+ 严格封装机制下必须显式授予本地访问权限。

### 11.1 打包时添加标志

**jpackage 方式：**

```bash
jpackage \
  --type msi \
  --name MyApp \
  --module com.example.myapp/com.example.myapp.MainApp \
  --module-path "mods:libs" \
  --runtime-image build/runtime \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

**jlink 镜像方式（在启动脚本中固化）：**

使用 jpackage 生成的 app-image 会自动将 `--java-options` 写入启动脚本（`bin/MyApp` 或 `bin/MyApp.bat`），无需用户手动添加。

### 11.2 验证标志是否生效

```bash
# 检查生成的启动脚本
cat dist/MyApp/bin/MyApp  # Linux/macOS
type dist\MyApp\bin\MyApp.bat  # Windows
```

启动脚本中应包含 `--enable-native-access=javafx.graphics` 参数。

### 11.3 多模块本地访问

若应用同时使用其他本地库，可列出多个模块名：

```bash
--java-options "--enable-native-access=javafx.graphics,com.example.nativelib"
```

---

## 十二、打包部署最佳实践总结

| 实践                          | 说明                                                       |
|-------------------------------|------------------------------------------------------------|
| 优先使用模块化 + jlink        | 减小体积，提升启动速度                                     |
| 统一版本号管理                | 从构建配置或 Git tag 提取版本，保持一致                    |
| 跨平台分别构建                | jpackage 不支持交叉编译，各平台独立构建                    |
| 保持 upgrade-uuid 一致        | Windows 升级依赖固定 UUID                                  |
| macOS 必须签名 + 公证         | 否则用户无法正常打开                                       |
| 内嵌 JRE                      | 通过 jlink/jpackage 内嵌运行时，用户无需预装 Java          |
| JavaFX 24+ 添加本地访问标志   | `--enable-native-access=javafx.graphics`                   |
| CI/CD 自动化多平台打包        | 使用 GitHub Actions 矩阵构建                               |
| 提供自动更新检查              | 提升用户体验，及时推送修复                                 |
| 测试安装与卸载流程            | 确保快捷方式、注册表、文件关联正确                         |
