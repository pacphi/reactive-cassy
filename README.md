# Reactive Cassandra Example

[![Build Status](https://app.travis-ci.com/pacphi/reactive-cassy.svg?branch=main)](https://app.travis-ci.com/pacphi/reactive-cassy) [![Known Vulnerabilities](https://snyk.io/test/github/pacphi/reactive-cassy/badge.svg)](https://snyk.io/test/github/pacphi/reactive-cassy)

A demonstration project showcasing [Spring Data Cassandra](https://spring.io/projects/spring-data-cassandra) reactive support with modern deployment practices.

## Overview

This project explores reactive programming patterns with Cassandra using:

- **Spring Boot 3.5+** with WebFlux
- **Spring Data Cassandra Reactive** repositories
- **Testcontainers** for integration testing
- **Docker** for local development
- **GitHub Actions** for CI/CD

## Quick Start

### Prerequisites

- Java 21+
- Docker
- Gradle 8.14+

### Local Development

1. **Clone and build**

   ```bash
   git clone https://github.com/pacphi/reactive-cassy.git
   cd reactive-cassy
   gradle clean build
   ```

2. **Start Cassandra and run the application**

   ```bash
   docker run -d -p 9042:9042 -e CASSANDRA_HOST=127.0.0.1 \
       --name cassandra-server \
       bitnami/cassandra:latest

   gradle bootRun -Dspring.profiles.active=docker
   ```

3. **Test the API**

   ```bash
   # Create a customer
   curl -X POST http://localhost:8080/customers \
     -H "Content-Type: application/json" \
     -d '{"firstName":"Nick","lastName":"Fury"}'

   # Stream customers
   curl http://localhost:8080/stream/customers
   ```

## Documentation

- **[Architecture](docs/ARCHITECTURE.md)** - Technical implementation details
- **[Build & CI](docs/BUILD.md)** - Build requirements and CI/CD pipeline
- **[Testing](docs/TEST.md)** - Testing strategy and setup
- **[Deployment & API](docs/RUN.md)** - Deployment options and API reference
- **[Azure CosmosDB](docs/AZURE.md)** - Azure cloud deployment with CosmosDB
- **[Appendices](docs/APPENDICES.md)** - Additional resources and references

## API Endpoints

- `POST /customers` - Create customer
- `GET /customers/{id}` - Get customer by ID
- `GET /customers?firstName=&lastName=` - Search customers
- `PUT /customers/{id}` - Update customer
- `DELETE /customers/{id}` - Delete customer
- `GET /stream/customers` - Stream all customers (Server-Sent Events)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is open source and available under the MIT License.
