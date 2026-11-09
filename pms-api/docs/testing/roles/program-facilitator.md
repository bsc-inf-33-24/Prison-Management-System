# Program Facilitator API test guide

Create a user with role `Program Facilitator` and an active prison assignment, log in, and save the `accessToken` as `TOKEN_PROGRAM`.

## Programs

- `POST /api/v1/programs` to create a program. The request accepts `name`, `description`, `instructor`, `schedule`, and optional `instructorUserId`.
- `GET /api/v1/programs` and `GET /api/v1/programs/{programId}` to list and view programs.
- `PATCH /api/v1/programs/{programId}` to update program details.

Program records are not assigned to a prison in the current API. Program creation, listing, viewing, and updates are not restricted to the facilitator's assigned prison.

## Enrollments and attendance

- `POST /api/v1/programs/enrollments` to enroll an inmate in an active program.
- `GET /api/v1/programs/enrollments` to search enrollment records.
- `POST /api/v1/programs/attendance` and `POST /api/v1/programs/attendance/bulk` to record attendance.
- `GET /api/v1/programs/attendance` to search attendance records.

Enrollment and attendance operations are limited to inmates at the facilitator's assigned prison. Confirm `403 Forbidden` when attempting those operations for an inmate at another prison.