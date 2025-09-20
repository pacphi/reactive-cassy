# Build & CI Documentation

This document covers build requirements, local development setup, and the CI/CD pipeline.

## Prerequisites

### Required Software

| Tool | Version | Purpose |
|------|---------|---------|
| **Java JDK** | 21+ | Runtime and compilation |
| **Gradle** | 8.14+ | Build automation |
| **Docker** | Latest | Local Cassandra instances |
| **Git** | Latest | Version control |

### Optional Tools

| Tool | Version | Purpose |
|------|---------|---------|
| **httpie** | 3.2.4+ | API testing |
| **jq** | 1.6+ | JSON processing |
| **cqlsh** | Latest | Cassandra CLI |

## Build System

### Gradle Configuration

The project uses Gradle with the following key plugins:

```gradle
plugins {
    id 'java'
    id 'jacoco'                                           // Test coverage
    id 'idea'                                            // IDE integration
    id 'eclipse'                                         // IDE integration
    id 'org.springframework.boot' version '3.5.6'       // Spring Boot
    id 'io.spring.dependency-management' version '1.1.7' // Dependency management
    id 'com.gorylenko.gradle-git-properties' version '2.5.3' // Git info
    id 'com.github.ben-manes.versions' version '0.52.0' // Dependency updates
}
```

### Build Commands

#### Basic Build

```bash
# Clean and build
gradle clean build

# Build without tests
gradle clean assemble

# Continuous build (watches for changes)
gradle build --continuous
```

#### Running the Application

```bash
# Default profile
gradle bootRun

# Docker profile (local Cassandra)
gradle bootRun -Dspring.profiles.active=docker

# With custom system properties
gradle bootRun -Djava.library.path=/custom/path
```

#### Testing

```bash
# Run all tests
gradle test

# Run tests with coverage
gradle test jacocoTestReport

# Run specific test class
gradle test --tests "CustomerEndpointsTest"

# Run tests continuously
gradle test --continuous
```

#### Code Quality

```bash
# Generate test coverage report
gradle jacocoTestReport

# Check dependency updates
gradle dependencyUpdates

# Generate Git properties
gradle generateGitProperties
```

### Java Version Compatibility

The project requires **Java 21** due to:

- Spring Boot 3.5+ baseline requirement
- Virtual threads support (Project Loom)
- Pattern matching enhancements
- Record patterns and switch expressions

**JVM Arguments for Testing:**

```bash
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
--add-opens java.base/java.nio=ALL-UNNAMED
# ... (additional opens for reflection access)
-Dnet.bytebuddy.experimental=true
```

## Dependency Management

### Core Dependencies

#### Spring Framework

```gradle
implementation('org.springframework.boot:spring-boot-starter-actuator')
implementation('org.springframework.boot:spring-boot-starter-data-cassandra-reactive')
implementation('org.springframework.boot:spring-boot-starter-webflux')
implementation('org.springframework.boot:spring-boot-starter-hateoas')
```

#### Reactive Libraries

```gradle
implementation('io.projectreactor:reactor-core:3.7.11')
testImplementation('io.projectreactor:reactor-test:3.7.11')
```

#### Testing Framework

```gradle
testImplementation('org.junit.jupiter:junit-jupiter-api')
testImplementation('org.junit.jupiter:junit-jupiter-params')
testRuntimeOnly('org.junit.jupiter:junit-jupiter-engine')
testImplementation('org.springframework.boot:spring-boot-starter-test')
testImplementation('org.springframework.boot:spring-boot-testcontainers')
testImplementation('org.testcontainers:cassandra:1.21.3')
testImplementation('org.testcontainers:junit-jupiter:1.21.3')
```

#### Utilities

```gradle
implementation('org.apache.commons:commons-lang3:3.18.0')
testImplementation('org.mockito:mockito-core:5.19.0')
testImplementation('org.mockito:mockito-junit-jupiter:5.19.0')
```

### Dependency Updates

Check for updates regularly:

```bash
gradle dependencyUpdates
```

Update Gradle wrapper:

```bash
gradle wrapper --gradle-version=8.14.1
```

## CI/CD Pipeline

### GitHub Actions Workflow

The project uses GitHub Actions for continuous integration. See [`.github/workflows/ci.yml`](../.github/workflows/ci.yml).

#### Workflow Triggers

- **Push** to `main` branch
- **Pull requests** to `main` branch

#### Build Matrix

```yaml
runs-on: ubuntu-latest
java-version: '21'
distribution: 'liberica'
cache: gradle
```

#### Pipeline Steps

1. **Checkout Code**

   ```yaml
   - uses: actions/checkout@v5
   ```

2. **Setup Java 21**

   ```yaml
   - name: Set up JDK 21
     uses: actions/setup-java@v5
     with:
       java-version: '21'
       distribution: 'liberica'
       cache: gradle
   ```

3. **Grant Execute Permissions**

   ```yaml
   - name: Grant execute permission for gradlew
     run: chmod +x gradlew
   ```

4. **Build Application**

   ```yaml
   - name: Build with Gradle
     run: ./gradlew build
   ```

5. **Run Tests**

   ```yaml
   - name: Run tests
     run: ./gradlew test
   ```

6. **Generate Coverage Report**

   ```yaml
   - name: Generate test report
     run: ./gradlew jacocoTestReport
   ```

7. **Upload Coverage to Codecov**

   ```yaml
   - name: Upload coverage reports to Codecov
     uses: codecov/codecov-action@v5
     with:
       files: ./build/reports/jacoco/test/jacocoTestReport.xml
       fail_ci_if_error: false
   ```

### Legacy CI (Travis CI)

The project maintains a Travis CI configuration for compatibility:

```yaml
language: java
jdk:
  - openjdk21
script:
  - "./gradlew clean build"
```

**Note:** The primary CI pipeline is GitHub Actions. Travis CI is kept for historical purposes.

## Local Development Setup

### Quick Setup

```bash
# Clone repository
git clone https://github.com/pacphi/reactive-cassy.git
cd reactive-cassy

# Build project
gradle clean build

# Start Cassandra
docker run -d -p 9042:9042 \
  -e CASSANDRA_HOST=127.0.0.1 \
  --name cassandra-server \
  bitnami/cassandra:latest

# Run application
gradle bootRun -Dspring.profiles.active=docker
```

### IDE Configuration

#### IntelliJ IDEA

```bash
# Generate IDEA project files
gradle idea

# Open project in IntelliJ
idea .
```

#### Eclipse

```bash
# Generate Eclipse project files
gradle eclipse

# Import into Eclipse workspace
```

#### VS Code

- Install **Extension Pack for Java**
- Install **Spring Boot Extension Pack**
- Open project folder

### Development Workflow

1. **Make Changes**
   - Edit source code
   - Add/modify tests

2. **Verify Build**

   ```bash
   gradle build
   ```

3. **Run Tests**

   ```bash
   gradle test jacocoTestReport
   ```

4. **Test Locally**

   ```bash
   gradle bootRun -Dspring.profiles.active=docker
   ```

5. **Commit Changes**

   ```bash
   git add .
   git commit -m "feat: add new feature"
   git push origin feature-branch
   ```

## Build Optimization

### Gradle Performance

```gradle
# gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m
```

### Docker Build Cache

```bash
# Use BuildKit for faster builds
export DOCKER_BUILDKIT=1

# Multi-stage build optimization
docker build --target=development .
```

### Test Optimization

```bash
# Parallel test execution
gradle test --parallel

# Test result caching
gradle test --build-cache
```

## Troubleshooting

### Common Build Issues

#### Java Version Mismatch

```bash
# Check Java version
java -version
javac -version

# Set JAVA_HOME
export JAVA_HOME=/path/to/java21
```

#### Gradle Permission Issues

```bash
# Fix gradlew permissions
chmod +x gradlew
```

#### Docker Connection Issues

```bash
# Verify Docker is running
docker ps

# Check Cassandra container
docker logs cassandra-server
```

#### Test Failures

```bash
# Run tests with debug info
gradle test --info --stacktrace

# Clean test cache
gradle clean test
```
