# Role test guides

Each guide is a standalone checklist for one account role. Start with the shared [test setup](../api-testing-user-guide.md#1-test-setup) and [create users and tokens](../api-testing-user-guide.md#2-create-users-and-tokens), then follow the selected role guide. IDs in this API are UUIDs; use the values returned by your API.

All requests use `http://localhost:8080`, `Content-Type: application/json`, and (except login/forgot/reset) `Authorization: Bearer <ROLE_TOKEN>`. Swagger UI is at `/swagger-ui/index.html`.

Common access checks: no token should return `401`; a valid token with a role not allowed for that endpoint should return `403`.
