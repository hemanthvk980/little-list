# Little List Chrome button — local beta

Load this directory as an unpacked Manifest V3 extension in Chrome. The popup defaults to the live app at https://little-list-30e7.onrender.com. Choose Local development to use http://localhost:3000. No Web Store publishing or installation has been performed by the implementation task.

Click the action, then **Read this product** on a product page. Review suggestions, then continue into the app to select a registry and save. Login uses the regular app; the extension receives no host cookies or tokens. Pending captured details survive the login redirect in app sessionStorage and are removed after saving.

Permissions are only activeTab and scripting. There are no host permissions, cookies/history/storage permissions, background monitors, network scraping, remote executable code, or analytics. Data is read on explicit click and handed to the selected fixed app origin only on submission. The URL fragment avoids HTTP access logs; it is cleared by the app after import. Product image URLs are captured as suggestions and filtered through the app CDN allowlist; image bytes are not copied. Use only images you have permission to display.

Suggestions use bounded JSON-LD Product data, Open Graph title and INR price metadata, then optional retailer selectors for Amazon India, Flipkart and FirstCry. Retailer selectors can change. Arrays of offers, unknown/non-INR prices and missing data are left for manual review. No retailer order, availability or live-price synchronization is implemented. No third-party access controls are bypassed.

Before release: review the production APP_ORIGIN in popup.js, verify current retailer pages and selected variants, test unpacked Chrome behavior end-to-end, add store artwork and privacy/support URLs, review permissions/disclosures, and submit through the Chrome Web Store. The current extension has fixture tests but is not certified against every live retailer layout.

Run fixture tests from the project root: `node --test scripts/extension.test.mjs`.

## Updating an existing registry gift
Reload this unpacked extension in chrome://extensions after pulling code changes. Open the product, click Read this product and Review in Little List. Select your registry. If the product is already listed, choose Review update to existing gift; then explicitly save. If no duplicate is found, you can add it as a new gift after review. No registry is changed by reading the product page.
