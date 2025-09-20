# Architecture

This document describes the technical architecture and implementation details of the Reactive Cassandra Example application.

## Technology Stack

### Core Framework

- **Spring Boot 3.5+** - Main application framework
- **Spring WebFlux** - Reactive web framework for non-blocking I/O
- **Spring Data Cassandra Reactive** - Reactive data access layer
- **Project Reactor** - Reactive streams implementation (Mono/Flux)

### Database

- **Apache Cassandra** - Distributed NoSQL database
- **DataStax Java Driver** - Native Cassandra connectivity
- **Spring Data Cassandra** - Repository abstraction and mapping

### Testing

- **JUnit 5** - Testing framework
- **Testcontainers** - Integration testing with real Cassandra instances
- **Reactor Test** - Testing reactive streams
- **Spring Boot Test** - Application context testing

## Application Architecture

### Reactive Programming Model

The application follows a fully reactive programming model using Project Reactor:

```text
Client Request → WebFlux Controller → Reactive Repository → Cassandra → Reactive Response
```

**Key Benefits:**

- Non-blocking I/O operations
- Better resource utilization
- Improved scalability under load
- Backpressure handling

### Data Model

#### Customer Entity

```java
@Table("customer")
public class Customer {
    @PrimaryKey
    private UUID id;          // TimeUUID for ordering

    @Column("firstname")
    private String firstName;

    @Column("lastname")
    private String lastName;
}
```

#### Database Schema

```cql
CREATE KEYSPACE customers WITH replication = {
    'class': 'SimpleStrategy',
    'replication_factor': '3'
};

CREATE TABLE customer (
    id TimeUUID PRIMARY KEY,
    firstname text,
    lastname text
);

CREATE INDEX customerfirstnameindex ON customer (firstname);
CREATE INDEX customerlastnameindex ON customer (lastname);
```

### Repository Layer

#### CustomerRepository Interface

```java
public interface CustomerRepository extends ReactiveCrudRepository<Customer, UUID> {
    Flux<Customer> findByFirstName(String firstName);
    Flux<Customer> findByLastName(String lastName);

    @Query("SELECT * FROM customer WHERE firstname IN (?0) AND lastname = ?1")
    @AllowFiltering
    Flux<Customer> findByFirstNameInAndLastName(String firstName, String lastName);
}
```

**Key Features:**

- Reactive CRUD operations returning Mono/Flux
- Custom query methods with @Query annotation
- @AllowFiltering for non-indexed queries
- Automatic UUID generation for new entities

### Web Layer

#### REST Controller (CustomerEndpoints)

```java
@RestController
public class CustomerEndpoints {

    @PostMapping("/customers")
    Mono<ResponseEntity<EntityModel<Customer>>> newCustomer(@RequestBody Customer customer);

    @GetMapping("/customers/{id}")
    Mono<ResponseEntity<EntityModel<Customer>>> getCustomerById(@PathVariable UUID id);

    @GetMapping("/stream/customers")
    Flux<Customer> streamAllCustomers();
}
```

**Key Features:**

- Reactive endpoints returning Mono/Flux
- HATEOAS support with EntityModel/CollectionModel
- Server-Sent Events for streaming
- Proper HTTP status code handling

### HATEOAS Implementation

The application implements HATEOAS (Hypermedia as the Engine of Application State) using Spring HATEOAS:

#### CustomerResourceAssembler

```java
@Component
public class CustomerResourceAssembler
    implements RepresentationModelAssembler<Customer, EntityModel<Customer>> {

    @Override
    public EntityModel<Customer> toModel(Customer customer) {
        return EntityModel.of(customer)
            .add(linkTo(methodOn(CustomerEndpoints.class).getCustomerById(customer.getId())).withSelfRel());
    }
}
```

**Benefits:**

- Self-describing APIs
- Improved API discoverability
- Loose coupling between client and server
- Standard HAL+JSON format

## Configuration Management

### Application Profiles

#### Default Profile

- Local development configuration
- Embedded configuration for basic connectivity

#### Docker Profile (`docker`)

```yaml
spring:
  cassandra:
    contact-points: 127.0.0.1
    port: 9042
    username: cassandra
    password: cassandra
```

#### Cloud Profile (`cloud`)

```yaml
spring:
  cassandra:
    ssl:
      enabled: true
    contact-points: ${vcap.services.reactive-cassy-secrets.credentials.CONTACT_POINT}
    port: ${vcap.services.reactive-cassy-secrets.credentials.PORT}
    username: ${vcap.services.reactive-cassy-secrets.credentials.USERNAME}
    password: ${vcap.services.reactive-cassy-secrets.credentials.PASSWORD}
```

### Connection Configuration

**Timeouts and Resilience:**

- Connection timeout: 10 seconds
- Request timeout: 10 seconds
- Init query timeout: 10 seconds
- Graceful shutdown: 30 seconds

## Design Patterns

### Repository Pattern

- Abstraction over data access logic
- Clean separation of concerns
- Testable business logic

### Resource Assembler Pattern

- Consistent HATEOAS link generation
- Reusable representation logic
- Type-safe model transformation

### Reactive Streams Pattern

- Non-blocking data flow
- Backpressure handling
- Composable operations

## Error Handling

### Reactive Error Handling

```java
return repository.findById(id)
    .map(customer -> ResponseEntity.ok(assembler.toModel(customer)))
    .defaultIfEmpty(ResponseEntity.notFound().build());
```

**Strategies:**

- `defaultIfEmpty()` for not found scenarios
- `onErrorReturn()` for fallback values
- Proper HTTP status codes
- Graceful degradation

## Performance Considerations

### Reactive Benefits

- **Non-blocking I/O:** Improved thread utilization
- **Backpressure:** Prevents resource exhaustion
- **Streaming:** Memory-efficient data processing

### Cassandra Optimizations

- **TimeUUID Primary Keys:** Natural ordering and uniqueness
- **Secondary Indexes:** Efficient queries on firstname/lastname
- **Replication Factor:** Data durability and availability

### Connection Pooling

- Managed by DataStax driver
- Automatic connection management
- Configurable pool sizes and timeouts
