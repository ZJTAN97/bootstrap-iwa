# AGENTS.md - Development Guide for bootstrap-iwa

## Project Overview

Multi-module Spring Boot application (v4.0.3) with Java 25. Contains three microservices:
- **product**: Product service with MongoDB, RabbitMQ, MinIO
- **consumer**: Consumer service
- **search**: Search service with Elasticsearch

## Build Commands

### Full Build
```bash
./mvnw clean install
./mvnw clean package
```

### Running Tests
```bash
# Run all tests
./mvnw test

# Run tests for specific module
./mvnw -pl product test
./mvnw -pl consumer test
./mvnw -pl search test

# Run a single test class
./mvnw test -Dtest=ProductApplicationTests

# Run a single test method
./mvnw test -Dtest=ProductApplicationTests#testMethodName

# Run specific test with pattern
./mvnw test -Dtest="*Tests"
```

### Code Formatting (Spotless + Palantir)
```bash
# Check formatting (fails build if not formatted)
./mvnw spotless:check

# Apply formatting
./mvnw spotless:apply

# Apply formatting for specific module
./mvnw -pl product spotless:apply
```

### Running Applications
```bash
# Run from module directory
cd product && ../mvnw spring-boot:run
cd consumer && ../mvnw spring-boot:run
cd search && ../mvnw spring-boot:run
```

## Code Style Guidelines

### Formatting
- Uses **Palantir Java Format** (v2.83.0) via Spotless plugin
- Run `./mvnw spotless:apply` before committing
- 4-space indentation (standard Java)

### Naming Conventions
- **Classes**: PascalCase (e.g., `ProductController`, `ProductService`)
- **Records**: PascalCase (e.g., `Product`)
- **Packages**: lowercase with dots (e.g., `com.iwa.products.product`)
- **Methods/variables**: camelCase

### Imports
- Unused imports are automatically removed by Spotless
- Empty import order (no specific ordering enforced)
- Standard Spring imports first, then third-party, then java/... imports

### Language Features
- Java 25 features allowed (records, pattern matching, switch expressions)
- Use **records** for immutable DTOs/entities (e.g., `Product` record in product module)
- Use **sealed classes** where inheritance hierarchy needs control

### Project Structure
```
product/
  src/main/java/com/iwa/products/
    ProductApplication.java    # Main entry point
    product/                   # Domain layer
      Product.java            # Entity/record
      ProductController.java  # REST API
      ProductService.java     # Business logic
      ProductRepository.java  # Data access
    configuration/            # Configuration classes
    expiry/                   # Feature packages
consumer/
  src/main/java/com/iwa/consumer/
search/
  src/main/java/com/iwa/search/
```

### Spring Boot Patterns
- Use `@RestController` for REST endpoints
- Use `@Service` for business logic
- Use `@Repository` for data access
- Use constructor injection (preferred over field injection)

### Testing
- Spring Boot Test framework
- Test class naming: `*Tests` or `*Test` (e.g., `ProductApplicationTests`)
- Located in `src/test/java/`
- Use `@SpringBootTest` for integration tests

### Error Handling
- Use Spring's `@ExceptionHandler` or `@ControllerAdvice` for global exception handling
- Return appropriate HTTP status codes (4xx for client errors, 5xx for server errors)

### Configuration
- Application properties in `src/main/resources/application.properties` or `application.yml`
- Use `@ConfigurationProperties` for type-safe configuration properties
- Environment-specific configuration via Spring profiles (`application-{profile}.yml`)

### Logging
- Use SLF4J for logging (standard with Spring Boot)
- Follow log levels: ERROR for exceptions, WARN for degraded functionality, INFO for significant events, DEBUG for detailed flow

### Package Organization
- **product**: Domain-driven structure with `product/`, `configuration/`, `expiry/` packages
- **consumer**: Application entry point in root package, domain logic in subpackages
- **search**: Similar structure with `product/` for search-specific domain code

## Dependencies by Module

### Product Module
- Spring Data MongoDB (`spring-boot-starter-data-mongodb`)
- Spring Web MVC (`spring-boot-starter-webmvc`)
- Spring AMQP (`spring-boot-starter-amqp`)
- MinIO Java SDK (`minio:8.5.14`)
- Spring Docker Compose support

### Consumer Module
- Standard Spring Boot starters (see pom.xml for specifics)

### Search Module
- Spring Data Elasticsearch
- Spring Web MVC

## Testing Notes
- Test dependencies: `spring-boot-starter-test`, MongoDB test slice, AMQP test slice, WebMVC test slice
- Integration tests use `@SpringBootTest` with Docker Compose for infrastructure
- No Testcontainers currently configured - tests rely on Docker Compose services

## Dependencies

- Spring Boot 4.0.3
- Spring Data MongoDB
- Spring Data Elasticsearch
- Spring AMQP (RabbitMQ)
- MinIO (S3-compatible storage)
- Testcontainers not currently configured

## Docker Compose

Start all infrastructure services:
```bash
docker compose up -d
```

Services: MongoDB (27017), RabbitMQ (5672/15672), Elasticsearch (9200/9300), Kibana (5601), MinIO (9000/9001)
