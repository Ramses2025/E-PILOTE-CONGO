# E-PILOTE CONGO — Révision Critique de l'Architecture (Contexte Terrain Africain)

Analyse critique honnête de l'architecture v1, identification des faiblesses réelles pour un déploiement terrain au Congo Brazzaville, et proposition d'une architecture améliorée orientée robustesse terrain.

---

## PRÉAMBULE — Ce que l'architecture v1 a sous-estimé

L'architecture v1 a été conçue avec une vision "cloud-first avec fallback offline". Elle est théoriquement solide mais **trop optimiste** sur 5 points critiques pour le Congo Brazzaville :

1. Elle suppose une reconnexion régulière au cloud (toutes les heures/jours)
2. Elle sous-estime la collaboration **intra-école sans internet**
3. Elle sur-dimensionne l'infrastructure cloud pour un pays à déploiement progressif
4. Elle ignore la réalité des **mises à jour applicatives** en zone sans internet
5. Elle suppose que les administrateurs de groupe auront toujours accès au cloud

---

## A. ROBUSTESSE OFFLINE RÉELLE

### Faiblesses identifiées

**1. Collaboration intra-école non couverte**
La v1 ne traite que la sync local→cloud. Mais dans une école de 20 enseignants, tous offline pendant 3 jours, leurs Couchbase Lite sont des îles. Un directeur d'école ne peut pas consulter les absences saisies par un surveillant sur une autre machine, même s'ils sont dans la même salle.

**2. La "sync queue" n'est pas suffisante**
Une simple queue de documents en attente ne gère pas :
- Les dépendances entre documents (ex: saisir une note pour un élève qui a été transféré offline)
- L'ordre des opérations (un delete puis un insert du même doc)
- La queue qui grossit sans limite pendant 7+ jours offline

**3. Dépendance implicite au cloud au démarrage**
La v1 dit "un premier login en ligne est acceptable" — mais si le déploiement initial se fait dans une zone sans internet, l'app ne peut pas être provisionnée du tout. C'est un bloquant terrain réel.

**4. Token offline de 7 jours — insuffisant**
Dans certaines zones rurales du Congo Brazzaville, les écoles peuvent être sans internet pendant 2 à 4 semaines. Une expiration à 7 jours force des utilisateurs à ne plus pouvoir travailler.

### Corrections proposées

```
Architecture corrigée — Offline intra-école :

┌─────────────────────────────────────────────────┐
│               ÉCOLE (LAN local ou P2P)          │
│                                                 │
│  [PC Directeur]  ←──── LAN WiFi ────►  [Tablette Enseignant] │
│  CBLite Master          P2P sync         CBLite Satellite     │
│  (source vérité école)  ◄──────────►    (sync locale)        │
└─────────────────────────────────────────────────┘
         │ (quand internet dispo)
         ▼
    Sync Gateway → Capella
```

- **Nœud maître école** : le PC du directeur/secrétaire tourne en mode "école gateway" — expose une API locale (HTTP sur LAN) que les autres appareils de l'école peuvent syncer
- **Sync P2P intra-école** : Couchbase Lite supporte la sync peer-to-peer (depuis CBLite 3.x) via `MessageEndpointListener` — à exploiter
- **Token offline étendu à 30 jours** avec rotation automatique à la reconnexion

**Provisionnement offline initial** : package d'installation USB/SD contenant :
- L'APK / installer Desktop
- Un seed database avec modules, plans, structure de l'école (généré par l'admin central)
- Clé de déchiffrement temporaire + certificat école valable 90 jours

---

## B. SYNCHRONISATION DISTRIBUÉE

### Faiblesses identifiées

**1. Sync Gateway comme SPOF (Single Point of Failure)**
La v1 déploie Sync Gateway sur Capella. Si Capella est indisponible (maintenance, panne réseau nationale), AUCUNE école ne peut syncer. En Congo Brazzaville, les pannes d'infrastructure cloud peuvent durer plusieurs jours.

**2. Le retry exponentiel est insuffisant**
30s → 2m → 10m → 1h : après 1h, le système arrête d'essayer ? La v1 ne définit pas ce qui se passe après `retryCount` maximal. Des données peuvent rester bloquées indéfiniment.

**3. Pas de gestion du volume de la queue**
Si une école travaille 3 semaines offline avec 20 enseignants, la queue de sync peut contenir 10 000+ documents. La reconnexion va déclencher un "sync tsunami" qui sature le Sync Gateway et la bande passante disponible.

**4. Delta sync — hypothèse trop optimiste**
Le delta sync de Sync Gateway fonctionne bien pour des mises à jour fréquentes de petits documents. Mais si un document n'a pas été synced depuis 3 semaines, il n'y a pas de delta — c'est un full document transfer pour chaque item en queue.

### Corrections proposées

```
Architecture sync corrigée :

NIVEAU 0 : P2P intra-école (CBLite ↔ CBLite, LAN)
     ↓
NIVEAU 1 : Sync Gateway régional (optionnel, sur serveur local province)
     ↓
NIVEAU 2 : Couchbase Capella national (cloud)
```

- **Sync Gateway régional** : serveur physique déployé dans les grandes villes (Brazzaville, Pointe-Noire) — les écoles de la province syncentavec lui en priorité, lui sync avec Capella quand il a internet
- **Throttling du sync tsunami** : lors de la reconnexion, pousser max 100 documents/min, prioriser les données critiques (absences, notes finales) avant les données secondaires (logs, stats)
- **Retry illimité avec backoff plafonné à 6h** — jamais abandonner un document, jamais le perdre
- **Compression ZSTD** des payloads de sync avant envoi réseau

```kotlin
// SyncQueueManager corrigé
class SyncQueueManager {
    fun nextRetryDelay(retryCount: Int): Duration {
        val base = when {
            retryCount < 5  -> 30.seconds
            retryCount < 10 -> 5.minutes
            retryCount < 20 -> 1.hours
            else            -> 6.hours  // plafond, jamais abandonné
        }
        return base + Random.nextInt(30).seconds  // jitter pour éviter thundering herd
    }

    fun priorityScore(item: SyncQueueItem): Int = when (item.collection) {
        "notes_finales", "bulletins" -> 100   // critique — envoyé en premier
        "absences"                   -> 80
        "notes_cours"                -> 60
        "logs", "stats"              -> 10    // non-critique — envoyé en dernier
    }
}
```

---

## C. GESTION DES CONFLITS

### Faiblesses identifiées

**1. Le merge custom sur les notes est sous-spécifié**
La v1 dit "merge custom" mais ne définit pas : que se passe-t-il si deux enseignants ont modifié la note du même élève dans la même matière, offline, pendant 3 jours ? Le merge "par élève" ne résout pas ce conflit atomique.

**2. Conflits non détectables sans vecteur d'horloge**
La v1 utilise `updatedAt` (timestamp Unix). En mode offline multi-appareils, deux machines peuvent avoir des horloges décalées (NTP non disponible), rendant la comparaison de timestamps non fiable.

**3. Les conflits utilisateur final ne sont pas adressés**
Qui décide en cas de conflit sur une note ? L'enseignant ? Le directeur ? La v1 expose les conflits dans `_default.conflicts` mais sans workflow de résolution métier.

**4. Conflits de structure (schéma) ignorés**
Si un admin met à jour la liste des matières d'une classe pendant qu'un enseignant saisit des notes offline pour une ancienne liste de matières — conflit de structure non géré.

### Corrections proposées

**Vecteurs d'horloge (Vector Clocks) pour les documents critiques**
```json
{
  "_id": "note::eco001::3A::math::eleve042",
  "type": "note",
  "valeur": 14.5,
  "saisieParUserId": "user::u001",
  "_vectorClock": {
    "user::u001::device001": 7,
    "user::u003::device002": 3
  },
  "_baseRevision": "abc123",
  "updatedAt": 1725172200000
}
```

**Règles de résolution métier explicites**
```kotlin
sealed class ConflictPolicy {
    // Notes de cours : l'enseignant propriétaire de la matière gagne
    object TeacherOwnsSubject : ConflictPolicy()
    // Bulletins finaux : le directeur gagne toujours
    object DirectorWins : ConflictPolicy()
    // Absences : union des deux sets (jamais perdre une absence signalée)
    object UnionMerge : ConflictPolicy()
    // Config école : version cloud gagne (admin groupe a autorité)
    object CloudWins : ConflictPolicy()
    // Conflit non résolvable : mettre en quarantaine, alerter le directeur
    object Quarantine : ConflictPolicy()
}
```

**Workflow de résolution pour les conflits Quarantaine**
- Document conflictuel mis dans `_default.quarantine`
- Notification au directeur d'école à la prochaine connexion
- Interface de résolution manuelle dans l'app (côte à côte, choisir la version)
- SLA : résolution obligatoire avant impression des bulletins

---

## D. MODÉLISATION DES DONNÉES

### Faiblesses identifiées

**1. Documents trop gros pour la sync**
Un document `school::eco001` qui embarque `effectif: 450` et une liste de classes sera rechargé en entier à chaque modification mineure. Sur une connexion 2G congolaise (50–150 kbps), un document de 50KB = 3–8 secondes de transfert.

**2. `modulesAccess` dupliqué dans User ET Profil**
Si un profil change, il faut mettre à jour tous les utilisateurs qui ont ce profil. La v1 dénormalise `modulesAccess` dans le document User — cela crée des incohérences potentielles offline.

**3. Absence de versioning de schéma**
Aucun champ `schemaVersion` dans les documents. Quand l'app est mise à jour avec un nouveau schéma, les anciens documents offline ne peuvent pas être migrés automatiquement.

**4. Pas de TTL / archivage sur les données historiques**
Les notes de 2020 restent dans la base locale indéfiniment. Sur des machines à 32GB de stockage, après 5 ans d'utilisation, la base locale devient ingérable.

### Corrections proposées

**Décomposer les gros documents**
```json
// Au lieu d'un document école monolithique, décomposer :
{ "_id": "school::eco001::meta", "type": "school_meta", "nom": "...", "province": "..." }
{ "_id": "school::eco001::config", "type": "school_config", "planId": "...", "modulesEnabled": [...] }
{ "_id": "school::eco001::stats::2024", "type": "school_stats", "annee": 2024, "effectif": 450 }
```

**Résoudre la duplication profil/user**
```json
// User ne stocke QUE profilId, jamais modulesAccess
{
  "_id": "user::u001",
  "profilId": "prof003",
  // ❌ SUPPRIMÉ: "modulesAccess": [...]
  // L'app résout les permissions en jointure locale CBLite
}
```

**Versioning de schéma obligatoire**
```json
{
  "_id": "note::...",
  "_schemaVersion": 2,
  "_migratedAt": 1725172200000
}
```

**TTL et archivage glissant**
- Données de l'année scolaire courante : stockage local complet
- Données N-1 : résumés uniquement (moyennes, bulletins finaux)
- Données N-2 et plus : cloud uniquement, accessible à la demande

---

## E. SCALABILITÉ RÉELLE

### Faiblesses identifiées

**1. Sync Gateway est le goulot d'étranglement**
La v1 déploie Sync Gateway en cluster HA — mais sur Capella, le nombre de connexions WebSocket simultanées est limité par le plan souscrit. Avec 10 000 écoles qui se reconnectent toutes en même temps après une panne nationale, le Sync Gateway est saturé.

**2. Kubernetes est surdimensionné pour un MVP**
Un cluster K8s pour 10 écoles pilotes au Congo Brazzaville = complexité opérationnelle excessive pour une équipe gouvernementale locale qui doit maintenir le système.

**3. Pas de stratégie de déploiement progressif géographique**
La v1 déploie tout en cloud global. Elle ne prévoit pas un déploiement région par région qui permettrait d'absorber la charge progressivement.

### Corrections proposées

**Déploiement progressif en 3 cercles**
```
Cercle 1 (MVP) : Brazzaville ville
  → 1 Sync Gateway sur 1 VPS simple (2 vCPU, 4GB RAM)
  → Spring Boot monolithe (pas de microservices)
  → ~100 écoles maximum

Cercle 2 (V1) : Brazzaville + Pointe-Noire
  → Sync Gateway régional dans chaque ville
  → Spring Boot avec load balancer simple (Nginx)
  → ~1 000 écoles

Cercle 3 (V2) : National
  → Sync Gateway cluster sur Capella
  → Spring Boot horizontal + Redis + K8s
  → ~10 000+ écoles
```

**Remplacer K8s par Docker Compose pour le MVP**
```yaml
# docker-compose.yml — suffisant pour Cercle 1
services:
  backend:
    image: epilote-backend:latest
    restart: always
  sync-gateway:
    image: couchbase/sync-gateway:3.x
    restart: always
  nginx:
    image: nginx:alpine
    restart: always
```

---

## F. SÉCURITÉ EN MODE OFFLINE PROLONGÉ

### Faiblesses identifiées

**1. La clé de chiffrement CBLite est dérivable**
`userId + deviceId + serverSalt` — si quelqu'un connaît l'userId (souvent visible dans les logs) et récupère le deviceId (accessible via ADB ou propriétés système), la clé peut être reconstruite sans le salt serveur. Le salt doit être **non récupérable sans authentification**.

**2. Token offline 7 jours — risque de vol**
Un appareil volé avec un token offline valide permet l'accès à toutes les données de l'école pendant 7 jours sans pouvoir révoquer à distance. En zone sans internet, la révocation est impossible.

**3. Pas de politique de verrouillage de session**
Si un enseignant oublie son appareil ouvert dans une salle de classe, n'importe qui peut accéder aux données.

**4. Chiffrement CBLite AES-256 — suffisant mais la clé maître est faible**
Dériver la clé depuis des informations partiellement publiques (userId) est une mauvaise pratique cryptographique.

### Corrections proposées

**PBKDF2 pour la dérivation de clé**
```kotlin
fun deriveEncryptionKey(userPin: String, deviceId: String, salt: ByteArray): DatabaseEncryptionKey {
    val spec = PBEKeySpec(userPin.toCharArray(), salt + deviceId.toByteArray(), 100_000, 256)
    val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    return DatabaseEncryptionKey(key)
}
```
- La clé dépend du **PIN utilisateur** (connu seulement de lui) + salt stocké en Keystore Android / Windows DPAPI
- Sans le PIN, impossible de dériver la clé même avec accès physique à l'appareil

**Politique de verrouillage automatique**
- Verrouillage après 15 minutes d'inactivité
- Déverrouillage par PIN ou biométrie (ré-dérive la clé CBLite)
- Token offline invalidé localement après 3 tentatives PIN échouées

**Token offline à validité étendue mais révocation locale**
- Durée : 30 jours (adapté au terrain)
- Révocation offline : liste noire locale synchronisée à chaque reconnexion
- En cas de vol signalé : l'admin peut générer un "poison document" qui se sync et invalide tous les tokens de l'appareil volé dès reconnexion

---

## G. COMPLEXITÉ INUTILE

### Ce qui est surdimensionné dans la v1

| Composant v1 | Verdict | Simplification |
|---|---|---|
| Kubernetes (MVP) | ❌ Surdimensionné | Docker Compose jusqu'à V2 |
| Redis Cache | ⚠️ Prématuré au MVP | Intégrer après 1 000 écoles |
| GraphQL V2 | ✅ Justifié | Conserver |
| Scope Couchbase par groupe | ⚠️ Complexe | 1 bucket + filtrage applicatif MVP |
| OpenTelemetry + Grafana MVP | ❌ Trop tôt | Logs simples + alertes email MVP |
| Sync Gateway cluster HA (MVP) | ❌ Surdimensionné | 1 instance suffit pour < 500 écoles |

### Ce qui manque dans la v1 et doit être ajouté

| Manque | Priorité | Justification |
|---|---|---|
| Sync P2P intra-école (CBLite) | 🔴 Critique | Collaboration sans internet |
| Nœud Sync Gateway régional | 🔴 Critique | Résilience vs panne cloud |
| Package de provisionnement offline | 🔴 Critique | Déploiement en zone sans internet |
| Schéma versionné + migration | 🟡 Important | Évolution sans casse |
| Politique de verrouillage + PIN | 🟡 Important | Sécurité terrain réelle |
| TTL et archivage glissant | 🟡 Important | Gestion stockage long terme |
| Workflow résolution conflits (UI) | 🟡 Important | Métier, pas juste technique |

---

## ARCHITECTURE AMÉLIORÉE V2 — Orientée Terrain Congo Brazzaville

```
┌──────────────────────────────────────────────────────────────────┐
│                    CLOUD NATIONAL                                 │
│   Spring Boot (VPS simple MVP → K8s V2)                         │
│   Couchbase Capella (bucket epilote_prod)                        │
│   Sync Gateway National                                          │
└──────────────────────────┬───────────────────────────────────────┘
                           │ internet (quand dispo)
┌──────────────────────────▼───────────────────────────────────────┐
│              NŒUD RÉGIONAL (Brazzaville / Pointe-Noire)          │
│   Sync Gateway Régional (serveur physique province)              │
│   Couchbase Server Node (cache régional, réplique partielle)     │
│   Sync avec cloud toutes les heures si internet dispo            │
└──────────────────────────┬───────────────────────────────────────┘
                           │ réseau local / 3G/4G provincial
┌──────────────────────────▼───────────────────────────────────────┐
│                       ÉCOLE                                       │
│                                                                   │
│  [Serveur école = PC directeur]  ←── LAN WiFi ──►  [Appareil N] │
│   CBLite + MessageEndpointListener                 CBLite Client  │
│   Seed DB provisionnée offline                     sync P2P       │
│   Token école 30 jours                                            │
└──────────────────────────────────────────────────────────────────┘
```

### Changements structurels par rapport à la v1

| Aspect | V1 | V2 Améliorée |
|---|---|---|
| Sync intra-école | ❌ Absent | ✅ CBLite P2P (MessageEndpoint) |
| Niveau sync | 2 (local → cloud) | 3 (local → régional → cloud) |
| Provisionnement | Login en ligne requis | ✅ Package USB/SD offline |
| Token offline | 7 jours | ✅ 30 jours + révocation locale |
| Clé chiffrement | userId+deviceId | ✅ PBKDF2 + PIN utilisateur |
| Conflits | Merge simple | ✅ Vector clocks + quarantaine métier |
| Taille documents | Monolithiques | ✅ Décomposés (small docs) |
| Infra MVP | K8s | ✅ Docker Compose VPS simple |
| Archivage | ❌ Absent | ✅ TTL glissant par année scolaire |
| Verrouillage | Biométrie seule | ✅ PIN + biométrie + timeout 15min |

---

## RECOMMANDATION FINALE

L'architecture v1 est **correcte sur le papier** mais **fragile sur le terrain africain**. Les corrections prioritaires avant tout développement :

1. **Ajouter la sync P2P intra-école** (CBLite MessageEndpointListener) — non négociable
2. **Simplifier l'infra MVP** (Docker Compose, pas K8s) — réduire la charge opérationnelle
3. **Concevoir le provisionnement offline dès le départ** — sinon le déploiement rural est impossible
4. **Étendre le token offline à 30 jours** avec mécanisme de révocation locale
5. **Revoir la dérivation de clé** avec PBKDF2 + PIN — la sécurité terrain en dépend

Ces 5 points doivent être des **requirements MVP**, pas des optimisations V2.

*Avril 2026 — Révision critique validée, architecture terrain Congo Brazzaville*
