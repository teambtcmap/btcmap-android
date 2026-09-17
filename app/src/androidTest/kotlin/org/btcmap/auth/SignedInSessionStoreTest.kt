package org.btcmap.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.btcmap.api.CreateTokenResponse
import org.btcmap.api.User
import org.btcmap.db.table.user.User as DbUser
import org.btcmap.settings.authToken
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignedInSessionStoreTest {

    @JvmField
    @Rule
    val databaseRule = DatabaseRule()

    @JvmField
    @Rule
    val preferencesRule = PreferencesRule()

    private val prefs get() = preferencesRule.prefs

    @Test
    fun storesTokenAndCachedUser() = runBlocking {
        storeSignedInSession(databaseRule.db, prefs, response("token-1"))

        Assert.assertEquals("token-1", prefs.authToken)
        val user = databaseRule.db.user.select()
        Assert.assertNotNull(user)
        Assert.assertEquals(1L, user!!.id)
        Assert.assertEquals("satoshi", user.name)
    }

    @Test
    fun clearsBothWhenTokenCannotBeStored() = runBlocking {
        // A previous session is already stored...
        prefs.authToken = "old-token"
        databaseRule.db.user.insert(dbUser(id = 99, name = "stale"))

        // ...but a sign-in that fails to store its token must not leave the old
        // session behind in a half-applied state.
        val error = withUnavailableKeystore {
            runCatching { storeSignedInSession(databaseRule.db, prefs, response("new-token")) }
                .exceptionOrNull()
        }

        Assert.assertNotNull(error)
        Assert.assertNull(prefs.authToken)
        Assert.assertNull(databaseRule.db.user.select())
    }

    private fun response(token: String) = CreateTokenResponse(
        token = token,
        user = User(
            id = 1,
            name = "satoshi",
            roles = listOf("user"),
            savedPlaces = emptyList(),
            savedAreas = emptyList(),
        ),
    )

    private fun dbUser(id: Long, name: String) = DbUser(
        id = id,
        name = name,
        roles = emptyList(),
        savedPlaces = emptyList(),
        savedAreas = emptyList(),
    )

    private inline fun <T> withUnavailableKeystore(block: () -> T): T {
        val original = TokenCipher.keyProvider
        TokenCipher.keyProvider = { throw IllegalStateException("keystore unavailable") }
        try {
            return block()
        } finally {
            TokenCipher.keyProvider = original
        }
    }
}
