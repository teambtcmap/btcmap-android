package org.btcmap.auth

import org.junit.Assert
import org.junit.Test

/**
 * JVM coverage for the decrypt cache, including the priming done after a token
 * is stored. This path must never touch the keystore, so it is safe to assert
 * without a device.
 */
class TokenCipherCacheTest {

    @Test
    fun primedValueIsServedWithoutTheKeystore() {
        TokenCipher.rememberDecrypted("encoded-1", "token-1")

        // A cache hit must return straight away, without consulting the key.
        Assert.assertEquals(
            TokenCipher.DecryptResult.Success("token-1"),
            TokenCipher.decrypt("encoded-1"),
        )
    }

    @Test
    fun clearCacheIfKeepsOtherValues() {
        TokenCipher.rememberDecrypted("encoded-1", "token-1")

        TokenCipher.clearCacheIf("encoded-other")

        Assert.assertEquals(
            TokenCipher.DecryptResult.Success("token-1"),
            TokenCipher.decrypt("encoded-1"),
        )
    }
}
