# Backend deployment and database migration

## Deploying V9 (23 November 2026)

Keep the existing PostgreSQL service running. Do not recreate it, delete its data,
or deploy a second database for this update.

The Spring Boot backend includes Flyway and migration
`src/main/resources/db/migration/V9__guest_details.sql`. During backend startup,
Flyway applies pending migrations to the database configured by DATABASE_URL,
DATABASE_USER and DATABASE_PASSWORD. Existing applied migrations are not rerun.
V9 adds nullable guest-detail columns and an index; it does not remove registry data.

Deployment sequence:

1. Confirm the existing database is available and its normal backup is available.
2. In Render, select the backend service (little-list-1), then Manual Deploy >
   Deploy latest commit. A Git push only triggers this automatically if auto-deploy
   and build filters are configured to include the changed backend files.
3. Check backend logs for successful Flyway migration to v9 (or validation showing
   v9 already applied), followed by successful application startup.
4. Open https://little-list-1.onrender.com/actuator/health and confirm status UP.
5. Deploy or retry the frontend only after the backend is healthy. Its BACKEND_URL
   should point to the deployed backend, not the Docker Compose hostname backend.

A backend-only commit does not guarantee deployment order if both Render services
watch every repository change. For a controlled rollout, use manual deployments
and deploy the backend first. No Render dashboard settings are changed by this file.

## If startup fails

Read the first error and its Caused by section in backend logs. Check database
connectivity, credentials and migration errors before changing infrastructure.
Do not edit an already-applied Flyway migration, drop tables, or run Flyway repair
without diagnosing the failure. V9 is additive, but a successful migration alone
does not prove email, authentication or product imports are working.

Render deployment reference: https://render.com/docs/deploys
