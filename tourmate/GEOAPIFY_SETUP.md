> Current behavior: searches accept worldwide destinations. The earlier Sri Lanka-only geocoding filter and result restrictions have been removed. Include a region or country in ambiguous names.

# Geoapify setup

TourMate now uses Geoapify for live place discovery. The previous Google Places integration has been removed; no Google key or Google billing account is needed.

## Get a key

1. Sign up at https://myprojects.geoapify.com/ .
2. Create a project named **TourMate**.
3. Open **API Keys** and copy the generated key. Geoapify provides a free plan without a credit card; check https://www.geoapify.com/pricing/ for current credit and rate limits. A credit is not necessarily the same as one request.
4. Optionally restrict the key to your backend's public outbound IP if it is stable. Browser referrer restrictions are not suitable for requests sent by Spring Boot. Never paste the key into chat, commit it, or put it in a frontend `VITE_` variable.

## Configure and run

Add this line to the existing `tourmate/.env`, keeping the database and JWT settings:

```env
GEOAPIFY_API_KEY=your_actual_geoapify_key
```

A leftover `GOOGLE_PLACES_API_KEY` entry can be removed; the app does not use it.

```bash
cd ~/PycharmProjects/TourMate-GroupProject/tourmate
sudo docker compose up --build -d
```

Open http://localhost:5173/search. Select **Geoapify places**, enter **Ella**, choose **Restaurants & cafes**, and press **Search places**. You can narrow results with a place-name filter or change the radius. Open a result to view available contact information, opening hours and an OpenStreetMap location link. Attribution links appear on both pages.

If running Maven locally, export `GEOAPIFY_API_KEY` in the backend process environment alongside `JWT_SECRET`. Spring does not load Compose's `.env` automatically. Changing a Docker environment variable requires recreating the backend, not just restarting it.

## Behavior and data

- An initial search geocodes the destination with a Sri Lanka country filter, then queries Places with category, circular radius and proximity ordering. Usually this is two provider requests. Subsequent pages reuse the returned coordinates and make one Places request. Opening details makes one Place Details request, unless the same place is already in the 15-minute details cache. Photo lookups fetch a place detail record when needed; this can add one Geoapify request for each newly visible card. Wikimedia requests do not use the Geoapify key.
- Categories map to accommodation/hotels, restaurants/cafes, tourist attractions, or nature/outdoor places. The optional place-name field uses Geoapify's `name` parameter. This is structured geographic search, not Google-style natural-language search.
- The API returns twelve places per page. There is no total result count. A full page enables Next; the last Next may return an empty page. Previous remains available.
- A process-wide 120 requests/minute guard and connection/read timeouts limit accidental repeated calls. Keep the provider free plan's credit/rate limits in mind; this guard is not a daily-credit budget or distributed production rate limiter.
- Missing names, contacts and hours are shown appropriately; results without usable coordinates/IDs are not displayed. Known foreign-country records are excluded from the Sri Lanka search.
- Geoapify data is normalized into a small DTO; raw provider fields and API keys are not sent to the browser. No provider requests run while typing. Images are requested only for visible/nearby cards with at most three simultaneous lookups. Queued frontend photo requests are cancelled when their views unmount. Positive and missing-photo results are cached for 30 minutes in bounded server memory; transient provider failures are not cached. Detail records are cached for 15 minutes to share work between photo and detail pages. Results are not automatically saved to MySQL. Automated tests use synthetic credentials; a small live metadata check was also completed without displaying the configured key.
- Geoapify does not supply Google-style customer ratings/reviews or live prices. Live photos now use a general per-place lookup: fetch that place’s detail media references, resolve an exact Commons file, linked Wikidata P18 image, or linked Wikipedia lead image, and retrieve Commons attribution/license metadata. There is no city/name/coordinate photo table or destination-scenery fallback for live listings. Unsupported or missing media shows Photo unavailable. Curated sample pages keep their existing local photographs. TourMate’s own reviews remain planned.
- Raw opening-hours notation is displayed without claiming to interpret current open/closed status. Confirm with the venue.

## Endpoints

- `GET /api/geoapify/search?q=Ella&type=RESTAURANT&radius=10000&name=&page=0`
- `GET /api/geoapify/search?q=Ella&type=RESTAURANT&radius=10000&page=1&lat=6.8667&lon=81.0466`
- `GET /api/geoapify/details/{geoapifyPlaceId}`
- `GET /api/geoapify/photos/{geoapifyPlaceId}` — returns `{url, sourceUrl, author, license, licenseUrl}`; a missing image returns null fields.

Search responses contain `places`, `page`, `hasMore`, `latitude`, `longitude` and `location`. Supported type values are empty, `HOTEL`, `RESTAURANT`, `ATTRACTION` and `ACTIVITY`; radius is 1,000–50,000 metres and page is 0–99. Keep all submitted filters and coordinates the same while paginating. Both coordinates must be supplied together. The backend handles missing keys with 503, unknown destinations with 404, invalid inputs with 400, quota problems with 429 and upstream errors with sanitized 502 responses.

## Troubleshooting

- **Not configured:** add `GEOAPIFY_API_KEY` and recreate the backend container.
- **Key rejected:** check the copied key and backend IP restrictions in Geoapify MyProjects.
- **Destination not found:** enter a Sri Lankan town rather than a sentence such as `cheap restaurants in Ella`. Use the separate category/name inputs.
- **No places:** increase the radius, clear the optional name or try another category. OpenStreetMap coverage varies by area.
- **Quota reached:** wait or inspect provider usage; repeated searches consume credits. Do not configure automated retry loops.

## Verification

```bash
cd tourmate/tourmate-backend
mvn clean package '-Dtest=GeoapifyServiceTest,PlacePhotoServiceTest'
cd ../tourmate-frontend
npm run build
npm test
```

Twenty backend tests use local fake servers for Geoapify search/details and dynamic photo references, attribution, cache behavior, unsupported/missing media, URL validation and retry behavior. Nine frontend tests cover the API client, submit-only requests, pagination, lazy photos, attribution and unavailable images. Backend packaging and the frontend production build passed. The existing database integration suite still requires Docker/MySQL and was not run. A live Geoapify detail response was checked and had valid coordinates/ID but no linked media. The complete photo pipeline is covered by fake-server tests; images for arbitrary real listings depend on media coverage.

Provider references: https://apidocs.geoapify.com/docs/places/ and https://apidocs.geoapify.com/docs/place-details/ .

## Dynamic photo sources

Photo lookup uses the `wiki_and_media` fields documented by Geoapify. It follows only the place’s explicit reference; it does not perform name searches, nearby-image searches or scrape a venue website. Commons `File:` references and Commons-hosted image URLs are supported, as are Wikidata `Q…` identifiers and `language:Article` Wikipedia references/standard article URLs. Commons categories are not treated as venue photos. Images from arbitrary external website URLs are not automatically embedded.

Only Commons raster images with licensing metadata and valid Wikimedia image/source hosts are returned. The UI displays author, license and original source links alongside each photograph. No extra key is needed for Wikimedia. Coverage depends on community media references; it cannot guarantee a photo for every accommodation or restaurant. Missing photos remain explicit rather than showing a different place.

Provider metadata endpoints have fixed hosts and connection/read timeouts. This feature does not proxy arbitrary source URLs or store image binaries. Wikimedia receives the browser's image request. The automated test suites use synthetic keys; the separate live metadata check did not display or persist the real key.

### Canonical place IDs

Geoapify may return a canonical `place_id` in its primary `feature_type: details` record that differs from the original search result ID. The backend selects that primary feature and retains the request ID for cache/UI identity; nearby-place features are never substituted. This fixes a live-confirmed case where Trinco House details existed but the old equality check incorrectly returned 404. That record had no media references, so its photo response now correctly reports absence rather than a failed lookup. Regression tests cover both canonical IDs and rejection of nearby-place features.
