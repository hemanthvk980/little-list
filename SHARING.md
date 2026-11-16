## QR sharing and platform status — 16 November 2026

- Website: Next.js frontend and Spring Boot backend are implemented, with responsive mobile layouts.
- Android: no native Android project, APK or Play Store build exists. The Chrome extension is a separate desktop browser tool, not an Android app. A future Android client can reuse the Spring Boot API; mobile authentication, deep links, secure storage, device testing and release signing will need their own implementation.
- QR sharing: published registries show a locally generated 768 px PNG, a readable preview and Download QR code in Share registry. It encodes the short link plus `?via=qr`, and aggregate opens are attributed to QR code in Gift tracker → Link activity. No registry URL is sent to an external QR generation service.
- Revocation disables the printed QR link too. Republish and download a replacement after revoking. Counts have the same approximation limits as other short-link opens.
- Localhost QR codes are local-demo-only. A phone cannot reach this computer through a localhost URL. Use a deployed HTTPS app address before distributing invitations; no public hosting or tunnel was created.
- Renderer: [node-qrcode](https://github.com/soldair/node-qrcode) with medium error correction, four-module quiet zone, black modules and a white background. The download is deliberately free of decorative overlays for scanning.

# Short links, sharing and warmth — 16 November 2026

## Implemented

- First-party `/r/{code}` links: 22 URL-safe characters generated from 128 random bits. Existing published registries receive a code in migration V4; existing full links still work. This trades a few characters for resistance to guessing. A short production domain can further reduce length once a domain is chosen.
- Temporary 302 redirect with `Cache-Control: no-store`, no-referrer and noindex. Destination is always an internal registry, never a caller-provided URL. Revocation disables old short and full links; republishing generates new ones. Existing claims remain manageable.
- Owner-only Link activity section in Gift tracker: all-time opens and the last 30 days grouped by India calendar day and channel. WhatsApp uses `?via=whatsapp`; copied links use the direct/other bucket. These are self-reported link channels, not verified attribution.
- Atomic PostgreSQL aggregate increments. HEAD, prefetch and common preview/crawler user agents are excluded. Repeat visits, uncommon bots and abuse can inflate counts. These are approximate opens, not unique visitors or purchase conversion. Full-URL visits bypass these counts.
- No IP addresses, raw user agents, referrers, cookies or fingerprints are stored for analytics. Aggregate rows remain until the registry is deleted. Before deployment, configure infrastructure access-log retention separately and add edge rate limiting.
- Editable gentle WhatsApp message, warm ivory/plum/peach styling with decorative gold light, new homepage headline, gift-card transitions, and brief publishing/purchase celebration styling. Reduced-motion disables animations/transitions. The message is edited in the current page session, not persisted as registry settings.

## Family-feature priorities

| Feature | Current state / next step |
|---|---|
| Private planning / already have | Existing private checklist with explicit individual starter selection. Per-gift hiding within a published registry remains a separate feature. |
| Budget filters | ₹500, ₹1,000 and ₹2,500 available; host-configurable bands remain planned. |
| Occasions | Editable occasion wording and Indian occasion suggestions already available. |
| Buy locally | Guests can reserve and record purchases without a retailer link. |
| Size / timing | Variant fields available; structured delivery timing remains planned. |
| Family photograph / product imagery | Existing warm hero retained. Cover uploads and product-image import require storage, validation, resizing, deletion and permitted source handling; not implemented in this update. |
| Family co-hosts | Next separate milestone: invitation acceptance, membership roles, revocation and audit trails; never share host credentials. |
| Regional languages | Select pilot language with families, then translate the entire guest/verification/error journey. |
| Help as a gift | Planned separate service-gift type with time/quantity and clear arrangements. |
| First-year lists | Existing lists stay editable; carry-forward, birthday templates and opt-in reminders remain planned. |

## Approach references

Temporary redirects allow destinations to remain changeable: [MDN redirects](https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Redirections). `no-store` prevents stored responses: [MDN Cache-Control](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Cache-Control). Avoid tiny enumerable codes for unlisted family pages: [short-link enumeration research](https://www.cs.cornell.edu/~shmat/shmat_urls.pdf). No third-party shortener is necessary for this app.
