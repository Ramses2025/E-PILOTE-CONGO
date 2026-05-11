package cg.epilote.backend.auth

import cg.epilote.backend.admin.AdminPlanRepository
import cg.epilote.backend.admin.AdminSubscriptionRepository
import cg.epilote.backend.admin.PlanResponse
import cg.epilote.backend.admin.SubscriptionExpiredException
import cg.epilote.backend.admin.SubscriptionResponse
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder

@ExtendWith(MockitoExtension::class)
class AuthServiceMonetizationTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var jwtService: JwtService

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var subscriptionRepository: AdminSubscriptionRepository

    @Mock
    private lateinit var planRepository: AdminPlanRepository

    private lateinit var service: AuthService

    @BeforeEach
    fun setUp() {
        service = AuthService(
            userRepository = userRepository,
            jwtService = jwtService,
            passwordEncoder = passwordEncoder,
            subscriptionRepository = subscriptionRepository,
            planRepository = planRepository
        )
    }

    @Test
    fun `login blocks admin group when active subscription is missing`() {
        val user = adminGroupeUser()
        runBlocking { doReturn(user).`when`(userRepository).findByEmail("admin@school.cg") }
        doReturn(true).`when`(passwordEncoder).matches("secret", user.passwordHash)
        runBlocking { doReturn(null).`when`(subscriptionRepository).getActiveSubscriptionByGroupe("group-1") }

        val error = assertThrows(SubscriptionExpiredException::class.java) {
            runBlocking { service.login(LoginRequest("admin@school.cg", "secret")) }
        }

        assertEquals("Abonnement expiré ou inexistant — contactez le support E-PILOTE", error.message)
        runBlocking { verify(planRepository, never()).getPlanById(org.mockito.ArgumentMatchers.anyString()) }
    }

    @Test
    fun `login blocks admin group when plan is suspended`() {
        val user = adminGroupeUser()
        val subscription = activeSubscription(planId = "plan-pro")
        runBlocking { doReturn(user).`when`(userRepository).findByEmail("admin@school.cg") }
        doReturn(true).`when`(passwordEncoder).matches("secret", user.passwordHash)
        runBlocking { doReturn(subscription).`when`(subscriptionRepository).getActiveSubscriptionByGroupe("group-1") }
        runBlocking { doReturn(plan(isActive = false)).`when`(planRepository).getPlanById("plan-pro") }

        val error = assertThrows(SubscriptionExpiredException::class.java) {
            runBlocking { service.login(LoginRequest("admin@school.cg", "secret")) }
        }

        assertEquals("Le plan Plan Pro est suspendu — contactez le support E-PILOTE", error.message)
    }

    @Test
    fun `refresh blocks standard user when group subscription is expired`() {
        val user = standardUser()
        doReturn(true).`when`(jwtService).validateToken("refresh-token")
        doReturn(true).`when`(jwtService).isTokenType("refresh-token", "refresh")
        doReturn("user-1").`when`(jwtService).getUserIdFromToken("refresh-token")
        runBlocking { doReturn(user).`when`(userRepository).findById("user-1") }
        runBlocking { doReturn(null).`when`(subscriptionRepository).getActiveSubscriptionByGroupe("group-1") }

        val error = assertThrows(SubscriptionExpiredException::class.java) {
            runBlocking { service.refresh(RefreshRequest("refresh-token")) }
        }

        assertEquals("Abonnement du groupe expiré — contactez votre administrateur", error.message)
    }

    @Test
    fun `refresh bypasses subscription enforcement for super admin`() {
        val user = superAdminUser()
        doReturn(true).`when`(jwtService).validateToken("refresh-token")
        doReturn(true).`when`(jwtService).isTokenType("refresh-token", "refresh")
        doReturn("super-1").`when`(jwtService).getUserIdFromToken("refresh-token")
        runBlocking { doReturn(user).`when`(userRepository).findById("super-1") }
        doReturn("new-access-token").`when`(jwtService).generateAccessToken(user)

        val response = runBlocking { service.refresh(RefreshRequest("refresh-token")) }

        assertEquals("new-access-token", response.accessToken)
        assertEquals(3600, response.expiresIn)
        runBlocking { verify(subscriptionRepository, never()).getActiveSubscriptionByGroupe(org.mockito.ArgumentMatchers.anyString()) }
        runBlocking { verify(planRepository, never()).getPlanById(org.mockito.ArgumentMatchers.anyString()) }
    }

    private fun adminGroupeUser() = EpiloteUserDetails(
        userId = "admin-1",
        username = "admin",
        email = "admin@school.cg",
        firstName = "Admin",
        lastName = "Groupe",
        schoolId = null,
        groupId = "group-1",
        role = UserRole.ADMIN_GROUPE,
        permissions = emptyList(),
        passwordHash = "encoded-secret",
        isActive = true
    )

    private fun standardUser() = EpiloteUserDetails(
        userId = "user-1",
        username = "teacher",
        email = "teacher@school.cg",
        firstName = "Teacher",
        lastName = "User",
        schoolId = "school-1",
        groupId = "group-1",
        role = UserRole.USER,
        permissions = emptyList(),
        passwordHash = "encoded-secret",
        isActive = true
    )

    private fun superAdminUser() = EpiloteUserDetails(
        userId = "super-1",
        username = "super",
        email = "super@admin.cg",
        firstName = "Super",
        lastName = "Admin",
        schoolId = null,
        groupId = null,
        role = UserRole.SUPER_ADMIN,
        permissions = emptyList(),
        passwordHash = "encoded-secret",
        isActive = true
    )

    private fun activeSubscription(planId: String) = SubscriptionResponse(
        id = "sub::group-1",
        groupeId = "group-1",
        planId = planId,
        statut = "active",
        dateDebut = System.currentTimeMillis() - 1_000,
        dateFin = System.currentTimeMillis() + 86_400_000,
        renouvellementAuto = false,
        createdAt = System.currentTimeMillis() - 1_000
    )

    private fun plan(isActive: Boolean) = PlanResponse(
        id = "plan-pro",
        nom = "Plan Pro",
        type = "paid",
        prixXAF = 10000,
        maxStudents = 1000,
        maxPersonnel = 100,
        modulesIncluded = listOf("students", "billing"),
        isActive = isActive
    )
}
