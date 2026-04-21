package cg.epilote.desktop.ui.screens

import cg.epilote.desktop.data.AnnouncementApiDto
import cg.epilote.desktop.data.InvoiceApiDto
import cg.epilote.desktop.data.SubscriptionApiDto

internal const val NOTIFICATION_DAY_MS = 86_400_000L

internal data class AdminNotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val severity: String,
    val sourceType: String,
    val targetLabel: String,
    val createdAt: Long,
    val relatedReference: String = "",
    val relatedStatus: String = "",
    val linkedInvoice: InvoiceDto? = null
)

internal fun buildBroadcastNotifications(announcements: List<AnnouncementApiDto>): List<AdminNotificationItem> =
    announcements.map { announcement ->
        AdminNotificationItem(
            id = "announcement::${announcement.id}",
            title = announcement.titre.ifBlank { "Diffusion globale" },
            message = announcement.contenu,
            severity = "info",
            sourceType = "announcement",
            targetLabel = announcementTargetLabel(announcement.cible),
            createdAt = announcement.createdAt,
            relatedReference = announcement.createdBy,
            relatedStatus = announcement.cible
        )
    }.sortedByDescending { it.createdAt }

internal fun buildOperationalNotifications(
    groupes: List<GroupeDto>,
    adminGroupesByGroup: Map<String, List<UserDto>>,
    admins: List<AdminUserDto>,
    subscriptions: List<SubscriptionApiDto>,
    invoices: List<InvoiceApiDto>
): List<AdminNotificationItem> {
    val currentTime = System.currentTimeMillis()
    val items = mutableListOf<AdminNotificationItem>()
    val groupsById = groupes.associateBy { it.id }
    val subscriptionsById = subscriptions.associateBy { it.id }
    val subscriptionsByGroup = subscriptions.associateBy { it.groupeId }

    invoices.forEach { invoice ->
        val group = groupsById[invoice.groupeId]
        val linkedSubscription = subscriptionsById[invoice.subscriptionId] ?: subscriptionsByGroup[invoice.groupeId]
        val planId = linkedSubscription?.planId ?: group?.planId.orEmpty()
        val linkedInvoice = InvoiceDto(
            id = invoice.id,
            groupeId = invoice.groupeId,
            groupeNom = group?.nom ?: invoice.groupeId,
            groupeSlug = group?.slug.orEmpty(),
            subscriptionId = invoice.subscriptionId,
            planId = planId,
            planNom = planId.ifBlank { "Plan inconnu" },
            montantXAF = invoice.montantXAF,
            statut = invoice.statut,
            dateEmission = invoice.dateEmission,
            dateEcheance = invoice.dateEcheance,
            datePaiement = invoice.datePaiement,
            reference = invoice.reference,
            notes = invoice.notes
        )
        val target = group?.nom ?: invoice.groupeId
        val status = invoice.statut.lowercase()
        if (status == "overdue" || (status != "paid" && status != "cancelled" && invoice.dateEcheance in 1 until currentTime)) {
            items += AdminNotificationItem(
                id = "invoice-overdue::${invoice.id}",
                title = "Facture en retard",
                message = "La facture ${invoice.reference.ifBlank { invoice.id }} du groupe $target est échue et doit être relancée immédiatement.",
                severity = "critical",
                sourceType = "invoice",
                targetLabel = target,
                createdAt = invoice.dateEcheance.takeIf { it > 0 } ?: invoice.dateEmission,
                relatedReference = invoice.reference.ifBlank { invoice.id },
                relatedStatus = invoiceStatusLabel(invoice.statut),
                linkedInvoice = linkedInvoice
            )
        } else if (status == "draft" && currentTime - invoice.dateEmission > (2L * NOTIFICATION_DAY_MS)) {
            items += AdminNotificationItem(
                id = "invoice-draft::${invoice.id}",
                title = "Facture brouillon à finaliser",
                message = "La facture ${invoice.reference.ifBlank { invoice.id }} du groupe $target reste en brouillon depuis plus de 48h.",
                severity = "warning",
                sourceType = "invoice",
                targetLabel = target,
                createdAt = invoice.dateEmission,
                relatedReference = invoice.reference.ifBlank { invoice.id },
                relatedStatus = invoiceStatusLabel(invoice.statut),
                linkedInvoice = linkedInvoice
            )
        } else if (status != "paid" && status != "cancelled" && invoice.dateEcheance in currentTime..(currentTime + 7L * NOTIFICATION_DAY_MS)) {
            items += AdminNotificationItem(
                id = "invoice-due-soon::${invoice.id}",
                title = "Échéance facture proche",
                message = "La facture ${invoice.reference.ifBlank { invoice.id }} du groupe $target arrive à échéance dans les 7 prochains jours.",
                severity = "warning",
                sourceType = "invoice",
                targetLabel = target,
                createdAt = invoice.dateEcheance,
                relatedReference = invoice.reference.ifBlank { invoice.id },
                relatedStatus = invoiceStatusLabel(invoice.statut),
                linkedInvoice = linkedInvoice
            )
        }
    }

    subscriptions.forEach { subscription ->
        val group = groupsById[subscription.groupeId]
        val target = group?.nom ?: subscription.groupeId
        when {
            subscription.dateFin in 1 until currentTime && subscription.statut != "cancelled" -> items += AdminNotificationItem(
                id = "subscription-expired::${subscription.id}",
                title = "Abonnement expiré",
                message = "L'abonnement du groupe $target est expiré et doit être renouvelé ou régularisé.",
                severity = "critical",
                sourceType = "subscription",
                targetLabel = target,
                createdAt = subscription.dateFin,
                relatedReference = subscription.id,
                relatedStatus = subscription.statut
            )
            subscription.statut == "suspended" -> items += AdminNotificationItem(
                id = "subscription-suspended::${subscription.id}",
                title = "Abonnement suspendu",
                message = "Le groupe $target a un abonnement suspendu, avec impact potentiel sur l'accès plateforme.",
                severity = "warning",
                sourceType = "subscription",
                targetLabel = target,
                createdAt = subscription.createdAt,
                relatedReference = subscription.id,
                relatedStatus = subscription.statut
            )
            subscription.statut == "active" && subscription.dateFin in currentTime..(currentTime + 14L * NOTIFICATION_DAY_MS) -> items += AdminNotificationItem(
                id = "subscription-expiring::${subscription.id}",
                title = "Abonnement à échéance proche",
                message = "L'abonnement du groupe $target arrive à terme dans les 14 prochains jours.",
                severity = "warning",
                sourceType = "subscription",
                targetLabel = target,
                createdAt = subscription.dateFin,
                relatedReference = subscription.id,
                relatedStatus = subscription.statut
            )
        }
    }

    groupes.forEach { group ->
        if (!group.isActive) {
            items += AdminNotificationItem(
                id = "group-inactive::${group.id}",
                title = "Groupe scolaire inactif",
                message = "Le groupe ${group.nom} est désactivé sur la plateforme et doit être revu.",
                severity = "warning",
                sourceType = "group",
                targetLabel = group.nom,
                createdAt = group.createdAt,
                relatedReference = group.slug.ifBlank { group.id },
                relatedStatus = "inactive"
            )
        }
        if (adminGroupesByGroup[group.id].isNullOrEmpty()) {
            items += AdminNotificationItem(
                id = "group-no-admin::${group.id}",
                title = "Groupe sans administrateur",
                message = "Le groupe ${group.nom} ne possède actuellement aucun administrateur de groupe actif.",
                severity = "critical",
                sourceType = "group",
                targetLabel = group.nom,
                createdAt = group.createdAt,
                relatedReference = group.slug.ifBlank { group.id },
                relatedStatus = "sans admin"
            )
        }
    }

    admins.forEach { admin ->
        val adminLabel = listOf(admin.firstName, admin.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { admin.username }
        val target = admin.groupId?.let { groupsById[it]?.nom ?: it } ?: "Plateforme"
        if (!admin.isActive || admin.status.lowercase() != "active") {
            items += AdminNotificationItem(
                id = "admin-inactive::${admin.id}",
                title = "Administrateur inactif",
                message = "Le compte $adminLabel est inactif ou suspendu et nécessite un contrôle d'accès.",
                severity = "warning",
                sourceType = "admin",
                targetLabel = target,
                createdAt = admin.updatedAt.takeIf { it > 0 } ?: admin.createdAt,
                relatedReference = admin.email,
                relatedStatus = admin.status
            )
        }
        if (admin.mustChangePassword) {
            items += AdminNotificationItem(
                id = "admin-password::${admin.id}",
                title = "Changement de mot de passe requis",
                message = "Le compte $adminLabel doit encore renouveler son mot de passe avant usage nominal.",
                severity = "info",
                sourceType = "admin",
                targetLabel = target,
                createdAt = admin.updatedAt.takeIf { it > 0 } ?: admin.createdAt,
                relatedReference = admin.email,
                relatedStatus = "mot de passe à changer"
            )
        }
        if (admin.loginAttempts >= 5) {
            items += AdminNotificationItem(
                id = "admin-security::${admin.id}",
                title = "Tentatives de connexion élevées",
                message = "Le compte $adminLabel a enregistré ${admin.loginAttempts} tentatives de connexion. Une vérification sécurité est recommandée.",
                severity = "warning",
                sourceType = "admin",
                targetLabel = target,
                createdAt = admin.updatedAt.takeIf { it > 0 } ?: admin.createdAt,
                relatedReference = admin.email,
                relatedStatus = "${admin.loginAttempts} tentatives"
            )
        }
    }

    return items.sortedByDescending { it.createdAt }
}

internal fun announcementTargetLabel(target: String): String = when (target.lowercase()) {
    "groupes" -> "Tous les groupes scolaires"
    "billing" -> "Facturation / abonnements"
    "admins" -> "Administrateurs plateforme"
    else -> "Toute la plateforme"
}
