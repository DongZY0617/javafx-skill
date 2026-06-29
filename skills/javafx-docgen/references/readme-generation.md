# README Template and Content Extraction Rules

This reference defines how the JavaFX DocGen skill extracts project metadata from build files and composes a quick-start README. It covers metadata extraction from `pom.xml`, build and run instructions, native packaging with `jpackage`, the prerequisites section, and the project-structure section, ending with a complete README template.

## Overview

The README is the project's front door. The generator assembles it from deterministic sources — `pom.xml` (or `build.gradle`), `module-info.java`, and the detected main class — so it stays in sync with the actual build configuration. No hand-written content is required, but the template leaves clearly marked placeholders for content the generator cannot infer.

## Extracting Project Metadata from pom.xml

The generator reads the following elements from `pom.xml`:

| Element | Used For | Fallback |
|---------|----------|----------|
| `<name>` | Project title | `artifactId` |
| `<version>` | Version badge | `0.0.1-SNAPSHOT` |
| `<description>` | One-line summary | *Generated placeholder* |
| `<maven.compiler.source>` / `<release>` | JDK version | `17` |
| `<groupId>org.openjfx</groupId>` + `javafx-controls` version | JavaFX version | `21` |
| `<url>` | Project homepage link | Omitted |
| `<licenses>` | License badge | Omitted |

JavaFX version is read from the `javafx-controls` (or `javafx-fxml`) dependency version. If the version is a property reference such as `${javafx.version}`, the property value is resolved from `<properties>`.

```xml
<project>
    <name>User Manager FX</name>
    <version>1.2.0</version>
    <description>Desktop app for managing user records.</description>
    <properties>
        <maven.compiler.release>17</maven.compiler.release>
        <javafx.version>21.0.2</javafx.version>
    </properties>
</project>
```

## Generating Build Instructions

Build commands are emitted for the detected build tool.

### Maven

```bash
# Clean and compile
mvn clean compile

# Package into a JAR
mvn clean package

# Run tests
mvn test
```

### Gradle

If a `build.gradle` or `build.gradle.kts` exists instead of `pom.xml`:

```bash
# Clean and build
./gradlew clean build

# Run tests
./gradlew test
```

## Generating Run Instructions

Run commands cover three scenarios: running from source via the build tool, running a packaged JAR, and running with explicit module path.

### Maven (javafx-maven-plugin)

```bash
mvn javafx:run
```

### Direct module-path launch

Requires the `PATH_TO_FX` environment variable pointing at the JavaFX SDK `lib` directory.

```bash
java --module-path "$PATH_TO_FX" \
     --module com.example.app/com.example.app.Main
```

### Packaged JAR

```bash
java -jar target/user-manager-fx-1.2.0.jar
```

The module name comes from `module-info.java`; the main class is the class extending `javafx.application.Application` that declares `main()` or `start()`.

## Generating Packaging Instructions (jpackage)

Native installers are produced with `jpackage`. The generator emits platform-specific commands. Replace `${APP_NAME}`, `${APP_VERSION}`, `${MODULE}`, and `${MAIN_CLASS}` with extracted values.

### Windows

```bash
jpackage --name user-manager-fx --module com.example.app/com.example.app.Main \
         --type msi --app-version 1.2.0 --win-menu --win-shortcut
```

### macOS

```bash
jpackage --name user-manager-fx --module com.example.app/com.example.app.Main \
         --type dmg --app-version 1.2.0 --mac-package-name "User Manager FX"
```

### Linux

```bash
jpackage --name user-manager-fx --module com.example.app/com.example.app.Main \
         --type deb --app-version 1.2.0 --linux-menu-group "Utility"
```

## Prerequisites Section Template

The prerequisites block lists the minimum toolchain required to build and run the project. Values are sourced from the metadata extraction step.

```markdown
## Prerequisites

- **JDK** 17 or later (tested with JDK 21)
- **JavaFX** 21.0.2 (bundled by Maven; no manual install required)
- **Maven** 3.8+ (or Gradle 8+)
- **OS**: Windows 10+, macOS 12+, or a modern Linux distribution
```

## Project Structure Section

The generator renders a tree of the top-level source directories. Only directories are shown; individual files are summarized with counts.

```markdown
## Project Structure

user-manager-fx/
├── src/main/java/com/example/app/
│   ├── controller/      # JavaFX controllers (4 classes)
│   ├── model/           # Domain model (6 classes)
│   ├── service/         # Business services (3 classes)
│   └── Main.java        # Application entry point
├── src/main/resources/
│   ├── fxml/            # FXML layouts (5 files)
│   ├── css/             # Stylesheets (2 files)
│   └── images/          # Icons and assets
└── pom.xml
```

## Complete README Template

The assembled README follows this template. Bracketed tokens are replaced with extracted values; tokens marked `<!-- fill in -->` are left for the author.

```markdown
# User Manager FX

Desktop app for managing user records.

[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-blue)]()
[![Version](https://img.shields.io/badge/version-1.2.0-green)]()

## Prerequisites

- **JDK** 17 or later
- **JavaFX** 21.0.2
- **Maven** 3.8+

## Build

mvn clean package

## Run

mvn javafx:run

## Package

jpackage --name user-manager-fx \
         --module com.example.app/com.example.app.Main \
         --type msi --app-version 1.2.0

## Project Structure

<project tree>

## License

<!-- fill in -->

## Documentation

See the `docs/` directory for the API reference, user manual, and
architecture document.
```

The generator writes the final `README.md` to the project root. If a README already exists, it is backed up to `README.md.bak` before overwriting.
