# Testing Documentation

This document covers the testing strategy, framework setup, and best practices for the Reactive Cassandra Example project.

## Testing Strategy

### Testing Pyramid

The project follows the testing pyramid approach:

```text
    /\
   /  \   Unit Tests (Fast, Isolated)
  /____\
 /      \  Integration Tests (Medium Speed, Real Dependencies)
/________\
          End-to-End Tests (Slow, Full System)
```

#### Test Types

1. **Unit Tests**
   - Test individual components in isolation
   - Mock external dependencies
   - Fast execution (< 1ms per test)

2. **Integration Tests**
   - Test components working together
   - Use real Cassandra via Testcontainers
   - Medium execution time (< 5s per test)

3. **Contract Tests**
   - Verify API contracts and HATEOAS links
   - Test reactive streams behavior
   - Validate JSON serialization/deserialization

## Testing Framework

### Core Testing Dependencies

```gradle
testImplementation('org.junit.jupiter:junit-jupiter-api')
testImplementation('org.junit.jupiter:junit-jupiter-params')
testRuntimeOnly('org.junit.jupiter:junit-jupiter-engine')
testImplementation('org.springframework.boot:spring-boot-starter-test')
testImplementation('org.springframework.boot:spring-boot-testcontainers')
testImplementation('org.testcontainers:cassandra:1.21.3')
testImplementation('org.testcontainers:junit-jupiter:1.21.3')
testImplementation('io.projectreactor:reactor-test:3.7.11')
testImplementation('org.mockito:mockito-core:5.19.0')
testImplementation('org.mockito:mockito-junit-jupiter:5.19.0')
```

### Testing Tools

- **JUnit 5** - Primary testing framework
- **Testcontainers** - Real Cassandra instances for integration tests
- **Reactor Test** - Testing reactive streams with StepVerifier
- **Spring Boot Test** - Spring application context testing
- **Mockito** - Mocking framework for unit tests

## Integration Testing with Testcontainers

### Cassandra Container Setup

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class ReactiveCustomerRepositoryIntegrationTest {

    @Container
    static final CassandraContainer cassandra =
        new CassandraContainer("cassandra:4.1")
            .withInitScript("cql/simple.cql")
            .withStartupTimeout(Duration.ofMinutes(3));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cassandra.contact-points", cassandra::getHost);
        registry.add("spring.cassandra.port", cassandra::getFirstMappedPort);
        registry.add("spring.cassandra.local-datacenter", () -> "datacenter1");
    }
}
```

#### Key Features

- **Isolated Environment**: Each test gets a fresh Cassandra instance
- **Real Database**: Tests run against actual Cassandra, not mocks
- **Schema Initialization**: Automatic schema setup via init scripts
- **Dynamic Configuration**: Test-specific connection properties

### Test Configuration

#### Test Application Properties

```yaml
# src/test/resources/application.yml
spring:
  cassandra:
    keyspace-name: customers
    schema-action: recreate
    local-datacenter: datacenter1
    connection:
      init-query-timeout: PT30S
      connect-timeout: PT30S
    request:
      timeout: PT30S
    session-name: reactive-cassy-test
```

#### Database Schema for Tests

```sql
-- src/test/resources/cql/simple.cql
CREATE KEYSPACE IF NOT EXISTS customers WITH replication = {
    'class': 'SimpleStrategy',
    'replication_factor': '1'
};
USE customers;
DROP TABLE IF EXISTS customer;
DROP INDEX IF EXISTS customerfirstnameindex;
DROP INDEX IF EXISTS customerlastnameindex;
CREATE TABLE customer (
    id TimeUUID PRIMARY KEY,
    firstname text,
    lastname text
);
CREATE INDEX customerfirstnameindex ON customer (firstname);
CREATE INDEX customerlastnameindex ON customer (lastname);
```

## Reactive Testing with StepVerifier

### Testing Reactive Streams

```java
@Test
public void shouldInsertAndRetrieveCustomer() {
    // Given
    Customer customer = Customer.builder()
        .withFirstName("Tony")
        .withLastName("Stark")
        .build();

    // When & Then
    StepVerifier.create(
        repository.save(customer)
            .then(repository.findByLastName("Stark"))
            .collectList()
    )
    .assertNext(customers -> {
        assertThat(customers).hasSize(1);
        assertThat(customers.get(0).getFirstName()).isEqualTo("Tony");
        assertThat(customers.get(0).getLastName()).isEqualTo("Stark");
    })
    .verifyComplete();
}
```

### StepVerifier Patterns

#### Testing Mono

```java
StepVerifier.create(repository.findById(customerId))
    .assertNext(customer -> {
        assertThat(customer.getFirstName()).isEqualTo("Expected");
    })
    .verifyComplete();
```

#### Testing Flux

```java
StepVerifier.create(repository.findAll())
    .expectNextCount(4)
    .verifyComplete();
```

#### Testing Empty Results

```java
StepVerifier.create(repository.findById(nonExistentId))
    .verifyComplete();
```

#### Testing Errors

```java
StepVerifier.create(invalidOperation())
    .expectError(IllegalArgumentException.class)
    .verify();
```

## Test Classes

### ReactiveCassandraTemplateIntegrationTest

Tests the lower-level `ReactiveCassandraTemplate` operations:

```java
@Test
public void shouldPerformCRUDOperations() {
    // Test direct template operations
    Flux<Customer> insertAndCount = template.truncate(Customer.class)
        .thenMany(insertTestData())
        .flatMap(template::insert)
        .thenMany(template.findAll(Customer.class));

    StepVerifier.create(insertAndCount)
        .expectNextCount(4)
        .verifyComplete();
}
```

### ReactiveCustomerRepositoryIntegrationTest

Tests the repository layer with higher-level operations:

```java
@Test
public void shouldFindCustomersByFirstName() {
    StepVerifier.create(repository.findByFirstName("Bruce"))
        .assertNext(customer -> {
            assertThat(customer.getFirstName()).isEqualTo("Bruce");
            assertThat(customer.getLastName()).isEqualTo("Banner");
        })
        .verifyComplete();
}
```

## Test Data Management

### Test Data Setup

```java
@BeforeEach
public void setUp() {
    Flux<Customer> truncateAndInsert = template.truncate(Customer.class)
        .thenMany(Flux.just(
            Customer.builder().withFirstName("Nick").withLastName("Fury").build(),
            Customer.builder().withFirstName("Tony").withLastName("Stark").build(),
            Customer.builder().withFirstName("Bruce").withLastName("Banner").build(),
            Customer.builder().withFirstName("Peter").withLastName("Parker").build()
        ))
        .flatMap(template::insert);

    StepVerifier.create(truncateAndInsert)
        .expectNextCount(4)
        .verifyComplete();
}
```

### Data Builder Pattern

```java
public class CustomerTestDataBuilder {
    public static Customer aCustomer() {
        return Customer.builder()
            .withFirstName("John")
            .withLastName("Doe")
            .build();
    }

    public static Customer aCustomerWithName(String firstName, String lastName) {
        return Customer.builder()
            .withFirstName(firstName)
            .withLastName(lastName)
            .build();
    }
}
```

## Running Tests

### Command Line

```bash
# Run all tests
gradle test

# Run specific test class
gradle test --tests "ReactiveCustomerRepositoryIntegrationTest"

# Run tests with specific pattern
gradle test --tests "*Repository*"

# Run tests with coverage
gradle test jacocoTestReport

# Run tests continuously
gradle test --continuous
```

### IDE Integration

#### IntelliJ IDEA

- Right-click test class → "Run Tests"
- Use JUnit 5 run configuration
- Enable test coverage in run configuration

#### VS Code

- Install "Test Runner for Java" extension
- Use Test Explorer sidebar
- Run individual tests or test classes

## Test Coverage

### JaCoCo Configuration

```gradle
jacoco {
    toolVersion = "0.8.13"
}

jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
        csv.required = false
    }
}
```

### Coverage Goals

| Component | Target Coverage |
|-----------|----------------|
| **Controllers** | 90%+ |
| **Services** | 95%+ |
| **Repositories** | 85%+ |
| **Models/DTOs** | 80%+ |

### Viewing Coverage Reports

```bash
# Generate coverage report
gradle jacocoTestReport

# Open HTML report
open build/reports/jacoco/test/html/index.html
```

## Best Practices

### Reactive Testing Guidelines

1. **Use StepVerifier** for all reactive stream testing
2. **Test both happy path and error scenarios**
3. **Verify complete signals** with `.verifyComplete()`
4. **Test backpressure** scenarios when applicable
5. **Use `expectNextCount()`** for performance tests

### Integration Test Guidelines

1. **Isolate tests** with proper setup/teardown
2. **Use realistic test data** that mirrors production
3. **Test edge cases** like empty results and large datasets
4. **Verify database state** after operations
5. **Keep tests deterministic** with controlled data

### Performance Testing

```java
@Test
public void shouldHandleLargeDataSets() {
    Flux<Customer> customers = Flux.range(1, 1000)
        .map(i -> Customer.builder()
            .withFirstName("Customer" + i)
            .withLastName("Test")
            .build())
        .flatMap(repository::save);

    StepVerifier.create(customers)
        .expectNextCount(1000)
        .verifyComplete();
}
```

## Troubleshooting

### Common Test Issues

#### Testcontainers Startup Timeout

```bash
# Increase startup timeout
.withStartupTimeout(Duration.ofMinutes(5))
```

#### Test Data Isolation

```bash
# Use @Sql or custom setup methods
@BeforeEach
public void cleanDatabase() {
    template.truncate(Customer.class).block();
}
```

#### Memory Issues with Large Tests

```bash
# Adjust JVM settings for tests
test {
    jvmArgs = ['-Xmx2g', '-XX:MaxMetaspaceSize=512m']
}
```

#### Docker Issues on CI

```bash
# Ensure Docker is available in CI environment
# Use smaller container images
# Configure proper timeouts
```
