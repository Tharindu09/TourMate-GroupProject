# AI trip scheduling

Open **My trips → Plan with AI** (`/trips/ai`). Enter dates, a specific starting address or pin, travel mode, daily hours and areas. Then answer guided questions about interests, pace, dietary needs, walking/accessibility limits, meals and overnight accommodation. Manual venue selection is not required. AI retrieves real candidates, estimates visit durations, selects meals along the predicted route and optionally selects a hotel for each night between trip days. Review the generated schedule, route legs, uncertainties and conflicts before saving. Adjusting a draft uses the existing editor and requires a fresh route preview.

The questions are a structured questionnaire, not a free-form AI conversation. Answers become planning context; provider data supplies venue identities and coordinates, and Java/map routing supplies travel calculations. AI cannot invent a feasible route, verified price, opening hour or booking.

## Local model setup

The backend uses Spring AI 1.1.8 and Ollama. This release line supports the application's Spring Boot 3.5 version ([Spring AI compatibility](https://github.com/spring-projects/spring-ai)). No OpenAI API key is needed.

Install/start Ollama on the machine that will run inference, then pull the configured model:

```sh
ollama pull qwen3:8b
```

The model download and inference need sufficient disk space and RAM. Ollama model downloads are deliberately not triggered during backend startup. `GET /api/tags` is checked before spending place-provider quota; missing models or unreachable Ollama produce a setup error instead of a simulated AI itinerary.

For a backend run directly on the host, defaults are `http://localhost:11434` and `qwen3:8b`. For Compose, `.env.example` and `compose.yaml` configure:

```dotenv
OLLAMA_BASE_URL=http://host.docker.internal:11434
OLLAMA_MODEL=qwen3:8b
```

The Compose backend includes Linux's `host-gateway` mapping. Ollama must listen on an address reachable from the backend container; a host daemon bound only to loopback cannot accept container requests. Configure its listener for the Docker network, or supply a reachable remote Ollama URL. Set `OLLAMA_MODEL` to another installed instruction model if appropriate; structured-output reliability varies by model. Do not expose an unauthenticated Ollama listener to public networks.

The existing `GEOAPIFY_API_KEY` supplies worldwide candidates, geocoding and mode-specific route estimates. RapidAPI-selected places can be retained, but AI's automatic candidate retrieval uses Geoapify. No city IDs or named venue durations are hardcoded.

Rebuild after configuring the model:

```sh
cd tourmate
sudo docker compose up -d --build backend frontend
```

## How the draft is built

1. Java retrieves actual attraction candidates for each daily area, including names, descriptions, categories, coordinates and reported opening hours. Automatically retrieved candidates exclude venues selected on previous days. User-selected stops remain required.
2. Spring AI `ChatClient` calls Ollama and converts structured output into grounded candidate IDs, visit durations and reasoning. The prompt considers pace, interests, venue type, daily hours, transport and prior days. A large garden or complex can receive several hours; there is no blanket one-hour rule. This reasoning is an estimate, not verified venue knowledge.
3. Java rejects invented/duplicate candidate IDs, omitted required stops, missing explanations and invalid durations. Non-meal durations are 15–480 minutes; explicit fixed appointments retain their user duration. Meals are 30–120 minutes. Venue coordinates, prices and identities always come from the input/provider, never the model.
4. Pending meal stops query real restaurants near the predicted route position at the requested time. The model sees actual nearby options, their reported hours and route detour estimates. Defaults are 08:30, 12:30 and 19:00; users can add a meal with a different time before generation. Default meals outside daily hours are omitted with a warning; extend the day if you want them included.
5. The shared route planner recalculates the full route including meal detours, travel mode, buffers and fixed times. Its bounded relocation search attempts to reduce travel. If visits remain unscheduled, one additional AI ordering pass is allowed for each affected day; Java preserves all durations and visits and adopts the alternative only if it reduces conflicts. It never makes a plan fit by silently deleting required visits or squeezing their durations.
6. The preview lists unscheduled visits/meals, route legs, duration reasoning, uncertainties and budget limitations. Saving is blocked while any stop cannot fit. This is a practical bounded search, not a proof of global optimality.
7. An unedited AI draft is held server-side for one hour, scoped to its user. Save rechecks route feasibility and atomically persists the shared trip/day/item structure, transport entries, AI provenance and route snapshot with `planningMode=AI`. A consumed draft cannot be saved twice. Drafts disappear on backend restart. Editing the draft uses the manual save path after another preview; saved duration notes retain AI reasoning.

## Practical limits

- Up to 7 days and 40 user-selected stops per generation. At most two generations run concurrently, and only one per account. Candidate context, generated output, repair passes and stored drafts are bounded.
- Requests run synchronously; local CPU inference can take minutes. The AI proxy allows up to 20 minutes, and model HTTP reads are bounded. A provider failure reports an error; it does not fall back to fabricated recommendations.
- Provider opening-hours strings are passed to the model and retained in generated stop notes, but are **not** a reliable date-specific opening-hours validator. Check closure days, admission cutoffs, tickets, accessibility and dietary suitability before travel. The UI states these uncertainties.
- Routing estimates do not guarantee live traffic or public transport service at a particular departure time. Multi-timezone travel uses a common planning clock; review local times. Day-to-day travel begins at the preceding day's final location; overnight return journeys are not invented.
- Unknown admission/meal prices remain zero placeholders with a visible warning, not claims of free entry or budget compliance. Enter real group costs in the review editor.
- Meal candidates use a preliminary route; final insertion can shift the route. The complete preview verifies arrivals and detours, and may require adjusting a meal or visit. The UI also offers manual route-based restaurant and hotel alternatives.
- The model can choose only from retrieved candidates. An arbitrary must-see mentioned in text may be absent; select it explicitly or add a verified custom pin to require it.

## API and layers

Authenticated endpoints:

- `POST /api/trips/ai/preview`: `{plan: ManualPlanRequest, preferences, pace: RELAXED|BALANCED|ACTIVE, meals: [BREAKFAST|LUNCH|DINNER], includeHotels: boolean}` → server draft ID, generated plan, route preview, explanations, warnings and model.
- `POST /api/trips/ai/{draftId}/save`: revalidates and saves the owned draft, returning the normal trip response.

`AiTripPlanController` handles HTTP, `AiTripPlanService` handles retrieval/grounding/route verification and draft ownership, `AiScheduleAdvisor` encapsulates Spring AI, and `TripPlanWriter` writes through the existing transactional trip services and repositories. External inference and routing happen before persistence begins.

Tests cover real Spring AI/Ollama protocol conversion against a controlled HTTP server, malformed output/provider errors, hallucinated IDs, duration checks, required visits, fixed appointments, meal route context, ownership and route revalidation. API tests save/reload AI metadata through H2 and the actual security/controller/persistence stack. UI tests verify generation, 180-minute visit display, review-first saving and the server-draft save path. Live model quality requires a configured downloaded Ollama model and representative itinerary evaluation.

## Automatic venues and review

The AI flow now asks questions after basics and generates without manual stop selection. Breakfast, lunch and dinner are enabled by default and can be unchecked. Defaults are 08:30, 12:30 and 19:00, with a 45-minute starting duration; AI estimates 30–120 minutes for a retrieved meal venue. Meals outside daily hours are omitted with a warning. Optional hotels reserve a 30-minute check-in ending at the daily finish, for nights between trip days only. Hotels are retrieved near the predicted endpoint and selected only from provider candidates. A selected hotel feeds subsequent meal-location route calculations. Missing hotels produce a warning requiring the user to arrange accommodation; these are suggestions, not verified availability or bookings.

Review → Adjust preferences opens the duration, time, cost and venue editor. The manual planner retains its explicit selection flow, including route-based AI ranking. Ollama is required for generation, and provider failures are reported rather than replaced with fabricated data.

## AI ranking of nearby restaurants and hotels

Before generating the itinerary, use **Meal and hotel options along this day's route**. Select the type, meal purpose and local time; enable **Rank nearby options with AI**, enter cuisine, dietary or hotel preferences, then request options. Routes determine the estimated position at that time (including intermediate travel), Geoapify supplies real venues nearby, and Ollama ranks the full candidate list with reasons. The user selects the venue. AI cannot supply new venue IDs or change provider coordinates. Unknown hours and suitability remain unverified. Changing the route, time, type or preferences clears the result. If no attractions are selected yet, the estimate follows the available starting location and selected stops; add visits for a more useful route estimate.

Authenticated `POST /api/trips/ai/nearby` accepts `{nearby: {plan, date, at, category: RESTAURANT|HOTEL}, preferences}` and returns `{nearby, reasons, model}`. Ranking has two concurrent inference slots and checks Ollama before querying places. AI errors are explicit; uncheck ranking to request the existing route-based list without AI.

## Fix a loopback-only Ollama service on Linux

If `curl http://localhost:11434/api/tags` lists the configured model but `ss -ltn '( sport = :11434 )'` shows only `127.0.0.1:11434`, the Compose backend cannot access that listener through `host.docker.internal`. The installed model does not need downloading again.

Run `sudo systemctl edit ollama.service` and add this above the editor's discard marker:

```ini
[Service]
Environment="OLLAMA_HOST=0.0.0.0:11434"
```

Then run:

```sh
sudo systemctl daemon-reload
sudo systemctl restart ollama
curl http://localhost:11434/api/tags
```

This listener accepts network connections; keep port 11434 restricted to your machine and Docker network, not exposed to public networks. This follows the [Ollama Linux service configuration guide](https://docs.ollama.com/faq#setting-environment-variables-on-linux).

Keep these values in `tourmate/.env` when using Compose:

```dotenv
OLLAMA_BASE_URL=http://host.docker.internal:11434
OLLAMA_MODEL=qwen3:8b
```

From `tourmate/`, run `sudo docker compose up --build -d backend frontend`. To verify connectivity from the backend network without printing keys:

```sh
sudo docker compose exec backend bash -lc 'exec 3<>/dev/tcp/host.docker.internal/11434; printf "GET /api/tags HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3; cat <&3'
```

Expect HTTP 200 and the model list. Host connectivity alone does not establish container connectivity. If it still fails, inspect `sudo docker compose logs --tail=80 backend` and `sudo journalctl -u ollama --no-pager -n 50`.

## Slow or invalid Ollama output

The availability check only confirms that Ollama answers and the model is installed; inference can still time out or produce invalid output. `AiScheduleAdvisor` now disables separate thinking through the Ollama API, supplies the `Advice` JSON schema as the provider output format, requests concise explanations, and caps completion output at 1600 tokens. It distinguishes a length-limited result, empty visits and transport timeouts instead of asking users to download an already installed model. Map/venue validation still applies to every parsed result.

The Spring HTTP read timeout defaults to 600 seconds (`OLLAMA_READ_TIMEOUT` when running the backend directly); the reverse proxy AI timeout remains 1200 seconds for the whole HTTP request. A multi-day draft makes multiple model calls, so start with one day when checking a slow local machine. `journalctl -u ollama --no-pager -n 80` can show generation speed and request durations without needing Docker access. Local logs on this machine showed a chat request lasting over 20 minutes before this change. This confirms slow inference; it does not establish the exact cause of every prior failure. Live timing after the change must be checked separately.
