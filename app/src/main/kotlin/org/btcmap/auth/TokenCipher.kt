package org.btcmap.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal object TokenCipher {

    const val ENCODED_PREFIX = "enc:v1:"

    private const val TAG = "TokenCipher"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "btcmap.auth.token"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    @Volatile
    private var cachedCiphertext: String? = null

    @Volatile
    private var cachedPlaintext: String? = null

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE).apply { load(null) }
    }

    private val key: SecretKey by lazy {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
            ?: generateKey()
    }

    private fun generateKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val payload = cipher.iv + encrypted
        return ENCODED_PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String? {
        if (encoded == cachedCiphertext) {
            return cachedPlaintext
        }

        val plaintext = try {
            val payload = Base64.decode(encoded.removePrefix(ENCODED_PREFIX), Base64.NO_WRAP)
            val iv = payload.copyOfRange(0, IV_SIZE_BYTES)
            val ciphertext = payload.copyOfRange(IV_SIZE_BYTES, payload.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decrypt stored token", e)
            return null
        }

        cachedCiphertext = encoded
        cachedPlaintext = plaintext
        return plaintext
    }
}
