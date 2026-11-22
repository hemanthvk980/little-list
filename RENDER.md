## Render Free email update — 22 November 2026

The backend now supports `MAIL_PROVIDER=resend` through HTTPS, or `MAIL_PROVIDER=disabled` as a temporary mode that rejects email operations. See [EMAIL.md](EMAIL.md) for exact setup. Existing SMTP instructions below apply only to SMTP-capable hosting; Render Free blocks SMTP. Configure the provider on the backend and redeploy; the frontend settings are unchanged.

# Little List: complete Render deployment guide

Checked against Render documentation on 18 November 2026.
Repository: https://github.com/hemanthvk980/little-list

## What you will deploy

```text
Browser / WhatsApp / QR
          |
          v HTTPS
little-list (public Next.js Docker web service)
          |
          v private HTTP, port 8080
little-list-backend (Spring Boot Docker private service)
          |                         |
          v                         v
Render PostgreSQL              SMTP email provider
```

Google sign-in returns to the public frontend. Next proxies `/api`, `/r`, `/oauth2` and `/login/oauth2` to Spring. There is no separate public backend URL to distribute. Docker Compose, Mailpit, Jenkins and Helm are not needed for this Render topology. Render does not automatically run compose.yaml.

Use the same Render workspace and region for all three resources. Singapore is a reasonable starting point for an India pilot, subject to your latency and residency requirements. This guide does not establish Indian data residency. Render's [private-network documentation](https://render.com/docs/private-network) explains the same-region requirement and how to find internal addresses.

For real traffic choose paid, always-on services and a database backup policy. Private services do not offer the Free instance option; Render explicitly discourages Free instances for production. Review the dashboard cost estimate before provisioning. [Free instance limitations](https://render.com/docs/free)

## 1. Prepare the accounts and URLs

- Connect Render to GitHub and grant access to `hemanthvk980/little-list`. Select branch `main`.
- Keep the existing `little-list` web service from your failed attempt; you can repair it without deleting it.
- Record its assigned HTTPS URL from Render, for example `https://YOUR-ASSIGNED-NAME.onrender.com`. Do not assume it is exactly little-list.onrender.com.
- Prepare a Google OAuth web application and an SMTP provider with a verified sender. Enter secrets directly into the provider dashboards, never into Git or chat.
- Begin with one environment, ideally staging. Later create separate services and databases for production.

The previously failing commit was 2ac2de9. Commit 56fc5eb added the missing root Dockerfile and passed [GitHub verification](https://github.com/hemanthvk980/little-list/actions/runs/43832235408). Deploy the latest main commit containing that fix.

## 2. Create PostgreSQL

In Render choose **New > Postgres**. Name it `little-list-db`, choose the same region as your apps, and use PostgreSQL17 to match our local verification. Select the database plan, storage and backup options appropriate to your pilot.

When the database is Available, use its connection details to record the internal host, port, database name, username and password. These belong only in the backend service. Render documents the creation flow and internal connections in [Create and Connect to Render Postgres](https://render.com/docs/postgresql-creating-connecting).

Construct the Spring JDBC URL from the individual fields:

```text
DATABASE_URL=jdbc:postgresql://INTERNAL_DATABASE_HOST:5432/DATABASE_NAME
DATABASE_USER=DATABASE_USERNAME
DATABASE_PASSWORD=DATABASE_PASSWORD
```

Use the port actually shown by Render if different. Apply TLS options required for your chosen connection route/provider; do not blindly copy an external-connection TLS setting into an internal connection. A `postgresql://user:password@host/database` URL is not directly the JDBC URL expected by this application. Keep credentials in their separate variables.

Flyway creates/migrates application tables when Spring starts. Do not import the local demo database into production. Registry photos are currently stored in PostgreSQL, so database backups include them; no application disk is required for photo persistence.

## 3. Create the Spring Boot backend

Choose **New > Private Service**, connect the GitHub repository and select Docker as the language/runtime. Set the following fields exactly; paths below assume Root Directory is empty.

| Field | Value |
| --- | --- |
| Name | little-list-backend |
| Branch | main |
| Root Directory | Leave empty |
| Dockerfile Path | ./backend/Dockerfile |
| Docker Build Context Directory | ./backend |
| Docker Command | Leave empty; use the image command |
| Region | Same as database/frontend |
| Instances | 1 |

Choose sufficient memory for Java; start with roughly1GB for a pilot and observe actual usage. This is sizing guidance, not a guaranteed capacity estimate.

Open the backend **Environment** page and enter:

| Variable | Value |
| --- | --- |
| SPRING_PROFILES_ACTIVE | google |
| SERVER_ADDRESS | 0.0.0.0 |
| SERVER_PORT | 8080 |
| WEB_URL | Exact frontend HTTPS origin, no trailing slash |
| DATABASE_URL | JDBC URL from step2 |
| DATABASE_USER | Database username |
| DATABASE_PASSWORD | Database password |
| GOOGLE_CLIENT_ID | Google OAuth client ID |
| GOOGLE_CLIENT_SECRET | Google OAuth client secret |
| MAIL_HOST | Your SMTP provider hostname |
| MAIL_PORT | 587 for the STARTTLS setup below |
| MAIL_USERNAME | SMTP username supplied by provider |
| MAIL_PASSWORD | SMTP password/token supplied by provider |
| MAIL_FROM | Verified sender, e.g. gifts@your-domain.in |
| MAIL_AUTH | true |
| MAIL_STARTTLS | true |

This configuration expects a provider supporting authenticated STARTTLS on587. Do not change to implicit-TLS port465 without configuring the corresponding Spring Mail SSL properties. Never enable `local`, use demo credentials, or point production mail to Mailpit.

Deploy. Check logs for successful database connection, Flyway migrations and Spring startup on8080. If credentials are incomplete, resolve that before proceeding. From an authenticated Render shell on a service in the same private network, `/actuator/health` should return HTTP200 with status UP; the private backend is intentionally not browser-accessible from your laptop.

Copy the backend's **Connect > Internal** address or Service Address. If it shows `little-list-backend-XXXX:8080`, the frontend setting will be `http://little-list-backend-XXXX:8080`. Use the actual value, not the display name. [Private service address details](https://render.com/docs/private-network)

## 4. Repair/deploy the Next.js frontend

Open your existing `little-list` web service, then **Settings > Build & Deploy**. Configure:

| Field | Value |
| --- | --- |
| Runtime | Docker |
| Branch | main |
| Root Directory | Leave empty |
| Dockerfile Path | ./Dockerfile |
| Docker Build Context Directory | . |
| Docker Command | Leave empty |
| Health Check Path | / |

Set these variables on the FRONTEND service:

```text
PORT=3000
BACKEND_URL=http://ACTUAL_BACKEND_INTERNAL_HOST:8080
```

Do not put database, SMTP or Google client secrets on the frontend. Select **Save, rebuild, and deploy** when setting/changing BACKEND_URL. Our Next rewrites are compiled during `next build`, so a runtime restart alone retains the old backend destination. Render exposes environment variables as Docker build arguments when the Dockerfile declares them. [Docker builds](https://render.com/docs/docker), [Environment save options](https://render.com/docs/configure-environment-variables)

Use **Manual Deploy > Deploy latest commit** if needed. The original `open Dockerfile: no such file or directory` error should now be resolved. The alternate supported layout is Dockerfile `./frontend/Dockerfile` with context `./frontend`, also with Root Directory empty. Do not mix one layout's context with the other layout's Dockerfile. Render resolves build settings relative to Root Directory. [Monorepo path rules](https://render.com/docs/monorepo-support)

## 5. Complete Google sign-in and email verification

For the Google OAuth web client, register this exact authorized redirect URI:

```text
https://YOUR-FRONTEND-HOST/login/oauth2/code/google
```

Use the same origin as backend WEB_URL. Configure consent-screen access/test users as appropriate in your Google project. Do not register the private backend hostname. Visit the live frontend and verify that Google sign-in returns to the dashboard. Local demo sign-in must not be offered on the public app.

With an email address you control, test a guest reservation/purchase confirmation. Verify that the link uses the public HTTPS origin, lands on the confirmation page, and changes inventory only after explicit confirmation. Check the provider's delivery logs if mail does not arrive. Our current mail integration has timeouts but no durable outbox/retry queue; implement that before broad launch.

## 6. Domain and QR sharing

You can initially use the assigned onrender.com URL. For a custom domain, open frontend **Settings > Custom Domains**, add your domain, follow the exact DNS records Render provides and verify it. Render manages TLS certificates. [Custom domain setup](https://render.com/docs/custom-domains)

After switching origins, update backend WEB_URL and the Google redirect URI, then deploy the backend. Keep old shared-link domains working if links have already been distributed; download fresh QR images for the chosen canonical domain. A localhost QR will never become a public QR automatically. Do not change BACKEND_URL to the public frontend domain: it must remain the private backend address.

## 7. Automated deployment from GitHub

Connect through Render's GitHub integration rather than only pasting a public Git URL. Set both services' Auto-Deploy option to **After CI Checks Pass** once their first deployment works. GitHub Actions verifies code and containers, then Render builds each service from that checked commit. Services deploy independently, so backend/frontend changes must remain compatible during rollout. [CI-gated deploys](https://render.com/docs/deploys)

The GitHub `Deploy Kubernetes` workflow is for Helm clusters; do not run it for Render. GHCR images are currently compiled for a Kubernetes backend hostname. Prefer Git-connected Render builds so its actual BACKEND_URL is compiled into the frontend.

For initial production releases you may instead set Auto-Deploy Off and manually deploy a tested commit after staging acceptance. Configure branch protection/review rules separately in GitHub; a workflow file does not establish those settings.

## 8. Verify before inviting families

1. Open the public HTTPS homepage and sample registry while signed out.
2. Open `/api/v1/session`; it should return JSON, not HTML, a502 or a connection error. Do not publish its CSRF token.
3. Sign in with Google, create a private registry, add a gift and upload a photo.
4. Publish the link, open it in a signed-out browser, download and scan its QR on a phone.
5. Request a confirmation email, explicitly confirm a reservation/purchase, and check availability and Purchased tags.
6. Revoke the share link; the old link/QR must stop revealing the registry.
7. Sign out and verify that owner pages cannot be accessed anonymously.
8. Remove your test registry after testing. Use a separate staging account/database; do not populate a family account with test records.

The existing scripts/smoke.mjs is LOCAL ONLY: it uses demo login and Mailpit. Do not aim it at production. Production validation requires a separate test account and controlled recipients.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Dockerfile not found | Latest commit; Root Directory empty; correct Dockerfile path/context pair |
| COPY package.json/pom.xml fails | Build context mismatch; use the exact table above |
| Homepage works, APIs return502 | Backend is running; actual internal address/port; same workspace/region; rebuild frontend after BACKEND_URL change |
| Backend cannot start | Missing env values, JDBC URL format, database credentials, memory and Flyway logs |
| OAuth redirect mismatch | Exact public scheme/host/path matches WEB_URL and registered Google callback |
| Cookies/login loop | HTTPS origin consistency, proxy headers and secure session cookie; do not disable cookie security as a workaround |
| Mail fails | Verified sender, provider credentials, STARTTLS/port match, delivery logs; local Mailpit is not available in Render |
| Session lost during deployment | Backend sessions are in memory; use one instance until shared session storage is added |
| QR opens localhost | Set public WEB_URL, deploy backend, generate/download a fresh QR |
| Old Playwright title visible | It was a test database record, not application branding. Its local record was removed; reload or go back to your own registry |

## Staging, production and rollback

Repeat these steps for separate staging and production resources. Keep databases, OAuth clients, senders/SMTP credentials and WEB_URL values separate. Environment groups can share non-secret common defaults; never share production database credentials with staging.

Back up before migrations and test restore procedures. Roll back both services to compatible commits if needed; reverting containers does not undo Flyway migrations or restore data. Our one-instance in-memory sessions mean deployment can log users out; shared Spring Session storage is required before reliable multi-instance scaling. Public launch also needs rate limiting, retention/legal review, operational alerts and dependable mail retries described in DELIVERY.md.

## Optional Blueprint route

The repository includes render.yaml with explicit frontend/backend Docker paths and sync:false environment placeholders. It does not create PostgreSQL or fill credentials. Manual setup above is recommended for repairing your existing service. If importing through New > Blueprint, inspect resource matching and potential duplicate/paid resources before applying; configure the actual database and backend address afterward. A committed Blueprint is not automatically applied to a service created manually.

## Current evidence

- Root Dockerfile repair and GitHub verification succeeded for commit56fc5eb.
- The local synthetic Playwright registry was removed, and its shared API returned404 at cleanup.
- No successful Render production deployment or production SMTP/OAuth operation has been observed in this session. This guide is a setup procedure, not a claim that the public app is live.
