# Accounts and family onboarding

Implemented 26 November 2026. These changes are local code changes until deployed; this document does not certify a production security audit.

## Experience

The homepage retains Little List's ivory/plum palette and existing imagery, adding interactive family-moment suggestions and gentle motion that respects reduced-motion preferences. Babylist's public homepage and signup steps informed the functional review, but artwork, wording and layouts are Little List's own. No Babylist account was created for the review.

Onboarding has five visible progress stages: family chapter; optional arrival/homecoming date and multiples; parenting experience and help preference; title/occasion/greeting; chosen starter gifts. Chapters include pregnancy, adoption, surrogacy, baby already here, planning ahead and family support. Adoption uses homecoming/sibling language. Dates and preferences are optional. Anonymous progress is stored in browser session storage and resumes after account creation. Signed-in progress is saved to the backend. Users can go back and edit before creation.

The owner workspace shows days until an estimated date, with neutral messaging on/after that date. Calendar-day arithmetic avoids daylight-saving hour differences. Settings lets the owner change, clear or hide the countdown, immediately updating the workspace panel. Journey data is owner-only and excluded from public/shared registries.

## Account flow

- `/sign-up`: name, unique username (3–32 ASCII letters/digits/underscore), email and password.
- Email verification: `/account/verify#token=...`; explicit confirmation activates the account. Email scanners opening a link do not consume it.
- `/sign-in`: username/password or the existing Google OAuth flow when configured.
- `/account/recovery`: reset password or resend verification.
- `/account/reset#token=...`: choose a new password. All previously issued password sessions become invalid on their next request.
- Google and password identities are deliberately separate, even when emails match. Existing Google registries do not automatically transfer to a new password account. There is no account-linking feature.
- The admin allowlist still requires the configured verified Google issuer/subject. Creating a password account with the owner's email does not grant admin rights.

## Backend deployment

1. Back up PostgreSQL using your hosting provider's process. Deploy the backend first; Flyway V10 adds tables without modifying older migrations.
2. Keep database credentials configured as described in RENDER.md. Never expose database credentials to frontend environment variables.
3. Set `WEB_URL` to the public **frontend HTTPS origin**, without a trailing slash. Verification/reset links use this address.
4. Configure working email delivery using EMAIL.md. For Render Free use the existing HTTPS provider integration (`MAIL_PROVIDER=resend`, `RESEND_API_KEY`, verified `MAIL_FROM`), not an unreachable SMTP port. No new email provider is required.
5. Leave `PASSWORD_BREACH_CHECK=true` in production (default). Outbound HTTPS to `api.pwnedpasswords.com` must work. Signup/reset fails with a retryable 503 if screening cannot complete; signin remains available. No API key is needed. Local profile disables external screening for deterministic offline tests.
6. Keep `SPRING_PROFILES_ACTIVE=google` and Google credentials if offering Google login. Password accounts do not require Firebase or Google credentials. Never use `local` in production.
7. Rebuild/redeploy the frontend with its existing `BACKEND_URL` set to the real backend origin. Next rewrites are built into the deployment; a restart alone may retain an old backend destination.
8. Smoke-test signup, real inbox delivery, verification, signin, reset, logout, registry creation and guest privacy on HTTPS. No live production smoke test is implied by local checks.

## Security controls and limits

Passwords are NFC-normalized, 15–128 Unicode code points, hashed with PBKDF2-HMAC-SHA256 at 600,000 iterations and a random 16-byte salt. Paste and password managers are supported; there are no forced character-class rules or periodic expiry. A small local predictable-password blocklist is supplemented in production by Have I Been Pwned range screening: only the first five SHA-1 hash characters are transmitted, with response padding. SHA-1 is used only for the lookup protocol, never for password storage.

Verification/reset tokens have 256 bits of randomness, expire after 30 minutes, are stored only as SHA-256 hashes and consumed once. Issuing a replacement invalidates the previous token for that purpose. Email links carry the token in a fragment, with site-wide no-referrer policy. Login rotates the session ID and CSRF token. CSRF checks protect all authentication mutations; production session cookies are HttpOnly, Secure and SameSite=Lax with a 30-minute inactivity timeout.

Database-backed 15-minute rate limits: 10 login attempts per normalized username; 3 signup attempts per normalized email; 3 recovery requests per email; 8 attempts per token; plus 300 authentication requests per servlet remote address. Untrusted forwarded headers are not used. A reverse proxy may aggregate users under one address, so review trusted-proxy handling and edge rate limits before larger traffic. Counters are shared across application replicas; HTTP sessions are currently in memory. Multiple backend replicas need Spring Session/shared storage or carefully configured session affinity. Restarts sign users out.

Generic successful signup/recovery responses reduce account enumeration, but delivery timing and provider failures may still reveal differences. Email is sent synchronously after account changes commit; failed delivery can be retried via recovery. There is no transactional mail outbox yet. Screening and hashing also consume server/DB capacity: load-test before opening high-volume signup.

Hourly cleanup removes expired account tokens, rate-limit records older than a day and unverified accounts older than seven days. Existing guest-detail retention scheduling is now enabled too. Account deletion removes password credentials and associated tokens; registry deletion removes its journey data. Backups and email-provider copies follow their own retention rules.

Not implemented: MFA/passkeys, account linking, username recovery, distributed sessions, device management, CAPTCHA/adaptive bot protection or a formal penetration test. Do not describe this as certified compliance. Prioritize MFA for owner/admin access and operational monitoring before broad rollout.

References: [NIST SP 800-63B-4](https://pages.nist.gov/800-63-4/sp800-63b.html), [OWASP password storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html), [HIBP range API](https://haveibeenpwned.com/API/v3).

## API additions

All mutations require the CSRF token from `GET /api/v1/session` in `X-CSRF-TOKEN`.

| Endpoint | Purpose |
| --- | --- |
| POST `/api/v1/auth/signup` | name, username, email, password; generic 202 |
| POST `/api/v1/auth/login` | username, password; 204 or generic 401 |
| POST `/api/v1/auth/verify-email` | email; generic 202 |
| POST `/api/v1/auth/forgot-password` | email; generic 202 |
| POST `/api/v1/auth/verify` | token; 204 |
| POST `/api/v1/auth/reset` | token, password; 204 |
| GET/PUT `/api/v1/wishlists/{id}/journey` | Owner-only journey fields |

Journey fields: chapter, nullable arrivalDate (ISO date), multiple, experience, feeling, countdownEnabled. Onboarding completion accepts optional journey data; older drafts continue working.

## Verification

The full PostgreSQL integration suite passed 48 tests, with the opt-in live retailer audit skipped. Account tests cover verification, single-use/expired tokens, reset revocation, duplicate accounts, CSRF, throttling, password normalization, owner isolation and exclusion of journey details from shared output. A separate password-range parser test also passed, covering positive breach matches and zero-count padding (49 passing backend tests total). All 17 existing JavaScript checks and the Next production build passed.

Local Playwright exercised anonymous onboarding through signup, Mailpit verification, signin, registry creation and the private arrival countdown. Password recovery and signin with the replacement password passed through Mailpit. Countdown save/hide updated the other workspace panel immediately. The temporary QA account, registry and its mail were deleted after testing. Responsive layouts were checked at desktop and 390px widths. Production provider delivery and Render configuration still require deployment smoke tests.
