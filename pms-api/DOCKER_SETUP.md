# Docker Compose Setup Guide

This project uses Docker Compose to run the backend and its PostgreSQL database locally.

## Requirements

Before starting, make sure the following are installed on your machine:

- Docker
- Docker Compose v2 (`docker compose`)
- Git

### Platform-specific prerequisite

- Windows users: install Docker Desktop before continuing.
- Linux users: install Docker Engine + Docker Compose CLI tooling, not just a GUI wrapper.

This avoids common setup issues such as missing Docker daemon access, Compose not recognized, or containers not starting properly.

## 1) Clone the repository

```bash
git clone <repo-url>
cd pms-api
```

## 2) Create the environment file

Create a file named `.env` in the project root. This file is required because the Compose file uses environment variables such as `${DB_PASSWORD}` and `${JWT_SECRET}`.

Example:

```env
DB_PASSWORD=your_secure_db_password
JWT_SECRET=your_super_secret_jwt_key
INITIAL_ADMIN_USERNAME=admin
INITIAL_ADMIN_PASSWORD=admin123
```

> Replace the sample values with strong values for your environment.
> Do not commit the `.env` file to Git if it contains secrets.

## 3) Verify the Compose file

The project includes a `docker-compose.yml` file that defines:

- PostgreSQL database container
- Spring Boot backend container
- port mappings for local access

The current configuration exposes:

- PostgreSQL: `localhost:5382`
- Backend API: `http://localhost:8080`

### How the containers work together

Docker Compose creates a private network for the services in the same Compose file. In this setup:

- the `db` service runs PostgreSQL
- the `app` service runs the backend application
- the backend connects to the database using the internal hostname `db`
- the app does not connect to `localhost` for the database inside Docker; it uses the Compose service name because containers on the same network can resolve each other by name

This is why the backend environment value is configured like this:

```env
DB_URL=jdbc:postgresql://db:5432/pms
```

The `db` hostname is only valid inside the Docker network. From your machine, the database is still accessed via `localhost:5382`, which is mapped from the container's port `5432`.

In practical terms:

- browser or external client -> `http://localhost:8080`
- backend container -> `db:5432`
- host machine -> `localhost:5382` to reach PostgreSQL

This architecture keeps the database isolated from the host while allowing the app to communicate reliably inside the Compose network.

## 4) Start the stack

From the project root, run:

```bash
docker compose up --build -d
```

This command will:

- build the backend image from the Dockerfile
- start PostgreSQL
- start the application
- wire the app to the database using the internal Compose network

## 5) Check the containers

```bash
docker compose ps
```

To view logs:

```bash
docker compose logs -f
```

## 6) Stop the stack

```bash
docker compose down
```

If you also want to remove the PostgreSQL data volume:

```bash
docker compose down -v
```

> Use `-v` only if you want to reset the database completely.

## Email password recovery

Recovery email is disabled unless explicitly enabled. To send reset tokens through SMTP, add the following to `.env`:

```env
RECOVERY_EMAIL_ENABLED=true
RECOVERY_EMAIL_FROM=security@your-verified-domain.com
SPRING_MAIL_HOST=smtp.resend.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=resend
SPRING_MAIL_PASSWORD=re_your_resend_api_key
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
```

Use the SMTP provider's app password or service credential, not a personal mailbox password. Recreate the app container after changing these settings. Without a configured sender, password recovery stays unavailable. Repeating `POST /api/v1/auth/password/forgot` sends a new token and invalidates any outstanding token for that account.

## Production image and CI/CD

The GitHub Actions workflow is stored at the repository root in `.github/workflows/pms-api-image.yml`. It checks migration versions and runs Maven verification for pull requests. On pushes to `main` and version tags such as `v1.2.0`, it builds the API image and publishes it to Docker Hub.

To enable publishing, create these repository secrets in GitHub under **Settings → Secrets and variables → Actions**:

- `DOCKERHUB_USERNAME`: the Docker Hub account or organization that owns the image repository.
- `DOCKERHUB_TOKEN`: a Docker Hub access token with permission to push to that repository.

Create `pms-api` in Docker Hub under that account or organization, and make it public if the frontend team should pull without logging in. The workflow publishes these tags (replace `<dockerhub-namespace>` with the account or organization):

```text
<dockerhub-namespace>/pms-api:main
<dockerhub-namespace>/pms-api:sha-<commit-sha>
<dockerhub-namespace>/pms-api:v1.2.0
```

`main` is a moving tag; use a version or commit-SHA tag for deployments. Push a version tag to publish a release, for example `git tag v1.2.0 && git push origin v1.2.0`.

The API image contains only the API. PostgreSQL runs as its own container so its data can persist in a Docker volume and the database can be upgraded independently. The Docker Hub handoff includes [docker-compose.hub.yml](docker-compose.hub.yml), which starts both the API image and the official PostgreSQL image together.

Give the frontend team `docker-compose.hub.yml` and an `.env` configured for their environment (do not send production secrets in source control). They can start the complete stack with:

```bash
docker compose -f docker-compose.hub.yml pull
docker compose -f docker-compose.hub.yml up -d
```

The `.env` file needs `DOCKERHUB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `INITIAL_ADMIN_PASSWORD`. `PMS_API_TAG` is optional and defaults to `main`; pin it to a release tag such as `v1.2.0` for reproducible environments. The database container and API image are pulled from Docker Hub when Compose starts. If the API image is private, users must first run `docker login` with Docker Hub credentials that have pull access. Never put production secrets in the image or frontend bundle.

The frontend team normally consumes the deployed API URL, not this image. Share the deployment's base URL (for example, `https://api.example.com/api/v1`), the OpenAPI document at `/v3/api-docs`, the JWT authentication instructions, and the frontend origin that must be allowed by the API's CORS configuration.

The workflow builds and publishes the image; deployment to a hosting platform is a separate step. Configure that platform to deploy a specific image tag or digest and inject production secrets at runtime.

## 7) Access the app

Once the containers are running, open:

```text
http://localhost:8080
```

If you need to connect to the database directly from a local client:

```text
Host: localhost
Port: 5382
Database: pms
User: pms_user
Password: <DB_PASSWORD>
```

## 8) Notes for the team

- Always pull the latest branch before starting the project.
- If Docker fails because env variables are missing, create or update the `.env` file.
- If a port is already in use, change the host port in `docker-compose.yml` and then restart the containers.
- If the container does not start correctly, check logs with:

```bash
docker compose logs app
```

## 9) Typical workflow

```bash
git pull
cp .env.example .env   # if an example env file exists in your repo
docker compose up --build -d
docker compose logs -f
```

## Troubleshooting

### Docker cannot find the `.env` values

Make sure `.env` exists in the same folder as `docker-compose.yml` and contains the required keys:

```env
DB_PASSWORD=
JWT_SECRET=
INITIAL_ADMIN_USERNAME=
INITIAL_ADMIN_PASSWORD=
```

### Database connection fails

Check whether Postgres is healthy:

```bash
docker compose ps
```

Then review the app logs:

```bash
docker compose logs app
```

### Port already in use

Edit the port mapping in `docker-compose.yml` and restart:

```bash
docker compose down
docker compose up -d --build
```

## Final reminder

After these changes are pushed to GitHub, every teammate can set up the project by cloning the repo, creating their own `.env`, and running the commands above.
