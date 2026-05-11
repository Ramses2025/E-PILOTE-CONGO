package cg.epilote.backend.admin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.access.prepost.PreAuthorize

class AdminControllerSecurityAnnotationsTest {

    @Test
    fun `monetization endpoints require super admin role`() {
        assertPreAuthorize("createSubscription", "hasRole('SUPER_ADMIN')", CreateSubscriptionRequest::class.java)
        assertPreAuthorize("listSubscriptions", "hasRole('SUPER_ADMIN')")
        assertPreAuthorize("updateSubscriptionStatus", "hasRole('SUPER_ADMIN')", String::class.java, String::class.java, org.springframework.security.core.Authentication::class.java, jakarta.servlet.http.HttpServletRequest::class.java)
        assertPreAuthorize("createInvoice", "hasRole('SUPER_ADMIN')", CreateInvoiceRequest::class.java, org.springframework.security.core.Authentication::class.java, jakarta.servlet.http.HttpServletRequest::class.java)
        assertPreAuthorize("listInvoices", "hasRole('SUPER_ADMIN')")
        assertPreAuthorize("downloadInvoicePdf", "hasRole('SUPER_ADMIN')", String::class.java)
        assertPreAuthorize("updateInvoiceStatus", "hasRole('SUPER_ADMIN')", String::class.java, String::class.java, java.lang.Long::class.java, java.lang.Long::class.java, org.springframework.security.core.Authentication::class.java, jakarta.servlet.http.HttpServletRequest::class.java)
        assertPreAuthorize("recordPayment", "hasRole('SUPER_ADMIN')", RecordPaymentRequest::class.java, org.springframework.security.core.Authentication::class.java, jakarta.servlet.http.HttpServletRequest::class.java)
        assertPreAuthorize("listPaymentReceipts", "hasRole('SUPER_ADMIN')")
        assertPreAuthorize("listGroupePaymentReceipts", "hasRole('SUPER_ADMIN')", String::class.java)
    }

    private fun assertPreAuthorize(methodName: String, expected: String, vararg parameterTypes: Class<*>) {
        val method = AdminController::class.java.getDeclaredMethod(methodName, *parameterTypes)
        val annotation = method.getAnnotation(PreAuthorize::class.java)
        assertEquals(expected, annotation?.value, "L'endpoint $methodName doit être protégé par @PreAuthorize($expected)")
    }
}
