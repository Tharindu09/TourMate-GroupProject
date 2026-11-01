# Manual trip scheduler

Open **My trips → Plan a trip**, or go directly to `/trips/plan`. Sign in to save schedules to your account.

## Guided planning

1. **Trip basics:** enter your starting location, main areas, dates, group size, budget, transport, default daily hours and an optional extra buffer between stops. Choose the area for each day in the next step.
2. **Choose stops and meals:** TourMate loads suggestions around the selected daily area. Select the visits you want. Switch to restaurants and choose breakfast, lunch or dinner to select a meal option. You can also name a custom visit or meal stop. Data sources are explicit: Travel Advisor, Geoapify, or the stored TourMate sample catalogue. Provider failures are shown; sample places are never substituted silently.
3. **Visit preferences:** supply each stop's duration, estimated LKR group cost, notes and optional fixed visiting time. Meals require a chosen start time. Adjust daily hours, move stops to another day and prioritize flexible visits using the up button.
4. **Review schedule:** TourMate reserves fixed meal/visit times and fits flexible visits into the remaining gaps in your chosen priority order, using map-based travel times and your extra buffer between stops. Review totals and warnings before saving.

Stops that do not fit are listed with reasons and retained in your selections. Saving is disabled until those conflicts are resolved; the backend repeats this check. The complete trip and its items are then saved atomically, so an invalid selected place cannot leave a half-created trip.

This is deterministic arrangement of user-selected inputs, without AI. Geoapify/OpenStreetMap routing provides directed travel estimates for driving, walking, cycling or public transport. The first journey starts at the entered address or optional map pin; later days start at the preceding day's endpoint. Select an overnight hotel as the last stop if that is where you will stay. The buffer is additional time, never a replacement for travel. Public transport requires provider route coverage; timetables and live departures are not guaranteed and there is no driving fallback. Venue opening hours, dining access, dietary suitability and hotel availability must be confirmed. External prices are not converted into LKR.

Meal labels, the starting location, daily areas and original planning preferences are retained with the saved schedule. Existing schedules remain editable through the timeline. **Create empty trip** is still available for users who prefer to build everything directly in the editor.

## Editing a schedule

- **Edit** changes an activity's name, day, category, times, cost and notes.
- Drag activities within a day to reorder them or onto another day to move them. Moving preserves the time range; overlapping moves are rejected.
- The up/down buttons offer keyboard reordering. Use **Edit → Day** to move an activity without dragging, including on touch devices.
- **Remove** deletes an activity. Deleting an entire trip requires confirmation.
- **Edit trip** changes metadata, destinations and dates. Dates with activities cannot be removed until their activities are moved or removed. Extending a trip creates the extra days; existing days and activities keep their IDs.
- Budget totals and category subtotals update after every saved change. Costs are total estimated costs for the group, not per traveler. A negative remaining balance is shown as an over-budget amount.
- Saved activities with coordinates can display their OpenStreetMap marker.

## Rules

Trips contain 1–60 days, 1–100 travelers and up to 20 selected destinations. A day supports at most 100 activities. An activity needs a name, category, start/end time and non-negative cost. The end must be after the start, and times cannot overlap another activity in the same day. Adjacent activities are allowed. Split overnight activities into separate days. Times use the planning clock entered in the forms; timezone changes require user-adjusted preferences. Route durations are elapsed minutes, not automatic timezone conversions.

External places are saved as user-selected snapshots, including their provider ID and coordinates when supplied. Provider prices are not automatically converted into LKR; enter your own estimated LKR cost. The planner does not estimate travel time or verify opening hours.

## Architecture and storage

The scheduler follows [ARCHITECTURE.md](ARCHITECTURE.md):

- `controller/TripController.java`: authenticated HTTP endpoints.
- `service/TripService.java`: ownership, dates, conflicts, ordering and totals.
- `repository/TripRepository.java`, `TripDayRepository.java`, `TripItemRepository.java`: database operations.
- `model/Trip.java`, `TripDay.java`, `TripItem.java`: persistence entities.
- `dto/Trip*.java`: validated input and response contracts.
- Frontend `features/trips/`: trip list, forms and timeline.
- Frontend `services/tripService.ts` and `models/trip.ts`: API calls and types.

Flyway migration `V6__create_manual_trip_scheduler.sql` creates `trips`, `trip_destinations`, `trip_days` and `trip_items`. Migration V7 adds saved guided-planning preferences and meal labels; existing activities default to no meal label. Existing data is retained. Trip mutations lock the owned trip row, serializing writes that might otherwise bypass overlap checks. Foreign keys cascade trip deletion to its days and activities.

Every endpoint requires authentication. Trip reads and writes are scoped to the JWT subject; another user's trip or day returns 404. Client-supplied owner IDs are never used.

## API

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/trips?page=0&size=12` | Paginated account trip summaries |
| POST | `/api/trips` | Create a trip and its days |
| GET | `/api/trips/{id}` | Full schedule and budget totals |
| PUT | `/api/trips/{id}` | Edit trip metadata and dates |
| DELETE | `/api/trips/{id}` | Delete trip, days and activities |
| POST | `/api/trips/{id}/items` | Add an activity |
| PUT | `/api/trips/{id}/items/{itemId}` | Edit or move an activity |
| DELETE | `/api/trips/{id}/items/{itemId}` | Remove an activity |
| PUT | `/api/trips/{id}/days/{dayId}/order` | Save a complete list of activity IDs in order |
| PUT | `/api/trips/{id}/items/{itemId}/move` | Move to a day/position while retaining times |

## Run

From `tourmate/`:

```bash
sudo docker compose up -d --build backend frontend
```

The backend applies migrations V6 and V7 automatically. Open `http://localhost:5173/trips`.

## Verification

The scheduler has database-backed service tests (`TripPersistenceTest`) and full HTTP/authentication tests (`TripApiIntegrationTest`). They run all Flyway migrations on an isolated H2 database in MySQL compatibility mode and verify committed persistence, ownership, validation, overlap rollback, editing, ordering, moves, totals and cascade deletion. These tests do not use the development database.

```bash
cd tourmate-backend
mvn -Dtest=TripPersistenceTest,TripApiIntegrationTest,ManualSchedulePlannerTest test
cd ../tourmate-frontend
npm test
npm run build
```

Frontend tests cover creating trips, custom activities, rejected saves, discovered-place prefilling, keyboard reordering, moving via edit, confirmation and signed-out access. Browser checks use mocked API responses to verify the rendered desktop/mobile layout and native drag-and-drop interaction.

The existing `ApiIntegrationTest` uses real MySQL/Testcontainers and requires Docker access. H2 compatibility tests do not replace that MySQL-specific verification.

## Guided planner implementation

`ManualSchedulePlanner` contains the deterministic arrangement rules. `ManualTripPlanService` orchestrates atomic persistence through the existing trip services. `ManualTripPlanController` exposes authenticated `POST /api/trips/plan/preview` and `POST /api/trips/plan` endpoints. `ManualPlanRequest` and `ManualPlanPreview` define validated contracts. `GuidedTripPlanner.tsx` owns the four-step view, while `manualPlanService.ts` contains its HTTP calls.

A guided plan supports up to 30 selected stops per day, visit durations of 5–720 minutes, fixed preferences at minute precision, and a 0–240 minute allowance between stops. Keep preferred meal times inside each day's travel hours. When flexible visits have no free slot, the planner reports them instead of moving them to another day without the user's choice.

## Worldwide destination lookup

Travel Advisor uses its live autocomplete destination IDs and coordinates, without city-ID mappings or a fixed country filter. Geoapify search and result mapping accept worldwide locations. Saved activity coordinates use global latitude/longitude bounds; migration V8 relaxes the earlier regional constraint without changing existing data. OpenStreetMap name searches use only the selected name/address. The bundled TourMate catalogue is still explicitly labelled as sample data. Trip budgets remain expressed in LKR, independently of the selected destination. Provider coverage determines which places are returned.

## Route-aware scheduling and nearby venues

In trip basics, select driving, walking, cycling or public transport. Enter a specific starting address, or use the optional latitude/longitude pin. The route map shows the resolved start so it can be checked. Custom name-only stops cannot be routed: provide their coordinates or select a real venue.

In **Visit preferences**, use **Meal and hotel options along this day's route**. Choose a restaurant or hotel, meal/check-in purpose and time. The backend estimates the location at that time from scheduled visits, waiting periods or progress along a mapped travel leg. Progress along travel uses provider step timings and route geometry; it is an estimate, not GPS tracking. Venue searches are centered at that position, rather than the destination town. Returned options show their route travel time and distance in the selected mode. Choose an option and rebuild to validate the complete detour and fixed arrival time.

Unlocated custom placeholders are ignored only while searching for nearby choices, with a warning. They remain unscheduled in the final preview until replaced or pinned. Unavailable routes and provider failures never fall back to guessed speeds, straight-line durations or a different transport mode.

The preview lists journey departures, arrivals, distances and durations. **View route on map** draws the road/transit geometry and stop markers on an OpenStreetMap/Leaflet map. Saving stores timed `TRANSPORT` activities alongside visits and meals, and retains the route snapshot in planning preferences. Saved maps are marked stale after edits to the schedule rather than claiming the old estimates are still current.

Routing and coordinate lookups use the existing server-side `GEOAPIFY_API_KEY` and `RAPIDAPI_KEY`. Enable Geoapify Routing API access for that key. Directed route estimates and verified provider coordinates are cached for 15 minutes, with bounded caches and a routing request limit. API keys are not sent to the browser. Maps load only on request; `VITE_OSM_TILE_URL` can override the default OpenStreetMap tile endpoint.

Route previews support at most 100 selected stops across the trip and at most 200 candidate route combinations. Long or complex itineraries should be split into smaller plans. Network-backed route calculation completes before the database save transaction starts; persistence remains atomic.

### Routing architecture

- `GeoRoutingService`: mode-specific map routes, geometry, step timing, caching and provider error handling.
- `PlanLocationService`: starting address/pin and exact provider-ID coordinate resolution.
- `ManualSchedulePlanner`: route-aware fixed-time checks and insertion of flexible visits, including initial and inter-day journeys.
- `PlanNearbyService`: predicted position at a selected time and nearby restaurant/hotel candidates.
- `TripPlanWriter`: atomic saving of travel entries, visits, meal labels and route snapshots.
- `RouteMap` and `RouteNearbyPicker`: map and user selection views.

`POST /api/trips/plan/nearby` accepts `{plan, date, at, category}` where category is `RESTAURANT` or `HOTEL`; preview and save endpoints remain unchanged. All require authentication. No new database migration is needed for route snapshots and transport activities.

Routing tests use a local fake HTTP server to verify real response parsing, step interpolation, mode selection and sanitized failures. Planner tests cover initial travel, unreachable fixed times, inter-stop travel, overnight continuity and unavailable transit. Full authenticated tests verify persisted transport rows and atomic saves. Browser layout checks use mocked API responses; separate live Geoapify checks verified drive/walk differences and nearby restaurant/hotel results using the configured key without displaying it.

Provider references: [Geoapify Routing API](https://apidocs.geoapify.com/docs/routing/) and [Places API](https://apidocs.geoapify.com/docs/places/).

## Travel reduction and automatic meal suggestions

The scheduler does not claim a mathematically optimal itinerary. **Try reducing travel between flexible visits** is enabled by default in guided planning. It runs a bounded day-by-day relocation search over the already feasible visits, using directed provider travel times. A reordering is accepted only if total travel decreases, all scheduled visits remain, fixed appointment/meal times remain unchanged and the day still fits. An overnight hotel remains the final stop when already last. The search makes up to two improvement passes and allows at most 40 additional route pairs within the existing request budget. If it stops early, the best verified feasible order is retained and labelled accordingly.

The preview reports travel minutes before and after the search and explicitly states that this is a heuristic, not a global-optimality proof. It does not retry every permutation, move visits across days, or automatically recover stops that initially failed to fit. Those stops still appear in the unscheduled list for user adjustment.

Adding **breakfast**, **lunch** or **dinner** creates a meal placeholder with an editable time. The visible day's **Suggested places for your added meals** panel loads venue options automatically—no separate search click is required. Its authenticated `POST /api/trips/plan/meals` request takes `{plan, date}`. The backend shares a single route-context calculation across the day's pending meals and searches near the predicted position at each meal time. The existing individual `/nearby` picker remains available for manual refinements and hotels.

The UI debounces edits, cancels stale requests and serializes automatic requests. Offscreen day panels do not trigger new lookups. Changing meal time, mode, visits or durations refreshes the suggestions. An already selected/pinned meal does not trigger automatic replacement. The user must choose each venue, and the final schedule preview checks actual travel detours and arrival times before saving. Breakfast at the daily starting time may require a venue at the starting point, an earlier departure or a later meal time; suggestions are candidates, not a promise of arrival at the original time.

`ManualPlanRequest.optimizeTravel` enables the bounded search. Preview responses include `optimization` with before/after travel minutes, search-limit status and the algorithm's scope. `AutomaticMealSuggestions` renders the batched results, while `PlanNearbyService.meals` supplies the three time/location-specific options.
