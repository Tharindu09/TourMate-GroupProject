# TourMate architecture

The active application in `tourmate/` uses a layered Spring Boot backend and a React presentation layer. The older top-level `backend/` and `frontend/` projects are separate implementations.

## Backend

Sources live under `tourmate-backend/src/main/java/com/tourmate/`:

```text
com/tourmate/
├── TourmateApplication.java
├── controller/    # HTTP routes, request validation and response status/headers
├── service/       # Use cases, business rules and external provider orchestration
├── repository/    # Spring Data JPA database queries
├── model/         # JPA entities and domain enums
├── dto/           # Request/response records shared across layer boundaries
├── config/        # Security, JWT and CORS configuration
└── exception/     # Central HTTP error responses
```

Request flow:

```text
React view → frontend service → HTTP controller → backend service
                                                     ├→ repository → MySQL
                                                     └→ external API
```

Controllers inject services. They do not query repositories, hash passwords, issue JWTs or call external APIs. Authentication and profile operations belong to `AuthService` and `UserService`; destination searches belong to `DestinationService`. `PlaceService` handles local place searches. `TripService` owns manual schedules, account-scoped access, day generation, conflicts, ordering and budget totals; its records live in `Trip`, `TripDay`, `TripItem` and the trip DTOs. See [MANUAL_SCHEDULER.md](MANUAL_SCHEDULER.md). `GeoapifyService`, `RapidTravelService` and `PlacePhotoService` handle provider operations, normalization, limits and short-lived caches.

Repositories operate on entities in `model/`. Services return public request/response records from `dto/` where needed. Local catalogue endpoints retain their existing entity response shape. Account endpoints return `UserProfile`, never the persistence entity containing the password hash.

Spring Boot scans all layers from the root `com.tourmate` application package. Package moves do not change database table names, Flyway migrations, environment variables or HTTP paths.

To add an endpoint, define its request/response in `dto/`, implement its use case in `service/`, and expose it through a thin `controller/`. Add a repository query only when database access is needed. Keep framework configuration and exception handling in their existing layers.

## Frontend

Sources live under `tourmate-frontend/src/`:

```text
src/
├── api/client.ts    # Shared fetch transport, authorization header and HTTP errors
├── services/        # Auth, profile, catalogue, Geoapify and travel requests
├── models/          # TypeScript data contracts
├── pages/           # Route-level views
├── features/        # Feature views and React state (auth, travel, Geoapify)
├── components/      # Shared UI, cards, photographs and OpenStreetMap
├── utils/           # Formatting, safe links and travel display helpers
├── data/            # Bundled photograph metadata
├── App.tsx          # Routes and application layout
└── main.tsx         # React entry point
```

Views call domain functions in `services/` and render data from `models/`. Services use `api/client.ts`; they do not import React components. Display helpers belong in `utils/`. React components and hooks manage user interaction, loading and error states. React supplies the view layer; it does not duplicate Spring's MVC controllers in the browser.

For example, a map click calls `getTravelMap()` in `services/travelService.ts`, which calls `/api/rapidapi/map/{type}/{id}`. `RapidTravelController` delegates to `RapidTravelService`, which checks the search context and provider place ID before returning the `MapLocation` DTO. `PlaceMap` renders the OpenStreetMap marker.

## Verification

From `tourmate/tourmate-backend`:

```bash
mvn -Dtest=LayeredControllerTest,AccountServiceTest,RapidTravelServiceTest,GeoapifyServiceTest,PlacePhotoServiceTest,TripPersistenceTest,TripApiIntegrationTest,ManualSchedulePlannerTest clean test
mvn -DskipTests package
```

The focused suite covers controller routes, validation, security, extracted account logic and provider integrations with mocked dependencies/local HTTP servers. `ApiIntegrationTest` is the additional MySQL/Testcontainers suite and requires Docker access; run `mvn test` when Docker is available.

From `tourmate/tourmate-frontend`:

```bash
npm run build
npm test
```

After changing sources, rebuild containers from `tourmate/`:

```bash
sudo docker compose up -d --build backend frontend
```

Guided manual planning uses `ManualTripPlanController` → `ManualTripPlanService` → `ManualSchedulePlanner` (pure arrangement rules) and `TripService` (owned persistence). The preview is recalculated on save, and creation is transactional. User preferences are stored alongside the trip via migration V7.

Route-aware planning separates `GeoRoutingService` and `PlanLocationService` from `ManualSchedulePlanner`. `PlanNearbyService` locates recommendations along the estimated itinerary. `TripPlanWriter` persists the completed external-data preview atomically; external requests happen before that transaction. `RouteMap` draws provider geometry and uses OpenStreetMap tiles only when opened.

## AI scheduling layer

`AiTripPlanController` → `AiTripPlanService` → `AiScheduleAdvisor` (Spring AI/Ollama), `GeoapifyService`, `PlanNearbyService` and `ManualSchedulePlanner`. Generated IDs are grounded to real candidates before routing. `TripPlanWriter` persists verified drafts using the shared repositories and marks AI provenance. The model never writes directly to the database or supplies trusted coordinates. See [AI_SCHEDULER.md](AI_SCHEDULER.md).

### Automatic AI planning flow

`GuidedTripPlanner` in AI mode collects basics, then a structured questionnaire (interests, pace, dietary/accessibility needs, meals and overnight hotels). It submits an empty-stop plan plus preferences to `/api/trips/ai/preview`; manual venue selection is optional through draft adjustment. `AiTripPlanService` grounds visits in actual provider candidates, uses route-based meal options, optionally selects one hotel near each overnight endpoint, and rechecks the complete itinerary with `ManualSchedulePlanner`. Model output supplies choices and estimated visit durations; map services supply travel estimates, and server validation controls feasibility and saving. Unknown hours, costs, suitability and availability remain explicit uncertainties. The manual planner keeps its separate selection workflow.
