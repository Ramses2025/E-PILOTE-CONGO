---
name: testing-e2e-epilote
description: Test the E-PILOTE-CONGO platform end-to-end with backend API and desktop Compose UI. Use when verifying security, messaging, or UI changes.
---

# E-PILOTE-CONGO E2E Testing

## Prerequisites

### Devin Secrets Needed
- `info_couchbase` — Contains Couchbase Capella connection string, username, password, API key
- The JWT secret must be >= 32 characters (e.g. `EpiloteCongoSuperSecretKeyForJWT2024PlatformSecure`)

### Couchbase Capella IP Whitelisting
The VM's public IP must be whitelisted in Couchbase Capella before the backend can connect.
- Check VM IP: `curl -s https://ifconfig.me`
- If the backend times out with `WaitUntilReady timed out in stage WAIT_FOR_CONFIG`, the IP likely needs whitelisting
- Ask the user to whitelist in: Capella Console → Cluster → Settings → Allowed IP Addresses → Add `<VM_IP>/32`
- The VM IP may change after restarts, requiring re-whitelisting each time

## Backend Setup

```bash
# Set environment variables
export CB_CONNECTION_STRING="couchbases://cb.qfnlqsnztvyhsxs.cloud.couchbase.com"
export CB_USERNAME="PILOTE"
export CB_PASSWORD="<from info_couchbase secret>"
export CB_BUCKET="epilote_prod"
export JWT_SECRET="EpiloteCongoSuperSecretKeyForJWT2024PlatformSecure"

# Build backend JAR (if not already built)
cd /home/ubuntu/E-PILOTE-CONGO/epilote-backend
./gradlew bootJar

# Start backend
nohup java -jar build/libs/epilote-backend-0.0.1-SNAPSHOT.jar > /tmp/backend.log 2>&1 &

# Verify backend is running (should return 403, meaning server is up but auth required)
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/admin/dashboard-stats
```

## Desktop App Setup

### ICU Library Symlinks
CouchbaseLite requires ICU 71 but the system may only have ICU 70. Create symlinks if needed:
```bash
sudo ln -sf /usr/lib/x86_64-linux-gnu/libicuuc.so.70 /usr/lib/x86_64-linux-gnu/libicuuc.so.71
sudo ln -sf /usr/lib/x86_64-linux-gnu/libicui18n.so.70 /usr/lib/x86_64-linux-gnu/libicui18n.so.71
sudo ln -sf /usr/lib/x86_64-linux-gnu/libicudata.so.70 /usr/lib/x86_64-linux-gnu/libicudata.so.71
sudo ldconfig
```

### Gradle Wrapper
The `epilote-kmp` module needs its own `gradle-wrapper.jar`. If missing:
```bash
cp epilote-backend/gradle/wrapper/gradle-wrapper.jar epilote-kmp/gradle/wrapper/gradle-wrapper.jar
```

### Launch Desktop
```bash
cd /home/ubuntu/E-PILOTE-CONGO/epilote-kmp
DISPLAY=:0 ./gradlew :desktopApp:run -Dskiko.renderApi=SOFTWARE_FAST
```
- `SOFTWARE_FAST` is required because there is no GPU/OpenGL driver in the headless environment
- The desktop app connects to `http://localhost:8080` by default (configurable via `EPILOTE_BACKEND_URL` env var)

## Test Account
- Email: `super@admin.cg`
- Password: `Admin@2024!`
- Role: SUPER_ADMIN

## Login API Verification
```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"super@admin.cg","password":"Admin@2024!"}' | python3 -m json.tool
```
Expected: JSON with `accessToken`, `role: "SUPER_ADMIN"`, `userId: "user::super-admin"`

**Important**: The login endpoint is `/api/auth/login`, NOT `/api/admin/login`.

## E2E Test Flow (Desktop GUI)

1. **Login**: Type `super@admin.cg` in email field, `Admin@2024!` in password field, click "Ouvrir une session"
   - Expected: Dashboard with "Tableau de bord" title, "Bienvenue, Ramsès II", "En ligne" badge
   - Sidebar sections: PILOTAGE, ORGANISATION, CONFIGURATION, MONÉTISATION, COMMUNICATION, SUPPORT, AUDIT, PARAMÈTRES

2. **Messagerie**: Click "Messagerie" in sidebar under COMMUNICATION
   - Expected: Title "Messagerie", KPI row (Communications, Réception, Envoyés, Corbeille)
   - 5 tabs: Boîte de réception, Boîte d'envoi, Annonces officielles, Archives, Corbeille

3. **Send Message**: Click "Nouveau message" → fill subject and message → click "Envoyer"
   - Default target: "Tous les groupes scolaires"
   - Expected: Green banner "Message envoyé avec succès", dialog closes

4. **Verify Sent**: Click "Boîte d'envoi" tab
   - Expected: Sent message appears with subject, action buttons (Répondre, Archiver, Supprimer)

5. **Navigation Regression**: Click different sidebar items (Groupes scolaires, Tableau de bord)
   - Expected: Each screen loads without crash, sidebar highlights active item

## Unit Tests
```bash
cd /home/ubuntu/E-PILOTE-CONGO/epilote-backend
./gradlew clean test
```
Expected: 23/23 tests pass (RateLimitFilterTest, JwtServiceTest, AdminRealtimePathSupportTest)

## Common Issues
- **Backend won't start**: Check all env vars are set (CB_CONNECTION_STRING, CB_USERNAME, CB_PASSWORD, CB_BUCKET, JWT_SECRET)
- **Desktop compilation error**: Check for missing imports (e.g. `kotlinx.coroutines.flow.drop`)
- **Skiko OpenGL error**: Use `-Dskiko.renderApi=SOFTWARE_FAST`
- **CouchbaseLite native lib error**: Create ICU symlinks (see above)
- **Couchbase timeout**: VM IP needs whitelisting in Capella
