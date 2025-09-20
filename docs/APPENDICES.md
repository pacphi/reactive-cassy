# Appendices

This document contains additional resources, references, and supporting information for the Reactive Cassandra Example project.

## Appendix A: External References

### Spring Framework Documentation

- [Building REST services with Spring](https://spring.io/guides/tutorials/rest/) - Comprehensive tutorial on REST API development
- [Spring Data for Cassandra - Reference Documentation](https://docs.spring.io/spring-data/cassandra/reference/cassandra.html) - Official Spring Data Cassandra documentation
- [Reactive Streams with Spring Data Cassandra](https://dzone.com/articles/reactive-streams-with-spring-data-cassandra) - Deep dive into reactive programming patterns
- [Spring Data Cassandra 2.0 - Reactive examples](https://github.com/spring-projects/spring-data-examples/tree/master/cassandra/reactive) - Official Spring examples

### Cassandra Resources

- [Apache Cassandra Documentation](http://cassandra.apache.org/doc/latest/) - Official Cassandra documentation
- [Cassandra with Java](https://www.baeldung.com/cassandra-with-java) - Java integration tutorials
- [DataStax Java Driver Documentation](https://docs.datastax.com/en/developer/java-driver/latest/) - Official driver documentation

### Reactive Programming

- [Project Reactor Reference Guide](https://projectreactor.io/docs/core/release/reference/) - Complete Reactor documentation
- [Reactive Streams Specification](https://www.reactive-streams.org/) - The reactive streams specification
- [Reactive Programming with Spring 5](https://www.baeldung.com/spring-5-reactive-web) - Spring WebFlux tutorials

### HATEOAS and REST

- [Spring HATEOAS - API Evolution Example](https://github.com/spring-projects/spring-hateoas-examples/tree/master/api-evolution) - Advanced HATEOAS patterns
- [Richardson Maturity Model](https://martinfowler.com/articles/richardsonMaturityModel.html) - REST API maturity levels
- [HAL - Hypertext Application Language](http://stateless.co/hal_specification.html) - HAL specification

### Testing Resources

- [Testcontainers Documentation](https://www.testcontainers.org/) - Integration testing with real dependencies
- [Testing Reactive Streams](https://projectreactor.io/docs/test/release/reference/) - Reactor testing guide
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/) - Spring Boot testing strategies

## Appendix B: Cloud Platform Resources

### Azure Cosmos DB

- [Introduction to the Azure Cosmos DB Cassandra API](https://docs.microsoft.com/en-us/azure/cosmos-db/cassandra-introduction) - Official Azure documentation
- [Azure Cosmos DB Cassandra API Limitations](https://docs.microsoft.com/en-us/azure/cosmos-db/cassandra-support) - Current API limitations
- [Azure Cosmos DB Pricing](https://azure.microsoft.com/en-us/pricing/details/cosmos-db/) - Cost planning information

### Amazon Keyspaces

- [Amazon Keyspaces (for Apache Cassandra)](https://aws.amazon.com/keyspaces/) - AWS managed Cassandra service
- [Getting Started with Amazon Keyspaces](https://docs.aws.amazon.com/keyspaces/latest/devguide/getting-started.html) - Setup guide

### Google Cloud Bigtable

- [Cloud Bigtable Documentation](https://cloud.google.com/bigtable/docs) - Google's NoSQL database service
- [Migrating from Cassandra to Bigtable](https://cloud.google.com/bigtable/docs/cassandra-migration) - Migration guide

## Appendix C: Development Tools

### IDEs and Editors

- [IntelliJ IDEA Spring Boot Plugin](https://plugins.jetbrains.com/plugin/7906-spring-boot) - Enhanced Spring development
- [Visual Studio Code Spring Boot Extension Pack](https://marketplace.visualstudio.com/items?itemName=Pivotal.vscode-boot-dev-pack) - VS Code Spring support
- [Eclipse Spring Tools](https://spring.io/tools) - Eclipse-based Spring IDE

### Database Tools

- [DataStax DevCenter](https://downloads.datastax.com/devcenter/) - Cassandra development environment
- [Cassandra Web](https://github.com/avalente/cassandra-web) - Web-based Cassandra administration
- [DBeaver](https://dbeaver.io/) - Universal database tool with Cassandra support

### API Testing Tools

- [HTTPie](https://httpie.io/) - User-friendly command-line HTTP client
- [Postman](https://www.postman.com/) - API development and testing platform
- [Insomnia](https://insomnia.rest/) - REST client with JSON support

### Monitoring and Observability

- [Micrometer](https://micrometer.io/) - Application metrics facade
- [Prometheus](https://prometheus.io/) - Time series monitoring system
- [Grafana](https://grafana.com/) - Observability and monitoring dashboards

## Appendix D: Configuration Examples

### Docker Compose for Development

```yaml
# docker-compose.dev.yml
version: '3.8'
services:
  cassandra:
    image: bitnami/cassandra:4.1
    environment:
      - CASSANDRA_SEEDS=cassandra
      - CASSANDRA_PASSWORD_SEEDER=yes
      - CASSANDRA_PASSWORD=cassandra
    ports:
      - "9042:9042"
    volumes:
      - cassandra_data:/bitnami/cassandra
      - ./src/test/resources/cql:/docker-entrypoint-initdb.d

  cassandra-web:
    image: metavige/cassandra-web
    environment:
      - CASSANDRA_HOST=cassandra
      - CASSANDRA_PORT=9042
      - CASSANDRA_USERNAME=cassandra
      - CASSANDRA_PASSWORD=cassandra
    ports:
      - "3000:3000"
    depends_on:
      - cassandra

  app:
    build: .
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    ports:
      - "8080:8080"
    depends_on:
      - cassandra

volumes:
  cassandra_data:
```

### Kubernetes ConfigMap

```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: reactive-cassy-config
data:
  application.yml: |
    spring:
      cassandra:
        keyspace-name: customers
        contact-points: cassandra-service
        port: 9042
        username: cassandra
        password: cassandra
        connection:
          init-query-timeout: PT30S
          connect-timeout: PT30S
        request:
          timeout: PT30S
        session-name: reactive-cassy-k8s
    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
      endpoint:
        health:
          show-details: always
```

### GitHub Actions Workflow

```yaml
# .github/workflows/integration.yml
name: Integration Tests

on:
  pull_request:
    branches: [ main ]

jobs:
  integration-test:
    runs-on: ubuntu-latest

    services:
      cassandra:
        image: cassandra:4.1
        ports:
          - 9042:9042
        env:
          CASSANDRA_CLUSTER_NAME: test-cluster
        options: >-
          --health-cmd "cqlsh -e 'describe keyspaces'"
          --health-interval 30s
          --health-timeout 10s
          --health-retries 10

    steps:
    - uses: actions/checkout@v5

    - name: Set up JDK 21
      uses: actions/setup-java@v5
      with:
        java-version: '21'
        distribution: 'liberica'
        cache: gradle

    - name: Wait for Cassandra
      run: |
        timeout 300 bash -c 'until cqlsh -e "describe keyspaces"; do sleep 5; done'

    - name: Create test schema
      run: |
        cqlsh -f src/test/resources/cql/simple.cql

    - name: Run integration tests
      run: ./gradlew integrationTest

    - name: Upload test results
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: test-results
        path: build/reports/tests/
```

## Appendix E: Performance Tuning

### Cassandra Configuration Optimizations

```yaml
# application-performance.yml
spring:
  cassandra:
    connection:
      pool:
        local:
          size: 8
        remote:
          size: 4
      heartbeat-interval: PT30S
      idle-timeout: PT5M
    request:
      timeout: PT10S
      consistency: LOCAL_QUORUM
      serial-consistency: LOCAL_SERIAL
      page-size: 5000
      throttler:
        type: RATE_LIMITING
        max-requests-per-second: 1000
```

### JVM Tuning for Production

```bash
# Production JVM settings
JAVA_OPTS="-Xms2g -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.backgroundpreinitializer.ignore=true"
```

### Connection Pool Optimization

```java
// Custom connection configuration
@Configuration
public class CassandraConfig {

    @Bean
    @Primary
    public CqlSessionBuilderCustomizer sessionBuilderCustomizer() {
        return builder -> builder
            .withLocalDatacenter("datacenter1")
            .withConfigLoader(
                DriverConfigLoader.programmaticBuilder()
                    .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(10))
                    .withDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT, Duration.ofSeconds(10))
                    .withDuration(DefaultDriverOption.CONNECTION_INIT_QUERY_TIMEOUT, Duration.ofSeconds(10))
                    .withInt(DefaultDriverOption.CONNECTION_POOL_LOCAL_SIZE, 4)
                    .withInt(DefaultDriverOption.CONNECTION_POOL_REMOTE_SIZE, 2)
                    .build()
            );
    }
}
```

## Appendix F: Security Guidelines

### SSL/TLS Configuration

```yaml
# SSL configuration for production
spring:
  cassandra:
    ssl:
      enabled: true
      keystore: classpath:cassandra-keystore.jks
      keystore-password: ${KEYSTORE_PASSWORD}
      truststore: classpath:cassandra-truststore.jks
      truststore-password: ${TRUSTSTORE_PASSWORD}
```

### Authentication Configuration

```java
// Custom authentication provider
@Configuration
public class SecurityConfig {

    @Bean
    public AuthenticationProvider cassandraAuthProvider() {
        return PlainTextAuthProvider.builder("username", "password").build();
    }
}
```

### Environment-Specific Security

```yaml
# Security profiles
---
spring:
  config:
    activate:
      on-profile: production
  cassandra:
    ssl:
      enabled: true
    username: ${CASSANDRA_USERNAME}
    password: ${CASSANDRA_PASSWORD}
security:
  require-ssl: true
  cors:
    allowed-origins: ${ALLOWED_ORIGINS:https://yourdomain.com}
```

## Appendix G: Troubleshooting Guide

### Common Error Messages and Solutions

#### "Cannot execute this query as it might involve data filtering"

```java
// Problem: Query without proper WHERE clause
repository.findByFirstName("John"); // May fail without index

// Solution 1: Add @AllowFiltering
@AllowFiltering
Flux<Customer> findByFirstName(String firstName);

// Solution 2: Use proper primary key design
@Query("SELECT * FROM customer WHERE id = ?0")
Mono<Customer> findById(UUID id);
```

#### "NoHostAvailableException: All host(s) tried for query failed"

```yaml
# Check connection configuration
spring:
  cassandra:
    contact-points: localhost  # Verify host is reachable
    port: 9042                # Verify port is correct
    local-datacenter: datacenter1  # Must match Cassandra config
```

#### Memory Leaks in Reactive Streams

```java
// Problem: Not properly disposing of subscriptions
Flux<Customer> customers = repository.findAll();
customers.subscribe(); // Memory leak!

// Solution: Proper subscription management
Disposable subscription = repository.findAll()
    .subscribe(
        customer -> processCustomer(customer),
        error -> handleError(error),
        () -> handleComplete()
    );

// Clean up when done
subscription.dispose();
```

## Appendix H: Migration Guides

### From Blocking to Reactive

```java
// Before: Blocking implementation
@RestController
public class CustomerController {

    @GetMapping("/customers/{id}")
    public ResponseEntity<Customer> getCustomer(@PathVariable UUID id) {
        Customer customer = repository.findById(id).orElse(null);
        return customer != null ? ResponseEntity.ok(customer) : ResponseEntity.notFound().build();
    }
}

// After: Reactive implementation
@RestController
public class CustomerEndpoints {

    @GetMapping("/customers/{id}")
    public Mono<ResponseEntity<EntityModel<Customer>>> getCustomer(@PathVariable UUID id) {
        return repository.findById(id)
            .map(customer -> ResponseEntity.ok(assembler.toModel(customer)))
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
```

### From Spring Data JPA to Cassandra

```java
// JPA Repository
public interface CustomerJpaRepository extends JpaRepository<Customer, UUID> {
    List<Customer> findByLastName(String lastName);
}

// Cassandra Repository
public interface CustomerRepository extends ReactiveCrudRepository<Customer, UUID> {
    @AllowFiltering
    Flux<Customer> findByLastName(String lastName);
}
```

## Appendix I: Version Compatibility Matrix

| Component | Version | Compatibility Notes |
|-----------|---------|-------------------|
| **Java** | 21+ | Required for Spring Boot 3.5+ |
| **Spring Boot** | 3.5.6 | Latest stable release |
| **Spring Data Cassandra** | 4.5+ | Included with Spring Boot |
| **Cassandra** | 4.0+ | Recommended for production |
| **DataStax Driver** | 4.17+ | Included with Spring Data |
| **Testcontainers** | 1.21.3 | Latest stable |
| **Docker** | 20.10+ | For development containers |
| **Gradle** | 8.14+ | Build tool requirement |

## Appendix J: Useful Scripts

### Database Schema Management

```bash
#!/bin/bash
# schema-setup.sh
set -e

CASSANDRA_HOST=${1:-localhost}
CASSANDRA_PORT=${2:-9042}

echo "Setting up schema on $CASSANDRA_HOST:$CASSANDRA_PORT"

cqlsh $CASSANDRA_HOST $CASSANDRA_PORT << EOF
CREATE KEYSPACE IF NOT EXISTS customers WITH replication = {
  'class': 'SimpleStrategy',
  'replication_factor': '3'
};

USE customers;

CREATE TABLE IF NOT EXISTS customer (
  id TimeUUID PRIMARY KEY,
  firstname text,
  lastname text
);

CREATE INDEX IF NOT EXISTS customerfirstnameindex ON customer (firstname);
CREATE INDEX IF NOT EXISTS customerlastnameindex ON customer (lastname);

DESCRIBE customers;
EOF

echo "Schema setup complete"
```

### Health Check Script

```bash
#!/bin/bash
# health-check.sh
set -e

APP_URL=${1:-http://localhost:8080}

echo "Checking application health at $APP_URL"

# Check health endpoint
HEALTH_STATUS=$(curl -s "$APP_URL/actuator/health" | jq -r '.status')

if [ "$HEALTH_STATUS" = "UP" ]; then
    echo "✅ Application is healthy"
    exit 0
else
    echo "❌ Application is unhealthy: $HEALTH_STATUS"
    exit 1
fi
```

### Load Testing Script

```bash
#!/bin/bash
# load-test.sh
set -e

APP_URL=${1:-http://localhost:8080}
REQUESTS=${2:-100}
CONCURRENCY=${3:-10}

echo "Running load test against $APP_URL"
echo "Requests: $REQUESTS, Concurrency: $CONCURRENCY"

# Create customers
ab -n $REQUESTS -c $CONCURRENCY -p customer.json -T application/json "$APP_URL/customers"

# Get customers
ab -n $REQUESTS -c $CONCURRENCY "$APP_URL/stream/customers"

echo "Load test complete"
```
