# Capella Setup — E-PILOTE CONGO
# À exécuter manuellement dans l'UI Couchbase Capella

---

## ÉTAPE 1 — Créer le Bucket

Dans Capella → Data Tools → Buckets → Create Bucket :

| Paramètre | Valeur |
|---|---|
| Name | `epilote_prod` |
| Type | Couchbase |
| Memory Quota | 1024 MB (ajuster selon plan) |
| Replicas | 1 |
| Durability | None (MVP) |

---

## ÉTAPE 2 — Créer les Collections (Scope : `_default`)

Dans Capella → Data Tools → Scopes & Collections → `_default` → Add Collection :

Créer dans cet ordre exact (respecte les dépendances) :

```
config
school_groups
schools
academic_config
users
staff
students
inscriptions
evaluations
grades
report_cards
timetable
attendances
staff_attendances
invoices
expenses
budgets
accounting_pieces
payslips
sync_mutations
sync_conflicts
audit_logs
ledger_entries
messages
notifications
documents
books
disciplines
transfers
```

---

## ÉTAPE 3 — Créer les Index (Data Tools → Query)

Copier-coller et exécuter chaque bloc :

### Index utilisateurs
```sql
CREATE INDEX idx_users_username  ON `epilote_prod`._default.`users`(username)  WHERE type="user";
CREATE INDEX idx_users_school    ON `epilote_prod`._default.`users`(schoolId)  WHERE type="user";
CREATE INDEX idx_users_group     ON `epilote_prod`._default.`users`(groupId)   WHERE type="user";
CREATE INDEX idx_users_role      ON `epilote_prod`._default.`users`(role)      WHERE type="user";
```

### Index structure scolaire
```sql
CREATE INDEX idx_schools_group   ON `epilote_prod`._default.`schools`(groupId)     WHERE type="school";
CREATE INDEX idx_acfg_school     ON `epilote_prod`._default.`academic_config`(schoolId) WHERE type="academic_config";
```

### Index élèves & inscriptions
```sql
CREATE INDEX idx_students_school ON `epilote_prod`._default.`students`(schoolId)          WHERE type="student";
CREATE INDEX idx_students_class  ON `epilote_prod`._default.`students`(schoolId,currentClassId) WHERE type="student";
CREATE INDEX idx_inscriptions_school_year ON `epilote_prod`._default.`inscriptions`(schoolId,academicYearId) WHERE type="inscription";
```

### Index pédagogie
```sql
CREATE INDEX idx_grades_class    ON `epilote_prod`._default.`grades`(schoolId,classId,period)    WHERE type="grade";
CREATE INDEX idx_grades_student  ON `epilote_prod`._default.`grades`(schoolId,studentId,period)  WHERE type="grade";
CREATE INDEX idx_grades_subject  ON `epilote_prod`._default.`grades`(schoolId,classId,subjectId) WHERE type="grade";
CREATE INDEX idx_grades_review   ON `epilote_prod`._default.`grades`(schoolId,requiresReview)    WHERE type="grade" AND requiresReview=true;
CREATE INDEX idx_rc_class        ON `epilote_prod`._default.`report_cards`(schoolId,classId,period) WHERE type="report_card";
CREATE INDEX idx_evaluations_class ON `epilote_prod`._default.`evaluations`(schoolId,classId,period) WHERE type="evaluation";
```

### Index présences
```sql
CREATE INDEX idx_att_class_date  ON `epilote_prod`._default.`attendances`(schoolId,classId,date)   WHERE type="attendance";
CREATE INDEX idx_att_student     ON `epilote_prod`._default.`attendances`(schoolId,studentId)      WHERE type="attendance";
CREATE INDEX idx_staff_att_date  ON `epilote_prod`._default.`staff_attendances`(schoolId,date)     WHERE type="staff_attendance";
```

### Index finance
```sql
CREATE INDEX idx_invoices_school  ON `epilote_prod`._default.`invoices`(schoolId,status)           WHERE type="invoice";
CREATE INDEX idx_invoices_student ON `epilote_prod`._default.`invoices`(schoolId,studentId)        WHERE type="invoice";
CREATE INDEX idx_expenses_school  ON `epilote_prod`._default.`expenses`(schoolId,status)           WHERE type="expense";
CREATE INDEX idx_budgets_school   ON `epilote_prod`._default.`budgets`(schoolId,academicYearId)    WHERE type="budget";
```

### Index RH
```sql
CREATE INDEX idx_staff_school    ON `epilote_prod`._default.`staff`(schoolId)        WHERE type="staff";
CREATE INDEX idx_payslips_staff  ON `epilote_prod`._default.`payslips`(schoolId,staffId,year,month) WHERE type="payslip";
```

### Index sync
```sql
CREATE INDEX idx_mut_school_status ON `epilote_prod`._default.`sync_mutations`(schoolId,status)         WHERE type="sync_mutation";
CREATE INDEX idx_mut_entity        ON `epilote_prod`._default.`sync_mutations`(schoolId,entityType,entityId) WHERE type="sync_mutation";
CREATE INDEX idx_cnf_school_status ON `epilote_prod`._default.`sync_conflicts`(schoolId,status)         WHERE type="sync_conflict";
```

### Index config
```sql
CREATE INDEX idx_config_type ON `epilote_prod`._default.`config`(type);
```

---

## ÉTAPE 4 — Insérer les documents de référence (Data Tools → Query)

### 4.1 Config modules
```sql
UPSERT INTO `epilote_prod`._default.`config` (KEY, VALUE) VALUES (
  "config::modules",
  {
    "_id": "config::modules",
    "type": "config_modules",
    "_schemaVersion": 1,
    "modules": [
      { "id": "mod::notes",    "slug": "notes",    "name": "Saisie des notes",      "isCore": true,  "requiredPlan": "gratuit" },
      { "id": "mod::absences", "slug": "absences", "name": "Suivi des absences",    "isCore": true,  "requiredPlan": "gratuit" },
      { "id": "mod::bulletins","slug": "bulletins","name": "Bulletins scolaires",   "isCore": true,  "requiredPlan": "gratuit" },
      { "id": "mod::finance",  "slug": "finance",  "name": "Gestion financière",    "isCore": false, "requiredPlan": "premium"  },
      { "id": "mod::rh",       "slug": "rh",       "name": "Ressources humaines",  "isCore": false, "requiredPlan": "premium"  },
      { "id": "mod::agenda",   "slug": "agenda",   "name": "Agenda & événements",  "isCore": false, "requiredPlan": "standard" }
    ],
    "updatedAt": "2026-04-07T00:00:00Z"
  }
);
```

### 4.2 Plans d'abonnement
```sql
UPSERT INTO `epilote_prod`._default.`config` (KEY, VALUE) VALUES (
  "config::plans",
  {
    "_id": "config::plans",
    "type": "config_plans",
    "_schemaVersion": 1,
    "plans": [
      { "id": "plan::gratuit",  "name": "Gratuit",  "type": "gratuit",  "price": 0,       "maxStudents": 100,  "maxPersonnel": 10  },
      { "id": "plan::standard", "name": "Standard", "type": "standard", "price": 150000,  "maxStudents": 500,  "maxPersonnel": 50  },
      { "id": "plan::premium",  "name": "Premium",  "type": "premium",  "price": 350000,  "maxStudents": 2000, "maxPersonnel": 200 }
    ],
    "currency": "XAF",
    "updatedAt": "2026-04-07T00:00:00Z"
  }
);
```

### 4.3 Plan de comptes (Chart of Accounts — OHADA simplifié)
```sql
UPSERT INTO `epilote_prod`._default.`config` (KEY, VALUE) VALUES (
  "config::chart_of_accounts",
  {
    "_id": "config::chart_of_accounts",
    "type": "chart_of_accounts",
    "_schemaVersion": 1,
    "accounts": [
      { "code": "411000", "name": "Clients - Élèves",            "class": 4, "type": "actif" },
      { "code": "706000", "name": "Prestations de services",     "class": 7, "type": "produit" },
      { "code": "401000", "name": "Fournisseurs",                "class": 4, "type": "passif" },
      { "code": "601000", "name": "Achats fournitures",          "class": 6, "type": "charge" },
      { "code": "641000", "name": "Charges de personnel",        "class": 6, "type": "charge" },
      { "code": "512000", "name": "Banque",                      "class": 5, "type": "actif" },
      { "code": "571000", "name": "Caisse",                      "class": 5, "type": "actif" }
    ],
    "standard": "OHADA",
    "currency": "XAF",
    "updatedAt": "2026-04-07T00:00:00Z"
  }
);
```

### 4.4 Premier school_group de test
```sql
UPSERT INTO `epilote_prod`._default.`school_groups` (KEY, VALUE) VALUES (
  "grp::g001",
  {
    "_id": "grp::g001",
    "type": "school_group",
    "_schemaVersion": 1,
    "name": "Groupe Scolaire Pilote Congo",
    "slug": "groupe-pilote-congo",
    "email": "admin@epilote.cg",
    "country": "Congo",
    "city": "Brazzaville",
    "maxSchools": 3,
    "maxUsersPerSchool": 50,
    "isActive": true,
    "subscription": {
      "planId": "plan::gratuit",
      "planName": "Gratuit",
      "status": "active",
      "startDate": "2026-01-01",
      "endDate": "2026-12-31"
    },
    "createdAt": "2026-04-07T00:00:00Z",
    "updatedAt": "2026-04-07T00:00:00Z"
  }
);
```

### 4.5 École de test
```sql
UPSERT INTO `epilote_prod`._default.`schools` (KEY, VALUE) VALUES (
  "sch::s001",
  {
    "_id": "sch::s001",
    "type": "school",
    "_schemaVersion": 1,
    "groupId": "grp::g001",
    "name": "Lycée Pilote de Brazzaville",
    "slug": "lycee-pilote-bzv",
    "schoolType": "lycee",
    "city": "Brazzaville",
    "province": "brazzaville",
    "schoolCode": "LYC-BZV-001",
    "isActive": true,
    "currentAcademicYearId": "ay::2025-2026",
    "createdAt": "2026-04-07T00:00:00Z",
    "updatedAt": "2026-04-07T00:00:00Z"
  }
);
```

### 4.6 Configuration académique
```sql
UPSERT INTO `epilote_prod`._default.`academic_config` (KEY, VALUE) VALUES (
  "acfg::s001::2025-2026",
  {
    "_id": "acfg::s001::2025-2026",
    "type": "academic_config",
    "_schemaVersion": 1,
    "schoolId": "sch::s001",
    "academicYear": {
      "id": "ay::2025-2026",
      "name": "2025-2026",
      "startDate": "2025-09-08",
      "endDate": "2026-07-03",
      "isCurrent": true
    },
    "cycles": [
      {
        "id": "cycle::secondaire",
        "name": "Secondaire", "code": "SEC",
        "levels": [
          { "id": "lvl::6e", "name": "Sixième",   "shortName": "6ème",  "orderIndex": 1 },
          { "id": "lvl::5e", "name": "Cinquième", "shortName": "5ème",  "orderIndex": 2 },
          { "id": "lvl::4e", "name": "Quatrième", "shortName": "4ème",  "orderIndex": 3 },
          { "id": "lvl::3e", "name": "Troisième", "shortName": "3ème",  "orderIndex": 4 },
          { "id": "lvl::2e", "name": "Seconde",   "shortName": "2nde",  "orderIndex": 5 },
          { "id": "lvl::1e", "name": "Première",  "shortName": "1ère",  "orderIndex": 6 },
          { "id": "lvl::te", "name": "Terminale", "shortName": "Tle",   "orderIndex": 7 }
        ],
        "filieres": [
          { "id": "fil::A", "name": "Littéraire",          "code": "A" },
          { "id": "fil::C", "name": "Sciences exactes",    "code": "C" },
          { "id": "fil::D", "name": "Sciences naturelles", "code": "D" }
        ]
      }
    ],
    "periods": ["T1", "T2", "T3"],
    "updatedAt": "2026-04-07T00:00:00Z"
  }
);
```

### 4.7 Utilisateur Super Admin de test
```sql
UPSERT INTO `epilote_prod`._default.`users` (KEY, VALUE) VALUES (
  "user::u_superadmin",
  {
    "_id": "user::u_superadmin",
    "type": "user",
    "_schemaVersion": 1,
    "username": "superadmin",
    "email": "superadmin@epilote.cg",
    "firstName": "Super",
    "lastName": "Admin",
    "role": "SUPER_ADMIN",
    "status": "active",
    "groupId": null,
    "schoolId": null,
    "permissions": { "profileCode": "SUPER_ADMIN", "modules": [] },
    "mustChangePassword": true,
    "createdAt": "2026-04-07T00:00:00Z",
    "updatedAt": "2026-04-07T00:00:00Z"
  }
);
```

---

## ÉTAPE 5 — Vérification

Exécuter ces requêtes de validation dans Data Tools → Query :

```sql
-- Compter les documents par collection
SELECT "school_groups" AS coll, COUNT(*) AS cnt FROM `epilote_prod`._default.`school_groups`
UNION ALL
SELECT "schools",       COUNT(*) FROM `epilote_prod`._default.`schools`
UNION ALL
SELECT "academic_config", COUNT(*) FROM `epilote_prod`._default.`academic_config`
UNION ALL
SELECT "users",         COUNT(*) FROM `epilote_prod`._default.`users`
UNION ALL
SELECT "config",        COUNT(*) FROM `epilote_prod`._default.`config`;
```

Résultat attendu :
```
school_groups  : 1
schools        : 1
academic_config: 1
users          : 1
config         : 3
```

---

## ÉTAPE 6 — Créer le user Sync Gateway (Admin API Sync Gateway)

Après déploiement Docker, exécuter via curl ou Postman :

```bash
# Remplacer <SG_ADMIN_URL> par http://votre-vps:4985
curl -X PUT <SG_ADMIN_URL>/epilote_prod/_user/u_superadmin \
  -H "Content-Type: application/json" \
  -d '{
    "name": "u_superadmin",
    "password": "ChangeMe123!",
    "admin_channels": ["global"],
    "disabled": false
  }'
```

---

## RÉCAPITULATIF DES ACTIONS

| Étape | Outil | Statut |
|---|---|---|
| 1. Créer bucket `epilote_prod` | Capella UI | ⬜ À faire |
| 2. Créer 29 collections | Capella UI | ⬜ À faire |
| 3. Créer 30+ index | Capella Query | ⬜ À faire |
| 4. Insérer documents de référence | Capella Query | ⬜ À faire |
| 5. Vérifier les counts | Capella Query | ⬜ À faire |
| 6. Créer user Sync Gateway | curl/Postman | ⬜ Après déploiement Docker |
