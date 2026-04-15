package cg.epilote.shared.data.schema

/**
 * Canonical schema contract for E-PILOTE documents.
 *
 * Every document stored in Couchbase Lite or Capella MUST include the
 * mandatory fields listed here. This object is the single source of truth
 * for field names, collection names, and schema versioning.
 *
 * Rules:
 * - All field names use English camelCase.
 * - Legacy aliases (ecoleId, groupeId, eleves, notes…) are READ-ONLY
 *   during the migration period; all NEW writes use canonical names.
 * - [CURRENT_SCHEMA_VERSION] must be bumped whenever the document
 *   structure changes in a non-backward-compatible way.
 */
object SchemaContract {

    const val CURRENT_SCHEMA_VERSION = 1

    // ── Mandatory metadata fields (every document) ─────────────────
    const val FIELD_SCHEMA_VERSION   = "schemaVersion"
    const val FIELD_ENTITY_TYPE      = "entityType"
    const val FIELD_SCOPE_TYPE       = "scopeType"
    const val FIELD_SCHOOL_ID        = "schoolId"
    const val FIELD_GROUP_ID         = "groupId"
    const val FIELD_CREATED_AT       = "createdAt"
    const val FIELD_UPDATED_AT       = "updatedAt"
    const val FIELD_LAST_MODIFIED_BY = "lastModifiedBy"
    const val FIELD_DEVICE_ID        = "deviceId"

    // ── Scope types ────────────────────────────────────────────────
    const val SCOPE_GLOBAL = "global"
    const val SCOPE_GROUP  = "group"
    const val SCOPE_SCHOOL = "school"

    // ── Canonical collection names ─────────────────────────────────
    object Collections {
        const val GRADES          = "grades"
        const val ATTENDANCES     = "attendances"
        const val STUDENTS        = "students"
        const val INSCRIPTIONS    = "inscriptions"
        const val TIMETABLE       = "timetable"
        const val REPORT_CARDS    = "report_cards"
        const val DISCIPLINES     = "disciplines"
        const val ACADEMIC_CONFIG = "academic_config"
        const val STAFF           = "staff"
        const val STAFF_ATTENDANCES = "staff_attendances"
        const val ANNOUNCEMENTS   = "announcements"
        const val MESSAGES        = "messages"
        const val NOTIFICATIONS   = "notifications"
        const val INVOICES        = "invoices"
        const val EXPENSES        = "expenses"
        const val BUDGETS         = "budgets"
        const val PAYSLIPS        = "payslips"
        const val ACCOUNTING      = "accounting_pieces"
        const val CONFIG          = "config"
        const val SCHOOL_GROUPS   = "school_groups"
        const val USERS           = "users"
        const val SCHOOLS         = "schools"
        const val SESSIONS        = "sessions"
    }

    // ── Legacy field aliases (read-only during migration) ──────────
    object LegacyAliases {
        val SCHOOL_ID = listOf("ecoleId")
        val GROUP_ID  = listOf("groupeId")
    }
}
