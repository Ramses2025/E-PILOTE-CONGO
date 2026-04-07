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

-- ── BLOC 2 : Créer les index ─────────────────────────────────

CREATE INDEX idx_users_username   ON `epilote_prod`._default.`users`(username)  WHERE type="user";
CREATE INDEX idx_users_school     ON `epilote_prod`._default.`users`(schoolId)  WHERE type="user";
CREATE INDEX idx_users_group      ON `epilote_prod`._default.`users`(groupId)   WHERE type="user";

CREATE INDEX idx_schools_group    ON `epilote_prod`._default.`schools`(groupId) WHERE type="school";

CREATE INDEX idx_acfg_school      ON `epilote_prod`._default.`academic_config`(schoolId) WHERE type="academic_config";

CREATE INDEX idx_students_school  ON `epilote_prod`._default.`students`(schoolId) WHERE type="student";
CREATE INDEX idx_students_class   ON `epilote_prod`._default.`students`(schoolId, currentClassId) WHERE type="student";

CREATE INDEX idx_inscriptions_sy  ON `epilote_prod`._default.`inscriptions`(schoolId, academicYearId) WHERE type="inscription";

CREATE INDEX idx_grades_class     ON `epilote_prod`._default.`grades`(schoolId, classId, period) WHERE type="grade";
CREATE INDEX idx_grades_student   ON `epilote_prod`._default.`grades`(schoolId, studentId, period) WHERE type="grade";
CREATE INDEX idx_grades_review    ON `epilote_prod`._default.`grades`(schoolId, requiresReview) WHERE type="grade" AND requiresReview=true;

CREATE INDEX idx_rc_class         ON `epilote_prod`._default.`report_cards`(schoolId, classId, period) WHERE type="report_card";

CREATE INDEX idx_att_class_date   ON `epilote_prod`._default.`attendances`(schoolId, classId, date) WHERE type="attendance";
CREATE INDEX idx_att_student      ON `epilote_prod`._default.`attendances`(schoolId, studentId) WHERE type="attendance";

CREATE INDEX idx_invoices_school  ON `epilote_prod`._default.`invoices`(schoolId, status) WHERE type="invoice";
CREATE INDEX idx_invoices_student ON `epilote_prod`._default.`invoices`(schoolId, studentId) WHERE type="invoice";

CREATE INDEX idx_expenses_school  ON `epilote_prod`._default.`expenses`(schoolId, status) WHERE type="expense";
CREATE INDEX idx_budgets_school   ON `epilote_prod`._default.`budgets`(schoolId, academicYearId) WHERE type="budget";

CREATE INDEX idx_staff_school     ON `epilote_prod`._default.`staff`(schoolId) WHERE type="staff";
CREATE INDEX idx_payslips_staff   ON `epilote_prod`._default.`payslips`(schoolId, staffId, year, month) WHERE type="payslip";

CREATE INDEX idx_mut_status       ON `epilote_prod`._default.`sync_mutations`(schoolId, status) WHERE type="sync_mutation";
CREATE INDEX idx_cnf_status       ON `epilote_prod`._default.`sync_conflicts`(schoolId, status) WHERE type="sync_conflict";

CREATE INDEX idx_config_type      ON `epilote_prod`._default.`config`(type);

-- ── BLOC 3 : Insérer les documents de référence ──────────────

UPSERT INTO `epilote_prod`._default.`config` (KEY, VALUE) VALUES (
  "config::modules",
  {
    "_id": "config::modules", "type": "config_modules", "_schemaVersion": 1,
    "modules": [
      { "id": "mod::notes",    "slug": "notes",    "name": "Saisie des notes",    "isCore": true,  "requiredPlan": "gratuit" },
      { "id": "mod::absences", "slug": "absences", "name": "Suivi des absences",  "isCore": true,  "requiredPlan": "gratuit" },
      { "id": "mod::bulletins","slug": "bulletins","name": "Bulletins scolaires", "isCore": true,  "requiredPlan": "gratuit" },
      { "id": "mod::finance",  "slug": "finance",  "name": "Gestion financiere",  "isCore": false, "requiredPlan": "premium"  },
      { "id": "mod::rh",       "slug": "rh",       "name": "Ressources humaines", "isCore": false, "requiredPlan": "premium"  }
    ],
    "updatedAt": "2026-04-07T00:00:00Z"
  }
);

UPSERT INTO `epilote_prod`._default.`config` (KEY, VALUE) VALUES (
  "config::plans",
  {
    "_id": "config::plans", "type": "config_plans", "_schemaVersion": 1, "currency": "XAF",
    "plans": [
      { "id": "plan::gratuit",  "name": "Gratuit",  "type": "gratuit",  "price": 0,       "maxStudents": 100,  "maxPersonnel": 10  },
      { "id": "plan::standard", "name": "Standard", "type": "standard", "price": 150000,  "maxStudents": 500,  "maxPersonnel": 50  },
      { "id": "plan::premium",  "name": "Premium",  "type": "premium",  "price": 350000,  "maxStudents": 2000, "maxPersonnel": 200 }
    ],
    "updatedAt": "2026-04-07T00:00:00Z"
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
