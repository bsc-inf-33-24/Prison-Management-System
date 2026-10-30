# Super Admin API test guide

**Account:** initial admin (`admin` / `Admin123!`) or a Super Admin created through user management. Save login `accessToken` as `TOKEN_ADMIN`.

## User administration

`POST /api/v1/users` — create one test account per role. Repeat with a unique username and matching role each time:

The optional `email` field accepts a valid email address. Omit it for accounts without email. Users log in by setting the single `username` field to either their username or associated email address.

```json
{"fullName":"Test Records Officer","username":"records01","password":"Records123!","role":"Records Officer"}
```

Allowed role strings: `Super Admin`, `Records Officer`, `Disciplinary Officer`, `Case Officer`, `Visitor Officer`, `Rehabilitation Officer`, `Medical Officer`, `Duty Officer`. Password length is 8–16 characters.

- `GET /api/v1/users?status=active&role=Records%20Officer&search=records&page=0&size=20&sort=createdAt,desc`
- `GET /api/v1/users/{userUUID}`
- `PATCH /api/v1/users/{userUUID}/deactivate` and `PATCH /api/v1/users/{userUUID}/reactivate` (no body)
- `PUT /api/v1/users/{userUUID}/role`:

```json
{"role":"Records Officer","permissions":["inmates:read"]}
```

## Other allowed endpoints

- Inmate create/list/detail: `/api/v1/inmates` (GET/POST), `/api/v1/inmates/{inmateUUID}` (GET). Create body:

```json
{"firstName":"John","lastName":"Banda","dateOfBirth":"1990-04-15","gender":"Male","admissionDate":"2025-01-10","sentenceStartDate":"2025-01-10","sentenceEndDate":"2026-01-09","cellId":"a2bd2d14-15ba-44fc-92c1-3d6553d8e8f4"}
```

- Cell list/detail: `GET /api/v1/cells?blockName=A&classification=medium&page=0&size=20`, `GET /api/v1/cells/{cellUUID}`.
- Crime create/list/detail/update: `/api/v1/crimes` and `/api/v1/crimes/{crimeUUID}`. Body:

```json
{"offenceName":"Theft","description":"Test catalog offence","category":"Property"}
```

- Case create/list/detail/update/status and inmate cases: see [case guide](case-officer.md); Super Admin has the same access.
- Sentence create/list/detail/update: see [sentence examples in the full guide](../api-testing-user-guide.md#6-crimes-cases-and-sentences); replace IDs with UUIDs.
- Inmate incident history: `GET /api/v1/inmates/{inmateUUID}/incidents`.

## Denied checks

Confirm `403` for Super Admin on inmate update, cell create/allocation, incident create/outcome, visitor/visit create/update, and rehabilitation create/update. These operations are restricted to their specialist officer roles.
