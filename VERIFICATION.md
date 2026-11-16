## Registry photos, dashboard products and purchase reporting — 16 November 2026

Implemented registry profile-photo upload/removal, grouped gift categories and sidebar navigation, expandable checklist progress, dashboard/registry quick-add, editable gift ideas with retailer browsing shortcuts, guest I purchased this with email verification, and clear purchased/partially-purchased tags. Spring starter-mail remains the email integration. QR sharing from the preceding request is included. See INTEGRATIONS.md for the retailer API and Spring AI research, limits and next milestones. Live retailer feeds and AI recommendations are not enabled; native Android code does not exist.

Current automated verification: 16 real-PostgreSQL integration tests and 11 capture/extension/QR tests passed; Next production and Docker builds passed. Extended HTTP smoke covers QR attribution and emailed direct-purchase confirmation alongside reservation and security checks. The demo remains running on localhost:3000; its links/QRs need a public app URL before family members can use them on other devices.

## Short-link update — 16 November 2026

V4 adds first-party 22-character short-link codes, owner-only aggregate daily/channel opens, instant revocation checks and compatible full registry URLs. Share controls use the short URL and an editable WhatsApp message. Warm peach/gold accents, the requested homepage message, and reduced-motion-aware celebrations are implemented. See SHARING.md for exact counting semantics and the remaining family-feature roadmap.

Validation: 13 PostgreSQL integration tests passed with no failures/errors/skips; Next production and Docker builds passed. Extended HTTP smoke checks cover anonymous redirect, no-store, HEAD/bot exclusions, owner-only statistics and revoked-link rejection in addition to the existing claim workflow.

# Verification — 16 November 2026

## India registry update — 16 November 2026

Implemented the approved clean India-focused experience: homepage and sample registry, resumable three-step onboarding, individually selected starter collections, registry sidebar/mobile navigation, checklist decisions, gift tracker, greetings, variants, priorities, merchant and budget filters, and retailer link/manual capture for Amazon India, Flipkart and FirstCry. Capture survives sign-in; duplicate links prompt review. Gift saves and onboarding completion support idempotent retries. Migration V3 persists these additions with owner authorization.

The Manifest V3 Chrome extension is a local beta in extension/. It reads product suggestions only after a click, lets the user review them, then opens the app to choose a registry and save. It requests only activeTab and scripting. It has no automatic retailer fetching, live price synchronization, background monitoring or image import. The extension has not been installed or submitted to the Chrome Web Store, and live retailer layouts still need browser testing.

Verified: 11 PostgreSQL Testcontainers tests, 10 capture/extension tests, native Next production build, both Docker image builds, and native/container HTTP smoke tests. Browser checks covered login return, resumed onboarding, selected starter items, checklist persistence, INR/variant gift saving, duplicate warning, capture through logout/login, tracker counts, sharing/greeting and guest budget filtering. Mobile guest/tracker pages had no horizontal overflow at 390 px. Production Google sign-in and real SMTP remain unconfigured.

The initial container smoke ran before Spring was ready and failed to connect. Once ready it passed. Compose now has a backend health check and waits for health before starting the frontend.

Preview app processes and app containers were stopped after verification; PostgreSQL/Mailpit and their data remain intact. The latest demo registry contains four illustrative gifts; retailer URLs are test data, not verified product recommendations.

## Earlier baseline verification

# Verification — 15 November 2026

The local vertical slice is implemented and verified. No public deployment was performed.

| Check | Observed result |
| --- | --- |
| Java 21 / Maven wrapper `verify` | Passed; 8 PostgreSQL Testcontainers tests, 0 failures, 0 errors, 0 skips |
| Migrations | V1 and V2 applied to fresh Testcontainers and existing local PostgreSQL |
| Next.js production build | Passed, all 11 page routes generated/compiled; TypeScript passed |
| `npm audit --omit=dev --audit-level=high` | Passed; 0 reported vulnerabilities |
| Native-app HTTP smoke | Passed through Next → Spring → PostgreSQL and local SMTP |
| Docker builds | Both multi-stage images built successfully |
| Container startup | Backend and frontend started; Flyway confirmed schema current |
| Container HTTP smoke | Passed through containerized Next/Spring and existing PostgreSQL/Mailpit |
| Runtime identity | Backend `app`; frontend `node` (non-root) |
| Browser host flow | Local login → private draft → gift with category, note and INR estimate → publish |
| Browser guest flow | Shared list → test email → Mailpit verification link → explicit hold confirmation → marked purchased |
| Browser final inventory | 0 available, 0 held, 1 purchased; “All units spoken for” disabled |
| Mobile | Host editor and guest view inspected at 390 × 844; measured scroll width 375 ≤ viewport 390; override reset |

Integration tests cover last-unit races, competing quantity edits, owner isolation, request/hold expiry, revocation with existing claim management, repeated confirmation bound to the same secret, idempotent/final purchase and release, deletion rules, URL validation, and send quotas. Tests use isolated real PostgreSQL, not an in-memory database. Testcontainer resources were removed automatically.

The repeatable HTTP smoke additionally asserts anonymous-host denial, missing-CSRF rejection, session ID rotation on sign-in, logout invalidation, safe public fields, no-store, local SMTP delivery, GET confirmation-page safety, retry identity, revocation, and terminal-state conflict. It deletes only the lists it creates. Test mail remains in the local inbox.

Browser test data under local user `demo` was intentionally retained: “Our little one’s wishlist” / “Godh bharai”, with a purchased cotton swaddle. It is local sample data, not a real order. No WhatsApp message or external email was sent; the WhatsApp sharing control was not submitted.

Final teardown stops the native app processes and the two app containers. The pre-existing PostgreSQL and Mailpit services and database volume remain running/intact. To reopen the built app:

```powershell
cd C:\Users\Heman\Documents\Codex\2026-11-15\ana\outputs\little-list
docker compose -f compose.yaml -f compose.app.yaml up -d
```

Then open http://localhost:3000. See README.md for source development and tests.

Verification limits: Google OAuth and a production SMTP provider were not configured/tested; CI has not run on GitHub; no production deploy, image vulnerability scan, load test, formal accessibility audit, disaster recovery exercise, or backup restore was performed. Legal pages are explicit launch-review drafts. Edge request controls, mail outbox/retries, cleanup/retention, shared sessions, monitoring, analytics and production secrets remain pilot gates.

One Windows build initially failed because the running Java process held the output JAR open. After stopping that owned process, Maven wrapper `verify` passed. A first frontend build found five Windows-encoded generated files; those files were converted to UTF-8 and subsequent native and Docker builds passed.
