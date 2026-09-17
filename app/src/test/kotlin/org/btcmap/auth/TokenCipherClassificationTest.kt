package org.btcmap.auth

import org.junit.Assert
import org.junit.Test
import java.security.InvalidAlgorithmParameterException
import java.security.KeyStoreException
import java.security.ProviderException
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException

/**
 * JVM coverage for the pure failure classification used by [TokenCipher], so the
 * decision of when a stored token may be discarded is protected by
 * `./gradlew check` without a device. The keystore round-trip and the Android
 * `KeyPermanentlyInvalidatedException` case stay covered on-device by
 * `TokenCipherTest`.
 */
class TokenCipherClassificationTest {

    @Test
    fun corruptCiphertextIsUnrecoverable() {
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
    }

    @Test
    fun corruptionWrappedByTheProviderIsStillUnrecoverable() {
        Assert.assertEquals(
            TokenCipher.DecryptResult.Unrecoverable,
            TokenCipher.classifyDecryptFailure(RuntimeException(AEADBadTagException())),
        )
    }

    @Test
    fun transientKeystoreFailuresAreUnavailable() {
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
}
