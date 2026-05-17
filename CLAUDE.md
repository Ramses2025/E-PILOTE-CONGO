# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

E-PILOTE CONGO is a school management platform for Congo-Brazzaville. It is composed of two independent Gradle projects:

- **`epilote-backend/`** — Spring Boot 3 / Kotlin REST API (the admin and auth plane)
- **`epilote-kmp/`** — Kotlin Multiplatform project with a Compose Desktop admin client (`desktopApp`) and an Android app (`androidApp`), sharing business logic via the `shared` module

## Commands

All commands are run from within the subproject directory.

### Backend (`epilote-backend/`)

```bash
./gradlew build          # Compile + test
./gradlew test           # Run all tests
./gradlew test --tests "cg.epilote.backend.admin.AdminControllerSecurityAnnotationsTest"  # Single test class
./gradlew bootRun        # Start server on :8080
```

Required environment variables for `bootRun`:
```
CB_CONNECTION_STRING, CB_USERNAME, CB_PASSWORD, CB_BUCKET (default: epilote_prod)
JWT_SECRET
SUPER_ADMIN_EMAIL, SUPER_ADMIN_PASSWORD
APP_SERVICES_ADMIN_URL, APP_SERVICES_ADMIN_USER, APP_SERVICES_ADMIN_PASSWORD
```

### KMP Desktop app (`epilote-kmp/`)

```bash
./gradlew :desktopApp:run                # Run Compose Desktop app
./gradlew :desktopApp:test               # Run desktop tests
./gradlew :desktopApp:packageMsi         # Build Windows installer
./gradlew :desktopApp:packageExe         # Build Windows EXE
```

Couchbase Lite requires native JNI libraries. Set the path via Gradle property or env var:
```
EPILOTE_CBLITE_LIB_DIR=/path/to/cblite/native/libs
# or in epilote-kmp/gradle.properties:
epilote.cblite.lib.dir=/path/to/cblite/native/libs
```

## Architecture

### Two data planes

The system has two entirely separate data planes that must not be confused:

**Admin plane** (Spring Boot ↔ Couchbase Capella via Kotlin SDK):
- Auth, groups, schools, subscriptions, invoices, platform config
- REST-only; never touches Couchbase Lite
- `AdminRepository` and its sibling repositories read/write directly to Capella collections using N1QL (`scope.query(...).execute()`)

**Operational plane** (Couchbase Lite ↔ Couchbase App Services ↔ Capella):
- Students, grades, absences, timetable, bulletins, inscriptions, disciplines, messages
- Offline-first: UI writes to local CBLite first, `SyncManager` replicates via App Services
- `SyncManager` (`shared/data/sync/`) drives replication using `BasicAuthenticator` with a `syncToken` provisioned by the backend at login

### Backend package structure

```
cg.epilote.backend
├── auth/          # JWT (JwtService), filter (JwtAuthFilter), UserRepository, AuthService
├── admin/         # All super-admin and groupe-admin REST controllers + repositories
│   └── quota/     # QuotaGuard — enforces plan limits
├── ai/            # AIController + AIService (Mistral/OpenAI via OkHttp3)
└── config/        # Spring Boot config beans
    ├── CouchbaseConfig.kt        # Cluster + Bucket beans
    ├── CollectionBootstrap.kt    # Auto-creates collections & N1QL indexes at startup
    ├── SecurityConfig.kt         # Spring Security filter chain
    ├── TenantGuard.kt            # Filter: ADMIN_GROUPE can only access their own groupId
    └── SuperAdminInitializer.kt  # Seeds first SUPER_ADMIN on startup
```

Key backend patterns:
- Couchbase access uses the **Kotlin SDK** directly (`com.couchbase.client:kotlin-client`), not Spring Data Couchbase. All queries go through `scope.query(statement, parameters).execute()`.
- Document IDs follow `type::uuid` (e.g. `groupe::uuid`, `user::uuid`, `inv_plat::uuid`).
- Legacy field aliases exist for backward compat: `groupeId` → `groupId`, `ecoleId` → `schoolId`. New documents always use canonical names; reads must tolerate both via `IFMISSINGORNULL()` in N1QL or Kotlin's `?: ` fallback.
- The `_default` scope is used for all collections. `CollectionBootstrap` ensures all required collections exist idempotently at startup — add any new collection there.
- Security is layered: URL-level rules in `SecurityConfig` → `TenantGuard` filter for ADMIN_GROUPE path enforcement → `@PreAuthorize` on controller methods for fine-grained per-endpoint control.
- Three roles: `SUPER_ADMIN`, `ADMIN_GROUPE`, `USER`. At login, `AuthService.enforceSubscriptionAccess()` gates ADMIN_GROUPE and USER behind an active subscription check.

### KMP structure

```
epilote-kmp/
├── shared/          # Common Kotlin (Android + Desktop)
│   └── commonMain/
│       ├── data/local/      # CBLite repositories (EleveRepo, NoteRepo, etc.)
│       ├── data/remote/     # Ktor-based ApiClient + AuthApiService
│       ├── data/sync/       # SyncManager, EpiloteConflictResolver
│       ├── domain/model/    # Domain data classes
│       ├── domain/usecase/  # Business use cases (Login, SaveNote, SaveAbsence, …)
│       └── presentation/viewmodel/  # ViewModels shared across platforms
├── desktopApp/      # Compose Desktop (JVM target)
│   └── data/
│       ├── DesktopAdminClient.kt         # Ktor HTTP client wrapping backend REST API
│       ├── DesktopAdminClientApis.kt     # Extension functions per endpoint
│       ├── AdminDataRepository.kt        # SUPER_ADMIN operations
│       └── GroupeAdminDataRepository.kt  # ADMIN_GROUPE operations
└── androidApp/      # Android target (Compose Android)
```

The desktop app has two distinct user modes:
- **SUPER_ADMIN** — `SuperAdminDesktopScreenContent` uses `AdminDataRepository` → `DesktopAdminClient` → backend REST API
- **ADMIN_GROUPE / USER** — uses `shared` ViewModels → CBLite local DB → App Services sync

### Truth doctrine (from `docs/TRUTH_DOCTRINE.md`)

| Data type | Source of truth | Sync? |
|---|---|---|
| Auth, sessions, tokens | Backend / JWT | No |
| Groups, schools (creation) | Backend REST | No |
| Students, grades, absences, bulletins, staff | Couchbase Lite local | Yes (App Services) |
| Invoices, subscriptions, plans | Backend REST | No |

Write flow for operational data: `UI → ViewModel → UseCase → CBLite → App Services → Capella`
Read flow: `CBLite → Flow/LiveQuery → ViewModel → UI`

### Realtime (SSE)

`AdminRealtimeBroker` holds `SseEmitter` connections for the desktop admin UI. Any controller can inject it and call `broker.publish(event)` to push updates. The broker buffers up to 512 events for reconnect replay.

## Règles de gestion du contexte

**Seuils d'alerte (Claude doit surveiller proactivement) :**
- À **50%** de contexte → mettre à jour CLAUDE.md avec l'état actuel
- À **70%** de contexte → prévenir l'utilisateur et proposer de compacter
- À **80%** de contexte → compacter automatiquement via `/compact`

**Avant chaque /compact :**
1. Écrire un résumé dans CLAUDE.md (état, décisions, tâches en cours, bugs connus)
2. Commit Git : `git commit -m "chore: save context before compact"`
3. Lancer : `/compact "Garde : architecture, décisions techniques, tâches en cours, bugs connus. Résume le reste."`

**Après chaque /compact :**
- Confirmer à l'utilisateur que c'est fait + indiquer le nouveau % de contexte
- Reprendre exactement où on en était

**Commande rapide :** `/compact-save` — sauvegarde + commit + compact en une seule action.

---

## Key rules (from `.windsurf/rules/regle.md`)

Always consult official documentation for **Kotlin Multiplatform**, **Spring Boot (Kotlin)**, and **Couchbase (Capella / App Services)** before implementing. Follow documented APIs, patterns, and best practices only — do not invent implementations when a documented approach exists.

---

## Rôles utilisateurs

| Rôle | Accès | Endpoints |
|---|---|---|
| **SUPER_ADMIN** | Tout — plateforme globale | `/api/super-admin/**` |
| **ADMIN_GROUPE** | Son groupe uniquement (TenantGuard) | `/api/groupes/{groupeId}/**` |
| **USER** | Données métier via CBLite (pas de REST direct) | Sync Gateway seulement |

**SUPER_ADMIN** : crée groupes, plans, abonnements, factures, enregistre paiements, gère audit, annonces, messages.
**ADMIN_GROUPE** : crée écoles (quota), utilisateurs (quota), profils, demande renouvellement abonnement, consulte dashboard groupe.
**USER** : saisit notes/absences/bulletins via Android en offline-first.

Comptes actifs : `super@admin.cg` (SUPER_ADMIN), `eveque@epilote.cg` (SUPER_ADMIN), 3 ADMIN_GROUPE (rose@, marien@, edja@epilote.cg).

## État actuel

**✅ Fonctionne :**
- Backend Spring Boot opérationnel sur :8080
- Connexion Couchbase Capella active (bucket `epilote_prod`, 38 collections)
- Desktop Compose lancé, login SUPER_ADMIN fonctionnel
- Auth JWT + TenantGuard + QuotaGuard
- 6 groupes scolaires réels, 6 utilisateurs
- Facturation : 2 factures, 1 vrai paiement (MARIEN NGOUABI, 900 000 XAF)
- Audit logs actif (248 entrées)
- MCP Couchbase installé (`claude mcp add couchbase`)

**🟡 En cours / Partiel :**
- Collection legacy `groupes` contient 1 doc de test ("teste") à supprimer via N1QL direct (pas d'endpoint API)
- Android non testé

## Prochaines étapes

1. **Supprimer le doc "teste" legacy** : La collection `groupes` (legacy) contient 1 doc de test. Nécessite N1QL direct ou MCP Couchbase (`DELETE FROM groupes WHERE nom = "teste"`).

2. **Tester le login ADMIN_GROUPE** : Vérifier que les 3 groupes nouvellement abonnés (ECOLE DE SCIENCE, LAMARELLE, EDJA-SEMBE) peuvent se connecter avec leurs comptes ADMIN_GROUPE.

3. **Tester l'application Android** : Valider le flux CBLite → App Services → Capella sur Android.
