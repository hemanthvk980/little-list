# Owner administration

## Enable your account

1. Deploy the backend migration V7 and the frontend from this release. Existing registry owners are backfilled without invented names or emails.
2. Sign in using your own Google account. Open `/api/v1/identity` on the frontend while signed in. It returns only your own stable identity (issuer + `|` + subject).
3. Set backend environment variable `ADMIN_GOOGLE_IDENTITY` to that exact string, e.g. `https://accounts.google.com|YOUR_GOOGLE_SUBJECT`. Do not use an email, OAuth client ID, or client secret. Redeploy the backend.
4. Sign in again and visit `/admin`. An Owner workspace link appears on your dashboard for the allowed account only.
5. Enable two-step verification on that Google account. This is an account setting; the application does not enforce a fresh MFA challenge itself.

Empty configuration denies everyone. Only one configured Google identity is allowed. Local demo logins never grant admin access. Remove the variable and redeploy to revoke access. There is no self-service promotion endpoint.

## Features and data

Read-only Users → registries → gift items and checklist. All collections are paginated in SQL, with 20/50/100 rows and stable timestamp/ID ordering (checklist uses its unique key). Name/email search and a Has registries filter apply to users. Page/filter state is in the URL. No exports, impersonation, deletion or edits are offered. Archived gifts are excluded.

User profiles are captured at successful Google login; email is retained only when verified. Existing profiles show as pending until their next login. First seen for legacy owners is their earliest stored registry date, not an inferred signup date. User account deletion removes the profile too. No claim tokens, guest email hashes or share codes appear in admin responses.

Next.js separates the admin route bundle from ordinary routes. Suspense and route loading fallbacks cover navigation; explicit loading/error/empty states cover client API requests. No heavy optional charts currently exist, so no artificial React.lazy wrappers are needed.

## Security and operations

Spring Security protects every `/api/v1/admin/**` endpoint; service checks provide a second guard. Existing CSRF and secure HTTP-only SameSite cookies remain enabled. Session inactivity timeout is 30 minutes. Sessions remain in memory and deployments can log users out. For multiple backend replicas, use a shared session store before scaling.

Spring Security sends no-store response headers; admin page headers prohibit caching/indexing. Records are held only in component memory. Queries use bound parameters and fixed SQL ordering. Pagination is bounded to 100 rows, search to 100 characters. Admin reads write minimal action/target/actor/time audit events. A PostgreSQL advisory lock serializes a per-admin quota of 120 reads per minute across replicas. Audit insert failure fails the read closed. Rejected requests are blocked before data access; infrastructure security logs should monitor repeated denials without logging credentials.

Audit retention and restricted database access must be configured by the operator; no automatic purge or export is enabled. Privacy copy now discloses owner access to private registries and profile information; the wider legal policy remains a launch-review draft.

## Fallbacks

Branded bootie/sock 404 and teddy temporary-error pages use lightweight decorative emoji and no animation, respecting reduced motion by default. Root rendering failures have an independent global error page. `/r/{code}` now resolves server-side with a 15-second timeout, forwards bot/prefetch headers and HEAD without counting them as visits, preserves sharing-channel attribution, accepts only expected relative registry redirects, and never caches results. Missing/revoked links return branded HTML with HTTP 404; backend errors return branded HTML with HTTP 503 and Retry-After. The retry link reloads the same short URL without inserting request data into HTML. HEAD responses have no body.

The short-link handler reads BACKEND_URL at runtime. Other API/OAuth rewrites still require a frontend rebuild when BACKEND_URL changes. A Render edge 502 when the frontend itself is unreachable cannot be replaced by app code; use an independent maintenance/edge layer if that coverage is needed.

Production email transport and the earlier Render SMTP limitation are unchanged.
