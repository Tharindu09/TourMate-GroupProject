# TourMate rebuild


See [ARCHITECTURE.md](ARCHITECTURE.md) for the layered backend, frontend responsibilities and verification commands. The [manual scheduler](MANUAL_SCHEDULER.md) is available under **My trips → Plan a trip** (`/trips/plan`), with a four-step flow for user-selected visits, meals and preferences, map-based travel times and route-based restaurant/hotel options, automatic suggestions for added meals and optional travel-reducing visit reordering.
A separate implementation of the attached plan. The existing root `frontend/` and `backend/` are untouched.

## Implemented first increment

- Java 21 / Spring Boot, feature-based packages, MySQL and Flyway (`ddl-auto: validate`).
- Registration, BCrypt password hashing, signed one-hour JWTs, protected profile read/update, stateless security and explicit CORS origin.
- Public paginated destination search and place discovery; destination, category, minimum rating, maximum estimated cost and name filters combine.
- Base place schema plus hotel, restaurant, attraction, photo, amenity and opening-hours tables.
- Seven destinations and ten example places. Example prices are in LKR; no invented review ratings.
- React / TypeScript / Vite / Tailwind / TanStack Query: home, search, destination, place, register, login, profile and 404 pages. Locally bundled Wikimedia photographs appear on destination cards, place cards and detail pages, with author/license credits at `/photo-credits`.
- Swagger UI, Docker development setup and integration test sources.

## Run with Docker

Requires Docker Engine with Compose. Ports 5173, 8080 and 3307 must be free.

```bash
cd tourmate
cp .env.example .env
# Set JWT_SECRET in .env to a random value of at least 32 bytes.
docker compose up --build
```

Frontend: http://localhost:5173 · API: http://localhost:8080/api/destinations · Swagger: http://localhost:8080/swagger-ui/index.html

The database uses its own named volume and port 3307 to avoid the legacy database. Stop with `docker compose down`; this preserves data. Credentials in `.env.example` are for local development.

## Run locally

Requires Java 21, Maven 3.6.3+, Node 22.12+ and MySQL 8.4 (or run only MySQL using Compose).

```bash
cd tourmate
cp .env.example .env
# Set JWT_SECRET in .env.
docker compose up -d mysql
set -a
. ./.env
set +a
cd tourmate-backend
mvn spring-boot:run
```

In a second terminal:

```bash
cd tourmate/tourmate-frontend
npm install
npm run dev
```

The Vite proxy forwards `/api` to localhost:8080. Backend defaults use port 3307 and the example DB credentials; override `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ORIGIN` and `JWT_SECRET` as needed. `.env` is loaded by Compose, not automatically by Spring Boot.

## Checks

```bash
cd tourmate/tourmate-backend
mvn test
# Requires Docker: Testcontainers starts an isolated MySQL database.
cd ../tourmate-frontend
npm install
npm test
npm run build
```

Backend integration tests cover migrations/startup, register → login → protected profile, invalid credentials, duplicate accounts, token tampering, role update protection, combined place filters, pagination validation and missing records.

## Authentication

The UI keeps its access token in memory. Reloading the page requires signing in again; this avoids storing bearer tokens in localStorage. Logout clears the in-memory token and query cache. No refresh-token flow yet. Expired tokens receive 401; sign in again. There is no default administrator or seeded password.

## API

| Method | Route | Access |
| --- | --- | --- |
| POST | `/api/auth/register`, `/api/auth/login` | Public |
| GET, PUT | `/api/users/me` | Bearer JWT |
| GET | `/api/destinations`, `/api/destinations/search?q=ella` | Public |
| GET | `/api/destinations/{id}` | Public |
| GET | `/api/places?destinationId=1&type=HOTEL&maxPrice=15000&minRating=0&q=` | Public |
| GET | `/api/places/{id}` | Public |

List responses use `{content, totalElements, totalPages, number, size}`. Pagination starts at zero and size is limited to 100. Place `maxPrice` compares `estimatedCost` in LKR, not the 0–4 `priceLevel`. Monetary examples do not imply per-person or per-night totals.

## Next increments

See [ROADMAP.md](ROADMAP.md) for the remaining implementation sequence and [PLAN.md](PLAN.md) for the original plan. AI is deliberately deferred until the manual schedule services exist. Specialized place tables are schema foundations; their editing and detail APIs remain future work.

## Verification in this workspace

The latest Geoapify increment is verified with backend packaging, twenty focused backend tests against a local fake HTTP server, the frontend production build and nine frontend tests. JSON/XML and whitespace checks have passed. All twelve curated photographs remain bundled with their attribution records. The full MySQL/Testcontainers database suite was not run in this increment. A live Geoapify metadata check completed without displaying the key; that record had no media references.

Version references: [Spring Boot 3.5 Java/build requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html) and [Vite setup documentation](https://vite.dev/guide/).

## Database startup readiness

Compose waits for an authenticated TCP query against the application database before starting the backend. This avoids treating MySQL's temporary initialization server as ready. Flyway also retries its initial connection up to ten times.

After updating the Compose configuration, run `sudo docker compose up --build` from this folder. Existing database data is preserved. If a container still exits, inspect `sudo docker compose logs --tail=100 backend mysql`.

## Discovery photographs

Twelve photographs are bundled in `tourmate-frontend/public/images/`, so the UI does not depend on external image hosts at runtime. Sources, authors and licenses are recorded in `public/photo-credits.json` and displayed at `/photo-credits`. Images are resized and cropped in the interface. Demo hotel/restaurant listings explicitly show Ella scenery, not venue photographs.

Flyway V5 adds photo paths to seeded records with no existing cover image. Existing migrations and custom cover images are preserved. The UI also supplies matching local photos while an older backend is running. Broken cover URLs fall back to the matching bundled image, then a clear unavailable state. Rebuild with `sudo docker compose up --build -d` from `tourmate/` to update the Docker frontend and apply V5.

## Geoapify integration

Live search is available in Discover → **Geoapify places**. See [GEOAPIFY_SETUP.md](GEOAPIFY_SETUP.md) for key creation, configuration and test commands. Configure `GEOAPIFY_API_KEY` in `.env`, then rebuild with Compose. Search a Sri Lankan destination, category, radius and optional place name. Detail pages show available location/contact/opening-hours data with Geoapify/OpenStreetMap attribution.

The MySQL catalog remains accessible under **TourMate sample places** and retains its photographs. The former Google integration has been removed. Google keys are no longer forwarded or used. Geoapify results have no invented ratings, reviews, LKR costs or venue photographs; results are not automatically imported into MySQL.

### Photos on live search results

Live listing photographs are resolved dynamically from each place's own Geoapify media references, through Commons files, Wikidata P18 or linked Wikipedia articles. There is no per-city/per-place image map or area-photo substitution on live listings. Author, license and source links are displayed. Cards load photos near the viewport; requests are limited to three at once, and results/details use bounded short-lived caches. Extra initial detail requests consume Geoapify credits. Places without supported media show Photo unavailable; photos for every venue are not guaranteed. See [GEOAPIFY_SETUP.md](GEOAPIFY_SETUP.md) for the lookup flow and its limits.

## Interrupted Maven downloads during Docker builds

An error such as `Premature end of Content-Length delimited message body` means a dependency download was cut off. The backend Dockerfile keeps Maven's repository in a BuildKit cache and allows up to three build attempts, retrying previously missing artifacts. Completed downloads survive failed builds and source changes. Use the normal build command; `--no-cache` is not needed to apply source edits.

```bash
cd tourmate
sudo docker compose build backend
sudo docker compose up -d backend frontend
```

The first build with the new Maven cache still needs to populate it. If downloads continue to fail, retry after the connection stabilizes. This change does not remove database volumes. The updated Docker build could not be executed from the assistant session because Docker requires the user's sudo password.

## AI scheduler

Use **My trips → Plan with AI** for grounded place selection, AI visit-duration reasoning, route-aware meal choices and a checked itinerary preview. Spring AI connects to Ollama; install the configured model before generating. See [AI scheduler setup and limits](AI_SCHEDULER.md).
