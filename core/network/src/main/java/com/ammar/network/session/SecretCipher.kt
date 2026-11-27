package com.ammar.network.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** The [name] a value is stored under is bound to it, so one entry cannot be passed off as another. */
internal interface SecretCipher {
    fun encrypt(name: String, plain: ByteArray): ByteArray
    fun decrypt(name: String, sealed: ByteArray): ByteArray

    /** Throws the key away, so the next call starts with a new one. */
    fun reset()
}

/** Where the cipher's key lives. */
internal interface KeySource {
    /** The key, created if there is none. */
    fun get(): SecretKey
    fun delete()
}

/**
 * AES-GCM with the entry name as additional authenticated data. The output is the 12-byte IV the
 * provider picked, followed by ciphertext and tag.
 *
 * Values written before the name was bound no longer decrypt; they are dropped, which only signs
 * out builds made before sign-in was released.
 */
internal class AesGcmCipher(private val keys: KeySource) : SecretCipher {

    override fun encrypt(name: String, plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        // No IV is passed in: AndroidKeyStore refuses caller-chosen IVs, which prevents reuse.
        cipher.init(Cipher.ENCRYPT_MODE, keys.get())
        val iv = cipher.iv
        check(iv.size == IV_BYTES)
        cipher.updateAAD(name.encodeToByteArray())
        return iv + cipher.doFinal(plain)
    }

    override fun decrypt(name: String, sealed: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keys.get(), GCMParameterSpec(TAG_BITS, sealed, 0, IV_BYTES))
        cipher.updateAAD(name.encodeToByteArray())
        return cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES)
    }

    override fun reset() = keys.delete()

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}

/** A key that never leaves AndroidKeyStore. */
internal class AndroidKeystoreKey(private val alias: String) : KeySource {

    @Synchronized
    override fun get(): SecretKey {
        (keyStore().getKey(alias, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    @Synchronized
    override fun delete() = keyStore().deleteEntry(alias)

    private fun keyStore(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
    }
}
