-- ============================================================
-- E-PILOTE CONGO — Setup complet en un seul script
-- Coller dans : Capella → Data Tools → Query
-- Exécuter bloc par bloc (sélectionner + Run)
-- ============================================================

-- ── BLOC 1 : Créer les collections (27 collections) ─────────
-- Basé sur l'analyse fonctionnelle des 6 catégories métier
-- Sélectionner toutes les lignes → Run

-- Référentiel & structure
CREATE COLLECTION `epilote_prod`.`_default`.`config`            IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`school_groups`     IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`schools`           IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`academic_config`   IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`users`             IF NOT EXISTS;

-- Backend Admin (groupes, modules, plans, profils)
CREATE COLLECTION `epilote_prod`.`_default`.`groupes`           IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`modules`           IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`plans`             IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`profils`           IF NOT EXISTS;

-- Scolarité
CREATE COLLECTION `epilote_prod`.`_default`.`students`          IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`inscriptions`      IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`transfers`         IF NOT EXISTS;

-- Pédagogie
CREATE COLLECTION `epilote_prod`.`_default`.`grades`            IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`report_cards`      IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`timetable`         IF NOT EXISTS;

-- Vie scolaire
CREATE COLLECTION `epilote_prod`.`_default`.`attendances`       IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`disciplines`       IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`services`          IF NOT EXISTS;

-- Finances
CREATE COLLECTION `epilote_prod`.`_default`.`invoices`          IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`expenses`          IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`budgets`           IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`accounting_pieces` IF NOT EXISTS;

-- Personnel
CREATE COLLECTION `epilote_prod`.`_default`.`staff`             IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`staff_attendances` IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`payslips`          IF NOT EXISTS;

-- Communication
CREATE COLLECTION `epilote_prod`.`_default`.`announcements`     IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`messages`          IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`notifications`     IF NOT EXISTS;

-- Sync & Audit (audit_logs + ledger_entries = cloud only, pas sur CBLite)
CREATE COLLECTION `epilote_prod`.`_default`.`sync_mutations`    IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`sync_conflicts`    IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`audit_logs`        IF NOT EXISTS;
CREATE COLLECTION `epilote_prod`.`_default`.`ledger_entries`    IF NOT EXISTS;

-- ── BLOC 2 : Créer les index (IF NOT EXISTS = idempotent) ────

CREATE INDEX idx_users_username   IF NOT EXISTS ON `epilote_prod`._default.`users`(username)  WHERE type="user";
CREATE INDEX idx_users_email      IF NOT EXISTS ON `epilote_prod`._default.`users`(email)     WHERE type="user";
CREATE INDEX idx_users_role       IF NOT EXISTS ON `epilote_prod`._default.`users`(role)      WHERE type="user";
CREATE INDEX idx_users_school     IF NOT EXISTS ON `epilote_prod`._default.`users`(schoolId)  WHERE type="user";
CREATE INDEX idx_users_group      IF NOT EXISTS ON `epilote_prod`._default.`users`(groupId)   WHERE type="user";

CREATE INDEX idx_schools_group    IF NOT EXISTS ON `epilote_prod`._default.`schools`(groupId) WHERE type="school";

CREATE INDEX idx_acfg_school      IF NOT EXISTS ON `epilote_prod`._default.`academic_config`(schoolId) WHERE type="academic_config";

CREATE INDEX idx_students_school  IF NOT EXISTS ON `epilote_prod`._default.`students`(schoolId) WHERE type="student";
CREATE INDEX idx_students_class   IF NOT EXISTS ON `epilote_prod`._default.`students`(schoolId, currentClassId) WHERE type="student";

CREATE INDEX idx_inscriptions_sy  IF NOT EXISTS ON `epilote_prod`._default.`inscriptions`(schoolId, academicYearId) WHERE type="inscription";

CREATE INDEX idx_grades_class     IF NOT EXISTS ON `epilote_prod`._default.`grades`(schoolId, classeId, periode) WHERE type="grade";
CREATE INDEX idx_grades_student   IF NOT EXISTS ON `epilote_prod`._default.`grades`(schoolId, eleveId, periode) WHERE type="grade";
CREATE INDEX idx_grades_review    IF NOT EXISTS ON `epilote_prod`._default.`grades`(schoolId, requiresReview) WHERE type="grade" AND requiresReview=true;

CREATE INDEX idx_rc_class         IF NOT EXISTS ON `epilote_prod`._default.`report_cards`(schoolId, classeId, periode) WHERE type="report_card";

CREATE INDEX idx_att_class_date   IF NOT EXISTS ON `epilote_prod`._default.`attendances`(schoolId, classeId, date) WHERE type="attendance";
CREATE INDEX idx_att_student      IF NOT EXISTS ON `epilote_prod`._default.`attendances`(schoolId, eleveId) WHERE type="attendance";

CREATE INDEX idx_invoices_school  IF NOT EXISTS ON `epilote_prod`._default.`invoices`(schoolId, status) WHERE type="invoice";
CREATE INDEX idx_invoices_student IF NOT EXISTS ON `epilote_prod`._default.`invoices`(schoolId, studentId) WHERE type="invoice";

CREATE INDEX idx_expenses_school  IF NOT EXISTS ON `epilote_prod`._default.`expenses`(schoolId, status) WHERE type="expense";
CREATE INDEX idx_budgets_school   IF NOT EXISTS ON `epilote_prod`._default.`budgets`(schoolId, academicYearId) WHERE type="budget";

CREATE INDEX idx_staff_school     IF NOT EXISTS ON `epilote_prod`._default.`staff`(schoolId) WHERE type="staff";
CREATE INDEX idx_payslips_staff   IF NOT EXISTS ON `epilote_prod`._default.`payslips`(schoolId, staffId, year, month) WHERE type="payslip";
CREATE INDEX idx_staff_att_school IF NOT EXISTS ON `epilote_prod`._default.`staff_attendances`(schoolId, staffId, date) WHERE type="staff_attendance";

CREATE INDEX idx_mut_status       IF NOT EXISTS ON `epilote_prod`._default.`sync_mutations`(schoolId, status) WHERE type="sync_mutation";
CREATE INDEX idx_cnf_status       IF NOT EXISTS ON `epilote_prod`._default.`sync_conflicts`(schoolId, status) WHERE type="sync_conflict";

CREATE INDEX idx_config_type      IF NOT EXISTS ON `epilote_prod`._default.`config`(type);

-- Backend Admin indexes
CREATE INDEX idx_groupes_type     IF NOT EXISTS ON `epilote_prod`._default.`groupes`(type);
CREATE INDEX idx_modules_type     IF NOT EXISTS ON `epilote_prod`._default.`modules`(type);
CREATE INDEX idx_plans_type       IF NOT EXISTS ON `epilote_prod`._default.`plans`(type);
CREATE INDEX idx_profils_groupe   IF NOT EXISTS ON `epilote_prod`._default.`profils`(groupeId) WHERE type="profil";

-- ── BLOC 3 : Insérer les documents de référence ──────────────

UPSERT INTO `epilote_prod`._default.`config` (KEY, VALUE) VALUES (
  "config::categories",
  {
    "_id": "config::categories", "type": "config_categories", "_schemaVersion": 1,
    "categories": [
      { "code": "scolarite",     "nom": "Scolarité",     "isCore": true,  "ordre": 0 },
      { "code": "pedagogie",     "nom": "Pédagogie",     "isCore": true,  "ordre": 1 },
      { "code": "finances",      "nom": "Finances",      "isCore": false, "ordre": 2 },
      { "code": "personnel",     "nom": "Personnel",     "isCore": false, "ordre": 3 },
      { "code": "vie-scolaire",  "nom": "Vie Scolaire",  "isCore": false, "ordre": 4 },
      { "code": "communication", "nom": "Communication", "isCore": false, "ordre": 5 }
    ],
    "updatedAt": "2026-04-08T00:00:00Z"
  }
);

UPSERT INTO `epilote_prod`._default.`config` (KEY, VALUE) VALUES (
  "config::modules",
  {
    "_id": "config::modules", "type": "config_modules", "_schemaVersion": 2,
    "modules": [
      { "slug": "inscriptions",       "nom": "Inscriptions",        "categorieCode": "scolarite",     "isCore": true,  "requiredPlan": "gratuit", "ordre": 0  },
      { "slug": "eleves",             "nom": "Élèves",              "categorieCode": "scolarite",     "isCore": true,  "requiredPlan": "gratuit", "ordre": 1  },
      { "slug": "classes",            "nom": "Classes",             "categorieCode": "scolarite",     "isCore": true,  "requiredPlan": "gratuit", "ordre": 2  },
      { "slug": "transferts",         "nom": "Transferts",          "categorieCode": "scolarite",     "isCore": false, "requiredPlan": "premium", "ordre": 3  },
      { "slug": "documents",          "nom": "Documents scolaires", "categorieCode": "scolarite",     "isCore": false, "requiredPlan": "premium", "ordre": 4  },
      { "slug": "notes",              "nom": "Notes & Évaluations", "categorieCode": "pedagogie",     "isCore": true,  "requiredPlan": "gratuit", "ordre": 7  },
      { "slug": "matieres",           "nom": "Matières",            "categorieCode": "pedagogie",     "isCore": true,  "requiredPlan": "gratuit", "ordre": 5  },
      { "slug": "bulletins",          "nom": "Bulletins",           "categorieCode": "pedagogie",     "isCore": false, "requiredPlan": "premium", "ordre": 8  },
      { "slug": "emploi-du-temps",    "nom": "Emploi du temps",     "categorieCode": "pedagogie",     "isCore": false, "requiredPlan": "premium", "ordre": 6  },
      { "slug": "cahier-textes",      "nom": "Cahier de textes",    "categorieCode": "pedagogie",     "isCore": false, "requiredPlan": "premium", "ordre": 9  },
      { "slug": "evaluations",        "nom": "Évaluations",         "categorieCode": "pedagogie",     "isCore": false, "requiredPlan": "premium", "ordre": 11 },
      { "slug": "conseils",           "nom": "Conseils de classe",  "categorieCode": "pedagogie",     "isCore": false, "requiredPlan": "pro",     "ordre": 10 },
      { "slug": "finances",           "nom": "Finances",            "categorieCode": "finances",      "isCore": false, "requiredPlan": "premium", "ordre": 12 },
      { "slug": "facturation",        "nom": "Facturation",         "categorieCode": "finances",      "isCore": false, "requiredPlan": "premium", "ordre": 13 },
      { "slug": "depenses",           "nom": "Dépenses",            "categorieCode": "finances",      "isCore": false, "requiredPlan": "premium", "ordre": 14 },
      { "slug": "budget",             "nom": "Budget",              "categorieCode": "finances",      "isCore": false, "requiredPlan": "pro",     "ordre": 15 },
      { "slug": "comptabilite",       "nom": "Comptabilité",        "categorieCode": "finances",      "isCore": false, "requiredPlan": "pro",     "ordre": 16 },
      { "slug": "personnel",          "nom": "Personnel",           "categorieCode": "personnel",     "isCore": false, "requiredPlan": "premium", "ordre": 17 },
      { "slug": "presences-personnel","nom": "Présences personnel", "categorieCode": "personnel",     "isCore": false, "requiredPlan": "premium", "ordre": 18 },
      { "slug": "conges",             "nom": "Congés",              "categorieCode": "personnel",     "isCore": false, "requiredPlan": "premium", "ordre": 19 },
      { "slug": "paie",               "nom": "Paie",                "categorieCode": "personnel",     "isCore": false, "requiredPlan": "pro",     "ordre": 20 },
      { "slug": "presences-eleves",   "nom": "Présences élèves",    "categorieCode": "vie-scolaire",  "isCore": false, "requiredPlan": "gratuit", "ordre": 21 },
      { "slug": "discipline",         "nom": "Discipline",          "categorieCode": "vie-scolaire",  "isCore": false, "requiredPlan": "premium", "ordre": 22 },
      { "slug": "bibliotheque",       "nom": "Bibliothèque",        "categorieCode": "vie-scolaire",  "isCore": false, "requiredPlan": "premium", "ordre": 23 },
      { "slug": "cantine",            "nom": "Cantine",             "categorieCode": "vie-scolaire",  "isCore": false, "requiredPlan": "premium", "ordre": 24 },
      { "slug": "infirmerie",         "nom": "Infirmerie",          "categorieCode": "vie-scolaire",  "isCore": false, "requiredPlan": "premium", "ordre": 25 },
      { "slug": "annonces",           "nom": "Annonces",            "categorieCode": "communication", "isCore": false, "requiredPlan": "gratuit", "ordre": 26 },
      { "slug": "messagerie",         "nom": "Messagerie",          "categorieCode": "communication", "isCore": false, "requiredPlan": "premium", "ordre": 27 },
      { "slug": "notifications",      "nom": "Notifications",       "categorieCode": "communication", "isCore": false, "requiredPlan": "gratuit", "ordre": 28 },
      { "slug": "evenements",         "nom": "Événements",          "categorieCode": "communication", "isCore": false, "requiredPlan": "premium", "ordre": 29 }
    ],
    "updatedAt": "2026-04-08T00:00:00Z"
  }
);

UPSERT INTO `epilote_prod`._default.`config` (KEY, VALUE) VALUES (
  "config::plans",
  {
    "_id": "config::plans", "type": "config_plans", "_schemaVersion": 2, "currency": "XAF",
    "plans": [
      {
        "id": "plan::gratuit", "name": "Gratuit", "type": "gratuit", "price": 0,
        "maxStudents": 100, "maxPersonnel": 10,
        "modulesIncluded": [
          "inscriptions","eleves","classes","notes","matieres",
          "presences-eleves","annonces","notifications"
        ]
      },
      {
        "id": "plan::premium", "name": "Premium", "type": "premium", "price": 150000,
        "maxStudents": 2000, "maxPersonnel": 200,
        "modulesIncluded": [
          "inscriptions","eleves","classes","notes","matieres","presences-eleves","annonces","notifications",
          "bulletins","emploi-du-temps","cahier-textes","evaluations","transferts","documents",
          "finances","facturation","depenses","personnel","presences-personnel","conges",
          "discipline","bibliotheque","cantine","infirmerie","messagerie","evenements"
        ]
      },
      {
        "id": "plan::pro", "name": "Pro", "type": "pro", "price": 350000,
        "maxStudents": 10000, "maxPersonnel": 1000,
        "modulesIncluded": [
          "inscriptions","eleves","classes","notes","matieres","presences-eleves","annonces","notifications",
          "bulletins","emploi-du-temps","cahier-textes","evaluations","transferts","documents",
          "finances","facturation","depenses","personnel","presences-personnel","conges",
          "discipline","bibliotheque","cantine","infirmerie","messagerie","evenements",
          "conseils","budget","comptabilite","paie"
        ]
      }
    ],
    "updatedAt": "2026-04-08T00:00:00Z"
  }
);

UPSERT INTO `epilote_prod`._default.`school_groups` (KEY, VALUE) VALUES (
  "grp::g001",
  {
    "_id": "grp::g001", "type": "school_group", "_schemaVersion": 1,
    "name": "Groupe Scolaire Pilote Congo", "slug": "groupe-pilote-congo",
    "email": "admin@epilote.cg", "country": "Congo", "city": "Brazzaville",
    "maxSchools": 3, "maxUsersPerSchool": 50, "isActive": true,
    "subscription": {
      "planId": "plan::gratuit", "planName": "Gratuit",
      "status": "active", "startDate": "2026-01-01", "endDate": "2026-12-31"
    },
    "createdAt": "2026-04-07T00:00:00Z", "updatedAt": "2026-04-07T00:00:00Z"
  }
);

UPSERT INTO `epilote_prod`._default.`schools` (KEY, VALUE) VALUES (
  "sch::s001",
  {
    "_id": "sch::s001", "type": "school", "_schemaVersion": 1,
    "groupId": "grp::g001", "name": "Lycee Pilote de Brazzaville",
    "slug": "lycee-pilote-bzv", "schoolType": "lycee",
    "city": "Brazzaville", "province": "brazzaville",
    "schoolCode": "LYC-BZV-001", "isActive": true,
    "currentAcademicYearId": "ay::2025-2026",
    "createdAt": "2026-04-07T00:00:00Z", "updatedAt": "2026-04-07T00:00:00Z"
  }
);

UPSERT INTO `epilote_prod`._default.`academic_config` (KEY, VALUE) VALUES (
  "acfg::s001::2025-2026",
  {
    "_id": "acfg::s001::2025-2026", "type": "academic_config", "_schemaVersion": 1,
    "schoolId": "sch::s001",
    "academicYear": { "id": "ay::2025-2026", "name": "2025-2026", "startDate": "2025-09-08", "endDate": "2026-07-03", "isCurrent": true },
    "cycles": [{
      "id": "cycle::secondaire", "name": "Secondaire", "code": "SEC",
      "levels": [
        { "id": "lvl::6e", "name": "Sixieme",   "shortName": "6eme", "orderIndex": 1 },
        { "id": "lvl::5e", "name": "Cinquieme", "shortName": "5eme", "orderIndex": 2 },
        { "id": "lvl::4e", "name": "Quatrieme", "shortName": "4eme", "orderIndex": 3 },
        { "id": "lvl::3e", "name": "Troisieme", "shortName": "3eme", "orderIndex": 4 },
        { "id": "lvl::2e", "name": "Seconde",   "shortName": "2nde", "orderIndex": 5 },
        { "id": "lvl::1e", "name": "Premiere",  "shortName": "1ere", "orderIndex": 6 },
        { "id": "lvl::te", "name": "Terminale", "shortName": "Tle",  "orderIndex": 7 }
      ],
      "filieres": [
        { "id": "fil::A", "name": "Litteraire",          "code": "A" },
        { "id": "fil::C", "name": "Sciences exactes",    "code": "C" },
        { "id": "fil::D", "name": "Sciences naturelles", "code": "D" }
      ]
    }],
    "periods": ["T1", "T2", "T3"],
    "updatedAt": "2026-04-07T00:00:00Z"
  }
);

-- ── BLOC 4 : Vérification ────────────────────────────────────

SELECT "school_groups"    AS coll, COUNT(*) AS cnt FROM `epilote_prod`._default.`school_groups`
UNION ALL
SELECT "schools",         COUNT(*) FROM `epilote_prod`._default.`schools`
UNION ALL
SELECT "academic_config", COUNT(*) FROM `epilote_prod`._default.`academic_config`
UNION ALL
SELECT "config",          COUNT(*) FROM `epilote_prod`._default.`config`;
