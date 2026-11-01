# TourMate rebuild

A separate implementation of the attached plan. The existing root `frontend/` and `backend/` are untouched.

## Implemented first increment

- Java 21 / Spring Boot, feature-based packages, MySQL and Flyway (`ddl-auto: validate`).
- Registration, BCrypt password hashing, signed one-hour JWTs, protected profile read/update, stateless security and explicit CORS origin.
- Public paginated destination search and place discovery; destination, category, minimum rating, maximum estimated cost and name filters combine.
- Base place schema plus hotel, restaurant, attraction, photo, amenity and opening-hours tables.
- Seven destinations and ten example places. Example prices are in LKR; no invented review ratings.
- React / TypeScript / Vite / Tailwind / TanStack Query: home, search, destination, place, register, login, profile and 404 pages. Responsive CSS landscape illustrations are placeholders for destination photography.
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

TypeScript syntax transpilation passed for all 14 TypeScript/TSX files using the editor's embedded compiler. JSON, Maven XML and whitespace checks passed. These are static checks only. Full TypeScript type checking, the Vite production build, Vitest and Maven/Testcontainers tests were not run: standalone Node/npm, Maven and Docker are unavailable, and temporary dependency downloads could not connect. A bundled editor JDK was found, but that does not supply Maven or MySQL. No lockfile was generated because package installation could not run.

Version references: [Spring Boot 3.5 Java/build requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html) and [Vite setup documentation](https://vite.dev/guide/).
