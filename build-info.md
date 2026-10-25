```sh
# Install achhecode-playwright
mvn clean install

# Verify the library was installed
ls -la ~/.m2/repository/com/achhecode/achhecode-playwright/1.0.0/


## verify this class as it will be used in @springbootapplication for other projects

jar tf ~/.m2/repository/com/achhecode/achhecode-playwright/1.0.0/achhecode-playwright-1.0.0.jar \
  | grep -E 'PlaywrightCoreConfiguration|Configuration.class'

```

### Use in another services:

Import dependencies:
```xml
<dependency>
    <groupId>com.achhecode</groupId>
    <artifactId>achhecode-playwright</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Verify if project contains the library

```sh
mvn dependency:tree -Dincludes=com.achhecode:achhecode-playwright

```
Output:

```txt
....................
--- dependency:3.10.0:tree (default-cli) @ irctc-web-service ---
[INFO] com.achhecode:irctc-web-service:jar:1.0.1
[INFO] \- com.achhecode:achhecode-playwright:jar:1.0.0:compile
[INFO] ------------------------------------------------------------------------
....................
```

##### Inspect the jar
```sh
jar tf ~/.m2/repository/com/achhecode/achhecode-playwright/1.0.0/achhecode-playwright-1.0.0.jar
```

### Verify the dependency:

```sh
mvn -pl irctc-web-service dependency:tree \
  -Dincludes=com.achhecode:achhecode-playwright
```


## View reports
```sh
grep -n -E 'Caused by:|APPLICATION FAILED|Description:|Error creating bean|Exception' \
  irctc-web-service/target/surefire-reports/*.txt
```