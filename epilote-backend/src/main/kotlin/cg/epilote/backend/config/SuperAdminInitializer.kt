package cg.epilote.backend.config

import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.query.execute
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class SuperAdminInitializer(
    private val bucket: Bucket,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${epilote.super-admin.email:super@admin.cg}")
    private val superAdminEmail: String,
    @Value("\${epilote.super-admin.password:}")
    private val superAdminPassword: String
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(SuperAdminInitializer::class.java)

    override fun run(args: ApplicationArguments?) {
        runBlocking {
            try {
                ensureSuperAdmin()
            } catch (e: Exception) {
                log.warn("Impossible de vérifier/créer le SUPER_ADMIN : ${e.message}")
            }
        }
    }

    private suspend fun ensureSuperAdmin() {
        val scope = bucket.defaultScope()
        val usersCol: Collection = scope.collection("users")

        val superAdminId = "user::super-admin"
        val email = superAdminEmail
        val password = superAdminPassword.ifBlank {
            log.warn("⚠️  epilote.super-admin.password non configuré — utilisation du mot de passe par défaut (DANGEREUX EN PRODUCTION)")
            "Admin@2024!"
        }

        val existing = scope.query(
            "SELECT META().id FROM `users` WHERE `role` = 'SUPER_ADMIN' LIMIT 1"
        ).execute()

        if (existing.rows.isNotEmpty()) {
            log.info("SUPER_ADMIN déjà existant — aucune modification.")
            return
        }

        val passwordHash = passwordEncoder.encode(password)

        val doc = mapOf(
            "type"         to "user",
            "username"     to "superadmin",
            "email"        to email,
            "passwordHash" to passwordHash,
            "nom"          to "Administrateur",
            "prenom"       to "Super",
            "schoolId"     to null,
            "groupId"      to null,
            "role"         to "SUPER_ADMIN",
            "permissions"  to emptyList<Any>(),
            "isActive"     to true,
            "createdAt"    to System.currentTimeMillis(),
            "updatedAt"    to System.currentTimeMillis()
        )

        usersCol.insert(superAdminId, doc)

        log.info("═══════════════════════════════════════════════════")
        log.info("  SUPER ADMIN créé avec succès !")
        log.info("  Email    : $email")
        log.info("  Rôle     : SUPER_ADMIN")
        log.info("═══════════════════════════════════════════════════")
    }
}
