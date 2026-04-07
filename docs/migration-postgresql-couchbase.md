# Migration PostgreSQL → Couchbase : E-PILOTE CONGO

Stratégie complète et optimisée de migration du schéma PostgreSQL/Prisma vers Couchbase (documents JSON), analysée à partir des 70 tables du système de gestion scolaire E-PILOTE CONGO.

---

## 1. ARCHITECTURE DE HAUT NIVEAU

### 1.1 Domaines identifiés

| Domaine | Tables PG | Collections Couchbase cibles |
|---|---|---|
| **Identité & Auth** | users, refresh_tokens, access_profiles, access_profile_modules, user_modules | `users` |
| **Structure scolaire** | school_groups, schools, academic_years, school_cycles, grade_levels, filieres, classes, subjects | `school_groups`, `schools`, `academic_config` |
| **Inscriptions & Élèves** | students, inscriptions, inscription_state_history, class_change_history, student_school_history, siblings | `students`, `inscriptions` |
| **Pédagogie** | evaluations, grades, report_cards, class_councils, council_decisions, class_promotions, textbook_entries, time_slots | `evaluations`, `grades`, `report_cards`, `timetable` |
| **Présences** | attendances, staff_attendances, attendance_aggregations, attendance_alerts, attendance_policies | `attendances`, `staff_attendances` |
| **Finance élèves** | fees, invoices, invoice_items, invoice_payments, invoice_notifications, payments | `invoices` |
| **Finance dépenses** | expenses, expense_audits, recurring_expenses, suppliers, budgets, budget_lines, budget_revisions | `expenses`, `budgets` |
| **Comptabilité** | accounting_entries, accounting_pieces, accounting_piece_lines, chart_of_accounts, fiscal_years, fiscal_period_closes, financial_reports | `accounting_pieces`, `accounting_config` |
| **RH & Paie** | staff, payslips, payslip_audit_logs, leave_requests, staff_work_schedules | `staff`, `payslips` |
| **Abonnements** | subscription_plans, subscriptions, subscription_invoices, plan_modules, plan_pricing, plan_categories, plan_module_exclusions | `subscription_plans`, `subscriptions` |
| **Communication** | messages, announcements, notifications, school_events | `messages`, `notifications` |
| **Sync** | sync_mutations, sync_conflicts, sync_logs, sync_health_snapshots, school_sync_states | `sync_mutations`, `sync_conflicts` |
| **Audit / Ledger** | academic_ledger, financial_ledger, discipline_ledger, audit_logs, configuration_audits | `audit_logs`, `ledger_entries` |
| **Discipline & Santé** | disciplines, infirmary_visits, medical_records | `disciplines`, `medical_records` |
| **Bibliothèque & Cantine** | books, loans, menus, cafeteria_registrations, meal_attendances | `books`, `cafeteria` |
| **Documents** | documents, document_versions, document_templates, document_generation_requests | `documents` |
| **Config & Référentiel** | modules, business_categories, platform_settings, configuration_templates, rooms, holidays, role_profiles, group_role_overrides | `config` |
| **Transferts** | transfers, transfer_audit_logs | `transfers` |

### 1.2 Principe d'organisation Couchbase

```
Bucket : epilote_prod
Scope  : _default
Collections (20 ciblées pour MVP) :
  users, schools, school_groups, academic_config,
  students, inscriptions,
  evaluations, grades, report_cards, timetable,
  attendances, staff_attendances,
  invoices, expenses, budgets, accounting_pieces,
  staff, payslips,
  sync_mutations, sync_conflicts,
  audit_logs, ledger_entries,
  messages, notifications,
  documents, config
```

---

## 2. MODÈLE DE DOCUMENT — CONCEPTION

### Règle d'or : accès sans jointure

| Pattern PG | Pattern Couchbase |
|---|---|
| FK + JOIN | Dénormalisation ou référence par ID avec résolution app-side |
| Table pivot (N:M) | Array d'IDs ou array d'objets embeddés |
| Table d'audit séparée | Array `_auditTrail` dans le document ou collection dédiée |
| Colonnes nullable → NULL | Champs omis du document (JSON sparse) |
| `version INTEGER` | `_schemaVersion` + `updatedAt` pour CBLite |
| `is_deleted` + `deleted_at` | Champ `_deleted: true` (compatible Couchbase tombstone) |

---

## 3. EXEMPLES JSON PAR ENTITÉ MAJEURE

### 3.1 USER

```json
{
  "_id": "user::u_abc123",
  "type": "user",
  "_schemaVersion": 1,
  "email": "directeur@ecole-sacrecoeur.cg",
  "firstName": "Jean-Pierre",
  "lastName": "MOUKALA",
  "phone": "+242 06 123 45 67",
  "role": "DIRECTEUR",
  "status": "active",
  "groupId": "grp::g001",
  "schoolId": "sch::s001",
  "accessProfileCode": "DIRECTEUR",
  "permissions": {
    "profileCode": "DIRECTEUR",
    "modules": [
      { "moduleSlug": "notes",    "canRead": true,  "canWrite": true,  "canDelete": false, "canExport": true },
      { "moduleSlug": "finance",  "canRead": true,  "canWrite": false, "canDelete": false, "canExport": true },
      { "moduleSlug": "absences", "canRead": true,  "canWrite": true,  "canDelete": false, "canExport": true }
    ]
  },
  "lastLoginAt": "2025-10-01T07:30:00Z",
  "mustChangePassword": false,
  "createdAt": "2024-09-01T00:00:00Z",
  "updatedAt": "2025-10-01T07:30:00Z"
}
```
> **Décision** : `user_modules` et `access_profile_modules` fusionnés dans `permissions.modules` — résolution à la création/modification du profil, pas à chaque requête.

---

### 3.2 SCHOOL_GROUP

```json
{
  "_id": "grp::g001",
  "type": "school_group",
  "_schemaVersion": 1,
  "name": "Groupe Scolaire Sacré-Cœur",
  "slug": "groupe-sacre-coeur",
  "email": "contact@sacrecoeur.cg",
  "phone": "+242 06 000 00 00",
  "address": "Avenue de l'Indépendance, Brazzaville",
  "city": "Brazzaville",
  "country": "Congo",
  "logo": "https://cdn.epilote.cg/logos/g001.png",
  "maxSchools": 5,
  "maxUsersPerSchool": 100,
  "isActive": true,
  "subscription": {
    "planId": "plan::premium",
    "planName": "Premium",
    "status": "active",
    "startDate": "2025-01-01",
    "endDate": "2025-12-31",
    "billingPeriod": "yearly"
  },
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2025-01-01T00:00:00Z"
}
```
> **Décision** : L'abonnement actif est dénormalisé dans le groupe pour éviter une jointure à chaque vérification d'accès.

---

### 3.3 SCHOOL

```json
{
  "_id": "sch::s001",
  "type": "school",
  "_schemaVersion": 1,
  "groupId": "grp::g001",
  "name": "Lycée Sacré-Cœur de Brazzaville",
  "slug": "lycee-sacre-coeur-bzv",
  "schoolType": "lycee",
  "email": "lycee@sacrecoeur.cg",
  "phone": "+242 06 100 00 01",
  "address": "Rue de la Paix, Bacongo",
  "city": "Brazzaville",
  "province": "brazzaville",
  "logo": "https://cdn.epilote.cg/logos/s001.png",
  "directorId": "user::u_abc123",
  "principalName": "M. MOUKALA Jean-Pierre",
  "schoolCode": "LYC-BZV-001",
  "isActive": true,
  "cycles": ["cycle::secondaire"],
  "currentAcademicYearId": "ay::2024-2025",
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2025-01-01T00:00:00Z"
}
```

---

### 3.4 ACADEMIC_CONFIG (fusion de academic_years + cycles + grade_levels + filieres)

```json
{
  "_id": "acfg::s001::2024-2025",
  "type": "academic_config",
  "_schemaVersion": 1,
  "schoolId": "sch::s001",
  "academicYear": {
    "id": "ay::2024-2025",
    "name": "2024-2025",
    "startDate": "2024-09-02",
    "endDate": "2025-07-04",
    "isCurrent": true,
    "isActive": true
  },
  "cycles": [
    {
      "id": "cycle::secondaire",
      "name": "Secondaire",
      "code": "SEC",
      "levels": [
        { "id": "lvl::6e", "name": "Sixième",  "shortName": "6ème", "orderIndex": 1 },
        { "id": "lvl::5e", "name": "Cinquième","shortName": "5ème", "orderIndex": 2 },
        { "id": "lvl::4e", "name": "Quatrième","shortName": "4ème", "orderIndex": 3 },
        { "id": "lvl::3e", "name": "Troisième","shortName": "3ème", "orderIndex": 4 },
        { "id": "lvl::2e", "name": "Seconde",  "shortName": "2nde","orderIndex": 5 },
        { "id": "lvl::1e", "name": "Première", "shortName": "1ère","orderIndex": 6 },
        { "id": "lvl::te", "name": "Terminale","shortName": "Tle", "orderIndex": 7 }
      ],
      "filieres": [
        { "id": "fil::A",  "name": "Littéraire",     "code": "A"  },
        { "id": "fil::C",  "name": "Sciences exactes","code": "C"  },
        { "id": "fil::D",  "name": "Sciences naturelles","code": "D" }
      ]
    }
  ],
  "periods": ["T1", "T2", "T3"],
  "updatedAt": "2024-09-01T00:00:00Z"
}
```
> **Décision** : `academic_years`, `school_cycles`, `grade_levels`, `filieres` sont regroupés dans un seul document de configuration par école × année. Ces données changent rarement et sont lues ensemble systématiquement.

---

### 3.5 STUDENT

```json
{
  "_id": "stu::stu_xyz789",
  "type": "student",
  "_schemaVersion": 1,
  "schoolId": "sch::s001",
  "matricule": "LYC-2024-0042",
  "firstName": "Grâce",
  "lastName": "MBEMBA",
  "dateOfBirth": "2008-03-15",
  "gender": "F",
  "address": "Quartier Moungali, Brazzaville",
  "photo": "https://cdn.epilote.cg/photos/stu_xyz789.jpg",
  "currentClassId": "cls::cls_3a",
  "currentAcademicYearId": "ay::2024-2025",
  "isActive": true,
  "parents": [
    {
      "relation": "mere",
      "name": "Marie MBEMBA",
      "phone": "+242 06 987 65 43",
      "email": "marie.mbemba@gmail.com",
      "profession": "Enseignante"
    }
  ],
  "emergency": {
    "contactName": "Pierre MBEMBA",
    "contactPhone": "+242 06 111 22 33",
    "relation": "oncle"
  },
  "medicalSummary": {
    "bloodType": "A+",
    "allergies": ["arachides"],
    "chronicConditions": []
  },
  "schoolHistory": [
    {
      "schoolId": "sch::s001",
      "classId": "cls::cls_4a",
      "academicYearId": "ay::2023-2024",
      "entryDate": "2023-09-04",
      "exitDate": "2024-07-05",
      "finalAverage": 14.2,
      "finalRank": 3,
      "totalStudents": 42,
      "councilDecision": "admis"
    }
  ],
  "createdAt": "2024-09-02T00:00:00Z",
  "updatedAt": "2025-01-15T10:00:00Z"
}
```
> **Décision** : `medical_records` (statique, 1:1) et `schoolHistory` (historique modéré) intégrés dans le document étudiant. `infirmary_visits` reste dans sa propre collection (haute volumétrie).

---

### 3.6 INSCRIPTION

```json
{
  "_id": "ins::ins_2024_xyz789",
  "type": "inscription",
  "_schemaVersion": 1,
  "schoolId": "sch::s001",
  "academicYearId": "ay::2024-2025",
  "studentId": "stu::stu_xyz789",
  "classId": "cls::cls_3a",
  "status": "validated",
  "inscriptionType": "reinscription",
  "levelId": "lvl::3e",
  "filiereId": "fil::D",
  "isRedoublant": false,
  "isBoarder": false,
  "hasScholarship": false,
  "registrationFee": 25000,
  "tuitionFee": 350000,
  "isPaid": true,
  "paidAt": "2024-09-02T09:00:00Z",
  "paymentMethod": "cash",
  "paymentRef": "REC-2024-0042",
  "stateHistory": [
    { "fromState": "pending", "toState": "validated", "action": "validate",
      "changedById": "user::u_abc123", "changedAt": "2024-09-02T09:15:00Z" }
  ],
  "validatedAt": "2024-09-02T09:15:00Z",
  "validatedById": "user::u_abc123",
  "createdAt": "2024-09-01T14:00:00Z",
  "updatedAt": "2024-09-02T09:15:00Z"
}
```
> **Décision** : `inscription_state_history` est intégré comme tableau dans l'inscription. Historique borné (< 20 transitions max par inscription).

---

### 3.7 GRADE (Note)

```json
{
  "_id": "grd::s001::cls_3a::math::stu_xyz789::T1::DS1",
  "type": "grade",
  "_schemaVersion": 1,
  "schoolId": "sch::s001",
  "academicYearId": "ay::2024-2025",
  "classId": "cls::cls_3a",
  "studentId": "stu::stu_xyz789",
  "subjectId": "sub::math",
  "subjectName": "Mathématiques",
  "teacherId": "user::u_teacher01",
  "evaluationId": "eval::eval_001",
  "gradeType": "devoir",
  "value": 16.5,
  "maxValue": 20,
  "coefficient": 2,
  "period": "T1",
  "evaluationDate": "2024-10-15",
  "comment": "Bon travail",
  "requiresReview": false,
  "locked": false,
  "updatedAt": "2024-10-15T12:00:00Z",
  "createdAt": "2024-10-15T11:30:00Z"
}
```
> **Décision** : Format d'`_id` composite garantit l'unicité et évite une requête de recherche. `subjectName` dénormalisé pour lecture directe sans jointure.

---

### 3.8 REPORT_CARD (Bulletin)

```json
{
  "_id": "rc::s001::cls_3a::stu_xyz789::T1",
  "type": "report_card",
  "_schemaVersion": 1,
  "schoolId": "sch::s001",
  "academicYearId": "ay::2024-2025",
  "classId": "cls::cls_3a",
  "studentId": "stu::stu_xyz789",
  "studentName": "MBEMBA Grâce",
  "period": "T1",
  "status": "published",
  "subjectGrades": [
    { "subjectId": "sub::math",  "subjectName": "Mathématiques", "coefficient": 2, "average": 15.75, "rank": 3 },
    { "subjectId": "sub::fr",    "subjectName": "Français",      "coefficient": 3, "average": 13.0,  "rank": 7 },
    { "subjectId": "sub::svt",   "subjectName": "SVT",           "coefficient": 2, "average": 17.5,  "rank": 1 }
  ],
  "generalAverage": 15.2,
  "classAverage": 12.8,
  "rank": 4,
  "totalStudents": 42,
  "absencesCount": 2,
  "mainTeacherComment": "Élève sérieuse, continue ainsi.",
  "directorComment": "",
  "councilDecision": "",
  "hasCongratulations": false,
  "hasEncouragements": true,
  "hasWorkWarning": false,
  "hasConductWarning": false,
  "generatedAt": "2024-11-20T10:00:00Z",
  "publishedAt": "2024-11-21T08:00:00Z",
  "publishedById": "user::u_abc123",
  "updatedAt": "2024-11-21T08:00:00Z"
}
```

---

### 3.9 INVOICE (Facture avec items intégrés)

```json
{
  "_id": "inv::INV-2024-0001",
  "type": "invoice",
  "_schemaVersion": 1,
  "invoiceNumber": "INV-2024-0001",
  "schoolId": "sch::s001",
  "academicYearId": "ay::2024-2025",
  "direction": "income",
  "invoiceType": "tuition",
  "status": "paid",
  "studentId": "stu::stu_xyz789",
  "studentName": "MBEMBA Grâce",
  "classId": "cls::cls_3a",
  "items": [
    { "feeId": "fee::frais-scolarite-3e", "description": "Frais de scolarité 3ème",
      "quantity": 1, "unitPrice": 350000, "discount": 0, "total": 350000 }
  ],
  "subtotal": 350000,
  "discount": 0,
  "taxRate": 0,
  "taxAmount": 0,
  "total": 350000,
  "paidAmount": 350000,
  "currency": "XAF",
  "dueDate": "2024-10-01",
  "paidAt": "2024-09-15T10:30:00Z",
  "payments": [
    { "id": "ipmt::001", "amount": 350000, "method": "cash",
      "reference": "REC-2024-0042", "paidAt": "2024-09-15T10:30:00Z",
      "recordedById": "user::u_cashier01" }
  ],
  "notifications": [
    { "type": "email", "status": "sent", "createdAt": "2024-09-01T08:00:00Z" }
  ],
  "createdAt": "2024-09-01T00:00:00Z",
  "updatedAt": "2024-09-15T10:30:00Z"
}
```
> **Décision** : `invoice_items`, `invoice_payments`, `invoice_notifications` sont tous intégrés dans le document `invoice`. Une facture est une unité cohérente immuable une fois payée.

---

### 3.10 EXPENSE (Dépense)

```json
{
  "_id": "exp::EXP-2024-0015",
  "type": "expense",
  "_schemaVersion": 1,
  "expenseNumber": "EXP-2024-0015",
  "schoolId": "sch::s001",
  "academicYearId": "ay::2024-2025",
  "title": "Achat fournitures bureau",
  "category": "fournitures",
  "amount": 45000,
  "currency": "XAF",
  "supplier": {
    "id": "sup::sup001",
    "name": "Papeterie Centrale Brazzaville"
  },
  "paymentMethod": "cash",
  "paymentReference": "BN-2024-015",
  "paidAt": "2024-10-05T14:00:00Z",
  "receiptUrl": "https://cdn.epilote.cg/receipts/exp_2024_0015.pdf",
  "status": "paid",
  "approvedById": "user::u_abc123",
  "approvedAt": "2024-10-05T09:00:00Z",
  "budgetLineId": "bgl::fournitures-2024",
  "auditTrail": [
    { "action": "created",  "userId": "user::u_finance01", "at": "2024-10-04T16:00:00Z" },
    { "action": "approved", "userId": "user::u_abc123",    "at": "2024-10-05T09:00:00Z" },
    { "action": "paid",     "userId": "user::u_finance01", "at": "2024-10-05T14:00:00Z" }
  ],
  "createdAt": "2024-10-04T16:00:00Z",
  "updatedAt": "2024-10-05T14:00:00Z"
}
```
> **Décision** : `expense_audits` fusionné dans `auditTrail` (max 10 actions par dépense). Données fournisseur dénormalisées (nom) pour lecture directe.

---

### 3.11 BUDGET (avec lignes budgétaires intégrées)

```json
{
  "_id": "bgt::s001::2024-2025",
  "type": "budget",
  "_schemaVersion": 1,
  "schoolId": "sch::s001",
  "academicYearId": "ay::2024-2025",
  "name": "Budget Exercice 2024-2025",
  "fiscalYear": "2024-2025",
  "totalAmount": 25000000,
  "allocatedAmount": 22000000,
  "spentAmount": 8500000,
  "currency": "XAF",
  "status": "approved",
  "approvedById": "user::u_abc123",
  "approvedAt": "2024-09-01T00:00:00Z",
  "lines": [
    { "id": "bgl::salaires-2024", "code": "60100", "name": "Salaires enseignants",
      "category": "personnel", "plannedAmount": 15000000,
      "allocatedAmount": 15000000, "spentAmount": 5000000 },
    { "id": "bgl::fournitures-2024", "code": "60200", "name": "Fournitures bureau",
      "category": "fournitures", "plannedAmount": 500000,
      "allocatedAmount": 500000, "spentAmount": 245000 }
  ],
  "revisions": [
    { "userId": "user::u_abc123", "action": "approved",
      "reason": "Budget validé en conseil", "createdAt": "2024-09-01T00:00:00Z" }
  ],
  "createdAt": "2024-08-15T00:00:00Z",
  "updatedAt": "2024-09-01T00:00:00Z"
}
```
> **Décision** : `budget_lines` (< 50 lignes par budget) et `budget_revisions` (< 20 révisions) intégrés. Document unique par école × année.

---

### 3.12 ACCOUNTING_PIECE (Pièce comptable avec lignes)

```json
{
  "_id": "acp::PCE-2024-0001",
  "type": "accounting_piece",
  "_schemaVersion": 1,
  "pieceNumber": "PCE-2024-0001",
  "schoolId": "sch::s001",
  "academicYearId": "ay::2024-2025",
  "journalCode": "BQ",
  "journalType": "bank",
  "pieceDate": "2024-09-15",
  "description": "Paiement frais scolarité MBEMBA Grâce",
  "reference": "INV-2024-0001",
  "documentType": "invoice",
  "documentRef": "INV-2024-0001",
  "totalDebit": 350000,
  "totalCredit": 350000,
  "isBalanced": true,
  "status": "posted",
  "fiscalPeriod": "2024-09",
  "fiscalYear": 2024,
  "invoiceId": "inv::INV-2024-0001",
  "lines": [
    { "id": "apl::001", "accountCode": "411000", "accountName": "Clients",
      "debitAmount": 350000, "creditAmount": 0, "label": "Frais scolarité MBEMBA", "lineOrder": 1 },
    { "id": "apl::002", "accountCode": "706000", "accountName": "Prestations services",
      "debitAmount": 0, "creditAmount": 350000, "label": "Frais scolarité T1", "lineOrder": 2 }
  ],
  "createdById": "user::u_finance01",
  "validatedById": "user::u_abc123",
  "validatedAt": "2024-09-15T11:00:00Z",
  "createdAt": "2024-09-15T10:30:00Z",
  "updatedAt": "2024-09-15T11:00:00Z"
}
```
> **Décision** : `accounting_piece_lines` (2–20 lignes par pièce) intégrées. `accounting_entries` (ancien modèle) migré vers `accounting_pieces` (nouveau modèle).

---

### 3.13 STAFF (Personnel)

```json
{
  "_id": "stf::stf_ens001",
  "type": "staff",
  "_schemaVersion": 1,
  "schoolId": "sch::s001",
  "userId": "user::u_teacher01",
  "employeeId": "EMP-2024-001",
  "firstName": "Antoine",
  "lastName": "NGAKOSSO",
  "email": "a.ngakosso@sacrecoeur.cg",
  "phone": "+242 06 222 33 44",
  "staffType": "enseignant",
  "position": "Professeur de Mathématiques",
  "contractType": "CDI",
  "hireDate": "2020-09-01",
  "baseSalary": 450000,
  "currency": "XAF",
  "status": "active",
  "qualifications": [
    { "degree": "Licence de Mathématiques", "institution": "Université Marien Ngouabi", "year": 2015 }
  ],
  "subjectIds": ["sub::math", "sub::physique"],
  "classIds": ["cls::cls_3a", "cls::cls_2b"],
  "workSchedule": [
    { "dayOfWeek": 1, "startTime": "07:30", "endTime": "16:00", "breakStart": "12:00", "breakEnd": "13:30" },
    { "dayOfWeek": 2, "startTime": "07:30", "endTime": "16:00", "breakStart": "12:00", "breakEnd": "13:30" }
  ],
  "bankAccount": "XXXXXXXXXXXXXX",
  "bankName": "LCB",
  "createdAt": "2020-09-01T00:00:00Z",
  "updatedAt": "2025-01-01T00:00:00Z"
}
```
> **Décision** : `staff_work_schedules` (7 lignes max) intégrés dans `workSchedule`. `qualifications` et `experiences` déjà en JSONB dans PG → mapping direct.

---

### 3.14 PAYSLIP (Fiche de paie)

```json
{
  "_id": "psl::s001::stf_ens001::2024-10",
  "type": "payslip",
  "_schemaVersion": 1,
  "schoolId": "sch::s001",
  "staffId": "stf::stf_ens001",
  "staffName": "NGAKOSSO Antoine",
  "month": 10,
  "year": 2024,
  "fiscalPeriod": "2024-10",
  "baseSalary": 450000,
  "grossSalary": 495000,
  "totalDeductions": 74250,
  "netSalary": 420750,
  "currency": "XAF",
  "items": [
    { "label": "Salaire de base",       "type": "earning",    "amount": 450000 },
    { "label": "Prime d'ancienneté",    "type": "earning",    "amount": 45000  },
    { "label": "CNSS (5.04%)",          "type": "deduction",  "amount": 24948  },
    { "label": "IRPP",                  "type": "deduction",  "amount": 49302  }
  ],
  "status": "paid",
  "paymentMethod": "virement",
  "paymentDate": "2024-10-28",
  "transactionReference": "VRT-2024-10-001",
  "budgetLineId": "bgl::salaires-2024",
  "accountingLocked": true,
  "auditTrail": [
    { "action": "generated", "performedBy": "user::u_rh01",     "performedAt": "2024-10-25T09:00:00Z" },
    { "action": "approved",  "performedBy": "user::u_abc123",   "performedAt": "2024-10-26T14:00:00Z" },
    { "action": "paid",      "performedBy": "user::u_finance01","performedAt": "2024-10-28T10:00:00Z" }
  ],
  "createdAt": "2024-10-25T09:00:00Z",
  "updatedAt": "2024-10-28T10:00:00Z"
}
```
> **Décision** : `payslip_audit_logs` intégrés dans `auditTrail` (max 5 actions). `_id` composite garantit unicité staff × période.

---

### 3.15 SYNC_MUTATION

```json
{
  "_id": "mut::cli_dev01::1728400000000::grades",
  "type": "sync_mutation",
  "_schemaVersion": 1,
  "schoolId": "sch::s001",
  "clientMutationId": "cli_dev01::1728400000000::grades",
  "clientDeviceId": "dev::android::a1b2c3",
  "entityType": "grade",
  "entityId": "grd::s001::cls_3a::math::stu_xyz789::T1::DS1",
  "operation": "UPSERT",
  "payload": {
    "value": 16.5,
    "comment": "Bon travail",
    "updatedAt": "2024-10-15T11:30:00Z"
  },
  "previousState": { "value": 15.0 },
  "version": 42001,
  "status": "pending",
  "idempotencyKey": "cli_dev01::1728400000000::grd::s001::cls_3a::math",
  "naturalKeyHash": "sha256:abcdef...",
  "userId": "user::u_teacher01",
  "clientCreatedAt": "2024-10-15T11:30:00Z",
  "serverReceivedAt": "2024-10-15T12:00:00Z",
  "retryCount": 0,
  "source": "couchbase_lite",
  "operationScope": "entity",
  "entityVersion": 3,
  "createdAt": "2024-10-15T12:00:00Z",
  "updatedAt": "2024-10-15T12:00:00Z"
}
```

---

### 3.16 SYNC_CONFLICT

```json
{
  "_id": "cnf::mut_001::mut_002",
  "type": "sync_conflict",
  "_schemaVersion": 1,
  "schoolId": "sch::s001",
  "mutationId": "mut::cli_dev01::1728400000000::grades",
  "entityType": "grade",
  "entityId": "grd::s001::cls_3a::math::stu_xyz789::T1::DS1",
  "conflictType": "update_update",
  "status": "pending",
  "priority": "high",
  "clientValue": { "value": 16.5, "updatedAt": "2024-10-15T11:30:00Z", "userId": "user::u_teacher01" },
  "serverValue": { "value": 14.0, "updatedAt": "2024-10-15T11:29:00Z", "userId": "user::u_teacher02" },
  "clientUserId": "user::u_teacher01",
  "clientTimestamp": "2024-10-15T11:30:00Z",
  "serverTimestamp": "2024-10-15T11:29:00Z",
  "resolution": "client_wins",
  "resolvedValue": { "value": 16.5 },
  "resolvedById": "user::u_abc123",
  "resolvedAt": "2024-10-15T14:00:00Z",
  "resolutionNote": "Résolu par LWW — timestamp client plus récent",
  "conflictHash": "sha256:xyz...",
  "metadata": { "deviceId": "dev::android::a1b2c3", "appVersion": "1.0.0" },
  "createdAt": "2024-10-15T12:00:00Z",
  "updatedAt": "2024-10-15T14:00:00Z"
}
```

---

### 3.17 LEDGER_ENTRY (Audit financier immuable)

```json
{
  "_id": "ldg::fin::s001::1728400000001",
  "type": "ledger_entry",
  "ledgerType": "financial",
  "_schemaVersion": 1,
  "schoolId": "sch::s001",
  "entityId": "inv::INV-2024-0001",
  "entityType": "invoice",
  "mutationId": "mut::cli_dev01::1728400000001::invoices",
  "action": "PAYMENT_RECEIVED",
  "payloadBefore": { "status": "pending", "paidAmount": 0 },
  "payloadAfter":  { "status": "paid",    "paidAmount": 350000 },
  "previousHash": "sha256:aaabbbccc...",
  "currentHash":  "sha256:dddeeefff...",
  "createdBy": "user::u_cashier01",
  "createdAt": "2024-09-15T10:30:00Z"
}
```
> **Décision** : `academic_ledger`, `financial_ledger`, `discipline_ledger` unifiés dans `ledger_entries` avec un champ `ledgerType` discriminant. Structure de hash chaîné préservée pour garantir l'intégrité.

---

## 4. STRATÉGIE DE MIGRATION

### 4.1 Vue d'ensemble ETL

```
PostgreSQL (source)
    │
    ▼ EXTRACT
pgdump / COPY TO CSV / Prisma query
    │
    ▼ TRANSFORM
Script Node.js/Kotlin (mapping + enrichissement)
    │
    ▼ LOAD
Couchbase Kotlin SDK (bulk upsert par batch de 500)
    │
    ▼ VERIFY
Counts + checksum + index validation
```

### 4.2 Ordre de migration (dépendances respectées)

```
Phase 1 — Référentiel global (pas de dépendances)
  1. business_categories + modules              → config::modules
  2. subscription_plans + plan_modules + pricing → subscription_plans
  3. chart_of_accounts                          → config::chart_of_accounts
  4. school_cycles + grade_levels + filieres    → (intégré dans academic_config)

Phase 2 — Structure organisationnelle
  5. school_groups + subscriptions              → school_groups
  6. schools + school_cycle_assignments         → schools
  7. academic_years                             → academic_config (1 doc/école/année)
  8. access_profiles + access_profile_modules   → (intégré dans users)

Phase 3 — Utilisateurs & Personnel
  9.  users + user_modules                      → users
  10. staff + staff_work_schedules              → staff

Phase 4 — Élèves & Inscriptions
  11. students + medical_records + school_history → students
  12. inscriptions + inscription_state_history    → inscriptions

Phase 5 — Structure pédagogique
  13. subjects                                  → config::subjects (par école)
  14. classes                                   → academic_config ou collection dédiée
  15. time_slots                                → timetable

Phase 6 — Pédagogie opérationnelle
  16. evaluations                               → evaluations
  17. grades                                    → grades
  18. attendances                               → attendances
  19. report_cards                              → report_cards

Phase 7 — Finance
  20. fees                                      → config::fees
  21. invoices + invoice_items + invoice_payments + invoice_notifications → invoices
  22. payments                                  → (intégré dans invoices)
  23. expenses + expense_audits                 → expenses
  24. budgets + budget_lines + budget_revisions → budgets
  25. suppliers                                 → config::suppliers

Phase 8 — Comptabilité & RH
  26. accounting_pieces + accounting_piece_lines → accounting_pieces
  27. accounting_entries                         → accounting_pieces (fusion)
  28. fiscal_years + fiscal_period_closes        → config::fiscal
  29. payslips + payslip_audit_logs              → payslips
  30. leave_requests                             → staff (ou collection dédiée)

Phase 9 — Sync & Audit (migration à chaud)
  31. sync_mutations (pending uniquement)       → sync_mutations
  32. sync_conflicts (pending uniquement)       → sync_conflicts
  33. academic_ledger + financial_ledger + discipline_ledger → ledger_entries
  34. audit_logs                                → audit_logs

Phase 10 — Communication & Divers
  35. messages, announcements, notifications    → messages, notifications
  36. disciplines, infirmary_visits             → disciplines
  37. documents + document_versions             → documents
  38. books + loans                             → books
  39. transfers                                 → transfers
```

### 4.3 Script de migration type (Kotlin + Couchbase SDK)

```kotlin
// MigrationRunner.kt
import com.couchbase.client.kotlin.Cluster
import kotlinx.serialization.json.*

suspend fun migrateStudents(pgRows: List<StudentPgRow>, cluster: Cluster) {
    val collection = cluster
        .bucket("epilote_prod")
        .defaultScope()
        .collection("students")

    pgRows.chunked(500).forEach { batch ->
        val docs = batch.map { row ->
            val id = "stu::${row.id}"
            val json = buildJsonObject {
                put("_id",               id)
                put("type",              "student")
                put("_schemaVersion",    1)
                put("schoolId",          "sch::${row.schoolId}")
                put("matricule",         row.matricule)
                put("firstName",         row.firstName)
                put("lastName",          row.lastName)
                put("currentClassId",    row.classId?.let { "cls::$it" } ?: JsonNull)
                put("isActive",          row.isActive)
                put("updatedAt",         row.updatedAt.toIsoString())
                put("createdAt",         row.createdAt.toIsoString())
                // medical_records joint depuis la table séparée
                put("medicalSummary", buildJsonObject {
                    put("bloodType",         row.medicalRecord?.bloodType ?: JsonNull)
                    put("allergies",         JsonArray(row.medicalRecord?.allergies ?: emptyList()))
                    put("chronicConditions", JsonArray(row.medicalRecord?.chronicConditions ?: emptyList()))
                })
            }
            id to json
        }

        // Bulk upsert
        docs.forEach { (id, json) ->
            collection.upsert(id, json)
        }
        println("Batch migré : ${batch.size} étudiants")
    }
}
```

### 4.4 Outils recommandés

| Outil | Usage |
|---|---|
| **pgdump + COPY TO CSV** | Export initial PostgreSQL |
| **Kotlin + Couchbase Kotlin SDK** | Script de migration (cohérent avec la stack projet) |
| **Couchbase cbimport** | Import massif CSV/JSON pour données statiques |
| **Flyway ou script shell** | Orchestration des phases de migration |
| **Node.js + pg + couchbase** | Alternative pour les équipes JS |
| **Couchbase Data Sync (cbmigrate)** | Pour les gros volumes > 1M docs |

---

## 5. RISQUES ET RECOMMANDATIONS

### 5.1 Risques financiers

| Risque | Impact | Mitigation |
|---|---|---|
| Perte de chaîne de hash dans les ledgers | Critique — intégrité comptable | Migrer les ledgers en dernier, vérifier chaque hash après import |
| Arrondi décimal (numeric PG → double JSON) | Élevé — erreurs financières | Stocker les montants en **centimes entiers** (XAF = entier, pas de décimales) |
| Pièces comptables non équilibrées après migration | Élevé | Vérifier `totalDebit == totalCredit` sur 100% des pièces migrées |

### 5.2 Risques sync

| Risque | Impact | Mitigation |
|---|---|---|
| Mutations `pending` re-appliquées en double | Élevé — données corrompues | Migrer uniquement les mutations non traitées, avec `idempotencyKey` |
| Conflits non résolus perdus | Moyen | Migrer les conflits `pending` dans `sync_conflicts`, alerter les directeurs |

### 5.3 Risques structurels

| Risque | Impact | Mitigation |
|---|---|---|
| Documents trop volumineux (students avec historique long) | Moyen — perf CBLite | Tronquer `schoolHistory` à N-2 années, archiver le reste cloud-only |
| Collections dépassant 100 MB sur CBLite | Moyen | Activer le TTL glissant (1 année scolaire en local, archives sur Capella) |
| Champs NULL vs champs absents | Faible | Omettre les champs null dans les documents Couchbase (JSON sparse) |

### 5.4 Recommandations post-migration

1. **Valider les comptages** : `SELECT COUNT(*) FROM invoices` en PG doit correspondre au `SELECT COUNT(*) FROM epilote_prod._default.invoices` en N1QL.
2. **Créer tous les index** (voir `capella-indexes.sql`) avant d'ouvrir le trafic.
3. **Migrer en shadow mode** : pendant 2 semaines, écrire simultanément en PG et Couchbase, comparer les résultats.
4. **Basculement final** : désactiver PG uniquement après 5 jours de shadow mode sans divergence.
5. **Conserver PG en lecture seule** 90 jours post-migration pour rollback d'urgence.

---

## 6. COLLECTIONS COUCHBASE — RÉSUMÉ FINAL

| Collection | Nb docs estimé (MVP 5 écoles) | TTL local CBLite |
|---|---|---|
| `users` | ~500 | permanent |
| `schools` | ~5 | permanent |
| `school_groups` | ~2 | permanent |
| `academic_config` | ~5 (1/école) | permanent |
| `students` | ~2 500 | année courante |
| `inscriptions` | ~3 000 | 2 ans |
| `grades` | ~150 000 | 1 an |
| `report_cards` | ~8 000 | 1 an |
| `evaluations` | ~3 000 | 1 an |
| `attendances` | ~200 000 | 1 an |
| `invoices` | ~5 000 | 2 ans |
| `expenses` | ~2 000 | 2 ans |
| `budgets` | ~10 | permanent |
| `accounting_pieces` | ~10 000 | 2 ans |
| `staff` | ~200 | permanent |
| `payslips` | ~2 400 | 2 ans |
| `sync_mutations` | ~50 000 | 30 jours |
| `sync_conflicts` | ~200 | 90 jours |
| `audit_logs` | ~500 000 | cloud seulement |
| `ledger_entries` | ~30 000 | cloud seulement |
| `messages` | ~10 000 | 6 mois |
| `documents` | ~5 000 | cloud seulement |
| `config` | ~50 | permanent |

*Avril 2026 — Document de migration PostgreSQL → Couchbase, E-PILOTE CONGO*
