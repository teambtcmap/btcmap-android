package org.btcmap.auth

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class TokenCipherTest {

    @Test
    fun roundTripsPlaintext() {
        val encoded = TokenCipher.encrypt("satoshi-token")

        Assert.assertTrue(encoded.startsWith(TokenCipher.ENCODED_PREFIX))
        Assert.assertEquals("satoshi-token", TokenCipher.decrypt(encoded))
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

        Assert.assertNull(TokenCipher.decrypt(tampered))
    }

    @Test
    fun rejectsUndecodableCiphertext() {
        Assert.assertNull(TokenCipher.decrypt(TokenCipher.ENCODED_PREFIX + "not base64"))
    }

    @Test
    fun rejectsTooShortPayload() {
        val payload = ByteArray(4)
        val encoded = TokenCipher.ENCODED_PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)

        Assert.assertNull(TokenCipher.decrypt(encoded))
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
                            val actual = TokenCipher.decrypt(encoded)
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
