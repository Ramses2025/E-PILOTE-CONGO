package cg.epilote.backend.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService {

    @Value("\${jwt.secret}")
    private lateinit var secret: String

    @Value("\${jwt.expiration-ms}")
    private var expirationMs: Long = 3_600_000

    @Value("\${jwt.refresh-expiration-ms}")
    private var refreshExpirationMs: Long = 2_592_000_000

    @Value("\${jwt.offline-expiration-ms}")
    private var offlineExpirationMs: Long = 2_592_000_000

    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateAccessToken(user: EpiloteUserDetails): String = buildToken(
        subject = user.userId,
        claims = mapOf(
            "username"      to user.username,
            "ecoleId"       to (user.ecoleId ?: ""),
            "groupeId"      to (user.groupeId ?: ""),
            "role"          to user.role.name,
            "modulesAccess" to user.modulesAccess,
            "tokenType"     to "access"
        ),
        expirationMs = expirationMs
    )

    fun generateRefreshToken(userId: String): String = buildToken(
        subject = userId,
        claims = mapOf("tokenType" to "refresh"),
        expirationMs = refreshExpirationMs
    )

    fun generateOfflineToken(user: EpiloteUserDetails): String = buildToken(
        subject = user.userId,
        claims = mapOf(
            "ecoleId"       to (user.ecoleId ?: ""),
            "groupeId"      to (user.groupeId ?: ""),
            "role"          to user.role.name,
            "modulesAccess" to user.modulesAccess,
            "tokenType"     to "offline"
        ),
        expirationMs = offlineExpirationMs
    )

    fun validateToken(token: String): Boolean = runCatching {
        getClaims(token)
        true
    }.getOrDefault(false)

    fun getUserIdFromToken(token: String): String =
        getClaims(token).subject

    fun getClaimsFromToken(token: String): Claims =
        getClaims(token)

    fun isTokenType(token: String, type: String): Boolean =
        runCatching { getClaims(token)["tokenType"] == type }.getOrDefault(false)

    private fun buildToken(subject: String, claims: Map<String, Any>, expirationMs: Long): String {
        val now = Date()
        return Jwts.builder()
            .subject(subject)
            .claims(claims)
            .issuedAt(now)
            .expiration(Date(now.time + expirationMs))
            .signWith(signingKey)
            .compact()
    }

    private fun getClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
}
