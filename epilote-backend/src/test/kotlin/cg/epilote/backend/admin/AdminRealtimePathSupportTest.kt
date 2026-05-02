package cg.epilote.backend.admin

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdminRealtimePathSupportTest {

    @Test
    fun `shouldPublish returns true for mutating super-admin paths`() {
        assertTrue(AdminRealtimePathSupport.shouldPublish("/api/super-admin/groupes", "POST", 201))
        assertTrue(AdminRealtimePathSupport.shouldPublish("/api/super-admin/groupes/123", "PUT", 200))
        assertTrue(AdminRealtimePathSupport.shouldPublish("/api/super-admin/groupes/123", "DELETE", 204))
    }

    @Test
    fun `shouldPublish returns false for GET requests`() {
        assertFalse(AdminRealtimePathSupport.shouldPublish("/api/super-admin/groupes", "GET", 200))
    }

    @Test
    fun `shouldPublish returns false for events stream path`() {
        assertFalse(AdminRealtimePathSupport.shouldPublish("/api/super-admin/events/stream", "POST", 200))
    }

    @Test
    fun `shouldPublish returns false for error status codes`() {
        assertFalse(AdminRealtimePathSupport.shouldPublish("/api/super-admin/groupes", "POST", 400))
        assertFalse(AdminRealtimePathSupport.shouldPublish("/api/super-admin/groupes", "POST", 500))
    }

    @Test
    fun `resolveEntityType handles communication paths`() {
        assertEquals("announcement", AdminRealtimePathSupport.resolveEntityType("/api/super-admin/communications/announcements"))
        assertEquals("message", AdminRealtimePathSupport.resolveEntityType("/api/super-admin/communications/messages"))
    }

    @Test
    fun `resolveEntityType handles main entity paths`() {
        assertEquals("groupe", AdminRealtimePathSupport.resolveEntityType("/api/super-admin/groupes"))
        assertEquals("plan", AdminRealtimePathSupport.resolveEntityType("/api/super-admin/plans"))
        assertEquals("module", AdminRealtimePathSupport.resolveEntityType("/api/super-admin/modules"))
        assertEquals("category", AdminRealtimePathSupport.resolveEntityType("/api/super-admin/categories"))
        assertEquals("subscription", AdminRealtimePathSupport.resolveEntityType("/api/super-admin/subscriptions"))
        assertEquals("invoice", AdminRealtimePathSupport.resolveEntityType("/api/super-admin/invoices"))
        assertEquals("admin", AdminRealtimePathSupport.resolveEntityType("/api/super-admin/admins"))
    }

    @Test
    fun `resolveEntityType handles admin within group path`() {
        assertEquals("admin", AdminRealtimePathSupport.resolveEntityType("/api/super-admin/groupes/123/admins"))
    }

    @Test
    fun `resolveEntityId extracts IDs correctly`() {
        assertEquals("123", AdminRealtimePathSupport.resolveEntityId("/api/super-admin/groupes/123"))
        assertEquals("msg::1", AdminRealtimePathSupport.resolveEntityId("/api/super-admin/communications/messages/msg::1"))
        assertNull(AdminRealtimePathSupport.resolveEntityId("/api/super-admin/groupes"))
    }

    @Test
    fun `resolveAction maps HTTP methods`() {
        assertEquals("created", AdminRealtimePathSupport.resolveAction("POST", "/api/super-admin/groupes"))
        assertEquals("deleted", AdminRealtimePathSupport.resolveAction("DELETE", "/api/super-admin/groupes/123"))
        assertEquals("status_changed", AdminRealtimePathSupport.resolveAction("PUT", "/api/super-admin/invoices/123/status"))
        assertEquals("updated", AdminRealtimePathSupport.resolveAction("PUT", "/api/super-admin/groupes/123"))
    }

    @Test
    fun `eventType combines entity and action`() {
        assertEquals("GROUPE_CREATED", AdminRealtimePathSupport.eventType("groupe", "created"))
        assertEquals("MESSAGE_UPDATED", AdminRealtimePathSupport.eventType("message", "updated"))
    }
}
