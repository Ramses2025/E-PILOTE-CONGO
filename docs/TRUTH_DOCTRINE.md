# E-PILOTE — Doctrine de vérité par entité

> Ce document est la référence officielle pour identifier la source de vérité (SoT)
> de chaque type de données dans le système E-PILOTE.
> Toute implémentation doit s'y conformer strictement.

## Principes généraux

| Principe | Règle |
|---|---|
| **Offline-first** | L'opérationnel école vit d'abord en local (Couchbase Lite). |
| **Sync = convergence** | App Services/Capella est le lieu de convergence, pas la vérité. |
| **Backend = gouvernance** | Le backend Spring Boot est la vérité pour l'auth, les licences, et l'admin. |
| **UI = projection** | L'UI ne détient jamais de vérité ; elle projette la DB locale + status sync. |

## Source de vérité par type de donnée

| Entité | Source de vérité | Stockage | Sync ? | Notes |
|---|---|---|---|---|
| **Utilisateurs (auth)** | Backend | Couchbase Capella `users` | ❌ Non | Gérée par REST uniquement. Local = cache session. |
| **Sessions / Tokens** | Backend (émetteur) | Local (UserSessionRepository) | ❌ Non | JWT signé par backend, stocké localement, jamais synchronisé. |
| **Groupes scolaires** | Backend (SUPER_ADMIN) | Capella `school_groups` | ❌ Non | Seed initial via REST, cache local. |
| **Écoles (création)** | Backend (ADMIN_GROUPE) | Capella `schools` | ❌ Non | Créées via REST, distribuées en seed package. |
| **Élèves** | Local (école) | Couchbase Lite `students` | ✅ `sch::{schoolId}` | Offline-first, sync vers Capella. |
| **Notes/Grades** | Local (école) | Couchbase Lite `grades` | ✅ `sch::{schoolId}` | Offline-first. |
| **Absences** | Local (école) | Couchbase Lite `attendances` | ✅ `sch::{schoolId}` | Offline-first. |
| **Classes** | Local (école) | Couchbase Lite `academic_config` | ✅ `sch::{schoolId}` | Type `class`. |
| **Matières** | Local (école) | Couchbase Lite `academic_config` | ✅ `sch::{schoolId}` | Type `subject`. |
| **Personnel** | Local (école) | Couchbase Lite `staff` | ✅ `sch::{schoolId}` | Offline-first. |
| **Emploi du temps** | Local (école) | Couchbase Lite `timetable` | ✅ `sch::{schoolId}` | Offline-first. |
| **Bulletins** | Local (école) | Couchbase Lite `report_cards` | ✅ `sch::{schoolId}` | Offline-first. |
| **Disciplines** | Local (école) | Couchbase Lite `disciplines` | ✅ `sch::{schoolId}` | Offline-first. |
| **Inscriptions** | Local (école) | Couchbase Lite `inscriptions` | ✅ `sch::{schoolId}` | Offline-first. |
| **Annonces** | Local (école) | Couchbase Lite `announcements` | ✅ `sch::{schoolId}` | Offline-first. |
| **Messages** | Local | Couchbase Lite `messages` | ✅ `sch::{schoolId}` | Offline-first. |
| **Notifications** | Local | Couchbase Lite `notifications` | ✅ `sch::{schoolId}` | Offline-first. |
| **Factures / Paiements** | Non défini (v2) | Couchbase Lite (local only) | ❌ Non | Pas de cycle client complet ; retiré du sync v1. |
| **Salaires / Budgets** | Non défini (v2) | Couchbase Lite (local only) | ❌ Non | Idem. |

## Règles de flux

### Écriture
```
UI → ViewModel → UseCase → Repository.save() → Couchbase Lite (local)
                                                 ↓ (si online)
                                           App Services push
                                                 ↓
                                           Capella (convergence)
```

### Lecture
```
Couchbase Lite (local) → Flow/LiveQuery → ViewModel → UI
```

### Auth
```
UI → ViewModel → AuthApiService (REST) → Backend (vérité)
                                            ↓
                                     JWT + syncToken retournés
                                            ↓
                                  UserSessionRepository (cache local)
                                            ↓
                                  SyncManager (démarre réplication)
```

## Canaux de synchronisation

| Canal | Audience autorisée |
|---|---|
| `sch::{schoolId}` | Tout utilisateur de cette école |
| `sch::{schoolId}::admin` | DIRECTOR de cette école |
| `grp::{groupId}` | ADMIN_GROUPE, SUPER_ADMIN uniquement |
| `grp::{groupId}::admin` | ADMIN_GROUPE uniquement |

**Interdit** : un utilisateur de type USER, TEACHER, ACCOUNTANT ne reçoit jamais `grp::*`.

## Conventions de nommage

| Champ canonique | Alias legacy (lecture seule) |
|---|---|
| `schoolId` | `ecoleId` |
| `groupId` | `groupeId` |
| `students` (collection) | — |
| `grades` (collection) | — |

Tous les nouveaux documents portent `schemaVersion: 1` (cf. `SchemaContract.kt`).
