package com.ammar.network

import com.ammar.network.session.EncryptedStorage
import com.ammar.network.session.AesGcmCipher
import com.ammar.network.session.KeySource
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
    override fun clear() {
        values.clear()
    }
}

/** An ordinary key that can be made to fail the way a broken Keystore key does. */
internal class FakeKeys(private var key: SecretKey = newKey()) : KeySource {
    /** Thrown by [get] while set. [delete] repairs the key unless [broken] is permanent. */
    var broken: Exception? = null
    var permanent = false
    var deletes = 0

    override fun get(): SecretKey {
        broken?.let { throw it }
        return key
    }

    override fun delete() {
        deletes++
        if (!permanent) broken = null
        key = newKey()
    }
}

/** An ordinary JVM key. AndroidKeyStore does not exist in unit tests. */
internal fun newKey(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

internal fun storage(store: KeyValueStore = MemoryStore(), key: SecretKey = newKey()) =
    storage(store, FakeKeys(key))

internal fun storage(store: KeyValueStore, keys: KeySource) = EncryptedStorage(store, AesGcmCipher(keys))

internal fun signedInStore(sessionId: String = "test-session") =
    SessionStore(storage(), Clock.systemUTC()).apply { save(StoredSession(sessionId, "7", "tester")) }
