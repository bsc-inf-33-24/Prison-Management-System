# Disciplinary Officer API test guide

Create `discip01` with role `Disciplinary Officer`, log in, and use `accessToken` as `TOKEN_DISCIPLINE`. Create an inmate first using a Records Officer account.

## Record incident

`POST /api/v1/incidents`:

```json
{"participants":[{"inmateId":"<INMATE_UUID>","roleInIncident":"Subject"}],"incidentDate":"2026-01-20","description":"Test disciplinary incident","outcome":"Written warning issued"}
```

The optional outcome must be nonblank and at most 255 characters. Supplying it creates a resolved incident; omitting it creates an open incident with a null outcome.

- `GET /api/v1/inmates/{inmateUUID}/incidents`

Check a multiple-participant incident, nonexistent inmate, future date, empty or whitespace-only outcome, and an outcome longer than 255 characters.

## Denied checks

Records Officers can log incidents but receive `403` for incident history. Expect `403` for incident write requests using Case, Visitor, or Duty Officer tokens. Super Admin can read incident history but cannot log incidents.
