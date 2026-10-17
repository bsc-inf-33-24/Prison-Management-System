# Visitor Officer API test guide

Create `visitor01` with role `Visitor Officer`, log in, and use `accessToken` as `TOKEN_VISITOR`. Create an inmate first with a Records Officer account.

## Visitors

`POST /api/v1/visitors`:

```json
{"fullName":"Test Visitor","idNumber":"NID-TEST-001","phone":"+265 991234567","relationshipToInmate":"Sibling"}
```

Save the returned UUID as `VISITOR_UUID`. Read with `GET /api/v1/visitors/{visitorUUID}` or `GET /api/v1/visitors?fullName=Test&idNumber=NID-TEST-001&phone=991&relationshipToInmate=Sibling&page=0&size=20`.

## Visits

`POST /api/v1/visits`:

```json
{"visitorId":"<VISITOR_UUID>","inmateId":"<INMATE_UUID>","visitDateTime":"2026-01-15T10:00:00+02:00","durationMinutes":60}
```

Save returned visit UUID. Read using `GET /api/v1/visits/{visitUUID}`, `GET /api/v1/visits?visitorId=<VISITOR_UUID>&inmateId=<INMATE_UUID>&page=0&size=20`, and `GET /api/v1/inmates/{inmateUUID}/visits?page=0&size=20`.

`PUT /api/v1/visits/{visitUUID}`:

```json
{"visitDateTime":"2026-01-16T11:00:00+02:00","durationMinutes":90}
```

Test duration `-1` and `1441` as invalid values.

## Denied checks

Expect `403` for visitor/visit creation or visit updates with Records, Case, or Duty Officer tokens.
