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

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
    }
}
