package cg.epilote.desktop.ui.screens

import androidx.compose.ui.graphics.Color
import cg.epilote.desktop.data.AdminMessageApiDto
import cg.epilote.desktop.data.AnnouncementApiDto
import cg.epilote.desktop.data.InvoiceApiDto
import cg.epilote.desktop.data.SubscriptionApiDto

internal data class AdminAuditEntry(
    val id: String,
    val timestamp: Long,
    val actor: String,
    val role: String,
    val action: String,
    val entity: String,
    val entityId: String,
    val details: String,
    val status: String,
    val sourceType: String
)

internal fun buildAdminAuditEntries(
    groupes: List<GroupeDto>,
    admins: List<AdminUserDto>,
    subscriptions: List<SubscriptionApiDto>,
    invoices: List<InvoiceApiDto>,
    announcements: List<AnnouncementApiDto>,
    messages: List<AdminMessageApiDto>
): List<AdminAuditEntry> {
    val currentTime = System.currentTimeMillis()
    val groupsById = groupes.associateBy { it.id }
    val adminsById = admins.associateBy { it.id }
    val entries = mutableListOf<AdminAuditEntry>()

    groupes.forEach { group ->
        entries += AdminAuditEntry(
            id = "group-created::${group.id}",
            timestamp = group.createdAt,
            actor = "Super-admin",
            role = "SUPER_ADMIN",
            action = "Création groupe",
            entity = "Groupe",
            entityId = group.id,
            details = "Groupe ${group.nom} créé avec le plan ${group.planId.ifBlank { "non défini" }}.",
            status = "success",
            sourceType = "group"
        )
        if (!group.isActive) {
            entries += AdminAuditEntry(
                id = "group-inactive::${group.id}",
                timestamp = group.createdAt,
                actor = "Super-admin",
                role = "SUPER_ADMIN",
                action = "Groupe désactivé",
                entity = "Groupe",
                entityId = group.id,
                details = "Le groupe ${group.nom} apparaît désactivé dans le référentiel plateforme.",
                status = "warning",
                sourceType = "group"
            )
        }
    }

    admins.forEach { admin ->
        val adminName = adminDisplayName(admin)
        entries += AdminAuditEntry(
            id = "admin-created::${admin.id}",
            timestamp = admin.createdAt,
            actor = "Super-admin",
            role = "SUPER_ADMIN",
            action = "Création administrateur",
            entity = "Administrateur",
            entityId = admin.id,
            details = "Compte ${adminName} créé avec le rôle ${admin.role}.",
            status = "success",
            sourceType = "admin"
        )
        admin.lastLoginAt?.takeIf { it > 0 }?.let { loginAt ->
            entries += AdminAuditEntry(
                id = "admin-login::${admin.id}",
                timestamp = loginAt,
                actor = adminName,
                role = admin.role,
                action = "Connexion",
                entity = "Session",
                entityId = admin.id,
                details = "Dernière connexion connue du compte ${admin.email}.",
                status = "success",
                sourceType = "access"
            )
        }
        if (!admin.isActive || !admin.status.equals("active", true)) {
            entries += AdminAuditEntry(
                id = "admin-status::${admin.id}",
                timestamp = admin.updatedAt.takeIf { it > 0 } ?: admin.createdAt,
                actor = "Super-admin",
                role = "SUPER_ADMIN",
                action = "Compte administrateur restreint",
                entity = "Administrateur",
                entityId = admin.id,
                details = "Le compte ${adminName} est actuellement ${admin.status.ifBlank { "inactif" }}.",
                status = "warning",
                sourceType = "admin"
            )
        }
        if (admin.mustChangePassword) {
            entries += AdminAuditEntry(
                id = "admin-password::${admin.id}",
                timestamp = admin.updatedAt.takeIf { it > 0 } ?: admin.createdAt,
                actor = adminName,
                role = admin.role,
                action = "Mot de passe à renouveler",
                entity = "Sécurité",
                entityId = admin.id,
                details = "Le compte ${admin.email} doit encore changer son mot de passe.",
                status = "info",
                sourceType = "security"
            )
        }
        if (admin.loginAttempts >= 5) {
            entries += AdminAuditEntry(
                id = "admin-attempts::${admin.id}",
                timestamp = admin.updatedAt.takeIf { it > 0 } ?: admin.createdAt,
                actor = adminName,
                role = admin.role,
                action = "Tentatives de connexion élevées",
                entity = "Sécurité",
                entityId = admin.id,
                details = "${admin.loginAttempts} tentatives de connexion relevées pour ${admin.email}.",
                status = "warning",
                sourceType = "security"
            )
        }
    }

    subscriptions.forEach { subscription ->
        val groupLabel = groupsById[subscription.groupeId]?.nom ?: subscription.groupeId
        entries += AdminAuditEntry(
            id = "subscription-created::${subscription.id}",
            timestamp = subscription.createdAt,
            actor = "Super-admin",
            role = "SUPER_ADMIN",
            action = "Ouverture abonnement",
            entity = "Abonnement",
            entityId = subscription.id,
            details = "Abonnement ${subscription.planId} ouvert pour le groupe $groupLabel.",
            status = "success",
            sourceType = "subscription"
        )
        if (subscription.statut.equals("suspended", true)) {
            entries += AdminAuditEntry(
                id = "subscription-suspended::${subscription.id}",
                timestamp = subscription.createdAt,
                actor = "Super-admin",
                role = "SUPER_ADMIN",
                action = "Suspension abonnement",
                entity = "Abonnement",
                entityId = subscription.id,
                details = "L'abonnement du groupe $groupLabel est suspendu.",
                status = "warning",
                sourceType = "subscription"
            )
        }
        if (subscription.dateFin in 1 until currentTime && !subscription.statut.equals("cancelled", true)) {
            entries += AdminAuditEntry(
                id = "subscription-expired::${subscription.id}",
                timestamp = subscription.dateFin,
                actor = "Système",
                role = "SYSTEM",
                action = "Expiration abonnement",
                entity = "Abonnement",
                entityId = subscription.id,
                details = "L'abonnement du groupe $groupLabel a expiré.",
                status = "warning",
                sourceType = "subscription"
            )
        }
    }

    invoices.forEach { invoice ->
        val groupLabel = groupsById[invoice.groupeId]?.nom ?: invoice.groupeId
        val reference = invoice.reference.ifBlank { invoice.id }
        entries += AdminAuditEntry(
            id = "invoice-created::${invoice.id}",
            timestamp = invoice.dateEmission,
            actor = "Super-admin",
            role = "SUPER_ADMIN",
            action = "Émission facture",
            entity = "Facture",
            entityId = invoice.id,
            details = "Facture $reference émise pour $groupLabel (${formatMoneyXaf(invoice.montantXAF)}).",
            status = "success",
            sourceType = "invoice"
        )
        invoice.datePaiement?.takeIf { it > 0 }?.let { paidAt ->
            entries += AdminAuditEntry(
                id = "invoice-paid::${invoice.id}",
                timestamp = paidAt,
                actor = "Super-admin",
                role = "SUPER_ADMIN",
                action = "Paiement facture",
                entity = "Facture",
                entityId = invoice.id,
                details = "Facture $reference marquée payée pour $groupLabel.",
                status = "success",
                sourceType = "invoice"
            )
        }
        val normalizedStatus = invoice.statut.lowercase()
        if (normalizedStatus == "overdue" || (normalizedStatus != "paid" && normalizedStatus != "cancelled" && invoice.dateEcheance in 1 until currentTime)) {
            entries += AdminAuditEntry(
                id = "invoice-overdue::${invoice.id}",
                timestamp = invoice.dateEcheance.takeIf { it > 0 } ?: invoice.dateEmission,
                actor = "Système",
                role = "SYSTEM",
                action = "Facture en retard",
                entity = "Facture",
                entityId = invoice.id,
                details = "Facture $reference échue pour $groupLabel.",
                status = "warning",
                sourceType = "invoice"
            )
        }
        if (normalizedStatus == "cancelled") {
            entries += AdminAuditEntry(
                id = "invoice-cancelled::${invoice.id}",
                timestamp = invoice.dateEmission,
                actor = "Super-admin",
                role = "SUPER_ADMIN",
                action = "Annulation facture",
                entity = "Facture",
                entityId = invoice.id,
                details = "Facture $reference annulée pour $groupLabel.",
                status = "error",
                sourceType = "invoice"
            )
        }
    }

    announcements.forEach { announcement ->
        entries += AdminAuditEntry(
            id = "announcement::${announcement.id}",
            timestamp = announcement.createdAt,
            actor = auditActorLabel(announcement.createdBy, adminsById),
            role = auditActorRole(announcement.createdBy, adminsById),
            action = "Annonce officielle",
            entity = "Annonce",
            entityId = announcement.id,
            details = "Annonce ${announcement.titre.ifBlank { announcement.id }} publiée pour ${announcementTargetLabel(announcement.cible)}.",
            status = "success",
            sourceType = "announcement"
        )
    }

    messages.forEach { message ->
        entries += AdminAuditEntry(
            id = "message::${message.id}",
            timestamp = message.createdAt,
            actor = auditActorLabel(message.createdBy, adminsById),
            role = auditActorRole(message.createdBy, adminsById),
            action = "Communication envoyée",
            entity = "Messagerie",
            entityId = message.id,
            details = "Message ${message.sujet.ifBlank { message.id }} envoyé vers ${messageTargetLabel(message, groupsById, adminsById)} (${message.status}).",
            status = if (message.status == "deleted") "error" else if (message.status == "archived") "warning" else "success",
            sourceType = "message"
        )
    }

    return entries
        .filter { it.timestamp > 0 }
        .sortedByDescending { it.timestamp }
}

internal fun auditStatusLabel(status: String): String = when (status.lowercase()) {
    "success" -> "OK"
    "warning" -> "Alerte"
    "error" -> "Erreur"
    else -> "Info"
}

internal fun auditStatusColor(status: String): Color = when (status.lowercase()) {
    "success" -> Color(0xFF059669)
    "warning" -> Color(0xFFD97706)
    "error" -> Color(0xFFDC2626)
    else -> Color(0xFF2563EB)
}

internal fun auditSourceLabel(sourceType: String): String = when (sourceType.lowercase()) {
    "invoice" -> "Facturation"
    "subscription" -> "Abonnements"
    "admin" -> "Administrateurs"
    "security" -> "Sécurité"
    "group" -> "Groupes"
    "announcement" -> "Annonces"
    "message" -> "Messagerie"
    "access" -> "Accès"
    else -> "Plateforme"
}

private fun auditActorLabel(createdBy: String, adminsById: Map<String, AdminUserDto>): String {
    if (createdBy.isBlank()) return "Système"
    if (createdBy == "user::super-admin") return "Super-admin"
    return adminsById[createdBy]?.let(::adminDisplayName) ?: createdBy
}

private fun auditActorRole(createdBy: String, adminsById: Map<String, AdminUserDto>): String = when {
    createdBy.isBlank() -> "SYSTEM"
    createdBy == "user::super-admin" -> "SUPER_ADMIN"
    else -> adminsById[createdBy]?.role ?: "SYSTEM"
}
