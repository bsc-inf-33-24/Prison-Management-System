# Visitor Officer API test guide

Create `visitor01` with role `Visitor Officer`, log in, and use `accessToken` as `TOKEN_VISITOR`. Create an inmate first with a Records Officer account.

## Visitors

`POST /api/v1/visitors`:

```json
{"fullName":"Test Visitor","idNumber":"NID-TEST-001","phone":"+265 991234567","relationshipToInmate":"Sibling"}
```

Save the returned UUID as `VISITOR_UUID`. Read with `GET /api/v1/visitors/{visitorUUID}` or `GET /api/v1/visitors?fullName=Test&idNumber=NID-TEST-001&phone=991&relationshipToInmate=Sibling&page=0&size=20`.

## Visits

Record a family visit with `POST /api/v1/visits`:

```json
{"visitType":"FAMILY","visitorFullName":"Test Visitor","inmateId":"<INMATE_UUID>","visitDate":"2026-01-15","startTime":"10:00","endTime":"11:00"}
```

Search using `GET /api/v1/visits?inmateId=<INMATE_UUID>&visitType=FAMILY&page=0&size=20` or review history with `GET /api/v1/inmates/{inmateUUID}/visits?page=0&size=20`. Legal visits are recorded in the same module by Case Officers and Records Officers.

## Denied checks

Expect `403` when recording a family visit with a Case Officer or Duty Officer token. Records Officers can record and view both family and legal visits; they cannot record health visits.
