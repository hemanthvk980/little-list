# Product previews and images

## Flow
Paste a full product link in Add a gift and click Review gift. Dashboard quick-add also links to this editor with the pasted URL. The server returns suggestions; review/edit all fields and explicitly save. Editing an existing gift offers Refresh product details, which replaces the review form's name, price, variant and image suggestions; it does not save automatically. No periodic refresh or scraping runs during guest visits.

The authenticated, CSRF-protected `POST /api/v1/products/preview` takes `{ "url": "https://..." }`. It returns name, price (INR decimal string), variant, imageUrl, retailerUrl and a user-facing message. Unsupported, unavailable or blocked pages return empty suggestions and manual-entry guidance. This is metadata extraction, not an official retailer API or affiliation. Never promise live price or inventory.

## Supported extraction
- Exact Amazon India, Flipkart and FirstCry hosts, with a small explicit set of their short-link hosts. HTTPS only; no arbitrary subdomains.
- A single JSON-LD Product, Open Graph metadata and conservative product-page selectors. Multiple products, multi-offer/aggregate prices and non-INR prices are not guessed. Variant-bearing query parameters are preserved.
- Product images from an explicit CDN allowlist (Amazon media/SSL image domains, rukminim1/2/3.flixcart.com, cdn.fcglcdn.com and cdn.firstcry.com). A manual image URL must use these hosts too.
- Local-beta Chrome extension now captures an image suggestion and passes it through to the app. Reload the unpacked extension after updating. Its APP_ORIGIN remains localhost:3000; set the verified production app origin before distributing it.

Retailer changes, CAPTCHA, JavaScript-only pages and hotlink restrictions can prevent extraction/display. No CAPTCHA or access-control bypass is implemented. Use manual fields or extension capture when needed. Availability is checked at the retailer; registry availability counts mean unclaimed gift units.

## Storage and delivery
V8 adds nullable gift.image_url; PostgreSQL stores a URL, not product photo bytes. Existing gifts remain unchanged. Images load directly in the browser, with no-referrer, lazy loading, fixed dimensions, contain sizing and a category-art error fallback. The retailer still sees the visitor's IP when serving its image. Only use images you have permission to display. No images are copied into storage.

R2/S3 and new product-image uploads are not enabled in this release. They require an object-storage account/configuration and a separate upload integration. Existing family-photo storage is unchanged. No storage credentials are needed for these retailer image references. Next Image remote optimization is intentionally not used: this avoids adding an arbitrary server-side image-fetch endpoint.

## Fetch safeguards and limits
Authentication and CSRF are inherited from Spring Security. Limit: ten preview attempts per session per minute, four concurrent previews per backend instance. Retailer URLs and every redirect use an exact host allowlist, HTTPS and public DNS-address checks; credentials and custom ports are rejected. At most four requests, three seconds per request, one MB of HTML per response. HTML scripts are parsed as data, never executed. No browser cookies or user authorization headers are forwarded. Responses are no-store; metadata is not logged. Rate/concurrency limits are instance-local; add shared account/IP quotas at the gateway before scaling replicas.

## Deployment
Deploy backend first (Flyway automatically applies V8), then frontend. Existing BACKEND_URL configuration must point at that backend. There are no new mandatory environment variables. Keep public retail access subject to retailer terms; approved API adapters can replace metadata extraction later.

## Verification
Parser fixtures cover all three stores, INR/foreign/ambiguous prices, malformed/multiple products, CAPTCHA/no data and unsafe URLs. Database integration covers image save, guest visibility, replacement/removal and unsafe host rejection. Extension tests cover image capture and handoff. Browser checks use synthetic fixtures to avoid polluting real registries. Live retailer compatibility remains best effort, not a contractual integration.
