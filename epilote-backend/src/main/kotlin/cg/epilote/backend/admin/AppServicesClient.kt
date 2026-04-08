package cg.epilote.backend.admin

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class AppServicesClient {

    @Value("\${app-services.admin-url}")
    private lateinit var adminUrl: String

    @Value("\${app-services.admin-user}")
    private lateinit var adminUser: String

    @Value("\${app-services.admin-password}")
    private lateinit var adminPassword: String

    private val restTemplate = RestTemplate()

    private fun authHeaders(): HttpHeaders {
        val credentials = java.util.Base64.getEncoder()
            .encodeToString("$adminUser:$adminPassword".toByteArray())
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("Authorization", "Basic $credentials")
        }
    }

    fun provisionUser(userId: String, groupeId: String?, schoolIds: List<String>, role: String) {
        val syncPassword = "cbls::$userId"
        val channels = buildSet {
            schoolIds.filter { it.isNotBlank() }.forEach { schoolId ->
                add("sch::$schoolId")
                if (role == "DIRECTOR") add("sch::${schoolId}::admin")
            }
            if (!groupeId.isNullOrBlank()) {
                add("grp::$groupeId")
                if (role == "ADMIN_GROUPE") add("grp::${groupeId}::admin")
            }
        }

        val body = mapOf(
            "name"           to userId,
            "password"       to syncPassword,
            "admin_channels" to channels.toList(),
            "disabled"       to false
        )

        val updateUrl = "$adminUrl/epilote/_user/$userId"
        val createUrl = "$adminUrl/epilote/_user/"

        val updated = runCatching {
            restTemplate.exchange(updateUrl, HttpMethod.PUT, HttpEntity(body, authHeaders()), Map::class.java)
        }.isSuccess

        if (!updated) {
            runCatching {
                restTemplate.exchange(createUrl, HttpMethod.POST, HttpEntity(body, authHeaders()), Map::class.java)
            }
        }
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
