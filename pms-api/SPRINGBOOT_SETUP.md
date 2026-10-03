# Spring Boot Setup Overview

This document explains the main components of the current backend setup and what each one is responsible for.

## 1) Application entry point

File: `src/main/java/com/pms/api/PmsApiApplication.java`

This is the main Spring Boot startup class. It contains:

```java
@SpringBootApplication
public class PmsApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(PmsApiApplication.class, args);
    }
}
```

What it does:

- bootstraps the Spring application
- enables component scanning
- loads configuration
- starts the embedded web server
- initializes the application context

In short, this is the class that starts the backend when the project runs.

## 2) Build and dependency management

File: `pom.xml`

This Maven file defines the project dependencies and build configuration. It is the backbone of the backend setup.

### Key dependencies in the current project

- `spring-boot-starter-webmvc`
  - provides the web layer for HTTP endpoints
  - supports REST API development

- `spring-boot-starter-data-jpa`
  - enables Spring Data JPA and ORM support
  - allows Java entities to map to database tables

- `spring-boot-starter-security`
  - adds authentication and authorization support
  - protects endpoints and manages access control

- `spring-boot-starter-validation`
  - validates incoming request data using annotations such as `@Valid`

- `spring-boot-starter-flyway`
  - manages database schema migration scripts

- `spring-boot-starter-actuator`
  - exposes health and monitoring endpoints for the app

- `postgresql`
  - JDBC driver for PostgreSQL database access

- `flyway-database-postgresql`
  - PostgreSQL support for Flyway migrations

- `spring-boot-testcontainers` and Testcontainers dependencies
  - used for integration testing with temporary PostgreSQL containers

### Java version

The project is configured for Java 21:

```xml
<java.version>21</java.version>
```

That means the backend is built and run on Java 21.

## 3) Spring Boot web layer

The web layer is responsible for exposing REST endpoints to clients such as mobile apps, frontend apps, or Postman.

In the current setup, the project includes:

- Spring MVC starter
- default HTTP server configuration
- support for request/response handling

This layer is where controllers, DTOs, request models, and API endpoints will eventually live.

As of now, the project is still at the initial scaffold stage, so the API controllers and business logic are not yet implemented in the repository structure shown.

## 4) Persistence layer (JPA and PostgreSQL)

The project uses Spring Data JPA with PostgreSQL.

### What this gives the app

- Java entity classes can map to database tables
- repositories can abstract SQL queries for CRUD operations
- the backend can persist prison-related records such as inmates, staff, logs, and audit data

### Database driver

The PostgreSQL driver is included so the application can connect to the configured database.

### Expected runtime configuration

The project uses environment variables such as:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`

The Docker Compose file passes these values into the Spring Boot app container so the app can connect to PostgreSQL.

## 5) Database migrations with Flyway

The project includes Flyway support:

- `spring-boot-starter-flyway`
- `flyway-database-postgresql`

What Flyway does:

- runs SQL migration scripts automatically on startup
- keeps DB schema creation and updates versioned and repeatable
- helps teams keep the database in sync across machines

This is especially important in a backend project where multiple developers might need the same schema structure.

The migration files live under:

- `src/main/resources/db/migration/`

These files are used to create tables, seed data, or update schema across versions.

## 6) Security layer

The project includes Spring Security:

- `spring-boot-starter-security`

What this does:

- secures endpoints
- can validate login credentials
- can enforce JWT or session-based security
- protects sensitive admin and prison system APIs

This is a critical part of the backend because the app likely needs to control access for users such as:

- admins
- officers
- staff members
- prisoners or restricted roles

At this point, the repository does not yet show custom security configuration classes, but the foundation is already included.

## 7) Validation layer

The validation starter provides support for request validation.

This allows the app to enforce rules like:

- required fields
- email format checks
- length constraints
- number ranges

Typical annotations include:

- `@NotNull`
- `@NotBlank`
- `@Email`
- `@Size`
- `@Min` / `@Max`

This helps prevent bad input from reaching the database or service logic.

## 8) Actuator and monitoring

The app includes Spring Boot Actuator.

This exposes operational endpoints like:

- health
- metrics
- application info
- readiness/liveness information

These are useful for:

- checking whether the backend is running
- monitoring server status
- diagnosing deployment problems

Actuator is especially helpful in Docker and production-like environments.

## 9) Test infrastructure

The project includes several testing libraries:

- Spring Boot test starter
- Testcontainers
- PostgreSQL Testcontainers
- JUnit support via Spring Boot test integrations

What this means:

- tests can run against a real PostgreSQL database in a container
- the application can be validated in a more realistic environment
- integration tests are easier to maintain than mock-only tests

This is a strong setup for backend reliability.

## 10) Docker interaction with the Spring app

The Spring Boot app is intended to run inside Docker, together with PostgreSQL. The Compose file defines:

- `db` service for PostgreSQL
- `app` service for the backend

The app uses Docker networking to talk to PostgreSQL through the internal service name `db`, while the host machine accesses the database through `localhost:5382`.

This is a typical pattern for containerized Spring applications and is useful for local development and deployment consistency.

## 11) Current project status

The codebase currently contains the basic Spring Boot structure, but it is still in an early phase. The essential backend building blocks are already in place:

- Spring Boot app bootstrap
- web stack
- security foundation
- JPA persistence
- Flyway database migration support
- PostgreSQL integration
- testing support

The next steps usually include:

- creating domain models
- creating repositories
- creating services
- creating controllers
- defining REST APIs
- writing migration scripts for database tables
- configuring application security rules

## Summary

This Spring Boot setup is a full backend foundation for a prison management system. It is designed to support:

- REST API development
- database persistence with PostgreSQL
- secure access control
- migration-based schema management
- health/monitoring checks
- automated testing with containers

It is a solid base for building the actual prison management domain and business features on top of it.
