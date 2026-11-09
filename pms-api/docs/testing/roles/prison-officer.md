# Prison Officer API test guide

Create a user with role `Prison Officer` and an active prison assignment, log in, and save the `accessToken` as `TOKEN_PRISON`.

## Inmate enrollment

Create an active prison and cell with a Super Admin account, then use:

- `POST /api/v1/inmates` to enroll an inmate in the assigned prison. The request must include that prison's UUID and a cell UUID in that prison.
- Confirm `403 Forbidden` when the request targets a different prison.
- Confirm `403 Forbidden` for `GET /api/v1/inmates`, `GET /api/v1/inmates/{inmateId}`, and `PATCH /api/v1/inmates/{inmateId}`.

## Transfer requests

- `POST /api/v1/transfers` can request a transfer for an inmate currently held at the officer's assigned prison. The destination must be a different active prison.
- Confirm `403 Forbidden` when requesting a transfer for an inmate outside the officer's assigned prison.
- Confirm `403 Forbidden` for transfer listing, approval, rejection, and cancellation endpoints.