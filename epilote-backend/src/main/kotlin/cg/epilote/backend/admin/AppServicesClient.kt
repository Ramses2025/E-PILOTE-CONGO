package cg.epilote.backend.admin

 import cg.epilote.backend.auth.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class AppServicesClient(
    private val userRepository: UserRepository
) {

    @Value("\${app-services.admin-url}")
    private lateinit var adminUrl: String

    @Value("\${app-services.admin-user}")
    private lateinit var adminUser: String

    @Value("\${app-services.admin-password}")
    private lateinit var adminPassword: String

    private val restTemplate = RestTemplate()

    private val provisionedCollections = listOf(
        "grades",
        "attendances",
        "students",
        "inscriptions",
        "timetable",
        "report_cards",
        "disciplines",
        "academic_config",
        "staff",
        "staff_attendances",
        "announcements",
        "messages",
        "notifications"
    )

    private fun authHeaders(): HttpHeaders {
        val credentials = java.util.Base64.getEncoder()
            .encodeToString("$adminUser:$adminPassword".toByteArray())
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("Authorization", "Basic $credentials")
        }
    }

    fun provisionUser(userId: String, groupId: String?, schoolIds: List<String>, role: String): String {
        // SUPER_ADMIN works REST-only, never syncs via CBLite
        if (role == "SUPER_ADMIN") {
            throw IllegalArgumentException("SUPER_ADMIN does not use Couchbase Lite sync")
        }

        val syncToken = kotlinx.coroutines.runBlocking {
            userRepository.ensureSyncToken(userId)
        }
        val channels = buildSet {
            schoolIds.filter { it.isNotBlank() }.forEach { schoolId ->
                add("sch::$schoolId")
            }
            if (!groupId.isNullOrBlank() && role == "ADMIN_GROUPE") {
                add("grp::$groupId")
            }
        }

        val collectionAccess = mapOf(
            "_default" to provisionedCollections.associateWith {
                mapOf("admin_channels" to channels.toList())
            }
        )

        val body = mapOf(
            "name"              to userId,
            "password"          to syncToken,
            "collection_access" to collectionAccess,
            "disabled"          to false
        )

        val updateUrl = "$adminUrl/epilote/_user/$userId"
        val createUrl = "$adminUrl/epilote/_user/"

        val updateAttempt = runCatching {
            restTemplate.exchange(updateUrl, HttpMethod.PUT, HttpEntity(body, authHeaders()), Map::class.java)
        }

        if (updateAttempt.isSuccess) {
            return syncToken
        }

        val createAttempt = runCatching {
            restTemplate.exchange(createUrl, HttpMethod.POST, HttpEntity(body, authHeaders()), Map::class.java)
        }

        if (createAttempt.isSuccess) {
            return syncToken
        }

        val cause = createAttempt.exceptionOrNull() ?: updateAttempt.exceptionOrNull()
        throw IllegalStateException("Provisioning App Services impossible pour $userId", cause)
    }

    fun disableUser(userId: String) {
        runCatching {
            val body = mapOf("name" to userId, "disabled" to true)
            restTemplate.exchange(
                "$adminUrl/epilote/_user/$userId",
                HttpMethod.PUT,
                HttpEntity(body, authHeaders()),
                Map::class.java
            )
        }
    }
}
