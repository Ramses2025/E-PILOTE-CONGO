package cg.epilote.android.ui.navigation

object NavRoutes {
    const val LOGIN      = "login"
    const val PIN_SETUP  = "pin_setup"
    const val PIN_UNLOCK = "pin_unlock"
    const val DASHBOARD  = "dashboard"
    const val CLASSES    = "classes"
    const val MATIERES   = "matieres/{classeId}/{classeNom}"
    const val NOTES      = "notes/{classeId}/{matiereId}/{periode}"
    const val ABSENCES   = "absences/{classeId}"
    const val CONFLICTS  = "conflicts"
    const val BULLETINS  = "bulletins/{classeId}/{periode}"
}
