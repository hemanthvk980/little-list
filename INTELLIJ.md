# IntelliJ and local development

Project root on this computer:
`C:\Users\Heman\Documents\Codex\2026-11-15\ana\outputs\little-list`

Open that directory in IntelliJ to see backend, frontend, extension, scripts and documentation together. Add `backend/pom.xml` as a Maven project using the Maven tool window. Alternatively open `backend/pom.xml` directly as a project for Java-only work. Use Java 21 for both Project SDK and Maven runner; choose Maven wrapper. The installed portable JDK is `C:\Users\Heman\Documents\Codex\2026-11-15\ana\work\jdk\jdk-21.0.13.0.2+1`.

Java main class: `in.littlelist.LittleList`. Create an Application configuration with that main class, backend module classpath, backend working directory and program argument `--spring.profiles.active=local`. Community edition can run the Java main class; dedicated Spring tooling depends on the IDE edition. No personal IDE settings are committed.

## Choose one execution mode

The all-container demo is currently running at http://localhost:3000. Browse code in IntelliJ without stopping it. To rebuild it from the project root:

```powershell
docker compose -f compose.yaml -f compose.app.yaml up --build -d --wait
```

For debugging Java from IntelliJ, first free the app ports while preserving the database and mail inbox:

```powershell
docker compose -f compose.yaml -f compose.app.yaml stop frontend backend
docker compose up -d --wait
```

Run the Java configuration, then in the IntelliJ terminal:

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev -- --hostname 127.0.0.1
```

Node 24 is used in CI. Sign in with local-only `demo` / `littlelist-local`. Mailpit is http://localhost:8025. Never enable the local profile on a public deployment. Do not run native and container apps simultaneously on port 3000. Do not use `docker compose down -v`: that deletes registry data.

## Code map

| Area | Location |
| --- | --- |
| Registry/dashboard, photo and direct-add UI | frontend/components/registry.tsx, registry-extras.tsx |
| Guest purchase and confirmation UI | frontend/components/client.tsx |
| QR rendering | frontend/components/share-qr.tsx |
| API, inventory and purchase transactions | backend/src/main/java/in/littlelist/Api.java, WishlistService.java |
| Photos and short links | backend/src/main/java/in/littlelist/RegistryPhotos.java, ShortLinks.java |
| Database migrations | backend/src/main/resources/db/migration (through V6) |
| Browser extension | extension/ |
| CI | .github/workflows/verify.yml |

## Current verification

16 PostgreSQL integration tests, 11 JavaScript capture/extension/QR tests, production frontend/container builds and the extended HTTP smoke passed for the app changes. Browser checks confirmed photo upload, dashboard product saving, category navigation, QR preview and guest-page rendering. Browser automation became unavailable before the final direct-purchase UI check; the email-confirmed direct-purchase state and tag data are covered by HTTP/integration checks. No physical-phone QR scan or production SMTP/OAuth test is claimed.

The website and desktop Chrome extension have code. A native Android app is not implemented.
