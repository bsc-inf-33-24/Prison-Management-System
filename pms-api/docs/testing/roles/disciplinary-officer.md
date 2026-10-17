# Disciplinary Officer API test guide

Create `discip01` with role `Disciplinary Officer`, log in, and use `accessToken` as `TOKEN_DISCIPLINE`. Create an inmate first using a Records Officer account.

## Record incident

`POST /api/v1/incidents`:

```json
{"participants":[{"inmateId":"<INMATE_UUID>","roleInIncident":"Subject"}],"incidentDate":"2026-01-20","description":"Test disciplinary incident"}
```

Save the returned incident UUID as `INCIDENT_UUID`.

## Record outcome and view history

- `PATCH /api/v1/incidents/{incidentUUID}/outcome`:

```json
{"outcome":"Written warning issued"}
```

- `GET /api/v1/inmates/{inmateUUID}/incidents`

Check a multiple-participant incident, nonexistent inmate, future date, blank outcome, and submitting an outcome twice.

## Denied checks

Expect `403` for incident write requests using Records, Case, Visitor, or Duty Officer tokens. Super Admin can read incident history but cannot log or resolve incidents.
