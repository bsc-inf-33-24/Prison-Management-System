# Prison Management System REST API reference

This document reflects the current server implementation in the `pms-api` project. It is the source of truth for the public API contract and supersedes historical or stale notes that mention endpoints not present in the codebase.

## 1. API base and conventions

- Base URL: `http://localhost:8080`
- API prefix: `/api/v1`
- Content type for JSON request and response bodies: `application/json`
- Bearer token header for protected endpoints: `Authorization: Bearer <accessToken>`
- All public business endpoints are documented under the `/api/v1` prefix. The application also exposes Swagger UI and OpenAPI docs at `/swagger-ui/index.html` and `/v3/api-docs`.

## 2. Authentication and authorization

The security configuration explicitly permits the following requests without a token:

- `GET /actuator/health`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/password/forgot`
- `POST /api/v1/auth/password/reset`
- Swagger UI and OpenAPI metadata under `/swagger-ui/**` and `/v3/api-docs/**`

Every other endpoint requires a valid JWT bearer token. The token is issued by `POST /api/v1/auth/login` and contains the username as the subject, the user UUID as `uid`, and a verification counter as `ver`.

### Allowed roles

The `users` table only accepts the following role names:

- `Super Admin`
- `Records Officer`
- `Disciplinary Officer`
- `Case Officer`
- `Visitor Officer`
- `Instructor`
- `Medical Officer`
- `Duty Officer`
- `Prison Officer`
- `Program Facilitator`

The Spring Security rules are mapped to the `ROLE_...` authorities. For example, `Super Admin` becomes `ROLE_SUPER_ADMIN` and the check `hasRole('SUPER_ADMIN')` is used by user and admin endpoints.

## 3. Common response contracts

### Success payloads

Most endpoints return the DTO type directly. There is no global envelope around successful resource responses; the response is the resource JSON itself.

### Pagination response

User, inmate, cell, and rehabilitation list endpoints use a shared `PageResponse<T>` record:

```json
{
  "content": [
    { "id": "8d1e0e92-..." }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "sort": [
    { "property": "id", "direction": "ASC" }
  ]
}
```

The default configuration has recovery email disabled and uses an unavailable delivery implementation, which returns `503 Service Unavailable`. Configure the email recovery integration to make recovery delivery available; the generic `202` response applies when delivery is configured.

Pagination defaults:

- `page`: `0`
- `size`: `20`
- `max size`: `100`

### Error response

Exceptions handled by the application advice and the security entry/denied handlers use the `ApiError` schema:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "One or more request fields are invalid."
}
```

Common status codes:

- `400 Bad Request`: validation failure, invalid pagination or sort, bad query parameter, or invalid business input
- `401 Unauthorized`: missing/invalid JWT or invalid username/password
- `403 Forbidden`: authenticated caller does not have the required role/authority
- `404 Not Found`: resource or nested relation not found
- `409 Conflict`: duplicate username/email, conflicting state, or full cell capacity
- `429 Too Many Requests`: account temporarily locked after repeated failed logins
- `503 Service Unavailable`: password recovery delivery is unavailable

An unparseable UUID or date/time query value is rejected as `400`. A syntactically valid but unknown UUID generally returns `404` when the service performs a resource lookup. Unsupported HTTP methods/content types use Spring MVC's framework error handling and are not normalized by the application advice; their exact body is not specified here.

## 4. Endpoint inventory

### 4.1 Authentication

| Method | Path | Auth | Roles | Purpose |
|---|---|---:|---|---|
| `POST` | `/api/v1/auth/login` | No | Public | Sign in and receive a JWT |
| `POST` | `/api/v1/auth/logout` | Yes | Any authenticated user | Invalidate the current user’s access token state |
| `POST` | `/api/v1/auth/password/forgot` | No | Public | Start password recovery |
| `POST` | `/api/v1/auth/password/reset` | No | Public | Complete password recovery |
| `POST` | `/api/v1/auth/password/change` | Yes | Any authenticated user | Change the current user’s password |

### 4.2 User management

| Method | Path | Auth | Roles | Purpose |
|---|---|---:|---|---|
| `POST` | `/api/v1/users` | Yes | `Super Admin` | Create a user |
| `GET` | `/api/v1/users` | Yes | `Super Admin` | List/filter users |
| `GET` | `/api/v1/users/{userId}` | Yes | `Super Admin` | Fetch one user |
| `PATCH` | `/api/v1/users/{userId}` | Yes | `Super Admin` | Update user details or permissions |
| `PATCH` | `/api/v1/users/{userId}/deactivate` | Yes | `Super Admin` | Deactivate a user |
| `PATCH` | `/api/v1/users/{userId}/reactivate` | Yes | `Super Admin` | Reactivate a user |

### 4.3 Inmate records and custody

| Method | Path | Auth | Roles | Purpose |
|---|---|---:|---|---|
| `POST` | `/api/v1/inmates` | Yes | `Super Admin`, `Prison Officer` | Enroll an inmate and allocate a cell |
| `GET` | `/api/v1/inmates` | Yes | `Records Officer`, `Super Admin` | List inmates |
| `GET` | `/api/v1/inmates/{inmateId}` | Yes | `Records Officer`, `Super Admin` | View an inmate |
| `PATCH` | `/api/v1/inmates/{inmateId}` | Yes | `Super Admin` | Update an inmate record |

### 4.4 Cell allocation and inventory

| Method | Path | Auth | Roles | Purpose |
|---|---|---:|---|---|
| `PUT` | `/api/v1/inmates/{inmateId}/cell-allocation` | Yes | `Super Admin` | Move an inmate to another cell |
| `GET` | `/api/v1/cells` | Yes | `Records Officer`, `Super Admin` | List cells |
| `GET` | `/api/v1/cells/{cellId}` | Yes | `Records Officer`, `Super Admin` | View a cell |

### 4.5 Incidents and discipline

| Method | Path | Auth | Roles | Purpose |
|---|---|---:|---|---|
| `POST` | `/api/v1/incidents` | Yes | `Records Officer`, `Disciplinary Officer`, `Super Admin` | Log an incident |
| `GET` | `/api/v1/inmates/{inmateId}/incidents` | Yes | `Disciplinary Officer`, `Super Admin` | Read an inmate’s incident history |

### 4.6 Medical visits

| Method | Path | Auth | Roles | Purpose |
|---|---|---:|---|---|
| `POST` | `/api/v1/medical-visits` | Yes | `Medical Officer`, `Super Admin` | Record a medical visit |
| `GET` | `/api/v1/medical-visits` | Yes | `Medical Officer`, `Super Admin` | Search an inmate’s medical history |

### 4.7 Visitor records

| Method | Path | Auth | Roles | Purpose |
|---|---|---:|---|---|
| `POST` | `/api/v1/visits` | Yes | `Visitor Officer`, `Super Admin`, `Records Officer` | Create a visitor record |
| `GET` | `/api/v1/visits` | Yes | `Visitor Officer`, `Super Admin`, `Records Officer` | Search visits |
| `GET` | `/api/v1/inmates/{inmateId}/visits` | Yes | `Visitor Officer`, `Super Admin`, `Records Officer` | Fetch an inmate’s visit history |

### 4.8 Rehabilitation

| Method | Path | Auth | Roles | Purpose |
|---|---|---:|---|---|
| `POST` | `/api/v1/programs` | Yes | `Program Facilitator`, `Super Admin` | Create a program |
| `GET` | `/api/v1/programs` | Yes | `Program Facilitator`, `Super Admin`, `Records Officer`, `Instructor` | List/search programs |
| `GET` | `/api/v1/programs/{programId}` | Yes | `Program Facilitator`, `Records Officer`, `Super Admin`, `Instructor` | View a program |
| `PATCH` | `/api/v1/programs/{programId}` | Yes | `Program Facilitator`, `Super Admin` | Update a program |
| `POST` | `/api/v1/programs/enrollments` | Yes | `Program Facilitator`, `Super Admin` | Enroll an inmate |
| `GET` | `/api/v1/programs/enrollments` | Yes | `Program Facilitator`, `Records Officer`, `Super Admin` | List enrollment records |
| `POST` | `/api/v1/programs/attendance` | Yes | `Program Facilitator`, `Super Admin`, `Instructor` | Record attendance |
| `POST` | `/api/v1/programs/attendance/bulk` | Yes | `Program Facilitator`, `Super Admin`, `Instructor` | Record bulk attendance |
| `GET` | `/api/v1/programs/attendance` | Yes | `Program Facilitator`, `Records Officer`, `Super Admin` | Search attendance records |

### 4.9 Inmate transfers

| Method | Path | Auth | Roles | Purpose |
|---|---|---:|---|---|
| `POST` | `/api/v1/transfers` | Yes | `Records Officer`, `Super Admin`, `Prison Officer` | Submit an inmate transfer |
| `GET` | `/api/v1/transfers` | Yes | `Records Officer`, `Super Admin` | List transfers |
| `POST` | `/api/v1/transfers/{transferId}/approve` | Yes | `Records Officer`, `Super Admin` | Approve a transfer |
| `POST` | `/api/v1/transfers/{transferId}/reject` | Yes | `Records Officer`, `Super Admin` | Reject a transfer |
| `POST` | `/api/v1/transfers/{transferId}/cancel` | Yes | `Records Officer`, `Super Admin` | Cancel a transfer |

### 4.10 Monitoring

| Method | Path | Auth | Roles | Purpose |
|---|---|---:|---|---|
| `GET` | `/actuator/health` | No | Public | Spring Actuator health endpoint |

### 4.11 Duty rosters

| Method | Path | Auth | Roles | Purpose |
|---|---|---:|---|---|
| `POST` | `/api/v1/rosters` | Yes | `Duty Officer`, `Super Admin` | Create a draft roster |
| `GET` | `/api/v1/rosters` | Yes | `Duty Officer`, `Super Admin` | List rosters in the caller's prison, or filter by `prisonId` as Super Admin |
| `GET` | `/api/v1/rosters/{rosterId}` | Yes | `Duty Officer`, `Super Admin` | View a roster and its shifts |
| `PATCH` | `/api/v1/rosters/{rosterId}` | Yes | `Duty Officer`, `Super Admin` | Change the date range of a draft roster |
| `POST` | `/api/v1/rosters/{rosterId}/shifts` | Yes | `Duty Officer`, `Super Admin` | Add a shift to a draft roster |
| `PATCH` | `/api/v1/rosters/{rosterId}/shifts/{shiftId}` | Yes | `Duty Officer`, `Super Admin` | Update a shift; published changes notify affected officers |
| `DELETE` | `/api/v1/rosters/{rosterId}/shifts/{shiftId}` | Yes | `Duty Officer`, `Super Admin` | Remove a draft shift or cancel a published shift |
| `POST` | `/api/v1/rosters/{rosterId}/publish` | Yes | `Duty Officer`, `Super Admin` | Revalidate and publish a roster |
| `POST` | `/api/v1/rosters/{rosterId}/close` | Yes | `Duty Officer`, `Super Admin` | Close a published roster after its period ends |
| `GET` | `/api/v1/me/shifts?from=&to=` | Yes | `Prison Officer` | View the authenticated officer's published shifts |

Roster dates are inclusive in the configured roster time zone (`ROSTERS_TIME_ZONE`, default `Africa/Blantyre`). Shift timestamps must be ISO-8601 offset date-times. A shift may end exactly at midnight after the roster's final date, and its duration may not exceed 24 hours. An officer must be active, have the `Prison Officer` role, and belong to the roster's prison. Overlaps use half-open time ranges and return `409 Conflict`; roster mutations lock the assigned officer row while checking and saving to serialize concurrent assignments.

Only a Duty Officer's assigned prison is accessible. Super Admins must supply `prisonId` when creating a roster and may use it to filter lists. Each roster response contains public UUIDs, status, timestamps, and shifts. Publishing creates one in-app notification per distinct assigned officer; published shift updates, reassignments, and cancellations notify the affected officer(s). Draft shifts are not returned by `/api/v1/me/shifts`.

## 5. Request and response details by area

### 5.1 Authentication endpoints

#### `POST /api/v1/auth/login`

Purpose: authenticate a user and return a JWT bearer token.

Request body (the API sets new program status to `active`; status is not accepted in this request):

```json
{
  "username": "records01",
  "password": "Records123!"
}
```

or

```json
{
  "email": "records.officer@example.test",
  "password": "Records123!"
}
```

Rules:

- Exactly one of `username` or `email` must be supplied.
- `username` and `email` are trimmed; `email` is lowercased automatically.
- `password` is required and must not be blank; maximum length is 16 characters.
- The server checks the user’s locked status before attempting authentication.

Success `200 OK` example:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer"
}
```

Error examples:

- `401 Unauthorized` for invalid credentials or missing/invalid token
- `429 Too Many Requests` if the user is temporarily locked after excessive failed login attempts

#### `POST /api/v1/auth/logout`

Purpose: invalidate the authenticated user’s current token generation state.

Auth: bearer token required.

Success: `204 No Content`.

#### `POST /api/v1/auth/password/forgot`

Purpose: start password recovery using the account's registered email address. For an active account with a configured recovery email, a six-digit code is sent to that address. The API returns the same generic success message regardless of whether the address is registered.

Request body:

```json
{
  "userEmail": "records01@example.com"
}
```

Success `202 Accepted` example:

```json
{
  "message": "If the account can receive recovery instructions, they have been sent."
}
```

#### `POST /api/v1/auth/password/reset`

Purpose: consume a single-use, short-lived recovery code and replace the user’s password.

Request body:

```json
{
  "code": "<6-digit recovery code>",
  "newPassword": "NewPass123!"
}
```

Rules:

- `newPassword` must be 8–16 characters long
- password must satisfy the application password policy (must contain a digit and a punctuation mark)
- codes must contain exactly six digits
- reused or expired codes fail with `400 Bad Request`

Success: `204 No Content`.

#### `POST /api/v1/auth/password/change`

Purpose: change the logged-in user’s password.

Request body:

```json
{
  "currentPassword": "OldPass123!",
  "newPassword": "NewPass123!"
}
```

Rules:

- `currentPassword` must match the stored password hash
- the new password must not match the current password
- invalid passwords or weak passwords yield `400 Bad Request`

Success: `204 No Content`.

### 5.2 User management

#### `POST /api/v1/users`

Purpose: create a user account. Usable only by the super administrator.

Request body examples:

```json
{
  "fullName": "Records Officer",
  "username": "records01",
  "email": "records.officer@example.test",
  "password": "Records123!",
  "role": "Records Officer"
}
```

or without email:

```json
{
  "fullName": "Records Officer",
  "username": "records01",
  "password": "Records123!",
  "role": "Records Officer"
}
```

Validation:

- `fullName` required
- `username` required, maximum 16 characters
- `password` required, 8–16 characters; must contain at least one digit and one punctuation or special character
- `role` must be one of the allowed roles
- `email` is optional; if supplied it must be a valid email and is normalized to lowercase

Success `201 Created` response example:

```json
{
  "id": "4a3d7b2a-6c97-4b33-962a-0d1db78b86ce",
  "fullName": "Records Officer",
  "username": "records01",
  "email": "records.officer@example.test",
  "role": "Records Officer",
  "status": "active",
  "permissions": [],
  "createdAt": "2026-10-29T12:00:00Z",
  "updatedAt": "2026-10-29T12:00:00Z"
}
```

Notes:

- The `id` field is the public UUID used in subsequent path values.
- The internal numeric database key is never exposed to clients.

#### `GET /api/v1/users`

Purpose: list and filter users.

Query parameters:

- `status`: optional, values are `active` or `inactive`
- `role`: optional, must be a valid role string
- `search`: optional partial match over `fullName`, `username`, or `email`
- `page`: optional integer, default `0`
- `size`: optional integer, default `20`, max `100`
- `sort`: optional sort expression such as `createdAt,desc` or `username,asc`; valid properties are `id`, `fullName`, `username`, `role`, `status`, and `createdAt`

Success `200 OK` returns `PageResponse<UserResponse>`. Each user includes `prisonId` and `prisonName`; both are `null` when the user has no assigned prison.

#### `GET /api/v1/users/{userId}`

Purpose: fetch one user by public UUID.

Path parameter:

- `userId`: public UUID

Success `200 OK` returns the same `UserResponse` shape as create/update.

#### `PATCH /api/v1/users/{userId}`

Purpose: update user details and optionally role/permissions.

Body example:

```json
{
  "fullName": "Senior Records Officer",
  "username": "records02",
  "email": "records2@example.test",
  "role": "Records Officer",
  "permissions": ["inmates:read"]
}
```

Rules:

- fields are optional; null or omitted values are left unchanged
- username/email must be unique
- role validation uses the same allowed-role list as user creation

#### `PATCH /api/v1/users/{userId}/deactivate`

Purpose: deactivate the target user without deleting the record.

Success `200 OK` returns the updated record with `status` set to `inactive`.

#### `PATCH /api/v1/users/{userId}/reactivate`

Purpose: reactivate an inactive user.

Success `200 OK` returns the updated record with `status` set to `active`.

### 5.3 Inmate records

#### `POST /api/v1/inmates`

Purpose: create a new inmate record and assign them to an existing cell in one transaction.

Request body:

```json
{
  "firstName": "John",
  "lastName": "Banda",
  "dateOfBirth": "1990-04-15",
  "gender": "Male",
  "admissionDate": "2025-01-10",
  "offence": "Theft",
  "sentenceStartDate": "2025-01-10",
  "sentenceEndDate": "2026-01-09",
  "cellId": "4a3d7b2a-6c97-4b33-962a-0d1db78b86ce"
}
```

Validation:

- `firstName` and `lastName` are required, max 100 characters; `gender` is required, max 20 characters. Values are trimmed before persistence.
- `dateOfBirth` must be in the past
- `admissionDate` is required
- `offence` is optional and, when supplied, must be 1–500 characters
- `sentenceStartDate` and `sentenceEndDate` required
- `sentenceEndDate` must be on or after `sentenceStartDate`
- `cellId` required; if the assigned cell is full, the API returns `409 Conflict`

Success `201 Created` returns `InmateResponse`: public UUID `id`, names, birth/admission dates, gender, offence (nullable), calculated sentence duration and unit, sentence dates, public UUID `cellId`, and `createdAt`/`updatedAt` instants. The server creates the allocation as part of enrollment; no separate allocation call is needed.

Representative response:

```json
{
  "id": "7d0df6a2-0da6-4309-9c2d-7c50b1f939e3",
  "firstName": "John",
  "lastName": "Banda",
  "dateOfBirth": "1990-04-15",
  "gender": "Male",
  "admissionDate": "2025-01-10",
  "offence": "Theft",
  "sentenceDurationValue": 1,
  "sentenceDurationUnit": "YEARS",
  "sentenceStartDate": "2025-01-10",
  "sentenceEndDate": "2026-01-09",
  "cellId": "4a3d7b2a-6c97-4b33-962a-0d1db78b86ce",
  "createdAt": "2026-10-29T10:00:00Z",
  "updatedAt": "2026-10-29T10:00:00Z"
}
```

#### `GET /api/v1/inmates`

Query parameters:

- `search`: optional text search
- `gender`: optional exact gender match
- `page`: optional integer, default `0`
- `size`: optional integer, default `20`, max `100`
- `sort`: optional repeated parameter; each value is `property[,asc|desc]`. Allowed properties: `id`, `firstName`, `lastName`, `admissionDate`, `createdAt`. Default sort: `id,asc`.

Success `200 OK` returns `PageResponse<InmateResponse>`.
`search` is a case-insensitive partial match against first and last names only.

#### `GET /api/v1/inmates/{inmateId}`

Purpose: fetch a single inmate by public UUID.

#### `PATCH /api/v1/inmates/{inmateId}`

Purpose: update a previously created inmate record.

The patch body fields are optional: `firstName`, `lastName`, and `gender` (nonblank when provided); `dateOfBirth` (must be in the past); `admissionDate`. Other create fields, including `cellId`, offence, and sentence dates, are not updated by this endpoint. Success is `200 OK` with `InmateResponse`.

### 5.4 Cells

#### `PUT /api/v1/inmates/{inmateId}/cell-allocation`

Purpose: move an inmate to a different cell.

Request body:

```json
{
  "cellId": "2d561be3-7ceb-42ef-a321-72859af5204c"
}
```

Rules:

- `cellId` is required
- target cell must exist
- the target cell must have space
- moving to the same cell is rejected as a conflict
- the inmate must already exist

Success `200 OK` returns `CellMoveResponse` with details of the move.

#### `GET /api/v1/cells`

Query parameters:

- `blockName`: optional
- `page`: optional integer, default `0`
- `size`: optional integer, default `20`, max `100`
- `sort`: optional repeated parameter; allowed properties: `id`, `blockName`, `cellNumber`, `capacity`. Default sort: `id,asc`.

Success `200 OK` returns `PageResponse<CellResponse>`.

#### `GET /api/v1/cells/{cellId}`

Purpose: fetch one cell by public UUID.

Success `200 OK` returns a `CellDetailResponse` with the cell’s current inmate assignments and availability information.
Cell IDs in allocation request bodies and cell paths are the public UUIDs returned as `id`, not numeric database IDs. The database enforces uniqueness of the `(blockName, cellNumber)` pair; a duplicate conflicts with existing data and returns `409`.

### 5.5 Incidents and discipline

#### `POST /api/v1/incidents`

Purpose: log a disciplinary incident and link one or more inmates to it.

Request body:

```json
{
  "participants": [
    {
      "inmateId": "7d0df6a2-0da6-4309-9c2d-7c50b1f939e3",
      "roleInIncident": "Primary participant"
    },
    {
      "inmateId": "a46fd6f8-1db1-4e4d-9bcb-c1d7be4f2d7d",
      "roleInIncident": "Witness"
    }
  ],
  "incidentDate": "2026-10-29",
  "description": "Inmate quarrel in the recreation yard.",
  "outcome": "Warning issued"
}
```

Validation:

- at least one participant required
- each participant must include a valid `inmateId`
- `incidentDate` cannot be in the future
- `description` required
- duplicate inmate IDs are rejected
- `outcome` is optional; when supplied it must not be blank and must be at most 255 characters

Omit `outcome` or supply it as `null` to create the incident with `open` status and a null outcome. Supplying a nonblank outcome creates it with `resolved` status and stores the outcome.

Success `201 Created` returns an `IncidentResponse` with the recorded incident details and linked inmate set. For the request above, the response includes:

```json
{
  "id": "0ecb9777-8fa9-4c36-9527-011e6b9684de",
  "incidentDate": "2026-10-29",
  "description": "Inmate quarrel in the recreation yard.",
  "status": "resolved",
  "outcome": "Warning issued",
  "reportedBy": "Officer Name",
  "participants": [
    {
      "inmateId": "7d0df6a2-0da6-4309-9c2d-7c50b1f939e3",
      "roleInIncident": "Primary participant"
    },
    {
      "inmateId": "a46fd6f8-1db1-4e4d-9bcb-c1d7be4f2d7d",
      "roleInIncident": "Witness"
    }
  ]
}
```

Without `outcome`, the response has `"status": "open"` and `"outcome": null`.

`IncidentResponse` contains `id` (public UUID), `incidentDate`, `description`, `status`, nullable `outcome`, `reportedBy` (text name, not a user ID), and `participants` entries (`inmateId` public UUID and nullable `roleInIncident`).

#### `GET /api/v1/inmates/{inmateId}/incidents`

Purpose: retrieve a single inmate’s incident history.

Success `200 OK` returns a list of `IncidentHistoryItemResponse` objects.

### 5.6 Medical visits

#### `POST /api/v1/medical-visits`

Purpose: record an inmate’s medical consultation.

Request body:

```json
{
  "inmateId": "7d0df6a2-0da6-4309-9c2d-7c50b1f939e3",
  "visitDate": "2026-10-29",
  "sickness": "Fever and headache",
  "diagnosis": "Mild dehydration",
  "medicationsProvided": "Oral rehydration salts",
  "recommendations": "Rest and hydration for 24 hours"
}
```

Validation:

- `inmateId` required
- `visitDate` required and not in the future
- `sickness` and `diagnosis` required
- text fields are capped at 10000 characters

Success `201 Created` returns a `MedicalVisitResponse` describing the recorded visit.

The response fields are `id` (public UUID), `visitDate`, `sickness`, `diagnosis`, nullable `medicationsProvided` and `recommendations`, `recordedBy` (officer full name), and `recordedAt` (instant). `GET /api/v1/medical-visits` returns a medical history resource; the controller accepts the inmate public UUID as required `inmateId` and optional `name`.

```json
{
  "id": "8d1e0e92-9416-4cdc-847e-6be688a4e8d1",
  "visitDate": "2026-10-29",
  "sickness": "Fever and headache",
  "diagnosis": "Mild dehydration",
  "medicationsProvided": "Oral rehydration salts",
  "recommendations": "Rest and hydration for 24 hours",
  "recordedBy": "Medical Officer",
  "recordedAt": "2026-10-29T10:00:00Z"
}
```

#### `GET /api/v1/medical-visits`

Query parameters:

- `inmateId`: required UUID
- `name`: optional string filter by inmate name

Success `200 OK` returns `MedicalHistoryResponse`.

### 5.7 Visitor tracking

#### `POST /api/v1/visits`

Purpose: record a visitor visit.

Request body:

```json
{
  "visitorFullName": "Amina Phiri",
  "inmateId": "7d0df6a2-0da6-4309-9c2d-7c50b1f939e3",
  "visitDate": "2026-10-29",
  "startTime": "09:30",
  "endTime": "10:15"
}
```

Validation:

- `visitorFullName` required, up to 255 characters
- `inmateId` required
- `visitDate` required
- `startTime` and `endTime` required
- `endTime` must not be earlier than `startTime`
- duration is calculated server-side

Success `201 Created` returns `VisitResponse`.

#### `GET /api/v1/visits`

Query parameters:

- `visitId`: optional UUID
- `visitorName`: optional string filter
- `inmateId`: optional UUID
- `inmateName`: optional name filter
- `visitDateFrom`: optional `OffsetDateTime`
- `visitDateTo`: optional `OffsetDateTime`
- `loggedBy`: optional user UUID
- `page`: default `0`
- `size`: default `20`, maximum `100`
- `sort`: optional `field[,asc|desc]`; allowed aliases are `visitId`, `inmateId`, `visitDate`, `visitDateTime`, `startDateTime`, `endDateTime`, `loggedBy`, `visitorName`, and `inmateName`. Default is `startDateTime,desc`. An unknown sort field falls back to the default; a direction other than `desc` is treated as ascending.

`visitDateFrom`/`visitDateTo` are inclusive `OffsetDateTime` bounds (for example `2026-10-29T00:00:00+02:00`) and from must not be after to. Page must be non-negative and size 1–100. Success `200 OK` returns a Spring Data `Page<VisitResponse>`.

#### `GET /api/v1/inmates/{inmateId}/visits`

Purpose: fetch the visit history for one inmate.

Optional filters:

- `visitorName`
- `visitDateFrom`
- `visitDateTo`
- `page` (default `0`), `size` (default `20`, max `100`), `sort` (same sort aliases as visit search; default `startDateTime,desc`)

This route first resolves the inmate UUID; an unknown inmate returns `404`.

Representative `VisitResponse` shape:

```json
{
  "id": "8d1e0e92-9416-4cdc-847e-6be688a4e8d1",
  "visitor": {"fullName": "Amina Phiri"},
  "inmate": {"id": "7d0df6a2-0da6-4309-9c2d-7c50b1f939e3", "firstName": "John", "lastName": "Banda"},
  "visitDate": "2026-10-29",
  "startDateTime": "2026-10-29T09:30:00+02:00",
  "endDateTime": "2026-10-29T10:15:00+02:00",
  "durationMinutes": 45,
  "loggedBy": "4a3d7b2a-6c97-4b33-962a-0d1db78b86ce",
  "recordingOfficer": "Records Officer",
  "createdAt": "2026-10-29T07:30:00Z",
  "updatedAt": "2026-10-29T07:30:00Z"
}
```

### 5.8 Rehabilitation

#### `POST /api/v1/programs`

Purpose: create a rehabilitation program.

Request body:

```json
{
  "name": "Anger management",
  "description": "Weekly behaviour coaching and reflection sessions.",
  "instructor": "Program Instructor",
  "schedule": "Mondays 14:00-16:00"
}
```

`name` is required and nonblank (max 255 characters). `description` and `instructor` are optional (max 10000 and 255 characters); `schedule` is optional (max 255). Success `201 Created` returns `ProgramResponse`; newly created programs have status `active`.

#### `GET /api/v1/programs`

Query parameters:

- `name`
- `instructor`
- `status`
- `schedule`
- `page`, `size`, `sort` with allowed sort fields `id`, `name`, `instructor`, `status`, `schedule`

`status` filters accept `active`, `inactive`, or `completed` (case-insensitive). Names/instructors/schedules are optional filters. Pagination defaults to page `0`, size `20`; allowed sort properties are `id`, `name`, `instructor`, `status`, and `schedule`. Status cannot be changed through the program update DTO. Updating requires `name`; `instructor`, `description`, and `schedule` are optional.

#### `GET /api/v1/programs/{programId}`

Purpose: fetch one program by its public UUID, with details and enrollment counts in `ProgramDetailResponse`.

#### `PATCH /api/v1/programs/{programId}`

Purpose: update a program by public UUID. `name` is required; `instructor`, `description`, and `schedule` are optional. Success `200 OK` returns a `ProgramResponse` containing `id`, name, description, instructor, schedule, status, and enrolled inmate summaries.

#### `POST /api/v1/programs/enrollments`

Purpose: enroll an inmate into a program.

Request body:

```json
{
  "inmateId": "7d0df6a2-0da6-4309-9c2d-7c50b1f939e3",
  "programId": "c3d8c1ce-f333-4d5e-b765-9d20ef708835",
  "enrollmentDate": "2026-10-29"
}
```

Success `201 Created` returns `EnrollmentResponse`.
The response contains public UUIDs for enrollment `id`, `inmateId`, and `programId`, display names, `enrollmentDate`, and `status`.

#### `GET /api/v1/programs/enrollments`

Query parameters:

- `inmateId`
- `inmateName`
- `programId`
- `programName`
- `status`
- `page`, `size`

Default page/size are `0`/`20`; size must be 1–100. Results are sorted by `enrollmentDate` ascending; this endpoint does not accept a sort parameter.

#### `POST /api/v1/programs/attendance`

Purpose: record attendance for one inmate/program/session.

Request body:

```json
{
  "inmateId": "7d0df6a2-0da6-4309-9c2d-7c50b1f939e3",
  "programId": "c3d8c1ce-f333-4d5e-b765-9d20ef708835",
  "sessionDate": "2026-10-29",
  "attendanceStatus": "present",
  "remarks": "Attended and engaged well."
}
```

Rules:

- `inmateId`, `programId`, and `sessionDate` are required
- `attendanceStatus` is required, nonblank; service accepts `present`/`absent` (case-insensitive) and stores the corresponding value
- `remarks` is optional, max 10000 characters

Success `201 Created` returns an `AttendanceResponse`.

#### `POST /api/v1/programs/attendance/bulk`

Purpose: record attendance for multiple inmates in one request.

Request body:

```json
{
  "programId": "c3d8c1ce-f333-4d5e-b765-9d20ef708835",
  "sessionDate": "2026-10-29",
  "records": [
    {"inmateId": "7d0df6a2-0da6-4309-9c2d-7c50b1f939e3", "attendanceStatus": "present", "remarks": "Participated"},
    {"inmateId": "a46fd6f8-1db1-4e4d-9bcb-c1d7be4f2d7d", "attendanceStatus": "absent"}
  ]
}
```

`programId` and `sessionDate` are required. `records` must contain 1–500 non-null entries; each entry requires `inmateId` and nonblank `attendanceStatus`, with optional `remarks` up to 10000 characters. Success is `201 Created` with a list of attendance responses.

#### `GET /api/v1/programs/attendance`

Query parameters:

- `inmateId`
- `programName`
- `attendanceStatus`
- `sessionDateFrom`
- `sessionDateTo`
- `page`, `size`, `sort`

Allowed sort properties: `id` and `sessionDate`.

## 6. Workflow examples

### 6.1 Sign in and create a user (super admin)

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "admin",
    "password": "Admin123!"
  }'
```

Copy the returned `accessToken` into the next call:

In Bash, export the token value from the login response before the next request:

```bash
export TOKEN_ADMIN='<paste accessToken here>'
curl -X POST http://localhost:8080/api/v1/users \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -d '{
    "fullName": "Records Officer",
    "username": "records01",
    "email": "records.officer@example.test",
    "password": "Records123!",
    "role": "Records Officer"
  }'
```

### 6.2 Create a cell, enroll an inmate, and move the inmate

Use the created cell's returned public `id` as `CELL_ID`. Inmate enrollment allocates the inmate to that cell in the same request; use the returned inmate `id` as `INMATE_ID`.
```bash
export TOKEN_RECORDS='<records-officer access token>'
curl -X POST http://localhost:8080/api/v1/cells \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN_RECORDS" \
  -d '{
    "blockName": "A",
    "cellNumber": 1,
    "capacity": 2
  }'
```

```bash
curl -X POST http://localhost:8080/api/v1/inmates \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN_RECORDS" \
  --data-binary "{\"firstName\":\"John\",\"lastName\":\"Banda\",\"dateOfBirth\":\"1990-04-15\",\"gender\":\"Male\",\"admissionDate\":\"2025-01-10\",\"offence\":\"Theft\",\"sentenceStartDate\":\"2025-01-10\",\"sentenceEndDate\":\"2026-01-09\",\"cellId\":\"${CELL_ID}\"}"
```

For a later move, create or select a different cell with available space and use its UUID as `DESTINATION_CELL_ID`. Do not send the inmate’s current cell UUID:
```bash
curl -X PUT "http://localhost:8080/api/v1/inmates/${INMATE_ID}/cell-allocation" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN_RECORDS" \
  --data-binary "{\"cellId\":\"${DESTINATION_CELL_ID}\"}"
```

### 6.3 Log an incident and retrieve the incident history

```bash
curl -X POST http://localhost:8080/api/v1/incidents \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN_DISCIPLINE" \
  --data-binary "{\"participants\":[{\"inmateId\":\"${INMATE_ID}\",\"roleInIncident\":\"Primary participant\"}],\"incidentDate\":\"2026-10-29\",\"description\":\"Inmate quarrel in the recreation yard.\"}"
```

```bash
curl -X GET "http://localhost:8080/api/v1/inmates/$INMATE_ID/incidents" \
  -H "Authorization: Bearer $TOKEN_DISCIPLINE"
```

## 7. OpenAPI and Swagger status

The application already configures a bearer security scheme and describes the API with Springdoc/OpenAPI. The key configuration is in `src/main/java/com/pms/api/config/OpenApiConfig.java`:

- title: `Prison Management System API`
- version: `v1`
- security scheme: `bearerAuth`
- description: `Canonical documentation for the Prison Management System business API.`

`ApiVersioningAndSwaggerTest` checks that the generated document publishes canonical versioned paths, includes schemas/examples for user creation and login, and excludes several stale paths. This verifies selected paths and schemas, not every authorization rule, request constraint, response code, or error example. Controller-level OpenAPI annotations are incomplete on some operations; use this guide and the enforced controller/security rules as the authority for details not shown in Swagger. The live generated document is at `/v3/api-docs`; Swagger UI is at `/swagger-ui/index.html`.

## 8. Unverified or stale behavior to treat cautiously

The following items are not verified against the current implementation and should not be treated as active API behavior unless they appear in the controller source or generated OpenAPI output:

- `PUT /api/v1/users/{userId}/role`
- `POST /api/v1/crimes` and related crime endpoints
- `POST /api/v1/cases` and related case endpoints
- employee or case classification filters that are absent from the controllers
- any role or permission names not present in the `User.ALLOWED_ROLES` set or the `@PreAuthorize` checks

These appear in older documentation but are absent from the current controller inventory and therefore remain stale documentation rather than valid API contracts.
