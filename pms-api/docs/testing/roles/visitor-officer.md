# Visitor Officer API test guide

Create `visitor01` with role `Visitor Officer`, log in, and use `accessToken` as `TOKEN_VISITOR`. Create an inmate first with a Records Officer account.

## Visitors

`POST /api/v1/visitors`:

```json
{"fullName":"Test Visitor","idNumber":"NID-TEST-001","phone":"+265 991234567","relationshipToInmate":"Sibling"}
```

Save the returned UUID as `VISITOR_UUID`. Read with `GET /api/v1/visitors/{visitorUUID}` or `GET /api/v1/visitors?fullName=Test&idNumber=NID-TEST-001&phone=991&relationshipToInmate=Sibling&page=0&size=20`.

## Visits

Record a family or friend visit with `POST /api/v1/visits/family-friends`:

```json
{"visitorFullName":"Test Visitor","inmateId":"<INMATE_UUID>","visitDate":"2026-01-15","startTime":"10:00","endTime":"11:00"}
```

Search using `GET /api/v1/visits?inmateId=<INMATE_UUID>&visitType=FAMILY&page=0&size=20`, review all history with `GET /api/v1/inmates/{inmateUUID}/visits?page=0&size=20`, or retrieve only family/friend visits with `GET /api/v1/inmates/{inmateUUID}/visits/family-friends?page=0&size=20`. Legal visits are recorded with `POST /api/v1/visits/legal` by Case Officers and Records Officers and retrieved from the legal-specific inmate history route by authorized staff.

## Denied checks

Expect `403` when recording a family visit with a Case Officer or Duty Officer token. Records Officers can record and view both family and legal visits; they cannot record health visits.
