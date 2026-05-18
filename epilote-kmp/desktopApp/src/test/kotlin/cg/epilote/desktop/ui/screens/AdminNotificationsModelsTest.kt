package cg.epilote.desktop.ui.screens

import cg.epilote.desktop.data.AnnouncementApiDto
import cg.epilote.desktop.data.InvoiceApiDto
import cg.epilote.desktop.data.SubscriptionApiDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests unitaires pour la page Notifications — fonctions pures du modèle.
 *
 * Couvre :
 *   1. buildOperationalNotifications — factures overdue / draft / due-soon / paid
 *   2. buildOperationalNotifications — abonnements pending / expired / suspended / expiring
 *   3. buildOperationalNotifications — groupes inactifs / sans admin
 *   4. buildOperationalNotifications — admins inactifs / changement MDP / tentatives connexion
 *   5. buildOperationalNotifications — tri par createdAt décroissant
 *   6. buildBroadcastNotifications — mapping AnnouncementApiDto → AdminNotificationItem
 *   7. announcementTargetLabel — tous les cas
 *   8. notificationSeverityLabel — tous les cas
 *   9. notificationSourceLabel — tous les cas
 *  10. NOTIFICATION_DAY_MS — valeur correcte
 */
class AdminNotificationsModelsTest {

    // ── Temps de référence ────────────────────────────────────────────────────

    private val now = System.currentTimeMillis()
    private val oneDay = NOTIFICATION_DAY_MS
    private val twoDays = 2 * oneDay
    private val sevenDays = 7 * oneDay
    private val fourteenDays = 14 * oneDay

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private val groupA = GroupeDto(id = "groupe::a", nom = "Groupe A", isActive = true, createdAt = 0L)
    private val groupB = GroupeDto(id = "groupe::b", nom = "Groupe B", isActive = true, createdAt = 0L)
    private val inactiveGroup = GroupeDto(id = "groupe::inactive", nom = "Inactif", isActive = false, createdAt = 0L)

    private val activeAdmin = AdminUserDto(
        id = "user::admin1",
        username = "admin1",
        firstName = "Alice",
        lastName = "Kouba",
        email = "alice@test.cg",
        status = "active",
        isActive = true,
        mustChangePassword = false,
        loginAttempts = 0,
        createdAt = 0L
    )

    private val inactiveAdmin = AdminUserDto(
        id = "user::admin2",
        username = "admin2",
        firstName = "Bob",
        lastName = "Loko",
        email = "bob@test.cg",
        status = "suspended",
        isActive = false,
        mustChangePassword = false,
        loginAttempts = 0,
        createdAt = 0L
    )

    private val mustChangePasswordAdmin = AdminUserDto(
        id = "user::admin3",
        username = "admin3",
        firstName = "Cédric",
        lastName = "Mvo",
        email = "cedric@test.cg",
        status = "active",
        isActive = true,
        mustChangePassword = true,
        loginAttempts = 0,
        createdAt = 0L
    )

    private val highAttemptsAdmin = AdminUserDto(
        id = "user::admin4",
        username = "admin4",
        firstName = "Diane",
        lastName = "Oni",
        email = "diane@test.cg",
        status = "active",
        isActive = true,
        mustChangePassword = false,
        loginAttempts = 5,
        createdAt = 0L
    )

    private val adminOfGroupA = UserDto(id = "user::user1", groupId = "groupe::a")

    private fun invoice(
        id: String,
        groupeId: String,
        statut: String,
        dateEcheance: Long = 0L,
        dateEmission: Long = now - 3 * oneDay
    ) = InvoiceApiDto(
        id = id,
        groupeId = groupeId,
        subscriptionId = "",
        montantXAF = 50000L,
        statut = statut,
        dateEmission = dateEmission,
        dateEcheance = dateEcheance,
        reference = "REF-$id"
    )

    private fun subscription(
        id: String,
        groupeId: String,
        statut: String,
        dateFin: Long = 0L,
        createdAt: Long = 0L
    ) = SubscriptionApiDto(
        id = id,
        groupeId = groupeId,
        planId = "plan::basic",
        statut = statut,
        dateDebut = 0L,
        dateFin = dateFin,
        createdAt = createdAt
    )

    // ── Test 1 : Factures ─────────────────────────────────────────────────────

    @Test
    fun `overdue invoice generates critical notification`() {
        val inv = invoice("inv1", "groupe::a", statut = "overdue", dateEcheance = now - oneDay)
        val items = buildOperationalNotifications(
            groupes = listOf(groupA),
            adminGroupesByGroup = mapOf("groupe::a" to listOf(adminOfGroupA)),
            admins = emptyList(),
            subscriptions = emptyList(),
            invoices = listOf(inv)
        )
        val overdueItem = items.firstOrNull { it.id == "invoice-overdue::inv1" }
        assertNotNull(overdueItem, "Facture overdue doit générer une notification")
        assertEquals("critical", overdueItem!!.severity)
        assertEquals("invoice", overdueItem.sourceType)
        assertEquals("Groupe A", overdueItem.targetLabel)
    }

    @Test
    fun `implicitly overdue invoice (past due date, not paid) generates critical notification`() {
        val inv = invoice("inv2", "groupe::a", statut = "sent", dateEcheance = now - oneDay)
        val items = buildOperationalNotifications(
            groupes = listOf(groupA),
            adminGroupesByGroup = mapOf("groupe::a" to listOf(adminOfGroupA)),
            admins = emptyList(),
            subscriptions = emptyList(),
            invoices = listOf(inv)
        )
        assertTrue(items.any { it.id == "invoice-overdue::inv2" }, "Facture envoyée avec échéance dépassée doit être en retard")
    }

    @Test
    fun `paid invoice does not generate overdue notification`() {
        val inv = invoice("inv3", "groupe::a", statut = "paid", dateEcheance = now - oneDay)
        val items = buildOperationalNotifications(
            groupes = listOf(groupA),
            adminGroupesByGroup = mapOf("groupe::a" to listOf(adminOfGroupA)),
            admins = emptyList(),
            subscriptions = emptyList(),
            invoices = listOf(inv)
        )
        assertFalse(items.any { it.id.startsWith("invoice-overdue::inv3") }, "Facture payée ne doit PAS générer de notification overdue")
    }

    @Test
    fun `cancelled invoice does not generate overdue notification`() {
        val inv = invoice("inv4", "groupe::a", statut = "cancelled", dateEcheance = now - oneDay)
        val items = buildOperationalNotifications(
            groupes = listOf(groupA),
            adminGroupesByGroup = mapOf("groupe::a" to listOf(adminOfGroupA)),
            admins = emptyList(),
            subscriptions = emptyList(),
            invoices = listOf(inv)
        )
        assertFalse(items.any { it.id.startsWith("invoice-overdue") }, "Facture annulée ne doit PAS générer de notification overdue")
    }

    @Test
    fun `draft invoice older than 48h generates warning notification`() {
        val inv = invoice("inv5", "groupe::a", statut = "draft", dateEmission = now - twoDays - 1000L)
        val items = buildOperationalNotifications(
            groupes = listOf(groupA),
            adminGroupesByGroup = mapOf("groupe::a" to listOf(adminOfGroupA)),
            admins = emptyList(),
            subscriptions = emptyList(),
            invoices = listOf(inv)
        )
        val draftItem = items.firstOrNull { it.id == "invoice-draft::inv5" }
        assertNotNull(draftItem, "Facture brouillon >48h doit générer une notification warning")
        assertEquals("warning", draftItem!!.severity)
    }

    @Test
    fun `draft invoice younger than 48h does not generate warning notification`() {
        val inv = invoice("inv6", "groupe::a", statut = "draft", dateEmission = now - oneDay)
        val items = buildOperationalNotifications(
            groupes = listOf(groupA),
            adminGroupesByGroup = mapOf("groupe::a" to listOf(adminOfGroupA)),
            admins = emptyList(),
            subscriptions = emptyList(),
            invoices = listOf(inv)
        )
        assertFalse(items.any { it.id == "invoice-draft::inv6" }, "Facture brouillon <48h ne doit PAS générer de notification")
    }

    @Test
    fun `invoice due within 7 days generates warning notification`() {
        val inv = invoice("inv7", "groupe::a", statut = "sent", dateEcheance = now + 3 * oneDay)
        val items = buildOperationalNotifications(
            groupes = listOf(groupA),
            adminGroupesByGroup = mapOf("groupe::a" to listOf(adminOfGroupA)),
            admins = emptyList(),
            subscriptions = emptyList(),
            invoices = listOf(inv)
        )
        val dueSoon = items.firstOrNull { it.id == "invoice-due-soon::inv7" }
        assertNotNull(dueSoon, "Facture échéant dans 3 jours doit générer une notification")
        assertEquals("warning", dueSoon!!.severity)
    }

    @Test
    fun `invoice due in more than 7 days does not generate due-soon notification`() {
        val inv = invoice("inv8", "groupe::a", statut = "sent", dateEcheance = now + 10 * oneDay)
        val items = buildOperationalNotifications(
            groupes = listOf(groupA),
            adminGroupesByGroup = mapOf("groupe::a" to listOf(adminOfGroupA)),
            admins = emptyList(),
            subscriptions = emptyList(),
            invoices = listOf(inv)
        )
        assertFalse(items.any { it.id == "invoice-due-soon::inv8" }, "Facture >7 jours ne doit PAS générer de notification imminente")
    }

    @Test
    fun `invoice notification includes linkedInvoice`() {
        val inv = invoice("inv_linked", "groupe::a", statut = "overdue", dateEcheance = now - oneDay)
        val items = buildOperationalNotifications(
            groupes = listOf(groupA),
            adminGroupesByGroup = mapOf("groupe::a" to listOf(adminOfGroupA)),
            admins = emptyList(),
            subscriptions = emptyList(),
            invoices = listOf(inv)
        )
        val item = items.firstOrNull { it.id == "invoice-overdue::inv_linked" }
        assertNotNull(item?.linkedInvoice, "Notification facture doit avoir linkedInvoice renseigné")
        assertEquals("inv_linked", item!!.linkedInvoice!!.id)
        assertEquals("Groupe A", item.linkedInvoice!!.groupeNom)
    }

    // ── Test 2 : Abonnements ──────────────────────────────────────────────────

    @Test
    fun `pending subscription generates critical notification`() {
        val sub = subscription("sub1", "groupe::a", statut = "pending")
        val items = buildOperationalNotifications(
            groupes = listOf(groupA),
            adminGroupesByGroup = mapOf("groupe::a" to listOf(adminOfGroupA)),
            admins = emptyList(),
            subscriptions = listOf(sub),
            invoices = emptyList()
        )
        val item = items.firstOrNull { it.id == "subscription-pending::sub1" }
        assertNotNull(item, "Abonnement pending doit générer une notification critique")
        assertEquals("critical", item!!.severity)
        assertEquals("subscription", item.sourceType)
    }

    @Test
    fun `expired subscription generates critical notification`() {
        val sub = subscription("sub2", "groupe::a", statut = "active", dateFin = now - oneDay)
        val items = buildOperationalNotifications(
            groupes = listOf(groupA),
            adminGroupesByGroup = mapOf("groupe::a" to listOf(adminOfGroupA)),
            admins = emptyList(),
            subscriptions = listOf(sub),
            invoices = emptyList()
        )
        val item = items.firstOrNull { it.id == "subscription-expired::sub2" }
        assertNotNull(item, "Abonnement expiré doit générer une notification critique")
        assertEquals("critical", item!!.severity)
    }

    @Test
    fun `cancelled subscription does not generate expired notification`() {
        val sub = subscription("sub3", "groupe::a", statut = "cancelled", dateFin = now - oneDay)
        val items = buildOperationalNotifications(
            groupes = listOf(groupA),
            adminGroupesByGroup = mapOf("groupe::a" to listOf(adminOfGroupA)),
            admins = emptyList(),
            subscriptions = listOf(sub),
            invoices = emptyList()
        )
        assertFalse(items.any { it.id == "subscription-expired::sub3" }, "Abonnement cancelled ne doit PAS générer de notification expirée")
    }

    @Test
    fun `suspended subscription generates warning notification`() {
        val sub = subscription("sub4", "groupe::a", statut = "suspended")
        val items = buildOperationalNotifications(
            groupes = listOf(groupA),
            adminGroupesByGroup = mapOf("groupe::a" to listOf(adminOfGroupA)),
            admins = emptyList(),
            subscriptions = listOf(sub),
            invoices = emptyList()
        )
        val item = items.firstOrNull { it.id == "subscription-suspended::sub4" }
        assertNotNull(item, "Abonnement suspendu doit générer une notification warning")
        assertEquals("warning", item!!.severity)
    }

    @Test
    fun `active subscription expiring within 14 days generates warning`() {
        val sub = subscription("sub5", "groupe::a", statut = "active", dateFin = now + 7 * oneDay)
        val items = buildOperationalNotifications(
            groupes = listOf(groupA),
            adminGroupesByGroup = mapOf("groupe::a" to listOf(adminOfGroupA)),
            admins = emptyList(),
            subscriptions = listOf(sub),
            invoices = emptyList()
        )
        val item = items.firstOrNull { it.id == "subscription-expiring::sub5" }
        assertNotNull(item, "Abonnement expirant dans 7 jours doit générer une notification")
        assertEquals("warning", item!!.severity)
    }

    @Test
    fun `active subscription expiring in more than 14 days generates no warning`() {
        val sub = subscription("sub6", "groupe::a", statut = "active", dateFin = now + 30 * oneDay)
        val items = buildOperationalNotifications(
            groupes = listOf(groupA),
            adminGroupesByGroup = mapOf("groupe::a" to listOf(adminOfGroupA)),
            admins = emptyList(),
            subscriptions = listOf(sub),
            invoices = emptyList()
        )
        assertFalse(items.any { it.id == "subscription-expiring::sub6" }, "Abonnement >14 jours ne doit PAS générer de notification imminente")
    }

    // ── Test 3 : Groupes ──────────────────────────────────────────────────────

    @Test
    fun `inactive group generates warning notification`() {
        val items = buildOperationalNotifications(
            groupes = listOf(inactiveGroup),
            adminGroupesByGroup = mapOf("groupe::inactive" to listOf(adminOfGroupA)),
            admins = emptyList(),
            subscriptions = emptyList(),
            invoices = emptyList()
        )
        val item = items.firstOrNull { it.id == "group-inactive::groupe::inactive" }
        assertNotNull(item, "Groupe inactif doit générer une notification warning")
        assertEquals("warning", item!!.severity)
        assertEquals("group", item.sourceType)
        assertEquals("Inactif", item.targetLabel)
    }

    @Test
    fun `active group does not generate inactive notification`() {
        val items = buildOperationalNotifications(
            groupes = listOf(groupA),
            adminGroupesByGroup = mapOf("groupe::a" to listOf(adminOfGroupA)),
            admins = emptyList(),
            subscriptions = emptyList(),
            invoices = emptyList()
        )
        assertFalse(items.any { it.id == "group-inactive::groupe::a" }, "Groupe actif ne doit PAS générer de notification inactive")
    }

    @Test
    fun `group without admin generates critical notification`() {
        val items = buildOperationalNotifications(
            groupes = listOf(groupA),
            adminGroupesByGroup = emptyMap(),
            admins = emptyList(),
            subscriptions = emptyList(),
            invoices = emptyList()
        )
        val item = items.firstOrNull { it.id == "group-no-admin::groupe::a" }
        assertNotNull(item, "Groupe sans administrateur doit générer une notification critique")
        assertEquals("critical", item!!.severity)
    }

    @Test
    fun `group with admin does not generate no-admin notification`() {
        val items = buildOperationalNotifications(
            groupes = listOf(groupA),
            adminGroupesByGroup = mapOf("groupe::a" to listOf(adminOfGroupA)),
            admins = emptyList(),
            subscriptions = emptyList(),
            invoices = emptyList()
        )
        assertFalse(items.any { it.id == "group-no-admin::groupe::a" }, "Groupe avec admin ne doit PAS générer de notification sans-admin")
    }

    @Test
    fun `group with empty admin list generates no-admin notification`() {
        val items = buildOperationalNotifications(
            groupes = listOf(groupA),
            adminGroupesByGroup = mapOf("groupe::a" to emptyList()),
            admins = emptyList(),
            subscriptions = emptyList(),
            invoices = emptyList()
        )
        assertNotNull(items.firstOrNull { it.id == "group-no-admin::groupe::a" }, "Liste admin vide = sans admin = notification critique")
    }

    // ── Test 4 : Administrateurs ──────────────────────────────────────────────

    @Test
    fun `inactive admin generates warning notification`() {
        val items = buildOperationalNotifications(
            groupes = emptyList(),
            adminGroupesByGroup = emptyMap(),
            admins = listOf(inactiveAdmin),
            subscriptions = emptyList(),
            invoices = emptyList()
        )
        val item = items.firstOrNull { it.id == "admin-inactive::user::admin2" }
        assertNotNull(item, "Admin inactif doit générer une notification warning")
        assertEquals("warning", item!!.severity)
        assertEquals("admin", item.sourceType)
    }

    @Test
    fun `active admin does not generate inactive notification`() {
        val items = buildOperationalNotifications(
            groupes = emptyList(),
            adminGroupesByGroup = emptyMap(),
            admins = listOf(activeAdmin),
            subscriptions = emptyList(),
            invoices = emptyList()
        )
        assertFalse(items.any { it.id == "admin-inactive::user::admin1" }, "Admin actif ne doit PAS générer de notification inactive")
    }

    @Test
    fun `admin with mustChangePassword generates info notification`() {
        val items = buildOperationalNotifications(
            groupes = emptyList(),
            adminGroupesByGroup = emptyMap(),
            admins = listOf(mustChangePasswordAdmin),
            subscriptions = emptyList(),
            invoices = emptyList()
        )
        val item = items.firstOrNull { it.id == "admin-password::user::admin3" }
        assertNotNull(item, "Admin mustChangePassword doit générer une notification info")
        assertEquals("info", item!!.severity)
    }

    @Test
    fun `admin without mustChangePassword does not generate password notification`() {
        val items = buildOperationalNotifications(
            groupes = emptyList(),
            adminGroupesByGroup = emptyMap(),
            admins = listOf(activeAdmin),
            subscriptions = emptyList(),
            invoices = emptyList()
        )
        assertFalse(items.any { it.id == "admin-password::user::admin1" }, "Admin sans obligation MDP ne doit PAS générer de notification")
    }

    @Test
    fun `admin with 5 or more login attempts generates security warning`() {
        val items = buildOperationalNotifications(
            groupes = emptyList(),
            adminGroupesByGroup = emptyMap(),
            admins = listOf(highAttemptsAdmin),
            subscriptions = emptyList(),
            invoices = emptyList()
        )
        val item = items.firstOrNull { it.id == "admin-security::user::admin4" }
        assertNotNull(item, "Admin avec ≥5 tentatives doit générer une notification sécurité")
        assertEquals("warning", item!!.severity)
        assertTrue(item.message.contains("5"), "Le message doit mentionner le nombre de tentatives")
    }

    @Test
    fun `admin with 4 login attempts does not generate security warning`() {
        val safeAdmin = activeAdmin.copy(id = "user::admin5", loginAttempts = 4)
        val items = buildOperationalNotifications(
            groupes = emptyList(),
            adminGroupesByGroup = emptyMap(),
            admins = listOf(safeAdmin),
            subscriptions = emptyList(),
            invoices = emptyList()
        )
        assertFalse(items.any { it.id.startsWith("admin-security::user::admin5") }, "4 tentatives ne doit PAS générer de notification sécurité")
    }

    // ── Test 5 : Tri décroissant ──────────────────────────────────────────────

    @Test
    fun `buildOperationalNotifications sorts items by createdAt descending`() {
        val sub_old = subscription("sub_old", "groupe::a", statut = "pending", createdAt = 100L)
        val sub_new = subscription("sub_new", "groupe::b", statut = "pending", createdAt = 9999L)
        val items = buildOperationalNotifications(
            groupes = listOf(groupA, groupB),
            adminGroupesByGroup = mapOf(
                "groupe::a" to listOf(adminOfGroupA),
                "groupe::b" to listOf(adminOfGroupA)
            ),
            admins = emptyList(),
            subscriptions = listOf(sub_old, sub_new),
            invoices = emptyList()
        )
        val pendingItems = items.filter { it.id.startsWith("subscription-pending") }
        assertEquals(2, pendingItems.size)
        assertTrue(pendingItems[0].createdAt >= pendingItems[1].createdAt, "Les items doivent être triés par createdAt décroissant")
    }

    @Test
    fun `empty inputs return empty notifications`() {
        val items = buildOperationalNotifications(
            groupes = emptyList(),
            adminGroupesByGroup = emptyMap(),
            admins = emptyList(),
            subscriptions = emptyList(),
            invoices = emptyList()
        )
        assertTrue(items.isEmpty())
    }

    // ── Test 6 : buildBroadcastNotifications ──────────────────────────────────

    @Test
    fun `buildBroadcastNotifications maps announcement to notification item`() {
        val announcement = AnnouncementApiDto(
            id = "ann::001",
            titre = "Nouvelle fonctionnalité",
            contenu = "Export PDF disponible.",
            cible = "groupes",
            createdBy = "user::super-admin",
            createdAt = 1000L
        )
        val items = buildBroadcastNotifications(listOf(announcement))
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals("announcement::ann::001", item.id)
        assertEquals("Nouvelle fonctionnalité", item.title)
        assertEquals("Export PDF disponible.", item.message)
        assertEquals("info", item.severity)
        assertEquals("announcement", item.sourceType)
        assertEquals("Tous les groupes scolaires", item.targetLabel)
        assertEquals(1000L, item.createdAt)
    }

    @Test
    fun `buildBroadcastNotifications uses fallback title when blank`() {
        val announcement = AnnouncementApiDto(
            id = "ann::002",
            titre = "",
            contenu = "Contenu",
            cible = "all",
            createdBy = "user::super-admin",
            createdAt = 0L
        )
        val items = buildBroadcastNotifications(listOf(announcement))
        assertEquals("Diffusion globale", items[0].title)
    }

    @Test
    fun `buildBroadcastNotifications sorts by createdAt descending`() {
        val announcements = listOf(
            AnnouncementApiDto(id = "ann::old", titre = "Ancien", contenu = "...", cible = "all", createdBy = "", createdAt = 100L),
            AnnouncementApiDto(id = "ann::new", titre = "Récent", contenu = "...", cible = "all", createdBy = "", createdAt = 9999L)
        )
        val items = buildBroadcastNotifications(announcements)
        assertEquals("announcement::ann::new", items[0].id)
        assertEquals("announcement::ann::old", items[1].id)
    }

    @Test
    fun `buildBroadcastNotifications returns empty list for empty input`() {
        assertTrue(buildBroadcastNotifications(emptyList()).isEmpty())
    }

    // ── Test 7 : announcementTargetLabel ──────────────────────────────────────

    @Test
    fun `announcementTargetLabel groupes`() = assertEquals("Tous les groupes scolaires", announcementTargetLabel("groupes"))

    @Test
    fun `announcementTargetLabel billing`() = assertEquals("Facturation / abonnements", announcementTargetLabel("billing"))

    @Test
    fun `announcementTargetLabel admins`() = assertEquals("Administrateurs plateforme", announcementTargetLabel("admins"))

    @Test
    fun `announcementTargetLabel fallback for unknown`() = assertEquals("Toute la plateforme", announcementTargetLabel(""))

    @Test
    fun `announcementTargetLabel fallback for all`() = assertEquals("Toute la plateforme", announcementTargetLabel("all"))

    @Test
    fun `announcementTargetLabel is case-insensitive`() {
        assertEquals("Tous les groupes scolaires", announcementTargetLabel("GROUPES"))
        assertEquals("Facturation / abonnements", announcementTargetLabel("BILLING"))
    }

    // ── Test 8 : notificationSeverityLabel ────────────────────────────────────

    @Test
    fun `notificationSeverityLabel critical`() = assertEquals("Critique", notificationSeverityLabel("critical"))

    @Test
    fun `notificationSeverityLabel warning`() = assertEquals("Attention", notificationSeverityLabel("warning"))

    @Test
    fun `notificationSeverityLabel success`() = assertEquals("Traité", notificationSeverityLabel("success"))

    @Test
    fun `notificationSeverityLabel info fallback`() = assertEquals("Information", notificationSeverityLabel("info"))

    @Test
    fun `notificationSeverityLabel unknown fallback`() = assertEquals("Information", notificationSeverityLabel("unknown"))

    @Test
    fun `notificationSeverityLabel is case-insensitive`() {
        assertEquals("Critique", notificationSeverityLabel("CRITICAL"))
        assertEquals("Attention", notificationSeverityLabel("WARNING"))
    }

    // ── Test 9 : notificationSourceLabel ──────────────────────────────────────

    @Test
    fun `notificationSourceLabel invoice`() = assertEquals("Facturation", notificationSourceLabel("invoice"))

    @Test
    fun `notificationSourceLabel subscription`() = assertEquals("Abonnement", notificationSourceLabel("subscription"))

    @Test
    fun `notificationSourceLabel group`() = assertEquals("Groupe", notificationSourceLabel("group"))

    @Test
    fun `notificationSourceLabel admin`() = assertEquals("Administrateurs", notificationSourceLabel("admin"))

    @Test
    fun `notificationSourceLabel announcement`() = assertEquals("Diffusion", notificationSourceLabel("announcement"))

    @Test
    fun `notificationSourceLabel unknown fallback`() = assertEquals("Plateforme", notificationSourceLabel("unknown"))

    @Test
    fun `notificationSourceLabel is case-insensitive`() {
        assertEquals("Facturation", notificationSourceLabel("INVOICE"))
        assertEquals("Groupe", notificationSourceLabel("GROUP"))
    }

    // ── Test 10 : NOTIFICATION_DAY_MS ────────────────────────────────────────

    @Test
    fun `NOTIFICATION_DAY_MS equals 86400000 ms`() {
        assertEquals(86_400_000L, NOTIFICATION_DAY_MS)
    }

    // ── Test 11 : Cumul multi-sources ─────────────────────────────────────────

    @Test
    fun `multiple alert sources produce multiple notifications`() {
        val items = buildOperationalNotifications(
            groupes = listOf(groupA, inactiveGroup),
            adminGroupesByGroup = mapOf("groupe::a" to listOf(adminOfGroupA)),
            admins = listOf(inactiveAdmin, mustChangePasswordAdmin),
            subscriptions = listOf(subscription("sub1", "groupe::a", statut = "pending")),
            invoices = listOf(invoice("inv1", "groupe::a", statut = "overdue", dateEcheance = now - oneDay))
        )
        assertTrue(items.size >= 5, "Au moins 5 notifications attendues sur les différentes sources (trouvé: ${items.size})")
        assertTrue(items.any { it.severity == "critical" }, "Au moins une notification critique attendue")
        assertTrue(items.any { it.severity == "warning" }, "Au moins une notification warning attendue")
        assertTrue(items.any { it.severity == "info" }, "Au moins une notification info attendue")
    }

    @Test
    fun `notifications have unique ids across sources`() {
        val items = buildOperationalNotifications(
            groupes = listOf(groupA, groupB),
            adminGroupesByGroup = emptyMap(),
            admins = listOf(inactiveAdmin),
            subscriptions = listOf(subscription("sub1", "groupe::a", statut = "pending")),
            invoices = listOf(invoice("inv1", "groupe::b", statut = "overdue", dateEcheance = now - oneDay))
        )
        val ids = items.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Tous les IDs de notification doivent être uniques")
    }
}
