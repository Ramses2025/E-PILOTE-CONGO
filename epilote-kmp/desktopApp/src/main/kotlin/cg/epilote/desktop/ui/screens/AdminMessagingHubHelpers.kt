package cg.epilote.desktop.ui.screens

import androidx.compose.ui.graphics.Color

internal fun messageMailboxEmptyTitle(mailbox: AdminMessagingMailbox): String = when (mailbox) {
    AdminMessagingMailbox.INBOX -> "Boîte de réception vide"
    AdminMessagingMailbox.SENT -> "Aucun envoi"
    AdminMessagingMailbox.ANNOUNCEMENTS -> "Aucune annonce officielle"
    AdminMessagingMailbox.ARCHIVED -> "Aucune archive"
    AdminMessagingMailbox.TRASH -> "Corbeille vide"
}

internal fun messageMailboxEmptyMessage(mailbox: AdminMessagingMailbox): String = when (mailbox) {
    AdminMessagingMailbox.INBOX -> "Les messages reçus et annonces officielles apparaîtront ici."
    AdminMessagingMailbox.SENT -> "Les messages envoyés par le super-admin apparaîtront ici."
    AdminMessagingMailbox.ANNOUNCEMENTS -> "Les annonces officielles publiées sur la plateforme apparaîtront ici."
    AdminMessagingMailbox.ARCHIVED -> "Les fils archivés seront conservés ici."
    AdminMessagingMailbox.TRASH -> "Les fils supprimés pourront être restaurés depuis cette corbeille."
}

internal fun messageThreadActionTitle(status: String): String = when (status) {
    "archived" -> "Archiver le fil"
    "deleted" -> "Supprimer le fil"
    else -> "Restaurer le fil"
}

internal fun messageThreadActionMessage(status: String): String = when (status) {
    "archived" -> "Le fil sera déplacé dans les archives et restera consultable."
    "deleted" -> "Le fil sera déplacé dans la corbeille. Vous pourrez encore le restaurer plus tard."
    else -> "Le fil sera rétabli dans la boîte d'envoi active."
}

internal fun messageThreadActionSuccess(status: String): String = when (status) {
    "archived" -> "Fil archivé avec succès"
    "deleted" -> "Fil déplacé dans la corbeille"
    else -> "Fil restauré avec succès"
}

internal fun messageThreadActionColor(status: String): Color = when (status) {
    "deleted" -> Color(0xFFD62828)
    "archived" -> Color(0xFF475569)
    else -> Color(0xFF1D3557)
}
