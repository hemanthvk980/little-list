# Retailer import audit — 23 November 2026

Read-only checks using Playwright and the real Java ProductPreview.fetch method. No registries were seeded and no retail orders were placed. Prices below are observations, not a catalogue or ongoing price guarantee.

## Amazon India

24 search-result products plus the user-supplied blanket were opened in the browser. All 25 exposed a title, main image and displayed price through the existing Amazon selectors. The extension extractor itself was separately executed on the user-supplied blanket and returned its name, ₹499.00 and high-resolution image. Variants were not systematically audited across all 25. Browser navigation took approximately 2.4–4.4 seconds per item.

Direct Java lookup returned empty metadata for all 24 batch products. This demonstrates that adding selectors alone cannot make arbitrary Amazon server imports reliable. The browser and backend receive different responses. No proxy rotation, cookies from the browser, or CAPTCHA bypass is used.

| Product | Browser price (INR) | Browser image | Backend metadata |
| --- | ---: | --- | --- |
| [B0BYPKFS7G](https://www.amazon.in/dp/B0BYPKFS7G) | 17574 | Present | Unavailable |
| [B082LDLPR9](https://www.amazon.in/dp/B082LDLPR9) | 737 | Present | Unavailable |
| [B0GYZF8Q93](https://www.amazon.in/dp/B0GYZF8Q93) | 449 | Present | Unavailable |
| [B0H83B4C1K](https://www.amazon.in/dp/B0H83B4C1K) | 281 | Present | Unavailable |
| [B0FL127853](https://www.amazon.in/dp/B0FL127853) | 6494 | Present | Unavailable |
| [B08F38HSMN](https://www.amazon.in/dp/B08F38HSMN) | 578 | Present | Unavailable |
| [B08QTSGVFG](https://www.amazon.in/dp/B08QTSGVFG) | 218 | Present | Unavailable |
| [B0FG1BYR6T](https://www.amazon.in/dp/B0FG1BYR6T) | 153 | Present | Unavailable |
| [B0HH9TKF5W](https://www.amazon.in/dp/B0HH9TKF5W) | 299 | Present | Unavailable |
| [B07DKJXSPK](https://www.amazon.in/dp/B07DKJXSPK) | 179 | Present | Unavailable |
| [B0GCFNX3QX](https://www.amazon.in/dp/B0GCFNX3QX) | 1399 | Present | Unavailable |
| [B0H2Z18JXB](https://www.amazon.in/dp/B0H2Z18JXB) | 2999 | Present | Unavailable |
| [B09QQPX12P](https://www.amazon.in/dp/B09QQPX12P) | 1885 | Present | Unavailable |
| [B0G1TKFH8M](https://www.amazon.in/dp/B0G1TKFH8M) | 130 | Present | Unavailable |
| [B0CV5RC9WV](https://www.amazon.in/dp/B0CV5RC9WV) | 569 | Present | Unavailable |
| [B0F63XS9CZ](https://www.amazon.in/dp/B0F63XS9CZ) | 349 | Present | Unavailable |
| [B07X34F9MF](https://www.amazon.in/dp/B07X34F9MF) | 205 | Present | Unavailable |
| [B091MDNR7D](https://www.amazon.in/dp/B091MDNR7D) | 229 | Present | Present |
| [B0F4RBPXRK](https://www.amazon.in/dp/B0F4RBPXRK) | 263 | Present | Unavailable |
| [B07N3RT753](https://www.amazon.in/dp/B07N3RT753) | 165 | Present | Unavailable |
| [B0GS5LBS9V](https://www.amazon.in/dp/B0GS5LBS9V) | 549 | Present | Unavailable |
| [B0F7GJKNKL](https://www.amazon.in/dp/B0F7GJKNKL) | 426 | Present | Unavailable |
| [B00AWMBLZ4](https://www.amazon.in/dp/B00AWMBLZ4) | 374 | Present | Unavailable |
| [B0C3DB3WQT](https://www.amazon.in/dp/B0C3DB3WQT) | 295 | Present | Unavailable |
| [B0FH79G2MW](https://www.amazon.in/dp/B0FH79G2MW) | 499 | Present; extension validated | Not in this batch (previous direct-fetch failure) |

## FirstCry and Flipkart

- FirstCry Chicco toothpaste [41848](https://www.firstcry.com/chicco/chicco-dentifricio-toothpaste-strawberry-flavour-50-g/41848/product-detail): title and image present. Fully loaded page contains an INR structured offer and `#seoprice` 138.33. Immediate extraction after navigation initially missed price. Added the exact decimal fallback, avoiding the nearby club-price value 135.15; rerunning the real extension extractor returned 138.33 and the product image.
- FirstCry Adore tongue cleaner [12487947](https://www.firstcry.com/adore/adore-candyswirl-tongue-cleaner-with-safety-case-colour-may-vary/12487947/product-detail): title and image present; initial browser extraction price empty. Partial results remain editable.
- Flipkart [PWI blanket](https://www.flipkart.com/pwi-cartoon-single-swaddling-baby-blanket-ac-room/p/itma88fc6b0177a2): browser extractor returned price 286, Blue variant and a `rukmini1.flixcart.com` image. That exact CDN host was missing from our app/backend allowlists and is now accepted. Prefer structured title when available to avoid a visually truncated heading.

## Repeating backend measurements

`ProductImportAuditTest` is opt-in and skipped in normal CI. Supply `-Dretailer.audit=true`, `-Daudit.input=<absolute JSON file>` and `-Daudit.output=<absolute output JSON file>`, with `-Dtest=ProductImportAuditTest`. Input is an array of objects with an `asin` field. Requests use the same host checks, limits and parser as production. Do not interpret successful browser extraction as a successful backend integration.

## Remaining integration gate

Reliable server import across blocked retailers needs an approved retailer data API/feed or another licensed provider and credentials. None is configured. The Chrome extension captures the product page the user has opened; unsupported/partial data stays editable. No historical price is substituted into a failed live lookup.

Brand browsing sources checked: [Chicco India](https://www.chicco.in/), [Mee Mee](https://meemee.in/), [Pigeon India](https://pigeon-in.com/), [R for Rabbit](https://rforrabbit.com/), and [FirstCry brand search](https://www.firstcry.com/search?q=Babyhug). No popularity ranking or partnership is asserted.
