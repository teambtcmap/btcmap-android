package org.btcmap.auth

import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.security.InvalidAlgorithmParameterException
import java.security.KeyStoreException
import java.security.ProviderException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException

@RunWith(AndroidJUnit4::class)
class TokenCipherTest {

    private fun plaintextOf(encoded: String): String? =
        (TokenCipher.decrypt(encoded) as? TokenCipher.DecryptResult.Success)?.plaintext

    @Test
    fun roundTripsPlaintext() {
        val encoded = TokenCipher.encrypt("satoshi-token")

        Assert.assertTrue(encoded.startsWith(TokenCipher.ENCODED_PREFIX))
        Assert.assertEquals("satoshi-token", plaintextOf(encoded))
    }

    @Test
    fun usesFreshIvForEveryEncryption() {
        // The decrypt cache matches on the ciphertext, so a repeated plaintext
        // must not produce an identical payload.
        Assert.assertNotEquals(
            TokenCipher.encrypt("same-token"),
            TokenCipher.encrypt("same-token"),
        )
    }

    @Test
    fun rejectsTamperedCiphertext() {
        val encoded = TokenCipher.encrypt("satoshi-token")
        val payload = encoded.removePrefix(TokenCipher.ENCODED_PREFIX)
            .let { Base64.decode(it, Base64.NO_WRAP) }
            .apply { this[size - 1] = (this[size - 1].toInt() xor 0xFF).toByte() }
        val tampered = TokenCipher.ENCODED_PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)

        Assert.assertEquals(TokenCipher.DecryptResult.Unrecoverable, TokenCipher.decrypt(tampered))
    }

    @Test
    fun rejectsUndecodableCiphertext() {
        Assert.assertEquals(
            TokenCipher.DecryptResult.Unrecoverable,
            TokenCipher.decrypt(TokenCipher.ENCODED_PREFIX + "not base64"),
        )
    }

    @Test
    fun rejectsTooShortPayload() {
        val payload = ByteArray(4)
        val encoded = TokenCipher.ENCODED_PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)

        Assert.assertEquals(TokenCipher.DecryptResult.Unrecoverable, TokenCipher.decrypt(encoded))
    }

    @Test
    fun rejectsPayloadShorterThanIvAndTag() {
        // The smallest valid payload is a 12-byte IV plus a 16-byte GCM tag.
        val payload = ByteArray(12 + 16 - 1)
        val encoded = TokenCipher.ENCODED_PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)

        Assert.assertEquals(TokenCipher.DecryptResult.Unrecoverable, TokenCipher.decrypt(encoded))
    }

    @Test
    fun classifiesCorruptCiphertextAsUnrecoverable() {
        Assert.assertEquals(
            TokenCipher.DecryptResult.Unrecoverable,
            TokenCipher.classifyDecryptFailure(AEADBadTagException()),
        )
        Assert.assertEquals(
            TokenCipher.DecryptResult.Unrecoverable,
            TokenCipher.classifyDecryptFailure(BadPaddingException()),
        )
        Assert.assertEquals(
            TokenCipher.DecryptResult.Unrecoverable,
            TokenCipher.classifyDecryptFailure(IllegalBlockSizeException()),
        )
        Assert.assertEquals(
            TokenCipher.DecryptResult.Unrecoverable,
            TokenCipher.classifyDecryptFailure(InvalidAlgorithmParameterException()),
        )
        Assert.assertEquals(
            TokenCipher.DecryptResult.Unrecoverable,
            TokenCipher.classifyDecryptFailure(KeyPermanentlyInvalidatedException()),
        )
        // Corruption can also arrive wrapped by the crypto provider.
        Assert.assertEquals(
            TokenCipher.DecryptResult.Unrecoverable,
            TokenCipher.classifyDecryptFailure(RuntimeException(AEADBadTagException())),
        )
    }

    @Test
    fun classifiesTransientKeystoreFailuresAsUnavailable() {
        Assert.assertEquals(
            TokenCipher.DecryptResult.Unavailable,
            TokenCipher.classifyDecryptFailure(KeyStoreException("keystore unavailable")),
        )
        Assert.assertEquals(
            TokenCipher.DecryptResult.Unavailable,
            TokenCipher.classifyDecryptFailure(IllegalStateException("keystore busy")),
        )
        Assert.assertEquals(
            TokenCipher.DecryptResult.Unavailable,
            TokenCipher.classifyDecryptFailure(ProviderException(KeyStoreException())),
        )
    }

    @Test
    fun treatsPermanentlyInvalidatedKeyAsUnrecoverable() {
        val encoded = TokenCipher.encrypt("satoshi-token")

        TokenCipher.withKeyProvider({ throw KeyPermanentlyInvalidatedException() }) {
            Assert.assertEquals(
                TokenCipher.DecryptResult.Unrecoverable,
                TokenCipher.decrypt(encoded),
            )
        }
    }

    @Test
    fun treatsUnavailableKeyProviderAsUnavailable() {
        val encoded = TokenCipher.encrypt("satoshi-token")

        TokenCipher.withKeyProvider({ throw KeyStoreException("keystore unavailable") }) {
            Assert.assertEquals(
                TokenCipher.DecryptResult.Unavailable,
                TokenCipher.decrypt(encoded),
            )
        }

        // A transient failure must not be cached, so the token recovers afterwards.
        Assert.assertEquals("satoshi-token", plaintextOf(encoded))
    }

    @Test
    fun returnsConsistentTokenUnderConcurrentReads() {
        val tokenA = TokenCipher.encrypt("token-a")
        val tokenB = TokenCipher.encrypt("token-b")

        val threadCount = 4
        val iterations = 100
        val start = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>(null)
        val executor = Executors.newFixedThreadPool(threadCount)

        try {
            val tasks = (0 until threadCount).map { index ->
                val encoded = if (index % 2 == 0) tokenA else tokenB
                val expected = if (index % 2 == 0) "token-a" else "token-b"
                executor.submit {
                    try {
                        start.await()
                        repeat(iterations) {
                            val actual = plaintextOf(encoded)
                            if (actual != expected) {
                                throw AssertionError("Expected $expected but got $actual")
                            }
                        }
                    } catch (t: Throwable) {
                        failure.compareAndSet(null, t)
                    }
                }
            }

            start.countDown()
            tasks.forEach { it.get(60, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        failure.get()?.let { throw it }
    }
}
