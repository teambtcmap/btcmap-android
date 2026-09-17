package org.btcmap.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.InvalidAlgorithmParameterException
import java.security.KeyStore
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
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
    private const val GCM_TAG_LENGTH_BYTES = GCM_TAG_LENGTH_BITS / 8

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
     * Stand-in for the keystore key, set only while [withKeyProvider] runs. It is
     * null in production so the real keystore key is always used.
     */
    @Volatile
    internal var keyProviderOverride: (() -> SecretKey)? = null

    private val productionKeyProvider: () -> SecretKey = { defaultKey }

    /**
     * Runs [block] with [provider] standing in for the keystore key so tests can
     * simulate a missing or invalidated key. The override is always cleared
     * afterwards so it cannot leak into another test or a later request.
     */
    internal inline fun <T> withKeyProvider(noinline provider: () -> SecretKey, block: () -> T): T {
        check(keyProviderOverride == null) { "A key provider override is already active" }
        keyProviderOverride = provider
        return try {
            block()
        } finally {
            keyProviderOverride = null
        }
    }

    private fun key(): SecretKey = (keyProviderOverride ?: productionKeyProvider)()

    /** Drops the cached plaintext so it is not retained after signing out. */
    fun clearCache() {
        cache = null
    }

    /** Drops the cache only while it still holds [encoded], leaving a newer value intact. */
    internal fun clearCacheIf(encoded: String) {
        if (cache?.encoded == encoded) {
            cache = null
        }
    }

    /**
     * Seeds the cache after a token was encrypted so the next read does not hit
     * the keystore again, keeping main-thread reads off the crypto path.
     */
    internal fun rememberDecrypted(encoded: String, plaintext: String) {
        cache = Cache(encoded, DecryptResult.Success(plaintext))
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
        cipher.init(Cipher.ENCRYPT_MODE, key())
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
            key()
        } catch (e: Exception) {
            if (e.hasPermanentKeyInvalidationCause()) {
                Log.w(TAG, "Stored token key was permanently invalidated", e)
                return DecryptResult.Unrecoverable
            }
            Log.w(TAG, "Keystore unavailable; keeping the stored token", e)
            return DecryptResult.Unavailable
        }

        val iv: ByteArray
        val ciphertext: ByteArray
        try {
            val payload = Base64.decode(encoded.removePrefix(ENCODED_PREFIX), Base64.NO_WRAP)
            if (payload.size < IV_SIZE_BYTES + GCM_TAG_LENGTH_BYTES) {
                Log.w(TAG, "Stored token payload is too short; discarding")
                return DecryptResult.Unrecoverable
            }
            iv = payload.copyOfRange(0, IV_SIZE_BYTES)
            ciphertext = payload.copyOfRange(IV_SIZE_BYTES, payload.size)
        } catch (e: Exception) {
            Log.w(TAG, "Stored token cannot be decoded; discarding", e)
            return DecryptResult.Unrecoverable
        }

        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

            DecryptResult.Success(String(cipher.doFinal(ciphertext), Charsets.UTF_8))
        } catch (e: Exception) {
            classifyDecryptFailure(e).also { result ->
                // The key was obtained, so corruption can never be recovered, while
                // a keystore failure at this point may still be temporary. Android
                // keystore usually surfaces the latter from the cipher operation
                // rather than from the key lookup, so this classification is what
                // keeps a valid token from being discarded.
                if (result == DecryptResult.Unrecoverable) {
                    Log.w(TAG, "Stored token cannot be decrypted; discarding", e)
                } else {
                    Log.w(TAG, "Keystore unavailable; keeping the stored token", e)
                }
            }
        }
    }

    /**
     * Classifies a failure raised while decrypting with a key that was already
     * obtained. Only outcomes that can never change (corrupt payload or a
     * permanently invalidated key) are unrecoverable; every other crypto or
     * keystore failure may be temporary and must keep the stored token.
     */
    internal fun classifyDecryptFailure(e: Throwable): DecryptResult {
        var cause: Throwable? = e
        while (cause != null) {
            when (cause) {
                is KeyPermanentlyInvalidatedException,
                is AEADBadTagException,
                is BadPaddingException,
                is IllegalBlockSizeException,
                is InvalidAlgorithmParameterException,
                -> return DecryptResult.Unrecoverable
            }
            cause = cause.cause
        }
        return DecryptResult.Unavailable
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
