package org.btcmap.settings

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.auth.TokenCipher
import org.btcmap.util.PreferencesRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthTokenPersistenceTest {

    @JvmField
    @Rule
    val preferencesRule = PreferencesRule()

    private val prefs get() = preferencesRule.prefs

    @Test
    fun storesTokenEncrypted() {
        prefs.authToken = "secret-token"

        val stored = prefs.getString(KEY_AUTH_TOKEN, null)

        Assert.assertNotNull(stored)
        Assert.assertTrue(stored!!.startsWith(TokenCipher.ENCODED_PREFIX))
        Assert.assertNotEquals("secret-token", stored)
        Assert.assertEquals("secret-token", prefs.authToken)
    }

    @Test
    fun upgradesLegacyPlaintextToken() {
        prefs.edit().putString(KEY_AUTH_TOKEN, "legacy-token").commit()

        Assert.assertEquals("legacy-token", prefs.authToken)

        val stored = prefs.getString(KEY_AUTH_TOKEN, null)

        Assert.assertNotNull(stored)
        Assert.assertTrue(stored!!.startsWith(TokenCipher.ENCODED_PREFIX))
        Assert.assertEquals("legacy-token", prefs.authToken)
    }

    @Test
    fun dropsUndecryptableTokenAndSignsOut() {
        val payload = ByteArray(12 + 16) { it.toByte() }
        val garbage = TokenCipher.ENCODED_PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
        prefs.edit().putString(KEY_AUTH_TOKEN, garbage).commit()

        Assert.assertNull(prefs.authToken)
        Assert.assertNull(prefs.getString(KEY_AUTH_TOKEN, null))
        Assert.assertFalse(prefs.authorized)
    }

    @Test
    fun keepsLegacyPlaintextTokenWhenKeystoreIsUnavailable() {
        withUnavailableKeystore {
            prefs.edit().putString(KEY_AUTH_TOKEN, "legacy-token").commit()

            // The session must survive: the token is returned as-is and left in
            // place rather than being dropped when it cannot be encrypted.
            Assert.assertEquals("legacy-token", prefs.authToken)
            Assert.assertEquals("legacy-token", prefs.getString(KEY_AUTH_TOKEN, null))
            Assert.assertTrue(prefs.authorized)
        }
    }

    @Test
    fun keepsEncryptedTokenWhenKeystoreIsTemporarilyUnavailable() {
        prefs.authToken = "secret-token"
        val stored = prefs.getString(KEY_AUTH_TOKEN, null)

        withUnavailableKeystore {
            // The token cannot be read, but it must not be discarded and the user
            // must not be shown as signed out while it can still recover.
            Assert.assertNull(prefs.authToken)
            Assert.assertTrue(prefs.authorized)
            Assert.assertEquals(stored, prefs.getString(KEY_AUTH_TOKEN, null))
        }

        // Once the keystore recovers the same session is readable again, proving
        // the transient failure was not cached.
        Assert.assertEquals("secret-token", prefs.authToken)
    }

    @Test
    fun storingTokenFailsWhenKeystoreIsUnavailable() {
        withUnavailableKeystore {
            val error = runCatching { prefs.authToken = "secret-token" }.exceptionOrNull()

            Assert.assertNotNull(error)
            Assert.assertNull(prefs.getString(KEY_AUTH_TOKEN, null))
        }
    }

    private fun withUnavailableKeystore(block: () -> Unit) {
        val original = TokenCipher.keyProvider
        TokenCipher.keyProvider = { throw IllegalStateException("keystore unavailable") }
        try {
            block()
        } finally {
            TokenCipher.keyProvider = original
        }
    }

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
    }
}
