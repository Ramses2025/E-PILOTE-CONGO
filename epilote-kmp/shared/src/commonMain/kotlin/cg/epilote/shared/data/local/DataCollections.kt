package cg.epilote.shared.data.local

object DataCollections {
    val synced = listOf(
        "grades", "attendances", "students", "inscriptions",
        "timetable", "report_cards", "disciplines",
        "academic_config", "staff", "staff_attendances",
        "announcements", "messages", "notifications"
    )

    val localOnly = listOf(
        "config", "school_groups",
        "users", "schools",
        "invoices", "expenses", "budgets", "payslips", "accounting_pieces"
    )

    val all = synced + localOnly
}
