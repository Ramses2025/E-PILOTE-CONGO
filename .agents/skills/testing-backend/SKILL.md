---
name: testing-epilote-backend
description: Test the E-PILOTE-CONGO Spring Boot backend end-to-end. Use when verifying security, messaging, or API changes.
---

# Testing E-PILOTE-CONGO Backend

## Prerequisites

- Java 17+ (backend uses Spring Boot 3.2.4)
- Couchbase Capella credentials (connection string, username, password)
- The backend runs on port 8080 by default

## Devin Secrets Needed

- `info_couchbase` — Contains Couchbase connection string, username (PILOTE), and password. Required for backend startup.

## Running Unit Tests

```bash
cd epilote-backend
./gradlew clean test
```

- Expects 23 tests across 3 suites:
  - `RateLimitFilterTest` (6 tests) — rate limiting, IP tracking, remoteAddr-only
  - `JwtServiceTest` (7 tests) — token generation/validation, secret validation
  - `AdminRealtimePathSupportTest` (10 tests) — SSE path routing
- The `gradlew` is inside `epilote-backend/`, NOT at the project root

## Starting the Backend

```bash
cd epilote-backend
export COUCHBASE_CONNECTION_STRING="couchbases://..."
export COUCHBASE_USERNAME="PILOTE"
export COUCHBASE_PASSWORD="..."
export JWT_SECRET="<at-least-32-character-secret>"
java -jar build/libs/epilote-backend-*.jar
```

Or use Gradle:
```bash
./gradlew bootRun
```

Successful startup shows:
```
Started EpiloteApplicationKt in ~4 seconds
SUPER_ADMIN déjà existant — aucune modification
```

## Default Test Account

- Email: `super@admin.cg`
- Password: `Admin@2024!`
- Role: SUPER_ADMIN

## Key API Endpoints to Test

### Authentication
- `POST /api/auth/login` — Body: `{"email": "...", "password": "..."}` → Returns `LoginResponse` with `accessToken`, `role`, `userId`, `expiresIn`
- `POST /api/auth/refresh` — Body: `{"refreshToken": "..."}` → Returns new `accessToken`

### Messaging (requires Bearer token with SUPER_ADMIN role)
- `GET /api/super-admin/communications/messages?page=1&pageSize=5` — Paginated message list
- `GET /api/super-admin/communications/announcements?page=1&pageSize=5` — Paginated announcements
- `PUT /api/super-admin/communications/messages/{id}/read` — Mark message as read (adds userId to `readBy` array)
- `POST /api/super-admin/communications/messages` — Create message
- `POST /api/super-admin/communications/announcements` — Create announcement

### Security
- Invalid/falsified JWT tokens should return HTTP 403 on `/api/super-admin/**` endpoints
- Rate limiter: 10 attempts per minute per IP on `/api/auth/login` and `/api/auth/refresh`, then HTTP 429 with `{"code":"RATE_LIMITED",...}`

## Visual Testing with HTML Dashboard

For browser-based visual testing, create a simple HTML page that calls the API endpoints and displays results. Key considerations:

- **CORS**: The backend has no CORS configuration. If serving the HTML from a different origin, use a reverse proxy (Python `http.server` works well) that serves the HTML and proxies `/api/*` requests to `localhost:8080`
- **file:// protocol**: Browser blocks fetch requests from `file://` to `http://localhost`. Always serve via HTTP.

## Troubleshooting

- If `./gradlew` not found at project root, check `epilote-backend/gradlew` — the Gradle wrapper is per-module
- If backend fails to start, verify Couchbase credentials and that the bucket `epilote_prod` exists
- Rate limiter state resets on backend restart. Wait 60s or restart backend between rate limit tests
- The backend uses `request.remoteAddr` for rate limiting (not X-Forwarded-For) — this is intentional for anti-spoofing
