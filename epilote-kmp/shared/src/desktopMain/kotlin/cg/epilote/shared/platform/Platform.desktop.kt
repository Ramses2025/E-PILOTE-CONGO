package cg.epilote.shared.platform

import com.couchbase.lite.CouchbaseLite
import java.io.File
import java.security.SecureRandom
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory

private val saltDir: File by lazy {
    File(System.getProperty("user.home"), ".epilote" + File.separator + "keys").also { it.mkdirs() }
}

actual fun initCouchbaseLite(context: Any?) {
    CouchbaseLite.init()
}

actual fun getPlatformName(): String = "Desktop (Windows)"

actual fun deriveEncryptionKey(pin: String, userId: String): ByteArray {
    val salt = loadSalt(userId) ?: generateAndStoreSalt(userId)
    val spec = PBEKeySpec(pin.toCharArray(), salt, 150_000, 256)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    return factory.generateSecret(spec).encoded
}

actual fun generateAndStoreSalt(userId: String): ByteArray {
    val file = File(saltDir, "salt_$userId.bin")
    val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
    file.writeBytes(salt)
    return salt
}

actual fun loadSalt(userId: String): ByteArray? {
    val file = File(saltDir, "salt_$userId.bin")
    return if (file.exists()) file.readBytes() else null
}
