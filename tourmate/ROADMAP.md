# Implementation roadmap

The original full plan is preserved in PLAN.md. This is the first working discovery/authentication increment, not a completed 28-step platform.

- [x] Separate frontend/backend projects and Docker development configuration.
- [x] MySQL + Flyway foundation.
- [x] JWT register/login and profile.
- [x] Destination and place read APIs, combined filtering and sample data.
- [x] React discovery, details, authentication and profile pages.
- [ ] Destination/place create, update and delete with administrator authorization.
- [ ] Place-detail DTOs for photos, specialized data, amenities and opening hours.
- [ ] Interactive Leaflet maps (current details link to OpenStreetMap).
- [ ] Owned review CRUD with atomic aggregate rating recalculation.
- [ ] Favorites.
- [ ] Trips, date-generated days and ownership checks.
- [ ] Manual schedule items, custom activities, conflict validation and reorder/move.
- [ ] Drag and drop and budget categories/totals.
- [ ] Spring AI + Ollama connection and structured requirement extraction.
- [ ] Database-backed AI tools and itinerary generation.
- [ ] Validate generated place IDs, dates, hours, distances, costs and ownership before saving through the manual planner services.
- [ ] Hybrid editing, optimization and replanning.
- [ ] Natural-language search and review summaries; embeddings only when needed.
- [ ] Weather/currency integrations and PDF itinerary export.
- [ ] Admin dashboard and review moderation.
- [ ] Expand tests for every new module; CI and production deployment hardening.

## Architecture decisions

Keep Ollama behind a dedicated AI service and a Spring AI provider adapter. Never accept model text as a saved itinerary without schema and domain validation. Manual and generated schedules must share the same trip/day/item persistence and validation. Do not send tokens or user credentials to the model. The initial version has no AI dependency or misleading AI buttons.

## Verification record

Static TypeScript syntax checks passed for 14 files, along with JSON/XML parsing and whitespace checks. Full builds, type checks and tests remain unexecuted because build tools/dependency downloads and Docker were unavailable. Test sources are not evidence that tests passed. Docker-backed MySQL integration tests require Docker, Java and Maven.
