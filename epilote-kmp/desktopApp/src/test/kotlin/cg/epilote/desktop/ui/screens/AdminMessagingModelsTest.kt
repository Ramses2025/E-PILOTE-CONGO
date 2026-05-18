package cg.epilote.desktop.ui.screens

import cg.epilote.desktop.data.AdminMessageApiDto
import cg.epilote.desktop.data.AnnouncementApiDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests unitaires pour la page Messagerie — fonctions pures du modèle.
 *
 * Couvre :
 *   1. buildMessageThreads — regroupement par threadKey, fallback defaultThreadKey
 *   2. buildMessageThreads — targetLabel par type (group, admin, all_groups, all_admins)
 *   3. buildMessageThreads — tri par lastCreatedAt décroissant
 *   4. matchesMailbox — INBOX (non-owned), SENT (owned), ARCHIVED, TRASH
 *   5. filterThreads — filtrage par mailbox, recherche, targetFilter
 *   6. filterAnnouncements — recherche titre/contenu, cible
 *   7. defaultThreadKey — préfixes par targetType
 *   8. adminDisplayName — formatage du nom
 *   9. announcementTargetLabel — labels corrects par cible
 *  10. Incohérence targetOptions : "groupes"/"admins"/"billing" ≠ targetType message
 */
class AdminMessagingModelsTest {

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private val superAdminId = "user::super-admin"
    private val otherAdminId = "user::other-admin"

    private val groupLamarelle = GroupeDto(id = "groupe::lamarelle", nom = "LAMARELLE")
    private val groupEdjaSembe = GroupeDto(id = "groupe::edja-sembe", nom = "EDJA-SEMBE")

    private val adminUser = AdminUserDto(
        id = "user::admin-jean",
        username = "jdupont",
        firstName = "Jean",
        lastName = "Dupont",
        email = "jean@test.cg"
    )

    private fun msg(
        id: String,
        targetType: String,
        groupId: String? = null,
        adminId: String? = null,
        threadKey: String = "",
        status: String = "sent",
        createdBy: String = superAdminId,
        createdAt: Long = 1000L
    ) = AdminMessageApiDto(
        id = id,
        sujet = "Sujet $id",
        contenu = "Contenu $id",
        targetType = targetType,
        groupId = groupId,
        adminId = adminId,
        threadKey = threadKey,
        status = status,
        readBy = emptyList(),
        createdBy = createdBy,
        createdAt = createdAt
    )

    // ── Test 1 : buildMessageThreads — regroupement par threadKey ──────────────

    @Test
    fun `buildMessageThreads groups messages with same threadKey into one thread`() {
        val messages = listOf(
            msg("m1", "all_groups", threadKey = "all_groups", createdAt = 100L),
            msg("m2", "all_groups", threadKey = "all_groups", createdAt = 200L),
            msg("m3", "all_groups", threadKey = "all_groups", createdAt = 300L)
        )
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        assertEquals(1, threads.size, "3 messages avec même threadKey → 1 seul thread")
        assertEquals(3, threads[0].messages.size)
    }

    @Test
    fun `buildMessageThreads creates separate threads for different threadKeys`() {
        val messages = listOf(
            msg("m1", "group", groupId = "groupe::lamarelle", threadKey = "group::groupe::lamarelle"),
            msg("m2", "group", groupId = "groupe::edja-sembe", threadKey = "group::groupe::edja-sembe"),
            msg("m3", "all_groups", threadKey = "all_groups")
        )
        val threads = buildMessageThreads(messages, listOf(groupLamarelle, groupEdjaSembe), emptyList())
        assertEquals(3, threads.size, "3 threadKeys différents → 3 threads")
    }

    // ── Test 2 : buildMessageThreads — fallback defaultThreadKey ──────────────

    @Test
    fun `buildMessageThreads uses defaultThreadKey when threadKey is blank`() {
        val messages = listOf(
            msg("m1", "group", groupId = "groupe::lamarelle", threadKey = "")
        )
        val threads = buildMessageThreads(messages, listOf(groupLamarelle), emptyList())
        assertEquals(1, threads.size)
        assertEquals("group::groupe::lamarelle", threads[0].threadKey)
    }

    @Test
    fun `buildMessageThreads groups messages with blank threadKey by computed defaultThreadKey`() {
        val messages = listOf(
            msg("m1", "all_groups", threadKey = "", createdAt = 100L),
            msg("m2", "all_groups", threadKey = "", createdAt = 200L)
        )
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        assertEquals(1, threads.size, "Deux messages all_groups sans threadKey → 1 thread par defaultThreadKey")
    }

    // ── Test 3 : buildMessageThreads — targetLabel ────────────────────────────

    @Test
    fun `buildMessageThreads sets targetLabel to group name for group type`() {
        val messages = listOf(
            msg("m1", "group", groupId = "groupe::lamarelle", threadKey = "group::groupe::lamarelle")
        )
        val threads = buildMessageThreads(messages, listOf(groupLamarelle), emptyList())
        assertEquals("LAMARELLE", threads[0].targetLabel)
    }

    @Test
    fun `buildMessageThreads falls back to groupId when group not found`() {
        val messages = listOf(
            msg("m1", "group", groupId = "groupe::unknown", threadKey = "group::groupe::unknown")
        )
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        assertEquals("groupe::unknown", threads[0].targetLabel)
    }

    @Test
    fun `buildMessageThreads shows Groupe inconnu when groupId is null`() {
        val messages = listOf(
            msg("m1", "group", groupId = null, threadKey = "group::null")
        )
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        assertEquals("Groupe inconnu", threads[0].targetLabel)
    }

    @Test
    fun `buildMessageThreads sets targetLabel to admin display name for admin type`() {
        val messages = listOf(
            msg("m1", "admin", adminId = "user::admin-jean", threadKey = "admin::user::admin-jean")
        )
        val threads = buildMessageThreads(messages, emptyList(), listOf(adminUser))
        assertEquals("Jean Dupont", threads[0].targetLabel)
    }

    @Test
    fun `buildMessageThreads shows all_groups label for all_groups type`() {
        val messages = listOf(msg("m1", "all_groups", threadKey = "all_groups"))
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        assertEquals("Tous les groupes scolaires", threads[0].targetLabel)
    }

    @Test
    fun `buildMessageThreads shows all_admins label for all_admins type`() {
        val messages = listOf(msg("m1", "all_admins", threadKey = "all_admins"))
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        assertEquals("Tous les administrateurs", threads[0].targetLabel)
    }

    // ── Test 4 : buildMessageThreads — tri décroissant ────────────────────────

    @Test
    fun `buildMessageThreads sorts threads by lastCreatedAt descending`() {
        val messages = listOf(
            msg("m_old", "all_groups", threadKey = "all_groups", createdAt = 100L),
            msg("m_group", "group", groupId = "groupe::lamarelle", threadKey = "group::groupe::lamarelle", createdAt = 900L),
            msg("m_admins", "all_admins", threadKey = "all_admins", createdAt = 500L)
        )
        val threads = buildMessageThreads(messages, listOf(groupLamarelle), emptyList())
        assertEquals("group::groupe::lamarelle", threads[0].threadKey)
        assertEquals("all_admins", threads[1].threadKey)
        assertEquals("all_groups", threads[2].threadKey)
    }

    @Test
    fun `buildMessageThreads uses most recent message for sort key within thread`() {
        val messages = listOf(
            msg("m1", "all_groups", threadKey = "all_groups", createdAt = 100L),
            msg("m2", "all_groups", threadKey = "all_groups", createdAt = 999L),
            msg("m3", "group", groupId = "groupe::lamarelle", threadKey = "group::groupe::lamarelle", createdAt = 500L)
        )
        val threads = buildMessageThreads(messages, listOf(groupLamarelle), emptyList())
        assertEquals("all_groups", threads[0].threadKey, "Thread avec message le plus récent (999) doit être en premier")
    }

    // ── Test 5 : matchesMailbox ────────────────────────────────────────────────

    @Test
    fun `matchesMailbox SENT returns true when message is owned by current user`() {
        val messages = listOf(msg("m1", "all_groups", threadKey = "all_groups", status = "sent", createdBy = superAdminId))
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        assertTrue(threads[0].matchesMailbox(AdminMessagingMailbox.SENT, superAdminId))
    }

    @Test
    fun `matchesMailbox SENT returns false when message is not owned by current user`() {
        val messages = listOf(msg("m1", "all_groups", threadKey = "all_groups", status = "sent", createdBy = otherAdminId))
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        assertFalse(threads[0].matchesMailbox(AdminMessagingMailbox.SENT, superAdminId))
    }

    @Test
    fun `matchesMailbox INBOX returns true when message is NOT owned by current user`() {
        val messages = listOf(msg("m1", "all_groups", threadKey = "all_groups", status = "sent", createdBy = otherAdminId))
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        assertTrue(threads[0].matchesMailbox(AdminMessagingMailbox.INBOX, superAdminId))
    }

    @Test
    fun `matchesMailbox INBOX returns false when message IS owned by current user`() {
        val messages = listOf(msg("m1", "all_groups", threadKey = "all_groups", status = "sent", createdBy = superAdminId))
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        assertFalse(threads[0].matchesMailbox(AdminMessagingMailbox.INBOX, superAdminId))
    }

    @Test
    fun `matchesMailbox ARCHIVED returns true when status is archived`() {
        val messages = listOf(msg("m1", "all_groups", threadKey = "all_groups", status = "archived"))
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        assertTrue(threads[0].matchesMailbox(AdminMessagingMailbox.ARCHIVED, superAdminId))
    }

    @Test
    fun `matchesMailbox ARCHIVED returns false when status is sent`() {
        val messages = listOf(msg("m1", "all_groups", threadKey = "all_groups", status = "sent"))
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        assertFalse(threads[0].matchesMailbox(AdminMessagingMailbox.ARCHIVED, superAdminId))
    }

    @Test
    fun `matchesMailbox TRASH returns true when status is deleted`() {
        val messages = listOf(msg("m1", "all_groups", threadKey = "all_groups", status = "deleted"))
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        assertTrue(threads[0].matchesMailbox(AdminMessagingMailbox.TRASH, superAdminId))
    }

    @Test
    fun `matchesMailbox TRASH returns false when status is archived`() {
        val messages = listOf(msg("m1", "all_groups", threadKey = "all_groups", status = "archived"))
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        assertFalse(threads[0].matchesMailbox(AdminMessagingMailbox.TRASH, superAdminId))
    }

    @Test
    fun `matchesMailbox ANNOUNCEMENTS always returns false for message threads`() {
        val messages = listOf(msg("m1", "all_groups", threadKey = "all_groups"))
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        assertFalse(threads[0].matchesMailbox(AdminMessagingMailbox.ANNOUNCEMENTS, superAdminId))
    }

    // ── Test 6 : filterThreads — mailbox ──────────────────────────────────────

    @Test
    fun `filterThreads SENT shows only messages owned by current user`() {
        val messages = listOf(
            msg("m_mine", "all_groups", threadKey = "all_groups", createdBy = superAdminId),
            msg("m_other", "all_admins", threadKey = "all_admins", createdBy = otherAdminId)
        )
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        val result = filterThreads(threads, "", "all", AdminMessagingMailbox.SENT, superAdminId)
        assertEquals(1, result.size)
        assertEquals("all_groups", result[0].threadKey)
    }

    @Test
    fun `filterThreads INBOX shows only messages not owned by current user`() {
        val messages = listOf(
            msg("m_mine", "all_groups", threadKey = "all_groups", createdBy = superAdminId),
            msg("m_other", "all_admins", threadKey = "all_admins", createdBy = otherAdminId)
        )
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        val result = filterThreads(threads, "", "all", AdminMessagingMailbox.INBOX, superAdminId)
        assertEquals(1, result.size)
        assertEquals("all_admins", result[0].threadKey)
    }

    @Test
    fun `filterThreads search matches targetLabel case-insensitively`() {
        val messages = listOf(
            msg("m1", "group", groupId = "groupe::lamarelle", threadKey = "group::groupe::lamarelle", createdBy = superAdminId)
        )
        val threads = buildMessageThreads(messages, listOf(groupLamarelle), emptyList())
        val result = filterThreads(threads, "lamar", "all", AdminMessagingMailbox.SENT, superAdminId)
        assertEquals(1, result.size)
    }

    @Test
    fun `filterThreads search matches lastSubject case-insensitively`() {
        val messages = listOf(
            msg("m1", "all_groups", threadKey = "all_groups", createdBy = superAdminId)
        )
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        val result = filterThreads(threads, "SUJET", "all", AdminMessagingMailbox.SENT, superAdminId)
        assertEquals(1, result.size, "La recherche 'SUJET' doit matcher le sujet du message")
    }

    @Test
    fun `filterThreads search non-matching returns empty`() {
        val messages = listOf(msg("m1", "all_groups", threadKey = "all_groups"))
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        val result = filterThreads(threads, "INEXISTANT_XYZ", "all", AdminMessagingMailbox.SENT, superAdminId)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterThreads targetFilter group matches only group threads`() {
        val messages = listOf(
            msg("m_group", "group", groupId = "groupe::lamarelle", threadKey = "group::groupe::lamarelle", createdBy = superAdminId),
            msg("m_all", "all_groups", threadKey = "all_groups", createdBy = superAdminId)
        )
        val threads = buildMessageThreads(messages, listOf(groupLamarelle), emptyList())
        val result = filterThreads(threads, "", "group", AdminMessagingMailbox.SENT, superAdminId)
        assertEquals(1, result.size)
        assertEquals("group", result[0].targetType)
    }

    @Test
    fun `filterThreads targetFilter all shows all mailbox-matching threads`() {
        val messages = listOf(
            msg("m_group", "group", groupId = "groupe::lamarelle", threadKey = "group::groupe::lamarelle", createdBy = superAdminId),
            msg("m_all", "all_groups", threadKey = "all_groups", createdBy = superAdminId)
        )
        val threads = buildMessageThreads(messages, listOf(groupLamarelle), emptyList())
        val result = filterThreads(threads, "", "all", AdminMessagingMailbox.SENT, superAdminId)
        assertEquals(2, result.size)
    }

    @Test
    fun `filterThreads with announcement-specific targetFilter returns empty for message threads`() {
        // "groupes" est une valeur de cible d'annonce, pas un targetType de message
        // → aucun thread ne doit correspondre (incohérence documentée)
        val messages = listOf(
            msg("m1", "all_groups", threadKey = "all_groups", createdBy = superAdminId)
        )
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        val result = filterThreads(threads, "", "groupes", AdminMessagingMailbox.SENT, superAdminId)
        assertTrue(result.isEmpty(), "'groupes' est une cible annonce, pas un targetType message : doit retourner 0 thread")
    }

    // ── Test 7 : filterAnnouncements ──────────────────────────────────────────

    private fun ann(id: String, titre: String, contenu: String = "...", cible: String = "all") =
        AnnouncementApiDto(id = id, titre = titre, contenu = contenu, cible = cible, createdBy = superAdminId, createdAt = 0L)

    @Test
    fun `filterAnnouncements with blank search returns all`() {
        val announcements = listOf(ann("a1", "Maintenance"), ann("a2", "Mise à jour"))
        val result = filterAnnouncements(announcements, "", "all")
        assertEquals(2, result.size)
    }

    @Test
    fun `filterAnnouncements search matches titre case-insensitively`() {
        val announcements = listOf(ann("a1", "Maintenance prévue"), ann("a2", "Nouvelle fonctionnalité"))
        val result = filterAnnouncements(announcements, "maintenance", "all")
        assertEquals(1, result.size)
        assertEquals("a1", result[0].id)
    }

    @Test
    fun `filterAnnouncements search matches contenu case-insensitively`() {
        val announcements = listOf(ann("a1", "Info", contenu = "Votre abonnement expire bientôt"))
        val result = filterAnnouncements(announcements, "abonnement", "all")
        assertEquals(1, result.size)
    }

    @Test
    fun `filterAnnouncements search non-matching returns empty`() {
        val announcements = listOf(ann("a1", "Maintenance"))
        val result = filterAnnouncements(announcements, "INEXISTANT_ZZZ", "all")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterAnnouncements targetFilter groupes matches group announcements`() {
        val announcements = listOf(
            ann("a1", "Groupes scolaires", cible = "groupes"),
            ann("a2", "Admins", cible = "admins")
        )
        val result = filterAnnouncements(announcements, "", "groupes")
        assertEquals(1, result.size)
        assertEquals("a1", result[0].id)
    }

    @Test
    fun `filterAnnouncements targetFilter billing matches billing announcements`() {
        val announcements = listOf(
            ann("a1", "Facturation", cible = "billing"),
            ann("a2", "Global", cible = "all")
        )
        val result = filterAnnouncements(announcements, "", "billing")
        assertEquals(1, result.size)
        assertEquals("a1", result[0].id)
    }

    @Test
    fun `filterAnnouncements targetFilter all returns everything`() {
        val announcements = listOf(
            ann("a1", "Groupes", cible = "groupes"),
            ann("a2", "Admins", cible = "admins"),
            ann("a3", "Global", cible = "all")
        )
        val result = filterAnnouncements(announcements, "", "all")
        assertEquals(3, result.size)
    }

    // ── Test 8 : defaultThreadKey ─────────────────────────────────────────────

    @Test
    fun `defaultThreadKey for group returns group prefix with groupId`() {
        val m = msg("m1", "group", groupId = "groupe::abc")
        assertEquals("group::groupe::abc", defaultThreadKey(m))
    }

    @Test
    fun `defaultThreadKey for admin returns admin prefix with adminId`() {
        val m = msg("m1", "admin", adminId = "user::xyz")
        assertEquals("admin::user::xyz", defaultThreadKey(m))
    }

    @Test
    fun `defaultThreadKey for all_groups returns all_groups`() {
        val m = msg("m1", "all_groups")
        assertEquals("all_groups", defaultThreadKey(m))
    }

    @Test
    fun `defaultThreadKey for all_admins returns all_admins`() {
        val m = msg("m1", "all_admins")
        assertEquals("all_admins", defaultThreadKey(m))
    }

    @Test
    fun `defaultThreadKey for group with null groupId returns group with empty`() {
        val m = msg("m1", "group", groupId = null)
        assertEquals("group::", defaultThreadKey(m))
    }

    // ── Test 9 : adminDisplayName ─────────────────────────────────────────────

    @Test
    fun `adminDisplayName returns firstName lastName when both present`() {
        val admin = AdminUserDto(id = "u1", username = "jd", firstName = "Jean", lastName = "Dupont", email = "j@test.cg")
        assertEquals("Jean Dupont", adminDisplayName(admin))
    }

    @Test
    fun `adminDisplayName uses only firstName when lastName is blank`() {
        val admin = AdminUserDto(id = "u1", username = "jd", firstName = "Jean", lastName = "", email = "j@test.cg")
        assertEquals("Jean", adminDisplayName(admin))
    }

    @Test
    fun `adminDisplayName falls back to email when both names are blank`() {
        val admin = AdminUserDto(id = "u1", username = "jd", firstName = "", lastName = "", email = "jean@test.cg")
        assertEquals("jean@test.cg", adminDisplayName(admin))
    }

    @Test
    fun `adminDisplayName falls back to username when names and email are blank`() {
        val admin = AdminUserDto(id = "u1", username = "jdupont", firstName = "", lastName = "", email = "")
        assertEquals("jdupont", adminDisplayName(admin))
    }

    // ── Test 10 : announcementTargetLabel ─────────────────────────────────────

    @Test
    fun `announcementTargetLabel for groupes returns correct label`() {
        assertEquals("Tous les groupes scolaires", announcementTargetLabel("groupes"))
    }

    @Test
    fun `announcementTargetLabel for billing returns correct label`() {
        assertEquals("Facturation / abonnements", announcementTargetLabel("billing"))
    }

    @Test
    fun `announcementTargetLabel for admins returns correct label`() {
        assertEquals("Administrateurs plateforme", announcementTargetLabel("admins"))
    }

    @Test
    fun `announcementTargetLabel for unknown returns fallback`() {
        assertEquals("Toute la plateforme", announcementTargetLabel("all"))
        assertEquals("Toute la plateforme", announcementTargetLabel(""))
        assertEquals("Toute la plateforme", announcementTargetLabel("unknown_value"))
    }

    @Test
    fun `announcementTargetLabel is case-insensitive`() {
        assertEquals("Tous les groupes scolaires", announcementTargetLabel("GROUPES"))
        assertEquals("Facturation / abonnements", announcementTargetLabel("BILLING"))
    }

    // ── Test 11 : thread computed properties ──────────────────────────────────

    @Test
    fun `messageCount equals messages size`() {
        val messages = listOf(
            msg("m1", "all_groups", threadKey = "all_groups", createdAt = 100L),
            msg("m2", "all_groups", threadKey = "all_groups", createdAt = 200L)
        )
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        assertEquals(2, threads[0].messageCount)
    }

    @Test
    fun `lastSubject is the subject of the most recent message`() {
        val messages = listOf(
            msg("m1", "all_groups", threadKey = "all_groups", createdAt = 100L),
            msg("m2", "all_groups", threadKey = "all_groups", createdAt = 500L)
        )
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        assertEquals("Sujet m2", threads[0].lastSubject)
    }

    @Test
    fun `lastCreatedAt is the max createdAt in the thread`() {
        val messages = listOf(
            msg("m1", "all_groups", threadKey = "all_groups", createdAt = 100L),
            msg("m2", "all_groups", threadKey = "all_groups", createdAt = 800L),
            msg("m3", "all_groups", threadKey = "all_groups", createdAt = 400L)
        )
        val threads = buildMessageThreads(messages, emptyList(), emptyList())
        assertEquals(800L, threads[0].lastCreatedAt)
    }

    @Test
    fun `empty messages list produces empty threads`() {
        val threads = buildMessageThreads(emptyList(), emptyList(), emptyList())
        assertTrue(threads.isEmpty())
    }
}
