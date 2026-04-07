# ============================================================
# E-PILOTE CONGO — Setup Capella automatique
# Crée toutes les collections + insère les documents de base
# 
# PRÉREQUIS :
#   1. Remplir les variables ci-dessous
#   2. Dans Capella → Settings → API Keys → Create API Key
#      avec accès "Data Writer" sur le cluster
#   3. Lancer : .\setup-capella.ps1
# ============================================================

# ── CONFIGURATION — À REMPLIR ──────────────────────────────
$CLUSTER_ID    = "VOTRE_CLUSTER_ID"       # Capella → Cluster → Settings → Cluster ID
$TENANT_ID     = "VOTRE_ORGANIZATION_ID"  # Capella → Organization → Settings → Organization ID
$PROJECT_ID    = "VOTRE_PROJECT_ID"       # Capella → Projects → votre projet → ID dans l'URL
$API_KEY       = "VOTRE_API_KEY"          # Capella → API Keys
$API_SECRET    = "VOTRE_API_SECRET"
$BUCKET        = "epilote_prod"
$SCOPE         = "_default"

# URL de base Capella Management API
$BASE_URL = "https://cloudapi.cloud.couchbase.com/v4/organizations/$TENANT_ID/projects/$PROJECT_ID/clusters/$CLUSTER_ID"

# Headers communs
$CRED    = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${API_KEY}:${API_SECRET}"))
$HEADERS = @{
    "Authorization" = "Basic $CRED"
    "Content-Type"  = "application/json"
}

# ── LISTE DES COLLECTIONS ───────────────────────────────────
$COLLECTIONS = @(
    "config",
    "school_groups",
    "schools",
    "academic_config",
    "users",
    "staff",
    "students",
    "inscriptions",
    "evaluations",
    "grades",
    "report_cards",
    "timetable",
    "attendances",
    "staff_attendances",
    "invoices",
    "expenses",
    "budgets",
    "accounting_pieces",
    "payslips",
    "sync_mutations",
    "sync_conflicts",
    "audit_logs",
    "ledger_entries",
    "messages",
    "notifications",
    "documents",
    "books",
    "disciplines",
    "transfers"
)

# ── ÉTAPE 1 : Créer les collections ─────────────────────────
Write-Host "`n=== Creation des collections dans $BUCKET.$SCOPE ===" -ForegroundColor Cyan

foreach ($coll in $COLLECTIONS) {
    $url  = "$BASE_URL/buckets/$BUCKET/scopes/$SCOPE/collections"
    $body = @{ name = $coll; maxTTL = 0 } | ConvertTo-Json

    try {
        $resp = Invoke-RestMethod -Method POST -Uri $url -Headers $HEADERS -Body $body -ErrorAction Stop
        Write-Host "  [OK] $coll" -ForegroundColor Green
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -eq 409) {
            Write-Host "  [SKIP] $coll (existe deja)" -ForegroundColor Yellow
        } else {
            Write-Host "  [ERR] $coll — $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    Start-Sleep -Milliseconds 300  # éviter rate limiting
}

# ── ÉTAPE 2 : Créer les index via Query REST API ─────────────
Write-Host "`n=== Creation des index ===" -ForegroundColor Cyan

# URL du service Query (port 18093 sur Capella)
$QUERY_URL = "https://$((Invoke-RestMethod "$BASE_URL" -Headers $HEADERS).config.endpoints.queryEndpoint)/query/service"

$INDEXES = @(
    "CREATE INDEX idx_users_username  ON ``epilote_prod``._default.``users``(username)  WHERE type='user'",
    "CREATE INDEX idx_users_school    ON ``epilote_prod``._default.``users``(schoolId)  WHERE type='user'",
    "CREATE INDEX idx_users_group     ON ``epilote_prod``._default.``users``(groupId)   WHERE type='user'",
    "CREATE INDEX idx_schools_group   ON ``epilote_prod``._default.``schools``(groupId) WHERE type='school'",
    "CREATE INDEX idx_acfg_school     ON ``epilote_prod``._default.``academic_config``(schoolId) WHERE type='academic_config'",
    "CREATE INDEX idx_students_school ON ``epilote_prod``._default.``students``(schoolId) WHERE type='student'",
    "CREATE INDEX idx_students_class  ON ``epilote_prod``._default.``students``(schoolId,currentClassId) WHERE type='student'",
    "CREATE INDEX idx_inscriptions_sy ON ``epilote_prod``._default.``inscriptions``(schoolId,academicYearId) WHERE type='inscription'",
    "CREATE INDEX idx_grades_class    ON ``epilote_prod``._default.``grades``(schoolId,classId,period) WHERE type='grade'",
    "CREATE INDEX idx_grades_student  ON ``epilote_prod``._default.``grades``(schoolId,studentId,period) WHERE type='grade'",
    "CREATE INDEX idx_grades_review   ON ``epilote_prod``._default.``grades``(schoolId,requiresReview) WHERE type='grade' AND requiresReview=true",
    "CREATE INDEX idx_rc_class        ON ``epilote_prod``._default.``report_cards``(schoolId,classId,period) WHERE type='report_card'",
    "CREATE INDEX idx_att_class_date  ON ``epilote_prod``._default.``attendances``(schoolId,classId,date) WHERE type='attendance'",
    "CREATE INDEX idx_att_student     ON ``epilote_prod``._default.``attendances``(schoolId,studentId) WHERE type='attendance'",
    "CREATE INDEX idx_invoices_school ON ``epilote_prod``._default.``invoices``(schoolId,status) WHERE type='invoice'",
    "CREATE INDEX idx_invoices_student ON ``epilote_prod``._default.``invoices``(schoolId,studentId) WHERE type='invoice'",
    "CREATE INDEX idx_expenses_school ON ``epilote_prod``._default.``expenses``(schoolId,status) WHERE type='expense'",
    "CREATE INDEX idx_budgets_school  ON ``epilote_prod``._default.``budgets``(schoolId,academicYearId) WHERE type='budget'",
    "CREATE INDEX idx_staff_school    ON ``epilote_prod``._default.``staff``(schoolId) WHERE type='staff'",
    "CREATE INDEX idx_payslips_staff  ON ``epilote_prod``._default.``payslips``(schoolId,staffId,year,month) WHERE type='payslip'",
    "CREATE INDEX idx_mut_status      ON ``epilote_prod``._default.``sync_mutations``(schoolId,status) WHERE type='sync_mutation'",
    "CREATE INDEX idx_cnf_status      ON ``epilote_prod``._default.``sync_conflicts``(schoolId,status) WHERE type='sync_conflict'",
    "CREATE INDEX idx_config_type     ON ``epilote_prod``._default.``config``(type)"
)

foreach ($sql in $INDEXES) {
    $body = @{ statement = $sql } | ConvertTo-Json
    try {
        Invoke-RestMethod -Method POST -Uri $QUERY_URL -Headers $HEADERS -Body $body -ErrorAction Stop | Out-Null
        $name = ($sql -split " ")[2]
        Write-Host "  [OK] $name" -ForegroundColor Green
    } catch {
        Write-Host "  [ERR] $sql`n       $($_.Exception.Message)" -ForegroundColor Red
    }
    Start-Sleep -Milliseconds 500
}

# ── ÉTAPE 3 : Insérer les documents de référence ─────────────
Write-Host "`n=== Insertion des documents de reference ===" -ForegroundColor Cyan

$SEED_DOCS = @(
    @{
        collection = "config"
        key        = "config::modules"
        doc        = @{
            _id            = "config::modules"
            type           = "config_modules"
            _schemaVersion = 1
            modules        = @(
                @{ id="mod::notes";    slug="notes";    name="Saisie des notes";     isCore=$true;  requiredPlan="gratuit" },
                @{ id="mod::absences"; slug="absences"; name="Suivi des absences";   isCore=$true;  requiredPlan="gratuit" },
                @{ id="mod::bulletins";slug="bulletins";name="Bulletins scolaires";  isCore=$true;  requiredPlan="gratuit" },
                @{ id="mod::finance";  slug="finance";  name="Gestion financiere";   isCore=$false; requiredPlan="premium" },
                @{ id="mod::rh";       slug="rh";       name="Ressources humaines";  isCore=$false; requiredPlan="premium" }
            )
            updatedAt = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
        }
    },
    @{
        collection = "config"
        key        = "config::plans"
        doc        = @{
            _id            = "config::plans"
            type           = "config_plans"
            _schemaVersion = 1
            plans          = @(
                @{ id="plan::gratuit";  name="Gratuit";  type="gratuit";  price=0;      maxStudents=100;  maxPersonnel=10  },
                @{ id="plan::standard"; name="Standard"; type="standard"; price=150000; maxStudents=500;  maxPersonnel=50  },
                @{ id="plan::premium";  name="Premium";  type="premium";  price=350000; maxStudents=2000; maxPersonnel=200 }
            )
            currency  = "XAF"
            updatedAt = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
        }
    },
    @{
        collection = "school_groups"
        key        = "grp::g001"
        doc        = @{
            _id            = "grp::g001"
            type           = "school_group"
            _schemaVersion = 1
            name           = "Groupe Scolaire Pilote Congo"
            slug           = "groupe-pilote-congo"
            email          = "admin@epilote.cg"
            country        = "Congo"
            city           = "Brazzaville"
            maxSchools     = 3
            maxUsersPerSchool = 50
            isActive       = $true
            subscription   = @{
                planId       = "plan::gratuit"
                planName     = "Gratuit"
                status       = "active"
                startDate    = "2026-01-01"
                endDate      = "2026-12-31"
            }
            createdAt = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
            updatedAt = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
        }
    },
    @{
        collection = "schools"
        key        = "sch::s001"
        doc        = @{
            _id            = "sch::s001"
            type           = "school"
            _schemaVersion = 1
            groupId        = "grp::g001"
            name           = "Lycee Pilote de Brazzaville"
            slug           = "lycee-pilote-bzv"
            schoolType     = "lycee"
            city           = "Brazzaville"
            province       = "brazzaville"
            schoolCode     = "LYC-BZV-001"
            isActive       = $true
            currentAcademicYearId = "ay::2025-2026"
            createdAt = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
            updatedAt = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
        }
    },
    @{
        collection = "academic_config"
        key        = "acfg::s001::2025-2026"
        doc        = @{
            _id            = "acfg::s001::2025-2026"
            type           = "academic_config"
            _schemaVersion = 1
            schoolId       = "sch::s001"
            academicYear   = @{ id="ay::2025-2026"; name="2025-2026"; startDate="2025-09-08"; endDate="2026-07-03"; isCurrent=$true }
            cycles         = @(
                @{
                    id="cycle::secondaire"; name="Secondaire"; code="SEC"
                    levels = @(
                        @{ id="lvl::6e"; name="Sixieme";   shortName="6eme"; orderIndex=1 },
                        @{ id="lvl::5e"; name="Cinquieme"; shortName="5eme"; orderIndex=2 },
                        @{ id="lvl::4e"; name="Quatrieme"; shortName="4eme"; orderIndex=3 },
                        @{ id="lvl::3e"; name="Troisieme"; shortName="3eme"; orderIndex=4 },
                        @{ id="lvl::2e"; name="Seconde";   shortName="2nde"; orderIndex=5 },
                        @{ id="lvl::1e"; name="Premiere";  shortName="1ere"; orderIndex=6 },
                        @{ id="lvl::te"; name="Terminale"; shortName="Tle";  orderIndex=7 }
                    )
                    filieres = @(
                        @{ id="fil::A"; name="Litteraire";          code="A" },
                        @{ id="fil::C"; name="Sciences exactes";    code="C" },
                        @{ id="fil::D"; name="Sciences naturelles"; code="D" }
                    )
                }
            )
            periods   = @("T1","T2","T3")
            updatedAt = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
        }
    }
)

foreach ($item in $SEED_DOCS) {
    $url  = "$BASE_URL/buckets/$BUCKET/scopes/$SCOPE/collections/$($item.collection)/docs/$($item.key)"
    $body = $item.doc | ConvertTo-Json -Depth 10 -Compress
    try {
        Invoke-RestMethod -Method PUT -Uri $url -Headers $HEADERS -Body $body -ErrorAction Stop | Out-Null
        Write-Host "  [OK] $($item.collection)/$($item.key)" -ForegroundColor Green
    } catch {
        Write-Host "  [ERR] $($item.collection)/$($item.key) — $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n=== Setup termine ! ===" -ForegroundColor Cyan
Write-Host "Verifier dans Capella Data Tools → Query :"
Write-Host "  SELECT COUNT(*) FROM ``epilote_prod``._default.``school_groups``;" -ForegroundColor White
