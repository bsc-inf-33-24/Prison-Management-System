# Records Officer API test guide

Create `records01` with role `Records Officer`, log in, and use the returned `accessToken` as `TOKEN_RECORDS`.

## Inmates

- `GET /api/v1/inmates?search=John&classification=medium&gender=Male&facility=Test%20Facility&page=0&size=20&sort=lastName,asc`
- `GET /api/v1/inmates/{inmateUUID}`

## Cells and allocation

- `GET /api/v1/cells?blockName=A&classification=medium&page=0&size=20&sort=cellNumber,asc`
- `GET /api/v1/cells/{cellUUID}`

## Read access to other records

- Cases list/detail and inmate cases: `GET /api/v1/cases?...`, `GET /api/v1/cases/{caseUUID}`, `GET /api/v1/inmates/{inmateUUID}/cases?...`.
- Sentences: `POST/GET /api/v1/sentences`, `GET /api/v1/sentences/{sentenceUUID}`, `GET /api/v1/inmates/{inmateUUID}/sentences?...`, `PUT /api/v1/sentences/{sentenceUUID}`. Use the sentence body in the [full guide](../api-testing-user-guide.md#6-crimes-cases-and-sentences), with UUID IDs.
- Visitors/visits: list and detail `GET` endpoints, including `GET /api/v1/inmates/{inmateUUID}/visits?...`.
- Rehabilitation: list/detail programs, list enrollments/attendance, and history endpoints. Use the [full guide](../api-testing-user-guide.md#9-rehabilitation) for filters.

## Create incidents and submit transfers

- Log an incident with `POST /api/v1/incidents`:

```json
{"participants":[{"inmateId":"<INMATE_UUID>","roleInIncident":"Primary participant"}],"incidentDate":"2026-11-08","description":"Incident recorded by a Records Officer."}
```

- Submit a transfer with `POST /api/v1/transfers`:

```json
{"inmateId":"<INMATE_UUID>","destinationPrisonId":"<DESTINATION_PRISON_UUID>","reason":"Transfer request"}
```

Records Officers can also use the other transfer endpoints, subject to the prison access rules.

## Denied checks

Expect `403` when enrolling/updating inmates, allocating cells, creating/updating cases, managing crimes, recording incident outcomes, creating/updating visits, or writing rehabilitation records.
