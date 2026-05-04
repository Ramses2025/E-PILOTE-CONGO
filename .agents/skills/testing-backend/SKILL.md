---
name: testing-epilo-backend
description: Test the E-PILOTE-CONGO Spring Boot backend end-to-end. Use when verifying backend API changes, security fixes, or messaging features.
---

# Testing E-PILOTE Backend

## Prerequisites

### Devin Secrets Needed
- `info_couchbase` — Contains Couchbase connection string, username, and password
- Alternatively, individual secrets: `CB_CONNECTION_STRING`, `CB_USERNAME`, `CB_PASSWORD`

### Environment Variables
```bash
export CB_CONNECTION_STRING="couchbases://cb.qfnlqsnztvyhsxs.cloud.couchbase.com"
export CB_USERNAME="PILOTE"
export CB_PASSWORD="<from info_couchbase secret>"
export JWT_SECRET="<any string >= 32 characters>"  # For testing only
export CB_BUCKET="epilote_prod"  # Optional, defaults to epilote_prod
```

## Running Unit Tests
```bash
cd epilote-backend
./gradlew clean test --no-daemon
```

Expected: 23 tests across 3 suites:
- `RateLimitFilterTest` (6 tests) — rate limiting behavior
- `JwtServiceTest` (7 tests) — JWT token generation/validation
- `AdminRealtimePathSupportTest` (10 tests) — SSE path routing

## Starting the Backend
```bash
cd epilote-backend
CB_CONNECTION_STRING="..." CB_USERNAME="..." CB_PASSWORD="..." JWT_SECRET="..." \
  ./gradlew bootRun --no-daemon
```

Backend starts on `http://localhost:8080`. Look for:
- `Started EpiloteApplicationKt in X seconds` — successful startup
- `SUPER_ADMIN déjà existant` — confirms Couchbase connection works

### Couchbase IP Whitelisting
The Devin VM IP may need to be whitelisted in Capella. If connection fails, the user may need to add the VM IP (or temporarily open 0.0.0.0/0) in Capella > Database Access > Allowed IPs.

## API Testing via curl

### Authentication
```bash
# Login (default super admin)
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"super@admin.cg","password":"Admin@2024!"}'
```
Default super admin: `super@admin.cg` / `Admin@2024!` (if no SUPER_ADMIN_PASSWORD env var set).

Save the `accessToken` from the response for subsequent requests.

### Rate Limiter Testing
- Send 11+ POST requests to `/api/auth/login` within 1 minute
- Requests 1-10: should return 401 (invalid creds) or 200 (valid creds)
- Request 11+: should return HTTP 429 with `RATE_LIMITED`
- **Important**: Rate limit window is 60 seconds. Wait 60s between rate limiter tests.
- Rate limiter uses `remoteAddr` only (not X-Forwarded-For) for security

### Messaging Endpoints
```bash
TOKEN="<accessToken from login>"

# List messages with pagination
curl -s "http://localhost:8080/api/super-admin/communications/messages?page=1&pageSize=5" \
  -H "Authorization: Bearer $TOKEN"

# List announcements
curl -s "http://localhost:8080/api/super-admin/communications/announcements?page=1&pageSize=5" \
  -H "Authorization: Bearer $TOKEN"

# Mark message as read
curl -s -X PUT "http://localhost:8080/api/super-admin/communications/messages/{messageId}/read" \
  -H "Authorization: Bearer $TOKEN"
```

### JWT Secret Validation
To verify the secret length check works:
```bash
JWT_SECRET="short" ./gradlew bootRun --no-daemon 2>&1 | grep -i "256 bits"
```
Should fail with: `JWT_SECRET must be at least 256 bits (32 bytes)`

## Testing Scope
- **Backend API**: Fully testable via curl against running backend
- **Desktop Compose client**: Cannot be tested without running the desktop app (requires JVM + Compose Desktop setup). SSE and StateFlow changes are verified at compilation level only.
- **No web frontend exists** — all testing is shell-based, no browser recording needed

## Common Issues
- Rate limiter blocks all requests from localhost after 10 attempts — wait 60s for window to expire
- If backend fails to start, check Couchbase connection (IP whitelisting, credentials)
- `runBlocking` warnings in logs are expected during `SuperAdminInitializer` startup only
