## 23 November 2026 — product dialog, brands and purchase contacts

- Next.js production build passed. Browser fixtures verified desktop preview data, modal close, 390 px mobile layout without horizontal content overflow, brand search, purchased gift controls, partial quantity availability and purchase-record pagination. These UI fixtures did not create user registries or send emails.
- Real PostgreSQL/Testcontainers suite: AdminApi 7, EmailDelivery 5, WishlistService 19 passed. New cases cover owner-only contacts, explicit confirmation, no public leakage, pagination bounds, erasure without inventory change, exhausted request rejection and retention. ProductPreview 10 tests passed separately after CDN/image-candidate/FirstCry changes. Capture/extension/QR: 16 tests passed.
- Read-only retail audit: 25 Amazon browser products, 24 direct backend lookups, two FirstCry pages and one Flipkart page. See RETAILER-AUDIT.md for limitations; Amazon backend import is NOT claimed fixed. ProductImportAuditTest is intentionally opt-in/skipped in CI.
- No production deployment was verified. V9 must run on the backend before the new frontend. Thank-you tracking was deliberately deferred.

## 23 November 2026 — occasion/photo fixes and Amazon lookup

- ProductPreviewTest: 8 passing tests including the actual Amazon alternate price-block pattern and image URL shape. Existing database schema unchanged.
- Capture/extension/QR: 14 passing tests, including alternate price/image extraction and live/local handoff selection.
- Next production build passed.
- Playwright synthetic API fixtures: grouped occasion selection and custom wording round-trip; 390 px no overflow; selected photo preview and cancellation; in-flight cancellation feedback; centred photo placeholder; failed product refresh preserves edited name, price, image and note. No user data changed and no emails sent.
- Live browser inspection/execution of the extension extractor on user-supplied Amazon product B0FH79G2MW returned the actual title, ₹499.00 estimate and image URL. Direct Java fetch returned generic HTML without product data; server scraping remains unreliable. No promise of live Render lookup success or Web Store publication.

## 22 November 2026 — product previews and registry image rows

- Maven verify: 36 tests passed (17 wishlist/database, 7 admin/security, 7 product preview, 5 email), no failures/errors/skips. V8 image-reference migration exercised on PostgreSQL 17.
- Next production build passed; final TypeScript check passed after admin singular/plural copy correction.
- Capture, extension and QR suite: 13 tests passed.
- Playwright against an isolated local frontend with synthetic API/image fixtures: Review gift prefills name, price and image; desktop uses three horizontal columns; 390 px mobile has no document overflow; broken images use category art; purchase action still opens email confirmation; admin has no mojibake and checkbox measures 20 by 20 px. No real registry records or emails created.
- This verification does not establish reliable live scraping across retailers. CAPTCHA, JavaScript-only pages, changing markup and hotlink policies remain external limitations. No R2 account, product-image upload or production rollout was performed.

## Conditional email providers — 22 November 2026

Maven verify passed with 27 tests (16 registry, 6 admin, 5 email). Email tests cover SMTP health registration, absence of JavaMailSender/SMTP health in Resend mode even with stale Gmail settings, required API credentials, explicit disabled failure, local mock HTTP payload/idempotency headers and 401/429/500/redirect rejection without secret disclosure. No external emails were sent and no production provider key was used. Initial full test run required starting Docker; the complete suite was rerun successfully afterward. Live delivery requires operator configuration described in EMAIL.md.

## Owner admin and fallbacks — 22 November 2026

- 22 PostgreSQL-backed tests passed: 16 existing registry tests plus 6 admin tests. New checks cover anonymous/non-admin denial on every route, issuer mismatch, empty allowlist, profile upsert, pagination bounds and separation, drilldown queries, sensitive-field exclusion, missing records, no-store headers, audit events and rate limits.
- Next production build and TypeScript passed. The 11 existing capture/extension/QR tests passed; the additional production-server short-link integration test passed after correcting its test server working directory. It checks success redirects, QR attribution, HEAD/prefetch forwarding, HTTP 404/503 fallback bodies, no-store and rejection of external redirects.
- Playwright with synthetic intercepted API fixtures verified users pagination → registries → gift items, ordinary-user denial and 390px mobile layout without horizontal overflow. Browser fixtures are not a real Google login or live production validation. No user registry records were created.
- ADMIN.md documents the required owner identity setting. Live Render deployment and owner activation have not been verified. Production SMTP limitations remain.

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
