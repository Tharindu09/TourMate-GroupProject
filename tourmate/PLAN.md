> Implementation status: the manual trip planner and schedule builder (Phases 11–12), plus estimated budget totals, are implemented in the active `tourmate/` application. Use **My trips → Plan a trip** (`/trips/plan`) for guided manual planning: basics, area-based place/meal suggestions, user-selected durations and preferences, and a route-aware schedule preview with travel times, mode-dependent routes and location/time-based meal and hotel choices, automatic breakfast/lunch/dinner suggestions and bounded travel-reducing reordering (without a global optimality guarantee). See [MANUAL_SCHEDULER.md](MANUAL_SCHEDULER.md) for usage, constraints and verification.

Since you're rebuilding TourMate as an AI-powered TripAdvisor-style application, I’d implement it incrementally rather than starting with AI. The key is to get the place data + manual itinerary system correct first, because the AI planner will use those same services later.
Target system
                         TourMate

┌─────────────────┐      REST API       ┌──────────────────────┐
│     React       │ ◄────────────────► │     Spring Boot      │
│   TypeScript    │                     │      Java 21         │
└─────────────────┘                     └──────────┬───────────┘
                                                 │
                              ┌──────────────────┼────────────────┐
                              ▼                  ▼                ▼
                            MySQL            Spring AI       External APIs
                              │                  │                │
                        Application data      LLM API        Maps / Weather

I recommend:

Frontend: React + TypeScript + Vite + Tailwind CSS
Backend: Java 21 + Spring Boot + Spring Security + Spring Data JPA
Database: MySQL
AI: Spring AI + an LLM API
Maps: Leaflet + OpenStreetMap initially
API docs: Swagger/OpenAPI
DB migrations: Flyway
Deployment: Docker later
Phase 1 — Create the projects
Use two repositories/directories:
tourmate/
│
├── tourmate-backend/
│
└── tourmate-frontend/

Spring Boot
Create the backend with:
Java 21
Maven

Dependencies:

Spring Web
Spring Data JPA
Spring Security
Validation
MySQL Driver
Flyway
Lombok
Spring Boot DevTools

Don't add Spring AI yet.

Organize the backend by feature/domain, rather than one huge controller/service/repository package:
src/main/java/com/tourmate/

├── auth/
├── user/
├── destination/
├── place/
├── review/
├── favorite/
├── trip/
├── ai/
├── common/
├── config/
└── exception/

For example:
place/
├── Place.java
├── PlaceController.java
├── PlaceService.java
├── PlaceRepository.java
├── PlaceMapper.java
└── dto/

Phase 2 — Set up MySQL + Flyway
Create:
tourmate

Configure Spring:
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tourmate
    username: root
    password: your_password

  jpa:
    hibernate:
      ddl-auto: validate

Use Flyway migrations rather than relying on Hibernate to create your production schema.
src/main/resources/db/migration/

V1__create_users.sql
V2__create_destinations.sql
V3__create_places.sql
V4__create_reviews.sql
V5__create_favorites.sql
V6__create_trips.sql

Phase 3 — Implement authentication
Do this before the travel functionality.

Implement:
Register
Login
JWT access token
Password hashing
Authorization
User profile

Entities:
USER

id
first_name
last_name
email
password_hash
profile_image_url
role
created_at
updated_at

Roles:
USER
ADMIN

Endpoints:
POST /api/auth/register
POST /api/auth/login

GET  /api/users/me
PUT  /api/users/me

Flow:
React Login
     ↓
POST /auth/login
     ↓
Spring Security
     ↓
Authenticate
     ↓
JWT
     ↓
React
     ↓
Authenticated API requests

At the end of this phase, make sure register → login → protected endpoint works before moving on.
Phase 4 — Destination module
Now start the actual travel platform.

Create:
DESTINATION

id
name
city
country
description
cover_image_url
latitude
longitude

Examples:
Ella
Kandy
Galle
Colombo
Sigiriya
Nuwara Eliya
Mirissa

Endpoints:
GET /api/destinations
GET /api/destinations/{id}
GET /api/destinations/search?q=ella

Your React home page can now show:
Where to?

[ Search destinations... ]

Popular destinations

┌──────────┐ ┌──────────┐ ┌──────────┐
│  Ella    │ │  Kandy   │ │  Galle   │
│  IMAGE   │ │  IMAGE   │ │  IMAGE   │
└──────────┘ └──────────┘ └──────────┘

Phase 5 — Place system
This is one of the most important phases because the AI will eventually retrieve these places.

Use:
PLACE

id
destination_id
name
type
description
address
latitude
longitude
average_rating
review_count
price_level
phone
website

Types:
public enum PlaceType {
    HOTEL,
    RESTAURANT,
    ATTRACTION,
    ACTIVITY
}

Then specialized tables:
HOTEL_DETAIL
RESTAURANT_DETAIL
ATTRACTION_DETAIL

And:
PLACE_PHOTO
AMENITY
PLACE_AMENITY
PLACE_OPENING_HOURS

Build endpoints like:
GET /api/places
GET /api/places/{id}

GET /api/places?destinationId=1
GET /api/places?type=HOTEL
GET /api/places?minRating=4
GET /api/places?maxPrice=15000

Don't implement AI search yet. Make normal filtering work properly first.
Phase 6 — Build the React frontend
Now create:
npm create vite@latest tourmate-frontend

I'd use TypeScript.

Structure it something like:
src/

├── api/
├── components/
├── features/
│   ├── auth/
│   ├── destinations/
│   ├── places/
│   ├── reviews/
│   └── trips/
│
├── pages/
├── hooks/
├── types/
├── utils/
└── App.tsx

Main pages:
/
├── Home
├── Search
├── Destination Details
├── Place Details
├── Login
├── Register
├── Profile
├── Favorites
├── My Trips
└── Trip Planner

I would use TanStack Query for server-state/API fetching instead of putting everything into React state.
Phase 7 — TripAdvisor-style discovery UI
Now make it actually feel like a travel application.











Home:
              Where to?

     [ 🔍 Search destinations ]

[Hotels] [Restaurants] [Things to Do]


Explore Sri Lanka

Ella       Kandy       Galle       Sigiriya


Top experiences

Place A    Place B     Place C     Place D

Destination:
Ella

[Overview] [Hotels] [Restaurants] [Things to Do]

Top attractions
...

Hotels
...

Restaurants
...

Place details:
Nine Arch Bridge

★★★★★ 4.8    2,341 Reviews

[ Photos ]

About

Location

Opening hours

Map

Reviews

Phase 8 — Maps
Add Leaflet + OpenStreetMap.

Each place already contains:
latitude
longitude

Therefore you can display:
Places in Ella

┌──────────────────────┬───────────────────────┐
│                      │                       │
│ Nine Arch Bridge     │       📍              │
│ ⭐ 4.8               │             📍        │
│                      │    MAP                │
│ Ella Rock            │         📍            │
│ ⭐ 4.7               │                       │
│                      │                       │
└──────────────────────┴───────────────────────┘

This becomes important later for AI scheduling because distance between activities matters.
Phase 9 — Reviews
Create:
REVIEW

id
user_id
place_id
rating
title
content
travel_type
visit_date
created_at

Endpoints:
POST   /api/places/{id}/reviews
GET    /api/places/{id}/reviews
PUT    /api/reviews/{id}
DELETE /api/reviews/{id}

When a review is added, update/recalculate:
PLACE.average_rating
PLACE.review_count

Also add review photos later.
Phase 10 — Favorites
Simple but useful.
FAVORITE

user_id
place_id
created_at

Endpoints:
POST   /api/favorites/{placeId}
DELETE /api/favorites/{placeId}
GET    /api/favorites

Now users can save interesting places before building their trip.
Phase 11 — Manual Trip Planner
This is where I'd spend significant effort before touching AI.

Create:
TRIP
├── TRIP_DESTINATION
└── TRIP_DAY
       └── TRIP_ITEM

TRIP:
id
user_id
name
start_date
end_date
budget
travelers
planning_mode
status

When the user creates:
Ella Adventure

25 Oct → 27 Oct

Spring creates:
TRIP #101

├── Day 1 - Oct 25
├── Day 2 - Oct 26
└── Day 3 - Oct 27

Phase 12 — Manual schedule builder
This is the UI we discussed earlier.
Ella Adventure
Oct 25 – Oct 27

DAY 1
──────────────────────────────

09:00
📍 Nine Arch Bridge
09:00 → 10:30

12:30
🍴 Cafe Chill
12:30 → 13:30

15:00
🥾 Little Adam's Peak
15:00 → 17:00

        [+ Add Activity]


DAY 2
──────────────────────────────

        [+ Add Activity]

The user clicks:
+ Add Activity

Then:
Search places

[ Nine Arch Bridge        + ]
[ Ella Rock               + ]
[ Ravana Falls            + ]

Select:
Start:       09:00
End:         10:30
Cost:        LKR 0
Notes:       Bring camera

Save to:
TRIP_ITEM

Also allow:
Add Place
Add Custom Activity
Edit
Delete
Move
Reorder

Use drag-and-drop in React.

At this stage you have a fully functional non-AI TourMate.
Phase 13 — Budget tracking
Because each TRIP_ITEM can have an estimated cost, calculate:
Trip Budget

Total Budget                   LKR 60,000

Accommodation                  LKR 25,000
Food                           LKR 12,500
Transport                       LKR 7,500
Activities                      LKR 5,000
─────────────────────────────────────────
Estimated                       LKR 50,000

Remaining                       LKR 10,000

This information becomes extremely useful for the AI planner.
Phase 14 — Add Spring AI
Only now would I add AI.

Add Spring AI to the Spring Boot project and configure your chosen LLM provider.

Create a dedicated layer:
ai/

├── AiController
├── TripPlanningAgent
├── AiTripPlanningService
├── AiSearchService
├── ReviewSummaryService
├── dto/
└── tools/

Don't put LLM calls directly inside TripService.

Keep:
TripService

responsible for trips and:
AiTripPlanningService

responsible for AI reasoning/generation.
Phase 15 — AI requirement extraction
User enters:
Destination: Ella
Dates: Oct 25–27
Travelers: 2
Budget: LKR 60,000

Interests:
✓ Hiking
✓ Nature
✓ Photography
✓ Local food

Pace:
Balanced

"I don't want difficult hikes."

Have the AI convert this into structured data:
{
  "destination": "Ella",
  "travelers": 2,
  "budget": 60000,
  "interests": [
    "hiking",
    "nature",
    "photography",
    "local_food"
  ],
  "pace": "BALANCED",
  "requirements": [
    "avoid difficult hikes"
  ]
}

Structured output is preferable to asking the model for a giant block of text.
Phase 16 — Build AI tools
Now connect AI to your actual application services.

For example:
searchAttractions(...)
searchHotels(...)
searchRestaurants(...)
getPlaceDetails(...)
getOpeningHours(...)
getNearbyPlaces(...)
getWeather(...)

Conceptually:
                    AI Planner
                        │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
   searchHotels   searchPlaces   restaurants
          │             │             │
          └─────────────┼─────────────┘
                        ↓
                     MySQL

Now the LLM isn't expected to know your application data.
Phase 17 — AI itinerary generation
This becomes your main AI feature.
User Preferences
       ↓
AI Requirement Extraction
       ↓
Retrieve candidate places
       ↓
Hotels + Restaurants + Attractions
       ↓
Check
 • budget
 • opening hours
 • distance
 • duration
 • interests
       ↓
LLM Planner
       ↓
Structured itinerary
       ↓
Validate
       ↓
TRIP_DAY + TRIP_ITEM
       ↓
MySQL

Have AI return something structured:
{
  "days": [
    {
      "day": 1,
      "activities": [
        {
          "placeId": 34,
          "startTime": "09:00",
          "endTime": "10:30"
        },
        {
          "placeId": 82,
          "startTime": "12:30",
          "endTime": "13:30"
        }
      ]
    }
  ]
}

This is much better than returning formatted text.

Spring Boot validates it and saves the result.

React controls how the itinerary looks.
Phase 18 — Manual + AI hybrid
Now combine the two systems.
                 TRIP
                   │
          ┌────────┴─────────┐
          │                  │
       MANUAL               AI
          │                  │
User creates items      AI creates items
          │                  │
          └────────┬─────────┘
                   ↓
              TRIP_ITEM
                   ↓
           Same React Planner

The user can modify an AI-generated itinerary exactly as they modify a manual one.

That's a major architectural advantage.

Add buttons:
✨ Suggest Activity

✨ Optimize This Day

✨ Replace Activity

✨ Make Day Less Busy

✨ Reduce Cost

✨ Replan Day

✨ Replan Entire Trip

Phase 19 — Natural-language travel search
Once the planner works, add:
Find quiet hotels in Ella under LKR 15,000 with good mountain views.

AI extracts:
{
  "type": "HOTEL",
  "destination": "Ella",
  "maxPrice": 15000,
  "preferences": [
    "quiet",
    "mountain view"
  ]
}

Then combine structured DB filtering with semantic matching later.

Don't add a vector database just because you're using AI. Add one when semantic search/review retrieval actually requires it.
Phase 20 — AI review summaries
Once you have enough review data:
2,341 reviews
       ↓
Retrieve relevant reviews
       ↓
LLM
       ↓

AI Review Summary

✓ Excellent location
✓ Friendly staff
✓ Good breakfast

Common complaints
• Limited parking
• Wi-Fi can be unreliable

Best for
Couples and short stays

This is where a more traditional RAG/vector-search implementation can become useful.
Phase 21 — External APIs
Then integrate external services incrementally.

You could have:
Weather API
    ↓
Current/forecast weather

Maps
    ↓
Location + navigation

Currency API
    ↓
Currency conversion

Places/travel API
    ↓
Optional external place information

Don't make your core system dependent on five external APIs from day one.
Phase 22 — PDF itinerary
Generate something like:
TourMate
─────────────────────────

Ella Adventure
25–27 October 2026

DAY 1

09:00  Nine Arch Bridge
12:30  Lunch
15:00  Little Adam's Peak

DAY 2
...

Estimated Budget
LKR 54,500

The backend should generate the PDF from the saved itinerary rather than asking the AI to generate it.
Phase 23 — Admin panel
Add:
Dashboard

Destinations
Places
Hotels
Restaurants
Attractions
Users
Reviews
Reported reviews

Admins should be able to maintain the data the AI uses.

That's important: bad database data → bad AI recommendations.
Phase 24 — Testing
For Spring Boot:
JUnit 5
Mockito
Spring Boot Test
Testcontainers

Test especially:
Authentication
Place filtering
Trip creation
Trip ownership
Schedule conflicts
Budget calculations
AI output validation
Unauthorized modifications

For React:
Vitest
React Testing Library

Phase 25 — Docker + deployment
Finally:
docker-compose.yml

services:

  frontend
      React

  backend
      Spring Boot

  mysql
      MySQL

  redis        # optional later

Production:
Internet
   ↓
Frontend
   ↓
Spring Boot
   ↓
┌───────────────┬──────────────┐
▼               ▼              ▼
MySQL        AI Provider    External APIs

Recommended implementation order
If you're starting today, follow this exact order:
1. Spring Boot + MySQL + Flyway
2. User authentication + Spring Security/JWT
3. Destination CRUD
4. Place CRUD and place types
5. Search/filtering
6. React foundation
7. Home/destination/place pages
8. Maps
9. Reviews and ratings
10. Favorites
11. Trip creation
12. Trip days
13. Manual schedule builder
14. Drag/drop + custom activities
15. Budget management
16. Spring AI integration
17. AI requirement extraction
18. AI tools connected to your services/database
19. AI itinerary generation
20. AI ↔ manual editing
21. AI optimization/replanning
22. Natural-language search
23. Review summarization/RAG
24. Weather/currency/external APIs
25. PDF itinerary
26. Admin dashboard
27. Testing
28. Docker + deployment
The most important milestone is #13. Once the manual planner works correctly, the AI planner doesn't need a second scheduling architecture—it simply becomes another way of generating the same TRIP_DAY and TRIP_ITEM records. That will keep TourMate much cleaner and easier to implement.













What if I use Ollama for this?












Yes. Ollama is a very good choice for developing TourMate, especially because you can run the LLM locally without paying for an API during development. Spring AI has native Ollama integration, including chat models, embeddings, structured output, and tool calling. Home

Since you already have Ollama working locally, I would change the AI architecture to:
React
   ↓
Spring Boot
   ↓
Spring AI
   ↓
Ollama
   ↓
Local LLM

Your application database remains separate:
                    React
                      │
                      ▼
                 Spring Boot
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
      MySQL       Spring AI     External APIs
                      │
                      ▼
                    Ollama
                      │
                      ▼
                  Local LLM

How the trip planner would work
Suppose the user enters:
Plan a 3-day Ella trip for two people. Budget LKR 60,000. We like nature, photography and light hiking.

Don't send that to Ollama and simply display whatever it generates.

Instead:
User Request
     ↓
Spring Boot
     ↓
Ollama
     │
     │ Extract requirements
     ▼
{
 destination: "Ella",
 days: 3,
 budget: 60000,
 travelers: 2,
 interests: [
   "nature",
   "photography",
   "light hiking"
 ]
}
     ↓
Spring Boot
     ↓
Query MySQL
     ↓
Retrieve REAL
hotels / restaurants / attractions
     ↓
Ollama
     ↓
Generate optimized itinerary
     ↓
Spring Boot validates it
     ↓
TRIP_DAY + TRIP_ITEM
     ↓
MySQL

That's a much better use of Ollama.
Even better: give Ollama tools
Spring AI's Ollama integration supports tool calling, so your model can request functions while planning rather than relying entirely on information stuffed into one prompt. Home

For example:
Ollama
  │
  ├── searchHotels()
  │
  ├── searchRestaurants()
  │
  ├── searchAttractions()
  │
  ├── getPlaceDetails()
  │
  ├── getNearbyPlaces()
  │
  ├── getOpeningHours()
  │
  └── getWeather()

Suppose Ollama decides:
I need attractions in Ella suitable for light hiking.

It can request:
searchAttractions(
    destination = "Ella",
    difficulty = "EASY"
)

Your Java code queries MySQL:
MySQL
 ↓

Little Adam's Peak
Nine Arch Bridge
Ravana Falls
...

The results go back into the model's context, and it continues planning.

That makes your project much closer to an agentic travel planner.
Spring AI + Ollama configuration
Spring AI currently provides the spring-ai-starter-model-ollama starter and defaults to connecting to Ollama at localhost:11434. Home

Your configuration can therefore be roughly:
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: qwen3:8b

Then your service can work through Spring AI rather than directly making HTTP calls to Ollama.
@Service
public class AiTripPlanningService {

    private final ChatClient chatClient;

    public AiTripPlanningService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String planTrip(String request) {

        return chatClient
                .prompt()
                .system("""
                    You are the AI trip planner for TourMate.
                    Create realistic travel schedules using
                    the places provided by TourMate.
                    """)
                .user(request)
                .call()
                .content();
    }
}

Later, replace the String response with a structured Java object such as:
public record GeneratedTrip(
    List<GeneratedDay> days
) {}

public record GeneratedDay(
    int dayNumber,
    List<GeneratedActivity> activities
) {}

public record GeneratedActivity(
    Long placeId,
    LocalTime startTime,
    LocalTime endTime,
    double estimatedCost
) {}

That's preferable because you ultimately want to save:
GeneratedTrip
     ↓
Validate
     ↓
TRIP
TRIP_DAY
TRIP_ITEM

rather than trying to parse a pretty textual itinerary.
You can use Ollama for embeddings too
Later, when you implement semantic search/RAG, you don't necessarily need OpenAI embeddings either. Spring AI provides OllamaEmbeddingModel, so an embedding model running through Ollama can generate vectors locally. Home

That could power queries such as:
“quiet romantic hotels with mountain views”

User query
     ↓
Ollama Embedding Model
     ↓
Query vector
     ↓
Vector search
     ↓
Relevant TourMate places/reviews
     ↓
Ollama LLM
     ↓
Recommendation

I wouldn't build this part initially. Get the structured MySQL retrieval and AI planner working first.
Local development vs production
There is one important distinction.

For your development machine, Ollama is excellent:
Your Laptop

React
Spring Boot
MySQL
Ollama
Qwen

No per-request LLM cost, local experimentation, easy model switching, and you learn local LLM deployment at the same time.

For a public production deployment, though, Ollama has to run on the server, not on each user's browser:
Users
  ↓
Internet
  ↓
React
  ↓
Spring Boot
  ↓
┌───────────┬─────────────┐
│           │             │
MySQL     Ollama      External APIs
            │
            ▼
           LLM

That server needs enough compute for your chosen model and expected concurrent users. Alternatively, you can later replace the local model with a hosted provider without redesigning the whole application if you keep the AI layer properly abstracted.

So for TourMate V1, I'd actually recommend:

React + Spring Boot + Spring Security + Spring AI + MySQL + Ollama + REST APIs + External APIs

And build AI in this order:

Ollama connection → structured requirement extraction → database-backed tool calling → itinerary generation → validation → manual editing → AI replanning → embeddings/RAG later.

That would also make TourMate a considerably more interesting AI engineering project than simply calling an OpenAI endpoint.
## Implemented: AI scheduling

The Spring AI/Ollama scheduler is available at `/trips/ai`, using worldwide real candidates, per-place duration reasoning, required user selections, route-context restaurant choices, actual mode-specific travel checks, bounded ordering repair, review and shared transactional persistence. Setup, model requirements, tests and practical limits are documented in [AI_SCHEDULER.md](AI_SCHEDULER.md). Local Ollama must have the configured model installed before live generation. Natural-language catalogue search and guaranteed opening-hours validation are separate future work.
