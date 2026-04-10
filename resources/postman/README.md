# Sleep Logger API

## Project Structure

```
sleep/src/main/kotlin/.../sleep/
├── controller/       # REST endpoints
├── service/          # Business logic (validation, aggregation)
├── repository/       # JDBC data access (NamedParameterJdbcTemplate)
├── model/            # Domain entities and DTOs
├── exception/        # Custom exceptions + global error handler
└── db/               # DataSource configuration

sleep/src/main/resources/
└── db/migration/     # Flyway scripts (users table, sleep_log table)

sleep/src/test/kotlin/.../sleep/
├── controller/       # @WebMvcTest controller tests
├── service/          # Mock-based service unit tests
└── repository/       # Mock-based repository unit tests

resources/postman/    # Postman collection + README
```

## Postman Collection Setup

1. Import `SleepLoggerAPI.postman_collection.json` into Postman.
2. Start the application: `docker-compose up --build`
3. The collection uses `{{baseUrl}}` defaulting to `http://localhost:8080`.

## Dynamic Timestamps

All request bodies use timestamps generated relative to the current date via a collection-level pre-request script. This means the collection works correctly regardless of when it is run — no need to edit dates manually.

## Test Flow

Run the folders **in order** (1 → 2 → 3 → 4):

| Folder | Purpose |
|--------|---------|
| **1 — Create Sleep Log** | Creates a log for Alice, then exercises validation errors (bad times, invalid enum, missing fields, duplicate date, unknown user). |
| **2 — Get Last Night's Sleep** | Fetches Alice's log from step 1, verifies 404 for Bob (no data) and a non-existent user. |
| **3 — Seed Data for Averages** | Creates 5 consecutive nights of sleep data for Charlie with mixed feelings. |
| **4 — Get 30-Day Averages** | Retrieves Charlie's averages, Alice's single-entry averages, and verifies 404 for Bob. |

## Seeded Users

The database migration seeds three users:

| ID | Name |
|----|------|
| 1  | Alice |
| 2  | Bob |
| 3  | Charlie |

## Notes

- The database resets on each `docker-compose up --build`, so the full collection can be re-run cleanly.
- Folder 3 must be run before folder 4 — the seed requests populate Charlie's history for the averages endpoint.
