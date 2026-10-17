# Case Officer API test guide

Create `case01` with role `Case Officer`, log in, and use `accessToken` as `TOKEN_CASE`. Prepare an inmate (Records Officer) and crime (Super Admin) first.

## Crime catalog (read only)

- `GET /api/v1/crimes?category=Property&search=Theft&page=0&size=20&sort=offenceName,asc`
- `GET /api/v1/crimes/{crimeUUID}`

## Cases

`POST /api/v1/cases`:

```json
{"inmateId":"<INMATE_UUID>","caseNumber":"CASE-0001","court":"Blantyre High Court","caseStartDate":"2025-03-01","caseEndDate":"2027-03-01","crimeIds":["<CRIME_UUID>"]}
```

- `GET /api/v1/cases?status=ongoing&inmateId=<INMATE_UUID>&crimeId=<CRIME_UUID>&page=0&size=20`
- `GET /api/v1/cases/{caseUUID}`
- `PUT /api/v1/cases/{caseUUID}` — same body as create.
- `PATCH /api/v1/cases/{caseUUID}/status`:

```json
{"status":"closed","reason":"Case completed"}
```

- `GET /api/v1/inmates/{inmateUUID}/cases?status=ongoing&page=0&size=20`

## Sentences

`POST /api/v1/sentences` (and `PUT /api/v1/sentences/{sentenceUUID}`):

```json
{"caseId":"<CASE_UUID>","inmateId":"<INMATE_UUID>","duration":24,"startDate":"2025-03-01","releaseDate":"2027-03-01","status":"active"}
```

List/detail with `GET /api/v1/sentences?...`, `GET /api/v1/sentences/{sentenceUUID}`, and `GET /api/v1/inmates/{inmateUUID}/sentences?...`.

## Denied checks

Expect `403` for crime create/update and for creating or updating inmates/cells, incidents, visits, or rehabilitation records.
