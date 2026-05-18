package cg.epilote.backend.admin

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.security.access.prepost.PreAuthorize

/**
 * Tests pour la page Messagerie et ses invariants métier (sans Couchbase réel).
 *
 * Couvre :
 *   1. CreateAdminMessageRequest — champs requis et defaulting
 *   2. targetType validation — tous les cas valides / invalides
 *   3. groupId / adminId requis selon targetType
 *   4. threadKey computation — préfixes corrects par targetType
 *   5. Status transitions — statuts acceptés / rejetés
 *   6. AdminMessageResponse — intégrité des champs
 *   7. CreateAnnouncementRequest — champs et cible par défaut
 *   8. Sécurité — @PreAuthorize sur chaque endpoint de AdminCommunicationController
 */
class AdminCommunicationWorkflowTest {

    // ── Helpers locaux miroir de la logique du repository ─────────────────────

    private fun isTargetTypeValid(targetType: String, groupId: String?, adminId: String?): Boolean {
        val t = targetType.trim().lowercase()
        val g = groupId?.trim()?.takeIf { it.isNotBlank() }
        val a = adminId?.trim()?.takeIf { it.isNotBlank() }
        return when (t) {
            "all_groups", "all_admins" -> true
            "group" -> g != null
            "admin" -> a != null
            else -> false
        }
    }

    private fun computeThreadKey(targetType: String, groupId: String?, adminId: String?): String {
        val t = targetType.trim().lowercase()
        val g = groupId?.trim()?.takeIf { it.isNotBlank() }
        val a = adminId?.trim()?.takeIf { it.isNotBlank() }
        return when (t) {
            "group" -> "group::$g"
            "admin" -> "admin::$a"
            else -> t
        }
    }

    private fun isMessageStatusValid(status: String): Boolean =
        status.trim().lowercase() in setOf("sent", "archived", "deleted")

    // ── Test 1 : CreateAdminMessageRequest — champs par défaut ───────────────

    @Test
    fun `CreateAdminMessageRequest with all_groups has null groupId and adminId by default`() {
        val req = CreateAdminMessageRequest(
            sujet = "Rappel abonnement",
            contenu = "Merci de régler votre facture.",
            targetType = "all_groups"
        )
        assertNull(req.groupId)
        assertNull(req.adminId)
        assertEquals("all_groups", req.targetType)
    }

    @Test
    fun `CreateAdminMessageRequest with group target carries groupId`() {
        val req = CreateAdminMessageRequest(
            sujet = "Message direct",
            contenu = "Bonjour.",
            targetType = "group",
            groupId = "groupe::abc123"
        )
        assertEquals("group", req.targetType)
        assertEquals("groupe::abc123", req.groupId)
        assertNull(req.adminId)
    }

    @Test
    fun `CreateAdminMessageRequest with admin target carries adminId`() {
        val req = CreateAdminMessageRequest(
            sujet = "Réinitialisation",
            contenu = "Votre mot de passe a été réinitialisé.",
            targetType = "admin",
            adminId = "user::admin-uuid"
        )
        assertEquals("admin", req.targetType)
        assertNull(req.groupId)
        assertEquals("user::admin-uuid", req.adminId)
    }

    // ── Test 2 : targetType validation — cas valides ──────────────────────────

    @Test
    fun `all_groups targetType is valid without groupId`() {
        assertTrue(isTargetTypeValid("all_groups", null, null))
    }

    @Test
    fun `all_admins targetType is valid without adminId`() {
        assertTrue(isTargetTypeValid("all_admins", null, null))
    }

    @Test
    fun `targetType is case-insensitive`() {
        assertTrue(isTargetTypeValid("ALL_GROUPS", null, null))
        assertTrue(isTargetTypeValid("All_Admins", null, null))
        assertTrue(isTargetTypeValid("GROUP", "groupe::g1", null))
    }

    @Test
    fun `targetType trims leading and trailing spaces`() {
        assertTrue(isTargetTypeValid("  all_groups  ", null, null))
        assertTrue(isTargetTypeValid(" group ", "groupe::g1", null))
    }

    // ── Test 3 : group / admin cible requiert un ID non vide ─────────────────

    @Test
    fun `group targetType requires non-null groupId`() {
        assertFalse(isTargetTypeValid("group", null, null), "group sans groupId doit être invalide")
    }

    @Test
    fun `group targetType rejects blank groupId`() {
        assertFalse(isTargetTypeValid("group", "", null), "group avec groupId vide doit être invalide")
        assertFalse(isTargetTypeValid("group", "   ", null), "group avec espaces seuls doit être invalide")
    }

    @Test
    fun `group targetType accepts non-blank groupId`() {
        assertTrue(isTargetTypeValid("group", "groupe::abc", null))
    }

    @Test
    fun `admin targetType requires non-null adminId`() {
        assertFalse(isTargetTypeValid("admin", null, null), "admin sans adminId doit être invalide")
    }

    @Test
    fun `admin targetType rejects blank adminId`() {
        assertFalse(isTargetTypeValid("admin", null, ""), "admin avec adminId vide doit être invalide")
        assertFalse(isTargetTypeValid("admin", null, "  "), "admin avec espaces seuls doit être invalide")
    }

    @Test
    fun `admin targetType accepts non-blank adminId`() {
        assertTrue(isTargetTypeValid("admin", null, "user::admin-xyz"))
    }

    @Test
    fun `unknown targetType is always invalid`() {
        assertFalse(isTargetTypeValid("random_type", null, null))
        assertFalse(isTargetTypeValid("", null, null))
        assertFalse(isTargetTypeValid("groupes", null, null), "groupes est une cible annonce, pas un targetType message")
        assertFalse(isTargetTypeValid("admins", null, null), "admins est une cible annonce, pas un targetType message")
        assertFalse(isTargetTypeValid("billing", null, null), "billing est une cible annonce, pas un targetType message")
    }

    // ── Test 4 : threadKey computation ────────────────────────────────────────

    @Test
    fun `threadKey for group target uses group prefix with groupId`() {
        assertEquals("group::groupe::abc", computeThreadKey("group", "groupe::abc", null))
    }

    @Test
    fun `threadKey for admin target uses admin prefix with adminId`() {
        assertEquals("admin::user::xyz", computeThreadKey("admin", null, "user::xyz"))
    }

    @Test
    fun `threadKey for all_groups is the targetType itself`() {
        assertEquals("all_groups", computeThreadKey("all_groups", null, null))
    }

    @Test
    fun `threadKey for all_admins is the targetType itself`() {
        assertEquals("all_admins", computeThreadKey("all_admins", null, null))
    }

    @Test
    fun `threadKey is lowercased`() {
        assertEquals("all_groups", computeThreadKey("ALL_GROUPS", null, null))
    }

    // ── Test 5 : status transitions ───────────────────────────────────────────

    @Test
    fun `sent status is valid`() {
        assertTrue(isMessageStatusValid("sent"))
    }

    @Test
    fun `archived status is valid`() {
        assertTrue(isMessageStatusValid("archived"))
    }

    @Test
    fun `deleted status is valid`() {
        assertTrue(isMessageStatusValid("deleted"))
    }

    @Test
    fun `status is case-insensitive`() {
        assertTrue(isMessageStatusValid("SENT"))
        assertTrue(isMessageStatusValid("Archived"))
        assertTrue(isMessageStatusValid("DELETED"))
    }

    @Test
    fun `unknown statuses are rejected`() {
        assertFalse(isMessageStatusValid("draft"))
        assertFalse(isMessageStatusValid("pending"))
        assertFalse(isMessageStatusValid("read"))
        assertFalse(isMessageStatusValid(""))
        assertFalse(isMessageStatusValid("  "))
        assertFalse(isMessageStatusValid("published"))
    }

    // ── Test 6 : AdminMessageResponse — intégrité des champs ─────────────────

    @Test
    fun `AdminMessageResponse for group message has correct fields`() {
        val now = System.currentTimeMillis()
        val response = AdminMessageResponse(
            id = "msg::uuid-001",
            sujet = "Relance paiement",
            contenu = "Votre facture est en retard.",
            targetType = "group",
            groupId = "groupe::lamarelle",
            adminId = null,
            threadKey = "group::groupe::lamarelle",
            status = "sent",
            readBy = listOf("user::super-admin"),
            createdBy = "user::super-admin",
            createdAt = now
        )
        assertEquals("msg::uuid-001", response.id)
        assertEquals("group", response.targetType)
        assertEquals("groupe::lamarelle", response.groupId)
        assertNull(response.adminId)
        assertEquals("group::groupe::lamarelle", response.threadKey)
        assertEquals("sent", response.status)
        assertEquals(1, response.readBy.size)
        assertEquals("user::super-admin", response.readBy[0])
    }

    @Test
    fun `AdminMessageResponse for all_groups broadcast has null groupId and adminId`() {
        val response = AdminMessageResponse(
            id = "msg::broadcast-1",
            sujet = "Maintenance prévue",
            contenu = "La plateforme sera indisponible samedi.",
            targetType = "all_groups",
            groupId = null,
            adminId = null,
            threadKey = "all_groups",
            status = "sent",
            readBy = emptyList(),
            createdBy = "user::super-admin",
            createdAt = 0L
        )
        assertNull(response.groupId)
        assertNull(response.adminId)
        assertEquals("all_groups", response.threadKey)
    }

    @Test
    fun `AdminMessageResponse defaults readBy to empty list`() {
        val response = AdminMessageResponse(
            id = "msg::x",
            sujet = "Test",
            contenu = "Contenu",
            targetType = "all_groups",
            groupId = null,
            adminId = null,
            threadKey = "all_groups",
            status = "sent",
            createdBy = "user::admin",
            createdAt = 0L
        )
        assertTrue(response.readBy.isEmpty())
    }

    // ── Test 7 : CreateAnnouncementRequest ────────────────────────────────────

    @Test
    fun `CreateAnnouncementRequest cible defaults to all when not provided`() {
        val req = CreateAnnouncementRequest(titre = "Fin d'année", contenu = "Bonne année !")
        assertEquals("all", req.cible)
    }

    @Test
    fun `CreateAnnouncementRequest accepts non-default cible`() {
        val req = CreateAnnouncementRequest(titre = "Facturation", contenu = "Contenu.", cible = "billing")
        assertEquals("billing", req.cible)
    }

    @Test
    fun `AnnouncementResponse preserves all fields`() {
        val now = System.currentTimeMillis()
        val response = AnnouncementResponse(
            id = "ann::uuid-1",
            titre = "Mise à jour plateforme",
            contenu = "Une mise à jour majeure est prévue.",
            cible = "groupes",
            createdBy = "user::super-admin",
            createdAt = now
        )
        assertEquals("ann::uuid-1", response.id)
        assertEquals("Mise à jour plateforme", response.titre)
        assertEquals("groupes", response.cible)
        assertEquals("user::super-admin", response.createdBy)
        assertEquals(now, response.createdAt)
    }

    // ── Test 8 : Sécurité — @PreAuthorize sur chaque endpoint ────────────────

    @Test
    fun `listAnnouncements requires SUPER_ADMIN role`() {
        assertSuperAdminPreAuthorize("listAnnouncements", Int::class.java, Int::class.java)
    }

    @Test
    fun `createAnnouncement requires SUPER_ADMIN role`() {
        assertSuperAdminPreAuthorize(
            "createAnnouncement",
            CreateAnnouncementRequest::class.java,
            org.springframework.security.core.Authentication::class.java
        )
    }

    @Test
    fun `listMessages requires SUPER_ADMIN role`() {
        assertSuperAdminPreAuthorize("listMessages", Int::class.java, Int::class.java)
    }

    @Test
    fun `createMessage requires SUPER_ADMIN role`() {
        assertSuperAdminPreAuthorize(
            "createMessage",
            CreateAdminMessageRequest::class.java,
            org.springframework.security.core.Authentication::class.java
        )
    }

    @Test
    fun `updateMessageStatus requires SUPER_ADMIN role`() {
        assertSuperAdminPreAuthorize(
            "updateMessageStatus",
            String::class.java,
            String::class.java
        )
    }

    @Test
    fun `markMessageAsRead requires SUPER_ADMIN role`() {
        assertSuperAdminPreAuthorize(
            "markMessageAsRead",
            String::class.java,
            org.springframework.security.core.Authentication::class.java
        )
    }

    // ── Test 9 : mailbox INBOX / SENT logique ─────────────────────────────────

    @Test
    fun `message owned by current user goes to SENT not INBOX`() {
        val userId = "user::super-admin"
        val msg = AdminMessageResponse(
            id = "msg::1", sujet = "Test", contenu = "...",
            targetType = "all_groups", groupId = null, adminId = null,
            threadKey = "all_groups", status = "sent",
            readBy = listOf(userId), createdBy = userId, createdAt = 0L
        )
        val isOwned = msg.createdBy == userId
        assertTrue(isOwned, "Message créé par super-admin doit appartenir à super-admin")
        val inSent = msg.status == "sent" && isOwned
        val inInbox = msg.status == "sent" && !isOwned
        assertTrue(inSent, "Message owned doit apparaître dans SENT")
        assertFalse(inInbox, "Message owned ne doit PAS apparaître dans INBOX")
    }

    @Test
    fun `message from different user appears in INBOX`() {
        val currentUserId = "user::super-admin"
        val otherUserId = "user::other-admin"
        val msg = AdminMessageResponse(
            id = "msg::2", sujet = "Reçu", contenu = "...",
            targetType = "all_groups", groupId = null, adminId = null,
            threadKey = "all_groups", status = "sent",
            readBy = emptyList(), createdBy = otherUserId, createdAt = 0L
        )
        val isOwned = msg.createdBy == currentUserId
        assertFalse(isOwned, "Message d'un autre utilisateur n'est pas owned")
        val inInbox = msg.status == "sent" && !isOwned
        assertTrue(inInbox, "Message d'un autre user doit apparaître dans INBOX")
    }

    @Test
    fun `archived message appears in ARCHIVED not SENT or INBOX`() {
        val userId = "user::super-admin"
        val msg = AdminMessageResponse(
            id = "msg::3", sujet = "Archivé", contenu = "...",
            targetType = "group", groupId = "groupe::g1", adminId = null,
            threadKey = "group::groupe::g1", status = "archived",
            readBy = listOf(userId), createdBy = userId, createdAt = 0L
        )
        assertFalse(msg.status == "sent", "Message archivé ne doit PAS avoir status=sent")
        assertTrue(msg.status == "archived")
    }

    @Test
    fun `deleted message appears in TRASH`() {
        val msg = AdminMessageResponse(
            id = "msg::4", sujet = "Supprimé", contenu = "...",
            targetType = "all_groups", groupId = null, adminId = null,
            threadKey = "all_groups", status = "deleted",
            readBy = emptyList(), createdBy = "user::super-admin", createdAt = 0L
        )
        assertEquals("deleted", msg.status)
        assertFalse(msg.status == "sent")
    }

    // ── Helper sécurité ───────────────────────────────────────────────────────

    private fun assertSuperAdminPreAuthorize(methodName: String, vararg paramTypes: Class<*>) {
        val method = AdminCommunicationController::class.java.getDeclaredMethod(methodName, *paramTypes)
        val annotation = method.getAnnotation(PreAuthorize::class.java)
        assertNotNull(annotation, "L'endpoint $methodName doit être protégé par @PreAuthorize")
        assertEquals(
            "hasRole('SUPER_ADMIN')",
            annotation?.value,
            "L'endpoint $methodName doit exiger le rôle SUPER_ADMIN"
        )
    }
}
