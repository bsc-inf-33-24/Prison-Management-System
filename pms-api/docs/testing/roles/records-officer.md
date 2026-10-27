# Records Officer API test guide

Create `records01` with role `Records Officer`, log in, and use the returned `accessToken` as `TOKEN_RECORDS`.

## Inmates

- `POST /api/v1/inmates` and `PUT /api/v1/inmates/{inmateUUID}` use this body (use unique inmate numbers):

```json
{"inmateNumber":"INM-0001","firstName":"John","lastName":"Banda","dateOfBirth":"1990-04-15","gender":"Male","admissionDate":"2025-01-10","classification":"medium"}
```

- `GET /api/v1/inmates?search=John&classification=medium&gender=Male&facility=Test%20Facility&page=0&size=20&sort=lastName,asc`
- `GET /api/v1/inmates/{inmateUUID}`

## Cells and allocation

- `POST /api/v1/cells`:

```json
{"blockName":"A","cellNumber":1,"capacity":2,"classification":"medium"}
```

- `GET /api/v1/cells?blockName=A&classification=medium&page=0&size=20&sort=cellNumber,asc`
- `GET /api/v1/cells/{cellUUID}`
- `POST /api/v1/inmates/{inmateUUID}/cell-allocation` and `PUT` the same path to move:

```json
{"cellId":"<CELL_UUID>"}
```

Try a valid allocation, duplicate allocation, full cell, classification mismatch, and move to another compatible cell.

## Read access to other records

- Cases list/detail and inmate cases: `GET /api/v1/cases?...`, `GET /api/v1/cases/{caseUUID}`, `GET /api/v1/inmates/{inmateUUID}/cases?...`.
- Sentences: `POST/GET /api/v1/sentences`, `GET /api/v1/sentences/{sentenceUUID}`, `GET /api/v1/inmates/{inmateUUID}/sentences?...`, `PUT /api/v1/sentences/{sentenceUUID}`. Use the sentence body in the [full guide](../api-testing-user-guide.md#6-crimes-cases-and-sentences), with UUID IDs.
- Visitors/visits: list and detail `GET` endpoints, including `GET /api/v1/inmates/{inmateUUID}/visits?...`.
- Rehabilitation: list/detail programs, list enrollments/attendance, and history endpoints. Use the [full guide](../api-testing-user-guide.md#9-rehabilitation) for filters.

## Denied checks

Expect `403` when creating/updating cases, managing crimes, recording incidents, creating/updating visits, or writing rehabilitation records.
