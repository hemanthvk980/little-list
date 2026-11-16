# Registry experience and integration roadmap — 16 November 2026

## Implemented in this update

Profile-photo upload/removal in registry settings and a clickable sidebar avatar. Only JPEG/PNG, up to 5 MB and 16 million pixels; images are decoded, resized to a maximum 512 px edge and re-encoded as JPEG without original metadata. The bounded result is stored in PostgreSQL and deleted with the registry. Owner reads/uploads require ownership and uploads require CSRF. Guest photos resolve only through a currently published share ID; responses are no-store. This is a registry profile photo, not a global account avatar. Camera orientation/crop editing is not yet provided.

Dashboard and registry quick-add forms save products without leaving the page. Hosts choose the registry and category, paste a retailer link, and enter INR estimate, variant, quantity and note. Editable gift ideas include retailer browsing shortcuts; they are explicitly labelled as ideas, not live retailer listings. There are 15 checklist suggestions and an expanded category list. Gifts are grouped by category, the desktop sidebar has category links, and checklist sections show private decision progress. Quick-add does not fetch retailer data; the dedicated product editor retains duplicate-link review.

Guests can choose I purchased this directly on a gift, enter an email, open the verification email, and explicitly confirm. A verified request records one purchased unit atomically, without an intermediate reservation. Existing reservation users can still record a purchase from their private claim page; same-browser users are routed back to it. Retries do not double-count. Purchased tags appear in host and guest gift cards; partial quantities show the purchased count. Visible pages refresh every 15 seconds and on focus. Purchase status is guest-reported, never a verified retailer order.

Spring Boot starter-mail and JavaMailSender were already installed. Both reservation and purchase confirmations now use that integration, with a 30-minute email verification link. Local Mailpit captures all mail. Production sender credentials, DNS verification and SMTP delivery testing remain required. No order number, address or marketing subscription was added merely because it appeared in the reference screenshots.

QR downloads and QR-channel tracking are included from the preceding update. Website code exists; native Android code/APK does not.

## Retailer catalogue integration

| Retailer | Verified official option | What is needed |
|---|---|---|
| Amazon India | [Creators API](https://affiliate-program.amazon.in/creatorsapi/docs/en-us/introduction), including search, item and variation operations | Approved Associates/API access and credentials. Observe current programme eligibility and content terms. |
| Flipkart | [Affiliate product APIs](https://affiliate.flipkart.com/api-docs/af_prod_ref.html) and [programme overview](https://affiliate.flipkart.com/api-docs/af_overview.html) | Approved affiliate credentials and verification that the account can use the requested feeds/search. |
| FirstCry | No official public product API was verified in this research. [Official terms](https://www.firstcry.com/termsofuse) describe content ownership. | Arrange a permitted partner feed or continue user-entered links. Do not assume catalogue/image reuse is granted. |

Recommended implementation after access is available: a server-side RetailerCatalog interface with per-provider adapters returning stable product IDs, permitted images, INR prices, variant URLs, stock and fetched timestamps. Normalize and cache only within each provider's current rules, handle quotas/timeouts, keep credentials on the backend, and add affiliate disclosure when applicable. Use those approved data objects to render native product cards with Add to registry. Do not iframe entire store pages or invent live prices. Failed providers should leave manual URL entry available.

## Spring AI last: a bounded recommendation layer

The current app uses Spring Boot 3.5.x. The [official compatibility table](https://github.com/spring-projects/spring-ai/blob/main/README.md) pairs it with Spring AI 1.1.x; the 2.x line targets Boot 4.x. Pin a compatible released 1.1.x BOM when implementation begins. Do not blindly copy the latest unversioned documentation into this app.

Relevant primary sources:

- [Official Spring AI examples](https://github.com/spring-projects/spring-ai-examples), especially the chat hello-world examples, for configuration and the model-client boundary.
- [Spring AI 1.1 ChatClient](https://docs.spring.io/spring-ai/reference/1.1/api/chatclient.html) for a typed client and response mapping.
- [Spring Boot email integration](https://docs.spring.io/spring-boot/reference/io/email.html) for the existing starter-mail/JavaMailSender configuration and timeouts.

Start by ranking an approved catalogue or the finite checklist, not by asking a model to invent products. Pass only optional broad preferences (budget band, category and requested sizes), available product IDs and already-have/selected IDs. Do not send family photos, email addresses, personal notes, pregnancy details or share/claim credentials. Return a typed object such as `RecommendationIds(List<String> productIds)` using ChatClient `.call().entity(RecommendationIds.class)`, then validate every ID against the supplied candidates and reapply budget/availability filters in Java. The backend supplies all displayed titles/prices/URLs. Model output must never mutate a registry or override purchase inventory.

Introduce RecommendationService with a deterministic fallback first; configure an optional Spring AI provider only after provider/model, credentials, budget and data handling are chosen. Add bounded retries/timeouts, per-user quotas, opt-in UI and tests for malformed output, fabricated IDs, prompt injection in catalogue content, missing credentials and provider failure. Offer suggestions with explicit Add controls. No agent tools, autonomous purchases or vector database are needed for the first version.

AI integration is researched and designed here, but not enabled or falsely presented as working. Current gift ideas are deterministic. No provider credentials or live retailer API integrations were supplied.
