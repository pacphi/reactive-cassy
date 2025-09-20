# Deployment & API Documentation

This document covers deployment options, environment configuration, and the complete API reference for the Reactive Cassandra Example application.

## Local Development

### Quick Start with Docker

1. **Start Cassandra Container**

   ```bash
   docker run -d -p 9042:9042 \
     -e CASSANDRA_HOST=127.0.0.1 \
     --name cassandra-server \
     bitnami/cassandra:latest
   ```

2. **Connect and Create Schema**

   ```bash
   # Wait for Cassandra to start (about 30-60 seconds)
   docker logs cassandra-server

   # Connect with cqlsh
   docker exec -it cassandra-server cqlsh -u cassandra -p cassandra

   # Create keyspace and table
   CREATE KEYSPACE IF NOT EXISTS customers WITH replication = {
     'class': 'SimpleStrategy',
     'replication_factor': '3'
   };
   USE customers;
   CREATE TABLE customer (
     id TimeUUID PRIMARY KEY,
     firstname text,
     lastname text
   );
   CREATE INDEX customerfirstnameindex ON customer (firstname);
   CREATE INDEX customerlastnameindex ON customer (lastname);
   ```

3. **Run Application**

   ```bash
   gradle bootRun -Dspring.profiles.active=docker
   ```

### Alternative: Docker Compose

Create `docker-compose.yml`:

```yaml
version: '3.8'
services:
  cassandra:
    image: bitnami/cassandra:latest
    environment:
      - CASSANDRA_HOST=127.0.0.1
    ports:
      - "9042:9042"
    volumes:
      - cassandra_data:/bitnami/cassandra

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

```bash
docker-compose up -d
```

## Environment Configuration

### Application Profiles

#### Default Profile

Basic configuration for development:

```yaml
spring:
  cassandra:
    keyspace-name: customers
    connection:
      init-query-timeout: PT10S
      connect-timeout: PT10S
    request:
      timeout: PT10S
    session-name: reactive-cassy
```

#### Docker Profile (`docker`)

For local Docker-based Cassandra:

```yaml
spring:
  config:
    activate:
      on-profile: docker
  cassandra:
    contact-points: 127.0.0.1
    port: 9042
    username: cassandra
    password: cassandra
```

#### Cloud Profile (`cloud`)

For cloud deployments with external secrets:

```yaml
spring:
  config:
    activate:
      on-profile: cloud
  cassandra:
    ssl:
      enabled: true
    contact-points: ${vcap.services.reactive-cassy-secrets.credentials.CONTACT_POINT}
    port: ${vcap.services.reactive-cassy-secrets.credentials.PORT}
    username: ${vcap.services.reactive-cassy-secrets.credentials.USERNAME}
    password: ${vcap.services.reactive-cassy-secrets.credentials.PASSWORD}
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `default` | Active Spring profile |
| `SERVER_PORT` | `8080` | Application port |
| `CASSANDRA_CONTACT_POINTS` | `localhost` | Cassandra host(s) |
| `CASSANDRA_PORT` | `9042` | Cassandra port |
| `CASSANDRA_USERNAME` | - | Database username |
| `CASSANDRA_PASSWORD` | - | Database password |

## Cloud Deployment

### Docker Deployment

#### Build Container Image

```bash
# Create Dockerfile
cat > Dockerfile << EOF
FROM eclipse-temurin:21-jre
VOLUME /tmp
COPY build/libs/reactive-cassy-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EOF

# Build application
gradle clean build

# Build Docker image
docker build -t reactive-cassy .

# Run container
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  reactive-cassy
```

#### Multi-stage Build

```dockerfile
FROM gradle:8.14-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle clean build -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/reactive-cassy-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Kubernetes Deployment

#### Deployment Manifest

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: reactive-cassy
spec:
  replicas: 3
  selector:
    matchLabels:
      app: reactive-cassy
  template:
    metadata:
      labels:
        app: reactive-cassy
    spec:
      containers:
      - name: reactive-cassy
        image: reactive-cassy:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "cloud"
        - name: CASSANDRA_CONTACT_POINTS
          valueFrom:
            secretKeyRef:
              name: cassandra-secrets
              key: contact-points
---
apiVersion: v1
kind: Service
metadata:
  name: reactive-cassy-service
spec:
  selector:
    app: reactive-cassy
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

### Cloud Foundry Deployment

#### Using CF CLI

```bash
# Login to CF
cf login -a https://api.your-cf-domain.com

# Create user-provided service for secrets
cf create-user-provided-service reactive-cassy-secrets -p config/secrets.json

# Deploy application
cf push reactive-cassy -f manifest.yml --no-start
cf bind-service reactive-cassy reactive-cassy-secrets
cf start reactive-cassy
```

#### Manifest Configuration

```yaml
---
applications:
- name: reactive-cassy
  memory: 1G
  instances: 2
  path: build/libs/reactive-cassy-0.0.1-SNAPSHOT.jar
  buildpacks:
    - java_buildpack_offline
  env:
    SPRING_PROFILES_ACTIVE: cloud
    JAVA_OPTS: -Djava.security.egd=file:///dev/urandom
```

## API Reference

### Base URL

- **Local Development**: `http://localhost:8080`
- **Production**: `https://your-domain.com`

### Content Types

- **Request**: `application/json`
- **Response**: `application/hal+json` (HATEOAS), `application/json`
- **Streaming**: `text/event-stream` (Server-Sent Events)

### Authentication

Currently, no authentication is implemented. For production use, consider adding:
- Spring Security with JWT tokens
- OAuth2/OIDC integration
- API key-based authentication

## API Endpoints

### Root Resource

Get API entry point with available links.

**Request:**

```http
GET /
Accept: application/hal+json
```

**Response:**

```json
{
  "_links": {
    "self": {
      "href": "http://localhost:8080/"
    },
    "stream/customers": {
      "href": "http://localhost:8080/stream/customers"
    }
  }
}
```

### Create Customer

Create a new customer record.

**Request:**

```http
POST /customers
Content-Type: application/json

{
  "firstName": "Tony",
  "lastName": "Stark"
}
```

**Response:** `201 Created`

```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "firstName": "Tony",
  "lastName": "Stark",
  "_links": {
    "self": {
      "href": "http://localhost:8080/customers/f47ac10b-58cc-4372-a567-0e02b2c3d479"
    }
  }
}
```

**Error Responses:**

- `400 Bad Request` - Invalid JSON or missing required fields
- `500 Internal Server Error` - Database connection issues

### Get Customer by ID

Retrieve a specific customer by their unique identifier.

**Request:**

```http
GET /customers/{id}
Accept: application/hal+json
```

**Response:** `200 OK`

```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "firstName": "Tony",
  "lastName": "Stark",
  "_links": {
    "self": {
      "href": "http://localhost:8080/customers/f47ac10b-58cc-4372-a567-0e02b2c3d479"
    }
  }
}
```

**Error Responses:**

- `404 Not Found` - Customer does not exist
- `400 Bad Request` - Invalid UUID format

### Update Customer

Update an existing customer's information.

**Request:**

```http
PUT /customers/{id}
Content-Type: application/json

{
  "firstName": "Anthony",
  "lastName": "Stark"
}
```

**Response:** `200 OK`

```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "firstName": "Anthony",
  "lastName": "Stark",
  "_links": {
    "self": {
      "href": "http://localhost:8080/customers/f47ac10b-58cc-4372-a567-0e02b2c3d479"
    }
  }
}
```

**Error Responses:**

- `404 Not Found` - Customer does not exist
- `400 Bad Request` - Invalid JSON or UUID format

### Search Customers

Search customers by first name and/or last name.

**Requests:**

```http
# Search by last name only
GET /customers?lastName=Stark

# Search by first name only
GET /customers?firstName=Tony

# Search by both names
GET /customers?firstName=Tony&lastName=Stark
```

**Response:** `200 OK`

```json
{
  "_embedded": {
    "customers": [
      {
        "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        "firstName": "Tony",
        "lastName": "Stark",
        "_links": {
          "self": {
            "href": "http://localhost:8080/customers/f47ac10b-58cc-4372-a567-0e02b2c3d479"
          }
        }
      }
    ]
  },
  "_links": {
    "self": {
      "href": "http://localhost:8080/customers?lastName=Stark"
    }
  }
}
```

**Error Responses:**

- `404 Not Found` - No customers match the search criteria
- `400 Bad Request` - No search parameters provided

### Delete Customer

Remove a customer from the system.

**Request:**

```http
DELETE /customers/{id}
```

**Response:** `200 OK`

```
(Empty response body)
```

**Error Responses:**

- `404 Not Found` - Customer does not exist
- `400 Bad Request` - Invalid UUID format

### Stream All Customers

Get a real-time stream of all customers using Server-Sent Events.

**Request:**

```http
GET /stream/customers
Accept: text/event-stream
```

**Response:** `200 OK`

```text
Content-Type: text/event-stream

data: {"id":"f47ac10b-58cc-4372-a567-0e02b2c3d479","firstName":"Tony","lastName":"Stark"}

data: {"id":"e58ed763-928c-4155-bee9-fdbaaadc15f3","firstName":"Steve","lastName":"Rogers"}

data: {"id":"6ba7b810-9dad-11d1-80b4-00c04fd430c8","firstName":"Bruce","lastName":"Banner"}
```

**Usage with curl:**

```bash
curl -N -H "Accept: text/event-stream" http://localhost:8080/stream/customers
```

**Usage with JavaScript:**

```javascript
const eventSource = new EventSource('/stream/customers');
eventSource.onmessage = function(event) {
  const customer = JSON.parse(event.data);
  console.log('Customer:', customer);
};
```

## Testing the API

### Using curl

```bash
# Create a customer
curl -X POST http://localhost:8080/customers \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Peter","lastName":"Parker"}'

# Get customer by ID (replace with actual ID)
curl http://localhost:8080/customers/123e4567-e89b-12d3-a456-426614174000

# Search customers
curl "http://localhost:8080/customers?lastName=Parker"

# Update customer
curl -X PUT http://localhost:8080/customers/123e4567-e89b-12d3-a456-426614174000 \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Spider","lastName":"Man"}'

# Delete customer
curl -X DELETE http://localhost:8080/customers/123e4567-e89b-12d3-a456-426614174000

# Stream customers
curl -N http://localhost:8080/stream/customers
```

### Using HTTPie

```bash
# Create a customer
http POST :8080/customers firstName=Peter lastName=Parker

# Get customer
http :8080/customers/123e4567-e89b-12d3-a456-426614174000

# Search customers
http GET :8080/customers lastName==Parker

# Update customer
http PUT :8080/customers/123e4567-e89b-12d3-a456-426614174000 \
  firstName=Spider lastName=Man

# Delete customer
http DELETE :8080/customers/123e4567-e89b-12d3-a456-426614174000
```

## Monitoring and Health

### Health Check Endpoint

```http
GET /actuator/health
```

**Response:**

```json
{
  "status": "UP",
  "components": {
    "cassandra": {
      "status": "UP",
      "details": {
        "version": "4.1.0"
      }
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### Application Info

```http
GET /actuator/info
```

### Metrics

```http
GET /actuator/metrics
GET /actuator/metrics/http.server.requests
```

## Performance Considerations

### Connection Pooling

The application uses the DataStax driver's built-in connection pooling:

- **Default pool size**: 1 connection per host
- **Max requests per connection**: 1024
- **Connection timeout**: 10 seconds

### Reactive Backpressure

The streaming endpoint automatically handles backpressure:

- Slow clients won't affect server performance
- Memory usage remains constant regardless of dataset size
- Configurable buffer sizes for fine-tuning

### Caching Strategies

Consider implementing caching for frequently accessed data:

- Redis for session-based caching
- Caffeine for local application caching
- CDN for static content

## Troubleshooting

### Common Issues

#### Connection Refused

```text
java.net.ConnectException: Connection refused
```

**Solution**: Ensure Cassandra is running and accessible on the configured port.

#### Keyspace Not Found

```text
InvalidQueryException: Keyspace 'customers' does not exist
```

**Solution**: Create the keyspace and tables using the provided CQL scripts.

#### Timeout Issues

```text
ReadTimeoutException: Cassandra timeout during read query
```

**Solution**: Increase timeout values in application configuration or optimize queries.

#### Memory Issues

```text
OutOfMemoryError: Java heap space
```

**Solution**: Increase JVM heap size with `-Xmx` flag or optimize data processing.

### Debugging Tips

1. **Enable DEBUG logging**:

   ```yaml
   logging:
     level:
       com.datastax: DEBUG
       org.springframework.data.cassandra: DEBUG
   ```

2. **Monitor connection pools**:

   ```http
   GET /actuator/metrics/cassandra.session.connected-nodes
   ```

3. **Check application logs**:

   ```bash
   tail -f logs/application.log
   ```
