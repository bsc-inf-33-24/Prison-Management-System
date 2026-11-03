# Prison Management System API

This project exposes a Spring Boot REST API for inmate administration, custody management, incident tracking, medical visits, visitor tracking, rehabilitation operations, and user accounts.

## Prerequisites and local startup

- Java 21 or later
- Docker Desktop with Docker Compose (recommended), or PostgreSQL 17 and a local database

For the Compose setup, work from `pms-api/`:

1. Copy `.env.example` to `.env`.
2. Set non-empty, development-only values for `DB_PASSWORD`, `JWT_SECRET`, `INITIAL_ADMIN_USERNAME`, and `INITIAL_ADMIN_PASSWORD`. Use a strong JWT secret (at least 32 bytes). Do not use development values in production.
3. Start the database and API:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

The API listens on `http://localhost:8080`; Compose publishes PostgreSQL on port `5382`. Flyway applies migrations at startup. The configured initial administrator can log in at `/api/v1/auth/login`.

To stop the containers, run `docker compose down`. The Compose database uses a named volume, so stopping containers does not remove its data.

For running the app with Maven instead, start a PostgreSQL database and set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `JWT_SECRET`; set `INITIAL_ADMIN_USERNAME` and `INITIAL_ADMIN_PASSWORD` to provision the initial administrator. Then run `.\mvnw spring-boot:run` from this directory.

## Base URL

- Local development: `http://localhost:8080`
- API prefix: `/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Security model

All business endpoints are protected by JWT bearer authentication unless the controller explicitly allows anonymous access.

Public endpoints:

- `GET /actuator/health`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/password/forgot`
- `POST /api/v1/auth/password/reset`
- Swagger UI and OpenAPI endpoints under `/swagger-ui/**` and `/v3/api-docs/**`

Protected endpoints require an `Authorization: Bearer <token>` header. A token is issued by `POST /api/v1/auth/login` and returns an `accessToken` and `tokenType` of `Bearer`.

## Core documentation

- [docs/api-reference.md](./docs/api-reference.md) — current REST API reference, endpoint inventory, and workflow examples
- [docs/testing/api-testing-user-guide.md](./docs/testing/api-testing-user-guide.md) — manual API testing notes and role-based checks

## Runtime notes

- Default pagination: `page=0`, `size=20`, maximum `size=100`
- Default sort is defined by each endpoint; supported sortable fields are validated server-side
- Validation errors return `400 Bad Request` with the `ApiError` envelope
- Authorization failures return `401` or `403` depending on whether no token was supplied or the caller is authenticated but not allowed to access the resource

## Known documentation drift

Older material, including `docs/testing/api-testing-user-guide.md`, `DOCKER_SETUP.md`, and `SPRINGBOOT_SETUP.md`, has not been fully reconciled with the current API/setup. Some testing examples reference endpoints and resources not currently implemented, for example `crimes`, `cases`, and a `PUT /api/v1/users/{userId}/role` route. Treat those routes and examples as historical, not active API contracts. Use [docs/api-reference.md](./docs/api-reference.md) for routes and the setup instructions above for current local startup.
