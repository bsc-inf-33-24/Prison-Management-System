# Medical Officer API test guide

Create a user with the `Medical Officer` role and log in. The role can record a medical visit and search an inmate's medical history.

Record each visit separately with `POST /api/v1/medical-visits`:

```json
{
  "inmateId": "inmate-public-uuid",
  "visitDate": "2026-10-26",
  "sickness": "Persistent cough",
  "diagnosis": "Upper respiratory tract infection",
  "medicationsProvided": "Amoxicillin, 500 mg, three times daily",
  "recommendations": "Complete the prescribed course and return if symptoms worsen."
}
```

The authenticated Medical Officer is recorded from the login identity; the request cannot assign a different recorder. Retrieve a particular inmate's visit history by exact inmate number with `GET /api/v1/medical-visits?inmateNumber=...`. The response includes the inmate identity and visits ordered newest first, with date, sickness, diagnosis, medications, recommendations, and recording officer.

Both endpoints are restricted to the `Medical Officer` role. Other authenticated roles should receive `403`; requests without a valid token should receive `401`.
