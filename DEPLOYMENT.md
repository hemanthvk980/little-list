# Deployment, GitHub and next product milestones

Updated 16 November 2026. This is a production plan, not a deployed environment.

## Recommended hosting

| Option | Fit for Little List | Tradeoff |
| --- | --- | --- |
| Render | First pilot: Next container + private Spring container + PostgreSQL in Singapore | Separate paid services; configure backup policy and verify restore. |
| Railway | Alternative single workspace with two app services and PostgreSQL; Singapore region | Monitor usage costs and database backup/operations configuration. |
| Vercel + Render/Railway | Next frontend on Vercel, Spring and database elsewhere | More networking/auth configuration; Vercel cannot reach another provider's private network directly. |

My recommendation is Render for the first controlled pilot. Keep the Next frontend, Spring private service and database in the same region. One public HTTPS origin proxies `/api`, `/r`, `/oauth2` and `/login/oauth2` to Spring, preserving the existing cookie/CSRF model. Test Indian mobile latency before deciding on a region. Singapore is not Indian data residency. If India-only hosting becomes a requirement, assess an India-region cloud topology separately. No pricing quote or free-tier production promise is implied.

Official references checked: [Render monorepos](https://render.com/docs/monorepo-support), [Render deployment gates](https://render.com/docs/deploys), [Render regions](https://render.com/docs/regions), [Railway GitHub autodeploy](https://docs.railway.com/deployments/github-autodeploys), [Railway regions](https://docs.railway.com/deployments/regions), [Next.js on Vercel](https://vercel.com/docs/frameworks/full-stack/nextjs).

## GitHub and CI/CD

The repository is now https://github.com/hemanthvk980/little-list. See DELIVERY.md for the implemented Actions/Jenkins/Helm flow.

Keep little-list itself as the repository root (backend/, frontend/ and .github/ directly inside it). After choosing/creating the destination, initialize Git locally, review `.gitignore` and staged files, commit, then push to its `main` branch. Do not push this entire Codex workspace or test databases. Configure identity locally if Git requests it; use GitHub credential manager, never a token embedded in the remote URL.

The existing `Verify Little List` workflow tests real PostgreSQL via Testcontainers, builds Next, tests capture/extension/QR, runs the local SMTP HTTP smoke, audits production npm dependencies, and builds both containers. It now uses the pinned Maven wrapper, a timeout, superseded-run cancellation, and retained test reports. GitHub execution remains unverified until the first push.

1. Run CI on pull requests and pushes; require its `verify` job in branch protection and require reviews for main.
2. Connect only the chosen repository to the hosting provider; use separate staging/production services and databases.
3. On Render set Auto-Deploy to **After CI Checks Pass**, not unconditional commit deploys. Initially keep production deployment manual; staging can deploy after checks pass.
4. Deploy backend with Docker context `backend`, Dockerfile `backend/Dockerfile`; frontend context `frontend`, Dockerfile `frontend/Dockerfile`. Configure paths according to the provider's context-relative UI. Rebuild frontend with its `BACKEND_URL` build argument pointing to the backend private URL, including port8080.
5. Set the frontend port3000 and backend port8080. Probe backend `/actuator/health`; check frontend `/`. Validate login, photo upload, short link, QR attribution and SMTP in staging. The current smoke script is LOCAL ONLY and uses local login and Mailpit; do not run it against production.
6. Promote the tested commit after staging acceptance. Back up before schema changes; use additive migrations. App rollback does not reverse Flyway database changes. Restore drills must precede launch.

Production configuration: `SPRING_PROFILES_ACTIVE=google` (never local), `SERVER_ADDRESS=0.0.0.0`, `WEB_URL=https://your-domain`, JDBC `DATABASE_URL=jdbc:postgresql://host:5432/db` plus `DATABASE_USER`/`DATABASE_PASSWORD`; apply the provider's database TLS requirements. Set `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`; register `https://your-domain/login/oauth2/code/google`. Set `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_AUTH=true`, `MAIL_STARTTLS=true` for a compatible STARTTLS SMTP provider. Store these in provider secrets. `BACKEND_URL` is a frontend build-time setting: changing only runtime variables does not update rewrites.

Before public release: verify HTTPS/proxy redirects and secure cookies, external SMTP with a verified sender, durable mail outbox/retries, rate limits, retention/legal pages and restore monitoring. Sessions are currently in memory: start with one backend instance; deployment may log users out. Add Spring Session with a shared store before scaling. Local demo accounts, Mailpit and Compose credentials must stay local. Production configuration and accounts are not yet supplied, so automatic production CD is documented, not activated.

## Differentiating features to prioritize

These are product hypotheses, not claims that no competitor offers them.

| Priority | Enhancement | Concrete user outcome / integration |
| --- | --- | --- |
| Next | Family co-host roles | A sibling can organize gifts without sharing the parent's password; owner/editor roles and revocable invites. |
| Next | Help calendar | Meal drops, errands and visits as time slots, with capacity and private arrangements rather than shopping links. |
| Next | Already-have and borrow-first | Mark owned/borrowed items and offer family hand-me-downs before suggesting purchases. |
| Next | Private planning, selected gifts shared | Keep sensitive planning entries private while publishing only chosen gifts. |
| Pilot | Complete regional-language guest flow | English plus one pilot language, including errors and confirmation emails; family-reviewed copy. |
| Pilot | Occasion invitation kit | Download QR invitation card for Godh bharai/Seemantham/naming ceremony, editable wording and aggregate source activity. |
| Later | First-year carry-forward | Move unpurchased gifts and future sizes into milestones without auto-sharing or unsolicited reminders. |
| Later | Explainable suggestions | Spring AI ranks approved catalogue IDs by missing categories/budget, with a reason and deterministic fallback; no invented price or automatic purchase. |

Retailer integration should populate native product cards through approved catalogue/API access, not embedded storefront iframes. Amazon Creators API and Flipkart affiliate API require onboarding/credentials; no public FirstCry API was verified. Existing manual URL capture and explicit save work without these. See INTEGRATIONS.md for official Spring AI repositories, version compatibility and retailer sources. AI stays last as requested. Add payment collection only as a separately designed provider-backed feature; no UPI fund collection is implemented.
