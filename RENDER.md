# Render deployment repair

The screenshot shows Render looking for ./Dockerfile at the repository root. The old commit only had frontend/Dockerfile and backend/Dockerfile. The new root Dockerfile builds the frontend; .dockerignore excludes local environment files and generated files.

## Existing little-list web service settings

- Root Directory: empty (repository root)
- Dockerfile Path: ./Dockerfile
- Docker Build Context Directory: .
- Docker Command: empty (image default)
- PORT: 3000
- BACKEND_URL: http://YOUR_BACKEND_INTERNAL_HOST:8080

Copy the real private hostname from the backend service. BACKEND_URL is a build argument; rebuild after changing it. Deploy the latest main commit, not the older 2ac2de9. With After CI Checks Pass, wait for verification to finish.

## Required backend

A frontend-only service cannot provide registry APIs or login. Create/configure a separate private Docker service in the same region: Root Directory empty, Dockerfile Path ./backend/Dockerfile, Docker Build Context Directory ./backend. Set SERVER_ADDRESS=0.0.0.0, SERVER_PORT=8080 and SPRING_PROFILES_ACTIVE=google (never local).

Provide managed PostgreSQL credentials. DATABASE_URL must be a JDBC URL: jdbc:postgresql://HOST:5432/DATABASE with provider-required TLS options. Set DATABASE_USER and DATABASE_PASSWORD separately; a raw postgresql:// connection string is not a JDBC URL.

Set WEB_URL to the public frontend HTTPS origin and register WEB_URL/login/oauth2/code/google in Google OAuth. Supply Google and real SMTP credentials described in DELIVERY.md. Mailpit and demo accounts are local only.

render.yaml declares both Docker paths and contexts, with environment values entered in Render. It does not create a database or supply credentials. Importing it may create paid resources: review the resource summary before applying. Committing a Blueprint does not automatically reconfigure a manually created service.

## Test data

The local Playwright registry eb5286ac-1b36-4412-9276-7d045258a0fb was removed after checking its exact title. Its shared API now returns404. This was synthetic database data, not a hardcoded app heading or production seed. No other registry was modified. Future browser tests should use an isolated test account/database and clean up test records.

## Verification boundary

This repairs the repository-side missing Dockerfile error. Render settings, production credentials and a successful live deployment have not been verified from the screenshot. See DELIVERY.md for remaining production requirements.
