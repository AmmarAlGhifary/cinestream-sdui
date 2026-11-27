package com.ammar.network

import com.ammar.network.session.EncryptedStorage
import com.ammar.network.session.AesGcmCipher
import com.ammar.network.session.KeyValueStore
import com.ammar.network.session.SessionStore
import com.ammar.network.session.StoredSession
import java.time.Clock
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

internal class MemoryStore : KeyValueStore {
    val values = mutableMapOf<String, String>()
    override fun get(name: String) = values[name]
    override fun put(name: String, value: String) {
        values[name] = value
    }
    override fun remove(name: String) {
        values.remove(name)
    }
}

/** An ordinary JVM key. AndroidKeyStore does not exist in unit tests. */
internal fun newKey(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

internal fun storage(store: KeyValueStore = MemoryStore(), key: SecretKey = newKey()) =
    EncryptedStorage(store, AesGcmCipher { key })

internal fun signedInStore(sessionId: String = "test-session") =
    SessionStore(storage(), Clock.systemUTC()).apply { save(StoredSession(sessionId, "7", "tester")) }
