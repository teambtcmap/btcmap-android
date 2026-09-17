package org.btcmap.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
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

    internal sealed interface DecryptResult {
        data class Success(val plaintext: String) : DecryptResult

        /** The value can never be decrypted: replaced key or corrupt payload. */
        data object Unrecoverable : DecryptResult

        /** The keystore is temporarily unavailable; retry later without discarding. */
        data object Unavailable : DecryptResult
    }

    /**
     * Caches the last decrypt outcome. Keeping the ciphertext and its result in one
     * immutable value stops concurrent readers from observing a stale token and
     * avoids retrying the keystore for stable outcomes.
     */
    @Volatile
    private var cache: Cache? = null

    private data class Cache(
        val encoded: String,
        val result: DecryptResult,
    )

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE).apply { load(null) }
    }

    private val defaultKey: SecretKey by lazy {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
            ?: generateKey()
    }

    /**
     * Supplies the AES key. Production reads it from the Android keystore; tests
     * can replace it to simulate an unavailable keystore.
     */
    internal var keyProvider: () -> SecretKey = { defaultKey }

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
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider())
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val payload = cipher.iv + encrypted
        return ENCODED_PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): DecryptResult {
        cache?.let { if (it.encoded == encoded) return it.result }

        val result = tryDecrypt(encoded)

        // Cache only stable outcomes. An unavailable keystore may recover, so a
        // later read must be allowed to retry it rather than being pinned to a
        // failure.
        if (result != DecryptResult.Unavailable) {
            cache = Cache(encoded, result)
        }

        return result
    }

    private fun tryDecrypt(encoded: String): DecryptResult {
        val key = try {
            keyProvider()
        } catch (e: Exception) {
            if (e.hasPermanentKeyInvalidationCause()) {
                Log.w(TAG, "Stored token key was permanently invalidated", e)
                return DecryptResult.Unrecoverable
            }
            Log.w(TAG, "Keystore unavailable; keeping the stored token", e)
            return DecryptResult.Unavailable
        }

        return try {
            val payload = Base64.decode(encoded.removePrefix(ENCODED_PREFIX), Base64.NO_WRAP)
            val iv = payload.copyOfRange(0, IV_SIZE_BYTES)
            val ciphertext = payload.copyOfRange(IV_SIZE_BYTES, payload.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

            DecryptResult.Success(String(cipher.doFinal(ciphertext), Charsets.UTF_8))
        } catch (e: Exception) {
            // A key was obtained, so the stored value is corrupt, tampered with, or
            // was encrypted by a key that no longer exists. It cannot be recovered.
            Log.w(TAG, "Stored token cannot be decrypted; discarding", e)
            DecryptResult.Unrecoverable
        }
    }

    private fun Exception.hasPermanentKeyInvalidationCause(): Boolean {
        var cause: Throwable? = this
        while (cause != null) {
            if (cause is KeyPermanentlyInvalidatedException) return true
            cause = cause.cause
        }
        return false
    }
}
