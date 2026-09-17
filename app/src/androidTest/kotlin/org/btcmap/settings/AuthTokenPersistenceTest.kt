package org.btcmap.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthTokenPersistenceTest {

    @JvmField
    @Rule
    val databaseRule = DatabaseRule()

    @JvmField
    @Rule
    val preferencesRule = PreferencesRule()

    private val prefs get() = preferencesRule.prefs

    @Test
    fun storesTokenInTheDatabase() {
        prefs.setAuthTokenForTesting("secret-token")

        Assert.assertEquals("secret-token", prefs.authToken)
        Assert.assertEquals("secret-token", databaseRule.db.preference.select(KEY_AUTH_TOKEN))
        Assert.assertTrue(prefs.authorized)
    }

    @Test
    fun clearingTokenSignsOut() {
        prefs.setAuthTokenForTesting("secret-token")

        prefs.clearSession(databaseRule.db)

        Assert.assertNull(prefs.authToken)
        Assert.assertFalse(prefs.authorized)
        Assert.assertNull(databaseRule.db.preference.select(KEY_AUTH_TOKEN))
    }
}
