package com.ammar.network

import com.ammar.network.session.EncryptedStorage
import com.ammar.network.session.AesGcmCipher
import com.ammar.network.session.KeySource
import com.ammar.network.session.KeyValueStore
import com.ammar.network.session.SessionStore
import com.ammar.network.session.Account
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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

/** Records which threads touch [store]. */
internal class ThreadRecordingStore(private val store: KeyValueStore = MemoryStore()) : KeyValueStore {
    private val seen = mutableSetOf<String>()
    val threads: Set<String> get() = synchronized(seen) { seen.toSet() }

    // In tests coroutines append their own id to the thread name.
    private fun record() {
        synchronized(seen) { seen.add(Thread.currentThread().name.substringBefore(" @")) }
    }

    override fun get(name: String) = store.get(name).also { record() }
    override fun put(name: String, value: String) = store.put(name, value).also { record() }
    override fun remove(name: String) = store.remove(name).also { record() }
    override fun clear() = store.clear().also { record() }
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

internal const val ORIGIN = "https://backend.test:443"

internal val TESTER = Account("7", "tester")

/** Loads and answers on the calling thread unless another dispatcher is given. */
internal fun sessionStore(
    storage: EncryptedStorage = storage(),
    clock: Clock = Clock.systemUTC(),
    origin: String = ORIGIN,
    io: CoroutineDispatcher = Dispatchers.Unconfined
) = SessionStore(storage, origin, clock, io)

internal fun signedInStore(sessionId: String = "test-session") =
    sessionStore().apply { save(sessionId, TESTER) }
