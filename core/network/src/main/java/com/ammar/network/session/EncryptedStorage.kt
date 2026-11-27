package com.ammar.network.session

import android.content.SharedPreferences
import java.util.Base64

internal interface KeyValueStore {
    fun get(name: String): String?
    fun put(name: String, value: String)
    fun remove(name: String)
}

internal class SharedPreferencesStore(private val preferences: SharedPreferences) : KeyValueStore {
    override fun get(name: String): String? = preferences.getString(name, null)

    override fun put(name: String, value: String) {
        preferences.edit().putString(name, value).apply()
    }

    override fun remove(name: String) {
        preferences.edit().remove(name).apply()
    }
}

/** Text that is only ever written to [store] encrypted. */
internal class EncryptedStorage(
    private val store: KeyValueStore,
    private val cipher: SecretCipher
) {
    /** Null when there is nothing, or when it can no longer be decrypted (e.g. the key is gone). */
    fun read(name: String): String? {
        val stored = store.get(name) ?: return null
        return runCatching { cipher.decrypt(Base64.getDecoder().decode(stored)).decodeToString() }.getOrNull()
    }

    fun write(name: String, value: String) {
        store.put(name, Base64.getEncoder().encodeToString(cipher.encrypt(value.encodeToByteArray())))
    }

    fun remove(name: String) = store.remove(name)
}
