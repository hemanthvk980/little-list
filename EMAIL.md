# Email delivery configuration

`MAIL_PROVIDER` selects exactly one `EmailDelivery` bean with Spring `@ConditionalOnProperty`:

| Value | Transport | Use |
| --- | --- | --- |
| `smtp` (default) | Spring JavaMailSender | Local Mailpit or SMTP-capable hosting |
| `resend` | Resend HTTPS API on port 443 | Render Free |
| `disabled` | No external transport; requests fail with HTTP 503 | Temporary operation while credentials are prepared |

Conditional annotations select beans at startup; they do not switch providers after a runtime timeout. We deliberately avoid automatic SMTP-to-API failover because delivery can have succeeded before a timeout, causing duplicates. Resend requests have a stable idempotency key derived from the verification token's hash; the token itself is not used as the key. There is no automatic retry loop or durable outbox in this release.

## Render setup

The observed error is `MailHealthIndicator: Couldn't connect to smtp.gmail.com, 587`. Render Free blocks outbound SMTP ports 25, 465 and 587 for all providers. Changing SMTP credentials or the SMTP vendor will not solve that network restriction.

After deploying this commit, set these variables on the **backend**:

```dotenv
MAIL_PROVIDER=resend
RESEND_API_KEY=<your-resend-sending-api-key>
MAIL_FROM=Little List <gifts@your-verified-domain.example>
```

1. Create a Resend account and choose its free plan if suitable for your traffic. Current published free daily limit is 100 emails; verify current quotas in your account.
2. Add a domain you own and publish the DNS verification records Resend provides. Domain registration may cost money; the `onrender.com` subdomain is not a domain you control for email DNS verification.
3. Create a sending API key restricted to that domain where possible. Put it in Render environment settings, not GitHub or a frontend variable.
4. Set MAIL_FROM to an address on that verified domain. A Gmail address cannot be used as an arbitrary verified sender.
5. Save and redeploy the backend. No frontend rebuild is needed for this provider change.
6. Check backend `/actuator/health`, then test one real verification flow manually. Provider acceptance is not proof of inbox delivery; inspect the provider dashboard if needed.

For a limited self-test, Resend's `onboarding@resend.dev` sender can send only to the email associated with the Resend account. Verify your own domain before sending to other guests.

Old MAIL_HOST / MAIL_PORT / MAIL_USERNAME / MAIL_PASSWORD values are ignored in `resend` mode. Remove unused SMTP secrets after switching. Google login credentials remain unrelated and unchanged.

## Temporary fallback

If the API key/domain is not ready, explicitly set `MAIL_PROVIDER=disabled` and redeploy **after the updated code is available**. Core registry/admin functionality can run, but email-based reservations and purchase confirmations return an error. They are not auto-confirmed and no verification links are exposed in logs or API responses.

Do not interpret a healthy core service as working email in disabled mode. Likewise, Resend mode does not send probe emails on every health request. Monitor delivery failures and provider quota separately.

## SMTP and health

SMTP mode retains Spring's SMTP health contributor, configured connection/read/write timeouts and local Mailpit behavior. SMTP requires MAIL_HOST and MAIL_FROM. Resend requires RESEND_API_KEY and MAIL_FROM and fails startup if missing. Unknown provider names create no delivery bean and fail startup instead of silently selecting another provider.

JavaMail auto-configuration is excluded; only the SMTP conditional configuration creates JavaMailSender. Thus an old Gmail SMTP health probe cannot keep the Resend/disabled deployment DOWN. Resend failure logging contains only status codes or exception class names, never API keys, addresses, message bodies or verification links. HTTP timeouts are bounded, redirects are not followed and errors remain HTTP 503 to clients.

## Sources

- https://render.com/docs/free
- https://resend.com/pricing
- https://resend.com/docs/api-reference/emails/send-email
- https://resend.com/docs/knowledge-base/403-error-resend-dev-domain
