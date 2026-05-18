package cg.epilote.backend.admin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication

/**
 * Vérifie que chaque endpoint de AdminGroupeController est protégé par
 * @PreAuthorize("hasRole('ADMIN_GROUPE')") — régression contre toute
 * ouverture accidentelle d'un endpoint sensible.
 */
class AdminGroupeControllerSecurityAnnotationsTest {

    @Test
    fun `getGroupeDashboardStats requires ADMIN_GROUPE role`() {
        assertAdminGroupePreAuthorize("getGroupeDashboardStats", String::class.java)
    }

    @Test
    fun `createSubscriptionRequest requires ADMIN_GROUPE role`() {
        assertAdminGroupePreAuthorize(
            "createSubscriptionRequest",
            String::class.java,
            SubscriptionRequestBody::class.java,
            Authentication::class.java
        )
    }

    @Test
    fun `createEcole requires ADMIN_GROUPE role`() {
        assertAdminGroupePreAuthorize("createEcole", String::class.java, CreateEcoleRequest::class.java)
    }

    @Test
    fun `listEcoles requires ADMIN_GROUPE role`() {
        assertAdminGroupePreAuthorize("listEcoles", String::class.java)
    }

    @Test
    fun `listModulesDisponibles requires ADMIN_GROUPE role`() {
        assertAdminGroupePreAuthorize("listModulesDisponibles", String::class.java)
    }

    @Test
    fun `listCategoriesDisponibles requires ADMIN_GROUPE role`() {
        assertAdminGroupePreAuthorize("listCategoriesDisponibles", String::class.java)
    }

    @Test
    fun `createProfil requires ADMIN_GROUPE role`() {
        assertAdminGroupePreAuthorize(
            "createProfil",
            String::class.java,
            CreateProfilRequest::class.java,
            Authentication::class.java
        )
    }

    @Test
    fun `listProfils requires ADMIN_GROUPE role`() {
        assertAdminGroupePreAuthorize("listProfils", String::class.java)
    }

    @Test
    fun `createUser requires ADMIN_GROUPE role`() {
        assertAdminGroupePreAuthorize("createUser", String::class.java, CreateUserRequest::class.java)
    }

    @Test
    fun `listUsers requires ADMIN_GROUPE role`() {
        assertAdminGroupePreAuthorize("listUsers", String::class.java)
    }

    @Test
    fun `assignProfil requires ADMIN_GROUPE role`() {
        assertAdminGroupePreAuthorize("assignProfil", String::class.java, AssignProfilRequest::class.java)
    }

    @Test
    fun `invoiceTimeline requires ADMIN_GROUPE role`() {
        assertAdminGroupePreAuthorize("invoiceTimeline", String::class.java)
    }

    @Test
    fun `activityTimeline requires ADMIN_GROUPE role`() {
        assertAdminGroupePreAuthorize("activityTimeline", String::class.java)
    }

    @Test
    fun `getNotifications requires ADMIN_GROUPE role`() {
        assertAdminGroupePreAuthorize("getNotifications", String::class.java)
    }

    @Test
    fun `getModuleKpi requires ADMIN_GROUPE role`() {
        assertAdminGroupePreAuthorize("getModuleKpi", String::class.java, String::class.java)
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun assertAdminGroupePreAuthorize(methodName: String, vararg paramTypes: Class<*>) {
        val method = AdminGroupeController::class.java.getDeclaredMethod(methodName, *paramTypes)
        val annotation = method.getAnnotation(PreAuthorize::class.java)
        assertNotNull(annotation, "L'endpoint $methodName doit être protégé par @PreAuthorize")
        assertEquals(
            "hasRole('ADMIN_GROUPE')",
            annotation?.value,
            "L'endpoint $methodName doit exiger le rôle ADMIN_GROUPE"
        )
    }
}
