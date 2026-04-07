package cg.epilote.shared.platform

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.couchbase.lite.CouchbaseLite
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory

private var appContext: Context? = null

actual fun initCouchbaseLite(context: Any?) {
    appContext = context as? Context
    CouchbaseLite.init(context as Context)
}

actual fun getPlatformName(): String = "Android"

actual fun deriveEncryptionKey(pin: String, userId: String): ByteArray {
    val salt = loadSalt(userId) ?: generateAndStoreSalt(userId)
    val spec = PBEKeySpec(pin.toCharArray(), salt, 150_000, 256)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    return factory.generateSecret(spec).encoded
}

actual fun generateAndStoreSalt(userId: String): ByteArray {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val alias = "epilote_salt_$userId"

    if (!keyStore.containsAlias(alias)) {
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore")
        keyGen.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                .build()
        )
        keyGen.generateKey()
    }

    val key = keyStore.getKey(alias, null) as SecretKey
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(key)
    return mac.doFinal(userId.toByteArray())
}

actual fun loadSalt(userId: String): ByteArray? {
    return runCatching { generateAndStoreSalt(userId) }.getOrNull()
}
