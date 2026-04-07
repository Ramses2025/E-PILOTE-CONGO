@'
# E-PILOTE CONGO — Feuille de Route V1 Terrain (Plan Exhaustif)
 
Plan d'implémentation global et séquencé depuis l'état actuel jusqu'à une V1 déployable sur écoles pilotes au Congo Brazzaville, couvrant backend, KMP shared, UI Android, UI Desktop, infrastructure et sécurité.
 
---
 
## ÉTAT DE DÉPART (existant au 07/04/2026)
 
### ✅ Déjà fait
- **Backend** : Auth JWT complet, CRUD groupes/écoles/users/profils/modules/plans, config Couchbase, Spring Security
- **Infra** : Docker Compose (backend + Sync Gateway + Nginx), sync-gateway-config.json, .env.example
- **KMP shared** : Models.kt, NoteRepository, AbsenceRepository, EpiloteDatabase, SyncManager, EpiloteConflictResolver, NotesViewModel, SyncIndicatorViewModel
 
### ❌ Manquant (à construire)
- build.gradle.kts KMP + dépendances CBLite
- EleveRepository, ClasseRepository, MatiereRepository, UserSessionRepository
- AuthViewModel (login flow)
- UI Android complète (Jetpack Compose)
- UI Desktop Windows complète (Compose for Desktop)
- Provisionnement : endpoint seed + génération package
- Sécurité : PBKDF2, verrouillage PIN, token refresh automatique
- Index Couchbase Capella
- Monitoring minimal (health check, logs)
 
---
 
## MODULE 1 — KMP : FONDATION (build + data layer complet)
**Durée estimée : 1 semaine**
**Dépend de : rien (base de tout)**
 
### 1.1 Build système KMP
- [ ] `epilote-kmp/build.gradle.kts` racine (multiplatform, androidApp, desktopApp)
- [ ] `epilote-kmp/shared/build.gradle.kts` (commonMain, androidMain, desktopMain)
  - Dépendances : `couchbase-lite-android`, `couchbase-lite-kotlin`, Ktor client, Kotlinx Coroutines, Kotlinx Serialization
- [ ] `epilote-kmp/androidApp/build.gradle.kts` (Compose, CBLite Android)
- [ ] `epilote-kmp/desktopApp/build.gradle.kts` (Compose Desktop, CBLite JVM)
- [ ] `epilote-kmp/settings.gradle.kts`
- [ ] `gradle/libs.versions.toml` (version catalog centralisé)
 
**⚠️ Point critique** : CBLite Android et Desktop ont des artefacts différents. L'init `CouchbaseLite.init(context)` est Android-only → abstraction `expect/actual` obligatoire.
 
### 1.2 Abstraction platform (expect/actual)
- [ ] `expect fun initCouchbaseLite(context: Any?)`
  - `androidMain` : `CouchbaseLite.init(context as Context)`
  - `desktopMain` : `CouchbaseLite.init()` (pas de context sur Desktop)
- [ ] `expect fun getPlatformName(): String`
 
### 1.3 Repositories manquants
- [ ] `EleveRepository` : save, getByClasse, observeByClasse, searchByNom
- [ ] `ClasseRepository` : save, getByEcole, observeByEcole
- [ ] `MatiereRepository` : save, getByClasse
- [ ] `UserSessionRepository` : saveSession, getSession, clearSession (tokens + profil local)
 
### 1.4 Sécurité CBLite — PBKDF2
- [ ] Remplacer le hash simple dans `EpiloteDatabase` par PBKDF2 :
  - `expect fun deriveKey(pin: String, userId: String, salt: ByteArray): ByteArray`
  - `androidMain` : `SecretKeyFactory("PBKDF2WithHmacSHA256")`, salt dans Android Keystore
  - `desktopMain` : même algo, salt dans Windows DPAPI ou fichier chiffré
- [ ] Salt généré à la première installation, jamais retransmis
- [ ] Auto-verrouillage après 15 min d'inactivité (timer partagé)
 
---
 
## MODULE 2 — BACKEND : COMPLÉMENTS
**Durée estimée : 3–4 jours**
**Dépend de : Module 1 (pour tester le provisionnement)**
 
### 2.1 Index Couchbase Capella (obligatoire pour les performances)
- [ ] Créer via MCP Couchbase ou Capella UI :
  ```sql
  CREATE INDEX idx_users_ecole    ON epilote_prod._default.users(ecoleId)    WHERE type="user";
  CREATE INDEX idx_schools_groupe ON epilote_prod._default.schools(groupeId) WHERE type="school";
  CREATE INDEX idx_profils_groupe ON epilote_prod._default.profils(groupeId) WHERE type="profil";
  CREATE INDEX idx_modules_type   ON epilote_prod._default.modules(type);
  CREATE INDEX idx_plans_type     ON epilote_prod._default.plans(type);
  ```
 
### 2.2 Endpoint provisionnement
- [ ] `POST /api/provisioning/seed/{ecoleId}` → génère un package JSON contenant :
  - données école (school, classes, élèves, profils)
  - modules du plan
  - liste des utilisateurs (sans passwordHash, avec hash PIN temporaire)
- [ ] Endpoint protégé `ADMIN_GROUPE` ou `SUPER_ADMIN`
- [ ] `GET /api/provisioning/sync-token/{userId}` → retourne le sessionToken Sync Gateway pour CBLite
 
### 2.3 Gestion Sync Gateway users
- [ ] `SyncGatewayAdminClient` : client HTTP vers port 4985 (admin API)
  - `createSgUser(userId, password, channels)` → `POST /_user/`
  - `updateSgUserChannels(userId, channels)` → `PUT /_user/{userId}`
  - Appelé automatiquement à la création d'un user via `AdminController`
- [ ] Mapping canal : user → `ecole_{ecoleId}` + `global`
 
### 2.4 Endpoint health & info
- [ ] `GET /actuator/health` (déjà configuré Spring Boot, vérifier)
- [ ] `GET /api/info` → version app, statut cluster Couchbase
 
### 2.5 Validation et gestion d'erreurs globale
- [ ] `@ControllerAdvice GlobalExceptionHandler` : codes HTTP cohérents (400, 401, 403, 404, 500)
- [ ] Validation unicité username à la création user
 
---
 
## MODULE 3 — KMP : COUCHE MÉTIER (use cases)
**Durée estimée : 4–5 jours**
**Dépend de : Module 1**
 
### 3.1 Use cases Notes
- [ ] `SaveNoteUseCase` : valide (0–20), vérifie non-verrouillé, sauvegarde local
- [ ] `GetNotesByClasseUseCase` : retourne Flow<List<Note>>
- [ ] `LockBulletinUseCase` : verrouille tous les documents d'une période → `_locked=true`
- [ ] `GetConflictsUseCase` : retourne les notes avec `requiresReview=true`
- [ ] `ResolveConflictUseCase` : choisit la version finale, retire le flag
 
### 3.2 Use cases Absences
- [ ] `SaveAbsenceUseCase` : sauvegarde, jamais de suppression physique
- [ ] `JustifyAbsenceUseCase` : passe `justifiee=true` avec motif
- [ ] `GetAbsencesByDateUseCase` : Flow par date pour appel du jour
- [ ] `GetAbsencesByEleveUseCase` : historique élève
 
### 3.3 Use cases Élèves / Classes
- [ ] `GetElevesByClasseUseCase`
- [ ] `SearchEleveUseCase` : recherche nom/prénom/matricule en local
- [ ] `GetClassesByEcoleUseCase`
 
### 3.4 Use cases Auth / Session
- [ ] `LoginUseCase` : appel API backend → sauvegarde session locale (tokens + profil)
- [ ] `LogoutUseCase` : efface session locale, arrête SyncManager
- [ ] `GetCurrentSessionUseCase` : retourne session depuis CBLite ou null
- [ ] `RefreshTokenUseCase` : rafraîchit l'access token si expiré
- [ ] `CheckSessionValidUseCase` : vérifie expiration offline token (30 jours)
 
### 3.5 ViewModels manquants
- [ ] `LoginViewModel` : état login (loading/success/error), appel LoginUseCase
- [ ] `ElevesViewModel` : liste + recherche + observeByClasse
- [ ] `AbsencesViewModel` : liste du jour, saisie, justification
- [ ] `ClassesViewModel` : liste classes de l'école
- [ ] `BulletinViewModel` : calcul moyennes, verrouillage (V1)
 
---
 
## MODULE 4 — UI ANDROID
**Durée estimée : 2–3 semaines**
**Dépend de : Modules 1, 3**
 
### 4.1 Setup Android
- [ ] `androidApp/src/main/AndroidManifest.xml`
- [ ] `androidApp/src/main/kotlin/cg/epilote/android/MainActivity.kt`
- [ ] `androidApp/src/main/kotlin/cg/epilote/android/EpiloteApp.kt` (Application class, init CBLite)
- [ ] Thème Compose : couleurs, typographie (couleurs officielles/sobres)
- [ ] Navigation : `NavHost` avec routes définies
 
### 4.2 Écran Login
- [ ] `LoginScreen.kt` : champ username + password, bouton connexion
- [ ] Indicateur chargement pendant appel réseau
- [ ] Message d'erreur clair ("Vérifiez votre connexion" si réseau absent)
- [ ] Après login : init CBLite avec PIN, démarrage SyncManager
 
### 4.3 Écran PIN (sécurité locale)
- [ ] `PinScreen.kt` : saisie PIN 4–6 chiffres après login initial
- [ ] Confirmation PIN au premier setup
- [ ] Écran de déverrouillage PIN (après 15 min inactivité)
- [ ] Compteur tentatives (blocage après 5 échecs)
 
### 4.4 Dashboard principal
- [ ] `DashboardScreen.kt` : liste des modules accessibles selon profil
- [ ] Indicateur sync permanent en haut (`SyncIndicatorViewModel`)
- [ ] Nom de l'école + utilisateur connecté
 
### 4.5 Module Notes
- [ ] `ClassesListScreen.kt` : liste des classes de l'enseignant
- [ ] `MatiereListScreen.kt` : matières de la classe sélectionnée
- [ ] `ElevesNotesScreen.kt` : liste élèves + champ note par élève
  - Saisie numérique rapide (0–20, clavier numérique)
  - Sauvegarde automatique locale à chaque saisie
  - Indicateur "non synchronisé" sur les notes en attente
- [ ] `ConflictsScreen.kt` : liste des conflits `requiresReview`, résolution manuelle
 
### 4.6 Module Absences
- [ ] `AbsencesJourScreen.kt` : date du jour, liste élèves, checkbox absent/présent
- [ ] `AbsenceHistoriqueScreen.kt` : historique par élève
- [ ] `JustificationScreen.kt` : marquer absences comme justifiées avec motif
 
### 4.7 Composants partagés Android
- [ ] `SyncBanner.kt` : bandeau 🟢🟡🔴 + message + compteur (en haut de chaque écran)
- [ ] `OfflineBadge.kt` : badge rouge "Hors ligne" visible
- [ ] `LoadingOverlay.kt`
- [ ] `ErrorSnackbar.kt`
 
---
 
## MODULE 5 — UI DESKTOP WINDOWS
**Durée estimée : 1–2 semaines**
**Dépend de : Modules 1, 3 (code partagé ~75%)**
 
### 5.1 Setup Desktop
- [ ] `desktopApp/src/main/kotlin/cg/epilote/desktop/Main.kt` : `application { Window(...) }`
- [ ] `desktopApp/build.gradle.kts` : `compose.desktop { application { mainClass = "..." } }`
- [ ] Thème Compose Desktop (même que Android)
- [ ] Navigation : état local (pas de NavHost, navigation par state)
 
### 5.2 Écrans Desktop (identiques Android, layout adapté)
- [ ] `LoginWindow.kt`
- [ ] `PinWindow.kt`
- [ ] `DashboardWindow.kt` : sidebar navigation (classes, modules)
- [ ] `NotesWindow.kt` : tableau de saisie (grille élèves × matières)
  - Avantage Desktop : vue tableau complète, impression possible
- [ ] `AbsencesWindow.kt`
- [ ] `SyncStatusBar.kt` : barre de statut en bas de fenêtre
 
### 5.3 Impression (Desktop uniquement)
- [ ] `PrintService.kt` : export PDF des notes/absences via `java.awt.print` ou iText
- [ ] Aperçu avant impression
 
**⚠️ Point critique** : L'impression est une fonctionnalité Desktop uniquement, pas partageable avec Android.
 
---
 
## MODULE 6 — INFRASTRUCTURE & DÉPLOIEMENT
**Durée estimée : 2–3 jours**
**Dépend de : Backend fonctionnel (Module 2)**
 
### 6.1 Couchbase Capella — setup initial
- [ ] Créer bucket `epilote_prod` (si pas encore fait)
- [ ] Créer collections dans `_default` scope : `users`, `schools`, `groupes`, `profils`, `modules`, `plans`, `notes`, `absences`, `eleves`, `classes`, `matieres`
- [ ] Créer les index (voir Module 2.1)
- [ ] Créer user Sync Gateway dédié avec droits limités
 
### 6.2 Sync Gateway — configuration finale
- [ ] Tester la config `sync-gateway-config.json` existante
- [ ] Ajouter gestion des erreurs dans la fonction sync JS
- [ ] Activer TLS entre Nginx et Sync Gateway (port 4984)
- [ ] Tester connexion CBLite → Sync Gateway → Capella end-to-end
 
### 6.3 VPS — déploiement
- [ ] Provisionner VPS (Ubuntu 22.04, 2 vCPU, 4GB RAM)
- [ ] Installer Docker + Docker Compose
- [ ] Générer certificat TLS (Let's Encrypt via certbot ou Nginx)
- [ ] Copier `epilote-infra/` + `.env` (rempli avec vraies valeurs)
- [ ] `docker-compose up -d`
- [ ] Vérifier health check : `curl https://domain/actuator/health`
 
### 6.4 Build & distribution app
- [ ] Script de build APK Android : `./gradlew :androidApp:assembleRelease`
- [ ] Signature APK (keystore, pas Google Play au MVP)
- [ ] Script de build Desktop : `./gradlew :desktopApp:packageMsi` (Windows installer)
- [ ] Distribution via lien de téléchargement simple (pas de store au MVP)
 
### 6.5 Monitoring minimal
- [ ] Logs Spring Boot vers fichier (rotation quotidienne)
- [ ] `GET /actuator/health` retourne état Couchbase
- [ ] Alerte email sur échec health check (simple script cron ou UptimeRobot gratuit)
 
---
 
## MODULE 7 — SÉCURITÉ FINALE
**Durée estimée : 3–4 jours**
**Dépend de : Modules 1, 4, 5**
 
### 7.1 PBKDF2 complet (remplacer hash simple)
- [ ] Implémenter `expect fun deriveKey(...)` sur Android et Desktop
- [ ] Android : salt stocké dans Android Keystore (BiometricPrompt optionnel)
- [ ] Desktop : salt stocké dans `%APPDATA%\epilote\salt.bin` chiffré Windows DPAPI
- [ ] Migration : si base existante avec ancien hash → re-dériver à la prochaine ouverture PIN
 
### 7.2 Verrouillage automatique
- [ ] Timer d'inactivité dans chaque ViewModel racine : 15 min → déclenche `LockEvent`
- [ ] Sur `LockEvent` : fermer la base CBLite, rediriger vers `PinScreen`
- [ ] Sur déverrouillage PIN correct : ré-ouvrir la base, reprendre l'état
 
### 7.3 Gestion token refresh automatique
- [ ] Intercepteur Ktor : si access token expiré (401) → appel silencieux `RefreshTokenUseCase`
- [ ] Si refresh token expiré → logout forcé + message "Session expirée, reconnectez-vous"
- [ ] Offline token : validé localement sans réseau (signature JWT)
 
### 7.4 Révocation token (appareil perdu)
- [ ] Backend : `POST /api/admin/users/{userId}/revoke` → marque tous les tokens invalidés
- [ ] Document "poison" : `{ type: "token_revocation", userId: "...", revokedAt: ... }` synced via canal global
- [ ] Client : vérifie à chaque sync si son userId est révoqué → logout forcé
 
---
 
## MODULE 8 — DONNÉES MÉTIER COMPLÉMENTAIRES
**Durée estimée : 1 semaine**
**Dépend de : Modules 1, 2, 3**
 
### 8.1 Gestion des matières et coefficients
- [ ] `MatiereRepository` : CRUD local
- [ ] Backend : `POST /api/ecoles/{id}/matieres` (admin groupe)
- [ ] Calcul du coefficient dans la moyenne
 
### 8.2 Calcul des moyennes
- [ ] `MoyenneCalculator` (shared, pure Kotlin) :
  - Moyenne par matière
  - Moyenne générale (pondérée par coefficient)
  - Rang dans la classe
- [ ] `BulletinViewModel` : agrège notes + moyennes + absences par élève/période
 
### 8.3 Périodes scolaires
- [ ] Modèle `Periode` : T1, T2, T3 ou S1/S2 (configurable par école)
- [ ] Filtrage des notes par période active
- [ ] Bascule de période (admin école)
 
### 8.4 Provisionnement initial (seed)
- [ ] Backend génère un fichier JSON seed par école
- [ ] `ProvisioningImporter` côté client : importe le JSON dans CBLite au premier démarrage
- [ ] Fallback : si pas de seed, l'app démarre vide et sync dès connexion
 
---
 
## PÉRIMÈTRE V1 — CE QUI EST INCLUS / EXCLU
 
### ✅ INCLUS dans la V1
| Fonctionnalité | Module |
|---|---|
| Login sécurisé (online) + session offline 30 jours | Auth |
| Saisie notes par enseignant | Notes |
| Saisie absences quotidiennes | Absences |
| Liste des élèves par classe | Élèves |
| Synchronisation cloud automatique | Sync |
| Indicateur sync visible (🟢🟡🔴) | UX |
| Résolution manuelle des conflits | Conflits |
| Chiffrement local AES-256 + PIN | Sécurité |
| UI Android (mobile) | Android |
| UI Desktop Windows (PC direction) | Desktop |
| Impression notes/absences (Desktop) | Impression |
| Dashboard admin groupe (web via backend API) | Admin |
| Calcul moyennes par matière et général | Métier |
 
### ❌ EXCLU de la V1 (prévu V2)
| Fonctionnalité | Raison |
|---|---|
| Génération bulletins PDF mise en page | Complexité graphique |
| Module finances / comptabilité | Hors périmètre initial |
| Dashboard Super Admin visuel | API admin suffisante pour MVP |
| Notifications push (FCM) | Non critique pour terrain |
| Statistiques nationales / rapports | V2 |
| Multi-école par utilisateur | Architecture actuelle : 1 user = 1 école |
| Biométrie (empreinte) | PIN suffit pour V1 |
 
---
 
## SÉQUENCE D'EXÉCUTION RECOMMANDÉE
 
```
SEMAINE 1 : Module 1 (KMP build + data layer + PBKDF2)
SEMAINE 2 : Module 2 (backend compléments) + Module 3 (use cases)
SEMAINE 3-4 : Module 4 (UI Android — login, dashboard, notes, absences)
SEMAINE 5 : Module 5 (UI Desktop — adaptation des écrans Android)
SEMAINE 6 : Module 6 (infra déploiement VPS + Capella setup)
SEMAINE 7 : Module 7 (sécurité finale PBKDF2 + verrouillage + refresh)
SEMAINE 8 : Module 8 (moyennes, périodes, provisionnement)
            + Tests terrain sur 3–5 écoles pilotes
            + Corrections issues terrain
```
 
**Total estimé : 8 semaines, équipe 2–3 ingénieurs**
 
---
 
## DÉPENDANCES ENTRE MODULES
 
```
Module 1 (KMP build)
    ├──► Module 3 (use cases)
    │        ├──► Module 4 (UI Android)
    │        └──► Module 5 (UI Desktop)
    └──► Module 7 (sécurité CBLite)
 
Module 2 (backend)
    ├──► Module 6 (infra)
    └──► Module 3 (provisionnement)
 
Module 8 (métier)
    └── peut être développé en parallèle de Module 4/5
```
 
---
 
## RISQUES ET POINTS DE VIGILANCE
 
| Risque | Impact | Mitigation |
|---|---|---|
| CBLite SDK KMP : artefacts Android vs Desktop différents | Bloquant build | Tester dès Module 1, prévoir `expect/actual` |
| Sync Gateway config incorrecte → données non isolées | Sécurité critique | Test end-to-end avant toute mise en prod |
| PBKDF2 : migration users existants | Perte d'accès données | Prévoir migration douce au prochain login |
| Impression PDF Desktop : dépendances Java | Complexité | Évaluer iText vs PDFBox en début de Module 5 |
| Connexion Capella instable au déploiement VPS | Bloquant démo | Tester connectivité VPS → Capella avant déploiement |
 
---
 
*Avril 2026 — Feuille de route V1 validée, 8 semaines, équipe 2–3 ingénieurs*
'@ | Set-Content -Path "C:\Users\PAROISSE DE TSIEME\.windsurf\plans\epilote-roadmap-v1-93f0b2.md" -Encoding UTF8