# API Testing User Guide

This guide explains what to enter when manually testing the implemented APIs with Swagger UI, cURL, or Postman.

The API uses UUIDs for public resource IDs. Copy each ID returned by a create request into later paths and request bodies.

For a shorter role-specific checklist, use the individual guides:

- [Super Admin](roles/super-admin.md)
- [Records Officer](roles/records-officer.md)
- [Disciplinary Officer](roles/disciplinary-officer.md)
- [Case Officer](roles/case-officer.md)
- [Visitor Officer](roles/visitor-officer.md)
- [Rehabilitation Officer](roles/rehabilitation-officer.md)
- [Medical Officer](roles/medical-officer.md)
- [Duty Officer](roles/duty-officer.md)

## 1. Test setup

Use an isolated PostgreSQL test database. Configure an initial administrator:

```properties
INITIAL_ADMIN_USERNAME=admin
INITIAL_ADMIN_PASSWORD=Admin123!
JWT_SECRET=test-only-secret-that-is-long-enough-for-hmac-sha256
APP_FACILITY_NAME=Test Facility
```

Base URL:

```text
http://localhost:8080
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

Every protected request requires:

```http
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

Expected common responses: no token `401`, wrong role `403`, invalid input `400`, missing record `404`, duplicate/conflicting operation `409`.

## 2. Create users and tokens

### Login as the initial administrator

`POST /api/v1/auth/login`

```json
{
  "username": "admin",
  "password": "Admin123!"
}
```

Save the returned `accessToken` as `TOKEN_ADMIN`.

### Create role users

`POST /api/v1/users` using `TOKEN_ADMIN`:

```json
{
  "fullName": "Test Records Officer",
  "username": "records01",
  "email": "records01@example.test",
  "password": "Records123!",
  "role": "Records Officer"
}
```

`email` is optional. Remove the `email` property or set it to `null` when the user has no email address. Supplied addresses must be valid; the API trims surrounding spaces, stores the address in lowercase, and rejects duplicates with `409 Conflict`. Users can log in with either their username or email by supplying exactly one of those fields in the login request.

Repeat with these role values and unique usernames:

```text
discip01   Disciplinary Officer   Discip123!
case01     Case Officer           Case123!
visitor01  Visitor Officer        Visitor123!
rehab01    Rehabilitation Officer Rehab123!
ordinary1  Duty Officer            Duty123!
```

Login each account with `POST /api/v1/auth/login` and save `TOKEN_RECORDS`, `TOKEN_DISCIPLINE`, `TOKEN_CASE`, `TOKEN_VISITOR`, `TOKEN_REHAB`, and `TOKEN_ORDINARY`.

## 3. Authentication inputs

| Endpoint | Input |
|---|---|
| `POST /api/v1/auth/login` | Username login: `{ "username": "records01", "password": "Records123!" }`; email login: `{ "email": "records01@example.test", "password": "Records123!" }` |
| `POST /api/v1/auth/logout` | No body; valid bearer token |
| `POST /api/v1/auth/password/forgot` | `{ "username": "records01" }` |
| `POST /api/v1/auth/password/reset` | `{ "token": "recovery-token", "newPassword": "NewPass123!" }` |
| `POST /api/v1/auth/password/change` | `{ "currentPassword": "Records123!", "newPassword": "NewRecords123!" }` |

Test missing fields, invalid credentials, expired/reused reset tokens, weak passwords, and reuse of tokens after logout or password change.

## 4. User management inputs

All endpoints require `TOKEN_ADMIN`:

| Endpoint | Input |
|---|---|
| `POST /api/v1/users` | `fullName`, `username`, `password`, `role` |
| `GET /api/v1/users` | Optional `status`, `role`, `search`, `page`, `size`, `sort` |
| `GET /api/v1/users/{userId}` | `userId` returned from user creation |
| `PATCH /api/v1/users/{userId}/deactivate` | Path ID only |
| `PATCH /api/v1/users/{userId}/reactivate` | Path ID only |
| `PUT /api/v1/users/{userId}/role` | `{ "role": "Records Officer", "permissions": ["inmates:read"] }` |

List example:

```text
GET /api/v1/users?status=active&role=Records%20Officer&search=records&page=0&size=20&sort=createdAt,desc
```

## 5. Inmates and cells

### Create inmates

`POST /api/v1/inmates` using `TOKEN_RECORDS` or `TOKEN_ADMIN`:

```json
{
  "firstName": "John",
  "lastName": "Banda",
  "dateOfBirth": "1990-04-15",
  "gender": "Male",
  "admissionDate": "2025-01-10",
  "sentenceStartDate": "2025-01-10",
  "sentenceEndDate": "2026-01-09",
  "cellId": "a2bd2d14-15ba-44fc-92c1-3d6553d8e8f4"
}
```

Create a second inmate with a different name. Save `INMATE_ID` and `INMATE_ID_2`.

Other inmate inputs:

```text
GET  /api/v1/inmates?search=John&classification=medium&gender=Male&facility=Test%20Facility&page=0&size=20&sort=lastName,asc
GET   /api/v1/inmates/{inmateUuid}
PATCH /api/v1/inmates/{inmateUuid}
```

The update body contains the editable profile fields. Test future birth date, missing fields, invalid IDs, and wrong roles.

Names are not unique: create multiple inmates with the same or similar names. Successful responses expose a UUID in `id`; the numeric database key is never returned.

### Create cells

`POST /api/v1/cells` using `TOKEN_RECORDS`:

```json
{
  "blockName": "A",
  "cellNumber": 1,
  "capacity": 1,
  "classification": "medium"
}
```

Create another cell with block `B`, cell number `2`, capacity `2`, and classification `low`. Save `CELL_ID` and `CELL_ID_2`.

Allocation and movement:

```text
POST /api/v1/inmates/{inmateId}/cell-allocation
PUT  /api/v1/inmates/{inmateId}/cell-allocation
```

Body:

```json
{ "cellId": 1 }
```

Replace `1` with `CELL_ID`. Test full capacity, classification mismatch, duplicate allocation, same-cell movement, and valid movement to `CELL_ID_2`.

Cell reads:

```text
GET /api/v1/cells?blockName=A&classification=medium&page=0&size=20&sort=cellNumber,asc
GET /api/v1/cells/{cellId}
```

## 6. Crimes, cases, and sentences

### Crime

Create with `TOKEN_ADMIN`:

`POST /api/v1/crimes`

```json
{
  "offenceName": "Theft",
  "description": "Test catalog offence",
  "category": "Property"
}
```

Save `CRIME_ID`. List/get with `TOKEN_ADMIN` or `TOKEN_CASE`:

```text
GET /api/v1/crimes?category=Property&search=Theft&page=0&size=20&sort=offenceName,asc
GET /api/v1/crimes/{crimeId}
PUT /api/v1/crimes/{crimeId}
```

The update body is the same as creation.

### Case

Create with `TOKEN_CASE` or `TOKEN_ADMIN`:

`POST /api/v1/cases`

```json
{
  "inmateId": 1,
  "caseNumber": "CASE-0001",
  "court": "Blantyre High Court",
  "caseStartDate": "2025-03-01",
  "caseEndDate": "2027-03-01",
  "crimeIds": [1]
}
```

Replace `1` with `INMATE_ID` and `CRIME_ID`. Save `CASE_ID`.

```text
GET  /api/v1/cases?status=ongoing&inmateId=1&crimeId=1&caseStartDateFrom=2025-01-01&caseStartDateTo=2027-12-31&page=0&size=20&sort=caseStartDate,desc
GET  /api/v1/cases/{caseId}
PUT  /api/v1/cases/{caseId}
GET  /api/v1/inmates/{inmateId}/cases?status=ongoing&page=0&size=20&sort=caseStartDate,desc
```

Update status with `PATCH /api/v1/cases/{caseId}/status`:

```json
{ "status": "closed", "reason": "Case completed" }
```

Valid statuses are `ongoing`, `closed`, and `appealed`. Test invalid transitions and missing reasons.

### Sentence

Create with `TOKEN_RECORDS`, `TOKEN_CASE`, or `TOKEN_ADMIN`:

`POST /api/v1/sentences`

```json
{
  "caseId": 1,
  "inmateId": 1,
  "duration": 24,
  "startDate": "2025-03-01",
  "releaseDate": "2027-03-01",
  "status": "active"
}
```

The case and inmate must match and the case must be ongoing. Save `SENTENCE_ID`.

```text
GET /api/v1/sentences?inmateId=1&caseId=1&status=active&startDateFrom=2025-01-01&startDateTo=2027-12-31&page=0&size=20&sort=startDate,asc
GET /api/v1/sentences/{sentenceId}
GET /api/v1/inmates/{inmateId}/sentences?status=active&page=0&size=20&sort=startDate,asc
PUT /api/v1/sentences/{sentenceId}
```

The update body is the same as creation. Test duration `0`, invalid status, release date before start date, and mismatched records.

## 7. Incidents and discipline

Create and resolve using `TOKEN_DISCIPLINE`; history requires `TOKEN_DISCIPLINE` or `TOKEN_ADMIN`.

`POST /api/v1/incidents`

```json
{
  "participants": [
    { "inmateId": 1, "roleInIncident": "Subject" }
  ],
  "incidentDate": "2026-01-20",
  "description": "Test disciplinary incident"
}
```

Replace `1` with `INMATE_ID` and save `INCIDENT_ID`.

```text
PATCH /api/v1/incidents/{incidentId}/outcome
GET   /api/v1/inmates/{inmateId}/incidents
```

Outcome body:

```json
{ "outcome": "Written warning issued" }
```

Test multiple participants, duplicate participants, nonexistent inmates, future dates, blank outcomes, and resolving the same incident twice.

## 8. Visitors and visits

Create/update with `TOKEN_VISITOR`; reads also allow `TOKEN_RECORDS`.

### Visitor

`POST /api/v1/visitors`

```json
{
  "fullName": "Test Visitor",
  "idNumber": "NID-TEST-001",
  "phone": "+265 991234567",
  "relationshipToInmate": "Sibling"
}
```

Save `VISITOR_ID`. Then test:

```text
GET /api/v1/visitors/{visitorId}
GET /api/v1/visitors?fullName=Test&idNumber=NID-TEST-001&phone=991&relationshipToInmate=Sibling&page=0&size=20&sort=fullName,asc
```

### Visit

`POST /api/v1/visits`

```json
{
  "visitorId": 1,
  "inmateId": 1,
  "visitDateTime": "2026-01-15T10:00:00+02:00",
  "durationMinutes": 60
}
```

Replace IDs with `VISITOR_ID` and `INMATE_ID`; save `VISIT_ID`.

```text
GET /api/v1/visits/{visitId}
GET /api/v1/visits?visitorId=1&visitorName=Test&visitorIdNumber=NID-TEST-001&inmateId=1&inmateName=John&visitDateFrom=2026-01-01T00:00:00%2B02:00&visitDateTo=2026-12-31T23:59:59%2B02:00&loggedBy=1&page=0&size=20&sort=visitDateTime,desc
GET /api/v1/inmates/{inmateId}/visits?visitorName=Test&visitorIdNumber=NID-TEST-001&page=0&size=20&sort=visitDateTime,desc
PUT /api/v1/visits/{visitId}
```

Update body:

```json
{
  "visitDateTime": "2026-01-16T11:00:00+02:00",
  "durationMinutes": 90
}
```

Valid duration is `0` through `1440`. Test `-1` and `1441`.

## 9. Rehabilitation

Create/update with `TOKEN_REHAB`; list/history also allow `TOKEN_RECORDS`.

### Program

`POST /api/v1/programs`

```json
{
  "name": "Literacy Training",
  "description": "Basic literacy and numeracy",
  "instructor": "Test Instructor",
  "schedule": "Monday and Wednesday 09:00"
}
```

Save `PROGRAM_ID`. Test:

```text
GET /api/v1/programs?name=Literacy&instructor=Test&status=active&schedule=Monday&page=0&size=20&sort=name,asc
GET /api/v1/programs/{programId}
PUT /api/v1/programs/{programId}
PATCH /api/v1/programs/{programId}/status
```

Update details body:

```json
{
  "name": "Advanced Literacy",
  "description": "Updated description",
  "instructor": "New Instructor",
  "schedule": "Friday 09:00"
}
```

Status body:

```json
{ "status": "inactive", "reason": "Instructor unavailable" }
```

Valid statuses are `active`, `inactive`, and `completed`; inactive/completed require a reason.

### Enrollment

`POST /api/v1/programs/enrollments`

```json
{
  "inmateId": 1,
  "programId": 1,
  "enrollmentDate": "2026-01-05"
}
```

Replace IDs with `INMATE_ID` and `PROGRAM_ID`. Save `ENROLLMENT_ID`.

```text
GET /api/v1/programs/enrollments?inmateId=1&inmateName=John&programId=1&programName=Literacy&status=enrolled&enrollmentDateFrom=2026-01-01&enrollmentDateTo=2026-12-31&page=0&size=20&sort=enrollmentDate,asc
PATCH /api/v1/programs/enrollments/{enrollmentId}/status
```

Status update body:

```json
{ "status": "completed", "reason": "Program completed successfully" }
```

Test duplicate enrollment, inactive programs, invalid transitions, and missing reasons.

### Attendance

`POST /api/v1/programs/attendance`

```json
{
  "enrollmentId": 1,
  "sessionDate": "2026-01-07",
  "attendanceStatus": "present",
  "remarks": "Attended on time"
}
```

Replace `1` with `ENROLLMENT_ID`; save `ATTENDANCE_ID`.

```text
GET /api/v1/programs/attendance?enrollmentId=1&inmateId=1&programId=1&attendanceStatus=present&sessionDateFrom=2026-01-01&sessionDateTo=2026-12-31&page=0&size=20&sort=sessionDate,desc
PUT /api/v1/programs/attendance/{attendanceId}
```

Update body:

```json
{ "attendanceStatus": "absent", "remarks": "Absent with permission" }
```

Valid statuses are `present` and `absent`. The current database does not support `excused`.

### History

```text
GET /api/v1/inmates/{inmateId}/rehabilitation-history?page=0&size=20&sort=enrollmentDate,desc
GET /api/v1/programs/enrollments/{enrollmentId}/attendance?page=0&size=20&sort=sessionDate,desc
```

## 10. Tests to repeat for every endpoint

- Remove the token, use a malformed token, and use a valid token with the wrong role.
- Remove each required field; test `null`, blank, wrong type, invalid enum, and oversized values.
- Test `0`, negative, malformed, and nonexistent IDs.
- Test page `-1`, size `0`, size `101`, invalid sort, and reversed date ranges.
- Send malformed JSON and an unsupported content type.
- Repeat duplicate create and status operations.
- Verify failed requests do not change database state.
- Verify responses do not expose passwords, tokens, stack traces, SQL, or secrets.

## 11. Recommended execution order

1. Create users and obtain role tokens.
2. Create inmates.
3. Create cells and test allocation/movement.
4. Create crimes, cases, and sentences.
5. Create visitors and visits.
6. Create and resolve incidents.
7. Create rehabilitation programs, enrollments, and attendance.
8. Test all list, detail, filter, pagination, and history endpoints.
9. Repeat authorization, validation, conflict, and failed-write tests.

Record every result as `PASS`, `FAIL`, or `BLOCKED` with the request, response, role, expected status, actual status, and database side effect.
