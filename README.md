# AchheCode Playwright

A reusable Java library that provides configurable Playwright browser automation for Spring Boot applications.

AchheCode Playwright centralizes browser initialization, connection management, and lifecycle handling so multiple services can reuse a common Playwright integration.

## Features

- **Spring Boot integration** — Provides `PlaywrightCoreConfiguration` for browser automation setup.
- **Configurable browser modes** — Supports launching a browser or attaching to an existing Chromium-based browser through CDP.
- **Headless and headed execution** — Configure whether a locally launched browser is visible.
- **Custom browser executable** — Supports specifying a custom browser executable path.
- **Externalized configuration** — Override browser settings using `application.yml` or `application.properties`.
- **Centralized lifecycle management** — Manage browser resources through the library instead of creating duplicate browser instances in consuming applications.
- **Maven dependency support** — Reuse the library across multiple Spring Boot services.

## Release Information

| Property | Value |
|---|---|
| Version | `1.0.0` |
| Release type | Initial release |
| Packaging | JAR |
| Build tool | Maven |
| Group ID | `com.achhecode` |
| Artifact ID | `achhecode-playwright` |

## Requirements

- Java version compatible with the library's build configuration.
- Maven.
- A Spring Boot application.
- Playwright-compatible browser binaries for the selected browser mode.

## Installation

### 1. Build and install the library

From the library's root directory, run:

```sh
mvn clean install
```

This builds the library and installs the generated artifact into your local Maven repository.

### 2. Verify the installation

Check that the JAR and Maven metadata exist:

```sh
ls -la ~/.m2/repository/com/achhecode/achhecode-playwright/1.0.0/
```

The directory should contain the library JAR and associated Maven metadata, such as the POM file.

### 3. Verify the Spring configuration class

The consuming application requires `PlaywrightCoreConfiguration`. Verify that it is packaged in the JAR:

```sh
jar tf ~/.m2/repository/com/achhecode/achhecode-playwright/1.0.0/achhecode-playwright-1.0.0.jar \
  | grep -E 'PlaywrightCoreConfiguration|Configuration.class'
```

Confirm that the output includes the expected fully qualified class path, such as:

```text
com/achhecode/playwright/config/PlaywrightCoreConfiguration.class
```

The package path above must match the actual package declaration in the released library.

## Use in another service

### 1. Add the Maven dependency

Add the following dependency to the consuming application's `pom.xml`:

```xml
<dependency>
    <groupId>com.achhecode</groupId>
    <artifactId>achhecode-playwright</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Import the library configuration

Import `PlaywrightCoreConfiguration` into the consuming application's main Spring Boot class.

**The `@Import` annotation is required for the integration described here.**

```java
import com.achhecode.playwright.config.PlaywrightCoreConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(PlaywrightCoreConfiguration.class)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

Use the configuration class's actual package if it differs in your release.

### 3. Configure the browser

The consuming service can override only the properties it needs. Unspecified properties use the library's configured defaults.

**Launch a visible browser**

```yaml
playwright:
  browser:
    mode: LAUNCH
    headless: false
```

**Attach to an existing browser**

```yaml
playwright:
  browser:
    mode: ATTACHED
    host: 127.0.0.1
    port: 9222
```

The external Chromium-based browser must already be running with remote debugging enabled on the configured port.

**Use a custom browser executable**

```yaml
playwright:
  browser:
    mode: LAUNCH
    headless: false
    executable-path: "/path/to/browser-executable"
```

The executable must be compatible with the Playwright browser implementation.

For applications that do not need to override the defaults, no additional browser configuration is necessary.

## Verify the dependency

After adding the dependency, run this command from the consuming service's directory:

```sh
mvn dependency:tree \
  -Dincludes=com.achhecode:achhecode-playwright
```

The output should show the library under the consuming application's dependency tree. For example:

```text
[INFO] com.example:sample-service:jar:1.0.0
[INFO] \- com.achhecode:achhecode-playwright:jar:1.0.0:compile
```

The application coordinates and versions in this example are illustrative.

### Verify a specific module in a multi-module project

From the repository root, run:

```sh
mvn -pl sample-service dependency:tree \
  -Dincludes=com.achhecode:achhecode-playwright
```

Replace `sample-service` with the actual Maven module name or path.

## Inspect the JAR

To list the contents of the installed library:

```sh
jar tf ~/.m2/repository/com/achhecode/achhecode-playwright/1.0.0/achhecode-playwright-1.0.0.jar
```

To check for the main configuration class and related configuration classes:

```sh
jar tf ~/.m2/repository/com/achhecode/achhecode-playwright/1.0.0/achhecode-playwright-1.0.0.jar \
  | grep -E 'PlaywrightCoreConfiguration|Configuration.class'
```

Verify that the expected classes are present before troubleshooting Spring configuration or dependency-resolution issues.

## Troubleshooting

### 1. Library not found

If Maven cannot resolve `achhecode-playwright`:

1. Run `mvn clean install` from the library's root directory.
2. Verify the installed artifact directory under `~/.m2/repository/com/achhecode/achhecode-playwright/1.0.0/`.
3. Confirm the group ID, artifact ID, and version in the consuming application's `pom.xml`.
4. Run `mvn dependency:tree -Dincludes=com.achhecode:achhecode-playwright`.

### 2. Configuration class not found

If the application cannot resolve `PlaywrightCoreConfiguration`:

1. Inspect the installed JAR using `jar tf`.
2. Confirm that `PlaywrightCoreConfiguration.class` is included.
3. Verify the import statement against the actual package declaration.
4. If you recently changed the library, rebuild and reinstall it, then reload or rebuild the consuming application.

### 3. Application fails to start

Inspect the Maven test reports for the underlying exception:

```sh
grep -n -E 'Caused by:|APPLICATION FAILED|Description:|Error creating bean|Exception' \
  sample-service/target/surefire-reports/*.txt
```

Replace `sample-service` with the actual module directory.

If the application fails during startup rather than during tests, also inspect the application's console output and application logs. Surefire reports contain test-related failures and may not exist for every startup failure.

Look for common causes such as:

- Missing configuration classes.
- Invalid property names or values.
- Browser startup failures.
- Missing browser binaries or operating-system dependencies.
- Port or CDP connection errors.

### 4. Browser fails to launch

- Confirm that the selected browser mode is correct.
- Ensure the required Playwright browser binaries are installed.
- If using `executable-path`, confirm that the path points to a valid, executable browser.
- Check the browser launch arguments and application logs.
- Verify that the environment has the operating-system libraries required by the browser.

### 5. Cannot attach to an existing browser

For `ATTACHED` mode:

- Start a compatible Chromium-based browser with remote debugging enabled.
- Ensure the configured host and port match the browser's CDP endpoint.
- Confirm that the endpoint is reachable from the consuming service.
- Check for port conflicts and firewall restrictions.

Only attach to a browser endpoint you trust. CDP access can grant substantial control over browser tabs and session data.

### 6. Errors during application shutdown

If shutdown logs contain errors such as `Failed to read message from driver, pipe closed`:

- Avoid declaring separate `Playwright` and `Browser` Spring beans that duplicate the library's managed instances.
- Do not independently call `close()` on resources owned by `PlaywrightManager`.
- Allow the library to manage browser-level cleanup.
- Close individual pages when their work is complete.
- Check whether another component has already shut down the Playwright driver.

The consuming application should use the library's public manager API rather than creating a second browser lifecycle.

## Design principles

- Keep browser initialization and lifecycle management inside the library.
- Let consuming services focus on their own automation workflows.
- Allow browser behavior to be configured through Spring Boot properties.
- Avoid duplicate browser instances and conflicting cleanup responsibilities.
- Keep page-level cleanup separate from browser-level shutdown.
