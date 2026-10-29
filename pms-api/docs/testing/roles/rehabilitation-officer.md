# Rehabilitation Officer API test guide

Create `rehab01` with role `Rehabilitation Officer`, log in, and use `accessToken` as `TOKEN_REHAB`. Create an inmate with a Records Officer account first.

## Programs

`POST /api/v1/programs`:

```json
{"name":"Literacy Training","description":"Basic literacy and numeracy","instructor":"Test Instructor","schedule":"Monday and Wednesday 09:00"}
```

Save `PROGRAM_UUID`. List `GET /api/v1/programs?name=Literacy&instructor=Test&status=active&page=0&size=20`; detail `GET /api/v1/programs/{programUUID}`.

- `PUT /api/v1/programs/{programUUID}` with updated program fields:

```json
{"name":"Advanced Literacy","description":"Updated description","instructor":"New Instructor","schedule":"Friday 09:00"}
```

- `PATCH /api/v1/programs/{programUUID}/status`:

```json
{"status":"inactive","reason":"Instructor unavailable"}
```

## Enrollment and attendance

- `POST /api/v1/programs/enrollments`:

```json
{"inmateId":"<INMATE_UUID>","programId":"<PROGRAM_UUID>","enrollmentDate":"2026-01-05"}
```

- `GET /api/v1/programs/enrollments?inmateId=<INMATE_UUID>&programId=<PROGRAM_UUID>&status=enrolled&page=0&size=20`
- `PATCH /api/v1/programs/enrollments/{enrollmentUUID}/status`:

```json
{"status":"completed","reason":"Program completed successfully"}
```

- `POST /api/v1/programs/attendance`:

```json
{"enrollmentId":"<ENROLLMENT_UUID>","sessionDate":"2026-01-07","attendanceStatus":"present","remarks":"Attended on time"}
```

- `GET /api/v1/programs/attendance?enrollmentId=<ENROLLMENT_UUID>&attendanceStatus=present&page=0&size=20`
- `PUT /api/v1/programs/attendance/{attendanceUUID}`:

```json
{"attendanceStatus":"absent","remarks":"Absent with permission"}
```

History reads: `GET /api/v1/inmates/{inmateUUID}/rehabilitation-history?page=0&size=20` and `GET /api/v1/programs/enrollments/{enrollmentUUID}/attendance?page=0&size=20`.

Test duplicate enrollment, inactive program, invalid status transition, missing reason, and attendance values other than `present` or `absent`.
