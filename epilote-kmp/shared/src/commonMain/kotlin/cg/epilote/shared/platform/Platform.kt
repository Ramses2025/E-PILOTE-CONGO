package cg.epilote.shared.platform

expect fun initCouchbaseLite(context: Any?)

expect fun getPlatformName(): String

expect fun deriveEncryptionKey(pin: String, userId: String): ByteArray

expect fun generateAndStoreSalt(userId: String): ByteArray

expect fun loadSalt(userId: String): ByteArray?
