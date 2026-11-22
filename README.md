## Owner dashboard and fallback pages — 21 November 2026

Read-only `/admin` now supports paginated users, their registries, gift items and checklist entries. Google login persists verified profiles; V7 backfills existing owners. Access is denied until `ADMIN_GOOGLE_IDENTITY` is configured. See [ADMIN.md](ADMIN.md) for setup, security controls and deployment limitations. Branded 404/error pages and a timeout-aware short-link handler are included. Production email configuration remains separate.

## Help page update — 21 November 2026

Replaced the developer-facing help paragraph with parent and guest guides, expandable FAQs, and sample/dashboard links. Covers email confirmation, 48-hour reservations, purchase recording, privacy and QR sharing. Removed local Mailpit and launch-setup text from the public page. This update does not configure production email delivery or change backend routing.

## Complete Render guide - 18 November 2026

Follow [RENDER.md](RENDER.md) for the complete database, backend and frontend deployment sequence, exact service settings, Google/SMTP configuration, CI/CD, domains and troubleshooting. The root Docker build fix passed GitHub Actions. Live Render deployment still needs configuration and verification.

## Render build fix - 16 November 2026

A root frontend Dockerfile now supports Render default builds. See [RENDER.md](RENDER.md) for exact settings and required backend/database configuration. render.yaml declares both service build contexts. The local Playwright test registry was removed.

# GitHub delivery update — 16 November 2026

Repository: https://github.com/hemanthvk980/little-list. See [DELIVERY.md](DELIVERY.md) for GitHub Actions, the Jenkins alternative, Helm and environment/secret management. Helm templates are implemented; no cluster deployment or Jenkins server is configured. GitHub run status is available in the repository Actions tab; earlier notes saying no repository exists are historical.

# Current developer guide — 16 November 2026

Start with [INTELLIJ.md](INTELLIJ.md) for opening/debugging the code and [DEPLOYMENT.md](DEPLOYMENT.md) for hosting, GitHub CI/CD and prioritized enhancements. [INTEGRATIONS.md](INTEGRATIONS.md) records retailer/Spring AI research. The current verification counts below supersede older dated milestones. GitHub CI is prepared locally but not yet executed remotely; no production deployment exists.

## Registry photos, dashboard products and purchase reporting — 16 November 2026

Implemented registry profile-photo upload/removal, grouped gift categories and sidebar navigation, expandable checklist progress, dashboard/registry quick-add, editable gift ideas with retailer browsing shortcuts, guest I purchased this with email verification, and clear purchased/partially-purchased tags. Spring starter-mail remains the email integration. QR sharing from the preceding request is included. See INTEGRATIONS.md for the retailer API and Spring AI research, limits and next milestones. Live retailer feeds and AI recommendations are not enabled; native Android code does not exist.

Current automated verification: 16 real-PostgreSQL integration tests and 11 capture/extension/QR tests passed; Next production and Docker builds passed. Extended HTTP smoke covers QR attribution and emailed direct-purchase confirmation alongside reservation and security checks. The demo remains running on localhost:3000; its links/QRs need a public app URL before family members can use them on other devices.

## QR sharing and platform status — 16 November 2026

- Website: Next.js frontend and Spring Boot backend are implemented, with responsive mobile layouts.
- Android: no native Android project, APK or Play Store build exists. The Chrome extension is a separate desktop browser tool, not an Android app. A future Android client can reuse the Spring Boot API; mobile authentication, deep links, secure storage, device testing and release signing will need their own implementation.
- QR sharing: published registries show a locally generated 768 px PNG, a readable preview and Download QR code in Share registry. It encodes the short link plus `?via=qr`, and aggregate opens are attributed to QR code in Gift tracker → Link activity. No registry URL is sent to an external QR generation service.
- Revocation disables the printed QR link too. Republish and download a replacement after revoking. Counts have the same approximation limits as other short-link opens.
- Localhost QR codes are local-demo-only. A phone cannot reach this computer through a localhost URL. Use a deployed HTTPS app address before distributing invitations; no public hosting or tunnel was created.
- Renderer: [node-qrcode](https://github.com/soldair/node-qrcode) with medium error correction, four-module quiet zone, black modules and a white background. The download is deliberately free of decorative overlays for scanning.

# Little List

A working local baby wishlist: Next.js 16 + React 19, Java 21 + Spring Boot 3.5, PostgreSQL 17, Flyway, and Mailpit. This is the starter vertical slice, not a public production deployment.

## India registry update — 16 November 2026

Implemented the approved clean India-focused experience: homepage and sample registry, resumable three-step onboarding, individually selected starter collections, registry sidebar/mobile navigation, checklist decisions, gift tracker, greetings, variants, priorities, merchant and budget filters, and retailer link/manual capture for Amazon India, Flipkart and FirstCry. Capture survives sign-in; duplicate links prompt review. Gift saves and onboarding completion support idempotent retries. Migration V3 persists these additions with owner authorization.

The Manifest V3 Chrome extension is a local beta in extension/. It reads product suggestions only after a click, lets the user review them, then opens the app to choose a registry and save. It requests only activeTab and scripting. It has no automatic retailer fetching, live price synchronization, background monitoring or image import. The extension has not been installed or submitted to the Chrome Web Store, and live retailer layouts still need browser testing.

Verified: 11 PostgreSQL Testcontainers tests, 10 capture/extension tests, native Next production build, both Docker image builds, and native/container HTTP smoke tests. Browser checks covered login return, resumed onboarding, selected starter items, checklist persistence, INR/variant gift saving, duplicate warning, capture through logout/login, tracker counts, sharing/greeting and guest budget filtering. Mobile guest/tracker pages had no horizontal overflow at 390 px. Production Google sign-in and real SMTP remain unconfigured.

## Short links and celebratory sharing

See SHARING.md for short-link design, approximate analytics semantics and the family-feature roadmap. The Gift tracker now includes owner-only Link activity. Full links remain compatible.

## Run locally

Install Java 21, Node 24, and Docker Desktop. Maven is provided by the wrapper (first run downloads Maven 3.9.12).

From this directory:

```powershell
docker compose up -d --wait
cd backend
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

In another terminal:

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev -- --hostname 127.0.0.1
```

Open http://localhost:3000. Local sign-in: `demo` / `littlelist-local`. A second isolated host is `another` with the same local password. These identities exist only with the `local` Spring profile. On macOS/Linux use `./mvnw` and `npm`.

On the original Windows workspace, the portable Java installation is `../../../work/jdk/jdk-21.0.13.0.2+1` relative to `backend`. Set `JAVA_HOME` to its absolute path and prepend `$env:JAVA_HOME\bin` to `PATH` if Java is not installed globally.

The verification inbox is http://localhost:8025. Mailpit captures messages locally; no external email account is needed. Create a list, add an item, publish, open the shared link, request an email using any `@example.test` address, open its Mailpit message, confirm, and record purchase. Save the private claim link before leaving.

PostgreSQL data persists in `littlelist-data`. `docker compose down` stops the services; do not add `-v` unless intentionally deleting the database.

## Optional all-container run

Stop a native frontend already using port 3000, then run:

```powershell
docker compose -f compose.yaml -f compose.app.yaml up --build -d
```

This is still a local demo. Backend and frontend runtimes use non-root users. The backend is reachable only on the Compose network; the frontend and supporting tools bind to host loopback. Next rewrites bake `BACKEND_URL` into the build; change the Docker build argument for a different topology. Use `docker compose -f compose.yaml -f compose.app.yaml stop frontend backend` to stop only the apps.

## Verify

```powershell
cd backend
.\mvnw.cmd verify
cd ../frontend
npm.cmd ci
npm.cmd run build
npm.cmd audit --omit=dev --audit-level=high
cd ..
node --test scripts/extension.test.mjs scripts/capture.test.mjs scripts/qr.test.mjs
node scripts/smoke.mjs
```

The integration suite creates an isolated PostgreSQL Testcontainer; Docker must be running. The HTTP smoke test requires both apps plus local Mailpit and uses only disposable lists it creates. It verifies the real Next proxy, session rotation/login/logout, CSRF rejection, ownership, no-store, email delivery, GET safety, confirmation retries, share revocation, and repeated purchase recording. The smoke test deletes its own wishlist in `finally`; its test email remains in Mailpit.

Stop a running packaged Java process before rebuilding its JAR on Windows; Windows locks open JARs. The frontend production command is `npm run start` after `npm run build`.

`.github/workflows/verify.yml` assumes this directory is the repository root. It runs PostgreSQL tests, frontend build, HTTP smoke test, npm audit, and container builds. It has been written but has not been run on GitHub. No remote repository, deployment, or production credentials were created.

## Implemented behavior

- Private drafts, list details, gift creation/editing, quantities, category, variant notes, optional integer-paise estimates and optional retailer URL. Server validates http/https schemes and rejects embedded credentials; it never fetches a retailer URL.
- Publish opaque UUID link, copy/WhatsApp controls, guest preview, revocation and new link on republish. Publishing twice retains the same link. All authenticated operations enforce ownership; public DTOs omit owners and guest credentials.
- Guest request sends a 30-minute verification link but allocates nothing. Only explicit CSRF-protected POST confirmation allocates one unit: a 48-hour hold for reservation intent, or a purchased unit for purchase intent. Tokens are 256-bit random values, stored only as SHA-256 hashes. URLs keep claim secrets in fragments to avoid server access logs and referrers.
- Retry confirmation with the same browser-generated management secret returns the same claim. A different secret cannot replay it. The browser saves that secret in localStorage before posting. Retrying in a different browser is intentionally unsupported; keep the private management link.
- Every inventory write takes wishlist → gift locks in the same order. Counts use database wall-clock time after locking. Purchased + active held cannot exceed quantity. Expired holds stop counting without a cleanup task. Purchase and release are idempotent for identical states and otherwise final.
- Existing claims remain manageable after unpublishing. Gift removal archives only when there are no purchases/active holds. Wishlist deletion removes its requests and claims. Account-data deletion removes all owned lists and invalidates the session; it does not delete an external Google account.
- Five verification requests per recipient or gift per 15 minutes, with database-backed serialization and HTTP 429/Retry-After. Starter limits: 100 lists per owner, 100 gifts per list; list responses are bounded rather than paginated.
- CSRF, server sessions, no-store API responses, no-referrer headers, noindex on private/share/claim pages, generic metadata, loading/error/empty states, keyboard labels, mobile layout, help and explicitly unfinished legal drafts.
- SMTP calls occur after the request transaction commits, with connection/read/write timeouts. A send failure returns 503 and consumes a quota slot. There is no outbox, guaranteed retry, or delivery tracking yet. SQL lock/statement and connection-pool waits are bounded.

API schemas and operations are in `openapi.json`. Forms use HTML field constraints plus server Bean Validation; server errors preserve user inputs. Security-filter errors may have empty bodies. Host IDs and claim credentials are not sent to analytics; no analytics SDK is installed.

## Chrome button (local beta)

Extract the extension package or use extension/. In Chrome, open chrome://extensions, enable Developer mode, choose Load unpacked, and select that directory. Start the app on http://localhost:3000 first. On a product page, click the extension, Read this product, review details, then Review in Little List. See extension/README.md for release gates.

## Before a public pilot

Configure production Google OIDC, a verified mail sender/provider, HTTPS domain, managed database and secrets. Use the `google` profile without `local`. The example `.env` is documentation: export variables in your process or configure your deployment platform; Spring does not automatically read it. Verify production cookies and OAuth redirects against the real reverse proxy.

Add edge IP/body/concurrency limits and sign-in throttling, a reliable mail outbox with delivery monitoring, data-retention cleanup, provider credential redaction, reviewed CSP, dependency/image scanning, staging deployment and rollback, a backup/restore drill, operational dashboards/alerts, support contact, legal review, and privacy-configured pilot analytics. Google sign-in is wired but untested without credentials. This starter does not claim load, accessibility-audit, failover, or production-security certification.

No retailer metadata scraping, payment collection, cash funds, co-hosting, SMS/OTP, queues, Redis, WebSockets/SSE, Kubernetes, read replicas, or sharding is implemented. Adoption triggers remain in the parent product plan. API instances use in-memory sessions: deploy one instance until a shared session store is configured.
