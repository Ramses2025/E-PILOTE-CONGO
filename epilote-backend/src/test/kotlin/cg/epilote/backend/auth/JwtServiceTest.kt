package cg.epilote.backend.auth

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JwtServiceTest {

    private fun createService(secret: String): JwtService {
        val service = JwtService()
        ReflectionTestUtils.setField(service, "secret", secret)
        ReflectionTestUtils.setField(service, "expirationMs", 3_600_000L)
        ReflectionTestUtils.setField(service, "refreshExpirationMs", 2_592_000_000L)
        ReflectionTestUtils.setField(service, "offlineExpirationMs", 2_592_000_000L)
        return service
    }

    @Test
    fun `validateSecret rejects blank secret`() {
        val service = createService("")
        assertThrows<IllegalArgumentException> {
            service.validateSecret()
        }
    }

    @Test
    fun `validateSecret rejects short secret`() {
        val service = createService("too-short")
        assertThrows<IllegalArgumentException> {
            service.validateSecret()
        }
    }

    @Test
    fun `validateSecret accepts 32-byte secret`() {
        val service = createService("a".repeat(32))
        service.validateSecret()
    }

    @Test
    fun `generateAccessToken and validateToken round-trip`() {
        val service = createService("a".repeat(64))
        service.validateSecret()

        val user = EpiloteUserDetails(
            userId = "user::123",
            username = "testuser",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            schoolId = "school::1",
            groupId = "group::1",
            role = UserRole.USER,
            permissions = emptyList(),
            passwordHash = "hash",
            isActive = true,
            mustChangePassword = false
        )

        val token = service.generateAccessToken(user)
        assertTrue(service.validateToken(token))
        assertEquals("user::123", service.getUserIdFromToken(token))
        assertTrue(service.isTokenType(token, "access"))
        assertFalse(service.isTokenType(token, "refresh"))
    }

    @Test
    fun `generateRefreshToken creates refresh type`() {
        val service = createService("a".repeat(64))
        service.validateSecret()

        val token = service.generateRefreshToken("user::456")
        assertTrue(service.validateToken(token))
        assertEquals("user::456", service.getUserIdFromToken(token))
        assertTrue(service.isTokenType(token, "refresh"))
    }

    @Test
    fun `validateToken returns false for invalid token`() {
        val service = createService("a".repeat(64))
        service.validateSecret()

        assertFalse(service.validateToken("invalid.token.here"))
    }

    @Test
    fun `validateToken returns false for tampered token`() {
        val service = createService("a".repeat(64))
        service.validateSecret()

        val service2 = createService("b".repeat(64))
        service2.validateSecret()

        val user = EpiloteUserDetails(
            userId = "user::789",
            username = "tamper",
            email = "tamper@example.com",
            firstName = "Tamper",
            lastName = "Test",
            schoolId = null,
            groupId = null,
            role = UserRole.SUPER_ADMIN,
            permissions = emptyList(),
            passwordHash = "hash",
            isActive = true,
            mustChangePassword = false
        )

        val token = service2.generateAccessToken(user)
        assertFalse(service.validateToken(token))
    }
}
