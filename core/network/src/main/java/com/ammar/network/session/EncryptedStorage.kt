package com.ammar.network.session

import android.content.SharedPreferences
import java.security.GeneralSecurityException
import java.security.ProviderException
import java.util.Base64
import javax.crypto.AEADBadTagException

internal interface KeyValueStore {
    fun get(name: String): String?
    fun put(name: String, value: String)
    fun remove(name: String)
    fun clear()
}

internal class SharedPreferencesStore(private val preferences: SharedPreferences) : KeyValueStore {
    override fun get(name: String): String? = preferences.getString(name, null)

    override fun put(name: String, value: String) {
        preferences.edit().putString(name, value).apply()
    }

    override fun remove(name: String) {
        preferences.edit().remove(name).apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }
}

/**
 * Text that is only ever written to [store] encrypted.
 *
 * A key can stop working for good. When it does, the key and everything stored with it are thrown
 * away, so that the next sign-in works without the user having to clear the app's data.
 */
internal class EncryptedStorage(
    private val store: KeyValueStore,
    private val cipher: SecretCipher
) {
    /** Null when there is nothing, or when it can no longer be read. */
    fun read(name: String): String? {
        val stored = store.get(name) ?: return null
        return try {
            cipher.decrypt(Base64.getDecoder().decode(stored)).decodeToString()
        } catch (e: AEADBadTagException) {
            // The key works; this value was written with another one, or was changed.
            store.remove(name)
            null
        } catch (e: Exception) {
            if (e.isKeyFailure()) reset() else store.remove(name)
            null
        }
    }

    /**
     * Retries once with a new key if the current one fails.
     *
     * @return true if that happened, which means every other stored value is gone.
     */
    fun write(name: String, value: String): Boolean {
        var wasReset = false
        val sealed = try {
            seal(value)
        } catch (e: Exception) {
            if (!e.isKeyFailure()) throw e
            reset()
            wasReset = true
            seal(value)
        }
        store.put(name, sealed)
        return wasReset
    }

    fun remove(name: String) = store.remove(name)

    private fun seal(value: String): String =
        Base64.getEncoder().encodeToString(cipher.encrypt(value.encodeToByteArray()))

    private fun reset() {
        // A key too broken to delete is replaced anyway the next time one is generated.
        runCatching { cipher.reset() }
        store.clear()
    }

    // AndroidKeyStore also reports a broken key as a ProviderException.
    private fun Exception.isKeyFailure() = this is GeneralSecurityException || this is ProviderException
}
